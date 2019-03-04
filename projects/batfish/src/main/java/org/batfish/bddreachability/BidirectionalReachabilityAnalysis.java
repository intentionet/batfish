package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.batfish.bddreachability.BDDReachabilityAnalysisSessionFactory.computeInitializedSesssions;
import static org.batfish.bddreachability.OriginationStateToTerminationState.originationStateToTerminationState;
import static org.batfish.bddreachability.ReversePassOriginationState.reverseTraceOriginationState;
import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.datamodel.FlowDisposition.LOOP;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.TransformationToTransition;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.question.bidirectionalreachability.BidirectionalReachabilityResult;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationVisitor;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.Query;

/** Performs a bidirectional reachability analysis. */
public final class BidirectionalReachabilityAnalysis {
  private final BDDPacket _bddPacket;
  private final Map<String, Configuration> _configs;
  private final Set<String> _forbiddenTransitNodes;
  private final Set<String> _requiredTransitNodes;

  private final BDD _zero;

  private final Map<StateExpr, BDD> _forwardPassOriginationConstraints;
  private final Multimap<StateExpr, Location> _originateStateToLocation;
  private final BDDReachabilityAnalysisFactory _factory;
  private final Supplier<BDDReachabilityAnalysis> _forwardPassAnalysis;
  private final Supplier<Map<StateExpr, BDD>> _forwardPassForwardReachableBdds;
  private final Supplier<Map<StateExpr, BDD>> _returnPassOrigBdds;
  private final Supplier<Map<String, List<BDDFirewallSessionTraceInfo>>> _initializedSessions;
  private final Supplier<BDDReachabilityAnalysis> _returnPassAnalysis;
  private final Supplier<Map<Location, BDD>> _forwardPassStartLocationToReturnPassFailureBdds;
  private final Supplier<BiMap<StateExpr, StateExpr>>
      _forwardPassTerminationStateToReturnPassOriginationState;
  private final Supplier<Map<Location, BDD>> _forwardPassStartLocationToReturnPassSuccessBdds;
  private final Supplier<Map<StateExpr, BDD>> _returnPassForwardReachableBdds;
  private final Supplier<Map<StateExpr, BDD>> _returnPassQueryConstraints;
  private final Supplier<BDDReverseTransformationRanges> _reverseTransformationRanges;

  public BidirectionalReachabilityAnalysis(
      BDDPacket bddPacket,
      Map<String, Configuration> configs,
      ForwardingAnalysis forwardingAnalysis,
      IpSpaceAssignment srcIpSpaceAssignment,
      AclLineMatchExpr initialForwardHeaderSpace,
      Set<String> forbiddenTransitNodes,
      Set<String> requiredTransitNodes,
      Set<String> forwardPassFinalNodes,
      Set<FlowDisposition> forwardPassActions) {
    _bddPacket = bddPacket;
    _configs = configs;
    _factory =
        new BDDReachabilityAnalysisFactory(bddPacket, configs, forwardingAnalysis, false, true);
    _forbiddenTransitNodes = ImmutableSet.copyOf(forbiddenTransitNodes);
    _requiredTransitNodes = ImmutableSet.copyOf(requiredTransitNodes);

    _zero = bddPacket.getFactory().zero();

    // Compute _forwardPassOriginationConstraints
    {
      IpAccessListToBdd ipAccessListToBdd =
          new IpAccessListToBddImpl(
              _bddPacket,
              BDDSourceManager.forInterfaces(_bddPacket, ImmutableSet.of()),
              ImmutableMap.of(),
              ImmutableMap.of());
      BDD initialForwardHeaderSpaceBdd = ipAccessListToBdd.toBdd(initialForwardHeaderSpace);
      _forwardPassOriginationConstraints =
          _factory.rootConstraints(srcIpSpaceAssignment, initialForwardHeaderSpaceBdd);
    }

    // Compute _originateStateToLocation
    {
      LocationVisitor<Optional<StateExpr>> locationToStateExpr =
          new LocationToOriginationStateExpr(_configs);
      ImmutableMultimap.Builder<StateExpr, Location> builder = ImmutableMultimap.builder();
      srcIpSpaceAssignment.getEntries().stream()
          .flatMap(entry -> entry.getLocations().stream())
          .map(
              loc ->
                  loc.accept(locationToStateExpr)
                      .map(stateExpr -> Maps.immutableEntry(stateExpr, loc))
                      .orElse(null))
          .filter(Objects::nonNull)
          .forEach(builder::put);
      _originateStateToLocation = builder.build();
    }

    _forwardPassAnalysis =
        Suppliers.memoize(
            () ->
                _factory.bddReachabilityAnalysis(
                    srcIpSpaceAssignment,
                    initialForwardHeaderSpace,
                    forbiddenTransitNodes,
                    requiredTransitNodes,
                    forwardPassFinalNodes,
                    forwardPassActions));
    _forwardPassForwardReachableBdds =
        Suppliers.memoize(() -> _forwardPassAnalysis.get().computeForwardReachableStates());
    _forwardPassTerminationStateToReturnPassOriginationState =
        Suppliers.memoize(this::computeForwardPassTerminationStateToReturnPassOriginationState);
    _returnPassOrigBdds = Suppliers.memoize(this::computeReturnPassOrigBdds);
    _returnPassQueryConstraints = Suppliers.memoize(this::computeReturnPassQueryConstraints);
    _reverseTransformationRanges = Suppliers.memoize(this::computeReverseTransformationRanges);
    _returnPassForwardReachableBdds =
        Suppliers.memoize(this::computeReturnPassForwardReachableBdds);
    _initializedSessions = Suppliers.memoize(this::computeInitializedSessions);
    _returnPassAnalysis = Suppliers.memoize(this::computeReturnPassAnalysis);
    _forwardPassStartLocationToReturnPassFailureBdds =
        Suppliers.memoize(this::computeForwardPassStartLocationToReturnPassFailureBdds);
    _forwardPassStartLocationToReturnPassSuccessBdds =
        Suppliers.memoize(this::computeForwardPassStartLocationToReturnPassSuccessBdds);
  }

