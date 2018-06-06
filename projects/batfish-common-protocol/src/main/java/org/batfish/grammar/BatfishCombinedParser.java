package org.batfish.grammar;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.batfish.common.BatfishException;

public abstract class BatfishCombinedParser<P extends BatfishParser, L extends BatfishLexer> {

  private int _currentModeStart;

  private final List<String> _errors;

  private String _input;

  protected L _lexer;

  private BatfishLexerErrorListener _lexerErrorListener;

  protected P _parser;

  private BatfishParserErrorListener _parserErrorListener;

  private GrammarSettings _settings;

  private List<Integer> _tokenModes;

  protected CommonTokenStream _tokens;

  private final List<String> _warnings;

  public BatfishCombinedParser(
      Class<P> pClass, Class<L> lClass, String input, GrammarSettings settings) {
    _settings = settings;
    _tokenModes = new ArrayList<>();
    _currentModeStart = 0;
    _warnings = new ArrayList<>();
    _errors = new ArrayList<>();
    _input = input;
    CharStream inputStream = CharStreams.fromString(input);
    try {
      _lexer = lClass.getConstructor(CharStream.class).newInstance(inputStream);
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException e) {
      throw new BatfishException("Error constructing lexer using reflection", e);
    }
    _lexer.initErrorListener(this);
    _tokens = new CommonTokenStream(_lexer);
    try {
      _parser = pClass.getConstructor(TokenStream.class).newInstance(_tokens);
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException e) {
      throw new Error(e);
    }
    _parser.initErrorListener(this);
    _parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
  }

  public BatfishCombinedParser(
      Class<P> pClass,
      Class<L> lClass,
      String input,
      GrammarSettings settings,
      BatfishANTLRErrorStrategy.BatfishANTLRErrorStrategyFactory batfishANTLRErrorStrategyFactor,
      Set<Integer> separatorChars) {
    this(pClass, lClass, input, settings);
    /*
     * Do not supply recovery infrastructure with associated overhead unless recovery is actually
     * enabled.
     */
    if (!settings.getDisableUnrecognized()) {
      _parser.setInterpreter(new BatfishParserATNSimulator(_parser.getInterpreter()));
      _parser.setErrorHandler(batfishANTLRErrorStrategyFactor.build(_input));
      _lexer.setRecoveryStrategy(new BatfishLexerRecoveryStrategy(_lexer, separatorChars));
    }
  }

  public static String escape(String offendingTokenText) {
    return offendingTokenText.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
  }

  public List<String> getErrors() {
    return _errors;
  }

  public String getInput() {
    return _input;
  }

  public L getLexer() {
    return _lexer;
  }

  public BatfishLexerErrorListener getLexerErrorListener() {
    return _lexerErrorListener;
  }

  public P getParser() {
    return _parser;
  }

  public BatfishParserErrorListener getParserErrorListener() {
    return _parserErrorListener;
  }

  public GrammarSettings getSettings() {
    return _settings;
  }

  public int getTokenMode(Token t) {
    int tokenIndex = t.getTokenIndex();
    if (tokenIndex == -1) {
      // token probably added manually, not by parser
      return -1;
    }
    if (tokenIndex < _tokenModes.size()) {
      return _tokenModes.get(tokenIndex);
    } else {
      return _lexer._mode;
    }
  }

  public CommonTokenStream getTokens() {
    return _tokens;
  }

  public List<String> getWarnings() {
    return _warnings;
  }

  public abstract ParserRuleContext parse();

  public void setLexerErrorListener(BatfishLexerErrorListener lexerErrorListener) {
    _lexerErrorListener = lexerErrorListener;
  }

  public void setParserErrorListener(BatfishParserErrorListener parserErrorListener) {
    _parserErrorListener = parserErrorListener;
  }

  public void updateTokenModes(int mode) {
    for (int i = _currentModeStart; i <= _tokens.size(); i++) {
      _tokenModes.add(mode);
    }
    _currentModeStart = _tokens.size() + 1;
  }
}
