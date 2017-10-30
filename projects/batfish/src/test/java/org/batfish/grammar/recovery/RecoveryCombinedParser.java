package org.batfish.grammar.recovery;

import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.recovery.RecoveryParser.RecoveryContext;

public class RecoveryCombinedParser extends BatfishCombinedParser<RecoveryParser, RecoveryLexer> {

  public RecoveryCombinedParser(String input, GrammarSettings settings) {
    super(
        RecoveryParser.class,
        RecoveryLexer.class,
        input,
        settings,
        "\n",
        RecoveryLexer.NEWLINE,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public RecoveryContext parse() {
    return _parser.recovery();
  }
}
