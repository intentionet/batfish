package org.batfish.grammar.flatjuniper;

import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbor;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessLists;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfCost;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.hasMetric;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.isAdvertised;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OspfAreaSummary;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.juniper.JuniperStructureType;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link FlatJuniperParser}flat Junpier parser and {@link
 * FlatJuniperControlPlaneExtractor}.
 */
public class FlatJuniperGrammarTest {

  private static class HasClusterId extends FeatureMatcher<BgpNeighbor, Long> {
    public HasClusterId(Matcher<? super Long> subMatcher) {
      super(subMatcher, "clusterId", "clusterId");
    }

    @Override
    protected Long featureValueOf(BgpNeighbor actual) {
      return actual.getClusterId();
    }
  }

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/juniper/testconfigs/";

  private static String TESTRIGS_PREFIX = "org/batfish/grammar/juniper/testrigs/";

  private static HasClusterId hasClusterId(long expectedClusterId) {
    return new HasClusterId(equalTo(expectedClusterId));
  }

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private Configuration parseConfig(String hostname) throws IOException {
    return parseTextConfigs(hostname).get(hostname);
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    return getBatfishForConfigurationNames(configurationNames).loadConfigurations();
  }

  @Test
  public void testApplications() throws IOException {
    String hostname = "applications";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> unusedStructures =
        batfish.loadConvertConfigurationAnswerElementOrReparse().getUnusedStructures();

    /* a1 should be used, while a2 should be unused */
    assertThat(unusedStructures, hasKey(hostname));
    SortedMap<String, SortedMap<String, SortedSet<Integer>>> byType =
        unusedStructures.get(hostname);
    assertThat(byType, hasKey(JuniperStructureType.APPLICATION.getDescription()));
    SortedMap<String, SortedSet<Integer>> byName =
        byType.get(JuniperStructureType.APPLICATION.getDescription());
    assertThat(byName, hasKey("a2"));
    assertThat(byName, not(hasKey("a1")));
    assertThat(byName, not(hasKey("a3")));
  }

  @Test
  public void testApplicationWithTerms() throws IOException {
    String hostname = "application-with-terms";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasIpAccessLists(hasValue(anything())));

    List<IpAccessListLine> lines = c.getIpAccessLists().values().iterator().next().getLines();
    assertThat(lines, hasSize(2));

