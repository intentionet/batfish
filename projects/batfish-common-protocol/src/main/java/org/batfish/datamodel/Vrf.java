package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.dataplane.rib.RibGroup;
import org.batfish.datamodel.eigrp.EigrpProcess;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfProcess;

/** A virtual routing and forwarding (VRF) instance on a node. */
public class Vrf extends ComparableStructure<String> {

  public static class Builder extends NetworkFactoryBuilder<Vrf> {

    private String _name;

    private Configuration _owner;

    Builder(NetworkFactory networkFactory) {
      super(networkFactory, Vrf.class);
    }

    @Override
    public Vrf build() {
      String name = _name != null ? _name : generateName();
      Vrf vrf = new Vrf(name);
      if (_owner != null) {
        _owner.getVrfs().put(name, vrf);
      }
      return vrf;
    }

    public Builder setName(String name) {
      _name = name;
      return this;
    }

    public Builder setOwner(Configuration owner) {
      _owner = owner;
      return this;
    }
  }

  private static final String PROP_BGP_PROCESS = "bgpProcess";
  private static final String PROP_DESCRIPTION = "description";
  private static final String PROP_GENERATED_ROUTES = "aggregateRoutes";
  private static final String PROP_CROSS_VRF_IMPORT_POLICY = "crossVrfImportPolicy";
  private static final String PROP_CROSS_VRF_IMPORT_VRFS = "crossVrfImportVrfs";
  private static final String PROP_INTERFACES = "interfaces";
  private static final String PROP_ISIS_PROCESS = "isisProcess";
  private static final String PROP_EIGRP_PROCESSES = "eigrpProcesses";
  private static final String PROP_KERNEL_ROUTES = "kernelRoutes";
  private static final String PROP_OSPF_PROCESS = "ospfProcess";
  private static final String PROP_RIP_PROCESS = "ripProcess";
  private static final String PROP_STATIC_ROUTES = "staticRoutes";
  private static final String PROP_VNI_SETTINGS = "vniSettings";

  private static final long serialVersionUID = 1L;

  private SortedMap<RoutingProtocol, RibGroup> _appliedRibGroups;
  private transient NavigableSet<BgpAdvertisement> _bgpAdvertisements;
  private BgpProcess _bgpProcess;
  private String _description;
  private NavigableSet<GeneratedRoute6> _generatedIpv6Routes;
  private NavigableSet<GeneratedRoute> _generatedRoutes;
  private Map<Long, EigrpProcess> _eigrpProcesses;
  @Nullable private String _crossVrfImportPolicy;
  @Nullable private List<String> _crossVrfImportVrfs;
  private transient SortedSet<String> _interfaceNames;
  private NavigableMap<String, Interface> _interfaces;
  private IsisProcess _isisProcess;
  private SortedSet<KernelRoute> _kernelRoutes;
  private transient NavigableSet<BgpAdvertisement> _originatedAdvertisements;
  private transient NavigableSet<BgpAdvertisement> _originatedEbgpAdvertisements;
  private transient NavigableSet<BgpAdvertisement> _originatedIbgpAdvertisements;
  @Nullable private OspfProcess _ospfProcess;
  private transient NavigableSet<BgpAdvertisement> _receivedAdvertisements;
  private transient NavigableSet<BgpAdvertisement> _receivedEbgpAdvertisements;
  private transient NavigableSet<BgpAdvertisement> _receivedIbgpAdvertisements;
  private RipProcess _ripProcess;
  private transient NavigableSet<Route> _routes;
  private transient NavigableSet<BgpAdvertisement> _sentAdvertisements;
  private transient NavigableSet<BgpAdvertisement> _sentEbgpAdvertisements;
  private transient NavigableSet<BgpAdvertisement> _sentIbgpAdvertisements;
  private SnmpServer _snmpServer;
  private SortedSet<StaticRoute> _staticRoutes;
  private NavigableMap<Integer, VniSettings> _vniSettings;

  @JsonCreator
  public Vrf(@JsonProperty(PROP_NAME) String name) {
    super(name);
    _appliedRibGroups = ImmutableSortedMap.of();
    _eigrpProcesses = new TreeMap<>();
    _generatedRoutes = new TreeSet<>();
    _generatedIpv6Routes = new TreeSet<>();
    _interfaces = new TreeMap<>();
    _kernelRoutes = ImmutableSortedSet.of();
    _staticRoutes = new TreeSet<>();
    _vniSettings = new TreeMap<>();
  }

