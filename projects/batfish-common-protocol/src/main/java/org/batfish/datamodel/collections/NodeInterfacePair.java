package org.batfish.datamodel.collections;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Interface;

/** Combination of node name and interface name */
@ParametersAreNonnullByDefault
public final class NodeInterfacePair implements Serializable, Comparable<NodeInterfacePair> {
  // Soft values: let it be garbage collected in times of pressure.
  // Maximum size 2^18: Just some upper bound on cache size, well less than GiB.
  //   (~40 bytes reasonable entry size: 12+12+pointers, would be 10 MiB total).
  private static final LoadingCache<NodeInterfacePair, NodeInterfacePair> CACHE =
      CacheBuilder.newBuilder().softValues().maximumSize(1 << 18).build(CacheLoader.from(x -> x));

  private static final String PROP_HOSTNAME = "hostname";
  private static final String PROP_INTERFACE = "interface";

  @Nonnull private final String _hostname;
  @Nonnull private final String _interfaceName;

  @JsonCreator
  private static NodeInterfacePair jsonCreator(
      @Nullable @JsonProperty(PROP_HOSTNAME) String node,
      @Nullable @JsonProperty(PROP_INTERFACE) String iface) {
    checkArgument(node != null, "NodeInterfacePair missing %s", PROP_HOSTNAME);
    checkArgument(iface != null, "NodeInterfacePair missing %s", PROP_INTERFACE);
    return of(node, iface);
  }

  public static NodeInterfacePair of(String hostname, String interfaceName) {
    return CACHE.getUnchecked(new NodeInterfacePair(hostname, interfaceName));
  }

  public static NodeInterfacePair of(Interface iface) {
    return of(iface.getOwner().getHostname(), iface.getName());
  }

  @Deprecated
  public NodeInterfacePair(String hostname, String interfaceName) {
    _hostname = hostname;
    _interfaceName = interfaceName;
  }

  @Deprecated
  public NodeInterfacePair(Interface iface) {
    this(iface.getOwner().getNtpSourceInterface(), iface.getName());
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
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeInterfacePair)) {
      return false;
    }
    NodeInterfacePair that = (NodeInterfacePair) o;
    return _hostname.equals(that._hostname) && _interfaceName.equals(that._interfaceName);
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
