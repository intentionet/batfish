package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.common.util.CollectionUtil.toOrderedHashCode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Streams;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteBuilder;
import org.batfish.datamodel.AbstractRouteDecorator;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfInternalSummaryRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.OspfSessionProperties;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.ospf.OspfTopology.EdgeId;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.route.nh.NextHopDiscard;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.dataplane.protocols.GeneratedRouteHelper;
import org.batfish.dataplane.rib.AbstractRib;
import org.batfish.dataplane.rib.OspfExternalType1Rib;
import org.batfish.dataplane.rib.OspfExternalType2Rib;
import org.batfish.dataplane.rib.OspfInterAreaRib;
import org.batfish.dataplane.rib.OspfInternalSummaryRib;
import org.batfish.dataplane.rib.OspfIntraAreaRib;
import org.batfish.dataplane.rib.OspfRib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/** An OSPF routing process, a dataplane version of {@link OspfProcess} */
@ParametersAreNonnullByDefault
final class OspfRoutingProcess implements RoutingProcess<OspfTopology, OspfRoute> {
  /* Provided configuration */
  /** The configuration/datamodel process */
  @Nonnull private final OspfProcess _process;
  /** The parent {@link Configuration} */
  @Nonnull private final Configuration _c;
  /** The name of the VRF we are in */
  @Nonnull private final String _vrfName;
  /** The current known topology */
  @Nonnull private OspfTopology _topology;
  /** The area summary filter for each area, if present. */
  @Nonnull private final Map<Long, RouteFilterList> _areaFilters;

  /* Computed configuration & cached variables */
  private final boolean _useMinMetricForSummaries;
  /** Export policy for external routes */
  @Nonnull private final RoutingPolicy _exportPolicy;

  /* Internal RIBs */
  @Nonnull private final OspfIntraAreaRib _intraAreaRib;
  @Nonnull private final OspfInterAreaRib _interAreaRib;
  @Nonnull private final OspfInternalSummaryRib _internalSummaryRib;
  @Nonnull private final OspfExternalType1Rib _type1Rib;
  @Nonnull private final OspfExternalType2Rib _type2Rib;
  @Nonnull private final OspfRib _ospfRib;

  /* Message queues */
  @Nonnull
  private SortedMap<OspfTopology.EdgeId, Queue<RouteAdvertisement<OspfIntraAreaRoute>>>
      _intraAreaIncomingRoutes = ImmutableSortedMap.of();

  @Nonnull
  private SortedMap<EdgeId, Queue<RouteAdvertisement<OspfInterAreaRoute>>>
      _interAreaIncomingRoutes = ImmutableSortedMap.of();

  @Nonnull
  private SortedMap<OspfTopology.EdgeId, Queue<RouteAdvertisement<OspfExternalType1Route>>>
      _type1IncomingRoutes = ImmutableSortedMap.of();

  @Nonnull
  private SortedMap<OspfTopology.EdgeId, Queue<RouteAdvertisement<OspfExternalType2Route>>>
      _type2IncomingRoutes = ImmutableSortedMap.of();

  /* State we need to maintain between iterations */

  /** Delta that captures process initialization (creating intra-area routes based on interfaces) */
  @Nonnull private InternalDelta _initializationDelta;
  /** Delta to pass to the main RIB */
  @Nonnull private RibDelta.Builder<OspfRoute> _changeset;
  /** Delta of routes we have locally queued for re-distribution */
  @Nonnull private ExternalDelta _queuedForRedistribution;
  /**
   * Delta of external routes we have activated in current iteration (but haven't advertised yet)
   */
  @Nonnull private RibDelta<OspfExternalRoute> _activatedGeneratedRoutes;

  /**
   * If this router is an ABR, keep track neighbors into which a default route was injected, since
   * the default route needs to be injected only once
   */
  @Nonnull private Set<OspfNeighborConfigId> _neighborsWhereDefaultIARouteWasInjected;

  OspfRoutingProcess(
      OspfProcess process, String vrfName, Configuration configuration, OspfTopology topology) {
    _c = configuration;
    _vrfName = vrfName;
    _process = process;
    _areaFilters =
        _process.getAreas().values().stream()
            .map(
                a ->
                    Optional.ofNullable(a.getSummaryFilter())
                        .map(_c.getRouteFilterLists()::get)
                        .map(l -> new SimpleEntry<>(a.getAreaNumber(), l))
                        .orElse(null))
            .filter(Objects::nonNull)
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    _topology = topology;

    _intraAreaRib = new OspfIntraAreaRib();
    _interAreaRib = new OspfInterAreaRib();
    _internalSummaryRib = new OspfInternalSummaryRib();
    _type1Rib = new OspfExternalType1Rib(_c.getHostname());
    _type2Rib = new OspfExternalType2Rib(_c.getHostname());
    _ospfRib = new OspfRib();

    updateQueues(topology);

    // Determine whether to use min metric by default, based on RFC1583 compatibility setting.
    // Routers (at least Cisco and Juniper) default to min metric unless using RFC2328 with
    // RFC1583 compatibility explicitly disabled, in which case they default to max.
    _useMinMetricForSummaries = firstNonNull(process.getRfc1583Compatible(), Boolean.TRUE);

    // Figure out what the export policy is. If undefined, fail closed -- export nothing.
    String exportPolicy = _process.getExportPolicy();
    if (exportPolicy == null || !_c.getRoutingPolicies().containsKey(exportPolicy)) {
      _exportPolicy =
          // Can't use the builder, because that attempts to modify configuration. grrr.
          new RoutingPolicy(
              String.format("~Drop_All_OSPF_External_%s~", _process.getProcessId()), _c);
      _exportPolicy.setStatements(ImmutableList.of(Statements.ExitReject.toStaticStatement()));
    } else {
      _exportPolicy = _c.getRoutingPolicies().get(exportPolicy);
    }

    _changeset = RibDelta.builder();
    _initializationDelta = new InternalDelta(RibDelta.empty(), RibDelta.empty(), RibDelta.empty());
    _queuedForRedistribution = new ExternalDelta();
    _activatedGeneratedRoutes = RibDelta.empty();
    _neighborsWhereDefaultIARouteWasInjected = new HashSet<>(0);
  }

  @Override
  public void initialize(Node n) {
    initializeIntraAreaRoutes();
  }

  @Override
  public void executeIteration(Map<String, Node> allNodes) {
    if (!_initializationDelta.isEmpty()) {
      // If we haven't sent out the first round of updates after initialization, do so now. Then
      // clear the initialization delta
      sendOutInternalRoutes(_initializationDelta, allNodes, _topology);
      _initializationDelta =
          new InternalDelta(RibDelta.empty(), RibDelta.empty(), RibDelta.empty());
    }

    // Process internal routes
    InternalDelta internalDelta = processInternalRoutes();
    sendOutInternalRoutes(internalDelta, allNodes, _topology);

    // Send out anything we had queued for redistribution
    sendOutExternalRoutes(_queuedForRedistribution, allNodes, _topology);
    _queuedForRedistribution = new ExternalDelta();

    // Process new external routes and re-advertise them as necessary
    ExternalDelta externalDelta = processExternalRoutes();
    sendOutExternalRoutes(externalDelta, allNodes, _topology);

    // Re-advertise activated generated routes, clear delta
    sendOutActiveGeneratedRoutes(allNodes, _topology);
    _activatedGeneratedRoutes = RibDelta.empty();

    // Keep track of what what updates will go into the main RIB
    _changeset.from(RibDelta.importRibDelta(_ospfRib, internalDelta._intraArea));
    _changeset.from(RibDelta.importRibDelta(_ospfRib, internalDelta._interArea));
    _changeset.from(RibDelta.importRibDelta(_ospfRib, internalDelta._internalSummary));
    _changeset.from(RibDelta.importRibDelta(_ospfRib, externalDelta._type1));
    _changeset.from(RibDelta.importRibDelta(_ospfRib, externalDelta._type2));
  }

