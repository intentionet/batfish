package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;

@ParametersAreNonnullByDefault
public final class MatchLocalPreference extends BooleanExpr {
  private static final String PROP_COMPARATOR = "comparator";
  private static final String PROP_METRIC = "metric";

  @Nonnull private final IntComparator _comparator;
  @Nonnull private final LongExpr _metric;

  @JsonCreator
  private static MatchLocalPreference jsonCreator(
      @Nullable @JsonProperty(PROP_COMPARATOR) IntComparator comparator,
      @Nullable @JsonProperty(PROP_METRIC) LongExpr metric) {
    checkArgument(comparator != null, "%s must be provided", PROP_COMPARATOR);
    checkArgument(metric != null, "%s must be provided", PROP_METRIC);
    return new MatchLocalPreference(comparator, metric);
  }

  public MatchLocalPreference(IntComparator comparator, LongExpr metric) {
    _comparator = comparator;
    _metric = metric;
  }

  @Override
  public <T, U> T accept(BooleanExprVisitor<T, U> visitor, U arg) {
    return visitor.visitMatchLocalPreference(this, arg);
  }

  @Override
  public Result evaluate(Environment environment) {
    throw new BatfishException("No implementation for MatchLocalPreference.evaluate()");
  }

  @JsonProperty(PROP_COMPARATOR)
  @Nonnull
  public IntComparator getComparator() {
    return _comparator;
  }

  @JsonProperty(PROP_METRIC)
  @Nonnull
  public LongExpr getMetric() {
    return _metric;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof MatchLocalPreference)) {
      return false;
    }
    MatchLocalPreference other = (MatchLocalPreference) obj;
    return _comparator == other._comparator && Objects.equals(_metric, other._metric);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_comparator.ordinal(), _metric);
  }
}
