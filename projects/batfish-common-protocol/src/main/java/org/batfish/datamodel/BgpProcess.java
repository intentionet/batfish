package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;

/** Represents a bgp process on a router */
public class BgpProcess implements Serializable {

  public static class Builder extends NetworkFactoryBuilder<BgpProcess> {

    private Ip _routerId;
    private Vrf _vrf;

    Builder(NetworkFactory networkFactory) {
      super(networkFactory, BgpProcess.class);
    }

    @Override
    public BgpProcess build() {
      BgpProcess bgpProcess = new BgpProcess();
      if (_vrf != null) {
        _vrf.setBgpProcess(bgpProcess);
      }
      if (_routerId != null) {
        bgpProcess.setRouterId(_routerId);
      }
      return bgpProcess;
    }

    public BgpProcess.Builder setRouterId(Ip routerId) {
      _routerId = routerId;
      return this;
    }

    public BgpProcess.Builder setVrf(Vrf vrf) {
      _vrf = vrf;
      return this;
    }
  }

  private class ClusterIdsSupplier implements Serializable, Supplier<Set<Long>> {
    private static final long serialVersionUID = 1L;

    @Override
    public Set<Long> get() {
      return _activeNeighbors.values().stream()
          .map(BgpPeerConfig::getClusterId)
          .collect(ImmutableSet.toImmutableSet());
    }
  }

  private static final String PROP_PASSIVE_NEIGHBORS = "dynamicNeighbors";
  private static final String PROP_MULTIPATH_EBGP = "multipathEbgp";
  private static final String PROP_MULTIPATH_EQUIVALENT_AS_PATH_MATCH_MODE =
      "multipathEquivalentAsPathMatchMode";
  private static final String PROP_MULTIPATH_IBGP = "multipathIbgp";
  private static final String PROP_ACTIVE_NEIGHBORS = "neighbors";
  private static final String PROP_ROUTER_ID = "routerId";
  private static final String PROP_TIE_BREAKER = "tieBreaker";

  private static final long serialVersionUID = 1L;

  private Supplier<Set<Long>> _clusterIds;
  private boolean _multipathEbgp;
  private MultipathEquivalentAsPathMatchMode _multipathEquivalentAsPathMatchMode;
  private boolean _multipathIbgp;

  /**
   * A map of all non-dynamic bgp neighbors with which the router owning this process is configured
   * to peer, keyed unique ID.
   */
  @Nonnull private SortedMap<Prefix, BgpActivePeerConfig> _activeNeighbors;

  /**
   * A map of all dynamic bgp neighbors with which the router owning this process is configured to
   * peer, keyed by unique ID.
   */
  @Nonnull private SortedMap<Prefix, BgpPassivePeerConfig> _passiveNeighbors;

  /** Space of prefixes to be advertised using explicit network statements */
  private PrefixSpace _originationSpace;

  private Ip _routerId;

  private BgpTieBreaker _tieBreaker;

  /** Constructs a BgpProcess */
  public BgpProcess() {
    _activeNeighbors = new TreeMap<>();
    _tieBreaker = BgpTieBreaker.ARRIVAL_ORDER;
    _clusterIds = new ClusterIdsSupplier();
    _originationSpace = new PrefixSpace();
    _activeNeighbors = new TreeMap<>();
    _passiveNeighbors = new TreeMap<>();
  }

  /**
   * Expand the origination space for this prefix
   *
   * @param space {@link PrefixSpace} to add
   */
  public void addToOriginationSpace(PrefixSpace space) {
    _originationSpace.addSpace(space);
  }

  /**
   * Returns set of all cluster IDs for all neighbors. The result is memoized, so this should only
   * be called after the neighbors are finalized.
   */
  @JsonIgnore
  public Set<Long> getClusterIds() {
    return _clusterIds.get();
  }

  /** @return {@link #_activeNeighbors} */
  @JsonProperty(PROP_ACTIVE_NEIGHBORS)
  @JsonPropertyDescription("Neighbor relationships configured for this BGP process")
  @Nonnull
  public SortedMap<Prefix, BgpActivePeerConfig> getActiveNeighbors() {
    return _activeNeighbors;
  }

  @JsonProperty(PROP_MULTIPATH_EBGP)
  public boolean getMultipathEbgp() {
    return _multipathEbgp;
  }

  @JsonProperty(PROP_MULTIPATH_EQUIVALENT_AS_PATH_MATCH_MODE)
  public MultipathEquivalentAsPathMatchMode getMultipathEquivalentAsPathMatchMode() {
    return _multipathEquivalentAsPathMatchMode;
  }

  @JsonProperty(PROP_MULTIPATH_IBGP)
  public boolean getMultipathIbgp() {
    return _multipathIbgp;
  }

  /** @return {@link #_passiveNeighbors} */
  @JsonProperty(PROP_PASSIVE_NEIGHBORS)
  @JsonPropertyDescription("Neighbor relationships configured for this BGP process")
  @Nonnull
  public SortedMap<Prefix, BgpPassivePeerConfig> getPassiveNeighbors() {
    return _passiveNeighbors;
  }

  @JsonIgnore
  public PrefixSpace getOriginationSpace() {
    return _originationSpace;
  }

  @JsonProperty(PROP_ROUTER_ID)
  @JsonPropertyDescription(
      "The configured router ID for this BGP process. Note that it can be overridden for "
          + "individual neighbors.")
  public Ip getRouterId() {
    return _routerId;
  }

  @JsonProperty(PROP_TIE_BREAKER)
  public BgpTieBreaker getTieBreaker() {
    return _tieBreaker;
  }

  @JsonProperty(PROP_MULTIPATH_EBGP)
  public void setMultipathEbgp(boolean multipathEbgp) {
    _multipathEbgp = multipathEbgp;
  }

  @JsonProperty(PROP_MULTIPATH_EQUIVALENT_AS_PATH_MATCH_MODE)
  public void setMultipathEquivalentAsPathMatchMode(
      MultipathEquivalentAsPathMatchMode multipathEquivalentAsPathMatchMode) {
    _multipathEquivalentAsPathMatchMode = multipathEquivalentAsPathMatchMode;
  }

  @JsonProperty(PROP_MULTIPATH_IBGP)
  public void setMultipathIbgp(boolean multipathIbgp) {
    _multipathIbgp = multipathIbgp;
  }

  @JsonProperty(PROP_ACTIVE_NEIGHBORS)
  public void setNeighbors(SortedMap<Prefix, BgpActivePeerConfig> neighbors) {
    _activeNeighbors = firstNonNull(neighbors, new TreeMap<>());
  }

  public void setOriginationSpace(PrefixSpace originationSpace) {
    _originationSpace = originationSpace;
  }

  @JsonProperty(PROP_PASSIVE_NEIGHBORS)
  public void setPassiveNeighbors(@Nullable SortedMap<Prefix, BgpPassivePeerConfig> neighbors) {
    _passiveNeighbors = firstNonNull(neighbors, new TreeMap<>());
  }

  @JsonProperty(PROP_ROUTER_ID)
  public void setRouterId(Ip routerId) {
    _routerId = routerId;
  }

  @JsonProperty(PROP_TIE_BREAKER)
  public void setTieBreaker(BgpTieBreaker tieBreaker) {
    _tieBreaker = tieBreaker;
  }
}
