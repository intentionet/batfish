package org.batfish.z3;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import java.util.LinkedHashMap;
import java.util.Map;
import org.batfish.common.BatfishException;
import org.batfish.config.Settings;

public class NodSatJob<KeyT> extends Z3ContextJob<NodSatResult<KeyT>> {

  private final SatQuerySynthesizer<KeyT> _query;

  private final Synthesizer _synthesizer;

  public NodSatJob(Settings settings, Synthesizer synthesizer, SatQuerySynthesizer<KeyT> query) {
    super(settings);
    _synthesizer = synthesizer;
    _query = query;
  }

  @Override
  public NodSatResult<KeyT> call() {
    Map<KeyT, Boolean> results = new LinkedHashMap<>();
    long startTime = System.currentTimeMillis();
    try (Context ctx = new Context()) {
      ReachabilityProgram baseProgram = _query.synthesizeBaseProgram(_synthesizer);
      ReachabilityProgram queryProgram = _query.getReachabilityProgram(_synthesizer.getInput());
      NodProgram program = new NodProgram(ctx, baseProgram, queryProgram);
      Fixedpoint fix = mkFixedpoint(program, false);
      for (int queryNum = 0; queryNum < program.getQueries().size(); queryNum++) {
        BoolExpr query = program.getQueries().get(queryNum);
        KeyT key = _query.getKeys().get(queryNum);
        Status status = fix.query(query);
        switch (status) {
          case SATISFIABLE:
            results.put(key, true);
            break;
          case UNKNOWN:
            return new NodSatResult<>(
                startTime,
                _logger.getHistory(),
                new BatfishException("Query satisfiability unknown"));
          case UNSATISFIABLE:
            results.put(key, false);
            break;
          default:
            return new NodSatResult<>(
                startTime, _logger.getHistory(), new BatfishException("invalid status"));
        }
      }
      return new NodSatResult<>(results, _logger.getHistory(), startTime);
    } catch (Z3Exception e) {
      return new NodSatResult<>(
          startTime,
          _logger.getHistory(),
          new BatfishException("Error running NoD on concatenated data plane", e));
    }
  }
}
