package org.batfish.dataplane;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.ITracerouteEngine;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.FlowTraceHop;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.collections.NodeInterfacePair;

public class TracerouteEngineImpl implements ITracerouteEngine {
  private static ITracerouteEngine _instance = new TracerouteEngineImpl();

  private static final String TRACEROUTE_INGRESS_NODE_INTERFACE_NAME =
      "traceroute_source_interface";

  private static final String TRACEROUTE_INGRESS_NODE_NAME = "traceroute_source_node";

  public static ITracerouteEngine getInstance() {
    return _instance;
  }

  private TracerouteEngineImpl() {}

  @Override
  public SortedMap<Flow, Set<FlowTrace>> processFlows(
      DataPlane dataPlane,
      Set<Flow> flows,
      Map<String, Map<String, Fib>> fibs,
      boolean ignoreAcls) {
    Map<Flow, Set<FlowTrace>> flowTraces = new ConcurrentHashMap<>();
    Map<String, Configuration> configurations = dataPlane.getConfigurations();
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
              SortedSet<Edge> edges = new TreeSet<>();
              String ingressInterfaceName = flow.getIngressInterface();
              if (ingressInterfaceName != null) {
                edges.add(
                    new Edge(
                        TRACEROUTE_INGRESS_NODE_NAME,
                        TRACEROUTE_INGRESS_NODE_INTERFACE_NAME,
                        ingressNodeName,
                        ingressInterfaceName));
                processCurrentNextHopInterfaceEdges(
                    dataPlane,
                    configurations,
                    fibs,
                    TRACEROUTE_INGRESS_NODE_NAME,
                    visitedEdges,
                    hops,
                    currentFlowTraces,
                    flow,
                    flow,
                    ingressInterfaceName,
                    configurations.get(ingressNodeName).getIpAccessLists(),
                    configurations.get(ingressNodeName).getIpSpaces(),
                    dstIp,
                    null,
                    new TreeSet<>(),
                    null,
                    null,
                    edges,
                    false,
                    ignoreAcls);
              } else {
                collectFlowTraces(
                    dataPlane,
                    configurations,
                    dataPlane.getFibs(),
                    ingressNodeName,
                    visitedEdges,
                    hops,
                    currentFlowTraces,
                    flow,
                    flow,
                    ignoreAcls);
              }
            });
    return new TreeMap<>(flowTraces);
  }

  private boolean processCurrentNextHopInterfaceEdges(
      DataPlane dp,
      Map<String, Configuration> configurations,
      Map<String, Map<String, Fib>> fibs,
      String currentNodeName,
      Set<Edge> visitedEdges,
      List<FlowTraceHop> hopsSoFar,
      Set<FlowTrace> flowTraces,
      Flow originalFlow,
      Flow transformedFlow,
      String srcInterface,
      Map<String, IpAccessList> aclDefinitions,
      Map<String, IpSpace> namedIpSpaces,
      Ip dstIp,
      @Nullable String nextHopInterfaceName,
      SortedSet<String> routesForThisNextHopInterface,
      @Nullable Ip finalNextHopIp,
      @Nullable NodeInterfacePair nextHopInterface,
      SortedSet<Edge> edges,
      boolean arp,
      boolean ignoreAcls) {
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
              edge,
              routesForThisNextHopInterface,
              null,
              null,
              hopFlow(originalFlow, transformedFlow));
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
        Ip arpIp = finalNextHopIp != null ? finalNextHopIp : dstIp;
        boolean neighborUnreachable =
            interfaceRepliesToArpRequestForIp(
                dp, configurations.get(edge.getNode2()).getInterfaces().get(edge.getInt2()), arpIp);
        if (neighborUnreachable) {
          unreachableNeighbors++;
          continue;
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
            configurations
                .get(currentNodeName)
                .getInterfaces()
                .get(nextHopInterfaceName)
                .getOutgoingFilter();
        if (!ignoreAcls && outFilter != null) {
          FlowDisposition disposition = FlowDisposition.DENIED_OUT;
          boolean denied =
              flowTraceFilterHelper(
                  flowTraces,
                  originalFlow,
                  transformedFlow,
                  srcInterface,
                  aclDefinitions,
                  namedIpSpaces,
                  newHops,
                  outFilter,
                  disposition);
          if (denied) {
            potentialNeighbors--;
            continue;
          }
        }
      }
      IpAccessList inFilter =
          configurations.get(nextNodeName).getInterfaces().get(edge.getInt2()).getIncomingFilter();
      if (!ignoreAcls && inFilter != null) {
        FlowDisposition disposition = FlowDisposition.DENIED_IN;
        boolean denied =
            flowTraceFilterHelper(
                flowTraces,
                originalFlow,
                transformedFlow,
                srcInterface,
                aclDefinitions,
                namedIpSpaces,
                newHops,
                inFilter,
                disposition);
        if (denied) {
          potentialNeighbors--;
          continue;
        }
      }
      // recurse
      collectFlowTraces(
          dp,
          configurations,
          fibs,
          nextNodeName,
          newVisitedEdges,
          newHops,
          flowTraces,
          originalFlow,
          transformedFlow,
          ignoreAcls);
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

  @VisibleForTesting
  static boolean interfaceRepliesToArpRequestForIp(DataPlane dp, Interface iface, Ip arpIp) {
    // TODO this is wrong. without proxy-arp, only reply to the interface's owned IPs.
    Set<String> arpIpOwners = dp.getIpOwners().get(arpIp);
    if (arpIpOwners != null && arpIpOwners.contains(iface.getOwner().getHostname())) {
      // neighbor owns arpIp
      return true;
    }


    // neighbor does not own arpIp. Request succeeds if proxy-arp is enabled and the destination
    // is not on the incoming edge.
    // TODO _AND_ the VRF has a route to arpIp!
    return iface.getProxyArp()
        && iface.getAllAddresses().stream().noneMatch(addr -> addr.getPrefix().containsIp(arpIp));
  }

  /**
   * Applies the given list of source NAT rules to the given flow and returns the new transformed
   * flow. If {@code sourceNats} is null, empty, or does not contain any ACL rules matching the
   * {@link Flow}, the original flow is returned.
   *
   * <p>Each {@link SourceNat} is expected to be valid: it must have a NAT IP or pool.
   */
  @VisibleForTesting
  static Flow applySourceNat(
      Flow flow,
      @Nullable String srcInterface,
      Map<String, IpAccessList> aclDefinitions,
      Map<String, IpSpace> namedIpSpaces,
      @Nullable List<SourceNat> sourceNats) {
    if (CommonUtil.isNullOrEmpty(sourceNats)) {
      return flow;
    }
    Optional<SourceNat> matchingSourceNat =
        sourceNats
            .stream()
            .filter(
                sourceNat ->
                    sourceNat.getAcl() != null
                        && sourceNat
                                .getAcl()
                                .filter(flow, srcInterface, aclDefinitions, namedIpSpaces)
                                .getAction()
                            != LineAction.REJECT)
            .findFirst();
    if (!matchingSourceNat.isPresent()) {
      // No NAT rule matched.
      return flow;
    }
    SourceNat sourceNat = matchingSourceNat.get();
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

  private void collectFlowTraces(
      DataPlane dp,
      Map<String, Configuration> configurations,
      Map<String, Map<String, Fib>> fibs,
      String currentNodeName,
      Set<Edge> visitedEdges,
      List<FlowTraceHop> hopsSoFar,
      Set<FlowTrace> flowTraces,
      Flow originalFlow,
      Flow transformedFlow,
      boolean ignoreAcls) {
    Ip dstIp = transformedFlow.getDstIp();
    Set<String> dstIpOwners = dp.getIpOwners().get(dstIp);
    Configuration currentConfiguration = configurations.get(currentNodeName);
    // TODO: ipVrfOwners
    if (dstIpOwners != null && dstIpOwners.contains(currentNodeName)) {
      FlowTrace trace =
          new FlowTrace(FlowDisposition.ACCEPTED, hopsSoFar, FlowDisposition.ACCEPTED.toString());
      flowTraces.add(trace);
    } else {
      Map<String, IpAccessList> aclDefinitions = currentConfiguration.getIpAccessLists();
      NavigableMap<String, IpSpace> namedIpSpaces = currentConfiguration.getIpSpaces();
      String vrfName;
      String srcInterface;
      if (hopsSoFar.isEmpty()) {
        vrfName = transformedFlow.getIngressVrf();
        srcInterface = null;
      } else {
        FlowTraceHop lastHop = hopsSoFar.get(hopsSoFar.size() - 1);
        srcInterface = lastHop.getEdge().getInt2();
        vrfName = currentConfiguration.getInterfaces().get(srcInterface).getVrf().getName();
      }
      Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfacesByRoute =
          fibs.get(currentNodeName).get(vrfName).getNextHopInterfacesByRoute(dstIp);
      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfacesWithRoutes =
          fibs.get(currentNodeName).get(vrfName).getNextHopInterfaces(dstIp);
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
                    newEdge,
                    routesForThisNextHopInterface,
                    null,
                    null,
                    hopFlow(originalFlow, transformedFlow));
            newHops.add(newHop);
            FlowTrace nullRouteTrace =
                new FlowTrace(
                    FlowDisposition.NULL_ROUTED, newHops, FlowDisposition.NULL_ROUTED.toString());
            flowTraces.add(nullRouteTrace);
          } else {
            Interface outgoingInterface =
                configurations
                    .get(nextHopInterface.getHostname())
                    .getInterfaces()
                    .get(nextHopInterface.getInterface());

            // Apply any relevant source NAT rules.
            transformedFlow =
                applySourceNat(
                    transformedFlow,
                    srcInterface,
                    aclDefinitions,
                    namedIpSpaces,
                    outgoingInterface.getSourceNats());

            SortedSet<Edge> edges = dp.getTopology().getInterfaceEdges().get(nextHopInterface);
            if (edges != null) {
              boolean continueToNextNextHopInterface =
                  processCurrentNextHopInterfaceEdges(
                      dp,
                      configurations,
                      fibs,
                      currentNodeName,
                      visitedEdges,
                      hopsSoFar,
                      flowTraces,
                      originalFlow,
                      transformedFlow,
                      srcInterface,
                      aclDefinitions,
                      namedIpSpaces,
                      dstIp,
                      nextHopInterfaceName,
                      routesForThisNextHopInterface,
                      finalNextHopIp,
                      nextHopInterface,
                      edges,
                      true,
                      ignoreAcls);
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
                      null,
                      null,
                      hopFlow(originalFlow, transformedFlow));
              List<FlowTraceHop> newHops = new ArrayList<>(hopsSoFar);
              newHops.add(neighborUnreachableHop);
              /** Check if denied out. If not, make standard neighbor-unreachable trace. */
              IpAccessList outFilter = outgoingInterface.getOutgoingFilter();
              boolean denied = false;
              if (!ignoreAcls && outFilter != null) {
                FlowDisposition disposition = FlowDisposition.DENIED_OUT;
                denied =
                    flowTraceFilterHelper(
                        flowTraces,
                        originalFlow,
                        transformedFlow,
                        srcInterface,
                        aclDefinitions,
                        namedIpSpaces,
                        newHops,
                        outFilter,
                        disposition);
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

  private boolean flowTraceFilterHelper(
      Set<FlowTrace> flowTraces,
      Flow originalFlow,
      Flow transformedFlow,
      String srcInterface,
      Map<String, IpAccessList> aclDefinitions,
      Map<String, IpSpace> namedIpSpaces,
      List<FlowTraceHop> newHops,
      IpAccessList filter,
      FlowDisposition disposition) {
    boolean out = disposition == FlowDisposition.DENIED_OUT;
    FilterResult outResult =
        filter.filter(transformedFlow, srcInterface, aclDefinitions, namedIpSpaces);
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
    boolean denied = outResult.getAction() == LineAction.REJECT;
    if (denied) {
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
                deniedOutEdge,
                lastHop.getRoutes(),
                null,
                null,
                hopFlow(originalFlow, transformedFlow));
        newHops.add(deniedOutHop);
      }
      FlowTrace trace = new FlowTrace(disposition, newHops, notes);
      flowTraces.add(trace);
    } else {
      FlowTraceHop hop = newHops.get(newHops.size() - 1);
      String filterNotes = "{" + outFilterName + "}{" + lineDesc + "}";
      if (out) {
        hop.setFilterOut(filterNotes);
      } else {
        hop.setFilterIn(filterNotes);
      }
    }
    return denied;
  }

  @Nullable
  private Flow hopFlow(Flow originalFlow, Flow transformedFlow) {
    if (originalFlow == transformedFlow) {
      return null;
    } else {
      return transformedFlow;
    }
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
        new FlowTraceHop(
            neighborUnreachbleEdge, routes, null, null, hopFlow(originalFlow, transformedFlow));
    List<FlowTraceHop> newHops = new ArrayList<>(completedHops);
    newHops.add(neighborUnreachableHop);
    FlowTrace trace =
        new FlowTrace(
            FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
            newHops,
            FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK.toString());
    return trace;
  }
}
