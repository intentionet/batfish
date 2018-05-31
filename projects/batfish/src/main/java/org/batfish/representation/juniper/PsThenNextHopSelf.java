package org.batfish.representation.juniper;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.Statement;

public final class PsThenNextHopSelf extends PsThen {

  public static final PsThenNextHopSelf INSTANCE = new PsThenNextHopSelf();

  /** */
  private static final long serialVersionUID = 1L;

  private PsThenNextHopSelf() {}

  @Override
  public void applyTo(
      List<Statement> statements,
      JuniperConfiguration juniperVendorConfiguration,
      Configuration c,
      Warnings w) {
    // TODO: what is the meaning of destinationVrf?
    statements.add(new SetNextHop(SelfNextHop.getInstance(), false));
  }
}
