package org.batfish.datamodel.acl;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import javax.annotation.Nonnull;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlagsMatchConditions;

public final class AclLineMatchExprs {

  private AclLineMatchExprs() {}

  public static final FalseExpr FALSE = FalseExpr.INSTANCE;

  public static final OriginatingFromDevice ORIGINATING_FROM_DEVICE =
      OriginatingFromDevice.INSTANCE;

  public static final TrueExpr TRUE = TrueExpr.INSTANCE;

  public static AclLineMatchExpr and(AclLineMatchExpr... exprs) {
    return and(
        Arrays.stream(exprs)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  /**
   * Constant-time constructor for AndMatchExpr. Simplifies if given zero or one conjunct. Doesn't
   * do other simplifications that require more work (like removing all {@link TrueExpr TrueExprs}).
   */
  public static AclLineMatchExpr and(Iterable<AclLineMatchExpr> exprs) {
    Iterator<AclLineMatchExpr> iter = exprs.iterator();

    if (!iter.hasNext()) {
      // Empty. Return the identity element
      return TrueExpr.INSTANCE;
    }

    AclLineMatchExpr first = iter.next();
    if (!iter.hasNext()) {
      // Only 1 element
      return first;
    }

    return new AndMatchExpr(exprs);
  }

  public static MatchHeaderSpace match(HeaderSpace headerSpace) {
    return new MatchHeaderSpace(headerSpace);
  }

  public static @Nonnull AclLineMatchExpr matchDscp(DscpType dscp) {
    return matchDscp(dscp.number());
  }

  public static @Nonnull AclLineMatchExpr matchDscp(int dscp) {
    checkArgument(0 <= dscp && dscp <= 63, "Invalid DSCP: %s", dscp);
    return new MatchHeaderSpace(HeaderSpace.builder().setDscps(ImmutableList.of(dscp)).build());
  }

  public static MatchHeaderSpace matchDst(IpSpace ipSpace) {
    return new MatchHeaderSpace(HeaderSpace.builder().setDstIps(ipSpace).build());
  }

  public static MatchHeaderSpace matchDst(Ip ip) {
    return matchDst(ip.toIpSpace());
  }

  public static MatchHeaderSpace matchDst(IpWildcard wc) {
    return matchDst(wc.toIpSpace());
  }

  public static MatchHeaderSpace matchDst(Prefix prefix) {
    return matchDst(prefix.toIpSpace());
  }

  public static MatchHeaderSpace matchDstIp(String ip) {
    return matchDst(Ip.parse(ip).toIpSpace());
  }

  public static @Nonnull AclLineMatchExpr matchDstPort(int port) {
    checkArgument(0 <= port && port <= 0xFFFF, "Invalid port: %s", port);
    return match(
        HeaderSpace.builder().setDstPorts(ImmutableList.of(SubRange.singleton(port))).build());
  }

  public static @Nonnull AclLineMatchExpr matchDstPort(IntegerSpace portSpace) {
    checkArgument(
        0 <= portSpace.least() && portSpace.greatest() <= 0xFFFF,
        "Invalid port space: %s",
        portSpace);
    return match(HeaderSpace.builder().setDstPorts(portSpace.getSubRanges()).build());
  }

  public static AclLineMatchExpr matchDstPrefix(String prefix) {
    return matchDst(Prefix.parse(prefix));
  }

  public static MatchHeaderSpace matchSrc(IpSpace ipSpace) {
    return new MatchHeaderSpace(HeaderSpace.builder().setSrcIps(ipSpace).build());
  }

  public static @Nonnull AclLineMatchExpr matchFragmentOffset(int fragmentOffset) {
    checkArgument(
        0 <= fragmentOffset && fragmentOffset <= 8191,
        "Invalid fragment offset: %s",
        fragmentOffset);
    return new MatchHeaderSpace(
        HeaderSpace.builder()
            .setFragmentOffsets(ImmutableList.of(SubRange.singleton(fragmentOffset)))
            .build());
  }

  public static @Nonnull AclLineMatchExpr matchFragmentOffset(IntegerSpace fragmentOffsetSpace) {
    checkArgument(
        0 <= fragmentOffsetSpace.least() && fragmentOffsetSpace.greatest() <= 8191,
        "Invalid fragmentOffsetSpace: %s",
        fragmentOffsetSpace);
    return new MatchHeaderSpace(
        HeaderSpace.builder().setFragmentOffsets(fragmentOffsetSpace.getSubRanges()).build());
  }

  public static @Nonnull AclLineMatchExpr matchIcmp(int icmpType, int icmpCode) {
    checkArgument(0 <= icmpCode && icmpCode <= 255, "Invalid ICMP code: %s", icmpCode);
    return and(
        matchIcmpType(icmpType),
        new MatchHeaderSpace(
            HeaderSpace.builder()
                .setIcmpCodes(ImmutableList.of(SubRange.singleton(icmpCode)))
                .build()));
  }

  public static @Nonnull AclLineMatchExpr matchIcmpType(int icmpType) {
    checkArgument(0 <= icmpType && icmpType <= 255, "Invalid ICMP type: %s", icmpType);
    return new MatchHeaderSpace(
        HeaderSpace.builder().setIcmpTypes(ImmutableList.of(SubRange.singleton(icmpType))).build());
  }

  public static @Nonnull AclLineMatchExpr matchIpProtocol(int ipProtocolNumber) {
    checkArgument(
        0 <= ipProtocolNumber && ipProtocolNumber <= 255,
        "Invalid IP protocol number: %s",
        ipProtocolNumber);
    return new MatchHeaderSpace(
        HeaderSpace.builder()
            .setIpProtocols(ImmutableList.of(IpProtocol.fromNumber(ipProtocolNumber)))
            .build());
  }

  public static @Nonnull AclLineMatchExpr matchIpProtocol(IpProtocol ipProtocol) {
    return new MatchHeaderSpace(
        HeaderSpace.builder().setIpProtocols(ImmutableList.of(ipProtocol)).build());
  }

  public static @Nonnull AclLineMatchExpr matchPacketLength(IntegerSpace packetLengthSpace) {
    checkArgument(
        0 <= packetLengthSpace.least() && packetLengthSpace.greatest() <= 0xFFFF,
        "Invalid packetLengthSpace: %s",
        packetLengthSpace);
    return new MatchHeaderSpace(
        HeaderSpace.builder().setPacketLengths(packetLengthSpace.getSubRanges()).build());
  }

  public static @Nonnull AclLineMatchExpr matchPacketLength(int packetLength) {
    checkArgument(
        0 <= packetLength && packetLength <= 0xFFFF, "Invalid packetLength: %s", packetLength);
    return new MatchHeaderSpace(
        HeaderSpace.builder()
            .setPacketLengths(ImmutableList.of(SubRange.singleton(packetLength)))
            .build());
  }

  public static MatchHeaderSpace matchSrc(Ip ip) {
    return matchSrc(ip.toIpSpace());
  }

  public static @Nonnull AclLineMatchExpr matchSrc(IpWildcard wc) {
    return matchSrc(wc.toIpSpace());
  }

  public static MatchHeaderSpace matchSrc(Prefix prefix) {
    return matchSrc(prefix.toIpSpace());
  }

  public static MatchHeaderSpace matchSrcIp(String ip) {
    return matchSrc(Ip.parse(ip).toIpSpace());
  }

  public static MatchHeaderSpace matchSrcPrefix(String prefix) {
    return matchSrc(Prefix.parse(prefix).toIpSpace());
  }

  public static MatchHeaderSpace matchSrcPort(int port) {
    checkArgument(0 <= port && port <= 0xFFFF, "Invalid port: %s", port);
    return match(
        HeaderSpace.builder().setSrcPorts(ImmutableList.of(SubRange.singleton(port))).build());
  }

  public static @Nonnull AclLineMatchExpr matchSrcPort(IntegerSpace portSpace) {
    checkArgument(
        0 <= portSpace.least() && portSpace.greatest() <= 0xFFFF,
        "Invalid port space: %s",
        portSpace);
    return match(HeaderSpace.builder().setSrcPorts(portSpace.getSubRanges()).build());
  }

  public static @Nonnull MatchSrcInterface matchSrcInterface(Iterable<String> ifaces) {
    return new MatchSrcInterface(ImmutableList.copyOf(ifaces));
  }

  public static @Nonnull MatchSrcInterface matchSrcInterface(String... ifaces) {
    return matchSrcInterface(ImmutableList.copyOf(ifaces));
  }

  public static @Nonnull AclLineMatchExpr matchTcpFlags(
      TcpFlagsMatchConditions... tcpFlagsMatchConditions) {
    return new MatchHeaderSpace(
        HeaderSpace.builder().setTcpFlags(ImmutableList.copyOf(tcpFlagsMatchConditions)).build());
  }

  /**
   * Smart constructor for {@link NotMatchExpr} that does constant-time simplifications (i.e. when
   * expr is {@link #TRUE} or {@link #FALSE}).
   */
  public static AclLineMatchExpr not(AclLineMatchExpr expr) {
    if (expr == TRUE) {
      return FALSE;
    }
    if (expr == FALSE) {
      return TRUE;
    }
    if (expr instanceof NotMatchExpr) {
      return ((NotMatchExpr) expr).getOperand();
    }
    return new NotMatchExpr(expr);
  }

  public static AclLineMatchExpr or(AclLineMatchExpr... exprs) {
    return or(
        Arrays.stream(exprs).collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural())));
  }

