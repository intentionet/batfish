package org.batfish.question.differentialreachability;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.question.specifiers.PathConstraintsUtil.createPathConstraints;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.util.Set;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowHistory.FlowHistoryInfo;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.PacketHeaderConstraintsUtil;
import org.batfish.datamodel.PathConstraints;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableDiff;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.question.ReachabilityParameters;
import org.batfish.specifier.FlexibleInferFromLocationIpSpaceSpecifierFactory;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.IpSpaceSpecifierFactory;
import org.batfish.specifier.Location;
import org.batfish.specifier.SpecifierContext;

/** An {@link Answerer} for {@link DifferentialReachabilityQuestion}. */
public class DifferentialReachabilityAnswerer extends Answerer {
  public static final String COL_FLOW = "flow";

  static final String COL_BASE_TRACES = TableDiff.baseColumnName(getTracesColumnName());

  static final String COL_DELTA_TRACES = TableDiff.deltaColumnName(getTracesColumnName());

  public DifferentialReachabilityAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  private static String getTracesColumnName() {
    return "traces";
  }

  @Override
  public TableAnswerElement answer() {
    return answerDiff();
  }

  private DifferentialReachabilityParameters parameters() {
    DifferentialReachabilityQuestion question = (DifferentialReachabilityQuestion) _question;
    PacketHeaderConstraints headerConstraints = question.getHeaderConstraints();
    IpSpaceSpecifierFactory flexibleIpSpaceSpecifierFactory =
        new FlexibleInferFromLocationIpSpaceSpecifierFactory();
    SpecifierContext ctxt = _batfish.specifierContext();

    PathConstraints pathConstraints = createPathConstraints(question.getPathConstraints());
    Set<String> forbiddenTransitNodes = pathConstraints.getForbiddenLocations().resolve(ctxt);
    Set<String> requiredTransitNodes = pathConstraints.getTransitLocations().resolve(ctxt);
    Set<Location> startLocations = pathConstraints.getStartLocation().resolve(ctxt);
    Set<String> finalNodes = pathConstraints.getEndLocation().resolve(ctxt);

    IpSpaceAssignment ipSpaceAssignment =
        flexibleIpSpaceSpecifierFactory
            .buildIpSpaceSpecifier(headerConstraints.getSrcIps())
            .resolve(startLocations, ctxt);
    IpSpace dstIps =
        firstNonNull(
            AclIpSpace.union(
                flexibleIpSpaceSpecifierFactory
                    .buildIpSpaceSpecifier(headerConstraints.getDstIps())
                    .resolve(ImmutableSet.of(), ctxt)
                    .getEntries()
                    .stream()
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
        question.getIgnoreAcls(),
        ipSpaceAssignment,
        requiredTransitNodes);
  }

  @Override
  public TableAnswerElement answerDiff() {
    DifferentialReachabilityParameters parameters = parameters();
    DifferentialReachabilityResult result = _batfish.bddReducedReachability(parameters);

    Set<Flow> flows =
        Sets.union(result.getDecreasedReachabilityFlows(), result.getIncreasedReachabilityFlows());

    _batfish.pushBaseSnapshot();
    _batfish.processFlows(flows, parameters.getIgnoreAcls());
    _batfish.popSnapshot();
    _batfish.pushDeltaSnapshot();
    _batfish.processFlows(flows, parameters.getIgnoreAcls());
    _batfish.popSnapshot();

    FlowHistory flowHistory = _batfish.getHistory();
    Multiset<Row> rows = flowHistoryToRows(flowHistory);
    TableAnswerElement table = new TableAnswerElement(createMetadata());
    table.postProcessAnswer(_question, rows);
    return table;
  }

  private static TableMetadata createMetadata() {
    ImmutableList<ColumnMetadata> columnMetadata =
        ImmutableList.of(
            new ColumnMetadata(COL_FLOW, Schema.FLOW, "The flow", true, true),
            new ColumnMetadata(
                COL_BASE_TRACES,
                Schema.set(Schema.FLOW_TRACE),
                "The flow traces in the BASE environment",
                false,
                true),
            new ColumnMetadata(
                COL_DELTA_TRACES,
                Schema.set(Schema.FLOW_TRACE),
                "The flow traces in the DELTA environment",
                false,
                true));
    return new TableMetadata(columnMetadata, "Flows with reduced reachability");
  }

  /**
   * Converts a flowHistory object into a set of Rows. Expects that the traces correspond to only
   * one environment.
   */
  private Multiset<Row> flowHistoryToRows(FlowHistory flowHistory) {
    Multiset<Row> rows = LinkedHashMultiset.create();
    for (FlowHistoryInfo historyInfo : flowHistory.getTraces().values()) {
      rows.add(
          Row.of(
              COL_FLOW,
              historyInfo.getFlow(),
              COL_BASE_TRACES,
              historyInfo.getPaths().get(Flow.BASE_FLOW_TAG),
              COL_DELTA_TRACES,
              historyInfo.getPaths().get(Flow.DELTA_FLOW_TAG)));
    }
    return rows;
  }
}