  /** Update incoming message queues based on a new topology */
  private void updateQueues(OspfTopology topology) {
    // Preserve existing intra-area queues
    ImmutableSortedMap.Builder<EdgeId, Queue<RouteAdvertisement<OspfIntraAreaRoute>>>
        intraAreaBuilder = ImmutableSortedMap.naturalOrder();
    intraAreaBuilder.putAll(_intraAreaIncomingRoutes);
    // Preserve existing inter-area queues
    ImmutableSortedMap.Builder<EdgeId, Queue<RouteAdvertisement<OspfInterAreaRoute>>>
        interAreaBuilder = ImmutableSortedMap.naturalOrder();
    interAreaBuilder.putAll(_interAreaIncomingRoutes);
    // Preserve existing type1 queues
    ImmutableSortedMap.Builder<EdgeId, Queue<RouteAdvertisement<OspfExternalType1Route>>>
        type1Builder = ImmutableSortedMap.naturalOrder();
    type1Builder.putAll(_type1IncomingRoutes);
    // Preserve existing type2 queues
    ImmutableSortedMap.Builder<EdgeId, Queue<RouteAdvertisement<OspfExternalType2Route>>>
        type2Builder = ImmutableSortedMap.naturalOrder();
    type2Builder.putAll(_type2IncomingRoutes);

    getIncomingEdgeStream(topology)
        .forEach(
            edgeId -> {
              if (!_intraAreaIncomingRoutes.containsKey(edgeId)) {
                intraAreaBuilder.put(edgeId, new ConcurrentLinkedQueue<>());
              }
              if (!_interAreaIncomingRoutes.containsKey(edgeId)) {
                interAreaBuilder.put(edgeId, new ConcurrentLinkedQueue<>());
              }
              if (!_type1IncomingRoutes.containsKey(edgeId)) {
                type1Builder.put(edgeId, new ConcurrentLinkedQueue<>());
              }
              if (!_type2IncomingRoutes.containsKey(edgeId)) {
                type2Builder.put(edgeId, new ConcurrentLinkedQueue<>());
              }
            });
    _intraAreaIncomingRoutes = intraAreaBuilder.build();
    _interAreaIncomingRoutes = interAreaBuilder.build();
    _type1IncomingRoutes = type1Builder.build();
    _type2IncomingRoutes = type2Builder.build();

    // Edges should always be consistent across all types of queues
    assert _intraAreaIncomingRoutes.keySet().equals(_interAreaIncomingRoutes.keySet());
    assert _intraAreaIncomingRoutes.keySet().equals(_type1IncomingRoutes.keySet());
    assert _intraAreaIncomingRoutes.keySet().equals(_type2IncomingRoutes.keySet());
  }

  @Override
  public void updateTopology(OspfTopology topology) {
    _topology = topology;
    updateQueues(topology);
    /*
    TODO:
      1. Send existing routes to new neighbors
      2. Remove routes received from edges that are now down
    */

  }

  /**
   * Applies distribute list on a {@link AbstractRouteBuilder} based on the configuration and
   * interface on which the route arrives. If the distribute list denies the route, it is declared
   * as non-routing
   *
   * @param c {@link Configuration} on which the route arrives
   * @param vrfName name of the {@link org.batfish.datamodel.Vrf} on which the route arrives
   * @param ifaceName name of the {@link Interface} on which the route arrives
   * @param routeBuilder {@link AbstractRouteBuilder} representing the route
   */
  @VisibleForTesting
  static void applyDistributeList(
      Configuration c, String vrfName, String ifaceName, AbstractRouteBuilder<?, ?> routeBuilder) {
    Interface iface = c.getAllInterfaces().get(ifaceName);
    assert iface != null;
    if (iface.getOspfInboundDistributeListPolicy() == null) {
      return;
    }
    RoutingPolicy routingPolicy =
        c.getRoutingPolicies().get(iface.getOspfInboundDistributeListPolicy());
    assert routingPolicy != null;
    // if routingPolicy denies the input route, set the route as non-routing to prevent it from
    // going in the main RIB
    routeBuilder.setNonRouting(
        !routingPolicy.process(routeBuilder.build(), routeBuilder, Direction.IN));
  }

  @Nonnull
  @Override
  public RibDelta<OspfRoute> getUpdatesForMainRib() {
    RibDelta<OspfRoute> result = _changeset.build();
    // Clear state
    _changeset = RibDelta.builder();
    return result;
  }

  @Override
  public void redistribute(RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta) {
    _queuedForRedistribution = computeRedistributionDelta(mainRibDelta);
    _activatedGeneratedRoutes = activateGeneratedRoutes(mainRibDelta);
  }

  @Override
  public boolean isDirty() {
    return !_changeset.isEmpty()
        || !_queuedForRedistribution.isEmpty()
        || !_activatedGeneratedRoutes.isEmpty()
        || !_interAreaIncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        || !_intraAreaIncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        || !_type1IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        || !_type2IncomingRoutes.values().stream().allMatch(Queue::isEmpty);
  }

  /** Initialize intra-area routes based on available interfaces. */
  private void initializeIntraAreaRoutes() {
    RibDelta.Builder<OspfIntraAreaRoute> intraAreaBuilder = RibDelta.builder();
    _process
        .getAreas()
        .values()
        .forEach(area -> intraAreaBuilder.from(initializeRoutesByArea(area)));
    RibDelta<OspfIntraAreaRoute> intraAreaDelta = intraAreaBuilder.build();
    RibDelta<OspfInterAreaRoute> interAreaDelta = initializeInterAreaRoutes(intraAreaDelta);
    _initializationDelta = new InternalDelta(intraAreaDelta, interAreaDelta, RibDelta.empty());
    _changeset.from(RibDelta.importRibDelta(_ospfRib, intraAreaDelta));
  }

  /** Initialize inter-area routes based the initial intra-area routes. */
  @VisibleForTesting
  @Nonnull
  RibDelta<OspfInterAreaRoute> initializeInterAreaRoutes(
      RibDelta<OspfIntraAreaRoute> intraAreaDelta) {
    RibDelta.Builder<OspfInterAreaRoute> interAreaBuilder = RibDelta.builder();
    if (isABR()) {
      // If we are an ABR, also do conversion to IA routes here.
      intraAreaDelta
          .getRoutesStream()
          .filter(
              intraAreaRoute -> {
                // If this area has summaries, make sure we only convert the non-summarized-routes
                // to inter-area routes.
                RouteFilterList areaFilter = _areaFilters.get(intraAreaRoute.getArea());
                if (areaFilter == null) {
                  return true;
                }
                return areaFilter.permits(intraAreaRoute.getNetwork());
              })
          .forEach(
              r -> interAreaBuilder.add(OspfInterAreaRoute.builder(r).setNonRouting(true).build()));
    }
    return interAreaBuilder.build();
  }

  /**
   * Initialize intra-area routes based on available interfaces in each OSPF area.
   *
   * @param area {@link OspfArea area configuration}
   */
  @Nonnull
  @VisibleForTesting
  RibDelta<OspfIntraAreaRoute> initializeRoutesByArea(OspfArea area) {
    RibDelta.Builder<OspfIntraAreaRoute> deltaBuilder = RibDelta.builder();

    for (String ifaceName : area.getInterfaces()) {
      Interface iface = _c.getAllInterfaces().get(ifaceName);
      if (iface == null || !iface.getActive() || !iface.getOspfEnabled()) {
        /*
         * Skip non-existent interfaces, interfaces that are down,
         * and interfaces that have OSPF disabled
         */
        continue;
      }

      // Create a route for each interface address
      // Only create a /32 host address for this interface if 1. it is a loopback and the network
      // type is not P2P or 2. its type is p2mp (See RFC 2328 Section 2.1.1)
      Set<OspfIntraAreaRoute> allRoutes =
          getIfaceAddressesForIntraAreaRoutes(iface)
              .map(
                  ifaceAddr ->
                      computeIntraAreaRouteFromInterface(area.getAreaNumber(), iface, ifaceAddr))
              .collect(ImmutableSet.toImmutableSet());

      allRoutes.forEach(r -> deltaBuilder.from(_intraAreaRib.mergeRouteGetDelta(r)));
    }
    return deltaBuilder.build();
  }

  @VisibleForTesting
  static Stream<ConcreteInterfaceAddress> getIfaceAddressesForIntraAreaRoutes(Interface iface) {
    return (isLoopbackNotInP2PModeOrP2MP(iface)
        ? iface.getAllConcreteAddresses().stream()
            .map(
                ifaceAddr ->
                    ConcreteInterfaceAddress.create(ifaceAddr.getIp(), Prefix.MAX_PREFIX_LENGTH))
        : iface.getAllConcreteAddresses().stream());
  }

  private static boolean isLoopbackNotInP2PModeOrP2MP(Interface iface) {
    return isLoopbackNotInP2PMode(iface)
        || iface.getOspfNetworkType() == OspfNetworkType.POINT_TO_MULTIPOINT;
  }

  /**
   * Utility for checking if interface is a loopback and is not setup in OSPF point to point mode
   */
  private static boolean isLoopbackNotInP2PMode(Interface iface) {
    return iface.isLoopback()
        && iface.getConcreteAddress() != null
        && iface.getOspfSettings() != null
        && iface.getOspfSettings().getNetworkType() != OspfNetworkType.POINT_TO_POINT;
  }

