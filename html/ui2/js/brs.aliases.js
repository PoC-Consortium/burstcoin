/**
 * @depends {brs.js}
 */
var BRS;
BRS = (function (BRS, $, undefined) {
    BRS.pages.aliases = function () {
        BRS.sendRequest("getAliases+", {
            "account": BRS.account,
            "timestamp": 0
        }, function (response) {
            if (response.aliases && response.aliases.length) {
                var aliases = response.aliases;

                if (BRS.unconfirmedTransactions.length) {
                    for (var i = 0; i < BRS.unconfirmedTransactions.length; i++) {
                        var unconfirmedTransaction = BRS.unconfirmedTransactions[i];

                        if (unconfirmedTransaction.type === 1 && (unconfirmedTransaction.subtype === 1 || unconfirmedTransaction.subtype === 7)) {
                            var found = false;

                            for (var j = 0; j < aliases.length; j++) {
                                if (aliases[j].aliasName === unconfirmedTransaction.attachment.alias) {
                                    aliases[j].aliasURI = unconfirmedTransaction.attachment.uri;
                                    aliases[j].tentative = true;
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                aliases.push({
                                    "aliasName": unconfirmedTransaction.attachment.alias,
                                    "aliasURI": (unconfirmedTransaction.attachment.uri ? unconfirmedTransaction.attachment.uri : ""),
                                    "tentative": true
                                });
                            }
                        }
                    }
                }


                aliases.sort(function (a, b) {
                    var a_low = a.aliasName.toLowerCase(),
                        b_low = b.aliasName.toLowerCase();
                    if (a_low > b_low)
                        return 1;
                    if (a_low < b_low)
                        return -1;

                    return 0;
                });

                var rows = "";

                var alias_account_count = 0,
                    alias_uri_count = 0,
                    empty_alias_count = 0,
                    alias_count = aliases.length;

                for (var i = 0; i < alias_count; i++) {
                    var alias = aliases[i];

                    alias.status = "/";

                    var unconfirmedTransaction = BRS.getUnconfirmedTransactionFromCache(1, 6, {
                        "alias": alias.aliasName
                    });

                    if (unconfirmedTransaction) {
                        alias.tentative = true;
                        if (unconfirmedTransaction.recipient) {
                            alias.buyer = unconfirmedTransaction.recipient;
                        }
                        alias.priceNQT = unconfirmedTransaction.priceNQT;
                    }

                    if (!alias.aliasURI) {
                        alias.aliasURI = "";
                    }

                    if (alias.aliasURI.length > 100) {
                        alias.shortAliasURI = alias.aliasURI.substring(0, 100) + "...";
                        alias.shortAliasURI = alias.shortAliasURI.escapeHTML();
                    }
                    else {
                        alias.shortAliasURI = alias.aliasURI.escapeHTML();
                    }

                    alias.aliasURI = alias.aliasURI.escapeHTML();

                    var allowCancel = false;

                    if ("priceNQT" in alias) {
                        if (alias.priceNQT === "0") {
                            if (alias.buyer === BRS.account) {
                                alias.status = $.t("cancelling_sale");
                            }
                            else {
                                alias.status = $.t("transfer_in_progress");
                            }
                        }
                        else {
                            if (!alias.tentative) {
                                allowCancel = true;
                            }

                            if (typeof alias.buyer !== "undefined") {
                                alias.status = $.t("for_sale_direct");
                            }
                            else {
                                alias.status = $.t("for_sale_indirect");
                            }
                        }
                    }

                    if (alias.status !== "/") {
                        alias.status = "<span class='label label-small label-info'>" + alias.status + "</span>";
                    }

                    rows += "<tr" + (alias.tentative ? " class='tentative'" : "")
                        + " data-alias='" + String(alias.aliasName).toLowerCase().escapeHTML()
                        + "'><td class='alias'>" + String(alias.aliasName).escapeHTML()
                        + "</td><td class='uri'>"
                        + (alias.aliasURI.indexOf("http") === 0 ? "<a href='" + alias.aliasURI + "' target='_blank'>" + alias.shortAliasURI + "</a>"
                            : alias.shortAliasURI)
                        + "</td><td class='status'>" + alias.status
                        + "</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#register_alias_modal' data-alias='"
                        + String(alias.aliasName).escapeHTML() + "'>"
                        + $.t("edit")
                        + "</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#transfer_alias_modal' data-alias='"
                        + String(alias.aliasName).escapeHTML() + "'>"
                        + $.t("transfer")
                        + "</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#sell_alias_modal' data-alias='"
                        + String(alias.aliasName).escapeHTML() + "'>"
                        + $.t("sell")
                        + "</a>" + (allowCancel ? " <a class='btn btn-xs btn-default cancel_alias_sale' href='#' data-toggle='modal' data-target='#cancel_alias_sale_modal' data-alias='" + String(alias.aliasName).escapeHTML() + "'>" + $.t("cancel_sale") + "</a>" : "")
                        + "</td></tr>";

                    if (!alias.aliasURI) {
                        empty_alias_count++;
                    }
                    else if (alias.aliasURI.indexOf("http") === 0) {
                        alias_uri_count++;
                    }
                    else if (alias.aliasURI.indexOf("acct:") === 0 || alias.aliasURI.indexOf("nacc:") === 0) {
                        alias_account_count++;
                    }
                }

                $("#aliases_table tbody").empty().append(rows);
                BRS.dataLoadFinished($("#aliases_table"));

                $("#alias_account_count").html(alias_account_count).removeClass("loading_dots");
                $("#alias_uri_count").html(alias_uri_count).removeClass("loading_dots");
                $("#empty_alias_count").html(empty_alias_count).removeClass("loading_dots");
                $("#alias_count").html(alias_count).removeClass("loading_dots");
            }
            else {
                $("#aliases_table tbody").empty();
                BRS.dataLoadFinished($("#aliases_table"));

                $("#alias_account_count, #alias_uri_count, #empty_alias_count, #alias_count").html("0").removeClass("loading_dots");
            }

            BRS.pageLoaded();
        });
    };

    $("#transfer_alias_modal, #sell_alias_modal, #cancel_alias_sale_modal").on("show.bs.modal", function (e) {
        var $invoker = $(e.relatedTarget);

        var alias = String($invoker.data("alias"));

        $(this).find("input[name=aliasName]").val(alias.escapeHTML());
        $(this).find(".alias_name_display").html(alias.escapeHTML());

        if ($(this).attr("id") === "sell_alias_modal") {
            $(this).find("ul.nav-pills li").removeClass("active");
            $(this).find("ul.nav-pills li:first-child").addClass("active");
            $("#sell_alias_recipient_div").show();
        }
    });

    BRS.forms.sellAlias = function ($modal) {
        var data = BRS.getFormData($modal.find("form:first"));

        var successMessage = "";
        var errorMessage = "";

        if (data.modal === "cancel_alias_sale") {
            data.priceNXT = "0";
            data.recipient = BRS.accountRS;

            successMessage = $.t("success_cancel_alias");
            errorMessage = $.t("error_cancel_alias");
        }
        else if (data.modal === "transfer_alias") {
            data.priceNXT = "0";

            successMessage = $.t("success_transfer_alias");
            errorMessage = $.t("error_transfer_alias");
        }
        else {
            if (!data.recipient) {
                return {
                    "error": $.t("error_not_specified", {
                        "name": $.t("recipient").toLowerCase()
                    }).capitalize()
                };
            }

            successMessage = $.t("success_sell_alias");
            errorMessage = $.t("error_sell_alias");

            if (data.recipient === BRS.genesisRS) {
                if (!data.priceNXT || data.priceNXT === "0") {
                    return {
                        "error": $.t("error_not_specified", {
                            "name": $.t("price").toLowerCase()
                        }).capitalize()
                    };
                }

                delete data.add_message;
                delete data.encrypt_message;
                delete data.message;
                delete data.recipient;
            }
        }

        delete data.modal;

        return {
            "data": data,
            "successMessage": successMessage,
            "errorMessage": errorMessage
        };
    };

    BRS.forms.sellAliasComplete = function (response, data) {
        if (response.alreadyProcessed) {
            return;
        }

        var $row = $("#aliases_table tr[data-alias=" + String(data.aliasName).toLowerCase().escapeHTML() + "]");

        $row.addClass("tentative");

        //transfer
        if (data.priceNQT === "0") {
            if (data.recipient === BRS.account) {
                $row.find("td.status").html("<span class='label label-small label-info'>" + $.t("cancelling_sale") + "</span>");
                $row.find("a.cancel_alias_sale").remove();
            }
            else {
                $row.find("td.status").html("<span class='label label-small label-info'>" + $.t("transfer_in_progress") + "</span>");
            }
        }
        else {
            if (data.recipient !== BRS.genesis) {
                $row.find("td.status").html("<span class='label label-small label-info'>" + $.t("for_sale_direct") + "</span>");
            }
            else {
                $row.find("td.status").html("<span class='label label-small label-info'>" + $.t("for_sale_indirect") + "</span>");
            }
        }
    };

    /*
      $("#sell_alias_add_message").on("change", function(e) {
      var $modal = $(this).closest(".modal");
      var $active = $modal.find(".nav li.active a").first();

      if ($active.attr("id") == "sell_alias_to_anyone") {
      $("#sell_alias_to_anyone_message_options").show();
      $("#sell_alias_to_specific_account_message_options").hide();
      }
      else {
      $("#sell_alias_to_anyone_message_options").hide();
      $("#sell_alias_to_specific_account_message_options").show();
      }
      });*/

    $("#sell_alias_to_specific_account, #sell_alias_to_anyone").on("click", function (e) {
        e.preventDefault();

        $(this).closest("ul").find("li").removeClass("active");
        $(this).parent().addClass("active");

        var $modal = $(this).closest(".modal");

        if ($(this).attr("id") === "sell_alias_to_anyone") {
            $modal.find("input[name=recipient]").val(BRS.genesisRS);
            $("#sell_alias_recipient_div").hide();
            $modal.find(".add_message_container, .optional_message").hide();
        }
        else {
            $modal.find("input[name=recipient]").val("");
            $("#sell_alias_recipient_div").show();
            $modal.find(".add_message_container").show();

            if ($("#sell_alias_add_message").is(":checked")) {
                $modal.find(".optional_message").show();
            }
            else {
                $modal.find(".optional_message").hide();
            }
        }

        $modal.find("input[name=converted_account_id]").val("");
        $modal.find(".callout").hide();
    });

    $("#buy_alias_modal").on("show.bs.modal", function (e) {
        var $modal = $(this);

        var $invoker = $(e.relatedTarget);

        BRS.fetchingModalData = true;

        var alias = String($invoker.data("alias"));

        BRS.sendRequest("getAlias", {
            "aliasName": alias
        }, function (response) {
            BRS.fetchingModalData = false;

            if (response.errorCode) {
                e.preventDefault();
                $.notify($.t("error_alias_not_found"), {
                    "type": "danger"
                });
            }
            else {
                if (!("priceNQT" in response)) {
                    e.preventDefault();
                    $.notify($.t("error_alias_not_for_sale"), {
                        "type": "danger"
                    });
                }
                else if (typeof response.buyer !== "undefined" && response.buyer !== BRS.account) {
                    e.preventDefault();
                    $.notify($.t("error_alias_sale_different_account"), {
                        "type": "danger"
                    });
                }
                else {
                    $modal.find("input[name=recipient]").val(String(response.accountRS).escapeHTML());
                    $modal.find("input[name=aliasName]").val(alias.escapeHTML());
                    $modal.find(".alias_name_display").html(alias.escapeHTML());
                    $modal.find("input[name=amountNXT]").val(BRS.convertToNXT(response.priceNQT)).prop("readonly", true);
                }
            }
        }, false);
    });

    BRS.forms.buyAliasError = function () {
        $("#buy_alias_modal").find("input[name=priceNXT]").prop("readonly", false);
    };

    BRS.forms.buyAliasComplete = function (response, data) {
        if (response.alreadyProcessed) {
            return;
        }

        if (BRS.currentPage !== "aliases") {
            return;
        }

        data.aliasName = String(data.aliasName).escapeHTML();
        data.aliasURI = "";

        $("#aliases_table tbody").prepend("<tr class='tentative' data-alias='" + data.aliasName.toLowerCase() + "'><td class='alias'>" + data.aliasName + "</td><td class='uri'>" + (data.aliasURI && data.aliasURI.indexOf("http") === 0 ? "<a href='" + data.aliasURI + "' target='_blank'>" + data.aliasURI + "</a>" : data.aliasURI) + "</td><td>/</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#'>" + $.t("edit") + "</a> <a class='btn btn-xs btn-default' href='#'>" + $.t("transfer") + "</a> <a class='btn btn-xs btn-default' href='#'>" + $.t("sell") + "</a></td></tr>");

        if ($("#aliases_table").parent().hasClass("data-empty")) {
            $("#aliases_table").parent().removeClass("data-empty");
        }
    };

    $("#register_alias_modal").on("show.bs.modal", function (e) {
        var $invoker = $(e.relatedTarget);

        var alias = $invoker.data("alias");

        if (alias) {
            BRS.fetchingModalData = true;

            alias = String(alias);

            BRS.sendRequest("getAlias", {
                "aliasName": alias
            }, function (response) {
                if (response.errorCode) {
                    e.preventDefault();
                    $.notify($.t("error_alias_not_found"), {
                        "type": "danger"
                    });
                    BRS.fetchingModalData = false;
                }
                else {
                    var aliasURI;

                    if (/http:\/\//i.test(response.aliasURI)) {
                        setAliasType("uri", response.aliasURI);
                    }
                    else if ((aliasURI = /acct:(.*)@burst/.exec(response.aliasURI)) || (aliasURI = /nacc:(.*)/.exec(response.aliasURI))) {
                        setAliasType("account", response.aliasURI);
                        response.aliasURI = String(aliasURI[1]).toUpperCase();
                    }
                    else {
                        setAliasType("general", response.aliasURI);
                    }

                    $("#register_alias_modal h4.modal-title").html($.t("update_alias"));
                    $("#register_alias_modal .btn-primary").html($.t("update"));
                    $("#register_alias_alias").val(alias.escapeHTML()).hide();
                    $("#register_alias_alias_noneditable").html(alias.escapeHTML()).show();
                    $("#register_alias_alias_update").val(1);
                }
            }, false);
        }
        else {
            $("#register_alias_modal h4.modal-title").html($.t("register_alias"));
            $("#register_alias_modal .btn-primary").html($.t("register"));

            var prefill = $invoker.data("prefill-alias");

            if (prefill) {
                $("#register_alias_alias").val(prefill).show();
            }
            else {
                $("#register_alias_alias").val("").show();
            }
            $("#register_alias_alias_noneditable").html("").hide();
            $("#register_alias_alias_update").val(0);
            setAliasType("uri", "");
        }
    });

    BRS.incoming.aliases = function (transactions) {
        if (BRS.hasTransactionUpdates(transactions)) {
            BRS.loadPage("aliases");
        }
    };

    BRS.forms.setAlias = function ($modal) {
        var data = BRS.getFormData($modal.find("form:first"));

        data.aliasURI = $.trim(data.aliasURI).toLowerCase();

        if (data.type === "account") {
            if (!(/acct:(.*)@burst/.test(data.aliasURI)) && !(/nacc:(.*)/.test(data.aliasURI))) {
                if (/^(BURST\-)/i.test(data.aliasURI)) {
                    var address = new NxtAddress();

                    if (!address.set(data.aliasURI)) {
                        return {
                            "error": $.t("error_invalid_account_id")
                        };
                    }
                    else {
                        data.aliasURI = "acct:" + data.aliasURI + "@burst";
                    }
                }
                else if (/^\d+$/.test(data.aliasURI)) {
                    data.aliasURI = "acct:" + data.aliasURI + "@burst";
                }
                else {
                    return {
                        "error": $.t("error_invalid_account_id")
                    };
                }
            }
        }

        delete data.type;

        return {
            "data": data
        };
    };

    function setAliasType(type, uri) {
        $("#register_alias_type").val(type);

        if (type == "uri") {
            $("#register_alias_uri.masked").trigger("unmask", true);
            $("#register_alias_uri_label").html($.t("uri"));
            $("#register_alias_uri").prop("placeholder", $.t("uri"));
            if (uri) {
                if (uri === BRS.accountRS) {
                    $("#register_alias_uri").val("http://");
                }
                else if (!/https?:\/\//i.test(uri)) {
                    $("#register_alias_uri").val("http://" + uri);
                }
                else {
                    $("#register_alias_uri").val(uri);
                }
            }
            else {
                $("#register_alias_uri").val("http://");
            }
            $("#register_alias_help").hide();
        }
        else if (type === "account") {
            $("#register_alias_uri_label").html($.t("account_id"));
            $("#register_alias_uri").prop("placeholder", $.t("account_id"));
            $("#register_alias_uri").val("").mask("BURST-****-****-****-*****");

            if (uri) {
                var match = uri.match(/acct:(.*)@burst/i);
                if (!match) {
                    match = uri.match(/nacc:(.*)/i);
                }

                if (match && match[1]) {
                    uri = match[1];
                }

                if (/^\d+$/.test(uri)) {
                    var address = new NxtAddress();

                    if (address.set(uri)) {
                        uri = address.toString();
                    }
                    else {
                        uri = "";
                    }
                }
                else if (!/^BURST\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{4}\-[A-Z0-9]{5}/i.test(uri)) {
                    uri = BRS.accountRS;
                }

                uri = uri.toUpperCase();

                $("#register_alias_uri").val(uri);
            }
            else {
                $("#register_alias_uri").val(BRS.accountRS);
            }
            $("#register_alias_help").html($.t("alias_account_help")).show();
        }
        else {
            $("#register_alias_uri.masked").trigger("unmask", true);
            $("#register_alias_uri_label").html($.t("data"));
            $("#register_alias_uri").prop("placeholder", $.t("data"));
            if (uri) {
                if (uri == BRS.accountRS) {
                    $("#register_alias_uri").val("");
                }
                else if (uri == "http://") {
                    $("#register_alias_uri").val("");
                }
                else {
                    $("#register_alias_uri").val(uri);
                }
            }
            $("#register_alias_help").html($.t("alias_data_help")).show();
        }
    }

    $("#register_alias_type").on("change", function () {
        var type = $(this).val();
        setAliasType(type, $("#register_alias_uri").val());
    });

    BRS.forms.setAliasError = function (response, data) {
        if (response && response.errorCode && response.errorCode == 8) {
            var errorDescription = response.errorDescription.escapeHTML();

            BRS.sendRequest("getAlias", {
                "aliasName": data.aliasName
            }, function (response) {
                var message;

                if (!response.errorCode) {
                    if ("priceNQT" in response) {
                        if (response.buyer == BRS.account) {
                            message = $.t("alias_sale_direct_offer", {
                                "burst": BRS.formatAmount(response.priceNQT)
                            }) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>";
                        }
                        else if (typeof response.buyer == "undefined") {
                            message = $.t("alias_sale_indirect_offer", {
                                "burst": BRS.formatAmount(response.priceNQT)
                            }) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>";
                        }
                        else {
                            message = $.t("error_alias_sale_different_account");
                        }
                    }
                    else {
                        message = "<a href='#' data-user='" + BRS.getAccountFormatted(response, "account") + "'>" + $.t("view_owner_info_q") + "</a>";
                    }

                    $("#register_alias_modal").find(".error_message").html(errorDescription + ". " + message);
                }
            }, false);
        }
    };

    BRS.forms.setAliasComplete = function (response, data) {
        if (response.alreadyProcessed) {
            return;
        }

        if (BRS.currentPage === "aliases") {
            data.aliasName = String(data.aliasName).escapeHTML();
            data.aliasURI = String(data.aliasURI);

            if (data.aliasURI.length > 100) {
                data.shortAliasURI = data.aliasURI.substring(0, 100) + "...";
                data.shortAliasURI = data.shortAliasURI.escapeHTML();
            }
            else {
                data.shortAliasURI = data.aliasURI.escapeHTML();
            }

            data.aliasURI = data.aliasURI.escapeHTML();

            var $table = $("#aliases_table tbody");

            var $row = $table.find("tr[data-alias=" + data.aliasName.toLowerCase() + "]");

            if ($row.length) {
                $row.addClass("tentative");
                $row.find("td.alias").html(data.aliasName);

                if (data.aliasURI && data.aliasURI.indexOf("http") === 0) {
                    $row.find("td.uri").html("<a href='" + data.aliasURI + "' target='_blank'>" + data.shortAliasURI + "</a>");
                }
                else {
                    $row.find("td.uri").html(data.shortAliasURI);
                }

                $.notify($.t("success_alias_update"), {
                    "type": "success"
                });
            }
            else {
                var $rows = $table.find("tr");

                var rowToAdd = "<tr class='tentative' data-alias='" + data.aliasName.toLowerCase() + "'><td class='alias'>" + data.aliasName + "</td><td class='uri'>" + (data.aliasURI && data.aliasURI.indexOf("http") === 0 ? "<a href='" + data.aliasURI + "' target='_blank'>" + data.shortAliasURI + "</a>" : data.shortAliasURI) + "</td><td>/</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#'>" + $.t("edit") + "</a> <a class='btn btn-xs btn-default' href='#'>" + $.t("transfer") + "</a> <a class='btn btn-xs btn-default' href='#'>" + $.t("sell") + "</a></td></tr>";

                var rowAdded = false;

                var newAlias = data.aliasName.toLowerCase();

                if ($rows.length) {
                    $rows.each(function () {
                        var alias = $(this).data("alias");

                        if (newAlias < alias) {
                            $(this).before(rowToAdd);
                            rowAdded = true;
                            return false;
                        }
                    });
                }

                if (!rowAdded) {
                    $table.append(rowToAdd);
                }

                if ($("#aliases_table").parent().hasClass("data-empty")) {
                    $("#aliases_table").parent().removeClass("data-empty");
                }

                $.notify($.t("success_alias_register"), {
                    "type": "success"
                });
            }
        }
    };

    $("#alias_search").on("submit", function (e) {
        e.preventDefault();

        if (BRS.fetchingModalData) {
            return;
        }

        BRS.fetchingModalData = true;

        var alias = $.trim($("#alias_search input[name=q]").val());

        $("#alias_info_table tbody").empty();

        BRS.sendRequest("getAlias", {
            "aliasName": alias
        }, function (response, input) {
            if (response.errorCode) {
                $.notify($.t("error_alias_not_found")
                    + " <a href='#' data-toggle='modal' data-target='#register_alias_modal' data-prefill-alias='"
                    + String(alias).escapeHTML() + "'>"
                    + $.t("register_q") + "</a>", {
                    "type": "danger"
                });
                BRS.fetchingModalData = false;
            }
            else {
                $("#alias_info_modal_alias").html(String(response.aliasName).escapeHTML());

                var data = {
                    "account": BRS.getAccountTitle(response, "account"),
                    "last_updated": BRS.formatTimestamp(response.timestamp),
                    "data_formatted_html": String(response.aliasURI).autoLink()
                };

                if ("priceNQT" in response) {
                    if (response.buyer === BRS.account) {
                        $("#alias_sale_callout").html($.t("alias_sale_direct_offer", {
                            "burst": BRS.formatAmount(response.priceNQT)
                        }) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>").show();
                    }
                    else if (typeof response.buyer === "undefined") {
                        $("#alias_sale_callout").html($.t("alias_sale_indirect_offer", {
                            "burst": BRS.formatAmount(response.priceNQT)
                        }) + " <a href='#' data-alias='" + String(response.aliasName).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>").show();
                    }
                    else {
                        $("#alias_sale_callout").html($.t("error_alias_sale_different_account")).show();
                    }
                }
                else {
                    $("#alias_sale_callout").hide();
                }

                $("#alias_info_table tbody").append(BRS.createInfoTable(data));

                $("#alias_info_modal").modal("show");
                BRS.fetchingModalData = false;
            }
        });
    });

    return BRS;
}(BRS || {}, jQuery));
