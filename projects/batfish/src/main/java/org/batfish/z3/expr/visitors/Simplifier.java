package org.batfish.z3.expr.visitors;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Stream;
import org.batfish.common.BatfishException;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicRuleStatement;
import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.BitVecExpr;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.Comment;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.DeclareRelStatement;
import org.batfish.z3.expr.DeclareVarStatement;
import org.batfish.z3.expr.EqExpr;
import org.batfish.z3.expr.ExtractExpr;
import org.batfish.z3.expr.FalseExpr;
import org.batfish.z3.expr.GenericStatementVisitor;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.IdExpr;
import org.batfish.z3.expr.IfExpr;
import org.batfish.z3.expr.IntExpr;
import org.batfish.z3.expr.ListExpr;
import org.batfish.z3.expr.LitIntExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.PrefixMatchExpr;
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.expr.RuleStatement;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.Statement;
import org.batfish.z3.expr.TransformationRuleStatement;
import org.batfish.z3.expr.TransformationStateExpr;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.expr.VarIntExpr;

public class Simplifier implements ExprVisitor, GenericStatementVisitor<Statement> {

  private static final Comment UNUSABLE_RULE = new Comment("(unsatisfiable rule)");

  private static final Comment VACUOUS_RULE = new Comment("(vacuous rule)");

  public static BooleanExpr simplifyBooleanExpr(BooleanExpr expr) {
    Simplifier simplifier = new Simplifier();
    expr.accept(simplifier);
    return simplifier._simplifiedBooleanExpr;
  }

  public static Statement simplifyStatement(Statement statement) {
    Simplifier simplifier = new Simplifier();
    return statement.accept(simplifier);
  }

  private BooleanExpr _simplifiedBooleanExpr;

  private Simplifier() {}

  @Override
  public void visitAndExpr(AndExpr andExpr) {
    boolean changed = false;
    List<BooleanExpr> oldConjuncts = andExpr.getConjuncts();
    ImmutableList.Builder<BooleanExpr> newConjunctsBuilder = ImmutableList.builder();

    // first check for nested ANDs
    if (oldConjuncts.stream().anyMatch(Predicates.instanceOf(AndExpr.class))) {
      _simplifiedBooleanExpr =
          simplifyBooleanExpr(
              new AndExpr(
                  oldConjuncts
                      .stream()
                      .flatMap(
                          conjunct ->
                              conjunct instanceof AndExpr
                                  ? ((AndExpr) conjunct).getConjuncts().stream()
                                  : Stream.of(conjunct))
                      .collect(ImmutableList.toImmutableList())));
      return;
    }

    // no nested ANDs, so just simplify all conjuncts
    for (BooleanExpr conjunct : oldConjuncts) {
      BooleanExpr simplifiedConjunct = simplifyBooleanExpr(conjunct);
      if (conjunct != simplifiedConjunct) {
        changed = true;
      }
      if (simplifiedConjunct == FalseExpr.INSTANCE) {
        _simplifiedBooleanExpr = FalseExpr.INSTANCE;
        return;
      } else if (simplifiedConjunct != TrueExpr.INSTANCE) {
        newConjunctsBuilder.add(simplifiedConjunct);
      } else {
        changed = true;
      }
    }
    List<BooleanExpr> newConjuncts = newConjunctsBuilder.build();
    if (newConjuncts.size() == 0) {
      _simplifiedBooleanExpr = TrueExpr.INSTANCE;
    } else if (newConjuncts.size() == 1) {
      _simplifiedBooleanExpr = newConjuncts.get(0);
    } else if (!changed) {
      _simplifiedBooleanExpr = andExpr;
    } else {
      _simplifiedBooleanExpr = new AndExpr(newConjuncts);
    }
  }

  @Override
  public Statement visitBasicRuleStatement(BasicRuleStatement basicRuleStatement) {
    return visitRuleStatement(basicRuleStatement);
  }

  @Override
  public void visitBasicStateExpr(BasicStateExpr basicStateExpr) {
    _simplifiedBooleanExpr = basicStateExpr;
  }

