/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.escrowPageType = null;

    BRS.pages.escrow = function() {
        BRS.sendRequest("getAccountEscrowTransactions", {
            "account": BRS.account
        }, function(response) {
            if (response.escrows && response.escrows.length) {
                var escrows = {};
                var rows = "";

                for (var i = 0; i < response.escrows.length; i++) {
                    rows += "<tr><td><a href='#' data-escrow='" + String(response.escrows[i].id).escapeHTML() + "'>" + String(response.escrows[i].id).escapeHTML() + "</a></td><td>" + String(response.escrows[i].senderRS).escapeHTML() + "</td><td>" + String(response.escrows[i].recipientRS).escapeHTML() + "</td><td>" + BRS.formatAmount(response.escrows[i].amountNQT) + "</td</tr>";
                }
            }
            BRS.dataLoaded(rows);
        });
    }

    return BRS;
}(BRS || {}, jQuery));
