package org.batfish.symbolic.smt;

import com.microsoft.z3.BoolExpr;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;
import org.batfish.symbolic.collections.Table2;
import org.batfish.symbolic.collections.Table3;

/**
 * Class for the symbolic variables used to represent the final forwarding decision in the network.
 * This includes both the control plane and data plane forwading decisions, as well as the
 * per-protocol and overall best choices.
 *
 * @author Ryan Beckett
 */
class SymbolicDecisions {

  private Table2<String, Protocol, SymbolicRoute> _bestNeighborPerProtocol;

  private Map<String, SymbolicRoute> _bestNeighbor;

  private Table3<String, Protocol, LogicalEdge, BoolExpr> _choiceVariables;

  private Table2<String, GraphEdge, BoolExpr> _controlForwarding;

  private Table2<String, GraphEdge, BoolExpr> _dataForwarding;

  SymbolicDecisions() {
    _bestNeighbor = new HashMap<>();
    _bestNeighborPerProtocol = new Table2<>();
    _choiceVariables = new Table3<>();
    _controlForwarding = new Table2<>();
    _dataForwarding = new Table2<>();
  }

  Table2<String, Protocol, SymbolicRoute> getBestNeighborPerProtocol() {
    return _bestNeighborPerProtocol;
  }

  Map<String, SymbolicRoute> getBestNeighbor() {
    return _bestNeighbor;
  }

  Table3<String, Protocol, LogicalEdge, BoolExpr> getChoiceVariables() {
    return _choiceVariables;
  }

  Table2<String, GraphEdge, BoolExpr> getControlForwarding() {
    return _controlForwarding;
  }

  Table2<String, GraphEdge, BoolExpr> getDataForwarding() {
    return _dataForwarding;
  }

  @Nullable
  SymbolicRoute getBestVars(Optimizations opts, String router, Protocol proto) {
    if (opts.getSliceHasSingleProtocol().contains(router)) {
      return _bestNeighbor.get(router);
    } else {
      return _bestNeighborPerProtocol.get(router, proto);
    }
  }
}
