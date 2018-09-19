package org.batfish.datamodel.questions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.answers.AutocompleteSuggestion;
import org.batfish.datamodel.answers.Schema;

/**
 * Enables specification a set of interface properties.
 *
 * <p>Currently supported example specifiers:
 *
 * <ul>
 *   <li>channel-group -&gt; gets the interface's channel groups using a configured Java function
 *   <li>channel.* gets all properties that start with 'channel'
 * </ul>
 *
 * <p>In the future, we might add other specifier types, e.g., those based on Json Path
 */
public class InterfacePropertySpecifier extends PropertySpecifier {

  public static final String ACCESS_VLAN = "Access_VLAN";
  public static final String ACTIVE = "Active";
  public static final String ADDITIONAL_ARP_IPS = "Additional_ARP_IPs";
  public static final String ALLOWED_VLANS = "Allowed_VLANs";
  public static final String ALL_PREFIXES = "All_Prefixes";
  public static final String AUTO_STATE_VLAN = "Auto_State_VLAN";
  public static final String BANDWIDTH = "Bandwidth";
  public static final String BLACKLISTED = "Blacklisted";
  public static final String CHANNEL_GROUP = "Channel_Group";
  public static final String CHANNEL_GROUP_MEMBERS = "Channel_Group_Members";
  public static final String DECLARED_NAMES = "Declared_Names";
  public static final String DESCRIPTION = "Description";
  public static final String DHCP_RELAY_ADDRESSES = "DHCP_Relay_Addresses";
  public static final String HSRP_GROUPS = "HSRP_Groups";
  public static final String HSRP_VERSION = "HSRP_Version";
  public static final String INCOMING_FILTER_NAME = "Incoming_Filter_Name";
  public static final String INTERFACE_TYPE = "Interface_Type";
  public static final String MTU = "MTU";
  public static final String NATIVE_VLAN = "Native_VLAN";
  public static final String OSPF_AREA_NAME = "OSPF_Area_Name";
  public static final String OSPF_COST = "OSPF_Cost";
  public static final String OSPF_ENABLED = "OSPF_Enabled";
  public static final String OSPF_HELLO_MULTIPLIER = "OSPF_Hello_Multiplier";
  public static final String OSPF_PASSIVE = "OSPF_Passive";
  public static final String OSPF_POINT_TO_POINT = "OSPF_Point_To_Point";
  public static final String OUTGOING_FILTER_NAME = "Outgoing_Filter_Name";
  public static final String PRIMARY_ADDRESS = "Primary_Address";
  public static final String PROXY_ARP = "Proxy_ARP";
  public static final String RIP_ENABLED = "Rip_Enabled";
  public static final String RIP_PASSIVE = "Rip_Passive";
  public static final String ROUTING_POLICY_NAME = "Routing_Policy_Name";
  public static final String SOURCE_NATS = "Source_NATs";
  public static final String SPANNING_TREE_PORTFAST = "Spanning_Tree_Portfast";
  public static final String SWITCHPORT = "Switchport";
  public static final String SWITCHPORT_MODE = "Switchport_Mode";
  public static final String SWITCHPORT_TRUNK_ENCAPSULATION = "Switchport_Trunk_Encapsulation";
  public static final String VRF = "VRF";
  public static final String VRRP_GROUPS = "VRRP_Groups";
  public static final String ZONE_NAME = "Zone_Name";

