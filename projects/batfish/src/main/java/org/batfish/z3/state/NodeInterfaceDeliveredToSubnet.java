package org.batfish.z3.state;

import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;

@ParametersAreNonnullByDefault
public final class NodeInterfaceDeliveredToSubnet implements StateExpr {

  private final String _hostname;

  private final String _iface;

  public NodeInterfaceDeliveredToSubnet(String hostname, String iface) {
    _hostname = hostname;
    _iface = iface;
  }

  @Override
  public <R> R accept(StateExprVisitor<R> visitor) {
    return visitor.visitNodeInterfaceDeliveredToSubnet(this);
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
    if (!(o instanceof NodeInterfaceDeliveredToSubnet)) {
      return false;
    }
    NodeInterfaceDeliveredToSubnet that = (NodeInterfaceDeliveredToSubnet) o;
    return _hostname.equals(that._hostname) && _iface.equals(that._iface);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _iface);
  }
}
