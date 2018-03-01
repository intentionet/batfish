package org.batfish.symbolic.abstraction;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.batfish.bdp.BdpDataPlanePlugin;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BatfishCompressionTest {

  private SortedMap<String, Configuration> simpleNetwork() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    Configuration c3 = cb.build();
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Vrf v1 = vb.setOwner(c1).build();
    Vrf v2 = vb.setOwner(c2).build();
    Vrf v3 = vb.setOwner(c3).build();
    Prefix p12 = Prefix.parse("10.12.0.0/31");
    Prefix p23 = Prefix.parse("10.23.0.0/31");
    Interface.Builder ib = nf.interfaceBuilder().setActive(true);
    ib.setOwner(c1)
        .setVrf(v1)
        .setAddress(new InterfaceAddress(p12.getStartIp(), p12.getPrefixLength()))
        .build();
    ib.setOwner(c2)
        .setVrf(v2)
        .setAddress(new InterfaceAddress(p12.getEndIp(), p12.getPrefixLength()))
        .build();
    ib.setAddress(new InterfaceAddress(p23.getStartIp(), p23.getPrefixLength())).build();
    ib.setOwner(c3)
        .setVrf(v3)
        .setAddress(new InterfaceAddress(p23.getEndIp(), p23.getPrefixLength()))
        .build();
    StaticRoute s13 = StaticRoute.builder().setNetwork(p23).setNextHopIp(p12.getEndIp()).build();
    v1.getStaticRoutes().add(s13);
    StaticRoute s31 = StaticRoute.builder().setNetwork(p12).setNextHopIp(p23.getStartIp()).build();
    v3.getStaticRoutes().add(s31);

    return new TreeMap<>(
        ImmutableSortedMap.of(c1.getName(), c1, c2.getName(), c2, c3.getName(), c3));
  }

  private static void assertIsCompressedConfig(Configuration config) {
    config
        .getRoutingPolicies()
        .values()
        .forEach(
            p -> {
              assertEquals(p.getStatements().size(), 1);
              assertThat(p.getStatements().get(0), instanceOf(If.class));
              If i = (If) p.getStatements().get(0);
              assertNotNull(i.getGuard());
            });
  }

  private DataPlane getDataPlane(SortedMap<String, Configuration> configs) throws IOException {
    // make sure to reconstruct the network, since compression mutates it
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, tmp);
    BdpDataPlanePlugin bdpDataPlanePlugin = new BdpDataPlanePlugin();
    bdpDataPlanePlugin.initialize(batfish);
    batfish.registerDataPlanePlugin(bdpDataPlanePlugin, "bdp");
    batfish.computeDataPlane(false);
    return batfish.loadDataPlane();
  }

  private SortedMap<String, Configuration> compressNetwork(
      Map<String, Configuration> configs, HeaderSpace headerSpace) throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    IBatfish batfish = BatfishTestUtils.getBatfish(new TreeMap<>(configs), tmp);
    return new TreeMap<>(new BatfishCompressor(batfish, configs).compress(headerSpace));
  }

  /** Test that compression doesn't change the fibs for this network. */
  @Test
  public void testCompressionFibs_simpleNetwork() throws IOException {
    DataPlane origDataPlane = getDataPlane(simpleNetwork());
    Map<String, Map<String, SortedSet<FibRow>>> origFibs = origDataPlane.getFibRows();
    SortedMap<String, Configuration> compressedConfigs =
        compressNetwork(simpleNetwork(), new HeaderSpace());
    DataPlane compressedDataPlane = getDataPlane(compressedConfigs);
    Map<String, Map<String, SortedSet<FibRow>>> compressedFibs = compressedDataPlane.getFibRows();

    compressedConfigs.values().forEach(BatfishCompressionTest::assertIsCompressedConfig);
    assertThat(origFibs.keySet(), equalTo(compressedFibs.keySet()));
    assertEquals(origFibs, compressedFibs);

    origFibs.forEach(
        (router, value) ->
            value.forEach(
                (vrf, rows) -> {
                  assertEquals(rows, compressedFibs.get(router).get(vrf));
                }));
  }

  /** Build a network that can be easily compressed. */
  private SortedMap<String, Configuration> compressibleNetwork() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    Configuration c3 = cb.build();
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Vrf v1 = vb.setOwner(c1).build();
    Vrf v3 = vb.setOwner(c3).build();
    vb.setOwner(c2).build(); // add a vrf to c2 too
    Prefix p = Prefix.parse("10.23.0.0/31");
    Interface.Builder ib = nf.interfaceBuilder().setActive(true);
    ib.setOwner(c1)
        .setVrf(v1)
        .setAddress(new InterfaceAddress(p.getStartIp(), p.getPrefixLength()))
        .build();

    ib.setOwner(c3)
        .setVrf(v3)
        .setAddress(new InterfaceAddress(p.getStartIp(), p.getPrefixLength()))
        .build();

    StaticRoute staticRoute =
        StaticRoute.builder().setNetwork(p).setNextHopIp(p.getEndIp()).build();
    v1.getStaticRoutes().add(staticRoute);
    v3.getStaticRoutes().add(staticRoute);

    return new TreeMap<>(
        ImmutableSortedMap.of(c1.getName(), c1, c2.getName(), c2, c3.getName(), c3));
  }

  /**
   * Test the following invariant: if a FIB appears on concrete router “r”, then a corresponding
   * abstract FIB appears on one of these representatives. For example, if there is a concrete FIB
   * from C to D, then there should be an abstract FIB from A to B, where A is in representatives(C)
   * and B is in representatives(D).
   */
  @Test
  public void testCompressionFibs_compressibleNetwork() throws IOException {
    DataPlane origDataPlane = getDataPlane(compressibleNetwork());
    Map<String, Map<String, SortedSet<FibRow>>> origFibs = origDataPlane.getFibRows();
    SortedMap<String, Configuration> compressedConfigs =
        compressNetwork(compressibleNetwork(), new HeaderSpace());
    DataPlane compressedDataPlane = getDataPlane(compressedConfigs);
    Map<String, Map<String, SortedSet<FibRow>>> compressedFibs = compressedDataPlane.getFibRows();

    compressedConfigs.values().forEach(BatfishCompressionTest::assertIsCompressedConfig);

    // compressedFibs is a strict subset of origFibs
    assertThat(compressedFibs.keySet(), everyItem(isIn(origFibs.keySet())));
    assertThat(compressedFibs, not(equalTo(origFibs)));

    // all FIB entries in compressed network should also exist in the original network
    compressedFibs.forEach(
        (router, value) ->
            value.forEach((vrf, rows) -> assertEquals(rows, origFibs.get(router).get(vrf))));
  }

  /**
   * This network should be compressed from: A --> B --> D, A --> C --> D to A --> {B,C} --> D.
   * i.e., B and C should be merged into one node.
   *
   * @return Configurations for the original (uncompressed) network.
   */
  private SortedMap<String, Configuration> diamondNetwork() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration cA = cb.setHostname("A").build();
    Configuration cB = cb.setHostname("B").build();
    Configuration cC = cb.setHostname("C").build();
    Configuration cD = cb.setHostname("D").build();

    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Vrf vA = vb.setOwner(cA).build();
    Vrf vB = vb.setOwner(cB).build();
    Vrf vC = vb.setOwner(cC).build();
    Vrf vD = vb.setOwner(cD).build();
    Prefix pAB = Prefix.parse("10.12.0.0/31");
    Prefix pAC = Prefix.parse("10.13.0.0/31");
    Prefix pBD = Prefix.parse("10.24.0.0/31");
    Prefix pCD = Prefix.parse("10.34.0.0/31");
    Interface.Builder ib = nf.interfaceBuilder().setActive(true);

    // Add a route from A --> B
    ib.setOwner(cA)
        .setVrf(vA)
        .setAddress(new InterfaceAddress(pAB.getStartIp(), pAB.getPrefixLength()))
        .build();
    // Interface iBA
    ib.setOwner(cB)
        .setVrf(vA)
        .setAddress(new InterfaceAddress(pAB.getEndIp(), pAB.getPrefixLength()))
        .build();
    // Interface iAC
    ib.setOwner(cA)
        .setVrf(vA)
        .setAddress(new InterfaceAddress(pAC.getStartIp(), pAC.getPrefixLength()))
        .build();
    // Interface iCA
    ib.setOwner(cC)
        .setVrf(vC)
        .setAddress(new InterfaceAddress(pAC.getEndIp(), pAC.getPrefixLength()))
        .build();
    // Interface iBD
    ib.setOwner(cB)
        .setVrf(vB)
        .setAddress(new InterfaceAddress(pBD.getStartIp(), pBD.getPrefixLength()))
        .build();
    // Interface iDB
    ib.setOwner(cD)
        .setVrf(vD)
        .setAddress(new InterfaceAddress(pBD.getEndIp(), pBD.getPrefixLength()))
        .build();
    // Interface iCD
    ib.setOwner(cC)
        .setVrf(vC)
        .setAddress(new InterfaceAddress(pCD.getStartIp(), pCD.getPrefixLength()))
        .build();
    // Interface iDC
    ib.setOwner(cD)
        .setVrf(vD)
        .setAddress(new InterfaceAddress(pCD.getEndIp(), pCD.getPrefixLength()))
        .build();

    // For the destination
    Prefix pD = Prefix.parse("4.4.4.4/32");
    // Interface iD
    ib.setOwner(cD)
        .setVrf(vD)
        .setAddress(new InterfaceAddress(pD.getEndIp(), pD.getPrefixLength()))
        .build();

    StaticRoute.Builder bld = StaticRoute.builder().setNetwork(pD);
    vA.getStaticRoutes().add(bld.setNextHopIp(pAB.getEndIp()).build());
    vA.getStaticRoutes().add(bld.setNextHopIp(pAC.getEndIp()).build());
    vB.getStaticRoutes().add(bld.setNextHopIp(pBD.getEndIp()).build());
    vC.getStaticRoutes().add(bld.setNextHopIp(pCD.getEndIp()).build());

    return new TreeMap<>(
        ImmutableSortedMap.of(
            cA.getName(), cA,
            cB.getName(), cB,
            cC.getName(), cC,
            cD.getName(), cD));
  }

  /**
   * Test the following invariant: if a FIB appears on concrete router “r”, then a corresponding
   * abstract FIB appears on one of these representatives. For example, if there is a concrete FIB
   * from C to D, then there should be an abstract FIB from A to B, where A is in representatives(C)
   * and B is in representatives(D).
   */
  @Test
  public void testCompressionFibs_diamondNetwork() throws IOException {
    IpAccessListLine line = new IpAccessListLine();
    line.setDstIps(ImmutableList.of(new IpWildcard(Prefix.parse("4.4.4.4/32"))));
    SortedMap<String, Configuration> origConfigs = diamondNetwork();
    DataPlane origDataPlane = getDataPlane(origConfigs);
    Map<String, Map<String, SortedSet<FibRow>>> origFibs = origDataPlane.getFibRows();

    assertThat(
        origFibs
            .get("A")
            .get("default")
            .stream()
            .filter(fibRow -> fibRow.getNextHop().equals("C"))
            .collect(Collectors.toSet()),
        hasSize(greaterThan(0)));

    // compress a new copy since it will get mutated.
    SortedMap<String, Configuration> compressedConfigs =
        new TreeMap<>(compressNetwork(diamondNetwork(), line));
    DataPlane compressedDataPlane = getDataPlane(compressedConfigs);
    Map<String, Map<String, SortedSet<FibRow>>> compressedFibs = compressedDataPlane.getFibRows();

    compressedConfigs.values().forEach(BatfishCompressionTest::assertIsCompressedConfig);

    assertThat(compressedConfigs.values(), hasSize(3));

    // compressedFibs is a strict subset of origFibs
    assertThat(compressedFibs.keySet(), everyItem(isIn(origFibs.keySet())));

    // compression removed B or C entirely (but not both)
    assertThat(compressedFibs, either(not(hasKey("B"))).or(not(hasKey("C"))));
    assertThat(compressedFibs, either(hasKey("B")).or(hasKey("C")));

    String remains = compressedConfigs.containsKey("B") ? "B" : "C";

    // A and D lose their connections to the removed node.
    assertThat(compressedFibs.get("A"), not(equalTo(origFibs.get("A"))));
    assertThat(compressedFibs.get("D"), not(equalTo(origFibs.get("D"))));

    // The remaining node is unchanged.
    assertThat(compressedFibs.get(remains), equalTo(origFibs.get(remains)));
  }
}
