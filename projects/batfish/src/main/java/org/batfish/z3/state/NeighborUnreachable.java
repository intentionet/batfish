package org.batfish.z3.state;

public class NeighborUnreachable implements StateExpr {

  public static final NeighborUnreachable INSTANCE = new NeighborUnreachable();

  private NeighborUnreachable() {}

  @Override
  public <R> R accept(StateExprVisitor<R> visitor) {
    return visitor.visitNeighborUnreachable();
  }
}
