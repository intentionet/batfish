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
import org.batfish.datamodel.vxlan.Layer2Vni;

public class MockDataPlane implements DataPlane {

  public static class Builder {
    @Nonnull private Table<String, String, Set<Bgpv4Route>> _bgpRoutes;
    @Nonnull private Table<String, String, Set<Bgpv4Route>> _bgpBackupRoutes;
    @Nonnull private Table<String, String, Set<EvpnRoute<?, ?>>> _evpnRoutes;
    @Nonnull private Table<String, String, Set<EvpnRoute<?, ?>>> _evpnBackupRoutes;
    @Nonnull private Map<String, Map<String, Fib>> _fibs;
    @Nullable private ForwardingAnalysis _forwardingAnalysis;

    @Nonnull
    private SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> _ribs;

    @Nonnull private Table<String, String, Set<Layer2Vni>> _vniSettings;

    private Builder() {
      _bgpRoutes = HashBasedTable.create();
      _evpnRoutes = HashBasedTable.create();
      _fibs = ImmutableMap.of();
      _ribs = ImmutableSortedMap.of();
      _vniSettings = HashBasedTable.create();
    }

    public MockDataPlane build() {
      return new MockDataPlane(this);
    }

    public Builder setBgpRoutes(@Nonnull Table<String, String, Set<Bgpv4Route>> bgpRoutes) {
      _bgpRoutes = bgpRoutes;
      return this;
    }

    public Builder setBgpBackupRoutes(
        @Nonnull Table<String, String, Set<Bgpv4Route>> bgpBackupRoutes) {
      _bgpBackupRoutes = bgpBackupRoutes;
      return this;
    }

    public Builder setEvpnRoutes(@Nonnull Table<String, String, Set<EvpnRoute<?, ?>>> evpnRoutes) {
      _evpnRoutes = evpnRoutes;
      return this;
    }

    public Builder setEvpnBackupRoutes(
        @Nonnull Table<String, String, Set<EvpnRoute<?, ?>>> evpnBackupRoutes) {
      _evpnBackupRoutes = evpnBackupRoutes;
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

    public Builder setVniSettings(@Nonnull Table<String, String, Set<Layer2Vni>> vniSettings) {
      _vniSettings = vniSettings;
      return this;
    }

    public Builder setRibs(
        SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs) {
      _ribs = ribs;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  @Nonnull private Table<String, String, Set<Bgpv4Route>> _bgpRoutes;
  @Nonnull private Table<String, String, Set<Bgpv4Route>> _bgpBackupRoutes;
  @Nonnull private Table<String, String, Set<EvpnRoute<?, ?>>> _evpnRoutes;
  @Nonnull private Table<String, String, Set<EvpnRoute<?, ?>>> _evpnBackupRoutes;
  @Nonnull private final Map<String, Map<String, Fib>> _fibs;
  @Nullable private final ForwardingAnalysis _forwardingAnalysis;

  @Nonnull
  private final SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>>
      _ribs;

  @Nonnull private Table<String, String, Set<Layer2Vni>> _vniSettings;

  private MockDataPlane(Builder builder) {
    _bgpRoutes = builder._bgpRoutes;
    _bgpBackupRoutes = builder._bgpBackupRoutes;
    _evpnRoutes = builder._evpnRoutes;
    _evpnBackupRoutes = builder._evpnBackupRoutes;
    _fibs = builder._fibs;
    _forwardingAnalysis = builder._forwardingAnalysis;
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
  public Table<String, String, Set<Bgpv4Route>> getBgpBackupRoutes() {
    return _bgpBackupRoutes;
  }

  @Nonnull
  @Override
  public Table<String, String, Set<EvpnRoute<?, ?>>> getEvpnRoutes() {
    return _evpnRoutes;
  }

  @Nonnull
  @Override
  public Table<String, String, Set<EvpnRoute<?, ?>>> getEvpnBackupRoutes() {
    return _evpnBackupRoutes;
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
  public SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> getRibs() {
    return _ribs;
  }

  @Override
  @Nonnull
  public SortedMap<String, SortedMap<String, Map<Prefix, Map<String, Set<String>>>>>
      getPrefixTracingInfoSummary() {
    return ImmutableSortedMap.of();
  }

  @Nonnull
  @Override
  public Table<String, String, Set<Layer2Vni>> getLayer2Vnis() {
    return _vniSettings;
  }
}
