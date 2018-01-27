/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	//todo: use a startForgingError function instaed!

	NRS.forms.startForgingComplete = function(response, data) {
		if ("deadline" in response) {
			$("#forging_indicator").addClass("forging");
			$("#forging_indicator span").html($.t("forging")).attr("data-i18n", "forging");
			NRS.isForging = true;
			$.growl($.t("success_start_forging"), {
				type: "success"
			});
		} else {
			NRS.isForging = false;
			$.growl($.t("error_start_forging"), {
				type: 'danger'
			});
		}
	}

	NRS.forms.stopForgingComplete = function(response, data) {
		if ($("#stop_forging_modal .show_logout").css("display") == "inline") {
			NRS.logout();
			return;
		}

		$("#forging_indicator").removeClass("forging");
		$("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");

		NRS.isForging = false;

		if (response.foundAndStopped) {
			$.growl($.t("success_stop_forging"), {
				type: 'success'
			});
		} else {
			$.growl($.t("error_stop_forging"), {
				type: 'danger'
			});
		}
	}

	$("#forging_indicator").click(function(e) {
		e.preventDefault();

		if (NRS.downloadingBlockchain) {
			$.growl($.t("error_forging_blockchain_downloading"), {
				"type": "danger"
			});
		} else if (NRS.state.isScanning) {
			$.growl($.t("error_forging_blockchain_rescanning"), {
				"type": "danger"
			});
		} else if (!NRS.accountInfo.publicKey) {
			$.growl($.t("error_forging_no_public_key"), {
				"type": "danger"
			});
		} else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
			if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo) {
				$.growl($.t("error_forging_lease"), {
					"type": "danger"
				});
			} else {
				$.growl($.t("error_forging_effective_balance"), {
					"type": "danger"
				});
			}
		} else if ($(this).hasClass("forging")) {
			$("#stop_forging_modal").modal("show");
		} else {
			$("#start_forging_modal").modal("show");
		}
	});

	return NRS;
}(NRS || {}, jQuery));