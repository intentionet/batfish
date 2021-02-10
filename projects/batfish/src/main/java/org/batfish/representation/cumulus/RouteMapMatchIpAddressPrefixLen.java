package org.batfish.representation.cumulus;

import javax.annotation.Nonnull;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;

/**
 * A {@link RouteMapMatch} that matches routes based on whether the route's network has the given
 * prefix length.
 */
public final class RouteMapMatchIpAddressPrefixLen implements RouteMapMatch {

  private final int _len;

  public RouteMapMatchIpAddressPrefixLen(int len) {
    _len = len;
  }

  @Override
  public @Nonnull BooleanExpr toBooleanExpr(
      Configuration c, CumulusNodeConfiguration vc, Warnings w) {
    return new MatchPrefixSet(
        DestinationNetwork.instance(),
        new ExplicitPrefixSet(
            new PrefixSpace(new PrefixRange(Prefix.ZERO, SubRange.singleton(_len)))));
  }

  public int getLen() {
    return _len;
  }
}
