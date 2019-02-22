package org.batfish.z3.expr;

import static org.batfish.z3.expr.ExtractExpr.newExtractExpr;
import static org.batfish.z3.expr.HeaderSpaceMatchExpr.matchPrefix;
import static org.batfish.z3.expr.visitors.Simplifier.simplifyBooleanExpr;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Noop;
import org.batfish.datamodel.transformation.ShiftIpAddressIntoSubnet;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.datamodel.transformation.TransformationStepVisitor;
import org.batfish.z3.AclLineMatchExprToBooleanExpr;
import org.batfish.z3.Field;

/**
 * Converts {@link Transformation Transformations} to transitions in the NOD reachability engine.
 */
public final class TransformationTransitionGenerator {
  private int _ctr;

  private final String _node1;
  private final String _iface1;

  @Nullable private final String _node2;
  @Nullable private final String _iface2;

  private final String _tag; // distinguish this transformation from others on the same interface
  private final AclLineMatchExprToBooleanExpr _aclLineMatchExprToBooleanExpr;
  private final StateExpr _outState;

  // The generated rule statements
  private ImmutableList.Builder<BasicRuleStatement> _statements;

  private TransformationTransitionGenerator(
      String node1,
      String iface1,
      @Nullable String node2,
      @Nullable String iface2,
      String tag,
      AclLineMatchExprToBooleanExpr aclLineMatchExprToBooleanExpr,
      StateExpr outState) {
    _ctr = 0;
    _statements = ImmutableList.builder();
    _node1 = node1;
    _iface1 = iface1;
    _node2 = node2;
    _iface2 = iface2;
    _tag = tag;
    _aclLineMatchExprToBooleanExpr = aclLineMatchExprToBooleanExpr;
    _outState = outState;
  }

  private class StepRuleVisitor implements TransformationStepVisitor<BasicRuleStatement> {
    private final StateExpr _preState;
    private final StateExpr _postState;

    StepRuleVisitor(StateExpr preState, StateExpr postState) {
      _preState = preState;
      _postState = postState;
    }

    private Field getField(IpField ipField) {
      switch (ipField) {
        case DESTINATION:
          return Field.DST_IP;
        case SOURCE:
          return Field.SRC_IP;
        default:
          throw new IllegalArgumentException("Unknown IpField: " + ipField);
      }
    }

    @Override
    public BasicRuleStatement visitAssignIpAddressFromPool(
        AssignIpAddressFromPool assignIpAddressFromPool) {
      Field field = getField(assignIpAddressFromPool.getIpField());

      ImmutableList<BooleanExpr> disjuncts =
          assignIpAddressFromPool.getIpRanges().asRanges().stream()
              .map(range -> assignFromPoolExpr(field, range.lowerEndpoint(), range.upperEndpoint()))
              .collect(ImmutableList.toImmutableList());
      BooleanExpr expr = disjuncts.size() == 1 ? disjuncts.get(0) : new OrExpr(disjuncts);

      return new BasicRuleStatement(expr, _preState, _postState);
    }

    @Override
    public BasicRuleStatement visitNoop(Noop noop) {
      return new BasicRuleStatement(_preState, _postState);
    }

    @Override
    public BasicRuleStatement visitShiftIpAddressIntoSubnet(
        ShiftIpAddressIntoSubnet shiftIpAddressIntoSubnet) {
      return new BasicRuleStatement(
          shiftIntoSubnetExpr(
              getField(shiftIpAddressIntoSubnet.getIpField()),
              shiftIpAddressIntoSubnet.getSubnet()),
          _preState,
          _postState);
    }

    @Override
    public BasicRuleStatement visitAssignPortFromPool(AssignPortFromPool assignPortFromPool) {
      // TODO
      throw new BatfishException("PAT is not supported");
    }
  }

  /** Generate a new StateExpr for this transformation, and transitions from it to the end state. */
  private StateExpr generateTransitions(Transformation transformation) {
    int id = _ctr++;
    StateExpr state = new TransformationExpr(_node1, _iface1, _node2, _iface2, _tag, id);

    BooleanExpr guardExpr =
        simplifyBooleanExpr(
            _aclLineMatchExprToBooleanExpr.toBooleanExpr(transformation.getGuard()));
    BooleanExpr notGuardExpr =
        guardExpr == TrueExpr.INSTANCE ? FalseExpr.INSTANCE : new NotExpr(guardExpr);

    StateExpr andThen =
        transformation.getAndThen() == null
            ? _outState
            : generateTransitions(transformation.getAndThen());
    StateExpr orElse =
        transformation.getOrElse() == null
            ? _outState
            : generateTransitions(transformation.getOrElse());

    StateExpr stepsExpr =
        generateStepTransitions(id, transformation.getTransformationSteps(), andThen);

    _statements.add(new BasicRuleStatement(guardExpr, state, stepsExpr));
    _statements.add(new BasicRuleStatement(notGuardExpr, state, orElse));
    return state;
  }

