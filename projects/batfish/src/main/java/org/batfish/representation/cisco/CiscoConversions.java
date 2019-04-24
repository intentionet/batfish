package org.batfish.representation.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.singletonList;
import static org.batfish.datamodel.Interface.INVALID_LOCAL_INTERFACE;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.representation.cisco.CiscoConfiguration.MATCH_DEFAULT_ROUTE;
import static org.batfish.representation.cisco.CiscoConfiguration.MATCH_DEFAULT_ROUTE6;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpCommonExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpDefaultRouteExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpGenerationPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerImportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeProtocolObjectGroupAclName;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.Ip6AccessListLine;
import org.batfish.datamodel.Ip6Wildcard;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecDynamicPeerConfig;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPhase2Policy;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.IpsecStaticPeerConfig;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.Route6FilterLine;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralEigrpMetric;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProcessAsn;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetEigrpMetric;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.visitors.HeaderSpaceConverter;
import org.batfish.representation.cisco.DistributeList.DistributeListFilterType;

/** Utilities that convert Cisco-specific representations to vendor-independent model. */
@ParametersAreNonnullByDefault
class CiscoConversions {

  static Ip getHighestIp(Map<String, Interface> allInterfaces) {
    Map<String, Interface> interfacesToCheck;
    Map<String, Interface> loopbackInterfaces = new HashMap<>();
    for (Entry<String, Interface> e : allInterfaces.entrySet()) {
      String ifaceName = e.getKey();
      Interface iface = e.getValue();
      if (ifaceName.toLowerCase().startsWith("loopback")
          && iface.getActive()
          && iface.getAddress() != null) {
        loopbackInterfaces.put(ifaceName, iface);
      }
    }
    if (loopbackInterfaces.isEmpty()) {
      interfacesToCheck = allInterfaces;
    } else {
      interfacesToCheck = loopbackInterfaces;
    }
    Ip highestIp = Ip.ZERO;
    for (Interface iface : interfacesToCheck.values()) {
      if (!iface.getActive()) {
        continue;
      }
      for (InterfaceAddress address : iface.getAllAddresses()) {
        Ip ip = address.getIp();
        if (highestIp.asLong() < ip.asLong()) {
          highestIp = ip;
        }
      }
    }
    return highestIp;
  }

  /**
   * Converts a {@link CryptoMapEntry} to an {@link IpsecPhase2Policy} and a list of {@link
   * IpsecPeerConfig}
   */
  private static void convertCryptoMapEntry(
      final Configuration c,
      CryptoMapEntry cryptoMapEntry,
      String cryptoMapNameSeqNumber,
      String cryptoMapName,
      Warnings w) {
    // skipping incomplete static or dynamic crypto maps
    if (!cryptoMapEntry.getDynamic()) {
      if (cryptoMapEntry.getAccessList() == null || cryptoMapEntry.getPeer() == null) {
        return;
      }
    } else {
      if (cryptoMapEntry.getAccessList() == null) {
        return;
      }
    }

    IpsecPhase2Policy ipsecPhase2Policy = toIpsecPhase2Policy(cryptoMapEntry);
    String ipsecPhase2PolicyName =
        String.format("~IPSEC_PHASE2_POLICY:%s~", cryptoMapNameSeqNumber);

    // add IPSec phase 2 policies to existing ones
    ImmutableSortedMap.Builder<String, IpsecPhase2Policy> ipsecPhase2PolicyBuilder =
        ImmutableSortedMap.naturalOrder();
    c.setIpsecPhase2Policies(
        ipsecPhase2PolicyBuilder
            .putAll(c.getIpsecPhase2Policies())
            .put(ipsecPhase2PolicyName, ipsecPhase2Policy)
            .build());

    ImmutableSortedMap.Builder<String, IpsecPeerConfig> ipsecPeerConfigsBuilder =
        ImmutableSortedMap.naturalOrder();
    c.setIpsecPeerConfigs(
        ipsecPeerConfigsBuilder
            .putAll(c.getIpsecPeerConfigs())
            .putAll(
                toIpsecPeerConfigs(
                    c,
                    cryptoMapEntry,
                    cryptoMapNameSeqNumber,
                    cryptoMapName,
                    ipsecPhase2PolicyName,
                    w))
            .build());
  }

