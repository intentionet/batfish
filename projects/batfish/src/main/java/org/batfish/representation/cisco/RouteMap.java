package org.batfish.representation.cisco;

import java.util.NavigableMap;
import java.util.TreeMap;
import org.batfish.common.util.ComparableStructure;

public class RouteMap extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  private NavigableMap<Integer, RouteMapClause> _clauses;

  public RouteMap(String name) {
    super(name);
    _clauses = new TreeMap<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof RouteMap)) {
      return false;
    }

    RouteMap other = (RouteMap) o;

    // TODO: replace with a sensible implementation
    return _name.equals(other._name);
  }

  public NavigableMap<Integer, RouteMapClause> getClauses() {
    return _clauses;
  }

  @Override
  public int hashCode() {
    // TODO: replace with a sensible implementation
    return _name.hashCode();
  }
}
