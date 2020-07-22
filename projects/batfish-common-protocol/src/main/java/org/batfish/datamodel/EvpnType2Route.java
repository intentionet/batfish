package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.Community;

/** An EVPN type 2 route */
@ParametersAreNonnullByDefault
public final class EvpnType2Route extends EvpnRoute<EvpnType2Route.Builder, EvpnType2Route> {

  /** Builder for {@link EvpnType2Route} */
  @ParametersAreNonnullByDefault
  public static final class Builder
      extends EvpnRoute.Builder<EvpnType2Route.Builder, EvpnType2Route> {

    @Nullable private Ip _ip;
    @Nullable private MacAddress _macAddress;

    private Builder() {}

    @Nonnull
    @Override
    public Builder newBuilder() {
      return new Builder();
    }

    @Nonnull
    @Override
    public EvpnType2Route build() {
      checkArgument(_ip != null, "Missing %s", PROP_IP);
      checkArgument(_originatorIp != null, "Missing %s", PROP_ORIGINATOR_IP);
      checkArgument(_originType != null, "Missing %s", PROP_ORIGIN_TYPE);
      checkArgument(_protocol != null, "Missing %s", PROP_PROTOCOL);
      checkArgument(_routeDistinguisher != null, "Missing %s", PROP_ROUTE_DISTINGUISHER);
      return new EvpnType2Route(
          getAdmin(),
          _asPath,
          _clusterList,
          _communities,
          _discard,
          _ip,
          _localPreference,
          _macAddress,
          getMetric(),
          firstNonNull(_nextHopInterface, Route.UNSET_NEXT_HOP_INTERFACE),
          firstNonNull(getNextHopIp(), Route.UNSET_ROUTE_NEXT_HOP_IP),
          getNonForwarding(),
          getNonRouting(),
          _originatorIp,
          _originType,
          _protocol,
          _receivedFromIp,
          _receivedFromRouteReflectorClient,
          _routeDistinguisher,
          _srcProtocol,
          getTag(),
          _weight);
    }

    public Builder setIp(@Nonnull Ip ip) {
      _ip = ip;
      return this;
    }

    public Builder setMacAddress(@Nullable MacAddress macAddress) {
      _macAddress = macAddress;
      return this;
    }

    @Nullable
    public Ip getIp() {
      return _ip;
    }

    @Nullable
    public MacAddress getMacAddress() {
      return _macAddress;
    }

    @Override
    @Nonnull
    public Builder getThis() {
      return this;
    }
  }

  private static final String PROP_IP = "ip";
  private static final String PROP_MAC_ADDRESS = "macAddress";

  @Nonnull private final Ip _ip;
  @Nullable private final MacAddress _macAddress;
  /* Cache the hashcode */
  private transient int _hashCode = 0;

  @JsonCreator
  private static EvpnType2Route jsonCreator(
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int admin,
      @Nullable @JsonProperty(PROP_AS_PATH) AsPath asPath,
      @Nullable @JsonProperty(PROP_CLUSTER_LIST) Set<Long> clusterList,
      @Nullable @JsonProperty(PROP_COMMUNITIES) Set<Community> communities,
      @JsonProperty(PROP_DISCARD) boolean discard,
      @Nullable @JsonProperty(PROP_IP) Ip ip,
      @JsonProperty(PROP_LOCAL_PREFERENCE) long localPreference,
      @Nullable @JsonProperty(PROP_MAC_ADDRESS) MacAddress macAddress,
      @JsonProperty(PROP_METRIC) long med,
      @Nullable @JsonProperty(PROP_NEXT_HOP_INTERFACE) String nextHopInterface,
      @Nullable @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @Nullable @JsonProperty(PROP_ORIGINATOR_IP) Ip originatorIp,
      @Nullable @JsonProperty(PROP_ORIGIN_TYPE) OriginType originType,
      @Nullable @JsonProperty(PROP_PROTOCOL) RoutingProtocol protocol,
      @Nullable @JsonProperty(PROP_RECEIVED_FROM_IP) Ip receivedFromIp,
      @JsonProperty(PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT)
          boolean receivedFromRouteReflectorClient,
      @Nullable @JsonProperty(PROP_ROUTE_DISTINGUISHER) RouteDistinguisher routeDistinguisher,
      @Nullable @JsonProperty(PROP_SRC_PROTOCOL) RoutingProtocol srcProtocol,
      @JsonProperty(PROP_TAG) long tag,
      @JsonProperty(PROP_WEIGHT) int weight) {
    checkArgument(ip != null, "Missing %s", PROP_IP);
    checkArgument(originatorIp != null, "Missing %s", PROP_ORIGINATOR_IP);
    checkArgument(originType != null, "Missing %s", PROP_ORIGIN_TYPE);
    checkArgument(protocol != null, "Missing %s", PROP_PROTOCOL);
    checkArgument(routeDistinguisher != null, "Missing %s", PROP_ROUTE_DISTINGUISHER);
    return new EvpnType2Route(
        admin,
        firstNonNull(asPath, AsPath.empty()),
        firstNonNull(clusterList, ImmutableSet.of()),
        firstNonNull(communities, ImmutableSet.of()),
        discard,
        ip,
        localPreference,
        macAddress,
        med,
        firstNonNull(nextHopInterface, Route.UNSET_NEXT_HOP_INTERFACE),
        firstNonNull(nextHopIp, Route.UNSET_ROUTE_NEXT_HOP_IP),
        false,
        false,
        originatorIp,
        originType,
        protocol,
        receivedFromIp,
        receivedFromRouteReflectorClient,
        routeDistinguisher,
        srcProtocol,
        tag,
        weight);
  }

