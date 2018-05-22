package org.batfish.representation.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.HeaderSpace;
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

/** Utilities that convert Cisco-specific representations to vendor-independent model. */
@ParametersAreNonnullByDefault
class CiscoConversions {

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
        ImmutableList.of(Statements.ReturnFalse.toStaticStatement()),
        ImmutableList.of());
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

  static org.batfish.datamodel.IsisProcess toIsisProcess(
      IsisProcess proc, Configuration c, CiscoConfiguration oldConfig) {
    org.batfish.datamodel.IsisProcess newProcess = new org.batfish.datamodel.IsisProcess();

    newProcess.setNetAddress(proc.getNetAddress());
    newProcess.setLevel(proc.getLevel());

    return newProcess;
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
            .map(l -> new RouteFilterLine(l.getAction(), l.getPrefix(), l.getLengthRange()))
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
                  .rebuild()
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
    return new RouteFilterLine(action, prefix, new SubRange(minPrefixLength, maxPrefixLength));
  }

  private CiscoConversions() {} // prevent instantiation of utility class
}
