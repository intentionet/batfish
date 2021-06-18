package org.batfish.representation.cisco_nxos;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represents the BGP configuration for an aggregate network configured for an address family in a
 * VRF.
 *
 * <p>Configuration entered using the {@code aggregate-address} command at the CLI {@code
 * config-router-af} or {@code config-router-vrf-af} levels.
 */
public class BgpVrfAddressFamilyAggregateNetworkConfiguration implements Serializable {

  public BgpVrfAddressFamilyAggregateNetworkConfiguration() {
    _asSet = false;
    _summaryOnly = false;
  }

  public BgpVrfAddressFamilyAggregateNetworkConfiguration(
      @Nullable String advertiseMap,
      boolean asSet,
      @Nullable String attributeMap,
      boolean summaryOnly,
      @Nullable String suppressMap) {
    _advertiseMap = advertiseMap;
    _asSet = asSet;
    _attributeMap = attributeMap;
    _summaryOnly = summaryOnly;
    _suppressMap = suppressMap;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BgpVrfAddressFamilyAggregateNetworkConfiguration)) {
      return false;
    }
    BgpVrfAddressFamilyAggregateNetworkConfiguration that =
        (BgpVrfAddressFamilyAggregateNetworkConfiguration) o;
    return Objects.equals(_advertiseMap, that._advertiseMap)
        && _asSet == that._asSet
        && Objects.equals(_attributeMap, that._attributeMap)
        && _summaryOnly == that._summaryOnly
        && Objects.equals(_suppressMap, that._suppressMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_advertiseMap, _asSet, _attributeMap, _summaryOnly, _suppressMap);
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .omitNullValues()
        .add("_asSet", _asSet)
        .add("_advertiseMap", _advertiseMap)
        .add("_attributeMap", _attributeMap)
        .add("_summaryOnly", _summaryOnly)
        .add("_suppressMap", _suppressMap)
        .toString();
  }

  @Nullable
  public String getAdvertiseMap() {
    return _advertiseMap;
  }

  public void setAdvertiseMap(@Nullable String advertiseMap) {
    _advertiseMap = advertiseMap;
  }

  public boolean getAsSet() {
    return _asSet;
  }

  public void setAsSet(boolean asSet) {
    _asSet = asSet;
  }

  @Nullable
  public String getAttributeMap() {
    return _attributeMap;
  }

  public void setAttributeMap(@Nullable String attributeMap) {
    _attributeMap = attributeMap;
  }

  public boolean getSummaryOnly() {
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

  private boolean _asSet;
  @Nullable private String _advertiseMap;
  @Nullable private String _attributeMap;
  private boolean _summaryOnly;
  @Nullable private String _suppressMap;
}
