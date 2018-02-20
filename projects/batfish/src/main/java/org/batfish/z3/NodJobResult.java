package org.batfish.z3;

import java.util.Collections;
import java.util.Set;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BatfishLogger.BatfishLoggerHistory;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.answers.NodAnswerElement;
import org.batfish.job.BatfishJobResult;

public class NodJobResult extends BatfishJobResult<Set<Flow>, NodAnswerElement> {

  /** Elapsed time in milliseconds */
  private Set<Flow> _flows;

  public NodJobResult(long startTime, BatfishLoggerHistory history) {
    super(System.currentTimeMillis() - startTime, history);
    _flows = Collections.<Flow>emptySet();
  }

  public NodJobResult(long startTime, BatfishLoggerHistory history, Set<Flow> flows) {
    super(System.currentTimeMillis() - startTime, history);
    _flows = flows;
  }

  public NodJobResult(long startTime, BatfishLoggerHistory history, Throwable failureCause) {
    super(System.currentTimeMillis() - startTime, history, failureCause);
  }

  @Override
  public void appendHistory(BatfishLogger logger) {
    logger.append(_history);
  }

  @Override
  public void applyTo(Set<Flow> flows, BatfishLogger logger, NodAnswerElement answerElement) {
    flows.addAll(_flows);
  }

  public Set<Flow> getFlows() {
    return _flows;
  }

  @Override
  public String toString() {
    int numFlows = _flows.size();
    String result = "" + numFlows + " flows";
    return result;
  }
}
