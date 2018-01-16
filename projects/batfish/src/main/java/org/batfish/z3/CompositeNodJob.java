package org.batfish.z3;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.config.Settings;
import org.batfish.datamodel.Flow;
import org.batfish.job.BatfishJob;

public class CompositeNodJob extends BatfishJob<NodJobResult> {

  private List<Synthesizer> _dataPlaneSynthesizers;

  private final SortedSet<Pair<String, String>> _nodeVrfSet;

  private int _numPrograms;

  private List<QuerySynthesizer> _querySynthesizers;

  private String _tag;

  public CompositeNodJob(
      Settings settings,
      List<Synthesizer> dataPlaneSynthesizer,
      List<QuerySynthesizer> querySynthesizer,
      SortedSet<Pair<String, String>> nodeVrfSet,
      String tag) {
    super(settings);
    _numPrograms = dataPlaneSynthesizer.size();
    if (_numPrograms != querySynthesizer.size()) {
      throw new BatfishException("mismatch between number of programs and number of queries");
    }
    _dataPlaneSynthesizers = dataPlaneSynthesizer;
    _querySynthesizers = querySynthesizer;
    _nodeVrfSet = new TreeSet<>();
    _nodeVrfSet.addAll(nodeVrfSet);
    _tag = tag;
  }

  @Override
  public NodJobResult call() {
    long startTime = System.currentTimeMillis();
    long elapsedTime;
    NodProgram latestProgram = null;
    try (Context ctx = new Context()) {
      BoolExpr[] answers = new BoolExpr[_numPrograms];
      Params p = ctx.mkParams();
      p.add("timeout", _settings.getZ3timeout());
      p.add("fixedpoint.engine", "datalog");
      p.add("fixedpoint.datalog.default_relation", "doc");
      p.add("fixedpoint.print_answer", true);
      for (int i = 0; i < _numPrograms; i++) {
        Synthesizer dataPlaneSynthesizer = _dataPlaneSynthesizers.get(i);
        QuerySynthesizer querySynthesizer = _querySynthesizers.get(i);
        NodProgram baseProgram = dataPlaneSynthesizer.synthesizeNodDataPlaneProgram(ctx);
        NodProgram queryProgram = querySynthesizer.getNodProgram(baseProgram);
        NodProgram program = baseProgram.append(queryProgram);
        latestProgram = program;
        Fixedpoint fix = ctx.mkFixedpoint();
        fix.setParameters(p);
        for (FuncDecl relationDeclaration : program.getRelationDeclarations().values()) {
          fix.registerRelation(relationDeclaration);
        }
        for (BoolExpr rule : program.getRules()) {
          fix.addRule(rule, null);
        }
        for (BoolExpr query : program.getQueries()) {
          Status status = fix.query(query);
          switch (status) {
            case SATISFIABLE:
              break;

            case UNKNOWN:
              throw new BatfishException("Query satisfiability unknown");

            case UNSATISFIABLE:
              break;

            default:
              throw new BatfishException("invalid status");
          }
        }
        Expr answer = fix.getAnswer();
        BoolExpr solverInput;
        if (answer.getArgs().length > 0) {
          List<Expr> reversedVarList = new ArrayList<>();
          reversedVarList.addAll(program.getVariablesAsConsts().values());
          Collections.reverse(reversedVarList);
          Expr[] reversedVars = reversedVarList.toArray(new Expr[] {});
          Expr substitutedAnswer = answer.substituteVars(reversedVars);
          solverInput = (BoolExpr) substitutedAnswer;
        } else {
          solverInput = (BoolExpr) answer;
        }
        if (_querySynthesizers.get(i).getNegate()) {
          answers[i] = ctx.mkNot(solverInput);
        } else {
          answers[i] = solverInput;
        }
      }
      BoolExpr compositeQuery = ctx.mkAnd(answers);
      Solver solver = ctx.mkSolver();
      solver.add(compositeQuery);
      Status solverStatus = solver.check();
      switch (solverStatus) {
        case SATISFIABLE:
          break;

        case UNKNOWN:
          throw new BatfishException("Stage 2 query satisfiability unknown");

        case UNSATISFIABLE:
          elapsedTime = System.currentTimeMillis() - startTime;
          return new NodJobResult(elapsedTime, _logger.getHistory());

        default:
          throw new BatfishException("invalid status");
      }
      Model model = solver.getModel();
      Map<String, Long> constraints = new LinkedHashMap<>();
      for (FuncDecl constDecl : model.getConstDecls()) {
        String name = constDecl.getName().toString();
        BitVecExpr varConstExpr = latestProgram.getVariablesAsConsts().get(name);
        long val = ((BitVecNum) model.getConstInterp(varConstExpr)).getLong();
        constraints.put(name, val);
      }
      Set<Flow> flows = new HashSet<>();
      for (Pair<String, String> nodeVrf : _nodeVrfSet) {
        String node = nodeVrf.getFirst();
        String vrf = nodeVrf.getSecond();
        Flow flow = createFlow(node, vrf, constraints);
        flows.add(flow);
      }
      elapsedTime = System.currentTimeMillis() - startTime;
      return new NodJobResult(elapsedTime, _logger.getHistory(), flows);
    } catch (Z3Exception e) {
      elapsedTime = System.currentTimeMillis() - startTime;
      return new NodJobResult(
          elapsedTime,
          _logger.getHistory(),
          new BatfishException("Error running NoD on concatenated data plane", e));
    }
  }

  private Flow createFlow(String node, String vrf, Map<String, Long> constraints) {
    return NodJob.createFlow(node, vrf, constraints, _tag);
  }
}
