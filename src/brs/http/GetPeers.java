package brs.http;

import brs.peer.Peer;
import brs.peer.Peers;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.ACTIVE_PARAMETER;
import static brs.http.common.Parameters.STATE_PARAMETER;
import static brs.http.common.ResultFields.PEERS_RESPONSE;

public final class GetPeers extends APIServlet.APIRequestHandler {

  static final GetPeers instance = new GetPeers();

  private GetPeers() {
    super(new APITag[] {APITag.INFO}, ACTIVE_PARAMETER, STATE_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    boolean active = "true".equalsIgnoreCase(req.getParameter(ACTIVE_PARAMETER));
    String stateValue = Convert.emptyToNull(req.getParameter(STATE_PARAMETER));

    JSONArray peers = new JSONArray();
    for (Peer peer : active ? Peers.getActivePeers() : stateValue != null ? Peers.getPeers(Peer.State.valueOf(stateValue)) : Peers.getAllPeers()) {
      peers.add(peer.getPeerAddress());
    }

    JSONObject response = new JSONObject();
    response.put(PEERS_RESPONSE, peers);
    return response;
  }

}
