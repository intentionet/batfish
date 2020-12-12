package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.common.util.CollectionUtil.toOrderedHashCode;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.routing_policy.Environment.Direction.IN;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRouteOnImport;
import static org.batfish.dataplane.rib.RibDelta.importDeltaToBuilder;
import static org.batfish.dataplane.rib.RibDelta.importRibDelta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.graph.ValueGraph;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EvpnRoute;
import org.batfish.datamodel.EvpnType3Route;
import org.batfish.datamodel.EvpnType5Route;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GenericRibReadOnly;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.AddressFamily;
import org.batfish.datamodel.bgp.AddressFamily.Type;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.dataplane.rib.RibGroup;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.dataplane.protocols.BgpProtocolHelper;
import org.batfish.dataplane.protocols.GeneratedRouteHelper;
import org.batfish.dataplane.rib.BgpRib;
import org.batfish.dataplane.rib.Bgpv4Rib;
import org.batfish.dataplane.rib.EvpnRib;
import org.batfish.dataplane.rib.Rib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/**
 * BGP routing process. A dataplane counter-part of {@link BgpProcess}. Maintains state necessary
 * for exchange of BGP routing messages.
 */
@ParametersAreNonnullByDefault
final class BgpRoutingProcess implements RoutingProcess<BgpTopology, BgpRoute<?, ?>> {
  /** Configuration for this process */
  @Nonnull private final BgpProcess _process;
  /** Parent node configuration */
  @Nonnull private final Configuration _c;
  /** Name of our VRF */
  @Nonnull private final String _vrfName;
  /** Reference to the parent {@link VirtualRouter} main RIB (read-only). */
  @Nonnull private final GenericRibReadOnly<AnnotatedRoute<AbstractRoute>> _mainRib;
  /** Current BGP topology */
  @Nonnull private BgpTopology _topology;
  /** Metadata about propagated prefixes to/from neighbors */
  @Nonnull private PrefixTracer _prefixTracer;

  /** Route dependency tracker for BGP IPv4 aggregate routes */
  @Nonnull
  RouteDependencyTracker<Bgpv4Route, AbstractRoute> _bgpAggDeps = new RouteDependencyTracker<>();
  /** All BGP neighbor that speaks IPv4 unicast address family that we know of */
  @Nonnull ImmutableSortedSet<EdgeId> _bgpv4Edges;
  /**
   * Incoming EVPN type 3 advertisements into this router from each BGP neighbor that speaks EVPN
   * address family
   */
  @Nonnull @VisibleForTesting
  SortedMap<EdgeId, Queue<RouteAdvertisement<EvpnType3Route>>> _evpnType3IncomingRoutes;

  // RIBs and RIB delta builders
  /** Helper RIB containing all paths obtained with external BGP, for IPv4 unicast */
  @Nonnull Bgpv4Rib _ebgpv4Rib;
  /** RIB containing paths obtained with iBGP, for IPv4 unicast */
  @Nonnull Bgpv4Rib _ibgpv4Rib;

  // outgoing RIB deltas for the current round (i.e., deltas generated in the previous round)
  @Nonnull private RibDelta<Bgpv4Route> _ebgpv4DeltaPrev = RibDelta.empty();
  @Nonnull private RibDelta<Bgpv4Route> _bgpv4DeltaPrev = RibDelta.empty();

  /**
   * Helper RIB containing paths obtained with iBGP during current iteration. An Adj-RIB-in of sorts
   * for IPv4 unicast
   */
  /** Combined BGP (both iBGP and eBGP) RIB, for IPv4 unicast */
  @Nonnull Bgpv4Rib _bgpv4Rib;
  /** Builder for constructing {@link RibDelta} which represent changes to {@link #_bgpv4Rib} */
  @Nonnull private Builder<Bgpv4Route> _bgpv4DeltaBuilder;
  /** {@link RibDelta} representing changes to {@link #_ebgpv4Rib} in the current iteration */
  @Nonnull private RibDelta<Bgpv4Route> _ebgpv4DeltaCurrent;

  /** eBGP RIB for EVPN type 3 routes */
  @Nonnull private EvpnRib<EvpnType3Route> _ebgpType3EvpnRib;
  /** iBGP RIB for EVPN type 3 routes */
  @Nonnull private EvpnRib<EvpnType3Route> _ibgpType3EvpnRib;
  /** Combined RIB for EVPN type 3 routes */
  @Nonnull private EvpnRib<EvpnType3Route> _evpnType3Rib;
  /** eBGP RIB for EVPN type 5 routes */
  @Nonnull private EvpnRib<EvpnType5Route> _ebgpType5EvpnRib;
  /** iBGP RIB for EVPN type 5 routes */
  @Nonnull private EvpnRib<EvpnType5Route> _ibgpType5EvpnRib;
  /** Combined RIB for EVPN type 5 routes */
  @Nonnull private EvpnRib<EvpnType5Route> _evpnType5Rib;

  /** Combined EVPN RIB for e/iBGP across all route types */
  @Nonnull EvpnRib<EvpnRoute<?, ?>> _evpnRib;
  /** Builder for constructing {@link RibDelta} for routes in {@link #_evpnRib} */
  @Nonnull private Builder<EvpnRoute<?, ?>> _evpnDeltaBuilder = RibDelta.builder();

  /** Keep track of EVPN type 3 routes initialized from our own VNI settings */
  @Nonnull private RibDelta<EvpnType3Route> _evpnInitializationDelta;

  /** Delta builder for routes that must be propagated to the main RIB */
  @Nonnull private RibDelta.Builder<BgpRoute<?, ?>> _toMainRib = RibDelta.builder();

  /* Indicates whether this BGP process has been initialized. */
  private boolean _initialized = false;

  /**
   * Mapping from extended community route target patterns to VRF name. Used for determining where
   * to merge EVPN routes
   */
  @Nonnull private final Map<String, String> _rtVrfMapping;

  /** Mapping of routes to be redistributed. Maps source VRF to a set of routes to process */
  @Nonnull private Map<String, RibDelta<? extends AnnotatedRoute<AbstractRoute>>> _toRedistribute;

  /** Set of edges (sessions) that came up since previous topology update */
  private Set<EdgeId> _edgesWentUp = ImmutableSet.of();
  /**
   * Type 3 routes that were created locally (across all VRFs). Save them so that if new sessions
   * come up, we can easily send out the updates
   */
  @Nonnull private RibDelta<EvpnType3Route> _localType3Routes = RibDelta.empty();

  private static final Logger LOGGER = LogManager.getLogger(BgpRoutingProcess.class);

