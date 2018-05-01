package org.batfish.dataplane.ibdp;

import static org.batfish.common.util.CommonUtil.initBgpTopology;
import static org.batfish.dataplane.ibdp.AbstractRib.importRib;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.graph.Network;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BdpOscillationException;
import org.batfish.common.Version;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.BdpAnswerElement;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule.Schedule;

public class IncrementalBdpEngine {

  private int _numIterations;

  private final BatfishLogger _bfLogger;

  private final BiFunction<String, Integer, AtomicInteger> _newBatch;

  private final IncrementalDataPlaneSettings _settings;

  public IncrementalBdpEngine(
      IncrementalDataPlaneSettings settings,
      BatfishLogger logger,
      BiFunction<String, Integer, AtomicInteger> newBatch) {
    _settings = settings;
    _bfLogger = logger;
    _newBatch = newBatch;
  }

  IncrementalDataPlane computeDataPlane(
      boolean differentialContext,
      Map<String, Configuration> configurations,
      Topology topology,
      Set<BgpAdvertisement> externalAdverts,
      BdpAnswerElement ae) {
    _bfLogger.resetTimer();
    IncrementalDataPlane dp = new IncrementalDataPlane();
    _bfLogger.info("\nComputing Data Plane using iBDP\n");

    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(configurations, true);
    Map<Ip, String> ipOwnersSimple = CommonUtil.computeIpOwnersSimple(ipOwners);
    dp.initIpOwners(configurations, ipOwners, ipOwnersSimple);

    // Generate our nodes, keyed by name, sorted for determinism
    ImmutableSortedMap.Builder<String, Node> builder =
        new ImmutableSortedMap.Builder<>(Ordering.natural());
    configurations.values().forEach(c -> builder.put(c.getHostname(), new Node(c)));
    ImmutableSortedMap<String, Node> nodes = builder.build();
    dp.setNodes(nodes);
    dp.setTopology(topology);

    /*
     * Run the data plane computation here:
     * - First, let the IGP routes converge
     * - Second, re-init BGP neighbors with reachability checks
     * - Third, let the EGP routes converge
     * - Finally, compute FIBs, return answer
     */
    computeIgpDataPlane(nodes, topology, ae);
    computeFibs(nodes);

    Network<BgpNeighbor, BgpSession> bgpTopology =
        initBgpTopology(
            configurations, dp.getIpOwners(), false, true, TracerouteEngineImpl.getInstance(), dp);

    boolean isOscillating =
        computeNonMonotonicPortionOfDataPlane(
            nodes, topology, dp, externalAdverts, ae, true, bgpTopology);
    if (isOscillating) {
      // If we are oscillating here, network has no stable solution.
      throw new BdpOscillationException("Network has no stable solution");
    }

    if (_settings.getCheckBgpSessionReachability()) {
      computeFibs(nodes);
      bgpTopology =
          initBgpTopology(
              configurations,
              dp.getIpOwners(),
              false,
              true,
              TracerouteEngineImpl.getInstance(),
              dp);
      // Update queues (if necessary) based on new neighbor relationships
      final Network<BgpNeighbor, BgpSession> finalBgpTopology = bgpTopology;
      nodes
          .values()
          .parallelStream()
          .forEach(
              n ->
                  n.getVirtualRouters().values().forEach(vr -> vr.initBgpQueues(finalBgpTopology)));
      // Do another pass on EGP computation in case any new sessions have been established
      computeNonMonotonicPortionOfDataPlane(
          nodes, topology, dp, externalAdverts, ae, false, bgpTopology);
    }
    // Generate the answers from the computation, compute final FIBs
    computeFibs(nodes);
    ae.setVersion(Version.getVersion());
    _bfLogger.printElapsedTime();
    return dp;
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
   * @param topology network Topology
   * @param dp data place instance
   * @param iteration iteration number (for stats tracking)
   * @param allNodes all nodes in the network (for correct neighbor referencing)
   * @param bgpTopology the bgp peering relationships
   */
  private void computeDependentRoutesIteration(
      Map<String, Node> nodes,
      Topology topology,
      IncrementalDataPlane dp,
      int iteration,
      Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    // (Re)initialization of dependent route calculation
    AtomicInteger reinitializeDependentCompleted =
        _newBatch.apply("Iteration " + iteration + ": Reinitialize dependent routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {

                /*
                 * For RIBs that do not require comparison to previous version, just re-init
                 */
                vr.reinitForNewIteration(allNodes);
              }
              reinitializeDependentCompleted.incrementAndGet();
            });

