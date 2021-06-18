package org.batfish.representation.cumulus;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.datamodel.bgp.AllowRemoteAsOutMode.ALWAYS;
import static org.batfish.datamodel.bgp.VniConfig.importRtPatternForAnyAs;
import static org.batfish.datamodel.routing_policy.Common.generateSuppressionPolicy;
import static org.batfish.representation.cumulus.BgpProcess.BGP_UNNUMBERED_IP;
import static org.batfish.representation.cumulus.CumulusConcatenatedConfiguration.CUMULUS_CLAG_DOMAIN_ID;
import static org.batfish.representation.cumulus.CumulusConcatenatedConfiguration.LOOPBACK_INTERFACE_NAME;
import static org.batfish.representation.cumulus.CumulusRoutingProtocol.VI_PROTOCOLS_MAP;
import static org.batfish.representation.cumulus.InterfaceConverter.getSuperInterfaceName;
import static org.batfish.representation.cumulus.OspfInterface.DEFAULT_OSPF_DEAD_INTERVAL;
import static org.batfish.representation.cumulus.OspfInterface.DEFAULT_OSPF_HELLO_INTERVAL;
import static org.batfish.representation.cumulus.OspfProcess.DEFAULT_OSPF_PROCESS_NAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.BgpAggregate;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.ospf.OspfAddresses;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfAreaSummary.SummaryRouteBehavior;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.routing_policy.Common;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.ColonSeparatedRendering;
import org.batfish.datamodel.routing_policy.communities.CommunityAcl;
import org.batfish.datamodel.routing_policy.communities.CommunityAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunityIn;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAcl;
import org.batfish.datamodel.routing_policy.communities.CommunitySetAclLine;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchRegex;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.communities.TypesFirstAscendingSpaceSeparated;
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
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.Not;
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
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;
import org.batfish.datamodel.vxlan.Vni;
import org.batfish.vendor.VendorStructureId;

/** Utilities that convert Cumulus-specific representations to vendor-independent model. */
@ParametersAreNonnullByDefault
public final class CumulusConversions {
  private static final int AGGREGATE_ROUTE_ADMIN_COST = 200; // TODO verify this

  private static final Prefix LOOPBACK_PREFIX = Prefix.parse("127.0.0.0/8");

  public static final int DEFAULT_STATIC_ROUTE_ADMINISTRATIVE_DISTANCE = 1;
  public static final int DEFAULT_STATIC_ROUTE_METRIC = 0;
  private static final int MAX_ADMINISTRATIVE_COST = 32767;

  private static final Long DEFAULT_REDISTRIBUTE_METRIC = 20L;
  private static final OspfMetricType DEFAULT_REDISTRIBUTE_METRIC_TYPE = OspfMetricType.E2;

  public static final Ip CLAG_LINK_LOCAL_IP = Ip.parse("169.254.40.94");

  public static final long DEFAULT_MAX_MED = 4294967294L;
  public static final long DEFAULT_OSPF_MAX_METRIC = 0xFFFF;

  @VisibleForTesting
  static GeneratedRoute GENERATED_DEFAULT_ROUTE =
      GeneratedRoute.builder().setNetwork(Prefix.ZERO).setAdmin(MAX_ADMINISTRATIVE_COST).build();

  @VisibleForTesting
  static final Statement REJECT_DEFAULT_ROUTE =
      new If(
          Common.matchDefaultRoute(), ImmutableList.of(Statements.ReturnFalse.toStaticStatement()));
  /**
   * Conversion factor for interface speed units. In the config Mbps are used, VI model expects bps
   */
  public static final double SPEED_CONVERSION_FACTOR = 10e6;

  // TODO: need to verify this
  public static final double DEFAULT_LOOPBACK_BANDWIDTH = 8e9;

  /**
   * Bandwidth cannot be determined from name alone, so we choose the following made-up plausible
   * value in absence of explicit information.
   */
  public static final double DEFAULT_PORT_BANDWIDTH = 10E9D;

  public static final int DEFAULT_LOOPBACK_MTU = 65536;

  /**
   * Bandwidth cannot be determined from name alone, so we choose the following made-up plausible
   * value in absence of explicit information.
   */
  public static final int DEFAULT_PORT_MTU = 1500;

  public static @Nonnull String computeBgpCommonExportPolicyName(String vrfName) {
    return String.format("~BGP_COMMON_EXPORT_POLICY:%s~", vrfName);
  }

  @VisibleForTesting
  public static @Nonnull String computeBgpPeerExportPolicyName(
      String vrfName, String peerInterface) {
    return String.format("~BGP_PEER_EXPORT_POLICY:%s:%s~", vrfName, peerInterface);
  }

  static String computeBgpDefaultRouteExportPolicyName(boolean ipv4, String vrf, String peer) {
    return String.format(
        "~BGP_DEFAULT_ROUTE_PEER_EXPORT_POLICY:IPv%s:%s:%s~", ipv4 ? "4" : "6", vrf, peer);
  }

  public static @Nonnull String computeBgpPeerImportPolicyName(String vrf, String peer) {
    return String.format("~BGP_PEER_IMPORT_POLICY:%s:%s~", vrf, peer);
  }

  public static String computeBgpGenerationPolicyName(boolean ipv4, String vrfName, String prefix) {
    return String.format("~AGGREGATE_ROUTE%s_GEN:%s:%s~", ipv4 ? "" : "6", vrfName, prefix);
  }

  public static String computeMatchSuppressedSummaryOnlyPolicyName(String vrfName) {
    return String.format("~MATCH_SUPPRESSED_SUMMARY_ONLY:%s~", vrfName);
  }

  public static String computeOspfExportPolicyName(String vrfName) {
    return String.format("~OSPF_EXPORT_POLICY:%s~", vrfName);
  }

  public static String computeOspfAreaRangeFilterName(String vrfName, long area) {
    return String.format("~OSPF_AREA_RANGE:%s:%s~", vrfName, area);
  }

