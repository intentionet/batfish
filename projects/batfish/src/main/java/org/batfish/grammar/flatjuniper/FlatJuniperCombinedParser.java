package org.batfish.grammar.flatjuniper;

import org.batfish.config.Settings;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;

public class FlatJuniperCombinedParser
    extends BatfishCombinedParser<FlatJuniperParser, FlatJuniperLexer> {

  public FlatJuniperCombinedParser(String input, Settings settings) {
    super(
        FlatJuniperParser.class,
        FlatJuniperLexer.class,
        input,
        settings,
        "\n",
        FlatJuniperLexer.NEWLINE,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Flat_juniper_configurationContext parse() {
    return _parser.flat_juniper_configuration();
  }
}
