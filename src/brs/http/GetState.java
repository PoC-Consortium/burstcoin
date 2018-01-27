package brs.http;

import static brs.http.common.Parameters.INCLUDE_COUNTS_PARAMETER;

import brs.*;
import brs.db.BurstIterator;
import brs.peer.Peer;
import brs.peer.Peers;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetTransferService;
import brs.services.EscrowService;
import brs.services.OrderService;
import brs.services.TradeService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetState extends APIServlet.APIRequestHandler {

  private final Blockchain blockchain;
  private final TradeService tradeService;
  private final AccountService accountService;
  private final EscrowService escrowService;
  private final OrderService orderService;
  private final AssetTransferService assetTransferService;
  private final AliasService aliasService;

  GetState(Blockchain blockchain, TradeService tradeService, AccountService accountService, EscrowService escrowService, OrderService orderService,
      AssetTransferService assetTransferService, AliasService aliasService) {
    super(new APITag[] {APITag.INFO}, INCLUDE_COUNTS_PARAMETER);
    this.blockchain = blockchain;
    this.tradeService = tradeService;
    this.accountService = accountService;
    this.escrowService = escrowService;
    this.orderService = orderService;
    this.assetTransferService = assetTransferService;
    this.aliasService = aliasService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    JSONObject response = new JSONObject();

    response.put("application", Burst.APPLICATION);
    response.put("version", Burst.VERSION);
    response.put("time", Burst.getEpochTime());
    response.put("lastBlock", blockchain.getLastBlock().getStringId());
    response.put("cumulativeDifficulty", blockchain.getLastBlock().getCumulativeDifficulty().toString());


    long totalEffectiveBalance = 0;
    try (BurstIterator<Account> accounts = accountService.getAllAccounts(0, -1)) {
      while(accounts.hasNext()) {
        long effectiveBalanceNXT = accounts.next().getBalanceNQT();
        if (effectiveBalanceNXT > 0) {
          totalEffectiveBalance += effectiveBalanceNXT;
        }
      }
    }
    try(BurstIterator<Escrow> escrows = escrowService.getAllEscrowTransactions()) {
      while(escrows.hasNext()) {
        totalEffectiveBalance += escrows.next().getAmountNQT();
      }
    }
    response.put("totalEffectiveBalanceNXT", totalEffectiveBalance / Constants.ONE_BURST);


    if (!"false".equalsIgnoreCase(req.getParameter("includeCounts"))) {
      response.put("numberOfBlocks", blockchain.getHeight() + 1);
      response.put("numberOfTransactions", blockchain.getTransactionCount());
      response.put("numberOfAccounts", Account.getCount());
      response.put("numberOfAssets", Asset.getCount());
      int askCount = orderService.getAskCount();
      int bidCount = orderService.getBidCount();
      response.put("numberOfOrders", askCount + bidCount);
      response.put("numberOfAskOrders", askCount);
      response.put("numberOfBidOrders", bidCount);
      response.put("numberOfTrades", tradeService.getCount());
      response.put("numberOfTransfers", assetTransferService.getAssetTransferCount());
      response.put("numberOfAliases", aliasService.getAliasCount());
      //response.put("numberOfPolls", Poll.getCount());
      //response.put("numberOfVotes", Vote.getCount());
    }
    response.put("numberOfPeers", Peers.getAllPeers().size());
    response.put("numberOfUnlockedAccounts", Burst.getGenerator().getAllGenerators().size());
    Peer lastBlockchainFeeder = Burst.getBlockchainProcessor().getLastBlockchainFeeder();
    response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
    response.put("lastBlockchainFeederHeight", Burst.getBlockchainProcessor().getLastBlockchainFeederHeight());
    response.put("isScanning", Burst.getBlockchainProcessor().isScanning());
    response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
    response.put("maxMemory", Runtime.getRuntime().maxMemory());
    response.put("totalMemory", Runtime.getRuntime().totalMemory());
    response.put("freeMemory", Runtime.getRuntime().freeMemory());

    return response;
  }

}
