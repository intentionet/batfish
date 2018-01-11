package org.batfish.symbolic.smt;

import java.util.Objects;
import org.batfish.symbolic.GraphEdge;

/**
 * A logical edge in the network graph. Wraps a graph edge with additional information about the
 * type of the edge (Import/Export) as well as the record of symbolic variables.
 *
 * <p>We will have two LogicalEdges for each GraphEdge: one for the route that's imported on the
 * port, one for the route that's exported to the port.
 *
 * @author Ryan Beckett
 */
class LogicalEdge {

  private GraphEdge _edge;

  private EdgeType _type;

  private SymbolicRoute _symbolicRoute;

  LogicalEdge(GraphEdge edge, EdgeType type, SymbolicRoute symbolicRoute) {
    _edge = edge;
    _type = type;
    _symbolicRoute = symbolicRoute;
  }

  EdgeType getEdgeType() {
    return _type;
  }

  SymbolicRoute getSymbolicRecord() {
    return _symbolicRoute;
  }

  GraphEdge getEdge() {
    return _edge;
  }

  boolean isAbstract() {
    return _edge.isAbstract();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LogicalEdge)) {
      return false;
    }
    LogicalEdge other = (LogicalEdge) o;
    return _type == other._type
        && Objects.equals(_edge, other._edge)
        && Objects.equals(_symbolicRoute, other._symbolicRoute);
  }

  @Override
  public int hashCode() {
    int result = _edge != null ? _edge.hashCode() : 0;
    result = 31 * result + (_type != null ? (_type == EdgeType.EXPORT ? 2 : 1) : 0);
    result = 31 * result + (_symbolicRoute != null ? _symbolicRoute.hashCode() : 0);
    return result;
  }
}
