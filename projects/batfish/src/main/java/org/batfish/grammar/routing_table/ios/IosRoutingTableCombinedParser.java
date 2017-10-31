package org.batfish.grammar.routing_table.ios;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.routing_table.ios.IosRoutingTableParser.Ios_routing_tableContext;

public class IosRoutingTableCombinedParser
    extends BatfishCombinedParser<IosRoutingTableParser, IosRoutingTableLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          IosRoutingTableLexer.NEWLINE, "\n");

  public IosRoutingTableCombinedParser(String input, GrammarSettings settings) {
    super(
        IosRoutingTableParser.class,
        IosRoutingTableLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Ios_routing_tableContext parse() {
    return _parser.ios_routing_table();
  }
}
