package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** A line in an Ip6AccessList */
public final class Ip6AccessListLine extends Header6Space {
  private static final String PROP_ACTION = "action";
  private static final String PROP_NAME = "name";

  private static final long serialVersionUID = 1L;

  private LineAction _action;

  private String _name;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof Ip6AccessListLine)) {
      return false;
    }
    Ip6AccessListLine other = (Ip6AccessListLine) obj;
    return super.equals(obj) && _action == other._action;
  }

  @JsonPropertyDescription(
      "The action the underlying access-list will take when this line matches an IPV6 packet.")
  @JsonProperty(PROP_ACTION)
  public LineAction getAction() {
    return _action;
  }

  /** The name of this line in the list */
  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    // TODO: implement better hashcode
    return 0;
  }

  @JsonProperty(PROP_ACTION)
  public void setAction(LineAction action) {
    _action = action;
  }

  @JsonProperty(PROP_NAME)
  public void setName(String name) {
    _name = name;
  }

  @Override
  public String toString() {
    return "[Action:" + _action + ", Base: " + super.toString() + "]";
  }
}
