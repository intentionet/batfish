package org.batfish.datamodel.acl;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public class FalseExpr implements AclLineMatchExpr {

  private FalseExpr() {}

  public static final FalseExpr falseExpr = new FalseExpr();

  @Override
  public <R> R accept(GenericAclLineMatchExprVisitor<R> visitor) {
    return visitor.visitFalseExpr(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(((Boolean) false));
  }

  @Override
  public boolean exprEquals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(getClass() == o.getClass())) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).toString();
  }
}
