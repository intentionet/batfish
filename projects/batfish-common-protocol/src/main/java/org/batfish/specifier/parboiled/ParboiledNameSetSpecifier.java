package org.batfish.specifier.parboiled;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.specifier.EnumSetSpecifier;
import org.batfish.specifier.NameSetSpecifier;
import org.batfish.specifier.SpecifierContext;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/** An {@link EnumSetSpecifier} that resolves based on the AST generated by {@link Parser}. */
@ParametersAreNonnullByDefault
public final class ParboiledNameSetSpecifier implements NameSetSpecifier {

  @ParametersAreNonnullByDefault
  private final class NameAstNodeToNames implements NameSetAstNodeVisitor<Set<String>> {
    private final Set<String> _allNames;

    NameAstNodeToNames(Set<String> allNames) {
      _allNames = allNames;
    }

    @Nonnull
    @Override
    public Set<String> visitNameNameSetAstNode(SingletonNameSetAstNode singletonNameSetAstNode) {
      return _allNames.stream()
          .filter(n -> n.equalsIgnoreCase(singletonNameSetAstNode.getName()))
          .collect(ImmutableSet.toImmutableSet());
    }

    @Nonnull
    @Override
    public Set<String> visitRegexNameSetAstNode(RegexNameSetAstNode regexNameSetAstNode) {
      return _allNames.stream()
          .filter(n -> regexNameSetAstNode.getPattern().matcher(n).find())
          .collect(ImmutableSet.toImmutableSet());
    }

    @Nonnull
    @Override
    public Set<String> visitUnionNameSetAstNode(UnionNameSetAstNode unionNameSetAstNode) {
      return Sets.union(
          unionNameSetAstNode.getLeft().accept(this), unionNameSetAstNode.getRight().accept(this));
    }
  }

  @Nonnull private final NameSetAstNode _ast;

  @Nonnull private final Grammar _grammar;

  ParboiledNameSetSpecifier(NameSetAstNode ast, Grammar grammar) {
    _ast = ast;
    _grammar = grammar;
  }

  /**
   * Returns an {@link NameSetSpecifier} based on parsing the {@code input} according to the
   * specified grammar
   *
   * @throws IllegalArgumentException if the parsing fails or does not produce the expected AST
   */
  public static ParboiledNameSetSpecifier parse(String input, Grammar grammar) {
    ParsingResult<AstNode> result =
        new ReportingParseRunner<AstNode>(Parser.instance().getInputRule(grammar)).run(input);

    if (!result.parseErrors.isEmpty()) {
      throw new IllegalArgumentException(
          ParserUtils.getErrorString(
              input, grammar, (InvalidInputError) result.parseErrors.get(0), Parser.ANCHORS));
    }

    AstNode ast = ParserUtils.getAst(result);
    checkArgument(
        ast instanceof NameSetAstNode,
        "Unexpected AST when parsing '%s' as enum grammar: %s",
        input,
        ast);

    return new ParboiledNameSetSpecifier((NameSetAstNode) ast, grammar);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParboiledNameSetSpecifier)) {
      return false;
    }
    return Objects.equals(_ast, ((ParboiledNameSetSpecifier) o)._ast);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ast);
  }

  @Override
  public Set<String> resolve(SpecifierContext ctxt) {
    return _ast.accept(new NameAstNodeToNames(Grammar.getNames(ctxt, _grammar)));
  }
}
