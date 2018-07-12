package org.batfish.symbolic.bdd;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.acl.AclLineMatchExpr;
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

/** Visit an {@link AclLineMatchExpr} and convert it to a BDD. */
public class AclLineMatchExprToBDD implements GenericAclLineMatchExprVisitor<BDD> {

  private final BDDOps _bddOps;

  private final BDDFactory _factory;

  private final BDDPacket _packet;

  public AclLineMatchExprToBDD(BDDFactory factory, BDDPacket packet) {
    _factory = factory;
    _bddOps = new BDDOps(factory);
    _packet = packet;
  }

  private static <T> void forbidHeaderSpaceField(String fieldName, Set<T> fieldValue) {
    if (fieldValue != null && !fieldValue.isEmpty()) {
      throw new BatfishException("unsupported HeaderSpace field " + fieldName);
    }
  }

  private static void forbidHeaderSpaceField(String fieldName, IpSpace fieldValue) {
    if (fieldValue != null && fieldValue != EmptyIpSpace.INSTANCE) {
      throw new BatfishException("unsupported HeaderSpace field " + fieldName);
    }
  }

  public @Nonnull BDD toBDD(AclLineMatchExpr expr) {
    return expr.accept(this);
  }

  private @Nullable BDD toBDD(@Nullable IpSpace ipSpace, BDDInteger var) {
    if (ipSpace == null) {
      return null;
    }
    return ipSpace.accept(new IpSpaceToBDD(_factory, var));
  }

  private @Nullable BDD toBDD(Set<IpProtocol> ipProtocols) {
    if (ipProtocols == null || ipProtocols.isEmpty()) {
      return null;
    }

    return _bddOps.or(
        ipProtocols
            .stream()
            .map(IpProtocol::number)
            .map(_packet.getIpProtocol()::value)
            .collect(Collectors.toList()));
  }

  private @Nullable BDD toBDD(@Nullable Set<SubRange> ranges, BDDInteger var) {
    if (ranges == null || ranges.isEmpty()) {
      return null;
    }
    return _bddOps.or(
        ranges.stream().map(range -> toBDD(range, var)).collect(ImmutableList.toImmutableList()));
  }

  private static BDD toBDD(SubRange range, BDDInteger var) {
    long start = range.getStart();
    long end = range.getEnd();
    return start == end ? var.value(start) : var.geq(start).and(var.leq(end));
  }

  private BDD toBDD(List<TcpFlags> tcpFlags) {
    if (tcpFlags == null || tcpFlags.isEmpty()) {
      return null;
    }

    return _bddOps.or(tcpFlags.stream().map(this::toBDD).collect(ImmutableList.toImmutableList()));
  }

  /** For TcpFlags */
  private static BDD toBDD(boolean useFlag, boolean flagValue, BDD flagBDD) {
    return useFlag ? flagValue ? flagBDD : flagBDD.not() : null;
  }

  private BDD toBDD(TcpFlags tcpFlags) {
    return _bddOps.and(
        toBDD(tcpFlags.getUseAck(), tcpFlags.getAck(), _packet.getTcpAck()),
        toBDD(tcpFlags.getUseCwr(), tcpFlags.getCwr(), _packet.getTcpCwr()),
        toBDD(tcpFlags.getUseEce(), tcpFlags.getEce(), _packet.getTcpEce()),
        toBDD(tcpFlags.getUseFin(), tcpFlags.getFin(), _packet.getTcpFin()),
        toBDD(tcpFlags.getUsePsh(), tcpFlags.getPsh(), _packet.getTcpPsh()),
        toBDD(tcpFlags.getUseRst(), tcpFlags.getRst(), _packet.getTcpRst()),
        toBDD(tcpFlags.getUseSyn(), tcpFlags.getSyn(), _packet.getTcpSyn()),
        toBDD(tcpFlags.getUseUrg(), tcpFlags.getUrg(), _packet.getTcpUrg()));
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
    HeaderSpace headerSpace = matchHeaderSpace.getHeaderspace();
    forbidHeaderSpaceField("dscps", headerSpace.getDscps());
    forbidHeaderSpaceField("ecns", headerSpace.getEcns());
    forbidHeaderSpaceField("fragmentOffsets", headerSpace.getFragmentOffsets());
    forbidHeaderSpaceField("notDscps", headerSpace.getNotDscps());
    forbidHeaderSpaceField("notDstIps", headerSpace.getNotDstIps());
    forbidHeaderSpaceField("notDstPorts", headerSpace.getNotDstPorts());
    forbidHeaderSpaceField("notEcns", headerSpace.getNotEcns());
    forbidHeaderSpaceField("notFragmentOffsets", headerSpace.getNotFragmentOffsets());
    forbidHeaderSpaceField("notIcmpCodes", headerSpace.getNotIcmpCodes());
    forbidHeaderSpaceField("notIcmpTypes", headerSpace.getNotIcmpTypes());
    forbidHeaderSpaceField("notIpProtocols", headerSpace.getNotIpProtocols());
    forbidHeaderSpaceField("notSrcIps", headerSpace.getNotSrcIps());
    forbidHeaderSpaceField("notSrcPorts", headerSpace.getNotSrcPorts());
    forbidHeaderSpaceField("states", headerSpace.getStates());

    return _bddOps.and(
        toBDD(headerSpace.getDstIps(), _packet.getDstIp()),
        toBDD(headerSpace.getSrcIps(), _packet.getSrcIp()),
        BDDOps.orNull(
            toBDD(headerSpace.getSrcOrDstIps(), _packet.getDstIp()),
            toBDD(headerSpace.getSrcOrDstIps(), _packet.getSrcIp())),
        toBDD(headerSpace.getDstPorts(), _packet.getDstPort()),
        toBDD(headerSpace.getSrcPorts(), _packet.getSrcPort()),
        BDDOps.orNull(
            toBDD(headerSpace.getSrcOrDstPorts(), _packet.getSrcPort()),
            toBDD(headerSpace.getSrcOrDstPorts(), _packet.getDstPort())),
        toBDD(headerSpace.getTcpFlags()),
        toBDD(headerSpace.getIcmpCodes(), _packet.getIcmpCode()),
        toBDD(headerSpace.getIcmpTypes(), _packet.getIcmpType()),
        toBDD(headerSpace.getIpProtocols()));
  }

  @Override
  public BDD visitMatchSrcInterface(MatchSrcInterface matchSrcInterface) {
    throw new BatfishException("MatchSrcInterface is unsupported");
  }

  @Override
  public BDD visitNotMatchExpr(NotMatchExpr notMatchExpr) {
    return notMatchExpr.getOperand().accept(this).not();
  }

  @Override
  public BDD visitOriginatingFromDevice(OriginatingFromDevice originatingFromDevice) {
    throw new BatfishException("OriginatingFromDevice is unsupported");
  }

  @Override
  public BDD visitOrMatchExpr(OrMatchExpr orMatchExpr) {
    return _bddOps.or(
        orMatchExpr
            .getDisjuncts()
            .stream()
            .map(this::toBDD)
            .collect(ImmutableList.toImmutableList()));
  }

  @Override
  public BDD visitPermittedByAcl(PermittedByAcl permittedByAcl) {
    throw new BatfishException("PermittedByAcl is unsupported");
  }

  @Override
  public BDD visitTrueExpr(TrueExpr trueExpr) {
    return _factory.one();
  }
}
