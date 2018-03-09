package brs;

import brs.common.Props;
import brs.services.AccountService;
import brs.services.DGSGoodsStoreService;
import brs.services.OrderService;
import brs.services.PropertyService;
import brs.services.TradeService;
import brs.util.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

public final class DebugTrace {

  private static final Logger logger = LoggerFactory.getLogger(DebugTrace.class);

  static String QUOTE;
  static String SEPARATOR;
  static boolean LOG_UNCONFIRMED;

  static OrderService orderService;
  static DGSGoodsStoreService dgsGoodsStoreService;

  static void init(PropertyService propertyService, BlockchainProcessor blockchainProcessor,
                   AccountService accountService, TradeService tradeService,
                   OrderService orderService, DGSGoodsStoreService dgsGoodsStoreService) {

    QUOTE           = propertyService.getString(Props.BRS_DEBUG_TRACE_QUOTE, "\"");
    SEPARATOR       = propertyService.getString(Props.BRS_DEBUG_TRACE_SEPARATOR, "\t");
    LOG_UNCONFIRMED = propertyService.getBoolean(Props.BRS_DEBUG_LOG_CONFIRMED);

    DebugTrace.orderService = orderService;
    DebugTrace.dgsGoodsStoreService = dgsGoodsStoreService;

    List<String> accountIdStrings = propertyService.getStringList(Props.BRS_DEBUG_TRACE_ACCOUNTS);
    String logName = propertyService.getString(Props.BRS_DEBUG_TRACE_LOG);
    if (accountIdStrings.isEmpty() || logName == null) {
      return;
    }
    Set<Long> accountIds = new HashSet<>();
    for (String accountId : accountIdStrings) {
      if ("*".equals(accountId)) {
        accountIds.clear();
        break;
      }
      accountIds.add(Convert.parseUnsignedLong(accountId));
    }
    final DebugTrace debugTrace = addDebugTrace(accountIds, logName, blockchainProcessor, accountService, tradeService);
    blockchainProcessor.addListener(block -> debugTrace.resetLog(), BlockchainProcessor.Event.RESCAN_BEGIN);
    logger.debug("Debug tracing of " + (accountIdStrings.contains("*") ? "ALL"
                                        : String.valueOf(accountIds.size())) + " accounts enabled");
  }

