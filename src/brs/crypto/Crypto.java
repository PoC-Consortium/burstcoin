package brs.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brs.util.Convert;

public final class Crypto {

  private static final Logger logger = LoggerFactory.getLogger(Crypto.class);

  private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
    @Override
    protected SecureRandom initialValue() {
      return new SecureRandom();
    }
  };

  private Crypto() {
  } //never

  public static MessageDigest getMessageDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      logger.info("Missing message digest algorithm: " + algorithm);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static MessageDigest sha256() {
    return getMessageDigest("SHA-256");
  }

  public static byte[] getPublicKey(String secretPhrase) {
    byte[] publicKey = new byte[32];
    Curve25519.keygen(publicKey, null, Crypto.sha256().digest(Convert.toBytes(secretPhrase)));

    return publicKey;
  }

  public static byte[] getPrivateKey(String secretPhrase) {
    byte[] s = Crypto.sha256().digest(Convert.toBytes(secretPhrase));
    Curve25519.clamp(s);
    return s;
  }

  public static void curve(byte[] Z, byte[] k, byte[] P) {
    Curve25519.curve(Z, k, P);
  }

  public static byte[] sign(byte[] message, String secretPhrase) {

    byte[] P = new byte[32];
    byte[] s = new byte[32];
    MessageDigest digest = Crypto.sha256();
    Curve25519.keygen(P, s, digest.digest(Convert.toBytes(secretPhrase)));
    byte[] m = digest.digest(message);

    digest.update(m);
    byte[] x = digest.digest(s);

    byte[] Y = new byte[32];
    Curve25519.keygen(Y, null, x);

    digest.update(m);
    byte[] h = digest.digest(Y);

    byte[] v = new byte[32];
    Curve25519.sign(v, h, x, s);

    byte[] signature = new byte[64];
    System.arraycopy(v, 0, signature, 0, 32);
    System.arraycopy(h, 0, signature, 32, 32);

    return signature;
  }

  public static boolean verify(byte[] signature, byte[] message, byte[] publicKey, boolean enforceCanonical) {

    if (enforceCanonical && !Curve25519.isCanonicalSignature(signature)) {
      logger.debug("Rejecting non-canonical signature");
      return false;
    }

    if (enforceCanonical && !Curve25519.isCanonicalPublicKey(publicKey)) {
      logger.debug("Rejecting non-canonical public key");
      return false;
    }

    byte[] Y = new byte[32];
    byte[] v = new byte[32];
    System.arraycopy(signature, 0, v, 0, 32);
    byte[] h = new byte[32];
    System.arraycopy(signature, 32, h, 0, 32);
    Curve25519.verify(Y, v, h, publicKey);

    MessageDigest digest = Crypto.sha256();
    byte[] m = digest.digest(message);
    digest.update(m);
    byte[] h2 = digest.digest(Y);

    return Arrays.equals(h, h2);
  }

  public static byte[] aesEncrypt(byte[] plaintext, byte[] myPrivateKey, byte[] theirPublicKey) {
    return aesEncrypt(plaintext, myPrivateKey, theirPublicKey, new byte[32]);
  }

  public static byte[] aesEncrypt(byte[] plaintext, byte[] myPrivateKey, byte[] theirPublicKey, byte[] nonce) {
    try {
      byte[] dhSharedSecret = new byte[32];
      Curve25519.curve(dhSharedSecret, myPrivateKey, theirPublicKey);
      for (int i = 0; i < 32; i++) {
        dhSharedSecret[i] ^= nonce[i];
      }
      byte[] key = sha256().digest(dhSharedSecret);
      byte[] iv = new byte[16];
      secureRandom.get().nextBytes(iv);
      PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
      CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
      aes.init(true, ivAndKey);
      byte[] output = new byte[aes.getOutputSize(plaintext.length)];
      int ciphertextLength = aes.processBytes(plaintext, 0, plaintext.length, output, 0);
      ciphertextLength += aes.doFinal(output, ciphertextLength);
      byte[] result = new byte[iv.length + ciphertextLength];
      System.arraycopy(iv, 0, result, 0, iv.length);
      System.arraycopy(output, 0, result, iv.length, ciphertextLength);
      return result;
    } catch (InvalidCipherTextException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static byte[] aesDecrypt(byte[] ivCiphertext, byte[] myPrivateKey, byte[] theirPublicKey) {
    return aesDecrypt(ivCiphertext, myPrivateKey, theirPublicKey, new byte[32]);
  }

  public static byte[] aesDecrypt(byte[] ivCiphertext, byte[] myPrivateKey, byte[] theirPublicKey, byte[] nonce) {
    try {
      if (ivCiphertext.length < 16 || ivCiphertext.length % 16 != 0) {
        throw new InvalidCipherTextException("invalid ciphertext");
      }
      byte[] iv = Arrays.copyOfRange(ivCiphertext, 0, 16);
      byte[] ciphertext = Arrays.copyOfRange(ivCiphertext, 16, ivCiphertext.length);
      byte[] dhSharedSecret = new byte[32];
      Curve25519.curve(dhSharedSecret, myPrivateKey, theirPublicKey);
      for (int i = 0; i < 32; i++) {
        dhSharedSecret[i] ^= nonce[i];
      }
      byte[] key = sha256().digest(dhSharedSecret);
      PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
      CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
      aes.init(false, ivAndKey);
      byte[] output = new byte[aes.getOutputSize(ciphertext.length)];
      int plaintextLength = aes.processBytes(ciphertext, 0, ciphertext.length, output, 0);
      plaintextLength += aes.doFinal(output, plaintextLength);
      byte[] result = new byte[plaintextLength];
      System.arraycopy(output, 0, result, 0, result.length);
      return result;
    } catch (InvalidCipherTextException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static byte[] getSharedSecret(byte[] myPrivateKey, byte[] theirPublicKey) {
    try {
      byte[] sharedSecret = new byte[32];
      Curve25519.curve(sharedSecret, myPrivateKey, theirPublicKey);
      return sharedSecret;
    } catch (RuntimeException e) {
      logger.info("Error getting shared secret", e);
      throw e;
    }
  }

  public static String rsEncode(long id) {
    return ReedSolomon.encode(id);
  }

  public static long rsDecode(String rsString) {
    rsString = rsString.toUpperCase();
    try {
      long id = ReedSolomon.decode(rsString);
      if (!rsString.equals(ReedSolomon.encode(id))) {
        throw new RuntimeException("ERROR: Reed-Solomon decoding of " + rsString + " not reversible, decoded to " + id);
      }
      return id;
    } catch (ReedSolomon.DecodeException e) {
      logger.debug("Reed-Solomon decoding failed for " + rsString + ": " + e.toString());
      throw new RuntimeException(e.toString(), e);
    }
  }

}
