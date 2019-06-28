package org.batfish.datamodel.routing_policy.statement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.batfish.common.Warnings;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;

public class If extends Statement {
  private static final String PROP_FALSE_STATEMENTS = "falseStatements";
  private static final String PROP_GUARD = "guard";
  private static final String PROP_TRUE_STATEMENTS = "trueStatements";

  private List<Statement> _falseStatements;

  private BooleanExpr _guard;

  private List<Statement> _trueStatements;

  @JsonCreator
  public If() {
    this(null, null, new ArrayList<>(), new ArrayList<>());
  }

  public If(BooleanExpr guard, List<Statement> trueStatements) {
    this(null, guard, trueStatements, ImmutableList.of());
  }

  public If(BooleanExpr guard, List<Statement> trueStatements, List<Statement> falseStatements) {
    this(null, guard, trueStatements, falseStatements);
  }

  public If(
      @Nullable String comment,
      @Nullable BooleanExpr guard,
      List<Statement> trueStatements,
      List<Statement> falseStatements) {
    setComment(comment);
    _guard = guard;
    _trueStatements = trueStatements;
    _falseStatements = falseStatements;
  }

  @Override
  public Set<String> collectSources(
      Set<String> parentSources, Map<String, RoutingPolicy> routingPolicies, Warnings w) {
    ImmutableSet.Builder<String> childSources = ImmutableSet.builder();
    for (Statement statement : _falseStatements) {
      childSources.addAll(statement.collectSources(parentSources, routingPolicies, w));
    }
    for (Statement statement : _trueStatements) {
      childSources.addAll(statement.collectSources(parentSources, routingPolicies, w));
    }
    if (_guard != null) {
      childSources.addAll(_guard.collectSources(parentSources, routingPolicies, w));
    }
    return childSources.build();
  }

  @Override
  public String toString() {
    ToStringHelper h = MoreObjects.toStringHelper(this).add("guard", _guard);
    if (_trueStatements != null && !_trueStatements.isEmpty()) {
      h.add("trueStatements", _trueStatements);
    }
    if (_falseStatements != null && !_falseStatements.isEmpty()) {
      h.add("falseStatements", _falseStatements);
    }
    return h.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof If)) {
      return false;
    }
    If other = (If) obj;
    return Objects.equals(_guard, other._guard)
        && Objects.equals(_trueStatements, other._trueStatements)
        && Objects.equals(_falseStatements, other._falseStatements);
  }

  @Override
  public Result execute(Environment environment) {
    Result exprResult = _guard.evaluate(environment);
    if (exprResult.getExit()) {
      return exprResult;
    }
    boolean guardVal = exprResult.getBooleanValue();
    List<Statement> toExecute = guardVal ? _trueStatements : _falseStatements;
    for (Statement statement : toExecute) {
      Result result = statement.execute(environment);
      if (result.getExit() || result.getReturn()) {
        return result;
      }
    }
    return Result.builder().setFallThrough(true).build();
  }

  @JsonProperty(PROP_FALSE_STATEMENTS)
  public List<Statement> getFalseStatements() {
    return _falseStatements;
  }

  @JsonProperty(PROP_GUARD)
  public BooleanExpr getGuard() {
    return _guard;
  }

  @JsonProperty(PROP_TRUE_STATEMENTS)
  public List<Statement> getTrueStatements() {
    return _trueStatements;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_falseStatements == null) ? 0 : _falseStatements.hashCode());
    result = prime * result + ((_guard == null) ? 0 : _guard.hashCode());
    result = prime * result + ((_trueStatements == null) ? 0 : _trueStatements.hashCode());
    return result;
  }

  @JsonProperty(PROP_FALSE_STATEMENTS)
  public void setFalseStatements(List<Statement> falseStatements) {
    _falseStatements = falseStatements;
  }

  @JsonProperty(PROP_GUARD)
  public void setGuard(BooleanExpr guard) {
    _guard = guard;
  }

  @JsonProperty(PROP_TRUE_STATEMENTS)
  public void setTrueStatements(List<Statement> trueStatements) {
    _trueStatements = trueStatements;
  }

  @Override
  public List<Statement> simplify() {
    if (_simplified != null) {
      return _simplified;
    }
    ImmutableList.Builder<Statement> simpleTrueStatementsBuilder = ImmutableList.builder();
    ImmutableList.Builder<Statement> simpleFalseStatementsBuilder = ImmutableList.builder();
    BooleanExpr simpleGuard = _guard.simplify();
    for (Statement trueStatement : _trueStatements) {
      simpleTrueStatementsBuilder.addAll(trueStatement.simplify());
    }
    List<Statement> simpleTrueStatements = simpleTrueStatementsBuilder.build();
    for (Statement falseStatement : _falseStatements) {
      simpleFalseStatementsBuilder.addAll(falseStatement.simplify());
    }
    List<Statement> simpleFalseStatements = simpleFalseStatementsBuilder.build();
    if (simpleGuard.equals(BooleanExprs.TRUE)) {
      _simplified = simpleTrueStatements;
    } else if (simpleGuard.equals(BooleanExprs.FALSE)) {
      _simplified = simpleFalseStatements;
    } else if (simpleTrueStatements.isEmpty() && simpleFalseStatements.isEmpty()) {
      _simplified = Collections.emptyList();
    } else {
      If simple = new If(getComment(), simpleGuard, simpleTrueStatements, simpleFalseStatements);
      _simplified = ImmutableList.of(simple);
      simple._simplified = _simplified;
    }
    return _simplified;
  }
}
