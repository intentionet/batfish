package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;

public class ExtendedAccessList extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private List<ExtendedAccessListLine> _lines;

  private StandardAccessList _parent;

  public ExtendedAccessList(String id) {
    super(id);
    _lines = new ArrayList<>();
  }

  public void addLine(ExtendedAccessListLine all) {
    _lines.add(all);
  }

  public List<ExtendedAccessListLine> getLines() {
    return _lines;
  }

  public StandardAccessList getParent() {
    return _parent;
  }

  public void setParent(StandardAccessList parent) {
    _parent = parent;
  }

  @Override
  public String toString() {
    String output = super.toString() + "\n" + "Identifier: " + _key;
    for (ExtendedAccessListLine line : _lines) {
      output += "\n" + line;
    }
    return output;
  }
}