  /** Return any RIB groups applied to a given routing protocol */
  public Map<RoutingProtocol, RibGroup> getAppliedRibGroups() {
    return _appliedRibGroups;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getBgpAdvertisements() {
    return _bgpAdvertisements;
  }

  @JsonProperty(PROP_BGP_PROCESS)
  @JsonPropertyDescription("BGP routing process for this VRF")
  public BgpProcess getBgpProcess() {
    return _bgpProcess;
  }

  @JsonProperty(PROP_DESCRIPTION)
  @JsonPropertyDescription("Description for this VRF")
  public String getDescription() {
    return _description;
  }

  @JsonPropertyDescription("Generated IPV6 routes for this VRF")
  public NavigableSet<GeneratedRoute6> getGeneratedIpv6Routes() {
    return _generatedIpv6Routes;
  }

  @JsonProperty(PROP_GENERATED_ROUTES)
  @JsonPropertyDescription("Generated IPV4 routes for this VRF")
  public NavigableSet<GeneratedRoute> getGeneratedRoutes() {
    return _generatedRoutes;
  }

  /** @return EIGRP routing processes for this VRF */
  @JsonProperty(PROP_EIGRP_PROCESSES)
  public Map<Long, EigrpProcess> getEigrpProcesses() {
    return _eigrpProcesses;
  }

  /** Name of policy used to filter incoming routes leaked from other VRFs */
  @Nullable
  @JsonProperty(PROP_CROSS_VRF_IMPORT_POLICY)
  public String getCrossVrfImportPolicy() {
    return _crossVrfImportPolicy;
  }

  /** Names of other VRFs that leak routes into this one */
  @Nullable
  @JsonProperty(PROP_CROSS_VRF_IMPORT_VRFS)
  public List<String> getCrossVrfImportVrfs() {
    return _crossVrfImportVrfs;
  }

  @JsonProperty(PROP_INTERFACES)
  @JsonPropertyDescription("Interfaces assigned to this VRF")
  public SortedSet<String> getInterfaceNames() {
    if (_interfaces != null && !_interfaces.isEmpty()) {
      return new TreeSet<>(_interfaces.keySet());
    } else {
      return firstNonNull(_interfaceNames, ImmutableSortedSet.of());
    }
  }

  @JsonIgnore
  public Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  @JsonProperty(PROP_ISIS_PROCESS)
  @JsonPropertyDescription("IS-IS routing process for this VRF")
  public IsisProcess getIsisProcess() {
    return _isisProcess;
  }

  @JsonProperty(PROP_KERNEL_ROUTES)
  public @Nonnull SortedSet<KernelRoute> getKernelRoutes() {
    return _kernelRoutes;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getOriginatedAdvertisements() {
    return _originatedAdvertisements;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getOriginatedEbgpAdvertisements() {
    return _originatedEbgpAdvertisements;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getOriginatedIbgpAdvertisements() {
    return _originatedIbgpAdvertisements;
  }

  @JsonPropertyDescription("OSPF routing process for this VRF")
  @JsonProperty(PROP_OSPF_PROCESS)
  public OspfProcess getOspfProcess() {
    return _ospfProcess;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getReceivedAdvertisements() {
    return _receivedAdvertisements;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getReceivedEbgpAdvertisements() {
    return _receivedEbgpAdvertisements;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getReceivedIbgpAdvertisements() {
    return _receivedIbgpAdvertisements;
  }

  @JsonProperty(PROP_RIP_PROCESS)
  public RipProcess getRipProcess() {
    return _ripProcess;
  }

  @JsonIgnore
  public NavigableSet<Route> getRoutes() {
    return _routes;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getSentAdvertisements() {
    return _sentAdvertisements;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getSentEbgpAdvertisements() {
    return _sentEbgpAdvertisements;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getSentIbgpAdvertisements() {
    return _sentIbgpAdvertisements;
  }

  public SnmpServer getSnmpServer() {
    return _snmpServer;
  }

  @JsonPropertyDescription("Static routes for this VRF")
  @JsonProperty(PROP_STATIC_ROUTES)
  public SortedSet<StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  @JsonProperty(PROP_VNI_SETTINGS)
  public NavigableMap<Integer, VniSettings> getVniSettings() {
    return _vniSettings;
  }

  public void initBgpAdvertisements() {
    _bgpAdvertisements = new TreeSet<>();
    _originatedAdvertisements = new TreeSet<>();
    _originatedEbgpAdvertisements = new TreeSet<>();
    _originatedIbgpAdvertisements = new TreeSet<>();
    _receivedAdvertisements = new TreeSet<>();
    _receivedEbgpAdvertisements = new TreeSet<>();
    _receivedIbgpAdvertisements = new TreeSet<>();
    _sentAdvertisements = new TreeSet<>();
    _sentEbgpAdvertisements = new TreeSet<>();
    _sentIbgpAdvertisements = new TreeSet<>();
  }

  public void initRoutes() {
    _routes = new TreeSet<>();
  }

  public void resolveReferences(Configuration owner) {
    if (_interfaceNames != null) {
      for (String ifaceName : _interfaceNames) {
        _interfaces.put(ifaceName, owner.getAllInterfaces().get(ifaceName));
      }
    }
  }

  public void setAppliedRibGroups(Map<RoutingProtocol, RibGroup> appliedRibGroups) {
    _appliedRibGroups = ImmutableSortedMap.copyOf(appliedRibGroups);
  }

  @JsonProperty(PROP_BGP_PROCESS)
  public void setBgpProcess(BgpProcess process) {
    _bgpProcess = process;
  }

  @JsonProperty(PROP_DESCRIPTION)
  public void setDescription(String description) {
    _description = description;
  }

  public void setGeneratedIpv6Routes(NavigableSet<GeneratedRoute6> generatedIpv6Routes) {
    _generatedIpv6Routes = generatedIpv6Routes;
  }

  @JsonProperty(PROP_GENERATED_ROUTES)
  public void setGeneratedRoutes(NavigableSet<GeneratedRoute> generatedRoutes) {
    _generatedRoutes = generatedRoutes;
  }

  @JsonProperty(PROP_EIGRP_PROCESSES)
  public void setEigrpProcesses(Map<Long, EigrpProcess> eigrpProcesses) {
    _eigrpProcesses = eigrpProcesses;
  }

  @JsonProperty(PROP_CROSS_VRF_IMPORT_POLICY)
  public void setCrossVrfImportPolicy(@Nonnull String crossVrfImportPolicy) {
    _crossVrfImportPolicy = crossVrfImportPolicy;
  }

  @JsonProperty(PROP_CROSS_VRF_IMPORT_VRFS)
  public void setCrossVrfImportVrfs(@Nonnull List<String> crossVrfImportVrfs) {
    _crossVrfImportVrfs = ImmutableList.copyOf(crossVrfImportVrfs);
  }

  @JsonProperty(PROP_INTERFACES)
  public void setInterfaceNames(SortedSet<String> interfaceNames) {
    _interfaceNames = interfaceNames;
  }

  @JsonIgnore
  public void setInterfaces(NavigableMap<String, Interface> interfaces) {
    _interfaces = interfaces;
  }

  @JsonProperty(PROP_ISIS_PROCESS)
  public void setIsisProcess(IsisProcess process) {
    _isisProcess = process;
  }

  @JsonProperty(PROP_KERNEL_ROUTES)
  public void setKernelRoutes(@Nonnull SortedSet<KernelRoute> kernelRoutes) {
    _kernelRoutes = kernelRoutes;
  }

  @JsonProperty(PROP_OSPF_PROCESS)
  public void setOspfProcess(OspfProcess process) {
    _ospfProcess = process;
  }

  @JsonProperty(PROP_RIP_PROCESS)
  public void setRipProcess(RipProcess ripProcess) {
    _ripProcess = ripProcess;
  }

  public void setSnmpServer(SnmpServer snmpServer) {
    _snmpServer = snmpServer;
  }

  @JsonProperty(PROP_STATIC_ROUTES)
  public void setStaticRoutes(SortedSet<StaticRoute> staticRoutes) {
    _staticRoutes = staticRoutes;
  }

  @JsonProperty(PROP_VNI_SETTINGS)
  public void setVniSettings(NavigableMap<Integer, VniSettings> vniSetting) {
    _vniSettings = vniSetting;
  }
}
