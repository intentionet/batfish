package org.batfish.dataplane.ibdp;

import static org.batfish.common.topology.TopologyUtil.computeLayer2Topology;
import static org.batfish.common.topology.TopologyUtil.computeLayer3Topology;
import static org.batfish.common.topology.TopologyUtil.computeRawLayer3Topology;
import static org.batfish.common.topology.TopologyUtil.pruneUnreachableTunnelEdges;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.common.util.IpsecUtil.retainReachableIpsecEdges;
import static org.batfish.common.util.IpsecUtil.toEdgeSet;
import static org.batfish.common.util.StreamUtil.toListInRandomOrder;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.initBgpTopology;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.computeVxlanTopology;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.prunedVxlanTopology;
import static org.batfish.dataplane.rib.AbstractRib.importRib;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.batfish.common.BdpOscillationException;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.common.plugin.TracerouteEngine;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.topology.L3Adjacencies;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.LegacyL3Adjacencies;
import org.batfish.common.topology.TunnelTopology;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.IncrementalBdpAnswerElement;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.eigrp.EigrpTopologyUtils;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule.Schedule;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.version.BatfishVersion;

/** Computes the entire dataplane by executing a fixed-point computation. */
final class IncrementalBdpEngine {

  private static final Logger LOGGER = LogManager.getLogger(IncrementalBdpEngine.class);
  /**
   * Maximum amount of topology iterations to do before deciding that the dataplane computation
   * cannot converge (there is some sort of flap)
   */
  private static final int MAX_TOPOLOGY_ITERATIONS = 10;

  private int _numIterations;
  private final IncrementalDataPlaneSettings _settings;

  IncrementalBdpEngine(IncrementalDataPlaneSettings settings) {
    _settings = settings;
  }

