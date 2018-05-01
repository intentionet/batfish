package org.batfish.dataplane;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RipAndBgpTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"bdp"}, {"ibdp"}});
  }

  @Parameter public String dpEngine;

  private static String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Test
  public void testOutputRoutes() throws IOException {
    String testrigResourcePrefix = TESTRIGS_PREFIX + "rip";
    Set<String> configurations = ImmutableSet.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(testrigResourcePrefix, configurations)
                .build(),
            _folder);
    batfish.getSettings().setDataplaneEngineName(dpEngine);
    batfish.computeDataPlane(false);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes(batfish.loadDataPlane());
    SortedSet<AbstractRoute> r1Routes = routes.get("r1").get(Configuration.DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r2Routes = routes.get("r2").get(Configuration.DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(Configuration.DEFAULT_VRF_NAME);
    Set<Prefix> r1Prefixes =
        r1Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r2Prefixes =
        r2Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix prefix1 = Prefix.parse("1.0.0.0/8");
    Prefix prefix2 = Prefix.parse("2.0.0.0/8");
    Prefix prefix3 = Prefix.parse("3.0.0.0/8");

    assertThat(r1Prefixes, containsInAnyOrder(prefix1, prefix2, prefix3));
    assertThat(r2Prefixes, containsInAnyOrder(prefix1, prefix2, prefix3));
    assertThat(r3Prefixes, containsInAnyOrder(prefix1, prefix3));
  }
}
