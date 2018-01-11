package org.batfish.bdp;

import static org.batfish.datamodel.RoutingProtocol.OSPF;
import static org.batfish.datamodel.RoutingProtocol.OSPF_E1;
import static org.batfish.datamodel.RoutingProtocol.OSPF_IA;
import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasMetric;
import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasProtocol;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.batfish.common.BatfishLogger;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfMetricType;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.BdpAnswerElement;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Test;

public class OspfTest {

  private static final Prefix C1_E1_2_PREFIX = new Prefix("10.12.0.1/24");
  private static final Prefix C1_L0_PREFIX = new Prefix("1.1.1.1/32");
  private static final Prefix C1_L1_PREFIX = new Prefix("1.1.1.11/32");
  private static final String C1_NAME = "R1";

  private static final Prefix C2_E2_1_PREFIX = new Prefix("10.12.0.2/24");
  private static final Prefix C2_E2_3_PREFIX = new Prefix("10.23.0.2/24");
  private static final Prefix C2_L0_PREFIX = new Prefix("2.2.2.2/32");
  private static final Prefix C2_L1_PREFIX = new Prefix("2.2.2.22/32");
  private static final String C2_NAME = "R2";

  private static final Prefix C3_E3_2_PREFIX = new Prefix("10.23.0.3/24");
  private static final Prefix C3_E3_4_PREFIX = new Prefix("10.34.0.3/24");
  private static final Prefix C3_L0_PREFIX = new Prefix("3.3.3.3/32");
  private static final Prefix C3_L1_PREFIX = new Prefix("3.3.3.33/32");
  private static final String C3_NAME = "R3";

  private static final Prefix C4_E4_3_PREFIX = new Prefix("10.34.0.4/24");
  private static final Prefix C4_L0_PREFIX = new Prefix("4.4.4.4/32");
  private static final Prefix C4_L1_PREFIX = new Prefix("4.4.4.44/32");
  private static final String C4_NAME = "R4";

  private static final long MAX_METRIC_EXTERNAL_NETWORKS = 16711680L;
  private static final long MAX_METRIC_STUB_NETWORKS = 65535L;
  private static final long MAX_METRIC_SUMMARY_NETWORKS = 16711680L;
  private static final long MAX_METRIC_TRANSIT_LINKS = 65535L;

  private static void assertNoRoute(
      SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode,
      String hostname,
      Prefix prefix) {
    assertThat(routesByNode, hasKey(hostname));
    SortedMap<String, SortedSet<AbstractRoute>> routesByVrf = routesByNode.get(hostname);
    assertThat(routesByVrf, hasKey(Configuration.DEFAULT_VRF_NAME));
    SortedSet<AbstractRoute> routes = routesByVrf.get(Configuration.DEFAULT_VRF_NAME);
    assertThat(routes, not(hasItem(hasPrefix(prefix))));
  }

  private static void assertRoute(
      SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode,
      RoutingProtocol protocol,
      String hostname,
      Prefix prefix,
      long expectedCost) {
    assertThat(routesByNode, hasKey(hostname));
    SortedMap<String, SortedSet<AbstractRoute>> routesByVrf = routesByNode.get(hostname);
    assertThat(routesByVrf, hasKey(Configuration.DEFAULT_VRF_NAME));
    SortedSet<AbstractRoute> routes = routesByVrf.get(Configuration.DEFAULT_VRF_NAME);
    assertThat(routes, hasItem(hasPrefix(prefix)));
    AbstractRoute route =
        routes.stream().filter(r -> r.getNetwork().equals(prefix)).findAny().get();
    assertThat(route, hasMetric(expectedCost));
    assertThat(route, hasProtocol(protocol));
  }

  private static List<Statement> getExportPolicyStatements(Prefix prefix) {
    long externalOspfMetric = 20L;
    If exportIfMatchL2Prefix = new If();
    exportIfMatchL2Prefix.setGuard(
        new MatchPrefixSet(
            new DestinationNetwork(),
            new ExplicitPrefixSet(
                new PrefixSpace(
                    ImmutableSet.of(
                        new PrefixRange(
                            prefix,
                            new SubRange(prefix.getPrefixLength(), prefix.getPrefixLength())))))));
    exportIfMatchL2Prefix.setTrueStatements(
        ImmutableList.of(
            new SetOspfMetricType(OspfMetricType.E1),
            new SetMetric(new LiteralLong(externalOspfMetric)),
            Statements.ExitAccept.toStaticStatement()));
    exportIfMatchL2Prefix.setFalseStatements(
        ImmutableList.of(Statements.ExitReject.toStaticStatement()));
    return ImmutableList.of(exportIfMatchL2Prefix);
  }

