package brs;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.Entity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import brs.crypto.Crypto;
import brs.peer.Peer;
import brs.util.Convert;

@Entity
public class Block {

  private static final Logger logger = LoggerFactory.getLogger(Block.class);
  private final int version;
  private final int timestamp;
  private final long previousBlockId;
  private final byte[] generatorPublicKey;
  private final byte[] previousBlockHash;
  private final long totalAmountNQT;
  private final long totalFeeNQT;
  private final int payloadLength;
  private final byte[] generationSignature;
  private final byte[] payloadHash;
  private volatile List<Transaction> blockTransactions;

  private byte[] blockSignature;

  private BigInteger cumulativeDifficulty = BigInteger.ZERO;

  private long baseTarget = Constants.INITIAL_BASE_TARGET;
  private volatile long nextBlockId;
  private int height = -1;
  private volatile long id;
  private volatile String stringId = null;
  private volatile long generatorId;
  private long nonce;

  private BigInteger pocTime = null;

  private final byte[] blockATs;

  private Peer downloadedFrom = null;
  private int byteLength = 0;

  Block(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT,
      int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature,
      byte[] blockSignature, byte[] previousBlockHash, List<Transaction> transactions,
      long nonce, byte[] blockATs) throws BurstException.ValidationException {

    if (payloadLength > Constants.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
      throw new BurstException.NotValidException(
          "attempted to create a block with payloadLength " + payloadLength);
    }

    this.version = version;
    this.timestamp = timestamp;
    this.previousBlockId = previousBlockId;
    this.totalAmountNQT = totalAmountNQT;
    this.totalFeeNQT = totalFeeNQT;
    this.payloadLength = payloadLength;
    this.payloadHash = payloadHash;
    this.generatorPublicKey = generatorPublicKey;
    this.generationSignature = generationSignature;
    this.blockSignature = blockSignature;

    this.previousBlockHash = previousBlockHash;
    if (transactions != null) {
      this.blockTransactions = Collections.unmodifiableList(transactions);
      if (blockTransactions.size() > Constants.MAX_NUMBER_OF_TRANSACTIONS) {
        throw new BurstException.NotValidException(
            "attempted to create a block with " + blockTransactions.size() + " transactions");
      }
      long previousId = 0;
      for (Transaction transaction : this.blockTransactions) {
        if (transaction.getId() <= previousId && previousId != 0) {
          throw new BurstException.NotValidException("Block transactions are not sorted!");
        }
        previousId = transaction.getId();
      }
    }
    this.nonce = nonce;
    this.blockATs = blockATs;
  }

  public Block(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget,
      long nextBlockId, int height, Long id, long nonce, byte[] blockATs) throws BurstException.ValidationException {

    this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash, null, nonce, blockATs);

