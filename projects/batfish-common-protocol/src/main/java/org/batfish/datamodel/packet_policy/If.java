package org.batfish.datamodel.packet_policy;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class If implements Statement {

  private static final String PROP_MATCH_CONDITION = "matchCondition";
  private static final String PROP_ACTIONS = "actions";
  private static final long serialVersionUID = 1;

  @Nonnull private final List<Statement> _actions;
  @Nonnull private final Statement _matchCondition;

  public If(Statement matchCondition, List<Statement> actions) {
    _matchCondition = matchCondition;
    _actions = ImmutableList.copyOf(actions);
  }

  @JsonCreator
  @Nonnull
  private static If jsonCreator(
      @Nullable @JsonProperty(PROP_ACTIONS) List<Statement> actions,
      @Nullable @JsonProperty(PROP_MATCH_CONDITION) Statement matchCondition) {
    checkArgument(matchCondition != null, "Missing %s", PROP_MATCH_CONDITION);
    return new If(matchCondition, firstNonNull(actions, ImmutableList.of()));
  }

  @Nonnull
  @JsonProperty(PROP_ACTIONS)
  public List<Statement> getActions() {
    return _actions;
  }

  @Nonnull
  @JsonProperty(PROP_MATCH_CONDITION)
  public Statement getMatchCondition() {
    return _matchCondition;
  }

  @Override
  public <T> T accept(PacketPolicyVisitor<T> visitor) {
    return visitor.visitIf(this);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    If anIf = (If) o;
    return Objects.equals(getMatchCondition(), anIf.getMatchCondition())
        && Objects.equals(getActions(), anIf.getActions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMatchCondition(), getActions());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).toString();
  }
}
