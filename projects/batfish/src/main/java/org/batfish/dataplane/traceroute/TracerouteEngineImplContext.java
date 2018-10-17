package org.batfish.dataplane.traceroute;

import static org.batfish.dataplane.traceroute.TracerouteUtils.createEnterSrcIfaceStep;
import static org.batfish.dataplane.traceroute.TracerouteUtils.isArpSuccessful;
import static org.batfish.dataplane.traceroute.TracerouteUtils.validateInputs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
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
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.EnterInputIfaceStep;
import org.batfish.datamodel.flow.ExitOutputIfaceStep;
import org.batfish.datamodel.flow.ExitOutputIfaceStep.ExitOutputIfaceStepDetail;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.InboundStep;
import org.batfish.datamodel.flow.InboundStep.InboundStepDetail;
import org.batfish.datamodel.flow.RouteInfo;
import org.batfish.datamodel.flow.RoutingStep;
import org.batfish.datamodel.flow.RoutingStep.RoutingStepDetail;
import org.batfish.datamodel.flow.Step;
import org.batfish.datamodel.flow.StepAction;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.pojo.Node;

/**
 * Class containing an implementation of {@link
 * org.batfish.dataplane.TracerouteEngineImpl#buildFlows(DataPlane, Set, Map, boolean)} and the
 * context (data) needed for it
 */
public class TracerouteEngineImplContext {

  private static class TransmissionContext {
    private final Map<String, IpAccessList> _aclDefinitions;
    private final Node _currentNode;
    private String _filterOutNotes;
    private final List<Trace> _flowTraces;
    private final List<Hop> _hopsSoFar;
    private final NavigableMap<String, IpSpace> _namedIpSpaces;
    private final Flow _originalFlow;
    private final List<RouteInfo> _routesForThisNextHopInterface;
    private final Flow _transformedFlow;

