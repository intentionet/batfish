package org.batfish.dataplane;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.dataplane.rib.RibGroup;
import org.batfish.datamodel.dataplane.rib.RibId;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetAdministrativeCost;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Test of RIB groups applied to BGP neighbors */
public class BgpRibGroupsTest {
  private static final String EXPORT_STATIC = "EXPORT_STATIC";
  private static final String RG_POLICY_NAME = "RG_IMPORT_POLICY";
  private static final String VRF_2 = "vrf2";
  private static final int ADMIN_OVERWRITE = 123;
  private static final String EXPORT_ALL = "EXPORT_ALL";

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  /*

  * EBGP on all nodes, 2 peers setup at r1
  * r1 has "vrf2", without any BGP process running.
  * r1 will leak route from default VRF2 to VRF2 using a rib group applied to the peering with R2 only
  * r2 & r3 will both advertise a different route into bgp
  * r2's static route should be in the VRF2's RIB
  * r3's static route should NOT be in the VRF2's RIB

                                     .3/31
                                   +----------+
              .2/31                |          |
               +-------------------+    r2    |
               |                   |          |
          +----+-----+             +----------+
          |          |
          |    r1    |
          |          |
          +----+-----+             +----------+
               |                   |          |
               +-------------------+    r3    |
              .4/31                |          |
                                   +----------+
                                    .5/31


   */
  private static SortedMap<String, Configuration> makeTestNetwork() {

    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.JUNIPER);

    Configuration c1 = cb.setHostname("r1").build();
    Configuration c2 = cb.setHostname("r2").build();
    Configuration c3 = cb.setHostname("r3").build();

    // Rib group import policy on R1:
    nf.routingPolicyBuilder()
        .setName(RG_POLICY_NAME)
        .setStatements(
            Collections.singletonList(
                new If(
                    new MatchProtocol(RoutingProtocol.BGP),
                    ImmutableList.of(
                        new SetAdministrativeCost(new LiteralInt(ADMIN_OVERWRITE)),
                        Statements.ReturnTrue.toStaticStatement()))))
        .setOwner(c1)
        .build();

    // Export static policies on r2 & r3
    RoutingPolicy.Builder rpb =
        nf.routingPolicyBuilder()
            .setName(EXPORT_STATIC)
            .setStatements(
                Collections.singletonList(
                    new If(
                        new MatchProtocol(RoutingProtocol.STATIC),
                        ImmutableList.of(
                            new SetOrigin(new LiteralOrigin(OriginType.IGP, null)),
                            Statements.ReturnTrue.toStaticStatement()))));
    rpb.setOwner(c2).build();
    rpb.setOwner(c3).build();

    // export policy (set origin type, allow) for all nodes
    nf.routingPolicyBuilder()
        .setName(EXPORT_ALL)
        .setStatements(
            ImmutableList.of(
                new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                Statements.ReturnTrue.toStaticStatement()))
        .setOwner(c1)
        .build();
    nf.routingPolicyBuilder()
        .setName(EXPORT_ALL)
        .setStatements(
            ImmutableList.of(
                new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                Statements.ReturnTrue.toStaticStatement()))
        .setOwner(c2)
        .build();
    nf.routingPolicyBuilder()
        .setName(EXPORT_ALL)
        .setStatements(
            ImmutableList.of(
                new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                Statements.ReturnTrue.toStaticStatement()))
        .setOwner(c3)
        .build();

    // R1
    Vrf c1v1 = nf.vrfBuilder().setOwner(c1).setName(Configuration.DEFAULT_VRF_NAME).build();
    nf.vrfBuilder().setOwner(c1).setName(VRF_2).build();

    nf.interfaceBuilder()
        .setAddress(new InterfaceAddress("1.1.1.2/31"))
        .setOwner(c1)
        .setVrf(c1v1)
        .build();
    nf.interfaceBuilder()
        .setAddress(new InterfaceAddress("1.1.1.4/31"))
        .setOwner(c1)
        .setVrf(c1v1)
        .build();

