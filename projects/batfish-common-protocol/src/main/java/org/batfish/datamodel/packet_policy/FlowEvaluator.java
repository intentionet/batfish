package org.batfish.datamodel.packet_policy;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.acl.Evaluator;
import org.batfish.datamodel.packet_policy.FlowEvaluator.StatementResult;

/**
 * Evaluates a {@link PacketPolicy} against a given {@link Flow}. As it walks the AST of {@link
 * Statement statements}, a {@link StatementResult result} of evaluating each individual statement
 * is returned.
 *
 * <p>To evaluate an entire policy, see {@link #evaluate(Flow, String, PacketPolicy, Set)} which
 * will return a {@link FlowResult}.
 */
@ParametersAreNonnullByDefault
public final class FlowEvaluator implements PacketPolicyVisitor<StatementResult> {

  private static final StatementResult MATCH_NOOP = new StatementResult(Noop.instance(), true);
  private static final StatementResult NO_MATCH = new StatementResult(Noop.instance(), false);

  // Start state
  @Nonnull private final String _srcInterface;
  @Nonnull private Set<String> _availableVrfs;

  // Modified state
  @Nonnull private Flow.Builder _currentFlow;

  @VisibleForTesting
  FlowEvaluator(Flow originalFlow, String srcInterface, Set<String> availableVrfs) {
    _currentFlow = originalFlow.toBuilder();
    _srcInterface = srcInterface;
    _availableVrfs = availableVrfs;
  }

  @Override
  public StatementResult visitDrop(Drop drop) {
    return new StatementResult(drop, true);
  }

  @Override
  public StatementResult visitNoop(Noop noop) {
    return MATCH_NOOP;
  }

  @Override
  public StatementResult visitIf(If ifExpr) {
    if (!ifExpr.getMatchCondition().accept(this).matched()) {
      return NO_MATCH;
    } else {
      Optional<StatementResult> result =
          ifExpr.getActions().stream().map(this::visit).filter(StatementResult::stop).findFirst();
      return result.orElse(MATCH_NOOP);
    }
  }

  @Override
  public StatementResult visitFibLookup(FibLookup fibLookup) {
    if (!_availableVrfs.contains(fibLookup.getVrfName())) {
      return NO_MATCH;
    }
    return new StatementResult(fibLookup, true);
  }

  @Override
  public StatementResult visitPacketMatchExpr(PacketMatchExpr expr) {
    return new StatementResult(
        Noop.instance(),
        Evaluator.matches(
            expr.getExpr(),
            _currentFlow.build(),
            _srcInterface,
            ImmutableMap.of(),
            ImmutableMap.of()));
  }

  @Nonnull
  private Flow getTransformedFlow() {
    return _currentFlow.build();
  }

  public static FlowResult evaluate(
      Flow f, String srcInterface, PacketPolicy policy, Set<String> availableVrfs) {
    FlowEvaluator evaluator = new FlowEvaluator(f, srcInterface, availableVrfs);
    Action action =
        policy.getStatements().stream()
            .map(evaluator::visit)
            .filter(StatementResult::stop)
            .findFirst()
            .orElse(new StatementResult(Drop.instance(), false))
            .getAction();
    checkState(action.isTerminal(), "Evaluation of packet policy must end with a terminal action");
    return new FlowResult(evaluator.getTransformedFlow(), action);
  }

  static final class StatementResult {

    private final Action _action;
    private final boolean _matched;

    @VisibleForTesting
    StatementResult(Action action, boolean matched) {
      _matched = matched;
      _action = action;
    }

    public Action getAction() {
      return _action;
    }

    public boolean matched() {
      return _matched;
    }

    /** Whether to stop evaluation of further statements */
    public boolean stop() {
      return matched() && _action.isTerminal();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StatementResult that = (StatementResult) o;
      return _matched == that._matched && Objects.equals(getAction(), that.getAction());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getAction(), _matched);
    }
  }

  /** Combination of final (possibly transformed) {@link Flow} and the action taken */
  public static final class FlowResult {
    private final Flow _finalFlow;
    private final Action _action;

    FlowResult(Flow finalFlow, Action action) {
      _finalFlow = finalFlow;
      _action = action;
    }

    public Flow getFinalFlow() {
      return _finalFlow;
    }

    public Action getAction() {
      return _action;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FlowResult that = (FlowResult) o;
      return Objects.equals(getFinalFlow(), that.getFinalFlow())
          && Objects.equals(getAction(), that.getAction());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFinalFlow(), getAction());
    }
  }
}
