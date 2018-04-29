/**
 * @depends {brs.js}
 * @depends {brs.modals.js}
 */
var BRS = (function(BRS, $, undefined) {
    $("#escrow_table").on("click", "a[data-escrow]", function(e) {
	e.preventDefault();

	var escrowId = $(this).data("escrow");

	BRS.showEscrowDecisionModal(escrowId);
    });
    
    BRS.showEscrowDecisionModal = function(escrow) {
	if (BRS.fetchingModalData) {
	    return;
	}
	
	BRS.fetchingModalData = true;
	
	if(typeof escrow != "object") {
	    BRS.sendRequest("getEscrowTransaction", {
		"escrow": escrow
	    }, function(response, input) {
		BRS.processEscrowDecisionModalData(response);
	    });
	}
	else {
	    BRS.processEscrowDecisionModalData(escrow);
	}
    };
    
    BRS.processEscrowDecisionModalData = function(escrow) {
	
	$("#escrow_decision_escrow").val(escrow.id);
	var decisions = "";
	for(var i = 0; i < escrow.signers.length; i++) {
	    decisions += escrow.signers[i].idRS + " " + escrow.signers[i].decision + "<br />";
	}
	$("#escrow_decision_decisions").html(decisions);
	$("#escrow_decision_required").html(escrow.requiredSigners + " signers required");
	$("#escrow_decision_deadline").html("Defaults to " + escrow.deadlineAction + " at " + BRS.formatTimestamp(escrow.deadline));
	
	$("#escrow_decision_modal").modal("show");
	BRS.fetchingModalData = false;
    };

    return BRS;
}(BRS || {}, jQuery));
