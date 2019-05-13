package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.common.util.CommonUtil.toImmutableSortedMap;
import static org.batfish.common.util.CommonUtil.toOrderedHashCode;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;

import com.google.common.collect.Streams;
import com.google.common.graph.ValueGraph;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EvpnRoute;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.dataplane.rib.Bgpv4Rib;
import org.batfish.dataplane.rib.EvpnRib;
import org.batfish.dataplane.rib.Rib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RouteAdvertisement;

@ParametersAreNonnullByDefault
final class BgpRoutingProcess implements RoutingProcess<BgpTopology, BgpRoute> {
  /** Configuration for this process */
  @Nonnull private final BgpProcess _process;
  /** Parent node configuration */
  @Nonnull private final Configuration _c;
  /** Name of our VRF */
  @Nonnull private final String _vrfName;
  /** Reference to the parent {@link VirtualRouter} main RIB (read-only). */
  @Nonnull private final Rib _mainRib;

  /** Route dependency tracker for BGP aggregate routes */
  RouteDependencyTracker<Bgpv4Route, AbstractRoute> _bgpAggDeps = new RouteDependencyTracker<>();
  /** Incoming messages into this router from each BGP neighbor */
  SortedMap<EdgeId, Queue<RouteAdvertisement<Bgpv4Route>>> _bgpv4IncomingRoutes;
  /** Combined BGP (both iBGP and eBGP) RIB, for all address families */
  Bgpv4Rib _bgpv4Rib;
  /** Builder for constructing {@link RibDelta} as pertains to the multipath BGP RIB */
  Builder<Bgpv4Route> _bgpv4DeltaBuilder;
  /** Helper RIB containing all paths obtained with external BGP */
  Bgpv4Rib _ebgpv4Rib;
  /**
   * Helper RIB containing paths obtained with external eBGP during current iteration. An Adj-RIB of
   * sorts.
   */
  Bgpv4Rib _ebgpv4StagingRib;
  /** Helper RIB containing paths obtained with iBGP */
  Bgpv4Rib _ibgpv4Rib;
  /**
   * Helper RIB containing paths obtained with iBGP during current iteration. An Adj-RIB of sorts.
   */
  Bgpv4Rib _ibgpv4StagingRib;
  /** Helper RIB containing paths obtained with EVPN over eBGP */
  EvpnRib<EvpnRoute> _ebgpEvpnRib;
  /** Helper RIB containing paths obtained with EVPN over iBGP */
  EvpnRib<EvpnRoute> _ibgpEvpnRib;

  /**
   * @param process the {@link BgpProcess} -- configuration for this routing process
   * @param configuration the parent {@link Configuration}
   * @param vrfName name of the VRF this process is in
   * @param mainRib take in a reference to MainRib for read-only use (e.g., getting IGP cost to
   *     next-hop)
   */
  BgpRoutingProcess(BgpProcess process, Configuration configuration, String vrfName, Rib mainRib) {
    _process = process;
    _c = configuration;
    _vrfName = vrfName;
    // TODO: really need to have a read-only RIB interface for safety
    _mainRib = mainRib;
    initRibs();
  }

  /** Initalize all necessary RIBs */
  private void initRibs() {
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
    // Ribs
    _ebgpEvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
    _ibgpEvpnRib = new EvpnRib<>(_mainRib, bestPathTieBreaker, null, multiPathMatchMode);
  }

  @Override
  public void initialize() {}

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
    initBgpQueues(topology);
  }

  @Override
  public void executeIteration(Map<String, Node> allNodes) {
    // Reinitialize staging RIBs, delta builders
    _bgpv4DeltaBuilder = RibDelta.builder();
    BgpTieBreaker bestPathTieBreaker =
        firstNonNull(_process.getTieBreaker(), BgpTieBreaker.ARRIVAL_ORDER);
    MultipathEquivalentAsPathMatchMode multiPathMatchMode =
        firstNonNull(_process.getMultipathEquivalentAsPathMatchMode(), EXACT_PATH);
    _ebgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);
    _ibgpv4StagingRib = new Bgpv4Rib(_mainRib, bestPathTieBreaker, null, multiPathMatchMode, false);
  }

  @Nonnull
  @Override
  public RibDelta<BgpRoute> getUpdatesForMainRib() {
    return RibDelta.<BgpRoute>builder().from(_bgpv4DeltaBuilder.build()).build();
  }

  @Override
  public void redistribute(RibDelta<? extends AnnotatedRoute<AbstractRoute>> mainRibDelta) {}

  @Override
  public boolean isDirty() {
    return
    // Message queues
    !_bgpv4IncomingRoutes.values().stream().allMatch(Queue::isEmpty)
        // Delta builders
        || !_bgpv4DeltaBuilder.build().isEmpty();
  }

  int iterationHashCode() {
    return Stream.of(
            // RIBs
            _bgpv4Rib.getTypedRoutes(),
            // Message queues
            _bgpv4IncomingRoutes,
            // Delta builders
            _bgpv4DeltaBuilder.build())
        .collect(toOrderedHashCode());
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
}
