package org.batfish.representation.cisco;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfAreaSummary;
import org.batfish.datamodel.OspfMetricType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;

public class OspfProcess extends ComparableStructure<String> {

  private static final long DEFAULT_DEFAULT_INFORMATION_METRIC = 1L;

  private static final OspfMetricType DEFAULT_DEFAULT_INFORMATION_METRIC_TYPE = OspfMetricType.E2;

  public static final long DEFAULT_MAX_METRIC_EXTERNAL_LSA = 0xFF0000L;

  public static final long DEFAULT_MAX_METRIC_SUMMARY_LSA = 0xFF0000L;

  private static final double DEFAULT_REFERENCE_BANDWIDTH_10_MBPS = 10E6D;

  private static final double DEFAULT_REFERENCE_BANDWIDTH_100_MBPS = 100E6D;

  private static final double DEFAULT_REFERENCE_BANDWIDTH_40_GBPS = 40E9D;

  public static final long MAX_METRIC_ROUTER_LSA = 0xFFFFL;

  private static final long serialVersionUID = 1L;

  private long _defaultInformationMetric;

  private OspfMetricType _defaultInformationMetricType;

  private boolean _defaultInformationOriginate;

  private boolean _defaultInformationOriginateAlways;

  private String _defaultInformationOriginateMap;

  private Integer _defaultInformationOriginateMapLine;

  private Long _defaultMetric;

  private Set<String> _interfaceBlacklist;

  private Set<String> _interfaceWhitelist;

  private Long _maxMetricExternalLsa;

  private boolean _maxMetricIncludeStub;

  private boolean _maxMetricRouterLsa;

  private Long _maxMetricSummaryLsa;

  private Set<OspfNetwork> _networks;

  private Map<Long, Boolean> _nssas;

  private boolean _passiveInterfaceDefault;

  private Map<RoutingProtocol, OspfRedistributionPolicy> _redistributionPolicies;

  private double _referenceBandwidth;

  private @Nullable Boolean _rfc1583Compatible;

  private Ip _routerId;

  private Map<Long, Map<Prefix, OspfAreaSummary>> _summaries;

  private Set<OspfWildcardNetwork> _wildcardNetworks;

  public static double getReferenceOspfBandwidth(ConfigurationFormat format) {
    switch (format) {
      case ARISTA: // EOS manual, Chapter 27, "auto-cost reference-bandwidth (OSPFv2)"
        return DEFAULT_REFERENCE_BANDWIDTH_10_MBPS;

      case CADANT: // Internet claims they use the Cisco defaults.
      case CISCO_ASA: // ASA uses 100 Mbps, switches to 40 Gbps for OSPF v3
      case CISCO_IOS: // https://www.cisco.com/c/en/us/td/docs/ios-xml/ios/iproute_ospf/command/iro-cr-book/ospf-a1.html#wp3271966058
      case CISCO_IOS_XR: // https://www.cisco.com/c/en/us/td/docs/ios_xr_sw/iosxr_r3-7/routing/command/reference/rr37ospf.html
      case FORCE10: // http://www.dell.com/support/manuals/us/en/19/force10-s4810/s4810_9.9.0.0_cli_pub/auto-cost
      case FOUNDRY: // http://www.brocade.com/content/html/en/command-reference-guide/FI_08030_CMDREF/GUID-D7109E43-D368-46FE-95AF-D522B203E501.html
        return DEFAULT_REFERENCE_BANDWIDTH_100_MBPS;

      case CISCO_NX: // https://www.cisco.com/c/m/en_us/techdoc/dc/reference/cli/nxos/commands/ospf/auto-cost-ospf.html
        return DEFAULT_REFERENCE_BANDWIDTH_40_GBPS;

      default:
        throw new BatfishException("Unknown default OSPF reference bandwidth for format " + format);
    }
  }

  public long getDefaultMetric(ConfigurationFormat format, RoutingProtocol protocol) {
    if (this._defaultMetric != null) {
      return this._defaultMetric;
    }

    switch (format) {
      case ARISTA:
        // Inferred from Arista manual OSPF v3 default-metric comment.
        return 10;

      case CADANT: // Vetted IOS and NXOS; assuming the rest use IOS defaults.
      case CISCO_ASA:
      case CISCO_IOS:
      case CISCO_IOS_XR:
      case CISCO_NX:
      case FORCE10:
      case FOUNDRY:
        // https://www.cisco.com/c/en/us/support/docs/ip/open-shortest-path-first-ospf/7039-1.html
        // "the cost allocated to the external route is 20 (the default is 1 for BGP)."
        switch (protocol) {
          case BGP:
            return 1;
          default:
            return 20;
        }

      default:
        throw new BatfishException("Unknown default OSPF reference bandwidth for format " + format);
    }
  }

  public OspfProcess(String name, ConfigurationFormat format) {
    super(name);
    _referenceBandwidth = getReferenceOspfBandwidth(format);
    _networks = new TreeSet<>();
    _defaultInformationMetric = DEFAULT_DEFAULT_INFORMATION_METRIC;
    _defaultInformationMetricType = DEFAULT_DEFAULT_INFORMATION_METRIC_TYPE;
    _nssas = new HashMap<>();
    _interfaceBlacklist = new HashSet<>();
    _interfaceWhitelist = new HashSet<>();
    _wildcardNetworks = new TreeSet<>();
    _redistributionPolicies = new EnumMap<>(RoutingProtocol.class);
    _summaries = new TreeMap<>();
  }

