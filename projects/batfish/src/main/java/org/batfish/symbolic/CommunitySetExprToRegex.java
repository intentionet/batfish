package org.batfish.symbolic;

import javax.annotation.Nonnull;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.routing_policy.expr.CommunityHalvesExpr;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.EmptyCommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.expr.NamedCommunitySet;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;

public class CommunitySetExprToRegex implements CommunitySetExprVisitor<RegexCommunitySet> {

  private static final CommunitySetExprToRegex INSTANCE = new CommunitySetExprToRegex();

  public static @Nonnull RegexCommunitySet convert(@Nonnull CommunitySetExpr communitySetExpr) {
    return communitySetExpr.accept(INSTANCE);
  }

  @Override
  public RegexCommunitySet visitCommunityHalvesExpr(CommunityHalvesExpr communityHalvesExpr) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public RegexCommunitySet visitCommunityList(CommunityList communityList) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public RegexCommunitySet visitEmptyCommunitySetExpr(EmptyCommunitySetExpr emptyCommunitySetExpr) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public RegexCommunitySet visitLiteralCommunity(LiteralCommunity literalCommunity) {
    return new RegexCommunitySet(
        String.format("^%s$", literalCommunity.getCommunity().matchString()));
  }

  @Override
  public RegexCommunitySet visitLiteralCommunityConjunction(
      LiteralCommunityConjunction literalCommunityConjunction) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public RegexCommunitySet visitLiteralCommunitySet(LiteralCommunitySet literalCommunitySet) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public RegexCommunitySet visitNamedCommunitySet(NamedCommunitySet namedCommunitySet) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }

  @Override
  public RegexCommunitySet visitRegexCommunitySet(RegexCommunitySet regexCommunitySet) {
    return regexCommunitySet;
  }
}
