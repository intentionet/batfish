package org.batfish.bddreachability;

import static org.batfish.common.util.CommonUtil.toImmutableMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.z3.IngressLocation;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.Drop;
import org.batfish.z3.state.NeighborUnreachable;
import org.batfish.z3.state.OriginateInterfaceLink;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.Query;
import org.batfish.z3.state.visitors.DefaultTransitionGenerator;

/**
 * A new reachability analysis engine using BDDs. The analysis maintains a graph that describes how
 * packets flow through the network and through logical phases of a router. The graph is similar to
 * the one generated by {@link DefaultTransitionGenerator} for reachability analysis using NOD. In
 * particular, the graph nodes are {@link StateExpr StateExprs} and the edges are mostly the same as
 * the NOD program rules/transitions. {@link BDD BDDs} label the nodes and edges of the graph. A
 * node label represent the set of packets that can reach that node, and an edge label represents
 * the set of packets that can traverse the edge.
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
 * source, which is very expensive for a large number of sources. Something like multipath
 * consistency -- "find me a packet and a node such that when the node sends the packet, it can be
 * either accepted and dropped" -- for which there are many sources but only a few sinks ({@link
 * Accept}, {@link Drop}, and {@link NeighborUnreachable}), backward reachability is much more
 * efficient. We still have to remember the sinks, but there's only ever 3 of those, no matter how
 * many sources there are.
 */
public class BDDReachabilityAnalysis {
  private final BDDPacket _bddPacket;

  // preState --> postState --> predicate
  private final Map<StateExpr, Map<StateExpr, Edge>> _edges;

  // postState --> preState --> predicate
  private final Map<StateExpr, Map<StateExpr, Edge>> _reverseEdges;

  // stateExprs that correspond to the IngressLocations of interest
  private final ImmutableSet<StateExpr> _ingressLocationStates;

  BDDReachabilityAnalysis(
      BDDPacket packet,
      Set<StateExpr> ingressLocationStates,
      Map<StateExpr, Map<StateExpr, Edge>> edges) {
    _bddPacket = packet;
    _edges = edges;
    _reverseEdges = computeReverseEdges(_edges);
    _ingressLocationStates = ImmutableSet.copyOf(ingressLocationStates);
  }

  private static Map<StateExpr, Map<StateExpr, Edge>> computeReverseEdges(
      Map<StateExpr, Map<StateExpr, Edge>> edges) {
    Map<StateExpr, Map<StateExpr, Edge>> reverseEdges = new HashMap<>();
    edges.forEach(
        (preState, preStateOutEdges) ->
            preStateOutEdges.forEach(
                (postState, edge) ->
                    reverseEdges
                        .computeIfAbsent(postState, k -> new HashMap<>())
                        .put(preState, edge)));
    // freeze
    return toImmutableMap(
        reverseEdges, Entry::getKey, entry -> ImmutableMap.copyOf(entry.getValue()));
  }

  private Map<StateExpr, BDD> computeReverseReachableStates() {
    Map<StateExpr, BDD> reverseReachableStates = new HashMap<>();
    Set<StateExpr> dirty = new HashSet<>();

    reverseReachableStates.put(Query.INSTANCE, _bddPacket.getFactory().one());
    dirty.add(Query.INSTANCE);

    List<Long> roundTimes = new LinkedList<>();
    List<Integer> roundDirties = new LinkedList<>();

    while (!dirty.isEmpty()) {
      Set<StateExpr> newDirty = new HashSet<>();
      long time = System.currentTimeMillis();

      dirty.forEach(
          postState -> {
            Map<StateExpr, Edge> postStateInEdges = _reverseEdges.get(postState);
            if (postStateInEdges == null) {
              // postState has no in-edges
              return;
            }

            BDD postStateBDD = reverseReachableStates.get(postState);
            postStateInEdges.forEach(
                (preState, edge) -> {
                  BDD result = edge.traverseBackward(postStateBDD);
                  if (result.isZero()) {
                    return;
                  }

                  // update preState BDD reverse-reachable from leaf
                  BDD oldReach = reverseReachableStates.get(preState);
                  BDD newReach = oldReach == null ? result : oldReach.or(result);
                  if (oldReach == null || !oldReach.equals(newReach)) {
                    reverseReachableStates.put(preState, newReach);
                    newDirty.add(preState);
                  }
                });
          });

      dirty = newDirty;

      time = System.currentTimeMillis() - time;
      roundTimes.add(time);
      roundDirties.add(dirty.size());
    }

    return ImmutableMap.copyOf(reverseReachableStates);
  }

  public BDDPacket getBDDPacket() {
    return _bddPacket;
  }

  public Map<IngressLocation, BDD> getIngressLocationReachableBDDs() {
    BDD zero = _bddPacket.getFactory().zero();
    Map<StateExpr, BDD> reverseReachableStates = computeReverseReachableStates();
    return _ingressLocationStates
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                BDDReachabilityAnalysis::toIngressLocation,
                root -> reverseReachableStates.getOrDefault(root, zero)));
  }

  @VisibleForTesting
  static IngressLocation toIngressLocation(StateExpr stateExpr) {
    Preconditions.checkArgument(
        stateExpr instanceof OriginateVrf || stateExpr instanceof OriginateInterfaceLink);

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
  Map<StateExpr, Map<StateExpr, Edge>> getEdges() {
    return _edges;
  }
}
