package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishException;
import org.batfish.common.util.CommonUtil;

public class BgpRoute extends AbstractRoute {

  public static class Builder extends AbstractRouteBuilder<Builder, BgpRoute> {

    private List<SortedSet<Long>> _asPath;

    private SortedSet<Long> _clusterList;

    private SortedSet<Long> _communities;

    private int _localPreference;

    private Ip _originatorIp;

    private OriginType _originType;

    private RoutingProtocol _protocol;

    private Ip _receivedFromIp;

    private boolean _receivedFromRouteReflectorClient;

    private RoutingProtocol _srcProtocol;

    private int _weight;

    public Builder() {
      _asPath = new ArrayList<>();
      _communities = new TreeSet<>();
      _clusterList = new TreeSet<>();
    }

    @Override
    public BgpRoute build() {
      if (_originatorIp == null) {
        throw new BatfishException("Missing originatorIp");
      }
      if (_originType == null) {
        throw new BatfishException("Missing originType");
      }
      if (_receivedFromIp == null) {
        throw new BatfishException("Missing receivedFromIp");
      }
      return new BgpRoute(
          getNetwork(),
          getNextHopIp(),
          getAdmin(),
          new AsPath(_asPath),
          _communities,
          _localPreference,
          getMetric(),
          _originatorIp,
          _clusterList,
          _receivedFromRouteReflectorClient,
          _originType,
          _protocol,
          _receivedFromIp,
          _srcProtocol,
          _weight);
    }

    public List<SortedSet<Long>> getAsPath() {
      return _asPath;
    }

    public SortedSet<Long> getClusterList() {
      return _clusterList;
    }

    public SortedSet<Long> getCommunities() {
      return _communities;
    }

    public int getLocalPreference() {
      return _localPreference;
    }

    public Ip getOriginatorIp() {
      return _originatorIp;
    }

    public OriginType getOriginType() {
      return _originType;
    }

    public RoutingProtocol getProtocol() {
      return _protocol;
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    public int getWeight() {
      return _weight;
    }

    public Builder setAsPath(List<SortedSet<Long>> asPath) {
      _asPath = asPath;
      return getThis();
    }

    public Builder setClusterList(SortedSet<Long> clusterList) {
      _clusterList = clusterList;
      return getThis();
    }

    public Builder setCommunities(SortedSet<Long> communities) {
      _communities = communities;
      return getThis();
    }

    public Builder setLocalPreference(int localPreference) {
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

    public Builder setReceivedFromIp(Ip receivedFromIp) {
      _receivedFromIp = receivedFromIp;
      return getThis();
    }

    public Builder setReceivedFromRouteReflectorClient(boolean receivedFromRouteReflectorClient) {
      _receivedFromRouteReflectorClient = receivedFromRouteReflectorClient;
      return getThis();
    }

    public Builder setSrcProtocol(RoutingProtocol srcProtocol) {
      _srcProtocol = srcProtocol;
      return getThis();
    }

    public Builder setWeight(int weight) {
      _weight = weight;
      return getThis();
    }
  }

  public static final int DEFAULT_LOCAL_PREFERENCE = 100;

  private static final String PROP_AS_PATH = "asPath";

  private static final String PROP_CLUSTER_LIST = "clusterList";

  private static final String PROP_COMMUNITIES = "communities";

  private static final String PROP_LOCAL_PREFERENCE = "localPreference";

  private static final String PROP_ORIGIN_TYPE = "originType";

  private static final String PROP_ORIGINATOR_IP = "originatorIp";

  private static final String PROP_RECEIVED_FROM_IP = "receivedFromIp";

  private static final String PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT =
      "receivedFromRouteReflectorClient";

  private static final String PROP_SRC_PROTOCOL = "srcProtocol";

  private static final String PROP_WEIGHT = "weight";

  /** */
  private static final long serialVersionUID = 1L;

  private final int _admin;

  private final AsPath _asPath;

  private final SortedSet<Long> _clusterList;

  private final SortedSet<Long> _communities;

  private final int _localPreference;

  private final long _med;

  private final Ip _nextHopIp;

  private final Ip _originatorIp;

  @Nonnull private final OriginType _originType;

  private final RoutingProtocol _protocol;

  private final Ip _receivedFromIp;

  private final boolean _receivedFromRouteReflectorClient;

  private final RoutingProtocol _srcProtocol;

  private final int _weight;

  @JsonCreator
  public BgpRoute(
      @JsonProperty(PROP_NETWORK) Prefix network,
      @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int admin,
      @JsonProperty(PROP_AS_PATH) AsPath asPath,
      @JsonProperty(PROP_COMMUNITIES) SortedSet<Long> communities,
      @JsonProperty(PROP_LOCAL_PREFERENCE) int localPreference,
      @JsonProperty(PROP_METRIC) long med,
      @JsonProperty(PROP_ORIGINATOR_IP) Ip originatorIp,
      @JsonProperty(PROP_CLUSTER_LIST) SortedSet<Long> clusterList,
      @JsonProperty(PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT)
          boolean receivedFromRouteReflectorClient,
      @Nonnull @JsonProperty(PROP_ORIGIN_TYPE) OriginType originType,
      @JsonProperty(PROP_PROTOCOL) RoutingProtocol protocol,
      @JsonProperty(PROP_RECEIVED_FROM_IP) Ip receivedFromIp,
      @JsonProperty(PROP_SRC_PROTOCOL) RoutingProtocol srcProtocol,
      @JsonProperty(PROP_WEIGHT) int weight) {
    super(network);
    _admin = admin;
    _asPath = asPath;
    _clusterList =
        clusterList == null ? Collections.emptySortedSet() : ImmutableSortedSet.copyOf(clusterList);
    _communities =
        communities == null ? Collections.emptySortedSet() : ImmutableSortedSet.copyOf(communities);
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

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof BgpRoute)) {
      return false;
    }
    BgpRoute other = (BgpRoute) o;
    if (_admin != other._admin) {
      return false;
    }
    if (!_asPath.equals(other._asPath)) {
      return false;
    }
    if (!_clusterList.equals(other._clusterList)) {
      return false;
    }
    if (!_communities.equals(other._communities)) {
      return false;
    }
    if (_localPreference != other._localPreference) {
      return false;
    }
    if (_med != other._med) {
      return false;
    }
    if (!_network.equals(other._network)) {
      return false;
    }
    if (!Objects.equals(_nextHopIp, other._nextHopIp)) {
      return false;
    }
    if (_originType != other._originType) {
      return false;
    }
    if (!Objects.equals(_originatorIp, other._originatorIp)) {
      return false;
    }
    if (_protocol != other._protocol) {
      return false;
    }
    if (!_receivedFromIp.equals(other._receivedFromIp)) {
      return false;
    }
    if (_weight != other._weight) {
      return false;
    }
    return true;
  }

