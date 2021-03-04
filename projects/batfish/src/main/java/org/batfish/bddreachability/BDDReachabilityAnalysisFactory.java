package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.alwaysTrue;
import static org.batfish.bddreachability.BidirectionalReachabilityReturnPassInstrumentation.instrumentReturnPassEdges;
import static org.batfish.bddreachability.SessionInstrumentation.sessionInstrumentation;
import static org.batfish.bddreachability.transition.Transitions.IDENTITY;
import static org.batfish.bddreachability.transition.Transitions.ZERO;
import static org.batfish.bddreachability.transition.Transitions.addLastHopConstraint;
import static org.batfish.bddreachability.transition.Transitions.addNoLastHopConstraint;
import static org.batfish.bddreachability.transition.Transitions.addOriginatingFromDeviceConstraint;
import static org.batfish.bddreachability.transition.Transitions.addOutgoingOriginalFlowFiltersConstraint;
import static org.batfish.bddreachability.transition.Transitions.addSourceInterfaceConstraint;
import static org.batfish.bddreachability.transition.Transitions.branch;
import static org.batfish.bddreachability.transition.Transitions.compose;
import static org.batfish.bddreachability.transition.Transitions.constraint;
import static org.batfish.bddreachability.transition.Transitions.eraseAndSet;
import static org.batfish.bddreachability.transition.Transitions.removeNodeSpecificConstraints;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.batfish.datamodel.FlowDisposition.LOOP;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.transformation.TransformationUtil.visitTransformationSteps;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.IpsRoutedOutInterfacesFactory.IpsRoutedOutInterfaces;
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
import org.batfish.common.bdd.MemoizedIpAccessListToBdd;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.VrfExprNameExtractor;
import org.batfish.datamodel.transformation.ApplyAll;
import org.batfish.datamodel.transformation.ApplyAny;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Noop;
import org.batfish.datamodel.transformation.PortField;
import org.batfish.datamodel.transformation.ShiftIpAddressIntoSubnet;
import org.batfish.datamodel.transformation.TransformationStepVisitor;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.LocationVisitor;
import org.batfish.symbolic.IngressLocation;
import org.batfish.symbolic.state.Accept;
import org.batfish.symbolic.state.DeliveredToSubnet;
import org.batfish.symbolic.state.DropAclIn;
import org.batfish.symbolic.state.DropAclOut;
import org.batfish.symbolic.state.DropNoRoute;
import org.batfish.symbolic.state.DropNullRoute;
import org.batfish.symbolic.state.ExitsNetwork;
import org.batfish.symbolic.state.InsufficientInfo;
import org.batfish.symbolic.state.InterfaceAccept;
import org.batfish.symbolic.state.NeighborUnreachable;
import org.batfish.symbolic.state.NodeAccept;
import org.batfish.symbolic.state.NodeDropAclIn;
import org.batfish.symbolic.state.NodeDropAclOut;
import org.batfish.symbolic.state.NodeDropNoRoute;
import org.batfish.symbolic.state.NodeDropNullRoute;
import org.batfish.symbolic.state.NodeInterfaceDeliveredToSubnet;
import org.batfish.symbolic.state.NodeInterfaceExitsNetwork;
import org.batfish.symbolic.state.NodeInterfaceInsufficientInfo;
import org.batfish.symbolic.state.NodeInterfaceNeighborUnreachable;
import org.batfish.symbolic.state.OriginateInterface;
import org.batfish.symbolic.state.OriginateInterfaceLink;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.PbrFibLookup;
import org.batfish.symbolic.state.PostInInterface;
import org.batfish.symbolic.state.PostInVrf;
import org.batfish.symbolic.state.PreInInterface;
import org.batfish.symbolic.state.PreOutEdge;
import org.batfish.symbolic.state.PreOutEdgePostNat;
import org.batfish.symbolic.state.PreOutInterfaceDeliveredToSubnet;
import org.batfish.symbolic.state.PreOutInterfaceExitsNetwork;
import org.batfish.symbolic.state.PreOutInterfaceInsufficientInfo;
import org.batfish.symbolic.state.PreOutInterfaceNeighborUnreachable;
import org.batfish.symbolic.state.PreOutVrf;
import org.batfish.symbolic.state.Query;
import org.batfish.symbolic.state.StateExpr;
import org.batfish.symbolic.state.VrfAccept;

/**
 * Constructs a the reachability graph for {@link BDDReachabilityAnalysis}. The public API is very
 * simple: it provides two methods for constructing {@link BDDReachabilityAnalysis}, depending on
 * whether or not you have a destination Ip constraint.
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
 * origination state), we erase the constraint on source by existential quantification.
 */
@ParametersAreNonnullByDefault
public final class BDDReachabilityAnalysisFactory {
  // node name --> acl name --> set of packets denied by the acl.
  private final Map<String, Map<String, Supplier<BDD>>> _aclDenyBDDs;

  // node name --> acl name --> set of packets permitted by the acl.
  private final Map<String, Map<String, Supplier<BDD>>> _aclPermitBDDs;

  /*
   * node -> vrf -> edge -> set of packets that vrf will forward out that edge successfully,
   * including that the neighbor will respond to ARP.
   */
  private final Map<String, Map<String, Map<org.batfish.datamodel.Edge, BDD>>> _arpTrueEdgeBDDs;

  /*
   * Symbolic variables corresponding to the different packet header fields. We use these to
   * generate new BDD constraints on those fields. Each constraint can be understood as the set
   * of packet headers for which the constraint is satisfied.
   */
  private final BDDPacket _bddPacket;

  @VisibleForTesting final @Nonnull BDDFibGenerator _bddFibGenerator;

  private final Map<String, BDDSourceManager> _bddSourceManagers;
  private final Map<String, BDDOutgoingOriginalFlowFilterManager>
      _bddOutgoingOriginalFlowFilterManagers;

  // Needed when initializing sessions.
  private final @Nullable LastHopOutgoingInterfaceManager _lastHopMgr;

  // node --> iface --> bdd nats
  private Map<String, Map<String, Transition>> _bddIncomingTransformations;
  private final Map<String, Map<String, Transition>> _bddOutgoingTransformations;

  private final Map<String, Configuration> _configs;

  // only use this for IpSpaces that have no references
  private final IpSpaceToBDD _dstIpSpaceToBDD;
  private final IpSpaceToBDD _srcIpSpaceToBDD;

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

  // node --> vrf --> set of packets null-routed by the vrf
  private final @Nonnull Map<String, Map<String, BDD>> _nullRoutedBDDs;

  // node --> vrf --> set of packets routable by the vrf
  private final Map<String, Map<String, BDD>> _routableBDDs;

  // conjunction of the BDD vars encoding source and dest IPs/Ports. Used for existential
  // quantification in source and destination NAT.
  private final BDD _dstIpVars;
  private final BDD _sourceIpVars;
  private final BDD _dstPortVars;
  private final BDD _sourcePortVars;

  // Pre-computed conversions from packet policies to BDDs
  private final Map<String, Map<String, PacketPolicyToBdd>> _convertedPacketPolicies;

  private final Set<org.batfish.datamodel.Edge> _topologyEdges;

  // ranges of IPs in all transformations in the network, per IP address field.
  private final Map<IpField, BDD> _transformationIpRanges;

  // ranges of ports in all transformations in the network, per port field.
  private final Map<PortField, BDD> _transformationPortRanges;

  /** node --&gt; vrf --&gt; interface --&gt; set of packets accepted by the interface */
  private final Map<String, Map<String, Map<String, BDD>>> _ifaceAcceptBDDs;

  /** node --&gt; vrf --&gt; set of packets accepted by the vrf */
  private final Map<String, Map<String, BDD>> _vrfAcceptBDDs;

  // node --> vrf --> nextVrf --> set of packets vrf delegates to nextVrf
  private final Map<String, Map<String, Map<String, BDD>>> _nextVrfBDDs;

  // node --> interface --> vrf
  private final Map<String, Map<String, String>> _interfacesToVrfsMap;

  private final BDD _zero;

