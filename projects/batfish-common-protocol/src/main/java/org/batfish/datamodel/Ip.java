package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;

public class Ip implements Comparable<Ip>, Serializable, IpSpace {

  private static Map<Ip, BitSet> _addressBitsCache = new ConcurrentHashMap<>();

  public static final Ip AUTO = new Ip(-1L);

  public static final Ip FIRST_CLASS_A_PRIVATE_IP = new Ip("10.0.0.0");

  public static final Ip FIRST_CLASS_B_PRIVATE_IP = new Ip("172.16.0.0");

  public static final Ip FIRST_CLASS_C_PRIVATE_IP = new Ip("192.168.0.0");

  public static final Ip FIRST_CLASS_E_EXPERIMENTAL_IP = new Ip("240.0.0.0");

  public static final Ip FIRST_MULTICAST_IP = new Ip("224.0.0.0");

  public static final Ip MAX = new Ip(0xFFFFFFFFL);

  private static final int NUM_BYTES = 4;

  private static final long serialVersionUID = 1L;

  public static final Ip ZERO = new Ip(0L);

  /**
   * See {@link #getBitAtPosition(long, int)}. Equivalent to {@code getBitAtPosition(ip.asLong(),
   * position)}
   */
  public static boolean getBitAtPosition(Ip ip, int position) {
    return getBitAtPosition(ip.asLong(), position);
  }

  /**
   * Return the boolean value of a bit at the given position.
   *
   * @param bits the representation of an IP address as a long
   * @param position bit position (0 means most significant, 31 least significant)
   * @return a boolean representation of the bit value
   */
  public static boolean getBitAtPosition(long bits, int position) {
    return (bits & (1 << (Prefix.MAX_PREFIX_LENGTH - 1 - position))) != 0;
  }

