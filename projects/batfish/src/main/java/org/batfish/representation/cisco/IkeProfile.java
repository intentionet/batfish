package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

public class IkeProfile extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private String _keyring;

  private Ip _localAddress;

  private Prefix _matchIdentityAddress;

  public IkeProfile(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
  }

  public void setKeyring(String keyring) {
    _keyring = keyring;
  }

  public void setLocalAddress(Ip address) {
    _localAddress = address;
  }

  public void setMatchIdentityAddress(Ip address, Ip mask) {
    _matchIdentityAddress = new Prefix(address, mask);
  }
}
