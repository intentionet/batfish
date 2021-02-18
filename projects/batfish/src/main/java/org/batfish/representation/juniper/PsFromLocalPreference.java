package org.batfish.representation.juniper;

import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.IntComparator;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.MatchLocalPreference;

/** Represents a "from local-preference" line in a {@link PsTerm} */
public final class PsFromLocalPreference extends PsFrom {

  private final long _localPreference;

  public PsFromLocalPreference(long localPreference) {
    _localPreference = localPreference;
  }

  public long getLocalPreference() {
    return _localPreference;
  }

  @Override
  public BooleanExpr toBooleanExpr(JuniperConfiguration jc, Configuration c, Warnings warnings) {
    return new MatchLocalPreference(IntComparator.EQ, new LiteralLong(_localPreference));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof PsFromLocalPreference)) {
      return false;
    }
    PsFromLocalPreference that = (PsFromLocalPreference) o;
    return _localPreference == that._localPreference;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(_localPreference);
  }
}
