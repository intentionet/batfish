package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class Layer3Node implements Comparable<Layer3Node> {

  private static final String PROP_HOSTNAME = "hostname";

  private static final String PROP_INTERFACE_NAME = "interfaceName";

  @JsonCreator
  private static @Nonnull Layer3Node create(
      @JsonProperty(PROP_HOSTNAME) String hostname,
      @JsonProperty(PROP_INTERFACE_NAME) String interfaceName) {
    return new Layer3Node(requireNonNull(hostname), requireNonNull(interfaceName));
  }

  private final String _hostname;

  private final String _interfaceName;

  public Layer3Node(@Nonnull String hostname, @Nonnull String interfaceName) {
    _hostname = hostname;
    _interfaceName = interfaceName;
  }

  @Override
  public int compareTo(Layer3Node o) {
    return Comparator.comparing(Layer3Node::getHostname)
        .thenComparing(Layer3Node::getInterfaceName)
        .compare(this, o);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer3Node)) {
      return false;
    }
    Layer3Node rhs = (Layer3Node) obj;
    return _hostname.equals(rhs._hostname) && _interfaceName.equals(rhs._interfaceName);
  }

  @JsonProperty(PROP_HOSTNAME)
  public @Nonnull String getHostname() {
    return _hostname;
  }

  @JsonProperty(PROP_INTERFACE_NAME)
  public @Nonnull String getInterfaceName() {
    return _interfaceName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _interfaceName);
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .add(PROP_HOSTNAME, _hostname)
        .add(PROP_INTERFACE_NAME, _interfaceName)
        .toString();
  }
}
