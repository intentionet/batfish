package org.batfish.representation.cisco;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.representation.cisco.CiscoConversions.generateAggregateRoutePolicy;
import static org.batfish.representation.cisco.CiscoConversions.suppressSummarizedPrefixes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.common.util.ReferenceCountedStructure;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GeneratedRoute6;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.IkePolicy;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecPolicy;
import org.batfish.datamodel.IpsecProposal;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.IsisInterfaceLevelSettings;
import org.batfish.datamodel.IsisInterfaceSettings;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Prefix6Range;
import org.batfish.datamodel.Prefix6Space;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Route6FilterLine;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SnmpServer;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork6;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefix6Set;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefix6Set;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.RouteIsClassful;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.CallStatement;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vendor_family.cisco.Aaa;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthentication;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthenticationLogin;
import org.batfish.datamodel.vendor_family.cisco.CiscoFamily;
import org.batfish.datamodel.vendor_family.cisco.Line;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.representation.cisco.nx.CiscoNxBgpGlobalConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpRedistributionPolicy;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;
import org.batfish.vendor.VendorConfiguration;

public final class CiscoConfiguration extends VendorConfiguration {

  /** Matches the IPv4 default route. */
  static final MatchPrefixSet MATCH_DEFAULT_ROUTE;

  /** Matches the IPv6 default route. */
  static final MatchPrefix6Set MATCH_DEFAULT_ROUTE6;

  /** Matches anything but the IPv4 default route. */
  static final Not NOT_DEFAULT_ROUTE;

  static {
    MATCH_DEFAULT_ROUTE =
        new MatchPrefixSet(
            new DestinationNetwork(),
            new ExplicitPrefixSet(
                new PrefixSpace(new PrefixRange(Prefix.ZERO, new SubRange(0, 0)))));
    MATCH_DEFAULT_ROUTE.setComment("match default route");

    NOT_DEFAULT_ROUTE = new Not(MATCH_DEFAULT_ROUTE);

    MATCH_DEFAULT_ROUTE6 =
        new MatchPrefix6Set(
            new DestinationNetwork6(),
            new ExplicitPrefix6Set(
                new Prefix6Space(
                    Collections.singleton(new Prefix6Range(Prefix6.ZERO, new SubRange(0, 0))))));
    MATCH_DEFAULT_ROUTE6.setComment("match default route");
  }

  private static final int CISCO_AGGREGATE_ROUTE_ADMIN_COST = 200;

  /*
   * This map is used to convert interface names to their canonical forms.
   * The entries are visited in insertion order until a key is found of which the name to convert is
   * case-insensitively a prefix. The value corresponding to that key is chosen as the canonical
   * form for that name.
   *
   * NOTE: Entries are sorted by priority. Do not reorder unless you have a good reason.
   * For instance, we don't want 'e' to be canonically considered 'Embedded-Service-Engine' instead
   * of 'Ethernet'.
   */
  private static final Map<String, String> CISCO_INTERFACE_PREFIXES =
      ImmutableMap.<String, String>builder()
          .put("ap", "ap")
          .put("Async", "Async")
          .put("ATM", "ATM")
          .put("BDI", "BDI")
          .put("BRI", "BRI")
          .put("Bundle-Ether", "Bundle-Ethernet")
          .put("BVI", "BVI")
          .put("Cable", "Cable")
          .put("cable-downstream", "cable-downstream")
          .put("cable-mac", "cable-mac")
          .put("cable-upstream", "cable-upstream")
          .put("Cellular", "Cellular")
          .put("Crypto-Engine", "Crypto-Engine")
          .put("cmp-mgmt", "cmp-mgmt")
          .put("Dialer", "Dialer")
          .put("Dot11Radio", "Dot11Radio")
          .put("Ethernet", "Ethernet")
          .put("Embedded-Service-Engine", "Embedded-Service-Engine")
          .put("FastEthernet", "FastEthernet")
          .put("fc", "fc")
          .put("fe", "FastEthernet")
          .put("fortyGigE", "FortyGigabitEthernet")
          .put("FortyGigabitEthernet", "FortyGigabitEthernet")
          .put("GigabitEthernet", "GigabitEthernet")
          .put("ge", "GigabitEthernet")
          .put("GMPLS", "GMPLS")
          .put("HundredGigE", "HundredGigabitEthernet")
          .put("ip", "ip")
          .put("Group-Async", "Group-Async")
          .put("LongReachEthernet", "LongReachEthernet")
          .put("Loopback", "Loopback")
          .put("ma", "Management")
          .put("Management", "Management")
          .put("ManagementEthernet", "ManagementEthernet")
          .put("mfr", "mfr")
          .put("mgmt", "mgmt")
          .put("MgmtEth", "ManagementEthernet")
          .put("Modular-Cable", "Modular-Cable")
          .put("Multilink", "Multilink")
          .put("Null", "Null")
          .put("nve", "nve")
          .put("Port-channel", "Port-Channel")
          .put("POS", "POS")
          .put("PTP", "PTP")
          .put("Serial", "Serial")
          .put("Service-Engine", "Service-Engine")
          .put("TenGigabitEthernet", "TenGigabitEthernet")
          .put("TenGigE", "TenGigabitEthernet")
          .put("te", "TenGigabitEthernet")
          .put("trunk", "trunk")
          .put("Tunnel", "Tunnel")
          .put("tunnel-ip", "tunnel-ip")
          .put("tunnel-te", "tunnel-te")
          .put("ve", "VirtualEthernet")
          .put("Virtual-Template", "Virtual-Template")
          .put("Vlan", "Vlan")
          .put("Vxlan", "Vxlan")
          .put("Wideband-Cable", "Wideband-Cable")
          .build();

  static final boolean DEFAULT_VRRP_PREEMPT = true;

  static final int DEFAULT_VRRP_PRIORITY = 100;

  public static final String MANAGEMENT_VRF_NAME = "management";

  static final int MAX_ADMINISTRATIVE_COST = 32767;

  public static final String NXOS_MANAGEMENT_INTERFACE_PREFIX = "mgmt";

  private static final long serialVersionUID = 1L;

  public static final String VENDOR_NAME = "cisco";

  private static final int VLAN_NORMAL_MAX_CISCO = 1005;

  private static final int VLAN_NORMAL_MIN_CISCO = 2;

  public static String computeBgpCommonExportPolicyName(String vrf) {
    return "~BGP_COMMON_EXPORT_POLICY:" + vrf + "~";
  }

