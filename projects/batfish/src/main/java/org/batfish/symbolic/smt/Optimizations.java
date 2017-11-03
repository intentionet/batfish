package org.batfish.symbolic.smt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.questions.smt.HeaderQuestion;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetLocalPreference;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.symbolic.AstVisitor;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;
import org.batfish.symbolic.collections.Table2;

/**
 * A class containing all the information pertinent for optimizations of the network encoding.
 *
 * <p>Right now, the optimizations here are mainly network-wide. For example, if we remove the
 * local-pref attribute, then it is done so for all records in the network. In the future, there
 * might be good optimizations involving part of the network only.
 *
 * @author Ryan Beckett
 */
class Optimizations {

  private static final boolean ENABLE_IMPORT_EXPORT_MERGE_OPTIMIZATION = true;

  private static final boolean ENABLE_EXPORT_MERGE_OPTIMIZATION = true;

  private static final boolean ENABLE_SLICING_OPTIMIZATION = true;

  private static final String AGGREGATION_SUPPRESS_NAME = "MATCH_SUPPRESSED_SUMMARY_ONLY";

  private EncoderSlice _encoderSlice;

  private Map<String, List<Protocol>> _protocols;

  private Map<String, Set<Prefix>> _suppressedAggregates;

  private Map<String, List<GeneratedRoute>> _relevantAggregates;

  private Set<String> _sliceHasSingleProtocol;

  private Table2<String, Protocol, Boolean> _sliceCanKeepSingleExportVar;

  private Table2<String, Protocol, List<GraphEdge>> _sliceCanCombineImportExportVars;

  private Set<String> _needBgpInternal;

  private Set<String> _needRouterId;

  private boolean _keepLocalPref;

  private boolean _keepAdminDist;

  private boolean _keepMed;

  private boolean _keepOspfType;

  private boolean _needOriginatorIds;

  Optimizations(EncoderSlice encoderSlice) {
    _encoderSlice = encoderSlice;
    _protocols = new HashMap<>();
    _relevantAggregates = new HashMap<>();
    _suppressedAggregates = new HashMap<>();
    _sliceHasSingleProtocol = new HashSet<>();
    _sliceCanCombineImportExportVars = new Table2<>();
    _sliceCanKeepSingleExportVar = new Table2<>();
    _needRouterId = new HashSet<>();
    _needBgpInternal = new HashSet<>();
    _keepLocalPref = true;
    _keepAdminDist = true;
    _keepMed = true;
    _keepOspfType = true;
    _needOriginatorIds = true;
  }

  void computeOptimizations() {
    _keepLocalPref = computeKeepLocalPref();
    _keepAdminDist = computeKeepAdminDistance();
    _keepMed = computeKeepMed();
    _keepOspfType = computeKeepOspfType();
    initProtocols();
    computeBgpInternalNeeded();
    computeCanUseSingleBest();
    computeCanMergeExportVars();
    computeCanMergeImportExportVars();
    computeRelevantAggregates();
    computeSuppressedAggregates();
    computeNeedClientIds();
    computeRouterIdNeeded();
  }

  /*
   * Check if we need to remember which client the original route was sent from.
   * This will be the case if there is at least one route reflector client in use.
   */
  private void computeNeedClientIds() {
    _needOriginatorIds = _encoderSlice.getGraph().getRouteReflectorParent().size() > 0;
  }

