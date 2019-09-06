package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.datamodel.collections.NodeInterfacePair;

final class MockBDDReverseFlowTransformationFactory implements BDDReverseFlowTransformationFactory {
  private final Map<NodeInterfacePair, Transition> _reverseFlowIncomingTransformations;
  private final Map<NodeInterfacePair, Transition> _reverseFlowOutgoingTransformations;

  MockBDDReverseFlowTransformationFactory(
      Map<NodeInterfacePair, Transition> reverseFlowIncomingTransformations,
      Map<NodeInterfacePair, Transition> reverseFlowOutgoingTransformations) {
    _reverseFlowIncomingTransformations = ImmutableMap.copyOf(reverseFlowIncomingTransformations);
    _reverseFlowOutgoingTransformations = ImmutableMap.copyOf(reverseFlowOutgoingTransformations);
  }

  @Nonnull
  @Override
  public Transition reverseFlowIncomingTransformation(String hostname, String iface) {
    return checkNotNull(
        _reverseFlowIncomingTransformations.get(NodeInterfacePair.of(hostname, iface)),
        "Missing reverseFlowIncomingTransformations entry for %s:%s",
        hostname,
        iface);
  }

  @Nonnull
  @Override
  public Transition reverseFlowOutgoingTransformation(String hostname, String iface) {
    return checkNotNull(
        _reverseFlowOutgoingTransformations.get(NodeInterfacePair.of(hostname, iface)));
  }
}
