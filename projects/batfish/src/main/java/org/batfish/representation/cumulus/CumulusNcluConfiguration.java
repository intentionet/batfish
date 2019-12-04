package org.batfish.representation.cumulus;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.PATH_LENGTH;
import static org.batfish.datamodel.bgp.VniConfig.importRtPatternForAnyAs;
import static org.batfish.representation.cumulus.BgpProcess.BGP_UNNUMBERED_IP;
import static org.batfish.representation.cumulus.CumulusConversions.generateExportAggregateConditions;
import static org.batfish.representation.cumulus.CumulusConversions.generateGeneratedRoutes;
import static org.batfish.representation.cumulus.CumulusConversions.suppressSummarizedPrefixes;
import static org.batfish.representation.cumulus.CumulusRoutingProtocol.VI_PROTOCOLS_MAP;
import static org.batfish.representation.cumulus.OspfInterface.DEFAULT_OSPF_DEAD_INTERVAL;
import static org.batfish.representation.cumulus.OspfInterface.DEFAULT_OSPF_HELLO_INTERVAL;
import static org.batfish.representation.cumulus.OspfProcess.DEFAULT_OSPF_PROCESS_NAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vendor_family.cumulus.CumulusFamily;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Vni;
import org.batfish.vendor.VendorConfiguration;

/** A {@link VendorConfiguration} for the Cumulus NCLU configuration language. */
public class CumulusNcluConfiguration extends VendorConfiguration {

  @VisibleForTesting public static final String CUMULUS_CLAG_DOMAIN_ID = "~CUMULUS_CLAG_DOMAIN~";

  /**
   * Bandwidth cannot be determined from name alone, so we choose the following made-up plausible
   * value in absence of explicit information.
   */
  private static final double DEFAULT_PORT_BANDWIDTH = 10E9D;

  public static final int DEFAULT_STATIC_ROUTE_ADMINISTRATIVE_DISTANCE = 1;
  public static final int DEFAULT_STATIC_ROUTE_METRIC = 0;
  public static final String LOOPBACK_INTERFACE_NAME = "lo";

  private static final Ip CLAG_LINK_LOCAL_IP = Ip.parse("169.254.40.94");
  private static final Prefix LOOPBACK_PREFIX = Prefix.parse("127.0.0.0/8");
  /**
   * Conversion factor for interface speed units. In the config Mbps are used, VI model expects bps
   */
  private static final double SPEED_CONVERSION_FACTOR = 10e6;

  // Follow the default setting of Cisco.
  // TODO: need to verify this
  public static final double DEFAULT_LOOPBACK_BANDWIDTH = 8e9;

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

  public static @Nonnull String computeBgpCommonExportPolicyName(String vrfName) {
    return String.format("~BGP_COMMON_EXPORT_POLICY:%s~", vrfName);
  }

  @VisibleForTesting
  public static @Nonnull String computeBgpPeerExportPolicyName(
      String vrfName, String peerInterface) {
    return String.format("~BGP_PEER_EXPORT_POLICY:%s:%s~", vrfName, peerInterface);
  }

  public static @Nonnull String computeBgpPeerImportPolicyName(String vrf, String peer) {
    return String.format("~BGP_PEER_IMPORT_POLICY:%s:%s~", vrf, peer);
  }

  private @Nullable BgpProcess _bgpProcess;
  private @Nonnull Map<String, Bond> _bonds;
  private @Nonnull Bridge _bridge;
  private transient Configuration _c;
  private @Nullable String _hostname;
  private @Nullable OspfProcess _ospfProcess;
  private @Nonnull Map<String, Interface> _interfaces;
  private final @Nonnull List<Ip> _ipv4Nameservers;
  private final @Nonnull List<Ip6> _ipv6Nameservers;
  private final @Nonnull Loopback _loopback;
  private final @Nonnull Map<String, RouteMap> _routeMaps;
  private final @Nonnull Set<StaticRoute> _staticRoutes;
  private @Nonnull Map<String, Vlan> _vlans;
  private @Nonnull Map<String, Vrf> _vrfs;
  private @Nonnull Map<String, Vxlan> _vxlans;
  private final @Nonnull Map<String, IpAsPathAccessList> _ipAsPathAccessLists;
  private final @Nonnull Map<String, IpPrefixList> _ipPrefixLists;
  private final @Nonnull Map<String, IpCommunityList> _ipCommunityLists;

  @Nonnull
  private static final LinkLocalAddress LINK_LOCAL_ADDRESS = LinkLocalAddress.of(BGP_UNNUMBERED_IP);

  public CumulusNcluConfiguration() {
    _bonds = new HashMap<>();
    _bridge = new Bridge();
    _interfaces = new HashMap<>();
    _ipAsPathAccessLists = new HashMap<>();
    _ipPrefixLists = new HashMap<>();
    _ipCommunityLists = new HashMap<>();
    _ipv4Nameservers = new LinkedList<>();
    _ipv6Nameservers = new LinkedList<>();
    _loopback = new Loopback();
    _routeMaps = new HashMap<>();
    _staticRoutes = new HashSet<>();
    _vlans = new HashMap<>();
    _vrfs = new HashMap<>();
    _vxlans = new HashMap<>();
  }

  private void addInterfaceNeighbor(
      BgpInterfaceNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc) {
    if (neighbor.getRemoteAs() == null && neighbor.getRemoteAsType() == null) {
      getWarnings().redFlag("Skipping invalidly configured BGP peer " + neighbor.getName());
      return;
    }
    RoutingPolicy exportRoutingPolicy = computeBgpNeighborExportRoutingPolicy(neighbor, bgpVrf);
    @Nullable
    RoutingPolicy importRoutingPolicy = computeBgpNeighborImportRoutingPolicy(neighbor, bgpVrf, _c);
    generateBgpUnnumberedPeerConfig(
        neighbor, localAs, bgpVrf, newProc, exportRoutingPolicy, importRoutingPolicy);
  }

  @VisibleForTesting
  void generateBgpUnnumberedPeerConfig(
      BgpInterfaceNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc,
      RoutingPolicy exportRoutingPolicy,
      @Nullable RoutingPolicy importRoutingPolicy) {
    BgpUnnumberedPeerConfig.builder()
        .setBgpProcess(newProc)
        .setConfederation(bgpVrf.getConfederationId())
        .setDescription(neighbor.getDescription())
        .setGroup(neighbor.getPeerGroup())
        .setLocalAs(localAs)
        .setLocalIp(BGP_UNNUMBERED_IP)
        .setPeerInterface(neighbor.getName())
        .setRemoteAsns(computeRemoteAsns(neighbor, localAs))
        .setEbgpMultihop(neighbor.getEbgpMultihop() != null)
        // Ipv4 unicast is enabled by default
        .setIpv4UnicastAddressFamily(
            convertIpv4UnicastAddressFamily(
                neighbor.getIpv4UnicastAddressFamily(), exportRoutingPolicy, importRoutingPolicy))
        .setEvpnAddressFamily(
            toEvpnAddressFamily(neighbor, localAs, bgpVrf, newProc, exportRoutingPolicy))
        .build();
  }

