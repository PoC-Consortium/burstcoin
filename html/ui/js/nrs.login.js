/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.newlyCreatedAccount = false;

	NRS.allowLoginViaEnter = function() {
		$("#login_password").keypress(function(e) {
			if (e.which == '13') {
				e.preventDefault();
				var password = $("#login_password").val();
				NRS.login(password);
			}
		});
	}

	NRS.showLoginOrWelcomeScreen = function() {
		if (NRS.hasLocalStorage && localStorage.getItem("logged_in")) {
			NRS.showLoginScreen();
		} else {
			NRS.showWelcomeScreen();
		}
	}

	NRS.showLoginScreen = function() {
		$("#account_phrase_custom_panel, #account_phrase_generator_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
		$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
		$("#login_panel").show();
		setTimeout(function() {
			$("#login_password").focus()
		}, 10);
	}

	NRS.showWelcomeScreen = function() {
		$("#login_panel, account_phrase_custom_panel, #account_phrase_generator_panel, #account_phrase_custom_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#welcome_panel").show();
	}

	NRS.registerUserDefinedAccount = function() {
		$("#account_phrase_generator_panel, #login_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
		$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
		$("#account_phrase_custom_panel").show();
		$("#registration_password").focus();
	}

	NRS.registerAccount = function() {
		$("#login_panel, #welcome_panel").hide();
		$("#account_phrase_generator_panel").show();
		$("#account_phrase_generator_panel step_3 .callout").hide();

		var $loading = $("#account_phrase_generator_loading");
		var $loaded = $("#account_phrase_generator_loaded");

		if (window.crypto || window.msCrypto) {
			$loading.find("span.loading_text").html($.t("generating_passphrase_wait"));
		}

		$loading.show();
		$loaded.hide();

		if (typeof PassPhraseGenerator == "undefined") {
			$.when(
				$.getScript("js/crypto/3rdparty/seedrandom.js"),
				$.getScript("js/crypto/passphrasegenerator.js")
			).done(function() {
				$loading.hide();
				$loaded.show();

				PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
			}).fail(function(jqxhr, settings, exception) {
				alert($.t("error_word_list"));
			});
		} else {
			$loading.hide();
			$loaded.show();

			PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
		}
	}

	NRS.verifyGeneratedPassphrase = function() {
		var password = $.trim($("#account_phrase_generator_panel .step_3 textarea").val());

		if (password != PassPhraseGenerator.passPhrase) {
			$("#account_phrase_generator_panel .step_3 .callout").show();
		} else {
			NRS.newlyCreatedAccount = true;
			NRS.login(password);
			PassPhraseGenerator.reset();
			$("#account_phrase_generator_panel textarea").val("");
			$("#account_phrase_generator_panel .step_3 .callout").hide();
		}
	}

	$("#account_phrase_custom_panel form").submit(function(event) {
		event.preventDefault()

		var password = $("#registration_password").val();
		var repeat = $("#registration_password_repeat").val();

		var error = "";

		if (password.length < 35) {
			error = $.t("error_passphrase_length");
		} else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
			error = $.t("error_passphrase_strength");
		} else if (password != repeat) {
			error = $.t("error_passphrase_match");
		}

		if (error) {
			$("#account_phrase_custom_panel .callout").first().removeClass("callout-info").addClass("callout-danger").html(error);
		} else {
			$("#registration_password, #registration_password_repeat").val("");
			NRS.login(password);
		}
	});

	NRS.login = function(password, callback) {
		if (!password.length) {
			$.growl($.t("error_passphrase_required_login"), {
				"type": "danger",
				"offset": 10
			});
			return;
		} else if (!NRS.isTestNet && password.length < 12 && $("#login_check_password_length").val() == 1) {
			$("#login_check_password_length").val(0);
			$("#login_error .callout").html($.t("error_passphrase_login_length"));
			$("#login_error").show();
			return;
		}
		else {
			NRS.settings["remember_passphrase"] = $("#remember_password").is(":checked");
			NRS.applySettings("remember_passphrase");
			if ( NRS.hasLocalStorage ) {
				if ( NRS.settings["remember_passphrase"] ) {
					localStorage.setItem("burst.passphrase", $("#login_password").val());
				}
				else {
					localStorage.removeItem("burst.passphrase");
				}
                    }
		}

		$("#login_password, #registration_password, #registration_password_repeat").val("");
		$("#login_check_password_length").val(1);

		NRS.sendRequest("getBlockchainStatus", function(response) {
			if (response.errorCode) {
				$.growl($.t("error_server_connect"), {
					"type": "danger",
					"offset": 10
				});

				return;
			}

			NRS.state = response;

			var login_response_function = function(response) {
				if (!response.errorCode) {
					NRS.account = String(response.account).escapeHTML();
					NRS.accountRS = String(response.accountRS).escapeHTML();
					NRS.publicKey = NRS.getPublicKey(converters.stringToHexString(password));
				}

				if (!NRS.account) {
					$.growl($.t("error_find_account_id"), {
						"type": "danger",
						"offset": 10
					});
					return;
				} else if (!NRS.accountRS) {
					$.growl($.t("error_generate_account_id"), {
						"type": "danger",
						"offset": 10
					});
					return;
				}

				var watch_only = response.watch_only;
				NRS.sendRequest("getAccountPublicKey", {
					"account": NRS.account
				}, function(response) {
					if (response && response.publicKey && response.publicKey != NRS.generatePublicKey(password)) {
						if (watch_only != true) {
							$.growl($.t("error_account_taken"), {
								"type": "danger",
								"offset": 10
							});
							return;
						} else {
							// Can't use stadard 'dashboard_status' div because it is cleared by other functions.
							// So create a similar div for this purpose
							var watch_only_message = "You are logged in as a watch-only address.  You will need the full passphrase for most operations."
							$(".content").prepend($(`<div class="alert-danger alert alert-no-icon" style="padding: 5px; margin-bottom: 15px;">${watch_only_message}</div>`));
						}						
					}

				        if ($("#remember_password").is(":checked")) {
					        NRS.rememberPassword = true;
						$("#remember_password").prop("checked", false);
						NRS.setPassword(password);
						$(".secret_phrase, .show_secret_phrase").hide();
						$(".hide_secret_phrase").show();
				        }

					$("#account_id").html(String(NRS.accountRS).escapeHTML()).css("font-size", "12px");

					var passwordNotice = "";

					if (watch_only != true && password.length < 35) {
						passwordNotice = $.t("error_passphrase_length_secure");
					} else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
						passwordNotice = $.t("error_passphrase_strength_secure");
					}

					if (passwordNotice) {
						$.growl("<strong>" + $.t("warning") + "</strong>: " + passwordNotice, {
							"type": "danger"
						});
					}

					if (NRS.state) {
						NRS.checkBlockHeight();
					}

					NRS.getAccountInfo(true, function() {
						if (NRS.accountInfo.currentLeasingHeightFrom) {
							NRS.isLeased = (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo);
						} else {
							NRS.isLeased = false;
						}

						//forging requires password to be sent to the server, so we don't do it automatically if not localhost
						if (!NRS.accountInfo.publicKey || NRS.accountInfo.effectiveBalanceNXT == 0 || !NRS.isLocalHost || NRS.downloadingBlockchain || NRS.isLeased) {
							$("#forging_indicator").removeClass("forging");
							$("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
							$("#forging_indicator").show();
							NRS.isForging = false;
						} else if (NRS.isLocalHost) {
							NRS.sendRequest("startForging", {
								"secretPhrase": password
							}, function(response) {
								if ("deadline" in response) {
									$("#forging_indicator").addClass("forging");
									$("#forging_indicator span").html($.t("forging")).attr("data-i18n", "forging");
									NRS.isForging = true;
								} else {
									$("#forging_indicator").removeClass("forging");
									$("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
									NRS.isForging = false;
								}
								$("#forging_indicator").show();
							});
						}
					});

					//NRS.getAccountAliases();

					NRS.unlock();

					if (NRS.isOutdated) {
						$.growl($.t("nrs_update_available"), {
							"type": "danger"
						});
					}

					if (!NRS.downloadingBlockchain) {
						NRS.checkIfOnAFork();
					}

					NRS.setupClipboardFunctionality();

					if (callback) {
						callback();
					}

					NRS.checkLocationHash(password);

					$(window).on("hashchange", NRS.checkLocationHash);

					NRS.getInitialTransactions();
				});
			};

			if (password.trim().toUpperCase().startsWith("BURST-") && password.length == 26) {
				// Login to a watch-only address
				var account_id = password.trim();

				// Get the account information for the given address				
				NRS.sendRequest("getAccount", {
					"account": account_id
				}, function(response) {
					// If it is successful, set the "watch_only" flag and all the standard
					// login response logic.
					if (!response.errorCode) {
						response.watch_only = true;  // flag to tell later code to disable some checks.
						login_response_function(response);
					} else {
						// Otherwise, show an error.  The address is in the right format perhaps, but
						// an address does not exist on the blockchain so there's nothing to see.						
						$.growl("<strong>" + $.t("warning") + "</strong>: " + response.errorDescription, {
							"type": "danger"
						});
					}
				});
			} else {
				// Standard login logic
				// this is done locally..  'sendRequest' has special logic to prevent
				// transmitting the passphrase to the server unncessarily via NRS.getAccountId()
				NRS.sendRequest("getAccountId", {
					"secretPhrase": password
				}, login_response_function);
			}


		});
	}

	$("#logout_button_container").on("show.bs.dropdown", function(e) {
		if (!NRS.isForging) {
			e.preventDefault();
		}
	});

	NRS.showLockscreen = function() {
		if (NRS.hasLocalStorage && localStorage.getItem("logged_in")) {
			setTimeout(function() {
				$("#login_password").focus()
			}, 10);
		} else {
			NRS.showWelcomeScreen();
		}

		$("#center").show();
	}

	NRS.unlock = function() {
		if (NRS.hasLocalStorage && !localStorage.getItem("logged_in")) {
			localStorage.setItem("logged_in", true);
		}

		var userStyles = ["header", "sidebar", "boxes"];

		for (var i = 0; i < userStyles.length; i++) {
			var color = NRS.settings[userStyles[i] + "_color"];
			if (color) {
				NRS.updateStyle(userStyles[i], color);
			}
		}

		var contentHeaderHeight = $(".content-header").height();
		var navBarHeight = $("nav.navbar").height();

		//	$(".content-splitter-right").css("bottom", (contentHeaderHeight + navBarHeight + 10) + "px");

		$("#lockscreen").hide();
		$("body, html").removeClass("lockscreen");

		$("#login_error").html("").hide();

		$(document.documentElement).scrollTop(0);
	}

	$("#logout_button").click(function(e) {
		if (!NRS.isForging) {
			e.preventDefault();
			NRS.logout();
		}
	});

	NRS.logout = function(stopForging) {
		if (stopForging && NRS.isForging) {
			$("#stop_forging_modal .show_logout").show();
			$("#stop_forging_modal").modal("show");
		} else {
			NRS.setDecryptionPassword("");
			NRS.setPassword("");
			window.location.reload();
		}
	}

	NRS.setPassword = function(password) {
		NRS.setEncryptionPassword(password);
		NRS.setServerPassword(password);
	}
	return NRS;
}(NRS || {}, jQuery));
