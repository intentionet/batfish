package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;

public final class MatchEntireCommunitySet extends BooleanExpr {

  private static final String PROP_EXPR = "expr";

  private final CommunitySetExpr _expr;

  @JsonCreator
  private static MatchEntireCommunitySet create(@JsonProperty(PROP_EXPR) CommunitySetExpr expr) {
    checkArgument(expr != null, "%s must be provided", PROP_EXPR);
    return new MatchEntireCommunitySet(expr);
  }

  public MatchEntireCommunitySet(CommunitySetExpr expr) {
    _expr = expr;
  }

  @Override
  public <T, U> T accept(BooleanExprVisitor<T, U> visitor, U arg) {
    return visitor.visitMatchEntireCommunitySet(this, arg);
  }

  @Override
  public Result evaluate(Environment environment) {
    Set<Community> inputCommunities = null;
    if (environment.getUseOutputAttributes()
        && environment.getOutputRoute() instanceof BgpRoute.Builder<?, ?>) {
      BgpRoute.Builder<?, ?> bgpRouteBuilder =
          (BgpRoute.Builder<?, ?>) environment.getOutputRoute();
      inputCommunities = bgpRouteBuilder.getCommunities();
    } else if (environment.getReadFromIntermediateBgpAttributes()) {
      inputCommunities = environment.getIntermediateBgpAttributes().getCommunities();
    } else if (environment.getOriginalRoute() instanceof BgpRoute) {
      BgpRoute<?, ?> bgpRoute = (BgpRoute<?, ?>) environment.getOriginalRoute();
      inputCommunities = bgpRoute.getCommunities();
    }
    return inputCommunities == null
        ? new Result(false)
        : new Result(_expr.matchCommunities(environment, inputCommunities));
  }

  @JsonProperty(PROP_EXPR)
  public CommunitySetExpr getExpr() {
    return _expr;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MatchEntireCommunitySet)) {
      return false;
    }
    MatchEntireCommunitySet other = (MatchEntireCommunitySet) obj;
    return Objects.equals(_expr, other._expr);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(_expr);
  }
}
