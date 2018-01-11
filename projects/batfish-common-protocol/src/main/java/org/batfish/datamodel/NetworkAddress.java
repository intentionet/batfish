package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishException;

public final class NetworkAddress implements Comparable<NetworkAddress>, Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  private Ip _address;

  private int _networkBits;

  public NetworkAddress(@Nonnull Ip address, int networkBits) {
    if (address == null) {
      throw new BatfishException("Cannot create NetworkAddress with null network");
    }
    _address = address;
    _networkBits = networkBits;
  }

  public NetworkAddress(@Nonnull Ip address, @Nonnull Ip networkMask) {
    if (address == null) {
      throw new BatfishException("Cannot create NetworkAddress with null address");
    }
    if (networkMask == null) {
      throw new BatfishException("Cannot create NetworkAddress with null mask");
    }
    _address = address;
    _networkBits = networkMask.numSubnetBits();
  }

  @JsonCreator
  public NetworkAddress(String text) {
    String[] parts = text.split("/");
    if (parts.length != 2) {
      throw new BatfishException("Invalid NetworkAddress string: \"" + text + "\"");
    }
    _address = new Ip(parts[0]);
    try {
      _networkBits = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new BatfishException("Invalid network length: \"" + parts[1] + "\"", e);
    }
  }

  @Override
  public int compareTo(NetworkAddress rhs) {
    int ret = _address.compareTo(rhs._address);
    if (ret != 0) {
      return ret;
    }
    return Integer.compare(_networkBits, rhs._networkBits);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof NetworkAddress)) {
      return false;
    }
    NetworkAddress rhs = (NetworkAddress) o;
    return _address.equals(rhs._address) && _networkBits == rhs._networkBits;
  }

  public Ip getAddress() {
    return _address;
  }

  public int getNetworkBits() {
    return _networkBits;
  }

  public Ip getSubnetMask() {
    return Ip.numSubnetBitsToSubnetMask(_networkBits);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_address, _networkBits);
  }

  @Override
  @JsonValue
  public String toString() {
    return _address + "/" + _networkBits;
  }
}
