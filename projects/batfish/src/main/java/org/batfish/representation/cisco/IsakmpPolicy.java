package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.IkeProposal;

public class IsakmpPolicy extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private IkeProposal _proposal;

  public IsakmpPolicy(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _proposal = new IkeProposal(name, definitionLine);
  }

  public int getDefinitionLine() {
    return _definitionLine;
  }

  public IkeProposal getProposal() {
    return _proposal;
  }

  public void setProposal(IkeProposal proposal) {
    _proposal = proposal;
  }
}
