package org.batfish.grammar;

import javax.annotation.Nullable;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class BatfishParser extends Parser {

  public BatfishParser(TokenStream input) {
    super(input);
  }

  @Override
  public Token consume() {
    Token o = getCurrentToken();
    if (o.getType() != EOF) {
      getInputStream().consume();
    }
    boolean hasListener = _parseListeners != null && !_parseListeners.isEmpty();
    if ((_buildParseTrees || hasListener) && !_errHandler.inErrorRecoveryMode(this)) {
      TerminalNode node = _ctx.addChild(createTerminalNode(_ctx, o));
      if (_parseListeners != null) {
        for (ParseTreeListener listener : _parseListeners) {
          listener.visitTerminal(node);
        }
      }
    }
    return o;
  }

  @Nullable
  public String getStateInfo() {
    return null;
  }

  public void initErrorListener(BatfishCombinedParser<?, ?> parser) {
    BatfishParserErrorListener errorListener =
        new BatfishParserErrorListener(this.getClass().getSimpleName(), parser);
    removeErrorListeners();
    addErrorListener(errorListener);
    parser.setParserErrorListener(errorListener);
  }

  public void createErrorNodeLine() {
    ((BatfishANTLRErrorStrategy) _errHandler).recoverInCurrentNode(this);
  }
}
