package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.symbolic.bdd.BDDInteger;
import org.batfish.symbolic.bdd.BDDOps;
import org.batfish.symbolic.bdd.BDDPacket;
import org.batfish.symbolic.bdd.IpSpaceToBDD;

public final class BDDIpAccessListSpecializer extends IpAccessListSpecializer {
  private final BDDIpSpaceSpecializer _dstIpSpaceSpecializer;
  private final BDD _headerSpaceBDD;
  private final BDDPacket _pkt;
  private final BDDIpSpaceSpecializer _srcIpSpaceSpecializer;

  public BDDIpAccessListSpecializer(
      BDDPacket pkt, BDD headerSpaceBDD, Map<String, IpSpace> namedIpSpaces) {
    _dstIpSpaceSpecializer =
        new BDDIpSpaceSpecializer(
            headerSpaceBDD,
            namedIpSpaces,
            new IpSpaceToBDD(pkt.getDstIp().getFactory(), pkt.getDstIp(), namedIpSpaces));
    _pkt = pkt;
    _headerSpaceBDD = headerSpaceBDD;
    _srcIpSpaceSpecializer =
        new BDDIpSpaceSpecializer(
            headerSpaceBDD,
            namedIpSpaces,
            new IpSpaceToBDD(pkt.getSrcIp().getFactory(), pkt.getSrcIp(), namedIpSpaces));
  }

  @Override
  boolean canSpecialize() {
    // _headerspaceBDD.isOne means there is no constraint.
    return !_headerSpaceBDD.isOne();
  }

  @Override
  public HeaderSpace specialize(HeaderSpace headerSpace) {
    return headerSpace
        .toBuilder()
        // combine dstIps and notDstIps into dstIps
        .setDstIps(specializeIpSpace(headerSpace.getDstIps(), _dstIpSpaceSpecializer))
        .setNotDstIps(specializeIpSpace(headerSpace.getNotDstIps(), _dstIpSpaceSpecializer))
        .setDstPorts(specializeSubRange(headerSpace.getDstPorts(), _pkt.getDstPort()))
        .setNotDstPorts(specializeSubRange(headerSpace.getNotDstPorts(), _pkt.getDstPort()))
        .setIpProtocols(specializeIpProtocols(headerSpace.getIpProtocols()))
        .setIcmpCodes(specializeSubRange(headerSpace.getIcmpCodes(), _pkt.getIcmpCode()))
        .setIcmpTypes(specializeSubRange(headerSpace.getIcmpTypes(), _pkt.getIcmpType()))
        .setSrcOrDstIps(
            specializeIpSpace(
                headerSpace.getSrcOrDstIps(), _dstIpSpaceSpecializer, _srcIpSpaceSpecializer))
        .setSrcOrDstPorts(
            specializeSubRange(
                headerSpace.getSrcOrDstPorts(), _pkt.getSrcPort(), _pkt.getDstPort()))
        .setSrcPorts(specializeSubRange(headerSpace.getSrcPorts(), _pkt.getSrcPort()))
        .setNotSrcPorts(specializeSubRange(headerSpace.getNotSrcPorts(), _pkt.getSrcPort()))
        .setSrcIps(specializeIpSpace(headerSpace.getSrcIps(), _srcIpSpaceSpecializer))
        .setNotSrcIps(specializeIpSpace(headerSpace.getNotSrcIps(), _srcIpSpaceSpecializer))
        .setTcpFlags(specializeTcpFlags(headerSpace.getTcpFlags()))
        .build();
  }

  private @Nullable static IpSpace specializeIpSpace(
      @Nullable IpSpace ipSpace, BDDIpSpaceSpecializer... specializers) {
    if (ipSpace == null) {
      return null;
    }
    // union the subspaces relevant to each specializer
    List<IpSpace> specializedIpSpaces =
        Arrays.stream(specializers)
            .map(specializer -> specializer.specialize(ipSpace))
            .collect(Collectors.toList());
    return specializedIpSpaces.contains(UniverseIpSpace.INSTANCE)
        ? UniverseIpSpace.INSTANCE
        : AclIpSpace.union(specializedIpSpaces);
  }

  private @Nullable SortedSet<IpProtocol> specializeIpProtocols(
      @Nullable SortedSet<IpProtocol> ipProtocols) {
    return ipProtocols == null
        ? null
        : ipProtocols
            .stream()
            .map(
                ipProtocol ->
                    _headerSpaceBDD.and(_pkt.getIpProtocol().value(ipProtocol.number())).isZero()
                        ? null
                        : ipProtocol)
            .filter(Objects::nonNull)
            .collect(ImmutableSortedSet.toImmutableSortedSet(ipProtocols.comparator()));
  }

  private @Nullable SortedSet<SubRange> specializeSubRange(
      @Nullable SortedSet<SubRange> subRanges, BDDInteger... vars) {
    List<BDDInteger> varList = Arrays.asList(vars);
    return subRanges == null
        ? null
        : subRanges
            .stream()
            .map(
                subRange ->
                    varList
                            .stream()
                            .allMatch(var -> _headerSpaceBDD.and(toBDD(subRange, var)).isZero())
                        ? null
                        : subRange)
            .filter(Objects::nonNull)
            .collect(ImmutableSortedSet.toImmutableSortedSet(subRanges.comparator()));
  }

  private @Nullable IpSpace specializeSrcIps(@Nullable IpSpace srcIps) {
    return srcIps == null ? null : _srcIpSpaceSpecializer.specialize(srcIps);
  }

  private @Nullable List<TcpFlags> specializeTcpFlags(@Nullable List<TcpFlags> tcpFlags) {
    return tcpFlags == null
        ? null
        : tcpFlags
            .stream()
            .map(
                flags -> {
                  BDD flagsBDD =
                      BDDOps.andNull(
                          tcpFlagBDD(flags.getUseAck(), flags.getAck(), _pkt.getTcpAck()),
                          tcpFlagBDD(flags.getUseCwr(), flags.getCwr(), _pkt.getTcpCwr()),
                          tcpFlagBDD(flags.getUseEce(), flags.getEce(), _pkt.getTcpEce()),
                          tcpFlagBDD(flags.getUseFin(), flags.getFin(), _pkt.getTcpFin()),
                          tcpFlagBDD(flags.getUsePsh(), flags.getPsh(), _pkt.getTcpPsh()),
                          tcpFlagBDD(flags.getUseRst(), flags.getRst(), _pkt.getTcpRst()),
                          tcpFlagBDD(flags.getUseUrg(), flags.getUrg(), _pkt.getTcpUrg()));
                  return flagsBDD == null || _headerSpaceBDD.and(flagsBDD).isZero() ? null : flags;
                })
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
  }

  private @Nullable static BDD tcpFlagBDD(boolean use, boolean value, BDD bitBDD) {
    if (!use) {
      return null;
    }
    return value ? bitBDD : bitBDD.not();
  }

  private static BDD toBDD(SubRange subRange, BDDInteger var) {
    return var.geq(subRange.getStart()).and(var.leq(subRange.getEnd()));
  }
}
