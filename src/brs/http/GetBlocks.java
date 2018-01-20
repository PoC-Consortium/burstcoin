package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_TRANSACTIONS_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.BLOCKS_RESPONSE;

import brs.Block;
import brs.Blockchain;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.http.common.Parameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBlocks extends APIServlet.APIRequestHandler {

  private final Blockchain blockchain;

  GetBlocks(Blockchain blockchain) {
    super(new APITag[] {APITag.BLOCKS}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_TRANSACTIONS_PARAMETER);
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    if (lastIndex < 0 || lastIndex - firstIndex > 99) {
      lastIndex = firstIndex + 99;
    }

    boolean includeTransactions = Parameters.isTrue(req.getParameter(Parameters.INCLUDE_TRANSACTIONS_PARAMETER));

    JSONArray blocks = new JSONArray();
    try (BurstIterator<? extends Block> iterator = blockchain.getBlocks(firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Block block = iterator.next();
        blocks.add(JSONData.block(block, includeTransactions));
      }
    }

    JSONObject response = new JSONObject();
    response.put(BLOCKS_RESPONSE, blocks);

    return response;
  }

}
