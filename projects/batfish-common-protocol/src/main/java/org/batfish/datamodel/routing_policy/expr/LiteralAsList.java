package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.batfish.datamodel.routing_policy.Environment;

public class LiteralAsList extends AsPathListExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private List<AsExpr> _list;

  @JsonCreator
  private LiteralAsList() {}

  public LiteralAsList(List<AsExpr> list) {
    _list = list;
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
    LiteralAsList other = (LiteralAsList) obj;
    if (_list == null) {
      if (other._list != null) {
        return false;
      }
    } else if (!_list.equals(other._list)) {
      return false;
    }
    return true;
  }

  @Override
  public List<Long> evaluate(Environment environment) {
    return _list
        .stream()
        .map(expr -> expr.evaluate(environment))
        .collect(ImmutableList.toImmutableList());
  }

  public List<AsExpr> getList() {
    return _list;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_list == null) ? 0 : _list.hashCode());
    return result;
  }

  public void setList(List<AsExpr> list) {
    _list = ImmutableList.copyOf(list);
  }
}
