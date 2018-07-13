package org.batfish.datamodel.acl;

import java.util.Arrays;
import java.util.List;
import org.batfish.datamodel.HeaderSpace;

public final class AclLineMatchExprs {
  private AclLineMatchExprs() {}

  public static final FalseExpr FALSE = FalseExpr.INSTANCE;

  public static final OriginatingFromDevice ORIGINATING_FROM_DEVICE =
      OriginatingFromDevice.INSTANCE;

  public static final TrueExpr TRUE = TrueExpr.INSTANCE;

  public static AndMatchExpr and(AclLineMatchExpr... exprs) {
    return and(Arrays.asList(exprs));
  }

  public static AndMatchExpr and(List<AclLineMatchExpr> exprs) {
    return new AndMatchExpr(exprs);
  }

  public static MatchHeaderSpace match(HeaderSpace headerSpace) {
    return new MatchHeaderSpace(headerSpace);
  }

  public static NotMatchExpr not(AclLineMatchExpr expr) {
    return new NotMatchExpr(expr);
  }

  public static OrMatchExpr or(AclLineMatchExpr... exprs) {
    return or(Arrays.asList(exprs));
  }

  public static OrMatchExpr or(List<AclLineMatchExpr> exprs) {
    return new OrMatchExpr(exprs);
  }
}
