parser grammar Arista_bgp;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

eos_router_bgp
:
  ROUTER BGP asn = bgp_asn NEWLINE
  (
    eos_rb_inner
    | eos_rb_vlan
    | eos_rb_vlan_aware_bundle
    | eos_rb_vrf
  )*
;

eos_rb_inner
:
  eos_rbi_aggregate_address
  | eos_rbi_neighbor
  | eos_rbi_router_id
  | eos_rbi_shutdown
  | eos_rbi_timers
;

eos_rbi_aggregate_address
:
  AGGREGATE_ADDRESS
  (
    eos_rb_aa_v4
    | eos_rb_aa_v6
  )
;

eos_rbi_neighbor
:
  NEIGHBOR
  (
    eos_rbi_neighbor4
    | eos_rbi_neighbor6
    // Definition of a peer group
    | eos_rbi_peer_group
  )
;

eos_rbi_neighbor4
:
  name = IP_ADDRESS
  (
    eos_rbi_neighbor_common
    //| eos_rbin_local_v6_addr
    // Assigning a peer group
    | eos_rbin_peer_group
  )
;

eos_rbi_neighbor6
:
  name = IPV6_ADDRESS
  (
    eos_rbi_neighbor_common
    //| eos_rbin_local_v4_addr
    // Assigning a peer group
    | eos_rbin_peer_group
  )
;

eos_rbi_neighbor_common
:
  (
    eos_rbinc_additional_paths
    | eos_rbinc_allowas_in
    | eos_rbinc_auto_local_addr
    | eos_rbinc_default_originate
    | eos_rbinc_description
    | eos_rbinc_dont_capability_negotiate
    | eos_rbinc_ebgp_multihop
    | eos_rbinc_enforce_first_as
    | eos_rbinc_export_localpref
    | eos_rbinc_fall_over
//    | eos_rbinc_graceful_restart
//    | eos_rbinc_graceful_restart_helper
//    | eos_rbinc_idle_restart_timer
//    | eos_rbinc_import_localpref
//    | eos_rbinc_link_bandwidth
    | eos_rbinc_local_as
//    | eos_rbinc_maximum_accepted_routes
//    | eos_rbinc_metric_out
//    | eos_rbinc_monitoring
//    | eos_rbinc_next_hop_peer
//    | eos_rbinc_next_hop_self
//    | eos_rbinc_next_hop_unchanged
//    | eos_rbinc_out_delay
//    | eos_rbinc_password
//    | eos_rbinc_prefix_list
    | eos_rbinc_remote_as
//    | eos_rbinc_remove_private_as
//    | eos_rbinc_route_map
//    | eos_rbinc_route_reflector_client
//    | eos_rbinc_route_to_peer
//    | eos_rbinc_send_community
//    | eos_rbinc_shutdown
//    | eos_rbinc_soft_reconfiguration
//    | eos_rbinc_soft_timers
//    | eos_rbinc_soft_transport
//    | eos_rbinc_soft_ttl
//    | eos_rbinc_soft_update_source
//    | eos_rbinc_soft_weight
  )
;

eos_rbinc_additional_paths
:
  ADDITIONAL_PATHS (SEND ANY | RECEIVE) NEWLINE
;

eos_rbinc_allowas_in
:
  ALLOWAS_IN (num = DEC)? NEWLINE
;

eos_rbinc_auto_local_addr
:
  AUTO_LOCAL_ADDR NEWLINE
;

eos_rbinc_default_originate
:
  DEFAULT_ORIGINATE
  (
    ALWAYS
    | ROUTE_MAP rm = VARIABLE
  )* NEWLINE
;

eos_rbinc_description
:
  DESCRIPTION (desc = ~NEWLINE) NEWLINE
;

eos_rbinc_dont_capability_negotiate
:
  DONT_CAPABILITY_NEGOTIATE NEWLINE
;

eos_rbinc_ebgp_multihop
:
  EBGP_MULTIHOP (num = DEC)? NEWLINE
;

eos_rbinc_enforce_first_as
:
  ENFORCE_FIRST_AS NEWLINE
;

eos_rbinc_export_localpref
:
  EXPORT_LOCALPREF value = DEC NEWLINE
;

eos_rbinc_fall_over
:
  FALL_OVER BFD NEWLINE
;

eos_rbinc_local_as
:
  LOCAL_AS asn = bgp_asn NEWLINE
;

eos_rbinc_remote_as
:
  REMOTE_AS asn = bgp_asn NEWLINE
;

// Assigning a peer group to a neighbor
eos_rbin_peer_group
:
  PEER_GROUP name = VARIABLE NEWLINE
;


// Defining a peer group
eos_rbi_peer_group
:
  name = VARIABLE
  (
    PEER_GROUP NEWLINE
    | eos_rbi_neighbor_common
  )
;
eos_rbi_router_id
:
  ROUTER_ID id = IP_ADDRESS NEWLINE
;

eos_rbi_shutdown
:
  SHUTDOWN NEWLINE
;

eos_rbi_timers
:
  TIMERS BGP keepalive = DEC hold = DEC NEWLINE
;

eos_rb_aa_modifiers
:
  ADVERTISE_ONLY
  | AS_SET
  | ATTRIBUTE_MAP attr_map = VARIABLE
  | MATCH_MAP match_map = VARIABLE
  | SUMMARY_ONLY
;

eos_rb_aa_v4
:
  (
    ip = IP_ADDRESS mask = IP_ADDRESS
    | prefix = IP_PREFIX
  ) eos_rb_aa_modifiers*
  NEWLINE
;

eos_rb_aa_v6
:
  prefix = IPV6_PREFIX eos_rb_aa_modifiers* NEWLINE
;

eos_rb_vlan
:
  VLAN id = DEC NEWLINE
  eos_rb_vlan_tail*
;

eos_rb_vlan_aware_bundle
:
  VLAN_AWARE_BUNDLE name = VARIABLE NEWLINE
  (
    eos_rb_vab_vlan
    | eos_rb_vlan_tail
  )*
;

eos_rb_vlan_tail_rd
:
  RD rd = route_distinguisher NEWLINE
;

eos_rb_vlan_tail_redistribute
:
  REDISTRIBUTE
  (
    HOST_ROUTE
    | LEARNED
    | ROUTER_MAC
    | STATIC
  ) NEWLINE
;

eos_rb_vlan_tail_route_target
:
  ROUTE_TARGET
  ( BOTH | IMPORT | EXPORT )
  rt = route_target NEWLINE
;

eos_rb_vab_vlan
:
  VLAN vlans = eos_vlan_id NEWLINE
;

eos_rb_vlan_tail
:
  eos_rb_vlan_tail_rd
  | eos_rb_vlan_tail_redistribute
  | eos_rb_vlan_tail_route_target
;

eos_rb_vrf
:
  VRF name = VARIABLE NEWLINE
  eos_rb_inner*
;