package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.bddreachability.transition.Transitions.addOriginatingFromDeviceConstraint;
import static org.batfish.bddreachability.transition.Transitions.addSourceInterfaceConstraint;
import static org.batfish.bddreachability.transition.Transitions.compose;
import static org.batfish.bddreachability.transition.Transitions.constraint;
import static org.batfish.bddreachability.transition.Transitions.eraseAndSet;
import static org.batfish.bddreachability.transition.Transitions.or;
import static org.batfish.bddreachability.transition.Transitions.removeSourceConstraint;
import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.transformation.TransformationUtil.visitTransformationSteps;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.TransformationToTransition;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.common.BatfishException;
import org.batfish.common.bdd.BDDInteger;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.HeaderSpaceToBDD;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.bdd.MemoizedIpSpaceToBDD;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Noop;
import org.batfish.datamodel.transformation.ShiftIpAddressIntoSubnet;
import org.batfish.datamodel.transformation.TransformationStepVisitor;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.LocationVisitor;
import org.batfish.z3.IngressLocation;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.DeliveredToSubnet;
import org.batfish.z3.state.DropAclIn;
import org.batfish.z3.state.DropAclOut;
import org.batfish.z3.state.DropNoRoute;
import org.batfish.z3.state.DropNullRoute;
import org.batfish.z3.state.ExitsNetwork;
import org.batfish.z3.state.InsufficientInfo;
import org.batfish.z3.state.NeighborUnreachable;
import org.batfish.z3.state.NodeAccept;
import org.batfish.z3.state.NodeDropAclIn;
import org.batfish.z3.state.NodeDropAclOut;
import org.batfish.z3.state.NodeDropNoRoute;
import org.batfish.z3.state.NodeDropNullRoute;
import org.batfish.z3.state.NodeInterfaceDeliveredToSubnet;
import org.batfish.z3.state.NodeInterfaceExitsNetwork;
import org.batfish.z3.state.NodeInterfaceInsufficientInfo;
import org.batfish.z3.state.NodeInterfaceNeighborUnreachable;
import org.batfish.z3.state.OriginateInterfaceLink;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.PostInInterface;
import org.batfish.z3.state.PostInVrf;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOutEdge;
import org.batfish.z3.state.PreOutEdgePostNat;
import org.batfish.z3.state.PreOutInterfaceDeliveredToSubnet;
import org.batfish.z3.state.PreOutInterfaceExitsNetwork;
import org.batfish.z3.state.PreOutInterfaceInsufficientInfo;
import org.batfish.z3.state.PreOutInterfaceNeighborUnreachable;
import org.batfish.z3.state.PreOutVrf;
import org.batfish.z3.state.Query;

/**
 * Constructs a the reachability graph for {@link BDDReachabilityAnalysis}. The graph is very
 * similar to the NOD programs generated by {@link
 * org.batfish.z3.state.visitors.DefaultTransitionGenerator}. The public API is very simple: it
 * provides two methods for constructing {@link BDDReachabilityAnalysis}, depending on whether or
 * not you have a destination Ip constraint.
 *
 * <p>The core of the implementation is the {@code generateEdges()} method and its many helpers,
 * which generate the {@link StateExpr nodes} and {@link Edge edges} of the reachability graph. Each
 * node represents a step of the routing process within some network device or between devices. The
 * edges represent the flow of traffic between these steps. Each edge is labeled with a {@link BDD}
 * that represents the set of packets that can traverse that edge. If the edge represents a source
 * NAT, the edge will be labeled with the NAT rules (match conditions and set of pool IPs).
 *
 * <p>To support {@link org.batfish.datamodel.acl.MatchSrcInterface} and {@link
 * org.batfish.datamodel.acl.OriginatingFromDevice} {@link
 * org.batfish.datamodel.acl.AclLineMatchExpr ACL expressions}, we maintain the invariant that
 * whenever a packet is inside a node, it has a valid source (according to the BDDSourceManager of
 * that node). For forward edges this is established by constraining to a single source. For
 * backward edges it's established using {@link BDDSourceManager#isValidValue}. When we exit the
 * node (e.g. forward into another node or a disposition state, or backward into another node or an
 * origination state), we erase the contraint on source by existential quantification.
 */
@ParametersAreNonnullByDefault
public final class BDDReachabilityAnalysisFactory {
  // node name --> acl name --> set of packets denied by the acl.
  private final Map<String, Map<String, Supplier<BDD>>> _aclDenyBDDs;

  // node name --> acl name --> set of packets permitted by the acl.
  private final Map<String, Map<String, Supplier<BDD>>> _aclPermitBDDs;

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

  private final Map<String, BDDSourceManager> _bddSourceManagers;

  // node --> iface --> bdd nats
  private Map<String, Map<String, Transition>> _bddIncomingTransformations;
  private final Map<String, Map<String, Transition>> _bddOutgoingTransformations;

  private final Map<String, Configuration> _configs;

  // only use this for IpSpaces that have no references
  private final IpSpaceToBDD _dstIpSpaceToBDD;
  private final IpSpaceToBDD _srcIpSpaceToBDD;

  private final ForwardingAnalysis _forwardingAnalysis;

  private final boolean _ignoreFilters;

  /*
   * node --> vrf --> interface --> set of packets that get routed out the interface but do not
   * reach the neighbor, or exits network, or delivered to subnet
   * This includes neighbor unreachable, exits network, and delivered to subnet
   */
  private final Map<String, Map<String, Map<String, BDD>>> _neighborUnreachableBDDs;

  private final Map<String, Map<String, Map<String, BDD>>> _deliveredToSubnetBDDs;

  private final Map<String, Map<String, Map<String, BDD>>> _exitsNetworkBDDs;

  private final Map<String, Map<String, Map<String, BDD>>> _insufficientInfoBDDs;

  private final BDD _one;

  private final BDD _requiredTransitNodeBDD;

  // node --> vrf --> set of packets routable by the vrf
  private final Map<String, Map<String, BDD>> _routableBDDs;

  // conjunction of the BDD vars encoding source and dest IPs. Used for existential quantification
  // in source and destination NAT.
  private final BDD _dstIpVars;
  private final BDD _sourceIpVars;

  // ranges of all transformations in the network, per IP address field.
  private final Map<IpField, BDD> _transformationRanges;

  // node --> vrf --> set of packets accepted by the vrf
  private final Map<String, Map<String, BDD>> _vrfAcceptBDDs;

  // node --> vrf --> set of packets not accepted by the vrf
  private final Map<String, Map<String, BDD>> _vrfNotAcceptBDDs;

  private BDD _zero;

  public BDDReachabilityAnalysisFactory(
      BDDPacket packet, Map<String, Configuration> configs, ForwardingAnalysis forwardingAnalysis) {
    this(packet, configs, forwardingAnalysis, false);
  }

