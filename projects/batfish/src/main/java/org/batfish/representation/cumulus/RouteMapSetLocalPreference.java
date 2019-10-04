package org.batfish.representation.cumulus;

import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.statement.SetLocalPreference;
import org.batfish.datamodel.routing_policy.statement.Statement;

/** A {@link RouteMapSet} that sets local preference on a BGP route */
public class RouteMapSetLocalPreference implements RouteMapSet {

  private final long _localPreference;

  public RouteMapSetLocalPreference(long localPreference) {
    _localPreference = localPreference;
  }

  public long getLocalPreference() {
    return _localPreference;
  }

  @Nonnull
  @Override
  public Stream<Statement> toStatements(Configuration c, CumulusNcluConfiguration vc, Warnings w) {
    return Stream.of(new SetLocalPreference(new LiteralLong(_localPreference)));
  }
}
