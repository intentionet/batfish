package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

public class Keyring extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private Ip _localAddress;

  private Ip _remoteAddress;

  private String _key;

  public Keyring(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
  }

  public int getDefinitionLine() {
    return _definitionLine;
  }

  public String getKey() {
    return _key;
  }

  public Ip getLocalAddress() {
    return _localAddress;
  }

  public Ip getRemoteAddress() {
    return _remoteAddress;
  }

  public boolean match(Ip localAddress, Prefix matchIdentity) {
    return localAddress.equals(_localAddress) && matchIdentity.contains(_remoteAddress);
  }

  public void setKey(String key) {
    _key = key;
  }

  public void setLocalAddress(Ip address) {
    _localAddress = address;
  }

  public void setRemoteAddress(Ip address) {
    _remoteAddress = address;
  }
}
