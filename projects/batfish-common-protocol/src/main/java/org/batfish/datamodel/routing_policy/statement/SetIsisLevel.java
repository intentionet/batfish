package org.batfish.datamodel.routing_policy.statement;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.expr.IsisLevelExpr;

public class SetIsisLevel extends Statement {

  /** */
  private static final long serialVersionUID = 1L;

  private IsisLevelExpr _level;

  @JsonCreator
  private SetIsisLevel() {}

  public SetIsisLevel(IsisLevelExpr level) {
    _level = level;
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
    SetIsisLevel other = (SetIsisLevel) obj;
    if (_level == null) {
      if (other._level != null) {
        return false;
      }
    } else if (!_level.equals(other._level)) {
      return false;
    }
    return true;
  }

  @Override
  public Result execute(Environment environment) {
    Result result = new Result();
    IsisLevel level = _level.evaluate(environment);
    IsisRoute.Builder isisRouteBuilder = (IsisRoute.Builder) environment.getOutputRoute();
    isisRouteBuilder.setLevel(level);
    return result;
  }

  public IsisLevelExpr getLevel() {
    return _level;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_level == null) ? 0 : _level.hashCode());
    return result;
  }

  public void setLevel(IsisLevelExpr level) {
    _level = level;
  }
}
