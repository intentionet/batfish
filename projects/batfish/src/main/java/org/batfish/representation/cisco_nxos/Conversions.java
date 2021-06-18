package org.batfish.representation.cisco_nxos;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.singletonList;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Names.generatedBgpCommonExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpDefaultRouteExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerEvpnExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerEvpnImportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerImportPolicyName;
import static org.batfish.datamodel.routing_policy.Common.generateSuppressionPolicy;
import static org.batfish.datamodel.routing_policy.statement.Statements.RemovePrivateAs;
import static org.batfish.representation.cisco_nxos.Vrf.MAC_VRF_OFFSET;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.AllowRemoteAsOutMode;
import org.batfish.datamodel.bgp.BgpAggregate;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.routing_policy.Common;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.AsnValue;
import org.batfish.datamodel.routing_policy.expr.AutoAs;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetDefaultTag;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.SetTag;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;
import org.batfish.representation.cisco_nxos.BgpVrfL2VpnEvpnAddressFamilyConfiguration.RetainRouteType;

/**
 * A utility class for converting between Cisco NX-OS configurations and the Batfish
 * vendor-independent {@link org.batfish.datamodel}.
 */
@ParametersAreNonnullByDefault
final class Conversions {

  /** Matches anything but the IPv4 default route. */
  static final Not NOT_DEFAULT_ROUTE = new Not(Common.matchDefaultRoute());

  private static final int MAX_ADMINISTRATIVE_COST = 32767;

