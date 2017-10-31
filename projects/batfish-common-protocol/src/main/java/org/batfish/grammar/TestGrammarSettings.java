package org.batfish.grammar;

public class TestGrammarSettings implements GrammarSettings {

  private final boolean _disableUnrecognized;

  private final int _maxParserContextLines;

  private final int _maxParserContextTokens;

  private final boolean _printParseTree;

  private final boolean _throwOnLexerError;

  private final boolean _throwOnParserError;

  public TestGrammarSettings(
      boolean disableUnrecognized,
      int maxParserContextLines,
      int maxParserContextTokens,
      boolean printParseTree,
      boolean throwOnLexerError,
      boolean throwOnParserError) {
    _disableUnrecognized = disableUnrecognized;
    _maxParserContextLines = maxParserContextLines;
    _maxParserContextTokens = maxParserContextTokens;
    _printParseTree = printParseTree;
    _throwOnLexerError = throwOnLexerError;
    _throwOnParserError = throwOnParserError;
  }

  public boolean getDisableUnrecognized() {
    return _disableUnrecognized;
  }

  public int getMaxParserContextLines() {
    return _maxParserContextLines;
  }

  public int getMaxParserContextTokens() {
    return _maxParserContextTokens;
  }

  public boolean getPrintParseTree() {
    return _printParseTree;
  }

  public boolean getThrowOnLexerError() {
    return _throwOnLexerError;
  }

  public boolean getThrowOnParserError() {
    return _throwOnParserError;
  }

  @Override
  public void setDisableUnrecognized(boolean b) {
    throw new UnsupportedOperationException("immutable");
  }

  @Override
  public void setPrintParseTree(boolean b) {
    throw new UnsupportedOperationException("immutable");
  }

  @Override
  public void setThrowOnLexerError(boolean b) {
    throw new UnsupportedOperationException("immutable");
  }

  @Override
  public void setThrowOnParserError(boolean b) {
    throw new UnsupportedOperationException("immutable");
  }
}
