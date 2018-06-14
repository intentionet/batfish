package org.batfish.representation.juniper;

import java.util.Objects;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Ip;

public class IkeGateway extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private Ip _address;

  private Interface _externalInterface;

  private int _externalInterfaceLine;

  private String _ikePolicy;

  private Ip _localAddress;

  public IkeGateway(String name) {
    super(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IkeGateway)) {
      return false;
    }
    IkeGateway other = (IkeGateway) o;
    // TODO: compare all fields
    return _name.equals(other._name);
  }

  public Ip getAddress() {
    return _address;
  }

  public Interface getExternalInterface() {
    return _externalInterface;
  }

  public int getExternalInterfaceLine() {
    return _externalInterfaceLine;
  }

  public String getIkePolicy() {
    return _ikePolicy;
  }

  public Ip getLocalAddress() {
    return _localAddress;
  }

  @Override
  public int hashCode() {
    // TODO: hash all fields
    return Objects.hash(_name);
  }

  public void setAddress(Ip address) {
    _address = address;
  }

  public void setExternalInterface(Interface externalInterface) {
    _externalInterface = externalInterface;
  }

  public void setExternalInterfaceLine(int externalInterfaceLine) {
    _externalInterfaceLine = externalInterfaceLine;
  }

  public void setIkePolicy(String ikePolicy) {
    _ikePolicy = ikePolicy;
  }

  public void setLocalAddress(Ip localAddress) {
    _localAddress = localAddress;
  }
}
