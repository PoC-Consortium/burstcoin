/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	$("#transactions_table, #dashboard_transactions_table").on("click", "a[data-transaction]", function(e) {
		e.preventDefault();

		var transactionId = $(this).data("transaction");

		NRS.showTransactionModal(transactionId);
	});

	NRS.showTransactionModal = function(transaction) {
		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		$("#transaction_info_output_top, #transaction_info_output_bottom, #transaction_info_bottom").html("").hide();
		$("#transaction_info_callout").hide();
		$("#transaction_info_table").hide();
		$("#transaction_info_table tbody").empty();

		if (typeof transaction != "object") {
			NRS.sendRequest("getTransaction", {
				"transaction": transaction
			}, function(response, input) {
				response.transaction = input.transaction;
				NRS.processTransactionModalData(response);
			});
		} else {
			NRS.processTransactionModalData(transaction);
		}
	}

	NRS.processTransactionModalData = function(transaction) {
		var async = false;

		var transactionDetails = $.extend({}, transaction);
		delete transactionDetails.attachment;
		if (transactionDetails.referencedTransaction == "0") {
			delete transactionDetails.referencedTransaction;
		}
		delete transactionDetails.transaction;

		$("#transaction_info_modal_transaction").html(String(transaction.transaction).escapeHTML());

		$("#transaction_info_tab_link").tab("show");

		$("#transaction_info_details_table tbody").empty().append(NRS.createInfoTable(transactionDetails, true));
		$("#transaction_info_table tbody").empty();

		var incorrect = false;

		if (transaction.senderRS == NRS.accountRS) {
			$("#transaction_info_actions").hide();
		} else {
			if (transaction.senderRS in NRS.contacts) {
				var accountButton = NRS.contacts[transaction.senderRS].name.escapeHTML();
				$("#transaction_info_modal_add_as_contact").hide();
			} else {
				var accountButton = transaction.senderRS;
				$("#transaction_info_modal_add_as_contact").show();
			}

			$("#transaction_info_actions").show();
			$("#transaction_info_actions_tab button").data("account", accountButton);
		}

		if (transaction.type == 0) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"type": $.t("ordinary_payment"),
						"amount": transaction.amountNQT,
						"fee": transaction.feeNQT,
						"recipient": NRS.getAccountTitle(transaction, "recipient"),
						"sender": NRS.getAccountTitle(transaction, "sender")
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				default:
					incorrect = true;
					break;
			}
		} else if (transaction.type == 1) {
			switch (transaction.subtype) {
				case 0:
					var message;

					var $output = $("#transaction_info_output_top");

					if (transaction.attachment) {
						if (transaction.attachment.message) {
							if (!transaction.attachment["version.Message"]) {
								try {
									message = converters.hexStringToString(transaction.attachment.message);
								} catch (err) {
									//legacy
									if (transaction.attachment.message.indexOf("feff") === 0) {
										message = NRS.convertFromHex16(transaction.attachment.message);
									} else {
										message = NRS.convertFromHex8(transaction.attachment.message);
									}
								}
							} else {
								message = String(transaction.attachment.message);
							}
							$output.html("<div style='color:#999999;padding-bottom:10px'><i class='fa fa-unlock'></i> " + $.t("public_message") + "</div><div style='padding-bottom:10px'>" + String(message).escapeHTML().nl2br() + "</div>");
						}

						if (transaction.attachment.encryptedMessage || (transaction.attachment.encryptToSelfMessage && NRS.account == transaction.sender)) {
							$output.append("<div id='transaction_info_decryption_form'></div><div id='transaction_info_decryption_output' style='display:none;padding-bottom:10px;'></div>");

							if (NRS.account == transaction.recipient || NRS.account == transaction.sender) {
								var fieldsToDecrypt = {};

								if (transaction.attachment.encryptedMessage) {
									fieldsToDecrypt.encryptedMessage = $.t("encrypted_message");
								}
								if (transaction.attachment.encryptToSelfMessage && NRS.account == transaction.sender) {
									fieldsToDecrypt.encryptToSelfMessage = $.t("note_to_self");
								}

								NRS.tryToDecrypt(transaction, fieldsToDecrypt, (transaction.recipient == NRS.account ? transaction.sender : transaction.recipient), {
									"noPadding": true,
									"formEl": "#transaction_info_decryption_form",
									"outputEl": "#transaction_info_decryption_output"
								});
							} else {
								$output.append("<div style='padding-bottom:10px'>" + $.t("encrypted_message_no_permission") + "</div>");
							}
						}
					} else {
						$output.append("<div style='padding-bottom:10px'>" + $.t("message_empty") + "</div>");
					}

					$output.append("<table><tr><td><strong>" + $.t("from") + "</strong>:&nbsp;</td><td>" + NRS.getAccountLink(transaction, "sender") + "</td></tr><tr><td><strong>" + $.t("to") + "</strong>:&nbsp;</td><td>" + NRS.getAccountLink(transaction, "recipient") + "</td></tr></table>").show();

					break;
				case 1:
					var data = {
						"type": $.t("alias_assignment"),
						"alias": transaction.attachment.alias,
						"data_formatted_html": transaction.attachment.uri.autoLink()
					};

					if (transaction.sender != NRS.account) {
						data["sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 2:
					var data = {
						"type": $.t("poll_creation"),
						"name": transaction.attachment.name,
						"description": transaction.attachment.description
					};

					if (transaction.sender != NRS.account) {
						data["sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 3:
					var data = {
						"type": $.t("vote_casting")
					};

					if (transaction.sender != NRS.account) {
						data["sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 4:
					var data = {
						"type": $.t("hub_announcement")
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 5:
					var data = {
						"type": $.t("account_info"),
						"name": transaction.attachment.name,
						"description": transaction.attachment.description
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 6:
					if (transaction.attachment.priceNQT == "0") {
						if (transaction.sender == transaction.recipient) {
							var type = $.t("alias_sale_cancellation");
						} else {
							var type = $.t("alias_transfer");
						}
					} else {
						var type = $.t("alias_sale");
					}

					var data = {
						"type": type,
						"alias_name": transaction.attachment.alias
					}

					if (type == $.t("alias_sale")) {
						data["price"] = transaction.attachment.priceNQT
					}

					if (type != $.t("alias_sale_cancellation")) {
						data["recipient"] = NRS.getAccountTitle(transaction, "recipient");
					}

					data["sender"] = NRS.getAccountTitle(transaction, "sender");

					if (type == $.t("alias_sale")) {
						var message = "";
						var messageStyle = "info";

						NRS.sendRequest("getAlias", {
							"aliasName": transaction.attachment.alias
						}, function(response) {
							NRS.fetchingModalData = false;

							if (!response.errorCode) {
								if (transaction.recipient != response.buyer || transaction.attachment.priceNQT != response.priceNQT) {
									message = $.t("alias_sale_info_outdated");
									messageStyle = "danger";
								} else if (transaction.recipient == NRS.account) {
									message = $.t("alias_sale_direct_offer", {
										"burst": NRS.formatAmount(transaction.attachment.priceNQT)
									}) + " <a href='#' data-alias='" + String(transaction.attachment.alias).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>";
								} else if (typeof transaction.recipient == "undefined") {
									message = $.t("alias_sale_indirect_offer", {
										"burst": NRS.formatAmount(transaction.attachment.priceNQT)
									}) + " <a href='#' data-alias='" + String(transaction.attachment.alias).escapeHTML() + "' data-toggle='modal' data-target='#buy_alias_modal'>" + $.t("buy_it_q") + "</a>";
								} else if (transaction.senderRS == NRS.accountRS) {
									if (transaction.attachment.priceNQT != "0") {
										message = $.t("your_alias_sale_offer") + " <a href='#' data-alias='" + String(transaction.attachment.alias).escapeHTML() + "' data-toggle='modal' data-target='#cancel_alias_sale_modal'>" + $.t("cancel_sale_q") + "</a>";
									}
								} else {
									message = $.t("error_alias_sale_different_account");
								}
							}
						}, false);

						if (message) {
							$("#transaction_info_bottom").html("<div class='callout callout-bottom callout-" + messageStyle + "'>" + message + "</div>").show();
						}
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 7:
					var data = {
						"type": $.t("alias_buy"),
						"alias_name": transaction.attachment.alias,
						"price": transaction.amountNQT,
						"recipient": NRS.getAccountTitle(transaction, "recipient"),
						"sender": NRS.getAccountTitle(transaction, "sender")
					}

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				default:
					incorrect = true;
					break;
			}
		} else if (transaction.type == 2) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"type": $.t("asset_issuance"),
						"name": transaction.attachment.name,
						"quantity": [transaction.attachment.quantityQNT, transaction.attachment.decimals],
						"decimals": transaction.attachment.decimals,
						"description": transaction.attachment.description
					};

					if (transaction.sender != NRS.account) {
						data["sender"] = NRS.getAccountTitle(transaction, "sender");
					}

					$("#transaction_info_callout").html("<a href='#' data-goto-asset='" + String(transaction.transaction).escapeHTML() + "'>Click here</a> to view this asset in the Asset Exchange.").show();

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 1:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"type": $.t("asset_transfer"),
							"asset_name": asset.name,
							"quantity": [transaction.attachment.quantityQNT, asset.decimals]
						};

						data["sender"] = NRS.getAccountTitle(transaction, "sender");
						data["recipient"] = NRS.getAccountTitle(transaction, "recipient");

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 2:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"type": $.t("ask_order_placement"),
							"asset_name": asset.name,
							"quantity": [transaction.attachment.quantityQNT, asset.decimals],
							"price_formatted_html": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " BURST",
							"total_formatted_html": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " BURST"
						};

						if (transaction.sender != NRS.account) {
							data["sender"] = NRS.getAccountTitle(transaction, "sender");
						}

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 3:
					async = true;

					NRS.sendRequest("getAsset", {
						"asset": transaction.attachment.asset
					}, function(asset, input) {
						var data = {
							"type": $.t("bid_order_placement"),
							"asset_name": asset.name,
							"quantity": [transaction.attachment.quantityQNT, asset.decimals],
							"price_formatted_html": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " BURST",
							"total_formatted_html": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " BURST"
						};

						if (transaction.sender != NRS.account) {
							data["sender"] = NRS.getAccountTitle(transaction, "sender");
						}

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 4:
					async = true;

					NRS.sendRequest("getTransaction", {
						"transaction": transaction.attachment.order
					}, function(transaction, input) {
						if (transaction.attachment.asset) {
							NRS.sendRequest("getAsset", {
								"asset": transaction.attachment.asset
							}, function(asset) {
								var data = {
									"type": $.t("ask_order_cancellation"),
									"asset_name": asset.name,
									"quantity": [transaction.attachment.quantityQNT, asset.decimals],
									"price_formatted_html": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " BURST",
									"total_formatted_html": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " BURST"
								};

								if (transaction.sender != NRS.account) {
									data["sender"] = NRS.getAccountTitle(transaction, "sender");
								}

								$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
								$("#transaction_info_table").show();

								$("#transaction_info_modal").modal("show");
								NRS.fetchingModalData = false;
							});
						} else {
							NRS.fetchingModalData = false;
						}
					});

					break;
				case 5:
					async = true;

					NRS.sendRequest("getTransaction", {
						"transaction": transaction.attachment.order
					}, function(transaction) {
						if (transaction.attachment.asset) {
							NRS.sendRequest("getAsset", {
								"asset": transaction.attachment.asset
							}, function(asset) {
								var data = {
									"type": $.t("bid_order_cancellation"),
									"asset_name": asset.name,
									"quantity": [transaction.attachment.quantityQNT, asset.decimals],
									"price_formatted_html": NRS.formatOrderPricePerWholeQNT(transaction.attachment.priceNQT, asset.decimals) + " BURST",
									"total_formatted_html": NRS.formatAmount(NRS.calculateOrderTotalNQT(transaction.attachment.quantityQNT, transaction.attachment.priceNQT)) + " BURST"
								};

								if (transaction.sender != NRS.account) {
									data["sender"] = NRS.getAccountTitle(transaction, "sender");
								}

								$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
								$("#transaction_info_table").show();

								$("#transaction_info_modal").modal("show");
								NRS.fetchingModalData = false;
							});
						} else {
							NRS.fetchingModalData = false;
						}
					});

					break;
				default:
					incorrect = true;
					break;
			}
		} else if (transaction.type == 3) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"type": $.t("marketplace_listing"),
						"name": transaction.attachment.name,
						"description": transaction.attachment.description,
						"price": transaction.attachment.priceNQT,
						"quantity_formatted_html": NRS.format(transaction.attachment.quantity),
						"seller": NRS.getAccountFormatted(transaction, "sender")
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;
				case 1:
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"type": $.t("marketplace_removal"),
							"item_name": goods.name,
							"seller": NRS.getAccountFormatted(goods, "seller")
						};

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 2:
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"type": $.t("marketplace_item_price_change"),
							"item_name": goods.name,
							"new_price_formatted_html": NRS.formatAmount(transaction.attachment.priceNQT) + " BURST",
							"seller": NRS.getAccountFormatted(goods, "seller")
						};

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 3:
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"type": $.t("marketplace_item_quantity_change"),
							"item_name": goods.name,
							"delta_quantity": transaction.attachment.deltaQuantity,
							"seller": NRS.getAccountFormatted(goods, "seller")
						};

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						$("#transaction_info_modal").modal("show");
						NRS.fetchingModalData = false;
					});

					break;
				case 4:
					async = true;

					NRS.sendRequest("getDGSGood", {
						"goods": transaction.attachment.goods
					}, function(goods) {
						var data = {
							"type": $.t("marketplace_purchase"),
							"item_name": goods.name,
							"price": transaction.attachment.priceNQT,
							"quantity_formatted_html": NRS.format(transaction.attachment.quantity),
							"buyer": NRS.getAccountFormatted(transaction, "sender"),
							"seller": NRS.getAccountFormatted(goods, "seller")
						};

						$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
						$("#transaction_info_table").show();

						NRS.sendRequest("getDGSPurchase", {
							"purchase": transaction.transaction
						}, function(purchase) {
							var callout = "";

							if (purchase.errorCode) {
								if (purchase.errorCode == 4) {
									callout = $.t("incorrect_purchase");
								} else {
									callout = String(purchase.errorDescription).escapeHTML();
								}
							} else {
								if (NRS.account == transaction.recipient || NRS.account == transaction.sender) {
									if (purchase.pending) {
										if (NRS.account == transaction.recipient) {
											callout = "<a href='#' data-toggle='modal' data-target='#dgs_delivery_modal' data-purchase='" + String(transaction.transaction).escapeHTML() + "'>" + $.t("deliver_goods_q") + "</a>";
										} else {
											callout = $.t("waiting_on_seller");
										}
									} else {
										if (purchase.refundNQT) {
											callout = $.t("purchase_refunded");
										} else {
											callout = $.t("purchase_delivered");
										}
									}
								}
							}

							if (callout) {
								$("#transaction_info_bottom").html("<div class='callout " + (purchase.errorCode ? "callout-danger" : "callout-info") + " callout-bottom'>" + callout + "</div>").show();
							}

							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
					});

					break;
				case 5:
					async = true;

					NRS.sendRequest("getDGSPurchase", {
						"purchase": transaction.attachment.purchase
					}, function(purchase) {
						NRS.sendRequest("getDGSGood", {
							"goods": purchase.goods
						}, function(goods) {
							var data = {
								"type": $.t("marketplace_delivery"),
								"item_name": goods.name,
								"price": purchase.priceNQT
							};

							data["quantity_formatted_html"] = NRS.format(purchase.quantity);

							if (purchase.quantity != "1") {
								var orderTotal = NRS.formatAmount(new BigInteger(String(purchase.quantity)).multiply(new BigInteger(String(purchase.priceNQT))));
								data["total_formatted_html"] = orderTotal + " BURST";
							}

							if (transaction.attachment.discountNQT) {
								data["discount"] = transaction.attachment.discountNQT;
							}

							data["buyer"] = NRS.getAccountFormatted(purchase, "buyer");
							data["seller"] = NRS.getAccountFormatted(purchase, "seller");

							if (transaction.attachment.goodsData) {
								if (NRS.account == purchase.seller || NRS.account == purchase.buyer) {
									NRS.tryToDecrypt(transaction, {
										"goodsData": {
											"title": $.t("data"),
											"nonce": "goodsNonce"
										}
									}, (purchase.buyer == NRS.account ? purchase.seller : purchase.buyer));
								} else {
									data["data"] = $.t("encrypted_goods_data_no_permission");
								}
							}

							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();

							var callout;

							if (NRS.account == purchase.buyer) {
								if (purchase.refundNQT) {
									callout = $.t("purchase_refunded");
								} else if (!purchase.feedbackNote) {
									callout = $.t("goods_received") + " <a href='#' data-toggle='modal' data-target='#dgs_feedback_modal' data-purchase='" + String(transaction.attachment.purchase).escapeHTML() + "'>" + $.t("give_feedback_q") + "</a>";
								}
							} else if (NRS.account == purchase.seller && purchase.refundNQT) {
								callout = $.t("purchase_refunded");
							}

							if (callout) {
								$("#transaction_info_bottom").append("<div class='callout callout-info callout-bottom'>" + callout + "</div>").show();
							}

							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
					});

					break;
				case 6:
					async = true;

					NRS.sendRequest("getDGSPurchase", {
						"purchase": transaction.attachment.purchase
					}, function(purchase) {
						NRS.sendRequest("getDGSGood", {
							"goods": purchase.goods
						}, function(goods) {
							var data = {
								"type": $.t("marketplace_feedback"),
								"item_name": goods.name,
								"buyer": NRS.getAccountFormatted(purchase, "buyer"),
								"seller": NRS.getAccountFormatted(purchase, "seller")
							};

							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();

							if (purchase.seller == NRS.account || purchase.buyer == NRS.account) {
								NRS.sendRequest("getDGSPurchase", {
									"purchase": transaction.attachment.purchase
								}, function(purchase) {
									var callout;

									if (purchase.buyer == NRS.account) {
										if (purchase.refundNQT) {
											callout = $.t("purchase_refunded");
										}
									} else {
										if (!purchase.refundNQT) {
											callout = "<a href='#' data-toggle='modal' data-target='#dgs_refund_modal' data-purchase='" + String(transaction.attachment.purchase).escapeHTML() + "'>" + $.t("refund_this_purchase_q") + "</a>";
										} else {
											callout = $.t("purchase_refunded");
										}
									}

									if (callout) {
										$("#transaction_info_bottom").append("<div class='callout callout-info callout-bottom'>" + callout + "</div>").show();
									}

									$("#transaction_info_modal").modal("show");
									NRS.fetchingModalData = false;
								});

							} else {
								$("#transaction_info_modal").modal("show");
								NRS.fetchingModalData = false;
							}
						});
					});

					break;
				case 7:
					async = true;

					NRS.sendRequest("getDGSPurchase", {
						"purchase": transaction.attachment.purchase
					}, function(purchase) {
						NRS.sendRequest("getDGSGood", {
							"goods": purchase.goods
						}, function(goods) {
							var data = {
								"type": $.t("marketplace_refund"),
								"item_name": goods.name
							};

							var orderTotal = new BigInteger(String(purchase.quantity)).multiply(new BigInteger(String(purchase.priceNQT)));

							data["order_total_formatted_html"] = NRS.formatAmount(orderTotal) + " BURST";

							data["refund"] = transaction.attachment.refundNQT;

							data["buyer"] = NRS.getAccountFormatted(purchase, "buyer");
							data["seller"] = NRS.getAccountFormatted(purchase, "seller");

							$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
							$("#transaction_info_table").show();

							$("#transaction_info_modal").modal("show");
							NRS.fetchingModalData = false;
						});
					});

					break;
				default:
					incorrect = true;
					break
			}
		} else if (transaction.type == 4) {
			switch (transaction.subtype) {
				case 0:
					var data = {
						"type": $.t("balance_leasing"),
						"period": transaction.attachment.period
					};

					$("#transaction_info_table tbody").append(NRS.createInfoTable(data));
					$("#transaction_info_table").show();

					break;

				default:
					incorrect = true;
					break;
			}
		}

		if (!(transaction.type == 1 && transaction.subtype == 0)) {
			if (transaction.attachment) {
				if (transaction.attachment.message) {
					if (!transaction.attachment["version.Message"]) {
						try {
							message = converters.hexStringToString(transaction.attachment.message);
						} catch (err) {
							//legacy
							if (transaction.attachment.message.indexOf("feff") === 0) {
								message = NRS.convertFromHex16(transaction.attachment.message);
							} else {
								message = NRS.convertFromHex8(transaction.attachment.message);
							}
						}
					} else {
						message = String(transaction.attachment.message);
					}

					$("#transaction_info_output_bottom").append("<div style='padding-left:5px;'><label><i class='fa fa-unlock'></i> " + $.t("public_message") + "</label><div>" + String(message).escapeHTML().nl2br() + "</div></div>");
				}

				if (transaction.attachment.encryptedMessage || (transaction.attachment.encryptToSelfMessage && NRS.account == transaction.sender)) {
					if (transaction.attachment.message) {
						$("#transaction_info_output_bottom").append("<div style='height:5px'></div>");
					}

					if (NRS.account == transaction.sender || NRS.account == transaction.recipient) {
						var fieldsToDecrypt = {};

						if (transaction.attachment.encryptedMessage) {
							fieldsToDecrypt.encryptedMessage = $.t("encrypted_message");
						}
						if (transaction.attachment.encryptToSelfMessage && NRS.account == transaction.sender) {
							fieldsToDecrypt.encryptToSelfMessage = $.t("note_to_self");
						}

						NRS.tryToDecrypt(transaction, fieldsToDecrypt, (transaction.recipient == NRS.account ? transaction.sender : transaction.recipient), {
							"formEl": "#transaction_info_output_bottom",
							"outputEl": "#transaction_info_output_bottom"
						});
					} else {
						$("#transaction_info_output_bottom").append("<div style='padding-left:5px;'><label><i class='fa fa-lock'></i> " + $.t("encrypted_message") + "</label><div>" + $.t("encrypted_message_no_permission") + "</div></div>");
					}
				}

				$("#transaction_info_output_bottom").show();
			}
		}

		if (incorrect) {
			$.notify($.t("error_unknown_transaction_type"), {
				"type": "danger"
			});

			NRS.fetchingModalData = false;
			return;
		}

		if (!async) {
			$("#transaction_info_modal").modal("show");
			NRS.fetchingModalData = false;
		}
	}

	$("#transaction_info_modal").on("hide.bs.modal", function(e) {
		NRS.removeDecryptionForm($(this));
		$("#transaction_info_output_bottom, #transaction_info_output_top, #transaction_info_bottom").html("").hide();
	});

	return NRS;
}(NRS || {}, jQuery));