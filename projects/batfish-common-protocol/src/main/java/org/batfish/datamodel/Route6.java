package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;

@JsonPropertyOrder({Route6.PROP_DIFF_SYMBOL})
public class Route6 implements Comparable<Route6>, Serializable {

  private static final String PROP_ADMINISTRATIVE_COST = "administrativeCost";

  public static final String AMBIGUOUS_NEXT_HOP = "(ambiguous)";

  protected static final String PROP_DIFF_SYMBOL = "diffSymbol";

  private static final String PROP_METRIC = "metric";

  private static final String PROP_NETWORK = "network";

  private static final String PROP_NEXT_HOP_INTERFACE = "nextHopInterface";

  private static final String PROP_NEXT_HOP_IP = "nextHopIp";

  private static final String PROP_NEXT_HOP = "nextHop";

  private static final String PROP_NODE = "node";

  private static final String PROP_PROTOCOL = "protocol";

  /** */
  private static final long serialVersionUID = 1L;

  private static final String PROP_TAG = "tag";

  public static final String UNSET_NEXT_HOP = "(unknown)";

  public static final String UNSET_NEXT_HOP_INTERFACE = "dynamic";

  public static final int UNSET_ROUTE_ADMIN = -1;

  public static final int UNSET_ROUTE_COST = -1;

  public static final Ip6 UNSET_ROUTE_NEXT_HOP_IP = Ip6.AUTO;

  public static final int UNSET_ROUTE_TAG = -1;

  private static final String PROP_VRF = "vrf";

  private final int _administrativeCost;

  private transient String _diffSymbol;

  private final int _metric;

  private final Prefix6 _network;

  private final transient String _nextHop;

  private final String _nextHopInterface;

  private final Ip6 _nextHopIp;

  private final String _node;

  private final RoutingProtocol _protocol;

  private final int _tag;

  private final String _vrf;

  @JsonCreator
  public Route6(
      @JsonProperty(PROP_NODE) String node,
      @JsonProperty(PROP_VRF) String vrf,
      @JsonProperty(PROP_NETWORK) Prefix6 network,
      @JsonProperty(PROP_NEXT_HOP_IP) Ip6 nextHopIp,
      @JsonProperty(PROP_NEXT_HOP) String nextHop,
      @JsonProperty(PROP_NEXT_HOP_INTERFACE) String nextHopInterface,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int administrativeCost,
      @JsonProperty(PROP_METRIC) int metric,
      @JsonProperty(PROP_PROTOCOL) RoutingProtocol protocol,
      @JsonProperty(PROP_TAG) int tag) {
    _network = network;
    _nextHopIp = nextHopIp;
    _node = node;
    _nextHop = nextHop;
    _nextHopInterface = nextHopInterface;
    _administrativeCost = administrativeCost;
    _metric = metric;
    _protocol = protocol;
    _tag = tag;
    _vrf = vrf;
  }

