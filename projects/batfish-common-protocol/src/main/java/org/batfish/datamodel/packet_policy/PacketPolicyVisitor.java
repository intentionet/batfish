package org.batfish.datamodel.packet_policy;

/**
 * All evaluators of {@link PacketPolicy} must implement this interface to correctly handle
 * available statemnt types.
 */
public interface PacketPolicyVisitor<T> {
  default T visit(Statement step) {
    return step.accept(this);
  }

  T visitDrop(Drop drop);

  T visitFibLookup(FibLookup fibLookup);

  T visitIf(If ifExpr);

  T visitNoop(Noop noop);

  T visitPacketMatchExpr(PacketMatchExpr expr);
}
