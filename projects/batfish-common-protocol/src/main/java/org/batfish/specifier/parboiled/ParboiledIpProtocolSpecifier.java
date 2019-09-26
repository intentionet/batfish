package org.batfish.specifier.parboiled;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.specifier.parboiled.Parser.VALID_IP_PROTOCOLS;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IpProtocol;
import org.batfish.specifier.IpProtocolSpecifier;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/** An {@link IpProtocolSpecifier} that resolves based on the AST generated by {@link Parser}. */
@ParametersAreNonnullByDefault
public final class ParboiledIpProtocolSpecifier implements IpProtocolSpecifier {

  private static final class IpProtocolSets {
    @Nonnull private Set<IpProtocol> _including;
    @Nonnull private Set<IpProtocol> _excluding;

    IpProtocolSets() {
      this(ImmutableSet.of(), ImmutableSet.of());
    }

    IpProtocolSets(Set<IpProtocol> including, Set<IpProtocol> excluding) {
      _including = ImmutableSet.copyOf(including);
      _excluding = ImmutableSet.copyOf(excluding);
    }

    IpProtocolSets addIncluding(IpProtocol ipProtocol) {
      return new IpProtocolSets(
          ImmutableSet.<IpProtocol>builder().add(ipProtocol).addAll(_including).build(),
          _excluding);
    }

    IpProtocolSets addExcluding(IpProtocol ipProtocol) {
      return new IpProtocolSets(
          _including,
          ImmutableSet.<IpProtocol>builder().add(ipProtocol).addAll(_excluding).build());
    }

    Set<IpProtocol> toIpProtocols() {
      return Sets.difference(_including.isEmpty() ? VALID_IP_PROTOCOLS : _including, _excluding);
    }

    static IpProtocolSets union(IpProtocolSets sets1, IpProtocolSets sets2) {
      return new IpProtocolSets(
          Sets.union(sets1._including, sets2._including),
          Sets.union(sets1._excluding, sets2._excluding));
    }
  }

  @ParametersAreNonnullByDefault
  private final class IpProtocolAstNodeToIpProtocols
      implements IpProtocolAstNodeVisitor<IpProtocolSets> {

    private IpProtocolSets _ipProtocolSets;

    IpProtocolAstNodeToIpProtocols(IpProtocolSets ipProtocolSets) {
      _ipProtocolSets = ipProtocolSets;
    }

    @Nonnull
    @Override
    public IpProtocolSets visitIpProtocolIpProtocolAstNode(IpProtocolIpProtocolAstNode astNode) {
      return _ipProtocolSets.addIncluding(astNode.getIpProtocol());
    }

    @Override
    public IpProtocolSets visitNotIpProtocolAstNode(NotIpProtocolAstNode astNode) {
      return _ipProtocolSets.addExcluding(astNode.getIpProtocol());
    }

    @Nonnull
    @Override
    public IpProtocolSets visitUnionIpProtocolAstNode(UnionIpProtocolAstNode astNode) {
      return IpProtocolSets.union(astNode.getLeft().accept(this), astNode.getRight().accept(this));
    }
  }

  private final IpProtocolAstNode _ast;

  ParboiledIpProtocolSpecifier(IpProtocolAstNode ast) {
    _ast = ast;
  }

  /**
   * Returns an {@link IpProtocolSpecifier} based on {@code input} which is parsed as {@link
   * Grammar#IP_PROTOCOL_SPECIFIER}.
   *
   * @throws IllegalArgumentException if the parsing fails or does not produce the expected AST
   */
  public static ParboiledIpProtocolSpecifier parse(String input) {
    ParsingResult<AstNode> result =
        new ReportingParseRunner<AstNode>(
                Parser.instance().getInputRule(Grammar.IP_PROTOCOL_SPECIFIER))
            .run(input);

    if (!result.parseErrors.isEmpty()) {
      throw new IllegalArgumentException(
          ParserUtils.getErrorString(
              input,
              Grammar.IP_PROTOCOL_SPECIFIER,
              (InvalidInputError) result.parseErrors.get(0),
              Parser.ANCHORS));
    }

    AstNode ast = ParserUtils.getAst(result);
    checkArgument(
        ast instanceof IpProtocolAstNode,
        "ParboiledIpProtocolSpecifier requires an IP protocol specifier input");

    return new ParboiledIpProtocolSpecifier((IpProtocolAstNode) ast);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParboiledIpProtocolSpecifier)) {
      return false;
    }
    return Objects.equals(_ast, ((ParboiledIpProtocolSpecifier) o)._ast);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ast);
  }

  @Override
  public Set<IpProtocol> resolve() {
    return _ast.accept(new IpProtocolAstNodeToIpProtocols(new IpProtocolSets())).toIpProtocols();
  }
}
