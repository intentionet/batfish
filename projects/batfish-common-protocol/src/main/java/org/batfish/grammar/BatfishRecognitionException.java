package org.batfish.grammar;

import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class BatfishRecognitionException extends RecognitionException {

  /** */
  private static final long serialVersionUID = 1L;

  public BatfishRecognitionException(
      Recognizer<?, ?> recognizer, IntStream input, ParserRuleContext ctx) {
    super(null, recognizer, input, ctx);
  }

}
