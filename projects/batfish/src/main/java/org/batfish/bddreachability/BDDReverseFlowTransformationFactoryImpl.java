package org.batfish.bddreachability;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.bddreachability.transition.TransformationToTransition;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.transformation.Transformation;

/**
 * Construct the transformations for return-flow sessions initialized by forward flows. At a high
 * level, the return-flow transformation is fairly simple: we run the forward transformation
 * backward except we transformation src fields instead of dest ones, and vice versa. The caveat is
 * that the transformations are not usually invertible. For instance a transformation might map all
 * source IPs to a single target source Ip. When we run that backward on the destination IP, we
 * first select packets for which the single target Ip is the destination Ip, then unset the
 * destination Ip (effectively allowing the packet to have any destination Ip). This is problematic
 * because it could (and almost always would) include Ips that could never be transformed in the
 * forward direction pass. To remedy this, clients must constrain the output to be within the input
 * of the forward transformation, using {@link BDDReverseTransformationRanges}.
 */
@ParametersAreNonnullByDefault
class BDDReverseFlowTransformationFactoryImpl implements BDDReverseFlowTransformationFactory {
  private final Map<String, Configuration> _configs;
  private final Map<String, TransformationToTransition> _transformationToTransitions;

  // caches
  private final Map<NodeInterfacePair, Transition> _reverseFlowIncomingTransformations;
  private final Map<NodeInterfacePair, Transition> _reverseFlowOutgoingTransformations;

  public BDDReverseFlowTransformationFactoryImpl(
      Map<String, Configuration> configs,
      Map<String, TransformationToTransition> transformationToTransitions) {
    _configs = configs;
    _transformationToTransitions = transformationToTransitions;
    _reverseFlowIncomingTransformations = new HashMap<>();
    _reverseFlowOutgoingTransformations = new HashMap<>();
  }

  @Override
  public @Nonnull Transition reverseFlowIncomingTransformation(String hostname, String iface) {
    return _reverseFlowIncomingTransformations.computeIfAbsent(
        NodeInterfacePair.of(hostname, iface),
        k ->
            computeReverseFlowTransformation(
                hostname,
                _configs.get(hostname).getAllInterfaces().get(iface).getIncomingTransformation()));
  }

  @Override
  public @Nonnull Transition reverseFlowOutgoingTransformation(String hostname, String iface) {
    return _reverseFlowOutgoingTransformations.computeIfAbsent(
        NodeInterfacePair.of(hostname, iface),
        k ->
            computeReverseFlowTransformation(
                hostname,
                _configs.get(hostname).getAllInterfaces().get(iface).getOutgoingTransformation()));
  }

  private Transition computeReverseFlowTransformation(
      String hostname, Transformation transformation) {
    return _transformationToTransitions.get(hostname).toReturnFlowTransition(transformation);
  }
}
