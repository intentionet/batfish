package org.batfish.z3.expr;

import java.util.Objects;
import org.batfish.z3.BasicHeaderField;

public class DeclareVarStatement extends Statement {

  private final BasicHeaderField _headerField;

  public DeclareVarStatement(BasicHeaderField headerField) {
    _headerField = headerField;
  }

  @Override
  public <T> T accept(GenericStatementVisitor<T> visitor) {
    return visitor.visitDeclareVarStatement(this);
  }

  @Override
  public void accept(VoidStatementVisitor visitor) {
    visitor.visitDeclareVarStatement(this);
  }

  public BasicHeaderField getHeaderField() {
    return _headerField;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_headerField);
  }

  @Override
  public boolean statementEquals(Statement e) {
    return Objects.equals(_headerField, ((DeclareVarStatement) e)._headerField);
  }
}
