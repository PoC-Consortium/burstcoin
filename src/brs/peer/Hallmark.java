package brs.peer;

import brs.Account;
import brs.Constants;
import brs.crypto.Crypto;
import brs.util.Convert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

public final class Hallmark {

  public static int parseDate(String dateValue) {
    return Integer.parseInt(dateValue.substring(0, 4)) * 10000
        + Integer.parseInt(dateValue.substring(5, 7)) * 100
        + Integer.parseInt(dateValue.substring(8, 10));
  }

  public static String formatDate(int date) {
    int year = date / 10000;
    int month = (date % 10000) / 100;
    int day = date % 100;
    return (year < 10 ? "000" : (year < 100 ? "00" : (year < 1000 ? "0" : ""))) + year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day;
  }

  public static String generateHallmark(String secretPhrase, String host, int weight, int date) {

    if (host.length() == 0 || host.length() > 100) {
      throw new IllegalArgumentException("Hostname length should be between 1 and 100");
    }
    if (weight <= 0 || weight > Constants.MAX_BALANCE_NXT) {
      throw new IllegalArgumentException("Weight should be between 1 and " + Constants.MAX_BALANCE_NXT);
    }

    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    byte[] hostBytes = Convert.toBytes(host);

    ByteBuffer buffer = ByteBuffer.allocate(32 + 2 + hostBytes.length + 4 + 4 + 1);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(publicKey);
    buffer.putShort((short)hostBytes.length);
    buffer.put(hostBytes);
    buffer.putInt(weight);
    buffer.putInt(date);

    byte[] data = buffer.array();
    data[data.length - 1] = (byte) ThreadLocalRandom.current().nextInt();
    byte[] signature = Crypto.sign(data, secretPhrase);

    return Convert.toHexString(data) + Convert.toHexString(signature);

  }

  public static Hallmark parseHallmark(String hallmarkString) {

    byte[] hallmarkBytes = Convert.parseHexString(hallmarkString);

    ByteBuffer buffer = ByteBuffer.wrap(hallmarkBytes);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    byte[] publicKey = new byte[32];
    buffer.get(publicKey);
    int hostLength = buffer.getShort();
    if (hostLength > 300) {
      throw new IllegalArgumentException("Invalid host length");
    }
    byte[] hostBytes = new byte[hostLength];
    buffer.get(hostBytes);
    String host = Convert.toString(hostBytes);
    int weight = buffer.getInt();
    int date = buffer.getInt();
    buffer.get();
    byte[] signature = new byte[64];
    buffer.get(signature);

    byte[] data = new byte[hallmarkBytes.length - 64];
    System.arraycopy(hallmarkBytes, 0, data, 0, data.length);

    boolean isValid = host.length() < 100 && weight > 0 && weight <= Constants.MAX_BALANCE_NXT
        && Crypto.verify(signature, data, publicKey, true);

    return new Hallmark(hallmarkString, publicKey, signature, host, weight, date, isValid);

  }

  private final String hallmarkString;
  private final String host;
  private final int weight;
  private final int date;
  private final byte[] publicKey;
  private final long accountId;
  private final byte[] signature;
  private final boolean isValid;

  private Hallmark(String hallmarkString, byte[] publicKey, byte[] signature, String host, int weight, int date, boolean isValid) {
    this.hallmarkString = hallmarkString;
    this.host = host;
    this.publicKey = publicKey;
    this.accountId = Account.getId(publicKey);
    this.signature = signature;
    this.weight = weight;
    this.date = date;
    this.isValid = isValid;
  }

  public String getHallmarkString() {
    return hallmarkString;
  }

  public String getHost() {
    return host;
  }

  public int getWeight() {
    return weight;
  }

  public int getDate() {
    return date;
  }

  public byte[] getSignature() {
    return signature;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public long getAccountId() {
    return accountId;
  }

  public boolean isValid() {
    return isValid;
  }

}