    private TransmissionContext(
        Map<String, IpAccessList> aclDefinitions,
        Node currentNode,
        List<Trace> flowTraces,
        List<Hop> hopsSoFar,
        NavigableMap<String, IpSpace> namedIpSpaces,
        Flow originalFlow,
        List<RouteInfo> routesForThisNextHopInterface,
        Flow transformedFlow) {
      _aclDefinitions = aclDefinitions;
      _currentNode = currentNode;
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
              _currentNode,
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

  static final String TRACEROUTE_DUMMY_OUT_INTERFACE = "traceroute_dummy_out_interface";

  static final String TRACEROUTE_DUMMY_NODE = "traceroute_dummy_node";

  private final Map<String, Configuration> _configurations;
  private final DataPlane _dataPlane;
  private final Map<String, Map<String, Fib>> _fibs;
  private final Set<Flow> _flows;
  private final ForwardingAnalysis _forwardingAnalysis;
  private final boolean _ignoreAcls;

  public TracerouteEngineImplContext(
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

  private void processCurrentNextHopInterfaceEdges(
      String currentNodeName,
      Set<Hop> visitedHops,
      @Nullable String srcInterface,
      Ip dstIp,
      String nextHopInterfaceName,
      @Nullable Ip finalNextHopIp,
      SortedSet<Edge> edges,
      TransmissionContext transmissionContext,
      ImmutableList.Builder<Step<?>> stepBuilder) {
    if (!processFlowTransmission(
        currentNodeName,
        srcInterface,
        dstIp,
        nextHopInterfaceName,
        finalNextHopIp,
        transmissionContext,
        stepBuilder)) {
      return;
    }
    IpAccessList outFilter =
        _configurations
            .get(transmissionContext._currentNode.getName())
            .getAllInterfaces()
            .get(nextHopInterfaceName)
            .getOutgoingFilter();
    stepBuilder.add(
        ExitOutputIfaceStep.builder()
            .setDetail(
                ExitOutputIfaceStepDetail.builder()
                    .setOutputInterface(
                        new NodeInterfacePair(currentNodeName, nextHopInterfaceName))
                    .setOutputFilter(outFilter != null ? outFilter.getName() : null)
                    .setTransformedFlow(
                        hopFlow(
                            transmissionContext._originalFlow,
                            transmissionContext._transformedFlow))
                    .build())
            .setAction(StepAction.SENT_OUT)
            .build());
    Hop hop = new Hop(new Node(currentNodeName), stepBuilder.build());
    transmissionContext._hopsSoFar.add(hop);
    for (Edge edge : edges) {
      if (!edge.getNode1().equals(currentNodeName)) {
        continue;
      }
      if (isArpSuccessful(
          finalNextHopIp != null ? finalNextHopIp : dstIp,
          _forwardingAnalysis,
          _configurations.get(edge.getNode2()),
          edge.getInt2())) {
        processHop(
            edge.getNode2(),
            edge.getInt2(),
            transmissionContext,
            transmissionContext._transformedFlow,
            visitedHops);
      }
    }
  }

  private boolean processFlowTransmission(
      String currentNodeName,
      @Nullable String srcInterface,
      Ip dstIp,
      String nextHopInterfaceName,
      @Nullable Ip finalNextHopIp,
      TransmissionContext transmissionContext,
      ImmutableList.Builder<Step<?>> stepBuilder) {
    // check output filter
    IpAccessList outFilter =
        _configurations
            .get(transmissionContext._currentNode.getName())
            .getAllInterfaces()
            .get(nextHopInterfaceName)
            .getOutgoingFilter();
    if (!_ignoreAcls && outFilter != null) {
      FilterResult filterResult =
          outFilter.filter(
              transmissionContext._transformedFlow,
              srcInterface,
              transmissionContext._aclDefinitions,
              transmissionContext._namedIpSpaces);
      if (filterResult.getAction() == LineAction.DENY) {
        stepBuilder.add(
            ExitOutputIfaceStep.builder()
                .setDetail(
                    ExitOutputIfaceStepDetail.builder()
                        .setOutputInterface(
                            new NodeInterfacePair(currentNodeName, nextHopInterfaceName))
                        .setOutputFilter(outFilter.getName())
                        .setTransformedFlow(
                            hopFlow(
                                transmissionContext._originalFlow,
                                transmissionContext._transformedFlow))
                        .build())
                .setAction(StepAction.BLOCKED)
                .build());
        Hop deniedOutHop = new Hop(new Node(currentNodeName), stepBuilder.build());
        transmissionContext._hopsSoFar.add(deniedOutHop);
        Trace trace = new Trace(FlowDisposition.DENIED_OUT, transmissionContext._hopsSoFar);
        transmissionContext._flowTraces.add(trace);
        return false;
      }
    }
    Ip arpIp = finalNextHopIp != null ? finalNextHopIp : dstIp;
    Configuration c = _configurations.get(transmissionContext._currentNode.getName());
    // halt processing and add neighbor-unreachable trace if no one would respond
    if (_forwardingAnalysis
        .getNeighborUnreachable()
        .get(transmissionContext._currentNode.getName())
        .get(c.getAllInterfaces().get(nextHopInterfaceName).getVrfName())
        .get(nextHopInterfaceName)
        .containsIp(arpIp, c.getIpSpaces())) {
      stepBuilder.add(
          ExitOutputIfaceStep.builder()
              .setDetail(
                  ExitOutputIfaceStepDetail.builder()
                      .setOutputInterface(
                          new NodeInterfacePair(currentNodeName, nextHopInterfaceName))
                      .setOutputFilter(outFilter != null ? outFilter.getName() : null)
                      .setTransformedFlow(
                          hopFlow(
                              transmissionContext._originalFlow,
                              transmissionContext._transformedFlow))
                      .build())
              .setAction(StepAction.DROPPED)
              .build());
      Hop neighborUnreachableOrExitsNetwork =
          new Hop(new Node(currentNodeName), stepBuilder.build());
      transmissionContext._hopsSoFar.add(neighborUnreachableOrExitsNetwork);
      Trace trace =
          new Trace(
              FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
              transmissionContext._hopsSoFar);
      transmissionContext._flowTraces.add(trace);
      return false;
    }
    return true;
  }

  /**
   * Builds the possible {@link Trace}s for a {@link Set} of {@link Flow}s in {@link
   * TracerouteEngineImplContext#_flows}
   *
   * @return {@link SortedMap} of {@link Flow} to a {@link List} of {@link Trace}s
   */
  public SortedMap<Flow, List<Trace>> buildFlows() {
    Map<Flow, List<Trace>> flowTraces = new ConcurrentHashMap<>();
    _flows
        .parallelStream()
        .forEach(
            flow -> {
              List<Trace> currentFlowTraces =
                  flowTraces.computeIfAbsent(flow, k -> new ArrayList<>());
              validateInputs(_configurations, flow);
              String ingressNodeName = flow.getIngressNode();
              Set<Hop> visitedHops = Collections.emptySet();
              List<Hop> hops = new ArrayList<>();
              String ingressInterfaceName = flow.getIngressInterface();
              if (ingressInterfaceName != null) {
                TransmissionContext transmissionContext =
                    new TransmissionContext(
                        Maps.newHashMap(),
                        new Node(ingressNodeName),
                        currentFlowTraces,
                        hops,
                        Maps.newTreeMap(),
                        flow,
                        new ArrayList<>(),
                        flow);
                processHop(
                    ingressNodeName, ingressInterfaceName, transmissionContext, flow, visitedHops);
              } else {
                TransmissionContext transmissionContext =
                    new TransmissionContext(
                        Maps.newHashMap(),
                        new Node(flow.getIngressNode()),
                        currentFlowTraces,
                        hops,
                        Maps.newTreeMap(),
                        flow,
                        new ArrayList<>(),
                        flow);
                processHop(ingressNodeName, null, transmissionContext, flow, visitedHops);
              }
            });
    return new TreeMap<>(flowTraces);
  }

  private void processHop(
      String currentNodeName,
      @Nullable String inputIfaceName,
      TransmissionContext oldTransmissionContext,
      Flow currentFlow,
      Set<Hop> visitedHops) {
    List<Step<?>> steps = new ArrayList<>();
    Configuration currentConfiguration = _configurations.get(currentNodeName);
    if (currentConfiguration == null) {
      throw new BatfishException(
          String.format(
              "Node %s is not in the network, cannot perform traceroute", currentNodeName));
    }

    Map<String, IpAccessList> aclDefinitions = currentConfiguration.getIpAccessLists();
    NavigableMap<String, IpSpace> namedIpSpaces = currentConfiguration.getIpSpaces();

    TransmissionContext transmissionContext = oldTransmissionContext.branch();

    Ip dstIp = currentFlow.getDstIp();

    // trace was received on a source interface of this hop
    if (inputIfaceName != null) {
      EnterInputIfaceStep enterIfaceStep =
          createEnterSrcIfaceStep(
              currentConfiguration,
              inputIfaceName,
              _ignoreAcls,
              currentFlow,
              aclDefinitions,
              namedIpSpaces,
              _dataPlane);
      if (enterIfaceStep != null) {
        steps.add(enterIfaceStep);
        if (enterIfaceStep.getAction() == StepAction.BLOCKED) {
          Hop deniedHop = new Hop(new Node(currentNodeName), ImmutableList.copyOf(steps));
          transmissionContext._hopsSoFar.add(deniedHop);
          Trace trace = new Trace(FlowDisposition.DENIED_IN, transmissionContext._hopsSoFar);
          transmissionContext._flowTraces.add(trace);
          return;
        }
      }
    }

    // Figure out where the trace came from..
    String vrfName;
    if (inputIfaceName == null) {
      vrfName = currentFlow.getIngressVrf();
    } else {
      vrfName = currentConfiguration.getAllInterfaces().get(inputIfaceName).getVrfName();
    }

    // Accept if the flow is destined for this vrf on this host.
    if (_dataPlane
        .getIpVrfOwners()
        .getOrDefault(currentFlow.getDstIp(), ImmutableMap.of())
        .getOrDefault(currentConfiguration.getHostname(), ImmutableSet.of())
        .contains(vrfName)) {
      InboundStep inboundStep =
          InboundStep.builder()
              .setAction(StepAction.RECEIVED)
              .setDetail(new InboundStepDetail())
              .build();
      steps.add(inboundStep);

      Hop acceptedHop = new Hop(new Node(currentNodeName), ImmutableList.copyOf(steps));
      transmissionContext._hopsSoFar.add(acceptedHop);
      Trace trace = new Trace(FlowDisposition.ACCEPTED, transmissionContext._hopsSoFar);
      transmissionContext._flowTraces.add(trace);
      return;
    }

    // .. and what the next hops are based on the FIB.
    Fib currentFib = _fibs.get(currentNodeName).get(vrfName);
    Set<String> nextHopInterfaces = currentFib.getNextHopInterfaces(dstIp);
    if (nextHopInterfaces.isEmpty()) {
      // add a step for  NO_ROUTE from source to output interface
      RoutingStep.Builder routingStepBuilder = RoutingStep.builder();
      routingStepBuilder
          .setDetail(RoutingStepDetail.builder().build())
          .setAction(StepAction.DROPPED);
      steps.add(routingStepBuilder.build());
      Hop noRouteHop = new Hop(new Node(currentNodeName), ImmutableList.copyOf(steps));
      transmissionContext._hopsSoFar.add(noRouteHop);
      Trace trace = new Trace(FlowDisposition.NO_ROUTE, transmissionContext._hopsSoFar);
      transmissionContext._flowTraces.add(trace);
      return;
    }

    Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfacesByRoute =
        currentFib.getNextHopInterfacesByRoute(dstIp);

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
                List<RouteInfo> routesForThisNextHopInterface =
                    routeCandidates
                        .stream()
                        .map(
                            rc ->
                                new RouteInfo(
                                    rc.getClass().getSimpleName(),
                                    rc.getNetwork(),
                                    rc.getNextHopIp()))
                        .collect(ImmutableList.toImmutableList());

                ImmutableList.Builder<Step<?>> clonedStepsBuilder = ImmutableList.builder();
                clonedStepsBuilder.addAll(steps);
                clonedStepsBuilder.add(
                    RoutingStep.builder()
                        .setDetail(
                            RoutingStepDetail.builder()
                                .setRoutes(routesForThisNextHopInterface)
                                .build())
                        .setAction(StepAction.FORWARDED)
                        .build());

                if (nextHopInterfaceName.equals(Interface.NULL_INTERFACE_NAME)) {
                  clonedStepsBuilder.add(
                      ExitOutputIfaceStep.builder()
                          .setDetail(
                              ExitOutputIfaceStepDetail.builder()
                                  .setOutputInterface(
                                      new NodeInterfacePair(
                                          currentNodeName, Interface.NULL_INTERFACE_NAME))
                                  .build())
                          .setAction(StepAction.DROPPED)
                          .build());
                  Hop nullRoutedHop =
                      new Hop(new Node(currentNodeName), clonedStepsBuilder.build());
                  transmissionContext._hopsSoFar.add(nullRoutedHop);
                  Trace trace =
                      new Trace(FlowDisposition.NULL_ROUTED, transmissionContext._hopsSoFar);
                  transmissionContext._flowTraces.add(trace);
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
                        currentFlow,
                        inputIfaceName,
                        aclDefinitions,
                        namedIpSpaces,
                        outgoingInterface.getSourceNats());

                SortedSet<Edge> edges =
                    _dataPlane.getTopology().getInterfaceEdges().get(nextHopInterface);

                TransmissionContext clonedTransmissionContext =
                    new TransmissionContext(
                        aclDefinitions,
                        new Node(currentNodeName),
                        transmissionContext._flowTraces,
                        transmissionContext._hopsSoFar,
                        namedIpSpaces,
                        currentFlow,
                        routesForThisNextHopInterface,
                        newTransformedFlow);
                if (edges == null || edges.isEmpty()) {
                  updateDeniedOrUnreachableTrace(
                      currentNodeName,
                      outgoingInterface,
                      inputIfaceName,
                      clonedTransmissionContext,
                      clonedStepsBuilder);
                } else {
                  processCurrentNextHopInterfaceEdges(
                      currentNodeName,
                      visitedHops,
                      inputIfaceName,
                      dstIp,
                      nextHopInterfaceName,
                      finalNextHopIp,
                      edges,
                      clonedTransmissionContext,
                      clonedStepsBuilder);
                }
              });
    }
  }

