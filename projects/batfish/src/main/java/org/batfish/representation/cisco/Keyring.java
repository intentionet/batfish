package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Ip;

public class Keyring extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private  Ip _localAddress;

  private Ip _remoteAddress;

  private String _key;

  public Keyring(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
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
