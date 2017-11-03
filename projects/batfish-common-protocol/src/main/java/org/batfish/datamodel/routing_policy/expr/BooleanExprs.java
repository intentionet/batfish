package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;

public enum BooleanExprs {
  CallExprContext,
  CallStatementContext,
  False,
  True;

  public static class StaticBooleanExpr extends BooleanExpr {
    /** */
    private static final long serialVersionUID = 1L;

    private static final String PROP_TYPE = "type";

    private BooleanExprs _type;

    @JsonCreator
    public StaticBooleanExpr(@JsonProperty(PROP_TYPE) BooleanExprs type) {
      _type = type;
    }

    @Override
    public boolean equals(Object rhs) {
      if (rhs instanceof StaticBooleanExpr) {
        return _type.equals(((StaticBooleanExpr) rhs)._type);
      }
      return false;
    }

    @Override
    public Result evaluate(Environment environment) {
      Result result = new Result();
      switch (_type) {
        case CallExprContext:
          result.setBooleanValue(environment.getCallExprContext());
          break;
        case CallStatementContext:
          result.setBooleanValue(environment.getCallStatementContext());
          break;
        case False:
          result.setBooleanValue(false);
          break;
        case True:
          result.setBooleanValue(true);
          break;
        default:
          throw new BatfishException(
              "Unhandled " + BooleanExprs.class.getCanonicalName() + ": " + _type);
      }
      return result;
    }

    @JsonProperty(PROP_TYPE)
    public BooleanExprs getType() {
      return _type;
    }

    @Override
    public int hashCode() {
      return _type.ordinal();
    }
  }

  public StaticBooleanExpr toStaticBooleanExpr() {
    return new StaticBooleanExpr(this);
  }
}
