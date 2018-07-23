package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.RoutingProtocol.OSPF;
import static org.batfish.datamodel.RoutingProtocol.OSPF_E1;
import static org.batfish.datamodel.RoutingProtocol.OSPF_E2;
import static org.batfish.datamodel.RoutingProtocol.OSPF_IA;
import static org.batfish.dataplane.ibdp.TestUtils.assertNoRoute;
import static org.batfish.dataplane.ibdp.TestUtils.assertRoute;

import com.google.common.collect.ImmutableList;
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
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.ospf.NssaSettings;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubSettings;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Test;

public class OspfTest {

  private static final InterfaceAddress C1_E1_2_ADDRESS = new InterfaceAddress("10.12.0.1/24");
  private static final InterfaceAddress C1_L0_ADDRESS = new InterfaceAddress("1.1.1.1/32");
  private static final InterfaceAddress C1_L1_ADDRESS = new InterfaceAddress("1.1.1.11/32");
  private static final String C1_NAME = "R1";

  private static final InterfaceAddress C2_E2_1_ADDRESS = new InterfaceAddress("10.12.0.2/24");
  private static final InterfaceAddress C2_E2_3_ADDRESS = new InterfaceAddress("10.23.0.2/24");
  private static final InterfaceAddress C2_L0_ADDRESS = new InterfaceAddress("2.2.2.2/32");
  private static final InterfaceAddress C2_L1_ADDRESS = new InterfaceAddress("2.2.2.22/32");
  private static final String C2_NAME = "R2";

  private static final InterfaceAddress C3_E3_2_ADDRESS = new InterfaceAddress("10.23.0.3/24");
  private static final InterfaceAddress C3_E3_4_ADDRESS = new InterfaceAddress("10.34.0.3/24");
  private static final InterfaceAddress C3_L0_ADDRESS = new InterfaceAddress("3.3.3.3/32");
  private static final InterfaceAddress C3_L1_ADDRESS = new InterfaceAddress("3.3.3.33/32");
  private static final String C3_NAME = "R3";

  private static final InterfaceAddress C4_E4_3_ADDRESS = new InterfaceAddress("10.34.0.4/24");
  private static final InterfaceAddress C4_L0_ADDRESS = new InterfaceAddress("4.4.4.4/32");
  private static final InterfaceAddress C4_L1_ADDRESS = new InterfaceAddress("4.4.4.44/32");
  private static final String C4_NAME = "R4";

  private static final long MAX_METRIC_EXTERNAL_NETWORKS = 16711680L;
  private static final long MAX_METRIC_STUB_NETWORKS = 65535L;
  private static final long MAX_METRIC_SUMMARY_NETWORKS = 16711680L;
  private static final long MAX_METRIC_TRANSIT_LINKS = 65535L;

