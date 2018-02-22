package org.batfish.z3.expr;

import org.batfish.z3.expr.visitors.ExprVisitor;
import org.batfish.z3.expr.visitors.GenericBooleanExprVisitor;

public class TrueExpr extends BooleanExpr {

  public static final TrueExpr INSTANCE = new TrueExpr();

  private TrueExpr() {}

  @Override
  public void accept(ExprVisitor visitor) {
    visitor.visitTrueExpr(this);
  }

  @Override
  public <R> R accept(GenericBooleanExprVisitor<R> visitor) {
    return visitor.visitTrueExpr(this);
  }

  @Override
  public boolean exprEquals(Expr e) {
    return true;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }
}
