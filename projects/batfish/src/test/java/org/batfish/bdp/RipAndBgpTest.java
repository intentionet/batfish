package org.batfish.bdp;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
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

public class RipAndBgpTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

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
    BdpDataPlanePlugin dataPlanePlugin = new BdpDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    dataPlanePlugin.computeDataPlane(false);
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes();
    SortedSet<AbstractRoute> r1Routes = routes.get("r1").get(Configuration.DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r2Routes = routes.get("r2").get(Configuration.DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(Configuration.DEFAULT_VRF_NAME);
    Set<Prefix> r1Prefixes =
        r1Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r2Prefixes =
        r2Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix prefix1 = Prefix.fromString("1.0.0.0/8");
    Prefix prefix2 = Prefix.fromString("2.0.0.0/8");
    Prefix prefix3 = Prefix.fromString("3.0.0.0/8");

    assertThat(r1Prefixes, containsInAnyOrder(prefix1, prefix2, prefix3));
    assertThat(r2Prefixes, containsInAnyOrder(prefix1, prefix2, prefix3));
    assertThat(r3Prefixes, containsInAnyOrder(prefix1, prefix3));
  }
}
