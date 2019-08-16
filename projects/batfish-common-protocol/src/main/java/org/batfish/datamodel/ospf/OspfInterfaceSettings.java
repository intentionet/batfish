package org.batfish.datamodel.ospf;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Contains OSPF settings for an OSPF interface. */
@ParametersAreNonnullByDefault
public class OspfInterfaceSettings implements Serializable {

  public static @Nonnull Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Long _ospfAreaName;
    private Integer _ospfCost;
    private Integer _ospfDeadInterval;
    private boolean _ospfEnabled;
    private Integer _ospfHelloMultiplier;
    private String _ospfInboundDistributeListPolicy;
    private OspfNetworkType _ospfNetworkType;
    private Boolean _ospfPassive;
    private String _ospfProcess;

    public Builder() {
      _ospfEnabled = true;
    }

    public Builder setAreaName(@Nullable Long ospfAreaName) {
      _ospfAreaName = ospfAreaName;
      return this;
    }

    public Builder setCost(@Nullable Integer ospfCost) {
      _ospfCost = ospfCost;
      return this;
    }

    public Builder setDeadInterval(Integer ospfDeadInterval) {
      _ospfDeadInterval = ospfDeadInterval;
      return this;
    }

    public Builder setEnabled(boolean ospfEnabled) {
      _ospfEnabled = ospfEnabled;
      return this;
    }

    public Builder setHelloMultiplier(Integer ospfHelloMultiplier) {
      _ospfHelloMultiplier = ospfHelloMultiplier;
      return this;
    }

    public Builder setInboundDistributeListPolicy(String ospfInboundDistributeListPolicy) {
      _ospfInboundDistributeListPolicy = ospfInboundDistributeListPolicy;
      return this;
    }

    public Builder setNetworkType(@Nullable OspfNetworkType ospfNetworkType) {
      _ospfNetworkType = ospfNetworkType;
      return this;
    }

    public Builder setPassive(Boolean ospfPassive) {
      _ospfPassive = ospfPassive;
      return this;
    }

    public Builder setProcess(String ospfProcess) {
      _ospfProcess = ospfProcess;
      return this;
    }

