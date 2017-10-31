package org.batfish.grammar.recovery;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.batfish.grammar.recovery.RecoveryParser.Block_statementContext;
import org.batfish.grammar.recovery.RecoveryParser.Inner_statementContext;
import org.batfish.grammar.recovery.RecoveryParser.Simple_statementContext;
import org.batfish.grammar.recovery.RecoveryParser.StatementContext;
import org.batfish.grammar.recovery.RecoveryParser.Tail_wordContext;

public final class RecoveryExtractor extends RecoveryParserBaseListener {

  private int _numBlockStatements;

  private int _numErrorNodes;

  private int _numInnerStatements;

  private int _numSimpleStatements;

  private int _numStatements;

  private int _numTailWords;

  @Override
  public void exitBlock_statement(Block_statementContext ctx) {
    _numBlockStatements++;
  }

  @Override
  public void exitInner_statement(Inner_statementContext ctx) {
    _numInnerStatements++;
  }

  @Override
  public void exitSimple_statement(Simple_statementContext ctx) {
    _numSimpleStatements++;
  }

  @Override
  public void exitStatement(StatementContext ctx) {
    _numStatements++;
  }

  @Override
  public void exitTail_word(Tail_wordContext ctx) {
    _numTailWords++;
  }

  public int getNumBlockStatements() {
    return _numBlockStatements;
  }

  public int getNumErrorNodes() {
    return _numErrorNodes;
  }

  public int getNumInnerStatements() {
    return _numInnerStatements;
  }

  public int getNumSimpleStatements() {
    return _numSimpleStatements;
  }

  public int getNumStatements() {
    return _numStatements;
  }

  public int getNumTailWords() {
    return _numTailWords;
  }

  @Override
  public void visitErrorNode(ErrorNode node) {
    _numErrorNodes++;
  }
}
