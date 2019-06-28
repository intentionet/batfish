package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;

public final class BooleanExprs {
  public enum StaticExpressionType {
    CallExprContext,
    CallStatementContext,
    False,
    True,
  }

  /**
   * Boolean expression that evaluates to true iff the given {@link Environment} has {@link
   * Environment#getCallExprContext() callExprContext} set.
   */
  public static final StaticBooleanExpr CALL_EXPR_CONTEXT =
      new StaticBooleanExpr(StaticExpressionType.CallExprContext);

  /**
   * Boolean expression that evaluates to true iff the given {@link Environment} has {@link
   * Environment#getCallStatementContext()} callStatementContext} set.
   */
  public static final StaticBooleanExpr CALL_STATEMENT_CONTEXT =
      new StaticBooleanExpr(StaticExpressionType.CallStatementContext);

  /** Boolean expression that always evaluates to false. */
  public static final StaticBooleanExpr FALSE = new StaticBooleanExpr(StaticExpressionType.False);

  /** Boolean expression that always evaluates to true. */
  public static final StaticBooleanExpr TRUE = new StaticBooleanExpr(StaticExpressionType.True);

  public static final class StaticBooleanExpr extends BooleanExpr {

    private static final String PROP_TYPE = "type";

    private final StaticExpressionType _type;

    private StaticBooleanExpr(StaticExpressionType type) {
      _type = type;
    }

    @JsonCreator
    private static StaticBooleanExpr create(@JsonProperty(PROP_TYPE) StaticExpressionType type) {
      checkArgument(type != null, "%s must be provided", PROP_TYPE);
      switch (type) {
        case CallExprContext:
          return CALL_EXPR_CONTEXT;

        case CallStatementContext:
          return CALL_STATEMENT_CONTEXT;

        case False:
          return FALSE;

        case True:
          return TRUE;

        default:
          throw new BatfishException(
              "Unhandled " + StaticBooleanExpr.class.getCanonicalName() + ": " + type);
      }
    }

    @Override
    public Result evaluate(Environment environment) {
      switch (_type) {
        case CallExprContext:
          return new Result(environment.getCallExprContext());
        case CallStatementContext:
          return new Result(environment.getCallStatementContext());
        case False:
          return new Result(false);
        case True:
          return new Result(true);
        default:
          throw new BatfishException(
              "Unhandled " + StaticBooleanExpr.class.getCanonicalName() + ": " + _type);
      }
    }

    @JsonProperty(PROP_TYPE)
    public StaticExpressionType getType() {
      return _type;
    }

    @Override
    public boolean equals(Object rhs) {
      return rhs instanceof StaticBooleanExpr && _type == ((StaticBooleanExpr) rhs)._type;
    }

    @Override
    public String toString() {
      return _type.toString();
    }

    @Override
    public int hashCode() {
      return _type.ordinal();
    }
  }
}
