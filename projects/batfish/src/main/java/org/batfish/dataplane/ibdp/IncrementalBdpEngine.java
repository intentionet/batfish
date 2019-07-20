package org.batfish.dataplane.ibdp;

import static org.batfish.common.topology.IpOwners.computeIpNodeOwners;
import static org.batfish.common.topology.TopologyUtil.computeLayer2Topology;
import static org.batfish.common.topology.TopologyUtil.computeLayer3Topology;
import static org.batfish.common.topology.TopologyUtil.computeRawLayer3Topology;
import static org.batfish.common.topology.TopologyUtil.pruneUnreachableTunnelEdges;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.common.util.IpsecUtil.retainReachableIpsecEdges;
import static org.batfish.common.util.IpsecUtil.toEdgeSet;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.initBgpTopology;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.computeVxlanTopology;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.prunedVxlanTopology;
import static org.batfish.dataplane.rib.AbstractRib.importRib;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BdpOscillationException;
import org.batfish.common.Version;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.common.plugin.TracerouteEngine;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.topology.TunnelTopology;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.IncrementalBdpAnswerElement;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.eigrp.EigrpTopologyUtils;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.batfish.datamodel.vxlan.VxlanTopologyUtils;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule.Schedule;
import org.batfish.dataplane.rib.Bgpv4Rib;
import org.batfish.dataplane.rib.RibDelta;

class IncrementalBdpEngine {

  private static final int MAX_TOPOLOGY_ITERATIONS = 10;

  private int _numIterations;
  private final BatfishLogger _bfLogger;
  private final IncrementalDataPlaneSettings _settings;

  IncrementalBdpEngine(IncrementalDataPlaneSettings settings, BatfishLogger logger) {
    _settings = settings;
    _bfLogger = logger;
  }