  private static long ipStrToLong(String addr) {
    String[] addrArray = addr.split("\\.");
    if (addrArray.length != 4) {
      if (addr.startsWith("INVALID_IP") || addr.startsWith("AUTO/NONE")) {
        String[] tail = addr.split("\\(");
        if (tail.length == 2) {
          String[] longStrParts = tail[1].split("l");
          if (longStrParts.length == 2) {
            String longStr = longStrParts[0];
            return Long.parseLong(longStr);
          }
        }
      }
      throw new IllegalArgumentException("Invalid ip string: \"" + addr + "\"");
    }
    long num = 0;
    for (int i = 0; i < addrArray.length; i++) {
      int power = 3 - i;
      String segmentStr = addrArray[i];
      try {
        int segment = Integer.parseInt(segmentStr);
        num += ((segment % 256 * Math.pow(256, power)));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid ip segment: \"" + segmentStr + "\" in ip string: \"" + addr + "\"", e);
      }
    }
    return num;
  }

  /**
   * Return a {@code long} that represents the {@code /numBits} network mask in a IPv4 subnet.
   *
   * <p>For example, for {@code numBits = 8}, should return {@code 0xFF000000L} representing a mask
   * of the top 8 bits.
   */
  // Visible for testing only.
  static long numSubnetBitsToSubnetLong(int numBits) {
    return ~(0xFFFFFFFFL >> numBits) & 0xFFFFFFFFL;
  }

  public static Ip numSubnetBitsToSubnetMask(int numBits) {
    long mask = numSubnetBitsToSubnetLong(numBits);
    return new Ip(mask);
  }

  private final long _ip;

  public Ip(long ipAsLong) {
    _ip = ipAsLong;
  }

  @JsonCreator
  public Ip(String ipAsString) {
    _ip = ipStrToLong(ipAsString);
  }

  @Override
  public <R> R accept(GenericIpSpaceVisitor<R> visitor) {
    return visitor.visitIp(this);
  }

  public long asLong() {
    return _ip;
  }

  @Override
  public int compareTo(@Nullable Ip rhs) {
    return Long.compare(_ip, rhs._ip);
  }

  @Override
  public boolean containsIp(Ip ip) {
    return _ip == ip._ip;
  }

  @Override
  public IpSpace complement() {
    return AclIpSpace.builder()
        .thenRejecting(this)
        .thenPermitting(UniverseIpSpace.INSTANCE)
        .build();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Ip)) {
      return false;
    }
    Ip rhs = (Ip) o;
    return _ip == rhs._ip;
  }

  /** @deprecated In favor of much simpler {@link #getBitAtPosition(Ip, int)} */
  @Deprecated
  public BitSet getAddressBits() {
    BitSet bits = _addressBitsCache.get(this);
    if (bits == null) {
      int addressAsInt = (int) (_ip);
      ByteBuffer b = ByteBuffer.allocate(NUM_BYTES);
      b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a
      // byte
      // buffer is always BIG_ENDIAN.
      b.putInt(addressAsInt);
      BitSet bitsWithHighestMostSignificant = BitSet.valueOf(b.array());
      bits = new BitSet(Prefix.MAX_PREFIX_LENGTH);
      for (int i = Prefix.MAX_PREFIX_LENGTH - 1, j = 0; i >= 0; i--, j++) {
        bits.set(j, bitsWithHighestMostSignificant.get(i));
      }
      _addressBitsCache.put(this, bits);
    }
    return bits;
  }

  public Ip getClassMask() {
    long firstOctet = _ip >> 24;
    if (firstOctet <= 127) {
      return new Ip(0xFF000000L);
    } else if (firstOctet <= 191) {
      return new Ip(0XFFFF0000L);
    } else if (firstOctet <= 223) {
      return new Ip(0xFFFFFF00L);
    } else {
      throw new BatfishException("Cannot compute classmask");
    }
  }

  /**
   * Returns the size of an IPv4 network in IPv4 Address Class of this {@link Ip}. Returns {@code
   * -1} if the class does not have a defined subnet size, e.g. the experimental subnet.
   */
  public int getClassNetworkSize() {
    long firstOctet = _ip >> 24;
    if (firstOctet <= 127) {
      return 8;
    } else if (firstOctet <= 191) {
      return 16;
    } else if (firstOctet <= 223) {
      return 24;
    } else {
      return -1;
    }
  }

  public Ip getNetworkAddress(int subnetBits) {
    long mask = numSubnetBitsToSubnetLong(subnetBits);
    return new Ip(_ip & mask);
  }

  public Ip getNetworkAddress(Ip mask) {
    return new Ip(_ip & mask.asLong());
  }

  public Ip getSubnetEnd(Ip mask) {
    return new Ip(_ip | mask.inverted().asLong());
  }

  public Ip getWildcardEndIp(Ip wildcard) {
    return new Ip(_ip | wildcard.asLong());
  }

  @Override
  public int hashCode() {
    return Long.hashCode(_ip);
  }

  public Ip inverted() {
    long invertedLong = (~_ip) & 0xFFFFFFFFL;
    return new Ip(invertedLong);
  }

  public String networkString(int prefixLength) {
    return toString() + "/" + prefixLength;
  }

  public String networkString(Ip mask) {
    return toString() + "/" + mask.numSubnetBits();
  }

  public int numSubnetBits() {
    int numTrailingZeros = Long.numberOfTrailingZeros(_ip);
    if (numTrailingZeros > Prefix.MAX_PREFIX_LENGTH) {
      return 0;
    } else {
      return Prefix.MAX_PREFIX_LENGTH - numTrailingZeros;
    }
  }

  @Override
  @JsonValue
  public String toString() {
    if (!valid()) {
      if (_ip == -1L) {
        return "AUTO/NONE(-1l)";
      } else {
        return "INVALID_IP(" + _ip + "l)";
      }
    } else {
      return ((_ip >> 24) & 0xFF)
          + "."
          + ((_ip >> 16) & 0xFF)
          + "."
          + ((_ip >> 8) & 0xFF)
          + "."
          + (_ip & 0xFF);
    }
  }

  public boolean valid() {
    return 0L <= _ip && _ip <= 0xFFFFFFFFL;
  }
}
