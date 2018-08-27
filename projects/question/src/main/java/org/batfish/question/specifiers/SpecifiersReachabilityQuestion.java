package org.batfish.question.specifiers;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Protocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.ReachabilityParameters;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.FlexibleInferFromLocationIpSpaceSpecifierFactory;
import org.batfish.specifier.FlexibleLocationSpecifierFactory;
import org.batfish.specifier.FlexibleUniverseIpSpaceSpecifierFactory;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.IpSpaceSpecifierFactory;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.LocationSpecifierFactory;
import org.batfish.specifier.NameRegexNodeSpecifierFactory;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.NodeSpecifierFactory;

/**
 * A version of reachability question that supports {@link LocationSpecifier location} and {@link
 * IpSpaceSpecifier ipSpace} specifiers.
 */
public final class SpecifiersReachabilityQuestion extends Question {
  private static final String DEFAULT_DESTINATION_IP_SPACE_SPECIFIER_FACTORY =
      FlexibleUniverseIpSpaceSpecifierFactory.NAME;

  private static final String DEFAULT_SOURCE_IP_SPACE_SPECIFIER_FACTORY =
      FlexibleInferFromLocationIpSpaceSpecifierFactory.NAME;

  private static final String DEFAULT_SOURCE_LOCATION_SPECIFIER_FACTORY =
      FlexibleLocationSpecifierFactory.NAME;

  private static final String PROP_ACTIONS = "actions";

  private static final String PROP_DESTINATION_IP_SPACE_SPECIFIER_FACTORY =
      "destinationIpSpaceSpecifierFactory";

  private static final String PROP_DESTINATION_IP_SPACE_SPECIFIER_INPUT =
      "destinationIpSpaceSpecifierInput";

  private static final String PROP_DST_PORTS = "dstPorts";

  private static final String PROP_DST_PROTOCOLS = "dstProtocols";

  private static final String PROP_FINAL_NODES_SPECIFIER_FACTORY = "finalNodesSpecifierFactory";

  private static final String PROP_FINAL_NODES_SPECIFIER_INPUT = "finalNodesSpecifierInput";

  private static final String PROP_FORBIDDEN_TRANSIT_NODES_NODE_SPECIFIER_INPUT =
      "forbiddenTransitNodesNodeSpecifierFactory";

  private static final String PROP_FORBIDDEN_TRANSIT_NODES_NODE_SPECIFIER_FACTORY =
      "forbiddenTransitNodesNodeSpecifierInput";

  private static final String PROP_SOURCE_IP_SPACE_SPECIFIER_FACTORY =
      "sourceIpSpaceSpecifierFactory";

  private static final String PROP_SOURCE_IP_SPACE_SPECIFIER_INPUT = "sourceIpSpaceSpecifierInput";

  private static final String PROP_SOURCE_LOCATION_SPECIFIER_FACTORY =
      "sourceLocationSpecifierFactory";

  private static final String PROP_SOURCE_LOCATION_SPECIFIER_INPUT = "sourceLocationSpecifierInput";

  private static final String PROP_REQUIRED_TRANSIT_NODES_SPECIFIER_FACTORY =
      "requiredTransitNodesNodeSpecifierFactory";

  private static final String PROP_REQUIRED_TRANSIT_NODES_SPECIFIER_INPUT =
      "requiredTransitNodesNodeSpecifierInput";

  private static @Nullable NodeSpecifier getNodeSpecifier(
      @Nullable String factoryName, @Nullable String input, @Nullable NodeSpecifier def) {
    if (factoryName == null && input == null) {
      return def;
    }
    NodeSpecifierFactory factory =
        (factoryName == null)
            ? new NameRegexNodeSpecifierFactory()
            : NodeSpecifierFactory.load(factoryName);
    return factory.buildNodeSpecifier(input);
  }

  private SortedSet<ForwardingAction> _actions;

  private String _destinationIpSpaceSpecifierFactory;

  private String _destinationIpSpaceSpecifierInput;

  private SortedSet<SubRange> _dstPorts;

  private SortedSet<Protocol> _dstProtocols;

  private String _finalNodesNodeSpecifierFactory;

  private String _finalNodesNodeSpecifierInput;

  private String _forbiddenTransitNodesNodeSpecifierFactory;

  private String _forbiddenTransitNodesNodeSpecifierInput;

  private String _sourceIpSpaceSpecifierFactory;

  private String _sourceIpSpaceSpecifierInput;

  private String _sourceLocationSpecifierFactory;

  private String _sourceLocationSpecifierInput;

  private String _requiredTransitNodesNodeSpecifierFactory;

  private String _requiredTransitNodesNodeSpecifierInput;

  public SpecifiersReachabilityQuestion() {}

  @JsonProperty(PROP_ACTIONS)
  public SortedSet<ForwardingAction> getActions() {
    return firstNonNull(_actions, ImmutableSortedSet.of(ForwardingAction.ACCEPT));
  }

