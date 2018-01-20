package brs.http;

import static brs.http.JSONResponses.ERROR_INCORRECT_REQUEST;
import static brs.http.JSONResponses.ERROR_NOT_ALLOWED;
import static brs.http.JSONResponses.POST_REQUIRED;
import static brs.http.common.Parameters.REQUEST_TYPE_PARAMETER;

import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.Burst;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.EscrowService;
import brs.services.OrderService;
import brs.services.ParameterService;
import brs.services.TradeService;
import brs.util.JSON;
import brs.util.Subnet;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class APIServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(APIServlet.class);

  public static void injectServices(TransactionProcessor transactionProcessor, Blockchain blockchain, BlockchainProcessor blockchainProcessor, ParameterService parameterService,
      AccountService accountService, AliasService aliasService, OrderService orderService, AssetService assetService, AssetTransferService assetTransferService,
      TradeService tradeService, EscrowService escrowService) {
    final Map<String, APIRequestHandler> map = new HashMap<>();

    map.put("broadcastTransaction", new BroadcastTransaction(transactionProcessor, parameterService));
    map.put("calculateFullHash", new CalculateFullHash());
    map.put("cancelAskOrder", new CancelAskOrder(parameterService, transactionProcessor, blockchain, accountService, orderService));
    map.put("cancelBidOrder", new CancelBidOrder(parameterService, transactionProcessor, blockchain, accountService, orderService));
    //map.put("castVote", CastVote.instance);
    //map.put("createPoll", CreatePoll.instance);
    map.put("decryptFrom", new DecryptFrom(parameterService));
    map.put("dgsListing", new DGSListing(parameterService, transactionProcessor, blockchain, accountService));
    map.put("dgsDelisting", new DGSDelisting(parameterService, transactionProcessor, blockchain, accountService));
    map.put("dgsDelivery", new DGSDelivery(parameterService, transactionProcessor, blockchain, accountService));
    map.put("dgsFeedback", new DGSFeedback(parameterService, transactionProcessor, blockchain, accountService));
    map.put("dgsPriceChange", new DGSPriceChange(parameterService, transactionProcessor, blockchain, accountService));
    map.put("dgsPurchase", new DGSPurchase(parameterService, transactionProcessor, blockchain, accountService));
    map.put("dgsQuantityChange", new DGSQuantityChange(parameterService, transactionProcessor, blockchain, accountService));
    map.put("dgsRefund", new DGSRefund(parameterService, transactionProcessor, blockchain, accountService));
    map.put("decodeHallmark", new DecodeHallmark());
    map.put("decodeToken", new DecodeToken());
    map.put("encryptTo", new EncryptTo(parameterService));
    map.put("generateToken", GenerateToken.instance);
    map.put("getAccount", new GetAccount(parameterService, accountService));
    map.put("getAccountBlockIds", new GetAccountBlockIds(parameterService, blockchain));
    map.put("getAccountBlocks", new GetAccountBlocks(blockchain, parameterService));
    map.put("getAccountId", new GetAccountId());
    map.put("getAccountPublicKey", new GetAccountPublicKey(parameterService));
    map.put("getAccountTransactionIds", new GetAccountTransactionIds(parameterService, blockchain));
    map.put("getAccountTransactions", new GetAccountTransactions(parameterService, blockchain));
    map.put("getAccountLessors", new GetAccountLessors(parameterService, blockchain));
    map.put("sellAlias", new SellAlias(parameterService, transactionProcessor, blockchain, accountService));
    map.put("buyAlias", new BuyAlias(parameterService, transactionProcessor, blockchain, aliasService, accountService));
    map.put("getAlias", new GetAlias(parameterService));
    map.put("getAliases", new GetAliases(parameterService));
    map.put("getAllAssets", GetAllAssets.instance);
    map.put("getAsset", new GetAsset(parameterService));
    map.put("getAssets", GetAssets.instance);
    map.put("getAssetIds", GetAssetIds.instance);
    map.put("getAssetsByIssuer", new GetAssetsByIssuer(parameterService));
    map.put("getAssetAccounts", new GetAssetAccounts(parameterService, assetService));
    map.put("getBalance", new GetBalance(parameterService));
    map.put("getBlock", new GetBlock(blockchain));
    map.put("getBlockId", new GetBlockId(blockchain));
    map.put("getBlocks", new GetBlocks(blockchain));
    map.put("getBlockchainStatus", new GetBlockchainStatus(blockchainProcessor, blockchain));
    map.put("getConstants", GetConstants.instance);
    map.put("getDGSGoods", GetDGSGoods.instance);
    map.put("getDGSGood", new GetDGSGood(parameterService));
    map.put("getDGSPurchases", GetDGSPurchases.instance);
    map.put("getDGSPurchase", GetDGSPurchase.instance);
    map.put("getDGSPendingPurchases", GetDGSPendingPurchases.instance);
    map.put("getGuaranteedBalance", new GetGuaranteedBalance(parameterService));
    map.put("getECBlock", new GetECBlock(blockchain));
    map.put("getMyInfo", GetMyInfo.instance);
    //map.put("getNextBlockGenerators", GetNextBlockGenerators.instance);
    map.put("getPeer", GetPeer.instance);
    map.put("getPeers", GetPeers.instance);
    //map.put("getPoll", GetPoll.instance);
    //map.put("getPollIds", GetPollIds.instance);
    map.put("getState", new GetState(blockchain, tradeService, accountService, escrowService));
    map.put("getTime", GetTime.instance);
    map.put("getTrades", new GetTrades(parameterService, assetService, tradeService));
    map.put("getAllTrades", GetAllTrades.instance);
    map.put("getAssetTransfers", new GetAssetTransfers(parameterService, accountService, assetService, assetTransferService));
    map.put("getTransaction", new GetTransaction(transactionProcessor, blockchain));
    map.put("getTransactionBytes", new GetTransactionBytes(blockchain, transactionProcessor));
    map.put("getUnconfirmedTransactionIds", new GetUnconfirmedTransactionIds(transactionProcessor));
    map.put("getUnconfirmedTransactions", new GetUnconfirmedTransactions(transactionProcessor));
    map.put("getAccountCurrentAskOrderIds", new GetAccountCurrentAskOrderIds(parameterService));
    map.put("getAccountCurrentBidOrderIds", new GetAccountCurrentBidOrderIds(parameterService));
    map.put("getAccountCurrentAskOrders", new GetAccountCurrentAskOrders(parameterService));
    map.put("getAccountCurrentBidOrders", new GetAccountCurrentBidOrders(parameterService));
    map.put("getAllOpenAskOrders", new GetAllOpenAskOrders(orderService));
    map.put("getAllOpenBidOrders", new GetAllOpenBidOrders(orderService));
    map.put("getAskOrder", new GetAskOrder(orderService));
    map.put("getAskOrderIds", new GetAskOrderIds(parameterService));
    map.put("getAskOrders", new GetAskOrders(parameterService));
    map.put("getBidOrder", GetBidOrder.instance);
    map.put("getBidOrderIds", new GetBidOrderIds(parameterService, orderService));
    map.put("getBidOrders", new GetBidOrders(parameterService, orderService));
    map.put("issueAsset", new IssueAsset(parameterService, transactionProcessor, blockchain, accountService));
    map.put("leaseBalance", new LeaseBalance(parameterService, transactionProcessor, blockchain, accountService));
    map.put("longConvert", LongConvert.instance);
    map.put("markHost", MarkHost.instance);
    map.put("parseTransaction", new ParseTransaction(parameterService));
    map.put("placeAskOrder", new PlaceAskOrder(parameterService, transactionProcessor, blockchain, accountService));
    map.put("placeBidOrder", new PlaceBidOrder(parameterService, transactionProcessor, blockchain, accountService));
    map.put("rsConvert", RSConvert.instance);
    map.put("readMessage", new ReadMessage(blockchain, accountService));
    map.put("sendMessage", new SendMessage(parameterService, transactionProcessor, blockchain, accountService));
    map.put("sendMoney", new SendMoney(parameterService, transactionProcessor, blockchain, accountService));
    map.put("setAccountInfo", new SetAccountInfo(parameterService, transactionProcessor, blockchain, accountService));
    map.put("setAlias", new SetAlias(parameterService, transactionProcessor, blockchain, accountService, aliasService));
    map.put("signTransaction", new SignTransaction(parameterService));
    //map.put("startForging", StartForging.instance);
    //map.put("stopForging", StopForging.instance);
    //map.put("getForging", GetForging.instance);
    map.put("transferAsset", new TransferAsset(parameterService, transactionProcessor, blockchain, accountService));
    map.put("getMiningInfo", new GetMiningInfo(blockchain));
    map.put("submitNonce", new SubmitNonce(accountService, blockchain));
    map.put("getRewardRecipient", new GetRewardRecipient(parameterService, blockchain));
    map.put("setRewardRecipient", new SetRewardRecipient(parameterService, transactionProcessor, blockchain, accountService));
    map.put("getAccountsWithRewardRecipient", new GetAccountsWithRewardRecipient(parameterService, accountService));
    map.put("sendMoneyEscrow", new SendMoneyEscrow(parameterService, transactionProcessor, blockchain, accountService));
    map.put("escrowSign", new EscrowSign(parameterService, transactionProcessor, blockchain, accountService));
    map.put("getEscrowTransaction", GetEscrowTransaction.instance);
    map.put("getAccountEscrowTransactions", new GetAccountEscrowTransactions(parameterService));
    map.put("sendMoneySubscription", new SendMoneySubscription(parameterService, transactionProcessor, blockchain, accountService));
    map.put("subscriptionCancel", new SubscriptionCancel(parameterService, transactionProcessor, blockchain, accountService));
    map.put("getSubscription", GetSubscription.instance);
    map.put("getAccountSubscriptions", new GetAccountSubscriptions(parameterService));
    map.put("getSubscriptionsToAccount", new GetSubscriptionsToAccount(parameterService));
    map.put("createATProgram", new CreateATProgram(parameterService, transactionProcessor, blockchain, accountService));
    map.put("getAT", GetAT.instance);
    map.put("getATDetails", GetATDetails.instance);
    map.put("getATIds", GetATIds.instance);
    map.put("getATLong", GetATLong.instance);
    map.put("getAccountATs", new GetAccountATs(parameterService));

    if (API.enableDebugAPI) {
      map.put("clearUnconfirmedTransactions", new ClearUnconfirmedTransactions(transactionProcessor));
      map.put("fullReset", new FullReset(blockchainProcessor));
      map.put("popOff", new PopOff(blockchainProcessor, blockchain));
      map.put("scan", new Scan(blockchainProcessor, blockchain));
    }

    apiRequestHandlers = Collections.unmodifiableMap(map);
  }

  abstract static class APIRequestHandler {

    private final List<String> parameters;
    private final Set<APITag> apiTags;

    APIRequestHandler(APITag[] apiTags, String... parameters) {
      this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
      this.apiTags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(apiTags)));
    }

    final List<String> getParameters() {
      return parameters;
    }

    final Set<APITag> getAPITags() {
      return apiTags;
    }

    abstract JSONStreamAware processRequest(HttpServletRequest request) throws BurstException;

    boolean requirePost() {
      return false;
    }

    boolean startDbTransaction() {
      return false;
    }

  }

  private static final boolean enforcePost = Burst.getBooleanProperty("API.ServerEnforcePOST");

  static Map<String, APIRequestHandler> apiRequestHandlers;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    process(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    process(req, resp);
  }

  private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    resp.setHeader("Pragma", "no-cache");
    resp.setDateHeader("Expires", 0);

    JSONStreamAware response = JSON.emptyJSON;

    try {

      long startTime = System.currentTimeMillis();

      if (API.allowedBotHosts != null) {
        InetAddress remoteAddress = InetAddress.getByName(req.getRemoteHost());
        boolean allowed = false;
        for (Subnet allowedSubnet : API.allowedBotHosts) {
          if (allowedSubnet.isInNet(remoteAddress)) {
            allowed = true;
            break;
          }
        }
        if (!allowed) {
          response = ERROR_NOT_ALLOWED;
          return;
        }
      }

      String requestType = req.getParameter(REQUEST_TYPE_PARAMETER);
      if (requestType == null) {
        response = ERROR_INCORRECT_REQUEST;
        return;
      }

      APIRequestHandler apiRequestHandler = apiRequestHandlers.get(requestType);
      if (apiRequestHandler == null) {
        response = ERROR_INCORRECT_REQUEST;
        return;
      }

      if (enforcePost && apiRequestHandler.requirePost() && !"POST".equals(req.getMethod())) {
        response = POST_REQUIRED;
        return;
      }

      try {
        if (apiRequestHandler.startDbTransaction()) {
          Burst.getStores().beginTransaction();
        }
        response = apiRequestHandler.processRequest(req);
      } catch (ParameterException e) {
        response = e.getErrorResponse();
      } catch (BurstException | RuntimeException e) {
        logger.debug("Error processing API request", e);
        response = ERROR_INCORRECT_REQUEST;
      } finally {
        if (apiRequestHandler.startDbTransaction()) {
          Burst.getStores().endTransaction();
        }
      }

      if (response instanceof JSONObject) {
        ((JSONObject) response).put("requestProcessingTime", System.currentTimeMillis() - startTime);
      }

    } finally {
      resp.setContentType("text/plain; charset=UTF-8");
      try (Writer writer = resp.getWriter()) {
        response.writeJSONString(writer);
      }
    }

  }

}