  public BDDReachabilityAnalysisFactory(
      BDDPacket packet,
      Map<String, Configuration> configs,
      ForwardingAnalysis forwardingAnalysis,
      boolean ignoreFilters) {
    _bddPacket = packet;
    _one = packet.getFactory().one();
    _zero = packet.getFactory().zero();
    _ignoreFilters = ignoreFilters;
    _requiredTransitNodeBDD = _bddPacket.allocateBDDBit("requiredTransitNodes");
    _bddSourceManagers = BDDSourceManager.forNetwork(_bddPacket, configs);
    _configs = configs;
    _forwardingAnalysis = forwardingAnalysis;
    _dstIpSpaceToBDD = new MemoizedIpSpaceToBDD(_bddPacket.getDstIp(), ImmutableMap.of());
    _srcIpSpaceToBDD = new MemoizedIpSpaceToBDD(_bddPacket.getSrcIp(), ImmutableMap.of());

    _aclPermitBDDs = computeAclBDDs(_bddPacket, _bddSourceManagers, configs);
    _aclDenyBDDs = computeAclDenyBDDs(_aclPermitBDDs);

    _bddIncomingTransformations = computeBDDIncomingTransformations();
    _bddOutgoingTransformations = computeBDDOutgoingTransformations();

    _arpTrueEdgeBDDs = computeArpTrueEdgeBDDs(forwardingAnalysis, _dstIpSpaceToBDD);
    _neighborUnreachableBDDs =
        computeDispositionBDDs(forwardingAnalysis.getNeighborUnreachable(), _dstIpSpaceToBDD);
    _deliveredToSubnetBDDs =
        computeDispositionBDDs(forwardingAnalysis.getDeliveredToSubnet(), _dstIpSpaceToBDD);
    _exitsNetworkBDDs =
        computeDispositionBDDs(forwardingAnalysis.getExitsNetwork(), _dstIpSpaceToBDD);
    _insufficientInfoBDDs =
        computeDispositionBDDs(forwardingAnalysis.getInsufficientInfo(), _dstIpSpaceToBDD);
    _routableBDDs = computeRoutableBDDs(forwardingAnalysis, _dstIpSpaceToBDD);
    _vrfAcceptBDDs = computeVrfAcceptBDDs(configs, _dstIpSpaceToBDD);
    _vrfNotAcceptBDDs = computeVrfNotAcceptBDDs(_vrfAcceptBDDs);

    _dstIpVars = Arrays.stream(_bddPacket.getDstIp().getBitvec()).reduce(_one, BDD::and);
    _sourceIpVars = Arrays.stream(_bddPacket.getSrcIp().getBitvec()).reduce(_one, BDD::and);

    _transformationRanges = computeTransformationRanges();
  }

  /**
   * Lazily compute the ACL BDDs, since we may only need some of them (depending on ignoreFilters,
   * forbidden transit nodes, etc). When ignoreFilters is enabled, we still need the ACLs used in
   * NATs. This is simpler than trying to precompute which ACLs we actually need.
   */
  private static Map<String, Map<String, Supplier<BDD>>> computeAclBDDs(
      BDDPacket bddPacket,
      Map<String, BDDSourceManager> bddSourceManagers,
      Map<String, Configuration> configs) {
    return toImmutableMap(
        configs,
        Entry::getKey,
        nodeEntry -> {
          Configuration config = nodeEntry.getValue();
          IpAccessListToBdd aclToBdd =
              new IpAccessListToBddImpl(
                  bddPacket,
                  bddSourceManagers.get(config.getHostname()),
                  config.getIpAccessLists(),
                  config.getIpSpaces());
          return toImmutableMap(
              config.getIpAccessLists(),
              Entry::getKey,
              aclEntry -> Suppliers.memoize(() -> aclToBdd.toBdd(aclEntry.getValue())));
        });
  }

