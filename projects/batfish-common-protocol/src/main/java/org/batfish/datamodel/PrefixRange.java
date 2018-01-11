package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;

public final class PrefixRange implements Serializable, Comparable<PrefixRange> {

  /** */
  private static final long serialVersionUID = 1L;

  public PrefixRange(Prefix prefix, SubRange lengthRange) {
    // Canonicalize the prefix by dropping extra bits in the address that are longer than any
    // relevant length.
    int realPrefixLength = Math.min(prefix.getPrefixLength(), lengthRange.getEnd());
    Ip realPrefixAddress = prefix.getAddress().getNetworkAddress(realPrefixLength);
    this._prefix = new Prefix(realPrefixAddress, prefix.getPrefixLength()).getNetworkPrefix();
    this._lengthRange = lengthRange;
  }

  /** Returns a {@link PrefixRange} that contains exactly the specified {@link Prefix}. */
  public static PrefixRange fromPrefix(Prefix prefix) {
    int prefixLength = prefix.getPrefixLength();
    return new PrefixRange(prefix, new SubRange(prefixLength, prefixLength));
  }

  @JsonCreator
  public static PrefixRange fromString(String prefixRangeStr) {
    String[] parts = prefixRangeStr.split(":");
    if (parts.length == 1) {
      return fromPrefix(new Prefix(parts[0]));
    } else if (parts.length == 2) {
      return new PrefixRange(new Prefix(parts[0]), new SubRange(parts[1]));
    } else {
      throw new BatfishException("Invalid PrefixRange string: '" + prefixRangeStr + "'");
    }
  }

  public SubRange getLengthRange() {
    return _lengthRange;
  }

  public Prefix getPrefix() {
    return _prefix;
  }

  public boolean includesPrefixRange(PrefixRange argPrefixRange) {
    Prefix prefix = getPrefix();
    SubRange lengthRange = getLengthRange();
    int prefixLength = prefix.getPrefixLength();
    long maskedPrefixAsLong = prefix.getAddress().getNetworkAddress(prefixLength).asLong();
    Prefix argPrefix = argPrefixRange.getPrefix();
    SubRange argLengthRange = argPrefixRange.getLengthRange();
    long argMaskedPrefixAsLong = argPrefix.getAddress().getNetworkAddress(prefixLength).asLong();
    return maskedPrefixAsLong == argMaskedPrefixAsLong
        && lengthRange.getStart() <= argLengthRange.getStart()
        && lengthRange.getEnd() >= argLengthRange.getEnd();
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
      return _prefix + ":" + low + "-" + high;
    }
  }

  @Override
  public int compareTo(@Nonnull PrefixRange o) {
    int prefixCmp = this._prefix.compareTo(o._prefix);
    if (prefixCmp != 0) {
      return prefixCmp;
    }

    return this._lengthRange.compareTo(o._lengthRange);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_prefix, _lengthRange);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof PrefixRange)) {
      return false;
    }
    PrefixRange o = (PrefixRange) obj;
    return _prefix.equals(o._prefix) && _lengthRange.equals(o._lengthRange);
  }

  private final Prefix _prefix;
  private final SubRange _lengthRange;
}
