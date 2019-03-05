package org.batfish.bddreachability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.collections.NodeInterfacePair;

interface BDDReverseTransformationRanges {
  /** Invariant: if lastHop is nonnull, then inIface is nonnull. */
  @Nonnull
  BDD reverseIncomingTransformationRange(
      String node, String iface, @Nullable String inIface, @Nullable NodeInterfacePair lastHop);

  /** Invariant: if lastHop is nonnull, then inIface is nonnull. */
  @Nonnull
  BDD reverseOutgoingTransformationRange(
      String node, String iface, @Nullable String inIface, @Nullable NodeInterfacePair lastHop);
}
