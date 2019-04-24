package org.batfish.symbolic.bdd;

import static org.batfish.symbolic.CommunityVarCollector.collectCommunityVars;
import static org.batfish.symbolic.bdd.CommunityVarConverter.toCommunityVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.BatfishException;
import org.batfish.common.bdd.BDDInteger;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.AsPathListExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.ConjunctionChain;
import org.batfish.datamodel.routing_policy.expr.DecrementLocalPreference;
import org.batfish.datamodel.routing_policy.expr.DecrementMetric;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.FirstMatchChain;
import org.batfish.datamodel.routing_policy.expr.IncrementLocalPreference;
import org.batfish.datamodel.routing_policy.expr.IncrementMetric;
import org.batfish.datamodel.routing_policy.expr.IntExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralAsList;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LongExpr;
import org.batfish.datamodel.routing_policy.expr.MatchAsPath;
import org.batfish.datamodel.routing_policy.expr.MatchCommunitySet;
import org.batfish.datamodel.routing_policy.expr.MatchIpv4;
import org.batfish.datamodel.routing_policy.expr.MatchIpv6;
import org.batfish.datamodel.routing_policy.expr.MatchPrefix6Set;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.MultipliedAs;
import org.batfish.datamodel.routing_policy.expr.NamedCommunitySet;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.PrefixSetExpr;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.AddCommunity;
import org.batfish.datamodel.routing_policy.statement.DeleteCommunity;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.PrependAsPath;
import org.batfish.datamodel.routing_policy.statement.RetainCommunity;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.batfish.datamodel.routing_policy.statement.SetDefaultPolicy;
import org.batfish.datamodel.routing_policy.statement.SetLocalPreference;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements.StaticStatement;
import org.batfish.symbolic.CommunityVar;
import org.batfish.symbolic.CommunityVar.Type;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.OspfType;
import org.batfish.symbolic.Protocol;
import org.batfish.symbolic.TransferParam;
import org.batfish.symbolic.TransferResult;
import org.batfish.symbolic.collections.Table2;
import org.batfish.symbolic.utils.PrefixUtils;

/** @author Ryan Beckett */
class TransferBDD {

  private static BDDFactory factory = BDDRoute.factory;

  private static Table2<String, String, TransferResult<TransferReturn, BDD>> CACHE = new Table2<>();

  private SortedMap<CommunityVar, List<CommunityVar>> _commDeps;

  private Set<CommunityVar> _comms;

  private Configuration _conf;

  private Graph _graph;

  private PolicyQuotient _policyQuotient;

  private Set<Prefix> _ignoredNetworks;

  private List<Statement> _statements;

  TransferBDD(Graph g, Configuration conf, List<Statement> statements, PolicyQuotient pq) {
    _graph = g;
    _conf = conf;
    _policyQuotient = pq;
    _statements = statements;
  }

  /*
   * Check if the first length bits match the BDDInteger
   * representing the advertisement prefix.
   *
   * Note: We assume the prefix is never modified, so it will
   * be a bitvector containing only the underlying variables:
   * [var(0), ..., var(n)]
   */
  public static BDD firstBitsEqual(BDD[] bits, Prefix p, int length) {
    long b = p.getStartIp().asLong();
    BDD acc = factory.one();
    for (int i = 0; i < length; i++) {
      boolean res = Ip.getBitAtPosition(b, i);
      if (res) {
        acc = acc.and(bits[i]);
      } else {
        acc = acc.diff(bits[i]);
      }
    }
    return acc;
  }

  /*
   * Apply the effect of modifying a long value (e.g., to set the metric)
   */
  private BDDInteger applyLongExprModification(
      TransferParam<BDDRoute> p, BDDInteger x, LongExpr e) {
    if (e instanceof LiteralLong) {
      LiteralLong z = (LiteralLong) e;
      p.debug("LiteralLong: " + z.getValue());
      return BDDInteger.makeFromValue(x.getFactory(), 32, z.getValue());
    }
    if (e instanceof DecrementMetric) {
      DecrementMetric z = (DecrementMetric) e;
      p.debug("Decrement: " + z.getSubtrahend());
      return x.sub(BDDInteger.makeFromValue(x.getFactory(), 32, z.getSubtrahend()));
    }
    if (e instanceof IncrementMetric) {
      IncrementMetric z = (IncrementMetric) e;
      p.debug("Increment: " + z.getAddend());
      return x.add(BDDInteger.makeFromValue(x.getFactory(), 32, z.getAddend()));
    }
    if (e instanceof IncrementLocalPreference) {
      IncrementLocalPreference z = (IncrementLocalPreference) e;
      p.debug("IncrementLocalPreference: " + z.getAddend());
      return x.add(BDDInteger.makeFromValue(x.getFactory(), 32, z.getAddend()));
    }
    if (e instanceof DecrementLocalPreference) {
      DecrementLocalPreference z = (DecrementLocalPreference) e;
      p.debug("DecrementLocalPreference: " + z.getSubtrahend());
      return x.sub(BDDInteger.makeFromValue(x.getFactory(), 32, z.getSubtrahend()));
    }
    throw new BatfishException("int expr transfer function: " + e);
  }

