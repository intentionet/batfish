package org.batfish.grammar;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Token;

/**
 * Used by {@link BatfishLexer} to recover from lexing errors. Emits an unmatchable token during
 * lexing errors so error handling can be deferred to the parser.
 */
public class BatfishLexerRecoveryStrategy {

  /** Newline separator chars (CR, LF) */
  public static final Set<Integer> NEWLINES = newlines();

  /** Whitespace separator chars (space, tab) */
  public static final Set<Integer> WHITESPACE = whitespace();

  /** Newline and whitespace separator chars (CR, LF, space, tab) */
  public static final Set<Integer> WHITESPACE_AND_NEWLINES = whitespaceAndNewlines();

  private static Set<Integer> newlines() {
    return ImmutableSet.of((int) '\n', (int) '\r');
  }

  private static Set<Integer> whitespace() {
    return ImmutableSet.of((int) ' ', (int) '\t');
  }

  private static Set<Integer> whitespaceAndNewlines() {
    return ImmutableSet.<Integer>builder().addAll(whitespace()).addAll(newlines()).build();
  }

  private final BatfishLexer _lexer;

  private final Set<Integer> _separatorChars;

  /**
   * Construct a {@link BatfishLexerRecoveryStrategy} for given {@link lexer} using {@link
   * separatorChars} to mark end of invalid chars to be consumed and discarded.
   *
   * @param lexer The {@link BatfishLexer} using this strategy
   * @param separatorChars The chars used to mark the end (non-inclusive) of any string of invalid
   *     chars
   */
  public BatfishLexerRecoveryStrategy(BatfishLexer lexer, Set<Integer> separatorChars) {
    _lexer = lexer;
    _separatorChars =
        ImmutableSet.copyOf(Sets.union(separatorChars, Collections.singleton(IntStream.EOF)));
  }

  /**
   * Wrap current unmatchable char up to next char in provided separator chars in a {@link
   * BatfishLexer.UNMATCHABLE_TOKEN} and emit it.
   */
  public void recover() {
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
