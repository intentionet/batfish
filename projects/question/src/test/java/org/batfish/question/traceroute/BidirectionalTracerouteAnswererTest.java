package org.batfish.question.traceroute;

import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.DELIVERED_TO_SUBNET;
import static org.batfish.datamodel.FlowDisposition.DENIED_IN;
import static org.batfish.datamodel.FlowDisposition.DENIED_OUT;
import static org.batfish.datamodel.FlowDisposition.EXITS_NETWORK;
import static org.batfish.datamodel.FlowDisposition.NEIGHBOR_UNREACHABLE;
import static org.batfish.datamodel.FlowDisposition.NO_ROUTE;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.COL_FORWARD_FLOW;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.COL_FORWARD_TRACES;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.COL_NEW_SESSIONS;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.COL_REVERSE_FLOW;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.COL_REVERSE_TRACES;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.computeBidirectionalTraces;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.groupTraces;
import static org.batfish.question.traceroute.BidirectionalTracerouteAnswerer.toRow;
import static org.batfish.question.traceroute.MockTracerouteEngine.forFlow;
import static org.batfish.question.traceroute.MockTracerouteEngine.forFlows;
import static org.batfish.question.traceroute.MockTracerouteEngine.forSessions;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.batfish.common.plugin.TracerouteEngine;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.flow.Accept;
import org.batfish.datamodel.flow.BidirectionalTrace;
import org.batfish.datamodel.flow.FirewallSessionTraceInfo;
import org.batfish.datamodel.flow.SessionMatchExpr;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TraceAndReverseFlow;
import org.batfish.datamodel.table.Row;
import org.junit.Test;

/** Tests for {@link BidirectionalTracerouteAnswerer}. */
public final class BidirectionalTracerouteAnswererTest {
  private static final Flow FORWARD_FLOW =
      Flow.builder()
          .setDstIp(Ip.parse("1.1.1.1"))
          .setTag("TAG")
          .setIngressNode("forwardIngressNode")
          .setIngressInterface("forwardIngressInterface")
          .build();

  private static final Flow REVERSE_FLOW =
      Flow.builder()
          .setDstIp(Ip.parse("1.1.1.1"))
          .setTag("TAG")
          .setIngressNode("reverseIngressNode")
          .setIngressInterface("reverseIngressInterface")
          .build();

