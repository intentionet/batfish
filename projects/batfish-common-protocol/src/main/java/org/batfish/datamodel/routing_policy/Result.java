package org.batfish.datamodel.routing_policy;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nullable;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.statement.If;

/** Result of evaluating a {@link RoutingPolicy} or a {@link BooleanExpr}. */
public final class Result {
  private final boolean _booleanValue;
  private final boolean _exit;
  private final boolean _fallThrough;
  private final boolean _return;

  /** Creates a Result with all fields false. */
  public Result() {
    this(false, false, false, false);
  }

  /**
   * Creates a Result with the given {@link #getBooleanValue() booleanValue} and false {@link
   * #getExit() exit}, {@link #getFallThrough() fallThrough}, and {@link #getReturn() return}.
   */
  public Result(boolean booleanValue) {
    this(booleanValue, false, false, false);
  }

  /**
   * Creates a Result with the given {@link #getBooleanValue() booleanValue}, {@link #getExit()
   * exit}, {@link #getFallThrough() fallThrough}, and {@link #getReturn() return} values.
   */
  public Result(boolean booleanValue, boolean exit, boolean fallThrough, boolean aReturn) {
    _booleanValue = booleanValue;
    _exit = exit;
    _fallThrough = fallThrough;
    _return = aReturn;
  }

  /** The boolean value of this result. */
  public boolean getBooleanValue() {
    return _booleanValue;
  }

  /**
   * Whether to stop evaluation of all parent policies/statement (callers). Indicates reaching a
   * terminal accept/reject action.
   */
  public boolean getExit() {
    return _exit;
  }

  /**
   * Whether an evaluation of policy/statement resulted in a fall-through (e.g., an {@link If} where
   * none of the statements inside the IF caused a {@link #_exit} or {@link #_return}, or a policy
   * that didn't match).
   */
  public boolean getFallThrough() {
    return _fallThrough;
  }

  /**
   * Whether to stop evaluation of current policy/statement, but continue evaluation of parent
   * policies (callers).
   */
  public boolean getReturn() {
    return _return;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Result)) {
      return false;
    }
    Result result = (Result) o;
    return _booleanValue == result._booleanValue
        && _exit == result._exit
        && _fallThrough == result._fallThrough
        && _return == result._return;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_booleanValue, _exit, _fallThrough, _return);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(Result.class)
        .add("booleanValue", _booleanValue)
        .add("exit", _exit)
        .add("fallThrough", _fallThrough)
        .add("return", _return)
        .toString();
  }

  public Builder toBuilder() {
    return builder()
        .setBooleanValue(_booleanValue)
        .setExit(_exit)
        .setFallThrough(_fallThrough)
        .setReturn(_return);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private boolean _booleanValue;
    private boolean _exit;
    private boolean _fallThrough;
    private boolean _return;

    private Builder() {}

    public Result build() {
      return new Result(_booleanValue, _exit, _fallThrough, _return);
    }

    public Builder setBooleanValue(boolean booleanValue) {
      _booleanValue = booleanValue;
      return this;
    }

    public Builder setExit(boolean exit) {
      _exit = exit;
      return this;
    }

    public Builder setFallThrough(boolean fallThrough) {
      _fallThrough = fallThrough;
      return this;
    }

    public Builder setReturn(boolean aReturn) {
      _return = aReturn;
      return this;
    }
  }
}
