package org.batfish.specifier.parboiled;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

class InterfaceGroupInterfaceAstNode implements InterfaceAstNode {
  private final String _interfaceGroup;
  private final String _referenceBook;

  InterfaceGroupInterfaceAstNode(AstNode interfaceGroup, AstNode referenceBook) {
    checkArgument(interfaceGroup instanceof StringAstNode, "interfaceGroup must be a string");
    checkArgument(referenceBook instanceof StringAstNode, "referenceBook must be a string");
    _interfaceGroup = ((StringAstNode) interfaceGroup).getStr();
    _referenceBook = ((StringAstNode) referenceBook).getStr();
  }

  InterfaceGroupInterfaceAstNode(String interfaceGroup, String referenceBook) {
    _interfaceGroup = interfaceGroup;
    _referenceBook = referenceBook;
  }

  @Override
  public <T> T accept(AstNodeVisitor<T> visitor) {
    return visitor.visitInterfaceGroupInterfaceSpecAstNode(this);
  }

  @Override
  public <T> T accept(InterfaceAstNodeVisitor<T> visitor) {
    return visitor.visitInterfaceGroupInterfaceSpecAstNode(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InterfaceGroupInterfaceAstNode)) {
      return false;
    }
    InterfaceGroupInterfaceAstNode that = (InterfaceGroupInterfaceAstNode) o;
    return Objects.equals(_interfaceGroup, that._interfaceGroup)
        && Objects.equals(_referenceBook, that._referenceBook);
  }

  String getAddressGroup() {
    return _interfaceGroup;
  }

  String getAddressBook() {
    return _referenceBook;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_interfaceGroup, _referenceBook);
  }
}
