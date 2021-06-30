package org.batfish.common.topology;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** Something that can tell whether two different L3 interfaces are in the same broadcast domain. */
@ParametersAreNonnullByDefault
public interface L3Adjacencies {
  /**
   * Return whether the two interfaces are in the same broadcast domain.
   *
   * <p>The assumption is that both interfaces are active, L3 interfaces.
   */
  boolean inSameBroadcastDomain(NodeInterfacePair i1, NodeInterfacePair i2);

  /**
   * Return whether the two interfaces are in the same broadcast domain, and they are physically on
   * a point-to-point link.
   *
   * <p>The assumption is that both interfaces are active, L3 interfaces.
   */
  default boolean inSamePointToPointDomain(NodeInterfacePair i1, NodeInterfacePair i2) {
    return pairedPointToPointL3Interface(i1).map(i -> i.equals(i2)).orElse(false);
  }

  /**
   * Return the interface on the point-to-point interface that is in the same broadcast domain, if
   * any.
   *
   * <p>The assumption is that {@code iface} is an active, L3 interface.
   */
  @Nonnull
  Optional<NodeInterfacePair> pairedPointToPointL3Interface(NodeInterfacePair iface);
}
