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
    });

    // hide multi-out
    $(".hide").hide();
    $(".multi-out").hide();
    $(".multi-out-same").hide();
    $(".multi-out-recipients").append($("#additional_multi_out_recipient").html());
    $(".multi-out-recipients").append($("#additional_multi_out_recipient").html());
    $(".multi-out-same-recipients").append($("#additional_multi_out_same_recipient").html());
    $(".multi-out-same-recipients").append($("#additional_multi_out_same_recipient").html());
    $(".multi-out .remove_recipient").each(function() {
        $(this).remove();
    });

    // just to be safe set total display
    var current_fee = parseFloat($("#multi-out-fee").val(), 10);
    var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);
    $("#multi-out-fee").val(fee);
    var total_multi_out = fee;
    var amount_total = 0;

    $(".ordinary-nav a").on("click", function(e) {
        $(".multi-out").hide();
        $(".ordinary").fadeIn();
        if (!$(".ordinary-nav").hasClass("active")) {
            $(".ordinary-nav").addClass("active");
        }
        if ($(".multi-out-nav").toggleClass("active")) {
            $(".multi-out-nav").removeClass("active");
        }
    });

    $(".multi-out-nav a").on("click", function(e) {
        $(".ordinary").hide();
        $(".multi-out").fadeIn();
        if ($(".ordinary-nav").hasClass("active")) {
            $(".ordinary-nav").removeClass("active");
        }
        if (!$(".multi-out-nav").hasClass("active")) {
            $(".multi-out-nav").addClass("active");
        }
    });

    // multi-out inputs
    var multi_out_recipients = 2;
    var multi_out_same_recipients = 2;
    $(".add_recipients").on("click", function(e) {
        e.preventDefault();
        if ($(".same_out_checkbox").is(":checked")) {
            if (multi_out_same_recipients < 128) {
                multi_out_same_recipients++;
                $(".multi-out-same-recipients").append($("#additional_multi_out_same_recipient").html()); //add input box

                total_multi_out = 0;
                amount_total = 0;
                var current_amount = parseFloat($("#multi-out-same-amount").val(), 10);
                var current_fee = parseFloat($("#multi-out-fee").val(), 10);

                var amount = isNaN(current_amount) ? 0.00000001 : (current_amount < 0.00000001 ? 0.00000001 : current_amount);
                var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);

                $("#multi-out-same-amount").val(amount.toFixed(8));
                $("#multi-out-fee").val(fee.toFixed(8));

                $(".multi-out-same-recipients .multi-out-recipient").each(function() {
                    amount_total += amount;
                });

                total_multi_out = amount_total + fee;

                $(".total_amount_multi_out").html(BRS.formatAmount(BRS.convertToNQT(total_multi_out)) + " BURST");
            }
        } else {
            if (multi_out_recipients < 64) {
                multi_out_recipients++;
                $(".multi-out-recipients").append($("#additional_multi_out_recipient").html()); //add input box
            }
        }
    });

    $(document).on("click", ".remove_recipient .remove_recipient_button", function(e) {
        e.preventDefault();
        $(this).parent().parent('div').remove();
        multi_out_recipients--;

        if ($(".same_out_checkbox").is(":checked")) {
            multi_out_same_recipients--;

            total_multi_out = 0;
            amount_total = 0;
            var current_amount = parseFloat($("#multi-out-same-amount").val(), 10);
            var current_fee = parseFloat($("#multi-out-fee").val(), 10);

            var amount = isNaN(current_amount) ? 0.00000001 : (current_amount < 0.00000001 ? 0.00000001 : current_amount);
            var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);

            $("#multi-out-same-amount").val(amount.toFixed(8));
            $("#multi-out-fee").val(fee.toFixed(8));

            $(".multi-out-same-recipients .multi-out-recipient").each(function() {
                amount_total += amount;
            });

            total_multi_out = amount_total + fee;

            $(".total_amount_multi_out").html(BRS.formatAmount(BRS.convertToNQT(total_multi_out)) + " BURST");
        } else {
            multi_out_recipients--;
            // get amount for each recipient
            total_multi_out = 0;
            amount_total = 0;
            $(".multi-out .multi-out-amount").each(function() {
                var current_amount = parseFloat($(this).val(), 10);
                var amount = isNaN(current_amount) ? 0.00000001 : (current_amount < 0.00000001 ? 0.00000001 : current_amount);
                $(this).val(amount.toFixed(8));
                amount_total += amount;
            });

            var current_fee = parseFloat($("#multi-out-fee").val(), 10);
            var fee = isNaN(fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);
            $("#multi-out-fee").val(fee.toFixed(8));
            total_multi_out = amount_total + fee;

            $(".total_amount_multi_out").html(BRS.formatAmount(BRS.convertToNQT(total_multi_out)) + " BURST");
        }
    });

    $(document).on("change remove", ".multi-out-amount", function(e) {
        // get amount for each recipient
        total_multi_out = 0;
        amount_total = 0;
        $(".multi-out .multi-out-amount").each(function() {
            var current_amount = parseFloat($(this).val(), 10);
            var amount = isNaN(current_amount) ? 0.00000001 : (current_amount < 0.00000001 ? 0.00000001 : current_amount);
            $(this).val(amount.toFixed(8));
            amount_total += amount;
        });

        var current_fee = parseFloat($("#multi-out-fee").val(), 10);
        var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);
        $("#multi-out-fee").val(fee.toFixed(8));
        total_multi_out = amount_total + fee;

        $(".total_amount_multi_out").html(BRS.formatAmount(BRS.convertToNQT(total_multi_out)) + " BURST");
    });

    $("#multi-out-same-amount").on("change", function(e) {
        total_multi_out = 0;
        amount_total = 0;
        var current_amount = parseFloat($(this).val(), 10);
        var current_fee = parseFloat($("#multi-out-fee").val(), 10);

        var amount = isNaN(current_amount) ? 0.00000001 : (current_amount < 0.00000001 ? 0.00000001 : current_amount);
        var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);

        $("#multi-out-same-amount").val(amount.toFixed(8));
        $("#multi-out-fee").val(fee.toFixed(8));

        $(".multi-out-same-recipients .multi-out-recipient").each(function() {
            amount_total += amount;
        });

        total_multi_out = amount_total + fee;

        $(".total_amount_multi_out").html(BRS.formatAmount(BRS.convertToNQT(total_multi_out)) + " BURST");
    });

    $(".same_out_checkbox").on("change", function(e) {
        $(".total_amount_multi_out").html("- BURST");
        if ($(this).is(":checked")) {
            $(".multi-out-same").fadeIn();
            $(".multi-out-ordinary").hide();
        } else {
            $(".multi-out-same").hide();
            $(".multi-out-ordinary").fadeIn();
        }
    });

    $("#multi-out-fee").on("change", function(e) {
        var current_fee = parseFloat($(this).val(), 10);
        var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);

        $("#multi-out-fee").val(fee.toFixed(8));

        $(".total_amount_multi_out").html(BRS.formatAmount(BRS.convertToNQT(amount_total + fee)) + " BURST");
    });

    $("#multi-out-submit").on("click", function(e) {
        var recipients = [];
        var passphrase = $("#multi-out-passphrase").val();
        if (passphrase == "") {
            $(".multi-out").find(".error_message").html("Passphrase is empty!").show();
            return;
        }

        var fee = parseFloat($("#multi-out-fee").val(), 10);
        if (isNaN(fee)) {
            $(".multi-out").find(".error_message").html("Fee is not specified!").show();
            return;
        }

        if ($(".same_out_checkbox").is(":checked")) {
            var amount = $("#multi-out-same-amount").val();
            if (isNaN(amount) || amount <= 0) {
                $(".multi-out").find(".error_message").html("Wrong amount!").show();
                return;
            }
            $(".multi-out-same-recipients .multi-out-recipient").each(function() {
                recipients.push($(this).val());
            });
            // verify recipients
            var ids = [];
            for (var i = 0; i < recipients.length; i++) {
                var address = new NxtAddress();
                address.set(recipients[i]);
                if (!address.ok()) {
                    $(".multi-out").find(".error_message").html("Wrong addresses. Please verify!").show();
                    return;
                }
                var id = address.account_id();
                ids.push(id);
            }
            // duplicate ids?
            if ((new Set(ids)).size !== ids.length) {
                $(".multi-out").find(".error_message").html("Duplicate recipients not allowed!").show();
                return;
            }
            BRS.sendMultiOutSame(ids, amount, fee, passphrase);
        } else {
            var amounts = [];
            $(".multi-out-recipients .multi-out-amount").each(function() {
                var amount = $(this).val();
                if (isNaN(amount) || amount <= 0) {
                    $(".multi-out").find(".error_message").html("Wrong amount!").show();
                    return;
                }
                amounts.push(amount);
            });
            $(".multi-out-recipients .multi-out-recipient").each(function() {
                recipients.push($(this).val());
            });

            if (recipients.length != amounts.length) {
                $(".multi-out").find(".error_message").html("Amount count does not match recipient count").show();
                return;
            }
            // verify recipients
            var ids = [];
            for (var i = 0; i < recipients.length; i++) {
                var address = new NxtAddress();
                address.set(recipients[i]);
                if (!address.ok()) {
                    $(".multi-out").find(".error_message").html("Wrong addresses. Please verify!").show();
                    return;
                }
                var id = address.account_id();
                ids.push(id);
            }
            // duplicate ids?
            if ((new Set(ids)).size !== ids.length) {
                $(".multi-out").find(".error_message").html("Duplicate recipients not allowed!").show();
                return;
            }
            BRS.sendMultiOut(ids, amounts, fee, passphrase);
        }
    });

    $(".add_message").on("change", function(e) {
        if ($(this).is(":checked")) {
            $(this).closest("form").find(".optional_message").fadeIn();
            $(this).closest(".form-group").css("margin-bottom", "5px");
        } else {
            $(this).closest("form").find(".optional_message").hide();
            $(this).closest(".form-group").css("margin-bottom", "");
        }
    });

    $(".add_note_to_self").on("change", function(e) {
        if ($(this).is(":checked")) {
            $(this).closest("form").find(".optional_note").fadeIn();
        } else {
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
            } else {
                $(this).mask("BURST-****-****-****-*****");
            }
        });

        var $visible_modal = $(".modal.in");

        if ($visible_modal.length) {
            if ($visible_modal.hasClass("locked")) {
                var $btn = $visible_modal.find("button.btn-primary:not([data-dismiss=modal])");
                BRS.unlockForm($visible_modal, $btn, true);
            } else {
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
        // multi-out reset
        multi_out_recipients = 2;
        multi_out_same_recipients = 2;
        // remove recipients
        $(".multi-out-recipients").empty();
        $(".multi-out-same-recipients").empty();
        // add default recipients
        $(".multi-out").hide();
        $(".multi-out-same").hide();
        $(".multi-out-ordinary").fadeIn();
        $(".multi-out-recipients").append($("#additional_multi_out_recipient").html());
        $(".multi-out-recipients").append($("#additional_multi_out_recipient").html());
        $(".multi-out-same-recipients").append($("#additional_multi_out_same_recipient").html());
        $(".multi-out-same-recipients").append($("#additional_multi_out_same_recipient").html());
        $(".multi-out .remove_recipient").each(function() {
            $(this).remove();
        });
        // uncheck same out
        $(".same_out_checkbox").prop('checked', false);
        // reset fee and amount
        $("#multi-out-fee").val((0.1).toFixed(8));
        $("#multi-out-same-amount").val('');
        // show ordinary
        $(".ordinary").fadeIn();
        if (!$(".ordinary-nav").hasClass("active")) {
            $(".ordinary-nav").addClass("active");
        }
        if ($(".multi-out-nav").toggleClass("active")) {
            $(".multi-out-nav").removeClass("active");
        }

        $(this).find("input[name=recipient], input[name=account_id]").not("[type=hidden]").trigger("unmask");

        $(this).find(":input:not(button)").each(function(index) {
            var defaultValue = $(this).data("default");
            var type = $(this).attr("type");
            var tag = $(this).prop("tagName").toLowerCase();

            if (type == "checkbox") {
                if (defaultValue == "checked") {
                    $(this).prop("checked", true);
                } else {
                    $(this).prop("checked", false);
                }
            } else if (type == "hidden") {
                if (defaultValue !== undefined) {
                    $(this).val(defaultValue);
                }
            } else if (tag == "select") {
                if (defaultValue !== undefined) {
                    $(this).val(defaultValue);
                } else {
                    $(this).find("option:selected").prop("selected", false);
                    $(this).find("option:first").prop("selected", "selected");
                }
            } else {
                if (defaultValue !== undefined) {
                    $(this).val(defaultValue);
                } else {
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
                defaultFee = 0.1;
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
    };

    BRS.closeModal = function($modal) {
        if (!$modal) {
            $modal = $("div.modal.in:first");
        }

        $modal.find("button").prop("disabled", false);

        var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");

        $btn.button("reset");
        $modal.modal("unlock");
        $modal.modal("hide");
    };

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
        } else {
            $modal.find(".advanced").hide();
        }

        $modal.find(".advanced_extend").each(function(index, obj) {
            var normalSize = $(obj).data("normal");
            var advancedSize = $(obj).data("advanced");

            if (text == "advanced") {
                $(obj).addClass("col-xs-" + advancedSize + " col-sm-" + advancedSize + " col-md-" + advancedSize).removeClass("col-xs-" + normalSize + " col-sm-" + normalSize + " col-md-" + normalSize);
            } else {
                $(obj).removeClass("col-xs-" + advancedSize + " col-sm-" + advancedSize + " col-md-" + advancedSize).addClass("col-xs-" + normalSize + " col-sm-" + normalSize + " col-md-" + normalSize);
            }
        });

        if (text == $.t("advanced")) {
            $(this).text($.t("basic"));
        } else {
            $(this).text($.t("advanced"));
        }
    });

    return BRS;
}(BRS || {}, jQuery));
