package brs.http;

import static brs.http.JSONResponses.ERROR_INCORRECT_REQUEST;
import static brs.http.JSONResponses.ERROR_NOT_ALLOWED;
import static brs.http.JSONResponses.POST_REQUIRED;

import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.Burst;
import brs.BurstException;
import brs.EconomicClustering;
import brs.Generator;
import brs.TransactionProcessor;
import brs.common.Props;
import brs.services.ATService;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.BlockService;
import brs.services.DGSGoodsStoreService;
import brs.services.EscrowService;
import brs.services.OrderService;
import brs.services.ParameterService;
import brs.services.PropertyService;
import brs.services.SubscriptionService;
import brs.services.TimeService;
import brs.services.TradeService;
import brs.services.TransactionService;
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class APIServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(APIServlet.class);

  public APIServlet(TransactionProcessor transactionProcessor, Blockchain blockchain, BlockchainProcessor blockchainProcessor, ParameterService parameterService,
      AccountService accountService, AliasService aliasService, OrderService orderService, AssetService assetService, AssetTransferService assetTransferService,
      TradeService tradeService, EscrowService escrowService, DGSGoodsStoreService digitalGoodsStoreService, AssetAccountService assetAccountService,
      SubscriptionService subscriptionService, ATService atService, TimeService timeService, EconomicClustering economicClustering, TransactionService transactionService,
      BlockService blockService, Generator generator, PropertyService propertyService, APITransactionManager apiTransactionManager) {
    enforcePost = propertyService.getBoolean(Props.API_SERVER_ENFORCE_POST);

    final Map<String, APIRequestHandler> map = new HashMap<>();

    map.put("broadcastTransaction", new BroadcastTransaction(transactionProcessor, parameterService, transactionService));
    map.put("calculateFullHash", new CalculateFullHash());
    map.put("cancelAskOrder", new CancelAskOrder(parameterService, blockchain, orderService, apiTransactionManager));
    map.put("cancelBidOrder", new CancelBidOrder(parameterService, blockchain, orderService, apiTransactionManager));
    //map.put("castVote", CastVote.instance);
    //map.put("createPoll", CreatePoll.instance);
    map.put("decryptFrom", new DecryptFrom(parameterService));
    map.put("dgsListing", new DGSListing(parameterService, blockchain, apiTransactionManager));
    map.put("dgsDelisting", new DGSDelisting(parameterService, blockchain, apiTransactionManager));
    map.put("dgsDelivery", new DGSDelivery(parameterService, blockchain, accountService, apiTransactionManager));
    map.put("dgsFeedback", new DGSFeedback(parameterService, blockchain, accountService, apiTransactionManager));
    map.put("dgsPriceChange", new DGSPriceChange(parameterService, blockchain, apiTransactionManager));
    map.put("dgsPurchase", new DGSPurchase(parameterService, blockchain, accountService, timeService, apiTransactionManager));
    map.put("dgsQuantityChange", new DGSQuantityChange(parameterService, blockchain, apiTransactionManager));
    map.put("dgsRefund", new DGSRefund(parameterService, blockchain, accountService, apiTransactionManager));
    map.put("decodeHallmark", new DecodeHallmark());
    map.put("decodeToken", new DecodeToken());
    map.put("encryptTo", new EncryptTo(parameterService, accountService));
    map.put("generateToken", new GenerateToken(timeService));
    map.put("getAccount", new GetAccount(parameterService, accountService));
    map.put("getAccountBlockIds", new GetAccountBlockIds(parameterService, blockchain));
    map.put("getAccountBlocks", new GetAccountBlocks(blockchain, parameterService, blockService));
    map.put("getAccountId", new GetAccountId());
    map.put("getAccountPublicKey", new GetAccountPublicKey(parameterService));
    map.put("getAccountTransactionIds", new GetAccountTransactionIds(parameterService, blockchain));
    map.put("getAccountTransactions", new GetAccountTransactions(parameterService, blockchain));
    map.put("getAccountLessors", new GetAccountLessors(parameterService, blockchain));
    map.put("sellAlias", new SellAlias(parameterService, blockchain, apiTransactionManager));
    map.put("buyAlias", new BuyAlias(parameterService, blockchain, aliasService, apiTransactionManager));
    map.put("getAlias", new GetAlias(parameterService, aliasService));
    map.put("getAliases", new GetAliases(parameterService, aliasService));
    map.put("getAllAssets", new GetAllAssets(assetService, assetAccountService, assetTransferService, tradeService));
    map.put("getAsset", new GetAsset(parameterService, assetAccountService, assetTransferService, tradeService));
    map.put("getAssets", new GetAssets(assetService, assetAccountService, assetTransferService, tradeService));
    map.put("getAssetIds", new GetAssetIds(assetService));
    map.put("getAssetsByIssuer", new GetAssetsByIssuer(parameterService, assetService, tradeService, assetTransferService, assetAccountService));
    map.put("getAssetAccounts", new GetAssetAccounts(parameterService, assetService));
    map.put("getBalance", new GetBalance(parameterService));
    map.put("getBlock", new GetBlock(blockchain, blockService));
    map.put("getBlockId", new GetBlockId(blockchain));
    map.put("getBlocks", new GetBlocks(blockchain, blockService));
    map.put("getBlockchainStatus", new GetBlockchainStatus(blockchainProcessor, blockchain, timeService));
    map.put("getConstants", GetConstants.instance);
    map.put("getDGSGoods", new GetDGSGoods(digitalGoodsStoreService));
    map.put("getDGSGood", new GetDGSGood(parameterService));
    map.put("getDGSPurchases", new GetDGSPurchases(digitalGoodsStoreService));
    map.put("getDGSPurchase", new GetDGSPurchase(parameterService));
    map.put("getDGSPendingPurchases", new GetDGSPendingPurchases(digitalGoodsStoreService));
    map.put("getGuaranteedBalance", new GetGuaranteedBalance(parameterService));
    map.put("getECBlock", new GetECBlock(blockchain, timeService, economicClustering));
    map.put("getMyInfo", GetMyInfo.instance);
    //map.put("getNextBlockGenerators", GetNextBlockGenerators.instance);
    map.put("getPeer", GetPeer.instance);
    map.put("getPeers", GetPeers.instance);
    //map.put("getPoll", GetPoll.instance);
    //map.put("getPollIds", GetPollIds.instance);
    map.put("getState", new GetState(blockchain, tradeService, accountService, escrowService, orderService, assetTransferService, aliasService, timeService, assetService, generator));
    map.put("getTime", new GetTime(timeService));
    map.put("getTrades", new GetTrades(parameterService, assetService, tradeService));
    map.put("getAllTrades", new GetAllTrades(tradeService, assetService));
    map.put("getAssetTransfers", new GetAssetTransfers(parameterService, accountService, assetService, assetTransferService));
    map.put("getTransaction", new GetTransaction(transactionProcessor, blockchain));
    map.put("getTransactionBytes", new GetTransactionBytes(blockchain, transactionProcessor));
    map.put("getUnconfirmedTransactionIds", new GetUnconfirmedTransactionIds(transactionProcessor));
    map.put("getUnconfirmedTransactions", new GetUnconfirmedTransactions(transactionProcessor));
    map.put("getAccountCurrentAskOrderIds", new GetAccountCurrentAskOrderIds(parameterService, orderService));
    map.put("getAccountCurrentBidOrderIds", new GetAccountCurrentBidOrderIds(parameterService, orderService));
    map.put("getAccountCurrentAskOrders", new GetAccountCurrentAskOrders(parameterService, orderService));
    map.put("getAccountCurrentBidOrders", new GetAccountCurrentBidOrders(parameterService, orderService));
    map.put("getAllOpenAskOrders", new GetAllOpenAskOrders(orderService));
    map.put("getAllOpenBidOrders", new GetAllOpenBidOrders(orderService));
    map.put("getAskOrder", new GetAskOrder(orderService));
    map.put("getAskOrderIds", new GetAskOrderIds(parameterService, orderService));
    map.put("getAskOrders", new GetAskOrders(parameterService, orderService));
    map.put("getBidOrder", new GetBidOrder(orderService));
    map.put("getBidOrderIds", new GetBidOrderIds(parameterService, orderService));
    map.put("getBidOrders", new GetBidOrders(parameterService, orderService));
    map.put("issueAsset", new IssueAsset(parameterService, blockchain, apiTransactionManager));
    map.put("leaseBalance", new LeaseBalance(parameterService, blockchain, accountService, apiTransactionManager));
    map.put("longConvert", LongConvert.instance);
    map.put("markHost", MarkHost.instance);
    map.put("parseTransaction", new ParseTransaction(parameterService, transactionService));
    map.put("placeAskOrder", new PlaceAskOrder(parameterService, blockchain, apiTransactionManager, accountService));
    map.put("placeBidOrder", new PlaceBidOrder(parameterService, blockchain, apiTransactionManager));
    map.put("rsConvert", RSConvert.instance);
    map.put("readMessage", new ReadMessage(blockchain, accountService));
    map.put("sendMessage", new SendMessage(parameterService, apiTransactionManager));
    map.put("sendMoney", new SendMoney(parameterService, apiTransactionManager));
    map.put("setAccountInfo", new SetAccountInfo(parameterService, blockchain, apiTransactionManager));
    map.put("setAlias", new SetAlias(parameterService, blockchain, aliasService, apiTransactionManager));
    map.put("signTransaction", new SignTransaction(parameterService, transactionService));
    //map.put("startForging", StartForging.instance);
    //map.put("stopForging", StopForging.instance);
    //map.put("getForging", GetForging.instance);
    map.put("transferAsset", new TransferAsset(parameterService, blockchain, apiTransactionManager, accountService));
    map.put("getMiningInfo", new GetMiningInfo(blockchain));
    map.put("submitNonce", new SubmitNonce(accountService, blockchain, generator));
    map.put("getRewardRecipient", new GetRewardRecipient(parameterService, blockchain, accountService));
    map.put("setRewardRecipient", new SetRewardRecipient(parameterService, blockchain, accountService, apiTransactionManager));
    map.put("getAccountsWithRewardRecipient", new GetAccountsWithRewardRecipient(parameterService, accountService));
    map.put("sendMoneyEscrow", new SendMoneyEscrow(parameterService, blockchain, apiTransactionManager));
    map.put("escrowSign", new EscrowSign(parameterService, blockchain, escrowService, apiTransactionManager));
    map.put("getEscrowTransaction", new GetEscrowTransaction(escrowService));
    map.put("getAccountEscrowTransactions", new GetAccountEscrowTransactions(parameterService, escrowService));
    map.put("sendMoneySubscription", new SendMoneySubscription(parameterService, blockchain, apiTransactionManager));
    map.put("subscriptionCancel", new SubscriptionCancel(parameterService, subscriptionService, blockchain, apiTransactionManager));
    map.put("getSubscription", new GetSubscription(subscriptionService));
    map.put("getAccountSubscriptions", new GetAccountSubscriptions(parameterService, subscriptionService));
    map.put("getSubscriptionsToAccount", new GetSubscriptionsToAccount(parameterService, subscriptionService));
    map.put("createATProgram", new CreateATProgram(parameterService, blockchain, apiTransactionManager));
    map.put("getAT", new GetAT(parameterService, accountService));
    map.put("getATDetails", new GetATDetails(parameterService, accountService));
    map.put("getATIds", new GetATIds(atService));
    map.put("getATLong", GetATLong.instance);
    map.put("getAccountATs", new GetAccountATs(parameterService, atService, accountService));

    if (API.enableDebugAPI) {
      map.put("clearUnconfirmedTransactions", new ClearUnconfirmedTransactions(transactionProcessor));
      map.put("fullReset", new FullReset(blockchainProcessor));
      map.put("popOff", new PopOff(blockchainProcessor, blockchain, blockService));
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

  private static boolean enforcePost;

  static Map<String, APIRequestHandler> apiRequestHandlers;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    process(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

      String requestType = req.getParameter("requestType");
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
