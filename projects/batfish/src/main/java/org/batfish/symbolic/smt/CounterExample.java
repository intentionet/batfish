package org.batfish.symbolic.smt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.symbolic.CommunityVar;
import org.batfish.symbolic.CommunityVar.Type;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;
import org.batfish.symbolic.answers.FlowHistory;
import org.batfish.symbolic.answers.FlowTrace;
import org.batfish.symbolic.answers.FlowTraceHop;
import org.batfish.symbolic.utils.Tuple;

class CounterExample {

  private Model _model;

  CounterExample(Model model) {
    this._model = model;
  }

  String evaluate(Expr e) {
    Expr val = _model.evaluate(e, true);
    return val.toString();
  }

  boolean isTrue(Expr e) {
    return "true".equals(evaluate(e));
  }

  boolean isFalse(Expr e) {
    return "false".equals(evaluate(e));
  }

  int intVal(Expr e) {
    return Integer.parseInt(evaluate(e));
  }

  boolean boolVal(Expr e) {
    return Boolean.parseBoolean(evaluate(e));
  }

  Ip ipVal(Expr e) {
    return Ip.create(Long.parseLong(evaluate(e)));
  }

  Flow buildFlow(SymbolicPacket pkt, String routerName) {
    Ip srcIp = ipVal(pkt.getSrcIp());
    Ip dstIp = ipVal(pkt.getDstIp());
    Integer srcPort = intVal(pkt.getSrcPort());
    Integer dstPort = intVal(pkt.getDstPort());
    IpProtocol ipProtocol = IpProtocol.fromNumber(intVal(pkt.getIpProtocol()));
    Integer icmpType = intVal(pkt.getIcmpType());
    Integer icmpCode = intVal(pkt.getIcmpCode());
    Integer tcpFlagsCwr = isTrue(pkt.getTcpCwr()) ? 0 : 1;
    Integer tcpFlagsEce = isTrue(pkt.getTcpEce()) ? 0 : 1;
    Integer tcpFlagsUrg = isTrue(pkt.getTcpUrg()) ? 0 : 1;
    Integer tcpFlagsAck = isTrue(pkt.getTcpAck()) ? 0 : 1;
    Integer tcpFlagsPsh = isTrue(pkt.getTcpPsh()) ? 0 : 1;
    Integer tcpFlagsRst = isTrue(pkt.getTcpRst()) ? 0 : 1;
    Integer tcpFlagsSyn = isTrue(pkt.getTcpSyn()) ? 0 : 1;
    Integer tcpFlagsFin = isTrue(pkt.getTcpFin()) ? 0 : 1;

    Flow.Builder b = Flow.builder();
    b.setIngressNode(routerName);
    b.setSrcIp(srcIp);
    b.setDstIp(dstIp);
    b.setSrcPort(srcPort);
    b.setDstPort(dstPort);
    b.setIpProtocol(ipProtocol);
    b.setIcmpType(icmpType);
    b.setIcmpCode(icmpCode);
    b.setTcpFlagsCwr(tcpFlagsCwr);
    b.setTcpFlagsEce(tcpFlagsEce);
    b.setTcpFlagsUrg(tcpFlagsUrg);
    b.setTcpFlagsAck(tcpFlagsAck);
    b.setTcpFlagsPsh(tcpFlagsPsh);
    b.setTcpFlagsRst(tcpFlagsRst);
    b.setTcpFlagsSyn(tcpFlagsSyn);
    b.setTcpFlagsFin(tcpFlagsFin);
    b.setTag("SMT");
    return b.build();
  }

  private Edge fromGraphEdge(GraphEdge graphEdge) {
    String node1 = graphEdge.getRouter();
    String int1 = graphEdge.getStart() == null ? "none" : graphEdge.getStart().getName();
    String node2 = graphEdge.getPeer();
    String int2 = graphEdge.getEnd() == null ? "none" : graphEdge.getEnd().getName();
    return Edge.of(node1, int1, node2, int2);
  }

  SortedSet<Edge> buildFailedLinks(Encoder encoder) {
    Set<GraphEdge> failedGraphEdges = new HashSet<>();
    Graph g = encoder.getMainSlice().getGraph();
    for (List<GraphEdge> edges : g.getEdgeMap().values()) {
      for (GraphEdge graphEdge : edges) {
        ArithExpr e = encoder.getSymbolicFailures().getFailedVariable(graphEdge);
        assert e != null;
        if (intVal(e) != 0) {
          // Don't add both directions?
          GraphEdge other = g.getOtherEnd().get(graphEdge);
          if (other == null || !failedGraphEdges.contains(other)) {
            failedGraphEdges.add(graphEdge);
          }
        }
      }
    }
    // Convert to Batfish Edge type
    SortedSet<Edge> failedEdges = new TreeSet<>();
    for (GraphEdge graphEdge : failedGraphEdges) {
      failedEdges.add(fromGraphEdge(graphEdge));
    }
    return failedEdges;
  }

