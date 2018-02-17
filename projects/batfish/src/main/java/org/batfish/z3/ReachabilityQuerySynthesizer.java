package org.batfish.z3;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicRuleStatement;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.RuleStatement;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.TransformationRuleStatement;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.Debug;
import org.batfish.z3.state.Drop;
import org.batfish.z3.state.DropAcl;
import org.batfish.z3.state.DropAclIn;
import org.batfish.z3.state.DropAclOut;
import org.batfish.z3.state.DropNoRoute;
import org.batfish.z3.state.DropNullRoute;
import org.batfish.z3.state.NodeAccept;
import org.batfish.z3.state.NodeDrop;
import org.batfish.z3.state.NodeDropAcl;
import org.batfish.z3.state.NodeDropAclIn;
import org.batfish.z3.state.NodeDropAclOut;
import org.batfish.z3.state.NodeDropNoRoute;
import org.batfish.z3.state.NodeDropNullRoute;
import org.batfish.z3.state.NodeTransit;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.Query;

public class ReachabilityQuerySynthesizer extends BaseQuerySynthesizer {

  private Set<ForwardingAction> _actions;

  private Set<String> _finalNodes;

  private HeaderSpace _headerSpace;

  private Map<String, Set<String>> _ingressNodeVrfs;

  private Set<String> _notTransitNodes;

  private Set<String> _transitNodes;

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
  public ReachabilityProgram getReachabilityProgram(SynthesizerInput input) {
    ImmutableList.Builder<BooleanExpr> queryConditionsBuilder = ImmutableList.builder();

    // create query condition for action at final node(s)
    ImmutableList.Builder<BooleanExpr> finalActionsBuilder = ImmutableList.builder();
    for (ForwardingAction action : _actions) {
      switch (action) {
        case ACCEPT:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              BooleanExpr accept = new NodeAccept(finalNode);
              finalActionsBuilder.add(accept);
            }
          } else {
            finalActionsBuilder.add(Accept.INSTANCE);
          }
          break;

        case DEBUG:
          finalActionsBuilder.add(Debug.INSTANCE);
          break;

        case DROP:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              BooleanExpr drop = new NodeDrop(finalNode);
              finalActionsBuilder.add(drop);
            }
          } else {
            finalActionsBuilder.add(Drop.INSTANCE);
          }
          break;

        case DROP_ACL:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              BooleanExpr drop = new NodeDropAcl(finalNode);
              finalActionsBuilder.add(drop);
            }
          } else {
            finalActionsBuilder.add(DropAcl.INSTANCE);
          }
          break;

        case DROP_ACL_IN:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              BooleanExpr drop = new NodeDropAclIn(finalNode);
              finalActionsBuilder.add(drop);
            }
          } else {
            finalActionsBuilder.add(DropAclIn.INSTANCE);
          }
          break;

        case DROP_ACL_OUT:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              BooleanExpr drop = new NodeDropAclOut(finalNode);
              finalActionsBuilder.add(drop);
            }
          } else {
            finalActionsBuilder.add(DropAclOut.INSTANCE);
          }
          break;

        case DROP_NO_ROUTE:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              BooleanExpr drop = new NodeDropNoRoute(finalNode);
              finalActionsBuilder.add(drop);
            }
          } else {
            finalActionsBuilder.add(DropNoRoute.INSTANCE);
          }
          break;

        case DROP_NULL_ROUTE:
          if (_finalNodes.size() > 0) {
            for (String finalNode : _finalNodes) {
              BooleanExpr drop = new NodeDropNullRoute(finalNode);
              finalActionsBuilder.add(drop);
            }
          } else {
            finalActionsBuilder.add(DropNullRoute.INSTANCE);
          }
          break;

        case FORWARD:
        default:
          throw new BatfishException("unsupported action");
      }
    }
    OrExpr finalActions = new OrExpr(finalActionsBuilder.build());
    queryConditionsBuilder.add(finalActions);
    queryConditionsBuilder.add(SaneExpr.INSTANCE);

    // check transit constraints (unordered)
    BooleanExpr transitExpr = null;
    for (String nodeName : _transitNodes) {
      transitExpr = new NodeTransit(nodeName);
      queryConditionsBuilder.add(transitExpr);
    }
    for (String nodeName : _notTransitNodes) {
      transitExpr = new NodeTransit(nodeName);
      queryConditionsBuilder.add(new NotExpr(transitExpr));
    }

    // add headerSpace constraints
    BooleanExpr matchHeaderSpace = new HeaderSpaceMatchExpr(_headerSpace);
    queryConditionsBuilder.add(matchHeaderSpace);

    ImmutableList.Builder<RuleStatement> rules = ImmutableList.builder();
    // create rules for injecting symbolic packets into ingress node(s)
    for (String ingressNode : _ingressNodeVrfs.keySet()) {
      for (String ingressVrf : _ingressNodeVrfs.get(ingressNode)) {
        rules.add(
            new BasicRuleStatement(
                CurrentIsOriginalExpr.INSTANCE, new OriginateVrf(ingressNode, ingressVrf)));
      }
    }
    rules.add(
        new TransformationRuleStatement(
            new AndExpr(queryConditionsBuilder.build()), Query.INSTANCE));
    return ReachabilityProgram.builder()
        .setInput(input)
        .setQueries(ImmutableList.of(new QueryStatement(Query.INSTANCE)))
        .setRules(rules.build())
        .build();
  }
}
