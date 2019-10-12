package org.batfish.datamodel.acl.normalize;

import static org.batfish.datamodel.IpAccessListLine.accepting;
import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.acl.AclLineMatchExprs.FALSE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDstIp;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcIp;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.acl.normalize.AclToAclLineMatchExpr.aclLines;
import static org.batfish.datamodel.acl.normalize.AclToAclLineMatchExpr.toAclLineMatchExpr;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.junit.Test;

public class AclToAclLineMatchExprTest {
  // 5 orthogonal match expressions
  private static final AclLineMatchExpr EXPR_A = matchDstIp("1.1.1.1");
  private static final AclLineMatchExpr EXPR_B = matchSrcIp("2.2.2.2");
  private static final AclLineMatchExpr EXPR_C =
      AclLineMatchExprs.match(
          HeaderSpace.builder().setDstPorts(ImmutableList.of(SubRange.singleton(1))).build());
  private static final AclLineMatchExpr EXPR_D =
      AclLineMatchExprs.match(
          HeaderSpace.builder().setSrcPorts(ImmutableList.of(SubRange.singleton(2))).build());
  private static final AclLineMatchExpr EXPR_E =
      AclLineMatchExprs.match(
          HeaderSpace.builder().setIpProtocols(ImmutableList.of(IpProtocol.TCP)).build());

  private static final IpAccessListLine ACCEPT_A = accepting(EXPR_A);
  private static final IpAccessListLine ACCEPT_B = accepting(EXPR_B);
  private static final IpAccessListLine REJECT_B = rejecting(EXPR_B);
  private static final IpAccessListLine ACCEPT_C = accepting(EXPR_C);
  private static final IpAccessListLine REJECT_C = rejecting(EXPR_C);
  private static final IpAccessListLine ACCEPT_D = accepting(EXPR_D);
  private static final IpAccessListLine REJECT_E = rejecting(EXPR_E);

  private static final List<IpAccessListLine> SIMPLE_ACL_LINES =
      ImmutableList.of(ACCEPT_A, REJECT_B, ACCEPT_C, REJECT_C, ACCEPT_D, REJECT_E);

  private static final IpAccessList SIMPLE_ACL =
      IpAccessList.builder().setName("acl").setLines(SIMPLE_ACL_LINES).build();

  private static final AclLineMatchExpr ACL_REFERENT_EXPR = or(EXPR_A, EXPR_B);

  private static final List<IpAccessListLine> ACL_REFERENT_LINES =
      ImmutableList.of(ACCEPT_A, ACCEPT_B);

  private static final IpAccessList ACL_REFERENT =
      IpAccessList.builder().setName("referent").setLines(ACL_REFERENT_LINES).build();

  private static final AclLineMatchExpr EXPR_REFERENCE = permittedByAcl(ACL_REFERENT.getName());

  private static final IpAccessList ACL_REFERRER =
      IpAccessList.builder()
          .setName("referrer")
          .setLines(
              ImmutableList.of(ACCEPT_C, REJECT_C, accepting(EXPR_REFERENCE), ACCEPT_D, REJECT_E))
          .build();

  private static IpAccessListToBdd aclLineMatchExprToBDD() {
    return aclLineMatchExprToBDD(ImmutableMap.of());
  }

  private static IpAccessListToBdd aclLineMatchExprToBDD(Map<String, IpAccessList> namedAcls) {
    BDDPacket pkt = new BDDPacket();
    return new IpAccessListToBddImpl(
        pkt, BDDSourceManager.empty(pkt), namedAcls, ImmutableMap.of());
  }

  private static AclToAclLineMatchExpr aclToAclLineMatchExpr() {
    ImmutableMap<String, IpAccessList> namedAcls =
        ImmutableMap.of(ACL_REFERENT.getName(), ACL_REFERENT);
    return new AclToAclLineMatchExpr(aclLineMatchExprToBDD(namedAcls), namedAcls);
  }

  @Test
  public void testSimple() {
    assertThat(
        toAclLineMatchExpr(aclLineMatchExprToBDD(), SIMPLE_ACL, ImmutableMap.of()),
        equalTo(or(EXPR_A, and(not(EXPR_B), EXPR_C), and(not(EXPR_B), not(EXPR_C), EXPR_D))));
  }

  @Test
  public void testSimpleLines() {
    assertThat(
        aclLines(aclLineMatchExprToBDD(), SIMPLE_ACL, ImmutableMap.of()),
        equalTo(SIMPLE_ACL_LINES));
  }

  @Test
  public void testReference() {
    Map<String, IpAccessList> namedAcls = ImmutableMap.of(ACL_REFERENT.getName(), ACL_REFERENT);
    AclLineMatchExpr expr =
        toAclLineMatchExpr(aclLineMatchExprToBDD(namedAcls), ACL_REFERRER, namedAcls);
    assertThat(
        expr,
        equalTo(
            or(
                // line 1: ACCEPT_C
                EXPR_C,
                // line 3: permitted by ACL_REFERENT
                and(not(EXPR_C), ACL_REFERENT_EXPR),
                // line 4: ACCEPT_D
                and(not(EXPR_C), EXPR_D))));
  }

  @Test
  public void testReferenceLines() {
    Map<String, IpAccessList> namedAcls = ImmutableMap.of(ACL_REFERENT.getName(), ACL_REFERENT);
    List<IpAccessListLine> lines =
        aclLines(aclLineMatchExprToBDD(namedAcls), ACL_REFERRER, namedAcls);
    assertThat(
        lines,
        equalTo(
            ImmutableList.of(
                ACCEPT_C,
                REJECT_C,
                // line 3: reference to ACL_REFERENT is inlined
                accepting(ACL_REFERENT_EXPR),
                ACCEPT_D,
                REJECT_E)));
  }

  @Test
  public void testAnd() {
    AclToAclLineMatchExpr toAclLineMatchExpr = aclToAclLineMatchExpr();
    AclLineMatchExpr and = and(EXPR_REFERENCE, EXPR_C);
    assertThat(toAclLineMatchExpr.visit(and), equalTo(and(ACL_REFERENT_EXPR, EXPR_C)));
  }

  @Test
  public void testOr() {
    AclToAclLineMatchExpr toAclLineMatchExpr = aclToAclLineMatchExpr();
    AclLineMatchExpr or = or(EXPR_REFERENCE, EXPR_C);
    assertThat(toAclLineMatchExpr.visit(or), equalTo(or(ACL_REFERENT_EXPR, EXPR_C)));
  }

  @Test
  public void testNot() {
    AclToAclLineMatchExpr toAclLineMatchExpr = aclToAclLineMatchExpr();
    AclLineMatchExpr not = not(EXPR_REFERENCE);
    assertThat(toAclLineMatchExpr.visit(not), equalTo(not(ACL_REFERENT_EXPR)));
  }

  @Test
  public void testIdentities() {
    AclToAclLineMatchExpr toAclLineMatchExpr = aclToAclLineMatchExpr();
    assertThat(toAclLineMatchExpr.visit(EXPR_A), equalTo(EXPR_A));
    assertThat(toAclLineMatchExpr.visit(FALSE), equalTo(FALSE));
    assertThat(
        toAclLineMatchExpr.visit(matchSrcInterface("foo")), equalTo(matchSrcInterface("foo")));
    assertThat(toAclLineMatchExpr.visit(ORIGINATING_FROM_DEVICE), equalTo(ORIGINATING_FROM_DEVICE));
    assertThat(toAclLineMatchExpr.visit(TRUE), equalTo(TRUE));
  }
}
