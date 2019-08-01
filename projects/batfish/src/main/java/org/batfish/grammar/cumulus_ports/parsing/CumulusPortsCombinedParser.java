package org.batfish.grammar.cumulus_ports.parsing;

import org.batfish.grammar.BatfishANTLRErrorStrategy;
import org.batfish.grammar.BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishLexerRecoveryStrategy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.cumulus_ports.CumulusPortsLexer;
import org.batfish.grammar.cumulus_ports.CumulusPortsParser;
import org.batfish.grammar.cumulus_ports.CumulusPortsParser.Cumulus_ports_configurationContext;

public class CumulusPortsCombinedParser
    extends BatfishCombinedParser<CumulusPortsParser, CumulusPortsLexer> {

  private static final BatfishANTLRErrorStrategyFactory NEWLINE_BASED_RECOVERY =
      new BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory(
          CumulusPortsLexer.NEWLINE, "\n");

  public CumulusPortsCombinedParser(String input, GrammarSettings settings) {
    super(
        CumulusPortsParser.class,
        CumulusPortsLexer.class,
        input,
        settings,
        NEWLINE_BASED_RECOVERY,
        BatfishLexerRecoveryStrategy.WHITESPACE_AND_NEWLINES);
  }

  @Override
  public Cumulus_ports_configurationContext parse() {
    return _parser.cumulus_ports_configuration();
  }

  void setOffset(int offset) {
    _lexer.getInputStream().seek(offset);
  }

  void setLine(int line) {
    _lexer.getInterpreter().setLine(line);
  }
}
