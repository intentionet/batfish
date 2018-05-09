package org.batfish.dataplane;

import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasPrefix;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for activation conditions of static routes with next hop IP in bdp/ibdp */
@RunWith(Parameterized.class)
public class StaticRouteActivationTest {

  private Configuration _n1;
  private Interface _i1;
  private Vrf _vrf;
  private static final int INTERFACE_BITS = 24;
  private final Ip _interfaceIp = new Ip("1.1.1.1");
  private final Ip _baseNextHopIp = new Ip("1.1.1.2");

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"bdp"}, {"ibdp"}});
  }

  @Parameter public String dpEngine;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb = nf.configurationBuilder();
    Vrf.Builder vb = nf.vrfBuilder();
    Interface.Builder ib = nf.interfaceBuilder();
    _n1 = cb.setHostname("n1").setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    _vrf = vb.setOwner(_n1).setName(Configuration.DEFAULT_VRF_NAME).build();
    _i1 =
        ib.setVrf(_vrf)
            .setName("Eth1")
            .setOwner(_n1)
            .setActive(true)
            .setAddress(new InterfaceAddress(_interfaceIp, INTERFACE_BITS))
            .build();
  }

  @Test
  public void testCoveredStaticRouteIsInstalled() throws IOException {
    Ip networkIp = new Ip("1.1.1.0");
    _vrf.setStaticRoutes(
        ImmutableSortedSet.of(
            new StaticRoute(new Prefix(networkIp, 27), _baseNextHopIp, _i1.getName(), 1, 1),
            new StaticRoute(new Prefix(networkIp, 29), _baseNextHopIp, _i1.getName(), 1, 1)));

    ImmutableSortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(_n1.getHostname(), _n1);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);

    batfish.getSettings().setDataplaneEngineName(dpEngine);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    /*
     * Connected route covers both static routes, and one static route completely covers the other.
     * All three should be in the rib.
     */
    Set<AbstractRoute> routes =
        dp.getRibs().get(_n1.getHostname()).get(Configuration.DEFAULT_VRF_NAME).getRoutes();

    assertThat(routes, hasItem(hasPrefix(new Prefix(_interfaceIp, INTERFACE_BITS))));
    assertThat(routes, hasItem(hasPrefix(new Prefix(networkIp, 27))));
    assertThat(routes, hasItem(hasPrefix(new Prefix(networkIp, 29))));
  }

  @Test
  public void testStaticRouteFlatteningDirect() throws IOException {
    Ip networkIp = new Ip("2.2.2.0");
    final int ROUTE_PREFIX_LENGTH = 24;
    Ip intermediateNextHopIp = new Ip("2.2.2.2");
    _vrf.setStaticRoutes(
        ImmutableSortedSet.of(
            new StaticRoute(
                new Prefix(networkIp, ROUTE_PREFIX_LENGTH),
                intermediateNextHopIp,
                _i1.getName(),
                1,
                1),
            new StaticRoute(
                new Prefix(networkIp, ROUTE_PREFIX_LENGTH), _baseNextHopIp, _i1.getName(), 1, 1)));

    ImmutableSortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(_n1.getHostname(), _n1);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);

    batfish.getSettings().setDataplaneEngineName(dpEngine);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    Set<AbstractRoute> routes =
        dp.getRibs().get(_n1.getHostname()).get(Configuration.DEFAULT_VRF_NAME).getRoutes();

    /*
     * Ensure that route with intermediate next hop is not installed, as it "collapses"
     * to baseNextHop
     */
    assertThat(routes, hasItem(hasPrefix(new Prefix(_interfaceIp, INTERFACE_BITS))));
    assertThat(
        routes,
        hasItem(
            allOf(
                hasPrefix(new Prefix(networkIp, ROUTE_PREFIX_LENGTH)),
                hasNextHopIp(_baseNextHopIp))));
    assertThat(routes, not(hasItem(hasNextHopIp(intermediateNextHopIp))));
  }

  @Test
  public void testStaticRouteLoop() throws IOException {
    Ip networkIp = new Ip("2.2.2.0");
    final int ROUTE_PREFIX_LENGTH = 24;
    Ip intermediateNextHopIp = new Ip("3.3.3.3");
    _vrf.setStaticRoutes(
        ImmutableSortedSet.of(
            new StaticRoute(
                new Prefix(networkIp, ROUTE_PREFIX_LENGTH),
                intermediateNextHopIp,
                _i1.getName(),
                1,
                1),
            new StaticRoute(
                new Prefix(networkIp, ROUTE_PREFIX_LENGTH), _baseNextHopIp, _i1.getName(), 1, 1),
            new StaticRoute(
                new Prefix(intermediateNextHopIp, Prefix.MAX_PREFIX_LENGTH),
                // Creates a loop of static routes
                new Ip("2.2.2.2"),
                Route.UNSET_NEXT_HOP_INTERFACE,
                1,
                1)));

    ImmutableSortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(_n1.getHostname(), _n1);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);

    batfish.getSettings().setDataplaneEngineName(dpEngine);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    /*
     * Ensure all (even looped) routes get installed
     */
    Set<AbstractRoute> routes =
        dp.getRibs().get(_n1.getHostname()).get(Configuration.DEFAULT_VRF_NAME).getRoutes();

    assertThat(routes, hasItem(hasPrefix(new Prefix(_interfaceIp, INTERFACE_BITS))));
    assertThat(
        routes,
        hasItem(
            allOf(
                hasPrefix(new Prefix(networkIp, ROUTE_PREFIX_LENGTH)),
                hasNextHopIp(_baseNextHopIp))));

    assertThat(
        routes,
        hasItem(
            allOf(
                hasPrefix(new Prefix(networkIp, ROUTE_PREFIX_LENGTH)),
                hasNextHopIp(intermediateNextHopIp))));
    assertThat(
        routes, hasItem(hasPrefix(new Prefix(intermediateNextHopIp, Prefix.MAX_PREFIX_LENGTH))));
  }
}
