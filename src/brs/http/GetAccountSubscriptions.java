package brs.http;

import brs.Account;
import brs.BurstException;
import brs.Subscription;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import brs.services.SubscriptionService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.SUBSCRIPTIONS_RESPONSE;

public final class GetAccountSubscriptions extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final SubscriptionService subscriptionService;

  GetAccountSubscriptions(ParameterService parameterService, SubscriptionService subscriptionService) {
    super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
    this.subscriptionService = subscriptionService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getAccount(req);

    JSONObject response = new JSONObject();

    JSONArray subscriptions = new JSONArray();

    BurstIterator<Subscription> accountSubscriptions = subscriptionService.getSubscriptionsByParticipant(account.getId());

    while (accountSubscriptions.hasNext()) {
      subscriptions.add(JSONData.subscription(accountSubscriptions.next()));
    }

    response.put(SUBSCRIPTIONS_RESPONSE, subscriptions);
    return response;
  }
}
