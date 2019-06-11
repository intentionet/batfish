package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.common.util.CollectionUtil.toOrderedHashCode;
import static org.batfish.datamodel.routing_policy.Environment.Direction.IN;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRouteOnImport;
import static org.batfish.dataplane.protocols.IsisProtocolHelper.convertRouteLevel1ToLevel2;
import static org.batfish.dataplane.protocols.IsisProtocolHelper.setOverloadOnAllRoutes;
import static org.batfish.dataplane.protocols.StaticRouteHelper.isInterfaceRoute;
import static org.batfish.dataplane.protocols.StaticRouteHelper.shouldActivateNextHopIpRoute;
import static org.batfish.dataplane.rib.AbstractRib.importRib;
import static org.batfish.dataplane.rib.RibDelta.importDeltaToBuilder;
import static org.batfish.dataplane.rib.RibDelta.importRibDelta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import com.google.common.graph.Network;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteBuilder;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EvpnRoute;
import org.batfish.datamodel.EvpnType3Route;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.FibImpl;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.dataplane.rib.RibGroup;
import org.batfish.datamodel.dataplane.rib.RibId;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.dataplane.protocols.BgpProtocolHelper;
import org.batfish.dataplane.protocols.GeneratedRouteHelper;
import org.batfish.dataplane.rib.AnnotatedRib;
import org.batfish.dataplane.rib.Bgpv4Rib;
import org.batfish.dataplane.rib.ConnectedRib;
import org.batfish.dataplane.rib.IsisLevelRib;
import org.batfish.dataplane.rib.IsisRib;
import org.batfish.dataplane.rib.KernelRib;
import org.batfish.dataplane.rib.LocalRib;
import org.batfish.dataplane.rib.Rib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RipInternalRib;
import org.batfish.dataplane.rib.RipRib;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;
import org.batfish.dataplane.rib.StaticRib;

public class VirtualRouter implements Serializable {

  private static final long serialVersionUID = 1L;

  /** The BGP routing process. Null if BGP is not configured for this VRF */
  @Nullable transient BgpRoutingProcess _bgpRoutingProcess;

  /** Parent configuration for this virtual router */
  @Nonnull private final Configuration _c;

  /** The RIB containing connected routes */
  private transient ConnectedRib _connectedRib;

  /**
   * Queues containing routes that are coming in from other VRFs (as a result of explicitly
   * configured leaking or applied RIB groups).
   */
  private transient SortedMap<
          CrossVrfEdgeId, Queue<RouteAdvertisement<AnnotatedRoute<AbstractRoute>>>>
      _crossVrfIncomingRoutes;

  /**
   * The independent RIB contains connected and static routes, which are unaffected by BDP
   * iterations (hence, independent).
   */
  transient Rib _independentRib;

  /** Incoming messages into this router from each IS-IS circuit */
  transient SortedMap<IsisEdge, Queue<RouteAdvertisement<IsisRoute>>> _isisIncomingRoutes;

  transient IsisLevelRib _isisL1Rib;
  transient IsisLevelRib _isisL2Rib;
  private transient IsisLevelRib _isisL1StagingRib;
  private transient IsisLevelRib _isisL2StagingRib;
  private transient IsisRib _isisRib;
  transient KernelRib _kernelRib;
  transient LocalRib _localRib;

  /** The main RIB, a combination different protocol RIBs */
  final Rib _mainRib;

  private final Map<String, Rib> _mainRibs;

  /** Keeps track of changes to the main RIB */
  @VisibleForTesting
  transient RibDelta.Builder<AnnotatedRoute<AbstractRoute>> _mainRibRouteDeltaBuilder;

  @Nonnull final String _name;
  @Nonnull private final Node _node;
  private transient Map<String, OspfRoutingProcess> _ospfProcesses;

  transient RipInternalRib _ripInternalRib;
  transient RipInternalRib _ripInternalStagingRib;
  transient RipRib _ripRib;
  transient StaticRib _staticInterfaceRib;
  transient StaticRib _staticNextHopRib;

  /** FIB (forwarding information base) built from the main RIB */
  private Fib _fib;

  /** RIB containing generated routes */
  private transient Rib _generatedRib;

  /** Metadata about propagated prefixes to/from neighbors */
  private PrefixTracer _prefixTracer;

  /** List of all EIGRP processes in this VRF */
  @VisibleForTesting transient ImmutableMap<Long, VirtualEigrpProcess> _virtualEigrpProcesses;

  /**
   * VNI settings that are updated dynamically as the dataplane is being computed (e.g., based on
   * EVPN route advertisements).
   */
  private Set<VniSettings> _vniSettings;

  /** A {@link Vrf} that this virtual router represents */
  final Vrf _vrf;

  VirtualRouter(@Nonnull final String name, @Nonnull final Node node) {
    _node = node;
    _c = node.getConfiguration();
    _name = name;
    _vrf = _c.getVrfs().get(name);
    // Main RIB + delta builder
    _mainRib = new Rib();
    _mainRibs = ImmutableMap.of(RibId.DEFAULT_RIB_NAME, _mainRib);
    _mainRibRouteDeltaBuilder = RibDelta.builder();
    // Init rest of the RIBs
    initRibs();

    _prefixTracer = new PrefixTracer();
    _virtualEigrpProcesses = ImmutableMap.of();
    _ospfProcesses = ImmutableMap.of();
    _vniSettings = ImmutableSet.copyOf(_vrf.getVniSettings().values());
    if (_vrf.getBgpProcess() != null) {
      _bgpRoutingProcess =
          new BgpRoutingProcess(_vrf.getBgpProcess(), _c, _name, _mainRib, BgpTopology.EMPTY);
    }
  }

  @VisibleForTesting
  void initCrossVrfQueues() {
    // TODO: also handle non-default RIBs
    // https://github.com/batfish/batfish/issues/3050
    _crossVrfIncomingRoutes =
        _node.getVirtualRouters().keySet().stream()
            .filter(n -> !_name.equals(n))
            .collect(
                ImmutableSortedMap.toImmutableSortedMap(
                    Ordering.natural(),
                    vrfName -> new CrossVrfEdgeId(vrfName, RibId.DEFAULT_RIB_NAME),
                    v -> new ConcurrentLinkedQueue<>()));
  }

  /**
   * Convert a given RibDelta into {@link RouteAdvertisement} objects and enqueue them onto a given
   * queue.
   *
   * @param queue the message queue
   * @param delta {@link RibDelta} representing changes.
   */
  static <R extends AbstractRoute, D extends R> void queueDelta(
      Queue<RouteAdvertisement<R>> queue, @Nonnull RibDelta<D> delta) {
    delta
        .getActions()
        .forEach(
            r -> {
              // REPLACE does not make sense across routers, update with WITHDRAW
              Reason reason = r.getReason() == Reason.REPLACE ? Reason.WITHDRAW : r.getReason();
              queue.add(
                  RouteAdvertisement.<R>builder().setRoute(r.getRoute()).setReason(reason).build());
            });
  }

  /** Lookup the VirtualRouter owner of a remote BGP neighbor. */
  @Nullable
  @VisibleForTesting
  static VirtualRouter getRemoteBgpNeighborVR(
      @Nonnull BgpPeerConfigId bgpId, @Nonnull final Map<String, Node> allNodes) {
    return allNodes.get(bgpId.getHostname()).getVirtualRouters().get(bgpId.getVrfName());
  }

