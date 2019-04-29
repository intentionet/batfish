package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.bddreachability.BDDReachabilityUtils.computeForwardEdgeTable;
import static org.batfish.common.util.CommonUtil.toImmutableMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.z3.IngressLocation;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.OriginateInterfaceLink;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.Query;

/**
 * A new reachability analysis engine using BDDs. The analysis maintains a graph that describes how
 * packets flow through the network and through logical phases of a router. In particular, the graph
 * nodes are {@link StateExpr StateExprs} and the edges are mostly the same as the NOD program
 * rules/transitions. {@link BDD BDDs} label the nodes and edges of the graph. A node label
 * represent the set of packets that can reach that node, and an edge label represents the set of
 * packets that can traverse the edge. There is a single designated {@link Query} StateExpr that we
 * compute reachability sets (i.e. sets of packets that reach the query state). The query state
 * never has any out-edges, and has in-edges from the dispositions of interest.
 *
 * <p>The two main departures from the NOD program are: 1) ACLs are encoded as a single BDD that
 * labels an edge (rather than a series of states/transitions in NOD programs). 2) Source NAT is
 * handled differently -- we don't maintain separate original and current source IP variables.
 * Instead, we keep track of where/how the packet is transformed as it flows through the network,
 * and reconstruct it after the fact. This requires some work that can't be expressed in BDDs.
 *
 * <p>We currently implement backward all-pairs reachability. Forward reachability is useful for
 * questions with a tight source constraint, e.g. "find me packets send from node A that get
 * dropped". When reasoning about many sources simultaneously, we have to somehow remember the
 * source, which is very expensive for a large number of sources. For queries that have to consider
 * all packets that can reach the query state, backward reachability is much more efficient.
 */
public class BDDReachabilityAnalysis {
  private final BDDPacket _bddPacket;

  // preState --> postState --> predicate
  private final Table<StateExpr, StateExpr, Edge> _forwardEdgeTable;

  // stateExprs that correspond to the IngressLocations of interest
  private final ImmutableSet<StateExpr> _ingressLocationStates;

  private final BDD _queryHeaderSpaceBdd;