  public static Map<String, PropertyDescriptor<Interface>> JAVA_MAP =
      new ImmutableMap.Builder<String, PropertyDescriptor<Interface>>()
          .put(ACCESS_VLAN, new PropertyDescriptor<>(Interface::getAccessVlan, Schema.INTEGER))
          .put(ACTIVE, new PropertyDescriptor<>(Interface::getActive, Schema.BOOLEAN))
          .put(
              ADDITIONAL_ARP_IPS,
              new PropertyDescriptor<>(Interface::getAdditionalArpIps, Schema.list(Schema.IP)))
          .put(
              ALLOWED_VLANS,
              new PropertyDescriptor<>(Interface::getAllowedVlans, Schema.list(Schema.STRING)))
          .put(
              ALL_PREFIXES,
              new PropertyDescriptor<>(Interface::getAllAddresses, Schema.list(Schema.STRING)))
          .put(AUTO_STATE_VLAN, new PropertyDescriptor<>(Interface::getAutoState, Schema.BOOLEAN))
          .put(BANDWIDTH, new PropertyDescriptor<>(Interface::getBandwidth, Schema.DOUBLE))
          .put(BLACKLISTED, new PropertyDescriptor<>(Interface::getBlacklisted, Schema.BOOLEAN))
          .put(CHANNEL_GROUP, new PropertyDescriptor<>(Interface::getChannelGroup, Schema.STRING))
          .put(
              CHANNEL_GROUP_MEMBERS,
              new PropertyDescriptor<>(
                  Interface::getChannelGroupMembers, Schema.list(Schema.STRING)))
          .put(
              DECLARED_NAMES,
              new PropertyDescriptor<>(Interface::getDeclaredNames, Schema.list(Schema.STRING)))
          .put(DESCRIPTION, new PropertyDescriptor<>(Interface::getDescription, Schema.STRING))
          .put(
              DHCP_RELAY_ADDRESSES,
              new PropertyDescriptor<>(Interface::getDhcpRelayAddresses, Schema.list(Schema.IP)))
          .put(
              HSRP_GROUPS,
              new PropertyDescriptor<>(Interface::getHsrpGroups, Schema.set(Schema.STRING)))
          .put(HSRP_VERSION, new PropertyDescriptor<>(Interface::getHsrpVersion, Schema.STRING))
          // skip incoming filter
          .put(
              INCOMING_FILTER_NAME,
              new PropertyDescriptor<>(Interface::getIncomingFilter, Schema.STRING))
          .put(INTERFACE_TYPE, new PropertyDescriptor<>(Interface::getInterfaceType, Schema.STRING))
          .put(MTU, new PropertyDescriptor<>(Interface::getMtu, Schema.INTEGER))
          .put(NATIVE_VLAN, new PropertyDescriptor<>(Interface::getNativeVlan, Schema.INTEGER))
          // skip ospf area
          .put(OSPF_AREA_NAME, new PropertyDescriptor<>(Interface::getOspfAreaName, Schema.INTEGER))
          .put(OSPF_COST, new PropertyDescriptor<>(Interface::getOspfCost, Schema.INTEGER))
          .put(OSPF_ENABLED, new PropertyDescriptor<>(Interface::getOspfEnabled, Schema.BOOLEAN))
          .put(
              OSPF_HELLO_MULTIPLIER,
              new PropertyDescriptor<>(Interface::getOspfHelloMultiplier, Schema.INTEGER))
          .put(OSPF_PASSIVE, new PropertyDescriptor<>(Interface::getOspfPassive, Schema.BOOLEAN))
          .put(
              OSPF_POINT_TO_POINT,
              new PropertyDescriptor<>(Interface::getOspfPointToPoint, Schema.BOOLEAN))
          // skip outgoing filter
          .put(
              OUTGOING_FILTER_NAME,
              new PropertyDescriptor<>(Interface::getOutgoingFilterName, Schema.STRING))
          // skip getOwner
          .put(PRIMARY_ADDRESS, new PropertyDescriptor<>(Interface::getAddress, Schema.STRING))
          .put(PROXY_ARP, new PropertyDescriptor<>(Interface::getProxyArp, Schema.BOOLEAN))
          .put(RIP_ENABLED, new PropertyDescriptor<>(Interface::getRipEnabled, Schema.BOOLEAN))
          .put(RIP_PASSIVE, new PropertyDescriptor<>(Interface::getRipPassive, Schema.BOOLEAN))
          // skip routing policy
          .put(
              ROUTING_POLICY_NAME,
              new PropertyDescriptor<>(Interface::getRoutingPolicyName, Schema.STRING))
          .put(
              SOURCE_NATS,
              new PropertyDescriptor<>(Interface::getSourceNats, Schema.list(Schema.STRING)))
          .put(
              SPANNING_TREE_PORTFAST,
              new PropertyDescriptor<>(Interface::getSpanningTreePortfast, Schema.BOOLEAN))
          .put(SWITCHPORT, new PropertyDescriptor<>(Interface::getSwitchport, Schema.BOOLEAN))
          .put(
              SWITCHPORT_MODE,
              new PropertyDescriptor<>(Interface::getSwitchportMode, Schema.STRING))
          .put(
              SWITCHPORT_TRUNK_ENCAPSULATION,
              new PropertyDescriptor<>(Interface::getSwitchportTrunkEncapsulation, Schema.STRING))
          .put(VRF, new PropertyDescriptor<>(Interface::getVrf, Schema.STRING))
          .put(
              VRRP_GROUPS,
              new PropertyDescriptor<>(Interface::getVrrpGroups, Schema.list(Schema.INTEGER)))
          // skip zone
          .put(ZONE_NAME, new PropertyDescriptor<>(Interface::getZoneName, Schema.STRING))
          .build();

  public static final InterfacePropertySpecifier ALL = new InterfacePropertySpecifier(".*");

  private final String _expression;

  private final Pattern _pattern;

  @JsonCreator
  public InterfacePropertySpecifier(String expression) {
    _expression = expression;
    _pattern = Pattern.compile(_expression.trim(), Pattern.CASE_INSENSITIVE);
  }

  public InterfacePropertySpecifier(Collection<String> properties) {
    // quote and join
    _expression =
        properties.stream().map(String::trim).map(Pattern::quote).collect(Collectors.joining("|"));
    _pattern = Pattern.compile(_expression, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Returns a list of suggestions based on the query, based on {@link
   * PropertySpecifier#baseAutoComplete}.
   */
  public static List<AutocompleteSuggestion> autoComplete(String query) {
    return PropertySpecifier.baseAutoComplete(query, JAVA_MAP.keySet());
  }

  @Override
  public Set<String> getMatchingProperties() {
    return JAVA_MAP
        .keySet()
        .stream()
        .filter(prop -> _pattern.matcher(prop).matches())
        .collect(Collectors.toSet());
  }

  @Override
  @JsonValue
  public String toString() {
    return _expression;
  }
}