  IpSpaceSpecifier getDestinationIpSpaceSpecifier() {
    return IpSpaceSpecifierFactory.load(
            firstNonNull(
                _destinationIpSpaceSpecifierFactory,
                DEFAULT_DESTINATION_IP_SPACE_SPECIFIER_FACTORY))
        .buildIpSpaceSpecifier(_destinationIpSpaceSpecifierInput);
  }

  @JsonProperty(PROP_DESTINATION_IP_SPACE_SPECIFIER_FACTORY)
  public @Nullable String getDestinationIpSpaceSpecifierFactory() {
    return _destinationIpSpaceSpecifierFactory;
  }

  @JsonProperty(PROP_DESTINATION_IP_SPACE_SPECIFIER_INPUT)
  public @Nullable String getDestinationIpSpaceSpecifierInput() {
    return _destinationIpSpaceSpecifierInput;
  }

  @Override
  public boolean getDataPlane() {
    return true;
  }

  @JsonProperty(PROP_DST_PORTS)
  public SortedSet<SubRange> getDstPorts() {
    return _dstPorts;
  }

  @JsonProperty(PROP_DST_PROTOCOLS)
  public SortedSet<Protocol> getDstProtocols() {
    return _dstProtocols;
  }

  NodeSpecifier getFinalNodesSpecifier() {
    return getNodeSpecifier(
        _finalNodesNodeSpecifierFactory,
        _finalNodesNodeSpecifierInput,
        AllNodesNodeSpecifier.INSTANCE);
  }

  @JsonProperty(PROP_FINAL_NODES_SPECIFIER_FACTORY)
  public String getFinalNodesSpecifierFactory() {
    return _finalNodesNodeSpecifierFactory;
  }

  @JsonProperty(PROP_FINAL_NODES_SPECIFIER_INPUT)
  public String getFinalNodesSpecifierInput() {
    return _finalNodesNodeSpecifierInput;
  }

  NodeSpecifier getForbiddenTransitNodesSpecifier() {
    return getNodeSpecifier(
        _forbiddenTransitNodesNodeSpecifierFactory, _forbiddenTransitNodesNodeSpecifierInput, null);
  }

  @VisibleForTesting
  HeaderSpace getHeaderSpace() {
    return HeaderSpace.builder()
        .setDstPorts(firstNonNull(_dstPorts, ImmutableList.of()))
        .setDstProtocols(firstNonNull(_dstProtocols, ImmutableList.of()))
        .build();
  }

  @Override
  public String getName() {
    return "specifiersreachability";
  }

  ReachabilityParameters getReachabilityParameters() {
    return ReachabilityParameters.builder()
        .setActions(getActions())
        .setDestinationIpSpaceSpecifier(getDestinationIpSpaceSpecifier())
        .setFinalNodesSpecifier(getFinalNodesSpecifier())
        .setForbiddenTransitNodesSpecifier(getForbiddenTransitNodesSpecifier())
        .setHeaderSpace(getHeaderSpace())
        .setRequiredTransitNodesSpecifier(getRequiredTransitNodesSpecifier())
        .setSourceLocationSpecifier(getSourceLocationSpecifier())
        .setSourceIpSpaceSpecifier(getSourceIpSpaceSpecifier())
        .setSpecialize(true)
        .build();
  }

  NodeSpecifier getRequiredTransitNodesSpecifier() {
    return getNodeSpecifier(
        _requiredTransitNodesNodeSpecifierFactory, _requiredTransitNodesNodeSpecifierInput, null);
  }

  IpSpaceSpecifier getSourceIpSpaceSpecifier() {
    return IpSpaceSpecifierFactory.load(
            firstNonNull(_sourceIpSpaceSpecifierFactory, DEFAULT_SOURCE_IP_SPACE_SPECIFIER_FACTORY))
        .buildIpSpaceSpecifier(_sourceIpSpaceSpecifierInput);
  }

  @JsonProperty(PROP_SOURCE_IP_SPACE_SPECIFIER_FACTORY)
  public String getSourceIpSpaceSpecifierFactory() {
    return _sourceIpSpaceSpecifierFactory;
  }

  @JsonProperty(PROP_SOURCE_IP_SPACE_SPECIFIER_INPUT)
  public String getSourceIpSpaceSpecifierInput() {
    return _sourceIpSpaceSpecifierInput;
  }

  @VisibleForTesting
  LocationSpecifier getSourceLocationSpecifier() {
    return LocationSpecifierFactory.load(
            firstNonNull(
                _sourceLocationSpecifierFactory, DEFAULT_SOURCE_LOCATION_SPECIFIER_FACTORY))
        .buildLocationSpecifier(_sourceLocationSpecifierInput);
  }

  @JsonProperty(PROP_SOURCE_LOCATION_SPECIFIER_FACTORY)
  public String getSourceLocationSpecifierFactory() {
    return _sourceLocationSpecifierFactory;
  }

  @JsonProperty(PROP_SOURCE_LOCATION_SPECIFIER_INPUT)
  public String getSourceLocationSpecifierInput() {
    return _sourceLocationSpecifierInput;
  }

