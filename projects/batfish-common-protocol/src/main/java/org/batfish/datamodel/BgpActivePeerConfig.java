package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.dataplane.rib.RibGroup;

/** Represent a BGP config which allows peering with a single remote peer. */
public final class BgpActivePeerConfig extends BgpPeerConfig {

  private static final String PROP_PEER_ADDRESS = "peerAddress";

  static final long serialVersionUID = 1L;

  /** The remote peer's IP address */
  @Nullable private final Ip _peerAddress;

  /** The autonomous system number that the containing BGP process considers this peer to have. */
  @Nullable private final Long _remoteAs;

  @JsonCreator
  protected BgpActivePeerConfig(
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
      @JsonProperty(PROP_PEER_ADDRESS) @Nullable Ip peerAddress,
      @JsonProperty(PROP_REMOTE_AS) @Nullable Long remoteAs,
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
    _peerAddress = peerAddress;
    _remoteAs = remoteAs;
  }

  @Nullable
  @JsonProperty(PROP_PEER_ADDRESS)
  @JsonPropertyDescription("The IPV4 address of the remote peer")
  public Ip getPeerAddress() {
    return _peerAddress;
  }

  @JsonProperty(PROP_REMOTE_AS)
  @JsonPropertyDescription("The remote autonomous system of this peering")
  @Nullable
  public Long getRemoteAs() {
    return _remoteAs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BgpActivePeerConfig)) {
      return false;
    }
    BgpActivePeerConfig that = (BgpActivePeerConfig) o;
    return Objects.equals(_peerAddress, that._peerAddress)
        && Objects.equals(_remoteAs, that._remoteAs)
        && super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _peerAddress, _remoteAs);
  }

  public static class Builder extends BgpPeerConfig.Builder<Builder, BgpActivePeerConfig> {
    @Nullable private Ip _peerAddress;
    @Nullable private Long _remoteAs;

    protected Builder() {
      super();
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    @Override
    @Nonnull
    public BgpActivePeerConfig build() {
      BgpActivePeerConfig bgpPeerConfig =
          new BgpActivePeerConfig(
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
              _peerAddress,
              _remoteAs,
              _routeReflectorClient,
              _sendCommunity);
      if (_bgpProcess != null) {
        _bgpProcess
            .getActiveNeighbors()
            .put(
                Prefix.create(Objects.requireNonNull(_peerAddress), Prefix.MAX_PREFIX_LENGTH),
                bgpPeerConfig);
      }
      return bgpPeerConfig;
    }

    public Builder setPeerAddress(@Nullable Ip peerAddress) {
      _peerAddress = peerAddress;
      return this;
    }

    public Builder setRemoteAs(@Nullable Long remoteAs) {
      _remoteAs = remoteAs;
      return this;
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
