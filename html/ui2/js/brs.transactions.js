/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.lastTransactions = "";

    BRS.unconfirmedTransactions = [];
    BRS.unconfirmedTransactionIds = "";
    BRS.unconfirmedTransactionsChange = true;

    BRS.transactionsPageType = null;

    BRS.getInitialTransactions = function() {
	BRS.sendRequest("getAccountTransactions", {
	    "account": BRS.account,
	    "firstIndex": 0,
	    "lastIndex": 9
	}, function(response) {
	    if (response.transactions && response.transactions.length) {
		var transactions = [];
		var transactionIds = [];

		for (var i = 0; i < response.transactions.length; i++) {
		    var transaction = response.transactions[i];

		    transaction.confirmed = true;
		    transactions.push(transaction);

		    transactionIds.push(transaction.transaction);
		}

		BRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
		    BRS.handleInitialTransactions(transactions.concat(unconfirmedTransactions), transactionIds);
		});
	    }
            else {
		BRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
		    BRS.handleInitialTransactions(unconfirmedTransactions, []);
		});
	    }
	});
    }

    BRS.handleInitialTransactions = function(transactions, transactionIds) {
	if (transactions.length) {
	    var rows = "";

	    transactions.sort(BRS.sortArray);

	    if (transactionIds.length) {
		BRS.lastTransactions = transactionIds.toString();
	    }

	    for (var i = 0; i < transactions.length; i++) {
		var transaction = transactions[i];

		var receiving = transaction.recipient == BRS.account;

		var account = (receiving ? "sender" : "recipient");

		if (transaction.amountNQT) {
		    transaction.amount = new BigInteger(transaction.amountNQT);
		    transaction.fee = new BigInteger(transaction.feeNQT);
		}

		rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "' data-timestamp='" + String(transaction.timestamp).escapeHTML() + "'>" + BRS.formatTimestamp(transaction.timestamp) + "</a></td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fas fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fas fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + BRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + BRS.formatAmount(transaction.fee) + "</span></td><td>" + BRS.getAccountLink(transaction, account) + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-content='" + BRS.formatAmount(transaction.confirmations) + " confirmations' data-container='body' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
	    }

	    $("#dashboard_transactions_table tbody").empty().append(rows);
	}

	BRS.dataLoadFinished($("#dashboard_transactions_table"));
    }

    BRS.getNewTransactions = function() {
	//check if there is a new transaction..
	BRS.sendRequest("getAccountTransactionIds", {
	    "account": BRS.account,
	    "timestamp": BRS.blocks[0].timestamp + 1,
	    "firstIndex": 0,
	    "lastIndex": 0
	}, function(response) {
	    //if there is, get latest 10 transactions
	    if (response.transactionIds && response.transactionIds.length) {
		BRS.sendRequest("getAccountTransactions", {
		    "account": BRS.account,
		    "firstIndex": 0,
		    "lastIndex": 9
		}, function(response) {
		    if (response.transactions && response.transactions.length) {
			var transactionIds = [];

			$.each(response.transactions, function(key, transaction) {
			    transactionIds.push(transaction.transaction);
			    response.transactions[key].confirmed = true;
			});

			BRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
			    BRS.handleIncomingTransactions(response.transactions.concat(unconfirmedTransactions), transactionIds);
			});
		    }
                    else {
			BRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
			    BRS.handleIncomingTransactions(unconfirmedTransactions);
			});
		    }
		});
	    }
            else {
		BRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
		    BRS.handleIncomingTransactions(unconfirmedTransactions);
		});
	    }
	});
    }

    BRS.getUnconfirmedTransactions = function(callback) {
	BRS.sendRequest("getUnconfirmedTransactions", {
	    "account": BRS.account
	}, function(response) {
	    if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
		var unconfirmedTransactions = [];
		var unconfirmedTransactionIds = [];

		response.unconfirmedTransactions.sort(function(x, y) {
		    if (x.timestamp < y.timestamp) {
			return 1;
		    }
                    else if (x.timestamp > y.timestamp) {
			return -1;
		    }
                    else {
			return 0;
		    }
		});

		for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
		    var unconfirmedTransaction = response.unconfirmedTransactions[i];

		    unconfirmedTransaction.confirmed = false;
		    unconfirmedTransaction.unconfirmed = true;
		    unconfirmedTransaction.confirmations = "/";

		    if (unconfirmedTransaction.attachment) {
			for (var key in unconfirmedTransaction.attachment) {
			    if (!unconfirmedTransaction.hasOwnProperty(key)) {
				unconfirmedTransaction[key] = unconfirmedTransaction.attachment[key];
			    }
			}
		    }

		    unconfirmedTransactions.push(unconfirmedTransaction);
		    unconfirmedTransactionIds.push(unconfirmedTransaction.transaction);
		}

		BRS.unconfirmedTransactions = unconfirmedTransactions;

		var unconfirmedTransactionIdString = unconfirmedTransactionIds.toString();

		if (unconfirmedTransactionIdString != BRS.unconfirmedTransactionIds) {
		    BRS.unconfirmedTransactionsChange = true;
		    BRS.unconfirmedTransactionIds = unconfirmedTransactionIdString;
		}
                else {
		    BRS.unconfirmedTransactionsChange = false;
		}

		if (callback) {
		    callback(unconfirmedTransactions);
		}
                else if (BRS.unconfirmedTransactionsChange) {
		    BRS.incoming.updateDashboardTransactions(unconfirmedTransactions, true);
		}
	    }
            else {
		BRS.unconfirmedTransactions = [];

		if (BRS.unconfirmedTransactionIds) {
		    BRS.unconfirmedTransactionsChange = true;
		}
                else {
		    BRS.unconfirmedTransactionsChange = false;
		}

		BRS.unconfirmedTransactionIds = "";

		if (callback) {
		    callback([]);
		}
                else if (BRS.unconfirmedTransactionsChange) {
		    BRS.incoming.updateDashboardTransactions([], true);
		}
	    }
	});
    }

    BRS.handleIncomingTransactions = function(transactions, confirmedTransactionIds) {
	var oldBlock = (confirmedTransactionIds === false); //we pass false instead of an [] in case there is no new block..

	if (typeof confirmedTransactionIds != "object") {
	    confirmedTransactionIds = [];
	}

	if (confirmedTransactionIds.length) {
	    BRS.lastTransactions = confirmedTransactionIds.toString();
	}

	if (confirmedTransactionIds.length || BRS.unconfirmedTransactionsChange) {
	    transactions.sort(BRS.sortArray);

	    BRS.incoming.updateDashboardTransactions(transactions, confirmedTransactionIds.length == 0);
	}

	//always refresh peers and unconfirmed transactions..
	if (BRS.currentPage == "peers") {
	    BRS.incoming.peers();
	}
        else if (BRS.currentPage == "transactions" && BRS.transactionsPageType == "unconfirmed") {
	    BRS.incoming.transactions();
	}
        else {
	    if (BRS.currentPage != 'messages' && (!oldBlock || BRS.unconfirmedTransactionsChange)) {
		if (BRS.incoming[BRS.currentPage]) {
		    BRS.incoming[BRS.currentPage](transactions);
		}
	    }
	}
	// always call incoming for messages to enable message notifications
	if (!oldBlock || BRS.unconfirmedTransactionsChange) {
	    BRS.incoming['messages'](transactions);
	}
    }

    BRS.sortArray = function(a, b) {
	return b.timestamp - a.timestamp;
    }

    BRS.incoming.updateDashboardTransactions = function(newTransactions, unconfirmed) {
	var newTransactionCount = newTransactions.length;

	if (newTransactionCount) {
	    var rows = "";

	    var onlyUnconfirmed = true;

	    for (var i = 0; i < newTransactionCount; i++) {
		var transaction = newTransactions[i];

		var receiving = transaction.recipient == BRS.account;
		var account = (receiving ? "sender" : "recipient");

		if (transaction.confirmed) {
		    onlyUnconfirmed = false;
		}

		if (transaction.amountNQT) {
		    transaction.amount = new BigInteger(transaction.amountNQT);
		    transaction.fee = new BigInteger(transaction.feeNQT);
		}

		rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "' data-timestamp='" + String(transaction.timestamp).escapeHTML() + "'>" + BRS.formatTimestamp(transaction.timestamp) + "</a></td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fas fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fas fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + BRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + BRS.formatAmount(transaction.fee) + "</span></td><td>" + BRS.getAccountLink(transaction, account) + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-content='" + (transaction.confirmed ? BRS.formatAmount(transaction.confirmations) + " " + $.t("confirmations") : $.t("unconfirmed_transaction")) + "' data-container='body' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
	    }

	    if (onlyUnconfirmed) {
		$("#dashboard_transactions_table tbody tr.tentative").remove();
		$("#dashboard_transactions_table tbody").prepend(rows);
	    }
            else {
		$("#dashboard_transactions_table tbody").empty().append(rows);
	    }

	    var $parent = $("#dashboard_transactions_table").parent();

	    if ($parent.hasClass("data-empty")) {
		$parent.removeClass("data-empty");
		if ($parent.data("no-padding")) {
		    $parent.parent().addClass("no-padding");
		}
	    }
	}
        else if (unconfirmed) {
	    $("#dashboard_transactions_table tbody tr.tentative").remove();
	}
    }

    //todo: add to dashboard? 
    BRS.addUnconfirmedTransaction = function(transactionId, callback) {
	BRS.sendRequest("getTransaction", {
	    "transaction": transactionId
	}, function(response) {
	    if (!response.errorCode) {
		response.transaction = transactionId;
		response.confirmations = "/";
		response.confirmed = false;
		response.unconfirmed = true;

		if (response.attachment) {
		    for (var key in response.attachment) {
			if (!response.hasOwnProperty(key)) {
			    response[key] = response.attachment[key];
			}
		    }
		}

		var alreadyProcessed = false;

		try {
		    var regex = new RegExp("(^|,)" + transactionId + "(,|$)");

		    if (regex.exec(BRS.lastTransactions)) {
			alreadyProcessed = true;
		    }
                    else {
			$.each(BRS.unconfirmedTransactions, function(key, unconfirmedTransaction) {
			    if (unconfirmedTransaction.transaction == transactionId) {
				alreadyProcessed = true;
				return false;
			    }
			});
		    }
		} catch (e) {}

		if (!alreadyProcessed) {
		    BRS.unconfirmedTransactions.unshift(response);
		}

		if (callback) {
		    callback(alreadyProcessed);
		}

		BRS.incoming.updateDashboardTransactions(BRS.unconfirmedTransactions, true);

		BRS.getAccountInfo();
	    }
            else if (callback) {
		callback(false);
	    }
	});
    }

    BRS.pages.transactions = function() {
	if (BRS.transactionsPageType == "unconfirmed") {
	    BRS.displayUnconfirmedTransactions();
	    return;
	}

	var rows = "";

	var params = {
	    "account": BRS.account,
	    "firstIndex": 0,
	    "lastIndex": 100
	};

	if (BRS.transactionsPageType) {
	    params.type = BRS.transactionsPageType.type;
	    params.subtype = BRS.transactionsPageType.subtype;
	    var unconfirmedTransactions = BRS.getUnconfirmedTransactionsFromCache(params.type, params.subtype);
	}
        else {
	    var unconfirmedTransactions = BRS.unconfirmedTransactions;
	}

	if (unconfirmedTransactions) {
	    for (var i = 0; i < unconfirmedTransactions.length; i++) {
		rows += BRS.getTransactionRowHTML(unconfirmedTransactions[i]);
	    }
	}

	BRS.sendRequest("getAccountTransactions+", params, function(response) {
	    if (response.transactions && response.transactions.length) {
		for (var i = 0; i < response.transactions.length; i++) {
		    var transaction = response.transactions[i];

		    transaction.confirmed = true;

		    rows += BRS.getTransactionRowHTML(transaction);
		}

		BRS.dataLoaded(rows);
	    }
            else {
		BRS.dataLoaded(rows);
	    }
	});
    }

    BRS.incoming.transactions = function(transactions) {
	BRS.loadPage("transactions");
    }

    BRS.displayUnconfirmedTransactions = function() {
	BRS.sendRequest("getUnconfirmedTransactions", function(response) {
	    var rows = "";

	    if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
		for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
		    rows += BRS.getTransactionRowHTML(response.unconfirmedTransactions[i]);
		}
	    }

	    BRS.dataLoaded(rows);
	});
    }

    BRS.getTransactionRowHTML = function(transaction) {
	var transactionType = $.t("unknown");

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
		transactionType = $.t("hub_announcements");
		break;
	    case 5:
		transactionType = $.t("account_info");
		break;
	    case 6:
		if (transaction.attachment.priceNQT == "0") {
		    if (transaction.sender == BRS.account && transaction.recipient == BRS.account) {
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
		transactionType = $.t("marketplace_listing");
		break;
	    case 1:
		transactionType = $.t("marketplace_removal");
		break;
	    case 2:
		transactionType = $.t("marketplace_price_change");
		break;
	    case 3:
		transactionType = $.t("marketplace_quantity_change");
		break;
	    case 4:
		transactionType = $.t("marketplace_purchase");
		break;
	    case 5:
		transactionType = $.t("marketplace_delivery");
		break;
	    case 6:
		transactionType = $.t("marketplace_feedback");
		break;
	    case 7:
		transactionType = $.t("marketplace_refund");
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

	var receiving = transaction.recipient == BRS.account;
	var account = (receiving ? "sender" : "recipient");

	if (transaction.amountNQT) {
	    transaction.amount = new BigInteger(transaction.amountNQT);
	    transaction.fee = new BigInteger(transaction.feeNQT);
	}

	var hasMessage = false;

	if (transaction.attachment) {
	    if (transaction.attachment.encryptedMessage || transaction.attachment.message) {
		hasMessage = true;
	    }
            else if (transaction.sender == BRS.account && transaction.attachment.encryptToSelfMessage) {
		hasMessage = true;
	    }
	}

	return "<tr " + (!transaction.confirmed && (transaction.recipient == BRS.account || transaction.sender == BRS.account) ? " class='tentative'" : "") + "><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "'>" + String(transaction.transaction).escapeHTML() + "</a></td><td>" + (hasMessage ? "<i class='far fa-envelope-open'></i>&nbsp;" : "/") + "</td><td>" + BRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fas fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fas fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + BRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + BRS.formatAmount(transaction.fee) + "</td><td>" + BRS.getAccountLink(transaction, account) + "</td><td class='confirmations' data-content='" + (transaction.confirmed ? BRS.formatAmount(transaction.confirmations) + " " + $.t("confirmations") : $.t("unconfirmed_transaction")) + "' data-container='body' data-placement='left'>" + (!transaction.confirmed ? "/" : (transaction.confirmations > 1440 ? "1440+" : BRS.formatAmount(transaction.confirmations))) + "</td></tr>";
    }

    $("#transactions_page_type li a").click(function(e) {
	e.preventDefault();

	var type = $(this).data("type");

	if (!type) {
	    BRS.transactionsPageType = null;
	}
        else if (type == "unconfirmed") {
	    BRS.transactionsPageType = "unconfirmed";
	}
        else {
	    type = type.split(":");
	    BRS.transactionsPageType = {
		"type": type[0],
		"subtype": type[1]
	    };
	}

	$(this).parents(".btn-group").find(".text").text($(this).text());

	$(".popover").remove();

	BRS.loadPage("transactions");
    });

    return BRS;
}(BRS || {}, jQuery));
