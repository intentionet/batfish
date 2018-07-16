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

public final class Layer2Node implements Comparable<Layer2Node> {

  private static final String PROP_HOSTNAME = "hostname";

  private static final String PROP_INTERFACE_NAME = "interfaceName";

  private static final String PROP_VLAN_ID = "vlanId";

  @JsonCreator
  private static @Nonnull Layer2Node create(
      @JsonProperty(PROP_HOSTNAME) String hostname,
      @JsonProperty(PROP_INTERFACE_NAME) String interfaceName,
      @JsonProperty(PROP_VLAN_ID) Integer vlanId) {
    return new Layer2Node(requireNonNull(hostname), requireNonNull(interfaceName), vlanId);
  }

  private final String _hostname;

  private final String _interfaceName;

  private final Integer _vlanId;

  public Layer2Node(@Nonnull Layer1Node layer1Node, @Nullable Integer vlanId) {
    _hostname = layer1Node.getHostname();
    _interfaceName = layer1Node.getInterfaceName();
    _vlanId = vlanId;
  }

  public Layer2Node(
      @Nonnull String hostname, @Nonnull String interfaceName, @Nullable Integer vlanId) {
    _hostname = hostname;
    _interfaceName = interfaceName;
    _vlanId = vlanId;
  }

  @Override
  public int compareTo(Layer2Node o) {
    return Comparator.comparing(Layer2Node::getHostname)
        .thenComparing(Layer2Node::getInterfaceName)
        .thenComparing(Layer2Node::getVlanId, nullsFirst(naturalOrder()))
        .compare(this, o);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer2Node)) {
      return false;
    }
    Layer2Node rhs = (Layer2Node) obj;
    return _hostname.equals(rhs._hostname)
        && _interfaceName.equals(rhs._interfaceName)
        && Objects.equals(_vlanId, rhs._vlanId);
  }

  @JsonProperty(PROP_HOSTNAME)
  public @Nonnull String getHostname() {
    return _hostname;
  }

  @JsonProperty(PROP_INTERFACE_NAME)
  public @Nonnull String getInterfaceName() {
    return _interfaceName;
  }

  @JsonProperty(PROP_VLAN_ID)
  public @Nullable Integer getVlanId() {
    return _vlanId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _interfaceName, _vlanId);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .omitNullValues()
        .add(PROP_HOSTNAME, _hostname)
        .add(PROP_INTERFACE_NAME, _interfaceName)
        .add(PROP_VLAN_ID, _vlanId)
        .toString();
  }
}
