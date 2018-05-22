package org.batfish.grammar;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.ParseTreeSentences;
import org.batfish.common.util.CommonUtil;

public class ParseTreePrettyPrinter implements ParseTreeListener {

  private BatfishCombinedParser<?, ?> _combinedParser;
  private ParserRuleContext _ctx;
  private int _indent;
  private ParseTreeSentences _ptSentences;
  private List<String> _ruleNames;
  private Vocabulary _vocabulary;

  private ParseTreePrettyPrinter(
      ParserRuleContext ctx, BatfishCombinedParser<?, ?> combinedParser) {
    Parser grammar = combinedParser.getParser();
    List<String> ruleNames = Arrays.asList(grammar.getRuleNames());
    _vocabulary = grammar.getVocabulary();
    _combinedParser = combinedParser;
    _ruleNames = ruleNames;
    _ctx = ctx;
    _ptSentences = new ParseTreeSentences();
    _indent = 0;
  }

  public static ParseTreeSentences getParseTreeSentences(
      ParserRuleContext ctx, BatfishCombinedParser<?, ?> combinedParser) {
    ParseTreeWalker walker = new ParseTreeWalker();
    ParseTreePrettyPrinter printer = new ParseTreePrettyPrinter(ctx, combinedParser);
    walker.walk(printer, ctx);
    return printer._ptSentences;
  }

  // Visible for testing
  static String printWithCharacterLimit(List<String> strings, int maxStringLength) {
    StringBuilder sb = new StringBuilder();

    // A limit of <= 0 is treated as effectively no limit
    if (maxStringLength <= 0) {
      maxStringLength = Integer.MAX_VALUE;
    }

    ListIterator<String> iter = strings.listIterator();
    while (maxStringLength > sb.length() && iter.hasNext()) {
      String string = iter.next();

      // Assume we're okay adding the whole string even if it pushes us over the maxStringLength
      sb.append(string);
      if (iter.hasNext()) {
        sb.append("\n");
      }
    }

    if (iter.hasNext()) {
      sb.append("and ");
      sb.append(strings.size() - iter.nextIndex());
      sb.append(" more line(s)");
    }
    return sb.toString();
  }

  public static String print(ParserRuleContext ctx, BatfishCombinedParser<?, ?> combinedParser) {
    int maxStringLength = combinedParser.getSettings().getMaxParseTreePrintLength();
    List<String> strings = getParseTreeSentences(ctx, combinedParser).getSentences();
    return printWithCharacterLimit(strings, maxStringLength);
  }

  @Override
  public void enterEveryRule(ParserRuleContext ctx) {
    if (ctx != _ctx) {
      _ptSentences.getSentences().add("");
    }
    for (int i = 0; i < _indent; i++) {
      _ptSentences.appendToLastSentence("  ");
    }
    _indent++;
    if (ctx.getParent() != null) {
      for (Field f : ctx.getParent().getClass().getFields()) {
        try {
          if (f.get(ctx.getParent()) == ctx) {
            _ptSentences.appendToLastSentence(f.getName() + " = ");
            break;
          }
        } catch (IllegalAccessException e) {
          // Should not be reachable, because we check that it's accessible.
        }
      }
    }
    String ruleName = _ruleNames.get(ctx.getRuleIndex());
    _ptSentences.appendToLastSentence("(" + ruleName);
  }

  @Override
  public void exitEveryRule(ParserRuleContext ctx) {
    _ptSentences.appendToLastSentence(")");
    _indent--;
  }

  @Override
  public void visitErrorNode(ErrorNode ctx) {
    String nodeText = CommonUtil.escape(ctx.getText());
    // _sb.append("\n");
    _ptSentences.getSentences().add("");
    for (int i = 0; i < _indent; i++) {
      _ptSentences.appendToLastSentence("  ");
    }
    int tokenType = ctx.getSymbol().getType();
    String tokenName;
    if (tokenType == Lexer.EOF) {
      tokenName = "EOF";
      _ptSentences.appendToLastSentence(tokenName + ":" + nodeText);
    } else if (tokenType == BatfishLexer.UNRECOGNIZED_LINE_TOKEN) {
      _ptSentences.appendToLastSentence("<UnrecognizedLine>:'" + nodeText + "'");
    } else {
      tokenName = _vocabulary.getSymbolicName(tokenType);
      _ptSentences.appendToLastSentence("<ErrorNode>:" + tokenName + ":'" + nodeText + "'");
    }
  }

  @Override
  public void visitTerminal(TerminalNode ctx) {
    String nodeText = CommonUtil.escape(ctx.getText());
    _ptSentences.getSentences().add("");
    for (int i = 0; i < _indent; i++) {
      _ptSentences.appendToLastSentence("  ");
    }
    Token t = ctx.getSymbol();
    int tokenType = t.getType();
    int modeAsInt = _combinedParser.getTokenMode(t);
    String mode;
    if (modeAsInt == Token.EOF) {
      mode = "<MANUAL/UNKNOWN>";
    } else {
      mode = _combinedParser.getLexer().getModeNames()[modeAsInt];
    }
    for (Field f : ctx.getParent().getClass().getFields()) {
      try {
        if (f.isAccessible() && f.get(ctx.getParent()) == ctx) {
          _ptSentences.appendToLastSentence(f.getName() + " = ");
          break;
        }
      } catch (Throwable e) {
        // Ignore and continue
      }
    }
    String tokenName;
    if (tokenType == -1) {
      tokenName = "EOF";
      _ptSentences.appendToLastSentence(tokenName + ":" + nodeText);
    } else {
      tokenName = _vocabulary.getSymbolicName(tokenType);
      _ptSentences.appendToLastSentence(tokenName + ":'" + nodeText + "'");
    }
    _ptSentences.appendToLastSentence("  <== mode:" + mode);
  }
}
