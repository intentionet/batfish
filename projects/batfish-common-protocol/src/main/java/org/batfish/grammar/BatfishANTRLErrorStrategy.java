package org.batfish.grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

public class BatfishANTRLErrorStrategy extends DefaultErrorStrategy {

  private final List<String> _lines;

  private int _separatorToken;

  public BatfishANTRLErrorStrategy(
      int separatorToken, String minimumRequiredSeparatorText, String text) {
    _lines =
        Collections.unmodifiableList(
            Arrays.asList(text.split(Pattern.quote(minimumRequiredSeparatorText))));
    _separatorToken = separatorToken;
  }

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

      // Get the line number and separator text from the separator token
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
           * If not at the top level, error out to pop up a level
           */
          throw new InputMismatchException(recognizer);
        }

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
           * If not at the top level, error out to pop up a level
           */
          throw new InputMismatchException(recognizer);
        }

      default:
        return;
    }
  }
}
