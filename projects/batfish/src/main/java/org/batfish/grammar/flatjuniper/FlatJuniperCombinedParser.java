package org.batfish.grammar.flatjuniper;

import org.batfish.config.Settings;
import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;

public class FlatJuniperCombinedParser
    extends BatfishCombinedParser<FlatJuniperParser, FlatJuniperLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          FlatJuniperLexer.NEWLINE, "\n");

  public FlatJuniperCombinedParser(String input, Settings settings) {
    super(
        FlatJuniperParser.class,
        FlatJuniperLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Flat_juniper_configurationContext parse() {
    return _parser.flat_juniper_configuration();
  }
}
