package org.batfish.representation.palo_alto;

import javax.annotation.Nonnull;

public class ServiceOrServiceGroupReference
    implements ServiceGroupMember, Comparable<ServiceOrServiceGroupReference> {
  private static final long serialVersionUID = 1L;

  private final String _name;

  public ServiceOrServiceGroupReference(String name) {
    _name = name;
  }

  @Override
  public int compareTo(@Nonnull ServiceOrServiceGroupReference o) {
    return _name.compareTo(o._name);
  }
}
