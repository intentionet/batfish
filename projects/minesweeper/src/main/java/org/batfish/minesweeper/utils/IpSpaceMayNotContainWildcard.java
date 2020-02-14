package org.batfish.minesweeper.utils;

import java.util.Map;
import org.batfish.common.ip.AclIpSpace;
import org.batfish.common.ip.AclIpSpaceLine;
import org.batfish.common.ip.AclIpSpaceLine.LineAction;
import org.batfish.common.ip.EmptyIpSpace;
import org.batfish.common.ip.GenericIpSpaceVisitor;
import org.batfish.common.ip.IpIpSpace;
import org.batfish.common.ip.IpSpace;
import org.batfish.common.ip.IpSpaceReference;
import org.batfish.common.ip.IpWildcard;
import org.batfish.common.ip.IpWildcardIpSpace;
import org.batfish.common.ip.IpWildcardSetIpSpace;
import org.batfish.common.ip.PrefixIpSpace;
import org.batfish.common.ip.UniverseIpSpace;

/**
 * Test if it's possible (or certain) that {@code _ipWildcard} might not be contained within an
 * input {@link IpSpace}. We do not require this to be perfect -- false positives are OK, but
 * false-negatives are not. This can return false only if we know for certain that {@code
 * _ipWildcard} is contained with the input {@link IpSpace}.
 */
public class IpSpaceMayNotContainWildcard implements GenericIpSpaceVisitor<Boolean> {
  private final IpWildcard _ipWildcard;

  private final IpSpaceMayIntersectWildcard _mayIntersect;

  private final Map<String, IpSpace> _namedIpSpaces;

  public IpSpaceMayNotContainWildcard(IpWildcard ipWildcard, Map<String, IpSpace> namedIpSpaces) {
    _ipWildcard = ipWildcard;
    _mayIntersect = new IpSpaceMayIntersectWildcard(ipWildcard, namedIpSpaces, this);
    _namedIpSpaces = namedIpSpaces;
  }

  public IpSpaceMayNotContainWildcard(
      IpWildcard ipWildcard,
      Map<String, IpSpace> namedIpSpaces,
      IpSpaceMayIntersectWildcard mayIntersect) {
    _ipWildcard = ipWildcard;
    _mayIntersect = mayIntersect;
    _namedIpSpaces = namedIpSpaces;
  }

  @Override
  public Boolean castToGenericIpSpaceVisitorReturnType(Object o) {
    return (Boolean) o;
  }

  private boolean ipSpaceContainsWildcard(IpSpace ipSpace) {
    return !ipSpace.accept(this);
  }

  private boolean ipSpaceMayIntersectWildcard(IpSpace ipSpace) {
    return ipSpace.accept(_mayIntersect);
  }

  @Override
  public Boolean visitAclIpSpace(AclIpSpace aclIpSpace) {
    for (AclIpSpaceLine line : aclIpSpace.getLines()) {
      if (line.getAction() == LineAction.DENY && ipSpaceMayIntersectWildcard(line.getIpSpace())) {
        return true;
      }

      if (line.getAction() == LineAction.PERMIT && ipSpaceContainsWildcard(line.getIpSpace())) {
        return false;
      }
    }
    /*
     * If we reach this point, no PERMIT line is guaranteed to contain ipWildcard. This means
     * it's possible (though not certain) that this does not contain ipWildcard.
     */
    return true;
  }

  @Override
  public Boolean visitEmptyIpSpace(EmptyIpSpace emptyIpSpace) {
    return true;
  }

  @Override
  public Boolean visitIpIpSpace(IpIpSpace ipIpSpace) {
    return !IpWildcard.create(ipIpSpace.getIp()).equals(_ipWildcard);
  }

  @Override
  public Boolean visitIpSpaceReference(IpSpaceReference ipSpaceReference) {
    return _namedIpSpaces.get(ipSpaceReference.getName()).accept(this);
  }

  @Override
  public Boolean visitIpWildcardIpSpace(IpWildcardIpSpace ipWildcardIpSpace) {
    return !ipWildcardIpSpace.getIpWildcard().supersetOf(_ipWildcard);
  }

  @Override
  public Boolean visitIpWildcardSetIpSpace(IpWildcardSetIpSpace ipWildcardSetIpSpace) {
    /* Need to be careful not to give a false-negative here. We can return false only
     * if we're completely sure that ipWildcardSetIpSpace containsIp _ipWildcard.
     */
    return ipWildcardSetIpSpace.getBlacklist().stream().anyMatch(_ipWildcard::subsetOf)
        || ipWildcardSetIpSpace.getWhitelist().stream().noneMatch(_ipWildcard::subsetOf);
  }

  @Override
  public Boolean visitPrefixIpSpace(PrefixIpSpace prefixIpSpace) {
    return !IpWildcard.create(prefixIpSpace.getPrefix()).supersetOf(_ipWildcard);
  }

  @Override
  public Boolean visitUniverseIpSpace(UniverseIpSpace universeIpSpace) {
    return false;
  }
}