  @VisibleForTesting
  @Nullable
  Ipv4UnicastAddressFamily convertIpv4UnicastAddressFamily(
      @Nullable BgpNeighborIpv4UnicastAddressFamily ipv4UnicastAddressFamily,
      RoutingPolicy exportRoutingPolicy,
      RoutingPolicy importRoutingPolicy) {

    // check if address family was explicitly deactivated (address family is activated by default)
    boolean deactivated =
        ipv4UnicastAddressFamily != null
            && Boolean.FALSE.equals(ipv4UnicastAddressFamily.getActivated());
    if (deactivated) {
      return null;
    }

    // According to the docs, the neighbor must have been explicitly activated for
    // route-reflector-client to take effect:
    // https://docs.cumulusnetworks.com/display/DOCS/Border+Gateway+Protocol+-+BGP#BorderGatewayProtocol-BGP-RouteReflectors
    //
    // The docs appear to be wrong: explicit activation is not enforced by either NCLU or FRR, and
    // we have tested that route reflection works without it.
    boolean routeReflectorClient =
        Optional.ofNullable(ipv4UnicastAddressFamily)
            .map(af -> Boolean.TRUE.equals(af.getRouteReflectorClient()))
            .orElse(false);

    return Ipv4UnicastAddressFamily.builder()
        .setAddressFamilyCapabilities(
            AddressFamilyCapabilities.builder()
                .setSendCommunity(true)
                .setSendExtendedCommunity(true)
                .build())
        .setExportPolicy(exportRoutingPolicy.getName())
        .setImportPolicy(importRoutingPolicy == null ? null : importRoutingPolicy.getName())
        .setRouteReflectorClient(routeReflectorClient)
        .build();
  }

  private void addIpv4BgpNeighbor(
      BgpIpNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc) {
    if (neighbor.getPeerIp() == null
        || (neighbor.getRemoteAs() == null && neighbor.getRemoteAsType() == null)) {
      getWarnings().redFlag("Skipping invalidly configured BGP peer " + neighbor.getName());
      return;
    }
    RoutingPolicy exportRoutingPolicy = computeBgpNeighborExportRoutingPolicy(neighbor, bgpVrf);
    @Nullable
    RoutingPolicy importRoutingPolicy = computeBgpNeighborImportRoutingPolicy(neighbor, bgpVrf, _c);
    generateBgpActivePeerConfig(
        neighbor, localAs, bgpVrf, newProc, exportRoutingPolicy, importRoutingPolicy, _c);
  }