  SortedSet<BgpAdvertisement> buildEnvRoutingTable(Encoder enc) {
    SortedSet<BgpAdvertisement> routes = new TreeSet<>();
    EncoderSlice slice = enc.getMainSlice();
    LogicalGraph logicalGraph = slice.getLogicalGraph();

    for (Entry<LogicalEdge, SymbolicRoute> entry : logicalGraph.getEnvironmentVars().entrySet()) {
      LogicalEdge logicalEdge = entry.getKey();
      SymbolicRoute record = entry.getValue();
      // If there is an external advertisement
      if (boolVal(record.getPermitted())) {
        // If we actually use it
        GraphEdge ge = logicalEdge.getEdge();
        String routerName = ge.getRouter();
        SymbolicDecisions decisions = slice.getSymbolicDecisions();
        BoolExpr ctrFwd = decisions.getControlForwarding().get(routerName, ge);
        assert ctrFwd != null;

        if (boolVal(ctrFwd)) {
          SymbolicRoute symbolicRoute = decisions.getBestNeighbor().get(routerName);
          SymbolicPacket pkt = slice.getSymbolicPacket();
          Flow flow = buildFlow(pkt, routerName);
          Prefix pfx = buildPrefix(symbolicRoute, flow);
          int pathLength = intVal(symbolicRoute.getMetric());

          // Create dummy information
          BgpActivePeerConfig n = slice.getGraph().getEbgpNeighbors().get(logicalEdge.getEdge());
          String srcNode = "as" + n.getRemoteAsns();
          Ip zeroIp = Ip.create(0);
          Ip dstIp = n.getLocalIp();

          // Recover AS path
          ImmutableList.Builder<AsSet> b = ImmutableList.builder();
          for (int i = 0; i < pathLength; i++) {
            b.add(AsSet.of(-1L));
          }
          AsPath path = AsPath.of(b.build());

          // Recover communities
          ImmutableSortedSet.Builder<Community> communities = ImmutableSortedSet.naturalOrder();
          for (Entry<CommunityVar, BoolExpr> entry2 : symbolicRoute.getCommunities().entrySet()) {
            CommunityVar cvar = entry2.getKey();
            BoolExpr expr = entry2.getValue();
            if (cvar.getType() == Type.EXACT && boolVal(expr) && cvar.getLiteralValue() != null) {
              communities.add(cvar.getLiteralValue());
            }
          }

          BgpAdvertisement adv =
              new BgpAdvertisement(
                  BgpAdvertisementType.EBGP_RECEIVED,
                  pfx,
                  zeroIp,
                  srcNode,
                  "default",
                  zeroIp,
                  routerName,
                  "default",
                  dstIp,
                  RoutingProtocol.BGP,
                  OriginType.EGP,
                  100,
                  80,
                  zeroIp,
                  path,
                  communities.build(),
                  ImmutableSortedSet.of(),
                  0);

          routes.add(adv);
        }
      }
    }

    return routes;
  }

  /*
   * Build an individual flow hop along a path
   */
  private FlowTraceHop buildFlowTraceHop(GraphEdge graphEdge, String route) {
    String node1 = graphEdge.getRouter();
    String int1 = graphEdge.getStart().getName();
    String node2 = graphEdge.getPeer() == null ? "(none)" : graphEdge.getPeer();
    String int2 = graphEdge.getEnd() == null ? "null_interface" : graphEdge.getEnd().getName();
    Edge edge = Edge.of(node1, int1, node2, int2);
    SortedSet<String> routes = new TreeSet<>();
    routes.add(route);
    return new FlowTraceHop(edge, routes, null, null, null);
  }

  /*
   * Reconstruct the actual route used to forward the packet.
   */
  String buildRoute(Prefix pfx, Protocol proto, GraphEdge graphEdge) {
    String type;
    String nextHopIP;
    String nextHopInt;
    if (proto.isConnected()) {
      type = "ConnectedRoute";
      nextHopIP = "AUTO/NONE(-1l)";
      nextHopInt = graphEdge.getStart().getName();
    } else {
      type = StringUtils.capitalize(proto.name().toLowerCase()) + "Route";
      nextHopIP = graphEdge.getStart().getAddress().getIp().toString();
      nextHopInt = "dynamic";
    }
    return String.format("%s<%s,nhip:%s,nhint:%s>", type, pfx, nextHopIP, nextHopInt);
  }

