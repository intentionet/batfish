package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigInteger;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;

public class Ip6Wildcard extends Pair<Ip6, Ip6> {

  public static final Ip6Wildcard ANY = new Ip6Wildcard(Ip6.ZERO, Ip6.MAX);

  /** */
  private static final long serialVersionUID = 1L;

  private static Ip6 parseAddress(String str) {
    if (str.contains(";")) {
      String[] parts = str.split(";");
      if (parts.length != 2) {
        throw new BatfishException("Invalid Ip6Wildcard string: '" + str + "'");
      } else {
        return new Ip6(parts[0]);
      }
    } else if (str.contains("/")) {
      String[] parts = str.split("/");
      if (parts.length != 2) {
        throw new BatfishException("Invalid Ip6Wildcard string: '" + str + "'");
      } else {
        return new Ip6(parts[0]);
      }
    } else {
      return new Ip6(str);
    }
  }

  private static Ip6 parseMask(String str) {
    if (str.contains(";")) {
      String[] parts = str.split(";");
      if (parts.length != 2) {
        throw new BatfishException("Invalid Ip6Wildcard string: '" + str + "'");
      } else {
        return new Ip6(parts[1]);
      }
    } else if (str.contains("/")) {
      String[] parts = str.split("/");
      if (parts.length != 2) {
        throw new BatfishException("Invalid Ip6Wildcard string: '" + str + "'");
      } else {
        int prefixLength = Integer.parseInt(parts[1]);
        return Ip6.numSubnetBitsToSubnetMask(prefixLength).inverted();
      }
    } else {
      return Ip6.ZERO;
    }
  }

  public Ip6Wildcard(Ip6 ip) {
    this(new Prefix6(ip, Prefix6.MAX_PREFIX_LENGTH));
  }

  public Ip6Wildcard(Ip6 address, Ip6 wildcardMask) {
    super(address, wildcardMask);
    if (!wildcardMask.valid()) {
      throw new BatfishException("Invalid wildcard: " + wildcardMask);
    }
  }

  public Ip6Wildcard(Prefix6 prefix) {
    this(prefix.getAddress(), prefix.getPrefixWildcard());
  }

  @JsonCreator
  public Ip6Wildcard(String str) {
    super(parseAddress(str), parseMask(str));
  }

  public boolean contains(Ip6 ip) {
    BigInteger wildcardIpAsBigInteger = getIp().asBigInteger();
    BigInteger wildcardMask = getWildcard().asBigInteger();
    BigInteger ipAsBigInteger = ip.asBigInteger();
    BigInteger maskedIpAsBigInteger = ipAsBigInteger.or(wildcardMask);
    BigInteger maskedWildcard = wildcardIpAsBigInteger.or(wildcardMask);
    return maskedIpAsBigInteger.equals(maskedWildcard);
  }

  public Ip6 getIp() {
    return _first;
  }

  public Ip6 getWildcard() {
    return _second;
  }

  public boolean isPrefix() {
    BigInteger w = _second.asBigInteger();
    BigInteger wp = w.add(BigInteger.ONE);
    int numTrailingZeros = wp.getLowestSetBit();
    BigInteger check = BigInteger.ONE.shiftLeft(numTrailingZeros);
    return wp.equals(check);
  }

  public Prefix6 toPrefix() {
    if (isPrefix()) {
      return new Prefix6(_first, _second.inverted());
    } else {
      throw new BatfishException("Invalid wildcard format for conversion to prefix: " + _second);
    }
  }

  @JsonValue
  @Override
  public String toString() {
    if (_second.equals(Ip6.ZERO)) {
      return _first.toString();
    } else if (isPrefix()) {
      return toPrefix().toString();
    } else {
      return _first + ";" + _second;
    }
  }
}
