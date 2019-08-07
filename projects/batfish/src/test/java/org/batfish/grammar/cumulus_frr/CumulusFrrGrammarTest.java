package org.batfish.grammar.cumulus_frr;

import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Cumulus_frr_configurationContext;
import org.batfish.representation.cumulus.CumulusNcluConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class CumulusFrrGrammarTest {
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cumulus_frr/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static CumulusNcluConfiguration parseVendorConfig(String filename) {
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    return parseVendorConfig(filename, settings);
  }

  private static CumulusNcluConfiguration parseVendorConfig(
      String filename, GrammarSettings settings) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + filename);
    CumulusNcluConfiguration configuration = new CumulusNcluConfiguration();
    CumulusFrrCombinedParser parser = new CumulusFrrCombinedParser(src, settings, 1, 0);
    Cumulus_frr_configurationContext ctxt = parser.parse();
    ParseTreeWalker walker = new BatfishParseTreeWalker(parser);
    Warnings w = new Warnings();
    CumulusFrrConfigurationBuilder cb = new CumulusFrrConfigurationBuilder(configuration, w);
    walker.walk(cb, ctxt);
    return cb.getVendorConfiguration();
  }

  private static CumulusNcluConfiguration parse(String src) {
    Settings settings = new Settings();
    settings.setDisableUnrecognized(true);
    settings.setThrowOnLexerError(true);
    settings.setThrowOnParserError(true);

    CumulusNcluConfiguration configuration = new CumulusNcluConfiguration();
    CumulusFrrCombinedParser parser = new CumulusFrrCombinedParser(src, settings, 1, 0);
    Cumulus_frr_configurationContext ctxt = parser.parse();
    ParseTreeWalker walker = new BatfishParseTreeWalker(parser);
    Warnings w = new Warnings();
    CumulusFrrConfigurationBuilder cb = new CumulusFrrConfigurationBuilder(configuration, w);
    walker.walk(cb, ctxt);
    return cb.getVendorConfiguration();
  }

  @Test
  public void testCumulusFrrVrf() {
    CumulusNcluConfiguration config = parse("vrf NAME\n exits-vrf");
    assertThat(config.getVrfs().keySet(), equalTo(ImmutableSet.of("NAME")));
  }
}