    public OspfInterfaceSettings build() {
      return create(
          _ospfAreaName,
          _ospfCost,
          _ospfDeadInterval,
          _ospfEnabled,
          _ospfHelloMultiplier,
          _ospfInboundDistributeListPolicy,
          _ospfNetworkType,
          _ospfPassive,
          _ospfProcess);
    }
  }

  @Nullable private Long _ospfAreaName;
  private int _ospfCost;
  private int _ospfDeadInterval;
  private boolean _ospfEnabled;
  @Nullable private Integer _ospfHelloMultiplier;
  @Nullable private String _ospfInboundDistributeListPolicy;
  @Nonnull private OspfNetworkType _ospfNetworkType;
  private boolean _ospfPassive;
  @Nullable private String _ospfProcess;

  private static final String PROP_AREA = "area";
  private static final String PROP_COST = "cost";
  private static final String PROP_DEAD_INTERVAL = "deadInterval";
  private static final String PROP_ENABLED = "enabled";
  private static final String PROP_HELLO_MULTIPLIER = "helloMultiplier";
  private static final String PROP_INBOUND_DISTRIBUTE_LIST_POLICY = "inboundDistributeListPolicy";
  private static final String PROP_NETWORK_TYPE = "networkType";
  private static final String PROP_PASSIVE = "passive";
  private static final String PROP_PROCESS = "process";

  @JsonCreator
  private static OspfInterfaceSettings create(
      @Nullable @JsonProperty(PROP_AREA) Long area,
      @Nullable @JsonProperty(PROP_COST) Integer cost,
      @Nullable @JsonProperty(PROP_DEAD_INTERVAL) Integer deadInterval,
      @Nullable @JsonProperty(PROP_ENABLED) Boolean enabled,
      @Nullable @JsonProperty(PROP_HELLO_MULTIPLIER) Integer helloMultiplier,
      @Nullable @JsonProperty(PROP_INBOUND_DISTRIBUTE_LIST_POLICY)
          String inboundDistributeListPolicy,
      @Nullable @JsonProperty(PROP_NETWORK_TYPE) OspfNetworkType networkType,
      @Nullable @JsonProperty(PROP_PASSIVE) Boolean passive,
      @Nullable @JsonProperty(PROP_PROCESS) String process) {
    checkArgument(cost != null, "OSPF cost must be specified");
    checkArgument(networkType != null, "Network type must be specified");
    return new OspfInterfaceSettings(
        area,
        cost,
        firstNonNull(deadInterval, 0),
        firstNonNull(enabled, true),
        helloMultiplier,
        inboundDistributeListPolicy,
        networkType,
        firstNonNull(passive, false),
        process);
  }

  private OspfInterfaceSettings(
      @Nullable Long area,
      int cost,
      Integer deadInterval,
      boolean enabled,
      @Nullable Integer helloMultiplier,
      @Nullable String inboundDistributeListPolicy,
      OspfNetworkType networkType,
      Boolean passive,
      @Nullable String process) {
    _ospfAreaName = area;
    _ospfCost = cost;
    _ospfDeadInterval = deadInterval;
    _ospfEnabled = enabled;
    _ospfHelloMultiplier = helloMultiplier;
    _ospfInboundDistributeListPolicy = inboundDistributeListPolicy;
    _ospfNetworkType = networkType;
    _ospfPassive = passive;
    _ospfProcess = process;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _ospfAreaName,
        _ospfCost,
        _ospfDeadInterval,
        _ospfEnabled,
        _ospfHelloMultiplier,
        _ospfInboundDistributeListPolicy,
        _ospfNetworkType,
        _ospfPassive,
        _ospfProcess);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof OspfInterfaceSettings)) {
      return false;
    }
    OspfInterfaceSettings other = (OspfInterfaceSettings) o;
    if (!Objects.equals(_ospfAreaName, other._ospfAreaName)) {
      return false;
    }
    if (!Objects.equals(_ospfCost, other._ospfCost)) {
      return false;
    }
    if (!Objects.equals(_ospfDeadInterval, other._ospfDeadInterval)) {
      return false;
    }
    if (!Objects.equals(_ospfEnabled, other._ospfEnabled)) {
      return false;
    }
    if (!Objects.equals(_ospfHelloMultiplier, other._ospfHelloMultiplier)) {
      return false;
    }
    if (!Objects.equals(_ospfInboundDistributeListPolicy, other._ospfInboundDistributeListPolicy)) {
      return false;
    }
    if (!Objects.equals(_ospfNetworkType, other._ospfNetworkType)) {
      return false;
    }
    if (!Objects.equals(_ospfPassive, other._ospfPassive)) {
      return false;
    }
    if (!Objects.equals(_ospfProcess, other._ospfProcess)) {
      return false;
    }
    return true;
  }

  /** The OSPF area to which this interface belongs. */
  @JsonProperty(PROP_AREA)
  public Long getAreaName() {
    return _ospfAreaName;
  }

  /** The OSPF cost of this interface. */
  @JsonProperty(PROP_COST)
  public Integer getCost() {
    return _ospfCost;
  }

  /** Dead-interval in seconds for OSPF updates. */
  @JsonProperty(PROP_DEAD_INTERVAL)
  public Integer getDeadInterval() {
    return _ospfDeadInterval;
  }

  /** Whether or not OSPF is enabled at all on this interface (either actively or passively). */
  @JsonProperty(PROP_ENABLED)
  public boolean getEnabled() {
    return _ospfEnabled;
  }

  /** Number of OSPF packets to send out during dead-interval period for fast OSPF updates. */
  @JsonProperty(PROP_HELLO_MULTIPLIER)
  public Integer getHelloMultiplier() {
    return _ospfHelloMultiplier;
  }

  /**
   * Returns name of the routing policy which is generated from the Global and Interface level
   * inbound distribute-lists for OSPF
   */
  @JsonProperty(PROP_INBOUND_DISTRIBUTE_LIST_POLICY)
  @Nullable
  public String getInboundDistributeListPolicy() {
    return _ospfInboundDistributeListPolicy;
  }

  /** OSPF network type for this interface. */
  @JsonProperty(PROP_NETWORK_TYPE)
  public OspfNetworkType getNetworkType() {
    return _ospfNetworkType;
  }

  /**
   * Whether or not OSPF is enabled passively on this interface. If passive, this interface is
   * included in the OSPF RIB, but no OSPF packets are sent from it.
   */
  @JsonProperty(PROP_PASSIVE)
  public boolean getPassive() {
    return _ospfPassive;
  }

  /** The OSPF process this interface is associated with. */
  @JsonProperty(PROP_PROCESS)
  public String getProcess() {
    return _ospfProcess;
  }

  public void setCost(int cost) {
    _ospfCost = cost;
  }

  public void setInboundDistributeListPolicy(String inboundDistributeListPolicy) {
    _ospfInboundDistributeListPolicy = inboundDistributeListPolicy;
  }

  public void setHelloMultiplier(Integer helloMultiplier) {
    _ospfHelloMultiplier = helloMultiplier;
  }
}
