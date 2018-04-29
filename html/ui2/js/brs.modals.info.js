/**
 * @depends {brs.js}
 * @depends {brs.modals.js}
 */
var BRS = (function(BRS, $, undefined) {
    $("#brs_modal").on("show.bs.modal", function(e) {
	if (BRS.fetchingModalData) {
	    return;
	}

	BRS.fetchingModalData = true;

	BRS.sendRequest("getState", function(state) {
	    for (var key in state) {
		var el = $("#brs_node_state_" + key);
		if (el.length) {
		    if (key.indexOf("number") != -1) {
			el.html(BRS.formatAmount(state[key]));
		    }
                    else if (key.indexOf("Memory") != -1) {
			el.html(BRS.formatVolume(state[key]));
		    }
                    else if (key == "time") {
			el.html(BRS.formatTimestamp(state[key]));
		    }
                    else {
			el.html(String(state[key]).escapeHTML());
		    }
		}
	    }

	    $("#brs_update_explanation").show();
	    $("#brs_modal_state").show();

	    BRS.fetchingModalData = false;
	});
    });

    $("#brs_modal").on("hide.bs.modal", function(e) {
	$("body").off("dragover.brs, drop.brs");

	$("#brs_update_drop_zone, #brs_update_result, #brs_update_hashes, #brs_update_hash_progress").hide();

	$(this).find("ul.nav li.active").removeClass("active");
	$("#brs_modal_state_nav").addClass("active");

	$(".brs_modal_content").hide();
    });

    $("#brs_modal ul.nav li").click(function(e) {
	e.preventDefault();

	var tab = $(this).data("tab");

	$(this).siblings().removeClass("active");
	$(this).addClass("active");

	$(".brs_modal_content").hide();

	var content = $("#brs_modal_" + tab);

	content.show();
    });

    return BRS;
}(BRS || {}, jQuery));
