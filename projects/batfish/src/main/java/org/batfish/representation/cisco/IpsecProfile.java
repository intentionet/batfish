package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.common.util.DefinedStructure;
import org.batfish.datamodel.DiffieHellmanGroup;

public class IpsecProfile extends ComparableStructure<String> implements DefinedStructure {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private DiffieHellmanGroup _pfsGroup;

  private String _transformSet;

  public IpsecProfile(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
  }

  @Override
  public int getDefinitionLine() {
    return _definitionLine;
  }

  public DiffieHellmanGroup getPfsGroup() {
    return _pfsGroup;
  }

  public String getTransformSet() {
    return _transformSet;
  }

  public void setPfsGroup(DiffieHellmanGroup pfsGroup) {
    _pfsGroup = pfsGroup;
  }

  public void setTransformSet(String transformSet) {
    _transformSet = transformSet;
  }
}
