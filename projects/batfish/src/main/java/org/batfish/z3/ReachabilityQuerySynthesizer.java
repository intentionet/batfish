package org.batfish.z3;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Z3Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.z3.node.AcceptExpr;
import org.batfish.z3.node.AndExpr;
import org.batfish.z3.node.BooleanExpr;
import org.batfish.z3.node.DebugExpr;
import org.batfish.z3.node.DropAclExpr;
import org.batfish.z3.node.DropAclInExpr;
import org.batfish.z3.node.DropAclOutExpr;
import org.batfish.z3.node.DropExpr;
import org.batfish.z3.node.DropNoRouteExpr;
import org.batfish.z3.node.DropNullRouteExpr;
import org.batfish.z3.node.NodeAcceptExpr;
import org.batfish.z3.node.NodeDropAclExpr;
import org.batfish.z3.node.NodeDropAclInExpr;
import org.batfish.z3.node.NodeDropAclOutExpr;
import org.batfish.z3.node.NodeDropExpr;
import org.batfish.z3.node.NodeDropNoRouteExpr;
import org.batfish.z3.node.NodeDropNullRouteExpr;
import org.batfish.z3.node.NodeTransitExpr;
import org.batfish.z3.node.NotExpr;
import org.batfish.z3.node.OrExpr;
import org.batfish.z3.node.OriginateVrfExpr;
import org.batfish.z3.node.QueryExpr;
import org.batfish.z3.node.QueryRelationExpr;
import org.batfish.z3.node.RuleExpr;
import org.batfish.z3.node.SaneExpr;

public class ReachabilityQuerySynthesizer extends BaseQuerySynthesizer {

  private Set<ForwardingAction> _actions;

  private Set<String> _finalNodes;

  private HeaderSpace _headerSpace;

  private Map<String, Set<String>> _ingressNodeVrfs;

  private Set<String> _transitNodes;

  private Set<String> _notTransitNodes;

  public ReachabilityQuerySynthesizer(
      Set<ForwardingAction> actions,
      HeaderSpace headerSpace,
      Set<String> finalNodes,
      Map<String, Set<String>> ingressNodeVrfs,
      Set<String> transitNodes,
      Set<String> notTransitNodes) {
    _actions = actions;
    _finalNodes = finalNodes;
    _headerSpace = headerSpace;
    _ingressNodeVrfs = ingressNodeVrfs;
    _transitNodes = transitNodes;
    _notTransitNodes = notTransitNodes;
  }

  @Override
  public NodProgram getNodProgram(NodProgram baseProgram) throws Z3Exception {
    NodProgram program = new NodProgram(baseProgram.getContext());

    // create rules for injecting symbolic packets into ingress node(s)
    List<RuleExpr> originateRules = new ArrayList<>();
    for (String ingressNode : _ingressNodeVrfs.keySet()) {
      for (String ingressVrf : _ingressNodeVrfs.get(ingressNode)) {
        OriginateVrfExpr originate = new OriginateVrfExpr(ingressNode, ingressVrf);
        RuleExpr originateRule = new RuleExpr(originate);
        originateRules.add(originateRule);
      }
    }

    AndExpr queryConditions = new AndExpr();

    // create query condition for action at final node(s)
    OrExpr finalActions = new OrExpr();
    for (ForwardingAction action : _actions) {
      switch (action) {
        case ACCEPT:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              NodeAcceptExpr accept = new NodeAcceptExpr(finalNode);
              finalActions.addDisjunct(accept);
            }
          } else {
            finalActions.addDisjunct(AcceptExpr.INSTANCE);
          }
          break;

        case DEBUG:
          finalActions.addDisjunct(DebugExpr.INSTANCE);
          break;

        case DROP:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              NodeDropExpr drop = new NodeDropExpr(finalNode);
              finalActions.addDisjunct(drop);
            }
          } else {
            finalActions.addDisjunct(DropExpr.INSTANCE);
          }
          break;

        case DROP_ACL:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              NodeDropAclExpr drop = new NodeDropAclExpr(finalNode);
              finalActions.addDisjunct(drop);
            }
          } else {
            finalActions.addDisjunct(DropAclExpr.INSTANCE);
          }
          break;

        case DROP_ACL_IN:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              NodeDropAclInExpr drop = new NodeDropAclInExpr(finalNode);
              finalActions.addDisjunct(drop);
            }
          } else {
            finalActions.addDisjunct(DropAclInExpr.INSTANCE);
          }
          break;

        case DROP_ACL_OUT:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              NodeDropAclOutExpr drop = new NodeDropAclOutExpr(finalNode);
              finalActions.addDisjunct(drop);
            }
          } else {
            finalActions.addDisjunct(DropAclOutExpr.INSTANCE);
          }
          break;

        case DROP_NO_ROUTE:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              NodeDropNoRouteExpr drop = new NodeDropNoRouteExpr(finalNode);
              finalActions.addDisjunct(drop);
            }
          } else {
            finalActions.addDisjunct(DropNoRouteExpr.INSTANCE);
          }
          break;

        case DROP_NULL_ROUTE:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              NodeDropNullRouteExpr drop = new NodeDropNullRouteExpr(finalNode);
              finalActions.addDisjunct(drop);
            }
          } else {
            finalActions.addDisjunct(DropNullRouteExpr.INSTANCE);
          }
          break;

        case FORWARD:
        default:
          throw new BatfishException("unsupported action");
      }
    }
    queryConditions.addConjunct(finalActions);
    queryConditions.addConjunct(SaneExpr.INSTANCE);

    // check transit constraints (unordered)
    NodeTransitExpr transitExpr = null;
    for (String nodeName : _transitNodes) {
      transitExpr = new NodeTransitExpr(nodeName);
      queryConditions.addConjunct(transitExpr);
    }
    for (String nodeName : _notTransitNodes) {
      transitExpr = new NodeTransitExpr(nodeName);
      queryConditions.addConjunct(new NotExpr(transitExpr));
    }

    // add headerSpace constraints
    BooleanExpr matchHeaderSpace = Synthesizer.matchHeaderSpace(_headerSpace);
    queryConditions.addConjunct(matchHeaderSpace);

    RuleExpr queryRule = new RuleExpr(queryConditions, QueryRelationExpr.INSTANCE);
    List<BoolExpr> rules = program.getRules();
    for (RuleExpr originateRule : originateRules) {
      BoolExpr originateBoolExpr = originateRule.toBoolExpr(baseProgram);
      rules.add(originateBoolExpr);
    }
    rules.add(queryRule.toBoolExpr(baseProgram));
    QueryExpr query = new QueryExpr(QueryRelationExpr.INSTANCE);
    BoolExpr queryBoolExpr = query.toBoolExpr(baseProgram);
    program.getQueries().add(queryBoolExpr);
    return program;
  }
}
