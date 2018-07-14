package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class Layer1Edge implements Comparable<Layer1Edge> {

  private static final String PROP_NODE1 = "node1";

  private static final String PROP_NODE2 = "node2";

  @JsonCreator
  private static @Nonnull Layer1Edge create(
      @JsonProperty(PROP_NODE1) Layer1Node node1, @JsonProperty(PROP_NODE2) Layer1Node node2) {
    return new Layer1Edge(requireNonNull(node1), requireNonNull(node2));
  }

  private final Layer1Node _node1;

  private final Layer1Node _node2;

  public Layer1Edge(@Nonnull Layer1Node node1, @Nonnull Layer1Node node2) {
    _node1 = node1;
    _node2 = node2;
  }

  public Layer1Edge(
      @Nonnull String node1Hostname,
      @Nonnull String node1InterfaceName,
      @Nonnull String node2Hostname,
      @Nonnull String node2InterfaceName) {
    this(
        new Layer1Node(node1Hostname, node1InterfaceName),
        new Layer1Node(node2Hostname, node2InterfaceName));
  }

  @Override
  public int compareTo(Layer1Edge o) {
    return Comparator.comparing(Layer1Edge::getNode1)
        .thenComparing(Layer1Edge::getNode2)
        .compare(this, o);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer1Edge)) {
      return false;
    }
    Layer1Edge rhs = (Layer1Edge) obj;
    return _node1.equals(rhs._node1) && _node2.equals(rhs._node2);
  }

  @JsonProperty(PROP_NODE1)
  public @Nonnull Layer1Node getNode1() {
    return _node1;
  }

  @JsonProperty(PROP_NODE2)
  public @Nonnull Layer1Node getNode2() {
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
