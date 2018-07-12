package org.batfish.representation.palo_alto;

import java.util.SortedSet;
import java.util.TreeSet;

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
}
