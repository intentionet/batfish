package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.MoreObjects;
import java.io.Serializable;

/** A line in an Route6FilterList */
public class Route6FilterLine implements Serializable {

  private static final String PROP_ACTION = "action";

  private static final String PROP_LENGTH_RANGE = "lengthRange";

  private static final String PROP_IP_WILDCARD = "ipWildcard";

  private static final long serialVersionUID = 1L;

  private final LineAction _action;

  private final SubRange _lengthRange;

  private final Ip6Wildcard _ipWildcard;

  @JsonCreator
  public Route6FilterLine(
      @JsonProperty(PROP_ACTION) LineAction action,
      @JsonProperty(PROP_IP_WILDCARD) Ip6Wildcard ipWildcard,
      @JsonProperty(PROP_LENGTH_RANGE) SubRange lengthRange) {
    _action = action;
    _ipWildcard = ipWildcard;
    _lengthRange = lengthRange;
  }

  public Route6FilterLine(LineAction action, Prefix6 prefix, SubRange lengthRange) {
    _action = action;
    _ipWildcard = new Ip6Wildcard(prefix);
    _lengthRange = lengthRange;
  }

  public Route6FilterLine(LineAction action, Prefix6Range prefixRange) {
    this(action, new Ip6Wildcard(prefixRange.getPrefix6()), prefixRange.getLengthRange());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof Route6FilterLine)) {
      return false;
    }

    Route6FilterLine other = (Route6FilterLine) obj;
    return _action == other._action
        && _lengthRange.equals(other._lengthRange)
        && _ipWildcard.equals(other._ipWildcard);
  }

  @JsonProperty(PROP_ACTION)
  @JsonPropertyDescription(
      "The action the underlying access-list will take when this line matches an IPV6 route.")
  public LineAction getAction() {
    return _action;
  }

  @JsonProperty(PROP_LENGTH_RANGE)
  @JsonPropertyDescription("The range of acceptable prefix-lengths for a route.")
  public SubRange getLengthRange() {
    return _lengthRange;
  }

  @JsonProperty(PROP_IP_WILDCARD)
  @JsonPropertyDescription(
      "The bits against which to compare a route's prefix. The mask of this IP Wildcard determines "
          + "which bits must match")
  public Ip6Wildcard getIpWildcard() {
    return _ipWildcard;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _action.ordinal();
    result = prime * result + _lengthRange.hashCode();
    result = prime * result + _ipWildcard.hashCode();
    return result;
  }

  public String toCompactString() {
    StringBuilder sb = new StringBuilder();
    sb.append(_action + " ");
    sb.append(_ipWildcard + " ");
    sb.append(_lengthRange + " ");
    return sb.toString();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .add("Action", _action)
        .add("IpWildCard", _ipWildcard)
        .add("LengthRange", _lengthRange)
        .toString();
  }
}
