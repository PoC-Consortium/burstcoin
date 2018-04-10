package brs;

import brs.Appendix.AbstractAppendix;
import brs.crypto.Crypto;
import brs.db.BurstKey;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.*;

public class Transaction implements Comparable<Transaction> {

  private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

  public static class Builder {

    private final short deadline;
    private final byte[] senderPublicKey;
    private final long amountNQT;
    private final long feeNQT;
    private final TransactionType type;
    private final byte version;
    private final int timestamp;
    private final Attachment.AbstractAttachment attachment;

    private long recipientId;
    private String referencedTransactionFullHash;
    private byte[] signature;
    private Appendix.Message message;
    private Appendix.EncryptedMessage encryptedMessage;
    private Appendix.EncryptToSelfMessage encryptToSelfMessage;
    private Appendix.PublicKeyAnnouncement publicKeyAnnouncement;
    private long blockId;
    private int height = Integer.MAX_VALUE;
    private long id;
    private long senderId;
    private int blockTimestamp = -1;
    private String fullHash;
    private int ecBlockHeight;
    private long ecBlockId;

    public Builder(byte version, byte[] senderPublicKey, long amountNQT, long feeNQT, int timestamp, short deadline,
                       Attachment.AbstractAttachment attachment) {
      this.version = version;
      this.timestamp = timestamp;
      this.deadline = deadline;
      this.senderPublicKey = senderPublicKey;
      this.amountNQT = amountNQT;
      this.feeNQT = feeNQT;
      this.attachment = attachment;
      this.type = attachment.getTransactionType();
    }

    public Transaction build() throws BurstException.NotValidException {
      return new Transaction(this);
    }

    public Builder recipientId(long recipientId) {
      this.recipientId = recipientId;
      return this;
    }

    public Builder referencedTransactionFullHash(String referencedTransactionFullHash) {
      this.referencedTransactionFullHash = referencedTransactionFullHash;
      return this;
    }

    public Builder referencedTransactionFullHash(byte[] referencedTransactionFullHash) {
      if (referencedTransactionFullHash != null) {
        this.referencedTransactionFullHash = Convert.toHexString(referencedTransactionFullHash);
      }
      return this;
    }

    public Builder message(Appendix.Message message) {
      this.message = message;
      return this;
    }

    public Builder encryptedMessage(Appendix.EncryptedMessage encryptedMessage) {
      this.encryptedMessage = encryptedMessage;
      return this;
    }

    public Builder encryptToSelfMessage(Appendix.EncryptToSelfMessage encryptToSelfMessage) {
      this.encryptToSelfMessage = encryptToSelfMessage;
      return this;
    }

    public Builder publicKeyAnnouncement(Appendix.PublicKeyAnnouncement publicKeyAnnouncement) {
      this.publicKeyAnnouncement = publicKeyAnnouncement;
      return this;
    }

    public Builder id(long id) {
      this.id = id;
      return this;
    }

    public Builder signature(byte[] signature) {
      this.signature = signature;
      return this;
    }

    public Builder blockId(long blockId) {
      this.blockId = blockId;
      return this;
    }

    public Builder height(int height) {
      this.height = height;
      return this;
    }

    public Builder senderId(long senderId) {
      this.senderId = senderId;
      return this;
    }

    Builder fullHash(String fullHash) {
      this.fullHash = fullHash;
      return this;
    }

    public Builder fullHash(byte[] fullHash) {
      if (fullHash != null) {
        this.fullHash = Convert.toHexString(fullHash);
      }
      return this;
    }

    public Builder blockTimestamp(int blockTimestamp) {
      this.blockTimestamp = blockTimestamp;
      return this;
    }

    public Builder ecBlockHeight(int height) {
      this.ecBlockHeight = height;
      return this;
    }

    public Builder ecBlockId(long blockId) {
      this.ecBlockId = blockId;
      return this;
    }

  }

  private final short deadline;
  private final byte[] senderPublicKey;
  private final long recipientId;
  private final long amountNQT;
  private final long feeNQT;
  private final String referencedTransactionFullHash;
  private final TransactionType type;
  private final int ecBlockHeight;
  private final long ecBlockId;
  private final byte version;
  private final int timestamp;
  private final Attachment.AbstractAttachment attachment;
  private final Appendix.Message message;
  private final Appendix.EncryptedMessage encryptedMessage;
  private final Appendix.EncryptToSelfMessage encryptToSelfMessage;
  private final Appendix.PublicKeyAnnouncement publicKeyAnnouncement;

