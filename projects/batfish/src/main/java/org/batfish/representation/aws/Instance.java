package org.batfish.representation.aws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Instance implements AwsVpcEntity, Serializable {

  public enum Status {
    PENDING("pending"),
    RUNNING("running"),
    SHUTTING_DOWN("shutting-down"),
    TERMINATED("terminated"),
    STOPPING("stopping"),
    STOPPED("stopped");

    private static final Map<String, Status> MAP = initMap();

    @JsonCreator
    public static Status fromString(String name) {
      Status value = MAP.get(name.toLowerCase());
      if (value == null) {
        throw new BatfishException(
            "No " + Status.class.getSimpleName() + " with name: '" + name + "'");
      }
      return value;
    }

    private static Map<String, Status> initMap() {
      ImmutableMap.Builder<String, Status> map = ImmutableMap.builder();
      for (Status value : Status.values()) {
        String name = value._name.toLowerCase();
        map.put(name, value);
      }
      return map.build();
    }

    private final String _name;

    Status(String name) {
      _name = name;
    }

    @JsonValue
    public String getName() {
      return _name;
    }
  }

  private static final long serialVersionUID = 1L;

  private transient IpAccessList _inAcl;

  private final String _instanceId;

  private final List<String> _networkInterfaces;

  private transient IpAccessList _outAcl;

  private final List<String> _securityGroups;

  private final Status _status;

  private final String _subnetId;

  private final Map<String, String> _tags;

  private final String _vpcId;

  public Instance(JSONObject jObj, BatfishLogger logger) throws JSONException {
    _securityGroups = new LinkedList<>();
    _networkInterfaces = new LinkedList<>();
    _instanceId = jObj.getString(JSON_KEY_INSTANCE_ID);

    boolean hasVpcId = jObj.has(JSON_KEY_VPC_ID);
    _vpcId = hasVpcId ? jObj.getString(JSON_KEY_VPC_ID) : null;
    _subnetId = hasVpcId ? jObj.getString(JSON_KEY_SUBNET_ID) : null;

    JSONArray securityGroups = jObj.getJSONArray(JSON_KEY_SECURITY_GROUPS);
    initSecurityGroups(securityGroups, logger);

    JSONArray networkInterfaces = jObj.getJSONArray(JSON_KEY_NETWORK_INTERFACES);
    initNetworkInterfaces(networkInterfaces, logger);

    _tags = new HashMap<>();
    JSONArray tagArray = jObj.getJSONArray(JSON_KEY_TAGS);
    for (int index = 0; index < tagArray.length(); index++) {
      JSONObject childObject = tagArray.getJSONObject(index);
      _tags.put(childObject.getString("Key"), childObject.getString("Value"));
    }

    String stateName = jObj.getJSONObject(JSON_KEY_STATE).getString("Name");
    _status = Status.fromString(stateName);

    // check if the public and private ip addresses are associated with an
    // interface
  }

  @Override
  public String getId() {
    return _instanceId;
  }

  public IpAccessList getInAcl() {
    return _inAcl;
  }

  public String getInstanceId() {
    return _instanceId;
  }

  public List<String> getNetworkInterfaces() {
    return _networkInterfaces;
  }

  public IpAccessList getOutAcl() {
    return _outAcl;
  }

  public List<String> getSecurityGroups() {
    return _securityGroups;
  }

  public Status getStatus() {
    return _status;
  }

  public String getSubnetId() {
    return _subnetId;
  }

  public String getVpcId() {
    return _vpcId;
  }

  private void initNetworkInterfaces(JSONArray routes, BatfishLogger logger) throws JSONException {

    for (int index = 0; index < routes.length(); index++) {
      JSONObject childObject = routes.getJSONObject(index);
      _networkInterfaces.add(childObject.getString(JSON_KEY_NETWORK_INTERFACE_ID));
    }
  }

  private void initSecurityGroups(JSONArray associations, BatfishLogger logger)
      throws JSONException {

    for (int index = 0; index < associations.length(); index++) {
      JSONObject childObject = associations.getJSONObject(index);
      _securityGroups.add(childObject.getString(JSON_KEY_GROUP_ID));
    }
  }

  public Configuration toConfigurationNode(AwsConfiguration awsVpcConfig, Region region) {
    String sgIngressAclName = "~SECURITY_GROUP_INGRESS_ACL~";
    String sgEgressAclName = "~SECURITY_GROUP_EGRESS_ACL~";
    String name = _tags.getOrDefault("Name", _instanceId);
    Configuration cfgNode = Utils.newAwsConfiguration(name);

    List<IpAccessListLine> inboundRules = new LinkedList<>();
    List<IpAccessListLine> outboundRules = new LinkedList<>();
    for (String sGroupId : _securityGroups) {
      SecurityGroup sGroup = region.getSecurityGroups().get(sGroupId);

      if (sGroup == null) {
        throw new BatfishException(
            "Security group " + sGroupId + " for instance " + _instanceId + " not found");
      }

      sGroup.addInOutAccessLines(inboundRules, outboundRules);
    }

    // create ACLs from inboundRules and outboundRules
    _inAcl = new IpAccessList(sgIngressAclName, inboundRules);
    _outAcl = new IpAccessList(sgEgressAclName, outboundRules);
    cfgNode.getIpAccessLists().put(sgIngressAclName, _inAcl);
    cfgNode.getIpAccessLists().put(sgEgressAclName, _outAcl);

    for (String interfaceId : _networkInterfaces) {

      NetworkInterface netInterface = region.getNetworkInterfaces().get(interfaceId);

      if (netInterface == null) {
        throw new BatfishException(
            "Network interface " + interfaceId + " for instance " + _instanceId + " not found");
      }

      ImmutableSortedSet.Builder<Prefix> ifacePrefixesBuilder =
          new ImmutableSortedSet.Builder<>(Comparator.naturalOrder());

      Subnet subnet = region.getSubnets().get(netInterface.getSubnetId());
      Prefix ifaceSubnet = subnet.getCidrBlock();
      Ip defaultGatewayAddress = subnet.computeInstancesIfaceAddress();
      StaticRoute defaultRoute =
          StaticRoute.builder()
              .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
              .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
              .setNextHopIp(defaultGatewayAddress)
              .setNetwork(Prefix.ZERO)
              .build();
      cfgNode.getDefaultVrf().getStaticRoutes().add(defaultRoute);

      for (Ip ip : netInterface.getIpAddressAssociations().keySet()) {
        if (!ifaceSubnet.contains(ip)) {
          throw new BatfishException(
              "Instance subnet: " + ifaceSubnet + " does not contain private ip: " + ip);
        }
        if (ip.equals(ifaceSubnet.getEndAddress())) {
          throw new BatfishException(
              "Expected end address: " + ip + " to be used by generated subnet node");
        }
        Prefix prefix = new Prefix(ip, ifaceSubnet.getPrefixLength());
        ifacePrefixesBuilder.add(prefix);
      }
      SortedSet<Prefix> ifacePrefixes = ifacePrefixesBuilder.build();
      Interface iface = Utils.newInterface(interfaceId, cfgNode, ifacePrefixes.first());
      iface.setAllAddresses(ifacePrefixes);

      // apply ACLs to interface
      iface.setIncomingFilter(_inAcl);
      iface.setOutgoingFilter(_outAcl);

      cfgNode.getVendorFamily().getAws().setVpcId(_vpcId);
      cfgNode.getVendorFamily().getAws().setSubnetId(_subnetId);
      cfgNode.getVendorFamily().getAws().setRegion(region.getName());
    }

    return cfgNode;
  }
}
