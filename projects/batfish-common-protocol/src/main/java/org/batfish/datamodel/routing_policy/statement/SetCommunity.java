package org.batfish.datamodel.routing_policy.statement;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.EmptyCommunitySetExpr;

public final class SetCommunity extends Statement {

  private static final long serialVersionUID = 1L;

  private static final String PROP_EXPR = "expr";

  public static final SetCommunity NONE = new SetCommunity(EmptyCommunitySetExpr.INSTANCE);

  @Nonnull private CommunitySetExpr _expr;

  @JsonCreator
  private static SetCommunity jsonCreator(
      @Nullable @JsonProperty(PROP_EXPR) CommunitySetExpr expr) {
    checkArgument(expr != null, "%s must be provided", PROP_EXPR);
    return new SetCommunity(expr);
  }

  public SetCommunity(@Nonnull CommunitySetExpr expr) {
    _expr = expr;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof SetCommunity)) {
      return false;
    }
    SetCommunity other = (SetCommunity) obj;
    return _expr.equals(other._expr);
  }

  @Override
  public Result execute(Environment environment) {
    Result result = new Result();
    BgpRoute.Builder bgpRoute = (BgpRoute.Builder) environment.getOutputRoute();
    SortedSet<Community> communities = _expr.asLiteralCommunities(environment);
    bgpRoute.setCommunities(communities);
    if (environment.getWriteToIntermediateBgpAttributes()) {
      environment.getIntermediateBgpAttributes().setCommunities(communities);
    }
    return result;
  }

  @JsonProperty(PROP_EXPR)
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

  public void setExpr(@Nonnull CommunitySetExpr expr) {
    _expr = expr;
  }
}
