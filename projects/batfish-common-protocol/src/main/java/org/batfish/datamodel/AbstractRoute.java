package org.batfish.datamodel;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
@ParametersAreNonnullByDefault
public abstract class AbstractRoute implements Serializable, Comparable<AbstractRoute> {

  private static final long serialVersionUID = 1L;

  public static final int NO_TAG = -1;

  static final String PROP_ADMINISTRATIVE_COST = "administrativeCost";

  static final String PROP_METRIC = "metric";

  static final String PROP_NETWORK = "network";

  static final String PROP_NEXT_HOP_INTERFACE = "nextHopInterface";

  static final String PROP_NEXT_HOP_IP = "nextHopIp";

  static final String PROP_NEXT_HOP = "nextHop";

  static final String PROP_NODE = "node";

  static final String PROP_PROTOCOL = "protocol";

  static final String PROP_TAG = "tag";

  static final String PROP_VRF = "vrf";

  protected final Prefix _network;

  private String _nextHop;

  private String _node;

  private boolean _nonRouting;

  private String _vrf;

  @JsonCreator
  protected AbstractRoute(@Nullable @JsonProperty(PROP_NETWORK) Prefix network) {
    _network = requireNonNull(network, "Cannot crate a route without a network");
  }

  @Override
  public final int compareTo(@Nonnull AbstractRoute rhs) {
    return Comparator.comparing(AbstractRoute::getNetwork)
        .thenComparingInt(AbstractRoute::getAdministrativeCost)
        .thenComparing(AbstractRoute::getMetric)
        .thenComparing(AbstractRoute::routeCompare)
        .thenComparing(AbstractRoute::getNextHopIp)
        .thenComparing(AbstractRoute::getNextHopInterface)
        .thenComparingInt(AbstractRoute::getTag)
        .compare(this, rhs);
  }

  @Override
  public abstract boolean equals(Object o);

  public final String fullString() {
    String nhnode = _nextHop;
    if (!Route.UNSET_NEXT_HOP_INTERFACE.equals(getNextHopInterface())
        && Route.UNSET_ROUTE_NEXT_HOP_IP.equals(getNextHopIp())) {
      // static interface with no next hop ip
      nhnode = "N/A";
    }

    return String.format(
        "%s vrf:%s net:%s nhip:%s nhint:%s nhnode:%s admin:%s cost:%s tag:%s prot:%s %s",
        _node,
        _vrf,
        _network,
        getNextHopIp(),
        getNextHopInterface(),
        nhnode,
        getAdministrativeCost(),
        getMetric(),
        getTag() == Route.UNSET_ROUTE_TAG ? "none" : getTag(),
        getProtocol(),
        protocolRouteString());
  }

  @JsonIgnore
  public abstract int getAdministrativeCost();

  @JsonIgnore
  public abstract Long getMetric();

  @JsonProperty(PROP_NETWORK)
  @JsonPropertyDescription("IPV4 network of this route")
  public final Prefix getNetwork() {
    return _network;
  }

  @JsonProperty(PROP_NEXT_HOP)
  @JsonPropertyDescription("Next hop node, if known")
  public String getNextHop() {
    return _nextHop;
  }

  @JsonIgnore
  public abstract String getNextHopInterface();

  @JsonIgnore
  public abstract Ip getNextHopIp();

  @JsonProperty(PROP_NODE)
  public String getNode() {
    return _node;
  }

  @JsonIgnore
  public final boolean getNonRouting() {
    return _nonRouting;
  }

  @JsonIgnore
  public abstract RoutingProtocol getProtocol();

  @JsonIgnore
  public abstract int getTag();

  @JsonProperty(PROP_VRF)
  public String getVrf() {
    return _vrf;
  }

  @Override
  public abstract int hashCode();

  protected abstract String protocolRouteString();

  public abstract int routeCompare(AbstractRoute rhs);

  @JsonProperty(PROP_NEXT_HOP)
  public void setNextHop(String nextHop) {
    _nextHop = nextHop;
  }

  @JsonProperty(PROP_NODE)
  public void setNode(String node) {
    _node = node;
  }

  @JsonIgnore
  public final void setNonRouting(boolean nonRouting) {
    _nonRouting = nonRouting;
  }

  @JsonProperty(PROP_VRF)
  public void setVrf(String vrf) {
    _vrf = vrf;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "<"
        + _network
        + ",nhip:"
        + getNextHopIp()
        + ",nhint:"
        + getNextHopInterface()
        + ">";
  }

  public Route toSummaryRoute(String hostname, String vrfName, Map<Ip, String> ipOwners) {
    RouteBuilder rb = new RouteBuilder();
    rb.setNode(hostname);
    rb.setNetwork(getNetwork());
    Ip nextHopIp = getNextHopIp();
    if (getProtocol() == RoutingProtocol.CONNECTED
        || getProtocol() == RoutingProtocol.LOCAL
        || (getProtocol() == RoutingProtocol.STATIC
            && nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP))
        || Interface.NULL_INTERFACE_NAME.equals(getNextHopInterface())) {
      rb.setNextHop(Configuration.NODE_NONE_NAME);
    }
    if (!nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
      rb.setNextHopIp(nextHopIp);
      String nextHop = ipOwners.get(nextHopIp);
      if (nextHop != null) {
        rb.setNextHop(nextHop);
      }
    }
    String nextHopInterface = getNextHopInterface();
    if (!nextHopInterface.equals(Route.UNSET_NEXT_HOP_INTERFACE)) {
      rb.setNextHopInterface(nextHopInterface);
    }
    rb.setAdministrativeCost(getAdministrativeCost());
    rb.setCost(getMetric());
    rb.setProtocol(getProtocol());
    rb.setTag(getTag());
    rb.setVrf(vrfName);
    return rb.build();
  }
}
