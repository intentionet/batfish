package org.batfish.representation.cisco;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.common.util.CommonUtil.toImmutableSortedMap;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.datamodel.Interface.computeInterfaceType;
import static org.batfish.datamodel.Interface.isRealInterfaceName;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.representation.cisco.CiscoConversions.convertCryptoMapSet;
import static org.batfish.representation.cisco.CiscoConversions.generateAggregateRoutePolicy;
import static org.batfish.representation.cisco.CiscoConversions.resolveIsakmpProfileIfaceNames;
import static org.batfish.representation.cisco.CiscoConversions.resolveKeyringIfaceNames;
import static org.batfish.representation.cisco.CiscoConversions.resolveTunnelIfaceNames;
import static org.batfish.representation.cisco.CiscoConversions.suppressSummarizedPrefixes;
import static org.batfish.representation.cisco.CiscoConversions.toCommunityList;
import static org.batfish.representation.cisco.CiscoConversions.toIkePhase1Key;
import static org.batfish.representation.cisco.CiscoConversions.toIkePhase1Policy;
import static org.batfish.representation.cisco.CiscoConversions.toIkePhase1Proposal;
import static org.batfish.representation.cisco.CiscoConversions.toIpAccessList;
import static org.batfish.representation.cisco.CiscoConversions.toIpSpace;
import static org.batfish.representation.cisco.CiscoConversions.toIpsecPeerConfig;
import static org.batfish.representation.cisco.CiscoConversions.toIpsecPhase2Policy;
import static org.batfish.representation.cisco.CiscoConversions.toIpsecPhase2Proposal;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GeneratedRoute6;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPhase2Policy;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.Line;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Mlag;
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
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.eigrp.EigrpInterfaceSettings;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
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
import org.batfish.datamodel.tracking.TrackMethod;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.vendor_family.cisco.Aaa;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthentication;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthenticationLogin;
import org.batfish.datamodel.vendor_family.cisco.CiscoFamily;
import org.batfish.representation.cisco.CiscoAsaNat.Section;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.representation.cisco.eos.AristaEosVxlan;
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
            DestinationNetwork.instance(),
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

  private static final IpAccessListLine ACL_LINE_EXISTING_CONNECTION =
      new IpAccessListLine(
          LineAction.PERMIT,
          new MatchHeaderSpace(
              HeaderSpace.builder().setStates(ImmutableList.of(FlowState.ESTABLISHED)).build()),
          "~EXISTING_CONNECTION~");

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

  public static String computeIcmpObjectGroupAclName(String name) {
    return String.format("~ICMP_OBJECT_GROUP~%s~", name);
  }

  /**
   * Computes a mapping of interface names to the primary {@link Ip} owned by each of the interface.
   * Filters out the interfaces having no primary {@link InterfaceAddress}
   */
  private static Map<String, Ip> computeInterfaceOwnedPrimaryIp(Map<String, Interface> interfaces) {
    return interfaces.entrySet().stream()
        .filter(e -> Objects.nonNull(e.getValue().getAddress()))
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getAddress().getIp()));
  }

  public static String computeProtocolObjectGroupAclName(String name) {
    return String.format("~PROTOCOL_OBJECT_GROUP~%s~", name);
  }

  public static String computeServiceObjectAclName(String name) {
    return String.format("~SERVICE_OBJECT~%s~", name);
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

  private final Map<String, CryptoMapSet> _cryptoMapSets;

  private final List<Ip> _dhcpRelayServers;

  private NavigableSet<String> _dnsServers;

  private String _dnsSourceInterface;

  private String _domainName;

  private Map<String, VlanTrunkGroup> _eosVlanTrunkGroups;

  private AristaEosVxlan _eosVxlan;

  @Nullable private MlagConfiguration _eosMlagConfiguration;

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

  private final Map<Integer, IsakmpPolicy> _isakmpPolicies;

  private final Map<String, IsakmpProfile> _isakmpProfiles;

  private final Map<String, Keyring> _keyrings;

  private final Map<String, MacAccessList> _macAccessLists;

  private final Map<String, NatPool> _natPools;

  private final Map<String, IcmpTypeObjectGroup> _icmpTypeObjectGroups;

  private final Map<String, IntegerSpace> _namedVlans;

  private final Set<String> _natInside;

  private final Set<String> _natOutside;

  private final List<CiscoAsaNat> _ciscoAsaNats;

  private final List<CiscoIosNat> _ciscoIosNats;

  private final Map<String, NetworkObjectGroup> _networkObjectGroups;

  private final Map<String, NetworkObjectInfo> _networkObjectInfos;

  private final Map<String, NetworkObject> _networkObjects;

  private String _ntpSourceInterface;

  private CiscoNxBgpGlobalConfiguration _nxBgpGlobalConfiguration;

  private final Map<String, ObjectGroup> _objectGroups;

  private final Map<String, Prefix6List> _prefix6Lists;

  private final Map<String, PrefixList> _prefixLists;

  private final Map<String, ProtocolObjectGroup> _protocolObjectGroups;

  private final Map<String, RouteMap> _routeMaps;

  private final Map<String, RoutePolicy> _routePolicies;

  /**
   * Maps zone names to integers. Only includes zones that were created for security levels. In
   * effect, the reverse of computeSecurityLevelZoneName.
   */
  private final Map<String, Integer> _securityLevels;

  private boolean _sameSecurityTrafficInter;

  private boolean _sameSecurityTrafficIntra;

  private final Map<String, ServiceObject> _serviceObjects;

  private SnmpServer _snmpServer;

  private String _snmpSourceInterface;

  private boolean _spanningTreePortfastDefault;

  private final Map<String, StandardAccessList> _standardAccessLists;

  private final Map<String, StandardCommunityList> _standardCommunityLists;

  private final Map<String, StandardIpv6AccessList> _standardIpv6AccessLists;

  private NavigableSet<String> _tacacsServers;

  private String _tacacsSourceInterface;

  private ConfigurationFormat _vendor;

  private final Map<String, Vrf> _vrfs;

  private final SortedMap<String, VrrpInterface> _vrrpGroups;

  private final Map<String, ServiceObjectGroup> _serviceObjectGroups;

  private final Map<String, Map<String, SecurityZonePair>> _securityZonePairs;

  private final Map<String, SecurityZone> _securityZones;

  private final Map<String, TrackMethod> _trackingGroups;

  private Map<String, NamedCommunitySet> _communitySets;

  // initialized when needed
  private Multimap<Integer, Interface> _interfacesBySecurityLevel;

  public CiscoConfiguration() {
    _asPathAccessLists = new TreeMap<>();
    _asPathSets = new TreeMap<>();
    _cf = new CiscoFamily();
    _communitySets = new TreeMap<>();
    _cryptoMapSets = new HashMap<>();
    _dhcpRelayServers = new ArrayList<>();
    _dnsServers = new TreeSet<>();
    _eosVlanTrunkGroups = new HashMap<>();
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
    _icmpTypeObjectGroups = new TreeMap<>();
    _namedVlans = new HashMap<>();
    _natInside = new TreeSet<>();
    _natOutside = new TreeSet<>();
    _ciscoAsaNats = new ArrayList<>();
    _ciscoIosNats = new ArrayList<>();
    _networkObjectGroups = new TreeMap<>();
    _networkObjectInfos = new TreeMap<>();
    _networkObjects = new TreeMap<>();
    _nxBgpGlobalConfiguration = new CiscoNxBgpGlobalConfiguration();
    _objectGroups = new TreeMap<>();
    _prefixLists = new TreeMap<>();
    _prefix6Lists = new TreeMap<>();
    _protocolObjectGroups = new TreeMap<>();
    _routeMaps = new TreeMap<>();
    _routePolicies = new TreeMap<>();
    _securityLevels = new TreeMap<>();
    _securityZonePairs = new TreeMap<>();
    _securityZones = new TreeMap<>();
    _serviceObjectGroups = new TreeMap<>();
    _serviceObjects = new TreeMap<>();
    _standardAccessLists = new TreeMap<>();
    _standardIpv6AccessLists = new TreeMap<>();
    _standardCommunityLists = new TreeMap<>();
    _tacacsServers = new TreeSet<>();
    _trackingGroups = new TreeMap<>();
    _vrfs = new TreeMap<>();
    _vrfs.put(Configuration.DEFAULT_VRF_NAME, new Vrf(Configuration.DEFAULT_VRF_NAME));
    _vrrpGroups = new TreeMap<>();
  }

  private void applyVrrp(Configuration c) {
    _vrrpGroups.forEach(
        (ifaceName, vrrpInterface) -> {
          org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
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
    we.setPreStatements(
        ImmutableList.of(Statements.SetWriteIntermediateBgpAttributes.toStaticStatement()));
    we.setPostStatements(
        ImmutableList.of(Statements.UnsetWriteIntermediateBgpAttributes.toStaticStatement()));
    we.setPostTrueStatements(
        ImmutableList.of(
            Statements.SetReadIntermediateBgpAttributes.toStaticStatement(),
            new SetOrigin(new LiteralOrigin(originType, null))));
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

  public Map<String, CryptoMapSet> getCryptoMapSets() {
    return _cryptoMapSets;
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

  @Nonnull
  public Map<String, VlanTrunkGroup> getEosVlanTrunkGroups() {
    return _eosVlanTrunkGroups;
  }

  public AristaEosVxlan getEosVxlan() {
    return _eosVxlan;
  }

  @Nullable
  public MlagConfiguration getEosMlagConfiguration() {
    return _eosMlagConfiguration;
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
        return _interfaces.values().stream()
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

  public Map<Integer, IsakmpPolicy> getIsakmpPolicies() {
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

  public Map<String, IntegerSpace> getNamedVlans() {
    return _namedVlans;
  }

  public Set<String> getNatInside() {
    return _natInside;
  }

  public Set<String> getNatOutside() {
    return _natOutside;
  }

  public List<CiscoAsaNat> getCiscoAsaNats() {
    return _ciscoAsaNats;
  }

  public List<CiscoIosNat> getCiscoIosNats() {
    return _ciscoIosNats;
  }

  private String getNewInterfaceName(Interface iface) {
    return firstNonNull(iface.getAlias(), iface.getName());
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

  @Nullable
  private String getSecurityZoneName(Interface iface) {
    String zoneName = iface.getSecurityZone();
    if (zoneName == null) {
      return null;
    }
    SecurityZone securityZone = _securityZones.get(zoneName);
    if (securityZone == null) {
      return null;
    }
    return zoneName;
  }

  @Nullable
  private String getSecurityLevelZoneName(Interface iface) {
    Integer level = iface.getSecurityLevel();
    if (level == null) {
      return null;
    }
    return computeSecurityLevelZoneName(level);
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

  public void setEosMlagConfiguration(@Nullable MlagConfiguration eosMlagConfiguration) {
    _eosMlagConfiguration = eosMlagConfiguration;
  }

  public void setEosVxlan(AristaEosVxlan eosVxlan) {
    _eosVxlan = eosVxlan;
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
    checkNotNull(hostname, "'hostname' cannot be null");
    _hostname = hostname.toLowerCase();
  }

  public void setNtpSourceInterface(String ntpSourceInterface) {
    _ntpSourceInterface = ntpSourceInterface;
  }

  public void setSameSecurityTrafficInter(boolean permit) {
    _sameSecurityTrafficInter = permit;
  }

  public void setSameSecurityTrafficIntra(boolean permit) {
    _sameSecurityTrafficIntra = permit;
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
      newBgpProcess.setMultipathEbgp(ipv4af.getMaximumPathsEbgp() > 1);
      newBgpProcess.setMultipathIbgp(ipv4af.getMaximumPathsIbgp() > 1);
    }

    // Next we build up the BGP common export policy.
    RoutingPolicy bgpCommonExportPolicy =
        new RoutingPolicy(computeBgpCommonExportPolicyName(vrfName), c);
    c.getRoutingPolicies().put(bgpCommonExportPolicy.getName(), bgpCommonExportPolicy);

    // 1. If there are any ipv4 summary only networks, do not export the more specific routes.
    if (ipv4af != null) {
      Stream<Prefix> summaryOnlyNetworks =
          ipv4af.getAggregateNetworks().entrySet().stream()
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
            GeneratedRoute.builder()
                .setNetwork(prefix)
                .setAdmin(CISCO_AGGREGATE_ROUTE_ADMIN_COST)
                .setGenerationPolicy(generatedPolicy.getName())
                .setDiscard(true);

        // Conditions to generate this route
        List<BooleanExpr> generateAggregateConditions = new ArrayList<>();
        generateAggregateConditions.add(
            new MatchPrefixSet(
                DestinationNetwork.instance(),
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
                            DestinationNetwork.instance(), new ExplicitPrefixSet(exportSpace)),
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
          ipv6af.getIpv6Networks().keySet().stream()
              .map(p6 -> new Route6FilterLine(LineAction.PERMIT, Prefix6Range.fromPrefix6(p6)))
              .collect(ImmutableList.toImmutableList());
      Route6FilterList localFilter6 =
          new Route6FilterList("~BGP_NETWORK6_NETWORKS_FILTER:" + vrfName + "~", lines);
      c.getRoute6FilterLists().put(localFilter6.getName(), localFilter6);
    }

    // Before we process any neighbors, execute the inheritance.
    nxBgpGlobal.doInherit(_w);

    // Process active neighbors first.
    Map<Prefix, BgpActivePeerConfig> activeNeighbors =
        CiscoNxConversions.getNeighbors(c, v, newBgpProcess, nxBgpGlobal, nxBgpVrf, _w);
    newBgpProcess.setNeighbors(ImmutableSortedMap.copyOf(activeNeighbors));

    // Process passive neighbors next
    Map<Prefix, BgpPassivePeerConfig> passiveNeighbors =
        CiscoNxConversions.getPassiveNeighbors(c, v, newBgpProcess, nxBgpGlobal, nxBgpVrf, _w);
    newBgpProcess.setPassiveNeighbors(ImmutableSortedMap.copyOf(passiveNeighbors));

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
    if (firstNonNull(proc.getMaximumPathsEbgp(), 0) > 1) {
      multipathEbgp = true;
    }
    if (firstNonNull(proc.getMaximumPathsIbgp(), 0) > 1) {
      multipathIbgp = true;
    }
    newBgpProcess.setMultipathEbgp(multipathEbgp);
    newBgpProcess.setMultipathIbgp(multipathIbgp);

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
        proc.getAggregateNetworks().entrySet().stream()
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
          GeneratedRoute.builder()
              .setNetwork(prefix)
              .setAdmin(CISCO_AGGREGATE_ROUTE_ADMIN_COST)
              .setGenerationPolicy(generatedPolicy.getName())
              .setDiscard(true);

      // Conditions to generate this route
      List<BooleanExpr> generateAggregateConditions = new ArrayList<>();
      generateAggregateConditions.add(
          new MatchPrefixSet(
              DestinationNetwork.instance(),
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
      If currentGeneratedRouteConditional =
          new If(
              new MatchPrefix6Set(
                  new DestinationNetwork6(),
                  new ExplicitPrefix6Set(
                      new Prefix6Space(
                          Collections.singleton(new Prefix6Range(prefix6, prefixRange))))),
              ImmutableList.of(Statements.ReturnTrue.toStaticStatement()));
      currentGeneratedRoutePolicy.getStatements().add(currentGeneratedRouteConditional);
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
                }
              }
              BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weExpr, OriginType.IGP);
              Conjunction exportNetworkConditions = new Conjunction();
              PrefixSpace space = new PrefixSpace();
              space.addPrefix(prefix);
              newBgpProcess.addToOriginationSpace(space);
              exportNetworkConditions
                  .getConjuncts()
                  .add(
                      new MatchPrefixSet(
                          DestinationNetwork.instance(), new ExplicitPrefixSet(space)));
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
                        LineAction.PERMIT, prefix6, new SubRange(prefixLen, prefixLen));
                localFilter6.addLine(line);
                String mapName = bgpNetwork6.getRouteMapName();
                if (mapName != null) {
                  RouteMap routeMap = _routeMaps.get(mapName);
                  if (routeMap != null) {
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
          peerExportConditions.getConjuncts().add(new CallExpr(outboundRouteMapName));
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

        defaultRoute = GeneratedRoute.builder();
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
      String description = lpg.getDescription();
      if (lpg.getActive() && !lpg.getShutdown()) {
        if (lpg.getRemoteAs() == null) {
          _w.redFlag("No remote-as set for peer: " + lpg.getName());
          continue;
        }
        Long pgLocalAs = lpg.getLocalAs();
        long localAs = pgLocalAs != null ? pgLocalAs : proc.getProcnum();

        BgpPeerConfig.Builder<?, ?> newNeighborBuilder;
        if (lpg instanceof IpBgpPeerGroup) {
          IpBgpPeerGroup ipg = (IpBgpPeerGroup) lpg;
          newNeighborBuilder =
              BgpActivePeerConfig.builder()
                  .setPeerAddress(ipg.getIp())
                  .setRemoteAs(lpg.getRemoteAs());
        } else if (lpg instanceof DynamicIpBgpPeerGroup) {
          DynamicIpBgpPeerGroup dpg = (DynamicIpBgpPeerGroup) lpg;
          // Sort the remove AS numbers for consistent refs.
          SortedSet<Long> asns =
              ImmutableSortedSet.<Long>naturalOrder()
                  .add(dpg.getRemoteAs())
                  .addAll(firstNonNull(dpg.getAlternateAs(), ImmutableList.of()))
                  .build();
          newNeighborBuilder =
              BgpPassivePeerConfig.builder()
                  .setPeerPrefix(dpg.getPrefix())
                  .setRemoteAs(ImmutableList.copyOf(asns));
        } else if (lpg instanceof Ipv6BgpPeerGroup || lpg instanceof DynamicIpv6BgpPeerGroup) {
          // TODO: implement ipv6 bgp neighbors
          continue;
        } else {
          throw new VendorConversionException("Invalid BGP leaf neighbor type");
        }
        newNeighborBuilder.setBgpProcess(newBgpProcess);

        newNeighborBuilder.setAdditionalPathsReceive(lpg.getAdditionalPathsReceive());
        newNeighborBuilder.setAdditionalPathsSelectAll(lpg.getAdditionalPathsSelectAll());
        newNeighborBuilder.setAdditionalPathsSend(lpg.getAdditionalPathsSend());
        newNeighborBuilder.setAdvertiseInactive(lpg.getAdvertiseInactive());
        newNeighborBuilder.setAllowLocalAsIn(lpg.getAllowAsIn());
        newNeighborBuilder.setAllowRemoteAsOut(
            firstNonNull(lpg.getDisablePeerAsCheck(), Boolean.TRUE));
        newNeighborBuilder.setRouteReflectorClient(lpg.getRouteReflectorClient());
        newNeighborBuilder.setClusterId(clusterId.asLong());
        newNeighborBuilder.setDefaultMetric(defaultMetric);
        newNeighborBuilder.setDescription(description);
        newNeighborBuilder.setEbgpMultihop(lpg.getEbgpMultihop());
        if (defaultRoute != null) {
          newNeighborBuilder.setGeneratedRoutes(ImmutableSet.of(defaultRoute.build()));
        }
        newNeighborBuilder.setGroup(lpg.getGroupName());
        if (importPolicy != null) {
          newNeighborBuilder.setImportPolicy(inboundRouteMapName);
        }
        newNeighborBuilder.setLocalAs(localAs);
        newNeighborBuilder.setLocalIp(updateSource);
        newNeighborBuilder.setExportPolicy(peerExportPolicyName);
        newNeighborBuilder.setSendCommunity(lpg.getSendCommunity());
        newNeighborBuilder.build();
      }
    }
    return newBgpProcess;
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
      String ifaceName, Interface iface, Map<String, IpAccessList> ipAccessLists, Configuration c) {
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(
            ifaceName, c, computeInterfaceType(iface.getName(), c.getConfigurationFormat()));
    if (newIface.getInterfaceType() == InterfaceType.VLAN) {
      newIface.setVlan(CommonUtil.getInterfaceVlanNumber(ifaceName));
    }
    String vrfName = iface.getVrf();
    Vrf vrf = _vrfs.computeIfAbsent(vrfName, Vrf::new);
    newIface.setDescription(iface.getDescription());
    newIface.setActive(iface.getActive());
    newIface.setChannelGroup(iface.getChannelGroup());
    newIface.setCryptoMap(iface.getCryptoMap());
    newIface.setHsrpGroups(
        CommonUtil.toImmutableMap(
            iface.getHsrpGroups(), Entry::getKey, e -> CiscoConversions.toHsrpGroup(e.getValue())));
    newIface.setHsrpVersion(iface.getHsrpVersion());
    newIface.setAutoState(iface.getAutoState());
    newIface.setVrf(c.getVrfs().get(vrfName));
    newIface.setSpeed(
        firstNonNull(
            iface.getSpeed(),
            Interface.getDefaultSpeed(iface.getName(), c.getConfigurationFormat())));
    newIface.setBandwidth(
        firstNonNull(
            iface.getBandwidth(),
            newIface.getSpeed(),
            Interface.getDefaultBandwidth(iface.getName(), c.getConfigurationFormat())));
    if (iface.getDhcpRelayClient()) {
      newIface.setDhcpRelayAddresses(_dhcpRelayServers);
    } else {
      newIface.setDhcpRelayAddresses(ImmutableList.copyOf(iface.getDhcpRelayAddresses()));
    }
    newIface.setMlagId(iface.getMlagId());
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

    if (!iface.getOspfShutdown()) {
      OspfProcess proc = vrf.getOspfProcess();
      if (proc != null) {
        if (firstNonNull(
            iface.getOspfPassive(),
            proc.getPassiveInterfaceDefault()
                ^ proc.getNonDefaultInterfaces().contains(ifaceName))) {
          proc.getPassiveInterfaces().add(ifaceName);
        }
        newIface.setOspfAreaName(iface.getOspfArea());
        newIface.setOspfCost(iface.getOspfCost());
        newIface.setOspfDeadInterval(iface.getOspfDeadInterval());
        newIface.setOspfHelloMultiplier(iface.getOspfHelloMultiplier());
      } else if (iface.getOspfArea() != null
          || iface.getOspfCost() != null
          || iface.getOspfPassive() != null) {
        _w.redFlag(
            "Interface: '" + ifaceName + "' contains OSPF settings, but there is no OSPF process");
      }
    }

    EigrpProcess eigrpProcess = null;
    if (iface.getAddress() != null) {
      for (EigrpProcess process : vrf.getEigrpProcesses().values()) {
        if (process.getNetworks().contains(iface.getAddress().getPrefix())) {
          // Found a process on interface
          if (eigrpProcess != null) {
            // Cisco does not recommend running multiple EIGRP autonomous systems on the same
            // interface
            _w.redFlag("Interface: '" + iface.getName() + "' matches multiple EIGRP processes");
            break;
          }
          eigrpProcess = process;
        }
      }
    }
    // Let toEigrpProcess handle null asn failure
    if (eigrpProcess != null && eigrpProcess.getAsn() != null) {
      boolean passive =
          eigrpProcess
              .getInterfacePassiveStatus()
              .getOrDefault(getNewInterfaceName(iface), eigrpProcess.getPassiveInterfaceDefault());

      // For bandwidth/delay, defaults are separate from actuals to inform metric calculations
      EigrpMetric metric =
          EigrpMetric.builder()
              .setBandwidth(iface.getBandwidth())
              .setMode(eigrpProcess.getMode())
              .setDefaultBandwidth(
                  Interface.getDefaultBandwidth(iface.getName(), c.getConfigurationFormat()))
              .setDefaultDelay(
                  Interface.getDefaultDelay(iface.getName(), c.getConfigurationFormat()))
              .setDelay(iface.getDelay())
              .build();

      newIface.setEigrp(
          EigrpInterfaceSettings.builder()
              .setAsn(eigrpProcess.getAsn())
              .setEnabled(true)
              .setMetric(metric)
              .setPassive(passive)
              .build());
      if (newIface.getEigrp() == null) {
        _w.redFlag("Interface: '" + iface.getName() + "' failed to set EIGRP settings");
      }
    }
    if (eigrpProcess == null && iface.getDelay() != null) {
      _w.redFlag(
          "Interface: '"
              + iface.getName()
              + "' contains EIGRP settings but is not part of an EIGRP process");
    }

    boolean level1 = false;
    boolean level2 = false;
    IsisProcess isisProcess = vrf.getIsisProcess();
    if (isisProcess != null && iface.getIsisInterfaceMode() != IsisInterfaceMode.UNSET) {
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
      IsisInterfaceLevelSettings levelSettings =
          IsisInterfaceLevelSettings.builder()
              .setCost(iface.getIsisCost())
              .setMode(iface.getIsisInterfaceMode())
              .build();
      if (level1) {
        isisInterfaceSettingsBuilder.setLevel1(levelSettings);
      }
      if (level2) {
        isisInterfaceSettingsBuilder.setLevel2(levelSettings);
      }
      newIface.setIsis(isisInterfaceSettingsBuilder.build());
    }

    // switch settings
    newIface.setAccessVlan(iface.getAccessVlan());

    if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
      newIface.setNativeVlan(firstNonNull(iface.getNativeVlan(), 1));
    }

    newIface.setSwitchportMode(iface.getSwitchportMode());
    SwitchportEncapsulationType encapsulation = iface.getSwitchportTrunkEncapsulation();
    if (encapsulation == null) { // no encapsulation set, so use default..
      // TODO: check if this is OK
      encapsulation = SwitchportEncapsulationType.DOT1Q;
    }
    newIface.setSwitchportTrunkEncapsulation(encapsulation);
    if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
      /*
       * Compute allowed VLANs:
       * - If allowed VLANs are set, honor them;
       * - Otherwise prune allowed VLANs based on configured trunk groups (if any).
       *
       * https://www.arista.com/en/um-eos/eos-section-19-3-vlan-configuration-procedures#ww1152330
       */
      if (!Interface.ALL_VLANS.equals(iface.getAllowedVlans())
          || iface.getVlanTrunkGroups().isEmpty()) {
        newIface.setAllowedVlans(iface.getAllowedVlans());
      } else {
        newIface.setAllowedVlans(
            iface.getVlanTrunkGroups().stream()
                .map(_eosVlanTrunkGroups::get)
                .map(VlanTrunkGroup::getVlans)
                .reduce(IntegerSpace::union)
                .get());
      }
    }

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

    /*
     * NAT rules are specified at the top level, but are applied as incoming transformations on the
     * outside interface (outside-to-inside) and outgoing transformations on the outside interface
     * (inside-to-outside)
     *
     * Currently, only static NATs have both incoming and outgoing transformations
     */

    List<CiscoAsaNat> ciscoAsaNats = firstNonNull(_ciscoAsaNats, ImmutableList.of());
    List<CiscoIosNat> ciscoIosNats = firstNonNull(_ciscoIosNats, ImmutableList.of());
    List<AristaDynamicSourceNat> aristaDynamicSourceNats =
        firstNonNull(iface.getAristaNats(), ImmutableList.of());
    int natTypes =
        (aristaDynamicSourceNats.isEmpty() ? 0 : 1)
            + (ciscoAsaNats.isEmpty() ? 0 : 1)
            + (ciscoIosNats.isEmpty() ? 0 : 1);
    if (natTypes > 1) {
      _w.redFlag("Multiple NAT types should not be present in same configuration.");
    } else if (!aristaDynamicSourceNats.isEmpty()) {
      generateAristaDynamicSourceNats(newIface, aristaDynamicSourceNats);
    } else if (!ciscoAsaNats.isEmpty()) {
      generateCiscoAsaNatTransformations(ifaceName, newIface, ciscoAsaNats);
    } else if (!ciscoIosNats.isEmpty()) {
      generateCiscoIosNatTransformations(ifaceName, newIface, ipAccessLists, c);
    }

    String routingPolicyName = iface.getRoutingPolicy();
    if (routingPolicyName != null) {
      newIface.setRoutingPolicy(routingPolicyName);
    }

    if (_vendor == ConfigurationFormat.CISCO_ASA) {
      newIface.setPostTransformationIncomingFilter(newIface.getIncomingFilter());
      newIface.setPreTransformationOutgoingFilter(newIface.getOutgoingFilter());
      newIface.setIncomingFilter(null);
      newIface.setOutgoingFilter((IpAccessList) null);
    }
    return newIface;
  }

  private void generateAristaDynamicSourceNats(
      org.batfish.datamodel.Interface newIface,
      List<AristaDynamicSourceNat> aristaDynamicSourceNats) {
    Ip interfaceIp = newIface.getAddress().getIp();
    Transformation next = null;
    for (AristaDynamicSourceNat nat : Lists.reverse(aristaDynamicSourceNats)) {
      next = nat.toTransformation(interfaceIp, _natPools, next).orElse(next);
    }
    newIface.setOutgoingTransformation(next);
  }

  private void generateCiscoAsaNatTransformations(
      String ifaceName, org.batfish.datamodel.Interface newIface, List<CiscoAsaNat> ciscoAsaNats) {

    if (!ciscoAsaNats.stream().map(CiscoAsaNat::getSection).allMatch(Section.OBJECT::equals)) {
      _w.unimplemented("No support for Twice NAT");
    }

    // ASA places incoming and outgoing object NATs as transformations on the outside interface.
    // Each NAT rule specifies an outside interface or ANY_INTERFACE
    SortedSet<CiscoAsaNat> objectNats =
        ciscoAsaNats.stream()
            .filter(nat -> nat.getSection().equals(Section.OBJECT))
            .filter(
                nat ->
                    nat.getOutsideInterface().equals(CiscoAsaNat.ANY_INTERFACE)
                        || nat.getOutsideInterface().equals(ifaceName))
            .collect(Collectors.toCollection(TreeSet::new));

    newIface.setIncomingTransformation(
        objectNats.stream()
            .map(nat -> nat.toIncomingTransformation(_networkObjects, _w))
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toList(), CiscoAsaNatUtil::toTransformationChain)));

    newIface.setOutgoingTransformation(
        objectNats.stream()
            .map(nat -> nat.toOutgoingTransformation(_networkObjects, _w))
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toList(), CiscoAsaNatUtil::toTransformationChain)));
  }

  private void generateCiscoIosNatTransformations(
      String ifaceName,
      org.batfish.datamodel.Interface newIface,
      Map<String, IpAccessList> ipAccessLists,
      Configuration c) {
    List<CiscoIosNat> incomingNats = new ArrayList<>();
    List<CiscoIosNat> outgoingNats = new ArrayList<>();

    // Check if this is an outside interface
    if (getNatOutside().contains(ifaceName)) {
      incomingNats.addAll(getCiscoIosNats());
      outgoingNats.addAll(getCiscoIosNats());
    }

    // Convert the IOS NATs to a mapping of transformations. Each field (source or destination)
    // can be modified independently but not jointly. A single CiscoIosNat can represent an incoming
    // NAT, an outgoing NAT, or both.

    Map<CiscoIosNat, Transformation.Builder> convertedIncomingNats =
        incomingNats.stream()
            .map(nat -> new SimpleEntry<>(nat, nat.toIncomingTransformation(_natPools)))
            .filter(entry -> entry.getValue().isPresent())
            .collect(Collectors.toMap(SimpleEntry::getKey, entry -> entry.getValue().get()));
    if (!convertedIncomingNats.isEmpty()) {
      newIface.setIncomingTransformation(
          CiscoIosNatUtil.toIncomingTransformationChain(convertedIncomingNats));
    }

    Map<CiscoIosNat, Transformation.Builder> convertedOutgoingNats =
        outgoingNats.stream()
            .map(
                nat ->
                    new SimpleEntry<>(
                        nat,
                        nat.toOutgoingTransformation(ipAccessLists, _natPools, getNatInside(), c)))
            .filter(entry -> entry.getValue().isPresent())
            .collect(Collectors.toMap(SimpleEntry::getKey, entry -> entry.getValue().get()));
    if (!convertedOutgoingNats.isEmpty()) {
      newIface.setOutgoingTransformation(
          CiscoIosNatUtil.toOutgoingTransformationChain(convertedOutgoingNats));
    }
  }

  private void applyZoneFilter(
      Interface iface, org.batfish.datamodel.Interface newIface, Configuration c) {
    String zoneName = firstNonNull(getSecurityZoneName(iface), getSecurityLevelZoneName(iface));
    if (zoneName == null) {
      return;
    }
    String zoneOutgoingAclName = computeZoneOutgoingAclName(zoneName);
    IpAccessList zoneOutgoingAcl = c.getIpAccessLists().get(zoneOutgoingAclName);
    if (zoneOutgoingAcl == null) {
      return;
    }
    String oldOutgoingFilterName = newIface.getOutgoingFilterName();
    if (oldOutgoingFilterName == null && allowsIntraZoneTraffic(zoneName)) {
      // No interface outbound filter and no interface-specific handling
      newIface.setOutgoingFilter(zoneOutgoingAcl);
      return;
    }

    // Construct a new ACL that combines filters, i.e. 1 AND (2 OR 3)
    // 1) the interface outbound filter, if it exists
    // 2) the zone filter
    // 3) interface-specific zone filtering, if necessary

    AclLineMatchExpr ifaceFilter = FalseExpr.INSTANCE;
    if (_sameSecurityTrafficIntra && !_sameSecurityTrafficInter) {
      ifaceFilter =
          new MatchSrcInterface(
              ImmutableList.of(newIface.getName()), "Allow traffic received on this interface");
    } else if (!_sameSecurityTrafficIntra && _sameSecurityTrafficInter) {
      ifaceFilter =
          new MatchSrcInterface(
              _interfacesBySecurityLevel.get(iface.getSecurityLevel()).stream()
                  .filter(other -> !other.equals(iface))
                  .map(this::getNewInterfaceName)
                  .collect(ImmutableList.toImmutableList()),
              String.format(
                  "Allow traffic received on other interfaces with security level %d",
                  iface.getSecurityLevel()));
    }

    String combinedOutgoingAclName = computeCombinedOutgoingAclName(newIface.getName());
    IpAccessList combinedOutgoingAcl;
    ImmutableList<AclLineMatchExpr> securityFilters =
        ImmutableList.of(new PermittedByAcl(zoneOutgoingAclName), ifaceFilter);

    if (oldOutgoingFilterName != null) {
      combinedOutgoingAcl =
          IpAccessList.builder()
              .setOwner(c)
              .setName(combinedOutgoingAclName)
              .setLines(
                  ImmutableList.of(
                      IpAccessListLine.accepting()
                          .setMatchCondition(
                              new AndMatchExpr(
                                  ImmutableList.of(
                                      new OrMatchExpr(securityFilters),
                                      new PermittedByAcl(oldOutgoingFilterName)),
                                  String.format(
                                      "Permit if permitted by policy for zone '%s' and permitted by"
                                          + " outgoing filter '%s'",
                                      zoneName, oldOutgoingFilterName)))
                          .build()))
              .build();
    } else {
      combinedOutgoingAcl =
          IpAccessList.builder()
              .setOwner(c)
              .setName(combinedOutgoingAclName)
              .setLines(
                  ImmutableList.of(
                      IpAccessListLine.accepting()
                          .setMatchCondition(
                              new OrMatchExpr(
                                  securityFilters,
                                  String.format(
                                      "Permit if permitted by policy for zone '%s'", zoneName)))
                          .build()))
              .build();
    }
    newIface.setOutgoingFilter(combinedOutgoingAcl);
  }

  public static String computeCombinedOutgoingAclName(String interfaceName) {
    return String.format("~COMBINED_OUTGOING_ACL~%s~", interfaceName);
  }

  // For testing.
  If convertOspfRedistributionPolicy(OspfRedistributionPolicy policy, OspfProcess proc) {
    RoutingProtocol protocol = policy.getSourceProtocol();
    // All redistribution must match the specified protocol.
    Conjunction ospfExportConditions = new Conjunction();
    if (protocol == RoutingProtocol.EIGRP) {
      ospfExportConditions
          .getConjuncts()
          .add(
              new Disjunction(
                  ImmutableList.of(
                      new MatchProtocol(RoutingProtocol.EIGRP),
                      new MatchProtocol(RoutingProtocol.EIGRP_EX))));
    } else {
      ospfExportConditions.getConjuncts().add(new MatchProtocol(protocol));
    }

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
        org.batfish.datamodel.ospf.OspfProcess.builder()
            .setReferenceBandwidth(proc.getReferenceBandwidth())
            .build();
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
    Map<Long, OspfArea.Builder> areas = new HashMap<>();
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
      org.batfish.datamodel.Interface iface = e.getValue();
      InterfaceAddress interfaceAddress = iface.getAddress();
      Long areaNum = iface.getOspfAreaName();
      // OSPF area number was not configured on the interface itself, so infer from IP address.
      if (areaNum == null) {
        if (interfaceAddress == null) {
          // This interface has no IP address configured, cannot be in an OSPF area.
          continue;
        }
        for (OspfNetwork network : networks) {
          Prefix networkPrefix = network.getPrefix();
          Ip networkAddress = networkPrefix.getStartIp();
          Ip maskedInterfaceAddress =
              interfaceAddress.getIp().getNetworkAddress(networkPrefix.getPrefixLength());
          if (maskedInterfaceAddress.equals(networkAddress)) {
            // we have a longest prefix match
            areaNum = network.getArea();
            break;
          }
        }
      }
      if (areaNum == null) {
        continue;
      }
      String ifaceName = e.getKey();
      areas.computeIfAbsent(areaNum, areaNumber -> OspfArea.builder().setNumber(areaNumber));
      ImmutableSortedSet.Builder<String> newAreaInterfacesBuilder =
          areaInterfacesBuilders.computeIfAbsent(areaNum, n -> ImmutableSortedSet.naturalOrder());
      newAreaInterfacesBuilder.add(ifaceName);
      iface.setOspfEnabled(true);
      boolean passive = proc.getPassiveInterfaces().contains(iface.getName());
      iface.setOspfPassive(passive);
    }
    areaInterfacesBuilders.forEach(
        (areaNum, interfacesBuilder) ->
            areas.get(areaNum).addInterfaces(interfacesBuilder.build()));
    proc.getNssas()
        .forEach(
            (areaId, nssaSettings) -> {
              if (!areas.containsKey(areaId)) {
                return;
              }
              areas.get(areaId).setStubType(StubType.NSSA);
              areas.get(areaId).setNssaSettings(toNssaSettings(nssaSettings));
            });

    proc.getStubs()
        .forEach(
            (areaId, stubSettings) -> {
              if (!areas.containsKey(areaId)) {
                return;
              }
              areas.get(areaId).setStubType(StubType.STUB);
              areas.get(areaId).setStubSettings(toStubSettings(stubSettings));
            });

    // create summarization filters for inter-area routes
    for (Entry<Long, Map<Prefix, OspfAreaSummary>> e1 : proc.getSummaries().entrySet()) {
      long areaLong = e1.getKey();
      Map<Prefix, OspfAreaSummary> summaries = e1.getValue();
      OspfArea.Builder area = areas.get(areaLong);
      String summaryFilterName = "~OSPF_SUMMARY_FILTER:" + vrfName + ":" + areaLong + "~";
      RouteFilterList summaryFilter = new RouteFilterList(summaryFilterName);
      c.getRouteFilterLists().put(summaryFilterName, summaryFilter);
      if (area == null) {
        area = OspfArea.builder().setNumber(areaLong);
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
                LineAction.DENY,
                new IpWildcard(prefix),
                new SubRange(filterMinPrefixLength, Prefix.MAX_PREFIX_LENGTH)));
      }
      area.addSummaries(ImmutableSortedMap.copyOf(summaries));
      summaryFilter.addLine(
          new RouteFilterLine(
              LineAction.PERMIT,
              new IpWildcard(Prefix.ZERO),
              new SubRange(0, Prefix.MAX_PREFIX_LENGTH)));
    }
    newProcess.setAreas(toImmutableSortedMap(areas, Entry::getKey, e -> e.getValue().build()));

    // set pointers from interfaces to their parent areas
    newProcess
        .getAreas()
        .values()
        .forEach(
            area ->
                area.getInterfaces()
                    .forEach(
                        ifaceName ->
                            c.getVrfs()
                                .get(vrfName)
                                .getInterfaces()
                                .get(ifaceName)
                                .setOspfArea(area)));

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
          GeneratedRoute.Builder route = GeneratedRoute.builder();
          route.setNetwork(Prefix.ZERO);
          route.setAdmin(MAX_ADMINISTRATIVE_COST);
          route.setGenerationPolicy(defaultOriginateMapName);
          newProcess.addGeneratedRoute(route.build());
        }
      } else if (proc.getDefaultInformationOriginateAlways()) {
        useAggregateDefaultOnly = true;
        // add generated aggregate with no precondition
        GeneratedRoute.Builder route = GeneratedRoute.builder();
        route.setNetwork(Prefix.ZERO);
        route.setAdmin(MAX_ADMINISTRATIVE_COST);
        newProcess.addGeneratedRoute(route.build());
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

    Ip routerId = proc.getRouterId();
    if (routerId == null) {
      routerId = CiscoConversions.getHighestIp(oldConfig.getInterfaces());
      if (routerId == Ip.ZERO) {
        _w.redFlag("No candidates for OSPF router-id");
        return null;
      }
    }
    newProcess.setRouterId(routerId);

    // policies for redistributing routes
    ospfExportStatements.addAll(
        proc.getRedistributionPolicies().values().stream()
            .map(policy -> convertOspfRedistributionPolicy(policy, proc))
            .collect(Collectors.toList()));

    return newProcess;
  }

  @Nullable
  private Mlag toMlag(@Nullable MlagConfiguration mlag) {
    if (mlag == null || mlag.getDomainId() == null) {
      return null;
    }
    return Mlag.builder()
        .setId(mlag.getDomainId())
        .setPeerAddress(mlag.getPeerAddress())
        .setPeerInterface(mlag.getPeerLink())
        .setLocalInterface(mlag.getLocalInterface())
        .build();
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
      RipProcess proc, String vrfName, Configuration c) {
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
          GeneratedRoute.Builder route = GeneratedRoute.builder();
          route.setNetwork(Prefix.ZERO);
          route.setAdmin(MAX_ADMINISTRATIVE_COST);
          route.setGenerationPolicy(defaultOriginateMapName);
          newProcess.getGeneratedRoutes().add(route.build());
        }
      } else {
        // add generated aggregate with no precondition
        GeneratedRoute.Builder route = GeneratedRoute.builder();
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
        case PERMIT:
          matchStatements.add(Statements.ReturnTrue.toStaticStatement());
          break;

        case DENY:
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
        case PERMIT:
          if (continueStatement == null) {
            onMatchStatements.add(Statements.ExitAccept.toStaticStatement());
          } else {
            onMatchStatements.add(Statements.SetDefaultActionAccept.toStaticStatement());
            onMatchStatements.add(new CallStatement(continueTargetPolicy.getName()));
          }
          break;

        case DENY:
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
  public List<Configuration> toVendorIndependentConfigurations() {
    final Configuration c = new Configuration(_hostname, _vendor);
    c.getVendorFamily().setCisco(_cf);
    c.setDefaultInboundAction(LineAction.PERMIT);
    c.setDefaultCrossZoneAction(LineAction.PERMIT);
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
      if (list == null) {
        continue;
      }
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

    // convert standard/expanded community lists and community-sets to community lists
    for (StandardCommunityList scList : _standardCommunityLists.values()) {
      CommunityList cList = toCommunityList(scList);
      c.getCommunityLists().put(cList.getName(), cList);
    }
    for (ExpandedCommunityList ecList : _expandedCommunityLists.values()) {
      CommunityList cList = toCommunityList(ecList);
      c.getCommunityLists().put(cList.getName(), cList);
    }
    _communitySets.forEach(
        (name, communitySet) -> c.getCommunityLists().put(name, toCommunityList(communitySet)));

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
    for (StandardAccessList saList : _standardAccessLists.values()) {
      if (isAclUsedForRouting(saList.getName())) {
        RouteFilterList rfList = CiscoConversions.toRouteFilterList(saList);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      c.getIpAccessLists()
          .put(saList.getName(), toIpAccessList(saList.toExtendedAccessList(), this._objectGroups));
    }
    for (ExtendedAccessList eaList : _extendedAccessLists.values()) {
      if (isAclUsedForRouting(eaList.getName())) {
        RouteFilterList rfList = CiscoConversions.toRouteFilterList(eaList);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      IpAccessList ipaList = toIpAccessList(eaList, this._objectGroups);
      c.getIpAccessLists().put(ipaList.getName(), ipaList);
    }

    /*
     * Consolidate info about networkObjects
     * - Associate networkObjects with their Info
     * - Associate ASA Object NATs with their object (needed for sorting)
     * - Removes ASA Object NATs that were created without a valid network object
     */
    _networkObjectInfos.forEach(
        (name, info) -> {
          if (_networkObjects.containsKey(name)) {
            _networkObjects.get(name).setInfo(info);
          }
        });
    _ciscoAsaNats.removeIf(
        nat -> {
          if (nat.getSection() != Section.OBJECT) {
            return false;
          }
          String objectName = ((NetworkObjectAddressSpecifier) nat.getRealSource()).getName();
          NetworkObject object = _networkObjects.get(objectName);
          if (object == null) {
            // Network object has a NAT but no addresses
            _w.redFlag("Invalid reference for object NAT " + objectName + ".");
            return true;
          }
          if (object.getStart() == null) {
            // Unsupported network object type, already warned
            return true;
          }
          nat.setRealSourceObject(object);
          return false;
        });

    // convert each NetworkObject and NetworkObjectGroup to IpSpace
    _networkObjectGroups.forEach(
        (name, networkObjectGroup) -> c.getIpSpaces().put(name, toIpSpace(networkObjectGroup)));
    _networkObjectGroups
        .keySet()
        .forEach(
            name ->
                c.getIpSpaceMetadata()
                    .put(
                        name,
                        new IpSpaceMetadata(
                            name, CiscoStructureType.NETWORK_OBJECT_GROUP.getDescription())));
    _networkObjects.forEach(
        (name, networkObject) -> c.getIpSpaces().put(name, networkObject.toIpSpace()));
    _networkObjects
        .keySet()
        .forEach(
            name ->
                c.getIpSpaceMetadata()
                    .put(
                        name,
                        new IpSpaceMetadata(
                            name, CiscoStructureType.NETWORK_OBJECT.getDescription())));

    // convert each IcmpTypeGroup to IpAccessList
    _icmpTypeObjectGroups.forEach(
        (name, icmpTypeObjectGroups) ->
            c.getIpAccessLists()
                .put(computeIcmpObjectGroupAclName(name), toIpAccessList(icmpTypeObjectGroups)));

    // convert each ProtocolObjectGroup to IpAccessList
    _protocolObjectGroups.forEach(
        (name, protocolObjectGroup) ->
            c.getIpAccessLists()
                .put(computeProtocolObjectGroupAclName(name), toIpAccessList(protocolObjectGroup)));

    // convert each ServiceObject and ServiceObjectGroup to IpAccessList
    _serviceObjectGroups.forEach(
        (name, serviceObjectGroup) ->
            c.getIpAccessLists()
                .put(computeServiceObjectGroupAclName(name), toIpAccessList(serviceObjectGroup)));
    _serviceObjects.forEach(
        (name, serviceObject) ->
            c.getIpAccessLists()
                .put(computeServiceObjectAclName(name), toIpAccessList(serviceObject)));

    // convert standard/extended ipv6 access lists to ipv6 access lists or
    // route6 filter
    // lists
    for (StandardIpv6AccessList saList : _standardIpv6AccessLists.values()) {
      if (isAclUsedForRoutingv6(saList.getName())) {
        Route6FilterList rfList = CiscoConversions.toRoute6FilterList(saList);
        c.getRoute6FilterLists().put(rfList.getName(), rfList);
      }
      c.getIp6AccessLists()
          .put(
              saList.getName(),
              CiscoConversions.toIp6AccessList(saList.toExtendedIpv6AccessList()));
    }
    for (ExtendedIpv6AccessList eaList : _extendedIpv6AccessLists.values()) {
      if (isAclUsedForRoutingv6(eaList.getName())) {
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

    // create zones based on IOS security zones
    _securityZones.forEach((name, securityZone) -> c.getZones().put(name, new Zone(name)));

    // populate zone interfaces based on IOS security zones
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

    _interfacesBySecurityLevel =
        _interfaces.values().stream()
            .filter(iface -> iface.getSecurityLevel() != null)
            .filter(iface -> iface.getAddress() != null)
            .collect(
                Multimaps.toMultimap(
                    Interface::getSecurityLevel,
                    Functions.identity(),
                    MultimapBuilder.hashKeys().arrayListValues()::build));

    // create and populate zones based on ASA security levels
    _interfacesBySecurityLevel.forEach(
        (level, iface) -> {
          String zoneName = computeSecurityLevelZoneName(level);
          Zone zone = c.getZones().computeIfAbsent(zoneName, Zone::new);
          zone.setInterfaces(
              ImmutableSet.<String>builder()
                  .addAll(zone.getInterfaces())
                  .add(getNewInterfaceName(iface))
                  .build());
          _securityLevels.putIfAbsent(zoneName, level);
        });

    // create zone policies
    createZoneAcls(c);

    // convert interfaces
    _interfaces.forEach(
        (ifaceName, iface) -> {
          // Handle renaming interfaces for ASA devices
          String newIfaceName = getNewInterfaceName(iface);
          org.batfish.datamodel.Interface newInterface =
              toInterface(newIfaceName, iface, c.getIpAccessLists(), c);
          String vrfName = iface.getVrf();
          if (vrfName == null) {
            throw new BatfishException("Missing vrf name for iface: '" + iface.getName() + "'");
          }
          c.getAllInterfaces().put(newIfaceName, newInterface);
          c.getVrfs().get(vrfName).getInterfaces().put(newIfaceName, newInterface);
        });
    /*
     * Second pass over the interfaces to set dependency pointers correctly for portchannels
     * and tunnel interfaces
     * TODO: VLAN interfaces
     */
    _interfaces.forEach(
        (ifaceName, iface) -> {
          // Portchannels
          String chGroup = iface.getChannelGroup();
          if (chGroup != null) {
            org.batfish.datamodel.Interface viIface = c.getAllInterfaces().get(chGroup);
            if (viIface != null) {
              viIface.addDependency(new Dependency(ifaceName, DependencyType.AGGREGATE));
            }
          }
          // Tunnels
          Tunnel tunnel = iface.getTunnel();
          if (tunnel != null && isRealInterfaceName(tunnel.getSourceInterfaceName())) {
            org.batfish.datamodel.Interface viIface = c.getAllInterfaces().get(ifaceName);
            if (viIface != null) {
              viIface.addDependency(
                  new Dependency(tunnel.getSourceInterfaceName(), DependencyType.BIND));
            }
          }
        });

    // copy tracking groups
    c.getTrackingGroups().putAll(_trackingGroups);

    // apply vrrp settings to interfaces
    applyVrrp(c);

    // convert MLAG configs
    if (_vendor.equals(ConfigurationFormat.ARISTA)) {
      Mlag viMlag = toMlag(_eosMlagConfiguration);
      if (viMlag != null) {
        c.setMlags(ImmutableMap.of(viMlag.getId(), viMlag));
      }
    }

    // ISAKMP policies to IKE Phase 1 proposals
    for (Entry<Integer, IsakmpPolicy> e : _isakmpPolicies.entrySet()) {
      IkePhase1Proposal ikePhase1Proposal = toIkePhase1Proposal(e.getValue());
      c.getIkePhase1Proposals().put(ikePhase1Proposal.getName(), ikePhase1Proposal);
    }
    resolveKeyringIsakmpProfileAddresses();
    resolveTunnelSourceInterfaces();

    resolveKeyringIfaceNames(_interfaces, _keyrings);
    resolveIsakmpProfileIfaceNames(_interfaces, _isakmpProfiles);
    resolveTunnelIfaceNames(_interfaces);

    // keyrings to IKE phase 1 keys
    ImmutableSortedMap.Builder<String, IkePhase1Key> ikePhase1KeysBuilder =
        ImmutableSortedMap.naturalOrder();
    _keyrings
        .values()
        .forEach(keyring -> ikePhase1KeysBuilder.put(keyring.getName(), toIkePhase1Key(keyring)));

    c.setIkePhase1Keys(ikePhase1KeysBuilder.build());

    // ISAKMP profiles to IKE phase 1 policies
    _isakmpProfiles
        .values()
        .forEach(
            isakmpProfile ->
                c.getIkePhase1Policies()
                    .put(isakmpProfile.getName(), toIkePhase1Policy(isakmpProfile, this, c, _w)));

    // convert ipsec transform sets
    ImmutableSortedMap.Builder<String, IpsecPhase2Proposal> ipsecPhase2ProposalsBuilder =
        ImmutableSortedMap.naturalOrder();
    for (Entry<String, IpsecTransformSet> e : _ipsecTransformSets.entrySet()) {
      ipsecPhase2ProposalsBuilder.put(e.getKey(), toIpsecPhase2Proposal(e.getValue()));
    }
    c.setIpsecPhase2Proposals(ipsecPhase2ProposalsBuilder.build());

    // ipsec policies
    ImmutableSortedMap.Builder<String, IpsecPhase2Policy> ipsecPhase2PoliciesBuilder =
        ImmutableSortedMap.naturalOrder();
    for (IpsecProfile ipsecProfile : _ipsecProfiles.values()) {
      ipsecPhase2PoliciesBuilder.put(ipsecProfile.getName(), toIpsecPhase2Policy(ipsecProfile));
    }
    c.setIpsecPhase2Policies(ipsecPhase2PoliciesBuilder.build());

    // crypto-map sets to IPsec Peer Configs
    for (CryptoMapSet cryptoMapSet : _cryptoMapSets.values()) {
      convertCryptoMapSet(c, cryptoMapSet, _cryptoMapSets, _w);
    }

    // IPsec tunnels to IPsec Peer Configs
    ImmutableSortedMap.Builder<String, IpsecPeerConfig> ipsecPeerConfigBuilder =
        ImmutableSortedMap.naturalOrder();
    ipsecPeerConfigBuilder.putAll(c.getIpsecPeerConfigs());
    for (Entry<String, Interface> e : _interfaces.entrySet()) {
      String name = e.getKey();
      Interface iface = e.getValue();
      Tunnel tunnel = iface.getTunnel();
      if (iface.getActive() && tunnel != null && tunnel.getMode() == TunnelMode.IPSEC) {
        if (tunnel.getIpsecProfileName() == null) {
          _w.redFlag(String.format("No IPSec Profile set for IPSec tunnel %s", name));
          continue;
        }
        // convert to IpsecPeerConfig
        ipsecPeerConfigBuilder.put(name, toIpsecPeerConfig(tunnel, name, this, c));
      }
    }
    c.setIpsecPeerConfigs(ipsecPeerConfigBuilder.build());

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
            org.batfish.datamodel.RipProcess newRipProcess = toRipProcess(ripProcess, vrfName, c);
            newVrf.setRipProcess(newRipProcess);
          }

          // convert ospf process
          OspfProcess ospfProcess = vrf.getOspfProcess();
          if (ospfProcess != null) {
            org.batfish.datamodel.ospf.OspfProcess newOspfProcess =
                toOspfProcess(ospfProcess, vrfName, c, this);
            newVrf.setOspfProcess(newOspfProcess);
          }

          // convert eigrp processes
          vrf.getEigrpProcesses().values().stream()
              .map(proc -> CiscoConversions.toEigrpProcess(proc, vrfName, c, this))
              .filter(Objects::nonNull)
              .forEach(
                  eigrpProcess ->
                      newVrf.getEigrpProcesses().put(eigrpProcess.getAsn(), eigrpProcess));

          // convert isis process
          IsisProcess isisProcess = vrf.getIsisProcess();
          if (isisProcess != null) {
            org.batfish.datamodel.isis.IsisProcess newIsisProcess =
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

    // convert Arista EOS VXLAN
    if (_eosVxlan != null) {
      String sourceIfaceName = _eosVxlan.getSourceInterface();
      Interface sourceIface = sourceIfaceName == null ? null : _interfaces.get(sourceIfaceName);
      org.batfish.datamodel.Vrf vrf =
          sourceIface != null ? c.getVrfs().get(sourceIface.getVrf()) : c.getDefaultVrf();

      _eosVxlan
          .getVlanVnis()
          .forEach(
              (vlan, vni) ->
                  vrf.getVniSettings().put(vni, toVniSettings(_eosVxlan, vni, vlan, sourceIface)));
    }

    // Define the Null0 interface if it has been referenced. Otherwise, these show as undefined
    // references.
    Optional<Integer> firstRefToNull0 =
        _structureReferences.getOrDefault(CiscoStructureType.INTERFACE, ImmutableSortedMap.of())
            .getOrDefault("Null0", ImmutableSortedMap.of()).entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .min(Integer::compare);
    if (firstRefToNull0.isPresent()) {
      defineStructure(CiscoStructureType.INTERFACE, "Null0", firstRefToNull0.get());
    }

    markConcreteStructure(
        CiscoStructureType.BFD_TEMPLATE, CiscoStructureUsage.INTERFACE_BFD_TEMPLATE);

    markConcreteStructure(
        CiscoStructureType.COMMUNITY_SET,
        CiscoStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_ANY,
        CiscoStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_EVERY,
        CiscoStructureUsage.ROUTE_POLICY_DELETE_COMMUNITY_IN,
        CiscoStructureUsage.ROUTE_POLICY_SET_COMMUNITY);

    markConcreteStructure(
        CiscoStructureType.SECURITY_ZONE_PAIR, CiscoStructureUsage.SECURITY_ZONE_PAIR_SELF_REF);

    markConcreteStructure(
        CiscoStructureType.INTERFACE,
        CiscoStructureUsage.BGP_UPDATE_SOURCE_INTERFACE,
        CiscoStructureUsage.DOMAIN_LOOKUP_SOURCE_INTERFACE,
        CiscoStructureUsage.EIGRP_AF_INTERFACE,
        CiscoStructureUsage.EIGRP_PASSIVE_INTERFACE,
        CiscoStructureUsage.FAILOVER_LAN_INTERFACE,
        CiscoStructureUsage.FAILOVER_LINK_INTERFACE,
        CiscoStructureUsage.INTERFACE_SELF_REF,
        CiscoStructureUsage.IP_DOMAIN_LOOKUP_INTERFACE,
        CiscoStructureUsage.IP_ROUTE_NHINT,
        CiscoStructureUsage.IP_TACACS_SOURCE_INTERFACE,
        CiscoStructureUsage.NTP_SOURCE_INTERFACE,
        CiscoStructureUsage.OBJECT_NAT_MAPPED_INTERFACE,
        CiscoStructureUsage.OBJECT_NAT_REAL_INTERFACE,
        CiscoStructureUsage.OSPF_AREA_INTERFACE,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_IN,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_OUT,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ROUTE_MAP_IN,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ROUTE_MAP_OUT,
        CiscoStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_IN,
        CiscoStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_OUT,
        CiscoStructureUsage.ROUTER_STATIC_ROUTE,
        CiscoStructureUsage.ROUTER_VRRP_INTERFACE,
        CiscoStructureUsage.SERVICE_POLICY_INTERFACE,
        CiscoStructureUsage.SNMP_SERVER_SOURCE_INTERFACE,
        CiscoStructureUsage.SNMP_SERVER_TRAP_SOURCE,
        CiscoStructureUsage.TACACS_SOURCE_INTERFACE,
        CiscoStructureUsage.TRACK_INTERFACE,
        CiscoStructureUsage.TWICE_NAT_MAPPED_INTERFACE,
        CiscoStructureUsage.TWICE_NAT_REAL_INTERFACE,
        CiscoStructureUsage.VXLAN_SOURCE_INTERFACE);

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
        CiscoStructureUsage.NTP_ACCESS_GROUP,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST,
        CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL6,
        CiscoStructureUsage.SSH_IPV6_ACL,
        CiscoStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_IN,
        CiscoStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_OUT);
    markAcls(
        CiscoStructureUsage.ACCESS_GROUP_GLOBAL_FILTER,
        CiscoStructureUsage.COPS_LISTENER_ACCESS_LIST,
        CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL,
        CiscoStructureUsage.INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP,
        CiscoStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL,
        CiscoStructureUsage.INTERFACE_IGMP_HOST_PROXY_ACCESS_LIST,
        CiscoStructureUsage.INTERFACE_INCOMING_FILTER,
        CiscoStructureUsage.INTERFACE_IP_INBAND_ACCESS_GROUP,
        CiscoStructureUsage.INTERFACE_OUTGOING_FILTER,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT,
        CiscoStructureUsage.RIP_DISTRIBUTE_LIST,
        CiscoStructureUsage.ROUTER_ISIS_DISTRIBUTE_LIST_ACL,
        CiscoStructureUsage.SNMP_SERVER_FILE_TRANSFER_ACL,
        CiscoStructureUsage.SNMP_SERVER_TFTP_SERVER_LIST,
        CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL,
        CiscoStructureUsage.SSH_ACL,
        CiscoStructureUsage.WCCP_GROUP_LIST,
        CiscoStructureUsage.WCCP_REDIRECT_LIST,
        CiscoStructureUsage.WCCP_SERVICE_LIST);

    markCommunityLists(
        CiscoStructureUsage.ROUTE_MAP_ADD_COMMUNITY,
        CiscoStructureUsage.ROUTE_MAP_DELETE_COMMUNITY,
        CiscoStructureUsage.ROUTE_MAP_MATCH_COMMUNITY_LIST,
        CiscoStructureUsage.ROUTE_MAP_SET_COMMUNITY);

    markConcreteStructure(
        CiscoStructureType.PREFIX_LIST,
        CiscoStructureUsage.BGP_INBOUND_PREFIX_LIST,
        CiscoStructureUsage.BGP_OUTBOUND_PREFIX_LIST,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_IN,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_OUT,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_PREFIX_LIST);
    markConcreteStructure(
        CiscoStructureType.PREFIX6_LIST,
        CiscoStructureUsage.BGP_INBOUND_PREFIX6_LIST,
        CiscoStructureUsage.BGP_OUTBOUND_PREFIX6_LIST,
        CiscoStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_IN,
        CiscoStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_OUT,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_PREFIX_LIST);

    markConcreteStructure(
        CiscoStructureType.PREFIX_SET, CiscoStructureUsage.ROUTE_POLICY_PREFIX_SET);

    // mark references to route-maps
    markConcreteStructure(
        CiscoStructureType.ROUTE_MAP,
        CiscoStructureUsage.BGP_ADVERTISE_MAP_EXIST_MAP,
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
        CiscoStructureUsage.EIGRP_REDISTRIBUTE_BGP_MAP,
        CiscoStructureUsage.EIGRP_REDISTRIBUTE_CONNECTED_MAP,
        CiscoStructureUsage.EIGRP_REDISTRIBUTE_EIGRP_MAP,
        CiscoStructureUsage.EIGRP_REDISTRIBUTE_ISIS_MAP,
        CiscoStructureUsage.EIGRP_REDISTRIBUTE_OSPF_MAP,
        CiscoStructureUsage.EIGRP_REDISTRIBUTE_RIP_MAP,
        CiscoStructureUsage.EIGRP_REDISTRIBUTE_STATIC_MAP,
        CiscoStructureUsage.INTERFACE_IP_VRF_SITEMAP,
        CiscoStructureUsage.INTERFACE_POLICY_ROUTING_MAP,
        CiscoStructureUsage.INTERFACE_SUMMARY_ADDRESS_EIGRP_LEAK_MAP,
        CiscoStructureUsage.OSPF_DEFAULT_ORIGINATE_ROUTE_MAP,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ROUTE_MAP_IN,
        CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ROUTE_MAP_OUT,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_BGP_MAP,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_CONNECTED_MAP,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_EIGRP_MAP,
        CiscoStructureUsage.OSPF_REDISTRIBUTE_STATIC_MAP,
        CiscoStructureUsage.PIM_ACCEPT_REGISTER_ROUTE_MAP,
        CiscoStructureUsage.RIP_DEFAULT_ORIGINATE_ROUTE_MAP,
        CiscoStructureUsage.RIP_REDISTRIBUTE_BGP_MAP,
        CiscoStructureUsage.RIP_REDISTRIBUTE_CONNECTED_MAP,
        CiscoStructureUsage.RIP_REDISTRIBUTE_STATIC_MAP);

    markConcreteStructure(
        CiscoStructureType.ROUTE_POLICY,
        CiscoStructureUsage.BGP_ADDITIONAL_PATHS_SELECTION_ROUTE_POLICY,
        CiscoStructureUsage.BGP_AGGREGATE_ROUTE_POLICY,
        CiscoStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_IN,
        CiscoStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_OUT);

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

    // Crypto, Isakmp, and IPSec
    markConcreteStructure(
        CiscoStructureType.CRYPTO_DYNAMIC_MAP_SET,
        CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_CRYPTO_DYNAMIC_MAP_SET);
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
        CiscoStructureType.POLICY_MAP,
        CiscoStructureUsage.CONTROL_PLANE_SERVICE_POLICY_INPUT,
        CiscoStructureUsage.CONTROL_PLANE_SERVICE_POLICY_OUTPUT,
        CiscoStructureUsage.INTERFACE_SERVICE_POLICY,
        CiscoStructureUsage.INTERFACE_SERVICE_POLICY_CONTROL_SUBSCRIBER,
        CiscoStructureUsage.POLICY_MAP_CLASS_SERVICE_POLICY,
        CiscoStructureUsage.SERVICE_POLICY_GLOBAL,
        CiscoStructureUsage.SERVICE_POLICY_INTERFACE_POLICY);

    // object-group
    markConcreteStructure(
        CiscoStructureType.ICMP_TYPE_OBJECT_GROUP,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_ICMP_TYPE_OBJECT_GROUP,
        CiscoStructureUsage.ICMP_TYPE_OBJECT_GROUP_GROUP_OBJECT);
    markConcreteStructure(
        CiscoStructureType.NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.NETWORK_OBJECT_GROUP_GROUP_OBJECT,
        CiscoStructureUsage.OBJECT_NAT_MAPPED_SOURCE_NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.TWICE_NAT_MAPPED_DESTINATION_NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.TWICE_NAT_MAPPED_SOURCE_NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.TWICE_NAT_REAL_DESTINATION_NETWORK_OBJECT_GROUP,
        CiscoStructureUsage.TWICE_NAT_REAL_SOURCE_NETWORK_OBJECT_GROUP);
    markConcreteStructure(
        CiscoStructureType.PROTOCOL_OBJECT_GROUP,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OBJECT_GROUP,
        CiscoStructureUsage.PROTOCOL_OBJECT_GROUP_GROUP_OBJECT);
    markConcreteStructure(
        CiscoStructureType.SERVICE_OBJECT_GROUP,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT_GROUP,
        CiscoStructureUsage.SERVICE_OBJECT_GROUP_GROUP_OBJECT);
    markAbstractStructure(
        CiscoStructureType.PROTOCOL_OR_SERVICE_OBJECT_GROUP,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP,
        ImmutableList.of(
            CiscoStructureType.PROTOCOL_OBJECT_GROUP, CiscoStructureType.SERVICE_OBJECT_GROUP));

    // objects
    markConcreteStructure(
        CiscoStructureType.ICMP_TYPE_OBJECT,
        CiscoStructureUsage.ICMP_TYPE_OBJECT_GROUP_ICMP_OBJECT);
    markConcreteStructure(
        CiscoStructureType.NETWORK_OBJECT,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT,
        CiscoStructureUsage.NETWORK_OBJECT_GROUP_NETWORK_OBJECT,
        CiscoStructureUsage.OBJECT_NAT_MAPPED_SOURCE_NETWORK_OBJECT,
        CiscoStructureUsage.OBJECT_NAT_REAL_SOURCE_NETWORK_OBJECT,
        CiscoStructureUsage.TWICE_NAT_MAPPED_DESTINATION_NETWORK_OBJECT,
        CiscoStructureUsage.TWICE_NAT_MAPPED_SOURCE_NETWORK_OBJECT,
        CiscoStructureUsage.TWICE_NAT_REAL_DESTINATION_NETWORK_OBJECT,
        CiscoStructureUsage.TWICE_NAT_REAL_SOURCE_NETWORK_OBJECT);
    markConcreteStructure(
        CiscoStructureType.SERVICE_OBJECT,
        CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT,
        CiscoStructureUsage.SERVICE_OBJECT_GROUP_SERVICE_OBJECT);
    markConcreteStructure(
        CiscoStructureType.PROTOCOL_OBJECT,
        CiscoStructureUsage.PROTOCOL_OBJECT_GROUP_PROTOCOL_OBJECT);

    // service template
    markConcreteStructure(
        CiscoStructureType.SERVICE_TEMPLATE,
        CiscoStructureUsage.CLASS_MAP_SERVICE_TEMPLATE,
        CiscoStructureUsage.CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE,
        CiscoStructureUsage.POLICY_MAP_EVENT_CLASS_ACTIVATE);

    // track
    markConcreteStructure(CiscoStructureType.TRACK, CiscoStructureUsage.INTERFACE_STANDBY_TRACK);

    // VXLAN
    markConcreteStructure(CiscoStructureType.VXLAN, CiscoStructureUsage.VXLAN_SELF_REF);

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

    markConcreteStructure(
        CiscoStructureType.AS_PATH_SET, CiscoStructureUsage.ROUTE_POLICY_AS_PATH_IN);

    // BGP inheritance. This is complicated, as there are many similar-but-overlapping concepts
    markConcreteStructure(CiscoStructureType.BGP_AF_GROUP, CiscoStructureUsage.BGP_USE_AF_GROUP);
    markConcreteStructure(
        CiscoStructureType.BGP_NEIGHBOR_GROUP, CiscoStructureUsage.BGP_USE_NEIGHBOR_GROUP);
    markConcreteStructure(
        CiscoStructureType.BGP_PEER_GROUP,
        CiscoStructureUsage.BGP_LISTEN_RANGE_PEER_GROUP,
        CiscoStructureUsage.BGP_NEIGHBOR_PEER_GROUP,
        CiscoStructureUsage.BGP_NEIGHBOR_STATEMENT);
    markConcreteStructure(
        CiscoStructureType.BGP_SESSION_GROUP, CiscoStructureUsage.BGP_USE_SESSION_GROUP);
    markConcreteStructure(
        CiscoStructureType.BGP_TEMPLATE_PEER, CiscoStructureUsage.BGP_INHERITED_PEER);
    markConcreteStructure(
        CiscoStructureType.BGP_TEMPLATE_PEER_POLICY, CiscoStructureUsage.BGP_INHERITED_PEER_POLICY);
    markConcreteStructure(
        CiscoStructureType.BGP_TEMPLATE_PEER_SESSION, CiscoStructureUsage.BGP_INHERITED_SESSION);
    markConcreteStructure(
        CiscoStructureType.BGP_UNDECLARED_PEER, CiscoStructureUsage.BGP_NEIGHBOR_WITHOUT_REMOTE_AS);
    markConcreteStructure(
        CiscoStructureType.BGP_UNDECLARED_PEER_GROUP,
        CiscoStructureUsage.BGP_PEER_GROUP_REFERENCED_BEFORE_DEFINED);

    c.simplifyRoutingPolicies();

    c.computeRoutingPolicySources(_w);

    return ImmutableList.of(c);
  }

  private static VniSettings toVniSettings(
      @Nonnull AristaEosVxlan vxlan,
      @Nonnull Integer vni,
      @Nonnull Integer vlan,
      @Nullable Interface sourceInterface) {
    Ip sourceAddress =
        sourceInterface == null
            ? null
            : sourceInterface.getAddress() == null ? null : sourceInterface.getAddress().getIp();

    // Prefer VLAN-specific or general flood address (in that order) over multicast address
    SortedSet<Ip> bumTransportIps =
        firstNonNull(vxlan.getVlanFloodAddresses().get(vlan), vxlan.getFloodAddresses());
    BumTransportMethod bumTransportMethod = BumTransportMethod.UNICAST_FLOOD_GROUP;

    if (bumTransportIps.isEmpty()) {
      Ip multicastAddress = vxlan.getMulticastGroup();
      bumTransportIps =
          multicastAddress == null
              ? ImmutableSortedSet.of()
              : ImmutableSortedSet.of(multicastAddress);
      bumTransportMethod = BumTransportMethod.MULTICAST_GROUP;
    }

    return new VniSettings(
        bumTransportIps,
        bumTransportMethod,
        sourceAddress,
        firstNonNull(vxlan.getUdpPort(), AristaEosVxlan.DEFAULT_UDP_PORT),
        vlan,
        vni);
  }

  private boolean allowsIntraZoneTraffic(String zoneName) {
    if (!_securityLevels.containsKey(zoneName)) {
      return true;
    }
    return _sameSecurityTrafficInter && _sameSecurityTrafficIntra;
  }

  private void createInspectClassMapAcls(Configuration c) {
    _inspectClassMaps.forEach(
        (inspectClassMapName, inspectClassMap) -> {
          String inspectClassMapAclName = computeInspectClassMapAclName(inspectClassMapName);
          MatchSemantics matchSemantics = inspectClassMap.getMatchSemantics();
          List<AclLineMatchExpr> matchConditions =
              inspectClassMap.getMatches().stream()
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
              .setSourceName(inspectClassMapName)
              .setSourceType(CiscoStructureType.INSPECT_CLASS_MAP.getDescription())
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
              .setSourceName(inspectPolicyMapName)
              .setSourceType(CiscoStructureType.INSPECT_POLICY_MAP.getDescription())
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
              SortedSet<String> interfaces = zone.getInterfaces();
              if (interfaces.isEmpty()) {
                return;
              }

              ImmutableList.Builder<IpAccessListLine> zonePolicies = ImmutableList.builder();

              // Allow traffic originating from device (no source interface)
              zonePolicies.add(
                  IpAccessListLine.accepting()
                      .setMatchCondition(OriginatingFromDevice.INSTANCE)
                      .setName("Allow traffic originating from this device")
                      .build());

              // Allow traffic staying within this zone (always true for IOS)
              if (allowsIntraZoneTraffic(zoneName)) {
                zonePolicies.add(
                    IpAccessListLine.accepting()
                        .setMatchCondition(matchSrcInterfaceBySrcZone.get(zoneName))
                        .setName(
                            String.format(
                                "Allow traffic received on interface in same zone: '%s'", zoneName))
                        .build());
              }

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
                                zonePair)
                            .ifPresent(zonePolicies::add));
              }

              // Security level policies
              zonePolicies.addAll(createSecurityLevelAcl(zoneName));

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
      SecurityZonePair zonePair) {
    String inspectPolicyMapName = zonePair.getInspectPolicyMap();
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
        .setSourceName(zonePair.getName())
        .setSourceType(CiscoStructureType.SECURITY_ZONE_PAIR.getDescription())
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

  private List<IpAccessListLine> createSecurityLevelAcl(String zoneName) {
    Integer level = _securityLevels.get(zoneName);
    if (level == null) {
      return ImmutableList.of();
    }

    // Allow outbound traffic from interfaces with higher security levels unconditionally
    List<IpAccessListLine> lines =
        _interfacesBySecurityLevel.keySet().stream()
            .filter(l -> l > level)
            .map(
                l ->
                    IpAccessListLine.accepting()
                        .setName("Traffic from security level " + l)
                        .setMatchCondition(
                            new MatchSrcInterface(
                                _interfacesBySecurityLevel.get(l).stream()
                                    .map(this::getNewInterfaceName)
                                    .collect(Collectors.toList())))
                        .build())
            .collect(Collectors.toList());

    // Allow outbound traffic from interfaces with lower security levels if that interface has an
    // inbound ACL
    lines.addAll(
        _interfacesBySecurityLevel.keySet().stream()
            .filter(l -> l < level)
            .map(
                l ->
                    IpAccessListLine.accepting()
                        .setName("Traffic from security level " + l + " with inbound filter")
                        .setMatchCondition(
                            new MatchSrcInterface(
                                _interfacesBySecurityLevel.get(l).stream()
                                    .filter(iface -> iface.getIncomingFilter() != null)
                                    .map(this::getNewInterfaceName)
                                    .collect(Collectors.toList())))
                        .build())
            .filter(
                line ->
                    !((MatchSrcInterface) line.getMatchCondition()).getSrcInterfaces().isEmpty())
            .collect(Collectors.toList()));

    // Allow traffic for existing connections
    lines.add(ACL_LINE_EXISTING_CONNECTION);
    return lines;
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

  public static String computeSecurityLevelZoneName(int securityLevel) {
    return String.format("SECURITY_LEVEL_%s", securityLevel);
  }

  private boolean isAclUsedForRouting(String aclName) {
    String currentMapName;
    for (Vrf vrf : _vrfs.values()) {
      // check ospf policies
      OspfProcess ospfProcess = vrf.getOspfProcess();
      if (ospfProcess != null) {
        for (OspfRedistributionPolicy rp : ospfProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpAccessList(aclName, currentMapName)) {
            return true;
          }
        }
        currentMapName = ospfProcess.getDefaultInformationOriginateMap();
        if (containsIpAccessList(aclName, currentMapName)) {
          return true;
        }
      }
      RipProcess ripProcess = vrf.getRipProcess();
      if (ripProcess != null) {
        // check rip distribute lists
        if (ripProcess.getDistributeListInAcl()
            && ripProcess.getDistributeListIn().equals(aclName)) {
          return true;
        }
        if (ripProcess.getDistributeListOutAcl()
            && ripProcess.getDistributeListOut().equals(aclName)) {
          return true;
        }
        // check rip redistribution policies
        for (RipRedistributionPolicy rp : ripProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpAccessList(aclName, currentMapName)) {
            return true;
          }
        }
      }
      // check bgp policies
      BgpProcess bgpProcess = vrf.getBgpProcess();
      if (bgpProcess != null) {
        for (BgpRedistributionPolicy rp : bgpProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpAccessList(aclName, currentMapName)) {
            return true;
          }
        }
        for (BgpPeerGroup pg : bgpProcess.getAllPeerGroups()) {
          currentMapName = pg.getInboundRouteMap();
          if (containsIpAccessList(aclName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getOutboundRouteMap();
          if (containsIpAccessList(aclName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getDefaultOriginateMap();
          if (containsIpAccessList(aclName, currentMapName)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean isAclUsedForRoutingv6(String aclName) {
    String currentMapName;
    for (Vrf vrf : _vrfs.values()) {
      // check ospf policies
      OspfProcess ospfProcess = vrf.getOspfProcess();
      if (ospfProcess != null) {
        for (OspfRedistributionPolicy rp : ospfProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpv6AccessList(aclName, currentMapName)) {
            return true;
          }
        }
        currentMapName = ospfProcess.getDefaultInformationOriginateMap();
        if (containsIpAccessList(aclName, currentMapName)) {
          return true;
        }
      }
      // check bgp policies
      BgpProcess bgpProcess = vrf.getBgpProcess();
      if (bgpProcess != null) {
        for (BgpRedistributionPolicy rp : bgpProcess.getRedistributionPolicies().values()) {
          currentMapName = rp.getRouteMap();
          if (containsIpv6AccessList(aclName, currentMapName)) {
            return true;
          }
        }
        for (BgpPeerGroup pg : bgpProcess.getAllPeerGroups()) {
          currentMapName = pg.getInboundRouteMap();
          if (containsIpv6AccessList(aclName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getInboundRoute6Map();
          if (containsIpv6AccessList(aclName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getOutboundRouteMap();
          if (containsIpv6AccessList(aclName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getOutboundRoute6Map();
          if (containsIpv6AccessList(aclName, currentMapName)) {
            return true;
          }
          currentMapName = pg.getDefaultOriginateMap();
          if (containsIpv6AccessList(aclName, currentMapName)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void markCommunityLists(CiscoStructureUsage... usages) {
    for (CiscoStructureUsage usage : usages) {
      markAbstractStructure(
          CiscoStructureType.COMMUNITY_LIST,
          usage,
          ImmutableList.of(
              CiscoStructureType.COMMUNITY_LIST_EXPANDED,
              CiscoStructureType.COMMUNITY_LIST_STANDARD));
    }
  }

  /**
   * Resolves the addresses of the interfaces used in localInterfaceName of IsaKmpProfiles and
   * Keyrings
   */
  private void resolveKeyringIsakmpProfileAddresses() {
    Map<String, Ip> ifaceNameToPrimaryIp = computeInterfaceOwnedPrimaryIp(_interfaces);

    _keyrings.values().stream()
        .filter(keyring -> !keyring.getLocalInterfaceName().equals(UNSET_LOCAL_INTERFACE))
        .forEach(
            keyring ->
                keyring.setLocalAddress(
                    firstNonNull(
                        ifaceNameToPrimaryIp.get(keyring.getLocalInterfaceName()), Ip.AUTO)));

    _isakmpProfiles.values().stream()
        .filter(
            isakmpProfile -> !isakmpProfile.getLocalInterfaceName().equals(UNSET_LOCAL_INTERFACE))
        .forEach(
            isakmpProfile ->
                isakmpProfile.setLocalAddress(
                    firstNonNull(
                        ifaceNameToPrimaryIp.get(isakmpProfile.getLocalInterfaceName()), Ip.AUTO)));
  }

  /** Resolves the addresses of the interfaces used in sourceInterfaceName of Tunnel interfaces */
  private void resolveTunnelSourceInterfaces() {
    Map<String, Ip> ifaceNameToPrimaryIp = computeInterfaceOwnedPrimaryIp(_interfaces);

    for (Interface iface : _interfaces.values()) {
      Tunnel tunnel = iface.getTunnel();
      if (tunnel != null && !tunnel.getSourceInterfaceName().equals(UNSET_LOCAL_INTERFACE)) {
        tunnel.setSourceAddress(ifaceNameToPrimaryIp.get(tunnel.getSourceInterfaceName()));
      }
    }
  }

  public Map<String, IcmpTypeObjectGroup> getIcmpTypeObjectGroups() {
    return _icmpTypeObjectGroups;
  }

  public Map<String, NetworkObjectGroup> getNetworkObjectGroups() {
    return _networkObjectGroups;
  }

  public Map<String, NetworkObjectInfo> getNetworkObjectInfos() {
    return _networkObjectInfos;
  }

  public Map<String, NetworkObject> getNetworkObjects() {
    return _networkObjects;
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

  public Map<String, ServiceObject> getServiceObjects() {
    return _serviceObjects;
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

  public Map<String, TrackMethod> getTrackingGroups() {
    return _trackingGroups;
  }

  public Map<String, NamedCommunitySet> getCommunitySets() {
    return _communitySets;
  }
}
