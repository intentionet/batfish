package org.batfish.symbolic.smt;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;

/**
 * Instruments the network model with additional information that is useful for checking other
 * properties. This information can be things like reachability, path length, load, etc.
 *
 * @author Ryan Beckett
 */
class PropertyAdder {

  private EncoderSlice _encoderSlice;

  PropertyAdder(EncoderSlice encoderSlice) {
    _encoderSlice = encoderSlice;
  }

  static BoolExpr allEqual(Context ctx, List<Expr> exprs) {
    BoolExpr acc = ctx.mkBool(true);
    if (exprs.size() > 1) {
      for (int i = 0; i < exprs.size() - 1; i++) {
        Expr x = exprs.get(i);
        Expr y = exprs.get(i + 1);
        acc = ctx.mkAnd(acc, ctx.mkEq(x, y));
      }
    }
    return acc;
  }

  /*
   * Initialize reachability and id variables an add the constraints
   * that all ids are at least 0 or higher, and reachable means non-zero.
   */
  private void initializeReachabilityVars(
      EncoderSlice slice,
      Context ctx,
      Solver solver,
      Map<String, BoolExpr> reachableVars,
      Map<String, ArithExpr> idVars) {

    String sliceName = slice.getSliceName();
    ArithExpr zero = ctx.mkInt(0);
    for (String r : _encoderSlice.getGraph().getRouters()) {
      int id = _encoderSlice.getEncoder().getId();
      String s1 = id + "_" + sliceName + "_reachable-id_" + r;
      String s2 = id + "_" + sliceName + "_reachable_" + r;
      ArithExpr idVar = ctx.mkIntConst(s1);
      BoolExpr var = ctx.mkBoolConst(s2);
      idVars.put(r, idVar);
      reachableVars.put(r, var);
      _encoderSlice.getAllVariables().put(idVar.toString(), idVar);
      _encoderSlice.getAllVariables().put(var.toString(), var);
      solver.add(ctx.mkEq(var, ctx.mkGt(idVar, zero)));
      solver.add(ctx.mkGe(idVar, zero));
    }
  }

  /*
   * Generates constraints for reachability through some neighbor.
   * If a router forwards to some neighbor with id label > 0, then the id of this router is
   * greater than that of all next hops. This prevents considering loops and also means that
   * we interpret the router as reachable. If there is no such neighbor, then this router is
   * not reachable and we set the id to 0.
   */
  private BoolExpr recursiveReachability(
      Context ctx,
      EncoderSlice slice,
      List<GraphEdge> edges,
      Map<String, ArithExpr> idVars,
      String router,
      ArithExpr id) {

    ArithExpr zero = ctx.mkInt(0);
    BoolExpr hasRecursiveRoute = ctx.mkFalse();
    BoolExpr largerIds = ctx.mkTrue();
    for (GraphEdge edge : edges) {
      if (!edge.isAbstract()) {
        BoolExpr fwd = _encoderSlice.getForwardsAcross().get(router, edge);
        if (edge.getPeer() != null) {
          ArithExpr peerId = idVars.get(edge.getPeer());
          BoolExpr peerReachable = ctx.mkGt(peerId, zero);
          BoolExpr sendToReachable = ctx.mkAnd(fwd, peerReachable);
          hasRecursiveRoute = ctx.mkOr(hasRecursiveRoute, sendToReachable);
          BoolExpr increasingId = ctx.mkImplies(sendToReachable, ctx.mkGt(id, peerId));
          largerIds = ctx.mkAnd(largerIds, increasingId);
        }
      }
    }
    return slice.mkIf(hasRecursiveRoute, largerIds, ctx.mkEq(id, zero));
  }

