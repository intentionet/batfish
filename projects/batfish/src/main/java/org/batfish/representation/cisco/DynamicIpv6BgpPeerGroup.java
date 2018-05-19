package org.batfish.representation.cisco;

import javax.annotation.Nullable;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;

public class DynamicIpv6BgpPeerGroup extends LeafBgpPeerGroup {

  /** */
  private static final long serialVersionUID = 1L;

  private Prefix6 _prefix6;

  public DynamicIpv6BgpPeerGroup(Prefix6 prefix6) {
    _prefix6 = prefix6;
  }

  @Override
  public String getName() {
    return _prefix6.toString();
  }

  @Nullable
  @Override
  public Prefix getNeighborPrefix() {
    return null;
  }

  @Override
  public Prefix6 getNeighborPrefix6() {
    return _prefix6;
  }

  @Nullable
  public static Prefix getPrefix() {
    return null;
  }
}
