package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.routing_policy.RoutingPolicy;

@JsonSchemaDescription("An OSPF routing process")
public class OspfProcess implements Serializable {

  public static class Builder extends NetworkFactoryBuilder<OspfProcess> {

    private String _exportPolicy;

    private Long _maxMetricExternalNetworks;

    private Long _maxMetricStubNetworks;

    private Long _maxMetricSummaryNetworks;

    private Long _maxMetricTransitLinks;

    private String _processId;

    private Vrf _vrf;

    Builder(NetworkFactory networkFactory) {
      super(networkFactory, OspfProcess.class);
    }

    @Override
    public OspfProcess build() {
      OspfProcess ospfProcess = new OspfProcess();
      if (_vrf != null) {
        _vrf.setOspfProcess(ospfProcess);
      }
      ospfProcess.setExportPolicy(_exportPolicy);
      ospfProcess.setMaxMetricExternalNetworks(_maxMetricExternalNetworks);
      ospfProcess.setMaxMetricStubNetworks(_maxMetricStubNetworks);
      ospfProcess.setMaxMetricSummaryNetworks(_maxMetricSummaryNetworks);
      ospfProcess.setMaxMetricTransitLinks(_maxMetricTransitLinks);
      ospfProcess.setProcessId(_processId);
      return ospfProcess;
    }

    public Builder setExportPolicy(RoutingPolicy exportPolicy) {
      _exportPolicy = exportPolicy != null ? exportPolicy.getName() : null;
      return this;
    }

    public Builder setMaxMetricExternalNetworks(Long maxMetricExternalNetworks) {
      _maxMetricExternalNetworks = maxMetricExternalNetworks;
      return this;
    }

    public Builder setMaxMetricStubNetworks(Long maxMetricStubNetworks) {
      _maxMetricStubNetworks = maxMetricStubNetworks;
      return this;
    }

    public Builder setMaxMetricSummaryNetworks(Long maxMetricSummaryNetworks) {
      _maxMetricSummaryNetworks = maxMetricSummaryNetworks;
      return this;
    }

    public Builder setMaxMetricTransitLinks(Long maxMetricTransitLinks) {
      _maxMetricTransitLinks = maxMetricTransitLinks;
      return this;
    }

    public Builder setProcessId(@Nullable String processId) {
      _processId = processId;
      return this;
    }

    public Builder setVrf(Vrf vrf) {
      _vrf = vrf;
      return this;
    }
  }

  private static final int DEFAULT_CISCO_VLAN_OSPF_COST = 1;

  private static final String PROP_EXPORT_POLICY = "exportPolicy";

  private static final String PROP_EXPORT_POLICY_SOURCES = "exportPolicySources";

  private static final String PROP_GENERATED_ROUTES = "generatedRoutes";

  private static final String PROP_MAX_METRIC_EXTERNAL_NETWORKS = "maxMetricExternalNetworks";

  private static final String PROP_MAX_METRIC_STUB_NETWORKS = "maxMetricStubNetworks";

  private static final String PROP_MAX_METRIC_SUMMARY_NETWORKS = "maxMetricSummaryNetworks";

  private static final String PROP_MAX_METRIC_TRANSIT_LINKS = "maxMetricTransitLinks";

  private static final String PROP_PROCESS_ID = "processId";

  private static final long serialVersionUID = 1L;

  private SortedMap<Long, OspfArea> _areas;

  private String _exportPolicy;

  private SortedSet<String> _exportPolicySources;

  private SortedSet<GeneratedRoute> _generatedRoutes;

  private Long _maxMetricExternalNetworks;

  private Long _maxMetricStubNetworks;

  private Long _maxMetricSummaryNetworks;

  private Long _maxMetricTransitLinks;

  private transient Map<IpLink, OspfNeighbor> _ospfNeighbors;

  private String _processId;

  private Double _referenceBandwidth;

  private @Nullable Boolean _rfc1583Compatible;

  private Ip _routerId;

  public OspfProcess() {
    _exportPolicySources = new TreeSet<>();
    _generatedRoutes = new TreeSet<>();
    _areas = new TreeMap<>();
  }

  public int computeInterfaceCost(Interface i) {
    Integer ospfCost = i.getOspfCost();
    if (ospfCost == null) {
      String interfaceName = i.getName();
      if (interfaceName.startsWith("Vlan")) {
        // TODO: fix for non-cisco
        ospfCost = DEFAULT_CISCO_VLAN_OSPF_COST;
      } else {
        if (i.getBandwidth() != null) {
          ospfCost = Math.max((int) (_referenceBandwidth / i.getBandwidth()), 1);
        } else {
          String hostname = i.getOwner().getHostname();
          throw new BatfishException(
              "Expected non-null interface bandwidth for \""
                  + hostname
                  + "\":\""
                  + interfaceName
                  + "\"");
        }
      }
    }
    return ospfCost;
  }

  @JsonPropertyDescription("The OSPF areas contained in this process")
  public SortedMap<Long, OspfArea> getAreas() {
    return _areas;
  }

