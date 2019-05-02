package org.batfish.datamodel.isis;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Topology;

/** A topology representing IS-IS sessions */
@ParametersAreNonnullByDefault
public final class IsisTopology {

  public static final IsisTopology EMPTY =
      new IsisTopology(
          NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build());

  /** Initialize the IS-IS topology as a directed graph. */
  public static @Nonnull IsisTopology initIsisTopology(
      Map<String, Configuration> configurations, Topology topology) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("IsisTopology.initIsisTopology").startActive()) {
      assert span != null; // avoid unused warning

      Set<IsisEdge> edges =
          topology.getEdges().stream()
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
      return new IsisTopology(graph);
    }
  }

  private final @Nonnull ImmutableNetwork<IsisNode, IsisEdge> _network;

  public IsisTopology(Network<IsisNode, IsisEdge> network) {
    _network = ImmutableNetwork.copyOf(network);
  }

  public @Nonnull ImmutableNetwork<IsisNode, IsisEdge> getNetwork() {
    return _network;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IsisTopology)) {
      return false;
    }
    return _network.equals(((IsisTopology) obj)._network);
  }

  @Override
  public int hashCode() {
    return _network.hashCode();
  }
}
