package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;

/** Represents a link by a pair of IP addresses. */
public final class IpLink implements Comparable<IpLink> {

  private final Ip _ip1;
  private final Ip _ip2;

  @JsonCreator
  public IpLink(@JsonProperty("ip1") Ip ip1, @JsonProperty("ip2") Ip ip2) {
    this._ip1 = ip1;
    this._ip2 = ip2;
  }

  public Ip getIp1() {
    return _ip1;
  }

  public Ip getIp2() {
    return _ip2;
  }

  @Override
  public int compareTo(@Nonnull IpLink o) {
    int cmp = _ip1.compareTo(o._ip1);
    if (cmp != 0) {
      return cmp;
    }
    return _ip2.compareTo(o._ip2);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof IpLink)) {
      return false;
    }
    IpLink other = (IpLink) o;
    return _ip1.equals(other._ip1) && _ip2.equals(other._ip2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ip1, _ip2);
  }

  @Override
  public String toString() {
    return String.format("<%s,%s>", _ip1, _ip2);
  }
}
