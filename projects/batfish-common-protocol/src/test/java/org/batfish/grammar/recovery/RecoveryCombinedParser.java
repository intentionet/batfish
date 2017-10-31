package org.batfish.grammar.recovery;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.recovery.RecoveryParser.RecoveryContext;

public class RecoveryCombinedParser extends BatfishCombinedParser<RecoveryParser, RecoveryLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(RecoveryLexer.NEWLINE, "\n");

  public RecoveryCombinedParser(String input, GrammarSettings settings) {
    super(
        RecoveryParser.class,
        RecoveryLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public RecoveryContext parse() {
    return _parser.recovery();
  }
}