  /*
   * Convert a Batfish AST boolean expression to a symbolic Z3 boolean expression
   * by performing inlining of stateful side effects.
   */
  private TransferResult<TransferReturn, BDD> compute(BooleanExpr expr, TransferParam<BDDRoute> p) {

    // TODO: right now everything is IPV4
    if (expr instanceof MatchIpv4) {
      p.debug("MatchIpv4");
      TransferReturn ret = new TransferReturn(p.getData(), factory.one());
      p.debug("MatchIpv4 Result: " + ret);
      return fromExpr(ret);
    }
    if (expr instanceof MatchIpv6) {
      p.debug("MatchIpv6");
      TransferReturn ret = new TransferReturn(p.getData(), factory.zero());
      return fromExpr(ret);
    }

    if (expr instanceof Conjunction) {
      p.debug("Conjunction");
      Conjunction c = (Conjunction) expr;
      BDD acc = factory.one();
      TransferResult<TransferReturn, BDD> result = new TransferResult<>();
      for (BooleanExpr be : c.getConjuncts()) {
        TransferResult<TransferReturn, BDD> r = compute(be, p.indent());
        acc = acc.and(r.getReturnValue().getSecond());
      }
      TransferReturn ret = new TransferReturn(p.getData(), acc);
      p.debug("Conjunction return: " + acc);
      return result.setReturnValue(ret);
    }

    if (expr instanceof Disjunction) {
      p.debug("Disjunction");
      Disjunction d = (Disjunction) expr;
      BDD acc = factory.zero();
      TransferResult<TransferReturn, BDD> result = new TransferResult<>();
      for (BooleanExpr be : d.getDisjuncts()) {
        TransferResult<TransferReturn, BDD> r = compute(be, p.indent());
        result = result.addChangedVariables(r);
        acc = acc.or(r.getReturnValue().getSecond());
      }
      TransferReturn ret = new TransferReturn(p.getData(), acc);
      p.debug("Disjunction return: " + acc);
      return result.setReturnValue(ret);
    }

    // TODO: thread the BDDRecord through calls
    if (expr instanceof ConjunctionChain) {
      p.debug("ConjunctionChain");
      ConjunctionChain d = (ConjunctionChain) expr;
      List<BooleanExpr> conjuncts = new ArrayList<>(d.getSubroutines());
      if (p.getDefaultPolicy() != null) {
        BooleanExpr be = new CallExpr(p.getDefaultPolicy().getDefaultPolicy());
        conjuncts.add(be);
      }
      if (conjuncts.isEmpty()) {
        TransferReturn ret = new TransferReturn(p.getData(), factory.one());
        return fromExpr(ret);
      } else {
        TransferResult<TransferReturn, BDD> result = new TransferResult<>();
        TransferParam<BDDRoute> record = p;
        BDD acc = factory.zero();
        for (int i = conjuncts.size() - 1; i >= 0; i--) {
          BooleanExpr conjunct = conjuncts.get(i);
          TransferParam<BDDRoute> param =
              record
                  .setDefaultPolicy(null)
                  .setChainContext(TransferParam.ChainContext.CONJUNCTION)
                  .indent();
          TransferResult<TransferReturn, BDD> r = compute(conjunct, param);
          record = record.setData(r.getReturnValue().getFirst());
          acc = ite(r.getFallthroughValue(), acc, r.getReturnValue().getSecond());
        }
        TransferReturn ret = new TransferReturn(record.getData(), acc);
        return result.setReturnValue(ret);
      }
    }

    if (expr instanceof FirstMatchChain) {
      p.debug("FirstMatchChain");
      FirstMatchChain chain = (FirstMatchChain) expr;
      List<BooleanExpr> chainPolicies = new ArrayList<>(chain.getSubroutines());
      if (p.getDefaultPolicy() != null) {
        BooleanExpr be = new CallExpr(p.getDefaultPolicy().getDefaultPolicy());
        chainPolicies.add(be);
      }
      if (chainPolicies.isEmpty()) {
        // No identity for an empty FirstMatchChain; default policy should always be set.
        throw new BatfishException("Default policy is not set");
      }
      TransferResult<TransferReturn, BDD> result = new TransferResult<>();
      TransferParam<BDDRoute> record = p;
      BDD acc = factory.zero();
      for (int i = chainPolicies.size() - 1; i >= 0; i--) {
        BooleanExpr policyMatcher = chainPolicies.get(i);
        TransferParam<BDDRoute> param =
            record
                .setDefaultPolicy(null)
                .setChainContext(TransferParam.ChainContext.CONJUNCTION)
                .indent();
        TransferResult<TransferReturn, BDD> r = compute(policyMatcher, param);
        record = record.setData(r.getReturnValue().getFirst());
        acc = ite(r.getFallthroughValue(), acc, r.getReturnValue().getSecond());
      }
      TransferReturn ret = new TransferReturn(record.getData(), acc);
      return result.setReturnValue(ret);
    }

    if (expr instanceof Not) {
      p.debug("mkNot");
      Not n = (Not) expr;
      TransferResult<TransferReturn, BDD> result = compute(n.getExpr(), p);
      TransferReturn r = result.getReturnValue();
      TransferReturn ret = new TransferReturn(r.getFirst(), r.getSecond().not());
      return result.setReturnValue(ret);
    }

    if (expr instanceof MatchProtocol) {
      MatchProtocol mp = (MatchProtocol) expr;
      Protocol proto = Protocol.fromRoutingProtocol(mp.getProtocol());
      if (proto == null) {
        p.debug("MatchProtocol(" + mp.getProtocol().protocolName() + "): false");
        TransferReturn ret = new TransferReturn(p.getData(), factory.zero());
        return fromExpr(ret);
      }
      BDD protoMatch = p.getData().getProtocolHistory().value(proto);
      p.debug("MatchProtocol(" + mp.getProtocol().protocolName() + "): " + protoMatch);
      TransferReturn ret = new TransferReturn(p.getData(), protoMatch);
      return fromExpr(ret);
    }

    if (expr instanceof MatchPrefixSet) {
      p.debug("MatchPrefixSet");
      MatchPrefixSet m = (MatchPrefixSet) expr;

      BDD r = matchPrefixSet(p.indent(), _conf, m.getPrefixSet(), p.getData());
      TransferReturn ret = new TransferReturn(p.getData(), r);
      return fromExpr(ret);

      // TODO: implement me
    } else if (expr instanceof MatchPrefix6Set) {
      p.debug("MatchPrefix6Set");
      TransferReturn ret = new TransferReturn(p.getData(), factory.zero());
      return fromExpr(ret);

    } else if (expr instanceof CallExpr) {
      p.debug("CallExpr");
      CallExpr c = (CallExpr) expr;
      String router = _conf.getHostname();
      String name = c.getCalledPolicyName();
      TransferResult<TransferReturn, BDD> r = CACHE.get(router, name);
      if (r != null) {
        return r;
      }
      RoutingPolicy pol = _conf.getRoutingPolicies().get(name);
      r =
          compute(
              pol.getStatements(),
              p.setCallContext(TransferParam.CallContext.EXPR_CALL).indent().enterScope(name));
      CACHE.put(router, name, r);
      return r;

    } else if (expr instanceof WithEnvironmentExpr) {
      p.debug("WithEnvironmentExpr");
      // TODO: this is not correct
      WithEnvironmentExpr we = (WithEnvironmentExpr) expr;
      // TODO: postStatements() and preStatements()
      return compute(we.getExpr(), p.deepCopy());

    } else if (expr instanceof MatchCommunitySet) {
      p.debug("MatchCommunitySet");
      MatchCommunitySet mcs = (MatchCommunitySet) expr;
      BDD c = matchCommunitySet(p.indent(), _conf, mcs.getExpr(), p.getData());
      TransferReturn ret = new TransferReturn(p.getData(), c);
      return fromExpr(ret);

    } else if (expr instanceof BooleanExprs.StaticBooleanExpr) {
      BooleanExprs.StaticBooleanExpr b = (BooleanExprs.StaticBooleanExpr) expr;
      TransferReturn ret;
      switch (b.getType()) {
        case CallExprContext:
          p.debug("CallExprContext");
          BDD x1 = mkBDD(p.getCallContext() == TransferParam.CallContext.EXPR_CALL);
          ret = new TransferReturn(p.getData(), x1);
          return fromExpr(ret);
        case CallStatementContext:
          p.debug("CallStmtContext");
          BDD x2 = mkBDD(p.getCallContext() == TransferParam.CallContext.STMT_CALL);
          ret = new TransferReturn(p.getData(), x2);
          return fromExpr(ret);
        case True:
          p.debug("True");
          ret = new TransferReturn(p.getData(), factory.one());
          return fromExpr(ret);
        case False:
          p.debug("False");
          ret = new TransferReturn(p.getData(), factory.zero());
          return fromExpr(ret);
        default:
          throw new BatfishException(
              "Unhandled " + BooleanExprs.class.getCanonicalName() + ": " + b.getType());
      }

    } else if (expr instanceof MatchAsPath) {
      p.debug("MatchAsPath");
      // System.out.println("Warning: use of unimplemented feature MatchAsPath");
      TransferReturn ret = new TransferReturn(p.getData(), factory.one());
      return fromExpr(ret);
    }

    throw new BatfishException("TODO: compute expr transfer function: " + expr);
  }

