package org.batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;

public class AsPathSet extends ComparableStructure<String> implements DefinedStructure {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private final List<AsPathSetElem> _elements;

  public AsPathSet(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _elements = new ArrayList<>();
  }

  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public List<AsPathSetElem> getElements() {
    return _elements;
  }
}
