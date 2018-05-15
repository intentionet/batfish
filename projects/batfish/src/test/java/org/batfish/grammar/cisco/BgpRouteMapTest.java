package org.batfish.grammar.cisco;

import static org.batfish.dataplane.matchers.PrefixTracerMatchers.fromHostname;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.toHostname;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasFilteredOut;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasInstalled;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasSent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.ibdp.IncrementalDataPlane;
import org.batfish.dataplane.ibdp.PrefixTracer;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests of {@link org.batfish.datamodel.routing_policy.RoutingPolicy} as it pertains to modeling of
 * BGP route maps and dataplane computation
 */
public class BgpRouteMapTest {
  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  private static final String DP_ENGINE = "ibdp";
  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Test
  public void testEmptyExportRouteMap() throws IOException {
    String testrigName = "bgp-empty-routemap";
    String hostname = "exporter";
    Prefix advertisedPrefix = Prefix.parse("1.1.1.1/32");
    List<String> configurationNames = ImmutableList.of(hostname);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDataplaneEngineName(DP_ENGINE);
    batfish.computeDataPlane(false);
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane();

    // Test that /32 loopback route is sent to 1.1.1.3, but NOT 1.1.1.4

    PrefixTracer pt = dp.getPrefixTracingInfo().get(hostname).get(Configuration.DEFAULT_VRF_NAME);
    // Sent should contain neighbor 1.1.1.3, because it uses TEST_EXPORT_PERMIT routemap
    assertThat(pt, wasSent(advertisedPrefix, toHostname("1.1.1.3")));
    assertThat(pt, not(wasFilteredOut(advertisedPrefix, toHostname("1.1.1.3"))));
    // Filtered should contain neighbor 1.1.1.4, because it uses TEST_EXPORT_DENY routemap
    assertThat(pt, not(wasSent(advertisedPrefix, toHostname("1.1.1.4"))));
    assertThat(pt, wasFilteredOut(advertisedPrefix, toHostname("1.1.1.4")));
  }

  /**
   * Test behavior in the presence of route map continue statments. See testrig config named
   * "exporter" for detailed construction of the routemap.
   */
  @Test
  public void testMatchWithContinue() throws IOException {
    String testrigName = "bgp-routemap-with-continue";
    Prefix advertisedPrefix = Prefix.parse("1.1.1.1/32");
    Prefix blockedPrefix = Prefix.parse("1.1.1.2/31");
    List<String> configurationNames = ImmutableList.of("exporter", "importer");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDataplaneEngineName(DP_ENGINE);
    batfish.computeDataPlane(false);
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane();

    /*
     * Test exports: the /32 route should have been exported while the /31 should be blocked
     */

    PrefixTracer pt = dp.getPrefixTracingInfo().get("exporter").get(Configuration.DEFAULT_VRF_NAME);
    assertThat(pt, wasFilteredOut(blockedPrefix, toHostname("importer")));
    assertThat(pt, not(wasSent(blockedPrefix, toHostname("importer"))));

    assertThat(pt, not(wasFilteredOut(advertisedPrefix, toHostname("importer"))));
    assertThat(pt, wasSent(advertisedPrefix, toHostname("importer")));

    /*
     * Test imports: the /32 route should have been installed and not filtered
     */
    pt = dp.getPrefixTracingInfo().get("importer").get(Configuration.DEFAULT_VRF_NAME);
    assertThat(pt, wasInstalled(advertisedPrefix, fromHostname("exporter")));
    assertThat(pt, not(wasInstalled(blockedPrefix, fromHostname("exporter"))));
  }
}
