package org.batfish.grammar.cisco;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.batfish.bdp.BdpDataPlanePlugin;
import org.batfish.common.CompositeBatfishException;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link CiscoParser}Cisco parser and {@link CiscoControlPlaneExtractor}. */
public class CiscoGrammarTest {

  private static String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco/testconfigs/";
  private static String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testAaaNewmodel() throws IOException {
    SortedMap<String, String> configurationText = new TreeMap<>();
    String configurationName = "aaaNoNewmodel";
    String aaaNoNewmodelConfigurationText =
        CommonUtil.readResource(TESTCONFIGS_PREFIX + configurationName);
    configurationText.put(configurationName, aaaNoNewmodelConfigurationText);
    configurationName = "aaaNewmodel";
    String aaaNewmodelConfigurationText =
        CommonUtil.readResource(TESTCONFIGS_PREFIX + configurationName);
    configurationText.put(configurationName, aaaNewmodelConfigurationText);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            configurationText,
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration newModelConfiguration = configurations.get("aaaNewmodel");
    boolean aaaNewmodel = newModelConfiguration.getVendorFamily().getCisco().getAaa().getNewModel();
    assertTrue(aaaNewmodel);
    Configuration noNewModelConfiguration = configurations.get("aaaNoNewmodel");
    aaaNewmodel = noNewModelConfiguration.getVendorFamily().getCisco().getAaa().getNewModel();
    assertFalse(aaaNewmodel);
  }

