package org.batfish.grammar;

public interface GrammarSettings {

  boolean getDisableUnrecognized();

  int getMaxParserContextLines();

  int getMaxParserContextTokens();

  boolean getPrintParseTree();

  boolean getThrowOnLexerError();

  boolean getThrowOnParserError();

  void setDisableUnrecognized(boolean b);

  void setPrintParseTree(boolean b);

  void setThrowOnLexerError(boolean b);

  void setThrowOnParserError(boolean b);
}
