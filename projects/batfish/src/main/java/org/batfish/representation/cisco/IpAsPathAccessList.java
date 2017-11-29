package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;

public class IpAsPathAccessList extends ComparableStructure<String> implements DefinedStructure {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private List<IpAsPathAccessListLine> _lines;

  public IpAsPathAccessList(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _lines = new ArrayList<>();
  }

  public void addLine(IpAsPathAccessListLine line) {
    _lines.add(line);
  }

  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public List<IpAsPathAccessListLine> getLines() {
    return _lines;
  }
}
