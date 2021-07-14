package org.batfish.representation.cisco_xr;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.batfish.datamodel.IkePhase1Policy.PREFIX_ISAKMP_KEY;
import static org.batfish.datamodel.IkePhase1Policy.PREFIX_RSA_PUB;
import static org.batfish.datamodel.Interface.INVALID_LOCAL_INTERFACE;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.datamodel.Names.generatedBgpCommonExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpDefaultRouteExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerImportPolicyName;
import static org.batfish.datamodel.Names.generatedOspfInboundDistributeListName;
import static org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
import static org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
import static org.batfish.datamodel.routing_policy.Common.generateSuppressionPolicy;
import static org.batfish.datamodel.routing_policy.statement.Statements.ExitAccept;
import static org.batfish.datamodel.routing_policy.statement.Statements.ExitReject;
import static org.batfish.representation.cisco_xr.CiscoXrConfiguration.computeAbfIpv4PolicyName;
import static org.batfish.representation.cisco_xr.CiscoXrConfiguration.toJavaRegex;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX_LIST;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.Ip6AccessListLine;
import org.batfish.datamodel.Ip6Wildcard;
import org.batfish.datamodel.IpAccessList;
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
import org.batfish.datamodel.Route6FilterLine;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.VrfLeakingConfig;
import org.batfish.datamodel.VrfLeakingConfig.BgpLeakConfig;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.bgp.BgpAggregate;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.eigrp.EigrpMetricValues;
import org.batfish.datamodel.eigrp.EigrpMetricVersion;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.packet_policy.Drop;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.FibLookupOverrideLookupIp;
import org.batfish.datamodel.packet_policy.IngressInterfaceVrf;
import org.batfish.datamodel.packet_policy.LiteralVrfName;
import org.batfish.datamodel.packet_policy.PacketMatchExpr;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.packet_policy.Return;
import org.batfish.datamodel.packet_policy.VrfExpr;
import org.batfish.datamodel.route.nh.NextHop;
import org.batfish.datamodel.routing_policy.Common;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchAny;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchExpr;
import org.batfish.datamodel.routing_policy.as_path.AsPathMatchRegex;
import org.batfish.datamodel.routing_policy.as_path.AsSetsMatchingRanges;
import org.batfish.datamodel.routing_policy.as_path.HasAsPathLength;
import org.batfish.datamodel.routing_policy.communities.ColonSeparatedRendering;
import org.batfish.datamodel.routing_policy.communities.CommunityExprsSet;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchAny;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExprs;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAll;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchAny;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchRegex;
import org.batfish.datamodel.routing_policy.communities.CommunitySetUnion;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityGlobalAdministratorHighMatch;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityGlobalAdministratorLowMatch;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityGlobalAdministratorMatch;
import org.batfish.datamodel.routing_policy.communities.ExtendedCommunityLocalAdministratorMatch;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.communities.MatchCommunities;
import org.batfish.datamodel.routing_policy.communities.RouteTargetExtendedCommunities;
import org.batfish.datamodel.routing_policy.communities.RouteTargetExtendedCommunityExpr;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityHighLowExprs;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityHighMatch;
import org.batfish.datamodel.routing_policy.communities.StandardCommunityLowMatch;
import org.batfish.datamodel.routing_policy.communities.TypesFirstAscendingSpaceSeparated;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.IntComparator;
import org.batfish.datamodel.routing_policy.expr.IntComparison;
import org.batfish.datamodel.routing_policy.expr.IntExpr;
import org.batfish.datamodel.routing_policy.expr.IntMatchAll;
import org.batfish.datamodel.routing_policy.expr.IntMatchExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralEigrpMetric;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.LongComparison;
import org.batfish.datamodel.routing_policy.expr.LongExpr;
import org.batfish.datamodel.routing_policy.expr.LongMatchAll;
import org.batfish.datamodel.routing_policy.expr.LongMatchExpr;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProcessAsn;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.expr.Uint32HighLowExpr;
import org.batfish.datamodel.routing_policy.expr.VarInt;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetEigrpMetric;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.visitors.HeaderSpaceConverter;
import org.batfish.representation.cisco_xr.DistributeList.DistributeListFilterType;
import org.batfish.vendor.VendorStructureId;

