package org.batfish.datamodel.bgp.community;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.primitives.Ints;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;

/**
 * Represents an extended BGP community, as described in <a
 * href=https://tools.ietf.org/html/rfc4360">RFC4360</a>
 */
@ParametersAreNonnullByDefault
public final class ExtendedCommunity implements Community {

  private static final long serialVersionUID = 1L;

  private final byte _type;
  private final byte _subType;
  private final long _globalAdministrator;
  private final long _localAdministrator;

  // Cached string representations
  @Nullable private String _str;
  @Nullable private String _regexStr;

  private ExtendedCommunity(
      byte type, byte subType, long globalAdministrator, long localAdministrator) {
    _type = type;
    _subType = subType;
    _globalAdministrator = globalAdministrator;
    _localAdministrator = localAdministrator;
  }

  @JsonCreator
  private static ExtendedCommunity create(@Nullable String value) {
    checkArgument(value != null, "Extended community string cannot be null");
    return parse(value);
  }

  @Nonnull
  public static ExtendedCommunity parse(String communityStr) {
    String[] parts = communityStr.trim().split(":");
    checkArgument(parts.length == 3, "Invalid extended community string: %s", communityStr);
    long gaLong;
    long laLong;
    String subType = parts[0].toLowerCase();
    String globalAdministrator = parts[1].toLowerCase();
    String localAdministrator = parts[2].toLowerCase();

    // Try to figure out type/subtype first
    Byte typeByte = null;
    byte subTypeByte;
    // Special values
    if (subType.equals("target")) {
      subTypeByte = 0x02;
    } else if (subType.equals("origin")) {
      subTypeByte = 0x03;
    } else {
      // They type/subtype is a literal integer
      Integer intVal = Ints.tryParse(subType);
      if (intVal != null && intVal < 0xFFFF && intVal > 0) {
        subTypeByte = (byte) (intVal & 0xFF);
        typeByte = (byte) ((intVal & 0xFF00) >> 8);
      } else {
        throw new IllegalArgumentException(
            String.format("Invalid extended community type: %s", communityStr));
      }
    }
    // Local administrator, can only be a number
    laLong = Long.parseUnsignedLong(localAdministrator);
    // Gloval administrator, is complicated. Try a bunch of combinations
    String[] gaParts = globalAdministrator.split("\\.");
    if (gaParts.length == 4) { // Dotted IP address notation
      // type 0x01, 1-byte subtype, 4-byte ip address, 2-byte number la
      typeByte = firstNonNull(typeByte, (byte) 0x01);
      // Ip.parse() ensures IP is valid
      gaLong = Ip.parse(globalAdministrator).asLong();
      checkArgument(laLong <= 0xFFFFL, "Invalid local administrator value %s", localAdministrator);
    } else if (gaParts.length == 2) { // Dotted AS notation
      // type 0x02, 1-byte subtype, 2-byte.2-byte dotted as, 2-byte number la
      typeByte = firstNonNull(typeByte, (byte) 0x02);
      int hi = Integer.parseUnsignedInt(gaParts[0]);
      int lo = Integer.parseUnsignedInt(gaParts[1]);
      checkArgument(
          hi <= 0xFFFF && lo <= 0xFFFF,
          "Invalid global administrator value %s",
          globalAdministrator);
      gaLong = hi << 16 | lo;
      checkArgument(laLong <= 0xFFFFL, "Invalid local administrator value %s", localAdministrator);
    } else { // Regular numbers, almost
      if (globalAdministrator.endsWith("l")) { // e.g., 123L, a shorthand for forcing 4-byte GA.
        // type 0x02, 1-byte subtype, 4-byte number ga, 2-byte number la
        typeByte = firstNonNull(typeByte, (byte) 0x02);
        // Strip the "L" and convert to long
        gaLong =
            Long.parseUnsignedLong(
                globalAdministrator.substring(0, globalAdministrator.length() - 1));
        checkArgument(
            gaLong <= 0xFFFFFFFFL, "Invalid global administrator value %s", globalAdministrator);
        checkArgument(
            laLong <= 0xFFFFL, "Invalid local administrator value %s", localAdministrator);
      } else { // No type hint, try both variants with 2-byte GA preferred
        // Try for 2-byte ga, unless the number is too big, in which case it is a 4-byte ga.
        gaLong = Long.parseUnsignedLong(globalAdministrator);
        if (gaLong <= 0xFFFFL) {
          checkArgument(
              laLong <= 0xFFFFFFFFL, "Invalid local administrator value %s", localAdministrator);
          // type 0x00, 1-byte subtype, 2-byte number ga, 4-byte number la
          typeByte = firstNonNull(typeByte, (byte) 0x00);
        } else {
          // type 0x02, 1-byte subtype, 4-byte number ga, 2-byte number la
          checkArgument(
              laLong <= 0xFFFFL, "Invalid local administrator value %s", localAdministrator);
          typeByte = firstNonNull(typeByte, (byte) 0x02);
        }
      }
    }
    return of((int) typeByte << 8 | (int) subTypeByte, gaLong, laLong);
  }

  public static ExtendedCommunity of(int type, long globalAdministrator, long localAdministrator) {
    checkArgument(
        type >= 0 && type <= 0xFFFF,
        "Extended community type %d is not within the allowed range",
        type);
    checkArgument(
        globalAdministrator >= 0 && localAdministrator >= 0,
        "Administrator values must be positive");
    checkArgument(
        (globalAdministrator <= 0xFFFFFFFFL && localAdministrator <= 0xFFFFL)
            || (globalAdministrator <= 0xFFFFL && localAdministrator <= 0xFFFFFFFFL),
        "Extended community administrator values are not within the allowed range");
    return new ExtendedCommunity(
        (byte) (type >> 8), (byte) (type & 0xFF), globalAdministrator, localAdministrator);
  }

  public static ExtendedCommunity of(int type, Ip globalAdministrator, long localAdministrator) {
    checkArgument(
        localAdministrator >= 0 && localAdministrator <= 0xFFFF,
        "Local admin value %d is not within allowed range",
        localAdministrator);
    checkArgument(
        globalAdministrator.asLong() >= 0,
        "Invalid IP for global admin value: %s",
        globalAdministrator);
    return of(type, globalAdministrator.asLong(), localAdministrator);
  }

  @Override
  public boolean isTransitive() {
    // Second most significant bit is set
    return (_type & (byte) 0x40) != 0;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExtendedCommunity)) {
      return false;
    }
    ExtendedCommunity that = (ExtendedCommunity) o;
    return _type == that._type
        && _subType == that._subType
        && _globalAdministrator == that._globalAdministrator
        && _localAdministrator == that._localAdministrator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type, _subType, _globalAdministrator, _localAdministrator);
  }

  @Override
  public String matchString() {
    if (_regexStr == null) {
      // Type is skipped for regex matching
      _regexStr = _globalAdministrator + ":" + _localAdministrator;
    }
    return _regexStr;
  }

  @Override
  @Nonnull
  @JsonValue
  public String toString() {
    if (_str == null) {
      // To differentiate 4 vs 2-byte global admin values
      String gaSuffix = _type == 0x00 || _type == 0x40 ? "" : "L";
      _str =
          (((int) _type << 8) | _subType)
              + ":"
              + _globalAdministrator
              + gaSuffix
              + ":"
              + _localAdministrator;
    }
    return _str;
  }
}
