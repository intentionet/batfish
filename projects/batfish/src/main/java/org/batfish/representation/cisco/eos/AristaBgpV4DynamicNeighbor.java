package org.batfish.representation.cisco.eos;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Prefix;

/** Config for dynamic BGP neighbors (created using "bgp listen range") */
public class AristaBgpV4DynamicNeighbor implements AristaBgpHasPeerGroup {
  @Nonnull private final Prefix _range;
  @Nullable private String _peerGroup;

  public AristaBgpV4DynamicNeighbor(@Nonnull Prefix range) {
    super();
    _range = range;
  }

  @Nonnull
  public Prefix getRange() {
    return _range;
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
