package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import org.batfish.common.BatfishException;

public final class Prefix implements Comparable<Prefix>, Serializable {

  public static final int MAX_PREFIX_LENGTH = 32;

  /** */
  private static final long serialVersionUID = 1L;

  public static final Prefix ZERO = new Prefix(Ip.ZERO, 0);

  public static Prefix forNetworkAddress(NetworkAddress address) {
    return new Prefix(address.getAddress(), address.getNetworkBits());
  }

  private static long getNetworkEnd(long networkStart, int prefixLength) {
    long networkEnd = networkStart;
    int onesLength = 32 - prefixLength;
    for (int i = 0; i < onesLength; i++) {
      networkEnd |= ((long) 1 << i);
    }
    return networkEnd;
  }

  private static long numWildcardBitsToWildcardLong(int numBits) {
    long wildcard = 0;
    for (int i = 0; i < numBits; i++) {
      wildcard |= (1L << i);
    }
    return wildcard;
  }

  private Ip _address;

  private int _prefixLength;

  public Prefix(Ip address, int prefixLength) {
    if (address == null) {
      throw new BatfishException("Cannot create prefix with null network");
    }
    _address = address.getNetworkAddress(prefixLength);
    _prefixLength = prefixLength;
  }

  public Prefix(Ip address, Ip mask) {
    if (address == null) {
      throw new BatfishException("Cannot create prefix with null network");
    }
    if (mask == null) {
      throw new BatfishException("Cannot create prefix with null mask");
    }
    _address = address;
    _prefixLength = mask.numSubnetBits();
  }

  @JsonCreator
  public Prefix(String text) {
    String[] parts = text.split("/");
    if (parts.length != 2) {
      throw new BatfishException("Invalid prefix string: \"" + text + "\"");
    }
    _address = new Ip(parts[0]);
    try {
      _prefixLength = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new BatfishException("Invalid prefix length: \"" + parts[1] + "\"", e);
    }
  }

  @Override
  public int compareTo(Prefix rhs) {
    int ret = _address.compareTo(rhs._address);
    if (ret != 0) {
      return ret;
    }
    return Integer.compare(_prefixLength, rhs._prefixLength);
  }

  public boolean contains(Ip ip) {
    long start = _address.asLong();
    long end = getEndAddress().asLong();
    long ipAsLong = ip.asLong();
    return start <= ipAsLong && ipAsLong <= end;
  }

  public boolean containsPrefix(Prefix prefix) {
    return contains(prefix._address) && _prefixLength < prefix._prefixLength;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Prefix)) {
      return false;
    }
    Prefix rhs = (Prefix) o;
    return _address.equals(rhs._address) && _prefixLength == rhs._prefixLength;
  }

  public Ip getAddress() {
    return _address;
  }

  public Ip getEndAddress() {
    return new Ip(getNetworkEnd(_address.asLong(), _prefixLength));
  }

  public int getPrefixLength() {
    return _prefixLength;
  }

  public Ip getPrefixWildcard() {
    int numWildcardBits = MAX_PREFIX_LENGTH - _prefixLength;
    long wildcardLong = numWildcardBitsToWildcardLong(numWildcardBits);
    return new Ip(wildcardLong);
  }

  public Ip getSubnetMask() {
    return Ip.numSubnetBitsToSubnetMask(_prefixLength);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _address.hashCode();
    result = prime * result + _prefixLength;
    return result;
  }

  @Override
  @JsonValue
  public String toString() {
    return _address + "/" + _prefixLength;
  }
}