  ComputeDataPlaneResult computeDataPlane(
      Map<String, Configuration> configurations,
      TopologyContext callerTopologyContext,
      Set<BgpAdvertisement> externalAdverts) {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("Compute Data Plane").startActive()) {
      assert span != null; // avoid unused warning

      _bfLogger.resetTimer();
      IncrementalDataPlane.Builder dpBuilder = IncrementalDataPlane.builder();
      _bfLogger.info("\nComputing Data Plane using iBDP\n");

      // TODO: switch to topologies and owners from TopologyProvider
      Map<Ip, Set<String>> ipOwners = computeIpNodeOwners(configurations, true);
      TopologyContext initialTopologyContext =
          callerTopologyContext
              .toBuilder()
              .setEigrpTopology(
                  EigrpTopologyUtils.initEigrpTopology(
                      configurations, callerTopologyContext.getLayer3Topology()))
              .setIsisTopology(
                  IsisTopology.initIsisTopology(
                      configurations, callerTopologyContext.getLayer3Topology()))
              .setVxlanTopology(VxlanTopologyUtils.computeVxlanTopology(configurations))
              .setTunnelTopology(TopologyUtil.computeInitialTunnelTopology(configurations))
              .build();

      // Generate our nodes, keyed by name, sorted for determinism
      SortedMap<String, Node> nodes =
          toImmutableSortedMap(configurations.values(), Configuration::getHostname, Node::new);
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
      computeIgpDataPlane(nodes, initialTopologyContext, answerElement, networkConfigurations);

      /*
       * Perform a fixed-point computation.
       */
      int topologyIterations = 0;
      TopologyContext currentTopologyContext = initialTopologyContext;
      boolean converged = false;
      while (!converged && topologyIterations++ < MAX_TOPOLOGY_ITERATIONS) {
        try (ActiveSpan iterSpan =
            GlobalTracer.get()
                .buildSpan("Topology iteration " + topologyIterations)
                .startActive()) {
          assert iterSpan != null; // avoid unused warning

          // Force re-init of partial dataplane. Re-inits forwarding analysis, etc.
          computeFibs(nodes);
          IncrementalDataPlane partialDataplane =
              dpBuilder
                  .setNodes(nodes)
                  .setLayer3Topology(currentTopologyContext.getLayer3Topology())
                  .build();

          TracerouteEngine trEngCurrentL3Topogy =
              new TracerouteEngineImpl(
                  partialDataplane, currentTopologyContext.getLayer3Topology());

          // Update topologies
          // IPsec
          IpsecTopology newIpsecTopology =
              retainReachableIpsecEdges(
                  initialTopologyContext.getIpsecTopology(), configurations, trEngCurrentL3Topogy);
          // VXLAN
          VxlanTopology newVxlanTopology =
              prunedVxlanTopology(
                  computeVxlanTopology(partialDataplane.getVniSettings()),
                  configurations,
                  trEngCurrentL3Topogy);
          // Layer-2
          Optional<Layer2Topology> newLayer2Topology =
              currentTopologyContext
                  .getLayer1LogicalTopology()
                  .map(l1 -> computeLayer2Topology(l1, newVxlanTopology, configurations));

          // Tunnel topology
          TunnelTopology newTunnelTopology =
              pruneUnreachableTunnelEdges(
                  initialTopologyContext.getTunnelTopology(),
                  networkConfigurations,
                  trEngCurrentL3Topogy);

          // Layer-3
          Topology newLayer3Topology =
              computeLayer3Topology(
                  computeRawLayer3Topology(
                      initialTopologyContext.getRawLayer1PhysicalTopology(),
                      initialTopologyContext.getLayer1LogicalTopology(),
                      newLayer2Topology,
                      configurations),
                  // Overlay edges consist of "plain" tunnels and IPSec tunnels
                  Sets.union(
                      toEdgeSet(newIpsecTopology, configurations), newTunnelTopology.asEdgeSet()));

          // Initialize BGP topology
          BgpTopology newBgpTopology =
              initBgpTopology(
                  configurations,
                  ipOwners,
                  false,
                  true,
                  new TracerouteEngineImpl(partialDataplane, newLayer3Topology),
                  initialTopologyContext.getLayer2Topology().orElse(null));
          TopologyContext newTopologyContext =
              currentTopologyContext
                  .toBuilder()
                  .setBgpTopology(newBgpTopology)
                  .setLayer2Topology(newLayer2Topology)
                  .setLayer3Topology(newLayer3Topology)
                  .setVxlanTopology(newVxlanTopology)
                  .setIpsecTopology(newIpsecTopology)
                  .setTunnelTopology(newTunnelTopology)
                  .build();

          boolean isOscillating =
              computeNonMonotonicPortionOfDataPlane(
                  nodes,
                  externalAdverts,
                  answerElement,
                  newTopologyContext,
                  networkConfigurations,
                  ipOwners);
          if (isOscillating) {
            // If we are oscillating here, network has no stable solution.
            throw new BdpOscillationException("Network has no stable solution");
          }

          converged = currentTopologyContext.equals(newTopologyContext);
          currentTopologyContext = newTopologyContext;
        }
      }

      if (!converged) {
        throw new BdpOscillationException(
            String.format(
                "Could not reach a fixed point topology in %d iterations",
                MAX_TOPOLOGY_ITERATIONS));
      }

      // Generate the answers from the computation, compute final FIBs
      // TODO: Properly finalize topologies, IpOwners, etc.
      computeFibs(nodes);
      answerElement.setVersion(Version.getVersion());
      IncrementalDataPlane finalDataplane =
          IncrementalDataPlane.builder()
              .setNodes(nodes)
              .setLayer3Topology(currentTopologyContext.getLayer3Topology())
              .build();
      _bfLogger.printElapsedTime();
      return new ComputeDataPlaneResult(answerElement, finalDataplane, currentTopologyContext);
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
   * @param nodes nodes that are participating in the computation
   * @param iterationLabel iteration label (for stats tracking)
   * @param allNodes all nodes in the network (for correct neighbor referencing)
   * @param topologyContext the various network topologies
   */
  private static void computeDependentRoutesIteration(
      Map<String, Node> nodes,
      String iterationLabel,
      Map<String, Node> allNodes,
      TopologyContext topologyContext,
      NetworkConfigurations networkConfigurations) {
    try (ActiveSpan overallSpan =
        GlobalTracer.get().buildSpan(iterationLabel + ": Compute dependent routes").startActive()) {
      assert overallSpan != null; // avoid unused warning

      try (ActiveSpan span =
          GlobalTracer.get().buildSpan(iterationLabel + ": Init dependent routes").startActive()) {
        assert span != null; // avoid unused warning
        // (Re)initialization of dependent route calculation
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().parallelStream())
            .forEach(VirtualRouter::reinitForNewIteration);
      }

      // Static nextHopIp routes
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": Recompute static routes with next-hop IP")
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(VirtualRouter::activateStaticRoutes);
      }