  /**
   * Make an intraAreaRoute given the interface and one of its prefixes.
   *
   * @param areaNum the OSPF area the interface (and thus the route) belongs to
   * @param iface the {@link Interface} from which the route will be constructed
   * @param ifaceAddress the {@link ConcreteInterfaceAddress} for which the route is being
   *     constructed. Will inform the route's network and next hop IP.
   */
  @Nonnull
  private OspfIntraAreaRoute computeIntraAreaRouteFromInterface(
      Long areaNum, Interface iface, ConcreteInterfaceAddress ifaceAddress) {

    return OspfIntraAreaRoute.builder()
        .setNetwork(ifaceAddress.getPrefix())
        .setNextHopIp(ifaceAddress.getIp())
        .setAdmin(_process.getAdminCosts().get(RoutingProtocol.OSPF))
        .setMetric(getIncrementalCost(iface, true))
        .setArea(areaNum)
        .build();
  }

  /** Check if this is an area border router */
  private boolean isABR() {
    return _process.isAreaBorderRouter();
  }

  /**
   * Extract the {@link OspfRoutingProcess} belonging to the given {@link OspfNeighborConfigId
   * OspfNeighborId}
   */
  @Nullable
  private static OspfRoutingProcess getNeighborProcess(
      OspfNeighborConfigId ospfNeighborId, Map<String, Node> allNodes) {
    return allNodes
        .get(ospfNeighborId.getHostname())
        .getVirtualRouterOrThrow(ospfNeighborId.getVrfName())
        .getOspfProcesses()
        .get(ospfNeighborId.getProcName());
  }

  /** Return the stream of incoming edges, for all neighbors that belong to this process. */
  @Nonnull
  private Stream<EdgeId> getIncomingEdgeStream(OspfTopology topology) {
    return _process.getOspfNeighborConfigs().keySet().stream()
        .flatMap(ospfNeighborConfigId -> topology.incomingEdges(ospfNeighborConfigId).stream());
  }

  /**
   * Given a route advertisement, add or remove the route from the RIB. Any (import) transformations
   * must already have been applied to the route.
   */
  @Nonnull
  private static <R extends AbstractRoute, T extends R> RibDelta<R> processRouteAdvertisement(
      RouteAdvertisement<T> ra, AbstractRib<R> rib) {
    if (ra.isWithdrawn()) {
      return rib.removeRouteGetDelta(ra.getRoute(), ra.getReason());
    } else {
      return rib.mergeRouteGetDelta(ra.getRoute());
    }
  }

  /** Process all OSPF internal messages from all the message queues */
  @Nonnull
  private InternalDelta processInternalRoutes() {
    InternalDelta intraProcessingDelta = processIntraAreaRoutes();
    RibDelta.Builder<OspfInterAreaRoute> interAreaDelta = processInterAreaRoutes();
    // non-forwarding, for re-advertisement only
    RibDelta<OspfInterAreaRoute> deltaOfInterAreaSummaries = computeInterAreaSummaries();
    // discard summary routes for local use only
    RibDelta<OspfInternalSummaryRoute> deltaOfInternalSummaries = computeInternalSummaries();
    // Merge inter-area deltas
    return new InternalDelta(
        intraProcessingDelta._intraArea,
        interAreaDelta
            .from(intraProcessingDelta._interArea)
            .from(deltaOfInterAreaSummaries)
            .build(),
        deltaOfInternalSummaries);
  }

  /**
   * Return the interface cost for a given interface.
   *
   * <p>For details see {@link #getIncrementalCost(Interface, boolean)}
   *
   * @param interfaceName name of the interface
   */
  @VisibleForTesting
  long getIncrementalCost(String interfaceName, boolean considerP2PasStub) {
    Interface iface = _c.getAllInterfaces().get(interfaceName);
    assert iface != null;
    return getIncrementalCost(iface, considerP2PasStub);
  }

  /**
   * Return the interface cost for a given interface (used as increment to route metric).
   *
   * @param iface the {@link Interface} for which to compute the incremental cost
   * @param considerP2PasStub whether or not to consider point-to-point links as stub networks.
   *     <strong>Do not confuse this with stub areas</strong>. Stub here means a non-transit link.
   *     If point-to-point links needs to be considered as stub links (which can happen during
   *     initialization), this can affect cost computation if {@link
   *     OspfProcess#getMaxMetricStubNetworks()} is set.
   */
  private long getIncrementalCost(Interface iface, boolean considerP2PasStub) {
    long cost = iface.getOspfCost();
    if (iface.getOspfPassive()
        || (considerP2PasStub && (iface.getOspfNetworkType() == OspfNetworkType.POINT_TO_POINT))) {
      return firstNonNull(_process.getMaxMetricStubNetworks(), cost);
    } else {
      return firstNonNull(_process.getMaxMetricTransitLinks(), cost);
    }
  }

  /**
   * Examine and merge all incoming inter-area route advertisements
   *
   * @return the resulting inter-area RIB delta builder.
   */
  @Nonnull
  private RibDelta.Builder<OspfInterAreaRoute> processInterAreaRoutes() {
    RibDelta.Builder<OspfInterAreaRoute> interAreaDelta = RibDelta.builder();
    _interAreaIncomingRoutes.forEach(
        (edgeId, queue) -> {
          String ifaceName = edgeId.getHead().getInterfaceName();
          long incrementalCost = getIncrementalCost(ifaceName, false);
          while (!queue.isEmpty()) {
            RouteAdvertisement<OspfInterAreaRoute> routeAdvertisement = queue.remove();

            transformInterAreaRouteOnImport(routeAdvertisement.getRoute(), incrementalCost)
                .ifPresent(
                    routeBuilder -> {
                      applyDistributeList(_c, _vrfName, ifaceName, routeBuilder);
                      interAreaDelta.from(
                          processRouteAdvertisement(
                              routeAdvertisement.toBuilder().setRoute(routeBuilder.build()).build(),
                              _interAreaRib));
                    });
          }
        });
    return interAreaDelta;
  }

  /**
   * Filter and transform inter-area route on import.
   *
   * @param route the {@link OspfInterAreaRoute} to transform
   * @param incrementalCost incremental cost of the OSPF link to be added to the route.
   * @return {@link Optional#empty()} if the route should be filtered out, otherwise a {@link
   *     RouteAdvertisement} containing the transformed route.
   */
  @Nonnull
  @VisibleForTesting
  Optional<OspfInterAreaRoute.Builder> transformInterAreaRouteOnImport(
      OspfInterAreaRoute route, long incrementalCost) {
    if (isABR() && route.getNetwork().equals(Prefix.ZERO)) {
      // ABR should not accept default inter-area routes, as it is the one that originates them
      return Optional.empty();
    }
    // Transform the route
    return Optional.of(
        route.toBuilder()
            .setMetric(route.getMetric() + incrementalCost)
            .setAdmin(_process.getAdminCosts().get(route.getProtocol()))
            // Clear any non-routing or non-forwarding bit
            .setNonRouting(false)
            .setNonForwarding(false));
  }

  /**
   * Examine and merge all incoming intra-area route advertisements
   *
   * @return the resulting intra-area RIB delta builder.
   */
  @Nonnull
  private InternalDelta processIntraAreaRoutes() {
    RibDelta.Builder<OspfIntraAreaRoute> intraAreaDelta = RibDelta.builder();
    RibDelta.Builder<OspfInterAreaRoute> interAreaDelta = RibDelta.builder();
    _intraAreaIncomingRoutes.forEach(
        (edgeId, queue) -> {
          String ifaceName = edgeId.getHead().getInterfaceName();
          long incrementalCost = getIncrementalCost(ifaceName, false);
          while (!queue.isEmpty()) {
            RouteAdvertisement<OspfIntraAreaRoute> routeAdvertisement = queue.remove();
            processIntraAreaAdvertisement(
                intraAreaDelta, interAreaDelta, ifaceName, incrementalCost, routeAdvertisement);
          }
        });
    return new InternalDelta(intraAreaDelta.build(), interAreaDelta.build(), RibDelta.empty());
  }