  @Override
  public void visitBitVecExpr(BitVecExpr bitVecExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Statement visitComment(Comment comment) {
    return comment;
  }

  @Override
  public void visitCurrentIsOriginal(CurrentIsOriginalExpr currentIsOriginalExpr) {
    _simplifiedBooleanExpr = simplifyBooleanExpr(currentIsOriginalExpr.getExpr());
  }

  @Override
  public Statement visitDeclareRelStatement(DeclareRelStatement declareRelStatement) {
    return declareRelStatement;
  }

  @Override
  public Statement visitDeclareVarStatement(DeclareVarStatement declareVarStatement) {
    return declareVarStatement;
  }

  @Override
  public void visitEqExpr(EqExpr eqExpr) {
    IntExpr lhs = eqExpr.getLhs();
    IntExpr rhs = eqExpr.getRhs();
    if (lhs.equals(rhs)) {
      _simplifiedBooleanExpr = TrueExpr.INSTANCE;
    } else if (lhs instanceof LitIntExpr && rhs instanceof LitIntExpr) {
      _simplifiedBooleanExpr = FalseExpr.INSTANCE;
    } else {
      _simplifiedBooleanExpr = eqExpr;
    }
  }

  @Override
  public void visitExtractExpr(ExtractExpr extractExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void visitFalseExpr(FalseExpr falseExpr) {
    _simplifiedBooleanExpr = falseExpr;
  }

  @Override
  public void visitHeaderSpaceMatchExpr(HeaderSpaceMatchExpr headerSpaceMatchExpr) {
    _simplifiedBooleanExpr = simplifyBooleanExpr(headerSpaceMatchExpr.getExpr());
  }

  @Override
  public void visitIdExpr(IdExpr idExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void visitIfExpr(IfExpr ifExpr) {
    BooleanExpr oldAntecedent = ifExpr.getAntecedent();
    BooleanExpr oldConsequent = ifExpr.getConsequent();
    BooleanExpr newAntecedent = simplifyBooleanExpr(oldAntecedent);
    BooleanExpr newConsequent = simplifyBooleanExpr(oldConsequent);
    if (newAntecedent == FalseExpr.INSTANCE
        || newConsequent == TrueExpr.INSTANCE
        || newAntecedent.equals(newConsequent)) {
      _simplifiedBooleanExpr = TrueExpr.INSTANCE;
    } else if (newAntecedent == TrueExpr.INSTANCE) {
      _simplifiedBooleanExpr = newConsequent;
    } else if (newAntecedent != oldAntecedent || newConsequent != oldConsequent) {
      _simplifiedBooleanExpr = new IfExpr(newAntecedent, newConsequent);
    } else {
      _simplifiedBooleanExpr = ifExpr;
    }
  }

  @Override
  public void visitListExpr(ListExpr listExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void visitLitIntExpr(LitIntExpr litIntExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void visitNotExpr(NotExpr notExpr) {
    BooleanExpr oldArg = notExpr.getArg();
    BooleanExpr newArg = simplifyBooleanExpr(oldArg);
    if (newArg == FalseExpr.INSTANCE) {
      _simplifiedBooleanExpr = TrueExpr.INSTANCE;
    } else if (newArg == TrueExpr.INSTANCE) {
      _simplifiedBooleanExpr = FalseExpr.INSTANCE;
    } else if (newArg instanceof NotExpr) {
      _simplifiedBooleanExpr = ((NotExpr) newArg).getArg();
    } else if (newArg != oldArg) {
      _simplifiedBooleanExpr = new NotExpr(newArg);
    } else {
      _simplifiedBooleanExpr = notExpr;
    }
  }

  @Override
  public void visitOrExpr(OrExpr orExpr) {
    boolean changed = false;
    List<BooleanExpr> oldDisjuncts = orExpr.getDisjuncts();
    ImmutableList.Builder<BooleanExpr> newDisjunctsBuilder = ImmutableList.builder();

    // first check for nested ORs
    if (oldDisjuncts.stream().anyMatch(Predicates.instanceOf(OrExpr.class))) {
      _simplifiedBooleanExpr =
          simplifyBooleanExpr(
              new OrExpr(
                  oldDisjuncts
                      .stream()
                      .flatMap(
                          disjunct ->
                              disjunct instanceof OrExpr
                                  ? ((OrExpr) disjunct).getDisjuncts().stream()
                                  : Stream.of(disjunct))
                      .collect(ImmutableList.toImmutableList())));
      return;
    }

    // no nested ORs, so just simplify all disjuncts
    for (BooleanExpr disjunct : oldDisjuncts) {
      BooleanExpr simplifiedDisjunct = simplifyBooleanExpr(disjunct);
      if (disjunct != simplifiedDisjunct) {
        changed = true;
      }
      if (simplifiedDisjunct == TrueExpr.INSTANCE) {
        _simplifiedBooleanExpr = TrueExpr.INSTANCE;
        return;
      } else if (simplifiedDisjunct != FalseExpr.INSTANCE) {
        newDisjunctsBuilder.add(simplifiedDisjunct);
      } else {
        changed = true;
      }
    }
    List<BooleanExpr> newDisjuncts = newDisjunctsBuilder.build();
    if (newDisjuncts.size() == 0) {
      _simplifiedBooleanExpr = FalseExpr.INSTANCE;
    } else if (newDisjuncts.size() == 1) {
      _simplifiedBooleanExpr = newDisjuncts.get(0);
    } else if (!changed) {
      _simplifiedBooleanExpr = orExpr;
    } else {
      _simplifiedBooleanExpr = new OrExpr(newDisjuncts);
    }
  }

  @Override
  public void visitPrefixMatchExpr(PrefixMatchExpr prefixMatchExpr) {
    _simplifiedBooleanExpr = simplifyBooleanExpr(prefixMatchExpr.getExpr());
  }

  @Override
  public Statement visitQueryStatement(QueryStatement queryStatement) {
    return queryStatement;
  }

  @Override
  public void visitRangeMatchExpr(RangeMatchExpr rangeMatchExpr) {
    _simplifiedBooleanExpr = simplifyBooleanExpr(rangeMatchExpr.getExpr());
  }

  public Statement visitRuleStatement(RuleStatement ruleStatement) {
    BooleanExpr oldExpr = ruleStatement.getSubExpression();
    BooleanExpr newExpr = simplifyBooleanExpr(oldExpr);
    if (newExpr != oldExpr) {
      if (newExpr == TrueExpr.INSTANCE) {
        return VACUOUS_RULE;
      } else if (newExpr == FalseExpr.INSTANCE) {
        throw new BatfishException("Unsatifiable!");
      } else if (newExpr instanceof IfExpr
          && ((IfExpr) newExpr).getAntecedent() == FalseExpr.INSTANCE) {
        return UNUSABLE_RULE;
      } else if (newExpr instanceof BasicStateExpr) {
        return new BasicRuleStatement((BasicStateExpr) newExpr);
      } else if (newExpr instanceof TransformationStateExpr) {
        return new TransformationRuleStatement((TransformationStateExpr) newExpr);
      } else if (newExpr instanceof IfExpr) {
        IfExpr newInterior = (IfExpr) newExpr;
        BooleanExpr newConsequent = newInterior.getConsequent();
        if (newConsequent instanceof BasicStateExpr) {
          return new BasicRuleStatement(
              newInterior.getAntecedent(), (BasicStateExpr) newConsequent);
        } else if (newConsequent instanceof TransformationStateExpr) {
          return new TransformationRuleStatement(
              newInterior.getAntecedent(), (TransformationStateExpr) newConsequent);
        } else {
          throw new BatfishException(
              String.format(
                  "Unexpected consequent type after simplification: %s",
                  newConsequent.getClass().getCanonicalName()));
        }
      } else {
        throw new BatfishException(
            String.format(
                "Unexpected type after simplification: %s", newExpr.getClass().getCanonicalName()));
      }
    } else {
      return ruleStatement;
    }
  }

  @Override
  public void visitSaneExpr(SaneExpr saneExpr) {
    _simplifiedBooleanExpr = simplifyBooleanExpr(saneExpr.getExpr());
  }

  @Override
  public Statement visitTransformationRuleStatement(
      TransformationRuleStatement transformationRuleStatement) {
    return visitRuleStatement(transformationRuleStatement);
  }

  @Override
  public void visitTransformationStateExpr(TransformationStateExpr transformationStateExpr) {
    /** TODO: something smarter */
    _simplifiedBooleanExpr = transformationStateExpr;
  }

  @Override
  public void visitTrueExpr(TrueExpr trueExpr) {
    _simplifiedBooleanExpr = trueExpr;
  }

  @Override
  public void visitVarIntExpr(VarIntExpr varIntExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }
}
