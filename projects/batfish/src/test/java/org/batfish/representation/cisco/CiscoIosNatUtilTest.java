package org.batfish.representation.cisco;

import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.representation.cisco.CiscoIosNatUtil.clauseToMatchExpr;
import static org.batfish.representation.cisco.CiscoIosNatUtil.lineToMatchExpr;
import static org.batfish.representation.cisco.CiscoIosNatUtil.toExpr;
import static org.batfish.representation.cisco.CiscoIosNatUtil.toMatchExpr;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Optional;
import java.util.Set;
import org.batfish.common.Warnings;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.junit.Test;

public class CiscoIosNatUtilTest {
  private static final String ACL = "acl";
  private static final String IFACE = "iface";
  private static final RouteMapMatchLine MATCH_ACL =
      new RouteMapMatchIpAccessListLine(ImmutableSet.of(ACL));
  private static final RouteMapMatchLine MATCH_IFACE =
      new RouteMapMatchInterfaceLine(ImmutableSet.of(IFACE));

  @Test
  public void testToMatchExpr_emptyRouteMap() {
    // Empty route-map should not convert
    Warnings w = new Warnings(true, true, true);
    assertFalse(toMatchExpr(new RouteMap("rm"), ImmutableSet.of(), IFACE, w).isPresent());
    assertThat(
        Iterables.getOnlyElement(w.getRedFlagWarnings()).getText(),
        containsString("empty route-map"));
  }

  @Test
  public void testToMatchExpr_inconvertibleClause() {
    RouteMapMatchLine acl2MatchLine =
        new RouteMapMatchIpAccessListLine(ImmutableSet.of("otherAcl"));
    RouteMapClause clause1 = rmc(10, MATCH_ACL);
    RouteMapClause clause2 = rmc(20, acl2MatchLine);
    RouteMapClause clause3 = rmc(30, MATCH_ACL);

    // Sanity check: make sure a route-map with only clause1 and clause3 does convert successfully
    assertTrue(
        toMatchExpr(
                rmWithClauses(clause1, clause3),
                ImmutableSet.of(ACL),
                IFACE,
                new Warnings(true, true, true))
            .isPresent());

    // Cannot convert route-map with an inconvertible clause, even if other clauses are convertible
    Warnings w = new Warnings(true, true, true);
    assertFalse(
        toMatchExpr(rmWithClauses(clause1, clause2, clause3), ImmutableSet.of(ACL), IFACE, w)
            .isPresent());
  }

  @Test
  public void testToMatchExpr_unmatchableClause() {
    // Unmatchable clause should be ignored and other clauses should be converted normally
    RouteMapClause clause1 = rmc(10, MATCH_ACL);
    RouteMapClause clause2 = rmc(20, MATCH_IFACE);
    RouteMapClause clause3 = rmc(30, MATCH_ACL);

    Warnings w = new Warnings(true, true, true);
    assertThat(
        toMatchExpr(
            rmWithClauses(clause1, clause2, clause3), ImmutableSet.of(ACL), "otherIface", w),
        equalTo(Optional.of(or(permittedByAcl(ACL), permittedByAcl(ACL)))));
    assertThat(w.getRedFlagWarnings(), empty());
  }

  @Test
  public void testToMatchExpr_singleClause() {
    Warnings w = new Warnings(true, true, true);
    assertThat(
        toMatchExpr(rmWithClauses(rmc(MATCH_ACL)), ImmutableSet.of(ACL), IFACE, w),
        equalTo(Optional.of(permittedByAcl(ACL))));
    assertThat(w.getRedFlagWarnings(), empty());
  }

  @Test
  public void testClauseToMatchExpr_emptyClause() {
    Warnings w = new Warnings(true, true, true);
    assertFalse(clauseToMatchExpr(rmc(), "rm", ImmutableSet.of(), IFACE, w).isPresent());
    assertThat(
        Iterables.getOnlyElement(w.getRedFlagWarnings()).getText(),
        containsString("clauses without match lines"));
  }

  @Test
  public void testClauseToMatchExpr_denyClause() {
    RouteMapClause rmc = new RouteMapClause(LineAction.DENY, "rmc", 10);
    rmc.addMatchLine(MATCH_ACL); // inconvertible bc it denies, not bc no match lines
    Warnings w = new Warnings(true, true, true);
    assertFalse(clauseToMatchExpr(rmc, "rm", ImmutableSet.of(ACL), IFACE, w).isPresent());
    assertThat(
        Iterables.getOnlyElement(w.getRedFlagWarnings()).getText(), containsString("deny clause"));
  }

  @Test
  public void testClauseToMatchExpr_setClause() {
    RouteMapClause rmc = rmc(MATCH_ACL);
    rmc.addSetLine(new RouteMapSetCommunityLine(ImmutableList.of(1L)));
    Warnings w = new Warnings(true, true, true);
    assertFalse(clauseToMatchExpr(rmc, "rm", ImmutableSet.of(ACL), IFACE, w).isPresent());
    assertThat(
        Iterables.getOnlyElement(w.getRedFlagWarnings()).getText(), containsString("set line"));
  }