  @VisibleForTesting
  void generateBgpActivePeerConfig(
      BgpIpNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc,
      RoutingPolicy exportRoutingPolicy,
      RoutingPolicy importRoutingPolicy,
      Configuration c) {
    BgpActivePeerConfig.builder()
        .setBgpProcess(newProc)
        .setConfederation(bgpVrf.getConfederationId())
        .setDescription(neighbor.getDescription())
        .setGroup(neighbor.getPeerGroup())
        .setLocalAs(localAs)
        .setLocalIp(
            Optional.ofNullable(
                    resolveLocalIpFromUpdateSource(neighbor.getBgpNeighborSource(), c, _w))
                .orElse(computeLocalIpForBgpNeighbor(neighbor.getPeerIp(), c, bgpVrf.getVrfName())))
        .setPeerAddress(neighbor.getPeerIp())
        .setRemoteAsns(computeRemoteAsns(neighbor, localAs))
        .setEbgpMultihop(neighbor.getEbgpMultihop() != null)
        // Ipv4 unicast is enabled by default
        .setIpv4UnicastAddressFamily(
            convertIpv4UnicastAddressFamily(
                neighbor.getIpv4UnicastAddressFamily(), exportRoutingPolicy, importRoutingPolicy))
        .setEvpnAddressFamily(
            toEvpnAddressFamily(neighbor, localAs, bgpVrf, newProc, exportRoutingPolicy))
        .build();
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

  @Nonnull
  private RoutingPolicy computeBgpNeighborExportRoutingPolicy(BgpNeighbor neighbor, BgpVrf bgpVrf) {
    String vrfName = bgpVrf.getVrfName();

    RoutingPolicy.Builder peerExportPolicy =
        RoutingPolicy.builder()
            .setOwner(_c)
            .setName(computeBgpPeerExportPolicyName(vrfName, neighbor.getName()));

    BooleanExpr peerExportConditions = computePeerExportConditions(neighbor, bgpVrf);
    List<Statement> acceptStmts = getAcceptStatements(neighbor, bgpVrf);

    peerExportPolicy.addStatement(
        new If(
            "peer-export policy main conditional: exitAccept if true / exitReject if false",
            peerExportConditions,
            acceptStmts,
            ImmutableList.of(Statements.ExitReject.toStaticStatement())));

    return peerExportPolicy.build();
  }

  @Nullable
  @VisibleForTesting
  static RoutingPolicy computeBgpNeighborImportRoutingPolicy(
      BgpNeighbor neighbor, BgpVrf bgpVrf, Configuration c) {
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

  private static List<Statement> getAcceptStatements(BgpNeighbor neighbor, BgpVrf bgpVrf) {
    SetNextHop setNextHop = getSetNextHop(neighbor, bgpVrf);
    return setNextHop == null
        ? ImmutableList.of(Statements.ExitAccept.toStaticStatement())
        : ImmutableList.of(setNextHop, Statements.ExitAccept.toStaticStatement());
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
  static @Nullable SetNextHop getSetNextHop(BgpNeighbor neighbor, BgpVrf bgpVrf) {
    if (neighbor.getRemoteAs() == null
        || bgpVrf.getAutonomousSystem() == null
        || !neighbor.getRemoteAs().equals(bgpVrf.getAutonomousSystem())) {
      return null;
    }

    boolean nextHopSelf =
        Optional.ofNullable(neighbor.getIpv4UnicastAddressFamily())
            .map(BgpNeighborIpv4UnicastAddressFamily::getNextHopSelf)
            .orElse(false);

    return nextHopSelf ? new SetNextHop(SelfNextHop.getInstance()) : null;
  }

  /** Scan all interfaces, find first that contains given remote IP */
  @Nullable
  @VisibleForTesting
  static Ip computeLocalIpForBgpNeighbor(Ip remoteIp, Configuration c, String vrfName) {
    org.batfish.datamodel.Vrf vrf = c.getVrfs().get(vrfName);
    if (vrf == null) {
      return null;
    }
    return vrf.getInterfaces().values().stream()
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

  @Nullable
  private EvpnAddressFamily toEvpnAddressFamily(
      BgpNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc,
      RoutingPolicy routingPolicy) {
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
            Comparator.nullsLast(Comparator.comparing(Vxlan::getId)), _vxlans.values()),
        (index, vxlan) -> {
          if (vxlan.getId() == null) {
            return;
          }
          vniToIndexBuilder.put(vxlan.getId(), index);
        });
    Map<Integer, Integer> vniToIndex = vniToIndexBuilder.build();

    if (evpnConfig.getAdvertiseAllVni()) {
      for (Layer2Vni vxlan : _c.getVrfs().get(bgpVrf.getVrfName()).getLayer2Vnis().values()) {
        RouteDistinguisher rd =
            RouteDistinguisher.from(newProc.getRouterId(), vniToIndex.get(vxlan.getVni()));
        ExtendedCommunity rt = toRouteTarget(localAs, vxlan.getVni());
        // Advertise L2 VNIs
        l2Vnis.add(
            Layer2VniConfig.builder()
                .setVni(vxlan.getVni())
                .setVrf(bgpVrf.getVrfName())
                .setRouteDistinguisher(rd)
                .setRouteTarget(rt)
                .build());
      }
    }
    // Advertise the L3 VNI per vrf if one is configured
    assert _bgpProcess != null; // Since we are in neighbor conversion, this must be true
    // Iterate over ALL vrfs, because even if the vrf doesn't appear in bgp process config, we
    // must be aware of the fact that it has a VNI and advertise it.
    _vrfs
        .values()
        .forEach(
            innerVrf -> {
              String innerVrfName = innerVrf.getName();
              Integer l3Vni = innerVrf.getVni();
              if (l3Vni == null) {
                return;
              }
              RouteDistinguisher rd =
                  RouteDistinguisher.from(
                      Optional.ofNullable(_c.getVrfs().get(innerVrfName).getBgpProcess())
                          .map(org.batfish.datamodel.BgpProcess::getRouterId)
                          .orElse(bgpVrf.getRouterId()),
                      vniToIndex.get(l3Vni));
              ExtendedCommunity rt = toRouteTarget(localAs, l3Vni);
              // Grab the BgpVrf for the innerVrf, if it exists
              @Nullable
              BgpVrf innerBgpVrf =
                  (innerVrfName.equals(Configuration.DEFAULT_VRF_NAME)
                      ? _bgpProcess.getDefaultVrf()
                      : _bgpProcess.getVrfs().get(innerVrfName));
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
                .build())
        .setRouteReflectorClient(
            firstNonNull(
                neighbor.getL2vpnEvpnAddressFamily().getRouteReflectorClient(), Boolean.FALSE))
        .setExportPolicy(routingPolicy.getName())
        .build();
  }

  private void applyBridgeSettings(
      InterfaceBridgeSettings bridge, org.batfish.datamodel.Interface newIface) {
    String name = newIface.getName();
    Integer access = bridge.getAccess();
    Integer ifacePvid = bridge.getPvid();
    IntegerSpace ifaceVids = bridge.getVids();
    if (!_bridge.getPorts().contains(name)) {
      if (access != null || ifacePvid != null || !ifaceVids.isEmpty()) {
        _w.redFlag(
            String.format(
                "No support for VLAN switching options on non-'bridge bridge' port: '%s'", name));
      }
      return;
    }
    newIface.setSwitchport(true);
    if (access != null) {
      // access
      newIface.setSwitchportMode(SwitchportMode.ACCESS);
      newIface.setAccessVlan(access);
      return;
    }
    // trunk
    newIface.setSwitchportMode(SwitchportMode.TRUNK);
    int nativeVlan = firstNonNull(ifacePvid, _bridge.getPvid());
    newIface.setNativeVlan(nativeVlan);
    newIface.setAllowedVlans(
        (!ifaceVids.isEmpty() ? ifaceVids : _bridge.getVids()).union(IntegerSpace.of(nativeVlan)));
  }

  private void applyCommonInterfaceSettings(
      Interface iface, org.batfish.datamodel.Interface newIface) {
    if (!iface.getIpAddresses().isEmpty()) {
      newIface.setAddress(iface.getIpAddresses().get(0));
    }
    newIface.setAllAddresses(iface.getIpAddresses());
    if (iface.getIpAddresses().isEmpty() && isUsedForBgpUnnumbered(iface.getName())) {
      newIface.setAddress(LINK_LOCAL_ADDRESS);
      newIface.setAllAddresses(ImmutableSet.of(LINK_LOCAL_ADDRESS));
    }
  }

  private boolean isUsedForBgpUnnumbered(@Nonnull String ifaceName) {
    return _bgpProcess != null
        && Stream.concat(
                Stream.of(_bgpProcess.getDefaultVrf()), _bgpProcess.getVrfs().values().stream())
            .flatMap(vrf -> vrf.getNeighbors().keySet().stream())
            .anyMatch(Predicate.isEqual(ifaceName));
  }

  /**
   * Create bidirectional associate between {@code vrf} and interface-like entities with vrf
   * assigned to {@code vrfName}.
   */
  private void assignInterfacesToVrf(
      org.batfish.datamodel.Vrf vrf, @Nonnull String assignedVrfName) {
    Stream<String> matchingBondNames =
        _bonds.entrySet().stream()
            .filter(bondEntry -> Objects.equals(bondEntry.getValue().getVrf(), assignedVrfName))
            .map(Entry::getKey);
    Stream<String> matchingInterfaceNames =
        _interfaces.entrySet().stream()
            .filter(
                interfaceEntry ->
                    Objects.equals(interfaceEntry.getValue().getVrf(), assignedVrfName))
            .map(Entry::getKey);
    Stream<String> matchingVlanNames =
        _vlans.entrySet().stream()
            .filter(
                interfaceEntry ->
                    Objects.equals(interfaceEntry.getValue().getVrf(), assignedVrfName))
            .map(Entry::getKey);
    Stream<String> matchingVrfLoopbackNames = Stream.of(assignedVrfName);
    Streams.concat(
            matchingBondNames, matchingInterfaceNames, matchingVlanNames, matchingVrfLoopbackNames)
        .map(_c.getAllInterfaces()::get)
        .forEach(
            iface -> {
              iface.setVrf(vrf);
              vrf.getInterfaces().put(iface.getName(), iface);
            });
  }

  private @Nonnull LongSpace computeRemoteAsns(BgpNeighbor neighbor, @Nullable Long localAs) {
    if (neighbor.getRemoteAsType() == RemoteAsType.EXPLICIT) {
      Long remoteAs = neighbor.getRemoteAs();
      return remoteAs == null ? LongSpace.EMPTY : LongSpace.of(remoteAs);
    } else if (localAs == null) {
      return LongSpace.EMPTY;
    } else if (neighbor.getRemoteAsType() == RemoteAsType.EXTERNAL) {
      return BgpPeerConfig.ALL_AS_NUMBERS.difference(LongSpace.of(localAs));
    } else if (neighbor.getRemoteAsType() == RemoteAsType.INTERNAL) {
      return LongSpace.of(localAs);
    }
    throw new IllegalArgumentException(
        String.format("Invalid remote-as type: %s", neighbor.getRemoteAsType()));
  }

  @VisibleForTesting
  void convertBgpProcess() {
    if (_bgpProcess == null) {
      return;
    }
    // First pass: only core processes
    _c.getDefaultVrf()
        .setBgpProcess(toBgpProcess(Configuration.DEFAULT_VRF_NAME, _bgpProcess.getDefaultVrf()));
    // We make one VI process per VRF because our current datamodel requires it
    _bgpProcess
        .getVrfs()
        .forEach(
            (vrfName, bgpVrf) ->
                _c.getVrfs().get(vrfName).setBgpProcess(toBgpProcess(vrfName, bgpVrf)));

    // Create dud processes for other VRFs that use L3 VNIs, so we can have proper RIBs
    _c.getVrfs()
        .forEach(
            (vrfName, vrf) -> {
              Vrf vsVrf = _vrfs.get(vrfName);
              if (vsVrf != null
                  && vsVrf.getVni() != null // has L3 VNI
                  && vrf.getBgpProcess() == null // process does not already exist
                  && _c.getDefaultVrf().getBgpProcess() != null) { // there is a default BGP proc
                vrf.setBgpProcess(
                    org.batfish.datamodel.BgpProcess.builder()
                        .setRouterId(_c.getDefaultVrf().getBgpProcess().getRouterId())
                        .setAdminCostsToVendorDefaults(ConfigurationFormat.CUMULUS_NCLU)
                        .build());
              }
            });

    /*
     * Second pass: Add neighbors.
     * Requires all VRFs & bgp processes in a VRF to be set in VI so that we can initialize address families
     * that access other VRFs (e.g., EVPN)
     */
    Iterables.concat(ImmutableSet.of(_bgpProcess.getDefaultVrf()), _bgpProcess.getVrfs().values())
        .forEach(
            bgpVrf -> {
              Long localAs = bgpVrf.getAutonomousSystem();
              org.batfish.datamodel.BgpProcess viBgpProcess =
                  _c.getVrfs().get(bgpVrf.getVrfName()).getBgpProcess();
              bgpVrf
                  .getNeighbors()
                  .forEach(
                      (neighborName, neighbor) -> {
                        if (neighbor instanceof BgpInterfaceNeighbor) {
                          BgpInterfaceNeighbor interfaceNeighbor = (BgpInterfaceNeighbor) neighbor;
                          interfaceNeighbor.inheritFrom(bgpVrf.getNeighbors());
                          addInterfaceNeighbor(interfaceNeighbor, localAs, bgpVrf, viBgpProcess);
                        } else if (neighbor instanceof BgpIpNeighbor) {
                          BgpIpNeighbor ipNeighbor = (BgpIpNeighbor) neighbor;
                          ipNeighbor.inheritFrom(bgpVrf.getNeighbors());
                          addIpv4BgpNeighbor(ipNeighbor, localAs, bgpVrf, viBgpProcess);
                        } else if (!(neighbor instanceof BgpPeerGroupNeighbor)) {
                          throw new IllegalArgumentException(
                              "Unsupported BGP neighbor type: "
                                  + neighbor.getClass().getSimpleName());
                        }
                      });
            });
  }

  void convertOspfProcess() {
    if (_ospfProcess == null) {
      return;
    }

    convertOspfVrf(_ospfProcess.getDefaultVrf(), _c.getDefaultVrf());

    _ospfProcess
        .getVrfs()
        .values()
        .forEach(
            ospfVrf -> {
              org.batfish.datamodel.Vrf vrf = _c.getVrfs().get(ospfVrf.getVrfName());

              if (vrf == null) {
                _w.redFlag(String.format("Vrf %s is not found.", ospfVrf.getVrfName()));
                return;
              }

              convertOspfVrf(ospfVrf, vrf);
            });
  }

  private void convertOspfVrf(OspfVrf ospfVrf, org.batfish.datamodel.Vrf vrf) {
    org.batfish.datamodel.ospf.OspfProcess ospfProcess = toOspfProcess(ospfVrf, vrf);
    vrf.addOspfProcess(ospfProcess);
  }

  private void convertBondInterfaces() {
    _bonds.forEach((name, bond) -> _c.getAllInterfaces().put(name, toInterface(bond)));
  }

  private void convertClags() {
    List<Interface> clagSourceInterfaces =
        _interfaces.values().stream()
            .filter(i -> i.getClag() != null)
            .collect(ImmutableList.toImmutableList());
    if (clagSourceInterfaces.isEmpty()) {
      return;
    }
    if (clagSourceInterfaces.size() > 1) {
      _w.redFlag(
          String.format(
              "CLAG configuration on multiple peering interfaces is unsupported: %s",
              clagSourceInterfaces.stream()
                  .map(Interface::getName)
                  .collect(ImmutableList.toImmutableList())));
      return;
    }
    Interface clagSourceInterface = clagSourceInterfaces.get(0);
    String sourceInterfaceName = clagSourceInterface.getName();
    Ip peerAddress = clagSourceInterface.getClag().getPeerIp();
    // Special case link-local addresses when no other addresses are defined
    org.batfish.datamodel.Interface viInterface = _c.getAllInterfaces().get(sourceInterfaceName);
    if (peerAddress == null
        && clagSourceInterface.getClag().isPeerIpLinkLocal()
        && viInterface.getAllAddresses().isEmpty()) {
      LinkLocalAddress lla = LinkLocalAddress.of(CLAG_LINK_LOCAL_IP);
      viInterface.setAddress(lla);
      viInterface.setAllAddresses(ImmutableSet.of(lla));
    }
    String peerInterfaceName = clagSourceInterface.getSuperInterfaceName();
    _c.setMlags(
        ImmutableMap.of(
            CUMULUS_CLAG_DOMAIN_ID,
            Mlag.builder()
                .setId(CUMULUS_CLAG_DOMAIN_ID)
                .setLocalInterface(sourceInterfaceName)
                .setPeerAddress(peerAddress)
                .setPeerInterface(peerInterfaceName)
                .build()));
  }

  private void convertDefaultVrf() {
    org.batfish.datamodel.Vrf defaultVrf =
        new org.batfish.datamodel.Vrf(Configuration.DEFAULT_VRF_NAME);
    defaultVrf.setStaticRoutes(
        _staticRoutes.stream()
            .map(StaticRoute::convert)
            .collect(ImmutableSortedSet.toImmutableSortedSet(naturalOrder())));
    // Add all unassigned interfaces to default VRF
    _c.getAllInterfaces()
        .forEach(
            (ifaceName, iface) -> {
              if (iface.getVrf() == null) {
                iface.setVrf(defaultVrf);
                defaultVrf.getInterfaces().put(ifaceName, iface);
              }
            });
    _c.getVrfs().put(Configuration.DEFAULT_VRF_NAME, defaultVrf);
  }

  private void convertDnsServers() {
    _c.setDnsServers(
        _ipv4Nameservers.stream()
            .map(Object::toString)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  @VisibleForTesting
  static void populateLoInInterfacesToLoopback(Interface iface, Loopback loopback) {
    checkArgument(
        iface.getType() == CumulusInterfaceType.LOOPBACK,
        String.format(
            "cannot populate interface with type %s to loopback", iface.getType().name()));
    loopback.getAddresses().addAll(iface.getIpAddresses());
  }

  private void convertLoopback() {
    Optional.ofNullable(_interfaces.get(LOOPBACK_INTERFACE_NAME))
        .ifPresent(iface -> populateLoInInterfacesToLoopback(iface, _loopback));

    org.batfish.datamodel.Interface newIface = createVIInterfaceForLo();

    if (!_loopback.getAddresses().isEmpty()) {
      newIface.setAddress(_loopback.getAddresses().get(0));
    }
    Builder<ConcreteInterfaceAddress> allAddresses = ImmutableSet.builder();
    allAddresses.addAll(_loopback.getAddresses());
    if (_loopback.getClagVxlanAnycastIp() != null) {
      // Just assume CLAG is correctly configured and comes up
      allAddresses.add(
          ConcreteInterfaceAddress.create(
              _loopback.getClagVxlanAnycastIp(), Prefix.MAX_PREFIX_LENGTH));
    }
    newIface.setAllAddresses(allAddresses.build());
    _c.getAllInterfaces().put(LOOPBACK_INTERFACE_NAME, newIface);
  }

  @VisibleForTesting
  org.batfish.datamodel.Interface createVIInterfaceForLo() {
    return org.batfish.datamodel.Interface.builder()
        .setActive(true)
        .setName(LOOPBACK_INTERFACE_NAME)
        .setOwner(_c)
        .setType(InterfaceType.LOOPBACK)
        .setBandwidth(
            Optional.ofNullable(_loopback.getBandwidth()).orElse(DEFAULT_LOOPBACK_BANDWIDTH))
        .build();
  }

  private void convertPhysicalInterfaces() {
    // create physical interfaces
    _interfaces.values().stream()
        .filter(i -> i.getType() == CumulusInterfaceType.PHYSICAL)
        .forEach(
            physicalInterface ->
                _c.getAllInterfaces()
                    .put(physicalInterface.getName(), toInterface(physicalInterface)));
    // create VI interface for every superinterface of a physical subinterface not explicitly
    // configured
    _interfaces.values().stream()
        .filter(i -> i.getType() == CumulusInterfaceType.PHYSICAL_SUBINTERFACE)
        .map(Interface::getSuperInterfaceName)
        .filter(Predicates.not(_interfaces::containsKey))
        .forEach(
            superInterfaceName ->
                _c.getAllInterfaces()
                    .put(
                        superInterfaceName,
                        toInterface(
                            new Interface(
                                superInterfaceName, CumulusInterfaceType.PHYSICAL, null, null))));
  }

  private void convertRouteMaps() {
    _routeMaps.forEach(
        (name, routeMap) -> new RouteMapConvertor(_c, this, routeMap, _w).toRouteMap());
  }

  private void convertSubinterfaces() {
    _interfaces.values().stream()
        .filter(
            i ->
                i.getType() == CumulusInterfaceType.BOND_SUBINTERFACE
                    || i.getType() == CumulusInterfaceType.PHYSICAL_SUBINTERFACE)
        .map(i -> toInterface(i, i.getSuperInterfaceName()))
        .forEach(newIface -> _c.getAllInterfaces().put(newIface.getName(), newIface));
  }

  private void convertVlanInterfaces() {
    _vlans.forEach((name, vlan) -> _c.getAllInterfaces().put(name, toInterface(vlan)));
  }

  private org.batfish.datamodel.Interface convertVrfLoopbackInterface(Vrf vrf) {
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(vrf.getName())
            .setOwner(_c)
            .setType(InterfaceType.LOOPBACK)
            .build();
    newIface.setActive(true);
    if (!vrf.getAddresses().isEmpty()) {
      newIface.setAddress(vrf.getAddresses().get(0));
    }
    newIface.setAllAddresses(vrf.getAddresses());
    return newIface;
  }

  private void convertVrfLoopbackInterfaces() {
    _vrfs.forEach((name, vrf) -> _c.getAllInterfaces().put(name, convertVrfLoopbackInterface(vrf)));
  }

  private void convertVrfs() {
    _vrfs.forEach(this::initVrf);
  }

  /**
   * Converts {@link Vxlan} into appropriate {@link Vni} for each VRF. Requires VI Vrfs to already
   * be properly initialized
   */
  private void convertVxlans() {
    if (_vxlans.isEmpty()) {
      return;
    }

    // Compute explicit VNI -> VRF mappings:
    Map<Integer, String> vniToVrf =
        _vrfs.values().stream()
            .filter(vrf -> vrf.getVni() != null)
            .collect(ImmutableMap.toImmutableMap(Vrf::getVni, Vrf::getName));

    // Put all valid VXLAN VNIs into appropriate VRF
    Map<String, Set<Layer2Vni>> vrfToVniSettings = new HashMap<>(0);
    _vxlans
        .values()
        .forEach(
            vxlan -> {
              if (vxlan.getId() == null
                  || vxlan.getLocalTunnelip() == null
                  || vxlan.getBridgeAccessVlan() == null) {
                // Not a valid VNI configuration
                return;
              }
              // TODO: this logic is wrong, fix it later.
              String vrfName = vniToVrf.getOrDefault(vxlan.getId(), Configuration.DEFAULT_VRF_NAME);
              vrfToVniSettings
                  .computeIfAbsent(vrfName, k -> new HashSet<>())
                  .add(
                      Layer2Vni.builder()
                          .setVni(vxlan.getId())
                          .setVlan(vxlan.getBridgeAccessVlan())
                          .setSourceAddress(
                              firstNonNull(
                                  _loopback.getClagVxlanAnycastIp(), vxlan.getLocalTunnelip()))
                          .setUdpPort(NamedPort.VXLAN.number())
                          .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                          .build());
            });
    vrfToVniSettings.forEach((vrfName, vnis) -> _c.getVrfs().get(vrfName).setLayer2Vnis(vnis));
  }

  public @Nullable BgpProcess getBgpProcess() {
    return _bgpProcess;
  }

  public @Nonnull Map<String, Bond> getBonds() {
    return _bonds;
  }

  public @Nonnull Bridge getBridge() {
    return _bridge;
  }

  @Override
  public @Nullable String getHostname() {
    return _hostname;
  }

  public @Nonnull Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull List<Ip> getIpv4Nameservers() {
    return _ipv4Nameservers;
  }

  public @Nonnull List<Ip6> getIpv6Nameservers() {
    return _ipv6Nameservers;
  }

  public @Nonnull Loopback getLoopback() {
    return _loopback;
  }

  public @Nullable OspfProcess getOspfProcess() {
    return _ospfProcess;
  }

  public @Nonnull Map<String, RouteMap> getRouteMaps() {
    return _routeMaps;
  }

  public @Nonnull Set<StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  public @Nonnull Map<String, Vlan> getVlans() {
    return _vlans;
  }

  public @Nonnull Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  public @Nonnull Map<String, Vxlan> getVxlans() {
    return _vxlans;
  }

  public @Nonnull Map<String, IpAsPathAccessList> getIpAsPathAccessLists() {
    return _ipAsPathAccessLists;
  }

  public @Nonnull Map<String, IpPrefixList> getIpPrefixLists() {
    return _ipPrefixLists;
  }

  public @Nonnull Map<String, IpCommunityList> getIpCommunityLists() {
    return _ipCommunityLists;
  }

  private void initVendorFamily() {
    _c.getVendorFamily()
        .setCumulus(
            CumulusFamily.builder()
                .setBridge(_bridge.toDataModel())
                .setInterfaceClagSettings(
                    _interfaces.entrySet().stream()
                        .filter(ifaceEntry -> ifaceEntry.getValue().getClag() != null)
                        .collect(
                            ImmutableSortedMap.toImmutableSortedMap(
                                Comparator.naturalOrder(),
                                Entry::getKey,
                                ifaceEntry -> ifaceEntry.getValue().getClag().toDataModel())))
                .build());
  }

  private void initVrf(String name, Vrf vrf) {
    org.batfish.datamodel.Vrf newVrf = new org.batfish.datamodel.Vrf(name);
    newVrf.setStaticRoutes(
        vrf.getStaticRoutes().stream()
            .map(StaticRoute::convert)
            .collect(ImmutableSortedSet.toImmutableSortedSet(naturalOrder())));
    assignInterfacesToVrf(newVrf, name);
    _c.getVrfs().put(name, newVrf);
  }

  private void markStructures() {
    markAbstractStructure(
        CumulusStructureType.ABSTRACT_INTERFACE,
        CumulusStructureUsage.BGP_NEIGHBOR_INTERFACE,
        ImmutableSet.of(
            CumulusStructureType.BOND,
            CumulusStructureType.INTERFACE,
            CumulusStructureType.LOOPBACK,
            CumulusStructureType.VLAN,
            CumulusStructureType.VRF));
    markAbstractStructure(
        CumulusStructureType.ABSTRACT_INTERFACE,
        CumulusStructureUsage.BRIDGE_PORT,
        ImmutableSet.of(
            CumulusStructureType.BOND,
            CumulusStructureType.INTERFACE,
            CumulusStructureType.LOOPBACK,
            CumulusStructureType.VLAN,
            CumulusStructureType.VRF));
    markAbstractStructure(
        CumulusStructureType.ABSTRACT_INTERFACE,
        CumulusStructureUsage.ROUTE_MAP_MATCH_INTERFACE,
        ImmutableSet.of(
            CumulusStructureType.BOND,
            CumulusStructureType.INTERFACE,
            CumulusStructureType.LOOPBACK,
            CumulusStructureType.VLAN,
            CumulusStructureType.VRF));
    markConcreteStructure(CumulusStructureType.BOND);
    markConcreteStructure(CumulusStructureType.INTERFACE);
    markConcreteStructure(CumulusStructureType.IP_AS_PATH_ACCESS_LIST);
    markConcreteStructure(CumulusStructureType.IP_COMMUNITY_LIST);
    markConcreteStructure(CumulusStructureType.IP_PREFIX_LIST);
    markConcreteStructure(CumulusStructureType.LOOPBACK);
    markConcreteStructure(CumulusStructureType.ROUTE_MAP);
    markConcreteStructure(CumulusStructureType.VLAN);
    markConcreteStructure(CumulusStructureType.VRF);
    markConcreteStructure(CumulusStructureType.VXLAN);
  }

  public void setBgpProcess(@Nullable BgpProcess bgpProcess) {
    _bgpProcess = bgpProcess;
  }

  /** For testing conversion methods. */
  @VisibleForTesting
  void setConfiguration(Configuration c) {
    _c = c;
  }

  @Override
  public void setHostname(@Nullable String hostname) {
    _hostname = hostname == null ? null : hostname.toLowerCase();
  }

  public void setOspfProcess(@Nullable OspfProcess ospfProcess) {
    _ospfProcess = ospfProcess;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {}

  /**
   * Returns {@link org.batfish.datamodel.BgpProcess} for named {@code bgpVrf} if valid, or else
   * {@code null}.
   */
  @Nullable
  org.batfish.datamodel.BgpProcess toBgpProcess(String vrfName, BgpVrf bgpVrf) {
    Ip routerId = bgpVrf.getRouterId();
    if (routerId == null) {
      routerId = inferRouterId();
    }
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
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
    Long confederationId = _bgpProcess.getDefaultVrf().getConfederationId();
    Long asn = _bgpProcess.getDefaultVrf().getAutonomousSystem();
    if (confederationId != null && asn != null) {
      // TODO: there probably is another way to define confederation members
      newProc.setConfederation(new BgpConfederation(confederationId, ImmutableSet.of(asn)));
    }

    BgpIpv4UnicastAddressFamily ipv4Unicast = bgpVrf.getIpv4Unicast();
    if (ipv4Unicast != null) {
      // Add networks from network statements to new process's origination space
      ipv4Unicast.getNetworks().keySet().forEach(newProc::addToOriginationSpace);

      // Generate aggregate routes
      generateGeneratedRoutes(_c, _c.getVrfs().get(vrfName), ipv4Unicast.getAggregateNetworks());
    }

    generateBgpCommonExportPolicy(vrfName, bgpVrf);

    return newProc;
  }

  @VisibleForTesting
  org.batfish.datamodel.ospf.OspfProcess toOspfProcess(
      OspfVrf ospfVrf, org.batfish.datamodel.Vrf vrf) {
    Ip routerId = ospfVrf.getRouterId();
    if (routerId == null) {
      routerId = inferRouterId();
    }

    org.batfish.datamodel.ospf.OspfProcess.Builder builder =
        org.batfish.datamodel.ospf.OspfProcess.builder();

    org.batfish.datamodel.ospf.OspfProcess proc =
        builder
            .setRouterId(routerId)
            .setProcessId(DEFAULT_OSPF_PROCESS_NAME)
            .setReferenceBandwidth(OspfProcess.DEFAULT_REFERENCE_BANDWIDTH)
            .build();

    addOspfInterfaces(vrf, proc.getProcessId());
    proc.setAreas(computeOspfAreas(vrf.getInterfaceNames()));
    return proc;
  }

  @VisibleForTesting
  void addOspfInterfaces(org.batfish.datamodel.Vrf vrf, String processId) {
    vrf.getInterfaces()
        .forEach(
            (ifaceName, iface) -> {
              Interface vsIface = _interfaces.get(iface.getName());
              OspfInterface ospfInterface = vsIface.getOspf();
              if (ospfInterface == null || ospfInterface.getOspfArea() == null) {
                // no ospf running on this interface
                return;
              }

              iface.setOspfSettings(
                  OspfInterfaceSettings.builder()
                      .setPassive(Optional.ofNullable(ospfInterface.getPassive()).orElse(false))
                      .setAreaName(ospfInterface.getOspfArea())
                      .setNetworkType(toOspfNetworkType(ospfInterface.getNetwork()))
                      .setDeadInterval(
                          Optional.ofNullable(ospfInterface.getDeadInterval())
                              .orElse(DEFAULT_OSPF_DEAD_INTERVAL))
                      .setHelloInterval(
                          Optional.ofNullable(ospfInterface.getHelloInterval())
                              .orElse(DEFAULT_OSPF_HELLO_INTERVAL))
                      .setProcess(processId)
                      .build());
            });
  }

  @VisibleForTesting
  SortedMap<Long, OspfArea> computeOspfAreas(Collection<String> interfaces) {
    Map<Long, List<String>> areaInterfaces =
        interfaces.stream()
            .map(_interfaces::get)
            .filter(vsIface -> vsIface.getOspf() != null && vsIface.getOspf().getOspfArea() != null)
            .collect(
                groupingBy(
                    vsIface -> vsIface.getOspf().getOspfArea(),
                    mapping(Interface::getName, Collectors.toList())));

    return toImmutableSortedMap(
        areaInterfaces,
        Entry::getKey,
        e -> OspfArea.builder().setNumber(e.getKey()).addInterfaces(e.getValue()).build());
  }

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
                "Conversion of Cumulus FRR OSPF network type '%s' is not handled.",
                type.toString()));
        return null;
    }
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
  Ip inferRouterId() {
    if (_loopback.getConfigured()) {
      Optional<ConcreteInterfaceAddress> maxLoIp =
          _loopback.getAddresses().stream()
              .filter(addr -> !LOOPBACK_PREFIX.containsIp(addr.getIp()))
              .max(ConcreteInterfaceAddress::compareTo);
      if (maxLoIp.isPresent()) {
        return maxLoIp.get().getIp();
      }
    }

    Optional<ConcreteInterfaceAddress> biggestInterfaceIp =
        _interfaces.values().stream()
            .flatMap(iface -> iface.getIpAddresses().stream())
            .max(InterfaceAddress::compareTo);

    return biggestInterfaceIp
        .map(ConcreteInterfaceAddress::getIp)
        .orElseGet(() -> Ip.parse("0.0.0.0"));
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
  private void generateBgpCommonExportPolicy(String vrfName, BgpVrf bgpVrf) {
    RoutingPolicy bgpCommonExportPolicy =
        RoutingPolicy.builder()
            .setOwner(_c)
            .setName(computeBgpCommonExportPolicyName(vrfName))
            .build();

    List<Statement> statements = new ArrayList<>();

    // 1. If there are any ipv4 summary only networks, do not export the more specific routes.
    if (bgpVrf.getIpv4Unicast() != null) {
      Stream<Prefix> summarizedPrefixes =
          bgpVrf.getIpv4Unicast().getAggregateNetworks().entrySet().stream()
              .filter(e -> e.getValue().isSummaryOnly())
              .map(Entry::getKey);
      Optional.ofNullable(suppressSummarizedPrefixes(_c, vrfName, summarizedPrefixes))
          .ifPresent(statements::add);
    }

    // 2. Setup export conditions, export if match, otherwise fall through
    Disjunction exportConditions = new Disjunction();

    // 2a. add export conditions for non-aggregate routes
    exportConditions.getDisjuncts().addAll(getBgpExportConditions(bgpVrf));

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

  private List<BooleanExpr> getBgpExportConditions(BgpVrf bgpVrf) {
    List<BooleanExpr> exportConditions = new ArrayList<>();

    // Always export BGP and iBGP routes
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP, RoutingProtocol.IBGP));

    // If no IPv4 address family is not defined, there is no capability to explicitly advertise v4
    // networks or redistribute protocols, so no non-BGP routes can be exported.
    if (bgpVrf.getIpv4Unicast() == null) {
      return exportConditions;
    }

    // Add conditions to redistribute other protocols
    for (BgpRedistributionPolicy redistributeProtocolPolicy :
        bgpVrf.getIpv4Unicast().getRedistributionPolicies().values()) {

      // Get a match expression for the protocol to be redistributed
      CumulusRoutingProtocol protocol = redistributeProtocolPolicy.getProtocol();
      MatchProtocol matchProtocol = new MatchProtocol(VI_PROTOCOLS_MAP.get(protocol));

      // Create a WithEnvironmentExpr with the redistribution route-map, if one is defined
      BooleanExpr weInterior = BooleanExprs.TRUE;
      String mapName = redistributeProtocolPolicy.getRouteMap();
      if (mapName != null && _routeMaps.keySet().contains(mapName)) {
        weInterior = new CallExpr(mapName);
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);

      // Export routes that match the protocol and WithEnvironmentExpr
      Conjunction exportProtocolConditions = new Conjunction(ImmutableList.of(matchProtocol, we));
      exportProtocolConditions.setComment(
          String.format("Redistribute %s routes into BGP", protocol));
      exportConditions.add(exportProtocolConditions);
    }

    // create origination prefilter from listed advertised networks
    bgpVrf
        .getIpv4Unicast()
        .getNetworks()
        .forEach(
            (prefix, bgpNetwork) -> {
              BooleanExpr weExpr = BooleanExprs.TRUE;
              BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weExpr, OriginType.IGP);
              Conjunction exportNetworkConditions = new Conjunction();
              exportNetworkConditions
                  .getConjuncts()
                  .add(
                      new MatchPrefixSet(
                          DestinationNetwork.instance(),
                          new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(prefix)))));
              /*
              Don't need to explicitly exclude BGP and iBGP routes here because those routes will
              already be matched earlier in exportConditions (which are disjuncts).
               */
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)));
              exportNetworkConditions.getConjuncts().add(we);
              exportConditions.add(exportNetworkConditions);
            });
    return exportConditions;
  }

  public void setBonds(@Nonnull Map<String, Bond> bonds) {
    _bonds = ImmutableMap.copyOf(bonds);
  }

  public void setBridge(@Nonnull Bridge bridge) {
    _bridge = bridge;
  }

  public void setInterfaces(@Nonnull Map<String, Interface> interfaces) {
    _interfaces = ImmutableMap.copyOf(interfaces);
  }

  public void setVlans(@Nonnull Map<String, Vlan> vlans) {
    _vlans = ImmutableMap.copyOf(vlans);
  }

  public void setVrfs(@Nonnull Map<String, Vrf> vrfs) {
    _vrfs = ImmutableMap.copyOf(vrfs);
  }

  public void setVxlans(Map<String, Vxlan> vxlans) {
    _vxlans = ImmutableMap.copyOf(vxlans);
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(Bond bond) {
    String name = bond.getName();
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(name)
            .setOwner(_c)
            .setType(InterfaceType.AGGREGATED)
            .build();

    bond.getSlaves().forEach(slave -> _c.getAllInterfaces().get(slave).setChannelGroup(name));
    newIface.setChannelGroupMembers(bond.getSlaves());
    newIface.setDependencies(
        bond.getSlaves().stream()
            .map(slave -> new Dependency(slave, DependencyType.AGGREGATE))
            .collect(ImmutableSet.toImmutableSet()));

    applyBridgeSettings(bond.getBridge(), newIface);

    newIface.setActive(true);
    if (!bond.getIpAddresses().isEmpty()) {
      newIface.setAddress(bond.getIpAddresses().get(0));
    }
    newIface.setAllAddresses(bond.getIpAddresses());

    newIface.setMlagId(bond.getClagId());

    return newIface;
  }

  @VisibleForTesting
  @Nonnull
  org.batfish.datamodel.Interface toInterface(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(name)
            .setOwner(_c)
            .setType(InterfaceType.PHYSICAL)
            .setActive(!iface.isDisabled())
            .build();
    applyCommonInterfaceSettings(iface, newIface);

    applyBridgeSettings(iface.getBridge(), newIface);

    newIface.setDescription(iface.getAlias());
    if (iface.getSpeed() != null) {
      double speed = iface.getSpeed() * SPEED_CONVERSION_FACTOR;
      newIface.setSpeed(speed);
      newIface.setBandwidth(speed);
    } else {
      newIface.setBandwidth(DEFAULT_PORT_BANDWIDTH);
    }

    return newIface;
  }

  @VisibleForTesting
  @Nonnull
  org.batfish.datamodel.Interface toInterface(Interface iface, String superInterfaceName) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(name)
            .setOwner(_c)
            .setType(
                iface.getType() == CumulusInterfaceType.BOND_SUBINTERFACE
                    ? InterfaceType.AGGREGATE_CHILD
                    : InterfaceType.LOGICAL)
            .setActive(!iface.isDisabled())
            .build();
    newIface.setDependencies(
        ImmutableSet.of(new Dependency(superInterfaceName, DependencyType.BIND)));
    newIface.setEncapsulationVlan(iface.getEncapsulationVlan());
    applyCommonInterfaceSettings(iface, newIface);
    return newIface;
  }

  private org.batfish.datamodel.Interface toInterface(Vlan vlan) {
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(vlan.getName())
            .setOwner(_c)
            .setType(InterfaceType.VLAN)
            .build();
    newIface.setActive(true);
    newIface.setVlan(vlan.getVlanId());

    // Interface addreses
    if (!vlan.getAddresses().isEmpty()) {
      newIface.setAddress(vlan.getAddresses().get(0));
    }
    ImmutableSet.Builder<InterfaceAddress> allAddresses = ImmutableSet.builder();
    allAddresses.addAll(vlan.getAddresses());
    vlan.getAddressVirtuals().values().forEach(allAddresses::addAll);
    newIface.setAllAddresses(allAddresses.build());
    newIface.setDescription(vlan.getAlias());

    return newIface;
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
  private ExtendedCommunity toRouteTarget(long asn, long vxlanId) {
    return ExtendedCommunity.target(asn & 0xFFFFL, vxlanId);
  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    _c = new Configuration(getHostname(), ConfigurationFormat.CUMULUS_NCLU);
    _c.setDefaultCrossZoneAction(LineAction.PERMIT);
    _c.setDefaultInboundAction(LineAction.PERMIT);

    convertPhysicalInterfaces();
    convertBondInterfaces();
    convertSubinterfaces();
    convertVlanInterfaces();
    convertLoopback();
    convertVrfLoopbackInterfaces();
    convertVrfs();
    convertDefaultVrf();
    convertIpAsPathAccessLists();
    convertIpPrefixLists();
    convertIpCommunityLists();
    convertRouteMaps();
    convertDnsServers();
    convertClags();
    convertVxlans();
    convertOspfProcess();
    convertBgpProcess();

    initVendorFamily();

    markStructures();

    warnDuplicateClagIds();

    return _c;
  }

  private void convertIpCommunityLists() {
    _ipCommunityLists.forEach(
        (name, list) -> _c.getCommunityLists().put(name, toCommunityList(list)));
  }

  @VisibleForTesting
  static CommunityList toCommunityList(IpCommunityList list) {
    return list.accept(
        ipCommunityList ->
            new CommunityList(
                ipCommunityList.getName(),
                ipCommunityList.getCommunities().stream()
                    .map(LiteralCommunity::new)
                    .map(k -> new CommunityListLine(ipCommunityList.getAction(), k))
                    .collect(ImmutableList.toImmutableList()),
                false));
  }

  private void convertIpAsPathAccessLists() {
    _ipAsPathAccessLists.forEach(
        (name, asPathAccessList) ->
            _c.getAsPathAccessLists().put(name, toAsPathAccessList(asPathAccessList)));
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

  private void convertIpPrefixLists() {
    _ipPrefixLists.forEach(
        (name, ipPrefixList) ->
            _c.getRouteFilterLists().put(name, toRouteFilterList(ipPrefixList)));
  }

  @VisibleForTesting
  static @Nonnull RouteFilterList toRouteFilterList(IpPrefixList ipPrefixList) {
    String name = ipPrefixList.getName();
    RouteFilterList rfl = new RouteFilterList(name);
    rfl.setLines(
        ipPrefixList.getLines().values().stream()
            .map(CumulusNcluConfiguration::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList()));
    return rfl;
  }

  @VisibleForTesting
  static @Nonnull RouteFilterLine toRouteFilterLine(IpPrefixListLine ipPrefixListLine) {
    return new RouteFilterLine(
        ipPrefixListLine.getAction(),
        ipPrefixListLine.getPrefix(),
        ipPrefixListLine.getLengthRange());
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations()
      throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  private void warnDuplicateClagIds() {
    Map<Integer, List<Bond>> clagBondsById = new HashMap<>();
    _bonds.values().stream()
        .filter(bond -> bond.getClagId() != null)
        .forEach(
            clagBond ->
                clagBondsById
                    .computeIfAbsent(clagBond.getClagId(), id -> new LinkedList<>())
                    .add(clagBond));
    clagBondsById.forEach(
        (id, clagBonds) -> {
          if (clagBonds.size() > 1) {
            _w.redFlag(
                String.format(
                    "clag-id %d is erroneously configured on more than one bond: %s",
                    id,
                    clagBonds.stream()
                        .map(Bond::getName)
                        .collect(ImmutableList.toImmutableList())));
          }
        });
  }
}