    // Static nextHopIp routes
    AtomicInteger recomputeStaticCompleted =
        _newBatch.apply(
            "Iteration " + iteration + ": Recompute static routes with next-hop IP", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.activateStaticRoutes();
              }
              recomputeStaticCompleted.incrementAndGet();
            });

    // Generated/aggregate routes
    AtomicInteger recomputeAggregateCompleted =
        _newBatch.apply(
            "Iteration " + iteration + ": Recompute aggregate/generated routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.recomputeGeneratedRoutes();
              }
              recomputeAggregateCompleted.incrementAndGet();
            });

    // OSPF external routes
    // recompute exports
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.initOspfExports();
              }
            });

    // Re-propagate OSPF exports
    AtomicBoolean ospfExternalChanged = new AtomicBoolean(true);
    int ospfExternalSubIterations = 0;
    while (ospfExternalChanged.get()) {
      ospfExternalSubIterations++;
      AtomicInteger propagateOspfExternalCompleted =
          _newBatch.apply(
              "Iteration "
                  + iteration
                  + ": Propagate OSPF external routes: subIteration: "
                  + ospfExternalSubIterations,
              nodes.size());
      ospfExternalChanged.set(false);
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  Entry<RibDelta<OspfExternalType1Route>, RibDelta<OspfExternalType2Route>> p =
                      vr.propagateOspfExternalRoutes(allNodes, topology);
                  if (p != null && vr.unstageOspfExternalRoutes(p.getKey(), p.getValue())) {
                    ospfExternalChanged.set(true);
                  }
                }
                propagateOspfExternalCompleted.incrementAndGet();
              });
    }

    computeIterationOfBgpRoutes(nodes, dp, iteration, allNodes, bgpTopology);
  }

  private void computeIterationOfBgpRoutes(
      Map<String, Node> nodes,
      IncrementalDataPlane dp,
      int iteration,
      Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {
    // BGP routes
    // first let's initialize nodes-level generated/aggregate routes
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                if (vr._vrf.getBgpProcess() != null) {
                  vr.initBgpAggregateRoutes();
                }
              }
            });
    AtomicInteger propagateBgpCompleted =
        _newBatch.apply("Iteration " + iteration + ": Propagate BGP routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                BgpProcess proc = vr._vrf.getBgpProcess();
                if (proc == null) {
                  continue;
                }
                Map<BgpMultipathRib, RibDelta<BgpRoute>> deltas =
                    vr.processBgpMessages(dp.getIpOwners(), bgpTopology);
                vr.finalizeBgpRoutesAndQueueOutgoingMessages(
                    proc.getMultipathEbgp(),
                    proc.getMultipathIbgp(),
                    deltas,
                    allNodes,
                    bgpTopology);
              }
              propagateBgpCompleted.incrementAndGet();
            });
  }

  /**
   * Run {@link VirtualRouter#computeFib} on all of the given nodes (and their virtual routers)
   *
   * @param nodes mapping of node names to node instances
   */
  private void computeFibs(Map<String, Node> nodes) {
    AtomicInteger completed = _newBatch.apply("Computing FIBs", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.computeFib();
              }
              completed.incrementAndGet();
            });
  }

  /**
   * Compute the IGP portion of the dataplane.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param topology The topology representing Layer 3 adjacencies between interface of the nodes
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   */
  private void computeIgpDataPlane(
      SortedMap<String, Node> nodes, Topology topology, BdpAnswerElement ae) {

    int numOspfInternalIterations;

    /*
     * For each virtual router, setup the initial easy-to-do routes, init protocol-based RIBs,
     * queue outgoing messages to neighbors
     */
    AtomicInteger initialCompleted =
        _newBatch.apply(
            "Compute initial connected and static routes, ospf setup, bgp setup", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.initForIgpComputation();
              }
              initialCompleted.incrementAndGet();
            });

    // OSPF internal routes
    numOspfInternalIterations = initOspfInternalRoutes(nodes, topology);

    // RIP internal routes
    initRipInternalRoutes(nodes, topology);

    // Activate static routes
    AtomicInteger staticRoutesAfterIgp =
        _newBatch.apply("Compute static routes after IGP protocol convergence", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                importRib(vr._mainRib, vr._independentRib);
                vr.activateStaticRoutes();
              }
              staticRoutesAfterIgp.incrementAndGet();
            });

    // Set iteration stats in the answer
    ae.setOspfInternalIterations(numOspfInternalIterations);
  }

  /**
   * Compute the EGP portion of the dataplane. Must be called after IGP has converged.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param topology The topology representing Layer 3 adjacencies between interface of the nodes
   * @param dp The output data plane
   * @param externalAdverts the set of external BGP advertisements
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   * @return true iff the computation is oscillating
   */
  private boolean computeNonMonotonicPortionOfDataPlane(
      SortedMap<String, Node> nodes,
      Topology topology,
      IncrementalDataPlane dp,
      Set<BgpAdvertisement> externalAdverts,
      BdpAnswerElement ae,
      boolean firstPass,
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    /*
     * Initialize all routers and their message queues (can be done as parallel as possible)
     */
    if (firstPass) {
      AtomicInteger setupCompleted =
          _newBatch.apply("Initialize virtual routers for iBDP-external", nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  vr.initForEgpComputation(
                      dp.getIpOwners(), externalAdverts, nodes, topology, bgpTopology);
                }
                setupCompleted.incrementAndGet();
              });

      // Queue initial outgoing messages
      AtomicInteger queueInitial = _newBatch.apply("Queue initial bgp messages", nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  vr.initBaseBgpRibs(externalAdverts, dp.getIpOwners(), nodes, bgpTopology);
                  vr.queueInitialBgpMessages(bgpTopology, nodes);
                }
                queueInitial.incrementAndGet();
              });
    }

    /*
     * Setup maps to track iterations. We need this for oscillation detection.
     * Specifically, if we detect that an iteration hashcode (a hash of all the nodes' RIBs)
     * has been previously encountered, we switch our schedule to a more restrictive one.
     */

    Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();

    AtomicBoolean dependentRoutesChanged = new AtomicBoolean(false);

    // Go into iteration mode, until the routes converge (or oscillation is detected)
    do {
      _numIterations++;

      AtomicBoolean currentChangedMonitor;
      currentChangedMonitor = dependentRoutesChanged;
      currentChangedMonitor.set(false);

      // Compute node schedule
      IbdpSchedule schedule = IbdpSchedule.getSchedule(_settings, nodes, bgpTopology);

      // compute dependent routes for each allowable set of nodes until we cover all nodes
      while (schedule.hasNext()) {
        Map<String, Node> iterationNodes = schedule.next();
        computeDependentRoutesIteration(
            iterationNodes, topology, dp, _numIterations, nodes, bgpTopology);
      }

      /*
       * Perform various bookkeeping at the end of the iteration:
       * - Collect sizes of certain RIBs this iteration
       * - Compute iteration hashcode
       * - Check for oscillations
       */
      computeIterationStatistics(nodes, ae, _numIterations);

      // Check to see if hash has changed
      AtomicInteger checkFixedPointCompleted =
          _newBatch.apply(
              "Iteration " + _numIterations + ": Check if fixed-point reached", nodes.size());

      // This hashcode uniquely identifies the iteration (i.e., network state)
      int iterationHashCode = computeIterationHashCode(nodes);
      SortedSet<Integer> iterationsWithThisHashCode =
          iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());

      if (iterationsWithThisHashCode.isEmpty()) {
        iterationsWithThisHashCode.add(_numIterations);
      } else {
        // If oscillation detected, switch to a more restrictive schedule
        if (_settings.getScheduleName() != Schedule.NODE_SERIALIZED) {
          _bfLogger.debugf(
              "Switching to a more restrictive schedule %s, iteration %d\n",
              Schedule.NODE_SERIALIZED, _numIterations);
          _settings.setScheduleName(Schedule.NODE_SERIALIZED);
        } else {
          return true; // Found an oscillation
        }
      }

      compareToPreviousIteration(nodes, dependentRoutesChanged, checkFixedPointCompleted);
    } while (!areQueuesEmpty(nodes, bgpTopology) || dependentRoutesChanged.get());

    // After convergence, compute BGP advertisements sent to the outside of the network
    AtomicInteger computeBgpAdvertisementsToOutsideCompleted =
        _newBatch.apply("Compute BGP advertisements sent to outside", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.computeBgpAdvertisementsToOutside(dp.getIpOwners());
              }
              computeBgpAdvertisementsToOutsideCompleted.incrementAndGet();
            });

    dp.setBgpTopology(bgpTopology);
    ae.setDependentRoutesIterations(_numIterations);
    return false; // No oscillations
  }

  private void compareToPreviousIteration(
      Map<String, Node> nodes,
      AtomicBoolean dependentRoutesChanged,
      AtomicInteger checkFixedPointCompleted) {
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                if (vr.hasOutstandingRoutes()) {
                  dependentRoutesChanged.set(true);
                }
              }
              checkFixedPointCompleted.incrementAndGet();
            });
  }

  /**
   * Check that the routers have processed all messages, queues are empty and there is nothing else
   * to do (i.e., we've converged to a stable network solution)
   *
   * @param nodes nodes to check
   * @param bgpTopology the bgp peering relationships
   * @return true iff all queues are empty
   */
  private boolean areQueuesEmpty(
      Map<String, Node> nodes, Network<BgpNeighbor, BgpSession> bgpTopology) {
    AtomicInteger computeQueuesAreEmpty =
        _newBatch.apply("Check for convergence (are queues empty?)", nodes.size());
    AtomicBoolean areEmpty = new AtomicBoolean(true);
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                if (!vr.hasProcessedAllMessages(bgpTopology)) {
                  areEmpty.set(false);
                }
              }
              computeQueuesAreEmpty.incrementAndGet();
            });

    return areEmpty.get();
  }

  /**
   * Compute the hashcode that uniquely identifies the state of hte network at a given iteration
   *
   * @param nodes map of nodes
   * @return integer hashcode
   */
  private static int computeIterationHashCode(Map<String, Node> nodes) {
    return nodes
        .values()
        .parallelStream()
        .flatMap(node -> node.getVirtualRouters().values().stream())
        .mapToInt(VirtualRouter::computeIterationHashCode)
        .sum();
  }

  private void computeIterationStatistics(
      Map<String, Node> nodes, BdpAnswerElement ae, int dependentRoutesIterations) {
    int numBgpBestPathRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n._virtualRouters.values().stream())
            .mapToInt(vr -> vr._bgpBestPathRib.getRoutes().size())
            .sum();
    ae.getBgpBestPathRibRoutesByIteration().put(dependentRoutesIterations, numBgpBestPathRibRoutes);
    int numBgpMultipathRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n._virtualRouters.values().stream())
            .mapToInt(vr -> vr._bgpMultipathRib.getRoutes().size())
            .sum();
    ae.getBgpMultipathRibRoutesByIteration()
        .put(dependentRoutesIterations, numBgpMultipathRibRoutes);
    int numMainRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n._virtualRouters.values().stream())
            .mapToInt(vr -> vr._mainRib.getRoutes().size())
            .sum();
    ae.getMainRibRoutesByIteration().put(dependentRoutesIterations, numMainRibRoutes);
  }

  SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(
      IncrementalDataPlane dp) {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByHostname =
        new TreeMap<>();
    for (Entry<String, Node> e : dp._nodes.entrySet()) {
      String hostname = e.getKey();
      Node node = e.getValue();
      for (Entry<String, VirtualRouter> e2 : node._virtualRouters.entrySet()) {
        String vrfName = e2.getKey();
        VirtualRouter vrf = e2.getValue();
        for (AbstractRoute route : vrf._mainRib.getRoutes()) {
          SortedMap<String, SortedSet<AbstractRoute>> routesByVrf =
              routesByHostname.computeIfAbsent(hostname, k -> new TreeMap<>());
          SortedSet<AbstractRoute> routes =
              routesByVrf.computeIfAbsent(vrfName, k -> new TreeSet<>());
          routes.add(route);
        }
      }
    }
    return routesByHostname;
  }

  /**
   * Run the IGP OSPF computation until convergence.
   *
   * @param nodes list of nodes for which to initialize the OSPF routes
   * @param topology the network topology
   * @return the number of iterations it took for internal OSPF routes to converge
   */
  int initOspfInternalRoutes(Map<String, Node> nodes, Topology topology) {
    AtomicBoolean ospfInternalChanged = new AtomicBoolean(true);
    int ospfInternalIterations = 0;
    while (ospfInternalChanged.get()) {
      ospfInternalIterations++;
      ospfInternalChanged.set(false);

      AtomicInteger ospfInterAreaSummaryCompleted =
          _newBatch.apply(
              "Compute OSPF Inter-area summaries: iteration " + ospfInternalIterations,
              nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  if (vr.computeInterAreaSummaries()) {
                    ospfInternalChanged.set(true);
                  }
                }
                ospfInterAreaSummaryCompleted.incrementAndGet();
              });
      AtomicInteger ospfInternalCompleted =
          _newBatch.apply(
              "Compute OSPF Internal routes: iteration " + ospfInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  if (vr.propagateOspfInternalRoutes(nodes, topology)) {
                    ospfInternalChanged.set(true);
                  }
                }
                ospfInternalCompleted.incrementAndGet();
              });
      AtomicInteger ospfInternalUnstageCompleted =
          _newBatch.apply(
              "Unstage OSPF Internal routes: iteration " + ospfInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  vr.unstageOspfInternalRoutes();
                }
                ospfInternalUnstageCompleted.incrementAndGet();
              });
    }
    AtomicInteger ospfInternalImportCompleted =
        _newBatch.apply("Import OSPF Internal routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.importOspfInternalRoutes();
              }
              ospfInternalImportCompleted.incrementAndGet();
            });
    return ospfInternalIterations;
  }

  /**
   * Run the IGP RIP computation until convergence
   *
   * @param nodes nodes for which to initialize the routes, keyed by name
   * @param topology network topology
   * @return number of iterations it took to complete the initialization
   */
  private int initRipInternalRoutes(SortedMap<String, Node> nodes, Topology topology) {
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
      AtomicInteger ripInternalCompleted =
          _newBatch.apply(
              "Compute RIP Internal routes: iteration " + ripInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  if (vr.propagateRipInternalRoutes(nodes, topology)) {
                    ripInternalChanged.set(true);
                  }
                }
                ripInternalCompleted.incrementAndGet();
              });
      AtomicInteger ripInternalUnstageCompleted =
          _newBatch.apply(
              "Unstage RIP Internal routes: iteration " + ripInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  vr.unstageRipInternalRoutes();
                }
                ripInternalUnstageCompleted.incrementAndGet();
              });
    }
    AtomicInteger ripInternalImportCompleted =
        _newBatch.apply("Import RIP Internal routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                importRib(vr._ripRib, vr._ripInternalRib);
                importRib(vr._independentRib, vr._ripRib);
              }
              ripInternalImportCompleted.incrementAndGet();
            });
    return ripInternalIterations;
  }
}
