package org.batfish.representation.cisco_xr;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.as_path.HasAsPathLength;
import org.batfish.datamodel.routing_policy.as_path.InputAsPath;
import org.batfish.datamodel.routing_policy.as_path.MatchAsPath;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.IntComparator;
import org.batfish.datamodel.routing_policy.expr.IntComparison;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;

/**
 * An route-policy boolean expression that evaluates to true iff the length of the route's as-path
 * is matched by a given comparator and magnitude.
 */
@ParametersAreNonnullByDefault
public final class RoutePolicyBooleanAsPathLength extends RoutePolicyBoolean {

  public RoutePolicyBooleanAsPathLength(IntComparator comparator, int length, boolean all) {
    _comparator = comparator;
    _length = length;
    _all = all;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RoutePolicyBooleanAsPathLength)) {
      return false;
    }
    RoutePolicyBooleanAsPathLength that = (RoutePolicyBooleanAsPathLength) o;
    return _length == that._length && _all == that._all && _comparator == that._comparator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_comparator.ordinal(), _length, _all);
  }

  @Override
  public BooleanExpr toBooleanExpr(CiscoXrConfiguration cc, Configuration c, Warnings w) {
    // TODO: what does _all do?
    return MatchAsPath.of(
        InputAsPath.instance(),
        HasAsPathLength.of(new IntComparison(_comparator, new LiteralInt(_length))));
  }

  private final @Nonnull IntComparator _comparator;
  private final int _length;
  private final boolean _all;
}
