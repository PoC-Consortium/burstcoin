package brs.http;

import static brs.http.common.Parameters.DECISION_PARAMETER;
import static brs.http.common.Parameters.ESCROW_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.Escrow;
import brs.BurstException;
import brs.services.EscrowService;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class EscrowSign extends CreateTransaction {
	
  private final ParameterService parameterService;
  private final EscrowService escrowService;
  private final Blockchain blockchain;
	
  EscrowSign(ParameterService parameterService, Blockchain blockchain, EscrowService escrowService, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, apiTransactionManager, ESCROW_PARAMETER, DECISION_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
    this.escrowService = escrowService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Long escrowId;
    try {
      escrowId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(ESCROW_PARAMETER)));
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified escrow");
      return response;
    }
		
    Escrow escrow = escrowService.getEscrowTransaction(escrowId);
    if(escrow == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 5);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Escrow transaction not found");
      return response;
    }
		
    Escrow.DecisionType decision = Escrow.stringToDecision(req.getParameter(DECISION_PARAMETER));
    if(decision == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 5);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified action");
      return response;
    }
		
    Account sender = parameterService.getSenderAccount(req);
    if(! isValidUser(escrow, sender)) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 5);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified action");
      return response;
    }
		
    if(escrow.getSenderId().equals(sender.getId()) && decision != Escrow.DecisionType.RELEASE) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Sender can only release");
      return response;
    }
		
    if(escrow.getRecipientId().equals(sender.getId()) && decision != Escrow.DecisionType.REFUND) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Recipient can only refund");
      return response;
    }
		
    Attachment.AdvancedPaymentEscrowSign attachment = new Attachment.AdvancedPaymentEscrowSign(escrow.getId(), decision, blockchain.getHeight());
		
    return createTransaction(req, sender, null, 0, attachment);
  }

  private boolean isValidUser(Escrow escrow, Account sender) {
    return
        escrow.getSenderId().equals(sender.getId()) ||
        escrow.getRecipientId().equals(sender.getId()) ||
        escrowService.isIdSigner(sender.getId(), escrow);
  }
}
