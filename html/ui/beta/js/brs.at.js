/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.atPageType = null;

    BRS.pages.at = function() {
	BRS.sendRequest("getAccountATs", {
	    "account": BRS.account
	}, function(response) {
	    if(response.ats && response.ats.length) {
		var ats = {};
		var rows = "";
		
		for(var i = 0; i < response.ats.length; i++) {
		    rows += "<tr><td>" + String(response.ats[i].atRS).escapeHTML() + "</td><td>" + String(response.ats[i].name).escapeHTML() + "</td><td>" + String(response.ats[i].description).escapeHTML() + "</td><td>" + BRS.formatAmount(response.ats[i].balanceNQT) + "</td></tr>";
		}
	    }
	    BRS.dataLoaded(rows);
	});
    }

    return BRS;
}(BRS || {}, jQuery));
