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

  int _ipsecProfileNameLine;

  TunnelMode _mode;

  IpProtocol _protocol;

  Ip _source;

  public Ip getDestination() {
    return _destination;
  }

  public String getIpsecProfileName() {
    return _ipsecProfileName;
  }

  public int getIpsecProfileNameLine() {
    return _ipsecProfileNameLine;
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

  public void setIpsecProfileNameLine(int line) {
    _ipsecProfileNameLine = line;
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
