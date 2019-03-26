package org.batfish.representation.cisco;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.IntExpr;
import org.batfish.datamodel.routing_policy.statement.SetWeight;
import org.batfish.datamodel.routing_policy.statement.Statement;

/** Represents a {@code `set weight [n]`} line in a route-map. */
public class RouteMapSetWeightLine extends RouteMapSetLine {

  private static final long serialVersionUID = 1L;

  private IntExpr _weight;

  public RouteMapSetWeightLine(IntExpr weight) {
    _weight = weight;
  }

  @Override
  public void applyTo(
      List<Statement> statements, CiscoConfiguration cc, Configuration c, Warnings w) {
    statements.add(new SetWeight(_weight));
  }

  public IntExpr getWeight() {
    return _weight;
  }

  @Override
  public RouteMapSetType getType() {
    return RouteMapSetType.WEIGHT;
  }
}
