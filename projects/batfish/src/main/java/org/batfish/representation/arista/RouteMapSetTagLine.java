package org.batfish.representation.arista;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.statement.SetTag;
import org.batfish.datamodel.routing_policy.statement.Statement;

/** Represents a {@code `set tag n`} line in a route-map. */
public class RouteMapSetTagLine extends RouteMapSetLine {

  private final long _tag;

  public RouteMapSetTagLine(long tag) {
    _tag = tag;
  }

  @Override
  public void applyTo(
      List<Statement> statements, AristaConfiguration cc, Configuration c, Warnings w) {
    statements.add(new SetTag(new LiteralLong(_tag)));
  }
}
