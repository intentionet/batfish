package org.batfish.symbolic.smt;

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
import org.apache.commons.lang.StringUtils;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.FlowTraceHop;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.symbolic.CommunityVar;
import org.batfish.symbolic.CommunityVar.Type;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;
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
    return new Ip(Long.parseLong(evaluate(e)));
  }

  Flow buildFlow(SymbolicPacket pkt, String router) {
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

    Flow.Builder b = new Flow.Builder();
    b.setIngressNode(router);
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

  private Edge fromGraphEdge(GraphEdge ge) {
    String w = ge.getRouter();
    String x = ge.getStart() == null ? "none" : ge.getStart().getName();
    String y = ge.getPeer();
    String z = ge.getEnd() == null ? "none" : ge.getEnd().getName();
    return new Edge(w, x, y, z);
  }

  SortedSet<Edge> buildFailedLinks(Encoder enc) {
    Set<GraphEdge> failed = new HashSet<>();
    Graph g = enc.getMainSlice().getGraph();
    for (List<GraphEdge> edges : g.getEdgeMap().values()) {
      for (GraphEdge ge : edges) {
        ArithExpr e = enc.getSymbolicFailures().getFailedVariable(ge);
        assert e != null;
        if (intVal(e) != 0) {
          // Don't add both directions?
          GraphEdge other = g.getOtherEnd().get(ge);
          if (other == null || !failed.contains(other)) {
            failed.add(ge);
          }
        }
      }
    }
    // Convert to Batfish Edge type
    SortedSet<Edge> failedEdges = new TreeSet<>();
    for (GraphEdge ge : failed) {
      failedEdges.add(fromGraphEdge(ge));
    }
    return failedEdges;
  }

  SortedSet<BgpAdvertisement> buildEnvRoutingTable(Encoder enc) {
    SortedSet<BgpAdvertisement> routes = new TreeSet<>();
    EncoderSlice slice = enc.getMainSlice();
    LogicalGraph lg = slice.getLogicalGraph();

    for (Entry<LogicalEdge, SymbolicRoute> entry : lg.getEnvironmentVars().entrySet()) {
      LogicalEdge lge = entry.getKey();
      SymbolicRoute record = entry.getValue();
      // If there is an external advertisement
      if (boolVal(record.getPermitted())) {
        // If we actually use it
        GraphEdge ge = lge.getEdge();
        String router = ge.getRouter();
        SymbolicDecisions decisions = slice.getSymbolicDecisions();
        BoolExpr ctrFwd = decisions.getControlForwarding().get(router, ge);
        assert ctrFwd != null;

        if (boolVal(ctrFwd)) {
          SymbolicRoute r = decisions.getBestNeighbor().get(router);
          SymbolicPacket pkt = slice.getSymbolicPacket();
          Flow f = buildFlow(pkt, router);
          Prefix pfx = buildPrefix(r, f);
          int pathLength = intVal(r.getMetric());

          // Create dummy information
          BgpNeighbor n = slice.getGraph().getEbgpNeighbors().get(lge.getEdge());
          String srcNode = "as" + n.getRemoteAs();
          Ip zeroIp = new Ip(0);
          Ip dstIp = n.getLocalIp();

          // Recover AS path
          List<SortedSet<Integer>> asSets = new ArrayList<>();
          for (int i = 0; i < pathLength; i++) {
            SortedSet<Integer> asSet = new TreeSet<>();
            asSet.add(-1);
            asSets.add(asSet);
          }
          AsPath path = new AsPath(asSets);

          // Recover communities
          SortedSet<Long> communities = new TreeSet<>();
          for (Entry<CommunityVar, BoolExpr> entry2 : r.getCommunities().entrySet()) {
            CommunityVar cvar = entry2.getKey();
            BoolExpr expr = entry2.getValue();
            if (cvar.getType() == Type.EXACT && boolVal(expr)) {
              communities.add(cvar.asLong());
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
                  router,
                  "default",
                  dstIp,
                  RoutingProtocol.BGP,
                  OriginType.EGP,
                  100,
                  80,
                  zeroIp,
                  path,
                  communities,
                  new TreeSet<>(),
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
  private FlowTraceHop buildFlowTraceHop(GraphEdge ge, String route) {
    String node1 = ge.getRouter();
    String int1 = ge.getStart().getName();
    String node2 = ge.getPeer() == null ? "(none)" : ge.getPeer();
    String int2 = ge.getEnd() == null ? "null_interface" : ge.getEnd().getName();
    Edge edge = new Edge(node1, int1, node2, int2);
    SortedSet<String> routes = new TreeSet<>();
    routes.add(route);
    return new FlowTraceHop(edge, routes, null);
  }

  /*
   * Reconstruct the actual route used to forward the packet.
   */
  String buildRoute(Prefix pfx, Protocol proto, GraphEdge ge) {
    String type;
    String nhip;
    String nhint;
    if (proto.isConnected()) {
      type = "ConnectedRoute";
      nhip = "AUTO/NONE(-1l)";
      nhint = ge.getStart().getName();
    } else {
      type = StringUtils.capitalize(proto.name().toLowerCase()) + "Route";
      nhip = ge.getStart().getAddress().getIp().toString();
      nhint = "dynamic";
    }
    return String.format("%s<%s,nhip:%s,nhint:%s>", type, pfx, nhip, nhint);
  }

  /*
   * Create a route from a graph edge
   */
  String buildRoute(EncoderSlice slice, GraphEdge ge) {
    String router = ge.getRouter();
    SymbolicDecisions decisions = slice.getSymbolicDecisions();
    SymbolicRoute r = decisions.getBestNeighbor().get(router);
    SymbolicPacket pkt = slice.getSymbolicPacket();
    Flow f = buildFlow(pkt, router);
    Prefix pfx = buildPrefix(r, f);
    Protocol proto = buildProcotol(r, slice, router);
    return buildRoute(pfx, proto, ge);
  }

  /*
   * Reconstruct the prefix from a symbolic record
   */
  Prefix buildPrefix(SymbolicRoute r, Flow f) {
    Integer pfxLen = intVal(r.getPrefixLength());
    return new Prefix(f.getDstIp(), pfxLen);
  }

  /*
   * Reconstruct the protocol from a symbolic record
   */
  Protocol buildProcotol(SymbolicRoute r, EncoderSlice slice, String router) {
    Protocol proto;
    if (r.getProtocolHistory().getBitVec() == null) {
      proto = slice.getProtocols().get(router).get(0);
    } else {
      Integer idx = intVal(r.getProtocolHistory().getBitVec());
      proto = r.getProtocolHistory().value(idx);
    }
    return proto;
  }

  /*
   * Build flow information for a given hop along a path
   */
  Tuple<Flow, FlowTrace> buildFlowTrace(Encoder enc, String router) {
    EncoderSlice slice = enc.getMainSlice();
    SymbolicPacket pkt = slice.getSymbolicPacket();
    SymbolicDecisions decisions = slice.getSymbolicDecisions();
    Flow f = buildFlow(pkt, router);

    SortedSet<String> visited = new TreeSet<>();
    List<FlowTraceHop> hops = new ArrayList<>();

    String current = router;
    while (true) {
      visited.add(current);
      // Get the forwarding variables
      Map<GraphEdge, BoolExpr> dfwd = decisions.getDataForwarding().get(current);
      Map<GraphEdge, BoolExpr> cfwd = decisions.getControlForwarding().get(current);
      Map<GraphEdge, BoolExpr> across = enc.getMainSlice().getForwardsAcross().get(current);
      // Find the route used
      SymbolicRoute r = decisions.getBestNeighbor().get(current);
      Protocol proto = buildProcotol(r, slice, current);
      Prefix pfx = buildPrefix(r, f);
      // pick the next router
      boolean found = false;
      for (Entry<GraphEdge, BoolExpr> entry : dfwd.entrySet()) {
        GraphEdge ge = entry.getKey();
        BoolExpr dexpr = entry.getValue();
        BoolExpr cexpr = cfwd.get(ge);
        BoolExpr aexpr = across.get(ge);
        String route = buildRoute(pfx, proto, ge);
        if (isTrue(dexpr)) {
          hops.add(buildFlowTraceHop(ge, route));
          if (ge.getPeer() != null && visited.contains(ge.getPeer())) {
            FlowTrace ft = new FlowTrace(FlowDisposition.LOOP, hops, "LOOP");
            return new Tuple<>(f, ft);
          }
          if (isFalse(aexpr)) {
            Interface i = ge.getEnd();
            IpAccessList acl = i.getIncomingFilter();
            FilterResult fr = acl.filter(f);
            String line = "default deny";
            if (fr.getMatchLine() != null) {
              line = acl.getLines().get(fr.getMatchLine()).getName();
            }
            String note = String.format("DENIED_IN{%s}{%s}", acl.getName(), line);
            FlowTrace ft = new FlowTrace(FlowDisposition.DENIED_IN, hops, note);
            return new Tuple<>(f, ft);
          }
          boolean isLoopback = slice.getGraph().isLoopback(ge);
          if (isLoopback) {
            FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
            return new Tuple<>(f, ft);
          }
          if (ge.getPeer() == null) {
            boolean isBgpPeering = slice.getGraph().getEbgpNeighbors().get(ge) != null;
            if (isBgpPeering) {
              FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
              return new Tuple<>(f, ft);
            } else {
              FlowTrace ft =
                  new FlowTrace(
                      FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
                      hops,
                      "NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK");
              return new Tuple<>(f, ft);
            }
          }
          if (slice.getGraph().isHost(ge.getPeer())) {
            FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
            return new Tuple<>(f, ft);
          }

          current = ge.getPeer();
          found = true;
          break;

        } else if (isTrue(cexpr)) {
          hops.add(buildFlowTraceHop(ge, route));
          Interface i = ge.getStart();
          IpAccessList acl = i.getOutgoingFilter();
          FilterResult fr = acl.filter(f);
          IpAccessListLine line = acl.getLines().get(fr.getMatchLine());
          String note = String.format("DENIED_OUT{%s}{%s}", acl.getName(), line.getName());
          FlowTrace ft = new FlowTrace(FlowDisposition.DENIED_OUT, hops, note);
          return new Tuple<>(f, ft);
        }
      }
      if (!found) {
        BoolExpr permitted = r.getPermitted();
        if (boolVal(permitted)) {
          // Check if there is an accepting interface
          for (GraphEdge ge : slice.getGraph().getEdgeMap().get(current)) {
            Interface i = ge.getStart();
            Ip ip = i.getAddress().getIp();
            if (ip.equals(f.getDstIp())) {
              FlowTrace ft = new FlowTrace(FlowDisposition.ACCEPTED, hops, "ACCEPTED");
              return new Tuple<>(f, ft);
            }
          }
          FlowTrace ft =
              new FlowTrace(
                  FlowDisposition.NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK,
                  hops,
                  "NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK");
          return new Tuple<>(f, ft);
        }
        FlowTrace ft = new FlowTrace(FlowDisposition.NO_ROUTE, hops, "NO_ROUTE");
        return new Tuple<>(f, ft);
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
      Encoder enc,
      Map<String, Boolean> reach) {

    FlowHistory fh = new FlowHistory();
    for (String source : sourceRouters) {
      Boolean sourceVar = reach.get(source);
      if (!sourceVar) {
        Tuple<Flow, FlowTrace> tup = buildFlowTrace(enc, source);
        SortedSet<Edge> failedLinks = buildFailedLinks(enc);
        SortedSet<BgpAdvertisement> envRoutes = buildEnvRoutingTable(enc);
        Environment baseEnv =
            new Environment("BASE", testrigName, failedLinks, null, null, null, null, envRoutes);
        fh.addFlowTrace(tup.getFirst(), "BASE", baseEnv, tup.getSecond());
      }
    }
    return fh;
  }

  /*
   * Create a trace-based counterexample demonstrating
   * the difference between two networks on a single packet.
   */
  FlowHistory buildFlowHistoryDiff(
      String testRigName,
      Collection<String> sourceRouters,
      Encoder enc,
      Encoder enc2,
      Map<String, BoolExpr> reach,
      Map<String, BoolExpr> reach2) {

    FlowHistory fh = new FlowHistory();
    assert (reach2 != null);
    for (String source : sourceRouters) {
      BoolExpr sourceVar1 = reach.get(source);
      BoolExpr sourceVar2 = reach2.get(source);
      String val1 = evaluate(sourceVar1);
      String val2 = evaluate(sourceVar2);
      if (!Objects.equals(val1, val2)) {
        Tuple<Flow, FlowTrace> diff = buildFlowTrace(enc, source);
        Tuple<Flow, FlowTrace> base = buildFlowTrace(enc2, source);
        SortedSet<Edge> failedLinksDiff = buildFailedLinks(enc);
        SortedSet<Edge> failedLinksBase = buildFailedLinks(enc2);
        SortedSet<BgpAdvertisement> envRoutesDiff = buildEnvRoutingTable(enc);
        SortedSet<BgpAdvertisement> envRoutesBase = buildEnvRoutingTable(enc2);
        Environment baseEnv =
            new Environment(
                "BASE", testRigName, failedLinksBase, null, null, null, null, envRoutesBase);
        Environment failedEnv =
            new Environment(
                "DELTA", testRigName, failedLinksDiff, null, null, null, null, envRoutesDiff);
        fh.addFlowTrace(base.getFirst(), "BASE", baseEnv, base.getSecond());
        fh.addFlowTrace(diff.getFirst(), "DELTA", failedEnv, diff.getSecond());
      }
    }
    return fh;
  }
}
