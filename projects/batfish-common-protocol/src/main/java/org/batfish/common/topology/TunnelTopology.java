package org.batfish.common.topology;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * Topology for containing edges between generic tunnels (e.g., GRE, IP-in-IP) that do not require
 * complex compatibility checks (but <em>do</em> require reachability to be established). Does not
 * include IPSec tunnel edges.
 *
 * <p>This graph is undirected.
 */
@ParametersAreNonnullByDefault
public final class TunnelTopology {
  public static final TunnelTopology EMPTY =
      new TunnelTopology(GraphBuilder.undirected().allowsSelfLoops(false).build());

  @Nonnull private Graph<NodeInterfacePair> _graph;

  private TunnelTopology(Graph<NodeInterfacePair> graph) {
    _graph = ImmutableGraph.copyOf(graph);
  }

  @Nonnull
  public Graph<NodeInterfacePair> getGraph() {
    return _graph;
  }

  /** Represent this topology as a set of layer 3 {@link Edge edges}. */
  public Set<Edge> asEdgeSet() {
    return _graph.edges().stream()
        .map(endpointPair -> new Edge(endpointPair.nodeU(), endpointPair.nodeV()))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TunnelTopology)) {
      return false;
    }
    TunnelTopology that = (TunnelTopology) o;
    return _graph.equals(that._graph);
  }

  @Override
  public int hashCode() {
    return _graph.hashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    @Nonnull private MutableGraph<NodeInterfacePair> _graph;

    private Builder() {
      _graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    }

    /** Add an edge to the topology */
    public Builder add(NodeInterfacePair nodeU, NodeInterfacePair nodeV) {
      _graph.putEdge(nodeU, nodeV);
      return this;
    }

    public TunnelTopology build() {
      return new TunnelTopology(_graph);
    }
  }
}
