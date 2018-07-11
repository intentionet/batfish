package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Objects.requireNonNull;
import static org.batfish.common.util.CommonUtil.toImmutableSortedMap;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.dataplane.protocols.IsisProtocolHelper.convertRouteLevel1ToLevel2;
import static org.batfish.dataplane.protocols.StaticRouteHelper.isInterfaceRoute;
import static org.batfish.dataplane.protocols.StaticRouteHelper.shouldActivateNextHopIpRoute;
import static org.batfish.dataplane.rib.AbstractRib.importRib;
import static org.batfish.dataplane.rib.RibDelta.importRibDelta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.Network;
import com.google.common.graph.ValueGraph;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.FibImpl;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfInternalRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.dataplane.exceptions.BgpRoutePropagationException;
import org.batfish.dataplane.protocols.BgpProtocolHelper;
import org.batfish.dataplane.protocols.GeneratedRouteHelper;
import org.batfish.dataplane.protocols.OspfProtocolHelper;
import org.batfish.dataplane.rib.BgpBestPathRib;
import org.batfish.dataplane.rib.BgpMultipathRib;
import org.batfish.dataplane.rib.ConnectedRib;
import org.batfish.dataplane.rib.IsisLevelRib;
import org.batfish.dataplane.rib.IsisRib;
import org.batfish.dataplane.rib.LocalRib;
import org.batfish.dataplane.rib.OspfExternalType1Rib;
import org.batfish.dataplane.rib.OspfExternalType2Rib;
import org.batfish.dataplane.rib.OspfInterAreaRib;
import org.batfish.dataplane.rib.OspfIntraAreaRib;
import org.batfish.dataplane.rib.OspfRib;
import org.batfish.dataplane.rib.Rib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RipInternalRib;
import org.batfish.dataplane.rib.RipRib;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;
import org.batfish.dataplane.rib.StaticRib;
import org.batfish.dataplane.topology.BgpEdgeId;

