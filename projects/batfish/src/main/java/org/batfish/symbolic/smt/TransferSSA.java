package org.batfish.symbolic.smt;

import static org.batfish.symbolic.CommunityVarCollector.collectCommunityVars;
import static org.batfish.symbolic.bdd.CommunityVarConverter.toCommunityVar;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
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
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.OspfType;
import org.batfish.symbolic.Protocol;
import org.batfish.symbolic.TransferParam;
import org.batfish.symbolic.TransferResult;
import org.batfish.symbolic.collections.PList;

/**
 * Class that computes a symbolic transfer function between two symbolic control plane records. The
 * transfer function is used to encode both import and export filters.
 *
 * <p>Batfish represents the AST much like vendors where there is a simple imperative language for
 * matching fields and making modifications to fields. Since this is not a good fit for a
 * declarative symbolic encoding of the network, we convert this stateful representation into a
 * stateless representation
 *
 * <p>The TransferSSA class makes policies stateless by converting the vendor-independent format to
 * a Static Single Assignment (SSA) form where all updates are reflected in new variables. Rather
 * than create a full control flow graph (CFG) as is typically done in SSA, we use a simple
 * conversion based on adding join points for every variable modified in an if statement.
 *
 * <p>The joint point defined as the [phi] function from SSA merges variables that may differ across
 * different branches of an if statement. For example, if there is the following filter:
 *
 * <p>if match(c1) then add community c2 else prepend path 2
 *
 * <p>Then this function will introduce a new variable at the end of the if statement that updates
 * the value of each variable modified based on the branch taken. For example:
 *
 * <p>c2' = (c1 ? true : c2) metric' = (c1 ? metric : metric + 2)
 *
 * <p>To model the return value of functions, we introduce three new variables: [fallthrough],
 * [returnValue] and [returnAssigned]. For example, if we have the following AST function in
 * Batfish:
 *
 * <p>function foo() if match(c1) then reject accept
 *
 * <p>This is modeled by introducing [returnValue] - the value that the function returns, and the
 * [returnAssigned] variable - whether a return or fallthrough statement has been hit so far in the
 * control flow.
 *
 * <p>Naturally, this kind of encoding can grow quite large since we introduce a large number of
 * extra variables. To make formula much simpler, we use a term size heuristic to inline variable
 * equalities when the inlined term will not be too large. Thus, additional variables are still
 * introduced, but only to keep the encoding compact. The 'simplify' and 'propagate-values' tactics
 * for z3 will further improve the encoding by removing any unnecessary variables. In this example,
 * the encoding will be simplified to [returnValue''' = not c1], which removes all intermediate
 * variables
 *
 * @author Ryan Beckett
 */
class TransferSSA {

  private static final int INLINE_HEURISTIC = 3000;

  private static int id = 0;

  private EncoderSlice _enc;

  private Configuration _conf;

  private SymbolicRoute _current;

  private SymbolicRoute _other;

  private Protocol _proto;

  private List<Statement> _statements;

  private Integer _addedCost;

  private Interface _iface;

  private GraphEdge _graphEdge;

  private Map<Prefix, Boolean> _aggregates;

  private boolean _isExport;

  TransferSSA(
      EncoderSlice encoderSlice,
      Configuration conf,
      SymbolicRoute other,
      SymbolicRoute current,
      Protocol proto,
      List<Statement> statements,
      Integer addedCost,
      GraphEdge ge,
      boolean isExport) {
    _enc = encoderSlice;
    _conf = conf;
    _current = current;
    _other = other;
    _proto = proto;
    _statements = statements;
    _addedCost = addedCost;
    _graphEdge = ge;
    _iface = ge.getStart();
    _isExport = isExport;
    _aggregates = null;
  }

  /*
   * Returns and increments a unique id for adding additional SSA variables
   */
  private static int generateId() {
    int result = TransferSSA.id;
    TransferSSA.id = result + 1;
    return result;
  }

  /*
   * Determines whether to model each aggregate route as
   * suppressing a more specific, or including the more specific
   */
  private Map<Prefix, Boolean> aggregateRoutes() {
    Map<Prefix, Boolean> acc = new HashMap<>();
    String name = _conf.getHostname();
    List<GeneratedRoute> aggregates = _enc.getOptimizations().getRelevantAggregates().get(name);
    Set<Prefix> suppressed = _enc.getOptimizations().getSuppressedAggregates().get(name);
    for (GeneratedRoute gr : aggregates) {
      Prefix p = gr.getNetwork();
      acc.put(p, suppressed.contains(p));
    }
    return acc;
  }

  /*
   * Converts a route filter list to a boolean expression.
   */
  private BoolExpr matchFilterList(RouteFilterList x, SymbolicRoute other) {
    BoolExpr acc = _enc.mkFalse();

    List<RouteFilterLine> lines = new ArrayList<>(x.getLines());
    Collections.reverse(lines);

    for (RouteFilterLine line : lines) {
      if (!line.getIpWildcard().isPrefix()) {
        throw new BatfishException("non-prefix IpWildcards are unsupported");
      }
      Prefix p = line.getIpWildcard().toPrefix();
      SubRange r = line.getLengthRange();
      PrefixRange range = new PrefixRange(p, r);
      BoolExpr matches = _enc.isRelevantFor(other.getPrefixLength(), range);
      BoolExpr action = _enc.mkBool(line.getAction() == LineAction.PERMIT);
      acc = _enc.mkIf(matches, action, acc);
    }
    return acc;
  }

