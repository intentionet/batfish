package org.batfish.grammar;

import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ParserATNSimulator;

public class BatfishParserATNSimulator extends ParserATNSimulator {

  private BatfishParser _parser;

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
        _parser.createErrorNodeLine();
      }
    } while (result == null);
    return result;
  }
}
