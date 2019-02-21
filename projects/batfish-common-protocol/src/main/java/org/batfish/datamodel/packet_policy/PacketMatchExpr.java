package org.batfish.datamodel.packet_policy;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.acl.AclLineMatchExpr;

@ParametersAreNonnullByDefault
public final class PacketMatchExpr implements Statement {
  private static final long serialVersionUID = 1L;

  private static final String PROP_EXPR = "expression";

  private final AclLineMatchExpr _expr;

  public PacketMatchExpr(AclLineMatchExpr expr) {
    _expr = expr;
  }

  @JsonCreator
  private static PacketMatchExpr jsonCreator(
      @Nullable @JsonProperty(PROP_EXPR) AclLineMatchExpr expr) {
    checkArgument(expr != null, "Missing %s", PROP_EXPR);
    return new PacketMatchExpr(expr);
  }

  @Override
  public <T> T accept(PacketPolicyVisitor<T> visitor) {
    return visitor.visitPacketMatchExpr(this);
  }

  @JsonProperty(PROP_EXPR)
  public AclLineMatchExpr getExpr() {
    return _expr;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PacketMatchExpr that = (PacketMatchExpr) o;
    return Objects.equals(getExpr(), that.getExpr());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getExpr());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("expr", _expr).toString();
  }
}
