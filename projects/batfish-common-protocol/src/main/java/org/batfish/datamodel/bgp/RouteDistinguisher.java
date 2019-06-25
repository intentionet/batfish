package org.batfish.datamodel.bgp;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;

/**
 * BGP route distinguisher
 *
 * <p>See <a href="https://tools.ietf.org/html/rfc4364#section-4.2">RFC</a> for details
 */
@ParametersAreNonnullByDefault
public final class RouteDistinguisher implements Serializable, Comparable<RouteDistinguisher> {

  private static final long serialVersionUID = 1L;

  private static final String ERR_MSG_SHORT_TEMPLATE =
      "ASN value required to be in range 0 to 0xFFFF, %d was provided";
  private static final String ERR_MSG_INT_TEMPLATE =
      "ASN value required to be in range 0 to 0xFFFFFFFF, %d was provided";

  private enum Type {
    TYPE0,
    TYPE1,
    TYPE2
  }

  private final long _value;
  @Nonnull private final Type _type;

  private RouteDistinguisher(long value, Type type) {
    _value = value;
    _type = type;
  }

  @JsonCreator
  private static RouteDistinguisher create(@Nullable String value) {
    checkArgument(value != null);
    return parse(value);
  }

  /** Attempt to create a route distinguisher from a given string value */
  @Nonnull
  public static RouteDistinguisher parse(String value) {
    String[] arr = value.split(":");
    checkArgument(arr.length == 2, "At most one occurrence of ':' is allowed. Input was %s", value);
    // First element will dictate type of the route distinguisher
    Integer asn1 = Ints.tryParse(arr[0]);
    // If first element is a valid 2-byte int, it's a type 0
    if (asn1 != null && asn1 <= 0xFFFF) {
      return from(asn1, Long.parseUnsignedLong(arr[1]));
    }
    // If first element is a valid 4-byte int, it's a type 2
    Long asn1Long = Longs.tryParse(arr[0]);
    if (asn1Long != null) {
      return from(asn1Long, Integer.parseUnsignedInt(arr[1]));
    }
    // Finally fall back to trying to parse an IP address for type 1
    return from(Ip.parse(arr[0]), Integer.parseUnsignedInt(arr[1]));
  }

  /**
   * Create a type 0 route distinguisher
   *
   * @param asn1 a valid 2-byte administrator subfield
   * @param asn2 a valid 4-byte assigned number subfield
   */
  @Nonnull
  public static RouteDistinguisher from(int asn1, long asn2) {
    checkArgument(asn1 >= 0 && asn1 <= 0xFFFF, ERR_MSG_SHORT_TEMPLATE, asn1);
    checkArgument(asn2 >= 0 && asn2 <= 0xFFFFFFFFL, ERR_MSG_INT_TEMPLATE, asn2);
    return new RouteDistinguisher(((long) asn1 << 32) | asn2, Type.TYPE0);
  }

  /**
   * Create a type 1 route distinguisher
   *
   * @param ip a valid 4-byte IP address as the administrator subfield
   * @param asn a valid 2-byte assigned number subfield
   */
  @Nonnull
  public static RouteDistinguisher from(Ip ip, int asn) {
    checkArgument(ip.asLong() >= 0, "Invalid IP value specified: %s", ip);
    checkArgument(asn >= 0 && asn <= 0xFFFFL, ERR_MSG_SHORT_TEMPLATE, asn);
    return new RouteDistinguisher((ip.asLong() << 16) | asn, Type.TYPE1);
  }

  /**
   * Create a type 2 route distinguisher
   *
   * @param asn1 a valid 4-byte administrator subfield
   * @param asn2 a valid 2-byte assigned number subfield
   */
  @Nonnull
  public static RouteDistinguisher from(long asn1, int asn2) {
    checkArgument(asn1 >= 0 && asn1 <= 0xFFFFFFFFL, ERR_MSG_INT_TEMPLATE, asn1);
    checkArgument(asn2 >= 0 && asn2 <= 0xFFFFL, ERR_MSG_SHORT_TEMPLATE, asn2);
    return new RouteDistinguisher((asn1 << 16) | asn2, Type.TYPE2);
  }

  /**
   * Create a new route distinguisher, infer the correct type (1 or 2) based on passed in values.
   *
   * @param asn1 a valid 4-byte administrator subfield
   * @param asn2 a valid 2-byte assigned number subfield
   * @throws IllegalArgumentException if the values are not
   */
  @Nonnull
  public static RouteDistinguisher from(long asn1, long asn2) {
    if (asn1 <= 0xFFFF) {
      return from((int) asn1, asn2);
    }
    checkArgument(asn2 <= 0xFFFF, ERR_MSG_SHORT_TEMPLATE, asn2);
    return from(asn1, (int) asn2);
  }

  public long getValue() {
    return _value;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RouteDistinguisher)) {
      return false;
    }
    RouteDistinguisher that = (RouteDistinguisher) o;
    return _value == that._value && _type == that._type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_value, _type);
  }

  @Override
  public int compareTo(RouteDistinguisher o) {
    // Only compare long values. Main use case: order determinism in JSON
    return Long.compare(_value, o._value);
  }

  @Override
  @Nonnull
  @JsonValue
  public String toString() {
    switch (_type) {
      case TYPE0:
        // Administrator (2 bytes):AssignedNumber (4 bytes)
        return (_value >> 32) + ":" + (_value & 0xFFFFFFFFL);
      case TYPE1:
        // Administrator (4 bytes, IP address):AssignedNumber (2 bytes)
        return Ip.create(_value >> 16).toString() + ":" + (_value & 0xFFFFL);
      case TYPE2:
        // Administrator (4 bytes, IP address):AssignedNumber (2 bytes)
        return (_value >> 16) + ":" + (_value & 0xFFFFL);
      default:
        return Long.toString(_value);
    }
  }
}
