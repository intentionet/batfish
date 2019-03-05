package org.batfish.question.bidirectionalreachability;

import static org.batfish.datamodel.FlowDisposition.SUCCESS_DISPOSITIONS;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.toHeaderSpaceBuilder;
import static org.batfish.question.specifiers.PathConstraintsUtil.createPathConstraints;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sf.javabdd.BDD;
import org.batfish.common.Answerer;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.TracePruner;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Flow.Builder;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.PathConstraints;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.specifiers.PathConstraintsInput;
import org.batfish.question.traceroute.BidirectionalTracerouteAnswerer;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationVisitor;
import org.batfish.specifier.parboiled.ParboiledIpSpaceSpecifierFactory;

/** Answerer for {@link BidirectionalReachabilityQuestion}. */
public final class BidirectionalReachabilityAnswerer extends Answerer {
  private final PathConstraintsInput _pathConstraintsInput;
  private final PacketHeaderConstraints _headerConstraints;
  private final ReturnFlowType _returnFlowType;
  private final Map<String, Configuration> _configs;

  BidirectionalReachabilityAnswerer(BidirectionalReachabilityQuestion question, IBatfish batfish) {
    super(question, batfish);
    _pathConstraintsInput = question.getPathConstraintsInput();
    _headerConstraints = question.getHeaderConstraints();
    _returnFlowType = question.getReturnFlowType();
    _configs = _batfish.loadConfigurations();
  }

  @Override
  public AnswerElement answer() {
    PathConstraints pathConstraints = createPathConstraints(_pathConstraintsInput);
    HeaderSpace headerSpace = toHeaderSpaceBuilder(_headerConstraints).build();

    ReachabilityParameters parameters =
        ReachabilityParameters.builder()
            .setActions(ImmutableSortedSet.copyOf(SUCCESS_DISPOSITIONS))
            .setDestinationIpSpaceSpecifier(
                getDestinationIpSpaceSpecifier(_headerConstraints.getDstIps()))
            .setForbiddenTransitNodesSpecifier(pathConstraints.getForbiddenLocations())
            .setHeaderSpace(headerSpace)
            .setIgnoreFilters(false)
            .setInvertSearch(false)
            .setRequiredTransitNodesSpecifier(pathConstraints.getTransitLocations())
            .setSourceLocationSpecifier(pathConstraints.getStartLocation())
            .setSourceIpSpaceSpecifier(getSourceIpSpaceSpecifier(_headerConstraints.getSrcIps()))
            .setSpecialize(false)
            .build();

    BDDPacket bddPacket = new BDDPacket();
    BidirectionalReachabilityResult result =
        _batfish.bidirectionalReachability(bddPacket, parameters);

    Map<Location, BDD> answerBdds = getAnswerBdds(result, _returnFlowType);

    String flowTag = _batfish.getFlowTag();

    Set<Flow> flows =
        answerBdds.entrySet().stream()
            .map(
                entry -> {
                  Location startLocation = entry.getKey();
                  BDD locationBdd = entry.getValue();
                  return bddPacket
                      .getFlow(locationBdd)
                      .map(FlowIngressLocationVisitor::new)
                      .map(startLocation::accept)
                      .map(builder -> builder.setTag(flowTag))
                      .map(Builder::build);
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

    return BidirectionalTracerouteAnswerer.bidirectionalTracerouteAnswerElement(
        _question, flows, _batfish.getTracerouteEngine(), false, TracePruner.DEFAULT_MAX_TRACES);
  }

  private static Map<Location, BDD> getAnswerBdds(
      BidirectionalReachabilityResult result, ReturnFlowType returnFlowType) {
    Map<Location, BDD> successBdds = result.getStartLocationReturnPassSuccessBdds();
    Map<Location, BDD> failureBdds = result.getStartLocationReturnPassFailureBdds();
    switch (returnFlowType) {
      case SUCCESS:
        // prefer success only
        return successBdds.entrySet().stream()
            .map(
                entry -> {
                  Location loc = entry.getKey();
                  BDD success = entry.getValue();
                  BDD fail = failureBdds.get(loc);
                  BDD successOnly = fail == null ? success : success.and(fail.not());
                  return Maps.immutableEntry(loc, successOnly.isZero() ? success : successOnly);
                })
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
      case FAILURE:
        // prefer failure only
        return failureBdds.entrySet().stream()
            .map(
                entry -> {
                  Location loc = entry.getKey();
                  BDD fail = entry.getValue();
                  BDD success = successBdds.get(loc);
                  BDD failOnly = success == null ? fail : fail.and(success.not());
                  return Maps.immutableEntry(loc, failOnly.isZero() ? fail : failOnly);
                })
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
      case MULTIPATH_INCONSISTENT:
        return successBdds.entrySet().stream()
            .flatMap(
                entry -> {
                  Location loc = entry.getKey();
                  BDD success = entry.getValue();
                  BDD fail = failureBdds.get(loc);
                  if (fail == null) {
                    return Stream.of();
                  }
                  BDD multipath = success.and(fail);
                  return multipath.isZero()
                      ? Stream.of()
                      : Stream.of(Maps.immutableEntry(loc, multipath));
                })
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
      default:
        throw new IllegalStateException("Unexpected ReturnFlowType: " + returnFlowType);
    }
  }

  private final class FlowIngressLocationVisitor implements LocationVisitor<Flow.Builder> {
    final Flow.Builder _builder;

    FlowIngressLocationVisitor(Builder builder) {
      _builder = builder;
    }

    @Override
    public Builder visitInterfaceLinkLocation(InterfaceLinkLocation loc) {
      return _builder.setIngressNode(loc.getNodeName()).setIngressInterface(loc.getInterfaceName());
    }

    @Override
    public Builder visitInterfaceLocation(InterfaceLocation loc) {
      return _builder
          .setIngressNode(loc.getNodeName())
          .setIngressVrf(
              _configs
                  .get(loc.getNodeName())
                  .getAllInterfaces()
                  .get(loc.getInterfaceName())
                  .getVrfName());
    }
  }

  private static IpSpaceSpecifier getDestinationIpSpaceSpecifier(String dstIps) {
    return dstIps == null
        ? new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE)
        : new ParboiledIpSpaceSpecifierFactory().buildIpSpaceSpecifier(dstIps);
  }

  private static IpSpaceSpecifier getSourceIpSpaceSpecifier(String srcIps) {
    return srcIps == null
        ? InferFromLocationIpSpaceSpecifier.INSTANCE
        : new ParboiledIpSpaceSpecifierFactory().buildIpSpaceSpecifier(srcIps);
  }
}
