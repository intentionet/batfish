package org.batfish.datamodel.acl;

import static org.batfish.datamodel.IpAccessListLine.accepting;
import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDstIp;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDstPrefix;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcIp;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcPrefix;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.datamodel.IpAccessList;
import org.junit.Before;
import org.junit.Test;

public class AclExplainerTest {

  private BDDSourceManager _mgr;

  private BDDPacket _pkt;

  @Before
  public void setup() {
    _pkt = new BDDPacket();
    _mgr = BDDSourceManager.forInterfaces(_pkt, ImmutableSet.of());
  }

  private AclLineMatchExpr explain(IpAccessList acl) {
    return explain(TRUE, acl);
  }

  private AclLineMatchExpr explain(AclLineMatchExpr invariantExpr, IpAccessList acl) {
    return AclExplainer.explain(
        _pkt, _mgr, invariantExpr, acl, ImmutableMap.of(), ImmutableMap.of());
  }

  private AclLineMatchExpr explainDifferential(IpAccessList denyAcl, IpAccessList permitAcl) {
    return explainDifferential(TRUE, denyAcl, permitAcl);
  }

  private AclLineMatchExpr explainDifferential(
      AclLineMatchExpr invariantExpr, IpAccessList denyAcl, IpAccessList permitAcl) {
    return AclExplainer.explainDifferential(
        _pkt,
        _mgr,
        invariantExpr,
        denyAcl,
        ImmutableMap.of(),
        ImmutableMap.of(),
        permitAcl,
        ImmutableMap.of(),
        ImmutableMap.of());
  }

  @Test
  public void testExplainSimple() {
    MatchHeaderSpace matchDstIp1 = matchDstIp("1.1.1.1");
    AclLineMatchExpr matchDstIp2 = matchDstIp("2.2.2.2");
    IpAccessList acl =
        IpAccessList.builder()
            .setName("acl")
            .setLines(ImmutableList.of(accepting(matchDstIp1), accepting(matchDstIp2)))
            .build();
    AclLineMatchExpr explanation = explain(acl);
    assertThat(explanation, equalTo(or(matchDstIp1, matchDstIp2)));
  }

  @Test
  public void testExplainInvariant() {
    MatchHeaderSpace matchDstIp1 = matchDstIp("1.1.1.1");
    AclLineMatchExpr matchDstIp2 = matchDstIp("2.2.2.2");
    IpAccessList acl =
        IpAccessList.builder()
            .setName("acl")
            .setLines(ImmutableList.of(accepting(matchDstIp1), accepting(matchDstIp2)))
            .build();
    AclLineMatchExpr explanation = explain(matchDstPrefix("2.0.0.0/8"), acl);
    assertThat(explanation, equalTo(matchDstIp2));
  }

  @Test
  public void testExplainRedundantPermit() {
    MatchHeaderSpace matchDstIp = matchDstIp("1.2.3.4");
    AclLineMatchExpr matchDstPrefix = matchDstPrefix("1.2.3.0/24");
    IpAccessList acl =
        IpAccessList.builder()
            .setName("acl")
            .setLines(ImmutableList.of(accepting(matchDstIp), accepting(matchDstPrefix)))
            .build();
    AclLineMatchExpr explanation = explain(acl);
    assertThat(explanation, equalTo(matchDstPrefix));
  }