  /*
   * Add reachability information to the network for a destination edge.
   * Each router will have a boolean variable determining if it can reach
   * the destination. A router is reachable if it has some neighbor that
   * is also reachable.
   */
  Map<String, BoolExpr> instrumentReachability(Set<GraphEdge> ges) {
    Context ctx = _encoderSlice.getCtx();
    Solver solver = _encoderSlice.getSolver();
    EncoderSlice slice = _encoderSlice;
    Map<String, BoolExpr> reachableVars = new HashMap<>();
    Map<String, ArithExpr> idVars = new HashMap<>();
    initializeReachabilityVars(slice, ctx, solver, reachableVars, idVars);
    Graph g = _encoderSlice.getGraph();

    for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
      String router = entry.getKey();
      List<GraphEdge> edges = entry.getValue();
      ArithExpr id = idVars.get(router);
      // Add the base case, reachable if we forward to a directly connected interface
      BoolExpr hasDirectRoute = ctx.mkFalse();
      BoolExpr isAbsorbed = ctx.mkFalse();
      SymbolicRoute r = _encoderSlice.getBestNeighborPerProtocol(router, Protocol.CONNECTED);

      for (GraphEdge ge : edges) {
        if (!ge.isAbstract() && ges.contains(ge)) {
          // If a host, consider reachable
          if (g.isHost(router)) {
            hasDirectRoute = ctx.mkTrue();
            break;
          }
          // Reachable if we leave the network
          if (ge.getPeer() == null) {
            BoolExpr fwdIface = _encoderSlice.getForwardsAcross().get(ge.getRouter(), ge);
            assert (fwdIface != null);
            hasDirectRoute = ctx.mkOr(hasDirectRoute, fwdIface);
          }
          // Also reachable if connected route and we use it despite not forwarding
          if (r != null) {
            BitVecExpr dstIp = _encoderSlice.getSymbolicPacket().getDstIp();
            BitVecExpr ip = ctx.mkBV(ge.getStart().getAddress().getIp().asLong(), 32);
            BoolExpr reach = ctx.mkAnd(r.getPermitted(), ctx.mkEq(dstIp, ip));
            isAbsorbed = ctx.mkOr(isAbsorbed, reach);
          }
        }
      }
      // Add the recursive case, where it is reachable through a neighbor
      BoolExpr recursive = recursiveReachability(ctx, slice, edges, idVars, router, id);
      BoolExpr guard = ctx.mkOr(hasDirectRoute, isAbsorbed);
      BoolExpr cond = slice.mkIf(guard, ctx.mkEq(id, ctx.mkInt(1)), recursive);
      solver.add(cond);
    }

