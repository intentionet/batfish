package org.batfish.representation.juniper;

import java.util.Objects;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Ip6Wildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.SubRange;

/** Representation of a route-filter with a IPv6 prefix and an address mask */
public final class Route6FilterLineAddressMask extends Route6FilterLine {

  private final Ip6 _addressMask;

  public Route6FilterLineAddressMask(Prefix6 prefix6, Ip6 addressMask) {
    super(prefix6);
    _addressMask = addressMask;
  }

  @Override
  public void applyTo(Route6FilterList rfl) {
    int prefixLength = _prefix6.getPrefixLength();
    org.batfish.datamodel.Route6FilterLine line =
        new org.batfish.datamodel.Route6FilterLine(
            LineAction.PERMIT,
            new Ip6Wildcard(
                new Prefix6(_prefix6.getAddress(), prefixLength).getAddress(), _addressMask),
            SubRange.singleton(prefixLength));
    rfl.addLine(line);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof Route6FilterLineAddressMask)) {
      return false;
    }

    Route6FilterLineAddressMask rhs = (Route6FilterLineAddressMask) o;
    return _prefix6.equals(rhs._prefix6) && _addressMask.equals(rhs._addressMask);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_prefix6, _addressMask);
  }
}
