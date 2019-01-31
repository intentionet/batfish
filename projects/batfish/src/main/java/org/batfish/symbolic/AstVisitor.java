package org.batfish.symbolic;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.ConjunctionChain;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.FirstMatchChain;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetDefaultPolicy;
import org.batfish.datamodel.routing_policy.statement.Statement;

/**
 * A helper visitor class for the Batfish datamodel. The AstVisitor lets users walk over each
 * statement and expression in the Batfish datamodel and apply callback functions on the element.
 *
 * @author Ryan Beckett
 */
public class AstVisitor {

  public AstVisitor() {}

  /*
   * Walk starting from an AST boolean expression
   */
  public void visit(
      Configuration conf, BooleanExpr e, Consumer<Statement> fs, Consumer<BooleanExpr> fe) {
    fe.accept(e);
    if (e instanceof Conjunction) {
      Conjunction c = (Conjunction) e;
      for (BooleanExpr be : c.getConjuncts()) {
        visit(conf, be, fs, fe);
      }

    } else if (e instanceof Disjunction) {
      Disjunction d = (Disjunction) e;
      for (BooleanExpr be : d.getDisjuncts()) {
        visit(conf, be, fs, fe);
      }

    } else if (e instanceof ConjunctionChain) {
      ConjunctionChain c = (ConjunctionChain) e;
      for (BooleanExpr be : c.getSubroutines()) {
        visit(conf, be, fs, fe);
      }

    } else if (e instanceof FirstMatchChain) {
      FirstMatchChain p = (FirstMatchChain) e;
      for (BooleanExpr be : p.getSubroutines()) {
        visit(conf, be, fs, fe);
      }

    } else if (e instanceof Not) {
      Not n = (Not) e;
      visit(conf, n.getExpr(), fs, fe);

    } else if (e instanceof CallExpr) {
      CallExpr c = (CallExpr) e;
      RoutingPolicy rp = conf.getRoutingPolicies().get(c.getCalledPolicyName());
      visit(conf, rp.getStatements(), fs, fe);
    }
  }

  /*
   * Walk starting from an AST statement
   */
  public void visit(
      Configuration conf, Statement s, Consumer<Statement> fs, Consumer<BooleanExpr> fe) {
    fs.accept(s);

    if (s instanceof If) {
      If i = (If) s;
      visit(conf, i.getGuard(), fs, fe);
      visit(conf, i.getTrueStatements(), fs, fe);
      visit(conf, i.getFalseStatements(), fs, fe);

    } else if (s instanceof SetDefaultPolicy) {
      SetDefaultPolicy p = (SetDefaultPolicy) s;
      RoutingPolicy rp = conf.getRoutingPolicies().get(p.getDefaultPolicy());
      visit(conf, rp.getStatements(), fs, fe);
    }
  }

  /*
   * Walk over a list of AST statements
   */
  public void visit(
      Configuration conf, List<Statement> ss, Consumer<Statement> fs, Consumer<BooleanExpr> fe) {
    for (Statement s : ss) {
      visit(conf, s, fs, fe);
    }
  }

  public void visit(
      Collection<Configuration> configs, Consumer<Statement> fs, Consumer<BooleanExpr> fe) {
    for (Configuration conf : configs) {
      for (RoutingPolicy pol : conf.getRoutingPolicies().values()) {
        visit(conf, pol.getStatements(), fs, fe);
      }
    }
  }
}