  /*
   * Converts a prefix set to a boolean expression.
   */
  private TransferResult<BoolExpr, BoolExpr> matchPrefixSet(
      Configuration conf, PrefixSetExpr e, SymbolicRoute other) {

    ArithExpr otherLen = other.getPrefixLength();

    TransferResult<BoolExpr, BoolExpr> result = new TransferResult<>();

    if (e instanceof ExplicitPrefixSet) {
      ExplicitPrefixSet x = (ExplicitPrefixSet) e;

      Set<PrefixRange> ranges = x.getPrefixSpace().getPrefixRanges();
      if (ranges.isEmpty()) {
        return result.setReturnValue(_enc.mkTrue());
      }

      // This is a total hack to deal with the fact that
      // we keep only a single FIB entry. Since BGP exporting a network
      // depends on the existence of an IGP route, we become more precise
      // by checking for static/connected/OSPF routes specifically.
      if (ranges.size() == 1) {
        for (PrefixRange r : ranges) {
          int start = r.getLengthRange().getStart();
          int end = r.getLengthRange().getEnd();
          Prefix pfx = r.getPrefix();
          if (start == end && start == pfx.getPrefixLength()) {
            String router = _conf.getHostname();
            Set<Prefix> origin = _enc.getOriginatedNetworks().get(router, Protocol.BGP);
            if (origin != null && origin.contains(pfx)) {
              // Compute static and connected routes
              Set<Prefix> ostatic = _enc.getOriginatedNetworks().get(router, Protocol.STATIC);
              Set<Prefix> oconn = _enc.getOriginatedNetworks().get(router, Protocol.CONNECTED);
              boolean hasStatic = ostatic != null && ostatic.contains(pfx);
              boolean hasConnected = oconn != null && oconn.contains(pfx);
              ArithExpr originLength = _enc.mkInt(pfx.getPrefixLength());
              if (hasStatic || hasConnected) {
                BoolExpr directRoute = _enc.isRelevantFor(originLength, r);
                ArithExpr newLength = _enc.mkIf(directRoute, originLength, otherLen);
                result = result.addChangedVariable("PREFIX-LEN", newLength);
                return result.setReturnValue(directRoute);
              } else {
                // Also use network statement if OSPF has a route with the correct length
                SymbolicRoute rec = _enc.getBestNeighborPerProtocol(router, Protocol.OSPF);
                if (rec != null) {
                  BoolExpr ospfRelevant = _enc.isRelevantFor(rec.getPrefixLength(), r);
                  ArithExpr newLength = _enc.mkIf(ospfRelevant, originLength, otherLen);
                  result = result.addChangedVariable("PREFIX-LEN", newLength);
                  return result.setReturnValue(ospfRelevant);
                }
              }
            }
          }
        }
      }

      // Compute if the other best route is relevant for this match statement
      BoolExpr acc = _enc.mkFalse();
      for (PrefixRange range : ranges) {
        acc = _enc.mkOr(acc, _enc.isRelevantFor(otherLen, range));
      }

      return result.setReturnValue(acc);

    } else if (e instanceof NamedPrefixSet) {
      NamedPrefixSet x = (NamedPrefixSet) e;
      String name = x.getName();
      RouteFilterList fl = conf.getRouteFilterLists().get(name);
      return result.setReturnValue(matchFilterList(fl, other));

    } else {
      throw new BatfishException("TODO: match prefix set: " + e);
    }
  }

  /*
   * Converts a community list to a boolean expression.
   */
  private BoolExpr matchCommunityList(CommunityList cl, SymbolicRoute other) {
    List<CommunityListLine> lines = new ArrayList<>(cl.getLines());
    Collections.reverse(lines);
    BoolExpr acc = _enc.mkFalse();
    for (CommunityListLine line : lines) {
      boolean action = (line.getAction() == LineAction.PERMIT);
      CommunityVar cvar = toCommunityVar(line.getMatchCondition());
      BoolExpr c = other.getCommunities().get(cvar);
      acc = _enc.mkIf(c, _enc.mkBool(action), acc);
    }
    return acc;
  }

  /*
   * Converts a community set to a boolean expression
   */
  private BoolExpr matchCommunitySet(Configuration conf, CommunitySetExpr e, SymbolicRoute other) {
    if (e instanceof CommunityList) {
      Set<CommunityVar> comms = collectCommunityVars(conf, e);
      BoolExpr acc = _enc.mkTrue();
      for (CommunityVar comm : comms) {
        BoolExpr c = other.getCommunities().get(comm);
        if (c == null) {
          throw new BatfishException("matchCommunitySet: should not be null");
        }
        acc = _enc.mkAnd(acc, c);
      }
      return acc;
    }

    if (e instanceof NamedCommunitySet) {
      NamedCommunitySet x = (NamedCommunitySet) e;
      CommunityList cl = conf.getCommunityLists().get(x.getName());
      return matchCommunityList(cl, other);
    }

    throw new BatfishException("TODO: match community set");
  }

  /*
   * Wrap a simple boolean expression return value in a transfer function result
   */
  private TransferResult<BoolExpr, BoolExpr> fromExpr(BoolExpr b) {
    return new TransferResult<BoolExpr, BoolExpr>()
        .setReturnAssignedValue(_enc.mkTrue())
        .setReturnValue(b);
  }

  private TransferResult<BoolExpr, BoolExpr> initialResult() {
    return new TransferResult<BoolExpr, BoolExpr>()
        .setReturnValue(_enc.mkFalse())
        .setFallthroughValue(_enc.mkFalse())
        .setReturnAssignedValue(_enc.mkFalse());
  }