    this.cumulativeDifficulty = cumulativeDifficulty == null ? BigInteger.ZERO : cumulativeDifficulty;
    this.baseTarget = baseTarget;
    this.nextBlockId = nextBlockId;
    this.height = height;
    this.id = id;
  }

  private final TransactionDb transactionDb() {
    return Burst.getDbs().getTransactionDb();
  }

  public boolean isVerified() {
    return pocTime != null;
  }

  public void setPeer(Peer peer) {
    this.downloadedFrom = peer;
  }

  public Peer getPeer() {
    return this.downloadedFrom;
  }

  public void setByteLength(int length) {
    this.byteLength = length;
  }

  public int getByteLength() {
    return this.byteLength;
  }

  public int getVersion() {
    return version;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public long getPreviousBlockId() {
    return previousBlockId;
  }

  public byte[] getGeneratorPublicKey() {
    return generatorPublicKey;
  }

  public byte[] getBlockHash() {
    return Crypto.sha256().digest(getBytes());
  }

  public byte[] getPreviousBlockHash() {
    return previousBlockHash;
  }

  public long getTotalAmountNQT() {
    return totalAmountNQT;
  }

  public long getTotalFeeNQT() {
    return totalFeeNQT;
  }

  public int getPayloadLength() {
    return payloadLength;
  }

  public byte[] getPayloadHash() {
    return payloadHash;
  }

  public byte[] getGenerationSignature() {
    return generationSignature;
  }

  public byte[] getBlockSignature() {
    return blockSignature;
  }

  public List<Transaction> getTransactions() {
    if (blockTransactions == null) {
      this.blockTransactions =
          Collections.unmodifiableList(transactionDb().findBlockTransactions(getId()));
      this.blockTransactions.forEach(transaction -> transaction.setBlock(this));
    }
    return blockTransactions;
  }

  public long getBaseTarget() {
    return baseTarget;
  }

  public BigInteger getCumulativeDifficulty() {
    return cumulativeDifficulty;
  }

  public long getNextBlockId() {
    return nextBlockId;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public long getId() {
    if (id == 0) {
      if (blockSignature == null) {
        throw new IllegalStateException("Block is not signed yet");
      }
      byte[] hash = Crypto.sha256().digest(getBytes());
      BigInteger bigInteger = new BigInteger(1,
          new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
      id = bigInteger.longValue();
      stringId = bigInteger.toString();
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

  public long getGeneratorId() {
    if (generatorId == 0) {
      generatorId = Account.getId(generatorPublicKey);
    }
    return generatorId;
  }

  public Long getNonce() {
    return nonce;
  }

  public boolean equals(Object o) {
    return o instanceof Block && this.getId() == ((Block) o).getId();
  }

  public int hashCode() {
    return (int) (getId() ^ (getId() >>> 32));
  }

  public JSONObject getJSONObject() {
    JSONObject json = new JSONObject();
    json.put("version", version);
    json.put("timestamp", timestamp);
    json.put("previousBlock", Convert.toUnsignedLong(previousBlockId));
    json.put("totalAmountNQT", totalAmountNQT);
    json.put("totalFeeNQT", totalFeeNQT);
    json.put("payloadLength", payloadLength);
    json.put("payloadHash", Convert.toHexString(payloadHash));
    json.put("generatorPublicKey", Convert.toHexString(generatorPublicKey));
    json.put("generationSignature", Convert.toHexString(generationSignature));
    if (version > 1) {
      json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
    }
    json.put("blockSignature", Convert.toHexString(blockSignature));
    JSONArray transactionsData = new JSONArray();
    getTransactions().forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
    json.put("transactions", transactionsData);
    json.put("nonce", Convert.toUnsignedLong(nonce));
    json.put("blockATs", Convert.toHexString(blockATs));
    return json;
  }

  static Block parseBlock(JSONObject blockData) throws BurstException.ValidationException {
    try {
      int version = ((Long) blockData.get("version")).intValue();
      int timestamp = ((Long) blockData.get("timestamp")).intValue();
      Long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
      long totalAmountNQT = Convert.parseLong(blockData.get("totalAmountNQT"));
      long totalFeeNQT = Convert.parseLong(blockData.get("totalFeeNQT"));
      int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
      byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
      byte[] generatorPublicKey =
          Convert.parseHexString((String) blockData.get("generatorPublicKey"));
      byte[] generationSignature =
          Convert.parseHexString((String) blockData.get("generationSignature"));
      byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
      byte[] previousBlockHash =
          version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
      Long nonce = Convert.parseUnsignedLong((String) blockData.get("nonce"));

      SortedMap<Long, Transaction> blockTransactions = new TreeMap<>();
      JSONArray transactionsData = (JSONArray) blockData.get("transactions");
    
      for (Object transactionData : transactionsData) {
        Transaction transaction =
                    Transaction.parseTransaction((JSONObject) transactionData);
          if (transaction.getSignature() != null) {
            if (blockTransactions.put(transaction.getId(), transaction) != null) {
              throw new BurstException.NotValidException(
                  "Block contains duplicate transactions: " + transaction.getStringId());
            }
          }
        }
      
      
    
      byte[] blockATs = Convert.parseHexString((String) blockData.get("blockATs"));
      return new Block(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT,
          payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature,
          previousBlockHash, new ArrayList<>(blockTransactions.values()), nonce, blockATs);
    } catch (BurstException.ValidationException | RuntimeException e) {
      logger.debug("Failed to parse block: " + blockData.toJSONString());
      throw e;
    }
  }

  public byte[] getBytes() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (version < 3 ? (4 + 4) : (8 + 8)) + 4
        + 32 + 32 + (32 + 32) + 8 + (blockATs != null ? blockATs.length : 0) + 64);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(version);
    buffer.putInt(timestamp);
    buffer.putLong(previousBlockId);
    buffer.putInt(getTransactions().size());
    if (version < 3) {
      buffer.putInt((int) (totalAmountNQT / Constants.ONE_BURST));
      buffer.putInt((int) (totalFeeNQT / Constants.ONE_BURST));
    } else {
      buffer.putLong(totalAmountNQT);
      buffer.putLong(totalFeeNQT);
    }
    buffer.putInt(payloadLength);
    buffer.put(payloadHash);
    buffer.put(generatorPublicKey);
    buffer.put(generationSignature);
    if (version > 1) {
      buffer.put(previousBlockHash);
    }
    buffer.putLong(nonce);
    if (blockATs != null)
      buffer.put(blockATs);
    if (buffer.limit() - buffer.position() < blockSignature.length)
      logger.error("Something is too large here - buffer should have {} bytes left but only has {}",
                   blockSignature.length,
                   (buffer.limit() - buffer.position()));
    buffer.put(blockSignature);
    return buffer.array();
  }

  void sign(String secretPhrase) {
    if (blockSignature != null) {
      throw new IllegalStateException("Block already signed");
    }
    blockSignature = new byte[64];
    byte[] data = getBytes();
    byte[] data2 = new byte[data.length - 64];
    System.arraycopy(data, 0, data2, 0, data2.length);
    blockSignature = Crypto.sign(data2, secretPhrase);
  }

  public byte[] getBlockATs() {
    return blockATs;
  }

  public BigInteger getPocTime() {
    return pocTime;
  }

  public void setPocTime(BigInteger pocTime) {
    this.pocTime = pocTime;
  }

  public void setBaseTarget(long baseTarget) {
    this.baseTarget = baseTarget;
  }

  public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
    this.cumulativeDifficulty = cumulativeDifficulty;
  }
}
