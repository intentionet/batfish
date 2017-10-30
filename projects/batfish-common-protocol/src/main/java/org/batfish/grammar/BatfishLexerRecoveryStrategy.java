package org.batfish.grammar;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.Token;
import org.batfish.common.util.CommonUtil;

public class BatfishLexerRecoveryStrategy {

  public static final Set<Integer> NEWLINES = newlines();

  public static final Set<Integer> WHITESPACE = whitespace();

  public static final Set<Integer> WHITESPACE_AND_NEWLINES = whitespaceAndNewlines();

  private static Set<Integer> newlines() {
    return ImmutableSet.of((int) '\n', (int) '\r');
  }

  private static Set<Integer> whitespace() {
    return ImmutableSet.of((int) ' ', (int) '\t');
  }

  private static Set<Integer> whitespaceAndNewlines() {
    return CommonUtil.immutableUnion(whitespace(), newlines(), HashSet::new);
  }

  private final BatfishLexer _lexer;

  private final Set<Integer> _separatorChars;

  public BatfishLexerRecoveryStrategy(BatfishLexer lexer, Set<Integer> separatorChars) {
    _lexer = lexer;
    _separatorChars =
        CommonUtil.immutableUnion(
            separatorChars, Collections.singleton(IntStream.EOF), HashSet::new);
  }

  public void recover(LexerNoViableAltException e) {
    int tokenStartMarker = _lexer._input.mark();
    try {
      _lexer._token = null;
      _lexer._channel = Token.DEFAULT_CHANNEL;
      _lexer._tokenStartCharIndex = _lexer._input.index();
      _lexer._tokenStartCharPositionInLine = _lexer.getInterpreter().getCharPositionInLine();
      _lexer._tokenStartLine = _lexer.getInterpreter().getLine();
      _lexer._text = null;
      _lexer._type = BatfishLexer.UNMATCHABLE_TOKEN;
      for (int nextChar = _lexer._input.LA(1);
          !_separatorChars.contains(nextChar);
          nextChar = _lexer._input.LA(1)) {
        if (nextChar == IntStream.EOF) {
          _lexer._hitEOF = true;
          _lexer.emitEOF();
          return;
        }
        _lexer.getInterpreter().consume(_lexer._input);
      }
      _lexer.emit();
    } finally {
      // make sure we release marker after match or
      // unbuffered char stream will keep buffering
      _lexer._input.release(tokenStartMarker);
    }
  }
}
