package org.batfish.common.topology;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * Represents a global broadcast domain with no point-to-point links. This is how Batfish determines
 * L3 adjacency given no L1 information.
 */
@ParametersAreNonnullByDefault
public final class GlobalBroadcastNoPointToPoint implements L3Adjacencies {
  public static GlobalBroadcastNoPointToPoint instance() {
    return INSTANCE;
  }

  @Override
  public boolean inSameBroadcastDomain(NodeInterfacePair i1, NodeInterfacePair i2) {
    return true;
  }

  @Override
  public @Nonnull Optional<NodeInterfacePair> pairedPointToPointL3Interface(
      NodeInterfacePair iface) {
    return Optional.empty();
  }

  private GlobalBroadcastNoPointToPoint() {} // prevent instantiation

  private static final GlobalBroadcastNoPointToPoint INSTANCE = new GlobalBroadcastNoPointToPoint();
}
