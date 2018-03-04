package brs.services.impl;

import brs.Account;
import brs.Appendix;
import brs.Attachment;
import brs.Blockchain;
import brs.DigitalGoodsStore.Event;
import brs.DigitalGoodsStore.Goods;
import brs.DigitalGoodsStore.Purchase;
import brs.Transaction;
import brs.crypto.EncryptedData;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedEntityTable;
import brs.db.VersionedValuesTable;
import brs.db.store.DigitalGoodsStoreStore;
import brs.services.AccountService;
import brs.services.DGSGoodsStoreService;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Listeners;
import java.util.ArrayList;
import java.util.List;

public class DGSGoodsStoreServiceImpl implements DGSGoodsStoreService {

  private final Blockchain blockchain;
  private final DigitalGoodsStoreStore digitalGoodsStoreStore;
  private final AccountService accountService;
  private final VersionedValuesTable<Purchase, EncryptedData> feedbackTable;
  private final VersionedValuesTable<Purchase, String> publicFeedbackTable;

  private VersionedEntityTable<Goods> goodsTable;
  private final VersionedEntityTable<Purchase> purchaseTable;
  private final LongKeyFactory<Goods> goodsDbKeyFactory;
  private final LongKeyFactory<Purchase> purchaseDbKeyFactory;

  private final Listeners<Goods,Event> goodsListeners = new Listeners<>();

  private final Listeners<Purchase,Event> purchaseListeners = new Listeners<>();

  public DGSGoodsStoreServiceImpl(Blockchain blockchain, DigitalGoodsStoreStore digitalGoodsStoreStore, AccountService accountService) {
    this.blockchain = blockchain;
    this.digitalGoodsStoreStore = digitalGoodsStoreStore;
    this.goodsTable = digitalGoodsStoreStore.getGoodsTable();
    this.purchaseTable = digitalGoodsStoreStore.getPurchaseTable();
    this.goodsDbKeyFactory = digitalGoodsStoreStore.getGoodsDbKeyFactory();
    this.purchaseDbKeyFactory = digitalGoodsStoreStore.getPurchaseDbKeyFactory();
    this.feedbackTable = digitalGoodsStoreStore.getFeedbackTable();
    this.publicFeedbackTable = digitalGoodsStoreStore.getPublicFeedbackTable();

    this.accountService = accountService;
  }

  @Override
  public boolean addGoodsListener(Listener<Goods> listener, Event eventType) {
    return goodsListeners.addListener(listener, eventType);
  }

  @Override
  public boolean removeGoodsListener(Listener<Goods> listener, Event eventType) {
    return goodsListeners.removeListener(listener, eventType);
  }

  @Override
  public boolean addPurchaseListener(Listener<Purchase> listener, Event eventType) {
    return purchaseListeners.addListener(listener, eventType);
  }

  @Override
  public boolean removePurchaseListener(Listener<Purchase> listener, Event eventType) {
    return purchaseListeners.removeListener(listener, eventType);
  }

  @Override
  public Goods getGoods(long goodsId) {
    return goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
  }

  @Override
  public BurstIterator<Goods> getAllGoods(int from, int to) {
    return goodsTable.getAll(from, to);
  }

  @Override
  public BurstIterator<Goods> getGoodsInStock(int from, int to) {
    return digitalGoodsStoreStore.getGoodsInStock(from, to);
  }

  @Override
  public BurstIterator<Goods> getSellerGoods(final long sellerId, final boolean inStockOnly, int from, int to) {
    return digitalGoodsStoreStore.getSellerGoods(sellerId, inStockOnly, from, to);
  }

  @Override
  public BurstIterator<Purchase> getAllPurchases(int from, int to) {
    return purchaseTable.getAll(from, to);
  }

  @Override
  public BurstIterator<Purchase> getSellerPurchases(long sellerId, int from, int to) {
    return digitalGoodsStoreStore.getSellerPurchases(sellerId, from, to);
  }

  @Override
  public BurstIterator<Purchase> getBuyerPurchases(long buyerId, int from, int to) {
    return digitalGoodsStoreStore.getBuyerPurchases(buyerId, from, to);
  }

  @Override
  public BurstIterator<Purchase> getSellerBuyerPurchases(final long sellerId, final long buyerId, int from, int to) {
    return digitalGoodsStoreStore.getSellerBuyerPurchases(sellerId, buyerId, from, to);
  }

  @Override
  public BurstIterator<Purchase> getPendingSellerPurchases(final long sellerId, int from, int to) {
    return digitalGoodsStoreStore.getPendingSellerPurchases(sellerId, from, to);
  }

  @Override
  public Purchase getPurchase(long purchaseId) {
    return purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
  }

  @Override
  public void changeQuantity(long goodsId, int deltaQuantity, boolean allowDelisted) {
    Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
    if (allowDelisted || ! goods.isDelisted()) {
      goods.changeQuantity(deltaQuantity);
      goodsTable.insert(goods);
      goodsListeners.notify(goods, Event.GOODS_QUANTITY_CHANGE);
    } else {
      throw new IllegalStateException("Can't change quantity of delisted goods");
    }
  }

