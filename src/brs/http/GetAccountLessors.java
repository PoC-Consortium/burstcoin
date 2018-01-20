package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.ResultFields.ACCOUNT_RESPONSE;
import static brs.http.common.ResultFields.HEIGHT_RESPONSE;

import brs.Account;
import brs.Blockchain;
import brs.Burst;
import brs.BurstException;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccountLessors extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  GetAccountLessors(ParameterService parameterService, Blockchain blockchain) {
    super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER, HEIGHT_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getAccount(req);
    int height = parameterService.getHeight(req);
    if (height < 0) {
      height = blockchain.getHeight();
    }

    JSONObject response = new JSONObject();
    JSONData.putAccount(response, ACCOUNT_RESPONSE, account.getId());
    response.put(HEIGHT_RESPONSE, height);
    JSONArray lessorsJSON = new JSONArray();

    /*try (DbIterator<Account> lessors = account.getLessors(height)) {
      if (lessors.hasNext()) {
      while (lessors.hasNext()) {
      Account lessor = lessors.next();
      JSONObject lessorJSON = new JSONObject();
      JSONData.putAccount(lessorJSON, "lessor", lessor.getId());
      lessorJSON.put("guaranteedBalanceNQT", String.valueOf(account.getGuaranteedBalanceNQT(1440, height)));
      lessorsJSON.add(lessorJSON);
      }
      }
      }*/
    response.put("lessors", lessorsJSON);
    return response;

  }

}