  /**
   * Converts each crypto map entry in all crypto map sets to {@link IpsecPhase2Policy} and {@link
   * IpsecPeerConfig}s
   */
  static void convertCryptoMapSet(
      Configuration c,
      CryptoMapSet ciscoCryptoMapSet,
      Map<String, CryptoMapSet> cryptoMapSets,
      Warnings w) {
    if (ciscoCryptoMapSet.getDynamic()) {
      return;
    }
    for (CryptoMapEntry cryptoMapEntry : ciscoCryptoMapSet.getCryptoMapEntries()) {
      String nameSeqNum =
          String.format("%s:%s", cryptoMapEntry.getName(), cryptoMapEntry.getSequenceNumber());
      if (cryptoMapEntry.getReferredDynamicMapSet() != null) {
        CryptoMapSet dynamicCryptoMapSet =
            cryptoMapSets.get(cryptoMapEntry.getReferredDynamicMapSet());
        if (dynamicCryptoMapSet != null && dynamicCryptoMapSet.getDynamic()) {
          // convert all entries of the referred dynamic crypto map
          dynamicCryptoMapSet
              .getCryptoMapEntries()
              .forEach(
                  cryptoMap ->
                      convertCryptoMapEntry(
                          c,
                          cryptoMap,
                          String.format("%s:%s", nameSeqNum, cryptoMap.getSequenceNumber()),
                          cryptoMapEntry.getName(),
                          w));
        }
      } else {
        convertCryptoMapEntry(c, cryptoMapEntry, nameSeqNum, cryptoMapEntry.getName(), w);
      }
    }
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
                singletonList(Statements.ReturnTrue.toStaticStatement())))
        .build();
  }

  /**
   * Returns the name of a {@link RoutingPolicy} to be used as the BGP import policy for the given
   * {@link LeafBgpPeerGroup}, or {@code null} if no constraints are imposed on the peer's inbound
   * routes. When a nonnull policy name is returned, the corresponding policy is guaranteed to exist
   * in the given configuration's routing policies.
   */
  @Nullable
  static String generateBgpImportPolicy(
      LeafBgpPeerGroup lpg, String vrfName, Configuration c, Warnings w) {
    // TODO Support filter-list
    // https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/5816-bgpfaq-5816.html

    String inboundRouteMapName = lpg.getInboundRouteMap();
    String inboundPrefixListName = lpg.getInboundPrefixList();
    String inboundIpAccessListName = lpg.getInboundIpAccessList();

    // TODO Support using multiple filters in BGP import policies
    if (Stream.of(inboundRouteMapName, inboundPrefixListName, inboundIpAccessListName)
            .filter(Objects::nonNull)
            .count()
        > 1) {
      w.redFlag(
          "Batfish does not support configuring more than one filter (route-map/prefix-list/distribute-list) for incoming BGP routes."
              + " When this occurs, only the route-map will be used, or the prefix-list if no route-map is configured.");
    }

    // Warnings for references to undefined route-maps and prefix-lists will be surfaced elsewhere.
    if (inboundRouteMapName != null && c.getRoutingPolicies().containsKey(inboundRouteMapName)) {
      // Inbound route-map is defined. Use that as the BGP import policy.
      return inboundRouteMapName;
    }

    String exportRouteFilter = null;
    if (inboundPrefixListName != null
        && c.getRouteFilterLists().containsKey(inboundPrefixListName)) {
      exportRouteFilter = inboundPrefixListName;
    } else if (inboundIpAccessListName != null
        && c.getRouteFilterLists().containsKey(inboundIpAccessListName)) {
      exportRouteFilter = inboundIpAccessListName;
    }

    if (exportRouteFilter != null) {
      // Inbound prefix-list or distribute-list is defined. Build an import policy around it.
      String generatedImportPolicyName = computeBgpPeerImportPolicyName(vrfName, lpg.getName());
      RoutingPolicy.builder()
          .setOwner(c)
          .setName(generatedImportPolicyName)
          .addStatement(
              new If(
                  new MatchPrefixSet(
                      DestinationNetwork.instance(), new NamedPrefixSet(exportRouteFilter)),
                  ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                  ImmutableList.of(Statements.ExitReject.toStaticStatement())))
          .build();
      return generatedImportPolicyName;
    }
    // Return null to indicate no constraints were imposed on inbound BGP routes.
    return null;
  }

  /**
   * Creates a {@link RoutingPolicy} to be used as the BGP export policy for the given {@link
   * LeafBgpPeerGroup}. The generated policy is added to the given configuration's routing policies.
   */
  static void generateBgpExportPolicy(
      LeafBgpPeerGroup lpg, String vrfName, boolean ipv4, Configuration c, Warnings w) {
    List<Statement> exportPolicyStatements = new ArrayList<>();
    if (lpg.getNextHopSelf() != null && lpg.getNextHopSelf()) {
      exportPolicyStatements.add(new SetNextHop(SelfNextHop.getInstance(), false));
    }
    if (lpg.getRemovePrivateAs() != null && lpg.getRemovePrivateAs()) {
      exportPolicyStatements.add(Statements.RemovePrivateAs.toStaticStatement());
    }

    // If defaultOriginate is set, generate a default route export policy. Default route will match
    // this policy and get exported without going through the rest of the export policy.
    // TODO Verify that nextHopSelf and removePrivateAs settings apply to default-originate route.
    if (lpg.getDefaultOriginate()) {
      initBgpDefaultRouteExportPolicy(ipv4, c);
      exportPolicyStatements.add(
          new If(
              "Export default route from peer with default-originate configured",
              new CallExpr(computeBgpDefaultRouteExportPolicyName(ipv4)),
              singletonList(Statements.ReturnTrue.toStaticStatement()),
              ImmutableList.of()));
    }

    // Conditions for exporting regular routes (not spawned by default-originate)
    List<BooleanExpr> peerExportConjuncts = new ArrayList<>();
    peerExportConjuncts.add(new CallExpr(computeBgpCommonExportPolicyName(vrfName)));

    // Add constraints on export routes from configured outbound filter.
    // TODO support configuring multiple outbound filters
    String outboundPrefixListName = lpg.getOutboundPrefixList();
    String outboundRouteMapName = lpg.getOutboundRouteMap();
    String outboundIpAccessListName = lpg.getOutboundIpAccessList();
    if (Stream.of(outboundRouteMapName, outboundPrefixListName, outboundIpAccessListName)
            .filter(Objects::nonNull)
            .count()
        > 1) {
      w.redFlag(
          "Batfish does not support configuring more than one filter (route-map/prefix-list/distribute-list) for outgoing BGP routes."
              + " When this occurs, only the route-map will be used, or the prefix-list if no route-map is configured.");
    }
    if (outboundRouteMapName != null && c.getRoutingPolicies().containsKey(outboundRouteMapName)) {
      peerExportConjuncts.add(new CallExpr(outboundRouteMapName));
    } else if (outboundPrefixListName != null
        && c.getRouteFilterLists().containsKey(outboundPrefixListName)) {
      peerExportConjuncts.add(
          new MatchPrefixSet(
              DestinationNetwork.instance(), new NamedPrefixSet(outboundPrefixListName)));
    } else if (outboundIpAccessListName != null
        && c.getRouteFilterLists().containsKey(outboundIpAccessListName)) {
      peerExportConjuncts.add(
          new MatchPrefixSet(
              DestinationNetwork.instance(), new NamedPrefixSet(outboundIpAccessListName)));
    }
    exportPolicyStatements.add(
        new If(
            "peer-export policy main conditional: exitAccept if true / exitReject if false",
            new Conjunction(peerExportConjuncts),
            ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
            ImmutableList.of(Statements.ExitReject.toStaticStatement())));
    RoutingPolicy.builder()
        .setOwner(c)
        .setName(computeBgpPeerExportPolicyName(vrfName, lpg.getName()))
        .setStatements(exportPolicyStatements)
        .build();
  }

  /**
   * Initializes export policy for IPv4 or IPv6 default routes if it doesn't already exist. This
   * policy is the same across BGP processes, so only one is created for each configuration.
   *
   * @param ipv4 Whether to initialize the IPv4 or IPv6 default route export policy
   */
  static void initBgpDefaultRouteExportPolicy(boolean ipv4, Configuration c) {
    String defaultRouteExportPolicyName = computeBgpDefaultRouteExportPolicyName(ipv4);
    if (!c.getRoutingPolicies().containsKey(defaultRouteExportPolicyName)) {
      RoutingPolicy.builder()
          .setOwner(c)
          .setName(defaultRouteExportPolicyName)
          .addStatement(
              new If(
                  new Conjunction(
                      ImmutableList.of(
                          ipv4 ? MATCH_DEFAULT_ROUTE : MATCH_DEFAULT_ROUTE6,
                          new MatchProtocol(RoutingProtocol.AGGREGATE))),
                  ImmutableList.of(
                      new SetOrigin(
                          new LiteralOrigin(
                              c.getConfigurationFormat() == ConfigurationFormat.CISCO_IOS
                                  ? OriginType.IGP
                                  : OriginType.INCOMPLETE,
                              null)),
                      Statements.ReturnTrue.toStaticStatement())))
          .addStatement(Statements.ReturnFalse.toStaticStatement())
          .build();
    }
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
        new RouteFilterList("~MATCH_SUPPRESSED_SUMMARY_ONLY:" + vrfName + "~");
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

  /**
   * Computes a mapping of primary {@link Ip}s to the names of interfaces owning them. Filters out
   * the interfaces having no primary {@link InterfaceAddress}
   */
  private static Map<Ip, String> computeIpToIfaceNameMap(Map<String, Interface> interfaces) {
    Map<Ip, String> ipToIfaceNameMap = new HashMap<>();
    for (Entry<String, Interface> interfaceNameToInterface : interfaces.entrySet()) {
      interfaceNameToInterface
          .getValue()
          .getAllAddresses()
          .forEach(
              interfaceAddress -> {
                ipToIfaceNameMap.put(interfaceAddress.getIp(), interfaceNameToInterface.getKey());
              });
    }
    return ipToIfaceNameMap;
  }

  /** Resolves the interface names of the addresses used as local addresses of {@link Keyring} */
  static void resolveKeyringIfaceNames(
      Map<String, Interface> interfaces, Map<String, Keyring> keyrings) {
    Map<Ip, String> iptoIfaceName = computeIpToIfaceNameMap(interfaces);

    // setting empty string as interface name if cannot find the IP
    keyrings.values().stream()
        .filter(keyring -> keyring.getLocalAddress() != null)
        .forEach(
            keyring ->
                keyring.setLocalInterfaceName(
                    firstNonNull(
                        iptoIfaceName.get(keyring.getLocalAddress()), INVALID_LOCAL_INTERFACE)));
  }

  /**
   * Resolves the interface names of the addresses used as local addresses of {@link IsakmpProfile}
   */
  static void resolveIsakmpProfileIfaceNames(
      Map<String, Interface> interfaces, Map<String, IsakmpProfile> isakpProfiles) {
    Map<Ip, String> iptoIfaceName = computeIpToIfaceNameMap(interfaces);

    isakpProfiles.values().stream()
        .filter(isakmpProfile -> isakmpProfile.getLocalAddress() != null)
        .forEach(
            isakmpProfile ->
                isakmpProfile.setLocalInterfaceName(
                    firstNonNull(
                        iptoIfaceName.get(isakmpProfile.getLocalAddress()),
                        INVALID_LOCAL_INTERFACE)));
  }

  /** Resolves the interface names of the addresses used as source addresses in {@link Tunnel}s */
  static void resolveTunnelIfaceNames(Map<String, Interface> interfaces) {
    Map<Ip, String> iptoIfaceName = computeIpToIfaceNameMap(interfaces);

    for (Interface iface : interfaces.values()) {
      Tunnel tunnel = iface.getTunnel();
      // resolve if tunnel's source interface name is not set
      if (tunnel != null
          && UNSET_LOCAL_INTERFACE.equals(tunnel.getSourceInterfaceName())
          && tunnel.getSourceAddress() != null) {
        tunnel.setSourceInterfaceName(
            firstNonNull(iptoIfaceName.get(tunnel.getSourceAddress()), INVALID_LOCAL_INTERFACE));
      }
    }
  }

  static AsPathAccessList toAsPathAccessList(AsPathSet asPathSet) {
    List<AsPathAccessListLine> lines =
        asPathSet.getElements().stream()
            .map(CiscoConversions::toAsPathAccessListLine)
            .collect(ImmutableList.toImmutableList());
    return new AsPathAccessList(asPathSet.getName(), lines);
  }

  static AsPathAccessList toAsPathAccessList(IpAsPathAccessList pathList) {
    List<AsPathAccessListLine> lines =
        pathList.getLines().stream()
            .map(IpAsPathAccessListLine::toAsPathAccessListLine)
            .collect(ImmutableList.toImmutableList());
    return new AsPathAccessList(pathList.getName(), lines);
  }

  static CommunityList toCommunityList(NamedCommunitySet communitySet) {
    return new CommunityList(
        communitySet.getName(),
        communitySet.getElements().stream()
            .map(CiscoConversions::toCommunityListLine)
            .collect(ImmutableList.toImmutableList()),
        false);
  }

  static CommunityList toCommunityList(ExpandedCommunityList ecList) {
    List<CommunityListLine> cllList =
        ecList.getLines().stream()
            .map(CiscoConversions::toCommunityListLine)
            .collect(ImmutableList.toImmutableList());
    return new CommunityList(ecList.getName(), cllList, false);
  }

  public static CommunityList toCommunityList(StandardCommunityList scList) {
    List<CommunityListLine> cllList =
        scList.getLines().stream()
            .map(CiscoConversions::toCommunityListLine)
            .collect(ImmutableList.toImmutableList());
    return new CommunityList(scList.getName(), cllList, false);
  }

  static org.batfish.datamodel.hsrp.HsrpGroup toHsrpGroup(HsrpGroup hsrpGroup) {
    return org.batfish.datamodel.hsrp.HsrpGroup.builder()
        .setAuthentication(hsrpGroup.getAuthentication())
        .setHelloTime(hsrpGroup.getHelloTime())
        .setHoldTime(hsrpGroup.getHoldTime())
        .setIp(hsrpGroup.getIp())
        .setGroupNumber(hsrpGroup.getGroupNumber())
        .setPreempt(hsrpGroup.getPreempt())
        .setPriority(hsrpGroup.getPriority())
        .setTrackActions(hsrpGroup.getTrackActions())
        .build();
  }

  static IkePhase1Key toIkePhase1Key(Keyring keyring) {
    IkePhase1Key ikePhase1Key = new IkePhase1Key();
    ikePhase1Key.setKeyHash(keyring.getKey());
    ikePhase1Key.setKeyType(IkeKeyType.PRE_SHARED_KEY);
    ikePhase1Key.setLocalInterface(keyring.getLocalInterfaceName());
    if (keyring.getRemoteIdentity() != null) {
      ikePhase1Key.setRemoteIdentity(keyring.getRemoteIdentity().toIpSpace());
    }
    return ikePhase1Key;
  }

  static IkePhase1Policy toIkePhase1Policy(
      IsakmpProfile isakmpProfile, CiscoConfiguration oldConfig, Configuration config, Warnings w) {
    IkePhase1Policy ikePhase1Policy = new IkePhase1Policy(isakmpProfile.getName());

    ImmutableList.Builder<String> ikePhase1ProposalBuilder = ImmutableList.builder();
    for (Entry<Integer, IsakmpPolicy> entry : oldConfig.getIsakmpPolicies().entrySet()) {
      ikePhase1ProposalBuilder.add(entry.getKey().toString());
    }

    ikePhase1Policy.setIkePhase1Proposals(ikePhase1ProposalBuilder.build());
    ikePhase1Policy.setSelfIdentity(isakmpProfile.getSelfIdentity());
    if (isakmpProfile.getMatchIdentity() != null) {
      ikePhase1Policy.setRemoteIdentity(isakmpProfile.getMatchIdentity().toIpSpace());
    }

    ikePhase1Policy.setLocalInterface(isakmpProfile.getLocalInterfaceName());
    ikePhase1Policy.setIkePhase1Key(getMatchingPsk(isakmpProfile, w, config.getIkePhase1Keys()));
    return ikePhase1Policy;
  }

  /**
   * Gets the {@link IkePhase1Key} that can be used for the given {@link IsakmpProfile} based on
   * {@code remoteIdentity} and {@code localInterfaceName} present in the {@link IkePhase1Key}
   */
  static IkePhase1Key getMatchingPsk(
      IsakmpProfile isakmpProfile, Warnings w, Map<String, IkePhase1Key> ikePhase1Keys) {
    IkePhase1Key ikePhase1Key = null;
    String isakmpProfileName = isakmpProfile.getName();
    if (isakmpProfile.getLocalInterfaceName().equals(INVALID_LOCAL_INTERFACE)) {
      w.redFlag(
          String.format(
              "Invalid local address interface configured for ISAKMP profile %s",
              isakmpProfileName));
    } else if (isakmpProfile.getKeyring() == null) {
      w.redFlag(String.format("Keyring not set for ISAKMP profile %s", isakmpProfileName));
    } else if (!ikePhase1Keys.containsKey(isakmpProfile.getKeyring())) {
      w.redFlag(
          String.format(
              "Cannot find keyring %s for ISAKMP profile %s",
              isakmpProfile.getKeyring(), isakmpProfileName));
    } else {
      IkePhase1Key tempIkePhase1Key = ikePhase1Keys.get(isakmpProfile.getKeyring());
      if (tempIkePhase1Key.getLocalInterface().equals(INVALID_LOCAL_INTERFACE)) {
        w.redFlag(
            String.format(
                "Invalid local address interface configured for keyring %s",
                isakmpProfile.getKeyring()));
      } else if (tempIkePhase1Key.match(
          isakmpProfile.getLocalInterfaceName(), isakmpProfile.getMatchIdentity())) {
        // found a matching keyring
        ikePhase1Key = tempIkePhase1Key;
      }
    }
    return ikePhase1Key;
  }

  static IkePhase1Proposal toIkePhase1Proposal(IsakmpPolicy isakmpPolicy) {
    IkePhase1Proposal ikePhase1Proposal = new IkePhase1Proposal(isakmpPolicy.getName().toString());
    ikePhase1Proposal.setDiffieHellmanGroup(isakmpPolicy.getDiffieHellmanGroup());
    ikePhase1Proposal.setAuthenticationMethod(isakmpPolicy.getAuthenticationMethod());
    ikePhase1Proposal.setEncryptionAlgorithm(isakmpPolicy.getEncryptionAlgorithm());
    ikePhase1Proposal.setLifetimeSeconds(isakmpPolicy.getLifetimeSeconds());
    ikePhase1Proposal.setHashingAlgorithm(isakmpPolicy.getHashAlgorithm());
    return ikePhase1Proposal;
  }

  static Ip6AccessList toIp6AccessList(ExtendedIpv6AccessList eaList) {
    String name = eaList.getName();
    List<Ip6AccessListLine> lines = new ArrayList<>();
    for (ExtendedIpv6AccessListLine fromLine : eaList.getLines()) {
      Ip6AccessListLine newLine = new Ip6AccessListLine();
      newLine.setName(fromLine.getName());
      newLine.setAction(fromLine.getAction());
      Ip6Wildcard srcIpWildcard = fromLine.getSourceIpWildcard();
      if (srcIpWildcard != null) {
        newLine.getSrcIps().add(srcIpWildcard);
      }
      Ip6Wildcard dstIpWildcard = fromLine.getDestinationIpWildcard();
      if (dstIpWildcard != null) {
        newLine.getDstIps().add(dstIpWildcard);
      }
      // TODO: src/dst address group
      IpProtocol protocol = fromLine.getProtocol();
      if (protocol != IpProtocol.IP) {
        newLine.getIpProtocols().add(protocol);
      }
      newLine.getDstPorts().addAll(fromLine.getDstPorts());
      newLine.getSrcPorts().addAll(fromLine.getSrcPorts());
      Integer icmpType = fromLine.getIcmpType();
      if (icmpType != null) {
        newLine.setIcmpTypes(new TreeSet<>(Collections.singleton(new SubRange(icmpType))));
      }
      Integer icmpCode = fromLine.getIcmpCode();
      if (icmpCode != null) {
        newLine.setIcmpCodes(new TreeSet<>(Collections.singleton(new SubRange(icmpCode))));
      }
      Set<FlowState> states = fromLine.getStates();
      newLine.getStates().addAll(states);
      List<TcpFlagsMatchConditions> tcpFlags = fromLine.getTcpFlags();
      newLine.getTcpFlags().addAll(tcpFlags);
      Set<Integer> dscps = fromLine.getDscps();
      newLine.getDscps().addAll(dscps);
      Set<Integer> ecns = fromLine.getEcns();
      newLine.getEcns().addAll(ecns);
      lines.add(newLine);
    }
    return new Ip6AccessList(name, lines);
  }

  static IpAccessList toIpAccessList(
      ExtendedAccessList eaList, Map<String, ObjectGroup> objectGroups) {
    List<IpAccessListLine> lines =
        eaList.getLines().stream()
            .map(l -> toIpAccessListLine(l, objectGroups))
            .collect(ImmutableList.toImmutableList());
    String sourceType =
        eaList.getParent() != null
            ? CiscoStructureType.IPV4_ACCESS_LIST_STANDARD.getDescription()
            : CiscoStructureType.IPV4_ACCESS_LIST_EXTENDED.getDescription();
    String name = eaList.getName();
    return IpAccessList.builder()
        .setName(name)
        .setLines(lines)
        .setSourceName(name)
        .setSourceType(sourceType)
        .build();
  }

  static IpAccessList toIpAccessList(IcmpTypeObjectGroup icmpTypeObjectGroup) {
    return IpAccessList.builder()
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(icmpTypeObjectGroup.toAclLineMatchExpr())
                    .build()))
        .setName(computeProtocolObjectGroupAclName(icmpTypeObjectGroup.getName()))
        .setSourceName(icmpTypeObjectGroup.getName())
        .setSourceType(CiscoStructureType.ICMP_TYPE_OBJECT_GROUP.getDescription())
        .build();
  }

  static IpAccessList toIpAccessList(ProtocolObjectGroup protocolObjectGroup) {
    return IpAccessList.builder()
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(protocolObjectGroup.toAclLineMatchExpr())
                    .build()))
        .setName(computeProtocolObjectGroupAclName(protocolObjectGroup.getName()))
        .setSourceName(protocolObjectGroup.getName())
        .setSourceType(CiscoStructureType.PROTOCOL_OBJECT_GROUP.getDescription())
        .build();
  }

  static IpAccessList toIpAccessList(ServiceObject serviceObject) {
    return IpAccessList.builder()
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(serviceObject.toAclLineMatchExpr())
                    .build()))
        .setName(CiscoConfiguration.computeServiceObjectAclName(serviceObject.getName()))
        .setSourceName(serviceObject.getName())
        .setSourceType(CiscoStructureType.SERVICE_OBJECT.getDescription())
        .build();
  }

  static IpAccessList toIpAccessList(ServiceObjectGroup serviceObjectGroup) {
    return IpAccessList.builder()
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(serviceObjectGroup.toAclLineMatchExpr())
                    .build()))
        .setName(CiscoConfiguration.computeServiceObjectGroupAclName(serviceObjectGroup.getName()))
        .setSourceName(serviceObjectGroup.getName())
        .setSourceType(CiscoStructureType.SERVICE_OBJECT_GROUP.getDescription())
        .build();
  }

  static IpSpace toIpSpace(NetworkObjectGroup networkObjectGroup) {
    return firstNonNull(AclIpSpace.union(networkObjectGroup.getLines()), EmptyIpSpace.INSTANCE);
  }

  /** Converts a {@link Tunnel} to an {@link IpsecPeerConfig} */
  static IpsecPeerConfig toIpsecPeerConfig(
      Tunnel tunnel,
      String tunnelIfaceName,
      CiscoConfiguration oldConfig,
      Configuration newConfig) {

    IpsecStaticPeerConfig.Builder ipsecStaticPeerConfigBuilder =
        IpsecStaticPeerConfig.builder()
            .setTunnelInterface(tunnelIfaceName)
            .setDestinationAddress(tunnel.getDestination())
            .setLocalAddress(tunnel.getSourceAddress())
            .setSourceInterface(tunnel.getSourceInterfaceName())
            .setIpsecPolicy(tunnel.getIpsecProfileName());

    IpsecProfile ipsecProfile = null;
    if (tunnel.getIpsecProfileName() != null) {
      ipsecProfile = oldConfig.getIpsecProfiles().get(tunnel.getIpsecProfileName());
    }

    if (ipsecProfile != null && ipsecProfile.getIsakmpProfile() != null) {
      ipsecStaticPeerConfigBuilder.setIkePhase1Policy(ipsecProfile.getIsakmpProfile());
    } else {
      ipsecStaticPeerConfigBuilder.setIkePhase1Policy(
          getIkePhase1Policy(
              newConfig.getIkePhase1Policies(),
              tunnel.getDestination(),
              tunnel.getSourceInterfaceName()));
    }

    return ipsecStaticPeerConfigBuilder.build();
  }

  /**
   * Converts a {@link CryptoMapEntry} to multiple {@link IpsecPeerConfig}(one per interface on
   * which crypto map is referred)
   */
  private static Map<String, IpsecPeerConfig> toIpsecPeerConfigs(
      Configuration c,
      CryptoMapEntry cryptoMapEntry,
      String cryptoMapNameSeqNumber,
      String cryptoMapName,
      String ipsecPhase2Policy,
      Warnings w) {

    List<org.batfish.datamodel.Interface> referencingInterfaces =
        c.getAllInterfaces().values().stream()
            .filter(iface -> Objects.equals(iface.getCryptoMap(), cryptoMapName))
            .collect(Collectors.toList());

    ImmutableSortedMap.Builder<String, IpsecPeerConfig> ipsecPeerConfigsBuilder =
        ImmutableSortedMap.naturalOrder();

    for (org.batfish.datamodel.Interface iface : referencingInterfaces) {
      // skipping interfaces with no ip-address
      if (iface.getAddress() == null) {
        w.redFlag(
            String.format(
                "Interface %s with declared crypto-map %s has no ip-address",
                iface.getName(), cryptoMapName));
        continue;
      }
      // add one IPSec peer config per interface for the crypto map entry
      ipsecPeerConfigsBuilder.put(
          String.format("~IPSEC_PEER_CONFIG:%s_%s~", cryptoMapNameSeqNumber, iface.getName()),
          toIpsecPeerConfig(c, cryptoMapEntry, iface, ipsecPhase2Policy, w));
    }
    return ipsecPeerConfigsBuilder.build();
  }

  private static IpsecPeerConfig toIpsecPeerConfig(
      Configuration c,
      CryptoMapEntry cryptoMapEntry,
      org.batfish.datamodel.Interface iface,
      String ipsecPhase2Policy,
      Warnings w) {

    IpsecPeerConfig.Builder<?, ?> newIpsecPeerConfigBuilder;

    String ikePhase1Policy = cryptoMapEntry.getIsakmpProfile();

    // static crypto maps
    if (cryptoMapEntry.getPeer() != null) {
      if (ikePhase1Policy == null) {
        ikePhase1Policy =
            getIkePhase1Policy(c.getIkePhase1Policies(), cryptoMapEntry.getPeer(), iface.getName());
      }
      newIpsecPeerConfigBuilder =
          IpsecStaticPeerConfig.builder()
              .setDestinationAddress(cryptoMapEntry.getPeer())
              .setIkePhase1Policy(ikePhase1Policy);
    } else {
      // dynamic crypto maps
      List<String> ikePhase1Policies;
      if (ikePhase1Policy != null) {
        ikePhase1Policies = ImmutableList.of(ikePhase1Policy);
      } else {
        ikePhase1Policies = getMatchingIKePhase1Policies(c.getIkePhase1Policies(), iface.getName());
      }

      newIpsecPeerConfigBuilder =
          IpsecDynamicPeerConfig.builder().setIkePhase1Policies(ikePhase1Policies);
    }

    newIpsecPeerConfigBuilder
        .setSourceInterface(iface.getName())
        .setIpsecPolicy(ipsecPhase2Policy)
        .setLocalAddress(iface.getAddress().getIp());

    setIpsecPeerConfigPolicyAccessList(c, cryptoMapEntry, newIpsecPeerConfigBuilder, w);

    return newIpsecPeerConfigBuilder.build();
  }

  private static void setIpsecPeerConfigPolicyAccessList(
      Configuration c,
      CryptoMapEntry cryptoMapEntry,
      IpsecPeerConfig.Builder<?, ?> ipsecPeerConfigBuilder,
      Warnings w) {
    if (cryptoMapEntry.getAccessList() != null) {
      IpAccessList cryptoAcl = c.getIpAccessLists().get(cryptoMapEntry.getAccessList());
      if (cryptoAcl != null) {
        IpAccessList symmetricCryptoAcl = createAclWithSymmetricalLines(cryptoAcl);
        if (symmetricCryptoAcl != null) {
          ipsecPeerConfigBuilder.setPolicyAccessList(symmetricCryptoAcl);
        } else {
          // log a warning if the ACL was not made symmetrical successfully
          w.redFlag(
              String.format(
                  "Cannot process the Access List for crypto map %s:%s",
                  cryptoMapEntry.getName(), cryptoMapEntry.getSequenceNumber()));
        }
      }
    }
  }

  /**
   * Returns a new symmetrical {@link IpAccessList} by adding mirror image {@link IpAccessListLine}s
   * to the original {@link IpAccessList} or null if the conversion is not supported
   */
  @VisibleForTesting
  @Nullable
  static IpAccessList createAclWithSymmetricalLines(IpAccessList ipAccessList) {
    List<IpAccessListLine> aclLines = new ArrayList<>(ipAccessList.getLines());

    for (IpAccessListLine ipAccessListLine : ipAccessList.getLines()) {
      HeaderSpace originalHeaderSpace =
          HeaderSpaceConverter.convert(ipAccessListLine.getMatchCondition());

      if (!originalHeaderSpace.equals(
          HeaderSpace.builder()
              .setSrcIps(originalHeaderSpace.getSrcIps())
              .setDstIps(originalHeaderSpace.getDstIps())
              .setSrcPorts(originalHeaderSpace.getSrcPorts())
              .setDstPorts(originalHeaderSpace.getDstPorts())
              .setIpProtocols(originalHeaderSpace.getIpProtocols())
              .setIcmpCodes(originalHeaderSpace.getIcmpCodes())
              .setTcpFlags(originalHeaderSpace.getTcpFlags())
              .build())) {
        //  not supported if the access list line contains any more fields
        return null;
      } else {
        HeaderSpace.Builder reversedHeaderSpaceBuilder = originalHeaderSpace.toBuilder();
        aclLines.add(
            IpAccessListLine.builder()
                .setMatchCondition(
                    new MatchHeaderSpace(
                        reversedHeaderSpaceBuilder
                            .setSrcIps(originalHeaderSpace.getDstIps())
                            .setSrcPorts(originalHeaderSpace.getDstPorts())
                            .setDstIps(originalHeaderSpace.getSrcIps())
                            .setDstPorts(originalHeaderSpace.getSrcPorts())
                            .build()))
                .setAction(ipAccessListLine.getAction())
                .build());
      }
    }

    return IpAccessList.builder().setName(ipAccessList.getName()).setLines(aclLines).build();
  }

  /**
   * Returns the first {@link IkePhase1Policy} name matching {@code remoteAddress} and {@code
   * localInterface}, null is returned if no matching {@link IkePhase1Policy} could not be found
   */
  @Nullable
  private static String getIkePhase1Policy(
      Map<String, IkePhase1Policy> ikePhase1Policies, Ip remoteAddress, String localInterface) {
    for (Entry<String, IkePhase1Policy> e : ikePhase1Policies.entrySet()) {
      IkePhase1Policy ikePhase1Policy = e.getValue();
      String ikePhase1PolicyLocalInterface = ikePhase1Policy.getLocalInterface();
      if (ikePhase1Policy.getRemoteIdentity().containsIp(remoteAddress, ImmutableMap.of())
          && (UNSET_LOCAL_INTERFACE.equals(ikePhase1PolicyLocalInterface)
              || ikePhase1PolicyLocalInterface.equals(localInterface))) {
        return e.getKey();
      }
    }
    return null;
  }

  /** Returns all {@link IkePhase1Policy} names matching the {@code localInterface} */
  private static List<String> getMatchingIKePhase1Policies(
      Map<String, IkePhase1Policy> ikePhase1Policies, String localInterface) {
    List<String> filteredIkePhase1Policies = new ArrayList<>();
    for (Entry<String, IkePhase1Policy> e : ikePhase1Policies.entrySet()) {
      String ikePhase1PolicyLocalInterface = e.getValue().getLocalInterface();
      if ((UNSET_LOCAL_INTERFACE.equals(ikePhase1PolicyLocalInterface)
          || ikePhase1PolicyLocalInterface.equals(localInterface))) {
        filteredIkePhase1Policies.add(e.getKey());
      }
    }
    return filteredIkePhase1Policies;
  }

  static IpsecPhase2Proposal toIpsecPhase2Proposal(IpsecTransformSet ipsecTransformSet) {
    IpsecPhase2Proposal ipsecPhase2Proposal = new IpsecPhase2Proposal();
    ipsecPhase2Proposal.setAuthenticationAlgorithm(ipsecTransformSet.getAuthenticationAlgorithm());
    ipsecPhase2Proposal.setEncryptionAlgorithm(ipsecTransformSet.getEncryptionAlgorithm());
    ipsecPhase2Proposal.setProtocols(ipsecTransformSet.getProtocols());
    ipsecPhase2Proposal.setIpsecEncapsulationMode(ipsecTransformSet.getIpsecEncapsulationMode());

    return ipsecPhase2Proposal;
  }

  static IpsecPhase2Policy toIpsecPhase2Policy(IpsecProfile ipsecProfile) {
    IpsecPhase2Policy ipsecPhase2Policy = new IpsecPhase2Policy();
    ipsecPhase2Policy.setPfsKeyGroup(ipsecProfile.getPfsGroup());
    ipsecPhase2Policy.setProposals(ImmutableList.copyOf(ipsecProfile.getTransformSets()));

    return ipsecPhase2Policy;
  }

  static IpsecPhase2Policy toIpsecPhase2Policy(CryptoMapEntry cryptoMapEntry) {
    IpsecPhase2Policy ipsecPhase2Policy = new IpsecPhase2Policy();
    ipsecPhase2Policy.setProposals(ImmutableList.copyOf(cryptoMapEntry.getTransforms()));
    ipsecPhase2Policy.setPfsKeyGroup(cryptoMapEntry.getPfsKeyGroup());
    return ipsecPhase2Policy;
  }

  @Nullable
  static org.batfish.datamodel.eigrp.EigrpProcess toEigrpProcess(
      EigrpProcess proc, String vrfName, Configuration c, CiscoConfiguration oldConfig) {
    org.batfish.datamodel.eigrp.EigrpProcess.Builder newProcess =
        org.batfish.datamodel.eigrp.EigrpProcess.builder();
    org.batfish.datamodel.Vrf vrf = c.getVrfs().get(vrfName);

    if (proc.getAsn() == null) {
      oldConfig.getWarnings().redFlag("Invalid EIGRP process");
      return null;
    }

    newProcess.setAsNumber(proc.getAsn());
    newProcess.setMode(proc.getMode());
    newProcess.setVrf(vrf);

    // TODO set stub process
    // newProcess.setStub(proc.isStub())

    // TODO create summary filters

    // TODO originate default route if configured

    Ip routerId = proc.getRouterId();
    if (routerId == null) {
      routerId = getHighestIp(oldConfig.getInterfaces());
      if (routerId == Ip.ZERO) {
        oldConfig
            .getWarnings()
            .redFlag("No candidates for EIGRP (AS " + proc.getAsn() + ") router-id");
        return null;
      }
    }
    newProcess.setRouterId(routerId);

    /*
     * Route redistribution modifies the configuration structure, so do this last to avoid having to
     * clean up configuration if another conversion step fails
     */
    String eigrpExportPolicyName = "~EIGRP_EXPORT_POLICY:" + vrfName + ":" + proc.getAsn() + "~";
    RoutingPolicy eigrpExportPolicy = new RoutingPolicy(eigrpExportPolicyName, c);
    c.getRoutingPolicies().put(eigrpExportPolicyName, eigrpExportPolicy);
    newProcess.setExportPolicy(eigrpExportPolicyName);

    eigrpExportPolicy
        .getStatements()
        .addAll(
            proc.getRedistributionPolicies().values().stream()
                .map(policy -> convertEigrpRedistributionPolicy(policy, proc, oldConfig))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

    return newProcess.build();
  }

  @Nullable
  private static If convertEigrpRedistributionPolicy(
      EigrpRedistributionPolicy policy, EigrpProcess proc, CiscoConfiguration oldConfig) {
    RoutingProtocol protocol = policy.getSourceProtocol();
    // All redistribution must match the specified protocol.
    Conjunction eigrpExportConditions = new Conjunction();
    BooleanExpr matchExpr;
    if (protocol == RoutingProtocol.EIGRP) {
      matchExpr =
          new Disjunction(
              ImmutableList.of(
                  new MatchProtocol(RoutingProtocol.EIGRP),
                  new MatchProtocol(RoutingProtocol.EIGRP_EX)));

      Long otherAsn =
          (Long) policy.getSpecialAttributes().get(EigrpRedistributionPolicy.EIGRP_AS_NUMBER);
      if (otherAsn == null) {
        oldConfig.getWarnings().redFlag("Unable to redistribute - policy has no ASN");
        return null;
      }
      eigrpExportConditions.getConjuncts().add(new MatchProcessAsn(otherAsn));
    } else {
      matchExpr = new MatchProtocol(protocol);
    }
    eigrpExportConditions.getConjuncts().add(matchExpr);

    // Default routes can be redistributed into EIGRP. Don't filter them.

    ImmutableList.Builder<Statement> eigrpExportStatements = ImmutableList.builder();

    // Set the metric
    // TODO prefer metric from route map
    // https://github.com/batfish/batfish/issues/2070
    EigrpMetric metric = policy.getMetric() != null ? policy.getMetric() : proc.getDefaultMetric();
    if (metric != null) {
      eigrpExportStatements.add(new SetEigrpMetric(new LiteralEigrpMetric(metric)));
    } else if (protocol != RoutingProtocol.EIGRP) {
      /*
       * TODO no default metric (and not EIGRP into EIGRP)
       * 1) connected can use the interface metric
       * 2) static with next hop interface can use the interface metric
       * 3) If none of the above, bad configuration
       */
      oldConfig.getWarnings().redFlag("Unable to redistribute - no metric");
      return null;
    }

    String exportRouteMapName = policy.getRouteMap();
    if (exportRouteMapName != null) {
      RouteMap exportRouteMap = oldConfig.getRouteMaps().get(exportRouteMapName);
      if (exportRouteMap != null) {
        eigrpExportConditions.getConjuncts().add(new CallExpr(exportRouteMapName));
      }
    }

    eigrpExportStatements.add(Statements.ExitAccept.toStaticStatement());

    // Construct a new policy and add it before returning.
    return new If(
        "EIGRP export routes for " + protocol.protocolName(),
        eigrpExportConditions,
        eigrpExportStatements.build(),
        ImmutableList.of());
  }

  static org.batfish.datamodel.isis.IsisProcess toIsisProcess(
      IsisProcess proc, Configuration c, CiscoConfiguration oldConfig) {
    org.batfish.datamodel.isis.IsisProcess.Builder newProcess =
        org.batfish.datamodel.isis.IsisProcess.builder();
    if (proc.getNetAddress() == null) {
      oldConfig.getWarnings().redFlag("Cannot create IS-IS process without specifying net-address");
      return null;
    }
    newProcess.setNetAddress(proc.getNetAddress());
    IsisLevelSettings settings = IsisLevelSettings.builder().build();
    switch (proc.getLevel()) {
      case LEVEL_1:
        newProcess.setLevel1(settings);
        break;
      case LEVEL_1_2:
        newProcess.setLevel1(settings);
        newProcess.setLevel2(settings);
        break;
      case LEVEL_2:
        newProcess.setLevel2(settings);
        break;
      default:
        throw new BatfishException("Unhandled IS-IS level.");
    }
    return newProcess.build();
  }

  static Route6FilterList toRoute6FilterList(ExtendedIpv6AccessList eaList) {
    String name = eaList.getName();
    List<Route6FilterLine> lines =
        eaList.getLines().stream()
            .map(CiscoConversions::toRoute6FilterLine)
            .collect(ImmutableList.toImmutableList());
    return new Route6FilterList(name, lines);
  }

  static Route6FilterList toRoute6FilterList(StandardIpv6AccessList eaList) {
    String name = eaList.getName();
    List<Route6FilterLine> lines =
        eaList.getLines().stream()
            .map(CiscoConversions::toRoute6FilterLine)
            .collect(ImmutableList.toImmutableList());
    return new Route6FilterList(name, lines);
  }

  static Route6FilterList toRoute6FilterList(Prefix6List list) {
    List<Route6FilterLine> lines =
        list.getLines().stream()
            .map(pl -> new Route6FilterLine(pl.getAction(), pl.getPrefix(), pl.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    return new Route6FilterList(list.getName(), lines);
  }

  static RouteFilterList toRouteFilterList(ExtendedAccessList eaList) {
    List<RouteFilterLine> lines =
        eaList.getLines().stream()
            .map(CiscoConversions::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList());
    return new RouteFilterList(eaList.getName(), lines);
  }

  static RouteFilterList toRouteFilterList(StandardAccessList saList) {
    List<RouteFilterLine> lines =
        saList.getLines().stream()
            .map(CiscoConversions::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList());
    return new RouteFilterList(saList.getName(), lines);
  }

  static RouteFilterList toRouteFilterList(PrefixList list) {
    RouteFilterList newRouteFilterList = new RouteFilterList(list.getName());
    List<RouteFilterLine> newLines =
        list.getLines().stream()
            .map(
                l ->
                    new RouteFilterLine(
                        l.getAction(), new IpWildcard(l.getPrefix()), l.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    newRouteFilterList.setLines(newLines);
    return newRouteFilterList;
  }

  @VisibleForTesting
  static boolean sanityCheckDistributeList(
      @Nonnull DistributeList distributeList,
      @Nonnull Configuration c,
      @Nonnull CiscoConfiguration oldConfig,
      String vrfName,
      String ospfProcessId) {
    if (distributeList.getFilterType() != DistributeListFilterType.PREFIX_LIST) {
      // only prefix-lists are supported in distribute-list
      oldConfig
          .getWarnings()
          .redFlag(
              String.format(
                  "OSPF process %s:%s in %s uses distribute-list of type %s, only prefix-lists are supported in dist-lists by Batfish",
                  vrfName, ospfProcessId, oldConfig.getHostname(), distributeList.getFilterType()));
      return false;
    } else if (!c.getRouteFilterLists().containsKey(distributeList.getFilterName())) {
      // if referred prefix-list is not defined, all prefixes will be allowed
      oldConfig
          .getWarnings()
          .redFlag(
              String.format(
                  "dist-list in OSPF process %s:%s uses a prefix-list which is not defined, this dist-list will allow everything",
                  vrfName, ospfProcessId));
      return false;
    }
    return true;
  }

  /**
   * Populates the {@link RoutingPolicy}s for inbound {@link DistributeList}s which use {@link
   * PrefixList} as the {@link DistributeList#_filterType}. {@link
   * DistributeListFilterType#ROUTE_MAP} and {@link DistributeListFilterType#ACCESS_LIST} are not
   * supported currently.
   *
   * @param ospfProcess {@link OspfProcess} for which {@link DistributeList}s are to be processed
   * @param c {@link Configuration} containing the Vendor Independent representation
   * @param vrf Id of the {@link Vrf} containing the {@link OspfProcess}
   * @param ospfProcessId {@link OspfProcess}'s Id
   */
  static void computeDistributeListPolicies(
      @Nonnull OspfProcess ospfProcess,
      @Nonnull Configuration c,
      @Nonnull String vrf,
      @Nonnull String ospfProcessId,
      @Nonnull CiscoConfiguration oldConfig) {
    DistributeList globalDistributeList = ospfProcess.getInboundGlobalDistributeList();

    BooleanExpr globalCondition = null;
    if (globalDistributeList != null
        && sanityCheckDistributeList(globalDistributeList, c, oldConfig, vrf, ospfProcessId)) {
      globalCondition =
          new MatchPrefixSet(
              DestinationNetwork.instance(),
              new NamedPrefixSet(globalDistributeList.getFilterName()));
    }

    Map<String, DistributeList> interfaceDistributeLists =
        ospfProcess.getInboundInterfaceDistributeLists();

    for (Entry<String, org.batfish.datamodel.Interface> entry :
        c.getVrfs().get(vrf).getInterfaces().entrySet()) {
      DistributeList ifaceDistributeList = interfaceDistributeLists.get(entry.getKey());
      BooleanExpr ifaceCondition = null;
      if (ifaceDistributeList != null
          && sanityCheckDistributeList(ifaceDistributeList, c, oldConfig, vrf, ospfProcessId)) {
        ifaceCondition =
            new MatchPrefixSet(
                DestinationNetwork.instance(),
                new NamedPrefixSet(ifaceDistributeList.getFilterName()));
      }

      if (globalCondition == null && ifaceCondition == null) {
        // doing nothing if both global and interface conditions are empty
        continue;
      }

      String policyName =
          String.format("~OSPF_DIST_LIST_%s_%s_%s~", vrf, ospfProcessId, entry.getKey());
      RoutingPolicy routingPolicy = new RoutingPolicy(policyName, c);
      routingPolicy
          .getStatements()
          .add(
              new If(
                  new Conjunction(
                      Stream.of(globalCondition, ifaceCondition)
                          .filter(Objects::nonNull)
                          .collect(ImmutableList.toImmutableList())),
                  ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                  ImmutableList.of(Statements.ExitReject.toStaticStatement())));
      c.getRoutingPolicies().put(routingPolicy.getName(), routingPolicy);
      entry.getValue().setOspfInboundDistributeListPolicy(policyName);
    }
  }

  static org.batfish.datamodel.StaticRoute toStaticRoute(Configuration c, StaticRoute staticRoute) {
    String nextHopInterface = staticRoute.getNextHopInterface();
    if (nextHopInterface != null && nextHopInterface.toLowerCase().startsWith("null")) {
      nextHopInterface = org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
    }
    return org.batfish.datamodel.StaticRoute.builder()
        .setNetwork(staticRoute.getPrefix())
        .setNextHopIp(staticRoute.getNextHopIp())
        .setNextHopInterface(nextHopInterface)
        .setAdministrativeCost(staticRoute.getDistance())
        .setTag(firstNonNull(staticRoute.getTag(), -1))
        .build();
  }

  private static IpAccessListLine toIpAccessListLine(
      ExtendedAccessListLine line, Map<String, ObjectGroup> objectGroups) {
    IpSpace srcIpSpace = line.getSourceAddressSpecifier().toIpSpace();
    IpSpace dstIpSpace = line.getDestinationAddressSpecifier().toIpSpace();
    AclLineMatchExpr matchService = line.getServiceSpecifier().toAclLineMatchExpr(objectGroups);
    AclLineMatchExpr match;
    if (matchService instanceof MatchHeaderSpace) {
      match =
          new MatchHeaderSpace(
              ((MatchHeaderSpace) matchService)
                  .getHeaderspace()
                  .toBuilder()
                  .setSrcIps(srcIpSpace)
                  .setDstIps(dstIpSpace)
                  .build());
    } else {
      match =
          new AndMatchExpr(
              ImmutableList.of(
                  matchService,
                  new MatchHeaderSpace(
                      HeaderSpace.builder().setSrcIps(srcIpSpace).setDstIps(dstIpSpace).build())));
    }

    return IpAccessListLine.builder()
        .setAction(line.getAction())
        .setMatchCondition(match)
        .setName(line.getName())
        .build();
  }

  private static AsPathAccessListLine toAsPathAccessListLine(AsPathSetElem elem) {
    String regex = CiscoConfiguration.toJavaRegex(elem.regex());
    AsPathAccessListLine line = new AsPathAccessListLine(LineAction.PERMIT, regex);
    return line;
  }

  private static CommunityListLine toCommunityListLine(CommunitySetElem elem) {
    return new CommunityListLine(LineAction.PERMIT, elem.toCommunitySetExpr());
  }

  private static CommunityListLine toCommunityListLine(ExpandedCommunityListLine eclLine) {
    String javaRegex = CiscoConfiguration.toJavaRegex(eclLine.getRegex());
    return new CommunityListLine(eclLine.getAction(), new RegexCommunitySet(javaRegex));
  }

  private static CommunityListLine toCommunityListLine(StandardCommunityListLine sclLine) {
    Collection<Long> lineCommunities = sclLine.getCommunities();
    CommunitySetExpr expr =
        lineCommunities.size() == 1
            ? new LiteralCommunity(StandardCommunity.of(lineCommunities.iterator().next()))
            : new LiteralCommunityConjunction(
                lineCommunities.stream()
                    .map(StandardCommunity::of)
                    .collect(ImmutableSet.toImmutableSet()));
    return new CommunityListLine(sclLine.getAction(), expr);
  }

  private static Route6FilterLine toRoute6FilterLine(ExtendedIpv6AccessListLine fromLine) {
    LineAction action = fromLine.getAction();
    Ip6 ip = fromLine.getSourceIpWildcard().getIp();
    BigInteger minSubnet = fromLine.getDestinationIpWildcard().getIp().asBigInteger();
    BigInteger maxSubnet =
        minSubnet.or(fromLine.getDestinationIpWildcard().getWildcard().asBigInteger());
    int minPrefixLength = fromLine.getDestinationIpWildcard().getIp().numSubnetBits();
    int maxPrefixLength = new Ip6(maxSubnet).numSubnetBits();
    int statedPrefixLength =
        fromLine.getSourceIpWildcard().getWildcard().inverted().numSubnetBits();
    int prefixLength = Math.min(statedPrefixLength, minPrefixLength);
    Prefix6 prefix = new Prefix6(ip, prefixLength);
    return new Route6FilterLine(action, prefix, new SubRange(minPrefixLength, maxPrefixLength));
  }

  /** Convert a standard IPv6 access list line to a route filter list line */
  private static Route6FilterLine toRoute6FilterLine(StandardIpv6AccessListLine fromLine) {
    LineAction action = fromLine.getAction();
    Prefix6 prefix = fromLine.getIpWildcard().toPrefix();

    return new Route6FilterLine(
        action,
        new Ip6Wildcard(prefix),
        new SubRange(prefix.getPrefixLength(), Prefix6.MAX_PREFIX_LENGTH));
  }

  private static RouteFilterLine toRouteFilterLine(ExtendedAccessListLine fromLine) {
    LineAction action = fromLine.getAction();
    IpWildcard srcIpWildcard =
        ((WildcardAddressSpecifier) fromLine.getSourceAddressSpecifier()).getIpWildcard();
    Ip ip = srcIpWildcard.getIp();
    IpWildcard dstIpWildcard =
        ((WildcardAddressSpecifier) fromLine.getDestinationAddressSpecifier()).getIpWildcard();
    long minSubnet = dstIpWildcard.getIp().asLong();
    long maxSubnet = minSubnet | dstIpWildcard.getWildcard().asLong();
    int minPrefixLength = dstIpWildcard.getIp().numSubnetBits();
    int maxPrefixLength = Ip.create(maxSubnet).numSubnetBits();
    int statedPrefixLength = srcIpWildcard.getWildcard().inverted().numSubnetBits();
    int prefixLength = Math.min(statedPrefixLength, minPrefixLength);
    Prefix prefix = Prefix.create(ip, prefixLength);
    return new RouteFilterLine(
        action, new IpWildcard(prefix), new SubRange(minPrefixLength, maxPrefixLength));
  }

  /** Convert a standard access list line to a route filter list line */
  private static RouteFilterLine toRouteFilterLine(StandardAccessListLine fromLine) {
    LineAction action = fromLine.getAction();
    /*
     * This cast is safe since the other address specifier (network object group specifier)
     * can be used only from extended ACLs.
     */
    IpWildcard srcIpWildcard =
        ((WildcardAddressSpecifier) fromLine.getSrcAddressSpecifier()).getIpWildcard();
    Prefix prefix = srcIpWildcard.toPrefix();

    return new RouteFilterLine(
        action,
        new IpWildcard(prefix),
        new SubRange(prefix.getPrefixLength(), Prefix.MAX_PREFIX_LENGTH));
  }

  private CiscoConversions() {} // prevent instantiation of utility class
}
