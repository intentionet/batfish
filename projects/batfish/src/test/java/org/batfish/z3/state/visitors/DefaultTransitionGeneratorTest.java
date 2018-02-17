package org.batfish.z3.state.visitors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.SynthesizerInput;
import org.batfish.z3.TestSynthesizerInput;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicRuleStatement;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.FalseExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.RuleStatement;
import org.batfish.z3.expr.TransformationRuleStatement;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.AclDeny;
import org.batfish.z3.state.AclLineMatch;
import org.batfish.z3.state.AclLineNoMatch;
import org.batfish.z3.state.AclPermit;
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
import org.batfish.z3.state.NumberedQuery;
import org.batfish.z3.state.Originate;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.PostIn;
import org.batfish.z3.state.PostInInterface;
import org.batfish.z3.state.PostInVrf;
import org.batfish.z3.state.PostOutInterface;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOut;
import org.batfish.z3.state.PreOutEdge;
import org.batfish.z3.state.PreOutInterface;
import org.junit.Before;
import org.junit.Test;

public class DefaultTransitionGeneratorTest {

  private static final String ACL1 = "acl1";

  private static final String ACL2 = "acl2";

  private static final String ACL3 = "acl3";

  private static final String INTERFACE1 = "interface1";

  private static final String INTERFACE2 = "interface2";

  private static final String INTERFACE3 = "interface3";

  private static final String INTERFACE4 = "interface4";

  private static final Ip IP1 = new Ip("1.1.1.1");

  private static final Ip IP2 = new Ip("2.2.2.2");

  private static final Ip IP3 = new Ip("3.3.3.3");

  private static final Ip IP4 = new Ip("4.4.4.4");

  private static final String LOOPBACK_INTERFACE = "Loopback0";

  private static final String NODE1 = "node1";

  private static final String NODE2 = "node2";

  private static final String NULL_INTERFACE = "Null0";

  private static final String VRF1 = "vrf1";

  private static final String VRF2 = "vrf2";

  private Map<String, Map<String, Map<Integer, LineAction>>> aclActions() {
    ImmutableMap<Integer, LineAction> acl1ActionsByLine =
        ImmutableMap.of(
            0, LineAction.ACCEPT, 1, LineAction.REJECT, 2, LineAction.ACCEPT, 3, LineAction.REJECT);
    ImmutableMap<Integer, LineAction> acl2ActionsByLine =
        ImmutableMap.of(
            0, LineAction.REJECT, 1, LineAction.ACCEPT, 2, LineAction.REJECT, 3, LineAction.ACCEPT);
    ImmutableMap<String, Map<Integer, LineAction>> aclActions =
        ImmutableMap.of(ACL1, acl1ActionsByLine, ACL2, acl2ActionsByLine, ACL3, ImmutableMap.of());
    Map<String, Map<String, Map<Integer, LineAction>>> aclActionss =
        ImmutableMap.of(NODE1, aclActions, NODE2, aclActions);
    return aclActionss;
  }

  private Map<String, Map<String, Map<Integer, BooleanExpr>>> aclConditions() {
    ImmutableMap<Integer, BooleanExpr> acl1ConditionsByLine =
        ImmutableMap.of(
            0,
            TrueExpr.INSTANCE,
            1,
            FalseExpr.INSTANCE,
            2,
            TrueExpr.INSTANCE,
            3,
            FalseExpr.INSTANCE);
    ImmutableMap<Integer, BooleanExpr> acl2ConditionsByLine =
        ImmutableMap.of(
            0,
            FalseExpr.INSTANCE,
            1,
            TrueExpr.INSTANCE,
            2,
            FalseExpr.INSTANCE,
            3,
            TrueExpr.INSTANCE);
    ImmutableMap<String, Map<Integer, BooleanExpr>> aclConditions =
        ImmutableMap.of(
            ACL1, acl1ConditionsByLine, ACL2, acl2ConditionsByLine, ACL3, ImmutableMap.of());
    Map<String, Map<String, Map<Integer, BooleanExpr>>> acl =
        ImmutableMap.of(NODE1, aclConditions, NODE2, aclConditions);
    return acl;
  }

