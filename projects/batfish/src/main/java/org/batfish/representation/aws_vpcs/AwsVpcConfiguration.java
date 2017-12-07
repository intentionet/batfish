package org.batfish.representation.aws_vpcs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.GenericConfigObject;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class AwsVpcConfiguration implements Serializable, GenericConfigObject {

  private static final long INITIAL_GENERATED_IP = Ip.FIRST_CLASS_E_EXPERIMENTAL_IP.asLong();

  private static final long serialVersionUID = 1L;

  private Map<String, Address> _addresses = new HashMap<>();

  private Map<String, Configuration> _configurationNodes = new HashMap<>();

  private long _currentGeneratedIpAsLong;

  private Map<String, CustomerGateway> _customerGateways = new HashMap<>();

  private Map<String, Instance> _instances = new HashMap<>();

  private Map<String, InternetGateway> _internetGateways = new HashMap<>();

  private Map<String, NatGateway> _natGateways = new HashMap<>();

  private Map<String, NetworkAcl> _networkAcls = new HashMap<>();

  private Map<String, NetworkInterface> _networkInterfaces = new HashMap<>();

  private Map<String, RouteTable> _routeTables = new HashMap<>();

  private Map<String, SecurityGroup> _securityGroups = new HashMap<>();

  private Map<String, Subnet> _subnets = new HashMap<>();

  private Map<String, VpcPeeringConnection> _vpcPeerings = new HashMap<>();

  private Map<String, Vpc> _vpcs = new HashMap<>();

  private Map<String, VpnConnection> _vpnConnections = new HashMap<>();

  private Map<String, VpnGateway> _vpnGateways = new HashMap<>();

  private transient Warnings _warnings;

  public AwsVpcConfiguration() {
    _currentGeneratedIpAsLong = INITIAL_GENERATED_IP;
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
        Address address = new Address(jsonObject, logger);
        _addresses.put(address.getId(), address);
        break;
      case AwsVpcEntity.JSON_KEY_INSTANCES:
        Instance instance = new Instance(jsonObject, logger);
        _instances.put(instance.getId(), instance);
        break;
      case AwsVpcEntity.JSON_KEY_CUSTOMER_GATEWAYS:
        CustomerGateway cGateway = new CustomerGateway(jsonObject, logger);
        _customerGateways.put(cGateway.getId(), cGateway);
        break;
      case AwsVpcEntity.JSON_KEY_INTERNET_GATEWAYS:
        InternetGateway iGateway = new InternetGateway(jsonObject, logger);
        _internetGateways.put(iGateway.getId(), iGateway);
        break;
      case AwsVpcEntity.JSON_KEY_NAT_GATEWAYS:
        NatGateway natGateway = new NatGateway(jsonObject, logger);
        _natGateways.put(natGateway.getId(), natGateway);
        break;
      case AwsVpcEntity.JSON_KEY_NETWORK_ACLS:
        NetworkAcl networkAcl = new NetworkAcl(jsonObject, logger);
        _networkAcls.put(networkAcl.getId(), networkAcl);
        break;
      case AwsVpcEntity.JSON_KEY_NETWORK_INTERFACES:
        NetworkInterface networkInterface = new NetworkInterface(jsonObject, logger);
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
        RouteTable routeTable = new RouteTable(jsonObject, logger);
        _routeTables.put(routeTable.getId(), routeTable);
        break;
      case AwsVpcEntity.JSON_KEY_SECURITY_GROUPS:
        SecurityGroup sGroup = new SecurityGroup(jsonObject, logger);
        _securityGroups.put(sGroup.getId(), sGroup);
        break;
      case AwsVpcEntity.JSON_KEY_SUBNETS:
        Subnet subnet = new Subnet(jsonObject, logger);
        _subnets.put(subnet.getId(), subnet);
        break;
      case AwsVpcEntity.JSON_KEY_VPCS:
        Vpc vpc = new Vpc(jsonObject, logger);
        _vpcs.put(vpc.getId(), vpc);
        break;
      case AwsVpcEntity.JSON_KEY_VPC_PEERING_CONNECTIONS:
        String code =
            jsonObject
                .getJSONObject(AwsVpcEntity.JSON_KEY_STATUS)
                .getString(AwsVpcEntity.JSON_KEY_CODE);
        if (!code.equals(AwsVpcEntity.STATUS_DELETED)) {
          VpcPeeringConnection vpcPeerConn = new VpcPeeringConnection(jsonObject, logger);
          _vpcPeerings.put(vpcPeerConn.getId(), vpcPeerConn);
        }
        break;
      case AwsVpcEntity.JSON_KEY_VPN_CONNECTIONS:
        VpnConnection vpnConnection = new VpnConnection(jsonObject, logger);
        _vpnConnections.put(vpnConnection.getId(), vpnConnection);
        break;
      case AwsVpcEntity.JSON_KEY_VPN_GATEWAYS:
        VpnGateway vpnGateway = new VpnGateway(jsonObject, logger);
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

  public Map<String, Configuration> getConfigurationNodes() {
    return _configurationNodes;
  }

  public Map<String, CustomerGateway> getCustomerGateways() {
    return _customerGateways;
  }

  public Map<String, Instance> getInstances() {
    return _instances;
  }

  public Map<String, InternetGateway> getInternetGateways() {
    return _internetGateways;
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

  public synchronized Prefix getNextGeneratedLinkSubnet() {
    Ip prefixBase = new Ip(_currentGeneratedIpAsLong);
    Prefix val = new Prefix(prefixBase, 31);
    _currentGeneratedIpAsLong += 2L;
    return val;
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

  public Warnings getWarnings() {
    return _warnings;
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

  public Map<String, Configuration> toConfigurations(Warnings warnings) {
    _warnings = warnings;

    for (Vpc vpc : _vpcs.values()) {
      Configuration cfgNode = vpc.toConfigurationNode(this);
      _configurationNodes.put(cfgNode.getName(), cfgNode);
    }

    for (InternetGateway igw : _internetGateways.values()) {
      Configuration cfgNode = igw.toConfigurationNode(this);
      _configurationNodes.put(cfgNode.getName(), cfgNode);
    }

    for (NatGateway ngw : _natGateways.values()) {
      _warnings.redFlag("NAT functionality not yet implemented for " + ngw.getId());
      Configuration cfgNode = ngw.toConfigurationNode(this);
      _configurationNodes.put(cfgNode.getName(), cfgNode);
    }

    for (VpnGateway vgw : _vpnGateways.values()) {
      Configuration cfgNode = vgw.toConfigurationNode(this);
      _configurationNodes.put(cfgNode.getName(), cfgNode);
    }

    for (Instance instance : _instances.values()) {
      Configuration cfgNode = instance.toConfigurationNode(this);
      _configurationNodes.put(cfgNode.getName(), cfgNode);
    }

    for (Subnet subnet : _subnets.values()) {
      Configuration cfgNode = subnet.toConfigurationNode(this);
      _configurationNodes.put(cfgNode.getName(), cfgNode);
    }

    for (VpnConnection vpnConnection : _vpnConnections.values()) {
      vpnConnection.applyToVpnGateway(this);
    }

    // TODO: for now, set all interfaces to have the same bandwidth
    for (Configuration cfgNode : _configurationNodes.values()) {
      for (Vrf vrf : cfgNode.getVrfs().values()) {
        for (Interface iface : vrf.getInterfaces().values()) {
          iface.setBandwidth(1E12d);
        }
      }
    }

    return _configurationNodes;
  }
}
