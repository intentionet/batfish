package org.batfish.grammar.routing_table.nxos;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.routing_table.nxos.NxosRoutingTableParser.Nxos_routing_tableContext;

public class NxosRoutingTableCombinedParser
    extends BatfishCombinedParser<NxosRoutingTableParser, NxosRoutingTableLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          NxosRoutingTableLexer.NEWLINE, "\n");

  public NxosRoutingTableCombinedParser(String input, GrammarSettings settings) {
    super(
        NxosRoutingTableParser.class,
        NxosRoutingTableLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Nxos_routing_tableContext parse() {
    return _parser.nxos_routing_table();
  }
}
