package org.batfish.bddreachability;

import static org.batfish.common.util.CommonUtil.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationVisitor;
import org.batfish.symbolic.bdd.BDDAcl;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.Drop;
import org.batfish.z3.state.NeighborUnreachable;
import org.batfish.z3.state.NodeAccept;
import org.batfish.z3.state.NodeDrop;
import org.batfish.z3.state.NodeDropAclIn;
import org.batfish.z3.state.NodeDropAclOut;
import org.batfish.z3.state.NodeDropNoRoute;
import org.batfish.z3.state.NodeDropNullRoute;
import org.batfish.z3.state.NodeInterfaceNeighborUnreachable;
import org.batfish.z3.state.OriginateInterfaceLink;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.PostInVrf;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOutEdge;
import org.batfish.z3.state.PreOutEdgePostNat;
import org.batfish.z3.state.PreOutVrf;

/**
 * Constructs a the reachability graph for {@link BDDReachabilityAnalysis}. The graph is very
 * similar to the NOD programs generated by {@link
 * org.batfish.z3.state.visitors.DefaultTransitionGenerator}. The public API is very simple: it
 * provides two methods for constructing {@link BDDReachabilityAnalysis}, depending on whether or
 * not you have a destination Ip constraint.
 *
 * <p>The core of the implementation is the {@link BDDReachabilityAnalysisFactory#generateRules()}
 * method and its many helpers, which generate the {@link StateExpr nodes} and {@link Edge edges} of
 * the reachability graph. Each node represents a step of the routing process within some network
 * device or between devices. The edges represent the flow of traffic between these steps. Each edge
 * is labeled with a {@link BDD} that represents the set of packets that can traverse that edge. If
 * the edge represents a source NAT, the edge will be labeled with the NAT rules (match conditions
 * and set of pool IPs).
 */
public final class BDDReachabilityAnalysisFactory {
  // node name --> acl name --> set of packets denied by the acl.
  private final Map<String, Map<String, BDD>> _aclDenyBDDs;

  // node name --> acl name --> set of packets permitted by the acl.
  private final Map<String, Map<String, BDD>> _aclPermitBDDs;

  /*
   * edge --> set of packets that will flow out the edge successfully, including that the
   * neighbor will respond to ARP.
   */
  private final Map<org.batfish.datamodel.Edge, BDD> _arpTrueEdgeBDDs;

  /*
   * Symbolic variables corresponding to the different packet header fields. We use these to
   * generate new BDD constraints on those fields. Each constraint can be understood as the set
   * of packet headers for which the constraint is satisfied.
   */
  private final BDDPacket _bddPacket;

  // node name -> node
  private final Map<String, Configuration> _configs;

  // preState --> postState --> edge.
  private final Map<StateExpr, Map<StateExpr, Edge>> _edges;

  private final ForwardingAnalysis _forwardingAnalysis;

  private IpSpaceToBDD _dstIpSpaceToBDD;

  /*
   * node --> vrf --> interface --> set of packets that get routed out the interface but do not
   * reach the neighbor
   */
  private final Map<String, Map<String, Map<String, BDD>>> _neighborUnreachableBDDs;

  // node --> vrf --> set of packets routable by the vrf
  private final Map<String, Map<String, BDD>> _routableBDDs;

  // conjunction of the BDD vars encoding source IP. Used for existential quantification in source
  // NAT.
  private final BDD _sourceIpVars;

  // node --> vrf --> set of packets accepted by the vrf
  private final Map<String, Map<String, BDD>> _vrfAcceptBDDs;

  // node --> vrf --> set of packets not accepted by the vrf
  private final Map<String, Map<String, BDD>> _vrfNotAcceptBDDs;

