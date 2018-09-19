package org.batfish.dataplane;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
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
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
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
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.collections.NodeInterfacePair;

@ParametersAreNonnullByDefault
class TracerouteEngineImplContext {
  private static class TransmissionContext {

    private final Map<String, IpAccessList> _aclDefinitions;
    private final String _currentNodeName;
    private String _filterOutNotes;
    private final Set<FlowTrace> _flowTraces;
    private final List<FlowTraceHop> _hopsSoFar;
    private final NavigableMap<String, IpSpace> _namedIpSpaces;
    private final Flow _originalFlow;
    private final SortedSet<String> _routesForThisNextHopInterface;
    private final Flow _transformedFlow;

    private TransmissionContext(
        Map<String, IpAccessList> aclDefinitions,
        String currentNodeName,
        Set<FlowTrace> flowTraces,
        List<FlowTraceHop> hopsSoFar,
        NavigableMap<String, IpSpace> namedIpSpaces,
        Flow originalFlow,
        SortedSet<String> routesForThisNextHopInterface,
        Flow transformedFlow) {
      _aclDefinitions = aclDefinitions;
      _currentNodeName = currentNodeName;
      _flowTraces = flowTraces;
      _hopsSoFar = new ArrayList<>(hopsSoFar);
      _namedIpSpaces = namedIpSpaces;
      _originalFlow = originalFlow;
      _routesForThisNextHopInterface = routesForThisNextHopInterface;
      _transformedFlow = transformedFlow;
    }

