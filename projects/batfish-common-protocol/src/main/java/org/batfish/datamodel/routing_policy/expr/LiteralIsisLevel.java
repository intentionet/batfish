package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.routing_policy.Environment;

public class LiteralIsisLevel extends IsisLevelExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private IsisLevel _level;

  @JsonCreator
  private LiteralIsisLevel() {}

  public LiteralIsisLevel(IsisLevel level) {
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
    LiteralIsisLevel other = (LiteralIsisLevel) obj;
    if (_level != other._level) {
      return false;
    }
    return true;
  }

  @Override
  public IsisLevel evaluate(Environment env) {
    return _level;
  }

  public IsisLevel getLevel() {
    return _level;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_level == null) ? 0 : _level.ordinal());
    return result;
  }

  public void setLevel(IsisLevel level) {
    _level = level;
  }
}