  /*
   * Convert a list of statements into a Z3 boolean expression for the transfer function.
   */
  private TransferResult<TransferReturn, BDD> compute(
      List<Statement> statements, TransferParam<BDDRoute> p) {
    TransferParam<BDDRoute> curP = p;
    boolean doesReturn = false;

    TransferResult<TransferReturn, BDD> result = new TransferResult<>();
    result =
        result
            .setReturnValue(new TransferReturn(curP.getData(), factory.zero()))
            .setFallthroughValue(factory.zero())
            .setReturnAssignedValue(factory.zero());

    for (Statement stmt : statements) {

      if (stmt instanceof StaticStatement) {
        StaticStatement ss = (StaticStatement) stmt;

        switch (ss.getType()) {
          case ExitAccept:
            doesReturn = true;
            curP.debug("ExitAccept");
            result = returnValue(result, true);
            break;

            // TODO: implement proper unsuppression of routes covered by aggregates
          case Unsuppress:
          case ReturnTrue:
            doesReturn = true;
            curP.debug("ReturnTrue");
            result = returnValue(result, true);
            break;

          case ExitReject:
            doesReturn = true;
            curP.debug("ExitReject");
            result = returnValue(result, false);
            break;

            // TODO: implement proper suppression of routes covered by aggregates
          case Suppress:
          case ReturnFalse:
            doesReturn = true;
            curP.debug("ReturnFalse");
            result = returnValue(result, false);
            break;

          case SetDefaultActionAccept:
            curP.debug("SetDefaulActionAccept");
            curP = curP.setDefaultAccept(true);
            break;

          case SetDefaultActionReject:
            curP.debug("SetDefaultActionReject");
            curP = curP.setDefaultAccept(false);
            break;

          case SetLocalDefaultActionAccept:
            curP.debug("SetLocalDefaultActionAccept");
            curP = curP.setDefaultAcceptLocal(true);
            break;

          case SetLocalDefaultActionReject:
            curP.debug("SetLocalDefaultActionReject");
            curP = curP.setDefaultAcceptLocal(false);
            break;

          case ReturnLocalDefaultAction:
            curP.debug("ReturnLocalDefaultAction");
            // TODO: need to set local default action in an environment
            if (curP.getDefaultAcceptLocal()) {
              result = returnValue(result, true);
            } else {
              result = returnValue(result, false);
            }
            break;

          case FallThrough:
            curP.debug("Fallthrough");
            result = fallthrough(result);
            break;

          case Return:
            // TODO: assumming this happens at the end of the function, so it is ignored for now.
            curP.debug("Return");
            break;

          case RemovePrivateAs:
            curP.debug("RemovePrivateAs");
            // System.out.println("Warning: use of unimplemented feature RemovePrivateAs");
            break;

          default:
            throw new BatfishException("TODO: computeTransferFunction: " + ss.getType());
        }

      } else if (stmt instanceof If) {
        curP.debug("If");
        If i = (If) stmt;
        TransferResult<TransferReturn, BDD> r = compute(i.getGuard(), curP.indent());
        BDD guard = r.getReturnValue().getSecond();
        curP.debug("guard: ");

        BDDRoute current = result.getReturnValue().getFirst();

        TransferParam<BDDRoute> pTrue = curP.indent().setData(current.deepCopy());
        TransferParam<BDDRoute> pFalse = curP.indent().setData(current.deepCopy());
        curP.debug("True Branch");
        TransferResult<TransferReturn, BDD> trueBranch = compute(i.getTrueStatements(), pTrue);
        curP.debug("True Branch: " + trueBranch.getReturnValue().getFirst().hashCode());
        curP.debug("False Branch");
        TransferResult<TransferReturn, BDD> falseBranch = compute(i.getFalseStatements(), pFalse);
        curP.debug("False Branch: " + trueBranch.getReturnValue().getFirst().hashCode());

        BDDRoute r1 = trueBranch.getReturnValue().getFirst();
        BDDRoute r2 = falseBranch.getReturnValue().getFirst();
        BDDRoute recordVal = ite(guard, r1, r2);

        // update return values
        BDD returnVal =
            ite(
                guard,
                trueBranch.getReturnValue().getSecond(),
                falseBranch.getReturnValue().getSecond());

        // p.debug("New Return Value (neg): " + returnVal.not());

        BDD returnAss =
            ite(guard, trueBranch.getReturnAssignedValue(), falseBranch.getReturnAssignedValue());

        // p.debug("New Return Assigned: " + returnAss);

        BDD fallThrough =
            ite(guard, trueBranch.getFallthroughValue(), falseBranch.getFallthroughValue());

        // p.debug("New fallthrough: " + fallThrough);

        result =
            result
                .setReturnValue(new TransferReturn(recordVal, returnVal))
                .setReturnAssignedValue(returnAss)
                .setFallthroughValue(fallThrough);

        curP.debug("If return: " + result.getReturnValue().getFirst().hashCode());

      } else if (stmt instanceof SetDefaultPolicy) {
        curP.debug("SetDefaultPolicy");
        curP = curP.setDefaultPolicy((SetDefaultPolicy) stmt);

      } else if (stmt instanceof SetMetric) {
        curP.debug("SetMetric");
        SetMetric sm = (SetMetric) stmt;
        LongExpr ie = sm.getMetric();
        BDD isBGP = curP.getData().getProtocolHistory().value(Protocol.BGP);
        BDD updateMed = isBGP.and(result.getReturnAssignedValue());
        BDD updateMet = isBGP.less(result.getReturnAssignedValue());
        BDDInteger newValue =
            applyLongExprModification(curP.indent(), curP.getData().getMetric(), ie);
        BDDInteger med = ite(updateMed, curP.getData().getMed(), newValue);
        BDDInteger met = ite(updateMet, curP.getData().getMetric(), newValue);
        curP.getData().setMetric(met);
        curP.getData().setMetric(med);

      } else if (stmt instanceof SetOspfMetricType) {
        curP.debug("SetOspfMetricType");
        SetOspfMetricType somt = (SetOspfMetricType) stmt;
        OspfMetricType mt = somt.getMetricType();
        BDDDomain<OspfType> current = result.getReturnValue().getFirst().getOspfMetric();
        BDDDomain<OspfType> newValue = new BDDDomain<>(current);
        if (mt == OspfMetricType.E1) {
          curP.indent().debug("Value: E1");
          newValue.setValue(OspfType.E1);
        } else {
          curP.indent().debug("Value: E2");
          newValue.setValue(OspfType.E1);
        }
        newValue = ite(result.getReturnAssignedValue(), curP.getData().getOspfMetric(), newValue);
        curP.getData().setOspfMetric(newValue);

      } else if (stmt instanceof SetLocalPreference) {
        curP.debug("SetLocalPreference");
        SetLocalPreference slp = (SetLocalPreference) stmt;
        LongExpr ie = slp.getLocalPreference();
        BDDInteger newValue =
            applyLongExprModification(curP.indent(), curP.getData().getLocalPref(), ie);
        newValue = ite(result.getReturnAssignedValue(), curP.getData().getLocalPref(), newValue);
        curP.getData().setLocalPref(newValue);

      } else if (stmt instanceof AddCommunity) {
        curP.debug("AddCommunity");
        AddCommunity ac = (AddCommunity) stmt;
        Set<CommunityVar> comms = collectCommunityVars(_conf, ac.getExpr());
        for (CommunityVar cvar : comms) {
          if (!_policyQuotient.getCommsAssignedButNotMatched().contains(cvar)) {
            curP.indent().debug("Value: " + cvar);
            BDD comm = curP.getData().getCommunities().get(cvar);
            BDD newValue = ite(result.getReturnAssignedValue(), comm, factory.one());
            curP.indent().debug("New Value: " + newValue);
            curP.getData().getCommunities().put(cvar, newValue);
          }
        }

      } else if (stmt instanceof SetCommunity) {
        curP.debug("SetCommunity");
        SetCommunity sc = (SetCommunity) stmt;
        Set<CommunityVar> comms = collectCommunityVars(_conf, sc.getExpr());
        for (CommunityVar cvar : comms) {
          if (!_policyQuotient.getCommsAssignedButNotMatched().contains(cvar)) {
            curP.indent().debug("Value: " + cvar);
            BDD comm = curP.getData().getCommunities().get(cvar);
            BDD newValue = ite(result.getReturnAssignedValue(), comm, factory.one());
            curP.indent().debug("New Value: " + newValue);
            curP.getData().getCommunities().put(cvar, newValue);
          }
        }

      } else if (stmt instanceof DeleteCommunity) {
        curP.debug("DeleteCommunity");
        DeleteCommunity ac = (DeleteCommunity) stmt;
        Set<CommunityVar> comms = collectCommunityVars(_conf, ac.getExpr());
        Set<CommunityVar> toDelete = new HashSet<>();
        // Find comms to delete
        for (CommunityVar cvar : comms) {
          if (cvar.getType() == Type.REGEX) {
            toDelete.addAll(_commDeps.get(cvar));
          } else {
            toDelete.add(cvar);
          }
        }
        // Delete the comms
        for (CommunityVar cvar : toDelete) {
          if (!_policyQuotient.getCommsAssignedButNotMatched().contains(cvar)) {
            curP.indent().debug("Value: " + cvar.getRegex() + ", " + cvar.getType());
            BDD comm = curP.getData().getCommunities().get(cvar);
            BDD newValue = ite(result.getReturnAssignedValue(), comm, factory.zero());
            curP.indent().debug("New Value: " + newValue);
            curP.getData().getCommunities().put(cvar, newValue);
          }
        }

      } else if (stmt instanceof RetainCommunity) {
        curP.debug("RetainCommunity");
        // no op

      } else if (stmt instanceof PrependAsPath) {
        curP.debug("PrependAsPath");
        PrependAsPath pap = (PrependAsPath) stmt;
        Integer prependCost = prependLength(pap.getExpr());
        curP.indent().debug("Cost: " + prependCost);
        BDDInteger met = curP.getData().getMetric();
        BDDInteger newValue = met.add(BDDInteger.makeFromValue(met.getFactory(), 32, prependCost));
        newValue = ite(result.getReturnAssignedValue(), curP.getData().getMetric(), newValue);
        curP.getData().setMetric(newValue);

      } else if (stmt instanceof SetOrigin) {
        curP.debug("SetOrigin");
        // System.out.println("Warning: use of unimplemented feature SetOrigin");
        // TODO: implement me

      } else if (stmt instanceof SetNextHop) {
        curP.debug("SetNextHop");
        // System.out.println("Warning: use of unimplemented feature SetNextHop");
        // TODO: implement me

      } else {
        throw new BatfishException("TODO: statement transfer function: " + stmt);
      }
    }

    // If this is the outermost call, then we relate the variables
    if (curP.getInitialCall()) {
      curP.debug("InitialCall finalizing");
      // Apply the default action
      if (!doesReturn) {
        curP.debug("Applying default action: " + curP.getDefaultAccept());
        if (curP.getDefaultAccept()) {
          result = returnValue(result, true);
        } else {
          result = returnValue(result, false);
        }
      }

      // Set all the values to 0 if the return is not true;
      TransferReturn ret = result.getReturnValue();
      BDDRoute retVal = ite(ret.getSecond(), ret.getFirst(), zeroedRecord());
      result = result.setReturnValue(new TransferReturn(retVal, ret.getSecond()));
    }
    return result;
  }

