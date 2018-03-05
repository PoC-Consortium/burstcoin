package brs.http;

import brs.Subscription;
import brs.services.SubscriptionService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.SUBSCRIPTION_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class GetSubscription extends APIServlet.APIRequestHandler {
	
  private final SubscriptionService subscriptionService;

  GetSubscription(SubscriptionService subscriptionService) {
    super(new APITag[] {APITag.ACCOUNTS}, SUBSCRIPTION_PARAMETER);
    this.subscriptionService = subscriptionService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    Long subscriptionId;
    try {
      subscriptionId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(SUBSCRIPTION_PARAMETER)));
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified subscription");
      return response;
    }
		
    Subscription subscription = subscriptionService.getSubscription(subscriptionId);

    if(subscription == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 5);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Subscription not found");
      return response;
    }
		
    return JSONData.subscription(subscription);
  }
}
