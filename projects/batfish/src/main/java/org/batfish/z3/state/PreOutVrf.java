package org.batfish.z3.state;

import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.visitors.StateExprVisitor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public final class PreOutVrf implements StateExpr {

  private final String _hostname;

  private final String _vrf;

  public PreOutVrf(String hostname, String vrf) {
    _hostname = hostname;
    _vrf = vrf;
  }

  @Override
  public <R> R accept(StateExprVisitor<R> visitor) {
    return visitor.visitPreOutVrf(this);
  }

  public String getHostname() {
    return _hostname;
  }

  public String getVrf() {
    return _vrf;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PreOutVrf)) {
      return false;
    }
    PreOutVrf preOutVrf = (PreOutVrf) o;
    return _hostname.equals(preOutVrf._hostname) && _vrf.equals(preOutVrf._vrf);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _vrf);
  }
}