  /**
   * Performs the iterative step in dataplane computations as topology changes.
   *
   * <p>The {@code currentTopologyContext} contains the connectivity learned so far in the network,
   * specifically for things like VXLAN, BGP, and others, and {@code nodes} contains the current
   * routing and forwarding tables.
   *
   * <p>Given these inputs, primarily the current Layer3 topology, the possible edges for each other
   * topology (obtained from {@code initialTopologyContext}) are pruned down based on which sessions
   * can be established given the current L3 topology and dataplane state. The resulting {@code
   * TopologyContext} for the next iteration of dataplane is returned.
   */
  private TopologyContext nextTopologyContext(
      TopologyContext currentTopologyContext,
      SortedMap<String, Node> nodes,
      List<VirtualRouter> vrs,
      TopologyContext initialTopologyContext,
      NetworkConfigurations networkConfigurations,
      Map<Ip, Map<String, Set<String>>> ipVrfOwners) {
    // Force re-init of partial dataplane. Re-inits forwarding analysis, etc.
    computeFibs(vrs);
    PartialDataplane partialDataplane =
        PartialDataplane.builder()
            .setNodes(nodes)
            .setLayer3Topology(currentTopologyContext.getLayer3Topology())
            .build();

    Map<String, Configuration> configurations = networkConfigurations.getMap();
    TracerouteEngine trEngCurrentL3Topology =
        new TracerouteEngineImpl(
            partialDataplane, currentTopologyContext.getLayer3Topology(), configurations);

    // Update topologies
    LOGGER.info("Updating dynamic topologies");

    // IPsec
    LOGGER.info("Updating IPsec topology");
    // Note: this uses the initial context since it is pruning down the potential edges initially
    // established.
    IpsecTopology newIpsecTopology =
        retainReachableIpsecEdges(
            initialTopologyContext.getIpsecTopology(), configurations, trEngCurrentL3Topology);

    // VXLAN
    LOGGER.info("Updating VXLAN topology");
    VxlanTopology newVxlanTopology =
        prunedVxlanTopology(
            computeVxlanTopology(partialDataplane.getLayer2Vnis()),
            configurations,
            trEngCurrentL3Topology);

    // Tunnel topology
    LOGGER.info("Updating Tunnel topology");
    TunnelTopology newTunnelTopology =
        pruneUnreachableTunnelEdges(
            initialTopologyContext.getTunnelTopology(), // like IPsec, pruning initial tunnels
            networkConfigurations,
            trEngCurrentL3Topology);

    // EIGRP topology
    LOGGER.info("Updating EIGRP topology");
    EigrpTopology newEigrpTopology =
        EigrpTopologyUtils.initEigrpTopology(
            configurations, currentTopologyContext.getLayer3Topology());

    // Initialize BGP topology
    LOGGER.info("Updating BGP topology");
    BgpTopology newBgpTopology =
        initBgpTopology(
            configurations,
            ipVrfOwners,
            false,
            true,
            trEngCurrentL3Topology,
            currentTopologyContext.getL3Adjacencies());

    // Update Layer-2 if necessary
    Optional<Layer2Topology> newLayer2Topology;
    if (!currentTopologyContext.getVxlanTopology().equals(newVxlanTopology)) {
      LOGGER.info("Updating Layer 2 topology");
      newLayer2Topology =
          initialTopologyContext // not updated across rounds
              .getLayer1LogicalTopology()
              .map(l1 -> computeLayer2Topology(l1, newVxlanTopology, configurations));
    } else {
      newLayer2Topology = currentTopologyContext.getLayer2Topology();
    }

    // L3 adjacencies
    L3Adjacencies newAdjacencies;
    if (!newLayer2Topology.equals(currentTopologyContext.getLayer2Topology())) {
      newAdjacencies =
          new LegacyL3Adjacencies(
              // neither L1 topology is updated across rounds
              initialTopologyContext.getRawLayer1PhysicalTopology().orElse(Layer1Topology.EMPTY),
              initialTopologyContext.getLayer1LogicalTopology().orElse(Layer1Topology.EMPTY),
              newLayer2Topology.orElse(Layer2Topology.EMPTY),
              configurations);
    } else {
      newAdjacencies = currentTopologyContext.getL3Adjacencies();
    }

    // Layer-3
    Topology newLayer3Topology;
    if (!newIpsecTopology.equals(currentTopologyContext.getIpsecTopology())
        || !newTunnelTopology.equals(currentTopologyContext.getTunnelTopology())
        || !newAdjacencies.equals(currentTopologyContext.getL3Adjacencies())) {
      LOGGER.info("Updating Layer 3 topology");
      newLayer3Topology =
          computeLayer3Topology(
              computeRawLayer3Topology(newAdjacencies, configurations),
              // Overlay edges consist of "plain" tunnels and IPSec tunnels
              Sets.union(
                  toEdgeSet(newIpsecTopology, configurations), newTunnelTopology.asEdgeSet()));
    } else {
      newLayer3Topology = currentTopologyContext.getLayer3Topology();
    }

    return currentTopologyContext.toBuilder()
        .setBgpTopology(newBgpTopology)
        .setLayer2Topology(newLayer2Topology)
        .setLayer3Topology(newLayer3Topology)
        .setL3Adjacencies(newAdjacencies)
        .setVxlanTopology(newVxlanTopology)
        .setIpsecTopology(newIpsecTopology)
        .setTunnelTopology(newTunnelTopology)
        .setEigrpTopology(newEigrpTopology)
        .build();
  }

