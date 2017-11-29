package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;

public class Prefix6List extends ComparableStructure<String> implements DefinedStructure {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private List<Prefix6ListLine> _lines;

  public Prefix6List(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _lines = new ArrayList<>();
  }

  public void addLine(Prefix6ListLine r) {
    _lines.add(r);
  }

  public void addLines(List<Prefix6ListLine> r) {
    _lines.addAll(r);
  }

  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public List<Prefix6ListLine> getLines() {
    return _lines;
  }
}
