package org.batfish.representation.cumulus;

import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.LongExpr;
//import org.batfish.datamodel.routing_policy.expr.NamedCommunitySet;
//import org.batfish.datamodel.routing_policy.statement.DeleteCommunity;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.communities.CommunitySetDifference;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExprReference;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;

/** Clause of set comm-list delete in route map. */
public class RouteMapSetCommListDelete implements RouteMapSet {

  @Nonnull private final String _name;

  public RouteMapSetCommListDelete(@Nonnull String name) {
    _name = name;
  }

  @Nonnull
  @Override
  public Stream<Statement> toStatements(Configuration c, CumulusNodeConfiguration vc, Warnings w) {
    //return Stream.of(new DeleteCommunity(new NamedCommunitySet(_name)));
    return Stream.of(
            new SetCommunities(
                    new CommunitySetDifference(
                            InputCommunities.instance(), new CommunityMatchExprReference(_name))));
  }

  public @Nonnull String getName() {
    return _name;
  }
}
