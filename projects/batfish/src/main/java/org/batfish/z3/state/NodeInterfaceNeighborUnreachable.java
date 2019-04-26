package org.batfish.z3.state;

import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public final class NodeInterfaceNeighborUnreachable implements StateExpr {

  private final String _hostname;

  private final String _iface;

  public NodeInterfaceNeighborUnreachable(String hostname, String iface) {
    _hostname = hostname;
    _iface = iface;
  }

  @Override
  public <R> R accept(StateExprVisitor<R> visitor) {
    return visitor.visitNodeInterfaceNeighborUnreachable(this);
  }

  public String getHostname() {
    return _hostname;
  }

  public String getIface() {
    return _iface;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeInterfaceNeighborUnreachable)) {
      return false;
    }

    NodeInterfaceNeighborUnreachable that = (NodeInterfaceNeighborUnreachable) o;
    return Objects.equals(_hostname, that._hostname) && Objects.equals(_iface, that._iface);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _iface);
  }
}
