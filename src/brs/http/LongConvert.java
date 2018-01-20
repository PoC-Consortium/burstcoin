package brs.http;

import brs.util.Convert;
import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

import static brs.http.common.Parameters.ID_PARAMETER;
import static brs.http.common.ResultFields.ERROR_RESPONSE;

public final class LongConvert extends APIServlet.APIRequestHandler {

  static final LongConvert instance = new LongConvert();

  private LongConvert() {
    super(new APITag[] {APITag.UTILS}, ID_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    String id = Convert.emptyToNull(req.getParameter(ID_PARAMETER));
    if (id == null) {
      return JSON.emptyJSON;
    }
    JSONObject response = new JSONObject();
    BigInteger bigInteger = new BigInteger(id);
    if (bigInteger.signum() < 0) {
      if (bigInteger.negate().compareTo(Convert.two64) > 0) {
        response.put(ERROR_RESPONSE, "overflow");
      }
      else {
        response.put("stringId", bigInteger.add(Convert.two64).toString());
        response.put("longId",   String.valueOf(bigInteger.longValue()));
      }
    }
    else {
      if (bigInteger.compareTo(Convert.two64) >= 0) {
        response.put(ERROR_RESPONSE, "overflow");
      }
      else {
        response.put("stringId", bigInteger.toString());
        response.put("longId",   String.valueOf(bigInteger.longValue()));
      }
    }
    return response;
  }

}
