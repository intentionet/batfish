package org.batfish.grammar.iptables;

import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.config.Settings;
import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;

public class IptablesCombinedParser extends BatfishCombinedParser<IptablesParser, IptablesLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(IptablesLexer.NEWLINE, "\n");

  public IptablesCombinedParser(String input, Settings settings) {
    super(
        IptablesParser.class,
        IptablesLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public ParserRuleContext parse() {
    return _parser.iptables_configuration();
  }
}