  @JsonIgnore(false)
  @JsonProperty(PROP_ADMINISTRATIVE_COST)
  @Override
  public int getAdministrativeCost() {
    return _admin;
  }

  @JsonProperty(PROP_AS_PATH)
  public AsPath getAsPath() {
    return _asPath;
  }

  @JsonProperty(PROP_CLUSTER_LIST)
  public SortedSet<Long> getClusterList() {
    return _clusterList;
  }

  @JsonProperty(PROP_COMMUNITIES)
  public SortedSet<Long> getCommunities() {
    return _communities;
  }

  @JsonProperty(PROP_LOCAL_PREFERENCE)
  public int getLocalPreference() {
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

  @JsonProperty(PROP_ORIGINATOR_IP)
  public Ip getOriginatorIp() {
    return _originatorIp;
  }

  @JsonProperty(PROP_ORIGIN_TYPE)
  public OriginType getOriginType() {
    return _originType;
  }

  @JsonIgnore(false)
  @JsonProperty(PROP_PROTOCOL)
  @Override
  public RoutingProtocol getProtocol() {
    return _protocol;
  }

  @JsonProperty(PROP_RECEIVED_FROM_IP)
  public Ip getReceivedFromIp() {
    return _receivedFromIp;
  }

  @JsonProperty(PROP_RECEIVED_FROM_ROUTE_REFLECTOR_CLIENT)
  public boolean getReceivedFromRouteReflectorClient() {
    return _receivedFromRouteReflectorClient;
  }

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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _admin;
    result = prime * result + ((_asPath == null) ? 0 : _asPath.hashCode());
    result = prime * result + _clusterList.hashCode();
    result = prime * result + ((_communities == null) ? 0 : _communities.hashCode());
    result = prime * result + _localPreference;
    result = prime * result + Long.hashCode(_med);
    result = prime * result + _network.hashCode();
    result = prime * result + ((_nextHopIp == null) ? 0 : _nextHopIp.hashCode());
    result = prime * result + _originType.ordinal();
    result = prime * result + ((_originatorIp == null) ? 0 : _originatorIp.hashCode());
    result = prime * result + ((_protocol == null) ? 0 : _protocol.ordinal());
    result = prime * result + ((_receivedFromIp == null) ? 0 : _receivedFromIp.hashCode());
    result = prime * result + _weight;
    return result;
  }

  @Override
  protected final String protocolRouteString() {
    return " asPath:"
        + _asPath
        + " clusterList:"
        + _clusterList
        + " communities:"
        + _communities
        + " localPreference:"
        + _localPreference
        + " med:"
        + _med
        + " originatorIp:"
        + _originatorIp
        + " originType:"
        + _originType
        + " receivedFromIp:"
        + _receivedFromIp
        + " srcProtocol:"
        + _srcProtocol
        + " weight:"
        + _weight;
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    BgpRoute castRhs = (BgpRoute) rhs;
    int ret;
    ret = _asPath.compareTo(castRhs._asPath);
    if (ret != 0) {
      return ret;
    }
    ret = CommonUtil.compareCollection(_clusterList, castRhs._clusterList);
    if (ret != 0) {
      return ret;
    }
    ret = CommonUtil.compareCollection(_communities, castRhs._communities);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_localPreference, castRhs._localPreference);
    if (ret != 0) {
      return ret;
    }
    ret = _originType.compareTo(castRhs._originType);
    if (ret != 0) {
      return ret;
    }
    ret = _originatorIp.compareTo(castRhs._originatorIp);
    if (ret != 0) {
      return ret;
    }
    ret = _receivedFromIp.compareTo(castRhs._receivedFromIp);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(_weight, castRhs._weight);
    return ret;
  }
}
