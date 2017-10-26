package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishException;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public abstract class AbstractRoute implements Serializable, Comparable<AbstractRoute> {

  protected static final String PROP_ADMINISTRATIVE_COST = "administrativeCost";

  protected static final String PROP_METRIC = "metric";

  protected static final String PROP_NETWORK = "network";

  protected static final String PROP_NEXT_HOP_INTERFACE = "nextHopInterface";

  protected static final String PROP_NEXT_HOP_IP = "nextHopIp";

  private static final String PROP_NEXT_HOP = "nextHop";

  public static final int NO_TAG = -1;

  private static final String PROP_NODE = "node";

  protected static final String PROP_PROTOCOL = "protocol";

  private static final long serialVersionUID = 1L;

  protected static final String PROP_TAG = "tag";

  private static final String PROP_VRF = "vrf";

  protected final Prefix _network;

  private String _nextHop;

  private String _node;

  private boolean _nonRouting;

  private String _vrf;

  @JsonCreator
  public AbstractRoute(@JsonProperty(PROP_NETWORK) Prefix network) {
    if (network == null) {
      throw new BatfishException("Cannot construct AbstractRoute with null network");
    }
    _network = network;
  }

  @Override
  public final int compareTo(AbstractRoute rhs) {
    int ret;
    ret = _network.compareTo(rhs._network);
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(getAdministrativeCost(), rhs.getAdministrativeCost());
    if (ret != 0) {
      return ret;
    }
    Long lhsMetric = getMetric();
    Long rhsMetric = rhs.getMetric();
    if (lhsMetric == null) {
      if (rhsMetric != null) {
        ret = -1;
      } else {
        ret = 0;
      }
    } else if (rhsMetric == null) {
      ret = 1;
    } else {
      ret = Long.compare(lhsMetric, rhsMetric);
    }
    if (ret != 0) {
      return ret;
    }
    ret = routeCompare(rhs);
    if (ret != 0) {
      return ret;
    }
    Ip lhsNextHopIp = getNextHopIp();
    Ip rhsNextHopIp = rhs.getNextHopIp();
    if (Route.UNSET_ROUTE_NEXT_HOP_IP.equals(lhsNextHopIp)) {
      if (!Route.UNSET_ROUTE_NEXT_HOP_IP.equals(rhsNextHopIp)) {
        ret = -1;
      } else {
        ret = 0;
      }
    } else if (Route.UNSET_ROUTE_NEXT_HOP_IP.equals(rhsNextHopIp)) {
      ret = 1;
    } else {
      ret = lhsNextHopIp.compareTo(rhsNextHopIp);
    }
    if (ret != 0) {
      return ret;
    }
    String nextHopInterface = getNextHopInterface();
    String rhsNextHopInterface = rhs.getNextHopInterface();
    if (Route.UNSET_NEXT_HOP_INTERFACE.equals(nextHopInterface)) {
      if (!Route.UNSET_NEXT_HOP_INTERFACE.equals(rhsNextHopInterface)) {
        ret = -1;
      } else {
        ret = 0;
      }
    } else {
      ret = nextHopInterface.compareTo(rhsNextHopInterface);
    }
    if (ret != 0) {
      return ret;
    }
    ret = Integer.compare(getTag(), rhs.getTag());
    return ret;
  }

  @Override
  public abstract boolean equals(Object o);

  public final String fullString() {
    String nhnode = _nextHop;
    Ip nextHopIp = getNextHopIp();
    String nhip;
    String tag;
    int tagInt = getTag();
    if (tagInt == Route.UNSET_ROUTE_TAG) {
      tag = "none";
    } else {
      tag = Integer.toString(tagInt);
    }
    String nhint = getNextHopInterface();
    if (!nhint.equals(Route.UNSET_NEXT_HOP_INTERFACE)
        && nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
      // static interface with no next hop ip
      nhnode = "N/A";
    }
    nhip = !Route.UNSET_ROUTE_NEXT_HOP_IP.equals(nextHopIp) ? nextHopIp.toString() : "N/A";
    String net = getNetwork().toString();
    String admin = Integer.toString(getAdministrativeCost());
    String cost = Long.toString(getMetric());
    String prot = getProtocol().protocolName();
    String routeStr =
        String.format(
            "%s vrf:%s net:%s nhip:%s nhint:%s nhnode:%s admin:%s cost:%s tag:%s prot:%s %s",
            _node, _vrf, net, nhip, nhint, nhnode, admin, cost, tag, prot, protocolRouteString());
    return routeStr;
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
  public String getNextHop() {
    return _nextHop;
  }

  @JsonIgnore
  @Nonnull
  public abstract String getNextHopInterface();

  @Nonnull
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
    Route outputRoute = rb.build();
    return outputRoute;
  }
}
