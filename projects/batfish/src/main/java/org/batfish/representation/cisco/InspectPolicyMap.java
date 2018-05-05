package org.batfish.representation.cisco;

import java.util.Map;
import java.util.TreeMap;
import org.batfish.common.util.DefinedStructure;
import org.batfish.datamodel.LineAction;

public class InspectPolicyMap extends DefinedStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private LineAction _classDefaultAction;

  private Map<String, InspectPolicyMapInspectClass> _inspectClasses;

  public InspectPolicyMap(String name, int definitionLine) {
    super(name, definitionLine);
    _classDefaultAction = LineAction.REJECT;
    _inspectClasses = new TreeMap<>();
  }

  public LineAction getClassDefaultAction() {
    return _classDefaultAction;
  }

  public Map<String, InspectPolicyMapInspectClass> getInspectClasses() {
    return _inspectClasses;
  }

  public void setClassDefaultAction(LineAction classDefaultAction) {
    _classDefaultAction = classDefaultAction;
  }
}
