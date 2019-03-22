package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.util.CommonUtil;

/**
 * A BGP Route. Captures attributes of both iBGP and eBGP routes.
 *
 * <p>For computational efficiency may contain additional attributes (that would otherwise be
 * present only in BGP advertisements on the wire)
 */
@ParametersAreNonnullByDefault
public final class BgpRoute extends AbstractRoute {

  /** Builder for {@link BgpRoute} */
  @ParametersAreNonnullByDefault
  public static final class Builder extends AbstractRouteBuilder<Builder, BgpRoute> {

    @Nonnull private AsPath _asPath;
    @Nonnull private ImmutableSortedSet.Builder<Long> _clusterList;
    @Nonnull private SortedSet<Long> _communities;
    private boolean _discard;
    private long _localPreference;
    @Nullable private Ip _originatorIp;
    @Nullable private OriginType _originType;
    @Nullable private RoutingProtocol _protocol;
    @Nullable private Ip _receivedFromIp;
    private boolean _receivedFromRouteReflectorClient;
    @Nullable private RoutingProtocol _srcProtocol;
    private int _weight;

    public Builder() {
      _asPath = AsPath.empty();
      _communities = new TreeSet<>();
      _clusterList = new ImmutableSortedSet.Builder<>(Ordering.natural());
    }

    @Override
    public BgpRoute build() {
      checkArgument(_originatorIp != null, "Missing %s", PROP_ORIGINATOR_IP);
      checkArgument(_originType != null, "Missing %s", PROP_ORIGIN_TYPE);
      checkArgument(_protocol != null, "Missing %s", PROP_PROTOCOL);
      return new BgpRoute(
          getNetwork(),
          getNextHopIp(),
          getAdmin(),
          _asPath,
          _communities,
          _discard,
          _localPreference,
          getMetric(),
          _originatorIp,
          _clusterList.build(),
          _receivedFromRouteReflectorClient,
          _originType,
          _protocol,
          _receivedFromIp,
          _srcProtocol,
          _weight,
          getNonForwarding(),
          getNonRouting());
    }

    @Nonnull
    public AsPath getAsPath() {
      return _asPath;
    }

    @Nonnull
    public SortedSet<Long> getClusterList() {
      return _clusterList.build();
    }

    @Nonnull
    public SortedSet<Long> getCommunities() {
      return _communities;
    }

    public long getLocalPreference() {
      return _localPreference;
    }

    @Nullable
    public Ip getOriginatorIp() {
      return _originatorIp;
    }

    @Nullable
    public OriginType getOriginType() {
      return _originType;
    }

    @Nullable
    public RoutingProtocol getProtocol() {
      return _protocol;
    }

    @Override
    @Nonnull
    protected Builder getThis() {
      return this;
    }

    public int getWeight() {
      return _weight;
    }

    public Builder setAsPath(AsPath asPath) {
      _asPath = asPath;
      return getThis();
    }

    /** Overwrite the clusterList attribute */
    public Builder setClusterList(Set<Long> clusterList) {
      _clusterList = new ImmutableSortedSet.Builder<>(Ordering.natural());
      _clusterList.addAll(clusterList);
      return getThis();
    }

    /** Add to the cluster list attribute */
    public Builder addClusterList(Set<Long> clusterList) {
      _clusterList.addAll(clusterList);
      return getThis();
    }

    /** Add to the cluster list attribute */
    public Builder addToClusterList(Long cluster) {
      _clusterList.add(cluster);
      return getThis();
    }

    /** Overwrite communities */
    public Builder setCommunities(Set<Long> communities) {
      _communities = new TreeSet<>();
      _communities.addAll(communities);
      return getThis();
    }

    /** Add communities */
    public Builder addCommunities(Set<Long> communities) {
      _communities.addAll(communities);
      return getThis();
    }

    /** Add a single community */
    public Builder addCommunity(Long community) {
      _communities.add(community);
      return getThis();
    }

