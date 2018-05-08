package org.batfish.representation.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.routing_policy.statement.Statements.RemovePrivateAs;
import static org.batfish.representation.cisco.CiscoConfiguration.MATCH_DEFAULT_ROUTE;
import static org.batfish.representation.cisco.CiscoConfiguration.MAX_ADMINISTRATIVE_COST;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpCommonExportPolicyName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.representation.cisco.nx.CiscoNxBgpGlobalConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfNeighborAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfNeighborConfiguration;

/**
 * A utility class for converting between Cisco NX-OS configurations and the Batfish
 * vendor-independent {@link org.batfish.datamodel}.
 */
@ParametersAreNonnullByDefault
final class CiscoNxConversions {
  /** Computes the router ID on Cisco NX-OS. */
  // See CiscoNxosTest#testRouterId for a test that is verifiable using GNS3.
  @Nonnull
  static Ip getNxBgpRouterId(CiscoNxBgpVrfConfiguration vrfConfig, Vrf vrf, Warnings w) {
    // If Router ID is configured in the VRF-Specific BGP config, it always wins.
    if (vrfConfig.getRouterId() != null) {
      return vrfConfig.getRouterId();
    }

    String messageBase =
        String.format(
            "Router-id is not manually configured for BGP process in VRF %s", vrf.getName());

    // Otherwise, Router ID is defined based on the interfaces in the VRF that have IP addresses.
    // NX-OS does use shutdown interfaces to configure router-id.
    Map<String, Interface> interfaceMap =
        vrf.getInterfaces()
            .entrySet()
            .stream()
            .filter(e -> e.getValue().getAddress() != null)
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

    // Next, NX-OS prefers the IP of Loopback0 if one exists.
    Interface loopback0 = interfaceMap.get("Loopback0");
    if (loopback0 != null) {
      w.redFlag(String.format("%s. Using the IP address of Loopback0", messageBase));
      return loopback0.getAddress().getIp();
    }

    // Next, NX-OS prefers "first" loopback interface. NX-OS is non-deterministic, but we will
    // enforce determinism by always choosing the smallest loopback IP.
    Collection<Interface> interfaces = interfaceMap.values();
    Optional<Ip> lowestLoopback =
        interfaces
            .stream()
            .filter(i -> i.getName().startsWith("Loopback"))
            .map(Interface::getAddress)
            .map(InterfaceAddress::getIp)
            .min(Comparator.naturalOrder());
    if (lowestLoopback.isPresent()) {
      w.redFlag(
          String.format(
              "%s. Making a non-deterministic choice from associated loopbacks", messageBase));
      return lowestLoopback.get();
    }

    // Finally, NX uses the first non-loopback interface defined in the vrf, assuming no loopback
    // addresses with IP address are present in the vrf. NX-OS is non-deterministic, by we will
    // enforce determinism by always choosing the smallest interface IP.
    Optional<Ip> lowestIp =
        interfaces
            .stream()
            .map(Interface::getAddress)
            .map(InterfaceAddress::getIp)
            .min(Comparator.naturalOrder());
    w.redFlag(
        String.format(
            "%s. Making a non-deterministic choice from associated interfaces", messageBase));
    assert lowestIp.isPresent(); // This cannot happen if interfaces is non-empty.
    return lowestIp.get();
  }

  private static boolean isActive(
      String name, CiscoNxBgpVrfNeighborConfiguration neighbor, Warnings w) {
    // Shutdown
    if (firstNonNull(neighbor.getShutdown(), Boolean.FALSE)) {
      return false;
    }

    // No active address family that we support.
    if (neighbor.getIpv4UnicastAddressFamily() == null
        && neighbor.getIpv6UnicastAddressFamily() == null) {
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
  static Map<Ip, BgpNeighbor> getNeighbors(
      Configuration c,
      Vrf vrf,
      org.batfish.datamodel.BgpProcess proc,
      CiscoNxBgpGlobalConfiguration bgpConfig,
      CiscoNxBgpVrfConfiguration nxBgpVrf,
      Warnings warnings) {
    return nxBgpVrf
        .getNeighbors()
        .entrySet()
        .stream()
        .peek(e -> e.getValue().doInherit(bgpConfig, warnings))
        .filter(e -> isActive(getTextDesc(e.getKey(), vrf), e.getValue(), warnings))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                e ->
                    CiscoNxConversions.toBgpNeighbor(
                        c,
                        vrf,
                        proc,
                        new Prefix(e.getKey(), Prefix.MAX_PREFIX_LENGTH),
                        bgpConfig,
                        nxBgpVrf,
                        e.getValue(),
                        false,
                        warnings)));
  }

