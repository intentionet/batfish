package org.batfish.representation.juniper;

import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.SubRange;

public class Route6FilterLineOrLonger extends Route6FilterLine {

  /** */
  private static final long serialVersionUID = 1L;

  public Route6FilterLineOrLonger(Prefix6 prefix6) {
    super(prefix6);
  }

  @Override
  public void applyTo(Route6FilterList rfl) {
    int prefixLength = _prefix6.getPrefixLength();
    org.batfish.datamodel.Route6FilterLine line =
        new org.batfish.datamodel.Route6FilterLine(
            LineAction.ACCEPT, _prefix6, new SubRange(prefixLength, Prefix6.MAX_PREFIX_LENGTH));
    rfl.addLine(line);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof Route6FilterLineOrLonger)) {
      return false;
    }

    Route6FilterLineOrLonger rhs = (Route6FilterLineOrLonger) o;
    return _prefix6.equals(rhs._prefix6);
  }

  @Override
  public int hashCode() {
    return _prefix6.hashCode();
  }
}