  public BDDReachabilityAnalysisFactory(
      BDDPacket packet, Map<String, Configuration> configs, ForwardingAnalysis forwardingAnalysis) {
    _bddPacket = packet;
    _configs = configs;
    _forwardingAnalysis = forwardingAnalysis;
    _dstIpSpaceToBDD = new IpSpaceToBDD(_bddPacket.getFactory(), _bddPacket.getDstIp());

    Map<String, Map<String, BDDAcl>> bddAcls = computeBDDAcls(_bddPacket, configs);
    _aclDenyBDDs = computeAclDenyBDDs(bddAcls);
    _aclPermitBDDs = computeAclPermitBDDs(bddAcls);

    _arpTrueEdgeBDDs = computeArpTrueEdgeBDDs(forwardingAnalysis, _dstIpSpaceToBDD);
    _neighborUnreachableBDDs = computeNeighborUnreachableBDDs(forwardingAnalysis, _dstIpSpaceToBDD);
    _routableBDDs = computeRoutableBDDs(forwardingAnalysis, _dstIpSpaceToBDD);
    _vrfAcceptBDDs = computeVrfAcceptBDDs(configs, _dstIpSpaceToBDD);
    _vrfNotAcceptBDDs = computeVrfNotAcceptBDDs(_vrfAcceptBDDs);

    _sourceIpVars =
        Arrays.stream(_bddPacket.getSrcIp().getBitvec())
            .reduce(_bddPacket.getFactory().one(), BDD::and);

    _edges = computeEdges();
  }

