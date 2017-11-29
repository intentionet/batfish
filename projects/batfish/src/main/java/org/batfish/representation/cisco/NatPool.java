package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;
import org.batfish.datamodel.Ip;

public class NatPool extends ComparableStructure<String> implements DefinedStructure {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private Ip _first;

  private Ip _last;

  public NatPool(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
  }

  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public Ip getFirst() {
    return _first;
  }

  public Ip getLast() {
    return _last;
  }

  public void setFirst(Ip first) {
    _first = first;
  }

  public void setLast(Ip last) {
    _last = last;
  }
}
