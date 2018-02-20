package org.batfish.z3;

import java.util.Map;
import javax.annotation.Nullable;
import org.batfish.common.BatfishLogger;
import org.batfish.common.BatfishLogger.BatfishLoggerHistory;
import org.batfish.datamodel.answers.NodFirstUnsatAnswerElement;
import org.batfish.job.BatfishJobResult;

public class NodFirstUnsatResult<KeyT, ResultT>
    extends BatfishJobResult<Map<KeyT, ResultT>, NodFirstUnsatAnswerElement> {

  private final Integer _firstUnsatQueryIndex;

  private final KeyT _key;

  private final ResultT _result;

  public NodFirstUnsatResult(
      KeyT key,
      @Nullable Integer firstUnsatQueryIndex,
      @Nullable ResultT result,
      BatfishLoggerHistory history,
      long startTime) {
    super(System.currentTimeMillis() - startTime, history);
    _firstUnsatQueryIndex = firstUnsatQueryIndex;
    _key = key;
    _result = result;
  }

  public NodFirstUnsatResult(long startTime, BatfishLoggerHistory history, Throwable failureCause) {
    super(System.currentTimeMillis() - startTime, history, failureCause);
    _key = null;
    _result = null;
    _firstUnsatQueryIndex = null;
  }

  @Override
  public void appendHistory(BatfishLogger logger) {
    logger.append(_history);
  }

  @Override
  public void applyTo(
      Map<KeyT, ResultT> output, BatfishLogger logger, NodFirstUnsatAnswerElement answerElement) {
    output.put(_key, _result);
  }

  @Override
  public String toString() {
    if (_key == null) {
      return "<FAILED>";
    } else if (_firstUnsatQueryIndex != null) {
      return "<FIRST_UNSAT: " + _firstUnsatQueryIndex + ":" + _result + ">";
    } else {
      return "<ALL_SAT>";
    }
  }
}
