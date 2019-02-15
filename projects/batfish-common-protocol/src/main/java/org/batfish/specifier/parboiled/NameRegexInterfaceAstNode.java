package org.batfish.specifier.parboiled;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.regex.Pattern;

final class NameRegexInterfaceAstNode implements InterfaceAstNode {
  private final String _regex;
  private final Pattern _pattern;

  NameRegexInterfaceAstNode(String regex) {
    _regex = regex;
    _pattern = Pattern.compile(regex); // this will barf on invalid regex
  }

  @Override
  public <T> T accept(AstNodeVisitor<T> visitor) {
    return visitor.visitNameRegexInterfaceSpecAstNode(this);
  }

  @Override
  public <T> T accept(InterfaceAstNodeVisitor<T> visitor) {
    return visitor.visitNameRegexInterfaceSpecAstNode(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NameRegexInterfaceAstNode that = (NameRegexInterfaceAstNode) o;
    return Objects.equals(_regex, that._regex);
  }

  public String getName() {
    return _regex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_regex);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this.getClass()).add("regex", _regex).toString();
  }
}
