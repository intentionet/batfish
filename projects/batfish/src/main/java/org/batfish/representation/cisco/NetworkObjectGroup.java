package org.batfish.representation.cisco;

import java.util.LinkedList;
import java.util.List;
import org.batfish.datamodel.IpSpace;

public class NetworkObjectGroup extends ObjectGroup {

  /** */
  private static final long serialVersionUID = 1L;

  private List<IpSpace> _lines;

  public NetworkObjectGroup(String name, int definitionLine) {
    super(name, definitionLine);
    _lines = new LinkedList<>();
  }

  public List<IpSpace> getLines() {
    return _lines;
  }
}
