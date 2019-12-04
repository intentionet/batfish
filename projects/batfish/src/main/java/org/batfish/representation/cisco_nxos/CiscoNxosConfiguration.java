package org.batfish.representation.cisco_nxos;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.batfish.datamodel.BumTransportMethod.MULTICAST_GROUP;
import static org.batfish.datamodel.BumTransportMethod.UNICAST_FLOOD_GROUP;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.datamodel.Names.generatedBgpCommonExportPolicyName;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.routing_policy.Common.generateGenerationPolicy;
import static org.batfish.datamodel.routing_policy.Common.matchDefaultRoute;
import static org.batfish.datamodel.routing_policy.Common.suppressSummarizedPrefixes;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.PORT_CHANNEL;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.CLASS_MAP_CP_MATCH_ACCESS_GROUP;
import static org.batfish.representation.cisco_nxos.Conversions.getVrfForL3Vni;
import static org.batfish.representation.cisco_nxos.Conversions.inferRouterId;
import static org.batfish.representation.cisco_nxos.Interface.BANDWIDTH_CONVERSION_FACTOR;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultBandwidth;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultSpeed;
import static org.batfish.representation.cisco_nxos.OspfInterface.DEFAULT_DEAD_INTERVAL_S;
import static org.batfish.representation.cisco_nxos.OspfInterface.DEFAULT_HELLO_INTERVAL_S;
import static org.batfish.representation.cisco_nxos.OspfInterface.OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER;
import static org.batfish.representation.cisco_nxos.OspfMaxMetricRouterLsa.DEFAULT_OSPF_MAX_METRIC;
import static org.batfish.representation.cisco_nxos.OspfProcess.DEFAULT_LOOPBACK_OSPF_COST;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.common.runtime.InterfaceRuntimeData;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRouteMetadata;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6Range;
import org.batfish.datamodel.Prefix6Space;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Route6FilterLine;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.eigrp.EigrpProcess;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.isis.IsisMetricType;
import org.batfish.datamodel.ospf.NssaSettings;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.StubSettings;
import org.batfish.datamodel.packet_policy.BoolExpr;
import org.batfish.datamodel.packet_policy.FalseExpr;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.FibLookupOverrideLookupIp;
import org.batfish.datamodel.packet_policy.IngressInterfaceVrf;
import org.batfish.datamodel.packet_policy.PacketMatchExpr;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.packet_policy.Return;
import org.batfish.datamodel.packet_policy.TrueExpr;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.ColonSeparatedRendering;
import org.batfish.datamodel.routing_policy.communities.CommunityAcl;
import org.batfish.datamodel.routing_policy.communities.CommunityAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunityIn;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExprReference;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAcl;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunitySetDifference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExprReference;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySetUnion;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.communities.MatchCommunities;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.communities.TypesFirstAscendingSpaceSeparated;
import org.batfish.datamodel.routing_policy.expr.AutoAs;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork6;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitAs;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefix6Set;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.IntComparator;
import org.batfish.datamodel.routing_policy.expr.IpNextHop;
import org.batfish.datamodel.routing_policy.expr.LiteralAsList;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchAsPath;
import org.batfish.datamodel.routing_policy.expr.MatchMetric;
import org.batfish.datamodel.routing_policy.expr.MatchPrefix6Set;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.MatchTag;
import org.batfish.datamodel.routing_policy.expr.MultipliedAs;
import org.batfish.datamodel.routing_policy.expr.NamedAsPathSet;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.UnchangedNextHop;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.PrependAsPath;
import org.batfish.datamodel.routing_policy.statement.SetIsisMetricType;
import org.batfish.datamodel.routing_policy.statement.SetLocalPreference;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.SetTag;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.tracking.DecrementPriority;
import org.batfish.datamodel.vendor_family.cisco_nxos.CiscoNxosFamily;
import org.batfish.datamodel.vendor_family.cisco_nxos.NexusPlatform;
import org.batfish.datamodel.vendor_family.cisco_nxos.NxosMajorVersion;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;
import org.batfish.representation.cisco_nxos.BgpVrfIpv6AddressFamilyConfiguration.Network;
import org.batfish.representation.cisco_nxos.Nve.IngressReplicationProtocol;
import org.batfish.vendor.VendorConfiguration;

/** Vendor-specific representation of a Cisco NX-OS network configuration */
public final class CiscoNxosConfiguration extends VendorConfiguration {

  /*
   * This map is used to convert interface names to their canonical forms.
   * The entries are visited in insertion order until a key is found of which the name to convert is
   * case-insensitively a prefix. The value corresponding to that key is chosen as the canonical
   * form for that name.
   *
   * NOTE: Entries are sorted by priority. Do not reorder unless you have a good reason.
   */
  private static final Map<String, String> CISCO_NXOS_INTERFACE_PREFIXES;
  private static final Pattern CISCO_NXOS_INTERFACE_PREFIXES_REGEX;

