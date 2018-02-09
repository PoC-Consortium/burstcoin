/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	var _password;

	NRS.multiQueue = null;

	NRS.setServerPassword = function(password) {
		_password = password;
	}

	NRS.sendOutsideRequest = function(url, data, callback, async) {
		if ($.isFunction(data)) {
			async = callback;
			callback = data;
			data = {};
		} else {
			data = data || {};
		}

		$.support.cors = true;

		$.ajax({
			url: url,
			crossDomain: true,
			dataType: "json",
			type: "GET",
			timeout: 30000,
			async: (async === undefined ? true : async),
			data: data
		}).done(function(json) {
			//why is this necessary??..
			if (json.errorCode && !json.errorDescription) {
				json.errorDescription = (json.errorMessage ? json.errorMessage : $.t("server_error_unknown"));
			}
			if (callback) {
				callback(json, data);
			}
		}).fail(function(xhr, textStatus, error) {
			if (callback) {
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	NRS.sendRequest = function(requestType, data, callback, async) {
		if (requestType == undefined) {
			return;
		}

		if ($.isFunction(data)) {
			async = callback;
			callback = data;
			data = {};
		} else {
			data = data || {};
		}

		$.each(data, function(key, val) {
			if (key != "secretPhrase") {
				if (typeof val == "string") {
					data[key] = $.trim(val);
				}
			}
		});

		//convert NXT to NQT...
		try {
			var nxtFields = ["feeNXT", "amountNXT", "priceNXT", "refundNXT", "discountNXT", "minActivationAmountNXT"];

			for (var i = 0; i < nxtFields.length; i++) {
				var nxtField = nxtFields[i];
				var field = nxtField.replace("NXT", "");

				if (nxtField in data) {
					data[field + "NQT"] = NRS.convertToNQT(data[nxtField]);
					delete data[nxtField];
				}
			}
		} catch (err) {
			if (callback) {
				callback({
					"errorCode": 1,
					"errorDescription": err + " (Field: " + field + ")"
				});
			}

			return;
		}

		if (!data.recipientPublicKey) {
			delete data.recipientPublicKey;
		}
		if (!data.referencedTransactionFullHash) {
			delete data.referencedTransactionFullHash;
		}

		//gets account id from passphrase client side, used only for login.
		if (requestType == "getAccountId") {
			var accountId = NRS.getAccountId(data.secretPhrase);

			var nxtAddress = new NxtAddress();

			if (nxtAddress.set(accountId)) {
				var accountRS = nxtAddress.toString();
			} else {
				var accountRS = "";
			}

			if (callback) {
				callback({
					"account": accountId,
					"accountRS": accountRS
				});
			}
			return;
		}

		//check to see if secretPhrase supplied matches logged in account, if not - show error.
		if ("secretPhrase" in data) {
			var accountId = NRS.getAccountId(NRS.rememberPassword ? _password : data.secretPhrase);
			if (accountId != NRS.account) {
				if (callback) {
					callback({
						"errorCode": 1,
						"errorDescription": $.t("error_passphrase_incorrect")
					});
				}
				return;
			} else {
				//ok, accountId matches..continue with the real request.
				NRS.processAjaxRequest(requestType, data, callback, async);
			}
		} else {
			NRS.processAjaxRequest(requestType, data, callback, async);
		}
	}

	NRS.processAjaxRequest = function(requestType, data, callback, async) {
		if (!NRS.multiQueue) {
			NRS.multiQueue = $.ajaxMultiQueue(8);
		}

		if (data["_extra"]) {
			var extra = data["_extra"];
			delete data["_extra"];
		} else {
			var extra = null;
		}

		var currentPage = null;
		var currentSubPage = null;

		//means it is a page request, not a global request.. Page requests can be aborted.
		if (requestType.slice(-1) == "+") {
			requestType = requestType.slice(0, -1);
			currentPage = NRS.currentPage;
		} else {
			//not really necessary... we can just use the above code..
			var plusCharacter = requestType.indexOf("+");

			if (plusCharacter > 0) {
				var subType = requestType.substr(plusCharacter);
				requestType = requestType.substr(0, plusCharacter);
				currentPage = NRS.currentPage;
			}
		}

		if (currentPage && NRS.currentSubPage) {
			currentSubPage = NRS.currentSubPage;
		}

		var type = (("secretPhrase" in data) || (data.broadcast == "false") ? "POST" : "GET");
		var url = NRS.server + "/burst?requestType=" + requestType;

		if (type == "GET") {
			if (typeof data == "string") {
				data += "&random=" + Math.random();
			} else {
				data.random = Math.random();
			}
		}

		var secretPhrase = "";

		//unknown account..
		if (type == "POST" && (NRS.accountInfo.errorCode && NRS.accountInfo.errorCode == 5)) {
			if (callback) {
				callback({
					"errorCode": 2,
					"errorDescription": $.t("error_new_account")
				}, data);
			} else {
				$.growl($.t("error_new_account"), {
					"type": "danger"
				});
			}
			return;
		}

		if (data.referencedTransactionFullHash) {
			if (!/^[a-z0-9]{64}$/.test(data.referencedTransactionFullHash)) {
				if (callback) {
					callback({
						"errorCode": -1,
						"errorDescription": $.t("error_invalid_referenced_transaction_hash")
					}, data);
				} else {
					$.growl($.t("error_invalid_referenced_transaction_hash"), {
						"type": "danger"
					});
				}
				return;
			}
		}

		if (!NRS.isLocalHost && type == "POST" && requestType != "startForging" && requestType != "stopForging") {
			if (NRS.rememberPassword) {
				secretPhrase = _password;
			} else {
				secretPhrase = data.secretPhrase;
			}

			delete data.secretPhrase;

			if (NRS.accountInfo && NRS.accountInfo.publicKey) {
				data.publicKey = NRS.accountInfo.publicKey;
			} else {
				data.publicKey = NRS.generatePublicKey(secretPhrase);
				NRS.accountInfo.publicKey = data.publicKey;
			}
		} else if (type == "POST" && NRS.rememberPassword) {
			data.secretPhrase = _password;
		}

		$.support.cors = true;

		if (type == "GET") {
			var ajaxCall = NRS.multiQueue.queue;
		} else {
			var ajaxCall = $.ajax;
		}

		//workaround for 1 specific case.. ugly
		if (data.querystring) {
			data = data.querystring;
			type = "POST";
		}

		if (requestType == "broadcastTransaction") {
			type = "POST";
		}

		ajaxCall({
			url: url,
			crossDomain: true,
			dataType: "json",
			type: type,
			timeout: 30000,
			async: (async === undefined ? true : async),
			currentPage: currentPage,
			currentSubPage: currentSubPage,
			shouldRetry: (type == "GET" ? 2 : undefined),
			data: data
		}).done(function(response, status, xhr) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (typeof data == "object" && "recipient" in data) {
				if (/^BURST\-/i.test(data.recipient)) {
					data.recipientRS = data.recipient;

					var address = new NxtAddress();

					if (address.set(data.recipient)) {
						data.recipient = address.account_id();
					}
				} else {
					var address = new NxtAddress();

					if (address.set(data.recipient)) {
						data.recipientRS = address.toString();
					}
				}
			}

			if (secretPhrase && response.unsignedTransactionBytes && !response.errorCode && !response.error) {
				var publicKey = NRS.generatePublicKey(secretPhrase);
				var signature = NRS.signBytes(response.unsignedTransactionBytes, converters.stringToHexString(secretPhrase));

				if (!NRS.verifyBytes(signature, response.unsignedTransactionBytes, publicKey)) {
					if (callback) {
						callback({
							"errorCode": 1,
							"errorDescription": $.t("error_signature_verification_client")
						}, data);
					} else {
						$.growl($.t("error_signature_verification_client"), {
							"type": "danger"
						});
					}
					return;
				} else {
					var payload = NRS.verifyAndSignTransactionBytes(response.unsignedTransactionBytes, signature, requestType, data);

					if (!payload) {
						if (callback) {
							callback({
								"errorCode": 1,
								"errorDescription": $.t("error_signature_verification_server")
							}, data);
						} else {
							$.growl($.t("error_signature_verification_server"), {
								"type": "danger"
							});
						}
						return;
					} else {
						if (data.broadcast == "false") {
							response.transactionBytes = payload;
							NRS.showRawTransactionModal(response);
						} else {
							if (callback) {
								if (extra) {
									data["_extra"] = extra;
								}

								NRS.broadcastTransactionBytes(payload, callback, response, data);
							} else {
								NRS.broadcastTransactionBytes(payload, null, response, data);
							}
						}
					}
				}
			} else {
				if (response.errorCode || response.errorDescription || response.errorMessage || response.error) {
					response.errorDescription = NRS.translateServerError(response);
					delete response.fullHash;
					if (!response.errorCode) {
						response.errorCode = -1;
					}
				}

				/*
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : $.t("error_unknown"));
				} else if (response.error && !response.errorDescription) {
					response.errorDescription = (typeof response.error == "string" ? response.error : $.t("error_unknown"));
					if (!response.errorCode) {
						response.errorCode = 1;
					}
				}
				*/

				if (response.broadcasted == false) {
					NRS.showRawTransactionModal(response);
				} else {
					if (callback) {
						if (extra) {
							data["_extra"] = extra;
						}
						callback(response, data);
					}
					if (data.referencedTransactionFullHash && !response.errorCode) {
						$.growl($.t("info_referenced_transaction_hash"), {
							"type": "info"
						});
					}
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if ((error == "error" || textStatus == "error") && (xhr.status == 404 || xhr.status == 0)) {
				if (type == "POST") {
					$.growl($.t("error_server_connect"), {
						"type": "danger",
						"offset": 10
					});
				}
			}

			if (error == "abort") {
				return;
			} else if (callback) {
				if (error == "timeout") {
					error = $.t("error_request_timeout");
				}
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	NRS.verifyAndSignTransactionBytes = function(transactionBytes, signature, requestType, data) {
		var transaction = {};

		var byteArray = converters.hexStringToByteArray(transactionBytes);

		transaction.type = byteArray[0];

		transaction.version = (byteArray[1] & 0xF0) >> 4;
		transaction.subtype = byteArray[1] & 0x0F;

		transaction.timestamp = String(converters.byteArrayToSignedInt32(byteArray, 2));
		transaction.deadline = String(converters.byteArrayToSignedShort(byteArray, 6));
		transaction.publicKey = converters.byteArrayToHexString(byteArray.slice(8, 40));
		transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, 40));
		transaction.amountNQT = String(converters.byteArrayToBigInteger(byteArray, 48));
		transaction.feeNQT = String(converters.byteArrayToBigInteger(byteArray, 56));

		var refHash = byteArray.slice(64, 96);
		transaction.referencedTransactionFullHash = converters.byteArrayToHexString(refHash);
		if (transaction.referencedTransactionFullHash == "0000000000000000000000000000000000000000000000000000000000000000") {
			transaction.referencedTransactionFullHash = "";
		}
		//transaction.referencedTransactionId = converters.byteArrayToBigInteger([refHash[7], refHash[6], refHash[5], refHash[4], refHash[3], refHash[2], refHash[1], refHash[0]], 0);

		transaction.flags = 0;

		if (transaction.version > 0) {
			transaction.flags = converters.byteArrayToSignedInt32(byteArray, 160);
			transaction.ecBlockHeight = String(converters.byteArrayToSignedInt32(byteArray, 164));
			transaction.ecBlockId = String(converters.byteArrayToBigInteger(byteArray, 168));
		}

		if (!("amountNQT" in data)) {
			data.amountNQT = "0";
		}

		if (!("recipient" in data)) {
			//recipient == genesis
			data.recipient = "0";
			data.recipientRS = "BURST-2222-2222-2222-22222";
		}

		if (transaction.publicKey != NRS.accountInfo.publicKey) {
			return false;
		}

		if (transaction.deadline !== data.deadline) {
			return false;
		}

		if (transaction.recipient !== data.recipient) {
			if (data.recipient == "1739068987193023818" && transaction.recipient == "0") {
				//ok
			} else {
				return false;
			}
		}

		if (transaction.amountNQT !== data.amountNQT || transaction.feeNQT !== data.feeNQT) {
			return false;
		}

		if ("referencedTransactionFullHash" in data) {
			if (transaction.referencedTransactionFullHash !== data.referencedTransactionFullHash) {
				return false;
			}
		} else if (transaction.referencedTransactionFullHash !== "") {
			return false;
		}

		if (transaction.version > 0) {
			//has empty attachment, so no attachmentVersion byte...
			if (requestType == "sendMoney" || requestType == "sendMessage") {
				var pos = 176;
			} else {
				var pos = 177;
			}
		} else {
			var pos = 160;
		}

		switch (requestType) {
			case "sendMoney":
				if (transaction.type !== 0 || transaction.subtype !== 0) {
					return false;
				}
				break;
			case "sendMessage":
				if (transaction.type !== 1 || transaction.subtype !== 0) {
					return false;
				}

				break;
			case "setAlias":
				if (transaction.type !== 1 || transaction.subtype !== 1) {
					return false;
				}

				var aliasLength = parseInt(byteArray[pos], 10);

				pos++;

				transaction.aliasName = converters.byteArrayToString(byteArray, pos, aliasLength);

				pos += aliasLength;

				var uriLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.aliasURI = converters.byteArrayToString(byteArray, pos, uriLength);

				pos += uriLength;

				if (transaction.aliasName !== data.aliasName || transaction.aliasURI !== data.aliasURI) {
					return false;
				}
				break;

			case "setRewardRecipient":
				if (transaction.type !== 20 || transaction.subtype !== 0) {
					return false;
				}
				break;

			case "createPoll":
				if (transaction.type !== 1 || transaction.subtype !== 2) {
					return false;
				}

				var nameLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				var nr_options = byteArray[pos];

				pos++;

				for (var i = 0; i < nr_options; i++) {
					var optionLength = converters.byteArrayToSignedShort(byteArray, pos);

					pos += 2;

					transaction["option" + i] = converters.byteArrayToString(byteArray, pos, optionLength);

					pos += optionLength;
				}

				transaction.minNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.maxNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.optionsAreBinary = String(byteArray[pos]);

				pos++;

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.minNumberOfOptions !== data.minNumberOfOptions || transaction.maxNumberOfOptions !== data.maxNumberOfOptions || transaction.optionsAreBinary !== data.optionsAreBinary) {
					return false;
				}

				for (var i = 0; i < nr_options; i++) {
					if (transaction["option" + i] !== data["option" + i]) {
						return false;
					}
				}

				if (("option" + i) in data) {
					return false;
				}

				break;
			case "castVote":
				if (transaction.type !== 1 || transaction.subtype !== 3) {
					return false;
				}

				transaction.poll = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var voteLength = byteArray[pos];

				pos++;

				transaction.votes = [];

				for (var i = 0; i < voteLength; i++) {
					transaction.votes.push(byteArray[pos]);

					pos++;
				}

				return false;

				break;
			case "hubAnnouncement":
				if (transaction.type !== 1 || transaction.subtype != 4) {
					return false;
				}

				var minFeePerByte = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var numberOfUris = parseInt(byteArray[pos], 10);

				pos++;

				var uris = [];

				for (var i = 0; i < numberOfUris; i++) {
					var uriLength = parseInt(byteArray[pos], 10);

					pos++;

					uris[i] = converters.byteArrayToString(byteArray, pos, uriLength);

					pos += uriLength;
				}

				//do validation

				return false;

				break;
			case "setAccountInfo":
				if (transaction.type !== 1 || transaction.subtype != 5) {
					return false;
				}

				var nameLength = parseInt(byteArray[pos], 10);

				pos++;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				if (transaction.name !== data.name || transaction.description !== data.description) {
					return false;
				}

				break;
			case "sellAlias":
				if (transaction.type !== 1 || transaction.subtype !== 6) {
					return false;
				}

				var aliasLength = parseInt(byteArray[pos], 10);

				pos++;

				transaction.alias = converters.byteArrayToString(byteArray, pos, aliasLength);

				pos += aliasLength;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.alias !== data.aliasName || transaction.priceNQT !== data.priceNQT) {
					return false;
				}

				break;
			case "buyAlias":
				if (transaction.type !== 1 && transaction.subtype !== 7) {
					return false;
				}

				var aliasLength = parseInt(byteArray[pos], 10);

				pos++;

				transaction.alias = converters.byteArrayToString(byteArray, pos, aliasLength);

				pos += aliasLength;

				if (transaction.alias !== data.aliasName) {
					return false;
				}

				break;
			case "issueAsset":
				if (transaction.type !== 2 || transaction.subtype !== 0) {
					return false;
				}

				var nameLength = byteArray[pos];

				pos++;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.decimals = String(byteArray[pos]);

				pos++;

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.quantityQNT !== data.quantityQNT || transaction.decimals !== data.decimals) {
					return false;
				}

				break;
			case "transferAsset":
				if (transaction.type !== 2 || transaction.subtype !== 1) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.asset !== data.asset || transaction.quantityQNT !== data.quantityQNT) {
					return false;
				}
				break;
			case "placeAskOrder":
			case "placeBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "placeAskOrder" && transaction.subtype !== 2) {
					return false;
				} else if (requestType == "placeBidOrder" && transaction.subtype !== 3) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.asset !== data.asset || transaction.quantityQNT !== data.quantityQNT || transaction.priceNQT !== data.priceNQT) {
					return false;
				}
				break;
			case "cancelAskOrder":
			case "cancelBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "cancelAskOrder" && transaction.subtype !== 4) {
					return false;
				} else if (requestType == "cancelBidOrder" && transaction.subtype !== 5) {
					return false;
				}

				transaction.order = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.order !== data.order) {
					return false;
				}

				break;
			case "dgsListing":
				if (transaction.type !== 3 && transaction.subtype != 0) {
					return false;
				}

				var nameLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				var tagsLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.tags = converters.byteArrayToString(byteArray, pos, tagsLength);

				pos += tagsLength;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.tags !== data.tags || transaction.quantity !== data.quantity || transaction.priceNQT !== data.priceNQT) {
					return false;
				}

				break;
			case "dgsDelisting":
				if (transaction.type !== 3 && transaction.subtype !== 1) {
					return false;
				}

				transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.goods !== data.goods) {
					return false;
				}

				break;
			case "dgsPriceChange":
				if (transaction.type !== 3 && transaction.subtype !== 2) {
					return false;
				}

				transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.goods !== data.goods || transaction.priceNQT !== data.priceNQT) {
					return false;
				}

				break;
			case "dgsQuantityChange":
				if (transaction.type !== 3 && transaction.subtype !== 3) {
					return false;
				}

				transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.deltaQuantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				if (transaction.goods !== data.goods || transaction.deltaQuantity !== data.deltaQuantity) {
					return false;
				}

				break;
			case "dgsPurchase":
				if (transaction.type !== 3 && transaction.subtype !== 4) {
					return false;
				}

				transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.deliveryDeadlineTimestamp = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				if (transaction.goods !== data.goods || transaction.quantity !== data.quantity || transaction.priceNQT !== data.priceNQT || transaction.deliveryDeadlineTimestamp !== data.deliveryDeadlineTimestamp) {
					return false;
				}

				break;
			case "dgsDelivery":
				if (transaction.type !== 3 && transaction.subtype !== 5) {
					return false;
				}

				transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var encryptedGoodsLength = converters.byteArrayToSignedShort(byteArray, pos);

				var goodsLength = converters.byteArrayToSignedInt32(byteArray, pos);

				transaction.goodsIsText = goodsLength < 0; // ugly hack??

				if (goodsLength < 0) {
					goodsLength &= 2147483647;
				}

				pos += 4;

				transaction.goodsData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedGoodsLength));

				pos += encryptedGoodsLength;

				transaction.goodsNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));

				pos += 32;

				transaction.discountNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var goodsIsText = (transaction.goodsIsText ? "true" : "false");

				if (goodsIsText != data.goodsIsText) {
					return false;
				}

				if (transaction.purchase !== data.purchase || transaction.goodsData !== data.goodsData || transaction.goodsNonce !== data.goodsNonce || transaction.discountNQT !== data.discountNQT) {
					return false;
				}

				break;
			case "dgsFeedback":
				if (transaction.type !== 3 && transaction.subtype !== 6) {
					return false;
				}

				transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.purchase !== data.purchase) {
					return false;
				}

				break;
			case "dgsRefund":
				if (transaction.type !== 3 && transaction.subtype !== 7) {
					return false;
				}

				transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.refundNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				if (transaction.purchase !== data.purchase || transaction.refundNQT !== data.refundNQT) {
					return false;
				}

				break;
			case "leaseBalance":
				if (transaction.type !== 4 && transaction.subtype !== 0) {
					return false;
				}

				transaction.period = String(converters.byteArrayToSignedShort(byteArray, pos));

				pos += 2;

				if (transaction.period !== data.period) {
					return false;
				}

				break;
			default:
				//invalid requestType..
				return false;
		}

		var position = 1;

		//non-encrypted message
		if ((transaction.flags & position) != 0 || (requestType == "sendMessage" && data.message)) {
			var attachmentVersion = byteArray[pos];

			pos++;

			var messageLength = converters.byteArrayToSignedInt32(byteArray, pos);

			transaction.messageIsText = messageLength < 0; // ugly hack??

			if (messageLength < 0) {
				messageLength &= 2147483647;
			}

			pos += 4;

			if (transaction.messageIsText) {
				transaction.message = converters.byteArrayToString(byteArray, pos, messageLength);
			} else {
				var slice = byteArray.slice(pos, pos + messageLength);
				transaction.message = converters.byteArrayToHexString(slice);
			}

			pos += messageLength;

			var messageIsText = (transaction.messageIsText ? "true" : "false");

			if (messageIsText != data.messageIsText) {
				return false;
			}

			if (transaction.message !== data.message) {
				return false;
			}
		} else if (data.message) {
			return false;
		}

		position <<= 1;

		//encrypted note
		if ((transaction.flags & position) != 0) {
			var attachmentVersion = byteArray[pos];

			pos++;

			var encryptedMessageLength = converters.byteArrayToSignedInt32(byteArray, pos);

			transaction.messageToEncryptIsText = encryptedMessageLength < 0;

			if (encryptedMessageLength < 0) {
				encryptedMessageLength &= 2147483647;
			}

			pos += 4;

			transaction.encryptedMessageData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedMessageLength));

			pos += encryptedMessageLength;

			transaction.encryptedMessageNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));

			pos += 32;

			var messageToEncryptIsText = (transaction.messageToEncryptIsText ? "true" : "false");

			if (messageToEncryptIsText != data.messageToEncryptIsText) {
				return false;
			}

			if (transaction.encryptedMessageData !== data.encryptedMessageData || transaction.encryptedMessageNonce !== data.encryptedMessageNonce) {
				return false;
			}
		} else if (data.encryptedMessageData) {
			return false;
		}

		position <<= 1;

		if ((transaction.flags & position) != 0) {
			var attachmentVersion = byteArray[pos];

			pos++;

			var recipientPublicKey = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));

			if (recipientPublicKey != data.recipientPublicKey) {
				return false;
			}
			pos += 32;
		} else if (data.recipientPublicKey) {
			return false;
		}

		position <<= 1;

		if ((transaction.flags & position) != 0) {
			var attachmentVersion = byteArray[pos];

			pos++;

			var encryptedToSelfMessageLength = converters.byteArrayToSignedInt32(byteArray, pos);

			transaction.messageToEncryptToSelfIsText = encryptedToSelfMessageLength < 0;

			if (encryptedToSelfMessageLength < 0) {
				encryptedToSelfMessageLength &= 2147483647;
			}

			pos += 4;

			transaction.encryptToSelfMessageData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedToSelfMessageLength));

			pos += encryptedToSelfMessageLength;

			transaction.encryptToSelfMessageNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));

			pos += 32;

			var messageToEncryptToSelfIsText = (transaction.messageToEncryptToSelfIsText ? "true" : "false");

			if (messageToEncryptToSelfIsText != data.messageToEncryptToSelfIsText) {
				return false;
			}

			if (transaction.encryptToSelfMessageData !== data.encryptToSelfMessageData || transaction.encryptToSelfMessageNonce !== data.encryptToSelfMessageNonce) {
				return false;
			}
		} else if (data.encryptToSelfMessageData) {
			return false;
		}

		return transactionBytes.substr(0, 192) + signature + transactionBytes.substr(320);
	}

	NRS.broadcastTransactionBytes = function(transactionData, callback, originalResponse, originalData) {
		$.ajax({
			url: NRS.server + "/burst?requestType=broadcastTransaction",
			crossDomain: true,
			dataType: "json",
			type: "POST",
			timeout: 30000,
			async: true,
			data: {
				"transactionBytes": transactionData
			}
		}).done(function(response, status, xhr) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (callback) {
				if (response.errorCode) {
					if (!response.errorDescription) {
						response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
					}
					callback(response, originalData);
				} else if (response.error) {
					response.errorCode = 1;
					response.errorDescription = response.error;
					callback(response, originalData);
				} else {
					if ("transactionBytes" in originalResponse) {
						delete originalResponse.transactionBytes;
					}
					originalResponse.broadcasted = true;
					originalResponse.transaction = response.transaction;
					originalResponse.fullHash = response.fullHash;
					callback(originalResponse, originalData);
					if (originalData.referencedTransactionFullHash) {
						$.growl($.t("info_referenced_transaction_hash"), {
							"type": "info"
						});
					}
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if (callback) {
				if (error == "timeout") {
					error = $.t("error_request_timeout");
				}
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	return NRS;
}(NRS || {}, jQuery));