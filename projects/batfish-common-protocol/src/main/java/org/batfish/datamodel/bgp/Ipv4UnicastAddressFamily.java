package org.batfish.datamodel.bgp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.datamodel.bgp.AddressFamilySettings.PROP_ADDRESS_FAMILY_SETTINGS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Objects;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Configuration for an IPv4 address family */
@ParametersAreNonnullByDefault
public final class Ipv4UnicastAddressFamily extends AddressFamily {

  private Ipv4UnicastAddressFamily(
      @Nonnull AddressFamilySettings addressFamilySettings,
      @Nullable String exportPolicy,
      SortedSet<String> exportPolicySources,
      @Nullable String importPolicy,
      SortedSet<String> importPolicySources) {
    super(
        addressFamilySettings,
        exportPolicy,
        exportPolicySources,
        importPolicy,
        importPolicySources);
  }

  @JsonCreator
  private static Ipv4UnicastAddressFamily jsonCreator(
      // super fields
      @Nullable @JsonProperty(PROP_ADDRESS_FAMILY_SETTINGS)
          AddressFamilySettings addressFamilySettings,
      @Nullable @JsonProperty(PROP_EXPORT_POLICY) String exportPolicy,
      @Nullable @JsonProperty(PROP_EXPORT_POLICY_SOURCES) SortedSet<String> exportPolicySources,
      @Nullable @JsonProperty(PROP_IMPORT_POLICY) String importPolicy,
      @Nullable @JsonProperty(PROP_IMPORT_POLICY_SOURCES) SortedSet<String> importPolicySources) {
    checkArgument(addressFamilySettings != null, "Missing %s", PROP_ADDRESS_FAMILY_SETTINGS);
    return new Ipv4UnicastAddressFamily(
        addressFamilySettings,
        exportPolicy,
        firstNonNull(exportPolicySources, ImmutableSortedSet.of()),
        importPolicy,
        firstNonNull(importPolicySources, ImmutableSortedSet.of()));
  }

  @Override
  public Type getType() {
    return Type.IPV4_UNICAST;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Ipv4UnicastAddressFamily)) {
      return false;
    }
    Ipv4UnicastAddressFamily that = (Ipv4UnicastAddressFamily) o;
    return _addressFamilySettings.equals(that._addressFamilySettings)
        && Objects.equals(_exportPolicy, that._exportPolicy)
        && Objects.equals(_importPolicy, that._importPolicy)
        && _exportPolicySources.equals(that._exportPolicySources)
        && _importPolicySources.equals(that._importPolicySources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _addressFamilySettings,
        _exportPolicy,
        _exportPolicySources,
        _importPolicy,
        _importPolicySources);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder
      extends AddressFamily.Builder<Builder, Ipv4UnicastAddressFamily> {

    private Builder() {
      _addressFamilySettings = AddressFamilySettings.builder().build();
    }

    @Nonnull
    @Override
    public Builder getThis() {
      return this;
    }

    @Nonnull
    @Override
    public Ipv4UnicastAddressFamily build() {
      checkArgument(_addressFamilySettings != null, "Missing %s", PROP_ADDRESS_FAMILY_SETTINGS);
      return new Ipv4UnicastAddressFamily(
          _addressFamilySettings,
          _exportPolicy,
          _exportPolicySources,
          _importPolicy,
          _importPolicySources);
    }
  }
}
