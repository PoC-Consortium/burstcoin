package brs.http;

import brs.Constants;
import brs.peer.Hallmark;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.*;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;


public final class MarkHost extends APIServlet.APIRequestHandler {

  static final MarkHost instance = new MarkHost();

  private MarkHost() {
    super(new APITag[] {APITag.TOKENS}, SECRET_PHRASE_PARAMETER, "host", "weight", "date");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);
    String host = req.getParameter("host");
    String weightValue = req.getParameter("weight");
    String dateValue = req.getParameter("date");
    if (secretPhrase == null) {
      return MISSING_SECRET_PHRASE;
    } else if (host == null) {
      return MISSING_HOST;
    } else if (weightValue == null) {
      return MISSING_WEIGHT;
    } else if (dateValue == null) {
      return MISSING_DATE;
    }

    if (host.length() > 100) {
      return INCORRECT_HOST;
    }

    int weight;
    try {
      weight = Integer.parseInt(weightValue);
      if (weight <= 0 || weight > Constants.MAX_BALANCE_NXT) {
        return INCORRECT_WEIGHT;
      }
    } catch (NumberFormatException e) {
      return INCORRECT_WEIGHT;
    }

    try {

      String hallmark = Hallmark.generateHallmark(secretPhrase, host, weight, Hallmark.parseDate(dateValue));

      JSONObject response = new JSONObject();
      response.put("hallmark", hallmark);
      return response;

    } catch (RuntimeException e) {
      return INCORRECT_DATE;
    }

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
