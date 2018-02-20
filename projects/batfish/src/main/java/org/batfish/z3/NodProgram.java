package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.RuleStatement;
import org.batfish.z3.expr.visitors.BoolExprTransformer;

public class NodProgram {

  private final NodContext _context;

  private final List<BoolExpr> _queries;

  private final List<BoolExpr> _rules;

  public NodProgram(Context ctx, ReachabilityProgram... programs) {
    _context = new NodContext(ctx, programs);
    _queries =
        Arrays.stream(programs)
            .flatMap(
                program ->
                    program
                        .getQueries()
                        .stream()
                        .map(QueryStatement::getSubExpression)
                        .map(
                            booleanExpr ->
                                BoolExprTransformer.toBoolExpr(
                                    booleanExpr, program.getInput(), _context)))
            .collect(ImmutableList.toImmutableList());
    _rules =
        Arrays.stream(programs)
            .flatMap(
                program ->
                    program
                        .getRules()
                        .stream()
                        .map(RuleStatement::getSubExpression)
                        .map(
                            booleanExpr ->
                                BoolExprTransformer.toBoolExpr(
                                    booleanExpr, program.getInput(), _context)))
            .collect(ImmutableList.toImmutableList());
  }

  public NodContext getNodContext() {
    return _context;
  }

  public List<BoolExpr> getQueries() {
    return _queries;
  }

  public List<BoolExpr> getRules() {
    return _rules;
  }

  public String toSmt2String() {
    StringBuilder sb = new StringBuilder();
    Arrays.stream(BasicHeaderField.values())
        .forEach(
            hf -> {
              String var = hf.name();
              int size = hf.getSize();
              sb.append(String.format("(declare-var %s (_ BitVec %d))\n", var, size));
            });
    StringBuilder sizeSb = new StringBuilder("(");
    Arrays.stream(BasicHeaderField.values())
        .map(BasicHeaderField::getSize)
        .forEach(s -> sizeSb.append(String.format(" (_ BitVec %d)", s)));
    String sizes = sizeSb.append(")").toString();
    _context
        .getRelationDeclarations()
        .keySet()
        .stream()
        .map(
            r ->
                r.contains(":") || r.contains("(") || r.contains(")")
                    ? String.format("|%s|", r)
                    : r)
        .forEach(relation -> sb.append(String.format("(declare-rel %s %s)\n", relation, sizes)));
    _rules.forEach(r -> sb.append(String.format("(rule %s)\n", r.toString())));

    sb.append("\n");
    sb.append("(query query_relation)\n");
    String[] intermediate = new String[] {sb.toString()};
    final AtomicInteger currentVar = new AtomicInteger(0);
    Arrays.stream(BasicHeaderField.values())
        .map(BasicHeaderField::name)
        .collect(
            ImmutableMap.toImmutableMap(
                Function.identity(), v -> String.format("(:var %d)", currentVar.getAndIncrement())))
        .forEach(
            (name, var) ->
                intermediate[0] =
                    intermediate[0].replaceAll(Pattern.quote(var), Matcher.quoteReplacement(name)));
    return intermediate[0];
  }
}
