package org.batfish.datamodel.packet_policy;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Switch to a regular destination-based forwarding pipeline. That is, perform a FIB lookup in a
 * given VRF, use the results to forward packet.
 */
@ParametersAreNonnullByDefault
public final class FibLookup implements Statement, Action {

  private static final long serialVersionUID = 1L;

  private static final String PROP_VRF_NAME = "vrfName";

  @Nonnull private final String _vrfName;

  public FibLookup(String vrfName) {
    _vrfName = vrfName;
  }

  @JsonCreator
  private static FibLookup jsonCreator(@Nullable @JsonProperty(PROP_VRF_NAME) String vrfName) {
    checkArgument(vrfName != null, "Missing %s", PROP_VRF_NAME);
    return new FibLookup(vrfName);
  }

  @JsonProperty(PROP_VRF_NAME)
  @Nonnull
  public String getVrfName() {
    return _vrfName;
  }

  @Override
  public <T> T accept(PacketPolicyVisitor<T> visitor) {
    return visitor.visitFibLookup(this);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FibLookup fibLookup = (FibLookup) o;
    return Objects.equals(getVrfName(), fibLookup.getVrfName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getVrfName());
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("vrfName", _vrfName).toString();
  }
}
