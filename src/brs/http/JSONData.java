package brs.http;

import static brs.http.common.ResultFields.ACCOUNT_RESPONSE;
import static brs.http.common.ResultFields.ALIAS_NAME_RESPONSE;
import static brs.http.common.ResultFields.ALIAS_RESPONSE;
import static brs.http.common.ResultFields.ALIAS_URI_RESPONSE;
import static brs.http.common.ResultFields.AMOUNT_NQT_RESPONSE;
import static brs.http.common.ResultFields.ASK_ORDER_HEIGHT_RESPONSE;
import static brs.http.common.ResultFields.ASK_ORDER_RESPONSE;
import static brs.http.common.ResultFields.ASSET_RESPONSE;
import static brs.http.common.ResultFields.ASSET_TRANSFER_RESPONSE;
import static brs.http.common.ResultFields.ATTACHMENT_RESPONSE;
import static brs.http.common.ResultFields.BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.BASE_TARGET_RESPONSE;
import static brs.http.common.ResultFields.BID_ORDER_HEIGHT_RESPONSE;
import static brs.http.common.ResultFields.BID_ORDER_RESPONSE;
import static brs.http.common.ResultFields.BLOCK_RESPONSE;
import static brs.http.common.ResultFields.BLOCK_REWARD_RESPONSE;
import static brs.http.common.ResultFields.BLOCK_SIGNATURE_RESPONSE;
import static brs.http.common.ResultFields.BLOCK_TIMESTAMP_RESPONSE;
import static brs.http.common.ResultFields.BUYER_RESPONSE;
import static brs.http.common.ResultFields.CONFIRMATIONS_RESPONSE;
import static brs.http.common.ResultFields.DATA_RESPONSE;
import static brs.http.common.ResultFields.DEADLINE_ACTION_RESPONSE;
import static brs.http.common.ResultFields.DEADLINE_RESPONSE;
import static brs.http.common.ResultFields.DECIMALS_RESPONSE;
import static brs.http.common.ResultFields.DECISION_RESPONSE;
import static brs.http.common.ResultFields.DELISTED_RESPONSE;
import static brs.http.common.ResultFields.DELIVERY_DEADLINE_TIMESTAMP_RESPONSE;
import static brs.http.common.ResultFields.DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.DISCOUNT_NQT_RESPONSE;
import static brs.http.common.ResultFields.EC_BLOCK_HEIGHT_RESPONSE;
import static brs.http.common.ResultFields.EC_BLOCK_ID_RESPONSE;
import static brs.http.common.ResultFields.EFFECTIVE_BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.FEEDBACK_NOTES_RESPONSE;
import static brs.http.common.ResultFields.FEE_NQT_RESPONSE;
import static brs.http.common.ResultFields.FORGED_BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.FREQUENCY_RESPONSE;
import static brs.http.common.ResultFields.FULL_HASH_RESPONSE;
import static brs.http.common.ResultFields.GENERATION_SIGNATURE_RESPONSE;
import static brs.http.common.ResultFields.GENERATOR_PUBLIC_KEY_RESPONSE;
import static brs.http.common.ResultFields.GENERATOR_RESPONSE;
import static brs.http.common.ResultFields.GOODS_DATA_RESPONSE;
import static brs.http.common.ResultFields.GOODS_IS_TEXT_RESPONSE;
import static brs.http.common.ResultFields.GOODS_RESPONSE;
import static brs.http.common.ResultFields.GUARANTEED_BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.HEIGHT_RESPONSE;
import static brs.http.common.ResultFields.ID_RESPONSE;
import static brs.http.common.ResultFields.ID_RS_RESPONSE;
import static brs.http.common.ResultFields.NAME_RESPONSE;
import static brs.http.common.ResultFields.NEXT_BLOCK_RESPONSE;
import static brs.http.common.ResultFields.NONCE_RESPONSE;
import static brs.http.common.ResultFields.NOTE_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_ACCOUNTS_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRADES_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRANSACTIONS_RESPONSE;
import static brs.http.common.ResultFields.NUMBER_OF_TRANSFERS_RESPONSE;
import static brs.http.common.ResultFields.ORDER_RESPONSE;
import static brs.http.common.ResultFields.PAYLOAD_HASH_RESPONSE;
import static brs.http.common.ResultFields.PAYLOAD_LENGTH_RESPONSE;
import static brs.http.common.ResultFields.PENDING_RESPONSE;
import static brs.http.common.ResultFields.PREVIOUS_BLOCK_HASH_RESPONSE;
import static brs.http.common.ResultFields.PREVIOUS_BLOCK_RESPONSE;
import static brs.http.common.ResultFields.PRICE_NQT_RESPONSE;
import static brs.http.common.ResultFields.PUBLIC_FEEDBACKS_RESPONSE;
import static brs.http.common.ResultFields.PURCHASE_RESPONSE;
import static brs.http.common.ResultFields.QUANTITY_QNT_RESPONSE;
import static brs.http.common.ResultFields.QUANTITY_RESPONSE;
import static brs.http.common.ResultFields.RECIPIENT_RESPONSE;
import static brs.http.common.ResultFields.RECIPIENT_RS_RESPONSE;
import static brs.http.common.ResultFields.REFERENCED_TRANSACTION_FULL_HASH_RESPONSE;
import static brs.http.common.ResultFields.REFUND_NOTE_RESPONSE;
import static brs.http.common.ResultFields.REFUND_NQT_RESPONSE;
import static brs.http.common.ResultFields.REQUIRED_SIGNERS_RESPONSE;
import static brs.http.common.ResultFields.SCOOP_NUM_RESPONSE;
import static brs.http.common.ResultFields.SELLER_RESPONSE;
import static brs.http.common.ResultFields.SENDER_PUBLIC_KEY_RESPONSE;
import static brs.http.common.ResultFields.SENDER_RESPONSE;
import static brs.http.common.ResultFields.SENDER_RS_RESPONSE;
import static brs.http.common.ResultFields.SIGNATURE_HASH_RESPONSE;
import static brs.http.common.ResultFields.SIGNATURE_RESPONSE;
import static brs.http.common.ResultFields.SIGNERS_RESPONSE;
import static brs.http.common.ResultFields.SUBTYPE_RESPONSE;
import static brs.http.common.ResultFields.TAGS_RESPONSE;
import static brs.http.common.ResultFields.TIMESTAMP_RESPONSE;
import static brs.http.common.ResultFields.TIME_NEXT_RESPONSE;
import static brs.http.common.ResultFields.TOTAL_AMOUNT_NQT_RESPONSE;
import static brs.http.common.ResultFields.TOTAL_FEE_NQT_RESPONSE;
import static brs.http.common.ResultFields.TRADE_TYPE_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTIONS_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_RESPONSE;
import static brs.http.common.ResultFields.TYPE_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_BALANCE_NQT_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_QUANTITY_QNT_RESPONSE;
import static brs.http.common.ResultFields.VERSION_RESPONSE;

