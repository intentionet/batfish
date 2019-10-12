package org.batfish.datamodel.acl;

import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDstIp;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.IdentityHashMap;
import java.util.Set;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AclExplanationTest {
  @Rule public ExpectedException _exception = ExpectedException.none();

  AclExplanation _explanation;

  @Before
  public void setup() {
    _explanation = new AclExplanation();
  }

  @Test
  public void testSimpleWithProvenance() {
    MatchHeaderSpace require = matchDst(Prefix.parse("1.2.3.0/24"));
    MatchHeaderSpace forbid = matchDst(Ip.parse("1.2.3.4"));

    _explanation.requireHeaderSpace(require);
    _explanation.forbidHeaderSpace(forbid);

    AclLineMatchExprWithProvenance<AclLineMatchExpr> explanationWithProvenance =
        _explanation.build();

    AclLineMatchExpr explanation = explanationWithProvenance.getMatchExpr();
    assertThat(explanation, equalTo(and(require, not(forbid))));

    IdentityHashMap<AclLineMatchExpr, Set<AclLineMatchExpr>> provenance =
        explanationWithProvenance.getProvenance();
    assertThat(provenance.entrySet(), hasSize(2));

    assertThat(provenance, hasEntry(require, ImmutableSet.of(require)));
    assertThat(provenance, hasEntry(forbid, ImmutableSet.of(forbid)));
  }

  @Test
  public void testIntersectOriginateFromDeviceWithProvenance() {
    _explanation.requireOriginatingFromDevice(ORIGINATING_FROM_DEVICE);
    _explanation.requireOriginatingFromDevice(ORIGINATING_FROM_DEVICE);

    AclLineMatchExprWithProvenance<AclLineMatchExpr> explanationWithProvenance =
        _explanation.build();

    AclLineMatchExpr explanation = explanationWithProvenance.getMatchExpr();
    assertThat(explanation, equalTo(ORIGINATING_FROM_DEVICE));

    IdentityHashMap<AclLineMatchExpr, Set<AclLineMatchExpr>> provenance =
        explanationWithProvenance.getProvenance();
    assertThat(provenance.entrySet(), hasSize(1));

    assertThat(
        provenance, hasEntry(ORIGINATING_FROM_DEVICE, ImmutableSet.of(ORIGINATING_FROM_DEVICE)));
  }

  @Test
  public void testIntersectSourcesWithProvenance() {
    MatchSrcInterface matchSrcInterface1 =
        new MatchSrcInterface(ImmutableSet.of("foo", "bar", "baz"));
    MatchSrcInterface matchSrcInterface2 = new MatchSrcInterface(ImmutableSet.of("foo"));
    _explanation.requireSourceInterfaces(matchSrcInterface1);
    _explanation.requireSourceInterfaces(matchSrcInterface2);
    AclLineMatchExprWithProvenance<AclLineMatchExpr> explanationWithProvenance =
        _explanation.build();

    AclLineMatchExpr explanation = explanationWithProvenance.getMatchExpr();
    assertThat(explanation, equalTo(matchSrcInterface("foo")));

    IdentityHashMap<AclLineMatchExpr, Set<AclLineMatchExpr>> provenance =
        explanationWithProvenance.getProvenance();
    assertThat(provenance.entrySet(), hasSize(1));

    assertThat(
        provenance, hasEntry(explanation, ImmutableSet.of(matchSrcInterface1, matchSrcInterface2)));
  }

  @Test
  public void testMultipleForbids() {
    MatchHeaderSpace forbid1 = matchDst(Ip.parse("1.2.3.4"));
    MatchHeaderSpace forbid2 = matchDst(Ip.parse("1.2.3.5"));

    _explanation.forbidHeaderSpace(forbid1);
    _explanation.forbidHeaderSpace(forbid2);
    assertThat(_explanation.build().getMatchExpr(), equalTo(and(not(forbid1), not(forbid2))));
  }

  @Test
  public void testIntersectRequire() {
    MatchHeaderSpace matchDstIp = matchDst(Ip.parse("1.2.3.4"));
    MatchHeaderSpace matchDstPrefix = matchDst(Prefix.parse("1.2.3.0/24"));
    MatchHeaderSpace matchDstPort =
        new MatchHeaderSpace(
            HeaderSpace.builder().setDstPorts(ImmutableList.of(SubRange.singleton(80))).build());
    MatchHeaderSpace matchDstPrefixAndPort =
        new MatchHeaderSpace(
            matchDstPrefix
                .getHeaderspace()
                .toBuilder()
                .setDstPorts(ImmutableList.of(SubRange.singleton(80)))
                .build());

    _explanation.requireHeaderSpace(matchDstPrefix);
    _explanation.requireHeaderSpace(matchDstPort);
    _explanation.forbidHeaderSpace(matchDstIp);
    AclLineMatchExpr expr = and(matchDstPrefixAndPort, not(matchDstIp));
    assertThat(_explanation.build().getMatchExpr(), equalTo(expr));
  }

  @Test
  public void testExplainLiterals() {
    MatchHeaderSpace matchHeaderSpace = matchDstIp("1.2.3.4");
    AclLineMatchExpr notMatchHeaderSpace = not(matchHeaderSpace);
    MatchSrcInterface matchSrcInterface = matchSrcInterface("foo");
    AclLineMatchExpr expr =
        AclExplanation.explainLiterals(
                ImmutableList.of(matchHeaderSpace, notMatchHeaderSpace, matchSrcInterface))
            .getMatchExpr();
    assertThat(expr, equalTo(and(matchHeaderSpace, matchSrcInterface, notMatchHeaderSpace)));
  }

  @Test
  public void testUnsatSource1() {
    _explanation.requireOriginatingFromDevice(ORIGINATING_FROM_DEVICE);
    _exception.expect(IllegalStateException.class);
    _exception.expectMessage("AclExplanation is unsatisfiable");
    _explanation.requireSourceInterfaces(new MatchSrcInterface(ImmutableSet.of("foo")));
  }

  @Test
  public void testUnsatSource2() {
    _explanation.requireSourceInterfaces(new MatchSrcInterface(ImmutableSet.of("foo")));
    _exception.expect(IllegalStateException.class);
    _exception.expectMessage("AclExplanation is unsatisfiable");
    _explanation.requireOriginatingFromDevice(ORIGINATING_FROM_DEVICE);
  }

  @Test
  public void testUnsatSource3() {
    _explanation.requireSourceInterfaces(new MatchSrcInterface(ImmutableSet.of("foo")));
    _exception.expect(IllegalStateException.class);
    _exception.expectMessage("AclExplanation is unsatisfiable");
    _explanation.requireSourceInterfaces(new MatchSrcInterface(ImmutableSet.of("bar")));
  }
}
