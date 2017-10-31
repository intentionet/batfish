package org.batfish.grammar;

import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ParserATNSimulator;

/**
 * Clones an existing {@link ParserATNSimulator} used by a {@BatfishParser} and wraps the {@link
 * adaptivePredict} function with a recovery mechanism. Intended for use with a {@link
 * BatfishParser} employing a {@link BatfishANTLRErrorStrategy} as its error recovery strategy.
 */
public class BatfishParserATNSimulator extends ParserATNSimulator {

  private BatfishParser _parser;

  /**
   * Construct a {@link BatfishParserATNSimulator} from a {@link ParserATNSimulator}
   *
   * @param parent The {@link ParserATNSimulator} to emulate modulo {@link adpativePredict}
   */
  public BatfishParserATNSimulator(ParserATNSimulator parent) {
    super(parent.getParser(), parent.atn, parent.decisionToDFA, parent.getSharedContextCache());
    _parser = (BatfishParser) parser;
  }

  @Override
  public int adaptivePredict(TokenStream input, int decision, ParserRuleContext outerContext) {
    Integer result = null;
    do {
      try {
        result = super.adaptivePredict(input, decision, outerContext);
      } catch (NoViableAltException e) {
        // If adaptive prediction fails, throw out current line and try again.
        _parser.createErrorNodeLine();
      }
    } while (result == null);
    return result;
  }
}
