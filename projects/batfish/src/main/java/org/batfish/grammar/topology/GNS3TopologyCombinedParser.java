package org.batfish.grammar.topology;

import org.batfish.config.Settings;
import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.topology.GNS3TopologyParser.TopologyContext;

public class GNS3TopologyCombinedParser
    extends BatfishCombinedParser<GNS3TopologyParser, GNS3TopologyLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          GNS3TopologyLexer.NEWLINE, "\n");

  public GNS3TopologyCombinedParser(String input, Settings settings) {
    super(
        GNS3TopologyParser.class,
        GNS3TopologyLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public TopologyContext parse() {
    return _parser.topology();
  }
}
