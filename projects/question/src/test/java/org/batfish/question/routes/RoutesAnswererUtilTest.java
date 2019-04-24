package org.batfish.question.routes;

import static org.batfish.datamodel.Prefix.MAX_PREFIX_LENGTH;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.routes.RoutesAnswerer.COL_ADMIN_DISTANCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_AS_PATH;
import static org.batfish.question.routes.RoutesAnswerer.COL_COMMUNITIES;
import static org.batfish.question.routes.RoutesAnswerer.COL_METRIC;
import static org.batfish.question.routes.RoutesAnswerer.COL_NETWORK;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP_IP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NODE;
import static org.batfish.question.routes.RoutesAnswerer.COL_ROUTE_ENTRY_PRESENCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_VRF_NAME;
import static org.batfish.question.routes.RoutesAnswererUtil.alignRouteRowAttributes;
import static org.batfish.question.routes.RoutesAnswererUtil.computeNextHopNode;
import static org.batfish.question.routes.RoutesAnswererUtil.getAbstractRouteRowsDiff;
import static org.batfish.question.routes.RoutesAnswererUtil.getBgpRibRoutes;
import static org.batfish.question.routes.RoutesAnswererUtil.getBgpRouteRowsDiff;
import static org.batfish.question.routes.RoutesAnswererUtil.getMainRibRoutes;
import static org.batfish.question.routes.RoutesAnswererUtil.getRoutesDiff;
import static org.batfish.question.routes.RoutesAnswererUtil.groupBgpRoutes;
import static org.batfish.question.routes.RoutesAnswererUtil.groupRoutes;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpRoute.Builder;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.Row;
import org.batfish.question.routes.DiffRoutesOutput.KeyPresenceStatus;
import org.batfish.question.routes.RoutesAnswererTest.MockRib;
import org.batfish.question.routes.RoutesAnswererUtil.RouteEntryPresenceStatus;
import org.batfish.question.routes.RoutesQuestion.RibProtocol;
import org.batfish.specifier.RoutingProtocolSpecifier;
import org.junit.Test;

/** Tests for {@link RoutesAnswererUtil} */
public class RoutesAnswererUtilTest {

  @Test
  public void testAlignRtRowAttrs() {
    RouteRowAttribute rra1 = RouteRowAttribute.builder().setNextHop("node1").build();
    RouteRowAttribute rra3 = RouteRowAttribute.builder().setNextHop("node3").build();
    RouteRowAttribute rra5 = RouteRowAttribute.builder().setNextHop("node5").build();

    RouteRowAttribute rra2 = RouteRowAttribute.builder().setNextHop("node2").build();
    RouteRowAttribute rra4 = RouteRowAttribute.builder().setNextHop("node4").build();

    List<List<RouteRowAttribute>> alignedRouteRowattrs =
        alignRouteRowAttributes(
            ImmutableList.of(rra1, rra3, rra5), ImmutableList.of(rra2, rra4, rra5));

    // expected result after merging the two lists
    List<List<RouteRowAttribute>> expectedOutput =
        ImmutableList.of(
            Lists.newArrayList(rra1, null),
            Lists.newArrayList(null, rra2),
            Lists.newArrayList(rra3, null),
            Lists.newArrayList(null, rra4),
            Lists.newArrayList(rra5, rra5));

    assertThat(alignedRouteRowattrs, equalTo(expectedOutput));
  }

  @Test
  public void testAlignRtRowAttrsTrailingNulls1() {
    RouteRowAttribute rra1 = RouteRowAttribute.builder().setNextHop("node1").build();
    RouteRowAttribute rra2 = RouteRowAttribute.builder().setNextHop("node2").build();
    RouteRowAttribute rra3 = RouteRowAttribute.builder().setNextHop("node3").build();

    List<List<RouteRowAttribute>> alignedRouteRowattrs =
        alignRouteRowAttributes(ImmutableList.of(rra1, rra2, rra3), ImmutableList.of(rra1));

    // null trails for non-matching attributes
    List<List<RouteRowAttribute>> expectedOutput =
        ImmutableList.of(
            Lists.newArrayList(rra1, rra1),
            Lists.newArrayList(rra2, null),
            Lists.newArrayList(rra3, null));

    assertThat(alignedRouteRowattrs, equalTo(expectedOutput));
  }