  private final List<? extends Appendix.AbstractAppendix> appendages;
  private final int appendagesSize;

  private volatile int height;
  private volatile long blockId;
  private volatile Block block;
  private volatile byte[] signature;
  private volatile int blockTimestamp;
  private volatile long id;
  private volatile String stringId;
  private volatile long senderId;
  private volatile String fullHash;
  private volatile BurstKey dbKey;

  private Transaction(Builder builder) throws BurstException.NotValidException {

    this.timestamp = builder.timestamp;
    this.deadline = builder.deadline;
    this.senderPublicKey = builder.senderPublicKey;
    this.recipientId = Optional.ofNullable(builder.recipientId).orElse(0L);
    this.amountNQT = builder.amountNQT;
    this.referencedTransactionFullHash = builder.referencedTransactionFullHash;
    this.signature = builder.signature;
    this.type = builder.type;
    this.version = builder.version;
    this.blockId = builder.blockId;
    this.height = builder.height;
    this.id = builder.id;
    this.senderId = builder.senderId;
    this.blockTimestamp = builder.blockTimestamp;
    this.fullHash = builder.fullHash;
    this.ecBlockHeight = builder.ecBlockHeight;
    this.ecBlockId = builder.ecBlockId;

    List<Appendix.AbstractAppendix> list = new ArrayList<>();
    if ((this.attachment = builder.attachment) != null) {
      list.add(this.attachment);
    }
    if ((this.message  = builder.message) != null) {
      list.add(this.message);
    }
    if ((this.encryptedMessage = builder.encryptedMessage) != null) {
      list.add(this.encryptedMessage);
    }
    if ((this.publicKeyAnnouncement = builder.publicKeyAnnouncement) != null) {
      list.add(this.publicKeyAnnouncement);
    }
    if ((this.encryptToSelfMessage = builder.encryptToSelfMessage) != null) {
      list.add(this.encryptToSelfMessage);
    }
    this.appendages = Collections.unmodifiableList(list);
    int countAppendeges = 0;
    for (Appendix appendage : appendages) {
      countAppendeges += appendage.getSize();
    }
    this.appendagesSize = countAppendeges;
    int effectiveHeight = (height < Integer.MAX_VALUE ? height : Burst.getBlockchain().getHeight());
    long minimumFeeNQT = type.minimumFeeNQT(effectiveHeight, countAppendeges);
    if(type == null || type.isSigned()) {
      if (builder.feeNQT > 0 && builder.feeNQT < minimumFeeNQT) {
        throw new BurstException.NotValidException(String.format("Requested fee %d less than the minimum fee %d",
                                                               builder.feeNQT, minimumFeeNQT));
      }
      if (builder.feeNQT <= 0) {
        feeNQT = minimumFeeNQT;
      } else {
        feeNQT = builder.feeNQT;
      }
    }
    else {
      feeNQT = builder.feeNQT;
    }

    if(type == null || type.isSigned()) {
      if (deadline < 1
          || feeNQT > Constants.MAX_BALANCE_NQT
          || amountNQT < 0
          || amountNQT > Constants.MAX_BALANCE_NQT
          || type == null) {
        throw new BurstException.NotValidException("Invalid transaction parameters:\n type: " + type + ", timestamp: " + timestamp
                                                 + ", deadline: " + deadline + ", fee: " + feeNQT + ", amount: " + amountNQT);
      }
    }

    if (attachment == null || type != attachment.getTransactionType()) {
      throw new BurstException.NotValidException("Invalid attachment " + attachment + " for transaction of type " + type);
    }

    if (! type.hasRecipient()) {
      if (recipientId != 0 || getAmountNQT() != 0) {
        throw new BurstException.NotValidException("Transactions of this type must have recipient == Genesis, amount == 0");
      }
    }

    for (Appendix.AbstractAppendix appendage : appendages) {
      if (! appendage.verifyVersion(this.version) && (!Constants.isTestnet || Burst.getBlockchain().getHeight() > 11795)) {
        throw new BurstException.NotValidException("Invalid attachment version " + appendage.getVersion()
                                                 + " for transaction version " + this.version);
      }
    }

  }

  public short getDeadline() {
    return deadline;
  }

  public byte[] getSenderPublicKey() {
    return senderPublicKey;
  }

  public long getRecipientId() {
    return recipientId;
  }

  public long getAmountNQT() {
    return amountNQT;
  }

  public long getFeeNQT() {
    return feeNQT;
  }

