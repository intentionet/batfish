package org.batfish.specifier.parboiled;

final class DifferenceInterfaceAstNode extends SetOpInterfaceAstNode {

  DifferenceInterfaceAstNode(InterfaceAstNode left, InterfaceAstNode right) {
    _left = left;
    _right = right;
  }

  @Override
  public <T> T accept(AstNodeVisitor<T> visitor) {
    return visitor.visitDifferenceInterfaceAstNode(this);
  }

  @Override
  public <T> T accept(InterfaceAstNodeVisitor<T> visitor) {
    return visitor.visitDifferenceInterfaceAstNode(this);
  }
}