  private Map<StateExpr, BDD> computeReturnPassQueryConstraints() {
    return computeReturnPassQueryConstraints(_bddPacket, _forwardPassOriginationConstraints);
  }

  /**
   * Convert forward-pass origination constraints to return-pass query constraints. For a
   * bidirectional query, we want return flows that make it back to corresponding origination
   * points. This method defines that correspondence: it connects both the forward and return flows
   * (by swapping source and destination fields) and the origination states in the forward pass
   * graph to termination states in the return pass graph.
   */
  @VisibleForTesting
  static Map<StateExpr, BDD> computeReturnPassQueryConstraints(
      BDDPacket packet, Map<StateExpr, BDD> forwardPassOriginationConstraints) {
    return forwardPassOriginationConstraints.entrySet().stream()
        .flatMap(
            entry -> {
              List<StateExpr> terminationStates =
                  originationStateToTerminationState(entry.getKey());
              if (terminationStates == null) {
                return Stream.of();
              }

              BDD queryConstraint = packet.swapSourceAndDestinationFields(entry.getValue());
              return terminationStates.stream()
                  .map(state -> Maps.immutableEntry(state, queryConstraint));
            })
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue, BDD::or));
  }

  private Map<StateExpr, BDD> computeReturnPassForwardReachableBdds() {
    return _returnPassAnalysis.get().computeForwardReachableStates();
  }

  private BiMap<StateExpr, StateExpr>
      computeForwardPassTerminationStateToReturnPassOriginationState() {
    return _forwardPassForwardReachableBdds.get().keySet().stream()
        .map(
            term -> {
              StateExpr orig = reverseTraceOriginationState(term);
              return orig == null ? null : Maps.immutableEntry(term, orig);
            })
        .filter(Objects::nonNull)
        .collect(ImmutableBiMap.toImmutableBiMap(Entry::getKey, Entry::getValue));
  }

  private Map<StateExpr, BDD> computeReturnPassOrigBdds() {
    Map<StateExpr, BDD> forwardReachableBdds = _forwardPassForwardReachableBdds.get();
    BiMap<StateExpr, StateExpr> forwardPassTerminationStateToReturnPassOriginationState =
        _forwardPassTerminationStateToReturnPassOriginationState.get();

    // construct the graph root (origination state) bdds for the return pass
    BDDSourceManager srcManager = _factory.getBDDSourceManagers().values().iterator().next();
    LastHopOutgoingInterfaceManager lastHopMgr = _factory.getLastHopManager();

    return forwardPassTerminationStateToReturnPassOriginationState.entrySet().stream()
        .map(
            entry -> {
              StateExpr term = entry.getKey();
              StateExpr orig = entry.getValue();
              BDD termBdd = forwardReachableBdds.get(term);

              BDD bdd = srcManager.existsSource(termBdd);
              if (lastHopMgr != null) {
                bdd = lastHopMgr.existsLastHop(bdd);
              }
              bdd = _bddPacket.swapSourceAndDestinationFields(bdd);
              return Maps.immutableEntry(orig, bdd);
            })
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, BDD::or));
  }

  private BDDReverseTransformationRanges computeReverseTransformationRanges() {
    return new BDDReverseTransformationRangesImpl(
        _configs,
        _forwardPassForwardReachableBdds.get(),
        _bddPacket,
        _factory.getAclPermitBdds(),
        _factory.getBDDSourceManagers(),
        _factory.getLastHopManager());
  }

  private Map<String, List<BDDFirewallSessionTraceInfo>> computeInitializedSessions() {
    checkNotNull(
        _factory.getLastHopManager(),
        "Cannot computeInitalizedSessions without a LastHopOutgoingInterfaceManager");

    // TODO creating these for every node is pretty wasteful. Better to limit to session nodes, or
    // be lazy
    Map<String, TransformationToTransition> transformationToTransitions =
        toImmutableMap(
            _configs,
            Entry::getKey,
            entry ->
                new TransformationToTransition(
                    _bddPacket, _factory.ipAccessListToBdd(entry.getValue())));

    BDDReverseFlowTransformationFactory reverseFlowTransformationFactory =
        new BDDReverseFlowTransformationFactoryImpl(_configs, transformationToTransitions);

    return computeInitializedSesssions(
        _bddPacket,
        _configs,
        _factory.getBDDSourceManagers(),
        _factory.getLastHopManager(),
        _forwardPassForwardReachableBdds.get(),
        reverseFlowTransformationFactory,
        _reverseTransformationRanges.get());
  }

  private BDDReachabilityAnalysis computeReturnPassAnalysis() {
    return _factory.bddReachabilityAnalysis(
        _returnPassOrigBdds.get(),
        _returnPassQueryConstraints.get(),
        _initializedSessions.get(),
        _forbiddenTransitNodes,
        _requiredTransitNodes,
        Sets.difference(FlowDisposition.FAILURE_DISPOSITIONS, ImmutableSet.of(LOOP)));
  }

  private Map<Location, BDD> computeForwardPassStartLocationToReturnPassFailureBdds() {
    Map<StateExpr, BDD> returnPassReachableBdds = _returnPassForwardReachableBdds.get();
    BDD failReturnFlows = returnPassReachableBdds.getOrDefault(Query.INSTANCE, _zero);
    if (failReturnFlows.isZero()) {
      return ImmutableMap.of();
    }
    BDD failForwardFlows = _bddPacket.swapSourceAndDestinationFields(failReturnFlows);
    return _originateStateToLocation.entries().stream()
        .flatMap(
            entry -> {
              StateExpr origState = entry.getKey();
              Location loc = entry.getValue();
              BDD bdd = failForwardFlows.and(_forwardPassOriginationConstraints.get(origState));
              return bdd.isZero() ? Stream.of() : Stream.of(Maps.immutableEntry(loc, bdd));
            })
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue, BDD::or));
  }

  private Map<Location, BDD> computeForwardPassStartLocationToReturnPassSuccessBdds() {
    Map<StateExpr, BDD> returnPassForwardReachableBdds = _returnPassForwardReachableBdds.get();
    Map<StateExpr, BDD> queryConstraints = _returnPassQueryConstraints.get();

    Map<StateExpr, BDD> reachableQueries =
        queryConstraints.entrySet().stream()
            .map(
                entry -> {
                  StateExpr key = entry.getKey();
                  BDD queryConstraint = entry.getValue();
                  BDD reachable = returnPassForwardReachableBdds.getOrDefault(key, _zero);
                  BDD reachableQuery = queryConstraint.and(reachable);
                  return reachableQuery.isZero() ? null : Maps.immutableEntry(key, reachableQuery);
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    BDDSourceManager srcMgr = _factory.getBDDSourceManagers().values().iterator().next();
    return _originateStateToLocation.entries().stream()
        .map(
            entry -> {
              StateExpr originationState = entry.getKey();
              Location loc = entry.getValue();
              List<StateExpr> terminationStates =
                  originationStateToTerminationState(originationState);

              if (terminationStates == null) {
                return null;
              }

              BDD bdd =
                  terminationStates.stream()
                      .map(reachableQueries::get)
                      .filter(Objects::nonNull)
                      .reduce(_zero, BDD::or);

              if (bdd.isZero()) {
                return null;
              }

              // remove non-header fields
              bdd = srcMgr.existsSource(bdd);
              bdd = _factory.getLastHopManager().existsLastHop(bdd);

              return Maps.immutableEntry(loc, _bddPacket.swapSourceAndDestinationFields(bdd));
            })
        .filter(Objects::nonNull)
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue, BDD::or));
  }

  public BidirectionalReachabilityResult getResult() {
    return new BidirectionalReachabilityResult(
        _forwardPassStartLocationToReturnPassSuccessBdds.get(),
        _forwardPassStartLocationToReturnPassFailureBdds.get());
  }

  BDDReverseTransformationRanges getReverseTransformationRanges() {
    return _reverseTransformationRanges.get();
  }

  Map<StateExpr, BDD> getReturnPassForwardReachableBdds() {
    return _returnPassForwardReachableBdds.get();
  }
}
