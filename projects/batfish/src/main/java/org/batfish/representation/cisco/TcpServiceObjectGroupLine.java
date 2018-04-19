package org.batfish.representation.cisco;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nonnull;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;

public class TcpServiceObjectGroupLine implements ServiceObjectGroupLine {

  /** */
  private static final long serialVersionUID = 1L;

  private List<SubRange> _ports;

  public TcpServiceObjectGroupLine(@Nonnull List<SubRange> ports) {
    _ports = requireNonNull(ports);
  }

  @Override
  public AclLineMatchExpr toAclLineMatchExpr() {
    return new MatchHeaderSpace(
        HeaderSpace.builder()
            .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
            .setSrcOrDstPorts(_ports)
            .build());
  }
}
