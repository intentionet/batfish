package org.batfish.representation.palo_alto;

import static org.batfish.representation.palo_alto.PaloAltoConfiguration.computeServiceGroupMemberAclName;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.collections4.list.TreeList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.acl.PermittedByAcl;

public final class ServiceGroup implements ServiceGroupMember {
  private static final long serialVersionUID = 1L;

  private final String _name;

  private final SortedSet<ServiceOrServiceGroupReference> _references;

  public ServiceGroup(String name) {
    _name = name;
    _references = new TreeSet<>();
  }

  @Override
  public String getName() {
    return _name;
  }

  public SortedSet<ServiceOrServiceGroupReference> getReferences() {
    return _references;
  }

  @Override
  public IpAccessList toIpAccessList(LineAction action, PaloAltoConfiguration pc, Vsys vsys) {
    List<IpAccessListLine> lines = new TreeList<>();
    for (ServiceOrServiceGroupReference memberReference : _references) {
      String vsysName = memberReference.getVsysName(pc, vsys);
      if (vsysName != null) {
        lines.add(
            new IpAccessListLine(
                action,
                new PermittedByAcl(
                    computeServiceGroupMemberAclName(vsysName, memberReference.getName())),
                _name));
      }
    }
    return new IpAccessList(_name, lines);
  }
}