  @Nonnull
  static Map<Prefix, BgpNeighbor> getPassiveNeighbors(
      Configuration c,
      Vrf vrf,
      org.batfish.datamodel.BgpProcess proc,
      CiscoNxBgpGlobalConfiguration bgpConfig,
      CiscoNxBgpVrfConfiguration nxBgpVrf,
      Warnings warnings) {
    return nxBgpVrf
        .getPassiveNeighbors()
        .entrySet()
        .stream()
        .peek(e -> e.getValue().doInherit(bgpConfig, warnings))
        .filter(e -> isActive(getTextDesc(e.getKey(), vrf), e.getValue(), warnings))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                e ->
                    CiscoNxConversions.toBgpNeighbor(
                        c,
                        vrf,
                        proc,
                        e.getKey(),
                        bgpConfig,
                        nxBgpVrf,
                        e.getValue(),
                        true,
                        warnings)));
  }

  /** Returns {@code asn} cast to {@code int}, warning if the AS number given is a 4-byte AS. */
  private static int coerceTwoByteAsn(
      Warnings warnings, String vrfName, Prefix prefix, String type, long asn) {
    if (asn >= (1 << 16)) {
      warnings.redFlag(
          String.format(
              "4-byte AS numbers are not fully supported: vrf %s neighbor %s %s-as %d",
              vrfName, prefix, type, asn));
    }
    return (int) asn;
  }

  @Nullable
  private static Ip computeUpdateSource(
      Vrf vrf,
      Prefix prefix,
      CiscoNxBgpVrfNeighborConfiguration neighbor,
      boolean dynamic,
      Warnings warnings) {
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
      InterfaceAddress address = iface.getAddress();
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
        vrf.getInterfaces()
            .values()
            .stream()
            .flatMap(i -> i.getAllAddresses().stream())
            .filter(ia -> ia != null && ia.getPrefix().containsIp(prefix.getStartIp()))
            .map(InterfaceAddress::getIp)
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
  private static BgpNeighbor toBgpNeighbor(
      Configuration c,
      Vrf vrf,
      BgpProcess proc,
      Prefix prefix,
      CiscoNxBgpGlobalConfiguration bgpConfig,
      CiscoNxBgpVrfConfiguration vrfConfig,
      CiscoNxBgpVrfNeighborConfiguration neighbor,
      boolean dynamic,
      Warnings warnings) {
    BgpNeighbor newNeighbor = new BgpNeighbor(prefix, c, dynamic);

    newNeighbor.setClusterId(firstNonNull(vrfConfig.getClusterId(), proc.getRouterId()).asLong());

    newNeighbor.setDescription(neighbor.getDescription());

    newNeighbor.setEbgpMultihop(firstNonNull(neighbor.getEbgpMultihopTtl(), 0) > 1);

    newNeighbor.setEnforceFirstAs(bgpConfig.getEnforceFirstAs());

    if (neighbor.getInheritPeer() != null) {
      newNeighbor.setGroup(neighbor.getInheritPeer());
    }

    if (neighbor.getLocalAs() != null) {
      newNeighbor.setLocalAs(
          coerceTwoByteAsn(warnings, vrf.getName(), prefix, "local", neighbor.getLocalAs()));
    } else if (vrfConfig.getLocalAs() != null) {
      newNeighbor.setLocalAs(
          coerceTwoByteAsn(warnings, vrf.getName(), prefix, "local", vrfConfig.getLocalAs()));
    } else {
      newNeighbor.setLocalAs(
          coerceTwoByteAsn(warnings, vrf.getName(), prefix, "local", bgpConfig.getLocalAs()));
    }

    newNeighbor.setLocalIp(computeUpdateSource(vrf, prefix, neighbor, dynamic, warnings));

    if (neighbor.getRemoteAs() != null) {
      newNeighbor.setRemoteAs(
          coerceTwoByteAsn(warnings, vrf.getName(), prefix, "remote", neighbor.getRemoteAs()));
    }

    newNeighbor.setVrf(vrf.getName());

    @Nullable
    CiscoNxBgpVrfNeighborAddressFamilyConfiguration naf4 = neighbor.getIpv4UnicastAddressFamily();
    @Nullable CiscoNxBgpVrfAddressFamilyConfiguration af4 = vrfConfig.getIpv4UnicastAddressFamily();
    @Nullable
    CiscoNxBgpVrfNeighborAddressFamilyConfiguration naf6 = neighbor.getIpv6UnicastAddressFamily();
    @Nullable CiscoNxBgpVrfAddressFamilyConfiguration af6 = vrfConfig.getIpv6UnicastAddressFamily();

    if (naf4 != null) {
      newNeighbor.setAdvertiseInactive(
          !firstNonNull(
              naf4.getSuppressInactive(), af4 != null ? af4.getSuppressInactive() : Boolean.FALSE));
      newNeighbor.setAllowLocalAsIn(firstNonNull(naf4.getAllowAsIn(), Boolean.FALSE));
      String inboundMap = naf4.getInboundRouteMap();
      newNeighbor.setImportPolicy(
          inboundMap != null && c.getRoutingPolicies().containsKey(inboundMap) ? inboundMap : null);
      newNeighbor.setSendCommunity(firstNonNull(naf4.getSendCommunityStandard(), Boolean.FALSE));
      newNeighbor.setRouteReflectorClient(
          firstNonNull(naf4.getRouteReflectorClient(), Boolean.FALSE));
    }

    // Export policy
    List<Statement> exportStatements = new LinkedList<>();
    if (naf4 != null && firstNonNull(naf4.getNextHopSelf(), Boolean.FALSE)) {
      exportStatements.add(new SetNextHop(SelfNextHop.getInstance(), false));
    }
    if (neighbor.getRemovePrivateAs() != null) {
      // TODO(handle different types of RemovePrivateAs)
      exportStatements.add(RemovePrivateAs.toStaticStatement());
    }
    // Peer-specific export policy.
    Conjunction peerExportGuard = new Conjunction();
    List<BooleanExpr> peerExportConditions = peerExportGuard.getConjuncts();
    exportStatements.add(
        new If(
            "peer-export policy main conditional: exitAccept if true / exitReject if false",
            peerExportGuard,
            ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
            ImmutableList.of(Statements.ExitReject.toStaticStatement())));
    List<BooleanExpr> localOrCommonOrigination = new LinkedList<>();
    localOrCommonOrigination.add(new CallExpr(computeBgpCommonExportPolicyName(vrf.getName())));
    // If `default-originate [route-map NAME]` is configured for this neighbor, generate the
    // default route and inject it.
    if (naf4 != null && firstNonNull(naf4.getDefaultOriginate(), Boolean.FALSE)) {
      GeneratedRoute defaultRoute =
          new GeneratedRoute.Builder()
              .setNetwork(Prefix.ZERO)
              .setAdmin(MAX_ADMINISTRATIVE_COST)
              .setGenerationPolicy(naf4.getDefaultOriginateMap())
              .build();
      newNeighbor.getGeneratedRoutes().add(defaultRoute);
      localOrCommonOrigination.add(MATCH_DEFAULT_ROUTE);
    }
    peerExportConditions.add(new Disjunction(localOrCommonOrigination));

    if (naf4 != null) {
      String outboundMap = naf4.getOutboundRouteMap();
      if (outboundMap != null && c.getRoutingPolicies().containsKey(outboundMap)) {
        peerExportConditions.add(new CallExpr(outboundMap));
      }
    }

    RoutingPolicy exportPolicy =
        new RoutingPolicy(
            String.format(
                "~BGP_PEER_EXPORT_POLICY:%s:%s~",
                vrf.getName(), dynamic ? prefix : prefix.getStartIp()),
            c);
    exportPolicy.setStatements(exportStatements);
    c.getRoutingPolicies().put(exportPolicy.getName(), exportPolicy);
    newNeighbor.setExportPolicy(exportPolicy.getName());

    return newNeighbor;
  }

  private static String getTextDesc(Ip ip, Vrf v) {
    return String.format("BGP neighbor %s in vrf %s", ip.toString(), v.getName());
  }

  private static String getTextDesc(Prefix prefix, Vrf v) {
    return String.format("BGP neighbor %s in vrf %s", prefix.toString(), v.getName());
  }

  private CiscoNxConversions() {} // prevent instantiation of utility class.
}
