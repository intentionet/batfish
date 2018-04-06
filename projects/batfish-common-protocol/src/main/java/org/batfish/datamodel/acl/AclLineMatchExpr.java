package org.batfish.datamodel.acl;

public interface AclLineMatchExpr {
  boolean equals(Object o);

  boolean exprEquals(Object o);

  int hashCode();

  String toString();

  <R> R accept(GenericAclLineMatchExprVisitor<R> visitor);
}
