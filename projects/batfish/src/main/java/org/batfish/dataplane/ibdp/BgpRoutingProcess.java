package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.common.util.CollectionUtil.toOrderedHashCode;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.routing_policy.Environment.Direction.IN;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRouteOnImport;
import static org.batfish.dataplane.rib.RibDelta.importRibDelta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.graph.ValueGraph;
import java.util.Collection;
import java.util.HashMap;
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
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
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
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.AddressFamily;
import org.batfish.datamodel.bgp.AddressFamily.Type;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.VniConfig;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Vni;
import org.batfish.dataplane.protocols.BgpProtocolHelper;
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
  @Nonnull final BgpProcess _process;
  /** Parent node configuration */
  @Nonnull private final Configuration _c;
  /** Name of our VRF */
  @Nonnull private final String _vrfName;
  /** Reference to the parent {@link VirtualRouter} main RIB (read-only). */
  @Nonnull private final Rib _mainRib;
  /** Current BGP topology */
  @Nonnull private BgpTopology _topology;
  /** Metadata about propagated prefixes to/from neighbors */
  @Nonnull private PrefixTracer _prefixTracer;

  /** Route dependency tracker for BGP IPv4 aggregate routes */
  @Nonnull
  RouteDependencyTracker<Bgpv4Route, AbstractRoute> _bgpAggDeps = new RouteDependencyTracker<>();
  /**
   * Incoming messages into this router from each BGP neighbor that speaks IPv4 unicast address
   * family
   */
  @Nonnull SortedMap<EdgeId, Queue<RouteAdvertisement<Bgpv4Route>>> _bgpv4IncomingRoutes;
  /**
   * Incoming EVPN type 3 advertisements into this router from each BGP neighbor that speaks EVPN
   * address family
   */
  @Nonnull @VisibleForTesting
  SortedMap<EdgeId, Queue<RouteAdvertisement<EvpnType3Route>>> _evpnType3IncomingRoutes;
  /**
   * Incoming EVPN type 5 advertisements into this router from each BGP neighbor that speaks EVPN
   * address family
   */
  @Nonnull
  private SortedMap<EdgeId, Queue<RouteAdvertisement<EvpnType5Route>>> _evpnType5IncomingRoutes;

  // RIBs and RIB delta builders
  /** Helper RIB containing all paths obtained with external BGP, for IPv4 unicast */
  @Nonnull Bgpv4Rib _ebgpv4Rib;
  /**
   * Helper RIB containing paths obtained with eBGP during current iteration. An Adj-RIB-in of sorts
   * for IPv4 unicast
   */
  @Nonnull Bgpv4Rib _ebgpv4StagingRib;
  /** RIB containing paths obtained with iBGP, for IPv4 unicast */
  @Nonnull Bgpv4Rib _ibgpv4Rib;
  /**
   * Helper RIB containing paths obtained with iBGP during current iteration. An Adj-RIB-in of sorts
   * for IPv4 unicast
   */
  @Nonnull Bgpv4Rib _ibgpv4StagingRib;
  /** Combined BGP (both iBGP and eBGP) RIB, for IPv4 unicast */
  @Nonnull Bgpv4Rib _bgpv4Rib;
  /** Builder for constructing {@link RibDelta} which represent changes to {@link #_bgpv4Rib} */
  @Nonnull Builder<Bgpv4Route> _bgpv4DeltaBuilder;

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
  @Nonnull private RibDelta.Builder<BgpRoute<?, ?>> _changeSet = RibDelta.builder();

  /* Hacky way to not re-init the process across topology computations. Not a permanent solution */
  private boolean _initialized = false;

  /**
   * Mapping from extended community route target patterns to VRF name. Used for determining where
   * to merge EVPN routes
   */
  @Nonnull private final Map<String, String> _rtVrfMapping;

  /** Mapping of routes to be redistributed. Maps source VRF to a set of routes to process */
  @Nonnull
  private Map<String, RibDelta<? extends AnnotatedRoute<AbstractRoute>>> _mainRibRoutesToProcess;

  @Nonnull private BgpDelta<EvpnType5Route> _type5RoutesToSendForEveryone;
  @Nonnull private Map<EdgeId, BgpDelta<EvpnType5Route>> _type5RoutesToSendPerNeighbor;

  /** Set of edges (sessions) that came up since previous topology update */
  private Set<EdgeId> _edgesWentUp = ImmutableSet.of();
  /**
   * Type 3 routes that were created locally (across all VRFs). Save them so that if new sessions
   * come up, we can easily send out the updates
   */
  @Nonnull private RibDelta<EvpnType3Route> _localType3Routes = RibDelta.empty();

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
    // TODO: really need to have a read-only RIB interface for safety
    _mainRib = mainRib;
    _topology = topology;
    _prefixTracer = prefixTracer;

    // Message queues start out empty
    _bgpv4IncomingRoutes = ImmutableSortedMap.of();
    _evpnType3IncomingRoutes = ImmutableSortedMap.of();
    _evpnType5IncomingRoutes = ImmutableSortedMap.of();

    // Initialize all RIBs
    BgpTieBreaker bestPathTieBreaker =
        firstNonNull(_process.getTieBreaker(), BgpTieBreaker.ARRIVAL_ORDER);
    MultipathEquivalentAsPathMatchMode multiPathMatchMode =
        firstNonNull(_process.getMultipathEquivalentAsPathMatchMode(), EXACT_PATH);
    _ebgpv4Rib =
        new Bgpv4Rib(
            _mainRib,
            bestPathTieBreaker,
            _process.getMultipathEbgp() ? null : 1,
            multiPathMatchMode,
            false);
    _ibgpv4Rib =
        new Bgpv4Rib(
            _mainRib,
            bestPathTieBreaker,
            _process.getMultipathIbgp() ? null : 1,
            multiPathMatchMode,
            false);
    _bgpv4Rib =
        new Bgpv4Rib(
            _mainRib,
            bestPathTieBreaker,
            _process.getMultipathEbgp() || _process.getMultipathIbgp() ? null : 1,
            multiPathMatchMode,
            false);
    _bgpv4DeltaBuilder = RibDelta.builder();

    _mainRibRoutesToProcess = new HashMap<>(1);

    _ebgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);
    _ibgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);
    // EVPN Ribs
    _ebgpType3EvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _ibgpType3EvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _evpnType3Rib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _ebgpType5EvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _ibgpType5EvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _evpnType5Rib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _evpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _evpnInitializationDelta = RibDelta.empty();
    _rtVrfMapping = computeRouteTargetToVrfMap(getAllPeerConfigs(_process));
    _type5RoutesToSendForEveryone = BgpDelta.empty();
    _type5RoutesToSendPerNeighbor = new HashMap<>(0);
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
    _bgpv4IncomingRoutes =
        getEdgeIdStream(graph, BgpPeerConfig::getIpv4UnicastAddressFamily, Type.IPV4_UNICAST)
            .collect(toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    // Create incoming message queues for sessions that exchange EVPN info
    _evpnType3IncomingRoutes =
        getEdgeIdStream(graph, BgpPeerConfig::getEvpnAddressFamily, Type.EVPN)
            .collect(toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    _evpnType5IncomingRoutes =
        getEdgeIdStream(graph, BgpPeerConfig::getEvpnAddressFamily, Type.EVPN)
            .collect(toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    assert _evpnType3IncomingRoutes.keySet().equals(_evpnType5IncomingRoutes.keySet());
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
    // Reinitialize staging RIBs, delta builders
    _bgpv4DeltaBuilder = RibDelta.builder();
    _evpnDeltaBuilder = RibDelta.builder();
    _changeSet = RibDelta.builder();
    BgpTieBreaker bestPathTieBreaker =
        firstNonNull(_process.getTieBreaker(), BgpTieBreaker.ARRIVAL_ORDER);
    MultipathEquivalentAsPathMatchMode multiPathMatchMode =
        firstNonNull(_process.getMultipathEquivalentAsPathMatchMode(), EXACT_PATH);
    _ebgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);
    _ibgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);

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

    // If we have any new edges, send out our RIB state to them.
    sendOutRoutesToNewEdges(_edgesWentUp, allNodes, nc);
    _edgesWentUp = ImmutableSet.of();

    // See computeType5DeltaFromMainRibRoutes for why this madness is needed
    // If we have main RIB routes to process (from any VRF) and we have EVPN neighbors, then
    // we make new type 5 routes and send them out
    if (!_mainRibRoutesToProcess.isEmpty() && !_evpnType5IncomingRoutes.isEmpty()) {
      for (String vrfName : _mainRibRoutesToProcess.keySet()) {
        computeType5DeltaFromMainRibRoutes(vrfName, _mainRibRoutesToProcess.get(vrfName), nc);
      }
      _mainRibRoutesToProcess = new HashMap<>(1);
    }
    if (!_type5RoutesToSendForEveryone.isEmpty()) {
      sendOutEvpnType5Routes(_type5RoutesToSendForEveryone, nc, allNodes);
    }
    _type5RoutesToSendForEveryone = BgpDelta.empty();
    if (!_type5RoutesToSendPerNeighbor.isEmpty()) {
      _type5RoutesToSendPerNeighbor
          .keySet()
          .forEach(
              edge -> {
                BgpPeerConfigId remoteConfigId = edge.tail();
                BgpSessionProperties session = getSessionProperties(_topology, edge);
                getNeighborBgpProcess(remoteConfigId, allNodes)
                    .enqueueEvpnType5Routes(
                        // Make sure to reverse the edge
                        edge.reverse(),
                        getEvpnTransformedRouteStream(
                            edge, _type5RoutesToSendPerNeighbor.get(edge), nc, allNodes, session));
              });
      _type5RoutesToSendPerNeighbor = new HashMap<>(0);
    }
    processBgpMessages(nc, allNodes);
  }

  private void sendOutRoutesToNewEdges(
      Set<EdgeId> edgesWentUp, Map<String, Node> allNodes, NetworkConfigurations nc) {
    if (edgesWentUp.isEmpty()) {
      return;
    }
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
    return _changeSet.build();
  }

  @Override
  public void redistribute(RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta) {
    redistribute(mainRibDelta, _vrfName);
  }

  /** Redistribute routes from {@code srcVrfName} into our VRF. */
  public void redistribute(
      RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta, String srcVrfName) {
    _mainRibRoutesToProcess.put(srcVrfName, mainRibDelta);
  }

  @Override
  public boolean isDirty() {
    return
    // Message queues
    !_bgpv4IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        || !_evpnType3IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        || !_evpnType5IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        // Delta builders
        || !_bgpv4DeltaBuilder.build().isEmpty()
        || !_evpnDeltaBuilder.build().isEmpty()
        // Initialization state
        || !_evpnInitializationDelta.isEmpty()
        // Intermediate state
        || !_type5RoutesToSendForEveryone.isEmpty()
        || !_type5RoutesToSendPerNeighbor.isEmpty();
  }

  /**
   * Process all incoming BGP messages: across all neighbors, across all address families.
   *
   * @param nc {@link NetworkConfigurations network configurations} wrapper
   * @param allNodes map of all network nodes
   */
  private void processBgpMessages(NetworkConfigurations nc, Map<String, Node> allNodes) {
    // Process EVPN messages and send out updates
    DeltaPair<EvpnType3Route> type3Delta = processEvpnType3Messages(nc, allNodes);
    DeltaPair<EvpnType5Route> type5Delta = processEvpnType5Messages(nc, allNodes);
    sendOutEvpnType3Routes(type3Delta._toAdvertise, nc, allNodes);
    sendOutEvpnType5Routes(type5Delta._toAdvertise, nc, allNodes);
    // Merge EVPN routes into EVPN RIB and prepare for merging into main RIB
    _changeSet.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType3Rib, type3Delta._toMerge._ebgpDelta)));
    _changeSet.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType3Rib, type3Delta._toMerge._ibgpDelta)));
    _changeSet.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType5Rib, type5Delta._toMerge._ebgpDelta)));
    _changeSet.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType5Rib, type5Delta._toMerge._ibgpDelta)));

    // TODO: migrate v4 route propagation here
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
        .flatMap(af -> Stream.concat(af.getL3VNIs().stream(), af.getL2VNIs().stream()))
        .forEach(
            vniConfig -> {
              Vrf vniVrf = _c.getVrfs().get(vniConfig.getVrf());
              assert vniVrf != null; // Invariant guaranteed by proper conversion
              // Layer 3 VNIs take precedence on most vendors
              Vni vniSettings = vniVrf.getLayer3Vnis().get(vniConfig.getVni());
              if (vniSettings == null) {
                vniSettings = vniVrf.getLayer2Vnis().get(vniConfig.getVni());
              }
              assert vniSettings != null; // Invariant guaranteed by proper conversion
              if (vniSettings.getSourceAddress() == null) {
                return;
              }
              EvpnType3Route route =
                  initEvpnType3Route(
                      ebgpAdmin,
                      vniSettings,
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
                    n.getVirtualRouters().get(vniVrf.getName()).getBgpRoutingProcess();
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
    _changeSet.from(_evpnDeltaBuilder.build());
  }

  /**
   * Create a new {@link EvpnType3Route} based on given {@link Layer2Vni}. Assumes Layer2Vni are
   * valid (e.g., have properly set source address).
   */
  @Nonnull
  @VisibleForTesting
  static EvpnType3Route initEvpnType3Route(
      int ebgpAdmin,
      Vni vniSettings,
      ExtendedCommunity routeTarget,
      RouteDistinguisher routeDistinguisher,
      Ip routerId) {
    checkArgument(
        vniSettings.getSourceAddress() != null,
        "Cannot construct type 3 route for invalid VNI %s",
        vniSettings.getVni());
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
    assert vniSettings.getSourceAddress() != null; // Conversion invariant
    type3RouteBuilder.setVniIp(vniSettings.getSourceAddress());

    return type3RouteBuilder.build();
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

  /** Process incoming EVPN type 5 messages, across all neighbors */
  private DeltaPair<EvpnType5Route> processEvpnType5Messages(
      NetworkConfigurations nc, Map<String, Node> allNodes) {
    DeltaPair<EvpnType5Route> deltaPair = DeltaPair.empty();
    for (Entry<EdgeId, Queue<RouteAdvertisement<EvpnType5Route>>> entry :
        _evpnType5IncomingRoutes.entrySet()) {
      EdgeId edge = entry.getKey();
      Queue<RouteAdvertisement<EvpnType5Route>> queue = entry.getValue();
      deltaPair =
          deltaPair.union(
              processEvpnMessagesFromNeighbor(edge, queue, nc, allNodes, EvpnType5Route.class));
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

  /** Send out EVPN type 5 routes to our neighbors */
  private void sendOutEvpnType5Routes(
      BgpDelta<EvpnType5Route> evpnDelta, NetworkConfigurations nc, Map<String, Node> allNodes) {
    _evpnType5IncomingRoutes
        .keySet()
        .forEach(
            edge -> {
              BgpPeerConfigId remoteConfigId = edge.tail();
              BgpSessionProperties session = getSessionProperties(_topology, edge);
              getNeighborBgpProcess(remoteConfigId, allNodes)
                  .enqueueEvpnType5Routes(
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
                        allNodes,
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
   * @param allNodes all nodes in the network
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
          Map<String, Node> allNodes,
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
            getNeighborBgpProcess(remoteConfigId, allNodes)._process,
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
    _changeSet.from(importRibDelta(_evpnRib, delta));
    return delta;
  }

  /**
   * Compute type 5 routes to send to neighbors from the main RIB v4 routes.
   *
   * <p>Affects {@link #_type5RoutesToSendForEveryone} and {@link #_type5RoutesToSendPerNeighbor}
   *
   * @param srcVrf the name of the <em>source</em> VRF (i.e., the VRF whose main RIB routes we are
   *     processing)
   */
  private void computeType5DeltaFromMainRibRoutes(
      String srcVrf,
      RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta,
      NetworkConfigurations nc) {
    RibDelta.Builder<EvpnType5Route> ebgpAll = RibDelta.builder();
    RibDelta.Builder<EvpnType5Route> ibgpAll = RibDelta.builder();
    for (EdgeId edge : _evpnType5IncomingRoutes.keySet()) {
      RibDelta.Builder<EvpnType5Route> ebgp = RibDelta.builder();
      RibDelta.Builder<EvpnType5Route> ibgp = RibDelta.builder();
      BgpPeerConfig ourConfig = nc.getBgpPeerConfig(edge.head());
      assert ourConfig != null;

      mainRibDelta
          .getActions()
          .forEach(
              ra -> {
                AbstractRoute route = ra.getRoute().getRoute();
                if (route instanceof Bgpv4Route) {
                  // These routes came into scrVrf's main RIB from the srcVrf's BGP RIB,
                  // so just convert them to type 5s and send to all neighbors
                  bgpv4RouteToType5Route(ebgpAll, ibgpAll, ourConfig, ra, route, srcVrf);
                } else if (!(route instanceof BgpRoute)) {
                  if (ourConfig.getIpv4UnicastAddressFamily() == null) {
                    return;
                  }
                  /*
                   * This is crap.
                   *
                   * Since we don't keep locally converted routes in BGP RIB,
                   * do a pretend export from abstract route to Bgpv4route,
                   * and then convert that to a type 5 route
                   *
                   * Note that our export policies are per-neighbor so we keep a mapping of
                   */
                  BgpPeerConfigId remoteConfigId = edge.tail();
                  BgpSessionProperties session = getSessionProperties(_topology, edge);
                  Bgpv4Route bgpv4Route =
                      exportNonBgpRouteToBgp(ra.getRoute(), remoteConfigId, ourConfig, session);
                  if (bgpv4Route != null) {
                    bgpv4RouteToType5Route(ebgp, ibgp, ourConfig, ra, bgpv4Route, srcVrf);
                  }
                }
              });

      BgpDelta<EvpnType5Route> existingDelta = _type5RoutesToSendPerNeighbor.get(edge);
      BgpDelta<EvpnType5Route> computedDelta = new BgpDelta<>(ebgp.build(), ibgp.build());
      if (existingDelta == null) {
        _type5RoutesToSendPerNeighbor.put(edge, computedDelta);
      } else {
        _type5RoutesToSendPerNeighbor.put(edge, existingDelta.union(computedDelta));
      }
    }
    // Since this function is called in a loop, union results to existing
    _type5RoutesToSendForEveryone =
        _type5RoutesToSendForEveryone.union(new BgpDelta<>(ebgpAll.build(), ibgpAll.build()));
  }

  private static void bgpv4RouteToType5Route(
      Builder<EvpnType5Route> ebgpDeltaBuilder,
      Builder<EvpnType5Route> ibgpDeltaBuilder,
      BgpPeerConfig ourConfig,
      RouteAdvertisement<? extends AnnotatedRoute<AbstractRoute>> ra,
      AbstractRoute route,
      String srcVrfName) {
    if (ourConfig.getEvpnAddressFamily() == null) {
      return;
    }
    ImmutableSet<Layer3VniConfig> allVniConfigs =
        ourConfig.getEvpnAddressFamily().getL3VNIs().stream()
            // Only advertise routes if permitted by VNI config
            .filter(layer3VniConfig -> layer3VniConfig.getVrf().equals(srcVrfName))
            .filter(Layer3VniConfig::getAdvertiseV4Unicast)
            .collect(ImmutableSet.toImmutableSet());
    for (VniConfig config : allVniConfigs) {
      if (route.getProtocol() == RoutingProtocol.BGP) {
        if (ra.isWithdrawn()) {
          ebgpDeltaBuilder.remove(
              toEvpnType5Route(
                  (Bgpv4Route) route, config.getRouteDistinguisher(), config.getRouteTarget()),
              ra.getReason());
        } else {
          ebgpDeltaBuilder.add(
              toEvpnType5Route(
                  (Bgpv4Route) route, config.getRouteDistinguisher(), config.getRouteTarget()));
        }
      } else if (route.getProtocol() == RoutingProtocol.IBGP) {
        if (ra.isWithdrawn()) {
          ibgpDeltaBuilder.remove(
              toEvpnType5Route(
                  (Bgpv4Route) route, config.getRouteDistinguisher(), config.getRouteTarget()),
              ra.getReason());
        } else {
          ibgpDeltaBuilder.add(
              toEvpnType5Route(
                  (Bgpv4Route) route, config.getRouteDistinguisher(), config.getRouteTarget()));
        }
      }
    }
  }

  /**
   * Convert a BGP v4 route to a EVPN type 5 route. Resets most original attributes of the route,
   * since
   */
  private static EvpnType5Route toEvpnType5Route(
      Bgpv4Route route, RouteDistinguisher rd, ExtendedCommunity rt) {
    return EvpnType5Route.builder()
        .setNetwork(route.getNetwork())
        .setAdmin(route.getAdministrativeCost())
        // Intentionally skip AS-path and communities -- we are generating a new route
        .addCommunity(rt) // add route target
        .setLocalPreference(route.getLocalPreference())
        .setMetric(route.getMetric())
        .setNextHopInterface(route.getNextHopInterface())
        .setNextHopIp(route.getNextHopIp())
        .setOriginatorIp(route.getNextHopIp())
        .setOriginType(route.getOriginType())
        .setProtocol(route.getProtocol())
        .setReceivedFromIp(route.getReceivedFromIp())
        .setReceivedFromRouteReflectorClient(route.getReceivedFromRouteReflectorClient())
        .setRouteDistinguisher(rd)
        .setSrcProtocol(route.getSrcProtocol())
        .build();
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
            // Message queues
            _bgpv4IncomingRoutes,
            _evpnType3IncomingRoutes,
            _evpnType5IncomingRoutes,
            // Delta builders
            _bgpv4DeltaBuilder.build(),
            _evpnDeltaBuilder.build(),
            // intermediate state
            _type5RoutesToSendForEveryone,
            _type5RoutesToSendPerNeighbor)
        .collect(toOrderedHashCode());
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

  /**
   * Message passing method between BGP processes. Take a collection of BGP {@link
   * RouteAdvertisement}s and puts them onto a local queue corresponding to the session between
   * given neighbors.
   */
  void enqueueBgpv4Routes(
      @Nonnull EdgeId edgeId, @Nonnull Collection<RouteAdvertisement<Bgpv4Route>> routes) {
    Queue<RouteAdvertisement<Bgpv4Route>> q = _bgpv4IncomingRoutes.get(edgeId);
    assert q != null; // Invariant of the session being up
    q.addAll(routes);
  }

  /**
   * Message passing method between BGP processes. Take a stream of BGP {@link RouteAdvertisement}s
   * and puts them onto a local queue corresponding to the session between given neighbors.
   */
  void enqueueBgpMessages(
      @Nonnull EdgeId edgeId, @Nonnull Stream<RouteAdvertisement<Bgpv4Route>> routes) {
    Queue<RouteAdvertisement<Bgpv4Route>> q = _bgpv4IncomingRoutes.get(edgeId);
    assert q != null; // Invariant of the session being up
    routes.forEach(q::add);
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

  /**
   * Message passing method between BGP processes. Take a collection of BGP {@link
   * RouteAdvertisement}s and puts them onto a local queue corresponding to the session between
   * given neighbors.
   */
  private void enqueueEvpnType5Routes(
      @Nonnull EdgeId edgeId, @Nonnull Stream<RouteAdvertisement<EvpnType5Route>> routes) {
    Queue<RouteAdvertisement<EvpnType5Route>> q = _evpnType5IncomingRoutes.get(edgeId);
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
            .getVirtualRouters()
            .get(id.getVrfName())
            .getBgpRoutingProcess();
    assert proc != null; // Otherwise our computation is really wrong
    return proc;
  }

  /** Return a BGP routing process for a sibling VRF on our node */
  @Nonnull
  private BgpRoutingProcess getVrfProcess(String vrf, Map<String, Node> allNodes) {
    BgpRoutingProcess proc =
        allNodes.get(_c.getHostname()).getVirtualRouters().get(vrf).getBgpRoutingProcess();
    assert proc != null;
    return proc;
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
