/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.newsRefresh = 0;

    BRS.pages.news = function() {
	if (BRS.settings.news != 1) {
	    $("#rss_news_container").hide();
	    $("#rss_news_disabled").show();
	    return;
	}
        else {
	    $("#rss_news_container").show();
	    $("#rss_news_disabled").hide();
	}

	var currentTime = new Date().getTime();

	if (currentTime - BRS.newsRefresh > 60 * 60 * 10) { //10 minutes before refreshing..
	    BRS.newsRefresh = currentTime;

	    $(".rss_news").empty().addClass("data-loading").html("<img src='img/loading_indicator.gif' width='32' height='32' />");

	    var settings = {
		"limit": 5,
		"layoutTemplate": "<div class='list-group'>{entries}</div>",
		"entryTemplate": "<a href='{url}' target='_blank' class='list-group-item'><h4 class='list-group-item-heading'>{title}</h4><p class='list-group-item-text'>{shortBodyPlain}</p></a>"
	    };

	    var settingsReddit = {
		"limit": 7,
		"filterLimit": 5,
		"layoutTemplate": "<div class='list-group'>{entries}</div>",
		"entryTemplate": "<a href='{url}' target='_blank' class='list-group-item'><h4 class='list-group-item-heading'>{title}</h4><p class='list-group-item-text'>{shortBodyReddit}</p></a>",
		"tokens": {
		    "shortBodyReddit": function(entry, tokens) {
			return entry.contentSnippet.replace("&lt;!-- SC_OFF --&gt;", "").replace("&lt;!-- SC_ON --&gt;", "").replace("[link]", "").replace("[comment]", "");
		    }
		},
		"filter": function(entry, tokens) {
		    return tokens.title.indexOf("Donations toward") == -1 && tokens.title.indexOf("BURST tipping bot has arrived") == -1;
		}
	    };

	    $("#burstforum_news").rss("https://forums.getburst.net/c/burst-community-announcements", settings, BRS.newsLoaded);
	    $("#burstforum2_news").rss("https://burstforum.net/category/1/announcements", settings, BRS.newsLoaded);		
	    $("#reddit_news").rss("http://www.reddit.com/r/burstcoin/.rss", settingsReddit, BRS.newsLoaded);
	    $("#burstcoinist_news").rss("https://www.burstcoin.ist/category/articles/", settings, BRS.newsLoaded);
	}

	BRS.pageLoaded();
    };

    BRS.newsLoaded = function($el) {
	$el.removeClass("data-loading").find("img").remove();
    };

    $("#rss_news_enable").on("click", function() {
	BRS.updateSettings("news", 1);
	BRS.loadPage("news");
    });

    return BRS;
}(BRS || {}, jQuery));