  BDDReachabilityAnalysis(
      BDDPacket packet,
      Set<StateExpr> ingressLocationStates,
      List<Edge> edges,
      BDD queryHeaderSpaceBdd) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("constructs BDDReachabilityAnalysis").startActive()) {
      assert span != null; // avoid unused warning
      _bddPacket = packet;
      _forwardEdgeTable = computeForwardEdgeTable(edges);
      _ingressLocationStates = ImmutableSet.copyOf(ingressLocationStates);
      _queryHeaderSpaceBdd = queryHeaderSpaceBdd;
    }
  }

  Map<StateExpr, BDD> computeReverseReachableStates() {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysis.computeReverseReachableStates")
            .startActive()) {
      assert span != null; // avoid unused warning
      Map<StateExpr, BDD> reverseReachableStates = new HashMap<>();
      reverseReachableStates.put(Query.INSTANCE, _queryHeaderSpaceBdd);
      backwardFixpoint(reverseReachableStates);
      return ImmutableMap.copyOf(reverseReachableStates);
    }
  }

  Map<StateExpr, BDD> computeReverseReachableStates(Map<StateExpr, BDD> roots) {
    Map<StateExpr, BDD> reverseReachableStates = new HashMap<>(roots);
    backwardFixpoint(reverseReachableStates);
    return ImmutableMap.copyOf(reverseReachableStates);
  }

  Map<StateExpr, BDD> computeForwardReachableStates() {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("BDDReachabilityAnalysis.computeForwardReachableStates")
            .startActive()) {
      assert span != null; // avoid unused warning
      Map<StateExpr, BDD> forwardReachableStates = new LinkedHashMap<>();
      BDD one = _bddPacket.getFactory().one();
      _ingressLocationStates.forEach(state -> forwardReachableStates.put(state, one));
      forwardFixpoint(forwardReachableStates);
      return ImmutableMap.copyOf(forwardReachableStates);
    }
  }

  private void backwardFixpoint(Map<StateExpr, BDD> reverseReachable) {
    fixpoint(reverseReachable, Tables.transpose(_forwardEdgeTable), Edge::traverseBackward);
  }

  private void forwardFixpoint(Map<StateExpr, BDD> reachable) {
    fixpoint(reachable, _forwardEdgeTable, Edge::traverseForward);
  }

  /** Apply edges to the reachableSets until a fixed point is reached. */
  @VisibleForTesting
  static void fixpoint(
      Map<StateExpr, BDD> reachableSets,
      Table<StateExpr, StateExpr, Edge> edges,
      BiFunction<Edge, BDD, BDD> traverse) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysis.fixpoint").startActive()) {
      assert span != null; // avoid unused warning
      Set<StateExpr> dirtyStates = ImmutableSet.copyOf(reachableSets.keySet());

      while (!dirtyStates.isEmpty()) {
        Set<StateExpr> newDirtyStates = new HashSet<>();

        dirtyStates.forEach(
            dirtyState -> {
              Map<StateExpr, Edge> dirtyStateEdges = edges.row(dirtyState);
              if (dirtyStateEdges == null) {
                // dirtyState has no edges
                return;
              }

              BDD dirtyStateBDD = reachableSets.get(dirtyState);
              dirtyStateEdges.forEach(
                  (neighbor, edge) -> {
                    BDD result = traverse.apply(edge, dirtyStateBDD);
                    if (result.isZero()) {
                      return;
                    }

                    // update neighbor's reachable set
                    BDD oldReach = reachableSets.get(neighbor);
                    BDD newReach = oldReach == null ? result : oldReach.or(result);
                    if (oldReach == null || !oldReach.equals(newReach)) {
                      reachableSets.put(neighbor, newReach);
                      newDirtyStates.add(neighbor);
                    }
                  });
            });

        dirtyStates = newDirtyStates;
      }
    }
  }

  private Map<StateExpr, BDD> reachableInNRounds(int numRounds) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysis.reachableInNRounds").startActive()) {
      assert span != null; // avoid unused warning
      BDD one = _bddPacket.getFactory().one();

      // All ingress locations are reachable in 0 rounds.
      Map<StateExpr, BDD> reachableInNRounds =
          toImmutableMap(_ingressLocationStates, Function.identity(), k -> one);

      for (int round = 0; !reachableInNRounds.isEmpty() && round < numRounds; round++) {
        reachableInNRounds = propagate(reachableInNRounds);
      }
      return reachableInNRounds;
    }
  }

  /*
   * Detect infinite routing loops in the network.
   */
  public Map<IngressLocation, BDD> detectLoops() {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysis.detectLoops").startActive()) {
      assert span != null; // avoid unused warning
      /*
       * Run enough rounds to exceed the max TTL (255). It takes at most 6 iterations to go between
       * hops:
       * PreInInterface -> PostInInterface -> PostInVrf -> PreOutVrf -> PreOutEdge ->
       * PreOutEdgePostNat -> PreInInterface
       *
       * Since we don't model TTL, all packets on loops will loop forever. But most paths will stop
       * long before numRounds. What's left will be a few candidate location/headerspace pairs that
       * may be on loops. In practice this is most likely way more iterations than necessary.
       */
      int numRounds = 256 * 6;
      Map<StateExpr, BDD> reachableInNRounds = reachableInNRounds(numRounds);

      /*
       * Identify which of the candidates are actually on loops
       */
      Map<StateExpr, BDD> loopBDDs =
          reachableInNRounds.entrySet().stream()
              .filter(entry -> confirmLoop(entry.getKey(), entry.getValue()))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

      /*
       * Run backward to find the ingress locations/headerspaces that lead to loops.
       */
      backwardFixpoint(loopBDDs);

      /*
       * Extract the ingress location BDDs.
       */
      return getIngressLocationBDDs(loopBDDs);
    }
  }

  private Map<StateExpr, BDD> propagate(Map<StateExpr, BDD> bdds) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysis.propagate").startActive()) {
      assert span != null; // avoid unused warning
      BDD zero = _bddPacket.getFactory().zero();
      Map<StateExpr, BDD> newReachableInNRounds = new HashMap<>();
      bdds.forEach(
          (source, sourceBdd) ->
              _forwardEdgeTable
                  .row(source)
                  .forEach(
                      (target, edge) -> {
                        BDD targetBdd = newReachableInNRounds.getOrDefault(target, zero);
                        BDD newTragetBdd = targetBdd.or(edge.traverseForward(sourceBdd));
                        if (!newTragetBdd.isZero()) {
                          newReachableInNRounds.put(target, newTragetBdd);
                        }
                      }));
      return newReachableInNRounds;
    }
  }

  /**
   * Run BFS from one step past the initial state. Each round, check if the initial state has been
   * reached yet.
   */
  private boolean confirmLoop(StateExpr stateExpr, BDD bdd) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("BDDReachabilityAnalysis.confirmLoop").startActive()) {
      assert span != null; // avoid unused warning
      Map<StateExpr, BDD> reachable = propagate(ImmutableMap.of(stateExpr, bdd));
      Set<StateExpr> dirty = new HashSet<>(reachable.keySet());

      BDD zero = _bddPacket.getFactory().zero();
      while (!dirty.isEmpty()) {
        Set<StateExpr> newDirty = new HashSet<>();

        dirty.forEach(
            preState -> {
              Map<StateExpr, Edge> preStateOutEdges = _forwardEdgeTable.row(preState);
              if (preStateOutEdges == null) {
                // preState has no out-edges
                return;
              }

              BDD preStateBDD = reachable.get(preState);
              preStateOutEdges.forEach(
                  (postState, edge) -> {
                    BDD result = edge.traverseForward(preStateBDD);
                    if (result.isZero()) {
                      return;
                    }

                    // update postState BDD reverse-reachable from leaf
                    BDD oldReach = reachable.getOrDefault(postState, zero);
                    BDD newReach = oldReach == null ? result : oldReach.or(result);
                    if (oldReach == null || !oldReach.equals(newReach)) {
                      reachable.put(postState, newReach);
                      newDirty.add(postState);
                    }
                  });
            });

        dirty = newDirty;
        if (dirty.contains(stateExpr)) {
          if (!reachable.get(stateExpr).and(bdd).isZero()) {
            return true;
          }
        }
      }
      return false;
    }
  }

  public BDDPacket getBDDPacket() {
    return _bddPacket;
  }

  public Map<IngressLocation, BDD> getIngressLocationReachableBDDs() {
    Map<StateExpr, BDD> reverseReachableStates = computeReverseReachableStates();
    return getIngressLocationBDDs(reverseReachableStates);
  }

  private Map<IngressLocation, BDD> getIngressLocationBDDs(
      Map<StateExpr, BDD> reverseReachableStates) {
    BDD zero = _bddPacket.getFactory().zero();
    return _ingressLocationStates.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                BDDReachabilityAnalysis::toIngressLocation,
                root -> reverseReachableStates.getOrDefault(root, zero)));
  }

  @VisibleForTesting
  static IngressLocation toIngressLocation(StateExpr stateExpr) {
    checkArgument(stateExpr instanceof OriginateVrf || stateExpr instanceof OriginateInterfaceLink);

    if (stateExpr instanceof OriginateVrf) {
      OriginateVrf originateVrf = (OriginateVrf) stateExpr;
      return IngressLocation.vrf(originateVrf.getHostname(), originateVrf.getVrf());
    } else {
      OriginateInterfaceLink originateInterfaceLink = (OriginateInterfaceLink) stateExpr;
      return IngressLocation.interfaceLink(
          originateInterfaceLink.getHostname(), originateInterfaceLink.getIface());
    }
  }

  @VisibleForTesting
  Map<StateExpr, Map<StateExpr, Edge>> getForwardEdgeMap() {
    return _forwardEdgeTable.rowMap();
  }
}
