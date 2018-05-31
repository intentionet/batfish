package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;

public class StandardAccessList extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private List<StandardAccessListLine> _lines;

  public StandardAccessList(String id) {
    super(id);
    _lines = new ArrayList<>();
  }

  public void addLine(StandardAccessListLine all) {
    _lines.add(all);
  }

  public List<StandardAccessListLine> getLines() {
    return _lines;
  }

  public ExtendedAccessList toExtendedAccessList() {
    ExtendedAccessList eal = new ExtendedAccessList(_key);
    eal.setParent(this);
    eal.getLines().clear();
    for (StandardAccessListLine sall : _lines) {
      eal.addLine(sall.toExtendedAccessListLine());
    }
    return eal;
  }
}
