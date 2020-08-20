package org.batfish.representation.arista;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.datamodel.Interface.computeInterfaceType;
import static org.batfish.datamodel.Interface.isRealInterfaceName;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.datamodel.routing_policy.Common.generateGenerationPolicy;
import static org.batfish.datamodel.routing_policy.Common.suppressSummarizedPrefixes;
import static org.batfish.representation.arista.AristaConversions.getVrfForVlan;
import static org.batfish.representation.arista.Conversions.computeDistributeListPolicies;
import static org.batfish.representation.arista.Conversions.convertCryptoMapSet;
import static org.batfish.representation.arista.Conversions.getIsakmpKeyGeneratedName;
import static org.batfish.representation.arista.Conversions.getRsaPubKeyGeneratedName;
import static org.batfish.representation.arista.Conversions.resolveIsakmpProfileIfaceNames;
import static org.batfish.representation.arista.Conversions.resolveKeyringIfaceNames;
import static org.batfish.representation.arista.Conversions.resolveTunnelIfaceNames;
import static org.batfish.representation.arista.Conversions.toCommunityList;
import static org.batfish.representation.arista.Conversions.toIkePhase1Key;
import static org.batfish.representation.arista.Conversions.toIkePhase1Policy;
import static org.batfish.representation.arista.Conversions.toIkePhase1Proposal;
import static org.batfish.representation.arista.Conversions.toIpAccessList;
import static org.batfish.representation.arista.Conversions.toIpsecPeerConfig;
import static org.batfish.representation.arista.Conversions.toIpsecPhase2Policy;
import static org.batfish.representation.arista.Conversions.toIpsecPhase2Proposal;
import static org.batfish.representation.arista.Conversions.toOspfDeadInterval;
import static org.batfish.representation.arista.Conversions.toOspfHelloInterval;
import static org.batfish.representation.arista.Conversions.toOspfNetworkType;
import static org.batfish.representation.arista.OspfProcess.DEFAULT_LOOPBACK_OSPF_COST;
import static org.batfish.representation.arista.eos.AristaRedistributeType.CONNECTED;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_EXTERNAL;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_INTERNAL;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL_TYPE_1;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL_TYPE_2;
import static org.batfish.representation.arista.eos.AristaRedistributeType.STATIC;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
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
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.GeneratedRoute;
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
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPhase2Policy;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.Line;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.Names;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SnmpServer;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TunnelConfiguration;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.bgp.BgpConfederation;
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
import org.batfish.datamodel.routing_policy.Common;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.RouteIsClassful;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.CallStatement;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
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
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;
import org.batfish.representation.arista.Tunnel.TunnelMode;
import org.batfish.representation.arista.eos.AristaBgpAggregateNetwork;
import org.batfish.representation.arista.eos.AristaBgpBestpathTieBreaker;
import org.batfish.representation.arista.eos.AristaBgpPeerFilter;
import org.batfish.representation.arista.eos.AristaBgpProcess;
import org.batfish.representation.arista.eos.AristaBgpRedistributionPolicy;
import org.batfish.representation.arista.eos.AristaBgpVrf;
import org.batfish.representation.arista.eos.AristaBgpVrfIpv4UnicastAddressFamily;
import org.batfish.representation.arista.eos.AristaEosVxlan;
import org.batfish.representation.arista.eos.AristaRedistributeType;
import org.batfish.vendor.VendorConfiguration;

public final class AristaConfiguration extends VendorConfiguration {

  /** Matches anything but the IPv4 default route. */
  static final Not NOT_DEFAULT_ROUTE = new Not(Common.matchDefaultRoute());

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
          .put("Ethernet", "Ethernet")
          .put("Embedded-Service-Engine", "Embedded-Service-Engine")
          .put("fc", "fc")
          .put("GMPLS", "GMPLS")
          .put("ip", "ip")
          .put("Group-Async", "Group-Async")
          .put("lo", "Loopback")
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
          .put("Recirc-Channel", "Recirc-Channel")
          .put("Redundant", "Redundant")
          .put("Serial", "Serial")
          .put("Service-Engine", "Service-Engine")
          .put("trunk", "trunk")
          .put("Tunnel", "Tunnel")
          .put("tunnel-ip", "tunnel-ip")
          .put("tunnel-te", "tunnel-te")
          .put("UnconnectedEthernet", "UnconnectedEthernet")
          .put("ve", "VirtualEthernet")
          .put("Virtual-Template", "Virtual-Template")
          .put("Vlan", "Vlan")
          .put("Vxlan", "Vxlan")
          .put("Wideband-Cable", "Wideband-Cable")
          .build();

  static final boolean DEFAULT_VRRP_PREEMPT = true;

  static final int DEFAULT_VRRP_PRIORITY = 100;

  public static final String DEFAULT_VRF_NAME = "default";
  public static final String MANAGEMENT_VRF_NAME = "management";

  static final int MAX_ADMINISTRATIVE_COST = 32767;

  public static final String MANAGEMENT_INTERFACE_PREFIX = "mgmt";

  private static final int VLAN_NORMAL_MAX_CISCO = 1005;

  private static final int VLAN_NORMAL_MIN_CISCO = 2;

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

  @Nullable private AristaBgpProcess _aristaBgp;

  private final Map<String, IpAsPathAccessList> _asPathAccessLists;

  private final CiscoFamily _cf;

  private final Map<String, CryptoMapSet> _cryptoMapSets;

  private final Map<String, NamedRsaPubKey> _cryptoNamedRsaPubKeys;

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

  private String _hostname;

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

  private final Map<String, IntegerSpace> _namedVlans;

  private String _ntpSourceInterface;

  private final Map<String, AristaBgpPeerFilter> _peerFilters;

  private final Map<String, Prefix6List> _prefix6Lists;

  private final Map<String, PrefixList> _prefixLists;

  private final Map<String, RouteMap> _routeMaps;

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

  private final Map<String, TrackMethod> _trackingGroups;

