package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;

public class IpWildcard extends Pair<Ip, Ip> {

  public static final IpWildcard ANY = new IpWildcard(Ip.ZERO, Ip.MAX);

  /** */
  private static final long serialVersionUID = 1L;

  private static Ip parseAddress(String str) {
    if (str.contains(":")) {
      String[] parts = str.split(":");
      if (parts.length != 2) {
        throw new BatfishException("Invalid IpWildcard string: '" + str + "'");
      } else {
        return new Ip(parts[0]);
      }
    } else if (str.contains("/")) {
      String[] parts = str.split("/");
      if (parts.length != 2) {
        throw new BatfishException("Invalid IpWildcard string: '" + str + "'");
      } else {
        return new Ip(parts[0]);
      }
    } else {
      return new Ip(str);
    }
  }

  private static Ip parseMask(String str) {
    if (str.contains(":")) {
      String[] parts = str.split(":");
      if (parts.length != 2) {
        throw new BatfishException("Invalid IpWildcard string: '" + str + "'");
      } else {
        return new Ip(parts[1]);
      }
    } else if (str.contains("/")) {
      String[] parts = str.split("/");
      if (parts.length != 2) {
        throw new BatfishException("Invalid IpWildcard string: '" + str + "'");
      } else {
        int prefixLength = Integer.parseInt(parts[1]);
        return Ip.numSubnetBitsToSubnetMask(prefixLength).inverted();
      }
    } else {
      return Ip.ZERO;
    }
  }

  public IpWildcard(Ip ip) {
    this(new Prefix(ip, Prefix.MAX_PREFIX_LENGTH));
  }

  public IpWildcard(Ip address, Ip wildcardMask) {
    super(address, wildcardMask);
    if (!wildcardMask.valid()) {
      throw new BatfishException("Invalid wildcard: " + wildcardMask);
    }
  }

  public IpWildcard(Prefix prefix) {
    this(prefix.getAddress(), prefix.getPrefixWildcard());
  }

  public IpWildcard(NetworkAddress address) {
    // TODO: fix this.
    this(Prefix.forNetworkAddress(address));
  }

  @JsonCreator
  public IpWildcard(String str) {
    super(parseAddress(str), parseMask(str));
  }

  public boolean contains(Ip ip) {
    long wildcardIpAsLong = getIp().asLong();
    long wildcardMask = getWildcard().asLong();
    long ipAsLong = ip.asLong();
    long maskedIpAsLong = ipAsLong | wildcardMask;
    long maskedWildcard = wildcardIpAsLong | wildcardMask;
    return maskedIpAsLong == maskedWildcard;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof IpWildcard)) {
      return false;
    }
    IpWildcard other = (IpWildcard) o;
    if (other.getFirst().equals(this.getFirst()) && other.getSecond().equals(this.getSecond())) {
      return true;
    } else {
      return false;
    }
  }

  public Ip getIp() {
    return _first;
  }

  public Ip getWildcard() {
    return _second;
  }

  public boolean isPrefix() {
    long w = _second.asLong();
    long wp = w + 1L;
    int numTrailingZeros = Long.numberOfTrailingZeros(wp);
    long check = 1L << numTrailingZeros;
    return wp == check;
  }

  public Prefix toPrefix() {
    if (isPrefix()) {
      return new Prefix(_first, _second.inverted());
    } else {
      throw new BatfishException("Invalid wildcard format for conversion to prefix: " + _second);
    }
  }

  @JsonValue
  @Override
  public String toString() {
    if (_second.equals(Ip.ZERO)) {
      return _first.toString();
    } else if (isPrefix()) {
      return toPrefix().toString();
    } else {
      return _first + ":" + _second;
    }
  }
}