/** Utilities that convert CiscoXr-specific representations to vendor-independent model. */
@ParametersAreNonnullByDefault
public class CiscoXrConversions {

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
      Configuration c,
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
      CryptoMapSet ciscoXrCryptoMapSet,
      Map<String, CryptoMapSet> cryptoMapSets,
      Warnings w) {
    if (ciscoXrCryptoMapSet.getDynamic()) {
      return;
    }
    for (CryptoMapEntry cryptoMapEntry : ciscoXrCryptoMapSet.getCryptoMapEntries()) {
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

  static @Nonnull BgpAggregate toBgpAggregate(
      BgpAggregateIpv4Network vsAggregate, Configuration c, Warnings w) {
    // undefined -> null for best effort on invalid config
    String routePolicy = vsAggregate.getRoutePolicy();
    if (routePolicy != null && !c.getRoutingPolicies().containsKey(routePolicy)) {
      w.redFlag(String.format("Ignoring undefined aggregate-address route-policy %s", routePolicy));
      routePolicy = null;
    }
    // TODO: handle as-set by generating generation policy wrapping route-policy
    return BgpAggregate.of(
        vsAggregate.getPrefix(),
        generateSuppressionPolicy(vsAggregate.getSummaryOnly(), c),
        routePolicy,
        null);
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

    @Override
    public CommunityMatchExpr visitCommunitySetDfaRegex(
        XrCommunitySetDfaRegex communitySetDfaRegex, Configuration arg) {
      // TODO: differentiate from IOS regex
      return new CommunityMatchRegex(
          ColonSeparatedRendering.instance(), toJavaRegex(communitySetDfaRegex.getRegex()));
    }

    @Override
    public CommunityMatchExpr visitWildcardCommunitySetElem(
        XrWildcardCommunitySetElem xrWildcardCommunitySetElem) {
      return new CommunityMatchAll(ImmutableList.of());
    }

    private static final CommunitySetElemToCommunityMatchExpr INSTANCE =
        new CommunitySetElemToCommunityMatchExpr();
  }

  /**
   * Convert {@code extcommunitySetRt} to a {@link CommunityMatchExpr} to be applied as a deletion
   * criterion against each route-target community in a route's extended community attribute.
   */
  static @Nonnull CommunityMatchExpr toCommunityMatchExpr(
      ExtcommunitySetRt extcommunitySetRt, Configuration c) {
    return new CommunityMatchAny(
        extcommunitySetRt.getElements().stream()
            .map(elem -> elem.accept(ExtcommunitySetRtElemToCommunityMatchExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  private static final class ExtcommunitySetRtElemToCommunityMatchExpr
      implements ExtcommunitySetRtElemVisitor<CommunityMatchExpr, Configuration> {

    private static final ExtcommunitySetRtElemToCommunityMatchExpr INSTANCE =
        new ExtcommunitySetRtElemToCommunityMatchExpr();

    @Override
    public CommunityMatchExpr visitExtcommunitySetRtElemAsDotColon(
        ExtcommunitySetRtElemAsDotColon extcommunitySetRtElemAsDotColon, Configuration arg) {
      return new CommunityMatchAll(
          ImmutableList.of(
              RouteTargetExtendedCommunities.instance(),
              new ExtendedCommunityGlobalAdministratorHighMatch(
                  extcommunitySetRtElemAsDotColon
                      .getGaHighRangeExpr()
                      .accept(XrUint16RangeExprToIntMatchExpr.INSTANCE, arg)),
              new ExtendedCommunityGlobalAdministratorLowMatch(
                  extcommunitySetRtElemAsDotColon
                      .getGaLowRangeExpr()
                      .accept(XrUint16RangeExprToIntMatchExpr.INSTANCE, arg)),
              new ExtendedCommunityLocalAdministratorMatch(
                  extcommunitySetRtElemAsDotColon
                      .getLaRangeExpr()
                      .accept(XrUint16RangeExprToIntMatchExpr.INSTANCE, arg))));
    }

    @Override
    public CommunityMatchExpr visitExtcommunitySetRtElemAsColon(
        ExtcommunitySetRtElemAsColon extcommunitySetRtElemAsColon, Configuration arg) {
      return new CommunityMatchAll(
          ImmutableList.of(
              RouteTargetExtendedCommunities.instance(),
              new ExtendedCommunityGlobalAdministratorMatch(
                  extcommunitySetRtElemAsColon
                      .getGaRangeExpr()
                      .accept(XrUint32RangeExprToLongMatchExpr.INSTANCE, arg)),
              new ExtendedCommunityLocalAdministratorMatch(
                  extcommunitySetRtElemAsColon
                      .getLaRangeExpr()
                      .accept(XrUint16RangeExprToIntMatchExpr.INSTANCE, arg))));
    }
  }

  private static final class XrUint16RangeExprToIntMatchExpr
      implements Uint16RangeExprVisitor<IntMatchExpr, Configuration> {
    private static final XrUint16RangeExprToIntMatchExpr INSTANCE =
        new XrUint16RangeExprToIntMatchExpr();

    @Override
    public IntMatchExpr visitLiteralUint16(LiteralUint16 literalUint16, Configuration arg) {
      return new IntComparison(IntComparator.EQ, new LiteralInt(literalUint16.getValue()));
    }

    @Override
    public IntMatchExpr visitLiteralUint16Range(
        LiteralUint16Range literalUint16Range, Configuration arg) {
      SubRange range = literalUint16Range.getRange();
      return IntMatchAll.of(
          new IntComparison(IntComparator.GE, new LiteralInt(range.getStart())),
          new IntComparison(IntComparator.LE, new LiteralInt(range.getEnd())));
    }

    @Override
    public IntMatchExpr visitUint16Reference(Uint16Reference uint16Reference, Configuration arg) {
      return new IntComparison(IntComparator.EQ, new VarInt(uint16Reference.getVar()));
    }

    @Override
    public IntMatchExpr visitWildcardUint16RangeExpr(
        WildcardUint16RangeExpr wildcardUint16RangeExpr) {
      return IntMatchAll.of();
    }

    @Override
    public IntMatchExpr visitPrivateAs(PrivateAs privateAs) {
      return IntMatchAll.of(
          new IntComparison(IntComparator.GE, new LiteralInt(64512)),
          new IntComparison(IntComparator.LE, new LiteralInt(65534)));
    }

    @Override
    public IntMatchExpr visitPeerAs(PeerAs peerAs) {
      // TODO: implement. In the meanwhile, prevent matching by using impossible condition
      return new IntComparison(IntComparator.LT, new LiteralInt(0));
    }
  }

  private static final class XrUint32RangeExprToLongMatchExpr
      implements Uint32RangeExprVisitor<LongMatchExpr, Configuration> {
    @Override
    public LongMatchExpr visitLiteralUint32(LiteralUint32 literalUint32, Configuration arg) {
      return new LongComparison(IntComparator.EQ, new LiteralLong(literalUint32.getValue()));
    }

    @Override
    public LongMatchExpr visitWildcardUint32RangeExpr(
        WildcardUint32RangeExpr wildcardUint32RangeExpr) {
      return LongMatchAll.of();
    }

    private static final XrUint32RangeExprToLongMatchExpr INSTANCE =
        new XrUint32RangeExprToLongMatchExpr();
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
    public CommunitySetExpr visitCommunitySetDfaRegex(
        XrCommunitySetDfaRegex xrCommunitySetDfaRegex, Configuration arg) {
      return CommunitySetExprs.empty();
    }

    @Override
    public CommunitySetExpr visitCommunitySetIosRegex(
        XrCommunitySetIosRegex communitySetIosRegex, Configuration arg) {
      return CommunitySetExprs.empty();
    }

    @Override
    public CommunitySetExpr visitWildcardCommunitySetElem(
        XrWildcardCommunitySetElem xrWildcardCommunitySetElem) {
      return CommunitySetExprs.empty();
    }

    private static final CommunitySetElemToCommunitySetExpr INSTANCE =
        new CommunitySetElemToCommunitySetExpr();
  }

  /**
   * Convert {@code extcommunitySetRt} to a {@link CommunitySetExpr} representing a set of concrete
   * communities for setting or appending to the extended community attribute within a route-policy.
   *
   * <p>Only concrete elements of {@code extcommunitySetRt} representing a single community are
   * considered.
   */
  public static @Nonnull CommunitySetExpr toCommunitySetExpr(
      ExtcommunitySetRt extcommunitySetRt, Configuration c) {
    return CommunitySetUnion.of(
        extcommunitySetRt.getElements().stream()
            .map(elem -> elem.accept(ExtcommunitySetRtElemToCommunitySetExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  private static final class ExtcommunitySetRtElemToCommunitySetExpr
      implements ExtcommunitySetRtElemVisitor<CommunitySetExpr, Configuration> {
    @Override
    public CommunitySetExpr visitExtcommunitySetRtElemAsDotColon(
        ExtcommunitySetRtElemAsDotColon extcommunitySetRtElemAsDotColon, Configuration arg) {
      Optional<IntExpr> gaHighExpr =
          extcommunitySetRtElemAsDotColon
              .getGaHighRangeExpr()
              .accept(XrUint16RangeExprToIntExpr.INSTANCE, null);
      if (!gaHighExpr.isPresent()) {
        return CommunitySetExprs.empty();
      }
      Optional<IntExpr> gaLowExpr =
          extcommunitySetRtElemAsDotColon
              .getGaLowRangeExpr()
              .accept(XrUint16RangeExprToIntExpr.INSTANCE, null);
      if (!gaLowExpr.isPresent()) {
        return CommunitySetExprs.empty();
      }
      Optional<IntExpr> laExpr =
          extcommunitySetRtElemAsDotColon
              .getLaRangeExpr()
              .accept(XrUint16RangeExprToIntExpr.INSTANCE, null);
      if (!laExpr.isPresent()) {
        return CommunitySetExprs.empty();
      }
      return CommunityExprsSet.of(
          new RouteTargetExtendedCommunityExpr(
              new Uint32HighLowExpr(gaHighExpr.get(), gaLowExpr.get()), laExpr.get()));
    }

    @Override
    public CommunitySetExpr visitExtcommunitySetRtElemAsColon(
        ExtcommunitySetRtElemAsColon extcommunitySetRtElemAsColon, Configuration arg) {
      Optional<LongExpr> gaExpr =
          extcommunitySetRtElemAsColon
              .getGaRangeExpr()
              .accept(XrUint32RangeExprToLongExpr.INSTANCE, null);
      if (!gaExpr.isPresent()) {
        return CommunitySetExprs.empty();
      }
      Optional<IntExpr> laExpr =
          extcommunitySetRtElemAsColon
              .getLaRangeExpr()
              .accept(XrUint16RangeExprToIntExpr.INSTANCE, null);
      if (!laExpr.isPresent()) {
        return CommunitySetExprs.empty();
      }
      return CommunityExprsSet.of(new RouteTargetExtendedCommunityExpr(gaExpr.get(), laExpr.get()));
    }

    private static final ExtcommunitySetRtElemToCommunitySetExpr INSTANCE =
        new ExtcommunitySetRtElemToCommunitySetExpr();
  }

  private static class XrUint16RangeExprToIntExpr
      implements Uint16RangeExprVisitor<Optional<IntExpr>, Void> {

    private static final XrUint16RangeExprToIntExpr INSTANCE = new XrUint16RangeExprToIntExpr();

    @Override
    public Optional<IntExpr> visitLiteralUint16(LiteralUint16 literalUint16, Void arg) {
      return Optional.of(new LiteralInt(literalUint16.getValue()));
    }

    @Override
    public Optional<IntExpr> visitLiteralUint16Range(
        LiteralUint16Range literalUint16Range, Void arg) {
      return Optional.empty();
    }

    @Override
    public Optional<IntExpr> visitUint16Reference(Uint16Reference uint16Reference, Void arg) {
      return Optional.of(new VarInt(uint16Reference.getVar()));
    }

    @Override
    public Optional<IntExpr> visitWildcardUint16RangeExpr(
        WildcardUint16RangeExpr wildcardUint16RangeExpr) {
      return Optional.empty();
    }

    @Override
    public Optional<IntExpr> visitPrivateAs(PrivateAs privateAs) {
      return Optional.empty();
    }

    @Override
    public Optional<IntExpr> visitPeerAs(PeerAs peerAs) {
      // TODO: implement
      return Optional.empty();
    }
  }

  private static class XrUint32RangeExprToLongExpr
      implements Uint32RangeExprVisitor<Optional<LongExpr>, Void> {

    @Override
    public Optional<LongExpr> visitLiteralUint32(LiteralUint32 literalUint32, Void arg) {
      return Optional.of(new LiteralLong(literalUint32.getValue()));
    }

    @Override
    public Optional<LongExpr> visitWildcardUint32RangeExpr(
        WildcardUint32RangeExpr wildcardUint32RangeExpr) {
      return Optional.empty();
    }

    private static final XrUint32RangeExprToLongExpr INSTANCE = new XrUint32RangeExprToLongExpr();
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
    public CommunitySetMatchExpr visitCommunitySetDfaRegex(
        XrCommunitySetDfaRegex communitySetDfaRegex, Configuration arg) {
      // TODO: properly differentiate from ios-regex
      return new CommunitySetMatchRegex(
          new TypesFirstAscendingSpaceSeparated(ColonSeparatedRendering.instance()),
          toJavaRegex(communitySetDfaRegex.getRegex()));
    }

    @Override
    public CommunitySetMatchExpr visitCommunitySetIosRegex(
        XrCommunitySetIosRegex communitySetIosRegex, Configuration arg) {
      return new CommunitySetMatchRegex(
          new TypesFirstAscendingSpaceSeparated(ColonSeparatedRendering.instance()),
          toJavaRegex(communitySetIosRegex.getRegex()));
    }

    @Override
    public CommunitySetMatchExpr visitWildcardCommunitySetElem(
        XrWildcardCommunitySetElem xrWildcardCommunitySetElem) {
      return new HasCommunity(new CommunityMatchAll(ImmutableList.of()));
    }
  }

  /**
   * Convert {@code extcommunitySetRt} to a {@link CommunitySetMatchExpr} that matches a route whose
   * extended community attribute's route-targets are matched by any element of {@code
   * extcommunitySetRt}.
   */
  public static @Nonnull CommunitySetMatchExpr convertMatchesAnyToCommunitySetMatchExpr(
      ExtcommunitySetRt extcommunitySetRt, Configuration c) {
    return new CommunitySetMatchAny(
        extcommunitySetRt.getElements().stream()
            .map(elem -> elem.accept(ExtcommunitySetRtElemToCommunitySetMatchExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Convert {@code extcommunitySetRt} to a {@link CommunitySetMatchExpr} that matches a route whose
   * extended community attribute's route-targets are matched by every element of {@code
   * extcommunitySetRt}.
   */
  public static @Nonnull CommunitySetMatchExpr convertMatchesEveryToCommunitySetMatchExpr(
      ExtcommunitySetRt extcommunitySetRt, Configuration c) {
    return new CommunitySetMatchAll(
        extcommunitySetRt.getElements().stream()
            .map(elem -> elem.accept(ExtcommunitySetRtElemToCommunitySetMatchExpr.INSTANCE, c))
            .collect(ImmutableSet.toImmutableSet()));
  }

  private static final class ExtcommunitySetRtElemToCommunitySetMatchExpr
      implements ExtcommunitySetRtElemVisitor<CommunitySetMatchExpr, Configuration> {
    @Override
    public CommunitySetMatchExpr visitExtcommunitySetRtElemAsDotColon(
        ExtcommunitySetRtElemAsDotColon extcommunitySetRtElemAsDotColon, Configuration arg) {
      return new HasCommunity(
          extcommunitySetRtElemAsDotColon.accept(
              ExtcommunitySetRtElemToCommunityMatchExpr.INSTANCE, arg));
    }

    @Override
    public CommunitySetMatchExpr visitExtcommunitySetRtElemAsColon(
        ExtcommunitySetRtElemAsColon extcommunitySetRtElemAsColon, Configuration arg) {
      return new HasCommunity(
          extcommunitySetRtElemAsColon.accept(
              ExtcommunitySetRtElemToCommunityMatchExpr.INSTANCE, arg));
    }

    private static final ExtcommunitySetRtElemToCommunitySetMatchExpr INSTANCE =
        new ExtcommunitySetRtElemToCommunitySetMatchExpr();
  }

  /**
   * Returns the name of a {@link RoutingPolicy} to be used as the BGP import policy for the given
   * {@link LeafBgpPeerGroup}, or {@code null} if no constraints are imposed on the peer's inbound
   * routes. When a nonnull policy name is returned, the corresponding policy is guaranteed to exist
   * in the given configuration's routing policies.
   */
  @Nullable
  static String generateBgpImportPolicy(
      LeafBgpPeerGroup lpg, long localAs, String vrfName, Configuration c) {
    // TODO Support filter-list
    // https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/5816-bgpfaq-5816.html

    String inboundRouteMapName = lpg.getInboundRouteMap();
    if (inboundRouteMapName != null && c.getRoutingPolicies().containsKey(inboundRouteMapName)) {
      // Inbound route-map is defined. Use that as the BGP import policy.
      return inboundRouteMapName;
    }
    // Warnings for references will be surfaced elsewhere.

    if (!Objects.equals(localAs, lpg.getRemoteAs())) {
      // For EBGP peers, if no inbound filter is defined, deny all routes (special XR behavior)
      Statement rejectAll = Statements.ExitReject.toStaticStatement();
      String policyName = generatedBgpPeerImportPolicyName(vrfName, lpg.getName());
      RoutingPolicy.builder().setOwner(c).setName(policyName).addStatement(rejectAll).build();
      return policyName;
    }
    // Return null to indicate no constraints were imposed on inbound BGP routes.
    return null;
  }

  /**
   * Creates a {@link RoutingPolicy} to be used as the BGP export policy for the given {@link
   * LeafBgpPeerGroup}. The generated policy is added to the given configuration's routing policies.
   */
  static void generateBgpExportPolicy(
      LeafBgpPeerGroup lpg, long localAs, String vrfName, boolean ipv4, Configuration c) {
    RoutingPolicy.Builder exportPolicy =
        RoutingPolicy.builder()
            .setOwner(c)
            .setName(generatedBgpPeerExportPolicyName(vrfName, lpg.getName()));
    if (lpg.getNextHopSelf() != null && lpg.getNextHopSelf()) {
      exportPolicy.addStatement(new SetNextHop(SelfNextHop.getInstance()));
    }
    if (lpg.getRemovePrivateAs() != null && lpg.getRemovePrivateAs()) {
      exportPolicy.addStatement(Statements.RemovePrivateAs.toStaticStatement());
    }

    // If defaultOriginate is set, generate a default route export policy. Default route will match
    // this policy and get exported without going through the rest of the export policy.
    // TODO Verify that nextHopSelf and removePrivateAs settings apply to default-originate route.
    // TODO Verify that default route can be originated even if no export filter is configured.
    if (lpg.getDefaultOriginate()) {
      initBgpDefaultRouteExportPolicy(ipv4, c);
      exportPolicy.addStatement(
          new If(
              "Export default route from peer with default-originate configured",
              new CallExpr(generatedBgpDefaultRouteExportPolicyName(ipv4)),
              singletonList(Statements.ReturnTrue.toStaticStatement()),
              ImmutableList.of()));
    }

    // Conditions for exporting regular routes (not spawned by default-originate)
    List<BooleanExpr> peerExportConjuncts = new ArrayList<>();
    peerExportConjuncts.add(new CallExpr(generatedBgpCommonExportPolicyName(vrfName)));

    // Add constraints on export routes from configured outbound filter.
    String outboundRouteMapName = lpg.getOutboundRouteMap();
    if (outboundRouteMapName != null && c.getRoutingPolicies().containsKey(outboundRouteMapName)) {
      peerExportConjuncts.add(new CallExpr(outboundRouteMapName));
    } else if (!Objects.equals(localAs, lpg.getRemoteAs())) {
      // For EBGP peers, if no outbound filter is defined, deny all routes (special XR behavior)
      peerExportConjuncts.add(BooleanExprs.FALSE);
    }
    exportPolicy
        .addStatement(
            new If(
                "peer-export policy main conditional: exitAccept if true / exitReject if false",
                new Conjunction(peerExportConjuncts),
                ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                ImmutableList.of(Statements.ExitReject.toStaticStatement())))
        .build();
  }

  /**
   * Initializes export policy for IPv4 or IPv6 default routes if it doesn't already exist. This
   * policy is the same across BGP processes, so at most two are created for each configuration, for
   * IPv4 and IPv6.
   *
   * @param ipv4 Whether to initialize the IPv4 or IPv6 default route export policy
   */
  static void initBgpDefaultRouteExportPolicy(boolean ipv4, Configuration c) {
    String defaultRouteExportPolicyName = generatedBgpDefaultRouteExportPolicyName(ipv4);
    if (!c.getRoutingPolicies().containsKey(defaultRouteExportPolicyName)) {
      SetOrigin setOrigin = new SetOrigin(new LiteralOrigin(OriginType.IGP, null));
      List<Statement> defaultRouteExportStatements =
          ImmutableList.of(setOrigin, Statements.ReturnTrue.toStaticStatement());
      RoutingPolicy.builder()
          .setOwner(c)
          .setName(defaultRouteExportPolicyName)
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

  static org.batfish.datamodel.hsrp.HsrpGroup toHsrpGroup(HsrpGroup hsrpGroup) {
    Ip groupIp = hsrpGroup.getIp();
    return org.batfish.datamodel.hsrp.HsrpGroup.builder()
        .setAuthentication(hsrpGroup.getAuthentication())
        .setHelloTime(hsrpGroup.getHelloTime())
        .setHoldTime(hsrpGroup.getHoldTime())
        .setIps(groupIp == null ? ImmutableSet.of() : ImmutableSet.of(groupIp))
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
      @Nonnull CiscoXrConfiguration oldConfig,
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
      @Nonnull CiscoXrConfiguration oldConfig,
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
      IsakmpProfile isakmpProfile,
      CiscoXrConfiguration oldConfig,
      Configuration config,
      Warnings w) {
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

  static Ip6AccessList toIp6AccessList(Ipv6AccessList eaList) {
    String name = eaList.getName();
    List<Ip6AccessListLine> lines = new ArrayList<>();
    for (Ipv6AccessListLine fromLine : eaList.getLines()) {
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
      List<TcpFlagsMatchConditions> tcpFlags = fromLine.getTcpFlags();
      newLine.getTcpFlags().addAll(tcpFlags);
      Set<Integer> dscps = fromLine.getDscps();
      newLine.getDscps().addAll(dscps);
      lines.add(newLine);
    }
    return new Ip6AccessList(name, lines);
  }

  static IpAccessList toIpAccessList(Ipv4AccessList eaList, Map<String, ObjectGroup> objectGroups) {
    List<AclLine> lines =
        eaList.getLines().stream()
            .map(l -> toExprAclLine(l, objectGroups))
            .collect(ImmutableList.toImmutableList());
    String name = eaList.getName();
    return IpAccessList.builder()
        .setName(name)
        .setLines(lines)
        .setSourceName(name)
        .setSourceType(IPV4_ACCESS_LIST.getDescription())
        .build();
  }

  static IpSpace toIpSpace(NetworkObjectGroup networkObjectGroup) {
    return firstNonNull(AclIpSpace.union(networkObjectGroup.getLines()), EmptyIpSpace.INSTANCE);
  }

  /**
   * Converts a {@link Tunnel} to an {@link IpsecPeerConfig}, or empty optional if it can't be
   * converted
   */
  static Optional<IpsecPeerConfig> toIpsecPeerConfig(
      Tunnel tunnel,
      String tunnelIfaceName,
      CiscoXrConfiguration oldConfig,
      Configuration newConfig,
      Warnings w) {
    Ip localAddress = tunnel.getSourceAddress();
    if (localAddress == null || !localAddress.valid()) {
      w.redFlag(
          String.format(
              "Cannot create IPsec peer on tunnel %s: cannot determine tunnel source address",
              tunnelIfaceName));
      return Optional.empty();
    }

    IpsecStaticPeerConfig.Builder ipsecStaticPeerConfigBuilder =
        IpsecStaticPeerConfig.builder()
            .setTunnelInterface(tunnelIfaceName)
            .setDestinationAddress(tunnel.getDestination())
            .setLocalAddress(localAddress)
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

    return Optional.of(ipsecStaticPeerConfigBuilder.build());
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
      String peerName =
          String.format("~IPSEC_PEER_CONFIG:%s_%s~", cryptoMapNameSeqNumber, iface.getName());
      toIpsecPeerConfig(c, cryptoMapEntry, iface, ipsecPhase2Policy, w)
          .ifPresent(config -> ipsecPeerConfigsBuilder.put(peerName, config));
    }
    return ipsecPeerConfigsBuilder.build();
  }

  private static Optional<IpsecPeerConfig> toIpsecPeerConfig(
      Configuration c,
      CryptoMapEntry cryptoMapEntry,
      org.batfish.datamodel.Interface iface,
      String ipsecPhase2Policy,
      Warnings w) {
    Ip localAddress =
        Optional.ofNullable(iface.getConcreteAddress())
            .map(ConcreteInterfaceAddress::getIp)
            .orElse(null);
    if (localAddress == null || !localAddress.valid()) {
      w.redFlag(
          String.format(
              "Cannot create IPsec peer on interface %s: no valid interface IP", iface.getName()));
      return Optional.empty();
    }

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
        .setLocalAddress(localAddress);

    setIpsecPeerConfigPolicyAccessList(c, cryptoMapEntry, newIpsecPeerConfigBuilder, w);

    return Optional.of(newIpsecPeerConfigBuilder.build());
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
   * Returns a new symmetrical {@link IpAccessList} by adding mirror image {@link ExprAclLine}s to
   * the original {@link IpAccessList} or null if the conversion is not supported
   */
  @VisibleForTesting
  @Nullable
  static IpAccessList createAclWithSymmetricalLines(IpAccessList ipAccessList) {
    List<AclLine> aclLines = new ArrayList<>(ipAccessList.getLines());

    for (AclLine line : ipAccessList.getLines()) {
      // Does not support types of ACL line other than ExprAclLine
      if (!(line instanceof ExprAclLine)) {
        return null;
      }
      ExprAclLine exprAclLine = (ExprAclLine) line;
      HeaderSpace originalHeaderSpace =
          HeaderSpaceConverter.convert(exprAclLine.getMatchCondition());

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
            ExprAclLine.builder()
                .setMatchCondition(
                    new MatchHeaderSpace(
                        reversedHeaderSpaceBuilder
                            .setSrcIps(originalHeaderSpace.getDstIps())
                            .setSrcPorts(originalHeaderSpace.getDstPorts())
                            .setDstIps(originalHeaderSpace.getSrcIps())
                            .setDstPorts(originalHeaderSpace.getSrcPorts())
                            .build()))
                .setAction(exprAclLine.getAction())
                .build());
      }
    }

    return IpAccessList.builder().setName(ipAccessList.getName()).setLines(aclLines).build();
  }

  /**
   * Checks if the given {@link DistributeList} is nonnull and successfully converted, and if so,
   * returns the name of the VI {@link RoutingPolicy} that represents it.
   */
  static @Nullable String getOspfInboundDistributeListPolicy(
      @Nullable DistributeList distributeList,
      String vrfName,
      String procName,
      Configuration c,
      Warnings w) {
    if (distributeList == null) {
      return null;
    }
    String filterName = distributeList.getFilterName();
    if (distributeList.getFilterType() == DistributeListFilterType.ACCESS_LIST) {
      if (c.getRouteFilterLists().containsKey(filterName)) {
        String rpName = generatedOspfInboundDistributeListName(vrfName, procName);
        if (!c.getRoutingPolicies().containsKey(rpName)) {
          RoutingPolicy.builder()
              .setName(rpName)
              .setOwner(c)
              .addStatement(
                  new If(
                      new MatchPrefixSet(
                          DestinationNetwork.instance(), new NamedPrefixSet(filterName)),
                      ImmutableList.of(ExitAccept.toStaticStatement()),
                      ImmutableList.of(ExitReject.toStaticStatement())))
              .build();
        }
        return rpName;
      } else {
        w.redFlag(
            String.format(
                "Ignoring OSPF distribute-list %s: access-list is not defined or failed to convert",
                filterName));
        return null;
      }
    } else {
      assert distributeList.getFilterType() == DistributeListFilterType.ROUTE_POLICY;
      if (c.getRoutingPolicies().containsKey(filterName)) {
        return filterName;
      } else {
        w.redFlag(
            String.format(
                "Ignoring OSPF distribute-list %s: route-policy is not defined or failed to"
                    + " convert",
                filterName));
        return null;
      }
    }
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
      EigrpProcess proc, String vrfName, Configuration c, CiscoXrConfiguration oldConfig) {
    org.batfish.datamodel.eigrp.EigrpProcess.Builder newProcess =
        org.batfish.datamodel.eigrp.EigrpProcess.builder()
            .setMetricVersion(/* TODO: investigate XR metric. */ EigrpMetricVersion.V1);

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
    newProcess.setRedistributionPolicy(eigrpExportPolicyName);

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
   * @param vsConfig Vendor specific {@link CiscoXrConfiguration configuration}
   * @return {@link List} of {@link If} statements
   */
  static List<If> eigrpRedistributionPoliciesToStatements(
      Collection<EigrpRedistributionPolicy> eigrpRedistributionPolicies,
      EigrpProcess vsEigrpProc,
      CiscoXrConfiguration vsConfig) {
    return eigrpRedistributionPolicies.stream()
        .map(policy -> convertEigrpRedistributionPolicy(policy, vsEigrpProc, vsConfig))
        .filter(Objects::nonNull)
        .collect(ImmutableList.toImmutableList());
  }

  @Nullable
  private static If convertEigrpRedistributionPolicy(
      EigrpRedistributionPolicy policy, EigrpProcess proc, CiscoXrConfiguration oldConfig) {
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
      // TODO update to route-policy if valid, or delete grammar and VS
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
      IsisProcess proc, CiscoXrConfiguration oldConfig) {
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

  static Route6FilterList toRoute6FilterList(Prefix6List list) {
    List<Route6FilterLine> lines =
        list.getLines().stream()
            .map(pl -> new Route6FilterLine(pl.getAction(), pl.getPrefix(), pl.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    return new Route6FilterList(list.getName(), lines);
  }

  static RouteFilterList toRouteFilterList(Ipv4AccessList eaList, String vendorConfigFilename) {
    List<RouteFilterLine> lines =
        eaList.getLines().stream()
            .map(CiscoXrConversions::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList());
    return new RouteFilterList(
        eaList.getName(),
        lines,
        new VendorStructureId(
            vendorConfigFilename, IPV4_ACCESS_LIST.getDescription(), eaList.getName()));
  }

  static RouteFilterList toRouteFilterList(PrefixList list, String vendorConfigFilename) {
    List<RouteFilterLine> newLines =
        list.getLines().stream()
            .map(
                l ->
                    new RouteFilterLine(
                        l.getAction(), IpWildcard.create(l.getPrefix()), l.getLengthRange()))
            .collect(ImmutableList.toImmutableList());
    return new RouteFilterList(
        list.getName(),
        newLines,
        new VendorStructureId(vendorConfigFilename, PREFIX_LIST.getDescription(), list.getName()));
  }

  /** Convert the specified ACL into a packet policy, for use in ACL based forwarding. */
  static PacketPolicy toPacketPolicy(
      Ipv4AccessList eaList, Map<String, ObjectGroup> objectGroups, Warnings warnings) {
    return new PacketPolicy(
        computeAbfIpv4PolicyName(eaList.getName()),
        eaList.getLines().stream()
            .filter(l -> canConvertAbfAclLine(l, eaList.getName(), warnings))
            .map(l -> toPacketPolicyStatement(l, objectGroups))
            .collect(ImmutableList.toImmutableList()),
        new Return(new FibLookup(IngressInterfaceVrf.instance())));
  }

  /**
   * Indicates if the specified acl line can be converted into the VI model; adds a warning if not.
   */
  private static boolean canConvertAbfAclLine(
      Ipv4AccessListLine line, String aclName, Warnings warnings) {
    Ipv4Nexthop nexthop1 = line.getNexthop1();
    if (nexthop1 == null) {
      return true;
    }

    String vrf1 = nexthop1.getVrf();
    String vrf2 = line.getNexthop2() == null ? vrf1 : line.getNexthop2().getVrf();
    String vrf3 = line.getNexthop3() == null ? vrf1 : line.getNexthop3().getVrf();
    if (!Objects.equals(vrf1, vrf2) || !Objects.equals(vrf1, vrf3)) {
      warnings.redFlag(
          String.format(
              "Access-list lines with different nexthop VRFs are not yet supported. Line '%s' in"
                  + " ACL %s will be ignored.",
              line.getName(), aclName));
      return false;
    }

    return true;
  }

  /**
   * Convert an {@link Ipv4AccessListLine} into a guarded-action {@link
   * org.batfish.datamodel.packet_policy.Statement}. Must only be called on lines that can be
   * converted according to {@code canConvertAbfAclLine}.
   */
  private static org.batfish.datamodel.packet_policy.Statement toPacketPolicyStatement(
      Ipv4AccessListLine line, Map<String, ObjectGroup> objectGroups) {
    return new org.batfish.datamodel.packet_policy.If(
        new PacketMatchExpr(toAclLineMatchExpr(line, objectGroups)),
        ImmutableList.of(toPacketPolicyActions(line)));
  }

  /**
   * Convert an {@link Ipv4AccessListLine} into a {@link
   * org.batfish.datamodel.packet_policy.Statement} action taken when the line is matched. Must only
   * be called on lines that can be converted according to {@code canConvertAbfAclLine}.
   */
  private static org.batfish.datamodel.packet_policy.Statement toPacketPolicyActions(
      Ipv4AccessListLine line) {
    if (line.getAction() == LineAction.DENY) {
      return new Return(Drop.instance());
    }

    Ipv4Nexthop nexthop1 = line.getNexthop1();

    if (nexthop1 != null) {
      Builder<Ip> ips = ImmutableList.builder();
      ips.add(nexthop1.getIp());
      Optional.ofNullable(line.getNexthop2()).ifPresent(n -> ips.add(n.getIp()));
      Optional.ofNullable(line.getNexthop3()).ifPresent(n -> ips.add(n.getIp()));

      String nexthopVrf = nexthop1.getVrf();
      VrfExpr vrfExpr =
          nexthopVrf == null ? IngressInterfaceVrf.instance() : new LiteralVrfName(nexthopVrf);
      return new Return(
          FibLookupOverrideLookupIp.builder()
              .setIps(ips.build())
              .setVrfExpr(vrfExpr)
              .setDefaultAction(new FibLookup(IngressInterfaceVrf.instance()))
              .setRequireConnected(false)
              .build());
    }

    // Nexthop not overridden
    return new Return(new FibLookup(IngressInterfaceVrf.instance()));
  }

  /**
   * Given a list of {@link If} statements, sets the false statements of every {@link If} to an
   * empty list and adds a rule at the end to allow EIGRP from provided ownAsn.
   */
  static List<Statement> clearFalseStatementsAndAddMatchOwnAsn(
      List<If> redistributeIfs, long ownAsn) {
    List<Statement> emptyFalseStatements = ImmutableList.of();
    List<Statement> redistributeIfsWithEmptyFalse =
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
   * Checks if the {@link DistributeList distributeList} can be converted to a routing policy.
   * Returns false if it refers to an extended access list, which is not supported and also returns
   * false if the access-list referred by it does not exist.
   *
   * <p>Adds appropriate {@link org.batfish.common.Warning} if the {@link DistributeList
   * distributeList} is not found to be valid for conversion to routing policy.
   *
   * @param c Vendor independent {@link Configuration configuration}
   * @param distributeList {@link DistributeList distributeList} to be validated
   * @param vsConfig Vendor specific {@link CiscoXrConfiguration configuration}
   * @return false if the {@link DistributeList distributeList} cannot be converted to a routing
   *     policy
   */
  static boolean sanityCheckEigrpDistributeList(
      @Nonnull Configuration c,
      @Nonnull DistributeList distributeList,
      @Nonnull CiscoXrConfiguration vsConfig) {
    if (distributeList.getFilterType() == DistributeListFilterType.ACCESS_LIST
        && vsConfig.getIpv4Acls().containsKey(distributeList.getFilterName())) {
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
                  "distribute-list refers an undefined access-list `%s`, it will not filter"
                      + " anything",
                  distributeList.getFilterName()));
      return false;
    }
    return true;
  }

  static org.batfish.datamodel.StaticRoute toStaticRoute(StaticRoute staticRoute) {
    String nextHopInterface = staticRoute.getNextHopInterface();
    if (nextHopInterface != null && nextHopInterface.toLowerCase().startsWith("null")) {
      nextHopInterface = org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
    }
    return org.batfish.datamodel.StaticRoute.builder()
        .setNetwork(staticRoute.getPrefix())
        .setNextHop(NextHop.legacyConverter(nextHopInterface, staticRoute.getNextHopIp()))
        .setAdministrativeCost(staticRoute.getDistance())
        .setTag(firstNonNull(staticRoute.getTag(), -1L))
        .build();
  }

  /**
   * Convert specified ACL line into an {@link AclLineMatchExpr} representing its match conditions.
   */
  private static AclLineMatchExpr toAclLineMatchExpr(
      Ipv4AccessListLine line, Map<String, ObjectGroup> objectGroups) {
    IpSpace srcIpSpace = line.getSourceAddressSpecifier().toIpSpace();
    IpSpace dstIpSpace = line.getDestinationAddressSpecifier().toIpSpace();
    AclLineMatchExpr matchService = line.getServiceSpecifier().toAclLineMatchExpr(objectGroups);
    AclLineMatchExpr match;
    if (matchService instanceof MatchHeaderSpace) {
      match =
          new MatchHeaderSpace(
              ((MatchHeaderSpace) matchService)
                  .getHeaderspace().toBuilder()
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
    return match;
  }

  private static ExprAclLine toExprAclLine(
      Ipv4AccessListLine line, Map<String, ObjectGroup> objectGroups) {
    return ExprAclLine.builder()
        .setAction(line.getAction())
        .setMatchCondition(toAclLineMatchExpr(line, objectGroups))
        .setName(line.getName())
        .build();
  }

  private static AsPathAccessListLine toAsPathAccessListLine(AsPathSetElem elem) {
    String regex = CiscoXrConfiguration.toJavaRegex(elem.regex());
    AsPathAccessListLine line = new AsPathAccessListLine(LineAction.PERMIT, regex);
    return line;
  }

  private static RouteFilterLine toRouteFilterLine(Ipv4AccessListLine fromLine) {
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

  /** Helper to convert CiscoXr VS OSPF network type to VI model type. */
  @VisibleForTesting
  @Nullable
  static org.batfish.datamodel.ospf.OspfNetworkType toOspfNetworkType(
      @Nullable OspfNetworkType type, Warnings warnings) {
    if (type == null) {
      // default is broadcast for all Ethernet interfaces
      // (https://learningnetwork.cisco_xr.com/thread/66827)
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
                "Conversion of CiscoXr OSPF network type '%s' is not handled.", type.toString()));
        return null;
    }
  }

  /**
   * Convert VRF leaking configs, if needed. Must be called after VRF address family inheritance is
   * completed, and routing policies have been converted.
   */
  public static void convertVrfLeakingConfig(Collection<Vrf> vrfs, Configuration c) {
    List<Vrf> vrfsWithIpv4Af =
        vrfs.stream()
            .filter(v -> v.getIpv4UnicastAddressFamily() != null)
            .collect(ImmutableList.toImmutableList());
    List<Vrf> vrfsWithImportRt =
        vrfsWithIpv4Af.stream()
            .filter(v -> !v.getIpv4UnicastAddressFamily().getRouteTargetImport().isEmpty())
            .collect(ImmutableList.toImmutableList());
    List<Vrf> vrfsWithoutExportPolicy =
        vrfsWithIpv4Af.stream()
            .filter(v -> v.getIpv4UnicastAddressFamily().getExportPolicy() == null)
            .collect(ImmutableList.toImmutableList());
    List<Vrf> vrfsWithExportPolicy =
        vrfsWithIpv4Af.stream()
            .filter(v -> v.getIpv4UnicastAddressFamily().getExportPolicy() != null)
            .collect(ImmutableList.toImmutableList());
    Multimap<ExtendedCommunity, Vrf> vrfsByExportRt = HashMultimap.create();
    // pre-compute RT to VRF name mapping
    for (Vrf vrf : vrfsWithoutExportPolicy) {
      assert vrf.getIpv4UnicastAddressFamily() != null;
      vrf.getIpv4UnicastAddressFamily()
          .getRouteTargetExport()
          .forEach(rt -> vrfsByExportRt.put(rt, vrf));
    }

    // Create non-default VRF <-> non-default VRF leaking configs for each VRF with import RT
    for (Vrf importingVrf : vrfsWithImportRt) {
      assert !importingVrf.getName().equals(Configuration.DEFAULT_VRF_NAME);
      VrfAddressFamily ipv4uaf = importingVrf.getIpv4UnicastAddressFamily();
      assert ipv4uaf != null;
      org.batfish.datamodel.Vrf viVrf = c.getVrfs().get(importingVrf.getName());
      assert viVrf != null;
      ipv4uaf.getRouteTargetImport().stream()
          .flatMap(importRt -> vrfsByExportRt.get(importRt).stream())
          .distinct()
          .forEach(
              exportingVrf -> {
                // Add leak config for every exporting vrf with no export policy whose export
                // route-target matches this vrf's import route-target.
                // Take care to prevent self-loops.
                if (importingVrf == exportingVrf) {
                  return;
                }
                viVrf.addVrfLeakingConfig(
                    VrfLeakingConfig.builder()
                        .setBgpLeakConfig(
                            bgpLeakConfig(
                                exportingVrf.getIpv4UnicastAddressFamily().getRouteTargetExport()))
                        .setImportFromVrf(exportingVrf.getName())
                        .setImportPolicy(routePolicyOrDrop(ipv4uaf.getImportPolicy(), c))
                        .build());
              });
      // Add leak config for every exporting vrf with an export route-policy, since the policy can
      // potentially alter the route-target to match the import route-target.
      for (Vrf policyExportingVrf : vrfsWithExportPolicy) {
        if (importingVrf == policyExportingVrf) {
          // Take care to prevent self-loops
          continue;
        }
        viVrf.addVrfLeakingConfig(
            VrfLeakingConfig.builder()
                .setBgpLeakConfig(bgpLeakConfig()) // RT handled by policy
                .setImportFromVrf(policyExportingVrf.getName())
                .setImportPolicy(
                    vrfExportImportPolicy(
                        policyExportingVrf.getName(),
                        routePolicyOrDrop(
                            policyExportingVrf.getIpv4UnicastAddressFamily().getExportPolicy(), c),
                        policyExportingVrf.getIpv4UnicastAddressFamily().getRouteTargetExport(),
                        importingVrf.getName(),
                        routePolicyOrDrop(ipv4uaf.getImportPolicy(), c),
                        ipv4uaf.getRouteTargetImport(),
                        c))
                .build());
      }
    }
    org.batfish.datamodel.Vrf viDefaultVrf = c.getVrfs().get(Configuration.DEFAULT_VRF_NAME);
    for (Vrf nonDefaultVrf : vrfsWithIpv4Af) {
      org.batfish.datamodel.Vrf viNonDefaultVrf = c.getVrfs().get(nonDefaultVrf.getName());
      VrfAddressFamily af = nonDefaultVrf.getIpv4UnicastAddressFamily();
      if (af.getExportToDefaultVrfPolicy() != null) {
        viDefaultVrf.addVrfLeakingConfig(
            VrfLeakingConfig.builder()
                .setBgpLeakConfig(bgpLeakConfig()) // RT handled by policy
                .setImportFromVrf(nonDefaultVrf.getName())
                .setImportPolicy(
                    vrfExportImportPolicy(
                        nonDefaultVrf.getName(),
                        routePolicyOrDrop(
                            nonDefaultVrf
                                .getIpv4UnicastAddressFamily()
                                .getExportToDefaultVrfPolicy(),
                            c),
                        nonDefaultVrf.getIpv4UnicastAddressFamily().getRouteTargetExport(),
                        Configuration.DEFAULT_VRF_NAME,
                        null,
                        null,
                        c))
                .build());
      }
      if (af.getImportFromDefaultVrfPolicy() != null) {
        viNonDefaultVrf.addVrfLeakingConfig(
            VrfLeakingConfig.builder()
                .setBgpLeakConfig(bgpLeakConfig()) // RT handled by policy
                .setImportFromVrf(Configuration.DEFAULT_VRF_NAME)
                .setImportPolicy(
                    vrfExportImportPolicy(
                        Configuration.DEFAULT_VRF_NAME,
                        routePolicyOrDrop(
                            nonDefaultVrf
                                .getIpv4UnicastAddressFamily()
                                .getImportFromDefaultVrfPolicy(),
                            c),
                        nonDefaultVrf
                            .getIpv4UnicastAddressFamily()
                            .getRouteTargetImport(), // auto-apply import route-target on pass
                        nonDefaultVrf.getName(),
                        null,
                        null,
                        c))
                .build());
      }
    }
  }

  @Nonnull
  private static BgpLeakConfig bgpLeakConfig(ExtendedCommunity... attachRouteTargets) {
    return bgpLeakConfig(Arrays.asList(attachRouteTargets));
  }

  @Nonnull
  private static BgpLeakConfig bgpLeakConfig(Iterable<ExtendedCommunity> attachRouteTargets) {
    return BgpLeakConfig.builder()
        // TODO: input and honor result of 'bgp distance' command argument 3 (local BGP admin)
        .setAdmin(
            RoutingProtocol.BGP.getDefaultAdministrativeCost(ConfigurationFormat.CISCO_IOS_XR))
        .setAttachRouteTargets(attachRouteTargets)
        .setWeight(BGP_VRF_LEAK_IGP_WEIGHT)
        .build();
  }

  @VisibleForTesting public static final int BGP_VRF_LEAK_IGP_WEIGHT = 0;

  /**
   * Create a policy for exporting from one VRF to another in the presence of an export policy. <br>
   * Preconditions:
   *
   * <ul>
   *   <li>{@code routeTargetImport} is {@code null} iff {@code exportingVrf} or {@code
   *       importingVrf} is the default VRF.
   *   <li>{@code routeTargetImport} must be {@code null} or non-empty.
   * </ul>
   *
   * @throws IllegalArgumentException when a precondition is violated.
   */
  private static @Nonnull String vrfExportImportPolicy(
      String exportingVrf,
      String exportPolicy,
      Set<ExtendedCommunity> routeTargetExport,
      String importingVrf,
      @Nullable String importPolicy,
      @Nullable Set<ExtendedCommunity> routeTargetImport,
      Configuration c) {
    // Implementation overview:
    // 1. (Re)write the export route-target to intermediate BGP properties so that they can be read
    //    later. Input communities from the original route are copied.
    // 2. Apply the export route-policy. At the beginning of export policy evalution, the input
    //    communities are the union of the original route's communities and the export RTs.
    // 3. If import route-target is non-null, drop the route if does not have a route-target
    //    matching the importing VRF's import route-target communities.
    // 4. Apply the import route-policy if it exists. This route-policy may permit with or without
    //    further modification, or may reject the route.
    boolean defaultVrfInvolved =
        importingVrf.equals(Configuration.DEFAULT_VRF_NAME)
            || exportingVrf.equals(Configuration.DEFAULT_VRF_NAME);
    checkArgument(
        (routeTargetImport != null && !defaultVrfInvolved)
            || (routeTargetImport == null && defaultVrfInvolved),
        "importPolicy must be null iff importing from or exporting to the default VRF");
    checkArgument(
        routeTargetImport == null || !routeTargetImport.isEmpty(),
        "When leaking between non-default VRFs, the importing VRF must have at least one import"
            + " RT");
    String policyName = computeVrfExportImportPolicyName(exportingVrf, importingVrf);
    if (c.getRoutingPolicies().containsKey(policyName)) {
      return policyName;
    }
    Statement addExportRt =
        new SetCommunities(
            CommunitySetUnion.of(
                InputCommunities.instance(),
                new LiteralCommunitySet(CommunitySet.of(routeTargetExport))));
    Statement applyExportPolicy =
        new If(
            new CallExpr(exportPolicy),
            ImmutableList.of(),
            ImmutableList.of(Statements.ReturnFalse.toStaticStatement()));
    Statement applyImportMap =
        importPolicy != null
            ? new If(
                new CallExpr(importPolicy),
                ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))
            : Statements.ReturnTrue.toStaticStatement();
    RoutingPolicy.Builder builder =
        RoutingPolicy.builder()
            .setName(policyName)
            .addStatement(Statements.SetWriteIntermediateBgpAttributes.toStaticStatement())
            .addStatement(addExportRt)
            .addStatement(Statements.SetReadIntermediateBgpAttributes.toStaticStatement())
            .addStatement(applyExportPolicy);
    if (routeTargetImport != null) {
      Statement filterImportRt =
          new If(
              new MatchCommunities(
                  InputCommunities.instance(),
                  new CommunitySetMatchAny(
                      routeTargetImport.stream()
                          .map(CommunityIs::new)
                          .map(HasCommunity::new)
                          .collect(ImmutableList.toImmutableList()))),
              ImmutableList.of(),
              ImmutableList.of(Statements.ReturnFalse.toStaticStatement()));
      builder.addStatement(filterImportRt);
    }
    builder.addStatement(applyImportMap).setOwner(c).build();
    return policyName;
  }

  @VisibleForTesting
  public static @Nonnull String computeVrfExportImportPolicyName(
      String exportingVrf, String importingVrf) {
    return String.format("~vrfExportImport~%s~%s", exportingVrf, importingVrf);
  }

  static @Nonnull String computeDedupedAsPathMatchExprName(String name) {
    return String.format("~DEDUPED~%s", name);
  }

  static @Nonnull String computeOriginalAsPathMatchExprName(String name) {
    return String.format("~ORIGINAL~%s", name);
  }

  static @Nonnull AsPathMatchExpr toAsPathMatchExpr(AsPathSet asPathSet, boolean usingDeduped) {
    return AsPathMatchAny.of(
        asPathSet.getElements().stream()
            .map(elem -> elem.accept(AS_PATH_SET_ELEM_TO_AS_PATH_MATCH_EXPR, usingDeduped))
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList()));
  }

  // For any given concrete AsPathSetElem, the corresponding visit method should return a non-null
  // value for exactly one value of usingDeduped.
  private static final class AsPathSetElemToAsPathMatchExpr
      implements AsPathSetElemVisitor<AsPathMatchExpr, Boolean> {
    @Override
    public AsPathMatchExpr visitDfaRegexAsPathSetElem(
        DfaRegexAsPathSetElem dfaRegexAsPathSetElem, Boolean usingDeduped) {
      if (usingDeduped) {
        return null;
      } else {
        // TODO: figure out how this is different from ios-regex
        return AsPathMatchRegex.of(toJavaRegex(dfaRegexAsPathSetElem.getRegex()));
      }
    }

    @Override
    public AsPathMatchExpr visitIosRegexAsPathSetElem(
        IosRegexAsPathSetElem iosRegexAsPathSetElem, Boolean usingDeduped) {
      if (usingDeduped) {
        return null;
      } else {
        return AsPathMatchRegex.of(toJavaRegex(iosRegexAsPathSetElem.getRegex()));
      }
    }

    @Override
    public AsPathMatchExpr visitLengthAsPathSetElem(
        LengthAsPathSetElem lengthAsPathSetElem, Boolean usingDeduped) {
      if (usingDeduped) {
        return null;
      } else {
        // TODO: something with getAll()?
        return HasAsPathLength.of(
            new IntComparison(
                lengthAsPathSetElem.getComparator(),
                new LiteralInt(lengthAsPathSetElem.getLength())));
      }
    }

    @Override
    public AsPathMatchExpr visitNeighborIsAsPathSetElem(
        NeighborIsAsPathSetElem neighborIsAsPathSetElem, Boolean usingDeduped) {
      if (usingDeduped ^ !neighborIsAsPathSetElem.getExact()) {
        // exact means not deduped
        return null;
      }
      return AsSetsMatchingRanges.of(false, true, neighborIsAsPathSetElem.getAsRanges());
    }

    @Override
    public AsPathMatchExpr visitOriginatesFromAsPathSetElem(
        OriginatesFromAsPathSetElem originatesFromAsPathSetElem, Boolean usingDeduped) {
      if (usingDeduped ^ !originatesFromAsPathSetElem.getExact()) {
        // exact means not deduped
        return null;
      }
      return AsSetsMatchingRanges.of(true, false, originatesFromAsPathSetElem.getAsRanges());
    }

    @Override
    public AsPathMatchExpr visitPassesThroughAsPathSetElem(
        PassesThroughAsPathSetElem passesThroughAsPathSetElem, Boolean usingDeduped) {
      if (usingDeduped ^ !passesThroughAsPathSetElem.getExact()) {
        // exact means not deduped
        return null;
      }
      return AsSetsMatchingRanges.of(false, false, passesThroughAsPathSetElem.getAsRanges());
    }

    @Override
    public AsPathMatchExpr visitUniqueLengthAsPathSetElem(
        UniqueLengthAsPathSetElem uniqueLengthAsPathSetElem, Boolean usingDeduped) {
      if (!usingDeduped) {
        return null;
      }
      // TODO: something with getAll()?
      return HasAsPathLength.of(
          new IntComparison(
              uniqueLengthAsPathSetElem.getComparator(),
              new LiteralInt(uniqueLengthAsPathSetElem.getLength())));
    }
  }

  static final AsPathSetElemToAsPathMatchExpr AS_PATH_SET_ELEM_TO_AS_PATH_MATCH_EXPR =
      new AsPathSetElemToAsPathMatchExpr();

  /**
   * Implements best-effort behavior for undefined route-policy.
   *
   * <p>Always returns {@code null} when given a null {@code policyName}, and non-null otherwise.
   */
  private static @Nullable String routePolicyOrDrop(@Nullable String policyName, Configuration c) {
    if (policyName == null || c.getRoutingPolicies().containsKey(policyName)) {
      return policyName;
    }
    String undefinedName = policyName + "~undefined";
    if (!c.getRoutingPolicies().containsKey(undefinedName)) {
      // For undefined route-policy, generate a RoutingPolicy that denies everything.
      RoutingPolicy.builder()
          .setName(undefinedName)
          .addStatement(ROUTE_POLICY_DENY_STATEMENT)
          .setOwner(c)
          .build();
    }
    return undefinedName;
  }

  private static final Statement ROUTE_POLICY_DENY_STATEMENT =
      new If(
          BooleanExprs.CALL_EXPR_CONTEXT,
          ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
          ImmutableList.of(Statements.ExitReject.toStaticStatement()));

  private CiscoXrConversions() {} // prevent instantiation of utility class
}
