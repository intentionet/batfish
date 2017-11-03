package org.batfish.representation.juniper;

import java.util.SortedSet;
import java.util.TreeSet;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Ip;

public class DhcpRelayServerGroup extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  private final int _definitionLine;

  private final SortedSet<Ip> _servers;

  public DhcpRelayServerGroup(String name, int definitionLine) {
    super(name);
    _servers = new TreeSet<>();
    _definitionLine = definitionLine;
  }

  public int getDefinitionLine() {
    return _definitionLine;
  }

  public SortedSet<Ip> getServers() {
    return _servers;
  }
}