  private static List<Statement> getExportPolicyStatements(InterfaceAddress address) {
    long externalOspfMetric = 20L;
    If exportIfMatchL2Prefix = new If();
    exportIfMatchL2Prefix.setGuard(
        new MatchPrefixSet(
            new DestinationNetwork(),
            new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(address.getPrefix())))));
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
        rpb.setOwner(c1).setStatements(getExportPolicyStatements(C1_L1_ADDRESS)).build();
    OspfProcess op1 = opb.setVrf(v1).setExportPolicy(c1ExportPolicy).build();
    OspfArea oa1a = oaba.setOspfProcess(op1).build();
    OspfArea oa1b = areaA == areaB ? oa1a : oabb.setOspfProcess(op1).build();
    ib.setOwner(c1).setVrf(v1).setOspfArea(oa1a);
    ib.setOspfPassive(true).setName(l0Name).setAddress(C1_L0_ADDRESS).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C1_L1_ADDRESS)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa1b);
    ib.setName(c1E1To2Name).setAddress(C1_E1_2_ADDRESS).build();

    Configuration c2 = cb.setHostname(C2_NAME).build();
    Vrf v2 = vb.setOwner(c2).build();
    RoutingPolicy c2ExportPolicy =
        rpb.setOwner(c2).setStatements(getExportPolicyStatements(C2_L1_ADDRESS)).build();
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
    ib.setOspfPassive(true).setName(l0Name).setAddress(C2_L0_ADDRESS).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C2_L1_ADDRESS)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa2b);
    ib.setName(c2E2To1Name).setAddress(C2_E2_1_ADDRESS).setOspfPointToPoint(true).build();
    ib.setOspfPointToPoint(false).setOspfArea(oa2d);
    ib.setName(c2E2To3Name).setAddress(C2_E2_3_ADDRESS).build();

    Configuration c3 = cb.setHostname(C3_NAME).build();
    Vrf v3 = vb.setOwner(c3).build();
    RoutingPolicy c3ExportPolicy =
        rpb.setOwner(c3).setStatements(getExportPolicyStatements(C3_L1_ADDRESS)).build();
    OspfProcess op3 = opb.setVrf(v3).setExportPolicy(c3ExportPolicy).build();
    OspfArea oa3d = oabd.setOspfProcess(op3).build();
    OspfArea oa3e = areaD == areaE ? oa3d : oabe.setOspfProcess(op3).build();
    OspfArea oa3f =
        areaD == areaF ? oa3d : areaE == areaF ? oa3e : oabf.setOspfProcess(op3).build();
    ib.setOwner(c3).setVrf(v3).setOspfArea(oa3e);
    ib.setOspfPassive(true).setName(l0Name).setAddress(C3_L0_ADDRESS).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C3_L1_ADDRESS)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa3d);
    ib.setName(c3E3To2Name).setAddress(C3_E3_2_ADDRESS).build();
    ib.setName(c3E3To4Name).setAddress(C3_E3_4_ADDRESS).setOspfArea(oa3f).build();

    Configuration c4 = cb.setHostname(C4_NAME).build();
    Vrf v4 = vb.setOwner(c4).build();
    RoutingPolicy c4ExportPolicy =
        rpb.setOwner(c4).setStatements(getExportPolicyStatements(C4_L1_ADDRESS)).build();
    OspfProcess op4 = opb.setVrf(v4).setExportPolicy(c4ExportPolicy).build();
    OspfArea oa4f = oabf.setOspfProcess(op4).build();
    OspfArea oa4g = areaF == areaG ? oa4f : oabg.setOspfProcess(op4).build();
    ib.setOwner(c4).setVrf(v4).setOspfArea(oa4g);
    ib.setOspfPassive(true).setName(l0Name).setAddress(C4_L0_ADDRESS).build();
    ib.setOspfEnabled(false)
        .setOspfPassive(false)
        .setOspfArea(null)
        .setName(l1Name)
        .setAddress(C4_L1_ADDRESS)
        .build();
    ib.setOspfEnabled(true).setOspfArea(oa4f);
    ib.setName(c4E4To3Name).setAddress(C4_E4_3_ADDRESS).build();

    SortedMap<String, Configuration> configurations =
        new ImmutableSortedMap.Builder<String, Configuration>(String::compareTo)
            .put(c1.getHostname(), c1)
            .put(c2.getHostname(), c2)
            .put(c3.getHostname(), c3)
            .put(c4.getHostname(), c4)
            .build();
    IncrementalBdpEngine engine =
        new IncrementalBdpEngine(
            new IncrementalDataPlaneSettings(),
            new BatfishLogger(BatfishLogger.LEVELSTR_OUTPUT, false),
            (s, i) -> new AtomicInteger());
    Topology topology = CommonUtil.synthesizeTopology(configurations);
    IncrementalDataPlane dp =
        (IncrementalDataPlane)
            engine.computeDataPlane(false, configurations, topology, Collections.emptySet())
                ._dataPlane;

    return IncrementalBdpEngine.getRoutes(dp);
  }

  /**
   * Get routes for a multi-area network with backbone area, regular area, stub area, and
   * not-so-stubby stub area.<br>
   * R0 &#8592; A0 &#8594; R1<br>
   * R0 &#8592; A1Stub &#8594; R2 &#8592; A1Stub &#8594; R3<br>
   * R0 &#8592; A2Nssa &#8594; R4 &#8592; A2Nssa &#8594; R5<br>
   * R0 &#8592; A3 &#8594; R6
   *
   * @param noSummaryStub1 Whether the stub area should receive inter-area routes from ABR
   * @param noSummaryNssa2 Whether the not-so-stubby stub area should receive inter-area routes from
   *     ABR
   * @param nssaDefaultType The nature of the default route the ABR advertises to the NSSA.
   */
  private static SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getOspfStubBehavior(
      boolean noSummaryStub1, boolean noSummaryNssa2, OspfDefaultOriginateType nssaDefaultType) {
    String r0Name = "R0";
    String r1Name = "R1";
    String r2Name = "R2";
    String r3Name = "R3";
    String r4Name = "R4";
    String r5Name = "R5";
    String r6Name = "R6";
    String i01Name = "Ethernet0/1";
    String i10Name = "Ethernet1/0";
    String i02Name = "Ethernet0/2";
    String i20Name = "Ethernet2/0";
    String i23Name = "Ethernet2/3";
    String i32Name = "Ethernet3/2";
    String i04Name = "Ethernet0/4";
    String i40Name = "Ethernet4/0";
    String i45Name = "Ethernet4/5";
    String i54Name = "Ethernet5/4";
    String i06Name = "Ethernet0/6";
    String i60Name = "Ethernet6/0";

    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Interface.Builder ib =
        nf.interfaceBuilder()
            .setActive(true)
            .setOspfCost(10)
            .setOspfEnabled(true)
            .setOspfPointToPoint(true)
            .setBandwidth(100E6);
    OspfProcess.Builder opb = nf.ospfProcessBuilder().setProcessId("1");
    OspfArea.Builder oab = nf.ospfAreaBuilder();

    // R0
    Configuration r0 = cb.setHostname(r0Name).build();
    Vrf v0 = vb.setOwner(r0).build();
    OspfProcess op0 = opb.setVrf(v0).build();
    oab.setOspfProcess(op0);
    OspfArea oaR0A0 = oab.setNumber(0L).setNonStub().build();
    OspfArea oaR0A1 =
        oab.setNumber(1L)
            .setStub(StubSettings.builder().setSuppressType3(noSummaryStub1).build())
            .build();
    OspfArea oaR0A2 =
        oab.setNumber(2L)
            .setNssa(
                NssaSettings.builder()
                    .setSuppressType3(noSummaryNssa2)
                    .setDefaultOriginateType(nssaDefaultType)
                    .build())
            .build();
    OspfArea oaR0A3 = oab.setNumber(3L).setNonStub().build();
    ib.setOwner(r0).setVrf(v0);
    // i01
    ib.setName(i01Name).setOspfArea(oaR0A0).setAddress(new InterfaceAddress("10.0.1.0/24")).build();
    // i02
    ib.setName(i02Name).setOspfArea(oaR0A1).setAddress(new InterfaceAddress("10.0.2.0/24")).build();
    // i04
    ib.setName(i04Name).setOspfArea(oaR0A2).setAddress(new InterfaceAddress("10.0.4.0/24")).build();
    // i06
    ib.setName(i06Name).setOspfArea(oaR0A3).setAddress(new InterfaceAddress("10.0.6.0/24")).build();

    // R1
    Configuration r1 = cb.setHostname(r1Name).build();
    Vrf v1 = vb.setOwner(r1).build();
    OspfProcess op1 = opb.setVrf(v1).build();
    oab.setOspfProcess(op1);
    OspfArea oaR1A0 = oab.setNumber(0L).setNonStub().build();
    ib.setOwner(r1).setVrf(v1);
    // i10
    ib.setName(i10Name).setOspfArea(oaR1A0).setAddress(new InterfaceAddress("10.0.1.1/24")).build();

    // R2
    Configuration r2 = cb.setHostname(r2Name).build();
    Vrf v2 = vb.setOwner(r2).build();
    OspfProcess op2 = opb.setVrf(v2).build();
    oab.setOspfProcess(op2);
    OspfArea oaR2A1 = oab.setNumber(1L).setStub(StubSettings.builder().build()).build();
    ib.setOwner(r2).setVrf(v2);
    // i20
    ib.setName(i20Name).setOspfArea(oaR2A1).setAddress(new InterfaceAddress("10.0.2.2/24")).build();
    // i23
    ib.setName(i23Name).setOspfArea(oaR2A1).setAddress(new InterfaceAddress("10.2.3.2/24")).build();

    // R3
    Configuration r3 = cb.setHostname(r3Name).build();
    Vrf v3 = vb.setOwner(r3).build();
    OspfProcess op3 = opb.setVrf(v3).build();
    oab.setOspfProcess(op3);
    OspfArea oaR3A1 = oab.setNumber(1L).setStub(StubSettings.builder().build()).build();
    ib.setOwner(r3).setVrf(v3);
    // i32
    ib.setName(i32Name).setOspfArea(oaR3A1).setAddress(new InterfaceAddress("10.2.3.3/24")).build();

    // R4
    Configuration r4 = cb.setHostname(r4Name).build();
    Vrf v4 = vb.setOwner(r4).build();
    OspfProcess op4 = opb.setVrf(v4).build();
    oab.setOspfProcess(op4);
    OspfArea oaR4A2 = oab.setNumber(2L).setNssa(NssaSettings.builder().build()).build();
    ib.setOwner(r4).setVrf(v4);
    // i40
    ib.setName(i40Name).setOspfArea(oaR4A2).setAddress(new InterfaceAddress("10.0.4.4/24")).build();
    // i45
    ib.setName(i45Name).setOspfArea(oaR4A2).setAddress(new InterfaceAddress("10.4.5.4/24")).build();

    // R5
    Configuration r5 = cb.setHostname(r5Name).build();
    Vrf v5 = vb.setOwner(r5).build();
    OspfProcess op5 = opb.setVrf(v5).build();
    oab.setOspfProcess(op5);
    OspfArea oaR5A2 = oab.setNumber(2L).setNssa(NssaSettings.builder().build()).build();
    ib.setOwner(r5).setVrf(v5);
    // i54
    ib.setName(i54Name).setOspfArea(oaR5A2).setAddress(new InterfaceAddress("10.4.5.5/24")).build();

    // R6
    Configuration r6 = cb.setHostname(r6Name).build();
    Vrf v6 = vb.setOwner(r6).build();
    v6.getStaticRoutes()
        .add(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("10.10.10.10/32"))
                .setAdministrativeCost(1)
                .setMetric(0)
                .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                .setTag(Route.UNSET_ROUTE_TAG)
                .build());
    RoutingPolicy exportStatic =
        nf.routingPolicyBuilder()
            .setOwner(r6)
            .setName("exportStatic")
            .setStatements(
                ImmutableList.<Statement>of(
                    new If(
                        new MatchProtocol(RoutingProtocol.STATIC),
                        ImmutableList.of(
                            new SetOspfMetricType(OspfMetricType.E2),
                            new SetMetric(new LiteralLong(33L)),
                            Statements.ExitAccept.toStaticStatement()),
                        ImmutableList.of(Statements.ExitReject.toStaticStatement()))))
            .build();
    OspfProcess op6 = opb.setVrf(v6).setExportPolicy(exportStatic).build();
    oab.setOspfProcess(op6);
    OspfArea oaR6A03 = oab.setNumber(3L).setNonStub().build();
    ib.setOwner(r6).setVrf(v6);
    // i60
    ib.setName(i60Name)
        .setOspfArea(oaR6A03)
        .setAddress(new InterfaceAddress("10.0.6.6/24"))
        .build();

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.<String, Configuration>naturalOrder()
            .put(r0Name, r0)
            .put(r1Name, r1)
            .put(r2Name, r2)
            .put(r3Name, r3)
            .put(r4Name, r4)
            .put(r5Name, r5)
            .put(r6Name, r6)
            .build();
    IncrementalBdpEngine engine =
        new IncrementalBdpEngine(
            new IncrementalDataPlaneSettings(),
            new BatfishLogger(BatfishLogger.LEVELSTR_OUTPUT, false),
            (s, i) -> new AtomicInteger());
    Topology topology = CommonUtil.synthesizeTopology(configurations);
    IncrementalDataPlane dp =
        (IncrementalDataPlane)
            engine.computeDataPlane(false, configurations, topology, Collections.emptySet())
                ._dataPlane;

    return IncrementalBdpEngine.getRoutes(dp);
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
    assertRoute(routesByNode, OSPF, C1_NAME, C2_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C1_NAME, C4_L0_ADDRESS, 65538L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_ADDRESS, 65557L);
    assertRoute(routesByNode, OSPF, C1_NAME, C2_E2_3_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_E3_4_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_E1_2_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_L0_ADDRESS, 65538L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_ADDRESS, 65557L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_E1_2_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
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
    assertRoute(routesByNode, OSPF, C1_NAME, C2_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C1_NAME, C4_L0_ADDRESS, 65538L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_ADDRESS, 65557L);
    assertRoute(routesByNode, OSPF, C1_NAME, C2_E2_3_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF, C1_NAME, C3_E3_4_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF, C3_NAME, C1_E1_2_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_L0_ADDRESS, 65538L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_ADDRESS, 65557L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF, C4_NAME, C1_E1_2_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
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
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_ADDRESS, 16711702L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
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
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_ADDRESS, 16711702L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
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
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_ADDRESS, 16711701);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C4_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF_IA, C2_NAME, C4_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C4_L1_ADDRESS, 65556L);
    assertRoute(routesByNode, OSPF_IA, C2_NAME, C3_E3_4_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C1_L1_ADDRESS, 16711702L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C2_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C2_L1_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C2_E2_3_ADDRESS, 2L);
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
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C1_NAME, C3_L1_ADDRESS, 16711701L);
    assertNoRoute(routesByNode, C1_NAME, C4_L0_ADDRESS);
    assertNoRoute(routesByNode, C1_NAME, C4_L1_ADDRESS);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 16711681L);
    assertNoRoute(routesByNode, C1_NAME, C3_E3_4_ADDRESS);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C1_L1_ADDRESS, 65555L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C2_NAME, C3_L1_ADDRESS, 65555L);
    assertNoRoute(routesByNode, C2_NAME, C4_L0_ADDRESS);
    assertNoRoute(routesByNode, C2_NAME, C4_L1_ADDRESS);
    assertNoRoute(routesByNode, C2_NAME, C3_E3_4_ADDRESS);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C1_L1_ADDRESS, 16711701L);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C2_L1_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_E1, C3_NAME, C4_L1_ADDRESS, 21L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 16711681L);
    assertNoRoute(routesByNode, C4_NAME, C1_L0_ADDRESS);
    assertNoRoute(routesByNode, C4_NAME, C1_L1_ADDRESS);
    assertNoRoute(routesByNode, C4_NAME, C2_L0_ADDRESS);
    assertNoRoute(routesByNode, C4_NAME, C2_L1_ADDRESS);
    assertNoRoute(routesByNode, C4_NAME, C3_L0_ADDRESS);
    assertRoute(routesByNode, OSPF_E1, C4_NAME, C3_L1_ADDRESS, 21L);
    assertNoRoute(routesByNode, C4_NAME, C1_E1_2_ADDRESS);
    assertNoRoute(routesByNode, C4_NAME, C2_E2_3_ADDRESS);
  }

  @Test
  public void testOspfStubBehaviorBackboneRoutes() {
    // NSSA args don't really matter
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.INTER_AREA);
    assertNoRoute(routesWithSummaries, "R0", Prefix.ZERO);
    assertRoute(routesWithSummaries, OSPF, "R0", Prefix.parse("10.2.3.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF, "R0", Prefix.parse("10.4.5.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_E2, "R0", Prefix.parse("10.10.10.10/32"), 33L);
    assertNoRoute(routesWithSummaries, "R1", Prefix.ZERO);
    assertRoute(routesWithSummaries, OSPF_IA, "R1", Prefix.parse("10.0.2.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R1", Prefix.parse("10.2.3.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R1", Prefix.parse("10.0.4.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R1", Prefix.parse("10.4.5.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R1", Prefix.parse("10.0.6.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_E2, "R1", Prefix.parse("10.10.10.10/32"), 33L);
  }

  @Test
  public void testOspfStubBehaviorNssaDefaultTypes() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesInterArea =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.INTER_AREA);
    assertRoute(routesInterArea, OSPF_IA, "R4", Prefix.ZERO, 10L);
    assertRoute(routesInterArea, OSPF_IA, "R5", Prefix.ZERO, 20L);

    /*
     * TODO: Re-enable tests when non-IA NSSA default origination is implemented
     * https://github.com/batfish/batfish/issues/1644
     */
    /*
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesExternalType1 =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.EXTERNAL_TYPE1);
    assertRoute(routesExternalType1, OSPF_E1, "R4", Prefix.ZERO, 10L);
    assertRoute(routesExternalType1, OSPF_E1, "R5", Prefix.ZERO, 20L);

    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesExternalType2 =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.EXTERNAL_TYPE2);
    assertRoute(routesExternalType2, OSPF_E2, "R4", Prefix.ZERO, 10L);
    assertRoute(routesExternalType2, OSPF_E2, "R5", Prefix.ZERO, 10L);
    */

    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesNone =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesNone, "R4", Prefix.ZERO);
    assertNoRoute(routesNone, "R5", Prefix.ZERO);
  }

  @Test
  public void testOspfStubBehaviorNssaRoutes() {
    // stub args don't really matter
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(true, false, OspfDefaultOriginateType.NONE);
    assertRoute(routesWithSummaries, OSPF_IA, "R4", Prefix.parse("10.0.1.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R4", Prefix.parse("10.0.2.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R4", Prefix.parse("10.2.3.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R4", Prefix.parse("10.0.6.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_E2, "R4", Prefix.parse("10.10.10.10/32"), 33L);
    assertRoute(routesWithSummaries, OSPF_IA, "R5", Prefix.parse("10.0.1.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R5", Prefix.parse("10.0.2.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R5", Prefix.parse("10.2.3.0/24"), 40L);
    assertRoute(routesWithSummaries, OSPF_IA, "R5", Prefix.parse("10.0.6.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_E2, "R5", Prefix.parse("10.10.10.10/32"), 33L);

    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesWithoutSummaries =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesWithoutSummaries, "R4", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "R4", Prefix.parse("10.0.2.0/24"));
    assertNoRoute(routesWithoutSummaries, "R4", Prefix.parse("10.2.3.0/24"));
    assertNoRoute(routesWithoutSummaries, "R4", Prefix.parse("10.0.6.0/24"));
    assertRoute(routesWithSummaries, OSPF_E2, "R4", Prefix.parse("10.10.10.10/32"), 33L);
    assertNoRoute(routesWithoutSummaries, "R5", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "R5", Prefix.parse("10.0.2.0/24"));
    assertNoRoute(routesWithoutSummaries, "R5", Prefix.parse("10.2.3.0/24"));
    assertNoRoute(routesWithoutSummaries, "R5", Prefix.parse("10.0.6.0/24"));
    assertRoute(routesWithSummaries, OSPF_E2, "R5", Prefix.parse("10.10.10.10/32"), 33L);
  }

  @Test
  public void testOspfStubBehaviorRegularAreaRoutes() {
    // NSSA args don't really matter
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesWithSummaries, "R6", Prefix.ZERO);
    assertRoute(routesWithSummaries, OSPF_IA, "R6", Prefix.parse("10.0.1.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R6", Prefix.parse("10.0.2.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R6", Prefix.parse("10.2.3.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R6", Prefix.parse("10.0.4.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R6", Prefix.parse("10.4.5.0/24"), 30L);
  }

  @Test
  public void testOspfStubBehaviorStubRoutes() {
    // NSSA args don't really matter
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(false, false, OspfDefaultOriginateType.NONE);
    assertRoute(routesWithSummaries, OSPF_IA, "R2", Prefix.parse("10.0.1.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R2", Prefix.parse("10.0.4.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R2", Prefix.parse("10.4.5.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R2", Prefix.parse("10.0.6.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "R2", Prefix.ZERO, 10L);
    assertNoRoute(routesWithSummaries, "R2", Prefix.parse("10.10.10.10/32"));
    assertRoute(routesWithSummaries, OSPF_IA, "R3", Prefix.parse("10.0.1.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R3", Prefix.parse("10.0.4.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R3", Prefix.parse("10.4.5.0/24"), 40L);
    assertRoute(routesWithSummaries, OSPF_IA, "R3", Prefix.parse("10.0.6.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "R3", Prefix.ZERO, 20L);
    assertNoRoute(routesWithSummaries, "R3", Prefix.parse("10.10.10.10/32"));

    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesWithoutSummaries =
        getOspfStubBehavior(true, false, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesWithoutSummaries, "R2", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "R2", Prefix.parse("10.0.4.0/24"));
    assertNoRoute(routesWithoutSummaries, "R2", Prefix.parse("10.4.5.0/24"));
    assertNoRoute(routesWithoutSummaries, "R2", Prefix.parse("10.0.6.0/24"));
    assertNoRoute(routesWithSummaries, "R2", Prefix.parse("10.10.10.10/32"));
    assertNoRoute(routesWithoutSummaries, "R3", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "R3", Prefix.parse("10.0.4.0/24"));
    assertNoRoute(routesWithoutSummaries, "R3", Prefix.parse("10.4.5.0/24"));
    assertNoRoute(routesWithoutSummaries, "R3", Prefix.parse("10.0.6.0/24"));
    assertNoRoute(routesWithSummaries, "R3", Prefix.parse("10.10.10.10/32"));
  }
}
