package org.batfish.representation.cisco;

import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.math.BigInteger;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.ReferenceCountedStructure;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GeneratedRoute6;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeGateway;
import org.batfish.datamodel.IkePolicy;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.Ip6AccessListLine;
import org.batfish.datamodel.Ip6Wildcard;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.IpsecPolicy;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.IsisInterfaceMode;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfAreaSummary;
import org.batfish.datamodel.OspfMetricType;
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
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;
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
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
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
import org.batfish.datamodel.vendor_family.cisco.Cable;
import org.batfish.datamodel.vendor_family.cisco.CiscoFamily;
import org.batfish.datamodel.vendor_family.cisco.Line;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.representation.cisco.nx.CiscoNxBgpGlobalConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;
import org.batfish.vendor.VendorConfiguration;

public final class CiscoConfiguration extends VendorConfiguration {

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

  private static final int MAX_ADMINISTRATIVE_COST = 32767;

  public static final String NXOS_MANAGEMENT_INTERFACE_PREFIX = "mgmt";

  private static final long serialVersionUID = 1L;

  public static final String VENDOR_NAME = "cisco";

  private static final int VLAN_NORMAL_MAX_CISCO = 1005;

  private static final int VLAN_NORMAL_MIN_CISCO = 2;

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

  private final Set<String> _bgpVrfAggregateAddressRouteMaps;

  private final CiscoFamily _cf;

  /**
   * These can be either ipv4 or ipv6, so we must check both protocols for access-lists when doing
   * undefined references check
   */
  private final Set<String> _classMapAccessGroups;

  private final Set<String> _controlPlaneAccessGroups;

  private final Set<String> _cryptoAcls;

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

  private final Set<String> _igmpAcls;

  private final Map<String, InspectClassMap> _inspectClassMaps;

  private final Map<String, InspectPolicyMap> _inspectPolicyMaps;

  private final Map<String, Interface> _interfaces;

  private final Set<String> _ipNatDestinationAccessLists;

  private final Set<String> _ipPimNeighborFilters;

  private final Map<String, IpsecProfile> _ipsecProfiles;

  private final Map<String, IpsecTransformSet> _ipsecTransformSets;

  private final Map<String, IsakmpPolicy> _isakmpPolicies;

  private final Map<String, IsakmpProfile> _isakmpProfiles;

  private final Map<String, Keyring> _keyrings;

  private final Set<String> _lineAccessClassLists;

  private final Set<String> _lineIpv6AccessClassLists;

  private final Map<String, MacAccessList> _macAccessLists;

  private final Set<String> _managementAccessGroups;

  private final Set<String> _msdpPeerSaLists;

  private final Map<String, NatPool> _natPools;

  private final Map<String, NetworkObjectGroup> _networkObjectGroups;

  private final Set<String> _ntpAccessGroups;

  private String _ntpSourceInterface;

  private CiscoNxBgpGlobalConfiguration _nxBgpGlobalConfiguration;

  private final Set<String> _pimAcls;

  private final Set<String> _pimRouteMaps;

  private final Map<String, Prefix6List> _prefix6Lists;

  private final Map<String, PrefixList> _prefixLists;

  private final Set<String> _referencedRouteMaps;

  private final Map<String, RouteMap> _routeMaps;

  private final Map<String, RoutePolicy> _routePolicies;

  private final Set<String> _snmpAccessLists;

  private SnmpServer _snmpServer;

  private String _snmpSourceInterface;

  private boolean _spanningTreePortfastDefault;

  private final Set<String> _sshAcls;

  private final Set<String> _sshIpv6Acls;

  private final Map<String, StandardAccessList> _standardAccessLists;

  private final Map<String, StandardCommunityList> _standardCommunityLists;

  private final Map<String, StandardIpv6AccessList> _standardIpv6AccessLists;

  private NavigableSet<String> _tacacsServers;

  private String _tacacsSourceInterface;

  private final SortedMap<String, Integer> _undefinedPeerGroups;

  private transient Set<String> _unimplementedFeatures;

  private transient Map<String, Integer> _unusedPeerGroups;

  private transient Map<String, Integer> _unusedPeerSessions;

  private ConfigurationFormat _vendor;

  private final Set<String> _verifyAccessLists;

  private final Map<String, Vrf> _vrfs;

  private final SortedMap<String, VrrpInterface> _vrrpGroups;

  private final Set<String> _wccpAcls;

  private final Map<String, ServiceObjectGroup> _serviceObjectGroups;

  private final Map<String, Map<String, SecurityZonePair>> _securityZonePairs;

  private final Map<String, SecurityZone> _securityZones;