  private TransferResult<TransferReturn, BDD> fallthrough(TransferResult<TransferReturn, BDD> r) {
    BDD b = ite(r.getReturnAssignedValue(), r.getFallthroughValue(), factory.one());
    return r.setFallthroughValue(b).setReturnAssignedValue(factory.one());
  }

  /*
   * Wrap a simple boolean expression return value in a transfer function result
   */
  private TransferResult<TransferReturn, BDD> fromExpr(TransferReturn b) {
    return new TransferResult<TransferReturn, BDD>()
        .setReturnAssignedValue(factory.one())
        .setReturnValue(b);
  }

  /*
   * Check if a prefix range match is applicable for the packet destination
   * Ip address, given the prefix length variable.
   *
   * Since aggregation is modelled separately, we assume that prefixLen
   * is not modified, and thus will contain only the underlying variables:
   * [var(0), ..., var(n)]
   */
  static BDD isRelevantFor(BDDRoute record, PrefixRange range) {
    Prefix p = range.getPrefix();
    BDD prefixMatch = firstBitsEqual(record.getPrefix().getBitvec(), p, p.getPrefixLength());

    BDDInteger prefixLength = record.getPrefixLength();
    SubRange r = range.getLengthRange();
    int lower = r.getStart();
    int upper = r.getEnd();
    BDD lenMatch =
        lower == upper
            ? prefixLength.value(lower)
            : prefixLength.geq(lower).and(prefixLength.leq(upper));

    return lenMatch.and(prefixMatch);
  }

