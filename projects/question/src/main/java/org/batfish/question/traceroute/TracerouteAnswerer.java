package org.batfish.question.traceroute;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.Answerer;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.TracePruner;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableDiff;
import org.batfish.datamodel.table.TableMetadata;

/** Produces the answer for {@link TracerouteQuestion} */
public final class TracerouteAnswerer extends Answerer {

  public static final String COL_FLOW = "Flow";
  public static final String COL_TRACES = "Traces";
  public static final String COL_TRACE_COUNT = "TraceCount";
  public static final String COL_BASE_TRACES = TableDiff.baseColumnName(COL_TRACES);
  public static final String COL_DELTA_TRACES = TableDiff.deltaColumnName(COL_TRACES);
  public static final String COL_BASE_TRACE_COUNT = TableDiff.baseColumnName(COL_TRACE_COUNT);
  public static final String COL_DELTA_TRACE_COUNT = TableDiff.deltaColumnName(COL_TRACE_COUNT);

  private final TracerouteAnswererHelper _helper;

  TracerouteAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
    TracerouteQuestion tracerouteQuestion = (TracerouteQuestion) question;
    _helper =
        new TracerouteAnswererHelper(
            tracerouteQuestion.getHeaderConstraints(),
            tracerouteQuestion.getSourceLocationStr(),
            _batfish.specifierContext());
  }

  @Override
  public AnswerElement answer(NetworkSnapshot snapshot) {
    TracerouteQuestion question = (TracerouteQuestion) _question;
    String tag = _batfish.getFlowTag();
    Set<Flow> flows = _helper.getFlows(tag);
    Multiset<Row> rows;
    SortedMap<Flow, List<Trace>> flowTraces =
        _batfish.getTracerouteEngine().computeTraces(flows, question.getIgnoreFilters());
    rows = flowTracesToRows(flowTraces, question.getMaxTraces());
    TableAnswerElement table = new TableAnswerElement(metadata(false));
    table.postProcessAnswer(_question, rows);
    return table;
  }

  @Override
  public AnswerElement answerDiff() {
    TracerouteQuestion question = ((TracerouteQuestion) _question);
    boolean ignoreFilters = question.getIgnoreFilters();
    Set<Flow> flows = _helper.getFlows(_batfish.getDifferentialFlowTag());
    Multiset<Row> rows;
    TableAnswerElement table;
    _batfish.pushBaseSnapshot();
    Map<Flow, List<Trace>> baseFlowTraces = _batfish.buildFlows(flows, ignoreFilters);
    _batfish.popSnapshot();

    _batfish.pushDeltaSnapshot();
    Map<Flow, List<Trace>> deltaFlowTraces = _batfish.buildFlows(flows, ignoreFilters);
    _batfish.popSnapshot();

    rows = diffFlowTracesToRows(baseFlowTraces, deltaFlowTraces, question.getMaxTraces());
    table = new TableAnswerElement(metadata(true));
    table.postProcessAnswer(_question, rows);
    return table;
  }

  /** Create metadata for the new traceroute v2 answer */
  public static TableMetadata metadata(boolean differential) {
    List<ColumnMetadata> columnMetadata;
    if (differential) {
      columnMetadata =
          ImmutableList.of(
              new ColumnMetadata(COL_FLOW, Schema.FLOW, "The flow", true, false),
              new ColumnMetadata(
                  TableDiff.baseColumnName(COL_TRACES),
                  Schema.set(Schema.TRACE),
                  "The traces in the BASE snapshot",
                  false,
                  true),
              new ColumnMetadata(
                  TableDiff.baseColumnName(COL_TRACE_COUNT),
                  Schema.INTEGER,
                  "The total number traces in the BASE snapshot",
                  false,
                  true),
              new ColumnMetadata(
                  TableDiff.deltaColumnName(COL_TRACES),
                  Schema.set(Schema.TRACE),
                  "The traces in the DELTA snapshot",
                  false,
                  true),
              new ColumnMetadata(
                  TableDiff.deltaColumnName(COL_TRACE_COUNT),
                  Schema.INTEGER,
                  "The total number traces in the DELTA snapshot",
                  false,
                  true));
    } else {
      columnMetadata =
          ImmutableList.of(
              new ColumnMetadata(COL_FLOW, Schema.FLOW, "The flow", true, false),
              new ColumnMetadata(
                  COL_TRACES, Schema.set(Schema.TRACE), "The traces for this flow", false, true),
              new ColumnMetadata(
                  COL_TRACE_COUNT,
                  Schema.INTEGER,
                  "The total number traces for this flow",
                  false,
                  true));
    }
    return new TableMetadata(columnMetadata, String.format("Paths for flow ${%s}", COL_FLOW));
  }

  public static Multiset<Row> flowTracesToRows(
      SortedMap<Flow, List<Trace>> flowTraces, int maxTraces) {
    Multiset<Row> rows = LinkedHashMultiset.create();
    for (Map.Entry<Flow, List<Trace>> flowTrace : flowTraces.entrySet()) {
      List<Trace> traces = flowTrace.getValue();
      List<Trace> prunedTraces = TracePruner.prune(traces, maxTraces);
      rows.add(
          Row.of(
              COL_FLOW,
              flowTrace.getKey(),
              COL_TRACES,
              prunedTraces,
              COL_TRACE_COUNT,
              traces.size()));
    }
    return rows;
  }

  public static Multiset<Row> diffFlowTracesToRows(
      Map<Flow, List<Trace>> baseFlowTraces,
      Map<Flow, List<Trace>> deltaFlowTraces,
      int maxTraces) {
    Multiset<Row> rows = LinkedHashMultiset.create();
    checkArgument(
        baseFlowTraces.keySet().equals(deltaFlowTraces.keySet()),
        "Base and delta flow traces should have same flows");
    for (Flow flow : baseFlowTraces.keySet()) {
      rows.add(
          Row.of(
              COL_FLOW,
              flow,
              TableDiff.baseColumnName(COL_TRACES),
              TracePruner.prune(baseFlowTraces.get(flow), maxTraces),
              TableDiff.baseColumnName(COL_TRACE_COUNT),
              baseFlowTraces.get(flow).size(),
              TableDiff.deltaColumnName(COL_TRACES),
              TracePruner.prune(deltaFlowTraces.get(flow), maxTraces),
              TableDiff.deltaColumnName(COL_TRACE_COUNT),
              deltaFlowTraces.get(flow).size()));
    }
    return rows;
  }
}
