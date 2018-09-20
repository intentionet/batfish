package org.batfish.datamodel.acl;

import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBDD;
import org.batfish.common.bdd.MemoizedIpAccessListToBDD;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.acl.normalize.AclToAclLineMatchExpr;

/**
 * Generate an explanation of the headerspace permitted by an {@link IpAccessList}. The explanation
 * is a single {@link AclLineMatchExpr} in a simplified normal form. First we normalize to something
 * analogous to Disjunctive Normal Form, then simplify further if possible.
 */
public final class AclExplainer {
  private AclExplainer() {}

  /**
   * Explain the flow space permitted by one {@link IpAccessList} ({@param permitAcl}) but denied by
   * another ({@param denyAcl}). The {@param invariantExp} allows scoping the explanation to a space
   * of interest (use {@link TrueExpr} to explain the entire difference).
   */
  public static AclLineMatchExpr explainDifferential(
      BDDPacket bddPacket,
      BDDSourceManager mgr,
      AclLineMatchExpr invariantExpr,
      IpAccessList denyAcl,
      Map<String, IpAccessList> denyNamedAcls,
      Map<String, IpSpace> denyNamedIpSpaces,
      IpAccessList permitAcl,
      Map<String, IpAccessList> permitNamedAcls,
      Map<String, IpSpace> permitNamedIpSpaces) {
    // Construct an ACL that permits the difference of the two ACLs.
    DifferentialIpAccessList differentialIpAccessList =
        DifferentialIpAccessList.create(
            denyAcl,
            denyNamedAcls,
            denyNamedIpSpaces,
            permitAcl,
            permitNamedAcls,
            permitNamedIpSpaces);

    IpAccessListToBDD ipAccessListToBDD =
        MemoizedIpAccessListToBDD.create(
            bddPacket,
            mgr,
            differentialIpAccessList.getNamedAcls(),
            differentialIpAccessList.getNamedIpSpaces());

    return explain(
        ipAccessListToBDD,
        scopedAcl(invariantExpr, differentialIpAccessList.getAcl()),
        differentialIpAccessList.getNamedAcls());
  }

  /**
   * Explain the flow space permitted by an {@link IpAccessList}. The {@param invariantExp} allows
   * scoping the explanation to a space of interest (use {@link TrueExpr} to explain the entire
   * space).
   */
  public static AclLineMatchExpr explain(
      BDDPacket bddPacket,
      BDDSourceManager mgr,
      AclLineMatchExpr invariantExpr,
      IpAccessList acl,
      Map<String, IpAccessList> namedAcls,
      Map<String, IpSpace> namedIpSpaces) {
    IpAccessListToBDD ipAccessListToBDD =
        MemoizedIpAccessListToBDD.create(bddPacket, mgr, namedAcls, namedIpSpaces);

    IpAccessList aclWithInvariant = scopedAcl(invariantExpr, acl);

    return explain(ipAccessListToBDD, aclWithInvariant, namedAcls);
  }

  private static AclLineMatchExpr explain(
      IpAccessListToBDD ipAccessListToBDD, IpAccessList acl, Map<String, IpAccessList> namedAcls) {
    // Convert acl to a single expression.
    AclLineMatchExpr aclExpr =
        AclToAclLineMatchExpr.toAclLineMatchExpr(ipAccessListToBDD, acl, namedAcls);

    // Reduce that expression to normal form.
    AclLineMatchExpr aclExprNf = AclLineMatchExprNormalizer.normalize(ipAccessListToBDD, aclExpr);

    // Simplify the normal form
    return AclExplanation.explainNormalForm(aclExprNf);
  }

  /**
   * Scope the headerspace permitted by an {@link IpAccessList} to those flows that also match
   * {@param invariantExpr}.
   */
  private static IpAccessList scopedAcl(AclLineMatchExpr invariantExpr, IpAccessList acl) {
    return IpAccessList.builder()
        .setName(acl.getName())
        .setLines(
            ImmutableList.<IpAccessListLine>builder()
                .add(rejecting(not(invariantExpr)))
                .addAll(acl.getLines())
                .build())
        .build();
  }
}
