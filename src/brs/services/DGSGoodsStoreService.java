package brs.services;

import brs.Appendix;
import brs.Attachment;
import brs.DigitalGoodsStore.Event;
import brs.DigitalGoodsStore.Goods;
import brs.DigitalGoodsStore.Purchase;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.util.Listener;

public interface DGSGoodsStoreService {

  boolean addGoodsListener(Listener<Goods> listener, Event eventType);

  boolean removeGoodsListener(Listener<Goods> listener, Event eventType);

  boolean addPurchaseListener(Listener<Purchase> listener, Event eventType);

  boolean removePurchaseListener(Listener<Purchase> listener, Event eventType);

  Goods getGoods(long goodsId);

  BurstIterator<Goods> getAllGoods(int from, int to);

  BurstIterator<Goods> getGoodsInStock(int from, int to);

  BurstIterator<Goods> getSellerGoods(long sellerId, boolean inStockOnly, int from, int to);

  BurstIterator<Purchase> getAllPurchases(int from, int to);

  BurstIterator<Purchase> getSellerPurchases(long sellerId, int from, int to);

  BurstIterator<Purchase> getBuyerPurchases(long buyerId, int from, int to);

  BurstIterator<Purchase> getSellerBuyerPurchases(long sellerId, long buyerId, int from, int to);

  BurstIterator<Purchase> getPendingSellerPurchases(long sellerId, int from, int to);

  Purchase getPurchase(long purchaseId);

  void changeQuantity(long goodsId, int deltaQuantity, boolean allowDelisted);

  void purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment);

  void addPurchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, long sellerId);

  void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment);

  void delistGoods(long goodsId);

  void feedback(long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message);

  void refund(long sellerId, long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage);

  BurstIterator<Purchase> getExpiredPendingPurchases(int timestamp);

  void changePrice(long goodsId, long priceNQT);

  void deliver(Transaction transaction, Attachment.DigitalGoodsDelivery attachment);

  Purchase getPendingPurchase(long purchaseId);

  void setPending(Purchase purchase, boolean pendingValue);
}