  public void computeNetworks(Collection<Interface> interfaces) {
    for (Interface i : interfaces) {
      InterfaceAddress address = i.getAddress();
      if (address == null) {
        continue;
      }
      for (OspfWildcardNetwork wn : _wildcardNetworks) {
        // first we check if the interface ip address matches the ospf
        // network when the wildcard is ORed to both
        long wildcardLong = wn.getWildcard().asLong();
        long ospfNetworkLong = wn.getNetworkAddress().asLong();
        long intIpLong = address.getIp().asLong();
        long wildcardedOspfNetworkLong = ospfNetworkLong | wildcardLong;
        long wildcardedIntIpLong = intIpLong | wildcardLong;
        if (wildcardedOspfNetworkLong == wildcardedIntIpLong) {
          // since we have a match, we add the INTERFACE network, ignoring
          // the wildcard stuff from before
          Prefix newOspfNetwork = address.getPrefix();
          _networks.add(new OspfNetwork(newOspfNetwork, wn.getArea()));
          break;
        }
      }
    }
  }

  public Set<String> getActiveInterfaceList() {
    return _interfaceWhitelist;
  }

  public long getDefaultInformationMetric() {
    return _defaultInformationMetric;
  }

  public OspfMetricType getDefaultInformationMetricType() {
    return _defaultInformationMetricType;
  }

  public boolean getDefaultInformationOriginate() {
    return _defaultInformationOriginate;
  }

  public boolean getDefaultInformationOriginateAlways() {
    return _defaultInformationOriginateAlways;
  }

  public String getDefaultInformationOriginateMap() {
    return _defaultInformationOriginateMap;
  }

  public Integer getDefaultInformationOriginateMapLine() {
    return _defaultInformationOriginateMapLine;
  }

  public Long getDefaultMetric() {
    return _defaultMetric;
  }

  public Long getMaxMetricExternalLsa() {
    return _maxMetricExternalLsa;
  }

  public boolean getMaxMetricIncludeStub() {
    return _maxMetricIncludeStub;
  }

  public boolean getMaxMetricRouterLsa() {
    return _maxMetricRouterLsa;
  }

  public Long getMaxMetricSummaryLsa() {
    return _maxMetricSummaryLsa;
  }

  public Set<OspfNetwork> getNetworks() {
    return _networks;
  }

  public Map<Long, Boolean> getNssas() {
    return _nssas;
  }

  public boolean getPassiveInterfaceDefault() {
    return _passiveInterfaceDefault;
  }

  public Set<String> getPassiveInterfaceList() {
    return _interfaceBlacklist;
  }

  public Map<RoutingProtocol, OspfRedistributionPolicy> getRedistributionPolicies() {
    return _redistributionPolicies;
  }

  public double getReferenceBandwidth() {
    return _referenceBandwidth;
  }

  public @Nullable Boolean getRfc1583Compatible() {
    return _rfc1583Compatible;
  }

  public Ip getRouterId() {
    return _routerId;
  }

  public Map<Long, Map<Prefix, OspfAreaSummary>> getSummaries() {
    return _summaries;
  }

  public Set<OspfWildcardNetwork> getWildcardNetworks() {
    return _wildcardNetworks;
  }

  public void setDefaultInformationMetric(int metric) {
    _defaultInformationMetric = metric;
  }

  public void setDefaultInformationMetricType(OspfMetricType metricType) {
    _defaultInformationMetricType = metricType;
  }

  public void setDefaultInformationOriginate(boolean b) {
    _defaultInformationOriginate = b;
  }

  public void setDefaultInformationOriginateAlways(boolean b) {
    _defaultInformationOriginateAlways = b;
  }

  public void setDefaultInformationOriginateMap(String name) {
    _defaultInformationOriginateMap = name;
  }

  public void setDefaultInformationOriginateMapLine(Integer defaultInformationOriginateMapLine) {
    _defaultInformationOriginateMapLine = defaultInformationOriginateMapLine;
  }

  public void setDefaultMetric(Long metric) {
    _defaultMetric = metric;
  }

  public void setMaxMetricExternalLsa(Long maxMetricExternalLsa) {
    _maxMetricExternalLsa = maxMetricExternalLsa;
  }

  public void setMaxMetricIncludeStub(boolean maxMetricIncludeStub) {
    _maxMetricIncludeStub = maxMetricIncludeStub;
  }

  public void setMaxMetricRouterLsa(boolean maxMetricRouterLsa) {
    _maxMetricRouterLsa = maxMetricRouterLsa;
  }

  public void setMaxMetricSummaryLsa(Long maxMetricSummaryLsa) {
    _maxMetricSummaryLsa = maxMetricSummaryLsa;
  }

  public void setPassiveInterfaceDefault(boolean b) {
    _passiveInterfaceDefault = b;
  }

  public void setReferenceBandwidth(double referenceBandwidth) {
    _referenceBandwidth = referenceBandwidth;
  }

  public void setRfc1583Compatible(@Nullable Boolean rfc1583Compatible) {
    _rfc1583Compatible = rfc1583Compatible;
  }

  public void setRouterId(Ip routerId) {
    _routerId = routerId;
  }
}
