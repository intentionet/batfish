package org.batfish.representation.cisco;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.TcpFlags;

public class StandardAccessListLine implements Serializable {

  private static final long serialVersionUID = 1L;

  private final LineAction _action;

  private final Set<Integer> _dscps;

  private final Set<Integer> _ecns;

  private final IpWildcard _ipWildcard;

  private final String _name;

  public StandardAccessListLine(
      String name,
      LineAction action,
      IpWildcard ipWildcard,
      Set<Integer> dscps,
      Set<Integer> ecns) {
    _name = name;
    _action = action;
    _ipWildcard = ipWildcard;
    _dscps = dscps;
    _ecns = ecns;
  }

  public LineAction getAction() {
    return _action;
  }

  public IpWildcard getIpWildcard() {
    return _ipWildcard;
  }

  public String getName() {
    return _name;
  }

  public ExtendedAccessListLine toExtendedAccessListLine() {
    return ExtendedAccessListLine.builder()
        .setAction(_action)
        .setName(_name)
        .setServiceSpecifier(
            SimpleServiceSpecifier.builder()
                .setProtocol(IpProtocol.IP)
                .setSrcIpWildcard(_ipWildcard)
                .setDstIpWildcard(IpWildcard.ANY)
                .setDscps(_dscps)
                .setEcns(_ecns)
                .build())
        .build();
  }
}