  @JsonProperty(PROP_REQUIRED_TRANSIT_NODES_SPECIFIER_FACTORY)
  public String getTransitNodesSpecifierFactory() {
    return _requiredTransitNodesNodeSpecifierFactory;
  }

  @JsonProperty(PROP_REQUIRED_TRANSIT_NODES_SPECIFIER_INPUT)
  public String getTransitNodesSpecifierInput() {
    return _requiredTransitNodesNodeSpecifierInput;
  }

  @JsonProperty(PROP_ACTIONS)
  public void setActions(Iterable<ForwardingAction> actionSet) {
    _actions = ImmutableSortedSet.copyOf(actionSet);
  }

  @JsonProperty(PROP_DESTINATION_IP_SPACE_SPECIFIER_FACTORY)
  public void setDestinationIpSpaceSpecifierFactory(String ipSpaceSpecifierFactory) {
    _destinationIpSpaceSpecifierFactory = ipSpaceSpecifierFactory;
  }

  @JsonProperty(PROP_DESTINATION_IP_SPACE_SPECIFIER_INPUT)
  public void setDestinationIpSpaceSpecifierInput(String ipSpaceSpecifierInput) {
    _destinationIpSpaceSpecifierInput = ipSpaceSpecifierInput;
  }

  @JsonProperty(PROP_DST_PORTS)
  public void setDstPorts(SortedSet<SubRange> dstPorts) {
    _dstPorts = ImmutableSortedSet.copyOf(dstPorts);
  }

  @JsonProperty(PROP_DST_PROTOCOLS)
  public void setDstProtocols(SortedSet<Protocol> dstProtocols) {
    _dstProtocols = ImmutableSortedSet.copyOf(dstProtocols);
  }

  @JsonProperty(PROP_FINAL_NODES_SPECIFIER_FACTORY)
  public void setFinalNodesSpecifierFactory(String finalNodesNodeSpecifierFactory) {
    _finalNodesNodeSpecifierFactory = finalNodesNodeSpecifierFactory;
  }

  @JsonProperty(PROP_FINAL_NODES_SPECIFIER_INPUT)
  public void setFinalNodesSpecifierInput(String finalNodesNodeSpecifierInput) {
    _finalNodesNodeSpecifierInput = finalNodesNodeSpecifierInput;
  }

  @JsonProperty(PROP_FORBIDDEN_TRANSIT_NODES_NODE_SPECIFIER_FACTORY)
  public void setForbiddenTransitNodesNodeSpecifierFactory(
      String forbiddenTransitNodesNodeSpecifierFactory) {
    _forbiddenTransitNodesNodeSpecifierFactory = forbiddenTransitNodesNodeSpecifierFactory;
  }

  @JsonProperty(PROP_FORBIDDEN_TRANSIT_NODES_NODE_SPECIFIER_INPUT)
  public void setForbiddenTransitNodesNodeSpecifierInput(
      String forbiddenTransitNodesNodeSpecifierInput) {
    _forbiddenTransitNodesNodeSpecifierInput = forbiddenTransitNodesNodeSpecifierInput;
  }

  @JsonProperty(PROP_SOURCE_IP_SPACE_SPECIFIER_FACTORY)
  public void setSourceIpSpaceSpecifierFactory(String ipSpaceSpecifierFactory) {
    _sourceIpSpaceSpecifierFactory = ipSpaceSpecifierFactory;
  }

  @JsonProperty(PROP_SOURCE_IP_SPACE_SPECIFIER_INPUT)
  public void setSourceIpSpaceSpecifierInput(String ipSpaceSpecifierInput) {
    _sourceIpSpaceSpecifierInput = ipSpaceSpecifierInput;
  }

  @JsonProperty(PROP_SOURCE_LOCATION_SPECIFIER_FACTORY)
  public void setSourceLocationSpecifierFactory(String locationSpecifierFactory) {
    _sourceLocationSpecifierFactory = locationSpecifierFactory;
  }

  @JsonProperty(PROP_SOURCE_LOCATION_SPECIFIER_INPUT)
  public void setSourceLocationSpecifierInput(String locationSpecifierInput) {
    _sourceLocationSpecifierInput = locationSpecifierInput;
  }

  @JsonProperty(PROP_REQUIRED_TRANSIT_NODES_SPECIFIER_FACTORY)
  public void setRequiredTransitNodesNodeSpecifierFactory(
      String requiredTransitNodesNodeSpecifierFactory) {
    _requiredTransitNodesNodeSpecifierFactory = requiredTransitNodesNodeSpecifierFactory;
  }

  @JsonProperty(PROP_REQUIRED_TRANSIT_NODES_SPECIFIER_INPUT)
  public void setRequiredTransitNodesNodeSpecifierInput(String transitNodesNodeSpecifierInput) {
    _requiredTransitNodesNodeSpecifierInput = transitNodesNodeSpecifierInput;
  }
}
