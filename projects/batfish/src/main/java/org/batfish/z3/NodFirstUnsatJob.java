package org.batfish.z3;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import org.batfish.common.BatfishException;
import org.batfish.config.Settings;

public class NodFirstUnsatJob<KeyT, ResultT>
    extends Z3ContextJob<NodFirstUnsatResult<KeyT, ResultT>> {

  private final FirstUnsatQuerySynthesizer<KeyT, ResultT> _query;

  private final Synthesizer _synthesizer;

  public NodFirstUnsatJob(
      Settings settings, Synthesizer synthesizer, FirstUnsatQuerySynthesizer<KeyT, ResultT> query) {
    super(settings);
    _synthesizer = synthesizer;
    _query = query;
  }

  @Override
  public NodFirstUnsatResult<KeyT, ResultT> call() {
    long startTime = System.currentTimeMillis();
    try (Context ctx = new Context()) {
      ReachabilityProgram baseProgram = _query.synthesizeBaseProgram(_synthesizer);
      ReachabilityProgram queryProgram = _query.getReachabilityProgram(_synthesizer.getInput());
      NodProgram program = new NodProgram(ctx, baseProgram, queryProgram);
      Fixedpoint fix = mkFixedpoint(program, false);
      KeyT key = _query.getKey();
      for (int queryNum = 0; queryNum < program.getQueries().size(); queryNum++) {
        BoolExpr query = program.getQueries().get(queryNum);
        Status status = fix.query(query);
        switch (status) {
          case SATISFIABLE:
            break;
          case UNKNOWN:
            return new NodFirstUnsatResult<>(
                startTime,
                _logger.getHistory(),
                new BatfishException("Query satisfiability unknown"));
          case UNSATISFIABLE:
            return new NodFirstUnsatResult<>(
                key,
                queryNum,
                _query.getResultsByQueryIndex().get(queryNum),
                _logger.getHistory(),
                startTime);
          default:
            return new NodFirstUnsatResult<>(
                startTime, _logger.getHistory(), new BatfishException("invalid status"));
        }
      }
      return new NodFirstUnsatResult<>(key, null, null, _logger.getHistory(), startTime);
    } catch (Z3Exception e) {
      return new NodFirstUnsatResult<>(
          startTime,
          _logger.getHistory(),
          new BatfishException("Error running NoD on concatenated data plane", e));
    }
  }
}
