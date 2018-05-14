/**
 * @depends {brs.js}
 * @depends {brs.modals.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.userInfoModal = {
	"user": 0
    };

    $("#blocks_table, #contacts_table, #transactions_table, #dashboard_transactions_table, #asset_account, #asset_exchange_ask_orders_table, #transfer_history_table, #asset_exchange_bid_orders_table, #alias_info_table, .dgs_page_contents, .modal-content, #register_alias_modal").on("click", "a[data-user]", function(e) {
	e.preventDefault();

	var account = $(this).data("user");

	BRS.showAccountModal(account);
    });

    BRS.showAccountModal = function(account) {
	if (BRS.fetchingModalData) {
	    return;
	}

	if (typeof account == "object") {
	    BRS.userInfoModal.user = account.account;
	}
        else {
	    BRS.userInfoModal.user = account;
	    BRS.fetchingModalData = true;
	}

	$("#user_info_modal_account").html(BRS.getAccountFormatted(BRS.userInfoModal.user));

	if (BRS.userInfoModal.user in BRS.contacts) {
	    var accountButton = BRS.contacts[BRS.userInfoModal.user].name.escapeHTML();
	    $("#user_info_modal_add_as_contact").hide();
	}
        else {
	    var accountButton = BRS.userInfoModal.user;
	    $("#user_info_modal_add_as_contact").show();
	}

	$("#user_info_modal_actions button").data("account", accountButton);

	if (BRS.fetchingModalData) {
	    BRS.sendRequest("getAccount", {
		"account": BRS.userInfoModal.user
	    }, function(response) {
		BRS.processAccountModalData(response);
		BRS.fetchingModalData = false;
	    });
	}
        else {
	    BRS.processAccountModalData(account);
	}

	$("#user_info_modal_transactions").show();

	BRS.userInfoModal.transactions();
    };

    BRS.processAccountModalData = function(account) {
	if (account.unconfirmedBalanceNQT == "0") {
	    $("#user_info_modal_account_balance").html("0");
	}
        else {
	    $("#user_info_modal_account_balance").html(BRS.formatAmount(account.unconfirmedBalanceNQT) + " BURST");
	}

	if (account.name) {
	    $("#user_info_modal_account_name").html(String(account.name).escapeHTML());
	    $("#user_info_modal_account_name_container").show();
	}
        else {
	    $("#user_info_modal_account_name_container").hide();
	}

	if (account.description) {
	    $("#user_info_description").show();
	    $("#user_info_modal_description").html(String(account.description).escapeHTML().nl2br());
	}
        else {
	    $("#user_info_description").hide();
	}

	$("#user_info_modal").modal("show");
    };

    $("#user_info_modal").on("hidden.bs.modal", function(e) {
	$(this).find(".user_info_modal_content").hide();
	$(this).find(".user_info_modal_content table tbody").empty();
	$(this).find(".user_info_modal_content:not(.data-loading,.data-never-loading)").addClass("data-loading");
	$(this).find("ul.nav li.active").removeClass("active");
	$("#user_info_transactions").addClass("active");
	BRS.userInfoModal.user = 0;
    });

    $("#user_info_modal ul.nav li").click(function(e) {
	e.preventDefault();

	var tab = $(this).data("tab");

	$(this).siblings().removeClass("active");
	$(this).addClass("active");

	$(".user_info_modal_content").hide();

	var content = $("#user_info_modal_" + tab);

	content.show();

	if (content.hasClass("data-loading")) {
	    BRS.userInfoModal[tab]();
	}
    });

    /*some duplicate methods here...*/
    BRS.userInfoModal.transactions = function(type) {
	BRS.sendRequest("getAccountTransactions", {
	    "account": BRS.userInfoModal.user,
	    "firstIndex": 0,
	    "lastIndex": 100
	}, function(response) {
	    if (response.transactions && response.transactions.length) {
		var rows = "";

		for (var i = 0; i < response.transactions.length; i++) {
		    var transaction = response.transactions[i];

		    var transactionType = "Unknown";

		    if (transaction.type == 0) {
			transactionType = $.t("ordinary_payment");
		    }
                    else if (transaction.type == 1) {
			switch (transaction.subtype) {
			case 0:
			    transactionType = $.t("arbitrary_message");
			    break;
			case 1:
			    transactionType = $.t("alias_assignment");
			    break;
			case 2:
			    transactionType = $.t("poll_creation");
			    break;
			case 3:
			    transactionType = $.t("vote_casting");
			    break;
			case 4:
			    transactionType = $.t("hub_announcement");
			    break;
			case 5:
			    transactionType = $.t("account_info");
			    break;
			case 6:
			    if (transaction.attachment.priceNQT == "0") {
				if (transaction.sender == transaction.recipient) {
				    transactionType = $.t("alias_sale_cancellation");
				}
                                else {
				    transactionType = $.t("alias_transfer");
				}
			    }
                            else {
				transactionType = $.t("alias_sale");
			    }
			    break;
			case 7:
			    transactionType = $.t("alias_buy");
			    break;
			}
		    }
                    else if (transaction.type == 2) {
			switch (transaction.subtype) {
			case 0:
			    transactionType = $.t("asset_issuance");
			    break;
			case 1:
			    transactionType = $.t("asset_transfer");
			    break;
			case 2:
			    transactionType = $.t("ask_order_placement");
			    break;
			case 3:
			    transactionType = $.t("bid_order_placement");
			    break;
			case 4:
			    transactionType = $.t("ask_order_cancellation");
			    break;
			case 5:
			    transactionType = $.t("bid_order_cancellation");
			    break;
			}
		    }
                    else if (transaction.type == 3) {
			switch (transaction.subtype) {
			case 0:
			    transactionType = $.t("market_listing");
			    break;
			case 1:
			    transactionType = $.t("market_removal");
			    break;
			case 2:
			    transactionType = $.t("market_price_change");
			    break;
			case 3:
			    transactionType = $.t("market_quantity_change");
			    break;
			case 4:
			    transactionType = $.t("market_purchase");
			    break;
			case 5:
			    transactionType = $.t("market_delivery");
			    break;
			case 6:
			    transactionType = $.t("market_feedback");
			    break;
			case 7:
			    transactionType = $.t("market_refund");
			    break;
			}
		    }
                    else if (transaction.type == 4) {
			switch (transaction.subtype) {
			case 0:
			    transactionType = $.t("balance_leasing");
			    break;
			}
		    }
                    else if (transaction.type == 20) {
			switch (transaction.subtype) {
			case 0:
			    transactionType = "Reward Recipient Assignment";
			    break;
			}
		    }
                    else if (transaction.type == 21) {
			switch (transaction.subtype) {
			case 0:
			    transactionType = "Escrow Creation";
			    break;
			case 1:
			    transactionType = "Escrow Signing";
			    break;
			case 2:
			    transactionType = "Escrow Result";
			    break;
			case 3:
			    transactionType = "Subscription Subscribe";
			    break;
			case 4:
			    transactionType = "Subscription Cancel";
			    break;
			case 5:
			    transactionType = "Subscription Payment";
			    break;
			}
		    }
                    else if (transaction.type == 22) {
			switch (transaction.subtype) {
			case 0:
			    transactionType = "AT Creation";
			    break;
			case 1:
			    transactionType = "AT Payment";
			    break;
			}
		    }

		    if (/^BURST\-/i.test(BRS.userInfoModal.user)) {
			var receiving = (transaction.recipientRS == BRS.userInfoModal.user);
		    }
                    else {
			var receiving = (transaction.recipient == BRS.userInfoModal.user);
		    }

		    if (transaction.amountNQT) {
			transaction.amount = new BigInteger(transaction.amountNQT);
			transaction.fee = new BigInteger(transaction.feeNQT);
		    }

		    var account = (receiving ? "sender" : "recipient");

		    rows += "<tr><td>" + BRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fas fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fas fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + BRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + BRS.formatAmount(transaction.fee) + "</td><td>" + BRS.getAccountTitle(transaction, account) + "</td></tr>";
		}

		$("#user_info_modal_transactions_table tbody").empty().append(rows);
		BRS.dataLoadFinished($("#user_info_modal_transactions_table"));
	    }
            else {
		$("#user_info_modal_transactions_table tbody").empty();
		BRS.dataLoadFinished($("#user_info_modal_transactions_table"));
	    }
	});
    };

    BRS.userInfoModal.aliases = function() {
	BRS.sendRequest("getAliases", {
	    "account": BRS.userInfoModal.user,
	    "timestamp": 0
	}, function(response) {
	    var rows = "";

	    if (response.aliases && response.aliases.length) {
		var aliases = response.aliases;

		aliases.sort(function(a, b) {
		    if (a.aliasName.toLowerCase() > b.aliasName.toLowerCase()) {
			return 1;
		    }
                    else if (a.aliasName.toLowerCase() < b.aliasName.toLowerCase()) {
			return -1;
		    }
                    else {
			return 0;
		    }
		});

		var alias_account_count = 0,
		    alias_uri_count = 0,
		    empty_alias_count = 0,
		    alias_count = aliases.length;

		for (var i = 0; i < alias_count; i++) {
		    var alias = aliases[i];

		    rows += "<tr data-alias='" + String(alias.aliasName).toLowerCase().escapeHTML() + "'><td class='alias'>" + String(alias.aliasName).escapeHTML() + "</td><td class='uri'>" + (alias.aliasURI.indexOf("http") === 0 ? "<a href='" + String(alias.aliasURI).escapeHTML() + "' target='_blank'>" + String(alias.aliasURI).escapeHTML() + "</a>" : String(alias.aliasURI).escapeHTML()) + "</td></tr>";
		    if (!alias.uri) {
			empty_alias_count++;
		    }
                    else if (alias.aliasURI.indexOf("http") === 0) {
			alias_uri_count++;
		    }
                    else if (alias.aliasURI.indexOf("acct:") === 0 || alias.aliasURI.indexOf("nacc:") === 0) {
			alias_account_count++;
		    }
		}
	    }

	    $("#user_info_modal_aliases_table tbody").empty().append(rows);
	    BRS.dataLoadFinished($("#user_info_modal_aliases_table"));
	});
    };

    BRS.userInfoModal.marketplace = function() {
	BRS.sendRequest("getDGSGoods", {
	    "seller": BRS.userInfoModal.user,
	    "firstIndex": 0,
	    "lastIndex": 100
	}, function(response) {
	    var rows = "";

	    if (response.goods && response.goods.length) {
		for (var i = 0; i < response.goods.length; i++) {
		    var good = response.goods[i];
		    if (good.name.length > 150) {
			good.name = good.name.substring(0, 150) + "...";
		    }
		    rows += "<tr><td><a href='#' data-goto-goods='" + String(good.goods).escapeHTML() + "' data-seller='" + String(BRS.userInfoModal.user).escapeHTML() + "'>" + String(good.name).escapeHTML() + "</a></td><td>" + BRS.formatAmount(good.priceNQT) + " BURST</td><td>" + BRS.format(good.quantity) + "</td></tr>";
		}
	    }

	    $("#user_info_modal_marketplace_table tbody").empty().append(rows);
	    BRS.dataLoadFinished($("#user_info_modal_marketplace_table"));
	});
    };

    BRS.userInfoModal.assets = function() {
	BRS.sendRequest("getAccount", {
	    "account": BRS.userInfoModal.user
	}, function(response) {
	    if (response.assetBalances && response.assetBalances.length) {
		var assets = {};
		var nrAssets = 0;
		var ignoredAssets = 0;

		for (var i = 0; i < response.assetBalances.length; i++) {
		    if (response.assetBalances[i].balanceQNT == "0") {
			ignoredAssets++;

			if (nrAssets + ignoredAssets == response.assetBalances.length) {
			    BRS.userInfoModal.addIssuedAssets(assets);
			}
			continue;
		    }

		    BRS.sendRequest("getAsset", {
			"asset": response.assetBalances[i].asset,
			"_extra": {
			    "balanceQNT": response.assetBalances[i].balanceQNT
			}
		    }, function(asset, input) {
			asset.asset = input.asset;
			asset.balanceQNT = input["_extra"].balanceQNT;

			assets[asset.asset] = asset;
			nrAssets++;

			if (nrAssets + ignoredAssets == response.assetBalances.length) {
			    BRS.userInfoModal.addIssuedAssets(assets);
			}
		    });
		}
	    }
            else {
		BRS.userInfoModal.addIssuedAssets({});
	    }
	});
    };

    BRS.userInfoModal.addIssuedAssets = function(assets) {
	BRS.sendRequest("getAssetsByIssuer", {
	    "account": BRS.userInfoModal.user
	}, function(response) {
	    if (response.assets && response.assets[0] && response.assets[0].length) {
		$.each(response.assets[0], function(key, issuedAsset) {
		    if (assets[issuedAsset.asset]) {
			assets[issuedAsset.asset].issued = true;
		    }
                    else {
			issuedAsset.balanceQNT = "0";
			issuedAsset.issued = true;
			assets[issuedAsset.asset] = issuedAsset;
		    }
		});

		BRS.userInfoModal.assetsLoaded(assets);
	    }
            else if (!$.isEmptyObject(assets)) {
		BRS.userInfoModal.assetsLoaded(assets);
	    }
            else {
		$("#user_info_modal_assets_table tbody").empty();
		BRS.dataLoadFinished($("#user_info_modal_assets_table"));
	    }
	});
    };

    BRS.userInfoModal.assetsLoaded = function(assets) {
	var assetArray = [];
	var rows = "";

	$.each(assets, function(key, asset) {
	    assetArray.push(asset);
	});

	assetArray.sort(function(a, b) {
	    if (a.issued && b.issued) {
		if (a.name.toLowerCase() > b.name.toLowerCase()) {
		    return 1;
		}
                else if (a.name.toLowerCase() < b.name.toLowerCase()) {
		    return -1;
		}
                else {
		    return 0;
		}
	    }
            else if (a.issued) {
		return -1;
	    }
            else if (b.issued) {
		return 1;
	    }
            else {
		if (a.name.toLowerCase() > b.name.toLowerCase()) {
		    return 1;
		}
                else if (a.name.toLowerCase() < b.name.toLowerCase()) {
		    return -1;
		}
                else {
		    return 0;
		}
	    }
	});

	for (var i = 0; i < assetArray.length; i++) {
	    var asset = assetArray[i];

	    var percentageAsset = BRS.calculatePercentage(asset.balanceQNT, asset.quantityQNT);

	    rows += "<tr" + (asset.issued ? " class='asset_owner'" : "") + "><td><a href='#' data-goto-asset='" + String(asset.asset).escapeHTML() + "'" + (asset.issued ? " style='font-weight:bold'" : "") + ">" + String(asset.name).escapeHTML() + "</a></td><td class='quantity'>" + BRS.formatQuantity(asset.balanceQNT, asset.decimals) + "</td><td>" + BRS.formatQuantity(asset.quantityQNT, asset.decimals) + "</td><td>" + percentageAsset + "%</td></tr>";
	}

	$("#user_info_modal_assets_table tbody").empty().append(rows);

	BRS.dataLoadFinished($("#user_info_modal_assets_table"));
    };

    return BRS;
}(BRS || {}, jQuery));
