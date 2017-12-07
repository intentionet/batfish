package org.batfish.bdp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class RipAndBgpTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/rip/configs/";

  @Test
  public void testOutputRoutes() throws IOException {
    SortedMap<String, String> configurationsText = new TreeMap<>();
    String[] configurationNames = new String[] {"r1", "r2", "r3"};
    for (String configurationName : configurationNames) {
      String configurationText = CommonUtil.readResource(TESTCONFIGS_PREFIX + configurationName);
      configurationsText.put(configurationName, configurationText);
    }
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            configurationsText,
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            _folder);
    BdpDataPlanePlugin dataPlanePlugin = new BdpDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    dataPlanePlugin.computeDataPlane(false);
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes();
    SortedSet<AbstractRoute> r1Routes = routes.get("r1").get(Configuration.DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r2Routes = routes.get("r2").get(Configuration.DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(Configuration.DEFAULT_VRF_NAME);
    Set<Prefix> r1Prefixes = r1Routes.stream().map(r -> r.getNetwork()).collect(Collectors.toSet());
    Set<Prefix> r2Prefixes = r2Routes.stream().map(r -> r.getNetwork()).collect(Collectors.toSet());
    Set<Prefix> r3Prefixes = r3Routes.stream().map(r -> r.getNetwork()).collect(Collectors.toSet());
    Prefix prefix1 = new Prefix("1.0.0.0/8");
    Prefix prefix2 = new Prefix("2.0.0.0/8");
    Prefix prefix3 = new Prefix("3.0.0.0/8");
    assertTrue(r1Prefixes.contains(prefix1));
    assertTrue(r1Prefixes.contains(prefix2));
    assertTrue(r2Prefixes.contains(prefix1));
    assertTrue(r2Prefixes.contains(prefix2));
    assertTrue(r2Prefixes.contains(prefix3));
    assertTrue(r3Prefixes.contains(prefix1));
    assertTrue(r3Prefixes.contains(prefix3));
  }
}
