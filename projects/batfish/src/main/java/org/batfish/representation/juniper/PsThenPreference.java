package org.batfish.representation.juniper;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.statement.SetAdministrativeCost;
import org.batfish.datamodel.routing_policy.statement.Statement;

public final class PsThenPreference extends PsThen {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _preference;

  public PsThenPreference(int preference) {
    _preference = preference;
  }

  @Override
  public void applyTo(
      List<Statement> statements,
      JuniperConfiguration juniperVendorConfiguration,
      Configuration c,
      Warnings warnings) {
    statements.add(new SetAdministrativeCost(new LiteralInt(_preference)));
  }

  public int getPreference() {
    return _preference;
  }
}
