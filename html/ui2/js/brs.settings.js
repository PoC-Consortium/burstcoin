/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.defaultSettings = {
	"submit_on_enter": 0,
	"animate_forging": 1,
	"news": -1,
	"console_log": 0,
	"fee_warning": "100000000000",
	"amount_warning": "10000000000000",
	"asset_transfer_warning": "10000",
	"24_hour_format": 1,
	"remember_passphrase": 0,
	"language": "en"
    };
    BRS.pages.settings = function() {
	for (var key in BRS.settings) {
	    if (/_warning/i.test(key) && key != "asset_transfer_warning") {
		if ($("#settings_" + key).length) {
		    $("#settings_" + key).val(BRS.convertToNXT(BRS.settings[key]));
		}
	    }
            else if (!/_color/i.test(key)) {
		if ($("#settings_" + key).length) {
		    $("#settings_" + key).val(BRS.settings[key]);
		}
	    }
	}

	if (BRS.settings.news != -1) {
	    $("#settings_news_initial").remove();
	}

	if (BRS.inApp) {
	    $("#settings_console_log_div").hide();
	}

	BRS.pageLoaded();
    };

    function getCssGradientStyle(start, stop, vertical) {
	var output = "";

	var startPosition = (vertical ? "left" : "top");

	output += "background-image: -moz-linear-gradient(" + startPosition + ", " + start + ", " + stop + ");";
	output += "background-image: -ms-linear-gradient(" + startPosition + ", " + start + ", " + stop + ");";
	output += "background-image: -webkit-gradient(linear, " + (vertical ? "left top, right top" : "0 0, 0 100%") + ", from(" + start + "), to(" + stop + "));";
	output += "background-image: -webkit-linear-gradient(" + startPosition + ", " + start + ", " + stop + ");";
	output += "background-image: -o-linear-gradient(" + startPosition + ", " + start + ", " + stop + ");";
	output += "background-image: linear-gradient(" + startPosition + ", " + start + ", " + stop + ");";
	output += "filter: progid:dximagetransform.microsoft.gradient(startColorstr='" + start + "', endColorstr='" + stop + "', GradientType=" + (vertical ? "1" : "0") + ");";
	return output;
    }
    BRS.getSettings = function() {
	if (BRS.databaseSupport) {
	    BRS.database.select("data", [{
		"id": "settings"
	    }], function(error, result) {
		if (result && result.length) {
		    BRS.settings = $.extend({}, BRS.defaultSettings, JSON.parse(result[0].contents));
		}
                else {
		    BRS.database.insert("data", {
			id: "settings",
			contents: "{}"
		    });
		    BRS.settings = BRS.defaultSettings;
		}
		BRS.applySettings();
	    });
	}
        else {
	    if (BRS.hasLocalStorage) {
		BRS.settings = $.extend({}, BRS.defaultSettings, JSON.parse(localStorage.getItem("settings")));
	    }
            else {
		BRS.settings = BRS.defaultSettings;
	    }
	    BRS.applySettings();
	}
    };

    BRS.applySettings = function(key) {
	if (!key || key == "language") {
	    if ($.i18n.lng() != BRS.settings.language) {
		$.i18n.setLng(BRS.settings.language, null, function() {
		    $("[data-i18n]").i18n();
		});
		if (key && window.localstorage) {
		    window.localStorage.setItem('i18next_lng', BRS.settings.language);
		}
		if (BRS.inApp) {
		    parent.postMessage({
			"type": "language",
			"version": BRS.settings.language
		    }, "*");
		}
	    }
	}

	if (!key || key == "submit_on_enter") {
	    if (BRS.settings.submit_on_enter) {
		$(".modal form:not('#decrypt_note_form_container')").on("submit.onEnter", function(e) {
		    e.preventDefault();
		    BRS.submitForm($(this).closest(".modal"));
		});
	    }
            else {
		$(".modal form").off("submit.onEnter");
	    }
	}

	if (!key || key == "animate_forging") {
	    if (BRS.settings.animate_forging) {
		$("#forging_indicator").addClass("animated");
	    }
            else {
		$("#forging_indicator").removeClass("animated");
	    }
	}

	if (!key || key == "news") {
	    if (BRS.settings.news === 0) {
		$("#news_link").hide();
	    }
            else if (BRS.settings.news == 1) {
		$("#news_link").show();
	    }
	}

	if (!BRS.inApp && !BRS.downloadingBlockchain) {
	    if (!key || key == "console_log") {
		if (BRS.settings.console_log === 0) {
		    $("#show_console").hide();
		}
                else {
		    $("#show_console").show();
		}
	    }
	}
        else if (BRS.inApp) {
	    $("#show_console").hide();
	}

	if (key == "24_hour_format") {
	    var $dashboard_dates = $("#dashboard_transactions_table a[data-timestamp], #dashboard_blocks_table td[data-timestamp]");

	    $.each($dashboard_dates, function(key, value) {
		$(this).html(BRS.formatTimestamp($(this).data("timestamp")));
	    });
	}

	if (!key || key == "remember_passphrase") {
	    if (BRS.settings.remember_passphrase) {
		BRS.setCookie("remember_passphrase", 1, 1000);
	    }
            else {
		BRS.deleteCookie("remember_passphrase");
	    }
	}
    };
    BRS.updateSettings = function(key, value) {
	if (key) {
	    BRS.settings[key] = value;
	}

	if (BRS.databaseSupport) {
	    BRS.database.update("data", {
		contents: JSON.stringify(BRS.settings)
	    }, [{
		id: "settings"
	    }]);
	}
        else if (BRS.hasLocalStorage) {
	    localStorage.setItem("settings", JSON.stringify(BRS.settings));
	}

	BRS.applySettings(key);
    };

    $("#settings_box select").on("change", function(e) {
	e.preventDefault();

	var key = $(this).attr("name");
	var value = $(this).val();

	BRS.updateSettings(key, value);
    });

    $("#settings_box input[type=text]").on("input", function(e) {
	var key = $(this).attr("name");
	var value = $(this).val();

	if (/_warning/i.test(key) && key != "asset_transfer_warning") {
	    value = BRS.convertToNQT(value);
	}
	BRS.updateSettings(key, value);
    });

    return BRS;
}(BRS || {}, jQuery));
