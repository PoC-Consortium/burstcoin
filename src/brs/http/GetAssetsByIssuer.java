package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

import brs.Account;
import brs.Asset;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAssetsByIssuer extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAssetsByIssuer(ParameterService parameterService) {                                 //TODO Clarify: Why 3?
    super(new APITag[] {APITag.AE, APITag.ACCOUNTS}, ACCOUNT_PARAMETER, ACCOUNT_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
    List<Account> accounts = parameterService.getAccounts(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONObject response = new JSONObject();
    JSONArray accountsJSONArray = new JSONArray();
    response.put(ASSETS_RESPONSE, accountsJSONArray);
    for (Account account : accounts) {
      JSONArray assetsJSONArray = new JSONArray();
      try (BurstIterator<Asset> assets = Asset.getAssetsIssuedBy(account.getId(), firstIndex, lastIndex)) {
        while (assets.hasNext()) {
          assetsJSONArray.add(JSONData.asset(assets.next()));
        }
      }
      accountsJSONArray.add(assetsJSONArray);
    }
    return response;
  }

}
