package org.batfish.grammar.routing_table.ios;

import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.routing_table.ios.IosRoutingTableParser.Ios_routing_tableContext;

public class IosRoutingTableCombinedParser
    extends BatfishCombinedParser<IosRoutingTableParser, IosRoutingTableLexer> {

  public IosRoutingTableCombinedParser(String input, GrammarSettings settings) {
    super(
        IosRoutingTableParser.class,
        IosRoutingTableLexer.class,
        input,
        settings,
        "\n",
        IosRoutingTableLexer.NEWLINE,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Ios_routing_tableContext parse() {
    return _parser.ios_routing_table();
  }
}