    BgpProcess bgpProc1 =
        nf.bgpProcessBuilder().setVrf(c1v1).setRouterId(Ip.parse("1.1.1.1")).build();
    RibGroup rg =
        new RibGroup(
            "RIB_GROUP",
            ImmutableList.of(new RibId("r1", VRF_2, RibId.DEFAULT_RIB_NAME)),
            RG_POLICY_NAME,
            null);
    nf.bgpNeighborBuilder()
        .setBgpProcess(bgpProc1)
        .setLocalAs(1L)
        .setRemoteAs(2L)
        .setPeerAddress(Ip.parse("1.1.1.3"))
        .setLocalIp(Ip.parse("1.1.1.2"))
        .setAppliedRibGroup(rg)
        .setExportPolicy(EXPORT_ALL)
        .build();
    nf.bgpNeighborBuilder()
        .setBgpProcess(bgpProc1)
        .setLocalAs(1L)
        .setRemoteAs(3L)
        .setPeerAddress(Ip.parse("1.1.1.5"))
        .setLocalIp(Ip.parse("1.1.1.4"))
        .build();

    // R2
    Vrf v2 = nf.vrfBuilder().setOwner(c2).setName(Configuration.DEFAULT_VRF_NAME).build();
    v2.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("2.2.2.0/24"))
                .setAdministrativeCost(1)
                .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                .build()));

    nf.interfaceBuilder()
        .setAddress(new InterfaceAddress("1.1.1.3/31"))
        .setOwner(c2)
        .setVrf(v2)
        .build();

    BgpProcess bgpProc2 =
        nf.bgpProcessBuilder().setVrf(v2).setRouterId(Ip.parse("2.2.2.2")).build();
    nf.bgpNeighborBuilder()
        .setBgpProcess(bgpProc2)
        .setLocalAs(2L)
        .setRemoteAs(1L)
        .setPeerAddress(Ip.parse("1.1.1.2"))
        .setLocalIp(Ip.parse("1.1.1.3"))
        .setExportPolicy(EXPORT_STATIC)
        .build();

    // R3
    Vrf v3 = nf.vrfBuilder().setOwner(c3).setName(Configuration.DEFAULT_VRF_NAME).build();
    v3.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("3.3.3.0/24"))
                .setAdministrativeCost(1)
                .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                .build()));

    nf.interfaceBuilder()
        .setAddress(new InterfaceAddress("1.1.1.5/31"))
        .setOwner(c2)
        .setVrf(v2)
        .build();

    BgpProcess bgpProc3 =
        nf.bgpProcessBuilder().setVrf(v3).setRouterId(Ip.parse("3.3.3.3")).build();
    nf.bgpNeighborBuilder()
        .setBgpProcess(bgpProc3)
        .setLocalAs(3L)
        .setRemoteAs(1L)
        .setPeerAddress(Ip.parse("1.1.1.4"))
        .setLocalIp(Ip.parse("1.1.1.5"))
        .setExportPolicy(EXPORT_STATIC)
        .build();

    return ImmutableSortedMap.of("r1", c1, "r2", c2, "r3", c3);
  }

  @Test
  public void testBgpRibGroupAcrossVrfs() throws IOException {
    SortedMap<String, Configuration> configs = makeTestNetwork();

    Batfish batfish = BatfishTestUtils.getBatfish(configs, folder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    Set<AbstractRoute> vrf2Routes = dp.getRibs().get("r1").get(VRF_2).getRoutes();
    assertThat(
        vrf2Routes,
        contains(
            BgpRoute.builder()
                .setNetwork(Prefix.parse("2.2.2.0/24"))
                .setAdmin(ADMIN_OVERWRITE)
                .setAsPath(AsPath.ofSingletonAsSets(2L))
                .setOriginatorIp(Ip.parse("2.2.2.2"))
                .setOriginType(OriginType.IGP)
                .setProtocol(RoutingProtocol.BGP)
                .setLocalPreference(100)
                .setReceivedFromIp(Ip.parse("1.1.1.3"))
                .setNextHopIp(Ip.parse("1.1.1.3"))
                .setSrcProtocol(RoutingProtocol.BGP)
                .build()));
  }
}
