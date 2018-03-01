package org.batfish.datamodel;

public class IpAddressAclLine {

  private final LineAction _action;

  private final IpSpace _ipSpace;

  public IpAddressAclLine(IpSpace ipSpace, LineAction action) {
    _ipSpace = ipSpace;
    _action = action;
  }

  public LineAction getAction() {
    return _action;
  }

  public IpSpace getIpSpace() {
    return _ipSpace;
  }
}
