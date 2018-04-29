/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.fetchingModalData = false;

    // save the original function object
    var _superModal = $.fn.modal;

    // add locked as a new option
    $.extend(_superModal.Constructor.DEFAULTS, {
	locked: false
    });

    // capture the original hide
    var _hide = _superModal.Constructor.prototype.hide;

    // add the lock, unlock and override the hide of modal
    $.extend(_superModal.Constructor.prototype, {
	// locks the dialog so that it cannot be hidden
	lock: function() {
	    this.options.locked = true;
	    this.$element.addClass("locked");
	}
	// unlocks the dialog so that it can be hidden by 'esc' or clicking on the backdrop (if not static)
	,
	unlock: function() {
	    this.options.locked = false;
	    this.$element.removeClass("locked");
	},
	// override the original hide so that the original is only called if the modal is unlocked
	hide: function() {
	    if (this.options.locked) return;

	    _hide.apply(this, arguments);
	}
    });

    //Reset scroll position of tab when shown.
    $('a[data-toggle="tab"]').on("shown.bs.tab", function(e) {
	var target = $(e.target).attr("href");
	$(target).scrollTop(0);
    })

    $(".add_message").on("change", function(e) {
	if ($(this).is(":checked")) {
	    $(this).closest("form").find(".optional_message").fadeIn();
	    $(this).closest(".form-group").css("margin-bottom", "5px");
	}
        else {
	    $(this).closest("form").find(".optional_message").hide();
	    $(this).closest(".form-group").css("margin-bottom", "");
	}
    });

    $(".add_note_to_self").on("change", function(e) {
	if ($(this).is(":checked")) {
	    $(this).closest("form").find(".optional_note").fadeIn();
	}
        else {
	    $(this).closest("form").find(".optional_note").hide();
	}
    });

    //hide modal when another one is activated.
    $(".modal").on("show.bs.modal", function(e) {
	var $inputFields = $(this).find("input[name=recipient], input[name=account_id]").not("[type=hidden]");

	$.each($inputFields, function() {
	    if ($(this).hasClass("noMask")) {
		$(this).mask("BURST-****-****-****-*****", {
		    "noMask": true
		}).removeClass("noMask");
	    }
            else {
		$(this).mask("BURST-****-****-****-*****");
	    }
	});

	var $visible_modal = $(".modal.in");

	if ($visible_modal.length) {
	    if ($visible_modal.hasClass("locked")) {
		var $btn = $visible_modal.find("button.btn-primary:not([data-dismiss=modal])");
		BRS.unlockForm($visible_modal, $btn, true);
	    }
            else {
		$visible_modal.modal("hide");
	    }
	}

	$(this).find(".form-group").css("margin-bottom", "");
    });

    $(".modal").on("shown.bs.modal", function() {
	$(this).find("input[type=text]:first, textarea:first, input[type=password]:first").not("[readonly]").first().focus();
	$(this).find("input[name=converted_account_id]").val("");
	BRS.showedFormWarning = false; //maybe not the best place... we assume forms are only in modals?
    });

    //Reset form to initial state when modal is closed
    $(".modal").on("hidden.bs.modal", function(e) {
	$(this).find("input[name=recipient], input[name=account_id]").not("[type=hidden]").trigger("unmask");

	$(this).find(":input:not(button)").each(function(index) {
	    var defaultValue = $(this).data("default");
	    var type = $(this).attr("type");
	    var tag = $(this).prop("tagName").toLowerCase();

	    if (type == "checkbox") {
		if (defaultValue == "checked") {
		    $(this).prop("checked", true);
		}
                else {
		    $(this).prop("checked", false);
		}
	    }
            else if (type == "hidden") {
		if (defaultValue !== undefined) {
		    $(this).val(defaultValue);
		}
	    }
            else if (tag == "select") {
		if (defaultValue !== undefined) {
		    $(this).val(defaultValue);
		}
                else {
		    $(this).find("option:selected").prop("selected", false);
		    $(this).find("option:first").prop("selected", "selected");
		}
	    }
            else {
		if (defaultValue !== undefined) {
		    $(this).val(defaultValue);
		}
                else {
		    $(this).val("");
		}
	    }
	});

	//Hidden form field
	$(this).find("input[name=converted_account_id]").val("");

	//Hide/Reset any possible error messages
	$(this).find(".callout-danger:not(.never_hide), .error_message, .account_info").html("").hide();

	$(this).find(".advanced").hide();

	$(this).find(".recipient_public_key").hide();

	$(this).find(".optional_message, .optional_note").hide();

	$(this).find(".advanced_info a").text($.t("advanced"));

	$(this).find(".advanced_extend").each(function(index, obj) {
	    var normalSize = $(obj).data("normal");
	    var advancedSize = $(obj).data("advanced");
	    $(obj).removeClass("col-xs-" + advancedSize + " col-sm-" + advancedSize + " col-md-" + advancedSize).addClass("col-xs-" + normalSize + " col-sm-" + normalSize + " col-md-" + normalSize);
	});

	var $feeInput = $(this).find("input[name=feeNXT]");

	if ($feeInput.length) {
	    var defaultFee = $feeInput.data("default");
	    if (!defaultFee) {
		defaultFee = 1;
	    }

	    $(this).find(".advanced_fee").html(BRS.formatAmount(BRS.convertToNQT(defaultFee)) + " BURST");
	}

	BRS.showedFormWarning = false;
    });

    BRS.showModalError = function(errorMessage, $modal) {
	var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");

	$modal.find("button").prop("disabled", false);

	$modal.find(".error_message").html(String(errorMessage).escapeHTML()).show();
	$btn.button("reset");
	$modal.modal("unlock");
    }

    BRS.closeModal = function($modal) {
	if (!$modal) {
	    $modal = $("div.modal.in:first");
	}

	$modal.find("button").prop("disabled", false);

	var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");

	$btn.button("reset");
	$modal.modal("unlock");
	$modal.modal("hide");
    }

    $("input[name=feeNXT]").on("change", function() {
	var $modal = $(this).closest(".modal");

	var $feeInfo = $modal.find(".advanced_fee");

	if ($feeInfo.length) {
	    $feeInfo.html(BRS.formatAmount(BRS.convertToNQT($(this).val())) + " BURST");
	}
    });

    $(".advanced_info a").on("click", function(e) {
	e.preventDefault();

	var $modal = $(this).closest(".modal");

	var text = $(this).text().toLowerCase();

	if (text == $.t("advanced")) {
	    var not = ".optional_note";
	    $modal.find(".advanced").not(not).fadeIn();
	}
        else {
	    $modal.find(".advanced").hide();
	}

	$modal.find(".advanced_extend").each(function(index, obj) {
	    var normalSize = $(obj).data("normal");
	    var advancedSize = $(obj).data("advanced");

	    if (text == "advanced") {
		$(obj).addClass("col-xs-" + advancedSize + " col-sm-" + advancedSize + " col-md-" + advancedSize).removeClass("col-xs-" + normalSize + " col-sm-" + normalSize + " col-md-" + normalSize);
	    }
            else {
		$(obj).removeClass("col-xs-" + advancedSize + " col-sm-" + advancedSize + " col-md-" + advancedSize).addClass("col-xs-" + normalSize + " col-sm-" + normalSize + " col-md-" + normalSize);
	    }
	});

	if (text == $.t("advanced")) {
	    $(this).text($.t("basic"));
	}
        else {
	    $(this).text($.t("advanced"));
	}
    });

    return BRS;
}(BRS || {}, jQuery));
