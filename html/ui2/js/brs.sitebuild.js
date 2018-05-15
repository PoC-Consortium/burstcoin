var BRS;
BRS = (function (BRS, $, undefined) {

    BRS.loadLockscreenHTML = function (path, options) {
        jQuery.ajaxSetup({async: false});
        $.get(path, '', function (data) {
            $("body").prepend(data);
        });
        jQuery.ajaxSetup({async: true});
    };

    BRS.loadHeaderHTML = function (path, options) {
        jQuery.ajaxSetup({async: false});
        $.get(path, '', function (data) {
            $("body").prepend(data);
        });
        jQuery.ajaxSetup({async: true});
    };

    BRS.loadSidebarHTML = function (path, options) {
        jQuery.ajaxSetup({async: false});
        $.get(path, '', function (data) {
            $("#sidebar").append(data);
        });
        jQuery.ajaxSetup({async: true});
    };

    BRS.loadSidebarContextHTML = function (path, options) {
        jQuery.ajaxSetup({async: false});
        $.get(path, '', function (data) {
            $("body").append(data);
        });
        jQuery.ajaxSetup({async: true});
    };

    BRS.loadPageHTML = function (path, options) {
        jQuery.ajaxSetup({async: false});
        $.get(path, '', function (data) {
            $("#content").append(data);
        });
        jQuery.ajaxSetup({async: true});
    };

    BRS.loadModalHTML = function (path, options) {
        jQuery.ajaxSetup({async: false});
        $.get(path, '', function (data) {
            $("body").append(data);
        });
        jQuery.ajaxSetup({async: true});
    };

    return BRS;
}(BRS || {}, jQuery));