  @Test
  public void testBgpLocalAs() throws IOException {
    String testrigName = "bgp-local-as";
    String[] configurationNames = new String[] {"r1", "r2"};
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigResource(
            TESTRIGS_PREFIX + testrigName, configurationNames, null, null, null, null, _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = batfish.computeIpOwners(configurations, true);
    batfish.initRemoteBgpNeighbors(configurations, ipOwners);
    Configuration r1 = configurations.get("r1");
    Configuration r2 = configurations.get("r2");
    assertThat(
        r1.getDefaultVrf()
            .getBgpProcess()
            .getNeighbors()
            .get(new Prefix("1.2.0.2/32"))
            .getRemoteBgpNeighbor(),
        is(notNullValue()));
    assertThat(
        r2.getDefaultVrf()
            .getBgpProcess()
            .getNeighbors()
            .get(new Prefix("1.2.0.1/32"))
            .getRemoteBgpNeighbor(),
        is(notNullValue()));
  }

  @Test
  public void testBgpMultipathRelax() throws IOException {
    String testrigName = "bgp-multipath-relax";
    String[] configurationNames =
        new String[] {"arista_disabled", "arista_enabled", "nxos_disabled", "nxos_enabled"};
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigResource(
            TESTRIGS_PREFIX + testrigName, configurationNames, null, null, null, null, _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = batfish.computeIpOwners(configurations, true);
    batfish.initRemoteBgpNeighbors(configurations, ipOwners);
    MultipathEquivalentAsPathMatchMode aristaDisabled =
        configurations
            .get("arista_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode aristaEnabled =
        configurations
            .get("arista_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode nxosDisabled =
        configurations
            .get("nxos_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode nxosEnabled =
        configurations
            .get("nxos_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();

    assertThat(aristaDisabled, equalTo(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
    assertThat(aristaEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(nxosDisabled, equalTo(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
    assertThat(nxosEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
  }

  @Test
  public void testBgpRemovePrivateAs() throws IOException {
    String testrigName = "bgp-remove-private-as";
    String[] configurationNames = new String[] {"r1", "r2", "r3"};
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigResource(
            TESTRIGS_PREFIX + testrigName, configurationNames, null, null, null, null, _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = batfish.computeIpOwners(configurations, true);
    batfish.initRemoteBgpNeighbors(configurations, ipOwners);
    BdpDataPlanePlugin dataPlanePlugin = new BdpDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    dataPlanePlugin.computeDataPlane(false);

    // Check that 1.1.1.1/32 appears on r3
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes();
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(Configuration.DEFAULT_VRF_NAME);
    Set<Prefix> r3Prefixes = r3Routes.stream().map(r -> r.getNetwork()).collect(Collectors.toSet());
    Prefix r1Loopback = new Prefix("1.1.1.1/32");
    assertTrue(r3Prefixes.contains(r1Loopback));

    // check that private AS is present in path in received 1.1.1.1/32 advert on r2
    batfish.initBgpAdvertisements(configurations);
    Configuration r2 = configurations.get("r2");
    boolean r2HasPrivate =
        r2.getReceivedEbgpAdvertisements()
            .stream()
            .filter(a -> a.getNetwork().equals(r1Loopback))
            .toArray(BgpAdvertisement[]::new)[0]
            .getAsPath()
            .getAsSets()
            .stream()
            .flatMap(asSet -> asSet.stream())
            .anyMatch(AsPath::isPrivateAs);
    assertTrue(r2HasPrivate);

    // check that private AS is absent from path in received 1.1.1.1/32 advert on r3
    Configuration r3 = configurations.get("r3");
    boolean r3HasPrivate =
        r3.getReceivedEbgpAdvertisements()
            .stream()
            .filter(a -> a.getNetwork().equals(r1Loopback))
            .toArray(BgpAdvertisement[]::new)[0]
            .getAsPath()
            .getAsSets()
            .stream()
            .flatMap(asSet -> asSet.stream())
            .anyMatch(AsPath::isPrivateAs);
    assertFalse(r3HasPrivate);
  }

  @Test
  public void testParsingRecovery() throws IOException {
    String testrigName = "parsing-recovery";
    String iosRecoveryName = "ios-recovery";
    String[] configurationNames = new String[] {iosRecoveryName};
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigResource(
            TESTRIGS_PREFIX + testrigName, configurationNames, null, null, null, null, _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    SortedMap<String, Configuration> configurations;
    try {
      configurations = batfish.loadConfigurations();
    } catch (CompositeBatfishException e) {
      throw e.asSingleException();
    }
    Configuration iosRecovery = configurations.get(iosRecoveryName);
    SortedMap<String, Interface> iosRecoveryInterfaces = iosRecovery.getInterfaces();
    Set<String> iosRecoveryInterfaceNames = iosRecoveryInterfaces.keySet();
    Set<Prefix> l3Prefixes = iosRecoveryInterfaces.get("Loopback3").getAllPrefixes();
    Set<Prefix> l4Prefixes = iosRecoveryInterfaces.get("Loopback4").getAllPrefixes();

    assertThat("Loopback0", isIn(iosRecoveryInterfaceNames));
    assertThat("Loopback1", isIn(iosRecoveryInterfaceNames));
    assertThat("Loopback2", not(isIn(iosRecoveryInterfaceNames)));
    assertThat("Loopback3", isIn(iosRecoveryInterfaceNames));
    assertThat(new Prefix("10.0.0.1/32"), not(isIn(l3Prefixes)));
    assertThat(new Prefix("10.0.0.2/32"), isIn(l3Prefixes));
    assertThat("Loopback4", isIn(iosRecoveryInterfaceNames));
    assertThat(new Prefix("10.0.0.3/32"), not(isIn(l4Prefixes)));
    assertThat(new Prefix("10.0.0.4/32"), isIn(l4Prefixes));
  }

  public String readTestConfig(String name) {
    return CommonUtil.readResource(TESTCONFIGS_PREFIX + "/" + name);
  }

  @Test
  public void testRfc1583Compatible() throws IOException {
    SortedMap<String, String> configurationTextMap = new TreeMap<>();
    String[] configurationNames =
        new String[] {"rfc1583Compatible", "rfc1583NoCompatible", "rfc1583Unconfigured"};
    Boolean[] expectedResults = new Boolean[] {Boolean.TRUE, Boolean.FALSE, null};
    for (String configName : configurationNames) {
      String configurationText = CommonUtil.readResource(TESTCONFIGS_PREFIX + configName);
      configurationTextMap.put(configName, configurationText);
    }
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            configurationTextMap,
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            Collections.emptySortedMap(),
            _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();

    for (int i = 0; i < configurationNames.length; i++) {
      Configuration configuration = configurations.get(configurationNames[i]);
      assertThat(configuration.getVrfs().size(), equalTo(1));
      for (Vrf vrf : configuration.getVrfs().values()) {
        assertThat(vrf.getOspfProcess().getRfc1583Compatible(), is(expectedResults[i]));
      }
    }
  }
}
