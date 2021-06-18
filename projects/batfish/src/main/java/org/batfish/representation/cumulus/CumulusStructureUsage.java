package org.batfish.representation.cumulus;

import javax.annotation.Nonnull;
import org.batfish.vendor.StructureUsage;

public enum CumulusStructureUsage implements StructureUsage {
  BGP_ADDRESS_FAMILY_IPV4_IMPORT_VRF("bgp address-family ipv4 unicast import vrf"),
  BGP_ADDRESS_FAMILY_IPV6_IMPORT_VRF("bgp address-family ipv6 unicast import vrf"),
  BGP_ADDRESS_FAMILY_L2VPN_ADVERTISE_IPV4_UNICAST(
      "bgp address-family l2vpn advertise ipv4 unicast"),
  BGP_ADDRESS_FAMILY_L2VPN_ADVERTISE_IPV6_UNICAST(
      "bgp address-family l2vpn advertise ipv6 unicast"),
  BGP_AGGREGATE_ADDRESS_ROUTE_MAP("bgp address-family aggregate-address route-map"),
  BGP_AGGREGATE_ADDRESS_SUPPRESS_MAP("bgp address-family aggregate-address suppress-map"),
  BGP_IPV4_UNICAST_REDISTRIBUTE_CONNECTED_ROUTE_MAP(
      "bgp ipv4 unicast redistribute connected route-map"),
  BGP_IPV4_UNICAST_REDISTRIBUTE_STATIC_ROUTE_MAP("bgp ipv4 unicast redistribute static route-map"),
  BGP_IPV4_UNICAST_REDISTRIBUTE_OSPF_ROUTE_MAP("bgp ipv4 unicast redistribute ospf route-map"),
  BGP_IPV4_UNICAST_NEIGHBOR_ROUTE_MAP_IN("bgp ipv4 unicast neighbor route-map in"),
  BGP_IPV4_UNICAST_NEIGHBOR_ROUTE_MAP_OUT("bgp ipv4 unicast neighbor route-map out"),
  BGP_L2VPN_EVPN_NEIGHBOR_ROUTE_MAP_IN("bgp l2vpn evpn neighbor route-map in"),
  BGP_L2VPN_EVPN_NEIGHBOR_ROUTE_MAP_OUT("bgp l2vpn evpn neighbor route-map out"),
  BGP_NEIGHBOR_INTERFACE("bgp neighbor interface"),
  BGP_NETWORK("bgp network"),
  BGP_VRF("bgp vrf"),
  BOND_SELF_REFERENCE("bond self-reference"),
  BOND_SLAVE("bond slave"),
  BOND_VRF("bond vrf"),
  BRIDGE_PORT("bridge ports"),
  INTERFACE_CLAG_BACKUP_IP_VRF("interface clag backup-ip vrf"),
  INTERFACE_SELF_REFERENCE("interface self-reference"),
  INTERFACE_VRF("interface vrf"),
  LOOPBACK_SELF_REFERENCE("loopback self-reference"),
  NET_ADD_INTERFACE("net add interface"),
  OSPF_REDISTRIBUTE_BGP_ROUTE_MAP("ospf redistribute bgp route-map"),
  OSPF_REDISTRIBUTE_CONNECTED_ROUTE_MAP("ospf redistribute connected route-map"),
  OSPF_REDISTRIBUTE_STATIC_ROUTE_MAP("ospf redistribute connected route-map"),
  PORT_BREAKOUT("port breakout"),
  PORT_DISABLED("port disabled"),
  PORT_SPEED("port speed"),
  STATIC_ROUTE_VRF("static route vrf"),
  ROUTE_MAP_CALL("route map call"),
  ROUTE_MAP_MATCH_COMMUNITY_LIST("route-map match community"),
  ROUTE_MAP_MATCH_INTERFACE("route-map match interface"),
  ROUTE_MAP_MATCH_AS_PATH("route-map match as-path"),
  ROUTE_MAP_MATCH_IP_ADDRESS_PREFIX_LIST("route-map match ip prefix-list"),
  ROUTE_MAP_SET_COMM_LIST_DELETE("route-map set comm-list delete"),
  VLAN_SELF_REFERENCE("vlan self-reference"),
  VLAN_VRF("vlan vrf"),
  VRF_SELF_REFERENCE("vrf self-reference"),
  VXLAN_SELF_REFERENCE("vxlan self-reference");

  private final @Nonnull String _description;

  private CumulusStructureUsage(String description) {
    _description = description;
  }

  @Override
  public @Nonnull String getDescription() {
    return _description;
  }
}
