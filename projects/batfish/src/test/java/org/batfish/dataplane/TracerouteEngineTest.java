package org.batfish.dataplane;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.acl.TrueExpr;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TracerouteEngineTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static Flow makeFlow() {
    Flow.Builder builder = new Flow.Builder();
    builder.setSrcIp(new Ip("1.2.3.4"));
    builder.setIngressNode("foo");
    builder.setTag("TEST");
    return builder.build();
  }

  private static IpAccessList makeAcl(String name, LineAction action) {
    IpAccessListLine aclLine =
        IpAccessListLine.builder().setAction(action).setMatchCondition(TrueExpr.INSTANCE).build();
    return new IpAccessList(name, singletonList(aclLine));
  }

  @Test
  public void testApplySourceNatSingleAclMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("accept", LineAction.ACCEPT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TraceRouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatSingleAclNoMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("reject", LineAction.REJECT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TraceRouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed, is(flow));
  }

  @Test
  public void testApplySourceNatFirstMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("firstAccept", LineAction.ACCEPT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("secondAccept", LineAction.ACCEPT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TraceRouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatLateMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("rejectAll", LineAction.REJECT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("acceptAnyway", LineAction.ACCEPT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TraceRouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.8")));
  }

  @Test
  public void testApplySourceNatInvalidAclThrows() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("matchAll", LineAction.ACCEPT));

    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("missing NAT address or pool");
    TraceRouteEngineImpl.applySourceNat(
        flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
  }
}
