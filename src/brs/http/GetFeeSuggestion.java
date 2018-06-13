package brs.http;

import brs.util.Convert;
import brs.util.FeeSuggestion;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_PRIORITY;
import static brs.http.JSONResponses.MISSING_PRIORITY;
import static brs.http.common.Parameters.FEE_SUGGESTION_PRIORITY_PARAMETER;

public final class GetFeeSuggestion extends APIServlet.APIRequestHandler {

  GetFeeSuggestion() {
    super(new APITag[] {APITag.TRANSACTIONS}, FEE_SUGGESTION_PRIORITY_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    String priorityParameter = Convert.emptyToNull(req.getParameter(FEE_SUGGESTION_PRIORITY_PARAMETER));

    if (priorityParameter == null) {
      return MISSING_PRIORITY;
    }

    FeeSuggestion.Priority priority;

    if(priorityParameter.equalsIgnoreCase("normal")) {
      priority = FeeSuggestion.Priority.NORMAL;
    } else if (priorityParameter.equalsIgnoreCase("express")) {
      priority = FeeSuggestion.Priority.EXPRESS;
    } else {
      return INCORRECT_PRIORITY;
    }

    JSONObject response = new JSONObject();
    long fee = FeeSuggestion.suggestFee(priority);
    response.put("suggestedFee", fee);

    return response;
  }

}
