package org.batfish.bddreachability;

import static org.batfish.bddreachability.BDDReverseTransformationRangesImpl.TransformationType.INCOMING;
import static org.batfish.bddreachability.BDDReverseTransformationRangesImpl.TransformationType.OUTGOING;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.BDDReverseTransformationRangesImpl.Key;
import org.batfish.datamodel.collections.NodeInterfacePair;

final class MockBDDReverseTransformationRanges implements BDDReverseTransformationRanges {
  private final Map<Key, BDD> _ranges;
  private final BDD _zero;

  MockBDDReverseTransformationRanges(BDD zero, Map<Key, BDD> ranges) {
    _zero = zero;
    _ranges = ImmutableMap.copyOf(ranges);
  }

  @Override
  public @Nonnull BDD reverseIncomingTransformationRange(
      String node, String iface, @Nullable String inIface, @Nullable NodeInterfacePair lastHop) {
    return _ranges.getOrDefault(new Key(node, iface, INCOMING, inIface, lastHop), _zero);
  }

  @Override
  public @Nonnull BDD reverseOutgoingTransformationRange(
      String node, String iface, @Nullable String inIface, @Nullable NodeInterfacePair lastHop) {
    return _ranges.getOrDefault(new Key(node, iface, OUTGOING, inIface, lastHop), _zero);
  }
}
