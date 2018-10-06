package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_TRANSACTIONS_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.FIRST_BLOCKHEIGHT_PARAMETER;
import static brs.http.common.Parameters.LAST_BLOCKHEIGHT_PARAMETER;

import brs.Block;
import brs.Blockchain;
import brs.db.BurstIterator;
import brs.http.common.Parameters;
import brs.services.BlockService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBlocks extends APIServlet.APIRequestHandler {

  private final Blockchain blockchain;
  private final BlockService blockService;

  GetBlocks(Blockchain blockchain, BlockService blockService) {
    super(new APITag[] {APITag.BLOCKS}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, FIRST_BLOCKHEIGHT_PARAMETER, LAST_BLOCKHEIGHT_PARAMETER, INCLUDE_TRANSACTIONS_PARAMETER);
    this.blockchain = blockchain;
    this.blockService = blockService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    int firstBlockHeight = ParameterParser.getFirstBlockHeight(req);
    int lastBlockHeight = ParameterParser.getLastBlockHeight(req);
    if (firstBlockHeight > 0) {
        int height = blockchain.getHeight();
        firstIndex = height - lastBlockHeight;
        lastIndex = height - firstBlockHeight;
        if (firstIndex < 0 || lastIndex - firstIndex > 99) {
            firstIndex = lastIndex - 99;
        }
    } else {
        firstIndex = ParameterParser.getFirstIndex(req);
        lastIndex = ParameterParser.getLastIndex(req);
        if (lastIndex < 0 || lastIndex - firstIndex > 99) {
          lastIndex = firstIndex + 99;
        }
    }


    boolean includeTransactions = Parameters.isTrue(req.getParameter(Parameters.INCLUDE_TRANSACTIONS_PARAMETER));

    JSONArray blocks = new JSONArray();
    try (BurstIterator<? extends Block> iterator = blockchain.getBlocks(firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Block block = iterator.next();
        blocks.add(JSONData.block(block, includeTransactions, blockchain.getHeight(), blockService.getBlockReward(block), blockService.getScoopNum(block)));
      }
    }

    JSONObject response = new JSONObject();
    response.put("blocks", blocks);

    return response;
  }

}