  @Override
  public void purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment) {
    Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(attachment.getGoodsId()));
    if (! goods.isDelisted() && attachment.getQuantity() <= goods.getQuantity() && attachment.getPriceNQT() == goods.getPriceNQT()
        && attachment.getDeliveryDeadlineTimestamp() > blockchain.getLastBlock().getTimestamp()) {
      changeQuantity(goods.getId(), -attachment.getQuantity(), false);
      addPurchase(transaction, attachment, goods.getSellerId());
    } else {
      Account buyer = accountService.getAccount(transaction.getSenderId());
      accountService.addToUnconfirmedBalanceNQT(buyer, Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
      // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
    }
  }

  @Override
  public void addPurchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, long sellerId) {
    Purchase purchase = new Purchase(transaction, attachment, sellerId);
    purchaseTable.insert(purchase);
    purchaseListeners.notify(purchase, Event.PURCHASE);
  }

  @Override
  public void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
    BurstKey dbKey = goodsDbKeyFactory.newKey(transaction.getId());
    Goods goods = new Goods(dbKey, transaction, attachment);
    goodsTable.insert(goods);
    goodsListeners.notify(goods, Event.GOODS_LISTED);
  }

  @Override
  public void delistGoods(long goodsId) {
    Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
    if (! goods.isDelisted()) {
      goods.setDelisted(true);
      goodsTable.insert(goods);
      goodsListeners.notify(goods, Event.GOODS_DELISTED);
    } else {
      throw new IllegalStateException("Goods already delisted");
    }
  }

  @Override
  public void feedback(long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message) {
    Purchase purchase = purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
    if (encryptedMessage != null) {
      purchase.addFeedbackNote(encryptedMessage.getEncryptedData());
      purchaseTable.insert(purchase);
      feedbackTable.insert(purchase, purchase.getFeedbackNotes());
    }
    if (message != null) {
      addPublicFeedback(purchase, Convert.toString(message.getMessage()));
    }
    purchaseListeners.notify(purchase, Event.FEEDBACK);
  }

  private void addPublicFeedback(Purchase purchase, String publicFeedback) {
    List<String> publicFeedbacks = purchase.getPublicFeedbacks();
    if (publicFeedbacks == null) {
      publicFeedbacks = new ArrayList<>();
    }
    publicFeedbacks.add(publicFeedback);
    purchase.setHasPublicFeedbacks(true);
    purchaseTable.insert(purchase);
    publicFeedbackTable.insert(purchase, publicFeedbacks);
  }

  @Override
  public void refund(long sellerId, long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage) {
    Purchase purchase = purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
    Account seller = accountService.getAccount(sellerId);
    accountService.addToBalanceNQT(seller, -refundNQT);
    Account buyer = accountService.getAccount(purchase.getBuyerId());
    accountService.addToBalanceAndUnconfirmedBalanceNQT(buyer, refundNQT);
    if (encryptedMessage != null) {
      purchase.setRefundNote(encryptedMessage.getEncryptedData());
      purchaseTable.insert(purchase);
    }
    purchase.setRefundNQT(refundNQT);
    purchaseTable.insert(purchase);
    purchaseListeners.notify(purchase, Event.REFUND);
  }

  @Override
  public BurstIterator<Purchase> getExpiredPendingPurchases(final int timestamp) {
    return digitalGoodsStoreStore.getExpiredPendingPurchases(timestamp);
  }

  @Override
  public void changePrice(long goodsId, long priceNQT) {
    Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
    if (! goods.isDelisted()) {
      goods.changePrice(priceNQT);
      goodsTable.insert(goods);
      goodsListeners.notify(goods, Event.GOODS_PRICE_CHANGE);
    } else {
      throw new IllegalStateException("Can't change price of delisted goods");
    }
  }

  @Override
  public void deliver(Transaction transaction, Attachment.DigitalGoodsDelivery attachment) {
    Purchase purchase = getPendingPurchase(attachment.getPurchaseId());
    if ( purchase == null ) {
      throw new RuntimeException("cant find purchase with id " + attachment.getPurchaseId());
    }
    setPending(purchase, false);
    long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
    Account buyer = accountService.getAccount(purchase.getBuyerId());
    accountService.addToBalanceNQT(buyer, Convert.safeSubtract(attachment.getDiscountNQT(), totalWithoutDiscount));
    accountService.addToUnconfirmedBalanceNQT(buyer, attachment.getDiscountNQT());
    Account seller = accountService.getAccount(transaction.getSenderId());
    accountService.addToBalanceAndUnconfirmedBalanceNQT(seller, Convert.safeSubtract(totalWithoutDiscount, attachment.getDiscountNQT()));
    purchase.setEncryptedGoods(attachment.getGoods(), attachment.goodsIsText());
    purchaseTable.insert(purchase);
    purchase.setDiscountNQT(attachment.getDiscountNQT());
    purchaseTable.insert(purchase);
    purchaseListeners.notify(purchase, Event.DELIVERY);
  }

  @Override
  public Purchase getPendingPurchase(long purchaseId) {
    Purchase purchase = getPurchase(purchaseId);
    return purchase == null || ! purchase.isPending() ? null : purchase;
  }

  @Override
  public void setPending(Purchase purchase, boolean pendingValue) {
    purchase.setPending(pendingValue);
    purchaseTable.insert(purchase);
  }

}
