package org.batfish.bddreachability;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.symbolic.state.InterfaceAccept;
import org.batfish.symbolic.state.NodeDropNoRoute;
import org.batfish.symbolic.state.NodeDropNullRoute;
import org.batfish.symbolic.state.StateExpr;
import org.batfish.symbolic.state.VrfAccept;

/**
 * Module used to generate forwarding {@link Edge}s with configurable pre/postStates.
 *
 * <p>{@link BDDFibGenerator} encodes the network's forwarding behavior in BDDs for reachability
 * analysis. Input is the result maps from {@link org.batfish.datamodel.ForwardingAnalysis}, with
 * constraints converted to {@link BDD}s. Forwarding behavior can be used in different ways, i.e.,
 * different parts of the reachability analysis graph, so the pre/post states of the forwarding
 * subgraph are configurable.
 */
@ParametersAreNonnullByDefault
public final class BDDFibGenerator {

  /*
   * node -> vrf -> edge -> set of packets that vrf will forward out that edge successfully,
   * including that the neighbor will respond to ARP.
   */
  private final Map<String, Map<String, Map<org.batfish.datamodel.Edge, BDD>>> _arpTrueEdgeBDDs;

  private final Map<String, Map<String, Map<String, BDD>>> _deliveredToSubnetBDDs;

  private final Map<String, Map<String, Map<String, BDD>>> _exitsNetworkBDDs;

  private final Map<String, Map<String, Map<String, BDD>>> _insufficientInfoBDDs;

  /*
   * node --> vrf --> interface --> set of packets that get routed out the interface but do not
   * reach the neighbor, or exits network, or delivered to subnet
   * This includes neighbor unreachable, exits network, and delivered to subnet
   */
  private final Map<String, Map<String, Map<String, BDD>>> _neighborUnreachableBDDs;

  // node --> vrf --> nextVrf --> set of packets vrf delegates to nextVrf
  private final Map<String, Map<String, Map<String, BDD>>> _nextVrfBDDs;

  // node --> vrf --> set of packets null-routed by the vrf
  private final Map<String, Map<String, BDD>> _nullRoutedBDDs;

  // node --> vrf --> set of packets routable by the vrf
  private final Map<String, Map<String, BDD>> _routableBDDs;

  // node --> vrf --> interface --> set of packets accepted by the interface
  private final Map<String, Map<String, Map<String, BDD>>> _ifaceAcceptBDDs;

  /** Returns a constraint BDD indicating flows that apply to the given outgoing interface. */
  private final BiFunction<String, String, BDD> _interfaceOutConstraint;

  // node --> vrf --> set of packets accepted by the vrf
  private final Map<String, Map<String, BDD>> _vrfAcceptBDDs;

  public BDDFibGenerator(
      Map<String, Map<String, Map<org.batfish.datamodel.Edge, BDD>>> arpTrueEdgeBDDs,
      Map<String, Map<String, Map<String, BDD>>> neighborUnreachableBDDs,
      Map<String, Map<String, Map<String, BDD>>> deliveredToSubnetBDDs,
      Map<String, Map<String, Map<String, BDD>>> exitsNetworkBDDs,
      Map<String, Map<String, Map<String, BDD>>> insufficientInfoBDDs,
      Map<String, Map<String, Map<String, BDD>>> ifaceAcceptBDDs,
      Map<String, Map<String, BDD>> vrfAcceptBDDs,
      Map<String, Map<String, BDD>> routableBDDs,
      Map<String, Map<String, Map<String, BDD>>> nextVrfBDDs,
      Map<String, Map<String, BDD>> nullRoutedBDDs,
      BiFunction<String, String, BDD> interfaceOutConstraint) {
    _arpTrueEdgeBDDs = arpTrueEdgeBDDs;
    _neighborUnreachableBDDs = neighborUnreachableBDDs;
    _deliveredToSubnetBDDs = deliveredToSubnetBDDs;
    _exitsNetworkBDDs = exitsNetworkBDDs;
    _insufficientInfoBDDs = insufficientInfoBDDs;
    _ifaceAcceptBDDs = ifaceAcceptBDDs;
    _routableBDDs = routableBDDs;
    _nextVrfBDDs = nextVrfBDDs;
    _nullRoutedBDDs = nullRoutedBDDs;
    // fully determined by ifaceAcceptBdds, but already computed in BDDReachabilityAnalysisFactory
    _vrfAcceptBDDs = vrfAcceptBDDs;
    _interfaceOutConstraint = interfaceOutConstraint;
  }

