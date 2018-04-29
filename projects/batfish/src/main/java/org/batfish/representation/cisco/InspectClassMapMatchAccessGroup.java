package org.batfish.representation.cisco;

import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.PermittedByAcl;

public class InspectClassMapMatchAccessGroup implements InspectClassMapMatch {

  /** */
  private static final long serialVersionUID = 1L;

  private String _name;

  public InspectClassMapMatchAccessGroup(String name) {
    _name = name;
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    _name = name;
  }

  @Override
  public AclLineMatchExpr toAclLineMatchExpr(
      CiscoConfiguration cc, Configuration c, MatchSemantics matchSemantics, Warnings w) {
    /* For now assume no match for non-existent ACLs */
    if (!c.getIpAccessLists().containsKey(_name)) {
      return FalseExpr.INSTANCE;
    }
    return new PermittedByAcl(_name);
  }
}
