package org.batfish.representation.cisco;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.datamodel.Interface.computeInterfaceType;
import static org.batfish.datamodel.Interface.isRealInterfaceName;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.datamodel.Names.generatedBgpRedistributionPolicyName;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.SOURCE_ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.bgp.AllowRemoteAsOutMode.ALWAYS;
import static org.batfish.datamodel.routing_policy.Common.initDenyAllBgpRedistributionPolicy;
import static org.batfish.datamodel.routing_policy.Common.matchDefaultRoute;
import static org.batfish.datamodel.routing_policy.Common.suppressSummarizedPrefixes;
import static org.batfish.representation.cisco.CiscoConversions.computeDistributeListPolicies;
import static org.batfish.representation.cisco.CiscoConversions.convertCryptoMapSet;
import static org.batfish.representation.cisco.CiscoConversions.convertVrfLeakingConfig;
import static org.batfish.representation.cisco.CiscoConversions.generateBgpExportPolicy;
import static org.batfish.representation.cisco.CiscoConversions.generateBgpImportPolicy;
import static org.batfish.representation.cisco.CiscoConversions.generateEigrpPolicy;
import static org.batfish.representation.cisco.CiscoConversions.getIsakmpKeyGeneratedName;
import static org.batfish.representation.cisco.CiscoConversions.getRsaPubKeyGeneratedName;
import static org.batfish.representation.cisco.CiscoConversions.matchOwnAsn;
import static org.batfish.representation.cisco.CiscoConversions.resolveIsakmpProfileIfaceNames;
import static org.batfish.representation.cisco.CiscoConversions.resolveKeyringIfaceNames;
import static org.batfish.representation.cisco.CiscoConversions.resolveTunnelIfaceNames;
import static org.batfish.representation.cisco.CiscoConversions.toBgpAggregate;
import static org.batfish.representation.cisco.CiscoConversions.toIkePhase1Key;
import static org.batfish.representation.cisco.CiscoConversions.toIkePhase1Policy;
import static org.batfish.representation.cisco.CiscoConversions.toIkePhase1Proposal;
import static org.batfish.representation.cisco.CiscoConversions.toIpAccessList;
import static org.batfish.representation.cisco.CiscoConversions.toIpSpace;
import static org.batfish.representation.cisco.CiscoConversions.toIpsecPeerConfig;
import static org.batfish.representation.cisco.CiscoConversions.toIpsecPhase2Policy;
import static org.batfish.representation.cisco.CiscoConversions.toIpsecPhase2Proposal;
import static org.batfish.representation.cisco.CiscoConversions.toOspfDeadInterval;
import static org.batfish.representation.cisco.CiscoConversions.toOspfHelloInterval;
import static org.batfish.representation.cisco.CiscoConversions.toOspfNetworkType;
import static org.batfish.representation.cisco.OspfProcess.DEFAULT_LOOPBACK_OSPF_COST;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.batfish.common.util.CollectionUtil;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.FirewallSessionInterfaceInfo.Action;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GeneratedRoute6;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPhase2Policy;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.Line;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.Names;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Prefix6Space;
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
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.TunnelConfiguration;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.eigrp.ClassicMetric;
import org.batfish.datamodel.eigrp.EigrpInterfaceSettings;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.eigrp.EigrpMetricValues;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.eigrp.WideMetric;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.CommunityAcl;
import org.batfish.datamodel.routing_policy.communities.CommunityAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunityIn;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAcl;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
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
import org.batfish.datamodel.routing_policy.statement.CallStatement;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.routing_policy.statement.TraceableStatement;
import org.batfish.datamodel.tracking.TrackMethod;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.vendor_family.cisco.Aaa;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthentication;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthenticationLogin;
import org.batfish.datamodel.vendor_family.cisco.CiscoFamily;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.vendor.VendorConfiguration;
import org.batfish.vendor.VendorStructureId;

public final class CiscoConfiguration extends VendorConfiguration {
  public static final int DEFAULT_STATIC_ROUTE_DISTANCE = 1;

  /** Matches anything but the IPv4 default route. */
  static final Not NOT_DEFAULT_ROUTE = new Not(matchDefaultRoute());

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
          .put("HundredGigabitEthernet", "HundredGigabitEthernet")
          .put("ip", "ip")
          .put("Group-Async", "Group-Async")
          .put("lo", "Loopback")
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
          .put("Port-channel", "Port-channel")
          .put("POS", "POS")
          .put("PTP", "PTP")
          .put("Redundant", "Redundant")
          .put("Serial", "Serial")
          .put("Service-Engine", "Service-Engine")
          .put("TenGigabitEthernet", "TenGigabitEthernet")
          .put("TenGigE", "TenGigabitEthernet")
          .put("te", "TenGigabitEthernet")
          .put("trunk", "trunk")
          .put("Tunnel", "Tunnel")
          .put("tunnel-ip", "tunnel-ip")
          .put("tunnel-te", "tunnel-te")
          .put("tw", "TwoGigabitEthernet")
          .put("twe", "TwentyFiveGigE")
          .put("TwentyFiveGigE", "TwentyFiveGigE")
          .put("TwentyFiveGigabitEthernet", "TwentyFiveGigE")
          .put("TwoGigabitEthernet", "TwoGigabitEthernet")
          .put("vasileft", "vasileft")
          .put("vasiright", "vasiright")
          .put("ve", "VirtualEthernet")
          .put("VirtualEthernet", "VirtualEthernet")
          .put("Virtual-Template", "Virtual-Template")
          .put("Vlan", "Vlan")
          .put("Vxlan", "Vxlan")
          .put("Wideband-Cable", "Wideband-Cable")
          .put("Wlan-ap", "Wlan-ap")
          .put("Wlan-GigabitEthernet", "Wlan-GigabitEthernet")
          .build();

  static final boolean DEFAULT_VRRP_PREEMPT = true;

  static final int DEFAULT_VRRP_PRIORITY = 100;

  public static final String MANAGEMENT_VRF_NAME = "management";

  static final int MAX_ADMINISTRATIVE_COST = 32767;

  public static final String MANAGEMENT_INTERFACE_PREFIX = "mgmt";

  /** Name of the generated static route resolution policy, implementing IOS resolution filtering */
  public static final String RESOLUTION_POLICY_NAME = "~RESOLUTION_POLICY~";

  private static final int VLAN_NORMAL_MAX_CISCO = 1005;

  private static final int VLAN_NORMAL_MIN_CISCO = 2;

  public static String computeBgpDefaultRouteExportPolicyName(
      boolean ipv4, String vrf, String peer) {
    return String.format(
        "~BGP_DEFAULT_ROUTE_PEER_EXPORT_POLICY:IPv%s:%s:%s~", ipv4 ? "4" : "6", vrf, peer);
  }

  public static String computeBgpPeerImportPolicyName(String vrf, String peer) {
    return String.format("~BGP_PEER_IMPORT_POLICY:%s:%s~", vrf, peer);
  }

  public static String computeIcmpObjectGroupAclName(String name) {
    return String.format("~ICMP_OBJECT_GROUP~%s~", name);
  }

