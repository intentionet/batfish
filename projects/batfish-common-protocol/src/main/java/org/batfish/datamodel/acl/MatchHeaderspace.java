package org.batfish.datamodel.acl;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.batfish.datamodel.HeaderSpace;

public class MatchHeaderspace implements AclLineMatchExpr {
  private final HeaderSpace _headerspace;

  public MatchHeaderspace(HeaderSpace headerspace) {
    _headerspace = headerspace;
  }

  @Override
  public <R> R accept(GenericAclLineMatchExprVisitor<R> visitor) {
    return visitor.visitMatchHeaderspace(this);
  }

  @Override
  public boolean exprEquals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(getClass() == o.getClass())) {
      return false;
    }
    return Objects.equals(_headerspace, ((MatchHeaderspace) o)._headerspace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_headerspace);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("headerSpace", _headerspace).toString();
  }

  public HeaderSpace getHeaderspace() {
    return _headerspace;
  }
}
