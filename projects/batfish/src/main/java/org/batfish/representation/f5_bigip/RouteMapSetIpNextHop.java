package org.batfish.representation.f5_bigip;

import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.routing_policy.expr.IpNextHop;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.Statement;

/** Route-map transformation that set the next hop of the route */
public final class RouteMapSetIpNextHop implements RouteMapSet {

  public RouteMapSetIpNextHop(Ip nextHop) {
    _nextHop = nextHop;
  }

  public @Nonnull Ip getNextHop() {
    return _nextHop;
  }

  @Override
  public @Nonnull Stream<Statement> toStatements(
      Configuration c, F5BigipConfiguration vc, Warnings w) {
    return Stream.of(new SetNextHop(new IpNextHop(ImmutableList.of(_nextHop))));
  }

  private final @Nonnull Ip _nextHop;
}
