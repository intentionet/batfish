package org.batfish.datamodel.packet_policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Drop the packet */
@ParametersAreNonnullByDefault
public final class Drop implements Statement, Action {
  private static final long serialVersionUID = 1L;
  private static final Drop INSTANCE = new Drop();

  private Drop() {}

  @Override
  public <T> T accept(PacketPolicyVisitor<T> visitor) {
    return visitor.visitDrop(this);
  }

  @JsonCreator
  public static Drop instance() {
    return INSTANCE;
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof Drop;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).toString();
  }
}
