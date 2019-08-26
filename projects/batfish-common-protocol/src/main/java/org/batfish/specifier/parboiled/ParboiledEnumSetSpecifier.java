package org.batfish.specifier.parboiled;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.specifier.EnumSetSpecifier;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/** An {@link EnumSetSpecifier} that resolves based on the AST generated by {@link Parser}. */
@ParametersAreNonnullByDefault
public final class ParboiledEnumSetSpecifier<T> implements EnumSetSpecifier<T> {

  @ParametersAreNonnullByDefault
  private static final class EnumValueSets<T> {
    @Nonnull private final Set<T> _including;
    @Nonnull private final Set<T> _excluding;
    @Nonnull private final Collection<T> _allValues;

    EnumValueSets(Collection<T> allValues) {
      this(ImmutableSet.of(), ImmutableSet.of(), allValues);
    }

    EnumValueSets(Set<T> including, Set<T> excluding, Collection<T> allValues) {
      _including = ImmutableSet.copyOf(including);
      _excluding = ImmutableSet.copyOf(excluding);
      _allValues = allValues;
    }

    EnumValueSets<T> addIncluding(Set<T> values) {
      return new EnumValueSets<>(
          ImmutableSet.<T>builder().addAll(values).addAll(_including).build(),
          _excluding,
          _allValues);
    }

    EnumValueSets<T> addExcluding(Set<T> values) {
      return new EnumValueSets<>(
          _including,
          ImmutableSet.<T>builder().addAll(values).addAll(_excluding).build(),
          _allValues);
    }

    Set<T> toValues() {
      return Sets.difference(
          _including.isEmpty() ? ImmutableSet.copyOf(_allValues) : _including, _excluding);
    }

    EnumValueSets<T> union(EnumValueSets<T> sets2) {
      return new EnumValueSets<>(
          Sets.union(_including, sets2._including),
          Sets.union(_excluding, sets2._excluding),
          _allValues);
    }
  }

  @ParametersAreNonnullByDefault
  static final class EnumSetAstNodeToEnumValues<T>
      implements EnumSetAstNodeVisitor<EnumValueSets<T>> {

    @Nonnull private final Collection<T> _allValues;
    @Nonnull private final EnumValueSets<T> _enumValueSets;

    EnumSetAstNodeToEnumValues(Collection<T> allValues) {
      _allValues = allValues;
      _enumValueSets = new EnumValueSets<>(allValues);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T1> EnumValueSets<T> visitValueEnumSetAstNode(
        ValueEnumSetAstNode<T1> valueEnumSetAstNode) {
      T value = (T) valueEnumSetAstNode.getValue();
      return _enumValueSets.addIncluding(ImmutableSet.of(value));
    }

    @Nonnull
    @Override
    public EnumValueSets<T> visitRegexEnumSetAstNode(RegexEnumSetAstNode regexEnumSetAstNode) {
      return _enumValueSets.addIncluding(
          _allValues.stream()
              .filter(prop -> regexEnumSetAstNode.getPattern().matcher(prop.toString()).find())
              .collect(ImmutableSet.toImmutableSet()));
    }

    @Override
    public EnumValueSets<T> visitNotEnumSetAstNode(NotEnumSetAstNode notEnumSetAstNode) {
      return _enumValueSets.addExcluding(notEnumSetAstNode.getAstNode().accept(this).toValues());
    }

    @Nonnull
    @Override
    public EnumValueSets<T> visitUnionEnumSetAstNode(UnionEnumSetAstNode unionEnumSetAstNode) {
      return unionEnumSetAstNode
          .getLeft()
          .accept(this)
          .union(unionEnumSetAstNode.getRight().accept(this));
    }
  }

  private final EnumSetAstNode _ast;

  private final Collection<T> _allValues;

  ParboiledEnumSetSpecifier(EnumSetAstNode ast, Collection<T> allValues) {
    _ast = ast;
    _allValues = allValues;
  }

  /**
   * Returns an {@link EnumSetSpecifier} based on parsing the {@code input} according to the
   * specified grammar
   *
   * @throws IllegalArgumentException if the parsing fails or does not produce the expected AST
   */
  @SuppressWarnings("unchecked")
  public static <T> ParboiledEnumSetSpecifier<T> parse(String input, Grammar grammar) {
    ParsingResult<AstNode> result =
        new ReportingParseRunner<AstNode>(Parser.instance().getInputRule(grammar)).run(input);

    if (!result.parseErrors.isEmpty()) {
      throw new IllegalArgumentException(
          ParserUtils.getErrorString(
              input, grammar, (InvalidInputError) result.parseErrors.get(0), Parser.ANCHORS));
    }

    AstNode ast = ParserUtils.getAst(result);
    checkArgument(
        ast instanceof EnumSetAstNode,
        "Unexpected AST when parsing '%s' as enum grammar: %s",
        input,
        ast);

    return new ParboiledEnumSetSpecifier<>(
        (EnumSetAstNode) ast, (Collection<T>) Grammar.getEnumValues(grammar));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParboiledEnumSetSpecifier)) {
      return false;
    }
    return Objects.equals(_ast, ((ParboiledEnumSetSpecifier) o)._ast);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ast);
  }

  @Override
  public Set<T> resolve() {
    return _ast.accept(new EnumSetAstNodeToEnumValues<>(_allValues)).toValues();
  }
}