  /*
   * Create a route from a graph edge
   */
  String buildRoute(EncoderSlice slice, GraphEdge graphEdge) {
    String router = graphEdge.getRouter();
    SymbolicDecisions decisions = slice.getSymbolicDecisions();
    SymbolicRoute symbolicRoute = decisions.getBestNeighbor().get(router);
    SymbolicPacket pkt = slice.getSymbolicPacket();
    Flow flow = buildFlow(pkt, router);
    Prefix pfx = buildPrefix(symbolicRoute, flow);
    Protocol proto = buildProcotol(symbolicRoute, slice, router);
    return buildRoute(pfx, proto, graphEdge);
  }

  /*
   * Reconstruct the prefix from a symbolic record
   */
  Prefix buildPrefix(SymbolicRoute symbolicRoute, Flow flow) {
    Integer pfxLen = intVal(symbolicRoute.getPrefixLength());
    return Prefix.create(flow.getDstIp(), pfxLen);
  }

  /*
   * Reconstruct the protocol from a symbolic record
   */
  Protocol buildProcotol(SymbolicRoute symbolicRoute, EncoderSlice slice, String router) {
    Protocol proto;
    if (symbolicRoute.getProtocolHistory().getBitVec() == null) {
      proto = slice.getProtocols().get(router).get(0);
    } else {
      Integer idx = intVal(symbolicRoute.getProtocolHistory().getBitVec());
      proto = symbolicRoute.getProtocolHistory().value(idx);
    }
    return proto;
  }

  /*
   * Build flow information for a given hop along a path
   */
  Tuple<Flow, FlowTrace> buildFlowTrace(Encoder encoder, String routerName) {
    EncoderSlice slice = encoder.getMainSlice();
    SymbolicPacket pkt = slice.getSymbolicPacket();
    SymbolicDecisions decisions = slice.getSymbolicDecisions();
    Flow flow = buildFlow(pkt, routerName);

    SortedSet<String> visitedRouters = new TreeSet<>();
    List<FlowTraceHop> hops = new ArrayList<>();

    String currentRouterName = routerName;
    while (true) {
      visitedRouters.add(currentRouterName);
      // Get the forwarding variables
      Map<GraphEdge, BoolExpr> dfwd = decisions.getDataForwarding().get(currentRouterName);
      Map<GraphEdge, BoolExpr> cfwd = decisions.getControlForwarding().get(currentRouterName);
      Map<GraphEdge, BoolExpr> across =
          encoder.getMainSlice().getForwardsAcross().get(currentRouterName);
      // Find the route used
      SymbolicRoute symbolicRoute = decisions.getBestNeighbor().get(currentRouterName);
      Protocol proto = buildProcotol(symbolicRoute, slice, currentRouterName);
      Prefix pfx = buildPrefix(symbolicRoute, flow);
      // pick the next router
      boolean found = false;
      for (Entry<GraphEdge, BoolExpr> entry : dfwd.entrySet()) {
        GraphEdge graphEdge = entry.getKey();
        BoolExpr dataForwardingExpr = entry.getValue();
        BoolExpr controlExpr = cfwd.get(graphEdge);
        BoolExpr aclExpr = across.get(graphEdge);
        String route = buildRoute(pfx, proto, graphEdge);
        // forwarded to the next router
        if (isTrue(dataForwardingExpr)) {
          hops.add(buildFlowTraceHop(graphEdge, route));
          // if visited the router before, then a loop detected
          if (graphEdge.getPeer() != null && visitedRouters.contains(graphEdge.getPeer())) {
            FlowTrace flowTrace = new FlowTrace(FlowDisposition.LOOP, hops, "LOOP");
            return new Tuple<>(flow, flowTrace);
          }

          // if acl denies the flow, DENY_IN detected
          if (isFalse(aclExpr)) {
            Interface interf = graphEdge.getEnd();
            IpAccessList acl = interf.getIncomingFilter();
            FilterResult filterResult =
                acl.filter(flow, null, ImmutableMap.of(), ImmutableMap.of());
            String line = "default deny";
            if (filterResult.getMatchLine() != null) {
              line = acl.getLines().get(filterResult.getMatchLine()).getName();
            }
            String note = String.format("DENIED_IN{%s}{%s}", acl.getName(), line);
            FlowTrace flowTrace = new FlowTrace(FlowDisposition.DENIED_IN, hops, note);
            return new Tuple<>(flow, flowTrace);
          }

          boolean isLoopback = slice.getGraph().isLoopback(graphEdge);
          if (isLoopback) {
            FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
            return new Tuple<>(flow, ft);
          }
          if (graphEdge.getPeer() == null) {
            boolean isBgpPeering = slice.getGraph().getEbgpNeighbors().get(graphEdge) != null;
            if (isBgpPeering) {
              FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
              return new Tuple<>(flow, ft);
            } else {
              // the peer is not a BGP peer
              FlowTrace flowTrace =
                  new FlowTrace(
                      FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
                      hops,
                      "NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK");
              return new Tuple<>(flow, flowTrace);
            }
          }
          if (slice.getGraph().isHost(graphEdge.getPeer())) {
            FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
            return new Tuple<>(flow, ft);
          }

          currentRouterName = graphEdge.getPeer();
          found = true;
          break;

        } else if (isTrue(controlExpr)) {
          // dataForwardingExpr is False but controlExpr is True
          hops.add(buildFlowTraceHop(graphEdge, route));
          Interface interf = graphEdge.getStart();
          IpAccessList acl = interf.getOutgoingFilter();
          FilterResult filterResult = acl.filter(flow, null, ImmutableMap.of(), ImmutableMap.of());
          IpAccessListLine line = acl.getLines().get(filterResult.getMatchLine());
          String note = String.format("DENIED_OUT{%s}{%s}", acl.getName(), line.getName());
          FlowTrace flowTrace = new FlowTrace(FlowDisposition.DENIED_OUT, hops, note);
          return new Tuple<>(flow, flowTrace);
        }
      }
      // if not found the next hop router
      if (!found) {
        BoolExpr permitted = symbolicRoute.getPermitted();
        if (boolVal(permitted)) {
          // Check if there is an accepting interface
          for (GraphEdge graphEdge : slice.getGraph().getEdgeMap().get(currentRouterName)) {
            Interface interf = graphEdge.getStart();
            Ip ip = interf.getAddress().getIp();
            if (ip.equals(flow.getDstIp())) {
              FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
              return new Tuple<>(flow, ft);
            }
          }
          FlowTrace flowTrace =
              new FlowTrace(
                  FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
                  hops,
                  "NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK");
          return new Tuple<>(flow, flowTrace);
        }
        FlowTrace flowTrace = new FlowTrace(FlowDisposition.NO_ROUTE, hops, "NO_ROUTE");
        return new Tuple<>(flow, flowTrace);
      }
    }
  }

