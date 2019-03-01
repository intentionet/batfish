package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.net.InetAddresses;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.batfish.common.BatfishException;

public class Ip6 implements Comparable<Ip6>, Serializable {

  private static Map<Ip6, BitSet> _addressBitsCache = new ConcurrentHashMap<>();

  public static final Ip6 AUTO = new Ip6(BigInteger.valueOf(-1L));

  public static final Ip6 MAX = new Ip6(new BigInteger("+FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16));

  private static final int NUM_BYTES = 16;

  private static final long serialVersionUID = 1L;

  public static final Ip6 ZERO = new Ip6(BigInteger.ZERO);

  private static String asIpv6AddressString(BigInteger ipv6AddressAsBigInteger) {
    BigInteger remainder = ipv6AddressAsBigInteger;
    String[] pieces = new String[8];
    for (int i = 0; i < 8; i++) {
      int mask = (int) remainder.shortValue() & 0xFFFF;
      pieces[7 - i] = String.format("%x", mask);
      remainder = remainder.shiftRight(16);
    }
    return StringUtils.join(pieces, ":");
  }

  private static BigInteger numSubnetBitsToSubnetBigInteger(int numBits) {
    BigInteger val = BigInteger.ZERO;
    for (int i = Prefix6.MAX_PREFIX_LENGTH - 1; i > Prefix6.MAX_PREFIX_LENGTH - 1 - numBits; i--) {
      val = val.or(BigInteger.ONE.shiftLeft(i));
    }
    return val;
  }

  public static Ip6 numSubnetBitsToSubnetMask(int numBits) {
    BigInteger mask = numSubnetBitsToSubnetBigInteger(numBits);
    return new Ip6(mask);
  }

  /**
   * Return an {@link Optional} {@link Ip6} from a string, or {@link Optional#empty} if the string
   * does not represent an {@link Ip6}.
   */
  public static @Nonnull Optional<Ip6> tryParse(@Nonnull String text) {
    try {
      return Optional.of(parse(text));
    } catch (IllegalArgumentException | BatfishException e) {
      return Optional.empty();
    }
  }

  private final BigInteger _ip6;

  public Ip6(BigInteger ip6AsBigInteger) {
    _ip6 = ip6AsBigInteger;
  }

  @JsonCreator
  private static @Nonnull Ip6 create(@Nullable String ipAsString) {
    if (ipAsString == null) {
      return null;
    }
    return parse(ipAsString);
  }

  public static @Nonnull Ip6 parse(@Nonnull String ipAsString) {
    boolean invalid = false;
    byte[] ip6AsByteArray = null;
    if (!ipAsString.contains(":")) {
      invalid = true;
    } else {
      try {
        ip6AsByteArray = InetAddresses.forString(ipAsString).getAddress();
      } catch (IllegalArgumentException e) {
        invalid = true;
      }
    }
    if (invalid) {
      throw new BatfishException("Invalid ipv6 address literal: \"" + ipAsString + "\"");
    }
    return new Ip6(new BigInteger(ip6AsByteArray));
  }

  public BigInteger asBigInteger() {
    return _ip6;
  }

  @Override
  public int compareTo(Ip6 rhs) {
    return _ip6.compareTo(rhs._ip6);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Ip6)) {
      return false;
    }
    Ip6 rhs = (Ip6) o;
    return _ip6.equals(rhs._ip6);
  }

  public BitSet getAddressBits() {
    BitSet bits = _addressBitsCache.get(this);
    if (bits == null) {
      ByteBuffer b = ByteBuffer.allocate(NUM_BYTES);
      byte[] ip6Bytes = _ip6.toByteArray();
      ArrayUtils.reverse(ip6Bytes);
      b.put(ip6Bytes);
      BitSet bitsWithHighestMostSignificant = BitSet.valueOf(b.array());
      bits = new BitSet(Prefix6.MAX_PREFIX_LENGTH);
      for (int i = 0; i < Prefix6.MAX_PREFIX_LENGTH; ++i) {
        bits.set(Prefix6.MAX_PREFIX_LENGTH - i - 1, bitsWithHighestMostSignificant.get(i));
      }
      _addressBitsCache.put(this, bits);
    }
    return bits;
  }

  public Ip6 getNetworkAddress(int subnetBits) {
    BigInteger mask = numSubnetBitsToSubnetBigInteger(subnetBits);
    return new Ip6(_ip6.and(mask));
  }

  @Override
  public int hashCode() {
    return _ip6.hashCode();
  }

  public Ip6 inverted() {
    BigInteger mask = new BigInteger("+FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
    BigInteger invertedBigInteger = mask.andNot(_ip6);
    return new Ip6(invertedBigInteger);
  }

  public String networkString(int prefixLength) {
    return toString() + "/" + prefixLength;
  }

  public int numSubnetBits() {
    int numTrailingZeros = _ip6.getLowestSetBit();
    if (numTrailingZeros == -1) {
      return 0;
    } else {
      return Prefix6.MAX_PREFIX_LENGTH - numTrailingZeros;
    }
  }

  @Override
  @JsonValue
  public String toString() {
    return asIpv6AddressString(_ip6);
  }

  public boolean valid() {
    return _ip6.compareTo(BigInteger.ZERO) >= 0
        && _ip6.compareTo(new BigInteger("+FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16)) <= 0;
  }
}
