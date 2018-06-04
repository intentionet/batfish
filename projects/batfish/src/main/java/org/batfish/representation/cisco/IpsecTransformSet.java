package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.IpsecProposal;

public class IpsecTransformSet extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private String _mode;

  private IpsecProposal _proposal;

  public IpsecTransformSet(String name) {
    super(name);
    _proposal = new IpsecProposal(name, -1);
  }
ß
  public String getMode() {
    return _mode;
  }

  public IpsecProposal getProposal() {
    return _proposal;
  }

  public void setMode(String mode) {
    _mode = mode;
  }
}
