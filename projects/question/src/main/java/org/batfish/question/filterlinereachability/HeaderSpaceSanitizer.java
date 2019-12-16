package org.batfish.question.filterlinereachability;

import java.util.Map;
import java.util.stream.Collectors;
import org.batfish.datamodel.AbstractAclLine;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.CircularReferenceException;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.GenericAclLineMatchExprVisitor;
import org.batfish.datamodel.acl.GenericIpAccessListLineVisitor;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.acl.UndefinedReferenceException;
import org.batfish.datamodel.visitors.IpSpaceDereferencer;

/**
 * Makes a version of the given {@link AbstractAclLine} or {@link AclLineMatchExpr} with any named
 * IP space references replaced with the dereferenced {@link IpSpace}. Throws {@link
 * CircularReferenceException} if any circular IP space reference is referenced, or {@link
 * UndefinedReferenceException} if any undefined IP space is referenced.
 */
public class HeaderSpaceSanitizer
    implements GenericAclLineMatchExprVisitor<AclLineMatchExpr>,
        GenericIpAccessListLineVisitor<AbstractAclLine> {

  private final Map<String, IpSpace> _namedIpSpaces;

  public HeaderSpaceSanitizer(Map<String, IpSpace> namedIpSpaces) {
    _namedIpSpaces = namedIpSpaces;
  }

  /* AbstractAclLine visit methods */

  @Override
  public AbstractAclLine visitIpAccessListLine(IpAccessListLine ipAccessListLine) {
    return ipAccessListLine
        .toBuilder()
        .setMatchCondition(visit(ipAccessListLine.getMatchCondition()))
        .build();
  }

  /* AclLineMatchExpr visit methods */

  @Override
  public AclLineMatchExpr visitAndMatchExpr(AndMatchExpr andMatchExpr)
      throws CircularReferenceException, UndefinedReferenceException {
    return new AndMatchExpr(
        andMatchExpr.getConjuncts().stream().map(this::visit).collect(Collectors.toList()));
  }

  @Override
  public AclLineMatchExpr visitFalseExpr(FalseExpr falseExpr) {
    return falseExpr;
  }

  @Override
  public AclLineMatchExpr visitMatchHeaderSpace(MatchHeaderSpace matchHeaderSpace)
      throws CircularReferenceException, UndefinedReferenceException {
    return new MatchHeaderSpace(
        IpSpaceDereferencer.dereferenceHeaderSpace(
            matchHeaderSpace.getHeaderspace(), _namedIpSpaces));
  }

  @Override
  public AclLineMatchExpr visitMatchSrcInterface(MatchSrcInterface matchSrcInterface) {
    return matchSrcInterface;
  }

  @Override
  public AclLineMatchExpr visitNotMatchExpr(NotMatchExpr notMatchExpr)
      throws CircularReferenceException, UndefinedReferenceException {
    return new NotMatchExpr(visit(notMatchExpr.getOperand()));
  }

  @Override
  public AclLineMatchExpr visitOriginatingFromDevice(OriginatingFromDevice originatingFromDevice) {
    return originatingFromDevice;
  }

  @Override
  public AclLineMatchExpr visitOrMatchExpr(OrMatchExpr orMatchExpr)
      throws CircularReferenceException, UndefinedReferenceException {
    return new OrMatchExpr(
        orMatchExpr.getDisjuncts().stream().map(this::visit).collect(Collectors.toList()));
  }

  @Override
  public AclLineMatchExpr visitPermittedByAcl(PermittedByAcl permittedByAcl) {
    return permittedByAcl;
  }

  @Override
  public AclLineMatchExpr visitTrueExpr(TrueExpr trueExpr) {
    return trueExpr;
  }
}
