package org.batfish.grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Intended as a replacement for default ANTLR parser error recovery strategy. The basic idea here
 * is to throw out any lines containing invalidities, and try to parse as if those lines were never
 * there. Meanwhile, each invalid lines should show up in the parse tree as {@link ErrorNode} at an
 * appropriate scope. The advantage of this strategy is that we do not unnecessarily pop back up to
 * the top level as soon as an error occurs.
 */
public class BatfishANTLRErrorStrategy extends DefaultErrorStrategy {

  /**
   * Construct a factory for making instances of {@link BatfishANTLRErrorStrategy} with supplied
   * {@link separatorToken} and {@link minimRequiredSeparatorText}.
   */
  public static class BatfishANTLRErrorStrategyFactory {

    private final String _minimumRequiredSeparatorText;

    private final int _separatorToken;

    public BatfishANTLRErrorStrategyFactory(
        int separatorToken, String minimumRequiredSeparatorText) {
      _minimumRequiredSeparatorText = minimumRequiredSeparatorText;
      _separatorToken = separatorToken;
    }

    public BatfishANTLRErrorStrategy build(String text) {
      return new BatfishANTLRErrorStrategy(_separatorToken, _minimumRequiredSeparatorText, text);
    }
  }

  /**
   * Generic {@link RecognitionException} used by {@link BatfishANTLRErrorStrategy} to be thrown in
   * situations not easily mappable to traditional ANTLR parser error conditions.
   */
  private static class BatfishRecognitionException extends RecognitionException {

    /** */
    private static final long serialVersionUID = 1L;

    public BatfishRecognitionException(
        Recognizer<?, ?> recognizer, IntStream input, ParserRuleContext ctx) {
      super(null, recognizer, input, ctx);
    }
  }

  private final List<String> _lines;

  private int _separatorToken;

  /**
   * Construct a {@link BatfishANTLRErrorStrategy} that throws out invalid lines from as delimited
   * by {@link separatorToken}. The {@link minimumRequiredSeparatorText} is used to split {@link
   * text} into lines, which {@link BatfishANTLRErrorStrategy} uses when creating instances of
   * {@link ErrorNode} from discarded lines.
   *
   * @param separatorToken Token that delimits lines
   * @param minimumRequiredSeparatorText Minimal string representation of {@link separatorToken}
   * @param text {@link text of file to split into lines}
   */
  private BatfishANTLRErrorStrategy(
      int separatorToken, String minimumRequiredSeparatorText, String text) {
    _lines =
        Collections.unmodifiableList(
            Arrays.asList(text.split(Pattern.quote(minimumRequiredSeparatorText))));
    _separatorToken = separatorToken;
  }

  /**
   * Consume all tokens until the next token is one expected by the current rule. Each line (as
   * delimited by supplied separator token) starting from the current line up to the last line
   * consumed is placed in an {@link ErrorNode} and inserted as a child of the current rule.
   *
   * @param recognizer The {@link Parser} to whom to delegate creation of each {@link ErrorNode}
   */
  private void consumeBlocksUntilWanted(Parser recognizer) {
    IntervalSet expecting = recognizer.getExpectedTokens();
    IntervalSet whatFollowsLoopIterationOrRule = expecting.or(getErrorRecoverySet(recognizer));

    IntervalSet followSet = new IntervalSet();
    followSet.add(_separatorToken);
    int nextToken;
    do {
      // Eat tokens until we are at the end of the line
      consumeUntil(recognizer, followSet);

      // Get the line number and separator text from the separator token
      Token separatorToken = recognizer.getCurrentToken();
      int lineIndex = separatorToken.getLine() - 1;
      String separator = separatorToken.getText();

      // Insert the current line as an {@link ErrorNode} as a child of the current rule
      createErrorNode(recognizer, recognizer.getContext(), lineIndex, separator);

      // Eat the separator token
      recognizer.consume();

      nextToken = recognizer.getInputStream().LA(1);
    } while (!whatFollowsLoopIterationOrRule.contains(nextToken));
  }

  /**
   * Create an error node with the text of the current line and insert it into parse tree
   *
   * @param recognizer The recognizer with which to create the error node
   * @param lineIndex The 0-based line of input whose text will go in the error node
   * @param separator The text of the separator to append to the input line text
   * @return The token contained in the error node
   */
  private Token createErrorNode(
      Parser recognizer, ParserRuleContext ctx, int lineIndex, String separator) {
    String lineText = _lines.get(lineIndex) + separator;
    Token lineToken =
        recognizer
            .getTokenFactory()
            .create(
                new Pair<TokenSource, CharStream>(null, null),
                BatfishLexer.UNRECOGNIZED_LINE_TOKEN,
                lineText,
                Lexer.DEFAULT_TOKEN_CHANNEL,
                -1,
                -1,
                lineIndex,
                0);
    ErrorNode errorNode = recognizer.createErrorNode(ctx, lineToken);
    ctx.addErrorNode(errorNode);
    return lineToken;
  }

