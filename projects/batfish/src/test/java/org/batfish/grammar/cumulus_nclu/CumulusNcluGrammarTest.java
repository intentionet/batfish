package org.batfish.grammar.cumulus_nclu;

import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.cumulus.Bond;
import org.batfish.representation.cumulus.CumulusNcluConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public final class CumulusNcluGrammarTest {
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cumulus_nclu/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  @SuppressWarnings("unused")
  private Configuration parseConfig(String hostname) throws IOException {
    return parseTextConfigs(hostname).get(hostname);
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.parseTextConfigs(_folder, names);
  }

  private @Nonnull CumulusNcluConfiguration parseVendorConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    CumulusNcluCombinedParser parser = new CumulusNcluCombinedParser(src, settings);
    CumulusNcluControlPlaneExtractor extractor =
        new CumulusNcluControlPlaneExtractor(src, parser, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(tree);
    assertThat(
        String.format("Ensure '%s' was successfully parsed", hostname),
        extractor.getVendorConfiguration(),
        notNullValue());
    return (CumulusNcluConfiguration) extractor.getVendorConfiguration();
  }

  @Test
  public void testBondExtraction() throws IOException {
    CumulusNcluConfiguration vc = parseVendorConfig("cumulus_nclu_bond");
    String bond1Name = "bond1";
    String bond2Name = "bond2";

    // referenced interfaces should have been created
    assertThat(
        vc.getInterfaces().keySet(),
        containsInAnyOrder(
            "swp1", //
            "swp2", //
            "swp3", //
            "swp4", //
            "swp5", //
            "swp6", //
            "swp7", //
            "swp8", //
            "swp1a-b2", //
            "swp1a-b3" //
            ));

    assertThat(
        "Ensure bonds were extracted",
        vc.getBonds().keySet(),
        containsInAnyOrder(bond1Name, bond2Name));

    Bond bond1 = vc.getBonds().get(bond1Name);
    Bond bond2 = vc.getBonds().get(bond2Name);

    assertThat("Ensure access VLAN ID was set", bond1.getBridge().getAccess(), equalTo(2));
    assertThat("Ensure CLAG ID was set", bond1.getClagId(), equalTo(1));

    assertThat(
        "Ensure trunk VLAN IDs were set",
        bond2.getBridge().getVids(),
        equalTo(IntegerSpace.of(Range.closed(3, 5))));
  }

  @Test
  public void testDnsExtraction() throws IOException {
    CumulusNcluConfiguration vc = parseVendorConfig("cumulus_nclu_dns");

    assertThat(vc.getIpv4Nameservers(), contains(Ip.parse("192.0.2.3"), Ip.parse("192.0.2.4")));
  }

  @Test
  public void testHostname() throws IOException {
    String filename = "cumulus_nclu_hostname";
    String hostname = "custom_hostname";
    Batfish batfish = getBatfishForConfigurationNames(filename);
    assertThat(batfish.loadConfigurations(), hasEntry(equalTo(hostname), hasHostname(hostname)));
  }
}
