package org.batfish.question.multipath;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_FLOW;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_TRACES;
import static org.batfish.question.traceroute.TracerouteAnswerer.createMetadata;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowHistory.FlowHistoryInfo;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.question.traceroute.TracerouteAnswerer;

public class MultipathConsistencyAnswerer extends Answerer {
  public MultipathConsistencyAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    Set<Flow> flows = _batfish.bddMultipathConsistency();
    if (_batfish.debugFlagEnabled("oldtraceroute")) {
      _batfish.processFlows(flows, false);
      FlowHistory flowHistory = _batfish.getHistory();
      Multiset<Row> rows = flowHistoryToRows(flowHistory);
      TableAnswerElement table = new TableAnswerElement(createMetadata(false));
      table.postProcessAnswer(_question, rows);
      return table;
    } else {
      SortedMap<Flow, List<Trace>> flowTraces = _batfish.buildFlows(flows, false);
      TableAnswerElement tableAnswer = new TableAnswerElement(TracerouteAnswerer.metadata());
      TracerouteAnswerer.flowTracesToRows(flowTraces, Integer.MAX_VALUE)
          .forEach(tableAnswer::addRow);
      return tableAnswer;
    }
  }

  /**
   * Converts {@code FlowHistoryInfo} into {@link Row}. Expects that the history object contains
   * traces for only one environment
   */
  static Row flowHistoryToRow(FlowHistoryInfo historyInfo) {
    // there should be only environment in this object
    checkArgument(
        historyInfo.getPaths().size() == 1,
        String.format(
            "Expect only one environment in flow history info. Found %d",
            historyInfo.getPaths().size()));
    Set<FlowTrace> paths =
        historyInfo.getPaths().values().stream().findAny().orElseGet(ImmutableSet::of);
    return Row.of(COL_FLOW, historyInfo.getFlow(), COL_TRACES, paths);
  }

  /** Converts a flowHistory object into a set of Rows. */
  public static Multiset<Row> flowHistoryToRows(FlowHistory flowHistory) {
    Multiset<Row> rows = LinkedHashMultiset.create();
    for (FlowHistoryInfo historyInfo : flowHistory.getTraces().values()) {
      rows.add(flowHistoryToRow(historyInfo));
    }
    return rows;
  }
}
