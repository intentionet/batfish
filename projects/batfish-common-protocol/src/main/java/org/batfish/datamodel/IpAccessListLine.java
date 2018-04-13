package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.MoreObjects;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.TrueExpr;

@JsonSchemaDescription("A line in an IpAccessList")
public final class IpAccessListLine implements Serializable {

  public static class Builder {

    private LineAction _action;

    private AclLineMatchExpr _matchCondition;

    private String _name;

    private Builder() {}

    public Builder accepting() {
      _action = LineAction.ACCEPT;
      return this;
    }

    public IpAccessListLine build() {
      return new IpAccessListLine(_action, _matchCondition, _name);
    }

    public Builder rejecting() {
      _action = LineAction.REJECT;
      return this;
    }

    public Builder setAction(LineAction action) {
      _action = action;
      return this;
    }

    public Builder setMatchCondition(AclLineMatchExpr matchCondition) {
      _matchCondition = matchCondition;
      return this;
    }

    public Builder setName(String name) {
      _name = name;
      return this;
    }
  }

  public static final IpAccessListLine ACCEPT_ALL =
      accepting().setMatchCondition(TrueExpr.INSTANCE).build();

  private static final String PROP_ACTION = "action";

  private static final String PROP_MATCH_CONDITION = "matchCondition";

  private static final String PROP_NAME = "name";

  public static final IpAccessListLine REJECT_ALL =
      rejecting().setMatchCondition(TrueExpr.INSTANCE).build();

  private static final long serialVersionUID = 1L;

  public static Builder accepting() {
    return new Builder().setAction(LineAction.ACCEPT);
  }

  public static IpAccessListLine acceptingHeaderSpace(HeaderSpace headerSpace) {
    return accepting().setMatchCondition(new MatchHeaderSpace(headerSpace)).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder rejecting() {
    return new Builder().setAction(LineAction.REJECT);
  }

  public static IpAccessListLine rejectingHeaderSpace(HeaderSpace headerSpace) {
    return rejecting().setMatchCondition(new MatchHeaderSpace(headerSpace)).build();
  }

  private final LineAction _action;

  private final AclLineMatchExpr _matchCondition;

  private final String _name;

  @JsonCreator
  public IpAccessListLine(
      @JsonProperty(PROP_ACTION) @Nonnull LineAction action,
      @JsonProperty(PROP_MATCH_CONDITION) @Nonnull AclLineMatchExpr matchCondition,
      @JsonProperty(PROP_NAME) String name) {
    _action = Objects.requireNonNull(action);
    _matchCondition = Objects.requireNonNull(matchCondition);
    _name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IpAccessListLine)) {
      return false;
    }
    IpAccessListLine other = (IpAccessListLine) obj;
    return _action == other._action
        && Objects.equals(_matchCondition, other._matchCondition)
        && Objects.equals(_name, other._name);
  }

  @JsonPropertyDescription(
      "The action the underlying access-list will take when this line matches an IPV4 packet.")
  public @Nonnull LineAction getAction() {
    return _action;
  }

  public @Nonnull AclLineMatchExpr getMatchCondition() {
    return _matchCondition;
  }

  @JsonSchemaDescription("The name of this line in the list")
  public String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_action, _matchCondition, _name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .omitNullValues()
        .add(PROP_ACTION, _action)
        .add(PROP_MATCH_CONDITION, _matchCondition)
        .add(PROP_NAME, _name)
        .toString();
  }
}
