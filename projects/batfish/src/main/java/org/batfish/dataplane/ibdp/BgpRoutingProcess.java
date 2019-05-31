package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.common.util.CollectionUtil.toOrderedHashCode;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.dataplane.rib.RibDelta.importRibDelta;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Streams;
import com.google.common.graph.ValueGraph;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
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
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.dataplane.rib.Bgpv4Rib;
import org.batfish.dataplane.rib.EvpnRib;
import org.batfish.dataplane.rib.Rib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RouteAdvertisement;

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
  @Nonnull
  private SortedMap<EdgeId, Queue<RouteAdvertisement<EvpnType3Route>>> _evpnType3IncomingRoutes;

  // RIBs and RIB delta builders
  /** Helper RIB containing all paths obtained with external BGP, for IPv4 unicast */
  @Nonnull Bgpv4Rib _ebgpv4Rib;
  /**
   * Helper RIB containing paths obtained with external eBGP during current iteration. An Adj-RIB-in
   * of sorts for IPv4 unicast
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

  /** Rib for EVPN type 3 routes */
  @Nonnull private EvpnRib<EvpnType3Route> _evpnType3Rib;
  /** Combined EVPN RIB for e/iBGP across all route types */
  @Nonnull EvpnRib<EvpnRoute<?, ?>> _evpnRib;
  /** Builder for constructing {@link RibDelta} for routes in {@link #_evpnRib} */
  @Nonnull private Builder<EvpnRoute<?, ?>> _evpnDeltaBuilder = RibDelta.builder();

  /** Delta builder for routes that must be propagated to the main RIB */
  @Nonnull private RibDelta.Builder<BgpRoute<?, ?>> _changeSet = RibDelta.builder();

  /* Hacky way to non re-init the process across topology computations. Not a permanent solution */
  private boolean _initialized = false;

  @Nonnull private final Map<String, String> _rtVrfMapping;

  /**
   * Create a new BGP process
   *
   * @param process the {@link BgpProcess} -- configuration for this routing process
   * @param configuration the parent {@link Configuration}
   * @param vrfName name of the VRF this process is in
   * @param mainRib take in a reference to MainRib for read-only use (e.g., getting IGP cost to
   *     next-hop)
   */
  BgpRoutingProcess(
      BgpProcess process,
      Configuration configuration,
      String vrfName,
      Rib mainRib,
      BgpTopology topology) {
    _process = process;
    _c = configuration;
    _vrfName = vrfName;
    // TODO: really need to have a read-only RIB interface for safety
    _mainRib = mainRib;
    _topology = topology;

    // Message queues start out empty
    _bgpv4IncomingRoutes = ImmutableSortedMap.of();
    _evpnType3IncomingRoutes = ImmutableSortedMap.of();

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

    _ebgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);
    _ibgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);
    // EVPN Ribs
    _evpnType3Rib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _evpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
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
        .flatMap(af -> af.getL3VNIs().stream())
        .forEach(l3vni -> rtVrfMappingBuilder.put(l3vni.getImportRouteTarget(), l3vni.getVrf()));
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
   * Initialize incoming BGP message queues.
   *
   * @param bgpTopology source of truth for which sessions get established.
   */
  private void initBgpQueues(BgpTopology bgpTopology) {
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> graph = bgpTopology.getGraph();
    _bgpv4IncomingRoutes =
        Streams.concat(
                _process.getActiveNeighbors().entrySet().stream()
                    .filter(e -> e.getValue().getIpv4UnicastAddressFamily() != null)
                    .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey(), false)),
                _process.getPassiveNeighbors().entrySet().stream()
                    .filter(e -> e.getValue().getIpv4UnicastAddressFamily() != null)
                    .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey(), true)),
                _process.getInterfaceNeighbors().entrySet().stream()
                    .filter(e -> e.getValue().getIpv4UnicastAddressFamily() != null)
                    .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey())))
            .filter(graph.nodes()::contains)
            .flatMap(dst -> graph.adjacentNodes(dst).stream().map(src -> new EdgeId(src, dst)))
            .collect(toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
  }

  @Override
  public void updateTopology(BgpTopology topology) {
    _topology = topology;
    initBgpQueues(_topology);
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
  }

  @Nonnull
  @Override
  public RibDelta<BgpRoute<?, ?>> getUpdatesForMainRib() {
    return _changeSet.build();
  }

  @Override
  public void redistribute(RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta) {}

  @Override
  public boolean isDirty() {
    return
    // Message queues
    !_bgpv4IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        || !_evpnType3IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        // Delta builders
        || !_bgpv4DeltaBuilder.build().isEmpty()
        || !_evpnDeltaBuilder.build().isEmpty();
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
        .flatMap(af -> af.getL3VNIs().stream())
        .forEach(
            layer3VniConfig -> {
              Vrf vniVrf = _c.getVrfs().get(layer3VniConfig.getVrf());
              assert vniVrf != null; // Invariant guaranteed by proper conversion
              VniSettings vniSettings = vniVrf.getVniSettings().get(layer3VniConfig.getVni());
              assert vniSettings != null; // Invariant guaranteed by proper conversion
              EvpnType3Route route =
                  initEvpnType3Route(
                      ebgpAdmin,
                      vniSettings,
                      layer3VniConfig.getRouteTarget(),
                      layer3VniConfig.getRouteDistinguisher(),
                      _process.getRouterId());

              if (vniVrf.getName().equals(_vrfName)) {
                RibDelta<EvpnType3Route> d = _evpnType3Rib.mergeRouteGetDelta(route);
                _evpnDeltaBuilder.from(d);
                initializationBuilder.from(d);
              } else {
                initializationBuilder.from(
                    n.getVirtualRouters()
                        .get(vniVrf.getName())
                        .getBgpRoutingProcess()
                        .processCrossVrfEvpnRoute(new RouteAdvertisement<>(route)));
              }
            });
    _changeSet.from(_evpnDeltaBuilder.build());
  }

  @Nonnull
  static EvpnType3Route initEvpnType3Route(
      int ebgpAdmin,
      VniSettings vniSettings,
      ExtendedCommunity routeTarget,
      RouteDistinguisher routeDistinguisher,
      Ip routerId) {
    // Locally all routes start as eBGP routes in our own RIB
    EvpnType3Route.Builder type3RouteBuilder = EvpnType3Route.builder();
    type3RouteBuilder.setAdmin(ebgpAdmin);
    type3RouteBuilder.setCommunities(ImmutableSet.of(routeTarget));
    type3RouteBuilder.setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE);
    type3RouteBuilder.setOriginatorIp(routerId);
    type3RouteBuilder.setOriginType(OriginType.EGP);
    type3RouteBuilder.setProtocol(RoutingProtocol.BGP);
    type3RouteBuilder.setRouteDistinguisher(routeDistinguisher);
    type3RouteBuilder.setVniIp(vniSettings.getSourceAddress());

    return type3RouteBuilder.build();
  }

  /**
   * Process EVPN routes that were received on a session in a different VRF, but must be merged into
   * our VRF
   */
  @Nonnull
  private synchronized RibDelta<EvpnType3Route> processCrossVrfEvpnRoute(
      RouteAdvertisement<EvpnType3Route> routeAdvertisement) {
    // TODO: consider switching return value to BgpDelta to differentiate e/iBGP
    RibDelta<EvpnType3Route> delta;
    if (routeAdvertisement.isWithdrawn()) {
      delta =
          _evpnType3Rib.removeRouteGetDelta(
              routeAdvertisement.getRoute(), routeAdvertisement.getReason());
    } else {
      delta = _evpnType3Rib.mergeRouteGetDelta(routeAdvertisement.getRoute());
    }
    // Queue up the routes to be merged into our main RIB
    _changeSet.from(importRibDelta(_evpnRib, delta));
    return delta;
  }

  int iterationHashCode() {
    return Stream.of(
            // RIBs
            _bgpv4Rib.getTypedRoutes(),
            _evpnRib.getTypedRoutes(),
            // Message queues
            _bgpv4IncomingRoutes,
            _evpnType3IncomingRoutes,
            // Delta builders
            _bgpv4DeltaBuilder.build(),
            _evpnDeltaBuilder.build())
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
  void enqueueBgpMessages(
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

  /** Return a BGP routing process for a sibling VRF on the our node */
  @Nonnull
  private BgpRoutingProcess getVrfProcess(String vrf, Map<String, Node> allNodes) {
    BgpRoutingProcess proc =
        allNodes.get(_c.getHostname()).getVirtualRouters().get(vrf).getBgpRoutingProcess();
    assert proc != null;
    return proc;
  }
}
