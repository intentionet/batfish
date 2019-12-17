package org.batfish.datamodel.flow;

import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.DENIED_IN;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.flow.BidirectionalTrace.Key;
import org.junit.Test;

/** Tests for {@link BidirectionalTrace}. */
public final class BidirectionalTraceTest {
  @Test
  public void testKeyEquals() {
    Flow flow1 = Flow.builder().setIngressNode("ingressNode").setTag("tag1").build();
    Flow flow2 = Flow.builder().setIngressNode("ingressNode").setTag("tag2").build();
    SessionMatchExpr dummySessionMatch =
        new SessionMatchExpr(IpProtocol.TCP, Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2"), null, null);
    FirewallSessionTraceInfo session =
        new FirewallSessionTraceInfo(
            "hostname", Accept.INSTANCE, ImmutableSet.of(), dummySessionMatch, null);
    new EqualsTester()
        .addEqualityGroup(
            new Key(flow1, ImmutableSet.of(), flow1), new Key(flow1, ImmutableSet.of(), flow1))
        .addEqualityGroup(new Key(flow2, ImmutableSet.of(), flow1))
        .addEqualityGroup(new Key(flow1, ImmutableSet.of(session), flow1))
        .addEqualityGroup(new Key(flow1, ImmutableSet.of(), null))
        .testEquals();
  }

  @Test
  public void testKey() {
    Flow flow1 = Flow.builder().setIngressNode("ingressNode").setTag("tag1").build();
    Flow flow2 = Flow.builder().setIngressNode("ingressNode").setTag("tag2").build();
    SessionMatchExpr dummySessionMatch =
        new SessionMatchExpr(IpProtocol.TCP, Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2"), null, null);
    Trace successTrace = new Trace(ACCEPTED, ImmutableList.of());
    assertThat(
        new BidirectionalTrace(flow1, successTrace, ImmutableSet.of(), flow2, successTrace)
            .getKey(),
        equalTo(new Key(flow1, ImmutableSet.of(), flow2)));
    assertThat(
        new BidirectionalTrace(flow2, successTrace, ImmutableSet.of(), flow1, successTrace)
            .getKey(),
        equalTo(new Key(flow2, ImmutableSet.of(), flow1)));
    FirewallSessionTraceInfo session =
        new FirewallSessionTraceInfo(
            "hostname", Accept.INSTANCE, ImmutableSet.of(), dummySessionMatch, null);
    Trace failTrace = new Trace(DENIED_IN, ImmutableList.of());
    assertThat(
        new BidirectionalTrace(flow1, failTrace, ImmutableSet.of(session), null, null).getKey(),
        equalTo(new Key(flow1, ImmutableSet.of(session), null)));
  }
}
