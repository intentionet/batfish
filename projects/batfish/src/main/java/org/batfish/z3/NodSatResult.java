package org.batfish.z3;

import java.util.Map;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BatfishLogger.BatfishLoggerHistory;
import org.batfish.datamodel.answers.NodSatAnswerElement;
import org.batfish.job.BatfishJobResult;

public class NodSatResult<KeyT> extends BatfishJobResult<Map<KeyT, Boolean>, NodSatAnswerElement> {

  private final Map<KeyT, Boolean> _results;

  public NodSatResult(long startTime, BatfishLoggerHistory history, Throwable failureCause) {
    super(System.currentTimeMillis() - startTime, history, failureCause);
    _results = null;
  }

  public NodSatResult(Map<KeyT, Boolean> results, BatfishLoggerHistory history, long startTime) {
    super(System.currentTimeMillis() - startTime, history);
    _results = results;
  }

  @Override
  public void appendHistory(BatfishLogger logger) {
    logger.append(_history);
  }

  @Override
  public void applyTo(
      Map<KeyT, Boolean> output, BatfishLogger logger, NodSatAnswerElement answerElement) {
    output.putAll(_results);
  }

  @Override
  public String toString() {
    if (_results == null) {
      return "<FAILED>";
    } else {
      int numSat = 0;
      int numUnsat = 0;
      for (Boolean result : _results.values()) {
        if (result) {
          numSat++;
        } else {
          numUnsat++;
        }
      }
      return "<UNSAT: " + numUnsat + ", SAT: " + numSat + ">";
    }
  }
}