import brs.*;
import brs.Alias.Offer;
import brs.at.AT_API_Helper;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.peer.Hallmark;
import brs.peer.Peer;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class JSONData {

  static JSONObject alias(Alias alias, Offer offer) {
    JSONObject json = new JSONObject();
    putAccount(json, ACCOUNT_RESPONSE, alias.getAccountId());
    json.put(ALIAS_NAME_RESPONSE, alias.getAliasName());
    json.put(ALIAS_URI_RESPONSE, alias.getAliasURI());
    json.put(TIMESTAMP_RESPONSE, alias.getTimestamp());
    json.put(ALIAS_RESPONSE, Convert.toUnsignedLong(alias.getId()));

    if (offer != null) {
      json.put(PRICE_NQT_RESPONSE, String.valueOf(offer.getPriceNQT()));
      if (offer.getBuyerId() != 0) {
        json.put(BUYER_RESPONSE, Convert.toUnsignedLong(offer.getBuyerId()));
      }
    }
    return json;
  }

  static JSONObject accountBalance(Account account) {
    JSONObject json = new JSONObject();
    if (account == null) {
      json.put(BALANCE_NQT_RESPONSE,             "0");
      json.put(UNCONFIRMED_BALANCE_NQT_RESPONSE, "0");
      json.put(EFFECTIVE_BALANCE_NQT_RESPONSE,   "0");
      json.put(FORGED_BALANCE_NQT_RESPONSE,      "0");
      json.put(GUARANTEED_BALANCE_NQT_RESPONSE,  "0");
    }
    else {
      json.put(BALANCE_NQT_RESPONSE, String.valueOf(account.getBalanceNQT()));
      json.put(UNCONFIRMED_BALANCE_NQT_RESPONSE, String.valueOf(account.getUnconfirmedBalanceNQT()));
      json.put(EFFECTIVE_BALANCE_NQT_RESPONSE, String.valueOf(account.getBalanceNQT()));
      json.put(FORGED_BALANCE_NQT_RESPONSE, String.valueOf(account.getForgedBalanceNQT()));
      json.put(GUARANTEED_BALANCE_NQT_RESPONSE, String.valueOf(account.getBalanceNQT()));
    }
    return json;
  }

  static JSONObject asset(Asset asset, int tradeCount, int transferCount, int assetAccountsCount) {
    JSONObject json = new JSONObject();
    putAccount(json, ACCOUNT_RESPONSE, asset.getAccountId());
    json.put(NAME_RESPONSE, asset.getName());
    json.put(DESCRIPTION_RESPONSE, asset.getDescription());
    json.put(DECIMALS_RESPONSE, asset.getDecimals());
    json.put(QUANTITY_QNT_RESPONSE, String.valueOf(asset.getQuantityQNT()));
    json.put(ASSET_RESPONSE, Convert.toUnsignedLong(asset.getId()));
    json.put(NUMBER_OF_TRADES_RESPONSE, tradeCount);
    json.put(NUMBER_OF_TRANSFERS_RESPONSE, transferCount);
    json.put(NUMBER_OF_ACCOUNTS_RESPONSE, assetAccountsCount);
    return json;
  }

  static JSONObject accountAsset(Account.AccountAsset accountAsset) {
    JSONObject json = new JSONObject();
    putAccount(json, ACCOUNT_RESPONSE, accountAsset.getAccountId());
    json.put(ASSET_RESPONSE, Convert.toUnsignedLong(accountAsset.getAssetId()));
    json.put(QUANTITY_QNT_RESPONSE, String.valueOf(accountAsset.getQuantityQNT()));
    json.put(UNCONFIRMED_QUANTITY_QNT_RESPONSE, String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
    return json;
  }

  static JSONObject askOrder(Order.Ask order) {
    JSONObject json = order(order);
    json.put(TYPE_RESPONSE, "ask");
    return json;
  }

  static JSONObject bidOrder(Order.Bid order) {
    JSONObject json = order(order);
    json.put(TYPE_RESPONSE, "bid");
    return json;
  }

  static JSONObject order(Order order) {
    JSONObject json = new JSONObject();
    json.put(ORDER_RESPONSE, Convert.toUnsignedLong(order.getId()));
    json.put(ASSET_RESPONSE, Convert.toUnsignedLong(order.getAssetId()));
    putAccount(json, ACCOUNT_RESPONSE, order.getAccountId());
    json.put(QUANTITY_QNT_RESPONSE, String.valueOf(order.getQuantityQNT()));
    json.put(PRICE_NQT_RESPONSE, String.valueOf(order.getPriceNQT()));
    json.put(HEIGHT_RESPONSE, order.getHeight());
    return json;
  }

  static JSONObject block(Block block, boolean includeTransactions, int currentBlockchainHeight, long blockReward, int scoopNum) {
    JSONObject json = new JSONObject();
    json.put(BLOCK_RESPONSE, block.getStringId());
    json.put(HEIGHT_RESPONSE, block.getHeight());
    putAccount(json, GENERATOR_RESPONSE, block.getGeneratorId());
    json.put(GENERATOR_PUBLIC_KEY_RESPONSE, Convert.toHexString(block.getGeneratorPublicKey()));
    json.put(NONCE_RESPONSE, Convert.toUnsignedLong(block.getNonce()));
    json.put(SCOOP_NUM_RESPONSE, scoopNum);
    json.put(TIMESTAMP_RESPONSE, block.getTimestamp());
    json.put(NUMBER_OF_TRANSACTIONS_RESPONSE, block.getTransactions().size());
    json.put(TOTAL_AMOUNT_NQT_RESPONSE, String.valueOf(block.getTotalAmountNQT()));
    json.put(TOTAL_FEE_NQT_RESPONSE, String.valueOf(block.getTotalFeeNQT()));
    json.put(BLOCK_REWARD_RESPONSE, Convert.toUnsignedLong(blockReward / Constants.ONE_BURST));
    json.put(PAYLOAD_LENGTH_RESPONSE, block.getPayloadLength());
    json.put(VERSION_RESPONSE, block.getVersion());
    json.put(BASE_TARGET_RESPONSE, Convert.toUnsignedLong(block.getBaseTarget()));

    if (block.getPreviousBlockId() != 0) {
      json.put(PREVIOUS_BLOCK_RESPONSE, Convert.toUnsignedLong(block.getPreviousBlockId()));
    }

    if (block.getNextBlockId() != 0) {
      json.put(NEXT_BLOCK_RESPONSE, Convert.toUnsignedLong(block.getNextBlockId()));
    }

    json.put(PAYLOAD_HASH_RESPONSE, Convert.toHexString(block.getPayloadHash()));
    json.put(GENERATION_SIGNATURE_RESPONSE, Convert.toHexString(block.getGenerationSignature()));

    if (block.getVersion() > 1) {
      json.put(PREVIOUS_BLOCK_HASH_RESPONSE, Convert.toHexString(block.getPreviousBlockHash()));
    }

    json.put(BLOCK_SIGNATURE_RESPONSE, Convert.toHexString(block.getBlockSignature()));

    JSONArray transactions = new JSONArray();
    for (Transaction transaction : block.getTransactions()) {
      transactions.add(includeTransactions ? transaction(transaction, currentBlockchainHeight) : Convert.toUnsignedLong(transaction.getId()));
    }
    json.put(TRANSACTIONS_RESPONSE, transactions);
    return json;
  }

  static JSONObject encryptedData(EncryptedData encryptedData) {
    JSONObject json = new JSONObject();
    json.put(DATA_RESPONSE, Convert.toHexString(encryptedData.getData()));
    json.put(NONCE_RESPONSE, Convert.toHexString(encryptedData.getNonce()));
    return json;
  }

  static JSONObject escrowTransaction(Escrow escrow) {
    JSONObject json = new JSONObject();
    json.put(ID_RESPONSE, Convert.toUnsignedLong(escrow.getId()));
    json.put(SENDER_RESPONSE, Convert.toUnsignedLong(escrow.getSenderId()));
    json.put(SENDER_RS_RESPONSE, Convert.rsAccount(escrow.getSenderId()));
    json.put(RECIPIENT_RESPONSE, Convert.toUnsignedLong(escrow.getRecipientId()));
    json.put(RECIPIENT_RS_RESPONSE, Convert.rsAccount(escrow.getRecipientId()));
    json.put(AMOUNT_NQT_RESPONSE, Convert.toUnsignedLong(escrow.getAmountNQT()));
    json.put(REQUIRED_SIGNERS_RESPONSE, escrow.getRequiredSigners());
    json.put(DEADLINE_RESPONSE, escrow.getDeadline());
    json.put(DEADLINE_ACTION_RESPONSE, Escrow.decisionToString(escrow.getDeadlineAction()));

    JSONArray signers = new JSONArray();
    BurstIterator<Escrow.Decision> decisions = escrow.getDecisions();
    while(decisions.hasNext()) {
      Escrow.Decision decision = decisions.next();
      if(decision.getAccountId().equals(escrow.getSenderId()) ||
         decision.getAccountId().equals(escrow.getRecipientId())) {
        continue;
      }
      JSONObject signerDetails = new JSONObject();
      signerDetails.put(ID_RESPONSE, Convert.toUnsignedLong(decision.getAccountId()));
      signerDetails.put(ID_RS_RESPONSE, Convert.rsAccount(decision.getAccountId()));
      signerDetails.put(DECISION_RESPONSE, Escrow.decisionToString(decision.getDecision()));
      signers.add(signerDetails);
    }
    json.put(SIGNERS_RESPONSE, signers);
    return json;
  }

  static JSONObject goods(DigitalGoodsStore.Goods goods) {
    JSONObject json = new JSONObject();
    json.put(GOODS_RESPONSE, Convert.toUnsignedLong(goods.getId()));
    json.put(NAME_RESPONSE, goods.getName());
    json.put(DESCRIPTION_RESPONSE, goods.getDescription());
    json.put(QUANTITY_RESPONSE, goods.getQuantity());
    json.put(PRICE_NQT_RESPONSE, String.valueOf(goods.getPriceNQT()));
    putAccount(json, SELLER_RESPONSE, goods.getSellerId());
    json.put(TAGS_RESPONSE, goods.getTags());
    json.put(DELISTED_RESPONSE, goods.isDelisted());
    json.put(TIMESTAMP_RESPONSE, goods.getTimestamp());
    return json;
  }

  static JSONObject hallmark(Hallmark hallmark) {
    JSONObject json = new JSONObject();
    putAccount(json, "account", Account.getId(hallmark.getPublicKey()));
    json.put("host", hallmark.getHost());
    json.put("weight", hallmark.getWeight());
    String dateString = Hallmark.formatDate(hallmark.getDate());
    json.put("date", dateString);
    json.put("valid", hallmark.isValid());
    return json;
  }

  static JSONObject token(Token token) {
    JSONObject json = new JSONObject();
    putAccount(json, "account", Account.getId(token.getPublicKey()));
    json.put("timestamp", token.getTimestamp());
    json.put("valid", token.isValid());
    return json;
  }

  static JSONObject peer(Peer peer) {
    JSONObject json = new JSONObject();
    json.put("state", peer.getState().ordinal());
    json.put("announcedAddress", peer.getAnnouncedAddress());
    json.put("shareAddress", peer.shareAddress());
    if (peer.getHallmark() != null) {
      json.put("hallmark", peer.getHallmark().getHallmarkString());
    }
    json.put("weight", peer.getWeight());
    json.put("downloadedVolume", peer.getDownloadedVolume());
    json.put("uploadedVolume", peer.getUploadedVolume());
    json.put("application", peer.getApplication());
    json.put("version", peer.getVersion());
    json.put("platform", peer.getPlatform());
    json.put("blacklisted", peer.isBlacklisted());
    json.put("lastUpdated", peer.getLastUpdated());
    return json;
  }

  static JSONObject purchase(DigitalGoodsStore.Purchase purchase) {
    JSONObject json = new JSONObject();
    json.put(PURCHASE_RESPONSE, Convert.toUnsignedLong(purchase.getId()));
    json.put(GOODS_RESPONSE, Convert.toUnsignedLong(purchase.getGoodsId()));
    json.put(NAME_RESPONSE, purchase.getName());
    putAccount(json, SELLER_RESPONSE, purchase.getSellerId());
    json.put(PRICE_NQT_RESPONSE, String.valueOf(purchase.getPriceNQT()));
    json.put(QUANTITY_RESPONSE, purchase.getQuantity());
    putAccount(json, BUYER_RESPONSE, purchase.getBuyerId());
    json.put(TIMESTAMP_RESPONSE, purchase.getTimestamp());
    json.put(DELIVERY_DEADLINE_TIMESTAMP_RESPONSE, purchase.getDeliveryDeadlineTimestamp());
    if (purchase.getNote() != null) {
      json.put(NOTE_RESPONSE, encryptedData(purchase.getNote()));
    }
    json.put(PENDING_RESPONSE, purchase.isPending());
    if (purchase.getEncryptedGoods() != null) {
      json.put(GOODS_DATA_RESPONSE, encryptedData(purchase.getEncryptedGoods()));
      json.put(GOODS_IS_TEXT_RESPONSE, purchase.goodsIsText());
    }
    if (purchase.getFeedbackNotes() != null) {
      JSONArray feedbacks = new JSONArray();
      for (EncryptedData encryptedData : purchase.getFeedbackNotes()) {
        feedbacks.add(encryptedData(encryptedData));
      }
      json.put(FEEDBACK_NOTES_RESPONSE, feedbacks);
    }
    if (purchase.getPublicFeedback() != null) {
      JSONArray publicFeedbacks = new JSONArray();
      publicFeedbacks.addAll(purchase.getPublicFeedback());
      json.put(PUBLIC_FEEDBACKS_RESPONSE, publicFeedbacks);
    }
    if (purchase.getRefundNote() != null) {
      json.put(REFUND_NOTE_RESPONSE, encryptedData(purchase.getRefundNote()));
    }
    if (purchase.getDiscountNQT() > 0) {
      json.put(DISCOUNT_NQT_RESPONSE, String.valueOf(purchase.getDiscountNQT()));
    }
    if (purchase.getRefundNQT() > 0) {
      json.put(REFUND_NQT_RESPONSE, String.valueOf(purchase.getRefundNQT()));
    }
    return json;
  }

  static JSONObject subscription(Subscription subscription) {
    JSONObject json = new JSONObject();
    json.put(ID_RESPONSE, Convert.toUnsignedLong(subscription.getId()));
    putAccount(json, SENDER_RESPONSE, subscription.getSenderId());
    putAccount(json, RECIPIENT_RESPONSE, subscription.getRecipientId());
    json.put(AMOUNT_NQT_RESPONSE, Convert.toUnsignedLong(subscription.getAmountNQT()));
    json.put(FREQUENCY_RESPONSE, subscription.getFrequency());
    json.put(TIME_NEXT_RESPONSE, subscription.getTimeNext());
    return json;
  }

  static JSONObject trade(Trade trade, Asset asset) {
    JSONObject json = new JSONObject();
    json.put(TIMESTAMP_RESPONSE, trade.getTimestamp());
    json.put(QUANTITY_QNT_RESPONSE, String.valueOf(trade.getQuantityQNT()));
    json.put(PRICE_NQT_RESPONSE, String.valueOf(trade.getPriceNQT()));
    json.put(ASSET_RESPONSE, Convert.toUnsignedLong(trade.getAssetId()));
    json.put(ASK_ORDER_RESPONSE, Convert.toUnsignedLong(trade.getAskOrderId()));
    json.put(BID_ORDER_RESPONSE, Convert.toUnsignedLong(trade.getBidOrderId()));
    json.put(ASK_ORDER_HEIGHT_RESPONSE, trade.getAskOrderHeight());
    json.put(BID_ORDER_HEIGHT_RESPONSE, trade.getBidOrderHeight());
    putAccount(json, SELLER_RESPONSE, trade.getSellerId());
    putAccount(json, BUYER_RESPONSE, trade.getBuyerId());
    json.put(BLOCK_RESPONSE, Convert.toUnsignedLong(trade.getBlockId()));
    json.put(HEIGHT_RESPONSE, trade.getHeight());
    json.put(TRADE_TYPE_RESPONSE, trade.isBuy() ? "buy" : "sell");
    if (asset != null) {
      json.put(NAME_RESPONSE, asset.getName());
      json.put(DECIMALS_RESPONSE, asset.getDecimals());
    }
    return json;
  }

  static JSONObject assetTransfer(AssetTransfer assetTransfer, Asset asset) {
    JSONObject json = new JSONObject();
    json.put(ASSET_TRANSFER_RESPONSE, Convert.toUnsignedLong(assetTransfer.getId()));
    json.put(ASSET_RESPONSE, Convert.toUnsignedLong(assetTransfer.getAssetId()));
    putAccount(json, SENDER_RESPONSE, assetTransfer.getSenderId());
    putAccount(json, RECIPIENT_RESPONSE, assetTransfer.getRecipientId());
    json.put(QUANTITY_QNT_RESPONSE, String.valueOf(assetTransfer.getQuantityQNT()));
    json.put(HEIGHT_RESPONSE, assetTransfer.getHeight());
    json.put(TIMESTAMP_RESPONSE, assetTransfer.getTimestamp());
    if (asset != null) {
      json.put(NAME_RESPONSE, asset.getName());
      json.put(DECIMALS_RESPONSE, asset.getDecimals());
    }

    return json;
  }

  static JSONObject unconfirmedTransaction(Transaction transaction) {
    JSONObject json = new JSONObject();
    json.put(TYPE_RESPONSE, transaction.getType().getType());
    json.put(SUBTYPE_RESPONSE, transaction.getType().getSubtype());
    json.put(TIMESTAMP_RESPONSE, transaction.getTimestamp());
    json.put(DEADLINE_RESPONSE, transaction.getDeadline());
    json.put(SENDER_PUBLIC_KEY_RESPONSE, Convert.toHexString(transaction.getSenderPublicKey()));
    if (transaction.getRecipientId() != 0) {
      putAccount(json, RECIPIENT_RESPONSE, transaction.getRecipientId());
    }
    json.put(AMOUNT_NQT_RESPONSE, String.valueOf(transaction.getAmountNQT()));
    json.put(FEE_NQT_RESPONSE, String.valueOf(transaction.getFeeNQT()));
    if (transaction.getReferencedTransactionFullHash() != null) {
      json.put(REFERENCED_TRANSACTION_FULL_HASH_RESPONSE, transaction.getReferencedTransactionFullHash());
    }
    byte[] signature = Convert.emptyToNull(transaction.getSignature());
    if (signature != null) {
      json.put(SIGNATURE_RESPONSE, Convert.toHexString(signature));
      json.put(SIGNATURE_HASH_RESPONSE, Convert.toHexString(Crypto.sha256().digest(signature)));
      json.put(FULL_HASH_RESPONSE, transaction.getFullHash());
      json.put(TRANSACTION_RESPONSE, transaction.getStringId());
    }
    else if (!transaction.getType().isSigned()) {
      json.put(FULL_HASH_RESPONSE, transaction.getFullHash());
      json.put(TRANSACTION_RESPONSE, transaction.getStringId());
    }
    JSONObject attachmentJSON = new JSONObject();
    for (Appendix appendage : transaction.getAppendages()) {
      attachmentJSON.putAll(appendage.getJSONObject());
    }
    if (! attachmentJSON.isEmpty()) {
      modifyAttachmentJSON(attachmentJSON);
      json.put(ATTACHMENT_RESPONSE, attachmentJSON);
    }
    putAccount(json, SENDER_RESPONSE, transaction.getSenderId());
    json.put(HEIGHT_RESPONSE, transaction.getHeight());
    json.put(VERSION_RESPONSE, transaction.getVersion());
    if (transaction.getVersion() > 0) {
      json.put(EC_BLOCK_ID_RESPONSE, Convert.toUnsignedLong(transaction.getECBlockId()));
      json.put(EC_BLOCK_HEIGHT_RESPONSE, transaction.getECBlockHeight());
    }

    return json;
  }

  public static JSONObject transaction(Transaction transaction, int currentBlockchainHeight) {
    JSONObject json = unconfirmedTransaction(transaction);
    json.put(BLOCK_RESPONSE, Convert.toUnsignedLong(transaction.getBlockId()));
    json.put(CONFIRMATIONS_RESPONSE, currentBlockchainHeight - transaction.getHeight());
    json.put(BLOCK_TIMESTAMP_RESPONSE, transaction.getBlockTimestamp());
    return json;
  }

  // ugly, hopefully temporary
  private static void modifyAttachmentJSON(JSONObject json) {
    Long quantityQNT = (Long) json.remove(QUANTITY_QNT_RESPONSE);
    if (quantityQNT != null) {
      json.put(QUANTITY_QNT_RESPONSE, String.valueOf(quantityQNT));
    }
    Long priceNQT = (Long) json.remove(PRICE_NQT_RESPONSE);
    if (priceNQT != null) {
      json.put(PRICE_NQT_RESPONSE, String.valueOf(priceNQT));
    }
    Long discountNQT = (Long) json.remove(DISCOUNT_NQT_RESPONSE);
    if (discountNQT != null) {
      json.put(DISCOUNT_NQT_RESPONSE, String.valueOf(discountNQT));
    }
    Long refundNQT = (Long) json.remove(REFUND_NQT_RESPONSE);
    if (refundNQT != null) {
      json.put(REFUND_NQT_RESPONSE, String.valueOf(refundNQT));
    }
  }

  static void putAccount(JSONObject json, String name, long accountId) {
    json.put(name, Convert.toUnsignedLong(accountId));
    json.put(name + "RS", Convert.rsAccount(accountId));
  }

  //TODO refactor the accountservice out of this :-)
  static JSONObject at(AT at, AccountService accountService) {
    JSONObject json = new JSONObject();
    ByteBuffer bf = ByteBuffer.allocate( 8 );
    bf.order( ByteOrder.LITTLE_ENDIAN );

    bf.put( at.getCreator() );
    bf.clear();
    putAccount(json, "creator", bf.getLong() );
    bf.clear();
    bf.put( at.getId() , 0 , 8 );
    long id = bf.getLong(0);
    json.put("at", Convert.toUnsignedLong( id ));
    json.put("atRS", Convert.rsAccount(id));
    json.put("atVersion", at.getVersion());
    json.put("name", at.getName());
    json.put("description", at.getDescription());
    json.put("creator", Convert.toUnsignedLong(AT_API_Helper.getLong(at.getCreator())));
    json.put("creatorRS", Convert.rsAccount(AT_API_Helper.getLong(at.getCreator())));
    json.put("machineCode", Convert.toHexString(at.getApCode()));
    json.put("machineData", Convert.toHexString(at.getApData()));
    json.put("balanceNQT", Convert.toUnsignedLong(accountService.getAccount(id).getBalanceNQT()));
    json.put("prevBalanceNQT", Convert.toUnsignedLong(at.getP_balance()));
    json.put("nextBlock", at.nextHeight());
    json.put("frozen", at.freezeOnSameBalance());
    json.put("running", at.getMachineState().isRunning());
    json.put("stopped", at.getMachineState().isStopped());
    json.put("finished", at.getMachineState().isFinished());
    json.put("dead", at.getMachineState().isDead());
    json.put("minActivation", Convert.toUnsignedLong(at.minActivationAmount()));
    json.put("creationBlock", at.getCreationBlockHeight());
    return json;
  }

  static JSONObject hex2long(String longString){
    JSONObject json = new JSONObject();
    json.put("hex2long", longString);
    return json;
  }

  private JSONData() {} // never

}