  public AristaConfiguration() {
    _asPathAccessLists = new TreeMap<>();
    _cf = new CiscoFamily();
    _cryptoNamedRsaPubKeys = new TreeMap<>();
    _cryptoMapSets = new HashMap<>();
    _dhcpRelayServers = new ArrayList<>();
    _dnsServers = new TreeSet<>();
    _eosVlanTrunkGroups = new HashMap<>();
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
    _namedVlans = new HashMap<>();
    _peerFilters = new HashMap<>();
    _prefixLists = new TreeMap<>();
    _prefix6Lists = new TreeMap<>();
    _routeMaps = new TreeMap<>();
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

  @Nullable
  public AristaBgpProcess getAristaBgp() {
    return _aristaBgp;
  }

  public void setAristaBgp(@Nullable AristaBgpProcess aristaBgp) {
    _aristaBgp = aristaBgp;
  }

  public Map<String, IpAsPathAccessList> getAsPathAccessLists() {
    return _asPathAccessLists;
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

  public String getNtpSourceInterface() {
    return _ntpSourceInterface;
  }

  public Map<String, AristaBgpPeerFilter> getPeerFilters() {
    return _peerFilters;
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

  public ConfigurationFormat getVendor() {
    return _vendor;
  }

  public Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  public SortedMap<String, VrrpInterface> getVrrpGroups() {
    return _vrrpGroups;
  }

  private void markAcls(AristaStructureUsage... usages) {
    for (AristaStructureUsage usage : usages) {
      markAbstractStructure(
          AristaStructureType.IP_ACCESS_LIST,
          usage,
          ImmutableList.of(
              AristaStructureType.IPV4_ACCESS_LIST_STANDARD,
              AristaStructureType.IPV4_ACCESS_LIST_EXTENDED,
              AristaStructureType.IPV6_ACCESS_LIST_STANDARD,
              AristaStructureType.IPV6_ACCESS_LIST_EXTENDED));
    }
  }

  private void markIpOrMacAcls(AristaStructureUsage... usages) {
    for (AristaStructureUsage usage : usages) {
      markAbstractStructure(
          AristaStructureType.ACCESS_LIST,
          usage,
          Arrays.asList(
              AristaStructureType.IPV4_ACCESS_LIST_EXTENDED,
              AristaStructureType.IPV4_ACCESS_LIST_STANDARD,
              AristaStructureType.IPV6_ACCESS_LIST_EXTENDED,
              AristaStructureType.IPV6_ACCESS_LIST_STANDARD,
              AristaStructureType.MAC_ACCESS_LIST));
    }
  }

  private void markIpv4Acls(AristaStructureUsage... usages) {
    for (AristaStructureUsage usage : usages) {
      markAbstractStructure(
          AristaStructureType.IPV4_ACCESS_LIST,
          usage,
          ImmutableList.of(
              AristaStructureType.IPV4_ACCESS_LIST_STANDARD,
              AristaStructureType.IPV4_ACCESS_LIST_EXTENDED));
    }
  }

  private void markIpv6Acls(AristaStructureUsage... usages) {
    for (AristaStructureUsage usage : usages) {
      markAbstractStructure(
          AristaStructureType.IPV6_ACCESS_LIST,
          usage,
          ImmutableList.of(
              AristaStructureType.IPV6_ACCESS_LIST_STANDARD,
              AristaStructureType.IPV6_ACCESS_LIST_EXTENDED));
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

  @Override
  public void setHostname(String hostname) {
    checkNotNull(hostname, "'hostname' cannot be null");
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

  private org.batfish.datamodel.BgpProcess toEosBgpProcess(
      Configuration c, AristaBgpProcess bgpGlobal, AristaBgpVrf bgpVrf) {
    String vrfName = bgpVrf.getName();
    org.batfish.datamodel.Vrf v = c.getVrfs().get(vrfName);
    int ebgpAdmin =
        firstNonNull(
            bgpVrf.getEbgpAdminDistance(),
            RoutingProtocol.BGP.getDefaultAdministrativeCost(c.getConfigurationFormat()));
    int ibgpAdmin =
        firstNonNull(
            bgpVrf.getIbgpAdminDistance(),
            RoutingProtocol.IBGP.getDefaultAdministrativeCost(c.getConfigurationFormat()));
    org.batfish.datamodel.BgpProcess newBgpProcess =
        new org.batfish.datamodel.BgpProcess(
            AristaConversions.getBgpRouterId(
                bgpVrf, v.getName(), c.getAllInterfaces(v.getName()), _w),
            ebgpAdmin,
            ibgpAdmin);

    boolean multipath = firstNonNull(bgpVrf.getMaxPaths(), 1) > 1;
    newBgpProcess.setMultipathEbgp(multipath);
    newBgpProcess.setMultipathIbgp(multipath); // TODO is this correct? Seems like it.

    // Arista `bestpath as-path multipath-relax` is enabled by default.
    // https://www.arista.com/en/um-eos/eos-section-33-1-bgp-conceptual-overview#ww1296175 step 8
    newBgpProcess.setMultipathEquivalentAsPathMatchMode(
        firstNonNull(bgpVrf.getBestpathAsPathMultipathRelax(), Boolean.TRUE)
            ? PATH_LENGTH
            : EXACT_PATH);
    BgpTieBreaker tieBreaker = BgpTieBreaker.ROUTER_ID; // default if not specified
    if (bgpVrf.getBestpathTieBreaker() == AristaBgpBestpathTieBreaker.CLUSTER_LIST_LENGTH) {
      tieBreaker = BgpTieBreaker.CLUSTER_LIST_LENGTH;
    }
    newBgpProcess.setTieBreaker(tieBreaker);

    // If confederations are present, convert
    if (bgpVrf.getConfederationIdentifier() != null) {
      LongSpace peers =
          firstNonNull(
              bgpVrf.getConfederationPeers(),
              LongSpace.of(firstNonNull(bgpVrf.getLocalAs(), bgpGlobal.getAsn())));
      newBgpProcess.setConfederation(
          // Assuming peers is a small space/set in most configs, so safe to enumerate
          new BgpConfederation(bgpVrf.getConfederationIdentifier(), peers.enumerate()));
    }

    // Process vrf-level address family configuration, such as export policy.
    if (bgpVrf.getDefaultIpv4Unicast()) {
      // Handle default activation for v4 unicast.
      bgpVrf.getOrCreateV4UnicastAf();
    }
    AristaBgpVrfIpv4UnicastAddressFamily ipv4af = bgpVrf.getV4UnicastAf();

    // Next we build up the BGP common export policy.
    RoutingPolicy bgpCommonExportPolicy =
        new RoutingPolicy(Names.generatedBgpCommonExportPolicyName(vrfName), c);
    c.getRoutingPolicies().put(bgpCommonExportPolicy.getName(), bgpCommonExportPolicy);

    // 1. If there are any ipv4 summary only networks, do not export the more specific routes.
    if (ipv4af != null) {
      Stream<Prefix> summaryOnlyNetworks =
          bgpVrf.getV4aggregates().entrySet().stream()
              .filter(e -> firstNonNull(e.getValue().getSummaryOnly(), Boolean.FALSE))
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
      for (Entry<Prefix, AristaBgpAggregateNetwork> e : bgpVrf.getV4aggregates().entrySet()) {
        Prefix prefix = e.getKey();
        AristaBgpAggregateNetwork agg = e.getValue();

        // TODO: add agg here for, e.g., match-map
        RoutingPolicy genPolicy = generateGenerationPolicy(c, vrfName, prefix);

        GeneratedRoute.Builder gr =
            GeneratedRoute.builder()
                .setNetwork(prefix)
                .setAdmin(CISCO_AGGREGATE_ROUTE_ADMIN_COST)
                .setGenerationPolicy(genPolicy.getName())
                .setDiscard(true);

        // Conditions to generate this route
        List<BooleanExpr> exportAggregateConditions = new ArrayList<>();
        exportAggregateConditions.add(
            new MatchPrefixSet(
                DestinationNetwork.instance(),
                new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(prefix)))));
        exportAggregateConditions.add(new MatchProtocol(RoutingProtocol.AGGREGATE));

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
        exportAggregateConditions.add(
            bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.IGP));

        v.getGeneratedRoutes().add(gr.build());
        // Do export a generated aggregate.
        exportConditions.add(new Conjunction(exportAggregateConditions));
      }
    }

    // Only redistribute default route if `default-information originate` is set.
    //    BooleanExpr redistributeDefaultRoute =
    //        ipv4af == null || !ipv4af.get() ? NOT_DEFAULT_ROUTE : BooleanExprs.TRUE;

    // TODO: Export RIP routes that should be redistributed.

    // Export static routes that should be redistributed.
    AristaBgpRedistributionPolicy staticPolicy =
        ipv4af == null ? null : bgpVrf.getRedistributionPolicies().get(STATIC);
    if (staticPolicy != null) {
      BooleanExpr filterByRouteMap =
          Optional.ofNullable(staticPolicy.getRouteMap())
              .filter(_routeMaps::containsKey)
              .<BooleanExpr>map(CallExpr::new)
              .orElse(BooleanExprs.TRUE);
      List<BooleanExpr> conditions =
          ImmutableList.of(
              new MatchProtocol(RoutingProtocol.STATIC),
              // TODO redistributeDefaultRoute,
              bgpRedistributeWithEnvironmentExpr(filterByRouteMap, OriginType.INCOMPLETE));
      Conjunction staticRedist = new Conjunction(conditions);
      staticRedist.setComment("Redistribute static routes into BGP");
      exportConditions.add(staticRedist);
    }
    // Export connected routes that should be redistributed.
    AristaBgpRedistributionPolicy connectedPolicy =
        ipv4af == null ? null : bgpVrf.getRedistributionPolicies().get(CONNECTED);
    if (connectedPolicy != null) {
      BooleanExpr filterByRouteMap =
          Optional.ofNullable(connectedPolicy.getRouteMap())
              .filter(_routeMaps::containsKey)
              .<BooleanExpr>map(CallExpr::new)
              .orElse(BooleanExprs.TRUE);
      List<BooleanExpr> conditions =
          ImmutableList.of(
              new MatchProtocol(RoutingProtocol.CONNECTED),
              // TODO redistributeDefaultRoute,
              bgpRedistributeWithEnvironmentExpr(filterByRouteMap, OriginType.INCOMPLETE));
      Conjunction connected = new Conjunction(conditions);
      connected.setComment("Redistribute connected routes into BGP");
      exportConditions.add(connected);
    }

    // Export OSPF routes that should be redistributed, to the best of our abilities in VI.
    // Warn and skip if our VI model doesn't allow a particular type of redistribution
    Map<AristaRedistributeType, MatchProtocol> protocolConversions =
        ImmutableMap.of(
            OSPF,
            new MatchProtocol(
                RoutingProtocol.OSPF,
                RoutingProtocol.OSPF_IA,
                RoutingProtocol.OSPF_E1,
                RoutingProtocol.OSPF_E2),
            OSPF_INTERNAL,
            new MatchProtocol(RoutingProtocol.OSPF, RoutingProtocol.OSPF_IA),
            OSPF_EXTERNAL,
            new MatchProtocol(RoutingProtocol.OSPF_E1, RoutingProtocol.OSPF_E2));

    for (AristaRedistributeType type :
        new AristaRedistributeType[] {
          OSPF,
          OSPF_INTERNAL,
          OSPF_EXTERNAL,
          OSPF_NSSA_EXTERNAL,
          OSPF_NSSA_EXTERNAL_TYPE_1,
          OSPF_NSSA_EXTERNAL_TYPE_2
        }) {
      AristaBgpRedistributionPolicy ospfPolicy =
          ipv4af == null ? null : bgpVrf.getRedistributionPolicies().get(type);
      if (ospfPolicy == null) {
        continue;
      }
      if (protocolConversions.get(type) == null) {
        _w.redFlag(String.format("Redistribution of %s routes is not yet supported", type));
        continue;
      }
      BooleanExpr filterByRouteMap =
          Optional.ofNullable(ospfPolicy.getRouteMap())
              .filter(_routeMaps::containsKey)
              .<BooleanExpr>map(CallExpr::new)
              .orElse(BooleanExprs.TRUE);
      List<BooleanExpr> conditions =
          ImmutableList.of(
              protocolConversions.get(type),
              // TODO redistributeDefaultRoute,
              bgpRedistributeWithEnvironmentExpr(filterByRouteMap, OriginType.INCOMPLETE));
      Conjunction ospf = new Conjunction(conditions);
      ospf.setComment(String.format("Redistribute %s routes into BGP", type));
      exportConditions.add(ospf);
    }

    // Now we add all the per-network export policies.
    if (ipv4af != null) {
      ipv4af
          .getNetworks()
          .forEach(
              (prefix, networkConf) -> {
                PrefixSpace exportSpace = new PrefixSpace(PrefixRange.fromPrefix(prefix));
                List<BooleanExpr> exportNetworkConditions =
                    ImmutableList.of(
                        new MatchPrefixSet(
                            DestinationNetwork.instance(), new ExplicitPrefixSet(exportSpace)),
                        new Not(
                            new MatchProtocol(
                                RoutingProtocol.BGP,
                                RoutingProtocol.IBGP,
                                RoutingProtocol.AGGREGATE)),
                        bgpRedistributeWithEnvironmentExpr(
                            networkConf.getRouteMap() != null
                                    && _routeMaps.containsKey(networkConf.getRouteMap())
                                ? new CallExpr(networkConf.getRouteMap())
                                : BooleanExprs.TRUE,
                            OriginType.IGP));
                newBgpProcess.addToOriginationSpace(exportSpace);
                exportConditions.add(new Conjunction(exportNetworkConditions));
              });
    }

    // Always export BGP or IBGP routes.
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP, RoutingProtocol.IBGP));

    // Finally, the export policy ends with returning false: do not export unmatched routes.
    bgpCommonExportPolicy.getStatements().add(Statements.ReturnFalse.toStaticStatement());
    //
    //    // Generate BGP_NETWORK6_NETWORKS filter.
    //    if (ipv6af != null) {
    //      List<Route6FilterLine> lines =
    //          ipv6af.getIpv6Networks().keySet().stream()
    //              .map(p6 -> new Route6FilterLine(LineAction.PERMIT,
    // Prefix6Range.fromPrefix6(p6)))
    //              .collect(ImmutableList.toImmutableList());
    //      Route6FilterList localFilter6 =
    //          new Route6FilterList("~BGP_NETWORK6_NETWORKS_FILTER:" + vrfName + "~", lines);
    //      c.getRoute6FilterLists().put(localFilter6.getName(), localFilter6);
    //    }

    // Process active neighbors first.
    Map<Prefix, BgpActivePeerConfig> activeNeighbors =
        AristaConversions.getNeighbors(c, v, newBgpProcess, bgpGlobal, bgpVrf, _eosVxlan, _w);
    newBgpProcess.setNeighbors(ImmutableSortedMap.copyOf(activeNeighbors));

    // Process passive neighbors next
    Map<Prefix, BgpPassivePeerConfig> passiveNeighbors =
        AristaConversions.getPassiveNeighbors(
            c, v, newBgpProcess, bgpGlobal, bgpVrf, _eosVxlan, _peerFilters, _w);
    newBgpProcess.setPassiveNeighbors(ImmutableSortedMap.copyOf(passiveNeighbors));

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

  private org.batfish.datamodel.Interface toInterface(
      String ifaceName, Interface iface, Map<String, IpAccessList> ipAccessLists, Configuration c) {
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(ifaceName)
            .setOwner(c)
            .setType(computeInterfaceType(iface.getName(), c.getConfigurationFormat()))
            .build();
    if (newIface.getInterfaceType() == InterfaceType.VLAN) {
      newIface.setVlan(CommonUtil.getInterfaceVlanNumber(ifaceName));
    }
    String vrfName = iface.getVrf();
    Vrf vrf = _vrfs.computeIfAbsent(vrfName, Vrf::new);
    newIface.setDescription(iface.getDescription());
    newIface.setActive(!iface.getShutdown());
    newIface.setChannelGroup(iface.getChannelGroup());
    newIface.setCryptoMap(iface.getCryptoMap());
    newIface.setAutoState(iface.getAutoState());
    newIface.setVrf(c.getVrfs().get(vrfName));
    newIface.setSpeed(firstNonNull(iface.getSpeed(), Interface.getDefaultSpeed(iface.getName())));
    newIface.setBandwidth(
        firstNonNull(
            iface.getBandwidth(),
            newIface.getSpeed(),
            Interface.getDefaultBandwidth(iface.getName())));
    if (iface.getDhcpRelayClient()) {
      newIface.setDhcpRelayAddresses(_dhcpRelayServers);
    } else {
      newIface.setDhcpRelayAddresses(ImmutableList.copyOf(iface.getDhcpRelayAddresses()));
    }
    newIface.setMlagId(iface.getMlagId());
    newIface.setMtu(getInterfaceMtu(iface));
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

    // subinterface settings
    newIface.setEncapsulationVlan(iface.getEncapsulationVlan());

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
      if (iface.getAllowedVlans() != null) {
        newIface.setAllowedVlans(iface.getAllowedVlans());
      } else if (!iface.getVlanTrunkGroups().isEmpty()) {
        newIface.setAllowedVlans(
            iface.getVlanTrunkGroups().stream()
                .map(_eosVlanTrunkGroups::get)
                .map(VlanTrunkGroup::getVlans)
                .reduce(IntegerSpace::union)
                .get());
      } else {
        newIface.setAllowedVlans(Interface.ALL_VLANS);
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

    /*
     * NAT rules are specified at the top level, but are applied as incoming transformations on the
     * outside interface (outside-to-inside) and outgoing transformations on the outside interface
     * (inside-to-outside)
     *
     * Currently, only static NATs have both incoming and outgoing transformations
     */

    List<AristaDynamicSourceNat> aristaDynamicSourceNats =
        firstNonNull(iface.getAristaNats(), ImmutableList.of());
    if (!aristaDynamicSourceNats.isEmpty()) {
      generateAristaDynamicSourceNats(newIface, aristaDynamicSourceNats);
    }

    String routingPolicyName = iface.getRoutingPolicy();
    if (routingPolicyName != null) {
      newIface.setRoutingPolicy(routingPolicyName);
    }

    return newIface;
  }

  private void generateAristaDynamicSourceNats(
      org.batfish.datamodel.Interface newIface,
      List<AristaDynamicSourceNat> aristaDynamicSourceNats) {
    ConcreteInterfaceAddress address = newIface.getConcreteAddress();
    if (address == null) {
      // nothing to do.
      return;
    }
    Ip interfaceIp = address.getIp();
    Transformation next = null;
    for (AristaDynamicSourceNat nat : Lists.reverse(aristaDynamicSourceNats)) {
      next = nat.toTransformation(interfaceIp, _natPools, next).orElse(next);
    }
    newIface.setOutgoingTransformation(next);
  }

  private If convertOspfRedistributionPolicy(OspfRedistributionPolicy policy, OspfProcess proc) {
    RoutingProtocol protocol = policy.getSourceProtocol();
    // All redistribution must match the specified protocol.
    Conjunction ospfExportConditions = new Conjunction();
    if (protocol == RoutingProtocol.ISIS_ANY) {
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

    ImmutableList.Builder<Statement> ospfExportStatements = ImmutableList.builder();

    // Set the metric type and value.
    ospfExportStatements.add(new SetOspfMetricType(policy.getMetricType()));
    long metric =
        policy.getMetric() != null ? policy.getMetric() : proc.getEffectiveDefaultMetric();
    // On Arista, the default route gets a special metric of 1.
    // https://www.arista.com/en/um-eos/eos-section-30-5-ospfv2-commands#ww1153059
    ospfExportStatements.add(
        new If(
            Common.matchDefaultRoute(),
            ImmutableList.of(new SetMetric(new LiteralLong(1L))),
            ImmutableList.of(new SetMetric(new LiteralLong(metric)))));

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
      OspfProcess proc, String vrfName, Configuration c, AristaConfiguration oldConfig) {
    Ip routerId = proc.getRouterId();
    if (routerId == null) {
      routerId = Conversions.getHighestIp(oldConfig.getInterfaces());
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
      assert vsIface != null;

      // How many OSPF processes are there in this vrf?
      boolean multiprocess = _vrfs.get(vrfName).getOspfProcesses().size() > 1;
      OspfNetwork network = getOspfNetworkForInterface(vsIface, proc);
      if (multiprocess && network == null && !proc.getName().equals(iface.getOspfProcess())) {
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
            summary.getAdvertised()
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
                    Common.matchDefaultRoute(),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement())))
            .build();
        route.setGenerationPolicy(defaultRouteGenerationPolicyName);
        newProcess.addGeneratedRoute(route.build());
      }
      ospfExportDefaultStatements.add(Statements.ExitAccept.toStaticStatement());
      ospfExportDefault.setGuard(
          new Conjunction(
              ImmutableList.of(
                  Common.matchDefaultRoute(), new MatchProtocol(RoutingProtocol.AGGREGATE))));
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
      ripExportDefaultConditions.getConjuncts().add(Common.matchDefaultRoute());
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

  private RoutingPolicy toRoutingPolicy(Configuration c, RouteMap map) {
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
                AristaStructureType.ROUTE_MAP_CLAUSE,
                name,
                AristaStructureUsage.ROUTE_MAP_CONTINUE,
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
    c.getVendorFamily().setCisco(_cf);
    c.setDeviceModel(DeviceModel.ARISTA_UNSPECIFIED);
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
    // TODO: logging configuration is per-vrf, but VI only has one.
    // Pick the default vrf source interface, and then union hosts across all.
    c.setLoggingSourceInterface(_vrfs.get(DEFAULT_VRF_NAME).getLoggingSourceInterface());
    c.setLoggingServers(
        _vrfs.values().stream()
            .map(Vrf::getLoggingHosts)
            .flatMap(map -> map.values().stream())
            .map(LoggingHost::getHost)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural())));
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
      AsPathAccessList apList = Conversions.toAsPathAccessList(pathList);
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

    // convert prefix lists to route filter lists
    for (PrefixList prefixList : _prefixLists.values()) {
      RouteFilterList newRouteFilterList = Conversions.toRouteFilterList(prefixList);
      c.getRouteFilterLists().put(newRouteFilterList.getName(), newRouteFilterList);
    }

    // convert ipv6 prefix lists to route6 filter lists
    for (Prefix6List prefixList : _prefix6Lists.values()) {
      Route6FilterList newRouteFilterList = Conversions.toRoute6FilterList(prefixList);
      c.getRoute6FilterLists().put(newRouteFilterList.getName(), newRouteFilterList);
    }

    // convert standard/extended access lists to access lists or route filter
    // lists
    for (StandardAccessList saList : _standardAccessLists.values()) {
      if (isAclUsedForRouting(saList.getName())) {
        RouteFilterList rfList = Conversions.toRouteFilterList(saList);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      c.getIpAccessLists().put(saList.getName(), toIpAccessList(saList.toExtendedAccessList()));
    }
    for (ExtendedAccessList eaList : _extendedAccessLists.values()) {
      if (isAclUsedForRouting(eaList.getName())) {
        RouteFilterList rfList = Conversions.toRouteFilterList(eaList);
        c.getRouteFilterLists().put(rfList.getName(), rfList);
      }
      IpAccessList ipaList = toIpAccessList(eaList);
      c.getIpAccessLists().put(ipaList.getName(), ipaList);
    }

    // convert standard/extended ipv6 access lists to ipv6 access lists or
    // route6 filter
    // lists
    for (StandardIpv6AccessList saList : _standardIpv6AccessLists.values()) {
      if (isAclUsedForRoutingv6(saList.getName())) {
        Route6FilterList rfList = Conversions.toRoute6FilterList(saList);
        c.getRoute6FilterLists().put(rfList.getName(), rfList);
      }
      c.getIp6AccessLists()
          .put(saList.getName(), Conversions.toIp6AccessList(saList.toExtendedIpv6AccessList()));
    }
    for (ExtendedIpv6AccessList eaList : _extendedIpv6AccessLists.values()) {
      if (isAclUsedForRoutingv6(eaList.getName())) {
        Route6FilterList rfList = Conversions.toRoute6FilterList(eaList);
        c.getRoute6FilterLists().put(rfList.getName(), rfList);
      }
      Ip6AccessList ipaList = Conversions.toIp6AccessList(eaList);
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

    // convert interfaces
    _interfaces.forEach(
        (ifaceName, iface) -> {
          // Handle renaming interfaces for ASA devices
          String newIfaceName = iface.getName();
          org.batfish.datamodel.Interface newInterface =
              toInterface(newIfaceName, iface, c.getIpAccessLists(), c);
          String vrfName = iface.getVrf();
          if (vrfName == null) {
            throw new BatfishException("Missing vrf name for iface: '" + iface.getName() + "'");
          }
          c.getAllInterfaces().put(newIfaceName, newInterface);
        });
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
      if (!iface.getShutdown() && tunnel != null && tunnel.getMode() == TunnelMode.IPSEC_IPV4) {
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
            newVrf.getStaticRoutes().add(Conversions.toStaticRoute(c, staticRoute));
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

          // convert isis process
          IsisProcess isisProcess = vrf.getIsisProcess();
          if (isisProcess != null) {
            org.batfish.datamodel.isis.IsisProcess newIsisProcess =
                Conversions.toIsisProcess(isisProcess, c, this);
            newVrf.setIsisProcess(newIsisProcess);
          }

          // convert bgp process
          AristaBgpVrf aristaBgp = _aristaBgp == null ? null : _aristaBgp.getVrfs().get(vrfName);
          if (aristaBgp != null) {
            org.batfish.datamodel.BgpProcess newBgpProcess =
                toEosBgpProcess(c, getAristaBgp(), aristaBgp);
            newVrf.setBgpProcess(newBgpProcess);
          }
        });

    /*
     * Another pass over interfaces to push final settings to VI interfaces and issue final warnings
     * (e.g. has OSPF settings but no associated OSPF process)
     */
    _interfaces.forEach(
        (ifaceName, vsIface) -> {
          org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
          if (iface == null) {
            // Should never get here
          } else {
            // Conversion of interface OSPF settings usually occurs per area
            // If the iface does not have an area, then need warn and convert settings here instead
            if (iface.getOspfAreaName() == null) {
              // Not part of an OSPF area
              if (vsIface.getOspfArea() != null
                  || vsIface.getOspfCost() != null
                  || vsIface.getOspfPassive() != null
                  || vsIface.getOspfNetworkType() != null
                  || vsIface.getOspfDeadInterval() != null
                  || vsIface.getOspfHelloInterval() != null) {
                _w.redFlag(
                    "Interface: '"
                        + ifaceName
                        + "' contains OSPF settings, but there is no corresponding OSPF area (or process)");
                finalizeInterfaceOspfSettings(iface, vsIface, null, null);
              }
            }
          }
        });

    // convert Arista EOS VXLAN
    if (_eosVxlan != null) {
      String sourceIfaceName = _eosVxlan.getSourceInterface();
      Interface sourceIface = sourceIfaceName == null ? null : _interfaces.get(sourceIfaceName);

      _eosVxlan
          .getVlanVnis()
          .forEach(
              (vlan, vni) -> {
                org.batfish.datamodel.Vrf vrf = getVrfForVlan(c, vlan).orElse(c.getDefaultVrf());
                vrf.addLayer2Vni(toL2Vni(_eosVxlan, vni, vlan, sourceIface));
              });
      _eosVxlan
          .getVrfToVni()
          .forEach(
              (vrfName, vni) ->
                  Optional.ofNullable(c.getVrfs().get(vrfName))
                      .ifPresent(vrf -> vrf.addLayer3Vni(toL3Vni(_eosVxlan, vni, sourceIface))));
    }

    // For EOS, if a VRF has VNIs but no BGP process, create a dummy BGP processes in VI.
    if (_eosVxlan != null) {
      // handle L3 VNIs
      _eosVxlan
          .getVrfToVni()
          .forEach(
              (vrfName, vni) -> {
                org.batfish.datamodel.Vrf viVrf = c.getVrfs().get(vrfName);
                if (viVrf != null && viVrf.getBgpProcess() == null) {
                  viVrf.setBgpProcess(
                      org.batfish.datamodel.BgpProcess.builder()
                          .setRouterId(Ip.ZERO)
                          .setAdminCostsToVendorDefaults(ConfigurationFormat.ARISTA)
                          .build());
                }
              });
      // handle L2 VNIs
      org.batfish.datamodel.BgpProcess defaultVrfBgpProc = c.getDefaultVrf().getBgpProcess();
      c.getVrfs()
          .values()
          .forEach(
              vrf -> {
                if (!vrf.getLayer2Vnis().isEmpty() // have L2 vnis in this VRF
                    && vrf.getBgpProcess() == null // no bgp process in this VRF
                    && defaultVrfBgpProc != null // Default VRF has a BGP process
                    && defaultVrfBgpProc
                        .allPeerConfigsStream()
                        .map(BgpPeerConfig::getEvpnAddressFamily)
                        // Default VRF has peers with EVPN enabled (so not just static vxlan)
                        .anyMatch(Objects::nonNull)) {
                  vrf.setBgpProcess(
                      org.batfish.datamodel.BgpProcess.builder()
                          .setRouterId(Ip.ZERO)
                          .setAdminCostsToVendorDefaults(ConfigurationFormat.ARISTA)
                          .build());
                }
              });
    }

    // Define the Null0 interface if it has been referenced. Otherwise, these show as undefined
    // references.
    Optional<Integer> firstRefToNull0 =
        _structureReferences.getOrDefault(AristaStructureType.INTERFACE, ImmutableSortedMap.of())
            .getOrDefault("Null0", ImmutableSortedMap.of()).entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .min(Integer::compare);
    if (firstRefToNull0.isPresent()) {
      defineSingleLineStructure(AristaStructureType.INTERFACE, "Null0", firstRefToNull0.get());
    }

    markConcreteStructure(AristaStructureType.BFD_TEMPLATE);
    markConcreteStructure(AristaStructureType.INTERFACE);
    markConcreteStructure(AristaStructureType.IPV4_ACCESS_LIST_STANDARD);

    // mark references to ACLs that may not appear in data model
    markIpOrMacAcls(
        AristaStructureUsage.CLASS_MAP_ACCESS_GROUP, AristaStructureUsage.CLASS_MAP_ACCESS_LIST);
    markIpv4Acls(
        AristaStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_IN,
        AristaStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_OUT,
        AristaStructureUsage.CONTROL_PLANE_ACCESS_GROUP,
        AristaStructureUsage.INTERFACE_IGMP_STATIC_GROUP_ACL,
        AristaStructureUsage.INTERFACE_IP_ACCESS_GROUP_IN,
        AristaStructureUsage.INTERFACE_IP_ACCESS_GROUP_OUT,
        AristaStructureUsage.INTERFACE_PIM_NEIGHBOR_FILTER,
        AristaStructureUsage.IP_NAT_DESTINATION_ACCESS_LIST,
        AristaStructureUsage.IP_NAT_SOURCE_ACCESS_LIST,
        AristaStructureUsage.LINE_ACCESS_CLASS_LIST,
        AristaStructureUsage.MANAGEMENT_SSH_ACCESS_GROUP,
        AristaStructureUsage.MANAGEMENT_TELNET_ACCESS_GROUP,
        AristaStructureUsage.MSDP_PEER_SA_LIST,
        AristaStructureUsage.NTP_ACCESS_GROUP,
        AristaStructureUsage.PIM_ACCEPT_REGISTER_ACL,
        AristaStructureUsage.PIM_ACCEPT_RP_ACL,
        AristaStructureUsage.PIM_RP_ADDRESS_ACL,
        AristaStructureUsage.PIM_RP_ANNOUNCE_FILTER,
        AristaStructureUsage.PIM_RP_CANDIDATE_ACL,
        AristaStructureUsage.PIM_SEND_RP_ANNOUNCE_ACL,
        AristaStructureUsage.PIM_SPT_THRESHOLD_ACL,
        AristaStructureUsage.ROUTE_MAP_MATCH_IPV4_ACCESS_LIST,
        AristaStructureUsage.SNMP_SERVER_COMMUNITY_ACL4,
        AristaStructureUsage.SSH_IPV4_ACL);
    markIpv6Acls(
        AristaStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_IN,
        AristaStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_OUT,
        AristaStructureUsage.LINE_ACCESS_CLASS_LIST6,
        AristaStructureUsage.NTP_ACCESS_GROUP,
        AristaStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST,
        AristaStructureUsage.SNMP_SERVER_COMMUNITY_ACL6,
        AristaStructureUsage.SSH_IPV6_ACL,
        AristaStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_IN,
        AristaStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_OUT);
    markAcls(
        AristaStructureUsage.ACCESS_GROUP_GLOBAL_FILTER,
        AristaStructureUsage.COPS_LISTENER_ACCESS_LIST,
        AristaStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL,
        AristaStructureUsage.INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP,
        AristaStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL,
        AristaStructureUsage.INTERFACE_IGMP_HOST_PROXY_ACCESS_LIST,
        AristaStructureUsage.INTERFACE_IP_ACCESS_GROUP_IN,
        AristaStructureUsage.INTERFACE_IP_INBAND_ACCESS_GROUP,
        AristaStructureUsage.INTERFACE_IP_ACCESS_GROUP_OUT,
        AristaStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN,
        AristaStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT,
        AristaStructureUsage.RIP_DISTRIBUTE_LIST,
        AristaStructureUsage.ROUTER_ISIS_DISTRIBUTE_LIST_ACL,
        AristaStructureUsage.SNMP_SERVER_FILE_TRANSFER_ACL,
        AristaStructureUsage.SNMP_SERVER_TFTP_SERVER_LIST,
        AristaStructureUsage.SNMP_SERVER_COMMUNITY_ACL,
        AristaStructureUsage.SSH_ACL,
        AristaStructureUsage.WCCP_GROUP_LIST,
        AristaStructureUsage.WCCP_REDIRECT_LIST,
        AristaStructureUsage.WCCP_SERVICE_LIST);

    markCommunityLists(
        AristaStructureUsage.ROUTE_MAP_ADD_COMMUNITY,
        AristaStructureUsage.ROUTE_MAP_DELETE_COMMUNITY,
        AristaStructureUsage.ROUTE_MAP_MATCH_COMMUNITY_LIST,
        AristaStructureUsage.ROUTE_MAP_SET_COMMUNITY);

    markConcreteStructure(AristaStructureType.PREFIX_LIST);
    markConcreteStructure(AristaStructureType.PREFIX6_LIST);

    // mark references to route-maps
    markConcreteStructure(AristaStructureType.ROUTE_MAP);

    // Cable
    markConcreteStructure(AristaStructureType.DEPI_CLASS);
    markConcreteStructure(AristaStructureType.DEPI_TUNNEL);
    markConcreteStructure(AristaStructureType.DOCSIS_POLICY);
    markConcreteStructure(AristaStructureType.DOCSIS_POLICY_RULE);
    markConcreteStructure(AristaStructureType.SERVICE_CLASS);

    // L2tp
    markConcreteStructure(AristaStructureType.L2TP_CLASS);

    // Crypto, Isakmp, and IPSec
    markConcreteStructure(AristaStructureType.CRYPTO_DYNAMIC_MAP_SET);
    markConcreteStructure(AristaStructureType.ISAKMP_PROFILE);
    markConcreteStructure(AristaStructureType.ISAKMP_POLICY);
    markConcreteStructure(AristaStructureType.IPSEC_PROFILE);
    markConcreteStructure(AristaStructureType.IPSEC_TRANSFORM_SET);
    markConcreteStructure(AristaStructureType.KEYRING);
    markConcreteStructure(AristaStructureType.NAMED_RSA_PUB_KEY);

    // class-map
    markConcreteStructure(AristaStructureType.INSPECT_CLASS_MAP);
    markConcreteStructure(AristaStructureType.CLASS_MAP);

    // policy-map
    markConcreteStructure(AristaStructureType.INSPECT_POLICY_MAP);
    markConcreteStructure(AristaStructureType.POLICY_MAP);

    // service template
    markConcreteStructure(AristaStructureType.SERVICE_TEMPLATE);

    // VXLAN
    markConcreteStructure(AristaStructureType.VXLAN);

    markConcreteStructure(AristaStructureType.NAT_POOL);
    markConcreteStructure(AristaStructureType.AS_PATH_ACCESS_LIST);

    markConcreteStructure(AristaStructureType.BGP_PEER_GROUP);

    return ImmutableList.of(c);
  }

  private static Layer2Vni toL2Vni(
      @Nonnull AristaEosVxlan vxlan, int vni, int vlan, @Nullable Interface sourceInterface) {
    Ip sourceAddress =
        sourceInterface == null
            ? null
            : sourceInterface.getAddress() == null ? null : sourceInterface.getAddress().getIp();

    // Prefer VLAN-specific or general flood address (in that order) over multicast address
    SortedSet<Ip> bumTransportIps =
        firstNonNull(vxlan.getVlanFloodAddresses().get(vlan), vxlan.getFloodAddresses());

    // default to unicast flooding unless specified otherwise
    BumTransportMethod bumTransportMethod = BumTransportMethod.UNICAST_FLOOD_GROUP;

    // Check if multicast is enabled
    Ip multicastAddress = vxlan.getMulticastGroup();
    if (bumTransportIps.isEmpty() && multicastAddress != null) {
      bumTransportMethod = BumTransportMethod.MULTICAST_GROUP;
      bumTransportIps = ImmutableSortedSet.of(multicastAddress);
    }

    return Layer2Vni.builder()
        .setBumTransportIps(bumTransportIps)
        .setBumTransportMethod(bumTransportMethod)
        .setSourceAddress(sourceAddress)
        .setUdpPort(firstNonNull(vxlan.getUdpPort(), AristaEosVxlan.DEFAULT_UDP_PORT))
        .setVlan(vlan)
        .setVni(vni)
        .setSrcVrf(Configuration.DEFAULT_VRF_NAME)
        .build();
  }

  private static Layer3Vni toL3Vni(
      @Nonnull AristaEosVxlan vxlan, @Nonnull Integer vni, @Nullable Interface sourceInterface) {
    Ip sourceAddress =
        sourceInterface == null
            ? null
            : sourceInterface.getAddress() == null ? null : sourceInterface.getAddress().getIp();

    SortedSet<Ip> bumTransportIps = vxlan.getFloodAddresses();

    // default to unicast flooding unless specified otherwise
    BumTransportMethod bumTransportMethod = BumTransportMethod.UNICAST_FLOOD_GROUP;

    // Check if multicast is enabled
    Ip multicastAddress = vxlan.getMulticastGroup();
    if (bumTransportIps.isEmpty() && multicastAddress != null) {
      bumTransportMethod = BumTransportMethod.MULTICAST_GROUP;
      bumTransportIps = ImmutableSortedSet.of(multicastAddress);
    }

    return Layer3Vni.builder()
        .setBumTransportIps(bumTransportIps)
        .setBumTransportMethod(bumTransportMethod)
        .setSourceAddress(sourceAddress)
        .setUdpPort(firstNonNull(vxlan.getUdpPort(), AristaEosVxlan.DEFAULT_UDP_PORT))
        .setVni(vni)
        .setSrcVrf(Configuration.DEFAULT_VRF_NAME)
        .build();
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
              .setSourceType(AristaStructureType.INSPECT_CLASS_MAP.getDescription())
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
              .setSourceType(AristaStructureType.INSPECT_POLICY_MAP.getDescription())
              .build();
        });
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
      // TODO: do we need to check Arista bgp policies?
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
      // TODO: do we need to check Arista bgp policies?
    }
    return false;
  }

  private void markCommunityLists(AristaStructureUsage... usages) {
    for (AristaStructureUsage usage : usages) {
      markAbstractStructure(
          AristaStructureType.COMMUNITY_LIST,
          usage,
          ImmutableList.of(
              AristaStructureType.COMMUNITY_LIST_EXPANDED,
              AristaStructureType.COMMUNITY_LIST_STANDARD));
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

  public Map<String, InspectClassMap> getInspectClassMaps() {
    return _inspectClassMaps;
  }

  public Map<String, InspectPolicyMap> getInspectPolicyMaps() {
    return _inspectPolicyMaps;
  }

  public Map<String, TrackMethod> getTrackingGroups() {
    return _trackingGroups;
  }
}