  @Test
  public void testExplainRedundantDeny() {
    MatchHeaderSpace matchSrcIp = matchSrcIp("1.2.3.4");
    AclLineMatchExpr matchSrcPrefix = matchSrcPrefix("1.2.3.0/24");
    MatchHeaderSpace matchDstIp = matchDstIp("1.2.3.4");
    AclLineMatchExpr matchDstPrefix = matchDstPrefix("1.2.3.0/24");
    IpAccessList acl =
        IpAccessList.builder()
            .setName("acl")
            .setLines(
                ImmutableList.of(
                    rejecting(matchSrcIp),
                    accepting(matchDstIp),
                    rejecting(matchSrcPrefix),
                    accepting(matchDstPrefix)))
            .build();
    assertThat(
        explain(acl),
        equalTo(or(and(matchDstIp, not(matchSrcIp)), and(matchDstPrefix, not(matchSrcPrefix)))));

    /*
     * Reverse the order of the rejecting lines. Now we get:
     * or(and(matchDstIp, not(matchSrcPrefix)), and(matchDstPrefix, not(matchSrcPrefix)))))
     * And then the first disjunct is removed because it's subsumed by the second.
     */
    acl =
        IpAccessList.builder()
            .setName("acl")
            .setLines(
                ImmutableList.of(
                    rejecting(matchSrcPrefix),
                    accepting(matchDstIp),
                    rejecting(matchSrcIp),
                    accepting(matchDstPrefix)))
            .build();
    assertThat(explain(acl), equalTo(or(and(matchDstPrefix, not(matchSrcPrefix)))));
  }

  @Test
  public void testExplainRejectLines() {
    MatchHeaderSpace matchDstIp = matchDstIp("1.2.3.4");
    AclLineMatchExpr matchDstPrefix = matchDstPrefix("1.2.3.0/24");
    IpAccessList acl =
        IpAccessList.builder()
            .setName("acl")
            .setLines(ImmutableList.of(accepting(matchDstIp), accepting(matchDstPrefix)))
            .build();
    AclLineMatchExpr explanation = explain(acl);
    assertThat(explanation, equalTo(matchDstPrefix));
  }

  @Test
  public void testExplainDifferentialSimple() {
    MatchHeaderSpace matchDstIp = matchDstIp("1.2.3.4");
    AclLineMatchExpr matchDstPrefix = matchDstPrefix("1.2.3.0/24");
    IpAccessList denyAcl =
        IpAccessList.builder()
            .setName("deny")
            .setLines(ImmutableList.of(accepting(matchDstIp)))
            .build();
    IpAccessList permitAcl =
        IpAccessList.builder()
            .setName("permit")
            .setLines(ImmutableList.of(accepting(matchDstPrefix)))
            .build();
    AclLineMatchExpr explanation = explainDifferential(denyAcl, permitAcl);
    assertThat(explanation, equalTo(and(matchDstPrefix, not(matchDstIp))));
  }

  @Test
  public void testExplainDifferentialReject() {
    MatchHeaderSpace matchDstIp = matchDstIp("1.2.3.4");
    AclLineMatchExpr matchDstPrefix24 = matchDstPrefix("1.2.3.0/24");
    AclLineMatchExpr matchDstPrefix16 = matchDstPrefix("1.2.0.0/16");
    IpAccessList denyAcl =
        IpAccessList.builder()
            .setName("deny")
            .setLines(ImmutableList.of(rejecting(matchDstIp), accepting(matchDstPrefix24)))
            .build();
    IpAccessList permitAcl =
        IpAccessList.builder()
            .setName("permit")
            .setLines(ImmutableList.of(accepting(matchDstPrefix16)))
            .build();
    assertThat(
        explainDifferential(denyAcl, permitAcl),
        equalTo(or(matchDstIp, and(matchDstPrefix16, not(matchDstPrefix24)))));
    /*
     * Add an invariant that contradicts the second disjunct
     */
    assertThat(explainDifferential(matchDstPrefix24, denyAcl, permitAcl), equalTo(matchDstIp));

    /*
     * Switch the prefixes. Now the second explanation becomes
     *   and(matchDstPrefix24, not(matchDstPrefix16))
     * Which is False and gets removed.
     */
    denyAcl =
        IpAccessList.builder()
            .setName("deny")
            .setLines(ImmutableList.of(rejecting(matchDstIp), accepting(matchDstPrefix16)))
            .build();
    permitAcl =
        IpAccessList.builder()
            .setName("permit")
            .setLines(ImmutableList.of(accepting(matchDstPrefix24)))
            .build();
    assertThat(explainDifferential(denyAcl, permitAcl), equalTo(matchDstIp));
  }
}
