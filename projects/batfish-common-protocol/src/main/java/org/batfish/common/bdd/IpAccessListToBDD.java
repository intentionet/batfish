package org.batfish.common.bdd;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.BatfishException;
import org.batfish.common.util.NonRecursiveSupplier;
import org.batfish.common.util.NonRecursiveSupplier.NonRecursiveSupplierException;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.GenericAclLineMatchExprVisitor;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;

/**
 * Converts {@link IpAccessList IpAccessLists} or {@link org.batfish.datamodel.acl.AclLineMatchExpr
 * AclLineMatchExprs} to {@link BDD}.
 */
@ParametersAreNonnullByDefault
public class IpAccessListToBDD implements GenericAclLineMatchExprVisitor<BDD> {

  @Nonnull private final Map<String, Supplier<BDD>> _aclEnv;

  @Nonnull private final BDDFactory _factory;

  @Nonnull private final BDDPacket _pkt;

  @Nonnull private final BDDOps _bddOps;

  @Nonnull private final BDDSourceManager _bddSrcManager;

  @Nonnull private final HeaderSpaceToBDD _headerSpaceToBDD;

  public IpAccessListToBDD(
      @Nonnull BDDPacket pkt,
      @Nonnull BDDSourceManager bddSrcManager,
      @Nonnull Map<String, IpAccessList> aclEnv,
      @Nonnull Map<String, IpSpace> ipSpaceEnv) {
    this(pkt, bddSrcManager, new HeaderSpaceToBDD(pkt, ipSpaceEnv), aclEnv);
  }

  public IpAccessListToBDD(
      @Nonnull BDDPacket pkt,
      @Nonnull BDDSourceManager bddSrcManager,
      @Nonnull HeaderSpaceToBDD headerSpaceToBDD,
      @Nonnull Map<String, IpAccessList> aclEnv) {
    /*
     * Use suppliers to convert each ACL in the environment on demand. Memoize to avoid converting
     * an ACL more than once. ACLs can refer to other ACLs in the environment, but if there is a
     * cyclic reference (direct or indirect), NonRecursiveSupplier will throw an exception (to avoid
     * going into an infinite loop).
     */
    _aclEnv = new HashMap<>();
    aclEnv.forEach(
        (name, acl) ->
            _aclEnv.put(name, Suppliers.memoize(new NonRecursiveSupplier<>(() -> toBdd(acl)))));
    _bddOps = new BDDOps(pkt.getFactory());
    _bddSrcManager = bddSrcManager;
    _factory = pkt.getFactory();
    _headerSpaceToBDD = headerSpaceToBDD;
    _pkt = pkt;
  }

  public static IpAccessListToBDD create(
      BDDPacket pkt,
      BDDSourceManager bddSrcManager,
      Map<String, IpAccessList> aclEnv,
      Map<String, IpSpace> ipSpaceEnv) {
    return new IpAccessListToBDD(pkt, bddSrcManager, aclEnv, ipSpaceEnv);
  }

  public static IpAccessListToBDD create(
      BDDPacket pkt, Map<String, IpAccessList> aclEnv, Map<String, IpSpace> ipSpaceEnv) {
    return new IpAccessListToBDD(
        pkt, BDDSourceManager.forInterfaces(pkt, ImmutableSet.of()), aclEnv, ipSpaceEnv);
  }

  public BDDPacket getBDDPacket() {
    return _pkt;
  }

  /**
   * Convert an Access Control List (ACL) to a symbolic boolean expression. The default action in an
   * ACL is to deny all traffic.
   */
  @Nonnull
  public BDD toBdd(IpAccessList acl) {
    BDDFactory bddFactory = _pkt.getFactory();
    BDD result = bddFactory.zero();
    for (IpAccessListLine line : Lists.reverse(acl.getLines())) {
      BDD lineBDD = visit(line.getMatchCondition());
      BDD actionBDD = line.getAction() == LineAction.PERMIT ? bddFactory.one() : bddFactory.zero();
      result = lineBDD.ite(actionBDD, result);
    }
    return result;
  }

  @Override
  public BDD visitAndMatchExpr(AndMatchExpr andMatchExpr) {
    return _bddOps.and(
        andMatchExpr
            .getConjuncts()
            .stream()
            .map(this::visit)
            .collect(ImmutableList.toImmutableList()));
  }

  @Override
  public BDD visitFalseExpr(FalseExpr falseExpr) {
    return _factory.zero();
  }

  @Override
  public BDD visitMatchHeaderSpace(MatchHeaderSpace matchHeaderSpace) {
    return _headerSpaceToBDD.toBDD(matchHeaderSpace.getHeaderspace());
  }

  @Override
  public BDD visitMatchSrcInterface(MatchSrcInterface matchSrcInterface) {
    return _bddOps.or(
        matchSrcInterface
            .getSrcInterfaces()
            .stream()
            .map(_bddSrcManager::getSourceInterfaceBDD)
            .collect(Collectors.toList()));
  }

  @Override
  public BDD visitNotMatchExpr(NotMatchExpr notMatchExpr) {
    return visit(notMatchExpr.getOperand()).not();
  }

  @Override
  public BDD visitOriginatingFromDevice(OriginatingFromDevice originatingFromDevice) {
    return _bddSrcManager.getOriginatingFromDeviceBDD();
  }

  @Override
  public BDD visitOrMatchExpr(OrMatchExpr orMatchExpr) {
    return _bddOps.or(
        orMatchExpr
            .getDisjuncts()
            .stream()
            .map(this::visit)
            .collect(ImmutableList.toImmutableList()));
  }

  @Override
  public BDD visitPermittedByAcl(PermittedByAcl permittedByAcl) {
    String name = permittedByAcl.getAclName();
    Preconditions.checkArgument(
        _aclEnv.containsKey(name), "Undefined PermittedByAcl reference: %s", name);
    try {
      return _aclEnv.get(name).get();
    } catch (NonRecursiveSupplierException e) {
      throw new BatfishException("Circular PermittedByAcl reference: " + name);
    }
  }

  @Override
  public BDD visitTrueExpr(TrueExpr trueExpr) {
    return _factory.one();
  }
}
