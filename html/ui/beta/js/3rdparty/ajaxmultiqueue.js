/**
 * @depends {jquery.min.js}
 */

/*
 * jQuery.ajaxMultiQueue - A queue for multiple concurrent ajax requests
 * (c) 2013 Amir Grozki
 * Dual licensed under the MIT and GPL licenses.
 *
 * Based on jQuery.ajaxQueue
 * (c) 2011 Corey Frang
 *
 * Requires jQuery 1.5+
 */
(function($) {
    $.ajaxMultiQueue = function(n) {
	return new MultiQueue(~~n);
    };

    function MultiQueue(number) {
	var queues, i,
	    current = 0;

	if (!queues) {
	    queues = new Array(number);

	    for (i = 0; i < number; i++) {
		queues[i] = $({});
	    }
	}

	function queue(ajaxOpts) {
	    var jqXHR,
		dfd = $.Deferred(),
		promise = dfd.promise();

	    queues[current].queue(doRequest);
	    current = (current + 1) % number;

	    function doRequest(next) {
		if (ajaxOpts.currentPage && ajaxOpts.currentPage != BRS.currentPage) {
		    next();
		}
                else if (ajaxOpts.currentSubPage && ajaxOpts.currentSubPage != BRS.currentSubPage) {
		    next();
		}
                else {
		    jqXHR = $.ajax(ajaxOpts);

		    jqXHR.done(dfd.resolve)
			.fail(dfd.reject)
			.then(next, next);
		}
	    }

	    return promise;
	};

	return {
	    queue: queue
	};
    }

})(jQuery);
