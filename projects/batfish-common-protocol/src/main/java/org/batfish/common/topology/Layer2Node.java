package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Range;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Layer2Node implements Serializable {
  private static final String PROP_HOSTNAME = "hostname";
  private static final String PROP_INTERFACE_NAME = "interfaceName";
  // For backwards compatibility
  private static final String PROP_SWITCHPORT_VLAN_ID = "switchportVlanId";
  private static final String PROP_SWITCHPORT_VLAN_RANGE = "switchportVlanRange";

  @JsonCreator
  private static @Nonnull Layer2Node create(
      @JsonProperty(PROP_HOSTNAME) @Nullable String hostname,
      @JsonProperty(PROP_INTERFACE_NAME) @Nullable String interfaceName,
      @JsonProperty(PROP_SWITCHPORT_VLAN_ID) Integer switchportVlanId,
      @JsonProperty(PROP_SWITCHPORT_VLAN_RANGE) Range<Integer> switchportVlanRange) {
    checkArgument(hostname != null, String.format("Missing %s", PROP_HOSTNAME));
    checkArgument(hostname != null, String.format("Missing %s", PROP_INTERFACE_NAME));
    @Nullable Range<Integer> range = null;
    if (switchportVlanRange != null) {
      range = switchportVlanRange;
    } else if (switchportVlanId != null) {
      range = Range.closed(switchportVlanId, switchportVlanId);
    }
    return new Layer2Node(hostname, interfaceName, range);
  }

  private final @Nonnull String _hostname;
  private final @Nonnull String _interfaceName;
  private final @Nullable Range<Integer> _switchportVlanRange;

  public Layer2Node(@Nonnull Layer1Node layer1Node, @Nullable Range<Integer> vlanRange) {
    this(layer1Node.getHostname(), layer1Node.getInterfaceName(), vlanRange);
  }

  public Layer2Node(
      @Nonnull String hostname, @Nonnull String interfaceName, @Nullable Range<Integer> vlanRange) {
    _hostname = hostname;
    _interfaceName = interfaceName;
    _switchportVlanRange = vlanRange;
  }

  public Layer2Node(@Nonnull String hostname, @Nonnull String interfaceName, int vlan) {
    this(hostname, interfaceName, Range.singleton(vlan));
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
        && Objects.equals(_switchportVlanRange, rhs._switchportVlanRange);
  }

  @JsonProperty(PROP_HOSTNAME)
  public @Nonnull String getHostname() {
    return _hostname;
  }

  @JsonProperty(PROP_INTERFACE_NAME)
  public @Nonnull String getInterfaceName() {
    return _interfaceName;
  }

  @JsonProperty(PROP_SWITCHPORT_VLAN_RANGE)
  @Nullable
  public Range<Integer> getSwitchportVlanRange() {
    return _switchportVlanRange;
  }

  @Override
  public int hashCode() {
    int ret = _hashCode;
    if (ret == 0) {
      ret = Objects.hash(_hostname, _interfaceName, _switchportVlanRange);
      _hashCode = ret;
    }
    return ret;
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .omitNullValues()
        .add(PROP_HOSTNAME, _hostname)
        .add(PROP_INTERFACE_NAME, _interfaceName)
        .add(PROP_SWITCHPORT_VLAN_RANGE, _switchportVlanRange)
        .toString();
  }

  private transient int _hashCode;
}
