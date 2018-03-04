package brs.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Blatantly stolen from https://stackoverflow.com/a/25165891
 * @author c3oe.de, based on snippets from Scott Plante, John Kugelmann
 */
public class Subnet
{
  final private int bytesSubnetCount;
  final private BigInteger bigMask;
  final private BigInteger bigSubnetMasked;

  /**
   * For use via format "192.168.0.0/24" or "2001:db8:85a3:880:0:0:0:0/57"
   * @param subnetAddress java.net.InetAddress (IP address)
   * @param bits          subnet mask in /bits notation
   */
  public Subnet(final InetAddress subnetAddress, final int bits)
  {
    this.bytesSubnetCount = subnetAddress.getAddress().length; // 4 or 16
    this.bigMask = BigInteger.valueOf(-1).shiftLeft(this.bytesSubnetCount * 8 - bits); // mask = -1 << 32 - bits
    this.bigSubnetMasked = new BigInteger(subnetAddress.getAddress()).and(this.bigMask);
  }

  /**
   * For use via format "192.168.0.0/255.255.255.0" or single address
   * @param subnetAddress java.net.InetAddress (IP address)
   * @param mask          java.net.InetAddress (IP address, here used as network mask)
   */
  public Subnet(final InetAddress subnetAddress, final InetAddress mask)
  {
    this.bytesSubnetCount = subnetAddress.getAddress().length;
    this.bigMask = null == mask ? BigInteger.valueOf(-1) : new BigInteger(mask.getAddress()); // no mask given case is handled here.
    this.bigSubnetMasked = new BigInteger(subnetAddress.getAddress()).and(this.bigMask);
  }

  /**
   * Subnet factory method.
   *
   * @param subnetMask format: "192.168.0.0/24" or "192.168.0.0/255.255.255.0"
   *                   or single address or "2001:db8:85a3:880:0:0:0:0/57"
   * @return a new instance
   * @throws UnknownHostException thrown if unsupported subnet mask.
   */
  public static Subnet createInstance(final String subnetMask)
    throws UnknownHostException
  {
    final String[] stringArr = subnetMask.split("/");
    if (2 > stringArr.length)
      return new Subnet(InetAddress.getByName(stringArr[0]), null);
    else if (stringArr[1].contains(".") || stringArr[1].contains(":"))
      return new Subnet(InetAddress.getByName(stringArr[0]), InetAddress.getByName(stringArr[1]));
    else
      return new Subnet(InetAddress.getByName(stringArr[0]), Integer.parseInt(stringArr[1]));
  }

  public boolean isInNet(final InetAddress address)
  {
    final byte[] bytesAddress = address.getAddress();
    if (this.bytesSubnetCount != bytesAddress.length)
      return false;
    final BigInteger bigAddress = new BigInteger(bytesAddress);
    return bigAddress.and(this.bigMask).equals(this.bigSubnetMasked);
  }

  @Override
  public final boolean equals(Object obj)
  {
    if (!(obj instanceof Subnet))
      return false;
    final Subnet other = (Subnet) obj;
    return this.bigSubnetMasked.equals(other.bigSubnetMasked) &&
        this.bigMask.equals(other.bigMask) &&
        this.bytesSubnetCount == other.bytesSubnetCount;
  }

  @Override
  public final int hashCode()
  {
    return this.bytesSubnetCount;
  }

  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder();
    bigInteger2IpString(buf, this.bigSubnetMasked, this.bytesSubnetCount);
    buf.append('/');
    bigInteger2IpString(buf, this.bigMask, this.bytesSubnetCount);
    return buf.toString();
  }

  private static void bigInteger2IpString(final StringBuilder buf, final BigInteger bigInteger, final int displayBytes)
  {
    final boolean isIPv4 = 4 == displayBytes;
    byte[] bytes = bigInteger.toByteArray();
    int diffLen = displayBytes - bytes.length;
    final byte fillByte = 0 > (int) bytes[0] ? (byte) 0xFF : (byte) 0x00;

    int integer;
    for (int i = 0; i < displayBytes; i++)
      {
        if (0 < i && !isIPv4 && i % 2 == 0)
          buf.append(':');
        else if (0 < i && isIPv4)
          buf.append('.');
        integer = 0xFF & (i < diffLen ? fillByte : bytes[i - diffLen]);
        if (!isIPv4 && 0x10 > integer)
          buf.append('0');
        buf.append(isIPv4 ? integer : Integer.toHexString(integer));
      }
  }
}
