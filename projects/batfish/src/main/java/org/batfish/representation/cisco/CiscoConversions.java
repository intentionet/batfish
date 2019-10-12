package org.batfish.representation.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.singletonList;
import static org.batfish.datamodel.IkePhase1Policy.PREFIX_ISAKMP_KEY;
import static org.batfish.datamodel.IkePhase1Policy.PREFIX_RSA_PUB;
import static org.batfish.datamodel.Interface.INVALID_LOCAL_INTERFACE;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.datamodel.Names.generatedBgpCommonExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerExportPolicyName;
import static org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
import static org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpDefaultRouteExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerImportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeIcmpObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeProtocolObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.toJavaRegex;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.Ip6AccessListLine;
import org.batfish.datamodel.Ip6Wildcard;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
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
import org.batfish.datamodel.eigrp.EigrpMetricValues;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.routing_policy.Common;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.ColonSeparatedRendering;
import org.batfish.datamodel.routing_policy.communities.CommunityExprsSet;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAny;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExprs;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAny;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySetUnion;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityHighLowExprs;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityHighMatch;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityLowMatch;
import org.batfish.datamodel.routing_policy.communities.TypesFirstAscendingSpaceSeparated;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.IntComparator;
import org.batfish.datamodel.routing_policy.expr.IntComparison;
import org.batfish.datamodel.routing_policy.expr.IntExpr;
import org.batfish.datamodel.routing_policy.expr.IntMatchAll;
import org.batfish.datamodel.routing_policy.expr.IntMatchExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralEigrpMetric;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProcessAsn;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.expr.VarInt;
import org.batfish.datamodel.routing_policy.statement.CallStatement;
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
public class CiscoConversions {

  // Defaults from
  // https://www.cisco.com/c/en/us/support/docs/ip/open-shortest-path-first-ospf/13689-17.html
  static int DEFAULT_OSPF_HELLO_INTERVAL_P2P_AND_BROADCAST = 10;

  static int DEFAULT_OSPF_HELLO_INTERVAL = 30;

  // Default dead interval is hello interval times 4
  static int OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER = 4;

  static int DEFAULT_OSPF_DEAD_INTERVAL_P2P_AND_BROADCAST =
      OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * DEFAULT_OSPF_HELLO_INTERVAL_P2P_AND_BROADCAST;

