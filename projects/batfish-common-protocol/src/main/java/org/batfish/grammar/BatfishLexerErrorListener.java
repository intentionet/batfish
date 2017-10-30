package org.batfish.grammar;

import java.util.Arrays;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.batfish.common.DebugBatfishException;

public class BatfishLexerErrorListener extends BatfishGrammarErrorListener {

  public BatfishLexerErrorListener(String grammarName, BatfishCombinedParser<?, ?> parser) {
    super(grammarName, parser);
  }

  @Override
  public void syntaxError(
      Recognizer<?, ?> recognizer,
      Object offendingSymbol,
      int line,
      int charPositionInLine,
      String msg,
      RecognitionException e) {
    if (!_settings.getDisableUnrecognized()) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    BatfishParser parser = _combinedParser.getParser();
    BatfishLexer lexer = _combinedParser.getLexer();
    List<String> ruleNames = Arrays.asList(parser.getRuleNames());
    ParserRuleContext ctx = parser.getContext();
    String ruleStack = ctx.toString(ruleNames);
    sb.append(
        "lexer: " + _grammarName + ": line " + line + ":" + charPositionInLine + ": " + msg + "\n");
    sb.append("Current rule stack: '" + ruleStack + "'.\n");
    if (ctx.getStart() != null) {
      sb.append(
          "Current rule starts at: line: "
              + ctx.getStart().getLine()
              + ", col "
              + ctx.getStart().getCharPositionInLine()
              + "\n");
    }
    sb.append("Parse tree for current rule:\n");
    sb.append(ParseTreePrettyPrinter.print(ctx, _combinedParser) + "\n");
    sb.append("Lexer mode: " + lexer.getMode() + "\n");
    sb.append("Lexer state variables:\n");
    sb.append(lexer.printStateVariables());

    // collect context from text
    String text = _combinedParser.getInput();
    String[] lines = text.split("\n", -1);
    int errorLineIndex = line - 1;
    int errorContextStartLine = Math.max(errorLineIndex - _settings.getMaxParserContextLines(), 0);
    int errorContextEndLine =
        Math.min(errorLineIndex + _settings.getMaxParserContextLines(), lines.length);
    sb.append("Error context lines:\n");
    for (int i = errorContextStartLine; i < errorLineIndex; i++) {
      sb.append(String.format("%-11s%s\n", "   " + (i + 1) + ":", lines[i]));
    }
    sb.append(
        String.format("%-11s%s\n", ">>>" + (errorLineIndex + 1) + ":", lines[errorLineIndex]));
    for (int i = errorLineIndex + 1; i <= errorContextEndLine && i < lines.length; i++) {
      sb.append(String.format("%-11s%s\n", "   " + (i + 1) + ":", lines[i]));
    }

    String error = sb.toString();
    if (_settings.getThrowOnLexerError()) {
      throw new DebugBatfishException("\n" + error);
    } else {
      _combinedParser.getErrors().add(error);
    }
  }
}
