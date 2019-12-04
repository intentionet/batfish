package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.dataplane.rib.RibGroup;
import org.batfish.datamodel.eigrp.EigrpProcess;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;

/** A virtual routing and forwarding (VRF) instance on a node. */
public class Vrf extends ComparableStructure<String> {

  public static class Builder extends NetworkFactoryBuilder<Vrf> {

    @Nullable private String _name;
    @Nullable private Configuration _owner;
    @Nonnull private Map<Long, EigrpProcess> _eigrpProcesses = ImmutableMap.of();

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
      vrf.setEigrpProcesses(_eigrpProcesses);
      return vrf;
    }

    public Builder setEigrpProcesses(@Nonnull Map<Long, EigrpProcess> eigrpProcesses) {
      _eigrpProcesses = eigrpProcesses;
      return this;
    }

    public Builder setName(@Nullable String name) {
      _name = name;
      return this;
    }

    public Builder setOwner(@Nullable Configuration owner) {
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
  private static final String PROP_OSPF_PROCESSES = "ospfProcesses";
  private static final String PROP_RIP_PROCESS = "ripProcess";
  private static final String PROP_STATIC_ROUTES = "staticRoutes";

  public static @Nonnull Builder builder() {
    return new Builder(null);
  }

  private SortedMap<RoutingProtocol, RibGroup> _appliedRibGroups;
  private BgpProcess _bgpProcess;
  private String _description;
  private NavigableSet<GeneratedRoute6> _generatedIpv6Routes;
  private NavigableSet<GeneratedRoute> _generatedRoutes;
  private SortedMap<Long, EigrpProcess> _eigrpProcesses;
  @Nullable private String _crossVrfImportPolicy;
  @Nullable private List<String> _crossVrfImportVrfs;
  private transient SortedSet<String> _interfaceNames;
  private NavigableMap<String, Interface> _interfaces;
  private IsisProcess _isisProcess;
  private SortedSet<KernelRoute> _kernelRoutes;
  @Nonnull private SortedMap<String, OspfProcess> _ospfProcesses;
  private RipProcess _ripProcess;
  private SnmpServer _snmpServer;
  private SortedSet<StaticRoute> _staticRoutes;
  private Map<Integer, Layer2Vni> _layer2Vnis;
  private Map<Integer, Layer3Vni> _layer3Vnis;

  public Vrf(@Nonnull String name) {
    super(name);
    _appliedRibGroups = ImmutableSortedMap.of();
    _eigrpProcesses = ImmutableSortedMap.of();
    _generatedRoutes = new TreeSet<>();
    _generatedIpv6Routes = new TreeSet<>();
    _interfaces = new TreeMap<>();
    _kernelRoutes = ImmutableSortedSet.of();
    _ospfProcesses = ImmutableSortedMap.of();
    _staticRoutes = new TreeSet<>();
    _layer2Vnis = ImmutableMap.of();
    _layer3Vnis = ImmutableMap.of();
  }

  @JsonCreator
  private static Vrf create(
      @Nullable @JsonProperty(PROP_NAME) String name,
      @Nullable @JsonProperty(PROP_OSPF_PROCESSES) Map<String, OspfProcess> ospfProcesses,
      // For backwards compatible deserialization
      @Nullable @JsonProperty(PROP_OSPF_PROCESS) OspfProcess ospfProcess) {
    checkArgument(name != null, "%s must be provided", PROP_NAME);
    Vrf v = new Vrf(name);
    if (ospfProcesses != null) {
      v.setOspfProcesses(ImmutableSortedMap.copyOf(ospfProcesses));
    } else if (ospfProcess != null) {
      v.setOspfProcesses(ImmutableSortedMap.of(ospfProcess.getProcessId(), ospfProcess));
    }
    return v;
  }

  /** Return any RIB groups applied to a given routing protocol */
  public Map<RoutingProtocol, RibGroup> getAppliedRibGroups() {
    return _appliedRibGroups;
  }

  /** BGP routing process for this VRF. */
  @JsonProperty(PROP_BGP_PROCESS)
  public BgpProcess getBgpProcess() {
    return _bgpProcess;
  }

  /** Description for this VRF. */
  @JsonProperty(PROP_DESCRIPTION)
  public String getDescription() {
    return _description;
  }

  /** Generated IPV6 routes for this VRF. */
  public NavigableSet<GeneratedRoute6> getGeneratedIpv6Routes() {
    return _generatedIpv6Routes;
  }

  /** Generated IPV4 routes for this VRF. */
  @JsonProperty(PROP_GENERATED_ROUTES)
  public NavigableSet<GeneratedRoute> getGeneratedRoutes() {
    return _generatedRoutes;
  }

  /** @return EIGRP routing processes for this VRF. This map cannot be modified. */
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

  /** Interfaces assigned to this VRF. */
  @JsonProperty(PROP_INTERFACES)
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

  @JsonIgnore
  public @Nonnull Stream<Interface> getActiveInterfaces() {
    return _interfaces.values().stream().filter(Interface::getActive);
  }

  /** IS-IS routing process for this VRF. */
  @JsonProperty(PROP_ISIS_PROCESS)
  public IsisProcess getIsisProcess() {
    return _isisProcess;
  }

  @JsonProperty(PROP_KERNEL_ROUTES)
  public @Nonnull SortedSet<KernelRoute> getKernelRoutes() {
    return _kernelRoutes;
  }

  /** OSPF routing processes for this VRF, keyed on {@link OspfProcess#getProcessId()}. */
  @JsonProperty(PROP_OSPF_PROCESSES)
  @Nonnull
  public Map<String, OspfProcess> getOspfProcesses() {
    return _ospfProcesses;
  }

  @JsonProperty(PROP_RIP_PROCESS)
  public RipProcess getRipProcess() {
    return _ripProcess;
  }

  public SnmpServer getSnmpServer() {
    return _snmpServer;
  }

  /** Static routes for this VRF. */
  @JsonProperty(PROP_STATIC_ROUTES)
  public SortedSet<StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  @JsonIgnore
  public Map<Integer, Layer2Vni> getLayer2Vnis() {
    return _layer2Vnis;
  }

  public void setLayer2Vnis(Collection<Layer2Vni> layer2Vnis) {
    _layer2Vnis = toImmutableMap(layer2Vnis, Layer2Vni::getVni, Function.identity());
  }

  public void addLayer2Vni(Layer2Vni vni) {
    _layer2Vnis =
        ImmutableMap.<Integer, Layer2Vni>builder()
            .putAll(_layer2Vnis)
            .put(vni.getVni(), vni)
            .build();
  }

  @JsonIgnore
  public Map<Integer, Layer3Vni> getLayer3Vnis() {
    return _layer3Vnis;
  }

  public void setLayer3Vnis(Collection<Layer3Vni> layer3Vnis) {
    _layer3Vnis = toImmutableMap(layer3Vnis, Layer3Vni::getVni, Function.identity());
  }

  public void addLayer3Vni(Layer3Vni vni) {
    _layer3Vnis =
        ImmutableMap.<Integer, Layer3Vni>builder()
            .putAll(_layer3Vnis)
            .put(vni.getVni(), vni)
            .build();
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

  // For Jackson/Builder use only.
  @JsonProperty(PROP_EIGRP_PROCESSES)
  private void setEigrpProcesses(@Nullable Map<Long, EigrpProcess> eigrpProcesses) {
    _eigrpProcesses =
        ImmutableSortedMap.copyOf(firstNonNull(eigrpProcesses, ImmutableSortedMap.of()));
  }

  /** Add an {@link EigrpProcess} to this VRF */
  public void addEigrpProcess(@Nonnull EigrpProcess proc) {
    _eigrpProcesses =
        ImmutableSortedMap.<Long, EigrpProcess>naturalOrder()
            .putAll(_eigrpProcesses)
            .put(proc.getAsn(), proc)
            .build();
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

  @JsonIgnore
  public void setOspfProcesses(@Nonnull SortedMap<String, OspfProcess> processes) {
    _ospfProcesses = processes;
  }

  /**
   * Sets the VRF's OSPF processes to a map keyed on process IDs with the given {@code processes} as
   * values.
   */
  @JsonIgnore
  public void setOspfProcesses(@Nonnull Stream<OspfProcess> processes) {
    setOspfProcesses(
        processes.collect(
            ImmutableSortedMap.toImmutableSortedMap(
                Comparator.naturalOrder(), OspfProcess::getProcessId, Functions.identity())));
  }

  public void addOspfProcess(@Nonnull OspfProcess ospfProcess) {
    _ospfProcesses =
        ImmutableSortedMap.<String, OspfProcess>naturalOrder()
            .putAll(_ospfProcesses)
            .put(ospfProcess.getProcessId(), ospfProcess)
            .build();
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
}