  /**
   * Process a intraArea advertisement from a neighbor. This will transform advertisement on import,
   * and place it in the appropriate RIB(s).
   *
   * @param intraAreaDelta builder for the intraArea RIB delta
   * @param interAreaDelta builder for the interArea RIB delta
   * @param interAreaDelta builder for the interArea RIB delta
   */
  @VisibleForTesting
  void processIntraAreaAdvertisement(
      RibDelta.Builder<OspfIntraAreaRoute> intraAreaDelta,
      RibDelta.Builder<OspfInterAreaRoute> interAreaDelta,
      String ifaceName,
      long incrementalCost,
      RouteAdvertisement<OspfIntraAreaRoute> routeAdvertisement) {
    OspfIntraAreaRoute.Builder ospfRouteBuilder =
        transformIntraAreaRouteOnImport(routeAdvertisement.getRoute(), incrementalCost);

    applyDistributeList(_c, _vrfName, ifaceName, ospfRouteBuilder);

    OspfIntraAreaRoute intraAreaRoute = ospfRouteBuilder.build();
    intraAreaDelta.from(
        processRouteAdvertisement(
            routeAdvertisement.toBuilder().setRoute(intraAreaRoute).build(), _intraAreaRib));

    /*
    If we are an ABR, convert intra-area routes to inter-area routes (i.e., Type 1 -> Type 3) and put them
    into our inter-area RIB. Note these non-routing because intra-area specific routes should always be preferred.
    */
    if (isABR()) {
      // If this area has summaries, make sure we only convert the non-summarized-routes to
      // inter-area routes.
      RouteFilterList areaFilter = _areaFilters.get(intraAreaRoute.getArea());
      if (areaFilter == null || areaFilter.permits(intraAreaRoute.getNetwork())) {
        OspfInterAreaRoute interAreaRoute =
            OspfInterAreaRoute.builder(intraAreaRoute).setNonRouting(true).build();
        interAreaDelta.from(
            processRouteAdvertisement(
                RouteAdvertisement.<OspfInterAreaRoute>builder()
                    .setReason(routeAdvertisement.getReason())
                    .setRoute(interAreaRoute)
                    .build(),
                _interAreaRib));
      }
    }
  }

  /**
   * Transform intra-area routes on import.
   *
   * @param route the {@link OspfIntraAreaRoute} to transform
   * @param incrementalCost incremental cost of the OSPF link to be added to the route.
   * @return A {@link RouteAdvertisement} containing the transformed route.
   */
  @Nonnull
  @VisibleForTesting
  OspfIntraAreaRoute.Builder transformIntraAreaRouteOnImport(
      OspfIntraAreaRoute route, long incrementalCost) {
    return route.toBuilder()
        .setMetric(route.getMetric() + incrementalCost)
        .setAdmin(_process.getAdminCosts().get(route.getProtocol()))
        // Clear any non-routing or non-forwarding bit
        .setNonRouting(false)
        .setNonForwarding(false);
  }

  /**
   * Compute inter-area summaries. Only applies to an ABR. Will return a {@link RibDelta} for all
   * new inter-area summaries to advertise, or empty delta if not an ABR.
   */
  @Nonnull
  private RibDelta<OspfInterAreaRoute> computeInterAreaSummaries() {
    if (!isABR()) {
      return RibDelta.empty();
    }
    RibDelta.Builder<OspfInterAreaRoute> deltaBuilder = RibDelta.builder();
    _process.getAreas().values().stream()
        .map(this::computeInterAreaSummariesForArea)
        .forEach(deltaBuilder::from);
    return deltaBuilder.build();
  }

  /**
   * Compute internal summaries. Only applies to an ABR. Will return a {@link RibDelta} for all new
   * internal summary discard routes to install, or empty delta if not an ABR.
   */
  @Nonnull
  private RibDelta<OspfInternalSummaryRoute> computeInternalSummaries() {
    if (!isABR()) {
      return RibDelta.empty();
    }
    RibDelta.Builder<OspfInternalSummaryRoute> deltaBuilder = RibDelta.builder();
    _process.getAreas().values().stream()
        .map(this::computeInternalSummariesForArea)
        .forEach(deltaBuilder::from);
    return deltaBuilder.build();
  }

  /** Compute inter-area summaries for a single area. */
  @Nonnull
  private RibDelta<OspfInterAreaRoute> computeInterAreaSummariesForArea(OspfArea area) {
    RibDelta.Builder<OspfInterAreaRoute> deltaBuilder = RibDelta.builder();
    area.getSummaries()
        .forEach(
            (prefix, summary) ->
                computeInterAreaSummaryRoute(prefix, summary, area.getAreaNumber())
                    .ifPresent(r -> deltaBuilder.from(_interAreaRib.mergeRouteGetDelta(r))));
    return deltaBuilder.build();
  }

  /** Compute internal summaries for a single area. */
  @Nonnull
  private RibDelta<OspfInternalSummaryRoute> computeInternalSummariesForArea(OspfArea area) {
    RibDelta.Builder<OspfInternalSummaryRoute> deltaBuilder = RibDelta.builder();
    area.getSummaries()
        .forEach(
            (prefix, summary) -> {
              if (summary.getAdvertised()) {
                computeInternalSummaryRoute(prefix, area.getAreaNumber())
                    // TODO: support withdrawals
                    .ifPresent(r -> deltaBuilder.from(_internalSummaryRib.mergeRouteGetDelta(r)));
              }
            });
    return deltaBuilder.build();
  }

  /**
   * Compute an inter-area summary route for a given area and prefix.
   *
   * <p><emph>Note:</emph> The resulting route will be marked non-routing
   *
   * @param prefix The prefix for which to create a summary route
   * @param summary The {@link OspfAreaSummary summary configuration} (e.g., whether or not to
   *     advertise)
   * @param areaNumber area number for which to generate the route
   * @return {@link Optional#empty()} if the summary is not supposed to be advertised or there are
   *     no contributing routes, otherwise an optional containing a new inter-area route
   */
  @Nonnull
  private Optional<OspfInterAreaRoute> computeInterAreaSummaryRoute(
      Prefix prefix, OspfAreaSummary summary, long areaNumber) {
    if (!summary.getAdvertised()) {
      return Optional.empty();
    }
    /*
    TODO:
      1. make summary computation delta-driven (No need to scan over all RIB routes)
      2. add dependency tracking so a withdrawn route may trigger summary withdrawal
         (if remaining number of contributing routes is 0).

     Both are only applicable to fully incremental computation
     */
    Stream<Long> contributingMetrics =
        _intraAreaRib.getTypedRoutes().stream()
            .filter(
                /*
                 * Only routes in the same area and within the summary prefix can
                 * contribute to creation of summary route
                 */
                candidateContributor ->
                    candidateContributor.getArea() == areaNumber
                        && prefix.containsPrefix(candidateContributor.getNetwork()))
            .map(OspfRoute::getMetric);

    // Use the metric as a proxy value to determine if there are any contributing routes (i.e.,
    // whether the stream is empty or not)
    Long computedMetric =
        _useMinMetricForSummaries
            ? contributingMetrics.min(Comparator.naturalOrder()).orElse(null)
            : contributingMetrics.max(Comparator.naturalOrder()).orElse(null);
    if (computedMetric == null) {
      return Optional.empty();
    }

    OspfInterAreaRoute summaryRoute =
        OspfInterAreaRoute.builder()
            .setNetwork(prefix)
            .setNextHop(NextHopDiscard.instance())
            .setAdmin(_process.getSummaryAdminCost())
            .setMetric(firstNonNull(summary.getMetric(), computedMetric))
            .setArea(areaNumber)
            // Note the non-routing bit: must not go into our own main RIB
            .setNonRouting(true)
            .build();
    return Optional.of(summaryRoute);
  }

  /**
   * Compute an internal summary discard route for a given area and prefix.
   *
   * <p><emph>Note:</emph> The resulting route will be used as a local discard route only. The
   * summary that gets advertised is an {@link OspfInterAreaRoute}, computed separately.
   *
   * @param prefix The prefix for which to create a summary route
   * @param areaNumber area number for which to generate the route
   * @return {@link Optional#empty()} if there are no contributing routes, otherwise an optional
   *     containing a new internal summary route
   */
  @Nonnull
  private Optional<OspfInternalSummaryRoute> computeInternalSummaryRoute(
      Prefix prefix, long areaNumber) {
    /*
    TODO:
      1. make summary computation delta-driven (No need to scan over all RIB routes)
      2. add dependency tracking so a withdrawn route may trigger summary withdrawal
         (if remaining number of contributing routes is 0).

     Both are only applicable to fully incremental computation
     */
    if (!_intraAreaRib.getTypedRoutes().stream()
        .anyMatch(
            /*
             * Only routes in the same area and within the summary prefix can
             * contribute to creation of summary route
             */
            candidateContributor ->
                candidateContributor.getArea() == areaNumber
                    && prefix.containsPrefix(candidateContributor.getNetwork()))) {
      return Optional.empty();
    }

    OspfInternalSummaryRoute summaryRoute =
        OspfInternalSummaryRoute.builder()
            .setNetwork(prefix)
            .setAdmin(_process.getAdminCosts().get(RoutingProtocol.OSPF_IS))
            .setMetric(_process.getSummaryDiscardMetric())
            .setArea(areaNumber)
            .build();
    return Optional.of(summaryRoute);
  }

