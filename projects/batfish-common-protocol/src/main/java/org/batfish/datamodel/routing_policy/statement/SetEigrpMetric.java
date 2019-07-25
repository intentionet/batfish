package org.batfish.datamodel.routing_policy.statement;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.EigrpExternalRoute;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.expr.EigrpMetricExpr;

@ParametersAreNonnullByDefault
public final class SetEigrpMetric extends Statement {
  private static final String PROP_METRIC = "metric";

  @Nonnull private final EigrpMetricExpr _metric;

  @JsonCreator
  private static SetEigrpMetric jsonCreator(
      @Nullable @JsonProperty(PROP_METRIC) EigrpMetricExpr metric) {
    checkArgument(metric != null, "%s must be provided", PROP_METRIC);
    return new SetEigrpMetric(metric);
  }

  public SetEigrpMetric(EigrpMetricExpr metric) {
    _metric = metric;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof SetEigrpMetric)) {
      return false;
    }
    SetEigrpMetric rhs = (SetEigrpMetric) obj;
    return _metric.equals(rhs._metric);
  }

  @Override
  public int hashCode() {
    return _metric.hashCode();
  }

  @Override
  public Result execute(Environment environment) {
    Result result = new Result();
    EigrpMetric metric = _metric.evaluate(environment);
    ((EigrpExternalRoute.Builder) environment.getOutputRoute()).setEigrpMetric(metric);
    return result;
  }

  @JsonProperty(PROP_METRIC)
  @Nonnull
  public EigrpMetricExpr getMetric() {
    return _metric;
  }
}
