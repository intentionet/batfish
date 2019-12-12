package org.batfish.question.differentialreachability;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.question.specifiers.PathConstraintsUtil.createPathConstraints;
import static org.batfish.question.traceroute.TracerouteAnswerer.diffFlowTracesToRows;
import static org.batfish.question.traceroute.TracerouteAnswerer.metadata;
import static org.batfish.specifier.SpecifierUtils.resolveActiveLocations;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.PacketHeaderConstraintsUtil;
import org.batfish.datamodel.PathConstraints;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.question.ReachabilityParameters;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.Location;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierFactories;

/** An {@link Answerer} for {@link DifferentialReachabilityQuestion}. */
public class DifferentialReachabilityAnswerer extends Answerer {
  public static final String COL_FLOW = "flow";

  public DifferentialReachabilityAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public TableAnswerElement answer(NetworkSnapshot snapshot) {
    throw new IllegalStateException(
        getClass().getSimpleName() + " can only be run in differential mode");
  }

  @VisibleForTesting
  DifferentialReachabilityParameters parameters(
      NetworkSnapshot snapshot, NetworkSnapshot reference) {
    DifferentialReachabilityQuestion question = (DifferentialReachabilityQuestion) _question;
    PacketHeaderConstraints headerConstraints = question.getHeaderConstraints();
    SpecifierContext snapshotCtxt = _batfish.specifierContext(snapshot);
    SpecifierContext referenceCtxt = _batfish.specifierContext(reference);

    PathConstraints pathConstraints = createPathConstraints(question.getPathConstraints());

    // forbiddenTransitNodes can be different in each snapshot. flow must not transit any
    Set<String> forbiddenTransitNodes =
        Sets.union(
            pathConstraints.getForbiddenLocations().resolve(snapshotCtxt),
            pathConstraints.getForbiddenLocations().resolve(referenceCtxt));

    // requiredTransitNodes can be different in each snapshot. flow must transit any one
    Set<String> requiredTransitNodes =
        Sets.union(
            pathConstraints.getTransitLocations().resolve(snapshotCtxt),
            pathConstraints.getTransitLocations().resolve(referenceCtxt));

    // only consider startLocations that are present+active in both snapshots
    Set<Location> startLocations =
        Sets.intersection(
            resolveActiveLocations(pathConstraints.getStartLocation(), snapshotCtxt),
            resolveActiveLocations(pathConstraints.getStartLocation(), referenceCtxt));
    if (startLocations.isEmpty()) {
      throw new BatfishException(
          "no matching startLocation is present and active in both snapshots");
    }

    // finalNodes can be different in each snapshot
    Set<String> finalNodes =
        Sets.union(
            pathConstraints.getEndLocation().resolve(snapshotCtxt),
            pathConstraints.getEndLocation().resolve(referenceCtxt));

    // TODO generate better IpSpaceAssignments for differential context
    IpSpaceAssignment ipSpaceAssignment =
        SpecifierFactories.getIpSpaceSpecifierOrDefault(
                headerConstraints.getSrcIps(), InferFromLocationIpSpaceSpecifier.INSTANCE)
            .resolve(startLocations, snapshotCtxt);
    IpSpace dstIps =
        firstNonNull(
            AclIpSpace.union(
                SpecifierFactories.getIpSpaceSpecifierOrDefault(
                        headerConstraints.getDstIps(), InferFromLocationIpSpaceSpecifier.INSTANCE)
                    .resolve(ImmutableSet.of(), snapshotCtxt).getEntries().stream()
                    .map(Entry::getIpSpace)
                    .collect(ImmutableList.toImmutableList())),
            UniverseIpSpace.INSTANCE);
    AclLineMatchExpr headerSpace =
        match(
            PacketHeaderConstraintsUtil.toHeaderSpaceBuilder(headerConstraints)
                .setDstIps(dstIps)
                .build());

    return new DifferentialReachabilityParameters(
        ReachabilityParameters.filterDispositions(question.getActions().getDispositions()),
        forbiddenTransitNodes,
        finalNodes,
        headerSpace,
        question.getIgnoreFilters(),
        question.getInvertSearch(),
        ipSpaceAssignment,
        question.getMaxTraces(),
        requiredTransitNodes);
  }

  @Override
  public TableAnswerElement answerDiff(NetworkSnapshot snapshot, NetworkSnapshot reference) {
    DifferentialReachabilityParameters parameters = parameters(snapshot, reference);
    DifferentialReachabilityResult result =
        _batfish.bddDifferentialReachability(snapshot, reference, parameters);

    Set<Flow> flows =
        Sets.union(result.getDecreasedReachabilityFlows(), result.getIncreasedReachabilityFlows());
    Multiset<Row> rows;
    TableAnswerElement table;
    Map<Flow, List<Trace>> baseFlowTraces =
        _batfish.buildFlows(snapshot, flows, parameters.getIgnoreFilters());

    Map<Flow, List<Trace>> deltaFlowTraces =
        _batfish.buildFlows(reference, flows, parameters.getIgnoreFilters());

    rows = diffFlowTracesToRows(baseFlowTraces, deltaFlowTraces, parameters.getMaxTraces());
    table = new TableAnswerElement(metadata(true));
    table.postProcessAnswer(_question, rows);
    return table;
  }
}