  /**
   * Infers the peer Ip for the interface. This is doable only if the interface has exactly one
   * (primary) address that is a /30 or /31. For a /30, the address must NOT be the first or last
   * address in the range.
   *
   * @return the inferred Ip or empty optional if that is not possible.
   */
  @VisibleForTesting
  @Nonnull
  static Optional<Ip> inferPeerIp(Interface viIface) {
    // one concrete interface address
    if (viIface.getAllAddresses().size() != 1
        || viIface.getAddress() == null
        || !(viIface.getAddress() instanceof ConcreteInterfaceAddress)) {
      return Optional.empty();
    }
    ConcreteInterfaceAddress ifaceAddress =
        (ConcreteInterfaceAddress) viIface.getAllAddresses().iterator().next();
    Prefix prefix = ifaceAddress.getPrefix();

    if (ifaceAddress.getNetworkBits() == Prefix.MAX_PREFIX_LENGTH - 1) { // 31
      return Optional.of(
          ifaceAddress.getIp().equals(prefix.getStartIp())
              ? prefix.getEndIp()
              : prefix.getStartIp());
    }
    if (ifaceAddress.getNetworkBits() == Prefix.MAX_PREFIX_LENGTH - 2) { // 30
      if (ifaceAddress.getIp().equals(prefix.getStartIp())
          || ifaceAddress.getIp().equals(prefix.getEndIp())) {
        return Optional.empty();
      }
      return Optional.of(
          ifaceAddress.getIp().equals(prefix.getFirstHostIp())
              ? prefix.getLastHostIp()
              : prefix.getFirstHostIp());
    }

    return Optional.empty();
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

  static BooleanExpr generateExportAggregateConditions(
      Map<Prefix, BgpVrfAddressFamilyAggregateNetworkConfiguration> aggregateNetworks) {
    return new Disjunction(
        aggregateNetworks.entrySet().stream()
            .map(
                entry -> {
                  Prefix prefix = entry.getKey();

                  // Conditions to generate this route
                  List<BooleanExpr> exportAggregateConjuncts = new ArrayList<>();
                  exportAggregateConjuncts.add(
                      new MatchPrefixSet(
                          DestinationNetwork.instance(),
                          new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(prefix)))));
                  exportAggregateConjuncts.add(new MatchProtocol(RoutingProtocol.AGGREGATE));

                  // TODO consider attribute map
                  BooleanExpr weInterior = BooleanExprs.TRUE;
                  exportAggregateConjuncts.add(
                      bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.IGP));

                  // Do export a generated aggregate.
                  return new Conjunction(exportAggregateConjuncts);
                })
            .collect(ImmutableList.toImmutableList()));
  }

  /** Creates BGP aggregates aggregate routes for the input vrf. */
  static void generateBgpAggregates(
      Configuration c,
      org.batfish.datamodel.BgpProcess proc,
      Map<Prefix, BgpVrfAddressFamilyAggregateNetworkConfiguration> aggregateNetworks) {
    aggregateNetworks.entrySet().stream()
        .map(
            aggregateByPrefixEntry ->
                toBgpAggregate(
                    aggregateByPrefixEntry.getKey(), aggregateByPrefixEntry.getValue(), c))
        .forEach(proc::addAggregate);
  }

  private static @Nonnull BgpAggregate toBgpAggregate(
      Prefix prefix,
      BgpVrfAddressFamilyAggregateNetworkConfiguration vsAggregate,
      Configuration c) {
    // TODO: handle as-set
    // TODO: handle matching-MED-only
    // TODO: handle origin
    // TODO: handle suppress-map
    // TODO: verify undefined route-map can be treated as omitted
    String routeMap =
        Optional.ofNullable(vsAggregate.getRouteMap())
            .filter(c.getRoutingPolicies()::containsKey)
            .orElse(null);
    return BgpAggregate.of(
        prefix, generateSuppressionPolicy(vsAggregate.isSummaryOnly(), c), null, routeMap);
  }

  /**
   * Creates a generation policy for the aggregate network with the given {@link Prefix}. The
   * generation policy matches any route with a destination more specific than {@code prefix}.
   *
   * @param c {@link Configuration} in which to create the generation policy
   * @param vrfName Name of VRF in which the aggregate network exists
   * @param prefix The aggregate network prefix
   */
  static void generateGenerationPolicy(Configuration c, String vrfName, Prefix prefix) {
    RoutingPolicy.builder()
        .setOwner(c)
        .setName(computeBgpGenerationPolicyName(true, vrfName, prefix.toString()))
        .addStatement(
            new If(
                // Match routes with destination networks more specific than prefix.
                new MatchPrefixSet(
                    DestinationNetwork.instance(),
                    new ExplicitPrefixSet(new PrefixSpace(PrefixRange.moreSpecificThan(prefix)))),
                ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                ImmutableList.of(Statements.ReturnFalse.toStaticStatement())))
        .build();
  }

  /**
   * Generates and returns a {@link Statement} that suppresses routes that are summarized by the
   * given set of {@link Prefix prefixes} configured as {@code summary-only}.
   *
   * <p>Returns {@code null} if {@code prefixesToSuppress} has no entries.
   *
   * <p>If any Batfish-generated structures are generated, does the bookkeeping in the provided
   * {@link Configuration} to ensure they are available and tracked.
   */
  @Nullable
  static If suppressSummarizedPrefixes(
      Configuration c, String vrfName, Stream<Prefix> summaryOnlyPrefixes) {
    Iterator<Prefix> prefixesToSuppress = summaryOnlyPrefixes.iterator();
    if (!prefixesToSuppress.hasNext()) {
      return null;
    }
    // Create a RouteFilterList that matches any network longer than a prefix marked summary only.
    RouteFilterList matchLonger =
        new RouteFilterList(computeMatchSuppressedSummaryOnlyPolicyName(vrfName));
    prefixesToSuppress.forEachRemaining(
        p ->
            matchLonger.addLine(
                new RouteFilterLine(LineAction.PERMIT, PrefixRange.moreSpecificThan(p))));
    // Bookkeeping: record that we created this RouteFilterList to match longer networks.
    c.getRouteFilterLists().put(matchLonger.getName(), matchLonger);

    return new If(
        "Suppress more specific networks for summary-only aggregate-address networks",
        new MatchPrefixSet(
            DestinationNetwork.instance(), new NamedPrefixSet(matchLonger.getName())),
        ImmutableList.of(Statements.Suppress.toStaticStatement()),
        ImmutableList.of());
  }

  static void convertBgpProcess(
      Configuration c, CumulusConcatenatedConfiguration vsConfig, Warnings w) {
    BgpProcess bgpProcess = vsConfig.getBgpProcess();
    if (bgpProcess == null) {
      return;
    }
    // First pass: only core processes
    c.getDefaultVrf()
        .setBgpProcess(toBgpProcess(c, vsConfig, DEFAULT_VRF_NAME, bgpProcess.getDefaultVrf()));
    // We make one VI process per VRF because our current datamodel requires it
    bgpProcess
        .getVrfs()
        .forEach(
            (vrfName, bgpVrf) ->
                c.getVrfs().get(vrfName).setBgpProcess(toBgpProcess(c, vsConfig, vrfName, bgpVrf)));

    // Create dud processes for other VRFs that use VNIs, so we can have proper RIBs
    c.getVrfs()
        .forEach(
            (vrfName, vrf) -> {
              if (!vrf.getLayer2Vnis().isEmpty()
                  || !vrf.getLayer3Vnis().isEmpty() // VRF has some VNI
                      && vrf.getBgpProcess() == null // process does not already exist
                      && c.getDefaultVrf().getBgpProcess() != null) { // there is a default BGP proc
                vrf.setBgpProcess(
                    org.batfish.datamodel.BgpProcess.builder()
                        .setRouterId(c.getDefaultVrf().getBgpProcess().getRouterId())
                        .setAdminCostsToVendorDefaults(ConfigurationFormat.CUMULUS_CONCATENATED)
                        .build());
              }
            });

    /*
     * Second pass: Add neighbors.
     * Requires all VRFs & bgp processes in a VRF to be set in VI so that we can initialize address families
     * that access other VRFs (e.g., EVPN)
     */
    Iterables.concat(ImmutableSet.of(bgpProcess.getDefaultVrf()), bgpProcess.getVrfs().values())
        .forEach(
            bgpVrf -> {
              bgpVrf
                  .getNeighbors()
                  .values()
                  .forEach(neighbor -> addBgpNeighbor(c, vsConfig, bgpVrf, neighbor, w));
            });
  }

  /**
   * Returns {@link org.batfish.datamodel.BgpProcess} for named {@code bgpVrf} if valid, or else
   * {@code null}.
   */
  @Nullable
  static org.batfish.datamodel.BgpProcess toBgpProcess(
      Configuration c, CumulusConcatenatedConfiguration vsConfig, String vrfName, BgpVrf bgpVrf) {
    BgpProcess bgpProcess = vsConfig.getBgpProcess();
    Ip routerId = bgpVrf.getRouterId();
    if (routerId == null) {
      routerId = inferRouterId(c);
    }
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(routerId, ebgpAdmin, ibgpAdmin);
    newProc.setMultipathEquivalentAsPathMatchMode(EXACT_PATH);
    /*
      BGP multipath enabled by default
      https://docs.cumulusnetworks.com/cumulus-linux/Layer-3/Border-Gateway-Protocol-BGP/#maximum-paths
    */
    newProc.setMultipathEbgp(true);
    newProc.setMultipathIbgp(true);
    if (firstNonNull(bgpVrf.getAsPathMultipathRelax(), Boolean.FALSE)) {
      newProc.setMultipathEquivalentAsPathMatchMode(PATH_LENGTH);
    }
    /*
    Cluster List Length functions like IGP cost in FRR/Quagga
    */
    newProc.setClusterListAsIgpCost(true);

    Long confederationId = bgpProcess.getDefaultVrf().getConfederationId();
    Long asn = bgpProcess.getDefaultVrf().getAutonomousSystem();
    if (confederationId != null && asn != null) {
      // TODO: there probably is another way to define confederation members
      newProc.setConfederation(new BgpConfederation(confederationId, ImmutableSet.of(asn)));
    }

    if (bgpVrf.isIpv4UnicastActive()) {
      BgpIpv4UnicastAddressFamily ipv4Unicast =
          firstNonNull(bgpVrf.getIpv4Unicast(), new BgpIpv4UnicastAddressFamily());

      // Add networks from network statements to new process's origination space
      Sets.union(bgpVrf.getNetworks().keySet(), ipv4Unicast.getNetworks().keySet())
          .forEach(newProc::addToOriginationSpace);

      // Generate aggregate routes
      generateBgpAggregates(c, newProc, ipv4Unicast.getAggregateNetworks());
    }

    generateBgpCommonExportPolicy(c, vrfName, bgpVrf, vsConfig.getRouteMaps());

    return newProc;
  }

  @VisibleForTesting
  static void addBgpNeighbor(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      BgpVrf bgpVrf,
      BgpNeighbor neighbor,
      Warnings w) {

    org.batfish.datamodel.BgpProcess viBgpProcess =
        c.getVrfs().get(bgpVrf.getVrfName()).getBgpProcess();
    neighbor.inheritFrom(bgpVrf.getNeighbors());
    Long localAs = null;
    if (neighbor.getLocalAs() != null) {
      localAs = neighbor.getLocalAs();
    } else if (bgpVrf.getAutonomousSystem() != null) {
      localAs = bgpVrf.getAutonomousSystem();
    }

    if (neighbor.getRemoteAs() == null) {
      w.redFlag(
          "Skipping BGP neighbor " + neighbor.getName() + ": missing remote-as configuration");
      return;
    }
    if (neighbor.getRemoteAs().getRemoteAs(localAs).isEmpty()) {
      w.redFlag(
          "Skipping BGP neighbor "
              + neighbor.getName()
              + ": unable to determine remote-as without local-as configured");
      return;
    }

    if (neighbor instanceof BgpInterfaceNeighbor) {
      BgpInterfaceNeighbor interfaceNeighbor = (BgpInterfaceNeighbor) neighbor;
      addInterfaceBgpNeighbor(c, vsConfig, interfaceNeighbor, localAs, bgpVrf, viBgpProcess, w);
    } else if (neighbor instanceof BgpIpNeighbor) {
      BgpIpNeighbor ipNeighbor = (BgpIpNeighbor) neighbor;
      addIpv4BgpNeighbor(c, vsConfig, ipNeighbor, localAs, bgpVrf, viBgpProcess, w);
    } else if (!(neighbor instanceof BgpPeerGroupNeighbor || neighbor instanceof BgpIpv6Neighbor)) {
      throw new IllegalArgumentException(
          "Unsupported BGP neighbor type: " + neighbor.getClass().getSimpleName());
    }
  }

  private static void addInterfaceBgpNeighbor(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      BgpInterfaceNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc,
      Warnings w) {
    BgpPeerConfig.Builder<?, ?> peerConfigBuilder;

    // if an interface neighbor has only one address and that address is a /31, it gets treated as
    // numbered peer
    Interface viIface = c.getAllInterfaces().get(neighbor.getName());

    // if this interface is not defined warn and move on
    if (viIface == null) {
      w.redFlag(
          String.format(
              "BGP interface neighbor is defined on %s, but the interface does not exist on the"
                  + " device",
              neighbor.getName()));
      return;
    }

    Optional<Ip> inferredIp = inferPeerIp(viIface);
    if (inferredIp.isPresent()) {
      InterfaceAddress localAddress = viIface.getAddress();
      // consequence of inferredIp
      assert localAddress instanceof ConcreteInterfaceAddress;
      peerConfigBuilder =
          BgpActivePeerConfig.builder()
              .setLocalIp(((ConcreteInterfaceAddress) localAddress).getIp())
              .setPeerAddress(inferredIp.get());
    } else {
      peerConfigBuilder =
          BgpUnnumberedPeerConfig.builder()
              .setLocalIp(BGP_UNNUMBERED_IP)
              .setPeerInterface(neighbor.getName());
    }
    generateBgpCommonPeerConfig(
        c, vsConfig, neighbor, localAs, bgpVrf, newProc, peerConfigBuilder, w);
  }

  @VisibleForTesting
  static void generateBgpCommonPeerConfig(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      BgpNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc,
      BgpPeerConfig.Builder<?, ?> peerConfigBuilder,
      Warnings w) {
    assert neighbor.getRemoteAs() != null; // precondition

    RoutingPolicy exportRoutingPolicy =
        computeBgpNeighborExportRoutingPolicy(c, neighbor, bgpVrf, localAs);
    @Nullable
    RoutingPolicy importRoutingPolicy = computeBgpNeighborImportRoutingPolicy(c, neighbor, bgpVrf);

    peerConfigBuilder
        .setBgpProcess(newProc)
        .setClusterId(inferClusterId(bgpVrf, newProc.getRouterId(), neighbor, localAs))
        .setConfederation(bgpVrf.getConfederationId())
        .setDescription(neighbor.getDescription())
        .setGroup(neighbor.getPeerGroup())
        .setLocalAs(localAs)
        .setRemoteAsns(neighbor.getRemoteAs().getRemoteAs(localAs))
        .setEbgpMultihop(neighbor.getEbgpMultihop() != null)
        .setGeneratedRoutes(
            bgpDefaultOriginate(neighbor) ? ImmutableSet.of(GENERATED_DEFAULT_ROUTE) : null)
        // Ipv4 unicast is enabled by default
        .setIpv4UnicastAddressFamily(
            convertIpv4UnicastAddressFamily(
                neighbor.getIpv4UnicastAddressFamily(),
                bgpVrf.getDefaultIpv4Unicast(),
                exportRoutingPolicy,
                importRoutingPolicy))
        .setEvpnAddressFamily(
            toEvpnAddressFamily(
                c, vsConfig, neighbor, localAs, bgpVrf, newProc, exportRoutingPolicy, w))
        .build();
  }

  @VisibleForTesting
  @Nullable
  static Ipv4UnicastAddressFamily convertIpv4UnicastAddressFamily(
      @Nullable BgpNeighborIpv4UnicastAddressFamily ipv4UnicastAddressFamily,
      boolean defaultIpv4Unicast,
      RoutingPolicy exportRoutingPolicy,
      @Nullable RoutingPolicy importRoutingPolicy) {

    // check if address family should be activated
    boolean explicitActivationSetting =
        ipv4UnicastAddressFamily != null && ipv4UnicastAddressFamily.getActivated() != null;
    if ((explicitActivationSetting && !ipv4UnicastAddressFamily.getActivated())
        || (!explicitActivationSetting && !defaultIpv4Unicast)) {
      return null;
    }

    // According to the docs, the neighbor must have been explicitly activated for
    // route-reflector-client to take effect:
    // https://docs.cumulusnetworks.com/display/DOCS/Border+Gateway+Protocol+-+BGP#BorderGatewayProtocol-BGP-RouteReflectors
    //
    // The docs appear to be wrong: explicit activation is not enforced by FRR, and we have tested
    // that route reflection works without it.
    boolean routeReflectorClient =
        Optional.ofNullable(ipv4UnicastAddressFamily)
            .map(af -> Boolean.TRUE.equals(af.getRouteReflectorClient()))
            .orElse(false);

    return Ipv4UnicastAddressFamily.builder()
        .setAddressFamilyCapabilities(
            AddressFamilyCapabilities.builder()
                .setSendCommunity(true)
                .setSendExtendedCommunity(true)
                .setAllowLocalAsIn(
                    (ipv4UnicastAddressFamily != null
                        && (firstNonNull(ipv4UnicastAddressFamily.getAllowAsIn(), 0) > 0)))
                .setAllowRemoteAsOut(ALWAYS) // no outgoing remote-as check on Cumulus
                .build())
        .setExportPolicy(exportRoutingPolicy.getName())
        .setImportPolicy(importRoutingPolicy == null ? null : importRoutingPolicy.getName())
        .setRouteReflectorClient(routeReflectorClient)
        .build();
  }

  private static void addIpv4BgpNeighbor(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      BgpIpNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc,
      Warnings w) {
    BgpActivePeerConfig.Builder peerConfigBuilder =
        BgpActivePeerConfig.builder()
            .setLocalIp(
                Optional.ofNullable(
                        resolveLocalIpFromUpdateSource(neighbor.getBgpNeighborSource(), c, w))
                    .orElse(
                        computeLocalIpForBgpNeighbor(neighbor.getPeerIp(), c, bgpVrf.getVrfName())))
            .setPeerAddress(neighbor.getPeerIp());
    generateBgpCommonPeerConfig(
        c, vsConfig, neighbor, localAs, bgpVrf, newProc, peerConfigBuilder, w);
  }

  @Nonnull
  private static RoutingPolicy computeBgpNeighborExportRoutingPolicy(
      Configuration c, BgpNeighbor neighbor, BgpVrf bgpVrf, @Nullable Long localAs) {
    String vrfName = bgpVrf.getVrfName();

    RoutingPolicy.Builder peerExportPolicy =
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(computeBgpPeerExportPolicyName(vrfName, neighbor.getName()));

    // If default originate is set for a neighbor, we will send it a "fresh" default route is not
    // subjected to the neighbor's outgoing route map. We will drop other default routes.
    if (bgpDefaultOriginate(neighbor)) {
      initBgpDefaultRouteExportPolicy(vrfName, neighbor.getName(), true, null, c);
      peerExportPolicy.addStatement(
          new If(
              "Export default route from peer with default-originate configured",
              new CallExpr(
                  computeBgpDefaultRouteExportPolicyName(true, vrfName, neighbor.getName())),
              singletonList(Statements.ReturnTrue.toStaticStatement()),
              ImmutableList.of()));

      peerExportPolicy.addStatement(REJECT_DEFAULT_ROUTE);
    }

    BooleanExpr peerExportConditions = computePeerExportConditions(neighbor, bgpVrf);
    List<Statement> acceptStmts = getAcceptStatements(neighbor, bgpVrf, localAs);

    peerExportPolicy.addStatement(
        new If(
            "peer-export policy main conditional: exitAccept if true / exitReject if false",
            peerExportConditions,
            acceptStmts,
            ImmutableList.of(Statements.ExitReject.toStaticStatement())));

    return peerExportPolicy.build();
  }

  /**
   * Initializes export policy for IPv4 or IPv6 default routes if it doesn't already exist. This
   * policy is the same across BGP processes, so only one is created for each configuration.
   *
   * @param ipv4 Whether to initialize the IPv4 or IPv6 default route export policy
   * @param defaultOriginateExportMapName Name of route-map to apply to generated route before
   *     export.
   */
  // TODO: This function is copied verbatim from CiscoConversations. Refactor after we've verified
  // the right behavior for default-originate.
  private static void initBgpDefaultRouteExportPolicy(
      String vrfName,
      String peerName,
      boolean ipv4,
      @Nullable String defaultOriginateExportMapName,
      Configuration c) {
    SetOrigin setOrigin =
        new SetOrigin(
            new LiteralOrigin(
                c.getConfigurationFormat() == ConfigurationFormat.CISCO_IOS
                    ? OriginType.IGP
                    : OriginType.INCOMPLETE,
                null));
    List<Statement> defaultRouteExportStatements;
    if (defaultOriginateExportMapName == null
        || !c.getRoutingPolicies().containsKey(defaultOriginateExportMapName)) {
      defaultRouteExportStatements =
          ImmutableList.of(setOrigin, Statements.ReturnTrue.toStaticStatement());
    } else {
      defaultRouteExportStatements =
          ImmutableList.of(
              setOrigin,
              new CallStatement(defaultOriginateExportMapName),
              Statements.ReturnTrue.toStaticStatement());
    }

    RoutingPolicy.builder()
        .setOwner(c)
        .setName(computeBgpDefaultRouteExportPolicyName(ipv4, vrfName, peerName))
        .addStatement(
            new If(
                new Conjunction(
                    ImmutableList.of(
                        ipv4 ? Common.matchDefaultRoute() : Common.matchDefaultRouteV6(),
                        new MatchProtocol(RoutingProtocol.AGGREGATE))),
                defaultRouteExportStatements))
        .addStatement(Statements.ReturnFalse.toStaticStatement())
        .build();
  }

  @Nullable
  @VisibleForTesting
  static RoutingPolicy computeBgpNeighborImportRoutingPolicy(
      Configuration c, BgpNeighbor neighbor, BgpVrf bgpVrf) {
    BooleanExpr peerImportConditions = getBgpNeighborImportPolicyCallExpr(neighbor);
    if (peerImportConditions == null) {
      return null;
    }

    String vrfName = bgpVrf.getVrfName();

    RoutingPolicy.Builder peerImportPolicy =
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(computeBgpPeerImportPolicyName(vrfName, neighbor.getName()));

    peerImportPolicy.addStatement(
        new If(
            "peer-import policy main conditional: exitAccept if true / exitReject if false",
            peerImportConditions,
            ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
            ImmutableList.of(Statements.ExitReject.toStaticStatement())));

    return peerImportPolicy.build();
  }

  private static BooleanExpr computePeerExportConditions(BgpNeighbor neighbor, BgpVrf bgpVrf) {
    BooleanExpr commonCondition =
        new CallExpr(computeBgpCommonExportPolicyName(bgpVrf.getVrfName()));
    BooleanExpr peerCondition = getBgpNeighborExportPolicyCallExpr(neighbor);

    return peerCondition == null
        ? commonCondition
        : new Conjunction(ImmutableList.of(commonCondition, peerCondition));
  }

  private static List<Statement> getAcceptStatements(
      BgpNeighbor neighbor, BgpVrf bgpVrf, @Nullable Long localAs) {
    ImmutableList.Builder<Statement> acceptStatements = ImmutableList.builder();
    SetNextHop setNextHop = getSetNextHop(neighbor, localAs);
    SetMetric setMaxMedMetric = getSetMaxMedMetric(bgpVrf);

    if (setNextHop != null) {
      acceptStatements.add(setNextHop);
    }
    if (setMaxMedMetric != null) {
      acceptStatements.add(setMaxMedMetric);
    }
    acceptStatements.add(Statements.ExitAccept.toStaticStatement());

    return acceptStatements.build();
  }

  private static @Nullable CallExpr getBgpNeighborExportPolicyCallExpr(BgpNeighbor neighbor) {
    return Optional.ofNullable(neighbor.getIpv4UnicastAddressFamily())
        .map(BgpNeighborIpv4UnicastAddressFamily::getRouteMapOut)
        .map(CallExpr::new)
        .orElse(null);
  }

  private static @Nullable CallExpr getBgpNeighborImportPolicyCallExpr(BgpNeighbor neighbor) {
    return Optional.ofNullable(neighbor.getIpv4UnicastAddressFamily())
        .map(BgpNeighborIpv4UnicastAddressFamily::getRouteMapIn)
        .map(CallExpr::new)
        .orElse(null);
  }

  @VisibleForTesting
  static @Nullable SetNextHop getSetNextHop(BgpNeighbor neighbor, @Nullable Long localAs) {
    if (neighbor.getRemoteAs() == null || localAs == null) {
      return null;
    }

    // TODO: Need to handle dynamic neighbors.
    boolean nextHopSelf =
        Optional.ofNullable(neighbor.getIpv4UnicastAddressFamily())
            .map(BgpNeighborIpv4UnicastAddressFamily::getNextHopSelf)
            .orElse(false);

    // Note that since localAs != null, this is authoritative.
    boolean isIBgp = neighbor.getRemoteAs().isKnownIbgp(localAs);
    if (isIBgp) {
      // Check for "force".
      // TODO: Handle v6 AFI.
      BgpNeighborIpv4UnicastAddressFamily ipv4af = neighbor.getIpv4UnicastAddressFamily();
      if (ipv4af != null) {
        Boolean nextHopSelfAll = ipv4af.getNextHopSelfAll();
        if (nextHopSelfAll == null || !nextHopSelfAll) {
          nextHopSelf = false;
        }
      }
    }

    return nextHopSelf ? new SetNextHop(SelfNextHop.getInstance()) : null;
  }

  @VisibleForTesting
  static @Nullable SetMetric getSetMaxMedMetric(BgpVrf bgpVrf) {
    return bgpVrf.getMaxMedAdministrative() != null
        ? new SetMetric(new LiteralLong(bgpVrf.getMaxMedAdministrative()))
        : null;
  }

  @Nullable
  @VisibleForTesting
  static Ip resolveLocalIpFromUpdateSource(
      @Nullable BgpNeighborSource source, Configuration c, Warnings warnings) {
    if (source == null) {
      return null;
    }

    BgpNeighborSourceVisitor<Ip> visitor =
        new BgpNeighborSourceVisitor<Ip>() {

          @Override
          public Ip visitBgpNeighborSourceAddress(BgpNeighborSourceAddress updateSourceAddress) {
            return updateSourceAddress.getAddress();
          }

          @Nullable
          @Override
          public Ip visitBgpNeighborSourceInterface(
              BgpNeighborSourceInterface updateSourceInterface) {
            org.batfish.datamodel.Interface iface =
                c.getAllInterfaces().get(updateSourceInterface.getInterface());

            if (iface == null) {
              warnings.redFlag(
                  String.format(
                      "cannot find interface named %s for update-source",
                      updateSourceInterface.getInterface()));
              return null;
            }

            ConcreteInterfaceAddress concreteAddress = iface.getConcreteAddress();
            if (concreteAddress == null) {
              warnings.redFlag(
                  String.format(
                      "cannot find an address for interface named %s for update-source",
                      updateSourceInterface.getInterface()));
              return null;
            }

            return iface.getConcreteAddress().getIp();
          }
        };

    return source.accept(visitor);
  }

  /** Scan all interfaces, find first that contains given remote IP */
  @Nullable
  @VisibleForTesting
  static Ip computeLocalIpForBgpNeighbor(Ip remoteIp, Configuration c, String vrfName) {
    org.batfish.datamodel.Vrf vrf = c.getVrfs().get(vrfName);
    if (vrf == null) {
      return null;
    }
    return c.getAllInterfaces(vrf.getName()).values().stream()
        .flatMap(
            i ->
                i.getAllConcreteAddresses().stream()
                    .filter(
                        addr ->
                            addr.getPrefix().containsIp(remoteIp)
                                && !addr.getIp().equals(remoteIp)))
        .findFirst()
        .map(ConcreteInterfaceAddress::getIp)
        .orElse(null);
  }

  /**
   * Create common BGP export policy. This policy permits:
   *
   * <ul>
   *   <li>BGP and iBGP routes
   *   <li>routes whose network matches a configured network statement
   *   <li>routes whose protocol matches a configured protocol
   *   <li>redistribution policy
   * </ul>
   *
   * <p>all other routes are denied.
   */
  private static void generateBgpCommonExportPolicy(
      Configuration c, String vrfName, BgpVrf bgpVrf, Map<String, RouteMap> routeMaps) {
    RoutingPolicy bgpCommonExportPolicy =
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(computeBgpCommonExportPolicyName(vrfName))
            .build();

    List<Statement> statements = new ArrayList<>();

    // 1. If there are any ipv4 summary only networks, do not export the more specific routes.
    if (bgpVrf.getIpv4Unicast() != null) {
      Stream<Prefix> summarizedPrefixes =
          bgpVrf.getIpv4Unicast().getAggregateNetworks().entrySet().stream()
              .filter(e -> e.getValue().isSummaryOnly())
              .map(Entry::getKey);
      Optional.ofNullable(suppressSummarizedPrefixes(c, vrfName, summarizedPrefixes))
          .ifPresent(statements::add);
    }

    // 2. Setup export conditions, export if match, otherwise fall through
    Disjunction exportConditions = new Disjunction();

    // 2a. add export conditions for non-aggregate routes
    exportConditions.getDisjuncts().addAll(getBgpExportConditions(bgpVrf, routeMaps));

    // 2b. add export conditions for aggregate routes
    if (bgpVrf.getIpv4Unicast() != null) {
      exportConditions
          .getDisjuncts()
          .add(generateExportAggregateConditions(bgpVrf.getIpv4Unicast().getAggregateNetworks()));
    }

    statements.add(
        new If(
            exportConditions,
            ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
            ImmutableList.of(Statements.ReturnFalse.toStaticStatement())));

    bgpCommonExportPolicy.setStatements(statements);
  }

  private static List<BooleanExpr> getBgpExportConditions(
      BgpVrf bgpVrf, Map<String, RouteMap> routeMaps) {
    List<BooleanExpr> exportConditions = new ArrayList<>();

    // Always export BGP and iBGP routes
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP, RoutingProtocol.IBGP));

    // we don't need to process redist policies and network statements (inside v4 address family or
    // bgp-vrf-level) if v4 address family is not active
    if (!bgpVrf.isIpv4UnicastActive()) {
      return exportConditions;
    }

    BgpIpv4UnicastAddressFamily bgpIpv4UnicastAddressFamily =
        firstNonNull(bgpVrf.getIpv4Unicast(), new BgpIpv4UnicastAddressFamily());

    // Add conditions to redistribute other protocols
    Stream.concat(
            bgpVrf.getRedistributionPolicies().values().stream(),
            bgpIpv4UnicastAddressFamily.getRedistributionPolicies().values().stream())
        .forEach(
            redistributeProtocolPolicy -> {

              // Get a match expression for the protocol to be redistributed
              CumulusRoutingProtocol protocol = redistributeProtocolPolicy.getProtocol();
              MatchProtocol matchProtocol = new MatchProtocol(VI_PROTOCOLS_MAP.get(protocol));

              // Create a WithEnvironmentExpr with the redistribution route-map, if one is defined
              BooleanExpr weInterior = BooleanExprs.TRUE;
              String mapName = redistributeProtocolPolicy.getRouteMap();
              if (mapName != null && routeMaps.containsKey(mapName)) {
                weInterior = new CallExpr(mapName);
              }
              BooleanExpr we =
                  bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);

              // Export routes that match the protocol and WithEnvironmentExpr
              Conjunction exportProtocolConditions =
                  new Conjunction(ImmutableList.of(matchProtocol, we));
              exportProtocolConditions.setComment(
                  String.format("Redistribute %s routes into BGP", protocol));
              exportConditions.add(exportProtocolConditions);
            });

    // create origination prefilter from listed advertised networks
    Iterables.concat(
            bgpVrf.getNetworks().values(), bgpIpv4UnicastAddressFamily.getNetworks().values())
        .forEach(
            network -> {
              @Nullable String routeMapName = network.getRouteMap();
              Conjunction exportNetworkConditions =
                  new Conjunction(
                      ImmutableList.of(
                          /* Match network prefix */
                          new MatchPrefixSet(
                              DestinationNetwork.instance(),
                              new ExplicitPrefixSet(
                                  new PrefixSpace(PrefixRange.fromPrefix(network.getNetwork())))),
                          /*
                          Don't need to explicitly exclude BGP and iBGP routes here because those routes will
                          already be matched earlier in exportConditions (which are disjuncts).
                           */
                          new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)),
                          bgpRedistributeWithEnvironmentExpr(
                              routeMapName != null && routeMaps.containsKey(routeMapName)
                                  ? new CallExpr(routeMapName)
                                  : BooleanExprs.TRUE,
                              OriginType.IGP)));
              exportConditions.add(exportNetworkConditions);
            });
    return exportConditions;
  }

  /** Returns whether we originate default toward this neighbor */
  private static boolean bgpDefaultOriginate(BgpNeighbor neighbor) {
    return neighbor.getIpv4UnicastAddressFamily() != null
        && Boolean.TRUE.equals(neighbor.getIpv4UnicastAddressFamily().getDefaultOriginate());
  }

  @Nullable
  private static EvpnAddressFamily toEvpnAddressFamily(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      BgpNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc,
      RoutingPolicy routingPolicy,
      Warnings w) {
    BgpL2vpnEvpnAddressFamily evpnConfig = bgpVrf.getL2VpnEvpn();
    // sadly, we allow localAs == null in VI datamodel
    if (evpnConfig == null
        || localAs == null
        || neighbor.getL2vpnEvpnAddressFamily() == null
        // l2vpn evpn AF must be explicitly activated for neighbor
        || !firstNonNull(neighbor.getL2vpnEvpnAddressFamily().getActivated(), Boolean.FALSE)) {
      return null;
    }
    ImmutableSet.Builder<Layer2VniConfig> l2Vnis = ImmutableSet.builder();
    ImmutableSet.Builder<Layer3VniConfig> l3Vnis = ImmutableSet.builder();
    ImmutableMap.Builder<Integer, Integer> vniToIndexBuilder = ImmutableMap.builder();
    CommonUtil.forEachWithIndex(
        // Keep indices in deterministic order
        ImmutableList.sortedCopyOf(
            Comparator.nullsLast(Comparator.naturalOrder()),
            vsConfig.getVxlans().values().stream()
                .map(Vxlan::getId)
                .collect(ImmutableSet.toImmutableSet())),
        (index, vni) -> {
          if (vni == null) {
            return;
          }
          vniToIndexBuilder.put(vni, index);
        });
    Map<Integer, Integer> vniToIndex = vniToIndexBuilder.build();

    if (evpnConfig.getAdvertiseAllVni()) {
      c.getVrfs()
          .values()
          .forEach(
              vrf ->
                  vrf.getLayer2Vnis()
                      .values()
                      .forEach(
                          vxlan -> {
                            RouteDistinguisher rd =
                                RouteDistinguisher.from(
                                    newProc.getRouterId(), vniToIndex.get(vxlan.getVni()));
                            ExtendedCommunity rt = toRouteTarget(localAs, vxlan.getVni());
                            // Advertise L2 VNIs
                            l2Vnis.add(
                                Layer2VniConfig.builder()
                                    .setVni(vxlan.getVni())
                                    .setVrf(vrf.getName())
                                    .setRouteDistinguisher(rd)
                                    .setRouteTarget(rt)
                                    .build());
                          }));
    }
    BgpProcess bgpProcess = vsConfig.getBgpProcess();
    // Advertise the L3 VNI per vrf if one is configured
    assert bgpProcess != null; // Since we are in neighbor conversion, this must be true
    // Iterate over ALL vrfs, because even if the vrf doesn't appear in bgp process config, we
    // must be aware of the fact that it has a VNI and advertise it.
    c.getVrfs()
        .values()
        .forEach(
            innerVrf -> {
              String innerVrfName = innerVrf.getName();
              Vrf vsVrf = vsConfig.getVrf(innerVrfName);
              if (vsVrf == null) {
                return;
              }
              Integer l3Vni = vsVrf.getVni();
              if (l3Vni == null) {
                return;
              }
              if (!vniToIndex.containsKey(l3Vni)) {
                w.redFlag(String.format("vni %s for vrf %s does not exist", l3Vni, innerVrfName));
                return;
              }
              RouteDistinguisher rd =
                  RouteDistinguisher.from(
                      Optional.ofNullable(c.getVrfs().get(innerVrfName).getBgpProcess())
                          .map(org.batfish.datamodel.BgpProcess::getRouterId)
                          .orElse(bgpVrf.getRouterId()),
                      vniToIndex.get(l3Vni));
              ExtendedCommunity rt = toRouteTarget(localAs, l3Vni);
              // Grab the BgpVrf for the innerVrf, if it exists
              @Nullable
              BgpVrf innerBgpVrf =
                  (innerVrfName.equals(DEFAULT_VRF_NAME)
                      ? bgpProcess.getDefaultVrf()
                      : bgpProcess.getVrfs().get(innerVrfName));
              l3Vnis.add(
                  Layer3VniConfig.builder()
                      .setVni(l3Vni)
                      .setVrf(innerVrfName)
                      .setRouteDistinguisher(rd)
                      .setRouteTarget(rt)
                      .setImportRouteTarget(importRtPatternForAnyAs(l3Vni))
                      .setAdvertiseV4Unicast(
                          Optional.ofNullable(innerBgpVrf)
                              .map(BgpVrf::getL2VpnEvpn)
                              .map(BgpL2vpnEvpnAddressFamily::getAdvertiseIpv4Unicast)
                              .isPresent())
                      .build());
            });

    return EvpnAddressFamily.builder()
        .setL2Vnis(l2Vnis.build())
        .setL3Vnis(l3Vnis.build())
        .setPropagateUnmatched(true)
        .setAddressFamilyCapabilities(
            AddressFamilyCapabilities.builder()
                .setSendCommunity(true)
                .setSendExtendedCommunity(true)
                .setAllowRemoteAsOut(ALWAYS) // no outgoing remote-as check on Cumulus
                .build())
        .setRouteReflectorClient(
            firstNonNull(
                neighbor.getL2vpnEvpnAddressFamily().getRouteReflectorClient(), Boolean.FALSE))
        .setExportPolicy(routingPolicy.getName())
        .build();
  }

  /**
   * Convert AS number and VXLAN ID to an extended route target community. If the AS number is a
   * 4-byte as, only the lower 2 bytes are used.
   *
   * <p>See <a
   * href="https://docs.cumulusnetworks.com/display/DOCS/Ethernet+Virtual+Private+Network+-+EVPN#EthernetVirtualPrivateNetwork-EVPN-RD-auto-derivationAuto-derivationofRDsandRTs">
   * cumulus documentation</a> for detailed explanation.
   */
  @Nonnull
  @VisibleForTesting
  static ExtendedCommunity toRouteTarget(long asn, long vxlanId) {
    return ExtendedCommunity.target(asn & 0xFFFFL, vxlanId);
  }

  static boolean isUsedForBgpUnnumbered(
      @Nonnull String ifaceName, @Nullable BgpProcess bgpProcess) {
    return bgpProcess != null
        && Stream.concat(
                Stream.of(bgpProcess.getDefaultVrf()), bgpProcess.getVrfs().values().stream())
            .flatMap(vrf -> vrf.getNeighbors().keySet().stream())
            .anyMatch(Predicate.isEqual(ifaceName));
  }

  static void convertOspfProcess(
      Configuration c, CumulusConcatenatedConfiguration vsConfig, Warnings w) {
    @Nullable OspfProcess ospfProcess = vsConfig.getOspfProcess();
    if (ospfProcess == null) {
      return;
    }

    convertOspfVrf(c, vsConfig, ospfProcess.getDefaultVrf(), c.getDefaultVrf(), w);

    ospfProcess
        .getVrfs()
        .values()
        .forEach(
            ospfVrf -> {
              org.batfish.datamodel.Vrf vrf = c.getVrfs().get(ospfVrf.getVrfName());

              if (vrf == null) {
                w.redFlag(String.format("Vrf %s is not found.", ospfVrf.getVrfName()));
                return;
              }

              convertOspfVrf(c, vsConfig, ospfVrf, vrf, w);
            });
  }

  private static void convertOspfVrf(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      OspfVrf ospfVrf,
      org.batfish.datamodel.Vrf vrf,
      Warnings w) {
    org.batfish.datamodel.ospf.OspfProcess ospfProcess =
        toOspfProcess(c, vsConfig, ospfVrf, c.getAllInterfaces(vrf.getName()), w);
    vrf.addOspfProcess(ospfProcess);
  }

  @VisibleForTesting
  static org.batfish.datamodel.ospf.OspfProcess toOspfProcess(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      OspfVrf ospfVrf,
      Map<String, org.batfish.datamodel.Interface> vrfInterfaces,
      Warnings w) {
    Ip routerId = ospfVrf.getRouterId();
    if (routerId == null) {
      routerId = inferRouterId(c);
    }

    org.batfish.datamodel.ospf.OspfProcess.Builder builder =
        org.batfish.datamodel.ospf.OspfProcess.builder();

    org.batfish.datamodel.ospf.OspfProcess proc =
        builder
            .setRouterId(routerId)
            .setProcessId(DEFAULT_OSPF_PROCESS_NAME)
            .setReferenceBandwidth(OspfProcess.DEFAULT_REFERENCE_BANDWIDTH)
            .build();

    // Configure any interfaces grabbed by the network statement
    for (OspfNetworkArea na : vsConfig.getOspfProcess().getNetworkAreas().values()) {
      Prefix network = na.getPrefix();
      long area = na.getArea();
      for (Interface i : vrfInterfaces.values()) {
        if (i.getAllConcreteAddresses().stream()
            .noneMatch(ip -> network.containsPrefix(ip.getPrefix()))) {
          // Skip i unless it has a concrete address that is fully contained in the OSPF network.
          continue;
        }

        // Make sure it has OSPF Interface settings, creating one if not.
        FrrInterface iface = vsConfig.getFrrConfiguration().getInterfaces().get(i.getName());
        checkState(iface != null, "Internal error: interface in VI but not in FRR");
        OspfInterface ospfInterface = iface.getOrCreateOspf();

        // If area was not already set at the interface level, set it here.
        if (ospfInterface.getOspfArea() == null) {
          ospfInterface.setOspfArea(area);
        }
      }
    }

    addOspfInterfaces(vsConfig, vrfInterfaces, proc.getProcessId(), w);
    proc.setAreas(computeOspfAreas(c, vsConfig, ospfVrf, vrfInterfaces.keySet()));

    // Handle Max Metric Router LSA
    if (firstNonNull(vsConfig.getOspfProcess().getMaxMetricRouterLsa(), Boolean.FALSE)) {
      proc.setMaxMetricTransitLinks(DEFAULT_OSPF_MAX_METRIC);
    }

    // Handle Redistribution
    String ospfExportPolicyName = computeOspfExportPolicyName(ospfVrf.getVrfName());
    RoutingPolicy ospfExportPolicy = new RoutingPolicy(ospfExportPolicyName, c);
    c.getRoutingPolicies().put(ospfExportPolicyName, ospfExportPolicy);
    List<Statement> ospfExportStatements = ospfExportPolicy.getStatements();
    proc.setExportPolicy(ospfExportPolicyName);

    // In FRR, default metric-type is E2 and metric value is 20.
    ospfExportStatements.add(new SetOspfMetricType(DEFAULT_REDISTRIBUTE_METRIC_TYPE));
    ospfExportStatements.add(new SetMetric(new LiteralLong(DEFAULT_REDISTRIBUTE_METRIC)));

    ospfExportStatements.addAll(
        vsConfig.getOspfProcess().getRedistributionPolicies().values().stream()
            .map(policy -> convertOspfRedistributionPolicy(policy, vsConfig.getRouteMaps()))
            .collect(Collectors.toList()));

    return proc;
  }

  @VisibleForTesting
  @Nonnull
  static If convertOspfRedistributionPolicy(
      RedistributionPolicy policy, Map<String, RouteMap> routeMaps) {
    CumulusRoutingProtocol protocol = policy.getCumulusRoutingProtocol();

    // All redistribution must match the specified protocol.
    Conjunction ospfExportConditions = new Conjunction();
    ospfExportConditions.getConjuncts().add(new MatchProtocol(VI_PROTOCOLS_MAP.get(protocol)));
    ImmutableList.Builder<Statement> ospfExportStatements = ImmutableList.builder();

    // If a route-map filter is present, honor it.
    String exportRouteMapName = policy.getRouteMap();
    if (exportRouteMapName != null) {
      RouteMap exportRouteMap = routeMaps.get(exportRouteMapName);
      if (exportRouteMap != null) {
        ospfExportConditions.getConjuncts().add(new CallExpr(exportRouteMapName));
      }
    }

    ospfExportStatements.add(Statements.ExitAccept.toStaticStatement());

    // Construct the policy and add it before returning.
    return new If(
        "OSPF export routes for " + protocol.name(),
        ospfExportConditions,
        ospfExportStatements.build(),
        ImmutableList.of());
  }

  /**
   * Logic of inferring router ID for Zebra based system
   * (https://github.com/coreswitch/zebra/blob/master/docs/router-id.md):
   *
   * <p>If the loopback is configured with an IP address NOT in 127.0.0.0/8, the numerically largest
   * such IP is used. Otherwise, the numerically largest IP configured on any interface on the
   * device is used. Otherwise, 0.0.0.0 is used.
   */
  @VisibleForTesting
  static Ip inferRouterId(Configuration c) {
    if (c.getAllInterfaces().containsKey(LOOPBACK_INTERFACE_NAME)) {
      Optional<ConcreteInterfaceAddress> maxLoIp =
          c.getAllInterfaces().get(LOOPBACK_INTERFACE_NAME).getAllConcreteAddresses().stream()
              .filter(addr -> !LOOPBACK_PREFIX.containsIp(addr.getIp()))
              .max(ConcreteInterfaceAddress::compareTo);
      if (maxLoIp.isPresent()) {
        return maxLoIp.get().getIp();
      }
    }

    Optional<ConcreteInterfaceAddress> biggestInterfaceIp =
        c.getAllInterfaces().values().stream()
            .filter(iface -> !iface.getName().equals(LOOPBACK_INTERFACE_NAME))
            .flatMap(iface -> iface.getAllConcreteAddresses().stream())
            .max(InterfaceAddress::compareTo);

    return biggestInterfaceIp
        .map(ConcreteInterfaceAddress::getIp)
        .orElseGet(() -> Ip.parse("0.0.0.0"));
  }

  /**
   * REF:
   * https://github.com/FRRouting/frr/blob/b4b1d1ebdbee99664c0607cf4abac977dfc896b6/bgpd/bgp_attr.c#L3851
   * If FRR has a cluster-id set it will use that, otherwise it will use router-id.
   *
   * @param bgpVrf BGP vrf for which to infer cluster ID
   * @param routerId router ID of the {@code bgpVrf} (already inferred if needed)
   */
  @VisibleForTesting
  @Nullable
  static Long inferClusterId(
      final BgpVrf bgpVrf, final Ip routerId, final BgpNeighbor neighbor, @Nullable Long localAs) {
    assert neighbor.getRemoteAs() != null; // precondition
    // Do not set cluster Id if peer is eBGP
    if (neighbor.getRemoteAs().isKnownEbgp(localAs)) {
      return null;
    }
    // Return clusterId if set in the config, otherwise return routerId as default.
    return bgpVrf.getClusterId() != null ? bgpVrf.getClusterId().asLong() : routerId.asLong();
  }

  @VisibleForTesting
  static void addOspfInterfaces(
      CumulusConcatenatedConfiguration vsConfig,
      Map<String, org.batfish.datamodel.Interface> viIfaces,
      String processId,
      Warnings w) {
    viIfaces.forEach(
        (ifaceName, iface) -> {
          Optional<OspfInterface> ospfOpt = vsConfig.getOspfInterface(ifaceName);
          if (!ospfOpt.isPresent() || ospfOpt.get().getOspfArea() == null) {
            return;
          }

          OspfInterface ospfInterface = ospfOpt.get();
          @Nullable Integer ospfCost = ospfInterface.getCost();
          if (iface.getInterfaceType() == InterfaceType.LOOPBACK) {
            // FRR does not include loopback cost, even if explicitly configured, in the LSA.
            // See: https://github.com/FRRouting/frr/issues/2922
            ospfCost = 0;
          }
          iface.setOspfSettings(
              OspfInterfaceSettings.builder()
                  .setPassive(
                      Optional.ofNullable(ospfInterface.getPassive())
                          .orElse(vsConfig.getOspfProcess().getDefaultPassiveInterface()))
                  .setAreaName(ospfInterface.getOspfArea())
                  .setNetworkType(toOspfNetworkType(ospfInterface.getNetwork(), w))
                  .setDeadInterval(
                      Optional.ofNullable(ospfInterface.getDeadInterval())
                          .orElse(DEFAULT_OSPF_DEAD_INTERVAL))
                  .setHelloInterval(
                      Optional.ofNullable(ospfInterface.getHelloInterval())
                          .orElse(DEFAULT_OSPF_HELLO_INTERVAL))
                  .setProcess(processId)
                  .setCost(ospfCost)
                  .setOspfAddresses(getOspfAddresses(vsConfig, ifaceName))
                  .build());
        });
  }

  private static @Nonnull OspfAddresses getOspfAddresses(
      CumulusConcatenatedConfiguration vsConfig, String ifaceName) {
    // use ImmutableSet to preserve order and remove duplicates
    ImmutableSet.Builder<ConcreteInterfaceAddress> addressesBuilder = ImmutableSet.builder();
    Optional.ofNullable(vsConfig.getInterfacesConfiguration().getInterfaces().get(ifaceName))
        .map(InterfacesInterface::getAddresses)
        .ifPresent(addressesBuilder::addAll);
    Optional.ofNullable(vsConfig.getFrrConfiguration().getInterfaces().get(ifaceName))
        .map(FrrInterface::getIpAddresses)
        .ifPresent(addressesBuilder::addAll);
    return OspfAddresses.of(addressesBuilder.build());
  }

  @VisibleForTesting
  static SortedMap<Long, OspfArea> computeOspfAreas(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      OspfVrf ospfVrf,
      Collection<String> vrfIfaceNames) {
    Map<Long, List<String>> areaInterfaces =
        vrfIfaceNames.stream()
            .filter(
                iface -> {
                  Optional<OspfInterface> ospfOpt = vsConfig.getOspfInterface(iface);
                  return ospfOpt.isPresent() && ospfOpt.get().getOspfArea() != null;
                })
            .collect(
                groupingBy(
                    iface -> vsConfig.getOspfInterface(iface).get().getOspfArea(),
                    mapping(Function.identity(), Collectors.toList())));

    // Ensure that the VRF-level OSPF config has each area used.
    areaInterfaces.keySet().forEach(ospfVrf::getOrCreateArea);

    // Now convert each area
    ImmutableSortedMap.Builder<Long, OspfArea> areas = ImmutableSortedMap.naturalOrder();
    ospfVrf
        .getAreas()
        .values()
        .forEach(
            vrfArea -> {
              long num = vrfArea.getArea();
              List<String> interfaces = areaInterfaces.get(num);
              if (interfaces.isEmpty()) {
                // No interfaces, skip area.
                return;
              }
              OspfArea area = convertArea(c, ospfVrf.getVrfName(), vrfArea, interfaces);
              areas.put(area.getAreaNumber(), area);
            });
    return areas.build();
  }

  private static @Nonnull OspfArea convertArea(
      Configuration c,
      String vrfName,
      org.batfish.representation.cumulus.OspfArea vsArea,
      Collection<String> interfaces) {
    OspfArea.Builder ret = OspfArea.builder().setNumber(vsArea.getArea()).addInterfaces(interfaces);

    // Handle OSPF Area Range conversion into VI OspfAreaSummary. It has two steps:
    // 1. convert OspfAreaRange to OspfAreaSummary
    // 2. add a RouteFilterList to force summarization and block more specific routes.
    if (!vsArea.getRanges().isEmpty()) {
      ImmutableList.Builder<RouteFilterLine> lines =
          ImmutableList.builderWithExpectedSize(vsArea.getRanges().size() + 1);
      for (OspfAreaRange range : vsArea.getRanges().values()) {
        // OSPF costs are only 16-bit, but the VI metric is a long, for other protocols that support
        // that. Upconvert if not null.
        Prefix prefix = range.getRange();
        Long cost = range.getCost() == null ? null : range.getCost().longValue();
        OspfAreaSummary summary =
            new OspfAreaSummary(SummaryRouteBehavior.ADVERTISE_AND_INSTALL_DISCARD, cost);
        ret.addSummary(prefix, summary);
        lines.add(new RouteFilterLine(LineAction.DENY, PrefixRange.moreSpecificThan(prefix)));
      }
      // Anything not summarized is permitted.
      lines.add(new RouteFilterLine(LineAction.PERMIT, PrefixRange.ALL));
      RouteFilterList summaryFilter =
          new RouteFilterList(
              computeOspfAreaRangeFilterName(vrfName, vsArea.getArea()), lines.build());
      c.getRouteFilterLists().put(summaryFilter.getName(), summaryFilter);
      ret.setSummaryFilter(summaryFilter.getName());
    }

    return ret.build();
  }

  @Nullable
  private static org.batfish.datamodel.ospf.OspfNetworkType toOspfNetworkType(
      @Nullable OspfNetworkType type, Warnings w) {
    if (type == null) {
      return null;
    }
    switch (type) {
      case BROADCAST:
        return org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
      case POINT_TO_POINT:
        return org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
      default:
        w.redFlag(
            String.format(
                "Conversion of Cumulus FRR OSPF network type '%s' is not handled.",
                type.toString()));
        return null;
    }
  }

  static void convertIpCommunityLists(
      Configuration c, Map<String, IpCommunityList> ipCommunityLists) {
    // create CommunitySetMatchExpr for route-map match community
    ipCommunityLists.forEach(
        (name, list) -> {
          if (list instanceof IpCommunityListStandard) {
            c.getCommunitySetMatchExprs()
                .put(name, toCommunitySetMatchExpr((IpCommunityListStandard) list));
          } else if (list instanceof IpCommunityListExpanded) {
            c.getCommunitySetMatchExprs()
                .put(name, toCommunitySetMatchExpr((IpCommunityListExpanded) list));
          }
        });

    // The following code-block is only for "set comm-list delete" statements.
    ipCommunityLists.forEach(
        (name, list) -> {
          if (list instanceof IpCommunityListStandard) {
            c.getCommunityMatchExprs()
                .put(name, toCommunityMatchExpr((IpCommunityListStandard) list));
          } else if (list instanceof IpCommunityListExpanded) {
            c.getCommunityMatchExprs()
                .put(name, toCommunityMatchExpr((IpCommunityListExpanded) list));
          }
        });
  }

  @VisibleForTesting
  static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      IpCommunityListStandard ipCommunityListStandard) {
    Set<Community> whitelist = new HashSet<>();
    Set<Community> blacklist = new HashSet<>();

    // TODO: support set comm-list delete semantics for line with more than one community
    for (IpCommunityListStandardLine line : ipCommunityListStandard.getLines()) {
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

  @VisibleForTesting
  static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      IpCommunityListExpanded ipCommunityListExpanded) {
    return new CommunityAcl(
        ipCommunityListExpanded.getLines().stream()
            .map(CumulusConversions::toCommunityAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunitySetMatchExpr toCommunitySetMatchExpr(
      IpCommunityListStandard ipCommunityListStandard) {
    return new CommunitySetAcl(
        ipCommunityListStandard.getLines().stream()
            .map(CumulusConversions::toCommunitySetAclLine)
            .collect(ImmutableList.toImmutableList()));
  }

  private static @Nonnull CommunitySetMatchExpr toCommunitySetMatchExpr(
      IpCommunityListExpanded ipCommunityListExpanded) {
    return new CommunitySetAcl(
        ipCommunityListExpanded.getLines().stream()
            .map(CumulusConversions::toCommunitySetAclLine)
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

  private static @Nonnull CommunitySetAclLine toCommunitySetAclLine(
      IpCommunityListExpandedLine line) {
    return new CommunitySetAclLine(
        line.getAction(),
        new CommunitySetMatchRegex(
            new TypesFirstAscendingSpaceSeparated(ColonSeparatedRendering.instance()),
            toJavaRegex(line.getRegex())));
  }

  private static @Nonnull CommunityAclLine toCommunityAclLine(IpCommunityListExpandedLine line) {
    return new CommunityAclLine(
        line.getAction(),
        new CommunityMatchRegex(ColonSeparatedRendering.instance(), toJavaRegex(line.getRegex())));
  }

  private static @Nonnull String toJavaRegex(String cumulusRegex) {
    String withoutQuotes;
    if (cumulusRegex.charAt(0) == '"' && cumulusRegex.charAt(cumulusRegex.length() - 1) == '"') {
      withoutQuotes = cumulusRegex.substring(1, cumulusRegex.length() - 1);
    } else {
      withoutQuotes = cumulusRegex;
    }
    String underscoreReplacement = "(,|\\\\{|\\\\}|^|\\$| )";
    String output = withoutQuotes.replaceAll("_", underscoreReplacement);
    return output;
  }

  static void convertIpAsPathAccessLists(
      Configuration c, Map<String, IpAsPathAccessList> ipAsPathAccessLists) {
    ipAsPathAccessLists.forEach(
        (name, asPathAccessList) ->
            c.getAsPathAccessLists().put(name, toAsPathAccessList(asPathAccessList)));
  }

  @VisibleForTesting
  static @Nonnull AsPathAccessList toAsPathAccessList(IpAsPathAccessList asPathAccessList) {
    String name = asPathAccessList.getName();
    List<AsPathAccessListLine> lines =
        asPathAccessList.getLines().stream()
            // TODO Check FRR AS path match semantics.
            // This regex assumes we should match any path containing the specified ASN anywhere.
            .map(
                line ->
                    new AsPathAccessListLine(
                        line.getAction(), String.format("(^| )%s($| )", line.getAsNum())))
            .collect(ImmutableList.toImmutableList());
    return new AsPathAccessList(name, lines);
  }

  static void convertIpPrefixLists(
      Configuration c, Map<String, IpPrefixList> ipPrefixLists, String vendorConfigFilename) {
    ipPrefixLists.forEach(
        (name, ipPrefixList) ->
            c.getRouteFilterLists()
                .put(name, toRouteFilterList(ipPrefixList, vendorConfigFilename)));
  }

  @VisibleForTesting
  static @Nonnull RouteFilterList toRouteFilterList(
      IpPrefixList ipPrefixList, String vendorConfigFilename) {
    List<RouteFilterLine> lines =
        ipPrefixList.getLines().values().stream()
            .map(CumulusConversions::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList());
    return new RouteFilterList(
        ipPrefixList.getName(),
        lines,
        new VendorStructureId(
            vendorConfigFilename,
            CumulusStructureType.IP_PREFIX_LIST.getDescription(),
            ipPrefixList.getName()));
  }

  @VisibleForTesting
  static @Nonnull RouteFilterLine toRouteFilterLine(IpPrefixListLine ipPrefixListLine) {
    return new RouteFilterLine(
        ipPrefixListLine.getAction(),
        ipPrefixListLine.getPrefix(),
        ipPrefixListLine.getLengthRange());
  }

  static void convertRouteMaps(
      Configuration c,
      CumulusConcatenatedConfiguration vc,
      Map<String, RouteMap> routeMaps,
      Warnings w) {
    routeMaps.forEach((name, routeMap) -> new RouteMapConvertor(c, vc, routeMap, w).toRouteMap());
  }

  static void convertDnsServers(Configuration c, List<Ip> ipv4Nameservers) {
    c.setDnsServers(
        ipv4Nameservers.stream()
            .map(Object::toString)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  static void convertClags(Configuration c, CumulusConcatenatedConfiguration vsConfig, Warnings w) {
    Map<String, InterfaceClagSettings> clagSourceInterfaces = vsConfig.getClagSettings();
    if (clagSourceInterfaces.isEmpty()) {
      return;
    }
    if (clagSourceInterfaces.size() > 1) {
      w.redFlag(
          String.format(
              "CLAG configuration on multiple peering interfaces is unsupported: %s",
              clagSourceInterfaces.keySet()));
      return;
    }
    // Interface clagSourceInterface = clagSourceInterfaces.get(0);
    Entry<String, InterfaceClagSettings> entry = clagSourceInterfaces.entrySet().iterator().next();
    String sourceInterfaceName = entry.getKey();
    InterfaceClagSettings clagSettings = entry.getValue();
    Ip peerAddress = clagSettings.getPeerIp();
    // Special case link-local addresses when no other addresses are defined
    org.batfish.datamodel.Interface viInterface = c.getAllInterfaces().get(sourceInterfaceName);
    if (peerAddress == null
        && clagSettings.isPeerIpLinkLocal()
        && viInterface.getAllAddresses().isEmpty()) {
      LinkLocalAddress lla = LinkLocalAddress.of(CLAG_LINK_LOCAL_IP);
      viInterface.setAddress(lla);
      viInterface.setAllAddresses(ImmutableSet.of(lla));
    }
    String peerInterfaceName = getSuperInterfaceName(sourceInterfaceName);
    c.setMlags(
        ImmutableMap.of(
            CUMULUS_CLAG_DOMAIN_ID,
            Mlag.builder()
                .setId(CUMULUS_CLAG_DOMAIN_ID)
                .setLocalInterface(sourceInterfaceName)
                .setPeerAddress(peerAddress)
                .setPeerInterface(peerInterfaceName)
                .build()));
  }

  /**
   * Converts {@link Vxlan} into appropriate {@link Vni} for each VRF. Requires VI Vrfs to already
   * be properly initialized
   */
  static void convertVxlans(
      Configuration c,
      CumulusConcatenatedConfiguration vsConfig,
      Map<Integer, String> vniToVrf,
      @Nullable Ip loopbackClagVxlanAnycastIp,
      @Nullable Ip loopbackVxlanLocalTunnelIp,
      Warnings w) {

    // Put all valid VXLAN VNIs into appropriate VRF
    vsConfig
        .getVxlans()
        .values()
        .forEach(
            vxlan -> {
              if (vxlan.getId() == null || vxlan.getBridgeAccessVlan() == null) {
                // Not a valid VNI configuration
                w.redFlag(
                    String.format(
                        "Vxlan %s is not configured properly: %s is not defined",
                        vxlan.getName(),
                        vxlan.getId() == null ? "vxlan id" : "bridge access vlan"));
                return;
              }
              // Cumulus documents complex conditions for when clag-anycast address is a valid
              // source:
              // https://docs.cumulusnetworks.com/cumulus-linux/Network-Virtualization/VXLAN-Active-Active-Mode/#active-active-vtep-anycast-ip-behavior
              // In testing, we couldn't reproduce that behavior and the address was always active.
              // We go with that assumption here until we can reproduce the exact behavior.
              Ip localIp =
                  Stream.of(
                          loopbackClagVxlanAnycastIp,
                          vxlan.getLocalTunnelip(),
                          loopbackVxlanLocalTunnelIp)
                      .filter(Objects::nonNull)
                      .findFirst()
                      .orElse(null);
              if (localIp == null) {
                w.redFlag(
                    String.format(
                        "Local tunnel IP for vxlan %s is not configured", vxlan.getName()));
                return;
              }
              @Nullable String vrfName = vniToVrf.get(vxlan.getId());
              if (vrfName != null) {
                // This is an L3 VNI.
                Optional.ofNullable(c.getVrfs().get(vrfName))
                    .ifPresent(
                        vrf ->
                            vrf.addLayer3Vni(
                                Layer3Vni.builder()
                                    .setVni(vxlan.getId())
                                    .setSourceAddress(localIp)
                                    .setUdpPort(NamedPort.VXLAN.number())
                                    .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                                    .setSrcVrf(DEFAULT_VRF_NAME)
                                    .build()));
              } else {
                // This is an L2 VNI. Find the VRF by looking up the VLAN
                vrfName = vsConfig.getVrfForVlan(vxlan.getBridgeAccessVlan());
                if (vrfName == null) {
                  // This is a workaround until we properly support pure-L2 VNIs (with no IRBs)
                  vrfName = DEFAULT_VRF_NAME;
                }
                Optional.ofNullable(c.getVrfs().get(vrfName))
                    .ifPresent(
                        vrf ->
                            vrf.addLayer2Vni(
                                Layer2Vni.builder()
                                    .setVni(vxlan.getId())
                                    .setVlan(vxlan.getBridgeAccessVlan())
                                    .setSourceAddress(localIp)
                                    .setUdpPort(NamedPort.VXLAN.number())
                                    .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                                    .setSrcVrf(DEFAULT_VRF_NAME)
                                    .build()));
              }
            });
  }
}
