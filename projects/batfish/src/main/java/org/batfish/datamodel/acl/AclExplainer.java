package org.batfish.datamodel.acl;

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
            invariantExpr,
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
        differentialIpAccessList.getAcl(),
        differentialIpAccessList.getNamedAcls());
  }

  public static AclLineMatchExpr explain(
      BDDPacket bddPacket,
      BDDSourceManager mgr,
      AclLineMatchExpr invariantExpr,
      IpAccessList acl,
      Map<String, IpAccessList> namedAcls,
      Map<String, IpSpace> namedIpSpaces) {
    IpAccessListToBDD ipAccessListToBDD =
        MemoizedIpAccessListToBDD.create(bddPacket, mgr, namedAcls, namedIpSpaces);

    IpAccessList aclWithInvariant =
        IpAccessList.builder()
            .setName(acl.getName())
            .setLines(
                ImmutableList.<IpAccessListLine>builder()
                    .add(IpAccessListLine.rejecting(not(invariantExpr)))
                    .addAll(acl.getLines())
                    .build())
            .build();

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
}
