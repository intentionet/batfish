package org.batfish.representation.cumulus;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.batfish.datamodel.routing_policy.statement.Statement;

public class RouteMapSetCommunity implements RouteMapSet {
  private @Nonnull List<StandardCommunity> _communities;

  public RouteMapSetCommunity(Iterable<StandardCommunity> communities) {
    _communities = ImmutableList.copyOf(communities);
  }

  @Nonnull
  @Override
  public Stream<Statement> toStatements(Configuration c, CumulusNcluConfiguration vc, Warnings w) {
    CommunitySetExpr communities = new LiteralCommunitySet(_communities);
    return Stream.of(new SetCommunity(communities));
  }

  public @Nonnull List<StandardCommunity> getCommunities() {
    return _communities;
  }

  public void setCommunities(List<StandardCommunity> communities) {
    _communities = communities;
  }
}
