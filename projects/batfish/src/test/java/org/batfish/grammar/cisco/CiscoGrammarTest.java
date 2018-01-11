package org.batfish.grammar.cisco;

import static java.util.Comparator.naturalOrder;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDeclaredNames;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.batfish.bdp.BdpDataPlanePlugin;
import org.batfish.common.CompositeBatfishException;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NetworkAddress;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
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
    List<String> configurationNames = ImmutableList.of("aaaNoNewmodel", "aaaNewmodel");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    configurationNames
                        .stream()
                        .collect(
                            ImmutableSortedMap.toImmutableSortedMap(
                                naturalOrder(),
                                n -> n,
                                n -> CommonUtil.readResource(TESTCONFIGS_PREFIX + n))))
                .build(),
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
    List<String> configurationNames = ImmutableList.of("r1", "r2");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(configurations, true);
    CommonUtil.initRemoteBgpNeighbors(configurations, ipOwners);
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
    List<String> configurationNames =
        ImmutableList.of("arista_disabled", "arista_enabled", "nxos_disabled", "nxos_enabled");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(configurations, true);
    CommonUtil.initRemoteBgpNeighbors(configurations, ipOwners);
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
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    SortedMap<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(configurations, true);
    CommonUtil.initRemoteBgpNeighbors(configurations, ipOwners);
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
  public void testCommunityListConversion() throws IOException {
    String testrigName = "community-list-conversion";
    String iosName = "ios";
    String nxosName = "nxos";
    String eosName = "eos";
    List<String> configurationNames = ImmutableList.of(iosName, nxosName, eosName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    SortedMap<String, Configuration> configurations;
    try {
      configurations = batfish.loadConfigurations();
    } catch (CompositeBatfishException e) {
      throw e.asSingleException();
    }

    Configuration iosCommunityListConfig = configurations.get(iosName);
    SortedMap<String, CommunityList> iosCommunityLists = iosCommunityListConfig.getCommunityLists();

    Configuration eosCommunityListConfig = configurations.get(eosName);
    SortedMap<String, CommunityList> eosCommunityLists = eosCommunityListConfig.getCommunityLists();

    Configuration nxosCommunityListConfig = configurations.get(nxosName);
    SortedMap<String, CommunityList> nxosCommunityLists =
        nxosCommunityListConfig.getCommunityLists();

    String iosRegexImpliedStd = getCLRegex(iosCommunityLists, "40");
    String iosRegexImpliedExp = getCLRegex(iosCommunityLists, "400");
    String iosRegexStd = getCLRegex(iosCommunityLists, "std_community");
    String iosRegexExp = getCLRegex(iosCommunityLists, "exp_community");
    String iosRegexStdAsnn = getCLRegex(iosCommunityLists, "std_as_nn");
    String iosRegexExpAsnn = getCLRegex(iosCommunityLists, "exp_as_nn");
    String iosRegexStdGshut = getCLRegex(iosCommunityLists, "std_gshut");
    String iosRegexExpGshut = getCLRegex(iosCommunityLists, "exp_gshut");
    String iosRegexStdInternet = getCLRegex(iosCommunityLists, "std_internet");
    String iosRegexExpInternet = getCLRegex(iosCommunityLists, "exp_internet");
    String iosRegexStdLocalAs = getCLRegex(iosCommunityLists, "std_local_AS");
    String iosRegexExpLocalAs = getCLRegex(iosCommunityLists, "exp_local_AS");
    String iosRegexStdNoAdv = getCLRegex(iosCommunityLists, "std_no_advertise");
    String iosRegexExpNoAdv = getCLRegex(iosCommunityLists, "exp_no_advertise");
    String iosRegexStdNoExport = getCLRegex(iosCommunityLists, "std_no_export");
    String iosRegexExpNoExport = getCLRegex(iosCommunityLists, "exp_no_export");

    String eosRegexStd = getCLRegex(eosCommunityLists, "eos_std");
    String eosRegexExp = getCLRegex(eosCommunityLists, "eos_exp");
    String eosRegexStdGshut = getCLRegex(eosCommunityLists, "eos_std_gshut");
    String eosRegexStdInternet = getCLRegex(eosCommunityLists, "eos_std_internet");
    String eosRegexStdLocalAs = getCLRegex(eosCommunityLists, "eos_std_local_AS");
    String eosRegexStdNoAdv = getCLRegex(eosCommunityLists, "eos_std_no_adv");
    String eosRegexStdNoExport = getCLRegex(eosCommunityLists, "eos_std_no_export");
    String eosRegexStdMulti = getCLRegex(eosCommunityLists, "eos_std_multi");
    String eosRegexExpMulti = getCLRegex(eosCommunityLists, "eos_exp_multi");

    String nxosRegexStd = getCLRegex(nxosCommunityLists, "nxos_std");
    String nxosRegexExp = getCLRegex(nxosCommunityLists, "nxos_exp");
    String nxosRegexStdInternet = getCLRegex(nxosCommunityLists, "nxos_std_internet");
    String nxosRegexStdLocalAs = getCLRegex(nxosCommunityLists, "nxos_std_local_AS");
    String nxosRegexStdNoAdv = getCLRegex(nxosCommunityLists, "nxos_std_no_adv");
    String nxosRegexStdNoExport = getCLRegex(nxosCommunityLists, "nxos_std_no_export");
    String nxosRegexStdMulti = getCLRegex(nxosCommunityLists, "nxos_std_multi");
    String nxosRegexExpMulti = getCLRegex(nxosCommunityLists, "nxos_exp_multi");

    // Check well known community regexes are generated properly
    String regexInternet =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.INTERNET.getValue()) + "$";
    String regexNoAdv =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.NO_ADVERTISE.getValue()) + "$";
    String regexNoExport =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.NO_EXPORT.getValue()) + "$";
    String regexGshut = "^" + CommonUtil.longToCommunity(WellKnownCommunity.GSHUT.getValue()) + "$";
    String regexLocalAs =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.LOCAL_AS.getValue()) + "$";
    assertThat(iosRegexStdInternet, equalTo(regexInternet));
    assertThat(iosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(iosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(iosRegexStdGshut, equalTo(regexGshut));
    assertThat(iosRegexStdLocalAs, equalTo(regexLocalAs));
    assertThat(eosRegexStdInternet, equalTo(regexInternet));
    assertThat(eosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(eosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(eosRegexStdGshut, equalTo(regexGshut));
    assertThat(eosRegexStdLocalAs, equalTo(regexLocalAs));
    // NX-OS does not support gshut
    assertThat(nxosRegexStdInternet, equalTo(regexInternet));
    assertThat(nxosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(nxosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(nxosRegexStdLocalAs, equalTo(regexLocalAs));

    // Confirm for the same literal communities, standard and expanded regexs are different
    assertThat(iosRegexImpliedStd, not(equalTo(iosRegexImpliedExp)));
    assertThat(iosRegexStd, not(equalTo(iosRegexExp)));
    assertThat(iosRegexStdAsnn, not(equalTo(iosRegexExpAsnn)));
    assertThat(iosRegexStdInternet, not(equalTo(iosRegexExpInternet)));
    assertThat(iosRegexStdNoAdv, not(equalTo(iosRegexExpNoAdv)));
    assertThat(iosRegexStdNoExport, not(equalTo(iosRegexExpNoExport)));
    assertThat(iosRegexStdGshut, not(equalTo(iosRegexExpGshut)));
    assertThat(iosRegexStdLocalAs, not(equalTo(iosRegexExpLocalAs)));
    assertThat(eosRegexStd, not(equalTo(eosRegexExp)));
    assertThat(eosRegexStdMulti, not(equalTo(eosRegexExpMulti)));
    assertThat(nxosRegexStd, not(equalTo(nxosRegexExp)));
    assertThat(nxosRegexStdMulti, not(equalTo(nxosRegexExpMulti)));
  }

  private static String getCLRegex(
      SortedMap<String, CommunityList> communityLists, String communityName) {
    return communityLists.get(communityName).getLines().get(0).getRegex();
  }

  @Test
  public void testInterfaceNames() throws IOException {
    String testrigName = "interface-names";
    String iosHostname = "ios";
    String i1Name = "Ethernet0/0";

    List<String> configurationNames = ImmutableList.of(iosHostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    SortedMap<String, Configuration> configurations;
    try {
      configurations = batfish.loadConfigurations();
    } catch (CompositeBatfishException e) {
      throw e.asSingleException();
    }
    Interface i1 = configurations.get(iosHostname).getInterfaces().get(i1Name);
    assertThat(i1, hasDeclaredNames("Ethernet0/0", "e0/0", "Eth0/0", "ether0/0-1"));
  }

  @Test
  public void testIpsecVpnIos() throws IOException {
    String testrigName = "ipsec-vpn-ios";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    SortedMap<String, Configuration> configurations;
    try {
      configurations = batfish.loadConfigurations();
    } catch (CompositeBatfishException e) {
      throw e.asSingleException();
    }

    assertThat(
        configurations.values().stream().flatMap(c -> c.getIpsecVpns().values().stream()).count(),
        equalTo(6L));
    configurations
        .values()
        .stream()
        .flatMap(c -> c.getIpsecVpns().values().stream())
        .forEach(iv -> assertThat(iv.getRemoteIpsecVpn(), not(nullValue())));
    /* Two tunnels should not be established because of a password mismatch between r1 and r3 */
    assertThat(
        configurations
            .values()
            .stream()
            .flatMap(c -> c.getInterfaces().values().stream())
            .filter(i -> i.getInterfaceType().equals(InterfaceType.TUNNEL) && i.getActive())
            .count(),
        equalTo(4L));
  }

  @Test
  public void testOspfMaxMetric() throws IOException {
    String testrigName = "ospf-max-metric";
    String iosMaxMetricName = "ios-max-metric";
    String iosMaxMetricCustomName = "ios-max-metric-custom";
    String iosMaxMetricOnStartupName = "ios-max-metric-on-startup";
    List<String> configurationNames =
        ImmutableList.of(iosMaxMetricName, iosMaxMetricCustomName, iosMaxMetricOnStartupName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    SortedMap<String, Configuration> configurations;
    try {
      configurations = batfish.loadConfigurations();
    } catch (CompositeBatfishException e) {
      throw e.asSingleException();
    }
    Configuration iosMaxMetric = configurations.get(iosMaxMetricName);
    Configuration iosMaxMetricCustom = configurations.get(iosMaxMetricCustomName);
    Configuration iosMaxMetricOnStartup = configurations.get(iosMaxMetricOnStartupName);
    OspfProcess proc = iosMaxMetric.getDefaultVrf().getOspfProcess();
    OspfProcess procCustom = iosMaxMetricCustom.getDefaultVrf().getOspfProcess();
    OspfProcess procOnStartup = iosMaxMetricOnStartup.getDefaultVrf().getOspfProcess();
    long expectedMaxMetricRouterLsa =
        org.batfish.representation.cisco.OspfProcess.MAX_METRIC_ROUTER_LSA;
    long expectedMaxMetricStub = org.batfish.representation.cisco.OspfProcess.MAX_METRIC_ROUTER_LSA;
    long expectedMaxMetricExternal =
        org.batfish.representation.cisco.OspfProcess.DEFAULT_MAX_METRIC_EXTERNAL_LSA;
    long expectedMaxMetricSummary =
        org.batfish.representation.cisco.OspfProcess.DEFAULT_MAX_METRIC_SUMMARY_LSA;
    long expectedCustomMaxMetricExternal = 12345L;
    long expectedCustomMaxMetricSummary = 23456L;

    assertThat(proc.getMaxMetricTransitLinks(), equalTo(expectedMaxMetricRouterLsa));
    assertThat(proc.getMaxMetricStubNetworks(), equalTo(expectedMaxMetricStub));
    assertThat(proc.getMaxMetricExternalNetworks(), equalTo(expectedMaxMetricExternal));
    assertThat(proc.getMaxMetricSummaryNetworks(), equalTo(expectedMaxMetricSummary));
    assertThat(procCustom.getMaxMetricTransitLinks(), equalTo(expectedMaxMetricRouterLsa));
    assertThat(procCustom.getMaxMetricStubNetworks(), equalTo(expectedMaxMetricStub));
    assertThat(procCustom.getMaxMetricExternalNetworks(), equalTo(expectedCustomMaxMetricExternal));
    assertThat(procCustom.getMaxMetricSummaryNetworks(), equalTo(expectedCustomMaxMetricSummary));
    assertThat(procOnStartup.getMaxMetricTransitLinks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricStubNetworks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricExternalNetworks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricSummaryNetworks(), is(nullValue()));
  }

  @Test
  public void testOspfPointToPoint() throws IOException {
    String testrigName = "ospf-point-to-point";
    String iosOspfPointToPoint = "ios-ospf-point-to-point";
    List<String> configurationNames = ImmutableList.of(iosOspfPointToPoint);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    SortedMap<String, Configuration> configurations;
    try {
      configurations = batfish.loadConfigurations();
    } catch (CompositeBatfishException e) {
      throw e.asSingleException();
    }
    Configuration iosMaxMetric = configurations.get(iosOspfPointToPoint);
    Interface e0Sub0 = iosMaxMetric.getInterfaces().get("Ethernet0/0");
    Interface e0Sub1 = iosMaxMetric.getInterfaces().get("Ethernet0/1");

    assertTrue(e0Sub0.getOspfPointToPoint());
    assertFalse(e0Sub1.getOspfPointToPoint());
  }

  @Test
  public void testParsingRecovery() throws IOException {
    String testrigName = "parsing-recovery";
    String iosRecoveryName = "ios-recovery";
    List<String> configurationNames = ImmutableList.of(iosRecoveryName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
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
    Set<NetworkAddress> l3Prefixes = iosRecoveryInterfaces.get("Loopback3").getAllAddresses();
    Set<NetworkAddress> l4Prefixes = iosRecoveryInterfaces.get("Loopback4").getAllAddresses();

    assertThat("Loopback0", isIn(iosRecoveryInterfaceNames));
    assertThat("Loopback1", isIn(iosRecoveryInterfaceNames));
    assertThat("Loopback2", not(isIn(iosRecoveryInterfaceNames)));
    assertThat("Loopback3", isIn(iosRecoveryInterfaceNames));
    assertThat(new NetworkAddress("10.0.0.1/32"), not(isIn(l3Prefixes)));
    assertThat(new NetworkAddress("10.0.0.2/32"), isIn(l3Prefixes));
    assertThat("Loopback4", isIn(iosRecoveryInterfaceNames));
    assertThat(new NetworkAddress("10.0.0.3/32"), not(isIn(l4Prefixes)));
    assertThat(new NetworkAddress("10.0.0.4/32"), isIn(l4Prefixes));
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
            TestrigText.builder().setConfigurationText(configurationTextMap).build(), _folder);
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
