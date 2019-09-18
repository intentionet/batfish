package org.batfish.representation.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.singletonList;
import static org.batfish.representation.cisco.CiscoConfiguration.MATCH_DEFAULT_ROUTE;
import static org.batfish.representation.cisco.CiscoConfiguration.MAX_ADMINISTRATIVE_COST;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpCommonExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeNxosBgpDefaultRouteExportPolicyName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.representation.cisco.eos.AristaBgpNeighborAddressFamily;
import org.batfish.representation.cisco.eos.AristaBgpProcess;
import org.batfish.representation.cisco.eos.AristaBgpV4Neighbor;
import org.batfish.representation.cisco.eos.AristaBgpVrf;
import org.batfish.representation.cisco.eos.AristaBgpVrfIpv4UnicastAddressFamily;

/**
 * A utility class for converting between Arista EOS configurations and the Batfish
 * vendor-independent {@link org.batfish.datamodel}.
 */
@ParametersAreNonnullByDefault
final class AristaConversions {
  /** Computes the router ID. */
  @Nonnull
  static Ip getBgpRouterId(AristaBgpVrf vrfConfig, Vrf vrf, Warnings w) {
    // If Router ID is configured in the VRF-Specific BGP config, it always wins.
    if (vrfConfig.getRouterId() != null) {
      return vrfConfig.getRouterId();
    }

    String messageBase =
        String.format(
            "Router-id is not manually configured for BGP process in VRF %s", vrf.getName());

    // Otherwise, Router ID is defined based on the interfaces in the VRF that have IP addresses.
    // EOS does NOT use shutdown interfaces to configure router-id.
    Map<String, Interface> interfaceMap =
        vrf.getInterfaces().entrySet().stream()
            .filter(e -> e.getValue().getActive())
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

    // Next, EOS prefers highest loopback IP.
    Collection<Interface> interfaces = interfaceMap.values();
    Optional<Ip> highestLoopback =
        interfaces.stream()
            .filter(i -> i.getName().startsWith("Loopback"))
            .map(Interface::getConcreteAddress)
            .map(ConcreteInterfaceAddress::getIp)
            .max(Comparator.naturalOrder());
    if (highestLoopback.isPresent()) {
      return highestLoopback.get();
    }

    // Finally, EOS uses the highest non-loopback interface IP defined in the vrf.
    Optional<Ip> highestIp =
        interfaces.stream()
            .map(Interface::getConcreteAddress)
            .map(ConcreteInterfaceAddress::getIp)
            .max(Comparator.naturalOrder());
    assert highestIp.isPresent(); // This cannot happen if interfaces is non-empty.
    return highestIp.get();
  }

  private static boolean isActive(
      String name, AristaBgpVrf vrf, AristaBgpV4Neighbor neighbor, Warnings w) {
    if (firstNonNull(neighbor.getShutdown(), Boolean.FALSE)) {
      return false;
    }

    // No active address family that we support.
    boolean v4 =
        Optional.ofNullable(vrf.getV4UnicastAf())
            .map(af -> af.getNeighbor(neighbor.getIp()))
            .map(AristaBgpNeighborAddressFamily::getActivate)
            .orElse(Boolean.FALSE);
    boolean evpn = false; // TODO
    //        Optional.ofNullable(vrf.getEvpnAf())
    //            .map(af -> af.getNeighbor(neighbor.getIp()))
    //            .map(AristaBgpNeighborAddressFamily::getActivate)
    //            .orElse(Boolean.FALSE);
    if (!v4 && !evpn) {
      w.redFlag("No supported address-family configured for " + name);
      return false;
    }

    // No remote AS set.
    if (neighbor.getRemoteAs() == null) {
      w.redFlag("No remote-as configured for " + name);
    }

    return true;
  }

