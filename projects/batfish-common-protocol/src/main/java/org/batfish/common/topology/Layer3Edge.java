package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class Layer3Edge implements Comparable<Layer3Edge> {

  private static final String PROP_NODE1 = "node1";

  private static final String PROP_NODE2 = "node2";

  @JsonCreator
  private static @Nonnull Layer3Edge create(
      @JsonProperty(PROP_NODE1) Layer3Node node1, @JsonProperty(PROP_NODE2) Layer3Node node2) {
    return new Layer3Edge(requireNonNull(node1), requireNonNull(node2));
  }

  private final Layer3Node _node1;

  private final Layer3Node _node2;

  public Layer3Edge(@Nonnull Layer3Node node1, @Nonnull Layer3Node node2) {
    _node1 = node1;
    _node2 = node2;
  }

  public Layer3Edge(
      @Nonnull String node1Hostname,
      @Nonnull String node1InterfaceName,
      @Nonnull String node2Hostname,
      @Nonnull String node2InterfaceName) {
    this(
        new Layer3Node(node1Hostname, node1InterfaceName),
        new Layer3Node(node2Hostname, node2InterfaceName));
  }

  @Override
  public int compareTo(Layer3Edge o) {
    return Comparator.comparing(Layer3Edge::getNode1)
        .thenComparing(Layer3Edge::getNode2)
        .compare(this, o);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer3Edge)) {
      return false;
    }
    Layer3Edge rhs = (Layer3Edge) obj;
    return _node1.equals(rhs._node1) && _node2.equals(rhs._node2);
  }

  @JsonProperty(PROP_NODE1)
  public @Nonnull Layer3Node getNode1() {
    return _node1;
  }

  @JsonProperty(PROP_NODE2)
  public @Nonnull Layer3Node getNode2() {
    return _node2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_node1, _node2);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass()).add(PROP_NODE1, _node1).add(PROP_NODE2, _node2).toString();
  }
}
