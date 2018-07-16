package org.batfish.common.topology;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import java.util.SortedSet;
import javax.annotation.Nonnull;

public final class Layer2Topology {

  private static final String PROP_EDGES = "edges";

  @JsonCreator
  private static @Nonnull Layer2Topology create(
      @JsonProperty(PROP_EDGES) Iterable<Layer2Edge> edges) {
    return new Layer2Topology(edges != null ? edges : ImmutableSortedSet.of());
  }

  private final ImmutableNetwork<Layer2Node, Layer2Edge> _graph;

  public Layer2Topology(@Nonnull Iterable<Layer2Edge> edges) {
    MutableNetwork<Layer2Node, Layer2Edge> graph =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    edges.forEach(
        edge -> {
          graph.addNode(edge.getNode1());
          graph.addNode(edge.getNode2());
          graph.addEdge(edge.getNode1(), edge.getNode2(), edge);
        });
    _graph = ImmutableNetwork.copyOf(graph);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer2Topology)) {
      return false;
    }
    return _graph.equals(((Layer2Topology) obj)._graph);
  }

  @JsonIgnore
  public @Nonnull ImmutableNetwork<Layer2Node, Layer2Edge> getGraph() {
    return _graph;
  }

  @JsonProperty(PROP_EDGES)
  private SortedSet<Layer2Edge> getJsonEdges() {
    return ImmutableSortedSet.copyOf(_graph.edges());
  }

  @Override
  public int hashCode() {
    return _graph.hashCode();
  }
}
