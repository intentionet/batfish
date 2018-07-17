package org.batfish.question.multipath;

import static org.batfish.question.traceroute.TracerouteAnswerer.createMetadata;
import static org.batfish.question.traceroute.TracerouteAnswerer.flowHistoryToRows;

import com.google.common.collect.Multiset;
import java.util.Set;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;

public class MultipathConsistencyAnswerer extends Answerer {

  public MultipathConsistencyAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    Set<Flow> flows = _batfish.bddMultipathConsistency();
    _batfish.processFlows(flows, false);
    FlowHistory flowHistory = _batfish.getHistory();
    Multiset<Row> rows = flowHistoryToRows(flowHistory);
    TableAnswerElement table = new TableAnswerElement(createMetadata());
    table.postProcessAnswer(_question, rows);
    return table;
  }
}
