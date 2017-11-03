package org.batfish.symbolic.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper class that simplifies adding variables to the network model. mkIf debugging is enabled,
 * then it creates new variables to track the state of the other variables in the model. This allows
 * the solver to create a minimal unsat core, which is useful for debugging.
 *
 * @author Ryan Beckett
 */
class UnsatCore {

  private boolean _doTrack;

  private Map<String, BoolExpr> _trackingVars;

  private int _trackingNum;

  UnsatCore(boolean doTrack) {
    _doTrack = doTrack;
    _trackingVars = new HashMap<>();
    _trackingNum = 0;
  }

  void track(Solver solver, Context ctx, BoolExpr be) {
    String name = "Pred" + _trackingNum;
    _trackingNum = _trackingNum + 1;
    _trackingVars.put(name, be);
    if (_doTrack) {
      solver.assertAndTrack(be, ctx.mkBoolConst(name));
    } else {
      solver.add(be);
    }
  }

  boolean getDoTrack() {
    return _doTrack;
  }

  Map<String, BoolExpr> getTrackingVars() {
    return _trackingVars;
  }
}
