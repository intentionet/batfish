package org.batfish.z3;

import static org.batfish.datamodel.AclIpSpace.difference;
import static org.batfish.datamodel.AclIpSpace.union;

import java.util.Map;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.TrueExpr;

public final class IpSpaceIpAccessListSpecializer extends IpAccessListSpecializer {
  private final boolean _canSpecialize;
  private final IpSpaceSpecializer _dstIpSpaceSpecializer;
  private final IpSpaceSpecializer _srcIpSpaceSpecializer;
  private final IpSpaceSpecializer _srcOrDstIpSpaceSpecializer;

  IpSpaceIpAccessListSpecializer(HeaderSpace headerSpace, Map<String, IpSpace> namedIpSpaces) {
    IpSpace dstIps = headerSpace.getDstIps();
    IpSpace srcIps = headerSpace.getSrcIps();
    IpSpace srcOrDstIps = headerSpace.getSrcOrDstIps();

    IpSpace notDstIps = headerSpace.getNotDstIps();
    IpSpace notSrcIps = headerSpace.getNotSrcIps();

    _dstIpSpaceSpecializer =
        (dstIps == null && srcOrDstIps == null && notDstIps == null)
            ? null
            : new IpSpaceIpSpaceSpecializer(
                difference(union(dstIps, srcOrDstIps), notDstIps), namedIpSpaces);
    _srcIpSpaceSpecializer =
        (srcIps == null && srcOrDstIps == null && notSrcIps == null)
            ? null
            : new IpSpaceIpSpaceSpecializer(
                difference(union(srcIps, srcOrDstIps), notSrcIps), namedIpSpaces);
    _srcOrDstIpSpaceSpecializer =
        (srcIps == null
                && dstIps == null
                && srcOrDstIps == null
                && notSrcIps == null
                && notDstIps == null)
            ? null
            : new IpSpaceIpSpaceSpecializer(
                difference(union(srcIps, dstIps, srcOrDstIps), union(notSrcIps, notDstIps)),
                namedIpSpaces);

    /*
     * Currently, specialization is based on srcIp and dstIp only. We can specialize only
     * if we have at least one IpSpace specializer.
     */
    _canSpecialize = _dstIpSpaceSpecializer != null || _srcIpSpaceSpecializer != null;
  }

  @Override
  public IpAccessList specialize(IpAccessList ipAccessList) {
    if (!_canSpecialize) {
      return ipAccessList;
    } else {
      return super.specialize(ipAccessList);
    }
  }

  private static IpSpace specializeWith(IpSpace dstIpSpace, IpSpaceSpecializer specializer) {
    return dstIpSpace != null && specializer != null
        ? specializer.specialize(dstIpSpace)
        : dstIpSpace;
  }

  @Override
  public AclLineMatchExpr visitMatchHeaderSpace(MatchHeaderSpace matchHeaderSpace) {
    HeaderSpace headerSpace = matchHeaderSpace.getHeaderspace();
    IpSpace dstIps = headerSpace.getDstIps();
    IpSpace notDstIps = headerSpace.getNotDstIps();
    IpSpace notSrcIps = headerSpace.getNotSrcIps();
    IpSpace srcIps = headerSpace.getSrcIps();
    IpSpace srcOrDstIps = headerSpace.getSrcOrDstIps();

    dstIps = specializeWith(dstIps, _dstIpSpaceSpecializer);
    notDstIps = specializeWith(notDstIps, _dstIpSpaceSpecializer);
    notSrcIps = specializeWith(notSrcIps, _srcIpSpaceSpecializer);
    srcIps = specializeWith(srcIps, _srcIpSpaceSpecializer);
    srcOrDstIps = specializeWith(srcOrDstIps, _srcOrDstIpSpaceSpecializer);

    if (constraintUnionEmpty(dstIps, srcOrDstIps)) {
      return FalseExpr.INSTANCE;
    }

    if (constraintUnionEmpty(srcIps, srcOrDstIps)) {
      return FalseExpr.INSTANCE;
    }

    if (notDstIps == UniverseIpSpace.INSTANCE || notSrcIps == UniverseIpSpace.INSTANCE) {
      return FalseExpr.INSTANCE;
    }

    HeaderSpace specializedHeaderSpace =
        headerSpace
            .toBuilder()
            .setDstIps(simplifyPositiveIpConstraint(dstIps))
            .setNotDstIps(simplifyNegativeIpConstraint(notDstIps))
            .setNotSrcIps(simplifyNegativeIpConstraint(notSrcIps))
            .setSrcIps(simplifyPositiveIpConstraint(srcIps))
            .setSrcOrDstIps(simplifyPositiveIpConstraint(srcOrDstIps))
            .build();

    if (specializedHeaderSpace.equals(HeaderSpace.builder().build())) {
      return TrueExpr.INSTANCE;
    }

    return new MatchHeaderSpace(specializedHeaderSpace);
  }
}