  @Test
  public void testClauseToMatchExpr_inconvertibleLine() {
    RouteMapMatchLine inconvertibleLine =
        new RouteMapMatchIpAccessListLine(ImmutableSet.of("otherAcl"));
    RouteMapClause rmc = rmc(MATCH_ACL, inconvertibleLine, MATCH_ACL);
    Warnings w = new Warnings(true, true, true);
    assertFalse(clauseToMatchExpr(rmc, "rm", ImmutableSet.of(ACL), IFACE, w).isPresent());
    assertThat(
        Iterables.getOnlyElement(w.getRedFlagWarnings()).getText(),
        containsString("undefined ACL"));
  }

  @Test
  public void testClauseToMatchExpr_unmatchableLine() {
    // Clause matches a different interface. Should successfully convert to false expr (no warnings)
    RouteMapClause rmc = rmc(MATCH_ACL, MATCH_IFACE, MATCH_ACL);
    Warnings w = new Warnings(true, true, true);
    assertThat(
        clauseToMatchExpr(rmc, "rm", ImmutableSet.of(ACL), "otherIface", w),
        equalTo(Optional.of(AclLineMatchExprs.FALSE)));
    assertThat(w.getRedFlagWarnings(), empty());
  }

  @Test
  public void testClauseToMatchExpr_singleLine() {
    RouteMapClause rmc = rmc(MATCH_ACL);
    Warnings w = new Warnings(true, true, true);
    assertThat(
        clauseToMatchExpr(rmc, "rm", ImmutableSet.of(ACL), IFACE, w),
        equalTo(Optional.of(permittedByAcl(ACL))));
    assertThat(w.getRedFlagWarnings(), empty());
  }

  @Test
  public void testLineToMatchExpr_undefinedAcl() {
    RouteMapMatchLine line = new RouteMapMatchIpAccessListLine(ImmutableSet.of(ACL, "otherAcl"));
    Warnings w = new Warnings(true, true, true);
    assertFalse(lineToMatchExpr(line, "rm", ImmutableSet.of(ACL), IFACE, w).isPresent());
    assertThat(
        Iterables.getOnlyElement(w.getRedFlagWarnings()).getText(),
        containsString("undefined ACL"));
  }

  @Test
  public void testLineToMatchExpr_unsupportedLineType() {
    RouteMapMatchLine unsupportedLine = new RouteMapMatchIpPrefixListLine(ImmutableSet.of("pl"));
    Warnings w = new Warnings(true, true, true);
    assertFalse(lineToMatchExpr(unsupportedLine, "rm", ImmutableSet.of(ACL), IFACE, w).isPresent());
    assertThat(
        Iterables.getOnlyElement(w.getRedFlagWarnings()).getText(),
        containsString(RouteMapMatchIpPrefixListLine.class.getCanonicalName()));
  }

  @Test
  public void testLineToMatchExpr_definedAcls() {
    String acl2 = "acl2";
    Set<String> definedAcls = ImmutableSet.of(ACL, acl2);
    RouteMapMatchLine line = new RouteMapMatchIpAccessListLine(definedAcls);
    RouteMap rm = rmWithClauses(rmc(line));
    OrMatchExpr matchExpr =
        (OrMatchExpr) toMatchExpr(rm, definedAcls, IFACE, new Warnings(true, true, true)).get();
    assertThat(
        matchExpr.getDisjuncts(), containsInAnyOrder(permittedByAcl(ACL), permittedByAcl(acl2)));
  }

  @Test
  public void testLineToMatchExpr_matchedIface() {
    Warnings w = new Warnings(true, true, true);
    assertThat(
        lineToMatchExpr(MATCH_IFACE, "rm", ImmutableSet.of(ACL), IFACE, w),
        equalTo(Optional.of(toExpr(IFACE))));
  }

  @Test
  public void testLineToMatchExpr_unmatchedIface() {
    Warnings w = new Warnings(true, true, true);
    assertThat(
        lineToMatchExpr(MATCH_IFACE, "rm", ImmutableSet.of(ACL), "otherIface", w),
        equalTo(Optional.of(AclLineMatchExprs.FALSE)));
    assertThat(w.getRedFlagWarnings(), empty());
  }

  /** Creates a permitting {@link RouteMapClause} with the given match lines and sequence number. */
  private static RouteMapClause rmc(int seqNum, RouteMapMatchLine... lines) {
    RouteMapClause rmc = new RouteMapClause(LineAction.PERMIT, "rmc", seqNum);
    for (RouteMapMatchLine line : lines) {
      rmc.addMatchLine(line);
    }
    return rmc;
  }

  /** Creates a permitting {@link RouteMapClause} with the given match lines and seq number 10. */
  private static RouteMapClause rmc(RouteMapMatchLine... lines) {
    return rmc(10, lines);
  }

  /** Creates a {@link RouteMap} with the given clauses. */
  private static RouteMap rmWithClauses(RouteMapClause... clauses) {
    RouteMap rm = new RouteMap("rm");
    for (RouteMapClause clause : clauses) {
      assert !rm.getClauses().containsKey(clause.getSeqNum()); // clauses must have unique seq nums
      rm.getClauses().put(clause.getSeqNum(), clause);
    }
    return rm;
  }
}
