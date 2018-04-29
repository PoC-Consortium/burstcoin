/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.newlyCreatedAccount = false;

    BRS.allowLoginViaEnter = function() {
	$("#login_password").keypress(function(e) {
	    if (e.which == '13') {
		e.preventDefault();
		var password = $("#login_password").val();
		BRS.login(password);
	    }
	});
    }

    BRS.showLoginOrWelcomeScreen = function() {
	if (BRS.hasLocalStorage && localStorage.getItem("logged_in")) {
	    BRS.showLoginScreen();
	}
        else {
	    BRS.showWelcomeScreen();
	}
    }

    BRS.showLoginScreen = function() {
	$("#account_phrase_custom_panel, #account_phrase_generator_panel, #welcome_panel, #custom_passphrase_link").hide();
	$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
	$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
	$("#login_panel").show();
	setTimeout(function() {
	    $("#login_password").focus()
	}, 10);
    }

    BRS.showWelcomeScreen = function() {
	$("#login_panel, account_phrase_custom_panel, #account_phrase_generator_panel, #account_phrase_custom_panel, #welcome_panel, #custom_passphrase_link").hide();
	$("#welcome_panel").show();
    }

    BRS.registerUserDefinedAccount = function() {
	$("#account_phrase_generator_panel, #login_panel, #welcome_panel, #custom_passphrase_link").hide();
	$("#account_phrase_custom_panel :input:not(:button):not([type=submit])").val("");
	$("#account_phrase_generator_panel :input:not(:button):not([type=submit])").val("");
	$("#account_phrase_custom_panel").show();
	$("#registration_password").focus();
    }

    BRS.registerAccount = function() {
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
		$.getScript("js/crypto/3rdparty/seedrandom.min.js"),
		$.getScript("js/crypto/passphrasegenerator.js")
	    ).done(function() {
		$loading.hide();
		$loaded.show();

		PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
	    }).fail(function(jqxhr, settings, exception) {
		alert($.t("error_word_list"));
	    });
	}
        else {
	    $loading.hide();
	    $loaded.show();

	    PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
	}
    }

    BRS.verifyGeneratedPassphrase = function() {
	var password = $.trim($("#account_phrase_generator_panel .step_3 textarea").val());

	if (password != PassPhraseGenerator.passPhrase) {
	    $("#account_phrase_generator_panel .step_3 .callout").show();
	}
        else {
	    BRS.newlyCreatedAccount = true;
	    BRS.login(password);
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
	}
        else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
	    error = $.t("error_passphrase_strength");
	}
        else if (password != repeat) {
	    error = $.t("error_passphrase_match");
	}

	if (error) {
	    $("#account_phrase_custom_panel .callout").first().removeClass("callout-info").addClass("callout-danger").html(error);
	}
        else {
	    $("#registration_password, #registration_password_repeat").val("");
	    BRS.login(password);
	}
    });

    BRS.login = function(password, callback) {
	if (!password.length) {
	    $.notify($.t("error_passphrase_required_login"), {
		"type": "danger",
		"offset": 10
	    });
	    return;
	}
        else if (!BRS.isTestNet && password.length < 12 && $("#login_check_password_length").val() == 1) {
	    $("#login_check_password_length").val(0);
	    $("#login_error .callout").html($.t("error_passphrase_login_length"));
	    $("#login_error").show();
	    return;
	}
	else {
	    BRS.settings["remember_passphrase"] = $("#remember_password").is(":checked");
	    BRS.applySettings("remember_passphrase");
	    if ( BRS.hasLocalStorage ) {
		if ( BRS.settings["remember_passphrase"] ) {
		    localStorage.setItem("burst.passphrase", $("#login_password").val());
		}
		else {
		    localStorage.removeItem("burst.passphrase");
		}
            }
	}

	$("#login_password, #registration_password, #registration_password_repeat").val("");
	$("#login_check_password_length").val(1);

	BRS.sendRequest("getBlockchainStatus", function(response) {
	    if (response.errorCode) {
		$.notify($.t("error_server_connect"), {
		    "type": "danger",
		    "offset": 10
		});

		return;
	    }

	    BRS.state = response;

	    var login_response_function = function(response) {
		if (!response.errorCode) {
		    BRS.account = String(response.account).escapeHTML();
		    BRS.accountRS = String(response.accountRS).escapeHTML();
		    BRS.publicKey = BRS.getPublicKey(converters.stringToHexString(password));
		}

		if (!BRS.account) {
		    $.notify($.t("error_find_account_id"), {
			"type": "danger",
			"offset": 10
		    });
		    return;
		}
                else if (!BRS.accountRS) {
		    $.notify($.t("error_generate_account_id"), {
			"type": "danger",
			"offset": 10
		    });
		    return;
		}

		var watch_only = response.watch_only;
		BRS.sendRequest("getAccountPublicKey", {
		    "account": BRS.account
		}, function(response) {
		    if (response && response.publicKey && response.publicKey != BRS.generatePublicKey(password)) {
			if (watch_only != true) {
			    $.notify($.t("error_account_taken"), {
				"type": "danger",
				"offset": 10
			    });
			    return;
			}
                        else {
			    // Can't use stadard 'dashboard_status' div because it is cleared by other functions.
			    // So create a similar div for this purpose
			    $(".content").prepend($('<div class="alert-danger alert alert-no-icon" style="padding: 5px; margin-bottom: 15px;">You are logged in as a watch-only address.  You will need the full passphrase for most operations.</div>'));
			}						
		    }

		    if ($("#remember_password").is(":checked")) {
			BRS.rememberPassword = true;
			$("#remember_password").prop("checked", false);
			BRS.setPassword(password);
			$(".secret_phrase, .show_secret_phrase").hide();
			$(".hide_secret_phrase").show();
		    }

		    $("#account_id").html(String(BRS.accountRS).escapeHTML()).css("font-size", "12px");

		    var passwordNotice = "";

		    if (watch_only != true && password.length < 35) {
			passwordNotice = $.t("error_passphrase_length_secure");
		    }
                    else if (password.length < 50 && (!password.match(/[A-Z]/) || !password.match(/[0-9]/))) {
			passwordNotice = $.t("error_passphrase_strength_secure");
		    }

		    if (passwordNotice) {
			$.notify("<strong>" + $.t("warning") + "</strong>: " + passwordNotice, {
			    "type": "danger"
			});
		    }

		    if (BRS.state) {
			BRS.checkBlockHeight();
		    }

		    BRS.getAccountInfo(true, function() {
			if (BRS.accountInfo.currentLeasingHeightFrom) {
			    BRS.isLeased = (BRS.lastBlockHeight >= BRS.accountInfo.currentLeasingHeightFrom && BRS.lastBlockHeight <= BRS.accountInfo.currentLeasingHeightTo);
			}
                        else {
			    BRS.isLeased = false;
			}

			//forging requires password to be sent to the server, so we don't do it automatically if not localhost
			if (!BRS.accountInfo.publicKey || BRS.accountInfo.effectiveBalanceBURST == 0 || !BRS.isLocalHost || BRS.downloadingBlockchain || BRS.isLeased) {
			    $("#forging_indicator").removeClass("forging");
			    $("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
			    $("#forging_indicator").show();
			    BRS.isForging = false;
			}
                        else if (BRS.isLocalHost) {
			    BRS.sendRequest("startForging", {
				"secretPhrase": password
			    }, function(response) {
				if ("deadline" in response) {
				    $("#forging_indicator").addClass("forging");
				    $("#forging_indicator span").html($.t("forging")).attr("data-i18n", "forging");
				    BRS.isForging = true;
				}
                                else {
				    $("#forging_indicator").removeClass("forging");
				    $("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
				    BRS.isForging = false;
				}
				$("#forging_indicator").show();
			    });
			}
		    });

		    //BRS.getAccountAliases();

		    BRS.unlock();

		    if (BRS.isOutdated) {
			$.notify($.t("brs_update_available"), {
			    "type": "danger"
			});
		    }

		    if (!BRS.downloadingBlockchain) {
			BRS.checkIfOnAFork();
		    }

		    BRS.setupClipboardFunctionality();

		    if (callback) {
			callback();
		    }

		    BRS.checkLocationHash(password);

		    $(window).on("hashchange", BRS.checkLocationHash);

		    BRS.getInitialTransactions();
		});
	    };

	    if (password.trim().toUpperCase().substring(0, 6) == "BURST-" && password.length == 26) {
		// Login to a watch-only address
		var account_id = password.trim();

		// Get the account information for the given address				
		BRS.sendRequest("getAccount", {
		    "account": account_id
		}, function(response) {
		    // If it is successful, set the "watch_only" flag and all the standard
		    // login response logic.
		    if (!response.errorCode) {
			response.watch_only = true;  // flag to tell later code to disable some checks.
			login_response_function(response);
		    }
                    else {
			// Otherwise, show an error.  The address is in the right format perhaps, but
			// an address does not exist on the blockchain so there's nothing to see.						
			$.notify("<strong>" + $.t("warning") + "</strong>: " + response.errorDescription, {
			    "type": "danger"
			});
		    }
		});
	    }
            else {
		// Standard login logic
		// this is done locally..  'sendRequest' has special logic to prevent
		// transmitting the passphrase to the server unncessarily via BRS.getAccountId()
		BRS.sendRequest("getAccountId", {
		    "secretPhrase": password
		}, login_response_function);
	    }


	});
    }

    $("#logout_button_container").on("show.bs.dropdown", function(e) {
	if (!BRS.isForging) {
	    e.preventDefault();
	}
    });

    BRS.showLockscreen = function() {
	if (BRS.hasLocalStorage && localStorage.getItem("logged_in")) {
	    setTimeout(function() {
		$("#login_password").focus()
	    }, 10);
	}
        else {
	    BRS.showWelcomeScreen();
	}

	$("#center").show();
    }

    BRS.unlock = function() {
	if (BRS.hasLocalStorage && !localStorage.getItem("logged_in")) {
	    localStorage.setItem("logged_in", true);
	}

	var userStyles = ["header", "sidebar", "boxes"];

	for (var i = 0; i < userStyles.length; i++) {
	    var color = BRS.settings[userStyles[i] + "_color"];
	    if (color) {
		BRS.updateStyle(userStyles[i], color);
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
	if (!BRS.isForging) {
	    e.preventDefault();
	    BRS.logout();
	}
    });

    BRS.logout = function(stopForging) {
	if (stopForging && BRS.isForging) {
	    $("#stop_forging_modal .show_logout").show();
	    $("#stop_forging_modal").modal("show");
	}
        else {
	    BRS.setDecryptionPassword("");
	    BRS.setPassword("");
	    window.location.reload();
	}
    }

    BRS.setPassword = function(password) {
	BRS.setEncryptionPassword(password);
	BRS.setServerPassword(password);
    }
    return BRS;
}(BRS || {}, jQuery));
