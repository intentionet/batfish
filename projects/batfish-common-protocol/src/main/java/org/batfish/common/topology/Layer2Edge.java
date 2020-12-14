package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Range;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Layer2Edge {
  private static final String PROP_ENCAPSULATED_VLAN_ID = "encapsulatedVlanId";
  private static final String PROP_NODE1 = "node1";
  private static final String PROP_NODE2 = "node2";

  @JsonCreator
  private static @Nonnull Layer2Edge create(
      @JsonProperty(PROP_NODE1) Layer2Node node1,
      @JsonProperty(PROP_NODE2) Layer2Node node2,
      // PROP_ENCAPSULATED_VLAN_ID for backwards compatibility
      @Nullable @JsonProperty(PROP_ENCAPSULATED_VLAN_ID) Integer encapsulatedVlanId) {
    return new Layer2Edge(requireNonNull(node1), requireNonNull(node2));
  }

  private final Layer2Node _node1;
  private final Layer2Node _node2;

  public Layer2Edge(
      @Nonnull Layer1Node node1,
      @Nullable Range<Integer> node1SwitchportVlanId,
      @Nonnull Layer1Node node2,
      @Nullable Range<Integer> node2SwitchportVlanId) {
    this(
        new Layer2Node(node1, node1SwitchportVlanId), new Layer2Node(node2, node2SwitchportVlanId));
  }

  public Layer2Edge(@Nonnull Layer2Node node1, @Nonnull Layer2Node node2) {
    _node1 = node1;
    _node2 = node2;
  }

  public Layer2Edge(
      @Nonnull String node1Hostname,
      @Nonnull String node1InterfaceName,
      @Nullable Integer node1SwitchportVlanId,
      @Nonnull String node2Hostname,
      @Nonnull String node2InterfaceName,
      @Nullable Integer node2SwitchportVlanId) {
    this(
        new Layer2Node(
            node1Hostname,
            node1InterfaceName,
            node1SwitchportVlanId == null ? null : Range.singleton(node1SwitchportVlanId)),
        new Layer2Node(
            node2Hostname,
            node2InterfaceName,
            node2SwitchportVlanId == null ? null : Range.singleton(node2SwitchportVlanId)));
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
    return (_hashCode == rhs._hashCode || _hashCode == 0 || rhs._hashCode == 0)
        && _node1.equals(rhs._node1)
        && _node2.equals(rhs._node2);
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
    int ret = _hashCode;
    if (ret == 0) {
      ret = Objects.hash(_node1, _node2);
      _hashCode = ret;
    }
    return ret;
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .omitNullValues()
        .add(PROP_NODE1, _node1)
        .add(PROP_NODE2, _node2)
        .toString();
  }

  private transient int _hashCode;
}
