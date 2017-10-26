package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;

public class MatchPrefix6Set extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private Prefix6Expr _prefix;

  private Prefix6SetExpr _prefixSet;

  @JsonCreator
  private MatchPrefix6Set() {}

  public MatchPrefix6Set(Prefix6Expr prefix, Prefix6SetExpr prefixSet) {
    _prefix = prefix;
    _prefixSet = prefixSet;
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
    MatchPrefix6Set other = (MatchPrefix6Set) obj;
    if (_prefix == null) {
      if (other._prefix != null) {
        return false;
      }
    } else if (!_prefix.equals(other._prefix)) {
      return false;
    }
    if (_prefixSet == null) {
      if (other._prefixSet != null) {
        return false;
      }
    } else if (!_prefixSet.equals(other._prefixSet)) {
      return false;
    }
    return true;
  }

  @Override
  public Result evaluate(Environment environment) {
    Prefix6 prefix = _prefix.evaluate(environment);
    boolean match = prefix != null && _prefixSet.matches(prefix, environment);
    Result result = new Result();
    result.setBooleanValue(match);
    return result;
  }

  public Prefix6Expr getPrefix() {
    return _prefix;
  }

  public Prefix6SetExpr getPrefixSet() {
    return _prefixSet;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_prefix == null) ? 0 : _prefix.hashCode());
    result = prime * result + ((_prefixSet == null) ? 0 : _prefixSet.hashCode());
    return result;
  }

  public void setPrefix(Prefix6Expr prefix) {
    _prefix = prefix;
  }

  public void setPrefixSet(Prefix6SetExpr prefixSet) {
    _prefixSet = prefixSet;
  }
}
