package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;

public class IpsecProfile extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private  String _pfs;

  private String _transformSet;

  public IpsecProfile(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
  }

  public void setPfs(String pfs) {
    _pfs = pfs;
  }

  public  void setTransformSet(String transformSet) {
    _transformSet = transformSet;
  }
}
