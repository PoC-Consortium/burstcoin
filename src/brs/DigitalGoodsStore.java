package brs;

import brs.crypto.EncryptedData;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.VersionedValuesTable;

import java.util.ArrayList;
import java.util.List;

public final class DigitalGoodsStore {

  public enum Event {
    GOODS_LISTED, GOODS_DELISTED, GOODS_PRICE_CHANGE, GOODS_QUANTITY_CHANGE,
    PURCHASE, DELIVERY, REFUND, FEEDBACK
  }

  public static class Goods {

    private static final BurstKey.LongKeyFactory<Goods> goodsDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getGoodsDbKeyFactory();
    }

    private static final VersionedEntityTable<Goods> goodsTable() {
      return Burst.getStores().getDigitalGoodsStoreStore().getGoodsTable();
    }

    private final long id;
    public final BurstKey dbKey;
    private final long sellerId;
    private final String name;
    private final String description;
    private final String tags;
    private final int timestamp;
    private int quantity;
    private long priceNQT;
    private boolean delisted;

    protected Goods(long id, BurstKey dbKey, long sellerId, String name, String description, String tags, int timestamp,
                    int quantity, long priceNQT, boolean delisted) {
      this.id = id;
      this.dbKey = dbKey;
      this.sellerId = sellerId;
      this.name = name;
      this.description = description;
      this.tags = tags;
      this.timestamp = timestamp;
      this.quantity = quantity;
      this.priceNQT = priceNQT;
      this.delisted = delisted;
    }

    public Goods(BurstKey dbKey, Transaction transaction, Attachment.DigitalGoodsListing attachment) {
      this.dbKey = dbKey;
      this.id = transaction.getId();
      this.sellerId = transaction.getSenderId();
      this.name = attachment.getName();
      this.description = attachment.getDescription();
      this.tags = attachment.getTags();
      this.quantity = attachment.getQuantity();
      this.priceNQT = attachment.getPriceNQT();
      this.delisted = false;
      this.timestamp = transaction.getTimestamp();
    }

    public long getId() {
      return id;
    }

    public long getSellerId() {
      return sellerId;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public String getTags() {
      return tags;
    }

    public int getTimestamp() {
      return timestamp;
    }

    public int getQuantity() {
      return quantity;
    }

    public void changeQuantity(int deltaQuantity) {
      quantity += deltaQuantity;
      if (quantity < 0) {
        quantity = 0;
      } else if (quantity > Constants.MAX_DGS_LISTING_QUANTITY) {
        quantity = Constants.MAX_DGS_LISTING_QUANTITY;
      }
    }

    public long getPriceNQT() {
      return priceNQT;
    }

    public void changePrice(long priceNQT) {
      this.priceNQT = priceNQT;
    }

    public boolean isDelisted() {
      return delisted;
    }

    public void setDelisted(boolean delisted) {
      this.delisted = delisted;
    }

  }

  public static class Purchase {

    private static final BurstKey.LongKeyFactory<Purchase> purchaseDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getPurchaseDbKeyFactory();
    }