    return reachableVars;
  }

  /*
   * Also instruments reachability, but to a destination router
   * rather than a destination port.
   */
  Map<String, BoolExpr> instrumentReachability(String router) {
    Context ctx = _encoderSlice.getCtx();
    Solver solver = _encoderSlice.getSolver();
    Map<String, BoolExpr> reachableVars = new HashMap<>();
    Map<String, ArithExpr> idVars = new HashMap<>();
    initializeReachabilityVars(_encoderSlice, ctx, solver, reachableVars, idVars);

    ArithExpr baseId = idVars.get(router);
    _encoderSlice.add(ctx.mkEq(baseId, ctx.mkInt(1)));

    Graph g = _encoderSlice.getGraph();
    for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
      String r = entry.getKey();
      List<GraphEdge> edges = entry.getValue();
      if (!r.equals(router)) {
        ArithExpr id = idVars.get(r);
        BoolExpr cond = recursiveReachability(ctx, _encoderSlice, edges, idVars, r, id);
        solver.add(cond);
      }
    }

    return reachableVars;
  }

  // Potentially useful in the future to optimize reachability when we know
  // that there can't be routing loops e.g., due to a preliminary static analysis

  /* public Map<String, BoolExpr> instrumentReachabilityFast(String router) {
    Context ctx = _encoderSlice.getCtx();
    Solver solver = _encoderSlice.getSolver();
    Map<String, BoolExpr> reachableVars = new HashMap<>();
    String sliceName = _encoderSlice.getSliceName();
    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (r, conf) -> {
              int id = _encoderSlice.getEncoder().getId();
              String s2 = id + "_" + sliceName + "_reachable_" + r;
              BoolExpr var = ctx.mkBoolConst(s2);
              reachableVars.put(r, var);
              _encoderSlice.getAllVariables().put(var.toString(), var);
            });

    BoolExpr baseReach = reachableVars.get(router);
    _encoderSlice.add(baseReach);
    _encoderSlice
        .getGraph()
        .getEdgeMap()
        .forEach(
            (r, edges) -> {
              if (!r.equals(router)) {
                BoolExpr reach = reachableVars.get(r);
                BoolExpr hasRecursiveRoute = ctx.mkFalse();
                for (GraphEdge edge : edges) {
                  if (!edge.isAbstract()) {
                    BoolExpr fwd = _encoderSlice.getForwardsAcross().get(r, edge);
                    if (edge.getPeer() != null) {
                      BoolExpr peerReachable = reachableVars.get(edge.getPeer());
                      BoolExpr sendToReachable = ctx.mkAnd(fwd, peerReachable);
                      hasRecursiveRoute = ctx.mkOr(hasRecursiveRoute, sendToReachable);
                    }
                  }
                }
                solver.add(ctx.mkEq(reach, hasRecursiveRoute));
              }
            });

    return reachableVars;
  }

  public Map<String, BoolExpr> instrumentReachabilityFast(Set<GraphEdge> ges) {
    Context ctx = _encoderSlice.getCtx();
    Solver solver = _encoderSlice.getSolver();
    EncoderSlice slice = _encoderSlice;
    String sliceName = _encoderSlice.getSliceName();
    Graph g = slice.getGraph();
    Map<String, BoolExpr> reachableVars = new HashMap<>();

    _encoderSlice
        .getGraph()
        .getConfigurations()
        .forEach(
            (r, conf) -> {
              int id = _encoderSlice.getEncoder().getId();
              String s2 = id + "_" + sliceName + "_reachable_" + r;
              BoolExpr var = ctx.mkBoolConst(s2);
              reachableVars.put(r, var);
              _encoderSlice.getAllVariables().put(var.toString(), var);
            });

    for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
      String router = entry.getKey();
      List<GraphEdge> edges = entry.getRegex();
      BoolExpr reach = reachableVars.get(router);

      // Add the base case, reachable if we forward to a directly connected interface
      BoolExpr hasDirectRoute = ctx.mkFalse();
      BoolExpr isAbsorbed = ctx.mkFalse();
      SymbolicRoute r = _encoderSlice.getBestNeighborPerProtocol(router, Protocol.CONNECTED);

      for (GraphEdge ge : edges) {
        if (!ge.isAbstract() && ges.contains(ge)) {
          // If a host, consider reachable
          if (g.isHost(router)) {
            hasDirectRoute = ctx.mkTrue();
            break;
          }
          // Reachable if we leave the network
          if (ge.getPeer() == null) {
            BoolExpr fwdIface = _encoderSlice.getForwardsAcross().get(ge.getRouter(), ge);
            assert (fwdIface != null);
            hasDirectRoute = ctx.mkOr(hasDirectRoute, fwdIface);
          }
          // Also reachable if connected route and we use it despite not forwarding
          if (r != null) {
            BitVecExpr dstIp = _encoderSlice.getSymbolicPacket().getDstIp();
            BitVecExpr ip = ctx.mkBV(ge.getStart().getIp().getIp().asLong(), 32);
            BoolExpr reachable = ctx.mkAnd(r.getPermitted(), ctx.mkEq(dstIp, ip));
            isAbsorbed = ctx.mkOr(isAbsorbed, reachable);
          }
        }
      }

      // Add the recursive case, where it is reachable through a neighbor
      BoolExpr hasRecursiveRoute = ctx.mkFalse();
      for (GraphEdge edge : edges) {
        if (!edge.isAbstract()) {
          BoolExpr fwd = _encoderSlice.getForwardsAcross().get(router, edge);
          if (edge.getPeer() != null) {
            BoolExpr peerReachable = reachableVars.get(edge.getPeer());
            BoolExpr sendToReachable = ctx.mkAnd(fwd, peerReachable);
            hasRecursiveRoute = ctx.mkOr(hasRecursiveRoute, sendToReachable);
          }
        }
      }

      BoolExpr cond = slice.mkOr(hasDirectRoute, isAbsorbed, hasRecursiveRoute);
      solver.add(slice.mkEq(reach, cond));
    }

    return reachableVars;
  } */

  /*
   * Instruments the network with path length information to a
   * destination port corresponding to a graph edge ge.
   * A router has a path of length n if some neighbor has a path
   * with length n-1.
   */
  Map<String, ArithExpr> instrumentPathLength(Set<GraphEdge> ges) {
    Context ctx = _encoderSlice.getCtx();
    Solver solver = _encoderSlice.getSolver();
    String sliceName = _encoderSlice.getSliceName();

    // Initialize path length variables
    Graph graph = _encoderSlice.getGraph();
    Map<String, ArithExpr> lenVars = new HashMap<>();
    for (String router : graph.getRouters()) {
      String name = _encoderSlice.getEncoder().getId() + "_" + sliceName + "_path-length_" + router;
      ArithExpr var = ctx.mkIntConst(name);
      lenVars.put(router, var);
      _encoderSlice.getAllVariables().put(var.toString(), var);
    }

    ArithExpr zero = ctx.mkInt(0);
    ArithExpr one = ctx.mkInt(1);
    ArithExpr minusOne = ctx.mkInt(-1);

    // Lower bound for all lengths
    lenVars.forEach((name, var) -> solver.add(ctx.mkGe(var, minusOne)));
    for (Entry<String, List<GraphEdge>> entry : graph.getEdgeMap().entrySet()) {
      String router = entry.getKey();
      List<GraphEdge> edges = entry.getValue();
      ArithExpr length = lenVars.get(router);

      // If there is a direct route, then we have length 0
      BoolExpr hasDirectRoute = ctx.mkFalse();
      BoolExpr isAbsorbed = ctx.mkFalse();
      SymbolicRoute r = _encoderSlice.getBestNeighborPerProtocol(router, Protocol.CONNECTED);

      for (GraphEdge ge : edges) {
        if (!ge.isAbstract() && ges.contains(ge)) {
          // Reachable if we leave the network
          if (ge.getPeer() == null) {
            BoolExpr fwdIface = _encoderSlice.getForwardsAcross().get(ge.getRouter(), ge);
            assert (fwdIface != null);
            hasDirectRoute = ctx.mkOr(hasDirectRoute, fwdIface);
          }
          // Also reachable if connected route and we use it despite not forwarding
          if (r != null) {
            BitVecExpr dstIp = _encoderSlice.getSymbolicPacket().getDstIp();
            BitVecExpr ip = ctx.mkBV(ge.getStart().getAddress().getIp().asLong(), 32);
            BoolExpr reach = ctx.mkAnd(r.getPermitted(), ctx.mkEq(dstIp, ip));
            isAbsorbed = ctx.mkOr(isAbsorbed, reach);
          }
        }
      }

      // Otherwise, we find length recursively
      BoolExpr accNone = ctx.mkTrue();
      BoolExpr accSome = ctx.mkFalse();
      for (GraphEdge edge : edges) {
        if (!edge.isAbstract() && edge.getPeer() != null) {
          BoolExpr dataFwd = _encoderSlice.getForwardsAcross().get(router, edge);
          assert (dataFwd != null);
          ArithExpr peerLen = lenVars.get(edge.getPeer());
          accNone = ctx.mkAnd(accNone, ctx.mkOr(ctx.mkLt(peerLen, zero), ctx.mkNot(dataFwd)));
          ArithExpr newVal = ctx.mkAdd(peerLen, one);
          BoolExpr fwd = ctx.mkAnd(ctx.mkGe(peerLen, zero), dataFwd, ctx.mkEq(length, newVal));
          accSome = ctx.mkOr(accSome, fwd);
        }
      }

      BoolExpr guard = _encoderSlice.mkOr(hasDirectRoute, isAbsorbed);
      BoolExpr cond1 = _encoderSlice.mkIf(accNone, ctx.mkEq(length, minusOne), accSome);
      BoolExpr cond2 = _encoderSlice.mkIf(guard, ctx.mkEq(length, zero), cond1);
      solver.add(cond2);
    }

    return lenVars;
  }

  /*
   * Instruments the network with load balancing information to destination
   * port for graph edge ge. Each router will split load according to the
   * number of neighbors it actively uses to get to ge.
   */
  Map<String, ArithExpr> instrumentLoad(Set<GraphEdge> ges) {
    Context ctx = _encoderSlice.getCtx();
    Solver solver = _encoderSlice.getSolver();
    String sliceName = _encoderSlice.getSliceName();

    Map<String, ArithExpr> loadVars = new HashMap<>();
    Graph graph = _encoderSlice.getGraph();
    for (String router : graph.getRouters()) {
      String name = _encoderSlice.getEncoder().getId() + "_" + sliceName + "_load_" + router;
      ArithExpr var = ctx.mkIntConst(name);
      loadVars.put(router, var);
      _encoderSlice.getAllVariables().put(var.toString(), var);
    }

    loadVars.forEach((name, var) -> solver.add(ctx.mkGe(var, ctx.mkInt(0))));
    ArithExpr zero = ctx.mkInt(0);
    ArithExpr one = ctx.mkInt(1);

    for (Entry<String, List<GraphEdge>> entry : graph.getEdgeMap().entrySet()) {
      String router = entry.getKey();
      List<GraphEdge> edges = entry.getValue();

      ArithExpr load = loadVars.get(router);
      BoolExpr hasDirectRoute = ctx.mkFalse();
      BoolExpr isAbsorbed = ctx.mkFalse();
      SymbolicRoute r = _encoderSlice.getBestNeighborPerProtocol(router, Protocol.CONNECTED);

      for (GraphEdge ge : edges) {
        if (!ge.isAbstract() && ges.contains(ge)) {
          // if we leave the network
          if (ge.getPeer() == null) {
            BoolExpr fwdIface = _encoderSlice.getForwardsAcross().get(ge.getRouter(), ge);
            assert (fwdIface != null);
            hasDirectRoute = ctx.mkOr(hasDirectRoute, fwdIface);
          }
          // if connected route and we use it despite not forwarding
          if (r != null) {
            BitVecExpr dstIp = _encoderSlice.getSymbolicPacket().getDstIp();
            BitVecExpr ip = ctx.mkBV(ge.getStart().getAddress().getIp().asLong(), 32);
            BoolExpr reach = ctx.mkAnd(r.getPermitted(), ctx.mkEq(dstIp, ip));
            isAbsorbed = ctx.mkOr(isAbsorbed, reach);
          }
        }
      }

      ArithExpr acc = ctx.mkInt(0);
      for (GraphEdge edge : edges) {
        if (!edge.isAbstract()) {
          BoolExpr fwd = _encoderSlice.getForwardsAcross().get(router, edge);
          assert (fwd != null);
          if (edge.getPeer() != null) {
            ArithExpr peerLoad = loadVars.get(edge.getPeer());
            ArithExpr x = (ArithExpr) ctx.mkITE(fwd, peerLoad, zero);
            acc = ctx.mkAdd(acc, x);
          }
        }
      }
      solver.add(ctx.mkEq(load, acc));

      BoolExpr guard = _encoderSlice.mkOr(hasDirectRoute, isAbsorbed);
      BoolExpr cond = _encoderSlice.mkIf(guard, ctx.mkEq(load, one), ctx.mkEq(load, acc));
      solver.add(cond);
    }

    return loadVars;
  }

  /*
   * Instruments the network to check if a router will be part
   * of a routing loop.
   */
  BoolExpr instrumentLoop(String router) {
    Context ctx = _encoderSlice.getCtx();
    Solver solver = _encoderSlice.getSolver();
    String sliceName = _encoderSlice.getSliceName();

    // Add on-loop variables to track a loop
    Map<String, BoolExpr> onLoop = new HashMap<>();
    Graph graph = _encoderSlice.getGraph();

    for (String r : graph.getRouters()) {
      String name =
          _encoderSlice.getEncoder().getId() + "_" + sliceName + "_on-loop_" + router + "_" + r;
      BoolExpr var = ctx.mkBoolConst(name);
      onLoop.put(r, var);
      _encoderSlice.getAllVariables().put(var.toString(), var);
    }

    for (Entry<String, List<GraphEdge>> entry : graph.getEdgeMap().entrySet()) {
      String r = entry.getKey();
      List<GraphEdge> edges = entry.getValue();
      BoolExpr var = onLoop.get(r);
      BoolExpr acc = ctx.mkBool(false);
      for (GraphEdge edge : edges) {
        if (!edge.isAbstract()) {
          BoolExpr fwd = _encoderSlice.getForwardsAcross().get(r, edge);
          String peer = edge.getPeer();
          if (peer != null) {
            if (peer.equals(router)) {
              // If next hop is static route router, then on loop
              acc = ctx.mkOr(acc, fwd);
            } else {
              // Otherwise check if next hop also is on the loop
              BoolExpr peerOnLoop = onLoop.get(peer);
              acc = ctx.mkOr(acc, ctx.mkAnd(fwd, peerOnLoop));
            }
          }
        }
      }
      solver.add(ctx.mkEq(var, acc));
    }

    return onLoop.get(router);
  }
}
