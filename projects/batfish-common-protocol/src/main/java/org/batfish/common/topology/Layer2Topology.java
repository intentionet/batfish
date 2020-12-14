package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.jgrapht.alg.util.UnionFind;

/** Tracks which interfaces are in the same layer 2 broadcast domain. */
@ParametersAreNonnullByDefault
public final class Layer2Topology {

  public static final Layer2Topology EMPTY = new Layer2Topology(ImmutableMap.of());
  private static final String PROP_REPRESENTATIVE_BY_NODE = "representativeByNode";

  public static final class Builder {
    private final Set<Layer2Node> _nodes = new HashSet<>();
    private final UnionFind<Layer2Node> _broadcastDomains = new UnionFind<>(ImmutableSet.of());

    private Builder() {}

    private void addNodeIfMissing(Layer2Node node) {
      if (_nodes.add(node)) {
        _broadcastDomains.addElement(node);
      }
    }

    public Builder addEdge(Layer2Edge edge) {
      Layer2Node node1 = edge.getNode1();
      Layer2Node node2 = edge.getNode2();
      addNodeIfMissing(node1);
      addNodeIfMissing(node2);
      _broadcastDomains.union(node1, node2);
      return this;
    }

    public Layer2Topology build() {
      return new Layer2Topology(
          _nodes.stream()
              .collect(ImmutableMap.toImmutableMap(Function.identity(), _broadcastDomains::find)));
    }
  }

  private static final class Layer2RepresentativeEntry {

    private static final String PROP_NODE = "node";
    private static final String PROP_REPRESENTATIVE = "representative";

    private final @Nonnull Layer2Node _node;
    private final @Nonnull Layer2Node _representative;

    @JsonCreator
    private static @Nonnull Layer2RepresentativeEntry create(
        @JsonProperty(PROP_NODE) @Nullable Layer2Node node,
        @JsonProperty(PROP_REPRESENTATIVE) @Nullable Layer2Node representative) {
      checkArgument(node != null, "Missing %s", PROP_NODE);
      checkArgument(representative != null, "Missing %s", PROP_REPRESENTATIVE);
      return new Layer2RepresentativeEntry(node, representative);
    }

    private Layer2RepresentativeEntry(Layer2Node node, Layer2Node representative) {
      _node = node;
      _representative = representative;
    }

    @JsonProperty(PROP_NODE)
    private @Nonnull Layer2Node getNode() {
      return _node;
    }

    @JsonProperty(PROP_REPRESENTATIVE)
    private @Nonnull Layer2Node getRepresentative() {
      return _representative;
    }
  }

  // node -> representative
  private final Map<Layer2Node, Layer2Node> _representativeByNode;

  public static Builder builder() {
    return new Builder();
  }

  @JsonCreator
  private static @Nonnull Layer2Topology create(
      @JsonProperty(PROP_REPRESENTATIVE_BY_NODE)
          List<Layer2RepresentativeEntry> representativeByNode) {
    return new Layer2Topology(
        firstNonNull(representativeByNode, ImmutableList.<Layer2RepresentativeEntry>of()).stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Layer2RepresentativeEntry::getNode,
                    Layer2RepresentativeEntry::getRepresentative)));
  }

  private Layer2Topology(Map<Layer2Node, Layer2Node> representativeByNode) {
    _representativeByNode = ImmutableMap.copyOf(representativeByNode);
  }

  public static @Nonnull Layer2Topology fromDomains(Collection<Set<Layer2Node>> domains) {
    return new Layer2Topology(
        domains.stream()
            .flatMap(
                domain -> {
                  if (domain.isEmpty()) {
                    return Stream.of();
                  }
                  Layer2Node repr =
                      domain.stream().max(Comparator.comparingInt(Layer2Node::hashCode)).get();
                  return domain.stream().map(node -> Maps.immutableEntry(node, repr));
                })
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue)));
  }

  @VisibleForTesting
  static @Nonnull Layer2Topology fromEdges(Set<Layer2Edge> edges) {
    Builder builder = builder();
    edges.forEach(builder::addEdge);
    return builder.build();
  }

  /**
   * Return the representative of the broadcast domain of {@code layer2Node}, or {@link
   * Optional#empty} if not represented in the layer-2 topology.
   */
  public @Nonnull Optional<Layer2Node> getBroadcastDomainRepresentative(Layer2Node layer2Node) {
    return Optional.ofNullable(_representativeByNode.get(layer2Node));
  }

  /**
   * Return the representative of the broadcast domain of the interface represented by {@code
   * hostname} and {@code interfaceName}, or {@link Optional#empty} if not represented in the
   * layer-2 topology.
   */
  public @Nonnull Optional<Layer2Node> getBroadcastDomainRepresentative(
      String hostname, String interfaceName) {
    return getBroadcastDomainRepresentative(new Layer2Node(hostname, interfaceName, null));
  }

  /** Convert a layer3 interface to a layer2 node. */
  private static @Nonnull Layer2Node layer2Node(String hostName, String iface) {
    return new Layer2Node(hostName, iface, null);
  }

  /** Return whether the two interfaces are in the same broadcast domain. */
  public boolean inSameBroadcastDomain(Layer2Node n1, Layer2Node n2) {
    Layer2Node r1 = _representativeByNode.get(n1);
    return r1 != null && r1.equals(_representativeByNode.get(n2));
  }

  /** Return whether the two interfaces are in the same broadcast domain. */
  public boolean inSameBroadcastDomain(NodeInterfacePair i1, NodeInterfacePair i2) {
    return inSameBroadcastDomain(
        layer2Node(i1.getHostname(), i1.getInterface()),
        layer2Node(i2.getHostname(), i2.getInterface()));
  }

  /** Return whether two non-switchport interfaces are in the same broadcast domain. */
  public boolean inSameBroadcastDomain(String host1, String iface1, String host2, String iface2) {
    return inSameBroadcastDomain(layer2Node(host1, iface1), layer2Node(host2, iface2));
  }

  @JsonIgnore
  public boolean isEmpty() {
    return _representativeByNode.isEmpty();
  }

  @JsonProperty(PROP_REPRESENTATIVE_BY_NODE)
  private @Nonnull List<Layer2RepresentativeEntry> getRepresentativeByNode() {
    return _representativeByNode.entrySet().stream()
        .map(e -> new Layer2RepresentativeEntry(e.getKey(), e.getValue()))
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer2Topology)) {
      return false;
    }
    return _representativeByNode.equals(((Layer2Topology) obj)._representativeByNode);
  }

  @Override
  public int hashCode() {
    return _representativeByNode.hashCode();
  }
}
