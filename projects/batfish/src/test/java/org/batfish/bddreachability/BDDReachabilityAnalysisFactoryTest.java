package org.batfish.bddreachability;

import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.DENIED_IN;
import static org.batfish.datamodel.FlowDisposition.DENIED_OUT;
import static org.batfish.datamodel.FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK;
import static org.batfish.datamodel.FlowDisposition.NO_ROUTE;
import static org.batfish.datamodel.FlowDisposition.NULL_ROUTED;
import static org.batfish.z3.expr.NodeInterfaceNeighborUnreachableMatchers.hasHostname;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.specifier.IpSpaceAssignment;
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
import org.batfish.z3.state.NodeInterfaceNeighborUnreachable;
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
  private static final BDD ONE = PKT.getFactory().one();

  private static final Set<FlowDisposition> ALL_DISPOSITIONS =
      ImmutableSet.of(
          ACCEPTED,
          DENIED_IN,
          DENIED_OUT,
          NO_ROUTE,
          NULL_ROUTED,
          NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK);

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
                  IpSpaceAssignment.builder().build(),
                  UniverseIpSpace.INSTANCE,
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

      Set<NodeInterfaceNeighborUnreachable> neighborUnreachables =
          edges
              .keySet()
              .stream()
              .filter(NodeInterfaceNeighborUnreachable.class::isInstance)
              .map(NodeInterfaceNeighborUnreachable.class::cast)
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
                  IpSpaceAssignment.builder().build(),
                  UniverseIpSpace.INSTANCE,
                  ImmutableSet.of(node),
                  ImmutableSet.of(),
                  ALL_DISPOSITIONS)
              .getEdges();
      Set<Edge> edges =
          edgeMap.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());

      // all edges into the query state require transit nodes transited bit to be set.
      edges
          .stream()
          .filter(edge -> edge._postState == Query.INSTANCE)
          .forEach(
              edge -> {
                assertThat(
                    "Edge into query state must require requiredTransitNodes bit to be one",
                    edge._traverseForward.apply(ONE).and(notTransited).isZero());
                assertThat(
                    "Edge into query state must require requiredTransitNodes bit to be one",
                    edge._traverseBackward.apply(ONE).and(notTransited).isZero());
              });

      // all edges from originate states initialize the transit nodes bit to zero
      edges
          .stream()
          .filter(
              edge ->
                  edge._preState instanceof OriginateVrf
                      || edge._preState instanceof OriginateInterfaceLink)
          .forEach(
              edge -> {
                assertThat(
                    "Edge out of originate state must require requiredTransitNodes bit to be zero",
                    edge._traverseForward.apply(ONE).and(transited).isZero());
                assertThat(
                    "Edge out of originate state must require requiredTransitNodes bit to be zero",
                    edge._traverseBackward.apply(ONE).and(transited).isZero());
              });

      edges
          .stream()
          .filter(
              edge ->
                  edge._preState instanceof PreOutEdgePostNat
                      && edge._postState instanceof PreInInterface)
          .forEach(
              edge -> {
                String hostname = ((PreOutEdgePostNat) edge._preState).getSrcNode();
                if (hostname.equals(node)) {
                  assertThat(
                      "Forward Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "requiredTransitNode must set the requiredTransitNodes bit",
                      edge._traverseForward.apply(ONE).and(notTransited).isZero());
                  BDD backwardOne = edge._traverseBackward.apply(ONE);
                  assertThat(
                      "Backward Edge from PreOutEdgePostNat to PreInInterface for a "
                          + " requiredTransitNode must not constrain the bit after exit",
                      backwardOne.exist(requiredTransitNodesBDD).equals(backwardOne));
                } else {
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge._traverseForward.apply(transited).and(notTransited).isZero());
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge._traverseForward.apply(notTransited).and(transited).isZero());
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge._traverseBackward.apply(transited).and(notTransited).isZero());
                  assertThat(
                      "Edge from PreOutEdgePostNat to PreInInterface for a "
                          + "non-requiredTransitNode must not touch the requiredTransitNodes bit",
                      edge._traverseBackward.apply(notTransited).and(transited).isZero());
                }
              });
    }
  }
}