  /*
   * Int:1/2   2/1      2/3   3/2      3/4   4/3
   * R1 <=========> R2 <=========> R3 <=========> R4
   *  A      B       C      D       E      F       G
   *
   *  Areas:
   *  A: R1 Loopback0
   *  B: R1 E1/2, R2 E2/1
   *  C: R2 Loopback0
   *  D: R2 E2/3, R3 E3/2
   *  E: R3 Loopback0
   *  F: R3 E3/4, R4 E4/3
   *  G: R4 Loopback0
   */
  private static SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getOspfRoutes(
      long areaA,
      long areaB,
      long areaC,
      long areaD,
      long areaE,
      long areaF,
      long areaG,
      Long maxMetricExternalNetworks,
      Long maxMetricStubNetworks,
      Long maxMetricSummaryNetworks,
      Long maxMetricTransitLinks) {

    String l0Name = "Loopback0";
    String l1Name = "Loopback1";

    String c1E1To2Name = "Ethernet1/2";

    String c2E2To1Name = "Ethernet2/1";
    String c2E2To3Name = "Ethernet2/3";

    String c3E3To2Name = "Ethernet3/2";
    String c3E3To4Name = "Ethernet3/4";

    String c4E4To3Name = "Ethernet4/3";

    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    RoutingPolicy.Builder rpb = nf.routingPolicyBuilder();
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    OspfProcess.Builder opb = nf.ospfProcessBuilder();
    OspfArea.Builder oaba = nf.ospfAreaBuilder().setNumber(areaA);
    OspfArea.Builder oabb = nf.ospfAreaBuilder().setNumber(areaB);
    OspfArea.Builder oabc = nf.ospfAreaBuilder().setNumber(areaC);
    OspfArea.Builder oabd = nf.ospfAreaBuilder().setNumber(areaD);
    OspfArea.Builder oabe = nf.ospfAreaBuilder().setNumber(areaE);
    OspfArea.Builder oabf = nf.ospfAreaBuilder().setNumber(areaF);
    OspfArea.Builder oabg = nf.ospfAreaBuilder().setNumber(areaG);
    Interface.Builder ib =
        nf.interfaceBuilder().setActive(true).setOspfCost(1).setOspfEnabled(true);

    Configuration c1 = cb.setHostname(C1_NAME).build();
    Vrf v1 = vb.setOwner(c1).build();
    RoutingPolicy c1ExportPolicy =
        rpb.setOwner(c1).setStatements(getExportPolicyStatements(C1_L1_PREFIX)).build();
    OspfProcess op1 = opb.setVrf(v1).setExportPolicy(c1ExportPolicy).build();
    OspfArea oa1a = oaba.setOspfProcess(op1).build();
    OspfArea oa1b = areaA == areaB ? oa1a : oabb.setOspfProcess(op1).build();
    ib.setOwner(c1).setVrf(v1).setOspfArea(oa1a);
    ib.setOspfPassive(true).setName(l0Name).setAddress(C1_L0_PREFIX).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C1_L1_PREFIX)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa1b);
    ib.setName(c1E1To2Name).setAddress(C1_E1_2_PREFIX).build();

    Configuration c2 = cb.setHostname(C2_NAME).build();
    Vrf v2 = vb.setOwner(c2).build();
    RoutingPolicy c2ExportPolicy =
        rpb.setOwner(c2).setStatements(getExportPolicyStatements(C2_L1_PREFIX)).build();
    OspfProcess op2 =
        opb.setVrf(v2)
            .setMaxMetricExternalNetworks(maxMetricExternalNetworks)
            .setMaxMetricStubNetworks(maxMetricStubNetworks)
            .setMaxMetricSummaryNetworks(maxMetricSummaryNetworks)
            .setMaxMetricTransitLinks(maxMetricTransitLinks)
            .setExportPolicy(c2ExportPolicy)
            .build();
    opb.setMaxMetricExternalNetworks(null)
        .setMaxMetricStubNetworks(null)
        .setMaxMetricSummaryNetworks(null)
        .setMaxMetricTransitLinks(null);
    OspfArea oa2b = oabb.setOspfProcess(op2).build();
    OspfArea oa2c = areaB == areaC ? oa2b : oabc.setOspfProcess(op2).build();
    OspfArea oa2d =
        areaB == areaD ? oa2b : areaC == areaD ? oa2c : oabd.setOspfProcess(op2).build();
    ib.setOwner(c2).setVrf(v2).setOspfArea(oa2c);
    ib.setOspfPassive(true).setName(l0Name).setAddress(C2_L0_PREFIX).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C2_L1_PREFIX)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa2b);
    ib.setName(c2E2To1Name).setAddress(C2_E2_1_PREFIX).setOspfPointToPoint(true).build();
    ib.setOspfPointToPoint(false).setOspfArea(oa2d);
    ib.setName(c2E2To3Name).setAddress(C2_E2_3_PREFIX).build();

    Configuration c3 = cb.setHostname(C3_NAME).build();
    Vrf v3 = vb.setOwner(c3).build();
    RoutingPolicy c3ExportPolicy =
        rpb.setOwner(c3).setStatements(getExportPolicyStatements(C3_L1_PREFIX)).build();
    OspfProcess op3 = opb.setVrf(v3).setExportPolicy(c3ExportPolicy).build();
    OspfArea oa3d = oabd.setOspfProcess(op3).build();
    OspfArea oa3e = areaD == areaE ? oa3d : oabe.setOspfProcess(op3).build();
    OspfArea oa3f =
        areaD == areaF ? oa3d : areaE == areaF ? oa3e : oabf.setOspfProcess(op3).build();
    ib.setOwner(c3).setVrf(v3).setOspfArea(oa3e);
    ib.setOspfPassive(true).setName(l0Name).setAddress(C3_L0_PREFIX).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C3_L1_PREFIX)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa3d);
    ib.setName(c3E3To2Name).setAddress(C3_E3_2_PREFIX).build();
    ib.setName(c3E3To4Name).setAddress(C3_E3_4_PREFIX).setOspfArea(oa3f).build();

    Configuration c4 = cb.setHostname(C4_NAME).build();
    Vrf v4 = vb.setOwner(c4).build();
    RoutingPolicy c4ExportPolicy =
        rpb.setOwner(c4).setStatements(getExportPolicyStatements(C4_L1_PREFIX)).build();
    OspfProcess op4 = opb.setVrf(v4).setExportPolicy(c4ExportPolicy).build();
    OspfArea oa4f = oabf.setOspfProcess(op4).build();
    OspfArea oa4g = areaF == areaG ? oa4f : oabg.setOspfProcess(op4).build();
    ib.setOwner(c4).setVrf(v4).setOspfArea(oa4g);
    ib.setOspfPassive(true).setName(l0Name).setAddress(C4_L0_PREFIX).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C4_L1_PREFIX)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa4f);
    ib.setName(c4E4To3Name).setAddress(C4_E4_3_PREFIX).build();

    SortedMap<String, Configuration> configurations =
        new ImmutableSortedMap.Builder<String, Configuration>(String::compareTo)
            .put(c1.getName(), c1)
            .put(c2.getName(), c2)
            .put(c3.getName(), c3)
            .put(c4.getName(), c4)
            .build();
    BdpEngine engine =
        new BdpEngine(
            new TestBdpSettings(),
            new BatfishLogger(BatfishLogger.LEVELSTR_OUTPUT, false),
            (s, i) -> new AtomicInteger());
    Topology topology = CommonUtil.synthesizeTopology(configurations);
    BdpDataPlane dp =
        engine.computeDataPlane(
            false,
            configurations,
            topology,
            Collections.emptySet(),
            Collections.emptySet(),
            new BdpAnswerElement());

    return engine.getRoutes(dp);
  }

  @Test
  public void testOspfArea0MaxMetricTransit() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode =
        getOspfRoutes(
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            MAX_METRIC_EXTERNAL_NETWORKS,
            null,
            null,
            MAX_METRIC_TRANSIT_LINKS);
    assertRoute(routesByNode, OSPF, C1_NAME, C2_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C1_NAME, C4_L0_PREFIX, 65538L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_PREFIX, 65557L);
    assertRoute(routesByNode, OSPF, C1_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 65537L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 2L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_L0_PREFIX, 65538L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_PREFIX, 65557L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_PREFIX, 3L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 2L);
  }

  @Test
  public void testOspfArea0MaxMetricTransitAndStub() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode =
        getOspfRoutes(
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            0L,
            MAX_METRIC_EXTERNAL_NETWORKS,
            MAX_METRIC_STUB_NETWORKS,
            null,
            MAX_METRIC_TRANSIT_LINKS);
    assertRoute(routesByNode, OSPF, C1_NAME, C2_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C1_NAME, C4_L0_PREFIX, 65538L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_PREFIX, 65557L);
    assertRoute(routesByNode, OSPF, C1_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 65537L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_L0_PREFIX, 65538L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_PREFIX, 65557L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 65537L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 2L);
  }

  @Test
  public void testOspfDualAreaBackboneInterAreaPropagationMaxMetricTransitStubSummary() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode =
        getOspfRoutes(
            1L,
            1L,
            0L,
            0L,
            0L,
            0L,
            0L,
            MAX_METRIC_EXTERNAL_NETWORKS,
            MAX_METRIC_STUB_NETWORKS,
            MAX_METRIC_SUMMARY_NETWORKS,
            MAX_METRIC_TRANSIT_LINKS);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_PREFIX, 16711702L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 2L);
  }

  @Test
  public void testOspfDualAreaNonBackboneInterAreaPropagationMaxMetricTransitStubSummary() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode =
        getOspfRoutes(
            0L,
            0L,
            1L,
            1L,
            1L,
            1L,
            1L,
            MAX_METRIC_EXTERNAL_NETWORKS,
            MAX_METRIC_STUB_NETWORKS,
            MAX_METRIC_SUMMARY_NETWORKS,
            MAX_METRIC_TRANSIT_LINKS);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_PREFIX, 16711702L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 2L);
  }

  @Test
  public void testOspfMultiAreaThroughBackboneMaxMetricTransitStubSummary() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode =
        getOspfRoutes(
            1L,
            1L,
            0L,
            0L,
            0L,
            2L,
            2L,
            MAX_METRIC_EXTERNAL_NETWORKS,
            MAX_METRIC_STUB_NETWORKS,
            MAX_METRIC_SUMMARY_NETWORKS,
            MAX_METRIC_TRANSIT_LINKS);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_PREFIX, 16711701);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF_IA, C2_NAME, C4_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_PREFIX, 65556L);
    assertRoute(routesByNode, OSPF_IA, C2_NAME, C3_E3_4_PREFIX.getNetworkPrefix(), 65536L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 16711681L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_PREFIX, 16711702L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C2_L0_PREFIX, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_PREFIX, 16711682L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C3_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 16711682L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 2L);
  }

  @Test
  public void testOspfMultiAreaThroughNonBackboneMaxMetricTransitStubSummary() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode =
        getOspfRoutes(
            0L,
            0L,
            1L,
            1L,
            1L,
            2L,
            2L,
            MAX_METRIC_EXTERNAL_NETWORKS,
            MAX_METRIC_STUB_NETWORKS,
            MAX_METRIC_SUMMARY_NETWORKS,
            MAX_METRIC_TRANSIT_LINKS);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_PREFIX, 16711701L);
    assertNoRoute(routesByNode, C1_NAME, C4_L0_PREFIX);
    assertNoRoute(routesByNode, C1_NAME, C4_L1_PREFIX);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_PREFIX.getNetworkPrefix(), 16711681L);
    assertNoRoute(routesByNode, C1_NAME, C3_E3_4_PREFIX.getNetworkPrefix());
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_PREFIX, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_PREFIX, 65555L);
    assertNoRoute(routesByNode, C2_NAME, C4_L0_PREFIX);
    assertNoRoute(routesByNode, C2_NAME, C4_L1_PREFIX);
    assertNoRoute(routesByNode, C2_NAME, C3_E3_4_PREFIX.getNetworkPrefix());
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_PREFIX, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_PREFIX, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_PREFIX, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_PREFIX, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_PREFIX, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_PREFIX.getNetworkPrefix(), 16711681L);
    assertNoRoute(routesByNode, C4_NAME, C1_L0_PREFIX);
    assertNoRoute(routesByNode, C4_NAME, C1_L1_PREFIX);
    assertNoRoute(routesByNode, C4_NAME, C2_L0_PREFIX);
    assertNoRoute(routesByNode, C4_NAME, C2_L1_PREFIX);
    assertNoRoute(routesByNode, C4_NAME, C3_L0_PREFIX);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_PREFIX, 21L);
    assertNoRoute(routesByNode, C4_NAME, C1_E1_2_PREFIX.getNetworkPrefix());
    assertNoRoute(routesByNode, C4_NAME, C2_E2_3_PREFIX.getNetworkPrefix());
  }
}
