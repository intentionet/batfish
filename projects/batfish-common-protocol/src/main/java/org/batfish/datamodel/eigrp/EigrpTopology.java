package org.batfish.datamodel.eigrp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Control plane representation of EIGRP connectivity. Vertices are {@link EigrpNeighborConfigId}s
 * and edges are {@link EigrpEdge}s.
 */
@ParametersAreNonnullByDefault
public final class EigrpTopology {

  private static final String PROP_EDGES = "edges";
  private static final String PROP_NODES = "nodes";

  public static final EigrpTopology EMPTY =
      new EigrpTopology(
          NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build());

  @JsonCreator
  private static @Nonnull EigrpTopology create(
      @JsonProperty(PROP_NODES) @Nullable Set<EigrpNeighborConfigId> nodes,
      @JsonProperty(PROP_EDGES) @Nullable Set<EigrpEdge> edges) {
    MutableNetwork<EigrpNeighborConfigId, EigrpEdge> network =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    if (nodes != null) {
      nodes.forEach(network::addNode);
    }
    if (edges != null) {
      edges.forEach(edge -> network.addEdge(edge.getNode1(), edge.getNode2(), edge));
    }
    return new EigrpTopology(network);
  }

  private final @Nonnull Network<EigrpNeighborConfigId, EigrpEdge> _network;

  public EigrpTopology(Network<EigrpNeighborConfigId, EigrpEdge> network) {
    _network = ImmutableNetwork.copyOf(network);
  }

  @JsonIgnore
  public @Nonnull Network<EigrpNeighborConfigId, EigrpEdge> getNetwork() {
    return _network;
  }

  @JsonProperty(PROP_EDGES)
  private @Nonnull Set<EigrpEdge> getEdges() {
    return _network.edges();
  }

  @JsonProperty(PROP_NODES)
  private @Nonnull Set<EigrpNeighborConfigId> getNodes() {
    return _network.nodes();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof EigrpTopology)) {
      return false;
    }
    return _network.equals(((EigrpTopology) obj)._network);
  }

  @Override
  public int hashCode() {
    return _network.hashCode();
  }
}