  public BDDReachabilityAnalysisFactory(
      BDDPacket packet,
      Map<String, Configuration> configs,
      ForwardingAnalysis forwardingAnalysis,
      IpsRoutedOutInterfacesFactory ipsRoutedOutInterfacesFactory,
      boolean ignoreFilters,
      boolean initializeSessions) {
    Span span = GlobalTracer.get().buildSpan("Construct BDDReachabilityAnalysisFactory").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      _bddPacket = packet;
      _one = packet.getFactory().one();
      _zero = packet.getFactory().zero();
      _ignoreFilters = ignoreFilters;
      _topologyEdges =
          forwardingAnalysis.getArpTrueEdge().values().stream()
              .flatMap(m -> m.values().stream())
              .flatMap(m -> m.keySet().stream())
              .collect(ImmutableSet.toImmutableSet());
      _lastHopMgr =
          initializeSessions
              ? new LastHopOutgoingInterfaceManager(packet, configs, _topologyEdges)
              : null;
      _requiredTransitNodeBDD = _bddPacket.allocateBDDBit("requiredTransitNodes");
      _bddSourceManagers = BDDSourceManager.forNetwork(_bddPacket, configs, initializeSessions);
      if (_ignoreFilters) {
        // If ignoring filters, make all BDDOutgoingOriginalFlowFilterManagers trivial; they should
        // never enforce any constraints.
        BDDOutgoingOriginalFlowFilterManager empty =
            BDDOutgoingOriginalFlowFilterManager.empty(_bddPacket);
        _bddOutgoingOriginalFlowFilterManagers =
            toImmutableMap(configs.keySet(), Function.identity(), k -> empty);
      } else {
        _bddOutgoingOriginalFlowFilterManagers =
            BDDOutgoingOriginalFlowFilterManager.forNetwork(
                _bddPacket, configs, _bddSourceManagers);
      }
      _configs = configs;
      _dstIpSpaceToBDD = _bddPacket.getDstIpSpaceToBDD();
      _srcIpSpaceToBDD = _bddPacket.getSrcIpSpaceToBDD();

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
      _nullRoutedBDDs = computeNullRoutedBDDs(forwardingAnalysis, _dstIpSpaceToBDD);
      _routableBDDs = computeRoutableBDDs(forwardingAnalysis, _dstIpSpaceToBDD);
      _ifaceAcceptBDDs =
          computeIfaceAcceptBDDs(configs, forwardingAnalysis.getAcceptsIps(), _dstIpSpaceToBDD);
      _vrfAcceptBDDs = computeVrfAcceptBDDs(); // must do this after populating _ifaceAcceptBDDs
      _nextVrfBDDs = computeNextVrfBDDs(forwardingAnalysis.getNextVrfIps(), _dstIpSpaceToBDD);
      _interfacesToVrfsMap = computeInterfacesToVrfsMap(configs);

      _convertedPacketPolicies = convertPacketPolicies(configs, ipsRoutedOutInterfacesFactory);

      _dstIpVars = Arrays.stream(_bddPacket.getDstIp().getBitvec()).reduce(_one, BDD::and);
      _sourceIpVars = Arrays.stream(_bddPacket.getSrcIp().getBitvec()).reduce(_one, BDD::and);
      _dstPortVars = Arrays.stream(_bddPacket.getDstPort().getBitvec()).reduce(_one, BDD::and);
      _sourcePortVars = Arrays.stream(_bddPacket.getSrcPort().getBitvec()).reduce(_one, BDD::and);

      RangeComputer rangeComputer = computeTransformationRanges();
      _transformationPortRanges = rangeComputer.getPortRanges();
      _transformationIpRanges = rangeComputer.getIpRanges();

      _bddFibGenerator =
          new BDDFibGenerator(
              _arpTrueEdgeBDDs,
              _neighborUnreachableBDDs,
              _deliveredToSubnetBDDs,
              _exitsNetworkBDDs,
              _insufficientInfoBDDs,
              _ifaceAcceptBDDs,
              _vrfAcceptBDDs,
              _routableBDDs,
              _nextVrfBDDs,
              _nullRoutedBDDs,
              this::flowsLeavingInterface);
    } finally {
      span.finish();
    }
  }

  /**
   * Computes VRF accept BDDs based on interface accept BDDs. Each VRF's accept BDD is the union of
   * its interfaces' accept BDDs.
   */
  private Map<String, Map<String, BDD>> computeVrfAcceptBDDs() {
    return toImmutableMap(
        _ifaceAcceptBDDs,
        Entry::getKey, // node name
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey, // vrf name
                vrfEntry ->
                    _bddPacket
                        .getFactory()
                        .orAll(vrfEntry.getValue().values()))); // vrf's accept BDD
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
    Span span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysisFactory.computeAclBDDs").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          configs,
          Entry::getKey,
          nodeEntry -> {
            Configuration config = nodeEntry.getValue();
            IpAccessListToBdd aclToBdd =
                ipAccessListToBdd(bddPacket, bddSourceManagers.get(config.getHostname()), config);
            return toImmutableMap(
                config.getIpAccessLists(),
                Entry::getKey,
                aclEntry -> Suppliers.memoize(() -> aclToBdd.toBdd(aclEntry.getValue())));
          });
    } finally {
      span.finish();
    }
  }

  Map<String, Map<String, Supplier<BDD>>> getAclPermitBdds() {
    return _aclPermitBDDs;
  }

  IpAccessListToBdd ipAccessListToBdd(Configuration config) {
    return ipAccessListToBdd(_bddPacket, _bddSourceManagers.get(config.getHostname()), config);
  }

  private static IpAccessListToBdd ipAccessListToBdd(
      BDDPacket bddPacket, BDDSourceManager srcMgr, Configuration config) {
    return new IpAccessListToBddImpl(
        bddPacket, srcMgr, config.getIpAccessLists(), config.getIpSpaces());
  }

  private static Map<String, Map<String, Supplier<BDD>>> computeAclDenyBDDs(
      Map<String, Map<String, Supplier<BDD>>> aclBDDs) {
    Span span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysisFactory.computeAclDenyBDDs").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          aclBDDs,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  aclEntry -> Suppliers.memoize(() -> aclEntry.getValue().get().not())));
    } finally {
      span.finish();
    }
  }

  private TransformationToTransition initTransformationToTransformation(Configuration node) {
    return new TransformationToTransition(
        _bddPacket,
        new IpAccessListToBddImpl(
            _bddPacket,
            _bddSourceManagers.get(node.getHostname()),
            new HeaderSpaceToBDD(_bddPacket, node.getIpSpaces()),
            node.getIpAccessLists()));
  }

  private Map<String, Map<String, Transition>> computeBDDIncomingTransformations() {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeBDDIncomingTransformations")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          _configs,
          Entry::getKey, /* node */
          nodeEntry -> {
            Configuration node = nodeEntry.getValue();
            TransformationToTransition toTransition = initTransformationToTransformation(node);
            return toImmutableMap(
                node.getActiveInterfaces(),
                Entry::getKey, /* iface */
                ifaceEntry ->
                    toTransition.toTransition(ifaceEntry.getValue().getIncomingTransformation()));
          });
    } finally {
      span.finish();
    }
  }

  private Map<String, Map<String, Transition>> computeBDDOutgoingTransformations() {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeBDDOutgoingTransformations")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          _configs,
          Entry::getKey, /* node */
          nodeEntry -> {
            Configuration node = nodeEntry.getValue();
            TransformationToTransition toTransition = initTransformationToTransformation(node);
            return toImmutableMap(
                nodeEntry.getValue().getActiveInterfaces(),
                Entry::getKey, /* iface */
                ifaceEntry ->
                    toTransition.toTransition(ifaceEntry.getValue().getOutgoingTransformation()));
          });
    } finally {
      span.finish();
    }
  }

  private static @Nonnull Map<String, Map<String, BDD>> computeNullRoutedBDDs(
      ForwardingAnalysis forwardingAnalysis, IpSpaceToBDD ipSpaceToBDD) {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeNullRoutedBDDs")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          forwardingAnalysis.getNullRoutedIps(),
          Entry::getKey /* hostname */,
          nullRoutedIpsByNodeVrfEntry ->
              toImmutableMap(
                  nullRoutedIpsByNodeVrfEntry.getValue(),
                  Entry::getKey /* vrf */,
                  nullRoutedIpsByVrfEntry ->
                      nullRoutedIpsByVrfEntry.getValue().accept(ipSpaceToBDD)));
    } finally {
      span.finish();
    }
  }

  private static Map<String, Map<String, BDD>> computeRoutableBDDs(
      ForwardingAnalysis forwardingAnalysis, IpSpaceToBDD ipSpaceToBDD) {
    Span span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysisFactory.computeRoutableBDDs").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          forwardingAnalysis.getRoutableIps(),
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry -> vrfEntry.getValue().accept(ipSpaceToBDD)));
    } finally {
      span.finish();
    }
  }

  /** For all configs/interfaces that have PBR policy defined, convert the packet policy to BDDs */
  private Map<String, Map<String, PacketPolicyToBdd>> convertPacketPolicies(
      Map<String, Configuration> configs,
      IpsRoutedOutInterfacesFactory ipsRoutedOutInterfacesFactory) {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.convertPacketPolicies")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          configs,
          Entry::getKey,
          configEntry ->
              configEntry.getValue().getVrfs().values().stream()
                  .flatMap(
                      vrf -> {
                        IpsRoutedOutInterfaces ipsRoutedOutInterfaces =
                            ipsRoutedOutInterfacesFactory.getIpsRoutedOutInterfaces(
                                configEntry.getKey(), vrf.getName());
                        return configEntry
                            .getValue()
                            .getActiveInterfaces(vrf.getName())
                            .values()
                            .stream()
                            .filter(iface -> iface.getRoutingPolicyName() != null)
                            .map(
                                iface ->
                                    Maps.immutableEntry(
                                        iface.getName(),
                                        PacketPolicyToBdd.evaluate(
                                            configEntry
                                                .getValue()
                                                .getPacketPolicies()
                                                .get(iface.getRoutingPolicyName()),
                                            ipAccessListToBdd(configEntry.getValue()),
                                            ipsRoutedOutInterfaces)));
                      })
                  .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue)));
    } finally {
      span.finish();
    }
  }

  IpSpaceToBDD getIpSpaceToBDD() {
    return _dstIpSpaceToBDD;
  }

  Map<String, Map<String, Map<String, BDD>>> getIfaceAcceptBDDs() {
    return _ifaceAcceptBDDs;
  }

  BDD getRequiredTransitNodeBDD() {
    return _requiredTransitNodeBDD;
  }

  private static Map<String, Map<String, Map<org.batfish.datamodel.Edge, BDD>>>
      computeArpTrueEdgeBDDs(ForwardingAnalysis forwardingAnalysis, IpSpaceToBDD ipSpaceToBDD) {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeArpTrueEdgeBDDs")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          forwardingAnalysis.getArpTrueEdge(),
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry ->
                      toImmutableMap(
                          vrfEntry.getValue(),
                          Entry::getKey,
                          edgeEntry -> edgeEntry.getValue().accept(ipSpaceToBDD))));
    } finally {
      span.finish();
    }
  }

  private static Map<String, Map<String, Map<String, BDD>>> computeDispositionBDDs(
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaceMap, IpSpaceToBDD ipSpaceToBDD) {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeDispositionBDDs")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
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
    } finally {
      span.finish();
    }
  }

  private Stream<Edge> generateRootEdges(Map<StateExpr, BDD> rootBdds) {
    return Streams.concat(
        generateRootEdges_OriginateInterfaceLink_PreInInterface(rootBdds),
        generateRootEdges_OriginateVrf_PostInVrf(rootBdds),
        generateRootEdges_OriginateInterface_PostInVrf(rootBdds));
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
              String iface = originateInterfaceLink.getInterface();
              PreInInterface preInInterface = new PreInInterface(hostname, iface);

              BDD rootBdd = entry.getValue();

              return new Edge(
                  originateInterfaceLink,
                  preInInterface,
                  compose(
                      constraint(rootBdd),
                      addNoLastHopConstraint(_lastHopMgr, hostname, iface),
                      addSourceInterfaceConstraint(_bddSourceManagers.get(hostname), iface)));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRootEdges_OriginateVrf_PostInVrf(Map<StateExpr, BDD> rootBdds) {
    return rootBdds.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof OriginateVrf)
        .map(
            entry -> {
              OriginateVrf originateVrf = (OriginateVrf) entry.getKey();
              String hostname = originateVrf.getHostname();
              String vrf = originateVrf.getVrf();
              PostInVrf postInVrf = new PostInVrf(hostname, vrf);
              BDD rootBdd = entry.getValue();
              // Keep this edge's transition in sync with transition of session-matching edge,
              // defined in SessionScopeFibLookupSessionEdges#visitOriginatingSessionScope.
              return new Edge(
                  originateVrf,
                  postInVrf,
                  compose(
                      addOriginatingFromDeviceConstraint(_bddSourceManagers.get(hostname)),
                      addOutgoingOriginalFlowFiltersConstraint(
                          _bddOutgoingOriginalFlowFilterManagers.get(hostname)),
                      constraint(rootBdd)));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRootEdges_OriginateInterface_PostInVrf(Map<StateExpr, BDD> rootBdds) {
    return rootBdds.entrySet().stream()
        .filter(e -> e.getKey() instanceof OriginateInterface)
        .map(
            e -> {
              OriginateInterface state = (OriginateInterface) e.getKey();
              String vrf = _interfacesToVrfsMap.get(state.getHostname()).get(state.getInterface());
              PostInVrf postInVrf = new PostInVrf(state.getHostname(), vrf);
              String hostname = state.getHostname();
              return new Edge(
                  state,
                  postInVrf,
                  compose(
                      addOriginatingFromDeviceConstraint(_bddSourceManagers.get(hostname)),
                      addOutgoingOriginalFlowFiltersConstraint(
                          _bddOutgoingOriginalFlowFilterManagers.get(hostname)),
                      constraint(e.getValue())));
            });
  }

  /** Generate edges to each disposition. Depends on final nodes. */
  private Stream<Edge> generateDispositionEdges(Set<String> finalNodes) {
    return Streams.concat(
        generateRules_NodeAccept_Accept(finalNodes),
        generateRules_NodeDropAclIn_DropAclIn(finalNodes),
        generateRules_NodeDropAclOut_DropAclOut(finalNodes),
        generateRules_NodeDropNoRoute_DropNoRoute(finalNodes),
        generateRules_NodeDropNullRoute_DropNullRoute(finalNodes),
        generateRules_NodeInterfaceDeliveredToSubnet_DeliveredToSubnet(finalNodes),
        generateRules_NodeInterfaceExitsNetwork_ExitsNetwork(finalNodes),
        generateRules_NodeInterfaceInsufficientInfo_InsufficientInfo(finalNodes),
        generateRules_NodeInterfaceNeighborUnreachable_NeighborUnreachable(finalNodes));
  }

  /*
   * These edges do not depend on the query. Compute them separately so that we can later cache them
   * across queries if we want to.
   */
  private Stream<Edge> generateEdges() {
    return Streams.concat(
        generateRules_PreInInterface_NodeDropAclIn(),
        generateRules_PreInInterface_PostInInterface(),
        generateRules_PreInInterface_PbrFibLookup(),
        generateRules_PostInInterface_NodeDropAclIn(),
        generateRules_PostInInterface_PostInVrf(),
        generateRules_PbrFibLookup_InterfaceAccept(),
        generateRules_PbrFibLookup_NodeDropNoRoute(),
        generateRules_PbrFibLookup_PreOutVrf(),
        generateRules_PreOutEdge_NodeDropAclOut(),
        generateRules_PreOutEdge_PreOutEdgePostNat(),
        generateRules_PreOutEdgePostNat_NodeDropAclOut(),
        generateRules_PreOutEdgePostNat_PreInInterface(),
        generateRules_PreOutInterfaceDisposition_NodeInterfaceDisposition(),
        generateRules_PreOutInterfaceDisposition_NodeDropAclOut(),
        generateRules_VrfAccept_NodeAccept(),
        generateFibRules());
  }

  private @Nonnull Stream<Edge> generateFibRules() {
    return _bddFibGenerator.generateForwardingEdges(
        alwaysTrue(),
        PostInVrf::new,
        PreOutEdge::new,
        PreOutVrf::new,
        PreOutInterfaceDeliveredToSubnet::new,
        PreOutInterfaceExitsNetwork::new,
        PreOutInterfaceInsufficientInfo::new,
        PreOutInterfaceNeighborUnreachable::new);
  }

  private Stream<Edge> generateRules_NodeAccept_Accept(Set<String> finalNodes) {
    return finalNodes.stream().map(node -> new Edge(new NodeAccept(node), Accept.INSTANCE));
  }

  private static Stream<Edge> generateRules_NodeDropAclIn_DropAclIn(Set<String> finalNodes) {
    return finalNodes.stream().map(node -> new Edge(new NodeDropAclIn(node), DropAclIn.INSTANCE));
  }

  private static Stream<Edge> generateRules_NodeDropAclOut_DropAclOut(Set<String> finalNodes) {
    return finalNodes.stream().map(node -> new Edge(new NodeDropAclOut(node), DropAclOut.INSTANCE));
  }

  private Stream<Edge> generateRules_NodeDropNoRoute_DropNoRoute(Set<String> finalNodes) {
    return finalNodes.stream()
        /* In differential context, nodes can be added or removed. This can lead to a finalNode
         * that doesn't exist in _configs.
         */
        .filter(_configs::containsKey)
        .map(
            node ->
                new Edge(
                    new NodeDropNoRoute(node),
                    DropNoRoute.INSTANCE,
                    removeNodeSpecificConstraints(
                        node,
                        _lastHopMgr,
                        _bddOutgoingOriginalFlowFilterManagers.get(node),
                        _bddSourceManagers.get(node))));
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
        .flatMap(Configuration::activeInterfaces)
        .map(
            iface -> {
              String node = iface.getOwner().getHostname();
              String ifaceName = iface.getName();
              return new Edge(
                  nodeInterfaceDispositionConstructor.apply(node, ifaceName),
                  dispositionNode,
                  removeNodeSpecificConstraints(
                      node,
                      _lastHopMgr,
                      _bddOutgoingOriginalFlowFilterManagers.get(node),
                      _bddSourceManagers.get(node)));
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
    return getInterfaces()
        .filter(iface -> iface.getPostTransformationIncomingFilter() != null)
        .map(
            i -> {
              IpAccessList acl = i.getPostTransformationIncomingFilter();
              String node = i.getOwner().getHostname();
              String iface = i.getName();

              BDD aclDenyBDD = ignorableAclDenyBDD(node, acl);
              return new Edge(
                  new PostInInterface(node, iface),
                  new NodeDropAclIn(node),
                  compose(
                      constraint(aclDenyBDD),
                      removeNodeSpecificConstraints(
                          node,
                          _lastHopMgr,
                          _bddOutgoingOriginalFlowFilterManagers.get(node),
                          _bddSourceManagers.get(node))));
            });
  }

  private Stream<Edge> generateRules_PostInInterface_PostInVrf() {
    return getInterfaces()
        .map(
            iface -> {
              IpAccessList acl = iface.getPostTransformationIncomingFilter();
              String nodeName = iface.getOwner().getHostname();
              String vrfName = iface.getVrfName();
              String ifaceName = iface.getName();

              PostInInterface preState = new PostInInterface(nodeName, ifaceName);
              PostInVrf postState = new PostInVrf(nodeName, vrfName);

              BDD inAclBDD = ignorableAclPermitBDD(nodeName, acl);
              return new Edge(preState, postState, constraint(inAclBDD));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRules_PreInInterface_NodeDropAclIn() {
    return getInterfaces()
        .filter(iface -> iface.getRoutingPolicyName() != null || iface.getIncomingFilter() != null)
        .map(
            i -> {
              String node = i.getOwner().getHostname();
              String iface = i.getName();

              // There are two ways to drop based on ACLs: incoming filter, or PBR.
              // PBR takes precedence (consistent with FlowTracer)
              Transition denyTransition =
                  i.getRoutingPolicyName() != null
                      ? _convertedPacketPolicies.get(node).get(iface).getToDrop()
                      : constraint(ignorableAclDenyBDD(node, i.getIncomingFilter()));

              return new Edge(
                  new PreInInterface(node, iface),
                  new NodeDropAclIn(node),
                  compose(
                      denyTransition,
                      removeNodeSpecificConstraints(
                          node,
                          _lastHopMgr,
                          _bddOutgoingOriginalFlowFilterManagers.get(node),
                          _bddSourceManagers.get(node))));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRules_PreInInterface_PbrFibLookup() {
    return getInterfaces()
        .filter(iface -> iface.getRoutingPolicyName() != null)
        .flatMap(
            iface -> {
              String nodeName = iface.getOwner().getHostname();
              String vrfName = iface.getVrfName();
              String ifaceName = iface.getName();
              Map<FibLookup, Transition> transitionByFibLookup =
                  _convertedPacketPolicies.get(nodeName).get(ifaceName).getFibLookups();
              VrfExprNameExtractor lookupVrfExtractor = new VrfExprNameExtractor(iface);

              PreInInterface preInInterface = new PreInInterface(nodeName, ifaceName);
              return transitionByFibLookup.entrySet().stream()
                  .map(
                      transitionByFibLookupEntry -> {
                        FibLookup fibLookup = transitionByFibLookupEntry.getKey();
                        String lookupVrf = fibLookup.getVrfExpr().accept(lookupVrfExtractor);
                        return new Edge(
                            preInInterface,
                            new PbrFibLookup(nodeName, vrfName, lookupVrf),
                            compose(
                                addOutgoingOriginalFlowFiltersConstraint(
                                    _bddOutgoingOriginalFlowFilterManagers.get(nodeName)),
                                transitionByFibLookupEntry.getValue()));
                      });
            });
  }

  /**
   * Flows at {@link PbrFibLookup} state have already matched the packet policy (not DENIED_IN).
   * From there they can either get accepted into the ingress VRF or forwarded based on the PBR FIB
   * lookup. These edges represent the former.
   */
  private Stream<Edge> generateRules_PbrFibLookup_InterfaceAccept() {
    return getInterfaces()
        .flatMap(this::interfaceToPbrFibLookups)
        .distinct()
        .flatMap(
            pbrFibLookup -> {
              // Create edge from PbrFibLookup to InterfaceAccept for each interface in ingress VRF
              String hostname = pbrFibLookup.getHostname();
              String ingressVrf = pbrFibLookup.getIngressVrf();
              return _ifaceAcceptBDDs.get(hostname).get(ingressVrf).entrySet().stream()
                  .map(
                      ifaceAcceptBddEntry ->
                          new Edge(
                              pbrFibLookup,
                              new InterfaceAccept(hostname, ifaceAcceptBddEntry.getKey()),
                              ifaceAcceptBddEntry.getValue()));
            });
  }

  /**
   * Flows at {@link PbrFibLookup} state have already matched the packet policy (not DENIED_IN).
   * From there they can be:
   *
   * <ul>
   *   <li>Accepted (represented by edge to {@link InterfaceAccept})
   *   <li>Forwarded based on the PBR FIB lookup, and:
   *       <ul>
   *         <li>Successfully routed out (represented by edge to {@link PreOutVrf})
   *         <li>Dropped due to no route (represented by edge to {@link NodeDropNoRoute}, created in
   *             this method)
   *       </ul>
   * </ul>
   */
  private Stream<Edge> generateRules_PbrFibLookup_NodeDropNoRoute() {
    return getInterfaces()
        .flatMap(this::interfaceToPbrFibLookups)
        .distinct()
        .map(
            pbrFibLookup -> {
              // Generate PbrFibLookup -> NodeDropNoRoute edge for each PbrFibLookup
              String hostname = pbrFibLookup.getHostname();
              String ingressVrf = pbrFibLookup.getIngressVrf();
              String lookupVrf = pbrFibLookup.getLookupVrf();
              BDD acceptedBdd = _vrfAcceptBDDs.get(hostname).get(ingressVrf);
              BDD routableBDD = _routableBDDs.get(hostname).get(lookupVrf);
              return new Edge(
                  pbrFibLookup, new NodeDropNoRoute(hostname), acceptedBdd.nor(routableBDD));
            });
  }

  /**
   * Flows at {@link PbrFibLookup} state have already matched the packet policy (not DENIED_IN).
   * From there they can be:
   *
   * <ul>
   *   <li>Accepted (represented by edge to {@link InterfaceAccept})
   *   <li>Forwarded based on the PBR FIB lookup, and:
   *       <ul>
   *         <li>Successfully routed out (represented by edge to {@link PreOutVrf}, created in this
   *             method)
   *         <li>Dropped due to no route (represented by edge to {@link NodeDropNoRoute})
   *       </ul>
   * </ul>
   */
  private Stream<Edge> generateRules_PbrFibLookup_PreOutVrf() {
    return getInterfaces()
        .flatMap(this::interfaceToPbrFibLookups)
        .distinct()
        .map(
            pbrFibLookup -> {
              // Generate PbrFibLookup -> PreOutVrf edge for each PbrFibLookup
              String hostname = pbrFibLookup.getHostname();
              String ingressVrf = pbrFibLookup.getIngressVrf();
              String lookupVrf = pbrFibLookup.getLookupVrf();
              BDD acceptedBdd = _vrfAcceptBDDs.get(hostname).get(ingressVrf);
              BDD routableBDD = _routableBDDs.get(hostname).get(lookupVrf);
              return new Edge(
                  pbrFibLookup, new PreOutVrf(hostname, lookupVrf), routableBDD.diff(acceptedBdd));
            });
  }

  /** Returns {@link PbrFibLookup} states reachable by entering the given interface */
  @Nonnull
  private Stream<PbrFibLookup> interfaceToPbrFibLookups(Interface iface) {
    if (iface.getRoutingPolicyName() == null) {
      // Interface does not have PBR
      return Stream.of();
    }
    String nodeName = iface.getOwner().getHostname();
    String vrfName = iface.getVrfName();
    String ifaceName = iface.getName();
    VrfExprNameExtractor lookupVrfExtractor = new VrfExprNameExtractor(iface);

    Map<FibLookup, Transition> transitionByFibLookup =
        _convertedPacketPolicies.get(nodeName).get(ifaceName).getFibLookups();
    return transitionByFibLookup.keySet().stream()
        .map(fibLookup -> fibLookup.getVrfExpr().accept(lookupVrfExtractor))
        .map(lookupVrf -> new PbrFibLookup(nodeName, vrfName, lookupVrf));
  }

  private @Nonnull BDD aclDenyBDD(String node, @Nullable IpAccessList acl) {
    return acl == null ? _zero : _aclDenyBDDs.get(node).get(acl.getName()).get();
  }

  private @Nonnull BDD aclPermitBDD(String node, @Nullable IpAccessList acl) {
    return acl == null ? _one : _aclPermitBDDs.get(node).get(acl.getName()).get();
  }

  private @Nonnull BDD ignorableAclDenyBDD(String node, @Nullable IpAccessList acl) {
    return _ignoreFilters ? _zero : aclDenyBDD(node, acl);
  }

  private @Nonnull BDD ignorableAclPermitBDD(String node, @Nullable IpAccessList acl) {
    return _ignoreFilters ? _one : aclPermitBDD(node, acl);
  }

  @VisibleForTesting
  Stream<Edge> generateRules_PreInInterface_PostInInterface() {
    return getInterfaces()
        // Policy-based routing edges handled elsewhere
        .filter(iface -> iface.getRoutingPolicyName() == null)
        .map(
            iface -> {
              IpAccessList acl = iface.getIncomingFilter();
              String nodeName = iface.getOwner().getHostname();
              String ifaceName = iface.getName();

              PreInInterface preState = new PreInInterface(nodeName, ifaceName);
              PostInInterface postState = new PostInInterface(nodeName, ifaceName);

              BDD inAclBDD = ignorableAclPermitBDD(nodeName, acl);

              Transition transition =
                  compose(
                      constraint(inAclBDD),
                      addOutgoingOriginalFlowFiltersConstraint(
                          _bddOutgoingOriginalFlowFilterManagers.get(nodeName)),
                      _bddIncomingTransformations.get(nodeName).get(ifaceName));
              return new Edge(preState, postState, transition);
            });
  }

  /**
   * Restricts to flows that leave this device via the given interface. Important for the
   * out-of-order flow computation when outgoing original flow filter is present on any interface on
   * the node.
   */
  private BDD flowsLeavingInterface(String node, String iface) {
    return _bddOutgoingOriginalFlowFilterManagers.get(node).outgoingInterfaceBDD(iface);
  }

  private Stream<Edge> generateRules_PreOutEdge_NodeDropAclOut() {
    if (_ignoreFilters) {
      return Stream.of();
    }
    return _topologyEdges.stream()
        .flatMap(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              Interface i1 = _configs.get(node1).getAllInterfaces().get(iface1);
              assert i1.getActive();
              IpAccessList preNatAcl = i1.getPreTransformationOutgoingFilter();
              BDD denyPreNat = ignorableAclDenyBDD(node1, preNatAcl);

              if (denyPreNat.isZero()) {
                return Stream.of();
              }
              return Stream.of(
                  new Edge(
                      new PreOutEdge(node1, iface1, node2, iface2),
                      new NodeDropAclOut(node1),
                      compose(
                          constraint(denyPreNat),
                          removeNodeSpecificConstraints(
                              node1,
                              _lastHopMgr,
                              _bddOutgoingOriginalFlowFilterManagers.get(node1),
                              _bddSourceManagers.get(node1)))));
            });
  }

  private Stream<Edge> generateRules_PreOutEdge_PreOutEdgePostNat() {
    return _topologyEdges.stream()
        .flatMap(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              Interface i1 = _configs.get(node1).getAllInterfaces().get(iface1);
              assert i1.getActive();
              IpAccessList preNatAcl = i1.getPreTransformationOutgoingFilter();

              BDD aclPermit = ignorableAclPermitBDD(node1, preNatAcl);
              if (aclPermit.isZero()) {
                return Stream.of();
              }

              PreOutEdge preState = new PreOutEdge(node1, iface1, node2, iface2);
              PreOutEdgePostNat postState = new PreOutEdgePostNat(node1, iface1, node2, iface2);
              return Stream.of(
                  new Edge(
                      preState,
                      postState,
                      compose(
                          constraint(aclPermit),
                          _bddOutgoingTransformations.get(node1).get(iface1))));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRules_PreOutEdgePostNat_NodeDropAclOut() {
    if (_ignoreFilters) {
      return Stream.of();
    }
    return _topologyEdges.stream()
        .flatMap(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              Interface i1 = _configs.get(node1).getAllInterfaces().get(iface1);
              assert i1.getActive();
              IpAccessList acl = i1.getOutgoingFilter();
              BDD aclDenyBDD = ignorableAclDenyBDD(node1, acl);

              BDDOutgoingOriginalFlowFilterManager originalFlowFilterMgr =
                  _bddOutgoingOriginalFlowFilterManagers.get(node1);
              BDD originalFlowAclDenyBdd =
                  originalFlowFilterMgr.deniedByOriginalFlowEgressFilter(iface1);
              BDD denyBdd = aclDenyBDD.or(originalFlowAclDenyBdd);
              return denyBdd.isZero()
                  ? Stream.of()
                  : Stream.of(
                      new Edge(
                          new PreOutEdgePostNat(node1, iface1, node2, iface2),
                          new NodeDropAclOut(node1),
                          compose(
                              constraint(denyBdd),
                              removeNodeSpecificConstraints(
                                  node1,
                                  _lastHopMgr,
                                  _bddOutgoingOriginalFlowFilterManagers.get(node1),
                                  _bddSourceManagers.get(node1)))));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRules_PreOutEdgePostNat_PreInInterface() {
    return _topologyEdges.stream()
        .map(
            edge -> {
              String node1 = edge.getNode1();
              String iface1 = edge.getInt1();
              String node2 = edge.getNode2();
              String iface2 = edge.getInt2();

              Interface i1 = _configs.get(node1).getAllInterfaces().get(iface1);
              assert i1.getActive();
              BDD aclPermitBDD = ignorableAclPermitBDD(node1, i1.getOutgoingFilter());

              BDDOutgoingOriginalFlowFilterManager originalFlowFilterMgr =
                  _bddOutgoingOriginalFlowFilterManagers.get(node1);
              BDD originalFlowAclPermitBdd =
                  originalFlowFilterMgr.permittedByOriginalFlowEgressFilter(iface1);

              return new Edge(
                  new PreOutEdgePostNat(node1, iface1, node2, iface2),
                  new PreInInterface(node2, iface2),
                  compose(
                      constraint(aclPermitBDD.and(originalFlowAclPermitBdd)),
                      removeNodeSpecificConstraints(
                          node1,
                          _lastHopMgr,
                          _bddOutgoingOriginalFlowFilterManagers.get(node1),
                          _bddSourceManagers.get(node1)),
                      addSourceInterfaceConstraint(_bddSourceManagers.get(node2), iface2),
                      addLastHopConstraint(_lastHopMgr, node1, iface1, node2, iface2)));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRules_PreOutInterfaceDisposition_NodeDropAclOut() {
    if (_ignoreFilters) {
      return Stream.of();
    }

    return _configs.entrySet().stream()
        .flatMap(
            nodeEntry -> {
              String node = nodeEntry.getKey();
              BDDOutgoingOriginalFlowFilterManager originalFlowFilterMgr =
                  _bddOutgoingOriginalFlowFilterManagers.get(node);
              return nodeEntry.getValue().getVrfs().entrySet().stream()
                  .flatMap(
                      vrfEntry -> {
                        StateExpr postState = new NodeDropAclOut(node);
                        return nodeEntry
                            .getValue()
                            .getActiveInterfaces(vrfEntry.getKey())
                            .values()
                            .stream()
                            .filter(
                                iface ->
                                    iface.getOutgoingOriginalFlowFilter() != null
                                        || iface.getPreTransformationOutgoingFilter() != null
                                        || iface.getOutgoingFilter() != null)
                            .flatMap(
                                iface -> {
                                  String ifaceName = iface.getName();
                                  BDD denyOriginalFlowBdd =
                                      originalFlowFilterMgr.deniedByOriginalFlowEgressFilter(
                                          ifaceName);
                                  BDD denyPreAclBDD =
                                      ignorableAclDenyBDD(
                                          node, iface.getPreTransformationOutgoingFilter());
                                  BDD denyPostAclBDD =
                                      ignorableAclDenyBDD(node, iface.getOutgoingFilter());
                                  Transition transformation =
                                      _bddOutgoingTransformations.get(node).get(ifaceName);

                                  // DENIED_OUT can be due to any of:
                                  // - denied by the outgoingOriginalFlowFilter
                                  // - denied by the pre-Transformation ACL
                                  // - transformed and denied by the post-Transformation ACL
                                  Transition deniedFlows =
                                      branch(
                                          // branch on whether denied before transformation
                                          denyOriginalFlowBdd.or(denyPreAclBDD),
                                          // deny all flows denied by pre-trans ACLs
                                          IDENTITY,
                                          // for flows permitted by first two ACLs, transform and
                                          // then apply the post-trans ACL. deny any that are denied
                                          // by the post-trans ACL.
                                          compose(transformation, constraint(denyPostAclBDD)));

                                  // Clear any node-specific constraints before exiting the node.
                                  Transition transition =
                                      compose(
                                          deniedFlows,
                                          removeNodeSpecificConstraints(
                                              node,
                                              _lastHopMgr,
                                              _bddOutgoingOriginalFlowFilterManagers.get(node),
                                              _bddSourceManagers.get(node)));

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

  @Nonnull
  private Stream<Edge> generateRules_VrfAccept_NodeAccept() {
    return _ifaceAcceptBDDs.entrySet().stream()
        .flatMap(
            nodeEntry -> {
              String hostname = nodeEntry.getKey();
              Transition removeNodeSpecificConstraints =
                  removeNodeSpecificConstraints(
                      hostname,
                      _lastHopMgr,
                      _bddOutgoingOriginalFlowFilterManagers.get(hostname),
                      _bddSourceManagers.get(hostname));
              StateExpr nodeAccept = new NodeAccept(hostname);
              return nodeEntry.getValue().keySet().stream() // vrf names
                  .map(
                      vrf ->
                          new Edge(
                              new VrfAccept(hostname, vrf),
                              nodeAccept,
                              removeNodeSpecificConstraints));
            });
  }

  @VisibleForTesting
  Stream<Edge> generateRules_PreOutInterfaceDisposition_NodeInterfaceDisposition() {
    return getInterfaces()
        .flatMap(
            iface -> {
              String node = iface.getOwner().getHostname();
              String ifaceName = iface.getName();
              BDD permitBeforeNatBDD =
                  ignorableAclPermitBDD(node, iface.getPreTransformationOutgoingFilter());
              BDD permitAfterNatBDD = ignorableAclPermitBDD(node, iface.getOutgoingFilter());
              Transition outgoingTransformation =
                  _bddOutgoingTransformations.get(node).get(ifaceName);

              if (permitBeforeNatBDD.isZero() || permitAfterNatBDD.isZero()) {
                return Stream.of();
              }

              BDDOutgoingOriginalFlowFilterManager originalFlowFilterMgr =
                  _bddOutgoingOriginalFlowFilterManagers.get(node);
              BDD permitOriginalFlowBdd =
                  originalFlowFilterMgr.permittedByOriginalFlowEgressFilter(ifaceName);

              /* 1. pre-transformation filter
               * 2. outgoingOriginalFlowFilter (also constrains to interface)
               *    can be applied in any order, so do it before transformation for
               *    less forward work.
               * 3. outgoing transformation
               * 4. post-transformation filter
               */
              Transition transition =
                  compose(
                      constraint(permitBeforeNatBDD.and(permitOriginalFlowBdd)),
                      outgoingTransformation,
                      constraint(permitAfterNatBDD));
              if (transition == ZERO) {
                return Stream.of();
              }

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

  public BDDReachabilityAnalysis bddReachabilityAnalysis(IpSpaceAssignment srcIpSpaceAssignment) {
    return bddReachabilityAnalysis(srcIpSpaceAssignment, false);
  }

  public BDDReachabilityAnalysis bddReachabilityAnalysis(
      IpSpaceAssignment srcIpSpaceAssignment, boolean useInterfaceRoots) {
    return bddReachabilityAnalysis(
        srcIpSpaceAssignment,
        matchDst(UniverseIpSpace.INSTANCE),
        ImmutableSet.of(),
        ImmutableSet.of(),
        _configs.keySet(),
        ImmutableSet.of(FlowDisposition.ACCEPTED),
        useInterfaceRoots);
  }

  public BDDLoopDetectionAnalysis bddLoopDetectionAnalysis(IpSpaceAssignment srcIpSpaceAssignment) {
    Map<StateExpr, BDD> ingressLocationStates = rootConstraints(srcIpSpaceAssignment, _one, false);
    Stream<Edge> edges = Stream.concat(generateEdges(), generateRootEdges(ingressLocationStates));
    return new BDDLoopDetectionAnalysis(_bddPacket, edges, ingressLocationStates.keySet());
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
    checkArgument(!actions.isEmpty(), "No actions");
    Span span = GlobalTracer.get().buildSpan("BDDReachabilityAnalysisFactory.getAllBDDs").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning

      Set<FlowDisposition> nonLoopActions = new HashSet<>(actions);
      boolean loopIncluded = nonLoopActions.remove(LOOP);

      if (nonLoopActions.isEmpty()) {
        // since actions is not empty, loopIncluded must be true. Thus just detect loops
        return bddLoopDetectionAnalysis(srcIpSpaceAssignment).detectLoops();
      } else if (!loopIncluded) {
        // only reachability, no loop detection
        return bddReachabilityAnalysis(
                srcIpSpaceAssignment,
                initialHeaderSpace,
                forbiddenTransitNodes,
                requiredTransitNodes,
                finalNodes,
                nonLoopActions)
            .getIngressLocationReachableBDDs();
      } else {
        // both reachability and loop detection
        return bddReachabilityAndLoopDetectionAnalysis(
                srcIpSpaceAssignment,
                initialHeaderSpace,
                forbiddenTransitNodes,
                requiredTransitNodes,
                finalNodes,
                nonLoopActions)
            .getIngressLocationBdds();
      }
    } finally {
      span.finish();
    }
  }

  private BDDReachabilityAndLoopDetectionAnalysis bddReachabilityAndLoopDetectionAnalysis(
      IpSpaceAssignment srcIpSpaceAssignment,
      AclLineMatchExpr initialHeaderSpace,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<String> finalNodes,
      Set<FlowDisposition> actions) {
    BDD initialHeaderSpaceBdd = computeInitialHeaderSpaceBdd(initialHeaderSpace);
    BDD finalHeaderSpaceBdd = computeFinalHeaderSpaceBdd(initialHeaderSpaceBdd);
    Map<StateExpr, BDD> roots = rootConstraints(srcIpSpaceAssignment, initialHeaderSpaceBdd, false);

    List<Edge> sharedEdges =
        Stream.concat(generateEdges(), generateRootEdges(roots)).collect(Collectors.toList());

    Stream<Edge> reachabilityEdges =
        Streams.concat(
            sharedEdges.stream(),
            generateDispositionEdges(finalNodes),
            generateQueryEdges(actions));
    reachabilityEdges = instrumentForbiddenTransitNodes(forbiddenTransitNodes, reachabilityEdges);
    reachabilityEdges = instrumentRequiredTransitNodes(requiredTransitNodes, reachabilityEdges);

    BDDLoopDetectionAnalysis loopDetectionAnalysis =
        new BDDLoopDetectionAnalysis(_bddPacket, sharedEdges.stream(), roots.keySet());
    BDDReachabilityAnalysis reachabilityAnalysis =
        new BDDReachabilityAnalysis(
            _bddPacket, roots.keySet(), reachabilityEdges, finalHeaderSpaceBdd);

    return new BDDReachabilityAndLoopDetectionAnalysis(reachabilityAnalysis, loopDetectionAnalysis);
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
  public BDDReachabilityAnalysis bddReachabilityAnalysis(
      IpSpaceAssignment srcIpSpaceAssignment,
      AclLineMatchExpr initialHeaderSpace,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<String> finalNodes,
      Set<FlowDisposition> actions) {
    return bddReachabilityAnalysis(
        srcIpSpaceAssignment,
        initialHeaderSpace,
        forbiddenTransitNodes,
        requiredTransitNodes,
        finalNodes,
        actions,
        false);
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
   * @param useInterfaceRoots Whether to perform analysis with {@link OriginateInterface} roots
   *     rather than {@link OriginateVrf}.
   */
  @VisibleForTesting
  public BDDReachabilityAnalysis bddReachabilityAnalysis(
      IpSpaceAssignment srcIpSpaceAssignment,
      AclLineMatchExpr initialHeaderSpace,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<String> finalNodes,
      Set<FlowDisposition> actions,
      boolean useInterfaceRoots) {
    checkArgument(!finalNodes.isEmpty(), "final nodes cannot be empty");
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.bddReachabilityAnalysis")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      BDD initialHeaderSpaceBdd = computeInitialHeaderSpaceBdd(initialHeaderSpace);
      BDD finalHeaderSpaceBdd = computeFinalHeaderSpaceBdd(initialHeaderSpaceBdd);

      Map<StateExpr, BDD> roots =
          rootConstraints(srcIpSpaceAssignment, initialHeaderSpaceBdd, useInterfaceRoots);

      Stream<Edge> edgeStream =
          Streams.concat(
              generateEdges(),
              generateRootEdges(roots),
              generateDispositionEdges(finalNodes),
              generateQueryEdges(actions));
      edgeStream = instrumentForbiddenTransitNodes(forbiddenTransitNodes, edgeStream);
      edgeStream = instrumentRequiredTransitNodes(requiredTransitNodes, edgeStream);

      return new BDDReachabilityAnalysis(
          _bddPacket, roots.keySet(), edgeStream, finalHeaderSpaceBdd);
    } finally {
      span.finish();
    }
  }

  private BDD computeInitialHeaderSpaceBdd(AclLineMatchExpr initialHeaderSpace) {
    IpAccessListToBdd ipAccessListToBdd =
        new MemoizedIpAccessListToBdd(
            _bddPacket, BDDSourceManager.empty(_bddPacket), ImmutableMap.of(), ImmutableMap.of());
    return ipAccessListToBdd.toBdd(initialHeaderSpace);
  }

  /**
   * Compute the space of possible final headers, under the assumption that any NAT rule may be
   * applied.
   */
  @VisibleForTesting
  BDD computeFinalHeaderSpaceBdd(BDD initialHeaderSpaceBdd) {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeFinalHeaderSpaceBdd")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      BDD finalHeaderSpace = initialHeaderSpaceBdd;

      BDD noDstIp = finalHeaderSpace.exist(_dstIpVars);
      if (!noDstIp.equals(finalHeaderSpace)) {
        // there's a constraint on dst Ip, so include nat pool Ips
        BDD dstTransformationRange = _transformationIpRanges.get(IpField.DESTINATION);
        if (dstTransformationRange != null) {
          // dst IP is either the initial one, or one of that NAT pool IPs.
          finalHeaderSpace = finalHeaderSpace.or(noDstIp.and(dstTransformationRange));
        }
      }

      BDD noSrcIp = finalHeaderSpace.exist(_sourceIpVars);
      if (!noSrcIp.equals(finalHeaderSpace)) {
        // there's a constraint on source Ip, so include nat pool Ips
        BDD srcNatPoolIps = _transformationIpRanges.getOrDefault(IpField.SOURCE, _zero);
        if (!srcNatPoolIps.isZero()) {
          /*
           * In this case, since source IPs usually don't play a huge role in routing, we could just
           * existentially quantify away the constraint. There's a performance trade-off: tighter
           * constraints prune more paths, but are more expensive to operate on.
           */
          finalHeaderSpace = finalHeaderSpace.or(noSrcIp.and(srcNatPoolIps));
        }
      }

      BDD noDstPort = finalHeaderSpace.exist(_dstPortVars);
      if (!noDstPort.equals(finalHeaderSpace)) {
        BDD dstTransformationRange = _transformationPortRanges.get(PortField.DESTINATION);
        if (dstTransformationRange != null) {
          finalHeaderSpace = finalHeaderSpace.or(noDstPort.and(dstTransformationRange));
        }
      }

      BDD noSrcPort = finalHeaderSpace.exist(_sourcePortVars);
      if (!noSrcPort.equals(finalHeaderSpace)) {
        BDD srcNatPool = _transformationPortRanges.getOrDefault(PortField.SOURCE, _zero);
        if (!srcNatPool.isZero()) {
          finalHeaderSpace = finalHeaderSpace.or(noSrcPort.and(srcNatPool));
        }
      }

      return finalHeaderSpace;
    } finally {
      span.finish();
    }
  }

  /**
   * Create a {@link BDDReachabilityAnalysis} with the given {@link BDDFirewallSessionTraceInfo
   * sessions}.
   *
   * @param returnPassOrigBdds Sets of packets at different locations to originate packets from in
   *     the return pass graph.
   * @param returnPassSuccessBdds Sets of packets at different locations in the return pass graph
   *     that are to be considered successful (and not failure). See {@link
   *     BidirectionalReachabilityReturnPassInstrumentation}.
   */
  BDDReachabilityAnalysis bddReachabilityAnalysis(
      Map<StateExpr, BDD> returnPassOrigBdds,
      Map<StateExpr, BDD> returnPassSuccessBdds,
      Map<String, List<BDDFirewallSessionTraceInfo>> initializedSessions,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<FlowDisposition> dispositions) {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.bddReachabilityAnalysis")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning

      /* We will use the return pass reachability graph a bit differently than usual: to find flows
       * that successfully return to the origination point of the forward flow, we'll look at states
       * like NodeInterfaceDeliveredToSubnet, rather than adding edges all the way to Query. Also,
       * to find return flows that fail, we add the usual query edges for failure dispositions, and
       * then search backward from Query to the origination points, which are the termination points
       * of the forward pass, and then in the forward reachability graph we'll propagate failing flows
       * from the termination points back to the forward pass origination points.
       *
       * This backward search would work for successful flows as well, but requires an additional
       * graph traversal (two backward searches instead of one forward search). The advantage is that
       * it would work in cases where the forward and return flow don't match (if that's possible).
       */
      Stream<Edge> returnPassEdges =
          Streams.concat(
              instrumentReturnPassEdges(
                  returnPassSuccessBdds,
                  sessionInstrumentation(
                      _bddPacket,
                      _configs,
                      _bddSourceManagers,
                      _lastHopMgr,
                      _bddOutgoingOriginalFlowFilterManagers,
                      _aclPermitBDDs,
                      Stream.concat(generateEdges(), generateDispositionEdges(_configs.keySet())),
                      initializedSessions,
                      _bddFibGenerator)),
              generateRootEdges(returnPassOrigBdds),
              generateQueryEdges(dispositions));
      returnPassEdges = instrumentForbiddenTransitNodes(forbiddenTransitNodes, returnPassEdges);
      returnPassEdges = instrumentRequiredTransitNodes(requiredTransitNodes, returnPassEdges);

      return new BDDReachabilityAnalysis(
          _bddPacket, returnPassOrigBdds.keySet(), returnPassEdges, _one);
    } finally {
      span.finish();
    }
  }

  class RangeComputer implements TransformationStepVisitor<Void> {
    private Map<IpField, BDD> _ipRanges;
    private Map<PortField, BDD> _portRanges;

    public RangeComputer() {
      _ipRanges = new HashMap<>();
      _portRanges = new HashMap<>();
    }

    public Map<IpField, BDD> getIpRanges() {
      return ImmutableMap.copyOf(_ipRanges);
    }

    public Map<PortField, BDD> getPortRanges() {
      return ImmutableMap.copyOf(_portRanges);
    }

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

    private BDDInteger getPortVar(PortField portField) {
      switch (portField) {
        case DESTINATION:
          return _bddPacket.getDstPort();
        case SOURCE:
          return _bddPacket.getSrcPort();
        default:
          throw new IllegalArgumentException("Unknown PortField " + portField);
      }
    }

    @Override
    public Void visitAssignIpAddressFromPool(AssignIpAddressFromPool assignIpAddressFromPool) {
      IpField ipField = assignIpAddressFromPool.getIpField();
      BDDInteger var = getIpSpaceToBDD(ipField).getBDDInteger();
      BDD bdd =
          assignIpAddressFromPool.getIpRanges().asRanges().stream()
              .map(
                  range ->
                      var.range(range.lowerEndpoint().asLong(), range.upperEndpoint().asLong()))
              .reduce(var.getFactory().zero(), BDD::or);
      _ipRanges.merge(ipField, bdd, BDD::or);
      return null;
    }

    @Override
    public Void visitNoop(Noop noop) {
      return null;
    }

    @Override
    public Void visitShiftIpAddressIntoSubnet(ShiftIpAddressIntoSubnet shiftIpAddressIntoSubnet) {
      IpField ipField = shiftIpAddressIntoSubnet.getIpField();
      BDD bdd = getIpSpaceToBDD(ipField).toBDD(shiftIpAddressIntoSubnet.getSubnet());
      _ipRanges.merge(ipField, bdd, BDD::or);
      return null;
    }

    @Override
    public Void visitAssignPortFromPool(AssignPortFromPool assignPortFromPool) {
      PortField portField = assignPortFromPool.getPortField();
      BDDInteger var = getPortVar(portField);
      BDD bdd = var.range(assignPortFromPool.getPoolStart(), assignPortFromPool.getPoolEnd());
      _portRanges.merge(portField, bdd, BDD::or);
      return null;
    }

    @Override
    public Void visitApplyAll(ApplyAll applyAll) {
      applyAll.getSteps().forEach(step -> step.accept(this));
      return null;
    }

    @Override
    public Void visitApplyAny(ApplyAny applyAny) {
      applyAny.getSteps().forEach(step -> step.accept(this));
      return null;
    }
  }

  private RangeComputer computeTransformationRanges() {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeTransformationRanges")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      RangeComputer rangeComputer = new RangeComputer();
      _configs
          .values()
          .forEach(
              configuration ->
                  configuration
                      .activeInterfaces()
                      .forEach(
                          iface -> {
                            visitTransformationSteps(
                                iface.getIncomingTransformation(), rangeComputer);
                            visitTransformationSteps(
                                iface.getOutgoingTransformation(), rangeComputer);
                          }));
      return rangeComputer;
    } finally {
      span.finish();
    }
  }

  Map<StateExpr, BDD> rootConstraints(
      IpSpaceAssignment srcIpSpaceAssignment,
      BDD initialHeaderSpaceBdd,
      boolean useInterfaceRoots) {
    Span span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysisFactory.rootConstraints").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      LocationVisitor<Optional<StateExpr>> locationToStateExpr =
          new LocationToOriginationStateExpr(_configs, useInterfaceRoots);
      IpSpaceToBDD srcIpSpaceToBDD = _bddPacket.getSrcIpSpaceToBDD();

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
    } finally {
      span.finish();
    }
  }

  /** Creates mapping of hostname -&gt; interface name -&gt; vrf name for active interfaces */
  private static Map<String, Map<String, String>> computeInterfacesToVrfsMap(
      Map<String, Configuration> configs) {
    return toImmutableMap(
        configs,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue().getActiveInterfaces().values(),
                Interface::getName,
                Interface::getVrfName));
  }

  private static Map<String, Map<String, Map<String, BDD>>> computeIfaceAcceptBDDs(
      Map<String, Configuration> configs,
      Map<String, Map<String, Map<String, IpSpace>>> acceptIps, // hostname -> vrf -> iface -> ips
      IpSpaceToBDD ipSpaceToBDD) {
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.computeIfaceAcceptBDDs")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      // Iterate over configs because we want all node/vrf/iface keys to be present in the map
      return toImmutableMap(
          configs,
          Entry::getKey, /* hostname */
          nodeEntry -> {
            String hostname = nodeEntry.getKey();
            Configuration c = nodeEntry.getValue();
            Map<String, Map<String, IpSpace>> nodeAcceptIps =
                acceptIps.getOrDefault(hostname, ImmutableMap.of());
            return toImmutableMap(
                c.getVrfs().keySet(),
                Function.identity(), /* vrf */
                vrf -> {
                  Map<String, IpSpace> vrfAcceptIps =
                      nodeAcceptIps.getOrDefault(vrf, ImmutableMap.of());
                  // Create entry for every interface in the current VRF
                  return c.getAllInterfaces().values().stream()
                      .filter(iface -> iface.getVrfName().equals(vrf))
                      .map(Interface::getName)
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Function.identity(), /* interface */
                              ifaceName ->
                                  vrfAcceptIps
                                      .getOrDefault(ifaceName, EmptyIpSpace.INSTANCE)
                                      .accept(ipSpaceToBDD)));
                });
          });
    } finally {
      span.finish();
    }
  }

  private Map<String, Map<String, Map<String, BDD>>> computeNextVrfBDDs(
      Map<String, Map<String, Map<String, IpSpace>>> nextVrfIpsByNodeVrf,
      IpSpaceToBDD ipSpaceToBDD) {
    Span span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysisFactory.computeNextVrfBDDs").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return toImmutableMap(
          nextVrfIpsByNodeVrf,
          Entry::getKey /* node */,
          nextVrfIpsByNodeVrfEntry ->
              toImmutableMap(
                  nextVrfIpsByNodeVrfEntry.getValue() /* nextVrfIpsByVrf */,
                  Entry::getKey /* vrf */,
                  nextVrfIpsByVrfEntry ->
                      toImmutableMap(
                          nextVrfIpsByVrfEntry.getValue() /* nextVrfIpsByNextVrf */,
                          Entry::getKey,
                          nextVrfIpsByNextVrfEntry ->
                              nextVrfIpsByNextVrfEntry.getValue().accept(ipSpaceToBDD))));
    } finally {
      span.finish();
    }
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
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.instrumentForbiddenTransitNodes")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
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
    } finally {
      span.finish();
    }
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
    Span span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysisFactory.instrumentRequiredTransitNodes")
            .start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
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
              return requiredTransitNodes.contains(hostname)
                  ? adaptEdgeSetTransitedBit(edge)
                  : edge;
            } else if (edge.getPreState() instanceof OriginateVrf
                || edge.getPreState() instanceof OriginateInterface
                || edge.getPreState() instanceof OriginateInterfaceLink) {
              return andThen(edge, notTransited);
            } else if (edge.getPostState() instanceof Query) {
              return andThen(edge, transited);
            } else {
              return edge;
            }
          });
    } finally {
      span.finish();
    }
  }

  public Map<String, BDDOutgoingOriginalFlowFilterManager>
      getBddOutgoingOriginalFlowFilterManagers() {
    return _bddOutgoingOriginalFlowFilterManagers;
  }

  public Map<String, BDDSourceManager> getBDDSourceManagers() {
    return _bddSourceManagers;
  }

  public @Nullable LastHopOutgoingInterfaceManager getLastHopManager() {
    return _lastHopMgr;
  }

  private @Nonnull Stream<Interface> getInterfaces() {
    return _configs.values().stream().flatMap(Configuration::activeInterfaces);
  }
}
