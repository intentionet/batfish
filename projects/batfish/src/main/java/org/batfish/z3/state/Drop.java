package org.batfish.z3.state;

import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;
import org.batfish.z3.state.visitors.StateVisitor;

public class Drop extends BasicStateExpr {

  public static class State extends BasicStateExpr.State {

    public static final State INSTANCE = new State();

    private State() {}

    @Override
    public void accept(StateVisitor visitor) {
      visitor.visitDrop(this);
    }
  }

  public static final Drop INSTANCE = new Drop();

  private Drop() {}

  @Override
  public void accept(StateExprVisitor visitor) {
    visitor.visitDrop(this);
  }

  @Override
  public State getState() {
    return State.INSTANCE;
  }
}
