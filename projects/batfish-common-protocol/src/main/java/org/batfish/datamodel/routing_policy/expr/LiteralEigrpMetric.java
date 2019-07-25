package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.routing_policy.Environment;

@ParametersAreNonnullByDefault
public final class LiteralEigrpMetric extends EigrpMetricExpr {
  private static final String PROP_METRIC = "metric";

  @Nonnull private final EigrpMetric _metric;

  @JsonCreator
  private static LiteralEigrpMetric jsonCreator(
      @Nullable @JsonProperty(PROP_METRIC) EigrpMetric metric) {
    checkArgument(metric != null, "%s must be provided", PROP_METRIC);
    return new LiteralEigrpMetric(metric);
  }

  public LiteralEigrpMetric(EigrpMetric metric) {
    _metric = metric;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof LiteralEigrpMetric)) {
      return false;
    }
    LiteralEigrpMetric rhs = (LiteralEigrpMetric) obj;
    return Objects.equals(_metric, rhs._metric);
  }

  @Override
  public int hashCode() {
    return _metric.hashCode();
  }

  @Override
  public EigrpMetric evaluate(Environment env) {
    return _metric;
  }

  @JsonProperty(PROP_METRIC)
  @Nonnull
  public EigrpMetric getMetric() {
    return _metric;
  }
}
