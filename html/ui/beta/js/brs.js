/**
 * @depends {3rdparty/jquery.min.js}
 * @depends {3rdparty/bootstrap.min.js}
 * @depends {3rdparty/big.js}
 * @depends {3rdparty/jsbn.js}
 * @depends {3rdparty/jsbn2.js}
 * @depends {3rdparty/pako.min.js}
 * @depends {3rdparty/webdb.js}
 * @depends {3rdparty/ajaxmultiqueue.js}
 * @depends {3rdparty/notify.min.js}
 * @depends {3rdparty/clipboard.js}
 * @depends {crypto/curve25519.js}
 * @depends {crypto/curve25519_.js}
 * @depends {crypto/passphrasegenerator.js}
 * @depends {crypto/sha256worker.js}
 * @depends {crypto/3rdparty/aes.js}
 * @depends {crypto/3rdparty/sha256.js}
 * @depends {crypto/3rdparty/jssha256.js}
 * @depends {crypto/3rdparty/seedrandom.js}
 * @depends {util/converters.js}
 * @depends {util/extensions.js}
 * @depends {util/nxtaddress.js}
 */
var BRS = (function(BRS, $, undefined) {
    "use strict";

    BRS.server = "";
    BRS.state = {};
    BRS.blocks = [];
    BRS.genesis = "0";
    BRS.genesisRS = "BURST-2222-2222-2222-22222";

    BRS.account = "";
    BRS.accountRS = ""
    BRS.publicKey = "";
    BRS.accountInfo = {};

    BRS.database = null;
    BRS.databaseSupport = false;

    BRS.settings = {};
    BRS.contacts = {};

    BRS.isTestNet = false;
    BRS.isLocalHost = false;
    BRS.isForging = false;
    BRS.isLeased = false;

    BRS.lastBlockHeight = 0;
    BRS.downloadingBlockchain = false;

    BRS.rememberPassword = false;
    BRS.selectedContext = null;

    BRS.currentPage = "dashboard";
    BRS.currentSubPage = "";
    BRS.pageNumber = 1;
    BRS.itemsPerPage = 50;

    BRS.pages = {};
    BRS.incoming = {};

    BRS.hasLocalStorage = true;
    BRS.inApp = false;
    BRS.appVersion = "";
    BRS.appPlatform = "";
    BRS.assetTableKeys = [];

    var stateInterval;
    var stateIntervalSeconds = 30;
    var isScanning = false;

    BRS.init = function() {
	if ( window.location.port === null || window.location.port.length === 0 || window.location.port != "6876") {
	    $(".testnet_only").hide();
	}
	else {
	    BRS.isTestNet = true;
	    $(".testnet_only, #testnet_login, #testnet_warning").show();
	}

	if (!BRS.server) {
	    var hostName = window.location.hostname.toLowerCase();
	    BRS.isLocalHost = hostName == "localhost" || hostName == "127.0.0.1" || BRS.isPrivateIP(hostName);
	}

	if (!BRS.isLocalHost) {
	    $(".remote_warning").show();
	}

	try {
	    window.localStorage;
	} catch (err) {
	    BRS.hasLocalStorage = false;
	}

	BRS.createDatabase(function() {
	    BRS.getSettings();
	});

	BRS.getState(function() {
	    setTimeout(function() {
		BRS.checkAliasVersions();
	    }, 5000);
	});

	BRS.showLockscreen();

	if ( BRS.getCookie("remember_passphrase") ) {
	    $("#remember_password").prop("checked", true);
	    if ( BRS.hasLocalStorage ) {
		$("#remember_password_container").show();
		var passphrase = localStorage.getItem("burst.passphrase");
		if ( passphrase != null && passphrase.length) {
		    $("#login_password").val(passphrase);
		}
	    }
	    else {
		$("#remember_password_container").hide();
	    }
	}

	if (window.parent) {
	    var match = window.location.href.match(/\?app=?(win|mac|lin)?\-?([\d\.]+)?/i);

	    if (match) {
		BRS.inApp = true;
		if (match[1]) {
		    BRS.appPlatform = match[1];
		}
		if (match[2]) {
		    BRS.appVersion = match[2];
		}

		if (!BRS.appPlatform || BRS.appPlatform == "mac") {
		    var macVersion = navigator.userAgent.match(/OS X 10_([0-9]+)/i);
		    if (macVersion && macVersion[1]) {
			macVersion = parseInt(macVersion[1]);

			if (macVersion < 9) {
			    $(".modal").removeClass("fade");
			}
		    }
		}

		$("#show_console").hide();

		parent.postMessage("loaded", "*");

		window.addEventListener("message", receiveMessage, false);
	    }
	}

	BRS.setStateInterval(30);

	if (!BRS.isTestNet) {
	    setInterval(BRS.checkAliasVersions, 1000 * 60 * 60);
	}

	BRS.allowLoginViaEnter();

	BRS.automaticallyCheckRecipient();

	$(".show_popover").popover({
	    "trigger": "hover"
	});

	$("#dashboard_transactions_table, #transactions_table").on("mouseenter", "td.confirmations", function() {
	    $(this).popover("show");
	}).on("mouseleave", "td.confirmations", function() {
	    $(this).popover("destroy");
	    $(".popover").remove();
	});

	_fix();

	$(window).on("resize", function() {
	    _fix();

	    if (BRS.currentPage == "asset_exchange") {
		BRS.positionAssetSidebar();
	    }
	});

	$("[data-toggle='tooltip']").tooltip();

	$(".sidebar .treeview").tree();

	$("#dgs_search_account_top, #dgs_search_account_center").mask("BURST-****-****-****-*****", {
	    "unmask": false
	});

	/*
	  $("#asset_exchange_search input[name=q]").addClear({
	  right: 0,
	  top: 4,
	  onClear: function(input) {
	  $("#asset_exchange_search").trigger("submit");
	  }
	  });

	  $("#id_search input[name=q], #alias_search input[name=q]").addClear({
	  right: 0,
	  top: 4
	  });*/
    }

    function _fix() {
	var height = $(window).height() - $("body > .header").height();
	//$(".wrapper").css("min-height", height + "px");
	var content = $(".wrapper").height();

	$(".content.content-stretch:visible").width($(".page:visible").width());

	if (content > height) {
	    $(".left-side, html, body").css("min-height", content + "px");
	}
	else {
	    $(".left-side, html, body").css("min-height", height + "px");
	}
    }

    BRS.setStateInterval = function(seconds) {
	if (seconds == stateIntervalSeconds && stateInterval) {
	    return;
	}

	if (stateInterval) {
	    clearInterval(stateInterval);
	}

	stateIntervalSeconds = seconds;

	stateInterval = setInterval(function() {
	    BRS.getState();
	}, 1000 * seconds);
    }

    BRS.getState = function(callback) {
	BRS.sendRequest("getBlockchainStatus", function(response) {
	    if (response.errorCode) {
		//todo
	    }
	    else {
		var firstTime = !("lastBlock" in BRS.state);
		var previousLastBlock = (firstTime ? "0" : BRS.state.lastBlock);

		BRS.state = response;

		if (firstTime) {
		    $("#brs_version").html(BRS.state.version).removeClass("loading_dots");
		    BRS.getBlock(BRS.state.lastBlock, BRS.handleInitialBlocks);
		}
		else if (BRS.state.isScanning) {
		    //do nothing but reset BRS.state so that when isScanning is done, everything is reset.
		    isScanning = true;
		}
		else if (isScanning) {
		    //rescan is done, now we must reset everything...
		    isScanning = false;
		    BRS.blocks = [];
		    BRS.tempBlocks = [];
		    BRS.getBlock(BRS.state.lastBlock, BRS.handleInitialBlocks);
		    if (BRS.account) {
			BRS.getInitialTransactions();
			BRS.getAccountInfo();
		    }
		}
		else if (previousLastBlock != BRS.state.lastBlock) {
		    BRS.tempBlocks = [];
		    if (BRS.account) {
			BRS.getAccountInfo();
		    }
		    BRS.getBlock(BRS.state.lastBlock, BRS.handleNewBlocks);
		    if (BRS.account) {
			BRS.getNewTransactions();
		    }
		}
		else {
		    if (BRS.account) {
			BRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
			    BRS.handleIncomingTransactions(unconfirmedTransactions, false);
			});
		    }
		    //only done so that download progress meter updates correctly based on lastFeederHeight
		    if (BRS.downloadingBlockchain) {
			BRS.updateBlockchainDownloadProgress();
		    }
		}

		if (callback) {
		    callback();
		}
	    }
	});
    }

    $("#logo, .sidebar-menu a").click(function(e, data) {
	if ($(this).hasClass("ignore")) {
	    $(this).removeClass("ignore");
	    return;
	}

	e.preventDefault();

	if ($(this).data("toggle") == "modal") {
	    return;
	}

	var page = $(this).data("page");

	if (page == BRS.currentPage) {
	    if (data && data.callback) {
		data.callback();
	    }
	    return;
	}

	$(".page").hide();

	$(document.documentElement).scrollTop(0);

	$("#" + page + "_page").show();

	$(".content-header h1").find(".loading_dots").remove();

	var changeActive = !($(this).closest("ul").hasClass("treeview-menu"));

	if (changeActive) {
	    var currentActive = $("ul.sidebar-menu > li.active");

	    if (currentActive.hasClass("treeview")) {
		currentActive.children("a").first().addClass("ignore").click();
	    }
	    else {
		currentActive.removeClass("active");
	    }

	    if ($(this).attr("id") && $(this).attr("id") == "logo") {
		$("#dashboard_link").addClass("active");
	    }
	    else {
		$(this).parent().addClass("active");
	    }
	}

	if (BRS.currentPage != "messages") {
	    $("#inline_message_password").val("");
	}

	//BRS.previousPage = BRS.currentPage;
	BRS.currentPage = page;
	BRS.currentSubPage = "";
	BRS.pageNumber = 1;
	BRS.showPageNumbers = false;

	if (BRS.pages[page]) {
	    BRS.pageLoading();

	    if (data && data.callback) {
		BRS.pages[page](data.callback);
	    }
	    else if (data) {
		BRS.pages[page](data);
	    }
	    else {
		BRS.pages[page]();
	    }
	}
    });

    $("button.goto-page, a.goto-page").click(function(event) {
	event.preventDefault();

	BRS.goToPage($(this).data("page"));
    });

    BRS.loadPage = function(page, callback) {
	BRS.pageLoading();
	BRS.pages[page](callback);
    }

    BRS.goToPage = function(page, callback) {
	var $link = $("ul.sidebar-menu a[data-page=" + page + "]");

	if ($link.length > 1) {
	    if ($link.last().is(":visible")) {
		$link = $link.last();
	    }
	    else {
		$link = $link.first();
	    }
	}

	if ($link.length == 1) {
	    if (callback) {
		$link.trigger("click", [{
		    "callback": callback
		}]);
	    }
	    else {
		$link.trigger("click");
	    }
	}
	else {
	    BRS.currentPage = page;
	    BRS.currentSubPage = "";
	    BRS.pageNumber = 1;
	    BRS.showPageNumbers = false;

	    $("ul.sidebar-menu a.active").removeClass("active");
	    $(".page").hide();
	    $("#" + page + "_page").show();
	    if (BRS.pages[page]) {
		BRS.pageLoading();
		BRS.pages[page](callback);
	    }
	}
    }

    BRS.pageLoading = function() {
	BRS.hasMorePages = false;

	var $pageHeader = $("#" + BRS.currentPage + "_page .content-header h1");
	$pageHeader.find(".loading_dots").remove();
	$pageHeader.append("<span class='loading_dots'><span>.</span><span>.</span><span>.</span></span>");
    }

    BRS.pageLoaded = function(callback) {
	var $currentPage = $("#" + BRS.currentPage + "_page");

	$currentPage.find(".content-header h1 .loading_dots").remove();

	if ($currentPage.hasClass("paginated")) {
	    BRS.addPagination();
	}

	if (callback) {
	    callback();
	}
    }

    BRS.addPagination = function(section) {
	var output = "";

	if (BRS.pageNumber == 2) {
	    output += "<a href='#' data-page='1'>&laquo; " + $.t("previous_page") + "</a>";
	}
	else if (BRS.pageNumber > 2) {
	    //output += "<a href='#' data-page='1'>&laquo; First Page</a>";
	    output += " <a href='#' data-page='" + (BRS.pageNumber - 1) + "'>&laquo; " + $.t("previous_page") + "</a>";
	}
	if (BRS.hasMorePages) {
	    if (BRS.pageNumber > 1) {
		output += "&nbsp;&nbsp;&nbsp;";
	    }
	    output += " <a href='#' data-page='" + (BRS.pageNumber + 1) + "'>" + $.t("next_page") + " &raquo;</a>";
	}

	var $paginationContainer = $("#" + BRS.currentPage + "_page .data-pagination");

	if ($paginationContainer.length) {
	    $paginationContainer.html(output);
	}
    }

    $(".data-pagination").on("click", "a", function(e) {
	e.preventDefault();

	BRS.goToPageNumber($(this).data("page"));
    });

    BRS.goToPageNumber = function(pageNumber) {
	/*if (!pageLoaded) {
	  return;
	  }*/
	BRS.pageNumber = pageNumber;

	BRS.pageLoading();

	BRS.pages[BRS.currentPage]();
    }

    BRS.createDatabase = function(callback) {
	var schema = {
	    contacts: {
		id: {
		    "primary": true,
		    "autoincrement": true,
		    "type": "NUMBER"
		},
		name: "VARCHAR(100) COLLATE NOCASE",
		email: "VARCHAR(200)",
		account: "VARCHAR(25)",
		accountRS: "VARCHAR(25)",
		description: "TEXT"
	    },
	    assets: {
		account: "VARCHAR(25)",
		accountRS: "VARCHAR(25)",
		asset: {
		    "primary": true,
		    "type": "VARCHAR(25)"
		},
		description: "TEXT",
		name: "VARCHAR(10)",
		decimals: "NUMBER",
		quantityQNT: "VARCHAR(15)",
		groupName: "VARCHAR(30) COLLATE NOCASE"
	    },
	    data: {
		id: {
		    "primary": true,
		    "type": "VARCHAR(40)"
		},
		contents: "TEXT"
	    }
	};

	BRS.assetTableKeys = ["account", "accountRS", "asset", "description", "name", "position", "decimals", "quantityQNT", "groupName"];

	try {
	    BRS.database = new WebDB("BRS_USER_DB", schema, 2, 4, function(error, db) {
		if (!error) {
		    BRS.databaseSupport = true;

		    BRS.loadContacts();

		    BRS.database.select("data", [{
			"id": "asset_exchange_version"
		    }], function(error, result) {
			if (!result || !result.length) {
			    BRS.database.delete("assets", [], function(error, affected) {
				if (!error) {
				    BRS.database.insert("data", {
					"id": "asset_exchange_version",
					"contents": 2
				    });
				}
			    });
			}
		    });

		    BRS.database.select("data", [{
			"id": "closed_groups"
		    }], function(error, result) {
			if (result && result.length) {
			    BRS.closedGroups = result[0].contents.split("#");
			}
			else {
			    BRS.database.insert("data", {
				id: "closed_groups",
				contents: ""
			    });
			}
		    });
		    if (callback) {
			callback();
		    }
		}
		else if (callback) {
		    callback();
		}
	    });
	} catch (err) {
	    BRS.database = null;
	    BRS.databaseSupport = false;
	    if (callback) {
		callback();
	    }
	}
    }

    BRS.getAccountInfo = function(firstRun, callback) {
	BRS.sendRequest("getAccount", {
	    "account": BRS.account
	}, function(response) {
	    var previousAccountInfo = BRS.accountInfo;

	    BRS.accountInfo = response;

	    if (response.errorCode) {
		$("#account_balance, #account_forged_balance").html("0");
		$("#account_nr_assets").html("0");

		if (BRS.accountInfo.errorCode == 5) {
		    if (BRS.downloadingBlockchain) {
			if (BRS.newlyCreatedAccount) {
			    $("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html($.t("status_new_account", {
				"account_id": String(BRS.accountRS).escapeHTML(),
				"public_key": String(BRS.publicKey).escapeHTML()
			    }) + "<br /><br />" + $.t("status_blockchain_downloading")).show();
			}
			else {
			    $("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html($.t("status_blockchain_downloading")).show();
			}
		    }
		    else if (BRS.state && BRS.state.isScanning) {
			$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html($.t("status_blockchain_rescanning")).show();
		    }
		    else {
			$("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html($.t("status_new_account", {
			    "account_id": String(BRS.accountRS).escapeHTML(),
			    "public_key": String(BRS.publicKey).escapeHTML()
			})).show();
		    }
		}
		else {
		    $("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html(BRS.accountInfo.errorDescription ? BRS.accountInfo.errorDescription.escapeHTML() : $.t("error_unknown")).show();
		}
	    }
	    else {
		if (BRS.accountRS && BRS.accountInfo.accountRS != BRS.accountRS) {
		    $.notify("Generated Reed Solomon address different from the one in the blockchain!", {
			"type": "danger"
		    });
		    BRS.accountRS = BRS.accountInfo.accountRS;
		}

		if (BRS.downloadingBlockchain) {
		    $("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html($.t("status_blockchain_downloading")).show();
		}
		else if (BRS.state && BRS.state.isScanning) {
		    $("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html($.t("status_blockchain_rescanning")).show();
		}
		else if (!BRS.accountInfo.publicKey) {
		    $("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html($.t("no_public_key_warning") + " " + $.t("public_key_actions")).show();
		}
		else {
		    $("#dashboard_message").hide();
		}

		//only show if happened within last week
		var showAssetDifference = (!BRS.downloadingBlockchain || (BRS.blocks && BRS.blocks[0] && BRS.state && BRS.state.time - BRS.blocks[0].timestamp < 60 * 60 * 24 * 7));

		if (BRS.databaseSupport) {
		    BRS.database.select("data", [{
			"id": "asset_balances_" + BRS.account
		    }], function(error, asset_balance) {
			if (asset_balance && asset_balance.length) {
			    var previous_balances = asset_balance[0].contents;

			    if (!BRS.accountInfo.assetBalances) {
				BRS.accountInfo.assetBalances = [];
			    }

			    var current_balances = JSON.stringify(BRS.accountInfo.assetBalances);

			    if (previous_balances != current_balances) {
				if (previous_balances != "undefined" && typeof previous_balances != "undefined") {
				    previous_balances = JSON.parse(previous_balances);
				}
				else {
				    previous_balances = [];
				}
				BRS.database.update("data", {
				    contents: current_balances
				}, [{
				    id: "asset_balances_" + BRS.account
				}]);
				if (showAssetDifference) {
				    BRS.checkAssetDifferences(BRS.accountInfo.assetBalances, previous_balances);
				}
			    }
			}
			else {
			    BRS.database.insert("data", {
				id: "asset_balances_" + BRS.account,
				contents: JSON.stringify(BRS.accountInfo.assetBalances)
			    });
			}
		    });
		}
		else if (showAssetDifference && previousAccountInfo && previousAccountInfo.assetBalances) {
		    var previousBalances = JSON.stringify(previousAccountInfo.assetBalances);
		    var currentBalances = JSON.stringify(BRS.accountInfo.assetBalances);

		    if (previousBalances != currentBalances) {
			BRS.checkAssetDifferences(BRS.accountInfo.assetBalances, previousAccountInfo.assetBalances);
		    }
		}

		$("#account_balance").html(BRS.formatStyledAmount(response.unconfirmedBalanceNQT));
		$("#account_forged_balance").html(BRS.formatStyledAmount(response.forgedBalanceNQT));

		var nr_assets = 0;

		if (response.assetBalances) {
		    for (var i = 0; i < response.assetBalances.length; i++) {
			if (response.assetBalances[i].balanceQNT != "0") {
			    nr_assets++;
			}
		    }
		}

		$("#account_nr_assets").html(nr_assets);

		if (BRS.lastBlockHeight) {
		    var isLeased = BRS.lastBlockHeight >= BRS.accountInfo.currentLeasingHeightFrom;
		    if (isLeased != BRS.IsLeased) {
			var leasingChange = true;
			BRS.isLeased = isLeased;
		    }
		}
		else {
		    var leasingChange = false;
		}

		if (leasingChange ||
		    (response.currentLeasingHeightFrom != previousAccountInfo.currentLeasingHeightFrom) ||
		    (response.lessors && !previousAccountInfo.lessors) ||
		    (!response.lessors && previousAccountInfo.lessors) ||
		    (response.lessors && previousAccountInfo.lessors && response.lessors.sort().toString() != previousAccountInfo.lessors.sort().toString())) {
		    BRS.updateAccountLeasingStatus();
		}

		if (response.name) {
		    $("#account_name").html(response.name.escapeHTML()).removeAttr("data-i18n");
		}
	    }

	    if (firstRun) {
		$("#account_balance, #account_forged_balance, #account_nr_assets").removeClass("loading_dots");
	    }

	    if (callback) {
		callback();
	    }
	});
    }

    BRS.updateAccountLeasingStatus = function() {
	var accountLeasingLabel = "";
	var accountLeasingStatus = "";

	if (BRS.lastBlockHeight >= BRS.accountInfo.currentLeasingHeightFrom) {
	    accountLeasingLabel = $.t("leased_out");
	    accountLeasingStatus = $.t("balance_is_leased_out", {
		"start": String(BRS.accountInfo.currentLeasingHeightFrom).escapeHTML(),
		"end": String(BRS.accountInfo.currentLeasingHeightTo).escapeHTML(),
		"account": String(BRS.accountInfo.currentLessee).escapeHTML()
	    });
	    $("#lease_balance_message").html($.t("balance_leased_out_help"));
	}
	else if (BRS.lastBlockHeight < BRS.accountInfo.currentLeasingHeightTo) {
	    accountLeasingLabel = $.t("leased_soon");
	    accountLeasingStatus = $.t("balance_will_be_leased_out", {
		"start": String(BRS.accountInfo.currentLeasingHeightFrom).escapeHTML(),
		"end": String(BRS.accountInfo.currentLeasingHeightTo).escapeHTML(),
		"account": String(BRS.accountInfo.currentLessee).escapeHTML()
	    });
	    $("#lease_balance_message").html($.t("balance_leased_out_help"));
	}
	else {
	    accountLeasingStatus = $.t("balance_not_leased_out");
	    $("#lease_balance_message").html($.t("balance_leasing_help"));
	}

	if (BRS.accountInfo.effectiveBalanceBURST == 0) {
	    $("#forging_indicator").removeClass("forging");
	    $("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
	    $("#forging_indicator").show();
	    BRS.isForging = false;
	}

	//no reed solomon available? do it myself? todo
	if (BRS.accountInfo.lessors) {
	    if (accountLeasingLabel) {
		accountLeasingLabel += ", ";
		accountLeasingStatus += "<br /><br />";
	    }

	    accountLeasingLabel += $.t("x_lessor", {
		"count": BRS.accountInfo.lessors.length
	    });
	    accountLeasingStatus += $.t("x_lessor_lease", {
		"count": BRS.accountInfo.lessors.length
	    });

	    var rows = "";

	    for (var i = 0; i < BRS.accountInfo.lessors.length; i++) {
		var lessor = BRS.convertNumericToRSAccountFormat(BRS.accountInfo.lessors[i]);

		rows += "<tr><td><a href='#' data-user='" + String(lessor).escapeHTML() + "'>" + BRS.getAccountTitle(lessor) + "</a></td></tr>";
	    }

	    $("#account_lessor_table tbody").empty().append(rows);
	    $("#account_lessor_container").show();
	}
	else {
	    $("#account_lessor_table tbody").empty();
	    $("#account_lessor_container").hide();
	}

	if (accountLeasingLabel) {
	    $("#account_leasing").html(accountLeasingLabel).show();
	}
	else {
	    $("#account_leasing").hide();
	}

	if (accountLeasingStatus) {
	    $("#account_leasing_status").html(accountLeasingStatus).show();
	}
	else {
	    $("#account_leasing_status").hide();
	}
    }

    BRS.checkAssetDifferences = function(current_balances, previous_balances) {
	var current_balances_ = {};
	var previous_balances_ = {};

	if (previous_balances.length) {
	    for (var k in previous_balances) {
		previous_balances_[previous_balances[k].asset] = previous_balances[k].balanceQNT;
	    }
	}

	if (current_balances.length) {
	    for (var k in current_balances) {
		current_balances_[current_balances[k].asset] = current_balances[k].balanceQNT;
	    }
	}

	var diff = {};

	for (var k in previous_balances_) {
	    if (!(k in current_balances_)) {
		diff[k] = "-" + previous_balances_[k];
	    } else if (previous_balances_[k] !== current_balances_[k]) {
		var change = (new BigInteger(current_balances_[k]).subtract(new BigInteger(previous_balances_[k]))).toString();
		diff[k] = change;
	    }
	}

	for (k in current_balances_) {
	    if (!(k in previous_balances_)) {
		diff[k] = current_balances_[k]; // property is new
	    }
	}

	var nr = Object.keys(diff).length;

	if (nr == 0) {
	    return;
	}
	else if (nr <= 3) {
	    for (k in diff) {
		BRS.sendRequest("getAsset", {
		    "asset": k,
		    "_extra": {
			"asset": k,
			"difference": diff[k]
		    }
		}, function(asset, input) {
		    if (asset.errorCode) {
			return;
		    }
		    asset.difference = input["_extra"].difference;
		    asset.asset = input["_extra"].asset;

		    if (asset.difference.charAt(0) != "-") {
			var quantity = BRS.formatQuantity(asset.difference, asset.decimals)

			if (quantity != "0") {
			    $.notify($.t("you_received_assets", {
				"asset": String(asset.asset).escapeHTML(),
				"name": String(asset.name).escapeHTML(),
				"count": quantity
			    }), {
				"type": "success"
			    });
			}
		    }
		    else {
			asset.difference = asset.difference.substring(1);

			var quantity = BRS.formatQuantity(asset.difference, asset.decimals)

			if (quantity != "0") {
			    $.notify($.t("you_sold_assets", {
				"asset": String(asset.asset).escapeHTML(),
				"name": String(asset.name).escapeHTML(),
				"count": quantity
			    }), {
				"type": "success"
			    });
			}
		    }
		});
	    }
	}
	else {
	    $.notify($.t("multiple_assets_differences"), {
		"type": "success"
	    });
	}
    }

    BRS.checkLocationHash = function(password) {
	if (window.location.hash) {
	    var hash = window.location.hash.replace("#", "").split(":")

	    if (hash.length == 2) {
		if (hash[0] == "message") {
		    var $modal = $("#send_message_modal");
		}
		else if (hash[0] == "send") {
		    var $modal = $("#send_money_modal");
		}
		else if (hash[0] == "asset") {
		    BRS.goToAsset(hash[1]);
		    return;
		}
		else {
		    var $modal = "";
		}

		if ($modal) {
		    var account_id = String($.trim(hash[1]));
		    if (!/^\d+$/.test(account_id) && account_id.indexOf("@") !== 0) {
			account_id = "@" + account_id;
		    }

		    $modal.find("input[name=recipient]").val(account_id.unescapeHTML()).trigger("blur");
		    if (password && typeof password == "string") {
			$modal.find("input[name=secretPhrase]").val(password);
		    }
		    $modal.modal("show");
		}
	    }

	    window.location.hash = "#";
	}
    }

    BRS.updateBlockchainDownloadProgress = function() {
	if (BRS.state.lastBlockchainFeederHeight && BRS.state.numberOfBlocks < BRS.state.lastBlockchainFeederHeight) {
	    var percentage = parseInt(Math.round((BRS.state.numberOfBlocks / BRS.state.lastBlockchainFeederHeight) * 100), 10);
	}
	else {
	    var percentage = 100;
	}

	if (percentage == 100) {
	    $("#downloading_blockchain .progress").hide();
	}
	else {
	    $("#downloading_blockchain .progress").show();
	    $("#downloading_blockchain .progress-bar").css("width", percentage + "%");
	    $("#downloading_blockchain .sr-only").html($.t("percent_complete", {
		"percent": percentage
	    }));
	}
    }

    BRS.checkIfOnAFork = function() {
	if (!BRS.downloadingBlockchain) {
	    var onAFork = true;

	    if (BRS.blocks && BRS.blocks.length >= 10) {
		for (var i = 0; i < 10; i++) {
		    if (BRS.blocks[i].generator != BRS.account) {
			onAFork = false;
			break;
		    }
		}
	    }
	    else {

		onAFork = false;
	    }

	    if (onAFork) {
		$.notify($.t("fork_warning"), {
		    "type": "danger"
		});
	    }
	}
    }

    $("#id_search").on("submit", function(e) {
	e.preventDefault();

	var id = $.trim($("#id_search input[name=q]").val());

	if (/BURST\-/i.test(id)) {
	    BRS.sendRequest("getAccount", {
		"account": id
	    }, function(response, input) {
		if (!response.errorCode) {
		    response.account = input.account;
		    BRS.showAccountModal(response);
		}
		else {
		    $.notify($.t("error_search_no_results"), {
			"type": "danger"
		    });
		}
	    });
	}
	else {
	    if (!/^\d+$/.test(id)) {
		$.notify($.t("error_search_invalid"), {
		    "type": "danger"
		});
		return;
	    }
	    BRS.sendRequest("getTransaction", {
		"transaction": id
	    }, function(response, input) {
		if (!response.errorCode) {
		    response.transaction = input.transaction;
		    BRS.showTransactionModal(response);
		}
		else {
		    BRS.sendRequest("getAccount", {
			"account": id
		    }, function(response, input) {
			if (!response.errorCode) {
			    response.account = input.account;
			    BRS.showAccountModal(response);
			}
			else {
			    BRS.sendRequest("getBlock", {
				"block": id
			    }, function(response, input) {
				if (!response.errorCode) {
				    response.block = input.block;
				    BRS.showBlockModal(response);
				}
				else {
				    $.notify($.t("error_search_no_results"), {
					"type": "danger"
				    });
				}
			    });
			}
		    });
		}
	    });
	}
    });

    return BRS;
}(BRS || {}, jQuery));

$(document).ready(function() {
    BRS.init();
});

function receiveMessage(event) {
    if (event.origin != "file://") {
	return;
    }
    //parent.postMessage("from iframe", "file://");
}
