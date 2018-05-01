/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.assets = [];
    BRS.assetIds = [];
    BRS.closedGroups = [];
    BRS.assetSearch = false;
    BRS.lastIssuerCheck = false;
    BRS.viewingAsset = false; //viewing non-bookmarked asset
    BRS.currentAsset = {};
    BRS.assetTradeHistoryType = "everyone";
    var currentAssetID = 0;

    BRS.pages.asset_exchange = function(callback) {
        $(".content.content-stretch:visible").width($(".page:visible").width());

        if (BRS.databaseSupport) {
            BRS.assets = [];
            BRS.assetIds = [];

            BRS.database.select("assets", null, function(error, assets) {
                //select already bookmarked assets
                $.each(assets, function(index, asset) {
                    BRS.cacheAsset(asset);
                });

                //check owned assets, see if any are not yet in bookmarked assets
                if (BRS.accountInfo.unconfirmedAssetBalances) {
                    var newAssetIds = [];

                    $.each(BRS.accountInfo.unconfirmedAssetBalances, function(key, assetBalance) {
                        if (BRS.assetIds.indexOf(assetBalance.asset) == -1) {
                            newAssetIds.push(assetBalance.asset);
                            BRS.assetIds.push(assetBalance.asset);
                        }
                    });

                    //add to bookmarked assets
                    if (newAssetIds.length) {
                        var qs = [];

                        for (var i = 0; i < newAssetIds.length; i++) {
                            qs.push("assets=" + encodeURIComponent(newAssetIds[i]));
                        }

                        qs = qs.join("&");
                        //first get the assets info
                        BRS.sendRequest("getAssets+", {
                            //special request.. ugly hack.. also does POST due to URL max length
                            "querystring": qs
                        }, function(response) {
                            if (response.assets && response.assets.length) {
                                BRS.saveAssetBookmarks(response.assets, function() {
                                    BRS.loadAssetExchangeSidebar(callback);
                                });
                            } else {
                                BRS.loadAssetExchangeSidebar(callback);
                            }
                        });
                    } else {
                        BRS.loadAssetExchangeSidebar(callback);
                    }
                } else {
                    BRS.loadAssetExchangeSidebar(callback);
                }
            });
        } else {
            //for users without db support, we only need to fetch owned assets
            if (BRS.accountInfo.unconfirmedAssetBalances) {
                var qs = [];

                $.each(BRS.accountInfo.unconfirmedAssetBalances, function(key, assetBalance) {
                    if (BRS.assetIds.indexOf(assetBalance.asset) == -1) {
                        qs.push("assets=" + encodeURIComponent(assetBalance.asset));
                    }
                });

                qs = qs.join("&");

                if (qs) {
                    BRS.sendRequest("getAssets+", {
                        "querystring": qs
                    }, function(response) {
                        if (response.assets && response.assets.length) {
                            $.each(response.assets, function(key, asset) {
                                BRS.cacheAsset(asset);
                            });
                        }
                        BRS.loadAssetExchangeSidebar(callback);
                    });
                } else {
                    BRS.loadAssetExchangeSidebar(callback);
                }
            } else {
                BRS.loadAssetExchangeSidebar(callback);
            }
        }
    }

    BRS.cacheAsset = function(asset) {
        if (BRS.assetIds.indexOf(asset.asset) != -1) {
            return;
        }

        BRS.assetIds.push(asset.asset);

        if (!asset.groupName) {
            asset.groupName = "";
        }

        var asset = {
            "asset": String(asset.asset),
            "name": String(asset.name).toLowerCase(),
            "description": String(asset.description),
            "groupName": String(asset.groupName).toLowerCase(),
            "account": String(asset.account),
            "accountRS": String(asset.accountRS),
            "quantityQNT": String(asset.quantityQNT),
            "decimals": parseInt(asset.decimals, 10)
        };

        BRS.assets.push(asset);
    }

    BRS.forms.addAssetBookmark = function($modal) {
        var data = BRS.getFormData($modal.find("form:first"));

        data.id = $.trim(data.id);

        if (!data.id) {
            return {
                "error": $.t("error_asset_or_account_id_required")
            };
        }

        if (!/^\d+$/.test(data.id) && !/^BURST\-/i.test(data.id)) {
            return {
                "error": $.t("error_asset_or_account_id_invalid")
            };
        }

        if (/^BURST\-/i.test(data.id)) {
            BRS.sendRequest("getAssetsByIssuer", {
                "account": data.id
            }, function(response) {
                if (response.errorCode) {
                    BRS.showModalError(BRS.translateServerError(response), $modal);
                } else {
                    if (response.assets && response.assets[0] && response.assets[0].length) {
                        BRS.saveAssetBookmarks(response.assets[0], BRS.forms.addAssetBookmarkComplete);
                    } else {
                        BRS.showModalError($.t("account_no_assets"), $modal);
                    }
                    //BRS.saveAssetIssuer(data.id);
                }
            });
        } else {
            BRS.sendRequest("getAsset", {
                "asset": data.id
            }, function(response) {
                if (response.errorCode) {
                    BRS.sendRequest("getAssetsByIssuer", {
                        "account": data.id
                    }, function(response) {
                        if (response.errorCode) {
                            BRS.showModalError(BRS.translateServerError(response), $modal);
                        } else {
                            if (response.assets && response.assets[0] && response.assets[0].length) {
                                BRS.saveAssetBookmarks(response.assets[0], BRS.forms.addAssetBookmarkComplete);
                                //BRS.saveAssetIssuer(data.id);
                            } else {
                                BRS.showModalError($.t("no_asset_found"), $modal);
                            }
                        }
                    });
                } else {
                    BRS.saveAssetBookmarks(new Array(response), BRS.forms.addAssetBookmarkComplete);
                }
            });
        }
    }

    $("#asset_exchange_bookmark_this_asset").on("click", function() {
        if (BRS.viewingAsset) {
            BRS.saveAssetBookmarks(new Array(BRS.viewingAsset), function(newAssets) {
                BRS.viewingAsset = false;
                BRS.loadAssetExchangeSidebar(function() {
                    $("#asset_exchange_sidebar a[data-asset=" + newAssets[0].asset + "]").addClass("active").trigger("click");
                });
            });
        }
    });

    BRS.forms.addAssetBookmarkComplete = function(newAssets, submittedAssets) {
        BRS.assetSearch = false;

        if (newAssets.length == 0) {
            BRS.closeModal();
            $.notify($.t("error_asset_already_bookmarked", {
                "count": submittedAssets.length
            }), {
                "type": "danger"
            });
            $("#asset_exchange_sidebar a.active").removeClass("active");
            $("#asset_exchange_sidebar a[data-asset=" + submittedAssets[0].asset + "]").addClass("active").trigger("click");
            return;
        } else {
            BRS.closeModal();

            var message = $.t("success_asset_bookmarked", {
                "count": newAssets.length
            });

            if (!BRS.databaseSupport) {
                message += " " + $.t("error_assets_save_db");
            }

            $.notify(message, {
                "type": "success"
            });

            BRS.loadAssetExchangeSidebar(function(callback) {
                $("#asset_exchange_sidebar a.active").removeClass("active");
                $("#asset_exchange_sidebar a[data-asset=" + newAssets[0].asset + "]").addClass("active").trigger("click");
            });
        }
    }

    BRS.saveAssetBookmarks = function(assets, callback) {
        var newAssetIds = [];
        var newAssets = [];

        $.each(assets, function(key, asset) {
            var newAsset = {
                "asset": String(asset.asset),
                "name": String(asset.name),
                "description": String(asset.description),
                "account": String(asset.account),
                "accountRS": String(asset.accountRS),
                "quantityQNT": String(asset.quantityQNT),
                "decimals": parseInt(asset.decimals, 10),
                "groupName": ""
            };

            newAssets.push(newAsset);

            if (BRS.databaseSupport) {
                newAssetIds.push({
                    "asset": String(asset.asset)
                });
            } else if (BRS.assetIds.indexOf(asset.asset) == -1) {
                BRS.assetIds.push(asset.asset);
                newAsset.name = newAsset.name.toLowerCase();
                BRS.assets.push(newAsset);
            }
        });

        if (!BRS.databaseSupport) {
            if (callback) {
                callback(newAssets, assets);
            }
            return;
        }

        BRS.database.select("assets", newAssetIds, function(error, existingAssets) {
            var existingIds = [];

            if (existingAssets.length) {
                $.each(existingAssets, function(index, asset) {
                    existingIds.push(asset.asset);
                });

                newAssets = $.grep(newAssets, function(v) {
                    return (existingIds.indexOf(v.asset) === -1);
                });
            }

            if (newAssets.length == 0) {
                if (callback) {
                    callback([], assets);
                }
            } else {
                BRS.database.insert("assets", newAssets, function(error) {
                    $.each(newAssets, function(key, asset) {
                        asset.name = asset.name.toLowerCase();
                        BRS.assetIds.push(asset.asset);
                        BRS.assets.push(asset);
                    });

                    if (callback) {
                        //for some reason we need to wait a little or DB won't be able to fetch inserted record yet..
                        setTimeout(function() {
                            callback(newAssets, assets);
                        }, 50);
                    }
                });
            }
        });
    }

    BRS.positionAssetSidebar = function() {
        $("#asset_exchange_sidebar").parent().css("position", "relative");
        $("#asset_exchange_sidebar").parent().css("padding-bottom", "5px");
        //$("#asset_exchange_sidebar_content").height($(window).height() - 120);
        $("#asset_exchange_sidebar").height($(window).height() - 120);
    }

    //called on opening the asset exchange page and automatic refresh
    BRS.loadAssetExchangeSidebar = function(callback) {
        if (!BRS.assets.length) {
            BRS.pageLoaded(callback);
            $("#asset_exchange_sidebar_content").empty();
            $("#no_asset_selected, #loading_asset_data, #no_asset_search_results, #asset_details").hide();
            $("#no_assets_available").show();
            $("#asset_exchange_page").addClass("no_assets");
            return;
        }

        var rows = "";

        $("#asset_exchange_page").removeClass("no_assets");

        BRS.positionAssetSidebar();

        BRS.assets.sort(function(a, b) {
            if (!a.groupName && !b.groupName) {
                if (a.name > b.name) {
                    return 1;
                } else if (a.name < b.name) {
                    return -1;
                } else {
                    return 0;
                }
            } else if (!a.groupName) {
                return 1;
            } else if (!b.groupName) {
                return -1;
            } else if (a.groupName > b.groupName) {
                return 1;
            } else if (a.groupName < b.groupName) {
                return -1;
            } else {
                if (a.name > b.name) {
                    return 1;
                } else if (a.name < b.name) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        var lastGroup = "";
        var ungrouped = true;
        var isClosedGroup = false;

        var isSearch = BRS.assetSearch !== false;
        var searchResults = 0;

        for (var i = 0; i < BRS.assets.length; i++) {
            var asset = BRS.assets[i];

            if (isSearch) {
                if (BRS.assetSearch.indexOf(asset.asset) == -1) {
                    continue;
                } else {
                    searchResults++;
                }
            }

            if (asset.groupName.toLowerCase() != lastGroup) {
                var to_check = (asset.groupName ? asset.groupName : "undefined");

                if (BRS.closedGroups.indexOf(to_check) != -1) {
                    isClosedGroup = true;
                } else {
                    isClosedGroup = false;
                }

                if (asset.groupName) {
                    ungrouped = false;
                    rows += "<a href='#' class='list-group-item list-group-item-header" + (asset.groupName == "Ignore List" ? " no-context" : "") + "'" + (asset.groupName != "Ignore List" ? " data-context='asset_exchange_sidebar_group_context' " : "data-context=''") + " data-groupname='" + asset.groupName.escapeHTML() + "' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>" + asset.groupName.toUpperCase().escapeHTML() + "</h4><i class='fas fa-angle-" + (isClosedGroup ? "right" : "down") + " group_icon'></i></h4></a>";
                } else {
                    ungrouped = true;
                    rows += "<a href='#' class='list-group-item list-group-item-header no-context' data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>UNGROUPED <i class='fa pull-right fa-angle-" + (isClosedGroup ? "right" : "down") + "'></i></h4></a>";
                }

                lastGroup = asset.groupName.toLowerCase();
            }

            var ownsAsset = false;

            if (BRS.accountInfo.assetBalances) {
                $.each(BRS.accountInfo.assetBalances, function(key, assetBalance) {
                    if (assetBalance.asset == asset.asset && assetBalance.balanceQNT != "0") {
                        ownsAsset = true;
                        return false;
                    }
                });
            }

            rows += "<a href='#' class='list-group-item list-group-item-" + (ungrouped ? "ungrouped" : "grouped") + (ownsAsset ? " owns_asset" : " not_owns_asset") + "' data-cache='" + i + "' data-asset='" + String(asset.asset).escapeHTML() + "'" + (!ungrouped ? " data-groupname='" + asset.groupName.escapeHTML() + "'" : "") + (isClosedGroup ? " style='display:none'" : "") + " data-closed='" + isClosedGroup + "'><h4 class='list-group-item-heading'>" + asset.name.escapeHTML() + "</h4><p class='list-group-item-text'>qty: " + BRS.formatQuantity(asset.quantityQNT, asset.decimals) + "</p></a>";
        }

        var active = $("#asset_exchange_sidebar a.active");


        if (active.length) {
            active = active.data("asset");
        } else {
            active = false;
        }

        $("#asset_exchange_sidebar_content").empty().append(rows);
        $("#asset_exchange_sidebar_search").show();

        if (isSearch) {
            if (active && BRS.assetSearch.indexOf(active) != -1) {
                //check if currently selected asset is in search results, if so keep it at that
                $("#asset_exchange_sidebar a[data-asset=" + active + "]").addClass("active");
            } else if (BRS.assetSearch.length == 1) {
                //if there is only 1 search result, click it
                $("#asset_exchange_sidebar a[data-asset=" + BRS.assetSearch[0] + "]").addClass("active").trigger("click");
            }
        } else if (active) {
            $("#asset_exchange_sidebar a[data-asset=" + active + "]").addClass("active");
        }

        if (isSearch || BRS.assets.length >= 10) {
            $("#asset_exchange_sidebar_search").show();
        } else {
            $("#asset_exchange_sidebar_search").hide();
        }

        if (isSearch && BRS.assetSearch.length == 0) {
            $("#no_asset_search_results").show();
            $("#asset_details, #no_asset_selected, #no_assets_available").hide();
        } else if (!$("#asset_exchange_sidebar a.active").length) {
            $("#no_asset_selected").show();
            $("#asset_details, #no_assets_available, #no_asset_search_results").hide();
        } else if (active) {
            $("#no_assets_available, #no_asset_selected, #no_asset_search_results").hide();
        }

        if (BRS.viewingAsset) {
            $("#asset_exchange_bookmark_this_asset").show();
        } else {
            $("#asset_exchange_bookmark_this_asset").hide();
        }

        BRS.pageLoaded(callback);
    }

    BRS.incoming.asset_exchange = function() {
        if (!BRS.viewingAsset) {
            //refresh active asset
            var $active = $("#asset_exchange_sidebar a.active");

            if ($active.length) {
                $active.trigger("click", [{
                    "refresh": true
                }]);
            }
        } else {
            BRS.loadAsset(BRS.viewingAsset, true);
        }

        //update assets owned (colored)
        $("#asset_exchange_sidebar a.list-group-item.owns_asset").removeClass("owns_asset").addClass("not_owns_asset");

        if (BRS.accountInfo.assetBalances) {
            $.each(BRS.accountInfo.assetBalances, function(key, assetBalance) {
                if (assetBalance.balanceQNT != "0") {
                    $("#asset_exchange_sidebar a.list-group-item[data-asset=" + assetBalance.asset + "]").addClass("owns_asset").removeClass("not_owns_asset");
                }
            });
        }
    }

    $("#asset_exchange_sidebar").on("click", "a", function(e, data) {
        e.preventDefault();

        currentAssetID = String($(this).data("asset")).escapeHTML();

        //refresh is true if data is refreshed automatically by the system (when a new block arrives)
        if (data && data.refresh) {
            var refresh = true;
        } else {
            var refresh = false;
        }

        //clicked on a group
        if (!currentAssetID) {
            if (BRS.databaseSupport) {
                var group = $(this).data("groupname");
                var closed = $(this).data("closed");

                if (!group) {
                    var $links = $("#asset_exchange_sidebar a.list-group-item-ungrouped");
                } else {
                    var $links = $("#asset_exchange_sidebar a.list-group-item-grouped[data-groupname='" + group.escapeHTML() + "']");
                }

                if (!group) {
                    group = "undefined";
                }

                if (closed) {
                    var pos = BRS.closedGroups.indexOf(group);
                    if (pos >= 0) {
                        BRS.closedGroups.splice(pos);
                    }
                    $(this).data("closed", "");
                    $(this).find("i").removeClass("fa-angle-right").addClass("fa-angle-down");
                    $links.show();
                } else {
                    BRS.closedGroups.push(group);
                    $(this).data("closed", true);
                    $(this).find("i").removeClass("fa-angle-down").addClass("fa-angle-right");
                    $links.hide();
                }

                BRS.database.update("data", {
                    "contents": BRS.closedGroups.join("#")
                }, [{
                    "id": "closed_groups"
                }]);
            }

            return;
        }

        if (BRS.databaseSupport) {
            BRS.database.select("assets", [{
                "asset": currentAssetID
            }], function(error, asset) {
                if (asset && asset.length && asset[0].asset == currentAssetID) {
                    BRS.loadAsset(asset[0], refresh);
                }
            });
        } else {
            BRS.sendRequest("getAsset+", {
                "asset": currentAssetID
            }, function(response, input) {
                if (!response.errorCode && response.asset == currentAssetID) {
                    BRS.loadAsset(response, refresh);
                }
            });
        }
    });

    BRS.loadAsset = function(asset, refresh) {
        var assetId = asset.asset;

        BRS.currentAsset = asset;
        BRS.currentSubPage = assetId;

        if (!refresh) {
            $("#asset_exchange_sidebar a.active").removeClass("active");
            $("#asset_exchange_sidebar a[data-asset=" + assetId + "]").addClass("active");

            $("#no_asset_selected, #loading_asset_data, #no_assets_available, #no_asset_search_results").hide();
            $("#asset_details").show().parent().animate({
                "scrollTop": 0
            }, 0);

            $("#asset_account").html("<a href='#' data-user='" + BRS.getAccountFormatted(asset, "account") + "' class='user_info'>" + BRS.getAccountTitle(asset, "account") + "</a>");
            $("#asset_id").html(assetId.escapeHTML());
            $("#asset_decimals").html(String(asset.decimals).escapeHTML());
            $("#asset_name").html(String(asset.name).escapeHTML());
            $("#asset_description").html(String(asset.description).autoLink());
            $("#asset_quantity").html(BRS.formatQuantity(asset.quantityQNT, asset.decimals));

            $(".asset_name").html(String(asset.name).escapeHTML());
            $("#sell_asset_button").data("asset", assetId);
            $("#buy_asset_button").data("asset", assetId);
            $("#sell_asset_for_burst").html($.t("sell_asset_for_burst", {
                "assetName": String(asset.name).escapeHTML()
            }));
            $("#buy_asset_with_burst").html($.t("buy_asset_with_burst", {
                "assetName": String(asset.name).escapeHTML()
            }));
            $("#sell_asset_price, #buy_asset_price").val("");
            $("#sell_asset_quantity, #sell_asset_total, #buy_asset_quantity, #buy_asset_total").val("0");

            $("#asset_exchange_ask_orders_table tbody").empty();
            $("#asset_exchange_bid_orders_table tbody").empty();
            $("#asset_exchange_trade_history_table tbody").empty();
            $("#asset_exchange_ask_orders_table").parent().addClass("data-loading").removeClass("data-empty");
            $("#asset_exchange_bid_orders_table").parent().addClass("data-loading").removeClass("data-empty");
            $("#asset_exchange_trade_history_table").parent().addClass("data-loading").removeClass("data-empty");

            $(".data-loading img.loading").hide();

            setTimeout(function() {
                $(".data-loading img.loading").fadeIn(200);
            }, 200);

            var nrDuplicates = 0;

            $.each(BRS.assets, function(key, singleAsset) {
                if (String(singleAsset.name).toLowerCase() == String(asset.name).toLowerCase() && singleAsset.asset != assetId) {
                    nrDuplicates++;
                }
            });

            $("#asset_exchange_duplicates_warning").html($.t("asset_exchange_duplicates_warning", {
                "count": nrDuplicates
            }));

            if (BRS.databaseSupport) {
                BRS.sendRequest("getAsset", {
                    "asset": assetId
                }, function(response) {
                    if (!response.errorCode) {
                        if (response.asset != asset.asset || response.account != asset.account || response.accountRS != asset.accountRS || response.decimals != asset.decimals || response.description != asset.description || response.name != asset.name || response.quantityQNT != asset.quantityQNT) {
                            BRS.database.delete("assets", [{
                                "asset": asset.asset
                            }], function() {
                                setTimeout(function() {
                                    BRS.loadPage("asset_exchange");
                                    $.notify("Invalid asset.", {
                                        "type": "danger"
                                    });
                                }, 50);
                            });
                        }
                    }
                });
            }

            if (asset.viewingAsset) {
                $("#asset_exchange_bookmark_this_asset").show();
                BRS.viewingAsset = asset;
            } else {
                $("#asset_exchange_bookmark_this_asset").hide();
                BRS.viewingAsset = false;
            }
        }

        if (BRS.accountInfo.unconfirmedBalanceNQT == "0") {
            $("#your_burst_balance").html("0");
            $("#buy_automatic_price").addClass("zero").removeClass("nonzero");
        } else {
            $("#your_burst_balance").html(BRS.formatAmount(BRS.accountInfo.unconfirmedBalanceNQT));
            $("#buy_automatic_price").addClass("nonzero").removeClass("zero");
        }

        if (BRS.accountInfo.unconfirmedAssetBalances) {
            for (var i = 0; i < BRS.accountInfo.unconfirmedAssetBalances.length; i++) {
                var balance = BRS.accountInfo.unconfirmedAssetBalances[i];

                if (balance.asset == assetId) {
                    BRS.currentAsset.yourBalanceNQT = balance.unconfirmedBalanceQNT;
                    $("#your_asset_balance").html(BRS.formatQuantity(balance.unconfirmedBalanceQNT, BRS.currentAsset.decimals));
                    if (balance.unconfirmedBalanceQNT == "0") {
                        $("#sell_automatic_price").addClass("zero").removeClass("nonzero");
                    } else {
                        $("#sell_automatic_price").addClass("nonzero").removeClass("zero");
                    }
                    break;
                }
            }
        }

        if (!BRS.currentAsset.yourBalanceNQT) {
            BRS.currentAsset.yourBalanceNQT = "0";
            $("#your_asset_balance").html("0");
        }

        BRS.loadAssetOrders("ask", assetId, refresh);
        BRS.loadAssetOrders("bid", assetId, refresh);

        //todo BRS.currentSubPageID ??...
        BRS.sendRequest("getTrades+" + assetId, {
            "asset": assetId,
            "account": ($("#ae_show_my_trades_only").is(":checked")) ? $("#account_id").text() : "",
            "firstIndex": 0,
            "lastIndex": 50
        }, function(response, input) {
            if (response.trades && response.trades.length) {
                var trades = response.trades;

                var rows = "";

                for (var i = 0; i < trades.length; i++) {
                    trades[i].priceNQT = new BigInteger(trades[i].priceNQT);
                    trades[i].quantityQNT = new BigInteger(trades[i].quantityQNT);
                    trades[i].totalNQT = new BigInteger(BRS.calculateOrderTotalNQT(trades[i].priceNQT, trades[i].quantityQNT));

                    rows += "<tr><td>" + BRS.formatTimestamp(trades[i].timestamp) + "</td><td>" + BRS.formatQuantity(trades[i].quantityQNT, BRS.currentAsset.decimals) + "</td><td class='asset_price'>" + BRS.formatOrderPricePerWholeQNT(trades[i].priceNQT, BRS.currentAsset.decimals) + "</td><td>" + BRS.formatAmount(trades[i].totalNQT) + "</td><td>" + String(trades[i].askOrder).escapeHTML() + "</td><td>" + String(trades[i].bidOrder).escapeHTML() + "</td></tr>";
                }

                $("#asset_exchange_trade_history_table tbody").empty().append(rows);
                BRS.dataLoadFinished($("#asset_exchange_trade_history_table"), !refresh);
            } else {
                $("#asset_exchange_trade_history_table tbody").empty();
                BRS.dataLoadFinished($("#asset_exchange_trade_history_table"), !refresh);
            }
        });
    }

    // if this is clicked we can assume there is asset selected
    // might need to implement some safety check just in case.
    // LithStud 2016.11.17
    $("#ae_show_my_trades_only").on("change", function() {
        $("#asset_exchange_sidebar a.active").trigger("click");
    });

    BRS.loadAssetOrders = function(type, assetId, refresh) {
        type = type.toLowerCase();

        BRS.sendRequest("get" + type.capitalize() + "Orders+" + assetId, {
            "asset": assetId,
            "timestamp": 0,
            "limit": 50
        }, function(response, input) {
            var orders = response[type + "Orders"];

            if (!orders) {
                orders = [];
            }

            if (BRS.unconfirmedTransactions.length) {
                var added = false;

                for (var i = 0; i < BRS.unconfirmedTransactions.length; i++) {
                    var unconfirmedTransaction = BRS.unconfirmedTransactions[i];
                    unconfirmedTransaction.order = unconfirmedTransaction.transaction;

                    if (unconfirmedTransaction.type == 2 && (type == "ask" ? unconfirmedTransaction.subtype == 2 : unconfirmedTransaction.subtype == 3) && unconfirmedTransaction.asset == assetId) {
                        orders.push($.extend(true, {}, unconfirmedTransaction)); //make sure it's a deep copy
                        added = true;
                    }
                }

                if (added) {
                    orders.sort(function(a, b) {
                        if (type == "ask") {
                            //lowest price at the top
                            return new BigInteger(a.priceNQT).compareTo(new BigInteger(b.priceNQT));
                        } else {
                            //highest price at the top
                            return new BigInteger(b.priceNQT).compareTo(new BigInteger(a.priceNQT));
                        }
                    });
                }
            }

            if (orders.length) {
                $("#" + (type == "ask" ? "sell" : "buy") + "_orders_count").html("(" + orders.length + (orders.length == 50 ? "+" : "") + ")");

                var rows = "";

                for (var i = 0; i < orders.length; i++) {
                    var order = orders[i];

                    order.priceNQT = new BigInteger(order.priceNQT);
                    order.quantityQNT = new BigInteger(order.quantityQNT);
                    order.totalNQT = new BigInteger(BRS.calculateOrderTotalNQT(order.quantityQNT, order.priceNQT));

                    if (i == 0 && !refresh) {
                        $("#" + (type == "ask" ? "buy" : "sell") + "_asset_price").val(BRS.calculateOrderPricePerWholeQNT(order.priceNQT, BRS.currentAsset.decimals));
                    }

                    var className = (order.account == BRS.account ? "your-order" : "") + (order.unconfirmed ? " tentative" : (BRS.isUserCancelledOrder(order) ? " tentative tentative-crossed" : ""));

                    rows += "<tr class='" + className + "' data-transaction='" + String(order.order).escapeHTML() + "' data-quantity='" + order.quantityQNT.toString().escapeHTML() + "' data-price='" + order.priceNQT.toString().escapeHTML() + "'><td>" + (order.unconfirmed ? "You - <strong>Pending</strong>" : (order.account == BRS.account ? "<strong>You</strong>" : "<a href='#' data-user='" + BRS.getAccountFormatted(order, "account") + "' class='user_info'>" + (order.account == BRS.currentAsset.account ? "Asset Issuer" : BRS.getAccountTitle(order, "account")) + "</a>")) + "</td><td>" + BRS.formatQuantity(order.quantityQNT, BRS.currentAsset.decimals) + "</td><td>" + BRS.formatOrderPricePerWholeQNT(order.priceNQT, BRS.currentAsset.decimals) + "</td><td>" + BRS.formatAmount(order.totalNQT) + "</tr>";
                }

                $("#asset_exchange_" + type + "_orders_table tbody").empty().append(rows);
            } else {
                $("#asset_exchange_" + type + "_orders_table tbody").empty();
                if (!refresh) {
                    $("#" + (type == "ask" ? "buy" : "sell") + "_asset_price").val("0");
                }
                $("#" + (type == "ask" ? "sell" : "buy") + "_orders_count").html("");
            }

            BRS.dataLoadFinished($("#asset_exchange_" + type + "_orders_table"), !refresh);
        });
    }

    BRS.isUserCancelledOrder = function(order) {
        if (BRS.unconfirmedTransactions.length) {
            for (var i = 0; i < BRS.unconfirmedTransactions.length; i++) {
                var unconfirmedTransaction = BRS.unconfirmedTransactions[i];

                if (unconfirmedTransaction.type == 2 && (order.type == "ask" ? unconfirmedTransaction.subtype == 4 : unconfirmedTransaction.subtype == 5) && unconfirmedTransaction.attachment.order == order.order) {
                    return true;
                }
            }
        }

        return false;
    }

    $("#asset_exchange_search").on("submit", function(e) {
        e.preventDefault();
        $("#asset_exchange_search input[name=q]").trigger("input");
    });

    $("#asset_exchange_search input[name=q]").on("input", function(e) {
        var input = $.trim($(this).val()).toLowerCase();

        if (!input) {
            BRS.assetSearch = false;
            BRS.loadAssetExchangeSidebar();
            $("#asset_exchange_clear_search").hide();
        } else {
            BRS.assetSearch = [];

            if (/BURST\-/i.test(input)) {
                $.each(BRS.assets, function(key, asset) {
                    if (asset.accountRS.toLowerCase() == input || asset.accountRS.toLowerCase().indexOf(input) !== -1) {
                        BRS.assetSearch.push(asset.asset);
                    }
                });
            } else {
                $.each(BRS.assets, function(key, asset) {
                    if (asset.account == input || asset.asset == input || asset.name.toLowerCase().indexOf(input) !== -1) {
                        BRS.assetSearch.push(asset.asset);
                    }
                });
            }

            BRS.loadAssetExchangeSidebar();
            $("#asset_exchange_clear_search").show();
            $("#asset_exchange_show_type").hide();
        }
    });

    $("#asset_exchange_clear_search").on("click", function() {
        $("#asset_exchange_search input[name=q]").val("");
        $("#asset_exchange_search").trigger("submit");
    });

    $("#buy_asset_box .box-header, #sell_asset_box .box-header").click(function(e) {
        e.preventDefault();
        //Find the box parent
        var box = $(this).parents(".box").first();
        //Find the body and the footer
        var bf = box.find(".box-body, .box-footer");
        if (!box.hasClass("collapsed-box")) {
            box.addClass("collapsed-box");
            $(this).find(".btn i.fa").removeClass("fa-minus").addClass("fa-plus");
            bf.slideUp();
        } else {
            box.removeClass("collapsed-box");
            bf.slideDown();
            $(this).find(".btn i.fa").removeClass("fa-plus").addClass("fa-minus");
        }
    });

    $("#asset_exchange_bid_orders_table tbody, #asset_exchange_ask_orders_table tbody").on("click", "td", function(e) {
        var $target = $(e.target);

        if ($target.prop("tagName").toLowerCase() == "a") {
            return;
        }

        var type = ($target.closest("table").attr("id") == "asset_exchange_bid_orders_table" ? "sell" : "buy");

        var $tr = $target.closest("tr");

        try {
            var priceNQT = new BigInteger(String($tr.data("price")));
            var quantityQNT = new BigInteger(String($tr.data("quantity")));
            var totalNQT = new BigInteger(BRS.calculateOrderTotalNQT(quantityQNT, priceNQT));

            $("#" + type + "_asset_price").val(BRS.calculateOrderPricePerWholeQNT(priceNQT, BRS.currentAsset.decimals));
            $("#" + type + "_asset_quantity").val(BRS.convertToQNTf(quantityQNT, BRS.currentAsset.decimals));
            $("#" + type + "_asset_total").val(BRS.convertToNXT(totalNQT));
        } catch (err) {
            return;
        }

        if (type == "sell") {
            try {
                var balanceNQT = new BigInteger(BRS.accountInfo.unconfirmedBalanceNQT);
            } catch (err) {
                return;
            }

            if (totalNQT.compareTo(balanceNQT) > 0) {
                $("#" + type + "_asset_total").css({
                    "background": "#ED4348",
                    "color": "white"
                });
            } else {
                $("#" + type + "_asset_total").css({
                    "background": "",
                    "color": ""
                });
            }
        }

        var box = $("#" + type + "_asset_box");

        if (box.hasClass("collapsed-box")) {
            box.removeClass("collapsed-box");
            box.find(".box-body").slideDown();
        }
    });

    $("#sell_automatic_price, #buy_automatic_price").on("click", function(e) {
        try {
            var type = ($(this).attr("id") == "sell_automatic_price" ? "sell" : "buy");

            var price = new Big(BRS.convertToNQT(String($("#" + type + "_asset_price").val())));
            var balance = new Big(type == "buy" ? BRS.accountInfo.unconfirmedBalanceNQT : BRS.currentAsset.yourBalanceNQT);
            var balanceNQT = new Big(BRS.accountInfo.unconfirmedBalanceNQT);
            var maxQuantity = new Big(BRS.convertToQNTf(BRS.currentAsset.quantityQNT, BRS.currentAsset.decimals));

            if (balance.cmp(new Big("0")) <= 0) {
                return;
            }

            if (price.cmp(new Big("0")) <= 0) {
                //get minimum price if no offers exist, based on asset decimals..
                price = new Big("" + Math.pow(10, BRS.currentAsset.decimals));
                $("#" + type + "_asset_price").val(BRS.convertToNXT(price.toString()));
            }

            var quantity = new Big(BRS.amountToPrecision((type == "sell" ? balanceNQT : balance).div(price).toString(), BRS.currentAsset.decimals));

            var total = quantity.times(price);

            //proposed quantity is bigger than available quantity
            if (quantity.cmp(maxQuantity) == 1) {
                quantity = maxQuantity;
                total = quantity.times(price);
            }

            if (type == "sell") {
                var maxUserQuantity = new Big(BRS.convertToQNTf(balance, BRS.currentAsset.decimals));
                if (quantity.cmp(maxUserQuantity) == 1) {
                    quantity = maxUserQuantity;
                    total = quantity.times(price);
                }
            }

            $("#" + type + "_asset_quantity").val(quantity.toString());
            $("#" + type + "_asset_total").val(BRS.convertToNXT(total.toString()));

            $("#" + type + "_asset_total").css({
                "background": "",
                "color": ""
            });
        } catch (err) {}
    });

    function isControlKey(charCode) {
        if (charCode >= 32)
            return false;
        if (charCode == 10)
            return false;
        if (charCode == 13)
            return false;

        return true;
    }

    $("#buy_asset_quantity, #buy_asset_price, #sell_asset_quantity, #sell_asset_price, #buy_asset_fee, #sell_asset_fee").keydown(function(e) {
        var charCode = !e.charCode ? e.which : e.charCode;

        if (isControlKey(charCode) || e.ctrlKey || e.metaKey) {
            return;
        }

        var isQuantityField = /_quantity/i.test($(this).attr("id"));

        var maxFractionLength = (isQuantityField ? BRS.currentAsset.decimals : 8 - BRS.currentAsset.decimals);

        if (maxFractionLength) {
            //allow 1 single period character
            if (charCode == 110 || charCode == 190) {
                if ($(this).val().indexOf(".") != -1) {
                    e.preventDefault();
                    return false;
                } else {
                    return;
                }
            }
        } else {
            //do not allow period
            if (charCode == 110 || charCode == 190 || charCode == 188) {
                $.notify($.t("error_fractions"), {
                    "type": "danger"
                });
                e.preventDefault();
                return false;
            }
        }

        var input = $(this).val() + String.fromCharCode(charCode);

        var afterComma = input.match(/\.(\d*)$/);

        //only allow as many as there are decimals allowed..
        if (afterComma && afterComma[1].length > maxFractionLength) {
            var selectedText = BRS.getSelectedText();

            if (selectedText != $(this).val()) {
                var errorMessage;

                if (isQuantityField) {
                    errorMessage = $.t("error_asset_decimals", {
                        "count": (0 + BRS.currentAsset.decimals)
                    });
                } else {
                    errorMessage = $.t("error_decimals", {
                        "count": (8 - BRS.currentAsset.decimals)
                    });
                }

                $.notify(errorMessage, {
                    "type": "danger"
                });

                e.preventDefault();
                return false;
            }
        }

        //numeric characters, left/right key, backspace, delete
        if (charCode == 8 || charCode == 37 || charCode == 39 || charCode == 46 || (charCode >= 48 && charCode <= 57 && !isNaN(String.fromCharCode(charCode))) || (charCode >= 96 && charCode <= 105)) {
            return;
        } else {
            //comma
            if (charCode == 188) {
                $.notify($.t("error_comma_not_allowed"), {
                    "type": "danger"
                });
            }
            e.preventDefault();
            return false;
        }
    });

    //calculate preview price (calculated on every keypress)
    $("#sell_asset_quantity, #sell_asset_price, #buy_asset_quantity, #buy_asset_price").keyup(function(e) {
        var orderType = $(this).data("type").toLowerCase();

        try {
            var quantityQNT = new BigInteger(BRS.convertToQNT(String($("#" + orderType + "_asset_quantity").val()), BRS.currentAsset.decimals));
            var priceNQT = new BigInteger(BRS.calculatePricePerWholeQNT(BRS.convertToNQT(String($("#" + orderType + "_asset_price").val())), BRS.currentAsset.decimals));

            if (priceNQT.toString() == "0" || quantityQNT.toString() == "0") {
                $("#" + orderType + "_asset_total").val("0");
            } else {
                var total = BRS.calculateOrderTotal(quantityQNT, priceNQT, BRS.currentAsset.decimals);
                $("#" + orderType + "_asset_total").val(total.toString());
            }
        } catch (err) {
            $("#" + orderType + "_asset_total").val("0");
        }
    });

    $("#asset_order_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);

        var orderType = $invoker.data("type");
        var assetId = $invoker.data("asset");

        $("#asset_order_modal_button").html(orderType + " Asset").data("resetText", orderType + " Asset");

        orderType = orderType.toLowerCase();

        try {
            //TODO
            var quantity = String($("#" + orderType + "_asset_quantity").val());
            var quantityQNT = new BigInteger(BRS.convertToQNT(quantity, BRS.currentAsset.decimals));
            var priceNQT = new BigInteger(BRS.calculatePricePerWholeQNT(BRS.convertToNQT(String($("#" + orderType + "_asset_price").val())), BRS.currentAsset.decimals));
            var feeNQT = new BigInteger(BRS.convertToNQT(String($("#" + orderType + "_asset_fee").val())));
            var totalNXT = BRS.formatAmount(BRS.calculateOrderTotalNQT(quantityQNT, priceNQT, BRS.currentAsset.decimals), false, true);
        } catch (err) {
            $.notify("Invalid input.", {
                "type": "danger"
            });
            return e.preventDefault();
        }

        if (priceNQT.toString() == "0" || quantityQNT.toString() == "0") {
            $.notify($.t("error_amount_price_required"), {
                "type": "danger"
            });
            return e.preventDefault();
        }

        if (feeNQT.toString() == "0") {
            feeNQT = new BigInteger("100000000");
        }

        var priceNQTPerWholeQNT = priceNQT.multiply(new BigInteger("" + Math.pow(10, BRS.currentAsset.decimals)));

        if (orderType == "buy") {
            var description = $.t("buy_order_description", {
                "quantity": BRS.formatQuantity(quantityQNT, BRS.currentAsset.decimals, true),
                "asset_name": $("#asset_name").html().escapeHTML(),
                "burst": BRS.formatAmount(priceNQTPerWholeQNT)
            });
            var tooltipTitle = $.t("buy_order_description_help", {
                "burst": BRS.formatAmount(priceNQTPerWholeQNT, false, true),
                "total_burst": totalNXT
            });
        } else {
            var description = $.t("sell_order_description", {
                "quantity": BRS.formatQuantity(quantityQNT, BRS.currentAsset.decimals, true),
                "asset_name": $("#asset_name").html().escapeHTML(),
                "burst": BRS.formatAmount(priceNQTPerWholeQNT)
            });
            var tooltipTitle = $.t("sell_order_description_help", {
                "burst": BRS.formatAmount(priceNQTPerWholeQNT, false, true),
                "total_burst": totalNXT
            });
        }

        $("#asset_order_description").html(description);
        $("#asset_order_total").html(totalNXT + " BURST");
        $("#asset_order_fee_paid").html(BRS.formatAmount(feeNQT) + " BURST");

        if (quantity != "1") {
            $("#asset_order_total_tooltip").show();
            $("#asset_order_total_tooltip").popover("destroy");
            $("#asset_order_total_tooltip").data("content", tooltipTitle);
            $("#asset_order_total_tooltip").popover({
                "content": tooltipTitle,
                "trigger": "hover"
            });
        } else {
            $("#asset_order_total_tooltip").hide();
        }

        $("#asset_order_type").val((orderType == "buy" ? "placeBidOrder" : "placeAskOrder"));
        $("#asset_order_asset").val(assetId);
        $("#asset_order_quantity").val(quantityQNT.toString());
        $("#asset_order_price").val(priceNQT.toString());
        $("#asset_order_fee").val(feeNQT.toString());
    });

    BRS.forms.orderAsset = function($modal) {
        var orderType = $("#asset_order_type").val();

        return {
            "requestType": orderType,
            "successMessage": (orderType == "placeBidOrder" ? $.t("success_buy_order_asset") : $.t("success_sell_order_asset")),
            "errorMessage": $.t("error_order_asset")
        };
    }

    BRS.forms.orderAssetComplete = function(response, data) {
        if (response.alreadyProcessed) {
            return;
        }

        if (data.requestType == "placeBidOrder") {
            var $table = $("#asset_exchange_bid_orders_table tbody");
        } else {
            var $table = $("#asset_exchange_ask_orders_table tbody");
        }

        if ($table.find("tr[data-transaction='" + String(response.transaction).escapeHTML() + "']").length) {
            return;
        }

        var $rows = $table.find("tr");

        data.quantityQNT = new BigInteger(data.quantityQNT);
        data.priceNQT = new BigInteger(data.priceNQT);
        data.totalNQT = new BigInteger(BRS.calculateOrderTotalNQT(data.quantityQNT, data.priceNQT));

        var rowToAdd = "<tr class='tentative' data-transaction='" + String(response.transaction).escapeHTML() + "' data-quantity='" + data.quantityQNT.toString().escapeHTML() + "' data-price='" + data.priceNQT.toString().escapeHTML() + "'><td>You - <strong>Pending</strong></td><td>" + BRS.formatQuantity(data.quantityQNT, BRS.currentAsset.decimals) + "</td><td>" + BRS.formatOrderPricePerWholeQNT(data.priceNQT, BRS.currentAsset.decimals) + "</td><td>" + BRS.formatAmount(data.totalNQT) + "</td></tr>";

        var rowAdded = false;

        if ($rows.length) {
            $rows.each(function() {
                var rowPrice = new BigInteger(String($(this).data("price")));

                if (data.requestType == "placeBidOrder" && data.priceNQT.compareTo(rowPrice) > 0) {
                    $(this).before(rowToAdd);
                    rowAdded = true;
                    return false;
                } else if (data.requestType == "placeAskOrder" && data.priceNQT.compareTo(rowPrice) < 0) {
                    $(this).before(rowToAdd);
                    rowAdded = true;
                    return false;
                }
            });
        }

        if (!rowAdded) {
            $table.append(rowToAdd);
            $table.parent().parent().removeClass("data-empty").parent().addClass("no-padding");
        }
    }

    BRS.forms.issueAsset = function($modal) {
        var data = BRS.getFormData($modal.find("form:first"));

        data.description = $.trim(data.description);

        if (!data.description) {
            return {
                "error": $.t("error_description_required")
            };
        } else if (!/^\d+$/.test(data.quantity)) {
            return {
                "error": $.t("error_whole_quantity")
            };
        } else {
            data.quantityQNT = String(data.quantity);

            if (data.decimals > 0) {
                for (var i = 0; i < data.decimals; i++) {
                    data.quantityQNT += "0";
                }
            }

            delete data.quantity;

            return {
                "data": data
            };
        }
    }

    $("#asset_exchange_sidebar_group_context").on("click", "a", function(e) {
        e.preventDefault();

        var groupName = BRS.selectedContext.data("groupname");
        var option = $(this).data("option");

        if (option == "change_group_name") {
            $("#asset_exchange_change_group_name_old_display").html(groupName.escapeHTML());
            $("#asset_exchange_change_group_name_old").val(groupName);
            $("#asset_exchange_change_group_name_new").val("");
            $("#asset_exchange_change_group_name_modal").modal("show");
        }
    });

    BRS.forms.assetExchangeChangeGroupName = function($modal) {
        var oldGroupName = $("#asset_exchange_change_group_name_old").val();
        var newGroupName = $("#asset_exchange_change_group_name_new").val();

        if (!newGroupName.match(/^[a-z0-9 ]+$/i)) {
            return {
                "error": $.t("error_group_name")
            };
        }

        BRS.database.update("assets", {
            "groupName": newGroupName
        }, [{
            "groupName": oldGroupName
        }], function() {
            setTimeout(function() {
                BRS.loadPage("asset_exchange");
                $.notify($.t("success_group_name_update"), {
                    "type": "success"
                });
            }, 50);
        });

        return {
            "stop": true
        };
    }

    $("#asset_exchange_sidebar_context").on("click", "a", function(e) {
        e.preventDefault();

        var assetId = BRS.selectedContext.data("asset");
        var option = $(this).data("option");

        BRS.closeContextMenu();

        if (option == "add_to_group") {
            $("#asset_exchange_group_asset").val(assetId);

            BRS.database.select("assets", [{
                "asset": assetId
            }], function(error, asset) {
                asset = asset[0];

                $("#asset_exchange_group_title").html(String(asset.name).escapeHTML());

                BRS.database.select("assets", [], function(error, assets) {
                    //BRS.database.execute("SELECT DISTINCT groupName FROM assets", [], function(groupNames) {
                    var groupNames = [];

                    $.each(assets, function(index, asset) {
                        if (asset.groupName && $.inArray(asset.groupName, groupNames) == -1) {
                            groupNames.push(asset.groupName);
                        }
                    });

                    assets = [];

                    groupNames.sort(function(a, b) {
                        if (a.toLowerCase() > b.toLowerCase()) {
                            return 1;
                        } else if (a.toLowerCase() < b.toLowerCase()) {
                            return -1;
                        } else {
                            return 0;
                        }
                    });

                    var groupSelect = $("#asset_exchange_group_group");

                    groupSelect.empty();

                    $.each(groupNames, function(index, groupName) {
                        groupSelect.append("<option value='" + groupName.escapeHTML() + "'" + (asset.groupName && asset.groupName.toLowerCase() == groupName.toLowerCase() ? " selected='selected'" : "") + ">" + groupName.escapeHTML() + "</option>");
                    });

                    groupSelect.append("<option value='0'" + (!asset.groupName ? " selected='selected'" : "") + ">None</option>");
                    groupSelect.append("<option value='-1'>New group</option>");

                    $("#asset_exchange_group_modal").modal("show");
                });
            });
        } else if (option == "remove_from_group") {
            BRS.database.update("assets", {
                "groupName": ""
            }, [{
                "asset": assetId
            }], function() {
                setTimeout(function() {
                    BRS.loadPage("asset_exchange");
                    $.notify($.t("success_asset_group_removal"), {
                        "type": "success"
                    });
                }, 50);
            });
        } else if (option == "remove_from_bookmarks") {
            var ownsAsset = false;

            if (BRS.accountInfo.unconfirmedAssetBalances) {
                $.each(BRS.accountInfo.unconfirmedAssetBalances, function(key, assetBalance) {
                    if (assetBalance.asset == assetId) {
                        ownsAsset = true;
                        return false;
                    }
                });
            }

            if (ownsAsset) {
                $.notify($.t("error_owned_asset_no_removal"), {
                    "type": "danger"
                });
            } else {
                //todo save delteed asset ids from accountissuers
                BRS.database.delete("assets", [{
                    "asset": assetId
                }], function(error, affected) {
                    setTimeout(function() {
                        BRS.loadPage("asset_exchange");
                        $.notify($.t("success_asset_bookmark_removal"), {
                            "type": "success"
                        });
                    }, 50);
                });
            }
        }
    });

    $("#asset_exchange_group_group").on("change", function() {
        var value = $(this).val();

        if (value == -1) {
            $("#asset_exchange_group_new_group_div").show();
        } else {
            $("#asset_exchange_group_new_group_div").hide();
        }
    });

    BRS.forms.assetExchangeGroup = function($modal) {
        var assetId = $("#asset_exchange_group_asset").val();
        var groupName = $("#asset_exchange_group_group").val();

        if (groupName == 0) {
            groupName = "";
        } else if (groupName == -1) {
            groupName = $("#asset_exchange_group_new_group").val();
        }

        BRS.database.update("assets", {
            "groupName": groupName
        }, [{
            "asset": assetId
        }], function() {
            setTimeout(function() {
                BRS.loadPage("asset_exchange");
                if (!groupName) {
                    $.notify($.t("success_asset_group_removal"), {
                        "type": "success"
                    });
                } else {
                    $.notify($.t("sucess_asset_group_add"), {
                        "type": "success"
                    });
                }
            }, 50);
        });

        return {
            "stop": true
        };
    }

    $("#asset_exchange_group_modal").on("hidden.bs.modal", function(e) {
        $("#asset_exchange_group_new_group_div").val("").hide();
    });

    /* TRANSFER HISTORY PAGE */
    BRS.pages.transfer_history = function() {
        BRS.sendRequest("getAssetTransfers+", {
            "account": BRS.accountRS,
            "firstIndex": BRS.pageNumber * BRS.itemsPerPage - BRS.itemsPerPage,
            "lastIndex": BRS.pageNumber * BRS.itemsPerPage
        }, function(response, input) {
            if (response.transfers && response.transfers.length) {
                if (response.transfers.length > BRS.itemsPerPage) {
                    BRS.hasMorePages = true;
                    response.transfers.pop();
                }

                var transfers = response.transfers;

                var rows = "";

                for (var i = 0; i < transfers.length; i++) {
                    transfers[i].quantityQNT = new BigInteger(transfers[i].quantityQNT);

                    var type = (transfers[i].recipientRS == BRS.accountRS ? "receive" : "send");

                    rows += "<tr><td><a href='#' data-transaction='" + String(transfers[i].assetTransfer).escapeHTML() + "'>" + String(transfers[i].assetTransfer).escapeHTML() + "</a></td><td><a href='#' data-goto-asset='" + String(transfers[i].asset).escapeHTML() + "'>" + String(transfers[i].name).escapeHTML() + "</a></td><td>" + BRS.formatTimestamp(transfers[i].timestamp) + "</td><td style='color:" + (type == "receive" ? "green" : "red") + "'>" + BRS.formatQuantity(transfers[i].quantityQNT, transfers[i].decimals) + "</td>" +
                        "<td><a href='#' data-user='" + BRS.getAccountFormatted(transfers[i], "recipient") + "' class='user_info'>" + BRS.getAccountTitle(transfers[i], "recipient") + "</a></td>" +
                        "<td><a href='#' data-user='" + BRS.getAccountFormatted(transfers[i], "sender") + "' class='user_info'>" + BRS.getAccountTitle(transfers[i], "sender") + "</a></td>" +
                        "</tr>";
                }

                BRS.dataLoaded(rows);
            } else {
                BRS.dataLoaded();
            }
        });
    }

    /* MY ASSETS PAGE */
    BRS.pages.my_assets = function() {
        if (BRS.accountInfo.assetBalances && BRS.accountInfo.assetBalances.length) {
            var result = {
                "assets": [],
                "bid_orders": {},
                "ask_orders": {}
            };
            var count = {
                "total_assets": BRS.accountInfo.assetBalances.length,
                "assets": 0,
                "ignored_assets": 0,
                "ask_orders": 0,
                "bid_orders": 0
            };

            for (var i = 0; i < BRS.accountInfo.assetBalances.length; i++) {
                if (BRS.accountInfo.assetBalances[i].balanceQNT == "0") {
                    count.ignored_assets++;
                    if (BRS.checkMyAssetsPageLoaded(count)) {
                        BRS.myAssetsPageLoaded(result);
                    }
                    continue;
                }

                BRS.sendRequest("getAskOrderIds+", {
                    "asset": BRS.accountInfo.assetBalances[i].asset,
                    "limit": 1,
                    "timestamp": 0
                }, function(response, input) {
                    if (BRS.currentPage != "my_assets") {
                        return;
                    }

                    if (response.askOrderIds && response.askOrderIds.length) {
                        BRS.sendRequest("getAskOrder+", {
                            "order": response.askOrderIds[0],
                            "_extra": {
                                "asset": input.asset
                            }
                        }, function(response, input) {
                            if (BRS.currentPage != "my_assets") {
                                return;
                            }

                            response.priceNQT = new BigInteger(response.priceNQT);

                            result.ask_orders[input["_extra"].asset] = response.priceNQT;
                            count.ask_orders++;
                            if (BRS.checkMyAssetsPageLoaded(count)) {
                                BRS.myAssetsPageLoaded(result);
                            }
                        });
                    } else {
                        result.ask_orders[input.asset] = -1;
                        count.ask_orders++;
                        if (BRS.checkMyAssetsPageLoaded(count)) {
                            BRS.myAssetsPageLoaded(result);
                        }
                    }
                });

                BRS.sendRequest("getBidOrderIds+", {
                    "asset": BRS.accountInfo.assetBalances[i].asset,
                    "limit": 1,
                    "timestamp": 0
                }, function(response, input) {
                    if (BRS.currentPage != "my_assets") {
                        return;
                    }

                    if (response.bidOrderIds && response.bidOrderIds.length) {
                        BRS.sendRequest("getBidOrder+", {
                            "order": response.bidOrderIds[0],
                            "_extra": {
                                "asset": input.asset
                            }
                        }, function(response, input) {
                            if (BRS.currentPage != "my_assets") {
                                return;
                            }

                            response.priceNQT = new BigInteger(response.priceNQT);

                            result.bid_orders[input["_extra"].asset] = response.priceNQT;
                            count.bid_orders++;
                            if (BRS.checkMyAssetsPageLoaded(count)) {
                                BRS.myAssetsPageLoaded(result);
                            }
                        });
                    } else {
                        result.bid_orders[input.asset] = -1;
                        count.bid_orders++;
                        if (BRS.checkMyAssetsPageLoaded(count)) {
                            BRS.myAssetsPageLoaded(result);
                        }
                    }
                });

                BRS.sendRequest("getAsset+", {
                    "asset": BRS.accountInfo.assetBalances[i].asset,
                    "_extra": {
                        "balanceQNT": BRS.accountInfo.assetBalances[i].balanceQNT
                    }
                }, function(asset, input) {
                    if (BRS.currentPage != "my_assets") {
                        return;
                    }

                    asset.asset = input.asset;
                    asset.balanceQNT = new BigInteger(input["_extra"].balanceQNT);
                    asset.quantityQNT = new BigInteger(asset.quantityQNT);

                    result.assets[count.assets] = asset;
                    count.assets++;

                    if (BRS.checkMyAssetsPageLoaded(count)) {
                        BRS.myAssetsPageLoaded(result);
                    }
                });
            }
        } else {
            BRS.dataLoaded();
        }
    }

    BRS.checkMyAssetsPageLoaded = function(count) {
        if ((count.assets + count.ignored_assets == count.total_assets) && (count.assets == count.ask_orders) && (count.assets == count.bid_orders)) {
            return true;
        } else {
            return false;
        }
    }

    BRS.myAssetsPageLoaded = function(result) {
        var rows = "";

        result.assets.sort(function(a, b) {
            if (a.name.toLowerCase() > b.name.toLowerCase()) {
                return 1;
            } else if (a.name.toLowerCase() < b.name.toLowerCase()) {
                return -1;
            } else {
                return 0;
            }
        });

        for (var i = 0; i < result.assets.length; i++) {
            var asset = result.assets[i];

            var lowestAskOrder = result.ask_orders[asset.asset];
            var highestBidOrder = result.bid_orders[asset.asset];

            var percentageAsset = BRS.calculatePercentage(asset.balanceQNT, asset.quantityQNT);

            if (highestBidOrder != -1) {
                var total = new BigInteger(BRS.calculateOrderTotalNQT(asset.balanceQNT, highestBidOrder, asset.decimals));
            } else {
                var total = 0;
            }

            var tentative = -1;

            if (BRS.unconfirmedTransactions.length) {
                for (var j = 0; j < BRS.unconfirmedTransactions.length; j++) {
                    var unconfirmedTransaction = BRS.unconfirmedTransactions[j];

                    if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == 1 && unconfirmedTransaction.attachment.asset == asset.asset) {
                        if (tentative == -1) {
                            if (unconfirmedTransaction.recipient == BRS.account) {
                                tentative = new BigInteger(unconfirmedTransaction.attachment.quantityQNT);
                            } else {
                                tentative = new BigInteger("-" + unconfirmedTransaction.attachment.quantityQNT);
                            }
                        } else {
                            if (unconfirmedTransaction.recipient == BRS.account) {
                                tentative = tentative.add(new BigInteger(unconfirmedTransaction.attachment.quantityQNT));
                            } else {
                                tentative = tentative.add(new BigInteger("-" + unconfirmedTransaction.attachment.quantityQNT));
                            }
                        }
                    }
                }
            }

            if (highestBidOrder != -1) {
                var totalNQT = new BigInteger(BRS.calculateOrderTotalNQT(asset.balanceQNT, highestBidOrder));
            }

            var sign = "+";

            if (tentative != -1 && tentative.compareTo(BigInteger.ZERO) < 0) {
                tentative = tentative.abs();
                sign = "-";
            }

            rows += "<tr" + (tentative != -1 ? " class='tentative tentative-allow-links'" : "") + " data-asset='" + String(asset.asset).escapeHTML() + "'><td><a href='#' data-goto-asset='" + String(asset.asset).escapeHTML() + "'>" + String(asset.name).escapeHTML() + "</a></td><td class='quantity'>" + BRS.formatQuantity(asset.balanceQNT, asset.decimals) + (tentative != -1 ? " " + sign + " <span class='added_quantity'>" + BRS.formatQuantity(tentative, asset.decimals) + "</span>" : "") + "</td><td>" + BRS.formatQuantity(asset.quantityQNT, asset.decimals) + "</td><td>" + percentageAsset + "%</td><td>" + (lowestAskOrder != -1 ? BRS.formatOrderPricePerWholeQNT(lowestAskOrder, asset.decimals) : "/") + "</td><td>" + (highestBidOrder != -1 ? BRS.formatOrderPricePerWholeQNT(highestBidOrder, asset.decimals) : "/") + "</td><td>" + (highestBidOrder != -1 ? BRS.formatAmount(totalNQT) : "/") + "</td><td><a href='#' data-toggle='modal' data-target='#transfer_asset_modal' data-asset='" + String(asset.asset).escapeHTML() + "' data-name='" + String(asset.name).escapeHTML() + "' data-decimals='" + String(asset.decimals).escapeHTML() + "'>" + $.t("transfer") + "</a></td></tr>";
        }

        BRS.dataLoaded(rows);
    }

    BRS.incoming.my_assets = function() {
        BRS.loadPage("my_assets");
    }

    $("#transfer_asset_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);

        var assetId = $invoker.data("asset");
        var assetName = $invoker.data("name");
        var decimals = $invoker.data("decimals");

        $("#transfer_asset_asset").val(assetId);
        $("#transfer_asset_decimals").val(decimals);
        $("#transfer_asset_name, #transfer_asset_quantity_name").html(String(assetName).escapeHTML());
        $("#transer_asset_available").html("");

        var confirmedBalance = 0;
        var unconfirmedBalance = 0;

        if (BRS.accountInfo.assetBalances) {
            $.each(BRS.accountInfo.assetBalances, function(key, assetBalance) {
                if (assetBalance.asset == assetId) {
                    confirmedBalance = assetBalance.balanceQNT;
                    return false;
                }
            });
        }

        if (BRS.accountInfo.unconfirmedAssetBalances) {
            $.each(BRS.accountInfo.unconfirmedAssetBalances, function(key, assetBalance) {
                if (assetBalance.asset == assetId) {
                    unconfirmedBalance = assetBalance.unconfirmedBalanceQNT;
                    return false;
                }
            });
        }

        var availableAssetsMessage = "";

        if (confirmedBalance == unconfirmedBalance) {
            availableAssetsMessage = " - " + $.t("available_for_transfer", {
                "qty": BRS.formatQuantity(confirmedBalance, decimals)
            });
        } else {
            availableAssetsMessage = " - " + $.t("available_for_transfer", {
                "qty": BRS.formatQuantity(unconfirmedBalance, decimals)
            }) + " (" + BRS.formatQuantity(confirmedBalance, decimals) + " " + $.t("total_lowercase") + ")";
        }

        $("#transfer_asset_available").html(availableAssetsMessage);
    });

    BRS.forms.transferAsset = function($modal) {
        var data = BRS.getFormData($modal.find("form:first"));

        if (!data.quantity) {
            return {
                "error": $.t("error_not_specified", {
                    "name": BRS.getTranslatedFieldName("quantity").toLowerCase()
                }).capitalize()
            };
        }

        if (!BRS.showedFormWarning) {
            if (BRS.settings["asset_transfer_warning"] && BRS.settings["asset_transfer_warning"] != 0) {
                if (new Big(data.quantity).cmp(new Big(BRS.settings["asset_transfer_warning"])) > 0) {
                    BRS.showedFormWarning = true;
                    return {
                        "error": $.t("error_max_asset_transfer_warning", {
                            "qty": String(BRS.settings["asset_transfer_warning"]).escapeHTML()
                        })
                    };
                }
            }
        }

        try {
            data.quantityQNT = BRS.convertToQNT(data.quantity, data.decimals);
        } catch (e) {
            return {
                "error": $.t("error_incorrect_quantity_plus", {
                    "err": e.escapeHTML()
                })
            };
        }

        delete data.quantity;
        delete data.decimals;

        if (!data.add_message) {
            delete data.add_message;
            delete data.message;
            delete data.encrypt_message;
        }

        return {
            "data": data
        };
    }

    BRS.forms.transferAssetComplete = function(response, data) {
        BRS.loadPage("my_assets");
    }

    $("body").on("click", "a[data-goto-asset]", function(e) {
        e.preventDefault();

        var $visible_modal = $(".modal.in");

        if ($visible_modal.length) {
            $visible_modal.modal("hide");
        }

        BRS.goToAsset($(this).data("goto-asset"));
    });

    BRS.goToAsset = function(asset) {
        BRS.assetSearch = false;
        $("#asset_exchange_sidebar_search input[name=q]").val("");
        $("#asset_exchange_clear_search").hide();

        $("#asset_exchange_sidebar a.list-group-item.active").removeClass("active");
        $("#no_asset_selected, #asset_details, #no_assets_available, #no_asset_search_results").hide();
        $("#loading_asset_data").show();

        $("ul.sidebar-menu a[data-page=asset_exchange]").last().trigger("click", [{
            callback: function() {
                var assetLink = $("#asset_exchange_sidebar a[data-asset=" + asset + "]");

                if (assetLink.length) {
                    assetLink.click();
                } else {
                    BRS.sendRequest("getAsset", {
                        "asset": asset
                    }, function(response) {
                        if (!response.errorCode) {
                            BRS.loadAssetExchangeSidebar(function() {
                                response.groupName = "";
                                response.viewingAsset = true;
                                BRS.loadAsset(response);
                            });
                        } else {
                            $.notify($.t("error_asset_not_found"), {
                                "type": "danger"
                            });
                        }
                    });
                }
            }
        }]);
    }

    /* OPEN ORDERS PAGE */
    BRS.pages.open_orders = function() {
        var loaded = 0;

        BRS.getOpenOrders("ask", function() {
            loaded++;
            if (loaded == 2) {
                BRS.pageLoaded();
            }
        });

        BRS.getOpenOrders("bid", function() {
            loaded++;
            if (loaded == 2) {
                BRS.pageLoaded();
            }
        });
    }

    BRS.getOpenOrders = function(type, callback) {
        var uppercase = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
        var lowercase = type.toLowerCase();

        var getCurrentOrderIds = "getAccountCurrent" + uppercase + "OrderIds+";
        var orderIds = lowercase + "OrderIds";
        var getOrder = "get" + uppercase + "Order+";

        var orders = [];

        BRS.sendRequest(getCurrentOrderIds, {
            "account": BRS.account,
            "timestamp": 0
        }, function(response) {
            if (response[orderIds] && response[orderIds].length) {
                var nr_orders = 0;

                for (var i = 0; i < response[orderIds].length; i++) {
                    BRS.sendRequest(getOrder, {
                        "order": response[orderIds][i]
                    }, function(order, input) {
                        if (BRS.currentPage != "open_orders") {
                            return;
                        }

                        order.order = input.order;
                        orders.push(order);

                        nr_orders++;

                        if (nr_orders == response[orderIds].length) {
                            var nr_orders_complete = 0;

                            for (var i = 0; i < nr_orders; i++) {
                                var order = orders[i];

                                BRS.sendRequest("getAsset+", {
                                    "asset": order.asset,
                                    "_extra": {
                                        "id": i
                                    }
                                }, function(asset, input) {
                                    if (BRS.currentPage != "open_orders") {
                                        return;
                                    }

                                    orders[input["_extra"].id].assetName = asset.name;
                                    orders[input["_extra"].id].decimals = asset.decimals;

                                    nr_orders_complete++;

                                    if (nr_orders_complete == nr_orders) {
                                        BRS.getUnconfirmedOrders(type, function(unconfirmedOrders) {
                                            BRS.openOrdersLoaded(orders.concat(unconfirmedOrders), lowercase, callback);
                                        });
                                    }
                                });

                                if (BRS.currentPage != "open_orders") {
                                    return;
                                }
                            }
                        }
                    });

                    if (BRS.currentPage != "open_orders") {
                        return;
                    }
                }
            } else {
                BRS.getUnconfirmedOrders(type, function(unconfirmedOrders) {
                    BRS.openOrdersLoaded(unconfirmedOrders, lowercase, callback);
                });
            }
        });
    }

    BRS.getUnconfirmedOrders = function(type, callback) {
        if (BRS.unconfirmedTransactions.length) {
            var unconfirmedOrders = [];

            for (var i = 0; i < BRS.unconfirmedTransactions.length; i++) {
                var unconfirmedTransaction = BRS.unconfirmedTransactions[i];

                if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == (type == "ask" ? 2 : 3)) {
                    unconfirmedOrders.push({
                        "account": unconfirmedTransaction.sender,
                        "asset": unconfirmedTransaction.attachment.asset,
                        "assetName": "",
                        "decimals": 0,
                        "height": 0,
                        "order": unconfirmedTransaction.transaction,
                        "priceNQT": unconfirmedTransaction.attachment.priceNQT,
                        "quantityQNT": unconfirmedTransaction.attachment.quantityQNT,
                        "tentative": true
                    })
                }
            }

            if (unconfirmedOrders.length == 0) {
                callback([]);
            } else {
                var nr_orders = 0;

                for (var i = 0; i < unconfirmedOrders.length; i++) {
                    BRS.sendRequest("getAsset+", {
                        "asset": unconfirmedOrders[i].asset,
                        "_extra": {
                            "id": i
                        }
                    }, function(asset, input) {
                        unconfirmedOrders[input["_extra"].id].assetName = asset.name;
                        unconfirmedOrders[input["_extra"].id].decimals = asset.decimals;

                        nr_orders++;

                        if (nr_orders == unconfirmedOrders.length) {
                            callback(unconfirmedOrders);
                        }
                    });
                }
            }
        } else {
            callback([]);
        }
    }

    BRS.openOrdersLoaded = function(orders, type, callback) {
        if (!orders.length) {
            $("#open_" + type + "_orders_table tbody").empty();
            BRS.dataLoadFinished($("#open_" + type + "_orders_table"));

            callback();

            return;
        }

        orders.sort(function(a, b) {
            if (a.assetName.toLowerCase() > b.assetName.toLowerCase()) {
                return 1;
            } else if (a.assetName.toLowerCase() < b.assetName.toLowerCase()) {
                return -1;
            } else {
                if (a.quantity * a.price > b.quantity * b.price) {
                    return 1;
                } else if (a.quantity * a.price < b.quantity * b.price) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        var rows = "";

        for (var i = 0; i < orders.length; i++) {
            var completeOrder = orders[i];

            var cancelled = false;

            if (BRS.unconfirmedTransactions.length) {
                for (var j = 0; j < BRS.unconfirmedTransactions.length; j++) {
                    var unconfirmedTransaction = BRS.unconfirmedTransactions[j];

                    if (unconfirmedTransaction.type == 2 && unconfirmedTransaction.subtype == (type == "ask" ? 4 : 5) && unconfirmedTransaction.attachment.order == completeOrder.order) {
                        cancelled = true;
                        break;
                    }
                }
            }

            completeOrder.priceNQT = new BigInteger(completeOrder.priceNQT);
            completeOrder.quantityQNT = new BigInteger(completeOrder.quantityQNT);
            completeOrder.totalNQT = new BigInteger(BRS.calculateOrderTotalNQT(completeOrder.quantityQNT, completeOrder.priceNQT));

            rows += "<tr data-order='" + String(completeOrder.order).escapeHTML() + "'" + (cancelled ? " class='tentative tentative-crossed'" : (completeOrder.tentative ? " class='tentative'" : "")) + "><td><a href='#' data-goto-asset='" + String(completeOrder.asset).escapeHTML() + "'>" + completeOrder.assetName.escapeHTML() + "</a></td><td>" + BRS.formatQuantity(completeOrder.quantityQNT, completeOrder.decimals) + "</td><td>" + BRS.formatOrderPricePerWholeQNT(completeOrder.priceNQT, completeOrder.decimals) + "</td><td>" + BRS.formatAmount(completeOrder.totalNQT) + "</td><td class='cancel'>" + (cancelled || completeOrder.tentative ? "/" : "<a href='#' data-toggle='modal' data-target='#cancel_order_modal' data-order='" + String(completeOrder.order).escapeHTML() + "' data-type='" + type + "'>" + $.t("cancel") + "</a>") + "</td></tr>";
        }

        $("#open_" + type + "_orders_table tbody").empty().append(rows);

        BRS.dataLoadFinished($("#open_" + type + "_orders_table"));
        orders = {};

        callback();
    }

    BRS.incoming.open_orders = function(transactions) {
        if (BRS.hasTransactionUpdates(transactions)) {
            BRS.loadPage("open_orders");
        }
    }

    $("#cancel_order_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);

        var orderType = $invoker.data("type");
        var orderId = $invoker.data("order");

        if (orderType == "bid") {
            $("#cancel_order_type").val("cancelBidOrder");
        } else {
            $("#cancel_order_type").val("cancelAskOrder");
        }

        $("#cancel_order_order").val(orderId);
    });

    BRS.forms.cancelOrder = function($modal) {
        var data = BRS.getFormData($modal.find("form:first"));

        var requestType = data.cancel_order_type;

        delete data.cancel_order_type;

        return {
            "data": data,
            "requestType": requestType
        };
    }

    BRS.forms.cancelOrderComplete = function(response, data) {
        if (data.requestType == "cancelAskOrder") {
            $.notify($.t("success_cancel_sell_order"), {
                "type": "success"
            });
        } else {
            $.notify($.t("success_cancel_buy_order"), {
                "type": "success"
            });
        }

        if (response.alreadyProcessed) {
            return;
        }

        $("#open_orders_page tr[data-order=" + String(data.order).escapeHTML() + "]").addClass("tentative tentative-crossed").find("td.cancel").html("/");
    }

    return BRS;
}(BRS || {}, jQuery));