  /**
   * Initializes helper data structures and easy-to-compute RIBs that are not affected by BDP
   * iterations (e.g., static route RIB, connected route RIB, etc.)
   */
  @VisibleForTesting
  void initForIgpComputation(TopologyContext topologyContext) {
    initConnectedRib();
    initKernelRib();
    initLocalRib();
    initStaticRibs();
    // Always import local and connected routes into your own rib
    importRib(_independentRib, _connectedRib);
    importRib(_independentRib, _kernelRib);
    importRib(_independentRib, _localRib);
    importRib(_independentRib, _staticInterfaceRib, _name);
    importRib(_mainRib, _independentRib);

    // Now check whether any rib groups are applied
    RibGroup connectedRibGroup = _vrf.getAppliedRibGroups().get(RoutingProtocol.CONNECTED);
    importRib(_mainRib, _connectedRib);
    if (connectedRibGroup != null) {
      applyRibGroup(connectedRibGroup, _connectedRib);
    }
    RibGroup localRibGroup = _vrf.getAppliedRibGroups().get(RoutingProtocol.LOCAL);
    if (localRibGroup != null) {
      applyRibGroup(localRibGroup, _localRib);
    }

    _ospfProcesses =
        _vrf.getOspfProcesses().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    e ->
                        new OspfRoutingProcess(
                            e.getValue(), _name, _c, topologyContext.getOspfTopology())));
    _ospfProcesses.values().forEach(p -> p.initialize(_node));

    initEigrp();
    initBaseRipRoutes();
  }

  /** Apply a rib group to a given source rib (which belongs to this VRF) */
  private void applyRibGroup(@Nonnull RibGroup ribGroup, @Nonnull AnnotatedRib<?> sourceRib) {
    RoutingPolicy policy = _c.getRoutingPolicies().get(ribGroup.getImportPolicy());
    checkState(policy != null, "RIB group %s is missing import policy", ribGroup.getName());
    sourceRib.getTypedRoutes().stream()
        .map(
            route -> {
              AbstractRouteBuilder<?, ?> builder = route.getRoute().toBuilder();
              boolean accept = policy.process(route, builder, null, _name, IN);
              return accept ? new AnnotatedRoute<AbstractRoute>(builder.build(), _name) : null;
            })
        .filter(Objects::nonNull)
        .forEach(
            r ->
                ribGroup
                    .getImportRibs()
                    .forEach(ribId -> _node.getRib(ribId).ifPresent(rib -> rib.mergeRoute(r))));
  }

  /** Initialize EIGRP processes */
  private void initEigrp() {
    _virtualEigrpProcesses =
        _vrf.getEigrpProcesses().values().stream()
            .map(eigrpProcess -> new VirtualEigrpProcess(eigrpProcess, _name, _c))
            .collect(ImmutableMap.toImmutableMap(VirtualEigrpProcess::getAsn, Function.identity()));
  }

  /**
   * Prepare for the EGP part of the computation
   *
   * @param topologyContext The various network topologies
   */
  void initForEgpComputation(TopologyContext topologyContext) {
    initQueuesAndDeltaBuilders(topologyContext);
    // Handle BGP process state
    if (_bgpRoutingProcess != null && !_bgpRoutingProcess.isInitialized()) {
      _bgpRoutingProcess.initialize(_node);
    }
    if (_bgpRoutingProcess != null) {
      // If the process exists, update the topology
      _bgpRoutingProcess.updateTopology(topologyContext.getBgpTopology());
    }
  }

  /**
   * Initializes RIB delta builders and protocol message queues.
   *
   * @param topologyContext The various network topologies
   */
  @VisibleForTesting
  void initQueuesAndDeltaBuilders(TopologyContext topologyContext) {
    // Initialize message queues for each EIGRP neighbor
    initEigrpQueues(topologyContext.getEigrpTopology());
    // Initialize message queues for each IS-IS neighbor
    initIsisQueues(topologyContext.getIsisTopology());
    // Initalize message queues for all neighboring VRFs/VirtualRouters
    initCrossVrfQueues();
  }

  /**
   * Goes through VRFs that can leak routes into this routing instance, and imports all routes from
   * their main ribs to {@link #_crossVrfIncomingRoutes}.
   */
  void initCrossVrfImports() {
    if (_vrf.getCrossVrfImportPolicy() == null || _vrf.getCrossVrfImportVrfs() == null) {
      return;
    }
    for (String vrfToImport : _vrf.getCrossVrfImportVrfs()) {
      VirtualRouter exportingVR = _node.getVirtualRouters().get(vrfToImport);
      CrossVrfEdgeId otherVrfToOurRib = new CrossVrfEdgeId(vrfToImport, RibId.DEFAULT_RIB_NAME);
      enqueueCrossVrfRoutes(
          otherVrfToOurRib,
          // TODO Will need to update once support is added for cross-VRF export policies
          exportingVR._mainRib.getTypedRoutes().stream().map(RouteAdvertisement::new),
          _vrf.getCrossVrfImportPolicy());
    }
  }

  /**
   * Initialize incoming EIGRP message queues for each adjacency
   *
   * @param eigrpTopology The topology representing EIGRP adjacencies
   */
  private void initEigrpQueues(EigrpTopology eigrpTopology) {
    _virtualEigrpProcesses.values().forEach(proc -> proc.initQueues(eigrpTopology));
  }

  private void initIsisQueues(IsisTopology isisTopology) {
    Network<IsisNode, IsisEdge> network = isisTopology.getNetwork();
    // Initialize message queues for each IS-IS circuit
    if (_vrf.getIsisProcess() == null) {
      _isisIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _isisIncomingRoutes =
          _vrf.getInterfaceNames().stream()
              .map(ifaceName -> new IsisNode(_c.getHostname(), ifaceName))
              .filter(network.nodes()::contains)
              .flatMap(n -> network.inEdges(n).stream())
              .collect(
                  toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    }
  }

  /**
   * Activate generated routes.
   *
   * @return a new {@link RibDelta} if a new route has been activated, otherwise {@code null}
   */
  private RibDelta<AnnotatedRoute<AbstractRoute>> activateGeneratedRoutes() {
    RibDelta.Builder<AnnotatedRoute<AbstractRoute>> builder = RibDelta.builder();

    /*
     * Loop over all generated routes and check whether any of the contributing routes can trigger
     * activation.
     */
    for (GeneratedRoute gr : _vrf.getGeneratedRoutes()) {
      String policyName = gr.getGenerationPolicy();
      RoutingPolicy generationPolicy =
          policyName != null ? _c.getRoutingPolicies().get(policyName) : null;
      GeneratedRoute.Builder grb =
          GeneratedRouteHelper.activateGeneratedRoute(
              gr, generationPolicy, _mainRib.getTypedRoutes(), _vrf.getName());

      if (grb != null) {
        // Routes have been changed
        builder.from(_generatedRib.mergeRouteGetDelta(annotateRoute(grb.build())));
      }
    }
    return builder.build();
  }

  /**
   * Recompute generated routes. If new generated routes were activated, process them into the main
   * RIB. Check if any BGP aggregates were affected by the new generated routes.
   */
  void recomputeGeneratedRoutes() {
    RibDelta<AnnotatedRoute<AbstractRoute>> d;
    RibDelta.Builder<AnnotatedRoute<AbstractRoute>> generatedRouteDeltaBuilder = RibDelta.builder();
    do {
      d = activateGeneratedRoutes();
      generatedRouteDeltaBuilder.from(d);
    } while (!d.isEmpty());

    d = generatedRouteDeltaBuilder.build();
    // Update main rib as well
    _mainRibRouteDeltaBuilder.from(importRibDelta(_mainRib, d));

    /*
     * Check dependencies for BGP aggregates.
     *
     * Updates from these BGP deltas into mainRib will be handled in finalizeBgp routes
     */
    if (!d.isEmpty()) {
      d.getActions()
          .filter(RouteAdvertisement::isWithdrawn)
          .forEach(
              r ->
                  _bgpRoutingProcess._bgpv4DeltaBuilder.from(
                      _bgpRoutingProcess._bgpAggDeps.deleteRoute(
                          r.getRoute().getRoute(), _bgpRoutingProcess._bgpv4Rib)));
    }
  }

  /**
   * Activate static routes with next hop IP. Adds a static route {@code route} to the main RIB if
   * there exists an active route to the {@code routes}'s next-hop-ip.
   *
   * <p>Removes static route from the main RIB for which next-hop-ip has become unreachable.
   */
  void activateStaticRoutes() {
    for (StaticRoute sr : _staticNextHopRib.getTypedRoutes()) {
      if (shouldActivateNextHopIpRoute(sr, _mainRib)) {
        _mainRibRouteDeltaBuilder.from(_mainRib.mergeRouteGetDelta(annotateRoute(sr)));
      } else {
        /*
         * If the route is not in the RIB, this has no effect. But might add some overhead (TODO)
         */
        _mainRibRouteDeltaBuilder.from(
            _mainRib.removeRouteGetDelta(annotateRoute(sr), Reason.WITHDRAW));
      }
    }
  }

  /** Compute the FIB from the main RIB */
  public void computeFib() {
    _fib = new FibImpl(_mainRib);
  }

  /**
   * Initializes BGP RIBs prior to any dataplane iterations based on the external BGP advertisements
   * coming into the network
   *
   * @param externalAdverts a set of external BGP advertisements
   * @param ipOwners mapping of IPs to their owners in our network
   * @param bgpTopology the bgp peering relationships
   */
  void initBaseBgpRibs(
      Set<BgpAdvertisement> externalAdverts,
      Map<Ip, Set<String>> ipOwners,
      final Map<String, Node> allNodes,
      BgpTopology bgpTopology,
      NetworkConfigurations networkConfigurations) {

    if (_bgpRoutingProcess == null) {
      // Nothing to do
      return;
    }

    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    Map<Bgpv4Rib, RibDelta.Builder<Bgpv4Route>> ribDeltas = new IdentityHashMap<>();
    ribDeltas.put(_bgpRoutingProcess._ebgpv4StagingRib, RibDelta.builder());
    ribDeltas.put(_bgpRoutingProcess._ibgpv4StagingRib, RibDelta.builder());

    Bgpv4Route.Builder outgoingRouteBuilder = new Bgpv4Route.Builder();
    // Process each BGP advertisement
    for (BgpAdvertisement advert : externalAdverts) {

      // If it is not for us, ignore it
      if (!advert.getDstNode().equals(_c.getHostname())) {
        continue;
      }

      // If we don't own the IP for this advertisement, ignore it
      Ip dstIp = advert.getDstIp();
      Set<String> dstIpOwners = ipOwners.get(dstIp);
      String hostname = _c.getHostname();
      if (dstIpOwners == null || !dstIpOwners.contains(hostname)) {
        continue;
      }

      Ip srcIp = advert.getSrcIp();
      // TODO: support passive and unnumbered bgp connections
      Prefix srcPrefix = Prefix.create(srcIp, Prefix.MAX_PREFIX_LENGTH);
      BgpPeerConfig neighbor = _vrf.getBgpProcess().getActiveNeighbors().get(srcPrefix);
      if (neighbor == null) {
        continue;
      }

      // Build a route based on the type of this advertisement
      BgpAdvertisementType type = advert.getType();
      boolean ebgp;
      boolean received;
      switch (type) {
        case EBGP_RECEIVED:
          ebgp = true;
          received = true;
          break;

        case EBGP_SENT:
          ebgp = true;
          received = false;
          break;

        case IBGP_RECEIVED:
          ebgp = false;
          received = true;
          break;

        case IBGP_SENT:
          ebgp = false;
          received = false;
          break;

        case EBGP_ORIGINATED:
        case IBGP_ORIGINATED:
        default:
          throw new BatfishException("Missing or invalid bgp advertisement type");
      }

      Bgpv4Rib targetRib =
          ebgp ? _bgpRoutingProcess._ebgpv4StagingRib : _bgpRoutingProcess._ibgpv4StagingRib;
      RoutingProtocol targetProtocol = ebgp ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
      int admin = _vrf.getBgpProcess().getAdminCost(targetProtocol);

      if (received) {
        AsPath asPath = advert.getAsPath();
        SortedSet<Long> clusterList = advert.getClusterList();
        SortedSet<Community> communities = ImmutableSortedSet.copyOf(advert.getCommunities());
        long localPreference = advert.getLocalPreference();
        long metric = advert.getMed();
        Prefix network = advert.getNetwork();
        Ip nextHopIp = advert.getNextHopIp();
        Ip originatorIp = advert.getOriginatorIp();
        OriginType originType = advert.getOriginType();
        RoutingProtocol srcProtocol = advert.getSrcProtocol();
        int weight = advert.getWeight();
        Bgpv4Route.Builder builder = new Bgpv4Route.Builder();
        builder.setAdmin(admin);
        builder.setAsPath(asPath);
        builder.setClusterList(clusterList);
        builder.setCommunities(communities);
        builder.setLocalPreference(localPreference);
        builder.setMetric(metric);
        builder.setNetwork(network);
        builder.setNextHopIp(nextHopIp);
        builder.setOriginatorIp(originatorIp);
        builder.setOriginType(originType);
        builder.setProtocol(targetProtocol);
        // TODO: support external route reflector clients
        builder.setReceivedFromIp(advert.getSrcIp());
        builder.setReceivedFromRouteReflectorClient(false);
        builder.setSrcProtocol(srcProtocol);
        // TODO: possibly support setting tag
        builder.setWeight(weight);
        Bgpv4Route route = builder.build();
        ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(route));
      } else {
        long localPreference;
        if (ebgp) {
          localPreference = Bgpv4Route.DEFAULT_LOCAL_PREFERENCE;
        } else {
          localPreference = advert.getLocalPreference();
        }
        outgoingRouteBuilder.setAsPath(advert.getAsPath());
        outgoingRouteBuilder.setCommunities(ImmutableSortedSet.copyOf(advert.getCommunities()));
        outgoingRouteBuilder.setLocalPreference(localPreference);
        outgoingRouteBuilder.setMetric(advert.getMed());
        outgoingRouteBuilder.setNetwork(advert.getNetwork());
        outgoingRouteBuilder.setNextHopIp(advert.getNextHopIp());
        outgoingRouteBuilder.setOriginatorIp(advert.getOriginatorIp());
        outgoingRouteBuilder.setOriginType(advert.getOriginType());
        outgoingRouteBuilder.setProtocol(targetProtocol);
        outgoingRouteBuilder.setReceivedFromIp(advert.getSrcIp());
        // TODO:
        // outgoingRouteBuilder.setReceivedFromRouteReflectorClient(...);
        outgoingRouteBuilder.setSrcProtocol(advert.getSrcProtocol());
        Bgpv4Route transformedOutgoingRoute = outgoingRouteBuilder.build();
        Bgpv4Route.Builder transformedIncomingRouteBuilder = new Bgpv4Route.Builder();

        // Incoming originatorIp
        transformedIncomingRouteBuilder.setOriginatorIp(transformedOutgoingRoute.getOriginatorIp());

        // Incoming receivedFromIp
        transformedIncomingRouteBuilder.setReceivedFromIp(
            transformedOutgoingRoute.getReceivedFromIp());

        // Incoming clusterList
        transformedIncomingRouteBuilder.addClusterList(transformedOutgoingRoute.getClusterList());

        // Incoming receivedFromRouteReflectorClient
        transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
            transformedOutgoingRoute.getReceivedFromRouteReflectorClient());

        // Incoming asPath
        transformedIncomingRouteBuilder.setAsPath(transformedOutgoingRoute.getAsPath());

        // Incoming communities
        transformedIncomingRouteBuilder.addCommunities(transformedOutgoingRoute.getCommunities());

        // Incoming protocol
        transformedIncomingRouteBuilder.setProtocol(targetProtocol);

        // Incoming network
        transformedIncomingRouteBuilder.setNetwork(transformedOutgoingRoute.getNetwork());

        // Incoming nextHopIp
        transformedIncomingRouteBuilder.setNextHopIp(transformedOutgoingRoute.getNextHopIp());

        // Incoming originType
        transformedIncomingRouteBuilder.setOriginType(transformedOutgoingRoute.getOriginType());

        // Incoming localPreference
        transformedIncomingRouteBuilder.setLocalPreference(
            transformedOutgoingRoute.getLocalPreference());

        // Incoming admin
        transformedIncomingRouteBuilder.setAdmin(admin);

        // Incoming metric
        transformedIncomingRouteBuilder.setMetric(transformedOutgoingRoute.getMetric());

        // Incoming srcProtocol
        transformedIncomingRouteBuilder.setSrcProtocol(targetProtocol);
        String importPolicyName = neighbor.getImportPolicy();
        // TODO: ensure there is always an import policy

        if (ebgp
            && transformedOutgoingRoute.getAsPath().containsAs(neighbor.getLocalAs())
            && !neighbor.getAllowLocalAsIn()) {
          // skip routes containing peer's AS unless
          // disable-peer-as-check (getAllowRemoteAsOut) is set
          continue;
        }

        /*
         * CREATE INCOMING ROUTE
         */
        boolean acceptIncoming = true;
        if (importPolicyName != null) {
          RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
          if (importPolicy != null) {
            // TODO Figure out whether transformedOutgoingRoute ought to have an annotation
            acceptIncoming =
                importPolicy.process(
                    transformedOutgoingRoute,
                    transformedIncomingRouteBuilder,
                    advert.getSrcIp(),
                    _name,
                    IN);
          }
        }
        if (acceptIncoming) {
          Bgpv4Route transformedIncomingRoute = transformedIncomingRouteBuilder.build();
          ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
        }
      }
    }

    // Propagate received routes through all the RIBs and send out appropriate messages to neighbors
    Map<Bgpv4Rib, RibDelta<Bgpv4Route>> deltas =
        ribDeltas.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().build()));
    finalizeBgpRoutesAndQueueOutgoingMessages(deltas, allNodes, bgpTopology, networkConfigurations);
  }

  /** Initialize RIP routes from the interface prefixes */
  @VisibleForTesting
  void initBaseRipRoutes() {
    if (_vrf.getRipProcess() == null) {
      return; // nothing to do
    }

    // init internal routes from connected routes
    for (String ifaceName : _vrf.getRipProcess().getInterfaces()) {
      Interface iface = _vrf.getInterfaces().get(ifaceName);
      if (iface.getActive()) {
        Set<Prefix> allNetworkPrefixes =
            iface.getAllConcreteAddresses().stream()
                .map(ConcreteInterfaceAddress::getPrefix)
                .collect(Collectors.toSet());
        long cost = RipProcess.DEFAULT_RIP_COST;
        for (Prefix prefix : allNetworkPrefixes) {
          RipInternalRoute route =
              new RipInternalRoute(
                  prefix,
                  Route.UNSET_ROUTE_NEXT_HOP_IP,
                  RoutingProtocol.RIP.getDefaultAdministrativeCost(_c.getConfigurationFormat()),
                  cost);
          _ripInternalRib.mergeRouteGetDelta(route);
        }
      }
    }
  }

  /**
   * This function creates BGP routes from generated routes that go into the BGP RIB, but cannot be
   * imported into the main RIB. The purpose of these routes is to prevent the local router from
   * accepting advertisements less desirable than the locally generated ones for a given network.
   */
  void initBgpAggregateRoutes() {
    if (_bgpRoutingProcess == null) {
      return;
    }
    // first import aggregates
    switch (_c.getConfigurationFormat()) {
      case JUNIPER:
      case JUNIPER_SWITCH:
        return;
        // $CASES-OMITTED$
      default:
        break;
    }
    for (AbstractRoute grAbstract : _generatedRib.getRoutes()) {
      GeneratedRoute gr = (GeneratedRoute) grAbstract;

      // Prevent route from being merged into the main RIB by marking it non-routing
      Bgpv4Route br =
          BgpProtocolHelper.convertGeneratedRouteToBgp(
              gr, _vrf.getBgpProcess().getRouterId(), true);
      /* TODO: tests for this */
      RibDelta<Bgpv4Route> d1 = _bgpRoutingProcess._bgpv4Rib.mergeRouteGetDelta(br);
      _bgpRoutingProcess._bgpv4DeltaBuilder.from(d1);
      if (!d1.isEmpty()) {
        _bgpRoutingProcess._bgpAggDeps.addRouteDependency(br, gr);
      }
    }
  }

  /**
   * Initialize the connected RIB -- a RIB containing connected routes (i.e., direct connections to
   * neighbors).
   */
  @VisibleForTesting
  void initConnectedRib() {
    // Look at all connected interfaces
    for (Interface i : _vrf.getInterfaces().values()) {
      if (i.getActive()) { // Make sure the interface is active
        // Create a route for each interface prefix
        for (ConcreteInterfaceAddress ifaceAddress : i.getAllConcreteAddresses()) {
          Prefix prefix = ifaceAddress.getPrefix();
          _connectedRib.mergeRoute(annotateRoute(new ConnectedRoute(prefix, i.getName())));
        }
      }
    }
  }

  /**
   * Initialize the kernel RIB -- a RIB containing non-forwarding routes installed unconditionally
   * for the purpose of redistribution
   */
  @VisibleForTesting
  void initKernelRib() {
    _vrf.getKernelRoutes().stream().map(this::annotateRoute).forEach(_kernelRib::mergeRoute);
  }

  /**
   * Initialize the local RIB -- a RIB containing non-forwarding /32 routes for exact addresses of
   * interfaces
   */
  @VisibleForTesting
  void initLocalRib() {
    // Look at all connected interfaces
    for (Interface i : _vrf.getInterfaces().values()) {
      if (i.getActive()) { // Make sure the interface is active
        // Create a route for each interface prefix
        for (ConcreteInterfaceAddress ifaceAddress : i.getAllConcreteAddresses()) {
          if (ifaceAddress.getNetworkBits() < Prefix.MAX_PREFIX_LENGTH) {
            _localRib.mergeRoute(annotateRoute(new LocalRoute(ifaceAddress, i.getName())));
          }
        }
      }
    }
  }

  /**
   * Initial computation of all exportable EIGRP routes for all EIGRP processes on this router
   *
   * @param allNodes map of all nodes, keyed by hostname
   */
  void initEigrpExports(Map<String, Node> allNodes) {
    _virtualEigrpProcesses
        .values()
        .forEach(proc -> proc.initExports(allNodes, _mainRib.getTypedRoutes()));
  }

  void initIsisExports(Map<String, Node> allNodes, NetworkConfigurations nc) {
    /* TODO: https://github.com/batfish/batfish/issues/1703 */
    IsisProcess proc = _vrf.getIsisProcess();
    if (proc == null) {
      return; // nothing to do
    }
    RibDelta.Builder<IsisRoute> d1 = RibDelta.builder();
    RibDelta.Builder<IsisRoute> d2 = RibDelta.builder();
    /*
     * init L1 and L2 routes from connected routes
     */
    int l1Admin = RoutingProtocol.ISIS_L1.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int l2Admin = RoutingProtocol.ISIS_L2.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    IsisLevelSettings l1Settings = proc.getLevel1();
    IsisLevelSettings l2Settings = proc.getLevel2();
    IsisRoute.Builder ifaceRouteBuilder =
        new IsisRoute.Builder()
            .setArea(proc.getNetAddress().getAreaIdString())
            .setSystemId(proc.getNetAddress().getSystemIdString());
    for (Interface iface : _vrf.getInterfaces().values()) {
      generateAllIsisInterfaceRoutes(
          d1, d2, l1Admin, l2Admin, l1Settings, l2Settings, ifaceRouteBuilder, iface);
    }

    // export default route for L1 neighbors on L1L2 routers that are not overloaded
    if (l1Settings != null && l2Settings != null && !proc.getOverload()) {
      IsisRoute defaultRoute =
          new IsisRoute.Builder()
              .setAdmin(l1Admin)
              .setArea(proc.getNetAddress().getAreaIdString())
              .setAttach(true)
              .setLevel(IsisLevel.LEVEL_1)
              .setMetric(0L)
              .setNetwork(Prefix.ZERO)
              .setNextHopIp(Route.UNSET_ROUTE_NEXT_HOP_IP)
              .setProtocol(RoutingProtocol.ISIS_L1)
              .setSystemId(proc.getNetAddress().getSystemIdString())
              .build();
      d1.from(_isisL1Rib.mergeRouteGetDelta(defaultRoute));
    }

    queueOutgoingIsisRoutes(allNodes, nc, d1.build(), d2.build());
  }

  /**
   * Generate IS-IS L1/L2 routes from a given interface and merge them into appropriate L1/L2 RIBs.
   */
  private void generateAllIsisInterfaceRoutes(
      Builder<IsisRoute> d1,
      Builder<IsisRoute> d2,
      int l1Admin,
      int l2Admin,
      @Nullable IsisLevelSettings l1Settings,
      @Nullable IsisLevelSettings l2Settings,
      IsisRoute.Builder routeBuilder,
      Interface iface) {
    IsisInterfaceSettings ifaceSettings = iface.getIsis();
    if (ifaceSettings == null) {
      return;
    }
    IsisInterfaceLevelSettings ifaceL1Settings = ifaceSettings.getLevel1();
    IsisInterfaceLevelSettings ifaceL2Settings = ifaceSettings.getLevel2();
    if (ifaceL1Settings != null && l1Settings != null) {
      generateIsisInterfaceRoutesPerLevel(l1Admin, routeBuilder, iface, IsisLevel.LEVEL_1)
          .forEach(r -> d1.from(_isisL1Rib.mergeRouteGetDelta(r)));
    }
    if (ifaceL2Settings != null && l2Settings != null) {
      generateIsisInterfaceRoutesPerLevel(l2Admin, routeBuilder, iface, IsisLevel.LEVEL_2)
          .forEach(r -> d2.from(_isisL2Rib.mergeRouteGetDelta(r)));
    }
  }

  /**
   * Generate IS-IS from a given interface for a given level (with a given metric/admin cost) and
   * merge them into the appropriate RIB.
   */
  private static Set<IsisRoute> generateIsisInterfaceRoutesPerLevel(
      int adminCost, IsisRoute.Builder routeBuilder, Interface iface, IsisLevel level) {
    IsisInterfaceLevelSettings ifaceLevelSettings =
        level == IsisLevel.LEVEL_1 ? iface.getIsis().getLevel1() : iface.getIsis().getLevel2();
    RoutingProtocol isisProtocol =
        level == IsisLevel.LEVEL_1 ? RoutingProtocol.ISIS_L1 : RoutingProtocol.ISIS_L2;
    long metric =
        ifaceLevelSettings.getMode() == IsisInterfaceMode.PASSIVE
            ? 0L
            : firstNonNull(ifaceLevelSettings.getCost(), IsisRoute.DEFAULT_METRIC);
    routeBuilder.setAdmin(adminCost).setLevel(level).setMetric(metric).setProtocol(isisProtocol);
    return iface.getAllConcreteAddresses().stream()
        .map(
            address ->
                routeBuilder.setNetwork(address.getPrefix()).setNextHopIp(address.getIp()).build())
        .collect(ImmutableSet.toImmutableSet());
  }

  @Nullable
  VirtualEigrpProcess getEigrpProcess(long asn) {
    return _virtualEigrpProcesses.get(asn);
  }

  /** Initialize all ribs on this router. All RIBs will be empty */
  @VisibleForTesting
  final void initRibs() {
    // Non-learned-protocol RIBs
    _connectedRib = new ConnectedRib();
    _kernelRib = new KernelRib();
    _localRib = new LocalRib();
    _generatedRib = new Rib();
    _independentRib = new Rib();

    // ISIS
    _isisRib = new IsisRib(isL1Only());
    _isisL1Rib = new IsisLevelRib(true);
    _isisL2Rib = new IsisLevelRib(true);
    _isisL1StagingRib = new IsisLevelRib(false);
    _isisL2StagingRib = new IsisLevelRib(false);

    // RIP
    _ripInternalRib = new RipInternalRib();
    _ripInternalStagingRib = new RipInternalRib();
    _ripRib = new RipRib();

    // Static
    _staticNextHopRib = new StaticRib();
    _staticInterfaceRib = new StaticRib();
  }

  private boolean isL1Only() {
    IsisProcess proc = _vrf.getIsisProcess();
    if (proc == null) {
      return false;
    }
    return proc.getLevel1() != null && proc.getLevel2() == null;
  }

  /**
   * Initialize the static route RIBs from the VRF config. Interface routes go into {@link
   * #_staticInterfaceRib}; routes that only have next-hop-ip go into {@link #_staticNextHopRib}
   */
  @VisibleForTesting
  void initStaticRibs() {
    for (StaticRoute sr : _vrf.getStaticRoutes()) {
      if (isInterfaceRoute(sr)) {
        // We have an interface route, check if interface is active
        Interface nextHopInterface = _c.getAllInterfaces().get(sr.getNextHopInterface());

        if (Interface.NULL_INTERFACE_NAME.equals(sr.getNextHopInterface())
            || (nextHopInterface != null && (nextHopInterface.getActive()))) {
          // Interface is active (or special null interface), install route
          _staticInterfaceRib.mergeRouteGetDelta(sr);
        }
      } else {
        if (Route.UNSET_ROUTE_NEXT_HOP_IP.equals(sr.getNextHopIp())) {
          continue;
        }
        // We have a next-hop-ip route, keep in it that RIB
        _staticNextHopRib.mergeRouteGetDelta(sr);
      }
    }
  }

  /**
   * Process BGP messages from neighbors, return a list of delta changes to the RIBs
   *
   * @param bgpTopology the bgp peering relationships
   * @return Map from a {@link Bgpv4Rib} to {@link RibDelta} objects
   */
  @Nonnull
  Map<Bgpv4Rib, RibDelta<Bgpv4Route>> processBgpMessages(
      BgpTopology bgpTopology, NetworkConfigurations nc, Map<String, Node> nodes) {

    // If we have no BGP process, nothing to do
    if (_bgpRoutingProcess == null) {
      return ImmutableMap.of();
    }

    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    Map<Bgpv4Rib, RibDelta.Builder<Bgpv4Route>> ribDeltas = new IdentityHashMap<>();
    ribDeltas.put(_bgpRoutingProcess._ebgpv4StagingRib, RibDelta.builder());
    ribDeltas.put(_bgpRoutingProcess._ibgpv4StagingRib, RibDelta.builder());

    // Process updates from each neighbor
    for (Entry<EdgeId, Queue<RouteAdvertisement<Bgpv4Route>>> e :
        _bgpRoutingProcess._bgpv4IncomingRoutes.entrySet()) {

      // Grab the queue containing all messages from remoteBgpPeerConfig
      Queue<RouteAdvertisement<Bgpv4Route>> queue = e.getValue();

      // Setup helper vars
      BgpPeerConfigId remoteConfigId = e.getKey().tail();
      BgpPeerConfigId ourConfigId = e.getKey().head();
      BgpSessionProperties sessionProperties =
          getBgpSessionProperties(bgpTopology, new EdgeId(remoteConfigId, ourConfigId));
      BgpPeerConfig ourBgpConfig = requireNonNull(nc.getBgpPeerConfig(e.getKey().head()));
      // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
      Ip remoteIp = sessionProperties.getTailIp();

      Bgpv4Rib targetRib =
          sessionProperties.isEbgp()
              ? _bgpRoutingProcess._ebgpv4StagingRib
              : _bgpRoutingProcess._ibgpv4StagingRib;
      Builder<AnnotatedRoute<AbstractRoute>> perNeighborDeltaForRibGroups = RibDelta.builder();

      // Process all routes from neighbor
      while (queue.peek() != null) {
        RouteAdvertisement<Bgpv4Route> remoteRouteAdvert = queue.remove();
        Bgpv4Route remoteRoute = remoteRouteAdvert.getRoute();

        Bgpv4Route.Builder transformedIncomingRouteBuilder =
            transformBgpRouteOnImport(
                remoteRoute,
                ourBgpConfig.getLocalAs(),
                ourBgpConfig.getAllowLocalAsIn(),
                sessionProperties.isEbgp(),
                _bgpRoutingProcess._process,
                ourConfigId.getPeerInterface());
        if (transformedIncomingRouteBuilder == null) {
          // Route could not be imported for core protocol reasons
          continue;
        }

        // Process route through import policy, if one exists
        String importPolicyName = ourBgpConfig.getImportPolicy();
        boolean acceptIncoming = true;
        // TODO: ensure there is always an import policy
        if (importPolicyName != null) {
          RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
          if (importPolicy != null) {
            acceptIncoming =
                importPolicy.process(
                    remoteRoute,
                    transformedIncomingRouteBuilder,
                    remoteIp,
                    ourConfigId.getRemotePeerPrefix(),
                    _name,
                    IN);
          }
        }
        if (!acceptIncoming) {
          // Route could not be imported due to routing policy
          _prefixTracer.filtered(
              remoteRoute.getNetwork(),
              ourConfigId.getHostname(),
              remoteIp,
              remoteConfigId.getVrfName(),
              importPolicyName,
              IN);
          continue;
        }
        Bgpv4Route transformedIncomingRoute = transformedIncomingRouteBuilder.build();

        // If new route gets leaked to other VRFs via RibGroup, this VRF should be its source
        AnnotatedRoute<AbstractRoute> annotatedTransformedRoute =
            annotateRoute(transformedIncomingRoute);

        if (remoteRouteAdvert.isWithdrawn()) {
          // Note this route was removed
          ribDeltas.get(targetRib).remove(transformedIncomingRoute, Reason.WITHDRAW);
          perNeighborDeltaForRibGroups.remove(annotatedTransformedRoute, Reason.WITHDRAW);
        } else {
          // Merge into staging rib, note delta
          ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
          perNeighborDeltaForRibGroups.add(annotatedTransformedRoute);
          _prefixTracer.installed(
              transformedIncomingRoute.getNetwork(),
              remoteConfigId.getHostname(),
              remoteIp,
              remoteConfigId.getVrfName(),
              importPolicyName);
        }
      }
      // Apply rib groups if any
      RibGroup rg = ourBgpConfig.getAppliedRibGroup();
      if (rg != null) {
        rg.getImportRibs()
            .forEach(
                rib ->
                    nodes
                        .get(_c.getHostname())
                        .getVirtualRouters()
                        .get(rib.getVrfName())
                        .enqueueCrossVrfRoutes(
                            new CrossVrfEdgeId(_name, rib.getRibName()),
                            perNeighborDeltaForRibGroups.build().getActions(),
                            rg.getImportPolicy()));
      }
    }
    // Return built deltas from RibDelta builders
    Map<Bgpv4Rib, RibDelta<Bgpv4Route>> builtDeltas = new IdentityHashMap<>();
    ribDeltas.forEach((rib, deltaBuilder) -> builtDeltas.put(rib, deltaBuilder.build()));
    return builtDeltas;
  }

  @Nullable
  Entry<RibDelta<IsisRoute>, RibDelta<IsisRoute>> propagateIsisRoutes(NetworkConfigurations nc) {
    if (_vrf.getIsisProcess() == null) {
      return null;
    }
    RibDelta.Builder<IsisRoute> l1DeltaBuilder = RibDelta.builder();
    RibDelta.Builder<IsisRoute> l2DeltaBuilder = RibDelta.builder();
    int l1Admin = RoutingProtocol.ISIS_L1.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int l2Admin = RoutingProtocol.ISIS_L2.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    _isisIncomingRoutes.forEach(
        (edge, queue) -> {
          Ip nextHopIp = edge.getNode1().getInterface(nc).getConcreteAddress().getIp();
          Interface iface = edge.getNode2().getInterface(nc);
          while (queue.peek() != null) {
            RouteAdvertisement<IsisRoute> routeAdvert = queue.remove();
            IsisRoute neighborRoute = routeAdvert.getRoute();
            IsisLevel routeLevel = neighborRoute.getLevel();
            IsisInterfaceLevelSettings isisLevelSettings =
                routeLevel == IsisLevel.LEVEL_1
                    ? iface.getIsis().getLevel1()
                    : iface.getIsis().getLevel2();

            // Do not propagate route if ISIS interface is not active at this level
            if (isisLevelSettings.getMode() != IsisInterfaceMode.ACTIVE) {
              break;
            }
            boolean withdraw = routeAdvert.isWithdrawn();
            int adminCost = routeLevel == IsisLevel.LEVEL_1 ? l1Admin : l2Admin;
            RoutingProtocol levelProtocol =
                routeLevel == IsisLevel.LEVEL_1 ? RoutingProtocol.ISIS_L1 : RoutingProtocol.ISIS_L2;
            RibDelta.Builder<IsisRoute> deltaBuilder =
                routeLevel == IsisLevel.LEVEL_1 ? l1DeltaBuilder : l2DeltaBuilder;
            long incrementalMetric =
                firstNonNull(isisLevelSettings.getCost(), IsisRoute.DEFAULT_METRIC);
            IsisRoute newRoute =
                neighborRoute
                    .toBuilder()
                    .setAdmin(adminCost)
                    .setLevel(routeLevel)
                    .setMetric(incrementalMetric + neighborRoute.getMetric())
                    .setNextHopIp(nextHopIp)
                    .setProtocol(levelProtocol)
                    .build();
            if (withdraw) {
              deltaBuilder.remove(newRoute, Reason.WITHDRAW);
            } else {
              IsisLevelRib levelStagingRib =
                  routeLevel == IsisLevel.LEVEL_1 ? _isisL1StagingRib : _isisL2StagingRib;
              deltaBuilder.from(levelStagingRib.mergeRouteGetDelta(newRoute));
            }
          }
        });
    return new SimpleEntry<>(l1DeltaBuilder.build(), l2DeltaBuilder.build());
  }

  /**
   * Propagate EIGRP external routes from our neighbors by reading EIGRP route "advertisements" from
   * our queues.
   *
   * @param allNodes mapping of node names to instances.
   * @param nc All network configurations
   * @return true if external routes changed
   */
  boolean propagateEigrpExternalRoutes(Map<String, Node> allNodes, NetworkConfigurations nc) {
    return _virtualEigrpProcesses.values().stream()
        .map(
            proc ->
                proc.unstageExternalRoutes(
                    allNodes,
                    proc.propagateExternalRoutes(nc),
                    _mainRibRouteDeltaBuilder,
                    _mainRib,
                    _name))
        .reduce(false, (a, b) -> a || b);
  }

  /**
   * Propagate EIGRP internal routes from every valid EIGRP neighbors
   *
   * @param nodes mapping of node names to instances.
   * @param topologyContext network topologies
   * @param nc All network configurations
   * @return true if new routes have been added to the staging RIB
   */
  boolean propagateEigrpInternalRoutes(
      Map<String, Node> nodes, TopologyContext topologyContext, NetworkConfigurations nc) {
    return _virtualEigrpProcesses.values().stream()
        .map(proc -> proc.propagateInternalRoutes(nodes, topologyContext.getEigrpTopology(), nc))
        .reduce(false, (a, b) -> a || b);
  }

  /**
   * Process RIP routes from our neighbors.
   *
   * @param nodes Mapping of node names to Node instances
   * @param topology The network topology
   * @return True if the rib has changed as a result of route propagation
   */
  boolean propagateRipInternalRoutes(Map<String, Node> nodes, Topology topology) {
    boolean changed = false;

    // No rip process, nothing to do
    if (_vrf.getRipProcess() == null) {
      return false;
    }

    String node = _c.getHostname();
    int admin = RoutingProtocol.RIP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so RIP won't produce anything
      return false;
    }

    for (Edge edge : edges) {
      // Do not accept routes from ourselves
      if (!edge.getNode1().equals(node)) {
        continue;
      }

      // Get interface
      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }

      // Get the neighbor and its interface + VRF
      String neighborName = edge.getNode2();
      Node neighbor = nodes.get(neighborName);
      String neighborInterfaceName = edge.getInt2();
      Interface neighborInterface =
          neighbor.getConfiguration().getAllInterfaces().get(neighborInterfaceName);
      String neighborVrfName = neighborInterface.getVrfName();
      VirtualRouter neighborVirtualRouter =
          nodes.get(neighborName).getVirtualRouters().get(neighborVrfName);

      if (connectingInterface.getRipEnabled()
          && !connectingInterface.getRipPassive()
          && neighborInterface.getRipEnabled()
          && !neighborInterface.getRipPassive()) {
        /*
         * We have a RIP neighbor relationship on this edge. So we should add all RIP routes
         * from this neighbor into our RIP internal staging rib, adding the incremental cost
         * (?), and using the neighborInterface's address as the next hop ip
         */
        for (RipInternalRoute neighborRoute :
            neighborVirtualRouter._ripInternalRib.getTypedRoutes()) {
          long newCost = neighborRoute.getMetric() + RipProcess.DEFAULT_RIP_COST;
          Ip nextHopIp = neighborInterface.getConcreteAddress().getIp();
          RipInternalRoute newRoute =
              new RipInternalRoute(neighborRoute.getNetwork(), nextHopIp, admin, newCost);
          if (!_ripInternalStagingRib.mergeRouteGetDelta(newRoute).isEmpty()) {
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  /**
   * Queue advertised BGP routes to all BGP neighbors.
   *
   * @param ebgpBestPathDelta {@link RibDelta} indicating what changed in the {@link
   *     BgpRoutingProcess#_ebgpv4Rib}
   * @param bgpDelta a {@link RibDelta} indicating what changed in the {@link
   *     BgpRoutingProcess#_bgpv4Rib}
   * @param mainDelta a {@link RibDelta} indicating what changed in the {@link #_mainRib}
   * @param allNodes map of all nodes in the network, keyed by hostname
   * @param bgpTopology the bgp peering relationships
   */
  private void queueOutgoingBgpRoutes(
      RibDelta<Bgpv4Route> ebgpBestPathDelta,
      RibDelta<Bgpv4Route> bgpDelta,
      RibDelta<AnnotatedRoute<AbstractRoute>> mainDelta,
      final Map<String, Node> allNodes,
      BgpTopology bgpTopology,
      NetworkConfigurations networkConfigurations) {
    for (EdgeId edge : _bgpRoutingProcess._bgpv4IncomingRoutes.keySet()) {
      queueOutgoingRoutesPerEdge(
          edge,
          ebgpBestPathDelta,
          bgpDelta,
          mainDelta,
          allNodes,
          bgpTopology,
          networkConfigurations);
    }
  }

  private void queueOutgoingRoutesPerEdge(
      EdgeId edge,
      RibDelta<Bgpv4Route> ebgpBestPathDelta,
      RibDelta<Bgpv4Route> bgpDelta,
      RibDelta<AnnotatedRoute<AbstractRoute>> mainDelta,
      Map<String, Node> allNodes,
      BgpTopology bgpTopology,
      NetworkConfigurations networkConfigurations) {
    final BgpSessionProperties session = getBgpSessionProperties(bgpTopology, edge);

    BgpPeerConfigId remoteConfigId = edge.tail();
    BgpPeerConfigId ourConfigId = edge.head();
    BgpPeerConfig ourConfig = networkConfigurations.getBgpPeerConfig(edge.head());
    BgpPeerConfig remoteConfig = networkConfigurations.getBgpPeerConfig(edge.tail());
    VirtualRouter remoteVirtualRouter = getRemoteBgpNeighborVR(remoteConfigId, allNodes);
    if (remoteVirtualRouter == null) {
      return;
    }

    // Queue mainRib updates that were not introduced by BGP process (i.e., IGP routes)
    // Also, do not double-export main RIB routes
    Stream<RouteAdvertisement<Bgpv4Route>> mainRibExports =
        mainDelta
            .getActions()
            .filter(adv -> !(adv.getRoute().getRoute() instanceof BgpRoute))
            .map(
                adv -> {
                  Bgpv4Route bgpRoute =
                      exportNonBgpRouteToBgp(
                          adv.getRoute(), ourConfigId, remoteConfigId, ourConfig, session);
                  return bgpRoute == null
                      ? null
                      : RouteAdvertisement.<Bgpv4Route>builder()
                          .setReason(adv.getReason())
                          .setRoute(bgpRoute)
                          .build();
                })
            .filter(Objects::nonNull);

    // Needs to retain annotations since export policy will be run on routes from resulting delta.
    Builder<AnnotatedRoute<Bgpv4Route>> bgpRibExports = RibDelta.builder();
    /*
     * By default only best-path routes from the BGP RIB that are **also installed in the main RIB**
     * will be advertised to our neighbors.
     *
     * However, there are additional knobs that control re-advertisement behavior:
     *
     * 1. Advertise external: advertise best-path eBGP routes to iBGP peers regardless of whether
     *    they are global BGP best-paths.
     * 2. Advertise inactive: advertise best-path BGP routes to neighboring peers even if
     *    they are not active in the main RIB.
     */
    if (session.getAdvertiseExternal()) {
      importDeltaToBuilder(bgpRibExports, ebgpBestPathDelta, _name);
    }
    if (session.getAdvertiseInactive()) {
      importDeltaToBuilder(bgpRibExports, bgpDelta, _name);
    } else {
      // Default behavior
      bgpRibExports.from(
          bgpDelta
              .getActions()
              .map(
                  r ->
                      RouteAdvertisement.<AnnotatedRoute<Bgpv4Route>>builder()
                          .setReason(r.getReason())
                          .setRoute(annotateRoute(r.getRoute()))
                          .build())
              .filter(r -> _mainRib.containsRoute(r.getRoute())));
    }

    /*
    * TODO: https://github.com/batfish/batfish/issues/704
       Add path is broken for all intents and purposes.
       Need support for additional-paths based on https://tools.ietf.org/html/rfc7911
       AND the combination of vendor-specific knobs, none of which are currently supported.
    */
    if (session.getAdditionalPaths()) {
      importDeltaToBuilder(bgpRibExports, bgpDelta, _name);
    }

    RibDelta<AnnotatedRoute<Bgpv4Route>> bgpRoutesToExport = bgpRibExports.build();
    // Compute a set of advertisements that can be queued on remote VR
    Stream<RouteAdvertisement<Bgpv4Route>> exportedAdvertisements =
        Stream.concat(
            bgpRoutesToExport
                .getActions()
                .map(
                    adv -> {
                      Bgpv4Route transformedRoute =
                          exportBgpRoute(
                              adv.getRoute(),
                              ourConfigId,
                              remoteConfigId,
                              ourConfig,
                              remoteConfig,
                              allNodes,
                              session);
                      return transformedRoute == null
                          ? null
                          // REPLACE does not make sense across routers, update with WITHDRAW
                          : RouteAdvertisement.<Bgpv4Route>builder()
                              .setReason(
                                  adv.getReason() == Reason.REPLACE
                                      ? Reason.WITHDRAW
                                      : adv.getReason())
                              .setRoute(transformedRoute)
                              .build();
                    })
                .filter(Objects::nonNull),
            mainRibExports);

    // Call this on the REMOTE VR and REVERSE the edge!
    remoteVirtualRouter.enqueueBgpMessages(edge.reverse(), exportedAdvertisements);
  }

  private static BgpSessionProperties getBgpSessionProperties(
      BgpTopology bgpTopology, EdgeId edge) {
    // BGP topology edge guaranteed to exist since the session is established
    Optional<BgpSessionProperties> session =
        bgpTopology.getGraph().edgeValue(edge.tail(), edge.head());
    return session.orElseThrow(
        () -> new IllegalArgumentException(String.format("No BGP edge %s in BGP topology", edge)));
  }

  private void queueOutgoingIsisRoutes(
      @Nonnull Map<String, Node> allNodes,
      NetworkConfigurations nc,
      @Nonnull RibDelta<IsisRoute> l1delta,
      @Nonnull RibDelta<IsisRoute> l2delta) {
    if (_vrf.getIsisProcess() == null || _isisIncomingRoutes == null) {
      return;
    }
    // All outgoing routes should have overload bit set
    RibDelta<IsisRoute> correctedL1Delta =
        _vrf.getIsisProcess().getOverload() ? setOverloadOnAllRoutes(l1delta) : l1delta;
    RibDelta<IsisRoute> correctedL2Delta =
        _vrf.getIsisProcess().getOverload() ? setOverloadOnAllRoutes(l2delta) : l2delta;
    // Loop over neighbors, enqueue messages
    for (IsisEdge edge : _isisIncomingRoutes.keySet()) {
      // Do not queue routes on non-active ISIS interface levels
      Interface iface = edge.getNode2().getInterface(nc);
      IsisInterfaceLevelSettings level1Settings = iface.getIsis().getLevel1();
      IsisInterfaceLevelSettings level2Settings = iface.getIsis().getLevel2();
      IsisLevel activeLevels = null;
      if (level1Settings != null && level1Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.LEVEL_1;
      }
      if (level2Settings != null && level2Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.union(activeLevels, IsisLevel.LEVEL_2);
      }
      if (activeLevels == null) {
        return;
      }

      VirtualRouter remoteVr =
          allNodes
              .get(edge.getNode1().getNode())
              .getVirtualRouters()
              .get(edge.getNode1().getInterface(nc).getVrfName());
      Queue<RouteAdvertisement<IsisRoute>> queue = remoteVr._isisIncomingRoutes.get(edge.reverse());
      IsisLevel circuitType = edge.getCircuitType();
      if (circuitType.includes(IsisLevel.LEVEL_1) && activeLevels.includes(IsisLevel.LEVEL_1)) {
        queueDelta(queue, correctedL1Delta);
      }
      if (circuitType.includes(IsisLevel.LEVEL_2) && activeLevels.includes(IsisLevel.LEVEL_2)) {
        queueDelta(queue, correctedL2Delta);
        if (_vrf.getIsisProcess().getLevel1() != null
            && _vrf.getIsisProcess().getLevel2() != null
            // An L1-L2 router in overload mode stops leaking route information between L1 and L2
            // levels and clears its attached bit.
            && !_vrf.getIsisProcess().getOverload()
            && correctedL1Delta != null) {

          // We are a L1_L2 router, we must "upgrade" L1 routes to L2 routes
          // TODO: a little cumbersome, simplify later
          RibDelta.Builder<IsisRoute> upgradedRoutes = RibDelta.builder();
          correctedL1Delta
              .getActions()
              .forEach(
                  ra -> {
                    Optional<IsisRoute> newRoute =
                        convertRouteLevel1ToLevel2(
                            ra.getRoute(),
                            RoutingProtocol.ISIS_L2.getDefaultAdministrativeCost(
                                _c.getConfigurationFormat()));
                    if (newRoute.isPresent()) {
                      if (ra.isWithdrawn()) {
                        upgradedRoutes.remove(newRoute.get(), ra.getReason());
                      } else {
                        upgradedRoutes.add(newRoute.get());
                      }
                    }
                  });
          queueDelta(queue, upgradedRoutes.build());
        }
      }
    }
  }

  /**
   * Propagate BGP routes received from neighbours into the appropriate RIBs. As the propagation is
   * happening, queue appropriate outgoing messages to neighbors as well.
   *
   * @param stagingDeltas a map of RIB to corresponding delta. Keys are expected to contain {@link
   *     BgpRoutingProcess#_ebgpv4StagingRib} and {@link BgpRoutingProcess#_ibgpv4StagingRib}
   * @param bgpTopology the bgp peering relationships
   */
  void finalizeBgpRoutesAndQueueOutgoingMessages(
      Map<Bgpv4Rib, RibDelta<Bgpv4Route>> stagingDeltas,
      final Map<String, Node> allNodes,
      BgpTopology bgpTopology,
      NetworkConfigurations networkConfigurations) {

    if (_bgpRoutingProcess == null) {
      return;
    }

    RibDelta<Bgpv4Route> ebgpStagingDelta =
        stagingDeltas.getOrDefault(_bgpRoutingProcess._ebgpv4StagingRib, RibDelta.empty());
    RibDelta<Bgpv4Route> ibgpStagingDelta =
        stagingDeltas.getOrDefault(_bgpRoutingProcess._ibgpv4StagingRib, RibDelta.empty());

    RibDelta<Bgpv4Route> ebgpDelta =
        importRibDelta(_bgpRoutingProcess._ebgpv4Rib, ebgpStagingDelta);
    RibDelta<Bgpv4Route> ibgpDelta =
        importRibDelta(_bgpRoutingProcess._ibgpv4Rib, ibgpStagingDelta);
    _bgpRoutingProcess._bgpv4DeltaBuilder.from(
        importRibDelta(_bgpRoutingProcess._bgpv4Rib, ebgpDelta));
    _bgpRoutingProcess._bgpv4DeltaBuilder.from(
        importRibDelta(_bgpRoutingProcess._bgpv4Rib, ibgpDelta));
    _mainRibRouteDeltaBuilder.from(
        RibDelta.importRibDelta(_mainRib, _bgpRoutingProcess._bgpv4DeltaBuilder.build(), _name));

    queueOutgoingBgpRoutes(
        ebgpDelta,
        _bgpRoutingProcess._bgpv4DeltaBuilder.build(),
        _mainRibRouteDeltaBuilder.build(),
        allNodes,
        bgpTopology,
        networkConfigurations);
  }

  /** Merges staged EIGRP internal routes into the "real" EIGRP-internal RIBs */
  void unstageEigrpInternalRoutes() {
    _virtualEigrpProcesses.values().forEach(VirtualEigrpProcess::unstageInternalRoutes);
  }

  /**
   * Move IS-IS routes from L1/L2 staging RIBs into their respective "proper" RIBs. Following that,
   * move any resulting deltas into the combined IS-IS RIB, and finally, main RIB.
   *
   * @param allNodes all network nodes, keyed by hostname
   * @param l1Delta staging Level 1 delta
   * @param l2Delta staging Level 2 delta
   * @return true if any routes from given deltas were merged into the combined IS-IS RIB.
   */
  boolean unstageIsisRoutes(
      Map<String, Node> allNodes,
      NetworkConfigurations nc,
      RibDelta<IsisRoute> l1Delta,
      RibDelta<IsisRoute> l2Delta) {
    RibDelta<IsisRoute> d1 = importRibDelta(_isisL1Rib, l1Delta);
    RibDelta<IsisRoute> d2 = importRibDelta(_isisL2Rib, l2Delta);
    queueOutgoingIsisRoutes(allNodes, nc, d1, d2);
    Builder<IsisRoute> isisDeltaBuilder = RibDelta.builder();
    isisDeltaBuilder.from(importRibDelta(_isisRib, d1));
    isisDeltaBuilder.from(importRibDelta(_isisRib, d2));
    _mainRibRouteDeltaBuilder.from(
        RibDelta.importRibDelta(_mainRib, isisDeltaBuilder.build(), _name));
    return !d1.isEmpty() || !d2.isEmpty();
  }

  /** Merges staged RIP routes into the "real" RIP RIB */
  void unstageRipInternalRoutes() {
    importRib(_ripInternalRib, _ripInternalStagingRib);
  }

  /** Re-initialize RIBs (at the start of each iteration). */
  void reinitForNewIteration() {
    _mainRibRouteDeltaBuilder = RibDelta.builder();

    /*
     * RIBs not read from can just be re-initialized
     */
    _ripRib = new RipRib();

    /*
     * Add routes that cannot change (does not affect below computation)
     */
    _mainRibRouteDeltaBuilder.from(importRib(_mainRib, _independentRib));

    /*
     * Re-add independent RIP routes to ripRib for tie-breaking
     */
    importRib(_ripRib, _ripInternalRib);
    /*
     * Re-init/re-add routes for all EIGRP processes
     */
    _virtualEigrpProcesses.values().forEach(VirtualEigrpProcess::reInitForNewIteration);
  }

  /**
   * Merge internal EIGRP RIBs into a general EIGRP RIB, then merge that into the independent RIB
   */
  void importEigrpInternalRoutes() {
    _virtualEigrpProcesses
        .values()
        .forEach(process -> process.importInternalRoutes(_independentRib, _name));
  }

  /**
   * Queues initial round of outgoing BGP messages based on the state of the RIBs prior to any data
   * plane iterations.
   */
  void queueInitialBgpMessages(
      final BgpTopology bgpTopology, final Map<String, Node> allNodes, NetworkConfigurations nc) {
    if (_bgpRoutingProcess == null) {
      // nothing to do
      return;
    }
    for (EdgeId edge : _bgpRoutingProcess._bgpv4IncomingRoutes.keySet()) {
      newBgpSessionEstablishedHook(
          edge, getBgpSessionProperties(bgpTopology, edge), allNodes, nc, bgpTopology);
    }
  }

  /** Deal with a newly established BGP session. */
  private void newBgpSessionEstablishedHook(
      @Nonnull EdgeId edge,
      @Nonnull BgpSessionProperties sessionProperties,
      @Nonnull Map<String, Node> allNodes,
      NetworkConfigurations nc,
      BgpTopology topology) {

    BgpPeerConfigId localConfigId = edge.head();
    BgpPeerConfigId remoteConfigId = edge.tail();
    BgpPeerConfig localConfig = nc.getBgpPeerConfig(localConfigId);
    BgpPeerConfig remoteConfig = nc.getBgpPeerConfig(remoteConfigId);

    VirtualRouter remoteVr = getRemoteBgpNeighborVR(remoteConfigId, allNodes);
    if (remoteVr == null) {
      return;
    }

    /*
    TODO:
      match up prefix tracer with proper prefixes. Low priority,
      currently originated prefixes are not exposed to users
     */
    // Note prefixes we tried to originate
    _mainRib.getTypedRoutes().forEach(r -> _prefixTracer.originated(r.getNetwork()));

    /*
     * Export routes by looking at main RIB and BGPv4 RIB
     */
    queueOutgoingRoutesPerEdge(
        edge,
        RibDelta.<Bgpv4Route>builder()
            .add(_bgpRoutingProcess._ebgpv4Rib.getBestPathRoutes())
            .build(),
        RibDelta.<Bgpv4Route>builder().add(_bgpRoutingProcess._bgpv4Rib.getTypedRoutes()).build(),
        RibDelta.<AnnotatedRoute<AbstractRoute>>builder().add(_mainRib.getTypedRoutes()).build(),
        allNodes,
        topology,
        nc);

    /*
     * Export neighbor-specific generated routes, these routes skip global export policy
     */
    Set<RouteAdvertisement<Bgpv4Route>> exportedNeighborSpecificRoutes =
        localConfig.getGeneratedRoutes().stream()
            .map(
                r -> {
                  // Activate route and convert to BGP if activated
                  Bgpv4Route bgpv4Route = processNeighborSpecificGeneratedRoute(r);
                  if (bgpv4Route == null) {
                    // Route was not activated
                    return null;
                  }
                  // Run pre-export transform, export policy, & post-export transform
                  return exportBgpRoute(
                      annotateRoute(bgpv4Route),
                      localConfigId,
                      remoteConfigId,
                      localConfig,
                      remoteConfig,
                      allNodes,
                      sessionProperties);
                })
            .filter(Objects::nonNull)
            .map(RouteAdvertisement::new)
            .collect(ImmutableSet.toImmutableSet());

    // Call this on the neighbor's VR, and reverse the edge!
    remoteVr.enqueueBgpMessages(edge.reverse(), exportedNeighborSpecificRoutes);
  }

  /**
   * Check whether given {@link GeneratedRoute} should be sent to a BGP neighbor. This checks
   * activation conditions for the generated route, and converts it to a {@link Bgpv4Route}. No
   * export policy computation is performed.
   *
   * @param generatedRoute route to process
   * @return a new {@link Bgpv4Route} if the {@code generatedRoute} was activated.
   */
  @Nullable
  private Bgpv4Route processNeighborSpecificGeneratedRoute(@Nonnull GeneratedRoute generatedRoute) {
    String policyName = generatedRoute.getGenerationPolicy();
    RoutingPolicy policy = policyName != null ? _c.getRoutingPolicies().get(policyName) : null;
    GeneratedRoute.Builder builder =
        GeneratedRouteHelper.activateGeneratedRoute(
            generatedRoute, policy, _mainRib.getTypedRoutes(), _vrf.getName());
    return builder != null
        ? BgpProtocolHelper.convertGeneratedRouteToBgp(
            builder.build(), _vrf.getBgpProcess().getRouterId(), false)
        : null;
  }

  public Configuration getConfiguration() {
    return _c;
  }

  ConnectedRib getConnectedRib() {
    return _connectedRib;
  }

  public Fib getFib() {
    return _fib;
  }

  Rib getMainRib() {
    return _mainRib;
  }

  /** Get current BGP routes. To be used during dataplane computation only */
  Set<Bgpv4Route> getBgpRoutes() {
    return _bgpRoutingProcess == null
        ? ImmutableSet.of()
        : _bgpRoutingProcess._bgpv4Rib.getTypedRoutes();
  }

  /** Get the number of best-path BGP routes. To be used during dataplane computation only */
  int getNumBgpBestPaths() {
    return _bgpRoutingProcess == null ? 0 : _bgpRoutingProcess._bgpv4Rib.getBestPathRoutes().size();
  }

  /**
   * Get the number of all BGP routes (with multipath, if applicable). To be used during dataplane
   * computation only
   */
  int getNumBgpPaths() {
    return _bgpRoutingProcess == null ? 0 : _bgpRoutingProcess._bgpv4Rib.getTypedRoutes().size();
  }

  /** Convenience method to get the VirtualRouter's hostname */
  String getHostname() {
    return _c.getHostname();
  }

  /**
   * Compute the "hashcode" of this router for the iBDP purposes. The hashcode is computed from the
   * following data structures:
   *
   * <ul>
   *   <li>RIBs (e.g., {@link #_mainRib})
   *   <li>message queues
   *   <li>Routing processes
   * </ul>
   *
   * @return integer hashcode
   */
  int computeIterationHashCode() {
    return Streams.concat(
            // RIB State
            Stream.of(_mainRib.getTypedRoutes()),
            // Message queues
            Stream.of(_isisIncomingRoutes, _crossVrfIncomingRoutes)
                .flatMap(m -> m.values().stream())
                .flatMap(Queue::stream),
            // Processes
            Stream.of(_ospfProcesses.values().stream().map(OspfRoutingProcess::iterationHashCode)),
            Stream.of(_virtualEigrpProcesses)
                .flatMap(m -> m.values().stream())
                .map(VirtualEigrpProcess::computeIterationHashCode),
            Stream.of(_bgpRoutingProcess == null ? 0 : _bgpRoutingProcess.iterationHashCode()))
        .collect(toOrderedHashCode());
  }

  PrefixTracer getPrefixTracer() {
    return _prefixTracer;
  }

  /**
   * Given an {@link AbstractRoute}, run it through the BGP outbound transformations and export
   * routing policy.
   *
   * @param exportCandidate a route to try and export
   * @param ourConfig {@link BgpPeerConfig} that sends the route
   * @param sessionProperties {@link BgpSessionProperties} representing the <em>incoming</em> edge:
   *     i.e. the edge from {@code remoteConfig} to {@code ourConfig}
   * @return The transformed route as a {@link Bgpv4Route}, or {@code null} if the route should not
   *     be exported.
   */
  @Nullable
  private Bgpv4Route exportNonBgpRouteToBgp(
      @Nonnull AnnotatedRoute<AbstractRoute> exportCandidate,
      @Nonnull BgpPeerConfigId ourConfigId,
      @Nonnull BgpPeerConfigId remoteConfigId,
      @Nonnull BgpPeerConfig ourConfig,
      @Nonnull BgpSessionProperties sessionProperties) {

    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(ourConfig.getExportPolicy());
    RoutingProtocol protocol =
        sessionProperties.isEbgp() ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
    Bgpv4Route.Builder transformedOutgoingRouteBuilder =
        exportCandidate.getRoute() instanceof GeneratedRoute
            ? BgpProtocolHelper.convertGeneratedRouteToBgp(
                    (GeneratedRoute) exportCandidate.getRoute(),
                    _bgpRoutingProcess.getRouterId(),
                    false)
                .toBuilder()
            : BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
                exportCandidate,
                _bgpRoutingProcess.getRouterId(),
                sessionProperties.getHeadIp(),
                _bgpRoutingProcess._process.getAdminCost(protocol),
                protocol);

    // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
    Ip remoteIp = sessionProperties.getHeadIp();

    // Process transformed outgoing route by the export policy
    boolean shouldExport =
        exportPolicy.process(
            exportCandidate,
            transformedOutgoingRouteBuilder,
            remoteIp,
            ourConfigId.getRemotePeerPrefix(),
            ourConfigId.getVrfName(),
            Direction.OUT);

    if (!shouldExport) {
      // This route could not be exported due to export policy
      _prefixTracer.filtered(
          exportCandidate.getNetwork(),
          remoteConfigId.getHostname(),
          remoteIp,
          remoteConfigId.getVrfName(),
          ourConfig.getExportPolicy(),
          Direction.OUT);
      return null;
    }

    // Apply final post-policy transformations before sending advertisement to neighbor
    BgpProtocolHelper.transformBgpRoutePostExport(
        transformedOutgoingRouteBuilder, ourConfig, sessionProperties);

    // Successfully exported route
    Bgpv4Route transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
    _prefixTracer.sentTo(
        transformedOutgoingRoute.getNetwork(),
        remoteConfigId.getHostname(),
        remoteIp,
        remoteConfigId.getVrfName(),
        ourConfig.getExportPolicy());

    return transformedOutgoingRoute;
  }

  /**
   * Given a {@link BgpRoute}, run it through the BGP outbound transformations and export routing
   * policy.
   *
   * @param exportCandidate a route to try and export
   * @param ourConfig {@link BgpPeerConfig} that sends the route
   * @param remoteConfig {@link BgpPeerConfig} that will be receiving the route
   * @param allNodes all nodes in the network
   * @param sessionProperties {@link BgpSessionProperties} representing the <em>incoming</em> edge:
   *     i.e. the edge from {@code remoteConfig} to {@code ourConfig}
   * @return The transformed route as a {@link BgpRoute}, or {@code null} if the route should not be
   *     exported.
   */
  @Nullable
  private <R extends BgpRoute<B, R>, B extends BgpRoute.Builder<B, R>> R exportBgpRoute(
      @Nonnull AnnotatedRoute<R> exportCandidate,
      @Nonnull BgpPeerConfigId ourConfigId,
      @Nonnull BgpPeerConfigId remoteConfigId,
      @Nonnull BgpPeerConfig ourConfig,
      @Nonnull BgpPeerConfig remoteConfig,
      @Nonnull Map<String, Node> allNodes,
      @Nonnull BgpSessionProperties sessionProperties) {

    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(ourConfig.getExportPolicy());
    B transformedOutgoingRouteBuilder =
        BgpProtocolHelper.transformBgpRoutePreExport(
            ourConfig,
            remoteConfig,
            sessionProperties,
            _vrf.getBgpProcess(),
            requireNonNull(getRemoteBgpNeighborVR(remoteConfigId, allNodes))._vrf.getBgpProcess(),
            exportCandidate.getRoute());
    if (transformedOutgoingRouteBuilder == null) {
      // This route could not be exported for core bgp protocol reasons
      return null;
    }

    // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
    Ip remoteIp = sessionProperties.getTailIp();

    // Process transformed outgoing route by the export policy
    boolean shouldExport =
        exportPolicy.process(
            exportCandidate,
            transformedOutgoingRouteBuilder,
            remoteIp,
            ourConfigId.getRemotePeerPrefix(),
            ourConfigId.getVrfName(),
            Direction.OUT);

    if (!shouldExport) {
      // This route could not be exported due to export policy
      _prefixTracer.filtered(
          exportCandidate.getNetwork(),
          remoteConfigId.getHostname(),
          remoteIp,
          remoteConfigId.getVrfName(),
          ourConfig.getExportPolicy(),
          Direction.OUT);
      return null;
    }

    // Apply final post-policy transformations before sending advertisement to neighbor
    BgpProtocolHelper.transformBgpRoutePostExport(
        transformedOutgoingRouteBuilder, ourConfig, sessionProperties);

    // Successfully exported route
    R transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
    _prefixTracer.sentTo(
        transformedOutgoingRoute.getNetwork(),
        remoteConfigId.getHostname(),
        remoteIp,
        remoteConfigId.getVrfName(),
        ourConfig.getExportPolicy());

    return transformedOutgoingRoute;
  }

  Optional<Rib> getRib(RibId id) {
    if (!_name.equals(id.getVrfName())) {
      return Optional.empty();
    }
    return Optional.ofNullable(_mainRibs.get(id.getRibName()));
  }

  private void enqueueCrossVrfRoutes(
      @Nonnull CrossVrfEdgeId remoteVrfToOurRib,
      @Nonnull Stream<RouteAdvertisement<AnnotatedRoute<AbstractRoute>>> routeAdverts,
      @Nullable String policyName) {
    if (!_crossVrfIncomingRoutes.containsKey(remoteVrfToOurRib)) {
      // We either messed up royally or https://github.com/batfish/batfish/issues/3050
      return;
    }

    Stream<RouteAdvertisement<AnnotatedRoute<AbstractRoute>>> filteredRoutes = routeAdverts;
    if (policyName != null) {
      RoutingPolicy policy = _c.getRoutingPolicies().get(policyName);
      filteredRoutes =
          routeAdverts
              .map(
                  ra -> {
                    AnnotatedRoute<AbstractRoute> annotatedRoute = ra.getRoute();
                    AbstractRouteBuilder<?, ?> routeBuilder = annotatedRoute.getRoute().toBuilder();
                    if (policy.process(annotatedRoute, routeBuilder, null, _name, IN)) {
                      // Preserve original route's source VRF
                      return ra.toBuilder()
                          .setRoute(
                              new AnnotatedRoute<>(
                                  routeBuilder.build(), annotatedRoute.getSourceVrf()))
                          .build();
                    }
                    return null;
                  })
              .filter(Objects::nonNull);
    }

    Queue<RouteAdvertisement<AnnotatedRoute<AbstractRoute>>> queue =
        _crossVrfIncomingRoutes.get(remoteVrfToOurRib);
    filteredRoutes.forEach(queue::add);
  }

  void processCrossVrfRoutes() {
    _crossVrfIncomingRoutes.forEach(
        (edgeId, queue) -> {
          while (queue.peek() != null) {
            RouteAdvertisement<AnnotatedRoute<AbstractRoute>> ra = queue.remove();
            // TODO: handle non-default main RIBs based on RIB specified in edgeID
            // https://github.com/batfish/batfish/issues/3050
            if (ra.isWithdrawn()) {
              _mainRibRouteDeltaBuilder.from(_mainRib.removeRouteGetDelta(ra.getRoute()));
            } else {
              _mainRibRouteDeltaBuilder.from(_mainRib.mergeRouteGetDelta(ra.getRoute()));
            }
          }
        });
  }

  /**
   * Goes through VRFs that can leak routes into this routing instance, and enqueues all their new
   * routes in {@link #_crossVrfIncomingRoutes}.
   */
  void queueCrossVrfImports() {
    if (_vrf.getCrossVrfImportPolicy() == null || _vrf.getCrossVrfImportVrfs() == null) {
      return;
    }
    for (String vrfToImport : _vrf.getCrossVrfImportVrfs()) {
      VirtualRouter exportingVR = _node.getVirtualRouters().get(vrfToImport);
      CrossVrfEdgeId otherVrfToOurRib = new CrossVrfEdgeId(vrfToImport, RibId.DEFAULT_RIB_NAME);
      enqueueCrossVrfRoutes(
          otherVrfToOurRib,
          // TODO Will need to update once support is added for cross-VRF export policies
          exportingVR._mainRibRouteDeltaBuilder.build().getActions(),
          _vrf.getCrossVrfImportPolicy());
    }
  }

  private <R extends AbstractRoute> AnnotatedRoute<R> annotateRoute(@Nonnull R route) {
    return new AnnotatedRoute<>(route, _name);
  }

  @Nullable
  BgpRoutingProcess getBgpRoutingProcess() {
    return _bgpRoutingProcess;
  }

  public Map<String, OspfRoutingProcess> getOspfProcesses() {
    return _ospfProcesses;
  }

  public Set<VniSettings> getVniSettings() {
    return _vniSettings;
  }

  /** Check whether this virtual router has any remaining computation to do */
  boolean isDirty() {
    return
    // Route Deltas
    !_mainRibRouteDeltaBuilder.isEmpty()
        // Message queues
        || !_isisIncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        || !_crossVrfIncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        // Processes
        || _ospfProcesses.values().stream().anyMatch(OspfRoutingProcess::isDirty)
        || (_bgpRoutingProcess != null && _bgpRoutingProcess.isDirty());
  }

  /** Execute one OSPF iteration, for all processes */
  void ospfIteration(Map<String, Node> allNodes) {
    _ospfProcesses.values().forEach(p -> p.executeIteration(allNodes));
  }

  void bgpIteration(Map<String, Node> allNodes) {
    if (_bgpRoutingProcess != null) {
      _bgpRoutingProcess.executeIteration(allNodes);
      updateFloodLists();
    }
  }

  /**
   * Process EVPN type 3 routes in our RIB and update flood lists for any {@link VniSettings} if
   * necessary.
   */
  private void updateFloodLists() {
    for (EvpnType3Route route : _bgpRoutingProcess.getEvpnType3Routes()) {
      _vniSettings =
          _vniSettings.stream()
              .map(vs -> updateVniFloodList(vs, route))
              .collect(ImmutableSet.toImmutableSet());
    }
  }

  @VisibleForTesting
  static VniSettings updateVniFloodList(VniSettings vs, EvpnType3Route route) {
    if (vs.getBumTransportMethod() != BumTransportMethod.UNICAST_FLOOD_GROUP
        || route.getVniIp().equals(vs.getSourceAddress())) {
      // Only update settings if transport method is unicast.
      // Do not add our own source to the flood list.
      return vs;
    }
    return vs.addToFloodList(route.getVniIp());
  }

  void redistribute() {
    // TODO: expand to processes other than OSPF
    _ospfProcesses
        .values()
        .forEach(
            p ->
                p.redistribute(
                    // For the time being use all main RIB routes
                    // TODO: later switch to just the delta (after iteration 1)
                    RibDelta.<AnnotatedRoute<AbstractRoute>>builder()
                        .add(_mainRib.getTypedRoutes())
                        .build()));
  }

  void mergeOspfRoutesToMainRib() {
    _ospfProcesses
        .values()
        .forEach(
            p ->
                _mainRibRouteDeltaBuilder.from(
                    importRibDelta(_mainRib, p.getUpdatesForMainRib(), _name)));
  }

  void mergeBgpRoutes() {
    if (_bgpRoutingProcess == null) {
      return;
    }
    _mainRibRouteDeltaBuilder.from(
        importRibDelta(_mainRib, _bgpRoutingProcess.getUpdatesForMainRib(), _name));
  }

  /** Temporary wrapper for {@link BgpRoutingProcess#enqueueBgpv4Routes(EdgeId, Collection)} */
  private void enqueueBgpMessages(
      @Nonnull EdgeId edgeId, @Nonnull Collection<RouteAdvertisement<Bgpv4Route>> routes) {
    _bgpRoutingProcess.enqueueBgpv4Routes(edgeId, routes);
  }

  /** Temporary wrapper for {@link BgpRoutingProcess#enqueueBgpMessages(EdgeId, Stream)} */
  private void enqueueBgpMessages(
      @Nonnull EdgeId edgeId, @Nonnull Stream<RouteAdvertisement<Bgpv4Route>> routes) {
    _bgpRoutingProcess.enqueueBgpMessages(edgeId, routes);
  }

  public Set<EvpnRoute<?, ?>> getEvpnRoutes() {
    if (_bgpRoutingProcess == null) {
      return ImmutableSet.of();
    }
    return _bgpRoutingProcess._evpnRib.getTypedRoutes();
  }
}