  @Nullable
  private static Flow hopFlow(@Nullable Flow originalFlow, @Nullable Flow transformedFlow) {
    if (originalFlow == transformedFlow) {
      return null;
    } else {
      return transformedFlow;
    }
  }

  private void updateDeniedOrUnreachableTrace(
      String currentNodeName,
      Interface outInterface,
      String sourceInterfaceName,
      TransmissionContext transmissionContext,
      ImmutableList.Builder<Step<?>> stepsTillNow) {
    IpAccessList outFilter = outInterface.getOutgoingFilter();
    boolean denied = false;
    if (!_ignoreAcls && outFilter != null) {
      FilterResult filterResult =
          outFilter.filter(
              transmissionContext._transformedFlow,
              sourceInterfaceName,
              transmissionContext._aclDefinitions,
              transmissionContext._namedIpSpaces);
      denied = filterResult.getAction() == LineAction.DENY;
    }
    ExitOutputIfaceStep.Builder exitOutIfaceBuilder = ExitOutputIfaceStep.builder();
    exitOutIfaceBuilder.setDetail(
        ExitOutputIfaceStepDetail.builder()
            .setOutputInterface(new NodeInterfacePair(currentNodeName, outInterface.getName()))
            .setOutputFilter(outFilter != null ? outFilter.getName() : null)
            .setTransformedFlow(
                hopFlow(transmissionContext._originalFlow, transmissionContext._transformedFlow))
            .build());
    Trace trace;
    if (denied) {
      // add a denied out step action and terminate the current trace
      exitOutIfaceBuilder.setAction(StepAction.BLOCKED);
      List<Step<?>> currentSteps = stepsTillNow.add(exitOutIfaceBuilder.build()).build();
      Hop deniedOutHop = new Hop(new Node(currentNodeName), currentSteps);
      transmissionContext._hopsSoFar.add(deniedOutHop);
      trace = new Trace(FlowDisposition.DENIED_OUT, transmissionContext._hopsSoFar);
    } else {
      // add a neighbor unreachable step and terminate the current trace
      exitOutIfaceBuilder.setAction(StepAction.DROPPED);
      List<Step<?>> currentSteps = stepsTillNow.add(exitOutIfaceBuilder.build()).build();
      Hop neighborUnreachableHop = new Hop(new Node(currentNodeName), currentSteps);
      transmissionContext._hopsSoFar.add(neighborUnreachableHop);
      trace =
          new Trace(
              FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
              transmissionContext._hopsSoFar);
    }
    transmissionContext._flowTraces.add(trace);
  }
}