  /*
   * Convert a Batfish AST boolean expression to a symbolic Z3 boolean expression
   * by performing inlining of stateful side effects.
   */
  private TransferResult<BoolExpr, BoolExpr> compute(
      BooleanExpr expr, TransferParam<SymbolicRoute> p) {
    TransferParam<SymbolicRoute> pCur = p;
    // TODO: right now everything is IPV4
    if (expr instanceof MatchIpv4) {
      pCur.debug("MatchIpv4");
      return fromExpr(_enc.mkTrue());
    }
    if (expr instanceof MatchIpv6) {
      pCur.debug("MatchIpv6");
      return fromExpr(_enc.mkFalse());
    }

    if (expr instanceof Conjunction) {
      pCur.debug("Conjunction");
      Conjunction c = (Conjunction) expr;
      BoolExpr acc = _enc.mkTrue();
      TransferResult<BoolExpr, BoolExpr> result = new TransferResult<>();
      for (BooleanExpr be : c.getConjuncts()) {
        TransferResult<BoolExpr, BoolExpr> r = compute(be, pCur.indent());
        result = result.addChangedVariables(r);
        acc = _enc.mkAnd(acc, r.getReturnValue());
      }
      pCur.debug("has changed variable");
      return result.setReturnValue(acc);
    }

    if (expr instanceof Disjunction) {
      pCur.debug("Disjunction");
      Disjunction d = (Disjunction) expr;
      BoolExpr acc = _enc.mkFalse();
      TransferResult<BoolExpr, BoolExpr> result = new TransferResult<>();
      for (BooleanExpr be : d.getDisjuncts()) {
        TransferResult<BoolExpr, BoolExpr> r = compute(be, pCur.indent());
        result = result.addChangedVariables(r);
        acc = _enc.mkOr(acc, r.getReturnValue());
      }
      pCur.debug("has changed variable");
      return result.setReturnValue(acc);
    }

    if (expr instanceof ConjunctionChain) {
      pCur.debug("ConjunctionChain");
      ConjunctionChain d = (ConjunctionChain) expr;
      List<BooleanExpr> conjuncts = new ArrayList<>(d.getSubroutines());
      if (pCur.getDefaultPolicy() != null) {
        BooleanExpr be = new CallExpr(pCur.getDefaultPolicy().getDefaultPolicy());
        conjuncts.add(be);
      }
      if (conjuncts.isEmpty()) {
        return fromExpr(_enc.mkTrue());
      } else {
        TransferResult<BoolExpr, BoolExpr> result = new TransferResult<>();
        BoolExpr acc = _enc.mkFalse();
        for (int i = conjuncts.size() - 1; i >= 0; i--) {
          BooleanExpr conjunct = conjuncts.get(i);
          TransferParam<SymbolicRoute> param =
              pCur.setDefaultPolicy(null).setChainContext(TransferParam.ChainContext.CONJUNCTION);
          TransferResult<BoolExpr, BoolExpr> r = compute(conjunct, param);
          result = result.addChangedVariables(r);
          acc = _enc.mkIf(r.getFallthroughValue(), acc, r.getReturnValue());
        }
        pCur.debug("ConjunctionChain Result: " + acc);
        return result.setReturnValue(acc);
      }
    }

    if (expr instanceof FirstMatchChain) {
      pCur.debug("FirstMatchChain");
      FirstMatchChain chain = (FirstMatchChain) expr;
      List<BooleanExpr> chainPolicies = new ArrayList<>(chain.getSubroutines());
      if (pCur.getDefaultPolicy() != null) {
        BooleanExpr be = new CallExpr(pCur.getDefaultPolicy().getDefaultPolicy());
        chainPolicies.add(be);
      }
      if (chainPolicies.isEmpty()) {
        // No identity for an empty FirstMatchChain; default policy should always be set.
        throw new BatfishException("Default policy is not set");
      }
      TransferResult<BoolExpr, BoolExpr> result = new TransferResult<>();
      BoolExpr acc = _enc.mkFalse();
      for (int i = chainPolicies.size() - 1; i >= 0; i--) {
        BooleanExpr policyMatcher = chainPolicies.get(i);
        TransferParam<SymbolicRoute> param =
            pCur.setDefaultPolicy(null).setChainContext(TransferParam.ChainContext.CONJUNCTION);
        TransferResult<BoolExpr, BoolExpr> r = compute(policyMatcher, param);
        result = result.addChangedVariables(r);
        acc = _enc.mkIf(r.getFallthroughValue(), acc, r.getReturnValue());
      }
      pCur.debug("FirstMatchChain Result: " + acc);
      return result.setReturnValue(acc);
    }

    if (expr instanceof Not) {
      pCur.debug("mkNot");
      Not n = (Not) expr;
      TransferResult<BoolExpr, BoolExpr> result = compute(n.getExpr(), pCur);
      return result.setReturnValue(_enc.mkNot(result.getReturnValue()));
    }

    if (expr instanceof MatchProtocol) {
      MatchProtocol mp = (MatchProtocol) expr;
      Protocol proto = Protocol.fromRoutingProtocol(mp.getProtocol());
      if (proto == null) {
        pCur.debug("MatchProtocol(" + mp.getProtocol().protocolName() + "): false");
        return fromExpr(_enc.mkFalse());
      }
      if (_other.getProtocolHistory() == null) {
        BoolExpr protoMatch = _enc.mkBool(proto.equals(_proto));
        pCur.debug("MatchProtocol(" + mp.getProtocol().protocolName() + "): " + protoMatch);
        return fromExpr(protoMatch);
      }
      BoolExpr protoMatch = _other.getProtocolHistory().checkIfValue(proto);
      pCur.debug("MatchProtocol(" + mp.getProtocol().protocolName() + "): " + protoMatch);
      return fromExpr(protoMatch);
    }

    if (expr instanceof MatchPrefixSet) {
      pCur.debug("MatchPrefixSet");
      MatchPrefixSet m = (MatchPrefixSet) expr;
      // For BGP, may change prefix length
      TransferResult<BoolExpr, BoolExpr> result =
          matchPrefixSet(_conf, m.getPrefixSet(), pCur.getData());
      return result.setReturnAssignedValue(_enc.mkTrue());

      // TODO: implement me
    } else if (expr instanceof MatchPrefix6Set) {
      pCur.debug("MatchPrefix6Set");
      return fromExpr(_enc.mkFalse());

    } else if (expr instanceof CallExpr) {
      pCur.debug("CallExpr");
      // TODO: the call can modify certain fields, need to keep track of these variables
      CallExpr c = (CallExpr) expr;
      String name = c.getCalledPolicyName();
      RoutingPolicy pol = _conf.getRoutingPolicies().get(name);
      pCur = pCur.setCallContext(TransferParam.CallContext.EXPR_CALL);
      TransferResult<BoolExpr, BoolExpr> r =
          compute(pol.getStatements(), pCur.indent().enterScope(name), initialResult());
      pCur.debug("CallExpr (return): " + r.getReturnValue());
      pCur.debug("CallExpr (fallthrough): " + r.getFallthroughValue());
      return r;

    } else if (expr instanceof WithEnvironmentExpr) {
      pCur.debug("WithEnvironmentExpr");
      // TODO: this is not correct
      WithEnvironmentExpr we = (WithEnvironmentExpr) expr;
      // TODO: postStatements() and preStatements()
      return compute(we.getExpr(), pCur);

    } else if (expr instanceof MatchCommunitySet) {
      pCur.debug("MatchCommunitySet");
      MatchCommunitySet mcs = (MatchCommunitySet) expr;
      return fromExpr(matchCommunitySet(_conf, mcs.getExpr(), pCur.getData()));

    } else if (expr instanceof BooleanExprs.StaticBooleanExpr) {
      BooleanExprs.StaticBooleanExpr b = (BooleanExprs.StaticBooleanExpr) expr;
      switch (b.getType()) {
        case CallExprContext:
          pCur.debug("CallExprContext");
          return fromExpr(
              _enc.mkBool(pCur.getCallContext() == TransferParam.CallContext.EXPR_CALL));
        case CallStatementContext:
          pCur.debug("CallStmtContext");
          return fromExpr(
              _enc.mkBool(pCur.getCallContext() == TransferParam.CallContext.STMT_CALL));
        case True:
          pCur.debug("True");
          return fromExpr(_enc.mkTrue());
        case False:
          pCur.debug("False");
          return fromExpr(_enc.mkFalse());
        default:
          throw new BatfishException(
              "Unhandled " + BooleanExprs.class.getCanonicalName() + ": " + b.getType());
      }
    } else if (expr instanceof MatchAsPath) {
      pCur.debug("MatchAsPath");
      System.out.println("Warning: use of unimplemented feature MatchAsPath");
      return fromExpr(_enc.mkFalse());
    }

    String s = (_isExport ? "export" : "import");
    String msg =
        String.format(
            "Unimplemented feature %s for %s transfer function on interface %s",
            expr.toString(), s, _graphEdge.toString());

    throw new BatfishException(msg);
  }

  /*
   * Deal with the possibility of null variables due to optimizations
   */
  private ArithExpr getOrDefault(ArithExpr x, ArithExpr d) {
    if (x != null) {
      return x;
    }
    return d;
  }

  /*
   * Apply the effect of modifying a long value (e.g., to set the metric)
   */
  private ArithExpr applyLongExprModification(ArithExpr x, LongExpr e) {
    if (e instanceof LiteralLong) {
      LiteralLong z = (LiteralLong) e;
      return _enc.mkInt(z.getValue());
    }
    if (e instanceof DecrementMetric) {
      DecrementMetric z = (DecrementMetric) e;
      return _enc.mkSub(x, _enc.mkInt(z.getSubtrahend()));
    }
    if (e instanceof IncrementMetric) {
      IncrementMetric z = (IncrementMetric) e;
      return _enc.mkSum(x, _enc.mkInt(z.getAddend()));
    }
    if (e instanceof IncrementLocalPreference) {
      IncrementLocalPreference z = (IncrementLocalPreference) e;
      return _enc.mkSum(x, _enc.mkInt(z.getAddend()));
    }
    if (e instanceof DecrementLocalPreference) {
      DecrementLocalPreference z = (DecrementLocalPreference) e;
      return _enc.mkSub(x, _enc.mkInt(z.getSubtrahend()));
    }
    throw new BatfishException("int expr transfer function: " + e);
  }

