package org.batfish.representation.cisco;

import javax.annotation.Nonnull;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.expr.CommunityHalvesExpr;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;

public class CommunitySetElemHalves implements CommunitySetElem {

  private static final long serialVersionUID = 1L;

  private final CommunitySetElemHalfExpr _prefix;

  private final CommunitySetElemHalfExpr _suffix;

  public CommunitySetElemHalves(
      @Nonnull CommunitySetElemHalfExpr prefix, @Nonnull CommunitySetElemHalfExpr suffix) {
    _prefix = prefix;
    _suffix = suffix;
  }

  public CommunitySetElemHalves(long value) {
    int prefixInt = (int) ((value & 0xFFFF0000L) >> 16);
    _prefix = new LiteralCommunitySetElemHalf(prefixInt);
    int suffixInt = (int) (value & 0xFFFFL);
    _suffix = new LiteralCommunitySetElemHalf(suffixInt);
  }

  public CommunitySetElemHalfExpr getPrefix() {
    return _prefix;
  }

  public CommunitySetElemHalfExpr getSuffix() {
    return _suffix;
  }

  @Nonnull
  @Override
  public CommunitySetExpr toCommunitySetExpr() {
    if (_prefix instanceof LiteralCommunitySetElemHalf
        && _suffix instanceof LiteralCommunitySetElemHalf) {
      LiteralCommunitySetElemHalf prefix = (LiteralCommunitySetElemHalf) _prefix;
      LiteralCommunitySetElemHalf suffix = (LiteralCommunitySetElemHalf) _suffix;
      int prefixInt = prefix.getValue();
      int suffixInt = suffix.getValue();
      return new LiteralCommunity(StandardCommunity.of(prefixInt, suffixInt));
    } else {
      return new CommunityHalvesExpr(_prefix.toCommunityHalfExpr(), _suffix.toCommunityHalfExpr());
    }
  }
}