    /** Add communities */
    public Builder removeCommunities(Set<Long> communities) {
      _communities.removeAll(communities);
      return getThis();
    }

    public Builder setDiscard(boolean discard) {
      _discard = discard;
      return getThis();
    }

    public Builder setLocalPreference(long localPreference) {
      _localPreference = localPreference;
      return getThis();
    }

    public Builder setOriginatorIp(Ip originatorIp) {
      _originatorIp = originatorIp;
      return getThis();
    }

    public Builder setOriginType(OriginType originType) {
      _originType = originType;
      return getThis();
    }

    public Builder setProtocol(RoutingProtocol protocol) {
      _protocol = protocol;
      return getThis();
    }

    public Builder setReceivedFromIp(@Nullable Ip receivedFromIp) {
      _receivedFromIp = receivedFromIp;
      return getThis();
    }

    public Builder setReceivedFromRouteReflectorClient(boolean receivedFromRouteReflectorClient) {
      _receivedFromRouteReflectorClient = receivedFromRouteReflectorClient;
      return getThis();
    }

    public Builder setSrcProtocol(@Nullable RoutingProtocol srcProtocol) {
      _srcProtocol = srcProtocol;
      return getThis();
    }

    public Builder setWeight(int weight) {
      _weight = weight;
      return getThis();
    }
  }

  /** Default local preference for a BGP route if one is not set explicitly */
  public static final long DEFAULT_LOCAL_PREFERENCE = 100L;

  public static final String PROP_AS_PATH = "asPath";

  private static final String PROP_CLUSTER_LIST = "clusterList";

  public static final String PROP_COMMUNITIES = "communities";

  private static final String PROP_DISCARD = "discard";

  public static final String PROP_LOCAL_PREFERENCE = "localPreference";

  private static final String PROP_ORIGIN_TYPE = "originType";

  private static final String PROP_ORIGINATOR_IP = "originatorIp";

  private static final String PROP_RECEIVED_FROM_IP = "receivedFromIp";

  private static final String PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT =
      "receivedFromRouteReflectorClient";

  private static final String PROP_SRC_PROTOCOL = "srcProtocol";

  private static final String PROP_WEIGHT = "weight";

  private static final Comparator<BgpRoute> COMPARATOR =
      Comparator.comparing(BgpRoute::getAsPath)
          .thenComparing(BgpRoute::getClusterList, CommonUtil::compareCollection)
          .thenComparing(BgpRoute::getCommunities, CommonUtil::compareCollection)
          .thenComparing(BgpRoute::getDiscard)
          .thenComparing(BgpRoute::getLocalPreference)
          .thenComparing(BgpRoute::getOriginType)
          .thenComparing(BgpRoute::getOriginatorIp)
          .thenComparing(BgpRoute::getReceivedFromIp)
          .thenComparing(BgpRoute::getReceivedFromRouteReflectorClient)
          .thenComparing(BgpRoute::getSrcProtocol)
          .thenComparing(BgpRoute::getWeight);

  private static final long serialVersionUID = 1L;

  @Nonnull private final AsPath _asPath;
  @Nonnull private final SortedSet<Long> _clusterList;
  @Nonnull private final SortedSet<Long> _communities;
  private final boolean _discard;
  private final long _localPreference;
  private final long _med;
  @Nonnull private final Ip _nextHopIp;
  @Nonnull private final Ip _originatorIp;
  @Nonnull private final OriginType _originType;
  @Nonnull private final RoutingProtocol _protocol;
  @Nullable private final Ip _receivedFromIp;
  private final boolean _receivedFromRouteReflectorClient;
  @Nullable private final RoutingProtocol _srcProtocol;
  /* NOTE: Cisco-only attribute */
  private final int _weight;
  /* Cache the hashcode */
  private transient volatile int _hashCode = 0;