  /*
   * Create a constraint that the metric field does not overflow
   * for a given routing protocol.
   */
  private BoolExpr noOverflow(ArithExpr metric, Protocol proto) {
    if (!_enc.getEncoder().getQuestion().getModelOverflow()) {
      return _enc.mkTrue();
    }
    if (proto.isConnected()) {
      return _enc.mkTrue();
    }
    if (proto.isStatic()) {
      return _enc.mkTrue();
    }
    if (proto.isOspf()) {
      return _enc.mkLe(metric, _enc.mkInt(65535));
    }
    if (proto.isBgp()) {
      return _enc.mkLe(metric, _enc.mkInt(255));
    }
    throw new BatfishException("Encoding[noOverflow]: unrecognized protocol: " + proto.name());
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
   * Get the BgpPeerConfig object given the current
   * graph edge and protocol information
   */
  private BgpPeerConfig getBgpNeighbor() {
    Graph g = _enc.getGraph();
    if (_graphEdge.isAbstract()) {
      return g.getIbgpNeighbors().get(_graphEdge);
    } else {
      return g.getEbgpNeighbors().get(_graphEdge);
    }
  }

  /*
   * Determine if BGP communities should be
   * sent to/from the neighboring BGP peer.
   */
  private boolean sendCommunity() {
    if (_proto.isBgp()) {
      if (!_isExport) {
        return true;
      }
      BgpPeerConfig n = getBgpNeighbor();
      return n.getSendCommunity();
    } else {
      return false;
    }
  }

  /*
   * Relate the symbolic control plane route variables
   */
  private BoolExpr relateVariables(
      TransferParam<SymbolicRoute> p, TransferResult<BoolExpr, BoolExpr> result) {

    ArithExpr defaultLen = _enc.mkInt(_enc.defaultLength());
    ArithExpr defaultAd = _enc.defaultAdminDistance(_conf, _proto, p.getData());
    ArithExpr defaultMed = _enc.mkInt(_enc.defaultMed(_proto));
    ArithExpr defaultLp = _enc.mkInt(_enc.defaultLocalPref());
    ArithExpr defaultId = _enc.mkInt(_enc.defaultId());
    ArithExpr defaultMet = _enc.mkInt(_enc.defaultMetric());

    // TODO: remove all isChanged calls with actual symbolic values that test for a change

    boolean isIbgp = _graphEdge.isAbstract() && _proto.isBgp();

    // Update prefix length when aggregation
    BoolExpr len =
        _enc.safeEq(
            _current.getPrefixLength(), getOrDefault(p.getData().getPrefixLength(), defaultLen));
    BoolExpr per = _enc.safeEq(_current.getPermitted(), p.getData().getPermitted());

    // Only update the router id for import edges
    BoolExpr id = _enc.mkTrue();
    if (!_isExport) {
      id = _enc.safeEq(_current.getRouterId(), getOrDefault(p.getData().getRouterId(), defaultId));
    }

    // Update OSPF area id
    BoolExpr area;
    if (p.getData().getOspfArea() == null || _iface.getOspfAreaName() == null) {
      area = _enc.mkTrue();
    } else {
      area = _enc.safeEqEnum(_current.getOspfArea(), _iface.getOspfAreaName());
    }

    // Set the IGP metric accordingly
    BoolExpr igpMet = _enc.mkTrue();
    boolean isNonClient =
        _graphEdge.isAbstract()
            && (_enc.getGraph().peerType(_graphEdge) != Graph.BgpSendType.TO_EBGP);
    boolean isClient =
        _graphEdge.isAbstract()
            && (_enc.getGraph().peerType(_graphEdge) == Graph.BgpSendType.TO_RR);

    if (_graphEdge.isAbstract() && _current.getIgpMetric() != null) {
      String router = _graphEdge.getRouter();
      String peer = _graphEdge.getPeer();

      // Case where it is a non client, we lookup the next-hop
      if (isNonClient) {
        EncoderSlice s = _enc.getEncoder().getSlice(peer);
        SymbolicRoute r = s.getSymbolicDecisions().getBestNeighbor().get(router);
        igpMet = _enc.mkEq(_current.getIgpMetric(), r.getMetric());
      }

      // Case where it is a client, next-hop depends on the clientId tag we added
      if (isClient) {
        BoolExpr acc = _enc.mkTrue();
        for (Map.Entry<String, Integer> entry : _enc.getGraph().getOriginatorId().entrySet()) {
          String r = entry.getKey();
          Integer clientId = entry.getValue();
          if (!r.equals(router)) {
            EncoderSlice s = _enc.getEncoder().getSlice(r);
            SymbolicRoute record = s.getSymbolicDecisions().getBestNeighbor().get(r);
            BoolExpr eq = _enc.mkEq(_current.getIgpMetric(), record.getMetric());
            acc =
                _enc.mkAnd(
                    acc, _enc.mkImplies(p.getData().getClientId().checkIfValue(clientId), eq));
          }
        }
        igpMet = acc;
      }
    }

    // Set whether or not is iBGP or not on import
    BoolExpr isInternal =
        _enc.safeEq(_current.getBgpInternal(), _enc.mkBool(isIbgp)); // TODO: and !isExport?

    // Update OSPF type
    BoolExpr type;
    if (result.isChanged("OSPF-TYPE")) {
      type = _enc.safeEqEnum(_current.getOspfType(), p.getData().getOspfType());
    } else {
      boolean hasAreaIface = _iface.getOspfAreaName() != null;
      boolean hasArea = p.getData().getOspfArea() != null;
      boolean hasType = p.getData().getOspfType() != null;
      boolean areaPossiblyChanged = hasType && hasArea && hasAreaIface;
      // Check if area changed
      if (areaPossiblyChanged) {
        BoolExpr internal = p.getData().getOspfType().isInternal();
        BoolExpr same = p.getData().getOspfArea().checkIfValue(_iface.getOspfAreaName());
        BoolExpr update = _enc.mkAnd(internal, _enc.mkNot(same));
        BoolExpr copyOld = _enc.safeEqEnum(_current.getOspfType(), p.getData().getOspfType());
        type = _enc.mkIf(update, _current.getOspfType().checkIfValue(OspfType.OIA), copyOld);
      } else {
        type = _enc.safeEqEnum(_current.getOspfType(), p.getData().getOspfType());
      }
    }

    BoolExpr comms = _enc.mkTrue();
    // update all community values
    for (Map.Entry<CommunityVar, BoolExpr> entry : _current.getCommunities().entrySet()) {
      CommunityVar cvar = entry.getKey();
      BoolExpr e = entry.getValue();
      BoolExpr eOther = p.getData().getCommunities().get(cvar);
      // Update the communities if they should be sent
      if (sendCommunity()) {
        if (cvar.getType() != CommunityVar.Type.REGEX) {
          comms = _enc.mkAnd(comms, _enc.mkEq(e, eOther));
        }
      } else {
        comms = _enc.mkAnd(comms, _enc.mkNot(e));
      }
    }

    ArithExpr otherAd =
        (p.getData().getAdminDist() == null ? defaultAd : p.getData().getAdminDist());
    ArithExpr otherMed = (p.getData().getMed() == null ? defaultMed : p.getData().getMed());
    ArithExpr otherLp = getOrDefault(p.getData().getLocalPref(), defaultLp);
    ArithExpr otherMet = getOrDefault(p.getData().getMetric(), defaultMet);
    // otherMet = applyMetricUpdate(otherMet);

    BoolExpr ad = _enc.safeEq(_current.getAdminDist(), otherAd);
    BoolExpr history = _enc.equalHistories(_current, p.getData());
    BoolExpr med = _enc.safeEq(_current.getMed(), otherMed);
    BoolExpr met = _enc.safeEq(_current.getMetric(), otherMet);
    BoolExpr lp = _enc.safeEq(_current.getLocalPref(), otherLp);

    // If this was an external route, then we need to add the correct next-hop tag
    boolean isEbgpEdge = _enc.getGraph().getEbgpNeighbors().get(_graphEdge) != null;
    BoolExpr cid = _enc.mkTrue();
    if (_isExport && _proto.isBgp() && p.getData().getClientId() != null) {
      if (isEbgpEdge) {
        cid = _current.getClientId().checkIfValue(0);
      } else {
        cid = _enc.safeEqEnum(_current.getClientId(), p.getData().getClientId());
      }
    }
    if (!_isExport && _proto.isBgp() && p.getData().getClientId() != null) {
      BoolExpr fromExternal = p.getData().getClientId().checkIfValue(0);
      BoolExpr edgeIsInternal = _enc.mkBool(!isClient && !isNonClient);
      BoolExpr copyOver = _enc.safeEqEnum(_current.getClientId(), p.getData().getClientId());
      Integer x = _enc.getGraph().getOriginatorId().get(_graphEdge.getRouter());
      SymbolicOriginatorId soid = _current.getClientId();
      BoolExpr setNewValue = (x == null ? soid.checkIfValue(0) : soid.checkIfValue(x));
      cid = _enc.mkIf(_enc.mkAnd(fromExternal, edgeIsInternal), setNewValue, copyOver);
    }

    BoolExpr updates =
        _enc.mkAnd(
            per, len, ad, med, lp, met, id, cid, type, area, comms, history, isInternal, igpMet);
    BoolExpr noOverflow = noOverflow(otherMet, _proto);

    return _enc.mkIf(noOverflow, updates, _enc.mkNot(_current.getPermitted()));
  }

  /*
   * Create a new variable reflecting the final return value of the function
   */
  private TransferResult<BoolExpr, BoolExpr> returnValue(
      TransferParam<SymbolicRoute> p, TransferResult<BoolExpr, BoolExpr> r, boolean val) {
    BoolExpr b = _enc.mkIf(r.getReturnAssignedValue(), r.getReturnValue(), _enc.mkBool(val));
    BoolExpr newRet = createBoolVariableWith(p, "RETURN", b);
    return r.setReturnValue(newRet)
        .setReturnAssignedValue(_enc.mkTrue())
        .addChangedVariable("RETURN", newRet);
  }

  private TransferResult<BoolExpr, BoolExpr> fallthrough(
      TransferParam<SymbolicRoute> p, TransferResult<BoolExpr, BoolExpr> r) {
    BoolExpr b = _enc.mkIf(r.getReturnAssignedValue(), r.getFallthroughValue(), _enc.mkTrue());
    BoolExpr newFallthrough = createBoolVariableWith(p, "FALLTHROUGH", b);
    return r.setFallthroughValue(newFallthrough)
        .setReturnAssignedValue(_enc.mkTrue())
        .addChangedVariable("FALLTHROUGH", newFallthrough);
  }

  private void updateSingleValue(TransferParam<SymbolicRoute> p, String variableName, Expr expr) {
    switch (variableName) {
      case "METRIC":
        p.getData().setMetric((ArithExpr) expr);
        break;
      case "PREFIX-LEN":
        p.getData().setPrefixLength((ArithExpr) expr);
        break;
      case "ADMIN-DIST":
        p.getData().setAdminDist((ArithExpr) expr);
        break;
      case "LOCAL-PREF":
        p.getData().setLocalPref((ArithExpr) expr);
        break;
      case "OSPF-TYPE":
        p.getData().getOspfType().setBitVec((BitVecExpr) expr);
        break;
      case "RETURN":
        break;
      default:
        for (Map.Entry<CommunityVar, BoolExpr> entry : p.getData().getCommunities().entrySet()) {
          CommunityVar cvar = entry.getKey();
          if (variableName.equals(cvar.getRegex())) {
            p.getData().getCommunities().put(cvar, (BoolExpr) expr);
            return;
          }
        }

        throw new BatfishException("Unimplemented: update for " + variableName);
    }
  }

  /*
   * The [phi] function from SSA that merges variables that may differ across
   * different branches of an If statement.
   */
  private Pair<Expr, Expr> joinPoint(
      TransferParam<SymbolicRoute> p,
      TransferResult<BoolExpr, BoolExpr> r,
      BoolExpr guard,
      Pair<String, Pair<Expr, Expr>> values) {
    String variableName = values.getFirst();
    Expr trueBranch = values.getSecond().getFirst();
    Expr falseBranch = values.getSecond().getSecond();

    if (variableName.equals("RETURN") || variableName.equals("FALLTHROUGH")) {
      Expr t =
          (trueBranch == null
              ? _enc.mkFalse()
              : trueBranch); // can use False because the value has not been assigned
      Expr f = (falseBranch == null ? _enc.mkFalse() : falseBranch);
      Expr tass = (trueBranch == null ? r.getReturnAssignedValue() : _enc.mkTrue());
      Expr fass = (falseBranch == null ? r.getReturnAssignedValue() : _enc.mkTrue());
      BoolExpr newAss = _enc.mkIf(guard, (BoolExpr) tass, (BoolExpr) fass);
      BoolExpr retAss = createBoolVariableWith(p, "ASSIGNED", newAss);
      BoolExpr variable =
          (variableName.equals("RETURN") ? r.getReturnValue() : r.getFallthroughValue());
      BoolExpr newValue =
          _enc.mkIf(
              r.getReturnAssignedValue(), variable, _enc.mkIf(guard, (BoolExpr) t, (BoolExpr) f));
      BoolExpr ret = createBoolVariableWith(p, variableName, newValue);
      return new Pair<>(ret, retAss);
    }

    if (variableName.equals("PREFIX-LEN")) {
      Expr t = (trueBranch == null ? p.getData().getPrefixLength() : trueBranch);
      Expr f = (falseBranch == null ? p.getData().getPrefixLength() : falseBranch);
      ArithExpr newValue = _enc.mkIf(guard, (ArithExpr) t, (ArithExpr) f);
      newValue = _enc.mkIf(r.getReturnAssignedValue(), p.getData().getPrefixLength(), newValue);
      ArithExpr ret = createArithVariableWith(p, "PREFIX-LEN", newValue);
      p.getData().setPrefixLength(ret);
      return new Pair<>(ret, null);
    }
    if (variableName.equals("ADMIN-DIST")) {
      Expr t = (trueBranch == null ? p.getData().getAdminDist() : trueBranch);
      Expr f = (falseBranch == null ? p.getData().getAdminDist() : falseBranch);
      ArithExpr newValue = _enc.mkIf(guard, (ArithExpr) t, (ArithExpr) f);
      newValue = _enc.mkIf(r.getReturnAssignedValue(), p.getData().getAdminDist(), newValue);
      ArithExpr ret = createArithVariableWith(p, "ADMIN-DIST", newValue);
      p.getData().setAdminDist(ret);
      return new Pair<>(ret, null);
    }
    if (variableName.equals("LOCAL-PREF")) {
      Expr t = (trueBranch == null ? p.getData().getLocalPref() : trueBranch);
      Expr f = (falseBranch == null ? p.getData().getLocalPref() : falseBranch);
      ArithExpr newValue = _enc.mkIf(guard, (ArithExpr) t, (ArithExpr) f);
      newValue = _enc.mkIf(r.getReturnAssignedValue(), p.getData().getLocalPref(), newValue);
      ArithExpr ret = createArithVariableWith(p, "LOCAL-PREF", newValue);
      p.getData().setLocalPref(ret);
      return new Pair<>(ret, null);
    }
    if (variableName.equals("METRIC")) {
      Expr t = (trueBranch == null ? p.getData().getMetric() : trueBranch);
      Expr f = (falseBranch == null ? p.getData().getMetric() : falseBranch);
      ArithExpr newValue = _enc.mkIf(guard, (ArithExpr) t, (ArithExpr) f);
      newValue = _enc.mkIf(r.getReturnAssignedValue(), p.getData().getMetric(), newValue);
      ArithExpr ret = createArithVariableWith(p, "METRIC", newValue);
      p.getData().setMetric(ret);
      return new Pair<>(ret, null);
    }
    if (variableName.equals("OSPF-TYPE")) {
      Expr t = (trueBranch == null ? p.getData().getOspfType().getBitVec() : trueBranch);
      Expr f = (falseBranch == null ? p.getData().getOspfType().getBitVec() : falseBranch);
      BitVecExpr newValue = _enc.mkIf(guard, (BitVecExpr) t, (BitVecExpr) f);
      newValue =
          _enc.mkIf(r.getReturnAssignedValue(), p.getData().getOspfType().getBitVec(), newValue);
      BitVecExpr ret = createBitVecVariableWith(p, "OSPF-TYPE", 2, newValue);
      p.getData().getOspfType().setBitVec(ret);
      return new Pair<>(ret, null);
    }

    for (Map.Entry<CommunityVar, BoolExpr> entry : p.getData().getCommunities().entrySet()) {
      CommunityVar cvar = entry.getKey();
      if (variableName.equals(cvar.getRegex())) {
        Expr t = (trueBranch == null ? p.getData().getCommunities().get(cvar) : trueBranch);
        Expr f = (falseBranch == null ? p.getData().getCommunities().get(cvar) : falseBranch);
        BoolExpr newValue = _enc.mkIf(guard, (BoolExpr) t, (BoolExpr) f);
        newValue =
            _enc.mkIf(r.getReturnAssignedValue(), p.getData().getCommunities().get(cvar), newValue);
        BoolExpr ret = createBoolVariableWith(p, cvar.getRegex(), newValue);
        p.getData().getCommunities().put(cvar, ret);
        return new Pair<>(ret, null);
      }
    }

    throw new BatfishException("[joinPoint]: unhandled case for " + variableName);
  }

  /*
   * Convert a list of statements into a Z3 boolean expression for the transfer function.
   */
  private TransferResult<BoolExpr, BoolExpr> compute(
      List<Statement> statements,
      TransferParam<SymbolicRoute> p,
      TransferResult<BoolExpr, BoolExpr> result) {
    TransferParam<SymbolicRoute> curP = p;
    TransferResult<BoolExpr, BoolExpr> curResult = result;
    boolean doesReturn = false;

    for (Statement stmt : statements) {

      if (stmt instanceof StaticStatement) {
        StaticStatement ss = (StaticStatement) stmt;

        switch (ss.getType()) {
          case ExitAccept:
            doesReturn = true;
            curP.debug("ExitAccept");
            curResult = returnValue(curP, curResult, true);
            break;

            // TODO: implement proper unsuppression of routes covered by aggregates
          case Unsuppress:
          case ReturnTrue:
            doesReturn = true;
            curP.debug("ReturnTrue");
            curResult = returnValue(curP, curResult, true);
            break;

          case ExitReject:
            doesReturn = true;
            curP.debug("ExitReject");
            curResult = returnValue(curP, curResult, false);
            break;

            // TODO: implement proper suppression of routes covered by aggregates
          case Suppress:
          case ReturnFalse:
            doesReturn = true;
            curP.debug("ReturnFalse");
            curResult = returnValue(curP, curResult, false);
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
              curResult = returnValue(curP, curResult, true);
            } else {
              curResult = returnValue(curP, curResult, false);
            }
            break;

          case FallThrough:
            curP.debug("Fallthrough");
            curResult = fallthrough(curP, curResult);
            break;

          case Return:
            // TODO: assumming this happens at the end of the function, so it is ignored for now.
            curP.debug("Return");
            break;

          case RemovePrivateAs:
            curP.debug("RemovePrivateAs");
            System.out.println("Warning: use of unimplemented feature RemovePrivateAs");
            break;

          default:
            throw new BatfishException("TODO: computeTransferFunction: " + ss.getType());
        }

      } else if (stmt instanceof If) {
        curP.debug("If");
        If i = (If) stmt;
        TransferResult<BoolExpr, BoolExpr> r = compute(i.getGuard(), curP);
        curResult = curResult.addChangedVariables(r);
        BoolExpr guard = (BoolExpr) r.getReturnValue().simplify();
        String str = guard.toString();

        // If there are updates in the guard, add them to the parameter p before entering branches
        for (Pair<String, Expr> changed : r.getChangedVariables()) {
          curP.debug("CHANGED: " + changed.getFirst());
          updateSingleValue(curP, changed.getFirst(), changed.getSecond());
        }

        curP.debug("guard: " + str);
        // If we know the branch ahead of time, then specialize
        switch (str) {
          case "true":
            curP.debug("True Branch");
            curResult = compute(i.getTrueStatements(), curP.indent(), curResult);
            break;
          case "false":
            curP.debug("False Branch");
            compute(i.getFalseStatements(), curP.indent(), curResult);
            break;
          default:
            curP.debug("True Branch");
            // clear changed variables before proceeding
            TransferParam<SymbolicRoute> p1 = curP.indent().setData(curP.getData().copy());
            TransferParam<SymbolicRoute> p2 = curP.indent().setData(curP.getData().copy());

            TransferResult<BoolExpr, BoolExpr> trueBranch =
                compute(i.getTrueStatements(), p1, initialResult());
            curP.debug("False Branch");
            TransferResult<BoolExpr, BoolExpr> falseBranch =
                compute(i.getFalseStatements(), p2, initialResult());
            curP.debug("JOIN");
            PList<Pair<String, Pair<Expr, Expr>>> pairs =
                trueBranch.mergeChangedVariables(falseBranch);

            // Extract and deal with the return value first so that other
            // variables have this reflected in their value
            int idx = pairs.find(pair -> pair.getFirst().equals("RETURN"));
            if (idx >= 0) {
              Pair<String, Pair<Expr, Expr>> ret = pairs.get(idx);
              pairs = pairs.minus(idx);
              pairs = pairs.plus(pairs.size(), ret);
            }

            for (Pair<String, Pair<Expr, Expr>> pair : pairs) {
              String s = pair.getFirst();
              curP.debug("CHANGED: " + s);
              Pair<Expr, Expr> x = joinPoint(curP, curResult, guard, pair);
              curResult = curResult.addChangedVariable(s, x.getFirst());
              if (s.equals("RETURN")) {
                curResult =
                    curResult
                        .setReturnValue((BoolExpr) x.getFirst())
                        .setReturnAssignedValue((BoolExpr) x.getSecond());
              }
              if (s.equals("FALLTHROUGH")) {
                curResult =
                    curResult
                        .setFallthroughValue((BoolExpr) x.getFirst())
                        .setReturnAssignedValue((BoolExpr) x.getSecond());
              }
            }

            break;
        }

      } else if (stmt instanceof SetDefaultPolicy) {
        curP.debug("SetDefaultPolicy");
        curP = curP.setDefaultPolicy((SetDefaultPolicy) stmt);

      } else if (stmt instanceof SetMetric) {
        curP.debug("SetMetric");
        // TODO: what is the semantics for BGP? Is this MED?
        if (!_current.getProto().isBgp()) {
          SetMetric sm = (SetMetric) stmt;
          LongExpr ie = sm.getMetric();
          ArithExpr newValue = applyLongExprModification(curP.getData().getMetric(), ie);
          newValue =
              _enc.mkIf(curResult.getReturnAssignedValue(), curP.getData().getMetric(), newValue);
          ArithExpr x = createArithVariableWith(curP, "METRIC", newValue);
          curP.getData().setMetric(x);
          curResult = curResult.addChangedVariable("METRIC", x);
        }

      } else if (stmt instanceof SetOspfMetricType) {
        curP.debug("SetOspfMetricType");
        SetOspfMetricType somt = (SetOspfMetricType) stmt;
        OspfMetricType mt = somt.getMetricType();
        SymbolicOspfType t;
        if (mt == OspfMetricType.E1) {
          t = new SymbolicOspfType(_enc, OspfType.E1);
        } else {
          t = new SymbolicOspfType(_enc, OspfType.E2);
        }
        BitVecExpr newValue = t.getBitVec();
        newValue =
            _enc.mkIf(
                curResult.getReturnAssignedValue(),
                curP.getData().getOspfType().getBitVec(),
                newValue);
        BitVecExpr x = createBitVecVariableWith(curP, "OSPF-TYPE", 2, newValue);
        curP.getData().getOspfType().setBitVec(x);
        curResult = curResult.addChangedVariable("OSPF-TYPE", x);

      } else if (stmt instanceof SetLocalPreference) {
        curP.debug("SetLocalPreference");
        SetLocalPreference slp = (SetLocalPreference) stmt;
        LongExpr ie = slp.getLocalPreference();
        ArithExpr newValue = applyLongExprModification(curP.getData().getLocalPref(), ie);
        newValue =
            _enc.mkIf(curResult.getReturnAssignedValue(), curP.getData().getLocalPref(), newValue);
        ArithExpr x = createArithVariableWith(curP, "LOCAL-PREF", newValue);
        curP.getData().setLocalPref(x);
        curResult = curResult.addChangedVariable("LOCAL-PREF", x);

      } else if (stmt instanceof AddCommunity) {
        curP.debug("AddCommunity");
        AddCommunity ac = (AddCommunity) stmt;
        Set<CommunityVar> comms = collectCommunityVars(_conf, ac.getExpr());
        for (CommunityVar cvar : comms) {
          BoolExpr newValue =
              _enc.mkIf(
                  curResult.getReturnAssignedValue(),
                  curP.getData().getCommunities().get(cvar),
                  _enc.mkTrue());
          BoolExpr x = createBoolVariableWith(curP, cvar.getRegex(), newValue);
          curP.getData().getCommunities().put(cvar, x);
          curResult = curResult.addChangedVariable(cvar.getRegex(), x);
        }

      } else if (stmt instanceof SetCommunity) {
        curP.debug("SetCommunity");
        SetCommunity sc = (SetCommunity) stmt;
        Set<CommunityVar> comms = collectCommunityVars(_conf, sc.getExpr());
        for (CommunityVar cvar : comms) {
          BoolExpr newValue =
              _enc.mkIf(
                  curResult.getReturnAssignedValue(),
                  curP.getData().getCommunities().get(cvar),
                  _enc.mkTrue());
          BoolExpr x = createBoolVariableWith(curP, cvar.getRegex(), newValue);
          curP.getData().getCommunities().put(cvar, x);
          curResult = curResult.addChangedVariable(cvar.getRegex(), x);
        }

      } else if (stmt instanceof DeleteCommunity) {
        curP.debug("DeleteCommunity");
        DeleteCommunity ac = (DeleteCommunity) stmt;
        Set<CommunityVar> comms = collectCommunityVars(_conf, ac.getExpr());
        Set<CommunityVar> toDelete = new HashSet<>();
        // Find comms to delete
        for (CommunityVar cvar : comms) {
          if (cvar.getType() == Type.REGEX) {
            toDelete.addAll(_enc.getCommunityDependencies().get(cvar));
          } else {
            toDelete.add(cvar);
          }
        }
        // Delete each community
        for (CommunityVar cvar : toDelete) {
          BoolExpr newValue =
              _enc.mkIf(
                  curResult.getReturnAssignedValue(),
                  curP.getData().getCommunities().get(cvar),
                  _enc.mkFalse());
          BoolExpr x = createBoolVariableWith(curP, cvar.getRegex(), newValue);
          curP.getData().getCommunities().put(cvar, x);
          curResult = curResult.addChangedVariable(cvar.getRegex(), x);
        }

      } else if (stmt instanceof RetainCommunity) {
        curP.debug("RetainCommunity");
        // no op

      } else if (stmt instanceof PrependAsPath) {
        curP.debug("PrependAsPath");
        PrependAsPath pap = (PrependAsPath) stmt;
        Integer prependCost = prependLength(pap.getExpr());
        ArithExpr newValue = _enc.mkSum(curP.getData().getMetric(), _enc.mkInt(prependCost));
        newValue =
            _enc.mkIf(curResult.getReturnAssignedValue(), curP.getData().getMetric(), newValue);
        ArithExpr x = createArithVariableWith(curP, "METRIC", newValue);
        curP.getData().setMetric(x);
        curResult = curResult.addChangedVariable("METRIC", x);

      } else if (stmt instanceof SetOrigin) {
        curP.debug("SetOrigin");
        System.out.println("Warning: use of unimplemented feature SetOrigin");

      } else if (stmt instanceof SetNextHop) {
        curP.debug("SetNextHop");
        System.out.println("Warning: use of unimplemented feature SetNextHop");

      } else {

        String s = (_isExport ? "export" : "import");
        String msg =
            String.format(
                "Unimplemented feature %s for %s transfer function on interface %s",
                stmt.toString(), s, _graphEdge.toString());

        throw new BatfishException(msg);
      }
    }

    // If this is the outermost call, then we relate the variables
    if (curP.getInitialCall()) {
      curP.debug("InitialCall finalizing");

      // Apply the default action
      if (!doesReturn) {
        curP.debug("Applying default action: " + curP.getDefaultAccept());
        if (curP.getDefaultAccept()) {
          curResult = returnValue(curP, curResult, true);
        } else {
          curResult = returnValue(curP, curResult, false);
        }
      }
      BoolExpr related = relateVariables(curP, curResult);
      BoolExpr retValue =
          _enc.mkIf(curResult.getReturnValue(), related, _enc.mkNot(_current.getPermitted()));
      curResult = curResult.setReturnValue(retValue);
    }
    return curResult;
  }

