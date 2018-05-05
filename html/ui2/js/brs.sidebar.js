/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    $(".sidebar_context").on("contextmenu", "a", function(e) {
	e.preventDefault();

	if (!BRS.databaseSupport) {
	    return;
	}

	BRS.closeContextMenu();

	if ($(this).hasClass("no-context")) {
	    return;
	}

	BRS.selectedContext = $(this);

	BRS.selectedContext.addClass("context");

	$(document).on("click.contextmenu", BRS.closeContextMenu);

	var contextMenu = $(this).data("context");

	if (!contextMenu) {
	    contextMenu = $(this).closest(".list-group").attr("id") + "_context";
	}

	var $contextMenu = $("#" + contextMenu);

	if ($contextMenu.length) {
	    var $options = $contextMenu.find("ul.dropdown-menu a");

	    $.each($options, function() {
		var requiredClass = $(this).data("class");

		if (!requiredClass) {
		    $(this).show();
		}
                else if (BRS.selectedContext.hasClass(requiredClass)) {
		    $(this).show();
		}
                else {
		    $(this).hide();
		}
	    });

	    $contextMenu.css({
		display: "block",
		left: e.pageX,
		top: e.pageY
	    });
	}

	return false;
    });

    BRS.closeContextMenu = function(e) {
	if (e && e.which == 3) {
	    return;
	}

	$(".context_menu").hide();

	if (BRS.selectedContext) {
	    BRS.selectedContext.removeClass("context");
	    //BRS.selectedContext = null;
	}

	$(document).off("click.contextmenu");
    }

    return BRS;
}(BRS || {}, jQuery));
