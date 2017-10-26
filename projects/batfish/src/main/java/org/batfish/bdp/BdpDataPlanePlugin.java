package org.batfish.bdp;

import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.apache.commons.collections4.map.LRUMap;
import org.batfish.common.BatfishException;
import org.batfish.common.BdpOscillationException;
import org.batfish.common.Pair;
import org.batfish.common.Version;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.BdpSettings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.FlowTraceHop;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RouteBuilder;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.Answer;
import org.batfish.datamodel.answers.BdpAnswerElement;
import org.batfish.datamodel.collections.AdvertisementSet;
import org.batfish.datamodel.collections.EdgeSet;
import org.batfish.datamodel.collections.IbgpTopology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.RouteSet;

@AutoService(Plugin.class)
public class BdpDataPlanePlugin extends DataPlanePlugin {

  private static final String TRACEROUTE_INGRESS_NODE_INTERFACE_NAME =
      "traceroute_source_interface";

  private static final String TRACEROUTE_INGRESS_NODE_NAME = "traceroute_source_node";

  /**
   * Applies the given list of source NAT rules to the given flow and returns the new transformed
   * flow. If {@code sourceNats} is null, empty, or does not contain any ACL rules matching the
   * {@link Flow}, the original flow is returned.
   *
   * <p>Each {@link SourceNat} is expected to be valid: it must have a NAT IP or pool.
   */
  static Flow applySourceNat(Flow flow, @Nullable List<SourceNat> sourceNats) {
    if (CommonUtil.isNullOrEmpty(sourceNats)) {
      return flow;
    }

    for (SourceNat sourceNat : sourceNats) {
      IpAccessList acl = sourceNat.getAcl();
      if (acl != null) {
        FilterResult result = acl.filter(flow);
        if (result.getAction() == LineAction.REJECT) {
          // This ACL does not match the flow.
          continue;
        }
      }

      Ip natPoolStartIp = sourceNat.getPoolIpFirst();
      if (natPoolStartIp == null) {
        throw new BatfishException(
            String.format(
                "Error processing Source NAT rule %s: missing NAT address or pool", sourceNat));
      }
      Flow.Builder transformedFlowBuilder = new Flow.Builder(flow);
      transformedFlowBuilder.setSrcIp(natPoolStartIp);
      return transformedFlowBuilder.build();
    }

    // No NAT rule matched.
    return flow;
  }

  private int _maxRecordedIterations;

  private final Map<BdpDataPlane, Map<Flow, Set<FlowTrace>>> _flowTraces;

  private BdpSettings _settings;

  public BdpDataPlanePlugin() {
    _flowTraces = new HashMap<>();
  }

  private boolean checkDependentRoutesChanged(
      AtomicBoolean dependentRoutesChanged,
      AtomicBoolean evenDependentRoutesChanged,
      AtomicBoolean oddDependentRoutesChanged,
      SortedSet<Prefix> oscillatingPrefixes,
      int dependentRoutesIterations) {
    if (oscillatingPrefixes.isEmpty()) {
      return dependentRoutesChanged.get();
    } else if (dependentRoutesIterations % 2 == 1) {
      return true;
    } else {
      return evenDependentRoutesChanged.get() || oddDependentRoutesChanged.get();
    }
  }

  private void collectFlowTraces(
      BdpDataPlane dp,
      String currentNodeName,
      Set<Edge> visitedEdges,
      List<FlowTraceHop> hopsSoFar,
      Set<FlowTrace> flowTraces,
      Flow originalFlow,
      Flow transformedFlow) {
    Ip dstIp = transformedFlow.getDstIp();
    Set<String> dstIpOwners = dp._ipOwners.get(dstIp);
    if (dstIpOwners != null && dstIpOwners.contains(currentNodeName)) {
      FlowTrace trace =
          new FlowTrace(FlowDisposition.ACCEPTED, hopsSoFar, FlowDisposition.ACCEPTED.toString());
      flowTraces.add(trace);
    } else {
      Node currentNode = dp._nodes.get(currentNodeName);
      String vrfName;
      if (hopsSoFar.isEmpty()) {
        vrfName = transformedFlow.getIngressVrf();
      } else {
        FlowTraceHop lastHop = hopsSoFar.get(hopsSoFar.size() - 1);
        String receivingInterface = lastHop.getEdge().getInt2();
        vrfName = currentNode._c.getInterfaces().get(receivingInterface).getVrf().getName();
      }
      VirtualRouter currentVirtualRouter = currentNode._virtualRouters.get(vrfName);
      Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfacesByRoute =
          currentVirtualRouter._fib.getNextHopInterfacesByRoute(dstIp);
      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfacesWithRoutes =
          currentVirtualRouter._fib.getNextHopInterfaces(dstIp);
      if (!nextHopInterfacesWithRoutes.isEmpty()) {
        for (String nextHopInterfaceName : nextHopInterfacesWithRoutes.keySet()) {
          // SortedSet<String> routesForThisNextHopInterface = new
          // TreeSet<>(
          // nextHopInterfacesWithRoutes.get(nextHopInterfaceName)
          // .stream().map(ar -> ar.toString())
          // .collect(Collectors.toSet()));
          SortedSet<String> routesForThisNextHopInterface = new TreeSet<>();
          Ip finalNextHopIp = null;
          for (Entry<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> e :
              nextHopInterfacesByRoute.entrySet()) {
            AbstractRoute routeCandidate = e.getKey();
            Map<String, Map<Ip, Set<AbstractRoute>>> routeCandidateNextHopInterfaces = e.getValue();
            if (routeCandidateNextHopInterfaces.containsKey(nextHopInterfaceName)) {
              Ip nextHopIp = routeCandidate.getNextHopIp();
              if (!nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
                Set<Ip> finalNextHopIps =
                    routeCandidateNextHopInterfaces.get(nextHopInterfaceName).keySet();
                if (finalNextHopIps.size() > 1) {
                  throw new BatfishException(
                      "Can not currently handle multiple final next hop ips across multiple "
                          + "routes leading to one next hop interface");
                }
                Ip newFinalNextHopIp = finalNextHopIps.iterator().next();
                if (finalNextHopIp != null && !newFinalNextHopIp.equals(finalNextHopIp)) {
                  throw new BatfishException(
                      "Can not currently handle multiple final next hop ips for same next hop "
                          + "interface");
                }
                finalNextHopIp = newFinalNextHopIp;
              }
              routesForThisNextHopInterface.add(routeCandidate + "_fnhip:" + finalNextHopIp);
            }
          }
          NodeInterfacePair nextHopInterface =
              new NodeInterfacePair(currentNodeName, nextHopInterfaceName);
          if (nextHopInterfaceName.equals(Interface.NULL_INTERFACE_NAME)) {
            List<FlowTraceHop> newHops = new ArrayList<>(hopsSoFar);
            Edge newEdge =
                new Edge(
                    nextHopInterface,
                    new NodeInterfacePair(
                        Configuration.NODE_NONE_NAME, Interface.NULL_INTERFACE_NAME));
            FlowTraceHop newHop =
                new FlowTraceHop(
                    newEdge, routesForThisNextHopInterface, hopFlow(originalFlow, transformedFlow));
            newHops.add(newHop);
            FlowTrace nullRouteTrace =
                new FlowTrace(
                    FlowDisposition.NULL_ROUTED, newHops, FlowDisposition.NULL_ROUTED.toString());
            flowTraces.add(nullRouteTrace);
          } else {
            Interface outgoingInterface =
                dp._nodes
                    .get(nextHopInterface.getHostname())
                    ._c
                    .getInterfaces()
                    .get(nextHopInterface.getInterface());

            // Apply any relevant source NAT rules.
            transformedFlow = applySourceNat(transformedFlow, outgoingInterface.getSourceNats());

            EdgeSet edges = dp._topology.getInterfaceEdges().get(nextHopInterface);
            if (edges != null) {
              boolean continueToNextNextHopInterface = false;
              continueToNextNextHopInterface =
                  processCurrentNextHopInterfaceEdges(
                      dp,
                      currentNodeName,
                      visitedEdges,
                      hopsSoFar,
                      flowTraces,
                      originalFlow,
                      transformedFlow,
                      dstIp,
                      dstIpOwners,
                      nextHopInterfaceName,
                      routesForThisNextHopInterface,
                      finalNextHopIp,
                      nextHopInterface,
                      edges,
                      true);
              if (continueToNextNextHopInterface) {
                continue;
              }
            } else {
              /*
               * Should only get here for delta environment where
               * non-flow-sink interface from base has no edges in delta
               */
              Edge neighborUnreachbleEdge =
                  new Edge(
                      nextHopInterface,
                      new NodeInterfacePair(
                          Configuration.NODE_NONE_NAME, Interface.NULL_INTERFACE_NAME));
              FlowTraceHop neighborUnreachableHop =
                  new FlowTraceHop(
                      neighborUnreachbleEdge,
                      routesForThisNextHopInterface,
                      hopFlow(originalFlow, transformedFlow));
              List<FlowTraceHop> newHops = new ArrayList<>(hopsSoFar);
              newHops.add(neighborUnreachableHop);
              /** Check if denied out. If not, make standard neighbor-unreachable trace. */
              IpAccessList outFilter = outgoingInterface.getOutgoingFilter();
              boolean denied = false;
              if (outFilter != null) {
                FlowDisposition disposition = FlowDisposition.NEIGHBOR_UNREACHABLE_OR_DENIED_OUT;
                denied =
                    flowTraceDeniedHelper(
                        flowTraces, originalFlow, transformedFlow, newHops, outFilter, disposition);
              }
              if (!denied) {
                FlowTrace trace =
                    new FlowTrace(
                        FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
                        newHops,
                        FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK.toString());
                flowTraces.add(trace);
              }
            }
          }
        }
      } else {
        FlowTrace trace =
            new FlowTrace(FlowDisposition.NO_ROUTE, hopsSoFar, FlowDisposition.NO_ROUTE.toString());
        flowTraces.add(trace);
      }
    }
  }

