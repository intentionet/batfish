package org.batfish.dataplane.traceroute;

import static java.util.Collections.singletonList;
import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.DELIVERED_TO_SUBNET;
import static org.batfish.datamodel.FlowDisposition.DENIED_IN;
import static org.batfish.datamodel.FlowDisposition.DENIED_OUT;
import static org.batfish.datamodel.FlowDisposition.EXITS_NETWORK;
import static org.batfish.datamodel.FlowDisposition.INSUFFICIENT_INFO;
import static org.batfish.datamodel.FlowDisposition.LOOP;
import static org.batfish.datamodel.FlowDisposition.NEIGHBOR_UNREACHABLE;
import static org.batfish.datamodel.FlowDisposition.NO_ROUTE;
import static org.batfish.datamodel.matchers.TraceMatchers.hasDisposition;
import static org.batfish.dataplane.traceroute.TracerouteUtils.getFinalActionForDisposition;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DestinationNat;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.EnterInputIfaceStep;
import org.batfish.datamodel.flow.ExitOutputIfaceStep;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.OriginateStep;
import org.batfish.datamodel.flow.PreSourceNatOutgoingFilterStep;
import org.batfish.datamodel.flow.PreSourceNatOutgoingFilterStep.PreSourceNatOutgoingFilterStepDetail;
import org.batfish.datamodel.flow.RouteInfo;
import org.batfish.datamodel.flow.RoutingStep;
import org.batfish.datamodel.flow.Step;
import org.batfish.datamodel.flow.StepAction;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.matchers.TraceMatchers;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link TracerouteEngineImpl} */
public class TracerouteEngineImplTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Rule public TemporaryFolder _tempFolder = new TemporaryFolder();

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
    return IpAccessList.builder().setName(name).setLines(singletonList(aclLine)).build();
  }

  @Test
  public void testApplyDestinationNatSingleAclMatch() {
    Flow flow = makeFlow();

    DestinationNat nat =
        DestinationNat.builder()
            .setAcl(makeAcl("accept", LineAction.PERMIT))
            .setPoolIpFirst(new Ip("4.5.6.7"))
            .build();

    Flow transformed =
        TracerouteEngineImplContext.applyDestinationNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed.getDstIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplyDestinationNatSingleAclNoMatch() {
    Flow flow = makeFlow();

    DestinationNat nat =
        DestinationNat.builder()
            .setAcl(makeAcl("reject", LineAction.DENY))
            .setPoolIpFirst(new Ip("4.5.6.7"))
            .build();

    Flow transformed =
        TracerouteEngineImplContext.applyDestinationNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed, is(flow));
  }

  @Test
  public void testApplyDestinationNatFirstMatchWins() {
    Flow flow = makeFlow();

    DestinationNat nat =
        DestinationNat.builder()
            .setAcl(makeAcl("firstAccept", LineAction.PERMIT))
            .setPoolIpFirst(new Ip("4.5.6.7"))
            .build();

    DestinationNat secondNat =
        DestinationNat.builder()
            .setAcl(makeAcl("secondAccept", LineAction.PERMIT))
            .setPoolIpFirst(new Ip("4.5.6.8"))
            .build();

    Flow transformed =
        TracerouteEngineImplContext.applyDestinationNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getDstIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplyDestinationNatLateMatchWins() {
    Flow flow = makeFlow();

    DestinationNat nat =
        DestinationNat.builder()
            .setAcl(makeAcl("rejectAll", LineAction.DENY))
            .setPoolIpFirst(new Ip("4.5.6.7"))
            .build();

    DestinationNat secondNat =
        DestinationNat.builder()
            .setAcl(makeAcl("acceptAnyway", LineAction.PERMIT))
            .setPoolIpFirst(new Ip("4.5.6.8"))
            .build();

    Flow transformed =
        TracerouteEngineImplContext.applyDestinationNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getDstIp(), equalTo(new Ip("4.5.6.8")));
  }

  @Test
  public void testApplySourceNatSingleAclMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("accept", LineAction.PERMIT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatSingleAclNoMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("reject", LineAction.DENY));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed, is(flow));
  }

  @Test
  public void testApplySourceNatFirstMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("firstAccept", LineAction.PERMIT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("secondAccept", LineAction.PERMIT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatLateMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("rejectAll", LineAction.DENY));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("acceptAnyway", LineAction.PERMIT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.8")));
  }

  /*
   * iface1 and iface2 are interfaces on the same node. Send traffic with dstIp=iface2's ip to
   * iface1. Should accept if and only if iface1 and iface2 are in the same VRF.
   */
  @Test
  public void testAcceptVrf() throws IOException {
    // Construct network
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration config = cb.build();
    Vrf.Builder vb = nf.vrfBuilder().setOwner(config);
    Interface.Builder ib = nf.interfaceBuilder().setOwner(config);

    Vrf vrf1 = vb.build();
    Vrf vrf2 = vb.build();

    Interface i1 = ib.setVrf(vrf1).setAddress(new InterfaceAddress("1.1.1.1/24")).build();
    Interface i2 = ib.setVrf(vrf2).setAddress(new InterfaceAddress("2.2.2.2/24")).build();
    ib.setVrf(vrf2).setAddress(new InterfaceAddress("3.3.3.3/24")).build();

    // Compute data plane
    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(config.getHostname(), config);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    // Construct flows
    Flow.Builder fb =
        Flow.builder()
            .setDstIp(new Ip("3.3.3.3"))
            .setIngressNode(config.getHostname())
            .setTag("TAG");

    Flow flow1 = fb.setIngressInterface(i1.getName()).setIngressVrf(vrf1.getName()).build();
    Flow flow2 = fb.setIngressInterface(i2.getName()).setIngressVrf(vrf2.getName()).build();

    // Compute flow traces
    SortedMap<Flow, List<Trace>> traces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow1, flow2), dp.getFibs(), false);

    assertThat(traces, hasEntry(equalTo(flow1), contains(hasDisposition(NO_ROUTE))));
    assertThat(traces, hasEntry(equalTo(flow2), contains(hasDisposition(ACCEPTED))));
  }

  @Test
  public void testArpMultipleAccess() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Interface.Builder ib = nf.interfaceBuilder().setProxyArp(true);

    Configuration source = cb.build();
    Vrf vSource = vb.setOwner(source).build();
    ib.setOwner(source).setVrf(vSource).setAddress(new InterfaceAddress("10.0.0.1/24")).build();

    Configuration dst = cb.build();
    Vrf vDst = vb.setOwner(dst).build();
    ib.setOwner(dst).setVrf(vDst).setAddress(new InterfaceAddress("10.0.0.2/24")).build();

    Configuration other = cb.build();
    Vrf vOther = vb.setOwner(other).build();
    ib.setOwner(other).setVrf(vOther).setAddress(new InterfaceAddress("10.0.0.3/24")).build();

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.<String, Configuration>naturalOrder()
            .put(source.getHostname(), source)
            .put(dst.getHostname(), dst)
            .put(other.getHostname(), other)
            .build();
    Batfish batfish = BatfishTestUtils.getBatfish(configurations, _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();
    Flow flow =
        Flow.builder()
            .setIngressNode(source.getHostname())
            .setSrcIp(new Ip("10.0.0.1"))
            .setDstIp(new Ip("10.0.0.2"))
            .setTag("tag")
            .build();
    List<Trace> traces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow), dp.getFibs(), false)
            .get(flow);

    /*
     *  Since the 'other' neighbor should not respond to ARP:
     *  - There should only be one trace, ending at 'dst'.
     *  - It should be accepting.
     */
    assertThat(traces, Matchers.contains(TraceMatchers.hasDisposition(ACCEPTED)));
  }

  @Test
  public void testAclBeforeArpNoEdge() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c = cb.build();
    Vrf v = nf.vrfBuilder().setOwner(c).setName(Configuration.DEFAULT_VRF_NAME).build();
    // denies everything
    IpAccessList outgoingFilter =
        nf.aclBuilder().setOwner(c).setName("outgoingAcl").setLines(ImmutableList.of()).build();
    nf.interfaceBuilder()
        .setOwner(c)
        .setVrf(v)
        .setAddress(new InterfaceAddress("1.0.0.0/24"))
        .setOutgoingFilter(outgoingFilter)
        .build();
    SortedMap<String, Configuration> configurations = ImmutableSortedMap.of(c.getHostname(), c);
    Batfish b = BatfishTestUtils.getBatfish(configurations, _tempFolder);
    // make batfish call the new traceroute engine

    b.computeDataPlane(false);
    Flow flow =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setDstIp(new Ip("1.0.0.1"))
            .setTag("tag")
            .build();
    SortedMap<Flow, List<Trace>> flowTraces = b.buildFlows(ImmutableSet.of(flow), false);
    Trace trace = flowTraces.get(flow).iterator().next();

    /* Flow should be blocked by ACL before ARP, which would otherwise result in unreachable neighbor */
    assertThat(trace.getDisposition(), equalTo(FlowDisposition.DENIED_OUT));
  }

  @Test
  public void testAclBeforeArpWithEdge() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Interface.Builder ib = nf.interfaceBuilder();

    // c1
    Configuration c1 = cb.build();
    Vrf v1 = vb.setOwner(c1).build();
    // denies everything
    IpAccessList outgoingFilter =
        nf.aclBuilder().setOwner(c1).setName("outgoingAcl").setLines(ImmutableList.of()).build();
    ib.setOwner(c1)
        .setVrf(v1)
        .setOutgoingFilter(outgoingFilter)
        .setAddress(new InterfaceAddress("1.0.0.0/24"))
        .build();

    // c2
    Configuration c2 = cb.build();
    Vrf v2 = vb.setOwner(c2).build();
    ib.setOwner(c2)
        .setVrf(v2)
        .setOutgoingFilter(null)
        .setAddress(new InterfaceAddress("1.0.0.3/24"))
        .build();

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    Batfish b = BatfishTestUtils.getBatfish(configurations, _tempFolder);

    // make batfish call the new traceroute engine
    b.computeDataPlane(false);
    Flow flow =
        Flow.builder()
            .setIngressNode(c1.getHostname())
            .setTag("tag")
            .setDstIp(new Ip("1.0.0.1"))
            .build();
    SortedMap<Flow, List<Trace>> flowTraces = b.buildFlows(ImmutableSet.of(flow), false);
    Trace trace = flowTraces.get(flow).iterator().next();

    /* Flow should be blocked by ACL before ARP, which would otherwise result in unreachable neighbor */
    assertThat(trace.getDisposition(), equalTo(FlowDisposition.DENIED_OUT));
  }

  /** Tests ingressInterface with an incoming ACL. */
  @Test
  public void testDeniedInVsAccept() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Interface.Builder ib =
        nf.interfaceBuilder()
            .setOwner(c)
            .setVrf(nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(c).build());

    // This interface has no incoming filter.
    Interface ifaceAllowIn = ib.setAddress(new InterfaceAddress("2.0.0.2/24")).build();

    // This interface has an incoming filter that denies everything.
    Interface ifaceDenyIn =
        ib.setIncomingFilter(
                nf.aclBuilder().setOwner(c).setName("in").setLines(ImmutableList.of()).build())
            .setAddress(new InterfaceAddress("1.0.0.1/24"))
            .build();

    Batfish b = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(c.getHostname(), c), _tempFolder);
    b.computeDataPlane(false);

    Flow.Builder fb =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setTag("denied")
            .setDstIp(ifaceDenyIn.getAddress().getIp());

    Flow flowDenied = fb.setIngressInterface(ifaceDenyIn.getName()).build();
    Flow flowAllowed = fb.setIngressInterface(ifaceAllowIn.getName()).build();

    SortedMap<Flow, List<Trace>> flowTraces =
        b.buildFlows(ImmutableSet.of(flowDenied, flowAllowed), false);

    /* Flow coming in through ifaceDenyIn should be blocked by ACL. */
    Trace trace = Iterables.getOnlyElement(flowTraces.get(flowDenied));
    assertThat(trace.getDisposition(), equalTo(FlowDisposition.DENIED_IN));

    /* Flow coming in through ifaceAllowIn should be allowed in and then accepted. */
    trace = Iterables.getOnlyElement(flowTraces.get(flowAllowed));
    assertThat(trace.getDisposition(), equalTo(FlowDisposition.ACCEPTED));
  }

  /** When ingress node is non-existent, don't crash with null-pointer. */
  @Test
  public void testTracerouteOutsideNetwork() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Batfish batfish =
        BatfishTestUtils.getBatfish(ImmutableSortedMap.of(c1.getHostname(), c1), _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();
    Set<Flow> flows =
        ImmutableSet.of(Flow.builder().setTag("tag").setIngressNode("missingNode").build());
    Map<String, Map<String, Fib>> fibs = dp.getFibs();

    _thrown.expect(IllegalArgumentException.class);
    TracerouteEngineImpl.getInstance().buildFlows(dp, flows, fibs, false);
  }

  /*
   * Create a network with a forwarding loop. When we run with ACLs enabled, the loop is detected.
   * When we run with ACLs enabled, it's not an infinite loop: we apply source NAT in the first
   * iteration, and drop with an ingress ACL in the second iteration.
   */
  @Test
  public void testLoop() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Vrf v1 = nf.vrfBuilder().setOwner(c1).build();
    InterfaceAddress c1Addr = new InterfaceAddress("1.0.0.0/31");
    InterfaceAddress c2Addr = new InterfaceAddress("1.0.0.1/31");
    Interface i1 =
        nf.interfaceBuilder().setActive(true).setOwner(c1).setVrf(v1).setAddress(c1Addr).build();
    Prefix loopPrefix = Prefix.parse("2.0.0.0/32");
    v1.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(loopPrefix)
                .setAdministrativeCost(1)
                .setNextHopInterface(i1.getName())
                .setNextHopIp(c2Addr.getIp())
                .build()));
    Configuration c2 = cb.build();
    Vrf v2 = nf.vrfBuilder().setOwner(c2).build();
    Interface i2 =
        nf.interfaceBuilder().setActive(true).setOwner(c2).setVrf(v2).setAddress(c2Addr).build();
    Prefix natPoolIp = Prefix.parse("5.5.5.5/32");
    i2.setIncomingFilter(
        nf.aclBuilder()
            .setOwner(c2)
            .setLines(
                ImmutableList.of(IpAccessListLine.rejecting(AclLineMatchExprs.matchSrc(natPoolIp))))
            .build());
    i2.setSourceNats(
        ImmutableList.of(
            SourceNat.builder()
                .setAcl(
                    nf.aclBuilder()
                        .setOwner(c2)
                        .setLines(ImmutableList.of(IpAccessListLine.ACCEPT_ALL))
                        .build())
                .setPoolIpFirst(natPoolIp.getStartIp())
                .setPoolIpLast(natPoolIp.getStartIp())
                .build()));
    v2.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(loopPrefix)
                .setAdministrativeCost(1)
                .setNextHopInterface(i2.getName())
                .setNextHopIp(c1Addr.getIp())
                .build()));
    Batfish batfish =
        BatfishTestUtils.getBatfish(
            ImmutableSortedMap.of(c1.getHostname(), c1, c2.getHostname(), c2), _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();
    Flow flow =
        Flow.builder()
            .setTag("tag")
            .setIngressNode(c1.getHostname())
            .setIngressVrf(v1.getName())
            .setDstIp(loopPrefix.getStartIp())
            // any src Ip other than the NAT pool IP will do
            .setSrcIp(new Ip("6.6.6.6"))
            .build();
    SortedMap<Flow, List<Trace>> flowTraces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow), dp.getFibs(), false);
    assertThat(flowTraces.get(flow), contains(TraceMatchers.hasDisposition(DENIED_IN)));
    flowTraces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow), dp.getFibs(), true);
    assertThat(flowTraces.get(flow), contains(TraceMatchers.hasDisposition(LOOP)));
  }

  @Test
  public void testGetFinalActionForDisposition() {
    assertThat(
        getFinalActionForDisposition(DELIVERED_TO_SUBNET), equalTo(StepAction.DELIVERED_TO_SUBNET));
    assertThat(getFinalActionForDisposition(EXITS_NETWORK), equalTo(StepAction.EXITS_NETWORK));
    assertThat(
        getFinalActionForDisposition(INSUFFICIENT_INFO), equalTo(StepAction.INSUFFICIENT_INFO));
    assertThat(
        getFinalActionForDisposition(NEIGHBOR_UNREACHABLE),
        equalTo(StepAction.NEIGHBOR_UNREACHABLE));
  }

  @Test
  public void testApplyPreSourceNatFilter() {
    String node = "node";
    String iface1 = "iface1";
    String iface2 = "iface2";
    String prefix = "1.2.3.4/24";
    String filterName = "preSourceFilter";

    IpAccessList filter =
        IpAccessList.builder()
            .setName(filterName)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting(
                        AclLineMatchExprs.and(
                            new MatchSrcInterface(ImmutableList.of(iface1)),
                            AclLineMatchExprs.matchSrc(Prefix.parse(prefix)))),
                    IpAccessListLine.rejecting(
                        AclLineMatchExprs.and(
                            new MatchSrcInterface(ImmutableList.of(iface2)),
                            AclLineMatchExprs.matchSrc(Prefix.parse(prefix))))))
            .build();

    Flow flow = makeFlow();

    PreSourceNatOutgoingFilterStep step =
        TracerouteEngineImplContext.applyPreSourceNatFilter(
            flow,
            node,
            iface1,
            iface2,
            filter,
            ImmutableMap.of(filterName, filter),
            ImmutableMap.of(),
            false);

    assertThat(step.getAction(), equalTo(StepAction.PERMITTED));

    PreSourceNatOutgoingFilterStepDetail detail = step.getDetail();
    assertThat(detail.getFilter(), equalTo(filterName));
    assertThat(detail.getOutputInterface(), equalTo(iface2));
    assertThat(detail.getNode(), equalTo(node));

    step =
        TracerouteEngineImplContext.applyPreSourceNatFilter(
            flow,
            node,
            iface2,
            iface1,
            filter,
            ImmutableMap.of(filterName, filter),
            ImmutableMap.of(),
            false);

    assertThat(step.getAction(), equalTo(StepAction.DENIED));

    detail = step.getDetail();
    assertThat(detail.getFilter(), equalTo(filterName));
    assertThat(detail.getOutputInterface(), equalTo(iface1));
    assertThat(detail.getNode(), equalTo(node));
  }

  @Test
  public void testPreSourceNatFilter() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Vrf v1 = nf.vrfBuilder().setOwner(c1).build();
    InterfaceAddress i1Addr = new InterfaceAddress("1.0.0.1/31");
    InterfaceAddress i2Addr = new InterfaceAddress("2.0.0.1/31");
    Interface i1 =
        nf.interfaceBuilder().setActive(true).setOwner(c1).setVrf(v1).setAddress(i1Addr).build();
    Interface i2 =
        nf.interfaceBuilder().setActive(true).setOwner(c1).setVrf(v1).setAddress(i2Addr).build();
    v1.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("0.0.0.0/0"))
                .setAdministrativeCost(1)
                .setNextHopInterface(i2.getName())
                .build()));

    String filterName = "preSourceFilter";

    IpAccessList filter =
        IpAccessList.builder()
            .setName(filterName)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting(
                        AclLineMatchExprs.and(
                            new MatchSrcInterface(ImmutableList.of(i1.getName())),
                            AclLineMatchExprs.matchSrc(Prefix.parse("10.0.0.1/32"))))))
            .build();

    c1.getIpAccessLists().put(filterName, filter);
    i2.setPreSourceNatOutgoingFilter(filter);

    Batfish batfish =
        BatfishTestUtils.getBatfish(ImmutableSortedMap.of(c1.getHostname(), c1), _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();
    Flow flow =
        Flow.builder()
            .setTag("tag")
            .setIngressNode(c1.getHostname())
            .setIngressVrf(v1.getName())
            .setIngressInterface(i1.getName())
            .setSrcIp(new Ip("10.0.0.1"))
            .setDstIp(new Ip("20.6.6.6"))
            .build();
    SortedMap<Flow, List<Trace>> flowTraces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow), dp.getFibs(), false);
    List<Trace> traceList = flowTraces.get(flow);
    assertThat(traceList, contains(TraceMatchers.hasDisposition(EXITS_NETWORK)));
    assertThat(traceList, hasSize(1));
    List<Hop> hops = traceList.get(0).getHops();
    assertThat(hops, hasSize(1));
    List<Step<?>> steps = hops.get(0).getSteps();
    // should have ingress acl -> routing -> presourcenat acl -> egress acl
    assertThat(steps, hasSize(4));

    assertThat(steps.get(0), instanceOf(EnterInputIfaceStep.class));
    assertThat(steps.get(1), instanceOf(RoutingStep.class));
    assertThat(steps.get(2), instanceOf(PreSourceNatOutgoingFilterStep.class));
    assertThat(steps.get(3), instanceOf(ExitOutputIfaceStep.class));

    EnterInputIfaceStep step0 = (EnterInputIfaceStep) steps.get(0);
    assertThat(step0.getAction(), equalTo(StepAction.RECEIVED));
    assertThat(step0.getDetail().getInputFilter(), nullValue());
    assertThat(step0.getDetail().getInputVrf(), equalTo(v1.getName()));
    assertThat(
        step0.getDetail().getInputInterface(),
        equalTo(new NodeInterfacePair(c1.getHostname(), i1.getName())));

    RoutingStep step1 = (RoutingStep) steps.get(1);
    assertThat(step1.getAction(), equalTo(StepAction.FORWARDED));
    assertThat(step1.getDetail().getRoutes(), hasSize(1));
    assertThat(
        step1.getDetail().getRoutes(),
        contains(new RouteInfo(RoutingProtocol.STATIC, Prefix.parse("0.0.0.0/0"), Ip.AUTO)));

    PreSourceNatOutgoingFilterStep step2 = (PreSourceNatOutgoingFilterStep) steps.get(2);
    assertThat(step2.getAction(), equalTo(StepAction.PERMITTED));
    assertThat(step2.getDetail().getNode(), equalTo(c1.getHostname()));
    assertThat(step2.getDetail().getOutputInterface(), equalTo(i2.getName()));
    assertThat(step2.getDetail().getFilter(), equalTo(filter.getName()));

    ExitOutputIfaceStep step3 = (ExitOutputIfaceStep) steps.get(3);
    assertThat(step3.getAction(), equalTo(StepAction.EXITS_NETWORK));
    assertThat(step3.getDetail().getOutputFilter(), nullValue());
    assertThat(
        step3.getDetail().getOutputInterface(),
        equalTo(new NodeInterfacePair(c1.getHostname(), i2.getName())));
    assertThat(step3.getDetail().getTransformedFlow(), nullValue());

    Flow flow2 =
        Flow.builder()
            .setTag("tag")
            .setIngressNode(c1.getHostname())
            .setIngressVrf(v1.getName())
            .setIngressInterface(i1.getName())
            .setSrcIp(new Ip("10.1.1.1"))
            .setDstIp(new Ip("20.6.6.6"))
            .build();
    flowTraces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow2), dp.getFibs(), false);
    assertThat(flowTraces.get(flow2), contains(TraceMatchers.hasDisposition(DENIED_OUT)));

    traceList = flowTraces.get(flow2);
    assertThat(traceList, hasSize(1));
    hops = traceList.get(0).getHops();
    assertThat(hops, hasSize(1));

    steps = hops.get(0).getSteps();
    assertThat(steps, hasSize(3));

    assertThat(steps.get(0), instanceOf(EnterInputIfaceStep.class));
    assertThat(steps.get(1), instanceOf(RoutingStep.class));
    assertThat(steps.get(2), instanceOf(PreSourceNatOutgoingFilterStep.class));

    step2 = (PreSourceNatOutgoingFilterStep) steps.get(2);
    assertThat(step2.getAction(), equalTo(StepAction.DENIED));

    flowTraces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow2), dp.getFibs(), true);
    assertThat(flowTraces.get(flow2), contains(TraceMatchers.hasDisposition(EXITS_NETWORK)));
  }

  @Test
  public void testPreSourceNatFilterOriginatingPackets() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Vrf v1 = nf.vrfBuilder().setOwner(c1).build();
    InterfaceAddress i1Addr = new InterfaceAddress("1.0.0.1/31");
    InterfaceAddress i2Addr = new InterfaceAddress("2.0.0.1/31");
    Interface i1 =
        nf.interfaceBuilder().setActive(true).setOwner(c1).setVrf(v1).setAddress(i1Addr).build();
    Interface i2 =
        nf.interfaceBuilder().setActive(true).setOwner(c1).setVrf(v1).setAddress(i2Addr).build();
    v1.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("0.0.0.0/0"))
                .setAdministrativeCost(1)
                .setNextHopInterface(i2.getName())
                .build()));

    String filterName = "preSourceFilter";

    IpAccessList filter =
        IpAccessList.builder()
            .setName(filterName)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.accepting(
                        AclLineMatchExprs.and(
                            new MatchSrcInterface(ImmutableList.of(i1.getName())),
                            AclLineMatchExprs.matchSrc(Prefix.parse("10.0.0.1/32")))),
                    new IpAccessListLine(
                        LineAction.PERMIT, OriginatingFromDevice.INSTANCE, "HOST_OUTBOUND")))
            .build();

    c1.getIpAccessLists().put(filterName, filter);
    i2.setPreSourceNatOutgoingFilter(filter);

    Batfish batfish =
        BatfishTestUtils.getBatfish(ImmutableSortedMap.of(c1.getHostname(), c1), _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    Flow flow =
        Flow.builder()
            .setTag("tag")
            .setIngressNode(c1.getHostname())
            .setIngressVrf(v1.getName())
            .setSrcIp(new Ip("1.0.0.1"))
            .setDstIp(new Ip("20.6.6.6"))
            .build();
    SortedMap<Flow, List<Trace>> flowTraces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow), dp.getFibs(), false);
    List<Trace> traceList = flowTraces.get(flow);
    assertThat(traceList, contains(TraceMatchers.hasDisposition(EXITS_NETWORK)));
    assertThat(traceList, hasSize(1));
    List<Hop> hops = traceList.get(0).getHops();
    assertThat(hops, hasSize(1));
    List<Step<?>> steps = hops.get(0).getSteps();
    // should have originated -> routing -> presourcenat acl -> egress acl
    assertThat(steps, hasSize(4));

    assertThat(steps.get(0), instanceOf(OriginateStep.class));
    assertThat(steps.get(1), instanceOf(RoutingStep.class));
    assertThat(steps.get(2), instanceOf(PreSourceNatOutgoingFilterStep.class));
    assertThat(steps.get(3), instanceOf(ExitOutputIfaceStep.class));

    assertThat(steps.get(0).getAction(), equalTo(StepAction.ORIGINATED));
    assertThat(steps.get(1).getAction(), equalTo(StepAction.FORWARDED));
    assertThat(steps.get(2).getAction(), equalTo(StepAction.PERMITTED));
    assertThat(steps.get(3).getAction(), equalTo(StepAction.EXITS_NETWORK));
  }
}
