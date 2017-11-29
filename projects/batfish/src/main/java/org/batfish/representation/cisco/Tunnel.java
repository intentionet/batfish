package org.batfish.representation.cisco;

import org.batfish.common.util.ReferenceCountedStructure;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;

public class Tunnel extends ReferenceCountedStructure {

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

  public Ip getDestination() {
    return _destination;
  }

  public String getIpsecProfileName() {
    return _ipsecProfileName;
  }

  public TunnelMode getMode() {
    return _mode;
  }

  public Ip getSource() {
    return _source;
  }

  public void setDestination(Ip destination) {
    _destination = destination;
  }

  public void setIpsecProfileName(String name) {
    _ipsecProfileName = name;
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