  public CiscoConfiguration(Set<String> unimplementedFeatures) {
    _asPathAccessLists = new TreeMap<>();
    _asPathSets = new TreeMap<>();
    _bgpVrfAggregateAddressRouteMaps = new TreeSet<>();
    _cf = new CiscoFamily();
    _classMapAccessGroups = new TreeSet<>();
    _controlPlaneAccessGroups = new TreeSet<>();
    _cryptoAcls = new TreeSet<>();
    _dhcpRelayServers = new ArrayList<>();
    _dnsServers = new TreeSet<>();
    _expandedCommunityLists = new TreeMap<>();
    _extendedAccessLists = new TreeMap<>();
    _extendedIpv6AccessLists = new TreeMap<>();
    _failoverInterfaces = new TreeMap<>();
    _failoverPrimaryAddresses = new TreeMap<>();
    _failoverStandbyAddresses = new TreeMap<>();
    _igmpAcls = new TreeSet<>();
    _isakmpPolicies = new TreeMap<>();
    _isakmpProfiles = new TreeMap<>();
    _inspectClassMaps = new TreeMap<>();
    _inspectPolicyMaps = new TreeMap<>();
    _interfaces = new TreeMap<>();
    _ipNatDestinationAccessLists = new TreeSet<>();
    _ipPimNeighborFilters = new TreeSet<>();
    _ipsecTransformSets = new TreeMap<>();
    _ipsecProfiles = new TreeMap<>();
    _keyrings = new TreeMap<>();
    _lineAccessClassLists = new TreeSet<>();
    _lineIpv6AccessClassLists = new TreeSet<>();
    _macAccessLists = new TreeMap<>();
    _managementAccessGroups = new TreeSet<>();
    _msdpPeerSaLists = new TreeSet<>();
    _natPools = new TreeMap<>();
    _networkObjectGroups = new TreeMap<>();
    _ntpAccessGroups = new TreeSet<>();
    _nxBgpGlobalConfiguration = new CiscoNxBgpGlobalConfiguration();
    _pimAcls = new TreeSet<>();
    _pimRouteMaps = new TreeSet<>();
    _prefixLists = new TreeMap<>();
    _prefix6Lists = new TreeMap<>();
    _referencedRouteMaps = new TreeSet<>();
    _routeMaps = new TreeMap<>();
    _routePolicies = new TreeMap<>();
    _securityZonePairs = new TreeMap<>();
    _securityZones = new TreeMap<>();
    _serviceObjectGroups = new TreeMap<>();
    _snmpAccessLists = new TreeSet<>();
    _sshAcls = new TreeSet<>();
    _sshIpv6Acls = new TreeSet<>();
    _standardAccessLists = new TreeMap<>();
    _standardIpv6AccessLists = new TreeMap<>();
    _standardCommunityLists = new TreeMap<>();
    _tacacsServers = new TreeSet<>();
    _undefinedPeerGroups = new TreeMap<>();
    _unimplementedFeatures = unimplementedFeatures;
    _verifyAccessLists = new HashSet<>();
    _vrfs = new TreeMap<>();
    _vrfs.put(Configuration.DEFAULT_VRF_NAME, new Vrf(Configuration.DEFAULT_VRF_NAME));
    _vrrpGroups = new TreeMap<>();
    _wccpAcls = new TreeSet<>();
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
          } else {
            int line = vrrpInterface.getDefinitionLine();
            undefined(
                CiscoStructureType.INTERFACE,
                ifaceName,
                CiscoStructureUsage.ROUTER_VRRP_INTERFACE,
                line);
          }
        });
  }

  private WithEnvironmentExpr bgpRedistributeWithEnvironmentExpr(
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

  private void convertForPurpose(Set<RouteMap> routingRouteMaps, RouteMap map) {
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

  public Set<String> getBgpVrfAggregateAddressRouteMaps() {
    return _bgpVrfAggregateAddressRouteMaps;
  }

  public CiscoFamily getCf() {
    return _cf;
  }

  public Set<String> getClassMapAccessGroups() {
    return _classMapAccessGroups;
  }

  public Set<String> getControlPlaneAccessGroups() {
    return _controlPlaneAccessGroups;
  }

  public Set<String> getCryptoAcls() {
    return _cryptoAcls;
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

  public Set<String> getIgmpAcls() {
    return _igmpAcls;
  }

  private @Nullable Interface getInterfaceByTunnelAddresses(Ip sourceAddress, Prefix destPrefix) {
    for (Interface iface : _interfaces.values()) {
      Tunnel tunnel = iface.getTunnel();
      if (tunnel != null
          && tunnel.getSource() != null
          && tunnel.getSource().equals(sourceAddress)
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

  public Set<String> getIpNatDestinationAccessLists() {
    return _ipNatDestinationAccessLists;
  }

  public Set<String> getIpPimNeighborFilters() {
    return _ipPimNeighborFilters;
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

  public Set<String> getLineAccessClassLists() {
    return _lineAccessClassLists;
  }

  public Set<String> getLineIpv6AccessClassLists() {
    return _lineIpv6AccessClassLists;
  }

  public Map<String, MacAccessList> getMacAccessLists() {
    return _macAccessLists;
  }

  public Set<String> getManagementAccessGroups() {
    return _managementAccessGroups;
  }

  public Set<String> getMsdpPeerSaLists() {
    return _msdpPeerSaLists;
  }

  public Map<String, NatPool> getNatPools() {
    return _natPools;
  }

  public Set<String> getNtpAccessGroups() {
    return _ntpAccessGroups;
  }

  public String getNtpSourceInterface() {
    return _ntpSourceInterface;
  }

  public CiscoNxBgpGlobalConfiguration getNxBgpGlobalConfiguration() {
    return _nxBgpGlobalConfiguration;
  }

  public Set<String> getPimAcls() {
    return _pimAcls;
  }

  public Set<String> getPimRouteMaps() {
    return _pimRouteMaps;
  }

  public Map<String, Prefix6List> getPrefix6Lists() {
    return _prefix6Lists;
  }

  public Map<String, PrefixList> getPrefixLists() {
    return _prefixLists;
  }

  public Set<String> getReferencedRouteMaps() {
    return _referencedRouteMaps;
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

  public Set<String> getSnmpAccessLists() {
    return _snmpAccessLists;
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

  public Set<String> getSshAcls() {
    return _sshAcls;
  }

  public Set<String> getSshIpv6Acls() {
    return _sshIpv6Acls;
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
        } else {
          int updateSourceInterfaceLine = lpg.getUpdateSourceLine();
          undefined(
              CiscoStructureType.INTERFACE,
              updateSourceInterface,
              CiscoStructureUsage.BGP_UPDATE_SOURCE_INTERFACE,
              updateSourceInterfaceLine);
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

  public Set<String> getVerifyAccessLists() {
    return _verifyAccessLists;
  }

  public Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  public SortedMap<String, VrrpInterface> getVrrpGroups() {
    return _vrrpGroups;
  }

  public Set<String> getWccpAcls() {
    return _wccpAcls;
  }

  private void markAcls(CiscoStructureUsage usage) {
    markStructure(
        CiscoStructureType.IP_ACCESS_LIST,
        usage,
        Arrays.asList(
            _extendedAccessLists,
            _standardAccessLists,
            _extendedIpv6AccessLists,
            _standardIpv6AccessLists));
  }

  private void markDepiClasses(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.DEPI_CLASS, usage, _cf.getDepiClasses());
  }

  private void markDepiTunnels(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.DEPI_TUNNEL, usage, _cf.getDepiTunnels());
  }

  private void markDocsisPolicies(CiscoStructureUsage usage) {
    Cable cable = _cf.getCable();
    markStructure(
        CiscoStructureType.DOCSIS_POLICY, usage, cable != null ? cable.getDocsisPolicies() : null);
  }

  private void markDocsisPolicyRules(CiscoStructureUsage usage) {
    Cable cable = _cf.getCable();
    markStructure(
        CiscoStructureType.DOCSIS_POLICY_RULE,
        usage,
        cable != null ? cable.getDocsisPolicyRules() : null);
  }

  private void markInspectClassMaps(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.INSPECT_CLASS_MAP, usage, _inspectClassMaps);
  }

  private void markInspectPolicyMaps(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.INSPECT_POLICY_MAP, usage, _inspectPolicyMaps);
  }

  private void markIpOrMacAcls(CiscoStructureUsage usage) {
    markStructure(
        CiscoStructureType.ACCESS_LIST,
        usage,
        Arrays.asList(
            _extendedAccessLists,
            _standardAccessLists,
            _extendedIpv6AccessLists,
            _macAccessLists,
            _standardIpv6AccessLists));
  }

  private void markIpsecProfiles(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.IPSEC_PROFILE, usage, _ipsecProfiles);
  }

  private void markIpsecTransformSets(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.IPSEC_TRANSFORM_SET, usage, _ipsecTransformSets);
  }

  private void markIpv4Acls(CiscoStructureUsage usage) {
    markStructure(
        CiscoStructureType.IPV4_ACCESS_LIST,
        usage,
        Arrays.asList(_extendedAccessLists, _standardAccessLists));
  }

  private void markIpv6Acls(CiscoStructureUsage usage) {
    markStructure(
        CiscoStructureType.IPV6_ACCESS_LIST,
        usage,
        Arrays.asList(_extendedIpv6AccessLists, _standardIpv6AccessLists));
  }

  private void markKeyrings(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.KEYRING, usage, _keyrings);
  }

  private void markL2tpClasses(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.L2TP_CLASS, usage, _cf.getL2tpClasses());
  }

  private void markNetworkObjectGroups(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.NETWORK_OBJECT_GROUP, usage, _networkObjectGroups);
  }

  private void markRouteMaps(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.ROUTE_MAP, usage, _routeMaps);
  }

  private void markSecurityZones(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.SECURITY_ZONE, usage, _securityZones);
  }

  private void markServiceClasses(CiscoStructureUsage usage) {
    Cable cable = _cf.getCable();
    markStructure(
        CiscoStructureType.SERVICE_CLASS, usage, cable != null ? cable.getServiceClasses() : null);
  }

  private void markServiceObjectGroups(CiscoStructureUsage usage) {
    markStructure(CiscoStructureType.SERVICE_OBJECT_GROUP, usage, _serviceObjectGroups);
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

  private AsPathAccessList toAsPathAccessList(AsPathSet asPathSet) {
    String name = asPathSet.getName();
    AsPathAccessList list = new AsPathAccessList(name);
    for (AsPathSetElem elem : asPathSet.getElements()) {
      AsPathAccessListLine line = toAsPathAccessListLine(elem);
      list.getLines().add(line);
    }
    return list;
  }

  private AsPathAccessList toAsPathAccessList(IpAsPathAccessList pathList) {
    String name = pathList.getName();
    AsPathAccessList newList = new AsPathAccessList(name);
    for (IpAsPathAccessListLine fromLine : pathList.getLines()) {
      fromLine.applyTo(newList);
    }
    return newList;
  }

  private AsPathAccessListLine toAsPathAccessListLine(AsPathSetElem elem) {
    String rawRegex = elem.regex();
    String regex = toJavaRegex(rawRegex);
    AsPathAccessListLine line = new AsPathAccessListLine();
    line.setAction(LineAction.ACCEPT);
    line.setRegex(regex);
    return line;
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

    CiscoNxBgpVrfAddressFamilyConfiguration ipv4af = nxBgpVrf.getIpv4UnicastAddressFamily();
    if (ipv4af != null) {
      newBgpProcess.setMultipathEbgp(ipv4af.getMaximumPathsEbgp() > 1);
      newBgpProcess.setMultipathIbgp(ipv4af.getMaximumPathsIbgp() > 1);
    }

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
    Integer maximumPaths = proc.getMaximumPaths();
    Integer maximumPathsEbgp = proc.getMaximumPathsEbgp();
    Integer maximumPathsIbgp = proc.getMaximumPathsIbgp();
    boolean multipathEbgp = false;
    boolean multipathIbgp = false;
    if (maximumPaths != null && maximumPaths > 1) {
      multipathEbgp = true;
      multipathIbgp = true;
    }
    if (maximumPathsEbgp != null && maximumPathsEbgp > 1) {
      multipathEbgp = true;
    }
    if (maximumPathsIbgp != null && maximumPathsIbgp > 1) {
      multipathIbgp = true;
    }
    newBgpProcess.setMultipathEbgp(multipathEbgp);
    newBgpProcess.setMultipathIbgp(multipathIbgp);
    Map<Prefix, BgpNeighbor> newBgpNeighbors = newBgpProcess.getNeighbors();
    int defaultMetric = proc.getDefaultMetric();
    Ip bgpRouterId = getBgpRouterId(c, vrfName, proc);
    MatchPrefixSet matchDefaultRoute =
        new MatchPrefixSet(
            new DestinationNetwork(),
            new ExplicitPrefixSet(
                new PrefixSpace(
                    Collections.singleton(new PrefixRange(Prefix.ZERO, new SubRange(0, 0))))));
    matchDefaultRoute.setComment("match default route");
    MatchPrefix6Set matchDefaultRoute6 =
        new MatchPrefix6Set(
            new DestinationNetwork6(),
            new ExplicitPrefix6Set(
                new Prefix6Space(
                    Collections.singleton(new Prefix6Range(Prefix6.ZERO, new SubRange(0, 0))))));
    matchDefaultRoute.setComment("match default route");
    newBgpProcess.setRouterId(bgpRouterId);
    Set<BgpAggregateIpv4Network> summaryOnlyNetworks = new HashSet<>();
    Set<BgpAggregateIpv6Network> summaryOnlyIpv6Networks = new HashSet<>();

    List<BooleanExpr> attributeMapPrefilters = new ArrayList<>();

    // add generated routes for aggregate ipv4 addresses
    for (Entry<Prefix, BgpAggregateIpv4Network> e : proc.getAggregateNetworks().entrySet()) {
      Prefix prefix = e.getKey();
      BgpAggregateIpv4Network aggNet = e.getValue();
      boolean summaryOnly = aggNet.getSummaryOnly();
      int prefixLength = prefix.getPrefixLength();
      SubRange prefixRange = new SubRange(prefixLength + 1, Prefix.MAX_PREFIX_LENGTH);
      if (summaryOnly) {
        summaryOnlyNetworks.add(aggNet);
      }

      // create generation policy for aggregate network
      String generationPolicyName = "~AGGREGATE_ROUTE_GEN:" + vrfName + ":" + prefix + "~";
      RoutingPolicy currentGeneratedRoutePolicy = new RoutingPolicy(generationPolicyName, c);
      If currentGeneratedRouteConditional = new If();
      currentGeneratedRoutePolicy.getStatements().add(currentGeneratedRouteConditional);
      currentGeneratedRouteConditional.setGuard(
          new MatchPrefixSet(
              new DestinationNetwork(),
              new ExplicitPrefixSet(
                  new PrefixSpace(Collections.singleton(new PrefixRange(prefix, prefixRange))))));
      currentGeneratedRouteConditional
          .getTrueStatements()
          .add(Statements.ReturnTrue.toStaticStatement());
      c.getRoutingPolicies().put(generationPolicyName, currentGeneratedRoutePolicy);
      GeneratedRoute.Builder gr = new GeneratedRoute.Builder();
      gr.setNetwork(prefix);
      gr.setAdmin(CISCO_AGGREGATE_ROUTE_ADMIN_COST);
      gr.setGenerationPolicy(generationPolicyName);
      gr.setDiscard(true);

      // set attribute map for aggregate network
      String attributeMapName = aggNet.getAttributeMap();
      Conjunction applyCurrentAggregateAttributesConditions = new Conjunction();
      applyCurrentAggregateAttributesConditions
          .getConjuncts()
          .add(
              new MatchPrefixSet(
                  new DestinationNetwork(),
                  new ExplicitPrefixSet(
                      new PrefixSpace(Collections.singleton(PrefixRange.fromPrefix(prefix))))));
      applyCurrentAggregateAttributesConditions
          .getConjuncts()
          .add(new MatchProtocol(RoutingProtocol.AGGREGATE));
      BooleanExpr weInterior = BooleanExprs.True.toStaticBooleanExpr();
      if (attributeMapName != null) {
        int attributeMapLine = aggNet.getAttributeMapLine();
        RouteMap attributeMap = _routeMaps.get(attributeMapName);
        if (attributeMap != null) {
          // need to apply attribute changes if this specific route is
          // matched
          weInterior = new CallExpr(attributeMapName);
          attributeMap.getReferers().put(aggNet, "attribute-map of aggregate route: " + prefix);
          gr.setAttributePolicy(attributeMapName);
        } else {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              attributeMapName,
              CiscoStructureUsage.BGP_AGGREGATE_ATTRIBUTE_MAP,
              attributeMapLine);
        }
      }
      v.getGeneratedRoutes().add(gr.build());
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.IGP);
      applyCurrentAggregateAttributesConditions.getConjuncts().add(we);
      attributeMapPrefilters.add(applyCurrentAggregateAttributesConditions);
    }

    // add generated routes for aggregate ipv6 addresses
    // TODO: merge with above to make cleaner
    for (Entry<Prefix6, BgpAggregateIpv6Network> e : proc.getAggregateIpv6Networks().entrySet()) {
      Prefix6 prefix6 = e.getKey();
      BgpAggregateIpv6Network aggNet = e.getValue();
      boolean summaryOnly = aggNet.getSummaryOnly();
      int prefixLength = prefix6.getPrefixLength();
      SubRange prefixRange = new SubRange(prefixLength + 1, Prefix6.MAX_PREFIX_LENGTH);
      if (summaryOnly) {
        summaryOnlyIpv6Networks.add(aggNet);
      }

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
        int attributeMapLine = aggNet.getAttributeMapLine();
        RouteMap attributeMap = _routeMaps.get(attributeMapName);
        if (attributeMap != null) {
          attributeMap
              .getReferers()
              .put(aggNet, "attribute-map of aggregate ipv6 route: " + prefix6);
          gr.setAttributePolicy(attributeMapName);
        } else {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              attributeMapName,
              CiscoStructureUsage.BGP_AGGREGATE_ATTRIBUTE_MAP,
              attributeMapLine);
        }
      }
    }

    /*
     * Create common bgp export policy. This policy encompasses network
     * statements, aggregate-address with/without summary-only, redistribution
     * from other protocols, and default-origination
     */
    String bgpCommonExportPolicyName = "~BGP_COMMON_EXPORT_POLICY:" + vrfName + "~";
    RoutingPolicy bgpCommonExportPolicy = new RoutingPolicy(bgpCommonExportPolicyName, c);
    c.getRoutingPolicies().put(bgpCommonExportPolicyName, bgpCommonExportPolicy);
    List<Statement> bgpCommonExportStatements = bgpCommonExportPolicy.getStatements();

    // create policy for denying suppressed summary-only networks
    if (summaryOnlyNetworks.size() > 0) {
      If suppressSummaryOnly = new If();
      bgpCommonExportStatements.add(suppressSummaryOnly);
      suppressSummaryOnly.setComment(
          "Suppress summarized of summary-only aggregate-address networks");
      String matchSuppressedSummaryOnlyRoutesName =
          "~MATCH_SUPPRESSED_SUMMARY_ONLY:" + vrfName + "~";
      RouteFilterList matchSuppressedSummaryOnlyRoutes =
          new RouteFilterList(matchSuppressedSummaryOnlyRoutesName);
      c.getRouteFilterLists()
          .put(matchSuppressedSummaryOnlyRoutesName, matchSuppressedSummaryOnlyRoutes);
      for (BgpAggregateIpv4Network summaryOnlyNetwork : summaryOnlyNetworks) {
        Prefix prefix = summaryOnlyNetwork.getPrefix();
        int prefixLength = prefix.getPrefixLength();
        RouteFilterLine line =
            new RouteFilterLine(
                LineAction.ACCEPT,
                prefix,
                new SubRange(prefixLength + 1, Prefix.MAX_PREFIX_LENGTH));
        matchSuppressedSummaryOnlyRoutes.addLine(line);
      }
      suppressSummaryOnly.setGuard(
          new MatchPrefixSet(
              new DestinationNetwork(), new NamedPrefixSet(matchSuppressedSummaryOnlyRoutesName)));
      suppressSummaryOnly.getTrueStatements().add(Statements.ReturnFalse.toStaticStatement());
    }

    If preFilter = new If();
    bgpCommonExportStatements.add(preFilter);
    bgpCommonExportStatements.add(Statements.ReturnFalse.toStaticStatement());
    Disjunction preFilterConditions = new Disjunction();
    preFilter.setGuard(preFilterConditions);
    preFilter.getTrueStatements().add(Statements.ReturnTrue.toStaticStatement());

    preFilterConditions.getDisjuncts().addAll(attributeMapPrefilters);

    // create redistribution origination policies
    // redistribute rip
    BgpRedistributionPolicy redistributeRipPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.RIP);
    if (redistributeRipPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.True.toStaticBooleanExpr();
      Conjunction exportRipConditions = new Conjunction();
      exportRipConditions.setComment("Redistribute RIP routes into BGP");
      exportRipConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.RIP));
      String mapName = redistributeRipPolicy.getRouteMap();
      if (mapName != null) {
        int mapLine = redistributeRipPolicy.getRouteMapLine();
        RouteMap redistributeRipRouteMap = _routeMaps.get(mapName);
        if (redistributeRipRouteMap != null) {
          redistributeRipRouteMap.getReferers().put(proc, "RIP redistribution route-map");
          weInterior = new CallExpr(mapName);
        } else {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              mapName,
              CiscoStructureUsage.BGP_REDISTRIBUTE_RIP_MAP,
              mapLine);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportRipConditions.getConjuncts().add(we);
      preFilterConditions.getDisjuncts().add(exportRipConditions);
    }

    // redistribute static
    BgpRedistributionPolicy redistributeStaticPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.STATIC);
    if (redistributeStaticPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.True.toStaticBooleanExpr();
      Conjunction exportStaticConditions = new Conjunction();
      exportStaticConditions.setComment("Redistribute static routes into BGP");
      exportStaticConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.STATIC));
      String mapName = redistributeStaticPolicy.getRouteMap();
      if (mapName != null) {
        int mapLine = redistributeStaticPolicy.getRouteMapLine();
        RouteMap redistributeStaticRouteMap = _routeMaps.get(mapName);
        if (redistributeStaticRouteMap != null) {
          redistributeStaticRouteMap.getReferers().put(proc, "static redistribution route-map");
          weInterior = new CallExpr(mapName);
        } else {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              mapName,
              CiscoStructureUsage.BGP_REDISTRIBUTE_STATIC_MAP,
              mapLine);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportStaticConditions.getConjuncts().add(we);
      preFilterConditions.getDisjuncts().add(exportStaticConditions);
    }

    // redistribute connected
    BgpRedistributionPolicy redistributeConnectedPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.CONNECTED);
    if (redistributeConnectedPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.True.toStaticBooleanExpr();
      Conjunction exportConnectedConditions = new Conjunction();
      exportConnectedConditions.setComment("Redistribute connected routes into BGP");
      exportConnectedConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.CONNECTED));
      String mapName = redistributeConnectedPolicy.getRouteMap();
      if (mapName != null) {
        int mapLine = redistributeConnectedPolicy.getRouteMapLine();
        RouteMap redistributeConnectedRouteMap = _routeMaps.get(mapName);
        if (redistributeConnectedRouteMap != null) {
          redistributeConnectedRouteMap
              .getReferers()
              .put(proc, "connected redistribution route-map");
          weInterior = new CallExpr(mapName);
        } else {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              mapName,
              CiscoStructureUsage.BGP_REDISTRIBUTE_CONNECTED_MAP,
              mapLine);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportConnectedConditions.getConjuncts().add(we);
      preFilterConditions.getDisjuncts().add(exportConnectedConditions);
    }

    // redistribute ospf
    BgpRedistributionPolicy redistributeOspfPolicy =
        proc.getRedistributionPolicies().get(RoutingProtocol.OSPF);
    if (redistributeOspfPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.True.toStaticBooleanExpr();
      Conjunction exportOspfConditions = new Conjunction();
      exportOspfConditions.setComment("Redistribute OSPF routes into BGP");
      exportOspfConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.OSPF));
      String mapName = redistributeOspfPolicy.getRouteMap();
      if (mapName != null) {
        int mapLine = redistributeOspfPolicy.getRouteMapLine();
        RouteMap redistributeOspfRouteMap = _routeMaps.get(mapName);
        if (redistributeOspfRouteMap != null) {
          redistributeOspfRouteMap.getReferers().put(proc, "ospf redistribution route-map");
          weInterior = new CallExpr(mapName);
        } else {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              mapName,
              CiscoStructureUsage.BGP_REDISTRIBUTE_OSPF_MAP,
              mapLine);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportOspfConditions.getConjuncts().add(we);
      preFilterConditions.getDisjuncts().add(exportOspfConditions);
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
    _unusedPeerGroups = new TreeMap<>();
    int fakePeerCounter = -1;
    // peer groups / peer templates
    for (Entry<String, NamedBgpPeerGroup> e : proc.getNamedPeerGroups().entrySet()) {
      String name = e.getKey();
      NamedBgpPeerGroup namedPeerGroup = e.getValue();
      if (!namedPeerGroup.getInherited()) {
        _unusedPeerGroups.put(name, namedPeerGroup.getDefinitionLine());
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
    _unusedPeerSessions = new TreeMap<>();
    int fakeGroupCounter = 1;
    for (NamedBgpPeerGroup namedPeerGroup : proc.getPeerSessions().values()) {
      namedPeerGroup.getParentSession(proc, this).inheritUnsetFields(proc, this);
    }
    for (Entry<String, NamedBgpPeerGroup> e : proc.getPeerSessions().entrySet()) {
      String name = e.getKey();
      NamedBgpPeerGroup namedPeerGroup = e.getValue();
      if (!namedPeerGroup.getInherited()) {
        _unusedPeerSessions.put(name, namedPeerGroup.getDefinitionLine());
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
              BooleanExpr weExpr = BooleanExprs.True.toStaticBooleanExpr();
              if (mapName != null) {
                int mapLine = bgpNetwork.getRouteMapLine();
                RouteMap routeMap = _routeMaps.get(mapName);
                if (routeMap != null) {
                  weExpr = new CallExpr(mapName);
                  routeMap.getReferers().put(proc, "bgp ipv4 advertised network route-map");
                } else {
                  undefined(
                      CiscoStructureType.ROUTE_MAP,
                      mapName,
                      CiscoStructureUsage.BGP_NETWORK_ORIGINATION_ROUTE_MAP,
                      mapLine);
                }
              }
              BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weExpr, OriginType.IGP);
              Conjunction exportNetworkConditions = new Conjunction();
              PrefixSpace space = new PrefixSpace();
              space.addPrefix(prefix);
              exportNetworkConditions
                  .getConjuncts()
                  .add(new MatchPrefixSet(new DestinationNetwork(), new ExplicitPrefixSet(space)));
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.BGP)));
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.IBGP)));
              // TODO: ban aggregates?
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)));
              exportNetworkConditions.getConjuncts().add(we);
              preFilterConditions.getDisjuncts().add(exportNetworkConditions);
            });
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
                int mapLine = bgpNetwork6.getRouteMapLine();
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
                  // TODO: ban aggregates?
                  exportNetwork6Conditions
                      .getConjuncts()
                      .add(new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)));
                  exportNetwork6Conditions.getConjuncts().add(we);
                  preFilterConditions.getDisjuncts().add(exportNetwork6Conditions);
                } else {
                  undefined(
                      CiscoStructureType.ROUTE_MAP,
                      mapName,
                      CiscoStructureUsage.BGP_NETWORK6_ORIGINATION_ROUTE_MAP,
                      mapLine);
                }
              }
            });
    c.getRoute6FilterLists().put(localFilter6Name, localFilter6);

    MatchProtocol isEbgp = new MatchProtocol(RoutingProtocol.BGP);
    MatchProtocol isIbgp = new MatchProtocol(RoutingProtocol.IBGP);
    preFilterConditions.getDisjuncts().add(isEbgp);
    preFilterConditions.getDisjuncts().add(isIbgp);

    for (LeafBgpPeerGroup lpg : leafGroups) {
      // update source
      String updateSourceInterface = lpg.getUpdateSource();
      boolean ipv4 = lpg.getNeighborPrefix() != null;
      Ip updateSource = getUpdateSource(c, vrfName, lpg, updateSourceInterface, ipv4);
      RoutingPolicy importPolicy = null;
      String inboundRouteMapName = lpg.getInboundRouteMap();
      if (inboundRouteMapName != null) {
        int inboundRouteMapLine = lpg.getInboundRouteMapLine();
        importPolicy = c.getRoutingPolicies().get(inboundRouteMapName);
        if (importPolicy == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              inboundRouteMapName,
              CiscoStructureUsage.BGP_INBOUND_ROUTE_MAP,
              inboundRouteMapLine);
        } else {
          RouteMap inboundRouteMap = _routeMaps.get(inboundRouteMapName);
          inboundRouteMap
              .getReferers()
              .put(lpg, "inbound route-map for leaf peer-group: " + lpg.getName());
        }
      }
      String inboundRoute6MapName = lpg.getInboundRoute6Map();
      RoutingPolicy importPolicy6 = null;
      if (inboundRoute6MapName != null) {
        int inboundRoute6MapLine = lpg.getInboundRoute6MapLine();
        importPolicy6 = c.getRoutingPolicies().get(inboundRoute6MapName);
        if (importPolicy6 == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              inboundRoute6MapName,
              CiscoStructureUsage.BGP_INBOUND_ROUTE6_MAP,
              inboundRoute6MapLine);
        } else {
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
      If peerExportConditional = new If();
      peerExportConditional.setComment(
          "peer-export policy main conditional: exitAccept if true / exitReject if false");
      peerExportPolicy.getStatements().add(peerExportConditional);
      Conjunction peerExportConditions = new Conjunction();
      peerExportConditional.setGuard(peerExportConditions);
      peerExportConditional.getTrueStatements().add(Statements.ExitAccept.toStaticStatement());
      peerExportConditional.getFalseStatements().add(Statements.ExitReject.toStaticStatement());
      Disjunction localOrCommonOrigination = new Disjunction();
      peerExportConditions.getConjuncts().add(localOrCommonOrigination);
      localOrCommonOrigination.getDisjuncts().add(new CallExpr(bgpCommonExportPolicyName));
      String outboundRouteMapName = lpg.getOutboundRouteMap();
      if (outboundRouteMapName != null) {
        int outboundRouteMapLine = lpg.getOutboundRouteMapLine();
        RouteMap outboundRouteMap = _routeMaps.get(outboundRouteMapName);
        if (outboundRouteMap == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              outboundRouteMapName,
              CiscoStructureUsage.BGP_OUTBOUND_ROUTE_MAP,
              outboundRouteMapLine);
        } else {
          outboundRouteMap
              .getReferers()
              .put(lpg, "outbound route-map for leaf peer-group: " + lpg.getName());
          peerExportConditions.getConjuncts().add(new CallExpr(outboundRouteMapName));
        }
      }
      String outboundRoute6MapName = lpg.getOutboundRoute6Map();
      if (outboundRoute6MapName != null) {
        int outboundRoute6MapLine = lpg.getOutboundRoute6MapLine();
        RouteMap outboundRoute6Map = _routeMaps.get(outboundRoute6MapName);
        if (outboundRoute6Map == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              outboundRoute6MapName,
              CiscoStructureUsage.BGP_OUTBOUND_ROUTE6_MAP,
              outboundRoute6MapLine);
        } else {
          outboundRoute6Map
              .getReferers()
              .put(lpg, "outbound ipv6 route-map for leaf peer-group: " + lpg.getName());
        }
      }

      // set up default export policy for this peer group
      GeneratedRoute.Builder defaultRoute = null;
      GeneratedRoute6.Builder defaultRoute6 = null;
      if (lpg.getDefaultOriginate()) {
        if (ipv4) {
          localOrCommonOrigination.getDisjuncts().add(matchDefaultRoute);
        } else {
          localOrCommonOrigination.getDisjuncts().add(matchDefaultRoute6);
        }
        defaultRoute = new GeneratedRoute.Builder();
        defaultRoute.setNetwork(Prefix.ZERO);
        defaultRoute.setAdmin(MAX_ADMINISTRATIVE_COST);
        defaultRoute6 = new GeneratedRoute6.Builder();
        defaultRoute6.setNetwork(Prefix6.ZERO);
        defaultRoute6.setAdmin(MAX_ADMINISTRATIVE_COST);

        String defaultOriginateMapName = lpg.getDefaultOriginateMap();
        if (defaultOriginateMapName != null) { // originate contingent on
          // generation policy
          int defaultOriginateMapLine = lpg.getDefaultOriginateMapLine();
          RoutingPolicy defaultRouteGenerationPolicy =
              c.getRoutingPolicies().get(defaultOriginateMapName);
          if (defaultRouteGenerationPolicy == null) {
            undefined(
                CiscoStructureType.ROUTE_MAP,
                defaultOriginateMapName,
                CiscoStructureUsage.BGP_DEFAULT_ORIGINATE_ROUTE_MAP,
                defaultOriginateMapLine);
          } else {
            RouteMap defaultRouteGenerationRouteMap = _routeMaps.get(defaultOriginateMapName);
            defaultRouteGenerationRouteMap
                .getReferers()
                .put(lpg, "default route generation policy for leaf peer-group: " + lpg.getName());
            defaultRoute.setGenerationPolicy(defaultOriginateMapName);
          }
        } else {
          String defaultRouteGenerationPolicyName =
              "~BGP_DEFAULT_ROUTE_GENERATION_POLICY:" + vrfName + ":" + lpg.getName() + "~";
          RoutingPolicy defaultRouteGenerationPolicy =
              new RoutingPolicy(defaultRouteGenerationPolicyName, c);
          If defaultRouteGenerationConditional = new If();
          defaultRouteGenerationPolicy.getStatements().add(defaultRouteGenerationConditional);
          if (ipv4) {
            defaultRouteGenerationConditional.setGuard(matchDefaultRoute);
          } else {
            defaultRouteGenerationConditional.setGuard(matchDefaultRoute6);
          }
          defaultRouteGenerationConditional
              .getTrueStatements()
              .add(Statements.ReturnTrue.toStaticStatement());
          if (lpg.getActive() && !lpg.getShutdown()) {
            c.getRoutingPolicies()
                .put(defaultRouteGenerationPolicyName, defaultRouteGenerationPolicy);
          }
          if (ipv4) {
            defaultRoute.setGenerationPolicy(defaultRouteGenerationPolicyName);
          } else {
            defaultRoute6.setGenerationPolicy(defaultRouteGenerationPolicyName);
          }
        }
      }

      Ip clusterId = lpg.getClusterId();
      if (clusterId == null) {
        clusterId = bgpRouterId;
      }
      boolean routeReflectorClient = lpg.getRouteReflectorClient();
      boolean sendCommunity = lpg.getSendCommunity();
      boolean additionalPathsReceive = lpg.getAdditionalPathsReceive();
      boolean additionalPathsSelectAll = lpg.getAdditionalPathsSelectAll();
      boolean additionalPathsSend = lpg.getAdditionalPathsSend();
      boolean advertiseInactive = lpg.getAdvertiseInactive();
      boolean ebgpMultihop = lpg.getEbgpMultihop();
      boolean allowasIn = lpg.getAllowAsIn();
      boolean disablePeerAsCheck = lpg.getDisablePeerAsCheck();
      String inboundPrefixListName = lpg.getInboundPrefixList();
      if (inboundPrefixListName != null) {
        int inboundPrefixListLine = lpg.getInboundPrefixListLine();
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
        } else {
          if (ipv4) {
            undefined(
                CiscoStructureType.PREFIX_LIST,
                inboundPrefixListName,
                CiscoStructureUsage.BGP_INBOUND_PREFIX_LIST,
                inboundPrefixListLine);
          } else {
            undefined(
                CiscoStructureType.PREFIX6_LIST,
                inboundPrefixListName,
                CiscoStructureUsage.BGP_INBOUND_PREFIX6_LIST,
                inboundPrefixListLine);
          }
        }
      }
      String outboundPrefixListName = lpg.getOutboundPrefixList();
      if (outboundPrefixListName != null) {
        int outboundPrefixListLine = lpg.getOutboundPrefixListLine();
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
        } else {
          if (ipv4) {
            undefined(
                CiscoStructureType.PREFIX_LIST,
                outboundPrefixListName,
                CiscoStructureUsage.BGP_OUTBOUND_PREFIX_LIST,
                outboundPrefixListLine);
          } else {
            undefined(
                CiscoStructureType.PREFIX6_LIST,
                outboundPrefixListName,
                CiscoStructureUsage.BGP_OUTBOUND_PREFIX6_LIST,
                outboundPrefixListLine);
          }
        }
      }
      String description = lpg.getDescription();
      if (lpg.getActive() && !lpg.getShutdown()) {
        if (lpg.getRemoteAs() == null) {
          _w.redFlag("No remote-as set for peer: " + lpg.getName());
          continue;
        }
        Integer pgLocalAs = lpg.getLocalAs();
        int localAs = pgLocalAs != null ? pgLocalAs : proc.getName();
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

        newNeighbor.setAdditionalPathsReceive(additionalPathsReceive);
        newNeighbor.setAdditionalPathsSelectAll(additionalPathsSelectAll);
        newNeighbor.setAdditionalPathsSend(additionalPathsSend);
        newNeighbor.setAdvertiseInactive(advertiseInactive);
        newNeighbor.setAllowLocalAsIn(allowasIn);
        newNeighbor.setAllowRemoteAsOut(disablePeerAsCheck);
        newNeighbor.setRouteReflectorClient(routeReflectorClient);
        newNeighbor.setClusterId(clusterId.asLong());
        newNeighbor.setDefaultMetric(defaultMetric);
        newNeighbor.setDescription(description);
        newNeighbor.setEbgpMultihop(ebgpMultihop);
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
        newNeighbor.setSendCommunity(sendCommunity);
        newNeighbor.setVrf(vrfName);
      }
    }
    return newBgpProcess;
  }

  private CommunityList toCommunityList(ExpandedCommunityList ecList) {
    List<CommunityListLine> cllList = new ArrayList<>();
    for (ExpandedCommunityListLine ecll : ecList.getLines()) {
      cllList.add(toCommunityListLine(ecll));
    }
    CommunityList cList = new CommunityList(ecList.getName(), cllList);
    return cList;
  }

  private CommunityListLine toCommunityListLine(ExpandedCommunityListLine eclLine) {
    String regex = eclLine.getRegex();
    String javaRegex = toJavaRegex(regex);
    return new CommunityListLine(eclLine.getAction(), javaRegex);
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
    int sourceNatAclLine = nat.getAclNameLine();
    if (sourceNatAcl == null) {
      undefined(
          CiscoStructureType.IP_ACCESS_LIST,
          sourceNatAclName,
          CiscoStructureUsage.IP_NAT_SOURCE_ACCESS_LIST,
          sourceNatAclLine);
    } else {
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
      int sourceNatPoolLine = nat.getNatPoolLine();
      NatPool sourceNatPool = _natPools.get(sourceNatPoolName);
      if (sourceNatPool != null) {
        sourceNatPool.getReferers().put(iface, "source nat pool for interface: " + iface.getName());
        Ip firstIp = sourceNatPool.getFirst();
        if (firstIp != null) {
          Ip lastIp = sourceNatPool.getLast();
          convertedNat.setPoolIpFirst(firstIp);
          convertedNat.setPoolIpLast(lastIp);
        }
      } else {
        undefined(
            CiscoStructureType.NAT_POOL,
            sourceNatPoolName,
            CiscoStructureUsage.IP_NAT_SOURCE_POOL,
            sourceNatPoolLine);
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
    }
    if (level1) {
      newIface.setIsisL1InterfaceMode(iface.getIsisInterfaceMode());
    } else {
      newIface.setIsisL1InterfaceMode(IsisInterfaceMode.UNSET);
    }
    if (level2) {
      newIface.setIsisL2InterfaceMode(iface.getIsisInterfaceMode());
    } else {
      newIface.setIsisL2InterfaceMode(IsisInterfaceMode.UNSET);
    }
    newIface.setIsisCost(iface.getIsisCost());
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
      int incomingFilterLine = iface.getIncomingFilterLine();
      IpAccessList incomingFilter = ipAccessLists.get(incomingFilterName);
      if (incomingFilter == null) {
        undefined(
            CiscoStructureType.IP_ACCESS_LIST,
            incomingFilterName,
            CiscoStructureUsage.INTERFACE_INCOMING_FILTER,
            incomingFilterLine);
      } else {
        String msg = "incoming acl for interface: " + iface.getName();
        ExtendedAccessList incomingExtendedAccessList =
            _extendedAccessLists.get(incomingFilterName);
        if (incomingExtendedAccessList != null) {
          incomingExtendedAccessList.getReferers().put(iface, msg);
        }
        StandardAccessList incomingStandardAccessList =
            _standardAccessLists.get(incomingFilterName);
        if (incomingStandardAccessList != null) {
          incomingStandardAccessList.getReferers().put(iface, msg);
        }
      }
      newIface.setIncomingFilter(incomingFilter);
    }
    String outgoingFilterName = iface.getOutgoingFilter();
    if (outgoingFilterName != null) {
      int outgoingFilterLine = iface.getOutgoingFilterLine();
      IpAccessList outgoingFilter = ipAccessLists.get(outgoingFilterName);
      if (outgoingFilter == null) {
        undefined(
            CiscoStructureType.IP_ACCESS_LIST,
            outgoingFilterName,
            CiscoStructureUsage.INTERFACE_OUTGOING_FILTER,
            outgoingFilterLine);
      } else {
        String msg = "outgoing acl for interface: " + iface.getName();
        ExtendedAccessList outgoingExtendedAccessList =
            _extendedAccessLists.get(outgoingFilterName);
        if (outgoingExtendedAccessList != null) {
          outgoingExtendedAccessList.getReferers().put(iface, msg);
        }
        StandardAccessList outgoingStandardAccessList =
            _standardAccessLists.get(outgoingFilterName);
        if (outgoingStandardAccessList != null) {
          outgoingStandardAccessList.getReferers().put(iface, msg);
        }
      }
      newIface.setOutgoingFilter(outgoingFilter);
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
      int routingPolicyLine = iface.getRoutingPolicyLine();
      RouteMap routingPolicyRouteMap = _routeMaps.get(routingPolicyName);
      if (routingPolicyRouteMap == null) {
        undefined(
            CiscoStructureType.ROUTE_MAP,
            routingPolicyName,
            CiscoStructureUsage.INTERFACE_POLICY_ROUTING_MAP,
            routingPolicyLine);
      } else {
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
                                      new PermittedByAcl(oldOutgoingFilterName),
                                      new PermittedByAcl(zoneOutgoingAclName))))
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

  private Ip6AccessList toIp6AccessList(ExtendedIpv6AccessList eaList) {
    String name = eaList.getName();
    List<Ip6AccessListLine> lines = new ArrayList<>();
    for (ExtendedIpv6AccessListLine fromLine : eaList.getLines()) {
      Ip6AccessListLine newLine = new Ip6AccessListLine();
      newLine.setName(fromLine.getName());
      newLine.setAction(fromLine.getAction());
      Ip6Wildcard srcIpWildcard = fromLine.getSourceIpWildcard();
      if (srcIpWildcard != null) {
        newLine.getSrcIps().add(srcIpWildcard);
      }
      Ip6Wildcard dstIpWildcard = fromLine.getDestinationIpWildcard();
      if (dstIpWildcard != null) {
        newLine.getDstIps().add(dstIpWildcard);
      }
      // TODO: src/dst address group
      IpProtocol protocol = fromLine.getProtocol();
      if (protocol != IpProtocol.IP) {
        newLine.getIpProtocols().add(protocol);
      }
      newLine.getDstPorts().addAll(fromLine.getDstPorts());
      newLine.getSrcPorts().addAll(fromLine.getSrcPorts());
      Integer icmpType = fromLine.getIcmpType();
      if (icmpType != null) {
        newLine.setIcmpTypes(new TreeSet<>(Collections.singleton(new SubRange(icmpType))));
      }
      Integer icmpCode = fromLine.getIcmpCode();
      if (icmpCode != null) {
        newLine.setIcmpCodes(new TreeSet<>(Collections.singleton(new SubRange(icmpCode))));
      }
      Set<State> states = fromLine.getStates();
      newLine.getStates().addAll(states);
      List<TcpFlags> tcpFlags = fromLine.getTcpFlags();
      newLine.getTcpFlags().addAll(tcpFlags);
      Set<Integer> dscps = fromLine.getDscps();
      newLine.getDscps().addAll(dscps);
      Set<Integer> ecns = fromLine.getEcns();
      newLine.getEcns().addAll(ecns);
      lines.add(newLine);
    }
    return new Ip6AccessList(name, lines);
  }

  private IpAccessList toIpAccessList(ExtendedAccessList eaList) {
    String name = eaList.getName();
    List<IpAccessListLine> lines = new ArrayList<>(eaList.getLines().size());
    for (ExtendedAccessListLine fromLine : eaList.getLines()) {
      AclLineMatchExpr match;
      IpSpace srcIpSpace = fromLine.getSourceAddressSpecifier().toIpSpace();
      IpSpace dstIpSpace = fromLine.getDestinationAddressSpecifier().toIpSpace();
      AclLineMatchExpr matchService = fromLine.getServiceSpecifier().toAclLineMatchExpr();
      if (matchService instanceof MatchHeaderSpace) {
        match =
            new MatchHeaderSpace(
                ((MatchHeaderSpace) matchService)
                    .getHeaderspace()
                    .rebuild()
                    .setSrcIps(srcIpSpace)
                    .setDstIps(dstIpSpace)
                    .build());
      } else {
        match =
            new AndMatchExpr(
                ImmutableList.of(
                    matchService,
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(srcIpSpace)
                            .setDstIps(dstIpSpace)
                            .build())));
      }

      lines.add(
          IpAccessListLine.builder()
              .setAction(fromLine.getAction())
              .setMatchCondition(match)
              .setName(fromLine.getName())
              .build());
    }
    return new IpAccessList(name, lines);
  }

  private IpSpace toIpSpace(NetworkObjectGroup networkObjectGroup) {
    return IpWildcardSetIpSpace.builder().including(networkObjectGroup.getLines()).build();
  }

  private org.batfish.datamodel.IsisProcess toIsisProcess(
      IsisProcess proc, Configuration c, CiscoConfiguration oldConfig) {
    org.batfish.datamodel.IsisProcess newProcess = new org.batfish.datamodel.IsisProcess();

    newProcess.setNetAddress(proc.getNetAddress());
    newProcess.setLevel(proc.getLevel());

    // if (proc.getLevel() == IsisLevel.LEVEL_1_2) {
    // PolicyMap leakL1Policy = new PolicyMap(
    // ISIS_LEAK_L1_ROUTES_POLICY_NAME);
    // c.getPolicyMaps().put(ISIS_LEAK_L1_ROUTES_POLICY_NAME, leakL1Policy);
    // for (Entry<RoutingProtocol, IsisRedistributionPolicy> e : proc
    // .getRedistributionPolicies().entrySet()) {
    // if (!e.getKey().equals(RoutingProtocol.ISIS_L1)) {
    // continue;
    // }
    // IsisRedistributionPolicy rp = e.getValue();
    // Prefix summaryPrefix = rp.getSummaryPrefix();
    // // add clause suppressing l1 summarized routes, and also add
    // // aggregates for summarized addresses
    // PolicyMapClause suppressClause = new PolicyMapClause();
    // PolicyMapClause allowSummaryClause = new PolicyMapClause();
    // leakL1Policy.getClauses().add(suppressClause);
    // leakL1Policy.getClauses().add(allowSummaryClause);
    // suppressClause.setAction(PolicyMapAction.DENY);
    // allowSummaryClause.setAction(PolicyMapAction.PERMIT);
    // String summarizedFilterName =
    // ISIS_SUPPRESS_SUMMARIZED_ROUTE_FILTER_NAME
    // + ":" + summaryPrefix.toString();
    // RouteFilterList summarizedFilter = new RouteFilterList(
    // summarizedFilterName);
    // c.getRouteFilterLists().put(summarizedFilterName, summarizedFilter);
    // String summaryFilterName = ISIS_ALLOW_SUMMARY_ROUTE_FILTER_NAME
    // + ":" + summaryPrefix.toString();
    // RouteFilterList summaryFilter = new RouteFilterList(
    // summaryFilterName);
    // c.getRouteFilterLists().put(summaryFilterName, summaryFilter);
    // PolicyMapMatchRouteFilterListLine matchSummarized = new
    // PolicyMapMatchRouteFilterListLine(
    // Collections.singleton(summarizedFilter));
    // PolicyMapMatchRouteFilterListLine matchSummary = new
    // PolicyMapMatchRouteFilterListLine(
    // Collections.singleton(summaryFilter));
    // suppressClause.getMatchLines().add(matchSummarized);
    // suppressClause.getMatchLines()
    // .add(new PolicyMapMatchProtocolLine(RoutingProtocol.ISIS_L1));
    // allowSummaryClause.getMatchLines().add(matchSummary);
    // allowSummaryClause.getMatchLines().add(
    // new PolicyMapMatchProtocolLine(RoutingProtocol.AGGREGATE));
    // Integer summaryMetric = rp.getMetric();
    // if (summaryMetric == null) {
    // summaryMetric =
    // org.batfish.datamodel.IsisProcess.DEFAULT_ISIS_INTERFACE_COST;
    // }
    // allowSummaryClause.getSetLines()
    // .add(new PolicyMapSetMetricLine(summaryMetric));
    // IsisLevel summaryLevel = rp.getLevel();
    // if (summaryLevel == null) {
    // summaryLevel = IsisRedistributionPolicy.DEFAULT_LEVEL;
    // }
    // allowSummaryClause.getSetLines()
    // .add(new PolicyMapSetLevelLine(summaryLevel));
    // int length = summaryPrefix.getPrefixLength();
    // int rejectLowerBound = length + 1;
    // if (rejectLowerBound > Prefix.MAX_LENGTH) {
    // throw new VendorConversionException(
    // "Invalid summary prefix: " + summaryPrefix.toString());
    // }
    // SubRange summarizedRange = new SubRange(rejectLowerBound, Prefix.MAX_LENGTH);
    // RouteFilterLine summarized = new RouteFilterLine(LineAction.ACCEPT,
    // summaryPrefix, summarizedRange);
    // RouteFilterLine summary = new RouteFilterLine(LineAction.ACCEPT,
    // summaryPrefix, new SubRange(length, length));
    // summarizedFilter.addLine(summarized);
    // summaryFilter.addLine(summary);
    //
    // String filterName = "~ISIS_MATCH_SUMMARIZED_OF:"
    // + summaryPrefix.toString() + "~";
    // String generationPolicyName = "~ISIS_AGGREGATE_ROUTE_GEN:"
    // + summaryPrefix.toString() + "~";
    // PolicyMap generationPolicy = makeRouteExportPolicy(c,
    // generationPolicyName, filterName, summaryPrefix,
    // summarizedRange, LineAction.ACCEPT, null, null,
    // PolicyMapAction.PERMIT);
    // Set<PolicyMap> generationPolicies = new HashSet<>();
    // generationPolicies.add(generationPolicy);
    // GeneratedRoute gr = new GeneratedRoute(summaryPrefix,
    // MAX_ADMINISTRATIVE_COST, generationPolicies);
    // gr.setDiscard(true);
    // newProcess.getGeneratedRoutes().add(gr);
    // }
    // // add clause allowing remaining l1 routes
    // PolicyMapClause leakL1Clause = new PolicyMapClause();
    // leakL1Clause.setAction(PolicyMapAction.PERMIT);
    // leakL1Clause.getMatchLines()
    // .add(new PolicyMapMatchProtocolLine(RoutingProtocol.ISIS_L1));
    // leakL1Policy.getClauses().add(leakL1Clause);
    // newProcess.getOutboundPolicyMaps().add(leakL1Policy);
    // leakL1Clause.getSetLines()
    // .add(new PolicyMapSetLevelLine(IsisLevel.LEVEL_2));
    //
    // // generate routes, policies for summary addresses
    // }
    //
    // // policy map for redistributing connected routes
    // // TODO: honor subnets option
    // IsisRedistributionPolicy rcp = proc.getRedistributionPolicies()
    // .get(RoutingProtocol.CONNECTED);
    // if (rcp != null) {
    // Integer metric = rcp.getMetric();
    // IsisLevel exportLevel = rcp.getLevel();
    // boolean explicitMetric = metric != null;
    // boolean routeMapMetric = false;
    // if (!explicitMetric) {
    // metric =
    // IsisRedistributionPolicy.DEFAULT_REDISTRIBUTE_CONNECTED_METRIC;
    // }
    // // add default export map with metric
    // PolicyMap exportConnectedPolicy;
    // String mapName = rcp.getMap();
    // if (mapName != null) {
    // exportConnectedPolicy = c.getPolicyMaps().get(mapName);
    // if (exportConnectedPolicy == null) {
    // undefined("undefined reference to route-map: " + mapName,
    // ROUTE_MAP, mapName);
    // }
    // else {
    // RouteMap exportConnectedRouteMap = _routeMaps.get(mapName);
    // exportConnectedRouteMap.getReferers().put(proc,
    // "is-is export connected route-map");
    //
    // // crash if both an explicit metric is set and one exists in the
    // // route map
    // for (PolicyMapClause clause : exportConnectedPolicy
    // .getClauses()) {
    // for (PolicyMapSetLine line : clause.getSetLines()) {
    // if (line.getType() == PolicyMapSetType.METRIC) {
    // if (explicitMetric) {
    // throw new Error(
    // "Explicit redistribution metric set while route map also contains set
    // metric line");
    // }
    // else {
    // routeMapMetric = true;
    // break;
    // }
    // }
    // }
    // }
    // PolicyMapMatchLine matchConnectedLine = new PolicyMapMatchProtocolLine(
    // RoutingProtocol.CONNECTED);
    // PolicyMapSetLine setMetricLine = null;
    // // add a set metric line if no metric provided by route map
    // if (!routeMapMetric) {
    // // use default metric if no explicit metric is set
    // setMetricLine = new PolicyMapSetMetricLine(metric);
    // }
    // for (PolicyMapClause clause : exportConnectedPolicy
    // .getClauses()) {
    // clause.getMatchLines().add(matchConnectedLine);
    // if (!routeMapMetric) {
    // clause.getSetLines().add(setMetricLine);
    // }
    // }
    // newProcess.getOutboundPolicyMaps().add(exportConnectedPolicy);
    // newProcess.getPolicyExportLevels()
    // .put(exportConnectedPolicy.getName(), exportLevel);
    // }
    // }
    // else {
    // exportConnectedPolicy = makeRouteExportPolicy(c,
    // ISIS_EXPORT_CONNECTED_POLICY_NAME, null, null, null, null,
    // metric, RoutingProtocol.CONNECTED, PolicyMapAction.PERMIT);
    // newProcess.getOutboundPolicyMaps().add(exportConnectedPolicy);
    // newProcess.getPolicyExportLevels()
    // .put(exportConnectedPolicy.getName(), exportLevel);
    // c.getPolicyMaps().put(exportConnectedPolicy.getName(),
    // exportConnectedPolicy);
    // }
    // }
    //
    // // policy map for redistributing static routes
    // // TODO: honor subnets option
    // IsisRedistributionPolicy rsp = proc.getRedistributionPolicies()
    // .get(RoutingProtocol.STATIC);
    // if (rsp != null) {
    // Integer metric = rsp.getMetric();
    // IsisLevel exportLevel = rsp.getLevel();
    // boolean explicitMetric = metric != null;
    // boolean routeMapMetric = false;
    // if (!explicitMetric) {
    // metric = IsisRedistributionPolicy.DEFAULT_REDISTRIBUTE_STATIC_METRIC;
    // }
    // // add export map with metric
    // PolicyMap exportStaticPolicy;
    // String mapName = rsp.getMap();
    // if (mapName != null) {
    // exportStaticPolicy = c.getPolicyMaps().get(mapName);
    // if (exportStaticPolicy == null) {
    // undefined("undefined reference to route-map: " + mapName,
    // ROUTE_MAP, mapName);
    // }
    // else {
    // RouteMap exportStaticRouteMap = _routeMaps.get(mapName);
    // exportStaticRouteMap.getReferers().put(proc,
    // "is-is static redistribution route-map");
    // // crash if both an explicit metric is set and one exists in the
    // // route map
    // for (PolicyMapClause clause : exportStaticPolicy.getClauses()) {
    // for (PolicyMapSetLine line : clause.getSetLines()) {
    // if (line.getType() == PolicyMapSetType.METRIC) {
    // if (explicitMetric) {
    // throw new Error(
    // "Explicit redistribution metric set while route map also contains set
    // metric line");
    // }
    // else {
    // routeMapMetric = true;
    // break;
    // }
    // }
    // }
    // }
    // PolicyMapSetLine setMetricLine = null;
    // // add a set metric line if no metric provided by route map
    // if (!routeMapMetric) {
    // // use default metric if no explicit metric is set
    // setMetricLine = new PolicyMapSetMetricLine(metric);
    // }
    //
    // PolicyMapMatchLine matchStaticLine = new PolicyMapMatchProtocolLine(
    // RoutingProtocol.STATIC);
    // for (PolicyMapClause clause : exportStaticPolicy.getClauses()) {
    // boolean containsRouteFilterList = modifyRejectDefault(clause);
    // if (!containsRouteFilterList) {
    // RouteFilterList generatedRejectDefaultRouteList = c
    // .getRouteFilterLists()
    // .get(ISIS_EXPORT_STATIC_REJECT_DEFAULT_ROUTE_FILTER_NAME);
    // if (generatedRejectDefaultRouteList == null) {
    // generatedRejectDefaultRouteList = makeRouteFilter(
    // ISIS_EXPORT_STATIC_REJECT_DEFAULT_ROUTE_FILTER_NAME,
    // Prefix.ZERO, new SubRange(0, 0),
    // LineAction.REJECT);
    // }
    // Set<RouteFilterList> lists = new HashSet<>();
    // lists.add(generatedRejectDefaultRouteList);
    // PolicyMapMatchLine line = new PolicyMapMatchRouteFilterListLine(
    // lists);
    // clause.getMatchLines().add(line);
    // }
    // Set<PolicyMapSetLine> setList = clause.getSetLines();
    // clause.getMatchLines().add(matchStaticLine);
    // if (!routeMapMetric) {
    // setList.add(setMetricLine);
    // }
    // }
    // newProcess.getOutboundPolicyMaps().add(exportStaticPolicy);
    // newProcess.getPolicyExportLevels()
    // .put(exportStaticPolicy.getName(), exportLevel);
    //
    // }
    // }
    // else { // export static routes without named policy
    // exportStaticPolicy = makeRouteExportPolicy(c,
    // ISIS_EXPORT_STATIC_POLICY_NAME,
    // ISIS_EXPORT_STATIC_REJECT_DEFAULT_ROUTE_FILTER_NAME,
    // Prefix.ZERO, new SubRange(0, 0), LineAction.REJECT, metric,
    // RoutingProtocol.STATIC, PolicyMapAction.PERMIT);
    // newProcess.getOutboundPolicyMaps().add(exportStaticPolicy);
    // newProcess.getPolicyExportLevels().put(exportStaticPolicy.getName(),
    // exportLevel);
    // }
    // }
    return newProcess;
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

  static final Not NOT_DEFAULT_ROUTE =
      new Not(
          new MatchPrefixSet(
              new DestinationNetwork(),
              new ExplicitPrefixSet(
                  new PrefixSpace(
                      Collections.singleton(new PrefixRange(Prefix.ZERO, new SubRange(0, 0)))))));

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
      if (exportRouteMap == null) {
        undefined(
            CiscoStructureType.ROUTE_MAP,
            exportRouteMapName,
            structureType,
            policy.getRouteMapLine());
      } else {
        exportRouteMap.getReferers().put(proc, structureType.getDescription());
        ospfExportConditions.getConjuncts().add(new CallExpr(exportRouteMapName));
      }
    }

    ospfExportStatements.add(Statements.ExitAccept.toStaticStatement());

    // Construct the policy and add it before returning.
    If ospfExportIf = new If();
    ospfExportIf.setComment("OSPF export routes for " + protocol.protocolName());
    ospfExportIf.setGuard(ospfExportConditions);
    ospfExportIf.setTrueStatements(ospfExportStatements.build());

    return ospfExportIf;
  }

  private org.batfish.datamodel.OspfProcess toOspfProcess(
      OspfProcess proc, String vrfName, Configuration c, CiscoConfiguration oldConfig) {
    org.batfish.datamodel.OspfProcess newProcess = new org.batfish.datamodel.OspfProcess();
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
    List<OspfNetwork> networks = new ArrayList<>();
    networks.addAll(proc.getNetworks());
    Collections.sort(
        networks,
        new Comparator<OspfNetwork>() {
          // sort so longest prefixes are first
          @Override
          public int compare(OspfNetwork lhs, OspfNetwork rhs) {
            int lhsPrefixLength = lhs.getPrefix().getPrefixLength();
            int rhsPrefixLength = rhs.getPrefix().getPrefixLength();
            int result = Integer.compare(rhsPrefixLength, lhsPrefixLength); // intentionally swapped
            if (result == 0) {
              long lhsIp = lhs.getPrefix().getStartIp().asLong();
              long rhsIp = rhs.getPrefix().getStartIp().asLong();
              result = Long.compare(lhsIp, rhsIp);
            }
            return result;
          }
        });

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
                prefix,
                new SubRange(filterMinPrefixLength, Prefix.MAX_PREFIX_LENGTH)));
      }
      area.setSummaries(ImmutableSortedMap.copyOf(summaries));
      summaryFilter.addLine(
          new RouteFilterLine(
              LineAction.ACCEPT, Prefix.ZERO, new SubRange(0, Prefix.MAX_PREFIX_LENGTH)));
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
      ospfExportDefaultConditions
          .getConjuncts()
          .add(
              new MatchPrefixSet(
                  new DestinationNetwork(),
                  new ExplicitPrefixSet(
                      new PrefixSpace(
                          Collections.singleton(
                              new PrefixRange(Prefix.ZERO, new SubRange(0, 0)))))));
      long metric = proc.getDefaultInformationMetric();
      ospfExportDefaultStatements.add(new SetMetric(new LiteralLong(metric)));
      OspfMetricType metricType = proc.getDefaultInformationMetricType();
      ospfExportDefaultStatements.add(new SetOspfMetricType(metricType));
      // add default export map with metric
      String defaultOriginateMapName = proc.getDefaultInformationOriginateMap();
      boolean useAggregateDefaultOnly;
      if (defaultOriginateMapName != null) {
        int defaultOriginateMapLine = proc.getDefaultInformationOriginateMapLine();
        useAggregateDefaultOnly = true;
        RoutingPolicy ospfDefaultGenerationPolicy =
            c.getRoutingPolicies().get(defaultOriginateMapName);
        if (ospfDefaultGenerationPolicy == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              defaultOriginateMapName,
              CiscoStructureUsage.OSPF_DEFAULT_ORIGINATE_ROUTE_MAP,
              defaultOriginateMapLine);
        } else {
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
      ripExportDefaultConditions
          .getConjuncts()
          .add(
              new MatchPrefixSet(
                  new DestinationNetwork(),
                  new ExplicitPrefixSet(
                      new PrefixSpace(
                          Collections.singleton(
                              new PrefixRange(Prefix.ZERO, new SubRange(0, 0)))))));
      long metric = proc.getDefaultInformationMetric();
      ripExportDefaultStatements.add(new SetMetric(new LiteralLong(metric)));
      // add default export map with metric
      String defaultOriginateMapName = proc.getDefaultInformationOriginateMap();
      if (defaultOriginateMapName != null) {
        int defaultOriginateMapLine = proc.getDefaultInformationOriginateMapLine();
        RoutingPolicy ripDefaultGenerationPolicy =
            c.getRoutingPolicies().get(defaultOriginateMapName);
        if (ripDefaultGenerationPolicy == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              defaultOriginateMapName,
              CiscoStructureUsage.RIP_DEFAULT_ORIGINATE_ROUTE_MAP,
              defaultOriginateMapLine);
        } else {
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
        int exportConnectedRouteMapLine = rcp.getRouteMapLine();
        RouteMap exportConnectedRouteMap = _routeMaps.get(exportConnectedRouteMapName);
        if (exportConnectedRouteMap == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              exportConnectedRouteMapName,
              CiscoStructureUsage.RIP_REDISTRIBUTE_CONNECTED_MAP,
              exportConnectedRouteMapLine);
        } else {
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
      ripExportStaticConditions
          .getConjuncts()
          .add(
              new Not(
                  new MatchPrefixSet(
                      new DestinationNetwork(),
                      new ExplicitPrefixSet(
                          new PrefixSpace(
                              Collections.singleton(
                                  new PrefixRange(Prefix.ZERO, new SubRange(0, 0))))))));

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
        int exportStaticRouteMapLine = rsp.getRouteMapLine();
        RouteMap exportStaticRouteMap = _routeMaps.get(exportStaticRouteMapName);
        if (exportStaticRouteMap == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              exportStaticRouteMapName,
              CiscoStructureUsage.RIP_REDISTRIBUTE_STATIC_MAP,
              exportStaticRouteMapLine);
        } else {
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
      ripExportBgpConditions
          .getConjuncts()
          .add(
              new Not(
                  new MatchPrefixSet(
                      new DestinationNetwork(),
                      new ExplicitPrefixSet(
                          new PrefixSpace(
                              Collections.singleton(
                                  new PrefixRange(Prefix.ZERO, new SubRange(0, 0))))))));

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
        int exportBgpRouteMapLine = rbp.getRouteMapLine();
        RouteMap exportBgpRouteMap = _routeMaps.get(exportBgpRouteMapName);
        if (exportBgpRouteMap == null) {
          undefined(
              CiscoStructureType.ROUTE_MAP,
              exportBgpRouteMapName,
              CiscoStructureUsage.RIP_REDISTRIBUTE_BGP_MAP,
              exportBgpRouteMapLine);
        } else {
          exportBgpRouteMap.getReferers().put(proc, "rip redistribute bgp route-map");
          ripExportBgpConditions.getConjuncts().add(new CallExpr(exportBgpRouteMapName));
        }
      }
      ripExportBgpStatements.add(Statements.ExitAccept.toStaticStatement());
      ripExportBgp.setGuard(ripExportBgpConditions);
    }
    return newProcess;
  }

  private Route6FilterLine toRoute6FilterLine(ExtendedIpv6AccessListLine fromLine) {
    LineAction action = fromLine.getAction();
    Ip6 ip = fromLine.getSourceIpWildcard().getIp();
    BigInteger minSubnet = fromLine.getDestinationIpWildcard().getIp().asBigInteger();
    BigInteger maxSubnet =
        minSubnet.or(fromLine.getDestinationIpWildcard().getWildcard().asBigInteger());
    int minPrefixLength = fromLine.getDestinationIpWildcard().getIp().numSubnetBits();
    int maxPrefixLength = new Ip6(maxSubnet).numSubnetBits();
    int statedPrefixLength =
        fromLine.getSourceIpWildcard().getWildcard().inverted().numSubnetBits();
    int prefixLength = Math.min(statedPrefixLength, minPrefixLength);
    Prefix6 prefix = new Prefix6(ip, prefixLength);
    return new Route6FilterLine(action, prefix, new SubRange(minPrefixLength, maxPrefixLength));
  }

  private Route6FilterList toRoute6FilterList(ExtendedIpv6AccessList eaList) {
    String name = eaList.getName();
    Route6FilterList newList = new Route6FilterList(name);
    List<Route6FilterLine> lines = new ArrayList<>();
    for (ExtendedIpv6AccessListLine fromLine : eaList.getLines()) {
      Route6FilterLine newLine = toRoute6FilterLine(fromLine);
      lines.add(newLine);
    }
    newList.getLines().addAll(lines);
    return newList;
  }

  private Route6FilterList toRoute6FilterList(Prefix6List list) {
    Route6FilterList newRouteFilterList = new Route6FilterList(list.getName());
    for (Prefix6ListLine prefixListLine : list.getLines()) {
      Route6FilterLine newRouteFilterListLine =
          new Route6FilterLine(
              prefixListLine.getAction(),
              prefixListLine.getPrefix(),
              prefixListLine.getLengthRange());
      newRouteFilterList.addLine(newRouteFilterListLine);
    }
    return newRouteFilterList;
  }

  private RouteFilterLine toRouteFilterLine(ExtendedAccessListLine fromLine) {
    LineAction action = fromLine.getAction();
    IpWildcard srcIpWildcard =
        ((WildcardAddressSpecifier) fromLine.getSourceAddressSpecifier()).getIpWildcard();
    Ip ip = srcIpWildcard.getIp();
    IpWildcard dstIpWildcard =
        ((WildcardAddressSpecifier) fromLine.getDestinationAddressSpecifier()).getIpWildcard();
    long minSubnet = dstIpWildcard.getIp().asLong();
    long maxSubnet = minSubnet | dstIpWildcard.getWildcard().asLong();
    int minPrefixLength = dstIpWildcard.getIp().numSubnetBits();
    int maxPrefixLength = new Ip(maxSubnet).numSubnetBits();
    int statedPrefixLength = srcIpWildcard.getWildcard().inverted().numSubnetBits();
    int prefixLength = Math.min(statedPrefixLength, minPrefixLength);
    Prefix prefix = new Prefix(ip, prefixLength);
    return new RouteFilterLine(action, prefix, new SubRange(minPrefixLength, maxPrefixLength));
  }

  private RouteFilterList toRouteFilterList(ExtendedAccessList eaList) {
    String name = eaList.getName();
    RouteFilterList newList = new RouteFilterList(name);
    for (ExtendedAccessListLine fromLine : eaList.getLines()) {
      RouteFilterLine newLine = toRouteFilterLine(fromLine);
      newList.addLine(newLine);
    }
    return newList;
  }

  private RouteFilterList toRouteFilterList(PrefixList list) {
    RouteFilterList newRouteFilterList = new RouteFilterList(list.getName());
    List<RouteFilterLine> newLines =
        list.getLines()
            .stream()
            .map(l -> new RouteFilterLine(l.getAction(), l.getPrefix(), l.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    newRouteFilterList.setLines(newLines);
    return newRouteFilterList;
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
        int statementLine = continueStatement.getStatementLine();
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
                statementLine);
            continueStatement = null;
          }
        } else {
          continueStatement = null;
        }
      }
      switch (rmClause.getAction()) {
        case ACCEPT:
          if (continueStatement == null) {
            onMatchStatements.add(Statements.ReturnTrue.toStaticStatement());
          } else {
            onMatchStatements.add(Statements.SetLocalDefaultActionAccept.toStaticStatement());
            onMatchStatements.add(new CallStatement(continueTargetPolicy.getName()));
          }
          break;

        case REJECT:
          onMatchStatements.add(Statements.ReturnFalse.toStaticStatement());
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
    If endPolicy = new If();
    If nonBoolean = new If();
    endPolicy.setGuard(BooleanExprs.CallExprContext.toStaticBooleanExpr());
    endPolicy.setTrueStatements(
        Collections.singletonList(Statements.ReturnLocalDefaultAction.toStaticStatement()));
    endPolicy.setFalseStatements(Collections.singletonList(nonBoolean));
    nonBoolean.setGuard(BooleanExprs.CallStatementContext.toStaticBooleanExpr());
    nonBoolean.setTrueStatements(Collections.singletonList(Statements.Return.toStaticStatement()));
    nonBoolean.setFalseStatements(
        Collections.singletonList(Statements.DefaultAction.toStaticStatement()));
    return rp;
  }

  private org.batfish.datamodel.StaticRoute toStaticRoute(
      Configuration c, StaticRoute staticRoute) {
    Ip nextHopIp = staticRoute.getNextHopIp();
    Prefix prefix = staticRoute.getPrefix();
    String nextHopInterface = staticRoute.getNextHopInterface();
    if (nextHopInterface != null && CommonUtil.isNullInterface(nextHopInterface)) {
      nextHopInterface = org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
    }
    Integer oldTag = staticRoute.getTag();
    int tag;
    tag = oldTag != null ? oldTag : -1;
    return org.batfish.datamodel.StaticRoute.builder()
        .setNetwork(prefix)
        .setNextHopIp(nextHopIp)
        .setNextHopInterface(nextHopInterface)
        .setAdministrativeCost(staticRoute.getDistance())
        .setTag(tag)
        .build();
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
      AsPathAccessList apList = toAsPathAccessList(pathList);
      c.getAsPathAccessLists().put(apList.getName(), apList);
    }

    // convert as-path-sets to vendor independent format
    for (AsPathSet asPathSet : _asPathSets.values()) {
      AsPathAccessList apList = toAsPathAccessList(asPathSet);
      c.getAsPathAccessLists().put(apList.getName(), apList);
    }

    // convert standard/expanded community lists to community lists
    for (StandardCommunityList scList : _standardCommunityLists.values()) {
      ExpandedCommunityList ecList = scList.toExpandedCommunityList();
      CommunityList cList = toCommunityList(ecList);
      c.getCommunityLists().put(cList.getName(), cList);
    }
    for (ExpandedCommunityList ecList : _expandedCommunityLists.values()) {
      CommunityList cList = toCommunityList(ecList);
      c.getCommunityLists().put(cList.getName(), cList);
    }

    // convert prefix lists to route filter lists
    for (PrefixList prefixList : _prefixLists.values()) {
      RouteFilterList newRouteFilterList = toRouteFilterList(prefixList);
      c.getRouteFilterLists().put(newRouteFilterList.getName(), newRouteFilterList);
    }

    // convert ipv6 prefix lists to route6 filter lists
    for (Prefix6List prefixList : _prefix6Lists.values()) {
      Route6FilterList newRouteFilterList = toRoute6FilterList(prefixList);
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
        RouteFilterList rfList = toRouteFilterList(eaList);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      IpAccessList ipaList = toIpAccessList(eaList);
      c.getIpAccessLists().put(ipaList.getName(), ipaList);
    }

    // convert each NetworkObjectGroup to IpSpace
    _networkObjectGroups.forEach(
        (name, networkObjectGroup) -> c.getIpSpaces().put(name, toIpSpace(networkObjectGroup)));

    // convert each ServiceObjectGroup to IpAccessList
    _serviceObjectGroups.forEach(
        (name, serviceObjectGroup) ->
            c.getIpAccessLists()
                .put(computeServiceObjectGroupAclName(name), toIpAccessList(serviceObjectGroup)));

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
        Route6FilterList rfList = toRoute6FilterList(eaList);
        c.getRoute6FilterLists().put(rfList.getName(), rfList);
      }
      Ip6AccessList ipaList = toIp6AccessList(eaList);
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

    addIkePoliciesAndGateways(c);

    // ipsec proposals
    for (Entry<String, IpsecTransformSet> e : _ipsecTransformSets.entrySet()) {
      c.getIpsecProposals().put(e.getKey(), e.getValue().getProposal());
    }

    // ipsec policies
    for (Entry<String, IpsecProfile> e : _ipsecProfiles.entrySet()) {
      String name = e.getKey();
      IpsecProfile profile = e.getValue();

      IpsecPolicy policy = new IpsecPolicy(name);
      policy.setPfsKeyGroup(profile.getPfsGroup());
      String transformSetName = profile.getTransformSet();
      if (c.getIpsecProposals().containsKey(transformSetName)) {
        policy.getProposals().put(transformSetName, c.getIpsecProposals().get(transformSetName));
      }
      c.getIpsecPolicies().put(name, policy);
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
        Ip source = tunnel.getSource();
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
            newVrf.getStaticRoutes().add(toStaticRoute(c, staticRoute));
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
            org.batfish.datamodel.OspfProcess newOspfProcess =
                toOspfProcess(ospfProcess, vrfName, c, this);
            newVrf.setOspfProcess(newOspfProcess);
          }

          // convert isis process
          IsisProcess isisProcess = vrf.getIsisProcess();
          if (isisProcess != null) {
            org.batfish.datamodel.IsisProcess newIsisProcess = toIsisProcess(isisProcess, c, this);
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
      String name = e.getKey();
      int line = e.getValue();
      undefined(
          CiscoStructureType.BGP_PEER_GROUP,
          name,
          CiscoStructureUsage.BGP_NEIGHBOR_STATEMENT,
          line);
    }

    // mark references to IPv4/6 ACLs that may not appear in data model
    markIpOrMacAcls(CiscoStructureUsage.CLASS_MAP_ACCESS_GROUP);
    markIpv4Acls(CiscoStructureUsage.CONTROL_PLANE_ACCESS_GROUP);
    markAcls(CiscoStructureUsage.COPS_LISTENER_ACCESS_LIST);
    markAcls(CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL);
    markAcls(CiscoStructureUsage.INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP);
    markAcls(CiscoStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL);
    markIpv4Acls(CiscoStructureUsage.INTERFACE_IGMP_STATIC_GROUP_ACL);
    markAcls(CiscoStructureUsage.INTERFACE_IP_INBAND_ACCESS_GROUP);
    markIpv4Acls(CiscoStructureUsage.INTERFACE_IP_VERIFY_ACCESS_LIST);
    markIpv4Acls(CiscoStructureUsage.INTERFACE_PIM_NEIGHBOR_FILTER);
    markIpv4Acls(CiscoStructureUsage.IP_NAT_DESTINATION_ACCESS_LIST);
    markIpv4Acls(CiscoStructureUsage.IP_NAT_SOURCE_ACCESS_LIST);
    markAcls(CiscoStructureUsage.LINE_ACCESS_CLASS_LIST);
    markIpv6Acls(CiscoStructureUsage.LINE_ACCESS_CLASS_LIST6);
    markIpv4Acls(CiscoStructureUsage.MANAGEMENT_SSH_ACCESS_GROUP);
    markIpv4Acls(CiscoStructureUsage.MANAGEMENT_TELNET_ACCESS_GROUP);
    markIpv4Acls(CiscoStructureUsage.MSDP_PEER_SA_LIST);
    markIpv4Acls(CiscoStructureUsage.NTP_ACCESS_GROUP);
    markIpv4Acls(CiscoStructureUsage.PIM_ACCEPT_REGISTER_ACL);
    markIpv4Acls(CiscoStructureUsage.PIM_ACCEPT_RP_ACL);
    markIpv4Acls(CiscoStructureUsage.PIM_RP_ADDRESS_ACL);
    markIpv4Acls(CiscoStructureUsage.PIM_RP_ANNOUNCE_FILTER);
    markIpv4Acls(CiscoStructureUsage.PIM_RP_CANDIDATE_ACL);
    markIpv4Acls(CiscoStructureUsage.PIM_SEND_RP_ANNOUNCE_ACL);
    markIpv4Acls(CiscoStructureUsage.PIM_SPT_THRESHOLD_ACL);
    markAcls(CiscoStructureUsage.RIP_DISTRIBUTE_LIST);
    markAcls(CiscoStructureUsage.ROUTER_ISIS_DISTRIBUTE_LIST_ACL);
    markAcls(CiscoStructureUsage.SNMP_SERVER_FILE_TRANSFER_ACL);
    markAcls(CiscoStructureUsage.SNMP_SERVER_TFTP_SERVER_LIST);
    markAcls(CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL);
    markIpv4Acls(CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL4);
    markIpv6Acls(CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL6);
    markAcls(CiscoStructureUsage.SSH_ACL);
    markIpv4Acls(CiscoStructureUsage.SSH_IPV4_ACL);
    markIpv6Acls(CiscoStructureUsage.SSH_IPV6_ACL);
    markAcls(CiscoStructureUsage.WCCP_GROUP_LIST);
    markAcls(CiscoStructureUsage.WCCP_REDIRECT_LIST);
    markAcls(CiscoStructureUsage.WCCP_SERVICE_LIST);

    // mark references to mac-ACLs that may not appear in data model
    // TODO: fill in

    // mark references to route-maps that may not appear in data model
    markRouteMaps(CiscoStructureUsage.BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP);
    markRouteMaps(CiscoStructureUsage.BGP_REDISTRIBUTE_OSPFV3_MAP);
    markRouteMaps(CiscoStructureUsage.BGP_ROUTE_MAP_OTHER);
    markRouteMaps(CiscoStructureUsage.BGP_VRF_AGGREGATE_ROUTE_MAP);
    markRouteMaps(CiscoStructureUsage.PIM_ACCEPT_REGISTER_ROUTE_MAP);

    // Cable
    markDepiClasses(CiscoStructureUsage.DEPI_TUNNEL_DEPI_CLASS);
    markDepiTunnels(CiscoStructureUsage.CONTROLLER_DEPI_TUNNEL);
    markDepiTunnels(CiscoStructureUsage.DEPI_TUNNEL_PROTECT_TUNNEL);
    markDocsisPolicies(CiscoStructureUsage.DOCSIS_GROUP_DOCSIS_POLICY);
    markDocsisPolicyRules(CiscoStructureUsage.DOCSIS_POLICY_DOCSIS_POLICY_RULE);
    markServiceClasses(CiscoStructureUsage.QOS_ENFORCE_RULE_SERVICE_CLASS);

    // L2tp
    markL2tpClasses(CiscoStructureUsage.DEPI_TUNNEL_L2TP_CLASS);

    // Vpn
    markIpsecProfiles(CiscoStructureUsage.TUNNEL_PROTECTION_IPSEC_PROFILE);
    markIpsecTransformSets(CiscoStructureUsage.IPSEC_PROFILE_TRANSFORM_SET);
    markKeyrings(CiscoStructureUsage.ISAKMP_PROFILE_KEYRING);

    // class-map
    markInspectClassMaps(CiscoStructureUsage.INSPECT_POLICY_MAP_INSPECT_CLASS);

    // policy-map
    markInspectPolicyMaps(CiscoStructureUsage.ZONE_PAIR_INSPECT_SERVICE_POLICY);

    // object-group
    markNetworkObjectGroups(CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP);
    markServiceObjectGroups(CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT_GROUP);

    // zone
    markSecurityZones(CiscoStructureUsage.INTERFACE_ZONE_MEMBER);
    markSecurityZones(CiscoStructureUsage.ZONE_PAIR_DESTINATION_ZONE);
    markSecurityZones(CiscoStructureUsage.ZONE_PAIR_SOURCE_ZONE);

    // warn about unreferenced data structures
    warnUnusedStructure(_asPathSets, CiscoStructureType.AS_PATH_SET);
    warnUnusedCommunityLists();
    warnUnusedStructure(_cf.getDepiClasses(), CiscoStructureType.DEPI_CLASS);
    warnUnusedStructure(_cf.getDepiTunnels(), CiscoStructureType.DEPI_TUNNEL);
    warnUnusedDocsisPolicies();
    warnUnusedDocsisPolicyRules();
    warnUnusedStructure(_asPathAccessLists, CiscoStructureType.AS_PATH_ACCESS_LIST);
    warnUnusedIpAccessLists();
    warnUnusedStructure(_inspectClassMaps, CiscoStructureType.INSPECT_CLASS_MAP);
    warnUnusedStructure(_inspectPolicyMaps, CiscoStructureType.INSPECT_POLICY_MAP);
    warnUnusedStructure(_ipsecProfiles, CiscoStructureType.IPSEC_PROFILE);
    warnUnusedStructure(_ipsecTransformSets, CiscoStructureType.IPSEC_TRANSFORM_SET);
    warnUnusedIpv6AccessLists();
    warnUnusedKeyrings();
    warnUnusedStructure(_cf.getL2tpClasses(), CiscoStructureType.L2TP_CLASS);
    warnUnusedStructure(_macAccessLists, CiscoStructureType.MAC_ACCESS_LIST);
    warnUnusedStructure(_natPools, CiscoStructureType.NAT_POOL);
    warnUnusedStructure(_networkObjectGroups, CiscoStructureType.NETWORK_OBJECT_GROUP);
    warnUnusedStructure(_prefixLists, CiscoStructureType.PREFIX_LIST);
    warnUnusedStructure(_prefix6Lists, CiscoStructureType.PREFIX6_LIST);
    warnUnusedPeerGroups();
    warnUnusedPeerSessions();
    warnUnusedStructure(_routeMaps, CiscoStructureType.ROUTE_MAP);
    warnUnusedStructure(_securityZones, CiscoStructureType.SECURITY_ZONE);
    warnUnusedStructure(_serviceObjectGroups, CiscoStructureType.SERVICE_OBJECT_GROUP);
    warnUnusedServiceClasses();
    c.simplifyRoutingPolicies();

    c.computeRoutingPolicySources(_w);

    return c;
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
          ImmutableList.Builder<IpAccessListLine> disjuncts = ImmutableList.builder();
          inspectPolicyMap
              .getInspectClasses()
              .forEach(
                  (inspectClassName, inspectPolicyMapInspectClass) -> {
                    if (!inspectPolicyMapInspectClass.getInspect()) {
                      return;
                    }
                    String inspectClassMapAclName = computeInspectClassMapAclName(inspectClassName);
                    if (!c.getIpAccessLists().containsKey(inspectClassMapAclName)) {
                      return;
                    }
                    disjuncts.add(
                        IpAccessListLine.accepting()
                            .setMatchCondition(new PermittedByAcl(inspectClassMapAclName))
                            .build());
                  });
          IpAccessList.builder()
              .setOwner(c)
              .setName(inspectPolicyMapAclName)
              .setLines(disjuncts.build())
              .build();
        });
  }

  private void createZoneAcls(Configuration c) {
    Map<String, MatchSrcInterface> matchSrcInterfaceBySrcZone =
        toImmutableMap(
            c.getZones(),
            Entry::getKey,
            zoneByNameEntry -> new MatchSrcInterface(zoneByNameEntry.getValue().getInterfaces()));
    _securityZonePairs.forEach(
        (dstZoneName, zonePairsBySrcZoneName) -> {
          if (!_securityZones.containsKey(dstZoneName)) {
            return;
          }
          ImmutableList.Builder<IpAccessListLine> zonePairPermits = ImmutableList.builder();
          zonePairsBySrcZoneName.forEach(
              (srcZoneName, zonePair) ->
                  createZonePairAcl(
                          c,
                          matchSrcInterfaceBySrcZone.get(srcZoneName),
                          dstZoneName,
                          srcZoneName,
                          zonePair.getInspectPolicyMap())
                      .ifPresent(zonePairPermits::add));
          String zoneOutgoingAclName = computeZoneOutgoingAclName(dstZoneName);
          IpAccessList.builder()
              .setName(zoneOutgoingAclName)
              .setOwner(c)
              .setLines(zonePairPermits.build())
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
                    .build()))
        .build();
    return Optional.of(
        IpAccessListLine.accepting()
            .setMatchCondition(new PermittedByAcl(zonePairAclName))
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

  private IpAccessList toIpAccessList(ServiceObjectGroup serviceObjectGroup) {
    return IpAccessList.builder()
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(serviceObjectGroup.toAclLineMatchExpr())
                    .build()))
        .setName(computeServiceObjectGroupAclName(serviceObjectGroup.getName()))
        .build();
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
        _w.redFlag("Cannot get PSK hash since keyring not configured for isakmpProfile " + name);
      } else if (_keyrings.containsKey(keyringName)) {
        Keyring keyring = _keyrings.get(keyringName);
        if (keyring.match(isakmpProfile.getLocalAddress(), isakmpProfile.getMatchIdentity())) {
          ikePolicy.setPreSharedKeyHash(keyring.getKey());
        } else {
          _w.redFlag(
              "The addresses of keyring " + keyringName + " do not match isakmpProfile " + name);
        }
      }

      Ip localAddress = isakmpProfile.getLocalAddress();
      Prefix remotePrefix = isakmpProfile.getMatchIdentity();
      if (localAddress == null || remotePrefix == null) {
        _w.redFlag(
            "Can't get IkeGateway: Local or remote address is not set for isakmpProfile " + name);
      } else {
        IkeGateway ikeGateway = new IkeGateway(e.getKey());
        c.getIkeGateways().put(name, ikeGateway);
        ikeGateway.setAddress(remotePrefix.getStartIp());
        Interface oldIface = getInterfaceByTunnelAddresses(localAddress, remotePrefix);
        if (oldIface != null) {
          ikeGateway.setExternalInterface(c.getInterfaces().get(oldIface.getName()));
        } else {
          _w.redFlag("External interface not found for ikeGateway for isakmpProfile " + name);
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

  private void warnUnusedCommunityLists() {
    warnUnusedStructure(_expandedCommunityLists, CiscoStructureType.COMMUNITY_LIST_EXPANDED);
    warnUnusedStructure(_standardCommunityLists, CiscoStructureType.COMMUNITY_LIST_STANDARD);
  }

  private void warnUnusedDocsisPolicies() {
    if (_cf.getCable() != null) {
      warnUnusedStructure(_cf.getCable().getDocsisPolicies(), CiscoStructureType.DOCSIS_POLICY);
    }
  }

  private void warnUnusedDocsisPolicyRules() {
    if (_cf.getCable() != null) {
      warnUnusedStructure(
          _cf.getCable().getDocsisPolicyRules(), CiscoStructureType.DOCSIS_POLICY_RULE);
    }
  }

  private void warnUnusedIpAccessLists() {
    warnUnusedStructure(_extendedAccessLists, CiscoStructureType.IP_ACCESS_LIST_EXTENDED);
    warnUnusedStructure(_standardAccessLists, CiscoStructureType.IP_ACCESS_LIST_STANDARD);
  }

  private void warnUnusedIpv6AccessLists() {
    warnUnusedStructure(_extendedIpv6AccessLists, CiscoStructureType.IPV6_ACCESS_LIST_EXTENDED);
    warnUnusedStructure(_standardIpv6AccessLists, CiscoStructureType.IPV6_ACCESS_LIST_STANDARD);
  }

  private void warnUnusedKeyrings() {
    warnUnusedStructure(_keyrings, CiscoStructureType.KEYRING);
  }

  private void warnUnusedPeerGroups() {
    if (_unusedPeerGroups != null) {
      _unusedPeerGroups.forEach(
          (name, line) -> {
            unused(CiscoStructureType.BGP_PEER_GROUP, name, line);
          });
    }
  }

  private void warnUnusedPeerSessions() {
    if (_unusedPeerSessions != null) {
      _unusedPeerSessions.forEach(
          (name, line) -> {
            unused(CiscoStructureType.BGP_PEER_SESSION, name, line);
          });
    }
  }

  private void warnUnusedServiceClasses() {
    if (_cf.getCable() != null) {
      warnUnusedStructure(_cf.getCable().getServiceClasses(), CiscoStructureType.SERVICE_CLASS);
    }
  }

  public Map<String, NetworkObjectGroup> getNetworkObjectGroups() {
    return _networkObjectGroups;
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
