package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Stream;
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.RuleStatement;
import org.batfish.z3.expr.visitors.Simplifier;

public class ReachabilityProgram {

  public static class Builder {

    private SynthesizerInput _input;

    private List<QueryStatement> _queries;

    private List<RuleStatement> _rules;

    private Builder() {
      _queries = ImmutableList.of();
      _rules = ImmutableList.of();
    }

    public ReachabilityProgram build() {
      return new ReachabilityProgram(_queries, _rules, _input);
    }

    public Builder setInput(SynthesizerInput input) {
      _input = input;
      return this;
    }

    public Builder setQueries(List<QueryStatement> queries) {
      _queries = ImmutableList.copyOf(queries);
      return this;
    }

    public Builder setRules(List<RuleStatement> rules) {
      _rules = ImmutableList.copyOf(rules);
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final SynthesizerInput _input;

  private final List<QueryStatement> _queries;

  private final List<RuleStatement> _rules;

  private ReachabilityProgram(
      List<QueryStatement> queries, List<RuleStatement> rules, SynthesizerInput input) {
    _queries = ImmutableList.copyOf(queries);
    _input = input;

    /*
     * Simplify rule statements if desired, and remove statements that simplify to trivial
     * statements that are no longer rules.
     */
    _rules =
        _input.getSimplify()
            ? rules
                .stream()
                .map(Simplifier::simplifyStatement)
                .filter(s -> s instanceof RuleStatement)
                .map(s -> (RuleStatement) s)
                .collect(ImmutableList.toImmutableList())
            : rules;
  }

  public SynthesizerInput getInput() {
    return _input;
  }

  public List<QueryStatement> getQueries() {
    return _queries;
  }

  public List<RuleStatement> getRules() {
    return _rules;
  }
}
