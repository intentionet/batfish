package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.DiffieHellmanGroup;

public class IpsecProfile extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private String _isakmpProfile;

  private DiffieHellmanGroup _pfsGroup;

  private String _transformSet;

  public IpsecProfile(String name) {
    super(name);
  }

  public String getIsakmpProfile() {
    return _isakmpProfile;
  }

  public DiffieHellmanGroup getPfsGroup() {
    return _pfsGroup;
  }

  public String getTransformSet() {
    return _transformSet;
  }

  public void setIsakmpProfile(String isakmpProfile) {
    _isakmpProfile = isakmpProfile;
  }

  public void setPfsGroup(DiffieHellmanGroup pfsGroup) {
    _pfsGroup = pfsGroup;
  }

  public void setTransformSet(String transformSet) {
    _transformSet = transformSet;
  }
}