  /**
   * Computes a mapping of interface names to the primary {@link Ip} owned by each of the interface.
   * Filters out the interfaces having no primary {@link InterfaceAddress}
   */
  private static Map<String, Ip> computeInterfaceOwnedPrimaryIp(Map<String, Interface> interfaces) {
    return interfaces
        .entrySet()
        .stream()
        .filter(e -> Objects.nonNull(e.getValue().getAddress()))
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getAddress().getIp()));
  }

  public static String computeProtocolObjectGroupAclName(String name) {
    return String.format("~PROTOCOL_OBJECT_GROUP~%s~", name);
  }

  public static String computeServiceObjectGroupAclName(String name) {
    return String.format("~SERVICE_OBJECT_GROUP~%s~", name);
  }

  @Override
  public String canonicalizeInterfaceName(String ifaceName) {
    Matcher matcher = Pattern.compile("[A-Za-z][-A-Za-z0-9]*[A-Za-z]").matcher(ifaceName);
    if (matcher.find()) {
      String ifacePrefix = matcher.group();
      String canonicalPrefix = getCanonicalInterfaceNamePrefix(ifacePrefix);
      String suffix = ifaceName.substring(ifacePrefix.length());
      return canonicalPrefix + suffix;
    }
    throw new BatfishException("Invalid interface name: '" + ifaceName + "'");
  }

  public static String getCanonicalInterfaceNamePrefix(String prefix) {
    for (Entry<String, String> e : CISCO_INTERFACE_PREFIXES.entrySet()) {
      String matchPrefix = e.getKey();
      String canonicalPrefix = e.getValue();
      if (matchPrefix.toLowerCase().startsWith(prefix.toLowerCase())) {
        return canonicalPrefix;
      }
    }
    throw new BatfishException("Invalid interface name prefix: '" + prefix + "'");
  }

  private static String getRouteMapClausePolicyName(RouteMap map, int continueTarget) {
    String mapName = map.getName();
    String clausePolicyName = "~RMCLAUSE~" + mapName + "~" + continueTarget + "~";
    return clausePolicyName;
  }

  static String toJavaRegex(String ciscoRegex) {
    String withoutQuotes;
    if (ciscoRegex.charAt(0) == '"' && ciscoRegex.charAt(ciscoRegex.length() - 1) == '"') {
      withoutQuotes = ciscoRegex.substring(1, ciscoRegex.length() - 1);
    } else {
      withoutQuotes = ciscoRegex;
    }
    String underscoreReplacement = "(,|\\\\{|\\\\}|^|\\$| )";
    String output = withoutQuotes.replaceAll("_", underscoreReplacement);
    return output;
  }

  private final Map<String, IpAsPathAccessList> _asPathAccessLists;

  private final Map<String, AsPathSet> _asPathSets;

  private final CiscoFamily _cf;

  private final List<Ip> _dhcpRelayServers;

  private NavigableSet<String> _dnsServers;

  private String _dnsSourceInterface;

  private String _domainName;

  private final Map<String, ExpandedCommunityList> _expandedCommunityLists;

  private final Map<String, ExtendedAccessList> _extendedAccessLists;

  private final Map<String, ExtendedIpv6AccessList> _extendedIpv6AccessLists;

  private boolean _failover;

  private String _failoverCommunicationInterface;

  private String _failoverCommunicationInterfaceAlias;

  private final Map<String, String> _failoverInterfaces;

  private final Map<String, InterfaceAddress> _failoverPrimaryAddresses;

  private boolean _failoverSecondary;

  private final Map<String, InterfaceAddress> _failoverStandbyAddresses;

  private String _failoverStatefulSignalingInterface;

  private String _failoverStatefulSignalingInterfaceAlias;

  private String _hostname;

  private final Map<String, InspectClassMap> _inspectClassMaps;

  private final Map<String, InspectPolicyMap> _inspectPolicyMaps;

  private final Map<String, Interface> _interfaces;

  private final Map<String, IpsecProfile> _ipsecProfiles;

  private final Map<String, IpsecTransformSet> _ipsecTransformSets;

  private final Map<String, IsakmpPolicy> _isakmpPolicies;

  private final Map<String, IsakmpProfile> _isakmpProfiles;

  private final Map<String, Keyring> _keyrings;

  private final Map<String, MacAccessList> _macAccessLists;

  private final Map<String, NatPool> _natPools;

  private final Map<String, NetworkObjectGroup> _networkObjectGroups;

  private String _ntpSourceInterface;

  private CiscoNxBgpGlobalConfiguration _nxBgpGlobalConfiguration;

  private final Map<String, ObjectGroup> _objectGroups;

  private final Map<String, Prefix6List> _prefix6Lists;

  private final Map<String, PrefixList> _prefixLists;

  private final Map<String, ProtocolObjectGroup> _protocolObjectGroups;

  private final Map<String, RouteMap> _routeMaps;

  private final Map<String, RoutePolicy> _routePolicies;

  private SnmpServer _snmpServer;

  private String _snmpSourceInterface;

  private boolean _spanningTreePortfastDefault;

  private final Map<String, StandardAccessList> _standardAccessLists;

  private final Map<String, StandardCommunityList> _standardCommunityLists;

  private final Map<String, StandardIpv6AccessList> _standardIpv6AccessLists;

  private NavigableSet<String> _tacacsServers;

  private String _tacacsSourceInterface;

  private final SortedMap<String, Integer> _undefinedPeerGroups;

  private transient Set<String> _unimplementedFeatures;

  private transient Set<NamedBgpPeerGroup> _unusedPeerGroups;

  private transient Set<NamedBgpPeerGroup> _unusedPeerSessions;

  private ConfigurationFormat _vendor;

  private final Map<String, Vrf> _vrfs;

  private final SortedMap<String, VrrpInterface> _vrrpGroups;

  private final Map<String, ServiceObjectGroup> _serviceObjectGroups;

  private final Map<String, Map<String, SecurityZonePair>> _securityZonePairs;

  private final Map<String, SecurityZone> _securityZones;

  public CiscoConfiguration(Set<String> unimplementedFeatures) {
    _asPathAccessLists = new TreeMap<>();
    _asPathSets = new TreeMap<>();
    _cf = new CiscoFamily();
    _dhcpRelayServers = new ArrayList<>();
    _dnsServers = new TreeSet<>();
    _expandedCommunityLists = new TreeMap<>();
    _extendedAccessLists = new TreeMap<>();
    _extendedIpv6AccessLists = new TreeMap<>();
    _failoverInterfaces = new TreeMap<>();
    _failoverPrimaryAddresses = new TreeMap<>();
    _failoverStandbyAddresses = new TreeMap<>();
    _isakmpPolicies = new TreeMap<>();
    _isakmpProfiles = new TreeMap<>();
    _inspectClassMaps = new TreeMap<>();
    _inspectPolicyMaps = new TreeMap<>();
    _interfaces = new TreeMap<>();
    _ipsecTransformSets = new TreeMap<>();
    _ipsecProfiles = new TreeMap<>();
    _keyrings = new TreeMap<>();
    _macAccessLists = new TreeMap<>();
    _natPools = new TreeMap<>();
    _networkObjectGroups = new TreeMap<>();
    _nxBgpGlobalConfiguration = new CiscoNxBgpGlobalConfiguration();
    _objectGroups = new TreeMap<>();
    _prefixLists = new TreeMap<>();
    _prefix6Lists = new TreeMap<>();
    _protocolObjectGroups = new TreeMap<>();
    _routeMaps = new TreeMap<>();
    _routePolicies = new TreeMap<>();
    _securityZonePairs = new TreeMap<>();
    _securityZones = new TreeMap<>();
    _serviceObjectGroups = new TreeMap<>();
    _standardAccessLists = new TreeMap<>();
    _standardIpv6AccessLists = new TreeMap<>();
    _standardCommunityLists = new TreeMap<>();
    _tacacsServers = new TreeSet<>();
    _undefinedPeerGroups = new TreeMap<>();
    _unimplementedFeatures = unimplementedFeatures;
    _vrfs = new TreeMap<>();
    _vrfs.put(Configuration.DEFAULT_VRF_NAME, new Vrf(Configuration.DEFAULT_VRF_NAME));
    _vrrpGroups = new TreeMap<>();
  }

  private void applyVrrp(Configuration c) {
    _vrrpGroups.forEach(
        (ifaceName, vrrpInterface) -> {
          org.batfish.datamodel.Interface iface = c.getInterfaces().get(ifaceName);
          if (iface != null) {
            vrrpInterface
                .getVrrpGroups()
                .forEach(
                    (groupNum, vrrpGroup) -> {
                      org.batfish.datamodel.VrrpGroup newGroup =
                          new org.batfish.datamodel.VrrpGroup(groupNum);
                      newGroup.setPreempt(vrrpGroup.getPreempt());
                      newGroup.setPriority(vrrpGroup.getPriority());
                      InterfaceAddress ifaceAddress = iface.getAddress();
                      if (ifaceAddress != null) {
                        int prefixLength = ifaceAddress.getNetworkBits();
                        Ip address = vrrpGroup.getVirtualAddress();
                        if (address != null) {
                          InterfaceAddress virtualAddress =
                              new InterfaceAddress(address, prefixLength);
                          newGroup.setVirtualAddress(virtualAddress);
                        } else {
                          _w.redFlag(
                              "No virtual address set for VRRP on interface: '" + ifaceName + "'");
                        }
                      } else {
                        _w.redFlag(
                            "Could not determine prefix length of VRRP address on interface '"
                                + ifaceName
                                + "' due to missing prefix");
                      }
                      iface.getVrrpGroups().put(groupNum, newGroup);
                    });
          }
        });
  }

  private static WithEnvironmentExpr bgpRedistributeWithEnvironmentExpr(
      BooleanExpr expr, OriginType originType) {
    WithEnvironmentExpr we = new WithEnvironmentExpr();
    we.setExpr(expr);
    we.getPreStatements().add(Statements.SetWriteIntermediateBgpAttributes.toStaticStatement());
    we.getPostStatements().add(Statements.UnsetWriteIntermediateBgpAttributes.toStaticStatement());
    we.getPostTrueStatements().add(Statements.SetReadIntermediateBgpAttributes.toStaticStatement());
    we.getPostTrueStatements().add(new SetOrigin(new LiteralOrigin(originType, null)));
    return we;
  }

  private boolean containsIpAccessList(String eaListName, String mapName) {
    if (mapName != null) {
      RouteMap currentMap = _routeMaps.get(mapName);
      if (currentMap != null) {
        for (RouteMapClause clause : currentMap.getClauses().values()) {
          for (RouteMapMatchLine matchLine : clause.getMatchList()) {
            if (matchLine instanceof RouteMapMatchIpAccessListLine) {
              RouteMapMatchIpAccessListLine ipall = (RouteMapMatchIpAccessListLine) matchLine;
              for (String listName : ipall.getListNames()) {
                if (eaListName.equals(listName)) {
                  return true;
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  private boolean containsIpv6AccessList(String eaListName, String mapName) {
    if (mapName != null) {
      RouteMap currentMap = _routeMaps.get(mapName);
      if (currentMap != null) {
        for (RouteMapClause clause : currentMap.getClauses().values()) {
          for (RouteMapMatchLine matchLine : clause.getMatchList()) {
            if (matchLine instanceof RouteMapMatchIpv6AccessListLine) {
              RouteMapMatchIpv6AccessListLine ipall = (RouteMapMatchIpv6AccessListLine) matchLine;
              for (String listName : ipall.getListNames()) {
                if (eaListName.equals(listName)) {
                  return true;
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  private static void convertForPurpose(Set<RouteMap> routingRouteMaps, RouteMap map) {
    if (routingRouteMaps.contains(map)) {
      for (RouteMapClause clause : map.getClauses().values()) {
        List<RouteMapMatchLine> matchList = clause.getMatchList();
        for (RouteMapMatchLine line : matchList) {
          if (line instanceof RouteMapMatchIpAccessListLine) {
            RouteMapMatchIpAccessListLine matchIpAccessListLine =
                (RouteMapMatchIpAccessListLine) line;
            matchIpAccessListLine.setRouting(true);
          }
        }
      }
    }
  }

  public Map<String, IpAsPathAccessList> getAsPathAccessLists() {
    return _asPathAccessLists;
  }

  public Map<String, AsPathSet> getAsPathSets() {
    return _asPathSets;
  }

  private Ip getBgpRouterId(final Configuration c, String vrfName, BgpProcess proc) {
    Ip routerId;
    Ip processRouterId = proc.getRouterId();
    org.batfish.datamodel.Vrf vrf = c.getVrfs().get(vrfName);
    if (processRouterId == null) {
      processRouterId = _vrfs.get(Configuration.DEFAULT_VRF_NAME).getBgpProcess().getRouterId();
    }
    if (processRouterId == null) {
      processRouterId = Ip.ZERO;
      for (Entry<String, org.batfish.datamodel.Interface> e : vrf.getInterfaces().entrySet()) {
        String iname = e.getKey();
        org.batfish.datamodel.Interface iface = e.getValue();
        if (iname.startsWith("Loopback")) {
          InterfaceAddress address = iface.getAddress();
          if (address != null) {
            Ip currentIp = address.getIp();
            if (currentIp.asLong() > processRouterId.asLong()) {
              processRouterId = currentIp;
            }
          }
        }
      }
      if (processRouterId.equals(Ip.ZERO)) {
        for (org.batfish.datamodel.Interface currentInterface : vrf.getInterfaces().values()) {
          InterfaceAddress address = currentInterface.getAddress();
          if (address != null) {
            Ip currentIp = address.getIp();
            if (currentIp.asLong() > processRouterId.asLong()) {
              processRouterId = currentIp;
            }
          }
        }
      }
    }
    routerId = processRouterId;
    return routerId;
  }

  public CiscoFamily getCf() {
    return _cf;
  }

  public Vrf getDefaultVrf() {
    return _vrfs.get(Configuration.DEFAULT_VRF_NAME);
  }

  public List<Ip> getDhcpRelayServers() {
    return _dhcpRelayServers;
  }

  public NavigableSet<String> getDnsServers() {
    return _dnsServers;
  }

  public String getDnsSourceInterface() {
    return _dnsSourceInterface;
  }

  public Map<String, ExpandedCommunityList> getExpandedCommunityLists() {
    return _expandedCommunityLists;
  }

  public Map<String, ExtendedAccessList> getExtendedAcls() {
    return _extendedAccessLists;
  }

  public Map<String, ExtendedIpv6AccessList> getExtendedIpv6Acls() {
    return _extendedIpv6AccessLists;
  }

  public boolean getFailover() {
    return _failover;
  }

  public String getFailoverCommunicationInterface() {
    return _failoverCommunicationInterface;
  }

  public String getFailoverCommunicationInterfaceAlias() {
    return _failoverCommunicationInterfaceAlias;
  }

  public Map<String, String> getFailoverInterfaces() {
    return _failoverInterfaces;
  }

  public Map<String, InterfaceAddress> getFailoverPrimaryAddresses() {
    return _failoverPrimaryAddresses;
  }

  public boolean getFailoverSecondary() {
    return _failoverSecondary;
  }

  public Map<String, InterfaceAddress> getFailoverStandbyAddresses() {
    return _failoverStandbyAddresses;
  }

  public String getFailoverStatefulSignalingInterface() {
    return _failoverStatefulSignalingInterface;
  }

  public String getFailoverStatefulSignalingInterfaceAlias() {
    return _failoverStatefulSignalingInterfaceAlias;
  }

  @Override
  public String getHostname() {
    return _hostname;
  }

  private @Nullable Interface getInterfaceByTunnelAddresses(Ip sourceAddress, Prefix destPrefix) {
    for (Interface iface : _interfaces.values()) {
      Tunnel tunnel = iface.getTunnel();
      if (tunnel != null
          && tunnel.getSourceAddress() != null
          && tunnel.getSourceAddress().equals(sourceAddress)
          && tunnel.getDestination() != null
          && destPrefix.containsIp(tunnel.getDestination())) {
        /*
         * We found a tunnel interface with the required parameters. Now return the external
         * interface with this address.
         */
        return _interfaces
            .values()
            .stream()
            .filter(
                i -> i.getAllAddresses().stream().anyMatch(p -> p.getIp().equals(sourceAddress)))
            .findFirst()
            .orElse(null);
      }
    }
    return null;
  }

  public Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public Map<String, IpsecProfile> getIpsecProfiles() {
    return _ipsecProfiles;
  }

  public Map<String, IpsecTransformSet> getIpsecTransformSets() {
    return _ipsecTransformSets;
  }

  public Map<String, IsakmpPolicy> getIsakmpPolicies() {
    return _isakmpPolicies;
  }

  public Map<String, IsakmpProfile> getIsakmpProfiles() {
    return _isakmpProfiles;
  }

  public Map<String, Keyring> getKeyrings() {
    return _keyrings;
  }

  public Map<String, MacAccessList> getMacAccessLists() {
    return _macAccessLists;
  }

  public Map<String, NatPool> getNatPools() {
    return _natPools;
  }

  public String getNtpSourceInterface() {
    return _ntpSourceInterface;
  }

  public CiscoNxBgpGlobalConfiguration getNxBgpGlobalConfiguration() {
    return _nxBgpGlobalConfiguration;
  }

  public Map<String, Prefix6List> getPrefix6Lists() {
    return _prefix6Lists;
  }

  public Map<String, PrefixList> getPrefixLists() {
    return _prefixLists;
  }

  public Map<String, RouteMap> getRouteMaps() {
    return _routeMaps;
  }

  public Map<String, RoutePolicy> getRoutePolicies() {
    return _routePolicies;
  }

  private Set<RouteMap> getRoutingRouteMaps() {
    Set<RouteMap> maps = new LinkedHashSet<>();
    String currentMapName;
    RouteMap currentMap;
    // check ospf policies
    for (Vrf vrf : _vrfs.values()) {
      OspfProcess ospfProcess = vrf.getOspfProcess();
      if (ospfProcess != null) {
        for (OspfRedistributionPolicy rp : ospfProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (currentMapName != null) {
            currentMap = _routeMaps.get(currentMapName);
            if (currentMap != null) {
              maps.add(currentMap);
            }
          }
        }
        currentMapName = ospfProcess.getDefaultInformationOriginateMap();
        if (currentMapName != null) {
          currentMap = _routeMaps.get(currentMapName);
          if (currentMap != null) {
            maps.add(currentMap);
          }
        }
      }
      // check bgp policies
      BgpProcess bgpProcess = vrf.getBgpProcess();
      if (bgpProcess != null) {
        for (BgpRedistributionPolicy rp : bgpProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (currentMapName != null) {
            currentMap = _routeMaps.get(currentMapName);
            if (currentMap != null) {
              maps.add(currentMap);
            }
          }
        }
        for (BgpPeerGroup pg : bgpProcess.getAllPeerGroups()) {
          currentMapName = pg.getInboundRouteMap();
          if (currentMapName != null) {
            currentMap = _routeMaps.get(currentMapName);
            if (currentMap != null) {
              maps.add(currentMap);
            }
          }
          currentMapName = pg.getInboundRoute6Map();
          if (currentMapName != null) {
            currentMap = _routeMaps.get(currentMapName);
            if (currentMap != null) {
              maps.add(currentMap);
            }
          }
          currentMapName = pg.getOutboundRouteMap();
          if (currentMapName != null) {
            currentMap = _routeMaps.get(currentMapName);
            if (currentMap != null) {
              maps.add(currentMap);
            }
          }
          currentMapName = pg.getOutboundRoute6Map();
          if (currentMapName != null) {
            currentMap = _routeMaps.get(currentMapName);
            if (currentMap != null) {
              maps.add(currentMap);
            }
          }
        }
      }
    }
    return maps;
  }

  public SnmpServer getSnmpServer() {
    return _snmpServer;
  }

  public String getSnmpSourceInterface() {
    return _snmpSourceInterface;
  }

  public boolean getSpanningTreePortfastDefault() {
    return _spanningTreePortfastDefault;
  }

  public Map<String, StandardAccessList> getStandardAcls() {
    return _standardAccessLists;
  }

  public Map<String, StandardCommunityList> getStandardCommunityLists() {
    return _standardCommunityLists;
  }

  public Map<String, StandardIpv6AccessList> getStandardIpv6Acls() {
    return _standardIpv6AccessLists;
  }

  public NavigableSet<String> getTacacsServers() {
    return _tacacsServers;
  }

  public String getTacacsSourceInterface() {
    return _tacacsSourceInterface;
  }

  public SortedMap<String, Integer> getUndefinedPeerGroups() {
    return _undefinedPeerGroups;
  }

  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
  }

  private Ip getUpdateSource(
      Configuration c,
      String vrfName,
      LeafBgpPeerGroup lpg,
      String updateSourceInterface,
      boolean ipv4) {
    Ip updateSource = null;
    org.batfish.datamodel.Vrf vrf = c.getVrfs().get(vrfName);
    if (ipv4) {
      if (updateSourceInterface != null) {
        org.batfish.datamodel.Interface sourceInterface =
            vrf.getInterfaces().get(updateSourceInterface);
        if (sourceInterface != null) {
          InterfaceAddress address = sourceInterface.getAddress();
          if (address != null) {
            Ip sourceIp = address.getIp();
            updateSource = sourceIp;
          } else {
            _w.redFlag(
                "bgp update source interface: '"
                    + updateSourceInterface
                    + "' not assigned an ip address");
          }
        }
      } else {
        if (lpg instanceof DynamicIpBgpPeerGroup) {
          updateSource = Ip.AUTO;
        } else {
          Ip neighborAddress = lpg.getNeighborPrefix().getStartIp();
          for (org.batfish.datamodel.Interface iface : vrf.getInterfaces().values()) {
            for (InterfaceAddress interfaceAddress : iface.getAllAddresses()) {
              if (interfaceAddress.getPrefix().containsIp(neighborAddress)) {
                Ip ifaceAddress = interfaceAddress.getIp();
                updateSource = ifaceAddress;
              }
            }
          }
        }
      }
      if (updateSource == null && lpg.getNeighborPrefix().getStartIp().valid()) {
        _w.redFlag("Could not determine update source for BGP neighbor: '" + lpg.getName() + "'");
      }
    }
    return updateSource;
  }

  public ConfigurationFormat getVendor() {
    return _vendor;
  }

  public Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  public SortedMap<String, VrrpInterface> getVrrpGroups() {
    return _vrrpGroups;
  }

  private void markAcls(CiscoStructureUsage... usages) {
    for (CiscoStructureUsage usage : usages) {
      markAbstractStructure(
          CiscoStructureType.IP_ACCESS_LIST,
          usage,
          ImmutableList.of(
              CiscoStructureType.IPV4_ACCESS_LIST_STANDARD,
              CiscoStructureType.IPV4_ACCESS_LIST_EXTENDED,
              CiscoStructureType.IPV6_ACCESS_LIST_STANDARD,
              CiscoStructureType.IPV6_ACCESS_LIST_EXTENDED));
    }
  }

  private void markIpOrMacAcls(CiscoStructureUsage... usages) {
    for (CiscoStructureUsage usage : usages) {
      markAbstractStructure(
          CiscoStructureType.ACCESS_LIST,
          usage,
          Arrays.asList(
              CiscoStructureType.IPV4_ACCESS_LIST_EXTENDED,
              CiscoStructureType.IPV4_ACCESS_LIST_STANDARD,
              CiscoStructureType.IPV6_ACCESS_LIST_EXTENDED,
              CiscoStructureType.IPV6_ACCESS_LIST_STANDARD,
              CiscoStructureType.MAC_ACCESS_LIST));
    }
  }

  private void markIpv4Acls(CiscoStructureUsage... usages) {
    for (CiscoStructureUsage usage : usages) {
      markAbstractStructure(
          CiscoStructureType.IPV4_ACCESS_LIST,
          usage,
          ImmutableList.of(
              CiscoStructureType.IPV4_ACCESS_LIST_STANDARD,
              CiscoStructureType.IPV4_ACCESS_LIST_EXTENDED));
    }
  }

  private void markIpv6Acls(CiscoStructureUsage... usages) {
    for (CiscoStructureUsage usage : usages) {
      markAbstractStructure(
          CiscoStructureType.IPV6_ACCESS_LIST,
          usage,
          ImmutableList.of(
              CiscoStructureType.IPV6_ACCESS_LIST_STANDARD,
              CiscoStructureType.IPV6_ACCESS_LIST_EXTENDED));
    }
  }

  private void processFailoverSettings() {
    if (_failover) {
      Interface commIface;
      InterfaceAddress commAddress;
      Interface sigIface;
      InterfaceAddress sigAddress;
      if (_failoverSecondary) {
        commIface = _interfaces.get(_failoverCommunicationInterface);
        commAddress = _failoverStandbyAddresses.get(_failoverCommunicationInterfaceAlias);
        sigIface = _interfaces.get(_failoverStatefulSignalingInterface);
        sigAddress = _failoverStandbyAddresses.get(_failoverStatefulSignalingInterfaceAlias);
        for (Interface iface : _interfaces.values()) {
          iface.setAddress(iface.getStandbyAddress());
        }
      } else {
        commIface = _interfaces.get(_failoverCommunicationInterface);
        commAddress = _failoverPrimaryAddresses.get(_failoverCommunicationInterfaceAlias);
        sigIface = _interfaces.get(_failoverStatefulSignalingInterface);
        sigAddress = _failoverPrimaryAddresses.get(_failoverStatefulSignalingInterfaceAlias);
      }
      commIface.setAddress(commAddress);
      commIface.setActive(true);
      sigIface.setAddress(sigAddress);
      sigIface.setActive(true);
    }
  }

  private void processLines() {
    // nxos does not have 'login authentication' for lines, so just have it
    // use default list if one exists
    if (_vendor == ConfigurationFormat.CISCO_NX
        && _cf.getAaa() != null
        && _cf.getAaa().getAuthentication() != null
        && _cf.getAaa().getAuthentication().getLogin() != null
        && _cf.getAaa()
                .getAuthentication()
                .getLogin()
                .getLists()
                .get(AaaAuthenticationLogin.DEFAULT_LIST_NAME)
            != null) {
      for (Line line : _cf.getLines().values()) {
        line.setLoginAuthentication(AaaAuthenticationLogin.DEFAULT_LIST_NAME);
      }
    }
  }

  public void setDnsSourceInterface(String dnsSourceInterface) {
    _dnsSourceInterface = dnsSourceInterface;
  }

  public void setDomainName(String domainName) {
    _domainName = domainName;
  }

  public void setFailover(boolean failover) {
    _failover = failover;
  }

  public void setFailoverCommunicationInterface(String failoverCommunicationInterface) {
    _failoverCommunicationInterface = failoverCommunicationInterface;
  }

  public void setFailoverCommunicationInterfaceAlias(String failoverCommunicationInterfaceAlias) {
    _failoverCommunicationInterfaceAlias = failoverCommunicationInterfaceAlias;
  }

  public void setFailoverSecondary(boolean failoverSecondary) {
    _failoverSecondary = failoverSecondary;
  }

  public void setFailoverStatefulSignalingInterface(String failoverStatefulSignalingInterface) {
    _failoverStatefulSignalingInterface = failoverStatefulSignalingInterface;
  }

  public void setFailoverStatefulSignalingInterfaceAlias(
      String failoverStatefulSignalingInterfaceAlias) {
    _failoverStatefulSignalingInterfaceAlias = failoverStatefulSignalingInterfaceAlias;
  }

  @Override
  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  public void setNtpSourceInterface(String ntpSourceInterface) {
    _ntpSourceInterface = ntpSourceInterface;
  }

  public void setSnmpServer(SnmpServer snmpServer) {
    _snmpServer = snmpServer;
  }

  public void setSnmpSourceInterface(String snmpSourceInterface) {
    _snmpSourceInterface = snmpSourceInterface;
  }

  public void setSpanningTreePortfastDefault(boolean spanningTreePortfastDefault) {
    _spanningTreePortfastDefault = spanningTreePortfastDefault;
  }

  public void setTacacsSourceInterface(String tacacsSourceInterface) {
    _tacacsSourceInterface = tacacsSourceInterface;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _vendor = format;
  }

  private org.batfish.datamodel.BgpProcess toNxBgpProcess(
      Configuration c,
      CiscoNxBgpGlobalConfiguration nxBgpGlobal,
      CiscoNxBgpVrfConfiguration nxBgpVrf,
      String vrfName) {
    org.batfish.datamodel.BgpProcess newBgpProcess = new org.batfish.datamodel.BgpProcess();
    org.batfish.datamodel.Vrf v = c.getVrfs().get(vrfName);

    if (nxBgpVrf.getBestpathCompareRouterId()) {
      newBgpProcess.setTieBreaker(BgpTieBreaker.ROUTER_ID);
    }

    newBgpProcess.setRouterId(CiscoNxConversions.getNxBgpRouterId(nxBgpVrf, v, _w));

    // From NX-OS docs for `bestpath as-path multipath-relax`
    //  Allows load sharing across providers with different (but equal-length) autonomous system
    //  paths. Without this option, the AS paths must be identical for load sharing.
    newBgpProcess.setMultipathEquivalentAsPathMatchMode(
        nxBgpVrf.getBestpathAsPathMultipathRelax() ? PATH_LENGTH : EXACT_PATH);

    // Process vrf-level address family configuration, such as export policy.
    CiscoNxBgpVrfAddressFamilyConfiguration ipv4af = nxBgpVrf.getIpv4UnicastAddressFamily();
    if (ipv4af != null) {
      // Batfish seems to only track the IPv4 properties for multipath ebgp/ibgp.
      newBgpProcess.setMultipathEbgp(
          ipv4af.getMaximumPathsEbgp() > 1 || nxBgpVrf.getBestpathAsPathMultipathRelax());
      newBgpProcess.setMultipathIbgp(ipv4af.getMaximumPathsIbgp() > 1);
    } else {
      newBgpProcess.setMultipathEbgp(nxBgpVrf.getBestpathAsPathMultipathRelax());
    }

    // Next we build up the BGP common export policy.
    RoutingPolicy bgpCommonExportPolicy =
        new RoutingPolicy(computeBgpCommonExportPolicyName(vrfName), c);
    c.getRoutingPolicies().put(bgpCommonExportPolicy.getName(), bgpCommonExportPolicy);

    // 1. If there are any ipv4 summary only networks, do not export the more specific routes.
    if (ipv4af != null) {
      Stream<Prefix> summaryOnlyNetworks =
          ipv4af
              .getAggregateNetworks()
              .entrySet()
              .stream()
              .filter(e -> e.getValue().getSummaryOnly())
              .map(Entry::getKey);
      If suppressLonger = suppressSummarizedPrefixes(c, vrfName, summaryOnlyNetworks);
      if (suppressLonger != null) {
        bgpCommonExportPolicy.getStatements().add(suppressLonger);
      }
    }

    // The body of the export policy is a huge disjunction over many reasons routes may be exported.
    Disjunction routesShouldBeExported = new Disjunction();
    bgpCommonExportPolicy
        .getStatements()
        .add(
            new If(
                routesShouldBeExported,
                ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                ImmutableList.of()));
    // This list of reasons to export a route will be built up over the remainder of this function.
    List<BooleanExpr> exportConditions = routesShouldBeExported.getDisjuncts();

    // Generate and distribute aggregate routes.
    if (ipv4af != null) {
      for (Entry<Prefix, CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration> e :
          ipv4af.getAggregateNetworks().entrySet()) {
        Prefix prefix = e.getKey();
        CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration agg = e.getValue();
        RoutingPolicy generatedPolicy = generateAggregateRoutePolicy(c, vrfName, prefix);

        GeneratedRoute.Builder gr =
            new GeneratedRoute.Builder()
                .setNetwork(prefix)
                .setAdmin(CISCO_AGGREGATE_ROUTE_ADMIN_COST)
                .setGenerationPolicy(generatedPolicy.getName())
                .setDiscard(true);

        // Conditions to generate this route
        List<BooleanExpr> generateAggregateConditions = new ArrayList<>();
        generateAggregateConditions.add(
            new MatchPrefixSet(
                new DestinationNetwork(),
                new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(prefix)))));
        generateAggregateConditions.add(new MatchProtocol(RoutingProtocol.AGGREGATE));

        // If defined, set attribute map for aggregate network
        BooleanExpr weInterior = BooleanExprs.TRUE;
        String attributeMapName = agg.getAttributeMap();
        if (attributeMapName != null) {
          RouteMap attributeMap = _routeMaps.get(attributeMapName);
          if (attributeMap != null) {
            // need to apply attribute changes if this specific route is matched
            weInterior = new CallExpr(attributeMapName);
            gr.setAttributePolicy(attributeMapName);
          }
        }
        generateAggregateConditions.add(
            bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.IGP));

        v.getGeneratedRoutes().add(gr.build());
        // Do export a generated aggregate.
        exportConditions.add(new Conjunction(generateAggregateConditions));
      }
    }

    // Only redistribute default route if `default-information originate` is set.
    BooleanExpr redistributeDefaultRoute =
        ipv4af == null || !ipv4af.getDefaultInformationOriginate()
            ? NOT_DEFAULT_ROUTE
            : BooleanExprs.TRUE;

    // Export RIP routes that should be redistributed.
    CiscoNxBgpRedistributionPolicy ripPolicy =
        ipv4af == null ? null : ipv4af.getRedistributionPolicy(RoutingProtocol.RIP);
    if (ripPolicy != null) {
      String routeMap = ripPolicy.getRouteMap();
      RouteMap map = _routeMaps.get(routeMap);
      /* TODO: how do we match on source tag (aka RIP process id)? */
      List<BooleanExpr> conditions =
          ImmutableList.of(
              new MatchProtocol(RoutingProtocol.RIP),
              redistributeDefaultRoute,
              bgpRedistributeWithEnvironmentExpr(
                  map == null ? BooleanExprs.TRUE : new CallExpr(routeMap), OriginType.INCOMPLETE));
      Conjunction rip = new Conjunction(conditions);
      rip.setComment("Redistribute RIP routes into BGP");
      exportConditions.add(rip);
    }

    // Export static routes that should be redistributed.
    CiscoNxBgpRedistributionPolicy staticPolicy =
        ipv4af == null ? null : ipv4af.getRedistributionPolicy(RoutingProtocol.STATIC);
    if (staticPolicy != null) {
      String routeMap = staticPolicy.getRouteMap();
      RouteMap map = _routeMaps.get(routeMap);
      List<BooleanExpr> conditions =
          ImmutableList.of(
              new MatchProtocol(RoutingProtocol.STATIC),
              redistributeDefaultRoute,
              bgpRedistributeWithEnvironmentExpr(
                  map == null ? BooleanExprs.TRUE : new CallExpr(routeMap), OriginType.INCOMPLETE));
      Conjunction staticRedist = new Conjunction(conditions);
      staticRedist.setComment("Redistribute static routes into BGP");
      exportConditions.add(staticRedist);
    }

    // Export connected routes that should be redistributed.
    CiscoNxBgpRedistributionPolicy connectedPolicy =
        ipv4af == null ? null : ipv4af.getRedistributionPolicy(RoutingProtocol.CONNECTED);
    if (connectedPolicy != null) {
      String routeMap = connectedPolicy.getRouteMap();
      RouteMap map = _routeMaps.get(routeMap);
      List<BooleanExpr> conditions =
          ImmutableList.of(
              new MatchProtocol(RoutingProtocol.CONNECTED),
              redistributeDefaultRoute,
              bgpRedistributeWithEnvironmentExpr(
                  map == null ? BooleanExprs.TRUE : new CallExpr(routeMap), OriginType.INCOMPLETE));
      Conjunction connected = new Conjunction(conditions);
      connected.setComment("Redistribute connected routes into BGP");
      exportConditions.add(connected);
    }

    // Export OSPF routes that should be redistributed.
    CiscoNxBgpRedistributionPolicy ospfPolicy =
        ipv4af == null ? null : ipv4af.getRedistributionPolicy(RoutingProtocol.OSPF);
    if (ospfPolicy != null) {
      String routeMap = ospfPolicy.getRouteMap();
      RouteMap map = _routeMaps.get(routeMap);
      /* TODO: how do we match on source tag (aka OSPF process)? */
      List<BooleanExpr> conditions =
          ImmutableList.of(
              new MatchProtocol(RoutingProtocol.OSPF),
              redistributeDefaultRoute,
              bgpRedistributeWithEnvironmentExpr(
                  map == null ? BooleanExprs.TRUE : new CallExpr(routeMap), OriginType.INCOMPLETE));
      Conjunction ospf = new Conjunction(conditions);
      ospf.setComment("Redistribute OSPF routes into BGP");
      exportConditions.add(ospf);
    }

    // Now we add all the per-network export policies.
    if (ipv4af != null) {
      ipv4af
          .getIpNetworks()
          .forEach(
              (prefix, routeMapOrEmpty) -> {
                PrefixSpace exportSpace = new PrefixSpace(PrefixRange.fromPrefix(prefix));
                List<BooleanExpr> exportNetworkConditions =
                    ImmutableList.of(
                        new MatchPrefixSet(
                            new DestinationNetwork(), new ExplicitPrefixSet(exportSpace)),
                        new Not(new MatchProtocol(RoutingProtocol.BGP)),
                        new Not(new MatchProtocol(RoutingProtocol.IBGP)),
                        new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)),
                        bgpRedistributeWithEnvironmentExpr(
                            _routeMaps.containsKey(routeMapOrEmpty)
                                ? new CallExpr(routeMapOrEmpty)
                                : BooleanExprs.TRUE,
                            OriginType.IGP));
                newBgpProcess.addToOriginationSpace(exportSpace);
                exportConditions.add(new Conjunction(exportNetworkConditions));
              });
    }

    CiscoNxBgpVrfAddressFamilyConfiguration ipv6af = nxBgpVrf.getIpv6UnicastAddressFamily();
    if (ipv6af != null) {
      ipv6af
          .getIpv6Networks()
          .forEach(
              (prefix6, routeMapOrEmpty) -> {
                List<BooleanExpr> exportNetworkConditions =
                    ImmutableList.of(
                        new MatchPrefix6Set(
                            new DestinationNetwork6(),
                            new ExplicitPrefix6Set(
                                new Prefix6Space(Prefix6Range.fromPrefix6(prefix6)))),
                        new Not(new MatchProtocol(RoutingProtocol.BGP)),
                        new Not(new MatchProtocol(RoutingProtocol.IBGP)),
                        new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)),
                        bgpRedistributeWithEnvironmentExpr(
                            _routeMaps.containsKey(routeMapOrEmpty)
                                ? new CallExpr(routeMapOrEmpty)
                                : BooleanExprs.TRUE,
                            OriginType.IGP));
                exportConditions.add(new Conjunction(exportNetworkConditions));
              });
    }

    // Always export BGP or IBGP routes.
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP));
    exportConditions.add(new MatchProtocol(RoutingProtocol.IBGP));

    // Finally, the export policy ends with returning false: do not export unmatched routes.
    bgpCommonExportPolicy.getStatements().add(Statements.ReturnFalse.toStaticStatement());

    // Generate BGP_NETWORK6_NETWORKS filter.
    if (ipv6af != null) {
      List<Route6FilterLine> lines =
          ipv6af
              .getIpv6Networks()
              .keySet()
              .stream()
              .map(p6 -> new Route6FilterLine(LineAction.ACCEPT, Prefix6Range.fromPrefix6(p6)))
              .collect(ImmutableList.toImmutableList());
      Route6FilterList localFilter6 =
          new Route6FilterList("~BGP_NETWORK6_NETWORKS_FILTER:" + vrfName + "~", lines);
      c.getRoute6FilterLists().put(localFilter6.getName(), localFilter6);
    }

    // Before we process any neighbors, execute the inheritance.
    nxBgpGlobal.doInherit(_w);

    // This is ugly logic to handle the fact that BgpNeighbor does not currently support
    // separate tracking of active and passive neighbors.
    SortedMap<Prefix, BgpNeighbor> newNeighbors = new TreeMap<>();
    // Process active neighbors first.
    Map<Ip, BgpNeighbor> activeNeighbors =
        CiscoNxConversions.getNeighbors(c, v, newBgpProcess, nxBgpGlobal, nxBgpVrf, _w);
    activeNeighbors.forEach(
        (key, value) -> newNeighbors.put(new Prefix(key, Prefix.MAX_PREFIX_LENGTH), value));
    // Process passive neighbors next. Note that for now, a passive neighbor listening
    // to a /32 will overwrite an active neighbor of the same IP.
    // TODO(https://github.com/batfish/batfish/issues/1228)
    Map<Prefix, BgpNeighbor> passiveNeighbors =
        CiscoNxConversions.getPassiveNeighbors(c, v, newBgpProcess, nxBgpGlobal, nxBgpVrf, _w);
    newNeighbors.putAll(passiveNeighbors);

    newBgpProcess.setNeighbors(ImmutableSortedMap.copyOf(newNeighbors));

    return newBgpProcess;
  }

  private org.batfish.datamodel.BgpProcess toBgpProcess(
      final Configuration c, BgpProcess proc, String vrfName) {
    org.batfish.datamodel.BgpProcess newBgpProcess = new org.batfish.datamodel.BgpProcess();
    org.batfish.datamodel.Vrf v = c.getVrfs().get(vrfName);
    BgpTieBreaker tieBreaker = proc.getTieBreaker();
    if (tieBreaker != null) {
      newBgpProcess.setTieBreaker(tieBreaker);
    }
    MultipathEquivalentAsPathMatchMode multipathEquivalentAsPathMatchMode =
        proc.getAsPathMultipathRelax() ? PATH_LENGTH : EXACT_PATH;
    newBgpProcess.setMultipathEquivalentAsPathMatchMode(multipathEquivalentAsPathMatchMode);
    boolean multipathEbgp = false;
    boolean multipathIbgp = false;
    if (firstNonNull(proc.getMaximumPaths(), 0) > 1) {
      multipathEbgp = true;
      multipathIbgp = true;
    }
    if (firstNonNull(proc.getMaximumPathsEbgp(), 0) > 1 || proc.getAsPathMultipathRelax()) {
      multipathEbgp = true;
    }
    if (firstNonNull(proc.getMaximumPathsIbgp(), 0) > 1) {
      multipathIbgp = true;
    }
    newBgpProcess.setMultipathEbgp(multipathEbgp);
    newBgpProcess.setMultipathIbgp(multipathIbgp);

    Map<Prefix, BgpNeighbor> newBgpNeighbors = newBgpProcess.getNeighbors();
    int defaultMetric = proc.getDefaultMetric();
    Ip bgpRouterId = getBgpRouterId(c, vrfName, proc);
    newBgpProcess.setRouterId(bgpRouterId);

    /*
     * Create common bgp export policy. This policy encompasses network
     * statements, aggregate-address with/without summary-only, redistribution
     * from other protocols, and default-origination
     */
    RoutingPolicy bgpCommonExportPolicy =
        new RoutingPolicy(computeBgpCommonExportPolicyName(vrfName), c);
    c.getRoutingPolicies().put(bgpCommonExportPolicy.getName(), bgpCommonExportPolicy);
    List<Statement> bgpCommonExportStatements = bgpCommonExportPolicy.getStatements();

    // Never export routes suppressed because they are more specific than summary-only aggregate
    Stream<Prefix> summaryOnlyNetworks =
        proc.getAggregateNetworks()
            .entrySet()
            .stream()
            .filter(e -> e.getValue().getSummaryOnly())
            .map(Entry::getKey);
    If suppressSummaryOnly = suppressSummarizedPrefixes(c, vrfName, summaryOnlyNetworks);
    if (suppressSummaryOnly != null) {
      bgpCommonExportStatements.add(suppressSummaryOnly);
    }

    // The body of the export policy is a huge disjunction over many reasons routes may be exported.
    Disjunction routesShouldBeExported = new Disjunction();
    bgpCommonExportStatements.add(
        new If(
            routesShouldBeExported,
            ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
            ImmutableList.of()));
    // This list of reasons to export a route will be built up over the remainder of this function.
    List<BooleanExpr> exportConditions = routesShouldBeExported.getDisjuncts();

    // Finally, the export policy ends with returning false: do not export unmatched routes.
    bgpCommonExportStatements.add(Statements.ReturnFalse.toStaticStatement());

    // Export the generated routes for aggregate ipv4 addresses
    for (Entry<Prefix, BgpAggregateIpv4Network> e : proc.getAggregateNetworks().entrySet()) {
      Prefix prefix = e.getKey();
      BgpAggregateIpv4Network aggNet = e.getValue();

      // Generate a policy that matches routes to be aggregated.
      RoutingPolicy generatedPolicy = generateAggregateRoutePolicy(c, vrfName, prefix);

      GeneratedRoute.Builder gr =
          new GeneratedRoute.Builder()
              .setNetwork(prefix)
              .setAdmin(CISCO_AGGREGATE_ROUTE_ADMIN_COST)
              .setGenerationPolicy(generatedPolicy.getName())
              .setDiscard(true);

      // Conditions to generate this route
      List<BooleanExpr> generateAggregateConditions = new ArrayList<>();
      generateAggregateConditions.add(
          new MatchPrefixSet(
              new DestinationNetwork(),
              new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(prefix)))));
      generateAggregateConditions.add(new MatchProtocol(RoutingProtocol.AGGREGATE));

      // If defined, set attribute map for aggregate network
      BooleanExpr weInterior = BooleanExprs.TRUE;
      String attributeMapName = aggNet.getAttributeMap();
      if (attributeMapName != null) {
        RouteMap attributeMap = _routeMaps.get(attributeMapName);
        if (attributeMap != null) {
          // need to apply attribute changes if this specific route is matched
          weInterior = new CallExpr(attributeMapName);
          attributeMap.getReferers().put(aggNet, "attribute-map of aggregate route: " + prefix);
          gr.setAttributePolicy(attributeMapName);
        }
      }
      generateAggregateConditions.add(
          bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.IGP));

      v.getGeneratedRoutes().add(gr.build());
      // Do export a generated aggregate.
      exportConditions.add(new Conjunction(generateAggregateConditions));
    }

    // add generated routes for aggregate ipv6 addresses
    // TODO: merge with above to make cleaner
    for (Entry<Prefix6, BgpAggregateIpv6Network> e : proc.getAggregateIpv6Networks().entrySet()) {
      Prefix6 prefix6 = e.getKey();
      BgpAggregateIpv6Network aggNet = e.getValue();
      int prefixLength = prefix6.getPrefixLength();
      SubRange prefixRange = new SubRange(prefixLength + 1, Prefix6.MAX_PREFIX_LENGTH);

      // create generation policy for aggregate network
      String generationPolicyName = "~AGGREGATE_ROUTE6_GEN:" + vrfName + ":" + prefix6 + "~";
      RoutingPolicy currentGeneratedRoutePolicy = new RoutingPolicy(generationPolicyName, c);
      If currentGeneratedRouteConditional = new If();
      currentGeneratedRoutePolicy.getStatements().add(currentGeneratedRouteConditional);
      currentGeneratedRouteConditional.setGuard(
          new MatchPrefix6Set(
              new DestinationNetwork6(),
              new ExplicitPrefix6Set(
                  new Prefix6Space(
                      Collections.singleton(new Prefix6Range(prefix6, prefixRange))))));
      currentGeneratedRouteConditional
          .getTrueStatements()
          .add(Statements.ReturnTrue.toStaticStatement());
      c.getRoutingPolicies().put(generationPolicyName, currentGeneratedRoutePolicy);
      GeneratedRoute6 gr = new GeneratedRoute6(prefix6, CISCO_AGGREGATE_ROUTE_ADMIN_COST);
      gr.setGenerationPolicy(generationPolicyName);
      gr.setDiscard(true);
      v.getGeneratedIpv6Routes().add(gr);

      // set attribute map for aggregate network
      String attributeMapName = aggNet.getAttributeMap();
      if (attributeMapName != null) {
        RouteMap attributeMap = _routeMaps.get(attributeMapName);
        if (attributeMap != null) {
          attributeMap
              .getReferers()
              .put(aggNet, "attribute-map of aggregate ipv6 route: " + prefix6);
          gr.setAttributePolicy(attributeMapName);
        }
      }
    }

    // Export RIP routes that should be redistributed.
    BgpRedistributionPolicy redistributeRipPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.RIP);
    if (redistributeRipPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.TRUE;
      Conjunction exportRipConditions = new Conjunction();
      exportRipConditions.setComment("Redistribute RIP routes into BGP");
      exportRipConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.RIP));
      String mapName = redistributeRipPolicy.getRouteMap();
      if (mapName != null) {
        RouteMap redistributeRipRouteMap = _routeMaps.get(mapName);
        if (redistributeRipRouteMap != null) {
          redistributeRipRouteMap.getReferers().put(proc, "RIP redistribution route-map");
          weInterior = new CallExpr(mapName);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportRipConditions.getConjuncts().add(we);
      exportConditions.add(exportRipConditions);
    }

    // Export static routes that should be redistributed.
    BgpRedistributionPolicy redistributeStaticPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.STATIC);
    if (redistributeStaticPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.TRUE;
      Conjunction exportStaticConditions = new Conjunction();
      exportStaticConditions.setComment("Redistribute static routes into BGP");
      exportStaticConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.STATIC));
      String mapName = redistributeStaticPolicy.getRouteMap();
      if (mapName != null) {
        RouteMap redistributeStaticRouteMap = _routeMaps.get(mapName);
        if (redistributeStaticRouteMap != null) {
          redistributeStaticRouteMap.getReferers().put(proc, "static redistribution route-map");
          weInterior = new CallExpr(mapName);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportStaticConditions.getConjuncts().add(we);
      exportConditions.add(exportStaticConditions);
    }

    // Export connected routes that should be redistributed.
    BgpRedistributionPolicy redistributeConnectedPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.CONNECTED);
    if (redistributeConnectedPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.TRUE;
      Conjunction exportConnectedConditions = new Conjunction();
      exportConnectedConditions.setComment("Redistribute connected routes into BGP");
      exportConnectedConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.CONNECTED));
      String mapName = redistributeConnectedPolicy.getRouteMap();
      if (mapName != null) {
        RouteMap redistributeConnectedRouteMap = _routeMaps.get(mapName);
        if (redistributeConnectedRouteMap != null) {
          redistributeConnectedRouteMap
              .getReferers()
              .put(proc, "connected redistribution route-map");
          weInterior = new CallExpr(mapName);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportConnectedConditions.getConjuncts().add(we);
      exportConditions.add(exportConnectedConditions);
    }

    // Export OSPF routes that should be redistributed.
    BgpRedistributionPolicy redistributeOspfPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.OSPF);
    if (redistributeOspfPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.TRUE;
      Conjunction exportOspfConditions = new Conjunction();
      exportOspfConditions.setComment("Redistribute OSPF routes into BGP");
      exportOspfConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.OSPF));
      String mapName = redistributeOspfPolicy.getRouteMap();
      if (mapName != null) {
        RouteMap redistributeOspfRouteMap = _routeMaps.get(mapName);
        if (redistributeOspfRouteMap != null) {
          redistributeOspfRouteMap.getReferers().put(proc, "ospf redistribution route-map");
          weInterior = new CallExpr(mapName);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportOspfConditions.getConjuncts().add(we);
      exportConditions.add(exportOspfConditions);
    }

    // cause ip peer groups to inherit unset fields from owning named peer
    // group if it exists, and then always from process master peer group
    Set<LeafBgpPeerGroup> leafGroups = new LinkedHashSet<>();
    leafGroups.addAll(proc.getIpPeerGroups().values());
    leafGroups.addAll(proc.getIpv6PeerGroups().values());
    leafGroups.addAll(proc.getDynamicIpPeerGroups().values());
    leafGroups.addAll(proc.getDynamicIpv6PeerGroups().values());
    for (LeafBgpPeerGroup lpg : leafGroups) {
      lpg.inheritUnsetFields(proc, this);
    }
    _unusedPeerGroups = new HashSet<>();
    int fakePeerCounter = -1;
    // peer groups / peer templates
    for (Entry<String, NamedBgpPeerGroup> e : proc.getNamedPeerGroups().entrySet()) {
      String name = e.getKey();
      NamedBgpPeerGroup namedPeerGroup = e.getValue();
      if (!namedPeerGroup.getInherited()) {
        _unusedPeerGroups.add(namedPeerGroup);
        Ip fakeIp = new Ip(fakePeerCounter);
        IpBgpPeerGroup fakePg = new IpBgpPeerGroup(fakeIp);
        fakePg.setGroupName(name);
        fakePg.setActive(false);
        fakePg.setShutdown(true);
        leafGroups.add(fakePg);
        fakePg.inheritUnsetFields(proc, this);
        fakePeerCounter--;
      }
      namedPeerGroup.inheritUnsetFields(proc, this);
    }
    // separate because peer sessions can inherit from other peer sessions
    _unusedPeerSessions = new HashSet<>();
    int fakeGroupCounter = 1;
    for (NamedBgpPeerGroup namedPeerGroup : proc.getPeerSessions().values()) {
      namedPeerGroup.getParentSession(proc, this).inheritUnsetFields(proc, this);
    }
    for (Entry<String, NamedBgpPeerGroup> e : proc.getPeerSessions().entrySet()) {
      String name = e.getKey();
      NamedBgpPeerGroup namedPeerGroup = e.getValue();
      if (!namedPeerGroup.getInherited()) {
        _unusedPeerSessions.add(namedPeerGroup);
        String fakeNamedPgName = "~FAKE_PG_" + fakeGroupCounter + "~";
        NamedBgpPeerGroup fakeNamedPg = new NamedBgpPeerGroup(fakeNamedPgName, -1);
        fakeNamedPg.setPeerSession(name);
        proc.getNamedPeerGroups().put(fakeNamedPgName, fakeNamedPg);
        Ip fakeIp = new Ip(fakePeerCounter);
        IpBgpPeerGroup fakePg = new IpBgpPeerGroup(fakeIp);
        fakePg.setGroupName(fakeNamedPgName);
        fakePg.setActive(false);
        fakePg.setShutdown(true);
        leafGroups.add(fakePg);
        fakePg.inheritUnsetFields(proc, this);
        fakeGroupCounter++;
        fakePeerCounter--;
      }
    }

    // create origination prefilter from listed advertised networks
    proc.getIpNetworks()
        .forEach(
            (prefix, bgpNetwork) -> {
              String mapName = bgpNetwork.getRouteMapName();
              BooleanExpr weExpr = BooleanExprs.TRUE;
              if (mapName != null) {
                RouteMap routeMap = _routeMaps.get(mapName);
                if (routeMap != null) {
                  weExpr = new CallExpr(mapName);
                  routeMap.getReferers().put(proc, "bgp ipv4 advertised network route-map");
                }
              }
              BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weExpr, OriginType.IGP);
              Conjunction exportNetworkConditions = new Conjunction();
              PrefixSpace space = new PrefixSpace();
              space.addPrefix(prefix);
              newBgpProcess.addToOriginationSpace(space);
              exportNetworkConditions
                  .getConjuncts()
                  .add(new MatchPrefixSet(new DestinationNetwork(), new ExplicitPrefixSet(space)));
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.BGP)));
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.IBGP)));
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)));
              exportNetworkConditions.getConjuncts().add(we);
              exportConditions.add(exportNetworkConditions);
            });
    if (!proc.getIpv6Networks().isEmpty()) {
      String localFilter6Name = "~BGP_NETWORK6_NETWORKS_FILTER:" + vrfName + "~";
      Route6FilterList localFilter6 = new Route6FilterList(localFilter6Name);
      proc.getIpv6Networks()
          .forEach(
              (prefix6, bgpNetwork6) -> {
                int prefixLen = prefix6.getPrefixLength();
                Route6FilterLine line =
                    new Route6FilterLine(
                        LineAction.ACCEPT, prefix6, new SubRange(prefixLen, prefixLen));
                localFilter6.addLine(line);
                String mapName = bgpNetwork6.getRouteMapName();
                if (mapName != null) {
                  RouteMap routeMap = _routeMaps.get(mapName);
                  if (routeMap != null) {
                    routeMap.getReferers().put(proc, "bgp ipv6 advertised network route-map");
                    BooleanExpr we =
                        bgpRedistributeWithEnvironmentExpr(new CallExpr(mapName), OriginType.IGP);
                    Conjunction exportNetwork6Conditions = new Conjunction();
                    Prefix6Space space6 = new Prefix6Space();
                    space6.addPrefix6(prefix6);
                    exportNetwork6Conditions
                        .getConjuncts()
                        .add(
                            new MatchPrefix6Set(
                                new DestinationNetwork6(), new ExplicitPrefix6Set(space6)));
                    exportNetwork6Conditions
                        .getConjuncts()
                        .add(new Not(new MatchProtocol(RoutingProtocol.BGP)));
                    exportNetwork6Conditions
                        .getConjuncts()
                        .add(new Not(new MatchProtocol(RoutingProtocol.IBGP)));
                    exportNetwork6Conditions
                        .getConjuncts()
                        .add(new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)));
                    exportNetwork6Conditions.getConjuncts().add(we);
                    exportConditions.add(exportNetwork6Conditions);
                  }
                }
              });
      c.getRoute6FilterLists().put(localFilter6Name, localFilter6);
    }

    // Export BGP and IBGP routes.
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP));
    exportConditions.add(new MatchProtocol(RoutingProtocol.IBGP));

    for (LeafBgpPeerGroup lpg : leafGroups) {
      // update source
      String updateSourceInterface = lpg.getUpdateSource();
      boolean ipv4 = lpg.getNeighborPrefix() != null;
      Ip updateSource = getUpdateSource(c, vrfName, lpg, updateSourceInterface, ipv4);
      RoutingPolicy importPolicy = null;
      String inboundRouteMapName = lpg.getInboundRouteMap();
      if (inboundRouteMapName != null) {
        importPolicy = c.getRoutingPolicies().get(inboundRouteMapName);
        if (importPolicy != null) {
          RouteMap inboundRouteMap = _routeMaps.get(inboundRouteMapName);
          inboundRouteMap
              .getReferers()
              .put(lpg, "inbound route-map for leaf peer-group: " + lpg.getName());
        }
      }
      String inboundRoute6MapName = lpg.getInboundRoute6Map();
      if (inboundRoute6MapName != null) {
        RoutingPolicy importPolicy6 = c.getRoutingPolicies().get(inboundRoute6MapName);
        if (importPolicy6 != null) {
          RouteMap inboundRouteMap = _routeMaps.get(inboundRoute6MapName);
          inboundRouteMap
              .getReferers()
              .put(lpg, "inbound route-map for leaf peer-group: " + lpg.getName());
        }
      }
      String peerExportPolicyName =
          "~BGP_PEER_EXPORT_POLICY:" + vrfName + ":" + lpg.getName() + "~";
      RoutingPolicy peerExportPolicy = new RoutingPolicy(peerExportPolicyName, c);
      if (lpg.getActive() && !lpg.getShutdown()) {
        c.getRoutingPolicies().put(peerExportPolicyName, peerExportPolicy);
      }
      if (lpg.getNextHopSelf() != null && lpg.getNextHopSelf()) {
        peerExportPolicy.getStatements().add(new SetNextHop(SelfNextHop.getInstance(), false));
      }
      if (lpg.getRemovePrivateAs() != null && lpg.getRemovePrivateAs()) {
        peerExportPolicy.getStatements().add(Statements.RemovePrivateAs.toStaticStatement());
      }
      Conjunction peerExportConditions = new Conjunction();
      If peerExportConditional =
          new If(
              "peer-export policy main conditional: exitAccept if true / exitReject if false",
              peerExportConditions,
              ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
              ImmutableList.of(Statements.ExitReject.toStaticStatement()));
      peerExportPolicy.getStatements().add(peerExportConditional);
      Disjunction localOrCommonOrigination = new Disjunction();
      peerExportConditions.getConjuncts().add(localOrCommonOrigination);
      localOrCommonOrigination.getDisjuncts().add(new CallExpr(bgpCommonExportPolicy.getName()));
      String outboundRouteMapName = lpg.getOutboundRouteMap();
      if (outboundRouteMapName != null) {
        RouteMap outboundRouteMap = _routeMaps.get(outboundRouteMapName);
        if (outboundRouteMap != null) {
          outboundRouteMap
              .getReferers()
              .put(lpg, "outbound route-map for leaf peer-group: " + lpg.getName());
          peerExportConditions.getConjuncts().add(new CallExpr(outboundRouteMapName));
        }
      }
      String outboundRoute6MapName = lpg.getOutboundRoute6Map();
      if (outboundRoute6MapName != null) {
        RouteMap outboundRoute6Map = _routeMaps.get(outboundRoute6MapName);
        if (outboundRoute6Map != null) {
          outboundRoute6Map
              .getReferers()
              .put(lpg, "outbound ipv6 route-map for leaf peer-group: " + lpg.getName());
        }
      }

      // set up default export policy for this peer group
      GeneratedRoute.Builder defaultRoute = null;
      GeneratedRoute6.Builder defaultRoute6 = null;
      if (lpg.getDefaultOriginate()) {
        String defaultRouteExportPolicyName;
        defaultRouteExportPolicyName =
            String.format("~BGP_DEFAULT_ROUTE_PEER_EXPORT_POLICY:%s:%s~", vrfName, lpg.getName());
        RoutingPolicy defaultRouteExportPolicy = new RoutingPolicy(defaultRouteExportPolicyName, c);
        c.getRoutingPolicies().put(defaultRouteExportPolicyName, defaultRouteExportPolicy);
        defaultRouteExportPolicy
            .getStatements()
            .add(
                new If(
                    ipv4 ? MATCH_DEFAULT_ROUTE : MATCH_DEFAULT_ROUTE6,
                    ImmutableList.of(
                        new SetOrigin(
                            new LiteralOrigin(
                                c.getConfigurationFormat() == ConfigurationFormat.CISCO_IOS
                                    ? OriginType.IGP
                                    : OriginType.INCOMPLETE,
                                null)),
                        Statements.ReturnTrue.toStaticStatement())));
        defaultRouteExportPolicy.getStatements().add(Statements.ReturnFalse.toStaticStatement());
        localOrCommonOrigination
            .getDisjuncts()
            .add(new CallExpr(defaultRouteExportPolicy.getName()));

        defaultRoute = new GeneratedRoute.Builder();
        defaultRoute.setNetwork(Prefix.ZERO);
        defaultRoute.setAdmin(MAX_ADMINISTRATIVE_COST);
        defaultRoute6 = new GeneratedRoute6.Builder();
        defaultRoute6.setNetwork(Prefix6.ZERO);
        defaultRoute6.setAdmin(MAX_ADMINISTRATIVE_COST);

        String defaultOriginateMapName = lpg.getDefaultOriginateMap();
        if (defaultOriginateMapName != null) { // originate contingent on
          // generation policy
          RoutingPolicy defaultRouteGenerationPolicy =
              c.getRoutingPolicies().get(defaultOriginateMapName);
          if (defaultRouteGenerationPolicy != null) {
            RouteMap defaultRouteGenerationRouteMap = _routeMaps.get(defaultOriginateMapName);
            defaultRouteGenerationRouteMap
                .getReferers()
                .put(lpg, "default route generation policy for leaf peer-group: " + lpg.getName());
            defaultRoute.setGenerationPolicy(defaultOriginateMapName);
          }
        } else {
          If defaultRouteGenerationConditional =
              new If(
                  ipv4 ? MATCH_DEFAULT_ROUTE : MATCH_DEFAULT_ROUTE6,
                  ImmutableList.of(Statements.ReturnTrue.toStaticStatement()));
          RoutingPolicy defaultRouteGenerationPolicy =
              new RoutingPolicy(
                  "~BGP_DEFAULT_ROUTE_GENERATION_POLICY:" + vrfName + ":" + lpg.getName() + "~", c);
          defaultRouteGenerationPolicy.getStatements().add(defaultRouteGenerationConditional);
          if (lpg.getActive() && !lpg.getShutdown()) {
            c.getRoutingPolicies()
                .put(defaultRouteGenerationPolicy.getName(), defaultRouteGenerationPolicy);
          }
          if (ipv4) {
            defaultRoute.setGenerationPolicy(defaultRouteGenerationPolicy.getName());
          } else {
            defaultRoute6.setGenerationPolicy(defaultRouteGenerationPolicy.getName());
          }
        }
      }

      Ip clusterId = lpg.getClusterId();
      if (clusterId == null) {
        clusterId = bgpRouterId;
      }
      String inboundPrefixListName = lpg.getInboundPrefixList();
      if (inboundPrefixListName != null) {
        ReferenceCountedStructure inboundPrefixList;
        if (ipv4) {
          inboundPrefixList = _prefixLists.get(inboundPrefixListName);
        } else {
          inboundPrefixList = _prefix6Lists.get(inboundPrefixListName);
        }
        if (inboundPrefixList != null) {
          inboundPrefixList
              .getReferers()
              .put(lpg, "inbound prefix-list for neighbor: '" + lpg.getName() + "'");
        }
      }
      String outboundPrefixListName = lpg.getOutboundPrefixList();
      if (outboundPrefixListName != null) {
        ReferenceCountedStructure outboundPrefixList;
        if (ipv4) {
          outboundPrefixList = _prefixLists.get(outboundPrefixListName);
        } else {
          outboundPrefixList = _prefix6Lists.get(outboundPrefixListName);
        }
        if (outboundPrefixList != null) {
          outboundPrefixList
              .getReferers()
              .put(lpg, "outbound prefix-list for neighbor: '" + lpg.getName() + "'");
        }
      }
      String description = lpg.getDescription();
      if (lpg.getActive() && !lpg.getShutdown()) {
        if (lpg.getRemoteAs() == null) {
          _w.redFlag("No remote-as set for peer: " + lpg.getName());
          continue;
        }
        Long pgLocalAs = lpg.getLocalAs();
        long localAs = pgLocalAs != null ? pgLocalAs : proc.getName();
        BgpNeighbor newNeighbor;
        if (lpg instanceof IpBgpPeerGroup) {
          IpBgpPeerGroup ipg = (IpBgpPeerGroup) lpg;
          Ip neighborAddress = ipg.getIp();
          newNeighbor = new BgpNeighbor(neighborAddress, c, false);
        } else if (lpg instanceof DynamicIpBgpPeerGroup) {
          DynamicIpBgpPeerGroup dpg = (DynamicIpBgpPeerGroup) lpg;
          Prefix neighborAddressRange = dpg.getPrefix();
          newNeighbor = new BgpNeighbor(neighborAddressRange, c, true);
        } else if (lpg instanceof Ipv6BgpPeerGroup || lpg instanceof DynamicIpv6BgpPeerGroup) {
          // TODO: implement ipv6 bgp neighbors
          continue;
        } else {
          throw new VendorConversionException("Invalid BGP leaf neighbor type");
        }
        newBgpNeighbors.put(newNeighbor.getPrefix(), newNeighbor);

        newNeighbor.setAdditionalPathsReceive(lpg.getAdditionalPathsReceive());
        newNeighbor.setAdditionalPathsSelectAll(lpg.getAdditionalPathsSelectAll());
        newNeighbor.setAdditionalPathsSend(lpg.getAdditionalPathsSend());
        newNeighbor.setAdvertiseInactive(lpg.getAdvertiseInactive());
        newNeighbor.setAllowLocalAsIn(lpg.getAllowAsIn());
        newNeighbor.setAllowRemoteAsOut(lpg.getDisablePeerAsCheck());
        newNeighbor.setRouteReflectorClient(lpg.getRouteReflectorClient());
        newNeighbor.setClusterId(clusterId.asLong());
        newNeighbor.setDefaultMetric(defaultMetric);
        newNeighbor.setDescription(description);
        newNeighbor.setEbgpMultihop(lpg.getEbgpMultihop());
        if (defaultRoute != null) {
          newNeighbor.getGeneratedRoutes().add(defaultRoute.build());
        }
        newNeighbor.setGroup(lpg.getGroupName());
        if (importPolicy != null) {
          newNeighbor.setImportPolicy(inboundRouteMapName);
        }
        newNeighbor.setLocalAs(localAs);
        newNeighbor.setLocalIp(updateSource);
        newNeighbor.setExportPolicy(peerExportPolicyName);
        newNeighbor.setRemoteAs(lpg.getRemoteAs());
        newNeighbor.setSendCommunity(lpg.getSendCommunity());
        newNeighbor.setVrf(vrfName);
      }
    }
    return newBgpProcess;
  }

  /**
   * Processes a {@link CiscoSourceNat} rule. This function performs two actions:
   *
   * <p>1. Record references to ACLs and NAT pools by the various parsed {@link CiscoSourceNat}
   * objects.
   *
   * <p>2. Convert to vendor-independent {@link SourceNat} objects if valid, aka, no undefined ACL
   * and valid output configuration.
   *
   * <p>Returns the vendor-independeng {@link SourceNat}, or {@code null} if the source NAT rule is
   * invalid.
   */
  @Nullable
  SourceNat processSourceNat(
      CiscoSourceNat nat, Interface iface, Map<String, IpAccessList> ipAccessLists) {
    String sourceNatAclName = nat.getAclName();
    if (sourceNatAclName == null) {
      // Source NAT rules must have an ACL; this rule is invalid.
      return null;
    }

    SourceNat convertedNat = new SourceNat();

    /* source nat acl */
    IpAccessList sourceNatAcl = ipAccessLists.get(sourceNatAclName);
    if (sourceNatAcl != null) {
      convertedNat.setAcl(sourceNatAcl);
      String msg = "source nat acl for interface: " + iface.getName();
      ExtendedAccessList sourceNatExtendedAccessList = _extendedAccessLists.get(sourceNatAclName);
      if (sourceNatExtendedAccessList != null) {
        sourceNatExtendedAccessList.getReferers().put(iface, msg);
      }
      StandardAccessList sourceNatStandardAccessList = _standardAccessLists.get(sourceNatAclName);
      if (sourceNatStandardAccessList != null) {
        sourceNatStandardAccessList.getReferers().put(iface, msg);
      }
    }

    /* source nat pool */
    String sourceNatPoolName = nat.getNatPool();
    if (sourceNatPoolName != null) {
      NatPool sourceNatPool = _natPools.get(sourceNatPoolName);
      if (sourceNatPool != null) {
        sourceNatPool.getReferers().put(iface, "source nat pool for interface: " + iface.getName());
        Ip firstIp = sourceNatPool.getFirst();
        if (firstIp != null) {
          Ip lastIp = sourceNatPool.getLast();
          convertedNat.setPoolIpFirst(firstIp);
          convertedNat.setPoolIpLast(lastIp);
        }
      }
    }

    // The source NAT rule is valid iff it has an ACL and a pool of IPs to NAT into.
    if (convertedNat.getAcl() != null && convertedNat.getPoolIpFirst() != null) {
      return convertedNat;
    } else {
      return null;
    }
  }

  private static final Pattern INTERFACE_WITH_SUBINTERFACE = Pattern.compile("^(.*)\\.(\\d+)$");

  /**
   * Returns the MTU that should be assigned to the given interface, taking into account
   * vendor-specific conventions such as Arista subinterfaces.
   */
  private int getInterfaceMtu(Interface iface) {
    if (_vendor == ConfigurationFormat.ARISTA) {
      Matcher m = INTERFACE_WITH_SUBINTERFACE.matcher(iface.getName());
      if (m.matches()) {
        String parentInterfaceName = m.group(1);
        Interface parentInterface = _interfaces.get(parentInterfaceName);
        if (parentInterface != null) {
          return parentInterface.getMtu();
        }
      }
    }

    return iface.getMtu();
  }

  private org.batfish.datamodel.Interface toInterface(
      Interface iface, Map<String, IpAccessList> ipAccessLists, Configuration c) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface = new org.batfish.datamodel.Interface(name, c);
    String vrfName = iface.getVrf();
    Vrf vrf = _vrfs.computeIfAbsent(vrfName, Vrf::new);
    newIface.setDescription(iface.getDescription());
    newIface.setActive(iface.getActive());
    newIface.setChannelGroup(iface.getChannelGroup());
    newIface.setAutoState(iface.getAutoState());
    newIface.setVrf(c.getVrfs().get(vrfName));
    newIface.setBandwidth(iface.getBandwidth());
    if (iface.getDhcpRelayClient()) {
      newIface.getDhcpRelayAddresses().addAll(_dhcpRelayServers);
    } else {
      newIface.getDhcpRelayAddresses().addAll(iface.getDhcpRelayAddresses());
    }
    newIface.setMtu(getInterfaceMtu(iface));
    newIface.setOspfPointToPoint(iface.getOspfPointToPoint());
    newIface.setProxyArp(iface.getProxyArp());
    newIface.setSpanningTreePortfast(iface.getSpanningTreePortfast());
    newIface.setSwitchport(iface.getSwitchport());
    newIface.setDeclaredNames(ImmutableSortedSet.copyOf(iface.getDeclaredNames()));

    // All prefixes is the combination of the interface prefix + any secondary prefixes.
    ImmutableSet.Builder<InterfaceAddress> allPrefixes = ImmutableSet.builder();
    if (iface.getAddress() != null) {
      newIface.setAddress(iface.getAddress());
      allPrefixes.add(iface.getAddress());
    }
    allPrefixes.addAll(iface.getSecondaryAddresses());
    newIface.setAllAddresses(allPrefixes.build());

    Long ospfAreaLong = iface.getOspfArea();
    if (ospfAreaLong != null) {
      OspfProcess proc = vrf.getOspfProcess();
      if (proc != null) {
        if (iface.getOspfActive()) {
          proc.getActiveInterfaceList().add(name);
        }
        if (iface.getOspfPassive()) {
          proc.getPassiveInterfaceList().add(name);
        }
        for (InterfaceAddress address : newIface.getAllAddresses()) {
          Prefix prefix = address.getPrefix();
          OspfNetwork ospfNetwork = new OspfNetwork(prefix, ospfAreaLong);
          proc.getNetworks().add(ospfNetwork);
        }
      } else {
        _w.redFlag(
            "Interface: '" + name + "' contains OSPF settings, but there is no OSPF process");
      }
    }
    boolean level1 = false;
    boolean level2 = false;
    IsisProcess isisProcess = vrf.getIsisProcess();
    if (isisProcess != null) {
      switch (isisProcess.getLevel()) {
        case LEVEL_1:
          level1 = true;
          break;
        case LEVEL_1_2:
          level1 = true;
          level2 = true;
          break;
        case LEVEL_2:
          level2 = true;
          break;
        default:
          throw new VendorConversionException("Invalid IS-IS level");
      }
      IsisInterfaceSettings.Builder isisInterfaceSettingsBuilder = IsisInterfaceSettings.builder();
      if (level1) {
        IsisInterfaceLevelSettings isisInterfaceLevel1Settings = new IsisInterfaceLevelSettings();
        isisInterfaceSettingsBuilder.setLevel1(isisInterfaceLevel1Settings);
        isisInterfaceLevel1Settings.setCost(iface.getIsisCost());
        isisInterfaceLevel1Settings.setMode(iface.getIsisInterfaceMode());
      }
      if (level2) {
        IsisInterfaceLevelSettings isisInterfaceLevel2Settings = new IsisInterfaceLevelSettings();
        isisInterfaceSettingsBuilder.setLevel2(isisInterfaceLevel2Settings);
        isisInterfaceLevel2Settings.setCost(iface.getIsisCost());
        isisInterfaceLevel2Settings.setMode(iface.getIsisInterfaceMode());
      }
      newIface.setIsis(isisInterfaceSettingsBuilder.build());
    }
    newIface.setOspfCost(iface.getOspfCost());
    newIface.setOspfDeadInterval(iface.getOspfDeadInterval());
    newIface.setOspfHelloMultiplier(iface.getOspfHelloMultiplier());

    // switch settings
    newIface.setAccessVlan(iface.getAccessVlan());
    newIface.setNativeVlan(iface.getNativeVlan());
    newIface.setSwitchportMode(iface.getSwitchportMode());
    SwitchportEncapsulationType encapsulation = iface.getSwitchportTrunkEncapsulation();
    if (encapsulation == null) { // no encapsulation set, so use default..
      // TODO: check if this is OK
      encapsulation = SwitchportEncapsulationType.DOT1Q;
    }
    newIface.setSwitchportTrunkEncapsulation(encapsulation);
    newIface.addAllowedRanges(iface.getAllowedVlans());

    String incomingFilterName = iface.getIncomingFilter();
    if (incomingFilterName != null) {
      newIface.setIncomingFilter(ipAccessLists.get(incomingFilterName));
    }
    String outgoingFilterName = iface.getOutgoingFilter();
    if (outgoingFilterName != null) {
      newIface.setOutgoingFilter(ipAccessLists.get(outgoingFilterName));
    }
    // Apply zone outgoing filter if necessary
    applyZoneFilter(iface, newIface, c);

    List<CiscoSourceNat> origSourceNats = iface.getSourceNats();
    if (origSourceNats != null) {
      // Process each of the CiscoSourceNats:
      //   1) Collect references to ACLs and NAT pools.
      //   2) For valid CiscoSourceNat rules, add them to the newIface source NATs list.
      newIface.setSourceNats(
          origSourceNats
              .stream()
              .map(nat -> processSourceNat(nat, iface, ipAccessLists))
              .filter(Objects::nonNull)
              .collect(ImmutableList.toImmutableList()));
    }
    String routingPolicyName = iface.getRoutingPolicy();
    if (routingPolicyName != null) {
      RouteMap routingPolicyRouteMap = _routeMaps.get(routingPolicyName);
      if (routingPolicyRouteMap != null) {
        routingPolicyRouteMap
            .getReferers()
            .put(iface, "routing policy for interface: " + iface.getName());
      }
      newIface.setRoutingPolicy(routingPolicyName);
    }
    return newIface;
  }

  private void applyZoneFilter(
      Interface iface, org.batfish.datamodel.Interface newIface, Configuration c) {
    String zoneName = iface.getSecurityZone();
    if (zoneName == null) {
      return;
    }
    SecurityZone securityZone = _securityZones.get(zoneName);
    if (securityZone == null) {
      return;
    }
    String zoneOutgoingAclName = computeZoneOutgoingAclName(zoneName);
    IpAccessList zoneOutgoingAcl = c.getIpAccessLists().get(zoneOutgoingAclName);
    if (zoneOutgoingAcl == null) {
      return;
    }
    String oldOutgoingFilterName = newIface.getOutgoingFilterName();
    if (oldOutgoingFilterName != null) {
      String combinedOutgoingAclName = computeCombinedOutgoingAclName(newIface.getName());
      IpAccessList combinedOutgoingAcl =
          IpAccessList.builder()
              .setOwner(c)
              .setName(combinedOutgoingAclName)
              .setLines(
                  ImmutableList.of(
                      IpAccessListLine.accepting()
                          .setMatchCondition(
                              new AndMatchExpr(
                                  ImmutableList.of(
                                      new PermittedByAcl(zoneOutgoingAclName),
                                      new PermittedByAcl(oldOutgoingFilterName)),
                                  String.format(
                                      "Permit if permitted by policy for zone '%s' and permitted by outgoing filter '%s'",
                                      zoneName, oldOutgoingFilterName)))
                          .build()))
              .build();
      newIface.setOutgoingFilter(combinedOutgoingAcl);
    } else {
      newIface.setOutgoingFilter(zoneOutgoingAcl);
    }
  }

  public static String computeCombinedOutgoingAclName(String interfaceName) {
    return String.format("~COMBINED_OUTGOING_ACL~%s~", interfaceName);
  }

  /**
   * For a given protocol, processes any redistribution policy that exists and adds any new policy
   * statements to {@code allOspfExportStatements}.
   */
  private void applyOspfRedistributionPolicy(
      OspfProcess proc,
      RoutingProtocol protocol,
      CiscoStructureUsage structureType,
      List<Statement> allOspfExportStatements) {
    OspfRedistributionPolicy policy = proc.getRedistributionPolicies().get(protocol);
    if (policy == null) {
      // There is no redistribution policy for this protocol.
      return;
    }

    If redistributionPolicy = convertOspfRedistributionPolicy(policy, proc, structureType);
    allOspfExportStatements.add(redistributionPolicy);
  }

  // For testing.
  If convertOspfRedistributionPolicy(
      OspfRedistributionPolicy policy, OspfProcess proc, CiscoStructureUsage structureType) {
    RoutingProtocol protocol = policy.getSourceProtocol();
    // All redistribution must match the specified protocol.
    Conjunction ospfExportConditions = new Conjunction();
    ospfExportConditions.getConjuncts().add(new MatchProtocol(protocol));

    // Do not redistribute the default route.
    ospfExportConditions.getConjuncts().add(NOT_DEFAULT_ROUTE);

    ImmutableList.Builder<Statement> ospfExportStatements = ImmutableList.builder();

    // Set the metric type and value.
    ospfExportStatements.add(new SetOspfMetricType(policy.getMetricType()));
    long metric =
        policy.getMetric() != null ? policy.getMetric() : proc.getDefaultMetric(_vendor, protocol);
    ospfExportStatements.add(new SetMetric(new LiteralLong(metric)));

    // If only classful routes should be redistributed, filter to classful routes.
    if (policy.getOnlyClassfulRoutes()) {
      ospfExportConditions.getConjuncts().add(RouteIsClassful.instance());
    }

    // If a route-map filter is present, honor it.
    String exportRouteMapName = policy.getRouteMap();
    if (exportRouteMapName != null) {
      RouteMap exportRouteMap = _routeMaps.get(exportRouteMapName);
      if (exportRouteMap != null) {
        exportRouteMap.getReferers().put(proc, structureType.getDescription());
        ospfExportConditions.getConjuncts().add(new CallExpr(exportRouteMapName));
      }
    }

    ospfExportStatements.add(Statements.ExitAccept.toStaticStatement());

    // Construct the policy and add it before returning.
    return new If(
        "OSPF export routes for " + protocol.protocolName(),
        ospfExportConditions,
        ospfExportStatements.build(),
        ImmutableList.of());
  }

  private org.batfish.datamodel.ospf.OspfProcess toOspfProcess(
      OspfProcess proc, String vrfName, Configuration c, CiscoConfiguration oldConfig) {
    org.batfish.datamodel.ospf.OspfProcess newProcess =
        new org.batfish.datamodel.ospf.OspfProcess();
    org.batfish.datamodel.Vrf vrf = c.getVrfs().get(vrfName);

    if (proc.getMaxMetricRouterLsa()) {
      newProcess.setMaxMetricTransitLinks(OspfProcess.MAX_METRIC_ROUTER_LSA);
      if (proc.getMaxMetricIncludeStub()) {
        newProcess.setMaxMetricStubNetworks(OspfProcess.MAX_METRIC_ROUTER_LSA);
      }
      newProcess.setMaxMetricExternalNetworks(proc.getMaxMetricExternalLsa());
      newProcess.setMaxMetricSummaryNetworks(proc.getMaxMetricSummaryLsa());
    }

    newProcess.setProcessId(proc.getName());

    // establish areas and associated interfaces
    Map<Long, OspfArea> areas = newProcess.getAreas();
    Map<Long, ImmutableSortedSet.Builder<String>> areaInterfacesBuilders = new HashMap<>();
    // Sort networks with longer prefixes first, then lower start IPs and areas.
    SortedSet<OspfNetwork> networks =
        ImmutableSortedSet.copyOf(
            Comparator.<OspfNetwork>comparingInt(n -> n.getPrefix().getPrefixLength())
                .reversed()
                .thenComparing(n -> n.getPrefix().getStartIp())
                .thenComparingLong(OspfNetwork::getArea),
            proc.getNetworks());

    // Set RFC 1583 compatibility
    newProcess.setRfc1583Compatible(proc.getRfc1583Compatible());

    for (Entry<String, org.batfish.datamodel.Interface> e : vrf.getInterfaces().entrySet()) {
      String ifaceName = e.getKey();
      org.batfish.datamodel.Interface iface = e.getValue();
      InterfaceAddress interfaceAddress = iface.getAddress();
      if (interfaceAddress == null) {
        continue;
      }
      for (OspfNetwork network : networks) {
        Prefix networkPrefix = network.getPrefix();
        Ip networkAddress = networkPrefix.getStartIp();
        Ip maskedInterfaceAddress =
            interfaceAddress.getIp().getNetworkAddress(networkPrefix.getPrefixLength());
        if (maskedInterfaceAddress.equals(networkAddress)) {
          // we have a longest prefix match
          long areaNum = network.getArea();
          OspfArea newArea = areas.computeIfAbsent(areaNum, OspfArea::new);
          ImmutableSortedSet.Builder<String> newAreaInterfacesBuilder =
              areaInterfacesBuilders.computeIfAbsent(
                  areaNum, n -> ImmutableSortedSet.naturalOrder());
          newAreaInterfacesBuilder.add(ifaceName);
          iface.setOspfArea(newArea);
          iface.setOspfEnabled(true);
          boolean passive =
              proc.getPassiveInterfaceList().contains(iface.getName())
                  || (proc.getPassiveInterfaceDefault()
                      && !proc.getActiveInterfaceList().contains(iface.getName()));
          iface.setOspfPassive(passive);
          break;
        }
      }
      areaInterfacesBuilders.forEach(
          (areaNum, interfacesBuilder) ->
              areas.get(areaNum).setInterfaces(interfacesBuilder.build()));
    }
    proc.getNssas()
        .forEach(
            (areaId, nssaSettings) -> {
              if (!areas.containsKey(areaId)) {
                return;
              }
              areas.get(areaId).setStubType(StubType.NSSA);
              areas.get(areaId).setNssa(toNssaSettings(nssaSettings));
            });

    proc.getStubs()
        .forEach(
            (areaId, stubSettings) -> {
              if (!areas.containsKey(areaId)) {
                return;
              }
              areas.get(areaId).setStubType(StubType.STUB);
              areas.get(areaId).setStub(toStubSettings(stubSettings));
            });

    // create summarization filters for inter-area routes
    for (Entry<Long, Map<Prefix, OspfAreaSummary>> e1 : proc.getSummaries().entrySet()) {
      long areaLong = e1.getKey();
      Map<Prefix, OspfAreaSummary> summaries = e1.getValue();
      OspfArea area = areas.get(areaLong);
      String summaryFilterName = "~OSPF_SUMMARY_FILTER:" + vrfName + ":" + areaLong + "~";
      RouteFilterList summaryFilter = new RouteFilterList(summaryFilterName);
      c.getRouteFilterLists().put(summaryFilterName, summaryFilter);
      if (area == null) {
        area = new OspfArea(areaLong);
        areas.put(areaLong, area);
      }
      area.setSummaryFilter(summaryFilterName);
      for (Entry<Prefix, OspfAreaSummary> e2 : summaries.entrySet()) {
        Prefix prefix = e2.getKey();
        OspfAreaSummary summary = e2.getValue();
        int prefixLength = prefix.getPrefixLength();
        int filterMinPrefixLength =
            summary.getAdvertised()
                ? Math.min(Prefix.MAX_PREFIX_LENGTH, prefixLength + 1)
                : prefixLength;
        summaryFilter.addLine(
            new RouteFilterLine(
                LineAction.REJECT,
                new IpWildcard(prefix),
                new SubRange(filterMinPrefixLength, Prefix.MAX_PREFIX_LENGTH)));
      }
      area.setSummaries(ImmutableSortedMap.copyOf(summaries));
      summaryFilter.addLine(
          new RouteFilterLine(
              LineAction.ACCEPT,
              new IpWildcard(Prefix.ZERO),
              new SubRange(0, Prefix.MAX_PREFIX_LENGTH)));
    }

    String ospfExportPolicyName = "~OSPF_EXPORT_POLICY:" + vrfName + "~";
    RoutingPolicy ospfExportPolicy = new RoutingPolicy(ospfExportPolicyName, c);
    c.getRoutingPolicies().put(ospfExportPolicyName, ospfExportPolicy);
    List<Statement> ospfExportStatements = ospfExportPolicy.getStatements();
    newProcess.setExportPolicy(ospfExportPolicyName);

    // policy map for default information
    if (proc.getDefaultInformationOriginate()) {
      If ospfExportDefault = new If();
      ospfExportStatements.add(ospfExportDefault);
      ospfExportDefault.setComment("OSPF export default route");
      Conjunction ospfExportDefaultConditions = new Conjunction();
      List<Statement> ospfExportDefaultStatements = ospfExportDefault.getTrueStatements();
      ospfExportDefaultConditions.getConjuncts().add(MATCH_DEFAULT_ROUTE);
      long metric = proc.getDefaultInformationMetric();
      ospfExportDefaultStatements.add(new SetMetric(new LiteralLong(metric)));
      OspfMetricType metricType = proc.getDefaultInformationMetricType();
      ospfExportDefaultStatements.add(new SetOspfMetricType(metricType));
      // add default export map with metric
      String defaultOriginateMapName = proc.getDefaultInformationOriginateMap();
      boolean useAggregateDefaultOnly;
      if (defaultOriginateMapName != null) {
        useAggregateDefaultOnly = true;
        RoutingPolicy ospfDefaultGenerationPolicy =
            c.getRoutingPolicies().get(defaultOriginateMapName);
        if (ospfDefaultGenerationPolicy != null) {
          RouteMap generationRouteMap = _routeMaps.get(defaultOriginateMapName);
          generationRouteMap.getReferers().put(proc, "ospf default-originate route-map");
          GeneratedRoute.Builder route = new GeneratedRoute.Builder();
          route.setNetwork(Prefix.ZERO);
          route.setAdmin(MAX_ADMINISTRATIVE_COST);
          route.setGenerationPolicy(defaultOriginateMapName);
          newProcess.getGeneratedRoutes().add(route.build());
        }
      } else if (proc.getDefaultInformationOriginateAlways()) {
        useAggregateDefaultOnly = true;
        // add generated aggregate with no precondition
        GeneratedRoute.Builder route = new GeneratedRoute.Builder();
        route.setNetwork(Prefix.ZERO);
        route.setAdmin(MAX_ADMINISTRATIVE_COST);
        newProcess.getGeneratedRoutes().add(route.build());
      } else {
        // do not generate an aggregate default route;
        // just redistribute any existing default route with the new metric
        useAggregateDefaultOnly = false;
      }
      if (useAggregateDefaultOnly) {
        ospfExportDefaultConditions
            .getConjuncts()
            .add(new MatchProtocol(RoutingProtocol.AGGREGATE));
      }
      ospfExportDefaultStatements.add(Statements.ExitAccept.toStaticStatement());
      ospfExportDefault.setGuard(ospfExportDefaultConditions);
    }

    // policy for redistributing connected routes
    applyOspfRedistributionPolicy(
        proc,
        RoutingProtocol.CONNECTED,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_CONNECTED_MAP,
        ospfExportStatements);
    // ... for static routes
    applyOspfRedistributionPolicy(
        proc,
        RoutingProtocol.STATIC,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_STATIC_MAP,
        ospfExportStatements);
    // ... for BGP routes
    applyOspfRedistributionPolicy(
        proc,
        RoutingProtocol.BGP,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_BGP_MAP,
        ospfExportStatements);

    newProcess.setReferenceBandwidth(proc.getReferenceBandwidth());
    Ip routerId = proc.getRouterId();
    if (routerId == null) {
      Map<String, Interface> interfacesToCheck;
      Map<String, Interface> allInterfaces = oldConfig.getInterfaces();
      Map<String, Interface> loopbackInterfaces = new HashMap<>();
      for (Entry<String, Interface> e : allInterfaces.entrySet()) {
        String ifaceName = e.getKey();
        Interface iface = e.getValue();
        if (ifaceName.toLowerCase().startsWith("loopback")
            && iface.getActive()
            && iface.getAddress() != null) {
          loopbackInterfaces.put(ifaceName, iface);
        }
      }
      if (loopbackInterfaces.isEmpty()) {
        interfacesToCheck = allInterfaces;
      } else {
        interfacesToCheck = loopbackInterfaces;
      }
      Ip highestIp = Ip.ZERO;
      for (Interface iface : interfacesToCheck.values()) {
        if (!iface.getActive()) {
          continue;
        }
        for (InterfaceAddress address : iface.getAllAddresses()) {
          Ip ip = address.getIp();
          if (highestIp.asLong() < ip.asLong()) {
            highestIp = ip;
          }
        }
      }
      if (highestIp == Ip.ZERO) {
        _w.redFlag("No candidates for OSPF router-id");
        return null;
      }
      routerId = highestIp;
    }
    newProcess.setRouterId(routerId);
    return newProcess;
  }

  private org.batfish.datamodel.ospf.StubSettings toStubSettings(StubSettings stubSettings) {
    return org.batfish.datamodel.ospf.StubSettings.builder()
        .setSuppressType3(stubSettings.getNoSummary())
        .build();
  }

  private org.batfish.datamodel.ospf.NssaSettings toNssaSettings(NssaSettings nssaSettings) {
    return org.batfish.datamodel.ospf.NssaSettings.builder()
        .setDefaultOriginateType(
            nssaSettings.getDefaultInformationOriginate()
                ? OspfDefaultOriginateType.INTER_AREA
                : OspfDefaultOriginateType.NONE)
        .setSuppressType3(nssaSettings.getNoSummary())
        .build();
  }

  private org.batfish.datamodel.RipProcess toRipProcess(
      RipProcess proc, String vrfName, Configuration c, CiscoConfiguration oldConfig) {
    org.batfish.datamodel.RipProcess newProcess = new org.batfish.datamodel.RipProcess();
    org.batfish.datamodel.Vrf vrf = c.getVrfs().get(vrfName);

    // establish areas and associated interfaces
    SortedSet<Prefix> networks = proc.getNetworks();
    for (Entry<String, org.batfish.datamodel.Interface> e : vrf.getInterfaces().entrySet()) {
      String ifaceName = e.getKey();
      org.batfish.datamodel.Interface i = e.getValue();
      InterfaceAddress interfaceAddress = i.getAddress();
      if (interfaceAddress == null) {
        continue;
      }
      Prefix interfaceNetwork = interfaceAddress.getPrefix();
      if (networks.contains(interfaceNetwork)) {
        newProcess.getInterfaces().add(ifaceName);
        i.setRipEnabled(true);
        boolean passive =
            proc.getPassiveInterfaceList().contains(i.getName())
                || (proc.getPassiveInterfaceDefault()
                    && !proc.getActiveInterfaceList().contains(ifaceName));
        i.setOspfPassive(passive);
      }
    }

    String ripExportPolicyName = "~RIP_EXPORT_POLICY:" + vrfName + "~";
    RoutingPolicy ripExportPolicy = new RoutingPolicy(ripExportPolicyName, c);
    c.getRoutingPolicies().put(ripExportPolicyName, ripExportPolicy);
    List<Statement> ripExportStatements = ripExportPolicy.getStatements();
    newProcess.setExportPolicy(ripExportPolicyName);

    // policy map for default information
    if (proc.getDefaultInformationOriginate()) {
      If ripExportDefault = new If();
      ripExportStatements.add(ripExportDefault);
      ripExportDefault.setComment("RIP export default route");
      Conjunction ripExportDefaultConditions = new Conjunction();
      List<Statement> ripExportDefaultStatements = ripExportDefault.getTrueStatements();
      ripExportDefaultConditions.getConjuncts().add(MATCH_DEFAULT_ROUTE);
      long metric = proc.getDefaultInformationMetric();
      ripExportDefaultStatements.add(new SetMetric(new LiteralLong(metric)));
      // add default export map with metric
      String defaultOriginateMapName = proc.getDefaultInformationOriginateMap();
      if (defaultOriginateMapName != null) {
        RoutingPolicy ripDefaultGenerationPolicy =
            c.getRoutingPolicies().get(defaultOriginateMapName);
        if (ripDefaultGenerationPolicy != null) {
          RouteMap generationRouteMap = _routeMaps.get(defaultOriginateMapName);
          generationRouteMap.getReferers().put(proc, "rip default-originate route-map");
          GeneratedRoute.Builder route = new GeneratedRoute.Builder();
          route.setNetwork(Prefix.ZERO);
          route.setAdmin(MAX_ADMINISTRATIVE_COST);
          route.setGenerationPolicy(defaultOriginateMapName);
          newProcess.getGeneratedRoutes().add(route.build());
        }
      } else {
        // add generated aggregate with no precondition
        GeneratedRoute.Builder route = new GeneratedRoute.Builder();
        route.setNetwork(Prefix.ZERO);
        route.setAdmin(MAX_ADMINISTRATIVE_COST);
        newProcess.getGeneratedRoutes().add(route.build());
      }
      ripExportDefaultConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.AGGREGATE));
      ripExportDefaultStatements.add(Statements.ExitAccept.toStaticStatement());
      ripExportDefault.setGuard(ripExportDefaultConditions);
    }

    // policy for redistributing connected routes
    RipRedistributionPolicy rcp = proc.getRedistributionPolicies().get(RoutingProtocol.CONNECTED);
    if (rcp != null) {
      If ripExportConnected = new If();
      ripExportConnected.setComment("RIP export connected routes");
      Conjunction ripExportConnectedConditions = new Conjunction();
      ripExportConnectedConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.CONNECTED));
      List<Statement> ripExportConnectedStatements = ripExportConnected.getTrueStatements();

      Long metric = rcp.getMetric();
      boolean explicitMetric = metric != null;
      if (!explicitMetric) {
        metric = RipRedistributionPolicy.DEFAULT_REDISTRIBUTE_CONNECTED_METRIC;
      }
      ripExportStatements.add(new SetMetric(new LiteralLong(metric)));
      ripExportStatements.add(ripExportConnected);
      // add default export map with metric
      String exportConnectedRouteMapName = rcp.getRouteMap();
      if (exportConnectedRouteMapName != null) {
        RouteMap exportConnectedRouteMap = _routeMaps.get(exportConnectedRouteMapName);
        if (exportConnectedRouteMap != null) {
          exportConnectedRouteMap.getReferers().put(proc, "rip redistribute connected route-map");
          ripExportConnectedConditions
              .getConjuncts()
              .add(new CallExpr(exportConnectedRouteMapName));
        }
      }
      ripExportConnectedStatements.add(Statements.ExitAccept.toStaticStatement());
      ripExportConnected.setGuard(ripExportConnectedConditions);
    }

    // policy map for redistributing static routes
    RipRedistributionPolicy rsp = proc.getRedistributionPolicies().get(RoutingProtocol.STATIC);
    if (rsp != null) {
      If ripExportStatic = new If();
      ripExportStatic.setComment("RIP export static routes");
      Conjunction ripExportStaticConditions = new Conjunction();
      ripExportStaticConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.STATIC));
      List<Statement> ripExportStaticStatements = ripExportStatic.getTrueStatements();
      ripExportStaticConditions.getConjuncts().add(NOT_DEFAULT_ROUTE);

      Long metric = rsp.getMetric();
      boolean explicitMetric = metric != null;
      if (!explicitMetric) {
        metric = RipRedistributionPolicy.DEFAULT_REDISTRIBUTE_STATIC_METRIC;
      }
      ripExportStatements.add(new SetMetric(new LiteralLong(metric)));
      ripExportStatements.add(ripExportStatic);
      // add export map with metric
      String exportStaticRouteMapName = rsp.getRouteMap();
      if (exportStaticRouteMapName != null) {
        RouteMap exportStaticRouteMap = _routeMaps.get(exportStaticRouteMapName);
        if (exportStaticRouteMap != null) {
          exportStaticRouteMap.getReferers().put(proc, "rip redistribute static route-map");
          ripExportStaticConditions.getConjuncts().add(new CallExpr(exportStaticRouteMapName));
        }
      }
      ripExportStaticStatements.add(Statements.ExitAccept.toStaticStatement());
      ripExportStatic.setGuard(ripExportStaticConditions);
    }

    // policy map for redistributing bgp routes
    RipRedistributionPolicy rbp = proc.getRedistributionPolicies().get(RoutingProtocol.BGP);
    if (rbp != null) {
      If ripExportBgp = new If();
      ripExportBgp.setComment("RIP export bgp routes");
      Conjunction ripExportBgpConditions = new Conjunction();
      ripExportBgpConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.BGP));
      List<Statement> ripExportBgpStatements = ripExportBgp.getTrueStatements();
      ripExportBgpConditions.getConjuncts().add(NOT_DEFAULT_ROUTE);

      Long metric = rbp.getMetric();
      boolean explicitMetric = metric != null;
      if (!explicitMetric) {
        metric = RipRedistributionPolicy.DEFAULT_REDISTRIBUTE_BGP_METRIC;
      }
      ripExportStatements.add(new SetMetric(new LiteralLong(metric)));
      ripExportStatements.add(ripExportBgp);
      // add export map with metric
      String exportBgpRouteMapName = rbp.getRouteMap();
      if (exportBgpRouteMapName != null) {
        RouteMap exportBgpRouteMap = _routeMaps.get(exportBgpRouteMapName);
        if (exportBgpRouteMap != null) {
          exportBgpRouteMap.getReferers().put(proc, "rip redistribute bgp route-map");
          ripExportBgpConditions.getConjuncts().add(new CallExpr(exportBgpRouteMapName));
        }
      }
      ripExportBgpStatements.add(Statements.ExitAccept.toStaticStatement());
      ripExportBgp.setGuard(ripExportBgpConditions);
    }
    return newProcess;
  }

  private RoutingPolicy toRoutingPolicy(final Configuration c, RouteMap map) {
    boolean hasContinue =
        map.getClauses().values().stream().anyMatch(clause -> clause.getContinueLine() != null);
    if (hasContinue) {
      return toRoutingPolicies(c, map);
    }
    RoutingPolicy output = new RoutingPolicy(map.getName(), c);
    List<Statement> statements = output.getStatements();
    Map<Integer, If> clauses = new HashMap<>();
    // descend map so continue targets are available
    If followingClause = null;
    for (Entry<Integer, RouteMapClause> e : map.getClauses().descendingMap().entrySet()) {
      int clauseNumber = e.getKey();
      RouteMapClause rmClause = e.getValue();
      String clausePolicyName = getRouteMapClausePolicyName(map, clauseNumber);
      Conjunction conj = new Conjunction();
      // match ipv4s must be disjoined with match ipv6
      Disjunction matchIpOrPrefix = new Disjunction();
      for (RouteMapMatchLine rmMatch : rmClause.getMatchList()) {
        BooleanExpr matchExpr = rmMatch.toBooleanExpr(c, this, _w);
        if (rmMatch instanceof RouteMapMatchIpAccessListLine
            || rmMatch instanceof RouteMapMatchIpPrefixListLine
            || rmMatch instanceof RouteMapMatchIpv6AccessListLine
            || rmMatch instanceof RouteMapMatchIpv6PrefixListLine) {
          matchIpOrPrefix.getDisjuncts().add(matchExpr);
        } else {
          conj.getConjuncts().add(matchExpr);
        }
      }
      if (!matchIpOrPrefix.getDisjuncts().isEmpty()) {
        conj.getConjuncts().add(matchIpOrPrefix);
      }
      If ifExpr = new If();
      clauses.put(clauseNumber, ifExpr);
      ifExpr.setComment(clausePolicyName);
      ifExpr.setGuard(conj);
      List<Statement> matchStatements = ifExpr.getTrueStatements();
      for (RouteMapSetLine rmSet : rmClause.getSetList()) {
        rmSet.applyTo(matchStatements, this, c, _w);
      }
      switch (rmClause.getAction()) {
        case ACCEPT:
          matchStatements.add(Statements.ReturnTrue.toStaticStatement());
          break;

        case REJECT:
          matchStatements.add(Statements.ReturnFalse.toStaticStatement());
          break;

        default:
          throw new BatfishException("Invalid action");
      }
      if (followingClause != null) {
        ifExpr.getFalseStatements().add(followingClause);
      } else {
        ifExpr.getFalseStatements().add(Statements.ReturnLocalDefaultAction.toStaticStatement());
      }
      followingClause = ifExpr;
    }
    statements.add(followingClause);
    return output;
  }

  private RoutingPolicy toRoutingPolicies(Configuration c, RouteMap map) {
    RoutingPolicy output = new RoutingPolicy(map.getName(), c);
    List<Statement> statements = output.getStatements();
    Map<Integer, RoutingPolicy> clauses = new HashMap<>();
    // descend map so continue targets are available
    RoutingPolicy followingClause = null;
    Integer followingClauseNumber = null;
    for (Entry<Integer, RouteMapClause> e : map.getClauses().descendingMap().entrySet()) {
      int clauseNumber = e.getKey();
      RouteMapClause rmClause = e.getValue();
      String clausePolicyName = getRouteMapClausePolicyName(map, clauseNumber);
      Conjunction conj = new Conjunction();
      // match ipv4s must be disjoined with match ipv6
      Disjunction matchIpOrPrefix = new Disjunction();
      for (RouteMapMatchLine rmMatch : rmClause.getMatchList()) {
        BooleanExpr matchExpr = rmMatch.toBooleanExpr(c, this, _w);
        if (rmMatch instanceof RouteMapMatchIpAccessListLine
            || rmMatch instanceof RouteMapMatchIpPrefixListLine
            || rmMatch instanceof RouteMapMatchIpv6AccessListLine
            || rmMatch instanceof RouteMapMatchIpv6PrefixListLine) {
          matchIpOrPrefix.getDisjuncts().add(matchExpr);
        } else {
          conj.getConjuncts().add(matchExpr);
        }
      }
      if (!matchIpOrPrefix.getDisjuncts().isEmpty()) {
        conj.getConjuncts().add(matchIpOrPrefix);
      }
      RoutingPolicy clausePolicy = new RoutingPolicy(clausePolicyName, c);
      c.getRoutingPolicies().put(clausePolicyName, clausePolicy);
      If ifStatement = new If();
      clausePolicy.getStatements().add(ifStatement);
      clauses.put(clauseNumber, clausePolicy);
      ifStatement.setComment(clausePolicyName);
      ifStatement.setGuard(conj);
      List<Statement> onMatchStatements = ifStatement.getTrueStatements();
      for (RouteMapSetLine rmSet : rmClause.getSetList()) {
        rmSet.applyTo(onMatchStatements, this, c, _w);
      }
      RouteMapContinue continueStatement = rmClause.getContinueLine();
      Integer continueTarget = null;
      RoutingPolicy continueTargetPolicy = null;
      if (continueStatement != null) {
        continueTarget = continueStatement.getTarget();
        if (continueTarget == null) {
          continueTarget = followingClauseNumber;
        }
        if (continueTarget != null) {
          if (continueTarget <= clauseNumber) {
            throw new BatfishException("Can only continue to later clause");
          }
          continueTargetPolicy = clauses.get(continueTarget);
          if (continueTargetPolicy == null) {
            String name = "clause: '" + continueTarget + "' in route-map: '" + map.getName() + "'";
            undefined(
                CiscoStructureType.ROUTE_MAP_CLAUSE,
                name,
                CiscoStructureUsage.ROUTE_MAP_CONTINUE,
                continueStatement.getStatementLine());
            continueStatement = null;
          }
        } else {
          continueStatement = null;
        }
      }
      switch (rmClause.getAction()) {
        case ACCEPT:
          if (continueStatement == null) {
            onMatchStatements.add(Statements.ExitAccept.toStaticStatement());
          } else {
            onMatchStatements.add(Statements.SetDefaultActionAccept.toStaticStatement());
            onMatchStatements.add(new CallStatement(continueTargetPolicy.getName()));
          }
          break;

        case REJECT:
          onMatchStatements.add(Statements.ExitReject.toStaticStatement());
          break;

        default:
          throw new BatfishException("Invalid action");
      }
      if (followingClause != null) {
        ifStatement.getFalseStatements().add(new CallStatement(followingClause.getName()));
      } else {
        ifStatement
            .getFalseStatements()
            .add(Statements.ReturnLocalDefaultAction.toStaticStatement());
      }
      followingClause = clausePolicy;
      followingClauseNumber = clauseNumber;
    }
    statements.add(new CallStatement(followingClause.getName()));
    return output;
  }

  private RoutingPolicy toRoutingPolicy(Configuration c, RoutePolicy routePolicy) {
    String name = routePolicy.getName();
    RoutingPolicy rp = new RoutingPolicy(name, c);
    List<Statement> statements = rp.getStatements();
    for (RoutePolicyStatement routePolicyStatement : routePolicy.getStatements()) {
      routePolicyStatement.applyTo(statements, this, c, _w);
    }
    If nonBoolean =
        new If(
            BooleanExprs.CALL_STATEMENT_CONTEXT,
            Collections.singletonList(Statements.Return.toStaticStatement()),
            Collections.singletonList(Statements.DefaultAction.toStaticStatement()));
    @SuppressWarnings("unused") // TODO(https://github.com/batfish/batfish/issues/1306)
    If endPolicy =
        new If(
            BooleanExprs.CALL_EXPR_CONTEXT,
            Collections.singletonList(Statements.ReturnLocalDefaultAction.toStaticStatement()),
            Collections.singletonList(nonBoolean));
    return rp;
  }

  @Override
  public String toString() {
    if (_hostname != null) {
      return getClass().getSimpleName() + "<" + _hostname + ">";
    } else {
      return super.toString();
    }
  }

  @Override
  public Configuration toVendorIndependentConfiguration() {
    final Configuration c = new Configuration(_hostname, _vendor);
    c.getVendorFamily().setCisco(_cf);
    c.setDefaultInboundAction(LineAction.ACCEPT);
    c.setDefaultCrossZoneAction(LineAction.ACCEPT);
    c.setDnsServers(_dnsServers);
    c.setDnsSourceInterface(_dnsSourceInterface);
    c.setDomainName(_domainName);
    c.setNormalVlanRange(new SubRange(VLAN_NORMAL_MIN_CISCO, VLAN_NORMAL_MAX_CISCO));
    c.setTacacsServers(_tacacsServers);
    c.setTacacsSourceInterface(_tacacsSourceInterface);
    c.setNtpSourceInterface(_ntpSourceInterface);
    if (_cf.getNtp() != null) {
      c.setNtpServers(new TreeSet<>(_cf.getNtp().getServers().keySet()));
    }
    if (_cf.getLogging() != null) {
      c.setLoggingSourceInterface(_cf.getLogging().getSourceInterface());
      c.setLoggingServers(new TreeSet<>(_cf.getLogging().getHosts().keySet()));
    }
    c.setSnmpSourceInterface(_snmpSourceInterface);

    processLines();
    processFailoverSettings();

    // remove line login authentication lists if they don't exist
    for (Line line : _cf.getLines().values()) {
      String list = line.getLoginAuthentication();
      boolean found = false;
      Aaa aaa = _cf.getAaa();
      if (aaa != null) {
        AaaAuthentication authentication = aaa.getAuthentication();
        if (authentication != null) {
          AaaAuthenticationLogin login = authentication.getLogin();
          if (login != null && login.getLists().containsKey(list)) {
            found = true;
          }
        }
      }
      if (!found) {
        line.setLoginAuthentication(null);
      }
    }

    // initialize vrfs
    for (String vrfName : _vrfs.keySet()) {
      c.getVrfs().put(vrfName, new org.batfish.datamodel.Vrf(vrfName));
    }

    // snmp server
    if (_snmpServer != null) {
      String snmpServerVrf = _snmpServer.getVrf();
      c.getVrfs().get(snmpServerVrf).setSnmpServer(_snmpServer);
    }

    // convert as path access lists to vendor independent format
    for (IpAsPathAccessList pathList : _asPathAccessLists.values()) {
      AsPathAccessList apList = CiscoConversions.toAsPathAccessList(pathList);
      c.getAsPathAccessLists().put(apList.getName(), apList);
    }

    // convert as-path-sets to vendor independent format
    for (AsPathSet asPathSet : _asPathSets.values()) {
      AsPathAccessList apList = CiscoConversions.toAsPathAccessList(asPathSet);
      c.getAsPathAccessLists().put(apList.getName(), apList);
    }

    // convert standard/expanded community lists to community lists
    for (StandardCommunityList scList : _standardCommunityLists.values()) {
      ExpandedCommunityList ecList = scList.toExpandedCommunityList();
      CommunityList cList = CiscoConversions.toCommunityList(ecList);
      c.getCommunityLists().put(cList.getName(), cList);
    }
    for (ExpandedCommunityList ecList : _expandedCommunityLists.values()) {
      CommunityList cList = CiscoConversions.toCommunityList(ecList);
      c.getCommunityLists().put(cList.getName(), cList);
    }

    // convert prefix lists to route filter lists
    for (PrefixList prefixList : _prefixLists.values()) {
      RouteFilterList newRouteFilterList = CiscoConversions.toRouteFilterList(prefixList);
      c.getRouteFilterLists().put(newRouteFilterList.getName(), newRouteFilterList);
    }

    // convert ipv6 prefix lists to route6 filter lists
    for (Prefix6List prefixList : _prefix6Lists.values()) {
      Route6FilterList newRouteFilterList = CiscoConversions.toRoute6FilterList(prefixList);
      c.getRoute6FilterLists().put(newRouteFilterList.getName(), newRouteFilterList);
    }

    // convert standard/extended access lists to access lists or route filter
    // lists
    List<ExtendedAccessList> allACLs = new ArrayList<>();
    for (StandardAccessList saList : _standardAccessLists.values()) {
      ExtendedAccessList eaList = saList.toExtendedAccessList();
      allACLs.add(eaList);
    }
    allACLs.addAll(_extendedAccessLists.values());
    for (ExtendedAccessList eaList : allACLs) {
      if (usedForRouting(eaList)) {
        String msg = "used for routing";
        StandardAccessList parent = eaList.getParent();
        if (parent != null) {
          parent.getReferers().put(this, msg);
        } else {
          eaList.getReferers().put(this, msg);
        }
        RouteFilterList rfList = CiscoConversions.toRouteFilterList(eaList);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      IpAccessList ipaList = CiscoConversions.toIpAccessList(eaList, this._objectGroups);
      c.getIpAccessLists().put(ipaList.getName(), ipaList);
    }

    // convert each NetworkObjectGroup to IpSpace
    _networkObjectGroups.forEach(
        (name, networkObjectGroup) ->
            c.getIpSpaces().put(name, CiscoConversions.toIpSpace(networkObjectGroup)));

    // convert each ProtocolObjectGroup to IpAccessList
    _protocolObjectGroups.forEach(
        (name, protocolObjectGroup) ->
            c.getIpAccessLists()
                .put(computeProtocolObjectGroupAclName(name), toIpAccessList(protocolObjectGroup)));

    // convert each ServiceObjectGroup to IpAccessList
    _serviceObjectGroups.forEach(
        (name, serviceObjectGroup) ->
            c.getIpAccessLists()
                .put(
                    computeServiceObjectGroupAclName(name),
                    CiscoConversions.toIpAccessList(serviceObjectGroup)));

    // convert standard/extended ipv6 access lists to ipv6 access lists or
    // route6 filter
    // lists
    List<ExtendedIpv6AccessList> allIpv6ACLs = new ArrayList<>();
    for (StandardIpv6AccessList saList : _standardIpv6AccessLists.values()) {
      ExtendedIpv6AccessList eaList = saList.toExtendedIpv6AccessList();
      allIpv6ACLs.add(eaList);
    }
    allIpv6ACLs.addAll(_extendedIpv6AccessLists.values());
    for (ExtendedIpv6AccessList eaList : allIpv6ACLs) {
      if (usedForRouting(eaList)) {
        String msg = "used for routing";
        StandardIpv6AccessList parent = eaList.getParent();
        if (parent != null) {
          parent.getReferers().put(this, msg);
        } else {
          eaList.getReferers().put(this, msg);
        }
        Route6FilterList rfList = CiscoConversions.toRoute6FilterList(eaList);
        c.getRoute6FilterLists().put(rfList.getName(), rfList);
      }
      Ip6AccessList ipaList = CiscoConversions.toIp6AccessList(eaList);
      c.getIp6AccessLists().put(ipaList.getName(), ipaList);
    }

    // convert route maps to policy maps
    Set<RouteMap> routingRouteMaps = getRoutingRouteMaps();
    for (RouteMap map : _routeMaps.values()) {
      convertForPurpose(routingRouteMaps, map);
      // convert route maps to RoutingPolicy objects
      RoutingPolicy newPolicy = toRoutingPolicy(c, map);
      c.getRoutingPolicies().put(newPolicy.getName(), newPolicy);
    }

    // convert RoutePolicy to RoutingPolicy
    for (RoutePolicy routePolicy : _routePolicies.values()) {
      RoutingPolicy routingPolicy = toRoutingPolicy(c, routePolicy);
      c.getRoutingPolicies().put(routingPolicy.getName(), routingPolicy);
    }

    createInspectClassMapAcls(c);

    // create inspect policy-map ACLs
    createInspectPolicyMapAcls(c);

    // create zones
    _securityZones.forEach(
        (name, securityZone) -> {
          c.getZones().put(name, new Zone(name));
        });

    // populate zone interfaces
    _interfaces.forEach(
        (ifaceName, iface) -> {
          String zoneName = iface.getSecurityZone();
          if (zoneName == null) {
            return;
          }
          Zone zone = c.getZones().get(zoneName);
          if (zone == null) {
            return;
          }
          zone.setInterfaces(
              ImmutableSet.<String>builder().addAll(zone.getInterfaces()).add(ifaceName).build());
        });

    // create zone policies
    createZoneAcls(c);

    // convert interfaces
    _interfaces.forEach(
        (ifaceName, iface) -> {
          org.batfish.datamodel.Interface newInterface =
              toInterface(iface, c.getIpAccessLists(), c);
          String vrfName = iface.getVrf();
          if (vrfName == null) {
            throw new BatfishException("Missing vrf name for iface: '" + iface.getName() + "'");
          }
          c.getInterfaces().put(ifaceName, newInterface);
          c.getVrfs().get(vrfName).getInterfaces().put(ifaceName, newInterface);
        });

    // apply vrrp settings to interfaces
    applyVrrp(c);

    // get IKE proposals
    for (Entry<String, IsakmpPolicy> e : _isakmpPolicies.entrySet()) {
      c.getIkeProposals().put(e.getKey(), e.getValue().getProposal());
    }
    resolveKeyringIsakmpProfileAddresses();
    resolveTunnelSourceInterfaces();
    addIkePoliciesAndGateways(c);

    // ipsec proposals
    for (Entry<String, IpsecTransformSet> e : _ipsecTransformSets.entrySet()) {
      c.getIpsecProposals().put(e.getKey(), e.getValue().getProposal());
    }

    // ipsec policies
    for (IpsecProfile ipsecProfile : _ipsecProfiles.values()) {
      IpsecPolicy ipsecPolicy = toIpSecPolicy(c, ipsecProfile);
      c.getIpsecPolicies().put(ipsecPolicy.getName(), ipsecPolicy);
    }

    // ipsec vpns
    for (Entry<String, Interface> e : _interfaces.entrySet()) {
      String name = e.getKey();
      Interface iface = e.getValue();
      Tunnel tunnel = iface.getTunnel();
      if (tunnel != null && tunnel.getMode() == TunnelMode.IPSEC) {
        IpsecVpn ipsecVpn = new IpsecVpn(name, c);
        ipsecVpn.setBindInterface(c.getInterfaces().get(name));
        ipsecVpn.setIpsecPolicy(c.getIpsecPolicies().get(tunnel.getIpsecProfileName()));
        Ip source = tunnel.getSourceAddress();
        Ip destination = tunnel.getDestination();
        if (source == null || destination == null) {
          _w.redFlag("Can't match IkeGateway: tunnel source or destination is not set for " + name);
        } else {
          for (IkeGateway ikeGateway : c.getIkeGateways().values()) {
            if (source.equals(ikeGateway.getLocalIp())
                && destination.equals(ikeGateway.getAddress())) {
              ipsecVpn.setIkeGateway(ikeGateway);
            }
          }
          if (ipsecVpn.getIkeGateway() == null) {
            _w.redFlag("Can't find matching IkeGateway for " + name);
          }
        }
        c.getIpsecVpns().put(ipsecVpn.getName(), ipsecVpn);
      }
    }

    // convert routing processes
    _vrfs.forEach(
        (vrfName, vrf) -> {
          org.batfish.datamodel.Vrf newVrf = c.getVrfs().get(vrfName);

          // description
          newVrf.setDescription(vrf.getDescription());

          // add snmp trap servers to main list
          if (newVrf.getSnmpServer() != null) {
            c.getSnmpTrapServers().addAll(newVrf.getSnmpServer().getHosts().keySet());
          }

          // convert static routes
          for (StaticRoute staticRoute : vrf.getStaticRoutes()) {
            newVrf.getStaticRoutes().add(CiscoConversions.toStaticRoute(c, staticRoute));
          }

          // convert rip process
          RipProcess ripProcess = vrf.getRipProcess();
          if (ripProcess != null) {
            org.batfish.datamodel.RipProcess newRipProcess =
                toRipProcess(ripProcess, vrfName, c, this);
            newVrf.setRipProcess(newRipProcess);
          }

          // convert ospf process
          OspfProcess ospfProcess = vrf.getOspfProcess();
          if (ospfProcess != null) {
            org.batfish.datamodel.ospf.OspfProcess newOspfProcess =
                toOspfProcess(ospfProcess, vrfName, c, this);
            newVrf.setOspfProcess(newOspfProcess);
          }

          // convert isis process
          IsisProcess isisProcess = vrf.getIsisProcess();
          if (isisProcess != null) {
            org.batfish.datamodel.IsisProcess newIsisProcess =
                CiscoConversions.toIsisProcess(isisProcess, c, this);
            newVrf.setIsisProcess(newIsisProcess);
          }

          // convert bgp process (non-NX-OS)
          BgpProcess bgpProcess = vrf.getBgpProcess();
          if (bgpProcess != null) {
            org.batfish.datamodel.BgpProcess newBgpProcess = toBgpProcess(c, bgpProcess, vrfName);
            newVrf.setBgpProcess(newBgpProcess);
          }

          // convert NX-OS BGP configuration
          CiscoNxBgpVrfConfiguration nxBgp = vrf.getBgpNxConfig();
          if (nxBgp != null) {
            org.batfish.datamodel.BgpProcess newBgpProcess =
                toNxBgpProcess(c, getNxBgpGlobalConfiguration(), nxBgp, vrfName);
            newVrf.setBgpProcess(newBgpProcess);
          }
        });

    // warn about references to undefined peer groups
    for (Entry<String, Integer> e : _undefinedPeerGroups.entrySet()) {
      undefined(
          CiscoStructureType.BGP_PEER_GROUP,
          e.getKey(),
          CiscoStructureUsage.BGP_NEIGHBOR_STATEMENT,
          e.getValue());
    }

    markConcreteStructure(
        CiscoStructureType.INTERFACE,
        CiscoStructureUsage.BGP_UPDATE_SOURCE_INTERFACE,
        CiscoStructureUsage.INTERFACE_SELF_REF,
        CiscoStructureUsage.ROUTER_VRRP_INTERFACE);

    // mark references to ACLs that may not appear in data model
    markIpOrMacAcls(
        CiscoStructureUsage.CLASS_MAP_ACCESS_GROUP, CiscoStructureUsage.CLASS_MAP_ACCESS_LIST);
    markIpv4Acls(
        CiscoStructureUsage.CONTROL_PLANE_ACCESS_GROUP,
        CiscoStructureUsage.INTERFACE_IGMP_STATIC_GROUP_ACL,
        CiscoStructureUsage.INTERFACE_INCOMING_FILTER,
        CiscoStructureUsage.INTERFACE_IP_VERIFY_ACCESS_LIST,
        CiscoStructureUsage.INTERFACE_OUTGOING_FILTER,
        CiscoStructureUsage.INTERFACE_PIM_NEIGHBOR_FILTER,
        CiscoStructureUsage.IP_NAT_DESTINATION_ACCESS_LIST,
        CiscoStructureUsage.IP_NAT_SOURCE_ACCESS_LIST,
        CiscoStructureUsage.LINE_ACCESS_CLASS_LIST,
        CiscoStructureUsage.MANAGEMENT_SSH_ACCESS_GROUP,
        CiscoStructureUsage.MANAGEMENT_TELNET_ACCESS_GROUP,
        CiscoStructureUsage.MSDP_PEER_SA_LIST,
        CiscoStructureUsage.NTP_ACCESS_GROUP,
        CiscoStructureUsage.PIM_ACCEPT_REGISTER_ACL,
        CiscoStructureUsage.PIM_ACCEPT_RP_ACL,
        CiscoStructureUsage.PIM_RP_ADDRESS_ACL,
        CiscoStructureUsage.PIM_RP_ANNOUNCE_FILTER,
        CiscoStructureUsage.PIM_RP_CANDIDATE_ACL,
        CiscoStructureUsage.PIM_SEND_RP_ANNOUNCE_ACL,
        CiscoStructureUsage.PIM_SPT_THRESHOLD_ACL,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_ACCESS_LIST,
        CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL4,
        CiscoStructureUsage.SSH_IPV4_ACL);
    markIpv6Acls(
        CiscoStructureUsage.LINE_ACCESS_CLASS_LIST6,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST,
        CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL6,
        CiscoStructureUsage.SSH_IPV6_ACL);
    markAcls(
        CiscoStructureUsage.COPS_LISTENER_ACCESS_LIST,
        CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL,
        CiscoStructureUsage.INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP,
        CiscoStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL,
        CiscoStructureUsage.INTERFACE_IP_INBAND_ACCESS_GROUP,
        CiscoStructureUsage.IP_NAT_SOURCE_ACCESS_LIST,
        CiscoStructureUsage.RIP_DISTRIBUTE_LIST,
        CiscoStructureUsage.ROUTER_ISIS_DISTRIBUTE_LIST_ACL,
        CiscoStructureUsage.SNMP_SERVER_FILE_TRANSFER_ACL,
        CiscoStructureUsage.SNMP_SERVER_TFTP_SERVER_LIST,
        CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL,
        CiscoStructureUsage.SSH_ACL,
        CiscoStructureUsage.WCCP_GROUP_LIST,
        CiscoStructureUsage.WCCP_REDIRECT_LIST,
        CiscoStructureUsage.WCCP_SERVICE_LIST);

    markConcreteStructure(
        CiscoStructureType.PREFIX_LIST,
        CiscoStructureUsage.BGP_INBOUND_PREFIX_LIST,
        CiscoStructureUsage.BGP_OUTBOUND_PREFIX_LIST,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_PREFIX_LIST);
    markConcreteStructure(
        CiscoStructureType.PREFIX6_LIST,
        CiscoStructureUsage.BGP_INBOUND_PREFIX6_LIST,
        CiscoStructureUsage.BGP_OUTBOUND_PREFIX6_LIST,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_PREFIX_LIST);

    markConcreteStructure(
        CiscoStructureType.PREFIX_SET, CiscoStructureUsage.ROUTE_POLICY_PREFIX_SET);

    // mark references to route-maps
    markConcreteStructure(
        CiscoStructureType.ROUTE_MAP,
        CiscoStructureUsage.BGP_AGGREGATE_ATTRIBUTE_MAP,
        CiscoStructureUsage.BGP_DEFAULT_ORIGINATE_ROUTE_MAP,
        CiscoStructureUsage.BGP_INBOUND_ROUTE_MAP,
        CiscoStructureUsage.BGP_INBOUND_ROUTE6_MAP,
        CiscoStructureUsage.BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP,
        CiscoStructureUsage.BGP_NETWORK_ORIGINATION_ROUTE_MAP,
        CiscoStructureUsage.BGP_NETWORK6_ORIGINATION_ROUTE_MAP,
        CiscoStructureUsage.BGP_OUTBOUND_ROUTE_MAP,
        CiscoStructureUsage.BGP_OUTBOUND_ROUTE6_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_CONNECTED_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_EIGRP_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_ISIS_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_LISP_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_OSPF_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_OSPFV3_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_RIP_MAP,
        CiscoStructureUsage.BGP_REDISTRIBUTE_STATIC_MAP,
        CiscoStructureUsage.BGP_ROUTE_MAP_ADVERTISE,
        CiscoStructureUsage.BGP_ROUTE_MAP_ATTRIBUTE,
        CiscoStructureUsage.BGP_ROUTE_MAP_OTHER,
        CiscoStructureUsage.BGP_ROUTE_MAP_SUPPRESS,
        CiscoStructureUsage.BGP_ROUTE_MAP_UNSUPPRESS,
        CiscoStructureUsage.BGP_VRF_AGGREGATE_ROUTE_MAP,
        CiscoStructureUsage.INTERFACE_POLICY_ROUTING_MAP,
        CiscoStructureUsage.OSPF_DEFAULT_ORIGINATE_ROUTE_MAP,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_BGP_MAP,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_CONNECTED_MAP,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_STATIC_MAP,
        CiscoStructureUsage.PIM_ACCEPT_REGISTER_ROUTE_MAP,
        CiscoStructureUsage.RIP_DEFAULT_ORIGINATE_ROUTE_MAP,
        CiscoStructureUsage.RIP_REDISTRIBUTE_BGP_MAP,
        CiscoStructureUsage.RIP_REDISTRIBUTE_CONNECTED_MAP,
        CiscoStructureUsage.RIP_REDISTRIBUTE_STATIC_MAP);

    markConcreteStructure(
        CiscoStructureType.BGP_TEMPLATE_PEER, CiscoStructureUsage.BGP_INHERITED_PEER);
    markConcreteStructure(
        CiscoStructureType.BGP_TEMPLATE_PEER_POLICY, CiscoStructureUsage.BGP_INHERITED_PEER_POLICY);
    markConcreteStructure(
        CiscoStructureType.BGP_TEMPLATE_PEER_SESSION, CiscoStructureUsage.BGP_INHERITED_SESSION);

    // Cable
    markConcreteStructure(
        CiscoStructureType.DEPI_CLASS, CiscoStructureUsage.DEPI_TUNNEL_DEPI_CLASS);
    markConcreteStructure(
        CiscoStructureType.DEPI_TUNNEL,
        CiscoStructureUsage.CONTROLLER_DEPI_TUNNEL,
        CiscoStructureUsage.DEPI_TUNNEL_PROTECT_TUNNEL);
    markConcreteStructure(
        CiscoStructureType.DOCSIS_POLICY, CiscoStructureUsage.DOCSIS_GROUP_DOCSIS_POLICY);
    markConcreteStructure(
        CiscoStructureType.DOCSIS_POLICY_RULE,
        CiscoStructureUsage.DOCSIS_POLICY_DOCSIS_POLICY_RULE);
    markConcreteStructure(
        CiscoStructureType.SERVICE_CLASS, CiscoStructureUsage.QOS_ENFORCE_RULE_SERVICE_CLASS);

    // L2tp
    markConcreteStructure(
        CiscoStructureType.L2TP_CLASS, CiscoStructureUsage.DEPI_TUNNEL_L2TP_CLASS);

    // Crypto, Isakmp, and Ipsec
    markConcreteStructure(
        CiscoStructureType.ISAKMP_PROFILE,
        CiscoStructureUsage.ISAKMP_PROFILE_SELF_REF,
        CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ISAKMP_PROFILE,
        CiscoStructureUsage.IPSEC_PROFILE_ISAKMP_PROFILE);
    markConcreteStructure(
        CiscoStructureType.ISAKMP_POLICY, CiscoStructureUsage.ISAKMP_POLICY_SELF_REF);
    markConcreteStructure(
        CiscoStructureType.IPSEC_PROFILE, CiscoStructureUsage.TUNNEL_PROTECTION_IPSEC_PROFILE);
    markConcreteStructure(
        CiscoStructureType.IPSEC_TRANSFORM_SET,
        CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_TRANSFORM_SET,
        CiscoStructureUsage.IPSEC_PROFILE_TRANSFORM_SET);
    markConcreteStructure(CiscoStructureType.KEYRING, CiscoStructureUsage.ISAKMP_PROFILE_KEYRING);

    // class-map
    markConcreteStructure(
        CiscoStructureType.INSPECT_CLASS_MAP, CiscoStructureUsage.INSPECT_POLICY_MAP_INSPECT_CLASS);
    markConcreteStructure(
        CiscoStructureType.CLASS_MAP,
        CiscoStructureUsage.POLICY_MAP_CLASS,
        CiscoStructureUsage.POLICY_MAP_EVENT_CLASS);

    // policy-map
    markConcreteStructure(
        CiscoStructureType.INSPECT_POLICY_MAP,
        CiscoStructureUsage.ZONE_PAIR_INSPECT_SERVICE_POLICY);
    markConcreteStructure(
        CiscoStructureType.POLICY_MAP, CiscoStructureUsage.INTERFACE_SERVICE_POLICY);

    // object-group
    markConcreteStructure(
        CiscoStructureType.NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.NETWORK_OBJECT_GROUP_GROUP_OBJECT,
        CiscoStructureUsage.NETWORK_OBJECT_GROUP_NETWORK_OBJECT);
    markAbstractStructure(
        CiscoStructureType.PROTOCOL_OR_SERVICE_OBJECT_GROUP,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP,
        ImmutableList.of(
            CiscoStructureType.PROTOCOL_OBJECT_GROUP, CiscoStructureType.SERVICE_OBJECT_GROUP));

    // service template
    markConcreteStructure(
        CiscoStructureType.SERVICE_TEMPLATE,
        CiscoStructureUsage.CLASS_MAP_SERVICE_TEMPLATE,
        CiscoStructureUsage.CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE,
        CiscoStructureUsage.POLICY_MAP_EVENT_CLASS_ACTIVATE);

    // zone
    markConcreteStructure(
        CiscoStructureType.SECURITY_ZONE,
        CiscoStructureUsage.INTERFACE_ZONE_MEMBER,
        CiscoStructureUsage.ZONE_PAIR_DESTINATION_ZONE,
        CiscoStructureUsage.ZONE_PAIR_SOURCE_ZONE);

    markConcreteStructure(CiscoStructureType.NAT_POOL, CiscoStructureUsage.IP_NAT_SOURCE_POOL);
    markConcreteStructure(
        CiscoStructureType.AS_PATH_ACCESS_LIST,
        CiscoStructureUsage.BGP_NEIGHBOR_FILTER_AS_PATH_ACCESS_LIST,
        CiscoStructureUsage.ROUTE_MAP_MATCH_AS_PATH_ACCESS_LIST);

    // record references to defined structures
    recordCommunityLists();
    recordDocsisPolicies();
    recordDocsisPolicyRules();
    recordPeerGroups();
    recordPeerSessions();
    recordServiceClasses();

    c.simplifyRoutingPolicies();

    c.computeRoutingPolicySources(_w);

    return c;
  }

  private IpAccessList toIpAccessList(ProtocolObjectGroup protocolObjectGroup) {
    return IpAccessList.builder()
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(protocolObjectGroup.toAclLineMatchExpr())
                    .build()))
        .setName(computeProtocolObjectGroupAclName(protocolObjectGroup.getName()))
        .build();
  }

  /** Converts an Ipsec Profile to Ipsec policy */
  private IpsecPolicy toIpSecPolicy(Configuration configuration, IpsecProfile ipsecProfile) {
    String name = ipsecProfile.getName();

    IpsecPolicy policy = new IpsecPolicy(name);
    policy.setPfsKeyGroup(ipsecProfile.getPfsGroup());

    for (String transformSetName : ipsecProfile.getTransformSets()) {
      IpsecProposal ipsecProposalName = configuration.getIpsecProposals().get(transformSetName);
      if (ipsecProposalName != null) {
        policy.getProposals().add(ipsecProposalName);
      }
    }

    String isakmpProfileName = ipsecProfile.getIsakmpProfile();
    if (isakmpProfileName != null) {
      IkeGateway ikeGateway = configuration.getIkeGateways().get(isakmpProfileName);
      if (ikeGateway != null) {
        policy.setIkeGateway(ikeGateway);
      }
    }

    return policy;
  }

  private void createInspectClassMapAcls(Configuration c) {
    _inspectClassMaps.forEach(
        (inspectClassMapName, inspectClassMap) -> {
          String inspectClassMapAclName = computeInspectClassMapAclName(inspectClassMapName);
          MatchSemantics matchSemantics = inspectClassMap.getMatchSemantics();
          List<AclLineMatchExpr> matchConditions =
              inspectClassMap
                  .getMatches()
                  .stream()
                  .map(
                      inspectClassMapMatch ->
                          inspectClassMapMatch.toAclLineMatchExpr(this, c, matchSemantics, _w))
                  .collect(ImmutableList.toImmutableList());
          AclLineMatchExpr matchClassMap;
          switch (matchSemantics) {
            case MATCH_ALL:
              matchClassMap = new AndMatchExpr(matchConditions);
              break;
            case MATCH_ANY:
              matchClassMap = new OrMatchExpr(matchConditions);
              break;
            default:
              throw new BatfishException(
                  String.format(
                      "Unsupported %s: %s", MatchSemantics.class.getSimpleName(), matchSemantics));
          }
          IpAccessList.builder()
              .setOwner(c)
              .setName(inspectClassMapAclName)
              .setLines(
                  ImmutableList.of(
                      IpAccessListLine.accepting().setMatchCondition(matchClassMap).build()))
              .build();
        });
  }

  private void createInspectPolicyMapAcls(Configuration c) {
    _inspectPolicyMaps.forEach(
        (inspectPolicyMapName, inspectPolicyMap) -> {
          String inspectPolicyMapAclName = computeInspectPolicyMapAclName(inspectPolicyMapName);
          ImmutableList.Builder<IpAccessListLine> policyMapAclLines = ImmutableList.builder();
          inspectPolicyMap
              .getInspectClasses()
              .forEach(
                  (inspectClassName, inspectPolicyMapInspectClass) -> {
                    PolicyMapClassAction action = inspectPolicyMapInspectClass.getAction();
                    if (action == null) {
                      return;
                    }
                    String inspectClassMapAclName = computeInspectClassMapAclName(inspectClassName);
                    if (!c.getIpAccessLists().containsKey(inspectClassMapAclName)) {
                      return;
                    }
                    AclLineMatchExpr matchCondition = new PermittedByAcl(inspectClassMapAclName);
                    switch (action) {
                      case DROP:
                        policyMapAclLines.add(
                            IpAccessListLine.rejecting()
                                .setMatchCondition(matchCondition)
                                .setName(
                                    String.format(
                                        "Drop if matched by class-map: '%s'", inspectClassName))
                                .build());
                        break;

                      case INSPECT:
                        policyMapAclLines.add(
                            IpAccessListLine.accepting()
                                .setMatchCondition(matchCondition)
                                .setName(
                                    String.format(
                                        "Inspect if matched by class-map: '%s'", inspectClassName))
                                .build());
                        break;

                      case PASS:
                        policyMapAclLines.add(
                            IpAccessListLine.accepting()
                                .setMatchCondition(matchCondition)
                                .setName(
                                    String.format(
                                        "Pass if matched by class-map: '%s'", inspectClassName))
                                .build());
                        break;

                      default:
                        _w.unimplemented("Unimplemented policy-map class action: " + action);
                        return;
                    }
                  });
          policyMapAclLines.add(
              IpAccessListLine.builder()
                  .setAction(inspectPolicyMap.getClassDefaultAction())
                  .setMatchCondition(TrueExpr.INSTANCE)
                  .setName(
                      String.format(
                          "class-default action: %s", inspectPolicyMap.getClassDefaultAction()))
                  .build());
          IpAccessList.builder()
              .setOwner(c)
              .setName(inspectPolicyMapAclName)
              .setLines(policyMapAclLines.build())
              .build();
        });
  }

  private void createZoneAcls(Configuration c) {
    // Mapping: zoneName -> (MatchSrcInterface for interfaces in zone)
    Map<String, MatchSrcInterface> matchSrcInterfaceBySrcZone =
        toImmutableMap(
            c.getZones(),
            Entry::getKey,
            zoneByNameEntry -> new MatchSrcInterface(zoneByNameEntry.getValue().getInterfaces()));

    c.getZones()
        .forEach(
            (zoneName, zone) -> {
              // Don't bother if zone is empty
              if (zone.getInterfaces().isEmpty()) {
                return;
              }

              ImmutableList.Builder<IpAccessListLine> zonePolicies = ImmutableList.builder();

              // Allow traffic originating from device (no source interface)
              zonePolicies.add(
                  IpAccessListLine.accepting()
                      .setMatchCondition(OriginatingFromDevice.INSTANCE)
                      .setName("Allow traffic originating from this device")
                      .build());

              // Allow traffic staying within this zone
              zonePolicies.add(
                  IpAccessListLine.accepting()
                      .setMatchCondition(matchSrcInterfaceBySrcZone.get(zoneName))
                      .setName(
                          String.format(
                              "Allow traffic received on interface in same zone: '%s'", zoneName))
                      .build());

              /*
               * Add zone-pair policies
               */
              // zoneName refers to dstZone
              Map<String, SecurityZonePair> zonePairsBySrcZoneName =
                  _securityZonePairs.get(zoneName);
              if (zonePairsBySrcZoneName != null) {
                zonePairsBySrcZoneName.forEach(
                    (srcZoneName, zonePair) ->
                        createZonePairAcl(
                                c,
                                matchSrcInterfaceBySrcZone.get(srcZoneName),
                                zoneName,
                                srcZoneName,
                                zonePair.getInspectPolicyMap())
                            .ifPresent(zonePolicies::add));
              }

              IpAccessList.builder()
                  .setName(computeZoneOutgoingAclName(zoneName))
                  .setOwner(c)
                  .setLines(zonePolicies.build())
                  .build();
            });
  }

  public Optional<IpAccessListLine> createZonePairAcl(
      Configuration c,
      MatchSrcInterface matchSrcZoneInterface,
      String dstZoneName,
      String srcZoneName,
      String inspectPolicyMapName) {
    if (!_securityZones.containsKey(srcZoneName)) {
      return Optional.empty();
    }
    if (inspectPolicyMapName == null) {
      return Optional.empty();
    }
    String inspectPolicyMapAclName = computeInspectPolicyMapAclName(inspectPolicyMapName);
    if (!c.getIpAccessLists().containsKey(inspectPolicyMapAclName)) {
      return Optional.empty();
    }
    PermittedByAcl permittedByPolicyMap = new PermittedByAcl(inspectPolicyMapAclName);
    String zonePairAclName = computeZonePairAclName(srcZoneName, dstZoneName);
    IpAccessList.builder()
        .setName(zonePairAclName)
        .setOwner(c)
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(
                        new AndMatchExpr(
                            ImmutableList.of(matchSrcZoneInterface, permittedByPolicyMap)))
                    .setName(
                        String.format(
                            "Allow traffic received on interface in zone '%s' permitted by policy-map: '%s'",
                            srcZoneName, inspectPolicyMapName))
                    .build()))
        .build();
    return Optional.of(
        IpAccessListLine.accepting()
            .setMatchCondition(new PermittedByAcl(zonePairAclName))
            .setName(
                String.format(
                    "Allow traffic from zone '%s' to '%s' permitted by service-policy: %s",
                    srcZoneName, dstZoneName, inspectPolicyMapName))
            .build());
  }

  public static String computeZoneOutgoingAclName(@Nonnull String zoneName) {
    return String.format("~ZONE_OUTGOING_ACL~%s~", zoneName);
  }

  public static String computeZonePairAclName(
      @Nonnull String srcZoneName, @Nonnull String dstZoneName) {
    return String.format("~ZONE_PAIR_ACL~SRC~%s~DST~%s", srcZoneName, dstZoneName);
  }

  public static String computeInspectPolicyMapAclName(@Nonnull String inspectPolicyMapName) {
    return String.format("~INSPECT_POLICY_MAP_ACL~%s~", inspectPolicyMapName);
  }

  public static String computeInspectClassMapAclName(@Nonnull String inspectClassMapName) {
    return String.format("~INSPECT_CLASS_MAP_ACL~%s~", inspectClassMapName);
  }

  private void addIkePoliciesAndGateways(Configuration c) {
    // get IKE gateways and policies from Cisco isakmp profiles and keyrings
    for (Entry<String, IsakmpProfile> e : _isakmpProfiles.entrySet()) {
      String name = e.getKey();
      IsakmpProfile isakmpProfile = e.getValue();
      IkePolicy ikePolicy = new IkePolicy(name);
      c.getIkePolicies().put(name, ikePolicy);
      ikePolicy.setProposals(c.getIkeProposals());

      String keyringName = isakmpProfile.getKeyring();
      if (keyringName == null) {
        _w.redFlag(
            String.format(
                "Cannot get PSK hash since keyring not configured for isakmpProfile %s", name));
      } else if (_keyrings.containsKey(keyringName)) {
        Keyring keyring = _keyrings.get(keyringName);
        // LocalAddress can only be Ip.AUTO if LocalInterfaceName contains an invalid interface
        if (Objects.equals(isakmpProfile.getLocalAddress(), Ip.AUTO)) {
          _w.redFlag(
              String.format(
                  "Invalid local address interface configured for ISAKMP profile %s", name));
        } else if (keyring.match(
            isakmpProfile.getLocalAddress(), isakmpProfile.getMatchIdentity())) {
          ikePolicy.setPreSharedKeyHash(keyring.getKey());
        } else {
          _w.redFlag(
              String.format(
                  "The addresses of keyring %s do not match isakmpProfile %s", keyringName, name));
        }
      }

      Ip localAddress = isakmpProfile.getLocalAddress();
      Prefix remotePrefix = isakmpProfile.getMatchIdentity();
      if (localAddress == null || remotePrefix == null) {
        _w.redFlag(
            String.format(
                "Can't get IkeGateway: Local or remote address is not set for isakmpProfile %s",
                name));
      } else {
        IkeGateway ikeGateway = new IkeGateway(e.getKey());
        c.getIkeGateways().put(name, ikeGateway);
        ikeGateway.setAddress(remotePrefix.getStartIp());
        Interface oldIface = getInterfaceByTunnelAddresses(localAddress, remotePrefix);
        if (oldIface != null) {
          ikeGateway.setExternalInterface(c.getInterfaces().get(oldIface.getName()));
        } else {
          _w.redFlag(
              String.format(
                  "External interface not found for ikeGateway for isakmpProfile %s", name));
        }
        ikeGateway.setIkePolicy(ikePolicy);
        ikeGateway.setLocalIp(isakmpProfile.getLocalAddress());
      }
    }
  }

  private boolean usedForRouting(ExtendedAccessList eaList) {
    String eaListName = eaList.getName();
    String currentMapName;
    for (Vrf vrf : _vrfs.values()) {
      // check ospf policies
      OspfProcess ospfProcess = vrf.getOspfProcess();
      if (ospfProcess != null) {
        for (OspfRedistributionPolicy rp : ospfProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpAccessList(eaListName, currentMapName)) {
            return true;
          }
        }
        currentMapName = ospfProcess.getDefaultInformationOriginateMap();
        if (containsIpAccessList(eaListName, currentMapName)) {
          return true;
        }
      }
      RipProcess ripProcess = vrf.getRipProcess();
      if (ripProcess != null) {
        // check rip distribute lists
        if (ripProcess.getDistributeListInAcl()
            && ripProcess.getDistributeListIn().equals(eaListName)) {
          return true;
        }
        if (ripProcess.getDistributeListOutAcl()
            && ripProcess.getDistributeListOut().equals(eaListName)) {
          return true;
        }
        // check rip redistribution policies
        for (RipRedistributionPolicy rp : ripProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpAccessList(eaListName, currentMapName)) {
            return true;
          }
        }
      }
      // check bgp policies
      BgpProcess bgpProcess = vrf.getBgpProcess();
      if (bgpProcess != null) {
        for (BgpRedistributionPolicy rp : bgpProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpAccessList(eaListName, currentMapName)) {
            return true;
          }
        }
        for (BgpPeerGroup pg : bgpProcess.getAllPeerGroups()) {
          currentMapName = pg.getInboundRouteMap();
          if (containsIpAccessList(eaListName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getOutboundRouteMap();
          if (containsIpAccessList(eaListName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getDefaultOriginateMap();
          if (containsIpAccessList(eaListName, currentMapName)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean usedForRouting(ExtendedIpv6AccessList eaList) {
    String eaListName = eaList.getName();
    String currentMapName;
    for (Vrf vrf : _vrfs.values()) {
      // check ospf policies
      OspfProcess ospfProcess = vrf.getOspfProcess();
      if (ospfProcess != null) {
        for (OspfRedistributionPolicy rp : ospfProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpv6AccessList(eaListName, currentMapName)) {
            return true;
          }
        }
        currentMapName = ospfProcess.getDefaultInformationOriginateMap();
        if (containsIpAccessList(eaListName, currentMapName)) {
          return true;
        }
      }
      // check bgp policies
      BgpProcess bgpProcess = vrf.getBgpProcess();
      if (bgpProcess != null) {
        for (BgpRedistributionPolicy rp : bgpProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpv6AccessList(eaListName, currentMapName)) {
            return true;
          }
        }
        for (BgpPeerGroup pg : bgpProcess.getAllPeerGroups()) {
          currentMapName = pg.getInboundRouteMap();
          if (containsIpv6AccessList(eaListName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getInboundRoute6Map();
          if (containsIpv6AccessList(eaListName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getOutboundRouteMap();
          if (containsIpv6AccessList(eaListName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getOutboundRoute6Map();
          if (containsIpv6AccessList(eaListName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getDefaultOriginateMap();
          if (containsIpv6AccessList(eaListName, currentMapName)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void recordCommunityLists() {
    recordStructure(_expandedCommunityLists, CiscoStructureType.COMMUNITY_LIST_EXPANDED);
    recordStructure(_standardCommunityLists, CiscoStructureType.COMMUNITY_LIST_STANDARD);
  }

  private void recordDocsisPolicies() {
    if (_cf.getCable() != null) {
      recordStructure(_cf.getCable().getDocsisPolicies(), CiscoStructureType.DOCSIS_POLICY);
    }
  }

  private void recordDocsisPolicyRules() {
    if (_cf.getCable() != null) {
      recordStructure(_cf.getCable().getDocsisPolicyRules(), CiscoStructureType.DOCSIS_POLICY_RULE);
    }
  }

  private void recordPeerGroups() {
    for (Vrf vrf : getVrfs().values()) {
      BgpProcess proc = vrf.getBgpProcess();
      if (proc == null) {
        continue;
      }
      for (NamedBgpPeerGroup peerGroup : proc.getNamedPeerGroups().values()) {
        int numReferrers =
            (_unusedPeerGroups != null && _unusedPeerGroups.contains(peerGroup))
                ? 0
                // we are not properly counting references for peer groups
                : DefinedStructureInfo.UNKNOWN_NUM_REFERRERS;

        recordStructure(
            CiscoStructureType.BGP_PEER_GROUP,
            peerGroup.getName(),
            numReferrers,
            peerGroup.getDefinitionLine());
      }
    }
  }

  private void recordPeerSessions() {
    for (Vrf vrf : getVrfs().values()) {
      BgpProcess proc = vrf.getBgpProcess();
      if (proc == null) {
        continue;
      }
      for (NamedBgpPeerGroup peerSession : proc.getPeerSessions().values()) {
        // use -1 for now; we are not counting references for peerSessions
        int numReferrers =
            (_unusedPeerSessions != null && _unusedPeerSessions.contains(peerSession))
                ? 0
                // we are not properly counting references for peer sessions
                : DefinedStructureInfo.UNKNOWN_NUM_REFERRERS;
        recordStructure(
            CiscoStructureType.BGP_PEER_SESSION,
            peerSession.getName(),
            numReferrers,
            peerSession.getDefinitionLine());
      }
    }
  }

  private void recordServiceClasses() {
    if (_cf.getCable() != null) {
      recordStructure(_cf.getCable().getServiceClasses(), CiscoStructureType.SERVICE_CLASS);
    }
  }

  /**
   * Resolves the addresses of the interfaces used in localInterfaceName of IsaKmpProfiles and
   * Keyrings
   */
  private void resolveKeyringIsakmpProfileAddresses() {
    Map<String, Ip> ifaceNameToPrimaryIp = computeInterfaceOwnedPrimaryIp(_interfaces);

    _keyrings
        .values()
        .stream()
        .filter(keyring -> keyring.getLocalInterfaceName() != null)
        .forEach(
            keyring -> {
              keyring.setLocalAddress(
                  firstNonNull(ifaceNameToPrimaryIp.get(keyring.getLocalInterfaceName()), Ip.AUTO));
            });

    _isakmpProfiles
        .values()
        .stream()
        .filter(isakmpProfile -> isakmpProfile.getLocalInterfaceName() != null)
        .forEach(
            isakmpProfile -> {
              isakmpProfile.setLocalAddress(
                  firstNonNull(
                      ifaceNameToPrimaryIp.get(isakmpProfile.getLocalInterfaceName()), Ip.AUTO));
            });
  }

  /** Resolves the addresses of the interfaces used in sourceInterfaceName of Tunnel interfaces */
  private void resolveTunnelSourceInterfaces() {
    Map<String, Ip> ifaceNameToPrimaryIp = computeInterfaceOwnedPrimaryIp(_interfaces);

    for (Interface iface : _interfaces.values()) {
      Tunnel tunnel = iface.getTunnel();
      if (tunnel != null && tunnel.getSourceInterfaceName() != null) {
        tunnel.setSourceAddress(ifaceNameToPrimaryIp.get(tunnel.getSourceInterfaceName()));
      }
    }
  }

  public Map<String, NetworkObjectGroup> getNetworkObjectGroups() {
    return _networkObjectGroups;
  }

  public Map<String, ObjectGroup> getObjectGroups() {
    return _objectGroups;
  }

  public Map<String, ProtocolObjectGroup> getProtocolObjectGroups() {
    return _protocolObjectGroups;
  }

  public Map<String, ServiceObjectGroup> getServiceObjectGroups() {
    return _serviceObjectGroups;
  }

  public Map<String, InspectClassMap> getInspectClassMaps() {
    return _inspectClassMaps;
  }

  public Map<String, InspectPolicyMap> getInspectPolicyMaps() {
    return _inspectPolicyMaps;
  }

  public Map<String, Map<String, SecurityZonePair>> getSecurityZonePairs() {
    return _securityZonePairs;
  }

  public Map<String, SecurityZone> getSecurityZones() {
    return _securityZones;
  }
}
