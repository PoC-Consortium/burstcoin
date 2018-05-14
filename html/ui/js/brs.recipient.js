/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.automaticallyCheckRecipient = function() {
        var $recipientFields = $("#send_money_recipient, #transfer_asset_recipient, #send_message_recipient, #add_contact_account_id, #update_contact_account_id, #lease_balance_recipient, #transfer_alias_recipient, #sell_alias_recipient");

        $recipientFields.on("blur", function() {
            $(this).trigger("checkRecipient");
        });

        $recipientFields.on("checkRecipient", function() {
            var value = $(this).val();
            var modal = $(this).closest(".modal");

            if (value && value != "BURST-____-____-____-_____") {
                BRS.checkRecipient(value, modal);
            } else {
                modal.find(".account_info").hide();
            }
        });

        $recipientFields.on("oldRecipientPaste", function() {});
    }

    $("#send_message_modal, #send_money_modal, #add_contact_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);

        var account = $invoker.data("account");

        if (!account) {
            account = $invoker.data("contact");
        }

        if (account) {
            var $inputField = $(this).find("input[name=recipient], input[name=account_id]").not("[type=hidden]");

            if (!/BURST\-/i.test(account)) {
                $inputField.addClass("noMask");
            }

            $inputField.val(account).trigger("checkRecipient");
        }
    });

    $("#send_money_amount").on("change", function(e) {
        var current_amount = parseFloat($(this).val(), 10);
        var current_fee = parseFloat($("#send_money_fee").val(), 10);
        var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);
        var amount = isNaN(current_amount) ? 0.00000001 : (current_amount < 0.00000001 ? 0.00000001 : current_amount);

        $("#send_money_amount").val(amount.toFixed(8));
        $("#send_money_fee").val(fee.toFixed(8));

        $(this).closest(".modal").find(".total_amount_ordinary").html(BRS.formatAmount(BRS.convertToNQT(amount + fee)) + " BURST");
    });

    $("#send_money_fee").on("change", function(e) {
        var current_amount = parseFloat($("#send_money_amount").val(), 10);
        var current_fee = parseFloat($(this).val(), 10);
        var fee = isNaN(current_fee) ? 0.1 : (current_fee < 0.00735 ? 0.00735 : current_fee);
        var amount = isNaN(current_amount) ? 0.00000001 : (current_amount < 0.00000001 ? 0.00000001 : current_amount);

        $("#send_money_amount").val(amount.toFixed(8));
        $("#send_money_fee").val(fee.toFixed(8));

        $(this).closest(".modal").find(".total_amount_ordinary").html(BRS.formatAmount(BRS.convertToNQT(amount + fee)) + " BURST");
    });

    //todo later: http://twitter.github.io/typeahead.js/
    $("span.recipient_selector button").on("click", function(e) {
        if (!Object.keys(BRS.contacts).length) {
            e.preventDefault();
            e.stopPropagation();
            return;
        }

        var $list = $(this).parent().find("ul");

        $list.empty();

        for (var accountId in BRS.contacts) {
            $list.append("<li><a href='#' data-contact='" + String(BRS.contacts[accountId].name).escapeHTML() + "'>" + String(BRS.contacts[accountId].name).escapeHTML() + "</a></li>");
        }
    });

    $("span.recipient_selector").on("click", "ul li a", function(e) {
        e.preventDefault();
        $(this).closest("form").find("input[name=converted_account_id]").val("");
        $(this).closest("form").find("input[name=recipient],input[name=account_id]").not("[type=hidden]").trigger("unmask").val($(this).data("contact")).trigger("blur");
    });

    $(document).on("click", ".recipient_selector_multi_out button", function(e) {
        if (!Object.keys(BRS.contacts).length) {
            e.preventDefault();
            e.stopPropagation();
            return;
        }

        var $list = $(this).parent().find("ul");

        $list.empty();

        for (var accountId in BRS.contacts) {
            $list.append("<li><a href='#' data-contact='" + String(accountId).escapeHTML() + "'>" + String(BRS.contacts[accountId].name).escapeHTML() + "</a></li>");
        }
    });

    $(document).on("click", ".recipient_selector_multi_out ul li a", function(e) {
        e.preventDefault();
        // fucking ugly hack serious jquery cancer - angular 6 please
        $(this).parent().parent().parent().parent().find(".multi-out-recipient").val($(this).data("contact"));
    });

    BRS.forms.sendMoneyComplete = function(response, data) {
        if (!(data["_extra"] && data["_extra"].convertedAccount) && !(data.recipient in BRS.contacts)) {
            $.notify($.t("success_send_money") + " <a href='#' data-account='" + BRS.getAccountFormatted(data, "recipient") + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>" + $.t("add_recipient_to_contacts_q") + "</a>", {
                "type": "success"
            });
        } else {
            $.notify($.t("success_send_money"), {
                "type": "success"
            });
        }
    }

    BRS.sendMoneyShowAccountInformation = function(accountId) {
        BRS.getAccountError(accountId, function(response) {
            if (response.type == "success") {
                $("#send_money_account_info").hide();
            } else {
                $("#send_money_account_info").html(response.message).show();

            }
        });
    }

    BRS.getAccountError = function(accountId, callback) {
        BRS.sendRequest("getAccount", {
            "account": accountId
        }, function(response) {
            if (response.publicKey) {
                callback({
                    "type": "info",
                    "message": $.t("recipient_info", {
                        "burst": BRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
                    }),
                    "account": response
                });
            } else {
                if (response.errorCode) {
                    if (response.errorCode == 4) {
                        callback({
                            "type": "danger",
                            "message": $.t("recipient_malformed") + (!/^(BURST\-)/i.test(accountId) ? " " + $.t("recipient_alias_suggestion") : ""),
                            "account": null
                        });
                    } else if (response.errorCode == 5) {
                        callback({
                            "type": "warning",
                            "message": $.t("recipient_unknown_pka"),
                            "account": null,
                            "noPublicKey": true
                        });
                    } else {
                        callback({
                            "type": "danger",
                            "message": $.t("recipient_problem") + " " + String(response.errorDescription).escapeHTML(),
                            "account": null
                        });
                    }
                } else {
                    callback({
                        "type": "warning",
                        "message": $.t("recipient_no_public_key_pka", {
                            "burst": BRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
                        }),
                        "account": response,
                        "noPublicKey": true
                    });
                }
            }
        });
    }

    BRS.correctAddressMistake = function(el) {
        $(el).closest(".modal-body").find("input[name=recipient],input[name=account_id]").val($(el).data("address")).trigger("blur");
    }

    BRS.checkRecipient = function(account, modal) {
        var classes = "callout-info callout-danger callout-warning";

        var callout = modal.find(".account_info").first();
        var accountInputField = modal.find("input[name=converted_account_id]");
        var merchantInfoField = modal.find("input[name=merchant_info]");
        var recipientPublicKeyField = modal.find("input[name=recipientPublicKey]");

        accountInputField.val("");
        merchantInfoField.val("");

        account = $.trim(account);

        //solomon reed. Btw, this regex can be shortened..
        if (/^(BURST\-)?[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+/i.test(account)) {
            var address = new NxtAddress();

            if (address.set(account)) {
                BRS.getAccountError(account, function(response) {
                    modal.find("input[name=recipientPublicKey]").val("");
                    modal.find(".recipient_public_key").hide();
                    if (response.account && response.account.description) {
                        checkForMerchant(response.account.description, modal);
                    }

                    var message = response.message.escapeHTML();

                    callout.removeClass(classes).addClass("callout-" + response.type).html(message).show();
                });
            } else {
                if (address.guess.length == 1) {
                    callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed_suggestion", {
                        "recipient": "<span class='malformed_address' data-address='" + String(address.guess[0]).escapeHTML() + "' onclick='BRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[0], account) + "</span>"
                    })).show();
                } else if (address.guess.length > 1) {
                    var html = $.t("recipient_malformed_suggestion", {
                        "count": address.guess.length
                    }) + "<ul>";
                    for (var i = 0; i < address.guess.length; i++) {
                        html += "<li><span clas='malformed_address' data-address='" + String(address.guess[i]).escapeHTML() + "' onclick='BRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[i], account) + "</span></li>";
                    }

                    callout.removeClass(classes).addClass("callout-danger").html(html).show();
                } else {
                    callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
                }
            }
        } else if (!(/^\d+$/.test(account))) {
            if (BRS.databaseSupport && account.charAt(0) != '@') {
                BRS.database.select("contacts", [{
                    "name": account
                }], function(error, contact) {
                    if (!error && contact.length) {
                        contact = contact[0];
                        BRS.getAccountError(contact.accountRS, function(response) {
                            modal.find("input[name=recipientPublicKey]").val("");
                            modal.find(".recipient_public_key").hide();
                            if (response.account && response.account.description) {
                                checkForMerchant(response.account.description, modal);
                            }

                            callout.removeClass(classes).addClass("callout-" + response.type).html($.t("contact_account_link", {
                                "account_id": BRS.getAccountFormatted(contact, "account")
                            }) + " " + response.message.escapeHTML()).show();

                            if (response.type == "info" || response.type == "warning") {
                                accountInputField.val(contact.accountRS);
                            }
                        });
                    } else if (/^[a-z0-9]+$/i.test(account)) {
                        BRS.checkRecipientAlias(account, modal);
                    } else {
                        callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
                    }
                });
            } else if (/^[a-z0-9@]+$/i.test(account)) {
                if (account.charAt(0) == '@') {
                    account = account.substring(1);
                    BRS.checkRecipientAlias(account, modal);
                }
            } else {
                callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
            }
        } else {
            BRS.getAccountError(account, function(response) {
                callout.removeClass(classes).addClass("callout-" + response.type).html(response.message.escapeHTML()).show();
            });
        }
    }

    BRS.checkRecipientAlias = function(account, modal) {
        var classes = "callout-info callout-danger callout-warning";
        var callout = modal.find(".account_info").first();
        var accountInputField = modal.find("input[name=converted_account_id]");

        accountInputField.val("");

        BRS.sendRequest("getAlias", {
            "aliasName": account
        }, function(response) {
            if (response.errorCode) {
                callout.removeClass(classes).addClass("callout-danger").html($.t("error_invalid_account_id")).show();
            } else {
                if (response.aliasURI) {
                    var alias = String(response.aliasURI);
                    var timestamp = response.timestamp;

                    var regex_1 = /acct:(.*)@burst/;
                    var regex_2 = /nacc:(.*)/;

                    var match = alias.match(regex_1);

                    if (!match) {
                        match = alias.match(regex_2);
                    }

                    if (match && match[1]) {
                        match[1] = String(match[1]).toUpperCase();

                        if (/^\d+$/.test(match[1])) {
                            var address = new NxtAddress();

                            if (address.set(match[1])) {
                                match[1] = address.toString();
                            } else {
                                accountInputField.val("");
                                callout.html("Invalid account alias.");
                            }
                        }

                        BRS.getAccountError(match[1], function(response) {
                            modal.find("input[name=recipientPublicKey]").val("");
                            modal.find(".recipient_public_key").hide();
                            if (response.account && response.account.description) {
                                checkForMerchant(response.account.description, modal);
                            }

                            accountInputField.val(match[1].escapeHTML());
                            callout.html($.t("alias_account_link", {
                                "account_id": String(match[1]).escapeHTML()
                            }) + ". " + $.t("recipient_unknown_pka") + " " + $.t("alias_last_adjusted", {
                                "timestamp": BRS.formatTimestamp(timestamp)
                            })).removeClass(classes).addClass("callout-" + response.type).show();
                        });
                    } else {
                        callout.removeClass(classes).addClass("callout-danger").html($.t("alias_account_no_link") + (!alias ? $.t("error_uri_empty") : $.t("uri_is", {
                            "uri": String(alias).escapeHTML()
                        }))).show();
                    }
                } else if (response.aliasName) {
                    callout.removeClass(classes).addClass("callout-danger").html($.t("error_alias_empty_uri")).show();
                } else {
                    callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? $.t("error") + ": " + String(response.errorDescription).escapeHTML() : $.t("error_alias")).show();
                }
            }
        });
    }

    function checkForMerchant(accountInfo, modal) {
        var requestType = modal.find("input[name=request_type]").val();

        if (requestType == "sendMoney" || requestType == "transferAsset") {
            if (accountInfo.match(/merchant/i)) {
                modal.find("input[name=merchant_info]").val(accountInfo);
                var checkbox = modal.find("input[name=add_message]");
                if (!checkbox.is(":checked")) {
                    checkbox.prop("checked", true).trigger("change");
                }
            }
        }
    }

    return BRS;
}(BRS || {}, jQuery));