  /*
   * Check if the BGP local preference is needed. If it is never set,
   * then the variable can be removed from the encoding.
   */
  private boolean computeKeepLocalPref() {
    if (!Optimizations.ENABLE_SLICING_OPTIMIZATION) {
      return true;
    }
    Boolean[] val = new Boolean[1];
    val[0] = false;
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) ->
                conf.getRoutingPolicies()
                    .forEach(
                        (name, pol) -> {
                          AstVisitor v = new AstVisitor();
                          v.visit(
                              conf,
                              pol.getStatements(),
                              stmt -> {
                                if (stmt instanceof SetLocalPreference) {
                                  val[0] = true;
                                }
                              },
                              expr -> {});
                        }));
    return val[0];
  }

  /*
   * Check if administrative distance needs to be kept for
   * every single message. If it is never set with a custom
   * value, then it can be inferred for the best choice based
   * on the default protocol value.
   */
  private boolean computeKeepAdminDistance() {
    if (!Optimizations.ENABLE_SLICING_OPTIMIZATION) {
      return true;
    }
    AstVisitor v = new AstVisitor();
    Boolean[] val = new Boolean[1];
    val[0] = false;
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) ->
                conf.getRoutingPolicies()
                    .forEach(
                        (name, pol) ->
                            v.visit(
                                conf,
                                pol.getStatements(),
                                stmt -> {
                                  if (stmt instanceof SetOspfMetricType) {
                                    val[0] = true;
                                  }
                                },
                                expr -> {})));
    return val[0];
  }

  // TODO: also check if med never set
  /*
   * Check if we need to keep around the BGP Med attribute.
   */
  private boolean computeKeepMed() {
    return !Optimizations.ENABLE_SLICING_OPTIMIZATION;
    /* if (!Optimizations.ENABLE_SLICING_OPTIMIZATION) {
        return true;
    }
    return _hasEnvironment; */
  }

  /*
   * Check if we need to keep around the OSPF type. If the type
   * is never set via redistribution, and there is a single area,
   * then it is unnecessary.
   */
  private boolean computeKeepOspfType() {
    if (!Optimizations.ENABLE_SLICING_OPTIMIZATION) {
      return true;
    }
    // First check if the ospf metric type is ever set
    AstVisitor v = new AstVisitor();
    Boolean[] val = new Boolean[1];
    val[0] = false;
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) ->
                conf.getRoutingPolicies()
                    .forEach(
                        (name, pol) ->
                            v.visit(
                                conf,
                                pol.getStatements(),
                                stmt -> {
                                  if (stmt instanceof SetOspfMetricType) {
                                    val[0] = true;
                                  }
                                },
                                expr -> {})));
    if (val[0]) {
      return true;
    }

    // Next see if the there are multiple ospf areas
    Set<Long> areaIds = new HashSet<>();
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) -> {
              Set<Long> ids = _encoderSlice.getGraph().getAreaIds().get(router);
              areaIds.addAll(ids);
            });

    return areaIds.size() > 1;
  }

  /*
   * Determine which protocols need to be modeled given the range of
   * destination IPs specified by the encoder.
   */
  private void initProtocols() {
    Graph g = _encoderSlice.getGraph();
    g.getConfigurations().forEach((router, conf) -> getProtocols().put(router, new ArrayList<>()));
    g.getConfigurations()
        .forEach(
            (router, conf) -> {
              List<Protocol> protos = getProtocols().get(router);
              if (conf.getDefaultVrf().getOspfProcess() != null) {
                protos.add(Protocol.OSPF);
              }
              // TODO: re-evaluate this in the future
              // TODO: do we want to model BGP's impact on IGP in other slices?
              if (conf.getDefaultVrf().getBgpProcess() != null && _encoderSlice.isMainSlice()) {
                protos.add(Protocol.BGP);
              }
              if (needToModelConnected(conf)) {
                protos.add(Protocol.CONNECTED);
              }
              if (needToModelStatic(conf)) {
                protos.add(Protocol.STATIC);
              }
            });
  }

  /*
   * We need to model the connected protocol if its interface Ip
   * overlaps with the destination IP of interest in the packet.
   */
  private boolean needToModelConnected(Configuration conf) {
    return !Optimizations.ENABLE_SLICING_OPTIMIZATION
        || hasRelevantOriginatedRoute(conf, Protocol.CONNECTED);
  }

  /*
   * We need to model the static protocol if its interface Ip
   * overlaps with the destination IP of interest in the packet.
   */
  private boolean needToModelStatic(Configuration conf) {
    if (Optimizations.ENABLE_SLICING_OPTIMIZATION) {
      return hasRelevantOriginatedRoute(conf, Protocol.STATIC);
    } else {
      return !conf.getDefaultVrf().getStaticRoutes().isEmpty();
    }
  }

  /*
   * Check if we need to model the router ID
   */
  private void computeRouterIdNeeded() {
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) -> {

              // If iBGP is used, and no multipath, then we need the routerId
              boolean usesIbgp = false;
              for (GraphEdge ge : _encoderSlice.getGraph().getEdgeMap().get(router)) {
                if (_encoderSlice.getGraph().getIbgpNeighbors().get(ge) != null) {
                  usesIbgp = true;
                  break;
                }
              }

              // If eBGP is used, and no multipath, then we need the routerId
              boolean usesEbgp = _encoderSlice.getProtocols().get(router).contains(Protocol.BGP);

              // check if multipath is used
              boolean ibgpMultipath = false;
              boolean ebgpMultipath = false;
              BgpProcess p = conf.getDefaultVrf().getBgpProcess();

              if (p != null) {
                ibgpMultipath = p.getMultipathIbgp();
                ebgpMultipath = p.getMultipathEbgp();
              }

              if ((usesIbgp && !ibgpMultipath) || (usesEbgp && !ebgpMultipath)) {
                _needRouterId.add(router);
              }
            });
  }

  /*
   * Check if we need to remember if a route was learned via iBGP or eBGP.
   * Any router than only has iBGP or eBGP connections will not need the mark.
   */
  private void computeBgpInternalNeeded() {
    _needBgpInternal = new HashSet<>();
    _encoderSlice
        .getGraph()
        .getIbgpNeighbors()
        .forEach((ge, n) -> _needBgpInternal.add(ge.getRouter()));
  }

  /*
   * If there is only a single protocol running on a router, then
   * there is no need to keep both per-protocol and overall best
   * copies of the final choice.
   */
  private void computeCanUseSingleBest() {
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) -> {
              if (getProtocols().get(router).size() == 1) {
                _sliceHasSingleProtocol.add(router);
              }
            });
  }

  /*
   * Determines when we can merge export variables into a single copy.
   * This will be safe when there is no peer-specific export filter.
   */
  private void computeCanMergeExportVars() {
    Graph g = _encoderSlice.getGraph();

    HeaderQuestion q = _encoderSlice.getEncoder().getQuestion();
    boolean noFailures = q.getFailures() == 0;

    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) -> {
              HashMap<Protocol, Boolean> map = new HashMap<>();
              _sliceCanKeepSingleExportVar.put(router, map);

              // Can be no peer-specific export, which includes dropping due to
              // the neighbor already being the root of the tree.
              for (Protocol proto : getProtocols().get(router)) {
                if (proto.isConnected() || proto.isStatic()) {
                  map.put(proto, noFailures && Optimizations.ENABLE_EXPORT_MERGE_OPTIMIZATION);

                } else if (proto.isOspf()) {
                  // Ensure all interfaces are active
                  boolean allIfacesActive = true;
                  for (GraphEdge edge : g.getEdgeMap().get(router)) {
                    if (g.isEdgeUsed(conf, proto, edge)) {
                      Interface iface = edge.getStart();
                      allIfacesActive = allIfacesActive && g.isInterfaceActive(proto, iface);
                    }
                  }

                  // Ensure single area for this router
                  Set<Long> areas = _encoderSlice.getGraph().getAreaIds().get(router);

                  boolean singleArea = areas.size() <= 1;

                  map.put(
                      proto,
                      noFailures
                          && allIfacesActive
                          && singleArea
                          && ENABLE_EXPORT_MERGE_OPTIMIZATION);

                } else if (proto.isBgp()) {

                  boolean acc = true;
                  BgpProcess p = conf.getDefaultVrf().getBgpProcess();
                  for (Map.Entry<Prefix, BgpNeighbor> e : p.getNeighbors().entrySet()) {
                    BgpNeighbor n = e.getValue();
                    // If iBGP used, then don't merge
                    if (n.getLocalAs().equals(n.getRemoteAs())) {
                      acc = false;
                      break;
                    }
                    // If not the default export policy, then don't merge
                    if (!isDefaultBgpExport(conf, n)) {
                      acc = false;
                      break;
                    }
                  }
                  map.put(proto, noFailures && acc && ENABLE_EXPORT_MERGE_OPTIMIZATION);

                } else {
                  throw new BatfishException("Error: unkown protocol: " + proto.name());
                }
              }
            });
  }

  /*
   * Determine when import and export variables can be merged along an edge.
   * This will be safe when there is no peer-specific import filter
   */
  private void computeCanMergeImportExportVars() {

    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) -> {
              Map<Protocol, List<GraphEdge>> map = new HashMap<>();
              _sliceCanCombineImportExportVars.put(router, map);
              for (Protocol proto : getProtocols().get(router)) {

                List<GraphEdge> edges = new ArrayList<>();
                if (Optimizations.ENABLE_IMPORT_EXPORT_MERGE_OPTIMIZATION) {

                  if (!proto.isConnected() && !proto.isStatic() && !proto.isOspf()) {

                    for (GraphEdge ge : _encoderSlice.getGraph().getEdgeMap().get(router)) {

                      // Don't merge when an abstract edge is used.
                      boolean safeMergeEdge =
                          _encoderSlice.getGraph().isEdgeUsed(conf, proto, ge) && !ge.isAbstract();

                      // Don't merge when bgp internal/external can differ
                      boolean sameInternal =
                          (ge.getPeer() == null)
                              || (_needBgpInternal.contains(router)
                                  == _needBgpInternal.contains(ge.getPeer()));

                      // Check if there are any local modifications on import
                      boolean isPure = true;
                      RoutingPolicy pol =
                          _encoderSlice.getGraph().findImportRoutingPolicy(router, proto, ge);
                      if (pol != null) {
                        isPure = false;
                      }

                      boolean noFailures = _encoderSlice.getEncoder().getFailures() == 0;

                      if (safeMergeEdge && sameInternal && isPure && noFailures) {
                        if (hasExportVariables(ge, proto)) {
                          edges.add(ge);
                        }
                      }
                    }
                  }
                }
                map.put(proto, edges);
              }
            });
  }

  /*
   * Computes aggregates that are applicable to the encoding.
   */
  private void computeRelevantAggregates() {
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) -> {
              List<GeneratedRoute> routes = new ArrayList<>();
              _relevantAggregates.put(router, routes);
              for (GeneratedRoute gr : conf.getDefaultVrf().getGeneratedRoutes()) {
                Prefix p = gr.getNetwork();
                if (_encoderSlice.relevantPrefix(p)) {
                  routes.add(gr);
                }
              }
            });
  }

  /*
   * Computes whether each aggregate will suppress more specific routes
   * or if it will advertise both.
   */
  private void computeSuppressedAggregates() {
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (router, conf) -> {
              Set<Prefix> prefixes = new HashSet<>();
              _suppressedAggregates.put(router, prefixes);
              conf.getRouteFilterLists()
                  .forEach(
                      (name, filter) -> {
                        if (name.contains(AGGREGATION_SUPPRESS_NAME)) {
                          for (RouteFilterLine line : filter.getLines()) {
                            prefixes.add(line.getPrefix());
                          }
                        }
                      });
            });
  }

  /*
   * Determine if a BGP neighbor uses the default export policy
   */
  private boolean isDefaultBgpExport(Configuration conf, BgpNeighbor n) {

    // Check if valid neighbor
    if (n == null || n.getExportPolicy() == null) {
      return true;
    }

    // Ensure a single if statement
    RoutingPolicy pol = conf.getRoutingPolicies().get(n.getExportPolicy());
    List<Statement> stmts = pol.getStatements();
    if (stmts.size() != 1) {
      return false;
    }
    Statement s = stmts.get(0);
    if (!(s instanceof If)) {
      return false;
    }

    // Ensure that the true branch accepts and the false branch rejects
    If i = (If) s;
    BooleanExpr be = i.getGuard();
    List<Statement> trueStmts = i.getTrueStatements();
    List<Statement> falseStmts = i.getFalseStatements();

    if (trueStmts.size() != 1 || falseStmts.size() != 1) {
      return false;
    }
    Statement s1 = trueStmts.get(0);
    Statement s2 = falseStmts.get(0);
    if (!(s1 instanceof Statements.StaticStatement)
        || !(s2 instanceof Statements.StaticStatement)) {
      return false;
    }
    Statements.StaticStatement x = (Statements.StaticStatement) s1;
    Statements.StaticStatement y = (Statements.StaticStatement) s2;
    if (x.getType() != Statements.ExitAccept || y.getType() != Statements.ExitReject) {
      return false;
    }

    // Ensure condition just hands off to the common export policy
    if (!(be instanceof CallExpr)) {
      return false;
    }

    CallExpr ce = (CallExpr) be;

    return ce.getCalledPolicyName().contains(Graph.BGP_COMMON_FILTER_LIST_NAME);
  }

  /*
   * Check if a particular protocol has an originated route that is
   * possibly relevant for symbolic the destination Ip packet.
   */
  private boolean hasRelevantOriginatedRoute(Configuration conf, Protocol proto) {
    Set<Prefix> prefixes = Graph.getOriginatedNetworks(conf, proto);
    for (Prefix p1 : prefixes) {
      if (_encoderSlice.relevantPrefix(p1)) {
        return true;
      }
    }
    return false;
  }

  /*
   * Check if a graph edge will have export variables for a given protocol.
   * This will happen when the edge's interface is used in the protocol
   * and the other end of the interface is internal.
   */
  private boolean hasExportVariables(GraphEdge e, Protocol proto) {
    if (e.getEnd() != null) {
      String peer = e.getPeer();
      List<Protocol> peerProtocols = getProtocols().get(peer);
      if (peerProtocols.contains(proto)) {
        Configuration peerConf = _encoderSlice.getGraph().getConfigurations().get(peer);
        GraphEdge other = _encoderSlice.getGraph().getOtherEnd().get(e);
        if (_encoderSlice.getGraph().isEdgeUsed(peerConf, proto, other)) {
          return true;
        }
      }
    }
    return false;
  }

  /*
   * Getters and Setters
   */

  boolean getNeedOriginatorIds() {
    return _needOriginatorIds;
  }

  Map<String, List<Protocol>> getProtocols() {
    return _protocols;
  }

  Map<String, List<GeneratedRoute>> getRelevantAggregates() {
    return _relevantAggregates;
  }

  Map<String, Set<Prefix>> getSuppressedAggregates() {
    return _suppressedAggregates;
  }

  Set<String> getNeedRouterId() {
    return _needRouterId;
  }

  Set<String> getNeedBgpInternal() {
    return _needBgpInternal;
  }

  Table2<String, Protocol, Boolean> getSliceCanKeepSingleExportVar() {
    return _sliceCanKeepSingleExportVar;
  }

  Table2<String, Protocol, List<GraphEdge>> getSliceCanCombineImportExportVars() {
    return _sliceCanCombineImportExportVars;
  }

  Set<String> getSliceHasSingleProtocol() {
    return _sliceHasSingleProtocol;
  }

  boolean getKeepLocalPref() {
    return _keepLocalPref;
  }

  boolean getKeepAdminDist() {
    return _keepAdminDist;
  }

  boolean getKeepMed() {
    return _keepMed;
  }

  boolean getKeepOspfType() {
    return _keepOspfType;
  }
}
