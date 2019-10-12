package org.batfish.representation.juniper;

import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.SubRange;

public final class Route6FilterLineThrough extends Route6FilterLine {

  private final Prefix6 _throughPrefix6;

  public Route6FilterLineThrough(Prefix6 prefix6, Prefix6 throughPrefix6) {
    super(prefix6);
    _throughPrefix6 = throughPrefix6;
  }

  @Override
  public void applyTo(Route6FilterList rfl) {
    int low = _prefix6.getPrefixLength();
    int high = _throughPrefix6.getPrefixLength();
    for (int i = low; i <= high; i++) {
      Ip6 currentNetworkAddress = _throughPrefix6.getAddress().getNetworkAddress(i);
      Prefix6 currentPrefix6 = new Prefix6(currentNetworkAddress, i);
      org.batfish.datamodel.Route6FilterLine line =
          new org.batfish.datamodel.Route6FilterLine(
              LineAction.PERMIT, currentPrefix6, SubRange.singleton(i));
      rfl.addLine(line);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof Route6FilterLineThrough)) {
      return false;
    }

    Route6FilterLineThrough rhs = (Route6FilterLineThrough) o;
    return _prefix6.equals(rhs._prefix6) && _throughPrefix6.equals(rhs._throughPrefix6);
  }

  public Prefix6 getThroughPrefix6() {
    return _throughPrefix6;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _throughPrefix6.hashCode();
    result = prime * result + _prefix6.hashCode();
    return result;
  }
}
