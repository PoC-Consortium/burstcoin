/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	$("#token_modal").on("show.bs.modal", function(e) {
		$("#generate_token_output, #decode_token_output").html("").hide();

		$("#token_modal_generate_token").show();
		$("#token_modal_button").text($.t("generate")).data("form", "generate_token_form");
	});

	NRS.forms.generateToken = function($modal) {
		var data = $.trim($("#generate_token_data").val());

		if (!data) {
			return {
				"error": "Data is a required field."
			};
			$("#generate_token_output").html("").hide();
		} else {
			return {};
		}
	}

	NRS.forms.generateTokenComplete = function(response, data) {
		$("#token_modal").find(".error_message").hide();

		if (response.token) {
			$("#generate_token_output").html($.t("generated_token_is") + "<br /><br /><textarea style='width:100%' rows='3'>" + String(response.token).escapeHTML() + "</textarea>").show();
		} else {
			$.notify($.t("error_generate_token"), {
				"type": "danger"
			});
			$("#generate_token_modal").modal("hide");
		}
	}

	NRS.forms.generateTokenError = function() {
		$("#generate_token_output").hide();
	}

	NRS.forms.decodeTokenComplete = function(response, data) {
		$("#token_modal").find(".error_message").hide();

		if (response.valid) {
			$("#decode_token_output").html($.t("success_valid_token", {
				"account_link": NRS.getAccountLink(response, "account"),
				"timestamp": NRS.formatTimestamp(response.timestamp)
			})).addClass("callout-info").removeClass("callout-danger").show();
		} else {
			$("#decode_token_output").html($.t("error_invalid_token", {
				"account_link": NRS.getAccountLink(response, "account"),
				"timestamp": NRS.formatTimestamp(response.timestamp)
			})).addClass("callout-danger").removeClass("callout-info").show();
		}
	}

	NRS.forms.decodeTokenError = function() {
		$("#decode_token_output").hide();
	}

	$("#token_modal ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		$(this).siblings().removeClass("active");
		$(this).addClass("active");

		$(".token_modal_content").hide();

		var content = $("#token_modal_" + tab);

		if (tab == "generate_token") {
			$("#token_modal_button").text($.t("generate")).data("form", "generate_token_form");
		} else {
			$("#token_modal_button").text($.t("validate")).data("form", "validate_token_form");
		}

		$("#token_modal .error_message").hide();

		content.show();
	});

	$("#token_modal").on("hidden.bs.modal", function(e) {
		$(this).find(".token_modal_content").hide();
		$(this).find("ul.nav li.active").removeClass("active");
		$("#generate_token_nav").addClass("active");
	});

	return NRS;
}(NRS || {}, jQuery));