package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicRuleStatement;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.TransformationRuleStatement;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.Drop;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.Query;

public class MultipathInconsistencyQuerySynthesizer extends BaseQuerySynthesizer {

  private HeaderSpace _headerSpace;

  private String _hostname;

  private String _vrf;

  public MultipathInconsistencyQuerySynthesizer(
      String hostname, String vrf, HeaderSpace headerSpace) {
    _hostname = hostname;
    _vrf = vrf;
    _headerSpace = headerSpace;
  }

  @Override
  public ReachabilityProgram getReachabilityProgram(SynthesizerInput input) {
    return ReachabilityProgram.builder()
        .setInput(input)
        .setQueries(ImmutableList.of(new QueryStatement(Query.INSTANCE)))
        .setRules(
            ImmutableList.of(
                new BasicRuleStatement(
                    CurrentIsOriginalExpr.INSTANCE, new OriginateVrf(_hostname, _vrf)),
                new TransformationRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            Accept.INSTANCE,
                            Drop.INSTANCE,
                            SaneExpr.INSTANCE,
                            new HeaderSpaceMatchExpr(_headerSpace))),
                    Query.INSTANCE)))
        .build();
  }
}
