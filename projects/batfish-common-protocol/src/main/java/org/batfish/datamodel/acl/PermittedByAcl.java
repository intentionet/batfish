package org.batfish.datamodel.acl;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public class PermittedByAcl implements AclLineMatchExpr {
  private final String _aclName;

  public PermittedByAcl(String aclName) {
    _aclName = aclName;
  }

  @Override
  public <R> R accept(GenericAclLineMatchExprVisitor<R> visitor) {
    return visitor.visitPermittedByAcl(this);
  }

  @Override
  public boolean exprEquals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(getClass() == o.getClass())) {
      return false;
    }
    return _aclName.equals(((PermittedByAcl) o)._aclName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_aclName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("aclName", _aclName).toString();
  }

  public String getAclName() {
    return _aclName;
  }
}