  /*
   * If-then-else statement
   */
  private BDD ite(BDD b, BDD x, BDD y) {
    return b.ite(x, y);
  }

  /*
   * Map ite over BDDInteger type
   */
  private BDDInteger ite(BDD b, BDDInteger x, BDDInteger y) {
    return x.ite(b, y);
  }

  /*
   * Map ite over BDDDomain type
   */
  private <T> BDDDomain<T> ite(BDD b, BDDDomain<T> x, BDDDomain<T> y) {
    BDDDomain<T> result = new BDDDomain<>(x);
    BDDInteger i = ite(b, x.getInteger(), y.getInteger());
    result.setInteger(i);
    return result;
  }

  private BDDRoute ite(BDD guard, BDDRoute r1, BDDRoute r2) {
    BDDRoute ret = new BDDRoute(_comms);

    BDDInteger x;
    BDDInteger y;

    // update integer values based on condition
    // x = r1.getPrefixLength();
    // y = r2.getPrefixLength();
    // ret.getPrefixLength().setValue(ite(guard, x, y));

    // x = r1.getIp();
    // y = r2.getIp();
    // ret.getIp().setValue(ite(guard, x, y));

    x = r1.getAdminDist();
    y = r2.getAdminDist();
    ret.getAdminDist().setValue(ite(guard, x, y));

    x = r1.getLocalPref();
    y = r2.getLocalPref();
    ret.getLocalPref().setValue(ite(guard, x, y));

    x = r1.getMetric();
    y = r2.getMetric();
    ret.getMetric().setValue(ite(guard, x, y));

    x = r1.getMed();
    y = r2.getMed();
    ret.getMed().setValue(ite(guard, x, y));

    r1.getCommunities()
        .forEach(
            (c, var1) -> {
              BDD var2 = r2.getCommunities().get(c);
              ret.getCommunities().put(c, ite(guard, var1, var2));
            });

    // BDDInteger i =
    //    ite(guard, r1.getProtocolHistory().getInteger(), r2.getProtocolHistory().getInteger());
    // ret.getProtocolHistory().setInteger(i);

    return ret;
  }

