package brs.peer;

import brs.Block;
import brs.Blockchain;
import brs.Constants;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.ArrayList;
import java.util.List;

final class GetNextBlocks extends PeerServlet.PeerRequestHandler {

  private final Blockchain blockchain;

  GetNextBlocks(Blockchain blockchain) {
    this.blockchain = blockchain;
  }


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();

    List<Block> nextBlocks = new ArrayList<>();
    int totalLength = 0;
    long blockId = Convert.parseUnsignedLong(request.get("blockId").toString());
    List<? extends Block> blocks = blockchain.getBlocksAfter(blockId, 100);

    for (Block block : blocks) {
      int length = Constants.BLOCK_HEADER_LENGTH + block.getPayloadLength();
      if (totalLength + length > 1048576) {
        break;
      }
      nextBlocks.add(block);
      totalLength += length;
    }

    JSONArray nextBlocksArray = new JSONArray();
    for (Block nextBlock : nextBlocks) {
      nextBlocksArray.add(nextBlock.getJSONObject());
    }
    response.put("nextBlocks", nextBlocksArray);

    return response;
  }

}
