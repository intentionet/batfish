package org.batfish.representation.cisco.eos;

import javax.annotation.Nonnull;

public final class AristaBgpPeerGroupNeighbor extends AristaBgpNeighbor {
  @Nonnull private final String _name;

  public AristaBgpPeerGroupNeighbor(String name) {
    super();
    _name = name;
  }

  @Nonnull
  public String getName() {
    return _name;
  }
}
