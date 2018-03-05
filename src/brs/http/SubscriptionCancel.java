package brs.http;

import static brs.http.common.Parameters.SUBSCRIPTION_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Subscription;
import brs.services.ParameterService;
import brs.services.SubscriptionService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class SubscriptionCancel extends CreateTransaction {

  private final ParameterService parameterService;
  private final SubscriptionService subscriptionService;
  private final Blockchain blockchain;

  public SubscriptionCancel(ParameterService parameterService, SubscriptionService subscriptionService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
    super(new APITag[]{APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, apiTransactionManager, SUBSCRIPTION_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
    this.subscriptionService = subscriptionService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final Account sender = parameterService.getSenderAccount(req);

    String subscriptionString = Convert.emptyToNull(req.getParameter(SUBSCRIPTION_PARAMETER));
    if (subscriptionString == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Subscription Id not specified");
      return response;
    }

    Long subscriptionId;
    try {
      subscriptionId = Convert.parseUnsignedLong(subscriptionString);
    } catch (Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Failed to parse subscription id");
      return response;
    }

    Subscription subscription = subscriptionService.getSubscription(subscriptionId);
    if (subscription == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 5);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Subscription not found");
      return response;
    }

    if (sender.getId() != subscription.getSenderId() &&
        sender.getId() != subscription.getRecipientId()) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 7);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Must be sender or recipient to cancel subscription");
      return response;
    }

    Attachment.AdvancedPaymentSubscriptionCancel attachment = new Attachment.AdvancedPaymentSubscriptionCancel(subscription.getId(), blockchain.getHeight());

    return createTransaction(req, sender, null, 0, attachment);
  }
}