  /*
   * Check if we can inline a new SSA variable. We can simply conservatively check
   * if the size of the term will get no larger after inlining. Right now we only
   * check for True and False values because z3 seems to have some issue with
   * identifying the AST expression kind (e.g., e.isTrue() throws an exception).
   */
  private boolean canInline(Expr e) {
    // TODO: such a huge hack
    String s = e.toString();
    // p.debug("[STRING]: " + s);
    // p.debug("Can Inline: " + b);
    return s.length() <= INLINE_HEURISTIC;
  }

  /*
   * A collection of functions to create new SSA variables on-the-fly,
   * while also simultaneously setting their value based on an old value.
   */
  private ArithExpr createArithVariableWith(
      TransferParam<SymbolicRoute> p, String name, ArithExpr e) {
    ArithExpr eSimple = (ArithExpr) e.simplify();
    if (canInline(eSimple)) {
      p.debug(name + "=" + eSimple);
      return eSimple;
    }
    String s = "SSA_" + name + generateId();
    ArithExpr x = _enc.getCtx().mkIntConst(s);
    // _enc.getAllVariables().add(x);
    BoolExpr eq = _enc.mkEq(x, eSimple);
    _enc.add(eq);
    p.debug(eq.toString());
    return x;
  }

  private BoolExpr createBoolVariableWith(TransferParam<SymbolicRoute> p, String name, BoolExpr e) {
    BoolExpr simpleE = (BoolExpr) e.simplify();
    if (canInline(simpleE)) {
      p.debug(name + "=" + simpleE);
      return simpleE;
    }
    String s = "SSA_" + name + generateId();
    BoolExpr x = _enc.getCtx().mkBoolConst(s);
    // _enc.getAllVariables().add(x);
    BoolExpr eq = _enc.mkEq(x, simpleE);
    _enc.add(eq);
    p.debug(eq.toString());
    return x;
  }

