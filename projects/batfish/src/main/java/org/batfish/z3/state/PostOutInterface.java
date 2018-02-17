package org.batfish.z3.state;

import org.batfish.z3.expr.TransformationStateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;
import org.batfish.z3.state.visitors.StateVisitor;

public class PostOutInterface extends TransformationStateExpr {

  public static class State extends TransformationStateExpr.State {

    public static final State INSTANCE = new State();

    private State() {}

    @Override
    public void accept(StateVisitor visitor) {
      visitor.visitPostOutInterface(this);
    }
  }

  private final String _hostname;

  private final String _iface;

  public PostOutInterface(String hostname, String iface) {
    _hostname = hostname;
    _iface = iface;
  }

  @Override
  public void accept(StateExprVisitor visitor) {
    visitor.visitPostOutInterface(this);
  }

  public String getHostname() {
    return _hostname;
  }

  public String getIface() {
    return _iface;
  }

  @Override
  public State getState() {
    return State.INSTANCE;
  }
}
