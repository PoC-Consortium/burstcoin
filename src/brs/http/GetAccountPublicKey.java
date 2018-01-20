package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.ResultFields.PUBLIC_KEY_RESPONSE;

import brs.Account;
import brs.BurstException;
import brs.services.ParameterService;
import brs.util.Convert;
import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountPublicKey extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAccountPublicKey(ParameterService parameterService) {
    super(new APITag[] {APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getAccount(req);

    if (account.getPublicKey() != null) {
      JSONObject response = new JSONObject();
      response.put(PUBLIC_KEY_RESPONSE, Convert.toHexString(account.getPublicKey()));
      return response;
    } else {
      return JSON.emptyJSON;
    }
  }

}
