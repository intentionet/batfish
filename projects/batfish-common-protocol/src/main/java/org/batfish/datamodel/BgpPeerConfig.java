package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.dataplane.rib.RibGroup;

/** Represents a configured BGP peering, at the control plane level */
@JsonSchemaDescription("A configured e/iBGP peering relationship")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class BgpPeerConfig implements Serializable {

  static final String PROP_ADDITIONAL_PATHS_RECEIVE = "additionalPathsReceive";

  static final String PROP_ADDITIONAL_PATHS_SELECT_ALL = "additionalPathsSelectAll";

  static final String PROP_ADDITIONAL_PATHS_SEND = "additionalPathsSend";

  static final String PROP_ADVERTISE_EXTERNAL = "advertiseExternal";

  static final String PROP_ADVERTISE_INACTIVE = "advertiseInactive";

  static final String PROP_ALLOW_LOCAL_AS_IN = "allowLocalAsIn";

  static final String PROP_ALLOW_REMOTE_AS_OUT = "allowRemoteAsOut";

  static final String PROP_APPLIED_RIB_GROUP = "appliedRibGroup";

  static final String PROP_AUTHENTICATION_SETTINGS = "authenticationSettings";

  static final String PROP_CLUSTER_ID = "clusterId";

  static final String PROP_DEFAULT_METRIC = "defaultMetric";

  static final String PROP_DESCRIPTION = "description";

  static final String PROP_EBGP_MULTIHOP = "ebgpMultihop";

  static final String PROP_ENFORCE_FIRST_AS = "enforceFirstAs";

  static final String PROP_EXPORT_POLICY = "exportPolicy";

  static final String PROP_EXPORT_POLICY_SOURCES = "exportPolicySources";

  static final String PROP_GENERATED_ROUTES = "generatedRoutes";

  static final String PROP_GROUP = "group";

  static final String PROP_IMPORT_POLICY = "importPolicy";

  static final String PROP_IMPORT_POLICY_SOURCES = "importPolicySources";

  static final String PROP_LOCAL_AS = "localAs";

  static final String PROP_LOCAL_IP = "localIp";

  static final String PROP_REMOTE_AS = "remoteAs";

  static final String PROP_ROUTE_REFLECTOR = "routeReflectorClient";

  static final String PROP_SEND_COMMUNITY = "sendCommunity";

  static final long serialVersionUID = 1L;

  private final boolean _additionalPathsReceive;

  private final boolean _additionalPathsSelectAll;

  private final boolean _additionalPathsSend;

  private final boolean _advertiseExternal;

  private final boolean _advertiseInactive;

  private final boolean _allowLocalAsIn;

  private final boolean _allowRemoteAsOut;

  @Nullable private final RibGroup _appliedRibGroup;

  @Nullable private final BgpAuthenticationSettings _authenticationSettings;

  /** The cluster id associated with this peer to be used in route reflection */
  @Nullable private final Long _clusterId;

  /** The default metric associated with routes sent to this peer */
  private final int _defaultMetric;

  protected final String _description;

  private final boolean _ebgpMultihop;

  private final boolean _enforceFirstAs;

  @Nullable private final String _exportPolicy;

  @Nonnull private SortedSet<String> _exportPolicySources;

  /**
   * The set of generated and/or aggregate routes to be potentially sent to this peer before
   * outbound policies are taken into account
   */
  @Nonnull private final Set<GeneratedRoute> _generatedRoutes;

  /**
   * The group name associated with this peer in the vendor-specific configuration from which the
   * containing configuration is derived. This field is OPTIONAL and should not impact the
   * subsequent data plane computation.
   */
  @Nullable private final String _group;

  @Nullable private final String _importPolicy;

  @Nonnull private SortedSet<String> _importPolicySources;

  /** The autonomous system number of the containing BGP process as reported to this peer */
  @Nullable private final Long _localAs;

  /** The ip address of the containing router as reported to this peer */
  @Nullable private final Ip _localIp;

  /** Flag indicating that this neighbor is a route reflector client */
  private final boolean _routeReflectorClient;

  /**
   * Flag governing whether to include community numbers in outgoing route advertisements to this
   * peer
   */
  private boolean _sendCommunity;

  @JsonCreator
  protected BgpPeerConfig(
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
      @JsonProperty(PROP_ROUTE_REFLECTOR) boolean routeReflectorClient,
      @JsonProperty(PROP_SEND_COMMUNITY) boolean sendCommunity) {
    _additionalPathsReceive = additionalPathsReceive;
    _additionalPathsSelectAll = additionalPathsSelectAll;
    _additionalPathsSend = additionalPathsSend;
    _advertiseExternal = advertiseExternal;
    _advertiseInactive = advertiseInactive;
    _allowLocalAsIn = allowLocalAsIn;
    _allowRemoteAsOut = allowRemoteAsOut;
    _appliedRibGroup = appliedRibGroup;
    _authenticationSettings = authenticationSettings;
    _clusterId = clusterId;
    _defaultMetric = defaultMetric;
    _description = description;
    _ebgpMultihop = ebgpMultihop;
    _enforceFirstAs = enforceFirstAs;
    _exportPolicy = exportPolicy;
    _exportPolicySources = firstNonNull(exportPolicySources, ImmutableSortedSet.of());
    _generatedRoutes = firstNonNull(generatedRoutes, ImmutableSet.of());
    _group = group;
    _importPolicy = importPolicy;
    _importPolicySources = firstNonNull(importPolicySources, ImmutableSortedSet.of());
    _localAs = localAs;
    _localIp = localIp;
    _routeReflectorClient = routeReflectorClient;
    _sendCommunity = sendCommunity;
  }

  @JsonProperty(PROP_ADDITIONAL_PATHS_RECEIVE)
  public boolean getAdditionalPathsReceive() {
    return _additionalPathsReceive;
  }

  @JsonProperty(PROP_ADDITIONAL_PATHS_SELECT_ALL)
  public boolean getAdditionalPathsSelectAll() {
    return _additionalPathsSelectAll;
  }

  @JsonProperty(PROP_ADDITIONAL_PATHS_SEND)
  public boolean getAdditionalPathsSend() {
    return _additionalPathsSend;
  }

  @JsonProperty(PROP_ADVERTISE_EXTERNAL)
  @JsonPropertyDescription(
      "Whether to advertise the best eBGP route for each network independently of whether it is "
          + "the best BGP route for that network")
  public boolean getAdvertiseExternal() {
    return _advertiseExternal;
  }

  @JsonProperty(PROP_ADVERTISE_INACTIVE)
  @JsonPropertyDescription(
      "Whether to advertise the best BGP route for each network independently of whether it is "
          + "the best overall route for that network")
  public boolean getAdvertiseInactive() {
    return _advertiseInactive;
  }

  @JsonProperty(PROP_ALLOW_LOCAL_AS_IN)
  @JsonPropertyDescription(
      "Whether to allow reception of advertisements containing the local AS number in the AS-path")
  public boolean getAllowLocalAsIn() {
    return _allowLocalAsIn;
  }

  @JsonProperty(PROP_ALLOW_REMOTE_AS_OUT)
  @JsonPropertyDescription(
      "Whether to allow sending of advertisements containing the remote AS number in the AS-path")
  public boolean getAllowRemoteAsOut() {
    return _allowRemoteAsOut;
  }

  /** Return the {@link RibGroup} applied to this config */
  @Nullable
  public RibGroup getAppliedRibGroup() {
    return _appliedRibGroup;
  }

  @Nullable
  @JsonProperty(PROP_AUTHENTICATION_SETTINGS)
  @JsonPropertyDescription("The authentication setting to be used for this neighbor")
  public BgpAuthenticationSettings getAuthenticationSettings() {
    return _authenticationSettings;
  }

  @JsonProperty(PROP_CLUSTER_ID)
  @JsonPropertyDescription("Route-reflection cluster-id for this peer")
  @Nullable
  public Long getClusterId() {
    return _clusterId;
  }

  @JsonProperty(PROP_DEFAULT_METRIC)
  @JsonPropertyDescription("Default MED for routes sent to this neighbor")
  public int getDefaultMetric() {
    return _defaultMetric;
  }

  @JsonProperty(PROP_DESCRIPTION)
  @JsonPropertyDescription("Description of this peer")
  public String getDescription() {
    return _description;
  }

  @JsonProperty(PROP_EBGP_MULTIHOP)
  @JsonPropertyDescription(
      "Whether to allow establishment of a multihop eBGP connection with this peer")
  public boolean getEbgpMultihop() {
    return _ebgpMultihop;
  }

  @JsonProperty(PROP_ENFORCE_FIRST_AS)
  @JsonPropertyDescription(
      "Whether to discard updates received from an external BGP (eBGP) peers that do not list "
          + "their autonomous system (AS) number as the first AS path segment")
  public boolean getEnforceFirstAs() {
    return _enforceFirstAs;
  }

  @JsonProperty(PROP_EXPORT_POLICY)
  @JsonPropertyDescription("The policy governing all advertisements sent to this peer")
  @Nullable
  public String getExportPolicy() {
    return _exportPolicy;
  }

  @JsonProperty(PROP_EXPORT_POLICY_SOURCES)
  @Nonnull
  public SortedSet<String> getExportPolicySources() {
    return _exportPolicySources;
  }

  @JsonProperty(PROP_GENERATED_ROUTES)
  @JsonPropertyDescription(
      "Generated routes specific to this peer not otherwise imported into any of this node's RIBs")
  @Nonnull
  public Set<GeneratedRoute> getGeneratedRoutes() {
    return _generatedRoutes;
  }

  @JsonProperty(PROP_GROUP)
  @JsonPropertyDescription(
      "Name of a group in the original vendor-specific configuration to which this peer is "
          + "assigned")
  @Nullable
  public String getGroup() {
    return _group;
  }

  @JsonProperty(PROP_IMPORT_POLICY)
  @JsonPropertyDescription("Routing policy governing all advertisements received from this peer")
  @Nullable
  public String getImportPolicy() {
    return _importPolicy;
  }

  @JsonProperty(PROP_IMPORT_POLICY_SOURCES)
  @Nonnull
  public SortedSet<String> getImportPolicySources() {
    return _importPolicySources;
  }

  @JsonProperty(PROP_LOCAL_AS)
  @JsonPropertyDescription("The local autonomous system of this peering")
  @Nullable
  public Long getLocalAs() {
    return _localAs;
  }

  @JsonProperty(PROP_LOCAL_IP)
  @JsonPropertyDescription("The local (source) IPV4 address of this peering")
  @Nullable
  public Ip getLocalIp() {
    return _localIp;
  }

  @JsonProperty(PROP_ROUTE_REFLECTOR)
  @JsonPropertyDescription("Whether or not this peer is a route-reflector client")
  public boolean getRouteReflectorClient() {
    return _routeReflectorClient;
  }

  @JsonProperty(PROP_SEND_COMMUNITY)
  @JsonPropertyDescription(
      "Whether or not to propagate the community attribute(s) of advertisements to this peer")
  public boolean getSendCommunity() {
    return _sendCommunity;
  }

  public void setExportPolicySources(@Nonnull SortedSet<String> exportPolicySources) {
    _exportPolicySources = exportPolicySources;
  }

  public void setImportPolicySources(@Nonnull SortedSet<String> importPolicySources) {
    _importPolicySources = importPolicySources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BgpPeerConfig that = (BgpPeerConfig) o;
    return _additionalPathsReceive == that._additionalPathsReceive
        && _additionalPathsSelectAll == that._additionalPathsSelectAll
        && _additionalPathsSend == that._additionalPathsSend
        && _advertiseExternal == that._advertiseExternal
        && _advertiseInactive == that._advertiseInactive
        && _allowLocalAsIn == that._allowLocalAsIn
        && _allowRemoteAsOut == that._allowRemoteAsOut
        && _defaultMetric == that._defaultMetric
        && _ebgpMultihop == that._ebgpMultihop
        && _enforceFirstAs == that._enforceFirstAs
        && _routeReflectorClient == that._routeReflectorClient
        && _sendCommunity == that._sendCommunity
        && Objects.equals(_authenticationSettings, that._authenticationSettings)
        && Objects.equals(_clusterId, that._clusterId)
        && Objects.equals(_description, that._description)
        && Objects.equals(_exportPolicy, that._exportPolicy)
        && Objects.equals(_exportPolicySources, that._exportPolicySources)
        && Objects.equals(_generatedRoutes, that._generatedRoutes)
        && Objects.equals(_group, that._group)
        && Objects.equals(_importPolicy, that._importPolicy)
        && Objects.equals(_importPolicySources, that._importPolicySources)
        && Objects.equals(_localAs, that._localAs)
        && Objects.equals(_localIp, that._localIp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _additionalPathsReceive,
        _additionalPathsSelectAll,
        _additionalPathsSend,
        _advertiseExternal,
        _advertiseInactive,
        _allowLocalAsIn,
        _allowRemoteAsOut,
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
        _routeReflectorClient,
        _sendCommunity);
  }

  public abstract static class Builder<S extends Builder<S, T>, T extends BgpPeerConfig> {
    protected boolean _additionalPathsReceive;
    protected boolean _additionalPathsSelectAll;
    protected boolean _additionalPathsSend;
    protected boolean _advertiseExternal;
    protected boolean _advertiseInactive;
    protected boolean _allowLocalAsIn;
    protected boolean _allowRemoteAsOut;
    @Nullable protected RibGroup _appliedRibGroup;
    @Nullable protected BgpAuthenticationSettings _authenticationSettings;
    @Nullable protected BgpProcess _bgpProcess;
    protected Long _clusterId;
    protected int _defaultMetric;
    protected String _description;
    protected boolean _ebgpMultihop;
    protected boolean _enforceFirstAs;
    protected String _exportPolicy;
    @Nullable protected SortedSet<String> _exportPolicySources;
    @Nullable protected Set<GeneratedRoute> _generatedRoutes;
    @Nullable protected String _group;
    @Nullable protected String _importPolicy;
    @Nullable protected SortedSet<String> _importPolicySources;
    @Nullable protected Long _localAs;
    @Nullable protected Ip _localIp;
    protected boolean _routeReflectorClient;
    protected boolean _sendCommunity;

    // Identifying fields
    @Nullable protected String _hostname;

    protected Builder() {}

    public abstract T build();

    protected abstract S getThis();

    public S setAdditionalPathsReceive(boolean additionalPathsReceive) {
      _additionalPathsReceive = additionalPathsReceive;
      return getThis();
    }

    public S setAdditionalPathsSelectAll(boolean additionalPathsSelectAll) {
      _additionalPathsSelectAll = additionalPathsSelectAll;
      return getThis();
    }

    public S setAdditionalPathsSend(boolean additionalPathsSend) {
      _additionalPathsSend = additionalPathsSend;
      return getThis();
    }

    public S setAdvertiseExternal(boolean advertiseExternal) {
      _advertiseExternal = advertiseExternal;
      return getThis();
    }

    public S setAdvertiseInactive(boolean advertiseInactive) {
      _advertiseInactive = advertiseInactive;
      return getThis();
    }

    public S setAllowLocalAsIn(boolean allowLocalAsIn) {
      _allowLocalAsIn = allowLocalAsIn;
      return getThis();
    }

    public S setAllowRemoteAsOut(boolean allowRemoteAsOut) {
      _allowRemoteAsOut = allowRemoteAsOut;
      return getThis();
    }

    public S setAppliedRibGroup(@Nullable RibGroup ribGroup) {
      _appliedRibGroup = ribGroup;
      return getThis();
    }

    public S setAuthenticationSettings(@Nullable BgpAuthenticationSettings authenticationSettings) {
      _authenticationSettings = authenticationSettings;
      return getThis();
    }

    /**
     * If specified, upon calling {@link #build()}, the neighbor will be added to the appropriate
     * list of neighbors in the given {@link BgpProcess}.
     */
    public S setBgpProcess(@Nonnull BgpProcess bgpProcess) {
      _bgpProcess = bgpProcess;
      return getThis();
    }

    public S setClusterId(Long clusterId) {
      _clusterId = clusterId;
      return getThis();
    }

    public S setDefaultMetric(int defaultMetric) {
      _defaultMetric = defaultMetric;
      return getThis();
    }

    public S setDescription(String description) {
      _description = description;
      return getThis();
    }

    public S setEbgpMultihop(boolean ebgpMultihop) {
      _ebgpMultihop = ebgpMultihop;
      return getThis();
    }

    public S setEnforceFirstAs(boolean enforceFirstAs) {
      _enforceFirstAs = enforceFirstAs;
      return getThis();
    }

    public S setExportPolicy(String exportPolicy) {
      _exportPolicy = exportPolicy;
      return getThis();
    }

    public S setExportPolicySources(@Nullable SortedSet<String> exportPolicySources) {
      _exportPolicySources = exportPolicySources;
      return getThis();
    }

    public S setGeneratedRoutes(@Nullable Set<GeneratedRoute> generatedRoutes) {
      _generatedRoutes = generatedRoutes;
      return getThis();
    }

    public S setGroup(@Nullable String group) {
      _group = group;
      return getThis();
    }

    public S setImportPolicy(@Nullable String importPolicy) {
      _importPolicy = importPolicy;
      return getThis();
    }

    public S setImportPolicySources(@Nullable SortedSet<String> importPolicySources) {
      _importPolicySources = importPolicySources;
      return getThis();
    }

    public S setLocalAs(@Nullable Long localAs) {
      _localAs = localAs;
      return getThis();
    }

    public S setLocalIp(@Nullable Ip localIp) {
      _localIp = localIp;
      return getThis();
    }

    public S setRouteReflectorClient(boolean routeReflectorClient) {
      _routeReflectorClient = routeReflectorClient;
      return getThis();
    }

    public S setSendCommunity(boolean sendCommunity) {
      _sendCommunity = sendCommunity;
      return getThis();
    }
  }
}