  /*
   * Converts a community list to a boolean expression.
   */
  private BDD matchCommunityList(TransferParam<BDDRoute> p, CommunityList cl, BDDRoute other) {
    List<CommunityListLine> lines = new ArrayList<>(cl.getLines());
    Collections.reverse(lines);
    BDD acc = factory.zero();
    for (CommunityListLine line : lines) {
      boolean action = (line.getAction() == LineAction.PERMIT);
      CommunityVar cvar = toRegexCommunityVar(toCommunityVar(line.getMatchCondition()));
      p.debug("Match Line: " + cvar);
      p.debug("Action: " + line.getAction());
      // Skip this match if it is irrelevant
      if (_policyQuotient.getCommsMatchedButNotAssigned().contains(cvar)) {
        continue;
      }
      List<CommunityVar> deps = _commDeps.get(cvar);
      for (CommunityVar dep : deps) {
        p.debug("Test for: " + dep);
        BDD c = other.getCommunities().get(dep);
        acc = ite(c, mkBDD(action), acc);
      }
    }
    return acc;
  }

  /*
   * Converts a community set to a boolean expression
   */
  private BDD matchCommunitySet(
      TransferParam<BDDRoute> p, Configuration conf, CommunitySetExpr e, BDDRoute other) {

    if (e instanceof CommunityList) {
      Set<CommunityVar> comms =
          ((CommunityList) e)
              .getLines().stream()
                  .map(line -> toCommunityVar(line.getMatchCondition()))
                  .collect(Collectors.toSet());
      BDD acc = factory.one();
      for (CommunityVar comm : comms) {
        p.debug("Inline Community Set: " + comm);
        BDD c = other.getCommunities().get(comm);
        if (c == null) {
          throw new BatfishException("matchCommunitySet: should not be null");
        }
        acc = acc.and(c);
      }
      return acc;
    }

    if (e instanceof NamedCommunitySet) {
      p.debug("Named");
      NamedCommunitySet x = (NamedCommunitySet) e;
      CommunityList cl = conf.getCommunityLists().get(x.getName());
      p.debug("Named Community Set: " + cl.getName());
      return matchCommunityList(p, cl, other);
    }

    throw new BatfishException("TODO: match community set");
  }

