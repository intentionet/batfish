package org.batfish.bddreachability;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.PreInInterface;

/**
 * Implemention of valid ranges (outputs) of reversed transformations (for reverse flows). The valid
 * ranges correspond to the actual inputs the forward transformation was applied to in a forward
 * pass of a bidirectional reachability query (modulo swapping of source/dest header fields).
 */
final class BDDReverseTransformationRangesImpl implements BDDReverseTransformationRanges {
  private final BDDPacket _bddPacket;
  private final Map<String, Configuration> _configs;

  /**
   * BDDs defining the set of flows permitted by each filter in the network. Node -> AclName -> BDD.
   *
   * <p>null when _ignoreFilters is true.
   */
  private final @Nullable Map<String, Map<String, Supplier<BDD>>> _forwardFlowFilterBdds;

  private final Map<StateExpr, BDD> _forwardReachableSets;

  /** Caches for ranges of reversed incoming and outgoing transformations. */
  private final Map<NodeInterfacePair, BDD> _incomingCache;

  private final Map<NodeInterfacePair, BDD> _outgoingCache;

  /**
   * The StateExprs that collectively whose reachable sets (unioned together) define the range of a
   * reversed outgoing transformation.
   */
  private final Multimap<NodeInterfacePair, StateExpr> _outgoingTransformationRangeStates;

  private final BDD _one;
  private final BDD _zero;
  private final Map<String, BDDSourceManager> _srcManagers;
  private final LastHopOutgoingInterfaceManager _lastHopManager;

  public BDDReverseTransformationRangesImpl(
      Map<String, Configuration> configs,
      Map<StateExpr, BDD> forwardReachableSets,
      BDDPacket bddPacket,
      @Nullable Map<String, Map<String, Supplier<BDD>>> forwardFlowFilterBdds,
      Map<String, BDDSourceManager> srcManagers,
      LastHopOutgoingInterfaceManager lastHopManager) {
    _bddPacket = bddPacket;
    _configs = configs;
    _forwardReachableSets = forwardReachableSets;
    _forwardFlowFilterBdds = forwardFlowFilterBdds;
    _incomingCache = new HashMap<>();
    _outgoingCache = new HashMap<>();
    _outgoingTransformationRangeStates =
        computeOutgoingTransformationRangeStates(forwardReachableSets.keySet());
    _srcManagers = srcManagers;
    _lastHopManager = lastHopManager;
    _one = bddPacket.getFactory().one();
    _zero = bddPacket.getFactory().zero();
  }

  /** Computes the valid outputs of a reversed incoming transformation on an interface. */
  @Override
  public BDD reverseIncomingTransformationRange(String node, String iface) {
    return _incomingCache.computeIfAbsent(
        new NodeInterfacePair(node, iface), this::computeReverseIncomingTransformationRange);
  }

  /** Computes the valid outputs of a reversed outgoing transformation on an interface. */
  @Override
  public BDD reverseOutgoingTransformationRange(String node, String iface) {
    return _outgoingCache.computeIfAbsent(
        new NodeInterfacePair(node, iface), this::computeReverseOutgoingTransformationRange);
  }

  private BDD computeReverseIncomingTransformationRange(NodeInterfacePair nodeInterfacePair) {
    String node = nodeInterfacePair.getHostname();
    String iface = nodeInterfacePair.getInterface();
    BDD reach = _forwardReachableSets.getOrDefault(new PreInInterface(node, iface), _zero);
    BDD inAcl = forwardIncomingFilterFlowBdd(node, iface);
    BDD reachAndInAcl = reach.and(inAcl);
    BDD erased = eraseNonHeaderFields(reachAndInAcl);
    return _bddPacket.swapSourceAndDestinationFields(erased);
  }

  private BDD forwardIncomingFilterFlowBdd(String hostname, String iface) {
    if (_forwardFlowFilterBdds == null) {
      // ignoreFilters
      return _one;
    }
    IpAccessList incomingFilter =
        _configs.get(hostname).getAllInterfaces().get(iface).getIncomingFilter();
    return incomingFilter == null
        ? _one
        : _forwardFlowFilterBdds.get(hostname).get(incomingFilter.getName()).get();
  }

  private BDD computeReverseOutgoingTransformationRange(NodeInterfacePair iface) {
    String hostname = iface.getHostname();
    String iName = iface.getInterface();
    BDD outAcl = forwardPreTransformationOutgoingFilterFlowBdd(hostname, iName);
    BDD reach =
        _outgoingTransformationRangeStates.asMap().getOrDefault(iface, ImmutableSet.of()).stream()
            .map(_forwardReachableSets::get)
            .reduce(BDD::or)
            .orElse(_zero);
    BDD reachAndOutAcl = reach.and(outAcl);
    BDD erased = eraseNonHeaderFields(reachAndOutAcl);
    return _bddPacket.swapSourceAndDestinationFields(erased);
  }

  private BDD eraseNonHeaderFields(BDD bdd) {
    // Invariant: all source managers share the same source variable, so can use any of them
    return _srcManagers.values().iterator().next().existsSource(_lastHopManager.existsLastHop(bdd));
  }

  private BDD forwardPreTransformationOutgoingFilterFlowBdd(String hostname, String iface) {
    if (_forwardFlowFilterBdds == null) {
      // ignoreFilters
      return _one;
    }
    IpAccessList preTransformationOutgoingFilter =
        _configs.get(hostname).getAllInterfaces().get(iface).getPreTransformationOutgoingFilter();
    return preTransformationOutgoingFilter == null
        ? _one
        : _forwardFlowFilterBdds.get(hostname).get(preTransformationOutgoingFilter.getName()).get();
  }

  private static Multimap<NodeInterfacePair, StateExpr> computeOutgoingTransformationRangeStates(
      Set<StateExpr> states) {
    ImmutableMultimap.Builder<NodeInterfacePair, StateExpr> builder = ImmutableMultimap.builder();
    states.forEach(
        state -> {
          NodeInterfacePair outIface = state.accept(PreOutgoingTransformationNodeVisitor.INSTANCE);
          if (outIface != null) {
            builder.put(outIface, state);
          }
        });
    return builder.build();
  }
}
