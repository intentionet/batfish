package org.batfish.z3.expr;

import org.batfish.z3.expr.visitors.BooleanExprVisitor;
import org.batfish.z3.expr.visitors.ExprVisitor;
import org.batfish.z3.state.visitors.GeneralStateVisitor;

public abstract class BasicStateExpr extends StateExpr {

  public abstract static class State extends StateExpr.State {
    @Override
    public void accept(GeneralStateVisitor visitor) {
      visitor.visitBasicState(this);
    }
  }

  @Override
  public void accept(BooleanExprVisitor visitor) {
    visitor.visitBasicStateExpr(this);
  }

  @Override
  public abstract State getState();

  @Override
  public void accept(ExprVisitor visitor) {
    visitor.visitBasicStateExpr(this);
  }
}