  /** Send out updates of intra- and inter-area routes to all neighbors */
  private void sendOutInternalRoutes(
      InternalDelta delta, Map<String, Node> allNodes, OspfTopology topology) {
    _intraAreaIncomingRoutes
        .keySet()
        .forEach(edgeId -> sendOutInternalRoutesPerEdge(delta, allNodes, topology, edgeId));
  }

  /**
   * Send out updates of intra- and inter-area routes to a single neighbor
   *
   * @param delta the {@link InternalDelta} captures the updates
   * @param allNodes all nodes participating in DP computation
   * @param topology the current OSPF topology
   * @param edgeId the edge identifying neighbor relationship
   */
  private void sendOutInternalRoutesPerEdge(
      InternalDelta delta, Map<String, Node> allNodes, OspfTopology topology, EdgeId edgeId) {
    OspfRoutingProcess remoteProcess = getNeighborProcess(edgeId.getTail(), allNodes);
    assert remoteProcess != null; // Otherwise the edge should not have been established

    Optional<OspfSessionProperties> session = topology.getSession(edgeId);
    assert session.isPresent(); // Otherwise the edge should not have been established

    OspfArea areaConfig = _process.getAreas().get(session.get().getArea());
    sendOutIntraAreaRoutesPerEdge(
        delta._intraArea, edgeId, remoteProcess, areaConfig, session.get());
    if (isABR()) {
      sendOutInterAreaRoutesPerEdgeABR(
          delta._interArea, edgeId, remoteProcess, areaConfig, session.get());
    } else {
      sendOutInterAreaRoutesPerEdgeNonABR(
          delta._interArea, edgeId, remoteProcess, areaConfig, session.get());
    }
  }

  /** Send out intra-area routes to a given neighbor */
  private void sendOutIntraAreaRoutesPerEdge(
      RibDelta<OspfIntraAreaRoute> delta,
      EdgeId edgeId,
      OspfRoutingProcess remoteProcess,
      OspfArea areaConfig,
      OspfSessionProperties session) {
    remoteProcess.enqueueMessagesIntra(
        edgeId.reverse(),
        transformIntraAreaRoutesOnExport(delta, areaConfig, session.getIpLink().getIp2()));
  }

  /**
   * Filter intra-area routes that should be sent to a given area and transform them (i.e., update
   * attributes) in preparation for sending out to a neighbor
   *
   * @param delta RIB delta containing candidates for re-advertisement to neighbors
   * @param areaConfig {@link OspfArea} to which the routes will be sent
   * @param nextHopIp next hop IP to use for the transformed route
   */
  @VisibleForTesting
  static Stream<RouteAdvertisement<OspfIntraAreaRoute>> transformIntraAreaRoutesOnExport(
      RibDelta<OspfIntraAreaRoute> delta, OspfArea areaConfig, Ip nextHopIp) {
    return delta
        .getActions()
        /*
         * For intra-area routes, send the route to all neighbors in the same area as
         * the route's area
         */
        .filter(r -> r.getRoute().getArea() == areaConfig.getAreaNumber())
        .map(
            r ->
                r.toBuilder()
                    .setRoute(r.getRoute().toBuilder().setNextHopIp(nextHopIp).build())
                    .build())
        .distinct();
  }

  /** Send out inter-area routes from a regular (non-ABR) router to a neighbor */
  private void sendOutInterAreaRoutesPerEdgeNonABR(
      RibDelta<OspfInterAreaRoute> delta,
      EdgeId edgeId,
      OspfRoutingProcess remoteProcess,
      OspfArea areaConfig,
      OspfSessionProperties sessionProperties) {
    Ip nextHopIp = sessionProperties.getIpLink().getIp2();
    Collection<RouteAdvertisement<OspfInterAreaRoute>> updatedDelta =
        transformInterAreaRoutesOnExportNonABR(
            delta,
            areaConfig,
            sessionProperties.getIpLink().getIp1(),
            nextHopIp,
            _process.getMaxMetricTransitLinks());
    remoteProcess.enqueueMessagesInter(edgeId.reverse(), updatedDelta);
  }

  /**
   * Filter inter-area routes that should be sent to a given area and transform them (i.e., update
   * attributes) in preparation for sending out to a neighbor
   *
   * @param delta RIB delta containing candidates for re-advertisement to neighbors
   * @param areaConfig {@link OspfArea} to which the routes will be sent
   * @param neighborIp {@link Ip} of the neighbor to which the route is being sent. Used to
   *     implement a split-horizon rule to avoid loops
   * @param nextHopIp next hop IP to use for the transformed route
   * @param customMetric if set (i.e., not {@code null}) will be used to override the route's
   *     original metric
   */
  @VisibleForTesting
  static Collection<RouteAdvertisement<OspfInterAreaRoute>> transformInterAreaRoutesOnExportNonABR(
      RibDelta<OspfInterAreaRoute> delta,
      OspfArea areaConfig,
      Ip neighborIp,
      Ip nextHopIp,
      @Nullable Long customMetric) {
    return delta
        .getActions()
        /*
         * A regular (non-ABR) router can continue re-advertising
         * inter-area routes for area X in area X.
         */
        .filter(r -> r.getRoute().getArea() == areaConfig.getAreaNumber())
        /* Do not send the route to the neighbor that has sent it to us originally. (split horizon) */
        .filter(r -> !r.getRoute().getNextHopIp().equals(neighborIp))
        .map(
            r ->
                r.toBuilder()
                    .setRoute(
                        r.getRoute().toBuilder()
                            .setMetric(firstNonNull(customMetric, r.getRoute().getMetric()))
                            .setNextHopIp(nextHopIp)
                            .build())
                    .build())
        .collect(ImmutableSet.toImmutableSet());
  }

