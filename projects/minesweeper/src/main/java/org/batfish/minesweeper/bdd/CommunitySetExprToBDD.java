package org.batfish.minesweeper.bdd;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.routing_policy.communities.CommunityExprsSet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetDifference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExprReference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExprVisitor;
import org.batfish.datamodel.routing_policy.communities.CommunitySetReference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetUnion;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.minesweeper.CommunityVar;
import org.batfish.minesweeper.bdd.CommunitySetMatchExprToBDD.Arg;
import org.batfish.minesweeper.communities.CommunitySetExprVarCollector;

/**
 * Create a BDD that represents a {@link CommunitySetExpr}. The BDD is a predicate on community
 * atomic predicates. A concrete community C satisfies the BDD if ap(C) implies the BDD, where ap(C)
 * denotes the unique atomic predicate that the community C satisfies.
 */
@ParametersAreNonnullByDefault
public class CommunitySetExprToBDD implements CommunitySetExprVisitor<BDD, Arg> {
  @Override
  public BDD visitCommunityExprsSet(CommunityExprsSet communityExprsSet, Arg arg) {
    Set<CommunityVar> commVars =
        communityExprsSet.accept(
            new CommunitySetExprVarCollector(), arg.getTransferBDD().getConfiguration());
    return CommunitySetMatchExprToBDD.communityVarsToBDD(commVars, arg);
  }

  @Override
  public BDD visitCommunitySetDifference(CommunitySetDifference communitySetDifference, Arg arg) {
    BDD positive = communitySetDifference.getInitial().accept(this, arg);
    BDD negative =
        communitySetDifference.getRemovalCriterion().accept(new CommunityMatchExprToBDD(), arg);
    return positive.diffWith(negative);
  }

  @Override
  public BDD visitCommunitySetExprReference(
      CommunitySetExprReference communitySetExprReference, Arg arg) {
    String name = communitySetExprReference.getName();
    CommunitySetExpr expr =
        arg.getTransferBDD().getConfiguration().getCommunitySetExprs().get(name);
    if (expr == null) {
      throw new BatfishException("Cannot find community set expression: " + name);
    }
    return expr.accept(this, arg);
  }

  @Override
  public BDD visitCommunitySetReference(CommunitySetReference communitySetReference, Arg arg) {
    Set<CommunityVar> commVars =
        communitySetReference.accept(
            new CommunitySetExprVarCollector(), arg.getTransferBDD().getConfiguration());
    return CommunitySetMatchExprToBDD.communityVarsToBDD(commVars, arg);
  }

  @Override
  public BDD visitCommunitySetUnion(CommunitySetUnion communitySetUnion, Arg arg) {
    return BDDRoute.factory.orAll(
        communitySetUnion.getExprs().stream()
            .map(expr -> expr.accept(this, arg))
            .collect(Collectors.toList()));
  }

  @Override
  public BDD visitInputCommunities(InputCommunities inputCommunities, Arg arg) {
    throw new UnsupportedOperationException(
        "Currently not supporting matching on input communities");
  }

  @Override
  public BDD visitLiteralCommunitySet(LiteralCommunitySet literalCommunitySet, Arg arg) {
    Set<CommunityVar> commVars =
        literalCommunitySet.accept(
            new CommunitySetExprVarCollector(), arg.getTransferBDD().getConfiguration());
    return CommunitySetMatchExprToBDD.communityVarsToBDD(commVars, arg);
  }
}
