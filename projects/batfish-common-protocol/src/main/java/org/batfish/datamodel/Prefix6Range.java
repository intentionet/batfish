package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;

@ParametersAreNonnullByDefault
public class Prefix6Range implements Serializable, Comparable<Prefix6Range> {

  public static Prefix6Range fromPrefix6(Prefix6 prefix6) {
    int prefix6Length = prefix6.getPrefixLength();
    return new Prefix6Range(prefix6, SubRange.singleton(prefix6Length));
  }

  private static SubRange lengthRangeFromStr(String str) {
    Prefix6 prefix6;
    String[] mainParts = str.split(";");
    int numMainParts = mainParts.length;
    if (numMainParts < 1 || numMainParts > 2) {
      throw new BatfishException("Invalid Prefix6Range string: '" + str + "'");
    } else {
      prefix6 = Prefix6.parse(mainParts[0]);
      if (mainParts.length == 1) {
        int prefix6Length = prefix6.getPrefixLength();
        return SubRange.singleton(prefix6Length);
      } else {
        return new SubRange(mainParts[1]);
      }
    }
  }

  private static Prefix6 prefix6FromStr(String str) {
    Prefix6 prefix6;
    String[] mainParts = str.split(";");
    int numMainParts = mainParts.length;
    if (numMainParts < 1 || numMainParts > 2) {
      throw new BatfishException("Invalid Prefix6Range string: '" + str + "'");
    } else {
      prefix6 = Prefix6.parse(mainParts[0]);
      return prefix6;
    }
  }

  public Prefix6Range(Prefix6 prefix6, SubRange lengthRange) {
    _prefix = prefix6;
    _lengthRange = lengthRange;
  }

  @JsonCreator
  private static Prefix6Range jsonCreator(@Nullable String str) {
    checkArgument(str != null, "Prefix6Range cannot be null");
    return Prefix6Range.parse(str);
  }

  public static Prefix6Range parse(String str) {
    return new Prefix6Range(prefix6FromStr(str), lengthRangeFromStr(str));
  }

  @Nonnull
  public SubRange getLengthRange() {
    return _lengthRange;
  }

  @Nonnull
  public Prefix6 getPrefix6() {
    return _prefix;
  }

  public boolean includesPrefix6(Prefix6 argPrefix6) {
    Prefix6 prefix6 = getPrefix6();
    SubRange lengthRange = getLengthRange();
    int prefixLength = prefix6.getPrefixLength();
    int minPrefixLength = lengthRange.getStart();
    int maxPrefixLength = lengthRange.getEnd();
    int argPrefixLength = argPrefix6.getPrefixLength();
    if (minPrefixLength > argPrefixLength || maxPrefixLength < argPrefixLength) {
      return false;
    }
    BigInteger maskedPrefixAsBigInteger =
        prefix6.getAddress().getNetworkAddress(prefixLength).asBigInteger();
    BigInteger argMaskedPrefixAsBigInteger =
        argPrefix6.getAddress().getNetworkAddress(prefixLength).asBigInteger();
    return maskedPrefixAsBigInteger.equals(argMaskedPrefixAsBigInteger);
  }

  public boolean includesPrefix6Range(Prefix6Range argPrefixRange) {
    Prefix6 prefix6 = getPrefix6();
    SubRange lengthRange = getLengthRange();
    int prefixLength = prefix6.getPrefixLength();
    BigInteger maskedPrefixAsBigInteger =
        prefix6.getAddress().getNetworkAddress(prefixLength).asBigInteger();
    Prefix6 argPrefix = argPrefixRange.getPrefix6();
    SubRange argLengthRange = argPrefixRange.getLengthRange();
    BigInteger argMaskedPrefixAsBigInteger =
        argPrefix.getAddress().getNetworkAddress(prefixLength).asBigInteger();
    return maskedPrefixAsBigInteger.equals(argMaskedPrefixAsBigInteger)
        && lengthRange.getStart() <= argLengthRange.getStart()
        && lengthRange.getEnd() >= argLengthRange.getEnd();
  }

  @Nonnull private final Prefix6 _prefix;
  @Nonnull private final SubRange _lengthRange;

  @Override
  public int compareTo(Prefix6Range o) {
    return Comparator.comparing(Prefix6Range::getPrefix6)
        .thenComparing(Prefix6Range::getLengthRange)
        .compare(this, o);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Prefix6Range)) {
      return false;
    }
    Prefix6Range r = (Prefix6Range) o;
    return _prefix.equals(r._prefix) && _lengthRange.equals(r._lengthRange);
  }

  @Override
  public int hashCode() {
    return 31 * _prefix.hashCode() + _lengthRange.hashCode();
  }

  @Override
  @JsonValue
  public String toString() {
    int prefixLength = _prefix.getPrefixLength();
    int low = _lengthRange.getStart();
    int high = _lengthRange.getEnd();
    if (prefixLength == low && prefixLength == high) {
      return _prefix.toString();
    } else {
      return _prefix + ";" + low + "-" + high;
    }
  }
}
