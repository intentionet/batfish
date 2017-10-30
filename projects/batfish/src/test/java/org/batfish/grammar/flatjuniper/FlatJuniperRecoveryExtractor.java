package org.batfish.grammar.flatjuniper;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Set_lineContext;

public class FlatJuniperRecoveryExtractor extends FlatJuniperParserBaseListener {

  private int _numErrorNodes;

  private int _numSets;

  @Override
  public void exitSet_line(Set_lineContext ctx) {
    _numSets++;
  }

  public int getNumErrorNodes() {
    return _numErrorNodes;
  }

  public int getNumSets() {
    return _numSets;
  }

  @Override
  public void visitErrorNode(ErrorNode node) {
    _numErrorNodes++;
  }
}
