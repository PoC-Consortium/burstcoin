package brs;

import java.beans.ConstructorProperties;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brs.crypto.Crypto;
import brs.peer.Peer;
import brs.util.Convert;

@Entity
public final class BlockImpl implements Block {

  private static final Logger logger = LoggerFactory.getLogger(BlockImpl.class);
  private final TransactionDb transactionDb = Burst.getDbs().getTransactionDb();
  @Column(name = "VERSION")
  private final int version;
  @Column(name = "TIMESTAMP")
  private final int timestamp;
  @Column(name = "PREVIOUS_BLOCK_ID")
  private final long previousBlockId;
  @Column(name = "GENERATOR_PUBLIC_KEY")
  private final byte[] generatorPublicKey;
  @Column(name = "PREVIOUS_BLOCK_HASH")
  private final byte[] previousBlockHash;
  @Column(name = "TOTAL_AMOUNT")
  private final long totalAmountNQT;
  @Column(name = "TOTAL_FEE")
  private final long totalFeeNQT;
  @Column(name = "PAYLOAD_LENGTH")
  private final int payloadLength;
  @Column(name = "GENERATION_SIGNATURE")
  private final byte[] generationSignature;
  @Column(name = "PAYLOAD_HASH")
  private final byte[] payloadHash;
  private volatile List<TransactionImpl> blockTransactions;

  @Column(name = "BLOCK_SIGNATURE")
  private byte[] blockSignature;

  @Column(name = "CUMULATIVE_DIFFICULTY")
  //@javax.persistence.Convert(converter = BigIntegerConverter.class)
  //private BigInteger cumulativeDifficulty = BigInteger.ZERO;
  private byte[] cumulativeDifficulty = new byte[0];
  @Column(name = "BASE_TARGET")
  private long baseTarget = Constants.INITIAL_BASE_TARGET;
  @Column(name = "NEXT_BLOCK_ID")
  private volatile long nextBlockId;
  @Column(name = "HEIGHT")
  private int height = -1;
  @Column(name = "ID")
  private volatile long id;
  private volatile String stringId = null;
  private volatile long generatorId;
  @Column(name = "NONCE")
  private long nonce;

  private BigInteger pocTime = null;

  @Column(name = "ATS")
  private final byte[] blockATs;

  private Peer downloadedFrom = null;
  private int byteLength = 0;

  BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions, long nonce,
      byte[] blockATs) throws BurstException.ValidationException {

    if (payloadLength > Constants.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
      throw new BurstException.NotValidException("attempted to create a block with payloadLength " + payloadLength);
    }

    this.version = version;
    this.timestamp = timestamp;
    this.previousBlockId = Optional.ofNullable(previousBlockId).orElse(0L);
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
        throw new BurstException.NotValidException("attempted to create a block with " + blockTransactions.size() + " transactions");
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

  @ConstructorProperties({ "version", "timestamp", "previous_block_id", "total_amount", "total_fee", "payload_length", "payload_hash", "generator_public_key", "generation_signature", "block_signature", "previous_block_hash", "cumulative_difficulty", "base_target", "next_block_id", "height", "id",
      "nonce", "ats" })
  public BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash, byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, byte[] cumulativeDifficulty, long baseTarget,
      long nextBlockId, int height, Long id, long nonce, byte[] blockATs) throws BurstException.ValidationException {

    this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash, null, nonce, blockATs);

    this.cumulativeDifficulty = cumulativeDifficulty == null ? BigInteger.ZERO.toByteArray() : cumulativeDifficulty;
    this.baseTarget = baseTarget;
    this.nextBlockId = Optional.ofNullable(nextBlockId).orElse(0L);
    this.height = height;
    this.id = id;
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

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public int getTimestamp() {
    return timestamp;
  }

  @Override
  public long getPreviousBlockId() {
    return previousBlockId;
  }

  @Override
  public byte[] getGeneratorPublicKey() {
    return generatorPublicKey;
  }

  @Override
  public byte[] getBlockHash() {
    return Crypto.sha256().digest(getBytes());
  }

  @Override
  public byte[] getPreviousBlockHash() {
    return previousBlockHash;
  }

  @Override
  public long getTotalAmountNQT() {
    return totalAmountNQT;
  }

  @Override
  public long getTotalFeeNQT() {
    return totalFeeNQT;
  }

  @Override
  public int getPayloadLength() {
    return payloadLength;
  }

  @Override
  public byte[] getPayloadHash() {
    return payloadHash;
  }

  @Override
  public byte[] getGenerationSignature() {
    return generationSignature;
  }

  @Override
  public byte[] getBlockSignature() {
    return blockSignature;
  }

  @Override
  public List<TransactionImpl> getTransactions() {
    if (blockTransactions == null) {
      this.blockTransactions = Collections.unmodifiableList(transactionDb.findBlockTransactions(getId()));
      this.blockTransactions.forEach(transaction -> transaction.setBlock(this));
    }
    return blockTransactions;
  }

  @Override
  public long getBaseTarget() {
    return baseTarget;
  }

  @Override
  public BigInteger getCumulativeDifficulty() {
    return new BigInteger(cumulativeDifficulty);
  }

  @Override
  public long getNextBlockId() {
    return nextBlockId;
  }

  @Override
  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  @Override
  public long getId() {
    if (id == 0) {
      if (blockSignature == null) {
        throw new IllegalStateException("Block is not signed yet");
      }
      byte[] hash = Crypto.sha256().digest(getBytes());
      BigInteger bigInteger = new BigInteger(1, new byte[] { hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0] });
      id = bigInteger.longValue();
      stringId = bigInteger.toString();
    }
    return id;
  }

  @Override
  public String getStringId() {
    if (stringId == null) {
      getId();
      if (stringId == null) {
        stringId = Convert.toUnsignedLong(id);
      }
    }
    return stringId;
  }

  @Override
  public long getGeneratorId() {
    if (generatorId == 0) {
      generatorId = Account.getId(generatorPublicKey);
    }
    return generatorId;
  }

  @Override
  public Long getNonce() {
    return nonce;
  }

  @Override
  public int getScoopNum() {
    return Burst.getGenerator().calculateScoop(generationSignature, getHeight());
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof BlockImpl && this.getId() == ((BlockImpl) o).getId();
  }

  @Override
  public int hashCode() {
    return (int) (getId() ^ (getId() >>> 32));
  }

  @Override
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

  static BlockImpl parseBlock(JSONObject blockData) throws BurstException.ValidationException {
    try {
      int version = ((Long) blockData.get("version")).intValue();
      int timestamp = ((Long) blockData.get("timestamp")).intValue();
      Long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
      long totalAmountNQT = Convert.parseLong(blockData.get("totalAmountNQT"));
      long totalFeeNQT = Convert.parseLong(blockData.get("totalFeeNQT"));
      int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
      byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
      byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
      byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
      byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
      byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
      Long nonce = Convert.parseUnsignedLong((String) blockData.get("nonce"));

      SortedMap<Long, TransactionImpl> blockTransactions = new TreeMap<>();
      JSONArray transactionsData = (JSONArray) blockData.get("transactions");
      for (Object transactionData : transactionsData) {
        TransactionImpl transaction = TransactionImpl.parseTransaction((JSONObject) transactionData);
        if (blockTransactions.put(transaction.getId(), transaction) != null) {
          throw new BurstException.NotValidException("Block contains duplicate transactions: " + transaction.getStringId());
        }
      }
      byte[] blockATs = Convert.parseHexString((String) blockData.get("blockATs"));
      return new BlockImpl(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey, generationSignature, blockSignature, previousBlockHash, new ArrayList<>(blockTransactions.values()), nonce, blockATs);
    } catch (BurstException.ValidationException | RuntimeException e) {
      logger.debug("Failed to parse block: " + blockData.toJSONString());
      throw e;
    }
  }

  byte[] getBytes() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (version < 3 ? (4 + 4) : (8 + 8)) + 4 + 32 + 32 + (32 + 32) + 8 + (blockATs != null ? blockATs.length : 0) + 64);
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
      logger.error("Something is too large here - buffer should have " + blockSignature.length + " bytes left but only has " + (buffer.limit() - buffer.position()));
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

  boolean verifyBlockSignature() throws BlockchainProcessor.BlockOutOfOrderException {

    try {

      BlockImpl previousBlock = (BlockImpl) Burst.getBlockchain().getBlock(this.previousBlockId);
      if (previousBlock == null) {
        throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing");
      }

      byte[] data = getBytes();
      byte[] data2 = new byte[data.length - 64];
      System.arraycopy(data, 0, data2, 0, data2.length);

      byte[] publicKey;
      Account genAccount = Account.getAccount(generatorPublicKey);
      Account.RewardRecipientAssignment rewardAssignment;
      rewardAssignment = genAccount == null ? null : genAccount.getRewardRecipientAssignment();
      if (genAccount == null || rewardAssignment == null || previousBlock.getHeight() + 1 < Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_START_BLOCK) {
        publicKey = generatorPublicKey;
      } else {
        if (previousBlock.getHeight() + 1 >= rewardAssignment.getFromHeight()) {
          publicKey = Account.getAccount(rewardAssignment.getRecipientId()).getPublicKey();
        } else {
          publicKey = Account.getAccount(rewardAssignment.getPrevRecipientId()).getPublicKey();
        }
      }

      return Crypto.verify(blockSignature, data2, publicKey, version >= 3);

    } catch (RuntimeException e) {

      logger.info("Error verifying block signature", e);
      return false;

    }

  }

  boolean verifyGenerationSignature() throws BlockchainProcessor.BlockNotAcceptedException {
    try {
      BlockImpl previousBlock = (BlockImpl) Burst.getBlockchain().getBlock(this.previousBlockId);

      if (previousBlock == null) {
        throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify generation signature because previous block is missing");
      }

      // In case the verifier-Threads are not done with this yet - do it yourself.
      synchronized (this) {
        if (this.pocTime == null)
          preVerify();
      }

      byte[] correctGenerationSignature = Burst.getGenerator().calculateGenerationSignature(previousBlock.getGenerationSignature(), previousBlock.getGeneratorId());
      if (!Arrays.equals(generationSignature, correctGenerationSignature)) {
        return false;
      }
      int elapsedTime = timestamp - previousBlock.timestamp;
      BigInteger pTime = this.pocTime.divide(BigInteger.valueOf(previousBlock.getBaseTarget()));
      return BigInteger.valueOf(elapsedTime).compareTo(pTime) > 0;
    } catch (RuntimeException e) {
      logger.info("Error verifying block generation signature", e);
      return false;
    }
  }

  public void preVerify() throws BlockchainProcessor.BlockNotAcceptedException {
    preVerify(null);
  }

  public void preVerify(byte[] scoopData) throws BlockchainProcessor.BlockNotAcceptedException {
    synchronized (this) {
      // Remove from todo-list:
      synchronized (BlockchainProcessorImpl.DownloadCache) {
        BlockchainProcessorImpl.DownloadCache.removeUnverified(this.getId());
      }

      // Just in case its already verified
      if (this.pocTime != null)
        return;

      try {
        // Pre-verify poc:
        if (scoopData == null) {
          this.pocTime = Burst.getGenerator().calculateHit(getGeneratorId(), nonce, generationSignature, getScoopNum(), this.getHeight());
        } else {
          this.pocTime = Burst.getGenerator().calculateHit(getGeneratorId(), nonce, generationSignature, scoopData);
        }
      } catch (RuntimeException e) {
        logger.info("Error pre-verifying block generation signature", e);
        return;
      }

      for (TransactionImpl transaction : getTransactions()) {
        if (!transaction.verifySignature()) {
          logger.info("Bad transaction signature during block pre-verification for tx: " + Convert.toUnsignedLong(transaction.getId()) + " at block height: " + getHeight());
          throw new BlockchainProcessor.TransactionNotAcceptedException("Invalid signature for tx: " + Convert.toUnsignedLong(transaction.getId()) + "at block height: " + getHeight(), transaction);
        }
      }
    }
  }

  void apply() {
    Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
    generatorAccount.apply(generatorPublicKey, this.height);
    if (height < Constants.BURST_REWARD_RECIPIENT_ASSIGNMENT_START_BLOCK) {
      generatorAccount.addToBalanceAndUnconfirmedBalanceNQT(totalFeeNQT + getBlockReward());
      generatorAccount.addToForgedBalanceNQT(totalFeeNQT + getBlockReward());
    } else {
      Account rewardAccount;
      Account.RewardRecipientAssignment rewardAssignment = generatorAccount.getRewardRecipientAssignment();
      if (rewardAssignment == null) {
        rewardAccount = generatorAccount;
      } else if (height >= rewardAssignment.getFromHeight()) {
        rewardAccount = Account.getAccount(rewardAssignment.getRecipientId());
      } else {
        rewardAccount = Account.getAccount(rewardAssignment.getPrevRecipientId());
      }
      rewardAccount.addToBalanceAndUnconfirmedBalanceNQT(totalFeeNQT + getBlockReward());
      rewardAccount.addToForgedBalanceNQT(totalFeeNQT + getBlockReward());
    }
    getTransactions().forEach(transaction -> transaction.apply());
  }

  @Override
  public long getBlockReward() {
    if (this.height == 0 || this.height >= 1944000) {
      return 0;
    }
    int month = this.height / 10800;
    return BigInteger.valueOf(10000).multiply(BigInteger.valueOf(95).pow(month)).divide(BigInteger.valueOf(100).pow(month)).longValue() * Constants.ONE_BURST;
  }

  void setPrevious(BlockImpl previousBlock) {
    if (previousBlock != null) {
      if (previousBlock.getId() != getPreviousBlockId()) {
        // shouldn't happen as previous id is already verified, but just in case
        throw new IllegalStateException("Previous block id doesn't match");
      }
      this.height = previousBlock.getHeight() + 1;
      this.calculateBaseTarget(previousBlock);
    } else {
      this.height = 0;
    }
    getTransactions().forEach(transaction -> transaction.setBlock(this));
  }

  public void calculateBaseTarget(BlockImpl previousBlock) {

    if (this.getId() == Genesis.GENESIS_BLOCK_ID && previousBlockId == 0) {
      baseTarget = Constants.INITIAL_BASE_TARGET;
      cumulativeDifficulty = BigInteger.ZERO.toByteArray();
    } else if (this.height < 4) {
      baseTarget = Constants.INITIAL_BASE_TARGET;
      cumulativeDifficulty = previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET))).toByteArray();
    } else if (this.height < Constants.BURST_DIFF_ADJUST_CHANGE_BLOCK) {
      Block itBlock = previousBlock;
      BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
      do {
        itBlock = BlockchainProcessorImpl.DownloadCache.GetBlock(itBlock.getPreviousBlockId());
        avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(itBlock.getBaseTarget()));
      } while (itBlock.getHeight() > this.height - 4);
      avgBaseTarget = avgBaseTarget.divide(BigInteger.valueOf(4));
      long difTime = (long) this.timestamp - itBlock.getTimestamp();

      long curBaseTarget = avgBaseTarget.longValue();
      long newBaseTarget = BigInteger.valueOf(curBaseTarget).multiply(BigInteger.valueOf(difTime)).divide(BigInteger.valueOf(240L * 4)).longValue();
      if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
        newBaseTarget = Constants.MAX_BASE_TARGET;
      }
      if (newBaseTarget < (curBaseTarget * 9 / 10)) {
        newBaseTarget = curBaseTarget * 9 / 10;
      }
      if (newBaseTarget == 0) {
        newBaseTarget = 1;
      }
      long twofoldCurBaseTarget = curBaseTarget * 11 / 10;
      if (twofoldCurBaseTarget < 0) {
        twofoldCurBaseTarget = Constants.MAX_BASE_TARGET;
      }
      if (newBaseTarget > twofoldCurBaseTarget) {
        newBaseTarget = twofoldCurBaseTarget;
      }
      baseTarget = newBaseTarget;
      cumulativeDifficulty = previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(baseTarget))).toByteArray();
    } else {
      Block itBlock = previousBlock;
      BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
      int blockCounter = 1;
      do {
        itBlock = BlockchainProcessorImpl.DownloadCache.GetBlock(itBlock.getPreviousBlockId());
        blockCounter++;
        avgBaseTarget = (avgBaseTarget.multiply(BigInteger.valueOf(blockCounter)).add(BigInteger.valueOf(itBlock.getBaseTarget()))).divide(BigInteger.valueOf(blockCounter + 1L));
      } while (blockCounter < 24);
      long difTime = (long) this.timestamp - itBlock.getTimestamp();
      long targetTimespan = 24L * 4 * 60;

      if (difTime < targetTimespan / 2) {
        difTime = targetTimespan / 2;
      }

      if (difTime > targetTimespan * 2) {
        difTime = targetTimespan * 2;
      }

      long curBaseTarget = previousBlock.getBaseTarget();
      long newBaseTarget = avgBaseTarget.multiply(BigInteger.valueOf(difTime)).divide(BigInteger.valueOf(targetTimespan)).longValue();

      if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
        newBaseTarget = Constants.MAX_BASE_TARGET;
      }

      if (newBaseTarget == 0) {
        newBaseTarget = 1;
      }

      if (newBaseTarget < curBaseTarget * 8 / 10) {
        newBaseTarget = curBaseTarget * 8 / 10;
      }

      if (newBaseTarget > curBaseTarget * 12 / 10) {
        newBaseTarget = curBaseTarget * 12 / 10;
      }

      baseTarget = newBaseTarget;
      cumulativeDifficulty = previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(baseTarget))).toByteArray();
    }
  }

  @Override
  public byte[] getBlockATs() {
    return blockATs;
  }

}
