package org.batfish.representation.palo_alto;

import java.io.Serializable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

public final class AddressObject implements Serializable {
  private static final long serialVersionUID = 1L;

  private String _description;

  private Ip _ipFrom;

  private Ip _ipTo;

  private final String _name;

  public AddressObject(String name) {
    _name = name;
  }

  public String getDescription() {
    return _description;
  }

  public Ip getIpFrom() {
    return _ipFrom;
  }

  public Ip getIpTo() {
    return _ipTo;
  }

  public String getName() {
    return _name;
  }

  public void setDescription(String description) {
    _description = description;
  }

  /** Set Ip to and from values to {@code address} */
  public void setIpRange(Ip address) {
    _ipFrom = address;
    _ipTo = address;
  }

  /** Set Ip to and from values using {@code prefix} */
  public void setIpRange(Prefix prefix) {
    _ipFrom = prefix.getStartIp();
    _ipTo = prefix.getEndIp();
  }

  /** Set Ip to and from values to provided values */
  public void setIpRange(Ip from, Ip to) {
    _ipFrom = from;
    _ipTo = to;
  }
}
