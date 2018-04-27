package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.dataplane.rib.AbstractRib.importRib;
import static org.batfish.dataplane.rib.RibDelta.importRibDelta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.Network;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSession;
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
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfAreaSummary;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfInternalRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfMetricType;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.dataplane.rib.BgpBestPathRib;
import org.batfish.dataplane.rib.BgpMultipathRib;
import org.batfish.dataplane.rib.ConnectedRib;
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

public class VirtualRouter extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  /** Route dependency tracker for BGP aggregate routes */
  private transient RouteDependencyTracker<BgpRoute, AbstractRoute> _bgpAggDeps =
      new RouteDependencyTracker<>();

  /** Builder for constructing {@link RibDelta} as pertains to the best-path BGP RIB */
  private transient RibDelta.Builder<BgpRoute> _bgpBestPathDeltaBuilder;

  /** Best-path BGP RIB */
  transient BgpBestPathRib _bgpBestPathRib;

  /** Incoming messages into this router from each bgp neighbor (indexed by prefix) */
  transient SortedMap<Prefix, Queue<RouteAdvertisement<AbstractRoute>>> _bgpIncomingRoutes;

  /** Builder for constructing {@link RibDelta} as pertains to the multipath BGP RIB */
  private transient RibDelta.Builder<BgpRoute> _bgpMultiPathDeltaBuilder;

  /** BGP multipath RIB */
  transient BgpMultipathRib _bgpMultipathRib;

  /** Parent configuration for this Virtual router */
  private final Configuration _c;

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

  /** FIB (forwarding information base) built from the main RIB */
  Fib _fib;

  /** RIB containing generated routes */
  private transient Rib _generatedRib;

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

  /** The finalized RIB, a combination different protocol RIBs */
  Rib _mainRib;

  /** Keeps track of changes to the main RIB */
  private transient RibDelta.Builder<AbstractRoute> _mainRibRouteDeltaBuiler;

  private transient Set<BgpSession> _newBgpSessions = new HashSet<>();

  transient OspfExternalType1Rib _ospfExternalType1Rib;

  transient OspfExternalType1Rib _ospfExternalType1StagingRib;

  transient OspfExternalType2Rib _ospfExternalType2Rib;

  transient OspfExternalType2Rib _ospfExternalType2StagingRib;

  private transient RibDelta.Builder<OspfExternalRoute> _ospfExternalDeltaBuiler;

  @VisibleForTesting
  transient SortedMap<Prefix, Queue<RouteAdvertisement<OspfExternalRoute>>>
      _ospfExternalIncomingRoutes;

  transient OspfInterAreaRib _ospfInterAreaRib;

  transient OspfInterAreaRib _ospfInterAreaStagingRib;

  transient OspfIntraAreaRib _ospfIntraAreaRib;

  transient OspfIntraAreaRib _ospfIntraAreaStagingRib;

  private transient Map<Prefix, VirtualRouter> _ospfNeighbors;

  transient OspfRib _ospfRib;

  /** Set of all valid OSPF external routes that we know about */
  private Map<Prefix, SortedSet<OspfExternalType1Route>> _receivedOspExternalType1Routes;

  private Map<Prefix, SortedSet<OspfExternalType2Route>> _receivedOspExternalType2Routes;

  /** Set of all valid BGP "advertisements" we have received */
  private Map<Prefix, SortedSet<BgpRoute>> _receivedBgpRoutes;

  Set<BgpAdvertisement> _receivedBgpAdvertisements;

  transient RipInternalRib _ripInternalRib;

  transient RipInternalRib _ripInternalStagingRib;

  transient RipRib _ripRib;

  Set<BgpAdvertisement> _sentBgpAdvertisements;

  transient StaticRib _staticInterfaceRib;

  transient StaticRib _staticRib;

  final Vrf _vrf;

  VirtualRouter(final String name, final Configuration c) {
    super(name);
    _c = c;
    _vrf = c.getVrfs().get(name);
    initRibs();
    // Keep track of sent and received advertisements
    _receivedBgpAdvertisements = new LinkedHashSet<>();
    _sentBgpAdvertisements = new LinkedHashSet<>();
    _receivedOspExternalType1Routes = new TreeMap<>();
    _receivedOspExternalType2Routes = new TreeMap<>();
    _receivedBgpRoutes = new TreeMap<>();
    _bgpIncomingRoutes = new TreeMap<>();
  }

  /**
   * Initializes helper data structures and easy-to-compute RIBs that are not affected by BDP
   * iterations (e.g., static route RIB, connected route RIB, etc.)
   */
  @VisibleForTesting
  void initForIgpComputation() {
    _mainRibRouteDeltaBuiler = new RibDelta.Builder<>(_mainRib);
    _bgpBestPathDeltaBuilder = new RibDelta.Builder<>(_bgpBestPathRib);
    _bgpMultiPathDeltaBuilder = new RibDelta.Builder<>(_bgpMultipathRib);

    initConnectedRib();
    initStaticRib();
    importRib(_independentRib, _connectedRib);
    importRib(_independentRib, _staticInterfaceRib);
    importRib(_mainRib, _independentRib);
    initIntraAreaOspfRoutes();
    initBaseRipRoutes();
  }

  /**
   * Prep for the Egp part of the computation
   *
   * @param externalAdverts a set of external BGP advertisements
   * @param allNodes map of all network nodes, keyed by hostname
   * @param bgpTopology the bgp peering relationships
   */
  void initForEgpComputation(
      Set<BgpAdvertisement> externalAdverts,
      final Map<String, Node> allNodes,
      Topology topology,
      Network<BgpNeighbor, BgpSession> bgpTopology) {
    initQueuesAndDeltaBuilders(allNodes, topology, bgpTopology);
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
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    // Initialize message queues for each BGP neighbor
    initBgpQueues(bgpTopology);

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

  void initBgpQueues(Network<BgpNeighbor, BgpSession> bgpTopology) {
    if (_vrf.getBgpProcess() == null) {
      _bgpIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _vrf.getBgpProcess()
          .getNeighbors()
          .values()
          .forEach(n -> _bgpIncomingRoutes.put(n.getPrefix(), new ConcurrentLinkedQueue<>()));
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

    // Loop over all generated routes and check whether any of the contributing routes have changed
    for (GeneratedRoute gr : _vrf.getGeneratedRoutes()) {
      boolean active = true;
      String generationPolicyName = gr.getGenerationPolicy();
      GeneratedRoute.Builder grb = new GeneratedRoute.Builder();
      grb.setNetwork(gr.getNetwork());
      grb.setAdmin(gr.getAdministrativeCost());
      grb.setMetric(gr.getMetric() != null ? gr.getMetric() : 0);
      grb.setAttributePolicy(gr.getAttributePolicy());
      grb.setGenerationPolicy(gr.getGenerationPolicy());
      boolean discard = gr.getDiscard();
      grb.setDiscard(discard);
      if (discard) {
        grb.setNextHopInterface(Interface.NULL_INTERFACE_NAME);
      }
      if (generationPolicyName != null) {
        RoutingPolicy generationPolicy = _c.getRoutingPolicies().get(generationPolicyName);
        if (generationPolicy != null) {
          active = false;
          for (AbstractRoute contributingCandidate : _mainRib.getRoutes()) {
            boolean accept =
                generationPolicy.process(contributingCandidate, grb, null, _key, Direction.OUT);
            if (accept) {
              if (!discard) {
                grb.setNextHopIp(contributingCandidate.getNextHopIp());
              }
              active = true;
              break;
            }
          }
        }
      }
      if (active) {
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
    _mainRibRouteDeltaBuiler.from(d);

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
   * Re-activate static routes at the beginning of an iteration. Directly adds a static route R to
   * the main RIB if there exists an active route to the R's next-hop-ip.
   */
  void activateStaticRoutes() {
    for (StaticRoute sr : _staticRib.getRoutes()) {
      // See if we have any routes matching this route's next hop IP
      Set<AbstractRoute> matchingRoutes = _mainRib.longestPrefixMatch(sr.getNextHopIp());
      Prefix staticRoutePrefix = sr.getNetwork();

      for (AbstractRoute matchingRoute : matchingRoutes) {
        Prefix matchingRoutePrefix = matchingRoute.getNetwork();
        /*
         * check to make sure matching route's prefix does not totally
         * contain this static route's prefix
         */
        if (!matchingRoutePrefix.containsPrefix(staticRoutePrefix)) {
          _mainRibRouteDeltaBuiler.from(_mainRib.mergeRouteGetDelta(sr));
          break; // break out of the inner loop but continue processing static routes
        }
      }
    }
  }

  /** Compute the FIB from the main RIB */
  public void computeFib() {
    _fib = new FibImpl(_mainRib);
  }

  /**
   * Decides whether the current OSPF summary route metric needs to be changed based on the given
   * route's metric.
   *
   * <p>Routes from the same area or outside of areaPrefix have no effect on the summary metric.
   *
   * @param route The route in question, whose metric is considered
   * @param areaPrefix The Ip prefix of the OSPF area
   * @param currentMetric The current summary metric for the area
   * @param areaNum Area number.
   * @param useMin Whether to use the older RFC 1583 computation, which takes the minimum of metrics
   *     as opposed to the newer RFC 2328, which uses the maximum
   * @return the newly computed summary metric.
   */
  @Nullable
  static Long computeUpdatedOspfSummaryMetric(
      OspfInternalRoute route,
      Prefix areaPrefix,
      @Nullable Long currentMetric,
      long areaNum,
      boolean useMin) {
    Prefix contributingRoutePrefix = route.getNetwork();
    // Only update metric for different areas and if the area prefix contains the route prefix
    if (areaNum == route.getArea() || !areaPrefix.containsPrefix(contributingRoutePrefix)) {
      return currentMetric;
    }
    long contributingRouteMetric = route.getMetric();
    // Definitely update if we have no previous metric
    if (currentMetric == null) {
      return contributingRouteMetric;
    }
    // Take the best metric between the route's and current available.
    // Routers (at least Cisco and Juniper) default to min metric unless using RFC2328 with
    // RFC1583 compatibility explicitly disabled, in which case they default to max.
    if (useMin) {
      return Math.min(currentMetric, contributingRouteMetric);
    }
    return Math.max(currentMetric, contributingRouteMetric);
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
    boolean useMin = MoreObjects.firstNonNull(proc.getRfc1583Compatible(), true);

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
                computeUpdatedOspfSummaryMetric(contributingRoute, prefix, metric, areaNum, useMin);
          }
          for (OspfInterAreaRoute contributingRoute : _ospfInterAreaRib.getRoutes()) {
            metric =
                computeUpdatedOspfSummaryMetric(contributingRoute, prefix, metric, areaNum, useMin);
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
      Network<BgpNeighbor, BgpSession> bgpTopology) {

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
      BgpNeighbor neighbor = _vrf.getBgpProcess().getNeighbors().get(srcPrefix);
      if (neighbor == null) {
        continue;
      }

      // Build a route based on the type of this advertisement
      BgpAdvertisementType type = advert.getType();
      BgpRoute.Builder outgoingRouteBuilder = new BgpRoute.Builder();
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
        proc.getMultipathEbgp(), proc.getMultipathIbgp(), deltas, allNodes, bgpTopology);
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
   * accepting advertisements less desirable than the local generated ones for the given network.
   */
  void initBgpAggregateRoutes() {
    // TODO: implement bgp aggregate routes in incremental manner
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
      BgpRoute.Builder b = new BgpRoute.Builder();
      b.setAdmin(gr.getAdministrativeCost());
      b.setAsPath(gr.getAsPath().getAsSets());
      b.setMetric(gr.getMetric());
      b.setSrcProtocol(RoutingProtocol.AGGREGATE);
      b.setProtocol(RoutingProtocol.AGGREGATE);
      b.setNetwork(gr.getNetwork());
      b.setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE);
      /* Note: Origin type and originator IP should get overwritten, but are needed initially */
      b.setOriginatorIp(_vrf.getBgpProcess().getRouterId());
      b.setOriginType(OriginType.INCOMPLETE);
      b.setReceivedFromIp(Ip.ZERO);
      BgpRoute br = b.build();

      /*
       * TODO: tests for this
       * 1. Really hope setNonRouting(true) prevents this from being in the main RIB
       * 2. General functionality of aggregate routes is not well tested
       */
      br.setNonRouting(true);
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
  void initRibs() {
    _connectedRib = new ConnectedRib(this);
    // If bgp process is null, doesn't matter
    MultipathEquivalentAsPathMatchMode mpTieBreaker =
        _vrf.getBgpProcess() == null
            ? EXACT_PATH
            : _vrf.getBgpProcess().getMultipathEquivalentAsPathMatchMode();
    _ebgpMultipathRib = new BgpMultipathRib(this, mpTieBreaker);
    _ebgpStagingRib = new BgpMultipathRib(this, mpTieBreaker);
    _generatedRib = new Rib(this);
    _ibgpMultipathRib = new BgpMultipathRib(this, mpTieBreaker);
    _ibgpStagingRib = new BgpMultipathRib(this, mpTieBreaker);
    _independentRib = new Rib(this);
    _mainRib = new Rib(this);
    _ospfExternalType1Rib = new OspfExternalType1Rib(this, _receivedOspExternalType1Routes);
    _ospfExternalType2Rib = new OspfExternalType2Rib(this, _receivedOspExternalType2Routes);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(this, null);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(this, null);
    _ospfInterAreaRib = new OspfInterAreaRib(this);
    _ospfInterAreaStagingRib = new OspfInterAreaRib(this);
    _ospfIntraAreaRib = new OspfIntraAreaRib(this);
    _ospfIntraAreaStagingRib = new OspfIntraAreaRib(this);
    _ospfRib = new OspfRib(this);
    _ripInternalRib = new RipInternalRib(this);
    _ripInternalStagingRib = new RipInternalRib(this);
    _ripRib = new RipRib(this);
    _staticRib = new StaticRib(this);
    _staticInterfaceRib = new StaticRib(this);
    _bgpMultipathRib = new BgpMultipathRib(this, mpTieBreaker);

    _ebgpMultipathRib = new BgpMultipathRib(this, mpTieBreaker);
    _ibgpMultipathRib = new BgpMultipathRib(this, mpTieBreaker);
    BgpTieBreaker tieBreaker =
        _vrf.getBgpProcess() == null
            ? BgpTieBreaker.ARRIVAL_ORDER
            : _vrf.getBgpProcess().getTieBreaker();
    _ebgpBestPathRib = new BgpBestPathRib(this, tieBreaker, null);
    _ibgpBestPathRib = new BgpBestPathRib(this, tieBreaker, null);
    _bgpBestPathRib = new BgpBestPathRib(this, tieBreaker, _receivedBgpRoutes);
  }

  /** Initialize the static route RIB from the VRF config. */
  void initStaticRib() {
    for (StaticRoute sr : _vrf.getStaticRoutes()) {
      String nextHopInt = sr.getNextHopInterface();
      if (!nextHopInt.equals(Route.UNSET_NEXT_HOP_INTERFACE)
          && !Interface.NULL_INTERFACE_NAME.equals(nextHopInt)
          && (_c.getInterfaces().get(nextHopInt) == null
              || !_c.getInterfaces().get(nextHopInt).getActive())) {
        continue;
      }
      // interface route
      if (sr.getNextHopIp().equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
        _staticInterfaceRib.mergeRouteGetDelta(sr);
      } else {
        _staticRib.mergeRouteGetDelta(sr);
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

    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      Ip localIp = neighbor.getLocalIp();
      Set<String> localIpOwners = ipOwners.get(localIp);
      String hostname = _c.getHostname();
      if (localIpOwners == null || !localIpOwners.contains(hostname)) {
        continue;
      }
      Prefix remotePrefix = neighbor.getPrefix();
      if (neighbor.getDynamic() || Ip.AUTO.equals(neighbor.getLocalIp())) {
        // Do not support dynamic outside neighbors
        continue;
      }
      Ip remoteIp = remotePrefix.getStartIp();
      if (ipOwners.get(remoteIp) != null) {
        // Skip if neighbor is not outside the network
        continue;
      }

      int localAs = neighbor.getLocalAs();
      int remoteAs = neighbor.getRemoteAs();
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
          if (!ebgpSession && routeOriginatorIp != null && remoteIp.equals(routeOriginatorIp)) {
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
          SortedSet<Integer> newAsPathElement = new TreeSet<>();
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
        if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
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
                remotePrefix,
                remoteVrfName,
                Direction.OUT);
        if (acceptOutgoing) {
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
    }
    return numAdvertisements;
  }

  /**
   * Process BGP messages from neighbors, return a list of delta changes to the RIBs
   *
   * @param ipOwners Mapping of IPs to a set of node names that own that IP
   * @param bgpTopology the bgp peering relationships
   * @return List of {@link RibDelta objects}
   */
  Map<BgpMultipathRib, RibDelta<BgpRoute>> processBgpMessages(
      Map<Ip, Set<String>> ipOwners, Network<BgpNeighbor, BgpSession> bgpTopology) {

    // If we have no BGP process, nothing to do
    if (_vrf.getBgpProcess() == null) {
      return null;
    }

    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    Map<BgpMultipathRib, RibDelta.Builder<BgpRoute>> ribDeltas = new IdentityHashMap<>();
    ribDeltas.put(_ebgpStagingRib, new Builder<>(_ebgpStagingRib));
    ribDeltas.put(_ibgpStagingRib, new Builder<>(_ibgpStagingRib));

    // Init default admin costs
    int ebgpAdminCost =
        RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int ibgpAdminCost =
        RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());

    // Process updates from each neighbor

    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      if (!bgpTopology.nodes().contains(neighbor)) {
        continue;
      }
      for (BgpSession session : bgpTopology.inEdges(neighbor)) {
        BgpNeighbor remoteBgpNeighbor = session.getSrc();

        // Check that we can process something from this neighbor
        if (remoteBgpNeighbor == null) {
          continue;
        }

        Ip localIp = neighbor.getLocalIp();
        Set<String> localIpOwners = ipOwners.get(localIp);
        String hostname = _c.getHostname();

        if (localIpOwners == null || !localIpOwners.contains(hostname)) {
          continue;
        }

        Queue<RouteAdvertisement<AbstractRoute>> queue =
            _bgpIncomingRoutes.get(neighbor.getPrefix());

        // Setup helper vars
        Configuration remoteConfig = remoteBgpNeighbor.getOwner();
        String remoteHostname = remoteConfig.getHostname();
        String remoteVrfName = remoteBgpNeighbor.getVrf();
        Vrf remoteVrf = remoteConfig.getVrfs().get(remoteVrfName);
        RoutingPolicy remoteExportPolicy =
            remoteConfig.getRoutingPolicies().get(remoteBgpNeighbor.getExportPolicy());
        int remoteAs = neighbor.getRemoteAs();
        boolean ebgpSession = session.isEbgp();
        BgpMultipathRib targetRib = ebgpSession ? _ebgpStagingRib : _ibgpStagingRib;
        RoutingProtocol targetProtocol = ebgpSession ? RoutingProtocol.BGP : RoutingProtocol.IBGP;

        // Process all candidate routes and queue outgoing messages
        while (queue.peek() != null) {
          RouteAdvertisement<AbstractRoute> remoteRouteAdvert = queue.remove();
          AbstractRoute remoteRoute = remoteRouteAdvert.getRoute();
          BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder();
          RoutingProtocol remoteRouteProtocol = remoteRoute.getProtocol();
          boolean remoteRouteIsBgp =
              remoteRouteProtocol == RoutingProtocol.IBGP
                  || remoteRouteProtocol == RoutingProtocol.BGP;

          // originatorIP
          Ip originatorIp;
          if (!ebgpSession && remoteRouteProtocol == RoutingProtocol.IBGP) {
            BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
            originatorIp = bgpRemoteRoute.getOriginatorIp();
          } else {
            originatorIp = remoteVrf.getBgpProcess().getRouterId();
          }
          transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);
          transformedOutgoingRouteBuilder.setReceivedFromIp(remoteBgpNeighbor.getLocalIp());
          // note whether new route is received from route reflector client
          transformedOutgoingRouteBuilder.setReceivedFromRouteReflectorClient(
              !ebgpSession && neighbor.getRouteReflectorClient());

          /*
           * clusterList, receivedFromRouteReflectorClient, (originType
           * for bgp remote route)
           */
          if (remoteRouteIsBgp) {
            BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
            transformedOutgoingRouteBuilder.setOriginType(bgpRemoteRoute.getOriginType());
            if (ebgpSession
                && bgpRemoteRoute.getAsPath().containsAs(remoteBgpNeighbor.getRemoteAs())
                && !remoteBgpNeighbor.getAllowRemoteAsOut()) {
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

            Ip remoteOriginatorIp = bgpRemoteRoute.getOriginatorIp();

            /*
             *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
             *  same as originator id of advertisement
             */
            if (!ebgpSession
                && remoteOriginatorIp != null
                && _vrf.getBgpProcess().getRouterId().equals(remoteOriginatorIp)) {
              continue;
            }
            if (remoteRouteProtocol == RoutingProtocol.IBGP && !ebgpSession) {
              /*
               *  The remote route is iBGP. The session is iBGP. We consider whether to reflect, and
               *  modify the outgoing route as appropriate.
               */
              boolean remoteRouteReceivedFromRouteReflectorClient =
                  bgpRemoteRoute.getReceivedFromRouteReflectorClient();
              boolean sendingToRouteReflectorClient = remoteBgpNeighbor.getRouteReflectorClient();

              Ip remoteReceivedFromIp = bgpRemoteRoute.getReceivedFromIp();
              boolean remoteRouteOriginatedByRemoteNeighbor = remoteReceivedFromIp.equals(Ip.ZERO);
              if (!remoteRouteReceivedFromRouteReflectorClient
                  && !sendingToRouteReflectorClient
                  && !remoteRouteOriginatedByRemoteNeighbor) {
                /*
                 * Neither reflecting nor originating this iBGP route, so don't send
                 */
                continue;
              }
              transformedOutgoingRouteBuilder
                  .getClusterList()
                  .addAll(bgpRemoteRoute.getClusterList());
              if (!remoteRouteOriginatedByRemoteNeighbor) {
                // we are reflecting, so we need to get the clusterid associated with the
                // remoteRoute
                BgpNeighbor remoteReceivedFromSession =
                    remoteVrf
                        .getBgpProcess()
                        .getNeighbors()
                        .get(new Prefix(remoteReceivedFromIp, Prefix.MAX_PREFIX_LENGTH));
                long newClusterId = remoteReceivedFromSession.getClusterId();
                transformedOutgoingRouteBuilder.getClusterList().add(newClusterId);
              }
              Set<Long> localClusterIds = _vrf.getBgpProcess().getClusterIds();
              Set<Long> outgoingClusterList = transformedOutgoingRouteBuilder.getClusterList();
              if (localClusterIds.stream().anyMatch(outgoingClusterList::contains)) {
                /*
                 *  receiver will reject new route if it contains any of its local cluster ids
                 */
                continue;
              }
            }
          }

          // Outgoing asPath
          // Outgoing communities
          if (remoteRouteIsBgp) {
            BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
            transformedOutgoingRouteBuilder.setAsPath(bgpRemoteRoute.getAsPath().getAsSets());
            if (remoteBgpNeighbor.getSendCommunity()) {
              transformedOutgoingRouteBuilder
                  .getCommunities()
                  .addAll(bgpRemoteRoute.getCommunities());
            }
          }
          if (ebgpSession) {
            SortedSet<Integer> newAsPathElement = new TreeSet<>();
            newAsPathElement.add(remoteAs);
            transformedOutgoingRouteBuilder.getAsPath().add(0, newAsPathElement);
          }

          // Outgoing protocol
          transformedOutgoingRouteBuilder.setProtocol(targetProtocol);
          transformedOutgoingRouteBuilder.setNetwork(remoteRoute.getNetwork());

          // Outgoing metric
          if (remoteRouteIsBgp) {
            transformedOutgoingRouteBuilder.setMetric(remoteRoute.getMetric());
          }

          // Outgoing nextHopIp
          // Outgoing localPreference
          Ip nextHopIp;
          int localPreference;
          if (ebgpSession || !remoteRouteIsBgp) {
            nextHopIp = remoteBgpNeighbor.getLocalIp();
            localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
          } else {
            nextHopIp = remoteRoute.getNextHopIp();
            BgpRoute remoteIbgpRoute = (BgpRoute) remoteRoute;
            localPreference = remoteIbgpRoute.getLocalPreference();
          }
          if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
            // should only happen for ibgp
            String nextHopInterface = remoteRoute.getNextHopInterface();
            InterfaceAddress nextHopAddress =
                remoteVrf.getInterfaces().get(nextHopInterface).getAddress();
            if (nextHopAddress == null) {
              throw new BatfishException("remote route's nextHopInterface has no address");
            }
            nextHopIp = nextHopAddress.getIp();
          }
          transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
          transformedOutgoingRouteBuilder.setLocalPreference(localPreference);

          // Outgoing srcProtocol
          transformedOutgoingRouteBuilder.setSrcProtocol(remoteRoute.getProtocol());

          /*
           * CREATE OUTGOING ROUTE
           */
          boolean acceptOutgoing =
              remoteExportPolicy.process(
                  remoteRoute,
                  transformedOutgoingRouteBuilder,
                  localIp,
                  remoteBgpNeighbor.getPrefix(),
                  remoteVrfName,
                  Direction.OUT);
          if (acceptOutgoing) {
            BgpRoute transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
            // Record sent advertisement
            BgpAdvertisementType sentType =
                ebgpSession ? BgpAdvertisementType.EBGP_SENT : BgpAdvertisementType.IBGP_SENT;
            Ip sentReceivedFromIp = transformedOutgoingRoute.getReceivedFromIp();
            Ip sentOriginatorIp = transformedOutgoingRoute.getOriginatorIp();
            SortedSet<Long> sentClusterList =
                ImmutableSortedSet.copyOf(transformedOutgoingRoute.getClusterList());
            boolean sentReceivedFromRouteReflectorClient =
                transformedOutgoingRoute.getReceivedFromRouteReflectorClient();
            AsPath sentAsPath = transformedOutgoingRoute.getAsPath();
            SortedSet<Long> sentCommunities =
                ImmutableSortedSet.copyOf(transformedOutgoingRoute.getCommunities());
            Prefix sentNetwork = remoteRoute.getNetwork();
            Ip sentNextHopIp;
            String sentSrcNode = remoteHostname;
            String sentSrcVrf = remoteVrfName;
            Ip sentSrcIp = remoteBgpNeighbor.getLocalIp();
            String sentDstNode = hostname;
            String sentDstVrf = _vrf.getName();
            Ip sentDstIp = neighbor.getLocalIp();
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
            BgpRoute.Builder transformedIncomingRouteBuilder = new BgpRoute.Builder();

            // Incoming originatorIp
            transformedIncomingRouteBuilder.setOriginatorIp(sentOriginatorIp);

            // Incoming receivedFromIp
            transformedIncomingRouteBuilder.setReceivedFromIp(sentReceivedFromIp);

            // Incoming clusterList
            transformedIncomingRouteBuilder.getClusterList().addAll(sentClusterList);

            // Incoming receivedFromRouteReflectorClient
            transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
                sentReceivedFromRouteReflectorClient);

            // Incoming asPath
            transformedIncomingRouteBuilder.setAsPath(sentAsPath.getAsSets());

            // Incoming communities
            transformedIncomingRouteBuilder.getCommunities().addAll(sentCommunities);

            // Incoming protocol
            transformedIncomingRouteBuilder.setProtocol(targetProtocol);

            // Incoming network
            transformedIncomingRouteBuilder.setNetwork(sentNetwork);

            // Incoming nextHopIp
            transformedIncomingRouteBuilder.setNextHopIp(sentNextHopIp);

            // Incoming localPreference
            transformedIncomingRouteBuilder.setLocalPreference(sentLocalPreference);

            // Incoming admin
            int admin = ebgpSession ? ebgpAdminCost : ibgpAdminCost;
            transformedIncomingRouteBuilder.setAdmin(admin);

            // Incoming metric
            transformedIncomingRouteBuilder.setMetric(sentMed);

            // Incoming originType
            transformedIncomingRouteBuilder.setOriginType(sentOriginType);

            // Incoming srcProtocol
            transformedIncomingRouteBuilder.setSrcProtocol(sentSrcProtocol);
            String importPolicyName = neighbor.getImportPolicy();
            // TODO: ensure there is always an import policy

            if (transformedOutgoingRoute.getAsPath().containsAs(neighbor.getLocalAs())
                && !neighbor.getAllowLocalAsIn()) {
              // skip routes containing peer's AS unless
              // disable-peer-as-check (getAllowRemoteAsOut) is set
              continue;
            }

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
                        remoteBgpNeighbor.getLocalIp(),
                        remoteBgpNeighbor.getPrefix(),
                        _key,
                        Direction.IN);
              }
            }
            if (acceptIncoming) {
              BgpRoute transformedIncomingRoute = transformedIncomingRouteBuilder.build();
              BgpAdvertisementType receivedType =
                  ebgpSession
                      ? BgpAdvertisementType.EBGP_RECEIVED
                      : BgpAdvertisementType.IBGP_RECEIVED;
              Prefix receivedNetwork = sentNetwork;
              Ip receivedNextHopIp = sentNextHopIp;
              String receivedSrcNode = sentSrcNode;
              String receivedSrcVrf = sentSrcVrf;
              Ip receivedSrcIp = sentSrcIp;
              String receivedDstNode = sentDstNode;
              String receivedDstVrf = sentDstVrf;
              Ip receivedDstIp = sentDstIp;
              RoutingProtocol receivedSrcProtocol = sentSrcProtocol;
              OriginType receivedOriginType = transformedIncomingRoute.getOriginType();
              int receivedLocalPreference = transformedIncomingRoute.getLocalPreference();
              long receivedMed = transformedIncomingRoute.getMetric();
              Ip receivedOriginatorIp = sentOriginatorIp;
              AsPath receivedAsPath = transformedIncomingRoute.getAsPath();
              SortedSet<Long> receivedCommunities =
                  ImmutableSortedSet.copyOf(transformedIncomingRoute.getCommunities());
              SortedSet<Long> receivedClusterList = ImmutableSortedSet.copyOf(sentClusterList);
              int receivedWeight = transformedIncomingRoute.getWeight();
              BgpAdvertisement receivedAdvert =
                  new BgpAdvertisement(
                      receivedType,
                      receivedNetwork,
                      receivedNextHopIp,
                      receivedSrcNode,
                      receivedSrcVrf,
                      receivedSrcIp,
                      receivedDstNode,
                      receivedDstVrf,
                      receivedDstIp,
                      receivedSrcProtocol,
                      receivedOriginType,
                      receivedLocalPreference,
                      receivedMed,
                      receivedOriginatorIp,
                      receivedAsPath,
                      receivedCommunities,
                      receivedClusterList,
                      receivedWeight);

              if (remoteRouteAdvert.isWithdrawn()) {
                // Note this route was removed
                ribDeltas.get(targetRib).remove(transformedIncomingRoute, Reason.WITHDRAW);
                SortedSet<BgpRoute> b =
                    _receivedBgpRoutes.get(transformedIncomingRoute.getNetwork());
                if (b != null) {
                  b.remove(transformedIncomingRoute);
                }
              } else {
                // Merge into staging rib, note delta
                ribDeltas
                    .get(targetRib)
                    .from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
                _receivedBgpAdvertisements.add(receivedAdvert);
                _receivedBgpRoutes
                    .computeIfAbsent(transformedIncomingRoute.getNetwork(), k -> new TreeSet<>())
                    .add(transformedIncomingRoute);
              }
            }
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

  private static boolean isOspfInterAreaFromInterAreaPropagationAllowed(
      long areaNum, Node neighbor, OspfInternalRoute neighborRoute, OspfArea neighborArea) {
    long neighborRouteAreaNum = neighborRoute.getArea();
    // May only propagate to or from area 0
    if (areaNum != neighborRouteAreaNum && areaNum != 0L && neighborRouteAreaNum != 0L) {
      return false;
    }
    Prefix neighborRouteNetwork = neighborRoute.getNetwork();
    String neighborSummaryFilterName = neighborArea.getSummaryFilter();
    boolean hasSummaryFilter = neighborSummaryFilterName != null;
    boolean allowed = !hasSummaryFilter;

    // If there is a summary filter, run the route through it
    if (hasSummaryFilter) {
      RouteFilterList neighborSummaryFilter =
          neighbor.getConfiguration().getRouteFilterLists().get(neighborSummaryFilterName);
      allowed = neighborSummaryFilter.permits(neighborRouteNetwork);
    }
    return allowed;
  }

  private static boolean isOspfInterAreaFromIntraAreaPropagationAllowed(
      long areaNum, Node neighbor, OspfInternalRoute neighborRoute, OspfArea neighborArea) {
    long neighborRouteAreaNum = neighborRoute.getArea();
    // May only propagate to or from area 0
    if (areaNum == neighborRouteAreaNum || (areaNum != 0L && neighborRouteAreaNum != 0L)) {
      return false;
    }
    Prefix neighborRouteNetwork = neighborRoute.getNetwork();
    String neighborSummaryFilterName = neighborArea.getSummaryFilter();
    boolean hasSummaryFilter = neighborSummaryFilterName != null;
    boolean allowed = !hasSummaryFilter;

    // If there is a summary filter, run the route through it
    if (hasSummaryFilter) {
      RouteFilterList neighborSummaryFilter =
          neighbor.getConfiguration().getRouteFilterLists().get(neighborSummaryFilterName);
      allowed = neighborSummaryFilter.permits(neighborRouteNetwork);
    }
    return allowed;
  }

  boolean propagateOspfInterAreaRouteFromIntraAreaRoute(
      Node neighbor,
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    return isOspfInterAreaFromIntraAreaPropagationAllowed(
            areaNum, neighbor, neighborRoute, neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getIp(),
            incrementalCost,
            adminCost,
            areaNum);
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
    Long areaNum = area.getName();
    VirtualRouter neighborVirtualRouter =
        neighbor.getVirtualRouters().get(neighborInterface.getVrfName());
    boolean changed = false;
    for (OspfIntraAreaRoute neighborRoute : neighborVirtualRouter._ospfIntraAreaRib.getRoutes()) {
      changed |=
          propagateOspfIntraAreaRoute(
              neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
      changed |=
          propagateOspfInterAreaRouteFromIntraAreaRoute(
              neighbor, neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
    }
    for (OspfInterAreaRoute neighborRoute : neighborVirtualRouter._ospfInterAreaRib.getRoutes()) {
      changed |=
          propagateOspfInterAreaRouteFromInterAreaRoute(
              neighbor, neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
    }
    return changed;
  }

  boolean propagateOspfInterAreaRouteFromInterAreaRoute(
      Node neighbor,
      OspfInterAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    return isOspfInterAreaFromInterAreaPropagationAllowed(
            areaNum, neighbor, neighborRoute, neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getIp(),
            incrementalCost,
            adminCost,
            areaNum);
  }

  boolean propagateOspfIntraAreaRoute(
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    long newCost = neighborRoute.getMetric() + incrementalCost;
    Ip nextHopIp = neighborInterface.getAddress().getIp();
    OspfIntraAreaRoute newRoute =
        new OspfIntraAreaRoute(neighborRoute.getNetwork(), nextHopIp, adminCost, newCost, areaNum);
    return neighborRoute.getArea() == areaNum && (_ospfIntraAreaStagingRib.mergeRoute(newRoute));
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

  /**
   * Queue advertised BGP routes to all BGP neighbors.
   *
   * @param bgpBestPathDelta a {@link RibDelta} indicating what changed in the {@link
   *     #_bgpBestPathRib}
   * @param ebgpBestPathDelta {@link RibDelta} indicating what changed in the {@link
   *     #_bgpBestPathRib}
   * @param bgpMultiPathDelta a {@link RibDelta} indicating what changed in the {@link
   *     #_bgpMultipathRib}
   * @param mainDelta a {@link RibDelta} indicating what changed in the {@link #_mainRib}
   * @param allNodes map of all nodes in the network, keyed by hostname
   * @param bgpTopology the bgp peering relationships
   */
  private void queueOutgoingBgpRoutes(
      @Nullable RibDelta<BgpRoute> bgpBestPathDelta,
      RibDelta<BgpRoute> ebgpBestPathDelta,
      @Nullable RibDelta<BgpRoute> bgpMultiPathDelta,
      @Nullable RibDelta<AbstractRoute> mainDelta,
      final Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {
    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      if (!bgpTopology.nodes().contains(neighbor)) {
        continue;
      }
      for (BgpSession session : bgpTopology.incidentEdges(neighbor)) {
        BgpNeighbor remoteNeighbor = session.getSrc();
        if (neighbor == remoteNeighbor) {
          continue;
        }

        // Queue for this neighbor
        VirtualRouter remoteVirtualRouter = getRemoteBgpNeighborVR(session, allNodes);
        if (remoteVirtualRouter == null) {
          continue;
        }

        Queue<RouteAdvertisement<AbstractRoute>> queue =
            remoteVirtualRouter._bgpIncomingRoutes.get(remoteNeighbor.getPrefix());

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
        queueDelta(queue, finalBuilder.build());
      }
    }
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
          (key, remoteVR) -> {
            // Get remote neighbor's queue by prefix
            Queue<RouteAdvertisement<OspfExternalRoute>> q =
                remoteVR._ospfExternalIncomingRoutes.get(key);
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
      Network<BgpNeighbor, BgpSession> bgpTopology) {

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
        _bgpBestPathDeltaBuilder.build(),
        ebgpBestPathDelta,
        _bgpMultiPathDeltaBuilder.build(),
        _mainRibRouteDeltaBuiler.build(),
        allNodes,
        bgpTopology);
  }

  static Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> syncBgpDeltaPropagation(
      BgpBestPathRib bestPathRib, BgpMultipathRib multiPathRib, RibDelta<BgpRoute> delta) {

    // Build our fist attempt at best path delta
    Builder<BgpRoute> bestDeltaBuldiler = new Builder<>(bestPathRib);
    bestDeltaBuldiler.from(importRibDelta(bestPathRib, delta));
    RibDelta<BgpRoute> bestDelta = bestDeltaBuldiler.build();

    Builder<BgpRoute> mpBuilder = new Builder<>(multiPathRib);

    mpBuilder.from(importRibDelta(multiPathRib, bestDelta));
    if (bestDelta != null) {
      /*
       * Handle mods to the best path RIB
       */
      for (Prefix p : bestDelta.getPrefixes()) {
        List<RouteAdvertisement<BgpRoute>> actions = bestDelta.getActions(p);
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
            bestDeltaBuldiler.from(importRibDelta(bestPathRib, fakeDelta));
          }
        }
      }
    }
    // Set the (possibly updated) best path delta
    bestDelta = bestDeltaBuldiler.build();
    // Update best paths
    multiPathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Only iterate over valid prefixes (ones in best-path RIB) and see if anything should go into
    // multi-path RIB
    for (Prefix p : bestPathRib.getPrefixes()) {
      mpBuilder.from(importRibDelta(multiPathRib, delta, p));
    }
    return new SimpleImmutableEntry<>(bestDelta, mpBuilder.build());
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
  void reinitForNewIteration(final Map<String, Node> allNodes) {
    _mainRibRouteDeltaBuiler = new Builder<>(_mainRib);
    _bgpBestPathDeltaBuilder = new RibDelta.Builder<>(_bgpBestPathRib);
    _bgpMultiPathDeltaBuilder = new RibDelta.Builder<>(_bgpMultipathRib);
    _ospfExternalDeltaBuiler = new RibDelta.Builder<>(null);

    // Initial round of messages to the new neighbors
    for (BgpSession bgpSession : _newBgpSessions) {
      newBgpSessionEstablishedHook(bgpSession, allNodes);
    }
    _newBgpSessions.clear();

    /*
     * RIBs not read from can just be re-initialized
     */
    _ospfRib = new OspfRib(this);
    _ripRib = new RipRib(this);

    /*
     * Staging RIBs can also be re-initialized
     */
    MultipathEquivalentAsPathMatchMode mpTieBreaker =
        _vrf.getBgpProcess() == null
            ? EXACT_PATH
            : _vrf.getBgpProcess().getMultipathEquivalentAsPathMatchMode();
    _ebgpStagingRib = new BgpMultipathRib(this, mpTieBreaker);
    _ibgpStagingRib = new BgpMultipathRib(this, mpTieBreaker);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(this, null);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(this, null);

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
   * @return true if all queues are empty
   */
  public boolean hasProcessedAllMessages(Network<BgpNeighbor, BgpSession> bgpTopology) {
    // Check the BGP message queues
    if (_vrf.getBgpProcess() != null) {
      for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
        Queue<RouteAdvertisement<AbstractRoute>> queue =
            _bgpIncomingRoutes.get(neighbor.getPrefix());
        if (!queue.isEmpty()) {
          return false;
        }
      }
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
    return true;
  }

  /**
   * Queues initial round of outgoing BGP messages based on the state of the RIBs prior to any data
   * plane iterations.
   *
   * @param allNodes all nodes in the network
   */
  public void queueInitialBgpMessages(
      final Network<BgpNeighbor, BgpSession> bgpTopology, final Map<String, Node> allNodes) {
    if (_vrf.getBgpProcess() == null) {
      // nothing to do
      return;
    }
    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      if (!bgpTopology.nodes().contains(neighbor)) {
        continue;
      }
      for (BgpSession session : bgpTopology.inEdges(neighbor)) {
        newBgpSessionEstablishedHook(session, allNodes);
      }
    }
  }

  void newBgpSessionEstablishedHook(BgpSession session, Map<String, Node> allNodes) {
    VirtualRouter remoteVr = getRemoteBgpNeighborVR(session, allNodes);
    if (remoteVr == null) {
      return;
    }

    Queue<RouteAdvertisement<AbstractRoute>> queue =
        remoteVr._bgpIncomingRoutes.get(session.getSrc().getPrefix());
    if (queue == null) {
      return;
    }

    /*
     * Add route advertisement.
     * Note: export filtering is done in the processBgpMessages, here we queue everything from the
     * main RIB
     */
    for (AbstractRoute route : _mainRib.getRoutes()) {
      queue.add(new RouteAdvertisement<>(route));
    }
  }

  /**
   * Lookup the VirtualRouter owner of a remote BGP neighbor.
   *
   * @param session the {@link BgpSession}, where our end of the connection is {@code
   *     session.getDst()}
   * @param allNodes map containing all network nodes
   * @return a {@link VirtualRouter} that owns the {@code neighbor.getRemoteBgpNeighbor()}
   */
  @Nullable
  @VisibleForTesting
  static VirtualRouter getRemoteBgpNeighborVR(
      @Nonnull BgpSession session, final Map<String, Node> allNodes) {

    BgpNeighbor remoteBgpNeighbor = session.getSrc();
    String remoteHostname = remoteBgpNeighbor.getOwner().getHostname();
    String remoteVrfName = remoteBgpNeighbor.getVrf();
    return allNodes.get(remoteHostname).getVirtualRouters().get(remoteVrfName);
  }

  /**
   * This method aids in de-serialization of {@link VirtualRouter}
   *
   * @param in input stream to de-serialize from
   * @throws IOException if processing the stream fails
   * @throws ClassNotFoundException if deserialization cannot complete due to an unknown class
   */
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _bgpIncomingRoutes = null;
  }

  /**
   * Compute our OSPF neighbors.
   *
   * @param allNodes map of all network nodes, keyed by hostname
   * @param topology the Layer-3 network topology
   * @return A sorted map of neighbor prefixes to their names
   */
  @Nullable
  SortedMap<Prefix, VirtualRouter> getOspfNeighbors(
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

    SortedMap<Prefix, VirtualRouter> neighbors = new TreeMap<>();
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
        neighbors.put(connectingInterface.getAddress().getPrefix(), neighborVirtualRouter);
      }
    }

    return ImmutableSortedMap.copyOf(neighbors);
  }

  public Configuration getConfiguration() {
    return _c;
  }

  public ConnectedRib getConnectedRib() {
    return _connectedRib;
  }

  public Fib getFib() {
    return _fib;
  }

  public Rib getMainRib() {
    return _mainRib;
  }

  public BgpBestPathRib getBgpBestPathRib() {
    return _bgpBestPathRib;
  }

  /** Convenience method to get the VirtualRouter's hostname */
  String getHostname() {
    return _c.getHostname();
  }

  /**
   * Compute the "hashcode" of this router for the iBDP purposes. The hashcode is computed from the
   * following datastructures:
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
            .sum();
  }
}
