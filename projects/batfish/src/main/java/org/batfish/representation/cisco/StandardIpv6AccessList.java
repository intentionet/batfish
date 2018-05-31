package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;

public class StandardIpv6AccessList extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private List<StandardIpv6AccessListLine> _lines;

  public StandardIpv6AccessList(String id) {
    super(id);
    _lines = new ArrayList<>();
  }

  public void addLine(StandardIpv6AccessListLine all) {
    _lines.add(all);
  }

  public List<StandardIpv6AccessListLine> getLines() {
    return _lines;
  }

  public ExtendedIpv6AccessList toExtendedIpv6AccessList() {
    ExtendedIpv6AccessList eal = new ExtendedIpv6AccessList(_key);
    eal.setParent(this);
    eal.getLines().clear();
    for (StandardIpv6AccessListLine sall : _lines) {
      eal.addLine(sall.toExtendedIpv6AccessListLine());
    }
    return eal;
  }
}
