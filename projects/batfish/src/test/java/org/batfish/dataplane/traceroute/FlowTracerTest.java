package org.batfish.dataplane.traceroute;

import static org.batfish.datamodel.FlowDisposition.DENIED_IN;
import static org.batfish.datamodel.FlowDisposition.LOOP;
import static org.batfish.datamodel.FlowDisposition.NULL_ROUTED;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.matchers.TraceAndReverseFlowMatchers.hasNewFirewallSessions;
import static org.batfish.datamodel.matchers.TraceAndReverseFlowMatchers.hasTrace;
import static org.batfish.datamodel.matchers.TraceMatchers.hasDisposition;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignDestinationIp;
import static org.batfish.dataplane.traceroute.FlowTracer.initialFlowTracer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.FibEntry;
import org.batfish.datamodel.FibNextVrf;
import org.batfish.datamodel.FibNullRoute;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.MockDataPlane;
import org.batfish.datamodel.MockFib;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.Accept;
import org.batfish.datamodel.flow.ExitOutputIfaceStep;
import org.batfish.datamodel.flow.FirewallSessionTraceInfo;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.RoutingStep;
import org.batfish.datamodel.flow.Step;
import org.batfish.datamodel.flow.StepAction;
import org.batfish.datamodel.flow.TraceAndReverseFlow;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.transformation.Transformation;
import org.junit.Test;

