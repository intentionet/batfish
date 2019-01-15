package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;

/** Represents a {@code ScreenOption} checking no TCP flags */
public final class TCPNoFlag implements ScreenOption {

  /** */
  private static final long serialVersionUID = 1L;

  private static final String TCP_NO_FLAG = "tcp no flag";

  public static final TCPNoFlag INSTANCE = new TCPNoFlag();

  private TCPNoFlag() {}

  @Override
  public String getName() {
    return TCP_NO_FLAG;
  }

  @Override
  public AclLineMatchExpr toAclLineMatchExpr() {
    HeaderSpace headerSpace =
        HeaderSpace.builder()
            .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
            .setTcpFlags(
                ImmutableList.of(
                    TcpFlagsMatchConditions.builder()
                        .setTcpFlags(
                            TcpFlags.builder()
                                .setAck(false)
                                .setUrg(false)
                                .setPsh(false)
                                .setRst(false)
                                .setSyn(false)
                                .setFin(false)
                                .build())
                        .setUseAck(true)
                        .setUseUrg(true)
                        .setUsePsh(true)
                        .setUseRst(true)
                        .setUseSyn(true)
                        .setUseFin(true)
                        .build()))
            .build();
    return AclLineMatchExprs.match(headerSpace);
  }
}
