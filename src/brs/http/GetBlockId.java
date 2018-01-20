package brs.http;

import brs.Blockchain;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_HEIGHT;
import static brs.http.JSONResponses.MISSING_HEIGHT;
import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.ResultFields.BLOCK_RESPONSE;

public final class GetBlockId extends APIServlet.APIRequestHandler {

  private final Blockchain blockchain;

  GetBlockId(Blockchain blockchain) {
    super(new APITag[] {APITag.BLOCKS}, HEIGHT_PARAMETER);
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    int height;
    try {
      String heightValue = Convert.emptyToNull(req.getParameter(HEIGHT_PARAMETER));
      if (heightValue == null) {
        return MISSING_HEIGHT;
      }
      height = Integer.parseInt(heightValue);
    } catch (RuntimeException e) {
      return INCORRECT_HEIGHT;
    }

    try {
      JSONObject response = new JSONObject();
      response.put(BLOCK_RESPONSE, Convert.toUnsignedLong(blockchain.getBlockIdAtHeight(height)));
      return response;
    } catch (RuntimeException e) {
      return INCORRECT_HEIGHT;
    }

  }

}
