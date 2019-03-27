package org.batfish.bddreachability;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Streams;
import com.google.common.collect.Table;
import org.batfish.z3.expr.StateExpr;

/**
 * Utility methods for {@link BDDReachabilityAnalysis} and {@link BDDReachabilityAnalysisFactory}.
 */
final class BDDReachabilityUtils {
  static Table<StateExpr, StateExpr, Edge> computeForwardEdgeTable(Iterable<Edge> edges) {
    ImmutableTable.Builder<StateExpr, StateExpr, Edge> forwardEdges = ImmutableTable.builder();
    Streams.stream(edges)
        .distinct() // edges may contain duplicates
        .forEach(edge -> forwardEdges.put(edge.getPreState(), edge.getPostState(), edge));
    return forwardEdges.build();
  }
}
