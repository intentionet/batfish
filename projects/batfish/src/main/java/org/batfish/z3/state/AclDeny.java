package org.batfish.z3.state;

import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;
import org.batfish.z3.state.visitors.StateVisitor;

public class AclDeny extends BasicStateExpr {

  public static class State extends BasicStateExpr.State {

    public static final State INSTANCE = new State();

    private State() {}

    @Override
    public void accept(StateVisitor visitor) {
      visitor.visitAclDeny(this);
    }
  }

  private final String _acl;

  private final String _hostname;

  public AclDeny(String hostname, String acl) {
    _hostname = hostname;
    _acl = acl;
  }

  @Override
  public void accept(StateExprVisitor visitor) {
    visitor.visitAclDeny(this);
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
