package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.IpsecProposal;

public class IpsecTransformSet extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private String _mode;

  private IpsecProposal _proposal;

  public IpsecTransformSet(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _proposal = new IpsecProposal(name, definitionLine);
  }

  public IpsecProposal getProposal() {
    return _proposal;
  }

  public void setMode(String mode) {
    _mode = mode;
  }
}
