package org.batfish.z3.state;

import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;
import org.batfish.z3.state.visitors.StateVisitor;

public class Accept extends BasicStateExpr {

  public static class State extends BasicStateExpr.State {

    public static final State INSTANCE = new State();

    private State() {}

    @Override
    public void accept(StateVisitor visitor) {
      visitor.visitAccept(this);
    }
  }

  public static final Accept INSTANCE = new Accept();

  @Override
  public void accept(StateExprVisitor visitor) {
    visitor.visitAccept(this);
  }

  @Override
  public State getState() {
    return State.INSTANCE;
  }
}
