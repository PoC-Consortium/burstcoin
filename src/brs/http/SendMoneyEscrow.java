package brs.http;

import static brs.http.common.Parameters.*;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.*;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

public final class SendMoneyEscrow extends CreateTransaction {
	
  private final ParameterService parameterService;
  private final Blockchain blockchain;
	
  SendMoneyEscrow(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER, AMOUNT_NQT_PARAMETER, ESCROW_DEADLINE_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account sender = parameterService.getSenderAccount(req);
    Long recipient = ParameterParser.getRecipientId(req);
    Long amountNQT = ParameterParser.getAmountNQT(req);
    String signerString = Convert.emptyToNull(req.getParameter(SIGNERS_PARAMETER));
		
    Long requiredSigners;
    try {
      requiredSigners = Convert.parseLong(req.getParameter(REQUIRED_SIGNERS_PARAMETER));
      if(requiredSigners < 1 || requiredSigners > 10) {
        JSONObject response = new JSONObject();
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid number of requiredSigners");
        return response;
      }
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid requiredSigners parameter");
      return response;
    }
		
    if(signerString == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Signers not specified");
      return response;
    }
		
    String signersArray[] = signerString.split(";", 10);
		
    if(signersArray.length < 1 || signersArray.length > 10 || signersArray.length < requiredSigners) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid number of signers");
      return response;
    }
		
    ArrayList<Long> signers = new ArrayList<>();
		
    try {
      for(String signer : signersArray) {
        long id = Convert.parseAccountId(signer);
        signers.add(id);
      }
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid signers parameter");
      return response;
    }
		
    Long totalAmountNQT = Convert.safeAdd(amountNQT, signers.size() * Constants.ONE_BURST);
    if(sender.getBalanceNQT() < totalAmountNQT) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 6);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Insufficient funds");
      return response;
    }
		
    Long deadline;
    try {
      deadline = Convert.parseLong(req.getParameter(ESCROW_DEADLINE_PARAMETER));
      if(deadline < 1 || deadline > 7776000) {
        JSONObject response = new JSONObject();
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Escrow deadline must be 1 - 7776000");
        return response;
      }
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid " + ESCROW_DEADLINE_PARAMETER + " parameter");
      return response;
    }

    Escrow.DecisionType deadlineAction = Escrow.stringToDecision(req.getParameter(DEADLINE_ACTION_PARAMETER));
    if(deadlineAction == null || deadlineAction == Escrow.DecisionType.UNDECIDED) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid " + DEADLINE_ACTION_PARAMETER + " parameter");
      return response;
    }
		
    Attachment.AdvancedPaymentEscrowCreation attachment = new Attachment.AdvancedPaymentEscrowCreation(amountNQT, deadline.intValue(), deadlineAction, requiredSigners.intValue(), signers, blockchain.getHeight());
		
    return createTransaction(req, sender, recipient, 0, attachment);
  }
}
