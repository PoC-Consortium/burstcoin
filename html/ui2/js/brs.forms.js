/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.confirmedFormWarning = false;

    BRS.forms = {};

    $(".modal form input").keydown(function(e) {
        if (e.which === "13") {
            e.preventDefault();
            if (BRS.settings.submit_on_enter && e.target.type !== "textarea") {
                $(this).submit();
            } else {
                return false;
            }
        }
    });

    $(".modal button.btn-primary:not([data-dismiss=modal]):not([data-ignore=true])").click(function() {
        // ugly hack - this whole ui is hack, got a big urge to vomit
        if (!$(this).hasClass("multi-out")) {
            BRS.submitForm($(this).closest(".modal"), $(this));
        }
    });

    function getSuccessMessage(requestType) {
        var ignore = ["asset_exchange_change_group_name", "asset_exchange_group", "add_contact", "update_contact", "delete_contact",
            "send_message", "decrypt_messages", "start_forging", "stop_forging", "generate_token", "send_money", "set_alias", "add_asset_bookmark", "sell_alias"
        ];

        if (ignore.indexOf(requestType) !== -1) {
            return "";
        } else {
            var key = "success_" + requestType;

            if ($.i18n.exists(key)) {
                return $.t(key);
            } else {
                return "";
            }
        }
    }

    function getErrorMessage(requestType) {
        var ignore = ["start_forging", "stop_forging", "generate_token", "validate_token"];

        if (ignore.indexOf(requestType) !== -1) {
            return "";
        } else {
            var key = "error_" + requestType;

            if ($.i18n.exists(key)) {
                return $.t(key);
            } else {
                return "";
            }
        }
    }

    BRS.addMessageData = function(data, requestType) {
        var encrypted;
        if (requestType === "sendMessage") {
            data.add_message = true;
        }

        if (!data.add_message && !data.add_note_to_self) {
            delete data.message;
            delete data.note_to_self;
            delete data.encrypt_message;
            delete data.add_message;
            delete data.add_note_to_self;

            return data;
        } else if (!data.add_message) {
            delete data.message;
            delete data.encrypt_message;
            delete data.add_message;
        } else if (!data.add_note_to_self) {
            delete data.note_to_self;
            delete data.add_note_to_self;
        }

        data._extra = {
            "message": data.message,
            "note_to_self": data.note_to_self
        };

        if (data.add_message && data.message) {
            if (data.encrypt_message) {
                try {
                    var options = {};

                    if (data.recipient) {
                        options.account = data.recipient;
                    } else if (data.encryptedMessageRecipient) {
                        options.account = data.encryptedMessageRecipient;
                        delete data.encryptedMessageRecipient;
                    }

                    if (data.recipientPublicKey) {
                        options.publicKey = data.recipientPublicKey;
                    }

                    encrypted = BRS.encryptNote(data.message, options, data.secretPhrase);

                    data.encryptedMessageData = encrypted.message;
                    data.encryptedMessageNonce = encrypted.nonce;
                    data.messageToEncryptIsText = "true";

                    delete data.message;
                } catch (err) {
                    throw err;
                }
            } else {
                data.messageIsText = "true";
            }
        } else {
            delete data.message;
        }

        if (data.add_note_to_self && data.note_to_self) {
            try {
                encrypted = BRS.encryptNote(data.note_to_self, {
                    "publicKey": converters.hexStringToByteArray(BRS.generatePublicKey(data.secretPhrase))
                }, data.secretPhrase);

                data.encryptToSelfMessageData = encrypted.message;
                data.encryptToSelfMessageNonce = encrypted.nonce;
                data.messageToEncryptToSelfIsText = "true";

                delete data.note_to_self;
            } catch (err) {
                throw err;
            }
        } else {
            delete data.note_to_self;
        }

        delete data.add_message;
        delete data.encrypt_message;
        delete data.add_note_to_self;

        return data;
    };


    BRS.submitForm = function($modal, $btn) {
        if (!$btn) {
            $btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");
        }

        $modal = $btn.closest(".modal");
        var $form;
        $modal.modal("lock");
        $modal.find("button").prop("disabled", true);
        $btn.button("loading");

        if ($btn.data("form")) {
            $form = $modal.find("form#" + $btn.data("form"));
            if (!$form.length) {
                $form = $modal.find("form:first");
            }
        } else {
            $form = $modal.find("form:first");
        }

        var requestType = $form.find("input[name=request_type]").val();
        var requestTypeKey = requestType.replace(/([A-Z])/g, function($1) {
            return "_" + $1.toLowerCase();
        });

        var successMessage = getSuccessMessage(requestTypeKey);
        var errorMessage = getErrorMessage(requestTypeKey);

        var data = null;

        var formFunction = BRS.forms[requestType];
        var formErrorFunction = BRS.forms[requestType + "Error"];

        if (typeof formErrorFunction !== "function") {
            formErrorFunction = false;
        }

        var originalRequestType = requestType;

        if (BRS.downloadingBlockchain) {
            $form.find(".error_message").html($.t("error_blockchain_downloading")).show();
            if (formErrorFunction) {
                formErrorFunction();
            }
            BRS.unlockForm($modal, $btn);
            return;
        } else if (BRS.state.isScanning) {
            $form.find(".error_message").html($.t("error_form_blockchain_rescanning")).show();
            if (formErrorFunction) {
                formErrorFunction();
            }
            BRS.unlockForm($modal, $btn);
            return;
        }

        var invalidElement = false;

        //TODO
        $form.find(":input").each(function() {
            if ($(this).is(":invalid")) {
                var error = "";
                var name = String($(this).attr("name")).replace("NXT", "").replace("NQT", "").capitalize();
                var value = $(this).val();

                if ($(this).hasAttr("max")) {
                    if (!/^[\-\d\.]+$/.test(value)) {
                        error = $.t("error_not_a_number", {
                            "field": BRS.getTranslatedFieldName(name).toLowerCase()
                        }).capitalize();
                    } else {
                        var max = $(this).attr("max");

                        if (value > max) {
                            error = $.t("error_max_value", {
                                "field": BRS.getTranslatedFieldName(name).toLowerCase(),
                                "max": max
                            }).capitalize();
                        }
                    }
                }

                if ($(this).hasAttr("min")) {
                    if (!/^[\-\d\.]+$/.test(value)) {
                        error = $.t("error_not_a_number", {
                            "field": BRS.getTranslatedFieldName(name).toLowerCase()
                        }).capitalize();
                    } else {
                        var min = $(this).attr("min");
                        if (value < min) {
                            error = $.t("error_min_value", {
                                "field": BRS.getTranslatedFieldName(name).toLowerCase(),
                                "min": min
                            }).capitalize();
                        }
                    }
                }

                if (!error) {
                    error = $.t("error_invalid_field", {
                        "field": BRS.getTranslatedFieldName(name).toLowerCase()
                    }).capitalize();
                }

                $form.find(".error_message").html(error).show();

                if (formErrorFunction) {
                    formErrorFunction();
                }

                BRS.unlockForm($modal, $btn);
                invalidElement = true;
                return false;
            }
        });

        if (invalidElement) {
            return;
        }

        if (typeof formFunction === "function") {
            var output = formFunction($modal);

            if (!output) {
                return;
            } else if (output.error) {
                $form.find(".error_message").html(output.error.escapeHTML()).show();
                if (formErrorFunction) {
                    formErrorFunction();
                }
                BRS.unlockForm($modal, $btn);
                return;
            } else {
                if (output.requestType) {
                    requestType = output.requestType;
                }
                if (output.data) {
                    data = output.data;
                }
                if ("successMessage" in output) {
                    successMessage = output.successMessage;
                }
                if ("errorMessage" in output) {
                    errorMessage = output.errorMessage;
                }
                if (output.stop) {
                    BRS.unlockForm($modal, $btn, true);
                    return;
                }
            }
        }

        if (!data) {
            data = BRS.getFormData($form);
        }

        if (data.recipient) {
            data.recipient = $.trim(data.recipient);
            if (/^\d+$/.test(data.recipient)) {} else if (!/^BURST\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+/i.test(data.recipient)) {
                var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
                if (!convertedAccountId || (!/^\d+$/.test(convertedAccountId) && !/^BURST\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+/i.test(convertedAccountId))) {
                    $form.find(".error_message").html($.t("error_account_id")).show();
                    if (formErrorFunction) {
                        formErrorFunction(false, data);
                    }
                    BRS.unlockForm($modal, $btn);
                    return;
                } else {
                    data.recipient = convertedAccountId;
                    data._extra = {
                        "convertedAccount": true
                    };
                }
            }
        }

        if (requestType === "sendMoney" || requestType === "transferAsset") {
            var merchantInfo = $modal.find("input[name=merchant_info]").val();

            var result = merchantInfo.match(/#merchant:(.*)#/i);
            var regexp;
            if (result && result[1]) {
                merchantInfo = $.trim(result[1]);

                if (!data.add_message || !data.message) {
                    $form.find(".error_message").html($.t("info_merchant_message_required")).show();
                    if (formErrorFunction) {
                        formErrorFunction(false, data);
                    }
                    BRS.unlockForm($modal, $btn);
                    return;
                }

                if (merchantInfo === "numeric") {
                    merchantInfo = "[0-9]+";
                } else if (merchantInfo === "alphanumeric") {
                    merchantInfo = "[a-zA-Z0-9]+";
                }

                var regexParts = merchantInfo.match(/^\/(.*?)\/(.*)$/);

                if (!regexParts) {
                    regexParts = ["", merchantInfo, ""];
                }

                var strippedRegex = regexParts[1].replace(/^[\^\(]*/, "").replace(/[\$\)]*$/, "");

                if (regexParts[1].charAt(0) != "^") {
                    regexParts[1] = "^" + regexParts[1];
                }

                if (regexParts[1].slice(-1) != "$") {
                    regexParts[1] = regexParts[1] + "$";
                }

                if (regexParts[2].indexOf("i") !== -1) {
                    regexp = new RegExp(regexParts[1], "i");
                } else {
                    regexp = new RegExp(regexParts[1]);
                }

                if (!regexp.test(data.message)) {
                    var regexType;
                    var lengthRequirement = strippedRegex.match(/\{(.*)\}/);

                    if (lengthRequirement) {
                        strippedRegex = strippedRegex.replace(lengthRequirement[0], "+");
                    }

                    if (strippedRegex === "[0-9]+") {
                        regexType = "numeric";
                    } else if (strippedRegex === "[a-z0-9]+" || strippedRegex.toLowerCase() === "[a-za-z0-9]+" || strippedRegex === "[a-z0-9]+") {
                        regexType = "alphanumeric";
                    } else {
                        regexType = "custom";
                    }

                    if (lengthRequirement) {
                        var minLength, maxLength, requiredLength;

                        if (lengthRequirement[1].indexOf(",") !== -1) {
                            lengthRequirement = lengthRequirement[1].split(",");
                            minLength = parseInt(lengthRequirement[0], 10);
                            if (lengthRequirement[1]) {
                                maxLength = parseInt(lengthRequirement[1], 10);
                                errorMessage = $.t("error_merchant_message_" + regexType + "_range_length", {
                                    "minLength": minLength,
                                    "maxLength": maxLength
                                });
                            } else {
                                errorMessage = $.t("error_merchant_message_" + regexType + "_min_length", {
                                    "minLength": minLength
                                });
                            }
                        } else {
                            requiredLength = parseInt(lengthRequirement[1], 10);
                            errorMessage = $.t("error_merchant_message_" + regexType + "_length", {
                                "length": requiredLength
                            });
                        }
                    } else {
                        errorMessage = $.t("error_merchant_message_" + regexType);
                    }

                    $form.find(".error_message").html(errorMessage).show();
                    if (formErrorFunction) {
                        formErrorFunction(false, data);
                    }
                    BRS.unlockForm($modal, $btn);
                    return;
                }
            }
        }

        try {
            data = BRS.addMessageData(data, requestType);
        } catch (err) {
            $form.find(".error_message").html(String(err.message).escapeHTML()).show();
            if (formErrorFunction) {
                formErrorFunction();
            }
            BRS.unlockForm($modal, $btn);
            return;
        }

        if (data.deadline) {
            data.deadline = String(data.deadline * 60); //hours to minutes
        }

        if (data.doNotBroadcast) {
            data.broadcast = "false";
            delete data.doNotBroadcast;
            if (data.secretPhrase === "") {
                delete data.secretPhrase;
            }
        }

        if ("secretPhrase" in data && !data.secretPhrase.length && !BRS.rememberPassword) {
            $form.find(".error_message").html($.t("error_passphrase_required")).show();
            if (formErrorFunction) {
                formErrorFunction(false, data);
            }
            BRS.unlockForm($modal, $btn);
            return;
        }

        if (!BRS.showedFormWarning) {
            if ("amountNXT" in data && BRS.settings.amount_warning && BRS.settings.amount_warning !== "0") {
                if (new BigInteger(BRS.convertToNQT(data.amountNXT)).compareTo(new BigInteger(BRS.settings.amount_warning)) > 0) {
                    BRS.showedFormWarning = true;
                    $form.find(".error_message").html($.t("error_max_amount_warning", {
                        "burst": BRS.formatAmount(BRS.settings.amount_warning)
                    })).show();
                    if (formErrorFunction) {
                        formErrorFunction(false, data);
                    }
                    BRS.unlockForm($modal, $btn);
                    return;
                }
            }

            if ("feeNXT" in data && BRS.settings.fee_warning && BRS.settings.fee_warning !== "0") {
                if (new BigInteger(BRS.convertToNQT(data.feeNXT)).compareTo(new BigInteger(BRS.settings.fee_warning)) > 0) {
                    BRS.showedFormWarning = true;
                    $form.find(".error_message").html($.t("error_max_fee_warning", {
                        "burst": BRS.formatAmount(BRS.settings.fee_warning)
                    })).show();
                    if (formErrorFunction) {
                        formErrorFunction(false, data);
                    }
                    BRS.unlockForm($modal, $btn);
                    return;
                }
            }
        }

        BRS.sendRequest(requestType, data, function(response) {
            //todo check again.. response.error
            var formCompleteFunction;
            if (response.fullHash) {
                BRS.unlockForm($modal, $btn);

                if (!$modal.hasClass("modal-no-hide")) {
                    $modal.modal("hide");
                }

                if (successMessage) {
                    $.notify(successMessage.escapeHTML(), {
                        type: "success"
                    });
                }

                formCompleteFunction = BRS.forms[originalRequestType + "Complete"];

                if (requestType !== "parseTransaction") {
                    if (typeof formCompleteFunction === "function") {
                        data.requestType = requestType;

                        if (response.transaction) {
                            BRS.addUnconfirmedTransaction(response.transaction, function(alreadyProcessed) {
                                response.alreadyProcessed = alreadyProcessed;
                                formCompleteFunction(response, data);
                            });
                        } else {
                            response.alreadyProcessed = false;
                            formCompleteFunction(response, data);
                        }
                    } else {
                        BRS.addUnconfirmedTransaction(response.transaction);
                    }
                } else {
                    if (typeof formCompleteFunction === "function") {
                        data.requestType = requestType;
                        formCompleteFunction(response, data);
                    }
                }

                if (BRS.accountInfo && !BRS.accountInfo.publicKey) {
                    $("#dashboard_message").hide();
                }
            } else if (response.errorCode) {
                $form.find(".error_message").html(response.errorDescription.escapeHTML()).show();

                if (formErrorFunction) {
                    formErrorFunction(response, data);
                }

                BRS.unlockForm($modal, $btn);
            } else {
                var sentToFunction = false;

                if (!errorMessage) {
                    formCompleteFunction = BRS.forms[originalRequestType + "Complete"];

                    if (typeof formCompleteFunction === 'function') {
                        sentToFunction = true;
                        data.requestType = requestType;

                        BRS.unlockForm($modal, $btn);

                        if (!$modal.hasClass("modal-no-hide")) {
                            $modal.modal("hide");
                        }
                        formCompleteFunction(response, data);
                    } else {
                        errorMessage = $.t("error_unknown");
                    }
                }

                if (!sentToFunction) {
                    BRS.unlockForm($modal, $btn, true);

                    $.notify(errorMessage.escapeHTML(), {
                        type: 'danger'
                    });
                }
            }
        });
    };

    BRS.unlockForm = function($modal, $btn, hide) {
        $modal.find("button").prop("disabled", false);
        if ($btn) {
            $btn.button("reset");
        }
        $modal.modal("unlock");
        if (hide) {
            $modal.modal("hide");
        }
    };

    BRS.sendMultiOut = function(recipients, amounts, fee, passphrase) {
        var multiOutString = "";
        for (var i = 0; i < recipients.length; i++) {
            multiOutString += recipients[i] + ":" + BRS.convertToNQT(amounts[i]) + ";";
        }
        multiOutString = multiOutString.substring(0, multiOutString.length - 1);

        var data = {
            secretPhrase: passphrase,
            recipients: multiOutString,
            feeNQT: BRS.convertToNQT(fee),
            deadline: "1440",
        };

        BRS.sendRequest("sendMoneyMulti", data, function(response) {
            if (response.errorCode) {
                $(".multi-out").find(".error_message").html(response.errorDescription.escapeHTML()).show();
            } else {
                $(".modal").modal("hide");
            }
        });
    };

    BRS.sendMultiOutSame = function(recipients, amount, fee,  passphrase) {
        var multiOutString = "";
        for (var i = 0; i < recipients.length; i++) {
            multiOutString += recipients[i] + ";";
        }
        multiOutString = multiOutString.substring(0, multiOutString.length - 1);

        var data = {
            secretPhrase: passphrase,
            recipients: multiOutString,
            amountNQT: BRS.convertToNQT(amount),
            feeNQT: BRS.convertToNQT(fee),
            deadline: "1440",
        };

        BRS.sendRequest("sendMoneyMultiSame", data, function(response) {
            if (response.errorCode) {
                $(".multi-out").find(".error_message").html(response.errorDescription.escapeHTML()).show();
            } else {
                $(".modal").modal("hide");
            }
        });
    };

    return BRS;
}(BRS || {}, jQuery));