  ComputeDataPlaneResult computeDataPlane(
      Map<String, Configuration> configurations,
      TopologyContext initialTopologyContext,
      Set<BgpAdvertisement> externalAdverts) {
    Span span = GlobalTracer.get().buildSpan("Compute Data Plane").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning

      LOGGER.info("Computing Data Plane using iBDP");

      // TODO: switch to topologies and owners from TopologyProvider
      Map<Ip, Map<String, Set<String>>> ipVrfOwners = new IpOwners(configurations).getIpVrfOwners();

      // Generate our nodes, keyed by name, sorted for determinism
      SortedMap<String, Node> nodes =
          toImmutableSortedMap(configurations.values(), Configuration::getHostname, Node::new);
      // A collection of all the virtual routers in random order enables parallelization across all
      // VRs, and likely spreads nodes with similar hostnames across different cores. In contrast,
      // nodes.values().parallelStream().flatMap(get vrs stream) is only node-parallel and clusters
      // nodes by hostname. See https://github.com/batfish/batfish/pull/7054 description.
      List<VirtualRouter> vrs =
          toListInRandomOrder(nodes.values().stream().flatMap(n -> n.getVirtualRouters().stream()));
      NetworkConfigurations networkConfigurations = NetworkConfigurations.of(configurations);

      /*
       * Run the data plane computation here:
       * - First, let the IGP routes converge
       * - Second, re-init BGP neighbors with reachability checks
       * - Third, let the EGP routes converge
       * - Finally, compute FIBs, return answer
       */
      IncrementalBdpAnswerElement answerElement = new IncrementalBdpAnswerElement();
      // TODO: eventually, IGP needs to be part of fixed-point below, because tunnels.
      computeIgpDataPlane(nodes, vrs, initialTopologyContext, answerElement);

      LOGGER.info("Initialize virtual routers before topology fixed point");
      Span initializationSpan =
          GlobalTracer.get().buildSpan("Initialize virtual routers for iBDP-external").start();
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(initializationSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream()
            .forEach(
                vr -> vr.initForEgpComputationBeforeTopologyLoop(externalAdverts, ipVrfOwners));
      } finally {
        initializationSpan.finish();
      }

      /*
       * Perform a fixed-point computation, in which every round the topology is updated based
       * on what we have learned in the previous round.
       */
      // Since the topology iterations are incremental, clear fields that are pruned to get the real
      // topology. They are not actually yet included in topologies.
      TopologyContext priorTopologyContext =
          initialTopologyContext.toBuilder()
              .setIpsecTopology(IpsecTopology.EMPTY)
              .setTunnelTopology(TunnelTopology.EMPTY)
              .setVxlanTopology(VxlanTopology.EMPTY)
              .build();
      TopologyContext currentTopologyContext =
          nextTopologyContext(
              priorTopologyContext,
              nodes,
              vrs,
              initialTopologyContext,
              networkConfigurations,
              ipVrfOwners);
      int topologyIterations = 0;
      boolean converged = false;
      while (!converged && topologyIterations++ < MAX_TOPOLOGY_ITERATIONS) {
        Span iterSpan =
            GlobalTracer.get().buildSpan("Topology iteration " + topologyIterations).start();
        LOGGER.info("Starting topology iteration {}", topologyIterations);
        try (Scope iterScope = GlobalTracer.get().scopeManager().activate(iterSpan)) {
          assert iterScope != null; // avoid unused warning

          boolean isOscillating =
              computeNonMonotonicPortionOfDataPlane(
                  nodes, vrs, answerElement, currentTopologyContext, networkConfigurations);
          if (isOscillating) {
            // If we are oscillating here, network has no stable solution.
            LOGGER.error("Network has no stable solution");
            throw new BdpOscillationException("Network has no stable solution");
          }

          TopologyContext nextTopologyContext =
              nextTopologyContext(
                  currentTopologyContext,
                  nodes,
                  vrs,
                  initialTopologyContext,
                  networkConfigurations,
                  ipVrfOwners);
          converged = currentTopologyContext.equals(nextTopologyContext);
          currentTopologyContext = nextTopologyContext;
        } finally {
          iterSpan.finish();
        }
      }

      if (!converged) {
        LOGGER.error(
            "Could not reach a fixed point topology in {} iterations", MAX_TOPOLOGY_ITERATIONS);
        throw new BdpOscillationException(
            String.format(
                "Could not reach a fixed point topology in %d iterations",
                MAX_TOPOLOGY_ITERATIONS));
      }

      // Generate the answers from the computation, compute final FIBs
      // TODO: Properly finalize topologies, IpOwners, etc.
      LOGGER.info("Finalizing dataplane");
      answerElement.setVersion(BatfishVersion.getVersionStatic());
      IncrementalDataPlane finalDataplane =
          IncrementalDataPlane.builder()
              .setNodes(nodes)
              .setLayer3Topology(currentTopologyContext.getLayer3Topology())
              .build();
      return new IbdpResult(answerElement, finalDataplane, currentTopologyContext, nodes);
    } finally {
      span.finish();
    }
  }

  /**
   * Perform one iteration of the "dependent routes" dataplane computation. Dependent routes refers
   * to routes that could change because other routes have changed. For example, this includes:
   *
   * <ul>
   *   <li>static routes with next hop IP
   *   <li>aggregate routes
   *   <li>EGP routes (various protocols)
   * </ul>
   *
   * @param vrs virtual routers that are participating in the computation
   * @param iterationLabel iteration label (for stats tracking)
   * @param allNodes all nodes in the network (for correct neighbor referencing)
   */
  private static void computeDependentRoutesIteration(
      List<VirtualRouter> vrs,
      String iterationLabel,
      Map<String, Node> allNodes,
      NetworkConfigurations networkConfigurations,
      int iteration) {
    Span overallSpan =
        GlobalTracer.get().buildSpan(iterationLabel + ": Compute dependent routes").start();
    LOGGER.info("{}: Compute dependent routes", iterationLabel);
    try (Scope scope = GlobalTracer.get().scopeManager().activate(overallSpan)) {
      assert scope != null; // avoid unused warning

      // Static nextHopIp routes
      Span nhIpSpan =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": Recompute static routes with next-hop IP")
              .start();
      LOGGER.info("{}: Recompute static routes with next-hop IP", iterationLabel);
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(nhIpSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream().forEach(VirtualRouter::activateStaticRoutes);
      } finally {
        nhIpSpan.finish();
      }

      // Generated/aggregate routes
      Span genRoutesSpan =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": Recompute aggregate/generated routes")
              .start();
      LOGGER.info("{}: Recompute aggregate/generated routes", iterationLabel);
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(genRoutesSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream().forEach(VirtualRouter::recomputeGeneratedRoutes);
      } finally {
        genRoutesSpan.finish();
      }

      // EIGRP
      Span eigrpSpan =
          GlobalTracer.get().buildSpan(iterationLabel + ": propagate EIGRP routes").start();
      LOGGER.info("{}: Propagate EIGRP routes", iterationLabel);
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(eigrpSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream().forEach(vr -> vr.eigrpIteration(allNodes));
        vrs.parallelStream().forEach(VirtualRouter::mergeEigrpRoutesToMainRib);
      } finally {
        eigrpSpan.finish();
      }

      // Re-initialize IS-IS exports.
      Span isisSpan =
          GlobalTracer.get().buildSpan(iterationLabel + ": Recompute IS-IS exports").start();
      LOGGER.info("{}: Recompute IS-IS routes", iterationLabel);
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(isisSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream()
            .forEach(vr -> vr.initIsisExports(iteration, allNodes, networkConfigurations));
      } finally {
        isisSpan.finish();
      }

      // IS-IS route propagation
      AtomicBoolean isisChanged = new AtomicBoolean(true);
      int isisSubIterations = 0;
      while (isisChanged.get()) {
        isisSubIterations++;
        Span isisSpanRecompute =
            GlobalTracer.get()
                .buildSpan(
                    iterationLabel + ": Recompute IS-IS routes: subIteration: " + isisSubIterations)
                .start();
        LOGGER.info(
            "{}: Recompute IS-IS routes: subIteration {}", iterationLabel, isisSubIterations);
        try (Scope innerScope = GlobalTracer.get().scopeManager().activate(isisSpanRecompute)) {
          assert innerScope != null; // avoid unused warning
          isisChanged.set(false);
          vrs.parallelStream()
              .forEach(
                  vr -> {
                    Entry<RibDelta<IsisRoute>, RibDelta<IsisRoute>> p =
                        vr.propagateIsisRoutes(networkConfigurations);
                    if (p != null
                        && vr.unstageIsisRoutes(
                            allNodes, networkConfigurations, p.getKey(), p.getValue())) {
                      isisChanged.set(true);
                    }
                  });
        } finally {
          isisSpanRecompute.finish();
        }
      }

      Span span =
          GlobalTracer.get().buildSpan(iterationLabel + ": propagate OSPF external").start();
      LOGGER.info("{}: Propagate OSPF external", iterationLabel);
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(span)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream().forEach(vr -> vr.ospfIteration(allNodes));
        vrs.parallelStream().forEach(VirtualRouter::mergeOspfRoutesToMainRib);
      } finally {
        span.finish();
      }

      computeIterationOfBgpRoutes(iterationLabel, allNodes, vrs);

      leakAcrossVrfs(vrs, iterationLabel);
    } finally {
      overallSpan.finish();
    }
  }

  private static void computeIterationOfBgpRoutes(
      String iterationLabel, Map<String, Node> allNodes, List<VirtualRouter> vrs) {
    Span span =
        GlobalTracer.get().buildSpan(iterationLabel + ": Init for new BGP iteration").start();
    LOGGER.info("{}: Init for new BGP iteration", iterationLabel);
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      vrs.parallelStream().forEach(vr -> vr.bgpIteration(allNodes));
    } finally {
      span.finish();
    }
    Span genSpan =
        GlobalTracer.get()
            .buildSpan(iterationLabel + ": Init BGP generated/aggregate routes")
            .start();
    LOGGER.info("{}: Init BGP generated/aggregate routes", iterationLabel);
    try (Scope innerScope = GlobalTracer.get().scopeManager().activate(genSpan)) {
      assert innerScope != null; // avoid unused warning
      // first let's initialize nodes-level generated/aggregate routes
      vrs.parallelStream().forEach(VirtualRouter::initBgpAggregateRoutes);
    } finally {
      genSpan.finish();
    }

    Span propSpan =
        GlobalTracer.get().buildSpan(iterationLabel + ": Propagate BGP v4 routes").start();
    LOGGER.info("{}: Propagate BGP v4 routes", iterationLabel);

    try (Scope innerScope = GlobalTracer.get().scopeManager().activate(propSpan)) {
      assert innerScope != null; // avoid unused warning

      // Merge BGP routes from BGP process into the main RIB
      vrs.parallelStream().forEach(VirtualRouter::mergeBgpRoutesToMainRib);
    } finally {
      propSpan.finish();
    }
  }

  private static void queueRoutesForCrossVrfLeaking(List<VirtualRouter> vrs) {
    Span span = GlobalTracer.get().buildSpan("Queueing routes to leak across VRFs").start();
    LOGGER.info("Queueing routes to leak across VRFs");
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      vrs.parallelStream().forEach(VirtualRouter::queueCrossVrfImports);
    } finally {
      span.finish();
    }
  }

  private static void leakAcrossVrfs(List<VirtualRouter> vrs, String iterationLabel) {
    Span span =
        GlobalTracer.get().buildSpan(iterationLabel + ": Leaking routes across VRFs").start();
    LOGGER.info("{}: Leaking routes across VRFs", iterationLabel);
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      vrs.parallelStream().forEach(VirtualRouter::processCrossVrfRoutes);
    } finally {
      span.finish();
    }
  }

  /**
   * Run {@link VirtualRouter#computeFib} on all virtual routers
   *
   * @param vrs all virtual routers
   */
  private void computeFibs(List<VirtualRouter> vrs) {
    Span span = GlobalTracer.get().buildSpan("Compute FIBs").start();
    LOGGER.info("Compute FIBs");
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      vrs.parallelStream().forEach(VirtualRouter::computeFib);
    } finally {
      span.finish();
    }
  }

  /**
   * Compute the IGP portion of the dataplane.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param topologyContext The topology context in which various adjacencies are stored
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   */
  private void computeIgpDataPlane(
      SortedMap<String, Node> nodes,
      List<VirtualRouter> vrs,
      TopologyContext topologyContext,
      IncrementalBdpAnswerElement ae) {
    Span span = GlobalTracer.get().buildSpan("Compute IGP").start();
    LOGGER.info("Compute IGP");
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning

      int numOspfInternalIterations;

      /*
       * For each virtual router, setup the initial easy-to-do routes, init protocol-based RIBs,
       * queue outgoing messages to neighbors
       */
      Span initializeSpan = GlobalTracer.get().buildSpan("Initialize for IGP computation").start();
      LOGGER.info("Initialize for IGP computation");
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(initializeSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream().forEach(vr -> vr.initForIgpComputation(topologyContext));
      } finally {
        initializeSpan.finish();
      }

      // OSPF internal routes
      numOspfInternalIterations = initOspfInternalRoutes(nodes, topologyContext.getOspfTopology());

      // RIP internal routes
      initRipInternalRoutes(nodes, vrs, topologyContext.getLayer3Topology());

      // Activate static routes
      Span staticSpan =
          GlobalTracer.get().buildSpan("Compute static routes post IGP convergence").start();
      LOGGER.info("Compute static routes post IGP convergence");
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(staticSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream()
            .forEach(
                vr -> {
                  importRib(vr.getMainRib(), vr._independentRib);
                  vr.activateStaticRoutes();
                });
      } finally {
        staticSpan.finish();
      }

      // Set iteration stats in the answer
      ae.setOspfInternalIterations(numOspfInternalIterations);
    } finally {
      span.finish();
    }
  }

  /**
   * Compute the EGP portion of the route exchange. Must be called after IGP routing has converged.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   * @param topologyContext The various network topologies
   * @return true iff the computation is oscillating
   */
  private boolean computeNonMonotonicPortionOfDataPlane(
      SortedMap<String, Node> nodes,
      List<VirtualRouter> vrs,
      IncrementalBdpAnswerElement ae,
      TopologyContext topologyContext,
      NetworkConfigurations networkConfigurations) {
    LOGGER.info("Compute EGP");
    Span span = GlobalTracer.get().buildSpan("Compute EGP").start();
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning

      /*
       * Initialize all routers and their message queues (can be done as parallel as possible)
       */
      LOGGER.info("Initialize virtual routers with updated topologies");
      Span initializationSpan =
          GlobalTracer.get()
              .buildSpan("Initialize virtual routers with updated topologies")
              .start();
      try (Scope innerScope = GlobalTracer.get().scopeManager().activate(initializationSpan)) {
        assert innerScope != null; // avoid unused warning
        vrs.parallelStream()
            .forEach(vr -> vr.initForEgpComputationWithNewTopology(topologyContext));
      } finally {
        initializationSpan.finish();
      }

      /*
       * Setup maps to track iterations. We need this for oscillation detection.
       * Specifically, if we detect that an iteration hashcode (a hash of all the nodes' RIBs)
       * has been previously encountered, we switch our schedule to a more restrictive one.
       */

      Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();

      Schedule currentSchedule = _settings.getScheduleName();

      // Go into iteration mode, until the routes converge (or oscillation is detected)
      do {
        _numIterations++;
        Span iterSpan = GlobalTracer.get().buildSpan("Iteration " + _numIterations).start();
        LOGGER.info("Iteration {} begins", _numIterations);
        try (Scope innerScope = GlobalTracer.get().scopeManager().activate(iterSpan)) {
          assert innerScope != null; // avoid unused warning

          IbdpSchedule schedule;
          Span computeScheduleSpan = GlobalTracer.get().buildSpan("Compute schedule").start();
          LOGGER.info("Compute schedule");
          try (Scope computeScheduleScope =
              GlobalTracer.get().scopeManager().activate(computeScheduleSpan)) {
            assert computeScheduleScope != null; // avoid unused warning
            // Compute node schedule
            schedule = IbdpSchedule.getSchedule(_settings, currentSchedule, nodes, topologyContext);
          } finally {
            computeScheduleSpan.finish();
          }

          // (Re)initialization of dependent route calculation
          //  Since this is a local step, coloring not required.

          LOGGER.info("Re-Init for new route iteration");
          Span depRoutesspan =
              GlobalTracer.get().buildSpan("Re-Init for new route iteration").start();

          try (Scope reiinitscope = GlobalTracer.get().scopeManager().activate(depRoutesspan)) {
            assert reiinitscope != null; // avoid unused warning

            vrs.parallelStream().forEach(VirtualRouter::reinitForNewIteration);
          } finally {
            depRoutesspan.finish();
          }

          /*
          Redistribution: take all the routes merged into the main RIB during previous iteration
          and offer them to each routing process.

          This must be called before any `executeIteration` calls on any routing process.
          Since this is a local step, coloring not required.
          */
          Span redistributeSpan = GlobalTracer.get().buildSpan("Redistribute").start();
          LOGGER.info("Redistribute");
          try (Scope redistscope = GlobalTracer.get().scopeManager().activate(redistributeSpan)) {
            assert redistscope != null; // avoid unused warning
            vrs.parallelStream().forEach(VirtualRouter::redistribute);

            // Handle cross-VRF leaking here too.
            queueRoutesForCrossVrfLeaking(vrs);
          } finally {
            redistributeSpan.finish();
          }

          // compute dependent routes for each allowable set of nodes until we cover all nodes
          int nodeSet = 0;
          while (schedule.hasNext()) {
            Map<String, Node> iterationNodes = schedule.next();
            List<VirtualRouter> iterationVrs =
                toListInRandomOrder(
                    iterationNodes.values().stream().flatMap(n -> n.getVirtualRouters().stream()));
            String iterationlabel =
                String.format("Iteration %d Schedule %d", _numIterations, nodeSet);
            computeDependentRoutesIteration(
                iterationVrs, iterationlabel, nodes, networkConfigurations, _numIterations);
            ++nodeSet;
          }

          // Tell each VR that a route computation round has ended.
          // This must be the last thing called on a VR in a routing round.
          vrs.parallelStream().forEach(VirtualRouter::endOfEgpRound);

          /*
           * Perform various bookkeeping at the end of the iteration:
           * - Collect sizes of certain RIBs this iteration
           * - Compute iteration hashcode
           * - Check for oscillations
           */
          computeIterationStatistics(vrs, ae, _numIterations);

          // This hashcode uniquely identifies the iteration (i.e., network state)
          int iterationHashCode = computeIterationHashCode(vrs);
          SortedSet<Integer> iterationsWithThisHashCode =
              iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());

          if (iterationsWithThisHashCode.isEmpty()) {
            iterationsWithThisHashCode.add(_numIterations);
          } else {
            // If oscillation detected, switch to a more restrictive schedule
            if (currentSchedule != Schedule.NODE_SERIALIZED) {
              LOGGER.debug(
                  "Switching to a more restrictive schedule {}, iteration {}",
                  Schedule.NODE_SERIALIZED,
                  _numIterations);
              currentSchedule = Schedule.NODE_SERIALIZED;
            } else {
              return true; // Found an oscillation
            }
          }
        } finally {
          iterSpan.finish();
        }
      } while (hasNotReachedRoutingFixedPoint(vrs));

      ae.setDependentRoutesIterations(_numIterations);
      return false; // No oscillations
    }
  }

  /** Check if we have reached a routing fixed point */
  private boolean hasNotReachedRoutingFixedPoint(List<VirtualRouter> vrs) {
    Span span =
        GlobalTracer.get()
            .buildSpan("Iteration " + _numIterations + ": Check if fixed-point reached")
            .start();
    LOGGER.info("Iteration {}: Check if fixed point reached", _numIterations);
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return vrs.parallelStream().anyMatch(VirtualRouter::isDirty);
    } finally {
      span.finish();
    }
  }

  /**
   * Compute the hashcode that uniquely identifies the state of the network at a given iteration
   *
   * @param vrs all virtual routers in the network
   * @return integer hashcode
   */
  private int computeIterationHashCode(List<VirtualRouter> vrs) {
    Span span =
        GlobalTracer.get().buildSpan("Iteration " + _numIterations + ": Compute hashCode").start();
    LOGGER.info("Iteration {}: Compute hashCode", _numIterations);
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      return vrs.parallelStream().mapToInt(VirtualRouter::computeIterationHashCode).sum();

    } finally {
      span.finish();
    }
  }

  private static void computeIterationStatistics(
      List<VirtualRouter> vrs, IncrementalBdpAnswerElement ae, int dependentRoutesIterations) {
    Span span = GlobalTracer.get().buildSpan("Compute iteration statistics").start();
    LOGGER.info("Iteration {}: Compute statistics", dependentRoutesIterations);
    try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
      assert scope != null; // avoid unused warning
      int numBgpBestPathRibRoutes =
          vrs.parallelStream().mapToInt(VirtualRouter::getNumBgpBestPaths).sum();
      ae.getBgpBestPathRibRoutesByIteration()
          .put(dependentRoutesIterations, numBgpBestPathRibRoutes);
      int numBgpMultipathRibRoutes =
          vrs.parallelStream().mapToInt(VirtualRouter::getNumBgpPaths).sum();
      ae.getBgpMultipathRibRoutesByIteration()
          .put(dependentRoutesIterations, numBgpMultipathRibRoutes);
      int numMainRibRoutes =
          vrs.parallelStream().mapToInt(vr -> vr.getMainRib().getTypedRoutes().size()).sum();
      ae.getMainRibRoutesByIteration().put(dependentRoutesIterations, numMainRibRoutes);
    } finally {
      span.finish();
    }
  }

  /**
   * Return the main RIB routes for each node. Map structure: Hostname -&gt; VRF name -&gt; Set of
   * routes
   */
  @VisibleForTesting
  static SortedMap<String, SortedMap<String, Set<AbstractRoute>>> getRoutes(
      IncrementalDataPlane dp) {
    // Scan through all Nodes and their virtual routers, retrieve main rib routes
    return toImmutableSortedMap(
        dp.getRibs(),
        Entry::getKey,
        nodeEntry ->
            toImmutableSortedMap(
                nodeEntry.getValue(),
                Entry::getKey,
                vrfEntry -> ImmutableSet.copyOf(vrfEntry.getValue().getRoutes())));
  }

  private static final int MAX_OSPF_INTERNAL_ITERATIONS = 100000;

  /**
   * Run the IGP OSPF computation until convergence.
   *
   * @param allNodes list of nodes for which to initialize the OSPF routes
   * @param ospfTopology graph of OSPF adjacencies
   * @return the number of iterations it took for internal OSPF routes to converge
   */
  private int initOspfInternalRoutes(Map<String, Node> allNodes, OspfTopology ospfTopology) {
    int ospfInternalIterations = 0;
    boolean dirty = true;

    while (dirty) {
      ospfInternalIterations++;
      Span span =
          GlobalTracer.get()
              .buildSpan("OSPF internal: iteration " + ospfInternalIterations)
              .start();
      LOGGER.info("OSPF internal: Iteration {}", ospfInternalIterations);
      try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
        assert scope != null; // avoid unused warning
        // Compute node schedule
        IbdpSchedule schedule =
            IbdpSchedule.getSchedule(
                _settings,
                _settings.getScheduleName(),
                allNodes,
                TopologyContext.builder().setOspfTopology(ospfTopology).build());

        while (schedule.hasNext()) {
          Map<String, Node> scheduleNodes = schedule.next();
          List<VirtualRouter> scheduleVrs =
              toListInRandomOrder(
                  scheduleNodes.values().stream().flatMap(n -> n.getVirtualRouters().stream()));
          scheduleVrs.parallelStream()
              .forEach(virtualRouter -> virtualRouter.ospfIteration(allNodes));
          scheduleVrs.parallelStream().forEach(VirtualRouter::mergeOspfRoutesToMainRib);
        }
        dirty =
            allNodes.values().parallelStream()
                .flatMap(n -> n.getVirtualRouters().stream())
                .flatMap(vr -> vr.getOspfProcesses().values().stream())
                .anyMatch(OspfRoutingProcess::isDirty);
        if (ospfInternalIterations > MAX_OSPF_INTERNAL_ITERATIONS) {
          throw new BdpOscillationException(
              "OSPF did not converge after " + MAX_OSPF_INTERNAL_ITERATIONS + " iterations");
        }
      } finally {
        span.finish();
      }
    }
    return ospfInternalIterations;
  }

  /**
   * Run the IGP RIP computation until convergence
   *
   * @param nodes nodes for which to initialize the routes, keyed by name
   * @param topology network topology
   */
  private static void initRipInternalRoutes(
      SortedMap<String, Node> nodes, List<VirtualRouter> vrs, Topology topology) {
    /*
     * Consider this method to be a simulation within a simulation. Since RIP routes are not
     * affected by other protocols, we propagate all RIP routes amongst the nodes prior to
     * processing other routing protocols (e.g., OSPF & BGP)
     */
    AtomicBoolean ripInternalChanged = new AtomicBoolean(true);
    int ripInternalIterations = 0;
    while (ripInternalChanged.get()) {
      ripInternalIterations++;
      ripInternalChanged.set(false);
      Span span =
          GlobalTracer.get().buildSpan("RIP internal: iteration " + ripInternalIterations).start();
      LOGGER.info("RIP internal: Iteration {}", ripInternalIterations);
      try (Scope scope = GlobalTracer.get().scopeManager().activate(span)) {
        assert scope != null; // avoid unused warning
        vrs.parallelStream()
            .forEach(
                vr -> {
                  if (vr.propagateRipInternalRoutes(nodes, topology)) {
                    ripInternalChanged.set(true);
                  }
                });
      } finally {
        span.finish();
      }
      Span unstageSpan =
          GlobalTracer.get()
              .buildSpan("Unstage RIP internal: iteration " + ripInternalIterations)
              .start();
      LOGGER.info("Unstage RIP internal: Iteration {}", ripInternalIterations);
      try (Scope scope = GlobalTracer.get().scopeManager().activate(unstageSpan)) {
        assert scope != null; // avoid unused warning
        vrs.parallelStream().forEach(VirtualRouter::unstageRipInternalRoutes);
      } finally {
        unstageSpan.finish();
      }
      Span importSpan =
          GlobalTracer.get()
              .buildSpan("Import RIP internal: iteration " + ripInternalIterations)
              .start();
      LOGGER.info("Import RIP internal: Iteration {}", ripInternalIterations);
      try (Scope scope = GlobalTracer.get().scopeManager().activate(importSpan)) {
        assert scope != null; // avoid unused warning
        vrs.parallelStream()
            .forEach(
                vr -> {
                  importRib(vr._ripRib, vr._ripInternalRib);
                  importRib(vr._independentRib, vr._ripRib, vr.getName());
                });
      }
    }
  }
}