  private StateExpr generateStepTransitions(
      int transformationId, List<TransformationStep> transformationSteps, StateExpr andThen) {
    StateExpr next = andThen;
    int stepId = transformationSteps.size() - 1;
    for (TransformationStep step : Lists.reverse(transformationSteps)) {
      StateExpr stepExpr =
          new TransformationStepExpr(
              _node1, _iface1, _node2, _iface2, _tag, transformationId, stepId);
      _statements.add(step.accept(new StepRuleVisitor(stepExpr, next)));
      next = stepExpr;
      stepId--;
    }
    return next;
  }

  /**
   * Generates {@link BasicRuleStatement}s corresponding to either incoming or outgoing {@link
   * Transformation}s. Outgoing transformations are parameterized on edges and incoming
   * transformations are parameterized on an interface.
   *
   * @param node1 For outgoing transformations, the first node in the edge which has the
   *     transformation. For incoming transformations, the node of the interface which has the
   *     transformation.
   * @param iface1 For outgoing transformations, the interface of the first node in the edge which
   *     has the transformation. For incoming transformations, the interface which has the
   *     transformation.
   * @param node2 For outgoing transformations, the second node in the edge which has the
   *     transformation. For incoming transformations, null.
   * @param iface2 For outgoing transformations, the interface of the second node in the edge which
   *     has the transformation. For incoming transformations, null.
   * @param tag Distinguishes this transformation from others on the same interface
   * @param aclLineMatchExprToBooleanExpr Converts {@link AclLineMatchExpr} to {@link BooleanExpr}
   *     on {@code node1}
   * @param preStates The set of precondition states for this transition.
   * @param postState The postcondition state for this transition.
   * @param transformation The transformation for this transition.
   * @return A {@link List} of {@link BasicRuleStatement}s which correspond to the {@link
   *     Transformation}.
   */
  public static List<BasicRuleStatement> generateTransitions(
      @Nonnull String node1,
      @Nonnull String iface1,
      @Nullable String node2,
      @Nullable String iface2,
      String tag,
      AclLineMatchExprToBooleanExpr aclLineMatchExprToBooleanExpr,
      Set<StateExpr> preStates,
      StateExpr postState,
      @Nullable Transformation transformation) {
    if (transformation == null) {
      return ImmutableList.of(new BasicRuleStatement(preStates, postState));
    }

    TransformationTransitionGenerator gen =
        new TransformationTransitionGenerator(
            node1, iface1, node2, iface2, tag, aclLineMatchExprToBooleanExpr, postState);
    StateExpr stateExpr = gen.generateTransitions(transformation);
    gen._statements.add(new BasicRuleStatement(preStates, stateExpr));
    return gen._statements.build();
  }

  @VisibleForTesting
  static BooleanExpr shiftIntoSubnetExpr(Field field, Prefix subnet) {
    TransformedVarIntExpr transformedField = new TransformedVarIntExpr(field);

    // The shifting constraint has two parts: the transformed field is in the subnet, and
    // all the bits that don't correspond to the subnet are preserved.
    BooleanExpr shiftExpr = matchPrefix(subnet, transformedField);

    if (subnet.getPrefixLength() == Prefix.MAX_PREFIX_LENGTH) {
      return shiftExpr;
    }

    int high = Prefix.MAX_PREFIX_LENGTH - subnet.getPrefixLength() - 1;
    BooleanExpr preservedExpr =
        new EqExpr(newExtractExpr(transformedField, 0, high), newExtractExpr(field, 0, high));

    return new AndExpr(ImmutableList.of(shiftExpr, preservedExpr));
  }

  @VisibleForTesting
  static BooleanExpr assignFromPoolExpr(Field field, Ip poolStart, Ip poolEnd) {
    return new RangeMatchExpr(
        new TransformedVarIntExpr(field),
        field.getSize(),
        ImmutableSet.of(Range.closed(poolStart.asLong(), poolEnd.asLong())));
  }
}