    private TransmissionContext branch() {
      TransmissionContext transmissionContext =
          new TransmissionContext(
              _aclDefinitions,
              _currentNodeName,
              _flowTraces,
              _hopsSoFar,
              _namedIpSpaces,
              _originalFlow,
              _routesForThisNextHopInterface,
              _transformedFlow);
      transmissionContext._filterOutNotes = _filterOutNotes;
      return transmissionContext;
    }
  }

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
                            != LineAction.DENY)
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

  @VisibleForTesting
  static boolean interfaceRepliesToArpRequestForIp(Interface iface, Fib ifaceFib, Ip arpIp) {
    // interfaces without addresses never reply
    if (iface.getAllAddresses().isEmpty()) {
      return false;
    }
    // the interface that owns the arpIp always replies
    if (iface.getAllAddresses().stream().anyMatch(addr -> addr.getIp().equals(arpIp))) {
      return true;
    }

    /*
     * iface does not own arpIp, so it replies if and only if:
     * 1. proxy-arp is enabled
     * 2. the interface's vrf has a route to the destination
     * 3. the destination is not on the incoming edge.
     */
    Set<String> nextHopInterfaces = ifaceFib.getNextHopInterfaces(arpIp);
    return iface.getProxyArp()
        && !nextHopInterfaces.isEmpty()
        && nextHopInterfaces.stream().noneMatch(iface.getName()::equals);
  }

  private final Map<String, Configuration> _configurations;
  private final DataPlane _dataPlane;
  private final Map<String, Map<String, Fib>> _fibs;
  private final Set<Flow> _flows;
  private final ForwardingAnalysis _forwardingAnalysis;
  private final boolean _ignoreAcls;

  TracerouteEngineImplContext(
      DataPlane dataPlane,
      Set<Flow> flows,
      Map<String, Map<String, Fib>> fibs,
      boolean ignoreAcls) {
    _configurations = dataPlane.getConfigurations();
    _dataPlane = dataPlane;
    _flows = flows;
    _fibs = fibs;
    _ignoreAcls = ignoreAcls;
    _forwardingAnalysis = _dataPlane.getForwardingAnalysis();
  }

  private void collectFlowTraces(
      String currentNodeName,
      String currentVrfName,
      Set<Edge> visitedEdges,
      List<FlowTraceHop> hopsSoFar,
      Set<FlowTrace> flowTraces,
      Flow originalFlow,
      Flow transformedFlow) {
    Ip dstIp = transformedFlow.getDstIp();
    Configuration currentConfiguration = _configurations.get(currentNodeName);
    if (currentConfiguration == null) {
      throw new BatfishException(
          String.format(
              "Node %s is not in the network, cannot perform traceroute", currentNodeName));
    }

    // If the destination IP is owned by this VRF on this node, trace is ACCEPTED here.
    if (_dataPlane
        .getIpVrfOwners()
        .getOrDefault(dstIp, ImmutableMap.of())
        .getOrDefault(currentNodeName, ImmutableSet.of())
        .contains(currentVrfName)) {
      FlowTrace trace =
          new FlowTrace(FlowDisposition.ACCEPTED, hopsSoFar, FlowDisposition.ACCEPTED.toString());
      flowTraces.add(trace);
      return;
    }

    // Figure out where the trace came from..
    String vrfName;
    String srcInterface;
    if (hopsSoFar.isEmpty()) {
      vrfName = transformedFlow.getIngressVrf();
      srcInterface = null;
    } else {
      FlowTraceHop lastHop = hopsSoFar.get(hopsSoFar.size() - 1);
      srcInterface = lastHop.getEdge().getInt2();
      vrfName = currentConfiguration.getAllInterfaces().get(srcInterface).getVrf().getName();
    }
    // .. and what the next hops are based on the FIB.
    Fib currentFib = _fibs.get(currentNodeName).get(vrfName);
    Set<String> nextHopInterfaces = currentFib.getNextHopInterfaces(dstIp);
    if (nextHopInterfaces.isEmpty()) {
      // No interface can forward traffic for this dstIp.
      FlowTrace trace =
          new FlowTrace(FlowDisposition.NO_ROUTE, hopsSoFar, FlowDisposition.NO_ROUTE.toString());
      flowTraces.add(trace);
      return;
    }

    Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfacesByRoute =
        currentFib.getNextHopInterfacesByRoute(dstIp);
    Map<String, IpAccessList> aclDefinitions = currentConfiguration.getIpAccessLists();
    NavigableMap<String, IpSpace> namedIpSpaces = currentConfiguration.getIpSpaces();

    // For every interface with a route to the dst IP
    for (String nextHopInterfaceName : nextHopInterfaces) {
      TreeMultimap<Ip, AbstractRoute> resolvedNextHopWithRoutes = TreeMultimap.create();

      // Loop over all matching routes that use nextHopInterfaceName as one of the next hop
      // interfaces.
      for (Entry<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> e :
          nextHopInterfacesByRoute.entrySet()) {
        Map<Ip, Set<AbstractRoute>> finalNextHops = e.getValue().get(nextHopInterfaceName);
        if (finalNextHops == null || finalNextHops.isEmpty()) {
          continue;
        }

        AbstractRoute routeCandidate = e.getKey();
        Ip nextHopIp = routeCandidate.getNextHopIp();
        if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
          resolvedNextHopWithRoutes.put(nextHopIp, routeCandidate);
        } else {
          for (Ip resolvedNextHopIp : finalNextHops.keySet()) {
            resolvedNextHopWithRoutes.put(resolvedNextHopIp, routeCandidate);
          }
        }
      }

      NodeInterfacePair nextHopInterface =
          new NodeInterfacePair(currentNodeName, nextHopInterfaceName);
      resolvedNextHopWithRoutes
          .asMap()
          .forEach(
              (resolvedNextHopIp, routeCandidates) -> {
                // Later parts of the stack expect null instead of unset to trigger proxy arp, etc.
                Ip finalNextHopIp =
                    Route.UNSET_ROUTE_NEXT_HOP_IP.equals(resolvedNextHopIp)
                        ? null
                        : resolvedNextHopIp;
                SortedSet<String> routesForThisNextHopInterface =
                    routeCandidates
                        .stream()
                        .map(rc -> rc + "_fnhip:" + finalNextHopIp)
                        .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));

                List<FlowTraceHop> newHops = new ArrayList<>(hopsSoFar);
                if (nextHopInterfaceName.equals(Interface.NULL_INTERFACE_NAME)) {
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
                          FlowDisposition.NULL_ROUTED,
                          newHops,
                          FlowDisposition.NULL_ROUTED.toString());
                  flowTraces.add(nullRouteTrace);
                  return;
                }
                Interface outgoingInterface =
                    _configurations
                        .get(nextHopInterface.getHostname())
                        .getAllInterfaces()
                        .get(nextHopInterface.getInterface());

                // Apply any relevant source NAT rules.
                Flow newTransformedFlow =
                    applySourceNat(
                        transformedFlow,
                        srcInterface,
                        aclDefinitions,
                        namedIpSpaces,
                        outgoingInterface.getSourceNats());

                SortedSet<Edge> edges =
                    _dataPlane.getTopology().getInterfaceEdges().get(nextHopInterface);
                TransmissionContext transmissionContext =
                    new TransmissionContext(
                        aclDefinitions,
                        currentNodeName,
                        flowTraces,
                        newHops,
                        namedIpSpaces,
                        originalFlow,
                        routesForThisNextHopInterface,
                        newTransformedFlow);
                if (edges == null || edges.isEmpty()) {
                  /*
                   * Interface has no edges
                   */
                  /* Check if denied out. If not, make standard neighbor-unreachable trace. */
                  IpAccessList outFilter = outgoingInterface.getOutgoingFilter();
                  boolean denied = false;
                  if (!_ignoreAcls && outFilter != null) {
                    denied =
                        flowTraceFilterHelper(
                            srcInterface,
                            outFilter,
                            FlowDisposition.DENIED_OUT,
                            nextHopInterface,
                            transmissionContext);
                  }
                  if (!denied) {
                    Edge neighborUnreachableEdge =
                        new Edge(
                            nextHopInterface,
                            new NodeInterfacePair(
                                Configuration.NODE_NONE_NAME, Interface.NULL_INTERFACE_NAME));
                    FlowTraceHop neighborUnreachableHop =
                        new FlowTraceHop(
                            neighborUnreachableEdge,
                            routesForThisNextHopInterface,
                            null,
                            null,
                            hopFlow(originalFlow, newTransformedFlow));
                    neighborUnreachableHop.setFilterOut(transmissionContext._filterOutNotes);
                    newHops.add(neighborUnreachableHop);
                    FlowTrace trace =
                        new FlowTrace(
                            FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
                            newHops,
                            FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK.toString());
                    flowTraces.add(trace);
                  }
                } else {
                  processCurrentNextHopInterfaceEdges(
                      visitedEdges,
                      srcInterface,
                      dstIp,
                      nextHopInterfaceName,
                      finalNextHopIp,
                      nextHopInterface,
                      edges,
                      transmissionContext);
                }
              });
    }
  }

  private static boolean flowTraceFilterHelper(
      @Nullable String srcInterface,
      IpAccessList filter,
      FlowDisposition disposition,
      @Nullable NodeInterfacePair outInterface,
      TransmissionContext transmissionContext) {
    boolean out = disposition == FlowDisposition.DENIED_OUT;
    FilterResult filterResult =
        filter.filter(
            transmissionContext._transformedFlow,
            srcInterface,
            transmissionContext._aclDefinitions,
            transmissionContext._namedIpSpaces);
    Integer matchLine = filterResult.getMatchLine();
    String lineDesc;
    if (matchLine != null) {
      lineDesc = filter.getLines().get(matchLine).getName();
      if (lineDesc == null) {
        lineDesc = "line:" + matchLine;
      }
    } else {
      lineDesc = "no-match";
    }
    String filterNotes = "{" + filter.getName() + "}{" + lineDesc + "}";

    if (out) {
      transmissionContext._filterOutNotes = filterNotes;
    } else {
      FlowTraceHop hop =
          transmissionContext._hopsSoFar.get(transmissionContext._hopsSoFar.size() - 1);
      hop.setFilterIn(filterNotes);
    }

    boolean denied = filterResult.getAction() == LineAction.DENY;
    if (denied) {
      if (out) {
        Edge deniedOutEdge =
            new Edge(
                outInterface,
                new NodeInterfacePair(Configuration.NODE_NONE_NAME, Interface.NULL_INTERFACE_NAME));
        FlowTraceHop deniedOutHop =
            new FlowTraceHop(
                deniedOutEdge,
                transmissionContext._routesForThisNextHopInterface,
                null,
                null,
                hopFlow(transmissionContext._originalFlow, transmissionContext._transformedFlow));
        transmissionContext._hopsSoFar.add(deniedOutHop);
      }
      String notes = disposition + filterNotes;
      FlowTrace trace = new FlowTrace(disposition, transmissionContext._hopsSoFar, notes);
      transmissionContext._flowTraces.add(trace);
    }
    return denied;
  }

  @Nullable
  private static Flow hopFlow(@Nullable Flow originalFlow, @Nullable Flow transformedFlow) {
    if (originalFlow == transformedFlow) {
      return null;
    } else {
      return transformedFlow;
    }
  }

  private static FlowTrace neighborUnreachableTrace(
      NodeInterfacePair srcInterface, TransmissionContext transmissionContext) {
    Edge neighborUnreachableEdge =
        new Edge(
            srcInterface,
            new NodeInterfacePair(Configuration.NODE_NONE_NAME, Interface.NULL_INTERFACE_NAME));
    FlowTraceHop neighborUnreachableHop =
        new FlowTraceHop(
            neighborUnreachableEdge,
            transmissionContext._routesForThisNextHopInterface,
            null,
            null,
            hopFlow(transmissionContext._originalFlow, transmissionContext._transformedFlow));
    neighborUnreachableHop.setFilterOut(transmissionContext._filterOutNotes);
    List<FlowTraceHop> newHops =
        ImmutableList.<FlowTraceHop>builder()
            .addAll(transmissionContext._hopsSoFar)
            .add(neighborUnreachableHop)
            .build();
    return new FlowTrace(
        FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
        newHops,
        FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK.toString());
  }

  private void processCurrentNextHopInterfaceEdges(
      Set<Edge> visitedEdges,
      @Nullable String srcInterface,
      Ip dstIp,
      String nextHopInterfaceName,
      @Nullable Ip finalNextHopIp,
      NodeInterfacePair nextHopInterface,
      SortedSet<Edge> edges,
      TransmissionContext transmissionContext) {
    if (!processFlowTransmission(
        srcInterface,
        dstIp,
        nextHopInterfaceName,
        finalNextHopIp,
        nextHopInterface,
        transmissionContext)) {
      return;
    }
    for (Edge edge : edges) {
      if (!edge.getNode1().equals(transmissionContext._currentNodeName)) {
        continue;
      }
      processFlowReception(
          edge,
          transmissionContext,
          ImmutableSet.copyOf(visitedEdges),
          finalNextHopIp != null ? finalNextHopIp : dstIp);
    }
  }

  private void processFlowReception(
      Edge edge,
      TransmissionContext oldTransmissionContext,
      Set<Edge> visitedEdges,
      @Nullable Ip arpIp) {
    // do nothing if this neighbor would not have replied to ARP (excluding injection case)
    if (arpIp != null
        && !_forwardingAnalysis
            .getArpReplies()
            .get(edge.getNode2())
            .get(edge.getInt2())
            .containsIp(arpIp, _configurations.get(edge.getNode2()).getIpSpaces())) {
      return;
    }

    // branch because other edges share the same oldTransmissionContext
    TransmissionContext transmissionContext = oldTransmissionContext.branch();
    Set<Edge> newVisitedEdges = new LinkedHashSet<>(visitedEdges);
    FlowTraceHop newHop =
        new FlowTraceHop(
            edge,
            transmissionContext._routesForThisNextHopInterface,
            null,
            null,
            hopFlow(transmissionContext._originalFlow, transmissionContext._transformedFlow));
    newHop.setFilterOut(transmissionContext._filterOutNotes);
    newVisitedEdges.add(edge);
    transmissionContext._hopsSoFar.add(newHop);
    if (visitedEdges.contains(edge)) {
      FlowTrace trace =
          new FlowTrace(
              FlowDisposition.LOOP,
              transmissionContext._hopsSoFar,
              FlowDisposition.LOOP.toString());
      transmissionContext._flowTraces.add(trace);
      return;
    }
    String nextNodeName = edge.getNode2();
    // check input filter
    Interface nextInterface =
        _configurations.get(nextNodeName).getAllInterfaces().get(edge.getInt2());
    IpAccessList inFilter = nextInterface.getIncomingFilter();
    if (!_ignoreAcls && inFilter != null) {
      FlowDisposition disposition = FlowDisposition.DENIED_IN;
      boolean denied =
          flowTraceFilterHelper(null, inFilter, disposition, null, transmissionContext);
      if (denied) {
        return;
      }
    }
    // recurse
    collectFlowTraces(
        nextNodeName,
        nextInterface.getVrfName(),
        newVisitedEdges,
        transmissionContext._hopsSoFar,
        transmissionContext._flowTraces,
        transmissionContext._originalFlow,
        transmissionContext._transformedFlow);
  }

  SortedMap<Flow, Set<FlowTrace>> processFlows() {
    Map<Flow, Set<FlowTrace>> flowTraces = new ConcurrentHashMap<>();
    _flows
        .parallelStream()
        .forEach(
            flow -> {
              Set<FlowTrace> currentFlowTraces =
                  flowTraces.computeIfAbsent(flow, f -> new TreeSet<>());
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
              String ingressInterfaceName = flow.getIngressInterface();
              if (ingressInterfaceName != null) {
                Edge edge =
                    new Edge(
                        TRACEROUTE_INGRESS_NODE_NAME,
                        TRACEROUTE_INGRESS_NODE_INTERFACE_NAME,
                        ingressNodeName,
                        ingressInterfaceName);
                TransmissionContext transmissionContext =
                    new TransmissionContext(
                        _configurations.get(ingressNodeName).getIpAccessLists(),
                        ingressNodeName,
                        currentFlowTraces,
                        hops,
                        _configurations.get(ingressNodeName).getIpSpaces(),
                        flow,
                        new TreeSet<>(),
                        flow);
                processFlowReception(edge, transmissionContext, ImmutableSet.of(), null);
              } else {
                collectFlowTraces(
                    ingressNodeName,
                    firstNonNull(flow.getIngressVrf(), Configuration.DEFAULT_VRF_NAME),
                    visitedEdges,
                    hops,
                    currentFlowTraces,
                    flow,
                    flow);
              }
            });
    return new TreeMap<>(flowTraces);
  }

  private boolean processFlowTransmission(
      @Nullable String srcInterface,
      Ip dstIp,
      String nextHopInterfaceName,
      @Nullable Ip finalNextHopIp,
      NodeInterfacePair nextHopInterface,
      TransmissionContext transmissionContext) {
    // check output filter
    IpAccessList outFilter =
        _configurations
            .get(transmissionContext._currentNodeName)
            .getAllInterfaces()
            .get(nextHopInterfaceName)
            .getOutgoingFilter();
    if (!_ignoreAcls && outFilter != null) {
      boolean denied =
          flowTraceFilterHelper(
              srcInterface,
              outFilter,
              FlowDisposition.DENIED_OUT,
              new NodeInterfacePair(transmissionContext._currentNodeName, nextHopInterfaceName),
              transmissionContext);
      if (denied) {
        return false;
      }
    }
    Ip arpIp = finalNextHopIp != null ? finalNextHopIp : dstIp;
    Configuration c = _configurations.get(transmissionContext._currentNodeName);
    // halt processing and add neighbor-unreachable trace if no one would respond
    if (_forwardingAnalysis
        .getNeighborUnreachable()
        .get(transmissionContext._currentNodeName)
        .get(c.getAllInterfaces().get(nextHopInterfaceName).getVrfName())
        .get(nextHopInterfaceName)
        .containsIp(arpIp, c.getIpSpaces())) {
      FlowTrace trace = neighborUnreachableTrace(nextHopInterface, transmissionContext);
      transmissionContext._flowTraces.add(trace);
      return false;
    }
    return true;
  }
}