  /*
   * Converts a route filter list to a boolean expression.
   */
  private BDD matchFilterList(TransferParam<BDDRoute> p, RouteFilterList x, BDDRoute other) {
    BDD acc = factory.zero();
    List<RouteFilterLine> lines = new ArrayList<>(x.getLines());
    Collections.reverse(lines);
    for (RouteFilterLine line : lines) {
      if (!line.getIpWildcard().isPrefix()) {
        throw new BatfishException("non-prefix IpWildcards are unsupported");
      }
      Prefix pfx = line.getIpWildcard().toPrefix();
      if (!PrefixUtils.isContainedBy(pfx, _ignoredNetworks)) {
        SubRange r = line.getLengthRange();
        PrefixRange range = new PrefixRange(pfx, r);
        p.debug("Prefix Range: " + range);
        p.debug("Action: " + line.getAction());
        BDD matches = isRelevantFor(other, range);
        BDD action = mkBDD(line.getAction() == LineAction.PERMIT);
        acc = ite(matches, action, acc);
      }
    }
    return acc;
  }

  /*
   * Converts a prefix set to a boolean expression.
   */
  private BDD matchPrefixSet(
      TransferParam<BDDRoute> p, Configuration conf, PrefixSetExpr e, BDDRoute other) {
    if (e instanceof ExplicitPrefixSet) {
      ExplicitPrefixSet x = (ExplicitPrefixSet) e;

      Set<PrefixRange> ranges = x.getPrefixSpace().getPrefixRanges();
      if (ranges.isEmpty()) {
        p.debug("empty");
        return factory.one();
      }

      BDD acc = factory.zero();
      for (PrefixRange range : ranges) {
        p.debug("Prefix Range: " + range);
        if (!PrefixUtils.isContainedBy(range.getPrefix(), _ignoredNetworks)) {
          acc = acc.or(isRelevantFor(other, range));
        }
      }
      return acc;

    } else if (e instanceof NamedPrefixSet) {
      NamedPrefixSet x = (NamedPrefixSet) e;
      p.debug("Named: " + x.getName());
      String name = x.getName();
      RouteFilterList fl = conf.getRouteFilterLists().get(name);
      return matchFilterList(p, fl, other);

    } else {
      throw new BatfishException("TODO: match prefix set: " + e);
    }
  }

