package org.batfish.datamodel.isis;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Topology;

public class IsisTopology {
  /** Initialize the ISIS topology as a directed graph. */
  public static Network<IsisNode, IsisEdge> initIsisTopology(
      Map<String, Configuration> configurations, Topology topology) {
    Set<IsisEdge> edges =
        topology
            .getEdges()
            .stream()
            .map(edge -> IsisEdge.edgeIfCircuit(edge, configurations))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(ImmutableSet.toImmutableSet());
    MutableNetwork<IsisNode, IsisEdge> graph =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    ImmutableSet.Builder<IsisNode> nodes = ImmutableSet.builder();
    edges.forEach(
        edge -> {
          nodes.add(edge.getNode1());
          nodes.add(edge.getNode2());
        });
    nodes.build().forEach(graph::addNode);
    edges.forEach(edge -> graph.addEdge(edge.getNode1(), edge.getNode2(), edge));
    return ImmutableNetwork.copyOf(graph);
  }
}
