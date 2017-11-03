package org.batfish.symbolic.abstraction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.smt.EquivalenceType;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.answers.RoleAnswerElement;
import org.batfish.symbolic.bdd.BDDAcl;
import org.batfish.symbolic.bdd.BDDNetwork;
import org.batfish.symbolic.bdd.BDDRoute;
import org.batfish.symbolic.utils.Tuple;

public class Roles {

  private Graph _graph;

  private BDDNetwork _network;

  private Pattern _nodeRegex;

  private List<SortedSet<String>> _bgpInEcs = null;

  private List<SortedSet<String>> _bgpOutEcs = null;

  private List<SortedSet<String>> _aclInEcs = null;

  private List<SortedSet<String>> _aclOutEcs = null;

  private List<SortedSet<String>> _interfaceEcs = null;

  private List<SortedSet<String>> _nodeEcs = null;

  public static Roles create(IBatfish batfish, Pattern nodeRegex) {
    Roles rf = new Roles(batfish, nodeRegex);
    rf.computeRoles();
    return rf;
  }

  private Roles(IBatfish batfish, Pattern nodeRegex) {
    _graph = new Graph(batfish);
    _network = BDDNetwork.create(_graph);
    _nodeRegex = nodeRegex;
    _bgpInEcs = null;
    _bgpOutEcs = null;
    _aclInEcs = null;
    _aclOutEcs = null;
    _interfaceEcs = null;
    _nodeEcs = null;
  }

  public AnswerElement asAnswer(EquivalenceType t) {
    RoleAnswerElement ae = new RoleAnswerElement();
    if (t == EquivalenceType.POLICY) {
      ae.setImportBgpEcs(_bgpInEcs);
      ae.setExportBgpEcs(_bgpOutEcs);
      ae.setIncomingAclEcs(_aclInEcs);
      ae.setOutgoingAclEcs(_aclOutEcs);
    }
    if (t == EquivalenceType.INTERFACE) {
      ae.setInterfaceEcs(_interfaceEcs);
    }
    if (t == EquivalenceType.NODE) {
      ae.setNodeEcs(_nodeEcs);
    }
    return ae;
  }

  /*
   * Compute all the devices/interfaces configured with the
   * equivalent policies.
   */
  private void computeRoles() {
    Map<BDDRoute, SortedSet<String>> importBgpEcs = new HashMap<>();
    Map<BDDRoute, SortedSet<String>> exportBgpEcs = new HashMap<>();
    Map<BDD, SortedSet<String>> incomingAclEcs = new HashMap<>();
    Map<BDD, SortedSet<String>> outgoingAclEcs = new HashMap<>();
    Map<Tuple<InterfacePolicy, InterfacePolicy>, SortedSet<String>> interfaceEcs = new HashMap<>();
    Map<Set<Tuple<InterfacePolicy, InterfacePolicy>>, SortedSet<String>> nodeEcs = new HashMap<>();

    SortedSet<String> importBgpNull = new TreeSet<>();
    SortedSet<String> exportBgpNull = new TreeSet<>();
    SortedSet<String> incomingAclNull = new TreeSet<>();
    SortedSet<String> outgoingAclNull = new TreeSet<>();

    for (Entry<String, List<GraphEdge>> entry : _graph.getEdgeMap().entrySet()) {
      String router = entry.getKey();

      Matcher m = _nodeRegex.matcher(router);
      if (!m.matches()) {
        continue;
      }

      List<GraphEdge> ges = entry.getValue();
      Set<Tuple<InterfacePolicy, InterfacePolicy>> nodeEc = new HashSet<>();

      for (GraphEdge ge : ges) {
        String s = ge.toString();

        BDDRoute x1 = _network.getImportBgpPolicies().get(ge);
        if (x1 == null) {
          importBgpNull.add(s);
        } else {
          SortedSet<String> ec = importBgpEcs.computeIfAbsent(x1, k -> new TreeSet<>());
          ec.add(s);
        }

        BDDRoute x2 = _network.getExportBgpPolicies().get(ge);
        if (x2 == null) {
          exportBgpNull.add(s);
        } else {
          SortedSet<String> ec = exportBgpEcs.computeIfAbsent(x2, k -> new TreeSet<>());
          ec.add(s);
        }

        BDDAcl x4 = _network.getInAcls().get(ge);
        if (x4 == null) {
          incomingAclNull.add(s);
        } else {
          SortedSet<String> ec = incomingAclEcs.computeIfAbsent(x4.getBdd(), k -> new TreeSet<>());
          ec.add(s);
        }

        BDDAcl x5 = _network.getOutAcls().get(ge);
        if (x5 == null) {
          outgoingAclNull.add(s);
        } else {
          SortedSet<String> ec = outgoingAclEcs.computeIfAbsent(x5.getBdd(), k -> new TreeSet<>());
          ec.add(s);
        }

        InterfacePolicy x6 = _network.getImportPolicyMap().get(ge);
        InterfacePolicy x7 = _network.getExportPolicyMap().get(ge);
        Tuple<InterfacePolicy, InterfacePolicy> tup = new Tuple<>(x6, x7);

        SortedSet<String> ec = interfaceEcs.computeIfAbsent(tup, k -> new TreeSet<>());
        ec.add(s);
        nodeEc.add(tup);
      }

      SortedSet<String> ec = nodeEcs.computeIfAbsent(nodeEc, k -> new TreeSet<>());
      ec.add(router);
    }

    Comparator<SortedSet<String>> c = comparator();

    _bgpInEcs = new ArrayList<>(importBgpEcs.values());
    if (!importBgpNull.isEmpty()) {
      _bgpInEcs.add(importBgpNull);
    }
    _bgpInEcs.sort(c);

    _bgpOutEcs = new ArrayList<>(exportBgpEcs.values());
    if (!exportBgpNull.isEmpty()) {
      _bgpOutEcs.add(exportBgpNull);
    }
    _bgpOutEcs.sort(c);

    _aclInEcs = new ArrayList<>(incomingAclEcs.values());
    if (!incomingAclNull.isEmpty()) {
      _aclInEcs.add(incomingAclNull);
    }
    _aclInEcs.sort(c);

    _aclOutEcs = new ArrayList<>(outgoingAclEcs.values());
    if (!outgoingAclNull.isEmpty()) {
      _aclOutEcs.add(outgoingAclNull);
    }
    _aclOutEcs.sort(c);

    _interfaceEcs = new ArrayList<>(interfaceEcs.values());
    _interfaceEcs.sort(c);

    _nodeEcs = new ArrayList<>(nodeEcs.values());
    _nodeEcs.sort(c);
  }

  private Comparator<SortedSet<String>> comparator() {
    return (o1, o2) -> {
      String min1 = min(o1);
      String min2 = min(o2);
      return min1.compareTo(min2);
    };
  }

  /*
   * Helper functions to sort the sets by minimum element
   */
  private @Nullable String min(SortedSet<String> set) {
    String x = null;
    for (String s : set) {
      if (x == null || s.compareTo(x) < 0) {
        x = s;
      }
    }
    return x;
  }
}
