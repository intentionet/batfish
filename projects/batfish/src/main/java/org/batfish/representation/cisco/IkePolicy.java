package org.batfish.representation.cisco;

import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.IkeProposal;

public class IkePolicy extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private String _preSharedKeyHash;

  private IkeProposal _proposal;

  public IkePolicy(String name, int definitionLine) {
    super(name);
    _definitionLine = definitionLine;
    _proposal = new IkeProposal(name, definitionLine);
  }

  public int getDefinitionLine() {
    return _definitionLine;
  }

  public String getPreSharedKeyHash() {
    return _preSharedKeyHash;
  }

  public IkeProposal getProposal() {
    return _proposal;
  }

  public void setPreSharedKeyHash(String preSharedKeyHash) {
    _preSharedKeyHash = preSharedKeyHash;
  }

  public void setProposal(IkeProposal proposal) {
    _proposal = proposal;
  }
}
