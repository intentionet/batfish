package org.batfish.datamodel.visitors;

import java.util.List;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclIpSpaceLine;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpIpSpace;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcardIpSpace;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.PrefixIpSpace;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclTracer;

/**
 * Evaluates whether an {@link IpSpace} contains an {@link Ip}.<br>
 * Visit functions:<br>
 * - add tracing information via {@code _aclTracer} if the {@link IpSpace} is named<br>
 * - return true iff the {@link IpSpace} contains {@code _ip}.
 */
public class IpSpaceTracer implements GenericIpSpaceVisitor<Boolean> {

  private final IpSpaceDescriber _ipSpaceDescriber;

  private final Ip _ip;

  private final AclTracer _aclTracer;

  private final String _ipDescription;

  public IpSpaceTracer(AclTracer aclTracer, Ip ip, String ipDescription) {
    _aclTracer = aclTracer;
    _ip = ip;
    _ipDescription = ipDescription;
    _ipSpaceDescriber = new IpSpaceDescriber(aclTracer);
  }

  @Override
  public Boolean castToGenericIpSpaceVisitorReturnType(Object o) {
    return (Boolean) o;
  }

  @Override
  public Boolean visitAclIpSpace(AclIpSpace aclIpSpace) {
    String name = _aclTracer.getIpSpaceNames().get(aclIpSpace);
    _aclTracer.newTrace();
    List<AclIpSpaceLine> lines = aclIpSpace.getLines();
    for (int i = 0; i < lines.size(); i++) {
      AclIpSpaceLine line = lines.get(i);
      if (line.getIpSpace().accept(this)) {
        if (name != null) {
          _aclTracer.recordAction(
              name,
              _aclTracer.getIpSpaceMetadata().get(aclIpSpace),
              i,
              line,
              _ip,
              _ipDescription,
              _ipSpaceDescriber);
        }
        _aclTracer.endTrace();
        return line.getAction() == LineAction.PERMIT;
      }
    }
    if (name != null) {
      _aclTracer.recordDefaultDeny(
          name, _aclTracer.getIpSpaceMetadata().get(aclIpSpace), _ip, _ipDescription);
    }
    _aclTracer.endTraceNoMatch();
    return false;
  }

  @Override
  public Boolean visitEmptyIpSpace(EmptyIpSpace emptyIpSpace) {
    return reportIfNamed(emptyIpSpace);
  }

  @Override
  public Boolean visitIpIpSpace(IpIpSpace ipIpSpace) {
    return reportIfNamed(ipIpSpace);
  }

  @Override
  public Boolean visitIpSpaceReference(IpSpaceReference ipSpaceReference) {
    String name = ipSpaceReference.getName();
    IpSpace ipSpace = _aclTracer.getNamedIpSpaces().get(name);
    if (ipSpace != null) {
      _aclTracer.newTrace();
      Boolean accepted = ipSpace.accept(this);
      if (!accepted) {
        _aclTracer.endTraceNoMatch();
      } else {
        _aclTracer.endTrace();
      }
      return accepted;
    } else {
      return false;
    }
  }

  private boolean reportIfNamed(IpSpace ipSpace) {
    boolean result = ipSpace.containsIp(_ip, _aclTracer.getNamedIpSpaces());
    String name = _aclTracer.getIpSpaceNames().get(ipSpace);
    if (name != null && result) {
      _aclTracer.recordNamedIpSpaceAction(
          name,
          ipSpace.accept(_ipSpaceDescriber),
          _aclTracer.getIpSpaceMetadata().get(ipSpace),
          result,
          _ip,
          _ipDescription);
    }
    return result;
  }

  @Override
  public Boolean visitIpWildcardIpSpace(IpWildcardIpSpace ipWildcardIpSpace) {
    return reportIfNamed(ipWildcardIpSpace);
  }

  @Override
  public Boolean visitIpWildcardSetIpSpace(IpWildcardSetIpSpace ipWildcardSetIpSpace) {
    return reportIfNamed(ipWildcardSetIpSpace);
  }

  @Override
  public Boolean visitPrefixIpSpace(PrefixIpSpace prefixIpSpace) {
    return reportIfNamed(prefixIpSpace);
  }

  @Override
  public Boolean visitUniverseIpSpace(UniverseIpSpace universeIpSpace) {
    return reportIfNamed(universeIpSpace);
  }
}
