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

    BRS.defaultColors = {
	"header": "#084F6C",
	"sidebar": "#F4F4F4",
	"boxes": "#3E96BB"
    };

    var userStyles = {};

    userStyles.header = {
	"blue": {
	    "header_bg": "#3c8dbc",
	    "logo_bg": "#367fa9",
	    "link_bg_hover": "#357ca5"
	},
	"green": {
	    "header_bg": "#29BB9C",
	    "logo_bg": "#26AE91",
	    "link_bg_hover": "#1F8E77"
	},
	"red": {
	    "header_bg": "#cb4040",
	    "logo_bg": "#9e2b2b",
	    "link_bg_hover": "#9e2b2b",
	    "toggle_icon": "#d97474"
	},
	"brown": {
	    "header_bg": "#ba5d32",
	    "logo_bg": "#864324",
	    "link_bg_hover": "#864324",
	    "toggle_icon": "#d3815b"
	},
	"purple": {
	    "header_bg": "#86618f",
	    "logo_bg": "#614667",
	    "link_bg_hover": "#614667",
	    "toggle_icon": "#a586ad"
	},
	"gray": {
	    "header_bg": "#575757",
	    "logo_bg": "#363636",
	    "link_bg_hover": "#363636",
	    "toggle_icon": "#787878"
	},
	"pink": {
	    "header_bg": "#b94b6f",
	    "logo_bg": "#8b3652",
	    "link_bg_hover": "#8b3652",
	    "toggle_icon": "#cc7b95"
	},
	"bright-blue": {
	    "header_bg": "#2494F2",
	    "logo_bg": "#2380cf",
	    "link_bg_hover": "#36a3ff",
	    "toggle_icon": "#AEBECD"
	},
	"dark-blue": {
	    "header_bg": "#25313e",
	    "logo_bg": "#1b252e",
	    "link_txt": "#AEBECD",
	    "link_bg_hover": "#1b252e",
	    "link_txt_hover": "#fff",
	    "toggle_icon": "#AEBECD"
	}
    };

    userStyles.sidebar = {
	"dark-gray": {
	    "sidebar_bg": "#272930",
	    "user_panel_txt": "#fff",
	    "sidebar_top_border": "#1a1c20",
	    "sidebar_bottom_border": "#2f323a",
	    "menu_item_top_border": "#32353e",
	    "menu_item_bottom_border": "#1a1c20",
	    "menu_item_txt": "#c9d4f6",
	    "menu_item_bg_hover": "#2a2c34",
	    "menu_item_border_active": "#2494F2",
	    "submenu_item_bg": "#2A2A2A",
	    "submenu_item_txt": "#fff",
	    "submenu_item_bg_hover": "#222222"
	},
	"dark-blue": {
	    "sidebar_bg": "#34495e",
	    "user_panel_txt": "#fff",
	    "sidebar_top_border": "#142638",
	    "sidebar_bottom_border": "#54677a",
	    "menu_item_top_border": "#54677a",
	    "menu_item_bottom_border": "#142638",
	    "menu_item_txt": "#fff",
	    "menu_item_bg_hover": "#3d566e",
	    "menu_item_bg_active": "#2c3e50",
	    "submenu_item_bg": "#ECF0F1",
	    "submenu_item_bg_hover": "#E0E7E8",
	    "submenu_item_txt": "#333333"
	}
    };

    userStyles.boxes = {
	"green": {
	    "bg": "#34d2b1",
	    "bg_gradient": "#87e5d1"
	},
	"red": {
	    "bg": "#d25b5b",
	    "bg_gradient": "#da7575"
	},
	"brown": {
	    "bg": "#c76436",
	    "bg_gradient": "#d3825d"
	},
	"purple": {
	    "bg": "#8f6899",
	    "bg_gradient": "#a687ad"
	},
	"gray": {
	    "bg": "#5f5f5f",
	    "bg_gradient": "#797979"
	},
	"pink": {
	    "bg": "#be5779",
	    "bg_gradient": "#cc7c96"
	},
	"bright-blue": {
	    "bg": "#349cf3",
	    "bg_gradient": "#64b3f6"
	},
	"dark-blue": {
	    "bg": "#2b3949",
	    "bg_gradient": "#3e5369"
	}

    };

    BRS.pages.settings = function() {
	for (var style in userStyles) {
	    var $dropdown = $("#" + style + "_color_scheme");

	    $dropdown.empty();

	    $dropdown.append("<li><a href='#' data-color=''><span class='color' style='background-color:" + BRS.defaultColors[style] + ";border:1px solid black;'></span>Default</a></li>");

	    $.each(userStyles[style], function(key, value) {
		var bg = "";
		if (value.bg) {
		    bg = value.bg;
		}
                else if (value.header_bg) {
		    bg = value.header_bg;
		}
                else if (value.sidebar_bg) {
		    bg = value.sidebar_bg;
		}

		$dropdown.append("<li><a href='#' data-color='" + key + "'><span class='color' style='background-color: " + bg + ";border:1px solid black;'></span> " + key.replace("-", " ") + "</a></li>");
	    });

	    var $span = $dropdown.closest(".btn-group.colors").find("span.text");

	    var color = BRS.settings[style + "_color"];

	    if (!color) {
		colorTitle = "Default";
	    }
            else {
		var colorTitle = color.replace(/-/g, " ");
		colorTitle = colorTitle.replace(/\w\S*/g, function(txt) {
		    return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
		});
	    }

	    $span.html(colorTitle);
	}

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

	if (BRS.settings["news"] != -1) {
	    $("#settings_news_initial").remove();
	}

	if (BRS.inApp) {
	    $("#settings_console_log_div").hide();
	}

	BRS.pageLoaded();
    }

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

    BRS.updateStyle = function(type, color) {
	var css = "";

	if ($.isPlainObject(color)) {
	    var colors = color;
	}
        else {
	    var colors = userStyles[type][color];
	}

	if (colors) {
	    switch (type) {
	    case "boxes":
		css += ".small-box { background: " + colors.bg + "; " + getCssGradientStyle(colors.bg, colors.bg_gradient, true) + " }";

		break;
	    case "header":
		if (!colors.link_txt) {
		    colors.link_txt = "#fff";
		}
		if (!colors.toggle_icon) {
		    colors.toggle_icon = "#fff";
		}
		if (!colors.toggle_icon_hover) {
		    colors.toggle_icon_hover = "#fff";
		}
		if (!colors.link_txt_hover) {
		    colors.link_txt_hover = colors.link_txt;
		}
		if (!colors.link_bg_hover && colors.link_bg) {
		    colors.link_bg_hover = colors.link_bg;
		}

		if (!colors.logo_bg) {
		    css += ".header { background:" + colors.header_bg + " }";
		    if (colors.header_bg_gradient) {
			css += ".header { " + getCssGradientStyle(colors.header_bg, colors.header_bg_gradient) + " }";
		    }
		    css += ".header .navbar { background: inherit }";
		    css += ".header .logo { background: inherit }";
		}
                else {
		    css += ".header .navbar { background:" + colors.header_bg + " }";

		    if (colors.header_bg_gradient) {
			css += ".header .navbar { " + getCssGradientStyle(colors.header_bg, colors.header_bg_gradient) + " }";
		    }

		    css += ".header .logo { background: " + colors.logo_bg + " }";

		    if (colors.logo_bg_gradient) {
			css += ".header .logo { " + getCssGradientStyle(colors.logo_bg, colors.logo_bg_gradient) + " }";
		    }
		}

		css += ".header .navbar .nav a { color: " + colors.link_txt + (colors.link_bg ? "; background:" + colors.link_bg : "") + " }";
		css += ".header .navbar .nav > li > a:hover, .header .navbar .nav > li > a:focus, .header .navbar .nav > li > a:focus { color: " + colors.link_txt_hover + (colors.link_bg_hover ? "; background:" + colors.link_bg_hover : "") + " }";

		if (colors.link_bg_hover) {
		    css += ".header .navbar .nav > li > a:hover { " + getCssGradientStyle(colors.link_bg_hover, colors.link_bg_hover_gradient) + " }";
		}

		css += ".header .navbar .nav > li > ul a { color: #444444; }";
		css += ".header .navbar .nav > li > ul a:hover {  color: " + colors.link_txt_hover + (colors.link_bg_hover ? "; background:" + colors.link_bg_hover : "") + " }";

		css += ".header .navbar .sidebar-toggle .icon-bar { background: " + colors.toggle_icon + " }";
		css += ".header .navbar .sidebar-toggle:hover .icon-bar { background: " + colors.toggle_icon_hover + " }";

		if (colors.link_border) {
		    css += ".header .navbar .nav > li { border-left: 1px solid " + colors.link_border + " }";
		}

		if (colors.link_border_inset) {
		    css += ".header .navbar .nav > li { border-right: 1px solid " + colors.link_border_inset + " }";
		    css += ".header .navbar .nav > li:last-child { border-right:none }";
		    css += ".header .navbar .nav { border-left: 1px solid " + colors.link_border_inset + " }";
		}

		if (colors.header_border) {
		    css += ".header { border-bottom: 1px solid " + colors.header_border + " }";
		}
		break;
	    case "sidebar":
		if (!colors.user_panel_link) {
		    colors.user_panel_link = colors.user_panel_txt;
		}
		if (!colors.menu_item_bg) {
		    colors.menu_item_bg = colors.sidebar_bg;
		}
		if (!colors.menu_item_bg_active) {
		    colors.menu_item_bg_active = colors.menu_item_bg_hover;
		}
		if (!colors.menu_item_txt_hover) {
		    colors.menu_item_txt_hover = colors.menu_item_txt;
		}
		if (!colors.menu_item_txt_active) {
		    colors.menu_item_txt_active = colors.menu_item_txt_hover;
		}
		if (!colors.menu_item_border_active && colors.menu_item_border_hover) {
		    colors.menu_item_border_active = colors.menu_item_border_hover;
		}
		if (!colors.menu_item_border_size) {
		    colors.menu_item_border_size = 1;
		}

		css += ".left-side { background: " + colors.sidebar_bg + " }";

		css += ".left-side .user-panel > .info { color: " + colors.user_panel_txt + " }";

		if (colors.user_panel_bg) {
		    css += ".left-side .user-panel { background: " + colors.user_panel_bg + " }";
		    if (colors.user_panel_bg_gradient) {
			css += ".left-side .user-panel { " + getCssGradientStyle(colors.user_panel_bg, colors.user_panel_bg_gradient) + " }";
		    }
		}

		css += ".left-side .user-panel a { color:" + colors.user_panel_link + " }";

		if (colors.sidebar_top_border || colors.sidebar_bottom_border) {
		    css += ".left-side .sidebar > .sidebar-menu { " + (colors.sidebar_top_border ? "border-top: 1px solid " + colors.sidebar_top_border + "; " : "") + (colors.sidebar_bottom_border ? "border-bottom: 1px solid " + colors.sidebar_bottom_border : "") + " }";
		}

		css += ".left-side .sidebar > .sidebar-menu > li > a { background: " + colors.menu_item_bg + "; color: " + colors.menu_item_txt + (colors.menu_item_top_border ? "; border-top:1px solid " + colors.menu_item_top_border : "") + (colors.menu_item_bottom_border ? "; border-bottom: 1px solid " + colors.menu_item_bottom_border : "") + " }";

		if (colors.menu_item_bg_gradient) {
		    css += ".left-side .sidebar > .sidebar-menu > li > a { " + getCssGradientStyle(colors.menu_item_bg, colors.menu_item_bg_gradient) + " }";
		}

		css += ".left-side .sidebar > .sidebar-menu > li.active > a { background: " + colors.menu_item_bg_active + "; color: " + colors.menu_item_txt_active + (colors.menu_item_border_active ? "; border-left: " + colors.menu_item_border_size + "px solid " + colors.menu_item_border_active : "") + " }";

		if (colors.menu_item_border_hover || colors.menu_item_border_active) {
		    css += ".left-side .sidebar > .sidebar-menu > li > a { border-left: " + colors.menu_item_border_size + "px solid transparent }";
		}

		if (colors.menu_item_bg_active_gradient) {
		    css += ".left-side .sidebar > .sidebar-menu > li.active > a { " + getCssGradientStyle(colors.menu_item_bg_active, colors.menu_item_bg_active_gradient) + " }";
		}

		css += ".left-side .sidebar > .sidebar-menu > li > a:hover { background: " + colors.menu_item_bg_hover + "; color: " + colors.menu_item_txt_hover + (colors.menu_item_border_hover ? "; border-left: " + colors.menu_item_border_size + "px solid " + colors.menu_item_border_hover : "") + " }";

		if (colors.menu_item_bg_hover_gradient) {
		    css += ".left-side .sidebar > .sidebar-menu > li > a:hover { " + getCssGradientStyle(colors.menu_item_bg_hover, colors.menu_item_bg_hover_gradient) + " }";
		}

		css += ".sidebar .sidebar-menu .treeview-menu > li > a { background: " + colors.submenu_item_bg + "; color: " + colors.submenu_item_txt + (colors.submenu_item_top_border ? "; border-top:1px solid " + colors.submenu_item_top_border : "") + (colors.submenu_item_bottom_border ? "; border-bottom: 1px solid " + colors.submenu_item_bottom_border : "") + " }";

		if (colors.submenu_item_bg_gradient) {
		    css += ".sidebar .sidebar-menu .treeview-menu > li > a { " + getCssGradientStyle(colors.submenu_item_bg, colors.submenu_item_bg_gradient) + " }";
		}

		css += ".sidebar .sidebar-menu .treeview-menu > li > a:hover { background: " + colors.submenu_item_bg_hover + "; color: " + colors.submenu_item_txt_hover + " }";

		if (colors.submenu_item_bg_hover_gradient) {
		    css += ".sidebar .sidebar-menu .treeview-menu > li > a:hover { " + getCssGradientStyle(colors.submenu_item_bg_hover, colors.submenu_item_bg_hover_gradient) + " }";
		}

		break;
	    }
	}

	var $style = $("#user_" + type + "_style");

	if ($style[0].styleSheet) {
	    $style[0].styleSheet.cssText = css;
	}
        else {
	    $style.text(css);
	}
    }

    $("ul.color_scheme_editor").on("click", "li a", function(e) {
	e.preventDefault();

	var color = $(this).data("color");

	var scheme = $(this).closest("ul").data("scheme");

	var $span = $(this).closest(".btn-group.colors").find("span.text");

	if (!color) {
	    colorTitle = "Default";
	}
        else {
	    var colorTitle = color.replace(/-/g, " ");
	    colorTitle = colorTitle.replace(/\w\S*/g, function(txt) {
		return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
	    });
	}

	$span.html(colorTitle);

	if (color) {
	    BRS.updateSettings(scheme + "_color", color);
	    BRS.updateStyle(scheme, color);
	}
        else {
	    BRS.updateSettings(scheme + "_color");
	    BRS.updateStyle(scheme);
	}
    });

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
    }

    BRS.applySettings = function(key) {
	if (!key || key == "language") {
	    if ($.i18n.lng() != BRS.settings["language"]) {
		$.i18n.setLng(BRS.settings["language"], null, function() {
		    $("[data-i18n]").i18n();
		});
		if (key && window.localstorage) {
		    window.localStorage.setItem('i18next_lng', BRS.settings["language"]);
		}
		if (BRS.inApp) {
		    parent.postMessage({
			"type": "language",
			"version": BRS.settings["language"]
		    }, "*");
		}
	    }
	}

	if (!key || key == "submit_on_enter") {
	    if (BRS.settings["submit_on_enter"]) {
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
	    if (BRS.settings["animate_forging"]) {
		$("#forging_indicator").addClass("animated");
	    }
            else {
		$("#forging_indicator").removeClass("animated");
	    }
	}

	if (!key || key == "news") {
	    if (BRS.settings["news"] == 0) {
		$("#news_link").hide();
	    }
            else if (BRS.settings["news"] == 1) {
		$("#news_link").show();
	    }
	}

	if (!BRS.inApp && !BRS.downloadingBlockchain) {
	    if (!key || key == "console_log") {
		if (BRS.settings["console_log"] == 0) {
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
	    if (BRS.settings["remember_passphrase"]) {
		BRS.setCookie("remember_passphrase", 1, 1000);
	    }
            else {
		BRS.deleteCookie("remember_passphrase");
	    }
	}
    }

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
    }

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
