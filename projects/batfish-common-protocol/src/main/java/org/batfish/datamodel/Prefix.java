package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;

/** An IPv4 Prefix */
@ParametersAreNonnullByDefault
public final class Prefix implements Comparable<Prefix>, Serializable {

  // Soft values: let it be garbage collected in times of pressure.
  // Maximum size 2^20: Just some upper bound on cache size, well less than GiB.
  //   (12 bytes seems smallest possible entry (long + int), would be 12 MiB total).
  private static final Cache<Prefix, Prefix> CACHE =
      CacheBuilder.newBuilder().softValues().maximumSize(1 << 20).build();

  /** Maximum prefix length (number of bits) for a IPv4 address, which is 32 */
  public static final int MAX_PREFIX_LENGTH = 32;

  private static final long serialVersionUID = 1L;

  /** A "0.0.0.0/0" prefix */
  public static final Prefix ZERO = create(Ip.ZERO, 0);

  private static long getNetworkEnd(long networkStart, int prefixLength) {
    return networkStart | ((1L << (32 - prefixLength)) - 1);
  }

  private static long numWildcardBitsToWildcardLong(int numBits) {
    long wildcard = 0;
    for (int i = 0; i < numBits; i++) {
      wildcard |= (1L << i);
    }
    return wildcard;
  }

  /** Parse a {@link Prefix} from a string. */
  @Nonnull
  @JsonCreator
  public static Prefix parse(@Nullable String text) {
    checkArgument(text != null, "Invalid IPv4 prefix %s", text);
    String[] parts = text.split("/");
    if (parts.length != 2) {
      throw new BatfishException("Invalid prefix string: \"" + text + "\"");
    }
    Ip ip = Ip.parse(parts[0]);
    int prefixLength;
    try {
      prefixLength = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new BatfishException("Invalid prefix length: \"" + parts[1] + "\"", e);
    }
    return create(ip, prefixLength);
  }

  /**
   * Return an {@link Optional} {@link Prefix} from a string, or {@link Optional#empty} if the
   * string does not represent a {@link Prefix}.
   */
  @Nonnull
  public static Optional<Prefix> tryParse(@Nonnull String text) {
    try {
      return Optional.of(parse(text));
    } catch (IllegalArgumentException | BatfishException e) {
      return Optional.empty();
    }
  }

  /**
   * Parse a {@link Prefix} from a string.
   *
   * @throws IllegalArgumentException if the string does not represent a prefix in canonical form.
   */
  public static @Nonnull Prefix strict(String prefixStr) {
    Prefix prefix = parse(prefixStr);
    checkArgument(prefix.toString().equals(prefixStr), "Non-canonical prefix: %s", prefixStr);
    return prefix;
  }

  private final Ip _ip;

  private final int _prefixLength;

  private Prefix(Ip ip, int prefixLength) {
    checkArgument(
        prefixLength >= 0 && prefixLength <= MAX_PREFIX_LENGTH,
        "Invalid prefix length %d",
        prefixLength);
    if (ip.valid()) {
      // TODO: stop using Ip as a holder for invalid values.
      _ip = ip.getNetworkAddress(prefixLength);
    } else {
      _ip = ip;
    }
    _prefixLength = prefixLength;
  }

  public static Prefix create(Ip ip, int prefixLength) {
    Prefix p = new Prefix(ip, prefixLength);
    try {
      return CACHE.get(p, () -> p);
    } catch (ExecutionException e) {
      // This shouldn't happen, but handle anyway.
      return p;
    }
  }

  public static Prefix create(Ip address, Ip mask) {
    return create(address, mask.numSubnetBits());
  }

  /** Return the longest prefix that contains both input prefixes. */
  public static Prefix longestCommonPrefix(Prefix p1, Prefix p2) {
    if (p1.containsPrefix(p2)) {
      return p1;
    }
    if (p2.containsPrefix(p1)) {
      return p2;
    }
    long l1 = p1.getStartIp().asLong();
    long l2 = p2.getStartIp().asLong();
    long oneAtFirstDifferentBit = Long.highestOneBit(l1 ^ l2);
    int lengthInCommon = MAX_PREFIX_LENGTH - 1 - Long.numberOfTrailingZeros(oneAtFirstDifferentBit);
    return Prefix.create(Ip.create(l1), lengthInCommon);
  }

  @Override
  public int compareTo(Prefix rhs) {
    int ret = _ip.compareTo(rhs._ip);
    if (ret != 0) {
      return ret;
    }
    return Integer.compare(_prefixLength, rhs._prefixLength);
  }

  public boolean containsIp(Ip ip) {
    long start = _ip.asLong();
    long end = getNetworkEnd(start, _prefixLength);
    long ipAsLong = ip.asLong();
    return start <= ipAsLong && ipAsLong <= end;
  }

  public boolean containsPrefix(Prefix prefix) {
    return containsIp(prefix._ip) && _prefixLength <= prefix._prefixLength;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Prefix)) {
      return false;
    }
    Prefix rhs = (Prefix) o;
    return _ip.equals(rhs._ip) && _prefixLength == rhs._prefixLength;
  }

  @Nonnull
  public Ip getEndIp() {
    return Ip.create(getNetworkEnd(_ip.asLong(), _prefixLength));
  }

  public int getPrefixLength() {
    return _prefixLength;
  }

  @Nonnull
  public Ip getPrefixWildcard() {
    int numWildcardBits = MAX_PREFIX_LENGTH - _prefixLength;
    long wildcardLong = numWildcardBitsToWildcardLong(numWildcardBits);
    return Ip.create(wildcardLong);
  }

  @Nonnull
  public Ip getStartIp() {
    return _ip;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ip, _prefixLength);
  }

  public IpSpace toIpSpace() {
    if (_prefixLength == 0) {
      return UniverseIpSpace.INSTANCE;
    } else if (_prefixLength == MAX_PREFIX_LENGTH) {
      return _ip.toIpSpace();
    }
    return new PrefixIpSpace(this);
  }

  /**
   * Returns an {@link IpSpace} that contains this prefix except for the network and broadcast
   * addresses, if applicable. Following RFC 3021, the entire {@code /31} is treated as host IP
   * space. A prefix of length {@code /32} is preserved, though may be a degenerate case.
   */
  public IpSpace toHostIpSpace() {
    if (_prefixLength >= Prefix.MAX_PREFIX_LENGTH - 1) {
      return toIpSpace();
    }
    return AclIpSpace.builder()
        .thenRejecting(getStartIp().toIpSpace())
        .thenRejecting(getEndIp().toIpSpace())
        .thenPermitting(toIpSpace())
        .build();
  }

  /**
   * Returns the first ip in the prefix that is not a subnet address. When the prefix is /32 or /31,
   * returns the getStartIp(), otherwise, returns the ip after getStartIp().
   */
  public Ip getFirstHostIp() {
    if (_prefixLength >= Prefix.MAX_PREFIX_LENGTH - 1) {
      return getStartIp();
    } else {
      Ip subnetIp = getStartIp();
      return Ip.create(subnetIp.asLong() + 1);
    }
  }

  /**
   * Returns the last ip in the prefix that is not a broadcast address. When the prefix is /32 or
   * /31, returns the getEndIp(), otherwise, returns the ip before getEndIp().
   */
  public Ip getLastHostIp() {
    if (_prefixLength >= Prefix.MAX_PREFIX_LENGTH - 1) {
      return getEndIp();
    } else {
      Ip broadcastIp = getEndIp();
      return Ip.create(broadcastIp.asLong() - 1);
    }
  }

  @Override
  @JsonValue
  public String toString() {
    return _ip + "/" + _prefixLength;
  }
}
