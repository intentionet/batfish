package org.batfish.datamodel.visitors;

import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.UniverseIpSpace;

public class IpSpaceComparator implements GenericIpSpaceVisitor<Integer> {

  private final IpSpace _lhs;

  private IpSpaceComparator(IpSpace lhs) {
    _lhs = lhs;
  }

  public static int compare(IpSpace lhs, IpSpace rhs) {
    if (lhs == rhs) {
      return 0;
    }
    if (lhs.getClass() != rhs.getClass()) {
      return lhs.getClass().getCanonicalName().compareTo(rhs.getClass().getCanonicalName());
    }
    return rhs.accept(new IpSpaceComparator(lhs));
  }

  @Override
  public Integer castToGenericIpSpaceVisitorReturnType(Object o) {
    return (Integer) o;
  }

  @Override
  public Integer visitAclIpSpace(AclIpSpace aclIpSpace) {
    return ((AclIpSpace) _lhs).compareTo(aclIpSpace);
  }

  @Override
  public Integer visitEmptyIpSpace(EmptyIpSpace emptyIpSpace) {
    return 0;
  }

  @Override
  public Integer visitIp(Ip ip) {
    return ((Ip) _lhs).compareTo(ip);
  }

  @Override
  public Integer visitIpWildcard(IpWildcard ipWildcard) {
    return ((IpWildcard) _lhs).compareTo(ipWildcard);
  }

  @Override
  public Integer visitIpWildcardSetIpSpace(IpWildcardSetIpSpace ipWildcardSetIpSpace) {
    return ((IpWildcardSetIpSpace) _lhs).compareTo(ipWildcardSetIpSpace);
  }

  @Override
  public Integer visitPrefix(Prefix prefix) {
    return ((Prefix) _lhs).compareTo(prefix);
  }

  @Override
  public Integer visitUniverseIpSpace(UniverseIpSpace universeIpSpace) {
    return 0;
  }
}
