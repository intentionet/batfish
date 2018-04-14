package org.batfish.representation.juniper;

import java.io.Serializable;
import java.util.List;
import org.batfish.common.Warnings;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;

public final class FwFromApplication implements Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  private final Application _application;

  private final String _applicationName;

  public FwFromApplication(Application application) {
    _applicationName = null;
    _application = application;
  }

  public FwFromApplication(String applicationName) {
    _applicationName = applicationName;
    _application = null;
  }

  public void applyTo(
      JuniperConfiguration jc,
      HeaderSpace.Builder srcHeaderSpaceBuilder,
      LineAction action,
      List<IpAccessListLine> lines,
      Warnings w) {
    ApplicationSetMember application;
    if (_applicationName != null) {
      application = jc.getApplications().get(_applicationName);
      if (application == null) {
        application = jc.getApplicationSets().get(_applicationName);
      }
    } else {
      application = _application;
    }
    if (application == null) {
      w.redFlag("Reference to undefined application: \"" + _applicationName + "\"");
    } else {
      application.applyTo(jc, srcHeaderSpaceBuilder, action, lines, w);
    }
  }
}