  private EvpnType2Route(
      int admin,
      AsPath asPath,
      Set<Long> clusterList,
      Set<Community> communities,
      boolean discard,
      Ip ip,
      long localPreference,
      @Nullable MacAddress macAddress,
      long med,
      String nextHopInterface,
      Ip nextHopIp,
      boolean nonForwarding,
      boolean nonRouting,
      Ip originatorIp,
      OriginType originType,
      RoutingProtocol protocol,
      @Nullable Ip receivedFromIp,
      boolean receivedFromRouteReflectorClient,
      RouteDistinguisher routeDistinguisher,
      @Nullable RoutingProtocol srcProtocol,
      long tag,
      int weight) {
    super(
        ip.toPrefix(),
        nextHopIp,
        admin,
        asPath,
        communities,
        discard,
        localPreference,
        med,
        nextHopInterface,
        originatorIp,
        clusterList,
        receivedFromRouteReflectorClient,
        originType,
        protocol,
        receivedFromIp,
        srcProtocol,
        tag,
        weight,
        nonForwarding,
        nonRouting,
        routeDistinguisher);
    _ip = ip;
    _macAddress = macAddress;
  }

  @Nullable
  public MacAddress getMacAddress() {
    return _macAddress;
  }

  @Nonnull
  public Ip getIp() {
    return _ip;
  }

  public static Builder builder() {
    return new Builder();
  }

  // value of network is ignored during deserialization
  @JsonProperty(PROP_NETWORK)
  private void setNetwork(@Nullable Prefix network) {}

  /////// Keep #toBuilder, #equals, and #hashCode in sync ////////

  @Override
  public Builder toBuilder() {
    return builder()
        .setNetwork(getNetwork())
        .setAdmin(getAdministrativeCost())
        .setNonRouting(getNonRouting())
        .setNonForwarding(getNonForwarding())
        .setAsPath(_asPath)
        .setClusterList(_clusterList)
        .setCommunities(_communities)
        .setDiscard(_discard)
        .setIp(_ip)
        .setLocalPreference(_localPreference)
        .setMacAddress(_macAddress)
        .setMetric(_med)
        .setNextHopInterface(_nextHopInterface)
        .setNextHopIp(_nextHopIp)
        .setOriginatorIp(_originatorIp)
        .setOriginType(_originType)
        .setProtocol(_protocol)
        .setReceivedFromIp(_receivedFromIp)
        .setReceivedFromRouteReflectorClient(_receivedFromRouteReflectorClient)
        .setRouteDistinguisher(_routeDistinguisher)
        .setSrcProtocol(_srcProtocol)
        .setTag(_tag)
        .setWeight(_weight);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EvpnType2Route)) {
      return false;
    }
    EvpnType2Route other = (EvpnType2Route) o;
    return Objects.equals(_network, other._network)
        && _admin == other._admin
        && getNonRouting() == other.getNonRouting()
        && getNonForwarding() == other.getNonForwarding()
        && _discard == other._discard
        && Objects.equals(_ip, other._ip)
        && _localPreference == other._localPreference
        && Objects.equals(_macAddress, other._macAddress)
        && _med == other._med
        && _receivedFromRouteReflectorClient == other._receivedFromRouteReflectorClient
        && _weight == other._weight
        && Objects.equals(_asPath, other._asPath)
        && Objects.equals(_clusterList, other._clusterList)
        && Objects.equals(_communities, other._communities)
        && _nextHopInterface.equals(other._nextHopInterface)
        && Objects.equals(_nextHopIp, other._nextHopIp)
        && Objects.equals(_originatorIp, other._originatorIp)
        && _originType == other._originType
        && _protocol == other._protocol
        && Objects.equals(_receivedFromIp, other._receivedFromIp)
        && Objects.equals(_routeDistinguisher, other._routeDistinguisher)
        && _srcProtocol == other._srcProtocol
        && _tag == other._tag;
  }

  @Override
  public int hashCode() {
    int h = _hashCode;
    if (h == 0) {
      h = _admin;
      h = h * 31 + _asPath.hashCode();
      h = h * 31 + _clusterList.hashCode();
      h = h * 31 + _communities.hashCode();
      h = h * 31 + Boolean.hashCode(_discard);
      h = h * 31 + _ip.hashCode();
      h = h * 31 + Long.hashCode(_localPreference);
      h = h * 31 + Objects.hashCode(_macAddress);
      h = h * 31 + Long.hashCode(_med);
      h = h * 31 + _network.hashCode();
      h = h * 31 + _nextHopInterface.hashCode();
      h = h * 31 + _nextHopIp.hashCode();
      h = h * 31 + _originatorIp.hashCode();
      h = h * 31 + _originType.ordinal();
      h = h * 31 + _protocol.ordinal();
      h = h * 31 + Objects.hashCode(_receivedFromIp);
      h = h * 31 + Boolean.hashCode(_receivedFromRouteReflectorClient);
      h = h * 31 + _routeDistinguisher.hashCode();
      h = h * 31 + (_srcProtocol == null ? 0 : _srcProtocol.ordinal());
      h = h * 31 + Long.hashCode(_tag);
      h = h * 31 + _weight;

      _hashCode = h;
    }
    return h;
  }
}
