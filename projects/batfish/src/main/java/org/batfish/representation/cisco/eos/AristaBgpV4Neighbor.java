package org.batfish.representation.cisco.eos;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;

/** IPv4 BGP neighbor */
public final class AristaBgpV4Neighbor extends AristaBgpNeighbor implements AristaBgpHasPeerGroup {
  @Nonnull private final Ip _ip;
  @Nullable private String _peerGroup;

  public AristaBgpV4Neighbor(@Nonnull Ip ip) {
    super();
    _ip = ip;
  }

  @Nonnull
  public Ip getIp() {
    return _ip;
  }

  @Override
  @Nullable
  public String getPeerGroup() {
    return _peerGroup;
  }

  @Override
  public void setPeerGroup(@Nullable String peerGroup) {
    _peerGroup = peerGroup;
  }

  @Override
  public void inherit(Map<String, AristaBgpPeerGroupNeighbor> peerGroups) {
    // TODO: inheritance
  }
}
