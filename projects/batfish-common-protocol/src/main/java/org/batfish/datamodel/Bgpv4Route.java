package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.bgp.community.Community;

/**
 * A BGP Route. Captures attributes of both iBGP and eBGP routes.
 *
 * <p>For computational efficiency may contain additional attributes (that would otherwise be
 * present only in BGP advertisements on the wire)
 */
@ParametersAreNonnullByDefault
public final class Bgpv4Route extends BgpRoute<Bgpv4Route.Builder, Bgpv4Route> {

  /** Builder for {@link Bgpv4Route} */
  public static final class Builder extends BgpRoute.Builder<Builder, Bgpv4Route> {

    @Nonnull
    @Override
    public Builder newBuilder() {
      return new Builder();
    }

    @Nonnull
    @Override
    public Bgpv4Route build() {
      checkArgument(_originatorIp != null, "Missing %s", PROP_ORIGINATOR_IP);
      checkArgument(_originType != null, "Missing %s", PROP_ORIGIN_TYPE);
      checkArgument(_protocol != null, "Missing %s", PROP_PROTOCOL);
      return new Bgpv4Route(
          getNetwork(),
          getNextHopIp(),
          getAdmin(),
          _asPath,
          _communities,
          _discard,
          _localPreference,
          getMetric(),
          firstNonNull(_nextHopInterface, Route.UNSET_NEXT_HOP_INTERFACE),
          _originatorIp,
          _clusterList,
          _receivedFromRouteReflectorClient,
          _originType,
          _protocol,
          _receivedFromIp,
          _srcProtocol,
          getTag(),
          _weight,
          getNonForwarding(),
          getNonRouting());
    }

    @Override
    @Nonnull
    public Builder getThis() {
      return this;
    }
  }

  /* Cache the hashcode */
  private transient int _hashCode = 0;

  @JsonCreator
  private static Bgpv4Route jsonCreator(
      @Nullable @JsonProperty(PROP_NETWORK) Prefix network,
      @Nullable @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int admin,
      @Nullable @JsonProperty(PROP_AS_PATH) AsPath asPath,
      @Nullable @JsonProperty(PROP_COMMUNITIES) Set<Community> communities,
      @JsonProperty(PROP_DISCARD) boolean discard,
      @JsonProperty(PROP_LOCAL_PREFERENCE) long localPreference,
      @JsonProperty(PROP_METRIC) long med,
      @Nullable @JsonProperty(PROP_NEXT_HOP_INTERFACE) String nextHopInterface,
      @Nullable @JsonProperty(PROP_ORIGINATOR_IP) Ip originatorIp,
      @Nullable @JsonProperty(PROP_CLUSTER_LIST) Set<Long> clusterList,
      @JsonProperty(PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT)
          boolean receivedFromRouteReflectorClient,
      @Nullable @JsonProperty(PROP_ORIGIN_TYPE) OriginType originType,
      @Nullable @JsonProperty(PROP_PROTOCOL) RoutingProtocol protocol,
      @Nullable @JsonProperty(PROP_RECEIVED_FROM_IP) Ip receivedFromIp,
      @Nullable @JsonProperty(PROP_SRC_PROTOCOL) RoutingProtocol srcProtocol,
      @JsonProperty(PROP_TAG) long tag,
      @JsonProperty(PROP_WEIGHT) int weight) {
    checkArgument(originatorIp != null, "Missing %s", PROP_ORIGINATOR_IP);
    checkArgument(originType != null, "Missing %s", PROP_ORIGIN_TYPE);
    checkArgument(protocol != null, "Missing %s", PROP_PROTOCOL);
    return new Bgpv4Route(
        network,
        nextHopIp,
        admin,
        asPath,
        communities,
        discard,
        localPreference,
        med,
        firstNonNull(nextHopInterface, Route.UNSET_NEXT_HOP_INTERFACE),
        originatorIp,
        clusterList,
        receivedFromRouteReflectorClient,
        originType,
        protocol,
        receivedFromIp,
        srcProtocol,
        tag,
        weight,
        false,
        false);
  }

