package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.RoutingProtocol.OSPF;
import static org.batfish.datamodel.RoutingProtocol.OSPF_E1;
import static org.batfish.datamodel.RoutingProtocol.OSPF_E2;
import static org.batfish.datamodel.RoutingProtocol.OSPF_IA;
import static org.batfish.datamodel.RoutingProtocol.OSPF_IS;
import static org.batfish.datamodel.ospf.OspfTopologyUtils.computeOspfTopology;
import static org.batfish.dataplane.ibdp.TestUtils.assertNoRoute;
import static org.batfish.dataplane.ibdp.TestUtils.assertRoute;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.ospf.NssaSettings;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.OspfTopologyUtils;
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
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OspfTest {

  private static final ConcreteInterfaceAddress C1_E1_2_ADDRESS =
      ConcreteInterfaceAddress.parse("10.12.0.1/24");
  private static final ConcreteInterfaceAddress C1_L0_ADDRESS =
      ConcreteInterfaceAddress.parse("1.1.1.1/32");
  private static final Prefix C1_L0_SUMMARY_PREFIX = Prefix.strict("1.1.1.0/31");
  private static final String C1_L0_SUMMARY_FILTER_NAME = "c1l0summaryfilter";
  private static final ConcreteInterfaceAddress C1_L1_ADDRESS =
      ConcreteInterfaceAddress.parse("1.1.1.11/32");
  private static final String C1_NAME = "r1";

  private static final ConcreteInterfaceAddress C2_E2_1_ADDRESS =
      ConcreteInterfaceAddress.parse("10.12.0.2/24");
  private static final ConcreteInterfaceAddress C2_E2_3_ADDRESS =
      ConcreteInterfaceAddress.parse("10.23.0.2/24");
  private static final ConcreteInterfaceAddress C2_L0_ADDRESS =
      ConcreteInterfaceAddress.parse("2.2.2.2/32");
  private static final ConcreteInterfaceAddress C2_L1_ADDRESS =
      ConcreteInterfaceAddress.parse("2.2.2.22/32");
  private static final String C2_NAME = "r2";

  private static final ConcreteInterfaceAddress C3_E3_2_ADDRESS =
      ConcreteInterfaceAddress.parse("10.23.0.3/24");
  private static final ConcreteInterfaceAddress C3_E3_4_ADDRESS =
      ConcreteInterfaceAddress.parse("10.34.0.3/24");
  private static final ConcreteInterfaceAddress C3_L0_ADDRESS =
      ConcreteInterfaceAddress.parse("3.3.3.3/32");
  private static final ConcreteInterfaceAddress C3_L1_ADDRESS =
      ConcreteInterfaceAddress.parse("3.3.3.33/32");
  private static final String C3_NAME = "r3";

  private static final ConcreteInterfaceAddress C4_E4_3_ADDRESS =
      ConcreteInterfaceAddress.parse("10.34.0.4/24");
  private static final ConcreteInterfaceAddress C4_L0_ADDRESS =
      ConcreteInterfaceAddress.parse("4.4.4.4/32");
  private static final ConcreteInterfaceAddress C4_L1_ADDRESS =
      ConcreteInterfaceAddress.parse("4.4.4.44/32");
  private static final String C4_NAME = "r4";

  private static final long MAX_METRIC_EXTERNAL_NETWORKS = 16711680L;
  private static final long MAX_METRIC_STUB_NETWORKS = 65535L;
  private static final long MAX_METRIC_SUMMARY_NETWORKS = 16711680L;
  private static final long MAX_METRIC_TRANSIT_LINKS = 65535L;

  private static List<Statement> getExportPolicyStatements(ConcreteInterfaceAddress address) {
    long externalOspfMetric = 20L;
    If exportIfMatchL2Prefix = new If();
    exportIfMatchL2Prefix.setGuard(
        new MatchPrefixSet(
            DestinationNetwork.instance(),
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

  private static OspfInterfaceSettings.Builder baseOspfSettings() {
    return OspfInterfaceSettings.defaultSettingsBuilder().setCost(1).setProcess("1");
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
   *
   * summarizeR1L0: whether to summarize R1 loopback0's prefix on R2 for area B
   * summarizeR1L0Advertise: whether to advertise the summary of R1 loopback0's prefix
   * summarizeR1L0Metric: fixed or dynamic(null) metric for advertising the summary of
   *                      R1 loopback0's prefix from R2
   */
  private static SortedMap<String, SortedMap<String, Set<AbstractRoute>>> getOspfRoutes(
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
      Long maxMetricTransitLinks,
      boolean summarizeR1L0,
      boolean summaryR1L0Advertise,
      Long summarizeR1L0Metric) {

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
    if (summarizeR1L0) {
      oabb.addSummary(
          C1_L0_SUMMARY_PREFIX, new OspfAreaSummary(summaryR1L0Advertise, summarizeR1L0Metric));
      oabb.setSummaryFilter(C1_L0_SUMMARY_FILTER_NAME);
    }
    OspfArea.Builder oabc = nf.ospfAreaBuilder().setNumber(areaC);
    OspfArea.Builder oabd = nf.ospfAreaBuilder().setNumber(areaD);
    OspfArea.Builder oabe = nf.ospfAreaBuilder().setNumber(areaE);
    OspfArea.Builder oabf = nf.ospfAreaBuilder().setNumber(areaF);
    OspfArea.Builder oabg = nf.ospfAreaBuilder().setNumber(areaG);
    Interface.Builder ib = nf.interfaceBuilder();

    Configuration c1 = cb.setHostname(C1_NAME).build();
    Vrf v1 = vb.setOwner(c1).build();
    RoutingPolicy c1ExportPolicy =
        rpb.setOwner(c1).setStatements(getExportPolicyStatements(C1_L1_ADDRESS)).build();
    OspfProcess op1 =
        opb.setVrf(v1)
            .setRouterId(C1_L1_ADDRESS.getIp())
            .setProcessId("1")
            .setExportPolicy(c1ExportPolicy)
            .build();
    OspfArea oa1a = oaba.setOspfProcess(op1).build();
    OspfArea oa1b = areaA == areaB ? oa1a : oabb.setOspfProcess(op1).build();
    ib.setOwner(c1)
        .setVrf(v1)
        .setOspfSettings(
            baseOspfSettings().setAreaName(oa1a.getAreaNumber()).setPassive(true).build());
    Interface iface = ib.setName(l0Name).setAddress(C1_L0_ADDRESS).build();
    oa1a.addInterface(iface.getName());
    ib.setOspfSettings(baseOspfSettings().setProcess("1").setEnabled(false).build())
        .setName(l1Name)
        .setAddress(C1_L1_ADDRESS)
        .build();
    iface =
        ib.setOspfSettings(
                baseOspfSettings().setProcess("1").setAreaName(oa1b.getAreaNumber()).build())
            .setName(c1E1To2Name)
            .setAddress(C1_E1_2_ADDRESS)
            .build();
    oa1b.addInterface(iface.getName());

    Configuration c2 = cb.setHostname(C2_NAME).build();
    if (summarizeR1L0) {
      RouteFilterList rfl = new RouteFilterList(C1_L0_SUMMARY_FILTER_NAME);
      rfl.addLine(
          new RouteFilterLine(LineAction.DENY, PrefixRange.moreSpecificThan(C1_L0_SUMMARY_PREFIX)));
      rfl.addLine(RouteFilterLine.PERMIT_ALL);
      c2.getRouteFilterLists().put(C1_L0_SUMMARY_FILTER_NAME, rfl);
    }
    Vrf v2 = vb.setOwner(c2).build();
    RoutingPolicy c2ExportPolicy =
        rpb.setOwner(c2).setStatements(getExportPolicyStatements(C2_L1_ADDRESS)).build();
    OspfProcess op2 =
        opb.setVrf(v2)
            .setRouterId(C2_L1_ADDRESS.getIp())
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
    ib.setOwner(c2).setVrf(v2);
    iface =
        ib.setOspfSettings(
                baseOspfSettings().setPassive(true).setAreaName(oa2c.getAreaNumber()).build())
            .setName(l0Name)
            .setAddress(C2_L0_ADDRESS)
            .build();
    oa2c.addInterface(iface.getName());
    ib.setOspfSettings(baseOspfSettings().setProcess("1").setEnabled(false).build())
        .setName(l1Name)
        .setAddress(C2_L1_ADDRESS)
        .build();
    ib.setOspfSettings(
        baseOspfSettings().setProcess("1").setAreaName(oa2b.getAreaNumber()).build());
    iface = ib.setName(c2E2To1Name).setAddress(C2_E2_1_ADDRESS).build();
    oa2b.addInterface(iface.getName());
    iface =
        ib.setName(c2E2To3Name)
            .setAddress(C2_E2_3_ADDRESS)
            .setOspfSettings(
                baseOspfSettings()
                    .setAreaName(oa2d.getAreaNumber())
                    .setNetworkType(OspfNetworkType.BROADCAST)
                    .build())
            .build();
    oa2d.addInterface(iface.getName());

    Configuration c3 = cb.setHostname(C3_NAME).build();
    Vrf v3 = vb.setOwner(c3).build();
    RoutingPolicy c3ExportPolicy =
        rpb.setOwner(c3).setStatements(getExportPolicyStatements(C3_L1_ADDRESS)).build();
    OspfProcess op3 =
        opb.setVrf(v3).setRouterId(C3_L1_ADDRESS.getIp()).setExportPolicy(c3ExportPolicy).build();
    OspfArea oa3d = oabd.setOspfProcess(op3).build();
    OspfArea oa3e = areaD == areaE ? oa3d : oabe.setOspfProcess(op3).build();
    OspfArea oa3f =
        areaD == areaF ? oa3d : areaE == areaF ? oa3e : oabf.setOspfProcess(op3).build();
    iface =
        ib.setOwner(c3)
            .setVrf(v3)
            .setOspfSettings(
                baseOspfSettings().setPassive(true).setAreaName(oa3e.getAreaNumber()).build())
            .setName(l0Name)
            .setAddress(C3_L0_ADDRESS)
            .build();
    oa3e.addInterface(iface.getName());
    ib.setName(l1Name)
        .setAddress(C3_L1_ADDRESS)
        .setOspfSettings(baseOspfSettings().setEnabled(false).build())
        .build();
    iface =
        ib.setOspfSettings(
                baseOspfSettings()
                    .setNetworkType(OspfNetworkType.BROADCAST)
                    .setAreaName(oa3d.getAreaNumber())
                    .build())
            .setName(c3E3To2Name)
            .setAddress(C3_E3_2_ADDRESS)
            .build();
    oa3d.addInterface(iface.getName());
    iface =
        ib.setName(c3E3To4Name)
            .setAddress(C3_E3_4_ADDRESS)
            .setOspfSettings(
                baseOspfSettings()
                    .setNetworkType(OspfNetworkType.BROADCAST)
                    .setAreaName(oa3f.getAreaNumber())
                    .build())
            .build();
    oa3f.addInterface(iface.getName());

    Configuration c4 = cb.setHostname(C4_NAME).build();
    Vrf v4 = vb.setOwner(c4).build();
    RoutingPolicy c4ExportPolicy =
        rpb.setOwner(c4).setStatements(getExportPolicyStatements(C4_L1_ADDRESS)).build();
    OspfProcess op4 =
        opb.setVrf(v4).setExportPolicy(c4ExportPolicy).setRouterId(C4_L1_ADDRESS.getIp()).build();
    OspfArea oa4f = oabf.setOspfProcess(op4).build();
    OspfArea oa4g = areaF == areaG ? oa4f : oabg.setOspfProcess(op4).build();
    iface =
        ib.setOwner(c4)
            .setVrf(v4)
            .setOspfSettings(
                baseOspfSettings()
                    .setPassive(true)
                    .setNetworkType(OspfNetworkType.BROADCAST)
                    .setAreaName(oa4g.getAreaNumber())
                    .build())
            .setName(l0Name)
            .setAddress(C4_L0_ADDRESS)
            .build();
    oa4g.addInterface(iface.getName());
    ib.setOspfSettings(
            baseOspfSettings().setEnabled(false).setNetworkType(OspfNetworkType.BROADCAST).build())
        .setName(l1Name)
        .setAddress(C4_L1_ADDRESS)
        .build();
    iface =
        ib.setOspfSettings(
                baseOspfSettings()
                    .setNetworkType(OspfNetworkType.BROADCAST)
                    .setAreaName(oa4f.getAreaNumber())
                    .build())
            .setName(c4E4To3Name)
            .setAddress(C4_E4_3_ADDRESS)
            .build();
    oa4f.addInterface(iface.getName());

    SortedMap<String, Configuration> configurations =
        new ImmutableSortedMap.Builder<String, Configuration>(String::compareTo)
            .put(c1.getHostname(), c1)
            .put(c2.getHostname(), c2)
            .put(c3.getHostname(), c3)
            .put(c4.getHostname(), c4)
            .build();
    IncrementalBdpEngine engine = new IncrementalBdpEngine(new IncrementalDataPlaneSettings());
    OspfTopologyUtils.initNeighborConfigs(NetworkConfigurations.of(configurations));
    Topology topology = TopologyUtil.synthesizeL3Topology(configurations);
    IncrementalDataPlane dp =
        (IncrementalDataPlane)
            engine.computeDataPlane(
                    configurations,
                    TopologyContext.builder()
                        .setLayer3Topology(topology)
                        .setOspfTopology(
                            computeOspfTopology(NetworkConfigurations.of(configurations), topology))
                        .build(),
                    Collections.emptySet())
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
  private static SortedMap<String, SortedMap<String, Set<AbstractRoute>>> getOspfStubBehavior(
      boolean noSummaryStub1, boolean noSummaryNssa2, OspfDefaultOriginateType nssaDefaultType) {
    String r0Name = "r0";
    String r1Name = "r1";
    String r2Name = "r2";
    String r3Name = "r3";
    String r4Name = "r4";
    String r5Name = "r5";
    String r6Name = "r6";
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
    OspfInterfaceSettings.Builder ospf = baseOspfSettings().setCost(10).setProcess("1");
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Interface.Builder ib = nf.interfaceBuilder().setOspfSettings(ospf.build()).setBandwidth(100E6);
    OspfProcess.Builder opb = nf.ospfProcessBuilder().setProcessId("1");
    OspfArea.Builder oab = nf.ospfAreaBuilder();

    // R0
    Configuration r0 = cb.setHostname(r0Name).build();
    Vrf v0 = vb.setOwner(r0).build();
    OspfProcess op0 = opb.setVrf(v0).setRouterId(Ip.parse("10.0.0.0")).build();
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
    ib.setName(i01Name)
        .setOspfSettings(ospf.setAreaName(oaR0A0.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.1.0/24"))
        .build();
    oaR0A0.addInterface(i01Name);
    // i02
    ib.setName(i02Name)
        .setOspfSettings(ospf.setAreaName(oaR0A1.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.2.0/24"))
        .build();
    oaR0A1.addInterface(i02Name);
    // i04
    ib.setName(i04Name)
        .setOspfSettings(ospf.setAreaName(oaR0A2.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.4.0/24"))
        .build();
    oaR0A2.addInterface(i04Name);
    // i06
    ib.setName(i06Name)
        .setOspfSettings(ospf.setAreaName(oaR0A3.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.6.0/24"))
        .build();
    oaR0A3.addInterface(i06Name);

    // R1
    Configuration r1 = cb.setHostname(r1Name).build();
    Vrf v1 = vb.setOwner(r1).build();
    OspfProcess op1 = opb.setVrf(v1).setRouterId(Ip.parse("10.0.1.1")).build();
    oab.setOspfProcess(op1);
    OspfArea oaR1A0 = oab.setNumber(0L).setNonStub().build();
    ib.setOwner(r1).setVrf(v1);
    // i10
    ib.setName(i10Name)
        .setOspfSettings(ospf.setAreaName(oaR1A0.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.1.1/24"))
        .build();
    oaR1A0.addInterface(i10Name);

    // R2
    Configuration r2 = cb.setHostname(r2Name).build();
    Vrf v2 = vb.setOwner(r2).build();
    OspfProcess op2 = opb.setVrf(v2).setRouterId(Ip.parse("10.0.2.2")).build();
    oab.setOspfProcess(op2);
    OspfArea oaR2A1 = oab.setNumber(1L).setStub(StubSettings.builder().build()).build();
    ib.setOwner(r2).setVrf(v2);
    // i20
    ib.setName(i20Name)
        .setOspfSettings(ospf.setAreaName(oaR2A1.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.2.2/24"))
        .build();
    oaR2A1.addInterface(i20Name);
    // i23
    ib.setName(i23Name)
        .setOspfSettings(ospf.setAreaName(oaR2A1.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.2.3.2/24"))
        .build();
    oaR2A1.addInterface(i23Name);

    // R3
    Configuration r3 = cb.setHostname(r3Name).build();
    Vrf v3 = vb.setOwner(r3).build();
    OspfProcess op3 = opb.setVrf(v3).setRouterId(Ip.parse("10.0.3.3")).build();
    oab.setOspfProcess(op3);
    OspfArea oaR3A1 = oab.setNumber(1L).setStub(StubSettings.builder().build()).build();
    ib.setOwner(r3).setVrf(v3);
    // i32
    ib.setName(i32Name)
        .setOspfSettings(ospf.setAreaName(oaR3A1.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.2.3.3/24"))
        .build();
    oaR3A1.addInterface(i32Name);

    // R4
    Configuration r4 = cb.setHostname(r4Name).build();
    Vrf v4 = vb.setOwner(r4).build();
    OspfProcess op4 = opb.setVrf(v4).setRouterId(Ip.parse("10.0.4.4")).build();
    oab.setOspfProcess(op4);
    OspfArea oaR4A2 = oab.setNumber(2L).setNssa(NssaSettings.builder().build()).build();
    ib.setOwner(r4).setVrf(v4);
    // i40
    ib.setName(i40Name)
        .setOspfSettings(ospf.setAreaName(oaR4A2.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.4.4/24"))
        .build();
    oaR4A2.addInterface(i40Name);
    // i45
    ib.setName(i45Name)
        .setOspfSettings(ospf.setAreaName(oaR4A2.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.4.5.4/24"))
        .build();
    oaR4A2.addInterface(i45Name);

    // R5
    Configuration r5 = cb.setHostname(r5Name).build();
    Vrf v5 = vb.setOwner(r5).build();
    OspfProcess op5 = opb.setVrf(v5).setRouterId(Ip.parse("10.0.5.5")).build();
    oab.setOspfProcess(op5);
    OspfArea oaR5A2 = oab.setNumber(2L).setNssa(NssaSettings.builder().build()).build();
    ib.setOwner(r5).setVrf(v5);
    // i54
    ib.setName(i54Name)
        .setOspfSettings(ospf.setAreaName(oaR5A2.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.4.5.5/24"))
        .build();
    oaR5A2.addInterface(i54Name);

    // R6
    Configuration r6 = cb.setHostname(r6Name).build();
    Vrf v6 = vb.setOwner(r6).build();
    v6.getStaticRoutes()
        .add(
            StaticRoute.testBuilder()
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
                ImmutableList.of(
                    new If(
                        new MatchProtocol(RoutingProtocol.STATIC),
                        ImmutableList.of(
                            new SetOspfMetricType(OspfMetricType.E2),
                            new SetMetric(new LiteralLong(33L)),
                            Statements.ExitAccept.toStaticStatement()),
                        ImmutableList.of(Statements.ExitReject.toStaticStatement()))))
            .build();
    OspfProcess op6 =
        opb.setVrf(v6).setRouterId(Ip.parse("10.0.6.6")).setExportPolicy(exportStatic).build();
    oab.setOspfProcess(op6);
    OspfArea oaR6A03 = oab.setNumber(3L).setNonStub().build();
    ib.setOwner(r6).setVrf(v6);
    // i60
    ib.setName(i60Name)
        .setOspfSettings(ospf.setAreaName(oaR6A03.getAreaNumber()).build())
        .setAddress(ConcreteInterfaceAddress.parse("10.0.6.6/24"))
        .build();
    oaR6A03.addInterface(i60Name);

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.<String, Configuration>naturalOrder()
            .put(r0.getHostname(), r0)
            .put(r1.getHostname(), r1)
            .put(r2.getHostname(), r2)
            .put(r3.getHostname(), r3)
            .put(r4.getHostname(), r4)
            .put(r5.getHostname(), r5)
            .put(r6.getHostname(), r6)
            .build();
    IncrementalBdpEngine engine = new IncrementalBdpEngine(new IncrementalDataPlaneSettings());
    OspfTopologyUtils.initNeighborConfigs(NetworkConfigurations.of(configurations));
    Topology topology = TopologyUtil.synthesizeL3Topology(configurations);
    IncrementalDataPlane dp =
        (IncrementalDataPlane)
            engine.computeDataPlane(
                    configurations,
                    TopologyContext.builder()
                        .setLayer3Topology(topology)
                        .setOspfTopology(
                            computeOspfTopology(NetworkConfigurations.of(configurations), topology))
                        .build(),
                    Collections.emptySet())
                ._dataPlane;

    return IncrementalBdpEngine.getRoutes(dp);
  }

  @Test
  public void testOspfArea0MaxMetricTransit() {
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
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
            MAX_METRIC_TRANSIT_LINKS,
            false,
            false,
            null);
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
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
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
            MAX_METRIC_TRANSIT_LINKS,
            false,
            false,
            null);
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
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
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
            MAX_METRIC_TRANSIT_LINKS,
            false,
            false,
            null);
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
  public void
      testOspfDualAreaBackboneInterAreaPropagationMaxMetricTransitStubSummaryR2E21SummarizeAdvertiseFixedMetric() {
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
        getOspfRoutes(
            1L,
            1L,
            0L,
            0L,
            0L,
            0L,
            0L,
            null,
            MAX_METRIC_STUB_NETWORKS,
            MAX_METRIC_SUMMARY_NETWORKS,
            MAX_METRIC_TRANSIT_LINKS,
            true,
            true,
            100L);
    // only care about internal routes for this test
    assertNoRoute(routesByNode, C1_NAME, C1_L0_SUMMARY_PREFIX);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_ADDRESS, 16711681L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 65536L);
    // summary discard route
    assertRoute(routesByNode, OSPF_IS, C2_NAME, C1_L0_SUMMARY_PREFIX, 0L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 65536L);
    // summary prefix instead of interface prefix, with max-metric from R2
    // TODO: verify MAX_METRIC_SUMMARY_NETWORKS overrides fixed cost for R1L0 summary
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_SUMMARY_PREFIX, 16711681L);
    // suppressed by summary
    assertNoRoute(routesByNode, C3_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 65536L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 16711681L);
    // summary prefix instead of interface prefix
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_SUMMARY_PREFIX, 16711682L);
    // suppressed by summary
    assertNoRoute(routesByNode, C4_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 65537L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_ADDRESS, 16711682L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
  }

  @Test
  public void testOspfDualAreaBackboneInterAreaPropagationR2E21SummarizeAdvertiseDynamicMetric() {
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
        getOspfRoutes(1L, 1L, 0L, 0L, 0L, 0L, 0L, null, null, null, null, true, true, null);
    // only care about internal routes for this test
    assertNoRoute(routesByNode, C1_NAME, C1_L0_SUMMARY_PREFIX);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 2);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_ADDRESS, 4L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 2L);
    // summary discard route
    assertRoute(routesByNode, OSPF_IS, C2_NAME, C1_L0_SUMMARY_PREFIX, 0L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 2L);
    // summary prefix instead of interface prefix, with dynamic metric inherited from contributor
    // expected cost = contributor cost + link cost = 2 + 1 = 3
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_SUMMARY_PREFIX, 3L);
    // suppressed by summary
    assertNoRoute(routesByNode, C3_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 2L);
    // summary prefix
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_SUMMARY_PREFIX, 4L);
    // suppressed by summary
    assertNoRoute(routesByNode, C4_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
  }

  @Test
  public void testOspfDualAreaBackboneInterAreaPropagationR2E21SummarizeAdvertiseFixedMetric() {
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
        getOspfRoutes(1L, 1L, 0L, 0L, 0L, 0L, 0L, null, null, null, null, true, true, 100L);
    // only care about internal routes for this test
    assertNoRoute(routesByNode, C1_NAME, C1_L0_SUMMARY_PREFIX);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 2);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_ADDRESS, 4L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 2L);
    // summary discard route
    assertRoute(routesByNode, OSPF_IS, C2_NAME, C1_L0_SUMMARY_PREFIX, 0L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 2L);
    // summary prefix instead of interface prefix, with fixed metric
    // expected cost = fixed cost + link cost = 100 + 1 = 101
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_L0_SUMMARY_PREFIX, 101L);
    // suppressed by summary
    assertNoRoute(routesByNode, C3_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 2L);
    // summary prefix
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_L0_SUMMARY_PREFIX, 102L);
    // suppressed by summary
    assertNoRoute(routesByNode, C4_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
  }

  @Test
  public void testOspfDualAreaBackboneInterAreaPropagationR2E21SummarizeNoAdvertise() {
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
        getOspfRoutes(1L, 1L, 0L, 0L, 0L, 0L, 0L, null, null, null, null, true, false, null);
    // only care about internal routes for this test
    assertNoRoute(routesByNode, C1_NAME, C1_L0_SUMMARY_PREFIX);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_L0_ADDRESS, 2);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C4_L0_ADDRESS, 4L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C2_E2_3_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C1_NAME, C3_E3_4_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C2_NAME, C1_L0_ADDRESS, 2L);
    // summary discard route
    assertRoute(routesByNode, OSPF, C2_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF, C2_NAME, C4_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C2_NAME, C3_E3_4_ADDRESS, 2L);
    // summary discard route not installed for no-advertise
    assertNoRoute(routesByNode, C2_NAME, C1_L0_SUMMARY_PREFIX);
    // summary prefix not advertised
    assertNoRoute(routesByNode, C3_NAME, C1_L0_SUMMARY_PREFIX);
    // suppressed by summary
    assertNoRoute(routesByNode, C3_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C3_NAME, C2_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF, C3_NAME, C4_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C3_NAME, C1_E1_2_ADDRESS, 2L);
    // summary prefix not advertised
    assertNoRoute(routesByNode, C4_NAME, C1_L0_SUMMARY_PREFIX);
    // suppressed by summary
    assertNoRoute(routesByNode, C4_NAME, C1_L0_ADDRESS);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_L0_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C3_L0_ADDRESS, 2L);
    assertRoute(routesByNode, OSPF_IA, C4_NAME, C1_E1_2_ADDRESS, 3L);
    assertRoute(routesByNode, OSPF, C4_NAME, C2_E2_3_ADDRESS, 2L);
  }

  @Test
  public void testOspfDualAreaNonBackboneInterAreaPropagationMaxMetricTransitStubSummary() {
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
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
            MAX_METRIC_TRANSIT_LINKS,
            false,
            false,
            null);
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
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
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
            MAX_METRIC_TRANSIT_LINKS,
            false,
            false,
            null);
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
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesByNode =
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
            MAX_METRIC_TRANSIT_LINKS,
            false,
            false,
            null);
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
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.INTER_AREA);
    assertNoRoute(routesWithSummaries, "r0", Prefix.ZERO);
    assertRoute(routesWithSummaries, OSPF, "r0", Prefix.parse("10.2.3.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF, "r0", Prefix.parse("10.4.5.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_E2, "r0", Prefix.parse("10.10.10.10/32"), 33L);
    assertNoRoute(routesWithSummaries, "r1", Prefix.ZERO);
    assertRoute(routesWithSummaries, OSPF_IA, "r1", Prefix.parse("10.0.2.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r1", Prefix.parse("10.2.3.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r1", Prefix.parse("10.0.4.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r1", Prefix.parse("10.4.5.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r1", Prefix.parse("10.0.6.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_E2, "r1", Prefix.parse("10.10.10.10/32"), 33L);
  }

  @Test
  public void testOspfStubBehaviorNssaDefaultTypes() {
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesInterArea =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.INTER_AREA);
    assertRoute(routesInterArea, OSPF_IA, "r4", Prefix.ZERO, 10L);
    assertRoute(routesInterArea, OSPF_IA, "r5", Prefix.ZERO, 20L);

    /*
     * TODO: Re-enable tests when non-IA NSSA default origination is implemented
     * https://github.com/batfish/batfish/issues/1644
     */
    /*
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesExternalType1 =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.EXTERNAL_TYPE1);
    assertRoute(routesExternalType1, OSPF_E1, "r4", Prefix.ZERO, 10L);
    assertRoute(routesExternalType1, OSPF_E1, "r5", Prefix.ZERO, 20L);

    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesExternalType2 =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.EXTERNAL_TYPE2);
    assertRoute(routesExternalType2, OSPF_E2, "r4", Prefix.ZERO, 10L);
    assertRoute(routesExternalType2, OSPF_E2, "r5", Prefix.ZERO, 10L);
    */

    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesNone =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesNone, "r4", Prefix.ZERO);
    assertNoRoute(routesNone, "r5", Prefix.ZERO);
  }

  @Test
  public void testOspfStubBehaviorNssaRoutes() {
    // stub args don't really matter
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(true, false, OspfDefaultOriginateType.NONE);
    assertRoute(routesWithSummaries, OSPF_IA, "r4", Prefix.parse("10.0.1.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r4", Prefix.parse("10.0.2.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r4", Prefix.parse("10.2.3.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r4", Prefix.parse("10.0.6.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_E2, "r4", Prefix.parse("10.10.10.10/32"), 33L);
    assertRoute(routesWithSummaries, OSPF_IA, "r5", Prefix.parse("10.0.1.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r5", Prefix.parse("10.0.2.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r5", Prefix.parse("10.2.3.0/24"), 40L);
    assertRoute(routesWithSummaries, OSPF_IA, "r5", Prefix.parse("10.0.6.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_E2, "r5", Prefix.parse("10.10.10.10/32"), 33L);

    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesWithoutSummaries =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesWithoutSummaries, "r4", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "r4", Prefix.parse("10.0.2.0/24"));
    assertNoRoute(routesWithoutSummaries, "r4", Prefix.parse("10.2.3.0/24"));
    assertNoRoute(routesWithoutSummaries, "r4", Prefix.parse("10.0.6.0/24"));
    assertRoute(routesWithSummaries, OSPF_E2, "r4", Prefix.parse("10.10.10.10/32"), 33L);
    assertNoRoute(routesWithoutSummaries, "r5", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "r5", Prefix.parse("10.0.2.0/24"));
    assertNoRoute(routesWithoutSummaries, "r5", Prefix.parse("10.2.3.0/24"));
    assertNoRoute(routesWithoutSummaries, "r5", Prefix.parse("10.0.6.0/24"));
    assertRoute(routesWithSummaries, OSPF_E2, "r5", Prefix.parse("10.10.10.10/32"), 33L);
  }

  @Test
  public void testOspfStubBehaviorRegularAreaRoutes() {
    // NSSA args don't really matter
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(true, true, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesWithSummaries, "r6", Prefix.ZERO);
    assertRoute(routesWithSummaries, OSPF_IA, "r6", Prefix.parse("10.0.1.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r6", Prefix.parse("10.0.2.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r6", Prefix.parse("10.2.3.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r6", Prefix.parse("10.0.4.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r6", Prefix.parse("10.4.5.0/24"), 30L);
  }

  @Test
  public void testOspfStubBehaviorStubRoutes() {
    // NSSA args don't really matter
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesWithSummaries =
        getOspfStubBehavior(false, false, OspfDefaultOriginateType.NONE);
    assertRoute(routesWithSummaries, OSPF_IA, "r2", Prefix.parse("10.0.1.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r2", Prefix.parse("10.0.4.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r2", Prefix.parse("10.4.5.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r2", Prefix.parse("10.0.6.0/24"), 20L);
    assertRoute(routesWithSummaries, OSPF_IA, "r2", Prefix.ZERO, 10L);
    assertNoRoute(routesWithSummaries, "r2", Prefix.parse("10.10.10.10/32"));
    assertRoute(routesWithSummaries, OSPF_IA, "r3", Prefix.parse("10.0.1.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r3", Prefix.parse("10.0.4.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r3", Prefix.parse("10.4.5.0/24"), 40L);
    assertRoute(routesWithSummaries, OSPF_IA, "r3", Prefix.parse("10.0.6.0/24"), 30L);
    assertRoute(routesWithSummaries, OSPF_IA, "r3", Prefix.ZERO, 20L);
    assertNoRoute(routesWithSummaries, "r3", Prefix.parse("10.10.10.10/32"));

    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routesWithoutSummaries =
        getOspfStubBehavior(true, false, OspfDefaultOriginateType.NONE);
    assertNoRoute(routesWithoutSummaries, "r2", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "r2", Prefix.parse("10.0.4.0/24"));
    assertNoRoute(routesWithoutSummaries, "r2", Prefix.parse("10.4.5.0/24"));
    assertNoRoute(routesWithoutSummaries, "r2", Prefix.parse("10.0.6.0/24"));
    assertNoRoute(routesWithSummaries, "r2", Prefix.parse("10.10.10.10/32"));
    assertNoRoute(routesWithoutSummaries, "r3", Prefix.parse("10.0.1.0/24"));
    assertNoRoute(routesWithoutSummaries, "r3", Prefix.parse("10.0.4.0/24"));
    assertNoRoute(routesWithoutSummaries, "r3", Prefix.parse("10.4.5.0/24"));
    assertNoRoute(routesWithoutSummaries, "r3", Prefix.parse("10.0.6.0/24"));
    assertNoRoute(routesWithSummaries, "r3", Prefix.parse("10.10.10.10/32"));
  }

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Test
  public void testOspfWithMultipleEdgesInSameIface() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles("org/batfish/dataplane/ibdp/ospf-edge", "A1", "A2", "FWL")
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    IncrementalDataPlane dataplane =
        (IncrementalDataPlane) batfish.loadDataPlane(batfish.getSnapshot());
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routes =
        IncrementalBdpEngine.getRoutes(dataplane);
    assertRoute(routes, OSPF_E2, "a1", Prefix.ZERO, 1, Ip.parse("10.1.1.4"));
    assertRoute(routes, OSPF_E2, "a2", Prefix.ZERO, 1, Ip.parse("10.1.1.4"));
    assertRoute(routes, OSPF, "fwl", Prefix.parse("11.1.1.0/31"), 2, Ip.parse("10.1.1.1"));
    assertRoute(routes, OSPF, "fwl", Prefix.parse("11.1.1.0/31"), 2, Ip.parse("10.1.1.2"));
  }

  @Test
  public void testOspfDistributeList() throws IOException {
    // the network will result in two routes of each OSPF type on r2 and r3, and we will block one
    // of them by using distribute-list
    // r2 will have ospf, ospf e1 and ospf e2. r3 will have ospf IA
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    "org/batfish/dataplane/ibdp/ospf-distribute-lists", "r1", "r2", "r3")
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    IncrementalDataPlane dataplane =
        (IncrementalDataPlane) batfish.loadDataPlane(batfish.getSnapshot());
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routes =
        IncrementalBdpEngine.getRoutes(dataplane);

    // routes of each type not matched by the distribute-list's prefix-list are allowed
    assertRoute(routes, OSPF, "r2", Prefix.parse("2.2.2.0/24"), 11, Ip.parse("192.168.12.1"));
    assertRoute(routes, OSPF_IA, "r3", Prefix.parse("1.1.1.0/24"), 12, Ip.parse("192.168.13.1"));
    assertRoute(
        routes, OSPF_E1, "r2", Prefix.parse("192.168.16.0/24"), 21, Ip.parse("192.168.12.1"));
    assertRoute(
        routes, OSPF_E2, "r2", Prefix.parse("192.168.9.0/24"), 20, Ip.parse("192.168.13.2"));

    // routes of each type matched by the distribute-list's prefix-list are filtered
    assertNoRoute(routes, "r2", Prefix.parse("1.1.1.0/24"));
    assertNoRoute(routes, "r2", Prefix.parse("192.168.15.0/24"));
    assertNoRoute(routes, "r2", Prefix.parse("192.168.10.0/24"));
    assertNoRoute(routes, "r3", Prefix.parse("2.2.2.0/24"));
  }

  @Test
  public void testLoopbackRoutes() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    "org/batfish/dataplane/ibdp/ospf-loopback-routes", "advertiser", "listener")
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    IncrementalDataPlane dataplane =
        (IncrementalDataPlane) batfish.loadDataPlane(batfish.getSnapshot());
    SortedMap<String, SortedMap<String, Set<AbstractRoute>>> routes =
        IncrementalBdpEngine.getRoutes(dataplane);

    // expecting a /32 route to the loopback's address in addition to route generated using the
    // actual mask length
    assertRoute(
        routes, OSPF, "listener", Prefix.parse("192.168.61.4/32"), 11, Ip.parse("14.2.0.2"));
    assertNoRoute(routes, "listener", Prefix.parse("192.168.61.0/24"));
  }
}
