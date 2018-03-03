package brs;

import brs.crypto.EncryptedData;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public interface Attachment extends Appendix {

  TransactionType getTransactionType();

  abstract class AbstractAttachment extends AbstractAppendix implements Attachment {

    private AbstractAttachment(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
    }

    private AbstractAttachment(JSONObject attachmentData) {
      super(attachmentData);
    }

    private AbstractAttachment(byte version) {
      super(version);
    }

    private AbstractAttachment(int blockchainHeight) {
      super(blockchainHeight);
    }

    @Override
    public final void validate(Transaction transaction) throws BurstException.ValidationException {
      getTransactionType().validateAttachment(transaction);
    }

    @Override
    public final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
      getTransactionType().apply(transaction, senderAccount, recipientAccount);
    }

  }

  abstract class EmptyAttachment extends AbstractAttachment {

    private EmptyAttachment() {
      super((byte) 0);
    }

    @Override
    final int getMySize() {
      return 0;
    }

    @Override
    final void putMyBytes(ByteBuffer buffer) {
    }

    @Override
    final void putMyJSON(JSONObject json) {
    }

    @Override
    final boolean verifyVersion(byte transactionVersion) {
      return true;
    }

  }

  EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

      @Override
      String getAppendixName() {
        return "OrdinaryPayment";
      }

      @Override
      public TransactionType getTransactionType() {
        return TransactionType.Payment.ORDINARY;
      }

    };

  // the message payload is in the Appendix
  EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

      @Override
      String getAppendixName() {
        return "ArbitraryMessage";
      }

      @Override
      public TransactionType getTransactionType() {
        return TransactionType.Messaging.ARBITRARY_MESSAGE;
      }

    };

  EmptyAttachment AT_PAYMENT = new EmptyAttachment() {

      @Override
      public TransactionType getTransactionType() {
        return TransactionType.AutomatedTransactions.AT_PAYMENT;
      }

      @Override
      String getAppendixName() {
        return "AT Payment";
      }


    };

  class MessagingAliasAssignment extends AbstractAttachment {

    private final String aliasName;
    private final String aliasURI;

    MessagingAliasAssignment(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim();
      aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim();
    }

    MessagingAliasAssignment(JSONObject attachmentData) {
      super(attachmentData);
      aliasName = (Convert.nullToEmpty((String) attachmentData.get("alias"))).trim();
      aliasURI = (Convert.nullToEmpty((String) attachmentData.get("uri"))).trim();
    }

    public MessagingAliasAssignment(String aliasName, String aliasURI, int blockchainHeight) {
      super(blockchainHeight);
      this.aliasName = aliasName.trim();
      this.aliasURI = aliasURI.trim();
    }

    @Override
    String getAppendixName() {
      return "AliasAssignment";
    }

    @Override
    int getMySize() {
      return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      byte[] alias = Convert.toBytes(this.aliasName);
      byte[] uri = Convert.toBytes(this.aliasURI);
      buffer.put((byte)alias.length);
      buffer.put(alias);
      buffer.putShort((short) uri.length);
      buffer.put(uri);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("alias", aliasName);
      attachment.put("uri", aliasURI);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.Messaging.ALIAS_ASSIGNMENT;
    }

    public String getAliasName() {
      return aliasName;
    }

    public String getAliasURI() {
      return aliasURI;
    }
  }

  class MessagingAliasSell extends AbstractAttachment {

    private final String aliasName;
    private final long priceNQT;

    MessagingAliasSell(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
      this.priceNQT = buffer.getLong();
    }

    MessagingAliasSell(JSONObject attachmentData) {
      super(attachmentData);
      this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
      this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
    }

    public MessagingAliasSell(String aliasName, long priceNQT, int blockchainHeight) {
      super(blockchainHeight);
      this.aliasName = aliasName;
      this.priceNQT = priceNQT;
    }

    @Override
    String getAppendixName() {
      return "AliasSell";
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.Messaging.ALIAS_SELL;
    }

    @Override
    int getMySize() {
      return 1 + Convert.toBytes(aliasName).length + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      byte[] aliasBytes = Convert.toBytes(aliasName);
      buffer.put((byte)aliasBytes.length);
      buffer.put(aliasBytes);
      buffer.putLong(priceNQT);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("alias", aliasName);
      attachment.put("priceNQT", priceNQT);
    }

    public String getAliasName(){
      return aliasName;
    }

    public long getPriceNQT(){
      return priceNQT;
    }
  }

  final class MessagingAliasBuy extends AbstractAttachment {

    private final String aliasName;

    MessagingAliasBuy(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
    }

    MessagingAliasBuy(JSONObject attachmentData) {
      super(attachmentData);
      this.aliasName = Convert.nullToEmpty((String) attachmentData.get("alias"));
    }

    public MessagingAliasBuy(String aliasName, int blockchainHeight) {
      super(blockchainHeight);
      this.aliasName = aliasName;
    }

    @Override
    String getAppendixName() {
      return "AliasBuy";
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.Messaging.ALIAS_BUY;
    }

    @Override
    int getMySize() {
      return 1 + Convert.toBytes(aliasName).length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      byte[] aliasBytes = Convert.toBytes(aliasName);
      buffer.put((byte)aliasBytes.length);
      buffer.put(aliasBytes);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("alias", aliasName);
    }

    public String getAliasName(){
      return aliasName;
    }
  }

  final class MessagingAccountInfo extends AbstractAttachment {

    private final String name;
    private final String description;

    MessagingAccountInfo(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
      this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
    }

    MessagingAccountInfo(JSONObject attachmentData) {
      super(attachmentData);
      this.name = Convert.nullToEmpty((String) attachmentData.get("name"));
      this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
    }

    public MessagingAccountInfo(String name, String description, int blockchainHeight) {
      super(blockchainHeight);
      this.name = name;
      this.description = description;
    }

    @Override
    String getAppendixName() {
      return "AccountInfo";
    }

    @Override
    int getMySize() {
      return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      byte[] putName = Convert.toBytes(this.name);
      byte[] putDescription = Convert.toBytes(this.description);
      buffer.put((byte)putName.length);
      buffer.put(putName);
      buffer.putShort((short) putDescription.length);
      buffer.put(putDescription);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("name", name);
      attachment.put("description", description);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.Messaging.ACCOUNT_INFO;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

  }

  final class ColoredCoinsAssetIssuance extends AbstractAttachment {

    private final String name;
    private final String description;
    private final long quantityQNT;
    private final byte decimals;

    ColoredCoinsAssetIssuance(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
      this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
      this.quantityQNT = buffer.getLong();
      this.decimals = buffer.get();
    }

    ColoredCoinsAssetIssuance(JSONObject attachmentData) {
      super(attachmentData);
      this.name = (String) attachmentData.get("name");
      this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
      this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
      this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
    }

    public ColoredCoinsAssetIssuance(String name, String description, long quantityQNT, byte decimals, int blockchainHeight) {
      super(blockchainHeight);
      this.name = name;
      this.description = Convert.nullToEmpty(description);
      this.quantityQNT = quantityQNT;
      this.decimals = decimals;
    }

    @Override
    String getAppendixName() {
      return "AssetIssuance";
    }

    @Override
    int getMySize() {
      return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      byte[] name = Convert.toBytes(this.name);
      byte[] description = Convert.toBytes(this.description);
      buffer.put((byte)name.length);
      buffer.put(name);
      buffer.putShort((short) description.length);
      buffer.put(description);
      buffer.putLong(quantityQNT);
      buffer.put(decimals);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("name", name);
      attachment.put("description", description);
      attachment.put("quantityQNT", quantityQNT);
      attachment.put("decimals", decimals);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.ColoredCoins.ASSET_ISSUANCE;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public long getQuantityQNT() {
      return quantityQNT;
    }

    public byte getDecimals() {
      return decimals;
    }
  }

  final class ColoredCoinsAssetTransfer extends AbstractAttachment {

    private final long assetId;
    private final long quantityQNT;
    private final String comment;

    ColoredCoinsAssetTransfer(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.assetId = buffer.getLong();
      this.quantityQNT = buffer.getLong();
      this.comment = getVersion() == 0 ? Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) : null;
    }

    ColoredCoinsAssetTransfer(JSONObject attachmentData) {
      super(attachmentData);
      this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
      this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
      this.comment = getVersion() == 0 ? Convert.nullToEmpty((String) attachmentData.get("comment")) : null;
    }

    public ColoredCoinsAssetTransfer(long assetId, long quantityQNT, int blockchainHeight) {
      super(blockchainHeight);
      this.assetId = assetId;
      this.quantityQNT = quantityQNT;
      this.comment = null;
    }

    @Override
    String getAppendixName() {
      return "AssetTransfer";
    }

    @Override
    int getMySize() {
      return 8 + 8 + (getVersion() == 0 ? (2 + Convert.toBytes(comment).length) : 0);
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(assetId);
      buffer.putLong(quantityQNT);
      if (getVersion() == 0 && comment != null) {
        byte[] commentBytes = Convert.toBytes(this.comment);
        buffer.putShort((short) commentBytes.length);
        buffer.put(commentBytes);
      }
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("asset", Convert.toUnsignedLong(assetId));
      attachment.put("quantityQNT", quantityQNT);
      if (getVersion() == 0) {
        attachment.put("comment", comment);
      }
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.ColoredCoins.ASSET_TRANSFER;
    }

    public long getAssetId() {
      return assetId;
    }

    public long getQuantityQNT() {
      return quantityQNT;
    }

    public String getComment() {
      return comment;
    }

  }

  abstract class ColoredCoinsOrderPlacement extends AbstractAttachment {

    private final long assetId;
    private final long quantityQNT;
    private final long priceNQT;

    private ColoredCoinsOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.assetId = buffer.getLong();
      this.quantityQNT = buffer.getLong();
      this.priceNQT = buffer.getLong();
    }

    private ColoredCoinsOrderPlacement(JSONObject attachmentData) {
      super(attachmentData);
      this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
      this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
      this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
    }

    private ColoredCoinsOrderPlacement(long assetId, long quantityQNT, long priceNQT, int blockchainHeight) {
      super(blockchainHeight);
      this.assetId = assetId;
      this.quantityQNT = quantityQNT;
      this.priceNQT = priceNQT;
    }

    @Override
    int getMySize() {
      return 8 + 8 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(assetId);
      buffer.putLong(quantityQNT);
      buffer.putLong(priceNQT);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("asset", Convert.toUnsignedLong(assetId));
      attachment.put("quantityQNT", quantityQNT);
      attachment.put("priceNQT", priceNQT);
    }

    public long getAssetId() {
      return assetId;
    }

    public long getQuantityQNT() {
      return quantityQNT;
    }

    public long getPriceNQT() {
      return priceNQT;
    }
  }

  final class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

    ColoredCoinsAskOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
    }

    ColoredCoinsAskOrderPlacement(JSONObject attachmentData) {
      super(attachmentData);
    }

    public ColoredCoinsAskOrderPlacement(long assetId, long quantityQNT, long priceNQT, int blockchainHeight) {
      super(assetId, quantityQNT, priceNQT, blockchainHeight);
    }

    @Override
    String getAppendixName() {
      return "AskOrderPlacement";
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
    }

  }

  final class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

    ColoredCoinsBidOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
    }

    ColoredCoinsBidOrderPlacement(JSONObject attachmentData) {
      super(attachmentData);
    }

    public ColoredCoinsBidOrderPlacement(long assetId, long quantityQNT, long priceNQT, int blockchainHeight) {
      super(assetId, quantityQNT, priceNQT, blockchainHeight);
    }

    @Override
    String getAppendixName() {
      return "BidOrderPlacement";
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
    }

  }

  abstract class ColoredCoinsOrderCancellation extends AbstractAttachment {

    private final long orderId;

    private ColoredCoinsOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.orderId = buffer.getLong();
    }

    private ColoredCoinsOrderCancellation(JSONObject attachmentData) {
      super(attachmentData);
      this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
    }

    private ColoredCoinsOrderCancellation(long orderId, int blockchainHeight) {
      super(blockchainHeight);
      this.orderId = orderId;
    }

    @Override
    int getMySize() {
      return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(orderId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("order", Convert.toUnsignedLong(orderId));
    }

    public long getOrderId() {
      return orderId;
    }
  }

  final class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

    ColoredCoinsAskOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
    }

    ColoredCoinsAskOrderCancellation(JSONObject attachmentData) {
      super(attachmentData);
    }

    public ColoredCoinsAskOrderCancellation(long orderId, int blockchainHeight) {
      super(orderId, blockchainHeight);
    }

    @Override
    String getAppendixName() {
      return "AskOrderCancellation";
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
    }

  }

  final class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

    ColoredCoinsBidOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
    }

    ColoredCoinsBidOrderCancellation(JSONObject attachmentData) {
      super(attachmentData);
    }

    public ColoredCoinsBidOrderCancellation(long orderId, int blockchainHeight) {
      super(orderId, blockchainHeight);
    }

    @Override
    String getAppendixName() {
      return "BidOrderCancellation";
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
    }

  }

  final class DigitalGoodsListing extends AbstractAttachment {

    private final String name;
    private final String description;
    private final String tags;
    private final int quantity;
    private final long priceNQT;

    DigitalGoodsListing(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.name = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
      this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
      this.tags = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
      this.quantity = buffer.getInt();
      this.priceNQT = buffer.getLong();
    }

    DigitalGoodsListing(JSONObject attachmentData) {
      super(attachmentData);
      this.name = (String) attachmentData.get("name");
      this.description = (String) attachmentData.get("description");
      this.tags = (String) attachmentData.get("tags");
      this.quantity = ((Long) attachmentData.get("quantity")).intValue();
      this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
    }

    public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceNQT, int blockchainHeight) {
      super(blockchainHeight);
      this.name = name;
      this.description = description;
      this.tags = tags;
      this.quantity = quantity;
      this.priceNQT = priceNQT;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsListing";
    }

    @Override
    int getMySize() {
      return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2
          + Convert.toBytes(tags).length + 4 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      byte[] nameBytes = Convert.toBytes(name);
      buffer.putShort((short) nameBytes.length);
      buffer.put(nameBytes);
      byte[] descriptionBytes = Convert.toBytes(description);
      buffer.putShort((short) descriptionBytes.length);
      buffer.put(descriptionBytes);
      byte[] tagsBytes = Convert.toBytes(tags);
      buffer.putShort((short) tagsBytes.length);
      buffer.put(tagsBytes);
      buffer.putInt(quantity);
      buffer.putLong(priceNQT);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("name", name);
      attachment.put("description", description);
      attachment.put("tags", tags);
      attachment.put("quantity", quantity);
      attachment.put("priceNQT", priceNQT);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.LISTING;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getTags() { return tags; }

    public int getQuantity() { return quantity; }

    public long getPriceNQT() { return priceNQT; }

  }

  final class DigitalGoodsDelisting extends AbstractAttachment {

    private final long goodsId;

    DigitalGoodsDelisting(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.goodsId = buffer.getLong();
    }

    DigitalGoodsDelisting(JSONObject attachmentData) {
      super(attachmentData);
      this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
    }

    public DigitalGoodsDelisting(long goodsId, int blockchainHeight) {
      super(blockchainHeight);
      this.goodsId = goodsId;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsDelisting";
    }

    @Override
    int getMySize() {
      return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(goodsId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("goods", Convert.toUnsignedLong(goodsId));
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.DELISTING;
    }

    public long getGoodsId() { return goodsId; }

  }

  final class DigitalGoodsPriceChange extends AbstractAttachment {

    private final long goodsId;
    private final long priceNQT;

    DigitalGoodsPriceChange(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.goodsId = buffer.getLong();
      this.priceNQT = buffer.getLong();
    }

    DigitalGoodsPriceChange(JSONObject attachmentData) {
      super(attachmentData);
      this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
      this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
    }

    public DigitalGoodsPriceChange(long goodsId, long priceNQT, int blockchainHeight) {
      super(blockchainHeight);
      this.goodsId = goodsId;
      this.priceNQT = priceNQT;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsPriceChange";
    }

    @Override
    int getMySize() {
      return 8 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(goodsId);
      buffer.putLong(priceNQT);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("goods", Convert.toUnsignedLong(goodsId));
      attachment.put("priceNQT", priceNQT);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.PRICE_CHANGE;
    }

    public long getGoodsId() { return goodsId; }

    public long getPriceNQT() { return priceNQT; }

  }

  final class DigitalGoodsQuantityChange extends AbstractAttachment {

    private final long goodsId;
    private final int deltaQuantity;

    DigitalGoodsQuantityChange(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.goodsId = buffer.getLong();
      this.deltaQuantity = buffer.getInt();
    }

    DigitalGoodsQuantityChange(JSONObject attachmentData) {
      super(attachmentData);
      this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
      this.deltaQuantity = ((Long)attachmentData.get("deltaQuantity")).intValue();
    }

    public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity, int blockchainHeight) {
      super(blockchainHeight);
      this.goodsId = goodsId;
      this.deltaQuantity = deltaQuantity;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsQuantityChange";
    }

    @Override
    int getMySize() {
      return 8 + 4;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(goodsId);
      buffer.putInt(deltaQuantity);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("goods", Convert.toUnsignedLong(goodsId));
      attachment.put("deltaQuantity", deltaQuantity);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.QUANTITY_CHANGE;
    }

    public long getGoodsId() { return goodsId; }

    public int getDeltaQuantity() { return deltaQuantity; }

  }

  final class DigitalGoodsPurchase extends AbstractAttachment {

    private final long goodsId;
    private final int quantity;
    private final long priceNQT;
    private final int deliveryDeadlineTimestamp;

    DigitalGoodsPurchase(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.goodsId = buffer.getLong();
      this.quantity = buffer.getInt();
      this.priceNQT = buffer.getLong();
      this.deliveryDeadlineTimestamp = buffer.getInt();
    }

    DigitalGoodsPurchase(JSONObject attachmentData) {
      super(attachmentData);
      this.goodsId = Convert.parseUnsignedLong((String)attachmentData.get("goods"));
      this.quantity = ((Long)attachmentData.get("quantity")).intValue();
      this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
      this.deliveryDeadlineTimestamp = ((Long)attachmentData.get("deliveryDeadlineTimestamp")).intValue();
    }

    public DigitalGoodsPurchase(long goodsId, int quantity, long priceNQT, int deliveryDeadlineTimestamp, int blockchainHeight) {
      super(blockchainHeight);
      this.goodsId = goodsId;
      this.quantity = quantity;
      this.priceNQT = priceNQT;
      this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsPurchase";
    }

    @Override
    int getMySize() {
      return 8 + 4 + 8 + 4;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(goodsId);
      buffer.putInt(quantity);
      buffer.putLong(priceNQT);
      buffer.putInt(deliveryDeadlineTimestamp);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("goods", Convert.toUnsignedLong(goodsId));
      attachment.put("quantity", quantity);
      attachment.put("priceNQT", priceNQT);
      attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.PURCHASE;
    }

    public long getGoodsId() { return goodsId; }

    public int getQuantity() { return quantity; }

    public long getPriceNQT() { return priceNQT; }

    public int getDeliveryDeadlineTimestamp() { return deliveryDeadlineTimestamp; }

  }

  final class DigitalGoodsDelivery extends AbstractAttachment {

    private final long purchaseId;
    private final EncryptedData goods;
    private final long discountNQT;
    private final boolean goodsIsText;

    DigitalGoodsDelivery(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.purchaseId = buffer.getLong();
      int length = buffer.getInt();
      goodsIsText = length < 0;
      if (length < 0) {
        length &= Integer.MAX_VALUE;
      }
      this.goods = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_DGS_GOODS_LENGTH);
      this.discountNQT = buffer.getLong();
    }

    DigitalGoodsDelivery(JSONObject attachmentData) {
      super(attachmentData);
      this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
      this.goods = new EncryptedData(Convert.parseHexString((String)attachmentData.get("goodsData")),
                                     Convert.parseHexString((String)attachmentData.get("goodsNonce")));
      this.discountNQT = Convert.parseLong(attachmentData.get("discountNQT"));
      this.goodsIsText = Boolean.TRUE.equals(attachmentData.get("goodsIsText"));
    }

    public DigitalGoodsDelivery(long purchaseId, EncryptedData goods, boolean goodsIsText, long discountNQT, int blockchainHeight) {
      super(blockchainHeight);
      this.purchaseId = purchaseId;
      this.goods = goods;
      this.discountNQT = discountNQT;
      this.goodsIsText = goodsIsText;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsDelivery";
    }

    @Override
    int getMySize() {
      return 8 + 4 + goods.getSize() + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(purchaseId);
      buffer.putInt(goodsIsText ? goods.getData().length | Integer.MIN_VALUE : goods.getData().length);
      buffer.put(goods.getData());
      buffer.put(goods.getNonce());
      buffer.putLong(discountNQT);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
      attachment.put("goodsData", Convert.toHexString(goods.getData()));
      attachment.put("goodsNonce", Convert.toHexString(goods.getNonce()));
      attachment.put("discountNQT", discountNQT);
      attachment.put("goodsIsText", goodsIsText);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.DELIVERY;
    }

    public long getPurchaseId() { return purchaseId; }

    public EncryptedData getGoods() { return goods; }

    public long getDiscountNQT() { return discountNQT; }

    public boolean goodsIsText() {
      return goodsIsText;
    }

  }

  final class DigitalGoodsFeedback extends AbstractAttachment {

    private final long purchaseId;

    DigitalGoodsFeedback(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.purchaseId = buffer.getLong();
    }

    DigitalGoodsFeedback(JSONObject attachmentData) {
      super(attachmentData);
      this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
    }

    public DigitalGoodsFeedback(long purchaseId, int blockchainHeight) {
      super(blockchainHeight);
      this.purchaseId = purchaseId;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsFeedback";
    }

    @Override
    int getMySize() {
      return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(purchaseId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.FEEDBACK;
    }

    public long getPurchaseId() { return purchaseId; }

  }

  final class DigitalGoodsRefund extends AbstractAttachment {

    private final long purchaseId;
    private final long refundNQT;

    DigitalGoodsRefund(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.purchaseId = buffer.getLong();
      this.refundNQT = buffer.getLong();
    }

    DigitalGoodsRefund(JSONObject attachmentData) {
      super(attachmentData);
      this.purchaseId = Convert.parseUnsignedLong((String)attachmentData.get("purchase"));
      this.refundNQT = Convert.parseLong(attachmentData.get("refundNQT"));
    }

    public DigitalGoodsRefund(long purchaseId, long refundNQT, int blockchainHeight) {
      super(blockchainHeight);
      this.purchaseId = purchaseId;
      this.refundNQT = refundNQT;
    }

    @Override
    String getAppendixName() {
      return "DigitalGoodsRefund";
    }

    @Override
    int getMySize() {
      return 8 + 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(purchaseId);
      buffer.putLong(refundNQT);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("purchase", Convert.toUnsignedLong(purchaseId));
      attachment.put("refundNQT", refundNQT);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.DigitalGoods.REFUND;
    }

    public long getPurchaseId() { return purchaseId; }

    public long getRefundNQT() { return refundNQT; }

  }

  final class AccountControlEffectiveBalanceLeasing extends AbstractAttachment {

    private final short period;

    AccountControlEffectiveBalanceLeasing(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.period = buffer.getShort();
    }

    AccountControlEffectiveBalanceLeasing(JSONObject attachmentData) {
      super(attachmentData);
      this.period = ((Long) attachmentData.get("period")).shortValue();
    }

    public AccountControlEffectiveBalanceLeasing(short period, int blockchainHeight) {
      super(blockchainHeight);
      this.period = period;
    }

    @Override
    String getAppendixName() {
      return "EffectiveBalanceLeasing";
    }

    @Override
    int getMySize() {
      return 2;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putShort(period);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("period", period);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
    }

    public short getPeriod() {
      return period;
    }
  }

  final class BurstMiningRewardRecipientAssignment extends AbstractAttachment {

    BurstMiningRewardRecipientAssignment(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
    }

    BurstMiningRewardRecipientAssignment(JSONObject attachmentData) {
      super(attachmentData);
    }

    public BurstMiningRewardRecipientAssignment(int blockchainHeight) {
      super(blockchainHeight);
    }

    @Override
    String getAppendixName() {
      return "RewardRecipientAssignment";
    }

    @Override
    int getMySize() {
      return 0;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
    }

    @Override
    void putMyJSON(JSONObject attachment) {
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.BurstMining.REWARD_RECIPIENT_ASSIGNMENT;
    }
  }

  final class AdvancedPaymentEscrowCreation extends AbstractAttachment {

    private final Long amountNQT;
    private final byte requiredSigners;
    private final SortedSet<Long> signers = new TreeSet<>();
    private final int deadline;
    private final Escrow.DecisionType deadlineAction;

    AdvancedPaymentEscrowCreation(ByteBuffer buffer, byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);
      this.amountNQT = buffer.getLong();
      this.deadline = buffer.getInt();
      this.deadlineAction = Escrow.byteToDecision(buffer.get());
      this.requiredSigners = buffer.get();
      byte totalSigners = buffer.get();
      if(totalSigners > 10 || totalSigners <= 0) {
        throw new BurstException.NotValidException("Invalid number of signers listed on create escrow transaction");
      }
      for(int i = 0; i < totalSigners; i++) {
        if(!this.signers.add(buffer.getLong())) {
          throw new BurstException.NotValidException("Duplicate signer on escrow creation");
        }
      }
    }

    AdvancedPaymentEscrowCreation(JSONObject attachmentData) throws BurstException.NotValidException {
      super(attachmentData);
      this.amountNQT = Convert.parseUnsignedLong((String)attachmentData.get("amountNQT"));
      this.deadline = ((Long)attachmentData.get("deadline")).intValue();
      this.deadlineAction = Escrow.stringToDecision((String)attachmentData.get("deadlineAction"));
      this.requiredSigners = ((Long)attachmentData.get("requiredSigners")).byteValue();
      int totalSigners = ((JSONArray)attachmentData.get("signers")).size();
      if(totalSigners > 10 || totalSigners <= 0) {
        throw new BurstException.NotValidException("Invalid number of signers listed on create escrow transaction");
      }
      JSONArray signersJson = (JSONArray)attachmentData.get("signers");
      for(int i = 0; i < signersJson.size(); i++) {
        this.signers.add(Convert.parseUnsignedLong((String)signersJson.get(i)));
      }
      if(this.signers.size() != ((JSONArray)attachmentData.get("signers")).size()) {
        throw new BurstException.NotValidException("Duplicate signer on escrow creation");
      }
    }

    public AdvancedPaymentEscrowCreation(Long amountNQT, int deadline, Escrow.DecisionType deadlineAction,
                                         int requiredSigners, Collection<Long> signers, int blockchainHeight) throws BurstException.NotValidException {
      super(blockchainHeight);
      this.amountNQT = amountNQT;
      this.deadline = deadline;
      this.deadlineAction = deadlineAction;
      this.requiredSigners = (byte)requiredSigners;
      if(signers.size() > 10 || signers.isEmpty()) {
        throw new BurstException.NotValidException("Invalid number of signers listed on create escrow transaction");
      }
      this.signers.addAll(signers);
      if(this.signers.size() != signers.size()) {
        throw new BurstException.NotValidException("Duplicate signer on escrow creation");
      }
    }

    @Override
    String getAppendixName() {
      return "EscrowCreation";
    }

    @Override
    int getMySize() {
      int size = 8 + 4 + 1 + 1 + 1;
      size += (signers.size() * 8);
      return size;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(this.amountNQT);
      buffer.putInt(this.deadline);
      buffer.put(Escrow.decisionToByte(this.deadlineAction));
      buffer.put(this.requiredSigners);
      byte totalSigners = (byte) this.signers.size();
      buffer.put(totalSigners);
      this.signers.forEach(buffer::putLong);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("amountNQT", Convert.toUnsignedLong(this.amountNQT));
      attachment.put("deadline", this.deadline);
      attachment.put("deadlineAction", Escrow.decisionToString(this.deadlineAction));
      attachment.put("requiredSigners", (int)this.requiredSigners);
      JSONArray ids = new JSONArray();
      for(Long signer : this.signers) {
        ids.add(Convert.toUnsignedLong(signer));
      }
      attachment.put("signers", ids);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AdvancedPayment.ESCROW_CREATION;
    }

    public Long getAmountNQT() { return amountNQT; }

    public int getDeadline() { return deadline; }

    public Escrow.DecisionType getDeadlineAction() { return deadlineAction; }

    public int getRequiredSigners() { return (int)requiredSigners; }

    public Collection<Long> getSigners() { return Collections.unmodifiableCollection(signers); }

    public int getTotalSigners() { return signers.size(); }
  }

  final class AdvancedPaymentEscrowSign extends AbstractAttachment {

    private final Long escrowId;
    private final Escrow.DecisionType decision;

    AdvancedPaymentEscrowSign(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.escrowId = buffer.getLong();
      this.decision = Escrow.byteToDecision(buffer.get());
    }

    AdvancedPaymentEscrowSign(JSONObject attachmentData) {
      super(attachmentData);
      this.escrowId = Convert.parseUnsignedLong((String)attachmentData.get("escrowId"));
      this.decision = Escrow.stringToDecision((String)attachmentData.get("decision"));
    }

    public AdvancedPaymentEscrowSign(Long escrowId, Escrow.DecisionType decision, int blockchainHeight) {
      super(blockchainHeight);
      this.escrowId = escrowId;
      this.decision = decision;
    }

    @Override
    String getAppendixName() {
      return "EscrowSign";
    }

    @Override
    int getMySize() {
      return 8 + 1;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(this.escrowId);
      buffer.put(Escrow.decisionToByte(this.decision));
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("escrowId", Convert.toUnsignedLong(this.escrowId));
      attachment.put("decision", Escrow.decisionToString(this.decision));
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AdvancedPayment.ESCROW_SIGN;
    }

    public Long getEscrowId() { return this.escrowId; }

    public Escrow.DecisionType getDecision() { return this.decision; }
  }

  final class AdvancedPaymentEscrowResult extends AbstractAttachment {

    private final Long escrowId;
    private final Escrow.DecisionType decision;

    AdvancedPaymentEscrowResult(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.escrowId = buffer.getLong();
      this.decision = Escrow.byteToDecision(buffer.get());
    }

    AdvancedPaymentEscrowResult(JSONObject attachmentData) {
      super(attachmentData);
      this.escrowId = Convert.parseUnsignedLong((String) attachmentData.get("escrowId"));
      this.decision = Escrow.stringToDecision((String)attachmentData.get("decision"));
    }

    public AdvancedPaymentEscrowResult(Long escrowId, Escrow.DecisionType decision, int blockchainHeight) {
      super(blockchainHeight);
      this.escrowId = escrowId;
      this.decision = decision;
    }

    @Override
    String getAppendixName() {
      return "EscrowResult";
    }

    @Override
    int getMySize() {
      return 8 + 1;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(this.escrowId);
      buffer.put(Escrow.decisionToByte(this.decision));
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("escrowId", Convert.toUnsignedLong(this.escrowId));
      attachment.put("decision", Escrow.decisionToString(this.decision));
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AdvancedPayment.ESCROW_RESULT;
    }
  }

  final class AdvancedPaymentSubscriptionSubscribe extends AbstractAttachment {

    private final Integer frequency;

    AdvancedPaymentSubscriptionSubscribe(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.frequency = buffer.getInt();
    }

    AdvancedPaymentSubscriptionSubscribe(JSONObject attachmentData) {
      super(attachmentData);
      this.frequency = ((Long)attachmentData.get("frequency")).intValue();
    }

    public AdvancedPaymentSubscriptionSubscribe(int frequency, int blockchainHeight) {
      super(blockchainHeight);
      this.frequency = frequency;
    }

    @Override
    String getAppendixName() {
      return "SubscriptionSubscribe";
    }

    @Override
    int getMySize() {
      return 4;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putInt(this.frequency);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("frequency", this.frequency);
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AdvancedPayment.SUBSCRIPTION_SUBSCRIBE;
    }

    public Integer getFrequency() { return this.frequency; }
  }

  final class AdvancedPaymentSubscriptionCancel extends AbstractAttachment {

    private final Long subscriptionId;

    AdvancedPaymentSubscriptionCancel(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.subscriptionId = buffer.getLong();
    }

    AdvancedPaymentSubscriptionCancel(JSONObject attachmentData) {
      super(attachmentData);
      this.subscriptionId = Convert.parseUnsignedLong((String)attachmentData.get("subscriptionId"));
    }

    public AdvancedPaymentSubscriptionCancel(Long subscriptionId, int blockchainHeight) {
      super(blockchainHeight);
      this.subscriptionId = subscriptionId;
    }

    @Override
    String getAppendixName() {
      return "SubscriptionCancel";
    }

    @Override
    int getMySize() {
      return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(subscriptionId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("subscriptionId", Convert.toUnsignedLong(this.subscriptionId));
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AdvancedPayment.SUBSCRIPTION_CANCEL;
    }

    public Long getSubscriptionId() { return this.subscriptionId; }
  }

  final class AdvancedPaymentSubscriptionPayment extends AbstractAttachment {

    private final Long subscriptionId;

    AdvancedPaymentSubscriptionPayment(ByteBuffer buffer, byte transactionVersion) {
      super(buffer, transactionVersion);
      this.subscriptionId = buffer.getLong();
    }

    AdvancedPaymentSubscriptionPayment(JSONObject attachmentData) {
      super(attachmentData);
      this.subscriptionId = Convert.parseUnsignedLong((String) attachmentData.get("subscriptionId"));
    }

    public AdvancedPaymentSubscriptionPayment(Long subscriptionId, int blockchainHeight) {
      super(blockchainHeight);
      this.subscriptionId = subscriptionId;
    }

    @Override
    String getAppendixName() {
      return "SubscriptionPayment";
    }

    @Override
    int getMySize() {
      return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      buffer.putLong(this.subscriptionId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("subscriptionId", Convert.toUnsignedLong(this.subscriptionId));
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AdvancedPayment.SUBSCRIPTION_PAYMENT;
    }
  }

  final class AutomatedTransactionsCreation extends AbstractAttachment{

    private final String name;
    private final String description;
    private final byte[] creationBytes;


    AutomatedTransactionsCreation(ByteBuffer buffer,
                                  byte transactionVersion) throws BurstException.NotValidException {
      super(buffer, transactionVersion);

      this.name = Convert.readString( buffer , buffer.get() , Constants.MAX_AUTOMATED_TRANSACTION_NAME_LENGTH );
      this.description = Convert.readString( buffer , buffer.getShort() , Constants.MAX_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH );

      byte[] dst = new byte[ buffer.capacity() - buffer.position() ];
      buffer.get( dst , 0 , buffer.capacity() - buffer.position() );
      this.creationBytes = dst;

    }

    AutomatedTransactionsCreation(JSONObject attachmentData) {
      super(attachmentData);

      this.name = ( String ) attachmentData.get( "name" );
      this.description = ( String ) attachmentData.get( "description" );

      this.creationBytes = Convert.parseHexString( (String) attachmentData.get( "creationBytes" ) );

    }

    public AutomatedTransactionsCreation( String name, String description , byte[] creationBytes, int blockchainHeight) {
      super(blockchainHeight);
      this.name = name;
      this.description = description;
      this.creationBytes = creationBytes;
    }

    @Override
    public TransactionType getTransactionType() {
      return TransactionType.AutomatedTransactions.AUTOMATED_TRANSACTION_CREATION;
    }

    @Override
    String getAppendixName() {
      return "AutomatedTransactionsCreation";
    }
    @Override
    int getMySize() {
      return 1 + Convert.toBytes( name ).length + 2 + Convert.toBytes( description ).length + creationBytes.length;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
      byte[] nameBytes = Convert.toBytes( name );
      buffer.put( ( byte ) nameBytes.length );
      buffer.put( nameBytes );
      byte[] descriptionBytes = Convert.toBytes( description );
      buffer.putShort( ( short ) descriptionBytes.length );
      buffer.put( descriptionBytes );

      buffer.put( creationBytes );
    }

    @Override
    void putMyJSON(JSONObject attachment) {
      attachment.put("name", name);
      attachment.put("description", description);
      attachment.put("creationBytes", Convert.toHexString( creationBytes ) );
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public byte[] getCreationBytes() {
      return creationBytes;
    }


  }

}