  public String getReferencedTransactionFullHash() {
    return referencedTransactionFullHash;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public byte[] getSignature() {
    return signature;
  }

  public TransactionType getType() {
    return type;
  }

  public byte getVersion() {
    return version;
  }

  public long getBlockId() {
    return blockId;
  }

  public void setBlock(Block block) {
    this.block = block;
    this.blockId = block.getId();
    this.height = block.getHeight();
    this.blockTimestamp = block.getTimestamp();
  }

  void unsetBlock() {
    this.block = null;
    this.blockId = 0;
    this.blockTimestamp = -1;
    // must keep the height set, as transactions already having been included in a popped-off block before
    // get priority when sorted for inclusion in a new block
  }

  public int getTimestamp() {
    return timestamp;
  }

  public int getBlockTimestamp() {
    return blockTimestamp;
  }

  public int getExpiration() {
    return timestamp + deadline * 60;
  }

  public Attachment getAttachment() {
    return attachment;
  }

  public List<? extends AbstractAppendix> getAppendages() {
    return appendages;
  }

  public long getId() {
    if (id == 0) {
      if (signature == null && type.isSigned()) {
        throw new IllegalStateException("Transaction is not signed yet");
      }
      byte[] hash;
      if (useNQT()) {
        byte[] data = zeroSignature(getBytes());
        byte[] signatureHash = Crypto.sha256().digest(signature != null ? signature : new byte[64]);
        MessageDigest digest = Crypto.sha256();
        digest.update(data);
        hash = digest.digest(signatureHash);
      } else {
        hash = Crypto.sha256().digest(getBytes());
      }
      BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
      id = bigInteger.longValue();
      stringId = bigInteger.toString();
      fullHash = Convert.toHexString(hash);
    }
    return id;
  }

  public String getStringId() {
    if (stringId == null) {
      getId();
      if (stringId == null) {
        stringId = Convert.toUnsignedLong(id);
      }
    }
    return stringId;
  }

  public String getFullHash() {
    if (fullHash == null) {
      getId();
    }
    return fullHash;
  }

  public long getSenderId() {
    if (senderId == 0 && (type == null || type.isSigned())) {
      senderId = Account.getId(senderPublicKey);
    }
    return senderId;
  }

  public BurstKey getDbKey() {
    if (dbKey == null) {
      dbKey = Burst.getStores().getTransactionProcessorStore().getUnconfirmedTransactionDbKeyFactory().newKey(getId());
    }
    return dbKey;
  }

  public Appendix.Message getMessage() {
    return message;
  }

  public Appendix.EncryptedMessage getEncryptedMessage() {
    return encryptedMessage;
  }

  public Appendix.EncryptToSelfMessage getEncryptToSelfMessage() {
    return encryptToSelfMessage;
  }

  public Appendix.PublicKeyAnnouncement getPublicKeyAnnouncement() {
    return publicKeyAnnouncement;
  }

  public byte[] getBytes() {
    try {
      ByteBuffer buffer = ByteBuffer.allocate(getSize());
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.put(type.getType());
      buffer.put((byte) ((version << 4) | ( type.getSubtype() & 0xff ) ));
      buffer.putInt(timestamp);
      buffer.putShort(deadline);
      if(type.isSigned() || Burst.getBlockchain().getHeight() < Constants.AT_FIX_BLOCK_4) {
        buffer.put(senderPublicKey);
      }
      else {
        buffer.putLong(senderId);
        buffer.put(new byte[24]);
      }
      buffer.putLong(type.hasRecipient() ? recipientId : Genesis.CREATOR_ID);
      if (useNQT()) {
        buffer.putLong(amountNQT);
        buffer.putLong(feeNQT);
        if (referencedTransactionFullHash != null) {
          buffer.put(Convert.parseHexString(referencedTransactionFullHash));
        } else {
          buffer.put(new byte[32]);
        }
      } else {
        buffer.putInt((int) (amountNQT / Constants.ONE_BURST));
        buffer.putInt((int) (feeNQT / Constants.ONE_BURST));
        if (referencedTransactionFullHash != null) {
          buffer.putLong(Convert.fullHashToId(Convert.parseHexString(referencedTransactionFullHash)));
        } else {
          buffer.putLong(0L);
        }
      }
      buffer.put(signature != null ? signature : new byte[64]);
      if (version > 0) {
        buffer.putInt(getFlags());
        buffer.putInt(ecBlockHeight);
        buffer.putLong(ecBlockId);
      }
      appendages.forEach(appendage -> appendage.putBytes(buffer));
      return buffer.array();
    } catch (RuntimeException e) {
      logger.debug("Failed to get transaction bytes for transaction: " + getJSONObject().toJSONString());
      throw e;
    }
  }

  public static Transaction parseTransaction(byte[] bytes) throws BurstException.ValidationException {
    try {
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      byte type = buffer.get();
      byte subtype = buffer.get();
      byte version = (byte) ((subtype & 0xF0) >> 4);
      subtype = (byte) (subtype & 0x0F);
      int timestamp = buffer.getInt();
      short deadline = buffer.getShort();
      byte[] senderPublicKey = new byte[32];
      buffer.get(senderPublicKey);
      long recipientId = buffer.getLong();
      long amountNQT = buffer.getLong();
      long feeNQT = buffer.getLong();
      String referencedTransactionFullHash = null;
      byte[] referencedTransactionFullHashBytes = new byte[32];
      buffer.get(referencedTransactionFullHashBytes);
      if (Convert.emptyToNull(referencedTransactionFullHashBytes) != null) {
        referencedTransactionFullHash = Convert.toHexString(referencedTransactionFullHashBytes);
      }
      byte[] signature = new byte[64];
      buffer.get(signature);
      signature = Convert.emptyToNull(signature);
      int flags = 0;
      int ecBlockHeight = 0;
      long ecBlockId = 0;
      if (version > 0) {
        flags = buffer.getInt();
        ecBlockHeight = buffer.getInt();
        ecBlockId = buffer.getLong();
      }
      TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
      Transaction.Builder builder = new Transaction.Builder(version, senderPublicKey, amountNQT, feeNQT,
                                                                            timestamp, deadline, transactionType.parseAttachment(buffer, version))
          .referencedTransactionFullHash(referencedTransactionFullHash)
          .signature(signature)
          .ecBlockHeight(ecBlockHeight)
          .ecBlockId(ecBlockId);
      if (transactionType.hasRecipient()) {
        builder.recipientId(recipientId);
      }
      int position = 1;
      if ((flags & position) != 0 || (version == 0 && transactionType == TransactionType.Messaging.ARBITRARY_MESSAGE)) {
        builder.message(new Appendix.Message(buffer, version));
      }
      position <<= 1;
      if ((flags & position) != 0) {
        builder.encryptedMessage(new Appendix.EncryptedMessage(buffer, version));
      }
      position <<= 1;
      if ((flags & position) != 0) {
        builder.publicKeyAnnouncement(new Appendix.PublicKeyAnnouncement(buffer, version));
      }
      position <<= 1;
      if ((flags & position) != 0) {
        builder.encryptToSelfMessage(new Appendix.EncryptToSelfMessage(buffer, version));
      }
      return builder.build();
    } catch (BurstException.NotValidException|RuntimeException e) {
      logger.debug("Failed to parse transaction bytes: " + Convert.toHexString(bytes));
      throw e;
    }
  }

  public byte[] getUnsignedBytes() {
    return zeroSignature(getBytes());
  }

  /*
    @Override
    public Collection<TransactionType> getPhasingTransactionTypes() {
    return getType().getPhasingTransactionTypes();
    }

    @Override
    public Collection<TransactionType> getPhasedTransactionTypes() {
    return getType().getPhasedTransactionTypes();
    }
  */

  public JSONObject getJSONObject() {
    JSONObject json = new JSONObject();
    json.put("type", type.getType());
    json.put("subtype", type.getSubtype());
    json.put("timestamp", timestamp);
    json.put("deadline", deadline);
    json.put("senderPublicKey", Convert.toHexString(senderPublicKey));
    if (type.hasRecipient()) {
      json.put("recipient", Convert.toUnsignedLong(recipientId));
    }
    json.put("amountNQT", amountNQT);
    json.put("feeNQT", feeNQT);
    if (referencedTransactionFullHash != null) {
      json.put("referencedTransactionFullHash", referencedTransactionFullHash);
    }
    json.put("ecBlockHeight", ecBlockHeight);
    json.put("ecBlockId", Convert.toUnsignedLong(ecBlockId));
    json.put("signature", Convert.toHexString(signature));
    JSONObject attachmentJSON = new JSONObject();
    appendages.forEach(appendage -> attachmentJSON.putAll(appendage.getJSONObject()));
    //if (! attachmentJSON.isEmpty()) {
    json.put("attachment", attachmentJSON);
    //}
    json.put("version", version);
    return json;
  }

  static Transaction parseTransaction(JSONObject transactionData) throws BurstException.NotValidException {
    try {
      byte type = ((Long) transactionData.get("type")).byteValue();
      byte subtype = ((Long) transactionData.get("subtype")).byteValue();
      int timestamp = ((Long) transactionData.get("timestamp")).intValue();
      short deadline = ((Long) transactionData.get("deadline")).shortValue();
      byte[] senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
      long amountNQT = Convert.parseLong(transactionData.get("amountNQT"));
      long feeNQT = Convert.parseLong(transactionData.get("feeNQT"));
      String referencedTransactionFullHash = (String) transactionData.get("referencedTransactionFullHash");
      byte[] signature = Convert.parseHexString((String) transactionData.get("signature"));
      Long versionValue = (Long) transactionData.get("version");
      byte version = versionValue == null ? 0 : versionValue.byteValue();
      JSONObject attachmentData = (JSONObject) transactionData.get("attachment");
      if(attachmentData == null) {
        attachmentData = new JSONObject();
      }

      TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
      if (transactionType == null) {
        throw new BurstException.NotValidException("Invalid transaction type: " + type + ", " + subtype);
      }
      Transaction.Builder builder = new Transaction.Builder(version, senderPublicKey,
                                                                            amountNQT, feeNQT, timestamp, deadline,
                                                                            transactionType.parseAttachment(attachmentData))
          .referencedTransactionFullHash(referencedTransactionFullHash)
          .signature(signature);
      if (transactionType.hasRecipient()) {
        long recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
        builder.recipientId(recipientId);
      }

      builder.message(Appendix.Message.parse(attachmentData));
      builder.encryptedMessage(Appendix.EncryptedMessage.parse(attachmentData));
      builder.publicKeyAnnouncement((Appendix.PublicKeyAnnouncement.parse(attachmentData)));
      builder.encryptToSelfMessage(Appendix.EncryptToSelfMessage.parse(attachmentData));

      if (version > 0) {
        builder.ecBlockHeight(((Long) transactionData.get("ecBlockHeight")).intValue());
        builder.ecBlockId(Convert.parseUnsignedLong((String) transactionData.get("ecBlockId")));
      }
      return builder.build();
    } catch (BurstException.NotValidException|RuntimeException e) {
      logger.debug("Failed to parse transaction: " + transactionData.toJSONString());
      throw e;
    }
  }


  public int getECBlockHeight() {
    return ecBlockHeight;
  }

  public long getECBlockId() {
    return ecBlockId;
  }

  public void sign(String secretPhrase) {
    if (signature != null) {
      throw new IllegalStateException("Transaction already signed");
    }
    signature = Crypto.sign(getBytes(), secretPhrase);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Transaction && this.getId() == ((Transaction)o).getId();
  }

  @Override
  public int hashCode() {
    return (int)(getId() ^ (getId() >>> 32));
  }

  public int compareTo(Transaction other) {
    return Long.compare(this.getId(), other.getId());
  }

  public boolean verifySignature() {
    byte[] data = zeroSignature(getBytes());
    return Crypto.verify(signature, data, senderPublicKey, useNQT());
  }

  public int getSize() {
    return signatureOffset() + 64  + (version > 0 ? 4 + 4 + 8 : 0) + appendagesSize;
  }

  public int getAppendagesSize() {
    return appendagesSize;
  }

  private int signatureOffset() {
    return 1 + 1 + 4 + 2 + 32 + 8 + (useNQT() ? 8 + 8 + 32 : 4 + 4 + 8);
  }

  private boolean useNQT() {
    return this.height > Constants.NQT_BLOCK
        && (this.height < Integer.MAX_VALUE
            || Burst.getBlockchain().getHeight() >= Constants.NQT_BLOCK);
  }

  private byte[] zeroSignature(byte[] data) {
    int start = signatureOffset();
    for (int i = start; i < start + 64; i++) {
      data[i] = 0;
    }
    return data;
  }

  private int getFlags() {
    int flags = 0;
    int position = 1;
    if (message != null) {
      flags |= position;
    }
    position <<= 1;
    if (encryptedMessage != null) {
      flags |= position;
    }
    position <<= 1;
    if (publicKeyAnnouncement != null) {
      flags |= position;
    }
    position <<= 1;
    if (encryptToSelfMessage != null) {
      flags |= position;
    }
    return flags;
  }

  boolean isDuplicate(Map<TransactionType, Set<String>> duplicates) {
    return type.isDuplicate(this, duplicates);
  }

}
