package integration.common;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class BlockMessageBuilder {

  private long payloadLength;
  private long totalAmountNQT;
  private long version;
  private String nonce;
  private long totalFeeNQT;
  private String blockATs;
  private String previousBlock;
  private String generationSignature;
  private String generatorPublicKey;
  private String payloadHash;
  private String blockSignature;
  private JSONArray transactions = new JSONArray();
  private long timestamp;
  private String previousBlockHash;

  public BlockMessageBuilder payloadLength(long payloadLength) {
    this.payloadLength = payloadLength;
    return this;
  }

  public BlockMessageBuilder totalAmountNQT(long totalAmountNQT) {
    this.totalAmountNQT = totalAmountNQT;
    return this;
  }

  public BlockMessageBuilder version(long version) {
    this.version = version;
    return this;
  }

  public BlockMessageBuilder nonce(String nonce) {
    this.nonce = nonce;
    return this;
  }

  public BlockMessageBuilder totalFeeNQT(long totalFeeNQT) {
    this.totalFeeNQT = totalFeeNQT;
    return this;
  }

  public BlockMessageBuilder blockATs(String blockATs) {
    this.blockATs = blockATs;
    return this;
  }

  public BlockMessageBuilder previousBlock(String previousBlock) {
    this.previousBlock = previousBlock;
    return this;
  }

  public BlockMessageBuilder generationSignature(String generationSignature) {
    this.generationSignature = generationSignature;
    return this;
  }

  public BlockMessageBuilder generatorPublicKey(String generatorPublicKey) {
    this.generatorPublicKey = generatorPublicKey;
    return this;
  }

  public BlockMessageBuilder payloadHash(String payloadHash) {
    this.payloadHash = payloadHash;
    return this;
  }

  public BlockMessageBuilder blockSignature(String blockSignature) {
    this.blockSignature = blockSignature;
    return this;
  }

  public BlockMessageBuilder transactions(JSONArray transactions) {
    if(transactions == null) {
      this.transactions = new JSONArray();
    } else {
      this.transactions = transactions;
    }
    return this;
  }

  public BlockMessageBuilder timestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public BlockMessageBuilder previousBlockHash(String previousBlockHash) {
    this.previousBlockHash = previousBlockHash;
    return this;
  }

  public JSONObject toJson() {
    final Map overview = new HashMap();

    overview.put("payloadLength", payloadLength);
    overview.put("totalAmountNQT", totalAmountNQT);
    overview.put("version", version);
    overview.put("nonce", nonce);
    overview.put("totalFeeNQT", totalFeeNQT);
    overview.put("blockATs", blockATs);
    overview.put("previousBlock", previousBlock);
    overview.put("generationSignature", generationSignature);
    overview.put("generatorPublicKey", generatorPublicKey);
    overview.put("payloadHash", payloadHash);
    overview.put("blockSignature", blockSignature);
    overview.put("transactions", transactions);
    overview.put("timestamp", timestamp);
    overview.put("previousBlockHash", previousBlockHash);

    return new JSONObject(overview);
  }
}