  public static DebugTrace addDebugTrace(Set<Long> accountIds, String logName, BlockchainProcessor blockchainProcessor, AccountService accountService, TradeService tradeService) {
    final DebugTrace debugTrace = new DebugTrace(accountIds, logName);
    tradeService.addListener(debugTrace::trace, Trade.Event.TRADE);
    accountService.addListener(account -> debugTrace.trace(account, false), Account.Event.BALANCE);
    if (LOG_UNCONFIRMED) {
      accountService.addListener(account -> debugTrace.trace(account, true), Account.Event.UNCONFIRMED_BALANCE);
    }
    accountService.addAssetListener(accountAsset -> debugTrace.trace(accountAsset, false), Account.Event.ASSET_BALANCE);
    if (LOG_UNCONFIRMED) {
      accountService.addAssetListener(accountAsset -> debugTrace.trace(accountAsset, true), Account.Event.UNCONFIRMED_ASSET_BALANCE);
    }
    blockchainProcessor.addListener(debugTrace::traceBeforeAccept, BlockchainProcessor.Event.BEFORE_BLOCK_ACCEPT);
    blockchainProcessor.addListener(debugTrace::trace, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
    return debugTrace;
  }

  private static final String[] columns = {"height", "event", "account", "asset", "balance", "unconfirmed balance",
                                           "asset balance", "unconfirmed asset balance",
                                           "transaction amount", "transaction fee", "generation fee", "effective balance",
                                           "order", "order price", "order quantity", "order cost",
                                           "trade price", "trade quantity", "trade cost",
                                           "asset quantity", "transaction", "lessee", "lessor guaranteed balance",
                                           "purchase", "purchase price", "purchase quantity", "purchase cost", "discount", "refund",
                                           "sender", "recipient", "block", "timestamp"};

  private static final Map<String,String> headers = new HashMap<>();
  static {
    for (String entry : columns) {
      headers.put(entry, entry);
    }
  }

  private final Set<Long> accountIds;
  private final String logName;
  private PrintWriter log;

  private DebugTrace(Set<Long> accountIds, String logName) {
    this.accountIds = accountIds;
    this.logName = logName;
    resetLog();
  }

  void resetLog() {
    if (log != null) {
      log.close();
    }
    try {
      log = new PrintWriter((new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logName)))), true);
    } catch (IOException e) {
      logger.debug("Debug tracing to " + logName + " not possible", e);
      throw new RuntimeException(e);
    }
    this.log(headers);
  }

  private boolean include(long accountId) {
    return accountId != 0 && (accountIds.isEmpty() || accountIds.contains(accountId));
  }

  private boolean include(Attachment attachment) {
    if (attachment instanceof Attachment.DigitalGoodsPurchase) {
      long sellerId = dgsGoodsStoreService.getGoods(((Attachment.DigitalGoodsPurchase)attachment).getGoodsId()).getSellerId();
      return include(sellerId);
    } else if (attachment instanceof Attachment.DigitalGoodsDelivery) {
      long buyerId = dgsGoodsStoreService.getPurchase(((Attachment.DigitalGoodsDelivery)attachment).getPurchaseId()).getBuyerId();
      return include(buyerId);
    } else if (attachment instanceof Attachment.DigitalGoodsRefund) {
      long buyerId = dgsGoodsStoreService.getPurchase(((Attachment.DigitalGoodsRefund)attachment).getPurchaseId()).getBuyerId();
      return include(buyerId);
    }
    return false;
  }

  // Note: Trade events occur before the change in account balances
  private void trace(Trade trade) {
    long askAccountId = orderService.getAskOrder(trade.getAskOrderId()).getAccountId();
    long bidAccountId = orderService.getBidOrder(trade.getBidOrderId()).getAccountId();
    if (include(askAccountId)) {
      log(getValues(askAccountId, trade, true));
    }
    if (include(bidAccountId)) {
      log(getValues(bidAccountId, trade, false));
    }
  }

  private void trace(Account account, boolean unconfirmed) {
    if (include(account.getId())) {
      log(getValues(account.getId(), unconfirmed));
    }
  }

  private void trace(Account.AccountAsset accountAsset, boolean unconfirmed) {
    if (! include(accountAsset.getAccountId())) {
      return;
    }
    log(getValues(accountAsset.getAccountId(), accountAsset, unconfirmed));
  }


  private void traceBeforeAccept(Block block) {
    long generatorId = block.getGeneratorId();
    if (include(generatorId)) {
      log(getValues(generatorId, block));
    }
  }

  private void trace(Block block) {
    for (Transaction transaction : block.getTransactions()) {
      long senderId = transaction.getSenderId();
      if (include(senderId)) {
        log(getValues(senderId, transaction, false));
        log(getValues(senderId, transaction, transaction.getAttachment(), false));
      }
      long recipientId = transaction.getRecipientId();
      if (include(recipientId)) {
        log(getValues(recipientId, transaction, true));
        log(getValues(recipientId, transaction, transaction.getAttachment(), true));
      } else {
        Attachment attachment = transaction.getAttachment();
        if (include(attachment)) {
          log(getValues(recipientId, transaction, transaction.getAttachment(), true));
        }
      }
    }
  }

  private Map<String,String> lessorGuaranteedBalance(Account account, long lesseeId) {
    Map<String,String> map = new HashMap<>();
    map.put("account", Convert.toUnsignedLong(account.getId()));
    map.put("lessor guaranteed balance", String.valueOf(account.getBalanceNQT()));
    map.put("lessee", Convert.toUnsignedLong(lesseeId));
    map.put("timestamp", String.valueOf(Burst.getBlockchain().getLastBlock().getTimestamp()));
    map.put("height", String.valueOf(Burst.getBlockchain().getHeight()));
    map.put("event", "lessor guaranteed balance");
    return map;
  }

  private Map<String,String> getValues(long accountId, boolean unconfirmed) {
    Map<String,String> map = new HashMap<>();
    map.put("account", Convert.toUnsignedLong(accountId));
    Account account = Account.getAccount(accountId);
    map.put("balance", String.valueOf(account != null ? account.getBalanceNQT() : 0));
    map.put("unconfirmed balance", String.valueOf(account != null ? account.getUnconfirmedBalanceNQT() : 0));
    map.put("timestamp", String.valueOf(Burst.getBlockchain().getLastBlock().getTimestamp()));
    map.put("height", String.valueOf(Burst.getBlockchain().getHeight()));
    map.put("event", unconfirmed ? "unconfirmed balance" : "balance");
    return map;
  }

  private Map<String,String> getValues(long accountId, Trade trade, boolean isAsk) {
    Map<String,String> map = getValues(accountId, false);
    map.put("asset", Convert.toUnsignedLong(trade.getAssetId()));
    map.put("trade quantity", String.valueOf(isAsk ? - trade.getQuantityQNT() : trade.getQuantityQNT()));
    map.put("trade price", String.valueOf(trade.getPriceNQT()));
    long tradeCost = Convert.safeMultiply(trade.getQuantityQNT(), trade.getPriceNQT());
    map.put("trade cost", String.valueOf((isAsk ? tradeCost : - tradeCost)));
    map.put("event", "trade");
    return map;
  }

  private Map<String,String> getValues(long accountId, Transaction transaction, boolean isRecipient) {
    long amount = transaction.getAmountNQT();
    long fee = transaction.getFeeNQT();
    if (isRecipient) {
      fee = 0; // fee doesn't affect recipient account
    } else {
      // for sender the amounts are subtracted
      amount = - amount;
      fee = - fee;
    }
    if (fee == 0 && amount == 0) {
      return Collections.emptyMap();
    }
    Map<String,String> map = getValues(accountId, false);
    map.put("transaction amount", String.valueOf(amount));
    map.put("transaction fee", String.valueOf(fee));
    map.put("transaction", transaction.getStringId());
    if (isRecipient) {
      map.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
    } else {
      map.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
    }
    map.put("event", "transaction");
    return map;
  }

  private Map<String,String> getValues(long accountId, Block block) {
    long fee = block.getTotalFeeNQT();
    if (fee == 0) {
      return Collections.emptyMap();
    }
    Map<String,String> map = getValues(accountId, false);
    map.put("generation fee", String.valueOf(fee));
    map.put("block", block.getStringId());
    map.put("event", "block");
    map.put("timestamp", String.valueOf(block.getTimestamp()));
    map.put("height", String.valueOf(block.getHeight()));
    return map;
  }

  private Map<String,String> getValues(long accountId, Account.AccountAsset accountAsset, boolean unconfirmed) {
    Map<String,String> map = new HashMap<>();
    map.put("account", Convert.toUnsignedLong(accountId));
    map.put("asset", Convert.toUnsignedLong(accountAsset.getAssetId()));
    if (unconfirmed) {
      map.put("unconfirmed asset balance", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
    } else {
      map.put("asset balance", String.valueOf(accountAsset.getQuantityQNT()));
    }
    map.put("timestamp", String.valueOf(Burst.getBlockchain().getLastBlock().getTimestamp()));
    map.put("height", String.valueOf(Burst.getBlockchain().getHeight()));
    map.put("event", "asset balance");
    return map;
  }

  private Map<String,String> getValues(long accountId, Transaction transaction, Attachment attachment, boolean isRecipient) {
    Map<String,String> map = getValues(accountId, false);
    if (attachment instanceof Attachment.ColoredCoinsOrderPlacement) {
      if (isRecipient) {
        return Collections.emptyMap();
      }
      Attachment.ColoredCoinsOrderPlacement orderPlacement = (Attachment.ColoredCoinsOrderPlacement)attachment;
      boolean isAsk = orderPlacement instanceof Attachment.ColoredCoinsAskOrderPlacement;
      map.put("asset", Convert.toUnsignedLong(orderPlacement.getAssetId()));
      map.put("order", transaction.getStringId());
      map.put("order price", String.valueOf(orderPlacement.getPriceNQT()));
      long quantity = orderPlacement.getQuantityQNT();
      if (isAsk) {
        quantity = - quantity;
      }
      map.put("order quantity", String.valueOf(quantity));
      BigInteger orderCost = BigInteger.valueOf(orderPlacement.getPriceNQT()).multiply(BigInteger.valueOf(orderPlacement.getQuantityQNT()));
      if (! isAsk) {
        orderCost = orderCost.negate();
      }
      map.put("order cost", orderCost.toString());
      String event = (isAsk ? "ask" : "bid") + " order";
      map.put("event", event);
    }
    else if (attachment instanceof Attachment.ColoredCoinsAssetIssuance) {
      if (isRecipient) {
        return Collections.emptyMap();
      }
      Attachment.ColoredCoinsAssetIssuance assetIssuance = (Attachment.ColoredCoinsAssetIssuance)attachment;
      map.put("asset", transaction.getStringId());
      map.put("asset quantity", String.valueOf(assetIssuance.getQuantityQNT()));
      map.put("event", "asset issuance");
    }
    else if (attachment instanceof Attachment.ColoredCoinsAssetTransfer) {
      Attachment.ColoredCoinsAssetTransfer assetTransfer = (Attachment.ColoredCoinsAssetTransfer)attachment;
      map.put("asset", Convert.toUnsignedLong(assetTransfer.getAssetId()));
      long quantity = assetTransfer.getQuantityQNT();
      if (! isRecipient) {
        quantity = - quantity;
      }
      map.put("asset quantity", String.valueOf(quantity));
      map.put("event", "asset transfer");
    }
    else if (attachment instanceof Attachment.ColoredCoinsOrderCancellation) {
      Attachment.ColoredCoinsOrderCancellation orderCancellation = (Attachment.ColoredCoinsOrderCancellation)attachment;
      map.put("order", Convert.toUnsignedLong(orderCancellation.getOrderId()));
      map.put("event", "order cancel");
    }
    else if (attachment instanceof Attachment.DigitalGoodsPurchase) {
      Attachment.DigitalGoodsPurchase purchase = (Attachment.DigitalGoodsPurchase)transaction.getAttachment();
      if (isRecipient) {
        map = getValues(dgsGoodsStoreService.getGoods(purchase.getGoodsId()).getSellerId(), false);
      }
      map.put("event", "purchase");
      map.put("purchase", transaction.getStringId());
    }
    else if (attachment instanceof Attachment.DigitalGoodsDelivery) {
      Attachment.DigitalGoodsDelivery delivery = (Attachment.DigitalGoodsDelivery)transaction.getAttachment();
      DigitalGoodsStore.Purchase purchase = dgsGoodsStoreService.getPurchase(delivery.getPurchaseId());
      if (isRecipient) {
        map = getValues(purchase.getBuyerId(), false);
      }
      map.put("event", "delivery");
      map.put("purchase", Convert.toUnsignedLong(delivery.getPurchaseId()));
      long discount = delivery.getDiscountNQT();
      map.put("purchase price", String.valueOf(purchase.getPriceNQT()));
      map.put("purchase quantity", String.valueOf(purchase.getQuantity()));
      long cost = Convert.safeMultiply(purchase.getPriceNQT(), purchase.getQuantity());
      if (isRecipient) {
        cost = - cost;
      }
      map.put("purchase cost", String.valueOf(cost));
      if (! isRecipient) {
        discount = - discount;
      }
      map.put("discount", String.valueOf(discount));
    }
    else if (attachment instanceof Attachment.DigitalGoodsRefund) {
      Attachment.DigitalGoodsRefund refund = (Attachment.DigitalGoodsRefund)transaction.getAttachment();
      if (isRecipient) {
        map = getValues(dgsGoodsStoreService.getPurchase(refund.getPurchaseId()).getBuyerId(), false);
      }
      map.put("event", "refund");
      map.put("purchase", Convert.toUnsignedLong(refund.getPurchaseId()));
      long refundNQT = refund.getRefundNQT();
      if (! isRecipient) {
        refundNQT = - refundNQT;
      }
      map.put("refund", String.valueOf(refundNQT));
    }
    else if (attachment == Attachment.ARBITRARY_MESSAGE) {
      map = new HashMap<>();
      map.put("account", Convert.toUnsignedLong(accountId));
      map.put("timestamp", String.valueOf(Burst.getBlockchain().getLastBlock().getTimestamp()));
      map.put("height", String.valueOf(Burst.getBlockchain().getHeight()));
      map.put("event", "message");
      if (isRecipient) {
        map.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
      }
      else {
        map.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
      }
    }
    else {
      return Collections.emptyMap();
    }
    return map;
  }

  private void log(Map<String,String> map) {
    if (map.isEmpty()) {
      return;
    }
    StringBuilder buf = new StringBuilder();
    for (String column : columns) {
      if (!LOG_UNCONFIRMED && column.startsWith("unconfirmed")) {
        continue;
      }
      String value = map.get(column);
      if (value != null) {
        buf.append(QUOTE).append(value).append(QUOTE);
      }
      buf.append(SEPARATOR);
    }
    log.println(buf.toString());
  }

}
