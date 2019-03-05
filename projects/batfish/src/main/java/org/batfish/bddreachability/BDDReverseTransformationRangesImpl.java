package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.bddreachability.BDDReverseTransformationRangesImpl.TransformationType.INCOMING;
import static org.batfish.bddreachability.BDDReverseTransformationRangesImpl.TransformationType.OUTGOING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
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
@ParametersAreNonnullByDefault
final class BDDReverseTransformationRangesImpl implements BDDReverseTransformationRanges {
  private final BDDPacket _bddPacket;
  private final Map<String, Configuration> _configs;

  enum TransformationType {
    INCOMING,
    OUTGOING
  }

  /**
   * Identifies a reverse transformation for a session.
   *
   * <p>The node, iface and transformationType uniquely determine a particular transformation in the
   * network.
   *
   * <p>The inIface and lastHop determine a set of equivalent sessions (i.e. they are forwarded to
   * the same nextHop out the same interface).
   */
  public static final class Key {
    final @Nonnull String _node;
    final @Nonnull String _iface;
    final @Nonnull TransformationType _transformationType;

    final @Nullable String _inIface;
    final @Nullable NodeInterfacePair _lastHop;

    Key(
        @Nonnull String node,
        @Nonnull String iface,
        @Nonnull TransformationType transformationType,
        @Nullable String inIface,
        @Nullable NodeInterfacePair lastHop) {
      checkArgument(
          (inIface != null) || (lastHop == null), "If inIface is null, lastHop must be null");
      _node = node;
      _iface = iface;
      _transformationType = transformationType;
      _inIface = inIface;
      _lastHop = lastHop;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key key = (Key) o;
      return Objects.equals(_node, key._node)
          && Objects.equals(_iface, key._iface)
          && Objects.equals(_transformationType, key._transformationType)
          && Objects.equals(_inIface, key._inIface)
          && Objects.equals(_lastHop, key._lastHop);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_node, _iface, _transformationType, _inIface, _lastHop);
    }
  }

  /**
   * BDDs defining the set of flows permitted by each filter in the network. Node -> AclName -> BDD.
   *
   * <p>null when _ignoreFilters is true.
   */
  private final @Nullable Map<String, Map<String, Supplier<BDD>>> _forwardFlowFilterBdds;

  private final Map<StateExpr, BDD> _forwardReachableSets;

  /** Cache for ranges of reversed incoming and outgoing transformations. */
  private final Map<Key, BDD> _cache;

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
    _cache = new HashMap<>();
    _outgoingTransformationRangeStates =
        computeOutgoingTransformationRangeStates(forwardReachableSets.keySet());
    _srcManagers = srcManagers;
    _lastHopManager = lastHopManager;
    _one = bddPacket.getFactory().one();
    _zero = bddPacket.getFactory().zero();
  }

  /** Computes the valid outputs of a reversed incoming transformation on an interface. */
  @Override
  public BDD reverseIncomingTransformationRange(
      String node, String iface, @Nullable String inIface, @Nullable NodeInterfacePair lastHop) {
    return _cache.computeIfAbsent(
        new Key(node, iface, INCOMING, inIface, lastHop), this::computeReverseTransformationRange);
  }

  /** Computes the valid outputs of a reversed outgoing transformation on an interface. */
  @Override
  public BDD reverseOutgoingTransformationRange(
      String node, String iface, @Nullable String inIface, @Nullable NodeInterfacePair lastHop) {
    return _cache.computeIfAbsent(
        new Key(node, iface, OUTGOING, inIface, lastHop), this::computeReverseTransformationRange);
  }

  private BDD computeReverseTransformationRange(Key key) {
    switch (key._transformationType) {
      case INCOMING:
        assert key._inIface == null || key._inIface.equals(key._iface);
        return computeReverseIncomingTransformationRange(key._node, key._iface, key._lastHop);
      case OUTGOING:
        return computeReverseOutgoingTransformationRange(
            key._node, key._iface, key._inIface, key._lastHop);
      default:
        throw new IllegalArgumentException(
            "Unexpected TransformationType: " + key._transformationType);
    }
  }

  private BDD computeReverseIncomingTransformationRange(
      String node, String iface, @Nullable NodeInterfacePair lastHop) {
    BDD srcAndLastHop = sourceAndLastHopConstraint(node, iface, lastHop);
    BDD reach = _forwardReachableSets.getOrDefault(new PreInInterface(node, iface), _zero);
    BDD srcAndReach = srcAndLastHop.and(reach);
    BDD inAcl = forwardIncomingFilterFlowBdd(node, iface);
    BDD reachAndInAcl = srcAndReach.and(inAcl);
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

  private BDD computeReverseOutgoingTransformationRange(
      String node, String outIface, @Nullable String inIface, @Nullable NodeInterfacePair lastHop) {
    BDD outAcl = forwardPreTransformationOutgoingFilterFlowBdd(node, outIface);
    BDD srcAndLastHop = sourceAndLastHopConstraint(node, inIface, lastHop);
    BDD reach =
        _outgoingTransformationRangeStates.asMap()
            .getOrDefault(new NodeInterfacePair(node, outIface), ImmutableSet.of()).stream()
            .map(_forwardReachableSets::get)
            .reduce(BDD::or)
            .orElse(_zero);
    BDD reachAndOutAcl = srcAndLastHop.and(reach).and(outAcl);
    BDD erased = eraseNonHeaderFields(reachAndOutAcl);
    return _bddPacket.swapSourceAndDestinationFields(erased);
  }

  @VisibleForTesting
  BDD sourceAndLastHopConstraint(
      String node, @Nullable String inIface, @Nullable NodeInterfacePair lastHop) {
    checkArgument(inIface != null || lastHop == null, "Can't have a lastHop without an inIface");
    BDDSourceManager srcMgr = _srcManagers.get(node);
    if (inIface == null) {
      return srcMgr.getOriginatingFromDeviceBDD();
    }
    BDD srcBdd = srcMgr.getSourceInterfaceBDD(inIface);
    if (lastHop == null) {
      return srcBdd.and(_lastHopManager.getNoLastHopOutgoingInterfaceBdd(node, inIface));
    } else {
      return srcBdd.and(
          _lastHopManager.getLastHopOutgoingInterfaceBdd(
              lastHop.getHostname(), lastHop.getInterface(), node, inIface));
    }
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
