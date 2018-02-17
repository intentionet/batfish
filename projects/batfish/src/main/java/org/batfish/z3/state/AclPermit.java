package org.batfish.z3.state;

import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;
import org.batfish.z3.state.visitors.StateVisitor;

public class AclPermit extends BasicStateExpr {

  public static class State extends BasicStateExpr.State {

    public static final State INSTANCE = new State();

    private State() {}

    @Override
    public void accept(StateVisitor visitor) {
      visitor.visitAclPermit(this);
    }
  }

  private final String _acl;

  private final String _hostname;

  public AclPermit(String hostname, String acl) {
    _hostname = hostname;
    _acl = acl;
  }

  @Override
  public void accept(StateExprVisitor visitor) {
    visitor.visitAclPermit(this);
  }

  public String getAcl() {
    return _acl;
  }

  public String getHostname() {
    return _hostname;
  }

  @Override
  public State getState() {
    return State.INSTANCE;
  }
}
