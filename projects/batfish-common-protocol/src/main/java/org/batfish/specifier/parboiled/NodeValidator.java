package org.batfish.specifier.parboiled;

import static org.batfish.specifier.parboiled.ParboiledInputValidator.getErrorMessageEmptyDeviceRegex;
import static org.batfish.specifier.parboiled.ParboiledInputValidator.getErrorMessageMissingDevice;
import static org.batfish.specifier.parboiled.ParboiledInputValidator.getErrorMessageMissingNodeRoleDimension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.role.NodeRoleDimension;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierContext;

/** An {@link NodeSpecifier} that resolves based on the AST generated by {@link Parser}. */
@ParametersAreNonnullByDefault
final class NodeValidator {

  @ParametersAreNonnullByDefault
  private final class EmptyChecker implements NodeAstNodeVisitor<List<String>> {

    SpecifierContext _ctxt;

    EmptyChecker(SpecifierContext ctxt) {
      _ctxt = ctxt;
    }

    private List<String> concat(List<String> a, List<String> b) {
      return Lists.newArrayList(Iterables.concat(a, b));
    }

    @Override
    public List<String> visitDifferenceNodeAstNode(DifferenceNodeAstNode differenceNodeAstNode) {
      return concat(
          differenceNodeAstNode.getLeft().accept(this),
          differenceNodeAstNode.getRight().accept(this));
    }

    @Override
    public List<String> visitIntersectionNodeAstNode(
        IntersectionNodeAstNode intersectionNodeAstNode) {
      return concat(
          intersectionNodeAstNode.getLeft().accept(this),
          intersectionNodeAstNode.getRight().accept(this));
    }

    @Override
    public List<String> visitNameNodeAstNode(NameNodeAstNode nameNodeAstNode) {
      return (_ctxt.getConfigs().keySet().stream()
              .anyMatch(n -> n.equalsIgnoreCase(nameNodeAstNode.getName())))
          ? ImmutableList.of()
          : ImmutableList.of(getErrorMessageMissingDevice(nameNodeAstNode.getName()));
    }

    @Override
    public List<String> visitNameRegexNodeAstNode(NameRegexNodeAstNode nameRegexNodeAstNode) {
      return (_ctxt.getConfigs().keySet().stream()
              .anyMatch(n -> nameRegexNodeAstNode.getPattern().matcher(n).find()))
          ? ImmutableList.of()
          : ImmutableList.of(getErrorMessageEmptyDeviceRegex(nameRegexNodeAstNode.getRegex()));
    }

    @Override
    public List<String> visitRoleNodeAstNode(RoleNodeAstNode roleNodeAstNode) {
      Optional<NodeRoleDimension> refBook =
          _ctxt.getNodeRoleDimension(roleNodeAstNode.getDimensionName());
      if (refBook.isPresent()) {
        if (refBook.get().getRoles().stream()
            .anyMatch(r -> r.getName().equalsIgnoreCase(roleNodeAstNode.getRoleName()))) {
          return ImmutableList.of();
        } else {
          return ImmutableList.of(
              ParboiledInputValidator.getErrorMessageMissingNodeRole(
                  roleNodeAstNode.getRoleName(), roleNodeAstNode.getDimensionName()));
        }
      } else {
        return ImmutableList.of(
            getErrorMessageMissingNodeRoleDimension(roleNodeAstNode.getDimensionName()));
      }
    }

    @Override
    public List<String> visitTypeNodeAstNode(TypeNodeAstNode typeNodeAstNode) {
      // type information is not available yet
      return ImmutableList.of();
    }

    @Override
    public List<String> visitUnionNodeAstNode(UnionNodeAstNode unionNodeAstNode) {
      return concat(
          unionNodeAstNode.getLeft().accept(this), unionNodeAstNode.getRight().accept(this));
    }
  }

  private final NodeAstNode _ast;

  NodeValidator(NodeAstNode ast) {
    _ast = ast;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeValidator)) {
      return false;
    }
    return Objects.equals(_ast, ((NodeValidator) o)._ast);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_ast);
  }

  public List<String> emptyMessages(SpecifierContext ctxt) {
    return _ast.accept(new EmptyChecker(ctxt));
  }
}
