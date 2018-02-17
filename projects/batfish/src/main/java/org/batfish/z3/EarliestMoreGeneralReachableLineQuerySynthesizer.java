package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicRuleStatement;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.RuleStatement;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.state.NumberedQuery;

public class EarliestMoreGeneralReachableLineQuerySynthesizer
    extends FirstUnsatQuerySynthesizer<AclLine, Integer> {

  private String _aclName;

  private List<AclLine> _earlierReachableLines;

  private String _hostname;

  private IpAccessList _list;

  private AclLine _unreachableLine;

  public EarliestMoreGeneralReachableLineQuerySynthesizer(
      AclLine unreachableLine, List<AclLine> earlierReachableLines, IpAccessList list) {
    super(unreachableLine);
    _unreachableLine = unreachableLine;
    _earlierReachableLines = earlierReachableLines;
    _hostname = _unreachableLine.getHostname();
    _aclName = _unreachableLine.getAclName();
    _list = list;
  }

  @Override
  public ReachabilityProgram getReachabilityProgram(SynthesizerInput input) {
    int unreachableLineIndex = _unreachableLine.getLine();
    IpAccessListLine unreachableLine = _list.getLines().get(unreachableLineIndex);
    BooleanExpr matchUnreachableLineHeaderSpace = new HeaderSpaceMatchExpr(unreachableLine);
    ImmutableList.Builder<QueryStatement> queries = ImmutableList.builder();
    ImmutableList.Builder<RuleStatement> rules = ImmutableList.builder();
    for (AclLine earlierReachableLine : _earlierReachableLines) {
      int earlierLineIndex = earlierReachableLine.getLine();
      IpAccessListLine earlierLine = _list.getLines().get(earlierLineIndex);
      BooleanExpr matchEarlierLineHeaderSpace = new HeaderSpaceMatchExpr(earlierLine);
      NumberedQuery queryRel = new NumberedQuery(earlierLineIndex);
      rules.add(
          new BasicRuleStatement(
              new AndExpr(
                  ImmutableList.of(
                      new NotExpr(matchEarlierLineHeaderSpace),
                      matchUnreachableLineHeaderSpace,
                      SaneExpr.INSTANCE)),
              queryRel));
      QueryStatement query = new QueryStatement(queryRel);
      queries.add(query);
      _resultsByQueryIndex.add(earlierLineIndex);
    }
    return ReachabilityProgram.builder()
        .setInput(input)
        .setQueries(queries.build())
        .setRules(rules.build())
        .build();
  }

  @Override
  public ReachabilityProgram synthesizeBaseProgram(Synthesizer synthesizer) {
    return synthesizer.synthesizeNodAclProgram(_hostname, _aclName);
  }
}
