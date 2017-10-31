package org.batfish.grammar.flatvyos;

import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.config.Settings;
import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;

public class FlatVyosCombinedParser extends BatfishCombinedParser<FlatVyosParser, FlatVyosLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(FlatVyosLexer.NEWLINE, "\n");

  public FlatVyosCombinedParser(String input, Settings settings) {
    super(
        FlatVyosParser.class,
        FlatVyosLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public ParserRuleContext parse() {
    return _parser.flat_vyos_configuration();
  }
}
