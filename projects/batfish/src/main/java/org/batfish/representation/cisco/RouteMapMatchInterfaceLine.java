package org.batfish.representation.cisco;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;

@ParametersAreNonnullByDefault
public class RouteMapMatchInterfaceLine extends RouteMapMatchLine {

  private Set<String> _ifaceNames;

  public RouteMapMatchInterfaceLine(Set<String> names) {
    _ifaceNames = ImmutableSet.copyOf(names);
  }

  public Set<String> getInterfaceNames() {
    return _ifaceNames;
  }

  @Override
  public BooleanExpr toBooleanExpr(Configuration c, CiscoConfiguration cc, Warnings w) {
    /*
    TODO Support route-map match interface for route filtering. Matches any routes with the
     specified next hop interface.
     https://www.cisco.com/c/en/us/td/docs/security/asa/asa91/configuration/general/asa_91_general_config/route_maps.pdf
    */
    w.redFlag(
        "Route-map match interface is not fully supported. Batfish will ignore clauses using match"
            + " interface for route filtering.");
    return BooleanExprs.FALSE;
  }
}