  /*
   * Return a BDD from a boolean
   */
  private BDD mkBDD(boolean b) {
    return b ? factory.one() : factory.zero();
  }

  /*
   * Compute how many times to prepend to a path from the AST
   */
  private int prependLength(AsPathListExpr expr) {
    if (expr instanceof MultipliedAs) {
      MultipliedAs x = (MultipliedAs) expr;
      IntExpr e = x.getNumber();
      LiteralInt i = (LiteralInt) e;
      return i.getValue();
    }
    if (expr instanceof LiteralAsList) {
      LiteralAsList x = (LiteralAsList) expr;
      return x.getList().size();
    }
    throw new BatfishException("Error[prependLength]: unreachable");
  }

  /*
   * Create a new variable reflecting the final return value of the function
   */
  private TransferResult<TransferReturn, BDD> returnValue(
      TransferResult<TransferReturn, BDD> r, boolean val) {
    BDD b = ite(r.getReturnAssignedValue(), r.getReturnValue().getSecond(), mkBDD(val));
    TransferReturn ret = new TransferReturn(r.getReturnValue().getFirst(), b);
    return r.setReturnValue(ret).setReturnAssignedValue(factory.one());
  }

  /*
   * A record of default values that represent the value of the
   * outputs if the route is filtered / dropped in the policy
   */
  private BDDRoute zeroedRecord() {
    BDDRoute rec = new BDDRoute(_comms);
    rec.getMetric().setValue(0);
    rec.getLocalPref().setValue(0);
    rec.getAdminDist().setValue(0);
    rec.getPrefixLength().setValue(0);
    rec.getMed().setValue(0);
    rec.getPrefix().setValue(0);
    for (CommunityVar comm : _comms) {
      rec.getCommunities().put(comm, factory.zero());
    }
    rec.getProtocolHistory().getInteger().setValue(0);
    return rec;
  }

  /*
   * Communities assumed to not be attached
   */
  private void addCommunityAssumptions(BDDRoute route) {
    for (CommunityVar comm : _comms) {
      if (_policyQuotient.getCommsUsedOnlyLocally().contains(comm)) {
        route.getCommunities().put(comm, factory.zero());
      }
    }
  }

  /*
   * Create a BDDRecord representing the symbolic output of
   * the RoutingPolicy given the input variables.
   */
  public BDDRoute compute(@Nullable Set<Prefix> ignoredNetworks) {
    _ignoredNetworks = ignoredNetworks;
    _commDeps = _graph.getCommunityDependencies();
    _comms = _graph.getAllCommunities();
    BDDRoute o = new BDDRoute(_comms);
    addCommunityAssumptions(o);
    TransferParam<BDDRoute> p = new TransferParam<>(o, false);
    TransferResult<TransferReturn, BDD> result = compute(_statements, p);
    // BDDRoute route = result.getReturnValue().getFirst();
    // System.out.println("DOT: \n" + route.dot(route.getLocalPref().getBitvec()[31]));
    return result.getReturnValue().getFirst();
  }

  /*
   * Convert EXACT community vars to their REGEX equivalents.
   */
  private static CommunityVar toRegexCommunityVar(CommunityVar cvar) {
    switch (cvar.getType()) {
      case REGEX:
        return cvar;
      case EXACT:
        assert cvar.getLiteralValue() != null; // invariant of the EXACT type
        return CommunityVar.from(String.format("^%s$", cvar.getLiteralValue().matchString()));
      default:
        throw new BatfishException("Unexpected CommunityVar type: " + cvar.getType());
    }
  }
}
