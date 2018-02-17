package org.batfish.z3;

import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.state.StateParameter.Type;

/**
 * The input to reachability program synthesis, including settings and marshalled information from
 * configurations and data-plane
 */
public interface SynthesizerInput {

  /**
   * Mapping: hostname -> aclName -> lineNumber -> lineAction <br>
   * This mapping contains only the acls that are required for the operation this {@link
   * SynthesizerInput} is being used for. Specifically, for ACL-reachability this should contain all
   * ACLs, while for other reachability queries this will only contain ACLs that a packet could
   * reasonably encounter (e.g. ACLs assigned to interfaces).
   */
  Map<String, Map<String, Map<Integer, LineAction>>> getAclActions();

  /**
   * Mapping: hostname -> aclName -> lineNumber -> lineConditions <br>
   * lineConditions is a boolean expression representing the constraints on a header necessary for
   * that line to be matched.
   */
  Map<String, Map<String, Map<Integer, BooleanExpr>>> getAclConditions();

  Set<Edge> getEnabledEdges();

  Set<NodeInterfacePair> getEnabledFlowSinks();

  /** Mapping: hostname -> interfaces */
  Map<String, Set<String>> getEnabledInterfaces();

  /** Mapping: hostname -> vrf -> interfaces */
  Map<String, Map<String, Set<String>>> getEnabledInterfacesByNodeVrf();

  Set<String> getEnabledNodes();

  /** Mapping: hostname -> vrfs */
  Map<String, Set<String>> getEnabledVrfs();

  /** Mapping: hostname -> vrf -> outgoingInterface -> receivingNodeAndInterface -> condition */
  Map<String, Map<String, Map<String, Map<NodeInterfacePair, BooleanExpr>>>> getFibConditions();

  /** Mapping: hostname -> interface-> incomingAcl */
  Map<String, Map<String, String>> getIncomingAcls();

  /** Mapping: hostname -> ipsOwnedByHostname */
  Map<String, Set<Ip>> getIpsByHostname();

  /** Mapping: hostname -> interface-> outgoingAcl */
  Map<String, Map<String, String>> getOutgoingAcls();

  /** Whether to run simplifier on AST after rule generation */
  boolean getSimplify();

  /** Mapping: hostname -> interfacesOnSomeEdge */
  Map<String, Set<String>> getTopologyInterfaces();

  /**
   * Set of parameter types that should be vectorized rather than baked into name of relations.<br>
   * Applies to NoD only.
   */
  Set<Type> getVectorizedParameters();
}
