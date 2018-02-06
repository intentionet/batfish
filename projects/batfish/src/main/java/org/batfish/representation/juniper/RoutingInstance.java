package org.batfish.representation.juniper;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.SnmpServer;

public class RoutingInstance implements Serializable {

  private static final double DEFAULT_OSPF_REFERENCE_BANDWIDTH = 1E9;

  private static final String MASTER_INTERFACE_NAME = "MASTER_INTERFACE";

  /** */
  private static final long serialVersionUID = 1L;

  private Integer _as;

  private final SortedMap<String, DhcpRelayGroup> _dhcpRelayGroups;

  private final SortedMap<String, DhcpRelayServerGroup> _dhcpRelayServerGroups;

  private String _domainName;

  private String _forwardingTableExportPolicy;

  private int _forwardingTableExportPolicyLine;

  private final Interface _globalMasterInterface;

  private String _hostname;

  private final Map<String, Interface> _interfaces;

  private Map<Ip, IpBgpGroup> _ipBgpGroups;

  private final IsisSettings _isisSettings;

  private BgpGroup _masterBgpGroup;

  private String _name;

  private Map<String, NamedBgpGroup> _namedBgpGroups;

  private final Map<String, NodeDevice> _nodeDevices;

  private Map<Long, OspfArea> _ospfAreas;

  private Map<String, Integer> _ospfExportPolicies;

  private double _ospfReferenceBandwidth;

  private final Map<String, RoutingInformationBase> _ribs;

  private Ip _routerId;

  private SnmpServer _snmpServer;

  private final JuniperSystem _system;

  public RoutingInstance(String name) {
    _dhcpRelayGroups = new TreeMap<>();
    _dhcpRelayServerGroups = new TreeMap<>();
    _isisSettings = new IsisSettings();
    _interfaces = new TreeMap<>();
    _ipBgpGroups = new TreeMap<>();
    _masterBgpGroup = new BgpGroup();
    _masterBgpGroup.setMultipath(false);
    _masterBgpGroup.setMultipathMultipleAs(false);
    _globalMasterInterface = new Interface(MASTER_INTERFACE_NAME, -1);
    _globalMasterInterface.setRoutingInstance(name);
    _name = name;
    _namedBgpGroups = new TreeMap<>();
    _nodeDevices = new TreeMap<>();
    _ospfAreas = new TreeMap<>();
    _ospfExportPolicies = new LinkedHashMap<>();
    _ospfReferenceBandwidth = DEFAULT_OSPF_REFERENCE_BANDWIDTH;
    _ribs = new TreeMap<>();
    _ribs.put(
        RoutingInformationBase.RIB_IPV4_UNICAST,
        new RoutingInformationBase(RoutingInformationBase.RIB_IPV4_UNICAST));
    _ribs.put(
        RoutingInformationBase.RIB_IPV4_MULTICAST,
        new RoutingInformationBase(RoutingInformationBase.RIB_IPV4_MULTICAST));
    _ribs.put(
        RoutingInformationBase.RIB_IPV4_MPLS,
        new RoutingInformationBase(RoutingInformationBase.RIB_IPV4_MPLS));
    _ribs.put(
        RoutingInformationBase.RIB_IPV6_UNICAST,
        new RoutingInformationBase(RoutingInformationBase.RIB_IPV6_UNICAST));
    _ribs.put(
        RoutingInformationBase.RIB_MPLS,
        new RoutingInformationBase(RoutingInformationBase.RIB_MPLS));
    _ribs.put(
        RoutingInformationBase.RIB_ISIS,
        new RoutingInformationBase(RoutingInformationBase.RIB_ISIS));
    _system = new JuniperSystem();
  }

  public Integer getAs() {
    return _as;
  }

  public SortedMap<String, DhcpRelayGroup> getDhcpRelayGroups() {
    return _dhcpRelayGroups;
  }

  public SortedMap<String, DhcpRelayServerGroup> getDhcpRelayServerGroups() {
    return _dhcpRelayServerGroups;
  }

  public String getDomainName() {
    return _domainName;
  }

  public String getForwardingTableExportPolicy() {
    return _forwardingTableExportPolicy;
  }

  public int getForwardingTableExportPolicyLine() {
    return _forwardingTableExportPolicyLine;
  }

  public Interface getGlobalMasterInterface() {
    return _globalMasterInterface;
  }

  public String getHostname() {
    return _hostname;
  }

  public Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public Map<Ip, IpBgpGroup> getIpBgpGroups() {
    return _ipBgpGroups;
  }

  public IsisSettings getIsisSettings() {
    return _isisSettings;
  }

  public BgpGroup getMasterBgpGroup() {
    return _masterBgpGroup;
  }

  public String getName() {
    return _name;
  }

  public Map<String, NamedBgpGroup> getNamedBgpGroups() {
    return _namedBgpGroups;
  }

  public Map<String, NodeDevice> getNodeDevices() {
    return _nodeDevices;
  }

  public Map<Long, OspfArea> getOspfAreas() {
    return _ospfAreas;
  }

  public Map<String, Integer> getOspfExportPolicies() {
    return _ospfExportPolicies;
  }

  public double getOspfReferenceBandwidth() {
    return _ospfReferenceBandwidth;
  }

  public Map<String, RoutingInformationBase> getRibs() {
    return _ribs;
  }

  public Ip getRouterId() {
    return _routerId;
  }

  public SnmpServer getSnmpServer() {
    return _snmpServer;
  }

  public JuniperSystem getSystem() {
    return _system;
  }

  public void setAs(int as) {
    _as = as;
  }

  public void setDomainName(String domainName) {
    _domainName = domainName;
  }

  public void setForwardingTableExportPolicy(String forwardingTableExportPolicy) {
    _forwardingTableExportPolicy = forwardingTableExportPolicy;
  }

  public void setForwardingTableExportPolicyLine(int forwardingTableExportPolicyLine) {
    _forwardingTableExportPolicyLine = forwardingTableExportPolicyLine;
  }

  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  public void setOspfReferenceBandwidth(double ospfReferenceBandwidth) {
    _ospfReferenceBandwidth = ospfReferenceBandwidth;
  }

  public void setRouterId(Ip routerId) {
    _routerId = routerId;
  }

  public void setSnmpServer(SnmpServer snmpServer) {
    _snmpServer = snmpServer;
  }
}