    IpAccessListLine line1 = lines.get(0);
    assertThat(
        line1,
        equalTo(
            IpAccessListLine.acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setDstPorts(ImmutableList.of(new SubRange(90, 90)))
                    .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                    .setSrcPorts(ImmutableList.of(new SubRange(90, 90)))
                    .build())));

    IpAccessListLine line2 = lines.get(1);
    assertThat(
        line2,
        equalTo(
            IpAccessListLine.acceptingHeaderSpace(
                HeaderSpace.builder()
                    .setDstPorts(ImmutableList.of(new SubRange(91, 91)))
                    .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                    .setSrcPorts(ImmutableList.of(new SubRange(91, 91)))
                    .build())));
  }

  @Test
  public void testAutonomousSystem() throws IOException {
    String testrigName = "autonomous-system";
    String c1Name = "as1";
    String c2Name = "as2";
    String c3Name = "as3";
    Prefix neighborPrefix = Prefix.parse("1.0.0.1/32");

    List<String> configurationNames = ImmutableList.of(c1Name, c2Name, c3Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration c1 = configurations.get(c1Name);
    Configuration c2 = configurations.get(c2Name);
    Configuration c3 = configurations.get(c3Name);

    assertThat(c1, hasDefaultVrf(hasBgpProcess(hasNeighbor(neighborPrefix, hasLocalAs(1)))));
    assertThat(c2, hasDefaultVrf(hasBgpProcess(hasNeighbor(neighborPrefix, hasLocalAs(1)))));
    assertThat(c3, hasDefaultVrf(hasBgpProcess(hasNeighbor(neighborPrefix, hasLocalAs(1)))));
  }

  @Test
  public void testBgpClusterId() throws IOException {
    String testrigName = "rr";
    String configName = "rr";
    Ip neighbor1Ip = new Ip("2.2.2.2");
    Ip neighbor2Ip = new Ip("4.4.4.4");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration rr = configurations.get(configName);
    BgpProcess proc = rr.getDefaultVrf().getBgpProcess();
    BgpNeighbor neighbor1 =
        proc.getNeighbors().get(new Prefix(neighbor1Ip, Prefix.MAX_PREFIX_LENGTH));
    BgpNeighbor neighbor2 =
        proc.getNeighbors().get(new Prefix(neighbor2Ip, Prefix.MAX_PREFIX_LENGTH));

    assertThat(neighbor1, hasClusterId(new Ip("3.3.3.3").asLong()));
    assertThat(neighbor2, hasClusterId(new Ip("1.1.1.1").asLong()));
  }

  @Test
  public void testBgpMultipathMultipleAs() throws IOException {
    String testrigName = "multipath-multiple-as";
    List<String> configurationNames =
        ImmutableList.of("multiple_as_disabled", "multiple_as_enabled", "multiple_as_mixed");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    MultipathEquivalentAsPathMatchMode multipleAsDisabled =
        configurations
            .get("multiple_as_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsEnabled =
        configurations
            .get("multiple_as_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsMixed =
        configurations
            .get("multiple_as_mixed")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();

    assertThat(multipleAsDisabled, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
    assertThat(multipleAsEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(multipleAsMixed, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
  }

  @Test
  public void testEthernetSwitchingFilterReference() throws IOException {
    String hostname = "ethernet-switching-filter";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> unusedStructures =
        batfish.loadConvertConfigurationAnswerElementOrReparse().getUnusedStructures();

    /* esfilter should not be unused, whuile esfilter2 should be unused */
    assertThat(unusedStructures, hasKey(hostname));
    SortedMap<String, SortedMap<String, SortedSet<Integer>>> byType =
        unusedStructures.get(hostname);
    assertThat(byType, hasKey(JuniperStructureType.FIREWALL_FILTER.getDescription()));
    SortedMap<String, SortedSet<Integer>> byName =
        byType.get(JuniperStructureType.FIREWALL_FILTER.getDescription());
    assertThat(byName, hasKey("esfilter2"));
    assertThat(byName, not(hasKey("esfilter")));
  }

  @Test
  public void testOspfMetric() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/ospfmetric")
            .get("ospfmetric");
    OspfAreaSummary summary =
        config
            .getDefaultVrf()
            .getOspfProcess()
            .getAreas()
            .get(1L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, not(isAdvertised()));
    assertThat(summary, hasMetric(123L));

    // Defaults
    summary =
        config
            .getDefaultVrf()
            .getOspfProcess()
            .getAreas()
            .get(2L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, isAdvertised());
    assertThat(summary, hasMetric(nullValue()));

    // Interface override
    assertThat(config, hasInterface("fe-1/0/1.0", hasOspfCost(equalTo(17))));
  }

  @Test
  public void testOspfPsk() throws IOException {
    /* allow both encrypted and unencrypted key */
    parseConfig("ospf-psk");
  }

  @Test
  public void testTacplusPsk() throws IOException {
    /* allow both encrypted and unencrypted key */
    parseConfig("tacplus-psk");
  }

  @Test
  public void testInterfaceMtu() throws IOException {
    Configuration c = parseConfig("interfaceMtu");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c.getInterfaces().keySet(), equalTo(Collections.singleton("xe-0/0/0:0.0")));
    assertThat(c, hasInterface("xe-0/0/0:0.0", hasMtu(9000)));
  }

  @Test
  public void testOspfInterfaceAreaAssignment() throws IOException {
    Configuration c = parseConfig("ospfInterfaceAreaAssignment");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c, hasInterface("xe-0/0/0.0", isOspfPassive(equalTo(false))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(0L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.0"))))));

    assertThat(c, hasInterface("xe-0/0/0.1", isOspfPassive()));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(0L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.1"))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(1L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.1"))))));

    /* The following interfaces should be absent since they have no IP addresses assigned. */
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.2")))))));

    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(1L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))))));
  }

  @Test
  public void testParsingRecovery() {
    String recoveryText =
        CommonUtil.readResource("org/batfish/grammar/juniper/testconfigs/recovery");
    Settings settings = new Settings();
    FlatJuniperCombinedParser cp = new FlatJuniperCombinedParser(recoveryText, settings);
    Flat_juniper_configurationContext ctx = cp.parse();
    FlatJuniperRecoveryExtractor extractor = new FlatJuniperRecoveryExtractor();
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(extractor, ctx);

    assertThat(extractor.getNumSets(), equalTo(8));
    assertThat(extractor.getNumErrorNodes(), equalTo(8));
  }

  @Test
  public void testStaticRoutes() throws IOException {
    Configuration c = parseConfig("static-routes");

    assertThat(c, hasDefaultVrf(hasStaticRoutes(hasItem(hasPrefix(Prefix.parse("1.0.0.0/8"))))));
    assertThat(c, hasVrf("ri2", hasStaticRoutes(hasItem(hasPrefix(Prefix.parse("2.0.0.0/8"))))));
  }
}