  private BitVecExpr createBitVecVariableWith(
      TransferParam<SymbolicRoute> p, String name, int size, BitVecExpr e) {
    BitVecExpr simpleE = (BitVecExpr) e.simplify();
    if (canInline(simpleE)) {
      p.debug(name + "=" + simpleE);
      return simpleE;
    }
    String s = "SSA_" + name + generateId();
    BitVecExpr x = _enc.getCtx().mkBVConst(s, size);
    // _enc.getAllVariables().add(x);
    BoolExpr eq = _enc.mkEq(x, simpleE);
    _enc.add(eq);
    p.debug(eq.toString());
    return x;
  }

  /*
   * Create a new variable representing the new prefix length after
   * applying the effect of aggregation.
   */
  private void computeIntermediatePrefixLen(TransferParam<SymbolicRoute> param) {
    ArithExpr prefixLen = param.getData().getPrefixLength();
    if (_isExport && _proto.isBgp()) {
      _aggregates = aggregateRoutes();
      if (!_aggregates.isEmpty()) {
        for (Map.Entry<Prefix, Boolean> entry : _aggregates.entrySet()) {
          Prefix p = entry.getKey();
          Boolean isSuppressed = entry.getValue();
          ArithExpr len = _enc.mkInt(p.getPrefixLength());
          BoolExpr relevantPfx = _enc.isRelevantFor(p, _enc.getSymbolicPacket().getDstIp());
          BoolExpr relevantLen = _enc.mkGt(param.getData().getPrefixLength(), len);
          BoolExpr relevant = _enc.mkAnd(relevantPfx, relevantLen, _enc.mkBool(isSuppressed));
          prefixLen = _enc.mkIf(relevant, len, prefixLen);
        }
        ArithExpr i = createArithVariableWith(param, "PREFIX-LEN", prefixLen);
        param.getData().setPrefixLength(i);
      }
    }
  }

