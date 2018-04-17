package org.batfish.datamodel.routing_policy.expr;

import javax.annotation.Nullable;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.Environment;

/** Implements BGP next-hop-self semantics */
public class SelfNextHop extends NextHopExpr {

  private static final long serialVersionUID = 1L;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    return getClass() == obj.getClass();
  }

  @Override
  @Nullable
  public Ip getNextHopIp(Environment environment) {
    Prefix peerPrefix = environment.getPeerPrefix();
    if (peerPrefix == null) {
      return null;
    }
    BgpNeighbor neighbor = environment.getVrf().getBgpProcess().getNeighbors().get(peerPrefix);
    if (neighbor == null) {
      return null;
    }
    return neighbor.getLocalIp();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + 0x12345678;
    return result;
  }
}
