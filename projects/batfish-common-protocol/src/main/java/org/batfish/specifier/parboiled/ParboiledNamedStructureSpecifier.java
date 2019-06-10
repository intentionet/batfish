package org.batfish.specifier.parboiled;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.questions.NamedStructurePropertySpecifier;
import org.batfish.specifier.NamedStructureSpecifier;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/**
 * An {@link NamedStructureSpecifier} that resolves based on the AST generated by {@link Parser}.
 */
@ParametersAreNonnullByDefault
public final class ParboiledNamedStructureSpecifier implements NamedStructureSpecifier {

  @ParametersAreNonnullByDefault
  private final class NamedStructureAstNodeToNamedStructures
      implements NamedStructureAstNodeVisitor<Set<String>> {

    NamedStructureAstNodeToNamedStructures() {}

    @Nonnull
    @Override
    public Set<String> visitTypeNamedStructureAstNode(
        TypeNamedStructureAstNode typeNamedStructureAstNode) {
      return ImmutableSet.of(typeNamedStructureAstNode.getType());
    }

    @Nonnull
    @Override
    public Set<String> visitTypeRegexNamedStructureAstNode(
        TypeRegexNamedStructureAstNode typeRegexNamedStructureAstNode) {
      return NamedStructurePropertySpecifier.JAVA_MAP.keySet().stream()
          .filter(prop -> typeRegexNamedStructureAstNode.getPattern().matcher(prop).find())
          .collect(ImmutableSet.toImmutableSet());
    }

    @Nonnull
    @Override
    public Set<String> visitUnionNamedStructureAstNode(
        UnionNamedStructureAstNode unionNamedStructureAstNode) {
      return Sets.union(
          unionNamedStructureAstNode.getLeft().accept(this),
          unionNamedStructureAstNode.getRight().accept(this));
    }
  }

  private final NamedStructureAstNode _ast;

  ParboiledNamedStructureSpecifier(NamedStructureAstNode ast) {
    _ast = ast;
  }

  public ParboiledNamedStructureSpecifier(String input) {
    ParsingResult<AstNode> result =
        new ReportingParseRunner<AstNode>(
                Parser.instance().getInputRule(Grammar.NAMED_STRUCTURE_SPECIFIER))
            .run(input);

    if (!result.parseErrors.isEmpty()) {
      throw new IllegalArgumentException(
          ParserUtils.getErrorString(
              input,
              Grammar.NAMED_STRUCTURE_SPECIFIER,
              (InvalidInputError) result.parseErrors.get(0),
              Parser.ANCHORS));
    }

    AstNode ast = ParserUtils.getAst(result);
    checkArgument(
        ast instanceof NamedStructureAstNode,
        "ParboiledNamedStructureSpecifierFactory requires an NamedStructureSpecifier input");

    _ast = (NamedStructureAstNode) ast;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParboiledNamedStructureSpecifier)) {
      return false;
    }
    return Objects.equals(_ast, ((ParboiledNamedStructureSpecifier) o)._ast);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ast);
  }

  @Override
  public Set<String> resolve() {
    return _ast.accept(new NamedStructureAstNodeToNamedStructures());
  }
}