  @Test
  public void testAlignRtRowAttrsTrailingNulls2() {
    RouteRowAttribute rra1 = RouteRowAttribute.builder().setNextHop("node1").build();
    RouteRowAttribute rra2 = RouteRowAttribute.builder().setNextHop("node2").build();
    RouteRowAttribute rra3 = RouteRowAttribute.builder().setNextHop("node3").build();

    List<List<RouteRowAttribute>> alignedRouteRowattrs =
        alignRouteRowAttributes(ImmutableList.of(rra1), ImmutableList.of(rra1, rra2, rra3));

    // null trails for non-matching attributes
    List<List<RouteRowAttribute>> expectedOutput =
        ImmutableList.of(
            Lists.newArrayList(rra1, rra1),
            Lists.newArrayList(null, rra2),
            Lists.newArrayList(null, rra3));

    assertThat(alignedRouteRowattrs, equalTo(expectedOutput));
  }

  @Test
  public void testAlignRtRowAttrsLeadingNulls() {
    RouteRowAttribute rra1 = RouteRowAttribute.builder().setNextHop("node1").build();
    RouteRowAttribute rra2 = RouteRowAttribute.builder().setNextHop("node2").build();
    RouteRowAttribute rra3 = RouteRowAttribute.builder().setNextHop("node3").build();

    List<List<RouteRowAttribute>> alignedRouteRowattrs =
        alignRouteRowAttributes(ImmutableList.of(rra3), ImmutableList.of(rra1, rra2, rra3));

    // trailing nulls for non-matching attributes
    List<List<RouteRowAttribute>> expectedOutput =
        ImmutableList.of(
            Lists.newArrayList(null, rra1),
            Lists.newArrayList(null, rra2),
            Lists.newArrayList(rra3, rra3));

    assertThat(alignedRouteRowattrs, equalTo(expectedOutput));
  }

