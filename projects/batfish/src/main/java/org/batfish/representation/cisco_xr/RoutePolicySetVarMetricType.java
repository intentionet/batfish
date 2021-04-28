package org.batfish.representation.cisco_xr;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.statement.SetVarMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;

public class RoutePolicySetVarMetricType extends RoutePolicyStatement {

  private String _var;

  public RoutePolicySetVarMetricType(String var) {
    _var = var;
  }

  @Override
  public void applyTo(
      List<Statement> statements, CiscoXrConfiguration cc, Configuration c, Warnings w) {
    statements.add(new SetVarMetricType(_var));
    // Modified routes are not subject to default-drop disposition
    statements.add(Statements.SetDefaultActionAccept.toStaticStatement());
  }
}
