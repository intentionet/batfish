package org.batfish.datamodel.packet_policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Action to do nothing/continue on to the next statement */
@ParametersAreNonnullByDefault
public final class Noop implements Statement, Action {

  private static final long serialVersionUID = 1L;
  private static final Noop INSTANCE = new Noop();

  private Noop() {}

  @Override
  public <T> T accept(PacketPolicyVisitor<T> visitor) {
    return visitor.visitNoop(this);
  }

  @JsonCreator
  public static Noop instance() {
    return INSTANCE;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof Noop;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).toString();
  }
}
