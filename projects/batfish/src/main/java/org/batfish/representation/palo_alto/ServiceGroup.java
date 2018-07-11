package org.batfish.representation.palo_alto;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;

public final class ServiceGroup implements ServiceGroupMember {
  private static final long serialVersionUID = 1L;

  private final String _name;

  private final SortedSet<ServiceGroupMember> _members;

  public ServiceGroup(String name) {
    _name = name;
    _members = new TreeSet<>();
  }

  public String getName() {
    return _name;
  }

  public SortedSet<ServiceGroupMember> getMembers() {
    return _members;
  }

  @Override
  public void applyTo(Vsys vsys, LineAction action, List<IpAccessListLine> lines) {
    for (ServiceGroupMember member : _members) {
      member.applyTo(vsys, action, lines);
    }
  }
}
