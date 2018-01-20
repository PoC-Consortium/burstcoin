package brs.peer;

import brs.Burst;
import brs.BurstException;
import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.http.common.ResultFields.ERROR_RESPONSE;

final class ProcessTransactions extends PeerServlet.PeerRequestHandler {

  static final ProcessTransactions instance = new ProcessTransactions();

  private ProcessTransactions() {}


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    try {
      Burst.getTransactionProcessor().processPeerTransactions(request);
      return JSON.emptyJSON;
    } catch (RuntimeException | BurstException.ValidationException e) {
      //logger.debug("Failed to parse peer transactions: " + request.toJSONString());
      peer.blacklist(e);
      JSONObject response = new JSONObject();
      response.put(ERROR_RESPONSE, e.toString());
      return response;
    }

  }

}
