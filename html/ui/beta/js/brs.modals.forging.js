/**
 * @depends {brs.js}
 * @depends {brs.modals.js}
 */
var BRS = (function(BRS, $, undefined) {
    //todo: use a startForgingError function instaed!

    BRS.forms.startForgingComplete = function(response, data) {
	if ("deadline" in response) {
	    $("#forging_indicator").addClass("forging");
	    $("#forging_indicator span").html($.t("forging")).attr("data-i18n", "forging");
	    BRS.isForging = true;
	    $.notify($.t("success_start_forging"), {
		type: "success"
	    });
	}
        else {
	    BRS.isForging = false;
	    $.notify($.t("error_start_forging"), {
		type: 'danger'
	    });
	}
    }

    BRS.forms.stopForgingComplete = function(response, data) {
	if ($("#stop_forging_modal .show_logout").css("display") == "inline") {
	    BRS.logout();
	    return;
	}

	$("#forging_indicator").removeClass("forging");
	$("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");

	BRS.isForging = false;

	if (response.foundAndStopped) {
	    $.notify($.t("success_stop_forging"), {
		type: 'success'
	    });
	}
        else {
	    $.notify($.t("error_stop_forging"), {
		type: 'danger'
	    });
	}
    }

    $("#forging_indicator").click(function(e) {
	e.preventDefault();

	if (BRS.downloadingBlockchain) {
	    $.notify($.t("error_forging_blockchain_downloading"), {
		"type": "danger"
	    });
	}
        else if (BRS.state.isScanning) {
	    $.notify($.t("error_forging_blockchain_rescanning"), {
		"type": "danger"
	    });
	}
        else if (!BRS.accountInfo.publicKey) {
	    $.notify($.t("error_forging_no_public_key"), {
		"type": "danger"
	    });
	}
        else if (BRS.accountInfo.effectiveBalanceBURST == 0) {
	    if (BRS.lastBlockHeight >= BRS.accountInfo.currentLeasingHeightFrom && BRS.lastBlockHeight <= BRS.accountInfo.currentLeasingHeightTo) {
		$.notify($.t("error_forging_lease"), {
		    "type": "danger"
		});
	    }
            else {
		$.notify($.t("error_forging_effective_balance"), {
		    "type": "danger"
		});
	    }
	}
        else if ($(this).hasClass("forging")) {
	    $("#stop_forging_modal").modal("show");
	}
        else {
	    $("#start_forging_modal").modal("show");
	}
    });

    return BRS;
}(BRS || {}, jQuery));
