package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;

public class ExtendedIpv6AccessList extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private List<ExtendedIpv6AccessListLine> _lines;

  private StandardIpv6AccessList _parent;

  public ExtendedIpv6AccessList(String id) {
    super(id);
    _lines = new ArrayList<>();
  }

  public void addLine(ExtendedIpv6AccessListLine all) {
    _lines.add(all);
  }

  public List<ExtendedIpv6AccessListLine> getLines() {
    return _lines;
  }

  public StandardIpv6AccessList getParent() {
    return _parent;
  }

  public void setParent(StandardIpv6AccessList parent) {
    _parent = parent;
  }

  @Override
  public String toString() {
    String output = super.toString() + "\n" + "Identifier: " + _key;
    for (ExtendedIpv6AccessListLine line : _lines) {
      output += "\n" + line;
    }
    return output;
  }
}
