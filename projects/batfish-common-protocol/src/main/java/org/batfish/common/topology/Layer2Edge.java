package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Layer2Edge implements Comparable<Layer2Edge> {

  private static final String PROP_ENCAPSULATED_VLAN_ID = "encapsulatedVlanId";

  private static final String PROP_NODE1 = "node1";

  private static final String PROP_NODE2 = "node2";

  @JsonCreator
  private static @Nonnull Layer2Edge create(
      @JsonProperty(PROP_NODE1) Layer2Node node1,
      @JsonProperty(PROP_NODE2) Layer2Node node2,
      @JsonProperty(PROP_ENCAPSULATED_VLAN_ID) Integer encapsulatedVlanId) {
    return new Layer2Edge(requireNonNull(node1), requireNonNull(node2), encapsulatedVlanId);
  }

  private final Integer _encapsulatedVlanId;

  private final Layer2Node _node1;

  private final Layer2Node _node2;

  public Layer2Edge(
      @Nonnull Layer1Node node1,
      @Nullable Integer node1VlanId,
      @Nonnull Layer1Node node2,
      @Nullable Integer node2VlanId,
      @Nullable Integer encapsulatedVlanId) {
    this(
        new Layer2Node(node1, node1VlanId), new Layer2Node(node2, node2VlanId), encapsulatedVlanId);
  }

  public Layer2Edge(
      @Nonnull Layer2Node node1, @Nonnull Layer2Node node2, @Nullable Integer encapsulatedVlanId) {
    _node1 = node1;
    _node2 = node2;
    _encapsulatedVlanId = encapsulatedVlanId;
  }

  public Layer2Edge(
      @Nonnull String node1Hostname,
      @Nonnull String node1InterfaceName,
      @Nullable Integer node1VlanId,
      @Nonnull String node2Hostname,
      @Nonnull String node2InterfaceName,
      @Nullable Integer node2VlanId,
      @Nullable Integer encapsulatedVlanId) {
    this(
        new Layer2Node(node1Hostname, node1InterfaceName, node1VlanId),
        new Layer2Node(node2Hostname, node2InterfaceName, node2VlanId),
        encapsulatedVlanId);
  }

  @Override
  public int compareTo(Layer2Edge o) {
    return Comparator.comparing(Layer2Edge::getNode1)
        .thenComparing(Layer2Edge::getNode2)
        .thenComparing(Layer2Edge::getEncapsulatedVlanId, nullsFirst(naturalOrder()))
        .compare(this, o);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer2Edge)) {
      return false;
    }
    Layer2Edge rhs = (Layer2Edge) obj;
    return _node1.equals(rhs._node1)
        && _node2.equals(rhs._node2)
        && Objects.equals(_encapsulatedVlanId, rhs._encapsulatedVlanId);
  }

  @JsonProperty(PROP_ENCAPSULATED_VLAN_ID)
  public @Nullable Integer getEncapsulatedVlanId() {
    return _encapsulatedVlanId;
  }

  @JsonProperty(PROP_NODE1)
  public @Nonnull Layer2Node getNode1() {
    return _node1;
  }

  @JsonProperty(PROP_NODE2)
  public @Nonnull Layer2Node getNode2() {
    return _node2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_node1, _node2, _encapsulatedVlanId);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .omitNullValues()
        .add(PROP_NODE1, _node1)
        .add(PROP_NODE2, _node2)
        .add(PROP_ENCAPSULATED_VLAN_ID, _encapsulatedVlanId)
        .toString();
  }
}
