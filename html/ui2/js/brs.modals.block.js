/**
 * @depends {brs.js}
 * @depends {brs.modals.js}
 */
var BRS = (function(BRS, $, undefined) {
    $("#blocks_table, #dashboard_blocks_table").on("click", "a[data-block]", function(event) {
	event.preventDefault();

	if (BRS.fetchingModalData) {
	    return;
	}

	BRS.fetchingModalData = true;

	var blockHeight = $(this).data("block");

	BRS.sendRequest("getBlock+", {
	    "height": blockHeight,
	    "includeTransactions": "true"
	}, function(response) {
	    BRS.showBlockModal(response);
	});
    });

    BRS.showBlockModal = function(block) {
	$("#block_info_modal_block").html(String(block.block).escapeHTML());

	$("#block_info_transactions_tab_link").tab("show");

	var blockDetails = $.extend({}, block);
	delete blockDetails.transactions;
	delete blockDetails.previousBlockHash;
	delete blockDetails.nextBlockHash;
	delete blockDetails.generationSignature;
	delete blockDetails.payloadHash;
	delete blockDetails.block;

	$("#block_info_details_table tbody").empty().append(BRS.createInfoTable(blockDetails));
	$("#block_info_details_table").show();

	if (block.transactions.length) {
	    $("#block_info_transactions_none").hide();
	    $("#block_info_transactions_table").show();

	    var rows = "";

	    block.transactions.sort(function(a, b) {
		return a.timestamp - b.timestamp;
	    });

	    for (var i = 0; i < block.transactions.length; i++) {
		var transaction = block.transactions[i];

		if (transaction.amountNQT) {
		    transaction.amount = new BigInteger(transaction.amountNQT);
		    transaction.fee = new BigInteger(transaction.feeNQT);
		}

		rows += "<tr><td>" + BRS.formatTime(transaction.timestamp) + "</td><td>" + BRS.formatAmount(transaction.amount) + "</td><td>" + BRS.formatAmount(transaction.fee) + "</td><td>" + BRS.getAccountTitle(transaction, "recipient") + "</td><td>" + BRS.getAccountTitle(transaction, "sender") + "</td></tr>";
	    }

	    $("#block_info_transactions_table tbody").empty().append(rows);
	    $("#block_info_modal").modal("show");

	    BRS.fetchingModalData = false;
	}
        else {
	    $("#block_info_transactions_none").show();
	    $("#block_info_transactions_table").hide();
	    $("#block_info_modal").modal("show");

	    BRS.fetchingModalData = false;
	}
    };

    return BRS;
}(BRS || {}, jQuery));
