package org.batfish.representation.cumulus;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.batfish.datamodel.OriginType;

/**
 * Represents the BGP configuration for an aggregate network configured for an address family in a
 * VRF.
 *
 * <p>Configured using the {@code aggregate-address} command in {@code /etc/frr/frr.conf}.
 */
public final class BgpVrfAddressFamilyAggregateNetworkConfiguration implements Serializable {
  private boolean _asSet;
  private boolean _matchingMedOnly;
  private @Nullable OriginType _origin;
  private @Nullable String _routeMap;
  private boolean _summaryOnly;
  private @Nullable String _suppressMap;

  public BgpVrfAddressFamilyAggregateNetworkConfiguration() {}

  public BgpVrfAddressFamilyAggregateNetworkConfiguration(
      boolean asSet,
      boolean matchingMedOnly,
      @Nullable OriginType origin,
      @Nullable String routeMap,
      boolean summaryOnly,
      @Nullable String suppressMap) {
    _asSet = asSet;
    _matchingMedOnly = matchingMedOnly;
    _origin = origin;
    _routeMap = routeMap;
    _summaryOnly = summaryOnly;
    _suppressMap = suppressMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof BgpVrfAddressFamilyAggregateNetworkConfiguration)) {
      return false;
    }
    BgpVrfAddressFamilyAggregateNetworkConfiguration that =
        (BgpVrfAddressFamilyAggregateNetworkConfiguration) o;
    return _asSet == that._asSet
        && _matchingMedOnly == that._matchingMedOnly
        && _origin == that._origin
        && Objects.equals(_routeMap, that._routeMap)
        && _summaryOnly == that._summaryOnly
        && Objects.equals(_suppressMap, that._suppressMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_asSet, _matchingMedOnly, _origin, _routeMap, _summaryOnly, _suppressMap);
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .omitNullValues()
        .add("_asSet", _asSet)
        .add("_matchingMedOnly", _matchingMedOnly)
        .add("_origin", _origin)
        .add("_routeMap", _routeMap)
        .add("_summaryOnly", _summaryOnly)
        .add("_suppressMap", _suppressMap)
        .toString();
  }

  public boolean isAsSet() {
    return _asSet;
  }

  public void setAsSet(boolean asSet) {
    _asSet = asSet;
  }

  public boolean isMatchingMedOnly() {
    return _matchingMedOnly;
  }

  public void setMatchingMedOnly(boolean matchingMedOnly) {
    _matchingMedOnly = matchingMedOnly;
  }

  @Nullable
  public OriginType getOrigin() {
    return _origin;
  }

  public void setOrigin(@Nullable OriginType origin) {
    _origin = origin;
  }

  @Nullable
  public String getRouteMap() {
    return _routeMap;
  }

  public void setRouteMap(@Nullable String routeMap) {
    _routeMap = routeMap;
  }

  public boolean isSummaryOnly() {
    return _summaryOnly;
  }

  public void setSummaryOnly(boolean summaryOnly) {
    _summaryOnly = summaryOnly;
  }

  @Nullable
  public String getSuppressMap() {
    return _suppressMap;
  }

  public void setSuppressMap(@Nullable String suppressMap) {
    _suppressMap = suppressMap;
  }
}
