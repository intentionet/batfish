package org.batfish.symbolic.abstraction;

import javax.annotation.Nullable;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.symbolic.Graph;

public class EquivalenceClass {

  private Graph _graph;

  private HeaderSpace _headerSpace;

  private AbstractionMap _abstraction;

  public EquivalenceClass(
      HeaderSpace headerSpace, Graph graph, @Nullable AbstractionMap abstraction) {
    this._headerSpace = headerSpace;
    this._graph = graph;
    this._abstraction = abstraction;
  }

  public Graph getGraph() {
    return _graph;
  }

  public HeaderSpace getHeaderSpace() {
    return _headerSpace;
  }

  public AbstractionMap getAbstraction() {
    return _abstraction;
  }
}