    private static final BurstKey.LongKeyFactory<Purchase> feedbackDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getFeedbackDbKeyFactory();
    }

    private static final VersionedValuesTable<Purchase, EncryptedData> feedbackTable() {
      return Burst.getStores().getDigitalGoodsStoreStore().getFeedbackTable();
    }

    private static final BurstKey.LongKeyFactory<Purchase> publicFeedbackDbKeyFactory() {
      return Burst.getStores().getDigitalGoodsStoreStore().getPublicFeedbackDbKeyFactory();
    }

    private static final VersionedValuesTable<Purchase, String> publicFeedbackTable() {
      return Burst.getStores().getDigitalGoodsStoreStore().getPublicFeedbackTable();
    }

    private final long id;
    public final BurstKey dbKey;
    private final long buyerId;
    private final long goodsId;
    private final long sellerId;
    private final int quantity;
    private final long priceNQT;
    private final int deadline;
    private final EncryptedData note;
    private final int timestamp;
    private boolean isPending;
    private EncryptedData encryptedGoods;
    private boolean goodsIsText;
    private EncryptedData refundNote;
    private boolean hasFeedbackNotes;
    private List<EncryptedData> feedbackNotes;
    private boolean hasPublicFeedbacks;
    private List<String> publicFeedbacks;
    private long discountNQT;
    private long refundNQT;

    public Purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, long sellerId) {
      this.id = transaction.getId();
      this.dbKey = purchaseDbKeyFactory().newKey(this.id);
      this.buyerId = transaction.getSenderId();
      this.goodsId = attachment.getGoodsId();
      this.sellerId = sellerId;
      this.quantity = attachment.getQuantity();
      this.priceNQT = attachment.getPriceNQT();
      this.deadline = attachment.getDeliveryDeadlineTimestamp();
      this.note = transaction.getEncryptedMessage() == null ? null : transaction.getEncryptedMessage().getEncryptedData();
      this.timestamp = transaction.getTimestamp();
      this.isPending = true;
    }

    protected Purchase(long id, BurstKey dbKey, long buyerId, long goodsId, long sellerId, int quantity,
                       long priceNQT, int deadline, EncryptedData note, int timestamp, boolean isPending,
                       EncryptedData encryptedGoods,EncryptedData refundNote,
                       boolean hasFeedbackNotes, boolean hasPublicFeedbacks,
                       long discountNQT, long refundNQT) {
      this.id = id;
      this.dbKey = dbKey;
      this.buyerId = buyerId;
      this.goodsId = goodsId;
      this.sellerId = sellerId;
      this.quantity = quantity;
      this.priceNQT = priceNQT;
      this.deadline = deadline;
      this.note = note;
      this.timestamp = timestamp;
      this.isPending = isPending;
      this.encryptedGoods = encryptedGoods;
      this.refundNote = refundNote;
      this.hasFeedbackNotes = hasFeedbackNotes;
      this.hasPublicFeedbacks = hasPublicFeedbacks;
      this.discountNQT = discountNQT;
      this.refundNQT = refundNQT;
    }

    public long getId() {
      return id;
    }

    public long getBuyerId() {
      return buyerId;
    }

    public long getGoodsId() {
      return goodsId;
    }

    public long getSellerId() { return sellerId; }

    public int getQuantity() {
      return quantity;
    }

    public long getPriceNQT() {
      return priceNQT;
    }

    public int getDeliveryDeadlineTimestamp() {
      return deadline;
    }

    public EncryptedData getNote() {
      return note;
    }

    public boolean isPending() {
      return isPending;
    }

    public void setPending(boolean isPending) {
      this.isPending = isPending;

    }

    public int getTimestamp() {
      return timestamp;
    }

    public String getName() {
      return getGoods(goodsId).getName();
    }

    public EncryptedData getEncryptedGoods() {
      return encryptedGoods;
    }

    public boolean goodsIsText() {
      return goodsIsText;
    }

    public void setEncryptedGoods(EncryptedData encryptedGoods, boolean goodsIsText) {
      this.encryptedGoods = encryptedGoods;
      this.goodsIsText = goodsIsText;
    }

    public EncryptedData getRefundNote() {
      return refundNote;
    }

    public void setRefundNote(EncryptedData refundNote) {
      this.refundNote = refundNote;
    }

    public List<EncryptedData> getFeedbackNotes() {
      if (!hasFeedbackNotes) {
        return null;
      }
      feedbackNotes = feedbackTable().get(feedbackDbKeyFactory().newKey(this));
      return feedbackNotes;
    }

    public void addFeedbackNote(EncryptedData feedbackNote) {
      if (feedbackNotes == null) {
        feedbackNotes = new ArrayList<>();
      }
      feedbackNotes.add(feedbackNote);
      this.hasFeedbackNotes = true;
    }

    public List<String> getPublicFeedback() {
      if (!hasPublicFeedbacks) {
        return null;
      }
      publicFeedbacks =  publicFeedbackTable().get(publicFeedbackDbKeyFactory().newKey(this));
      return publicFeedbacks;
    }

    public long getDiscountNQT() {
      return discountNQT;
    }

    public void setDiscountNQT(long discountNQT) {
      this.discountNQT = discountNQT;
    }

    public long getRefundNQT() {
      return refundNQT;
    }

    public void setRefundNQT(long refundNQT) {
      this.refundNQT = refundNQT;
    }

    public List<String> getPublicFeedbacks() {
      return publicFeedbacks;
    }

    public void setHasPublicFeedbacks(boolean hasPublicFeedbacks) {
      this.hasPublicFeedbacks = hasPublicFeedbacks;
    }
  }

  public static Goods getGoods(long goodsId) {
    return Goods.goodsTable().get(Goods.goodsDbKeyFactory().newKey(goodsId));
  }

}