public class VirtualRouter extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  /** Parent configuration for this Virtual router */
  private final Configuration _c;

  /** Route dependency tracker for BGP aggregate routes */
  private transient RouteDependencyTracker<BgpRoute, AbstractRoute> _bgpAggDeps =
      new RouteDependencyTracker<>();

  /** Best-path BGP RIB */
  BgpBestPathRib _bgpBestPathRib;

  /** Builder for constructing {@link RibDelta} as pertains to the best-path BGP RIB */
  private transient RibDelta.Builder<BgpRoute> _bgpBestPathDeltaBuilder;

  /** Incoming messages into this router from each BGP neighbor */
  transient SortedMap<BgpEdgeId, Queue<RouteAdvertisement<BgpRoute>>> _bgpIncomingRoutes;

  /** BGP multipath RIB */
  BgpMultipathRib _bgpMultipathRib;

  /** Builder for constructing {@link RibDelta} as pertains to the multipath BGP RIB */
  private transient RibDelta.Builder<BgpRoute> _bgpMultiPathDeltaBuilder;

  /** The RIB containing connected routes */
  private transient ConnectedRib _connectedRib;

  /** Helper RIB containing best paths obtained with external BGP */
  transient BgpBestPathRib _ebgpBestPathRib;

  /** Helper RIB containing all paths obtained with external BGP */
  transient BgpMultipathRib _ebgpMultipathRib;

  /**
   * Helper RIB containing paths obtained with external eBGP during current iteration. An Adj-RIB of
   * sorts.
   */
  transient BgpMultipathRib _ebgpStagingRib;

  /** Helper RIB containing best paths obtained with iBGP */
  transient BgpBestPathRib _ibgpBestPathRib;

  /** Helper RIB containing all paths obtained with iBGP */
  transient BgpMultipathRib _ibgpMultipathRib;

  /**
   * Helper RIB containing paths obtained with iBGP during current iteration. An Adj-RIB of sorts.
   */
  transient BgpMultipathRib _ibgpStagingRib;
  /**
   * The independent RIB contains connected and static routes, which are unaffected by BDP
   * iterations (hence, independent).
   */
  transient Rib _independentRib;

  /** Incoming messages into this router from each IS-IS circuit */
  transient SortedMap<IsisEdge, Queue<RouteAdvertisement<IsisRoute>>> _isisIncomingRoutes;

  transient IsisLevelRib _isisL1Rib;

  transient IsisLevelRib _isisL2Rib;

  transient IsisLevelRib _isisL1StagingRib;

  transient IsisLevelRib _isisL2StagingRib;

  transient IsisRib _isisRib;

  transient LocalRib _localRib;

  /** The finalized RIB, a combination different protocol RIBs */
  Rib _mainRib;

  /** Keeps track of changes to the main RIB */
  private transient RibDelta.Builder<AbstractRoute> _mainRibRouteDeltaBuiler;

  transient OspfExternalType1Rib _ospfExternalType1Rib;

  transient OspfExternalType1Rib _ospfExternalType1StagingRib;

  transient OspfExternalType2Rib _ospfExternalType2Rib;

  transient OspfExternalType2Rib _ospfExternalType2StagingRib;

  @VisibleForTesting
  transient SortedMap<Prefix, Queue<RouteAdvertisement<OspfExternalRoute>>>
      _ospfExternalIncomingRoutes;

  transient OspfInterAreaRib _ospfInterAreaRib;

  transient OspfInterAreaRib _ospfInterAreaStagingRib;

  transient OspfIntraAreaRib _ospfIntraAreaRib;

  transient OspfIntraAreaRib _ospfIntraAreaStagingRib;

  transient OspfRib _ospfRib;

  /**
   * Set of all valid BGP routes we have received during the DP computation. Used to fill gaps in
   * BGP RIBs when routes are withdrawn.
   */
  private Map<Prefix, SortedSet<BgpRoute>> _receivedBgpRoutes;

  /** Set of all received BGP advertisements in {@link BgpAdvertisement} form */
  private Set<BgpAdvertisement> _receivedBgpAdvertisements;

  /** Set of all valid IS-IS level-1 routes that we know about */
  private Map<Prefix, SortedSet<IsisRoute>> _receivedIsisL1Routes;

  /** Set of all valid IS-IS level-2 routes that we know about */
  private Map<Prefix, SortedSet<IsisRoute>> _receivedIsisL2Routes;

  /** Set of all valid OSPF external Type 1 routes that we know about */
  private Map<Prefix, SortedSet<OspfExternalType1Route>> _receivedOspExternalType1Routes;

  /** Set of all valid OSPF external Type 2 routes that we know about */
  private Map<Prefix, SortedSet<OspfExternalType2Route>> _receivedOspExternalType2Routes;

  transient RipInternalRib _ripInternalRib;

  transient RipInternalRib _ripInternalStagingRib;

  transient RipRib _ripRib;

  /** Set of all sent BGP advertisements in {@link BgpAdvertisement} form */
  Set<BgpAdvertisement> _sentBgpAdvertisements;

  transient StaticRib _staticInterfaceRib;

  transient StaticRib _staticNextHopRib;

  /** FIB (forwarding information base) built from the main RIB */
  private Fib _fib;

  /** RIB containing generated routes */
  private transient Rib _generatedRib;

  private transient RibDelta.Builder<OspfExternalRoute> _ospfExternalDeltaBuiler;

  private transient Map<Prefix, OspfLink> _ospfNeighbors;

  // TODO: make non-transient. Currently transient because de-serialization crashes.
  /** Metadata about propagated prefixes to/from neighbors */
  private transient PrefixTracer _prefixTracer;

  /** A {@link Vrf} that this virtual router represents */
  final Vrf _vrf;

  VirtualRouter(final String name, final Configuration c) {
    super(name);
    _c = c;
    _vrf = c.getVrfs().get(name);
    initRibs();
    // Keep track of sent and received advertisements
    _receivedBgpAdvertisements = new LinkedHashSet<>();
    _sentBgpAdvertisements = new LinkedHashSet<>();
    _receivedIsisL1Routes = new TreeMap<>();
    _receivedIsisL2Routes = new TreeMap<>();
    _receivedOspExternalType1Routes = new TreeMap<>();
    _receivedOspExternalType2Routes = new TreeMap<>();
    _receivedBgpRoutes = new TreeMap<>();
    _bgpIncomingRoutes = new TreeMap<BgpEdgeId, Queue<RouteAdvertisement<BgpRoute>>>();
    _prefixTracer = new PrefixTracer();
  }

  /**
   * Convert a given RibDelta into {@link RouteAdvertisement} objects and enqueue them onto a given
   * queue.
   *
   * @param queue the message queue
   * @param delta {@link RibDelta} representing changes.
   */
  @VisibleForTesting
  static <R extends AbstractRoute, D extends R> void queueDelta(
      Queue<RouteAdvertisement<R>> queue, @Nullable RibDelta<D> delta) {
    if (delta == null) {
      // Nothing to do
      return;
    }
    for (RouteAdvertisement<D> r : delta.getActions()) {
      // REPLACE does not make sense across routers, update with WITHDRAW
      Reason reason = r.getReason() == Reason.REPLACE ? Reason.WITHDRAW : r.getReason();
      queue.add(new RouteAdvertisement<>(r.getRoute(), r.isWithdrawn(), reason));
    }
  }

  static Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> syncBgpDeltaPropagation(
      BgpBestPathRib bestPathRib, BgpMultipathRib multiPathRib, RibDelta<BgpRoute> delta) {

    // Build our first attempt at best path delta
    Builder<BgpRoute> bestDeltaBuilder = new Builder<>(bestPathRib);
    bestDeltaBuilder.from(importRibDelta(bestPathRib, delta));
    RibDelta<BgpRoute> bestPathDelta = bestDeltaBuilder.build();

    Builder<BgpRoute> mpBuilder = new Builder<>(multiPathRib);

    mpBuilder.from(importRibDelta(multiPathRib, bestPathDelta));
    if (bestPathDelta != null) {
      /*
       * Handle mods to the best path RIB
       */
      for (Prefix p : bestPathDelta.getPrefixes()) {
        List<RouteAdvertisement<BgpRoute>> actions = bestPathDelta.getActions(p);
        if (actions != null) {
          if (actions
              .stream()
              .map(RouteAdvertisement::getReason)
              .anyMatch(Predicate.isEqual(Reason.REPLACE))) {
            /*
             * Clear routes for prefixes where best path RIB was modified, because
             * a better route was chosen, and whatever we had in multipathRib is now invalid
             */
            mpBuilder.from(multiPathRib.clearRoutes(p));
          } else if (actions
              .stream()
              .map(RouteAdvertisement::getReason)
              .anyMatch(Predicate.isEqual(Reason.WITHDRAW))) {
            /*
             * Routes for that prefix were withdrawn. See if we have anything in the multipath RIB
             * to fix it.
             * Create a fake delta, let the routes fight it out for best path in the merge process
             */
            RibDelta<BgpRoute> fakeDelta =
                new Builder<BgpRoute>(null).add(multiPathRib.getRoutes(p)).build();
            bestDeltaBuilder.from(importRibDelta(bestPathRib, fakeDelta));
          }
        }
      }
    }
    // Set the (possibly updated) best path delta
    bestPathDelta = bestDeltaBuilder.build();
    // Update best paths
    multiPathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Only iterate over valid prefixes (ones in best-path RIB) and see if anything should go into
    // multi-path RIB
    for (Prefix p : bestPathRib.getPrefixes()) {
      mpBuilder.from(importRibDelta(multiPathRib, delta, p));
    }
    return new SimpleImmutableEntry<>(bestPathDelta, mpBuilder.build());
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
  void initForIgpComputation() {
    initConnectedRib();
    initLocalRib();
    initStaticRibs();
    importRib(_independentRib, _connectedRib);
    importRib(_independentRib, _localRib);
    importRib(_independentRib, _staticInterfaceRib);
    importRib(_mainRib, _independentRib);
    initIntraAreaOspfRoutes();
    initBaseRipRoutes();
  }

  /**
   * Prep for the Egp part of the computation
   *
   * @param allNodes map of all network nodes, keyed by hostname
   * @param bgpTopology the bgp peering relationships
   */
  void initForEgpComputation(
      final Map<String, Node> allNodes,
      Topology topology,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology,
      Network<IsisNode, IsisEdge> isisTopology) {
    initQueuesAndDeltaBuilders(allNodes, topology, bgpTopology, isisTopology);
  }

  /**
   * Initializes RIB delta builders and protocol message queues.
   *
   * @param allNodes map of all network nodes, keyed by hostname
   * @param topology Layer 3 network topology
   * @param bgpTopology the bgp peering relationships
   */
  @VisibleForTesting
  void initQueuesAndDeltaBuilders(
      final Map<String, Node> allNodes,
      final Topology topology,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology,
      Network<IsisNode, IsisEdge> isisTopology) {

    // Initialize message queues for each BGP neighbor
    initBgpQueues(bgpTopology);

    initIsisQueues(isisTopology);

    // Initialize message queues for each Ospf neighbor
    if (_vrf.getOspfProcess() == null) {
      _ospfExternalIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _ospfNeighbors = getOspfNeighbors(allNodes, topology);
      if (_ospfNeighbors == null) {
        _ospfExternalIncomingRoutes = ImmutableSortedMap.of();
      } else {
        _ospfExternalIncomingRoutes =
            _ospfNeighbors
                .keySet()
                .stream()
                .collect(
                    ImmutableSortedMap.toImmutableSortedMap(
                        Prefix::compareTo,
                        Function.identity(),
                        p -> new ConcurrentLinkedQueue<>()));
      }
    }
  }

  /**
   * Initialize incoming BGP message queues.
   *
   * @param bgpTopology source of truth for which sessions get established.
   */
  void initBgpQueues(ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology) {
    if (_vrf.getBgpProcess() == null) {
      _bgpIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _bgpIncomingRoutes =
          Stream.concat(
                  _vrf.getBgpProcess()
                      .getActiveNeighbors()
                      .entrySet()
                      .stream()
                      .map(
                          e ->
                              new BgpPeerConfigId(
                                  getHostname(), _vrf.getName(), e.getKey(), false)),
                  _vrf.getBgpProcess()
                      .getPassiveNeighbors()
                      .entrySet()
                      .stream()
                      .map(
                          e ->
                              new BgpPeerConfigId(getHostname(), _vrf.getName(), e.getKey(), true)))
              .filter(bgpTopology.nodes()::contains)
              .flatMap(
                  dst ->
                      bgpTopology.adjacentNodes(dst).stream().map(src -> new BgpEdgeId(src, dst)))
              .collect(
                  toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    }
  }

  private void initIsisQueues(Network<IsisNode, IsisEdge> isisTopology) {
    // Initialize message queues for each IS-IS circuit
    if (_vrf.getIsisProcess() == null) {
      _isisIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _isisIncomingRoutes =
          _vrf.getInterfaceNames()
              .stream()
              .map(ifaceName -> new IsisNode(_c.getHostname(), ifaceName))
              .filter(isisTopology.nodes()::contains)
              .flatMap(n -> isisTopology.inEdges(n).stream())
              .collect(
                  toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    }
  }

  /**
   * Activate generated routes.
   *
   * @return a new {@link RibDelta} if a new route has been activated, otherwise {@code null}
   */
  @VisibleForTesting
  RibDelta<AbstractRoute> activateGeneratedRoutes() {
    RibDelta.Builder<AbstractRoute> builder = new Builder<>(_generatedRib);

    /*
     * Loop over all generated routes and check whether any of the contributing routes can trigger
     * activation.
     */
    for (GeneratedRoute gr : _vrf.getGeneratedRoutes()) {
      String policyName = gr.getGenerationPolicy();
      RoutingPolicy generationPolicy =
          policyName != null ? _c.getRoutingPolicies().get(gr.getGenerationPolicy()) : null;
      GeneratedRoute.Builder grb =
          GeneratedRouteHelper.activateGeneratedRoute(
              gr, generationPolicy, _mainRib.getRoutes(), _vrf.getName());

      if (grb != null) {
        GeneratedRoute newGr = grb.build();
        // Routes have been changed
        RibDelta<AbstractRoute> d = _generatedRib.mergeRouteGetDelta(newGr);
        builder.from(d);
      }
    }
    return builder.build();
  }

  /**
   * Recompute generated routes. If new generated routes were activated, process them into the main
   * RIB. Check if any BGP aggregates were affected by the new generated routes.
   */
  void recomputeGeneratedRoutes() {
    RibDelta<AbstractRoute> d;
    RibDelta.Builder<AbstractRoute> generatedRouteDeltaBuilder = new Builder<>(_mainRib);
    do {
      d = activateGeneratedRoutes();
      generatedRouteDeltaBuilder.from(d);
    } while (d != null);

    d = generatedRouteDeltaBuilder.build();
    // Update main rib as well
    _mainRibRouteDeltaBuiler.from(importRibDelta(_mainRib, d));

    /*
     * Check dependencies for BGP aggregates.
     *
     * Updates from these BGP deltas into mainRib will be handled in finalizeBgp routes
     */
    if (d != null) {
      d.getActions()
          .stream()
          .filter(RouteAdvertisement::isWithdrawn)
          .forEach(
              r -> {
                _bgpBestPathDeltaBuilder.from(
                    _bgpAggDeps.deleteRoute(r.getRoute(), _bgpBestPathRib));
                _bgpMultiPathDeltaBuilder.from(
                    _bgpAggDeps.deleteRoute(r.getRoute(), _bgpMultipathRib));
              });
    }
  }

  /**
   * Activate static routes with next hop IP. Adds a static route {@code route} to the main RIB if
   * there exists an active route to the {@code routes}'s next-hop-ip.
   *
   * <p>Removes static route from the main RIB for which next-hop-ip has become unreachable.
   */
  void activateStaticRoutes() {
    for (StaticRoute sr : _staticNextHopRib.getRoutes()) {
      if (shouldActivateNextHopIpRoute(sr, _mainRib)) {
        _mainRibRouteDeltaBuiler.from(_mainRib.mergeRouteGetDelta(sr));
      } else {
        /*
         * If the route is not in the RIB, this has no effect. But might add some overhead (TODO)
         */
        _mainRibRouteDeltaBuiler.from(_mainRib.removeRouteGetDelta(sr, Reason.WITHDRAW));
      }
    }
  }

  /** Compute the FIB from the main RIB */
  public void computeFib() {
    _fib = new FibImpl(_mainRib);
  }

  boolean computeInterAreaSummaries() {
    OspfProcess proc = _vrf.getOspfProcess();
    boolean changed = false;
    // Ensure we have a running OSPF process on the VRF, otherwise bail.
    if (proc == null) {
      return false;
    }
    // Admin cost for the given protocol
    int admin = RoutingProtocol.OSPF_IA.getSummaryAdministrativeCost(_c.getConfigurationFormat());

    // Determine whether to use min metric by default, based on RFC1583 compatibility setting.
    // Routers (at least Cisco and Juniper) default to min metric unless using RFC2328 with
    // RFC1583 compatibility explicitly disabled, in which case they default to max.
    boolean useMin = firstNonNull(proc.getRfc1583Compatible(), Boolean.TRUE);

    // Compute summaries for each area
    for (Entry<Long, OspfArea> e : proc.getAreas().entrySet()) {
      long areaNum = e.getKey();
      OspfArea area = e.getValue();
      for (Entry<Prefix, OspfAreaSummary> e2 : area.getSummaries().entrySet()) {
        Prefix prefix = e2.getKey();
        OspfAreaSummary summary = e2.getValue();

        // Only advertised summaries can contribute
        if (!summary.getAdvertised()) {
          continue;
        }

        Long metric = summary.getMetric();
        if (summary.getMetric() == null) {
          // No metric was configured; compute it from any possible contributing routes.
          for (OspfIntraAreaRoute contributingRoute : _ospfIntraAreaRib.getRoutes()) {
            metric =
                OspfProtocolHelper.computeUpdatedOspfSummaryMetric(
                    contributingRoute, prefix, metric, areaNum, useMin);
          }
          for (OspfInterAreaRoute contributingRoute : _ospfInterAreaRib.getRoutes()) {
            metric =
                OspfProtocolHelper.computeUpdatedOspfSummaryMetric(
                    contributingRoute, prefix, metric, areaNum, useMin);
          }
        }

        // No routes contributed to the summary, nothing to construct
        if (metric == null) {
          continue;
        }

        // Non-null metric means we generate a new summary and put it in the RIB
        OspfInterAreaRoute summaryRoute =
            new OspfInterAreaRoute(prefix, Ip.ZERO, admin, metric, areaNum);
        if (_ospfInterAreaStagingRib.mergeRouteGetDelta(summaryRoute) != null) {
          changed = true;
        }
      }
    }
    return changed;
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
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology,
      NetworkConfigurations networkConfigurations) {

    BgpProcess proc = _vrf.getBgpProcess();
    if (proc == null) {
      // Nothing to do
      return;
    }

    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    Map<BgpMultipathRib, RibDelta.Builder<BgpRoute>> ribDeltas = new IdentityHashMap<>();
    ribDeltas.put(_ebgpStagingRib, new Builder<>(_ebgpStagingRib));
    ribDeltas.put(_ibgpStagingRib, new Builder<>(_ibgpStagingRib));

    // initialize admin costs for routes
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());

    BgpRoute.Builder outgoingRouteBuilder = new BgpRoute.Builder();
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
      // TODO: support passive bgp connections
      Prefix srcPrefix = new Prefix(srcIp, Prefix.MAX_PREFIX_LENGTH);
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

      BgpMultipathRib targetRib = ebgp ? _ebgpStagingRib : _ibgpStagingRib;
      RoutingProtocol targetProtocol = ebgp ? RoutingProtocol.BGP : RoutingProtocol.IBGP;

      if (received) {
        int admin = ebgp ? ebgpAdmin : ibgpAdmin;
        AsPath asPath = advert.getAsPath();
        SortedSet<Long> clusterList = advert.getClusterList();
        SortedSet<Long> communities = ImmutableSortedSet.copyOf(advert.getCommunities());
        int localPreference = advert.getLocalPreference();
        long metric = advert.getMed();
        Prefix network = advert.getNetwork();
        Ip nextHopIp = advert.getNextHopIp();
        Ip originatorIp = advert.getOriginatorIp();
        OriginType originType = advert.getOriginType();
        RoutingProtocol srcProtocol = advert.getSrcProtocol();
        int weight = advert.getWeight();
        BgpRoute.Builder builder = new BgpRoute.Builder();
        builder.setAdmin(admin);
        builder.setAsPath(asPath.getAsSets());
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
        BgpRoute route = builder.build();
        ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(route));
      } else {
        int localPreference;
        if (ebgp) {
          localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
        } else {
          localPreference = advert.getLocalPreference();
        }
        outgoingRouteBuilder.setAsPath(advert.getAsPath().getAsSets());
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
        BgpRoute transformedOutgoingRoute = outgoingRouteBuilder.build();
        BgpRoute.Builder transformedIncomingRouteBuilder = new BgpRoute.Builder();

        // Incoming originatorIp
        transformedIncomingRouteBuilder.setOriginatorIp(transformedOutgoingRoute.getOriginatorIp());

        // Incoming receivedFromIp
        transformedIncomingRouteBuilder.setReceivedFromIp(
            transformedOutgoingRoute.getReceivedFromIp());

        // Incoming clusterList
        transformedIncomingRouteBuilder
            .getClusterList()
            .addAll(transformedOutgoingRoute.getClusterList());

        // Incoming receivedFromRouteReflectorClient
        transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
            transformedOutgoingRoute.getReceivedFromRouteReflectorClient());

        // Incoming asPath
        transformedIncomingRouteBuilder.setAsPath(transformedOutgoingRoute.getAsPath().getAsSets());

        // Incoming communities
        transformedIncomingRouteBuilder
            .getCommunities()
            .addAll(transformedOutgoingRoute.getCommunities());

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
        int admin = ebgp ? ebgpAdmin : ibgpAdmin;
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
            acceptIncoming =
                importPolicy.process(
                    transformedOutgoingRoute,
                    transformedIncomingRouteBuilder,
                    advert.getSrcIp(),
                    _key,
                    Direction.IN);
          }
        }
        if (acceptIncoming) {
          BgpRoute transformedIncomingRoute = transformedIncomingRouteBuilder.build();
          ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
        }
      }
    }

    // Propagate received routes through all the RIBs and send out appropriate messages
    // to neighbors
    Map<BgpMultipathRib, RibDelta<BgpRoute>> deltas =
        ribDeltas
            .entrySet()
            .stream()
            .filter(e -> e.getValue().build() != null)
            .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().build()));
    finalizeBgpRoutesAndQueueOutgoingMessages(
        proc.getMultipathEbgp(),
        proc.getMultipathIbgp(),
        deltas,
        allNodes,
        bgpTopology,
        networkConfigurations);
  }

  /** Initialize Intra-area OSPF routes from the interface prefixes */
  private void initIntraAreaOspfRoutes() {
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return; // nothing to do
    }
    /*
     * init intra-area routes from connected routes
     * For each interface within an OSPF area and each interface prefix,
     * construct a new OSPF-IA route. Put it in the IA RIB.
     */
    proc.getAreas()
        .forEach(
            (areaNum, area) -> {
              for (String ifaceName : area.getInterfaces()) {
                Interface iface = _c.getInterfaces().get(ifaceName);
                if (iface.getActive()) {
                  Set<Prefix> allNetworkPrefixes =
                      iface
                          .getAllAddresses()
                          .stream()
                          .map(InterfaceAddress::getPrefix)
                          .collect(Collectors.toSet());
                  int interfaceOspfCost = iface.getOspfCost();
                  for (Prefix prefix : allNetworkPrefixes) {
                    long cost = interfaceOspfCost;
                    boolean stubNetwork = iface.getOspfPassive() || iface.getOspfPointToPoint();
                    if (stubNetwork) {
                      if (proc.getMaxMetricStubNetworks() != null) {
                        cost = proc.getMaxMetricStubNetworks();
                      }
                    } else if (proc.getMaxMetricTransitLinks() != null) {
                      cost = proc.getMaxMetricTransitLinks();
                    }
                    OspfIntraAreaRoute route =
                        new OspfIntraAreaRoute(
                            prefix,
                            null,
                            RoutingProtocol.OSPF.getDefaultAdministrativeCost(
                                _c.getConfigurationFormat()),
                            cost,
                            areaNum);
                    _ospfIntraAreaRib.mergeRouteGetDelta(route);
                  }
                }
              }
            });
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
            iface
                .getAllAddresses()
                .stream()
                .map(InterfaceAddress::getPrefix)
                .collect(Collectors.toSet());
        long cost = RipProcess.DEFAULT_RIP_COST;
        for (Prefix prefix : allNetworkPrefixes) {
          RipInternalRoute route =
              new RipInternalRoute(
                  prefix,
                  null,
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
   * accepting advertisements less desirable than the local generated ones for a given network.
   */
  void initBgpAggregateRoutes() {
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

      BgpRoute br =
          BgpProtocolHelper.convertGeneratedRouteToBgp(gr, _vrf.getBgpProcess().getRouterId());
      // Prevent route from being merged into the main RIB.
      br.setNonRouting(true);
      /* TODO: tests for this */
      RibDelta<BgpRoute> d1 = _bgpMultipathRib.mergeRouteGetDelta(br);
      _bgpBestPathDeltaBuilder.from(d1);
      RibDelta<BgpRoute> d2 = _bgpBestPathRib.mergeRouteGetDelta(br);
      _bgpMultiPathDeltaBuilder.from(d2);
      if (d1 != null || d2 != null) {
        _bgpAggDeps.addRouteDependency(br, gr);
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
        for (InterfaceAddress ifaceAddress : i.getAllAddresses()) {
          Prefix prefix = ifaceAddress.getPrefix();
          ConnectedRoute cr = new ConnectedRoute(prefix, i.getName());
          _connectedRib.mergeRoute(cr);
        }
      }
    }
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
        for (InterfaceAddress ifaceAddress : i.getAllAddresses()) {
          if (ifaceAddress.getNetworkBits() < Prefix.MAX_PREFIX_LENGTH) {
            LocalRoute lr = new LocalRoute(ifaceAddress, i.getName());
            _localRib.mergeRoute(lr);
          }
        }
      }
    }
  }

  @Nullable
  @VisibleForTesting
  OspfExternalRoute computeOspfExportRoute(
      AbstractRoute potentialExportRoute, RoutingPolicy exportPolicy, OspfProcess proc) {
    OspfExternalRoute.Builder outputRouteBuilder = new OspfExternalRoute.Builder();
    // Export based on the policy result of processing the potentialExportRoute
    boolean accept =
        exportPolicy.process(potentialExportRoute, outputRouteBuilder, null, _key, Direction.OUT);
    if (!accept) {
      return null;
    }
    OspfMetricType metricType = outputRouteBuilder.getOspfMetricType();
    outputRouteBuilder.setAdmin(
        outputRouteBuilder
            .getOspfMetricType()
            .toRoutingProtocol()
            .getDefaultAdministrativeCost(_c.getConfigurationFormat()));
    outputRouteBuilder.setNetwork(potentialExportRoute.getNetwork());
    Long maxMetricExternalNetworks = proc.getMaxMetricExternalNetworks();
    long costToAdvertiser;
    if (maxMetricExternalNetworks != null) {
      if (metricType == OspfMetricType.E1) {
        outputRouteBuilder.setMetric(maxMetricExternalNetworks);
      }
      costToAdvertiser = maxMetricExternalNetworks;
    } else {
      costToAdvertiser = 0L;
    }
    outputRouteBuilder.setCostToAdvertiser(costToAdvertiser);
    outputRouteBuilder.setAdvertiser(_c.getHostname());
    outputRouteBuilder.setArea(OspfRoute.NO_AREA);
    outputRouteBuilder.setLsaMetric(outputRouteBuilder.getMetric());
    OspfExternalRoute outputRoute = outputRouteBuilder.build();
    outputRoute.setNonRouting(true);
    return outputRoute;
  }

  void initIsisExports(Map<String, Node> allNodes, NetworkConfigurations nc) {
    /* TODO: https://github.com/batfish/batfish/issues/1703 */
    IsisProcess proc = _vrf.getIsisProcess();
    if (proc == null) {
      return; // nothing to do
    }
    RibDelta.Builder<IsisRoute> d1 = new Builder<>(_isisL1Rib);
    RibDelta.Builder<IsisRoute> d2 = new Builder<>(_isisL2Rib);
    /*
     * init L1 and L2 routes from connected routes
     */
    int l1Admin = RoutingProtocol.ISIS_L1.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int l2Admin = RoutingProtocol.ISIS_L2.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    IsisLevelSettings l1Settings = proc.getLevel1();
    IsisLevelSettings l2Settings = proc.getLevel2();
    IsisRoute.Builder builder =
        new IsisRoute.Builder()
            .setArea(proc.getNetAddress().getAreaIdString())
            .setSystemId(proc.getNetAddress().getSystemIdString());
    _vrf.getInterfaces()
        .values()
        .forEach(
            iface ->
                generateAllIsisInterfaceRoutes(
                    d1, d2, l1Admin, l2Admin, l1Settings, l2Settings, builder, iface));

    // export default route for L1 neighbors on L1L2 routers
    if (l1Settings != null && l2Settings != null) {
      IsisRoute defaultRoute =
          builder
              .setAdmin(l1Admin)
              .setAttach(true)
              .setLevel(IsisLevel.LEVEL_1)
              .setMetric(0L)
              .setNetwork(Prefix.ZERO)
              .setProtocol(RoutingProtocol.ISIS_L1)
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
      long metric =
          ifaceL1Settings.getMode() == IsisInterfaceMode.PASSIVE
              ? 0L
              : firstNonNull(ifaceL1Settings.getCost(), IsisRoute.DEFAULT_METRIC);
      generateIsisInterfaceRoutesPerLevel(
              l1Admin, routeBuilder, iface, metric, IsisLevel.LEVEL_1, RoutingProtocol.ISIS_L1)
          .forEach(r -> d1.from(_isisL1Rib.mergeRouteGetDelta(r)));
    }
    if (ifaceL2Settings != null && l2Settings != null) {
      long metric =
          ifaceL2Settings.getMode() == IsisInterfaceMode.PASSIVE
              ? 0L
              : firstNonNull(ifaceL2Settings.getCost(), IsisRoute.DEFAULT_METRIC);
      generateIsisInterfaceRoutesPerLevel(
              l2Admin, routeBuilder, iface, metric, IsisLevel.LEVEL_2, RoutingProtocol.ISIS_L2)
          .forEach(r -> d2.from(_isisL2Rib.mergeRouteGetDelta(r)));
    }
  }

  /**
   * Generate IS-IS from a given interface for a given level (with a given metric/admin cost) and
   * merge them into the appropriate RIB.
   */
  private static Set<IsisRoute> generateIsisInterfaceRoutesPerLevel(
      int adminCost,
      IsisRoute.Builder routeBuilder,
      Interface iface,
      long metric,
      IsisLevel level,
      RoutingProtocol isisProtocol) {
    routeBuilder.setAdmin(adminCost).setLevel(level).setMetric(metric).setProtocol(isisProtocol);
    return iface
        .getAllAddresses()
        .stream()
        .map(
            address ->
                routeBuilder.setNetwork(address.getPrefix()).setNextHopIp(address.getIp()).build())
        .collect(ImmutableSet.toImmutableSet());
  }

  void initOspfExports() {
    OspfProcess proc = _vrf.getOspfProcess();
    // Nothing to do
    if (proc == null) {
      return;
    }

    // get OSPF export policy name
    String exportPolicyName = _vrf.getOspfProcess().getExportPolicy();
    if (exportPolicyName == null) {
      return; // nothing to export
    }

    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(exportPolicyName);
    if (exportPolicy == null) {
      return; // nothing to export
    }

    // For each route in the previous RIB, compute an export route and add it to the appropriate
    // RIB.
    RibDelta.Builder<OspfExternalType1Route> d1 = new Builder<>(_ospfExternalType1Rib);
    RibDelta.Builder<OspfExternalType2Route> d2 = new Builder<>(_ospfExternalType2Rib);
    for (AbstractRoute potentialExport : _mainRib.getRoutes()) {
      OspfExternalRoute outputRoute = computeOspfExportRoute(potentialExport, exportPolicy, proc);
      if (outputRoute == null) {
        continue; // no need to export
      }
      if (outputRoute.getOspfMetricType() == OspfMetricType.E1) {
        d1.from(_ospfExternalType1Rib.mergeRouteGetDelta((OspfExternalType1Route) outputRoute));
      } else { // assuming here that MetricType exists. Or E2 is the default
        d2.from(_ospfExternalType2Rib.mergeRouteGetDelta((OspfExternalType2Route) outputRoute));
      }
    }
    queueOutgoingOspfExternalRoutes(d1.build(), d2.build());
  }

  /** Initialize all ribs on this router. All RIBs will be empty */
  @VisibleForTesting
  final void initRibs() {
    _connectedRib = new ConnectedRib();
    _localRib = new LocalRib();
    // If bgp process is null, doesn't matter
    MultipathEquivalentAsPathMatchMode mpTieBreaker =
        _vrf.getBgpProcess() == null
            ? EXACT_PATH
            : _vrf.getBgpProcess().getMultipathEquivalentAsPathMatchMode();
    _ebgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    _ebgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _generatedRib = new Rib();
    _ibgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    _ibgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _independentRib = new Rib();
    _isisRib = new IsisRib(isL1Only());
    _isisL1Rib = new IsisLevelRib(_receivedIsisL1Routes);
    _isisL2Rib = new IsisLevelRib(_receivedIsisL2Routes);
    _isisL1StagingRib = new IsisLevelRib(null);
    _isisL2StagingRib = new IsisLevelRib(null);
    _mainRib = new Rib();
    _ospfExternalType1Rib =
        new OspfExternalType1Rib(getHostname(), _receivedOspExternalType1Routes);
    _ospfExternalType2Rib =
        new OspfExternalType2Rib(getHostname(), _receivedOspExternalType2Routes);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(getHostname(), null);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(getHostname(), null);
    _ospfInterAreaRib = new OspfInterAreaRib();
    _ospfInterAreaStagingRib = new OspfInterAreaRib();
    _ospfIntraAreaRib = new OspfIntraAreaRib(this);
    _ospfIntraAreaStagingRib = new OspfIntraAreaRib(this);
    _ospfRib = new OspfRib();
    _ripInternalRib = new RipInternalRib();
    _ripInternalStagingRib = new RipInternalRib();
    _ripRib = new RipRib();
    _staticNextHopRib = new StaticRib();
    _staticInterfaceRib = new StaticRib();
    _bgpMultipathRib = new BgpMultipathRib(mpTieBreaker);

    _ebgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    _ibgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    BgpTieBreaker tieBreaker =
        _vrf.getBgpProcess() == null
            ? BgpTieBreaker.ARRIVAL_ORDER
            : _vrf.getBgpProcess().getTieBreaker();
    _ebgpBestPathRib = new BgpBestPathRib(tieBreaker, null, _mainRib);
    _ibgpBestPathRib = new BgpBestPathRib(tieBreaker, null, _mainRib);
    _bgpBestPathRib = new BgpBestPathRib(tieBreaker, _receivedBgpRoutes, _mainRib);

    _mainRibRouteDeltaBuiler = new RibDelta.Builder<>(_mainRib);
    _bgpBestPathDeltaBuilder = new RibDelta.Builder<>(_bgpBestPathRib);
    _bgpMultiPathDeltaBuilder = new RibDelta.Builder<>(_bgpMultipathRib);
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
        Interface nextHopInterface = _c.getInterfaces().get(sr.getNextHopInterface());

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
   * Compute a set of BGP advertisements to the outside of the network. Done after the dataplane
   * computation has converged.
   *
   * @param ipOwners mapping of IPs to their owners (nodes)
   * @return a number of sent out advertisements
   */
  int computeBgpAdvertisementsToOutside(Map<Ip, Set<String>> ipOwners) {
    int numAdvertisements = 0;

    // If we have no BGP process, nothing to do
    if (_vrf.getBgpProcess() == null) {
      return numAdvertisements;
    }

    /*
     * This operation only really makes sense for active neighbors, otherwise we're missing required
     * information for which advertisements would be sent out.
     */
    for (BgpActivePeerConfig neighbor : _vrf.getBgpProcess().getActiveNeighbors().values()) {
      String hostname = _c.getHostname();

      Ip remoteIp = neighbor.getPeerAddress();
      if (neighbor.getLocalIp() == null
          || remoteIp == null
          || neighbor.getLocalAs() == null
          || neighbor.getRemoteAs() == null
          || ipOwners.get(remoteIp) != null) {
        // Skip if neighbor is mis-configured or remote peer is inside the network
        continue;
      }

      long localAs = neighbor.getLocalAs();
      long remoteAs = neighbor.getRemoteAs();
      String remoteHostname = remoteIp.toString();
      String remoteVrfName = _vrf.getName();
      RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(neighbor.getExportPolicy());
      boolean ebgpSession = localAs != remoteAs;
      RoutingProtocol targetProtocol = ebgpSession ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
      Set<AbstractRoute> candidateRoutes = Collections.newSetFromMap(new IdentityHashMap<>());

      // Add IGP routes
      Set<AbstractRoute> activeRoutes = Collections.newSetFromMap(new IdentityHashMap<>());
      activeRoutes.addAll(_mainRib.getRoutes());
      for (AbstractRoute candidateRoute : activeRoutes) {
        if (candidateRoute.getProtocol() != RoutingProtocol.BGP
            && candidateRoute.getProtocol() != RoutingProtocol.IBGP) {
          candidateRoutes.add(candidateRoute);
        }
      }

      /*
       * bgp advertise-external
       *
       * When this is set, add best eBGP path independently of whether
       * it is preempted by an iBGP or IGP route. Only applicable to
       * iBGP sessions.
       */
      boolean advertiseExternal = !ebgpSession && neighbor.getAdvertiseExternal();
      if (advertiseExternal) {
        candidateRoutes.addAll(_ebgpBestPathRib.getRoutes());
      }

      /*
       * bgp advertise-inactive
       *
       * When this is set, add best BGP path independently of whether
       * it is preempted by an IGP route. Only applicable to eBGP
       * sessions.
       */
      boolean advertiseInactive = ebgpSession && neighbor.getAdvertiseInactive();
      /* Add best bgp paths if they are active, or if advertise-inactive */
      for (AbstractRoute candidateRoute : _bgpBestPathRib.getRoutes()) {
        if (advertiseInactive || activeRoutes.contains(candidateRoute)) {
          candidateRoutes.add(candidateRoute);
        }
      }

      /* Add all bgp paths if additional-paths active for this session */
      boolean additionalPaths =
          !ebgpSession
              && neighbor.getAdditionalPathsSend()
              && neighbor.getAdditionalPathsSelectAll();
      if (additionalPaths) {
        candidateRoutes.addAll(_bgpMultipathRib.getRoutes());
      }

      for (AbstractRoute route : candidateRoutes) {
        // TODO: update this using BgpProtocolHelper

        BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder();
        RoutingProtocol routeProtocol = route.getProtocol();
        boolean routeIsBgp =
            routeProtocol == RoutingProtocol.IBGP || routeProtocol == RoutingProtocol.BGP;

        // originatorIP
        Ip originatorIp;
        if (!ebgpSession && routeProtocol == RoutingProtocol.IBGP) {
          BgpRoute bgpRoute = (BgpRoute) route;
          originatorIp = bgpRoute.getOriginatorIp();
        } else {
          originatorIp = _vrf.getBgpProcess().getRouterId();
        }
        transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);
        transformedOutgoingRouteBuilder.setReceivedFromIp(neighbor.getLocalIp());

        /*
         * clusterList, receivedFromRouteReflectorClient, (originType for bgp remote route)
         */
        if (routeIsBgp) {
          BgpRoute bgpRoute = (BgpRoute) route;
          transformedOutgoingRouteBuilder.setOriginType(bgpRoute.getOriginType());
          if (ebgpSession
              && bgpRoute.getAsPath().containsAs(neighbor.getRemoteAs())
              && !neighbor.getAllowRemoteAsOut()) {
            // skip routes containing peer's AS unless
            // disable-peer-as-check (getAllowRemoteAsOut) is set
            continue;
          }
          /*
           * route reflection: reflect everything received from
           * clients to clients and non-clients. reflect everything
           * received from non-clients to clients. Do not reflect to
           * originator
           */

          Ip routeOriginatorIp = bgpRoute.getOriginatorIp();
          /*
           *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
           *  same as originator id of advertisement
           */
          if (!ebgpSession && remoteIp.equals(routeOriginatorIp)) {
            continue;
          }
          if (routeProtocol == RoutingProtocol.IBGP && !ebgpSession) {
            boolean routeReceivedFromRouteReflectorClient =
                bgpRoute.getReceivedFromRouteReflectorClient();
            boolean sendingToRouteReflectorClient = neighbor.getRouteReflectorClient();
            transformedOutgoingRouteBuilder.getClusterList().addAll(bgpRoute.getClusterList());
            if (!routeReceivedFromRouteReflectorClient && !sendingToRouteReflectorClient) {
              continue;
            }
            if (sendingToRouteReflectorClient) {
              // sender adds its local cluster id to clusterlist of
              // new route
              transformedOutgoingRouteBuilder.getClusterList().add(neighbor.getClusterId());
            }
          }
        }

        // Outgoing asPath
        // Outgoing communities
        if (routeIsBgp) {
          BgpRoute bgpRoute = (BgpRoute) route;
          transformedOutgoingRouteBuilder.setAsPath(bgpRoute.getAsPath().getAsSets());
          if (neighbor.getSendCommunity()) {
            transformedOutgoingRouteBuilder.getCommunities().addAll(bgpRoute.getCommunities());
          }
        }
        if (ebgpSession) {
          SortedSet<Long> newAsPathElement = new TreeSet<>();
          newAsPathElement.add(localAs);
          transformedOutgoingRouteBuilder.getAsPath().add(0, newAsPathElement);
        }

        // Outgoing protocol
        transformedOutgoingRouteBuilder.setProtocol(targetProtocol);
        transformedOutgoingRouteBuilder.setNetwork(route.getNetwork());

        // Outgoing metric
        if (routeIsBgp) {
          transformedOutgoingRouteBuilder.setMetric(route.getMetric());
        }

        // Outgoing nextHopIp
        // Outgoing localPreference
        Ip nextHopIp;
        int localPreference;
        if (ebgpSession || !routeIsBgp) {
          nextHopIp = neighbor.getLocalIp();
          localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
        } else {
          nextHopIp = route.getNextHopIp();
          BgpRoute ibgpRoute = (BgpRoute) route;
          localPreference = ibgpRoute.getLocalPreference();
        }
        if (Route.UNSET_ROUTE_NEXT_HOP_IP.equals(nextHopIp)) {
          // should only happen for ibgp
          String nextHopInterface = route.getNextHopInterface();
          InterfaceAddress nextHopAddress = _c.getInterfaces().get(nextHopInterface).getAddress();
          if (nextHopAddress == null) {
            throw new BatfishException("route's nextHopInterface has no address");
          }
          nextHopIp = nextHopAddress.getIp();
        }
        transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
        transformedOutgoingRouteBuilder.setLocalPreference(localPreference);

        // Outgoing srcProtocol
        transformedOutgoingRouteBuilder.setSrcProtocol(route.getProtocol());

        /*
         * CREATE OUTGOING ROUTE
         */
        boolean acceptOutgoing =
            exportPolicy.process(
                route,
                transformedOutgoingRouteBuilder,
                remoteIp,
                new Prefix(neighbor.getPeerAddress(), Prefix.MAX_PREFIX_LENGTH),
                remoteVrfName,
                Direction.OUT);
        if (!acceptOutgoing) {
          _prefixTracer.filtered(
              route.getNetwork(),
              remoteHostname,
              remoteIp,
              remoteVrfName,
              neighbor.getExportPolicy(),
              Direction.OUT);
          continue;
        }
        _prefixTracer.sentTo(
            route.getNetwork(),
            remoteHostname,
            remoteIp,
            remoteVrfName,
            neighbor.getExportPolicy());
        BgpRoute transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
        // Record sent advertisement
        BgpAdvertisementType sentType =
            ebgpSession ? BgpAdvertisementType.EBGP_SENT : BgpAdvertisementType.IBGP_SENT;
        Ip sentOriginatorIp = transformedOutgoingRoute.getOriginatorIp();
        SortedSet<Long> sentClusterList =
            ImmutableSortedSet.copyOf(transformedOutgoingRoute.getClusterList());
        AsPath sentAsPath = transformedOutgoingRoute.getAsPath();
        SortedSet<Long> sentCommunities =
            ImmutableSortedSet.copyOf(transformedOutgoingRoute.getCommunities());
        Prefix sentNetwork = route.getNetwork();
        Ip sentNextHopIp;
        String sentSrcNode = hostname;
        String sentSrcVrf = _vrf.getName();
        Ip sentSrcIp = neighbor.getLocalIp();
        String sentDstNode = remoteHostname;
        String sentDstVrf = remoteVrfName;
        Ip sentDstIp = remoteIp;
        int sentWeight = -1;
        if (ebgpSession) {
          sentNextHopIp = nextHopIp;
        } else {
          sentNextHopIp = transformedOutgoingRoute.getNextHopIp();
        }
        int sentLocalPreference = transformedOutgoingRoute.getLocalPreference();
        long sentMed = transformedOutgoingRoute.getMetric();
        OriginType sentOriginType = transformedOutgoingRoute.getOriginType();
        RoutingProtocol sentSrcProtocol = targetProtocol;
        BgpAdvertisement sentAdvert =
            new BgpAdvertisement(
                sentType,
                sentNetwork,
                sentNextHopIp,
                sentSrcNode,
                sentSrcVrf,
                sentSrcIp,
                sentDstNode,
                sentDstVrf,
                sentDstIp,
                sentSrcProtocol,
                sentOriginType,
                sentLocalPreference,
                sentMed,
                sentOriginatorIp,
                sentAsPath,
                ImmutableSortedSet.copyOf(sentCommunities),
                ImmutableSortedSet.copyOf(sentClusterList),
                sentWeight);
        _sentBgpAdvertisements.add(sentAdvert);
        numAdvertisements++;
      }
    }
    return numAdvertisements;
  }

  /**
   * Process BGP messages from neighbors, return a list of delta changes to the RIBs
   *
   * @param bgpTopology the bgp peering relationships
   * @return List of {@link RibDelta objects}
   */
  @Nullable
  Map<BgpMultipathRib, RibDelta<BgpRoute>> processBgpMessages(
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology, NetworkConfigurations nc) {

    // If we have no BGP process, nothing to do
    if (_vrf.getBgpProcess() == null) {
      return null;
    }

    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    Map<BgpMultipathRib, RibDelta.Builder<BgpRoute>> ribDeltas = new IdentityHashMap<>();
    ribDeltas.put(_ebgpStagingRib, new Builder<>(_ebgpStagingRib));
    ribDeltas.put(_ibgpStagingRib, new Builder<>(_ibgpStagingRib));

    // Process updates from each neighbor
    for (Entry<BgpEdgeId, Queue<RouteAdvertisement<BgpRoute>>> e : _bgpIncomingRoutes.entrySet()) {

      // Grab the queue containing all messages from remoteBgpPeerConfig
      Queue<RouteAdvertisement<BgpRoute>> queue = e.getValue();

      // Setup helper vars
      BgpPeerConfigId remoteConfigId = e.getKey().src();
      BgpPeerConfigId ourConfigId = e.getKey().dst();
      BgpSessionProperties sessionProperties =
          getBgpSessionProperties(bgpTopology, new BgpEdgeId(remoteConfigId, ourConfigId));
      BgpPeerConfig ourBgpConfig = requireNonNull(nc.getBgpPeerConfig(e.getKey().dst()));
      BgpPeerConfig remoteBgpConfig = requireNonNull(nc.getBgpPeerConfig(e.getKey().src()));

      BgpMultipathRib targetRib = sessionProperties.isEbgp() ? _ebgpStagingRib : _ibgpStagingRib;

      // Process all routes from neighbor
      while (queue.peek() != null) {
        RouteAdvertisement<BgpRoute> remoteRouteAdvert = queue.remove();
        BgpRoute remoteRoute = remoteRouteAdvert.getRoute();

        BgpRoute.Builder transformedIncomingRouteBuilder =
            BgpProtocolHelper.transformBgpRouteOnImport(
                ourBgpConfig, sessionProperties, remoteRoute, _c.getConfigurationFormat());
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
                    remoteBgpConfig.getLocalIp(),
                    ourConfigId.getRemotePeerPrefix(),
                    _key,
                    Direction.IN);
          }
        }
        if (!acceptIncoming) {
          // Route could not be imported due to routing policy
          _prefixTracer.filtered(
              remoteRoute.getNetwork(),
              ourConfigId.getHostname(),
              remoteBgpConfig.getLocalIp(),
              remoteConfigId.getVrfName(),
              importPolicyName,
              Direction.IN);
          continue;
        }
        BgpRoute transformedIncomingRoute = transformedIncomingRouteBuilder.build();

        if (remoteRouteAdvert.isWithdrawn()) {
          // Note this route was removed
          ribDeltas.get(targetRib).remove(transformedIncomingRoute, Reason.WITHDRAW);
          SortedSet<BgpRoute> b = _receivedBgpRoutes.get(transformedIncomingRoute.getNetwork());
          if (b != null) {
            b.remove(transformedIncomingRoute);
          }
        } else {
          // Merge into staging rib, note delta
          ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
          if (!remoteRouteAdvert.isWithdrawn()) {
            markReceivedBgpAdvertisement(
                ourConfigId,
                remoteConfigId,
                ourBgpConfig,
                remoteBgpConfig,
                sessionProperties,
                transformedIncomingRoute);
            _receivedBgpRoutes
                .computeIfAbsent(transformedIncomingRoute.getNetwork(), k -> new TreeSet<>())
                .add(transformedIncomingRoute);
            _prefixTracer.installed(
                transformedIncomingRoute.getNetwork(),
                remoteConfigId.getHostname(),
                remoteBgpConfig.getLocalIp(),
                remoteConfigId.getVrfName(),
                importPolicyName);
          }
        }
      }
    }
    // Return built deltas from RibDelta builders
    Map<BgpMultipathRib, RibDelta<BgpRoute>> builtDeltas = new IdentityHashMap<>();
    ribDeltas.forEach(
        (rib, deltaBuilder) -> {
          RibDelta<BgpRoute> delta = deltaBuilder.build();
          if (delta != null) {
            builtDeltas.put(rib, delta);
          }
        });
    return builtDeltas;
  }

  public @Nullable Entry<RibDelta<IsisRoute>, RibDelta<IsisRoute>> propagateIsisRoutes(
      final Map<String, Node> nodes, NetworkConfigurations nc) {
    if (_vrf.getIsisProcess() == null) {
      return null;
    }
    RibDelta.Builder<IsisRoute> l1DeltaBuilder = new RibDelta.Builder<>(_isisL1StagingRib);
    RibDelta.Builder<IsisRoute> l2DeltaBuilder = new RibDelta.Builder<>(_isisL2StagingRib);
    IsisRoute.Builder routeBuilder = new IsisRoute.Builder();
    int l1Admin = RoutingProtocol.ISIS_L1.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int l2Admin = RoutingProtocol.ISIS_L2.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    _isisIncomingRoutes.forEach(
        (edge, queue) -> {
          Ip nextHopIp = edge.getNode2().getInterface(nc).getAddress().getIp();
          Interface iface = edge.getNode1().getInterface(nc);
          routeBuilder.setNextHopIp(nextHopIp);
          while (queue.peek() != null) {
            RouteAdvertisement<IsisRoute> routeAdvert = queue.remove();
            IsisRoute neighborRoute = routeAdvert.getRoute();

            routeBuilder
                .setNetwork(neighborRoute.getNetwork())
                .setArea(neighborRoute.getArea())
                .setAttach(neighborRoute.getAttach())
                .setSystemId(neighborRoute.getSystemId());
            boolean withdraw = routeAdvert.isWithdrawn();
            // TODO: simplify
            if (neighborRoute.getLevel() == IsisLevel.LEVEL_1) {
              long incrementalMetric =
                  firstNonNull(iface.getIsis().getLevel1().getCost(), IsisRoute.DEFAULT_METRIC);
              IsisRoute newL1Route =
                  routeBuilder
                      .setAdmin(l1Admin)
                      .setLevel(IsisLevel.LEVEL_1)
                      .setMetric(incrementalMetric + neighborRoute.getMetric())
                      .setProtocol(RoutingProtocol.ISIS_L1)
                      .build();
              if (withdraw) {
                l1DeltaBuilder.remove(newL1Route, Reason.WITHDRAW);
                SortedSet<IsisRoute> backups = _receivedIsisL1Routes.get(newL1Route.getNetwork());
                if (backups != null) {
                  backups.remove(newL1Route);
                }
              } else {
                l1DeltaBuilder.from(_isisL1StagingRib.mergeRouteGetDelta(newL1Route));
                _receivedIsisL1Routes
                    .computeIfAbsent(newL1Route.getNetwork(), k -> new TreeSet<>())
                    .add(newL1Route);
              }
            } else { // neighborRoute is level2
              long incrementalMetric =
                  firstNonNull(iface.getIsis().getLevel2().getCost(), IsisRoute.DEFAULT_METRIC);
              IsisRoute newL2Route =
                  routeBuilder
                      .setAdmin(l2Admin)
                      .setLevel(IsisLevel.LEVEL_2)
                      .setMetric(incrementalMetric + neighborRoute.getMetric())
                      .setProtocol(RoutingProtocol.ISIS_L2)
                      .build();
              if (withdraw) {
                l2DeltaBuilder.remove(newL2Route, Reason.WITHDRAW);
                SortedSet<IsisRoute> backups = _receivedIsisL2Routes.get(newL2Route.getNetwork());
                if (backups != null) {
                  backups.remove(newL2Route);
                }
              } else {
                l2DeltaBuilder.from(_isisL2StagingRib.mergeRouteGetDelta(newL2Route));
                _receivedIsisL2Routes
                    .computeIfAbsent(newL2Route.getNetwork(), k -> new TreeSet<>())
                    .add(newL2Route);
              }
            }
          }
        });
    return new SimpleEntry<>(l1DeltaBuilder.build(), l2DeltaBuilder.build());
  }

  /**
   * Propagate OSPF external routes from our neighbors by reading OSPF route "advertisements" from
   * our queues.
   *
   * @param allNodes map of all nodes, keyed by hostname
   * @param topology the Layer-3 network topology
   * @return a pair of {@link RibDelta}s, for Type1 and Type2 routes
   */
  @Nullable
  public Entry<RibDelta<OspfExternalType1Route>, RibDelta<OspfExternalType2Route>>
      propagateOspfExternalRoutes(final Map<String, Node> allNodes, Topology topology) {
    String node = _c.getHostname();
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return null;
    }
    int admin = RoutingProtocol.OSPF.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so OSPF won't produce anything
      return null;
    }

    RibDelta.Builder<OspfExternalType1Route> builderType1 =
        new RibDelta.Builder<>(_ospfExternalType1StagingRib);
    RibDelta.Builder<OspfExternalType2Route> builderType2 =
        new RibDelta.Builder<>(_ospfExternalType2StagingRib);

    for (Edge edge : edges) {
      if (!edge.getNode1().equals(node)) {
        continue;
      }
      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }
      String neighborName = edge.getNode2();
      Node neighbor = allNodes.get(neighborName);
      String neighborInterfaceName = edge.getInt2();
      OspfArea area = connectingInterface.getOspfArea();
      Configuration nc = neighbor.getConfiguration();
      Interface neighborInterface = nc.getInterfaces().get(neighborInterfaceName);
      String neighborVrfName = neighborInterface.getVrfName();
      VirtualRouter neighborVirtualRouter =
          allNodes.get(neighborName).getVirtualRouters().get(neighborVrfName);

      OspfArea neighborArea = neighborInterface.getOspfArea();
      if (connectingInterface.getOspfEnabled()
          && !connectingInterface.getOspfPassive()
          && neighborInterface.getOspfEnabled()
          && !neighborInterface.getOspfPassive()
          && area != null
          && neighborArea != null
          && area.getName().equals(neighborArea.getName())) {
        /*
         * We have an ospf neighbor relationship on this edge. So we
         * should add all ospf external type 1(2) routes from this
         * neighbor into our ospf external type 1(2) staging rib. For
         * type 1, the cost of the route increases each time. For type 2,
         * the cost remains constant, but we must keep track of cost to
         * advertiser as a tie-breaker.
         */
        long connectingInterfaceCost = connectingInterface.getOspfCost();
        long incrementalCost =
            proc.getMaxMetricTransitLinks() != null
                ? proc.getMaxMetricTransitLinks()
                : connectingInterfaceCost;

        Queue<RouteAdvertisement<OspfExternalRoute>> q =
            _ospfExternalIncomingRoutes.get(connectingInterface.getAddress().getPrefix());
        while (q.peek() != null) {
          RouteAdvertisement<OspfExternalRoute> routeAdvert = q.remove();
          OspfExternalRoute neighborRoute = routeAdvert.getRoute();
          boolean withdraw = routeAdvert.isWithdrawn();
          if (neighborRoute instanceof OspfExternalType1Route) {
            long oldArea = neighborRoute.getArea();
            long connectionArea = area.getName();
            long newArea;
            long baseMetric = neighborRoute.getMetric();
            long baseCostToAdvertiser = neighborRoute.getCostToAdvertiser();
            newArea = connectionArea;
            if (oldArea != OspfRoute.NO_AREA) {
              Long maxMetricSummaryNetworks =
                  neighborVirtualRouter._vrf.getOspfProcess().getMaxMetricSummaryNetworks();
              if (connectionArea != oldArea) {
                if (connectionArea != 0L && oldArea != 0L) {
                  continue;
                }
                if (maxMetricSummaryNetworks != null) {
                  baseMetric = maxMetricSummaryNetworks + neighborRoute.getLsaMetric();
                  baseCostToAdvertiser = maxMetricSummaryNetworks;
                }
              }
            }
            long newMetric = baseMetric + incrementalCost;
            long newCostToAdvertiser = baseCostToAdvertiser + incrementalCost;
            OspfExternalType1Route newRoute =
                new OspfExternalType1Route(
                    neighborRoute.getNetwork(),
                    neighborInterface.getAddress().getIp(),
                    admin,
                    newMetric,
                    neighborRoute.getLsaMetric(),
                    newArea,
                    newCostToAdvertiser,
                    neighborRoute.getAdvertiser());
            if (withdraw) {
              builderType1.remove(newRoute, Reason.WITHDRAW);
              SortedSet<OspfExternalType1Route> backups =
                  _receivedOspExternalType1Routes.get(newRoute.getNetwork());
              if (backups != null) {
                backups.remove(newRoute);
              }
            } else {
              builderType1.from(_ospfExternalType1StagingRib.mergeRouteGetDelta(newRoute));
              _receivedOspExternalType1Routes
                  .computeIfAbsent(newRoute.getNetwork(), k -> new TreeSet<>())
                  .add(newRoute);
            }

          } else if (neighborRoute instanceof OspfExternalType2Route) {
            long oldArea = neighborRoute.getArea();
            long connectionArea = area.getName();
            long newArea;
            long baseCostToAdvertiser = neighborRoute.getCostToAdvertiser();
            if (oldArea == OspfRoute.NO_AREA) {
              newArea = connectionArea;
            } else {
              newArea = oldArea;
              Long maxMetricSummaryNetworks =
                  neighborVirtualRouter._vrf.getOspfProcess().getMaxMetricSummaryNetworks();
              if (connectionArea != oldArea && maxMetricSummaryNetworks != null) {
                baseCostToAdvertiser = maxMetricSummaryNetworks;
              }
            }
            long newCostToAdvertiser = baseCostToAdvertiser + incrementalCost;
            OspfExternalType2Route newRoute =
                new OspfExternalType2Route(
                    neighborRoute.getNetwork(),
                    neighborInterface.getAddress().getIp(),
                    admin,
                    neighborRoute.getMetric(),
                    neighborRoute.getLsaMetric(),
                    newArea,
                    newCostToAdvertiser,
                    neighborRoute.getAdvertiser());
            if (withdraw) {
              builderType2.remove(newRoute, Reason.WITHDRAW);
              SortedSet<OspfExternalType2Route> backups =
                  _receivedOspExternalType2Routes.get(newRoute.getNetwork());
              if (backups != null) {
                backups.remove(newRoute);
              }
            } else {
              builderType2.from(_ospfExternalType2StagingRib.mergeRouteGetDelta(newRoute));
              _receivedOspExternalType2Routes
                  .computeIfAbsent(newRoute.getNetwork(), k -> new TreeSet<>())
                  .add(newRoute);
            }
          }
        }
      }
    }
    return new SimpleEntry<>(builderType1.build(), builderType2.build());
  }

  /**
   * Construct an OSPF Inter-Area route and put into our staging rib. Note, no route validity checks
   * are performed, (i.e., whether the route should even go into the staging rib). {@link
   * #propagateOspfInternalRoutesFromNeighbor} takes care of such logic.
   *
   * @param neighborRoute the route to propagate
   * @param nextHopIp nextHopIp for this route (the neighbor's IP)
   * @param incrementalCost OSPF cost of the interface from which this route came (added to route
   *     cost)
   * @param adminCost OSPF administrative distance
   * @param areaNum area number of the route
   * @return True if the route was added to the inter-area staging RIB
   */
  @VisibleForTesting
  boolean stageOspfInterAreaRoute(
      OspfInternalRoute neighborRoute,
      Long maxMetricSummaryNetworks,
      Ip nextHopIp,
      long incrementalCost,
      int adminCost,
      long areaNum) {
    long newCost;
    if (maxMetricSummaryNetworks != null) {
      newCost = maxMetricSummaryNetworks + incrementalCost;
    } else {
      newCost = neighborRoute.getMetric() + incrementalCost;
    }
    OspfInterAreaRoute newRoute =
        new OspfInterAreaRoute(neighborRoute.getNetwork(), nextHopIp, adminCost, newCost, areaNum);
    return _ospfInterAreaStagingRib.mergeRoute(newRoute);
  }

  boolean propagateOspfInterAreaRouteFromIntraAreaRoute(
      Configuration neighbor,
      OspfProcess neighborProc,
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long linkAreaNum) {
    return OspfProtocolHelper.isOspfInterAreaFromIntraAreaPropagationAllowed(
            linkAreaNum, neighbor, neighborProc, neighborRoute, neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getIp(),
            incrementalCost,
            adminCost,
            linkAreaNum);
  }

  /**
   * Propagate OSPF Internal routes from a single neighbor.
   *
   * @param proc The receiving OSPF process
   * @param neighbor the neighbor
   * @param connectingInterface interface on which we are connected to the neighbor
   * @param neighborInterface interface that the neighbor uses to connect to us
   * @param adminCost route administrative distance
   * @return true if new routes have been added to our staging RIB
   */
  boolean propagateOspfInternalRoutesFromNeighbor(
      OspfProcess proc,
      Node neighbor,
      Interface connectingInterface,
      Interface neighborInterface,
      int adminCost) {
    OspfArea area = connectingInterface.getOspfArea();
    OspfArea neighborArea = neighborInterface.getOspfArea();
    // Ensure that the link (i.e., both interfaces) has OSPF enabled and OSPF areas are set
    if (!connectingInterface.getOspfEnabled()
        || connectingInterface.getOspfPassive()
        || !neighborInterface.getOspfEnabled()
        || neighborInterface.getOspfPassive()
        || area == null
        || neighborArea == null
        || !area.getName().equals(neighborArea.getName())) {
      return false;
    }
    /*
     * An OSPF neighbor relationship exists on this edge. So we examine all intra- and inter-area
     * routes belonging to the neighbor to see what should be propagated to this router. We add the
     * incremental cost associated with our settings and the connecting interface, and use the
     * neighborInterface's address as the next hop ip.
     */
    int connectingInterfaceCost = connectingInterface.getOspfCost();
    long incrementalCost =
        proc.getMaxMetricTransitLinks() != null
            ? proc.getMaxMetricTransitLinks()
            : connectingInterfaceCost;
    Long linkAreaNum = area.getName();
    Configuration neighborConfiguration = neighbor.getConfiguration();
    String neighborVrfName = neighborInterface.getVrfName();
    OspfProcess neighborProc =
        neighborConfiguration.getVrfs().get(neighborVrfName).getOspfProcess();
    VirtualRouter neighborVirtualRouter = neighbor.getVirtualRouters().get(neighborVrfName);
    boolean changed = false;
    for (OspfIntraAreaRoute neighborRoute : neighborVirtualRouter._ospfIntraAreaRib.getRoutes()) {
      changed |=
          propagateOspfIntraAreaRoute(
              neighborRoute, incrementalCost, neighborInterface, adminCost, linkAreaNum);
      changed |=
          propagateOspfInterAreaRouteFromIntraAreaRoute(
              neighborConfiguration,
              neighborProc,
              neighborRoute,
              incrementalCost,
              neighborInterface,
              adminCost,
              linkAreaNum);
    }
    for (OspfInterAreaRoute neighborRoute : neighborVirtualRouter._ospfInterAreaRib.getRoutes()) {
      changed |=
          propagateOspfInterAreaRouteFromInterAreaRoute(
              proc,
              neighborConfiguration,
              neighborProc,
              neighborRoute,
              incrementalCost,
              neighborInterface,
              adminCost,
              linkAreaNum);
    }
    changed |=
        originateOspfStubAreaDefaultRoute(
            neighborProc, incrementalCost, neighborInterface, adminCost, linkAreaNum);
    return changed;
  }

  /**
   * If neighbor is an ABR and this is a stub area link, propagate
   *
   * @param neighborProc The adjacent {@link OspfProcess}
   * @param incrementalCost The cost to reach the propagator
   * @param neighborInterface The propagator's interface on the link
   * @param adminCost The administrative cost of the route to be installed
   * @param linkAreaNum The area ID of the link
   * @return whether this route changed the RIB into which we merged it
   */
  private boolean originateOspfStubAreaDefaultRoute(
      OspfProcess neighborProc,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long linkAreaNum) {
    return OspfProtocolHelper.isOspfInterAreaDefaultOriginationAllowed(
            _vrf.getOspfProcess(), neighborProc, neighborInterface.getOspfArea())
        && _ospfInterAreaStagingRib.mergeRoute(
            new OspfInterAreaRoute(
                Prefix.ZERO,
                neighborInterface.getAddress().getIp(),
                adminCost,
                incrementalCost,
                linkAreaNum));
  }

  boolean propagateOspfInterAreaRouteFromInterAreaRoute(
      OspfProcess proc,
      Configuration neighbor,
      OspfProcess neighborProc,
      OspfInterAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long linkAreaNum) {
    return OspfProtocolHelper.isOspfInterAreaFromInterAreaPropagationAllowed(
            proc,
            linkAreaNum,
            neighbor,
            neighborProc,
            neighborRoute,
            neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getIp(),
            incrementalCost,
            adminCost,
            linkAreaNum);
  }

  boolean propagateOspfIntraAreaRoute(
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long linkAreaNum) {
    long newCost = neighborRoute.getMetric() + incrementalCost;
    Ip nextHopIp = neighborInterface.getAddress().getIp();
    OspfIntraAreaRoute newRoute =
        new OspfIntraAreaRoute(
            neighborRoute.getNetwork(), nextHopIp, adminCost, newCost, linkAreaNum);
    return neighborRoute.getArea() == linkAreaNum
        && (_ospfIntraAreaStagingRib.mergeRoute(newRoute));
  }

  /**
   * Propagate OSPF internal routes from every valid OSPF neighbor
   *
   * @param nodes mapping of node names to instances.
   * @param topology network topology
   * @return true if new routes have been added to the staging RIB
   */
  boolean propagateOspfInternalRoutes(Map<String, Node> nodes, Topology topology) {
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return false; // nothing to do
    }

    boolean changed = false;
    String node = _c.getHostname();

    // Default OSPF admin cost for constructing new routes
    int adminCost = RoutingProtocol.OSPF.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so OSPF won't produce anything
      return false;
    }

    for (Edge edge : edges) {
      if (!edge.getNode1().equals(node)) {
        continue;
      }

      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }

      String neighborName = edge.getNode2();
      Node neighbor = nodes.get(neighborName);
      Interface neighborInterface = neighbor.getConfiguration().getInterfaces().get(edge.getInt2());

      changed |=
          propagateOspfInternalRoutesFromNeighbor(
              proc, neighbor, connectingInterface, neighborInterface, adminCost);
    }
    return changed;
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
          neighbor.getConfiguration().getInterfaces().get(neighborInterfaceName);
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
        for (RipInternalRoute neighborRoute : neighborVirtualRouter._ripInternalRib.getRoutes()) {
          long newCost = neighborRoute.getMetric() + RipProcess.DEFAULT_RIP_COST;
          Ip nextHopIp = neighborInterface.getAddress().getIp();
          RipInternalRoute newRoute =
              new RipInternalRoute(neighborRoute.getNetwork(), nextHopIp, admin, newCost);
          if (_ripInternalStagingRib.mergeRouteGetDelta(newRoute) != null) {
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
   *     #_bgpBestPathRib}
   * @param bgpMultiPathDelta a {@link RibDelta} indicating what changed in the {@link
   *     #_bgpMultipathRib}
   * @param mainDelta a {@link RibDelta} indicating what changed in the {@link #_mainRib}
   * @param allNodes map of all nodes in the network, keyed by hostname
   * @param bgpTopology the bgp peering relationships
   */
  private void queueOutgoingBgpRoutes(
      RibDelta<BgpRoute> ebgpBestPathDelta,
      @Nullable RibDelta<BgpRoute> bgpMultiPathDelta,
      @Nullable RibDelta<AbstractRoute> mainDelta,
      final Map<String, Node> allNodes,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology,
      NetworkConfigurations networkConfigurations) {
    for (BgpEdgeId edge : _bgpIncomingRoutes.keySet()) {
      final BgpSessionProperties session = getBgpSessionProperties(bgpTopology, edge);

      BgpPeerConfigId remoteConfigId = edge.src();
      BgpPeerConfigId ourConfigId = edge.dst();
      BgpPeerConfig ourConfig = networkConfigurations.getBgpPeerConfig(edge.dst());
      BgpPeerConfig remoteConfig = networkConfigurations.getBgpPeerConfig(edge.src());
      VirtualRouter remoteVirtualRouter = getRemoteBgpNeighborVR(remoteConfigId, allNodes);
      if (remoteVirtualRouter == null) {
        continue;
      }

      Builder<AbstractRoute> finalBuilder = new Builder<>(null);

      // Definitely queue mainRib updates
      finalBuilder.from(mainDelta);
      // These knobs control which additional BGP routes get advertised
      if (session.getAdvertiseExternal()) {
        /*
         * Advertise external ensures that even if we withdrew an external route from the RIB
         */
        finalBuilder.from(ebgpBestPathDelta);
      }
      if (session.getAdvertiseInactive()) {
        /*
         * In case BGP routes were deleted from the main RIB
         * (e.g., preempted by a better IGP route)
         * and advertiseInactive is true, re-add inactive BGP routes from the BGP best-path RIB.
         * If the BGP routes are already active, this will have no effect.
         */
        if (mainDelta != null) {
          for (Prefix p : mainDelta.getPrefixes()) {
            if (_bgpBestPathRib.getRoutes(p) == null) {
              continue;
            }
            finalBuilder.add(_bgpBestPathRib.getRoutes(p));
          }
        }
      }
      if (session.getAdditionalPaths()) {
        finalBuilder.from(bgpMultiPathDelta);
      }
      RibDelta<AbstractRoute> routesToExport = finalBuilder.build();
      if (routesToExport == null) {
        continue;
      }

      // Compute a set of advertisements that can be queued on remote VR
      Set<RouteAdvertisement<BgpRoute>> exportedAdvertisements =
          routesToExport
              .getActions()
              .stream()
              .map(
                  adv -> {
                    BgpRoute transformedRoute =
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
                        : new RouteAdvertisement<>(
                            // REPLACE does not make sense across routers, update with WITHDRAW
                            transformedRoute,
                            adv.isWithdrawn(),
                            adv.getReason() == Reason.REPLACE ? Reason.WITHDRAW : adv.getReason());
                  })
              .filter(Objects::nonNull)
              .collect(ImmutableSet.toImmutableSet());

      // Call this on the REMOTE VR and REVERSE the edge!
      remoteVirtualRouter.enqueueBgpMessages(edge.reverse(), exportedAdvertisements);

      // Note what we sent
      markSentBgpAdvertisements(
          ourConfigId, remoteConfigId, ourConfig, remoteConfig, session, exportedAdvertisements);
    }
  }

  private static BgpSessionProperties getBgpSessionProperties(
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology, BgpEdgeId edge) {
    /*
    BGP topology edges not guaranteed to be symmetrical (in case of dynamic neighbors).
    So to get session properties, we might need to flip the src/dst edge
     */
    BgpSessionProperties tmpSession;
    try {
      tmpSession = bgpTopology.edgeValue(edge.src(), edge.dst());
    } catch (IllegalArgumentException e) {
      tmpSession = bgpTopology.edgeValue(edge.dst(), edge.src());
    }
    return tmpSession;
  }

  private void queueOutgoingIsisRoutes(
      @Nonnull Map<String, Node> allNodes,
      NetworkConfigurations nc,
      @Nullable RibDelta<IsisRoute> l1delta,
      @Nullable RibDelta<IsisRoute> l2delta) {
    if (_vrf.getIsisProcess() == null || _isisIncomingRoutes == null) {
      return;
    }
    // Loop over neighbors, enqueue messages
    _isisIncomingRoutes
        .keySet()
        .forEach(
            edge -> {
              VirtualRouter remoteVr =
                  allNodes
                      .get(edge.getNode1().getHostname())
                      .getVirtualRouters()
                      .get(edge.getNode1().getInterface(nc).getVrfName());
              Queue<RouteAdvertisement<IsisRoute>> queue =
                  remoteVr._isisIncomingRoutes.get(edge.reverse());
              IsisLevel circuitType = edge.getCircuitType();
              if (circuitType == IsisLevel.LEVEL_1_2 || circuitType == IsisLevel.LEVEL_1) {
                queueDelta(queue, l1delta);
              }
              if (circuitType == IsisLevel.LEVEL_1_2 || circuitType == IsisLevel.LEVEL_2) {
                queueDelta(queue, l2delta);
                if (_vrf.getIsisProcess().getLevel1() != null
                    && _vrf.getIsisProcess().getLevel2() != null
                    && l1delta != null) {

                  // We are a L1_L2 router, we must "upgrade" L1 routes to L2 routes
                  // TODO: a little cumbersome, simplify later
                  RibDelta.Builder<IsisRoute> upgradedRoutes = new RibDelta.Builder<>(null);
                  l1delta
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
            });
  }

  /**
   * Send out OSPF External route updates to our neighbors
   *
   * @param type1delta A {@link RibDelta} containing diffs with respect to OSPF Type1 external
   *     routes
   * @param type2delta A {@link RibDelta} containing diffs with respect to OSPF Type2 external
   *     routes
   */
  private void queueOutgoingOspfExternalRoutes(
      @Nullable RibDelta<OspfExternalType1Route> type1delta,
      @Nullable RibDelta<OspfExternalType2Route> type2delta) {
    if (_vrf.getOspfProcess() == null) {
      return;
    }
    if (_ospfNeighbors != null) {
      _ospfNeighbors.forEach(
          (key, ospfLink) -> {
            if (ospfLink._localOspfArea.getStubType() == StubType.STUB) {
              return;
            }
            // Get remote neighbor's queue by prefix
            Queue<RouteAdvertisement<OspfExternalRoute>> q =
                ospfLink._remoteVirtualRouter._ospfExternalIncomingRoutes.get(key);
            queueDelta(q, type1delta);
            queueDelta(q, type2delta);
          });
    }
  }

  /**
   * Propagate BGP routes received from neighbours into the appropriate RIBs. As the propagation is
   * happening, queue appropriate outgoing messages to neighbors as well.
   *
   * @param multipathEbgp whether or not EBGP is multipath
   * @param multipathIbgp whether or not IBGP is multipath
   * @param stagingDeltas a map of RIB to corresponding delta. Keys are expected to contain {@link
   *     #_ebgpStagingRib} and {@link #_ibgpStagingRib}
   * @param bgpTopology the bgp peering relationships
   */
  void finalizeBgpRoutesAndQueueOutgoingMessages(
      boolean multipathEbgp,
      boolean multipathIbgp,
      Map<BgpMultipathRib, RibDelta<BgpRoute>> stagingDeltas,
      final Map<String, Node> allNodes,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology,
      NetworkConfigurations networkConfigurations) {

    RibDelta<BgpRoute> ebgpStagingDelta = stagingDeltas.get(_ebgpStagingRib);
    RibDelta<BgpRoute> ibgpStagingDelta = stagingDeltas.get(_ibgpStagingRib);

    Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> e;
    RibDelta<BgpRoute> ebgpBestPathDelta;
    if (multipathEbgp) {
      e = syncBgpDeltaPropagation(_bgpBestPathRib, _bgpMultipathRib, ebgpStagingDelta);
      ebgpBestPathDelta = e.getKey();
      _bgpBestPathDeltaBuilder.from(e.getKey());
      _bgpMultiPathDeltaBuilder.from(e.getValue());
    } else {
      ebgpBestPathDelta = importRibDelta(_bgpBestPathRib, ebgpStagingDelta);
      _bgpBestPathDeltaBuilder.from(ebgpBestPathDelta);
      _bgpMultiPathDeltaBuilder.from(ebgpBestPathDelta);
    }

    if (multipathIbgp) {
      e = syncBgpDeltaPropagation(_bgpBestPathRib, _bgpMultipathRib, ibgpStagingDelta);
      _bgpBestPathDeltaBuilder.from(e.getKey());
      _bgpMultiPathDeltaBuilder.from(e.getValue());
    } else {
      RibDelta<BgpRoute> ibgpBestPathDelta = importRibDelta(_bgpBestPathRib, ibgpStagingDelta);
      _bgpBestPathDeltaBuilder.from(ibgpBestPathDelta);
      _bgpMultiPathDeltaBuilder.from(ibgpBestPathDelta);
    }

    _mainRibRouteDeltaBuiler.from(importRibDelta(_mainRib, _bgpMultiPathDeltaBuilder.build()));

    queueOutgoingBgpRoutes(
        ebgpBestPathDelta,
        _bgpMultiPathDeltaBuilder.build(),
        _mainRibRouteDeltaBuiler.build(),
        allNodes,
        bgpTopology,
        networkConfigurations);
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
    Builder<IsisRoute> isisDeltaBuilder = new Builder<>(_isisRib);
    isisDeltaBuilder.from(importRibDelta(_isisRib, d1));
    isisDeltaBuilder.from(importRibDelta(_isisRib, d2));
    _mainRibRouteDeltaBuiler.from(importRibDelta(_mainRib, isisDeltaBuilder.build()));
    return d1 != null || d2 != null;
  }

  /**
   * Merges staged OSPF external routes into the "real" OSPF-external RIBs
   *
   * @param type1Delta a {@link RibDelta} indicating changes to be made to {@link
   *     #_ospfExternalType1Rib}
   * @param type2Delta a {@link RibDelta} indicating changes to be made to {@link
   *     #_ospfExternalType2Rib}
   */
  boolean unstageOspfExternalRoutes(
      RibDelta<OspfExternalType1Route> type1Delta, RibDelta<OspfExternalType2Route> type2Delta) {
    RibDelta<OspfExternalType1Route> d1 = importRibDelta(_ospfExternalType1Rib, type1Delta);
    RibDelta<OspfExternalType2Route> d2 = importRibDelta(_ospfExternalType2Rib, type2Delta);
    queueOutgoingOspfExternalRoutes(d1, d2);
    Builder<OspfRoute> ospfDeltaBuilder = new Builder<>(_ospfRib);
    ospfDeltaBuilder.from(importRibDelta(_ospfRib, d1));
    ospfDeltaBuilder.from(importRibDelta(_ospfRib, d2));
    _mainRibRouteDeltaBuiler.from(importRibDelta(_mainRib, ospfDeltaBuilder.build()));
    return d1 != null || d2 != null;
  }

  /** Merges staged OSPF internal routes into the "real" OSPF-internal RIBs */
  void unstageOspfInternalRoutes() {
    importRib(_ospfIntraAreaRib, _ospfIntraAreaStagingRib);
    importRib(_ospfInterAreaRib, _ospfInterAreaStagingRib);
  }

  /** Merges staged RIP routes into the "real" RIP RIB */
  void unstageRipInternalRoutes() {
    importRib(_ripInternalRib, _ripInternalStagingRib);
  }

  /** Re-initialize RIBs (at the start of each iteration). */
  void reinitForNewIteration() {
    _mainRibRouteDeltaBuiler = new Builder<>(_mainRib);
    _bgpBestPathDeltaBuilder = new RibDelta.Builder<>(_bgpBestPathRib);
    _bgpMultiPathDeltaBuilder = new RibDelta.Builder<>(_bgpMultipathRib);
    _ospfExternalDeltaBuiler = new RibDelta.Builder<>(null);

    /*
     * RIBs not read from can just be re-initialized
     */
    _ospfRib = new OspfRib();
    _ripRib = new RipRib();

    /*
     * Staging RIBs can also be re-initialized
     */
    MultipathEquivalentAsPathMatchMode mpTieBreaker =
        _vrf.getBgpProcess() == null
            ? EXACT_PATH
            : _vrf.getBgpProcess().getMultipathEquivalentAsPathMatchMode();
    _ebgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _ibgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(getHostname(), null);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(getHostname(), null);

    /*
     * Add routes that cannot change (does not affect below computation)
     */
    _mainRibRouteDeltaBuiler.from(importRib(_mainRib, _independentRib));

    /*
     * Re-add independent OSPF routes to ospfRib for tie-breaking
     */
    importRib(_ospfRib, _ospfIntraAreaRib);
    importRib(_ospfRib, _ospfInterAreaRib);
    /*
     * Re-add independent RIP routes to ripRib for tie-breaking
     */
    importRib(_ripRib, _ripInternalRib);
  }

  /**
   * Merge intra/inter OSPF RIBs into a general OSPF RIB, then merge that into the independent RIB
   */
  void importOspfInternalRoutes() {
    importRib(_ospfRib, _ospfIntraAreaRib);
    importRib(_ospfRib, _ospfInterAreaRib);
    importRib(_independentRib, _ospfRib);
  }

  /**
   * Check if RIBs that contribute to the dataplane "dependent routes" computation have any routes
   * that still need to be merged. I.e., if this method returns true, we cannot converge yet.
   *
   * @return true if there are any routes remaining, in need of merging in to the RIBs
   */
  boolean hasOutstandingRoutes() {
    return _ospfExternalDeltaBuiler.build() != null
        || _mainRibRouteDeltaBuiler.build() != null
        || _bgpBestPathDeltaBuilder.build() != null
        || _bgpMultiPathDeltaBuilder.build() != null;
  }

  /**
   * Check if this router has processed all its incoming BGP messages (i.e., all router queues are
   * empty)
   *
   * @return true if all queues are empty.
   */
  boolean hasProcessedAllMessages() {
    boolean processedAll = true;
    // Check the BGP message queues
    if (_vrf.getBgpProcess() != null) {
      processedAll =
          _bgpIncomingRoutes
              .values()
              .stream()
              .map(Queue::isEmpty)
              .noneMatch(Predicate.isEqual(false));
    }
    // Check the OSPF external message queues
    if (_vrf.getOspfProcess() != null) {
      for (Queue<RouteAdvertisement<OspfExternalRoute>> queue :
          _ospfExternalIncomingRoutes.values()) {
        if (!queue.isEmpty()) {
          return false;
        }
      }
    }
    if (_vrf.getIsisProcess() != null) {
      for (Queue<RouteAdvertisement<IsisRoute>> queue : _isisIncomingRoutes.values()) {
        if (!queue.isEmpty()) {
          return false;
        }
      }
    }
    return processedAll;
  }

  /**
   * Queues initial round of outgoing BGP messages based on the state of the RIBs prior to any data
   * plane iterations.
   */
  void queueInitialBgpMessages(
      final ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology,
      final Map<String, Node> allNodes,
      NetworkConfigurations nc) {
    if (_vrf.getBgpProcess() == null) {
      // nothing to do
      return;
    }
    for (BgpEdgeId edge : _bgpIncomingRoutes.keySet()) {
      newBgpSessionEstablishedHook(edge, getBgpSessionProperties(bgpTopology, edge), allNodes, nc);
    }
  }

  /**
   * Utility "message passing" method between virtual routers. Take a set of BGP {@link
   * RouteAdvertisement}s and puts them onto a local queue corresponding to the session between
   * given neighbors.
   *
   * @param routes a set of BGP routes that are being exchanged
   */
  private void enqueueBgpMessages(
      @Nonnull BgpEdgeId edgeId, @Nonnull Set<RouteAdvertisement<BgpRoute>> routes) {
    _bgpIncomingRoutes.get(edgeId).addAll(routes);
  }

  /** Note which advertisement we sent, in full {@link BgpAdvertisement} form. */
  private void markSentBgpAdvertisements(
      BgpPeerConfigId localNeighborId,
      BgpPeerConfigId remoteNeighborId,
      BgpPeerConfig localNeighbor,
      BgpPeerConfig remoteNeighbor,
      BgpSessionProperties sessionProperties,
      Set<RouteAdvertisement<BgpRoute>> routeAdvertisements) {
    for (RouteAdvertisement<BgpRoute> routeAdvertisement : routeAdvertisements) {
      if (!routeAdvertisement.isWithdrawn()) {
        BgpRoute route = routeAdvertisement.getRoute();
        _sentBgpAdvertisements.add(
            BgpAdvertisement.builder()
                .setType(
                    sessionProperties.isEbgp()
                        ? BgpAdvertisementType.EBGP_SENT
                        : BgpAdvertisementType.IBGP_SENT)
                .setNetwork(route.getNetwork())
                .setNextHopIp(route.getNextHopIp())
                .setSrcNode(getHostname())
                .setSrcVrf(localNeighborId.getVrfName())
                .setSrcIp(localNeighbor.getLocalIp())
                .setDstNode(remoteNeighborId.getHostname())
                .setDstVrf(remoteNeighborId.getVrfName())
                .setDstIp(remoteNeighbor.getLocalIp())
                .setSrcProtocol(
                    sessionProperties.isEbgp() ? RoutingProtocol.BGP : RoutingProtocol.IBGP)
                .setOriginType(route.getOriginType())
                .setLocalPreference(route.getLocalPreference())
                .setMed(route.getMetric())
                .setOriginatorIp(route.getOriginatorIp())
                .setAsPath(route.getAsPath())
                .setCommunities(route.getCommunities())
                .setClusterList(route.getClusterList())
                .setWeight(-1)
                .build());
      }
    }
  }

  /** Note which advertisement we received, in full {@link BgpAdvertisement} form. */
  private void markReceivedBgpAdvertisement(
      BgpPeerConfigId localNeighborId,
      BgpPeerConfigId remoteNeighborId,
      BgpPeerConfig localNeighbor,
      BgpPeerConfig remoteNeighbor,
      BgpSessionProperties sessionProperties,
      BgpRoute route) {

    _receivedBgpAdvertisements.add(
        BgpAdvertisement.builder()
            .setType(
                sessionProperties.isEbgp()
                    ? BgpAdvertisementType.EBGP_RECEIVED
                    : BgpAdvertisementType.IBGP_RECEIVED)
            .setNetwork(route.getNetwork())
            .setNextHopIp(route.getNextHopIp())
            .setSrcNode(remoteNeighborId.getHostname())
            .setSrcVrf(remoteNeighborId.getVrfName())
            .setSrcIp(remoteNeighbor.getLocalIp())
            .setDstNode(getHostname())
            .setDstVrf(localNeighborId.getVrfName())
            .setDstIp(localNeighbor.getLocalIp())
            .setSrcProtocol(route.getProtocol())
            .setOriginType(route.getOriginType())
            .setLocalPreference(route.getLocalPreference())
            .setMed(route.getMetric())
            .setOriginatorIp(route.getOriginatorIp())
            .setAsPath(route.getAsPath())
            .setCommunities(route.getCommunities())
            .setClusterList(route.getClusterList())
            .setWeight(route.getWeight())
            .build());
  }

  /** Deal with a newly established BGP session. */
  private void newBgpSessionEstablishedHook(
      @Nonnull BgpEdgeId edge,
      @Nonnull BgpSessionProperties sessionProperties,
      @Nonnull Map<String, Node> allNodes,
      NetworkConfigurations nc) {

    BgpPeerConfigId localConfigId = edge.dst();
    BgpPeerConfigId remoteConfigId = edge.src();
    BgpPeerConfig localConfig = nc.getBgpPeerConfig(localConfigId);
    BgpPeerConfig remoteConfig = nc.getBgpPeerConfig(remoteConfigId);

    VirtualRouter remoteVr = getRemoteBgpNeighborVR(remoteConfigId, allNodes);
    if (remoteVr == null) {
      return;
    }

    // Note prefixes we tried to originate
    _mainRib.getRoutes().forEach(r -> _prefixTracer.originated(r.getNetwork()));

    /*
     * Export route advertisements by looking at main RIB
     */
    Set<RouteAdvertisement<BgpRoute>> exportedRoutes =
        _mainRib
            .getRoutes()
            .stream()
            // This performs transformations and filtering using the export policy
            .map(
                r ->
                    exportBgpRoute(
                        r,
                        localConfigId,
                        remoteConfigId,
                        localConfig,
                        remoteConfig,
                        allNodes,
                        sessionProperties))
            .filter(Objects::nonNull)
            .map(RouteAdvertisement::new)
            .collect(ImmutableSet.toImmutableSet());

    // Call this on the neighbor's VR!
    remoteVr.enqueueBgpMessages(edge.reverse(), exportedRoutes);

    /*
     * Export neighbor-specific generated routes, these routes skip global export policy
     */
    Set<RouteAdvertisement<BgpRoute>> exportedNeighborSpecificRoutes =
        localConfig
            .getGeneratedRoutes()
            .stream()
            .map(this::processNeighborSpecificGeneratedRoute)
            .filter(Objects::nonNull)
            .map(RouteAdvertisement::new)
            .collect(ImmutableSet.toImmutableSet());

    // Call this on the neighbor's VR, and reverse the egde!
    remoteVr.enqueueBgpMessages(edge.reverse(), exportedNeighborSpecificRoutes);

    // Note which BGP advertisements were sent
    markSentBgpAdvertisements(
        localConfigId,
        remoteConfigId,
        localConfig,
        remoteConfig,
        sessionProperties,
        exportedRoutes);
    markSentBgpAdvertisements(
        localConfigId,
        remoteConfigId,
        localConfig,
        remoteConfig,
        sessionProperties,
        exportedNeighborSpecificRoutes);
  }

  /**
   * Check whether given {@link GeneratedRoute} should be sent to a BGP neighbor. This checks
   * activation conditions for the generated route, and converts it to a {@link BgpRoute}. No export
   * policy computation is performed.
   *
   * @param generatedRoute route to process
   * @return a new {@link BgpRoute} if the {@code generatedRoute} was activated.
   */
  @Nullable
  private BgpRoute processNeighborSpecificGeneratedRoute(@Nonnull GeneratedRoute generatedRoute) {
    String policyName = generatedRoute.getGenerationPolicy();
    RoutingPolicy policy = policyName != null ? _c.getRoutingPolicies().get(policyName) : null;
    GeneratedRoute.Builder builder =
        GeneratedRouteHelper.activateGeneratedRoute(
            generatedRoute, policy, _mainRib.getRoutes(), _vrf.getName());
    return builder != null
        ? BgpProtocolHelper.convertGeneratedRouteToBgp(
            builder.build(), _vrf.getBgpProcess().getRouterId())
        : null;
  }

  /**
   * Compute our OSPF neighbors.
   *
   * @param allNodes map of all network nodes, keyed by hostname
   * @param topology the Layer-3 network topology
   * @return A sorted map of neighbor prefixes to links to which they correspond
   */
  @Nullable
  SortedMap<Prefix, OspfLink> getOspfNeighbors(
      final Map<String, Node> allNodes, Topology topology) {
    // Check we have ospf process
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return null;
    }

    String node = _c.getHostname();
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so OSPF won't produce anything
      return null;
    }

    SortedMap<Prefix, OspfLink> neighbors = new TreeMap<>();
    for (Edge edge : edges) {
      if (!edge.getNode1().equals(node)) {
        continue;
      }
      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }
      String neighborName = edge.getNode2();
      Node neighbor = allNodes.get(neighborName);
      String neighborInterfaceName = edge.getInt2();
      OspfArea area = connectingInterface.getOspfArea();
      Configuration nc = neighbor.getConfiguration();
      Interface neighborInterface = nc.getInterfaces().get(neighborInterfaceName);
      String neighborVrfName = neighborInterface.getVrfName();
      VirtualRouter neighborVirtualRouter =
          allNodes.get(neighborName).getVirtualRouters().get(neighborVrfName);

      OspfArea neighborArea = neighborInterface.getOspfArea();
      if (connectingInterface.getOspfEnabled()
          && !connectingInterface.getOspfPassive()
          && neighborInterface.getOspfEnabled()
          && !neighborInterface.getOspfPassive()
          && area != null
          && neighborArea != null
          && area.getName().equals(neighborArea.getName())) {
        neighbors.put(
            connectingInterface.getAddress().getPrefix(),
            new OspfLink(area, neighborArea, neighborVirtualRouter));
      }
    }

    return ImmutableSortedMap.copyOf(neighbors);
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

  BgpBestPathRib getBgpBestPathRib() {
    return _bgpBestPathRib;
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
   *   <li>"external" RIBs ({@link #_mainRib}, {@link #_ospfExternalType1Rib}, {@link
   *       #_ospfExternalType2Rib}
   *   <li>message queues ({@link #_bgpIncomingRoutes} and {@link #_ospfExternalIncomingRoutes})
   * </ul>
   *
   * @return integer hashcode
   */
  int computeIterationHashCode() {
    return _mainRib.getRoutes().hashCode()
        + _ospfExternalType1Rib.getRoutes().hashCode()
        + _ospfExternalType2Rib.getRoutes().hashCode()
        + _bgpIncomingRoutes
            .values()
            .stream()
            .flatMap(Queue::stream)
            .mapToInt(RouteAdvertisement::hashCode)
            .sum()
        + _ospfExternalIncomingRoutes
            .values()
            .stream()
            .flatMap(Queue::stream)
            .mapToInt(RouteAdvertisement::hashCode)
            .sum()
        + _isisIncomingRoutes
            .values()
            .stream()
            .flatMap(Queue::stream)
            .mapToInt(RouteAdvertisement::hashCode)
            .sum();
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
   * @param remoteConfig {@link BgpPeerConfig} that will be receiving the route
   * @param allNodes all nodes in the network
   * @return The transformed route as a {@link BgpRoute}, or {@code null} if the route should not be
   *     exported.
   */
  @Nullable
  private BgpRoute exportBgpRoute(
      @Nonnull AbstractRoute exportCandidate,
      @Nonnull BgpPeerConfigId ourConfigId,
      @Nonnull BgpPeerConfigId remoteConfigId,
      @Nonnull BgpPeerConfig ourConfig,
      @Nonnull BgpPeerConfig remoteConfig,
      @Nonnull Map<String, Node> allNodes,
      @Nonnull BgpSessionProperties sessionProperties) {

    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(ourConfig.getExportPolicy());
    BgpRoute.Builder transformedOutgoingRouteBuilder;
    try {
      transformedOutgoingRouteBuilder =
          BgpProtocolHelper.transformBgpRouteOnExport(
              ourConfig,
              remoteConfig,
              sessionProperties,
              _vrf,
              requireNonNull(getRemoteBgpNeighborVR(remoteConfigId, allNodes))._vrf,
              exportCandidate);
    } catch (BgpRoutePropagationException e) {
      // TODO: Log a warning
      return null;
    }
    if (transformedOutgoingRouteBuilder == null) {
      // This route could not be exported for core bgp protocol reasons
      return null;
    }

    // Process transformed outgoing route by the export policy
    boolean shouldExport =
        exportPolicy.process(
            exportCandidate,
            transformedOutgoingRouteBuilder,
            remoteConfig.getLocalIp(),
            ourConfigId.getRemotePeerPrefix(),
            ourConfigId.getVrfName(),
            Direction.OUT);

    VirtualRouter remoteVr = getRemoteBgpNeighborVR(remoteConfigId, allNodes);
    if (!shouldExport) {
      // This route could not be exported due to export policy
      _prefixTracer.filtered(
          exportCandidate.getNetwork(),
          requireNonNull(remoteVr).getHostname(),
          remoteConfig.getLocalIp(),
          remoteConfigId.getVrfName(),
          ourConfig.getExportPolicy(),
          Direction.OUT);
      return null;
    }

    // Successfully exported route
    BgpRoute transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
    _prefixTracer.sentTo(
        transformedOutgoingRoute.getNetwork(),
        requireNonNull(remoteVr).getHostname(),
        remoteConfig.getLocalIp(),
        remoteConfigId.getVrfName(),
        ourConfig.getExportPolicy());

    return transformedOutgoingRoute;
  }

  Set<BgpAdvertisement> getReceivedBgpAdvertisements() {
    return _receivedBgpAdvertisements;
  }

  Set<BgpAdvertisement> getSentBgpAdvertisements() {
    return _sentBgpAdvertisements;
  }

  public BgpMultipathRib getBgpMultipathRib() {
    return _bgpMultipathRib;
  }
}
