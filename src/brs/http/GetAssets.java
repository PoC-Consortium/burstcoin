package brs.http;

import brs.Asset;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_ASSET;
import static brs.http.JSONResponses.UNKNOWN_ASSET;
import static brs.http.common.Parameters.ASSETS_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

public final class GetAssets extends APIServlet.APIRequestHandler {

  static final GetAssets instance = new GetAssets();

  private GetAssets() {
    super(new APITag[] {APITag.AE}, ASSETS_PARAMETER, ASSETS_PARAMETER, ASSETS_PARAMETER); // limit to 3 for testing
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String[] assets = req.getParameterValues(ASSETS_PARAMETER);

    JSONObject response = new JSONObject();
    JSONArray assetsJSONArray = new JSONArray();
    response.put(ASSETS_RESPONSE, assetsJSONArray);
    for (String assetIdString : assets) {
      if (assetIdString == null || assetIdString.equals("")) {
        continue;
      }
      try {
        Asset asset = Asset.getAsset(Convert.parseUnsignedLong(assetIdString));
        if (asset == null) {
          return UNKNOWN_ASSET;
        }
        assetsJSONArray.add(JSONData.asset(asset));
      } catch (RuntimeException e) {
        return INCORRECT_ASSET;
      }
    }
    return response;
  }

}