  static {
    CISCO_NXOS_INTERFACE_PREFIXES =
        ImmutableMap.<String, String>builder()
            .put("Ethernet", "Ethernet")
            .put("loopback", "loopback")
            .put("mgmt", "mgmt")
            .put("Null", "Null")
            .put("nve", "nve")
            .put("port-channel", "port-channel")
            .put("Vlan", "Vlan")
            .build();
    CISCO_NXOS_INTERFACE_PREFIXES_REGEX =
        Pattern.compile(
            CISCO_NXOS_INTERFACE_PREFIXES.values().stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining("|")));
  }

  // https://www.cisco.com/c/en/us/td/docs/switches/datacenter/nexus9000/sw/7-x/vxlan/configuration/guide/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x_chapter_0100.html#ariaid-title14
  /** On NX-OS, there is a pre-populated VRF named "default". */
  public static final String DEFAULT_VRF_NAME = "default";
  /** On NX-OS, default VRF has id 1. */
  public static final int DEFAULT_VRF_ID = 1;
  /** On NX-OS, there is a pre-populated VRF named "management". */
  public static final String MANAGEMENT_VRF_NAME = "management";
  /** On NX-OS, management VRF has id 2. */
  public static final int MANAGEMENT_VRF_ID = 2;

  // https://www.cisco.com/c/en/us/td/docs/switches/datacenter/nexus7000/sw/qos/config/cisco_nexus7000_qos_config_guide_8x/configuring_classification.html
  /** On NX-OS, there is an implicit QoS class-map "class-default". */
  public static final String DEFAULT_CLASS_MAP_NAME = "class-default";
  /**
   * On NX-OS, there are implicit policy-maps "default-in-policy" and "default-out-policy" depending
   * on type.
   */
  public static final String DEFAULT_POLICY_MAP_IN = "default-in-policy";
  /**
   * On NX-OS, there are implicit policy-maps "default-in-policy" and "default-out-policy" depending
   * on type.
   */
  public static final String DEFAULT_POLICY_MAP_OUT = "default-out-policy";

  private int _currentContextVrfId;

  /** Returns canonical prefix of interface name if valid, else {@code null}. */
  public static @Nullable String getCanonicalInterfaceNamePrefix(String prefix) {
    for (Entry<String, String> e : CISCO_NXOS_INTERFACE_PREFIXES.entrySet()) {
      String matchPrefix = e.getKey();
      String canonicalPrefix = e.getValue();
      if (matchPrefix.toLowerCase().startsWith(prefix.toLowerCase())) {
        return canonicalPrefix;
      }
    }
    return null;
  }

  private static final IntegerSpace DEFAULT_RESERVED_VLAN_RANGE =
      IntegerSpace.of(Range.closed(3968, 4094));

  private static final int MAX_FRAGMENT_OFFSET = (1 << 13) - 1;
  private static final AclLineMatchExpr MATCH_INITIAL_FRAGMENT_OFFSET =
      match(
          HeaderSpace.builder()
              .setFragmentOffsets(ImmutableList.of(SubRange.singleton(0)))
              .build());
  private static final AclLineMatchExpr MATCH_NON_INITIAL_FRAGMENT_OFFSET =
      match(
          HeaderSpace.builder()
              .setFragmentOffsets(ImmutableList.of(new SubRange(1, MAX_FRAGMENT_OFFSET)))
              .build());

  private static final double OSPF_REFERENCE_BANDWIDTH_CONVERSION_FACTOR = 1E6D; // bps per Mbps

  /** Routing-related constants. */
  private static final int AGGREGATE_ROUTE_ADMIN_COST = 200;

  private static final double SPEED_CONVERSION_FACTOR = 1E6D;
  private static final Statement ROUTE_MAP_PERMIT_STATEMENT =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
          ImmutableList.of(Statements.ExitAccept.toStaticStatement()));
  private static final Statement ROUTE_MAP_DENY_STATEMENT =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
          ImmutableList.of(Statements.ExitReject.toStaticStatement()));

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

  private static @Nonnull Statement call(String routingPolicyName) {
    return new If(
        new CallExpr(routingPolicyName),
        ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
        ImmutableList.of(Statements.ReturnFalse.toStaticStatement()));
  }

  public static @Nonnull String toJavaRegex(String ciscoRegex) {
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

  private static @Nonnull RouteFilterLine toRouteFilterLine(IpPrefixListLine ipPrefixListLine) {
    return new RouteFilterLine(
        ipPrefixListLine.getAction(),
        ipPrefixListLine.getPrefix(),
        ipPrefixListLine.getLengthRange());
  }

  private static @Nonnull Route6FilterLine toRoute6FilterLine(
      Ipv6PrefixListLine ipv6PrefixListLine) {
    return new Route6FilterLine(
        ipv6PrefixListLine.getAction(),
        ipv6PrefixListLine.getPrefix6(),
        ipv6PrefixListLine.getLengthRange());
  }

  private static @Nonnull RouteFilterList toRouteFilterList(IpPrefixList ipPrefixList) {
    String name = ipPrefixList.getName();
    RouteFilterList rfl = new RouteFilterList(name);
    rfl.setLines(
        ipPrefixList.getLines().values().stream()
            .map(CiscoNxosConfiguration::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList()));
    return rfl;
  }

  private static @Nonnull Route6FilterList toRoute6FilterList(Ipv6PrefixList ipv6PrefixList) {
    String name = ipv6PrefixList.getName();
    Route6FilterList r6fl = new Route6FilterList(name);
    r6fl.setLines(
        ipv6PrefixList.getLines().values().stream()
            .map(CiscoNxosConfiguration::toRoute6FilterLine)
            .collect(ImmutableList.toImmutableList()));
    return r6fl;
  }

  private transient Configuration _c;
  private transient Multimap<Entry<String, String>, Long> _implicitOspfAreas;

  private @Nullable String _bannerExec;
  private @Nullable String _bannerMotd;
  private final @Nonnull BgpGlobalConfiguration _bgpGlobalConfiguration;
  private @Nullable String _bootKickstartSup1;
  private @Nullable String _bootKickstartSup2;
  private @Nullable String _bootNxosSup1;
  private @Nullable String _bootNxosSup2;
  private @Nullable String _bootSystemSup1;
  private @Nullable String _bootSystemSup2;
  private final @Nonnull Map<String, EigrpProcessConfiguration> _eigrpProcesses;
  private @Nullable Evpn _evpn;
  private @Nullable String _hostname;
  private final @Nonnull Map<String, Interface> _interfaces;
  private final @Nonnull Map<String, IpAccessList> _ipAccessLists;
  private final @Nonnull Map<String, IpAsPathAccessList> _ipAsPathAccessLists;
  private final @Nonnull Map<String, IpCommunityList> _ipCommunityLists;
  private @Nullable String _ipDomainName;
  private Map<String, List<String>> _ipNameServersByUseVrf;
  private final @Nonnull Map<String, IpPrefixList> _ipPrefixLists;
  private final @Nonnull Map<String, Ipv6AccessList> _ipv6AccessLists;
  private final @Nonnull Map<String, Ipv6PrefixList> _ipv6PrefixLists;
  private final @Nonnull Map<String, LoggingServer> _loggingServers;
  private @Nullable String _loggingSourceInterface;
  private @Nonnull NxosMajorVersion _majorVersion;
  private boolean _nonSwitchportDefaultShutdown;
  private final @Nonnull Map<String, NtpServer> _ntpServers;
  private @Nullable String _ntpSourceInterface;
  private final @Nonnull Map<Integer, Nve> _nves;
  private final @Nonnull Map<String, ObjectGroup> _objectGroups;
  private final @Nonnull Map<String, DefaultVrfOspfProcess> _ospfProcesses;
  private @Nonnull NexusPlatform _platform;
  private transient Multimap<String, String> _portChannelMembers;
  private @Nonnull IntegerSpace _reservedVlanRange;
  private final @Nonnull Map<String, RouteMap> _routeMaps;
  private final @Nonnull Map<String, SnmpServer> _snmpServers;
  private @Nullable String _snmpSourceInterface;
  private boolean _systemDefaultSwitchport;
  private boolean _systemDefaultSwitchportShutdown;
  private final @Nonnull Map<String, TacacsServer> _tacacsServers;
  private @Nullable String _tacacsSourceInterface;
  private @Nullable String _version;
  private final @Nonnull Map<Integer, Vlan> _vlans;
  private final @Nonnull Map<String, Vrf> _vrfs;

  public CiscoNxosConfiguration() {
    _bgpGlobalConfiguration = new BgpGlobalConfiguration();
    _eigrpProcesses = new HashMap<>();
    _interfaces = new HashMap<>();
    _ipAccessLists = new HashMap<>();
    _ipAsPathAccessLists = new HashMap<>();
    _ipCommunityLists = new HashMap<>();
    _ipNameServersByUseVrf = new HashMap<>();
    _ipPrefixLists = new HashMap<>();
    _ipv6AccessLists = new HashMap<>();
    _ipv6PrefixLists = new HashMap<>();
    _loggingServers = new HashMap<>();
    _majorVersion = NxosMajorVersion.UNKNOWN;
    _ntpServers = new HashMap<>();
    _nves = new HashMap<>();
    _objectGroups = new HashMap<>();
    _ospfProcesses = new HashMap<>();
    _platform = NexusPlatform.UNKNOWN;
    _reservedVlanRange = DEFAULT_RESERVED_VLAN_RANGE;
    _routeMaps = new HashMap<>();
    _snmpServers = new HashMap<>();
    _tacacsServers = new HashMap<>();
    _vlans = new HashMap<>();
    _vrfs = new HashMap<>();
    // Populate the default VRFs.
    Vrf def = getOrCreateVrf(DEFAULT_VRF_NAME);
    assert def.getId() == DEFAULT_VRF_ID;
    Vrf mgmt = getOrCreateVrf(MANAGEMENT_VRF_NAME);
    assert mgmt.getId() == MANAGEMENT_VRF_ID;
  }

  @Override
  public String canonicalizeInterfaceName(String ifaceName) {
    Matcher matcher = CISCO_NXOS_INTERFACE_PREFIXES_REGEX.matcher(ifaceName.toLowerCase());
    if (!matcher.find()) {
      throw new BatfishException("Invalid interface name: '" + ifaceName + "'");
    }
    String ifacePrefix = matcher.group();
    String canonicalPrefix = getCanonicalInterfaceNamePrefix(ifacePrefix);
    if (canonicalPrefix == null) {
      throw new BatfishException("Invalid interface name: '" + ifaceName + "'");
    }
    String suffix = ifaceName.substring(ifacePrefix.length());
    return canonicalPrefix + suffix;
  }

  private void convertBgp() {
    // Before we process any configuration, execute BGP inheritance.
    _bgpGlobalConfiguration.doInherit(_w);

    _bgpGlobalConfiguration
        .getVrfs()
        .forEach(
            (vrfName, bgpVrfConfig) -> {
              convertBgpVrf(_c, _bgpGlobalConfiguration, bgpVrfConfig, vrfName);
            });
    /*
     * If the VRF has a layer 3 VNI defined (but does not appear under "router bgp" in config),
     * it will still participate in BGP route exchange.
     * So make a dummy BgpProcess in VI land to avoid crashes and setup proper RIBs for that VRF.
     */
    _vrfs.values().stream()
        .filter(vrf -> vrf.getVni() != null)
        .forEach(
            vrf -> {
              org.batfish.datamodel.Vrf viVrf = _c.getVrfs().get(vrf.getName());
              if (viVrf.getBgpProcess() == null) {
                // If the VI vrf has no BGP process, create a dummy one
                viVrf.setBgpProcess(
                    BgpProcess.builder()
                        .setRouterId(inferRouterId(viVrf, _w, "BGP process"))
                        .setAdminCostsToVendorDefaults(_c.getConfigurationFormat())
                        .build());
              }
            });
  }

  private void convertBgpVrf(
      Configuration c,
      BgpGlobalConfiguration nxBgpGlobal,
      BgpVrfConfiguration nxBgpVrf,
      String vrfName) {
    org.batfish.datamodel.Vrf v = c.getVrfs().get(vrfName);
    if (v == null) {
      // Do nothing, but rely on the VRF having an undefined reference warning.
      return;
    }
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    org.batfish.datamodel.BgpProcess newBgpProcess =
        new org.batfish.datamodel.BgpProcess(
            Conversions.getBgpRouterId(nxBgpVrf, v, _w), ebgpAdmin, ibgpAdmin);
    if (nxBgpVrf.getBestpathCompareRouterId()) {
      newBgpProcess.setTieBreaker(BgpTieBreaker.ROUTER_ID);
    }

    // From NX-OS docs for `bestpath as-path multipath-relax`
    //  Allows load sharing across providers with different (but equal-length) autonomous system
    //  paths. Without this option, the AS paths must be identical for load sharing.
    newBgpProcess.setMultipathEquivalentAsPathMatchMode(
        nxBgpVrf.getBestpathAsPathMultipathRelax() ? PATH_LENGTH : EXACT_PATH);

    // Process vrf-level address family configuration, such as export policy.
    BgpVrfIpv4AddressFamilyConfiguration ipv4af = nxBgpVrf.getIpv4UnicastAddressFamily();
    if (ipv4af != null) {
      // Batfish seems to only track the IPv4 properties for multipath ebgp/ibgp.
      newBgpProcess.setMultipathEbgp(ipv4af.getMaximumPathsEbgp() > 1);
      newBgpProcess.setMultipathIbgp(ipv4af.getMaximumPathsIbgp() > 1);
    }

    // Next we build up the BGP common export policy.
    RoutingPolicy bgpCommonExportPolicy =
        new RoutingPolicy(generatedBgpCommonExportPolicyName(vrfName), c);
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
      for (Entry<Prefix, BgpVrfAddressFamilyAggregateNetworkConfiguration> e :
          ipv4af.getAggregateNetworks().entrySet()) {
        Prefix prefix = e.getKey();
        BgpVrfAddressFamilyAggregateNetworkConfiguration agg = e.getValue();
        RoutingPolicy genPolicy = generateGenerationPolicy(c, vrfName, prefix);

        GeneratedRoute.Builder gr =
            GeneratedRoute.builder()
                .setNetwork(prefix)
                .setAdmin(AGGREGATE_ROUTE_ADMIN_COST)
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
    BooleanExpr redistributeDefaultRoute =
        ipv4af == null || !ipv4af.getDefaultInformationOriginate()
            ? Conversions.NOT_DEFAULT_ROUTE
            : BooleanExprs.TRUE;

    // Export RIP routes that should be redistributed.
    List<RedistributionPolicy> ripPolicies =
        ipv4af == null
            ? ImmutableList.of()
            : ipv4af.getRedistributionPolicies(NxosRoutingProtocol.RIP);
    for (RedistributionPolicy ripPolicy : ripPolicies) {
      /* TODO: how do we match on source tag (aka RIP process id)? */
      String routeMap = ripPolicy.getRouteMap();
      org.batfish.representation.cisco_nxos.RouteMap map = _routeMaps.get(routeMap);
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
    RedistributionPolicy staticPolicy =
        ipv4af == null ? null : ipv4af.getRedistributionPolicy(RoutingProtocolInstance.staticc());
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
    RedistributionPolicy connectedPolicy =
        ipv4af == null ? null : ipv4af.getRedistributionPolicy(RoutingProtocolInstance.direct());
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
    List<RedistributionPolicy> ospfPolicies =
        ipv4af == null
            ? ImmutableList.of()
            : ipv4af.getRedistributionPolicies(NxosRoutingProtocol.OSPF);
    for (RedistributionPolicy ospfPolicy : ospfPolicies) {
      /* TODO: how do we match on source tag (aka OSPF process tag)? */
      String routeMap = ospfPolicy.getRouteMap();
      RouteMap map = _routeMaps.get(routeMap);
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
          .getNetworks()
          .forEach(
              network -> {
                PrefixSpace exportSpace =
                    new PrefixSpace(PrefixRange.fromPrefix(network.getNetwork()));
                @Nullable String routeMap = network.getRouteMap();
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
                            routeMap != null && _routeMaps.containsKey(routeMap)
                                ? new CallExpr(routeMap)
                                : BooleanExprs.TRUE,
                            OriginType.IGP));
                newBgpProcess.addToOriginationSpace(exportSpace);
                exportConditions.add(new Conjunction(exportNetworkConditions));
              });
    }

    BgpVrfIpv6AddressFamilyConfiguration ipv6af = nxBgpVrf.getIpv6UnicastAddressFamily();
    if (ipv6af != null) {
      ipv6af
          .getNetworks()
          .forEach(
              network -> {
                @Nullable String routeMap = network.getRouteMap();
                List<BooleanExpr> exportNetworkConditions =
                    ImmutableList.of(
                        new MatchPrefix6Set(
                            new DestinationNetwork6(),
                            new ExplicitPrefix6Set(
                                new Prefix6Space(Prefix6Range.fromPrefix6(network.getNetwork())))),
                        new Not(
                            new MatchProtocol(
                                RoutingProtocol.BGP,
                                RoutingProtocol.IBGP,
                                RoutingProtocol.AGGREGATE)),
                        bgpRedistributeWithEnvironmentExpr(
                            routeMap != null && _routeMaps.containsKey(routeMap)
                                ? new CallExpr(routeMap)
                                : BooleanExprs.TRUE,
                            OriginType.IGP));
                exportConditions.add(new Conjunction(exportNetworkConditions));
              });
    }

    // Always export BGP or IBGP routes.
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP, RoutingProtocol.IBGP));

    // Finally, the export policy ends with returning false: do not export unmatched routes.
    bgpCommonExportPolicy.getStatements().add(Statements.ReturnFalse.toStaticStatement());

    // Generate BGP_NETWORK6_NETWORKS filter.
    if (ipv6af != null) {
      List<Route6FilterLine> lines =
          ipv6af.getNetworks().stream()
              .map(Network::getNetwork)
              .map(p6 -> new Route6FilterLine(LineAction.PERMIT, Prefix6Range.fromPrefix6(p6)))
              .collect(ImmutableList.toImmutableList());
      Route6FilterList localFilter6 =
          new Route6FilterList("~BGP_NETWORK6_NETWORKS_FILTER:" + vrfName + "~", lines);
      c.getRoute6FilterLists().put(localFilter6.getName(), localFilter6);
    }

    // Process active neighbors first.
    Map<Prefix, BgpActivePeerConfig> activeNeighbors =
        Conversions.getNeighbors(c, this, v, newBgpProcess, nxBgpGlobal, nxBgpVrf, _w);
    newBgpProcess.setNeighbors(ImmutableSortedMap.copyOf(activeNeighbors));

    // Process passive neighbors next
    Map<Prefix, BgpPassivePeerConfig> passiveNeighbors =
        Conversions.getPassiveNeighbors(c, this, v, newBgpProcess, nxBgpGlobal, nxBgpVrf, _w);
    newBgpProcess.setPassiveNeighbors(ImmutableSortedMap.copyOf(passiveNeighbors));

    v.setBgpProcess(newBgpProcess);
  }

  private static void convertHsrp(
      InterfaceHsrp hsrp, org.batfish.datamodel.Interface.Builder newIfaceBuilder) {
    Optional.ofNullable(hsrp.getVersion())
        .map(Object::toString)
        .ifPresent(newIfaceBuilder::setHsrpVersion);
    newIfaceBuilder.setHsrpGroups(
        hsrp.getIpv4Groups().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey, hsrpGroupEntry -> toHsrpGroup(hsrpGroupEntry.getValue()))));
  }

  private void convertDomainName() {
    _c.setDomainName(_ipDomainName);
  }

  private void convertEigrp() {
    getEigrpProcesses().forEach(this::convertEigrpProcess);
  }

  private void convertEigrpProcess(String procName, EigrpProcessConfiguration processConfig) {
    processConfig
        .getVrfs()
        .forEach(
            (vrfName, vrfConfig) -> {
              convertEigrpProcessVrf(procName, processConfig, vrfName, vrfConfig);
            });
  }

  private void convertEigrpProcessVrf(
      String procName,
      EigrpProcessConfiguration processConfig,
      String vrfName,
      EigrpVrfConfiguration vrfConfig) {
    Integer asn = Optional.ofNullable(vrfConfig.getAsn()).orElse(processConfig.getAsn());
    if (asn == null) {
      _w.redFlag(
          String.format(
              "Must configure the EIGRP autonomous-system number for vrf %s in process %s",
              vrfName, procName));
      return;
    }
    org.batfish.datamodel.Vrf v = _c.getVrfs().get(vrfName);
    if (v == null) {
      // Already warned on undefined reference
      _w.redFlag(
          String.format(
              "Ignoring EIGRP configuration for non-existent vrf %s in process %s",
              vrfName, procName));
      return;
    }
    Ip routerId = vrfConfig.getRouterId();
    if (routerId == null) {
      routerId = inferRouterId(v, _w, "EIGRP process " + procName);
    }
    EigrpProcess.Builder proc = EigrpProcess.builder().setAsNumber(asn).setRouterId(routerId);
    proc.setMode(vrfConfig.getAsn() != null ? EigrpProcessMode.CLASSIC : EigrpProcessMode.NAMED);
    if (v.getEigrpProcesses().containsKey(Long.valueOf(asn))) {
      // TODO: figure out what this does and handle it.
      _w.redFlag(
          String.format(
              "VRF %s already has an EIGRP process for autonomous-system number %s. Skipping %s",
              vrfName, asn, procName));
    } else {
      v.addEigrpProcess(proc.build());
    }
  }

  private void convertInterface(Interface iface) {
    String ifaceName = iface.getName();
    org.batfish.datamodel.Interface newIface = toInterface(iface);
    _c.getAllInterfaces().put(ifaceName, newIface);
    org.batfish.datamodel.Vrf vrf = newIface.getVrf();
    vrf.getInterfaces().put(ifaceName, newIface);
  }

  private void convertInterfaces() {
    _portChannelMembers = HashMultimap.create();
    // non-port channels
    _interfaces.values().stream()
        .filter(iface -> iface.getType() != CiscoNxosInterfaceType.PORT_CHANNEL)
        .forEach(this::convertInterface);
    // port channels
    _interfaces.values().stream()
        .filter(iface -> iface.getType() == CiscoNxosInterfaceType.PORT_CHANNEL)
        .forEach(this::convertInterface);
  }

  private void convertObjectGroups() {
    _objectGroups.values().stream()
        .forEach(
            objectGroup ->
                objectGroup.accept(
                    new ObjectGroupVisitor<Void>() {
                      @Override
                      public Void visitObjectGroupIpAddress(
                          ObjectGroupIpAddress objectGroupIpAddress) {
                        _c.getIpSpaces()
                            .put(objectGroupIpAddress.getName(), toIpSpace(objectGroupIpAddress));
                        return null;
                      }
                    }));
  }

  private @Nonnull IpSpace toIpSpace(ObjectGroupIpAddress objectGroupIpAddress) {
    return AclIpSpace.permitting(
            objectGroupIpAddress.getLines().values().stream()
                .map(ObjectGroupIpAddressLine::getIpWildcard)
                .map(IpWildcard::toIpSpace))
        .build();
  }

  private void convertIpAccessLists() {
    _ipAccessLists.forEach(
        (name, ipAccessList) -> _c.getIpAccessLists().put(name, toIpAccessList(ipAccessList)));
  }

  private void convertIpv6AccessLists() {
    _ipv6AccessLists.forEach(
        (name, ipv6AccessList) ->
            _c.getIp6AccessLists().put(name, toIp6AccessList(ipv6AccessList)));
  }

  private void convertIpAsPathAccessLists() {
    _ipAsPathAccessLists.forEach(
        (name, ipAsPathAccessList) ->
            _c.getAsPathAccessLists().put(name, toAsPathAccessList(ipAsPathAccessList)));
  }

  private void convertIpCommunityLists() {
    // create CommunitySetMatchExpr for route-map match community
    _ipCommunityLists.forEach(
        (name, list) ->
            _c.getCommunitySetMatchExprs()
                .put(
                    name,
                    list.accept(
                        new IpCommunityListVisitor<CommunitySetMatchExpr>() {
                          @Override
                          public CommunitySetMatchExpr visitIpCommunityListExpanded(
                              IpCommunityListExpanded ipCommunityListExpanded) {
                            return toCommunitySetMatchExpr(ipCommunityListExpanded);
                          }

                          @Override
                          public CommunitySetMatchExpr visitIpCommunityListStandard(
                              IpCommunityListStandard ipCommunityListStandard) {
                            return toCommunitySetMatchExpr(ipCommunityListStandard);
                          }
                        })));

    // create CommunityMatchExpr for route-map set comm-list delete
    _ipCommunityLists.forEach(
        (name, list) ->
            _c.getCommunityMatchExprs()
                .put(
                    name,
                    list.accept(
                        new IpCommunityListVisitor<CommunityMatchExpr>() {
                          @Override
                          public CommunityMatchExpr visitIpCommunityListExpanded(
                              IpCommunityListExpanded ipCommunityListExpanded) {
                            return toCommunityMatchExpr(ipCommunityListExpanded);
                          }

                          @Override
                          public CommunityMatchExpr visitIpCommunityListStandard(
                              IpCommunityListStandard ipCommunityListStandard) {
                            return toCommunityMatchExpr(ipCommunityListStandard);
                          }
                        })));
  }

  private static CommunitySetMatchExpr toCommunitySetMatchExpr(
      IpCommunityListExpanded ipCommunityListExpanded) {
    return new CommunitySetAcl(
        ipCommunityListExpanded.getLines().values().stream()
            .map(CiscoNxosConfiguration::toCommunitySetAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunitySetAclLine toCommunitySetAclLine(
      IpCommunityListExpandedLine line) {
    return new CommunitySetAclLine(
        line.getAction(),
        new CommunitySetMatchRegex(
            new TypesFirstAscendingSpaceSeparated(ColonSeparatedRendering.instance()),
            toJavaRegex(line.getRegex())));
  }

  private static CommunitySetMatchExpr toCommunitySetMatchExpr(
      IpCommunityListStandard ipCommunityListStandard) {
    return new CommunitySetAcl(
        ipCommunityListStandard.getLines().values().stream()
            .map(CiscoNxosConfiguration::toCommunitySetAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunitySetAclLine toCommunitySetAclLine(
      IpCommunityListStandardLine line) {
    return new CommunitySetAclLine(
        line.getAction(),
        new CommunitySetMatchAll(
            line.getCommunities().stream()
                .map(community -> new HasCommunity(new CommunityIs(community)))
                .collect(ImmutableSet.toImmutableSet())));
  }

  private static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      IpCommunityListExpanded ipCommunityListExpanded) {
    return new CommunityAcl(
        ipCommunityListExpanded.getLines().values().stream()
            .map(CiscoNxosConfiguration::toCommunityAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunityAclLine toCommunityAclLine(IpCommunityListExpandedLine line) {
    return new CommunityAclLine(
        line.getAction(),
        new CommunityMatchRegex(ColonSeparatedRendering.instance(), toJavaRegex(line.getRegex())));
  }

  private static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      IpCommunityListStandard ipCommunityListStandard) {
    Set<Community> whitelist = new HashSet<>();
    Set<Community> blacklist = new HashSet<>();
    for (IpCommunityListStandardLine line : ipCommunityListStandard.getLines().values()) {
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

  private void convertIpNameServers() {
    _c.setDnsServers(
        _ipNameServersByUseVrf.values().stream()
            .flatMap(Collection::stream)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  private void convertTacacsServers() {
    _c.setTacacsServers(ImmutableSortedSet.copyOf(_tacacsServers.keySet()));
  }

  private void convertTacacsSourceInterface() {
    _c.setTacacsSourceInterface(_tacacsSourceInterface);
  }

  private void convertIpPrefixLists() {
    _ipPrefixLists.forEach(
        (name, ipPrefixList) ->
            _c.getRouteFilterLists().put(name, toRouteFilterList(ipPrefixList)));
  }

  private void convertIpv6PrefixLists() {
    _ipv6PrefixLists.forEach(
        (name, ipv6PrefixList) ->
            _c.getRoute6FilterLists().put(name, toRoute6FilterList(ipv6PrefixList)));
  }

  private void convertLoggingServers() {
    _c.setLoggingServers(ImmutableSortedSet.copyOf(_loggingServers.keySet()));
  }

  private void convertLoggingSourceInterface() {
    _c.setLoggingSourceInterface(_loggingSourceInterface);
  }

  private void convertNtpServers() {
    _c.setNtpServers(ImmutableSortedSet.copyOf(_ntpServers.keySet()));
  }

  private void convertNtpSourceInterface() {
    _c.setNtpSourceInterface(_ntpSourceInterface);
  }

  private void convertOspfProcesses() {
    _ospfProcesses
        .values()
        .forEach(
            proc -> {
              _c.getDefaultVrf().addOspfProcess(toOspfProcess(proc));
              proc.getVrfs()
                  .forEach(
                      (vrfName, ospfVrf) -> {
                        org.batfish.datamodel.Vrf vrf = _c.getVrfs().get(vrfName);
                        if (vrf == null) {
                          return;
                        }
                        vrf.addOspfProcess(toOspfProcess(proc, ospfVrf, vrf));
                      });
            });
  }

  private void convertRouteMaps() {
    _routeMaps.values().forEach(this::convertRouteMap);

    // Find which route maps are used for PBR
    _c.getAllInterfaces().values().stream()
        .map(org.batfish.datamodel.Interface::getRoutingPolicyName)
        .filter(Objects::nonNull)
        .distinct()
        // Extract route map objects
        .map(_routeMaps::get)
        .filter(Objects::nonNull)
        // Convert PBR route maps to packet policies
        .map(this::toPacketPolicy)
        .forEach(packetPolicy -> _c.getPacketPolicies().put(packetPolicy.getName(), packetPolicy));
  }

  private void convertSnmpServers() {
    _c.setSnmpTrapServers(ImmutableSortedSet.copyOf(_snmpServers.keySet()));
  }

  private void convertSnmpSourceInterface() {
    _c.setSnmpSourceInterface(_snmpSourceInterface);
  }

  private void convertStaticRoutes() {
    _vrfs.values().forEach(this::convertStaticRoutes);
  }

  private void convertStaticRoutes(Vrf vrf) {
    _c.getVrfs()
        .get(vrf.getName())
        .setStaticRoutes(
            vrf.getStaticRoutes().values().stream()
                .map(this::toStaticRoute)
                .filter(Objects::nonNull)
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  private void convertVrfs() {
    _vrfs.forEach((name, vrf) -> _c.getVrfs().put(name, toVrf(vrf)));
  }

  private void convertNves() {
    _nves
        .values()
        .forEach(nve -> nve.getMemberVnis().values().forEach(vni -> convertNveVni(nve, vni)));
  }

  private void convertNveVni(Nve nve, NveVni nveVni) {
    if (nve.isShutdown()) {
      return;
    }
    BumTransportMethod bumTransportMethod = getBumTransportMethod(nveVni, nve);
    SortedSet<Ip> bumTransportIps;
    if (nveVni.getIngressReplicationProtocol() != IngressReplicationProtocol.STATIC
        && bumTransportMethod == MULTICAST_GROUP) {
      bumTransportIps = ImmutableSortedSet.of(getMultiCastGroupIp(nveVni, nve));
    } else {
      bumTransportIps = ImmutableSortedSet.copyOf(nveVni.getPeerIps());
    }
    Integer vlan = getVlanForVni(nveVni.getVni());
    if (vlan == null) {
      return;
    }
    if (_c.getAllInterfaces().values().stream()
        .noneMatch(iface -> vlan.equals(iface.getVlan()) && iface.getActive())) {
      return;
    }

    if (nveVni.isAssociateVrf()) {
      // L3 VNI

      Vrf vsTenantVrfForL3Vni = getVrfForL3Vni(_vrfs, nveVni.getVni());
      if (vsTenantVrfForL3Vni == null || _c.getVrfs().get(vsTenantVrfForL3Vni.getName()) == null) {
        return;
      }
      Layer3Vni vniSettings =
          Layer3Vni.builder()
              .setBumTransportIps(bumTransportIps)
              .setBumTransportMethod(bumTransportMethod)
              .setSourceAddress(
                  nve.getSourceInterface() != null
                      ? getInterfaceIp(_c.getAllInterfaces(), nve.getSourceInterface())
                      : null)
              .setUdpPort(Layer2Vni.DEFAULT_UDP_PORT)
              .setVni(nveVni.getVni())
              .build();
      _c.getVrfs().get(vsTenantVrfForL3Vni.getName()).addLayer3Vni(vniSettings);
    } else {
      org.batfish.datamodel.Vrf viTenantVrfForL2Vni = getMemberVrfForVlan(vlan);
      Layer2Vni vniSettings =
          Layer2Vni.builder()
              .setBumTransportIps(bumTransportIps)
              .setBumTransportMethod(bumTransportMethod)
              .setSourceAddress(
                  nve.getSourceInterface() != null
                      ? getInterfaceIp(_c.getAllInterfaces(), nve.getSourceInterface())
                      : null)
              .setUdpPort(Layer2Vni.DEFAULT_UDP_PORT)
              .setVni(nveVni.getVni())
              .setVlan(vlan)
              .build();
      if (viTenantVrfForL2Vni == null) {
        return;
      }
      viTenantVrfForL2Vni.addLayer2Vni(vniSettings);
    }
  }

  /**
   * Gets the {@link org.batfish.datamodel.Vrf} which contains VLAN interface for {@code vlanNumber}
   * as its member
   *
   * @param vlanNumber VLAN number
   * @return {@link org.batfish.datamodel.Vrf} containing VLAN interface of {@code vlanNumber}
   */
  @Nullable
  private org.batfish.datamodel.Vrf getMemberVrfForVlan(int vlanNumber) {
    String vrfMemberForVlanIface =
        Optional.ofNullable(_interfaces.get(String.format("Vlan%d", vlanNumber)))
            .map(org.batfish.representation.cisco_nxos.Interface::getVrfMember)
            .orElse(null);

    // interface for this VLAN is not a member of any VRF
    if (vrfMemberForVlanIface == null) {
      return _c.getDefaultVrf();
    }

    // null if VRF member specified but is not valid
    return _c.getVrfs().get(vrfMemberForVlanIface);
  }

  @Nonnull
  private static BumTransportMethod getBumTransportMethod(NveVni nveVni, Nve nve) {
    if (nveVni.getIngressReplicationProtocol() == IngressReplicationProtocol.STATIC) {
      // since all multicast group commands are ignored in this case
      return UNICAST_FLOOD_GROUP;
    }
    return nveVni.getMcastGroup() != null
            || !nveVni.isAssociateVrf() && nve.getMulticastGroupL2() != null
            || nveVni.isAssociateVrf() && nve.getMulticastGroupL3() != null
        ? MULTICAST_GROUP
        : UNICAST_FLOOD_GROUP;
  }

  @Nonnull
  private static Ip getMultiCastGroupIp(NveVni nveVni, Nve nve) {
    if (nveVni.getMcastGroup() != null) {
      return nveVni.getMcastGroup();
    }
    if (nveVni.isAssociateVrf()) {
      assert nve.getMulticastGroupL3() != null;
      return nve.getMulticastGroupL3();
    }
    assert nve.getMulticastGroupL2() != null;
    return nve.getMulticastGroupL2();
  }

  @Nullable
  private Ip getInterfaceIp(
      Map<String, org.batfish.datamodel.Interface> interfaces, String ifaceName) {
    org.batfish.datamodel.Interface iface = interfaces.get(ifaceName);
    if (iface == null) {
      return null;
    }
    ConcreteInterfaceAddress concreteInterfaceAddress = iface.getConcreteAddress();
    if (concreteInterfaceAddress == null) {
      return null;
    }
    return concreteInterfaceAddress.getIp();
  }

  @Nullable
  private Integer getVlanForVni(Integer vni) {
    return _vlans.values().stream()
        .filter(vlan -> vni.equals(vlan.getVni()))
        .findFirst()
        .map(Vlan::getId)
        .orElse(null);
  }

  private @Nonnull CiscoNxosFamily createCiscoNxosFamily() {
    return CiscoNxosFamily.builder().setPlatform(_platform).setMajorVersion(_majorVersion).build();
  }

  /** Disable Vlan interfaces without corresponding top-level vlan declaration. */
  private void disableUnregisteredVlanInterfaces() {
    _c.getAllInterfaces().values().stream()
        .filter(
            iface ->
                iface.getInterfaceType() == InterfaceType.VLAN
                    && !_vlans.keySet().contains(iface.getVlan()))
        .forEach(iface -> iface.setActive(false));
  }

  public @Nullable String getBannerExec() {
    return _bannerExec;
  }

  public void setBannerExec(@Nullable String bannerExec) {
    _bannerExec = bannerExec;
  }

  public @Nullable String getBannerMotd() {
    return _bannerMotd;
  }

  public void setBannerMotd(@Nullable String bannerMotd) {
    _bannerMotd = bannerMotd;
  }

  public @Nonnull BgpGlobalConfiguration getBgpGlobalConfiguration() {
    return _bgpGlobalConfiguration;
  }

  public @Nullable String getBootKickstartSup1() {
    return _bootKickstartSup1;
  }

  public @Nullable String getBootKickstartSup2() {
    return _bootKickstartSup2;
  }

  public @Nullable String getBootNxosSup1() {
    return _bootNxosSup1;
  }

  public @Nullable String getBootNxosSup2() {
    return _bootNxosSup2;
  }

  public @Nullable String getBootSystemSup1() {
    return _bootSystemSup1;
  }

  public @Nullable String getBootSystemSup2() {
    return _bootSystemSup2;
  }

  public @Nonnull Vrf getDefaultVrf() {
    return _vrfs.get(DEFAULT_VRF_NAME);
  }

  public @Nonnull Map<String, EigrpProcessConfiguration> getEigrpProcesses() {
    return Collections.unmodifiableMap(_eigrpProcesses);
  }

  public @Nullable EigrpProcessConfiguration getEigrpProcess(String processTag) {
    return _eigrpProcesses.get(processTag);
  }

  public @Nonnull EigrpProcessConfiguration getOrCreateEigrpProcess(String processTag) {
    return _eigrpProcesses.computeIfAbsent(processTag, name -> new EigrpProcessConfiguration());
  }

  public @Nullable Evpn getEvpn() {
    return _evpn;
  }

  public void setEvpn(@Nullable Evpn evpn) {
    _evpn = evpn;
  }

  @Override
  public @Nullable String getHostname() {
    return _hostname;
  }

  public @Nonnull Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull Map<String, IpAccessList> getIpAccessLists() {
    return _ipAccessLists;
  }

  public @Nonnull Map<String, IpAsPathAccessList> getIpAsPathAccessLists() {
    return _ipAsPathAccessLists;
  }

  public @Nonnull Map<String, IpCommunityList> getIpCommunityLists() {
    return _ipCommunityLists;
  }

  public @Nullable String getIpDomainName() {
    return _ipDomainName;
  }

  public @Nonnull Map<String, List<String>> getIpNameServersByUseVrf() {
    return _ipNameServersByUseVrf;
  }

  public @Nonnull Map<String, IpPrefixList> getIpPrefixLists() {
    return _ipPrefixLists;
  }

  public @Nonnull Map<String, Ipv6AccessList> getIpv6AccessLists() {
    return _ipv6AccessLists;
  }

  public @Nonnull Map<String, Ipv6PrefixList> getIpv6PrefixLists() {
    return _ipv6PrefixLists;
  }

  public @Nonnull Map<String, LoggingServer> getLoggingServers() {
    return _loggingServers;
  }

  public @Nullable String getLoggingSourceInterface() {
    return _loggingSourceInterface;
  }

  public @Nonnull NxosMajorVersion getMajorVersion() {
    return _majorVersion;
  }

  public @Nonnull Map<String, NtpServer> getNtpServers() {
    return _ntpServers;
  }

  public @Nullable String getNtpSourceInterface() {
    return _ntpSourceInterface;
  }

  public @Nonnull Map<Integer, Nve> getNves() {
    return _nves;
  }

  public @Nonnull Map<String, ObjectGroup> getObjectGroups() {
    return _objectGroups;
  }

  public @Nonnull Map<String, DefaultVrfOspfProcess> getOspfProcesses() {
    return _ospfProcesses;
  }

  public @Nonnull NexusPlatform getPlatform() {
    return _platform;
  }

  /** Range of VLAN IDs reserved by the system and therefore unassignable. */
  public @Nonnull IntegerSpace getReservedVlanRange() {
    return _reservedVlanRange;
  }

  public @Nonnull Map<String, RouteMap> getRouteMaps() {
    return _routeMaps;
  }

  public @Nonnull Map<String, SnmpServer> getSnmpServers() {
    return _snmpServers;
  }

  public @Nullable String getSnmpSourceInterface() {
    return _snmpSourceInterface;
  }

  public boolean getSystemDefaultSwitchport() {
    return _systemDefaultSwitchport;
  }

  public boolean getSystemDefaultSwitchportShutdown() {
    return _systemDefaultSwitchportShutdown;
  }

  public @Nonnull Map<String, TacacsServer> getTacacsServers() {
    return _tacacsServers;
  }

  public @Nullable String getTacacsSourceInterface() {
    return _tacacsSourceInterface;
  }

  public @Nullable String getVersion() {
    return _version;
  }

  public void setVersion(@Nullable String version) {
    _version = version;
  }

  public @Nonnull Map<Integer, Vlan> getVlans() {
    return _vlans;
  }

  public @Nonnull Vrf getOrCreateVrf(String name) {
    return _vrfs.computeIfAbsent(name, n -> new Vrf(n, ++_currentContextVrfId));
  }

  /** Returns a read-only copy of the VRFs. */
  public @Nonnull Map<String, Vrf> getVrfs() {
    return Collections.unmodifiableMap(_vrfs);
  }

  private void markStructures() {
    markConcreteStructure(CiscoNxosStructureType.CLASS_MAP_CONTROL_PLANE);
    markConcreteStructure(CiscoNxosStructureType.CLASS_MAP_NETWORK_QOS);
    markConcreteStructure(CiscoNxosStructureType.CLASS_MAP_QOS);
    markConcreteStructure(CiscoNxosStructureType.CLASS_MAP_QUEUING);
    markConcreteStructure(CiscoNxosStructureType.INTERFACE);
    {
      // Mark abstract [mac|ip|ipv6] ACL references
      List<CiscoNxosStructureType> types =
          ImmutableList.of(
              CiscoNxosStructureType.IP_ACCESS_LIST,
              CiscoNxosStructureType.IPV6_ACCESS_LIST,
              CiscoNxosStructureType.MAC_ACCESS_LIST);
      for (CiscoNxosStructureUsage usage : ImmutableList.of(CLASS_MAP_CP_MATCH_ACCESS_GROUP)) {
        markAbstractStructure(
            CiscoNxosStructureType.IP_OR_MAC_ACCESS_LIST_ABSTRACT_REF, usage, types);
      }
    }
    {
      // Mark abstract [v4|v6] ACL references
      List<CiscoNxosStructureType> types =
          ImmutableList.of(
              CiscoNxosStructureType.IP_ACCESS_LIST, CiscoNxosStructureType.IPV6_ACCESS_LIST);
      for (CiscoNxosStructureUsage usage :
          ImmutableList.of(
              CiscoNxosStructureUsage.NTP_ACCESS_GROUP_PEER,
              CiscoNxosStructureUsage.NTP_ACCESS_GROUP_QUERY_ONLY,
              CiscoNxosStructureUsage.NTP_ACCESS_GROUP_SERVE,
              CiscoNxosStructureUsage.NTP_ACCESS_GROUP_SERVE_ONLY,
              CiscoNxosStructureUsage.SNMP_SERVER_COMMUNITY_USE_ACL)) {
        markAbstractStructure(CiscoNxosStructureType.IP_ACCESS_LIST_ABSTRACT_REF, usage, types);
      }
    }
    markConcreteStructure(CiscoNxosStructureType.IP_ACCESS_LIST);
    markConcreteStructure(CiscoNxosStructureType.IP_AS_PATH_ACCESS_LIST);
    {
      // Mark abstract community-list references
      List<CiscoNxosStructureType> types =
          ImmutableList.of(
              CiscoNxosStructureType.IP_COMMUNITY_LIST_STANDARD,
              CiscoNxosStructureType.IP_COMMUNITY_LIST_EXPANDED);
      for (CiscoNxosStructureUsage usage :
          ImmutableList.of(
              CiscoNxosStructureUsage.ROUTE_MAP_MATCH_COMMUNITY,
              CiscoNxosStructureUsage.ROUTE_MAP_SET_COMM_LIST_DELETE)) {
        markAbstractStructure(CiscoNxosStructureType.IP_COMMUNITY_LIST_ABSTRACT_REF, usage, types);
      }
    }
    markConcreteStructure(CiscoNxosStructureType.IP_PREFIX_LIST);
    markConcreteStructure(CiscoNxosStructureType.IPV6_ACCESS_LIST);
    markConcreteStructure(CiscoNxosStructureType.IPV6_PREFIX_LIST);
    markConcreteStructure(CiscoNxosStructureType.NVE);
    markConcreteStructure(CiscoNxosStructureType.OBJECT_GROUP_IP_ADDRESS);
    markConcreteStructure(CiscoNxosStructureType.POLICY_MAP_CONTROL_PLANE);
    markConcreteStructure(CiscoNxosStructureType.POLICY_MAP_NETWORK_QOS);
    markConcreteStructure(CiscoNxosStructureType.POLICY_MAP_QOS);
    markConcreteStructure(CiscoNxosStructureType.POLICY_MAP_QUEUING);
    markConcreteStructure(CiscoNxosStructureType.PORT_CHANNEL);
    markConcreteStructure(CiscoNxosStructureType.ROUTE_MAP);
    markConcreteStructure(CiscoNxosStructureType.ROUTE_MAP_ENTRY);

    markConcreteStructure(CiscoNxosStructureType.ROUTER_EIGRP);
    markConcreteStructure(CiscoNxosStructureType.ROUTER_ISIS);
    markConcreteStructure(CiscoNxosStructureType.ROUTER_OSPF);
    markConcreteStructure(CiscoNxosStructureType.ROUTER_OSPFV3);
    markConcreteStructure(CiscoNxosStructureType.ROUTER_RIP);

    markConcreteStructure(CiscoNxosStructureType.BGP_TEMPLATE_PEER);
    markConcreteStructure(CiscoNxosStructureType.BGP_TEMPLATE_PEER_POLICY);
    markConcreteStructure(CiscoNxosStructureType.BGP_TEMPLATE_PEER_SESSION);
    markConcreteStructure(CiscoNxosStructureType.VLAN);
    markConcreteStructure(CiscoNxosStructureType.VRF);
  }

  public void setBootKickstartSup1(@Nullable String bootKickstartSup1) {
    _bootKickstartSup1 = bootKickstartSup1;
  }

  public void setBootKickstartSup2(@Nullable String bootKickstartSup2) {
    _bootKickstartSup2 = bootKickstartSup2;
  }

  public void setBootNxosSup1(@Nullable String bootNxosSup1) {
    _bootNxosSup1 = bootNxosSup1;
  }

  public void setBootNxosSup2(@Nullable String bootNxosSup2) {
    _bootNxosSup2 = bootNxosSup2;
  }

  public void setBootSystemSup1(@Nullable String bootSystemSup1) {
    _bootSystemSup1 = bootSystemSup1;
  }

  public void setBootSystemSup2(@Nullable String bootSystemSup2) {
    _bootSystemSup2 = bootSystemSup2;
  }

  @Override
  public void setHostname(String hostname) {
    checkNotNull(hostname, "hostname cannot be null");
    _hostname = hostname.toLowerCase();
  }

  public void setIpDomainName(@Nullable String ipDomainName) {
    _ipDomainName = ipDomainName;
  }

  public void setLoggingSourceInterface(@Nullable String loggingSourceInterface) {
    _loggingSourceInterface = loggingSourceInterface;
  }

  public void setMajorVersion(NxosMajorVersion majorVersion) {
    _majorVersion = majorVersion;
  }

  public void setNonSwitchportDefaultShutdown(boolean nonSwitchportDefaultShutdown) {
    _nonSwitchportDefaultShutdown = nonSwitchportDefaultShutdown;
  }

  public void setNtpSourceInterface(@Nullable String ntpSourceInterface) {
    _ntpSourceInterface = ntpSourceInterface;
  }

  public void setPlatform(NexusPlatform platform) {
    _platform = platform;
  }

  public void setSnmpSourceInterface(@Nullable String snmpSourceInterface) {
    _snmpSourceInterface = snmpSourceInterface;
  }

  public void setTacacsSourceInterface(@Nullable String tacacsSourceInterface) {
    _tacacsSourceInterface = tacacsSourceInterface;
  }

  public void setSystemDefaultSwitchport(boolean systemDefaultSwitchport) {
    _systemDefaultSwitchport = systemDefaultSwitchport;
  }

  public void setSystemDefaultSwitchportShutdown(boolean systemDefaultSwitchportShutdown) {
    _systemDefaultSwitchportShutdown = systemDefaultSwitchportShutdown;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {}

  private static @Nonnull org.batfish.datamodel.hsrp.HsrpGroup toHsrpGroup(HsrpGroupIpv4 group) {
    org.batfish.datamodel.hsrp.HsrpGroup.Builder builder =
        org.batfish.datamodel.hsrp.HsrpGroup.builder()
            .setGroupNumber(group.getGroup())
            .setIp(group.getIp())
            .setPreempt(group.getPreempt());
    if (group.getHelloIntervalMs() != null) {
      builder.setHelloTime(group.getHelloIntervalMs());
    }
    if (group.getHoldTimeMs() != null) {
      builder.setHoldTime(group.getHoldTimeMs());
    }
    if (group.getPriority() != null) {
      builder.setPriority(group.getPriority());
    }
    builder.setTrackActions(
        group.getTracks().entrySet().stream()
            .collect(
                ImmutableSortedMap.toImmutableSortedMap(
                    Comparator.naturalOrder(),
                    trackEntry -> trackEntry.getKey().toString(),
                    trackEntry -> new DecrementPriority(trackEntry.getValue().getDecrement()))));
    return builder.build();
  }

  /** Helper to convert NXOS VS OSPF network type to VI model type. */
  private @Nullable org.batfish.datamodel.ospf.OspfNetworkType toOspfNetworkType(
      @Nullable OspfNetworkType type) {
    if (type == null) {
      return null;
    }
    switch (type) {
      case BROADCAST:
        return org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
      case POINT_TO_POINT:
        return org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
      default:
        _w.redFlag(
            String.format(
                "Conversion of Cisco NXOS OSPF network type '%s' is not handled.",
                type.toString()));
        return null;
    }
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(Interface iface) {
    String ifaceName = iface.getName();
    org.batfish.datamodel.Interface.Builder newIfaceBuilder =
        org.batfish.datamodel.Interface.builder()
            .setName(ifaceName)
            .setDeclaredNames(iface.getDeclaredNames());

    String parent = iface.getParentInterface();
    if (parent != null) {
      newIfaceBuilder.setDependencies(ImmutableSet.of(new Dependency(parent, DependencyType.BIND)));
    }

    // warn if non-switchport Ethernet without explicit (no) shutdown on Nexus 7000
    if (_platform == NexusPlatform.NEXUS_7000
        && iface.getType() == CiscoNxosInterfaceType.ETHERNET
        && iface.getSwitchportModeEffective(_systemDefaultSwitchport) == SwitchportMode.NONE
        && iface.getShutdown() == null) {
      _w.redFlag(
          String.format(
              "Non-switchport interface %s missing explicit (no) shutdown, so setting administratively active arbitrarily",
              ifaceName));
    }

    newIfaceBuilder.setActive(
        !iface.getShutdownEffective(
            _systemDefaultSwitchport,
            _systemDefaultSwitchportShutdown,
            _nonSwitchportDefaultShutdown));

    if (!iface.getIpAddressDhcp()) {
      Builder<ConcreteInterfaceAddress, ConnectedRouteMetadata> addressMetadata =
          ImmutableSortedMap.naturalOrder();
      InterfaceAddressWithAttributes addrWithAttr = iface.getAddress();
      if (addrWithAttr != null) {
        newIfaceBuilder.setAddress(addrWithAttr.getAddress());
        if (addrWithAttr.getAddress() instanceof ConcreteInterfaceAddress) {
          // convert any connected route metadata
          addressMetadata.put(
              (ConcreteInterfaceAddress) addrWithAttr.getAddress(),
              ConnectedRouteMetadata.builder()
                  .setAdmin(addrWithAttr.getRoutePreference())
                  .setGenerateLocalRoutes(true)
                  .setTag(addrWithAttr.getTag())
                  .build());
        }
      }
      newIfaceBuilder.setSecondaryAddresses(
          iface.getSecondaryAddresses().stream()
              .map(InterfaceAddressWithAttributes::getAddress)
              .collect(ImmutableSet.toImmutableSet()));
      iface.getSecondaryAddresses().stream()
          .filter(addr -> addr.getAddress() instanceof ConcreteInterfaceAddress)
          .forEach(
              addr ->
                  addressMetadata.put(
                      (ConcreteInterfaceAddress) addr.getAddress(),
                      ConnectedRouteMetadata.builder()
                          .setAdmin(addr.getRoutePreference())
                          .setGenerateLocalRoutes(true)
                          .setTag(addr.getTag())
                          .build()));
      newIfaceBuilder.setAddressMetadata(addressMetadata.build());
    }
    // TODO: handle DHCP

    newIfaceBuilder.setDescription(iface.getDescription());

    newIfaceBuilder.setDhcpRelayAddresses(iface.getDhcpRelayAddresses());

    newIfaceBuilder.setMtu(iface.getMtu());

    newIfaceBuilder.setProxyArp(firstNonNull(iface.getIpProxyArp(), Boolean.FALSE));

    // filters
    String ipAccessGroupIn = iface.getIpAccessGroupIn();
    if (ipAccessGroupIn != null) {
      newIfaceBuilder.setIncomingFilter(_c.getIpAccessLists().get(ipAccessGroupIn));
    }
    String ipAccessGroupOut = iface.getIpAccessGroupOut();
    if (ipAccessGroupOut != null) {
      newIfaceBuilder.setOutgoingFilter(_c.getIpAccessLists().get(ipAccessGroupOut));
    }

    // switchport+vlan settings
    SwitchportMode switchportMode = iface.getSwitchportModeEffective(_systemDefaultSwitchport);
    newIfaceBuilder.setSwitchport(switchportMode != SwitchportMode.NONE);
    newIfaceBuilder.setSwitchportMode(switchportMode.toSwitchportMode());
    switch (switchportMode) {
      case ACCESS:
        newIfaceBuilder.setAccessVlan(iface.getAccessVlan());
        break;

      case NONE:
        newIfaceBuilder.setEncapsulationVlan(iface.getEncapsulationVlan());
        break;

      case TRUNK:
        newIfaceBuilder.setAllowedVlans(iface.getAllowedVlans());
        newIfaceBuilder.setNativeVlan(iface.getNativeVlan());
        break;

      case DOT1Q_TUNNEL:
      case FEX_FABRIC:
      default:
        // unsupported
        break;
    }
    newIfaceBuilder.setVlan(iface.getVlan());
    newIfaceBuilder.setAutoState(iface.getAutostate());

    CiscoNxosInterfaceType type = iface.getType();
    newIfaceBuilder.setType(toInterfaceType(type, parent != null));

    Optional<InterfaceRuntimeData> runtimeData =
        Optional.ofNullable(_runtimeData.getInterface(ifaceName));
    Double runtimeBandwidth = runtimeData.map(InterfaceRuntimeData::getBandwidth).orElse(null);
    Double runtimeSpeed = runtimeData.map(InterfaceRuntimeData::getSpeed).orElse(null);

    Double speed;
    @Nullable Integer speedMbps = iface.getSpeedMbps();
    if (speedMbps != null) {
      speed = speedMbps * SPEED_CONVERSION_FACTOR;
      if (runtimeSpeed != null && !speed.equals(runtimeSpeed)) {
        _w.redFlag(
            String.format(
                "Interface %s:%s has configured speed %.0f bps but runtime data shows speed %.0f bps. Configured value will be used.",
                getHostname(), ifaceName, speed, runtimeSpeed));
      }
    } else if (runtimeSpeed != null) {
      speed = runtimeSpeed;
    } else {
      speed = getDefaultSpeed(type);
    }
    newIfaceBuilder.setSpeed(speed);
    Integer nxosBandwidth = iface.getBandwidth();
    Double finalBandwidth;
    if (nxosBandwidth != null) {
      finalBandwidth = nxosBandwidth * BANDWIDTH_CONVERSION_FACTOR;
      if (runtimeBandwidth != null && !finalBandwidth.equals(runtimeBandwidth)) {
        _w.redFlag(
            String.format(
                "Interface %s:%s has configured bandwidth %.0f bps but runtime data shows bandwidth %.0f bps. Configured value will be used.",
                getHostname(), ifaceName, finalBandwidth, runtimeBandwidth));
      }
    } else if (speedMbps != null) {
      // Prefer explicitly configured speed over runtime bandwidth
      finalBandwidth = speed;
    } else if (runtimeBandwidth != null) {
      finalBandwidth = runtimeBandwidth;
    } else if (speed != null) {
      finalBandwidth = speed;
    } else {
      finalBandwidth = getDefaultBandwidth(type);
    }
    newIfaceBuilder.setBandwidth(finalBandwidth);

    // port-channel members
    String portChannel = iface.getChannelGroup();
    if (portChannel != null) {
      newIfaceBuilder.setChannelGroup(portChannel);
      _portChannelMembers.put(portChannel, ifaceName);
    }

    // port-channels
    if (type == PORT_CHANNEL) {
      Collection<String> members = _portChannelMembers.get(ifaceName);
      newIfaceBuilder.setChannelGroupMembers(members);
      newIfaceBuilder.setDependencies(
          members.stream()
              .map(member -> new Dependency(member, DependencyType.AGGREGATE))
              .collect(ImmutableSet.toImmutableSet()));
    }

    if (iface.getHsrp() != null) {
      convertHsrp(iface.getHsrp(), newIfaceBuilder);
    }

    // PBR policy
    String pbrPolicy = iface.getPbrPolicy();
    // Do not convert undefined references
    if (pbrPolicy != null && _routeMaps.get(pbrPolicy) != null) {
      newIfaceBuilder.setRoutingPolicy(pbrPolicy);
    }

    org.batfish.datamodel.Interface newIface = newIfaceBuilder.build();

    String vrfName = firstNonNull(iface.getVrfMember(), DEFAULT_VRF_NAME);
    org.batfish.datamodel.Vrf vrf = _c.getVrfs().get(vrfName);
    if (vrf == null) {
      // Non-existent VRF set; disable and leave in default VRF
      newIface.setActive(false);
      vrf = _c.getVrfs().get(DEFAULT_VRF_NAME);
    } else if (_vrfs.get(vrfName).getShutdown()) {
      // VRF is shutdown; disable
      newIface.setActive(false);
    }
    newIface.setVrf(vrf);

    newIface.setOwner(_c);
    return newIface;
  }

  private @Nonnull InterfaceType toInterfaceType(
      CiscoNxosInterfaceType type, boolean subinterface) {
    switch (type) {
      case ETHERNET:
        return subinterface ? InterfaceType.LOGICAL : InterfaceType.PHYSICAL;
      case LOOPBACK:
        return InterfaceType.LOOPBACK;
      case MGMT:
        return InterfaceType.PHYSICAL;
      case PORT_CHANNEL:
        return subinterface ? InterfaceType.AGGREGATE_CHILD : InterfaceType.AGGREGATED;
      case VLAN:
        return InterfaceType.VLAN;
      default:
        return InterfaceType.UNKNOWN;
    }
  }

  private @Nonnull org.batfish.datamodel.IpAccessList toIpAccessList(IpAccessList list) {
    // TODO: handle and test top-level fragments behavior
    return org.batfish.datamodel.IpAccessList.builder()
        .setName(list.getName())
        .setSourceName(list.getName())
        .setSourceType(CiscoNxosStructureType.IP_ACCESS_LIST.getDescription())
        .setLines(
            list.getLines().values().stream()
                .flatMap(this::toIpAccessListLine)
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  /**
   * Converts the supplied {@code line} to zero or more vendor-independent {@link
   * org.batfish.datamodel.IpAccessListLine}s depending on semantics.
   */
  private @Nonnull Stream<org.batfish.datamodel.IpAccessListLine> toIpAccessListLine(
      IpAccessListLine line) {
    return line.accept(
        new IpAccessListLineVisitor<Stream<org.batfish.datamodel.IpAccessListLine>>() {
          @Override
          public Stream<org.batfish.datamodel.IpAccessListLine> visitActionIpAccessListLine(
              ActionIpAccessListLine actionIpAccessListLine) {
            LineAction action = actionIpAccessListLine.getAction();
            return Stream.of(
                org.batfish.datamodel.IpAccessListLine.builder()
                    .setAction(action)
                    .setMatchCondition(toAclLineMatchExpr(actionIpAccessListLine, action))
                    .setName(actionIpAccessListLine.getText())
                    .build());
          }

          @Override
          public Stream<org.batfish.datamodel.IpAccessListLine> visitRemarkIpAccessListLine(
              RemarkIpAccessListLine remarkIpAccessListLine) {
            return Stream.empty();
          }
        });
  }

  private @Nonnull AclLineMatchExpr toAclLineMatchExpr(
      ActionIpAccessListLine line, LineAction action) {
    /*
     * All rules:
     * - if 'fragments' present in rule
     *   - match only non-initial fragment
     * Also, for L3+L4 rules:
     * - permit rules
     *   - if L3-match and is non-initial-fragment
     *     - permit
     *   - if L3+L4 match and is initial fragment (or fragments not applicable to protocol)
     *     - permit
     * - deny rules
     *   - if L3+L4 match and is initial fragment (or fragments not applicable to protocol)
     *     - deny
     */
    // L3 match condition
    AclLineMatchExpr l3 = matchL3(line);

    // If L3 only, no special handling needed
    if (line.getL4Options() == null) {
      return l3;
    }

    // L4 handling
    AclLineMatchExpr l4 = matchL4(line);
    if (action == LineAction.PERMIT) {
      // permit if either non-initial fragment or l4 conditions match
      return and(l3, or(MATCH_NON_INITIAL_FRAGMENT_OFFSET, l4));
    }

    assert action == LineAction.DENY;
    // deny if initial fragment and l4 conditions match. else do nothing (no match).
    return and(l3, MATCH_INITIAL_FRAGMENT_OFFSET, l4);
  }

  private @Nonnull AclLineMatchExpr matchL3(ActionIpAccessListLine actionIpAccessListLine) {
    HeaderSpace.Builder hs = HeaderSpace.builder();
    if (actionIpAccessListLine.getProtocol() != null) {
      hs.setIpProtocols(ImmutableList.of(actionIpAccessListLine.getProtocol()));
    }
    hs.setSrcIps(toIpSpace(actionIpAccessListLine.getSrcAddressSpec()));
    hs.setDstIps(toIpSpace(actionIpAccessListLine.getDstAddressSpec()));
    Layer3Options l3Options = actionIpAccessListLine.getL3Options();
    if (l3Options.getDscp() != null) {
      hs.setDscps(ImmutableList.of(l3Options.getDscp()));
    }
    if (l3Options.getPacketLength() != null) {
      hs.setPacketLengths(l3Options.getPacketLength().getSubRanges());
    }
    if (l3Options.getPrecedence() != null) {
      // TODO: support precedence matching
      return AclLineMatchExprs.FALSE;
    }
    if (l3Options.getTtl() != null) {
      // TODO: support ttl matching
      return AclLineMatchExprs.FALSE;
    }
    AclLineMatchExpr matchL3ExceptFragmentOffset = match(hs.build());
    return actionIpAccessListLine.getFragments()
        ? and(MATCH_NON_INITIAL_FRAGMENT_OFFSET, matchL3ExceptFragmentOffset)
        : matchL3ExceptFragmentOffset;
  }

  private @Nonnull IpSpace toIpSpace(IpAddressSpec ipAddressSpec) {
    return ipAddressSpec.accept(
        new IpAddressSpecVisitor<IpSpace>() {

          @Override
          public IpSpace visitAddrGroupIpAddressSpec(
              AddrGroupIpAddressSpec addrGroupIpAddressSpec) {
            String name = addrGroupIpAddressSpec.getName();
            return _objectGroups.get(name) instanceof ObjectGroupIpAddress
                ? new IpSpaceReference(name)
                : EmptyIpSpace.INSTANCE;
          }

          @Override
          public IpSpace visitLiteralIpAddressSpec(LiteralIpAddressSpec literalIpAddressSpec) {
            return literalIpAddressSpec.getIpSpace();
          }
        });
  }

  private @Nonnull AclLineMatchExpr matchL4(ActionIpAccessListLine actionIpAccessListLine) {
    return actionIpAccessListLine
        .getL4Options()
        .accept(
            new Layer4OptionsVisitor<AclLineMatchExpr>() {
              @Override
              public AclLineMatchExpr visitIcmpOptions(IcmpOptions icmpOptions) {
                HeaderSpace.Builder hs =
                    HeaderSpace.builder()
                        .setIcmpTypes(ImmutableList.of(SubRange.singleton(icmpOptions.getType())));
                @Nullable Integer code = icmpOptions.getCode();
                if (code != null) {
                  hs.setIcmpCodes(ImmutableList.of(SubRange.singleton(code)));
                }
                return match(hs.build());
              }

              @Override
              public AclLineMatchExpr visitIgmpOptions(IgmpOptions igmpOptions) {
                // TODO: IGMP header field handling
                return AclLineMatchExprs.FALSE;
              }

              @Override
              public AclLineMatchExpr visitTcpOptions(TcpOptions tcpOptions) {
                ImmutableList.Builder<AclLineMatchExpr> conjuncts = ImmutableList.builder();
                HeaderSpace.Builder hs = HeaderSpace.builder();
                if (tcpOptions.getEstablished()) {
                  hs.setTcpFlags(
                      ImmutableList.of(
                          TcpFlagsMatchConditions.builder()
                              .setUseAck(true)
                              .setTcpFlags(TcpFlags.builder().setAck(true).build())
                              .build(),
                          TcpFlagsMatchConditions.builder()
                              .setUseRst(true)
                              .setTcpFlags(TcpFlags.builder().setRst(true).build())
                              .build()));
                }
                if (tcpOptions.getDstPortSpec() != null) {
                  conjuncts.add(
                      toPorts(tcpOptions.getDstPortSpec())
                          .map(AclLineMatchExprs::matchDstPort)
                          .orElse(AclLineMatchExprs.FALSE));
                }
                if (tcpOptions.getHttpMethod() != null) {
                  // TODO: support HTTP METHOD matching
                  return AclLineMatchExprs.FALSE;
                }
                if (tcpOptions.getSrcPortSpec() != null) {
                  conjuncts.add(
                      toPorts(tcpOptions.getSrcPortSpec())
                          .map(AclLineMatchExprs::matchSrcPort)
                          .orElse(AclLineMatchExprs.FALSE));
                }
                if (tcpOptions.getTcpFlags() != null) {
                  // TODO: validate logic
                  int tcpFlagsMask = firstNonNull(tcpOptions.getTcpFlagsMask(), 0);
                  hs.setTcpFlags(
                      ImmutableList.of(
                          toTcpFlagsMatchConditions(tcpOptions.getTcpFlags(), tcpFlagsMask)));
                }
                if (tcpOptions.getTcpOptionLength() != null) {
                  // TODO: support TCP option length matching
                  return AclLineMatchExprs.FALSE;
                }
                return and(conjuncts.add(match(hs.build())).build());
              }

              @Override
              public AclLineMatchExpr visitUdpOptions(UdpOptions udpOptions) {
                ImmutableList.Builder<AclLineMatchExpr> conjuncts = ImmutableList.builder();
                if (udpOptions.getDstPortSpec() != null) {
                  conjuncts.add(
                      toPorts(udpOptions.getDstPortSpec())
                          .map(AclLineMatchExprs::matchDstPort)
                          .orElse(AclLineMatchExprs.FALSE));
                }
                if (udpOptions.getSrcPortSpec() != null) {
                  conjuncts.add(
                      toPorts(udpOptions.getSrcPortSpec())
                          .map(AclLineMatchExprs::matchSrcPort)
                          .orElse(AclLineMatchExprs.FALSE));
                }
                return and(conjuncts.build());
              }
            });
  }

  private static @Nonnull TcpFlagsMatchConditions toTcpFlagsMatchConditions(
      TcpFlags tcpFlags, int tcpFlagsMask) {
    // NX-OS only supports lower 6 control bits
    // 0 in mask means use
    int chooseOnes = ~tcpFlagsMask & 0b111111;
    return TcpFlagsMatchConditions.builder()
        .setTcpFlags(tcpFlags)
        .setUseFin((chooseOnes & 0b000001) != 0)
        .setUseSyn((chooseOnes & 0b000010) != 0)
        .setUseRst((chooseOnes & 0b000100) != 0)
        .setUsePsh((chooseOnes & 0b001000) != 0)
        .setUseAck((chooseOnes & 0b010000) != 0)
        .setUseUrg((chooseOnes & 0b100000) != 0)
        .build();
  }

  private @Nonnull Ip6AccessList toIp6AccessList(Ipv6AccessList list) {
    // TODO: handle and test top-level fragments behavior
    // TODO: convert lines
    return new Ip6AccessList(list.getName());
  }

  /**
   * Convert a VS {@link DefaultVrfOspfProcess} to a VI {@link
   * org.batfish.datamodel.ospf.OspfProcess} in the default VRF.
   */
  private @Nonnull org.batfish.datamodel.ospf.OspfProcess toOspfProcess(
      DefaultVrfOspfProcess proc) {
    Ip routerId =
        proc.getRouterId() != null
            ? proc.getRouterId()
            : inferRouterId(_c.getDefaultVrf(), _w, "OSPF process " + proc.getName());
    return toOspfProcessBuilder(proc, proc.getName(), Configuration.DEFAULT_VRF_NAME)
        .setProcessId(proc.getName())
        .setRouterId(routerId)
        .build();
  }

  /**
   * Convert a VS {@link OspfVrf} to a VI {@link org.batfish.datamodel.ospf.OspfProcess} in a
   * non-default VRF using parent information from the containing VS {@link DefaultVrfOspfProcess}.
   */
  private @Nonnull org.batfish.datamodel.ospf.OspfProcess toOspfProcess(
      DefaultVrfOspfProcess proc, OspfVrf ospfVrf, org.batfish.datamodel.Vrf vrf) {
    String processName = proc.getName();
    Ip routerId =
        ospfVrf.getRouterId() != null
            ? ospfVrf.getRouterId()
            : proc.getRouterId() != null
                ? proc.getRouterId()
                : inferRouterId(vrf, _w, "OSPF process " + proc.getName());
    return toOspfProcessBuilder(ospfVrf, processName, ospfVrf.getVrf())
        .setProcessId(processName)
        .setRouterId(routerId)
        .build();
  }

  private @Nonnull org.batfish.datamodel.ospf.OspfProcess.Builder toOspfProcessBuilder(
      OspfProcess proc, String processName, String vrfName) {
    org.batfish.datamodel.ospf.OspfProcess.Builder builder =
        org.batfish.datamodel.ospf.OspfProcess.builder();

    // compute summaries to be used by all VI areas
    Map<Prefix, OspfAreaSummary> summaries =
        proc.getSummaryAddresses().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    summaryByPrefix -> toOspfAreaSummary(summaryByPrefix.getValue())));

    // convert areas
    Multimap<Long, IpWildcard> wildcardsByAreaId =
        ImmutableMultimap.copyOf(
            proc.getNetworks().entrySet().stream()
                .map(e -> Maps.immutableEntry(e.getValue(), e.getKey()))
                .collect(ImmutableList.toImmutableList()));
    Stream<OspfArea> implicitAreas =
        _implicitOspfAreas.get(Maps.immutableEntry(processName, vrfName)).stream()
            .filter(Predicates.not(proc.getAreas()::containsKey))
            .distinct()
            .map(OspfArea::new);
    builder
        .setAreas(
            Streams.concat(proc.getAreas().values().stream(), implicitAreas)
                .collect(
                    ImmutableSortedMap.toImmutableSortedMap(
                        Comparator.naturalOrder(),
                        OspfArea::getId,
                        area ->
                            toOspfArea(
                                processName,
                                vrfName,
                                proc,
                                area,
                                wildcardsByAreaId.get(area.getId()),
                                summaries))))
        .setReferenceBandwidth(
            OSPF_REFERENCE_BANDWIDTH_CONVERSION_FACTOR * proc.getAutoCostReferenceBandwidthMbps());

    // max-metric settings
    OspfMaxMetricRouterLsa maxMetricSettings = proc.getMaxMetricRouterLsa();
    if (maxMetricSettings != null) {
      builder.setMaxMetricTransitLinks((long) DEFAULT_OSPF_MAX_METRIC);
      Optional.ofNullable(maxMetricSettings.getExternalLsa())
          .map(Integer::longValue)
          .ifPresent(builder::setMaxMetricExternalNetworks);
      builder.setMaxMetricStubNetworks(
          maxMetricSettings.getIncludeStub() ? (long) DEFAULT_OSPF_MAX_METRIC : null);
      Optional.ofNullable(maxMetricSettings.getSummaryLsa())
          .map(Integer::longValue)
          .ifPresent(builder::setMaxMetricSummaryNetworks);
    }

    // export policy
    createOspfExportPolicy(proc, processName, vrfName, builder);

    return builder;
  }

  private @Nonnull OspfAreaSummary toOspfAreaSummary(OspfSummaryAddress ospfSummaryAddress) {
    return new OspfAreaSummary(!ospfSummaryAddress.getNotAdvertise(), null);
  }

  private void createOspfExportPolicy(
      OspfProcess proc,
      String processName,
      String vrfName,
      org.batfish.datamodel.ospf.OspfProcess.Builder builder) {
    ImmutableList.Builder<Statement> exportStatementsBuilder = ImmutableList.builder();
    ImmutableSortedSet.Builder<String> exportPolicySourcesBuilder =
        ImmutableSortedSet.naturalOrder();
    OspfDefaultOriginate defaultOriginate = proc.getDefaultOriginate();

    // First try redistributing static routes, which may include default route
    Optional.ofNullable(proc.getRedistributionPolicy(RoutingProtocolInstance.staticc()))
        .map(RedistributionPolicy::getRouteMap)
        .filter(_c.getRoutingPolicies()::containsKey)
        .ifPresent(
            routeMapName -> {
              exportPolicySourcesBuilder.add(routeMapName);
              exportStatementsBuilder.add(
                  new If(
                      new MatchProtocol(RoutingProtocol.STATIC),
                      ImmutableList.of(call(routeMapName))));
            });

    // Then try originating default route (either always or from RIB route not covered above)
    if (defaultOriginate != null) {
      BooleanExpr guard;
      if (defaultOriginate.getAlways()) {
        builder.setGeneratedRoutes(
            ImmutableSortedSet.of(GeneratedRoute.builder().setNetwork(Prefix.ZERO).build()));
        guard =
            new Conjunction(
                ImmutableList.<BooleanExpr>builder()
                    .add(matchDefaultRoute())
                    .add(new MatchProtocol(RoutingProtocol.AGGREGATE))
                    .build());
      } else {
        guard = matchDefaultRoute();
      }
      ImmutableList.Builder<Statement> defaultOriginateStatements = ImmutableList.builder();
      Optional.ofNullable(defaultOriginate.getRouteMap())
          .filter(_c.getRoutingPolicies()::containsKey)
          .ifPresent(
              defaultOriginateRouteMapName -> {
                exportPolicySourcesBuilder.add(defaultOriginateRouteMapName);
                defaultOriginateStatements.add(call(defaultOriginateRouteMapName));
              });
      defaultOriginateStatements.add(Statements.ExitAccept.toStaticStatement());
      exportStatementsBuilder.add(new If(guard, defaultOriginateStatements.build()));
    }
    // Then try remaining redistribution policies
    Optional.ofNullable(proc.getRedistributionPolicy(RoutingProtocolInstance.direct()))
        .map(RedistributionPolicy::getRouteMap)
        .filter(_c.getRoutingPolicies()::containsKey)
        .ifPresent(
            routeMapName -> {
              exportPolicySourcesBuilder.add(routeMapName);
              exportStatementsBuilder.add(
                  new If(
                      new MatchProtocol(RoutingProtocol.CONNECTED),
                      ImmutableList.of(call(routeMapName))));
            });
    List<Statement> exportInnerStatements = exportStatementsBuilder.build();
    int defaultRedistributionMetric =
        proc.getAreas().isEmpty()
            ? OspfArea.DEFAULT_DEFAULT_COST
            : proc.getAreas().values().iterator().next().getDefaultCost();
    if (!proc.getAreas().values().stream()
        .map(OspfArea::getDefaultCost)
        .allMatch(Predicates.equalTo(defaultRedistributionMetric))) {
      _w.unimplemented(
          String.format(
              "Unimplemented: OSPF process '%s': non-uniform default-cost across areas",
              processName));
      return;
    }
    if (exportInnerStatements.isEmpty()) {
      // nothing to export
      return;
    }
    String exportPolicyName = computeOspfExportPolicyName(processName, vrfName);
    RoutingPolicy exportPolicy =
        RoutingPolicy.builder()
            .setName(exportPolicyName)
            .setOwner(_c)
            .setStatements(
                ImmutableList.<Statement>builder()
                    .add(new SetOspfMetricType(OspfMetricType.E1))
                    .add(new SetMetric(new LiteralLong(defaultRedistributionMetric)))
                    .addAll(exportStatementsBuilder.build())
                    .add(Statements.ExitReject.toStaticStatement())
                    .build())
            .build();
    builder.setExportPolicy(exportPolicy);
    builder.setExportPolicySources(exportPolicySourcesBuilder.build());
    return;
  }

  private static @Nonnull String computeOspfExportPolicyName(String processName, String vrfName) {
    return String.format("~OSPF_EXPORT_POLICY~%s~%s~", processName, vrfName);
  }

  private @Nonnull org.batfish.datamodel.ospf.OspfArea toOspfArea(
      String processName,
      String vrfName,
      OspfProcess proc,
      OspfArea area,
      Collection<IpWildcard> wildcards,
      Map<Prefix, OspfAreaSummary> summaries) {
    org.batfish.datamodel.ospf.OspfArea.Builder builder =
        org.batfish.datamodel.ospf.OspfArea.builder().setNumber(area.getId());
    if (area.getTypeSettings() != null) {
      area.getTypeSettings()
          .accept(
              new OspfAreaTypeSettingsVisitor<Void>() {
                @Override
                public Void visitOspfAreaNssa(OspfAreaNssa ospfAreaNssa) {
                  builder.setNssa(toNssaSettings(ospfAreaNssa));
                  return null;
                }

                @Override
                public Void visitOspfAreaStub(OspfAreaStub ospfAreaStub) {
                  builder.setStub(toStubSettings(ospfAreaStub));
                  return null;
                }
              });
    }
    builder.setInterfaces(computeAreaInterfaces(processName, vrfName, proc, area, wildcards));
    builder.setSummaries(summaries);
    return builder.build();
  }

  private @Nonnull Set<String> computeAreaInterfaces(
      String processName,
      String vrfName,
      OspfProcess proc,
      OspfArea area,
      Collection<IpWildcard> wildcards) {
    org.batfish.datamodel.Vrf vrf = _c.getVrfs().get(vrfName);
    if (vrf == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<String> interfaces = ImmutableSet.builder();
    long areaId = area.getId();
    vrf.getInterfaces()
        .keySet()
        .forEach(
            ifaceName -> {
              Interface iface = _interfaces.get(ifaceName);
              OspfInterface ospf = iface.getOspf();
              if (ospf != null && ospf.getArea() != null) {
                // add to this area if interface is explictly configured to be in it.
                if (ospf.getArea().equals(areaId) && ospf.getProcess().equals(processName)) {
                  interfaces.add(ifaceName);
                  finalizeInterfaceOspfSettings(
                      ifaceName, areaId, processName, proc.getPassiveInterfaceDefault(), ospf);
                }
              } else {
                // Otherwise if OSPF area not explicitly configured on interface, add to this area
                // if interface IP is matched by wildcard.
                Optional<Ip> ipOpt =
                    Optional.ofNullable(iface.getAddress())
                        .map(InterfaceAddressWithAttributes::getAddress)
                        .filter(ConcreteInterfaceAddress.class::isInstance)
                        .map(ConcreteInterfaceAddress.class::cast)
                        .map(ConcreteInterfaceAddress::getIp);
                if (!ipOpt.isPresent()) {
                  return;
                }
                Ip ip = ipOpt.get();
                if (wildcards.stream().noneMatch(wildcard -> wildcard.containsIp(ip))) {
                  return;
                }
                interfaces.add(ifaceName);
                finalizeInterfaceOspfSettings(
                    ifaceName,
                    areaId,
                    processName,
                    proc.getPassiveInterfaceDefault(),
                    // If interface being added has no explicit OSPF configuration, use defaults
                    ospf != null ? ospf : new OspfInterface());
              }
            });
    return interfaces.build();
  }

  private void finalizeInterfaceOspfSettings(
      String ifaceName,
      long areaId,
      String processName,
      boolean passiveInterfaceDefault,
      OspfInterface ospf) {
    org.batfish.datamodel.Interface newIface = _c.getAllInterfaces().get(ifaceName);
    OspfInterfaceSettings.Builder ospfSettings = OspfInterfaceSettings.builder();
    ospfSettings.setCost(ospf.getCost());
    ospfSettings.setEnabled(true);
    ospfSettings.setAreaName(areaId);
    ospfSettings.setProcess(processName);
    ospfSettings.setPassive(
        ospf.getPassive() != null
            ? ospf.getPassive()
            : passiveInterfaceDefault || newIface.getName().startsWith("loopback"));
    org.batfish.datamodel.ospf.OspfNetworkType networkType = toOspfNetworkType(ospf.getNetwork());
    ospfSettings.setNetworkType(networkType);
    if (ospf.getCost() == null
        && newIface.isLoopback()
        && networkType != org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT) {
      ospfSettings.setCost(DEFAULT_LOOPBACK_OSPF_COST);
    } else {
      ospfSettings.setCost(ospf.getCost());
    }
    ospfSettings.setDeadInterval(toOspfDeadInterval(ospf));
    ospfSettings.setHelloInterval(toOspfHelloInterval(ospf));

    newIface.setOspfSettings(ospfSettings.build());
  }

  /**
   * Helper to infer dead interval from configured OSPF settings on an interface. Check explicitly
   * set dead interval, infer from hello interval, or use default, in that order. See
   * https://www.cisco.com/c/en/us/support/docs/ip/open-shortest-path-first-ospf/13689-17.html for
   * more details.
   */
  @VisibleForTesting
  static int toOspfDeadInterval(OspfInterface ospf) {
    Integer deadInterval = ospf.getDeadIntervalS();
    if (deadInterval != null) {
      return deadInterval;
    }
    Integer helloInterval = ospf.getHelloIntervalS();
    if (helloInterval != null) {
      return OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * helloInterval;
    }
    return DEFAULT_DEAD_INTERVAL_S;
  }

  /**
   * Helper to infer hello interval from configured OSPF settings on an interface. Check explicitly
   * set hello interval or use default, in that order. See
   * https://www.cisco.com/c/en/us/support/docs/ip/open-shortest-path-first-ospf/13689-17.html for
   * more details.
   */
  @VisibleForTesting
  static int toOspfHelloInterval(OspfInterface ospf) {
    Integer helloInterval = ospf.getHelloIntervalS();
    if (helloInterval != null) {
      return helloInterval;
    }
    return DEFAULT_HELLO_INTERVAL_S;
  }

  private @Nonnull NssaSettings toNssaSettings(OspfAreaNssa ospfAreaNssa) {
    return NssaSettings.builder()
        .setSuppressType3(ospfAreaNssa.getNoSummary())
        .setSuppressType7(ospfAreaNssa.getNoRedistribution())
        .build();
  }

  private @Nonnull StubSettings toStubSettings(OspfAreaStub ospfAreaStub) {
    return StubSettings.builder().setSuppressType3(ospfAreaStub.getNoSummary()).build();
  }

  /**
   * Return an {@link IntegerSpace} of allowed ports if {@code portSpec} is supported, or {@link
   * Optional#empty} if unsupported.
   */
  private @Nonnull Optional<IntegerSpace> toPorts(PortSpec portSpec) {
    // TODO: return an abstract space of integers to allow for named port spaces
    return portSpec.accept(
        new PortSpecVisitor<Optional<IntegerSpace>>() {
          @Override
          public Optional<IntegerSpace> visitLiteralPortSpec(LiteralPortSpec literalPortSpec) {
            return Optional.of(literalPortSpec.getPorts());
          }

          @Override
          public Optional<IntegerSpace> visitPortGroupPortSpec(
              PortGroupPortSpec portGroupPortSpec) {
            // TODO: support port groups
            return Optional.empty();
          }
        });
  }

  private static @Nonnull AsPathAccessList toAsPathAccessList(
      IpAsPathAccessList ipAsPathAccessList) {
    return new AsPathAccessList(
        ipAsPathAccessList.getName(),
        ipAsPathAccessList.getLines().values().stream()
            .map(CiscoNxosConfiguration::toAsPathAccessListLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull AsPathAccessListLine toAsPathAccessListLine(IpAsPathAccessListLine line) {
    return new AsPathAccessListLine(line.getAction(), toJavaRegex(line.getRegex()));
  }

  private void convertRouteMap(RouteMap routeMap) {
    /*
     * High-level overview:
     * - Group route-map entries into disjoint intervals, where each entry that is the target of a
     *   continue statement is the start of an interval.
     * - Generate a RoutingPolicy for each interval.
     * - Convert each entry into an If statement:
     *   - True branch of an entry with a continue statement calls the RoutingPolicy for the
     *     interval started by its target.
     *   - False branch of an entry at the end of an interval calls the RoutingPolicy for the next
     *     interval.
     */
    String routeMapName = routeMap.getName();

    // sequence -> next sequence if no match, or null if last sequence
    ImmutableMap.Builder<Integer, Integer> noMatchNextBySeqBuilder = ImmutableMap.builder();
    RouteMapEntry lastEntry = null;
    for (RouteMapEntry currentEntry : routeMap.getEntries().values()) {
      if (lastEntry != null) {
        int lastSequence = lastEntry.getSequence();
        noMatchNextBySeqBuilder.put(lastSequence, currentEntry.getSequence());
      }
      lastEntry = currentEntry;
    }

    // sequences that are valid targets of a continue statement
    Set<Integer> continueTargets =
        routeMap.getEntries().values().stream()
            .map(RouteMapEntry::getContinue)
            .filter(Objects::nonNull)
            .filter(routeMap.getEntries().keySet()::contains)
            .collect(ImmutableSet.toImmutableSet());

    // sequence -> next sequence if no match, or null if last sequence
    Map<Integer, Integer> noMatchNextBySeq = noMatchNextBySeqBuilder.build();

    /*
     * Initially:
     * - set the name of the generated routing policy for the route-map
     * - initialize the statement queue
     * For each entry in the route-map:
     * - If the current entry is the start of a new interval:
     *   - Build the RoutingPolicy for the previous interval.
     *   - Set the name of the new generated routing policy.
     *   - Clear the statement queue.
     * - After all entries have been processed:
     *   - Build the RoutingPolicy for the final interval.
     *     - If there were no continue statements, the final interval is the single policy for the
     *       whole route-map.
     */
    String currentRoutingPolicyName = routeMap.getName();
    ImmutableList.Builder<Statement> currentRoutingPolicyStatements = ImmutableList.builder();
    for (RouteMapEntry currentEntry : routeMap.getEntries().values()) {
      int currentSequence = currentEntry.getSequence();
      if (continueTargets.contains(currentSequence)) {
        // finalize the routing policy consisting of queued statements up to this point
        RoutingPolicy.builder()
            .setName(currentRoutingPolicyName)
            .setOwner(_c)
            .setStatements(currentRoutingPolicyStatements.build())
            .build();
        // reset statement queue
        currentRoutingPolicyStatements = ImmutableList.builder();
        // generate name for policy that will contain subsequent statements
        currentRoutingPolicyName = computeRoutingPolicyName(routeMapName, currentSequence);
      } // or else undefined reference
      currentRoutingPolicyStatements.add(
          toStatement(routeMapName, currentEntry, noMatchNextBySeq, continueTargets));
    }
    // finalize last routing policy
    // TODO: do default action, which changes when continuing from a permit
    currentRoutingPolicyStatements.add(ROUTE_MAP_DENY_STATEMENT);
    RoutingPolicy.builder()
        .setName(currentRoutingPolicyName)
        .setOwner(_c)
        .setStatements(currentRoutingPolicyStatements.build())
        .build();
  }

  public static @Nonnull String computeRouteMapEntryName(String routeMapName, int sequence) {
    return String.format("%s %d", routeMapName, sequence);
  }

  @VisibleForTesting
  public static @Nonnull String computeRoutingPolicyName(String routeMapName, int sequence) {
    return String.format("~%s~SEQ:%d~", routeMapName, sequence);
  }

  @Nonnull
  private PacketPolicy toPacketPolicy(RouteMap routeMap) {
    // TODO: handle continue statements
    return new PacketPolicy(
        routeMap.getName(),
        routeMap.getEntries().values().stream()
            .map(this::toPacketPolicyStatement)
            .collect(ImmutableList.toImmutableList()),
        // Default action is to fall through to the destination-based forwarding pipeline
        new Return(new FibLookup(IngressInterfaceVrf.instance())));
  }

  private @Nonnull Statement toStatement(
      String routeMapName,
      RouteMapEntry entry,
      Map<Integer, Integer> noMatchNextBySeq,
      Set<Integer> continueTargets) {
    ImmutableList.Builder<Statement> trueStatements = ImmutableList.builder();
    ImmutableList.Builder<BooleanExpr> conjuncts = ImmutableList.builder();

    // matches
    entry.getMatches().map(this::toBooleanExpr).forEach(conjuncts::add);

    // sets
    entry.getSets().flatMap(this::toStatements).forEach(trueStatements::add);

    Integer continueTarget = entry.getContinue();
    LineAction action = entry.getAction();
    Statement finalTrueStatement;

    // final action if matched
    if (continueTarget != null) {
      if (continueTargets.contains(continueTarget)) {
        // TODO: verify correct semantics: possibly, should add two statements in this case; first
        // should set default action to permit/deny if this is a permit/deny entry, and second
        // should call policy for next entry.
        finalTrueStatement = call(computeRoutingPolicyName(routeMapName, continueTarget));
      } else {
        // invalid continue target, so just deny
        // TODO: verify actual behavior
        finalTrueStatement = ROUTE_MAP_DENY_STATEMENT;
      }
    } else if (action == LineAction.PERMIT) {
      finalTrueStatement = ROUTE_MAP_PERMIT_STATEMENT;
    } else {
      assert action == LineAction.DENY;
      finalTrueStatement = ROUTE_MAP_DENY_STATEMENT;
    }
    trueStatements.add(finalTrueStatement);

    // final action if not matched
    Integer noMatchNext = noMatchNextBySeq.get(entry.getSequence());
    List<Statement> noMatchStatements =
        noMatchNext != null && continueTargets.contains(noMatchNext)
            ? ImmutableList.of(call(computeRoutingPolicyName(routeMapName, noMatchNext)))
            : ImmutableList.of();
    return new If(new Conjunction(conjuncts.build()), trueStatements.build(), noMatchStatements);
  }

  @Nonnull
  private org.batfish.datamodel.packet_policy.Statement toPacketPolicyStatement(
      RouteMapEntry entry) {
    // TODO: handle continue statement
    RouteMapMatchVisitor<BoolExpr> matchToBoolExpr =
        new RouteMapMatchVisitor<BoolExpr>() {

          @Override
          public BoolExpr visitRouteMapMatchAsPath(RouteMapMatchAsPath routeMapMatchAsPath) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchCommunity(
              RouteMapMatchCommunity routeMapMatchCommunity) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchInterface(
              RouteMapMatchInterface routeMapMatchInterface) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchIpAddress(
              RouteMapMatchIpAddress routeMapMatchIpAddress) {
            if (_ipAccessLists.containsKey(routeMapMatchIpAddress.getName())) {
              return new PacketMatchExpr(new PermittedByAcl(routeMapMatchIpAddress.getName()));
            } else {
              return FalseExpr.instance(); // fail-closed, match nothing
            }
          }

          @Override
          public BoolExpr visitRouteMapMatchIpAddressPrefixList(
              RouteMapMatchIpAddressPrefixList routeMapMatchIpAddressPrefixList) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchIpv6Address(
              RouteMapMatchIpv6Address routeMapMatchIpv6Address) {
            // incompatible with IPv4 forwarding, so fail-closed, match nothing
            return FalseExpr.instance();
          }

          @Override
          public BoolExpr visitRouteMapMatchIpv6AddressPrefixList(
              RouteMapMatchIpv6AddressPrefixList routeMapMatchIpv6AddressPrefixList) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchMetric(RouteMapMatchMetric routeMapMatchMetric) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchSourceProtocol(
              RouteMapMatchSourceProtocol routeMapMatchSourceProtocol) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchTag(RouteMapMatchTag routeMapMatchTag) {
            // TODO: somehow applicable to PBR? Documentation and semantics unclear
            _w.redFlag("'match tag' not supported in PBR policies");
            return null;
          }

          @Override
          public BoolExpr visitRouteMapMatchVlan(RouteMapMatchVlan routeMapMatchVlan) {
            // TODO: PBR implementation. Should match traffic coming in on any of specified VLANs.
            return null;
          }
        };
    List<BoolExpr> guardBoolExprs =
        entry
            .getMatches()
            .map(m -> m.accept(matchToBoolExpr))
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());

    if (guardBoolExprs.isEmpty()) {
      guardBoolExprs = ImmutableList.of(TrueExpr.instance()); // match anything
    }
    RouteMapSetVisitor<org.batfish.datamodel.packet_policy.Statement> setToStatement =
        new RouteMapSetVisitor<org.batfish.datamodel.packet_policy.Statement>() {
          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetAsPathPrependLastAs(
              RouteMapSetAsPathPrependLastAs routeMapSetAsPathPrependLastAs) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement
              visitRouteMapSetAsPathPrependLiteralAs(
                  RouteMapSetAsPathPrependLiteralAs routeMapSetAsPathPrependLiteralAs) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetCommListDelete(
              RouteMapSetCommListDelete routeMapSetCommListDelete) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetCommunity(
              RouteMapSetCommunity routeMapSetCommunity) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetIpNextHopLiteral(
              RouteMapSetIpNextHopLiteral routeMapSetIpNextHopLiteral) {
            // TODO: handle "load-share" modifier
            return new Return(
                FibLookupOverrideLookupIp.builder()
                    .setIps(routeMapSetIpNextHopLiteral.getNextHops())
                    // VRF to lookup in
                    .setVrfExpr(IngressInterfaceVrf.instance())
                    // Default action in case none of the next hops can be resolved
                    .setDefaultAction(new FibLookup(IngressInterfaceVrf.instance()))
                    .setRequireConnected(true)
                    .build());
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetIpNextHopUnchanged(
              RouteMapSetIpNextHopUnchanged routeMapSetIpNextHopUnchanged) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetLocalPreference(
              RouteMapSetLocalPreference routeMapSetLocalPreference) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetMetric(
              RouteMapSetMetric routeMapSetMetric) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetMetricType(
              RouteMapSetMetricType routeMapSetMetricType) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetOrigin(
              RouteMapSetOrigin routeMapSetOrigin) {
            // Not applicable to PBR
            return null;
          }

          @Override
          public org.batfish.datamodel.packet_policy.Statement visitRouteMapSetTag(
              RouteMapSetTag routeMapSetTag) {
            // Not applicable to PBR
            return null;
          }
        };
    List<org.batfish.datamodel.packet_policy.Statement> trueStatements =
        entry
            .getSets()
            .map(s -> s.accept(setToStatement))
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    if (trueStatements.size() > 1) {
      _w.redFlag(
          "Multiple set statements are not allowed in a single route map statement. Choosing the first one");
      trueStatements = ImmutableList.of(trueStatements.get(0));
    }
    if (guardBoolExprs.size() > 1) {
      _w.redFlag(
          "Multiple match conditions are not allowed in a single route map statement. Choosing the first one");
    }
    return new org.batfish.datamodel.packet_policy.If(guardBoolExprs.get(0), trueStatements);
  }

  private @Nonnull BooleanExpr toBooleanExpr(RouteMapMatch match) {
    return match.accept(
        new RouteMapMatchVisitor<BooleanExpr>() {

          @Override
          public BooleanExpr visitRouteMapMatchAsPath(RouteMapMatchAsPath routeMapMatchAsPath) {
            // TODO: test behavior for undefined reference
            return new Disjunction(
                routeMapMatchAsPath.getNames().stream()
                    .filter(_ipAsPathAccessLists::containsKey)
                    .map(name -> new MatchAsPath(new NamedAsPathSet(name)))
                    .collect(ImmutableList.toImmutableList()));
          }

          @Override
          public BooleanExpr visitRouteMapMatchCommunity(
              RouteMapMatchCommunity routeMapMatchCommunity) {
            // TODO: test behavior for undefined reference
            return new Disjunction(
                routeMapMatchCommunity.getNames().stream()
                    .filter(_c.getCommunitySetMatchExprs()::containsKey)
                    .map(
                        name ->
                            new MatchCommunities(
                                InputCommunities.instance(),
                                new CommunitySetMatchExprReference(name)))
                    .collect(ImmutableList.toImmutableList()));
          }

          @Override
          public BooleanExpr visitRouteMapMatchInterface(
              RouteMapMatchInterface routeMapMatchInterface) {
            // TODO: ignore shutdown interfaces?
            // TODO: ignore blacklisted interfaces?
            // TODO: HSRP addresses? Only if elected?
            return new Disjunction(
                new MatchPrefixSet(
                    DestinationNetwork.instance(),
                    new ExplicitPrefixSet(
                        new PrefixSpace(
                            routeMapMatchInterface.getNames().stream()
                                .map(_interfaces::get)
                                .filter(Objects::nonNull)
                                .flatMap(
                                    iface ->
                                        Stream.concat(
                                            Stream.of(iface.getAddress()),
                                            iface.getSecondaryAddresses().stream()))
                                .filter(Objects::nonNull)
                                .map(InterfaceAddressWithAttributes::getAddress)
                                .filter(ConcreteInterfaceAddress.class::isInstance)
                                .map(ConcreteInterfaceAddress.class::cast)
                                .flatMap(
                                    address ->
                                        address.getPrefix().getPrefixLength() <= 30
                                            ? Stream.of(
                                                address.getPrefix(), address.getIp().toPrefix())
                                            : Stream.of(address.getPrefix()))
                                .map(PrefixRange::fromPrefix)
                                .collect(ImmutableList.toImmutableList())))));
          }

          @Override
          public BooleanExpr visitRouteMapMatchIpAddress(
              RouteMapMatchIpAddress routeMapMatchIpAddress) {
            // Ignore, as it only applies to PBR and has no effect on route filtering/redistribution
            return BooleanExprs.TRUE;
          }

          @Override
          public BooleanExpr visitRouteMapMatchIpAddressPrefixList(
              RouteMapMatchIpAddressPrefixList routeMapMatchIpAddressPrefixList) {
            return new Disjunction(
                routeMapMatchIpAddressPrefixList.getNames().stream()
                    .filter(_ipPrefixLists::containsKey)
                    .map(
                        name ->
                            new MatchPrefixSet(
                                DestinationNetwork.instance(), new NamedPrefixSet(name)))
                    .collect(ImmutableList.toImmutableList()));
          }

          @Override
          public BooleanExpr visitRouteMapMatchIpv6Address(
              RouteMapMatchIpv6Address routeMapMatchIpv6Address) {
            // Ignore, as it only applies to PBR and has no effect on route filtering/redistribution
            return BooleanExprs.TRUE;
          }

          @Override
          public BooleanExpr visitRouteMapMatchIpv6AddressPrefixList(
              RouteMapMatchIpv6AddressPrefixList routeMapMatchIpv6AddressPrefixList) {
            // incompatible with IPv4 routing, so fail closed, match nothing.
            return BooleanExprs.FALSE;
          }

          @Override
          public BooleanExpr visitRouteMapMatchMetric(RouteMapMatchMetric routeMapMatchMetric) {
            return new MatchMetric(
                IntComparator.EQ, new LiteralLong(routeMapMatchMetric.getMetric()));
          }

          @Override
          public BooleanExpr visitRouteMapMatchSourceProtocol(
              RouteMapMatchSourceProtocol routeMapMatchSourceProtocol) {
            return routeMapMatchSourceProtocol
                .toRoutingProtocols()
                .<BooleanExpr>map(MatchProtocol::new)
                .orElse(BooleanExprs.FALSE);
          }

          @Override
          public BooleanExpr visitRouteMapMatchTag(RouteMapMatchTag routeMapMatchTag) {
            List<BooleanExpr> matchTags =
                routeMapMatchTag.getTags().stream()
                    .map(LiteralLong::new)
                    .map(ll -> new MatchTag(IntComparator.EQ, ll))
                    .collect(Collectors.toList());
            return new Disjunction(matchTags);
          }

          @Override
          public BooleanExpr visitRouteMapMatchVlan(RouteMapMatchVlan routeMapMatchVlan) {
            return visitRouteMapMatchInterface(
                new RouteMapMatchInterface(
                    routeMapMatchVlan.getVlans().stream()
                        .map(vlan -> String.format("Vlan%d", vlan))
                        .collect(ImmutableSet.toImmutableSet())));
          }
        });
  }

  private @Nonnull Stream<Statement> toStatements(RouteMapSet routeMapSet) {
    return routeMapSet.accept(
        new RouteMapSetVisitor<Stream<Statement>>() {
          @Override
          public Stream<Statement> visitRouteMapSetAsPathPrependLastAs(
              RouteMapSetAsPathPrependLastAs routeMapSetAsPathPrependLastAs) {
            return Stream.of(
                new PrependAsPath(
                    new MultipliedAs(
                        AutoAs.instance(),
                        new LiteralInt(routeMapSetAsPathPrependLastAs.getNumPrepends()))));
          }

          @Override
          public Stream<Statement> visitRouteMapSetAsPathPrependLiteralAs(
              RouteMapSetAsPathPrependLiteralAs routeMapSetAsPathPrependLiteralAs) {
            return Stream.of(
                new PrependAsPath(
                    new LiteralAsList(
                        routeMapSetAsPathPrependLiteralAs.getAsNumbers().stream()
                            .map(ExplicitAs::new)
                            .collect(ImmutableList.toImmutableList()))));
          }

          @Override
          public Stream<Statement> visitRouteMapSetCommListDelete(
              RouteMapSetCommListDelete routeMapSetCommListDelete) {
            String name = routeMapSetCommListDelete.getName();
            if (!_c.getCommunityMatchExprs().containsKey(name)) {
              return Stream.of();
            }
            return Stream.of(
                new SetCommunities(
                    new CommunitySetDifference(
                        InputCommunities.instance(), new CommunityMatchExprReference(name))));
          }

          @Override
          public Stream<Statement> visitRouteMapSetCommunity(
              RouteMapSetCommunity routeMapSetCommunity) {
            CommunitySetExpr communities =
                new LiteralCommunitySet(CommunitySet.of(routeMapSetCommunity.getCommunities()));
            return Stream.of(
                new SetCommunities(
                    routeMapSetCommunity.getAdditive()
                        ? CommunitySetUnion.of(InputCommunities.instance(), communities)
                        : communities));
          }

          @Override
          public Stream<Statement> visitRouteMapSetIpNextHopLiteral(
              RouteMapSetIpNextHopLiteral routeMapSetIpNextHopLiteral) {
            List<Ip> nextHopIps = routeMapSetIpNextHopLiteral.getNextHops();
            if (nextHopIps.size() > 1) {
              // Applicable to PBR only (not routing)
              return Stream.empty();
            }
            assert !nextHopIps.isEmpty();
            return Stream.of(new SetNextHop(new IpNextHop(nextHopIps)));
          }

          @Override
          public Stream<Statement> visitRouteMapSetIpNextHopUnchanged(
              RouteMapSetIpNextHopUnchanged routeMapSetIpNextHopUnchanged) {
            return Stream.of(new SetNextHop(UnchangedNextHop.getInstance()));
          }

          @Override
          public Stream<Statement> visitRouteMapSetLocalPreference(
              RouteMapSetLocalPreference routeMapSetLocalPreference) {
            return Stream.of(
                new SetLocalPreference(
                    new LiteralLong(routeMapSetLocalPreference.getLocalPreference())));
          }

          @Override
          public Stream<Statement> visitRouteMapSetMetric(RouteMapSetMetric routeMapSetMetric) {
            return Stream.of(new SetMetric(new LiteralLong(routeMapSetMetric.getMetric())));
          }

          @Override
          public Stream<Statement> visitRouteMapSetMetricType(
              RouteMapSetMetricType routeMapSetMetricType) {
            switch (routeMapSetMetricType.getMetricType()) {
              case EXTERNAL:
                return Stream.of(new SetIsisMetricType(IsisMetricType.EXTERNAL));

              case INTERNAL:
                return Stream.of(new SetIsisMetricType(IsisMetricType.INTERNAL));

              case TYPE_1:
                return Stream.of(new SetOspfMetricType(OspfMetricType.E1));

              case TYPE_2:
                return Stream.of(new SetOspfMetricType(OspfMetricType.E2));

              default:
                // should not happen
                return Stream.empty();
            }
          }

          @Override
          public Stream<Statement> visitRouteMapSetOrigin(RouteMapSetOrigin routeMapSetOrigin) {
            return Stream.of(new SetOrigin(new LiteralOrigin(routeMapSetOrigin.getOrigin(), null)));
          }

          @Override
          public Stream<Statement> visitRouteMapSetTag(RouteMapSetTag routeMapSetTag) {
            return Stream.of(new SetTag(new LiteralLong(routeMapSetTag.getTag())));
          }
        });
  }

  /**
   * Converts the supplied {@code staticRoute} to a a vendor-independent {@link
   * org.batfish.datamodel.StaticRoute} if all options are supported and static route contains no
   * undefined references. Otherwise, returns {@code null}.
   */
  private @Nullable org.batfish.datamodel.StaticRoute toStaticRoute(StaticRoute staticRoute) {
    // TODO: VI and VS support for lookup of next-hop-ip in a different VRF
    if (staticRoute.getNextHopVrf() != null) {
      return null;
    }
    // TODO: support track object number
    String nextHopInterface = staticRoute.getNextHopInterface();
    String newNextHopInterface;
    if (nextHopInterface != null) {
      if (!_interfaces.containsKey(nextHopInterface)) {
        // undefined reference
        return null;
      }
      newNextHopInterface = nextHopInterface;
    } else if (staticRoute.getDiscard()) {
      newNextHopInterface = NULL_INTERFACE_NAME;
    } else {
      newNextHopInterface = UNSET_NEXT_HOP_INTERFACE;
    }
    return org.batfish.datamodel.StaticRoute.builder()
        .setAdministrativeCost(staticRoute.getPreference())
        .setMetric(0L)
        .setNetwork(staticRoute.getPrefix())
        .setNextHopInterface(newNextHopInterface)
        .setNextHopIp(firstNonNull(staticRoute.getNextHopIp(), UNSET_ROUTE_NEXT_HOP_IP))
        .setTag(staticRoute.getTag())
        .build();
  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    _c = new Configuration(_hostname, ConfigurationFormat.CISCO_NX);
    _c.getVendorFamily().setCiscoNxos(createCiscoNxosFamily());
    _c.setDefaultInboundAction(LineAction.PERMIT);
    _c.setDefaultCrossZoneAction(LineAction.PERMIT);

    convertDomainName();
    convertObjectGroups();
    convertIpAccessLists();
    convertIpv6AccessLists();
    convertIpAsPathAccessLists();
    convertIpPrefixLists();
    convertIpv6PrefixLists();
    convertIpCommunityLists();
    convertVrfs();
    convertInterfaces();
    disableUnregisteredVlanInterfaces();
    convertIpNameServers();
    convertLoggingServers();
    convertLoggingSourceInterface();
    convertNtpServers();
    convertNtpSourceInterface();
    convertSnmpServers();
    convertSnmpSourceInterface();
    convertTacacsServers();
    convertTacacsSourceInterface();
    convertRouteMaps();
    convertStaticRoutes();
    computeImplicitOspfAreas();
    convertOspfProcesses();
    convertNves();
    convertBgp();
    convertEigrp();

    markStructures();
    return _c;
  }

  private void computeImplicitOspfAreas() {
    ImmutableMultimap.Builder<Entry<String, String>, Long> builder = ImmutableMultimap.builder();
    _interfaces
        .values()
        .forEach(
            iface -> {
              String vrf = firstNonNull(iface.getVrfMember(), Configuration.DEFAULT_VRF_NAME);
              OspfInterface ospf = iface.getOspf();
              if (ospf == null) {
                return;
              }
              String process = ospf.getProcess();
              if (process == null) {
                return;
              }
              builder.put(Maps.immutableEntry(process, vrf), ospf.getArea());
            });
    _implicitOspfAreas = builder.build();
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations()
      throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  private @Nonnull org.batfish.datamodel.Vrf toVrf(Vrf vrf) {
    org.batfish.datamodel.Vrf.Builder newVrfBuilder =
        org.batfish.datamodel.Vrf.builder().setName(vrf.getName());
    return newVrfBuilder.build();
  }
}