  /**
   * Generate edges related to forwarding using the provided state constructors and node-inclusion
   * criteria.
   */
  @Nonnull
  public Stream<Edge> generateForwardingEdges(
      Predicate<String> includedNode,
      StateExprConstructor2 postInVrf,
      StateExprConstructor4 preOutEdge,
      StateExprConstructor2 preOutVrf,
      StateExprConstructor2 preOutInterfaceDeliveredToSubnet,
      StateExprConstructor2 preOutInterfaceExitsNetwork,
      StateExprConstructor2 preOutInterfaceInsufficientInfo,
      StateExprConstructor2 preOutInterfaceNeighborUnreachable) {
    return Streams.concat(
        generateRules_PostInVrf_InterfaceAccept(includedNode, postInVrf),
        generateRules_InterfaceAccept_VrfAccept(includedNode),
        generateRules_PostInVrf_NodeDropNoRoute(includedNode, postInVrf),
        generateRules_PostInVrf_PostInVrf(includedNode, postInVrf),
        generateRules_PostInVrf_PreOutVrf(includedNode, postInVrf, preOutVrf),
        generateRules_PreOutVrf_NodeDropNullRoute(includedNode, preOutVrf),
        generateRules_PreOutVrf_PreOutEdge(includedNode, preOutVrf, preOutEdge),
        generateRules_PreOutVrf_PreOutInterfaceDisposition(
            includedNode,
            preOutVrf,
            preOutInterfaceDeliveredToSubnet,
            preOutInterfaceExitsNetwork,
            preOutInterfaceInsufficientInfo,
            preOutInterfaceNeighborUnreachable));
  }

  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_PostInVrf_InterfaceAccept(
      Predicate<String> includedNode, StateExprConstructor2 postInVrf) {
    return _ifaceAcceptBDDs.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry -> {
              String node = nodeEntry.getKey();
              return nodeEntry.getValue().entrySet().stream()
                  .flatMap(
                      vrfEntry -> {
                        String vrf = vrfEntry.getKey();
                        return vrfEntry.getValue().entrySet().stream()
                            .map(
                                ifaceEntry -> {
                                  String iface = ifaceEntry.getKey();
                                  BDD acceptBdd = ifaceEntry.getValue();
                                  return new Edge(
                                      postInVrf.apply(node, vrf),
                                      new InterfaceAccept(node, iface),
                                      acceptBdd);
                                });
                      });
            });
  }

  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_InterfaceAccept_VrfAccept(Predicate<String> includedNode) {
    return _ifaceAcceptBDDs.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry -> {
              String node = nodeEntry.getKey();
              return nodeEntry.getValue().entrySet().stream()
                  .flatMap(
                      vrfEntry ->
                          vrfEntry.getValue().keySet().stream()
                              .map(
                                  iface ->
                                      new Edge(
                                          new InterfaceAccept(node, iface),
                                          new VrfAccept(node, vrfEntry.getKey()))));
            });
  }

  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_PostInVrf_NodeDropNoRoute(
      Predicate<String> includedNode, StateExprConstructor2 postInVrf) {
    return _vrfAcceptBDDs.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD acceptBDD = vrfEntry.getValue();
                          BDD routableBDD = _routableBDDs.get(node).get(vrf);
                          return new Edge(
                              postInVrf.apply(node, vrf),
                              new NodeDropNoRoute(node),
                              acceptBDD.nor(routableBDD));
                        }));
  }

  /** Generate edges from vrf to nextVrf */
  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_PostInVrf_PostInVrf(
      Predicate<String> includedNode, StateExprConstructor2 postInVrf) {
    return _nextVrfBDDs.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry -> {
              String node = nodeEntry.getKey();
              return nodeEntry.getValue().entrySet().stream()
                  .flatMap(
                      vrfEntry -> {
                        String vrf = vrfEntry.getKey();
                        return vrfEntry.getValue().entrySet().stream()
                            .map(
                                nextVrfEntry -> {
                                  String nextVrf = nextVrfEntry.getKey();
                                  BDD nextVrfBDD = nextVrfEntry.getValue();
                                  return new Edge(
                                      postInVrf.apply(node, vrf),
                                      postInVrf.apply(node, nextVrf),
                                      nextVrfBDD);
                                });
                      });
            });
  }

  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_PostInVrf_PreOutVrf(
      Predicate<String> includedNode,
      StateExprConstructor2 postInVrf,
      StateExprConstructor2 preOutVrf) {
    return _vrfAcceptBDDs.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD routableBDD = _routableBDDs.get(node).get(vrf);
                          BDD acceptBDD = vrfEntry.getValue();
                          BDD[] routableDispatched =
                              Stream.concat(
                                      Stream.of(acceptBDD),
                                      _nextVrfBDDs.get(node).get(vrf).values().stream())
                                  .toArray(BDD[]::new);
                          return new Edge(
                              postInVrf.apply(node, vrf),
                              preOutVrf.apply(node, vrf),
                              routableBDD.diff(routableBDD.getFactory().orAll(routableDispatched)));
                        }));
  }

  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_PreOutVrf_NodeDropNullRoute(
      Predicate<String> includedNode, StateExprConstructor2 preOutVrf) {
    return _nullRoutedBDDs.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD nullRoutedBDD = vrfEntry.getValue();
                          return new Edge(
                              preOutVrf.apply(node, vrf),
                              new NodeDropNullRoute(node),
                              nullRoutedBDD);
                        }));
  }

  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_PreOutVrf_PreOutEdge(
      Predicate<String> includedNode,
      StateExprConstructor2 preOutVrf,
      StateExprConstructor4 preOutEdge) {
    return _arpTrueEdgeBDDs.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
                    .flatMap(
                        vrfEntry ->
                            vrfEntry.getValue().entrySet().stream()
                                .map(
                                    edgeEntry -> {
                                      org.batfish.datamodel.Edge edge = edgeEntry.getKey();
                                      BDD arpTrue = edgeEntry.getValue();

                                      String node1 = edge.getNode1();
                                      String iface1 = edge.getInt1();
                                      String vrf1 = vrfEntry.getKey();
                                      String node2 = edge.getNode2();
                                      String iface2 = edge.getInt2();

                                      BDD leavingIface1 =
                                          _interfaceOutConstraint.apply(node1, iface1);

                                      return new Edge(
                                          preOutVrf.apply(node1, vrf1),
                                          preOutEdge.apply(node1, iface1, node2, iface2),
                                          leavingIface1.and(arpTrue));
                                    })));
  }

  @Nonnull
  @VisibleForTesting
  Stream<Edge> generateRules_PreOutVrf_PreOutInterfaceDisposition(
      Predicate<String> includedNode,
      StateExprConstructor2 preOutVrf,
      StateExprConstructor2 preOutInterfaceDeliveredToSubnet,
      StateExprConstructor2 preOutInterfaceExitsNetwork,
      StateExprConstructor2 preOutInterfaceInsufficientInfo,
      StateExprConstructor2 preOutInterfaceNeighborUnreachable) {
    return Streams.concat(
        generateRules_PreOutVrf_PreOutInterfaceDisposition(
            includedNode,
            _deliveredToSubnetBDDs,
            preOutVrf,
            preOutInterfaceDeliveredToSubnet,
            _interfaceOutConstraint),
        generateRules_PreOutVrf_PreOutInterfaceDisposition(
            includedNode,
            _exitsNetworkBDDs,
            preOutVrf,
            preOutInterfaceExitsNetwork,
            _interfaceOutConstraint),
        generateRules_PreOutVrf_PreOutInterfaceDisposition(
            includedNode,
            _insufficientInfoBDDs,
            preOutVrf,
            preOutInterfaceInsufficientInfo,
            _interfaceOutConstraint),
        generateRules_PreOutVrf_PreOutInterfaceDisposition(
            includedNode,
            _neighborUnreachableBDDs,
            preOutVrf,
            preOutInterfaceNeighborUnreachable,
            _interfaceOutConstraint));
  }

  @Nonnull
  @VisibleForTesting
  static Stream<Edge> generateRules_PreOutVrf_PreOutInterfaceDisposition(
      Predicate<String> includedNode,
      Map<String, Map<String, Map<String, BDD>>> dispositionBddMap,
      StateExprConstructor2 preOutVrfConstructor,
      StateExprConstructor2 preOutInterfaceDispositionConstructor,
      BiFunction<String, String, BDD> interfaceOutConstraint) {
    return dispositionBddMap.entrySet().stream()
        .filter(byNodeEntry -> includedNode.test(byNodeEntry.getKey()))
        .flatMap(
            nodeEntry -> {
              String hostname = nodeEntry.getKey();
              return nodeEntry.getValue().entrySet().stream()
                  .flatMap(
                      vrfEntry -> {
                        String vrfName = vrfEntry.getKey();
                        StateExpr preState = preOutVrfConstructor.apply(hostname, vrfName);
                        return vrfEntry.getValue().entrySet().stream()
                            .filter(e -> !e.getValue().isZero())
                            .map(
                                ifaceEntry -> {
                                  String ifaceName = ifaceEntry.getKey();
                                  BDD bdd = ifaceEntry.getValue();
                                  BDD forIface = interfaceOutConstraint.apply(hostname, ifaceName);
                                  return new Edge(
                                      preState,
                                      preOutInterfaceDispositionConstructor.apply(
                                          hostname, ifaceName),
                                      forIface.and(bdd));
                                });
                      });
            });
  }
}
