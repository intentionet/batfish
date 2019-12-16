package org.batfish.representation.juniper;

import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.LineAction;

public final class FwFromApplicationOrApplicationSet extends FwFromApplicationSetMember {

  private final String _applicationOrApplicationSetName;

  public FwFromApplicationOrApplicationSet(String applicationOrApplicationSetName) {
    _applicationOrApplicationSetName = applicationOrApplicationSetName;
  }

  @Override
  public void applyTo(
      JuniperConfiguration jc,
      HeaderSpace.Builder srcHeaderSpaceBuilder,
      LineAction action,
      List<ExprAclLine> lines,
      Warnings w) {
    ApplicationSetMember application =
        jc.getMasterLogicalSystem().getApplications().get(_applicationOrApplicationSetName);
    if (application == null) {
      application =
          jc.getMasterLogicalSystem().getApplicationSets().get(_applicationOrApplicationSetName);
    }
    if (application != null) {
      application.applyTo(jc, srcHeaderSpaceBuilder, action, lines, w);
    }
  }
}
