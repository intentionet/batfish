package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.dataplane.rib.RibGroup;

/**
 * Represents a passive BGP config, which allows peering with multiple remote peers, but cannot
 * initiate connections.
 */
public final class BgpPassivePeerConfig extends BgpPeerConfig {

  private static final String PROP_PEER_PREFIX = "peerPrefix";

  static final long serialVersionUID = 1L;

  public static final Long ANY_AS = -1L;

  /** The prefix from which remote peers can connect. */
  @Nullable private Prefix _peerPrefix;

  /** The list of autonomous system that are allowed to connect. */
  @Nonnull private List<Long> _remoteAs;

  @JsonCreator
  protected BgpPassivePeerConfig(
      @JsonProperty(PROP_ADDITIONAL_PATHS_RECEIVE) boolean additionalPathsReceive,
      @JsonProperty(PROP_ADDITIONAL_PATHS_SELECT_ALL) boolean additionalPathsSelectAll,
      @JsonProperty(PROP_ADDITIONAL_PATHS_SEND) boolean additionalPathsSend,
      @JsonProperty(PROP_ADVERTISE_EXTERNAL) boolean advertiseExternal,
      @JsonProperty(PROP_ADVERTISE_INACTIVE) boolean advertiseInactive,
      @JsonProperty(PROP_ALLOW_LOCAL_AS_IN) boolean allowLocalAsIn,
      @JsonProperty(PROP_ALLOW_REMOTE_AS_OUT) boolean allowRemoteAsOut,
      @JsonProperty(PROP_APPLIED_RIB_GROUP) @Nullable RibGroup appliedRibGroup,
      @JsonProperty(PROP_AUTHENTICATION_SETTINGS) @Nullable
          BgpAuthenticationSettings authenticationSettings,
      @JsonProperty(PROP_CLUSTER_ID) @Nullable Long clusterId,
      @JsonProperty(PROP_DEFAULT_METRIC) int defaultMetric,
      @JsonProperty(PROP_DESCRIPTION) @Nullable String description,
      @JsonProperty(PROP_EBGP_MULTIHOP) boolean ebgpMultihop,
      @JsonProperty(PROP_ENFORCE_FIRST_AS) boolean enforceFirstAs,
      @JsonProperty(PROP_EXPORT_POLICY) @Nullable String exportPolicy,
      @JsonProperty(PROP_EXPORT_POLICY_SOURCES) @Nullable SortedSet<String> exportPolicySources,
      @JsonProperty(PROP_GENERATED_ROUTES) @Nullable Set<GeneratedRoute> generatedRoutes,
      @JsonProperty(PROP_GROUP) @Nullable String group,
      @JsonProperty(PROP_IMPORT_POLICY) @Nullable String importPolicy,
      @JsonProperty(PROP_IMPORT_POLICY_SOURCES) @Nullable SortedSet<String> importPolicySources,
      @JsonProperty(PROP_LOCAL_AS) @Nullable Long localAs,
      @JsonProperty(PROP_LOCAL_IP) @Nullable Ip localIp,
      @JsonProperty(PROP_PEER_PREFIX) @Nullable Prefix peerPrefix,
      @JsonProperty(PROP_REMOTE_AS) @Nullable List<Long> remoteAs,
      @JsonProperty(PROP_ROUTE_REFLECTOR) boolean routeReflectorClient,
      @JsonProperty(PROP_SEND_COMMUNITY) boolean sendCommunity) {
    super(
        additionalPathsReceive,
        additionalPathsSelectAll,
        additionalPathsSend,
        advertiseExternal,
        advertiseInactive,
        allowLocalAsIn,
        allowRemoteAsOut,
        appliedRibGroup,
        authenticationSettings,
        clusterId,
        defaultMetric,
        description,
        ebgpMultihop,
        enforceFirstAs,
        exportPolicy,
        exportPolicySources,
        generatedRoutes,
        group,
        importPolicy,
        importPolicySources,
        localAs,
        localIp,
        routeReflectorClient,
        sendCommunity);
    _peerPrefix = peerPrefix;
    _remoteAs = firstNonNull(remoteAs, ImmutableList.of(ANY_AS));
  }

  /** Check whether a connection from a peer with a given AS number will be accepted. */
  public boolean canConnect(@Nullable Long asNumber) {
    return _remoteAs.contains(ANY_AS) || (asNumber != null && _remoteAs.contains(asNumber));
  }

  public boolean canConnect(@Nonnull Ip address) {
    return _peerPrefix != null && _peerPrefix.containsIp(address);
  }

  @Nullable
  @JsonProperty(PROP_PEER_PREFIX)
  @JsonPropertyDescription("The IPV4 prefix of the remote peer")
  public Prefix getPeerPrefix() {
    return _peerPrefix;
  }

  @Nonnull
  @JsonProperty(PROP_REMOTE_AS)
  @JsonPropertyDescription("The remote autonomous system(s) allowed to peer")
  public List<Long> getRemoteAs() {
    return _remoteAs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BgpPassivePeerConfig)) {
      return false;
    }
    BgpPassivePeerConfig that = (BgpPassivePeerConfig) o;
    return Objects.equals(_peerPrefix, that._peerPrefix)
        && Objects.equals(_remoteAs, that._remoteAs)
        && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _peerPrefix, _remoteAs);
  }

  public static class Builder extends BgpPeerConfig.Builder<Builder, BgpPassivePeerConfig> {
    @Nullable private Prefix _peerPrefix;
    @Nullable private List<Long> _remoteAs;

    protected Builder() {
      super();
    }

    @Override
    @Nonnull
    public BgpPassivePeerConfig build() {
      BgpPassivePeerConfig bgpPeerConfig =
          new BgpPassivePeerConfig(
              _additionalPathsReceive,
              _additionalPathsSelectAll,
              _additionalPathsSend,
              _advertiseExternal,
              _advertiseInactive,
              _allowLocalAsIn,
              _allowRemoteAsOut,
              _appliedRibGroup,
              _authenticationSettings,
              _clusterId,
              _defaultMetric,
              _description,
              _ebgpMultihop,
              _enforceFirstAs,
              _exportPolicy,
              _exportPolicySources,
              _generatedRoutes,
              _group,
              _importPolicy,
              _importPolicySources,
              _localAs,
              _localIp,
              _peerPrefix,
              _remoteAs,
              _routeReflectorClient,
              _sendCommunity);
      if (_bgpProcess != null) {
        _bgpProcess.getPassiveNeighbors().put(_peerPrefix, bgpPeerConfig);
      }
      return bgpPeerConfig;
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    public Builder setPeerPrefix(@Nullable Prefix peerPrefix) {
      _peerPrefix = peerPrefix;
      return this;
    }

    public Builder setRemoteAs(@Nullable List<Long> remoteAs) {
      _remoteAs = remoteAs;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
