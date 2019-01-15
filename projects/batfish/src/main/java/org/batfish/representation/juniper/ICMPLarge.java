package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;

/** Represents a {@code ScreenOption} checking large icmp packets */
public final class ICMPLarge implements ScreenOption {

  /** */
  private static final long serialVersionUID = 1L;

  private static final String ICMP_LARGE = "icmp large";
  private static final int LARGEST_ICMP_PACKET_LENGTH = 1024;

  public static final ICMPLarge INSTANCE = new ICMPLarge();

  private ICMPLarge() {}

  @Override
  public String getName() {
    return ICMP_LARGE;
  }

  @Override
  public AclLineMatchExpr toAclLineMatchExpr() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setIpProtocols(ImmutableList.of(IpProtocol.ICMP))
            .setNotPacketLengths(ImmutableList.of(new SubRange(0, LARGEST_ICMP_PACKET_LENGTH)))
            .build();
    return AclLineMatchExprs.match(headerSpace);
  }
}
