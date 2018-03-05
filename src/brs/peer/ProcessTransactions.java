package brs.peer;

import brs.BurstException;
import brs.TransactionProcessor;
import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions extends PeerServlet.PeerRequestHandler {

  private final TransactionProcessor transactionProcessor;

  ProcessTransactions(TransactionProcessor transactionProcessor) {
    this.transactionProcessor = transactionProcessor;
  }


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    try {
      transactionProcessor.processPeerTransactions(request);
      return JSON.emptyJSON;
    } catch (RuntimeException | BurstException.ValidationException e) {
      //logger.debug("Failed to parse peer transactions: " + request.toJSONString());
      peer.blacklist(e);
      JSONObject response = new JSONObject();
      response.put("error", e.toString());
      return response;
    }
  }
}