  /**
   * Create a new BGP process
   *
   * @param process the {@link BgpProcess} -- configuration for this routing process
   * @param configuration the parent {@link Configuration}
   * @param vrfName name of the VRF this process is in
   * @param mainRib take in a reference to MainRib for read-only use (e.g., getting IGP cost to
   */
  BgpRoutingProcess(
      BgpProcess process,
      Configuration configuration,
      String vrfName,
      Rib mainRib,
      BgpTopology topology,
      PrefixTracer prefixTracer) {
    _process = process;
    _c = configuration;
    _vrfName = vrfName;
    _mainRib = mainRib;
    _topology = topology;
    _prefixTracer = prefixTracer;

    // Message queues start out empty
    _bgpv4Edges = ImmutableSortedSet.of();
    _evpnType3IncomingRoutes = ImmutableSortedMap.of();

    // Initialize all RIBs
    BgpTieBreaker bestPathTieBreaker =
        firstNonNull(_process.getTieBreaker(), BgpTieBreaker.ARRIVAL_ORDER);
    MultipathEquivalentAsPathMatchMode multiPathMatchMode =
        firstNonNull(_process.getMultipathEquivalentAsPathMatchMode(), EXACT_PATH);
    boolean clusterListAsIgpCost = _process.getClusterListAsIgpCost();
    _ebgpv4Rib =
        new Bgpv4Rib(
            _mainRib,
            bestPathTieBreaker,
            _process.getMultipathEbgp() ? null : 1,
            multiPathMatchMode,
            true,
            clusterListAsIgpCost);
    _ibgpv4Rib =
        new Bgpv4Rib(
            _mainRib,
            bestPathTieBreaker,
            _process.getMultipathIbgp() ? null : 1,
            multiPathMatchMode,
            true,
            clusterListAsIgpCost);
    _bgpv4Rib =
        new Bgpv4Rib(
            _mainRib,
            bestPathTieBreaker,
            _process.getMultipathEbgp() || _process.getMultipathIbgp() ? null : 1,
            multiPathMatchMode,
            true,
            clusterListAsIgpCost);
    _bgpv4DeltaBuilder = RibDelta.builder();

    _toRedistribute = new HashMap<>(1);

    // EVPN Ribs
    _ebgpType3EvpnRib =
        new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, clusterListAsIgpCost);
    _ibgpType3EvpnRib =
        new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, clusterListAsIgpCost);
    _evpnType3Rib =
        new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, clusterListAsIgpCost);
    /*
     TODO: type5 RIBs are currently unused. Correct implementation blocked on having local bgp
       ribs
    */
    _ebgpType5EvpnRib =
        new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, clusterListAsIgpCost);
    _ibgpType5EvpnRib =
        new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, clusterListAsIgpCost);
    _evpnType5Rib =
        new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, clusterListAsIgpCost);
    _evpnRib =
        new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, clusterListAsIgpCost);
    _evpnInitializationDelta = RibDelta.empty();
    _rtVrfMapping = computeRouteTargetToVrfMap(getAllPeerConfigs(_process));
    assert _rtVrfMapping != null; // Avoid unused warning
  }

  /**
   * Computes the mapping route targets to VRF names, for all layer 3 EVPN VNIs, across all bgp
   * neighbors in our VRF.
   */
  @VisibleForTesting
  static Map<String, String> computeRouteTargetToVrfMap(Stream<BgpPeerConfig> peerConfigs) {
    HashMap<String, String> rtVrfMappingBuilder = new HashMap<>();
    peerConfigs
        .map(BgpPeerConfig::getEvpnAddressFamily)
        .filter(Objects::nonNull)
        .flatMap(af -> Stream.concat(af.getL3VNIs().stream(), af.getL2VNIs().stream()))
        .forEach(vni -> rtVrfMappingBuilder.put(vni.getImportRouteTarget(), vni.getVrf()));
    return ImmutableMap.copyOf(rtVrfMappingBuilder);
  }

  @Override
  public void initialize(Node n) {
    _initialized = true;
    initLocalEvpnRoutes(n);
  }

  /** Returns true if this process has been initialized */
  public boolean isInitialized() {
    return _initialized;
  }

  /**
   * Initialize incoming BGP message queues for all address families.
   *
   * @param bgpTopology source of truth for which sessions get established.
   */
  private void initBgpQueues(BgpTopology bgpTopology) {
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> graph = bgpTopology.getGraph();
    // Create incoming message queues for sessions that exchange IPv4 unicast info
    _bgpv4Edges =
        getEdgeIdStream(graph, BgpPeerConfig::getIpv4UnicastAddressFamily, Type.IPV4_UNICAST)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
    // Create incoming message queues for sessions that exchange EVPN info
    _evpnType3IncomingRoutes =
        getEdgeIdStream(graph, BgpPeerConfig::getEvpnAddressFamily, Type.EVPN)
            .collect(toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
  }

  /**
   * Return a stream of BGP topology {@link EdgeId} based on BGP neighbors configured for this BGP
   * process.
   *
   * <p>Additionally filters the neighbors based on the desired address family (family must be
   * non-null for the neighbor to be considered).
   *
   * @param graph the BGP topology graph
   * @param familyExtractor function to execute on the {@link BgpPeerConfig} that returns the
   *     desired {@link AddressFamily}. If the address family is null, the peer will be omitted from
   *     edge computation
   */
  @Nonnull
  @VisibleForTesting
  Stream<EdgeId> getEdgeIdStream(
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> graph,
      Function<BgpPeerConfig, AddressFamily> familyExtractor,
      Type familyType) {
    return Streams.concat(
            _process.getActiveNeighbors().entrySet().stream()
                .filter(e -> familyExtractor.apply(e.getValue()) != null)
                .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey(), false)),
            _process.getPassiveNeighbors().entrySet().stream()
                .filter(e -> familyExtractor.apply(e.getValue()) != null)
                .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey(), true)),
            _process.getInterfaceNeighbors().entrySet().stream()
                .filter(e -> familyExtractor.apply(e.getValue()) != null)
                .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey())))
        .filter(graph.nodes()::contains) // avoid missing node exceptions
        .flatMap(
            id ->
                graph.incidentEdges(id).stream()
                    .filter(
                        pair -> pair.nodeV().equals(id))) // get all incoming edges for this node
        .filter(
            edge ->
                graph
                    .edgeValue(edge.nodeU(), edge.nodeV())
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                String.format(
                                    "Bgp session without session properties for edge %s -> %s",
                                    edge.nodeU(), edge.nodeV())))
                    .getAddressFamilies()
                    .contains(familyType)) // ensure the session contains desired address family
        .map(edge -> new EdgeId(edge.nodeU(), edge.nodeV()));
  }

  @Override
  public void updateTopology(BgpTopology topology) {
    BgpTopology oldTopology = _topology;
    _topology = topology;
    initBgpQueues(_topology);
    // New sessions got established
    _edgesWentUp =
        Sets.difference(
            getEdgeIdStream(topology.getGraph(), BgpPeerConfig::getEvpnAddressFamily, Type.EVPN)
                .collect(ImmutableSet.toImmutableSet()),
            getEdgeIdStream(oldTopology.getGraph(), BgpPeerConfig::getEvpnAddressFamily, Type.EVPN)
                .collect(ImmutableSet.toImmutableSet()));
    _topology = topology;
    // TODO: compute edges that went down, remove routes we received from those neighbors
  }

  @Override
  public void executeIteration(Map<String, Node> allNodes) {
    // Reinitialize delta builders
    _evpnDeltaBuilder = RibDelta.builder();

    // TODO: optimize, don't recreate the map each iteration
    NetworkConfigurations nc =
        NetworkConfigurations.of(
            allNodes.entrySet().stream()
                .collect(
                    ImmutableMap.toImmutableMap(
                        Entry::getKey, e -> e.getValue().getConfiguration())));
    if (!_evpnInitializationDelta.isEmpty()) {
      // If initialization delta has not been sent out, do so now
      sendOutEvpnType3Routes(
          new BgpDelta<>(_evpnInitializationDelta, RibDelta.empty()), nc, allNodes);
      _localType3Routes = _evpnInitializationDelta;
      _evpnInitializationDelta = RibDelta.empty();
    }

    /*
     If we have any new edges, send out our RIB state to them.
     EVPN only
    */
    sendOutRoutesToNewEdges(_edgesWentUp, allNodes, nc);

    processBgpMessages(nc, allNodes);

    // Clear new edges.
    _edgesWentUp = ImmutableSet.of();
  }

  private void sendOutRoutesToNewEdges(
      Set<EdgeId> edgesWentUp, Map<String, Node> allNodes, NetworkConfigurations nc) {
    if (edgesWentUp.isEmpty()) {
      // Nothing to do
      return;
    }

    // Send out the state of our BGPv4 RIB to the neighbors

    // TODO: _localType3Routes is not enough
    //    Ideally we need to re-send all EVPN routes we have to new neighbors
    edgesWentUp.forEach(
        edge -> {
          BgpPeerConfigId remoteConfigId = edge.tail();
          BgpSessionProperties session = getSessionProperties(_topology, edge);
          getNeighborBgpProcess(remoteConfigId, allNodes)
              .enqueueEvpnType3Routes(
                  // Make sure to reverse the edge
                  edge.reverse(),
                  getEvpnTransformedRouteStream(
                      edge,
                      new BgpDelta<>(_localType3Routes, RibDelta.empty()),
                      nc,
                      allNodes,
                      session));
        });
  }

  @Nonnull
  @Override
  public RibDelta<BgpRoute<?, ?>> getUpdatesForMainRib() {
    RibDelta<BgpRoute<?, ?>> result = _toMainRib.build();
    _toMainRib = RibDelta.builder();
    return result;
  }

  @Override
  public void redistribute(RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta) {
    redistribute(mainRibDelta, _vrfName);
  }

  /** Redistribute routes from {@code srcVrfName} into our VRF. */
  public void redistribute(
      RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta, String srcVrfName) {
    assert _toRedistribute.values().stream().allMatch(RibDelta::isEmpty);
    _toRedistribute.put(srcVrfName, mainRibDelta);
  }

  @Override
  public boolean isDirty() {
    return
    // Message queues
    !_evpnType3IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        // Outgoing message deltas. We need to send these to neighbors.
        // The reason we look at PREV values is because
        // endOfRound has been called BEFORE the isDirty check and we've already switched over.
        || !_ebgpv4DeltaPrev.isEmpty()
        || !_bgpv4DeltaPrev.isEmpty()
        || _toRedistribute.values().stream().anyMatch(d -> !d.isEmpty())
        // Delta builders
        || !_bgpv4DeltaBuilder.isEmpty()
        || !_evpnDeltaBuilder.isEmpty()
        // Initialization state
        || !_evpnInitializationDelta.isEmpty();
  }

  /**
   * Process all incoming BGP messages: across all neighbors, across all address families.
   *
   * @param nc {@link NetworkConfigurations network configurations} wrapper
   * @param allNodes map of all network nodes
   */
  private void processBgpMessages(NetworkConfigurations nc, Map<String, Node> allNodes) {
    // Process IPv4 unicast messages
    processBgpV4UnicastMessages(_topology, nc, allNodes);

    // Process EVPN messages and send out updates
    DeltaPair<EvpnType3Route> type3Delta = processEvpnType3Messages(nc, allNodes);
    sendOutEvpnType3Routes(type3Delta._toAdvertise, nc, allNodes);
    _toMainRib.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType3Rib, type3Delta._toMerge._ebgpDelta)));
    _toMainRib.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType3Rib, type3Delta._toMerge._ibgpDelta)));
  }

  /** Initialize the EVPN RIBs based on EVPN address family config */
  @VisibleForTesting
  void initLocalEvpnRoutes(Node n) {
    // default admin costs
    int ebgpAdmin = _process.getAdminCost(RoutingProtocol.BGP);

    Builder<EvpnType3Route> initializationBuilder = RibDelta.builder();
    getAllPeerConfigs(_process)
        .map(BgpPeerConfig::getEvpnAddressFamily)
        .filter(Objects::nonNull)
        .flatMap(af -> af.getL2VNIs().stream())
        .forEach(
            vniConfig -> {
              Vrf vniVrf = _c.getVrfs().get(vniConfig.getVrf());
              assert vniVrf != null; // Invariant guaranteed by proper conversion
              Layer2Vni l2Vni = vniVrf.getLayer2Vnis().get(vniConfig.getVni());
              assert l2Vni != null; // Invariant guaranteed by proper conversion
              if (l2Vni.getSourceAddress() == null) {
                return;
              }
              EvpnType3Route route =
                  initEvpnType3Route(
                      ebgpAdmin,
                      l2Vni,
                      vniConfig.getRouteTarget(),
                      vniConfig.getRouteDistinguisher(),
                      _process.getRouterId());

              if (vniVrf.getName().equals(_vrfName)) {
                // Merge into our own RIBs
                RibDelta<EvpnType3Route> d = _evpnType3Rib.mergeRouteGetDelta(route);
                _evpnDeltaBuilder.from(d);
                initializationBuilder.from(d);
              } else {
                // Merge into our sibling VRF corresponding to the VNI
                BgpRoutingProcess bgpRoutingProcess =
                    n.getVirtualRouter(vniVrf.getName())
                        .map(VirtualRouter::getBgpRoutingProcess)
                        .orElse(null);
                checkArgument(
                    bgpRoutingProcess != null,
                    "Missing bgp process for vrf %s, node %s",
                    vniVrf.getName(),
                    _c.getHostname());
                initializationBuilder.from(
                    bgpRoutingProcess.processCrossVrfEvpnRoute(
                        new RouteAdvertisement<>(route), EvpnType3Route.class));
              }
            });
    _evpnInitializationDelta = initializationBuilder.build();
    _toMainRib.from(_evpnDeltaBuilder.build());
  }

  /**
   * Create a new {@link EvpnType3Route} based on given {@link Layer2Vni}. Assumes {@code vni} is
   * valid (e.g., has properly set source address).
   */
  @Nonnull
  @VisibleForTesting
  static EvpnType3Route initEvpnType3Route(
      int ebgpAdmin,
      Layer2Vni vni,
      ExtendedCommunity routeTarget,
      RouteDistinguisher routeDistinguisher,
      Ip routerId) {
    checkArgument(
        vni.getSourceAddress() != null,
        "Cannot construct type 3 route for invalid VNI %s",
        vni.getVni());
    // Locally all routes start as eBGP routes in our own RIB
    EvpnType3Route.Builder type3RouteBuilder = EvpnType3Route.builder();
    type3RouteBuilder.setAdmin(ebgpAdmin);
    type3RouteBuilder.setCommunities(ImmutableSet.of(routeTarget));
    type3RouteBuilder.setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE);
    // so that this route is not installed back in the main RIB of any of the VRFs
    type3RouteBuilder.setNonRouting(true);
    type3RouteBuilder.setOriginatorIp(routerId);
    type3RouteBuilder.setOriginType(OriginType.EGP);
    type3RouteBuilder.setProtocol(RoutingProtocol.BGP);
    type3RouteBuilder.setRouteDistinguisher(routeDistinguisher);
    type3RouteBuilder.setVniIp(vni.getSourceAddress());

    return type3RouteBuilder.build();
  }

  /**
   * Process BGP messages from neighbors, merge them into our own RIBs.
   *
   * @param bgpTopology the bgp peering relationships
   */
  void processBgpV4UnicastMessages(
      BgpTopology bgpTopology, NetworkConfigurations nc, Map<String, Node> nodes) {
    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    Map<Bgpv4Rib, RibDelta.Builder<Bgpv4Route>> ribDeltaBuilders = new IdentityHashMap<>();
    ribDeltaBuilders.put(_ebgpv4Rib, RibDelta.builder());
    ribDeltaBuilders.put(_ibgpv4Rib, RibDelta.builder());

    // Process updates from each neighbor
    for (EdgeId edgeId : _bgpv4Edges) {
      pullV4UnicastMessages(
          bgpTopology, nc, nodes, ribDeltaBuilders, edgeId, _edgesWentUp.contains(edgeId));
    }

    unstage(ribDeltaBuilders);
  }

  /** Merge ribs, in a specific order, into real ribs */
  private void unstage(Map<Bgpv4Rib, Builder<Bgpv4Route>> ribDeltaBuilders) {
    _ebgpv4DeltaCurrent = ribDeltaBuilders.get(_ebgpv4Rib).build();
    RibDelta<Bgpv4Route> ibgpDelta = ribDeltaBuilders.get(_ibgpv4Rib).build();

    // Note: keep ebgp before ibgp, as ebgp routes are often preferred, so less thrash
    _bgpv4DeltaBuilder.from(importRibDelta(_bgpv4Rib, _ebgpv4DeltaCurrent));
    _bgpv4DeltaBuilder.from(importRibDelta(_bgpv4Rib, ibgpDelta));
    // Finally, prepare the delta we will feed into the main RIB
    RibDelta<Bgpv4Route> bgpv4RibDelta = _bgpv4DeltaBuilder.build();
    LOGGER.debug("Got v4 routes: {}", bgpv4RibDelta);
    _toMainRib.from(bgpv4RibDelta);
  }

  /** Pull v4Unicast routes from our neighbors' deltas, merge them into our own RIBs */
  private void pullV4UnicastMessages(
      BgpTopology bgpTopology,
      NetworkConfigurations nc,
      Map<String, Node> nodes,
      Map<Bgpv4Rib, Builder<Bgpv4Route>> ribDeltas,
      EdgeId edgeId,
      boolean isNewSession) {

    // Setup helper vars
    BgpPeerConfigId remoteConfigId = edgeId.tail();
    BgpPeerConfigId ourConfigId = edgeId.head();
    BgpSessionProperties sessionProperties =
        getBgpSessionProperties(bgpTopology, new EdgeId(remoteConfigId, ourConfigId));
    BgpPeerConfig ourBgpConfig = requireNonNull(nc.getBgpPeerConfig(edgeId.head()));
    assert ourBgpConfig.getIpv4UnicastAddressFamily() != null;
    // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
    boolean useRibGroups =
        ourBgpConfig.getAppliedRibGroup() != null
            && !ourBgpConfig.getAppliedRibGroup().getImportRibs().isEmpty();
    Ip remoteIp = sessionProperties.getTailIp();

    Bgpv4Rib targetRib = sessionProperties.isEbgp() ? _ebgpv4Rib : _ibgpv4Rib;
    Builder<AnnotatedRoute<AbstractRoute>> perNeighborDeltaForRibGroups = RibDelta.builder();

    BgpRoutingProcess neighborProcess = getNeighborBgpProcess(remoteConfigId, nodes);
    Iterator<RouteAdvertisement<Bgpv4Route>> exportedRoutes =
        neighborProcess
            .getOutgoingRoutesForEdge(edgeId.reverse(), nodes, bgpTopology, nc, isNewSession)
            .iterator();

    // Process all routes from neighbor
    while (exportedRoutes.hasNext()) {
      // consume exported routes
      RouteAdvertisement<Bgpv4Route> remoteRouteAdvert = exportedRoutes.next();
      Bgpv4Route remoteRoute = remoteRouteAdvert.getRoute();

      LOGGER.debug("{} Processing bgpv4 route {}", _c.getHostname(), remoteRoute);

      Bgpv4Route.Builder transformedIncomingRouteBuilder =
          transformBgpRouteOnImport(
              remoteRoute,
              sessionProperties.getHeadAs(),
              ourBgpConfig
                  .getIpv4UnicastAddressFamily()
                  .getAddressFamilyCapabilities()
                  .getAllowLocalAsIn(),
              sessionProperties.isEbgp(),
              _process,
              sessionProperties.getTailIp(),
              ourConfigId.getPeerInterface());
      if (transformedIncomingRouteBuilder == null) {
        // Route could not be imported for core protocol reasons
        _prefixTracer.filtered(
            remoteRoute.getNetwork(),
            remoteConfigId.getHostname(),
            remoteIp,
            remoteConfigId.getVrfName(),
            null,
            IN);
        continue;
      }

      // Process route through import policy, if one exists
      String importPolicyName = ourBgpConfig.getIpv4UnicastAddressFamily().getImportPolicy();
      boolean acceptIncoming = true;
      // TODO: ensure there is always an import policy
      if (importPolicyName != null) {
        RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
        if (importPolicy != null) {
          acceptIncoming =
              importPolicy.processBgpRoute(
                  remoteRoute, transformedIncomingRouteBuilder, sessionProperties, IN);
        }
      }
      if (!acceptIncoming) {
        // Route could not be imported due to routing policy
        _prefixTracer.filtered(
            remoteRoute.getNetwork(),
            remoteConfigId.getHostname(),
            remoteIp,
            remoteConfigId.getVrfName(),
            importPolicyName,
            IN);
        continue;
      }
      Bgpv4Route transformedIncomingRoute = transformedIncomingRouteBuilder.build();

      // If new route gets leaked to other VRFs via RibGroup, this VRF should be its
      AnnotatedRoute<AbstractRoute> annotatedTransformedRoute =
          annotateRoute(transformedIncomingRoute);

      if (remoteRouteAdvert.isWithdrawn()) {
        // Note this route was removed
        ribDeltas.get(targetRib).remove(transformedIncomingRoute, Reason.WITHDRAW);
        if (useRibGroups) {
          perNeighborDeltaForRibGroups.remove(annotatedTransformedRoute, Reason.WITHDRAW);
        }
      } else {
        // Merge into staging rib, note delta

        ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
        if (useRibGroups) {
          perNeighborDeltaForRibGroups.add(annotatedTransformedRoute);
        }
        _prefixTracer.installed(
            transformedIncomingRoute.getNetwork(),
            remoteConfigId.getHostname(),
            remoteIp,
            remoteConfigId.getVrfName(),
            importPolicyName);
      }
    }
    // Apply rib groups if any
    if (useRibGroups) {
      RibGroup rg = ourBgpConfig.getAppliedRibGroup();
      rg.getImportRibs()
          .forEach(
              rib ->
                  nodes
                      .get(_c.getHostname())
                      .getVirtualRouterOrThrow(rib.getVrfName())
                      .enqueueCrossVrfRoutes(
                          new CrossVrfEdgeId(_vrfName, rib.getRibName()),
                          perNeighborDeltaForRibGroups.build().getActions(),
                          rg.getImportPolicy()));
    }
  }

  private Stream<RouteAdvertisement<Bgpv4Route>> getOutgoingRoutesForEdge(
      EdgeId edge,
      Map<String, Node> allNodes,
      BgpTopology bgpTopology,
      NetworkConfigurations networkConfigurations,
      boolean isNewSession) {
    BgpSessionProperties session = BgpRoutingProcess.getBgpSessionProperties(bgpTopology, edge);

    BgpPeerConfigId remoteConfigId = edge.tail();
    BgpPeerConfigId ourConfigId = edge.head();
    BgpPeerConfig ourConfig = networkConfigurations.getBgpPeerConfig(edge.head());
    BgpPeerConfig remoteConfig = networkConfigurations.getBgpPeerConfig(edge.tail());

    BgpRoutingProcess remoteBgpRoutingProcess = getNeighborBgpProcess(remoteConfigId, allNodes);

    // Queue mainRib updates that were not introduced by BGP process (i.e., IGP routes)
    // Also, do not double-export main RIB routes: filter out bgp routes.
    Stream<RouteAdvertisement<Bgpv4Route>> mainRibExports =
        (isNewSession
                // Look at the entire main RIB if this session is new.
                ? _mainRib.getTypedRoutes().stream().map(RouteAdvertisement::adding)
                : _toRedistribute.values().stream().flatMap(RibDelta::getActions))
            .filter(adv -> !(adv.getRoute().getRoute() instanceof BgpRoute))
            .map(
                adv -> {
                  _prefixTracer.originated(adv.getRoute().getNetwork());
                  Bgpv4Route bgpRoute =
                      exportNonBgpRouteToBgp(adv.getRoute(), remoteConfigId, ourConfig, session);
                  if (bgpRoute == null) {
                    return null;
                  }
                  return RouteAdvertisement.<Bgpv4Route>builder()
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
      if (isNewSession) {
        bgpRibExports.from(
            _ebgpv4Rib.getTypedRoutes().stream()
                .map(this::annotateRoute)
                .map(RouteAdvertisement::new));
      } else {
        importDeltaToBuilder(bgpRibExports, _ebgpv4DeltaPrev, _vrfName);
      }
    }

    if (session.getAdvertiseInactive()) {
      if (isNewSession) {
        bgpRibExports.from(
            _bgpv4Rib.getTypedRoutes().stream()
                .map(this::annotateRoute)
                .map(RouteAdvertisement::new));
      } else {
        importDeltaToBuilder(bgpRibExports, _bgpv4DeltaPrev, _vrfName);
      }
    } else {
      // Default behavior
      if (isNewSession) {
        bgpRibExports.from(
            // note: only best paths are advertised here
            _bgpv4Rib.getBestPathRoutes().stream()
                .map(this::annotateRoute)
                .map(RouteAdvertisement::new));
      } else {
        bgpRibExports.from(
            _bgpv4DeltaPrev
                .getActions()
                .map(
                    r ->
                        RouteAdvertisement.<AnnotatedRoute<Bgpv4Route>>builder()
                            .setReason(r.getReason())
                            .setRoute(annotateRoute(r.getRoute()))
                            .build())
                .filter(r -> _mainRib.containsRoute(r.getRoute()) || r.isWithdrawn()));
      }
    }

    /*
    * TODO: https://github.com/batfish/batfish/issues/704
       Add path is broken for all intents and purposes.
       Need support for additional-paths based on https://tools.ietf.org/html/rfc7911
       AND the combination of vendor-specific knobs, none of which are currently supported.
    */
    if (session.getAdditionalPaths()) {
      importDeltaToBuilder(bgpRibExports, _bgpv4DeltaPrev, _vrfName);
    }

    /*
     * Export neighbor-specific generated routes.
     * These skip peer export policy, so do not merge them into bgpRoutesToExport
     */
    assert ourConfig != null;
    assert remoteConfig != null;
    Stream<RouteAdvertisement<Bgpv4Route>> neighborGeneratedRoutes =
        ourConfig.getGeneratedRoutes().stream()
            .map(
                r -> {
                  // Activate route and convert to BGP if activated
                  Bgpv4Route bgpv4Route =
                      processNeighborSpecificGeneratedRoute(r, session.getHeadIp());
                  if (bgpv4Route == null) {
                    // Route was not activated
                    return Optional.<Bgpv4Route>empty();
                  }
                  // Run pre-export transform, export policy, & post-export transform
                  return transformBgpRouteOnExport(
                      bgpv4Route,
                      ourConfigId,
                      remoteConfigId,
                      ourConfig,
                      remoteConfig,
                      remoteBgpRoutingProcess,
                      session,
                      Type.IPV4_UNICAST);
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(RouteAdvertisement::new);

    RibDelta<AnnotatedRoute<Bgpv4Route>> bgpRoutesToExport = bgpRibExports.build();

    LOGGER.debug(
        "{} exporting routes BEFORE export policy: {}", _c.getHostname(), bgpRoutesToExport);
    if (LOGGER.isDebugEnabled()) {
      ImmutableList<RouteAdvertisement<Bgpv4Route>> routeAdvertisements =
          neighborGeneratedRoutes.collect(ImmutableList.toImmutableList());
      LOGGER.debug(
          "{} exporting routes AFTER routing policy: {}", _c.getHostname(), routeAdvertisements);
      neighborGeneratedRoutes = routeAdvertisements.stream();
    }

    // Compute a set of advertisements that can be queued on remote VR
    Stream<RouteAdvertisement<Bgpv4Route>> advertisementStream =
        Stream.concat(
            bgpRoutesToExport
                .getActions()
                .map(
                    adv -> {
                      Optional<Bgpv4Route> transformedRoute =
                          transformBgpRouteOnExport(
                              adv.getRoute().getRoute(),
                              ourConfigId,
                              remoteConfigId,
                              ourConfig,
                              remoteConfig,
                              remoteBgpRoutingProcess,
                              session,
                              Type.IPV4_UNICAST);
                      // REPLACE does not make sense across routers, update with WITHDRAW
                      return transformedRoute
                          .map(
                              bgpv4Route ->
                                  RouteAdvertisement.<Bgpv4Route>builder()
                                      .setReason(
                                          adv.getReason() == Reason.REPLACE
                                              ? Reason.WITHDRAW
                                              : adv.getReason())
                                      .setRoute(bgpv4Route)
                                      .build())
                          .orElse(null);
                    })
                .filter(Objects::nonNull)
                .distinct(),
            mainRibExports);

    if (LOGGER.isDebugEnabled()) {
      ImmutableList<RouteAdvertisement<Bgpv4Route>> routeAdvertisements =
          advertisementStream.collect(ImmutableList.toImmutableList());
      LOGGER.debug(
          "{} exporting routes AFTER routing policy: {}", _c.getHostname(), routeAdvertisements);
      advertisementStream = routeAdvertisements.stream();
    }

    return Stream.concat(advertisementStream, neighborGeneratedRoutes);
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
  private Bgpv4Route processNeighborSpecificGeneratedRoute(
      @Nonnull GeneratedRoute generatedRoute, Ip nextHopIp) {
    String policyName = generatedRoute.getGenerationPolicy();
    RoutingPolicy policy = policyName != null ? _c.getRoutingPolicies().get(policyName) : null;
    @Nullable
    RoutingPolicy attrPolicy =
        generatedRoute.getAttributePolicy() != null
            ? _c.getRoutingPolicies().get(generatedRoute.getAttributePolicy())
            : null;
    GeneratedRoute.Builder builder =
        GeneratedRouteHelper.activateGeneratedRoute(
            generatedRoute, policy, _mainRib.getTypedRoutes());
    return builder != null
        ? BgpProtocolHelper.convertGeneratedRouteToBgp(
            builder.build(), attrPolicy, _process.getRouterId(), nextHopIp, false)
        : null;
  }

  /**
   * This function creates BGP routes from generated routes that go into the BGP RIB, but cannot be
   * imported into the main RIB. The purpose of these routes is to prevent the local router from
   * accepting advertisements less desirable than the locally generated ones for a given network.
   */
  void initBgpAggregateRoutes(Collection<AbstractRoute> generatedRoutes) {
    // first import aggregates
    switch (_c.getConfigurationFormat()) {
      case FLAT_JUNIPER:
      case JUNIPER:
      case JUNIPER_SWITCH:
        return;
        // $CASES-OMITTED$
      default:
        break;
    }
    for (AbstractRoute grAbstract : generatedRoutes) {
      GeneratedRoute gr = (GeneratedRoute) grAbstract;

      Bgpv4Route br =
          BgpProtocolHelper.convertGeneratedRouteToBgp(
              gr,
              Optional.ofNullable(gr.getAttributePolicy())
                  .map(p -> _c.getRoutingPolicies().get(p))
                  .orElse(null),
              _process.getRouterId(),
              Ip.ZERO,
              // Prevent route from being merged into the main RIB by marking it non-routing
              true);
      /* TODO: tests for this */
      RibDelta<Bgpv4Route> d1 = _bgpv4Rib.mergeRouteGetDelta(br);
      _bgpv4DeltaBuilder.from(d1);
      if (!d1.isEmpty()) {
        _bgpAggDeps.addRouteDependency(br, gr);
      }
    }
  }

  private <R extends AbstractRoute> AnnotatedRoute<R> annotateRoute(@Nonnull R route) {
    return new AnnotatedRoute<>(route, _vrfName);
  }

  /** Process incoming EVPN type 3 messages, across all neighbors */
  private DeltaPair<EvpnType3Route> processEvpnType3Messages(
      NetworkConfigurations nc, Map<String, Node> allNodes) {
    DeltaPair<EvpnType3Route> deltaPair = DeltaPair.empty();
    for (Entry<EdgeId, Queue<RouteAdvertisement<EvpnType3Route>>> entry :
        _evpnType3IncomingRoutes.entrySet()) {
      EdgeId edge = entry.getKey();
      Queue<RouteAdvertisement<EvpnType3Route>> queue = entry.getValue();
      deltaPair =
          deltaPair.union(
              processEvpnMessagesFromNeighbor(edge, queue, nc, allNodes, EvpnType3Route.class));
    }
    return deltaPair;
  }

  /** Process all incoming EVPN messages for a given session, identified by {@code edge} */
  private <B extends EvpnRoute.Builder<B, R>, R extends EvpnRoute<B, R>>
      DeltaPair<R> processEvpnMessagesFromNeighbor(
          EdgeId edge,
          Queue<RouteAdvertisement<R>> queue,
          NetworkConfigurations nc,
          Map<String, Node> allNodes,
          Class<R> clazz) {
    BgpPeerConfigId ourConfigId = edge.head();
    BgpPeerConfig ourBgpConfig = nc.getBgpPeerConfig(ourConfigId);
    assert ourBgpConfig != null; // because the edge exists
    assert ourBgpConfig.getEvpnAddressFamily() != null;
    // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
    BgpSessionProperties sessionProperties = getSessionProperties(_topology, edge);
    EvpnRib<R> targetRib = getRib(clazz, sessionProperties.isEbgp() ? RibType.EBGP : RibType.IBGP);
    RibDelta.Builder<R> toAdvertise = RibDelta.builder();
    RibDelta.Builder<R> toMerge = RibDelta.builder();
    while (!queue.isEmpty()) {
      RouteAdvertisement<R> routeAdvertisement = queue.remove();
      R route = routeAdvertisement.getRoute();
      B transformedBuilder =
          transformBgpRouteOnImport(
              route,
              sessionProperties.getHeadAs(),
              ourBgpConfig
                  .getEvpnAddressFamily()
                  .getAddressFamilyCapabilities()
                  .getAllowLocalAsIn(),
              sessionProperties.isEbgp(),
              _process,
              sessionProperties.getTailIp(),
              ourConfigId.getPeerInterface());
      if (transformedBuilder == null) {
        continue;
      }

      // Process route through import policy, if one exists
      String importPolicyName = ourBgpConfig.getEvpnAddressFamily().getImportPolicy();
      boolean acceptIncoming = true;
      if (importPolicyName != null) {
        RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
        if (importPolicy != null) {
          acceptIncoming =
              importPolicy.processBgpRoute(route, transformedBuilder, sessionProperties, IN);
        }
      }
      if (!acceptIncoming) {
        continue;
      }
      if (clazz.equals(EvpnType3Route.class)) {
        // Type 3 routes are special: they don't go into main RIB, they only update L2 VNI's flood
        // list
        transformedBuilder.setNonRouting(true);
      }
      R transformedRoute = transformedBuilder.build();
      Set<ExtendedCommunity> routeTargets = transformedRoute.getRouteTargets();
      if (routeTargets.isEmpty()) {
        // Skip if the route target is unrecognized
        continue;
      }
      // TODO:
      //  handle multiple route targets pointing to different VRFs (should merge into multiple VRFs)
      ExtendedCommunity routeTarget = routeTargets.iterator().next();
      Optional<String> targetVrf =
          _rtVrfMapping.entrySet().stream()
              .filter(e -> Pattern.compile(e.getKey()).matcher(routeTarget.matchString()).matches())
              .map(Entry::getValue)
              .findFirst();
      if (targetVrf.isPresent()) {
        if (_vrfName.equals(targetVrf.get())) {
          // Merge into our own RIBs, and put into re-advertisement delta
          RibDelta<R> d = targetRib.mergeRouteGetDelta(transformedRoute);
          toAdvertise.from(d);
          toMerge.from(d);
        } else {
          // Merge into other VRF's RIB and put into re-advertisement delta
          toAdvertise.from(
              getVrfProcess(targetVrf.get(), allNodes)
                  .processCrossVrfEvpnRoute(
                      routeAdvertisement.toBuilder().setRoute(transformedRoute).build(), clazz));
        }
      } else {
        // Simply propagate to neighbors, nothing to do locally
        toAdvertise.from(routeAdvertisement);
      }
    }

    BgpDelta<R> advertiseDelta =
        sessionProperties.isEbgp()
            ? new BgpDelta<>(toAdvertise.build(), RibDelta.empty())
            : new BgpDelta<>(RibDelta.empty(), toAdvertise.build());
    BgpDelta<R> mergeDelta =
        sessionProperties.isEbgp()
            ? new BgpDelta<>(toMerge.build(), RibDelta.empty())
            : new BgpDelta<>(RibDelta.empty(), toMerge.build());
    return new DeltaPair<>(advertiseDelta, mergeDelta);
  }

  /** Send out EVPN type 3 routes to our neighbors */
  private void sendOutEvpnType3Routes(
      BgpDelta<EvpnType3Route> evpnDelta, NetworkConfigurations nc, Map<String, Node> allNodes) {
    _evpnType3IncomingRoutes
        .keySet()
        .forEach(
            edge -> {
              BgpPeerConfigId remoteConfigId = edge.tail();
              BgpSessionProperties session = getSessionProperties(_topology, edge);
              getNeighborBgpProcess(remoteConfigId, allNodes)
                  .enqueueEvpnType3Routes(
                      // Make sure to reverse the edge
                      edge.reverse(),
                      getEvpnTransformedRouteStream(edge, evpnDelta, nc, allNodes, session));
            });
  }

  @Nonnull
  private <B extends EvpnRoute.Builder<B, R>, R extends EvpnRoute<B, R>>
      Stream<RouteAdvertisement<R>> getEvpnTransformedRouteStream(
          EdgeId edge,
          BgpDelta<R> evpnDelta,
          NetworkConfigurations nc,
          Map<String, Node> allNodes,
          BgpSessionProperties session) {
    BgpPeerConfigId remoteConfigId = edge.tail();
    BgpPeerConfigId ourConfigId = edge.head();
    BgpPeerConfig ourConfig = nc.getBgpPeerConfig(ourConfigId);
    BgpPeerConfig remoteConfig = nc.getBgpPeerConfig(remoteConfigId);
    assert ourConfig != null; // Invariant of the edge existing
    assert remoteConfig != null; // Invariant of the edge existing
    BgpRoutingProcess remoteBgpRoutingProcess = getNeighborBgpProcess(remoteConfigId, allNodes);
    return Stream.concat(evpnDelta._ebgpDelta.getActions(), evpnDelta._ibgpDelta.getActions())
        .map(
            // TODO: take into account address-family session settings, such as add-path or
            //   advertise-inactive
            adv ->
                transformBgpRouteOnExport(
                        // clear non-routing flag if set before sending it out
                        adv.getRoute().toBuilder().setNonRouting(false).build(),
                        ourConfigId,
                        remoteConfigId,
                        ourConfig,
                        remoteConfig,
                        remoteBgpRoutingProcess,
                        session,
                        Type.EVPN)
                    .map(
                        r ->
                            RouteAdvertisement.<R>builder()
                                .setReason(
                                    adv.getReason() == Reason.REPLACE
                                        ? Reason.WITHDRAW
                                        : adv.getReason())
                                .setRoute(r)
                                .build()))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  /**
   * Given a {@link BgpRoute}, run it through the BGP outbound transformations and export routing
   * policy.
   *
   * @param exportCandidate a route to try and export
   * @param ourConfig {@link BgpPeerConfig} that sends the route
   * @param remoteConfig {@link BgpPeerConfig} that will be receiving the route
   * @param remoteBgpRoutingProcess {@link BgpRoutingProcess} that will be recieving the route
   * @param sessionProperties {@link BgpSessionProperties} representing the <em>incoming</em> edge:
   *     i.e. the edge from {@code remoteConfig} to {@code ourConfig}
   * @param afType {@link AddressFamily.Type} for which the transformation should occur
   * @return The transformed route as a {@link Bgpv4Route}, or {@code null} if the route should not
   *     be exported.
   */
  <B extends BgpRoute.Builder<B, R>, R extends BgpRoute<B, R>>
      Optional<R> transformBgpRouteOnExport(
          BgpRoute<B, R> exportCandidate,
          BgpPeerConfigId ourConfigId,
          BgpPeerConfigId remoteConfigId,
          BgpPeerConfig ourConfig,
          BgpPeerConfig remoteConfig,
          BgpRoutingProcess remoteBgpRoutingProcess,
          BgpSessionProperties sessionProperties,
          AddressFamily.Type afType) {

    // Do some sanity checking first -- AF and policies should exist
    AddressFamily addressFamily = ourConfig.getAddressFamily(afType);
    checkArgument(
        addressFamily != null,
        "Missing address family %s for BGP peer %s",
        addressFamily,
        ourConfigId);
    String exportPolicyName = addressFamily.getExportPolicy();
    assert exportPolicyName != null; // Conversion guarantee
    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(exportPolicyName);
    assert exportPolicy != null; // Conversion guarantee

    B transformedOutgoingRouteBuilder =
        BgpProtocolHelper.transformBgpRoutePreExport(
            ourConfig,
            remoteConfig,
            sessionProperties,
            _process,
            remoteBgpRoutingProcess._process,
            exportCandidate,
            addressFamily.getType());

    if (transformedOutgoingRouteBuilder == null) {
      // This route could not be exported for core bgp protocol reasons
      return Optional.empty();
    }

    // Process transformed outgoing route by the export policy
    boolean shouldExport =
        exportPolicy.processBgpRoute(
            exportCandidate, transformedOutgoingRouteBuilder, sessionProperties, Direction.OUT);

    // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
    Ip remoteIp = sessionProperties.getTailIp();

    if (!shouldExport) {
      // This route could not be exported due to export policy
      _prefixTracer.filtered(
          exportCandidate.getNetwork(),
          remoteConfigId.getHostname(),
          remoteIp,
          remoteConfigId.getVrfName(),
          exportPolicyName,
          Direction.OUT);
      return Optional.empty();
    }
    // Apply final post-policy transformations before sending advertisement to neighbor
    BgpProtocolHelper.transformBgpRoutePostExport(
        transformedOutgoingRouteBuilder,
        sessionProperties.isEbgp(),
        sessionProperties.getConfedSessionType(),
        sessionProperties.getHeadAs(),
        sessionProperties.getHeadIp(),
        exportCandidate.getNextHopIp());
    // Successfully exported route
    R transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();

    _prefixTracer.sentTo(
        transformedOutgoingRoute.getNetwork(),
        remoteConfigId.getHostname(),
        remoteIp,
        remoteConfigId.getVrfName(),
        exportPolicyName);

    return Optional.of(transformedOutgoingRoute);
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
  Bgpv4Route exportNonBgpRouteToBgp(
      @Nonnull AnnotatedRoute<AbstractRoute> exportCandidate,
      @Nonnull BgpPeerConfigId remoteConfigId,
      @Nonnull BgpPeerConfig ourConfig,
      @Nonnull BgpSessionProperties sessionProperties) {

    RoutingPolicy exportPolicy =
        _c.getRoutingPolicies().get(ourConfig.getIpv4UnicastAddressFamily().getExportPolicy());
    RoutingProtocol protocol =
        sessionProperties.isEbgp() ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
    Bgpv4Route.Builder transformedOutgoingRouteBuilder =
        exportCandidate.getRoute() instanceof GeneratedRoute
            ? BgpProtocolHelper.convertGeneratedRouteToBgp(
                (GeneratedRoute) exportCandidate.getRoute(),
                Optional.ofNullable(
                        ((GeneratedRoute) exportCandidate.getRoute()).getAttributePolicy())
                    .map(p -> _c.getRoutingPolicies().get(p))
                    .orElse(null),
                _process.getRouterId(),
                sessionProperties.getHeadIp(),
                false)
                .toBuilder()
            : BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
                exportCandidate,
                getRouterId(),
                sessionProperties.getHeadIp(),
                _process.getAdminCost(protocol),
                protocol);

    // Process transformed outgoing route by the export policy
    boolean shouldExport =
        exportPolicy.processBgpRoute(
            exportCandidate, transformedOutgoingRouteBuilder, sessionProperties, Direction.OUT);

    // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
    Ip remoteIp = sessionProperties.getHeadIp();

    if (!shouldExport) {
      // This route could not be exported due to export policy
      _prefixTracer.filtered(
          exportCandidate.getNetwork(),
          remoteConfigId.getHostname(),
          remoteIp,
          remoteConfigId.getVrfName(),
          ourConfig.getIpv4UnicastAddressFamily().getExportPolicy(),
          Direction.OUT);
      return null;
    }

    // Apply final post-policy transformations before sending advertisement to neighbor
    BgpProtocolHelper.transformBgpRoutePostExport(
        transformedOutgoingRouteBuilder,
        sessionProperties.isEbgp(),
        sessionProperties.getConfedSessionType(),
        sessionProperties.getHeadAs(),
        sessionProperties.getHeadIp(),
        Route.UNSET_ROUTE_NEXT_HOP_IP);

    // Successfully exported route
    Bgpv4Route transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
    _prefixTracer.sentTo(
        transformedOutgoingRoute.getNetwork(),
        remoteConfigId.getHostname(),
        remoteIp,
        remoteConfigId.getVrfName(),
        ourConfig.getIpv4UnicastAddressFamily().getExportPolicy());

    return transformedOutgoingRoute;
  }

  /**
   * Initializes BGP RIBs prior to any dataplane iterations based on the external BGP advertisements
   * coming into the network.
   *
   * <p>Note: assumes the external advertisements are pre-transformation and will run import policy
   * on them, if present.
   *
   * @param externalAdverts a set of external BGP advertisements
   * @param ipVrfOwners mapping of IPs to their owners in our network
   */
  void processExternalBgpAdvertisements(
      Set<BgpAdvertisement> externalAdverts, Map<Ip, Map<String, Set<String>>> ipVrfOwners) {

    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    Map<Bgpv4Rib, RibDelta.Builder<Bgpv4Route>> ribDeltas = new IdentityHashMap<>();
    ribDeltas.put(_ebgpv4Rib, RibDelta.builder());
    ribDeltas.put(_ibgpv4Rib, RibDelta.builder());

    Bgpv4Route.Builder outgoingRouteBuilder = new Bgpv4Route.Builder();
    // Process each BGP advertisement
    for (BgpAdvertisement advert : externalAdverts) {

      // If it is not for us, ignore it
      if (!advert.getDstNode().equals(_c.getHostname())) {
        continue;
      }

      // If we don't own the IP for this advertisement, ignore it
      Ip dstIp = advert.getDstIp();
      Map<String, Set<String>> dstIpOwners = ipVrfOwners.get(dstIp);
      String hostname = _c.getHostname();
      if (dstIpOwners == null || !dstIpOwners.containsKey(hostname)) {
        continue;
      }

      Ip srcIp = advert.getSrcIp();
      // TODO: support passive and unnumbered bgp connections
      Prefix srcPrefix = srcIp.toPrefix();
      BgpPeerConfig neighbor = _process.getActiveNeighbors().get(srcPrefix);
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

      Bgpv4Rib targetRib = ebgp ? _ebgpv4Rib : _ibgpv4Rib;
      RoutingProtocol targetProtocol = ebgp ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
      int admin = _process.getAdminCost(targetProtocol);

      if (received) {
        Bgpv4Route.Builder builder = new Bgpv4Route.Builder();
        builder.setAdmin(admin);
        builder.setAsPath(advert.getAsPath());
        builder.setClusterList(advert.getClusterList());
        builder.setCommunities(advert.getCommunities());
        builder.setLocalPreference(advert.getLocalPreference());
        builder.setMetric(advert.getMed());
        builder.setNetwork(advert.getNetwork());
        builder.setNextHopIp(advert.getNextHopIp());
        builder.setOriginatorIp(advert.getOriginatorIp());
        builder.setOriginType(advert.getOriginType());
        builder.setProtocol(targetProtocol);
        // TODO: support external route reflector clients
        builder.setReceivedFromIp(advert.getSrcIp());
        builder.setReceivedFromRouteReflectorClient(false);
        builder.setSrcProtocol(advert.getSrcProtocol());
        // TODO: possibly support setting tag
        builder.setWeight(advert.getWeight());
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
        transformedIncomingRouteBuilder.addCommunities(
            transformedOutgoingRoute.getCommunities().getCommunities());

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
        String importPolicyName = neighbor.getIpv4UnicastAddressFamily().getImportPolicy();
        // TODO: ensure there is always an import policy

        if (ebgp
            && transformedOutgoingRoute.getAsPath().containsAs(neighbor.getLocalAs())
            && !neighbor
                .getIpv4UnicastAddressFamily()
                .getAddressFamilyCapabilities()
                .getAllowLocalAsIn()) {
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
                importPolicy.process(transformedOutgoingRoute, transformedIncomingRouteBuilder, IN);
          }
        }
        if (acceptIncoming) {
          Bgpv4Route transformedIncomingRoute = transformedIncomingRouteBuilder.build();
          ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
        }
      }
    }

    // Propagate received routes through all the RIBs
    unstage(ribDeltas);
  }

  /**
   * Process EVPN routes that were received on a session in a different VRF, but must be merged into
   * our VRF
   */
  @Nonnull
  private synchronized <B extends EvpnRoute.Builder<B, R>, R extends EvpnRoute<B, R>>
      RibDelta<R> processCrossVrfEvpnRoute(
          RouteAdvertisement<R> routeAdvertisement, Class<R> clazz) {
    // TODO: consider switching return value to BgpDelta to differentiate e/iBGP
    RibDelta<R> delta;
    BgpRib<R> rib = getRib(clazz, RibType.COMBINED);
    if (routeAdvertisement.isWithdrawn()) {
      delta =
          rib.removeRouteGetDelta(routeAdvertisement.getRoute(), routeAdvertisement.getReason());
    } else {
      delta = rib.mergeRouteGetDelta(routeAdvertisement.getRoute());
    }
    // Queue up the routes to be merged into our main RIB
    _toMainRib.from(importRibDelta(_evpnRib, delta));
    return delta;
  }

  @Nonnull
  private static BgpSessionProperties getSessionProperties(BgpTopology bgpTopology, EdgeId edge) {
    Optional<BgpSessionProperties> session =
        bgpTopology.getGraph().edgeValue(edge.tail(), edge.head());
    // BGP topology edge guaranteed to exist since the session is established
    assert session.isPresent();
    return session.get();
  }

  int iterationHashCode() {
    return Stream.of(
            // RIBs
            _bgpv4Rib.getTypedRoutes(),
            _evpnRib.getTypedRoutes(),
            // Outgoing RIB deltas
            // The reason we look at PREV values is because
            // endOfRound has been called BEFORE the isDirty check and we've already switched over.
            _ebgpv4DeltaPrev,
            _bgpv4DeltaPrev,
            // Message queues
            _evpnType3IncomingRoutes,
            // Delta builders
            _bgpv4DeltaBuilder.build(),
            _evpnDeltaBuilder.build())
        .collect(toOrderedHashCode());
  }

  public void endOfRound() {
    _bgpv4DeltaPrev = _bgpv4DeltaBuilder.build();
    _bgpv4DeltaBuilder = RibDelta.builder();
    _ebgpv4DeltaPrev = _ebgpv4DeltaCurrent;
    _toRedistribute = new HashMap<>();
  }

  /**
   * Return the stream of all {@link BgpPeerConfig peer configurations} that are part of this
   * process
   */
  private static Stream<BgpPeerConfig> getAllPeerConfigs(BgpProcess process) {
    return Streams.concat(
        process.getActiveNeighbors().values().stream(),
        process.getPassiveNeighbors().values().stream(),
        process.getInterfaceNeighbors().values().stream());
  }

  static BgpSessionProperties getBgpSessionProperties(BgpTopology bgpTopology, EdgeId edge) {
    // BGP topology edge guaranteed to exist since the session is established
    Optional<BgpSessionProperties> session =
        bgpTopology.getGraph().edgeValue(edge.tail(), edge.head());
    return session.orElseThrow(
        () -> new IllegalArgumentException(String.format("No BGP edge %s in BGP topology", edge)));
  }

  @Nonnull
  public Ip getRouterId() {
    return _process.getRouterId();
  }

  /** Return all type 3 EVPN routes */
  public Set<EvpnType3Route> getEvpnType3Routes() {
    return _evpnType3Rib.getTypedRoutes();
  }

  /**
   * Message passing method between BGP processes. Take a collection of BGP {@link
   * RouteAdvertisement}s and puts them onto a local queue corresponding to the session between
   * given neighbors.
   */
  private void enqueueEvpnType3Routes(
      @Nonnull EdgeId edgeId, @Nonnull Stream<RouteAdvertisement<EvpnType3Route>> routes) {
    Queue<RouteAdvertisement<EvpnType3Route>> q = _evpnType3IncomingRoutes.get(edgeId);
    assert q != null; // Invariant of the session being up
    routes.forEach(q::add);
  }

  /** Return a BGP routing process for a given {@link BgpPeerConfigId} */
  @Nonnull
  private static BgpRoutingProcess getNeighborBgpProcess(
      BgpPeerConfigId id, Map<String, Node> allNodes) {
    BgpRoutingProcess proc =
        allNodes
            .get(id.getHostname())
            .getVirtualRouterOrThrow(id.getVrfName())
            .getBgpRoutingProcess();
    assert proc != null; // Otherwise our computation is really wrong
    return proc;
  }

  /** Return a BGP routing process for a sibling VRF on our node */
  @Nonnull
  private BgpRoutingProcess getVrfProcess(String vrf, Map<String, Node> allNodes) {
    BgpRoutingProcess proc =
        allNodes.get(_c.getHostname()).getVirtualRouterOrThrow(vrf).getBgpRoutingProcess();
    assert proc != null;
    return proc;
  }

  /** Notifies the process an aggregate has been removed */
  void removeAggregate(AbstractRoute route) {
    // TODO: this is probably busted because it doesn't check e/iBGP ribs.
    _bgpv4DeltaBuilder.from(_bgpAggDeps.deleteRoute(route, _bgpv4Rib));
  }

  /** Return a set of all bgpv4 routes */
  public Set<Bgpv4Route> getV4Routes() {
    return _bgpv4Rib.getTypedRoutes();
  }

  /** Return a set of all bgpv4 bestpath routes */
  public Set<Bgpv4Route> getBestPathRoutes() {
    return _bgpv4Rib.getBestPathRoutes();
  }

  /** Container for eBGP+iBGP RIB deltas */
  private static final class BgpDelta<R extends BgpRoute<?, ?>> {

    private static final BgpDelta<?> EMPTY = new BgpDelta<>(RibDelta.empty(), RibDelta.empty());
    @Nonnull private final RibDelta<R> _ebgpDelta;
    @Nonnull private final RibDelta<R> _ibgpDelta;

    private BgpDelta(@Nonnull RibDelta<R> ebgpDelta, @Nonnull RibDelta<R> ibgpDelta) {
      _ebgpDelta = ebgpDelta;
      _ibgpDelta = ibgpDelta;
    }

    @Nonnull
    public RibDelta<R> getEbgpDelta() {
      return _ebgpDelta;
    }

    @Nonnull
    public RibDelta<R> getIbgpDelta() {
      return _ibgpDelta;
    }

    public boolean isEmpty() {
      return _ebgpDelta.isEmpty() && _ibgpDelta.isEmpty();
    }

    private BgpDelta<R> union(BgpDelta<R> other) {
      return new BgpDelta<>(
          RibDelta.<R>builder().from(_ebgpDelta).from(other._ebgpDelta).build(),
          RibDelta.<R>builder().from(_ibgpDelta).from(other._ibgpDelta).build());
    }

    @SuppressWarnings("unchecked") // fully variant, will never store any Ts
    @Nonnull
    public static <B extends BgpRoute.Builder<B, R>, R extends BgpRoute<B, R>> BgpDelta<R> empty() {
      return (BgpDelta<R>) EMPTY;
    }
  }

  /**
   * Container for a pair for {@link BgpDelta deltas}: one delta for re-advertisement to neighbors,
   * another for merging into local RIBs.
   */
  private static final class DeltaPair<R extends BgpRoute<?, ?>> {
    private static final DeltaPair<?> EMPTY =
        new DeltaPair<Bgpv4Route>(BgpDelta.empty(), BgpDelta.empty());
    @Nonnull private final BgpDelta<R> _toAdvertise;
    @Nonnull private final BgpDelta<R> _toMerge;

    private DeltaPair(BgpDelta<R> toAdvertise, BgpDelta<R> toMerge) {
      _toAdvertise = toAdvertise;
      _toMerge = toMerge;
    }

    @Nonnull
    private DeltaPair<R> union(DeltaPair<R> other) {
      return new DeltaPair<>(
          _toAdvertise.union(other._toAdvertise), _toMerge.union(other._toMerge));
    }

    @SuppressWarnings("unchecked") // fully variant, will never store any Ts
    @Nonnull
    private static <B extends BgpRoute.Builder<B, R>, R extends BgpRoute<B, R>>
        DeltaPair<R> empty() {
      return (DeltaPair<R>) EMPTY;
    }
  }

  /** Type of BGP RIB. Solely for use in {@link BgpRoutingProcess#getRib} */
  private enum RibType {
    /** For eBGP routes only */
    EBGP,
    /** For iBGP routes only */
    IBGP,
    /** Combined RIB, for both eBGP and iBGP routes */
    COMBINED
  }

  /** Return a RIB based on route type and {@link RibType} */
  @SuppressWarnings("unchecked")
  private <B extends BgpRoute.Builder<B, R>, R extends BgpRoute<B, R>, T extends BgpRib<R>>
      T getRib(Class<R> clazz, RibType type) {
    if (clazz.equals(Bgpv4Route.class)) {
      switch (type) {
        case EBGP:
          return (T) _ebgpv4Rib;
        case IBGP:
          return (T) _ibgpv4Rib;
        case COMBINED:
          return (T) _bgpv4Rib;
        default:
          throw new IllegalArgumentException("Unsupported RIB type: " + type);
      }
    } else if (clazz.equals(EvpnType3Route.class)) {
      switch (type) {
        case EBGP:
          return (T) _ebgpType3EvpnRib;
        case IBGP:
          return (T) _ibgpType3EvpnRib;
        case COMBINED:
          return (T) _evpnType3Rib;
        default:
          throw new IllegalArgumentException("Unsupported RIB type: " + type);
      }
    } else if (clazz.equals(EvpnType5Route.class)) {
      switch (type) {
        case EBGP:
          return (T) _ebgpType5EvpnRib;
        case IBGP:
          return (T) _ibgpType5EvpnRib;
        case COMBINED:
          return (T) _evpnType5Rib;
        default:
          throw new IllegalArgumentException("Unsupported RIB type: " + type);
      }
    } else {
      throw new IllegalArgumentException("Unsupported BGP route type: " + clazz.getCanonicalName());
    }
  }
}
