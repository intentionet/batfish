package org.batfish.grammar.iptables;

import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.config.Settings;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;

public class IptablesCombinedParser extends BatfishCombinedParser<IptablesParser, IptablesLexer> {

  public IptablesCombinedParser(String input, Settings settings) {
    super(
        IptablesParser.class,
        IptablesLexer.class,
        input,
        settings,
        "\n",
        IptablesLexer.NEWLINE,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public ParserRuleContext parse() {
    return _parser.iptables_configuration();
  }
}
