package org.batfish.datamodel.acl;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public class NotMatchExpr implements AclLineMatchExpr {
  private final AclLineMatchExpr _operand;

  public NotMatchExpr(AclLineMatchExpr operand) {
    _operand = operand;
  }

  @Override
  public <R> R accept(GenericAclLineMatchExprVisitor<R> visitor) {
    return visitor.visitNotMatchExpr(this);
  }

  @Override
  public boolean exprEquals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(getClass() == o.getClass())) {
      return false;
    }
    return Objects.equals(_operand, ((NotMatchExpr) o)._operand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_operand);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).toString();
  }

  public AclLineMatchExpr getOperand() {
    return _operand;
  }
}
