package org.batfish.representation.cisco_xr;

import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.IntComparator;
import org.batfish.datamodel.routing_policy.expr.LongExpr;
import org.batfish.datamodel.routing_policy.expr.MatchLocalPreference;

public class RoutePolicyBooleanLocalPreference extends RoutePolicyBoolean {

  private IntComparator _cmp;

  private final LongExpr _expr;

  public RoutePolicyBooleanLocalPreference(IntComparator cmp, LongExpr expr) {
    _cmp = cmp;
    _expr = expr;
  }

  public LongExpr getValue() {
    return _expr;
  }

  @Override
  public BooleanExpr toBooleanExpr(CiscoXrConfiguration cc, Configuration c, Warnings w) {
    return new MatchLocalPreference(_cmp, _expr);
  }
}
