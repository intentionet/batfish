package org.batfish.datamodel.collections;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Interface;

/** Combination of node name and interface name */
@ParametersAreNonnullByDefault
public class NodeInterfacePair implements Serializable, Comparable<NodeInterfacePair> {

  private static final String PROP_HOSTNAME = "hostname";
  private static final String PROP_INTERFACE = "interface";
  private static final long serialVersionUID = 1L;

  @Nonnull private final String _hostname;
  @Nonnull private final String _interfaceName;

  @JsonCreator
  private static NodeInterfacePair create(
      @Nullable @JsonProperty(PROP_HOSTNAME) String node,
      @Nullable @JsonProperty(PROP_INTERFACE) String iface) {
    checkArgument(node != null, "Cannot create NodeInterfacePair with null node");
    checkArgument(iface != null, "Cannot create NodeInterfacePair with null interface");
    return new NodeInterfacePair(node, iface);
  }

  public NodeInterfacePair(String hostname, String interfaceName) {
    _hostname = hostname;
    _interfaceName = interfaceName;
  }

  public NodeInterfacePair(Interface iface) {
    this(iface.getOwner().getHostname(), iface.getName());
  }

  /** Return node name */
  @JsonProperty(PROP_HOSTNAME)
  @Nonnull
  public String getHostname() {
    return _hostname;
  }

  /** Return node interface name */
  @JsonProperty(PROP_INTERFACE)
  @Nonnull
  public String getInterface() {
    return _interfaceName;
  }

  @Override
  public String toString() {
    return _hostname + ":" + _interfaceName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeInterfacePair)) {
      return false;
    }
    NodeInterfacePair that = (NodeInterfacePair) o;
    return Objects.equals(_hostname, that._hostname)
        && Objects.equals(_interfaceName, that._interfaceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _interfaceName);
  }

  @Override
  public int compareTo(NodeInterfacePair other) {
    return Comparator.comparing(NodeInterfacePair::getHostname)
        .thenComparing(NodeInterfacePair::getInterface)
        .compare(this, other);
  }
}
