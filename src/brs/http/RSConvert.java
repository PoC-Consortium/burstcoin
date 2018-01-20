package brs.http;

import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.JSONResponses.MISSING_ACCOUNT;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.ResultFields.ACCOUNT_RESPONSE;

public final class RSConvert extends APIServlet.APIRequestHandler {

  static final RSConvert instance = new RSConvert();

  private RSConvert() {
    super(new APITag[] {APITag.ACCOUNTS, APITag.UTILS}, ACCOUNT_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    String accountValue = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
    if (accountValue == null) {
      return MISSING_ACCOUNT;
    }
    try {
      long accountId = Convert.parseAccountId(accountValue);
      if (accountId == 0) {
        return INCORRECT_ACCOUNT;
      }
      JSONObject response = new JSONObject();
      JSONData.putAccount(response, ACCOUNT_RESPONSE, accountId);
      return response;
    } catch (RuntimeException e) {
      return INCORRECT_ACCOUNT;
    }
  }

}
