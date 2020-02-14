package org.batfish.minesweeper.smt;

import com.google.common.collect.Lists;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.batfish.common.ip.AclIpSpace;
import org.batfish.common.ip.AclIpSpaceLine;
import org.batfish.common.ip.AclIpSpaceLine.LineAction;
import org.batfish.common.ip.EmptyIpSpace;
import org.batfish.common.ip.GenericIpSpaceVisitor;
import org.batfish.common.ip.IpIpSpace;
import org.batfish.common.ip.IpSpaceReference;
import org.batfish.common.ip.IpWildcard;
import org.batfish.common.ip.IpWildcardIpSpace;
import org.batfish.common.ip.IpWildcardSetIpSpace;
import org.batfish.common.ip.PrefixIpSpace;
import org.batfish.common.ip.UniverseIpSpace;

public class IpSpaceToBoolExpr implements GenericIpSpaceVisitor<BoolExpr> {
  private final Context _context;

  private final BitVecExpr _var;

  public IpSpaceToBoolExpr(Context context, BitVecExpr var) {
    _context = context;
    _var = var;
  }

  @Override
  public BoolExpr castToGenericIpSpaceVisitorReturnType(Object o) {
    return (BoolExpr) o;
  }

  @Override
  public BoolExpr visitAclIpSpace(AclIpSpace aclIpSpace) {
    BoolExpr expr = _context.mkFalse();
    for (AclIpSpaceLine aclIpSpaceLine : Lists.reverse(aclIpSpace.getLines())) {
      BoolExpr matchExpr = aclIpSpaceLine.getIpSpace().accept(this);
      BoolExpr actionExpr =
          aclIpSpaceLine.getAction() == LineAction.PERMIT ? _context.mkTrue() : _context.mkFalse();
      expr = (BoolExpr) _context.mkITE(matchExpr, actionExpr, expr);
    }
    return expr;
  }

  @Override
  public BoolExpr visitEmptyIpSpace(EmptyIpSpace emptyIpSpace) {
    return _context.mkFalse();
  }

  @Override
  public BoolExpr visitIpIpSpace(IpIpSpace ipIpSpace) {
    return _context.mkEq(_var, _context.mkBV(ipIpSpace.getIp().asLong(), 32));
  }

  @Override
  public BoolExpr visitIpWildcardIpSpace(IpWildcardIpSpace ipWildcardIpSpace) {
    return toBoolExpr(ipWildcardIpSpace.getIpWildcard());
  }

  private BoolExpr toBoolExpr(IpWildcard ipWildcard) {
    BitVecExpr ip = _context.mkBV(ipWildcard.getIp().asLong(), 32);
    BitVecExpr mask = _context.mkBV(~ipWildcard.getWildcardMask(), 32);
    return _context.mkEq(_context.mkBVAND(_var, mask), _context.mkBVAND(ip, mask));
  }

  @Override
  public BoolExpr visitIpWildcardSetIpSpace(IpWildcardSetIpSpace ipWildcardSetIpSpace) {
    BoolExpr whitelistExpr =
        _context.mkOr(
            ipWildcardSetIpSpace.getWhitelist().stream()
                .map(this::toBoolExpr)
                .toArray(BoolExpr[]::new));

    BoolExpr blacklistExpr =
        _context.mkOr(
            ipWildcardSetIpSpace.getBlacklist().stream()
                .map(this::toBoolExpr)
                .toArray(BoolExpr[]::new));

    return _context.mkAnd(whitelistExpr, _context.mkNot(blacklistExpr));
  }

  @Override
  public BoolExpr visitPrefixIpSpace(PrefixIpSpace prefixIpSpace) {
    return toBoolExpr(IpWildcard.create(prefixIpSpace.getPrefix()));
  }

  @Override
  public BoolExpr visitUniverseIpSpace(UniverseIpSpace universeIpSpace) {
    return _context.mkTrue();
  }

  @Override
  public BoolExpr visitIpSpaceReference(IpSpaceReference ipSpaceReference) {
    throw new UnsupportedOperationException("no implementation for generated method");
  }
}
