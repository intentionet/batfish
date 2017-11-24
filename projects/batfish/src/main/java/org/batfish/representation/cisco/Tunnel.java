package org.batfish.representation.cisco;

import java.io.Serializable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;

public class Tunnel implements Serializable {

  public enum TunnelMode {
    GRE,
    IPSEC
  }

  private static final long serialVersionUID = 1L;

  Ip _destination;

  String _ipsecProfileName;

  TunnelMode _mode;

  IpProtocol _protocol;

  Ip _source;

  public void setDestination(Ip destination) {
    _destination = destination;
  }

  public void setIpsecProfile(String profileName) {
    _ipsecProfileName = profileName;
  }

  public void setMode(TunnelMode mode) {
    _mode = mode;
  }

  public void setProtocol(IpProtocol protocol) {
    _protocol = protocol;
  }

  public void setSource(Ip source) {
    _source = source;
  }
}