  private static final SessionMatchExpr DUMMY_SESSION_FLOW =
      new SessionMatchExpr(IpProtocol.HOPOPT, Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2"), null, null);

  @Test
  public void testNoReverseFlow() {
    Trace forwardTrace = new Trace(NO_ROUTE, ImmutableList.of());
    TraceAndReverseFlow traceAndReverseFlow =
        new TraceAndReverseFlow(forwardTrace, null, ImmutableSet.of());

    TracerouteEngine tracerouteEngine =
        forFlow(FORWARD_FLOW, ImmutableList.of(traceAndReverseFlow));

    List<BidirectionalTrace> bidirectionalTraces =
        computeBidirectionalTraces(ImmutableSet.of(FORWARD_FLOW), tracerouteEngine, false);

    assertThat(
        bidirectionalTraces,
        contains(
            new BidirectionalTrace(FORWARD_FLOW, forwardTrace, ImmutableSet.of(), null, null)));
  }

  @Test
  public void testReverseFlow() {
    Trace forwardTrace = new Trace(ACCEPTED, ImmutableList.of());
    TraceAndReverseFlow forwardTarf =
        new TraceAndReverseFlow(forwardTrace, REVERSE_FLOW, ImmutableSet.of());
    Trace reverseTrace = new Trace(DENIED_IN, ImmutableList.of());
    TraceAndReverseFlow reverseTarf =
        new TraceAndReverseFlow(reverseTrace, null, ImmutableSet.of());

    TracerouteEngine tracerouteEngine =
        forFlows(
            ImmutableMap.of(
                FORWARD_FLOW,
                ImmutableList.of(forwardTarf),
                REVERSE_FLOW,
                ImmutableList.of(reverseTarf)));

    List<BidirectionalTrace> bidirectionalTraces =
        computeBidirectionalTraces(ImmutableSet.of(FORWARD_FLOW), tracerouteEngine, false);

    assertThat(
        bidirectionalTraces,
        contains(
            new BidirectionalTrace(
                FORWARD_FLOW, forwardTrace, ImmutableSet.of(), REVERSE_FLOW, reverseTrace)));
  }

  @Test
  public void testMultipath() {
    Trace forwardTrace1 = new Trace(ACCEPTED, ImmutableList.of());
    Trace forwardTrace2 = new Trace(DENIED_IN, ImmutableList.of());
    Trace forwardTrace3 = new Trace(DELIVERED_TO_SUBNET, ImmutableList.of());
    TraceAndReverseFlow forwardTarf1 =
        new TraceAndReverseFlow(forwardTrace1, REVERSE_FLOW, ImmutableSet.of());
    TraceAndReverseFlow forwardTarf2 =
        new TraceAndReverseFlow(forwardTrace2, null, ImmutableSet.of());
    TraceAndReverseFlow forwardTarf3 =
        new TraceAndReverseFlow(forwardTrace3, REVERSE_FLOW, ImmutableSet.of());
    Trace reverseTrace1 = new Trace(DENIED_IN, ImmutableList.of());
    Trace reverseTrace2 = new Trace(EXITS_NETWORK, ImmutableList.of());
    TraceAndReverseFlow reverseTarf1 =
        new TraceAndReverseFlow(reverseTrace1, null, ImmutableSet.of());
    TraceAndReverseFlow reverseTarf2 =
        new TraceAndReverseFlow(reverseTrace2, FORWARD_FLOW, ImmutableSet.of());

    TracerouteEngine tracerouteEngine =
        forFlows(
            ImmutableMap.of(
                FORWARD_FLOW,
                ImmutableList.of(forwardTarf1, forwardTarf2, forwardTarf3),
                REVERSE_FLOW,
                ImmutableList.of(reverseTarf1, reverseTarf2)));

    List<BidirectionalTrace> bidirectionalTraces =
        computeBidirectionalTraces(ImmutableSet.of(FORWARD_FLOW), tracerouteEngine, false);

    assertThat(
        bidirectionalTraces,
        contains(
            new BidirectionalTrace(
                FORWARD_FLOW, forwardTrace1, ImmutableSet.of(), REVERSE_FLOW, reverseTrace1),
            new BidirectionalTrace(
                FORWARD_FLOW, forwardTrace1, ImmutableSet.of(), REVERSE_FLOW, reverseTrace2),
            new BidirectionalTrace(FORWARD_FLOW, forwardTrace2, ImmutableSet.of(), null, null),
            new BidirectionalTrace(
                FORWARD_FLOW, forwardTrace3, ImmutableSet.of(), REVERSE_FLOW, reverseTrace1),
            new BidirectionalTrace(
                FORWARD_FLOW, forwardTrace3, ImmutableSet.of(), REVERSE_FLOW, reverseTrace2)));
  }

  @Test
  public void testSession() {
    Trace forwardTrace = new Trace(ACCEPTED, ImmutableList.of());
    Trace reverseTrace = new Trace(NEIGHBOR_UNREACHABLE, ImmutableList.of());
    FirewallSessionTraceInfo session =
        new FirewallSessionTraceInfo(
            "session", Accept.INSTANCE, ImmutableSet.of(), DUMMY_SESSION_FLOW, null);
    TraceAndReverseFlow forwardTarf =
        new TraceAndReverseFlow(forwardTrace, REVERSE_FLOW, ImmutableList.of(session));
    TraceAndReverseFlow reverseTarf =
        new TraceAndReverseFlow(reverseTrace, null, ImmutableList.of());
    TracerouteEngine tracerouteEngine =
        forSessions(
            ImmutableMap.of(
                ImmutableSet.of(),
                ImmutableMap.of(FORWARD_FLOW, ImmutableList.of(forwardTarf)),
                ImmutableSet.of(session),
                ImmutableMap.of(REVERSE_FLOW, ImmutableList.of(reverseTarf))));
    List<BidirectionalTrace> bidirectionalTraces =
        computeBidirectionalTraces(ImmutableSet.of(FORWARD_FLOW), tracerouteEngine, false);
    assertThat(
        bidirectionalTraces,
        contains(
            new BidirectionalTrace(
                FORWARD_FLOW, forwardTrace, ImmutableSet.of(session), REVERSE_FLOW, reverseTrace)));
  }

  /** Make sure we don't mix traces and sessions. */
  @Test
  public void testSessions() {
    Trace sessionForwardTrace = new Trace(ACCEPTED, ImmutableList.of());
    Trace noSessionForwardTrace = new Trace(DELIVERED_TO_SUBNET, ImmutableList.of());
    FirewallSessionTraceInfo session =
        new FirewallSessionTraceInfo(
            "session", Accept.INSTANCE, ImmutableSet.of(), DUMMY_SESSION_FLOW, null);
    TraceAndReverseFlow sessionForwardTarf =
        new TraceAndReverseFlow(sessionForwardTrace, REVERSE_FLOW, ImmutableList.of(session));
    TraceAndReverseFlow noSessionForwardTarf =
        new TraceAndReverseFlow(noSessionForwardTrace, REVERSE_FLOW, ImmutableList.of());

    Trace sessionReverseTrace = new Trace(DENIED_IN, ImmutableList.of());
    Trace noSessionReverseTrace = new Trace(DENIED_OUT, ImmutableList.of());
    TraceAndReverseFlow sessionReverseTarf =
        new TraceAndReverseFlow(sessionReverseTrace, null, ImmutableList.of());
    TraceAndReverseFlow noSessionReverseTarf =
        new TraceAndReverseFlow(noSessionReverseTrace, null, ImmutableList.of());

    TracerouteEngine tracerouteEngine =
        forSessions(
            ImmutableMap.of(
                ImmutableSet.of(),
                ImmutableMap.of(
                    FORWARD_FLOW, ImmutableList.of(sessionForwardTarf, noSessionForwardTarf),
                    REVERSE_FLOW, ImmutableList.of(noSessionReverseTarf)),
                ImmutableSet.of(session),
                ImmutableMap.of(REVERSE_FLOW, ImmutableList.of(sessionReverseTarf))));
    List<BidirectionalTrace> bidirectionalTraces =
        computeBidirectionalTraces(ImmutableSet.of(FORWARD_FLOW), tracerouteEngine, false);
    assertThat(
        bidirectionalTraces,
        containsInAnyOrder(
            new BidirectionalTrace(
                FORWARD_FLOW,
                sessionForwardTrace,
                ImmutableSet.of(session),
                REVERSE_FLOW,
                sessionReverseTrace),
            new BidirectionalTrace(
                FORWARD_FLOW,
                noSessionForwardTrace,
                ImmutableSet.of(),
                REVERSE_FLOW,
                noSessionReverseTrace)));
  }

  @Test
  public void testGroupTraces() {
    Trace t1 = new Trace(ACCEPTED, ImmutableList.of());
    Trace t2 = new Trace(EXITS_NETWORK, ImmutableList.of());
    Trace t3 = new Trace(NEIGHBOR_UNREACHABLE, ImmutableList.of());
    Trace t4 = new Trace(DENIED_IN, ImmutableList.of());

    FirewallSessionTraceInfo session1 =
        new FirewallSessionTraceInfo(
            "session1", Accept.INSTANCE, ImmutableSet.of(), DUMMY_SESSION_FLOW, null);
    FirewallSessionTraceInfo session2 =
        new FirewallSessionTraceInfo(
            "session2", Accept.INSTANCE, ImmutableSet.of(), DUMMY_SESSION_FLOW, null);

    {
      // All BidirectionalTraces have the same key, so are in the same group.
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t3);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t4);
      BidirectionalTrace bt3 =
          new BidirectionalTrace(FORWARD_FLOW, t2, ImmutableSet.of(), REVERSE_FLOW, t3);
      BidirectionalTrace bt4 =
          new BidirectionalTrace(FORWARD_FLOW, t2, ImmutableSet.of(), REVERSE_FLOW, t4);

      List<BidirectionalTrace> bts = ImmutableList.of(bt1, bt2, bt3, bt4);
      assertThat(groupTraces(bts), hasEntry(equalTo(bt1.getKey()), equalTo(bts)));
    }