  @JsonProperty(PROP_EXPORT_POLICY)
  @JsonPropertyDescription(
      "The routing policy applied to routes in the main RIB to determine which ones are to be "
          + "exported into OSPF and how")
  public String getExportPolicy() {
    return _exportPolicy;
  }

  @JsonProperty(PROP_EXPORT_POLICY_SOURCES)
  public SortedSet<String> getExportPolicySources() {
    return _exportPolicySources;
  }

  @JsonPropertyDescription(
      "Generated IPV4 routes for the purpose of export into OSPF. These routes are not imported "
          + "into the main RIB.")
  @JsonProperty(PROP_GENERATED_ROUTES)
  public SortedSet<GeneratedRoute> getGeneratedRoutes() {
    return _generatedRoutes;
  }

  @JsonProperty(PROP_MAX_METRIC_EXTERNAL_NETWORKS)
  public Long getMaxMetricExternalNetworks() {
    return _maxMetricExternalNetworks;
  }

  @JsonProperty(PROP_MAX_METRIC_STUB_NETWORKS)
  public Long getMaxMetricStubNetworks() {
    return _maxMetricStubNetworks;
  }

  @JsonProperty(PROP_MAX_METRIC_SUMMARY_NETWORKS)
  public Long getMaxMetricSummaryNetworks() {
    return _maxMetricSummaryNetworks;
  }

  @JsonProperty(PROP_MAX_METRIC_TRANSIT_LINKS)
  public Long getMaxMetricTransitLinks() {
    return _maxMetricTransitLinks;
  }

  @JsonIgnore
  public Map<IpLink, OspfNeighbor> getOspfNeighbors() {
    return _ospfNeighbors;
  }

  @Nullable
  @JsonProperty(PROP_PROCESS_ID)
  public String getProcessId() {
    return _processId;
  }

  @JsonPropertyDescription(
      "The reference bandwidth by which an interface's bandwidth is divided to determine its OSPF "
          + "cost")
  public Double getReferenceBandwidth() {
    return _referenceBandwidth;
  }

  public @Nullable Boolean getRfc1583Compatible() {
    return _rfc1583Compatible;
  }

  @JsonPropertyDescription("The router-id of this OSPF process")
  public Ip getRouterId() {
    return _routerId;
  }

  public void initInterfaceCosts(Configuration c) {
    for (OspfArea area : _areas.values()) {
      for (String ifaceName : area.getInterfaces()) {
        Interface i = c.getInterfaces().get(ifaceName);
        if (i.getActive()) {
          i.setOspfCost(computeInterfaceCost(i));
        }
      }
    }
  }

  public void setAreas(SortedMap<Long, OspfArea> areas) {
    _areas = areas;
  }

  @JsonProperty(PROP_EXPORT_POLICY)
  public void setExportPolicy(String exportPolicy) {
    _exportPolicy = exportPolicy;
  }

  @JsonProperty(PROP_EXPORT_POLICY_SOURCES)
  public void setExportPolicySources(SortedSet<String> exportPolicySources) {
    _exportPolicySources = exportPolicySources;
  }

  @JsonProperty(PROP_GENERATED_ROUTES)
  public void setGeneratedRoutes(SortedSet<GeneratedRoute> generatedRoutes) {
    _generatedRoutes = generatedRoutes;
  }

  @JsonProperty(PROP_MAX_METRIC_EXTERNAL_NETWORKS)
  public void setMaxMetricExternalNetworks(Long maxMetricExternalNetworks) {
    _maxMetricExternalNetworks = maxMetricExternalNetworks;
  }

  @JsonProperty(PROP_MAX_METRIC_STUB_NETWORKS)
  public void setMaxMetricStubNetworks(Long maxMetricStubNetworks) {
    _maxMetricStubNetworks = maxMetricStubNetworks;
  }

  @JsonProperty(PROP_MAX_METRIC_SUMMARY_NETWORKS)
  public void setMaxMetricSummaryNetworks(Long maxMetricSummaryNetworks) {
    _maxMetricSummaryNetworks = maxMetricSummaryNetworks;
  }

  @JsonProperty(PROP_MAX_METRIC_TRANSIT_LINKS)
  public void setMaxMetricTransitLinks(Long maxMetricTransitLinks) {
    _maxMetricTransitLinks = maxMetricTransitLinks;
  }

  @JsonIgnore
  public void setOspfNeighbors(Map<IpLink, OspfNeighbor> ospfNeighbors) {
    _ospfNeighbors = ospfNeighbors;
  }

  @JsonProperty(PROP_PROCESS_ID)
  public void setProcessId(@Nullable String id) {
    _processId = id;
  }

  public void setReferenceBandwidth(Double referenceBandwidth) {
    _referenceBandwidth = referenceBandwidth;
  }

  public void setRfc1583Compatible(@Nullable Boolean rfc1583Compatible) {
    _rfc1583Compatible = rfc1583Compatible;
  }

  public void setRouterId(Ip id) {
    _routerId = id;
  }
}
