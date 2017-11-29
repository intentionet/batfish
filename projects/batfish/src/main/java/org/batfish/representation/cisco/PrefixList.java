package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;

public class PrefixList extends ComparableStructure<String> implements DefinedStructure {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private List<PrefixListLine> _lines;

  public PrefixList(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _lines = new ArrayList<>();
  }

  public void addLine(PrefixListLine r) {
    _lines.add(r);
  }

  public void addLines(List<PrefixListLine> r) {
    _lines.addAll(r);
  }

  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public List<PrefixListLine> getLines() {
    return _lines;
  }
}