  /**
   * Computes a mapping of interface names to the primary {@link Ip} owned by each of the interface.
   * Filters out the interfaces having no primary {@link ConcreteInterfaceAddress}
   */
  private static Map<String, Ip> computeInterfaceOwnedPrimaryIp(Map<String, Interface> interfaces) {
    return interfaces.entrySet().stream()
        .filter(e -> Objects.nonNull(e.getValue().getAddress()))
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getAddress().getIp()));
  }

  public static String computeOspfDefaultRouteGenerationPolicyName(String vrf, String proc) {
    return String.format("~OSPF_DEFAULT_ROUTE_GENERATION_POLICY:%s:%s~", vrf, proc);
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

  @VisibleForTesting
  static String getRouteMapClausePolicyName(RouteMap map, int continueTarget) {
    String mapName = map.getName();
    String clausePolicyName = "~RMCLAUSE~" + mapName + "~" + continueTarget + "~";
    return clausePolicyName;
  }

  private final Map<String, IpAsPathAccessList> _asPathAccessLists;

  private final CiscoFamily _cf;

  private final Map<String, CryptoMapSet> _cryptoMapSets;

  private final Map<String, NamedRsaPubKey> _cryptoNamedRsaPubKeys;

  private final List<Ip> _dhcpRelayServers;

  private NavigableSet<String> _dnsServers;

  private String _dnsSourceInterface;

  private String _domainName;

  private final Map<String, ExpandedCommunityList> _expandedCommunityLists;

  private final Map<String, ExtendedAccessList> _extendedAccessLists;

  private final Map<String, ExtendedIpv6AccessList> _extendedIpv6AccessLists;

  private String _hostname;
  private String _rawHostname;

  private final Map<String, InspectClassMap> _inspectClassMaps;

  private final Map<String, InspectPolicyMap> _inspectPolicyMaps;

  private final Map<String, Interface> _interfaces;

  private final Map<String, IpsecProfile> _ipsecProfiles;

  private final Map<String, IpsecTransformSet> _ipsecTransformSets;

  private final List<IsakmpKey> _isakmpKeys;

  private final Map<Integer, IsakmpPolicy> _isakmpPolicies;

  private final Map<String, IsakmpProfile> _isakmpProfiles;

  private final Map<String, Keyring> _keyrings;

  private final Map<String, MacAccessList> _macAccessLists;

  private final @Nonnull Map<String, NatPool> _natPools;

  private final Map<String, IcmpTypeObjectGroup> _icmpTypeObjectGroups;

  private final Map<String, IntegerSpace> _namedVlans;

  private final @Nonnull Set<String> _natInside;

  private final Set<String> _natOutside;

  private final List<CiscoIosNat> _ciscoIosNats;

  private final Map<String, NetworkObjectGroup> _networkObjectGroups;

  private final Map<String, NetworkObjectInfo> _networkObjectInfos;

  private final Map<String, NetworkObject> _networkObjects;

  private String _ntpSourceInterface;

  private final Map<String, ObjectGroup> _objectGroups;

  private final Map<String, Prefix6List> _prefix6Lists;

  private final Map<String, PrefixList> _prefixLists;

  private final Map<String, ProtocolObjectGroup> _protocolObjectGroups;

  private final Map<String, RouteMap> _routeMaps;

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

  public CiscoConfiguration() {
    _asPathAccessLists = new TreeMap<>();
    _cf = new CiscoFamily();
    _cryptoNamedRsaPubKeys = new TreeMap<>();
    _cryptoMapSets = new HashMap<>();
    _dhcpRelayServers = new ArrayList<>();
    _dnsServers = new TreeSet<>();
    _expandedCommunityLists = new TreeMap<>();
    _extendedAccessLists = new TreeMap<>();
    _extendedIpv6AccessLists = new TreeMap<>();
    _isakmpKeys = new ArrayList<>();
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
    _ciscoIosNats = new ArrayList<>();
    _networkObjectGroups = new TreeMap<>();
    _networkObjectInfos = new TreeMap<>();
    _networkObjects = new TreeMap<>();
    _objectGroups = new TreeMap<>();
    _prefixLists = new TreeMap<>();
    _prefix6Lists = new TreeMap<>();
    _protocolObjectGroups = new TreeMap<>();
    _routeMaps = new TreeMap<>();
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
                      ConcreteInterfaceAddress ifaceAddress = iface.getConcreteAddress();
                      if (ifaceAddress != null) {
                        int prefixLength = ifaceAddress.getNetworkBits();
                        Ip address = vrrpGroup.getVirtualAddress();
                        if (address != null) {
                          ConcreteInterfaceAddress virtualAddress =
                              ConcreteInterfaceAddress.create(address, prefixLength);
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
                      iface.addVrrpGroup(groupNum, newGroup);
                    });
          }
        });
  }

  private boolean containsIpAccessList(String eaListName, String mapName) {
    if (mapName == null || !_routeMaps.containsKey(mapName)) {
      return false;
    }
    return _routeMaps.get(mapName).getClauses().values().stream()
        .flatMap(clause -> clause.getMatchList().stream())
        .filter(line -> line instanceof RouteMapMatchIpAccessListLine)
        .anyMatch(
            line -> ((RouteMapMatchIpAccessListLine) line).getListNames().contains(eaListName));
  }

  private boolean containsIpv6AccessList(String eaListName, String mapName) {
    if (mapName == null || !_routeMaps.containsKey(mapName)) {
      return false;
    }
    return _routeMaps.get(mapName).getClauses().values().stream()
        .flatMap(clause -> clause.getMatchList().stream())
        .filter(line -> line instanceof RouteMapMatchIpv6AccessListLine)
        .anyMatch(
            line -> ((RouteMapMatchIpv6AccessListLine) line).getListNames().contains(eaListName));
  }

  public Map<String, IpAsPathAccessList> getAsPathAccessLists() {
    return _asPathAccessLists;
  }

  private Ip getBgpRouterId(Configuration c, String vrfName, BgpProcess proc) {
    Ip processRouterId = proc.getRouterId();
    if (processRouterId == null) {
      processRouterId = _vrfs.get(Configuration.DEFAULT_VRF_NAME).getBgpProcess().getRouterId();
    }
    if (processRouterId == null) {
      processRouterId = Ip.ZERO;
      for (Entry<String, org.batfish.datamodel.Interface> e :
          c.getAllInterfaces(vrfName).entrySet()) {
        String iname = e.getKey();
        org.batfish.datamodel.Interface iface = e.getValue();
        if (iname.startsWith("Loopback")) {
          ConcreteInterfaceAddress address = iface.getConcreteAddress();
          if (address != null) {
            Ip currentIp = address.getIp();
            if (currentIp.asLong() > processRouterId.asLong()) {
              processRouterId = currentIp;
            }
          }
        }
      }
      if (processRouterId.equals(Ip.ZERO)) {
        for (org.batfish.datamodel.Interface currentInterface :
            c.getAllInterfaces(vrfName).values()) {
          ConcreteInterfaceAddress address = currentInterface.getConcreteAddress();
          if (address != null) {
            Ip currentIp = address.getIp();
            if (currentIp.asLong() > processRouterId.asLong()) {
              processRouterId = currentIp;
            }
          }
        }
      }
    }
    return processRouterId;
  }

  public CiscoFamily getCf() {
    return _cf;
  }

  public Map<String, CryptoMapSet> getCryptoMapSets() {
    return _cryptoMapSets;
  }

  public Map<String, NamedRsaPubKey> getCryptoNamedRsaPubKeys() {
    return _cryptoNamedRsaPubKeys;
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

  @Override
  public String getHostname() {
    return _hostname;
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

  public List<IsakmpKey> getIsakmpKeys() {
    return _isakmpKeys;
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

  public @Nonnull Map<String, NatPool> getNatPools() {
    return _natPools;
  }

  public Map<String, IntegerSpace> getNamedVlans() {
    return _namedVlans;
  }

  public @Nonnull Set<String> getNatInside() {
    return _natInside;
  }

  public Set<String> getNatOutside() {
    return _natOutside;
  }

  public List<CiscoIosNat> getCiscoIosNats() {
    return _ciscoIosNats;
  }

  public String getNtpSourceInterface() {
    return _ntpSourceInterface;
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

  @Nullable
  private String getIOSSecurityZoneName(Interface iface) {
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
    if (ipv4) {
      if (updateSourceInterface != null) {
        org.batfish.datamodel.Interface sourceInterface =
            c.getAllInterfaces(vrfName).get(updateSourceInterface);
        if (sourceInterface != null) {
          ConcreteInterfaceAddress address = sourceInterface.getConcreteAddress();
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
          for (org.batfish.datamodel.Interface iface : c.getAllInterfaces(vrfName).values()) {
            for (ConcreteInterfaceAddress interfaceAddress : iface.getAllConcreteAddresses()) {
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

  public void setDnsSourceInterface(String dnsSourceInterface) {
    _dnsSourceInterface = dnsSourceInterface;
  }

  public void setDomainName(String domainName) {
    _domainName = domainName;
  }

  @Override
  public void setHostname(String hostname) {
    checkNotNull(hostname, "'hostname' cannot be null");
    _rawHostname = hostname;
    _hostname = hostname.toLowerCase();
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

  private org.batfish.datamodel.BgpProcess toBgpProcess(
      Configuration c, BgpProcess proc, String vrfName) {
    Ip bgpRouterId = getBgpRouterId(c, vrfName, proc);
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    org.batfish.datamodel.BgpProcess newBgpProcess =
        new org.batfish.datamodel.BgpProcess(bgpRouterId, ebgpAdmin, ibgpAdmin);
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

    // Global confederation config
    Long confederation = proc.getConfederation();
    if (confederation != null && !proc.getConfederationMembers().isEmpty()) {
      newBgpProcess.setConfederation(
          new BgpConfederation(confederation, proc.getConfederationMembers()));
    }

    // Populate process-level BGP aggregates
    proc.getAggregateNetworks().values().stream()
        .map(ipv4Aggregate -> toBgpAggregate(ipv4Aggregate, c))
        .forEach(newBgpProcess::addAggregate);
    // TODO Handle IPv6 aggregates

    /*
     * Create common BGP export policy. This policy's only function is to prevent export of
     * suppressed routes (contributors to summary-only aggregates).
     */
    RoutingPolicy.Builder bgpCommonExportPolicy =
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(Names.generatedBgpCommonExportPolicyName(vrfName));

    // Never export routes suppressed because they are more specific than summary-only aggregate
    Stream<Prefix> summaryOnlyNetworks =
        proc.getAggregateNetworks().entrySet().stream()
            .filter(e -> e.getValue().getSummaryOnly())
            .map(Entry::getKey);
    If suppressSummaryOnly = suppressSummarizedPrefixes(c, vrfName, summaryOnlyNetworks);
    if (suppressSummaryOnly != null) {
      bgpCommonExportPolicy.addStatement(suppressSummaryOnly);
    }

    // Finalize common export policy
    bgpCommonExportPolicy.addStatement(Statements.ReturnTrue.toStaticStatement()).build();

    // Create BGP redistribution policy to import main RIB routes into BGP RIB
    String redistPolicyName = generatedBgpRedistributionPolicyName(vrfName);
    RoutingPolicy.Builder redistributionPolicy =
        RoutingPolicy.builder().setOwner(c).setName(redistPolicyName);

    // Redistribute routes
    Stream.of(
            RoutingProtocol.RIP,
            RoutingProtocol.STATIC,
            RoutingProtocol.CONNECTED,
            RoutingProtocol.OSPF,
            RoutingProtocol.EIGRP)
        .map(redistProtocol -> matchRedistributedRoutes(proc, redistProtocol))
        .filter(Objects::nonNull)
        .forEach(redistributionPolicy::addStatement);

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
                  .add(
                      new Not(
                          new MatchProtocol(
                              RoutingProtocol.BGP,
                              RoutingProtocol.IBGP,
                              RoutingProtocol.AGGREGATE)));
              Optional.ofNullable(bgpNetwork.getRouteMapName())
                  .map(_routeMaps::get)
                  .ifPresent(
                      rm -> exportNetworkConditions.getConjuncts().add(new CallExpr(rm.getName())));
              redistributionPolicy.addStatement(
                  new If(
                      "Add network statement routes to BGP",
                      exportNetworkConditions,
                      ImmutableList.of(
                          new SetOrigin(new LiteralOrigin(OriginType.IGP, null)),
                          Statements.ExitAccept.toStaticStatement())));
            });
    if (!proc.getIpv6Networks().isEmpty()) {
      String localFilter6Name = "~BGP_NETWORK6_NETWORKS_FILTER:" + vrfName + "~";
      Route6FilterList localFilter6 = new Route6FilterList(localFilter6Name);
      proc.getIpv6Networks()
          .forEach(
              (prefix6, bgpNetwork6) -> {
                int prefixLen = prefix6.getPrefixLength();
                Route6FilterLine line =
                    new Route6FilterLine(LineAction.PERMIT, prefix6, SubRange.singleton(prefixLen));
                localFilter6.addLine(line);
                String mapName = bgpNetwork6.getRouteMapName();
                if (mapName != null) {
                  RouteMap routeMap = _routeMaps.get(mapName);
                  if (routeMap != null) {
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
                        .add(
                            new Not(
                                new MatchProtocol(
                                    RoutingProtocol.BGP,
                                    RoutingProtocol.IBGP,
                                    RoutingProtocol.AGGREGATE)));
                    exportNetwork6Conditions.getConjuncts().add(new CallExpr(mapName));
                    redistributionPolicy.addStatement(
                        new If(
                            "Add network 6 statement routes to BGP",
                            exportNetwork6Conditions,
                            ImmutableList.of(
                                new SetOrigin(new LiteralOrigin(OriginType.IGP, null)),
                                Statements.ExitAccept.toStaticStatement())));
                  }
                }
              });
      c.getRoute6FilterLists().put(localFilter6Name, localFilter6);
    }

    // Finalize redistribution policy and attach to process
    redistributionPolicy.addStatement(Statements.ExitReject.toStaticStatement()).build();
    newBgpProcess.setRedistributionPolicy(redistPolicyName);

    for (LeafBgpPeerGroup lpg : leafGroups) {
      if (!lpg.getActive() || lpg.getShutdown()) {
        continue;
      }
      if (lpg.getRemoteAs() == null) {
        _w.redFlag("No remote-as set for peer: " + lpg.getName());
        continue;
      }
      if (lpg instanceof Ipv6BgpPeerGroup || lpg instanceof DynamicIpv6BgpPeerGroup) {
        // TODO: implement ipv6 bgp neighbors
        continue;
      }
      // update source
      String updateSourceInterface = lpg.getUpdateSource();
      boolean ipv4 = lpg.getNeighborPrefix() != null;
      Ip updateSource = getUpdateSource(c, vrfName, lpg, updateSourceInterface, ipv4);

      // Get default-originate generation policy
      String defaultOriginateGenerationMap = null;
      if (lpg.getDefaultOriginate()) {
        defaultOriginateGenerationMap = lpg.getDefaultOriginateMap();
      }

      // Generate import and export policies
      String peerImportPolicyName = generateBgpImportPolicy(lpg, vrfName, c, _w);
      generateBgpExportPolicy(lpg, vrfName, ipv4, c, _w);

      // If defaultOriginate is set, create default route for this peer group
      GeneratedRoute.Builder defaultRoute = null;
      GeneratedRoute6.Builder defaultRoute6;
      if (lpg.getDefaultOriginate()) {
        defaultRoute = GeneratedRoute.builder();
        defaultRoute.setNetwork(Prefix.ZERO);
        defaultRoute.setAdmin(MAX_ADMINISTRATIVE_COST);
        defaultRoute6 = new GeneratedRoute6.Builder();
        defaultRoute6.setNetwork(Prefix6.ZERO);
        defaultRoute6.setAdmin(MAX_ADMINISTRATIVE_COST);

        if (defaultOriginateGenerationMap != null
            && c.getRoutingPolicies().containsKey(defaultOriginateGenerationMap)) {
          // originate contingent on generation policy
          if (ipv4) {
            defaultRoute.setGenerationPolicy(defaultOriginateGenerationMap);
          } else {
            defaultRoute6.setGenerationPolicy(defaultOriginateGenerationMap);
          }
        }
      }

      Ip clusterId = lpg.getClusterId();
      if (clusterId == null) {
        clusterId = bgpRouterId;
      }
      String description = lpg.getDescription();
      Long pgLocalAs = lpg.getLocalAs();
      long localAs = pgLocalAs != null ? pgLocalAs : proc.getProcnum();

      BgpPeerConfig.Builder<?, ?> newNeighborBuilder;
      if (lpg instanceof IpBgpPeerGroup) {
        IpBgpPeerGroup ipg = (IpBgpPeerGroup) lpg;
        newNeighborBuilder =
            BgpActivePeerConfig.builder()
                .setPeerAddress(ipg.getIp())
                .setRemoteAsns(
                    Optional.ofNullable(lpg.getRemoteAs())
                        .map(LongSpace::of)
                        .orElse(LongSpace.EMPTY));
      } else if (lpg instanceof DynamicIpBgpPeerGroup) {
        DynamicIpBgpPeerGroup dpg = (DynamicIpBgpPeerGroup) lpg;
        LongSpace.Builder asns = LongSpace.builder().including(dpg.getRemoteAs());
        Optional.ofNullable(dpg.getAlternateAs()).ifPresent(asns::includingAll);
        newNeighborBuilder =
            BgpPassivePeerConfig.builder()
                .setPeerPrefix(dpg.getPrefix())
                .setRemoteAsns(asns.build());
      } else {
        throw new VendorConversionException("Invalid BGP leaf neighbor type");
      }
      newNeighborBuilder.setBgpProcess(newBgpProcess);
      newNeighborBuilder.setConfederation(proc.getConfederation());
      newNeighborBuilder.setEnforceFirstAs(firstNonNull(proc.getEnforceFirstAs(), Boolean.TRUE));

      AddressFamilyCapabilities ipv4AfSettings =
          AddressFamilyCapabilities.builder()
              .setAdditionalPathsReceive(lpg.getAdditionalPathsReceive())
              .setAdditionalPathsSelectAll(lpg.getAdditionalPathsSelectAll())
              .setAdditionalPathsSend(lpg.getAdditionalPathsSend())
              .setAllowLocalAsIn(lpg.getAllowAsIn())
              .setAllowRemoteAsOut(ALWAYS) /* no outgoing remote-as check on IOS */
              /*
               * On Cisco IOS, advertise-inactive is true by default. This can be modified by
               * "bgp suppress-inactive" command,
               * which we currently do not parse/extract. So we choose the default value here.
               *
               * For other Cisco OS variations (e.g., IOS-XR) we did not find a similar command and for now,
               * we assume behavior to be identical to IOS family.
               */
              .setAdvertiseInactive(true)
              .setSendCommunity(lpg.getSendCommunity())
              .setSendExtendedCommunity(lpg.getSendExtendedCommunity())
              .build();
      newNeighborBuilder.setIpv4UnicastAddressFamily(
          Ipv4UnicastAddressFamily.builder()
              .setAddressFamilyCapabilities(ipv4AfSettings)
              .setImportPolicy(peerImportPolicyName)
              .setExportPolicy(Names.generatedBgpPeerExportPolicyName(vrfName, lpg.getName()))
              .setRouteReflectorClient(lpg.getRouteReflectorClient())
              .build());
      newNeighborBuilder.setClusterId(clusterId.asLong());
      newNeighborBuilder.setDefaultMetric(proc.getDefaultMetric());
      newNeighborBuilder.setDescription(description);
      newNeighborBuilder.setEbgpMultihop(lpg.getEbgpMultihop());
      if (defaultRoute != null) {
        newNeighborBuilder.setGeneratedRoutes(ImmutableSet.of(defaultRoute.build()));
      }
      newNeighborBuilder.setGroup(lpg.getGroupName());
      newNeighborBuilder.setLocalAs(localAs);
      newNeighborBuilder.setLocalIp(updateSource);
      newNeighborBuilder.build();
    }
    return newBgpProcess;
  }

  /**
   * Creates a {@link Statement} that matches and accepts routes of the given protocol that should
   * be redistributed into the given {@link BgpProcess}. Returns {@code null} if no routes of this
   * protocol should be redistributed, i.e. if the BGP process is not configured to redistribute it
   * or the redistribution route-map is undefined.
   */
  private @Nullable Statement matchRedistributedRoutes(
      BgpProcess bgpProcess, RoutingProtocol srcProtocol) {
    RedistributionPolicy redistributionPolicy =
        bgpProcess.getRedistributionPolicies().get(srcProtocol);
    if (redistributionPolicy == null) {
      // Process does not redistribute this protocol
      return null;
    }
    String mapName = redistributionPolicy.getRouteMap();
    if (mapName != null && !_routeMaps.containsKey(mapName)) {
      // Route-map is undefined. No redistribution will occur.
      return null;
    }
    MatchProtocol matchProtocol;
    switch (srcProtocol) {
      case RIP:
      case STATIC:
      case CONNECTED:
        matchProtocol = new MatchProtocol(srcProtocol);
        break;
      case OSPF:
        matchProtocol =
            firstNonNull(
                (MatchProtocol)
                    redistributionPolicy
                        .getSpecialAttributes()
                        .get(BgpRedistributionPolicy.OSPF_ROUTE_TYPES),
                // No match type means internal routes only, at least on IOS.
                // https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/5242-bgp-ospf-redis.html#redistributionofonlyospfinternalroutesintobgp
                new MatchProtocol(
                    RoutingProtocol.OSPF, RoutingProtocol.OSPF_IA, RoutingProtocol.OSPF_IS));
        break;
      case EIGRP:
        // key EIGRP indicates redist external too; EIGRP_EX is never used as a key
        matchProtocol = new MatchProtocol(RoutingProtocol.EIGRP, RoutingProtocol.EIGRP_EX);
        break;
      default:
        throw new IllegalStateException(
            String.format("Unexpected protocol for BGP redistribution: %s", srcProtocol));
    }
    List<BooleanExpr> matchConjuncts =
        Stream.of(
                matchProtocol,
                bgpProcess.getDefaultInformationOriginate() ? null : NOT_DEFAULT_ROUTE,
                mapName == null ? null : new CallExpr(mapName))
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    Conjunction redistExpr = new Conjunction(matchConjuncts);
    redistExpr.setComment(String.format("Redistribute %s routes into BGP", srcProtocol));
    return new If(redistExpr, ImmutableList.of(Statements.ExitAccept.toStaticStatement()));
  }

  private static final Pattern INTERFACE_WITH_SUBINTERFACE = Pattern.compile("^(.*)\\.(\\d+)$");

  /**
   * Get the {@link OspfNetwork} in the specified {@link OspfProcess} containing the specified
   * {@link Interface}'s address
   *
   * <p>Returns {@code null} if the interface address is {@code null} or the interface address does
   * not overlap with any {@link OspfNetwork} in the specified {@link OspfProcess}
   */
  private static @Nullable OspfNetwork getOspfNetworkForInterface(
      Interface iface, OspfProcess process) {
    ConcreteInterfaceAddress interfaceAddress = iface.getAddress();
    if (interfaceAddress == null) {
      // Iface has no IP address / isn't associated with a network in this OSPF process
      return null;
    }

    // Sort networks with longer prefixes first, then lower start IPs and areas
    SortedSet<OspfNetwork> networks =
        ImmutableSortedSet.copyOf(
            Comparator.<OspfNetwork>comparingInt(n -> n.getPrefix().getPrefixLength())
                .reversed()
                .thenComparing(n -> n.getPrefix().getStartIp())
                .thenComparingLong(OspfNetwork::getArea),
            process.getNetworks());
    for (OspfNetwork network : networks) {
      Prefix networkPrefix = network.getPrefix();
      Ip networkAddress = networkPrefix.getStartIp();
      Ip maskedInterfaceAddress =
          interfaceAddress.getIp().getNetworkAddress(networkPrefix.getPrefixLength());
      if (maskedInterfaceAddress.equals(networkAddress)) {
        // Found a longest prefix match, so found the network in this OSPF process for the iface
        return network;
      }
    }
    return null;
  }

  /**
   * Get the {@link OspfProcess} corresponding to the specified {@link Interface}
   *
   * <p>Returns {@code null} if the {@link Interface} does not have an {@link OspfProcess}
   * explicitly associated with it and does not overlap with an {@link OspfNetwork} in any {@link
   * OspfProcess} in the specified {@link Vrf}
   */
  private static @Nullable OspfProcess getOspfProcessForInterface(Vrf vrf, Interface iface) {
    if (iface.getOspfProcess() != null) {
      return vrf.getOspfProcesses().get(iface.getOspfProcess());
    }
    return vrf.getOspfProcesses().values().stream()
        .filter(p -> getOspfNetworkForInterface(iface, p) != null)
        .findFirst()
        .orElse(null);
  }

  private org.batfish.datamodel.Interface toInterface(
      String ifaceName, Interface iface, Configuration c) {
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(ifaceName)
            .setOwner(c)
            .setType(computeInterfaceType(iface.getName(), c.getConfigurationFormat()))
            .build();
    String vrfName = iface.getVrf();
    Vrf vrf = _vrfs.computeIfAbsent(vrfName, Vrf::new);
    newIface.setDescription(iface.getDescription());
    newIface.setActive(iface.getActive());
    newIface.setChannelGroup(iface.getChannelGroup());
    newIface.setCryptoMap(iface.getCryptoMap());
    newIface.setHsrpGroups(
        CollectionUtil.toImmutableMap(
            iface.getHsrpGroups(), Entry::getKey, e -> CiscoConversions.toHsrpGroup(e.getValue())));
    newIface.setHsrpVersion(iface.getHsrpVersion());
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
    newIface.setMtu(iface.getMtu());
    newIface.setProxyArp(iface.getProxyArp());
    newIface.setDeclaredNames(ImmutableSortedSet.copyOf(iface.getDeclaredNames()));
    newIface.setSwitchport(iface.getSwitchport());

    if (newIface.getSwitchport()) {
      newIface.setSwitchportMode(iface.getSwitchportMode());

      // switch settings
      if (iface.getSwitchportMode() == SwitchportMode.ACCESS) {
        newIface.setAccessVlan(firstNonNull(iface.getAccessVlan(), 1));
      }

      if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
        SwitchportEncapsulationType encapsulation =
            firstNonNull(
                // TODO: check if this is OK
                iface.getSwitchportTrunkEncapsulation(), SwitchportEncapsulationType.DOT1Q);
        newIface.setSwitchportTrunkEncapsulation(encapsulation);
        if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
          /*
           * Compute allowed VLANs:
           * - If allowed VLANs are set, honor them;
           */
          if (iface.getAllowedVlans() != null) {
            newIface.setAllowedVlans(iface.getAllowedVlans());
          } else {
            newIface.setAllowedVlans(Interface.ALL_VLANS);
          }
        }
        newIface.setNativeVlan(firstNonNull(iface.getNativeVlan(), 1));
      }

      newIface.setSpanningTreePortfast(iface.getSpanningTreePortfast());
    } else {
      newIface.setSwitchportMode(SwitchportMode.NONE);
      if (newIface.getInterfaceType() == InterfaceType.VLAN) {
        newIface.setVlan(CommonUtil.getInterfaceVlanNumber(ifaceName));
        newIface.setAutoState(iface.getAutoState());
      }

      // All prefixes is the combination of the interface prefix + any secondary prefixes.
      ImmutableSet.Builder<InterfaceAddress> allPrefixes = ImmutableSet.builder();
      if (iface.getAddress() != null) {
        newIface.setAddress(iface.getAddress());
        allPrefixes.add(iface.getAddress());
      }
      allPrefixes.addAll(iface.getSecondaryAddresses());
      newIface.setAllAddresses(allPrefixes.build());

      // subinterface settings
      newIface.setEncapsulationVlan(iface.getEncapsulationVlan());
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
              .getOrDefault(iface.getName(), eigrpProcess.getPassiveInterfaceDefault());

      // Export distribute lists
      String exportPolicyName =
          eigrpNeighborExportPolicyName(ifaceName, vrfName, eigrpProcess.getAsn());
      RoutingPolicy exportPolicy =
          generateEigrpPolicy(
              c,
              this,
              Arrays.asList(
                  eigrpProcess.getOutboundGlobalDistributeList(),
                  eigrpProcess.getOutboundInterfaceDistributeLists().get(ifaceName)),
              ImmutableList.of(matchOwnAsn(eigrpProcess.getAsn())),
              exportPolicyName);
      c.getRoutingPolicies().put(exportPolicyName, exportPolicy);

      // Import distribute lists
      String importPolicyName =
          eigrpNeighborImportPolicyName(ifaceName, vrfName, eigrpProcess.getAsn());
      RoutingPolicy importPolicy =
          generateEigrpPolicy(
              c,
              this,
              Arrays.asList(
                  eigrpProcess.getInboundGlobalDistributeList(),
                  eigrpProcess.getInboundInterfaceDistributeLists().get(ifaceName)),
              ImmutableList.of(),
              importPolicyName);
      c.getRoutingPolicies().put(importPolicyName, importPolicy);

      newIface.setEigrp(
          EigrpInterfaceSettings.builder()
              .setAsn(eigrpProcess.getAsn())
              .setEnabled(true)
              .setExportPolicy(exportPolicyName)
              .setImportPolicy(importPolicyName)
              .setMetric(computeEigrpMetricForInterface(iface, eigrpProcess.getMode()))
              .setPassive(passive)
              .build());
      if (newIface.getEigrp() == null) {
        _w.redFlag("Interface: '" + iface.getName() + "' failed to set EIGRP settings");
      }
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

    String incomingFilterName = iface.getIncomingFilter();
    if (incomingFilterName != null) {
      newIface.setIncomingFilter(c.getIpAccessLists().get(incomingFilterName));
    }
    String outgoingFilterName = iface.getOutgoingFilter();
    if (outgoingFilterName != null) {
      newIface.setOutgoingFilter(c.getIpAccessLists().get(outgoingFilterName));
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

    List<CiscoIosNat> ciscoIosNats = firstNonNull(_ciscoIosNats, ImmutableList.of());
    if (!ciscoIosNats.isEmpty()) {
      generateCiscoIosNatTransformations(ifaceName, vrfName, newIface, c);
    }

    String routingPolicyName = iface.getRoutingPolicy();
    if (routingPolicyName != null) {
      newIface.setPacketPolicy(routingPolicyName);
    }

    // For IOS and XR, FirewallSessionInterfaceInfo is created once for all NAT interfaces.
    return newIface;
  }

  public static String eigrpNeighborImportPolicyName(String ifaceName, String vrfName, Long asn) {
    return String.format("~EIGRP_IMPORT_POLICY_%s_%s_%s~", vrfName, asn, ifaceName);
  }

  public static String eigrpNeighborExportPolicyName(String ifaceName, String vrfName, Long asn) {
    return String.format("~EIGRP_EXPORT_POLICY_%s_%s_%s~", vrfName, asn, ifaceName);
  }

  @Nonnull
  private EigrpMetric computeEigrpMetricForInterface(Interface iface, EigrpProcessMode mode) {
    Long bw =
        Stream.of(iface.getBandwidth(), Interface.getDefaultBandwidth(iface.getName(), _vendor))
            .filter(Objects::nonNull)
            .findFirst()
            .map(bandwidth -> bandwidth.longValue() / 1000) // convert to kbps
            .orElse(null);
    // Bandwidth can be null for port-channels (will be calculated later).
    if (bw == null) {
      InterfaceType ifaceType = computeInterfaceType(iface.getName(), _vendor);
      assert ifaceType == InterfaceType.AGGREGATED || ifaceType == InterfaceType.AGGREGATE_CHILD;
    }
    EigrpMetricValues values =
        EigrpMetricValues.builder()
            .setDelay(
                firstNonNull(
                    iface.getDelay(), Interface.getDefaultDelay(iface.getName(), _vendor, bw)))
            .setBandwidth(bw)
            .build();
    if (mode == EigrpProcessMode.CLASSIC) {
      return ClassicMetric.builder().setValues(values).build();
    } else if (mode == EigrpProcessMode.NAMED) {
      return WideMetric.builder().setValues(values).build();
    } else {
      throw new IllegalArgumentException("Invalid EIGRP process mode: " + mode);
    }
  }

  private void generateCiscoIosNatTransformations(
      String ifaceName, String vrfName, org.batfish.datamodel.Interface newIface, Configuration c) {
    if (!getNatOutside().contains(ifaceName)) {
      return;
    }

    // Convert the IOS NATs to a mapping of transformations. Each field (source or destination)
    // can be modified independently but not jointly. A single CiscoIosNat can represent an incoming
    // NAT, an outgoing NAT, or both.
    Map<CiscoIosNat, Transformation.Builder> convertedIncomingNats = new HashMap<>();
    Map<CiscoIosNat, Transformation.Builder> convertedOutgoingNats = new HashMap<>();
    for (CiscoIosNat nat : getCiscoIosNats()) {
      // Filter to NAT rules in this interface's VRF
      if (!firstNonNull(nat.getVrf(), Configuration.DEFAULT_VRF_NAME).equals(vrfName)) {
        continue;
      }
      nat.toIncomingTransformation(
              ifaceName, c.getIpAccessLists(), _routeMaps, _natPools, _interfaces, _w)
          .ifPresent(incoming -> convertedIncomingNats.put(nat, incoming));
      nat.toOutgoingTransformation(
              ifaceName, _routeMaps, _natPools, getNatInside(), _interfaces, c, _w)
          .ifPresent(outgoing -> convertedOutgoingNats.put(nat, outgoing));
    }

    if (!convertedIncomingNats.isEmpty()) {
      newIface.setIncomingTransformation(
          CiscoIosNatUtil.toIncomingTransformationChain(convertedIncomingNats));
    }
    if (!convertedOutgoingNats.isEmpty()) {
      newIface.setOutgoingTransformation(
          CiscoIosNatUtil.toOutgoingTransformationChain(convertedOutgoingNats));
    }
  }

  private List<org.batfish.datamodel.StaticRoute> generateIosNatAddRouteRoutes() {
    return getCiscoIosNats().stream()
        .map(CiscoIosNat::toRoute)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(CiscoConversions::toStaticRoute)
        .collect(ImmutableList.toImmutableList());
  }

  private void applyZoneFilter(
      Interface iface, org.batfish.datamodel.Interface newIface, Configuration c) {
    if (getIOSSecurityZoneName(iface) != null) {
      applyIOSSecurityZoneFilter(iface, newIface, c);
    }
  }

  private void applyIOSSecurityZoneFilter(
      Interface iface, org.batfish.datamodel.Interface newIface, Configuration c) {
    String zoneName =
        checkNotNull(
            getIOSSecurityZoneName(iface),
            "interface %s is not in a security zone name",
            iface.getName());
    String zoneOutgoingAclName = computeZoneOutgoingAclName(zoneName);
    IpAccessList zoneOutgoingAcl = c.getIpAccessLists().get(zoneOutgoingAclName);
    if (zoneOutgoingAcl == null) {
      return;
    }
    IpAccessList oldOutgoingFilter = newIface.getOutgoingFilter();
    if (oldOutgoingFilter == null) {
      // No interface outbound filter
      newIface.setOutgoingFilter(zoneOutgoingAcl);
      return;
    }

    // Construct a new ACL that combines filters, i.e. 1 AND 2
    // 1) the interface outbound filter, if it exists
    // 2) the zone filter
    String oldOutgoingFilterName = oldOutgoingFilter.getName();
    IpAccessList combinedOutgoingAcl =
        IpAccessList.builder()
            .setOwner(c)
            .setName(computeCombinedOutgoingAclName(newIface.getName()))
            .setLines(
                ImmutableList.of(
                    ExprAclLine.accepting()
                        .setMatchCondition(
                            new AndMatchExpr(
                                ImmutableList.of(
                                    new PermittedByAcl(zoneOutgoingAclName),
                                    new PermittedByAcl(oldOutgoingFilterName)),
                                String.format(
                                    "Permit if permitted by policy for zone '%s' and permitted by"
                                        + " outgoing filter '%s'",
                                    zoneName, oldOutgoingFilterName)))
                        .build()))
            .build();
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
          .add(new MatchProtocol(RoutingProtocol.EIGRP, RoutingProtocol.EIGRP_EX));
    } else if (protocol == RoutingProtocol.ISIS_ANY) {
      ospfExportConditions
          .getConjuncts()
          .add(
              new MatchProtocol(
                  RoutingProtocol.ISIS_EL1,
                  RoutingProtocol.ISIS_EL2,
                  RoutingProtocol.ISIS_L1,
                  RoutingProtocol.ISIS_L2));
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
    Ip routerId = proc.getRouterId();
    if (routerId == null) {
      routerId = CiscoConversions.getHighestIp(oldConfig.getInterfaces());
      if (routerId == Ip.ZERO) {
        _w.redFlag("No candidates for OSPF router-id");
        return null;
      }
    }
    org.batfish.datamodel.ospf.OspfProcess newProcess =
        org.batfish.datamodel.ospf.OspfProcess.builder()
            .setProcessId(proc.getName())
            .setReferenceBandwidth(proc.getReferenceBandwidth())
            .setAdminCosts(
                org.batfish.datamodel.ospf.OspfProcess.computeDefaultAdminCosts(
                    c.getConfigurationFormat()))
            .setSummaryAdminCost(
                RoutingProtocol.OSPF_IA.getSummaryAdministrativeCost(c.getConfigurationFormat()))
            .setRouterId(routerId)
            .build();

    if (proc.getMaxMetricRouterLsa()) {
      newProcess.setMaxMetricTransitLinks(OspfProcess.MAX_METRIC_ROUTER_LSA);
      if (proc.getMaxMetricIncludeStub()) {
        newProcess.setMaxMetricStubNetworks(OspfProcess.MAX_METRIC_ROUTER_LSA);
      }
      newProcess.setMaxMetricExternalNetworks(proc.getMaxMetricExternalLsa());
      newProcess.setMaxMetricSummaryNetworks(proc.getMaxMetricSummaryLsa());
    }

    // establish areas and associated interfaces
    Map<Long, OspfArea.Builder> areas = new HashMap<>();
    Map<Long, ImmutableSortedSet.Builder<String>> areaInterfacesBuilders = new HashMap<>();

    // Set RFC 1583 compatibility
    newProcess.setRfc1583Compatible(proc.getRfc1583Compatible());

    for (Entry<String, org.batfish.datamodel.Interface> e :
        c.getAllInterfaces(vrfName).entrySet()) {
      org.batfish.datamodel.Interface iface = e.getValue();
      /*
       * Filter out interfaces that do not belong to this process, however if the process name is missing,
       * proceed down to inference based on network addresses.
       */
      Interface vsIface = _interfaces.get(iface.getName());
      if (vsIface.getOspfProcess() != null && !vsIface.getOspfProcess().equals(proc.getName())) {
        continue;
      }
      OspfNetwork network = getOspfNetworkForInterface(vsIface, proc);
      if (vsIface.getOspfProcess() == null && network == null) {
        // Interface is not in an OspfNetwork on this process
        continue;
      }

      String ifaceName = e.getKey();
      Long areaNum = vsIface.getOspfArea();
      // OSPF area number was not configured on the interface itself, so get from OspfNetwork
      if (areaNum == null) {
        if (network == null) {
          continue;
        }
        areaNum = network.getArea();
      }
      areas.computeIfAbsent(areaNum, areaNumber -> OspfArea.builder().setNumber(areaNumber));
      ImmutableSortedSet.Builder<String> newAreaInterfacesBuilder =
          areaInterfacesBuilders.computeIfAbsent(areaNum, n -> ImmutableSortedSet.naturalOrder());
      newAreaInterfacesBuilder.add(ifaceName);
      finalizeInterfaceOspfSettings(iface, vsIface, proc, areaNum);
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
            summary.isAdvertised()
                ? Math.min(Prefix.MAX_PREFIX_LENGTH, prefixLength + 1)
                : prefixLength;
        summaryFilter.addLine(
            new RouteFilterLine(
                LineAction.DENY,
                IpWildcard.create(prefix),
                new SubRange(filterMinPrefixLength, Prefix.MAX_PREFIX_LENGTH)));
      }
      area.addSummaries(ImmutableSortedMap.copyOf(summaries));
      summaryFilter.addLine(
          new RouteFilterLine(
              LineAction.PERMIT,
              IpWildcard.create(Prefix.ZERO),
              new SubRange(0, Prefix.MAX_PREFIX_LENGTH)));
    }
    newProcess.setAreas(toImmutableSortedMap(areas, Entry::getKey, e -> e.getValue().build()));

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
      List<Statement> ospfExportDefaultStatements = ospfExportDefault.getTrueStatements();
      long metric = proc.getDefaultInformationMetric();
      ospfExportDefaultStatements.add(new SetMetric(new LiteralLong(metric)));
      OspfMetricType metricType = proc.getDefaultInformationMetricType();
      ospfExportDefaultStatements.add(new SetOspfMetricType(metricType));
      // add default export map with metric
      String defaultOriginateMapName = proc.getDefaultInformationOriginateMap();
      GeneratedRoute.Builder route =
          GeneratedRoute.builder()
              .setNetwork(Prefix.ZERO)
              .setNonRouting(true)
              .setAdmin(MAX_ADMINISTRATIVE_COST);
      if (defaultOriginateMapName != null) {
        RoutingPolicy ospfDefaultGenerationPolicy =
            c.getRoutingPolicies().get(defaultOriginateMapName);
        if (ospfDefaultGenerationPolicy != null) {
          // TODO This should depend on a default route existing, unless `always` is configured
          // If `always` is configured, maybe the route-map should be ignored. Needs GNS3 check.
          route.setGenerationPolicy(defaultOriginateMapName);
          newProcess.addGeneratedRoute(route.build());
        }
      } else if (proc.getDefaultInformationOriginateAlways()) {
        // add generated aggregate with no precondition
        newProcess.addGeneratedRoute(route.build());
      } else {
        // Use a generated route that will only be generated if a default route exists in RIB
        String defaultRouteGenerationPolicyName =
            computeOspfDefaultRouteGenerationPolicyName(vrfName, proc.getName());
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(defaultRouteGenerationPolicyName)
            .addStatement(
                new If(
                    matchDefaultRoute(),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement())))
            .build();
        route.setGenerationPolicy(defaultRouteGenerationPolicyName);
        newProcess.addGeneratedRoute(route.build());
      }
      ospfExportDefaultStatements.add(Statements.ExitAccept.toStaticStatement());
      ospfExportDefault.setGuard(
          new Conjunction(
              ImmutableList.of(matchDefaultRoute(), new MatchProtocol(RoutingProtocol.AGGREGATE))));
    }

    computeDistributeListPolicies(proc, newProcess, c, vrfName, proc.getName(), oldConfig, _w);

    // policies for redistributing routes
    ospfExportStatements.addAll(
        proc.getRedistributionPolicies().values().stream()
            .map(policy -> convertOspfRedistributionPolicy(policy, proc))
            .collect(Collectors.toList()));

    return newProcess;
  }

  /** Setup OSPF settings on specified VI interface. */
  private void finalizeInterfaceOspfSettings(
      org.batfish.datamodel.Interface iface,
      Interface vsIface,
      @Nullable OspfProcess proc,
      @Nullable Long areaNum) {
    String ifaceName = vsIface.getName();
    OspfInterfaceSettings.Builder ospfSettings = OspfInterfaceSettings.builder().setPassive(false);
    if (proc != null) {
      ospfSettings.setProcess(proc.getName());
      if (firstNonNull(
          vsIface.getOspfPassive(),
          proc.getPassiveInterfaces().contains(ifaceName)
              || (proc.getPassiveInterfaceDefault()
                  ^ proc.getNonDefaultInterfaces().contains(ifaceName)))) {
        proc.getPassiveInterfaces().add(ifaceName);
        ospfSettings.setPassive(true);
      }
    }
    ospfSettings.setHelloMultiplier(vsIface.getOspfHelloMultiplier());

    ospfSettings.setAreaName(areaNum);
    ospfSettings.setEnabled(proc != null && areaNum != null && !vsIface.getOspfShutdown());
    org.batfish.datamodel.ospf.OspfNetworkType networkType =
        toOspfNetworkType(vsIface.getOspfNetworkType(), _w);
    ospfSettings.setNetworkType(networkType);
    if (vsIface.getOspfCost() == null
        && iface.isLoopback()
        && networkType != OspfNetworkType.POINT_TO_POINT) {
      ospfSettings.setCost(DEFAULT_LOOPBACK_OSPF_COST);
    } else {
      ospfSettings.setCost(vsIface.getOspfCost());
    }
    ospfSettings.setHelloInterval(toOspfHelloInterval(vsIface, networkType));
    ospfSettings.setDeadInterval(toOspfDeadInterval(vsIface, networkType));

    iface.setOspfSettings(ospfSettings.build());
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
        .setSuppressType7(nssaSettings.getNoRedistribution())
        .build();
  }

  private org.batfish.datamodel.RipProcess toRipProcess(
      RipProcess proc, String vrfName, Configuration c) {
    org.batfish.datamodel.RipProcess newProcess = new org.batfish.datamodel.RipProcess();

    // establish areas and associated interfaces
    SortedSet<Prefix> networks = proc.getNetworks();
    for (Entry<String, org.batfish.datamodel.Interface> e :
        c.getAllInterfaces(vrfName).entrySet()) {
      String ifaceName = e.getKey();
      org.batfish.datamodel.Interface i = e.getValue();
      ConcreteInterfaceAddress interfaceAddress = i.getConcreteAddress();
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
        i.setRipPassive(passive);
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
      ripExportDefaultConditions.getConjuncts().add(matchDefaultRoute());
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

  @VisibleForTesting
  RoutingPolicy toRoutingPolicy(Configuration c, RouteMap map) {
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
      List<Statement> matchStatements = new LinkedList<>();
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
      ifExpr.setTrueStatements(
          ImmutableList.of(
              makeClauseTraceable(matchStatements, clauseNumber, map.getName(), _filename)));
      followingClause = ifExpr;
    }
    statements.add(followingClause);
    return output;
  }

  @VisibleForTesting
  RoutingPolicy toRoutingPolicies(Configuration c, RouteMap map) {
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
      ifStatement.setTrueStatements(
          ImmutableList.of(
              makeClauseTraceable(onMatchStatements, clauseNumber, map.getName(), _filename)));
      followingClause = clausePolicy;
      followingClauseNumber = clauseNumber;
    }
    statements.add(new CallStatement(followingClause.getName()));
    return output;
  }

  @VisibleForTesting
  static TraceableStatement makeClauseTraceable(
      List<Statement> matchStatements, int clauseNumber, String mapName, String filename) {
    return new TraceableStatement(
        TraceElement.builder()
            .add("Matched ")
            .add(
                String.format("route-map %s clause %d", mapName, clauseNumber),
                new VendorStructureId(
                    filename,
                    CiscoStructureType.ROUTE_MAP_CLAUSE.getDescription(),
                    computeRouteMapClauseName(mapName, clauseNumber)))
            .build(),
        matchStatements);
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
    Configuration c = new Configuration(_hostname, _vendor);
    c.setHumanName(_rawHostname);
    // Only set CISCO_UNSPECIFIED if the device is actually a cisco device
    if (_vendor == ConfigurationFormat.CISCO_IOS) {
      c.setDeviceModel(DeviceModel.CISCO_UNSPECIFIED);
    }
    c.getVendorFamily().setCisco(_cf);
    c.setDefaultInboundAction(LineAction.PERMIT);
    c.setDefaultCrossZoneAction(LineAction.PERMIT);
    c.setDnsServers(_dnsServers);
    c.setDnsSourceInterface(_dnsSourceInterface);
    c.setDomainName(_domainName);
    c.setExportBgpFromBgpRib(true);
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

    // Build static route resolution policy used by VRFs; prevents resolution w/ default-routes
    RoutingPolicy.builder()
        .setOwner(c)
        .setName(RESOLUTION_POLICY_NAME)
        .setStatements(
            ImmutableList.of(
                new If(
                    matchDefaultRoute(),
                    ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement()))))
        .build();

    // initialize vrfs
    for (String vrfName : _vrfs.keySet()) {
      c.getVrfs()
          .put(
              vrfName,
              org.batfish.datamodel.Vrf.builder()
                  .setName(vrfName)
                  .setResolutionPolicy(RESOLUTION_POLICY_NAME)
                  .build());
      // inherit address family props
      Vrf vrf = _vrfs.get(vrfName);

      VrfAddressFamily ip4uaf = vrf.getIpv4UnicastAddressFamily();
      if (ip4uaf == null) {
        continue;
      }
      ip4uaf.inherit(vrf.getGenericAddressFamilyConfig());
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

    // convert standard/expanded community lists
    convertIpCommunityLists(c);

    // convert prefix lists to route filter lists
    for (PrefixList prefixList : _prefixLists.values()) {
      RouteFilterList newRouteFilterList =
          CiscoConversions.toRouteFilterList(prefixList, _filename);
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
        RouteFilterList rfList = CiscoConversions.toRouteFilterList(saList, _filename);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      c.getIpAccessLists()
          .put(saList.getName(), toIpAccessList(saList.toExtendedAccessList(), _objectGroups));
    }
    for (ExtendedAccessList eaList : _extendedAccessLists.values()) {
      if (isAclUsedForRouting(eaList.getName())) {
        RouteFilterList rfList = CiscoConversions.toRouteFilterList(eaList, _filename);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      IpAccessList ipaList = toIpAccessList(eaList, _objectGroups);
      c.getIpAccessLists().put(ipaList.getName(), ipaList);
    }

    /*
     * Consolidate info about networkObjects
     * - Associate networkObjects with their Info
     */
    _networkObjectInfos.forEach(
        (name, info) -> {
          if (_networkObjects.containsKey(name)) {
            _networkObjects.get(name).setInfo(info);
          }
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
                            name, CiscoStructureType.NETWORK_OBJECT_GROUP.getDescription(), null)));
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
                            name, CiscoStructureType.NETWORK_OBJECT.getDescription(), null)));

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
                .put(
                    computeServiceObjectGroupAclName(name),
                    toIpAccessList(serviceObjectGroup, _serviceObjects, _serviceObjectGroups)));
    _serviceObjects.forEach(
        (name, serviceObject) ->
            c.getIpAccessLists()
                .put(
                    computeServiceObjectAclName(name),
                    toIpAccessList(serviceObject, _serviceObjects, _serviceObjectGroups)));

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

    // TODO: convert route maps that are used for PBR to PacketPolicies

    for (RouteMap map : _routeMaps.values()) {
      // convert route maps to RoutingPolicy objects
      RoutingPolicy newPolicy = toRoutingPolicy(c, map);
      c.getRoutingPolicies().put(newPolicy.getName(), newPolicy);
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

    // create zone policies
    createZoneAcls(c);

    // convert interfaces
    _interfaces.forEach(
        (ifaceName, iface) -> {
          // Handle renaming interfaces for ASA devices
          String newIfaceName = iface.getName();
          org.batfish.datamodel.Interface newInterface = toInterface(newIfaceName, iface, c);
          String vrfName = iface.getVrf();
          if (vrfName == null) {
            throw new BatfishException("Missing vrf name for iface: '" + iface.getName() + "'");
          }
          c.getAllInterfaces().put(newIfaceName, newInterface);
        });
    /*
     * On IOS, add a single FirewallSessionInterfaceInfo for all inside interfaces and a
     * single FirewallSessionInterfaceInfo for all outside interfaces. This way, when an outgoing
     * flow exits an [inside|outside] interface, return flows will match the session if they enter
     * any [inside|outside] interface.
     */
    if (_vendor == ConfigurationFormat.CISCO_IOS) {
      // IOS does FIB lookups with the original dst IP for flows from inside to outside, but the
      // transformed dst IP for flows from outside to inside.
      if (!getNatInside().isEmpty()) {
        // Flows to inside interfaces should undergo NAT if they come from an outside interface
        FirewallSessionInterfaceInfo insideIfaceInfo =
            new FirewallSessionInterfaceInfo(
                Action.PRE_NAT_FIB_LOOKUP, getNatInside(), getNatOutside(), null, null);
        getNatInside().stream()
            .map(c.getAllInterfaces()::get)
            .forEach(iface -> iface.setFirewallSessionInterfaceInfo(insideIfaceInfo));
      }
      if (!getNatOutside().isEmpty()) {
        FirewallSessionInterfaceInfo outsideIfaceInfo =
            new FirewallSessionInterfaceInfo(
                Action.POST_NAT_FIB_LOOKUP,
                getNatOutside(),
                // Flows to outside interfaces should undergo NAT if they come from an inside
                // interface or originate on device
                ImmutableSet.<String>builder()
                    .addAll(getNatInside())
                    .add(SOURCE_ORIGINATING_FROM_DEVICE)
                    .build(),
                null,
                null);
        getNatOutside().stream()
            .map(c.getAllInterfaces()::get)
            .forEach(iface -> iface.setFirewallSessionInterfaceInfo(outsideIfaceInfo));
      }
    }

    /*
     * Second pass over the interfaces to set dependency pointers correctly for:
     * - portchannels
     * - subinterfaces
     * - redundant interfaces
     * - tunnel interfaces
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
          // subinterfaces
          Matcher m = INTERFACE_WITH_SUBINTERFACE.matcher(iface.getName());
          if (m.matches()) {
            String parentInterfaceName = m.group(1);
            Interface parentInterface = _interfaces.get(parentInterfaceName);
            if (parentInterface != null) {
              org.batfish.datamodel.Interface viIface = c.getAllInterfaces().get(ifaceName);
              if (viIface != null) {
                viIface.addDependency(new Dependency(parentInterfaceName, DependencyType.BIND));
              }
            }
          }
          // Redundant Interfaces
          {
            org.batfish.datamodel.Interface viIface = c.getAllInterfaces().get(ifaceName);
            if (viIface != null && viIface.getInterfaceType() == InterfaceType.REDUNDANT) {
              iface.getMemberInterfaces().stream()
                  .map(c.getAllInterfaces()::get)
                  .filter(Objects::nonNull)
                  .forEach(
                      memberViIface ->
                          viIface.addDependency(
                              new Dependency(memberViIface.getName(), DependencyType.AGGREGATE)));
            }
          }
          // Tunnels
          Tunnel tunnel = iface.getTunnel();
          if (tunnel != null) {
            org.batfish.datamodel.Interface viIface = c.getAllInterfaces().get(ifaceName);
            if (viIface != null) {
              // Add dependency
              if (isRealInterfaceName(tunnel.getSourceInterfaceName())) {
                String parentIfaceName = canonicalizeInterfaceName(tunnel.getSourceInterfaceName());
                viIface.addDependency(new Dependency(parentIfaceName, DependencyType.BIND));
                // Also set tunnel config while we're at at it.
                // Step one: determine IP address of parent interface
                @Nullable
                Ip parentIp =
                    Optional.ofNullable(c.getActiveInterfaces().get(parentIfaceName))
                        .map(org.batfish.datamodel.Interface::getConcreteAddress)
                        .map(ConcreteInterfaceAddress::getIp)
                        .orElse(null);
                // Step 2: create tunnel configs for non-IPsec tunnels. IPsec handled separately.
                if (tunnel.getMode() != TunnelMode.IPSEC_IPV4) {
                  // Ensure we have both src and dst IPs, otherwise don't convert
                  if (tunnel.getDestination() != null
                      && (tunnel.getSourceAddress() != null || parentIp != null)) {
                    viIface.setTunnelConfig(
                        TunnelConfiguration.builder()
                            .setSourceAddress(firstNonNull(tunnel.getSourceAddress(), parentIp))
                            .setDestinationAddress(tunnel.getDestination())
                            .build());
                  } else {
                    _w.redFlag(
                        String.format(
                            "Could not determine src/dst IPs for tunnel %s", iface.getName()));
                  }
                }
              }
            }
          }
        });

    // copy tracking groups
    c.getTrackingGroups().putAll(_trackingGroups);

    // apply vrrp settings to interfaces
    applyVrrp(c);

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
    // RSA pub named keys to IKE phase 1 key and IKE phase 1 policy
    _cryptoNamedRsaPubKeys
        .values()
        .forEach(
            namedRsaPubKey -> {
              IkePhase1Key ikePhase1Key = toIkePhase1Key(namedRsaPubKey);
              ikePhase1KeysBuilder.put(getRsaPubKeyGeneratedName(namedRsaPubKey), ikePhase1Key);

              IkePhase1Policy ikePhase1Policy =
                  toIkePhase1Policy(namedRsaPubKey, this, ikePhase1Key);
              c.getIkePhase1Policies().put(ikePhase1Policy.getName(), ikePhase1Policy);
            });

    // standalone ISAKMP keys to IKE phase 1 key and IKE phase 1 policy
    _isakmpKeys.forEach(
        isakmpKey -> {
          IkePhase1Key ikePhase1Key = toIkePhase1Key(isakmpKey);
          ikePhase1KeysBuilder.put(getIsakmpKeyGeneratedName(isakmpKey), ikePhase1Key);

          IkePhase1Policy ikePhase1Policy = toIkePhase1Policy(isakmpKey, this, ikePhase1Key);
          c.getIkePhase1Policies().put(ikePhase1Policy.getName(), ikePhase1Policy);
        });

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
      if (iface.getActive() && tunnel != null && tunnel.getMode() == TunnelMode.IPSEC_IPV4) {
        if (tunnel.getIpsecProfileName() == null) {
          _w.redFlag(String.format("No IPSec Profile set for IPSec tunnel %s", name));
          continue;
        }
        // convert to IpsecPeerConfig
        toIpsecPeerConfig(tunnel, name, this, c, _w)
            .ifPresent(config -> ipsecPeerConfigBuilder.put(name, config));
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
            newVrf.getStaticRoutes().add(CiscoConversions.toStaticRoute(staticRoute));
          }
          // For the default VRF, also convert static routes created by add-route in NAT rules
          if (vrf == getDefaultVrf()) {
            newVrf.getStaticRoutes().addAll(generateIosNatAddRouteRoutes());
          }

          // convert rip process
          RipProcess ripProcess = vrf.getRipProcess();
          if (ripProcess != null) {
            org.batfish.datamodel.RipProcess newRipProcess = toRipProcess(ripProcess, vrfName, c);
            newVrf.setRipProcess(newRipProcess);
          }

          // Convert OSPF processes.
          newVrf.setOspfProcesses(
              vrf.getOspfProcesses().values().stream()
                  .map(proc -> toOspfProcess(proc, vrfName, c, this))
                  .filter(Objects::nonNull));

          // convert eigrp processes
          vrf.getEigrpProcesses().values().stream()
              .map(proc -> CiscoConversions.toEigrpProcess(proc, vrfName, c, this))
              .filter(Objects::nonNull)
              .forEach(newVrf::addEigrpProcess);

          // convert isis process
          IsisProcess isisProcess = vrf.getIsisProcess();
          if (isisProcess != null) {
            org.batfish.datamodel.isis.IsisProcess newIsisProcess =
                CiscoConversions.toIsisProcess(isisProcess, c, this);
            newVrf.setIsisProcess(newIsisProcess);
          }

          // convert bgp process
          BgpProcess bgpProcess = vrf.getBgpProcess();
          if (bgpProcess != null) {
            org.batfish.datamodel.BgpProcess newBgpProcess = toBgpProcess(c, bgpProcess, vrfName);
            newVrf.setBgpProcess(newBgpProcess);
          } else if (vrf.getIpv4UnicastAddressFamily() != null
              && !vrf.getIpv4UnicastAddressFamily().getRouteTargetImport().isEmpty()) {
            /*
             * Despite no BGP config this vrf is leaked into. Make a dummy BGP process.
             */
            assert newVrf.getBgpProcess() == null;
            newVrf.setBgpProcess(
                org.batfish.datamodel.BgpProcess.builder()
                    .setRouterId(Ip.ZERO)
                    .setAdminCostsToVendorDefaults(c.getConfigurationFormat())
                    .setRedistributionPolicy(initDenyAllBgpRedistributionPolicy(c))
                    .build());
          }
        });
    /*
     * Another pass over interfaces to push final settings to VI interfaces.
     * (e.g. has OSPF settings but no associated OSPF process, common in show run all)
     */
    _interfaces.forEach(
        (ifaceName, vsIface) -> {
          org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
          if (iface == null) {
            // Should never get here
            return;
          } else if (iface.getOspfAreaName() != null) {
            // Already configured
            return;
          }
          // Not part of an OSPF area, but has settings
          if (vsIface.getOspfArea() != null
              || vsIface.getOspfCost() != null
              || vsIface.getOspfPassive() != null
              || vsIface.getOspfNetworkType() != null
              || vsIface.getOspfDeadInterval() != null
              || vsIface.getOspfHelloInterval() != null) {
            finalizeInterfaceOspfSettings(iface, vsIface, null, null);
          }
        });

    convertVrfLeakingConfig(_vrfs.values(), c);

    // Define the Null0 interface if it has been referenced. Otherwise, these show as undefined
    // references.
    Optional<Integer> firstRefToNull0 =
        _structureReferences
            .getOrDefault(CiscoStructureType.INTERFACE, ImmutableSortedMap.of())
            .getOrDefault("Null0", ImmutableSortedMap.of())
            .entrySet()
            .stream()
            .flatMap(e -> e.getValue().stream())
            .min(Integer::compare);
    if (firstRefToNull0.isPresent()) {
      defineSingleLineStructure(CiscoStructureType.INTERFACE, "Null0", firstRefToNull0.get());
    }

    markConcreteStructure(
        CiscoStructureType.BFD_TEMPLATE, CiscoStructureUsage.INTERFACE_BFD_TEMPLATE);

    markConcreteStructure(
        CiscoStructureType.SECURITY_ZONE_PAIR, CiscoStructureUsage.SECURITY_ZONE_PAIR_SELF_REF);

    markConcreteStructure(
        CiscoStructureType.INTERFACE,
        CiscoStructureUsage.BGP_UPDATE_SOURCE_INTERFACE,
        CiscoStructureUsage.DOMAIN_LOOKUP_SOURCE_INTERFACE,
        CiscoStructureUsage.EIGRP_AF_INTERFACE,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_IN,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_GATEWAY_IN,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_GATEWAY_OUT,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_IN,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_OUT,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_IN,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_OUT,
        CiscoStructureUsage.EIGRP_PASSIVE_INTERFACE,
        CiscoStructureUsage.INTERFACE_SELF_REF,
        CiscoStructureUsage.IP_NAT_INSIDE_SOURCE,
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
        CiscoStructureUsage.ROUTE_MAP_MATCH_INTERFACE,
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
        CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_IN,
        CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_OUT,
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
        CiscoStructureUsage.SNMP_SERVER_GROUP_V3_ACCESS,
        CiscoStructureUsage.SSH_IPV4_ACL);
    markIpv6Acls(
        CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_IN,
        CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_OUT,
        CiscoStructureUsage.LINE_ACCESS_CLASS_LIST6,
        CiscoStructureUsage.NTP_ACCESS_GROUP,
        CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST,
        CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL6,
        CiscoStructureUsage.SNMP_SERVER_GROUP_V3_ACCESS_IPV6,
        CiscoStructureUsage.SSH_IPV6_ACL,
        CiscoStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_IN,
        CiscoStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_OUT);
    markAcls(
        CiscoStructureUsage.ACCESS_GROUP_GLOBAL_FILTER,
        CiscoStructureUsage.COPS_LISTENER_ACCESS_LIST,
        CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL,
        CiscoStructureUsage.CRYPTO_MAP_MATCH_ADDRESS,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_IN,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT,
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

    markExtcommunityLists(CiscoStructureUsage.ROUTE_MAP_MATCH_EXTCOMMUNITY);

    markConcreteStructure(
        CiscoStructureType.PREFIX_LIST,
        CiscoStructureUsage.BGP_INBOUND_PREFIX_LIST,
        CiscoStructureUsage.BGP_OUTBOUND_PREFIX_LIST,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_GATEWAY_IN,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_GATEWAY_OUT,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_IN,
        CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_OUT,
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

    // mark references to route-maps
    markConcreteStructure(CiscoStructureType.ROUTE_MAP);

    // mark references to route-map clauses
    markConcreteStructure(CiscoStructureType.ROUTE_MAP_CLAUSE);

    // Cable
    markConcreteStructure(CiscoStructureType.DEPI_CLASS);
    markConcreteStructure(CiscoStructureType.DEPI_TUNNEL);
    markConcreteStructure(CiscoStructureType.DOCSIS_POLICY);
    markConcreteStructure(CiscoStructureType.DOCSIS_POLICY_RULE);
    markConcreteStructure(
        CiscoStructureType.SERVICE_CLASS, CiscoStructureUsage.QOS_ENFORCE_RULE_SERVICE_CLASS);

    // L2tp
    markConcreteStructure(CiscoStructureType.L2TP_CLASS);

    // Crypto, Isakmp, and IPSec
    markConcreteStructure(CiscoStructureType.CRYPTO_DYNAMIC_MAP_SET);
    markConcreteStructure(CiscoStructureType.ISAKMP_PROFILE);
    markConcreteStructure(CiscoStructureType.ISAKMP_POLICY);
    markConcreteStructure(CiscoStructureType.IPSEC_PROFILE);
    markConcreteStructure(CiscoStructureType.IPSEC_TRANSFORM_SET);
    markConcreteStructure(CiscoStructureType.KEYRING, CiscoStructureUsage.ISAKMP_PROFILE_KEYRING);
    markConcreteStructure(
        CiscoStructureType.NAMED_RSA_PUB_KEY, CiscoStructureUsage.NAMED_RSA_PUB_KEY_SELF_REF);

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
    markConcreteStructure(CiscoStructureType.POLICY_MAP);

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

    // service template
    markConcreteStructure(
        CiscoStructureType.SERVICE_TEMPLATE,
        CiscoStructureUsage.CLASS_MAP_SERVICE_TEMPLATE,
        CiscoStructureUsage.CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE,
        CiscoStructureUsage.POLICY_MAP_EVENT_CLASS_ACTIVATE);

    // track
    markConcreteStructure(CiscoStructureType.TRACK);

    // zone
    markConcreteStructure(
        CiscoStructureType.SECURITY_ZONE,
        CiscoStructureUsage.INTERFACE_ZONE_MEMBER,
        CiscoStructureUsage.ZONE_PAIR_DESTINATION_ZONE,
        CiscoStructureUsage.ZONE_PAIR_SOURCE_ZONE);

    markConcreteStructure(CiscoStructureType.NAT_POOL);
    markConcreteStructure(
        CiscoStructureType.AS_PATH_ACCESS_LIST,
        CiscoStructureUsage.BGP_NEIGHBOR_FILTER_AS_PATH_ACCESS_LIST,
        CiscoStructureUsage.ROUTE_MAP_MATCH_AS_PATH_ACCESS_LIST);

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
        CiscoStructureType.BGP_TEMPLATE_PEER_POLICY, CiscoStructureUsage.BGP_INHERITED_PEER_POLICY);
    markConcreteStructure(
        CiscoStructureType.BGP_TEMPLATE_PEER_SESSION, CiscoStructureUsage.BGP_INHERITED_SESSION);
    markConcreteStructure(
        CiscoStructureType.BGP_UNDECLARED_PEER, CiscoStructureUsage.BGP_NEIGHBOR_WITHOUT_REMOTE_AS);
    markConcreteStructure(
        CiscoStructureType.BGP_UNDECLARED_PEER_GROUP,
        CiscoStructureUsage.BGP_PEER_GROUP_REFERENCED_BEFORE_DEFINED);

    return ImmutableList.of(c);
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
                      ExprAclLine.accepting().setMatchCondition(matchClassMap).build()))
              .setSourceName(inspectClassMapName)
              .setSourceType(CiscoStructureType.INSPECT_CLASS_MAP.getDescription())
              .build();
        });
  }

  private void createInspectPolicyMapAcls(Configuration c) {
    _inspectPolicyMaps.forEach(
        (inspectPolicyMapName, inspectPolicyMap) -> {
          String inspectPolicyMapAclName = computeInspectPolicyMapAclName(inspectPolicyMapName);
          ImmutableList.Builder<AclLine> policyMapAclLines = ImmutableList.builder();
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
                            ExprAclLine.rejecting()
                                .setMatchCondition(matchCondition)
                                .setName(
                                    String.format(
                                        "Drop if matched by class-map: '%s'", inspectClassName))
                                .build());
                        break;

                      case INSPECT:
                        policyMapAclLines.add(
                            ExprAclLine.accepting()
                                .setMatchCondition(matchCondition)
                                .setName(
                                    String.format(
                                        "Inspect if matched by class-map: '%s'", inspectClassName))
                                .build());
                        break;

                      case PASS:
                        policyMapAclLines.add(
                            ExprAclLine.accepting()
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
              ExprAclLine.builder()
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

    c.getZones().values().stream()
        .map(
            zone -> {
              // Don't bother if zone is empty
              SortedSet<String> interfaces = zone.getInterfaces();
              if (interfaces.isEmpty()) {
                return null;
              }
              String zoneName = zone.getName();
              if (_securityZones.containsKey(zoneName)) {
                // IOS security zone
                return createIosSecurityZoneAcl(zone, matchSrcInterfaceBySrcZone, c);
              }
              // shouldn't reach here
              return null;
            })
        .filter(Objects::nonNull)
        .forEach(acl -> c.getIpAccessLists().put(acl.getName(), acl));
  }

  IpAccessList createIosSecurityZoneAcl(
      Zone zone, Map<String, MatchSrcInterface> matchSrcInterfaceBySrcZone, Configuration c) {

    ImmutableList.Builder<AclLine> zonePolicies = ImmutableList.builder();

    // Allow traffic originating from device (no source interface)
    zonePolicies.add(
        ExprAclLine.accepting()
            .setMatchCondition(OriginatingFromDevice.INSTANCE)
            .setName("Allow traffic originating from this device")
            .build());

    // Allow traffic staying within this zone (always true for IOS)
    String zoneName = zone.getName();
    zonePolicies.add(
        ExprAclLine.accepting()
            .setMatchCondition(matchSrcInterfaceBySrcZone.get(zoneName))
            .setName(
                String.format("Allow traffic received on interface in same zone: '%s'", zoneName))
            .build());

    /*
     * Add zone-pair policies
     */
    // zoneName refers to dstZone
    Map<String, SecurityZonePair> zonePairsBySrcZoneName = _securityZonePairs.get(zoneName);
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

    return IpAccessList.builder()
        .setName(computeZoneOutgoingAclName(zoneName))
        .setLines(zonePolicies.build())
        .build();
  }

  public Optional<ExprAclLine> createZonePairAcl(
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
                ExprAclLine.accepting()
                    .setMatchCondition(
                        new AndMatchExpr(
                            ImmutableList.of(matchSrcZoneInterface, permittedByPolicyMap)))
                    .setName(
                        String.format(
                            "Allow traffic received on interface in zone '%s' permitted by"
                                + " policy-map: '%s'",
                            srcZoneName, inspectPolicyMapName))
                    .build()))
        .setSourceName(zonePair.getName())
        .setSourceType(CiscoStructureType.SECURITY_ZONE_PAIR.getDescription())
        .build();
    return Optional.of(
        ExprAclLine.accepting()
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

  private boolean isAclUsedForRouting(@Nonnull String aclName) {
    String currentMapName;
    for (Vrf vrf : _vrfs.values()) {
      // check ospf policies
      for (OspfProcess ospfProcess : vrf.getOspfProcesses().values()) {
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
          if (aclName.equals(pg.getInboundIpAccessList())
              || aclName.equals(pg.getOutboundIpAccessList())) {
            return true;
          }
        }
      }
      // check EIGRP policies
      // distribute lists
      if (vrf.getEigrpProcesses().values().stream()
          .flatMap(
              eigrpProcess ->
                  Streams.concat(
                      Stream.of(
                              eigrpProcess.getInboundGlobalDistributeList(),
                              eigrpProcess.getOutboundGlobalDistributeList())
                          .filter(Objects::nonNull),
                      eigrpProcess.getInboundInterfaceDistributeLists().values().stream(),
                      eigrpProcess.getOutboundInterfaceDistributeLists().values().stream()))
          .anyMatch(distributeList -> distributeList.getFilterName().equals(aclName))) {
        return true;
      }
      // EIGRP redistribution policy
      if (vrf.getEigrpProcesses().values().stream()
          .map(EigrpProcess::getRedistributionPolicies)
          .flatMap(redisrPolicies -> redisrPolicies.values().stream())
          .anyMatch(rm -> containsIpAccessList(aclName, rm.getRouteMap()))) {
        return true;
      }
    }
    return false;
  }

  private boolean isAclUsedForRoutingv6(String aclName) {
    String currentMapName;
    for (Vrf vrf : _vrfs.values()) {
      // check ospf policies
      for (OspfProcess ospfProcess : vrf.getOspfProcesses().values()) {
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

  private void markExtcommunityLists(CiscoStructureUsage... usages) {
    for (CiscoStructureUsage usage : usages) {
      markAbstractStructure(
          CiscoStructureType.EXTCOMMUNITY_LIST,
          usage,
          ImmutableList.of(
              CiscoStructureType.EXTCOMMUNITY_LIST_EXPANDED,
              CiscoStructureType.EXTCOMMUNITY_LIST_STANDARD));
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

  private void convertIpCommunityLists(Configuration c) {
    // create CommunitySetMatchExpr for route-map match community
    _standardCommunityLists.forEach(
        (name, ipCommunityListStandard) ->
            c.getCommunitySetMatchExprs()
                .put(name, toCommunitySetMatchExpr(ipCommunityListStandard)));
    _expandedCommunityLists.forEach(
        (name, ipCommunityListExpanded) ->
            c.getCommunitySetMatchExprs()
                .put(name, toCommunitySetMatchExpr(ipCommunityListExpanded)));

    // create CommunityMatchExpr for route-map set comm-list delete
    _standardCommunityLists.forEach(
        (name, ipCommunityListStandard) ->
            c.getCommunityMatchExprs().put(name, toCommunityMatchExpr(ipCommunityListStandard)));
    _expandedCommunityLists.forEach(
        (name, ipCommunityListExpanded) ->
            c.getCommunityMatchExprs().put(name, toCommunityMatchExpr(ipCommunityListExpanded)));
  }

  private static CommunitySetMatchExpr toCommunitySetMatchExpr(
      ExpandedCommunityList ipCommunityListExpanded) {
    return new CommunitySetAcl(
        ipCommunityListExpanded.getLines().stream()
            .map(CiscoConversions::toCommunitySetAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static CommunitySetMatchExpr toCommunitySetMatchExpr(
      StandardCommunityList ipCommunityListStandard) {
    return new CommunitySetAcl(
        ipCommunityListStandard.getLines().stream()
            .map(CiscoConfiguration::toCommunitySetAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunitySetAclLine toCommunitySetAclLine(
      StandardCommunityListLine line) {
    return new CommunitySetAclLine(
        line.getAction(),
        new CommunitySetMatchAll(
            line.getCommunities().stream()
                .map(community -> new HasCommunity(new CommunityIs(community)))
                .collect(ImmutableSet.toImmutableSet())));
  }

  private static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      ExpandedCommunityList ipCommunityListExpanded) {
    return new CommunityAcl(
        ipCommunityListExpanded.getLines().stream()
            .map(CiscoConfiguration::toCommunityAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunityAclLine toCommunityAclLine(ExpandedCommunityListLine line) {
    return new CommunityAclLine(
        line.getAction(), CiscoConversions.toCommunityMatchRegex(line.getRegex()));
  }

  private static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      StandardCommunityList ipCommunityListStandard) {
    Set<Community> whitelist = new HashSet<>();
    Set<Community> blacklist = new HashSet<>();
    for (StandardCommunityListLine line : ipCommunityListStandard.getLines()) {
      if (line.getCommunities().size() != 1) {
        continue;
      }
      Community community = Iterables.getOnlyElement(line.getCommunities());
      if (line.getAction() == LineAction.PERMIT) {
        if (!blacklist.contains(community)) {
          whitelist.add(community);
        }
      } else {
        // DENY
        if (!whitelist.contains(community)) {
          blacklist.add(community);
        }
      }
    }
    return new CommunityIn(new LiteralCommunitySet(CommunitySet.of(whitelist)));
  }

  public static @Nonnull String computeRouteMapClauseName(String routeMapName, int sequence) {
    return String.format("%s %d", routeMapName, sequence);
  }
}
