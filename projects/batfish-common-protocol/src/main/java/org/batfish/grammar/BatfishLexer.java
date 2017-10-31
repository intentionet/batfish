package org.batfish.grammar;

import javax.annotation.Nullable;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.LexerNoViableAltException;

public abstract class BatfishLexer extends Lexer {

  public static final int UNMATCHABLE_TOKEN = Integer.MAX_VALUE;

  public static final int UNRECOGNIZED_LINE_TOKEN = Integer.MAX_VALUE - 1;

  private BatfishCombinedParser<?, ?> _parser;

  @Nullable private BatfishLexerRecoveryStrategy _recoveryStrategy;

  public BatfishLexer(CharStream input) {
    super(input);
  }

  public String getMode() {
    return this.getModeNames()[_mode];
  }

  @Nullable
  public BatfishLexerRecoveryStrategy getRecoveryStrategy() {
    return _recoveryStrategy;
  }

  public void initErrorListener(BatfishCombinedParser<?, ?> parser) {
    _parser = parser;
    BatfishLexerErrorListener errorListener =
        new BatfishLexerErrorListener(this.getClass().getSimpleName(), parser);
    removeErrorListeners();
    addErrorListener(errorListener);
    _parser.setLexerErrorListener(errorListener);
  }

  @Override
  public void mode(int m) {
    _parser.updateTokenModes(_mode);
    super.mode(m);
  }

  /**
   * Print custom lexer state (should be overridden)
   *
   * @return Should print custom lexer state variables and their values
   */
  public String printStateVariables() {
    return "";
  }

  @Override
  public void recover(LexerNoViableAltException e) {
    if (_recoveryStrategy != null) {
      _recoveryStrategy.recover();
    } else {
      super.recover(e);
    }
  }

  public void setRecoveryStrategy(@Nullable BatfishLexerRecoveryStrategy recoveryStrategy) {
    _recoveryStrategy = recoveryStrategy;
  }
}