/** Tests for {@link FlowTracer}. */
public final class FlowTracerTest {
  @Test
  public void testBuildDeniedTraceNoNewSessions() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf vrf = nf.vrfBuilder().setOwner(c).build();

    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("1.1.1.1"))
            .setIngressNode(c.getHostname())
            .setIngressVrf(vrf.getName())
            .setTag("tag")
            .build();

    List<TraceAndReverseFlow> traces = new ArrayList<>();

    TracerouteEngineImplContext ctxt =
        new TracerouteEngineImplContext(
            MockDataPlane.builder().setConfigs(ImmutableMap.of(c.getHostname(), c)).build(),
            Topology.EMPTY,
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableMap.of(),
            false);
    FlowTracer flowTracer = initialFlowTracer(ctxt, c.getHostname(), null, flow, traces::add);
    flowTracer.buildDeniedTrace(DENIED_IN);
    assertThat(
        traces,
        contains(allOf(hasTrace(hasDisposition(DENIED_IN)), hasNewFirewallSessions(empty()))));
  }

  @Test
  public void testBuildDeniedTraceNewSessions() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf vrf = nf.vrfBuilder().setOwner(c).build();

    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("1.1.1.1"))
            .setIngressNode(c.getHostname())
            .setIngressVrf(vrf.getName())
            .setTag("tag")
            .build();
    List<TraceAndReverseFlow> traces = new ArrayList<>();

    FirewallSessionTraceInfo sessionInfo =
        new FirewallSessionTraceInfo("hostname", Accept.INSTANCE, ImmutableSet.of(), TRUE, null);
    TracerouteEngineImplContext ctxt =
        new TracerouteEngineImplContext(
            MockDataPlane.builder().setConfigs(ImmutableMap.of(c.getHostname(), c)).build(),
            Topology.EMPTY,
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableMap.of(),
            false);
    FlowTracer flowTracer =
        new FlowTracer(
            ctxt,
            c,
            null,
            new Node(c.getHostname()),
            traces::add,
            NodeInterfacePair.of("node", "iface"),
            ImmutableSet.of(sessionInfo),
            flow,
            vrf.getName(),
            new ArrayList<>(),
            ImmutableList.of(),
            new Stack<>(),
            flow);

    flowTracer.buildDeniedTrace(DENIED_IN);
    assertThat(
        traces,
        contains(
            allOf(
                hasTrace(hasDisposition(DENIED_IN)),
                hasNewFirewallSessions(contains(sessionInfo)))));
  }

  @Test
  public void testFibLookupNullRoute() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    String hostname = c.getHostname();
    Vrf.Builder vb = nf.vrfBuilder().setOwner(c);
    Vrf srcVrf = vb.build();
    String srcVrfName = srcVrf.getName();

    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("1.1.1.1"))
            .setIngressNode(c.getHostname())
            .setIngressVrf(srcVrfName)
            .setTag("tag")
            .build();
    Ip dstIp = flow.getDstIp();
    ImmutableList.Builder<TraceAndReverseFlow> traces = ImmutableList.builder();

    Fib srcFib =
        MockFib.builder()
            .setFibEntries(
                ImmutableMap.of(
                    dstIp,
                    ImmutableSet.of(
                        new FibEntry(
                            FibNullRoute.INSTANCE,
                            ImmutableList.of(
                                StaticRoute.builder()
                                    .setAdmin(1)
                                    .setNetwork(Prefix.ZERO)
                                    .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                                    .build())))))
            .build();

    TracerouteEngineImplContext ctxt =
        new TracerouteEngineImplContext(
            MockDataPlane.builder().setConfigs(ImmutableMap.of(c.getHostname(), c)).build(),
            Topology.EMPTY,
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableMap.of(hostname, ImmutableMap.of(srcVrfName, srcFib)),
            false);
    FlowTracer flowTracer = initialFlowTracer(ctxt, hostname, null, flow, traces::add);
    flowTracer.fibLookup(dstIp, hostname, srcFib);

    assertThat(traces.build(), contains(hasTrace(hasDisposition(NULL_ROUTED))));
  }

  @Test
  public void testFibLookupNextVrf() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    String hostname = c.getHostname();
    Vrf.Builder vb = nf.vrfBuilder().setOwner(c);
    Vrf srcVrf = vb.build();
    String srcVrfName = srcVrf.getName();
    Vrf nextVrf = vb.build();
    String nextVrfName = nextVrf.getName();
    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("1.1.1.1"))
            .setIngressNode(c.getHostname())
            .setIngressVrf(srcVrfName)
            .setTag("tag")
            .build();
    Ip dstIp = flow.getDstIp();
    StaticRoute nextVrfRoute =
        StaticRoute.builder().setAdmin(1).setNetwork(Prefix.ZERO).setNextVrf(nextVrfName).build();
    StaticRoute nullRoute =
        StaticRoute.builder()
            .setAdmin(1)
            .setNetwork(Prefix.ZERO)
            .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
            .build();
    Fib srcFib =
        MockFib.builder()
            .setFibEntries(
                ImmutableMap.of(
                    dstIp,
                    ImmutableSet.of(
                        new FibEntry(new FibNextVrf(nextVrfName), ImmutableList.of(nextVrfRoute)))))
            .build();
    Fib nextFib =
        MockFib.builder()
            .setFibEntries(
                ImmutableMap.of(
                    dstIp,
                    ImmutableSet.of(
                        new FibEntry(FibNullRoute.INSTANCE, ImmutableList.of(nullRoute)))))
            .build();
    List<TraceAndReverseFlow> traces = new ArrayList<>();
    TracerouteEngineImplContext ctxt =
        new TracerouteEngineImplContext(
            MockDataPlane.builder().setConfigs(ImmutableMap.of(c.getHostname(), c)).build(),
            Topology.EMPTY,
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableMap.of(hostname, ImmutableMap.of(srcVrfName, srcFib, nextVrfName, nextFib)),
            false);
    FlowTracer flowTracer = initialFlowTracer(ctxt, hostname, null, flow, traces::add);
    flowTracer.fibLookup(dstIp, hostname, srcFib);

    // Should be delegated from srcFib to nextFib and eventually NULL_ROUTED
    assertThat(traces, contains(hasTrace(hasDisposition(NULL_ROUTED))));

    List<Hop> hops = traces.get(0).getTrace().getHops();

    // There should be a single hop
    assertThat(hops, hasSize(1));

    List<Step<?>> steps = hops.iterator().next().getSteps();

    // There should be 2 routing steps and an exit-output-iface step;
    // - next-vr should occur in the first step
    // - null-route should occur in the second step
    // - third step should have null-route action

    assertThat(
        steps,
        contains(
            instanceOf(RoutingStep.class),
            instanceOf(RoutingStep.class),
            instanceOf(ExitOutputIfaceStep.class)));
    assertThat(
        ((RoutingStep) steps.get(0)).getDetail().getRoutes().get(0).getNextVrf(),
        equalTo(nextVrfName));
    assertThat(
        ((RoutingStep) steps.get(1)).getDetail().getRoutes().get(0).getNextHopIp(),
        equalTo(Ip.AUTO));
    assertThat(((ExitOutputIfaceStep) steps.get(2)).getAction(), is(StepAction.NULL_ROUTED));
  }

  @Test
  public void testFibLookupNextVrfLoop() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    String hostname = c.getHostname();
    Vrf.Builder vb = nf.vrfBuilder().setOwner(c);
    Vrf vrf1 = vb.build();
    String vrf1Name = vrf1.getName();
    Vrf vrf2 = vb.build();
    String vrf2Name = vrf2.getName();
    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("1.1.1.1"))
            .setIngressNode(c.getHostname())
            .setIngressVrf(vrf1Name)
            .setTag("tag")
            .build();
    Ip dstIp = flow.getDstIp();
    StaticRoute vrf1NextVrfRoute =
        StaticRoute.builder().setAdmin(1).setNetwork(Prefix.ZERO).setNextVrf(vrf2Name).build();
    StaticRoute vrf2NextVrfRoute =
        StaticRoute.builder().setAdmin(1).setNetwork(Prefix.ZERO).setNextVrf(vrf1Name).build();
    Fib fib1 =
        MockFib.builder()
            .setFibEntries(
                ImmutableMap.of(
                    dstIp,
                    ImmutableSet.of(
                        new FibEntry(
                            new FibNextVrf(vrf2Name), ImmutableList.of(vrf1NextVrfRoute)))))
            .build();
    Fib fib2 =
        MockFib.builder()
            .setFibEntries(
                ImmutableMap.of(
                    dstIp,
                    ImmutableSet.of(
                        new FibEntry(
                            new FibNextVrf(vrf1Name), ImmutableList.of(vrf2NextVrfRoute)))))
            .build();
    List<TraceAndReverseFlow> traces = new ArrayList<>();
    TracerouteEngineImplContext ctxt =
        new TracerouteEngineImplContext(
            MockDataPlane.builder().setConfigs(ImmutableMap.of(c.getHostname(), c)).build(),
            Topology.EMPTY,
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableMap.of(hostname, ImmutableMap.of(vrf1Name, fib1, vrf2Name, fib2)),
            false);
    FlowTracer flowTracer = initialFlowTracer(ctxt, hostname, null, flow, traces::add);
    flowTracer.fibLookup(dstIp, hostname, fib1);

    // Should be delegated from fib1 to fib2 and then looped back to fib1
    assertThat(traces, contains(hasTrace(hasDisposition(LOOP))));

    List<Hop> hops = traces.get(0).getTrace().getHops();

    // There should be a single hop
    assertThat(hops, hasSize(1));

    List<Step<?>> steps = hops.iterator().next().getSteps();

    // There should be 2 routing steps, with no action due to loop.
    // - next-vr should occur in the first step
    // - next-vr should occur in the second step

    assertThat(steps, contains(instanceOf(RoutingStep.class), instanceOf(RoutingStep.class)));
    assertThat(
        ((RoutingStep) steps.get(0)).getDetail().getRoutes().get(0).getNextVrf(),
        equalTo(vrf2Name));
    assertThat(
        ((RoutingStep) steps.get(1)).getDetail().getRoutes().get(0).getNextVrf(),
        equalTo(vrf1Name));
  }

  /**
   * Test that {@link FlowTracer#eval(Transformation transformation)} evaluates the transformation
   * using the {@link IpSpace IpSpaces} defined on the current node.
   */
  @Test
  public void testTransformationEvaluatorNode() {
    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("2.2.2.2");
    Ip ip3 = Ip.parse("3.3.3.3");

    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    Configuration c1 = cb.build();
    c1.setIpSpaces(ImmutableSortedMap.of("ips", ip1.toIpSpace()));

    Configuration c2 = cb.build();
    c2.setIpSpaces(ImmutableSortedMap.of("ips", ip2.toIpSpace()));

    Transformation transformation =
        when(matchDst(new IpSpaceReference("ips"))).apply(assignDestinationIp(ip3, ip3)).build();

    TracerouteEngineImplContext ctxt =
        new TracerouteEngineImplContext(
            MockDataPlane.builder()
                .setConfigs(ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2))
                .build(),
            Topology.EMPTY,
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableMap.of(),
            false);
    Flow.Builder fb =
        Flow.builder()
            .setIngressNode(c1.getHostname())
            .setIngressVrf(Configuration.DEFAULT_VRF_NAME)
            .setTag("tag");

    // 1. evaluate dstIp=ip1 on c1. Should be transformed to ip3
    {
      Flow flow = fb.setDstIp(ip1).build();
      FlowTracer flowTracer = initialFlowTracer(ctxt, c1.getHostname(), null, flow, tarf -> {});
      assertThat(flowTracer.eval(transformation).getOutputFlow().getDstIp(), equalTo(ip3));
    }

    // 2. evaluate dstIp=ip2 on c1. Should not be transformed.
    {
      Flow flow = fb.setDstIp(ip2).build();
      FlowTracer flowTracer = initialFlowTracer(ctxt, c1.getHostname(), null, flow, tarf -> {});
      assertThat(flowTracer.eval(transformation).getOutputFlow().getDstIp(), equalTo(ip2));
    }

    // 3. evaluate dstIp=ip1 after forking to c2. Should not be transformed
    {
      Flow flow = fb.setDstIp(ip1).build();
      FlowTracer flowTracer = initialFlowTracer(ctxt, c1.getHostname(), null, flow, tarf -> {});
      flowTracer =
          flowTracer.forkTracer(c2, null, new ArrayList<>(), null, Configuration.DEFAULT_VRF_NAME);
      assertThat(flowTracer.eval(transformation).getOutputFlow().getDstIp(), equalTo(ip1));
    }

    // 4. evaluate dstIp=ip2 after forking to c2. Should be transformed to ip3
    {
      Flow flow = fb.setDstIp(ip2).build();
      FlowTracer flowTracer = initialFlowTracer(ctxt, c1.getHostname(), null, flow, tarf -> {});
      flowTracer =
          flowTracer.forkTracer(c2, null, new ArrayList<>(), null, Configuration.DEFAULT_VRF_NAME);
      assertThat(flowTracer.eval(transformation).getOutputFlow().getDstIp(), equalTo(ip3));
    }
  }
}