  @Override
  public int compareTo(Route6 rhs) {
    int result = _node.compareTo(rhs._node);
    if (result != 0) {
      return result;
    }
    result = _vrf.compareTo(rhs._vrf);
    if (result != 0) {
      return result;
    }
    result = _network.compareTo(rhs._network);
    if (result != 0) {
      return result;
    }
    result = _nextHopIp.compareTo(rhs._nextHopIp);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(_administrativeCost, rhs._administrativeCost);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(_metric, rhs._metric);
    if (result != 0) {
      return result;
    }
    result = _protocol.compareTo(rhs._protocol);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(_tag, rhs._tag);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Route6)) {
      return false;
    }
    Route6 other = (Route6) o;
    if (_administrativeCost != other._administrativeCost) {
      return false;
    }
    if (_metric != other._metric) {
      return false;
    }
    if (!_nextHopIp.equals(other._nextHopIp)) {
      return false;
    }
    if (!_node.equals(other._node)) {
      return false;
    }
    if (!_network.equals(other._network)) {
      return false;
    }
    if (_protocol != other._protocol) {
      return false;
    }
    if (_tag != other._tag) {
      return false;
    }
    if (!_vrf.equals(other._vrf)) {
      return false;
    }
    return true;
  }

  @JsonProperty(PROP_ADMINISTRATIVE_COST)
  public int getAdministrativeCost() {
    return _administrativeCost;
  }

  @JsonProperty(PROP_DIFF_SYMBOL)
  public String getDiffSymbol() {
    return _diffSymbol;
  }

  @JsonProperty(PROP_METRIC)
  public int getMetric() {
    return _metric;
  }

  @JsonProperty(PROP_NETWORK)
  public Prefix6 getNetwork() {
    return _network;
  }

  @JsonProperty(PROP_NEXT_HOP)
  public String getNextHop() {
    return _nextHop;
  }

  @JsonProperty(PROP_NEXT_HOP_INTERFACE)
  public String getNextHopInterface() {
    return _nextHopInterface;
  }

  @JsonProperty(PROP_NEXT_HOP_IP)
  public Ip6 getNextHopIp() {
    return _nextHopIp;
  }

  @JsonProperty(PROP_NODE)
  public String getNode() {
    return _node;
  }

  @JsonProperty(PROP_PROTOCOL)
  public RoutingProtocol getProtocol() {
    return _protocol;
  }

  @JsonProperty(PROP_TAG)
  public int getTag() {
    return _tag;
  }

  public String getVrf() {
    return _vrf;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _administrativeCost;
    result = prime * result + _metric;
    result = prime * result + _nextHopIp.hashCode();
    result = prime * result + _node.hashCode();
    result = prime * result + _network.hashCode();
    result = prime * result + _protocol.ordinal();
    result = prime * result + _tag;
    result = prime * result + _vrf.hashCode();
    return result;
  }

  public String prettyPrint(boolean diff) {
    String node = getNode();
    String nhnode = getNextHop();
    Ip6 nextHopIp = getNextHopIp();
    String nhip;
    String tag;
    int tagInt = getTag();
    if (tagInt == Route6.UNSET_ROUTE_TAG) {
      tag = "none";
    } else {
      tag = Integer.toString(tagInt);
    }
    String nhint = getNextHopInterface();
    if (!nhint.equals(Route6.UNSET_NEXT_HOP_INTERFACE)
        && nextHopIp.equals(Route6.UNSET_ROUTE_NEXT_HOP_IP)) {
      // static interface without next hop ip
      nhnode = "N/A";
      nhip = "N/A";
    }
    nhip = nextHopIp != null ? nextHopIp.toString() : "N/A";
    String vrf = getVrf();
    String net = getNetwork().toString();
    String admin = Integer.toString(getAdministrativeCost());
    String cost = Integer.toString(getMetric());
    String prot = getProtocol().protocolName();
    String diffStr = diff ? getDiffSymbol() + " " : "";
    String routeStr =
        String.format(
            "%s%s vrf:%s net:%s nhip:%s nhint:%s nhnode:%s admin:%s cost:%s tag:%s prot:%s\n",
            diffStr, node, vrf, net, nhip, nhint, nhnode, admin, cost, tag, prot);
    return routeStr;
  }

  @JsonProperty(PROP_DIFF_SYMBOL)
  public void setDiffSymbol(String diffSymbol) {
    _diffSymbol = diffSymbol;
  }

  @Override
  public String toString() {
    String nextHop = _nextHop;
    String nextHopIp = _nextHopIp.toString();
    String tag = Integer.toString(_tag);
    // extra formatting
    if (!_nextHopInterface.equals(UNSET_NEXT_HOP_INTERFACE)
        && _nextHopIp.equals(UNSET_ROUTE_NEXT_HOP_IP)) {
      // static interface without next hop ip
      nextHop = "N/A";
      nextHopIp = "N/A";
    }
    if (_tag == UNSET_ROUTE_TAG) {
      tag = "none";
    }
    return "Route<"
        + _node
        + ", "
        + _network
        + ", "
        + nextHopIp
        + ", "
        + nextHop
        + ", "
        + _nextHopInterface
        + ", "
        + _administrativeCost
        + ", "
        + _metric
        + ", "
        + tag
        + ", "
        + _protocol
        + ">";
  }
}
