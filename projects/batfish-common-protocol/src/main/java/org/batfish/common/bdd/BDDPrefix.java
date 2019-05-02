package org.batfish.common.bdd;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.SubRange;

/** A collection of attributes describing a {@link Prefix}, represented using {@link BDD}s */
@ParametersAreNonnullByDefault
public final class BDDPrefix {

  private static final int IP_LENGTH = 32;
  private static final int PREFIX_LENGTH_LENGTH = 6;

  private final @Nonnull BDDFactory _factory;
  private final @Nonnull BDDInteger _ip;
  private final @Nonnull BDDInteger _prefixLength;

  public BDDPrefix(BDDInteger ip, BDDInteger prefixLength) {
    checkArgument(
        ip.getBitvec().length == IP_LENGTH,
        "Expected size of ip BDDInteger to be %d but was %d",
        IP_LENGTH,
        ip.getBitvec().length);
    checkArgument(
        prefixLength.getBitvec().length == PREFIX_LENGTH_LENGTH,
        "Expected size of prefixLength BDDInteger to be %d but was %d",
        PREFIX_LENGTH_LENGTH,
        ip.getBitvec().length);
    checkArgument(
        ip.getFactory() == prefixLength.getFactory(),
        "Expected ip BDDInteger and prefixLength BDDInteger to use same factory");
    checkArgument(
        ip.getFactory() != null,
        "Expected ip BDDInteger and prefixLength BDDInteger to use non-null factory");
    _ip = ip;
    _factory = ip.getFactory();
    _prefixLength = prefixLength;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BDDPrefix)) {
      return false;
    }
    BDDPrefix other = (BDDPrefix) o;

    return _ip.equals(other._ip) && _prefixLength.equals(other._prefixLength);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ip, _prefixLength);
  }

  /**
   * Check if the first {@code length} bits of {@code _ip} match the BDDInteger representing {@code
   * ip}.
   */
  private @Nonnull BDD firstBitsEqual(Ip ip, int length) {
    BDD[] bits = _ip.getBitvec();
    long address = ip.asLong();
    BDD result = _factory.one();
    for (int i = 0; i < length; i++) {
      boolean res = Ip.getBitAtPosition(address, i);
      if (res) {
        result = result.and(bits[i]);
      } else {
        result = result.diff(bits[i]);
      }
    }
    return result;
  }

  /** @return The {@link BDDFactory} used by this {@link BDDPrefix}. */
  public @Nonnull BDDFactory getFactory() {
    return _ip.getFactory();
  }

  public @Nonnull BDDInteger getIp() {
    return _ip;
  }

  public @Nonnull BDDInteger getPrefixLength() {
    return _prefixLength;
  }

  /** Returns a {@link BDD} reprepsenting a {@link Prefix} in the provided {@code prefixRange}. */
  public @Nonnull BDD inPrefixRange(PrefixRange prefixRange) {
    Prefix p = prefixRange.getPrefix();
    BDD prefixMatch = firstBitsEqual(p.getStartIp(), p.getPrefixLength());

    SubRange r = prefixRange.getLengthRange();
    int lower = r.getStart();
    int upper = r.getEnd();
    BDD lenMatch = _prefixLength.range(lower, upper);

    return lenMatch.and(prefixMatch);
  }

  /** Returns a {@link BDD} reprepsenting a {@link Prefix} equal to the provided {@code prefix}. */
  public @Nonnull BDD isPrefix(Prefix prefix) {
    return firstBitsEqual(prefix.getStartIp(), prefix.getPrefixLength())
        .and(_prefixLength.value(prefix.getPrefixLength()));
  }

  /**
   * Returns a {@link BDD} reprepsenting a {@link Prefix} permitted by the provided {@code
   * routeFilterList}.
   */
  public @Nonnull BDD permittedByRouteFilterList(RouteFilterList routeFilterList) {
    BDD result = getFactory().zero();
    List<RouteFilterLine> lines = ImmutableList.copyOf(routeFilterList.getLines()).reverse();
    for (RouteFilterLine line : lines) {
      if (!line.getIpWildcard().isPrefix()) {
        throw new BatfishException("non-prefix IpWildcards are unsupported");
      }
      PrefixRange range = new PrefixRange(line.getIpWildcard().toPrefix(), line.getLengthRange());
      BDD matches = inPrefixRange(range);
      BDD action = line.getAction() == LineAction.PERMIT ? _factory.one() : _factory.zero();
      result = matches.ite(action, result);
    }
    return result;
  }
}
