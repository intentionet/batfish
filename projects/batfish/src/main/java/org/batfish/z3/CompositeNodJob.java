package org.batfish.z3;

import com.google.common.collect.ImmutableSet.Builder;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.config.Settings;

public class CompositeNodJob extends AbstractNodJob {

  private List<Synthesizer> _dataPlaneSynthesizers;

  private int _numPrograms;

  private List<QuerySynthesizer> _querySynthesizers;

  public CompositeNodJob(
      Settings settings,
      List<Synthesizer> dataPlaneSynthesizer,
      List<QuerySynthesizer> querySynthesizer,
      SortedSet<Pair<String, String>> nodeVrfSet,
      String tag) {
    super(settings, nodeVrfSet, tag);
    _numPrograms = dataPlaneSynthesizer.size();
    if (_numPrograms != querySynthesizer.size()) {
      throw new BatfishException("mismatch between number of programs and number of queries");
    }
    _dataPlaneSynthesizers = dataPlaneSynthesizer;
    _querySynthesizers = querySynthesizer;
  }

  @Override
  protected BoolExpr computeSmtInput(
      long startTime, Context ctx, Builder<Entry<String, BitVecExpr>> variablesAsConstsBuilder) {
    BoolExpr[] answers = new BoolExpr[_numPrograms];
    for (int i = 0; i < _numPrograms; i++) {
      Synthesizer dataPlaneSynthesizer = _dataPlaneSynthesizers.get(i);
      QuerySynthesizer querySynthesizer = _querySynthesizers.get(i);
      ReachabilityProgram baseProgram = dataPlaneSynthesizer.synthesizeNodDataPlaneProgram();
      ReachabilityProgram queryProgram =
          querySynthesizer.getReachabilityProgram(dataPlaneSynthesizer.getInput());
      NodProgram program = new NodProgram(ctx, baseProgram, queryProgram);
      variablesAsConstsBuilder.addAll(program.getNodContext().getVariablesAsConsts().entrySet());
      answers[i] = computeSmtConstraintsViaNod(program, _querySynthesizers.get(i).getNegate());
    }
    return ctx.mkAnd(answers);
  }
}