  private SortedSet<Prefix> collectOscillatingPrefixes(
      Map<Integer, RouteSet> iterationRoutes,
      Map<Integer, SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>>>
          iterationAbsRoutes,
      int first,
      int last) {
    SortedSet<Prefix> oscillatingPrefixes = new TreeSet<>();
    if (_settings.getBdpDetail()) {
      for (int i = first + 1; i <= last; i++) {
        SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> baseRoutesByHostname =
            iterationAbsRoutes.get(i - 1);
        SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> deltaRoutesByHostname =
            iterationAbsRoutes.get(i);
        SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByHostname =
            new TreeMap<>();
        Set<String> hosts = new LinkedHashSet<>();
        hosts.addAll(baseRoutesByHostname.keySet());
        hosts.addAll(deltaRoutesByHostname.keySet());
        for (String hostname : hosts) {
          SortedMap<String, SortedSet<AbstractRoute>> routesByVrf = new TreeMap<>();
          routesByHostname.put(hostname, routesByVrf);
          SortedMap<String, SortedSet<AbstractRoute>> baseRoutesByVrf =
              baseRoutesByHostname.get(hostname);
          SortedMap<String, SortedSet<AbstractRoute>> deltaRoutesByVrf =
              deltaRoutesByHostname.get(hostname);
          if (baseRoutesByVrf == null) {
            for (Entry<String, SortedSet<AbstractRoute>> e : deltaRoutesByVrf.entrySet()) {
              String vrfName = e.getKey();
              SortedSet<AbstractRoute> deltaRoutes = e.getValue();
              SortedSet<AbstractRoute> routes = new TreeSet<>();
              routesByVrf.put(vrfName, routes);
              for (AbstractRoute deltaRoute : deltaRoutes) {
                oscillatingPrefixes.add(deltaRoute.getNetwork());
              }
            }
          } else if (deltaRoutesByVrf == null) {
            for (Entry<String, SortedSet<AbstractRoute>> e : baseRoutesByVrf.entrySet()) {
              String vrfName = e.getKey();
              SortedSet<AbstractRoute> baseRoutes = e.getValue();
              SortedSet<AbstractRoute> routes = new TreeSet<>();
              routesByVrf.put(vrfName, routes);
              for (AbstractRoute baseRoute : baseRoutes) {
                oscillatingPrefixes.add(baseRoute.getNetwork());
              }
            }
          } else {
            Set<String> vrfNames = new LinkedHashSet<>();
            vrfNames.addAll(baseRoutesByVrf.keySet());
            vrfNames.addAll(deltaRoutesByVrf.keySet());
            for (String vrfName : vrfNames) {
              SortedSet<AbstractRoute> baseRoutes = baseRoutesByVrf.get(vrfName);
              SortedSet<AbstractRoute> deltaRoutes = deltaRoutesByVrf.get(vrfName);
              if (baseRoutes == null) {
                for (AbstractRoute deltaRoute : deltaRoutes) {
                  oscillatingPrefixes.add(deltaRoute.getNetwork());
                }
              } else if (deltaRoutes == null) {
                for (AbstractRoute baseRoute : baseRoutes) {
                  oscillatingPrefixes.add(baseRoute.getNetwork());
                }
              } else {
                SortedSet<AbstractRoute> prunedBaseRoutes =
                    CommonUtil.difference(baseRoutes, deltaRoutes, TreeSet::new);
                SortedSet<AbstractRoute> prunedDeltaRoutes =
                    CommonUtil.difference(deltaRoutes, baseRoutes, TreeSet::new);
                for (AbstractRoute baseRoute : prunedBaseRoutes) {
                  oscillatingPrefixes.add(baseRoute.getNetwork());
                }
                for (AbstractRoute deltaRoute : prunedDeltaRoutes) {
                  oscillatingPrefixes.add(deltaRoute.getNetwork());
                }
              }
            }
          }
        }
      }
    } else {
      for (int i = first + 1; i <= last; i++) {
        RouteSet baseRoutes = iterationRoutes.get(i - 1);
        RouteSet deltaRoutes = iterationRoutes.get(i);
        RouteSet added = CommonUtil.difference(deltaRoutes, baseRoutes, RouteSet::new);
        RouteSet removed = CommonUtil.difference(baseRoutes, deltaRoutes, RouteSet::new);
        RouteSet changed = CommonUtil.union(added, removed, RouteSet::new);
        for (Route route : changed) {
          oscillatingPrefixes.add(route.getNetwork());
        }
      }
    }
    return oscillatingPrefixes;
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
                boolean changed = false;
                if (!vr._mainRib.getRoutes().equals(vr._prevMainRib.getRoutes())) {
                  changed = true;
                }
                if (!vr._ospfExternalType1Rib
                    .getRoutes()
                    .equals(vr._prevOspfExternalType1Rib.getRoutes())) {
                  changed = true;
                }
                if (!vr._ospfExternalType2Rib
                    .getRoutes()
                    .equals(vr._prevOspfExternalType2Rib.getRoutes())) {
                  changed = true;
                }
                if (changed) {
                  dependentRoutesChanged.set(true);
                }
              }
              checkFixedPointCompleted.incrementAndGet();
            });
  }

  @Override
  public Answer computeDataPlane(boolean differentialContext) {
    Answer answer = new Answer();
    BdpDataPlane dp = new BdpDataPlane();
    BdpAnswerElement ae = new BdpAnswerElement();
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Topology topology = _batfish.computeTopology(configurations);
    _batfish.resetTimer();
    _logger.info("\n*** COMPUTING DATA PLANE ***\n");
    Map<Ip, Set<String>> ipOwners = _batfish.computeIpOwners(configurations, true);
    Map<Ip, String> ipOwnersSimple = _batfish.computeIpOwnersSimple(ipOwners);
    dp.initIpOwners(configurations, ipOwners, ipOwnersSimple);
    _batfish.initRemoteBgpNeighbors(configurations, dp.getIpOwners());
    SortedMap<String, Node> nodes = new TreeMap<>();
    AdvertisementSet externalAdverts = _batfish.processExternalBgpAnnouncements(configurations);
    SortedMap<Integer, SortedMap<Integer, Integer>> recoveryIterationHashCodes = new TreeMap<>();
    do {
      configurations.values().forEach(c -> nodes.put(c.getHostname(), new Node(c, nodes)));
    } while (computeFixedPoint(
        nodes, topology, dp, externalAdverts, ae, recoveryIterationHashCodes));
    computeFibs(nodes);
    dp.setNodes(nodes);
    dp.setTopology(topology);
    dp.setFlowSinks(_batfish.computeFlowSinks(configurations, differentialContext, topology));
    ae.setVersion(Version.getVersion());
    _batfish.newBatch("Writing data plane to disk", 0);
    _batfish.writeDataPlane(dp, ae);
    _batfish.printElapsedTime();
    answer.addAnswerElement(ae);
    return answer;
  }

  private void computeDependentRoutesIteration(
      Map<String, Node> nodes,
      Topology topology,
      BdpDataPlane dp,
      int dependentRoutesIterations,
      SortedSet<Prefix> oscillatingPrefixes) {

    /*
     * Oscillation recovery data structures
     */
    Map<BgpNeighbor, Map<BgpNeighbor, List<BgpAdvertisement>>> deferredBgpAdvertisements =
        new IdentityHashMap<>();
    Map<BgpNeighbor, Map<BgpNeighbor, List<BgpRoute>>> deferredIncomingRoutes =
        new IdentityHashMap<>();
    Map<BgpNeighbor, Map<BgpNeighbor, List<BgpMultipathRib>>> deferredIncomingRouteRibs =
        new IdentityHashMap<>();
    Map<Pair<String, String>, Map<Pair<String, String>, Set<Prefix>>> markedPrefixes =
        new HashMap<>();

    // (Re)initialization of dependent route calculation
    AtomicInteger reinitializeDependentCompleted =
        _batfish.newBatch(
            "Iteration " + dependentRoutesIterations + ": Reinitialize dependent routes",
            nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {

                /*
                 * RIBs that are read from
                 */
                vr._prevMainRib = vr._mainRib;
                vr._mainRib = new Rib(vr);

                vr._prevOspfExternalType1Rib = vr._ospfExternalType1Rib;
                vr._ospfExternalType1Rib = new OspfExternalType1Rib(vr);

                vr._prevOspfExternalType2Rib = vr._ospfExternalType2Rib;
                vr._ospfExternalType2Rib = new OspfExternalType2Rib(vr);

                vr._prevBgpMultipathRib = vr._bgpMultipathRib;
                vr._bgpMultipathRib = new BgpMultipathRib(vr);

                vr._prevBgpBestPathRib = vr._bgpBestPathRib;
                vr._bgpBestPathRib = new BgpBestPathRib(vr, vr._prevBgpBestPathRib, true);

                vr._prevEbgpMultipathRib = vr._ebgpMultipathRib;
                vr._ebgpMultipathRib = new BgpMultipathRib(vr);
                vr.importRib(vr._ebgpMultipathRib, vr._baseEbgpRib);

                vr._prevEbgpBestPathRib = vr._ebgpBestPathRib;
                vr._ebgpBestPathRib = new BgpBestPathRib(vr, vr._prevEbgpBestPathRib, true);
                vr.importRib(vr._ebgpBestPathRib, vr._baseEbgpRib);

                vr._prevIbgpBestPathRib = vr._ibgpBestPathRib;
                vr._ibgpBestPathRib = new BgpBestPathRib(vr, vr._prevIbgpBestPathRib, true);
                vr.importRib(vr._ibgpBestPathRib, vr._baseIbgpRib);

                vr._prevIbgpMultipathRib = vr._ibgpMultipathRib;
                vr._ibgpMultipathRib = new BgpMultipathRib(vr);
                vr.importRib(vr._ibgpMultipathRib, vr._baseIbgpRib);

                /*
                 * RIBs not read from
                 */
                vr._ospfRib = new OspfRib(vr);
                vr._ripRib = new RipRib(vr);

                /*
                 * Staging RIBs
                 */
                vr._ebgpStagingRib = new BgpMultipathRib(vr);
                vr._ibgpStagingRib = new BgpMultipathRib(vr);
                vr._ospfExternalType1StagingRib = new OspfExternalType1Rib(vr);
                vr._ospfExternalType2StagingRib = new OspfExternalType2Rib(vr);

                /*
                 * Add routes that cannot change (does not affect below
                 * computation)
                 */
                vr.importRib(vr._mainRib, vr._independentRib);

                /*
                 * Re-add independent OSPF routes to ospfRib for tie-breaking
                 */
                vr.importRib(vr._ospfRib, vr._ospfIntraAreaRib);
                vr.importRib(vr._ospfRib, vr._ospfInterAreaRib);
                /*
                 * Re-add independent RIP routes to ripRib for tie-breaking
                 */
                vr.importRib(vr._ripRib, vr._ripInternalRib);
              }
              reinitializeDependentCompleted.incrementAndGet();
            });

    // Static nextHopIp routes
    AtomicInteger recomputeStaticCompleted =
        _batfish.newBatch(
            "Iteration " + dependentRoutesIterations + ": Recompute static routes with next-hop IP",
            nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              boolean staticChanged;
              do {
                staticChanged = false;
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  if (vr.activateStaticRoutes()) {
                    staticChanged = true;
                  }
                }
              } while (staticChanged);
              recomputeStaticCompleted.incrementAndGet();
            });

    // Generated/aggregate routes
    AtomicInteger recomputeAggregateCompleted =
        _batfish.newBatch(
            "Iteration " + dependentRoutesIterations + ": Recompute aggregate/generated routes",
            nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                boolean generatedChanged;
                vr._generatedRib = new Rib(vr);
                do {
                  generatedChanged = false;
                  if (vr.activateGeneratedRoutes()) {
                    generatedChanged = true;
                  }
                } while (generatedChanged);
                vr.importRib(vr._mainRib, vr._generatedRib);
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

    // repropagate exports
    final AtomicBoolean ospfExternalChanged = new AtomicBoolean(true);
    int ospfExternalSubIterations = 0;
    while (ospfExternalChanged.get()) {
      ospfExternalSubIterations++;
      AtomicInteger propagateOspfExternalCompleted =
          _batfish.newBatch(
              "Iteration "
                  + dependentRoutesIterations
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
                  if (vr.propagateOspfExternalRoutes(nodes, topology)) {
                    ospfExternalChanged.set(true);
                  }
                }
                propagateOspfExternalCompleted.incrementAndGet();
              });
      AtomicInteger unstageOspfExternalCompleted =
          _batfish.newBatch(
              "Iteration "
                  + dependentRoutesIterations
                  + ": Unstage OSPF external routes: subIteration: "
                  + ospfExternalSubIterations,
              nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n._virtualRouters.values()) {
                  vr.unstageOspfExternalRoutes();
                }
                unstageOspfExternalCompleted.incrementAndGet();
              });
    }
    AtomicInteger importOspfExternalCompleted =
        _batfish.newBatch(
            "Iteration " + dependentRoutesIterations + ": Unstage OSPF external routes",
            nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.importRib(vr._ospfRib, vr._ospfExternalType1Rib);
                vr.importRib(vr._ospfRib, vr._ospfExternalType2Rib);
                vr.importRib(vr._mainRib, vr._ospfRib);
              }
              importOspfExternalCompleted.incrementAndGet();
            });

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
        _batfish.newBatch(
            "Iteration " + dependentRoutesIterations + ": Propagate BGP routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.propagateBgpRoutes(
                    nodes,
                    dp.getIpOwners(),
                    dependentRoutesIterations,
                    oscillatingPrefixes,
                    deferredBgpAdvertisements,
                    deferredIncomingRoutes,
                    deferredIncomingRouteRibs,
                    markedPrefixes);
              }
              propagateBgpCompleted.incrementAndGet();
            });
    AtomicInteger propagateDeferredBgpCompleted =
        _batfish.newBatch(
            "Iteration "
                + dependentRoutesIterations
                + ": Propagate deferred BGP routes (oscillation recovery)",
            nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.propagateDeferredBgpRoutes(
                    nodes,
                    dp.getIpOwners(),
                    dependentRoutesIterations,
                    oscillatingPrefixes,
                    deferredBgpAdvertisements,
                    deferredIncomingRoutes,
                    deferredIncomingRouteRibs,
                    markedPrefixes);
              }
              propagateDeferredBgpCompleted.incrementAndGet();
            });
    AtomicInteger importBgpCompleted =
        _batfish.newBatch(
            "Iteration " + dependentRoutesIterations + ": Import BGP routes into respective RIBs",
            nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                BgpProcess proc = vr._vrf.getBgpProcess();
                if (proc != null) {
                  vr.finalizeBgpRoutes(proc.getMultipathEbgp(), proc.getMultipathIbgp());
                }
              }
              importBgpCompleted.incrementAndGet();
            });
  }

  private void computeFibs(Map<String, Node> nodes) {
    AtomicInteger completed = _batfish.newBatch("Computing FIBs", nodes.size());
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
   * Attempt to compute the fixed point of the data plane.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param topology The topology representing physical adjacencies between interface of the nodes
   * @param dp The output data plane
   * @param externalAdverts Optional external BGP advertisements fed into the data plane computation
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   * @param recoveryIterationHashCodes Dependent-route computation iteration hash-code dictionaries,
   *     themselves keyed by outer recovery iteration.
   * @return true iff the computation is oscillating
   */
  private boolean computeFixedPoint(
      SortedMap<String, Node> nodes,
      Topology topology,
      BdpDataPlane dp,
      AdvertisementSet externalAdverts,
      BdpAnswerElement ae,
      SortedMap<Integer, SortedMap<Integer, Integer>> recoveryIterationHashCodes) {
    SortedSet<Prefix> oscillatingPrefixes = ae.getOscillatingPrefixes();
    // BEGIN DONE ONCE (except main rib)
    // connected, initial static routes, ospf setup, bgp setup
    AtomicInteger initialCompleted =
        _batfish.newBatch(
            "Compute initial connected and static routes, ospf setup, bgp setup", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.initConnectedRib();
                vr.importRib(vr._independentRib, vr._connectedRib);
                vr.importRib(vr._mainRib, vr._connectedRib);
                vr.initStaticRib();
                vr.importRib(vr._independentRib, vr._staticInterfaceRib);
                vr.importRib(vr._mainRib, vr._staticInterfaceRib);
                vr.initBaseOspfRoutes();
                vr.initBaseRipRoutes();
                vr.initEbgpTopology(dp);
                vr.initBaseBgpRibs(externalAdverts, dp.getIpOwners());
              }
              initialCompleted.incrementAndGet();
            });

    // OSPF internal routes
    final AtomicBoolean ospfInternalChanged = new AtomicBoolean(true);
    int ospfInternalIterations = 0;
    while (ospfInternalChanged.get()) {
      ospfInternalIterations++;
      ospfInternalChanged.set(false);
      AtomicInteger ospfInterAreaSummaryCompleted =
          _batfish.newBatch(
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
          _batfish.newBatch(
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
          _batfish.newBatch(
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
        _batfish.newBatch("Import OSPF Internal routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.importRib(vr._ospfRib, vr._ospfIntraAreaRib);
                vr.importRib(vr._ospfRib, vr._ospfInterAreaRib);
                vr.importRib(vr._independentRib, vr._ospfRib);
              }
              ospfInternalImportCompleted.incrementAndGet();
            });
    // END DONE ONCE (OSPF)

    // RIP internal routes
    final AtomicBoolean ripInternalChanged = new AtomicBoolean(true);
    int ripInternalIterations = 0;
    while (ripInternalChanged.get()) {
      ripInternalIterations++;
      ripInternalChanged.set(false);
      AtomicInteger ripInternalCompleted =
          _batfish.newBatch(
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
          _batfish.newBatch(
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
        _batfish.newBatch("Import RIP Internal routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n._virtualRouters.values()) {
                vr.importRib(vr._ripRib, vr._ripInternalRib);
                vr.importRib(vr._independentRib, vr._ripRib);
              }
              ripInternalImportCompleted.incrementAndGet();
            });
    // END DONE ONCE (RIP)

    Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();
    SortedMap<Integer, Integer> iterationHashCodes = new TreeMap<>();
    Map<Integer, RouteSet> iterationRoutes = null;
    Map<Integer, SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>>>
        iterationAbstractRoutes = null;
    if (_settings.getBdpRecordAllIterations()) {
      if (_settings.getBdpDetail()) {
        iterationAbstractRoutes = new TreeMap<>();
      } else {
        iterationRoutes = new TreeMap<>();
      }
    } else if (_maxRecordedIterations > 0) {
      if (_settings.getBdpDetail()) {
        iterationAbstractRoutes = new LRUMap<>(_maxRecordedIterations);
      } else {
        iterationRoutes = new LRUMap<>(_maxRecordedIterations);
      }
    }
    AtomicBoolean dependentRoutesChanged = new AtomicBoolean(false);
    AtomicBoolean evenDependentRoutesChanged = new AtomicBoolean(false);
    AtomicBoolean oddDependentRoutesChanged = new AtomicBoolean(false);
    int dependentRoutesIterations = 0;
    do {
      dependentRoutesIterations++;
      AtomicBoolean currentChangedMonitor;
      if (oscillatingPrefixes.isEmpty()) {
        currentChangedMonitor = dependentRoutesChanged;
      } else if (dependentRoutesIterations % 2 == 0) {
        currentChangedMonitor = evenDependentRoutesChanged;
      } else {
        currentChangedMonitor = oddDependentRoutesChanged;
      }
      currentChangedMonitor.set(false);
      computeDependentRoutesIteration(
          nodes, topology, dp, dependentRoutesIterations, oscillatingPrefixes);

      /* Collect sizes of certain RIBs this iteration */
      computeIterationStatistics(nodes, ae, dependentRoutesIterations);

      recordIterationDebugInfo(
          nodes, dp, iterationRoutes, iterationAbstractRoutes, dependentRoutesIterations);

      // Check to see if hash has changed
      AtomicInteger checkFixedPointCompleted =
          _batfish.newBatch(
              "Iteration " + dependentRoutesIterations + ": Check if fixed-point reached",
              nodes.size());
      int iterationHashCode = computeIterationHashCode(nodes);
      SortedSet<Integer> iterationsWithThisHashCode =
          iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());
      iterationHashCodes.put(dependentRoutesIterations, iterationHashCode);
      int minNumberOfUnchangedIterationsForConvergence = oscillatingPrefixes.isEmpty() ? 1 : 2;
      if (iterationsWithThisHashCode.isEmpty()
          || (!oscillatingPrefixes.isEmpty()
              && iterationsWithThisHashCode.equals(
                  Collections.singleton(dependentRoutesIterations - 1)))) {
        iterationsWithThisHashCode.add(dependentRoutesIterations);
      } else if (!iterationsWithThisHashCode.contains(
          dependentRoutesIterations - minNumberOfUnchangedIterationsForConvergence)) {
        int lowestIterationWithThisHashCode = iterationsWithThisHashCode.first();
        int completedOscillationRecoveryAttempts = ae.getCompletedOscillationRecoveryAttempts();
        if (!oscillatingPrefixes.isEmpty()) {
          completedOscillationRecoveryAttempts++;
          ae.setCompletedOscillationRecoveryAttempts(completedOscillationRecoveryAttempts);
        }
        recoveryIterationHashCodes.put(completedOscillationRecoveryAttempts, iterationHashCodes);
        handleOscillation(
            recoveryIterationHashCodes,
            iterationRoutes,
            iterationAbstractRoutes,
            lowestIterationWithThisHashCode,
            dependentRoutesIterations,
            oscillatingPrefixes,
            ae.getCompletedOscillationRecoveryAttempts());
        return true;
      }
      compareToPreviousIteration(nodes, currentChangedMonitor, checkFixedPointCompleted);
    } while (checkDependentRoutesChanged(
        dependentRoutesChanged,
        evenDependentRoutesChanged,
        oddDependentRoutesChanged,
        oscillatingPrefixes,
        dependentRoutesIterations));
    ae.setOspfInternalIterations(ospfInternalIterations);
    ae.setDependentRoutesIterations(dependentRoutesIterations);
    return false;
  }

  private int computeIterationHashCode(Map<String, Node> nodes) {
    int mainHash =
        nodes
            .values()
            .parallelStream()
            .mapToInt(
                n ->
                    n._virtualRouters
                        .values()
                        .stream()
                        .mapToInt(vr -> vr._mainRib.getRoutes().hashCode())
                        .sum())
            .sum();
    int ospfExternalType1Hash =
        nodes
            .values()
            .parallelStream()
            .mapToInt(
                n ->
                    n._virtualRouters
                        .values()
                        .stream()
                        .mapToInt(vr -> vr._ospfExternalType1Rib.getRoutes().hashCode())
                        .sum())
            .sum();
    int ospfExternalType2Hash =
        nodes
            .values()
            .parallelStream()
            .mapToInt(
                n ->
                    n._virtualRouters
                        .values()
                        .stream()
                        .mapToInt(vr -> vr._ospfExternalType2Rib.getRoutes().hashCode())
                        .sum())
            .sum();
    int hash = mainHash + ospfExternalType1Hash + ospfExternalType2Hash;
    return hash;
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

  private SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>>
      computeOutputAbstractRoutes(Map<String, Node> nodes, Map<Ip, String> ipOwners) {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> outputRoutes = new TreeMap<>();
    nodes.forEach(
        (hostname, node) -> {
          SortedMap<String, SortedSet<AbstractRoute>> routesByVrf = new TreeMap<>();
          outputRoutes.put(hostname, routesByVrf);
          node._virtualRouters.forEach(
              (vrName, vr) -> {
                SortedSet<AbstractRoute> routes = new TreeSet<>();
                routes.addAll(vr._mainRib.getRoutes());
                for (AbstractRoute route : routes) {
                  route.setNode(hostname);
                  route.setVrf(vrName);
                  Ip nextHopIp = route.getNextHopIp();
                  if (route.getProtocol() == RoutingProtocol.CONNECTED
                      || (route.getProtocol() == RoutingProtocol.STATIC
                          && nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP))
                      || Interface.NULL_INTERFACE_NAME.equals(route.getNextHopInterface())) {
                    route.setNextHop(Configuration.NODE_NONE_NAME);
                  }
                  if (!nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
                    String nextHop = ipOwners.get(nextHopIp);
                    if (nextHop != null) {
                      route.setNextHop(nextHop);
                    }
                  }
                }
                routesByVrf.put(vrName, routes);
              });
        });
    return outputRoutes;
  }

  private RouteSet computeOutputRoutes(Map<String, Node> nodes, Map<Ip, String> ipOwners) {
    RouteSet outputRoutes = new RouteSet();
    nodes.forEach(
        (hostname, node) -> {
          node._virtualRouters.forEach(
              (vrName, vr) -> {
                for (AbstractRoute route : vr._mainRib.getRoutes()) {
                  RouteBuilder rb = new RouteBuilder();
                  rb.setNode(hostname);
                  rb.setNetwork(route.getNetwork());
                  Ip nextHopIp = route.getNextHopIp();
                  if (route.getProtocol() == RoutingProtocol.CONNECTED
                      || (route.getProtocol() == RoutingProtocol.STATIC
                          && nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP))
                      || Interface.NULL_INTERFACE_NAME.equals(route.getNextHopInterface())) {
                    rb.setNextHop(Configuration.NODE_NONE_NAME);
                  }
                  if (!nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
                    rb.setNextHopIp(nextHopIp);
                    String nextHop = ipOwners.get(nextHopIp);
                    if (nextHop != null) {
                      rb.setNextHop(nextHop);
                    }
                  }
                  rb.setNextHopInterface(route.getNextHopInterface());
                  rb.setAdministrativeCost(route.getAdministrativeCost());
                  rb.setCost(route.getMetric());
                  rb.setProtocol(route.getProtocol());
                  rb.setTag(route.getTag());
                  rb.setVrf(vrName);
                  Route outputRoute = rb.build();
                  outputRoutes.add(outputRoute);
                }
              });
        });
    return outputRoutes;
  }

  @Override
  protected void dataPlanePluginInitialize() {
    _settings = (BdpSettings) _batfish.getDataPlanePluginSettings();
    _maxRecordedIterations = _settings.getBdpMaxRecordedIterations();
  }

  private String debugAbstractRoutesIterations(
      String msg,
      Map<Integer, SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>>>
          iterationAbsRoutes,
      int first,
      int last) {
    StringBuilder sb = new StringBuilder();
    sb.append(msg);
    sb.append("\n");
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> initialRoutes;
    initialRoutes = iterationAbsRoutes.get(first);
    sb.append("Initial routes (iteration " + first + "):\n");
    initialRoutes.forEach(
        (hostname, routesByVrf) -> {
          routesByVrf.forEach(
              (vrfName, routes) -> {
                for (AbstractRoute route : routes) {
                  sb.append(String.format("%s\n", route.fullString()));
                }
              });
        });
    for (int i = first + 1; i <= last; i++) {
      SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> baseRoutesByHostname =
          iterationAbsRoutes.get(i - 1);
      SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> deltaRoutesByHostname =
          iterationAbsRoutes.get(i);
      SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByHostname =
          new TreeMap<>();
      sb.append("Changed routes (iteration " + (i - 1) + " ==> " + i + "):\n");
      Set<String> hosts = new LinkedHashSet<>();
      hosts.addAll(baseRoutesByHostname.keySet());
      hosts.addAll(deltaRoutesByHostname.keySet());
      for (String hostname : hosts) {
        SortedMap<String, SortedSet<AbstractRoute>> routesByVrf = new TreeMap<>();
        routesByHostname.put(hostname, routesByVrf);
        SortedMap<String, SortedSet<AbstractRoute>> baseRoutesByVrf =
            baseRoutesByHostname.get(hostname);
        SortedMap<String, SortedSet<AbstractRoute>> deltaRoutesByVrf =
            deltaRoutesByHostname.get(hostname);
        if (baseRoutesByVrf == null) {
          for (Entry<String, SortedSet<AbstractRoute>> e : deltaRoutesByVrf.entrySet()) {
            String vrfName = e.getKey();
            SortedSet<AbstractRoute> deltaRoutes = e.getValue();
            SortedSet<AbstractRoute> routes = new TreeSet<>();
            routesByVrf.put(vrfName, routes);
            for (AbstractRoute deltaRoute : deltaRoutes) {
              sb.append(String.format("+ %s\n", deltaRoute.fullString()));
            }
          }
        } else if (deltaRoutesByVrf == null) {
          for (Entry<String, SortedSet<AbstractRoute>> e : baseRoutesByVrf.entrySet()) {
            String vrfName = e.getKey();
            SortedSet<AbstractRoute> baseRoutes = e.getValue();
            SortedSet<AbstractRoute> routes = new TreeSet<>();
            routesByVrf.put(vrfName, routes);
            for (AbstractRoute baseRoute : baseRoutes) {
              sb.append(String.format("- %s\n", baseRoute.fullString()));
            }
          }
        } else {
          Set<String> vrfNames = new LinkedHashSet<>();
          vrfNames.addAll(baseRoutesByVrf.keySet());
          vrfNames.addAll(deltaRoutesByVrf.keySet());
          for (String vrfName : vrfNames) {
            SortedSet<AbstractRoute> baseRoutes = baseRoutesByVrf.get(vrfName);
            SortedSet<AbstractRoute> deltaRoutes = deltaRoutesByVrf.get(vrfName);
            if (baseRoutes == null) {
              for (AbstractRoute deltaRoute : deltaRoutes) {
                sb.append(String.format("+ %s\n", deltaRoute.fullString()));
              }
            } else if (deltaRoutes == null) {
              for (AbstractRoute baseRoute : baseRoutes) {
                sb.append(String.format("- %s\n", baseRoute.fullString()));
              }
            } else {
              SortedSet<AbstractRoute> prunedBaseRoutes =
                  CommonUtil.difference(baseRoutes, deltaRoutes, TreeSet::new);
              SortedSet<AbstractRoute> prunedDeltaRoutes =
                  CommonUtil.difference(deltaRoutes, baseRoutes, TreeSet::new);
              for (AbstractRoute baseRoute : prunedBaseRoutes) {
                sb.append(String.format("- %s\n", baseRoute.fullString()));
              }
              for (AbstractRoute deltaRoute : prunedDeltaRoutes) {
                sb.append(String.format("+ %s\n", deltaRoute.fullString()));
              }
            }
          }
        }
      }
    }
    return sb.toString();
  }

  private String debugIterations(
      String msg, Map<Integer, RouteSet> iterationRoutes, int first, int last) {
    StringBuilder sb = new StringBuilder();
    sb.append(msg);
    sb.append("\n");
    RouteSet initialRoutes = iterationRoutes.get(first);
    sb.append("Initial routes (iteration " + first + "):\n");
    for (Route route : initialRoutes) {
      String routeStr = route.prettyPrint(null);
      sb.append(routeStr);
    }
    for (int i = first + 1; i <= last; i++) {
      RouteSet baseRoutes = iterationRoutes.get(i - 1);
      RouteSet deltaRoutes = iterationRoutes.get(i);
      RouteSet added = CommonUtil.difference(deltaRoutes, baseRoutes, RouteSet::new);
      RouteSet removed = CommonUtil.difference(baseRoutes, deltaRoutes, RouteSet::new);
      RouteSet changed = CommonUtil.union(added, removed, RouteSet::new);
      sb.append("Changed routes (iteration " + (i - 1) + " ==> " + i + "):\n");
      for (Route route : changed) {
        String diffSymbol = added.contains(route) ? "+" : "-";
        String routeStr = route.prettyPrint(diffSymbol);
        sb.append(routeStr);
      }
    }
    String errorMessage = sb.toString();
    return errorMessage;
  }

  private boolean flowTraceDeniedHelper(
      Set<FlowTrace> flowTraces,
      Flow originalFlow,
      Flow transformedFlow,
      List<FlowTraceHop> newHops,
      IpAccessList filter,
      FlowDisposition disposition) {
    boolean out =
        disposition == FlowDisposition.DENIED_OUT
            || disposition == FlowDisposition.NEIGHBOR_UNREACHABLE_OR_DENIED_OUT;
    FilterResult outResult = filter.filter(transformedFlow);
    boolean denied = outResult.getAction() == LineAction.REJECT;
    if (denied) {
      String outFilterName = filter.getName();
      Integer matchLine = outResult.getMatchLine();
      String lineDesc;
      if (matchLine != null) {
        lineDesc = filter.getLines().get(matchLine).getName();
        if (lineDesc == null) {
          lineDesc = "line:" + matchLine;
        }
      } else {
        lineDesc = "no-match";
      }
      String notes = disposition + "{" + outFilterName + "}{" + lineDesc + "}";
      if (out) {
        FlowTraceHop lastHop = newHops.get(newHops.size() - 1);
        newHops.remove(newHops.size() - 1);
        Edge lastEdge = lastHop.getEdge();
        Edge deniedOutEdge =
            new Edge(
                lastEdge.getFirst(),
                new NodeInterfacePair(Configuration.NODE_NONE_NAME, Interface.NULL_INTERFACE_NAME));
        FlowTraceHop deniedOutHop =
            new FlowTraceHop(
                deniedOutEdge, lastHop.getRoutes(), hopFlow(originalFlow, transformedFlow));
        newHops.add(deniedOutHop);
      }
      FlowTrace trace = new FlowTrace(disposition, newHops, notes);
      flowTraces.add(trace);
    }
    return denied;
  }

  @Override
  public AdvertisementSet getAdvertisements() {
    AdvertisementSet adverts = new AdvertisementSet();
    BdpDataPlane dp = loadDataPlane();
    for (Node node : dp._nodes.values()) {
      for (VirtualRouter vrf : node._virtualRouters.values()) {
        adverts.addAll(vrf._receivedBgpAdvertisements);
        adverts.addAll(vrf._sentBgpAdvertisements);
      }
    }
    return adverts;
  }

  @Override
  public List<Flow> getHistoryFlows() {
    BdpDataPlane dp = loadDataPlane();
    List<Flow> flowList = new ArrayList<>();
    _flowTraces
        .get(dp)
        .forEach(
            (flow, flowTraces) -> {
              for (int i = 0; i < flowTraces.size(); i++) {
                flowList.add(flow);
              }
            });
    return flowList;
  }

  @Override
  public List<FlowTrace> getHistoryFlowTraces() {
    BdpDataPlane dp = loadDataPlane();
    List<FlowTrace> flowTraceList = new ArrayList<>();
    _flowTraces
        .get(dp)
        .forEach(
            (flow, flowTraces) -> {
              for (FlowTrace flowTrace : flowTraces) {
                flowTraceList.add(flowTrace);
              }
            });
    return flowTraceList;
  }

  @Override
  public IbgpTopology getIbgpNeighbors() {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  @Override
  public SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes() {
    BdpDataPlane dp = loadDataPlane();
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
   * Recover from an oscillating data plane
   *
   * @param iterationHashCodes A mapping from iteration number to corresponding iteration hash
   * @param iterationRoutes A mapping from iteration number to router summaries
   * @param iterationAbstractRoutes A mapping from iteraiton number to detailed route information
   * @param start The first iteration in the oscillation
   * @param end The last iteration in the oscillation, which should be identical to the first
   * @param oscillatingPrefixes A set of prefixes to be populated with the set of all prefixes
   *     involved in the oscillation
   */
  private void handleOscillation(
      SortedMap<Integer, SortedMap<Integer, Integer>> recoveryIterationHashCodes,
      Map<Integer, RouteSet> iterationRoutes,
      Map<Integer, SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>>>
          iterationAbstractRoutes,
      int start,
      int end,
      SortedSet<Prefix> oscillatingPrefixes,
      int completedOscillationRecoveryAttempts) {
    if (completedOscillationRecoveryAttempts == _settings.getBdpMaxOscillationRecoveryAttempts()) {
      String msg =
          "Iteration "
              + end
              + " has same hash as iteration: "
              + start
              + "\n"
              + recoveryIterationHashCodes;
      if (!_settings.getBdpPrintOscillatingIterations() && !_settings.getBdpPrintAllIterations()) {
        throw new BdpOscillationException(msg);
      } else if (!_settings.getBdpPrintAllIterations()) {
        String errorMessage =
            _settings.getBdpDetail()
                ? debugAbstractRoutesIterations(msg, iterationAbstractRoutes, start, end)
                : debugIterations(msg, iterationRoutes, start, end);
        throw new BdpOscillationException(errorMessage);
      } else {
        String errorMessage =
            _settings.getBdpDetail()
                ? debugAbstractRoutesIterations(msg, iterationAbstractRoutes, 1, end)
                : debugIterations(msg, iterationRoutes, 1, end);
        throw new BdpOscillationException(errorMessage);
      }
    }
    /*
     * In order to record oscillating prefixes, we need routes from at least the first iteration of
     * the oscillation to the final iteration.
     */
    int minOscillationRecoveryIterations = end - start + 1;
    boolean enoughInfo =
        _settings.getBdpRecordAllIterations()
            || _maxRecordedIterations >= minOscillationRecoveryIterations;
    if (enoughInfo) {
      oscillatingPrefixes.addAll(
          collectOscillatingPrefixes(iterationRoutes, iterationAbstractRoutes, start, end));
    } else {
      _maxRecordedIterations = minOscillationRecoveryIterations;
    }
  }

  @Nullable
  private Flow hopFlow(Flow originalFlow, Flow transformedFlow) {
    if (originalFlow == transformedFlow) {
      return null;
    } else {
      return transformedFlow;
    }
  }

  private BdpDataPlane loadDataPlane() {
    return (BdpDataPlane) _batfish.loadDataPlane();
  }

  private FlowTrace neighborUnreachableTrace(
      List<FlowTraceHop> completedHops,
      NodeInterfacePair srcInterface,
      SortedSet<String> routes,
      Flow originalFlow,
      Flow transformedFlow) {
    Edge neighborUnreachbleEdge =
        new Edge(
            srcInterface,
            new NodeInterfacePair(Configuration.NODE_NONE_NAME, Interface.NULL_INTERFACE_NAME));
    FlowTraceHop neighborUnreachableHop =
        new FlowTraceHop(neighborUnreachbleEdge, routes, hopFlow(originalFlow, transformedFlow));
    List<FlowTraceHop> newHops = new ArrayList<>(completedHops);
    newHops.add(neighborUnreachableHop);
    FlowTrace trace =
        new FlowTrace(
            FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
            newHops,
            FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK.toString());
    return trace;
  }

  private boolean processCurrentNextHopInterfaceEdges(
      BdpDataPlane dp,
      String currentNodeName,
      Set<Edge> visitedEdges,
      List<FlowTraceHop> hopsSoFar,
      Set<FlowTrace> flowTraces,
      Flow originalFlow,
      Flow transformedFlow,
      Ip dstIp,
      Set<String> dstIpOwners,
      @Nullable String nextHopInterfaceName,
      SortedSet<String> routesForThisNextHopInterface,
      @Nullable Ip finalNextHopIp,
      @Nullable NodeInterfacePair nextHopInterface,
      EdgeSet edges,
      boolean arp) {
    boolean continueToNextNextHopInterface = false;
    int unreachableNeighbors = 0;
    int potentialNeighbors = 0;
    for (Edge edge : edges) {
      if (!edge.getNode1().equals(currentNodeName)) {
        continue;
      }
      potentialNeighbors++;
      List<FlowTraceHop> newHops = new ArrayList<>(hopsSoFar);
      Set<Edge> newVisitedEdges = new LinkedHashSet<>(visitedEdges);
      FlowTraceHop newHop =
          new FlowTraceHop(
              edge, routesForThisNextHopInterface, hopFlow(originalFlow, transformedFlow));
      newVisitedEdges.add(edge);
      newHops.add(newHop);
      /*
       * Check to see whether neighbor would refrain from sending ARP reply
       * (NEIGHBOR_UNREACHABLE)
       *
       * This occurs if:
       *
       * - Using interface-only route
       *
       * AND
       *
       * - Neighbor does not own arpIp
       *
       * AND EITHER
       *
       * -- Neighbor not using proxy-arp
       *
       * - OR
       *
       * -- Subnet of neighbor's receiving-interface contains arpIp
       */
      if (arp) {
        Ip arpIp;
        Set<String> arpIpOwners;
        if (finalNextHopIp == null) {
          arpIp = dstIp;
          arpIpOwners = dstIpOwners;
        } else {
          arpIp = finalNextHopIp;
          arpIpOwners = dp._ipOwners.get(arpIp);
        }
        // using interface-only route
        String node2 = edge.getNode2();
        if (arpIpOwners == null || !arpIpOwners.contains(node2)) {
          // neighbor does not own arpIp
          String int2Name = edge.getInt2();
          Interface int2 = dp._nodes.get(node2)._c.getInterfaces().get(int2Name);
          boolean neighborUnreachable = false;
          Boolean proxyArp = int2.getProxyArp();
          if (proxyArp == null || !proxyArp) {
            // TODO: proxyArp probably shouldn't be null
            neighborUnreachable = true;
          } else {
            for (Prefix prefix : int2.getAllPrefixes()) {
              if (prefix.getNetworkPrefix().contains(arpIp)) {
                neighborUnreachable = true;
                break;
              }
            }
          }
          if (neighborUnreachable) {
            unreachableNeighbors++;
            continue;
          }
        }
      }
      if (visitedEdges.contains(edge)) {
        FlowTrace trace =
            new FlowTrace(FlowDisposition.LOOP, newHops, FlowDisposition.LOOP.toString());
        flowTraces.add(trace);
        potentialNeighbors--;
        continue;
      }
      String nextNodeName = edge.getNode2();
      // now check output filter and input filter
      if (nextHopInterfaceName != null) {
        IpAccessList outFilter =
            dp._nodes
                .get(currentNodeName)
                ._c
                .getInterfaces()
                .get(nextHopInterfaceName)
                .getOutgoingFilter();
        if (outFilter != null) {
          FlowDisposition disposition = FlowDisposition.DENIED_OUT;
          boolean denied =
              flowTraceDeniedHelper(
                  flowTraces, originalFlow, transformedFlow, newHops, outFilter, disposition);
          if (denied) {
            potentialNeighbors--;
            continue;
          }
        }
      }
      IpAccessList inFilter =
          dp._nodes.get(nextNodeName)._c.getInterfaces().get(edge.getInt2()).getIncomingFilter();
      if (inFilter != null) {
        FlowDisposition disposition = FlowDisposition.DENIED_IN;
        boolean denied =
            flowTraceDeniedHelper(
                flowTraces, originalFlow, transformedFlow, newHops, inFilter, disposition);
        if (denied) {
          potentialNeighbors--;
          continue;
        }
      }
      // recurse
      collectFlowTraces(
          dp, nextNodeName, newVisitedEdges, newHops, flowTraces, originalFlow, transformedFlow);
    }
    if (arp && unreachableNeighbors > 0 && unreachableNeighbors == potentialNeighbors) {
      FlowTrace trace =
          neighborUnreachableTrace(
              hopsSoFar,
              nextHopInterface,
              routesForThisNextHopInterface,
              originalFlow,
              transformedFlow);
      flowTraces.add(trace);
      continueToNextNextHopInterface = true;
    }
    return continueToNextNextHopInterface;
  }

  @Override
  public void processFlows(Set<Flow> flows) {
    BdpDataPlane dp = loadDataPlane();
    Map<Flow, Set<FlowTrace>> flowTraces = new ConcurrentHashMap<>();
    flows
        .parallelStream()
        .forEach(
            flow -> {
              Set<FlowTrace> currentFlowTraces = new TreeSet<>();
              flowTraces.put(flow, currentFlowTraces);
              String ingressNodeName = flow.getIngressNode();
              if (ingressNodeName == null) {
                throw new BatfishException(
                    "Cannot construct flow trace since ingressNode is not specified");
              }
              Ip dstIp = flow.getDstIp();
              if (dstIp == null) {
                throw new BatfishException(
                    "Cannot construct flow trace since dstIp is not specified");
              }
              Set<Edge> visitedEdges = Collections.emptySet();
              List<FlowTraceHop> hops = new ArrayList<>();
              Set<String> dstIpOwners = dp._ipOwners.get(dstIp);
              EdgeSet edges = new EdgeSet();
              String ingressInterfaceName = flow.getIngressInterface();
              if (ingressInterfaceName != null) {
                edges.add(
                    new Edge(
                        TRACEROUTE_INGRESS_NODE_NAME,
                        TRACEROUTE_INGRESS_NODE_INTERFACE_NAME,
                        ingressNodeName,
                        ingressInterfaceName));
                processCurrentNextHopInterfaceEdges(
                    dp,
                    TRACEROUTE_INGRESS_NODE_NAME,
                    visitedEdges,
                    hops,
                    currentFlowTraces,
                    flow,
                    flow,
                    dstIp,
                    dstIpOwners,
                    null,
                    new TreeSet<>(),
                    null,
                    null,
                    edges,
                    false);
              } else {
                collectFlowTraces(
                    dp, ingressNodeName, visitedEdges, hops, currentFlowTraces, flow, flow);
              }
            });
    _flowTraces.put(dp, new TreeMap<>(flowTraces));
  }

  private void recordIterationDebugInfo(
      Map<String, Node> nodes,
      BdpDataPlane dp,
      Map<Integer, RouteSet> iterationRoutes,
      Map<Integer, SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>>>
          iterationAbstractRoutes,
      int dependentRoutesIterations) {
    if (_maxRecordedIterations > 0 || _settings.getBdpRecordAllIterations()) {
      Map<Ip, String> ipOwners = dp.getIpOwnersSimple();
      if (_settings.getBdpDetail()) {
        iterationAbstractRoutes.put(
            dependentRoutesIterations, computeOutputAbstractRoutes(nodes, ipOwners));
      } else {
        iterationRoutes.put(dependentRoutesIterations, computeOutputRoutes(nodes, ipOwners));
      }
    }
  }
}
