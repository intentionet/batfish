package org.batfish.datamodel.acl;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public class TrueExpr implements AclLineMatchExpr {

  private TrueExpr() {}

  public static final TrueExpr trueExpr = new TrueExpr();

  @Override
  public <R> R accept(GenericAclLineMatchExprVisitor<R> visitor) {
    return visitor.visitTrueExpr(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash((Boolean) true);
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