  @JsonCreator
  private static BgpRoute jsonCreator(
      @Nullable @JsonProperty(PROP_NETWORK) Prefix network,
      @Nullable @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int admin,
      @Nullable @JsonProperty(PROP_AS_PATH) AsPath asPath,
      @Nullable @JsonProperty(PROP_COMMUNITIES) SortedSet<Long> communities,
      @JsonProperty(PROP_DISCARD) boolean discard,
      @JsonProperty(PROP_LOCAL_PREFERENCE) long localPreference,
      @JsonProperty(PROP_METRIC) long med,
      @Nullable @JsonProperty(PROP_ORIGINATOR_IP) Ip originatorIp,
      @Nullable @JsonProperty(PROP_CLUSTER_LIST) SortedSet<Long> clusterList,
      @JsonProperty(PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT)
          boolean receivedFromRouteReflectorClient,
      @Nullable @JsonProperty(PROP_ORIGIN_TYPE) OriginType originType,
      @Nullable @JsonProperty(PROP_PROTOCOL) RoutingProtocol protocol,
      @Nullable @JsonProperty(PROP_RECEIVED_FROM_IP) Ip receivedFromIp,
      @Nullable @JsonProperty(PROP_SRC_PROTOCOL) RoutingProtocol srcProtocol,
      @JsonProperty(PROP_WEIGHT) int weight) {
    checkArgument(originatorIp != null, "Missing %s", PROP_ORIGINATOR_IP);
    checkArgument(originType != null, "Missing %s", PROP_ORIGIN_TYPE);
    checkArgument(protocol != null, "Missing %s", PROP_PROTOCOL);
    return new BgpRoute(
        network,
        nextHopIp,
        admin,
        asPath,
        communities,
        discard,
        localPreference,
        med,
        originatorIp,
        clusterList,
        receivedFromRouteReflectorClient,
        originType,
        protocol,
        receivedFromIp,
        srcProtocol,
        weight,
        false,
        false);
  }

