parser grammar CumulusNclu_bgp;

import CumulusNclu_common;

options {
  tokenVocab = CumulusNcluLexer;
}

a_bgp
:
  BGP
  (
    b_common
    | b_vrf
  )
;

b_common
:
  b_autonomous_system
  | b_ipv4_unicast
  | b_l2vpn
  | b_neighbor
  | b_router_id
;

b_autonomous_system
:
  AUTONOMOUS_SYSTEM as = uint32 NEWLINE
;

b_ipv4_unicast
:
  IPV4 UNICAST
  (
    bi4_network
    | bi4_redistribute_connected
    | bi4_redistribute_static
  )
;

bi4_network
:
  NETWORK network = ip_prefix NEWLINE
;

bi4_neighbor
:
  NEIGHBOR name = word
  (
    bi4n_activate
    | bi4n_route_reflector_client
  )
;

bi4n_activate
:
  ACTIVATE NEWLINE
;

bi4n_route_reflector_client
:
  ROUTE_REFLECTOR_CLIENT NEWLINE
;

bi4_redistribute_connected
:
  REDISTRIBUTE CONNECTED
  (
    ROUTE_MAP rm = word
  )? NEWLINE
;

bi4_redistribute_static
:
  REDISTRIBUTE STATIC
  (
    ROUTE_MAP rm = word
  )? NEWLINE
;

b_l2vpn
:
  L2VPN EVPN
  (
    ble_advertise_all_vni
    | ble_advertise_default_gw
    | ble_advertise_ipv4_unicast
    | ble_neighbor
  )
;

ble_advertise_all_vni
:
  ADVERTISE_ALL_VNI NEWLINE
;

ble_advertise_default_gw
:
  ADVERTISE_DEFAULT_GW NEWLINE
;

ble_advertise_ipv4_unicast
:
  ADVERTISE IPV4 UNICAST NEWLINE
;

ble_neighbor
:
  NEIGHBOR name = word blen_activate
;

blen_activate
:
  ACTIVATE NEWLINE
;

blen_route_reflector_client
:
  ROUTE_REFLECTOR_CLIENT NEWLINE
;


/*
 * This is a mess. The Cumulus grammar is ambiguous - you can't quite tell what grammar is legal
 * without knowing the type of the peer and without looking several tokens down the line.
 *
 * bgp neighbor foo peer-group;        // creates a peer-group named foo.
 * bgp neighbor 1.2.3.4 peer-group;    // creates a peer-group named 1.2.3.4. (yes you can do this).
 * bgp neighbor 2.3.4.5 peer-group bar;    // the neighbor 2.3.4.5 is in peer-group bar.
 *
 * interface peers always appear as [swp1 interface] instead of [foo] or [IP].
 */
b_neighbor
:
  NEIGHBOR
  (
    name = word bn_peer_group   // initial declaration for a peer-group
    | name = word bn_interface  // declare and/or configure an interface neighbor
    | name = word bn_peer       // declare and/or configure an IP neighbor, or configure a peer-group
  )
;

bn_interface
:
  INTERFACE
  (
    | bnp_peer_group
    | bnp_remote_as
  )
;

bn_peer
:
  bnp_description
  | bnp_peer_group
  | bnp_remote_as
;

bn_peer_group
:
  PEER_GROUP NEWLINE
;

bnp_description
:
  DESCRIPTION text = ~NEWLINE* NEWLINE
;

bnp_peer_group
:
  PEER_GROUP name = word NEWLINE
;

bnp_remote_as
:
  REMOTE_AS
  (
     EXTERNAL
     | INTERNAL
     | as = uint32
  ) NEWLINE
;

b_router_id
:
  ROUTER_ID id = ip_address NEWLINE
;

b_vrf
:
  VRF name = word b_common
;
