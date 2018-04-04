package brs.peer;

import brs.Blockchain;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;

final class GetNextBlockIds extends PeerServlet.PeerRequestHandler {

  private final Blockchain blockchain;

  GetNextBlockIds(Blockchain blockchain) {
    this.blockchain = blockchain;
  }


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();

    JSONArray nextBlockIds = new JSONArray();
    long blockId = Convert.parseUnsignedLong(request.get("blockId").toString());
    List<Long> ids = blockchain.getBlockIdsAfter(blockId, 100);

    for (Long id : ids) {
      nextBlockIds.add(Convert.toUnsignedLong(id));
    }

    response.put("nextBlockIds", nextBlockIds);

    return response;
  }

}
