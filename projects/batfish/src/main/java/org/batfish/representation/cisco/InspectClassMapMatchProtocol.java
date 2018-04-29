package org.batfish.representation.cisco;

import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.TrueExpr;

public class InspectClassMapMatchProtocol implements InspectClassMapMatch {

  /** */
  private static final long serialVersionUID = 1L;

  private final InspectClassMapProtocol _protocol;

  public InspectClassMapMatchProtocol(InspectClassMapProtocol protocol) {
    _protocol = protocol;
  }

  public InspectClassMapProtocol getProtocol() {
    return _protocol;
  }

  @Override
  public AclLineMatchExpr toAclLineMatchExpr(
      CiscoConfiguration cc, Configuration c, MatchSemantics matchSemantics, Warnings w) {
    /* TODO: Proper implementation: https://github.com/batfish/batfish/issues/1260 */
    switch (matchSemantics) {
      case MATCH_ALL:
        return TrueExpr.INSTANCE;
      case MATCH_ANY:
        return FalseExpr.INSTANCE;
      default:
        throw new BatfishException(
            String.format(
                "Unsupported %s: %s", MatchSemantics.class.getSimpleName(), matchSemantics));
    }
  }
}