  static int DEFAULT_OSPF_DEAD_INTERVAL =
      OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * DEFAULT_OSPF_HELLO_INTERVAL;

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
      for (ConcreteInterfaceAddress address : iface.getAllAddresses()) {
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
   * Convert {@code communitySet} to a {@link CommunityMatchExpr} to be applied as a deletion
   * criterion against each individual community in a route's standard community attribute.
   */
  public static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      XrCommunitySet communitySet, Configuration c) {
    return new CommunityMatchAny(
        communitySet.getElements().stream()
            .map(elem -> elem.accept(CommunitySetElemToCommunityMatchExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  private static final class CommunitySetElemToCommunityMatchExpr
      implements XrCommunitySetElemVisitor<CommunityMatchExpr, Configuration> {
    @Override
    public CommunityMatchExpr visitCommunitySetHighLowRangeExprs(
        XrCommunitySetHighLowRangeExprs highLowRangeExprs, Configuration arg) {
      return new CommunityMatchAll(
          ImmutableList.of(
              new StandardCommunityHighMatch(
                  highLowRangeExprs
                      .getHighRangeExpr()
                      .accept(XrUint16RangeExprToIntMatchExpr.INSTANCE, arg)),
              new StandardCommunityLowMatch(
                  highLowRangeExprs
                      .getLowRangeExpr()
                      .accept(XrUint16RangeExprToIntMatchExpr.INSTANCE, arg))));
    }

    @Override
    public CommunityMatchExpr visitCommunitySetIosRegex(
        XrCommunitySetIosRegex communitySetIosRegex, Configuration arg) {
      return new CommunityMatchRegex(
          ColonSeparatedRendering.instance(), toJavaRegex(communitySetIosRegex.getRegex()));
    }

    private static final CommunitySetElemToCommunityMatchExpr INSTANCE =
        new CommunitySetElemToCommunityMatchExpr();
  }

  private static final class XrUint16RangeExprToIntMatchExpr
      implements XrUint16RangeExprVisitor<IntMatchExpr, Configuration> {
    private static final XrUint16RangeExprToIntMatchExpr INSTANCE =
        new XrUint16RangeExprToIntMatchExpr();

    @Override
    public IntMatchExpr visitLiteralUint16(XrLiteralUint16 literalUint16, Configuration arg) {
      return new IntComparison(IntComparator.EQ, new LiteralInt(literalUint16.getValue()));
    }

    @Override
    public IntMatchExpr visitLiteralUint16Range(
        XrLiteralUint16Range literalUint16Range, Configuration arg) {
      SubRange range = literalUint16Range.getRange();
      return IntMatchAll.of(
          new IntComparison(IntComparator.GE, new LiteralInt(range.getStart())),
          new IntComparison(IntComparator.LE, new LiteralInt(range.getEnd())));
    }

    @Override
    public IntMatchExpr visitUint16Reference(XrUint16Reference uint16Reference, Configuration arg) {
      return new IntComparison(IntComparator.EQ, new VarInt(uint16Reference.getVar()));
    }
  }

  /**
   * Convert {@code communitySet} to a {@link CommunitySetExpr} representing a set of concrete
   * communities for setting or appending to the standard community attribute within a route-policy.
   *
   * <p>Only concrete elements of {@code communitySet} representing a single community are
   * considered.
   */
  public static @Nonnull CommunitySetExpr toCommunitySetExpr(
      XrCommunitySet communitySet, Configuration c) {
    return CommunitySetUnion.of(
        communitySet.getElements().stream()
            .map(elem -> elem.accept(CommunitySetElemToCommunitySetExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  private static final class CommunitySetElemToCommunitySetExpr
      implements XrCommunitySetElemVisitor<CommunitySetExpr, Configuration> {
    @Override
    public CommunitySetExpr visitCommunitySetHighLowRangeExprs(
        XrCommunitySetHighLowRangeExprs highLowRangeExprs, Configuration arg) {
      Optional<IntExpr> highExpr =
          highLowRangeExprs.getHighRangeExpr().accept(XrUint16RangeExprToIntExpr.INSTANCE, null);
      if (!highExpr.isPresent()) {
        return CommunitySetExprs.empty();
      }
      Optional<IntExpr> lowExpr =
          highLowRangeExprs.getLowRangeExpr().accept(XrUint16RangeExprToIntExpr.INSTANCE, null);
      if (!lowExpr.isPresent()) {
        return CommunitySetExprs.empty();
      }
      return CommunityExprsSet.of(new StandardCommunityHighLowExprs(highExpr.get(), lowExpr.get()));
    }

    @Override
    public CommunitySetExpr visitCommunitySetIosRegex(
        XrCommunitySetIosRegex communitySetIosRegex, Configuration arg) {
      return CommunitySetExprs.empty();
    }

    private static final CommunitySetElemToCommunitySetExpr INSTANCE =
        new CommunitySetElemToCommunitySetExpr();
  }

  private static class XrUint16RangeExprToIntExpr
      implements XrUint16RangeExprVisitor<Optional<IntExpr>, Void> {

    private static final XrUint16RangeExprToIntExpr INSTANCE = new XrUint16RangeExprToIntExpr();

    @Override
    public Optional<IntExpr> visitLiteralUint16(XrLiteralUint16 literalUint16, Void arg) {
      return Optional.of(new LiteralInt(literalUint16.getValue()));
    }

    @Override
    public Optional<IntExpr> visitLiteralUint16Range(
        XrLiteralUint16Range literalUint16Range, Void arg) {
      return Optional.empty();
    }

    @Override
    public Optional<IntExpr> visitUint16Reference(XrUint16Reference uint16Reference, Void arg) {
      return Optional.of(new VarInt(uint16Reference.getVar()));
    }
  }

  /**
   * Convert {@code communitySet} to a {@link CommunitySetMatchExpr} that matches a route whose
   * standard community attribute is matched by any element of {@code communitySet}.
   */
  public static @Nonnull CommunitySetMatchExpr convertMatchesAnyToCommunitySetMatchExpr(
      XrCommunitySet communitySet, Configuration c) {
    return new CommunitySetMatchAny(
        communitySet.getElements().stream()
            .map(elem -> elem.accept(CommunitySetElemToCommunitySetMatchExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Convert {@code communitySet} to a {@link CommunitySetMatchExpr} that matches a route whose
   * standard community attribute is matched by every element of {@code communitySet}.
   */
  public static @Nonnull CommunitySetMatchExpr convertMatchesEveryToCommunitySetMatchExpr(
      XrCommunitySet communitySet, Configuration c) {
    return new CommunitySetMatchAll(
        communitySet.getElements().stream()
            .map(elem -> elem.accept(CommunitySetElemToCommunitySetMatchExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  private static final class CommunitySetElemToCommunitySetMatchExpr
      implements XrCommunitySetElemVisitor<CommunitySetMatchExpr, Configuration> {
    private static final CommunitySetElemToCommunitySetMatchExpr INSTANCE =
        new CommunitySetElemToCommunitySetMatchExpr();

    @Override
    public CommunitySetMatchExpr visitCommunitySetHighLowRangeExprs(
        XrCommunitySetHighLowRangeExprs highLowRangeExprs, Configuration arg) {
      return new HasCommunity(
          highLowRangeExprs.accept(CommunitySetElemToCommunityMatchExpr.INSTANCE, arg));
    }

    @Override
    public CommunitySetMatchExpr visitCommunitySetIosRegex(
        XrCommunitySetIosRegex communitySetIosRegex, Configuration arg) {
      return new CommunitySetMatchRegex(
          new TypesFirstAscendingSpaceSeparated(ColonSeparatedRendering.instance()),
          toJavaRegex(communitySetIosRegex.getRegex()));
    }
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
      LeafBgpPeerGroup lpg,
      String vrfName,
      boolean ipv4,
      @Nullable String defaultOriginateExportRouteMapName,
      Configuration c,
      Warnings w) {
    List<Statement> exportPolicyStatements = new ArrayList<>();
    if (lpg.getNextHopSelf() != null && lpg.getNextHopSelf()) {
      exportPolicyStatements.add(new SetNextHop(SelfNextHop.getInstance()));
    }
    if (lpg.getRemovePrivateAs() != null && lpg.getRemovePrivateAs()) {
      exportPolicyStatements.add(Statements.RemovePrivateAs.toStaticStatement());
    }

    // If defaultOriginate is set, generate a default route export policy. Default route will match
    // this policy and get exported without going through the rest of the export policy.
    // TODO Verify that nextHopSelf and removePrivateAs settings apply to default-originate route.
    if (lpg.getDefaultOriginate()) {
      initBgpDefaultRouteExportPolicy(
          vrfName, lpg.getName(), ipv4, defaultOriginateExportRouteMapName, c);
      exportPolicyStatements.add(
          new If(
              "Export default route from peer with default-originate configured",
              new CallExpr(computeBgpDefaultRouteExportPolicyName(ipv4, vrfName, lpg.getName())),
              singletonList(Statements.ReturnTrue.toStaticStatement()),
              ImmutableList.of()));
    }

    // Conditions for exporting regular routes (not spawned by default-originate)
    List<BooleanExpr> peerExportConjuncts = new ArrayList<>();
    peerExportConjuncts.add(new CallExpr(generatedBgpCommonExportPolicyName(vrfName)));

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
        .setName(generatedBgpPeerExportPolicyName(vrfName, lpg.getName()))
        .setStatements(exportPolicyStatements)
        .build();
  }

  /**
   * Initializes export policy for IPv4 or IPv6 default routes if it doesn't already exist. This
   * policy is the same across BGP processes, so only one is created for each configuration.
   *
   * @param ipv4 Whether to initialize the IPv4 or IPv6 default route export policy
   * @param defaultOriginateExportMapName Name of route-map to apply to generated route before
   *     export. This is an Arista-specific concept and <a
   *     href=https://www.arista.com/en/um-eos/eos-section-32-4-bgp-commands#ww1116958>does not
   *     affect whether the route will be exported</a>.
   */
  static void initBgpDefaultRouteExportPolicy(
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
        || !c.getRoutingPolicies().keySet().contains(defaultOriginateExportMapName)) {
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

  /**
   * Computes a mapping of primary {@link Ip}s to the names of interfaces owning them. Filters out
   * the interfaces having no primary {@link ConcreteInterfaceAddress}
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
    ikePhase1Key.setKeyType(IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED);
    ikePhase1Key.setLocalInterface(keyring.getLocalInterfaceName());
    if (keyring.getRemoteIdentity() != null) {
      ikePhase1Key.setRemoteIdentity(keyring.getRemoteIdentity().toIpSpace());
    }
    return ikePhase1Key;
  }

  static IkePhase1Key toIkePhase1Key(@Nonnull NamedRsaPubKey rsaPubKey) {
    IkePhase1Key ikePhase1Key = new IkePhase1Key();
    ikePhase1Key.setKeyHash(rsaPubKey.getKey());
    ikePhase1Key.setKeyType(IkeKeyType.RSA_PUB_KEY);
    if (rsaPubKey.getAddress() != null) {
      ikePhase1Key.setRemoteIdentity(rsaPubKey.getAddress().toIpSpace());
    }
    return ikePhase1Key;
  }

  static IkePhase1Key toIkePhase1Key(@Nonnull IsakmpKey isakmpKey) {
    IkePhase1Key ikePhase1Key = new IkePhase1Key();
    ikePhase1Key.setKeyHash(isakmpKey.getKey());
    ikePhase1Key.setKeyType(isakmpKey.getIkeKeyType());
    ikePhase1Key.setRemoteIdentity(isakmpKey.getAddress());
    return ikePhase1Key;
  }

  static IkePhase1Policy toIkePhase1Policy(
      @Nonnull NamedRsaPubKey rsaPubKey,
      @Nonnull CiscoConfiguration oldConfig,
      @Nonnull IkePhase1Key ikePhase1KeyFromRsaPubKey) {
    IkePhase1Policy ikePhase1Policy = new IkePhase1Policy(getRsaPubKeyGeneratedName(rsaPubKey));

    ikePhase1Policy.setIkePhase1Proposals(
        oldConfig.getIsakmpPolicies().values().stream()
            .filter(
                isakmpPolicy ->
                    isakmpPolicy.getAuthenticationMethod()
                        == IkeAuthenticationMethod.RSA_ENCRYPTED_NONCES)
            .map(isakmpPolicy -> isakmpPolicy.getName().toString())
            .collect(ImmutableList.toImmutableList()));
    if (rsaPubKey.getAddress() != null) {
      ikePhase1Policy.setRemoteIdentity(rsaPubKey.getAddress().toIpSpace());
    }
    ikePhase1Policy.setIkePhase1Key(ikePhase1KeyFromRsaPubKey);
    // RSA pub key is not per interface so unsetting local interface
    ikePhase1Policy.setLocalInterface(UNSET_LOCAL_INTERFACE);
    return ikePhase1Policy;
  }

  static IkePhase1Policy toIkePhase1Policy(
      @Nonnull IsakmpKey isakmpKey,
      @Nonnull CiscoConfiguration oldConfig,
      @Nonnull IkePhase1Key ikePhase1KeyFromIsakmpKey) {
    IkePhase1Policy ikePhase1Policy = new IkePhase1Policy(getIsakmpKeyGeneratedName(isakmpKey));

    ikePhase1Policy.setIkePhase1Proposals(
        oldConfig.getIsakmpPolicies().values().stream()
            .filter(
                isakmpPolicy ->
                    isakmpPolicy.getAuthenticationMethod()
                        == IkeAuthenticationMethod.PRE_SHARED_KEYS)
            .map(isakmpPolicy -> isakmpPolicy.getName().toString())
            .collect(ImmutableList.toImmutableList()));
    ikePhase1Policy.setRemoteIdentity(isakmpKey.getAddress());

    ikePhase1Policy.setIkePhase1Key(ikePhase1KeyFromIsakmpKey);
    // ISAKMP key is not per interface so local interface will not be set
    ikePhase1Policy.setLocalInterface(UNSET_LOCAL_INTERFACE);
    return ikePhase1Policy;
  }

  static String getRsaPubKeyGeneratedName(NamedRsaPubKey namedRsaPubKey) {
    return String.format("~%s_%s~", PREFIX_RSA_PUB, namedRsaPubKey.getName());
  }

  static String getIsakmpKeyGeneratedName(IsakmpKey isakmpKey) {
    return String.format("~%s_%s~", PREFIX_ISAKMP_KEY, isakmpKey.getAddress());
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
      fromLine.getProtocol().ifPresent(p -> newLine.getIpProtocols().add(p));
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
        .setName(computeIcmpObjectGroupAclName(icmpTypeObjectGroup.getName()))
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
        .setName(computeServiceObjectAclName(serviceObject.getName()))
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
        .setName(computeServiceObjectGroupAclName(serviceObjectGroup.getName()))
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
      if (iface.getConcreteAddress() == null) {
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
        .setLocalAddress(iface.getConcreteAddress().getIp());

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

    if (proc.getAsn() == null) {
      oldConfig.getWarnings().redFlag("Invalid EIGRP process");
      return null;
    }

    newProcess.setAsNumber(proc.getAsn());
    newProcess.setMode(proc.getMode());

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
            eigrpRedistributionPoliciesToStatements(
                proc.getRedistributionPolicies().values(), proc, oldConfig));

    return newProcess.build();
  }

  /** Creates an {@link If} statement to allow EIGRP routes redistributed from supplied localAsn */
  @Nonnull
  private static If ifToAllowEigrpToOwnAsn(long localAsn) {
    return new If(
        new Conjunction(
            ImmutableList.of(
                new MatchProtocol(RoutingProtocol.EIGRP, RoutingProtocol.EIGRP_EX),
                new MatchProcessAsn(localAsn))),
        ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
        ImmutableList.of(Statements.ExitReject.toStaticStatement()));
  }

  /**
   * Converts {@link EigrpRedistributionPolicy}s in an {@link EigrpProcess} to equivalent {@link If}
   * statements
   *
   * @param eigrpRedistributionPolicies {@link EigrpRedistributionPolicy}s of the EIGRP process
   * @param vsEigrpProc Vendor specific {@link EigrpProcess}
   * @param vsConfig Vendor specific {@link CiscoConfiguration configuration}
   * @return {@link List} of {@link If} statements
   */
  static List<If> eigrpRedistributionPoliciesToStatements(
      Collection<EigrpRedistributionPolicy> eigrpRedistributionPolicies,
      EigrpProcess vsEigrpProc,
      CiscoConfiguration vsConfig) {
    return eigrpRedistributionPolicies.stream()
        .map(policy -> convertEigrpRedistributionPolicy(policy, vsEigrpProc, vsConfig))
        .filter(Objects::nonNull)
        .collect(ImmutableList.toImmutableList());
  }

  @Nullable
  private static If convertEigrpRedistributionPolicy(
      EigrpRedistributionPolicy policy, EigrpProcess proc, CiscoConfiguration oldConfig) {
    RoutingProtocol protocol = policy.getSourceProtocol();
    // All redistribution must match the specified protocol.
    Conjunction eigrpExportConditions = new Conjunction();
    BooleanExpr matchExpr;
    if (protocol == RoutingProtocol.EIGRP) {
      matchExpr = new MatchProtocol(RoutingProtocol.EIGRP, RoutingProtocol.EIGRP_EX);

      Long otherAsn =
          (Long) policy.getSpecialAttributes().get(EigrpRedistributionPolicy.EIGRP_AS_NUMBER);
      if (otherAsn == null) {
        oldConfig
            .getWarnings()
            .redFlag(
                String.format(
                    "Unable to redistribute %s into EIGRP proc %s - policy has no ASN",
                    protocol, proc.getAsn()));
        return null;
      }
      eigrpExportConditions.getConjuncts().add(new MatchProcessAsn(otherAsn));
    } else if (protocol == RoutingProtocol.ISIS_ANY) {
      matchExpr =
          new MatchProtocol(
              RoutingProtocol.ISIS_EL1,
              RoutingProtocol.ISIS_EL2,
              RoutingProtocol.ISIS_L1,
              RoutingProtocol.ISIS_L2);
    } else {
      matchExpr = new MatchProtocol(protocol);
    }
    eigrpExportConditions.getConjuncts().add(matchExpr);

    // Default routes can be redistributed into EIGRP. Don't filter them.

    ImmutableList.Builder<Statement> eigrpExportStatements = ImmutableList.builder();

    // Set the metric
    // TODO prefer metric from route map
    // https://github.com/batfish/batfish/issues/2070
    EigrpMetricValues metric =
        policy.getMetric() != null
            ? policy.getMetric()
            : Optional.ofNullable(proc.getDefaultMetric()).map(EigrpMetric::getValues).orElse(null);
    if (metric != null) {
      eigrpExportStatements.add(new SetEigrpMetric(new LiteralEigrpMetric(metric)));
    } else if (protocol != RoutingProtocol.EIGRP) {
      /*
       * TODO no default metric (and not EIGRP into EIGRP)
       * 1) connected can use the interface metric
       * 2) static with next hop interface can use the interface metric
       * 3) If none of the above, bad configuration
       */
      oldConfig
          .getWarnings()
          .redFlag(
              String.format(
                  "Unable to redistribute %s into EIGRP proc %s - no metric",
                  protocol, proc.getAsn()));
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
                        l.getAction(), IpWildcard.create(l.getPrefix()), l.getLengthRange()))
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
   * PrefixList} as the {@link DistributeList#getFilterType()}. {@link
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
      @Nonnull org.batfish.datamodel.ospf.OspfProcess newOspfProcess,
      @Nonnull Configuration c,
      @Nonnull String vrf,
      @Nonnull String ospfProcessId,
      @Nonnull CiscoConfiguration oldConfig,
      @Nonnull Warnings w) {
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

    for (String ifaceName :
        newOspfProcess.getAreas().values().stream()
            .flatMap(a -> a.getInterfaces().stream())
            .collect(Collectors.toList())) {
      org.batfish.datamodel.Interface iface = c.getVrfs().get(vrf).getInterfaces().get(ifaceName);
      DistributeList ifaceDistributeList = interfaceDistributeLists.get(ifaceName);
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

      String policyName = String.format("~OSPF_DIST_LIST_%s_%s_%s~", vrf, ospfProcessId, ifaceName);
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
      OspfInterfaceSettings ospfSettings = iface.getOspfSettings();
      if (ospfSettings == null) {
        w.redFlag(
            String.format(
                "Cannot attach inbound distribute list policy '%s' to interface '%s' not configured for OSPF.",
                ifaceName, iface.getName()));
      } else {
        ospfSettings.setInboundDistributeListPolicy(policyName);
      }
    }
  }

  /**
   * Given a list of {@link If} statements, sets the false statements of every {@link If} to an
   * empty list and adds a rule at the end to allow EIGRP from provided ownAsn.
   */
  static List<If> clearFalseStatementsAndAddMatchOwnAsn(List<If> redistributeIfs, long ownAsn) {
    List<Statement> emptyFalseStatements = ImmutableList.of();
    List<If> redistributeIfsWithEmptyFalse =
        redistributeIfs.stream()
            .map(
                redistributionStatement ->
                    new If(
                        redistributionStatement.getGuard(),
                        redistributionStatement.getTrueStatements(),
                        emptyFalseStatements))
            .collect(Collectors.toList());

    redistributeIfsWithEmptyFalse.add(ifToAllowEigrpToOwnAsn(ownAsn));

    return ImmutableList.copyOf(redistributeIfsWithEmptyFalse);
  }

  /**
   * Inserts an {@link If} generated from the provided distributeList to the beginning of
   * existingStatements and creates a {@link RoutingPolicy} from the result
   */
  static RoutingPolicy insertDistributeListFilterAndGetPolicy(
      @Nonnull Configuration c,
      @Nonnull CiscoConfiguration vsConfig,
      @Nullable DistributeList distributeList,
      @Nonnull List<If> existingStatements,
      @Nonnull String name) {
    ImmutableList.Builder<Statement> combinedStatments = ImmutableList.builder();
    if (distributeList != null && sanityCheckEigrpDistributeList(c, distributeList, vsConfig)) {
      combinedStatments.add(
          new If(
              new MatchPrefixSet(
                  DestinationNetwork.instance(),
                  new NamedPrefixSet(distributeList.getFilterName())),
              ImmutableList.of(),
              ImmutableList.of(Statements.ExitReject.toStaticStatement())));
    }
    combinedStatments.addAll(existingStatements);
    return RoutingPolicy.builder()
        .setOwner(c)
        .setName(name)
        .setStatements(combinedStatments.build())
        .build();
  }

  /**
   * Checks if the {@link DistributeList distributeList} can be converted to a routing policy.
   * Returns false if it refers to an extended access list, which is not supported and also returns
   * false if the access-list referred by it does not exist.
   *
   * <p>Adds appropriate {@link org.batfish.common.Warning} if the {@link DistributeList
   * distributeList} is not found to be valid for conversion to routing policy.
   *
   * @param c Vendor independent {@link Configuration configuration}
   * @param distributeList {@link DistributeList distributeList} to be validated
   * @param vsConfig Vendor specific {@link CiscoConfiguration configuration}
   * @return false if the {@link DistributeList distributeList} cannot be converted to a routing
   *     policy
   */
  static boolean sanityCheckEigrpDistributeList(
      @Nonnull Configuration c,
      @Nonnull DistributeList distributeList,
      @Nonnull CiscoConfiguration vsConfig) {
    if (distributeList.getFilterType() == DistributeListFilterType.ACCESS_LIST
        && vsConfig.getExtendedAcls().containsKey(distributeList.getFilterName())) {
      vsConfig
          .getWarnings()
          .redFlag(
              String.format(
                  "Extended access lists are not supported in EIGRP distribute-lists: %s",
                  distributeList.getFilterName()));
      return false;
    } else if (!c.getRouteFilterLists().containsKey(distributeList.getFilterName())) {
      // if referred access-list is not defined, all prefixes will be allowed
      vsConfig
          .getWarnings()
          .redFlag(
              String.format(
                  "distribute-list refers an undefined access-list `%s`, it will not filter anything",
                  distributeList.getFilterName()));
      return false;
    }
    return true;
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
        .setTag(firstNonNull(staticRoute.getTag(), -1L))
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

  private static CommunityListLine toCommunityListLine(ExpandedCommunityListLine eclLine) {
    String javaRegex = CiscoConfiguration.toJavaRegex(eclLine.getRegex());
    return new CommunityListLine(eclLine.getAction(), new RegexCommunitySet(javaRegex));
  }

  private static CommunityListLine toCommunityListLine(StandardCommunityListLine sclLine) {
    Collection<Long> lineCommunities = sclLine.getCommunities();
    org.batfish.datamodel.routing_policy.expr.CommunitySetExpr expr =
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
    long maxSubnet = minSubnet | dstIpWildcard.getWildcardMask();
    int minPrefixLength = dstIpWildcard.getIp().numSubnetBits();
    int maxPrefixLength = Ip.create(maxSubnet).numSubnetBits();
    int statedPrefixLength = srcIpWildcard.getWildcardMaskAsIp().inverted().numSubnetBits();
    int prefixLength = Math.min(statedPrefixLength, minPrefixLength);
    Prefix prefix = Prefix.create(ip, prefixLength);
    return new RouteFilterLine(
        action, IpWildcard.create(prefix), new SubRange(minPrefixLength, maxPrefixLength));
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
        IpWildcard.create(prefix),
        new SubRange(prefix.getPrefixLength(), Prefix.MAX_PREFIX_LENGTH));
  }

  /**
   * Helper to infer dead interval from configured OSPF settings on an interface. Check explicitly
   * set dead interval, infer from hello interval, or infer from OSPF network type, in that order.
   * See https://www.cisco.com/c/en/us/support/docs/ip/open-shortest-path-first-ospf/13689-17.html
   * for more details.
   */
  @VisibleForTesting
  static int toOspfDeadInterval(
      Interface iface, @Nullable org.batfish.datamodel.ospf.OspfNetworkType networkType) {
    Integer deadInterval = iface.getOspfDeadInterval();
    if (deadInterval != null) {
      return deadInterval;
    }
    Integer helloInterval = iface.getOspfHelloInterval();
    if (helloInterval != null) {
      return OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * helloInterval;
    }
    if (networkType == POINT_TO_POINT || networkType == BROADCAST) {
      return DEFAULT_OSPF_DEAD_INTERVAL_P2P_AND_BROADCAST;
    }
    return DEFAULT_OSPF_DEAD_INTERVAL;
  }

  /**
   * Helper to infer hello interval from configured OSPF settings on an interface. Check explicitly
   * set hello interval or infer from OSPF network type, in that order. See
   * https://www.cisco.com/c/en/us/support/docs/ip/open-shortest-path-first-ospf/13689-17.html for
   * more details.
   */
  @VisibleForTesting
  static int toOspfHelloInterval(
      Interface iface, @Nullable org.batfish.datamodel.ospf.OspfNetworkType networkType) {
    Integer helloInterval = iface.getOspfHelloInterval();
    if (helloInterval != null) {
      return helloInterval;
    }
    if (networkType == POINT_TO_POINT || networkType == BROADCAST) {
      return DEFAULT_OSPF_HELLO_INTERVAL_P2P_AND_BROADCAST;
    }
    return DEFAULT_OSPF_HELLO_INTERVAL;
  }

  /** Helper to convert Cisco VS OSPF network type to VI model type. */
  @VisibleForTesting
  @Nullable
  static org.batfish.datamodel.ospf.OspfNetworkType toOspfNetworkType(
      @Nullable OspfNetworkType type, Warnings warnings) {
    if (type == null) {
      // default is broadcast for all Ethernet interfaces
      // (https://learningnetwork.cisco.com/thread/66827)
      return org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
    }
    switch (type) {
      case BROADCAST:
        return org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
      case POINT_TO_POINT:
        return org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
      case NON_BROADCAST:
        return org.batfish.datamodel.ospf.OspfNetworkType.NON_BROADCAST_MULTI_ACCESS;
      case POINT_TO_MULTIPOINT:
        return org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_MULTIPOINT;
      default:
        warnings.redFlag(
            String.format(
                "Conversion of Cisco OSPF network type '%s' is not handled.", type.toString()));
        return null;
    }
  }

  private CiscoConversions() {} // prevent instantiation of utility class
}