  private BgpRoute(
      @Nullable Prefix network,
      @Nullable Ip nextHopIp,
      int admin,
      @Nullable AsPath asPath,
      @Nullable SortedSet<Long> communities,
      boolean discard,
      long localPreference,
      long med,
      Ip originatorIp,
      @Nullable SortedSet<Long> clusterList,
      boolean receivedFromRouteReflectorClient,
      OriginType originType,
      RoutingProtocol protocol,
      @Nullable Ip receivedFromIp,
      @Nullable RoutingProtocol srcProtocol,
      int weight,
      boolean nonForwarding,
      boolean nonRouting) {
    super(network, admin, nonRouting, nonForwarding);
    checkArgument(
        protocol == RoutingProtocol.BGP
            || protocol == RoutingProtocol.IBGP
            || protocol == RoutingProtocol.AGGREGATE,
        "Invalid BgpRoute protocol");
    _asPath = firstNonNull(asPath, AsPath.empty());
    _clusterList =
        clusterList == null ? ImmutableSortedSet.of() : ImmutableSortedSet.copyOf(clusterList);
    _communities =
        communities == null ? ImmutableSortedSet.of() : ImmutableSortedSet.copyOf(communities);
    _discard = discard;
    _localPreference = localPreference;
    _med = med;
    _nextHopIp = firstNonNull(nextHopIp, Route.UNSET_ROUTE_NEXT_HOP_IP);
    _originatorIp = originatorIp;
    _originType = originType;
    _protocol = protocol;
    _receivedFromIp = receivedFromIp;
    _receivedFromRouteReflectorClient = receivedFromRouteReflectorClient;
    _srcProtocol = srcProtocol;
    _weight = weight;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BgpRoute)) {
      return false;
    }
    BgpRoute other = (BgpRoute) o;
    return Objects.equals(_network, other._network)
        && _admin == other._admin
        && getNonRouting() == other.getNonRouting()
        && getNonForwarding() == other.getNonForwarding()
        && _discard == other._discard
        && _localPreference == other._localPreference
        && _med == other._med
        && _receivedFromRouteReflectorClient == other._receivedFromRouteReflectorClient
        && _weight == other._weight
        && Objects.equals(_asPath, other._asPath)
        && Objects.equals(_clusterList, other._clusterList)
        && Objects.equals(_communities, other._communities)
        && Objects.equals(_nextHopIp, other._nextHopIp)
        && Objects.equals(_originatorIp, other._originatorIp)
        && _originType == other._originType
        && _protocol == other._protocol
        && Objects.equals(_receivedFromIp, other._receivedFromIp)
        && _srcProtocol == other._srcProtocol;
  }

  @Override
  public int hashCode() {
    if (_hashCode != 0) {
      return _hashCode;
    }
    _hashCode =
        Objects.hash(
            _admin,
            _asPath,
            _clusterList,
            _communities,
            _discard,
            _localPreference,
            _med,
            _network,
            _nextHopIp,
            _originatorIp,
            _originType.ordinal(),
            _protocol.ordinal(),
            _receivedFromIp,
            _receivedFromRouteReflectorClient,
            _srcProtocol == null ? 0 : _srcProtocol.ordinal(),
            _weight);
    return _hashCode;
  }

  @Nonnull
  @JsonProperty(PROP_AS_PATH)
  public AsPath getAsPath() {
    return _asPath;
  }

  @Nonnull
  @JsonProperty(PROP_CLUSTER_LIST)
  public SortedSet<Long> getClusterList() {
    return _clusterList;
  }

  @Nonnull
  @JsonProperty(PROP_COMMUNITIES)
  public SortedSet<Long> getCommunities() {
    return _communities;
  }

  @JsonProperty(PROP_DISCARD)
  public boolean getDiscard() {
    return _discard;
  }

  @JsonProperty(PROP_LOCAL_PREFERENCE)
  public long getLocalPreference() {
    return _localPreference;
  }

  @JsonIgnore(false)
  @JsonProperty(PROP_METRIC)
  @Override
  public Long getMetric() {
    return _med;
  }

  @Nonnull
  @Override
  public String getNextHopInterface() {
    return Route.UNSET_NEXT_HOP_INTERFACE;
  }

  @Nonnull
  @JsonIgnore(false)
  @JsonProperty(PROP_NEXT_HOP_IP)
  @Override
  public Ip getNextHopIp() {
    return _nextHopIp;
  }

  @Nonnull
  @JsonProperty(PROP_ORIGINATOR_IP)
  public Ip getOriginatorIp() {
    return _originatorIp;
  }

  @Nonnull
  @JsonProperty(PROP_ORIGIN_TYPE)
  public OriginType getOriginType() {
    return _originType;
  }

  @Nonnull
  @JsonIgnore(false)
  @JsonProperty(PROP_PROTOCOL)
  @Override
  public RoutingProtocol getProtocol() {
    return _protocol;
  }

  @Nullable
  @JsonProperty(PROP_RECEIVED_FROM_IP)
  public Ip getReceivedFromIp() {
    return _receivedFromIp;
  }

  @JsonProperty(PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT)
  public boolean getReceivedFromRouteReflectorClient() {
    return _receivedFromRouteReflectorClient;
  }

  @Nullable
  @JsonProperty(PROP_SRC_PROTOCOL)
  public RoutingProtocol getSrcProtocol() {
    return _srcProtocol;
  }

  @Override
  public int getTag() {
    return NO_TAG;
  }

  @JsonProperty(PROP_WEIGHT)
  public int getWeight() {
    return _weight;
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    return COMPARATOR.compare(this, (BgpRoute) rhs);
  }

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
        .setNextHopIp(_nextHopIp)
        .setOriginatorIp(_originatorIp)
        .setOriginType(_originType)
        .setProtocol(_protocol)
        .setReceivedFromIp(_receivedFromIp)
        .setReceivedFromRouteReflectorClient(_receivedFromRouteReflectorClient)
        .setSrcProtocol(_srcProtocol)
        .setWeight(_weight);
  }
}
