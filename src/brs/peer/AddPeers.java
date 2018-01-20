package brs.peer;

import brs.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.http.common.Parameters.PEERS_PARAMETER;

final class AddPeers extends PeerServlet.PeerRequestHandler {

  static final AddPeers instance = new AddPeers();

  private AddPeers() {}

  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {
    JSONArray peers = (JSONArray)request.get(PEERS_PARAMETER);
    if (peers != null && Peers.getMorePeers) {
      for (Object announcedAddress : peers) {
        Peers.addPeer((String) announcedAddress);
      }
    }
    return JSON.emptyJSON;
  }

}
