package org.batfish.bddreachability;

import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.DELIVERED_TO_SUBNET;
import static org.batfish.datamodel.FlowDisposition.DENIED_IN;
import static org.batfish.datamodel.FlowDisposition.DENIED_OUT;
import static org.batfish.datamodel.FlowDisposition.EXITS_NETWORK;
import static org.batfish.datamodel.FlowDisposition.INSUFFICIENT_INFO;
import static org.batfish.datamodel.FlowDisposition.NEIGHBOR_UNREACHABLE;
import static org.batfish.datamodel.FlowDisposition.NO_ROUTE;
import static org.batfish.datamodel.FlowDisposition.NULL_ROUTED;
import static org.batfish.z3.expr.NodeInterfaceNeighborUnreachableMatchers.hasHostname;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TraceWrapperAsAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.loop.LoopNetwork;
import org.batfish.specifier.AllInterfacesLocationSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationSpecifiers;
import org.batfish.specifier.SpecifierContext;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.DropAclIn;
import org.batfish.z3.state.DropAclOut;
import org.batfish.z3.state.DropNoRoute;
import org.batfish.z3.state.DropNullRoute;
import org.batfish.z3.state.NodeAccept;
import org.batfish.z3.state.NodeDropAclIn;
import org.batfish.z3.state.NodeDropAclOut;
import org.batfish.z3.state.NodeDropNoRoute;
import org.batfish.z3.state.NodeDropNullRoute;
import org.batfish.z3.state.NodeInterfaceNeighborUnreachableOrExitsNetwork;
import org.batfish.z3.state.OriginateInterfaceLink;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOutEdgePostNat;
import org.batfish.z3.state.Query;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class BDDReachabilityAnalysisFactoryTest {
  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private static final BDDPacket PKT = new BDDPacket();
  private static final IpSpaceSpecifier CONSTANT_UNIVERSE_IPSPACE_SPECIFIER =
      new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE);
  private static final BDD ONE = PKT.getFactory().one();

  private static final Set<FlowDisposition> ALL_DISPOSITIONS =
      ImmutableSet.of(
          ACCEPTED,
          DENIED_IN,
          DENIED_OUT,
          NO_ROUTE,
          NULL_ROUTED,
          DELIVERED_TO_SUBNET,
          EXITS_NETWORK,
          NEIGHBOR_UNREACHABLE,
          INSUFFICIENT_INFO);

  private static IpSpaceAssignment ipSpaceAssignment(Batfish batfish) {
    SpecifierContext ctxt = batfish.specifierContext();
    Set<Location> locations = LocationSpecifiers.ALL_LOCATIONS.resolve(ctxt);
    return CONSTANT_UNIVERSE_IPSPACE_SPECIFIER.resolve(locations, ctxt);
  }

  @Test
  public void testBDDFactory() throws IOException {
    TestNetworkIndirection net = new TestNetworkIndirection();
    Batfish batfish = BatfishTestUtils.getBatfish(net._configs, temp);
    batfish.computeDataPlane(false);
    DataPlane dataPlane = batfish.loadDataPlane();

    // Confirm factory building does not throw, even with IpSpace and ACL indirection
    new BDDReachabilityAnalysisFactory(PKT, net._configs, dataPlane.getForwardingAnalysis());
  }

  @Test
  public void testFinalNodes() throws IOException {
    SortedMap<String, Configuration> configs = TestNetworkSources.twoNodeNetwork();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, temp);
    batfish.computeDataPlane(false);
    DataPlane dataPlane = batfish.loadDataPlane();

    assertThat(configs.size(), equalTo(2));
    for (String node : configs.keySet()) {
      String otherNode = configs.keySet().stream().filter(n -> !n.equals(node)).findFirst().get();
      Map<StateExpr, Map<StateExpr, Edge>> edges =
          new BDDReachabilityAnalysisFactory(PKT, configs, dataPlane.getForwardingAnalysis())
              .bddReachabilityAnalysis(
                  ipSpaceAssignment(batfish),
                  UniverseIpSpace.INSTANCE,
                  ImmutableSet.of(),
                  ImmutableSet.of(),
                  ImmutableSet.of(node),
                  ALL_DISPOSITIONS)
              .getEdges();
      assertThat(edges, hasEntry(equalTo(new NodeAccept(node)), hasKey(Accept.INSTANCE)));
      assertThat(edges, not(hasEntry(equalTo(new NodeAccept(otherNode)), hasKey(Accept.INSTANCE))));

      assertThat(edges, hasEntry(equalTo(new NodeDropAclIn(node)), hasKey(DropAclIn.INSTANCE)));
      assertThat(
          edges, not(hasEntry(equalTo(new NodeDropAclIn(otherNode)), hasKey(DropAclIn.INSTANCE))));

      assertThat(edges, hasEntry(equalTo(new NodeDropAclOut(node)), hasKey(DropAclOut.INSTANCE)));
      assertThat(
          edges,
          not(hasEntry(equalTo(new NodeDropAclOut(otherNode)), hasKey(DropAclOut.INSTANCE))));

      Set<NodeInterfaceNeighborUnreachableOrExitsNetwork> neighborUnreachables =
          edges
              .keySet()
              .stream()
              .filter(NodeInterfaceNeighborUnreachableOrExitsNetwork.class::isInstance)
              .map(NodeInterfaceNeighborUnreachableOrExitsNetwork.class::cast)
              .collect(Collectors.toSet());
      neighborUnreachables.forEach(nu -> assertThat(nu, hasHostname(node)));

      assertThat(edges, hasEntry(equalTo(new NodeDropNoRoute(node)), hasKey(DropNoRoute.INSTANCE)));
      assertThat(
          edges,
          not(hasEntry(equalTo(new NodeDropNoRoute(otherNode)), hasKey(DropNoRoute.INSTANCE))));

      assertThat(
          edges, hasEntry(equalTo(new NodeDropNullRoute(node)), hasKey(DropNullRoute.INSTANCE)));
      assertThat(
          edges,
          not(hasEntry(equalTo(new NodeDropNullRoute(otherNode)), hasKey(DropNullRoute.INSTANCE))));
    }
  }

  @Test
  public void testForbiddenTransitNodes() throws IOException {
    SortedMap<String, Configuration> configs = TestNetworkSources.twoNodeNetwork();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, temp);
    batfish.computeDataPlane(false);
    DataPlane dataPlane = batfish.loadDataPlane();

    assertThat(configs.size(), equalTo(2));
    for (String node : configs.keySet()) {
      String otherNode = configs.keySet().stream().filter(n -> !n.equals(node)).findFirst().get();
      BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
          new BDDReachabilityAnalysisFactory(PKT, configs, dataPlane.getForwardingAnalysis());
      Map<StateExpr, Map<StateExpr, Edge>> edgeMap =
          bddReachabilityAnalysisFactory
              .bddReachabilityAnalysis(
                  ipSpaceAssignment(batfish),
                  UniverseIpSpace.INSTANCE,
                  ImmutableSet.of(node),
                  ImmutableSet.of(),
                  ImmutableSet.of(),
                  ALL_DISPOSITIONS)
              .getEdges();

      Set<Edge> edges =
          edgeMap.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());

      assertThat(
          "Edges at which a forbiddenTransitNode would become transited should be removed.",
          edges
              .stream()
              .noneMatch(
                  edge ->
                      edge.getPreState() instanceof PreOutEdgePostNat
                          && edge.getPostState() instanceof PreInInterface
                          && ((PreOutEdgePostNat) edge.getPreState()).getSrcNode().equals(node)));
      assertThat(
          "Edges at which a non-forbiddenTransitNodes become transited should not be removed.",
          edges
              .stream()
              .anyMatch(
                  edge ->
                      edge.getPreState() instanceof PreOutEdgePostNat
                          && edge.getPostState() instanceof PreInInterface
                          && ((PreOutEdgePostNat) edge.getPreState())
                              .getSrcNode()
                              .equals(otherNode)));
    }
  }

  @Test
  public void testRequiredTransitNodes() throws IOException {
    SortedMap<String, Configuration> configs = TestNetworkSources.twoNodeNetwork();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, temp);
    batfish.computeDataPlane(false);
    DataPlane dataPlane = batfish.loadDataPlane();

    assertThat(configs.size(), equalTo(2));
    for (String node : configs.keySet()) {
      BDDReachabilityAnalysisFactory bddReachabilityAnalysisFactory =
          new BDDReachabilityAnalysisFactory(PKT, configs, dataPlane.getForwardingAnalysis());
      BDD requiredTransitNodesBDD = bddReachabilityAnalysisFactory.getRequiredTransitNodeBDD();
      BDD transited = requiredTransitNodesBDD;
      BDD notTransited = requiredTransitNodesBDD.not();
      Map<StateExpr, Map<StateExpr, Edge>> edgeMap =
          bddReachabilityAnalysisFactory
              .bddReachabilityAnalysis(
                  ipSpaceAssignment(batfish),
                  UniverseIpSpace.INSTANCE,
                  ImmutableSet.of(),
                  ImmutableSet.of(node),
                  ImmutableSet.of(),
                  ALL_DISPOSITIONS)
              .getEdges();
      Set<Edge> edges =
          edgeMap.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());

      // all edges into the query state require transit nodes transited bit to be set.
      edges
          .stream()
          .filter(edge -> edge.getPostState() == Query.INSTANCE)
          .forEach(
              edge -> {
                assertThat(
                    "Edge into query state must require requiredTransitNodes bit to be one",
                    edge.traverseForward(ONE).and(notTransited).isZero());
                assertThat(
                    "Edge into query state must require requiredTransitNodes bit to be one",
                    edge.traverseBackward(ONE).and(notTransited).isZero());
              });

      // all edges from originate states initialize the transit nodes bit to zero
      edges
          .stream()
          .filter(
              edge ->
                  edge.getPreState() instanceof OriginateVrf
                      || edge.getPreState() instanceof OriginateInterfaceLink)
          .forEach(
              edge -> {
                assertThat(
                    "Edge out of originate state must require requiredTransitNodes bit to be zero",
                    edge.traverseForward(ONE).and(transited).isZero());
                assertThat(
                    "Edge out of originate state must require requiredTransitNodes bit to be zero",
                    edge.traverseBackward(ONE).and(transited).isZero());
              });

      edges
          .stream()
          .filter(
              edge ->
                  edge.getPreState() instanceof PreOutEdgePostNat
                      && edge.getPostState() instanceof PreInInterface)
          .forEach(
              edge -> {
                String hostname = ((PreOutEdgePostNat) edge.getPreState()).getSrcNode();
                if (hostname.equals(node)) {
                  assertThat(
                      "Forward Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "requiredTransitNode must set the requiredTransitNodes bit",
                      edge.traverseForward(ONE).and(notTransited).isZero());
                  BDD backwardOne = edge.traverseBackward(ONE);
                  assertThat(
                      "Backward Edge from PreOutEdgePostNat to PreInInterface for a "
                          + " requiredTransitNode must not constrain the bit after exit",
                      backwardOne.exist(requiredTransitNodesBDD).equals(backwardOne));
                } else {
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge.traverseForward(transited).and(notTransited).isZero());
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge.traverseForward(notTransited).and(transited).isZero());
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge.traverseBackward(transited).and(notTransited).isZero());
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge.traverseBackward(notTransited).and(transited).isZero());
                }
              });
    }
  }

  @Test
  public void testGetAllBDDsLoop() throws IOException {
    SortedMap<String, Configuration> configs = LoopNetwork.testLoopNetwork(true);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, temp);
    batfish.computeDataPlane(false);

    ReachabilityParameters reachabilityParameters =
        ReachabilityParameters.builder()
            .setSourceLocationSpecifier(AllInterfacesLocationSpecifier.INSTANCE)
            .setActions(ImmutableSortedSet.of(FlowDisposition.LOOP))
            .setFinalNodesSpecifier(AllNodesNodeSpecifier.INSTANCE)
            .setHeaderSpace(new HeaderSpace())
            .build();

    AnswerElement answer = batfish.bddSingleReachability(reachabilityParameters);

    assertThat(answer, instanceOf(TraceWrapperAsAnswerElement.class));
    Map<Flow, List<Trace>> flowTraces = ((TraceWrapperAsAnswerElement) answer).getFlowTraces();
    Set<Flow> flows = flowTraces.keySet();
    assertThat(flows, hasSize(2));

    Set<Ip> srcIps = flows.stream().map(Flow::getSrcIp).collect(Collectors.toSet());
    Set<Ip> dstIps = flows.stream().map(Flow::getDstIp).collect(Collectors.toSet());

    assertThat(srcIps, contains(new Ip("1.0.0.0"), new Ip("1.0.0.1")));
    assertThat(dstIps, contains(new Ip("2.0.0.0")));

    Set<FlowDisposition> flowDispositions =
        flowTraces
            .values()
            .stream()
            .flatMap(List::stream)
            .map(Trace::getDisposition)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(flowDispositions, contains(FlowDisposition.LOOP));
  }

  @Test
  public void testGetAllBDDsLoopWithAccepted() throws IOException {
    SortedMap<String, Configuration> configs = new TreeMap<>(LoopNetwork.testLoopNetwork(true));

    // adding an isolated config which has no route to the looping destination prefix
    NetworkFactory nf = new NetworkFactory();
    Configuration loopbackConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("loopbackConfig")
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(loopbackConfig).build();

    InterfaceAddress loopbackAddress = new InterfaceAddress("1.2.3.4/32");
    nf.interfaceBuilder()
        .setActive(true)
        .setOwner(loopbackConfig)
        .setVrf(vrf)
        .setAddress(loopbackAddress)
        .build();
    configs.put(loopbackConfig.getHostname(), loopbackConfig);

    Batfish batfish = BatfishTestUtils.getBatfish(configs, temp);
    batfish.computeDataPlane(false);

    ReachabilityParameters reachabilityParameters =
        ReachabilityParameters.builder()
            .setSourceLocationSpecifier(AllInterfacesLocationSpecifier.INSTANCE)
            .setActions(ImmutableSortedSet.of(FlowDisposition.LOOP, FlowDisposition.NO_ROUTE))
            .setFinalNodesSpecifier(AllNodesNodeSpecifier.INSTANCE)
            .setHeaderSpace(HeaderSpace.builder().setDstIps(new Ip("2.0.0.0").toIpSpace()).build())
            .build();

    AnswerElement answer = batfish.bddSingleReachability(reachabilityParameters);

    assertThat(answer, instanceOf(TraceWrapperAsAnswerElement.class));
    Map<Flow, List<Trace>> flowTraces = ((TraceWrapperAsAnswerElement) answer).getFlowTraces();
    Set<Flow> flows = flowTraces.keySet();
    assertThat(flows, hasSize(3));

    Set<Ip> srcIps = flows.stream().map(Flow::getSrcIp).collect(Collectors.toSet());
    Set<Ip> dstIps = flows.stream().map(Flow::getDstIp).collect(Collectors.toSet());

    assertThat(srcIps, contains(new Ip("1.0.0.0"), new Ip("1.0.0.1"), new Ip("1.2.3.4")));
    assertThat(dstIps, contains(new Ip("2.0.0.0")));

    Set<FlowDisposition> flowDispositions =
        flowTraces
            .values()
            .stream()
            .flatMap(List::stream)
            .map(Trace::getDisposition)
            .collect(ImmutableSet.toImmutableSet());

    assertThat(
        flowDispositions, containsInAnyOrder(FlowDisposition.LOOP, FlowDisposition.NO_ROUTE));
  }
}
