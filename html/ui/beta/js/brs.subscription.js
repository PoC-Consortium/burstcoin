/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.subscriptionPageType = null;

    BRS.pages.subscription = function() {
	BRS.sendRequest("getAccountSubscriptions", {
	    "account": BRS.account
	}, function(response) {
	    if(response.subscriptions && response.subscriptions.length) {
		var subscriptions = {};
		var rows = "";
		
		for(var i = 0; i < response.subscriptions.length; i++) {
		    rows += "<tr><td><a href='#' data-subscription='" + String(response.subscriptions[i].id).escapeHTML() + "'>" + String(response.subscriptions[i].id).escapeHTML() + "</a></td><td>" + String(response.subscriptions[i].senderRS).escapeHTML() + "</td><td>" + String(response.subscriptions[i].recipientRS).escapeHTML() + "</td><td>" + BRS.formatAmount(response.subscriptions[i].amountNQT) + "</td><td>" + response.subscriptions[i].frequency + "</td><td>" + BRS.formatTimestamp(response.subscriptions[i].timeNext) + "</td></tr>";
		}
	    }
	    BRS.dataLoaded(rows);
	});
    }

    return BRS;
}(BRS || {}, jQuery));
