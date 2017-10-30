package org.batfish.grammar;

import java.util.BitSet;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

public abstract class BatfishGrammarErrorListener implements ANTLRErrorListener {

  protected final BatfishCombinedParser<?, ?> _combinedParser;

  protected final String _grammarName;

  protected final GrammarSettings _settings;

  public BatfishGrammarErrorListener(String grammarName, BatfishCombinedParser<?, ?> parser) {
    _grammarName = grammarName;
    _combinedParser = parser;
    _settings = _combinedParser.getSettings();
  }

  @Override
  public void reportAmbiguity(
      Parser arg0, DFA arg1, int arg2, int arg3, boolean arg4, BitSet arg5, ATNConfigSet arg6) {}

  @Override
  public void reportAttemptingFullContext(
      Parser arg0, DFA arg1, int arg2, int arg3, BitSet arg4, ATNConfigSet arg5) {}

  @Override
  public void reportContextSensitivity(
      Parser arg0, DFA arg1, int arg2, int arg3, int arg4, ATNConfigSet arg5) {}
}
