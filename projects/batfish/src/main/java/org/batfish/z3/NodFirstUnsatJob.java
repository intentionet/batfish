package org.batfish.z3;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Params;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import org.batfish.common.BatfishException;
import org.batfish.config.Settings;
import org.batfish.job.BatfishJob;

public class NodFirstUnsatJob<KeyT, ResultT>
    extends BatfishJob<NodFirstUnsatResult<KeyT, ResultT>> {

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
    long elapsedTime;
    try (Context ctx = new Context()) {
      ReachabilityProgram baseProgram = _query.synthesizeBaseProgram(_synthesizer);
      ReachabilityProgram queryProgram = _query.getReachabilityProgram(_synthesizer.getInput());
      NodProgram program = new NodProgram(ctx, baseProgram, queryProgram);
      Params p = ctx.mkParams();
      p.add("fixedpoint.engine", "datalog");
      p.add("fixedpoint.datalog.default_relation", "doc");
      Fixedpoint fix = ctx.mkFixedpoint();
      fix.setParameters(p);
      for (FuncDecl relationDeclaration : program.getContext().getRelationDeclarations().values()) {
        fix.registerRelation(relationDeclaration);
      }
      for (BoolExpr rule : program.getRules()) {
        fix.addRule(rule, null);
      }
      KeyT key = _query.getKey();
      for (int queryNum = 0; queryNum < program.getQueries().size(); queryNum++) {
        BoolExpr query = program.getQueries().get(queryNum);
        Status status = fix.query(query);
        elapsedTime = System.currentTimeMillis() - startTime;
        switch (status) {
          case SATISFIABLE:
            break;
          case UNKNOWN:
            return new NodFirstUnsatResult<>(
                elapsedTime,
                _logger.getHistory(),
                new BatfishException("Query satisfiability unknown"));
          case UNSATISFIABLE:
            return new NodFirstUnsatResult<>(
                key,
                queryNum,
                _query.getResultsByQueryIndex().get(queryNum),
                _logger.getHistory(),
                elapsedTime);
          default:
            return new NodFirstUnsatResult<>(
                elapsedTime, _logger.getHistory(), new BatfishException("invalid status"));
        }
      }
      elapsedTime = System.currentTimeMillis() - startTime;
      return new NodFirstUnsatResult<>(key, null, null, _logger.getHistory(), elapsedTime);
    } catch (Z3Exception e) {
      elapsedTime = System.currentTimeMillis() - startTime;
      return new NodFirstUnsatResult<>(
          elapsedTime,
          _logger.getHistory(),
          new BatfishException("Error running NoD on concatenated data plane", e));
    }
  }
}
