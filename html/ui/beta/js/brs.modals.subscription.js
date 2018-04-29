/**
 * @depends {brs.js}
 * @depends {brs.modals.js}
 */
var BRS = (function(BRS, $, undefined) {
    $("#subscription_table").on("click", "a[data-subscription]", function(e) {
	e.preventDefault();

	var subscriptionId = $(this).data("subscription");

	BRS.showSubscriptionCancelModal(subscriptionId);
    });
    
    BRS.showSubscriptionCancelModal = function(subscription) {
	if (BRS.fetchingModalData) {
	    return;
	}
	
	BRS.fetchingModalData = true;
	
	if(typeof subscription != "object") {
	    BRS.sendRequest("getSubscription", {
		"subscription": subscription
	    }, function(response, input) {
		BRS.processSubscriptionCancelModalData(response);
	    });
	}
	else {
	    BRS.processSubscriptionCancelModalData(subscription);
	}
    };
    
    BRS.processSubscriptionCancelModalData = function(subscription) {
	
	$("#subscription_cancel_subscription").val(subscription.id);
	$("#subscription_cancel_sender").html(subscription.senderRS);
	$("#subscription_cancel_recipient").html(subscription.recipientRS);
	$("#subscription_cancel_amount").html(BRS.formatAmount(subscription.amountNQT));
	$("#subscription_cancel_frequency").html(subscription.frequency);
	$("#subscription_cancel_time_next").html(BRS.formatTimestamp(subscription.timeNext));
	
	$("#subscription_cancel_modal").modal("show");
	BRS.fetchingModalData = false;
    };

    return BRS;
}(BRS || {}, jQuery));
