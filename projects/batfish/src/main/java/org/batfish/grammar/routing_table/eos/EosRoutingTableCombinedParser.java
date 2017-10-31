package org.batfish.grammar.routing_table.eos;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.routing_table.eos.EosRoutingTableParser.Eos_routing_tableContext;

public class EosRoutingTableCombinedParser
    extends BatfishCombinedParser<EosRoutingTableParser, EosRoutingTableLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          EosRoutingTableLexer.NEWLINE, "\n");

  public EosRoutingTableCombinedParser(String input, GrammarSettings settings) {
    super(
        EosRoutingTableParser.class,
        EosRoutingTableLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Eos_routing_tableContext parse() {
    return _parser.eos_routing_table();
  }
}
