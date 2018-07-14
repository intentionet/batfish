package org.batfish.z3;

import static org.batfish.datamodel.AclIpSpace.difference;
import static org.batfish.datamodel.AclIpSpace.union;

import java.util.Map;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpSpace;

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
  protected boolean canSpecialize() {
    return _canSpecialize;
  }

  private static IpSpace specializeWith(IpSpace dstIpSpace, IpSpaceSpecializer specializer) {
    return dstIpSpace != null && specializer != null
        ? specializer.specialize(dstIpSpace)
        : dstIpSpace;
  }

  @Override
  protected HeaderSpace specialize(HeaderSpace headerSpace) {
    return headerSpace
        .toBuilder()
        .setDstIps(specializeWith(headerSpace.getDstIps(), _dstIpSpaceSpecializer))
        .setNotDstIps(specializeWith(headerSpace.getNotDstIps(), _dstIpSpaceSpecializer))
        .setNotSrcIps(specializeWith(headerSpace.getNotSrcIps(), _srcIpSpaceSpecializer))
        .setSrcIps(specializeWith(headerSpace.getSrcIps(), _srcIpSpaceSpecializer))
        .setSrcOrDstIps(specializeWith(headerSpace.getSrcOrDstIps(), _srcOrDstIpSpaceSpecializer))
        .build();
  }
}
