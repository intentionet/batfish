package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.isEvpnType3RouteThat;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportIps;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig.Builder;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests with small networks with BGP EVPN peerings */
public class EvpnTest {
  static final int SUBNET_LEN = 24;
  private static String policyName;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private static SortedMap<String, Configuration> twoNodeNetwork() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("n1");

    Configuration c1 = cb.build();
    Configuration c2 = cb.setHostname("n2").build();

    policyName = "EXPORT_ALL";
    RoutingPolicy.Builder policyBuilder =
        RoutingPolicy.builder()
            .setName(policyName)
            .addStatement(Statements.ReturnTrue.toStaticStatement());
    policyBuilder.setOwner(c1).build();
    policyBuilder.setOwner(c2).build();

    Ip ipNode1 = Ip.parse("1.1.1.1");
    Ip ipNode2 = Ip.parse("1.1.1.2");

    Vrf vrf1 = nf.vrfBuilder().setOwner(c1).setName(DEFAULT_VRF_NAME).build();
    Vrf vrf2 = nf.vrfBuilder().setOwner(c2).setName(DEFAULT_VRF_NAME).build();
    BgpProcess bgpProcess1 =
        nf.bgpProcessBuilder()
            .setRouterId(ipNode1)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    BgpProcess bgpProcess2 =
        nf.bgpProcessBuilder()
            .setRouterId(ipNode2)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    vrf1.setBgpProcess(bgpProcess1);
    vrf2.setBgpProcess(bgpProcess2);

    nf.interfaceBuilder()
        .setOwner(c1)
        .setVrf(vrf1)
        .setAddress(ConcreteInterfaceAddress.create(ipNode1, SUBNET_LEN))
        .build();
    nf.interfaceBuilder()
        .setOwner(c2)
        .setVrf(vrf2)
        .setAddress(ConcreteInterfaceAddress.create(ipNode2, SUBNET_LEN))
        .build();

    int vni = 10001;
    Ip vniIp1 = Ip.parse("1.111.111.111");
    Ip vniIp2 = Ip.parse("2.222.222.222");
    vrf1.setVniSettings(
        ImmutableSortedMap.of(
            vni,
            VniSettings.builder()
                .setVni(vni)
                .setVlan(1)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setSourceAddress(vniIp1)
                .build()));
    vrf2.setVniSettings(
        ImmutableSortedMap.of(
            vni,
            VniSettings.builder()
                .setVni(vni)
                .setVlan(1)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setSourceAddress(vniIp2)
                .build()));

    Builder vniConfigBuilder =
        Layer3VniConfig.builder()
            .setVni(vni)
            .setVrf(DEFAULT_VRF_NAME)
            .setRouteDistinguisher(RouteDistinguisher.from(bgpProcess1.getRouterId(), 1))
            .setRouteTarget(ExtendedCommunity.target(65500, vni));
    Layer3VniConfig vniConfig1 = vniConfigBuilder.build();
    Layer3VniConfig vniConfig2 =
        vniConfigBuilder
            .setVni(vni)
            .setRouteDistinguisher(RouteDistinguisher.from(bgpProcess2.getRouterId(), 2))
            .build();
    nf.bgpNeighborBuilder()
        .setSendCommunity(true)
        .setPeerAddress(ipNode2)
        .setRemoteAs(2L)
        .setLocalIp(ipNode1)
        .setLocalAs(1L)
        .setBgpProcess(bgpProcess1)
        .setEvpnAddressFamily(new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of(vniConfig1)))
        .setExportPolicy(policyName)
        .build();
    nf.bgpNeighborBuilder()
        .setSendCommunity(true)
        .setPeerAddress(ipNode1)
        .setRemoteAs(1L)
        .setLocalIp(ipNode2)
        .setLocalAs(2L)
        .setBgpProcess(bgpProcess2)
        .setEvpnAddressFamily(new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of(vniConfig2)))
        .setExportPolicy(policyName)
        .build();

    return ImmutableSortedMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
  }

  @Test
  public void testEbgpL3Vni() throws IOException {
    Batfish batfish = BatfishTestUtils.getBatfish(twoNodeNetwork(), _folder);
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();

    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        dp.getRibs();
    Set<AbstractRoute> n1Routes = ribs.get("n1").get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> n2Routes = ribs.get("n2").get(DEFAULT_VRF_NAME).getRoutes();
    // Ensure routes are present in the main RIB
    assertThat(
        n1Routes, hasItem(isEvpnType3RouteThat(hasPrefix(Prefix.parse("2.222.222.222/32")))));
    assertThat(
        n2Routes, hasItem(isEvpnType3RouteThat(hasPrefix(Prefix.parse("1.111.111.111/32")))));
    // Ensure VNI flood lists were updated with peer's VTEP address
    assertThat(
        dp.getVniSettings().column(DEFAULT_VRF_NAME),
        hasEntry(equalTo("n1"), contains(hasBumTransportIps(contains(Ip.parse("2.222.222.222"))))));
    assertThat(
        dp.getVniSettings().column(DEFAULT_VRF_NAME),
        hasEntry(equalTo("n2"), contains(hasBumTransportIps(contains(Ip.parse("1.111.111.111"))))));
  }
}
