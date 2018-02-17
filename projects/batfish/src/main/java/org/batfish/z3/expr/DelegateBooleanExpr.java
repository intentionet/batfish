package org.batfish.z3.expr;

import com.google.common.base.Supplier;
import com.microsoft.z3.BoolExpr;
import org.batfish.z3.NodContext;
import org.batfish.z3.SynthesizerInput;
import org.batfish.z3.expr.visitors.BoolExprTransformer;
import org.batfish.z3.expr.visitors.BooleanExprVisitor;
import org.batfish.z3.expr.visitors.ExprVisitor;

public abstract class DelegateBooleanExpr extends BooleanExpr {

  @Override
  public void accept(BooleanExprVisitor visitor) {
    if (visitor instanceof BoolExprTransformer) {
      ((BoolExprTransformer) visitor).visitDelegateBooleanExpr(this);
    } else {
      throw new UnsupportedOperationException(
          String.format(
              "Unsupported delegate %s type: %s",
              BooleanExprVisitor.class, visitor.getClass().getCanonicalName()));
    }
  }

  @Override
  public void accept(ExprVisitor visitor) {
    throw new UnsupportedOperationException(
        String.format(
            "Unsupported delegate %s type: %s",
            ExprVisitor.class, visitor.getClass().getCanonicalName()));
  }

  public abstract BoolExpr acceptBoolExprTransformer(
      Supplier<com.microsoft.z3.Expr[]> headerFieldArgs,
      SynthesizerInput input,
      NodContext nodContext);
}
