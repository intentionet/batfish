package org.batfish.datamodel.routing_policy.statement;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.expr.IntExpr;

public class SetAdministrativeCost extends Statement {

  /** */
  private static final long serialVersionUID = 1L;

  private IntExpr _admin;

  @JsonCreator
  private SetAdministrativeCost() {}

  public SetAdministrativeCost(IntExpr admin) {
    _admin = admin;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SetAdministrativeCost other = (SetAdministrativeCost) obj;
    if (_admin == null) {
      if (other._admin != null) {
        return false;
      }
    } else if (!_admin.equals(other._admin)) {
      return false;
    }
    return true;
  }

  @Override
  public Result execute(Environment environment) {
    Result result = new Result();
    int admin = _admin.evaluate(environment);
    environment.getOutputRoute().setAdmin(admin);
    if (environment.getWriteToIntermediateBgpAttributes()) {
      environment.getIntermediateBgpAttributes().setAdmin(admin);
    }
    return result;
  }

  public IntExpr getAdmin() {
    return _admin;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_admin == null) ? 0 : _admin.hashCode());
    return result;
  }

  public void setAdmin(IntExpr admin) {
    _admin = admin;
  }
}