  private Map<String, Map<String, Map<String, Map<NodeInterfacePair, BooleanExpr>>>>
      fibConditions() {
    return ImmutableMap.of(
        NODE1,
        ImmutableMap.of(
            VRF1,
            ImmutableMap.of(
                INTERFACE1,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE2, INTERFACE1),
                    new NumberedQuery(0),
                    new NodeInterfacePair(NODE2, INTERFACE2),
                    new NumberedQuery(1)),
                INTERFACE2,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE2, INTERFACE1),
                    new NumberedQuery(2),
                    new NodeInterfacePair(NODE2, INTERFACE2),
                    new NumberedQuery(3)),
                LOOPBACK_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(4)),
                NULL_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(5)),
                FibRow.DROP_NO_ROUTE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(6))),
            VRF2,
            ImmutableMap.of(
                INTERFACE3,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE2, INTERFACE3),
                    new NumberedQuery(7),
                    new NodeInterfacePair(NODE2, INTERFACE4),
                    new NumberedQuery(8)),
                INTERFACE4,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE2, INTERFACE3),
                    new NumberedQuery(9),
                    new NodeInterfacePair(NODE2, INTERFACE4),
                    new NumberedQuery(10)),
                LOOPBACK_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(11)),
                NULL_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(12)),
                FibRow.DROP_NO_ROUTE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(13)))),
        NODE2,
        ImmutableMap.of(
            VRF1,
            ImmutableMap.of(
                INTERFACE1,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE1, INTERFACE1),
                    new NumberedQuery(14),
                    new NodeInterfacePair(NODE1, INTERFACE2),
                    new NumberedQuery(15)),
                INTERFACE2,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE1, INTERFACE1),
                    new NumberedQuery(16),
                    new NodeInterfacePair(NODE1, INTERFACE2),
                    new NumberedQuery(17)),
                LOOPBACK_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(18)),
                NULL_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(19)),
                FibRow.DROP_NO_ROUTE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(20))),
            VRF2,
            ImmutableMap.of(
                INTERFACE3,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE1, INTERFACE3),
                    new NumberedQuery(21),
                    new NodeInterfacePair(NODE1, INTERFACE4),
                    new NumberedQuery(22)),
                INTERFACE4,
                ImmutableMap.of(
                    new NodeInterfacePair(NODE1, INTERFACE3),
                    new NumberedQuery(23),
                    new NodeInterfacePair(NODE1, INTERFACE4),
                    new NumberedQuery(24)),
                LOOPBACK_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(25)),
                NULL_INTERFACE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(26)),
                FibRow.DROP_NO_ROUTE,
                ImmutableMap.of(NodeInterfacePair.NONE, new NumberedQuery(27)))));
  }

  @Before
  public void setup() {}

  @Test
  public void testPreOutInterface() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setEnabledEdges(
                ImmutableSet.of(
                    new Edge(NODE1, INTERFACE1, NODE2, INTERFACE1),
                    new Edge(NODE1, INTERFACE1, NODE2, INTERFACE2),
                    new Edge(NODE1, INTERFACE2, NODE2, INTERFACE1),
                    new Edge(NODE1, INTERFACE2, NODE2, INTERFACE2),
                    new Edge(NODE2, INTERFACE1, NODE1, INTERFACE1),
                    new Edge(NODE2, INTERFACE1, NODE1, INTERFACE2),
                    new Edge(NODE2, INTERFACE2, NODE1, INTERFACE1),
                    new Edge(NODE2, INTERFACE2, NODE1, INTERFACE2)))
            .setEnabledFlowSinks(
                ImmutableSet.of(
                    new NodeInterfacePair(NODE1, INTERFACE3),
                    new NodeInterfacePair(NODE2, INTERFACE3)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PreOutInterface.State.INSTANCE)));

    // ProjectPreOutEdgeForFlowSinks
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(
                    NODE1,
                    INTERFACE3,
                    Configuration.NODE_NONE_NAME,
                    Interface.FLOW_SINK_TERMINATION_NAME),
                new PreOutInterface(NODE1, INTERFACE3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(
                    NODE2,
                    INTERFACE3,
                    Configuration.NODE_NONE_NAME,
                    Interface.FLOW_SINK_TERMINATION_NAME),
                new PreOutInterface(NODE2, INTERFACE3))));

    // ProjectPreOutEdgeForTopologyEdges
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE1, INTERFACE1, NODE2, INTERFACE1),
                new PreOutInterface(NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE1, INTERFACE1, NODE2, INTERFACE2),
                new PreOutInterface(NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE1, INTERFACE2, NODE2, INTERFACE1),
                new PreOutInterface(NODE1, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE1, INTERFACE2, NODE2, INTERFACE2),
                new PreOutInterface(NODE1, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE2, INTERFACE1, NODE1, INTERFACE1),
                new PreOutInterface(NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE2, INTERFACE1, NODE1, INTERFACE2),
                new PreOutInterface(NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE2, INTERFACE2, NODE1, INTERFACE1),
                new PreOutInterface(NODE2, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreOutEdge(NODE2, INTERFACE2, NODE1, INTERFACE2),
                new PreOutInterface(NODE2, INTERFACE2))));
  }

  @Test
  public void testVisitAccept() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(Accept.State.INSTANCE)));

    assertThat(
        rules,
        equalTo(
            ImmutableSet.of(
                new BasicRuleStatement(
                    new OrExpr(ImmutableList.of(new NodeAccept(NODE1), new NodeAccept(NODE2))),
                    Accept.INSTANCE))));
  }

  @Test
  public void testVisitAclDeny() {
    SynthesizerInput input = TestSynthesizerInput.builder().setAclActions(aclActions()).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(AclDeny.State.INSTANCE)));

    // MatchDenyLine
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL1, 1), new AclDeny(NODE1, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL1, 3), new AclDeny(NODE1, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL2, 0), new AclDeny(NODE1, ACL2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL2, 2), new AclDeny(NODE1, ACL2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL1, 1), new AclDeny(NODE2, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL1, 3), new AclDeny(NODE2, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL2, 0), new AclDeny(NODE2, ACL2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL2, 2), new AclDeny(NODE2, ACL2))));

    // MatchNoLines
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineNoMatch(NODE1, ACL1, 3), new AclDeny(NODE1, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineNoMatch(NODE1, ACL2, 3), new AclDeny(NODE1, ACL2))));
    assertThat(rules, hasItem(new BasicRuleStatement(new AclDeny(NODE1, ACL3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineNoMatch(NODE2, ACL1, 3), new AclDeny(NODE2, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineNoMatch(NODE2, ACL2, 3), new AclDeny(NODE2, ACL2))));
    assertThat(rules, hasItem(new BasicRuleStatement(new AclDeny(NODE1, ACL3))));
  }

  @Test
  public void testVisitAclLineMatch() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setAclConditions(aclConditions()).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(AclLineMatch.State.INSTANCE)));

    // MatchCurrentAndDontMatchPrevious
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(TrueExpr.INSTANCE, new AclLineMatch(NODE1, ACL1, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(FalseExpr.INSTANCE, new AclLineNoMatch(NODE1, ACL1, 0))),
                new AclLineMatch(NODE1, ACL1, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(TrueExpr.INSTANCE, new AclLineNoMatch(NODE1, ACL1, 1))),
                new AclLineMatch(NODE1, ACL1, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(FalseExpr.INSTANCE, new AclLineNoMatch(NODE1, ACL1, 2))),
                new AclLineMatch(NODE1, ACL1, 3))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(FalseExpr.INSTANCE, new AclLineMatch(NODE1, ACL2, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(TrueExpr.INSTANCE, new AclLineNoMatch(NODE1, ACL2, 0))),
                new AclLineMatch(NODE1, ACL2, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(FalseExpr.INSTANCE, new AclLineNoMatch(NODE1, ACL2, 1))),
                new AclLineMatch(NODE1, ACL2, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(TrueExpr.INSTANCE, new AclLineNoMatch(NODE1, ACL2, 2))),
                new AclLineMatch(NODE1, ACL2, 3))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(TrueExpr.INSTANCE, new AclLineMatch(NODE2, ACL1, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(FalseExpr.INSTANCE, new AclLineNoMatch(NODE2, ACL1, 0))),
                new AclLineMatch(NODE2, ACL1, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(TrueExpr.INSTANCE, new AclLineNoMatch(NODE2, ACL1, 1))),
                new AclLineMatch(NODE2, ACL1, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(FalseExpr.INSTANCE, new AclLineNoMatch(NODE2, ACL1, 2))),
                new AclLineMatch(NODE2, ACL1, 3))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(FalseExpr.INSTANCE, new AclLineMatch(NODE2, ACL2, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(TrueExpr.INSTANCE, new AclLineNoMatch(NODE2, ACL2, 0))),
                new AclLineMatch(NODE2, ACL2, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(FalseExpr.INSTANCE, new AclLineNoMatch(NODE2, ACL2, 1))),
                new AclLineMatch(NODE2, ACL2, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(TrueExpr.INSTANCE, new AclLineNoMatch(NODE2, ACL2, 2))),
                new AclLineMatch(NODE2, ACL2, 3))));
  }

  @Test
  public void testVisitAclLineNoMatch() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setAclConditions(aclConditions()).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(AclLineNoMatch.State.INSTANCE)));

    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL1, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL1, 0))),
                new AclLineNoMatch(NODE1, ACL1, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL1, 1))),
                new AclLineNoMatch(NODE1, ACL1, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL1, 2))),
                new AclLineNoMatch(NODE1, ACL1, 3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL2, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL2, 0))),
                new AclLineNoMatch(NODE1, ACL2, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL2, 1))),
                new AclLineNoMatch(NODE1, ACL2, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE1, ACL2, 2))),
                new AclLineNoMatch(NODE1, ACL2, 3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL1, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL1, 0))),
                new AclLineNoMatch(NODE2, ACL1, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL1, 1))),
                new AclLineNoMatch(NODE2, ACL1, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL1, 2))),
                new AclLineNoMatch(NODE2, ACL1, 3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL2, 0))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL2, 0))),
                new AclLineNoMatch(NODE2, ACL2, 1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(FalseExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL2, 1))),
                new AclLineNoMatch(NODE2, ACL2, 2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new NotExpr(TrueExpr.INSTANCE), new AclLineNoMatch(NODE2, ACL2, 2))),
                new AclLineNoMatch(NODE2, ACL2, 3))));
  }

  @Test
  public void testVisitAclPermit() {
    SynthesizerInput input = TestSynthesizerInput.builder().setAclActions(aclActions()).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(AclPermit.State.INSTANCE)));

    // MatchPermitLine
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL1, 0), new AclPermit(NODE1, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL1, 2), new AclPermit(NODE1, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL2, 1), new AclPermit(NODE1, ACL2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE1, ACL2, 3), new AclPermit(NODE1, ACL2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL1, 0), new AclPermit(NODE2, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL1, 2), new AclPermit(NODE2, ACL1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL2, 1), new AclPermit(NODE2, ACL2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(new AclLineMatch(NODE2, ACL2, 3), new AclPermit(NODE2, ACL2))));
  }

  @Test
  public void testVisitDrop() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(Drop.State.INSTANCE)));

    assertThat(
        rules,
        equalTo(
            ImmutableSet.of(
                new BasicRuleStatement(
                    new OrExpr(ImmutableList.of(new NodeDrop(NODE1), new NodeDrop(NODE2))),
                    Drop.INSTANCE))));
  }

  @Test
  public void testVisitDropAcl() {
    SynthesizerInput input = TestSynthesizerInput.builder().build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(DropAcl.State.INSTANCE)));

    Set<RuleStatement> expectedCopyDropAclIn =
        ImmutableSet.of(new BasicRuleStatement(DropAclIn.INSTANCE, DropAcl.INSTANCE));

    Set<RuleStatement> expectedCopyDropAclOut =
        ImmutableSet.of(new BasicRuleStatement(DropAclOut.INSTANCE, DropAcl.INSTANCE));

    assertThat(rules, equalTo(Sets.union(expectedCopyDropAclIn, expectedCopyDropAclOut)));
  }

  @Test
  public void testVisitDropAclIn() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(DropAclIn.State.INSTANCE)));

    assertThat(
        rules,
        equalTo(
            ImmutableSet.of(
                new BasicRuleStatement(
                    new OrExpr(
                        ImmutableList.of(new NodeDropAclIn(NODE1), new NodeDropAclIn(NODE2))),
                    DropAclIn.INSTANCE))));
  }

  @Test
  public void testVisitDropAclOut() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(DropAclOut.State.INSTANCE)));

    assertThat(
        rules,
        equalTo(
            ImmutableSet.of(
                new BasicRuleStatement(
                    new OrExpr(
                        ImmutableList.of(new NodeDropAclOut(NODE1), new NodeDropAclOut(NODE2))),
                    DropAclOut.INSTANCE))));
  }

  @Test
  public void testVisitDropNoRoute() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(DropNoRoute.State.INSTANCE)));

    assertThat(
        rules,
        equalTo(
            ImmutableSet.of(
                new BasicRuleStatement(
                    new OrExpr(
                        ImmutableList.of(new NodeDropNoRoute(NODE1), new NodeDropNoRoute(NODE2))),
                    DropNoRoute.INSTANCE))));
  }

  @Test
  public void testVisitDropNullRoute() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(DropNullRoute.State.INSTANCE)));

    assertThat(
        rules,
        equalTo(
            ImmutableSet.of(
                new BasicRuleStatement(
                    new OrExpr(
                        ImmutableList.of(
                            new NodeDropNullRoute(NODE1), new NodeDropNullRoute(NODE2))),
                    DropNullRoute.INSTANCE))));
  }

  @Test
  public void testVisitNodeAccept() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setEnabledNodes(ImmutableSet.of(NODE1, NODE2))
            .setEnabledFlowSinks(
                ImmutableSet.of(
                    new NodeInterfacePair(NODE1, INTERFACE1),
                    new NodeInterfacePair(NODE1, INTERFACE2),
                    new NodeInterfacePair(NODE2, INTERFACE1),
                    new NodeInterfacePair(NODE2, INTERFACE2)))
            .setIpsByHostname(
                ImmutableMap.of(NODE1, ImmutableSet.of(IP1, IP2), NODE2, ImmutableSet.of(IP3, IP4)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeAccept.State.INSTANCE)));

    // PostInForMe
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PostIn(NODE1),
                        HeaderSpaceMatchExpr.matchDstIp(
                            ImmutableSet.of(new IpWildcard(IP1), new IpWildcard(IP2))))),
                new NodeAccept(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PostIn(NODE2),
                        HeaderSpaceMatchExpr.matchDstIp(
                            ImmutableSet.of(new IpWildcard(IP3), new IpWildcard(IP4))))),
                new NodeAccept(NODE2))));

    // PostOutFlowSinkInterface
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE1, INTERFACE1), new NodeAccept(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE1, INTERFACE2), new NodeAccept(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE2, INTERFACE1), new NodeAccept(NODE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE2, INTERFACE2), new NodeAccept(NODE2))));
  }

  @Test
  public void testVisitNodeDrop() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeDrop.State.INSTANCE)));

    // CopyNodeDropAcl
    assertThat(rules, hasItem(new BasicRuleStatement(new NodeDropAcl(NODE1), new NodeDrop(NODE1))));
    assertThat(rules, hasItem(new BasicRuleStatement(new NodeDropAcl(NODE2), new NodeDrop(NODE2))));

    // CopyNodeDropNoRoute
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NodeDropNoRoute(NODE1), new NodeDrop(NODE1))));
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NodeDropNoRoute(NODE2), new NodeDrop(NODE2))));

    // CopyNodeDropNullRoute
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NodeDropNullRoute(NODE1), new NodeDrop(NODE1))));
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NodeDropNullRoute(NODE2), new NodeDrop(NODE2))));
  }

  @Test
  public void testVisitNodeDropAcl() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setEnabledNodes(ImmutableSet.of(NODE1, NODE2)).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeDropAcl.State.INSTANCE)));

    Set<RuleStatement> expectedCopyNodeDropAclIn =
        ImmutableSet.of(
            new BasicRuleStatement(new NodeDropAclIn(NODE1), new NodeDropAcl(NODE1)),
            new BasicRuleStatement(new NodeDropAclIn(NODE2), new NodeDropAcl(NODE2)));

    Set<RuleStatement> expectedCopyNodeDropAclOut =
        ImmutableSet.of(
            new BasicRuleStatement(new NodeDropAclOut(NODE1), new NodeDropAcl(NODE1)),
            new BasicRuleStatement(new NodeDropAclOut(NODE2), new NodeDropAcl(NODE2)));

    assertThat(rules, equalTo(Sets.union(expectedCopyNodeDropAclIn, expectedCopyNodeDropAclOut)));
  }

  @Test
  public void testVisitNodeDropAclIn() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setIncomingAcls(
                ImmutableMap.of(
                    NODE1,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2),
                    NODE2,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2)))
            .setTopologyInterfaces(
                ImmutableMap.of(
                    NODE1,
                    ImmutableSet.of(INTERFACE1, INTERFACE2),
                    NODE2,
                    ImmutableSet.of(INTERFACE1, INTERFACE2)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeDropAclIn.State.INSTANCE)));

    // FailIncomingAcl
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE1, ACL1), new PreInInterface(NODE1, INTERFACE1))),
                new NodeDropAclIn(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE1, ACL2), new PreInInterface(NODE1, INTERFACE2))),
                new NodeDropAclIn(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE2, ACL1), new PreInInterface(NODE2, INTERFACE1))),
                new NodeDropAclIn(NODE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE2, ACL2), new PreInInterface(NODE2, INTERFACE2))),
                new NodeDropAclIn(NODE2))));
  }

  @Test
  public void testVisitNodeDropAclOut() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setOutgoingAcls(
                ImmutableMap.of(
                    NODE1,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2),
                    NODE2,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2)))
            .setTopologyInterfaces(
                ImmutableMap.of(
                    NODE1,
                    ImmutableSet.of(INTERFACE1, INTERFACE2),
                    NODE2,
                    ImmutableSet.of(INTERFACE1, INTERFACE2)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeDropAclOut.State.INSTANCE)));

    // FailOutgoingAcl
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE1, ACL1), new PreOutInterface(NODE1, INTERFACE1))),
                new NodeDropAclOut(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE1, ACL2), new PreOutInterface(NODE1, INTERFACE2))),
                new NodeDropAclOut(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE2, ACL1), new PreOutInterface(NODE2, INTERFACE1))),
                new NodeDropAclOut(NODE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclDeny(NODE2, ACL2), new PreOutInterface(NODE2, INTERFACE2))),
                new NodeDropAclOut(NODE2))));
  }

  @Test
  public void testVisitNodeDropNoRoute() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setFibConditions(fibConditions()).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeDropNoRoute.State.INSTANCE)));

    // DestinationRouting
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NumberedQuery(6), new NodeDropNoRoute(NODE1))));
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NumberedQuery(13), new NodeDropNoRoute(NODE1))));
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NumberedQuery(20), new NodeDropNoRoute(NODE2))));
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NumberedQuery(27), new NodeDropNoRoute(NODE2))));
  }

  @Test
  public void testVisitNodeDropNullRoute() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setFibConditions(fibConditions()).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeDropNullRoute.State.INSTANCE)));

    // DestinationRouting
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NumberedQuery(4), new NodeDropNullRoute(NODE1))));
    assertThat(
        rules, hasItem(new BasicRuleStatement(new NumberedQuery(5), new NodeDropNullRoute(NODE1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new NumberedQuery(11), new NodeDropNullRoute(NODE1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new NumberedQuery(12), new NodeDropNullRoute(NODE1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new NumberedQuery(18), new NodeDropNullRoute(NODE2))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new NumberedQuery(19), new NodeDropNullRoute(NODE2))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new NumberedQuery(25), new NodeDropNullRoute(NODE2))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new NumberedQuery(26), new NodeDropNullRoute(NODE2))));
  }

  @Test
  public void testVisitNodeTransit() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setEnabledInterfaces(
                ImmutableMap.of(
                    NODE1,
                    ImmutableSet.of(INTERFACE1, INTERFACE2),
                    NODE2,
                    ImmutableSet.of(INTERFACE1, INTERFACE2)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(NodeTransit.State.INSTANCE)));

    // ProjectPostOutInterface
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE1, INTERFACE1), new NodeTransit(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE1, INTERFACE2), new NodeTransit(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE2, INTERFACE1), new NodeTransit(NODE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostOutInterface(NODE2, INTERFACE2), new NodeTransit(NODE2))));
  }

  @Test
  public void testVisitOriginate() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setEnabledVrfs(
                ImmutableMap.of(
                    NODE1, ImmutableSet.of(VRF1, VRF2), NODE2, ImmutableSet.of(VRF1, VRF2)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(Originate.State.INSTANCE)));

    // ProjectOriginateVrf
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE1, VRF1), new Originate(NODE1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE1, VRF2), new Originate(NODE1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE2, VRF1), new Originate(NODE2))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE2, VRF2), new Originate(NODE2))));
  }

  @Test
  public void testVisitPostIn() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setEnabledNodes(ImmutableSet.of(NODE1, NODE2))
            .setEnabledInterfaces(
                ImmutableMap.of(
                    NODE1,
                    ImmutableSet.of(INTERFACE1, INTERFACE2),
                    NODE2,
                    ImmutableSet.of(INTERFACE1, INTERFACE2)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PostIn.State.INSTANCE)));

    // CopyOriginate
    assertThat(rules, hasItem(new BasicRuleStatement(new Originate(NODE1), new PostIn(NODE1))));
    assertThat(rules, hasItem(new BasicRuleStatement(new Originate(NODE2), new PostIn(NODE2))));

    // ProjectPostInInterface
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new PostInInterface(NODE1, INTERFACE1), new PostIn(NODE1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new PostInInterface(NODE1, INTERFACE2), new PostIn(NODE1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new PostInInterface(NODE2, INTERFACE1), new PostIn(NODE2))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new PostInInterface(NODE2, INTERFACE2), new PostIn(NODE2))));
  }

  @Test
  public void testVisitPostInInterface() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setIncomingAcls(
                ImmutableMap.of(
                    NODE1,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2),
                    NODE2,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2)))
            .setTopologyInterfaces(
                ImmutableMap.of(
                    NODE1,
                    ImmutableSet.of(INTERFACE1, INTERFACE2, INTERFACE3),
                    NODE2,
                    ImmutableSet.of(INTERFACE1, INTERFACE2, INTERFACE3)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PostInInterface.State.INSTANCE)));

    // PassIncomingAcl
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE1, ACL1), new PreInInterface(NODE1, INTERFACE1))),
                new PostInInterface(NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE1, ACL2), new PreInInterface(NODE1, INTERFACE2))),
                new PostInInterface(NODE1, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreInInterface(NODE1, INTERFACE3), new PostInInterface(NODE1, INTERFACE3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE2, ACL1), new PreInInterface(NODE2, INTERFACE1))),
                new PostInInterface(NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE2, ACL2), new PreInInterface(NODE2, INTERFACE2))),
                new PostInInterface(NODE2, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PreInInterface(NODE2, INTERFACE3), new PostInInterface(NODE2, INTERFACE3))));
  }

  @Test
  public void testVisitPostInVrf() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setEnabledInterfacesByNodeVrf(
                ImmutableMap.of(
                    NODE1,
                    ImmutableMap.of(
                        VRF1,
                        ImmutableSet.of(INTERFACE1, INTERFACE2),
                        VRF2,
                        ImmutableSet.of(INTERFACE3, INTERFACE4)),
                    NODE2,
                    ImmutableMap.of(
                        VRF1,
                        ImmutableSet.of(INTERFACE1, INTERFACE2),
                        VRF2,
                        ImmutableSet.of(INTERFACE3, INTERFACE4))))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PostInVrf.State.INSTANCE)));

    // CopyOriginateVrf
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE1, VRF1), new PostInVrf(NODE1, VRF1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE1, VRF2), new PostInVrf(NODE1, VRF2))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE2, VRF1), new PostInVrf(NODE2, VRF1))));
    assertThat(
        rules,
        hasItem(new BasicRuleStatement(new OriginateVrf(NODE2, VRF2), new PostInVrf(NODE2, VRF2))));

    // PostInInterfaceCorrespondingVrf
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE1, INTERFACE1), new PostInVrf(NODE1, VRF1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE1, INTERFACE2), new PostInVrf(NODE1, VRF1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE1, INTERFACE3), new PostInVrf(NODE1, VRF2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE1, INTERFACE4), new PostInVrf(NODE1, VRF2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE2, INTERFACE1), new PostInVrf(NODE2, VRF1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE2, INTERFACE2), new PostInVrf(NODE2, VRF1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE2, INTERFACE3), new PostInVrf(NODE2, VRF2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new PostInInterface(NODE2, INTERFACE4), new PostInVrf(NODE2, VRF2))));
  }

  @Test
  public void testVisitPostOutInterface() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setOutgoingAcls(
                ImmutableMap.of(
                    NODE1,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2),
                    NODE2,
                    ImmutableMap.of(INTERFACE1, ACL1, INTERFACE2, ACL2)))
            .setTopologyInterfaces(
                ImmutableMap.of(
                    NODE1,
                    ImmutableSet.of(INTERFACE1, INTERFACE2, INTERFACE3),
                    NODE2,
                    ImmutableSet.of(INTERFACE1, INTERFACE2, INTERFACE3)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PostOutInterface.State.INSTANCE)));

    // PassOutgoingAcl
    assertThat(
        rules,
        hasItem(
            new TransformationRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE1, ACL1), new PreOutInterface(NODE1, INTERFACE1))),
                new PostOutInterface(NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new TransformationRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE1, ACL2), new PreOutInterface(NODE1, INTERFACE2))),
                new PostOutInterface(NODE1, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new TransformationRuleStatement(
                new PreOutInterface(NODE1, INTERFACE3), new PostOutInterface(NODE1, INTERFACE3))));
    assertThat(
        rules,
        hasItem(
            new TransformationRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE2, ACL1), new PreOutInterface(NODE2, INTERFACE1))),
                new PostOutInterface(NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new TransformationRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new AclPermit(NODE2, ACL2), new PreOutInterface(NODE2, INTERFACE2))),
                new PostOutInterface(NODE2, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new TransformationRuleStatement(
                new PreOutInterface(NODE2, INTERFACE3), new PostOutInterface(NODE2, INTERFACE3))));
  }

  @Test
  public void testVisitPreInInterface() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setEnabledEdges(
                ImmutableSet.of(
                    new Edge(NODE1, INTERFACE1, NODE2, INTERFACE1),
                    new Edge(NODE1, INTERFACE1, NODE2, INTERFACE2),
                    new Edge(NODE1, INTERFACE1, NODE2, INTERFACE3),
                    new Edge(NODE1, INTERFACE2, NODE2, INTERFACE1),
                    new Edge(NODE1, INTERFACE2, NODE2, INTERFACE2),
                    new Edge(NODE1, INTERFACE2, NODE2, INTERFACE3),
                    new Edge(NODE1, INTERFACE3, NODE2, INTERFACE1),
                    new Edge(NODE1, INTERFACE3, NODE2, INTERFACE2),
                    new Edge(NODE1, INTERFACE3, NODE2, INTERFACE3),
                    new Edge(NODE2, INTERFACE1, NODE1, INTERFACE1),
                    new Edge(NODE2, INTERFACE1, NODE1, INTERFACE2),
                    new Edge(NODE2, INTERFACE1, NODE1, INTERFACE3),
                    new Edge(NODE2, INTERFACE2, NODE1, INTERFACE1),
                    new Edge(NODE2, INTERFACE2, NODE1, INTERFACE2),
                    new Edge(NODE2, INTERFACE2, NODE1, INTERFACE3),
                    new Edge(NODE2, INTERFACE3, NODE1, INTERFACE1),
                    new Edge(NODE2, INTERFACE3, NODE1, INTERFACE2),
                    new Edge(NODE2, INTERFACE3, NODE1, INTERFACE3)))
            .setEnabledFlowSinks(
                ImmutableSet.of(
                    new NodeInterfacePair(NODE1, INTERFACE3),
                    new NodeInterfacePair(NODE2, INTERFACE3)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PreInInterface.State.INSTANCE)));

    // PostOutNeighbor
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE1, INTERFACE1, NODE2, INTERFACE1),
                        new PostOutInterface(NODE1, INTERFACE1))),
                new PreInInterface(NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE1, INTERFACE1, NODE2, INTERFACE2),
                        new PostOutInterface(NODE1, INTERFACE1))),
                new PreInInterface(NODE2, INTERFACE2))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE1, INTERFACE1, NODE2, INTERFACE3),
                            new PostOutInterface(NODE1, INTERFACE1))),
                    new PreInInterface(NODE2, INTERFACE3)))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE1, INTERFACE2, NODE2, INTERFACE1),
                        new PostOutInterface(NODE1, INTERFACE2))),
                new PreInInterface(NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE1, INTERFACE2, NODE2, INTERFACE2),
                        new PostOutInterface(NODE1, INTERFACE2))),
                new PreInInterface(NODE2, INTERFACE2))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE1, INTERFACE2, NODE2, INTERFACE3),
                            new PostOutInterface(NODE1, INTERFACE2))),
                    new PreInInterface(NODE2, INTERFACE3)))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE1, INTERFACE3, NODE2, INTERFACE1),
                            new PostOutInterface(NODE1, INTERFACE3))),
                    new PreInInterface(NODE2, INTERFACE1)))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE1, INTERFACE3, NODE2, INTERFACE2),
                            new PostOutInterface(NODE1, INTERFACE3))),
                    new PreInInterface(NODE2, INTERFACE2)))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE1, INTERFACE3, NODE2, INTERFACE3),
                            new PostOutInterface(NODE1, INTERFACE3))),
                    new PreInInterface(NODE2, INTERFACE3)))));

    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE2, INTERFACE1, NODE1, INTERFACE1),
                        new PostOutInterface(NODE2, INTERFACE1))),
                new PreInInterface(NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE2, INTERFACE1, NODE1, INTERFACE2),
                        new PostOutInterface(NODE2, INTERFACE1))),
                new PreInInterface(NODE1, INTERFACE2))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE2, INTERFACE1, NODE1, INTERFACE3),
                            new PostOutInterface(NODE2, INTERFACE1))),
                    new PreInInterface(NODE1, INTERFACE3)))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE2, INTERFACE2, NODE1, INTERFACE1),
                        new PostOutInterface(NODE2, INTERFACE2))),
                new PreInInterface(NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PreOutEdge(NODE2, INTERFACE2, NODE1, INTERFACE2),
                        new PostOutInterface(NODE2, INTERFACE2))),
                new PreInInterface(NODE1, INTERFACE2))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE2, INTERFACE2, NODE1, INTERFACE3),
                            new PostOutInterface(NODE2, INTERFACE2))),
                    new PreInInterface(NODE1, INTERFACE3)))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE2, INTERFACE3, NODE1, INTERFACE1),
                            new PostOutInterface(NODE2, INTERFACE3))),
                    new PreInInterface(NODE1, INTERFACE1)))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE2, INTERFACE3, NODE1, INTERFACE2),
                            new PostOutInterface(NODE2, INTERFACE3))),
                    new PreInInterface(NODE1, INTERFACE2)))));
    assertThat(
        rules,
        not(
            hasItem(
                new BasicRuleStatement(
                    new AndExpr(
                        ImmutableList.of(
                            new PreOutEdge(NODE2, INTERFACE3, NODE1, INTERFACE3),
                            new PostOutInterface(NODE2, INTERFACE3))),
                    new PreInInterface(NODE1, INTERFACE3)))));
  }

  @Test
  public void testVisitPreOut() {
    SynthesizerInput input =
        TestSynthesizerInput.builder()
            .setIpsByHostname(
                ImmutableMap.of(NODE1, ImmutableSet.of(IP1, IP2), NODE2, ImmutableSet.of(IP3, IP4)))
            .build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PreOut.State.INSTANCE)));

    // PostInNotMine
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PostIn(NODE1),
                        new NotExpr(
                            HeaderSpaceMatchExpr.matchDstIp(
                                ImmutableSet.of(new IpWildcard(IP1), new IpWildcard(IP2)))))),
                new PreOut(NODE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new AndExpr(
                    ImmutableList.of(
                        new PostIn(NODE2),
                        new NotExpr(
                            HeaderSpaceMatchExpr.matchDstIp(
                                ImmutableSet.of(new IpWildcard(IP3), new IpWildcard(IP4)))))),
                new PreOut(NODE2))));
  }

  @Test
  public void testVisitPreOutEdge() {
    SynthesizerInput input =
        TestSynthesizerInput.builder().setFibConditions(fibConditions()).build();
    Set<RuleStatement> rules =
        ImmutableSet.copyOf(
            DefaultTransitionGenerator.generateTransitions(
                input, ImmutableSet.of(PreOutEdge.State.INSTANCE)));

    // DestinationRouting
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(0), new PreOutEdge(NODE1, INTERFACE1, NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(1), new PreOutEdge(NODE1, INTERFACE1, NODE2, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(2), new PreOutEdge(NODE1, INTERFACE2, NODE2, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(3), new PreOutEdge(NODE1, INTERFACE2, NODE2, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(7), new PreOutEdge(NODE1, INTERFACE3, NODE2, INTERFACE3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(8), new PreOutEdge(NODE1, INTERFACE3, NODE2, INTERFACE4))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(9), new PreOutEdge(NODE1, INTERFACE4, NODE2, INTERFACE3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(10), new PreOutEdge(NODE1, INTERFACE4, NODE2, INTERFACE4))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(14), new PreOutEdge(NODE2, INTERFACE1, NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(15), new PreOutEdge(NODE2, INTERFACE1, NODE1, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(16), new PreOutEdge(NODE2, INTERFACE2, NODE1, INTERFACE1))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(17), new PreOutEdge(NODE2, INTERFACE2, NODE1, INTERFACE2))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(21), new PreOutEdge(NODE2, INTERFACE3, NODE1, INTERFACE3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(22), new PreOutEdge(NODE2, INTERFACE3, NODE1, INTERFACE4))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(23), new PreOutEdge(NODE2, INTERFACE4, NODE1, INTERFACE3))));
    assertThat(
        rules,
        hasItem(
            new BasicRuleStatement(
                new NumberedQuery(24), new PreOutEdge(NODE2, INTERFACE4, NODE1, INTERFACE4))));
  }
}