  @Nonnull
  static Map<Prefix, BgpActivePeerConfig> getNeighbors(
      Configuration c,
      Vrf vrf,
      BgpProcess proc,
      AristaBgpProcess bgpConfig,
      AristaBgpVrf bgpVrf,
      Warnings warnings) {

    // Handle default activation for v4 unicast by auto-populating all PGs and v4 neighbors in it.
    if (bgpVrf.getDefaultIpv4Unicast()) {
      bgpVrf.getOrCreateV4UnicastAf();
    }

    return bgpVrf.getV4neighbors().entrySet().stream()
        .peek(e -> e.getValue().inherit(bgpConfig, bgpVrf, warnings))
        .filter(e -> isActive(getTextDesc(e.getKey(), vrf), bgpVrf, e.getValue(), warnings))
        .collect(
            ImmutableMap.toImmutableMap(
                e -> Prefix.create(e.getKey(), Prefix.MAX_PREFIX_LENGTH),
                e ->
                    (BgpActivePeerConfig)
                        AristaConversions.toBgpNeighbor(
                            c,
                            vrf,
                            proc,
                            Prefix.create(e.getKey(), Prefix.MAX_PREFIX_LENGTH),
                            bgpConfig,
                            bgpVrf,
                            e.getValue(),
                            false,
                            warnings)));
  }

  //  @Nonnull
  //  static Map<Prefix, BgpPassivePeerConfig> getPassiveNeighbors(
  //      Configuration c,
  //      Vrf vrf,
  //      BgpProcess proc,
  //      AristaBgpProcess bgpConfig,
  //      AristaBgpVrf bgpVrf,
  //      Warnings warnings) {
  //    return bgpVrf.getPassiveNeighbors().entrySet().stream()
  //        .peek(e -> e.getValue().doInherit(bgpConfig, warnings))
  //        .filter(e -> isActive(getTextDesc(e.getKey(), vrf), e.getValue(), warnings))
  //        .collect(
  //            ImmutableMap.toImmutableMap(
  //                Entry::getKey,
  //                e ->
  //                    (BgpPassivePeerConfig)
  //                        AristaConversions.toBgpNeighbor(
  //                            c,
  //                            vrf,
  //                            proc,
  //                            e.getKey(),
  //                            bgpConfig,
  //                            b,
  //                            e.getValue(),
  //                            true,
  //                            warnings)));
  //  }

