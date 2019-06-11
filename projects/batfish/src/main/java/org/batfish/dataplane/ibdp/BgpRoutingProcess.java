package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
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
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.dataplane.protocols.BgpProtocolHelper;
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

  /** eBGP rib for EVPN type 3 routes */
  private EvpnRib<EvpnType3Route> _ebgpType3EvpnRib;
  /** iBGP rib for EVPN type 3 routes */
  private EvpnRib<EvpnType3Route> _ibgpType3EvpnRib;
  /** Rib for EVPN type 3 routes */
  @Nonnull private EvpnRib<EvpnType3Route> _evpnType3Rib;
  /** Combined EVPN RIB for e/iBGP across all route types */
  @Nonnull EvpnRib<EvpnRoute<?, ?>> _evpnRib;
  /** Builder for constructing {@link RibDelta} for routes in {@link #_evpnRib} */
  @Nonnull private Builder<EvpnRoute<?, ?>> _evpnDeltaBuilder = RibDelta.builder();

  private RibDelta<EvpnType3Route> _evpnInitializationDelta;

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
    _ebgpType3EvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _ibgpType3EvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _evpnType3Rib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _evpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
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
    // Create incoming message queues for sessions that exchange EVPN info
    _evpnType3IncomingRoutes =
        Streams.concat(
                _process.getActiveNeighbors().entrySet().stream()
                    .filter(e -> e.getValue().getEvpnAddressFamily() != null)
                    .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey(), false)),
                _process.getPassiveNeighbors().entrySet().stream()
                    .filter(e -> e.getValue().getEvpnAddressFamily() != null)
                    .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey(), true)),
                _process.getInterfaceNeighbors().entrySet().stream()
                    .filter(e -> e.getValue().getEvpnAddressFamily() != null)
                    .map(e -> new BgpPeerConfigId(_c.getHostname(), _vrfName, e.getKey())))
            .filter(graph.nodes()::contains)
            .flatMap(dst -> graph.adjacentNodes(dst).stream().map(src -> new EdgeId(src, dst)))
            .collect(toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
  }

  @Override
  public void updateTopology(BgpTopology topology) {
    _topology = topology;
    initBgpQueues(_topology);
    // TODO: for any sessions that are brand new, send out the current state of our bgp RIB to them.
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
      sendOutEvpnRoutes(new BgpDelta<>(_evpnInitializationDelta, RibDelta.empty()), nc, allNodes);
      _evpnInitializationDelta = RibDelta.empty();
    }
    processBgpMessages(nc, allNodes);
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
        || !_evpnDeltaBuilder.build().isEmpty()
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
    // Process EVPN messages and send out updates
    DeltaPair<EvpnType3Route> type3Delta = processEvpnType3Messages(nc, allNodes);
    sendOutEvpnRoutes(type3Delta._toAdvertise, nc, allNodes);
    // Merge EVPN routes into EVPN RIB and prepare for merging into main RIB
    _changeSet.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType3Rib, type3Delta._toMerge._ebgpDelta)));
    _changeSet.from(
        importRibDelta(_evpnRib, importRibDelta(_evpnType3Rib, type3Delta._toMerge._ibgpDelta)));

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
              VniSettings vniSettings = vniVrf.getVniSettings().get(vniConfig.getVni());
              assert vniSettings != null; // Invariant guaranteed by proper conversion
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
                initializationBuilder.from(
                    n.getVirtualRouters()
                        .get(vniVrf.getName())
                        .getBgpRoutingProcess()
                        .processCrossVrfEvpnRoute(new RouteAdvertisement<>(route)));
              }
            });
    _evpnInitializationDelta = initializationBuilder.build();
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

  /** Process all incoming EVPN messages, across all neighbors */
  private DeltaPair<EvpnType3Route> processEvpnType3Messages(
      NetworkConfigurations nc, Map<String, Node> allNodes) {
    DeltaPair<EvpnType3Route> deltaPair = DeltaPair.empty();
    for (Entry<EdgeId, Queue<RouteAdvertisement<EvpnType3Route>>> entry :
        _evpnType3IncomingRoutes.entrySet()) {
      EdgeId edge = entry.getKey();
      Queue<RouteAdvertisement<EvpnType3Route>> queue = entry.getValue();
      deltaPair = deltaPair.union(processEvpnMessagesFromNeighbor(edge, queue, nc, allNodes));
    }
    return deltaPair;
  }

  /** Process all incoming EVPN messages for a given session, identified by {@code edge} */
  private DeltaPair<EvpnType3Route> processEvpnMessagesFromNeighbor(
      EdgeId edge,
      Queue<RouteAdvertisement<EvpnType3Route>> queue,
      NetworkConfigurations nc,
      Map<String, Node> allNodes) {
    BgpPeerConfigId ourConfigId = edge.head();
    BgpPeerConfig ourBgpConfig = nc.getBgpPeerConfig(ourConfigId);
    assert ourBgpConfig != null; // because the edge exists
    // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
    BgpSessionProperties sessionProperties = getSessionProperties(_topology, edge);
    Ip remoteIp = sessionProperties.getTailIp();
    EvpnRib<EvpnType3Route> targetRib =
        sessionProperties.isEbgp() ? _ebgpType3EvpnRib : _ibgpType3EvpnRib;
    RibDelta.Builder<EvpnType3Route> toAdvertise = RibDelta.builder();
    RibDelta.Builder<EvpnType3Route> toMerge = RibDelta.builder();
    while (!queue.isEmpty()) {
      RouteAdvertisement<EvpnType3Route> routeAdvertisement = queue.remove();
      EvpnType3Route route = routeAdvertisement.getRoute();
      EvpnType3Route.Builder transformedBuilder =
          transformBgpRouteOnImport(
              route,
              ourBgpConfig.getLocalAs(),
              ourBgpConfig.getAllowLocalAsIn(),
              sessionProperties.isEbgp(),
              _process,
              ourConfigId.getPeerInterface());
      if (transformedBuilder == null) {
        continue;
      }

      // Process route through import policy, if one exists
      String importPolicyName = ourBgpConfig.getImportPolicy();
      boolean acceptIncoming = true;
      if (importPolicyName != null) {
        RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
        if (importPolicy != null) {
          acceptIncoming =
              importPolicy.process(
                  route,
                  transformedBuilder,
                  remoteIp,
                  ourConfigId.getRemotePeerPrefix(),
                  _vrfName,
                  IN);
        }
      }
      if (!acceptIncoming) {
        continue;
      }
      EvpnType3Route transformedRoute = transformedBuilder.build();
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
          RibDelta<EvpnType3Route> d = targetRib.mergeRouteGetDelta(transformedRoute);
          toAdvertise.from(d);
          toMerge.from(d);
        } else {
          // Merge into other VRF's RIB and put into re-advertisement delta
          toAdvertise.from(
              getVrfProcess(targetVrf.get(), allNodes)
                  .processCrossVrfEvpnRoute(
                      routeAdvertisement.toBuilder().setRoute(transformedRoute).build()));
        }
      } else {
        // Simply propagate to neighbors, nothing to do locally
        toAdvertise.from(routeAdvertisement);
      }
    }

    BgpDelta<EvpnType3Route> advertiseDelta =
        sessionProperties.isEbgp()
            ? new BgpDelta<>(toAdvertise.build(), RibDelta.empty())
            : new BgpDelta<>(RibDelta.empty(), toAdvertise.build());
    BgpDelta<EvpnType3Route> mergeDelta =
        sessionProperties.isEbgp()
            ? new BgpDelta<>(toMerge.build(), RibDelta.empty())
            : new BgpDelta<>(RibDelta.empty(), toMerge.build());
    return new DeltaPair<>(advertiseDelta, mergeDelta);
  }

  /** Send out EVPN routes to our neighbors */
  private void sendOutEvpnRoutes(
      BgpDelta<EvpnType3Route> evpnDelta, NetworkConfigurations nc, Map<String, Node> allNodes) {
    _evpnType3IncomingRoutes
        .keySet()
        .forEach(
            edge -> {
              BgpPeerConfigId remoteConfigId = edge.tail();
              BgpPeerConfigId ourConfigId = edge.head();
              BgpPeerConfig ourConfig = nc.getBgpPeerConfig(ourConfigId);
              BgpPeerConfig remoteConfig = nc.getBgpPeerConfig(remoteConfigId);
              assert ourConfig != null; // Invariant of the edge existing
              assert remoteConfig != null; // Invariant of the edge existing
              BgpSessionProperties session = getSessionProperties(_topology, edge);
              getNeighborBgpProcess(remoteConfigId, allNodes)
                  // TODO: take into account address-family session settings, such as add-path or
                  //   advertise-inactive
                  .enqueueEvpnRoutes(
                      // Make sure to reverse the edge
                      edge.reverse(),
                      Stream.concat(
                              evpnDelta._ebgpDelta.getActions(), evpnDelta._ibgpDelta.getActions())
                          .map(
                              adv ->
                                  transformBgpRouteOnExport(
                                          adv.getRoute(),
                                          ourConfigId,
                                          remoteConfigId,
                                          ourConfig,
                                          remoteConfig,
                                          allNodes,
                                          session)
                                      .map(
                                          r ->
                                              RouteAdvertisement.<EvpnType3Route>builder()
                                                  .setReason(
                                                      adv.getReason() == Reason.REPLACE
                                                          ? Reason.WITHDRAW
                                                          : adv.getReason())
                                                  .setRoute(r)
                                                  .build()))
                          .filter(Optional::isPresent)
                          .map(Optional::get));
            });
  }

  /**
   * Given an {@link AbstractRoute}, run it through the BGP outbound transformations and export
   * routing policy.
   *
   * @param exportCandidate a route to try and export
   * @param ourConfig {@link BgpPeerConfig} that sends the route
   * @param remoteConfig {@link BgpPeerConfig} that will be receiving the route
   * @param allNodes all nodes in the network
   * @param sessionProperties {@link BgpSessionProperties} representing the <em>incoming</em> edge:
   *     i.e. the edge from {@code remoteConfig} to {@code ourConfig}
   * @return The transformed route as a {@link Bgpv4Route}, or {@code null} if the route should not
   *     be exported.
   */
  private <B extends EvpnRoute.Builder<B, R>, R extends EvpnRoute<B, R>>
      Optional<R> transformBgpRouteOnExport(
          @Nonnull BgpRoute<B, R> exportCandidate,
          @Nonnull BgpPeerConfigId ourConfigId,
          @Nonnull BgpPeerConfigId remoteConfigId,
          @Nonnull BgpPeerConfig ourConfig,
          @Nonnull BgpPeerConfig remoteConfig,
          @Nonnull Map<String, Node> allNodes,
          @Nonnull BgpSessionProperties sessionProperties) {
    // TODO: bring back prefix tracing

    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(ourConfig.getExportPolicy());
    B transformedOutgoingRouteBuilder =
        BgpProtocolHelper.transformBgpRoutePreExport(
            ourConfig,
            remoteConfig,
            sessionProperties,
            _process,
            getNeighborBgpProcess(remoteConfigId, allNodes)._process,
            exportCandidate);

    if (transformedOutgoingRouteBuilder == null) {
      // This route could not be exported for core bgp protocol reasons
      return Optional.empty();
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
      return Optional.empty();
    }
    // Apply final post-policy transformations before sending advertisement to neighbor
    BgpProtocolHelper.transformBgpRoutePostExport(
        transformedOutgoingRouteBuilder, ourConfig, sessionProperties);
    // Successfully exported route
    R transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
    return Optional.of(transformedOutgoingRoute);
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
  private void enqueueEvpnRoutes(
      @Nonnull EdgeId edgeId, @Nonnull Stream<RouteAdvertisement<EvpnType3Route>> routes) {
    Queue<RouteAdvertisement<EvpnType3Route>> q = _evpnType3IncomingRoutes.get(edgeId);
    assert q != null; // Invariant of the session being up
    routes.forEach(q::add);
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

    private BgpDelta<R> union(BgpDelta<R> other) {
      return new BgpDelta<>(
          RibDelta.<R>builder().from(_ebgpDelta).from(other._ebgpDelta).build(),
          RibDelta.<R>builder().from(_ibgpDelta).from(other._ibgpDelta).build());
    }

    @SuppressWarnings("unchecked") // fully variant, will never store any Ts
    @Nonnull
    private static <B extends BgpRoute.Builder<B, R>, R extends BgpRoute<B, R>>
        BgpDelta<R> empty() {
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
}
