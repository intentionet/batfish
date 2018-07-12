package org.batfish.representation.palo_alto;

import java.util.SortedSet;
import java.util.TreeSet;
import org.batfish.datamodel.IpProtocol;

public final class Service implements ServiceGroupMember {
  private static final long serialVersionUID = 1L;

  private String _description;

  private final String _name;

  private final SortedSet<Integer> _ports;

  private IpProtocol _protocol;

  private final SortedSet<Integer> _sourcePorts;

  public Service(String name) {
    _name = name;
    _ports = new TreeSet<>();
    _sourcePorts = new TreeSet<>();
  }

  public String getDescription() {
    return _description;
  }

  public String getName() {
    return _name;
  }

  public SortedSet<Integer> getPorts() {
    return _ports;
  }

  public IpProtocol getProtocol() {
    return _protocol;
  }

  public SortedSet<Integer> getSourcePorts() {
    return _sourcePorts;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setProtocol(IpProtocol protocol) {
    _protocol = protocol;
  }
}