  private static Map<String, Map<String, Supplier<BDD>>> computeAclDenyBDDs(
      Map<String, Map<String, Supplier<BDD>>> aclBDDs) {
    return toImmutableMap(
        aclBDDs,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                aclEntry -> Suppliers.memoize(() -> aclEntry.getValue().get().not())));
  }

  private TransformationToTransition initTransformationToTransformation(Configuration node) {
    IpSpaceToBDD dstIpSpaceToBdd =
        new MemoizedIpSpaceToBDD(_bddPacket.getDstIp(), node.getIpSpaces());
    IpSpaceToBDD srcIpSpaceToBdd =
        new MemoizedIpSpaceToBDD(_bddPacket.getSrcIp(), node.getIpSpaces());
    return new TransformationToTransition(
        _bddPacket,
        new IpAccessListToBddImpl(
            _bddPacket,
            _bddSourceManagers.get(node.getHostname()),
            new HeaderSpaceToBDD(_bddPacket, dstIpSpaceToBdd, srcIpSpaceToBdd),
            node.getIpAccessLists()));
  }

  private Map<String, Map<String, Transition>> computeBDDIncomingTransformations() {
    return toImmutableMap(
        _configs,
        Entry::getKey, /* node */
        nodeEntry -> {
          Configuration node = nodeEntry.getValue();
          TransformationToTransition toTransition = initTransformationToTransformation(node);
          return toImmutableMap(
              node.getAllInterfaces(),
              Entry::getKey, /* iface */
              ifaceEntry ->
                  toTransition.toTransition(ifaceEntry.getValue().getIncomingTransformation()));
        });
  }

  private Map<String, Map<String, Transition>> computeBDDOutgoingTransformations() {
    return toImmutableMap(
        _configs,
        Entry::getKey, /* node */
        nodeEntry -> {
          Configuration node = nodeEntry.getValue();
          TransformationToTransition toTransition = initTransformationToTransformation(node);
          return toImmutableMap(
              nodeEntry.getValue().getAllInterfaces(),
              Entry::getKey, /* iface */
              ifaceEntry ->
                  toTransition.toTransition(ifaceEntry.getValue().getOutgoingTransformation()));
        });
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

  BDD getRequiredTransitNodeBDD() {
    return _requiredTransitNodeBDD;
  }

  private static Map<org.batfish.datamodel.Edge, BDD> computeArpTrueEdgeBDDs(
      ForwardingAnalysis forwardingAnalysis, IpSpaceToBDD ipSpaceToBDD) {
    return toImmutableMap(
        forwardingAnalysis.getArpTrueEdge(),
        Entry::getKey,
        entry -> entry.getValue().accept(ipSpaceToBDD));
  }

  private static Map<String, Map<String, Map<String, BDD>>> computeDispositionBDDs(
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaceMap, IpSpaceToBDD ipSpaceToBDD) {
    return toImmutableMap(
        ipSpaceMap,
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

  private Stream<Edge> generateRootEdges(Map<StateExpr, BDD> rootBdds) {
    return Streams.concat(
        generateRootEdges_OriginateInterfaceLink_PreInInterface(rootBdds),
        generateRootEdges_OriginateVrf_PostInVrf(rootBdds));
  }

  private static Stream<Edge> generateQueryEdges(Set<FlowDisposition> actions) {
    return actions.stream()
        .map(
            action -> {
              switch (action) {
                case ACCEPTED:
                  return new Edge(Accept.INSTANCE, Query.INSTANCE);
                case DENIED_IN:
                  return new Edge(DropAclIn.INSTANCE, Query.INSTANCE);
                case DENIED_OUT:
                  return new Edge(DropAclOut.INSTANCE, Query.INSTANCE);
                case LOOP:
                  throw new BatfishException("FlowDisposition LOOP is unsupported");
                case NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK:
                  throw new BatfishException(
                      "FlowDisposition NEIGHBOR_UNREACHABLE_OR_EXITS is unsupported");
                case NEIGHBOR_UNREACHABLE:
                  return new Edge(NeighborUnreachable.INSTANCE, Query.INSTANCE);
                case DELIVERED_TO_SUBNET:
                  return new Edge(DeliveredToSubnet.INSTANCE, Query.INSTANCE);
                case EXITS_NETWORK:
                  return new Edge(ExitsNetwork.INSTANCE, Query.INSTANCE);
                case INSUFFICIENT_INFO:
                  return new Edge(InsufficientInfo.INSTANCE, Query.INSTANCE);
                case NO_ROUTE:
                  return new Edge(DropNoRoute.INSTANCE, Query.INSTANCE);
                case NULL_ROUTED:
                  return new Edge(DropNullRoute.INSTANCE, Query.INSTANCE);
                default:
                  throw new BatfishException("Unknown FlowDisposition " + action.toString());
              }
            });
  }

  private Stream<Edge> generateRootEdges_OriginateInterfaceLink_PreInInterface(
      Map<StateExpr, BDD> rootBdds) {
    return rootBdds.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof OriginateInterfaceLink)
        .map(
            entry -> {
              OriginateInterfaceLink originateInterfaceLink =
                  (OriginateInterfaceLink) entry.getKey();
              String hostname = originateInterfaceLink.getHostname();
              String iface = originateInterfaceLink.getIface();
              PreInInterface preInInterface = new PreInInterface(hostname, iface);

              BDD rootBdd = entry.getValue();

              return new Edge(
                  originateInterfaceLink,
                  preInInterface,
                  compose(
                      addSourceInterfaceConstraint(_bddSourceManagers.get(hostname), iface),
                      constraint(rootBdd)));
            });
  }

  private Stream<Edge> generateRootEdges_OriginateVrf_PostInVrf(Map<StateExpr, BDD> rootBdds) {
    return rootBdds.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof OriginateVrf)
        .map(
            entry -> {
              OriginateVrf originateVrf = (OriginateVrf) entry.getKey();
              String hostname = originateVrf.getHostname();
              String vrf = originateVrf.getVrf();
              PostInVrf postInVrf = new PostInVrf(hostname, vrf);
              BDD rootBdd = entry.getValue();
              return new Edge(
                  originateVrf,
                  postInVrf,
                  compose(
                      addOriginatingFromDeviceConstraint(_bddSourceManagers.get(hostname)),
                      constraint(rootBdd)));
            });
  }

  /*
   * These edges do not depend on the query. Compute them separately so that we can later cache them
   * across queries if we want to.
   */
  private Stream<Edge> generateEdges(Set<String> finalNodes) {
    return Streams.concat(
        generateRules_NodeAccept_Accept(finalNodes),
        generateRules_NodeDropAclIn_DropAclIn(finalNodes),
        generateRules_NodeDropAclOut_DropAclOut(finalNodes),
        generateRules_NodeDropNoRoute_DropNoRoute(finalNodes),
        generateRules_NodeDropNullRoute_DropNullRoute(finalNodes),
        generateRules_NodeInterfaceDeliveredToSubnet_DeliveredToSubnet(finalNodes),
        generateRules_NodeInterfaceExitsNetwork_ExitsNetwork(finalNodes),
        generateRules_NodeInterfaceInsufficientInfo_InsufficientInfo(finalNodes),
        generateRules_NodeInterfaceNeighborUnreachable_NeighborUnreachable(finalNodes),
        generateRules_PreInInterface_NodeDropAclIn(),
        generateRules_PreInInterface_PostInInterface(),
        generateRules_PostInInterface_NodeDropAclIn(),
        generateRules_PostInInterface_PostInVrf(),
        generateRules_PostInVrf_NodeAccept(),
        generateRules_PostInVrf_NodeDropNoRoute(),
        generateRules_PostInVrf_PreOutVrf(),
        generateRules_PreOutEdge_NodeDropAclOut(),
        generateRules_PreOutEdge_PreOutEdgePostNat(),
        generateRules_PreOutEdgePostNat_NodeDropAclOut(),
        generateRules_PreOutEdgePostNat_PreInInterface(),
        generateRules_PreOutInterfaceDisposition_NodeInterfaceDisposition(),
        generateRules_PreOutInterfaceDisposition_NodeDropAclOut(),
        generateRules_PreOutVrf_NodeDropNullRoute(),
        generateRules_PreOutVrf_PreOutInterfaceDisposition(),
        generateRules_PreOutVrf_PreOutEdge());
  }

  private static Stream<Edge> generateRules_NodeAccept_Accept(Set<String> finalNodes) {
    return finalNodes.stream().map(node -> new Edge(new NodeAccept(node), Accept.INSTANCE));
  }

  private static Stream<Edge> generateRules_NodeDropAclIn_DropAclIn(Set<String> finalNodes) {
    return finalNodes.stream().map(node -> new Edge(new NodeDropAclIn(node), DropAclIn.INSTANCE));
  }

  private static Stream<Edge> generateRules_NodeDropAclOut_DropAclOut(Set<String> finalNodes) {
    return finalNodes.stream().map(node -> new Edge(new NodeDropAclOut(node), DropAclOut.INSTANCE));
  }

  private static Stream<Edge> generateRules_NodeDropNoRoute_DropNoRoute(Set<String> finalNodes) {
    return finalNodes.stream()
        .map(node -> new Edge(new NodeDropNoRoute(node), DropNoRoute.INSTANCE));
  }

  private static Stream<Edge> generateRules_NodeDropNullRoute_DropNullRoute(
      Set<String> finalNodes) {
    return finalNodes.stream()
        .map(node -> new Edge(new NodeDropNullRoute(node), DropNullRoute.INSTANCE));
  }

  private Stream<Edge> generateRules_NodeInterfaceDisposition_Disposition(
      BiFunction<String, String, StateExpr> nodeInterfaceDispositionConstructor,
      StateExpr dispositionNode,
      Set<String> finalNodes) {
    return finalNodes.stream()
        .map(_configs::get)
        .filter(Objects::nonNull) // remove finalNodes that don't exist on this network
        .flatMap(c -> c.getAllInterfaces().values().stream())
        .map(
            iface -> {
              String nodeNode = iface.getOwner().getHostname();
              String ifaceName = iface.getName();
              return new Edge(
                  nodeInterfaceDispositionConstructor.apply(nodeNode, ifaceName), dispositionNode);
            });
  }

  private Stream<Edge> generateRules_NodeInterfaceNeighborUnreachable_NeighborUnreachable(
      Set<String> finalNodes) {
    return generateRules_NodeInterfaceDisposition_Disposition(
        NodeInterfaceNeighborUnreachable::new, NeighborUnreachable.INSTANCE, finalNodes);
  }

  private Stream<Edge> generateRules_NodeInterfaceDeliveredToSubnet_DeliveredToSubnet(
      Set<String> finalNodes) {
    return generateRules_NodeInterfaceDisposition_Disposition(
        NodeInterfaceDeliveredToSubnet::new, DeliveredToSubnet.INSTANCE, finalNodes);
  }

  private Stream<Edge> generateRules_NodeInterfaceExitsNetwork_ExitsNetwork(
      Set<String> finalNodes) {
    return generateRules_NodeInterfaceDisposition_Disposition(
        NodeInterfaceExitsNetwork::new, ExitsNetwork.INSTANCE, finalNodes);
  }

  private Stream<Edge> generateRules_NodeInterfaceInsufficientInfo_InsufficientInfo(
      Set<String> finalNodes) {
    return generateRules_NodeInterfaceDisposition_Disposition(
        NodeInterfaceInsufficientInfo::new, InsufficientInfo.INSTANCE, finalNodes);
  }

  private Stream<Edge> generateRules_PostInInterface_NodeDropAclIn() {
    return _configs.values().stream()
        .map(Configuration::getVrfs)
        .map(Map::values)
        .flatMap(Collection::stream)
        .flatMap(vrf -> vrf.getInterfaces().values().stream())
        .filter(iface -> iface.getPostTransformationIncomingFilter() != null)
        .map(
            i -> {
              String acl = i.getPostTransformationIncomingFilterName();
              String node = i.getOwner().getHostname();
              String iface = i.getName();

              BDD aclDenyBDD = ignorableAclDenyBDD(node, acl);
              return new Edge(
                  new PostInInterface(node, iface),
                  new NodeDropAclIn(node),
                  compose(
                      constraint(aclDenyBDD),
                      removeSourceConstraint(_bddSourceManagers.get(node))));
            });
  }

  private Stream<Edge> generateRules_PostInInterface_PostInVrf() {
    return _configs.values().stream()
        .map(Configuration::getVrfs)
        .map(Map::values)
        .flatMap(Collection::stream)
        .flatMap(vrf -> vrf.getInterfaces().values().stream())
        .map(
            iface -> {
              String aclName = iface.getPostTransformationIncomingFilterName();
              String nodeName = iface.getOwner().getHostname();
              String vrfName = iface.getVrfName();
              String ifaceName = iface.getName();

              PostInInterface preState = new PostInInterface(nodeName, ifaceName);
              PostInVrf postState = new PostInVrf(nodeName, vrfName);

              BDD inAclBDD = ignorableAclPermitBDD(nodeName, aclName);
              return new Edge(preState, postState, constraint(inAclBDD));
            });
  }

  private Stream<Edge> generateRules_PostInVrf_NodeAccept() {
    return _vrfAcceptBDDs.entrySet().stream()
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD acceptBDD = vrfEntry.getValue();
                          return new Edge(
                              new PostInVrf(node, vrf),
                              new NodeAccept(node),
                              compose(
                                  constraint(acceptBDD),
                                  removeSourceConstraint(_bddSourceManagers.get(node))));
                        }));
  }

  private Stream<Edge> generateRules_PostInVrf_NodeDropNoRoute() {
    return _vrfNotAcceptBDDs.entrySet().stream()
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD notAcceptBDD = vrfEntry.getValue();
                          BDD notRoutableBDD = _routableBDDs.get(node).get(vrf).not();
                          return new Edge(
                              new PostInVrf(node, vrf),
                              new NodeDropNoRoute(node),
                              compose(
                                  constraint(notAcceptBDD.and(notRoutableBDD)),
                                  removeSourceConstraint(_bddSourceManagers.get(node))));
                        }));
  }

  private Stream<Edge> generateRules_PostInVrf_PreOutVrf() {
    return _vrfNotAcceptBDDs.entrySet().stream()
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
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
    return _configs.values().stream()
        .map(Configuration::getVrfs)
        .map(Map::values)
        .flatMap(Collection::stream)
        .flatMap(vrf -> vrf.getInterfaces().values().stream())
        .filter(iface -> iface.getIncomingFilter() != null)
        .map(
            i -> {
              String acl = i.getIncomingFilterName();
              String node = i.getOwner().getHostname();
              String iface = i.getName();

              BDD aclDenyBDD = ignorableAclDenyBDD(node, acl);
              return new Edge(
                  new PreInInterface(node, iface),
                  new NodeDropAclIn(node),
                  compose(
                      constraint(aclDenyBDD),
                      removeSourceConstraint(_bddSourceManagers.get(node))));
            });
  }

  private BDD aclDenyBDD(String node, @Nullable String acl) {
    return acl == null ? _zero : _aclDenyBDDs.get(node).get(acl).get();
  }

  private BDD aclPermitBDD(String node, @Nullable String acl) {
    return acl == null ? _one : _aclPermitBDDs.get(node).get(acl).get();
  }

  private BDD ignorableAclDenyBDD(String node, @Nullable String acl) {
    return _ignoreFilters ? _zero : aclDenyBDD(node, acl);
  }

  private BDD ignorableAclPermitBDD(String node, @Nullable String acl) {
    return _ignoreFilters ? _one : aclPermitBDD(node, acl);
  }

  private Stream<Edge> generateRules_PreInInterface_PostInInterface() {
    return _configs.values().stream()
        .map(Configuration::getVrfs)
        .map(Map::values)
        .flatMap(Collection::stream)
        .flatMap(vrf -> vrf.getInterfaces().values().stream())
        .map(
            iface -> {
              String aclName = iface.getIncomingFilterName();
              String nodeName = iface.getOwner().getHostname();
              String ifaceName = iface.getName();

              PreInInterface preState = new PreInInterface(nodeName, ifaceName);
              PostInInterface postState = new PostInInterface(nodeName, ifaceName);

              BDD inAclBDD = ignorableAclPermitBDD(nodeName, aclName);

              Transition transition =
                  compose(
                      constraint(inAclBDD),
                      _bddIncomingTransformations.get(nodeName).get(ifaceName));
              return new Edge(preState, postState, transition);
            });
  }

  private Stream<Edge> generateRules_PreOutEdge_NodeDropAclOut() {
    if (_ignoreFilters) {
      return Stream.of();
    }
    return _forwardingAnalysis.getArpTrueEdge().keySet().stream()
        .flatMap(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              String preNatAcl =
                  _configs
                      .get(node1)
                      .getAllInterfaces()
                      .get(iface1)
                      .getPreTransformationOutgoingFilterName();

              BDD denyPreNat = ignorableAclDenyBDD(node1, preNatAcl);
              if (denyPreNat.equals(_zero)) {
                return Stream.of();
              }
              return Stream.of(
                  new Edge(
                      new PreOutEdge(node1, iface1, node2, iface2),
                      new NodeDropAclOut(node1),
                      compose(
                          constraint(denyPreNat),
                          removeSourceConstraint(_bddSourceManagers.get(node1)))));
            });
  }

  private Stream<Edge> generateRules_PreOutEdge_PreOutEdgePostNat() {
    return _forwardingAnalysis.getArpTrueEdge().keySet().stream()
        .flatMap(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              String preNatAcl =
                  _configs
                      .get(node1)
                      .getAllInterfaces()
                      .get(iface1)
                      .getPreTransformationOutgoingFilterName();

              BDD aclPermit = ignorableAclPermitBDD(node1, preNatAcl);
              if (aclPermit.equals(_zero)) {
                return Stream.of();
              }
              PreOutEdge preState = new PreOutEdge(node1, iface1, node2, iface2);
              PreOutEdgePostNat postState = new PreOutEdgePostNat(node1, iface1, node2, iface2);
              Transition transition =
                  compose(
                      constraint(aclPermit), _bddOutgoingTransformations.get(node1).get(iface1));
              return Stream.of(new Edge(preState, postState, transition));
            });
  }

  private Stream<Edge> generateRules_PreOutEdgePostNat_NodeDropAclOut() {
    if (_ignoreFilters) {
      return Stream.of();
    }
    return _forwardingAnalysis.getArpTrueEdge().keySet().stream()
        .flatMap(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              String aclName =
                  _configs.get(node1).getAllInterfaces().get(iface1).getOutgoingFilterName();

              if (aclName == null) {
                return Stream.of();
              }

              BDD aclDenyBDD = ignorableAclDenyBDD(node1, aclName);
              return Stream.of(
                  new Edge(
                      new PreOutEdgePostNat(node1, iface1, node2, iface2),
                      new NodeDropAclOut(node1),
                      compose(
                          constraint(aclDenyBDD),
                          removeSourceConstraint(_bddSourceManagers.get(node1)))));
            });
  }

  private Stream<Edge> generateRules_PreOutEdgePostNat_PreInInterface() {
    return _forwardingAnalysis.getArpTrueEdge().keySet().stream()
        .map(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              BDD aclPermitBDD =
                  ignorableAclPermitBDD(
                      node1,
                      _configs.get(node1).getAllInterfaces().get(iface1).getOutgoingFilterName());
              assert aclPermitBDD != null;

              return new Edge(
                  new PreOutEdgePostNat(node1, iface1, node2, iface2),
                  new PreInInterface(node2, iface2),
                  compose(
                      constraint(aclPermitBDD),
                      removeSourceConstraint(_bddSourceManagers.get(node1)),
                      addSourceInterfaceConstraint(_bddSourceManagers.get(node2), iface2)));
            });
  }

  private Stream<Edge> generateRules_PreOutInterfaceDisposition_NodeDropAclOut() {
    if (_ignoreFilters) {
      return Stream.of();
    }

    return _configs.entrySet().stream()
        .flatMap(
            nodeEntry -> {
              String node = nodeEntry.getKey();
              return nodeEntry.getValue().getVrfs().entrySet().stream()
                  .flatMap(
                      vrfEntry -> {
                        StateExpr postState = new NodeDropAclOut(node);
                        return vrfEntry.getValue().getInterfaces().values().stream()
                            .filter(iface -> iface.getOutgoingFilterName() != null)
                            .flatMap(
                                iface -> {
                                  String ifaceName = iface.getName();
                                  BDD denyPreAclBDD =
                                      ignorableAclDenyBDD(
                                          node, iface.getPreTransformationOutgoingFilterName());
                                  BDD permitPreAclBDD =
                                      ignorableAclPermitBDD(
                                          node, iface.getPreTransformationOutgoingFilterName());
                                  BDD denyPostAclBDD =
                                      ignorableAclDenyBDD(node, iface.getOutgoingFilterName());
                                  Transition transformation =
                                      _bddOutgoingTransformations.get(node).get(ifaceName);

                                  Transition transition =
                                      compose(
                                          or(
                                              constraint(denyPreAclBDD),
                                              compose(
                                                  constraint(permitPreAclBDD),
                                                  transformation,
                                                  constraint(denyPostAclBDD))),
                                          removeSourceConstraint(_bddSourceManagers.get(node)));

                                  return Stream.of(
                                          new PreOutInterfaceDeliveredToSubnet(node, ifaceName),
                                          new PreOutInterfaceExitsNetwork(node, ifaceName),
                                          new PreOutInterfaceInsufficientInfo(node, ifaceName),
                                          new PreOutInterfaceNeighborUnreachable(node, ifaceName))
                                      .map(preState -> new Edge(preState, postState, transition));
                                });
                      });
            });
  }

  private Stream<Edge> generateRules_PreOutVrf_NodeDropNullRoute() {
    return _forwardingAnalysis.getNullRoutedIps().entrySet().stream()
        .flatMap(
            nodeEntry ->
                nodeEntry.getValue().entrySet().stream()
                    .map(
                        vrfEntry -> {
                          String node = nodeEntry.getKey();
                          String vrf = vrfEntry.getKey();
                          BDD nullRoutedBDD = vrfEntry.getValue().accept(_dstIpSpaceToBDD);
                          return new Edge(
                              new PreOutVrf(node, vrf),
                              new NodeDropNullRoute(node),
                              compose(
                                  constraint(nullRoutedBDD),
                                  removeSourceConstraint(_bddSourceManagers.get(node))));
                        }));
  }

  private Stream<Edge> generateRules_PreOutVrf_PreOutInterfaceDisposition() {
    return _configs.values().stream()
        .flatMap(node -> node.getAllInterfaces().values().stream())
        .flatMap(
            iface -> {
              String hostname = iface.getOwner().getHostname();
              String ifaceName = iface.getName();
              String vrf = iface.getVrfName();

              StateExpr preState = new PreOutVrf(hostname, vrf);

              Stream.Builder<Edge> builder = Stream.builder();

              // delivered to subnet
              BDD deliveredToSubnet = _deliveredToSubnetBDDs.get(hostname).get(vrf).get(ifaceName);
              if (!deliveredToSubnet.isZero()) {
                builder.add(
                    new Edge(
                        preState,
                        new PreOutInterfaceDeliveredToSubnet(hostname, ifaceName),
                        deliveredToSubnet));
              }

              BDD exitsNetwork = _exitsNetworkBDDs.get(hostname).get(vrf).get(ifaceName);
              if (!exitsNetwork.isZero()) {
                builder.add(
                    new Edge(
                        preState,
                        new PreOutInterfaceExitsNetwork(hostname, ifaceName),
                        exitsNetwork));
              }

              BDD insufficientInfo = _insufficientInfoBDDs.get(hostname).get(vrf).get(ifaceName);
              if (!insufficientInfo.isZero()) {
                builder.add(
                    new Edge(
                        preState,
                        new PreOutInterfaceInsufficientInfo(hostname, ifaceName),
                        insufficientInfo));
              }

              BDD neighborUnreachable =
                  _neighborUnreachableBDDs.get(hostname).get(vrf).get(ifaceName);
              if (!neighborUnreachable.isZero()) {
                builder.add(
                    new Edge(
                        preState,
                        new PreOutInterfaceNeighborUnreachable(hostname, ifaceName),
                        neighborUnreachable));
              }
              return builder.build();
            });
  }

  private Stream<Edge> generateRules_PreOutInterfaceDisposition_NodeInterfaceDisposition() {
    return _configs.values().stream()
        .flatMap(config -> config.getAllInterfaces().values().stream())
        .flatMap(
            iface -> {
              String node = iface.getOwner().getHostname();
              String ifaceName = iface.getName();
              BDD permitBeforeNatBDD =
                  ignorableAclPermitBDD(node, iface.getPreTransformationOutgoingFilterName());
              BDD permitAfterNatBDD = ignorableAclPermitBDD(node, iface.getOutgoingFilterName());
              Transition outgoingTransformation =
                  _bddOutgoingTransformations.get(node).get(ifaceName);

              if (permitBeforeNatBDD.isZero() || permitAfterNatBDD.isZero()) {
                return Stream.of();
              }

              /* 1. pre-transformation filter
               * 2. outgoing transformation
               * 3. post-transformation filter
               * 4. erase source constraint
               */
              Transition transition =
                  compose(
                      constraint(permitBeforeNatBDD),
                      outgoingTransformation,
                      constraint(permitAfterNatBDD),
                      removeSourceConstraint(_bddSourceManagers.get(node)));

              return Stream.of(
                  new Edge(
                      new PreOutInterfaceDeliveredToSubnet(node, ifaceName),
                      new NodeInterfaceDeliveredToSubnet(node, ifaceName),
                      transition),
                  new Edge(
                      new PreOutInterfaceExitsNetwork(node, ifaceName),
                      new NodeInterfaceExitsNetwork(node, ifaceName),
                      transition),
                  new Edge(
                      new PreOutInterfaceInsufficientInfo(node, ifaceName),
                      new NodeInterfaceInsufficientInfo(node, ifaceName),
                      transition),
                  new Edge(
                      new PreOutInterfaceNeighborUnreachable(node, ifaceName),
                      new NodeInterfaceNeighborUnreachable(node, ifaceName),
                      transition));
            });
  }

  private Stream<Edge> generateRules_PreOutVrf_PreOutEdge() {
    return _arpTrueEdgeBDDs.entrySet().stream()
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
  /*
   * Optional.none is used when the location is not in the snapshot. This can happen in differential reachability,
   * for example.
   */
  private LocationVisitor<Optional<StateExpr>> getLocationToStateExpr() {
    return new LocationVisitor<Optional<StateExpr>>() {
      @Override
      public Optional<StateExpr> visitInterfaceLinkLocation(
          @Nonnull InterfaceLinkLocation interfaceLinkLocation) {
        return Optional.of(
            new OriginateInterfaceLink(
                interfaceLinkLocation.getNodeName(), interfaceLinkLocation.getInterfaceName()));
      }

      @Override
      public Optional<StateExpr> visitInterfaceLocation(
          @Nonnull InterfaceLocation interfaceLocation) {
        Configuration config = _configs.get(interfaceLocation.getNodeName());
        if (config == null) {
          return Optional.empty();
        }
        Interface iface = config.getAllInterfaces().get(interfaceLocation.getInterfaceName());
        if (iface == null) {
          return Optional.empty();
        }
        String vrf = iface.getVrfName();
        return Optional.of(new OriginateVrf(interfaceLocation.getNodeName(), vrf));
      }
    };
  }

  public BDDReachabilityAnalysis bddReachabilityAnalysis(IpSpaceAssignment srcIpSpaceAssignment) {
    return bddReachabilityAnalysis(
        srcIpSpaceAssignment,
        matchDst(UniverseIpSpace.INSTANCE),
        ImmutableSet.of(),
        ImmutableSet.of(),
        _configs.keySet(),
        ImmutableSet.of(FlowDisposition.ACCEPTED));
  }

  /**
   * Given a set of parameters finds a {@link Map} of {@link IngressLocation}s to {@link BDD}s while
   * including the results for {@link FlowDisposition#LOOP} if required
   *
   * @param srcIpSpaceAssignment An assignment of active source locations to the corresponding
   *     source {@link IpSpace}.
   * @param initialHeaderSpace The initial headerspace (i.e. before any packet transformations).
   * @param forbiddenTransitNodes A set of hostnames that must not be transited.
   * @param requiredTransitNodes A set of hostnames of which one must be transited.
   * @param finalNodes Find flows that stop at one of these nodes.
   * @param actions Find flows for which at least one trace has one of these actions.
   * @return {@link Map} of {@link IngressLocation}s to {@link BDD}s
   */
  public Map<IngressLocation, BDD> getAllBDDs(
      IpSpaceAssignment srcIpSpaceAssignment,
      AclLineMatchExpr initialHeaderSpace,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<String> finalNodes,
      Set<FlowDisposition> actions) {
    Set<FlowDisposition> actionsCopy = new HashSet<>(actions);

    boolean loopIncluded = actionsCopy.remove(FlowDisposition.LOOP);

    // detecting Loops can work with any disposition, using ACCEPTED arbitrarily here
    BDDReachabilityAnalysis analysis =
        bddReachabilityAnalysis(
            srcIpSpaceAssignment,
            initialHeaderSpace,
            forbiddenTransitNodes,
            requiredTransitNodes,
            finalNodes,
            actionsCopy.isEmpty() ? ImmutableSet.of(FlowDisposition.ACCEPTED) : actionsCopy);

    Map<IngressLocation, BDD> bddsNoLoop =
        actionsCopy.isEmpty() ? Maps.newHashMap() : analysis.getIngressLocationReachableBDDs();

    if (!loopIncluded) {
      return bddsNoLoop;
    } else {
      Map<IngressLocation, BDD> bddsLoop = analysis.detectLoops();

      // merging the two BDDs
      Map<IngressLocation, BDD> mergedBDDs = new HashMap<>(bddsNoLoop);
      bddsLoop.forEach(((ingressLocation, bdd) -> mergedBDDs.merge(ingressLocation, bdd, BDD::or)));
      return mergedBDDs;
    }
  }

  /**
   * Create a {@link BDDReachabilityAnalysis} with the specified parameters.
   *
   * @param srcIpSpaceAssignment An assignment of active source locations to the corresponding
   *     source {@link IpSpace}.
   * @param initialHeaderSpace The initial headerspace (i.e. before any packet transformations).
   * @param forbiddenTransitNodes A set of hostnames that must not be transited.
   * @param requiredTransitNodes A set of hostnames of which one must be transited.
   * @param finalNodes Find flows that stop at one of these nodes.
   * @param actions Find flows for which at least one trace has one of these actions.
   */
  @VisibleForTesting
  BDDReachabilityAnalysis bddReachabilityAnalysis(
      IpSpaceAssignment srcIpSpaceAssignment,
      AclLineMatchExpr initialHeaderSpace,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<String> finalNodes,
      Set<FlowDisposition> actions) {
    IpAccessListToBdd ipAccessListToBdd =
        new IpAccessListToBddImpl(
            _bddPacket,
            BDDSourceManager.forInterfaces(_bddPacket, ImmutableSet.of()),
            ImmutableMap.of(),
            ImmutableMap.of());
    BDD initialHeaderSpaceBdd = ipAccessListToBdd.toBdd(initialHeaderSpace);
    BDD finalHeaderSpaceBdd = computeFinalHeaderSpaceBdd(initialHeaderSpaceBdd);

    Map<StateExpr, BDD> roots = rootConstraints(srcIpSpaceAssignment, initialHeaderSpaceBdd);

    Stream<Edge> edgeStream =
        Streams.concat(
            generateEdges(finalNodes), generateRootEdges(roots), generateQueryEdges(actions));

    edgeStream = instrumentForbiddenTransitNodes(forbiddenTransitNodes, edgeStream);
    edgeStream = instrumentRequiredTransitNodes(requiredTransitNodes, edgeStream);

    List<Edge> edges = edgeStream.collect(ImmutableList.toImmutableList());

    return new BDDReachabilityAnalysis(_bddPacket, roots.keySet(), edges, finalHeaderSpaceBdd);
  }

  /**
   * Compute the space of possible final headers, under the assumption that any NAT rule may be
   * applied.
   */
  @VisibleForTesting
  BDD computeFinalHeaderSpaceBdd(BDD initialHeaderSpaceBdd) {
    BDD finalHeaderSpace = initialHeaderSpaceBdd;

    BDD noDstIp = finalHeaderSpace.exist(_dstIpVars);
    if (!noDstIp.equals(finalHeaderSpace)) {
      // there's a constraint on dst Ip, so include nat pool Ips
      BDD dstTransformationRange = _transformationRanges.getOrDefault(IpField.DESTINATION, _zero);
      if (!dstTransformationRange.isZero()) {
        // dst IP is either the initial one, or one of that NAT pool IPs.
        finalHeaderSpace = finalHeaderSpace.or(noDstIp.and(dstTransformationRange));
      }
    }

    BDD noSrcIp = finalHeaderSpace.exist(_sourceIpVars);
    if (!noSrcIp.equals(finalHeaderSpace)) {
      // there's a constraint on source Ip, so include nat pool Ips
      BDD srcNatPoolIps = _transformationRanges.getOrDefault(IpField.SOURCE, _zero);
      if (!srcNatPoolIps.isZero()) {
        /*
         * In this case, since source IPs usually don't play a huge role in routing, we could just
         * existentially quantify away the constraint. There's a performance trade-off: tighter
         * constraints prune more paths, but are more expensive to operate on.
         */
        finalHeaderSpace = finalHeaderSpace.or(noSrcIp.and(srcNatPoolIps));
      }
    }

    return finalHeaderSpace;
  }

  private Map<IpField, BDD> computeTransformationRanges() {
    HashMap<IpField, BDD> ranges = new HashMap<>();

    TransformationStepVisitor<Void> stepVisitor =
        new TransformationStepVisitor<Void>() {
          private IpSpaceToBDD getIpSpaceToBDD(IpField ipField) {
            switch (ipField) {
              case DESTINATION:
                return _dstIpSpaceToBDD;
              case SOURCE:
                return _srcIpSpaceToBDD;
              default:
                throw new IllegalArgumentException("Unknown IpField " + ipField);
            }
          }

          @Override
          public Void visitAssignIpAddressFromPool(
              AssignIpAddressFromPool assignIpAddressFromPool) {
            IpField ipField = assignIpAddressFromPool.getIpField();
            BDDInteger var = getIpSpaceToBDD(ipField).getBDDInteger();
            BDD bdd =
                assignIpAddressFromPool.getIpRanges().asRanges().stream()
                    .map(
                        range ->
                            var.geq(range.lowerEndpoint().asLong())
                                .and(var.leq(range.upperEndpoint().asLong())))
                    .reduce(var.getFactory().zero(), BDD::or);
            ranges.merge(ipField, bdd, BDD::or);
            return null;
          }

          @Override
          public Void visitNoop(Noop noop) {
            return null;
          }

          @Override
          public Void visitShiftIpAddressIntoSubnet(
              ShiftIpAddressIntoSubnet shiftIpAddressIntoSubnet) {
            IpField ipField = shiftIpAddressIntoSubnet.getIpField();
            BDD bdd = getIpSpaceToBDD(ipField).toBDD(shiftIpAddressIntoSubnet.getSubnet());
            ranges.merge(ipField, bdd, BDD::or);
            return null;
          }

          @Override
          public Void visitAssignPortFromPool(AssignPortFromPool assignPortFromPool) {
            // TODO
            return null;
          }
        };

    _configs
        .values()
        .forEach(
            configuration ->
                configuration
                    .getAllInterfaces()
                    .values()
                    .forEach(
                        iface -> {
                          visitTransformationSteps(iface.getIncomingTransformation(), stepVisitor);
                          visitTransformationSteps(iface.getOutgoingTransformation(), stepVisitor);
                        }));
    return ImmutableMap.copyOf(ranges);
  }

  private Map<StateExpr, BDD> rootConstraints(
      IpSpaceAssignment srcIpSpaceAssignment, BDD initialHeaderSpaceBdd) {
    LocationVisitor<Optional<StateExpr>> locationToStateExpr = getLocationToStateExpr();
    IpSpaceToBDD srcIpSpaceToBDD =
        new MemoizedIpSpaceToBDD(_bddPacket.getSrcIp(), ImmutableMap.of());

    // convert Locations to StateExprs, and merge srcIp constraints
    Map<StateExpr, BDD> rootConstraints = new HashMap<>();
    for (IpSpaceAssignment.Entry entry : srcIpSpaceAssignment.getEntries()) {
      BDD srcIpSpaceBDD = entry.getIpSpace().accept(srcIpSpaceToBDD);
      entry.getLocations().stream()
          .map(locationToStateExpr::visit)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .forEach(root -> rootConstraints.merge(root, srcIpSpaceBDD, BDD::or));
    }

    // add the global initial HeaderSpace and remove unsat entries
    Map<StateExpr, BDD> finalRootConstraints =
        rootConstraints.entrySet().stream()
            .map(
                entry ->
                    Maps.immutableEntry(
                        entry.getKey(), entry.getValue().and(initialHeaderSpaceBdd)))
            .filter(entry -> !entry.getValue().isZero())
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    // make sure there is at least one possible source
    checkArgument(
        !finalRootConstraints.isEmpty(),
        "No sources are compatible with the headerspace constraint");

    return finalRootConstraints;
  }

  private String ifaceVrf(String node, String iface) {
    return _configs.get(node).getAllInterfaces().get(iface).getVrfName();
  }

  private static Map<String, Map<String, BDD>> computeVrfAcceptBDDs(
      Map<String, Configuration> configs, IpSpaceToBDD ipSpaceToBDD) {
    /*
     * excludeInactive: true
     * The VRF should not own (i.e. cannot accept packets destined to) the dest IP inactive interfaces. Forwarding
     * analysis will consider these IPs to be internal to (or owned by) the network, but not owned by any particular
     * device or link.
     */
    Map<String, Map<String, IpSpace>> vrfOwnedIpSpaces =
        TopologyUtil.computeVrfOwnedIpSpaces(
            TopologyUtil.computeIpVrfOwners(true, TopologyUtil.computeNodeInterfaces(configs)));

    return CommonUtil.toImmutableMap(
        configs,
        Entry::getKey,
        nodeEntry ->
            CommonUtil.toImmutableMap(
                nodeEntry.getValue().getVrfs(),
                Entry::getKey,
                vrfEntry ->
                    vrfOwnedIpSpaces
                        .getOrDefault(nodeEntry.getKey(), ImmutableMap.of())
                        .getOrDefault(vrfEntry.getKey(), EmptyIpSpace.INSTANCE)
                        .accept(ipSpaceToBDD)));
  }

  /**
   * Adapt an edge to set the bit indicating that one of the nodes required to be transited has now
   * been transited.
   *
   * <p>Going forward, we erase the previous value of the bit as we enter the edge, then set it to 1
   * as we exit. Going backward, we just erase the bit, since the requirement has been satisfied.
   */
  @VisibleForTesting
  private Edge adaptEdgeSetTransitedBit(Edge edge) {
    return new Edge(
        edge.getPreState(),
        edge.getPostState(),
        compose(
            edge.getTransition(), eraseAndSet(_requiredTransitNodeBDD, _requiredTransitNodeBDD)));
  }

  /**
   * Adapt an edge, applying an additional constraint after traversing the edge (in the forward
   * direction).
   */
  private static Edge andThen(Edge edge, BDD constraint) {
    return new Edge(
        edge.getPreState(),
        edge.getPostState(),
        compose(edge.getTransition(), constraint(constraint)));
  }

  /**
   * Instrumentation to forbid certain nodes from being transited. Simply removes the edges from the
   * graph at which one of those nodes would become transited.
   */
  private Stream<Edge> instrumentForbiddenTransitNodes(
      Set<String> forbiddenTransitNodes, Stream<Edge> edgeStream) {
    if (forbiddenTransitNodes.isEmpty()) {
      return edgeStream;
    }

    // remove any edges at which a forbidden node becomes transited.
    return edgeStream.filter(
        edge ->
            !(edge.getPreState() instanceof PreOutEdgePostNat
                && edge.getPostState() instanceof PreInInterface
                && forbiddenTransitNodes.contains(
                    ((PreOutEdgePostNat) edge.getPreState()).getSrcNode())));
  }

  /**
   * Instrumentation to require that one of a set of nodes is transited. We use a single bit of
   * state in the BDDs to track this. The bit is initialized to 0 at the origination points, and
   * constrained to be 1 at the Query state. When one of the specified nodes becomes transited (i.e.
   * the flow leaves that node and enters another) we set the bit to 1. All other edges in the graph
   * propagate the current value of that bit unchanged.
   */
  private Stream<Edge> instrumentRequiredTransitNodes(
      Set<String> requiredTransitNodes, Stream<Edge> edgeStream) {
    if (requiredTransitNodes.isEmpty()) {
      return edgeStream;
    }

    BDD transited = _requiredTransitNodeBDD;
    BDD notTransited = _requiredTransitNodeBDD.not();

    return edgeStream.map(
        edge -> {
          if (edge.getPreState() instanceof PreOutEdgePostNat
              && edge.getPostState() instanceof PreInInterface) {
            String hostname = ((PreOutEdgePostNat) edge.getPreState()).getSrcNode();
            return requiredTransitNodes.contains(hostname) ? adaptEdgeSetTransitedBit(edge) : edge;
          } else if (edge.getPreState() instanceof OriginateVrf
              || edge.getPreState() instanceof OriginateInterfaceLink) {
            return andThen(edge, notTransited);
          } else if (edge.getPostState() instanceof Query) {
            return andThen(edge, transited);
          } else {
            return edge;
          }
        });
  }

  public Map<String, BDDSourceManager> getBDDSourceManagers() {
    return _bddSourceManagers;
  }

  public Map<String, Map<String, Map<String, BDD>>> getNeighborUnreachableBDDs() {
    return _neighborUnreachableBDDs;
  }

  public Map<String, Map<String, Map<String, BDD>>> getDeliveredToSubnetBDDs() {
    return _deliveredToSubnetBDDs;
  }

  public Map<String, Map<String, Map<String, BDD>>> getExitsNetworkBDDs() {
    return _exitsNetworkBDDs;
  }

  public Map<String, Map<String, Map<String, BDD>>> getInsufficientInfoBDDs() {
    return _insufficientInfoBDDs;
  }
}
