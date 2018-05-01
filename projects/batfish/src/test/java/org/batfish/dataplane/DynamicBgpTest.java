package org.batfish.dataplane;

import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasPrefix;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Network;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Prefix;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DynamicBgpTest {
  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"bdp"}, {"ibdp"}});
  }

  @Parameter public String dpEngine;

  /** Test correct handling of dynamic BGP sessions */
  @Test
  public void testDynamicBgp() throws IOException {
    String testrigName = "bgp-dynamic-session";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3", "r4");

    /*
     *
     * Setup is as follows:
     *
     *              +--+r3
     *  r1+------+r2|
     *              +--+r4
     *
     * Where r2 has a dynamic session listening to r3 & r4; We expect r1 to get routes advertised by
     * r3 & r4
     */
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    batfish.getSettings().setDataplaneEngineName(dpEngine);
    batfish.computeDataPlane(false); // compute and cache the dataPlane
    DataPlane dp = batfish.loadDataPlane();
    Network<BgpNeighbor, BgpSession> bgpTopology = dp.getBgpTopology();

    /*
     * Check peering edges. r1 <---> r2 has two edges, one in each direction. r2<--r3 and r2<--r4
     * are unidirectional connections, since dynamic neighbor cannot initiate connections.
     */
    assertThat(bgpTopology.edges(), hasSize(4));

    // Ensure routing info has been exchanged, and routes from r3/r4 exist on r1
    GenericRib<AbstractRoute> r1Rib = dp.getRibs().get("r1").get(Configuration.DEFAULT_VRF_NAME);
    assertThat(r1Rib.getRoutes(), hasItem(hasPrefix(Prefix.parse("9.9.9.33/32"))));
    assertThat(r1Rib.getRoutes(), hasItem(hasPrefix(Prefix.parse("9.9.9.44/32"))));
  }

  @Test
  public void testDynamicBgpWithoutUpdateSource() throws IOException {
    String testrigName = "bgp-dynamic-session-no-update-source";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3", "r4");

    /*
     * See test testDynamicBgp above for network setup
     */
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    batfish.getSettings().setDataplaneEngineName(dpEngine);
    batfish.computeDataPlane(false); // compute and cache the dataPlane

    DataPlane dp = batfish.loadDataPlane();
    Network<BgpNeighbor, BgpSession> bgpTopology = dp.getBgpTopology();

    /*
     * Check peering edges. r1 <---> r2 has two edges, one in each direction. r2<--r3 and r2<--r4
     * are unidirectional connections, since dynamic neighbor cannot initiate connections.
     */
    assertThat(bgpTopology.edges(), hasSize(4));

    // Ensure routing info has been exchanged, and routes from r3/r4 exist on r1
    GenericRib<AbstractRoute> r1Rib = dp.getRibs().get("r1").get(Configuration.DEFAULT_VRF_NAME);
    assertThat(r1Rib.getRoutes(), hasItem(hasPrefix(Prefix.parse("3.3.3.3/32"))));
    assertThat(r1Rib.getRoutes(), hasItem(hasPrefix(Prefix.parse("4.4.4.4/32"))));
  }

  /** Test BGP session establishment if dynamic seessions have been misconfigured */
  @Test
  public void testDynamicBgpMisconfigured() throws IOException {
    String testrigName = "bgp-dynamic-session-misconfigured";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3", "r4");

    /*
     *
     * Setup is as follows:
     *
     *              +--+r3
     *  r1+------+r2|
     *              +--+r4
     *
     * Where r2 has a dynamic session listening to r3 & r4;
     * BUT
     * r4 has been misconfigured and is outside of the prefix range.
     * We expect r1 to get routes advertised by r3 only
     */
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    batfish.getSettings().setDataplaneEngineName(dpEngine);
    batfish.computeDataPlane(false); // compute and cache the dataPlane

    DataPlane dp = batfish.loadDataPlane();
    Network<BgpNeighbor, BgpSession> bgpTopology = dp.getBgpTopology();

    /*
     * Check peering edges. r1 <---> r2 has two edges, one in each direction. r2<--r3 valid
     * are unidirectional connections, since dynamic neighbor cannot initiate connections.
     */
    assertThat(bgpTopology.edges(), hasSize(3));

    // Ensure routing info has been exchanged, and routes from r3/r4 exist on r1
    GenericRib<AbstractRoute> r1Rib = dp.getRibs().get("r1").get(Configuration.DEFAULT_VRF_NAME);
    assertThat(r1Rib.getRoutes(), hasItem(hasPrefix(Prefix.parse("9.9.9.33/32"))));
    assertThat(r1Rib.getRoutes(), not(hasItem(hasPrefix(Prefix.parse("9.9.9.44/32")))));
  }
}
