package org.batfish.specifier.parboiled;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.specifier.parboiled.InternetLocationAstNode.INTERNET_LOCATION;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.specifier.AllInterfacesLocationSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.InterfaceSpecifierInterfaceLocationSpecifier;
import org.batfish.specifier.IntersectionLocationSpecifier;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.NodeSpecifierInterfaceLocationSpecifier;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.ToInterfaceLinkLocationSpecifier;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/** An {@link LocationSpecifier} that resolves based on the AST generated by {@link Parser}. */
@ParametersAreNonnullByDefault
public final class ParboiledLocationSpecifier implements LocationSpecifier {

  @ParametersAreNonnullByDefault
  private final class LocationAstNodeToLocations implements LocationAstNodeVisitor<Set<Location>> {
    private final SpecifierContext _ctxt;

    LocationAstNodeToLocations(SpecifierContext ctxt) {
      _ctxt = ctxt;
    }

    @Nonnull
    @Override
    public Set<Location> visitDifferenceLocationAstNode(
        DifferenceLocationAstNode differenceLocationAstNode) {
      return Sets.difference(
          differenceLocationAstNode.getLeft().accept(this),
          differenceLocationAstNode.getRight().accept(this));
    }

    @Nonnull
    @Override
    public Set<Location> visitEnterLocationAstNode(EnterLocationAstNode enterLocationAstNode) {
      return new ToInterfaceLinkLocationSpecifier(
              new ParboiledLocationSpecifier(enterLocationAstNode.getInterfaceLocationAstNode()))
          .resolve(_ctxt);
    }

    @Nonnull
    @Override
    public Set<Location> visitInterfaceLocationAstNode(
        InterfaceLocationAstNode interfaceLocationAstNode) {
      NodeSpecifier nodes =
          interfaceLocationAstNode.getNodeAstNode() == null
              ? AllNodesNodeSpecifier.INSTANCE
              : new ParboiledNodeSpecifier(interfaceLocationAstNode.getNodeAstNode());
      LocationSpecifier interfaceLocations =
          interfaceLocationAstNode.getInterfaceAstNode() == null
              ? AllInterfacesLocationSpecifier.INSTANCE
              : new InterfaceSpecifierInterfaceLocationSpecifier(
                  new ParboiledInterfaceSpecifier(interfaceLocationAstNode.getInterfaceAstNode()));

      return new IntersectionLocationSpecifier(
              new NodeSpecifierInterfaceLocationSpecifier(nodes), interfaceLocations)
          .resolve(_ctxt);
    }

    @Override
    public Set<Location> visitInternetLocationAstNode() {
      return ImmutableSet.of(INTERNET_LOCATION);
    }

    @Nonnull
    @Override
    public Set<Location> visitIntersectionLocationAstNode(
        IntersectionLocationAstNode intersectionLocationAstNode) {
      return Sets.intersection(
          intersectionLocationAstNode.getLeft().accept(this),
          intersectionLocationAstNode.getRight().accept(this));
    }

    @Nonnull
    @Override
    public Set<Location> visitUnionLocationAstNode(UnionLocationAstNode unionLocationAstNode) {
      return Sets.union(
          unionLocationAstNode.getLeft().accept(this),
          unionLocationAstNode.getRight().accept(this));
    }
  }

  private final LocationAstNode _ast;

  ParboiledLocationSpecifier(LocationAstNode ast) {
    _ast = ast;
  }

  /**
   * Returns an {@link LocationSpecifier} based on {@code input} which is parsed as {@link
   * Grammar#LOCATION_SPECIFIER}.
   *
   * @throws IllegalArgumentException if the parsing fails or does not produce the expected AST
   */
  public static ParboiledLocationSpecifier parse(String input) {

    ParsingResult<AstNode> result =
        new ReportingParseRunner<AstNode>(
                Parser.instance().getInputRule(Grammar.LOCATION_SPECIFIER))
            .run(input);

    if (!result.parseErrors.isEmpty()) {
      throw new IllegalArgumentException(
          ParserUtils.getErrorString(
              input,
              Grammar.LOCATION_SPECIFIER,
              (InvalidInputError) result.parseErrors.get(0),
              Parser.ANCHORS));
    }

    AstNode ast = ParserUtils.getAst(result);
    checkArgument(
        ast instanceof LocationAstNode,
        "ParboiledLocationSpecifier requires a LocationSpecifier input");

    return new ParboiledLocationSpecifier((LocationAstNode) ast);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParboiledLocationSpecifier)) {
      return false;
    }
    return Objects.equals(_ast, ((ParboiledLocationSpecifier) o)._ast);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(_ast);
  }

  @Override
  public Set<Location> resolve(SpecifierContext ctxt) {
    return _ast.accept(new LocationAstNodeToLocations(ctxt));
  }
}