  private static Map<String, Map<String, BDDAcl>> computeBDDAcls(
      BDDPacket bddPacket, Map<String, Configuration> configs) {
    return toImmutableMap(
        configs,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue().getIpAccessLists(),
                Entry::getKey,
                aclEntry ->
                    BDDAcl.create(
                        bddPacket,
                        aclEntry.getValue(),
                        nodeEntry.getValue().getIpAccessLists(),
                        nodeEntry.getValue().getIpSpaces())));
  }

  private static Map<String, Map<String, BDD>> computeAclDenyBDDs(
      Map<String, Map<String, BDDAcl>> aclBDDs) {
    return toImmutableMap(
        aclBDDs,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                aclEntry -> aclEntry.getValue().getBdd().not()));
  }

  private static Map<String, Map<String, BDD>> computeAclPermitBDDs(
      Map<String, Map<String, BDDAcl>> aclBDDs) {
    return toImmutableMap(
        aclBDDs,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(), Entry::getKey, aclEntry -> aclEntry.getValue().getBdd()));
  }

  private Map<StateExpr, Map<StateExpr, Edge>> computeEdges() {
    Map<StateExpr, Map<StateExpr, Edge>> edges = new HashMap<>();

    generateRules()
        .forEach(
            edge ->
                edges
                    .computeIfAbsent(edge._preState, k -> new HashMap<>())
                    .put(edge._postState, edge));

    // freeze
    return toImmutableMap(
        edges,
        Entry::getKey,
        preStateEntry -> toImmutableMap(preStateEntry.getValue(), Entry::getKey, Entry::getValue));
  }

  private static Map<String, Map<String, BDD>> computeRoutableBDDs(
      ForwardingAnalysis forwardingAnalysis, IpSpaceToBDD ipSpaceToBDD) {
    return toImmutableMap(
        forwardingAnalysis.getRoutableIps(),
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().accept(ipSpaceToBDD)));
  }

  private static Map<String, Map<String, BDD>> computeVrfNotAcceptBDDs(
      Map<String, Map<String, BDD>> vrfAcceptBDDs) {
    return toImmutableMap(
        vrfAcceptBDDs,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(), Entry::getKey, vrfEntry -> vrfEntry.getValue().not()));
  }

  IpSpaceToBDD getIpSpaceToBDD() {
    return _dstIpSpaceToBDD;
  }

  Map<String, Map<String, BDD>> getVrfAcceptBDDs() {
    return _vrfAcceptBDDs;
  }

  private static Map<org.batfish.datamodel.Edge, BDD> computeArpTrueEdgeBDDs(
      ForwardingAnalysis forwardingAnalysis, IpSpaceToBDD ipSpaceToBDD) {
    return toImmutableMap(
        forwardingAnalysis.getArpTrueEdge(),
        Entry::getKey,
        entry -> entry.getValue().accept(ipSpaceToBDD));
  }

  private static Map<String, Map<String, Map<String, BDD>>> computeNeighborUnreachableBDDs(
      ForwardingAnalysis forwardingAnalysis, IpSpaceToBDD ipSpaceToBDD) {
    return toImmutableMap(
        forwardingAnalysis.getNeighborUnreachable(),
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                vrfEntry ->
                    toImmutableMap(
                        vrfEntry.getValue(),
                        Entry::getKey,
                        ifaceEntry -> ifaceEntry.getValue().accept(ipSpaceToBDD))));
  }

  private Stream<Edge> generateRules() {
    return Streams.concat(
        generateRules_NodeAccept_Accept(),
        generateRules_NodeDropAclIn_NodeDrop(),
        generateRules_NodeDropNoRoute_NodeDrop(),
        generateRules_NodeDropNullRoute_NodeDrop(),
        generateRules_NodeDropAclOut_NodeDrop(),
        generateRules_NodeDrop_Drop(),
        generateRules_NodeInterfaceNeighborUnreachable_NeighborUnreachable(),
        generateRules_PreInInterface_NodeDropAclIn(),
        generateRules_PreInInterface_PostInVrf(),
        generateRules_PostInVrf_NodeAccept(),
        generateRules_PostInVrf_NodeDropNoRoute(),
        generateRules_PostInVrf_PreOutVrf(),
        generateRules_PreOutEdge_PreOutEdgePostNat(),
        generateRules_PreOutEdgePostNat_NodeDropAclOut(),
        generateRules_PreOutEdgePostNat_PreInInterface(),
        generateRules_PreOutVrf_NodeDropNullRoute(),
        generateRules_PreOutVrf_NodeInterfaceNeighborUnreachable(),
        generateRules_PreOutVrf_PreOutEdge());
  }

  private Stream<Edge> generateRules_NodeAccept_Accept() {
    return _configs
        .keySet()
        .stream()
        .map(
            node -> new Edge(new NodeAccept(node), Accept.INSTANCE, _bddPacket.getFactory().one()));
  }

  private Stream<Edge> generateRules_NodeDropAclIn_NodeDrop() {
    return _configs
        .keySet()
        .stream()
        .map(
            node ->
                new Edge(
                    new NodeDropAclIn(node), new NodeDrop(node), _bddPacket.getFactory().one()));
  }

  private Stream<Edge> generateRules_NodeDropAclOut_NodeDrop() {
    return _configs
        .keySet()
        .stream()
        .map(
            node ->
                new Edge(
                    new NodeDropAclOut(node), new NodeDrop(node), _bddPacket.getFactory().one()));
  }

  private Stream<Edge> generateRules_NodeDropNoRoute_NodeDrop() {
    return _configs
        .keySet()
        .stream()
        .map(
            node ->
                new Edge(
                    new NodeDropNoRoute(node), new NodeDrop(node), _bddPacket.getFactory().one()));
  }

  private Stream<Edge> generateRules_NodeDropNullRoute_NodeDrop() {
    return _configs
        .keySet()
        .stream()
        .map(
            node ->
                new Edge(
                    new NodeDropNullRoute(node),
                    new NodeDrop(node),
                    _bddPacket.getFactory().one()));
  }

  private Stream<Edge> generateRules_NodeDrop_Drop() {
    return _configs
        .keySet()
        .stream()
        .map(node -> new Edge(new NodeDrop(node), Drop.INSTANCE, _bddPacket.getFactory().one()));
  }

  private Stream<Edge> generateRules_NodeInterfaceNeighborUnreachable_NeighborUnreachable() {
    return _configs
        .values()
        .stream()
        .flatMap(c -> c.getAllInterfaces().values().stream())
        .map(
            iface -> {
              String nodeNode = iface.getOwner().getHostname();
              String ifaceName = iface.getName();
              return new Edge(
                  new NodeInterfaceNeighborUnreachable(nodeNode, ifaceName),
                  NeighborUnreachable.INSTANCE,
                  _bddPacket.getFactory().one());
            });
  }

  private Stream<Edge> generateRules_PostInVrf_NodeAccept() {
    return _vrfAcceptBDDs
        .entrySet()
        .stream()
        .flatMap(
            nodeEntry ->
                nodeEntry
                    .getValue()
                    .entrySet()
                    .stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD acceptBDD = vrfEntry.getValue();
                          return new Edge(
                              new PostInVrf(node, vrf), new NodeAccept(node), acceptBDD);
                        }));
  }

  private Stream<Edge> generateRules_PostInVrf_NodeDropNoRoute() {
    return _vrfNotAcceptBDDs
        .entrySet()
        .stream()
        .flatMap(
            nodeEntry ->
                nodeEntry
                    .getValue()
                    .entrySet()
                    .stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD notAcceptBDD = vrfEntry.getValue();
                          BDD notRoutableBDD = _routableBDDs.get(node).get(vrf).not();
                          return new Edge(
                              new PostInVrf(node, vrf),
                              new NodeDropNoRoute(node),
                              notAcceptBDD.and(notRoutableBDD));
                        }));
  }

  private Stream<Edge> generateRules_PostInVrf_PreOutVrf() {
    return _vrfNotAcceptBDDs
        .entrySet()
        .stream()
        .flatMap(
            nodeEntry ->
                nodeEntry
                    .getValue()
                    .entrySet()
                    .stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD notAcceptBDD = vrfEntry.getValue();
                          BDD routableBDD = _routableBDDs.get(node).get(vrf);
                          return new Edge(
                              new PostInVrf(node, vrf),
                              new PreOutVrf(node, vrf),
                              notAcceptBDD.and(routableBDD));
                        }));
  }

  private Stream<Edge> generateRules_PreInInterface_NodeDropAclIn() {
    return _configs
        .values()
        .stream()
        .map(Configuration::getVrfs)
        .map(Map::values)
        .flatMap(Collection::stream)
        .flatMap(vrf -> vrf.getInterfaces().values().stream())
        .filter(iface -> iface.getIncomingFilter() != null)
        .map(
            iface -> {
              String aclName = iface.getIncomingFilterName();
              String nodeName = iface.getOwner().getHostname();
              String ifaceName = iface.getName();

              BDD aclDenyBDD = _aclDenyBDDs.get(nodeName).get(aclName);
              return new Edge(
                  new PreInInterface(nodeName, ifaceName), new NodeDropAclIn(nodeName), aclDenyBDD);
            });
  }

  private Stream<Edge> generateRules_PreInInterface_PostInVrf() {
    return _configs
        .values()
        .stream()
        .map(Configuration::getVrfs)
        .map(Map::values)
        .flatMap(Collection::stream)
        .flatMap(vrf -> vrf.getInterfaces().values().stream())
        .map(
            iface -> {
              String aclName = iface.getIncomingFilterName();
              String nodeName = iface.getOwner().getHostname();
              String vrfName = iface.getVrfName();
              String ifaceName = iface.getName();

              BDD inAclBDD =
                  aclName == null
                      ? _bddPacket.getFactory().one()
                      : _aclPermitBDDs.get(nodeName).get(aclName);
              return new Edge(
                  new PreInInterface(nodeName, ifaceName),
                  new PostInVrf(nodeName, vrfName),
                  inAclBDD);
            });
  }

  private Stream<Edge> generateRules_PreOutEdge_PreOutEdgePostNat() {
    return _forwardingAnalysis
        .getArpTrueEdge()
        .keySet()
        .stream()
        .map(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              PreOutEdge preOutEdge = new PreOutEdge(node1, iface1, node2, iface2);
              PreOutEdgePostNat preOutEdgePostNat =
                  new PreOutEdgePostNat(node1, iface1, node2, iface2);

              List<SourceNat> sourceNats =
                  _configs.get(node1).getAllInterfaces().get(iface1).getSourceNats();

              if (sourceNats == null) {
                return new Edge(preOutEdge, preOutEdgePostNat, _bddPacket.getFactory().one());
              }

              List<BDDSourceNat> bddSourceNats =
                  sourceNats
                      .stream()
                      .map(
                          sourceNat -> {
                            String aclName = sourceNat.getAcl().getName();
                            BDD match = _aclPermitBDDs.get(node1).get(aclName);
                            BDD setSrcIp =
                                _bddPacket
                                    .getSrcIp()
                                    .geq(sourceNat.getPoolIpFirst().asLong())
                                    .and(
                                        _bddPacket
                                            .getSrcIp()
                                            .leq(sourceNat.getPoolIpLast().asLong()));
                            return new BDDSourceNat(match, setSrcIp);
                          })
                      .collect(ImmutableList.toImmutableList());

              return new Edge(
                  preOutEdge,
                  preOutEdgePostNat,
                  sourceNatBackwardEdge(bddSourceNats),
                  sourceNatForwardEdge(bddSourceNats));
            });
  }

  private Stream<Edge> generateRules_PreOutEdgePostNat_NodeDropAclOut() {
    return _forwardingAnalysis
        .getArpTrueEdge()
        .keySet()
        .stream()
        .flatMap(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              String aclName =
                  _configs.get(node1).getAllInterfaces().get(iface1).getOutgoingFilterName();
              BDD aclDenyBDD = _aclDenyBDDs.get(node1).get(aclName);

              return aclDenyBDD != null
                  ? Stream.of(
                      new Edge(
                          new PreOutEdgePostNat(node1, iface1, node2, iface2),
                          new NodeDropAclOut(node1),
                          aclDenyBDD))
                  : Stream.of();
            });
  }

  private Stream<Edge> generateRules_PreOutEdgePostNat_PreInInterface() {
    return _forwardingAnalysis
        .getArpTrueEdge()
        .keySet()
        .stream()
        .map(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              String aclName =
                  _configs.get(node1).getAllInterfaces().get(iface1).getOutgoingFilterName();
              BDD aclPermitBDD =
                  aclName == null
                      ? _bddPacket.getFactory().one()
                      : _aclPermitBDDs.get(node1).get(aclName);
              assert aclPermitBDD != null;

              return new Edge(
                  new PreOutEdgePostNat(node1, iface1, node2, iface2),
                  new PreInInterface(node2, iface2),
                  aclPermitBDD);
            });
  }

  private Stream<Edge> generateRules_PreOutVrf_NodeDropNullRoute() {
    return _forwardingAnalysis
        .getNullRoutedIps()
        .entrySet()
        .stream()
        .flatMap(
            nodeEntry ->
                nodeEntry
                    .getValue()
                    .entrySet()
                    .stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD nullRoutedBDD = vrfEntry.getValue().accept(_dstIpSpaceToBDD);
                          return new Edge(
                              new PreOutVrf(node, vrf), new NodeDropNullRoute(node), nullRoutedBDD);
                        }));
  }

  private Stream<Edge> generateRules_PreOutVrf_NodeInterfaceNeighborUnreachable() {
    return _neighborUnreachableBDDs
        .entrySet()
        .stream()
        .flatMap(
            nodeEntry -> {
              String node = nodeEntry.getKey();
              return nodeEntry
                  .getValue()
                  .entrySet()
                  .stream()
                  .flatMap(
                      vrfEntry -> {
                        String vrf = vrfEntry.getKey();
                        return vrfEntry
                            .getValue()
                            .entrySet()
                            .stream()
                            .map(
                                ifaceEntry -> {
                                  String iface = ifaceEntry.getKey();
                                  BDD ipSpaceBDD = ifaceEntry.getValue();
                                  String outAcl =
                                      _configs
                                          .get(node)
                                          .getAllInterfaces()
                                          .get(iface)
                                          .getOutgoingFilterName();
                                  BDD outAclBDD =
                                      outAcl == null
                                          ? _bddPacket.getFactory().one()
                                          : _aclPermitBDDs.get(node).get(outAcl);
                                  return new Edge(
                                      new PreOutVrf(node, vrf),
                                      new NodeInterfaceNeighborUnreachable(node, iface),
                                      ipSpaceBDD.and(outAclBDD));
                                });
                      });
            });
  }

  private Stream<Edge> generateRules_PreOutVrf_PreOutEdge() {
    return _arpTrueEdgeBDDs
        .entrySet()
        .stream()
        .map(
            entry -> {
              org.batfish.datamodel.Edge edge = entry.getKey();
              BDD arpTrue = entry.getValue();

              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String vrf1 = ifaceVrf(edge.getNode1(), edge.getInt1());
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              return new Edge(
                  new PreOutVrf(node1, vrf1),
                  new PreOutEdge(node1, iface1, node2, iface2),
                  arpTrue);
            });
  }

  @Nonnull
  private LocationVisitor<StateExpr> getLocationToStateExpr() {
    return new LocationVisitor<StateExpr>() {
      @Override
      public StateExpr visitInterfaceLinkLocation(
          @Nonnull InterfaceLinkLocation interfaceLinkLocation) {
        return new OriginateInterfaceLink(
            interfaceLinkLocation.getNodeName(), interfaceLinkLocation.getInterfaceName());
      }

      @Override
      public StateExpr visitInterfaceLocation(@Nonnull InterfaceLocation interfaceLocation) {
        String vrf =
            _configs
                .get(interfaceLocation.getNodeName())
                .getAllInterfaces()
                .get(interfaceLocation.getInterfaceName())
                .getVrf()
                .getName();
        return new OriginateVrf(interfaceLocation.getNodeName(), vrf);
      }
    };
  }

  public BDDReachabilityAnalysis bddReachabilityAnalysis(IpSpaceAssignment srcIpSpaceAssignment) {
    return bddReachabilityAnalysis(srcIpSpaceAssignment, UniverseIpSpace.INSTANCE);
  }

  public BDDReachabilityAnalysis bddReachabilityAnalysis(
      IpSpaceAssignment srcIpSpaceAssignment, IpSpace dstIpSpace) {
    Map<StateExpr, BDD> roots = new HashMap<>();
    IpSpaceToBDD srcIpSpaceToBDD = new IpSpaceToBDD(_bddPacket.getFactory(), _bddPacket.getSrcIp());
    IpSpaceToBDD dstIpSpaceToBDD = new IpSpaceToBDD(_bddPacket.getFactory(), _bddPacket.getDstIp());
    BDD dstIpSpaceBDD = dstIpSpace.accept(dstIpSpaceToBDD);

    for (IpSpaceAssignment.Entry entry : srcIpSpaceAssignment.getEntries()) {
      BDD srcIpSpaceBDD = entry.getIpSpace().accept(srcIpSpaceToBDD);
      BDD headerspaceBDD = srcIpSpaceBDD.and(dstIpSpaceBDD);
      for (Location loc : entry.getLocations()) {
        StateExpr root = loc.accept(getLocationToStateExpr());
        roots.put(root, headerspaceBDD);
      }
    }

    return new BDDReachabilityAnalysis(_bddPacket, roots, _edges);
  }

  private String ifaceVrf(String node, String iface) {
    return _configs.get(node).getAllInterfaces().get(iface).getVrfName();
  }

  private static Map<String, Map<String, BDD>> computeVrfAcceptBDDs(
      Map<String, Configuration> configs, IpSpaceToBDD ipSpaceToBDD) {
    Map<String, Map<String, IpSpace>> vrfOwnedIpSpaces =
        CommonUtil.computeVrfOwnedIpSpaces(
            CommonUtil.computeIpVrfOwners(false, CommonUtil.computeNodeInterfaces(configs)));

    return CommonUtil.toImmutableMap(
        vrfOwnedIpSpaces,
        Entry::getKey,
        nodeEntry ->
            CommonUtil.toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().accept(ipSpaceToBDD)));
  }

  private Function<BDD, BDD> sourceNatForwardEdge(List<BDDSourceNat> sourceNats) {
    return orig -> {
      BDD remaining = orig;
      BDD result = orig.getFactory().zero();
      for (BDDSourceNat sourceNat : sourceNats) {
        /*
         * Check the condition, then set source IP (by existentially quantifying away the old value,
         * then ANDing on the new value.
         */
        BDD natted =
            remaining.and(sourceNat._condition).exist(_sourceIpVars).and(sourceNat._updateSrcIp);
        result = result.or(natted);
        remaining = remaining.and(sourceNat._condition.not());
      }
      result = result.or(remaining);
      return result;
    };
  }

  private Function<BDD, BDD> sourceNatBackwardEdge(@Nonnull List<BDDSourceNat> sourceNats) {
    return orig -> {
      BDD origExistSrcIp = orig.exist(_sourceIpVars);
      // non-natted case: srcIp unchanged, none of the lines match
      BDD result =
          sourceNats.stream().map(srcNat -> srcNat._condition.not()).reduce(orig, BDD::and);
      // natted cases
      for (BDDSourceNat sourceNat : sourceNats) {
        if (!orig.and(sourceNat._updateSrcIp).isZero()) {
          // this could be the NAT rule that was applied
          result = result.or(origExistSrcIp.and(sourceNat._condition));
        }
      }
      return result;
    };
  }

  public Map<StateExpr, Map<StateExpr, Edge>> getEdges() {
    return _edges;
  }
}
