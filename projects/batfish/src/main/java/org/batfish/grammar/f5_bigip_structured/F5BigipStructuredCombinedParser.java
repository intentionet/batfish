package org.batfish.grammar.f5_bigip_structured;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.F5_bigip_structured_configurationContext;

public class F5BigipStructuredCombinedParser
    extends BatfishCombinedParser<F5BigipStructuredParser, F5BigipStructuredLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          F5BigipStructuredLexer.NEWLINE, "\n");

  public F5BigipStructuredCombinedParser(String input, GrammarSettings settings) {
    super(
        F5BigipStructuredParser.class,
        F5BigipStructuredLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public F5_bigip_structured_configurationContext parse() {
    return _parser.f5_bigip_structured_configuration();
  }
}