  private Bgpv4Route(
      @Nullable Prefix network,
      @Nullable Ip nextHopIp,
      int admin,
      @Nullable AsPath asPath,
      @Nullable Set<Community> communities,
      boolean discard,
      long localPreference,
      long med,
      String nextHopInterface,
      Ip originatorIp,
      @Nullable Set<Long> clusterList,
      boolean receivedFromRouteReflectorClient,
      OriginType originType,
      RoutingProtocol protocol,
      @Nullable Ip receivedFromIp,
      @Nullable RoutingProtocol srcProtocol,
      long tag,
      int weight,
      boolean nonForwarding,
      boolean nonRouting) {
    super(
        network,
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
        nonRouting);
  }

  public static Builder builder() {
    return new Builder();
  }

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
        .setLocalPreference(_localPreference)
        .setMetric(_med)
        .setNextHopInterface(_nextHopInterface)
        .setNextHopIp(_nextHopIp)
        .setOriginatorIp(_originatorIp)
        .setOriginType(_originType)
        .setProtocol(_protocol)
        .setReceivedFromIp(_receivedFromIp)
        .setReceivedFromRouteReflectorClient(_receivedFromRouteReflectorClient)
        .setSrcProtocol(_srcProtocol)
        .setTag(_tag)
        .setWeight(_weight);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Bgpv4Route)) {
      return false;
    }
    Bgpv4Route other = (Bgpv4Route) o;
    return _network.equals(other._network)
        && _nextHopIp.equals(other._nextHopIp)
        && _originatorIp.equals(other._originatorIp)
        && _nextHopInterface.equals(other._nextHopInterface)
        && Objects.equals(_receivedFromIp, other._receivedFromIp)
        // Things above this line are more likely to cause false earlier.
        && _admin == other._admin
        && _discard == other._discard
        && _localPreference == other._localPreference
        && _med == other._med
        && _originType == other._originType
        && _protocol == other._protocol
        && _receivedFromRouteReflectorClient == other._receivedFromRouteReflectorClient
        && _srcProtocol == other._srcProtocol
        && _tag == other._tag
        && _weight == other._weight
        && getNonRouting() == other.getNonRouting()
        && getNonForwarding() == other.getNonForwarding()
        && _asPath.equals(other._asPath)
        && _clusterList.equals(other._clusterList)
        && _communities.equals(other._communities);
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
      h = h * 31 + Long.hashCode(_localPreference);
      h = h * 31 + Long.hashCode(_med);
      h = h * 31 + _network.hashCode();
      h = h * 31 + _nextHopInterface.hashCode();
      h = h * 31 + _nextHopIp.hashCode();
      h = h * 31 + Boolean.hashCode(getNonForwarding());
      h = h * 31 + Boolean.hashCode(getNonRouting());
      h = h * 31 + _originatorIp.hashCode();
      h = h * 31 + _originType.ordinal();
      h = h * 31 + _protocol.ordinal();
      h = h * 31 + Objects.hashCode(_receivedFromIp);
      h = h * 31 + Boolean.hashCode(_receivedFromRouteReflectorClient);
      h = h * 31 + (_srcProtocol == null ? 0 : _srcProtocol.ordinal());
      h = h * 31 + Long.hashCode(_tag);
      h = h * 31 + _weight;

      _hashCode = h;
    }
    return h;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("_network", _network)
        .add("_admin", _admin)
        .add("_tag", _tag)
        .add("_asPath", _asPath)
        .add("_clusterList", _clusterList)
        .add("_communities", _communities)
        .add("_discard", _discard)
        .add("_localPreference", _localPreference)
        .add("_med", _med)
        .add("_nextHopInterface", _nextHopInterface)
        .add("_nextHopIp", _nextHopIp)
        .add("_originatorIp", _originatorIp)
        .add("_originType", _originType)
        .add("_protocol", _protocol)
        .add("_receivedFromIp", _receivedFromIp)
        .add("_receivedFromRouteReflectorClient", _receivedFromRouteReflectorClient)
        .add("_srcProtocol", _srcProtocol)
        .add("_weight", _weight)
        .toString();
  }
}
