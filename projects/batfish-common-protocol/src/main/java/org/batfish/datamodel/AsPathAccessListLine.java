package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** A line in an AsPathAccessList */
@ParametersAreNonnullByDefault
public final class AsPathAccessListLine implements Serializable, Comparable<AsPathAccessListLine> {
  private static final String PROP_ACTION = "action";
  private static final String PROP_REGEX = "regex";

  private static final long serialVersionUID = 1L;

  @Nonnull private LineAction _action;

  @Nonnull private String _regex;

  @JsonCreator
  private static AsPathAccessListLine jsonCreator(
      @Nullable @JsonProperty(PROP_ACTION) LineAction action,
      @Nullable @JsonProperty(PROP_REGEX) String regex) {
    checkArgument(action != null, "%s must be provided", PROP_ACTION);
    checkArgument(regex != null, "%s must be provided", PROP_REGEX);
    return new AsPathAccessListLine(action, regex);
  }

  public AsPathAccessListLine(LineAction action, String regex) {
    _action = action;
    _regex = regex;
  }

  @Override
  public int compareTo(AsPathAccessListLine rhs) {
    int ret = _regex.compareTo(rhs._regex);
    return ret;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AsPathAccessListLine)) {
      return false;
    }
    AsPathAccessListLine other = (AsPathAccessListLine) o;
    return _action == other._action && _regex.equals(other._regex);
  }

  @JsonPropertyDescription(
      "The action the underlying access-list will take when this line matches a route.")
  @JsonProperty(PROP_ACTION)
  @Nonnull
  public LineAction getAction() {
    return _action;
  }

  @JsonPropertyDescription("The regex against which a route's AS-path will be compared")
  @JsonProperty(PROP_REGEX)
  @Nonnull
  public String getRegex() {
    return _regex;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _action.ordinal();
    result = prime * result + _regex.hashCode();
    return result;
  }

  public void setAction(LineAction action) {
    _action = action;
  }

  public void setRegex(String regex) {
    _regex = regex;
  }
}