  @Nullable
  private static Ip computeUpdateSource(
      Vrf vrf, Prefix prefix, AristaBgpV4Neighbor neighbor, boolean dynamic, Warnings warnings) {
    String updateSourceInterface = neighbor.getUpdateSource();
    if (updateSourceInterface != null) {
      Interface iface = vrf.getInterfaces().get(updateSourceInterface);
      if (iface == null) {
        warnings.redFlag(
            String.format(
                "BGP neighbor %s in vrf %s: configured update-source %s does not exist or "
                    + "is not associated with this vrf",
                dynamic ? prefix : prefix.getStartIp(), vrf.getName(), updateSourceInterface));
        return null;
      }
      ConcreteInterfaceAddress address = iface.getConcreteAddress();
      if (address == null) {
        warnings.redFlag(
            String.format(
                "BGP neighbor %s in vrf %s: configured update-source %s has no IP address",
                dynamic ? prefix : prefix.getStartIp(), vrf.getName(), updateSourceInterface));
        return null;
      }
      return address.getIp();
    } else if (dynamic) {
      return Ip.AUTO;
    }
    Optional<Ip> firstMatchingInterfaceAddress =
        vrf.getInterfaces().values().stream()
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
            prefix.getStartIp(), vrf.getName()));
    return null;
  }

  @Nonnull
  private static BgpPeerConfig toBgpNeighbor(
      Configuration c,
      Vrf vrf,
      BgpProcess proc,
      Prefix prefix,
      AristaBgpProcess bgpConfig,
      AristaBgpVrf vrfConfig,
      AristaBgpV4Neighbor neighbor,
      boolean dynamic,
      Warnings warnings) {

    BgpPeerConfig.Builder<?, ?> newNeighborBuilder;
    if (dynamic) {
      newNeighborBuilder =
          BgpPassivePeerConfig.builder()
              .setRemoteAsns(
                  Optional.ofNullable(neighbor.getRemoteAs())
                      .map(LongSpace::of)
                      .orElse(LongSpace.EMPTY))
              .setPeerPrefix(prefix);
    } else {
      newNeighborBuilder =
          BgpActivePeerConfig.builder()
              .setRemoteAs(neighbor.getRemoteAs())
              .setPeerAddress(prefix.getStartIp());
    }

    // TODO:
    newNeighborBuilder.setClusterId(proc.getRouterId().asLong());
    //        firstNonNull(vrfConfig.getClusterId(), proc.getRouterId()).asLong());

    newNeighborBuilder.setDescription(neighbor.getDescription());

    newNeighborBuilder.setEbgpMultihop(firstNonNull(neighbor.getEbgpMultihop(), 0) > 1);

    // todo
    newNeighborBuilder.setEnforceFirstAs(firstNonNull(neighbor.getEnforceFirstAs(), Boolean.FALSE));

    if (neighbor.getPeerGroup() != null) {
      newNeighborBuilder.setGroup(neighbor.getPeerGroup());
    }

    if (neighbor.getLocalAs() != null) {
      newNeighborBuilder.setLocalAs(neighbor.getLocalAs());
    } else if (vrfConfig.getLocalAs() != null) {
      newNeighborBuilder.setLocalAs(vrfConfig.getLocalAs());
    } else {
      newNeighborBuilder.setLocalAs(bgpConfig.getAsn());
    }

    newNeighborBuilder.setLocalIp(computeUpdateSource(vrf, prefix, neighbor, dynamic, warnings));

    @Nullable AristaBgpVrfIpv4UnicastAddressFamily af4 = vrfConfig.getV4UnicastAf();
    @Nullable
    AristaBgpNeighborAddressFamily naf4 = af4 == null ? null : af4.getNeighbor(neighbor.getIp());
    Ipv4UnicastAddressFamily.Builder ipv4FamilyBuilder = Ipv4UnicastAddressFamily.builder();
    boolean v4Enabled = naf4 != null && firstNonNull(naf4.getActivate(), Boolean.FALSE);

    if (v4Enabled) {
      ipv4FamilyBuilder.setAddressFamilyCapabilities(
          AddressFamilyCapabilities.builder()
              .setAdvertiseInactive(Boolean.FALSE) // todo
              .setAllowLocalAsIn(firstNonNull(neighbor.getAllowAsIn(), 0) > 0)
              .setAllowRemoteAsOut(true) // this is always true on Arista
              .setSendCommunity(firstNonNull(neighbor.getSendCommunity(), Boolean.FALSE))
              .setSendExtendedCommunity(firstNonNull(neighbor.getSendCommunity(), Boolean.FALSE))
              .build());

      String inboundMap = naf4.getRouteMapIn();
      ipv4FamilyBuilder
          .setImportPolicy(
              inboundMap != null && c.getRoutingPolicies().containsKey(inboundMap)
                  ? inboundMap
                  : null)
          .setRouteReflectorClient(firstNonNull(/* todo */ null, Boolean.FALSE));
    }

    // Export policy
    List<Statement> exportStatements = new LinkedList<>();
    if (firstNonNull(neighbor.getNextHopSelf(), Boolean.FALSE)) {
      exportStatements.add(new SetNextHop(SelfNextHop.getInstance()));
    }
    //    if (neighbor.getRemovePrivateAs() != null) {
    //      // TODO(handle different types of RemovePrivateAs)
    //      exportStatements.add(RemovePrivateAs.toStaticStatement());
    //    }

    // If defaultOriginate is set, generate route and default route export policy. Default route
    // will match this policy and get exported without going through the rest of the export policy.
    // TODO Verify that nextHopSelf and removePrivateAs settings apply to default-originate route.
    if (v4Enabled && naf4.getDefaultOriginate() != null) {
      initBgpDefaultRouteExportPolicy(c);
      exportStatements.add(
          new If(
              "Export default route from peer with default-originate configured",
              new CallExpr(computeNxosBgpDefaultRouteExportPolicyName(true)),
              singletonList(Statements.ReturnTrue.toStaticStatement()),
              ImmutableList.of()));

      GeneratedRoute defaultRoute =
          GeneratedRoute.builder()
              .setNetwork(Prefix.ZERO)
              .setAdmin(MAX_ADMINISTRATIVE_COST)
              .setGenerationPolicy(naf4.getDefaultOriginate().getRouteMap())
              .build();
      newNeighborBuilder.setGeneratedRoutes(ImmutableSet.of(defaultRoute));
    }

    // Peer-specific export policy, after matching default-originate route.
    Conjunction peerExportGuard = new Conjunction();
    List<BooleanExpr> peerExportConditions = peerExportGuard.getConjuncts();
    exportStatements.add(
        new If(
            "peer-export policy main conditional: exitAccept if true / exitReject if false",
            peerExportGuard,
            ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
            ImmutableList.of(Statements.ExitReject.toStaticStatement())));
    peerExportConditions.add(new CallExpr(computeBgpCommonExportPolicyName(vrf.getName())));

    if (v4Enabled) {
      String outboundMap = naf4.getRouteMapOut();
      if (outboundMap != null && c.getRoutingPolicies().containsKey(outboundMap)) {
        peerExportConditions.add(new CallExpr(outboundMap));
      }
    }

    RoutingPolicy exportPolicy =
        new RoutingPolicy(
            computeBgpPeerExportPolicyName(
                vrf.getName(), dynamic ? prefix.toString() : prefix.getStartIp().toString()),
            c);
    exportPolicy.setStatements(exportStatements);
    c.getRoutingPolicies().put(exportPolicy.getName(), exportPolicy);
    if (v4Enabled) {
      ipv4FamilyBuilder.setExportPolicy(exportPolicy.getName());
      newNeighborBuilder.setIpv4UnicastAddressFamily(ipv4FamilyBuilder.build());
    }

    return newNeighborBuilder.build();
  }

  /**
   * Initializes export policy for default routes if it doesn't already exist. This policy is the
   * same across BGP processes, so only one is created for each configuration.
   */
  static void initBgpDefaultRouteExportPolicy(Configuration c) {
    String defaultRouteExportPolicyName = computeNxosBgpDefaultRouteExportPolicyName(true);
    if (!c.getRoutingPolicies().containsKey(defaultRouteExportPolicyName)) {
      RoutingPolicy.builder()
          .setOwner(c)
          .setName(defaultRouteExportPolicyName)
          .addStatement(
              new If(
                  new Conjunction(
                      ImmutableList.of(
                          MATCH_DEFAULT_ROUTE, new MatchProtocol(RoutingProtocol.AGGREGATE))),
                  ImmutableList.of(
                      new SetOrigin(new LiteralOrigin(OriginType.IGP, null)),
                      Statements.ReturnTrue.toStaticStatement())))
          .addStatement(Statements.ReturnFalse.toStaticStatement())
          .build();
    }
  }

  @Nonnull
  static Optional<Vrf> getVrfForVlan(Configuration c, int vlan) {
    return c.getVrfs().values().stream()
        .filter(vrf -> vrf.getInterfaceNames().contains(String.format("Vlan%d", vlan)))
        .findFirst();
  }

  private static String getTextDesc(Ip ip, Vrf v) {
    return String.format("BGP neighbor %s in vrf %s", ip.toString(), v.getName());
  }

  private static String getTextDesc(Prefix prefix, Vrf v) {
    return String.format("BGP neighbor %s in vrf %s", prefix.toString(), v.getName());
  }

  private AristaConversions() {} // prevent instantiation of utility class.
}