  private void applyMetricUpdate(TransferParam<SymbolicRoute> p) {
    boolean updateOspf = (!_isExport && _proto.isOspf());
    boolean updateBgp = (_isExport && _proto.isBgp());
    boolean updateMetric = updateOspf || updateBgp;
    if (updateMetric) {
      // If it is a BGP route learned from IGP, then we use metric 0
      ArithExpr newValue;
      ArithExpr cost = _enc.mkInt(_addedCost);
      ArithExpr sum = _enc.mkSum(p.getData().getMetric(), cost);
      if (_proto.isBgp()) {
        BoolExpr isBGP;
        String router = _conf.getHostname();
        boolean hasProtocolVar = _other.getProtocolHistory() != null;
        boolean onlyBGP = _enc.getOptimizations().getSliceHasSingleProtocol().contains(router);
        if (hasProtocolVar) {
          isBGP = _other.getProtocolHistory().checkIfValue(Protocol.BGP);
        } else if (onlyBGP) {
          isBGP = _enc.mkTrue();
        } else {
          isBGP = _enc.mkFalse();
        }
        newValue = _enc.mkIf(isBGP, sum, cost);
      } else {
        newValue = sum;
      }
      p.getData().setMetric(newValue);
    }
  }

  private void setDefaultLocalPref(TransferParam<SymbolicRoute> p) {
    // must be the case that it is an environment variable
    if (p.getData().getLocalPref() == null) {
      p.getData().setLocalPref(_enc.mkInt(100));
    }
  }

  public BoolExpr compute() {
    SymbolicRoute o = new SymbolicRoute(_other);
    TransferParam<SymbolicRoute> p = new TransferParam<>(o, Encoder.ENABLE_DEBUGGING);
    computeIntermediatePrefixLen(p);
    applyMetricUpdate(p);
    setDefaultLocalPref(p);
    TransferResult<BoolExpr, BoolExpr> result = compute(_statements, p, initialResult());
    return result.getReturnValue();
  }
}