  /**
   * Attempt to get {@link recognizer} into a state where parsing can continue by throwing away the
   * current line and abandoning the current rule if possible. If at root already, the current line
   * is placed into an {@link ErrorNode} at the root and parsing continues at the next line (first
   * base case). If in a child rule with a parent that A) has its own parent and B) started on the
   * same line; then an exception is thrown to defer cleanup to the parent (recursive case). In any
   * other case, this rule is removed from its parent, and the current line is inserted as an {@link
   * ErrorNode} in its place as a child of that parent (second base case).
   *
   * @param recognizer The {@link Parser} needing to perform recovery
   * @return If base case applies, returns a {@link Token} whose containing the text of the
   *     created @{link ErrorNode}.
   */
  private Token recover(Parser recognizer) {
    lastErrorIndex = recognizer.getInputStream().index();
    if (lastErrorStates == null) {
      lastErrorStates = new IntervalSet();
    }
    lastErrorStates.add(recognizer.getState());
    IntervalSet followSet = new IntervalSet();
    followSet.add(_separatorToken);
    consumeUntil(recognizer, followSet);

    // Get the line number and separator text from the separator token
    Token separatorToken = recognizer.getCurrentToken();
    int lineIndex = separatorToken.getLine() - 1;
    String separator = separatorToken.getText();

    ParserRuleContext ctx = recognizer.getContext();
    ParserRuleContext parent = ctx.getParent();
    if (parent == null) {
      recognizer.consume();
      return createErrorNode(recognizer, parent, lineIndex, separator);
    } else if (parent.parent != null
        && (parent.getStart() == null || parent.getStart().getLine() == ctx.getStart().getLine())) {
      throw new BatfishRecognitionException(recognizer, recognizer.getInputStream(), parent);
    } else {
      List<ParseTree> parentChildren = parent.children;
      parentChildren.remove(parentChildren.size() - 1);
      recognizer.consume();
      return createErrorNode(recognizer, parent, lineIndex, separator);
    }
  }

  @Override
  public void recover(Parser recognizer, RecognitionException e) {
    recover(recognizer);
  }

  /**
   * Recover from adaptive prediction failure (when more than one token is needed for rule
   * prediction, and the first token by itself is insufficient to determine an error has occured) by
   * throwing away lines until adaptive prediction succeeds or there is nothing left to throw away.
   * Each discarded line is inserted as a child of the current rule as an {@link ErrorNode}.
   *
   * @param recognizer The {@link Parser} for whom adaptive prediction has failed
   */
  public void recoverInCurrentNode(Parser recognizer) {
    beginErrorCondition(recognizer);
    lastErrorIndex = recognizer.getInputStream().index();
    if (lastErrorStates == null) {
      lastErrorStates = new IntervalSet();
    }
    lastErrorStates.add(recognizer.getState());
    IntervalSet followSet = new IntervalSet();
    followSet.add(_separatorToken);
    consumeUntil(recognizer, followSet);

    // Get the line number and separator text from the separator token
    Token separatorToken = recognizer.getCurrentToken();
    int lineIndex = separatorToken.getLine() - 1;
    String separator = separatorToken.getText();

    ParserRuleContext ctx = recognizer.getContext();
    recognizer.consume();
    createErrorNode(recognizer, ctx, lineIndex, separator);
    endErrorCondition(recognizer);
  }

  @Override
  public Token recoverInline(Parser recognizer) throws RecognitionException {
    return recover(recognizer);
  }

  @Override
  public void reportError(Parser recognizer, RecognitionException e) {
    if (!(e instanceof BatfishRecognitionException)) {
      recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
    }
  }

  @Override
  public void sync(Parser recognizer) throws RecognitionException {
    /*
     * Copied from super
     */
    ATNState s = recognizer.getInterpreter().atn.states.get(recognizer.getState());
    if (inErrorRecoveryMode(recognizer)) {
      return;
    }
    TokenStream tokens = recognizer.getInputStream();
    int la = tokens.LA(1);
    IntervalSet nextTokens = recognizer.getATN().nextTokens(s);
    if (nextTokens.contains(Token.EPSILON) || nextTokens.contains(la)) {
      return;
    }

    boolean topLevel = recognizer.getContext().parent == null;

    switch (s.getStateType()) {
      case ATNState.BLOCK_START:
      case ATNState.STAR_BLOCK_START:
      case ATNState.PLUS_BLOCK_START:
      case ATNState.STAR_LOOP_ENTRY:
      case ATNState.PLUS_LOOP_BACK:
      case ATNState.STAR_LOOP_BACK:
        if (topLevel) {
          /*
           * When at top level, we cannot pop up. So consume every "line" until we have one that
           * starts with a token acceptable at the top level.
           */
          reportUnwantedToken(recognizer);
          consumeBlocksUntilWanted(recognizer);
          return;
        } else {
          /*
           * If not at the top level, error out to pop up a level. This may repeat until the next
           * token is acceptable at the given level.
           */
          throw new InputMismatchException(recognizer);
        }

      default:
        return;
    }
  }
}
