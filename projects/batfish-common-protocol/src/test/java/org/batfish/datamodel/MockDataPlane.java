package org.batfish.datamodel;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Table;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MockDataPlane implements DataPlane {

  public static class Builder {
    @Nonnull private Table<String, String, Set<Bgpv4Route>> _bgpRoutes;
    @Nonnull private Map<String, Configuration> _configurations;
    @Nonnull private Table<String, String, Set<EvpnRoute<?, ?>>> _evpnRoutes;
    @Nonnull private Map<String, Map<String, Fib>> _fibs;
    @Nullable private ForwardingAnalysis _forwardingAnalysis;

    @Nonnull
    private SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> _ribs;

    @Nonnull private Map<Ip, Map<String, Set<String>>> _ipVrfOwners;
    @Nonnull private Table<String, String, Set<VniSettings>> _vniSettings;

    private Builder() {
      _bgpRoutes = HashBasedTable.create();
      _configurations = ImmutableMap.of();
      _evpnRoutes = HashBasedTable.create();
      _fibs = ImmutableMap.of();
      _ribs = ImmutableSortedMap.of();
      _ipVrfOwners = ImmutableMap.of();
      _vniSettings = HashBasedTable.create();
    }

    public MockDataPlane build() {
      return new MockDataPlane(this);
    }

    public Builder setBgpRoutes(@Nonnull Table<String, String, Set<Bgpv4Route>> bgpRoutes) {
      _bgpRoutes = bgpRoutes;
      return this;
    }

    public Builder setConfigs(Map<String, Configuration> configs) {
      _configurations = configs;
      return this;
    }

    public Builder setEvpnRoutes(@Nonnull Table<String, String, Set<EvpnRoute<?, ?>>> evpnRoutes) {
      _evpnRoutes = evpnRoutes;
      return this;
    }

    public Builder setFibs(@Nonnull Map<String, Map<String, Fib>> fibs) {
      _fibs = fibs;
      return this;
    }

    public Builder setForwardingAnalysis(ForwardingAnalysis forwardingAnalysis) {
      _forwardingAnalysis = forwardingAnalysis;
      return this;
    }

    public Builder setIpVrfOwners(@Nonnull Map<Ip, Map<String, Set<String>>> ipVrfOwners) {
      _ipVrfOwners = ipVrfOwners;
      return this;
    }

    public Builder setVniSettings(@Nonnull Table<String, String, Set<VniSettings>> vniSettings) {
      _vniSettings = vniSettings;
      return this;
    }

    public Builder setRibs(
        SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs) {
      _ribs = ribs;
      return this;
    }
  }

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  @Nonnull private Table<String, String, Set<Bgpv4Route>> _bgpRoutes;
  @Nonnull private final Map<String, Configuration> _configurations;
  @Nonnull private Table<String, String, Set<EvpnRoute<?, ?>>> _evpnRoutes;
  @Nonnull private final Map<String, Map<String, Fib>> _fibs;
  @Nullable private final ForwardingAnalysis _forwardingAnalysis;
  @Nonnull private final Map<Ip, Map<String, Set<String>>> _ipVrfOwners;

  @Nonnull
  private final SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>>
      _ribs;

  @Nonnull private Table<String, String, Set<VniSettings>> _vniSettings;

  private MockDataPlane(Builder builder) {
    _bgpRoutes = builder._bgpRoutes;
    _configurations = builder._configurations;
    _evpnRoutes = builder._evpnRoutes;
    _fibs = builder._fibs;
    _forwardingAnalysis = builder._forwardingAnalysis;
    _ipVrfOwners = builder._ipVrfOwners;
    _ribs = ImmutableSortedMap.copyOf(builder._ribs);
    _vniSettings = builder._vniSettings;
  }

  @Nonnull
  @Override
  public Table<String, String, Set<Bgpv4Route>> getBgpRoutes() {
    return _bgpRoutes;
  }

  @Nonnull
  @Override
  public Table<String, String, Set<EvpnRoute<?, ?>>> getEvpnRoutes() {
    return _evpnRoutes;
  }

  @Nonnull
  @Override
  public Map<String, Map<String, Fib>> getFibs() {
    return _fibs;
  }

  @Nullable
  @Override
  public ForwardingAnalysis getForwardingAnalysis() {
    return _forwardingAnalysis;
  }

  @Nonnull
  @Override
  public Map<Ip, Map<String, Set<String>>> getIpVrfOwners() {
    return _ipVrfOwners;
  }

  @Nonnull
  @Override
  public SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> getRibs() {
    return _ribs;
  }

  @Nonnull
  @Override
  public Map<String, Configuration> getConfigurations() {
    return _configurations;
  }

  @Override
  @Nonnull
  public SortedMap<String, SortedMap<String, Map<Prefix, Map<String, Set<String>>>>>
      getPrefixTracingInfoSummary() {
    return ImmutableSortedMap.of();
  }

  @Nonnull
  @Override
  public Table<String, String, Set<VniSettings>> getVniSettings() {
    return _vniSettings;
  }
}
