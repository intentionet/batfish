package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;

public class LocalRoute extends AbstractRoute {

  private static final String PROP_SOURCE_PREFIX_LENGTH = "sourcePrefixLength";

  private static final long serialVersionUID = 1L;

  private final String _nextHopInterface;

  private final int _sourcePrefixLength;

  public LocalRoute(@Nonnull InterfaceAddress interfaceAddress, String nextHopInterface) {
    this(
        new Prefix(interfaceAddress.getIp(), Prefix.MAX_PREFIX_LENGTH),
        nextHopInterface,
        interfaceAddress.getNetworkBits());
  }

  @JsonCreator
  private LocalRoute(
      @JsonProperty(PROP_NETWORK) @Nonnull Prefix network,
      @JsonProperty(PROP_NEXT_HOP_INTERFACE) String nextHopInterface,
      @JsonProperty(PROP_SOURCE_PREFIX_LENGTH) int sourcePrefixLength) {
    super(network);
    _nextHopInterface = firstNonNull(nextHopInterface, Route.UNSET_NEXT_HOP_INTERFACE);
    _sourcePrefixLength = sourcePrefixLength;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof LocalRoute)) {
      return false;
    }
    LocalRoute rhs = (LocalRoute) o;
    return _network.equals(rhs._network)
        && _nextHopInterface.equals(rhs._nextHopInterface)
        && _sourcePrefixLength == rhs._sourcePrefixLength;
  }

  @Override
  public int getAdministrativeCost() {
    return 0;
  }

  @Override
  public Long getMetric() {
    return 0L;
  }

  @Nonnull
  @JsonIgnore(false)
  @JsonProperty(PROP_NEXT_HOP_INTERFACE)
  @Override
  public String getNextHopInterface() {
    return _nextHopInterface;
  }

  @Nonnull
  @Override
  public Ip getNextHopIp() {
    return Route.UNSET_ROUTE_NEXT_HOP_IP;
  }

  @Override
  public RoutingProtocol getProtocol() {
    return RoutingProtocol.LOCAL;
  }

  @JsonProperty(PROP_SOURCE_PREFIX_LENGTH)
  public int getSourcePrefixLength() {
    return _sourcePrefixLength;
  }

  @Override
  public int getTag() {
    return Route.UNSET_ROUTE_TAG;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_network, _nextHopInterface, _sourcePrefixLength);
  }

  @Override
  protected String protocolRouteString() {
    return " sourcePrefixLength: " + _sourcePrefixLength;
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    return Integer.compare(_sourcePrefixLength, ((LocalRoute) rhs)._sourcePrefixLength);
  }
}