  @Test
  public void testMainRibColumnsValue() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        OspfExternalType2Route.builder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopIp(Ip.parse("1.1.1.2"))
                            .setAdmin(10)
                            .setMetric(30L)
                            .setLsaMetric(2)
                            .setCostToAdvertiser(2)
                            .setArea(1L)
                            .setAdvertiser("n2")
                            .setOspfMetricType(OspfMetricType.E2)
                            .build()))));

    Multiset<Row> actual =
        getMainRibRoutes(
            ribs,
            ImmutableSet.of("n1"),
            null,
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            ".*",
            null);

    assertThat(actual, hasSize(1));
    Row row = actual.iterator().next();
    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("n1")), Schema.NODE),
            hasColumn(COL_VRF_NAME, equalTo(Configuration.DEFAULT_VRF_NAME), Schema.STRING),
            hasColumn(COL_NETWORK, equalTo(Prefix.parse("1.1.1.0/24")), Schema.PREFIX),
            hasColumn(COL_NEXT_HOP_IP, equalTo(Ip.parse("1.1.1.2")), Schema.IP),
            hasColumn(COL_ADMIN_DISTANCE, equalTo(10), Schema.INTEGER),
            hasColumn(COL_METRIC, equalTo(30L), Schema.LONG)));
  }

  @Test
  public void testBgpRibRouteColumnsValue() {
    Ip ip = Ip.parse("1.1.1.1");
    Table<String, String, Set<BgpRoute>> bgpRouteTable = HashBasedTable.create();
    bgpRouteTable.put(
        "node",
        "vrf",
        ImmutableSet.of(
            new Builder()
                .setNetwork(Prefix.create(ip, MAX_PREFIX_LENGTH))
                .setOriginType(OriginType.IGP)
                .setNextHopIp(ip)
                .setCommunities(ImmutableSortedSet.of(StandardCommunity.of(65537L)))
                .setProtocol(RoutingProtocol.BGP)
                .setOriginatorIp(Ip.parse("1.1.1.2"))
                .setAsPath(AsPath.ofSingletonAsSets(ImmutableList.of(1L, 2L)))
                .setTag(1)
                .build()));
    Multiset<Row> rows =
        getBgpRibRoutes(
            bgpRouteTable,
            RibProtocol.BGP,
            ImmutableSet.of("node"),
            null,
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            ".*");

    assertThat(rows, hasSize(1));

    Row row = rows.iterator().next();

    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("node")), Schema.NODE),
            hasColumn(COL_VRF_NAME, equalTo("vrf"), Schema.STRING),
            hasColumn(COL_NETWORK, equalTo(Prefix.create(ip, MAX_PREFIX_LENGTH)), Schema.PREFIX),
            hasColumn(COL_NEXT_HOP_IP, equalTo(ip), Schema.IP)));

    assertThat(
        row,
        allOf(
            hasColumn(
                COL_COMMUNITIES, equalTo(ImmutableList.of("1:1")), Schema.list(Schema.STRING)),
            hasColumn(COL_AS_PATH, equalTo("1 2"), Schema.STRING)));
  }

  @Test
  public void testGetBgpRoutesCommunities() {
    Ip ip = Ip.parse("1.1.1.1");
    Table<String, String, Set<BgpRoute>> bgpRouteTable = HashBasedTable.create();
    bgpRouteTable.put(
        "node",
        "vrf",
        ImmutableSet.of(
            new Builder()
                .setNetwork(Prefix.create(ip, MAX_PREFIX_LENGTH))
                .setOriginType(OriginType.IGP)
                .setOriginatorIp(ip)
                .setReceivedFromIp(ip)
                .setCommunities(ImmutableSortedSet.of(StandardCommunity.of(65537L)))
                .setProtocol(RoutingProtocol.BGP)
                .build()));
    Multiset<Row> rows =
        getBgpRibRoutes(
            bgpRouteTable,
            RibProtocol.BGP,
            ImmutableSet.of("node"),
            null,
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            ".*");
    assertThat(
        rows.iterator().next().get(COL_COMMUNITIES, Schema.list(Schema.STRING)),
        equalTo(ImmutableList.of("1:1")));
  }

  @Test
  public void testComputeNextHopNode() {
    assertThat(computeNextHopNode(null, ImmutableMap.of()), nullValue());
    assertThat(computeNextHopNode(Ip.parse("1.1.1.1"), null), nullValue());
    assertThat(computeNextHopNode(Ip.parse("1.1.1.1"), ImmutableMap.of()), nullValue());
    assertThat(
        computeNextHopNode(
            Ip.parse("1.1.1.1"), ImmutableMap.of(Ip.parse("1.1.1.1"), ImmutableSet.of("n1"))),
        equalTo("n1"));
    assertThat(
        computeNextHopNode(
            Ip.parse("1.1.1.1"), ImmutableMap.of(Ip.parse("1.1.1.2"), ImmutableSet.of("n1"))),
        nullValue());
  }

  @Test
  public void testBgpRoutesRowDiff() {
    RouteRowAttribute.Builder routeRowAttrBuilder =
        RouteRowAttribute.builder()
            .setAdminDistance(200)
            .setMetric(2L)
            .setOriginProtocol("bgp")
            .setAsPath(AsPath.ofSingletonAsSets(ImmutableList.of(1L, 2L)))
            .setLocalPreference(1L);
    List<List<RouteRowAttribute>> diffMatrix = new ArrayList<>();
    diffMatrix.add(Lists.newArrayList(routeRowAttrBuilder.build(), routeRowAttrBuilder.build()));

    List<List<RouteRowAttribute>> diffMatrixChanged = new ArrayList<>();
    diffMatrix.add(
        Lists.newArrayList(routeRowAttrBuilder.build(), routeRowAttrBuilder.setMetric(1L).build()));

    List<List<RouteRowAttribute>> diffMatrixMissingRefs = new ArrayList<>();
    diffMatrixMissingRefs.add(Lists.newArrayList(routeRowAttrBuilder.build(), null));

    List<List<RouteRowAttribute>> diffMatrixMissingBase = new ArrayList<>();
    diffMatrixMissingBase.add(Lists.newArrayList(null, routeRowAttrBuilder.build()));

    List<DiffRoutesOutput> diff =
        ImmutableList.of(
            new DiffRoutesOutput(
                new RouteRowKey("node", "vrf", Prefix.parse("1.1.1.1/24")),
                new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
                KeyPresenceStatus.IN_BOTH,
                diffMatrix,
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                new RouteRowKey("node", "vrf", Prefix.parse("1.1.1.1/24")),
                new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
                KeyPresenceStatus.IN_BOTH,
                diffMatrixChanged,
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                new RouteRowKey("node", "vrf", Prefix.parse("1.1.1.1/24")),
                new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
                KeyPresenceStatus.ONLY_IN_SNAPSHOT,
                diffMatrixMissingRefs,
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                new RouteRowKey("node", "vrf", Prefix.parse("1.1.1.1/24")),
                new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
                KeyPresenceStatus.ONLY_IN_REFERENCE,
                diffMatrixMissingBase,
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                new RouteRowKey("node", "vrf", Prefix.parse("1.1.1.1/24")),
                new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
                KeyPresenceStatus.IN_BOTH,
                diffMatrixMissingRefs,
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                new RouteRowKey("node", "vrf", Prefix.parse("1.1.1.1/24")),
                new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
                KeyPresenceStatus.IN_BOTH,
                diffMatrixMissingBase,
                KeyPresenceStatus.IN_BOTH));
    Multiset<Row> rows = getBgpRouteRowsDiff(diff, RibProtocol.BGP);

    assertThat(
        rows,
        containsInAnyOrder(
            ImmutableList.of(
                hasColumn(
                    COL_ROUTE_ENTRY_PRESENCE,
                    equalTo(RouteEntryPresenceStatus.UNCHANGED.routeEntryPresenceName()),
                    Schema.STRING),
                hasColumn(
                    COL_ROUTE_ENTRY_PRESENCE,
                    equalTo(RouteEntryPresenceStatus.CHANGED.routeEntryPresenceName()),
                    Schema.STRING),
                hasColumn(
                    COL_ROUTE_ENTRY_PRESENCE,
                    equalTo(RouteEntryPresenceStatus.ONLY_IN_SNAPSHOT.routeEntryPresenceName()),
                    Schema.STRING),
                hasColumn(
                    COL_ROUTE_ENTRY_PRESENCE,
                    equalTo(RouteEntryPresenceStatus.ONLY_IN_REFERENCE.routeEntryPresenceName()),
                    Schema.STRING),
                hasColumn(
                    COL_ROUTE_ENTRY_PRESENCE,
                    equalTo(RouteEntryPresenceStatus.ONLY_IN_SNAPSHOT.routeEntryPresenceName()),
                    Schema.STRING),
                hasColumn(
                    COL_ROUTE_ENTRY_PRESENCE,
                    equalTo(RouteEntryPresenceStatus.ONLY_IN_REFERENCE.routeEntryPresenceName()),
                    Schema.STRING))));
  }

  @Test
  public void testGroupMatchingRoutesByPrefix() {
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            "n1",
            ImmutableSortedMap.of(
                Configuration.DEFAULT_VRF_NAME,
                new MockRib<>(
                    ImmutableSet.of(
                        OspfExternalType2Route.builder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopIp(Ip.parse("1.1.1.2"))
                            .setAdmin(10)
                            .setMetric(30L)
                            .setLsaMetric(2)
                            .setCostToAdvertiser(2)
                            .setArea(1L)
                            .setAdvertiser("n2")
                            .setOspfMetricType(OspfMetricType.E2)
                            .build(),
                        OspfExternalType2Route.builder()
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .setNextHopIp(Ip.parse("1.1.1.3"))
                            .setAdmin(10)
                            .setMetric(20L)
                            .setLsaMetric(2)
                            .setCostToAdvertiser(2)
                            .setArea(1L)
                            .setAdvertiser("n2")
                            .setOspfMetricType(OspfMetricType.E2)
                            .build()))));

    Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> grouped =
        groupRoutes(
            ribs,
            ImmutableSet.of("n1"),
            null,
            ".*",
            RoutingProtocolSpecifier.ALL_PROTOCOLS_SPECIFIER,
            null);

    assertThat(grouped.keySet(), hasSize(1));
    RouteRowKey expectedKey =
        new RouteRowKey("n1", Configuration.DEFAULT_VRF_NAME, Prefix.parse("1.1.1.0/24"));
    assertThat(grouped.keySet().iterator().next(), equalTo(expectedKey));

    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroup = grouped.get(expectedKey);

    // checking equality of inner group
    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> expectedInnerMap =
        ImmutableMap.of(
            new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "ospfE2"),
            ImmutableSortedSet.of(
                RouteRowAttribute.builder().setAdminDistance(10).setMetric(30L).build()),
            new RouteRowSecondaryKey(Ip.parse("1.1.1.3"), "ospfE2"),
            ImmutableSortedSet.of(
                RouteRowAttribute.builder().setAdminDistance(10).setMetric(20L).build()));
    // matching the secondary key
    assertThat(innerGroup, equalTo(expectedInnerMap));
  }

  @Test
  public void testGroupMatchingBgpRoutesByPrefix() {
    BgpRoute bgpRoute1 =
        BgpRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setNextHopIp(Ip.parse("1.1.1.2"))
            .setOriginType(OriginType.IGP)
            .setOriginatorIp(Ip.parse("1.1.1.2"))
            .setProtocol(RoutingProtocol.BGP)
            .setLocalPreference(1L)
            .setAdmin(10)
            .setMetric(30L)
            .setAsPath(AsPath.ofSingletonAsSets(ImmutableList.of(1L, 2L)))
            .build();

    BgpRoute bgpRoute2 =
        BgpRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setNextHopIp(Ip.parse("1.1.1.3"))
            .setAdmin(10)
            .setMetric(20L)
            .setOriginType(OriginType.IGP)
            .setOriginatorIp(Ip.parse("1.1.1.2"))
            .setProtocol(RoutingProtocol.BGP)
            .setLocalPreference(1L)
            .setAsPath(AsPath.ofSingletonAsSets(ImmutableList.of(1L, 2L)))
            .build();

    Table<String, String, Set<BgpRoute>> bgpTable = HashBasedTable.create();
    bgpTable.row("node").put(Configuration.DEFAULT_VRF_NAME, new HashSet<>());
    bgpTable.row("node").get(Configuration.DEFAULT_VRF_NAME).add(bgpRoute1);
    bgpTable.row("node").get(Configuration.DEFAULT_VRF_NAME).add(bgpRoute2);

    Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> grouped =
        groupBgpRoutes(bgpTable, ImmutableSet.of("node"), ".*", null, ".*");

    assertThat(grouped.keySet(), hasSize(1));

    RouteRowKey expectedKey =
        new RouteRowKey("node", Configuration.DEFAULT_VRF_NAME, Prefix.parse("1.1.1.0/24"));
    assertThat(grouped.keySet().iterator().next(), equalTo(expectedKey));

    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroup = grouped.get(expectedKey);

    // checking equality of inner group
    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> expectedInnerMap =
        ImmutableMap.of(
            new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
            ImmutableSortedSet.of(
                RouteRowAttribute.builder()
                    .setAdminDistance(10)
                    .setMetric(30L)
                    .setAsPath(AsPath.ofSingletonAsSets(ImmutableList.of(1L, 2L)))
                    .setLocalPreference(1L)
                    .build()),
            new RouteRowSecondaryKey(Ip.parse("1.1.1.3"), "bgp"),
            ImmutableSortedSet.of(
                RouteRowAttribute.builder()
                    .setAdminDistance(10)
                    .setMetric(20L)
                    .setLocalPreference(1L)
                    .setAsPath(AsPath.ofSingletonAsSets(ImmutableList.of(1L, 2L)))
                    .build()));
    // matching the secondary key
    assertThat(innerGroup, equalTo(expectedInnerMap));
  }

  @Test
  public void testGetRoutesDiffCommonKey() {
    RouteRowKey routeRowKey = new RouteRowKey("node", "vrf", Prefix.parse("2.2.2.2/24"));
    RouteRowSecondaryKey rrsk1 = new RouteRowSecondaryKey(Ip.parse("1.1.1.1"), "bgp");
    RouteRowSecondaryKey rrsk2 = new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp");
    RouteRowSecondaryKey rrsk3 = new RouteRowSecondaryKey(Ip.parse("1.1.1.3"), "bgp");
    RouteRowSecondaryKey rrsk4 = new RouteRowSecondaryKey(Ip.parse("1.1.1.4"), "bgp");

    RouteRowAttribute rra1 = RouteRowAttribute.builder().setNextHop("node1").build();
    RouteRowAttribute rra2 = RouteRowAttribute.builder().setNextHop("node2").build();
    RouteRowAttribute rra3 = RouteRowAttribute.builder().setNextHop("node3").build();
    RouteRowAttribute rra4 = RouteRowAttribute.builder().setNextHop("node4").build();
    RouteRowAttribute rra5 = RouteRowAttribute.builder().setNextHop("node5").build();

    ImmutableMap.Builder<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>
        immutablelMapBuilderBase = ImmutableMap.builder();

    ImmutableMap.Builder<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>
        immutablelMapBuilderRef = ImmutableMap.builder();

    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroupsInBase =
        immutablelMapBuilderBase
            .put(rrsk1, ImmutableSortedSet.of(rra1))
            .put(rrsk2, ImmutableSortedSet.of(rra1, rra2))
            .put(rrsk3, ImmutableSortedSet.of(rra4))
            .build();

    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroupsInRef =
        immutablelMapBuilderRef
            .put(rrsk1, ImmutableSortedSet.of(rra2))
            .put(rrsk2, ImmutableSortedSet.of(rra1, rra3))
            .put(rrsk4, ImmutableSortedSet.of(rra5))
            .build();

    List<DiffRoutesOutput> diffRoutesOutputs =
        getRoutesDiff(
            ImmutableMap.of(routeRowKey, innerGroupsInBase),
            ImmutableMap.of(routeRowKey, innerGroupsInRef));

    assertThat(
        diffRoutesOutputs,
        containsInAnyOrder(
            new DiffRoutesOutput(
                routeRowKey,
                rrsk1,
                KeyPresenceStatus.IN_BOTH,
                ImmutableList.of(Lists.newArrayList(rra1, rra2)),
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                routeRowKey,
                rrsk2,
                KeyPresenceStatus.IN_BOTH,
                ImmutableList.of(
                    Lists.newArrayList(rra1, rra1),
                    Lists.newArrayList(rra2, null),
                    Lists.newArrayList(null, rra3)),
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                routeRowKey,
                rrsk3,
                KeyPresenceStatus.ONLY_IN_SNAPSHOT,
                ImmutableList.of(Lists.newArrayList(rra4, null)),
                KeyPresenceStatus.IN_BOTH),
            new DiffRoutesOutput(
                routeRowKey,
                rrsk4,
                KeyPresenceStatus.ONLY_IN_REFERENCE,
                ImmutableList.of(Lists.newArrayList(null, rra5)),
                KeyPresenceStatus.IN_BOTH)));
  }

  @Test
  public void testGetRoutesDiffNonCommonKey() {
    RouteRowKey routeRowKey1 = new RouteRowKey("node1", "vrf", Prefix.parse("1.1.1.1/24"));
    RouteRowKey routeRowKey2 = new RouteRowKey("node2", "vrf", Prefix.parse("1.1.1.2/24"));

    RouteRowSecondaryKey rrsk1 = new RouteRowSecondaryKey(Ip.parse("1.1.1.1"), "bgp");
    RouteRowSecondaryKey rrsk2 = new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp");

    RouteRowAttribute rra1 = RouteRowAttribute.builder().setNextHop("node11").build();
    RouteRowAttribute rra2 = RouteRowAttribute.builder().setNextHop("node22").build();

    ImmutableMap.Builder<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>
        immutablelMapBuilderBase = ImmutableMap.builder();

    ImmutableMap.Builder<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>
        immutablelMapBuilderRef = ImmutableMap.builder();

    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroupsInBase =
        immutablelMapBuilderBase.put(rrsk1, ImmutableSortedSet.of(rra1)).build();

    Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroupsInRef =
        immutablelMapBuilderRef.put(rrsk2, ImmutableSortedSet.of(rra2)).build();

    List<DiffRoutesOutput> diffRoutesOutputs =
        getRoutesDiff(
            ImmutableMap.of(routeRowKey1, innerGroupsInBase),
            ImmutableMap.of(routeRowKey2, innerGroupsInRef));

    assertThat(
        diffRoutesOutputs,
        containsInAnyOrder(
            new DiffRoutesOutput(
                routeRowKey1,
                rrsk1,
                KeyPresenceStatus.ONLY_IN_SNAPSHOT,
                ImmutableList.of(Lists.newArrayList(rra1, null)),
                KeyPresenceStatus.ONLY_IN_SNAPSHOT),
            new DiffRoutesOutput(
                routeRowKey2,
                rrsk2,
                KeyPresenceStatus.ONLY_IN_REFERENCE,
                ImmutableList.of(Lists.newArrayList(null, rra2)),
                KeyPresenceStatus.ONLY_IN_REFERENCE)));
  }

  @Test
  public void testAbstractRoutesRowDiff() {
    RouteRowAttribute.Builder routeRowAttrBuilder =
        RouteRowAttribute.builder().setAdminDistance(200).setMetric(2L).setNextHop("node1");

    List<List<RouteRowAttribute>> diffMatrix = new ArrayList<>();
    diffMatrix.add(Lists.newArrayList(routeRowAttrBuilder.build(), routeRowAttrBuilder.build()));

    List<DiffRoutesOutput> diff =
        ImmutableList.of(
            new DiffRoutesOutput(
                new RouteRowKey("node", "vrf", Prefix.parse("1.1.1.1/24")),
                new RouteRowSecondaryKey(Ip.parse("1.1.1.2"), "bgp"),
                KeyPresenceStatus.IN_BOTH,
                diffMatrix,
                KeyPresenceStatus.IN_BOTH));
    Multiset<Row> rows = getAbstractRouteRowsDiff(diff);

    assertThat(
        rows,
        containsInAnyOrder(
            ImmutableList.of(
                hasColumn(
                    COL_ROUTE_ENTRY_PRESENCE,
                    equalTo(RouteEntryPresenceStatus.UNCHANGED.routeEntryPresenceName()),
                    Schema.STRING))));
  }
}
