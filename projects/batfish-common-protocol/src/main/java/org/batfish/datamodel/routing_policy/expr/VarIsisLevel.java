package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.routing_policy.Environment;

public class VarIsisLevel extends IsisLevelExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private String _var;

  @JsonCreator
  private VarIsisLevel() {}

  public VarIsisLevel(String var) {
    _var = var;
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
    VarIsisLevel other = (VarIsisLevel) obj;
    if (_var == null) {
      if (other._var != null) {
        return false;
      }
    } else if (!_var.equals(other._var)) {
      return false;
    }
    return true;
  }

  @Override
  public IsisLevel evaluate(Environment env) {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  public String getVar() {
    return _var;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_var == null) ? 0 : _var.hashCode());
    return result;
  }

  public void setVar(String var) {
    _var = var;
  }
}
