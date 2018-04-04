package brs.peer;

import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.BurstException;
import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class ProcessBlock extends PeerServlet.PeerRequestHandler {

  private final Blockchain blockchain;
  private final BlockchainProcessor blockchainProcessor;

  public ProcessBlock(Blockchain blockchain, BlockchainProcessor blockchainProcessor) {
    this.blockchain = blockchain;
    this.blockchainProcessor = blockchainProcessor;
  }

  public static final JSONStreamAware ACCEPTED;
  static {
    JSONObject response = new JSONObject();
    response.put("accepted", true);
    ACCEPTED = JSON.prepare(response);
  }

  public static final JSONStreamAware NOT_ACCEPTED;
  static {
    JSONObject response = new JSONObject();
    response.put("accepted", false);
    NOT_ACCEPTED = JSON.prepare(response);
  }

  @Override
  public JSONStreamAware processRequest(JSONObject request, Peer peer) {

    try {

      if (! blockchain.getLastBlock().getStringId().equals(request.get("previousBlock"))) {
        // do this check first to avoid validation failures of future blocks and transactions
        // when loading blockchain from scratch
        return NOT_ACCEPTED;
      }
      blockchainProcessor.processPeerBlock(request);
      return ACCEPTED;

    } catch (BurstException|RuntimeException e) {
      if (peer != null) {
        peer.blacklist(e);
      }
      return NOT_ACCEPTED;
    }

  }

}
