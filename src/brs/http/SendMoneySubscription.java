package brs.http;
import static brs.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static brs.http.common.Parameters.FREQUENCY_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.Constants;
import brs.BurstException;
import brs.services.ParameterService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendMoneySubscription extends CreateTransaction {

  private final ParameterService parameterService;
  private final Blockchain blockchain;
	
  public SendMoneySubscription(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER, AMOUNT_NQT_PARAMETER, FREQUENCY_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account sender = parameterService.getSenderAccount(req);
    Long recipient = ParameterParser.getRecipientId(req);
    Long amountNQT = ParameterParser.getAmountNQT(req);
		
    int frequency;
    try {
      frequency = Integer.parseInt(req.getParameter(FREQUENCY_PARAMETER));
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid or missing frequency parameter");
      return response;
    }
		
    if(frequency < Constants.BURST_SUBSCRIPTION_MIN_FREQ ||
       frequency > Constants.BURST_SUBSCRIPTION_MAX_FREQ) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid frequency amount");
      return response;
    }
		
    Attachment.AdvancedPaymentSubscriptionSubscribe attachment = new Attachment.AdvancedPaymentSubscriptionSubscribe(frequency, blockchain.getHeight());
		
    return createTransaction(req, sender, recipient, amountNQT, attachment);
  }
}
