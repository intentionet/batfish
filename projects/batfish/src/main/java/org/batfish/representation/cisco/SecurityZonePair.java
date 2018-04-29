package org.batfish.representation.cisco;

import org.batfish.common.util.DefinedStructure;

public class SecurityZonePair extends DefinedStructure<String> {

  private static final long serialVersionUID = 1L;

  private final String _dstZone;

  private String _inspectPolicyMap;

  private final String _srcZone;

  public SecurityZonePair(String name, int definitionLine, String srcZone, String dstZone) {
    super(name, definitionLine);
    _srcZone = srcZone;
    _dstZone = dstZone;
  }

  public String getDstZone() {
    return _dstZone;
  }

  public String getInspectPolicyMap() {
    return _inspectPolicyMap;
  }

  public String getSrcZone() {
    return _srcZone;
  }

  public void setInspectPolicyMap(String inspectPolicyMap) {
    _inspectPolicyMap = inspectPolicyMap;
  }
}
