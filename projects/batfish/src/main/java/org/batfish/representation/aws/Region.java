package org.batfish.representation.aws;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.Vrf;
import org.batfish.main.Batfish;
import org.batfish.representation.aws.Instance.Status;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Region implements Serializable {

  private static final long serialVersionUID = 1L;

  private Map<String, Address> _addresses = new HashMap<>();

  private Map<String, Set<SecurityGroup>> _configurationSecurityGroups = new HashMap<>();

  private Map<String, CustomerGateway> _customerGateways = new HashMap<>();

  private Map<String, ElasticsearchDomain> _elasticsearchDomains = new HashMap<>();

  private Map<String, Instance> _instances = new HashMap<>();

  private Map<String, InternetGateway> _internetGateways = new HashMap<>();

  private String _name;

  private Map<String, NatGateway> _natGateways = new HashMap<>();

  private Map<String, NetworkAcl> _networkAcls = new HashMap<>();

  private Map<String, NetworkInterface> _networkInterfaces = new HashMap<>();

  private Map<String, RdsInstance> _rdsInstances = new HashMap<>();

  private Map<String, RouteTable> _routeTables = new HashMap<>();

  private Map<String, SecurityGroup> _securityGroups = new HashMap<>();

  private Map<String, Subnet> _subnets = new HashMap<>();

  private Map<String, VpcPeeringConnection> _vpcPeerings = new HashMap<>();

  private Map<String, Vpc> _vpcs = new HashMap<>();

  private Map<String, VpnConnection> _vpnConnections = new HashMap<>();

  private Map<String, VpnGateway> _vpnGateways = new HashMap<>();

  public static final String SG_INGRESS_ACL_NAME = "~SECURITY_GROUP_INGRESS_ACL~";
  public static final String SG_EGRESS_ACL_NAME = "~SECURITY_GROUP_EGRESS_ACL~";

  public Region(String name) {
    _name = name;
  }

  public void addConfigElement(JSONObject jsonObj, BatfishLogger logger) throws JSONException {

    Iterator<?> keys = jsonObj.keys();

    while (keys.hasNext()) {
      String key = (String) keys.next();

      if (ignoreElement(key)) {
        continue;
      }

      JSONArray jsonArray = jsonObj.getJSONArray(key);

      for (int index = 0; index < jsonArray.length(); index++) {
        JSONObject childObject = jsonArray.getJSONObject(index);
        addConfigElement(key, childObject, logger);
      }
    }
  }

  private void addConfigElement(String elementType, JSONObject jsonObject, BatfishLogger logger)
      throws JSONException {
    switch (elementType) {
      case AwsVpcEntity.JSON_KEY_ADDRESSES:
        Address address = new Address(jsonObject);
        _addresses.put(address.getId(), address);
        break;
      case AwsVpcEntity.JSON_KEY_INSTANCES:
        Instance instance = new Instance(jsonObject);
        if (instance.getStatus() == Status.RUNNING) {
          _instances.put(instance.getId(), instance);
        }
        break;
      case AwsVpcEntity.JSON_KEY_CUSTOMER_GATEWAYS:
        CustomerGateway cGateway = new CustomerGateway(jsonObject);
        _customerGateways.put(cGateway.getId(), cGateway);
        break;
      case AwsVpcEntity.JSON_KEY_DB_INSTANCES:
        RdsInstance rdsInstance = new RdsInstance(jsonObject);
        if (rdsInstance.getDbInstanceStatus() == RdsInstance.Status.AVAILABLE) {
          _rdsInstances.put(rdsInstance.getId(), rdsInstance);
        }
        break;
      case AwsVpcEntity.JSON_KEY_DOMAIN_STATUS_LIST:
        ElasticsearchDomain elasticsearchDomain = new ElasticsearchDomain(jsonObject);
        // we cannot represent an elasticsearch domain without vpc and subnets as a node
        if (elasticsearchDomain.getAvailable() && elasticsearchDomain.getVpcId() != null) {
          _elasticsearchDomains.put(elasticsearchDomain.getId(), elasticsearchDomain);
        }
        break;
      case AwsVpcEntity.JSON_KEY_INTERNET_GATEWAYS:
        InternetGateway iGateway = new InternetGateway(jsonObject);
        _internetGateways.put(iGateway.getId(), iGateway);
        break;
      case AwsVpcEntity.JSON_KEY_NAT_GATEWAYS:
        NatGateway natGateway = new NatGateway(jsonObject);
        _natGateways.put(natGateway.getId(), natGateway);
        break;
      case AwsVpcEntity.JSON_KEY_NETWORK_ACLS:
        NetworkAcl networkAcl = new NetworkAcl(jsonObject);
        _networkAcls.put(networkAcl.getId(), networkAcl);
        break;
      case AwsVpcEntity.JSON_KEY_NETWORK_INTERFACES:
        NetworkInterface networkInterface = new NetworkInterface(jsonObject);
        _networkInterfaces.put(networkInterface.getId(), networkInterface);
        break;
      case AwsVpcEntity.JSON_KEY_RESERVATIONS:
        // instances are embedded inside reservations
        JSONArray jsonArray = jsonObject.getJSONArray(AwsVpcEntity.JSON_KEY_INSTANCES);
        for (int index = 0; index < jsonArray.length(); index++) {
          JSONObject childObject = jsonArray.getJSONObject(index);
          addConfigElement(AwsVpcEntity.JSON_KEY_INSTANCES, childObject, logger);
        }
        break;
      case AwsVpcEntity.JSON_KEY_ROUTE_TABLES:
        RouteTable routeTable = new RouteTable(jsonObject);
        _routeTables.put(routeTable.getId(), routeTable);
        break;
      case AwsVpcEntity.JSON_KEY_SECURITY_GROUPS:
        SecurityGroup sGroup = new SecurityGroup(jsonObject);
        _securityGroups.put(sGroup.getId(), sGroup);
        break;
      case AwsVpcEntity.JSON_KEY_SUBNETS:
        Subnet subnet = new Subnet(jsonObject);
        _subnets.put(subnet.getId(), subnet);
        break;
      case AwsVpcEntity.JSON_KEY_VPCS:
        Vpc vpc = new Vpc(jsonObject);
        _vpcs.put(vpc.getId(), vpc);
        break;
      case AwsVpcEntity.JSON_KEY_VPC_PEERING_CONNECTIONS:
        String code =
            jsonObject
                .getJSONObject(AwsVpcEntity.JSON_KEY_STATUS)
                .getString(AwsVpcEntity.JSON_KEY_CODE);
        if (!code.equals(AwsVpcEntity.STATUS_DELETED)) {
          VpcPeeringConnection vpcPeerConn = new VpcPeeringConnection(jsonObject);
          _vpcPeerings.put(vpcPeerConn.getId(), vpcPeerConn);
        }
        break;
      case AwsVpcEntity.JSON_KEY_VPN_CONNECTIONS:
        VpnConnection vpnConnection = new VpnConnection(jsonObject, logger);
        _vpnConnections.put(vpnConnection.getId(), vpnConnection);
        break;
      case AwsVpcEntity.JSON_KEY_VPN_GATEWAYS:
        VpnGateway vpnGateway = new VpnGateway(jsonObject);
        _vpnGateways.put(vpnGateway.getId(), vpnGateway);
        break;
      default:
        // do nothing here
        logger.debugf("skipping top-level element: %s\n", elementType);
    }
  }

  public Map<String, Address> getAddresses() {
    return _addresses;
  }

  public Map<String, Set<SecurityGroup>> getConfigurationSecurityGroups() {
    return _configurationSecurityGroups;
  }

  public Map<String, CustomerGateway> getCustomerGateways() {
    return _customerGateways;
  }

  public Map<String, ElasticsearchDomain> getElasticSearchDomains() {
    return _elasticsearchDomains;
  }

  public Map<String, Instance> getInstances() {
    return _instances;
  }

  public Map<String, InternetGateway> getInternetGateways() {
    return _internetGateways;
  }

  public String getName() {
    return _name;
  }

  public Map<String, NatGateway> getNatGateways() {
    return _natGateways;
  }

  public Map<String, NetworkAcl> getNetworkAcls() {
    return _networkAcls;
  }

  public Map<String, NetworkInterface> getNetworkInterfaces() {
    return _networkInterfaces;
  }

  public Map<String, RdsInstance> getRdsInstances() {
    return _rdsInstances;
  }

  public Map<String, RouteTable> getRouteTables() {
    return _routeTables;
  }

  public Map<String, SecurityGroup> getSecurityGroups() {
    return _securityGroups;
  }

  public Map<String, Subnet> getSubnets() {
    return _subnets;
  }

  public Map<String, VpcPeeringConnection> getVpcPeeringConnections() {
    return _vpcPeerings;
  }

  public Map<String, Vpc> getVpcs() {
    return _vpcs;
  }

  public Map<String, VpnConnection> getVpnConnections() {
    return _vpnConnections;
  }

  public Map<String, VpnGateway> getVpnGateways() {
    return _vpnGateways;
  }

  private boolean ignoreElement(String key) {
    switch (key) {
      case AwsVpcEntity.JSON_KEY_AVAILABILITY_ZONES:
      case AwsVpcEntity.JSON_KEY_DHCP_OPTIONS:
      case AwsVpcEntity.JSON_KEY_REGIONS:
      case AwsVpcEntity.JSON_KEY_INSTANCE_STATUSES:
      case AwsVpcEntity.JSON_KEY_PLACEMENT_GROUPS:
      case AwsVpcEntity.JSON_KEY_SERVICE_NAMES:
      case AwsVpcEntity.JSON_KEY_TAGS:
        return true;
      default:
        return false;
    }
  }

  public void toConfigurationNodes(
      AwsConfiguration awsConfiguration, Map<String, Configuration> configurationNodes) {

    // updates the Ips which have been allocated already in subnets of all interfaces
    updateAllocatedIps();
    for (Vpc vpc : getVpcs().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      Configuration cfgNode = vpc.toConfigurationNode(awsConfiguration, this, warnings);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (ElasticsearchDomain elasticsearchDomain : getElasticSearchDomains().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      Configuration cfgNode =
          elasticsearchDomain.toConfigurationNode(awsConfiguration, this, warnings);
      cfgNode.setDeviceType(DeviceType.HOST);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (InternetGateway igw : getInternetGateways().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      Configuration cfgNode = igw.toConfigurationNode(awsConfiguration, this);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (NatGateway ngw : getNatGateways().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      warnings.redFlag("NAT functionality not yet implemented for " + ngw.getId());
      Configuration cfgNode = ngw.toConfigurationNode(awsConfiguration, this, warnings);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (VpnGateway vgw : getVpnGateways().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      Configuration cfgNode = vgw.toConfigurationNode(awsConfiguration, this, warnings);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (Instance instance : getInstances().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      Configuration cfgNode = instance.toConfigurationNode(this, warnings);
      cfgNode.setDeviceType(DeviceType.HOST);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (RdsInstance rdsInstance : getRdsInstances().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      Configuration cfgNode = rdsInstance.toConfigurationNode(awsConfiguration, this, warnings);
      cfgNode.setDeviceType(DeviceType.HOST);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (Subnet subnet : getSubnets().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      Configuration cfgNode = subnet.toConfigurationNode(awsConfiguration, this, warnings);
      configurationNodes.put(cfgNode.getHostname(), cfgNode);
      awsConfiguration.getWarningsByHost().put(cfgNode.getHostname(), warnings);
    }

    for (VpnConnection vpnConnection : getVpnConnections().values()) {
      Warnings warnings = Batfish.buildWarnings(awsConfiguration.getSettings());
      vpnConnection.applyToVpnGateway(awsConfiguration, this, warnings);
      awsConfiguration.getWarningsByHost().put(vpnConnection.getId(), warnings);
    }

    applySecurityGroupsAcls(configurationNodes);

    // TODO: for now, set all interfaces to have the same bandwidth
    for (Configuration cfgNode : configurationNodes.values()) {
      for (Vrf vrf : cfgNode.getVrfs().values()) {
        for (Interface iface : vrf.getInterfaces().values()) {
          iface.setBandwidth(1E12d);
        }
      }
    }
  }

  private void applySecurityGroupsAcls(Map<String, Configuration> cfgNodes) {
    for (Entry<String, Set<SecurityGroup>> entry : _configurationSecurityGroups.entrySet()) {
      Configuration cfgNode = cfgNodes.get(entry.getKey());
      List<IpAccessListLine> inboundRules = new LinkedList<>();
      List<IpAccessListLine> outboundRules = new LinkedList<>();
      entry
          .getValue()
          .forEach(
              securityGroup ->
                  securityGroup.addInOutAccessLines(inboundRules, outboundRules, this));

      // create ACLs from inboundRules and outboundRules
      IpAccessList inAcl =
          IpAccessList.builder().setName(SG_INGRESS_ACL_NAME).setLines(inboundRules).build();
      IpAccessList outAcl =
          IpAccessList.builder().setName(SG_EGRESS_ACL_NAME).setLines(outboundRules).build();

      cfgNode.getIpAccessLists().put(SG_INGRESS_ACL_NAME, inAcl);
      cfgNode.getIpAccessLists().put(SG_EGRESS_ACL_NAME, outAcl);

      // applying the filters to all interfaces in the node
      cfgNode
          .getInterfaces()
          .values()
          .forEach(
              iface -> {
                iface.setIncomingFilter(inAcl);
                iface.setOutgoingFilter(outAcl);
              });
    }
  }

  private void updateAllocatedIps() {
    _networkInterfaces
        .values()
        .forEach(
            networkInterface ->
                _subnets
                    .get(networkInterface.getSubnetId())
                    .getAllocatedIps()
                    .addAll(
                        networkInterface
                            .getIpAddressAssociations()
                            .keySet()
                            .stream()
                            .map(Ip::asLong)
                            .collect(Collectors.toSet())));
  }

  public void updateConfigurationSecurityGroups(String configName, SecurityGroup securityGroup) {
    Set<SecurityGroup> securityGroups =
        getConfigurationSecurityGroups().computeIfAbsent(configName, k -> new HashSet<>());
    securityGroups.add(securityGroup);
  }
}