  /** Send out inter-area routes from an ABR to a neighbor */
  private void sendOutInterAreaRoutesPerEdgeABR(
      RibDelta<OspfInterAreaRoute> delta,
      EdgeId edgeId,
      OspfRoutingProcess remoteProcess,
      OspfArea areaConfig,
      OspfSessionProperties sessionProperties) {
    /*
     The area border router (ABR) can:
     - convert intra-area routes from area X into inter-area routes and send them
       to neighbors in different areas (Y, Z, ...)
     - re-advertise inter-area routes of area X into other areas Y, Z, ...
     - Inject default routes into stub areas

     The ABR must NOT:
     - Send inter-area routes to totally stubby areas
     - Send routes that are covered by a summary (this is represented using a route
       filter list in batfish)
    */

    // Note: localIp == ip2
    Ip nextHopIp = sessionProperties.getIpLink().getIp2();

    remoteProcess.enqueueMessagesInter(
        edgeId.reverse(),
        Streams.concat(
                filterInterAreaRoutesToPropagateAtABR(
                    delta,
                    areaConfig,
                    sessionProperties.getIpLink().getIp1(),
                    nextHopIp,
                    _process.getMaxMetricSummaryNetworks()),
                computeDefaultInterAreaRouteToInject(edgeId, areaConfig, nextHopIp))
            .collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Return the routes that can be propagated from an ABR to a different OSPF neighbor, based on the
   * STUB settings for the area. May return an empty stream if no routes need to be sent.
   *
   * @param delta ABR's inter- or intra- RIB delta
   * @param areaConfig area configuration at the ABR for this neighbor adjacency
   * @param neighborIp IP of the neighbor to which we're sending the route
   * @param nextHopIp next hop ip to use when creating the route.
   * @param customMetric if provided (i.e., not {@code null}) it will be used instead of the routes
   */
  @Nonnull
  @VisibleForTesting
  static Stream<RouteAdvertisement<OspfInterAreaRoute>> filterInterAreaRoutesToPropagateAtABR(
      RibDelta<OspfInterAreaRoute> delta,
      OspfArea areaConfig,
      Ip neighborIp,
      Ip nextHopIp,
      @Nullable Long customMetric) {
    if (areaConfig.getStubType() == StubType.STUB && areaConfig.getStub().getSuppressType3()
        || areaConfig.getStubType() == StubType.NSSA && areaConfig.getNssa().getSuppressType3()) {
      // Nothing to do for totally stubby areas, where summaries are suppressed
      return Stream.empty();
    }
    return delta
        .getActions()
        // Only propagate routes from different areas
        .filter(r -> r.getRoute().getArea() != areaConfig.getAreaNumber())
        /* Do not send the route to the neighbor that has sent it to us originally. (split horizon) */
        .filter(r -> !r.getRoute().getNextHopIp().equals(neighborIp))
        // Overwrite area on the route before sending it out
        .map(
            r ->
                r.toBuilder()
                    .setRoute(
                        r.getRoute().toBuilder()
                            .setArea(areaConfig.getAreaNumber())
                            .setMetric(firstNonNull(customMetric, r.getRoute().getMetric()))
                            .setNextHopIp(nextHopIp)
                            .build())
                    .build());
  }

  /**
   * Return a default route if one needs to be injected from an ABR to a neighbor.
   *
   * <p>Keeps track of default routes already injected (per neighbor) to avoid cyclic computation
   *
   * @param edge the edge whose tail node is the neighbor in question
   * @param areaConfig {@link OspfArea} for which the ABR should consider injecting a route into
   * @param nextHopIp the next hop IP to use for the generated route
   * @return an Optional containing the default route. May be empty if no route needs to be injected
   *     into the given area.
   */
  @VisibleForTesting
  Stream<RouteAdvertisement<OspfInterAreaRoute>> computeDefaultInterAreaRouteToInject(
      OspfTopology.EdgeId edge, OspfArea areaConfig, Ip nextHopIp) {
    if (_neighborsWhereDefaultIARouteWasInjected.contains(edge.getTail())) {
      return Stream.empty();
    }
    _neighborsWhereDefaultIARouteWasInjected.add(edge.getTail());
    return computeDefaultInterAreaRouteToInject(areaConfig, nextHopIp)
        .map(Stream::of)
        .orElse(Stream.empty());
  }

  /**
   * Return a default route if one can be injected from an ABR into a given area, based on area
   * settings
   *
   * @param areaConfig {@link OspfArea} for which the ABR should consider injecting a route into
   * @param nextHopIp the next hop IP to use for the generated route
   * @return an Optional containing the default route. May be empty if no route needs to be injected
   *     into the given area.
   */
  @Nonnull
  @VisibleForTesting
  static Optional<RouteAdvertisement<OspfInterAreaRoute>> computeDefaultInterAreaRouteToInject(
      OspfArea areaConfig, Ip nextHopIp) {

    if (areaConfig.getStubType() != StubType.STUB
        && (areaConfig.getStubType() != StubType.NSSA
            || areaConfig.getNssa().getDefaultOriginateType()
                != OspfDefaultOriginateType.INTER_AREA)) {
      return Optional.empty();
    }

    return Optional.of(
        RouteAdvertisement.<OspfInterAreaRoute>builder()
            .setReason(Reason.ADD)
            .setRoute(
                OspfInterAreaRoute.builder()
                    .setNetwork(Prefix.ZERO)
                    .setNextHopIp(nextHopIp)
                    // Intentionally large. Must be correctly set on the receiver's side
                    .setAdmin(Integer.MAX_VALUE)
                    .setMetric(areaConfig.getMetricOfDefaultRoute())
                    .setArea(areaConfig.getAreaNumber())
                    .build())
            .build());
  }

  /**
   * Take routes to be redistributed, make OSPF external routes from them, merge into respective
   * RIBs, and note the deltas.
   */
  private ExternalDelta computeRedistributionDelta(
      RibDelta<? extends AbstractRouteDecorator> mainRibDelta) {
    RibDelta.Builder<OspfExternalType1Route> type1deltaBuilder = RibDelta.builder();
    RibDelta.Builder<OspfExternalType2Route> type2deltaBuilder = RibDelta.builder();
    mainRibDelta
        .getRoutesStream()
        .map(potentialExport -> convertToExternalRoute(potentialExport, _exportPolicy))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(
            route -> {
              if (route.getOspfMetricType() == OspfMetricType.E1) {
                type1deltaBuilder.from(
                    _type1Rib.mergeRouteGetDelta((OspfExternalType1Route) route));
              } else { // assuming here that MetricType exists. Or E2 is the default
                type2deltaBuilder.from(
                    _type2Rib.mergeRouteGetDelta((OspfExternalType2Route) route));
              }
            });
    return new ExternalDelta(type1deltaBuilder.build(), type2deltaBuilder.build());
  }

  /** Process all OSPF external messages from all the message queues */
  @Nonnull
  private ExternalDelta processExternalRoutes() {
    RibDelta<OspfExternalType1Route> type1delta = processType1Routes();
    RibDelta<OspfExternalType2Route> type2delta = processType2Routes();
    return new ExternalDelta(type1delta, type2delta);
  }

  /** Process type 1 routes from all message queues */
  @Nonnull
  private RibDelta<OspfExternalType1Route> processType1Routes() {
    RibDelta.Builder<OspfExternalType1Route> type1deltaBuilder = RibDelta.builder();
    _type1IncomingRoutes.forEach(
        (edgeId, queue) -> {
          String ifaceName = edgeId.getHead().getInterfaceName();
          OspfSessionProperties session = _topology.getSession(edgeId).orElse(null);
          assert session != null; // Invariant of the edge existing
          long incrementalCost = getIncrementalCost(ifaceName, false);
          while (!queue.isEmpty()) {
            RouteAdvertisement<OspfExternalType1Route> routeAdvertisement = queue.remove();
            OspfExternalType1Route.Builder ospfRouteBuilder =
                transformType1RouteOnImport(
                    // Neighbor IP is the IP of tail node which means Ip1
                    routeAdvertisement.getRoute(), session.getIpLink().getIp1(), incrementalCost);
            applyDistributeList(_c, _vrfName, ifaceName, ospfRouteBuilder);
            type1deltaBuilder.from(
                processRouteAdvertisement(
                    routeAdvertisement.toBuilder()
                        .setRoute((OspfExternalType1Route) ospfRouteBuilder.build())
                        .build(),
                    _type1Rib));
          }
        });
    return type1deltaBuilder.build();
  }

  /** Transform type1 routes on import */
  @Nonnull
  @VisibleForTesting
  OspfExternalType1Route.Builder transformType1RouteOnImport(
      OspfExternalType1Route route, Ip nextHopIp, long incrementalCost) {
    return transformType1and2CommonOnImport(route, nextHopIp)
        // For type 1 routes both cost to advertiser and metric get incremented
        .setCostToAdvertiser(route.getCostToAdvertiser() + incrementalCost)
        .setMetric(route.getMetric() + incrementalCost);
  }

  /** Process type 2 routes from all message queues */
  @Nonnull
  private RibDelta<OspfExternalType2Route> processType2Routes() {
    RibDelta.Builder<OspfExternalType2Route> type2deltaBuilder = RibDelta.builder();
    _type2IncomingRoutes.forEach(
        (edgeId, queue) -> {
          String headIfaceName = edgeId.getHead().getInterfaceName();
          OspfSessionProperties session = _topology.getSession(edgeId).orElse(null);
          assert session != null; // Invariant of the edge existing
          long incrementalCost = getIncrementalCost(edgeId.getHead().getInterfaceName(), false);
          while (!queue.isEmpty()) {
            RouteAdvertisement<OspfExternalType2Route> routeAdvertisement = queue.remove();
            OspfExternalType2Route.Builder ospfRouteBuilder =
                transformType2RouteOnImport(
                    // Neighbor IP is the IP of tail node which means Ip1
                    routeAdvertisement.getRoute(), session.getIpLink().getIp1(), incrementalCost);
            applyDistributeList(_c, _vrfName, headIfaceName, ospfRouteBuilder);
            type2deltaBuilder.from(
                processRouteAdvertisement(
                    routeAdvertisement.toBuilder()
                        .setRoute((OspfExternalType2Route) ospfRouteBuilder.build())
                        .build(),
                    _type2Rib));
          }
        });
    return type2deltaBuilder.build();
  }

  /** Transform type2 routes on import */
  @Nonnull
  @VisibleForTesting
  OspfExternalType2Route.Builder transformType2RouteOnImport(
      OspfExternalType2Route route, Ip nextHopIp, long incrementalCost) {
    return transformType1and2CommonOnImport(route, nextHopIp)
        /*
         * For type 2 routes the metric remains constant, but we must keep track of
         * cost to advertiser as a tie-breaker.
         */
        .setCostToAdvertiser(route.getCostToAdvertiser() + incrementalCost);
  }

  /** Common transformations for type 1 and type 2 routes on import (e.g., nextHopIp, admin cost) */
  private OspfExternalRoute.Builder transformType1and2CommonOnImport(
      OspfExternalRoute r, Ip nextHopIp) {
    assert r.getArea() != OspfRoute.NO_AREA; // Area must be set during export
    return r.toBuilder()
        .setNextHopIp(nextHopIp)
        .setAdmin(_process.getAdminCosts().get(r.getOspfMetricType().toRoutingProtocol()))
        // Clear non-routing bit
        .setNonRouting(false);
  }

  /** Send out all external route updates to all neighbors */
  private void sendOutExternalRoutes(
      ExternalDelta delta, Map<String, Node> allNodes, OspfTopology topology) {
    sendOutType1Routes(delta._type1, allNodes, topology);
    sendOutType2Routes(delta._type2, allNodes, topology);
  }

  /** Send out type 1 external route updates to all neighbors */
  private void sendOutType1Routes(
      RibDelta<OspfExternalType1Route> type1, Map<String, Node> allNodes, OspfTopology topology) {
    _type1IncomingRoutes
        .keySet()
        .forEach(
            edge -> {
              OspfSessionProperties session = topology.getSession(edge).orElse(null);
              assert session != null; // Invariant of the edge being in the topology
              OspfArea areaConfig = _process.getAreas().get(session.getArea());
              OspfRoutingProcess neighborProcess = getNeighborProcess(edge.getTail(), allNodes);
              assert neighborProcess != null;
              neighborProcess.enqueueMessagesType1(
                  edge.reverse(),
                  transformType1RoutesOnExport(
                      filterExternalRoutesOnExport(type1, areaConfig), areaConfig));
            });
  }

  /** Send out type 2 external route updates to all neighbors */
  private void sendOutType2Routes(
      RibDelta<OspfExternalType2Route> type2, Map<String, Node> allNodes, OspfTopology topology) {
    _type2IncomingRoutes
        .keySet()
        .forEach(
            edge -> {
              OspfSessionProperties session = topology.getSession(edge).orElse(null);
              assert session != null; // Invariant of the edge being in the topology
              OspfArea areaConfig = _process.getAreas().get(session.getArea());
              OspfRoutingProcess neighborProcess = getNeighborProcess(edge.getTail(), allNodes);
              assert neighborProcess != null;
              neighborProcess.enqueueMessagesType2(
                  edge.reverse(),
                  transformType2RoutesOnExport(
                      filterExternalRoutesOnExport(type2, areaConfig), areaConfig));
            });
  }

  /**
   * Filter external routes on export. In some cases external routes should not propagate to a
   * particular area (e.g., into a stub area).
   *
   * @param delta delta that captures which routes we are about to advertise
   * @param areaConfig configuration for the area into which the routes will be advertised
   * @param <T> specific external route type {@link OspfExternalType1Route} or {@link
   *     OspfExternalType2Route}
   * @return collection of routes that should be advertised into a given area
   */
  @Nonnull
  @VisibleForTesting
  <T extends OspfExternalRoute> Stream<RouteAdvertisement<T>> filterExternalRoutesOnExport(
      RibDelta<T> delta, OspfArea areaConfig) {
    // No external routes can propagate into a stub area
    if (areaConfig.getStubType() == StubType.STUB) {
      return Stream.of();
    }

    // If we're an ABR, do not propagate external routes to NSSAs that suppresses type 7 LSAs
    if (isABR()
        && areaConfig.getNssa() != null
        && areaConfig.getNssa().getSuppressType7()
        && areaConfig.getStubType() == StubType.NSSA) {
      return Stream.of();
    }

    return delta.getActions();
  }

  /**
   * Transform type 1 routes on export
   *
   * @param routeAdvertisements routes that are being sent to a given area
   * @param areaConfig area to which we are sending the routes
   */
  @Nonnull
  @VisibleForTesting
  Collection<RouteAdvertisement<OspfExternalType1Route>> transformType1RoutesOnExport(
      Stream<RouteAdvertisement<OspfExternalType1Route>> routeAdvertisements, OspfArea areaConfig) {
    Long metricOverride = _process.getMaxMetricSummaryNetworks();
    return routeAdvertisements
        .filter(
            /*
             * Route can propagate in the same area or, if crossing areas, only to/from area 0
             *
             * area == NO_AREA means we generated this route locally, so do not filter out
             * and override the area below
             */
            r ->
                r.getRoute().getArea() == 0
                    || areaConfig.getAreaNumber() == 0
                    || r.getRoute().getArea() == areaConfig.getAreaNumber()
                    || r.getRoute().getArea() == OspfRoute.NO_AREA)
        .map(
            r -> {
              final OspfExternalType1Route route = r.getRoute();
              return r.toBuilder()
                  .setRoute(
                      (OspfExternalType1Route)
                          route.toBuilder()
                              /*
                              Override the metric but only for "summary" routes that cross an area boundary.
                              area == NO_AREA means we generated this route locally, so it can't be summarized (?)
                               */
                              .setMetric(
                                  metricOverride != null
                                          && route.getArea() != OspfRoute.NO_AREA
                                          && areaConfig.getAreaNumber() != route.getArea()
                                      ? metricOverride + route.getLsaMetric()
                                      : route.getMetric())
                              .setCostToAdvertiser(
                                  firstNonNull(metricOverride, route.getCostToAdvertiser()))
                              // Override area before sending out
                              .setArea(areaConfig.getAreaNumber())
                              .build())
                  .build();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Transform type 2 routes on export
   *
   * @param routeAdvertisements routes that are being sent to a given area
   * @param areaConfig area to which we are sending the routes
   */
  @Nonnull
  @VisibleForTesting
  static Collection<RouteAdvertisement<OspfExternalType2Route>> transformType2RoutesOnExport(
      Stream<RouteAdvertisement<OspfExternalType2Route>> routeAdvertisements, OspfArea areaConfig) {
    return routeAdvertisements
        .map(
            r -> {
              final OspfExternalType2Route route = r.getRoute();
              return r.toBuilder()
                  .setRoute(
                      (OspfExternalType2Route)
                          route.toBuilder()
                              .setArea(
                                  // TODO: verify area setting logic
                                  route.getArea() == OspfRoute.NO_AREA
                                      ? areaConfig.getAreaNumber()
                                      : route.getArea())
                              .build())
                  .build();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Convert a potential route we need to export into an OSPF external route
   *
   * @param potentialExportRoute route from Main RIB we are considering for export
   * @param exportPolicy export policy for the route
   * @return an external route to be advertised or an {@link Optional#empty()} if the route should
   *     not be exported
   */
  @Nonnull
  @VisibleForTesting
  Optional<OspfExternalRoute> convertToExternalRoute(
      AbstractRouteDecorator potentialExportRoute, RoutingPolicy exportPolicy) {
    // Prepare the builder
    OspfExternalRoute.Builder outputRouteBuilder =
        OspfExternalRoute.builder().setNextHop(NextHopDiscard.instance());
    // Export based on the policy result of processing the potentialExportRoute
    boolean accept = exportPolicy.process(potentialExportRoute, outputRouteBuilder, Direction.OUT);
    if (!accept) {
      return Optional.empty();
    }

    // Routing policy must always set OSPF metric type, otherwise we don't know which type of route
    // to build
    assert outputRouteBuilder.getOspfMetricType() != null;

    outputRouteBuilder
        .setAdmin(
            _process
                .getAdminCosts()
                .get(outputRouteBuilder.getOspfMetricType().toRoutingProtocol()))
        .setNetwork(potentialExportRoute.getNetwork());

    // Override cost to advertiser if needed.
    Long maxMetricExternalNetworks = _process.getMaxMetricExternalNetworks();
    outputRouteBuilder.setCostToAdvertiser(firstNonNull(maxMetricExternalNetworks, 0L));
    // Also override metric (if type 1)
    OspfMetricType metricType = outputRouteBuilder.getOspfMetricType();
    if (maxMetricExternalNetworks != null && metricType == OspfMetricType.E1) {
      outputRouteBuilder.setMetric(maxMetricExternalNetworks);
    }

    outputRouteBuilder.setAdvertiser(_c.getHostname());
    outputRouteBuilder.setLsaMetric(outputRouteBuilder.getMetric());
    // Not defined yet, must be set correctly upon sending out a given edge
    outputRouteBuilder.setArea(OspfRoute.NO_AREA);
    // Note the non-routing bit, must not go into the main RIB
    outputRouteBuilder.setNonRouting(true);
    return Optional.of(outputRouteBuilder.build());
  }

  /**
   * Activate all generated routes and merge them into appropriates external route RIBs
   *
   * @param mainRibDelta contributing route candidates from the main RIB
   * @return a RIB delta indicating newly merged external routes
   */
  private RibDelta<OspfExternalRoute> activateGeneratedRoutes(
      RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta) {
    // Run each generated route through its own generation policy if present, then run the resulting
    // activated routes through the standard OSPF export policy.
    RibDelta.Builder<OspfExternalRoute> activated = RibDelta.builder();
    _process.getGeneratedRoutes().stream()
        .map(r -> activateGeneratedRoute(mainRibDelta, r))
        .filter(Objects::nonNull)
        .map(r -> convertToExternalRoute(new AnnotatedRoute<>(r, _vrfName), _exportPolicy))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(
            r -> {
              // Merge into correct RIB
              if (r.getOspfMetricType() == OspfMetricType.E1) {
                activated.from(_type1Rib.mergeRouteGetDelta((OspfExternalType1Route) r));
              } else { // only other option is E2
                activated.from(_type2Rib.mergeRouteGetDelta((OspfExternalType2Route) r));
              }
            });
    return activated.build();
  }

  /**
   * Activate a generated route by invoking its generation policy (if present)
   *
   * @param mainRibDelta contributing route candidates from the main RIB
   * @param r route to activate
   * @return {@link GeneratedRoute} with any updated attributes, or {@code null} if shouldn't be
   *     activated
   */
  @Nullable
  @VisibleForTesting
  GeneratedRoute activateGeneratedRoute(
      RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta, GeneratedRoute r) {
    String generationPolicyName = r.getGenerationPolicy();
    if (generationPolicyName == null) {
      // This route should be generated unconditionally
      return r;
    }
    RoutingPolicy generationPolicy = _c.getRoutingPolicies().get(generationPolicyName);
    if (generationPolicy == null) {
      // Ignore route; its generation is supposed to depend on some undefined policy
      return null;
    }
    GeneratedRoute.Builder activatedRoute =
        GeneratedRouteHelper.activateGeneratedRoute(
            r,
            generationPolicy,
            mainRibDelta.getRoutesStream().collect(ImmutableSet.toImmutableSet()));
    return activatedRoute == null ? null : activatedRoute.build();
  }

  /** Send out active generated routes to all neighbors */
  private void sendOutActiveGeneratedRoutes(Map<String, Node> allNodes, OspfTopology topology) {
    // type1 edges should be equal to type2 edges, doesn't matter which ones we iterate over.
    _type1IncomingRoutes
        .keySet()
        .forEach(
            edge -> {
              OspfSessionProperties session = topology.getSession(edge).orElse(null);
              assert session != null; // Invariant of the edge being in the topology
              OspfArea areaConfig = _process.getAreas().get(session.getArea());
              OspfRoutingProcess neighborProcess = getNeighborProcess(edge.getTail(), allNodes);
              assert neighborProcess != null;

              neighborProcess.enqueueMessagesType1(
                  edge.reverse(),
                  transformType1RoutesOnExport(
                      filterGeneratedRoutesOnExport(
                          _activatedGeneratedRoutes.getRoutesStream(),
                          areaConfig,
                          OspfExternalType1Route.class),
                      areaConfig));
              neighborProcess.enqueueMessagesType2(
                  edge.reverse(),
                  transformType2RoutesOnExport(
                      filterGeneratedRoutesOnExport(
                          _activatedGeneratedRoutes.getRoutesStream(),
                          areaConfig,
                          OspfExternalType2Route.class),
                      areaConfig));
            });
  }

  /**
   * Filter generated external routes on export. (e.g., default routes are not allowed to be
   * advertised into stub areas).
   *
   * @param activeGeneratedRoutes the routes to filter
   * @param areaConfig config for the area to which the routes are being sent to
   * @param clazz the expected type for route {@link OspfExternalType1Route} or {@link
   *     OspfExternalType2Route}
   */
  private static <T extends OspfExternalRoute>
      Stream<RouteAdvertisement<T>> filterGeneratedRoutesOnExport(
          Stream<OspfExternalRoute> activeGeneratedRoutes, OspfArea areaConfig, Class<T> clazz) {
    return activeGeneratedRoutes
        .filter(
            r -> areaConfig.getStubType() == StubType.NONE || !r.getNetwork().equals(Prefix.ZERO))
        .filter(clazz::isInstance)
        .map(r -> new RouteAdvertisement<>(clazz.cast(r)));
  }

  /**
   * Tell this process that a collection of intra-area route advertisements is coming in on a given
   * edge.
   *
   * @param edge {@link EdgeId} as with edge head pointing at {@code this} process
   * @param routes collection of route advertisements
   */
  @VisibleForTesting
  void enqueueMessagesIntra(EdgeId edge, Stream<RouteAdvertisement<OspfIntraAreaRoute>> routes) {
    Queue<RouteAdvertisement<OspfIntraAreaRoute>> queue = _intraAreaIncomingRoutes.get(edge);
    assert queue != null;
    routes.forEach(queue::add);
  }

  /**
   * Tell this process that a collection of inter-area route advertisements is coming in on a given
   * edge
   *
   * @param edge {@link EdgeId} as with edge head pointing at {@code this} process
   * @param routes collection of route advertisements
   */
  @VisibleForTesting
  void enqueueMessagesInter(
      EdgeId edge, Collection<RouteAdvertisement<OspfInterAreaRoute>> routes) {
    assert _interAreaIncomingRoutes.containsKey(edge);
    _interAreaIncomingRoutes.get(edge).addAll(routes);
  }

  /**
   * Tell this process that a collection of type1 route advertisements is coming in on a given edge
   *
   * @param edge {@link EdgeId} as with edge head pointing at {@code this} process
   * @param routes collection of route advertisements
   */
  private void enqueueMessagesType1(
      EdgeId edge, Collection<RouteAdvertisement<OspfExternalType1Route>> routes) {
    assert _type1IncomingRoutes.containsKey(edge);
    _type1IncomingRoutes.get(edge).addAll(routes);
  }

  /**
   * Tell this process that a collection of type2 route advertisements is coming in on a given edge
   *
   * @param edge {@link EdgeId} as with edge head pointing at {@code this} process
   * @param routes collection of route advertisements
   */
  private void enqueueMessagesType2(
      EdgeId edge, Collection<RouteAdvertisement<OspfExternalType2Route>> routes) {
    assert _type2IncomingRoutes.containsKey(edge);
    _type2IncomingRoutes.get(edge).addAll(routes);
  }

  int iterationHashCode() {
    return Streams.concat(
            // Message queues
            Stream.of(
                    _intraAreaIncomingRoutes,
                    _interAreaIncomingRoutes,
                    _type1IncomingRoutes,
                    _type2IncomingRoutes)
                .flatMap(m -> m.values().stream())
                .flatMap(Queue::stream),
            // Deltas
            _activatedGeneratedRoutes.getActions(),
            // RIB state
            Stream.of(_intraAreaRib, _interAreaRib, _internalSummaryRib, _type1Rib, _type2Rib)
                .map(AbstractRib::getTypedRoutes))
        .collect(toOrderedHashCode());
  }

  /** Wrapper around intra- and inter-area RIB deltas */
  private static final class InternalDelta {
    @Nonnull private final RibDelta<OspfIntraAreaRoute> _intraArea;
    @Nonnull private final RibDelta<OspfInterAreaRoute> _interArea;
    @Nonnull private final RibDelta<OspfInternalSummaryRoute> _internalSummary;

    InternalDelta(
        RibDelta<OspfIntraAreaRoute> intraArea,
        RibDelta<OspfInterAreaRoute> interArea,
        RibDelta<OspfInternalSummaryRoute> internalSummary) {
      _intraArea = intraArea;
      _interArea = interArea;
      _internalSummary = internalSummary;
    }

    private boolean isEmpty() {
      return _intraArea.isEmpty() && _interArea.isEmpty() && _internalSummary.isEmpty();
    }
  }

  /** Wrapper around type1 and type2 external RIB deltas */
  private static final class ExternalDelta {
    @Nonnull private final RibDelta<OspfExternalType1Route> _type1;
    @Nonnull private final RibDelta<OspfExternalType2Route> _type2;

    private ExternalDelta() {
      this(RibDelta.empty(), RibDelta.empty());
    }

    private ExternalDelta(
        RibDelta<OspfExternalType1Route> type1, RibDelta<OspfExternalType2Route> type2) {
      _type1 = type1;
      _type2 = type2;
    }

    private boolean isEmpty() {
      return _type1.isEmpty() && _type2.isEmpty();
    }
  }
}