  private static final Statement ROUTE_MAP_DENY_STATEMENT =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
          ImmutableList.of(Statements.ExitReject.toStaticStatement()));

  /**
   * Computes the router ID on Cisco NX-OS.
   *
   * <p>See
   * https://www.cisco.com/c/en/us/td/docs/switches/datacenter/sw/nx-os/tech_note/cisco_nxos_ios_ospf_comparison.html
   * for a description of the algorithm, which is in practice applied per-VRF.
   */
  // See CiscoNxosTest#testRouterId for a test that is verifiable using GNS3.
  @Nonnull
  static Ip getBgpRouterId(BgpVrfConfiguration vrfConfig, Configuration c, Vrf vrf, Warnings w) {
    // If Router ID is configured in the VRF-Specific BGP config, it always wins.
    if (vrfConfig.getRouterId() != null) {
      return vrfConfig.getRouterId();
    }

    return inferRouterId(vrf.getName(), c.getAllInterfaces(vrf.getName()), w, "BGP process");
  }

  /** Infers router ID on Cisco NX-OS when not configured in a routing process */
  @Nonnull
  static Ip inferRouterId(
      String vrfName, Map<String, Interface> vrfIfaces, Warnings w, String processDesc) {
    String messageBase =
        String.format(
            "Router-id is not manually configured for %s in VRF %s", processDesc, vrfName);

    // Otherwise, Router ID is defined based on the interfaces in the VRF that have IP addresses.
    // NX-OS does use shutdown interfaces to configure router-id.
    Map<String, org.batfish.datamodel.Interface> interfaceMap =
        vrfIfaces.entrySet().stream()
            .filter(e -> e.getValue().getConcreteAddress() != null)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    if (interfaceMap.isEmpty()) {
      w.redFlag(
          String.format(
              "%s. Unable to infer default router-id as no interfaces have IP addresses",
              messageBase));
      // With no interfaces in the VRF that have IP addresses, show ip bgp vrf all reports 0.0.0.0
      // as the router ID. Of course, this is not really relevant as no routes will be exchanged.
      return Ip.ZERO;
    }

    // Next, NX-OS prefers the IP of loopback0 if one exists.
    org.batfish.datamodel.Interface loopback0 = interfaceMap.get("loopback0");
    if (loopback0 != null) {
      // No need to warn.
      return loopback0.getConcreteAddress().getIp();
    }

    // Next, NX-OS prefers "first" loopback interface. Older versions of NX-OS appear to be
    // non-deterministic, newer ones always choose the smallest loopback IP.
    Collection<org.batfish.datamodel.Interface> interfaces = interfaceMap.values();
    Optional<Ip> lowestLoopback =
        interfaces.stream()
            .filter(i -> i.getInterfaceType() == InterfaceType.LOOPBACK)
            .map(org.batfish.datamodel.Interface::getConcreteAddress)
            .map(ConcreteInterfaceAddress::getIp)
            .min(Comparator.naturalOrder());
    if (lowestLoopback.isPresent()) {
      return lowestLoopback.get();
    }

    // Finally, NX-OS uses the first non-loopback interface defined in the vrf, assuming no loopback
    // addresses with IP address are present in the vrf. Older versions of NX-OS are
    // non-deterministic, newer ones choose the smallest IP.
    Optional<Ip> lowestIp =
        interfaces.stream()
            .map(org.batfish.datamodel.Interface::getConcreteAddress)
            .filter(Objects::nonNull)
            .map(ConcreteInterfaceAddress::getIp)
            .min(Comparator.naturalOrder());
    assert lowestIp.isPresent(); // This cannot happen if interfaces is non-empty.
    return lowestIp.get();
  }

  private static boolean isActive(String name, BgpVrfNeighborConfiguration neighbor, Warnings w) {
    // Shutdown
    if (firstNonNull(neighbor.getShutdown(), Boolean.FALSE)) {
      return false;
    }

    // No active address family that we support.
    if (neighbor.getIpv4UnicastAddressFamily() == null
        && neighbor.getIpv6UnicastAddressFamily() == null
        && neighbor.getL2VpnEvpnAddressFamily() == null) {
      w.redFlag("No supported address-family configured for " + name);
      return false;
    }

    return true;
  }

  static @Nonnull BgpAggregate toBgpAggregate(
      Prefix prefix,
      BgpVrfAddressFamilyAggregateNetworkConfiguration vsAggregate,
      Configuration c) {
    // TODO: handle as-set
    // TODO: handle suppress-map
    // TODO: verify undefined route-map can be treated as omitted
    String attributeMap =
        Optional.ofNullable(vsAggregate.getAttributeMap())
            .filter(c.getRoutingPolicies()::containsKey)
            .orElse(null);
    return BgpAggregate.of(
        prefix,
        generateSuppressionPolicy(vsAggregate.getSummaryOnly(), c),
        // TODO: put advertise-map here
        null,
        attributeMap);
  }

  @Nonnull
  static Map<Prefix, BgpActivePeerConfig> getNeighbors(
      Configuration c,
      CiscoNxosConfiguration vsConfig,
      Vrf vrf,
      BgpProcess proc,
      BgpGlobalConfiguration bgpConfig,
      BgpVrfConfiguration bgpVrf,
      Warnings warnings) {
    return bgpVrf.getNeighbors().entrySet().stream()
        .peek(e -> e.getValue().doInherit(bgpConfig, warnings))
        .filter(e -> isActive(getTextDesc(e.getKey(), vrf), e.getValue(), warnings))
        .collect(
            ImmutableMap.toImmutableMap(
                e -> e.getKey().toPrefix(),
                e ->
                    (BgpActivePeerConfig)
                        Conversions.toBgpNeighbor(
                            c,
                            vsConfig,
                            vrf,
                            proc,
                            e.getKey().toPrefix(),
                            bgpConfig,
                            bgpVrf,
                            e.getValue(),
                            false,
                            warnings)));
  }

  @Nonnull
  static Map<Prefix, BgpPassivePeerConfig> getPassiveNeighbors(
      Configuration c,
      CiscoNxosConfiguration vsConfig,
      Vrf vrf,
      BgpProcess proc,
      BgpGlobalConfiguration bgpConfig,
      BgpVrfConfiguration bgpVrf,
      Warnings warnings) {
    return bgpVrf.getPassiveNeighbors().entrySet().stream()
        .peek(e -> e.getValue().doInherit(bgpConfig, warnings))
        .filter(e -> isActive(getTextDesc(e.getKey(), vrf), e.getValue(), warnings))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                e ->
                    (BgpPassivePeerConfig)
                        Conversions.toBgpNeighbor(
                            c,
                            vsConfig,
                            vrf,
                            proc,
                            e.getKey(),
                            bgpConfig,
                            bgpVrf,
                            e.getValue(),
                            true,
                            warnings)));
  }

  @Nullable
  private static Ip computeUpdateSource(
      String vrfName,
      Map<String, org.batfish.datamodel.Interface> vrfInterfaces,
      Prefix prefix,
      BgpVrfNeighborConfiguration neighbor,
      boolean dynamic,
      Warnings warnings) {
    String updateSourceInterface = neighbor.getUpdateSource();
    if (updateSourceInterface != null) {
      org.batfish.datamodel.Interface iface = vrfInterfaces.get(updateSourceInterface);
      if (iface == null) {
        warnings.redFlag(
            String.format(
                "BGP neighbor %s in vrf %s: configured update-source %s does not exist or "
                    + "is not associated with this vrf",
                dynamic ? prefix : prefix.getStartIp(), vrfName, updateSourceInterface));
        return null;
      }
      ConcreteInterfaceAddress address = iface.getConcreteAddress();
      if (address == null) {
        warnings.redFlag(
            String.format(
                "BGP neighbor %s in vrf %s: configured update-source %s has no IP address",
                dynamic ? prefix : prefix.getStartIp(), vrfName, updateSourceInterface));
        return null;
      }
      return address.getIp();
    } else if (dynamic) {
      return Ip.AUTO;
    }
    Optional<Ip> firstMatchingInterfaceAddress =
        vrfInterfaces.values().stream()
            .flatMap(i -> i.getAllConcreteAddresses().stream())
            .filter(ia -> ia != null && ia.getPrefix().containsIp(prefix.getStartIp()))
            .map(ConcreteInterfaceAddress::getIp)
            .findFirst();
    if (firstMatchingInterfaceAddress.isPresent()) {
      /* TODO: Warn here? Seems like this may be standard practice, e.g., for a /31. */
      return firstMatchingInterfaceAddress.get();
    }

    warnings.redFlag(
        String.format(
            "BGP neighbor %s in vrf %s: could not determine update source",
            prefix.getStartIp(), vrfName));
    return null;
  }

  /**
   * Extracts the AS numbers from "match as-number" statements. See
   * https://www.cisco.com/c/m/en_us/techdoc/dc/reference/cli/nxos/commands/bgp/match-as-number.html
   */
  private static @Nonnull LongSpace extractRouteMapAsns(RouteMap map) {
    LongSpace asns = LongSpace.EMPTY;
    // Iterate backwards to support permit/deny semantics properly.
    for (RouteMapEntry entry : map.getEntries().descendingMap().values()) {
      RouteMapMatchAsNumber matchAsn = entry.getMatchAsNumber();
      if (matchAsn == null) {
        continue;
      }
      // Note: other match clauses are documented ignored.
      if (entry.getAction() == LineAction.PERMIT) {
        asns = asns.union(entry.getMatchAsNumber().getAsns());
      } else {
        asns = asns.difference(entry.getMatchAsNumber().getAsns());
      }
    }
    return asns;
  }

  @Nonnull
  private static BgpPeerConfig toBgpNeighbor(
      Configuration c,
      CiscoNxosConfiguration vsConfig,
      Vrf vrf,
      BgpProcess proc,
      Prefix prefix,
      BgpGlobalConfiguration bgpConfig,
      BgpVrfConfiguration vrfConfig,
      BgpVrfNeighborConfiguration neighbor,
      boolean dynamic,
      Warnings warnings) {

    BgpPeerConfig.Builder<?, ?> newNeighborBuilder;
    if (dynamic) {
      LongSpace remoteAsns;
      if (neighbor.getRemoteAs() != null) {
        remoteAsns = LongSpace.of(neighbor.getRemoteAs());
      } else {
        remoteAsns =
            Optional.ofNullable(neighbor.getRemoteAsRouteMap())
                .map(vsConfig.getRouteMaps()::get)
                .map(Conversions::extractRouteMapAsns)
                .orElse(LongSpace.EMPTY);
      }
      if (remoteAsns.isEmpty()) {
        warnings.redFlag("No remote-as configured for " + getTextDesc(prefix, vrf));
      }
      newNeighborBuilder =
          BgpPassivePeerConfig.builder().setRemoteAsns(remoteAsns).setPeerPrefix(prefix);
    } else {
      newNeighborBuilder =
          BgpActivePeerConfig.builder()
              .setRemoteAsns(
                  Optional.ofNullable(neighbor.getRemoteAs())
                      .map(LongSpace::of)
                      .orElse(LongSpace.EMPTY))
              .setPeerAddress(prefix.getStartIp());
      // No remote AS set.
      if (neighbor.getRemoteAs() == null) {
        warnings.redFlag("No remote-as configured for " + getTextDesc(prefix.getStartIp(), vrf));
      }
    }

    newNeighborBuilder.setClusterId(
        firstNonNull(vrfConfig.getClusterId(), proc.getRouterId()).asLong());

    newNeighborBuilder.setDescription(neighbor.getDescription());

    newNeighborBuilder.setEbgpMultihop(firstNonNull(neighbor.getEbgpMultihopTtl(), 0) > 1);

    newNeighborBuilder.setEnforceFirstAs(bgpConfig.getEnforceFirstAs());

    if (neighbor.getInheritPeer() != null) {
      newNeighborBuilder.setGroup(neighbor.getInheritPeer());
    }
    long localAs;
    if (neighbor.getLocalAs() != null) {
      localAs = neighbor.getLocalAs();
    } else if (vrfConfig.getLocalAs() != null) {
      localAs = vrfConfig.getLocalAs();
    } else {
      localAs = bgpConfig.getLocalAs();
    }
    newNeighborBuilder.setLocalAs(localAs);

    newNeighborBuilder.setLocalIp(
        computeUpdateSource(
            vrf.getName(), c.getAllInterfaces(vrf.getName()), prefix, neighbor, dynamic, warnings));

    @Nullable
    BgpVrfNeighborAddressFamilyConfiguration naf4 = neighbor.getIpv4UnicastAddressFamily();
    @Nullable BgpVrfIpv4AddressFamilyConfiguration af4 = vrfConfig.getIpv4UnicastAddressFamily();

    if (naf4 != null) {
      // import policy
      RoutingPolicy importPolicy =
          createNeighborImportPolicy(
              c,
              generatedBgpPeerImportPolicyName(
                  vrf.getName(), dynamic ? prefix.toString() : prefix.getStartIp().toString()),
              naf4,
              warnings);

      // export policy
      RoutingPolicy exportPolicy =
          createExportPolicyFromStatements(
              generatedBgpPeerExportPolicyName(
                  vrf.getName(), dynamic ? prefix.toString() : prefix.getStartIp().toString()),
              getExportStatementsForIpv4(
                  c, naf4, neighbor, newNeighborBuilder, vrf.getName(), warnings),
              c);

      Ipv4UnicastAddressFamily.Builder ipv4FamilyBuilder =
          Ipv4UnicastAddressFamily.builder()
              .setAddressFamilyCapabilities(
                  getAddressFamilyCapabilities(naf4, af4 != null && af4.getSuppressInactive()))
              .setExportPolicy(exportPolicy.getName())
              .setImportPolicy(importPolicy.getName())
              .setRouteReflectorClient(firstNonNull(naf4.getRouteReflectorClient(), Boolean.FALSE));

      newNeighborBuilder.setIpv4UnicastAddressFamily(ipv4FamilyBuilder.build());
    }

    // If neighbor has EVPN configured, set it up.
    @Nullable
    BgpVrfNeighborAddressFamilyConfiguration neighborL2VpnAf = neighbor.getL2VpnEvpnAddressFamily();
    if (neighborL2VpnAf != null) {
      @Nullable
      BgpVrfL2VpnEvpnAddressFamilyConfiguration vrfL2VpnAf = vrfConfig.getL2VpnEvpnAddressFamily();
      EvpnAddressFamily.Builder evpnFamilyBuilder =
          EvpnAddressFamily.builder().setPropagateUnmatched(false);

      RoutingPolicy importPolicy =
          createNeighborImportPolicy(
              c,
              generatedBgpPeerEvpnImportPolicyName(
                  vrf.getName(), dynamic ? prefix.toString() : prefix.getStartIp().toString()),
              neighborL2VpnAf,
              warnings);

      evpnFamilyBuilder
          .setAddressFamilyCapabilities(getAddressFamilyCapabilities(neighborL2VpnAf, false))
          .setImportPolicy(importPolicy.getName())
          .setRouteReflectorClient(
              firstNonNull(neighborL2VpnAf.getRouteReflectorClient(), Boolean.FALSE));
      if (vrfL2VpnAf != null) {
        if (vrfL2VpnAf.getRetainMode() == RetainRouteType.ROUTE_MAP) {
          warnings.redFlag("retain route-target is not supported for route-maps");
        } else {
          evpnFamilyBuilder.setPropagateUnmatched(
              vrfL2VpnAf.getRetainMode() == RetainRouteType.ALL);
        }
      }
      evpnFamilyBuilder.setL2Vnis(getL2VniConfigs(c, vrf, proc, localAs, vsConfig, warnings));
      evpnFamilyBuilder.setL3Vnis(getL3VniConfigs(c, vrf, proc, localAs, vsConfig, warnings));

      List<Statement> evpnStatements =
          getExportStatementsForEvpn(c, neighborL2VpnAf, neighbor, warnings);
      RoutingPolicy exportPolicy =
          createExportPolicyFromStatements(
              generatedBgpPeerEvpnExportPolicyName(
                  vrf.getName(), dynamic ? prefix.toString() : prefix.getStartIp().toString()),
              evpnStatements,
              c);
      newNeighborBuilder.setEvpnAddressFamily(
          evpnFamilyBuilder.setExportPolicy(exportPolicy.getName()).build());
    }
    return newNeighborBuilder.build();
  }

  private static SortedSet<Layer2VniConfig> getL2VniConfigs(
      Configuration c,
      Vrf vrfContainingBgpNeighbor,
      BgpProcess viBgpProcess,
      long localAs,
      CiscoNxosConfiguration vsConfig,
      Warnings warnings) {
    if (!vrfContainingBgpNeighbor.getName().equals(DEFAULT_VRF_NAME)) {
      // TODO: figure out what to do with BGP neighbors in non default tenant VRFs
      return ImmutableSortedSet.of();
    }
    ImmutableSortedSet.Builder<Layer2VniConfig> layer2Vnis = ImmutableSortedSet.naturalOrder();

    // looping over all VRFs in VI configuration so we can get all VNI settings which were valid and
    // mapped to some VRF (including the default VRF)
    for (Vrf tenantVrf : c.getVrfs().values()) {
      for (Layer2Vni l2Vni : tenantVrf.getLayer2Vnis().values()) {

        Integer macVrfId = getMacVrfIdForL2Vni(vsConfig, l2Vni.getVni());
        if (macVrfId == null) {
          continue;
        }

        EvpnVni evpnVni =
            Optional.ofNullable(vsConfig.getEvpn())
                .map(evpn -> evpn.getVni(l2Vni.getVni()))
                .orElse(null);
        if (evpnVni == null) {
          continue;
        }

        ExtendedCommunityOrAuto exportRtOrAuto = evpnVni.getExportRt();
        if (exportRtOrAuto == null) {
          // export route target is not present as auto and neither is user-defined, no L2 routes
          // (MAC-routes)
          // will be exported for hosts in this VNI. Assuming this to be an invalid EVPN
          // configuration
          // for lack of explicit doc from Cisco
          warnings.redFlag(
              String.format(
                  "No export route-target defined for L2 VNI '%s', no L2 routes will be exported",
                  l2Vni.getVni()));
          continue;
        }
        ExtendedCommunityOrAuto importRtOrAuto = evpnVni.getImportRt();
        if (importRtOrAuto == null) {
          // import route target is not present as auto and neither is user-defined, no L2 routes
          // (MAC-routes)
          // will be imported for this VNI. Assuming this to be an invalid EVPN configuration for
          // lack
          // of explicit doc from Cisco
          warnings.redFlag(
              String.format(
                  "No import route-target defined for L2 VNI '%s', no L2 routes will be imported",
                  l2Vni.getVni()));
          continue;
        }

        RouteDistinguisher rd =
            Optional.ofNullable(evpnVni.getRd())
                .map(RouteDistinguisherOrAuto::getRouteDistinguisher)
                .orElse(null);

        layer2Vnis.add(
            Layer2VniConfig.builder()
                .setVni(l2Vni.getVni())
                .setVrf(tenantVrf.getName())
                .setRouteDistinguisher(
                    firstNonNull(rd, RouteDistinguisher.from(viBgpProcess.getRouterId(), macVrfId)))
                .setImportRouteTarget(
                    importRtOrAuto.isAuto()
                        ? toRouteTarget(localAs, l2Vni.getVni()).matchString()
                        : importRtOrAuto.getExtendedCommunity().matchString())
                .setRouteTarget(
                    exportRtOrAuto.isAuto()
                        ? toRouteTarget(localAs, l2Vni.getVni())
                        : exportRtOrAuto.getExtendedCommunity())
                .build());
      }
    }
    return layer2Vnis.build();
  }

  private static SortedSet<Layer3VniConfig> getL3VniConfigs(
      Configuration c,
      Vrf vrfContainingBgpNeighbor,
      BgpProcess viBgpProcess,
      long localAs,
      CiscoNxosConfiguration vsConfig,
      Warnings warnings) {
    if (!vrfContainingBgpNeighbor.getName().equals(DEFAULT_VRF_NAME)) {
      // TODO: figure out what to do with tenant VRFs
      return ImmutableSortedSet.of();
    }
    ImmutableSortedSet.Builder<Layer3VniConfig> layer3Vnis = ImmutableSortedSet.naturalOrder();

    // looping over all VRFs in VI configuration so we can get all VNI settings which were valid and
    // mapped to some VRF (including the default VRF)
    for (Vrf tenantVrf : c.getVrfs().values()) {
      for (Layer3Vni l3Vni : tenantVrf.getLayer3Vnis().values()) {

        org.batfish.representation.cisco_nxos.Vrf vsTenantVrfForL3Vni =
            getVrfForL3Vni(vsConfig.getVrfs(), l3Vni.getVni());
        // there should be a tenant VRF for this VNI and that VRF should have an IPv4 AF
        // (other being IPv6 which we do not support); if not true then skip this VNI
        if (vsTenantVrfForL3Vni == null
            || !vsTenantVrfForL3Vni.getAddressFamilies().containsKey(AddressFamily.IPV4_UNICAST)) {
          continue;
        }
        RouteDistinguisher rd =
            Optional.ofNullable(vsTenantVrfForL3Vni.getRd())
                .map(RouteDistinguisherOrAuto::getRouteDistinguisher)
                .orElse(null);

        ExtendedCommunityOrAuto exportRtOrAuto =
            vsTenantVrfForL3Vni
                .getAddressFamilies()
                .get(AddressFamily.IPV4_UNICAST)
                .getExportRtEvpn();
        if (exportRtOrAuto == null) {
          // export route target is not present as auto and neither is user-defined, no L3 routes
          // (IP-routes)
          // will be exported from this VRF. Assuming this to be an invalid L3 VNI configuration
          // for lack of explicit doc from Cisco. (Cisco auto-generates it in common cases)
          warnings.redFlag(
              String.format(
                  "No export route-target defined for L3 VNI '%s', no L3 routes will be exported",
                  l3Vni.getVni()));
          continue;
        }
        ExtendedCommunityOrAuto importRtOrAuto =
            vsTenantVrfForL3Vni
                .getAddressFamilies()
                .get(AddressFamily.IPV4_UNICAST)
                .getImportRtEvpn();
        if (importRtOrAuto == null) {
          // import route target is not present as auto and neither is user-defined, no L3 routes
          // (IP-routes)
          // will be imported into this VRF. Assuming this to be an invalid L3 VNI configuration
          // for lack of explicit doc from Cisco. (Cisco auto-generates it in common cases)
          warnings.redFlag(
              String.format(
                  "No import route-target defined for L3 VNI '%s', no L3 routes will be imported",
                  l3Vni.getVni()));
          continue;
        }
        layer3Vnis.add(
            Layer3VniConfig.builder()
                .setVni(l3Vni.getVni())
                .setVrf(tenantVrf.getName())
                .setImportRouteTarget(
                    importRtOrAuto.isAuto()
                        ? toRouteTarget(localAs, l3Vni.getVni()).matchString()
                        : importRtOrAuto.getExtendedCommunity().matchString())
                .setRouteDistinguisher(
                    firstNonNull(
                        rd,
                        RouteDistinguisher.from(
                            viBgpProcess.getRouterId(), vsTenantVrfForL3Vni.getId())))
                .setRouteTarget(
                    exportRtOrAuto.isAuto()
                        ? toRouteTarget(localAs, l3Vni.getVni())
                        : exportRtOrAuto.getExtendedCommunity())
                // NXOS advertises EVPN type-5 always
                .setAdvertiseV4Unicast(true)
                .build());
      }
    }
    return layer3Vnis.build();
  }

  /**
   * Gets the MAC-VRF ID for the supplied L2 VNI as per
   * https://www.cisco.com/c/en/us/td/docs/switches/datacenter/nexus9000/sw/7-x/vxlan/configuration/guide/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x_chapter_0100.html
   */
  @Nullable
  private static Integer getMacVrfIdForL2Vni(CiscoNxosConfiguration vsConfig, Integer l2Vni) {
    Integer vlanNumber =
        vsConfig.getVlans().values().stream()
            .filter(vlan -> l2Vni.equals(vlan.getVni()))
            .findFirst()
            .map(Vlan::getId)
            .orElse(null);

    if (vlanNumber == null) {
      return null;
    }

    return MAC_VRF_OFFSET + vlanNumber;
  }

  /** Get the tenant VRF associated with a L3 VNI */
  @Nullable
  static org.batfish.representation.cisco_nxos.Vrf getVrfForL3Vni(
      Map<String, org.batfish.representation.cisco_nxos.Vrf> vrfs, int vni) {
    return vrfs.values().stream()
        .filter(vrf -> vrf.getVni() != null && vrf.getVni() == vni)
        .findFirst()
        .orElse(null);
  }

  /**
   * Convert AS number and VNI to an extended route target community as per type 0 route
   * distinguisher standard (2byte : 4 byte). So, converts AS number to 2 byte and uses VNI as it is
   * since it is already 3 bytes.
   *
   * <p>See <a
   * href="https://www.cisco.com/c/en/us/td/docs/switches/datacenter/nexus9000/sw/7-x/vxlan/configuration/guide/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x/b_Cisco_Nexus_9000_Series_NX-OS_VXLAN_Configuration_Guide_7x_chapter_0100.html">
   * Cisco NX-OS documentation</a> for detailed explanation.
   */
  @Nonnull
  private static ExtendedCommunity toRouteTarget(long asn, long vni) {
    return ExtendedCommunity.target(asn & 0xFFFFL, vni);
  }

  /**
   * Create and return an export policy from a list of statements. The policy is auto-added to the
   * given {@link Configuration}.
   */
  private static RoutingPolicy createExportPolicyFromStatements(
      String policyName, List<Statement> statements, Configuration configuration) {
    return RoutingPolicy.builder()
        .setOwner(configuration)
        .setName(policyName)
        .setStatements(statements)
        .build();
  }

  /**
   * Create and return an import policy for the given address family. The policy is auto-added to
   * the given {@link Configuration}.
   */
  private static RoutingPolicy createNeighborImportPolicy(
      Configuration c, String policyName, BgpVrfNeighborAddressFamilyConfiguration af, Warnings w) {
    RoutingPolicy.Builder ret = RoutingPolicy.builder().setOwner(c).setName(policyName);

    String routeMap = af.getInboundRouteMap();
    String prefixList = af.getInboundPrefixList();

    // Use inbound route-map or prefix-list if set, preferring route-map for now
    if (routeMap != null) {
      ret.addStatement(Statements.SetWriteIntermediateBgpAttributes.toStaticStatement());
      ret.addStatement(
          new If(
              new CallExpr(routeMapOrRejectAll(af.getInboundRouteMap(), c)),
              ImmutableList.of(
                  Statements.SetReadIntermediateBgpAttributes.toStaticStatement(),
                  new SetDefaultTag(AsnValue.of(AutoAs.instance())),
                  Statements.ExitAccept.toStaticStatement())));
      ret.addStatement(Statements.ExitReject.toStaticStatement());

      // TODO Support using multiple filters in import policies
      if (prefixList != null) {
        w.redFlag(
            "Batfish does not support configuring more than one filter"
                + " (route-map/prefix-list) for incoming BGP routes. When this occurs,"
                + " only the route-map will be used, or the prefix-list if no route-map is"
                + " configured.");
      }
    } else if (prefixList != null) {
      ret.addStatement(new SetTag(AsnValue.of(AutoAs.instance())));
      ret.addStatement(getPrefixListStatement(c, prefixList));
    } else {
      // Accept everything if neither is set
      ret.addStatement(new SetTag(AsnValue.of(AutoAs.instance())));
      ret.addStatement(Statements.ExitAccept.toStaticStatement());
    }

    return ret.build();
  }

  /**
   * Get the statement for the specified prefix-list used as a destination-network filter for BGP
   * routes. If the prefix-list is undefined, the statement will simply accept all destination
   * networks.
   */
  private static Statement getPrefixListStatement(Configuration c, String prefixList) {
    // An undefined prefix-list is treated as matching everything in this context
    if (!c.getRouteFilterLists().containsKey(prefixList)) {
      return Statements.ExitAccept.toStaticStatement();
    }

    return new If(
        new MatchPrefixSet(DestinationNetwork.instance(), new NamedPrefixSet(prefixList)),
        ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
        ImmutableList.of(Statements.ExitReject.toStaticStatement()));
  }

  /** Get address family capabilities for IPv4 and L2VPN address families */
  private static AddressFamilyCapabilities getAddressFamilyCapabilities(
      BgpVrfNeighborAddressFamilyConfiguration naf, boolean inheritedSupressInactive) {
    return AddressFamilyCapabilities.builder()
        .setAdvertiseInactive(!firstNonNull(naf.getSuppressInactive(), inheritedSupressInactive))
        .setAllowLocalAsIn(firstNonNull(naf.getAllowAsIn(), 0) > 0)
        .setAllowRemoteAsOut(
            firstNonNull(naf.getDisablePeerAsCheck(), Boolean.FALSE)
                ? AllowRemoteAsOutMode.ALWAYS
                : AllowRemoteAsOutMode.EXCEPT_FIRST)
        .setSendCommunity(firstNonNull(naf.getSendCommunityStandard(), Boolean.FALSE))
        .setSendExtendedCommunity(firstNonNull(naf.getSendCommunityExtended(), Boolean.FALSE))
        .build();
  }

  /**
   * Implements the NX-OS behavior for undefined route-maps when used in BGP import/export policies.
   *
   * <p>Always returns {@code null} when given a null {@code mapName}, and non-null otherwise.
   */
  private static @Nullable String routeMapOrRejectAll(@Nullable String mapName, Configuration c) {
    if (mapName == null || c.getRoutingPolicies().containsKey(mapName)) {
      return mapName;
    }
    String undefinedName = mapName + "~undefined";
    if (!c.getRoutingPolicies().containsKey(undefinedName)) {
      // For undefined route-map, generate a route-map that denies everything.
      RoutingPolicy.builder()
          .setName(undefinedName)
          .addStatement(ROUTE_MAP_DENY_STATEMENT)
          .setOwner(c)
          .build();
    }
    return undefinedName;
  }

  /** Get export statements for EVPN address family */
  private static List<Statement> getExportStatementsForEvpn(
      Configuration configuration,
      BgpVrfNeighborAddressFamilyConfiguration naf,
      BgpVrfNeighborConfiguration neighbor,
      Warnings w) {
    ImmutableList.Builder<Statement> statementsBuilder = ImmutableList.builder();

    if (neighbor.getRemovePrivateAs() != null) {
      statementsBuilder.add(RemovePrivateAs.toStaticStatement());
    }

    // Peer-specific export policy
    Conjunction peerExportGuard = new Conjunction();

    // Always export BGP or IBGP routes
    List<BooleanExpr> peerExportConditions = peerExportGuard.getConjuncts();
    peerExportConditions.add(new MatchProtocol(RoutingProtocol.BGP, RoutingProtocol.IBGP));

    String outboundMap = naf.getOutboundRouteMap();
    String outboundPrefixList = naf.getOutboundPrefixList();

    // Export policy generated for outbound route-map (if any)
    if (outboundMap != null) {
      peerExportConditions.add(new CallExpr(routeMapOrRejectAll(outboundMap, configuration)));

      // TODO Support using multiple filters in import policies
      if (outboundPrefixList != null) {
        w.redFlag(
            "Batfish does not support configuring more than one filter"
                + " (route-map/prefix-list) for outgoing BGP routes. When this occurs,"
                + " only the route-map will be used.");
      }
    } else if (outboundPrefixList != null) {
      statementsBuilder.add(getPrefixListStatement(configuration, outboundPrefixList));
    }

    return statementsBuilder
        .add(
            new If(
                "peer-export policy main conditional: exitAccept if true / exitReject if false",
                peerExportGuard,
                ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                ImmutableList.of(Statements.ExitReject.toStaticStatement())))
        .build();
  }

  /** Get export statements for IPv4 address family */
  private static List<Statement> getExportStatementsForIpv4(
      Configuration configuration,
      BgpVrfNeighborAddressFamilyConfiguration naf,
      BgpVrfNeighborConfiguration neighbor,
      BgpPeerConfig.Builder<?, ?> newNeighborBuilder,
      String vrfName,
      Warnings w) {
    ImmutableList.Builder<Statement> statementsBuilder = ImmutableList.builder();

    // Next Hop Self
    if (firstNonNull(naf.getNextHopSelf(), Boolean.FALSE)) {
      statementsBuilder.add(new SetNextHop(SelfNextHop.getInstance()));
    }
    if (neighbor.getRemovePrivateAs() != null) {
      // TODO(handle different types of RemovePrivateAs)
      statementsBuilder.add(RemovePrivateAs.toStaticStatement());
    }

    // If defaultOriginate is set, generate route and default route export policy. Default route
    // will match this policy and get exported without going through the rest of the export policy.
    // TODO Verify that nextHopSelf and removePrivateAs settings apply to default-originate route.
    if (firstNonNull(naf.getDefaultOriginate(), Boolean.FALSE)) {
      initBgpDefaultRouteExportPolicy(configuration);
      statementsBuilder.add(
          new If(
              "Export default route from peer with default-originate configured",
              new CallExpr(generatedBgpDefaultRouteExportPolicyName(true)),
              singletonList(Statements.ReturnTrue.toStaticStatement()),
              ImmutableList.of()));

      GeneratedRoute defaultRoute =
          GeneratedRoute.builder()
              .setNetwork(Prefix.ZERO)
              .setAdmin(MAX_ADMINISTRATIVE_COST)
              .setGenerationPolicy(naf.getDefaultOriginateMap())
              .build();
      newNeighborBuilder.setGeneratedRoutes(ImmutableSet.of(defaultRoute));
    }

    // Peer-specific export policy, after matching default-originate route.
    Conjunction peerExportGuard = new Conjunction();

    // Common BGP export policy
    List<BooleanExpr> peerExportConditions = peerExportGuard.getConjuncts();
    peerExportConditions.add(new CallExpr(generatedBgpCommonExportPolicyName(vrfName)));

    String outboundMap = naf.getOutboundRouteMap();
    String outboundPrefixList = naf.getOutboundPrefixList();

    // Export policy generated for route-map (if any)
    if (outboundMap != null) {
      peerExportConditions.add(new CallExpr(routeMapOrRejectAll(outboundMap, configuration)));

      // TODO Support using multiple filters in import policies
      if (outboundPrefixList != null) {
        w.redFlag(
            "Batfish does not support configuring more than one filter"
                + " (route-map/prefix-list) for outgoing BGP routes. When this occurs,"
                + " only the route-map will be used.");
      }
    } else if (outboundPrefixList != null) {
      statementsBuilder.add(getPrefixListStatement(configuration, outboundPrefixList));
    }

    return statementsBuilder
        .add(
            new If(
                "peer-export policy main conditional: exitAccept if true / exitReject if false",
                peerExportGuard,
                ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                ImmutableList.of(Statements.ExitReject.toStaticStatement())))
        .build();
  }

  /**
   * Initializes export policy for default routes if it doesn't already exist. This policy is the
   * same across BGP processes, so only one is created for each configuration.
   */
  static void initBgpDefaultRouteExportPolicy(Configuration c) {
    String defaultRouteExportPolicyName = generatedBgpDefaultRouteExportPolicyName(true);
    if (!c.getRoutingPolicies().containsKey(defaultRouteExportPolicyName)) {
      RoutingPolicy.builder()
          .setOwner(c)
          .setName(defaultRouteExportPolicyName)
          .addStatement(
              new If(
                  new Conjunction(
                      ImmutableList.of(
                          Common.matchDefaultRoute(),
                          new MatchProtocol(RoutingProtocol.AGGREGATE))),
                  ImmutableList.of(
                      new SetOrigin(new LiteralOrigin(OriginType.IGP, null)),
                      Statements.ReturnTrue.toStaticStatement())))
          .addStatement(Statements.ReturnFalse.toStaticStatement())
          .build();
    }
  }

  private static String getTextDesc(Ip ip, Vrf v) {
    return String.format("BGP neighbor %s in vrf %s", ip.toString(), v.getName());
  }

  private static String getTextDesc(Prefix prefix, Vrf v) {
    return String.format("BGP neighbor %s in vrf %s", prefix.toString(), v.getName());
  }

  private Conversions() {} // prevent instantiation of utility class.
}
