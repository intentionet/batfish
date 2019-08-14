package org.batfish.representation.cumulus;

import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.routing_policy.expr.IpNextHop;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.Statement;

public final class RouteMapSetIpNextHopLiteral implements RouteMapSet {

  private final @Nonnull Ip _nextHop;

  public RouteMapSetIpNextHopLiteral(Ip nextHop) {
    _nextHop = nextHop;
  }

  public @Nonnull Ip getNextHop() {
    return _nextHop;
  }

  @Nonnull
  @Override
  public Stream<Statement> toStatements(Configuration c, CumulusNcluConfiguration vc, Warnings w) {
    return Stream.of(new SetNextHop(new IpNextHop(ImmutableList.of(_nextHop)), false));
  }
}