  /*
   * Creates a trace-based example of what happens
   * to a packet (e.g., why it is not reachable).
   */
  FlowHistory buildFlowHistory(
      String testrigName,
      Collection<String> sourceRouters,
      Encoder encoder,
      Map<String, Boolean> reach) {

    FlowHistory flowHistory = new FlowHistory();
    for (String source : sourceRouters) {
      Boolean sourceVar = reach.get(source);
      if (!sourceVar) {
        Tuple<Flow, FlowTrace> tup = buildFlowTrace(encoder, source);
        SortedSet<Edge> failedLinks = buildFailedLinks(encoder);
        SortedSet<BgpAdvertisement> envRoutes = buildEnvRoutingTable(encoder);
        Environment baseEnv =
            new Environment(testrigName, failedLinks, null, null, null, null, envRoutes);
        flowHistory.addFlowTrace(tup.getFirst(), "BASE", baseEnv, tup.getSecond());
      }
    }
    return flowHistory;
  }

  /*
   * Create a trace-based counterexample demonstrating
   * the difference between two networks on a single packet.
   */
  FlowHistory buildFlowHistoryDiff(
      String testRigName,
      Collection<String> sourceRouters,
      Encoder encoder,
      Encoder encoder2,
      Map<String, BoolExpr> reach,
      Map<String, BoolExpr> reach2) {

    FlowHistory flowHistory = new FlowHistory();
    assert (reach2 != null);
    for (String source : sourceRouters) {
      BoolExpr sourceVar1 = reach.get(source);
      BoolExpr sourceVar2 = reach2.get(source);
      String val1 = evaluate(sourceVar1);
      String val2 = evaluate(sourceVar2);
      if (!Objects.equals(val1, val2)) {
        Tuple<Flow, FlowTrace> diff = buildFlowTrace(encoder, source);
        Tuple<Flow, FlowTrace> base = buildFlowTrace(encoder2, source);
        SortedSet<Edge> failedLinksDiff = buildFailedLinks(encoder);
        SortedSet<Edge> failedLinksBase = buildFailedLinks(encoder2);
        SortedSet<BgpAdvertisement> envRoutesDiff = buildEnvRoutingTable(encoder);
        SortedSet<BgpAdvertisement> envRoutesBase = buildEnvRoutingTable(encoder2);
        Environment baseEnv =
            new Environment(testRigName, failedLinksBase, null, null, null, null, envRoutesBase);
        Environment failedEnv =
            new Environment(
                testRigName + "-with-delta",
                failedLinksDiff,
                null,
                null,
                null,
                null,
                envRoutesDiff);
        flowHistory.addFlowTrace(base.getFirst(), "BASE", baseEnv, base.getSecond());
        flowHistory.addFlowTrace(diff.getFirst(), "DELTA", failedEnv, diff.getSecond());
      }
    }
    return flowHistory;
  }
}