    {
      // Traces with different forward flows are in different groups
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t2);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(REVERSE_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t2);

      assertThat(
          groupTraces(ImmutableList.of(bt1, bt2)),
          equalTo(
              ImmutableMap.of(
                  bt1.getKey(), ImmutableList.of(bt1), bt2.getKey(), ImmutableList.of(bt2))));
    }

    {
      // Traces with different number of sessions are in different groups
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t2);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(session1), REVERSE_FLOW, t2);

      assertThat(
          groupTraces(ImmutableList.of(bt1, bt2)),
          equalTo(
              ImmutableMap.of(
                  bt1.getKey(), ImmutableList.of(bt1), bt2.getKey(), ImmutableList.of(bt2))));
    }

    {
      // Traces with different sessions are in different groups
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(session1), REVERSE_FLOW, t2);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(session2), REVERSE_FLOW, t2);

      assertThat(
          groupTraces(ImmutableList.of(bt1, bt2)),
          equalTo(
              ImmutableMap.of(
                  bt1.getKey(), ImmutableList.of(bt1), bt2.getKey(), ImmutableList.of(bt2))));
    }

    {
      // Traces with different reverse flows are in different groups
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t2);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), FORWARD_FLOW, t2);

      assertThat(
          groupTraces(ImmutableList.of(bt1, bt2)),
          equalTo(
              ImmutableMap.of(
                  bt1.getKey(), ImmutableList.of(bt1), bt2.getKey(), ImmutableList.of(bt2))));
    }
  }

  @Test
  public void testToRow() {
    Trace t1 = new Trace(ACCEPTED, ImmutableList.of());
    Trace t2 = new Trace(EXITS_NETWORK, ImmutableList.of());

    {
      // Same key, different forward flows
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t2);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(FORWARD_FLOW, t2, ImmutableSet.of(), REVERSE_FLOW, t2);

      Row row = toRow(bt1.getKey(), ImmutableList.of(bt1, bt2));
      assertThat(
          row,
          allOf(
              hasColumn(COL_FORWARD_FLOW, equalTo(FORWARD_FLOW), Schema.FLOW),
              hasColumn(COL_FORWARD_TRACES, contains(t1, t2), Schema.list(Schema.TRACE)),
              hasColumn(COL_NEW_SESSIONS, empty(), Schema.list(Schema.STRING)),
              hasColumn(COL_REVERSE_FLOW, equalTo(REVERSE_FLOW), Schema.FLOW),
              hasColumn(COL_REVERSE_TRACES, contains(t2), Schema.list(Schema.TRACE))));
    }

    {
      // Same key, different reverse flows
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t1);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t2);

      Row row = toRow(bt1.getKey(), ImmutableList.of(bt1, bt2));
      assertThat(
          row,
          allOf(
              hasColumn(COL_FORWARD_FLOW, equalTo(FORWARD_FLOW), Schema.FLOW),
              hasColumn(COL_FORWARD_TRACES, contains(t1), Schema.list(Schema.TRACE)),
              hasColumn(COL_NEW_SESSIONS, empty(), Schema.list(Schema.STRING)),
              hasColumn(COL_REVERSE_FLOW, equalTo(REVERSE_FLOW), Schema.FLOW),
              hasColumn(COL_REVERSE_TRACES, contains(t1, t2), Schema.list(Schema.TRACE))));
    }

    {
      // 4 traces get collapsed to 2x2
      BidirectionalTrace bt1 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t1);
      BidirectionalTrace bt2 =
          new BidirectionalTrace(FORWARD_FLOW, t1, ImmutableSet.of(), REVERSE_FLOW, t2);
      BidirectionalTrace bt3 =
          new BidirectionalTrace(FORWARD_FLOW, t2, ImmutableSet.of(), REVERSE_FLOW, t1);
      BidirectionalTrace bt4 =
          new BidirectionalTrace(FORWARD_FLOW, t2, ImmutableSet.of(), REVERSE_FLOW, t2);

      Row row = toRow(bt1.getKey(), ImmutableList.of(bt1, bt2, bt3, bt4));
      assertThat(
          row,
          allOf(
              hasColumn(COL_FORWARD_FLOW, equalTo(FORWARD_FLOW), Schema.FLOW),
              hasColumn(COL_FORWARD_TRACES, contains(t1, t2), Schema.list(Schema.TRACE)),
              hasColumn(COL_NEW_SESSIONS, empty(), Schema.list(Schema.STRING)),
              hasColumn(COL_REVERSE_FLOW, equalTo(REVERSE_FLOW), Schema.FLOW),
              hasColumn(COL_REVERSE_TRACES, contains(t1, t2), Schema.list(Schema.TRACE))));
    }

    {
      // Sessions in the key
      FirewallSessionTraceInfo session1 =
          new FirewallSessionTraceInfo(
              "session1", Accept.INSTANCE, ImmutableSet.of(), DUMMY_SESSION_FLOW, null);
      FirewallSessionTraceInfo session2 =
          new FirewallSessionTraceInfo(
              "session2", Accept.INSTANCE, ImmutableSet.of(), DUMMY_SESSION_FLOW, null);
      Set<FirewallSessionTraceInfo> sessions = ImmutableSet.of(session1, session2);

      BidirectionalTrace bt = new BidirectionalTrace(FORWARD_FLOW, t1, sessions, REVERSE_FLOW, t1);
      Row row = toRow(bt.getKey(), ImmutableList.of(bt));
      assertThat(
          row,
          allOf(
              hasColumn(COL_FORWARD_FLOW, equalTo(FORWARD_FLOW), Schema.FLOW),
              hasColumn(COL_FORWARD_TRACES, contains(t1), Schema.list(Schema.TRACE)),
              hasColumn(
                  COL_NEW_SESSIONS, contains("session1", "session2"), Schema.list(Schema.STRING)),
              hasColumn(COL_REVERSE_FLOW, equalTo(REVERSE_FLOW), Schema.FLOW),
              hasColumn(COL_REVERSE_TRACES, contains(t1), Schema.list(Schema.TRACE))));
    }

    {
      // No return traces
      Trace t = new Trace(DENIED_IN, ImmutableList.of());
      BidirectionalTrace bt =
          new BidirectionalTrace(FORWARD_FLOW, t, ImmutableSet.of(), null, null);
      Row row = toRow(bt.getKey(), ImmutableList.of(bt));
      assertThat(
          row,
          allOf(
              hasColumn(COL_FORWARD_FLOW, equalTo(FORWARD_FLOW), Schema.FLOW),
              hasColumn(COL_FORWARD_TRACES, contains(t), Schema.list(Schema.TRACE)),
              hasColumn(COL_NEW_SESSIONS, empty(), Schema.list(Schema.STRING)),
              hasColumn(COL_REVERSE_FLOW, nullValue(), Schema.FLOW),
              hasColumn(COL_REVERSE_TRACES, empty(), Schema.list(Schema.TRACE))));
    }
  }
}
