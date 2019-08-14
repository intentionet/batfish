package org.batfish.representation.cisco_nxos;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.batfish.datamodel.BumTransportMethod.MULTICAST_GROUP;
import static org.batfish.datamodel.BumTransportMethod.UNICAST_FLOOD_GROUP;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpCommonExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpGenerationPolicyName;
import static org.batfish.representation.cisco.CiscoConversions.generateGenerationPolicy;
import static org.batfish.representation.cisco.CiscoConversions.suppressSummarizedPrefixes;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.PORT_CHANNEL;
import static org.batfish.representation.cisco_nxos.Interface.BANDWIDTH_CONVERSION_FACTOR;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultBandwidth;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultSpeed;
import static org.batfish.representation.cisco_nxos.OspfMaxMetricRouterLsa.DEFAULT_OSPF_MAX_METRIC;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
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
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.isis.IsisMetricType;
import org.batfish.datamodel.ospf.NssaSettings;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.StubSettings;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.AutoAs;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
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
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchAsPath;
import org.batfish.datamodel.routing_policy.expr.MatchEntireCommunitySet;
import org.batfish.datamodel.routing_policy.expr.MatchMetric;
import org.batfish.datamodel.routing_policy.expr.MatchPrefix6Set;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.MatchTag;
import org.batfish.datamodel.routing_policy.expr.MultipliedAs;
import org.batfish.datamodel.routing_policy.expr.NamedAsPathSet;
import org.batfish.datamodel.routing_policy.expr.NamedCommunitySet;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.AddCommunity;
import org.batfish.datamodel.routing_policy.statement.CallStatement;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.PrependAsPath;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
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

  public static final String NULL_VRF_NAME = "~NULL_VRF~";
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

  private static @Nonnull RouteFilterList toRouteFilterList(IpPrefixList ipPrefixList) {
    String name = ipPrefixList.getName();
    RouteFilterList rfl = new RouteFilterList(name);
    rfl.setLines(
        ipPrefixList.getLines().values().stream()
            .map(CiscoNxosConfiguration::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList()));
    return rfl;
  }

  private transient Configuration _c;
  private transient Multimap<Entry<String, String>, Long> _implicitOspfAreas;

  private @Nullable String _bannerExec;
  private @Nullable String _bannerMotd;
  private final @Nonnull BgpGlobalConfiguration _bgpGlobalConfiguration;
  private final @Nonnull Vrf _defaultVrf;
  private @Nullable Evpn _evpn;
  private @Nullable String _hostname;
  private final @Nonnull Map<String, Interface> _interfaces;
  private final @Nonnull Map<String, IpAccessList> _ipAccessLists;
  private final @Nonnull Map<String, IpAsPathAccessList> _ipAsPathAccessLists;
  private final @Nonnull Map<String, IpCommunityList> _ipCommunityLists;
  private final @Nonnull Map<String, IpPrefixList> _ipPrefixLists;
  private final @Nonnull Map<Integer, Nve> _nves;
  private final @Nonnull Map<String, ObjectGroup> _objectGroups;
  private final @Nonnull Map<String, DefaultVrfOspfProcess> _ospfProcesses;
  private transient Multimap<String, String> _portChannelMembers;
  private @Nonnull IntegerSpace _reservedVlanRange;
  private final @Nonnull Map<String, RouteMap> _routeMaps;
  private @Nullable String _version;
  private final @Nonnull Map<Integer, Vlan> _vlans;
  private final @Nonnull Map<String, Vrf> _vrfs;

  public CiscoNxosConfiguration() {
    _bgpGlobalConfiguration = new BgpGlobalConfiguration();
    _defaultVrf = new Vrf(DEFAULT_VRF_NAME);
    _interfaces = new HashMap<>();
    _ipAccessLists = new HashMap<>();
    _ipAsPathAccessLists = new HashMap<>();
    _ipCommunityLists = new HashMap<>();
    _ipPrefixLists = new HashMap<>();
    _nves = new HashMap<>();
    _objectGroups = new HashMap<>();
    _ospfProcesses = new HashMap<>();
    _reservedVlanRange = DEFAULT_RESERVED_VLAN_RANGE;
    _routeMaps = new HashMap<>();
    _vlans = new HashMap<>();
    _vrfs = new HashMap<>();
  }

  public void defineStructure(CiscoNxosStructureType type, String name, ParserRuleContext ctx) {
    for (int i = ctx.getStart().getLine(); i <= ctx.getStop().getLine(); ++i) {
      defineStructure(type, name, i);
    }
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

  private static @Nonnull Ip computeDefaultRouterId(final Configuration c) {
    // Algorithm:
    // https://www.cisco.com/c/en/us/td/docs/switches/datacenter/sw/nx-os/tech_note/cisco_nxos_ios_ospf_comparison.html
    Optional<Ip> address =
        Optional.ofNullable(c.getAllInterfaces().get("loopback0"))
            .map(org.batfish.datamodel.Interface::getConcreteAddress)
            .map(ConcreteInterfaceAddress::getIp);
    if (address.isPresent()) {
      return address.get();
    }
    address =
        c.getAllInterfaces().keySet().stream()
            .filter(name -> name.startsWith("loopback"))
            .sorted()
            .map(c.getAllInterfaces()::get)
            .map(org.batfish.datamodel.Interface::getConcreteAddress)
            .filter(Objects::nonNull)
            .map(ConcreteInterfaceAddress::getIp)
            .findFirst();
    if (address.isPresent()) {
      return address.get();
    }
    address =
        c.getAllInterfaces().keySet().stream()
            .sorted()
            .map(c.getAllInterfaces()::get)
            .map(org.batfish.datamodel.Interface::getConcreteAddress)
            .filter(Objects::nonNull)
            .map(ConcreteInterfaceAddress::getIp)
            .findFirst();
    return address.orElse(Ip.ZERO);
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
      for (Entry<Prefix, BgpVrfAddressFamilyAggregateNetworkConfiguration> e :
          ipv4af.getAggregateNetworks().entrySet()) {
        Prefix prefix = e.getKey();
        BgpVrfAddressFamilyAggregateNetworkConfiguration agg = e.getValue();
        generateGenerationPolicy(c, vrfName, prefix);

        GeneratedRoute.Builder gr =
            GeneratedRoute.builder()
                .setNetwork(prefix)
                .setAdmin(AGGREGATE_ROUTE_ADMIN_COST)
                .setGenerationPolicy(
                    computeBgpGenerationPolicyName(true, vrfName, prefix.toString()))
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
    BgpRedistributionPolicy ripPolicy =
        ipv4af == null ? null : ipv4af.getRedistributionPolicy(RoutingProtocol.RIP);
    if (ripPolicy != null) {
      String routeMap = ripPolicy.getRouteMap();
      org.batfish.representation.cisco_nxos.RouteMap map = _routeMaps.get(routeMap);
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
    BgpRedistributionPolicy staticPolicy =
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
    BgpRedistributionPolicy connectedPolicy =
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
    BgpRedistributionPolicy ospfPolicy =
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
        hsrp.getGroups().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey, hsrpGroupEntry -> toHsrpGroup(hsrpGroupEntry.getValue()))));
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

  private void convertIpAsPathAccessLists() {
    _ipAsPathAccessLists.forEach(
        (name, ipAsPathAccessList) ->
            _c.getAsPathAccessLists().put(name, toAsPathAccessList(ipAsPathAccessList)));
  }

  private void convertIpCommunityLists() {
    _ipCommunityLists.forEach(
        (name, list) ->
            _c.getCommunityLists()
                .put(
                    name,
                    list.accept(
                        new IpCommunityListVisitor<CommunityList>() {
                          @Override
                          public CommunityList visitIpCommunityListStandard(
                              IpCommunityListStandard ipCommunityListStandard) {
                            return toCommunityList(ipCommunityListStandard);
                          }
                        })));
  }

  private static @Nonnull CommunityList toCommunityList(IpCommunityListStandard list) {
    return new CommunityList(
        list.getName(),
        list.getLines().values().stream()
            .map(CiscoNxosConfiguration::toCommunityListLine)
            .collect(ImmutableList.toImmutableList()),
        false);
  }

  private static @Nonnull CommunityListLine toCommunityListLine(IpCommunityListStandardLine line) {
    return new CommunityListLine(line.getAction(), toCommunitySetExpr(line.getCommunities()));
  }

  private static @Nonnull CommunitySetExpr toCommunitySetExpr(Set<StandardCommunity> communities) {
    return new LiteralCommunityConjunction(communities);
  }

  private void convertIpPrefixLists() {
    _ipPrefixLists.forEach(
        (name, ipPrefixList) ->
            _c.getRouteFilterLists().put(name, toRouteFilterList(ipPrefixList)));
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
                        vrf.addOspfProcess(toOspfProcess(proc, ospfVrf));
                      });
            });
  }

  private void convertRouteMaps() {
    _routeMaps.forEach(
        (name, routeMap) -> _c.getRoutingPolicies().put(name, toRoutingPolicy(routeMap)));
  }

  private void convertStaticRoutes() {
    Stream.concat(Stream.of(_defaultVrf), _vrfs.values().stream())
        .forEach(this::convertStaticRoutes);
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
    _c.getVrfs().put(DEFAULT_VRF_NAME, new org.batfish.datamodel.Vrf(DEFAULT_VRF_NAME));
    _c.getVrfs().put(NULL_VRF_NAME, new org.batfish.datamodel.Vrf(NULL_VRF_NAME));
    _vrfs.forEach((name, vrf) -> _c.getVrfs().put(name, toVrf(vrf)));
  }

  private void convertNves() {
    _nves
        .values()
        .forEach(nve -> nve.getMemberVnis().values().forEach(vni -> convertNveVni(nve, vni)));
  }

  private void convertNveVni(@Nonnull Nve nve, @Nonnull NveVni nveVni) {
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
    VniSettings vniSettings =
        VniSettings.builder()
            .setBumTransportIps(bumTransportIps)
            .setBumTransportMethod(bumTransportMethod)
            .setSourceAddress(
                nve.getSourceInterface() != null
                    ? getInterfaceIp(_c.getAllInterfaces(), nve.getSourceInterface())
                    : null)
            .setUdpPort(VniSettings.DEFAULT_UDP_PORT)
            .setVni(nveVni.getVni())
            .setVlan(vlan)
            .build();
    _c.getDefaultVrf().getVniSettings().put(vniSettings.getVni(), vniSettings);
  }

  @Nonnull
  private static BumTransportMethod getBumTransportMethod(
      @Nonnull NveVni nveVni, @Nonnull Nve nve) {
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
  private static Ip getMultiCastGroupIp(@Nonnull NveVni nveVni, @Nonnull Nve nve) {
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
      @Nonnull Map<String, org.batfish.datamodel.Interface> interfaces, @Nonnull String ifaceName) {
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
  private Integer getVlanForVni(@Nonnull Integer vni) {
    return _vlans.values().stream()
        .filter(vlan -> vni.equals(vlan.getVni()))
        .findFirst()
        .map(Vlan::getId)
        .orElse(null);
  }

  private @Nonnull CiscoNxosFamily createCiscoNxosFamily() {
    return CiscoNxosFamily.builder().build();
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

  public @Nonnull Vrf getDefaultVrf() {
    return _defaultVrf;
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

  public @Nonnull Map<String, IpPrefixList> getIpPrefixLists() {
    return _ipPrefixLists;
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

  /** Range of VLAN IDs reserved by the system and therefore unassignable. */
  public @Nonnull IntegerSpace getReservedVlanRange() {
    return _reservedVlanRange;
  }

  public @Nonnull Map<String, RouteMap> getRouteMaps() {
    return _routeMaps;
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

  public @Nonnull Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  private void markStructures() {
    markConcreteStructure(
        CiscoNxosStructureType.INTERFACE,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_UPDATE_SOURCE,
        CiscoNxosStructureUsage.INTERFACE_SELF_REFERENCE,
        CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_INTERFACE,
        CiscoNxosStructureUsage.NVE_SOURCE_INTERFACE);
    markConcreteStructure(
        CiscoNxosStructureType.IP_AS_PATH_ACCESS_LIST,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_FILTER_LIST_IN,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_FILTER_LIST_OUT,
        CiscoNxosStructureUsage.BGP_NEIGHBOR6_FILTER_LIST_IN,
        CiscoNxosStructureUsage.BGP_NEIGHBOR6_FILTER_LIST_OUT);
    markConcreteStructure(
        CiscoNxosStructureType.IP_PREFIX_LIST,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_PREFIX_LIST_IN,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_PREFIX_LIST_OUT,
        CiscoNxosStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS_PREFIX_LIST);
    markConcreteStructure(
        CiscoNxosStructureType.IPV6_PREFIX_LIST,
        CiscoNxosStructureUsage.BGP_NEIGHBOR6_PREFIX_LIST_IN,
        CiscoNxosStructureUsage.BGP_NEIGHBOR6_PREFIX_LIST_OUT);
    markConcreteStructure(CiscoNxosStructureType.NVE, CiscoNxosStructureUsage.NVE_SELF_REFERENCE);
    markConcreteStructure(
        CiscoNxosStructureType.OBJECT_GROUP_IP_ADDRESS,
        CiscoNxosStructureUsage.IP_ACCESS_LIST_DESTINATION_ADDRGROUP,
        CiscoNxosStructureUsage.IP_ACCESS_LIST_SOURCE_ADDRGROUP);
    markConcreteStructure(
        CiscoNxosStructureType.PORT_CHANNEL, CiscoNxosStructureUsage.INTERFACE_CHANNEL_GROUP);
    markConcreteStructure(
        CiscoNxosStructureType.ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_ADDITIONAL_PATHS_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_ADVERTISE_MAP,
        CiscoNxosStructureUsage.BGP_ATTRIBUTE_MAP,
        CiscoNxosStructureUsage.BGP_DAMPENING_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_DEFAULT_ORIGINATE_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_EXIST_MAP,
        CiscoNxosStructureUsage.BGP_INJECT_MAP,
        CiscoNxosStructureUsage.BGP_L2VPN_EVPN_RETAIN_ROUTE_TARGET_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_ADVERTISE_MAP,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_EXIST_MAP,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_NON_EXIST_MAP,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_ROUTE_MAP_IN,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_ROUTE_MAP_OUT,
        CiscoNxosStructureUsage.BGP_NETWORK_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_NETWORK6_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_NEXTHOP_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_DIRECT_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_EIGRP_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_ISIS_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_LISP_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_OSPF_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_OSPFV3_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_RIP_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_STATIC_ROUTE_MAP,
        CiscoNxosStructureUsage.BGP_SUPPRESS_MAP,
        CiscoNxosStructureUsage.BGP_TABLE_MAP,
        CiscoNxosStructureUsage.BGP_UNSUPPRESS_MAP,
        CiscoNxosStructureUsage.OSPF_AREA_FILTER_LIST_IN,
        CiscoNxosStructureUsage.OSPF_AREA_FILTER_LIST_OUT);
    markConcreteStructure(
        CiscoNxosStructureType.ROUTER_OSPF,
        CiscoNxosStructureUsage.BGP_REDISTRIBUTE_OSPF_SOURCE_TAG);
    markConcreteStructure(
        CiscoNxosStructureType.BGP_TEMPLATE_PEER,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_INHERIT_PEER);
    markConcreteStructure(
        CiscoNxosStructureType.BGP_TEMPLATE_PEER_POLICY,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_INHERIT_PEER_POLICY);
    markConcreteStructure(
        CiscoNxosStructureType.BGP_TEMPLATE_PEER_SESSION,
        CiscoNxosStructureUsage.BGP_NEIGHBOR_INHERIT_PEER_SESSION);
    markConcreteStructure(CiscoNxosStructureType.VLAN, CiscoNxosStructureUsage.INTERFACE_VLAN);
    markConcreteStructure(
        CiscoNxosStructureType.VRF,
        CiscoNxosStructureUsage.INTERFACE_VRF_MEMBER,
        CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_VRF);
  }

  @Override
  public void setHostname(String hostname) {
    checkNotNull(hostname, "hostname cannot be null");
    _hostname = hostname.toLowerCase();
  }

  @Override
  public void setVendor(ConfigurationFormat format) {}

  private static @Nonnull org.batfish.datamodel.hsrp.HsrpGroup toHsrpGroup(HsrpGroup group) {
    org.batfish.datamodel.hsrp.HsrpGroup.Builder builder =
        org.batfish.datamodel.hsrp.HsrpGroup.builder()
            .setGroupNumber(group.getGroup())
            .setIp(group.getIp())
            .setPreempt(
                group.getPreemptDelayMinimumSeconds() != null); // true iff any preempt delay is set
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

  private @Nonnull org.batfish.datamodel.Interface toInterface(Interface iface) {
    String ifaceName = iface.getName();
    org.batfish.datamodel.Interface.Builder newIfaceBuilder =
        org.batfish.datamodel.Interface.builder().setName(ifaceName);

    String parent = iface.getParentInterface();
    if (parent != null) {
      newIfaceBuilder.setDependencies(ImmutableSet.of(new Dependency(parent, DependencyType.BIND)));
    }

    newIfaceBuilder.setActive(!iface.getShutdown());

    if (iface.getAddress() != null) {
      newIfaceBuilder.setAddress(iface.getAddress().getAddress());
    }
    newIfaceBuilder.setSecondaryAddresses(
        iface.getSecondaryAddresses().stream()
            .map(InterfaceAddressWithAttributes::getAddress)
            .collect(ImmutableSet.toImmutableSet()));

    newIfaceBuilder.setDescription(iface.getDescription());

    newIfaceBuilder.setMtu(iface.getMtu());

    // switchport+vlan settings
    SwitchportMode switchportMode = iface.getSwitchportMode();
    newIfaceBuilder.setSwitchportMode(switchportMode);
    switch (iface.getSwitchportMode()) {
      case ACCESS:
        newIfaceBuilder.setSwitchport(true);
        newIfaceBuilder.setAccessVlan(iface.getAccessVlan());
        break;

      case NONE:
        newIfaceBuilder.setEncapsulationVlan(iface.getEncapsulationVlan());
        break;

      case TRUNK:
        newIfaceBuilder.setSwitchport(true);
        newIfaceBuilder.setAllowedVlans(iface.getAllowedVlans());
        newIfaceBuilder.setNativeVlan(iface.getNativeVlan());
        break;

      case DOT1Q_TUNNEL:
      case DYNAMIC_AUTO:
      case DYNAMIC_DESIRABLE:
      case FEX_FABRIC:
      case TAP:
      case TOOL:
      default:
        // unsupported
        break;
    }
    newIfaceBuilder.setVlan(iface.getVlan());
    newIfaceBuilder.setAutoState(iface.getAutostate());

    CiscoNxosInterfaceType type = iface.getType();
    newIfaceBuilder.setType(toInterfaceType(type, parent != null));

    Double speed;
    @Nullable Integer speedMbps = iface.getSpeedMbps();
    if (speedMbps != null) {
      speed = speedMbps * SPEED_CONVERSION_FACTOR;
    } else {
      speed = getDefaultSpeed(type);
    }
    newIfaceBuilder.setSpeed(speed);
    Integer nxosBandwidth = iface.getBandwidth();
    Double finalBandwidth;
    if (nxosBandwidth != null) {
      finalBandwidth = nxosBandwidth * BANDWIDTH_CONVERSION_FACTOR;
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

    // OSPF properties
    OspfInterface ospf = iface.getOspf();
    if (ospf != null) {
      newIfaceBuilder.setOspfPointToPoint(ospf.getNetwork() == OspfNetworkType.POINT_TO_POINT);
      // TODO: update data model to support explicit hello and dead intervals
    }

    org.batfish.datamodel.Interface newIface = newIfaceBuilder.build();

    String vrfName = iface.getVrfMember();

    if (vrfName != null) {
      org.batfish.datamodel.Vrf vrf = _c.getVrfs().get(vrfName);
      if (vrf == null) {
        // Non-existent VRF set; disable and put in null VRF
        newIface.setActive(false);
        vrf = _c.getVrfs().get(NULL_VRF_NAME);
      } else if (_vrfs.get(vrfName).getShutdown()) {
        // VRF is shutdown; disable
        newIface.setActive(false);
      }
      newIface.setVrf(vrf);
    } else {
      // No VRF set; put in default VRF
      newIface.setVrf(_c.getDefaultVrf());
    }

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
                    .setName(Long.toString(actionIpAccessListLine.getLine()))
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

  /**
   * Convert a VS {@link DefaultVrfOspfProcess} to a VI {@link
   * org.batfish.datamodel.ospf.OspfProcess} in the default VRF.
   */
  private @Nonnull org.batfish.datamodel.ospf.OspfProcess toOspfProcess(
      DefaultVrfOspfProcess proc) {
    Ip routerId = proc.getRouterId() != null ? proc.getRouterId() : computeDefaultRouterId(_c);
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
      DefaultVrfOspfProcess proc, OspfVrf ospfVrf) {
    String processName = proc.getName();
    Ip routerId =
        ospfVrf.getRouterId() != null
            ? ospfVrf.getRouterId()
            : proc.getRouterId() != null ? proc.getRouterId() : computeDefaultRouterId(_c);
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
    Optional.ofNullable(proc.getRedistributeStaticRouteMap())
        .filter(_c.getRoutingPolicies()::containsKey)
        .ifPresent(
            routeMapName -> {
              exportPolicySourcesBuilder.add(routeMapName);
              exportStatementsBuilder.add(
                  new If(
                      new MatchProtocol(RoutingProtocol.STATIC),
                      ImmutableList.of(new CallStatement(routeMapName))));
            });

    // Then try orginating default route (either always or from RIB route not covered above)
    if (defaultOriginate != null) {
      BooleanExpr matchDefaultNetwork =
          new MatchPrefixSet(
              DestinationNetwork.instance(),
              new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(Prefix.ZERO))));
      BooleanExpr guard;
      if (defaultOriginate.getAlways()) {
        builder.setGeneratedRoutes(
            ImmutableSortedSet.of(GeneratedRoute.builder().setNetwork(Prefix.ZERO).build()));
        guard =
            new Conjunction(
                ImmutableList.<BooleanExpr>builder()
                    .add(matchDefaultNetwork)
                    .add(new MatchProtocol(RoutingProtocol.AGGREGATE))
                    .build());
      } else {
        guard = matchDefaultNetwork;
      }
      ImmutableList.Builder<Statement> defaultOriginateStatements = ImmutableList.builder();
      Optional.ofNullable(defaultOriginate.getRouteMap())
          .filter(_c.getRoutingPolicies()::containsKey)
          .ifPresent(
              defaultOriginateRouteMapName -> {
                exportPolicySourcesBuilder.add(defaultOriginateRouteMapName);
                defaultOriginateStatements.add(new CallStatement(defaultOriginateRouteMapName));
              });
      defaultOriginateStatements.add(Statements.ExitAccept.toStaticStatement());
      exportStatementsBuilder.add(new If(guard, defaultOriginateStatements.build()));
    }
    // Then try remaining redistribution policies
    Optional.ofNullable(proc.getRedistributeDirectRouteMap())
        .filter(_c.getRoutingPolicies()::containsKey)
        .ifPresent(
            routeMapName -> {
              exportPolicySourcesBuilder.add(routeMapName);
              exportStatementsBuilder.add(
                  new If(
                      new MatchProtocol(RoutingProtocol.CONNECTED),
                      ImmutableList.of(new CallStatement(routeMapName))));
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
                      ifaceName,
                      areaId,
                      processName,
                      proc.getPassiveInterfaceDefault(),
                      iface.getOspf());
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
                    iface.getOspf());
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
    newIface.setOspfCost(ospf.getCost());
    newIface.setOspfEnabled(true);
    newIface.setOspfAreaName(areaId);
    newIface.setOspfProcess(processName);
    newIface.setOspfPassive(
        ospf.getPassive() != null
            ? ospf.getPassive()
            : passiveInterfaceDefault || newIface.getName().startsWith("loopback"));
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

  private @Nonnull RoutingPolicy toRoutingPolicy(RouteMap routeMap) {
    // TODO: support continue entries
    ImmutableList.Builder<Statement> statements = ImmutableList.builder();
    routeMap.getEntries().values().stream().map(this::toStatement).forEach(statements::add);
    statements.add(ROUTE_MAP_DENY_STATEMENT);
    // TODO: clean up setting of owner
    return RoutingPolicy.builder()
        .setName(routeMap.getName())
        .setOwner(_c)
        .setStatements(statements.build())
        .build();
  }

  private @Nonnull Statement toStatement(RouteMapEntry entry) {
    ImmutableList.Builder<Statement> trueStatements = ImmutableList.builder();
    ImmutableList.Builder<BooleanExpr> conjuncts = ImmutableList.builder();

    // matches
    entry.getMatches().map(this::toBooleanExpr).forEach(conjuncts::add);

    // sets
    entry.getSets().flatMap(this::toStatements).forEach(trueStatements::add);

    // final action if matched
    LineAction action = entry.getAction();
    if (action == LineAction.PERMIT) {
      trueStatements.add(ROUTE_MAP_PERMIT_STATEMENT);
    } else {
      assert action == LineAction.DENY;
      trueStatements.add(ROUTE_MAP_DENY_STATEMENT);
    }
    return new If(new Conjunction(conjuncts.build()), trueStatements.build(), ImmutableList.of());
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
                    .filter(_ipCommunityLists::containsKey)
                    .map(name -> new MatchEntireCommunitySet(new NamedCommunitySet(name)))
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
                                .filter(_interfaces::containsKey)
                                .map(_interfaces::get)
                                .flatMap(
                                    iface ->
                                        Stream.concat(
                                            Stream.of(iface.getAddress()),
                                            iface.getSecondaryAddresses().stream()))
                                .map(InterfaceAddressWithAttributes::getAddress)
                                .filter(ConcreteInterfaceAddress.class::isInstance)
                                .map(ConcreteInterfaceAddress.class::cast)
                                .flatMap(
                                    address ->
                                        address.getPrefix().getPrefixLength() <= 30
                                            ? Stream.of(
                                                address.getPrefix(),
                                                Prefix.create(
                                                    address.getIp(), Prefix.MAX_PREFIX_LENGTH))
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
          public BooleanExpr visitRouteMapMatchMetric(RouteMapMatchMetric routeMapMatchMetric) {
            return new MatchMetric(
                IntComparator.EQ, new LiteralLong(routeMapMatchMetric.getMetric()));
          }

          @Override
          public BooleanExpr visitRouteMapMatchTag(RouteMapMatchTag routeMapMatchTag) {
            return new MatchTag(IntComparator.EQ, new LiteralLong(routeMapMatchTag.getTag()));
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
          public Stream<Statement> visitRouteMapSetCommunity(
              RouteMapSetCommunity routeMapSetCommunity) {
            CommunitySetExpr communities =
                new LiteralCommunitySet(routeMapSetCommunity.getCommunities());
            return Stream.of(
                routeMapSetCommunity.getAdditive()
                    ? new AddCommunity(communities)
                    : new SetCommunity(communities));
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
            return Stream.of(new SetNextHop(new IpNextHop(nextHopIps), false));
          }

          @Override
          public Stream<Statement> visitRouteMapSetIpNextHopUnchanged(
              RouteMapSetIpNextHopUnchanged routeMapSetIpNextHopUnchanged) {
            // TODO: implement
            return Stream.empty();
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
        .setAdministrativeCost((int) staticRoute.getPreference())
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

    convertVrfs();
    convertInterfaces();
    disableUnregisteredVlanInterfaces();
    convertStaticRoutes();
    convertObjectGroups();
    convertIpAccessLists();
    convertIpAsPathAccessLists();
    convertIpPrefixLists();
    convertIpCommunityLists();
    convertRouteMaps();
    computeImplicitOspfAreas();
    convertOspfProcesses();
    convertNves();
    convertBgp();

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
