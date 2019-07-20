package org.batfish.datamodel.eigrp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Ip;

/** Represents an EIGRP process on a router */
@ParametersAreNonnullByDefault
public final class EigrpProcess implements Serializable {
  private static final String PROP_ASN = "asn";
  private static final String PROP_EXPORT_POLICY = "exportPolicy";
  private static final String PROP_MODE = "eigrpMode";
  private static final String PROP_NEIGHBORS = "neighbors";
  private static final String PROP_ROUTER_ID = "routerId";

  private final long _asn;
  @Nullable private final String _exportPolicy;
  @Nonnull private final EigrpProcessMode _mode;
  @Nonnull private Map<String, EigrpNeighborConfig> _neighbors;
  @Nonnull private final Ip _routerId;

  private EigrpProcess(
      long asn,
      @Nullable String exportPolicy,
      EigrpProcessMode mode,
      Ip routerId,
      Map<String, EigrpNeighborConfig> neighbors) {
    _asn = asn;
    _exportPolicy = exportPolicy;
    _mode = mode;
    _neighbors = ImmutableMap.copyOf(neighbors);
    _routerId = routerId;
  }

  @JsonCreator
  private static EigrpProcess jsonCreator(
      @Nullable @JsonProperty(PROP_ASN) Long asn,
      @Nullable @JsonProperty(PROP_EXPORT_POLICY) String exportPolicy,
      @Nullable @JsonProperty(PROP_MODE) EigrpProcessMode mode,
      @Nullable @JsonProperty(PROP_NEIGHBORS) Map<String, EigrpNeighborConfig> neighbors,
      @Nullable @JsonProperty(PROP_ROUTER_ID) Ip routerId) {
    checkArgument(asn != null, "Missing %s", PROP_ASN);
    checkArgument(mode != null, "Missing %s", PROP_MODE);
    checkArgument(routerId != null, "Missing %s", PROP_ROUTER_ID);
    return new EigrpProcess(
        asn, exportPolicy, mode, routerId, firstNonNull(neighbors, ImmutableMap.of()));
  }

  public static Builder builder() {
    return new Builder();
  }

  /** @return The AS number for this process */
  @JsonProperty(PROP_ASN)
  public long getAsn() {
    return _asn;
  }

  /**
   * @return The routing policy applied to routes in the main RIB to determine which ones are
   *     exported into EIGRP and how
   */
  @Nullable
  @JsonProperty(PROP_EXPORT_POLICY)
  public String getExportPolicy() {
    return _exportPolicy;
  }

  /** @return All EIGRP neighbors in this process */
  @Nullable
  @JsonProperty(PROP_NEIGHBORS)
  public Map<String, EigrpNeighborConfig> getNeighbors() {
    return _neighbors;
  }

  /** @return The router-id of this EIGRP process */
  @Nonnull
  @JsonProperty(PROP_ROUTER_ID)
  public Ip getRouterId() {
    return _routerId;
  }

  /** @return The EIGRP mode for this process */
  @Nonnull
  @JsonProperty(PROP_MODE)
  public EigrpProcessMode getMode() {
    return _mode;
  }

  public void setNeighbors(@Nonnull Map<String, EigrpNeighborConfig> neighbors) {
    _neighbors = ImmutableMap.copyOf(neighbors);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EigrpProcess)) {
      return false;
    }
    EigrpProcess that = (EigrpProcess) o;
    return _asn == that._asn
        && Objects.equals(_exportPolicy, that._exportPolicy)
        && _mode == that._mode
        && _neighbors == that._neighbors
        && _routerId.equals(that._routerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_asn, _exportPolicy, _mode, _routerId, _neighbors);
  }

  public static class Builder {
    @Nullable private Long _asn;
    @Nullable private String _exportPolicy;
    @Nullable private EigrpProcessMode _mode;
    @Nullable private Map<String, EigrpNeighborConfig> _neighbors;
    @Nullable private Ip _routerId;

    private Builder() {}

    @Nonnull
    public EigrpProcess build() {
      checkArgument(_asn != null, "Missing %s", PROP_ASN);
      checkArgument(_mode != null, "Missing %s", PROP_MODE);
      checkArgument(_routerId != null, "Missing %s", PROP_ROUTER_ID);
      return new EigrpProcess(
          _asn, _exportPolicy, _mode, _routerId, firstNonNull(_neighbors, ImmutableMap.of()));
    }

    @Nonnull
    public Builder setAsNumber(long asn) {
      _asn = asn;
      return this;
    }

    @Nonnull
    public Builder setExportPolicy(@Nullable String exportPolicy) {
      _exportPolicy = exportPolicy;
      return this;
    }

    public Builder setNeighbors(@Nonnull Map<String, EigrpNeighborConfig> neighbors) {
      _neighbors = neighbors;
      return this;
    }

    @Nonnull
    public Builder setRouterId(Ip routerId) {
      _routerId = routerId;
      return this;
    }

    @Nonnull
    public Builder setMode(EigrpProcessMode mode) {
      _mode = mode;
      return this;
    }
  }
}
