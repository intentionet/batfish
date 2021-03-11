package org.batfish.grammar.cisco_asa;

import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.cisco_asa.AsaParser.Cisco_configurationContext;

public class AsaCombinedParser extends BatfishCombinedParser<AsaParser, AsaLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(AsaLexer.NEWLINE, "\n");

  public AsaCombinedParser(String input, GrammarSettings settings, ConfigurationFormat format) {
    super(
        AsaParser.class,
        AsaLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
    boolean cadant = format == ConfigurationFormat.CADANT;
    _lexer.setAsa(format == ConfigurationFormat.CISCO_ASA);
    _lexer.setCadant(cadant);
    _lexer.setFoundry(format == ConfigurationFormat.FOUNDRY);
    _lexer.setIos(format == ConfigurationFormat.CISCO_IOS);
    _parser.setAsa(format == ConfigurationFormat.CISCO_ASA);
    _parser.setCadant(cadant);
    _parser.setMultilineBgpNeighbors(false);
  }

  @Override
  public Cisco_configurationContext parse() {
    return _parser.cisco_configuration();
  }
}
