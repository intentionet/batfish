package org.batfish.representation.cisco;

import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;

/** Never matches; used for unimplemented features. */
public class RouteMapMatchUnimplementedLine extends RouteMapMatchLine {

  @Override
  public BooleanExpr toBooleanExpr(Configuration c, CiscoConfiguration cc, Warnings w) {
    return BooleanExprs.FALSE;
  }
}
