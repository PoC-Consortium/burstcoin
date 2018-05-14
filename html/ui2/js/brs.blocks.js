/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.blocksPageType = null;
    BRS.tempBlocks = [];
    var trackBlockchain = false;

    BRS.getBlock = function(blockID, callback, pageRequest) {
	BRS.sendRequest("getBlock" + (pageRequest ? "+" : ""), {
	    "block": blockID
	}, function(response) {
	    if (response.errorCode && response.errorCode == -1) {
		BRS.getBlock(blockID, callback, pageRequest);
	    }
            else {
		if (callback) {
		    response.block = blockID;
		    callback(response);
		}
	    }
	}, true);
    };

    BRS.handleInitialBlocks = function(response) {
	if (response.errorCode) {
	    BRS.dataLoadFinished($("#dashboard_blocks_table"));
	    return;
	}

	BRS.blocks.push(response);

	if (BRS.blocks.length < 10 && response.previousBlock) {
	    BRS.getBlock(response.previousBlock, BRS.handleInitialBlocks);
	}
        else {
	    BRS.checkBlockHeight(BRS.blocks[0].height);

	    if (BRS.state) {
		//if no new blocks in 6 hours, show blockchain download progress..
		var timeDiff = BRS.state.time - BRS.blocks[0].timestamp;
		if (timeDiff > 60 * 60 * 18) {
		    if (timeDiff > 60 * 60 * 24 * 14) {
			BRS.setStateInterval(30);
		    }
                    else if (timeDiff > 60 * 60 * 24 * 7) {
			//second to last week
			BRS.setStateInterval(15);
		    }
                    else {
			//last week
			BRS.setStateInterval(10);
		    }
		    BRS.downloadingBlockchain = true;
		    $("#brs_update_explanation span").hide();
		    $("#brs_update_explanation_wait").attr("style", "display: none !important");
		    $("#downloading_blockchain, #brs_update_explanation_blockchain_sync").show();
		    $("#show_console").hide();
		    BRS.updateBlockchainDownloadProgress();
		}
                else {
		    //continue with faster state intervals if we still haven't reached current block from within 1 hour
		    if (timeDiff < 60 * 60) {
			BRS.setStateInterval(30);
			trackBlockchain = false;
		    }
                    else {
			BRS.setStateInterval(10);
			trackBlockchain = true;
		    }
		}
	    }

	    var rows = "";

	    for (var i = 0; i < BRS.blocks.length; i++) {
		var block = BRS.blocks[i];

		rows += "<tr><td><a href='#' data-block='" + String(block.height).escapeHTML() + "' data-blockid='" + String(block.block).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td><td data-timestamp='" + String(block.timestamp).escapeHTML() + "'>" + BRS.formatTimestamp(block.timestamp) + "</td><td>" + BRS.formatAmount(block.totalAmountNQT) + " + " + BRS.formatAmount(block.totalFeeNQT) + "</td><td>" + BRS.formatAmount(block.numberOfTransactions) + "</td></tr>";
	    }

	    $("#dashboard_blocks_table tbody").empty().append(rows);
	    BRS.dataLoadFinished($("#dashboard_blocks_table"));
	}
    };

    BRS.handleNewBlocks = function(response) {
	if (BRS.downloadingBlockchain) {
	    //new round started...
	    if (BRS.tempBlocks.length === 0 && BRS.state.lastBlock != response.block) {
		return;
	    }
	}

	//we have all blocks 	
	if (response.height - 1 == BRS.lastBlockHeight || BRS.tempBlocks.length == 99) {
	    var newBlocks = [];

	    //there was only 1 new block (response)
	    if (BRS.tempBlocks.length === 0) {
		//remove oldest block, add newest block
		BRS.blocks.unshift(response);
		newBlocks.push(response);
	    }
            else {
		BRS.tempBlocks.push(response);
		//remove oldest blocks, add newest blocks
		[].unshift.apply(BRS.blocks, BRS.tempBlocks);
		newBlocks = BRS.tempBlocks;
		BRS.tempBlocks = [];
	    }

	    if (BRS.blocks.length > 100) {
		BRS.blocks = BRS.blocks.slice(0, 100);
	    }

	    BRS.checkBlockHeight(BRS.blocks[0].height);

	    BRS.incoming.updateDashboardBlocks(newBlocks);
	}
        else {
	    BRS.tempBlocks.push(response);
	    BRS.getBlock(response.previousBlock, BRS.handleNewBlocks);
	}
    };

    BRS.checkBlockHeight = function(blockHeight) {
	if (blockHeight) {
	    BRS.lastBlockHeight = blockHeight;
	}

	//no checks needed at the moment
    };

    //we always update the dashboard page..
    BRS.incoming.updateDashboardBlocks = function(newBlocks) {
	var newBlockCount = newBlocks.length;

	if (newBlockCount > 10) {
	    newBlocks = newBlocks.slice(0, 10);
	    newBlockCount = newBlocks.length;
	}

	if (BRS.downloadingBlockchain) {
	    if (BRS.state) {
		var timeDiff = BRS.state.time - BRS.blocks[0].timestamp;
		if (timeDiff < 60 * 60 * 18) {
		    if (timeDiff < 60 * 60) {
			BRS.setStateInterval(30);
		    }
                    else {
			BRS.setStateInterval(10);
			trackBlockchain = true;
		    }
		    BRS.downloadingBlockchain = false;
		    $("#dashboard_message").hide();
		    $("#downloading_blockchain, #brs_update_explanation_blockchain_sync").hide();
		    $("#brs_update_explanation_wait").removeAttr("style");
		    if (BRS.settings.console_log && !BRS.inApp) {
			$("#show_console").show();
		    }
		    $.notify($.t("success_blockchain_up_to_date"), {
			"type": "success"
		    });
		    BRS.checkAliasVersions();
		    BRS.checkIfOnAFork();
		}
                else {
		    if (timeDiff > 60 * 60 * 24 * 14) {
			BRS.setStateInterval(30);
		    }
                    else if (timeDiff > 60 * 60 * 24 * 7) {
			//second to last week
			BRS.setStateInterval(15);
		    }
                    else {
			//last week
			BRS.setStateInterval(10);
		    }

		    BRS.updateBlockchainDownloadProgress();
		}
	    }
	}
        else if (trackBlockchain) {
	    var timeDiff = BRS.state.time - BRS.blocks[0].timestamp;

	    //continue with faster state intervals if we still haven't reached current block from within 1 hour
	    if (timeDiff < 60 * 60) {
		BRS.setStateInterval(30);
		trackBlockchain = false;
	    }
            else {
		BRS.setStateInterval(10);
	    }
	}

	var rows = "";

	for (var i = 0; i < newBlockCount; i++) {
	    var block = newBlocks[i];

	    rows += "<tr><td><a href='#' data-block='" + String(block.height).escapeHTML() + "' data-blockid='" + String(block.block).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td><td data-timestamp='" + String(block.timestamp).escapeHTML() + "'>" + BRS.formatTimestamp(block.timestamp) + "</td><td>" + BRS.formatAmount(block.totalAmountNQT) + " + " + BRS.formatAmount(block.totalFeeNQT) + "</td><td>" + BRS.formatAmount(block.numberOfTransactions) + "</td></tr>";
	}

	if (newBlockCount == 1) {
	    $("#dashboard_blocks_table tbody tr:last").remove();
	}
        else if (newBlockCount == 10) {
	    $("#dashboard_blocks_table tbody").empty();
	}
        else {
	    $("#dashboard_blocks_table tbody tr").slice(10 - newBlockCount).remove();
	}

	$("#dashboard_blocks_table tbody").prepend(rows);

	//update number of confirmations... perhaps we should also update it in tne BRS.transactions array
	$("#dashboard_transactions_table tr.confirmed td.confirmations").each(function() {
	    if ($(this).data("incoming")) {
		$(this).removeData("incoming");
		return true;
	    }

	    var confirmations = parseInt($(this).data("confirmations"), 10);

	    var nrConfirmations = confirmations + newBlocks.length;

	    if (confirmations <= 10) {
		$(this).data("confirmations", nrConfirmations);
		$(this).attr("data-content", $.t("x_confirmations", {
		    "x": BRS.formatAmount(nrConfirmations, false, true)
		}));

		if (nrConfirmations > 10) {
		    nrConfirmations = '10+';
		}
		$(this).html(nrConfirmations);
	    }
            else {
		$(this).attr("data-content", $.t("x_confirmations", {
		    "x": BRS.formatAmount(nrConfirmations, false, true)
		}));
	    }
	});
    };

    BRS.pages.blocks = function() {
	if (BRS.blocksPageType == "forged_blocks") {
	    $("#forged_fees_total_box, #forged_blocks_total_box").show();
	    $("#blocks_transactions_per_hour_box, #blocks_generation_time_box").hide();

	    BRS.sendRequest("getAccountBlockIds+", {
		"account": BRS.account,
		"timestamp": 0
	    }, function(response) {
		if (response.blockIds && response.blockIds.length) {
		    var blocks = [];
		    var nrBlocks = 0;

		    var blockIds = response.blockIds.slice(0, 100);

		    if (response.blockIds.length > 100) {
			$("#blocks_page_forged_warning").show();
		    }

		    for (var i = 0; i < blockIds.length; i++) {
			BRS.sendRequest("getBlock+", {
			    "block": blockIds[i],
			    "_extra": {
				"nr": i
			    }
			}, function(block, input) {
			    if (BRS.currentPage != "blocks") {
				blocks = {};
				return;
			    }

			    block.block = input.block;
			    blocks[input["_extra"].nr] = block;
			    nrBlocks++;

			    if (nrBlocks == blockIds.length) {
				BRS.blocksPageLoaded(blocks);
			    }
			});
		    }
		}
                else {
		    BRS.blocksPageLoaded([]);
		}
	    });
	}
        else {
	    $("#forged_fees_total_box, #forged_blocks_total_box").hide();
	    $("#blocks_transactions_per_hour_box, #blocks_generation_time_box").show();

	    if (BRS.blocks.length < 100) {
		if (BRS.downloadingBlockchain) {
		    BRS.blocksPageLoaded(BRS.blocks);
		}
                else {
		    if (BRS.blocks && BRS.blocks.length) {
			var previousBlock = BRS.blocks[BRS.blocks.length - 1].previousBlock;
			//if previous block is undefined, dont try add it
			if (typeof previousBlock !== "undefined") {
			    BRS.getBlock(previousBlock, BRS.finish100Blocks, true);
			}
		    }
                    else {
			BRS.blocksPageLoaded([]);
		    }
		}
	    }
            else {
		BRS.blocksPageLoaded(BRS.blocks);
	    }
	}
    };

    BRS.incoming.blocks = function() {
	BRS.loadPage("blocks");
    };

    BRS.finish100Blocks = function(response) {
	BRS.blocks.push(response);
	if (BRS.blocks.length < 100 && typeof response.previousBlock !== "undefined") {
	    BRS.getBlock(response.previousBlock, BRS.finish100Blocks, true);
	}
        else {
	    BRS.blocksPageLoaded(BRS.blocks);
	}
    };

    BRS.blocksPageLoaded = function(blocks) {
	var rows = "";
	var totalAmount = new BigInteger("0");
	var totalFees = new BigInteger("0");
	var totalTransactions = 0;

	for (var i = 0; i < blocks.length; i++) {
	    var block = blocks[i];

	    totalAmount = totalAmount.add(new BigInteger(block.totalAmountNQT));

	    totalFees = totalFees.add(new BigInteger(block.totalFeeNQT));

	    totalTransactions += block.numberOfTransactions;

	    rows += "<tr><td><a href='#' data-block='" + String(block.height).escapeHTML() + "' data-blockid='" + String(block.block).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td><td>" + BRS.formatTimestamp(block.timestamp) + "</td><td>" + BRS.formatAmount(block.totalAmountNQT) + "</td><td>" + BRS.formatAmount(block.totalFeeNQT) + "</td><td>" + BRS.formatAmount(block.numberOfTransactions) + "</td><td>" + (block.generator != BRS.genesis ? "<a href='#' data-user='" + BRS.getAccountFormatted(block, "generator") + "' class='user_info'>" + BRS.getAccountTitle(block, "generator") + "</a>" : $.t("genesis")) + "</td><td>" + BRS.formatVolume(block.payloadLength) + "</td><td>" + Math.round(block.baseTarget / 153722867 * 100).pad(4) + " %</td></tr>";
	}

	if (blocks.length) {
	    var startingTime = blocks[blocks.length - 1].timestamp;
	    var endingTime = blocks[0].timestamp;
	    var time = endingTime - startingTime;
	}
        else {
	    var startingTime = endingTime = time = 0;
	}

	if (blocks.length) {
	    var averageFee = new Big(totalFees.toString()).div(new Big("100000000")).div(new Big(String(blocks.length))).toFixed(2);
	    var averageAmount = new Big(totalAmount.toString()).div(new Big("100000000")).div(new Big(String(blocks.length))).toFixed(2);
	}
        else {
	    var averageFee = 0;
	    var averageAmount = 0;
	}

	averageFee = BRS.convertToNQT(averageFee);
	averageAmount = BRS.convertToNQT(averageAmount);

	$("#blocks_average_fee").html(BRS.formatStyledAmount(averageFee)).removeClass("loading_dots");
	$("#blocks_average_amount").html(BRS.formatStyledAmount(averageAmount)).removeClass("loading_dots");

	if (BRS.blocksPageType == "forged_blocks") {
	    if (blocks.length == 100) {
		var blockCount = blocks.length + "+";
	    }
            else {
		var blockCount = blocks.length;
	    }

	    $("#forged_blocks_total").html(blockCount).removeClass("loading_dots");
	    $("#forged_fees_total").html(BRS.formatStyledAmount(BRS.accountInfo.forgedBalanceNQT)).removeClass("loading_dots");
	}
        else {
	    if (time === 0) {
		$("#blocks_transactions_per_hour").html("0").removeClass("loading_dots");
	    }
            else {
		$("#blocks_transactions_per_hour").html(Math.round(totalTransactions / (time / 60) * 60)).removeClass("loading_dots");
	    }
	    $("#blocks_average_generation_time").html(Math.round(time / 100) + "s").removeClass("loading_dots");
	}

	BRS.dataLoaded(rows);
    };

    $("#blocks_page_type .btn").click(function(e) {
	//	$("#blocks_page_type li a").click(function(e) {
	e.preventDefault();

	BRS.blocksPageType = $(this).data("type");

	$("#blocks_average_amount, #blocks_average_fee, #blocks_transactions_per_hour, #blocks_average_generation_time, #forged_blocks_total, #forged_fees_total").html("<span>.</span><span>.</span><span>.</span></span>").addClass("loading_dots");
	$("#blocks_table tbody").empty();
	$("#blocks_table").parent().addClass("data-loading").removeClass("data-empty");

	BRS.loadPage("blocks");
    });

    $("#goto_forged_blocks").click(function(e) {
	e.preventDefault();

	$("#blocks_page_type").find(".btn:last").button("toggle");
	BRS.blocksPageType = "forged_blocks";
	BRS.goToPage("blocks");
    });

    return BRS;
}(BRS || {}, jQuery));
