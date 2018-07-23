package org.batfish.z3;

import static org.batfish.datamodel.FlowDisposition.DENIED_OUT;
import static org.batfish.datamodel.FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK;
import static org.batfish.datamodel.matchers.EdgeMatchers.hasInt2;
import static org.batfish.datamodel.matchers.FlowTraceHopMatchers.hasEdge;
import static org.batfish.datamodel.matchers.FlowTraceMatchers.hasDisposition;
import static org.batfish.datamodel.matchers.FlowTraceMatchers.hasHop;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.microsoft.z3.Context;
import com.microsoft.z3.Status;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.config.Settings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.ForwardingAnalysisImpl;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.TrueExpr;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NodJobAclTest {
  /**
   * Test MatchSrcInterface AclLineMatchExpr. Build a network with two nodes srcNode and dstNode.
   * Traffic will originate at srcNode and be sent to dstNode. The two nodes can communicate via two
   * subnets (two interfaces on each node), and dstNode has another interface on the destination
   * subnet. That destination interface has an outgoing filter that only accepts traffic from
   * srcNode if it arrived on one of the two interfaces dstNode can receive traffic from srcNode on.
   */
  @Test
  public void testMatchSrcInterface() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = nf.interfaceBuilder().setActive(true).setBandwidth(1E9d);
    IpAccessList.Builder aclb = nf.aclBuilder();
    Vrf.Builder vb = nf.vrfBuilder();

    String iface1 = "iface1";
    String iface2 = "iface2";

    Configuration srcNode = cb.build();
    Configuration dstNode = cb.build();
    Vrf srcVrf = vb.setOwner(srcNode).build();
    Vrf dstVrf = vb.setOwner(dstNode).build();

    // create the ACL that only accepts traffic that entered via iface1
    IpAccessList matchSrcInterfaceAcl =
        aclb.setLines(
                ImmutableList.of(
                    IpAccessListLine.builder()
                        .setAction(LineAction.ACCEPT)
                        .setMatchCondition(new MatchSrcInterface(ImmutableList.of(iface1)))
                        .build()))
            .setOwner(dstNode)
            .build();

    ib.setOwner(srcNode)
        .setVrf(srcVrf)
        .setAddresses(new InterfaceAddress(new Ip("1.0.0.0"), 8))
        .build();
    ib.setAddresses(new InterfaceAddress(new Ip("2.0.0.0"), 8)).build();

    // create iface1
    ib.setOwner(dstNode)
        .setVrf(dstVrf)
        .setName(iface1)
        .setAddresses(new InterfaceAddress(new Ip("1.0.0.1"), 8))
        .build();

    // create iface2
    ib.setOwner(dstNode)
        .setVrf(dstVrf)
        .setName(iface2)
        .setAddresses(new InterfaceAddress(new Ip("2.0.0.1"), 8))
        .build();

    // For the destination
    Prefix pDest = Prefix.parse("3.0.0.0/8");
    ib.setOwner(dstNode)
        .setVrf(dstVrf)
        .setName("iface3")
        .setAddresses(new InterfaceAddress(pDest.getEndIp(), pDest.getPrefixLength()))
        .setOutgoingFilter(matchSrcInterfaceAcl)
        .build();

    StaticRoute.Builder bld = StaticRoute.builder().setNetwork(pDest);
    srcVrf.getStaticRoutes().add(bld.setNextHopIp(new Ip("1.0.0.1")).build());
    srcVrf.getStaticRoutes().add(bld.setNextHopIp(new Ip("2.0.0.1")).build());

    ImmutableSortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(srcNode.getHostname(), srcNode, dstNode.getHostname(), dstNode);

    /* set up data plane */
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, tmp);
    batfish.computeDataPlane(false);
    DataPlane dataPlane = batfish.loadDataPlane();

    /* set up synthesizer */
    Topology topology = new Topology(dataPlane.getTopologyEdges());
    SynthesizerInput input =
        SynthesizerInputImpl.builder()
            .setConfigurations(configs)
            .setForwardingAnalysis(
                new ForwardingAnalysisImpl(
                    configs, dataPlane.getRibs(), dataPlane.getFibs(), topology))
            .setSimplify(false)
            .setTopology(topology)
            .build();
    Synthesizer synthesizer = new Synthesizer(input);

    /* Construct NodJob */
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setSrcIps(ImmutableList.of(new IpWildcard("1.1.1.1/32")))
            .setDstIps(ImmutableList.of(new IpWildcard("3.0.0.1/32")))
            .build();
    IngressLocation ingressLocation = IngressLocation.vrf(srcNode.getHostname(), srcVrf.getName());
    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        ImmutableMap.of(ingressLocation, TrueExpr.INSTANCE);
    StandardReachabilityQuerySynthesizer querySynthesizer =
        StandardReachabilityQuerySynthesizer.builder()
            .setActions(ImmutableSet.of(ForwardingAction.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK))
            .setFinalNodes(ImmutableSet.of(dstNode.getHostname()))
            .setHeaderSpace(headerSpace)
            .setSrcIpConstraints(srcIpConstraints)
            .build();
    NodJob nodJob =
        new NodJob(new Settings(), synthesizer, querySynthesizer, srcIpConstraints, "tag", false);

    /* Run query */
    Context z3Context = new Context();
    SmtInput smtInput = nodJob.computeSmtInput(System.currentTimeMillis(), z3Context);

    Map<IngressLocation, Map<String, Long>> ingressLocationConstraints =
        nodJob.getSolutionPerIngressLocation(z3Context, smtInput);
    assertThat(ingressLocationConstraints.entrySet(), hasSize(1));
    assertThat(ingressLocationConstraints, hasKey(ingressLocation));
    Map<String, Long> fieldConstraints = ingressLocationConstraints.get(ingressLocation);

    assertThat(
        fieldConstraints, hasEntry(IngressLocationInstrumentation.INGRESS_LOCATION_FIELD_NAME, 0L));
    assertThat(smtInput._variablesAsConsts, hasKey("SRC_IP"));
    assertThat(fieldConstraints, hasKey(Field.SRC_IP.getName()));

    assertThat(fieldConstraints, hasEntry(Field.ORIG_SRC_IP.getName(), new Ip("1.1.1.1").asLong()));
    assertThat(fieldConstraints, hasEntry(Field.SRC_IP.getName(), new Ip("1.1.1.1").asLong()));

    Set<Flow> flows = nodJob.getFlows(ingressLocationConstraints);
    DataPlanePlugin dpPlugin = batfish.getDataPlanePlugin();
    dpPlugin.processFlows(flows, dataPlane, false);
    List<FlowTrace> flowTraces = dpPlugin.getHistoryFlowTraces(dataPlane);
    assertThat(flowTraces, hasSize(2));
    assertThat(
        flowTraces,
        containsInAnyOrder(
            ImmutableList.of(
                /* One trace should enter dstNode through iface1 and then pass the outgoing filter,
                 * resulting in the NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK disposition.
                 * Specifically, the first hop should have an edge with int2=iface1.
                 */
                allOf(
                    hasDisposition(NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK),
                    hasHop(0, hasEdge(hasInt2(iface1)))),
                /* One trace should enter dstNode through iface2 and then be dropped by the outgoing
                 * filter, resulting in the DENIED_OUT disposition. The first hop should have an
                 * edge with int2=iface2.
                 */
                allOf(hasDisposition(DENIED_OUT), hasHop(0, hasEdge(hasInt2(iface2)))))));
  }

  /**
   * Test MatchSrcInterface AclLineMatchExpr with a one-node network, using OriginateInterfaceLink.
   */
  @Test
  public void testMatchSrcInterface_OriginateInterface() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = nf.interfaceBuilder().setActive(true).setBandwidth(1E9d);
    IpAccessList.Builder aclb = nf.aclBuilder();
    Vrf.Builder vb = nf.vrfBuilder();

    String iface1 = "iface1";
    String iface2 = "iface2";
    String iface3 = "iface3";

    Configuration node = cb.build();
    Vrf vrf = vb.setOwner(node).build();

    // create the ACL that only accepts traffic that entered via iface1
    IpAccessList matchSrcInterfaceAcl =
        aclb.setLines(
                ImmutableList.of(
                    IpAccessListLine.builder()
                        .setAction(LineAction.ACCEPT)
                        .setMatchCondition(new MatchSrcInterface(ImmutableList.of(iface1)))
                        .build()))
            .setOwner(node)
            .build();

    // create iface1
    ib.setOwner(node)
        .setVrf(vrf)
        .setName(iface1)
        .setAddresses(new InterfaceAddress(new Ip("1.0.0.1"), 8))
        .build();

    // create iface2
    ib.setOwner(node)
        .setVrf(vrf)
        .setName(iface2)
        .setAddresses(new InterfaceAddress(new Ip("2.0.0.1"), 8))
        .build();

    // For the destination
    Prefix pDest = Prefix.parse("3.0.0.0/8");
    ib.setOwner(node)
        .setVrf(vrf)
        .setName(iface3)
        .setAddresses(new InterfaceAddress(pDest.getEndIp(), pDest.getPrefixLength()))
        .setOutgoingFilter(matchSrcInterfaceAcl)
        .build();

    ImmutableSortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(node.getHostname(), node);

    /* set up data plane */
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, tmp);
    batfish.computeDataPlane(false);
    DataPlane dataPlane = batfish.loadDataPlane();

    /* set up synthesizer */
    Topology topology = new Topology(dataPlane.getTopologyEdges());
    SynthesizerInput input =
        SynthesizerInputImpl.builder()
            .setConfigurations(configs)
            .setForwardingAnalysis(
                new ForwardingAnalysisImpl(
                    configs, dataPlane.getRibs(), dataPlane.getFibs(), topology))
            .setSimplify(false)
            .setTopology(topology)
            .build();
    Synthesizer synthesizer = new Synthesizer(input);

    /* Construct NodJob */
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setSrcIps(ImmutableList.of(new IpWildcard("1.1.1.1/32")))
            .setDstIps(ImmutableList.of(new IpWildcard("3.0.0.1/32")))
            .build();

    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        ImmutableMap.of(
            IngressLocation.interfaceLink(node.getHostname(), iface1), TrueExpr.INSTANCE,
            IngressLocation.interfaceLink(node.getHostname(), iface2), TrueExpr.INSTANCE);

    StandardReachabilityQuerySynthesizer querySynthesizer =
        StandardReachabilityQuerySynthesizer.builder()
            .setActions(ImmutableSet.of(ForwardingAction.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK))
            .setFinalNodes(ImmutableSet.of(node.getHostname()))
            .setHeaderSpace(headerSpace)
            .setSrcIpConstraints(srcIpConstraints)
            .build();

    NodJob nodJob =
        new NodJob(new Settings(), synthesizer, querySynthesizer, srcIpConstraints, "tag", true);

    /* Run query */
    Context z3Context = new Context();
    SmtInput smtInput = nodJob.computeSmtInput(System.currentTimeMillis(), z3Context);

    Map<IngressLocation, Map<String, Long>> ingressLocationConstraints =
        nodJob.getSolutionPerIngressLocation(z3Context, smtInput);
    assertThat(ingressLocationConstraints.entrySet(), hasSize(1));
    IngressLocation ingressLocation = IngressLocation.interfaceLink(node.getHostname(), iface1);
    assertThat(ingressLocationConstraints, hasKey(ingressLocation));
    Map<String, Long> fieldConstraints = ingressLocationConstraints.get(ingressLocation);

    assertThat(
        fieldConstraints, hasEntry(IngressLocationInstrumentation.INGRESS_LOCATION_FIELD_NAME, 0L));
    assertThat(smtInput._variablesAsConsts, hasKey("SRC_IP"));
    assertThat(fieldConstraints, hasKey(Field.SRC_IP.getName()));

    assertThat(fieldConstraints, hasEntry(Field.ORIG_SRC_IP.getName(), new Ip("1.1.1.1").asLong()));
    assertThat(fieldConstraints, hasEntry(Field.SRC_IP.getName(), new Ip("1.1.1.1").asLong()));

    Set<Flow> flows = nodJob.getFlows(ingressLocationConstraints);
    DataPlanePlugin dpPlugin = batfish.getDataPlanePlugin();
    dpPlugin.processFlows(flows, dataPlane, false);
    List<FlowTrace> flowTraces = dpPlugin.getHistoryFlowTraces(dataPlane);
    assertThat(flowTraces, hasSize(1));
    assertThat(
        flowTraces,
        contains(
            /* The trace should originate at iface1 and then pass the outgoing filter,
             * resulting in the NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK disposition.
             */
            allOf(
                hasDisposition(NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK),
                hasHop(0, hasEdge(hasInt2(iface1))))));
  }

  @Test
  public void testPermittedByAcl_accept() throws IOException {
    testPermittedByAcl_helper(LineAction.ACCEPT);
  }

  @Test
  public void testPermittedByAcl_reject() throws IOException {
    testPermittedByAcl_helper(LineAction.REJECT);
  }

  private static void testPermittedByAcl_helper(LineAction action) throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = nf.interfaceBuilder().setActive(true).setBandwidth(1E9d);
    Vrf.Builder vb = nf.vrfBuilder();
    IpAccessList.Builder aclb = nf.aclBuilder();

    IpWildcard srcIp = new IpWildcard("4.4.4.4/32");

    Configuration node = cb.build();
    Vrf vrf = vb.setOwner(node).build();

    // setup the ACL to be referred to from the outgoing filter
    aclb.setLines(
            ImmutableList.of(
                IpAccessListLine.builder()
                    .setAction(action)
                    .setMatchCondition(
                        new MatchHeaderSpace(
                            HeaderSpace.builder().setSrcIps(ImmutableList.of(srcIp)).build()))
                    .build()))
        .setName("acl1")
        .setOwner(node)
        .build();

    IpAccessList outgoingFilter =
        aclb.setName("acl2")
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.builder()
                        .setAction(LineAction.ACCEPT)
                        .setMatchCondition(new PermittedByAcl("acl1"))
                        .build()))
            .build();

    ib.setOwner(node)
        .setVrf(vrf)
        .setAddresses(new InterfaceAddress(new Ip("1.0.0.0"), 8))
        .setOutgoingFilter(outgoingFilter)
        .build();

    ImmutableSortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(node.getHostname(), node);

    /* set up data plane */
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, tmp);
    batfish.computeDataPlane(false);
    DataPlane dataPlane = batfish.loadDataPlane();

    /* set up synthesizer */
    Topology topology = new Topology(dataPlane.getTopologyEdges());
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setSrcIps(ImmutableList.of(srcIp))
            .setDstIps(ImmutableList.of(new IpWildcard("1.2.3.4/32")))
            .build();
    SynthesizerInput input =
        SynthesizerInputImpl.builder()
            .setConfigurations(configs)
            .setForwardingAnalysis(
                new ForwardingAnalysisImpl(
                    configs, dataPlane.getRibs(), dataPlane.getFibs(), topology))
            .setSimplify(true)
            .setSpecialize(false)
            .setHeaderSpace(headerSpace)
            .setTopology(topology)
            .build();
    Synthesizer synthesizer = new Synthesizer(input);

    /* Construct NodJob */
    IngressLocation ingressLocation = IngressLocation.vrf(node.getHostname(), vrf.getName());
    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        ImmutableMap.of(ingressLocation, TrueExpr.INSTANCE);
    StandardReachabilityQuerySynthesizer querySynthesizer =
        StandardReachabilityQuerySynthesizer.builder()
            .setActions(ImmutableSet.of(ForwardingAction.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK))
            .setHeaderSpace(headerSpace)
            .setSrcIpConstraints(srcIpConstraints)
            .build();

    NodJob nodJob =
        new NodJob(new Settings(), synthesizer, querySynthesizer, srcIpConstraints, "tag", true);

    /* Run query */
    Status status = NodJobTest.checkSat(nodJob);
    assertThat(
        status, equalTo(action == LineAction.ACCEPT ? Status.SATISFIABLE : Status.UNSATISFIABLE));
  }
}
