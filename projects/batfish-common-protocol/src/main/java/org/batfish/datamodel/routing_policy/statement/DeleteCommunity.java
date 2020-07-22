package org.batfish.datamodel.routing_policy.statement;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;

@ParametersAreNonnullByDefault
public final class DeleteCommunity extends Statement {
  private static final String PROP_EXPR = "expr";

  @Nonnull private final CommunitySetExpr _expr;

  @JsonCreator
  private static DeleteCommunity jsonCreator(
      @Nullable @JsonProperty(PROP_EXPR) CommunitySetExpr expr) {
    checkArgument(expr != null, "%s must be provided", PROP_EXPR);
    return new DeleteCommunity(expr);
  }

  public DeleteCommunity(CommunitySetExpr expr) {
    _expr = expr;
  }

  @Override
  public <T, U> T accept(StatementVisitor<T, U> visitor, U arg) {
    return visitor.visitDeleteCommunity(this, arg);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof DeleteCommunity)) {
      return false;
    }
    DeleteCommunity other = (DeleteCommunity) obj;
    return _expr.equals(other._expr);
  }

  @Override
  public Result execute(Environment environment) {
    BgpRoute.Builder<?, ?> outputRouteBuilder =
        (BgpRoute.Builder<?, ?>) environment.getOutputRoute();
    Set<Community> currentCommunities = outputRouteBuilder.getCommunities();
    SortedSet<Community> matchingCommunities =
        _expr.matchedCommunities(environment, currentCommunities);
    outputRouteBuilder.removeCommunities(matchingCommunities);
    Result result = new Result();
    return result;
  }

  @JsonProperty(PROP_EXPR)
  @Nonnull
  public CommunitySetExpr getExpr() {
    return _expr;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _expr.hashCode();
    return result;
  }
}
