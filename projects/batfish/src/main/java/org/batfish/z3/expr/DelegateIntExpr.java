package org.batfish.z3.expr;

import org.batfish.z3.NodContext;
import org.batfish.z3.expr.visitors.BitVecExprTransformer;
import org.batfish.z3.expr.visitors.ExprVisitor;
import org.batfish.z3.expr.visitors.IntExprVisitor;

public abstract class DelegateIntExpr extends IntExpr {

  @Override
  public void accept(ExprVisitor visitor) {
    throw new UnsupportedOperationException(
        String.format(
            "Unsupported delegate %s type: %s",
            ExprVisitor.class, visitor.getClass().getCanonicalName()));
  }

  @Override
  public void accept(IntExprVisitor visitor) {
    if (visitor instanceof BitVecExprTransformer) {
      ((BitVecExprTransformer) visitor).visitDelegateIntExpr(this);
    } else {
      throw new UnsupportedOperationException(
          String.format(
              "Unsupported delegate %s type: %s",
              IntExprVisitor.class, visitor.getClass().getCanonicalName()));
    }
  }

  public abstract com.microsoft.z3.BitVecExpr acceptBitVecExprTransformer(NodContext nodContext);
}
