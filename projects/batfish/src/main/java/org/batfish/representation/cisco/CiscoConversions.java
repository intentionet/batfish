package org.batfish.representation.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.singletonList;
import static org.batfish.datamodel.Interface.INVALID_LOCAL_INTERFACE;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeGateway;
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
import org.batfish.datamodel.IpsecPolicy;
import org.batfish.datamodel.IpsecProposal;
import org.batfish.datamodel.IpsecStaticPeerConfig;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Route6FilterLine;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.visitors.HeaderSpaceConverter;

/** Utilities that convert Cisco-specific representations to vendor-independent model. */
@ParametersAreNonnullByDefault
class CiscoConversions {

  /**
   * Converts a {@link CryptoMapEntry} to an {@link IpsecPolicy}, a list of {@link IpsecVpn}. Also
   * converts it to an {@link IpsecPhase2Policy} and a list of {@link IpsecPeerConfig}
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

    IpsecPolicy ipsecPolicy = toIpsecPolicy(c, cryptoMapEntry, cryptoMapNameSeqNumber);

    c.getIpsecPolicies().put(cryptoMapNameSeqNumber, ipsecPolicy);

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
            .putAll(c.getIpsecPeerconfigs())
            .putAll(
                toIpsecPeerConfigs(
                    c,
                    cryptoMapEntry,
                    cryptoMapNameSeqNumber,
                    cryptoMapName,
                    ipsecPhase2PolicyName,
                    w))
            .build());

    List<IpsecVpn> ipsecVpns =
        toIpsecVpns(c, cryptoMapEntry, cryptoMapNameSeqNumber, cryptoMapName, w);
    ipsecVpns.forEach(
        ipsecVpn -> {
          ipsecVpn.setIpsecPolicy(ipsecPolicy);
          c.getIpsecVpns().put(ipsecVpn.getName(), ipsecVpn);
        });
  }

  /**
   * Converts each crypto map entry in all crypto map sets to {@link IpsecPolicy}, {@link
   * IpsecVpn}s, {@link IpsecPhase2Policy} and {@link IpsecPeerConfig}s
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
   * Generates and returns a {@link RoutingPolicy} that matches routes that should be aggregated for
   * aggregate network indicated by the given {@link Prefix}.
   *
   * <p>Does the bookkeeping in the provided {@link Configuration} to ensure the generated policy is
   * available and tracked.
   */
  static RoutingPolicy generateAggregateRoutePolicy(
      Configuration c, String vrfName, Prefix prefix) {
    BooleanExpr matchLongerNetworks =
        new MatchPrefixSet(
            new DestinationNetwork(),
            new ExplicitPrefixSet(new PrefixSpace(PrefixRange.moreSpecificThan(prefix))));
    If currentGeneratedRouteConditional =
        new If(matchLongerNetworks, singletonList(Statements.ReturnTrue.toStaticStatement()));

    RoutingPolicy policy =
        new RoutingPolicy("~AGGREGATE_ROUTE_GEN:" + vrfName + ":" + prefix + "~", c);
    policy.setStatements(ImmutableList.of(currentGeneratedRouteConditional));
    c.getRoutingPolicies().put(policy.getName(), policy);
    return policy;
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
                new RouteFilterLine(LineAction.ACCEPT, PrefixRange.moreSpecificThan(p))));
    // Bookkeeping: record that we created this RouteFilterList to match longer networks.
    c.getRouteFilterLists().put(matchLonger.getName(), matchLonger);

    return new If(
        "Suppress more specific networks for summary-only aggregate-address networks",
        new MatchPrefixSet(new DestinationNetwork(), new NamedPrefixSet(matchLonger.getName())),
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
    keyrings
        .values()
        .stream()
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

    isakpProfiles
        .values()
        .stream()
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
          && tunnel.getSourceInterfaceName().equals(UNSET_LOCAL_INTERFACE)
          && tunnel.getSourceAddress() != null) {
        tunnel.setSourceInterfaceName(
            firstNonNull(iptoIfaceName.get(tunnel.getSourceAddress()), INVALID_LOCAL_INTERFACE));
      }
    }
  }

  static AsPathAccessList toAsPathAccessList(AsPathSet asPathSet) {
    List<AsPathAccessListLine> lines =
        asPathSet
            .getElements()
            .stream()
            .map(CiscoConversions::toAsPathAccessListLine)
            .collect(ImmutableList.toImmutableList());
    return new AsPathAccessList(asPathSet.getName(), lines);
  }

  static AsPathAccessList toAsPathAccessList(IpAsPathAccessList pathList) {
    List<AsPathAccessListLine> lines =
        pathList
            .getLines()
            .stream()
            .map(IpAsPathAccessListLine::toAsPathAccessListLine)
            .collect(ImmutableList.toImmutableList());
    return new AsPathAccessList(pathList.getName(), lines);
  }

  static CommunityList toCommunityList(ExpandedCommunityList ecList) {
    List<CommunityListLine> cllList =
        ecList
            .getLines()
            .stream()
            .map(CiscoConversions::toCommunityListLine)
            .collect(ImmutableList.toImmutableList());
    return new CommunityList(ecList.getName(), cllList);
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
      Set<State> states = fromLine.getStates();
      newLine.getStates().addAll(states);
      List<TcpFlags> tcpFlags = fromLine.getTcpFlags();
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
        eaList
            .getLines()
            .stream()
            .map(l -> toIpAccessListLine(l, objectGroups))
            .collect(ImmutableList.toImmutableList());
    return new IpAccessList(eaList.getName(), lines);
  }

  static IpSpace toIpSpace(NetworkObjectGroup networkObjectGroup) {
    return AclIpSpace.union(networkObjectGroup.getLines());
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
            .setPhysicalInterface(tunnel.getSourceInterfaceName())
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
        c.getInterfaces()
            .values()
            .stream()
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
        .setPhysicalInterface(iface.getName())
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

    return new IpAccessList(ipAccessList.getName(), aclLines);
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
          && (ikePhase1PolicyLocalInterface == null
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
      if ((ikePhase1PolicyLocalInterface == null
          || ikePhase1PolicyLocalInterface.equals(localInterface))) {
        filteredIkePhase1Policies.add(e.getKey());
      }
    }
    return filteredIkePhase1Policies;
  }

  static IpsecProposal toIpsecProposal(IpsecTransformSet ipsecTransformSet) {
    IpsecProposal ipsecProposal = new IpsecProposal(ipsecTransformSet.getName());
    ipsecProposal.setAuthenticationAlgorithm(ipsecTransformSet.getAuthenticationAlgorithm());
    ipsecProposal.setEncryptionAlgorithm(ipsecTransformSet.getEncryptionAlgorithm());
    ipsecProposal.setProtocols(ipsecTransformSet.getProtocols());

    return ipsecProposal;
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

  /** Converts a {@link CryptoMapEntry} to an {@link IpsecPolicy} */
  private static IpsecPolicy toIpsecPolicy(
      Configuration c, CryptoMapEntry cryptoMapEntry, String ipsecPolicyName) {
    IpsecPolicy ipsecPolicy = new IpsecPolicy(ipsecPolicyName);

    if (cryptoMapEntry.getIsakmpProfile() != null) {
      IkeGateway ikeGateway = c.getIkeGateways().get(cryptoMapEntry.getIsakmpProfile());
      if (ikeGateway != null) {
        ipsecPolicy.setIkeGateway(ikeGateway);
      }
    }

    cryptoMapEntry
        .getTransforms()
        .forEach(
            transform -> {
              IpsecProposal ipsecProposal = c.getIpsecProposals().get(transform);
              if (ipsecProposal != null) {
                ipsecPolicy.getProposals().add(ipsecProposal);
              }
            });

    ipsecPolicy.setPfsKeyGroup(cryptoMapEntry.getPfsKeyGroup());

    return ipsecPolicy;
  }

  /**
   * Converts a crypto map entry to a list of ipsec vpns(one per interface on which it is referred)
   */
  private static List<IpsecVpn> toIpsecVpns(
      Configuration c,
      CryptoMapEntry cryptoMapEntry,
      String ipsecVpnName,
      String cryptoMapName,
      Warnings w) {

    List<org.batfish.datamodel.Interface> referencingInterfaces =
        c.getInterfaces()
            .values()
            .stream()
            .filter(iface -> Objects.equals(iface.getCryptoMap(), cryptoMapName))
            .collect(Collectors.toList());

    List<IpsecVpn> ipsecVpns = new ArrayList<>();

    for (org.batfish.datamodel.Interface iface : referencingInterfaces) {
      // skipping interfaces with no ip-address
      if (iface.getAddress() == null) {
        w.redFlag(
            String.format(
                "Interface %s with declared crypto-map %s has no ip-address",
                iface.getName(), cryptoMapName));
        continue;
      }
      Ip bindingInterfaceIp = iface.getAddress().getIp();
      IkeGateway ikeGateway = null;

      if (cryptoMapEntry.getIsakmpProfile() != null) {
        ikeGateway = c.getIkeGateways().get(cryptoMapEntry.getIsakmpProfile());
        if (ikeGateway != null
            && (!ikeGateway.getAddress().equals(cryptoMapEntry.getPeer())
                || !ikeGateway.getLocalIp().equals(bindingInterfaceIp))) {
          w.redFlag(
              String.format(
                  "cryptoMap %s's binding interface or peer does not match ISAKMP profile's local "
                      + "Ip/remote Ip",
                  cryptoMapEntry.getName()));
          continue;
        }
      }

      IpsecVpn ipsecVpn = new IpsecVpn(String.format("%s:%s", ipsecVpnName, iface.getName()));

      if (ikeGateway != null) {
        ipsecVpn.setIkeGateway(ikeGateway);
      } else {
        // getting an IKE gateway which can be used for this VPN
        Optional<IkeGateway> filteredIkeGateway =
            c.getIkeGateways()
                .values()
                .stream()
                .filter(
                    ikeGateway1 ->
                        ikeGateway1.getLocalIp().equals(bindingInterfaceIp)
                            && ikeGateway1.getAddress().equals(cryptoMapEntry.getPeer()))
                .findFirst();
        filteredIkeGateway.ifPresent(ipsecVpn::setIkeGateway);
      }

      ipsecVpn.setBindInterface(iface);
      if (cryptoMapEntry.getAccessList() != null) {
        IpAccessList cryptoAcl = c.getIpAccessLists().get(cryptoMapEntry.getAccessList());
        if (cryptoAcl != null) {
          IpAccessList symmetricCryptoAcl = createAclWithSymmetricalLines(cryptoAcl);
          if (symmetricCryptoAcl != null) {
            ipsecVpn.setPolicyAccessList(symmetricCryptoAcl);
          } else {
            w.redFlag(
                String.format(
                    "Cannot process the Access List for crypto map %s:%s",
                    cryptoMapEntry.getName(), cryptoMapEntry.getSequenceNumber()));
          }
        }
      }
      ipsecVpns.add(ipsecVpn);
    }

    return ipsecVpns;
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
        eaList
            .getLines()
            .stream()
            .map(CiscoConversions::toRoute6FilterLine)
            .collect(ImmutableList.toImmutableList());
    return new Route6FilterList(name, lines);
  }

  static Route6FilterList toRoute6FilterList(Prefix6List list) {
    List<Route6FilterLine> lines =
        list.getLines()
            .stream()
            .map(pl -> new Route6FilterLine(pl.getAction(), pl.getPrefix(), pl.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    return new Route6FilterList(list.getName(), lines);
  }

  static RouteFilterList toRouteFilterList(ExtendedAccessList eaList) {
    List<RouteFilterLine> lines =
        eaList
            .getLines()
            .stream()
            .map(CiscoConversions::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList());
    return new RouteFilterList(eaList.getName(), lines);
  }

  static RouteFilterList toRouteFilterList(PrefixList list) {
    RouteFilterList newRouteFilterList = new RouteFilterList(list.getName());
    List<RouteFilterLine> newLines =
        list.getLines()
            .stream()
            .map(
                l ->
                    new RouteFilterLine(
                        l.getAction(), new IpWildcard(l.getPrefix()), l.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    newRouteFilterList.setLines(newLines);
    return newRouteFilterList;
  }

  static org.batfish.datamodel.StaticRoute toStaticRoute(Configuration c, StaticRoute staticRoute) {
    String nextHopInterface = staticRoute.getNextHopInterface();
    if (nextHopInterface != null && CommonUtil.isNullInterface(nextHopInterface)) {
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

  static IpAccessList toIpAccessList(ServiceObjectGroup serviceObjectGroup) {
    return IpAccessList.builder()
        .setLines(
            ImmutableList.of(
                IpAccessListLine.accepting()
                    .setMatchCondition(serviceObjectGroup.toAclLineMatchExpr())
                    .build()))
        .setName(CiscoConfiguration.computeServiceObjectGroupAclName(serviceObjectGroup.getName()))
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
    AsPathAccessListLine line = new AsPathAccessListLine();
    line.setAction(LineAction.ACCEPT);
    line.setRegex(regex);
    return line;
  }

  private static CommunityListLine toCommunityListLine(ExpandedCommunityListLine eclLine) {
    String javaRegex = CiscoConfiguration.toJavaRegex(eclLine.getRegex());
    return new CommunityListLine(eclLine.getAction(), javaRegex);
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
    int maxPrefixLength = new Ip(maxSubnet).numSubnetBits();
    int statedPrefixLength = srcIpWildcard.getWildcard().inverted().numSubnetBits();
    int prefixLength = Math.min(statedPrefixLength, minPrefixLength);
    Prefix prefix = new Prefix(ip, prefixLength);
    return new RouteFilterLine(
        action, new IpWildcard(prefix), new SubRange(minPrefixLength, maxPrefixLength));
  }

  private CiscoConversions() {} // prevent instantiation of utility class
}
