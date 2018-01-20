package brs.peer;

import brs.Burst;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;

import static brs.http.common.Parameters.BLOCK_ID_PARAMETER;

final class GetNextBlockIds extends PeerServlet.PeerRequestHandler {

  static final GetNextBlockIds instance = new GetNextBlockIds();

  private GetNextBlockIds() {}


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();

    JSONArray nextBlockIds = new JSONArray();
    long blockId = Convert.parseUnsignedLong((String) request.get(BLOCK_ID_PARAMETER));
    List<Long> ids = Burst.getBlockchain().getBlockIdsAfter(blockId, 1440);

    for (Long id : ids) {
      nextBlockIds.add(Convert.toUnsignedLong(id));
    }

    response.put("nextBlockIds", nextBlockIds);

    return response;
  }

}