  /**
   * Constant-time constructor for OrMatchExpr. Simplifies if given zero or one conjunct. Doesn't do
   * other simplifications that require more work (like removing all {@link FalseExpr FalseExprs}).
   */
  public static AclLineMatchExpr or(Iterable<AclLineMatchExpr> exprs) {
    Iterator<AclLineMatchExpr> iter = exprs.iterator();
    if (!iter.hasNext()) {
      // Empty. Return the identity element.
      return FalseExpr.INSTANCE;
    }
    AclLineMatchExpr first = iter.next();
    if (!iter.hasNext()) {
      // Only 1 element
      return first;
    }
    return new OrMatchExpr(exprs);
  }

  public static PermittedByAcl permittedByAcl(String aclName) {
    return new PermittedByAcl(aclName);
  }

  public static MatchHeaderSpace match5Tuple(
      Ip srcIp, int srcPort, Ip dstIp, int dstPort, IpProtocol ipProtocol) {
    return new MatchHeaderSpace(
        HeaderSpace.builder()
            .setSrcIps(srcIp.toIpSpace())
            .setSrcPorts(ImmutableList.of(SubRange.singleton(srcPort)))
            .setDstIps(dstIp.toIpSpace())
            .setDstPorts(ImmutableList.of(SubRange.singleton(dstPort)))
            .setIpProtocols(ImmutableList.of(ipProtocol))
            .build());
  }
}
