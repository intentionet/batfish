package org.batfish.representation.juniper;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.IncrementMetric;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.Statement;

/** A {@link Statement} that increments the metric on a route. */
public final class PsThenMetricAdd extends PsThen {

  /** */
  private static final long serialVersionUID = 1L;

  private final long _metric;

  public PsThenMetricAdd(long metric) {
    _metric = metric;
  }

  @Override
  public void applyTo(
      List<Statement> statements,
      JuniperConfiguration juniperVendorConfiguration,
      Configuration c,
      Warnings warnings) {
    statements.add(new SetMetric(new IncrementMetric(_metric)));
  }

  public long getMetric() {
    return _metric;
  }
}
