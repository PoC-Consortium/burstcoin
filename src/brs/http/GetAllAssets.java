package brs.http;

import brs.Asset;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

public final class GetAllAssets extends APIServlet.APIRequestHandler {

  static final GetAllAssets instance = new GetAllAssets();

  private GetAllAssets() {
    super(new APITag[] {APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONObject response = new JSONObject();
    JSONArray assetsJSONArray = new JSONArray();
    response.put(ASSETS_RESPONSE, assetsJSONArray);
    try (BurstIterator<Asset> assets = Asset.getAllAssets(firstIndex, lastIndex)) {
      while (assets.hasNext()) {
        assetsJSONArray.add(JSONData.asset(assets.next()));
      }
    }
    return response;
  }

}