      // Generated/aggregate routes
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": Recompute aggregate/generated routes")
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(VirtualRouter::recomputeGeneratedRoutes);
      }

      // EIGRP external routes: recompute exports
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": Recompute EIGRP exports")
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(vr -> vr.initEigrpExports(allNodes));
      }

      // Re-propagate EIGRP exports
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": Recompute EIGRP external routes")
              .startActive()) {
        assert span != null; // avoid unused warning
        AtomicBoolean eigrpExternalChanged = new AtomicBoolean(true);
        int eigrpExternalSubIterations = 0;
        while (eigrpExternalChanged.get()) {
          eigrpExternalSubIterations++;
          try (ActiveSpan eigrpSpan =
              GlobalTracer.get()
                  .buildSpan(
                      iterationLabel
                          + ": Recompute EIGRP external routes: "
                          + eigrpExternalSubIterations)
                  .startActive()) {
            assert eigrpSpan != null; // avoid unused warning
            eigrpExternalChanged.set(false);
            nodes
                .values()
                .parallelStream()
                .flatMap(n -> n.getVirtualRouters().values().stream())
                .forEach(
                    vr -> {
                      if (vr.propagateEigrpExternalRoutes(allNodes, networkConfigurations)) {
                        eigrpExternalChanged.set(true);
                      }
                    });
          }
        }
      }

      // Re-initialize IS-IS exports.
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": Recompute IS-IS exports")
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(vr -> vr.initIsisExports(allNodes, networkConfigurations));
      }

      // IS-IS route propagation
      AtomicBoolean isisChanged = new AtomicBoolean(true);
      int isisSubIterations = 0;
      while (isisChanged.get()) {
        isisSubIterations++;
        try (ActiveSpan isisSpan =
            GlobalTracer.get()
                .buildSpan(
                    iterationLabel + ": Recompute IS-IS routes: subIteration: " + isisSubIterations)
                .startActive()) {
          assert isisSpan != null; // avoid unused warning
          isisChanged.set(false);
          nodes
              .values()
              .parallelStream()
              .flatMap(n -> n.getVirtualRouters().values().stream())
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
        }
      }

      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan(iterationLabel + ": propagate OSPF external")
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(vr -> vr.ospfIteration(allNodes));
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(VirtualRouter::mergeOspfRoutesToMainRib);
      }

      computeIterationOfBgpRoutes(
          nodes, iterationLabel, allNodes, topologyContext.getBgpTopology(), networkConfigurations);

      try (ActiveSpan span =
          GlobalTracer.get().buildSpan(iterationLabel + ": Redistribute").startActive()) {
        assert span != null; // avoid unused warning
        nodes.values().stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(VirtualRouter::redistribute);
      }

      queueRoutesForCrossVrfLeaking(nodes, iterationLabel);
      leakAcrossVrfs(nodes, iterationLabel);
    }
  }

  private static void computeIterationOfBgpRoutes(
      Map<String, Node> nodes,
      String iterationLabel,
      Map<String, Node> allNodes,
      BgpTopology bgpTopology,
      NetworkConfigurations networkConfigurations) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan(iterationLabel + ": Init for new BGP iteration")
            .startActive()) {
      assert span != null;
      nodes
          .values()
          .parallelStream()
          .forEach(n -> n.getVirtualRouters().values().forEach(vr -> vr.bgpIteration(allNodes)));
    }
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan(iterationLabel + ": Init BGP generated/aggregate routes")
            .startActive()) {
      assert span != null; // avoid unused warning
      // first let's initialize nodes-level generated/aggregate routes
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> n.getVirtualRouters().values().forEach(VirtualRouter::initBgpAggregateRoutes));
    }

    try (ActiveSpan span =
        GlobalTracer.get().buildSpan(iterationLabel + ": Propagate BGP routes").startActive()) {
      assert span != null; // avoid unused warning
      nodes
          .values()
          .parallelStream()
          .flatMap(n -> n.getVirtualRouters().values().stream())
          .forEach(VirtualRouter::mergeBgpRoutesToMainRib);
      nodes
          .values()
          .parallelStream()
          .flatMap(n -> n.getVirtualRouters().values().stream())
          .forEach(
              vr -> {
                Map<Bgpv4Rib, RibDelta<Bgpv4Route>> deltas =
                    vr.processBgpMessages(bgpTopology, networkConfigurations, nodes);
                vr.finalizeBgpRoutesAndQueueOutgoingMessages(
                    deltas, allNodes, bgpTopology, networkConfigurations);
              });
    }
  }

  private static void queueRoutesForCrossVrfLeaking(
      Map<String, Node> nodes, String iterationLabel) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan(iterationLabel + ": Queueing routes to leak across VRFs")
            .startActive()) {
      assert span != null; // avoid unused warning

      nodes
          .values()
          .parallelStream()
          .flatMap(n -> n.getVirtualRouters().values().stream())
          .forEach(VirtualRouter::queueCrossVrfImports);
    }
  }

  private static void leakAcrossVrfs(Map<String, Node> nodes, String iterationLabel) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan(iterationLabel + ": Leaking routes across VRFs")
            .startActive()) {
      assert span != null; // avoid unused warning
      nodes
          .values()
          .parallelStream()
          .flatMap(n -> n.getVirtualRouters().values().stream())
          .forEach(VirtualRouter::processCrossVrfRoutes);
    }
  }

  /**
   * Run {@link VirtualRouter#computeFib} on all of the given nodes (and their virtual routers)
   *
   * @param nodes mapping of node names to node instances
   */
  private void computeFibs(Map<String, Node> nodes) {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("Compute FIBs").startActive()) {
      assert span != null; // avoid unused warning
      nodes
          .values()
          .parallelStream()
          .flatMap(n -> n.getVirtualRouters().values().stream())
          .forEach(VirtualRouter::computeFib);
    }
  }

  /**
   * Compute the IGP portion of the dataplane.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param topologyContext The topology context in which various adjacencies are stored
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   * @param networkConfigurations All configurations in the network
   */
  private void computeIgpDataPlane(
      SortedMap<String, Node> nodes,
      TopologyContext topologyContext,
      IncrementalBdpAnswerElement ae,
      NetworkConfigurations networkConfigurations) {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("Compute IGP").startActive()) {
      assert span != null; // avoid unused warning

      int numOspfInternalIterations;
      int numEigrpInternalIterations;

      /*
       * For each virtual router, setup the initial easy-to-do routes, init protocol-based RIBs,
       * queue outgoing messages to neighbors
       */
      try (ActiveSpan initializeSpan =
          GlobalTracer.get().buildSpan("Initialize for IGP computation").startActive()) {
        assert initializeSpan != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(vr -> vr.initForIgpComputation(topologyContext));
      }

      // EIGRP internal routes
      numEigrpInternalIterations =
          initEigrpInternalRoutes(nodes, topologyContext, networkConfigurations);

      // OSPF internal routes
      numOspfInternalIterations = initOspfInternalRoutes(nodes, topologyContext.getOspfTopology());

      // RIP internal routes
      initRipInternalRoutes(nodes, topologyContext.getLayer3Topology());

      // Activate static routes
      try (ActiveSpan staticSpan =
          GlobalTracer.get()
              .buildSpan("Compute static routes post IGP convergence")
              .startActive()) {
        assert staticSpan != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(
                vr -> {
                  importRib(vr.getMainRib(), vr._independentRib);
                  vr.activateStaticRoutes();
                });
      }

      // Set iteration stats in the answer
      ae.setOspfInternalIterations(numOspfInternalIterations);
      ae.setEigrpInternalIterations(numEigrpInternalIterations);
    }
  }

  /**
   * Compute the EGP portion of the route exchange. Must be called after IGP routing has converged.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param externalAdverts the set of external BGP advertisements
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   * @param topologyContext The various network topologies
   * @param ipOwners The ip owner mapping
   * @return true iff the computation is oscillating
   */
  private boolean computeNonMonotonicPortionOfDataPlane(
      SortedMap<String, Node> nodes,
      Set<BgpAdvertisement> externalAdverts,
      IncrementalBdpAnswerElement ae,
      TopologyContext topologyContext,
      NetworkConfigurations networkConfigurations,
      Map<Ip, Set<String>> ipOwners) {
    try (ActiveSpan span = GlobalTracer.get().buildSpan("Compute EGP").startActive()) {
      assert span != null; // avoid unused warning

      /*
       * Initialize all routers and their message queues (can be done as parallel as possible)
       */
      try (ActiveSpan innerSpan =
          GlobalTracer.get()
              .buildSpan("Initialize virtual routers for iBDP-external")
              .startActive()) {
        assert innerSpan != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(vr -> vr.initForEgpComputation(topologyContext));
      }

      try (ActiveSpan innerSpan =
          GlobalTracer.get().buildSpan("Queue initial cross-VRF leaking").startActive()) {
        assert innerSpan != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(VirtualRouter::initCrossVrfImports);
      }

      try (ActiveSpan innerSpan =
          GlobalTracer.get().buildSpan("Queue initial bgp messages").startActive()) {
        assert innerSpan != null; // avoid unused warning
        // Queue initial outgoing messages
        BgpTopology bgpTopology = topologyContext.getBgpTopology();
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(
                vr -> {
                  vr.processExternalBgpAdvertisements(
                      externalAdverts, ipOwners, nodes, bgpTopology, networkConfigurations);
                  vr.queueInitialBgpMessages(bgpTopology, nodes, networkConfigurations);
                });
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
        try (ActiveSpan iterSpan =
            GlobalTracer.get().buildSpan("Iteration " + _numIterations).startActive()) {
          assert iterSpan != null; // avoid unused warning

          IbdpSchedule schedule;
          try (ActiveSpan innerSpan =
              GlobalTracer.get().buildSpan("Compute schedule").startActive()) {
            assert innerSpan != null; // avoid unused warning
            // Compute node schedule
            schedule = IbdpSchedule.getSchedule(_settings, currentSchedule, nodes, topologyContext);
          }

          // compute dependent routes for each allowable set of nodes until we cover all nodes
          int nodeSet = 0;
          while (schedule.hasNext()) {
            Map<String, Node> iterationNodes = schedule.next();
            String iterationlabel =
                String.format("Iteration %d Schedule %d", _numIterations, nodeSet);
            computeDependentRoutesIteration(
                iterationNodes, iterationlabel, nodes, topologyContext, networkConfigurations);
            ++nodeSet;
          }

          /*
           * Perform various bookkeeping at the end of the iteration:
           * - Collect sizes of certain RIBs this iteration
           * - Compute iteration hashcode
           * - Check for oscillations
           */
          computeIterationStatistics(nodes, ae, _numIterations);

          // This hashcode uniquely identifies the iteration (i.e., network state)
          int iterationHashCode = computeIterationHashCode(nodes);
          SortedSet<Integer> iterationsWithThisHashCode =
              iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());

          if (iterationsWithThisHashCode.isEmpty()) {
            iterationsWithThisHashCode.add(_numIterations);
          } else {
            // If oscillation detected, switch to a more restrictive schedule
            if (currentSchedule != Schedule.NODE_SERIALIZED) {
              _bfLogger.debugf(
                  "Switching to a more restrictive schedule %s, iteration %d\n",
                  Schedule.NODE_SERIALIZED, _numIterations);
              currentSchedule = Schedule.NODE_SERIALIZED;
            } else {
              return true; // Found an oscillation
            }
          }
        }
      } while (hasNotReachedRoutingFixedPoint(nodes));

      ae.setDependentRoutesIterations(_numIterations);
      return false; // No oscillations
    }
  }

  /** Check if we have reached a routing fixed point */
  private boolean hasNotReachedRoutingFixedPoint(Map<String, Node> nodes) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("Iteration " + _numIterations + ": Check if fixed-point reached")
            .startActive()) {
      assert span != null; // avoid unused warning
      return nodes
          .values()
          .parallelStream()
          .flatMap(n -> n.getVirtualRouters().values().stream())
          .anyMatch(VirtualRouter::isDirty);
    }
  }

  /**
   * Compute the hashcode that uniquely identifies the state of the network at a given iteration
   *
   * @param nodes map of nodes, keyed by hostname
   * @return integer hashcode
   */
  private int computeIterationHashCode(Map<String, Node> nodes) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("Iteration " + _numIterations + ": Compute hashCode")
            .startActive()) {
      assert span != null; // avoid unused warning
      return nodes
          .values()
          .parallelStream()
          .flatMap(node -> node.getVirtualRouters().values().stream())
          .mapToInt(VirtualRouter::computeIterationHashCode)
          .sum();
    }
  }

  private static void computeIterationStatistics(
      Map<String, Node> nodes, IncrementalBdpAnswerElement ae, int dependentRoutesIterations) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("Compute iteration statistics").startActive()) {
      assert span != null; // avoid unused warning
      int numBgpBestPathRibRoutes =
          nodes.values().stream()
              .flatMap(n -> n.getVirtualRouters().values().stream())
              .mapToInt(VirtualRouter::getNumBgpBestPaths)
              .sum();
      ae.getBgpBestPathRibRoutesByIteration()
          .put(dependentRoutesIterations, numBgpBestPathRibRoutes);
      int numBgpMultipathRibRoutes =
          nodes.values().stream()
              .flatMap(n -> n.getVirtualRouters().values().stream())
              .mapToInt(VirtualRouter::getNumBgpPaths)
              .sum();
      ae.getBgpMultipathRibRoutesByIteration()
          .put(dependentRoutesIterations, numBgpMultipathRibRoutes);
      int numMainRibRoutes =
          nodes.values().stream()
              .flatMap(n -> n.getVirtualRouters().values().stream())
              .mapToInt(vr -> vr.getMainRib().getTypedRoutes().size())
              .sum();
      ae.getMainRibRoutesByIteration().put(dependentRoutesIterations, numMainRibRoutes);
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
        dp.getNodes(),
        Entry::getKey,
        nodeEntry ->
            toImmutableSortedMap(
                nodeEntry.getValue().getVirtualRouters(),
                Entry::getKey,
                vrfEntry -> ImmutableSet.copyOf(vrfEntry.getValue().getMainRib().getRoutes())));
  }

  /**
   * Run the IGP EIGRP computation until convergence.
   *
   * @param nodes list of nodes for which to initialize the EIGRP routes
   * @param topologyContext The topology context in which EIGRP adjacencies are stored
   * @param networkConfigurations All configurations in the network
   * @return the number of iterations it took for internal EIGRP routes to converge
   */
  private static int initEigrpInternalRoutes(
      Map<String, Node> nodes,
      TopologyContext topologyContext,
      NetworkConfigurations networkConfigurations) {
    AtomicBoolean eigrpInternalChanged = new AtomicBoolean(true);
    int eigrpInternalIterations = 0;
    while (eigrpInternalChanged.get()) {
      eigrpInternalIterations++;
      eigrpInternalChanged.set(false);

      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan("Compute EIGRP internal routes: iteration " + eigrpInternalIterations)
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(
                vr -> {
                  if (vr.propagateEigrpInternalRoutes(
                      nodes, topologyContext, networkConfigurations)) {
                    eigrpInternalChanged.set(true);
                  }
                });
      }
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan("Unstage EIGRP internal routes: iteration " + eigrpInternalIterations)
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(VirtualRouter::unstageEigrpInternalRoutes);
      }
    }
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("Import EIGRP internal routes").startActive()) {
      assert span != null; // avoid unused warning
      nodes
          .values()
          .parallelStream()
          .flatMap(n -> n.getVirtualRouters().values().stream())
          .forEach(VirtualRouter::importEigrpInternalRoutes);
    }
    return eigrpInternalIterations;
  }

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

      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan("OSPF internal: iteration " + ospfInternalIterations)
              .startActive()) {
        assert span != null; // avoid unused warning
        // Compute node schedule
        IbdpSchedule schedule =
            IbdpSchedule.getSchedule(
                _settings,
                _settings.getScheduleName(),
                allNodes,
                TopologyContext.builder().setOspfTopology(ospfTopology).build());

        while (schedule.hasNext()) {
          Map<String, Node> scheduleNodes = schedule.next();
          scheduleNodes
              .values()
              .parallelStream()
              .flatMap(n -> n.getVirtualRouters().values().stream())
              .forEach(virtualRouter -> virtualRouter.ospfIteration(allNodes));

          scheduleNodes
              .values()
              .parallelStream()
              .flatMap(n -> n.getVirtualRouters().values().stream())
              .forEach(VirtualRouter::mergeOspfRoutesToMainRib);
        }
        dirty =
            allNodes
                .values()
                .parallelStream()
                .flatMap(n -> n.getVirtualRouters().values().stream())
                .flatMap(vr -> vr.getOspfProcesses().values().stream())
                .anyMatch(OspfRoutingProcess::isDirty);
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
  private static void initRipInternalRoutes(SortedMap<String, Node> nodes, Topology topology) {
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
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan("RIP internal: iteration " + ripInternalIterations)
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(
                vr -> {
                  if (vr.propagateRipInternalRoutes(nodes, topology)) {
                    ripInternalChanged.set(true);
                  }
                });
      }
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan("Unstage RIP internal: iteration " + ripInternalIterations)
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(VirtualRouter::unstageRipInternalRoutes);
      }
      try (ActiveSpan span =
          GlobalTracer.get()
              .buildSpan("Import RIP internal: iteration " + ripInternalIterations)
              .startActive()) {
        assert span != null; // avoid unused warning
        nodes
            .values()
            .parallelStream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .forEach(
                vr -> {
                  importRib(vr._ripRib, vr._ripInternalRib);
                  importRib(vr._independentRib, vr._ripRib, vr.getName());
                });
      }
    }
  }
}
