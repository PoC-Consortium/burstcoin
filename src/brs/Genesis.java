package brs;

public final class Genesis {

  public static final long GENESIS_BLOCK_ID = 3444294670862540038L;
  public static final long CREATOR_ID = 0L;

  private static final byte[] CREATOR_PUBLIC_KEY = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
  };

  private static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[]{
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
  };

  public static byte[] getCreatorPublicKey() {
    return CREATOR_PUBLIC_KEY.clone();
  }

  public static byte[] getGenesisBlockSignature() {
    return GENESIS_BLOCK_SIGNATURE.clone();
  }

  private Genesis() {} // never

}
