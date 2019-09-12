parser grammar Arista_bgp;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

eos_router_bgp
:
  ROUTER BGP asn = bgp_asn NEWLINE
  (
    eos_rb_address_family
    | eos_rb_inner
    | eos_rb_vlan
    | eos_rb_vlan_aware_bundle
    | eos_rb_vrf
  )*
;

eos_rb_address_family
:
  ADDRESS_FAMILY
  (
    eos_rb_af_ipv4
//  | eos_rb_af_ipv6
    | eos_rb_af_evpn
//  | eos_rb_af_vpn_v4
//  | eos_rb_af_vpn_v6
  )
;

eos_rb_af_ipv4
:
  IPV4
  (
    eos_rb_af_ipv4_unicast
//  | eos_rb_af_ipv4_multicast
//  | eos_rb_af_ipv4_labeled_multicast
//  | eos_rb_af_ipv4_sr_te
  )
;

eos_rb_af_ipv4_unicast
:
  NEWLINE
  (
    eos_rbafipv4u_bgp
//    | eos_rbafipv4u_graceful_restart
    | eos_rbafipv4u_neighbor
    | eos_rbafipv4_no
//    | eos_rbafipv4u_network
//    | eos_rbafipv4u_redistribute
  )*
;

eos_rbafipv4u_bgp
:
  BGP
  null_rest_of_line
//  (
//    eos_rbafipv4b_additional_paths
//    | eos_rbafipv4b_next_hop
//    | eos_rbafipv4b_redistribute_internal
//    | eos_rbafipv4b_route
//  )
;

eos_rbafipv4u_neighbor
:
  NEIGHBOR
  (
    v4 = IP_ADDRESS
    | v6 = IPV6_ADDRESS
    | pg = VARIABLE
  )
  eos_rb_af_neighbor_common
;

eos_rbafipv4_no
:
  NO
  (
    eos_rbafipv4_no_neighbor
  )
;

eos_rbafipv4_no_neighbor
:
  NEIGHBOR
  (
    v4 = IP_ADDRESS
    | v6 = IPV6_ADDRESS
    | pg = VARIABLE
  )
  eos_rb_af_no_neighbor_common
;

eos_rb_af_evpn
:
  EVPN NEWLINE
  (
    eos_rb_af_evpn_bgp
//    | eos_rb_af_evpn_graceful_restart
//    | eos_rb_af_evpn_host_flap
    | eos_rb_af_evpn_neighbor
    | eos_rb_af_evpn_no
  )*
;

eos_rb_af_evpn_bgp
:
  BGP
  (
    eos_rbafeb_additional_paths
    | eos_rbafeb_next_hop_unchanged
  )
;

eos_rb_af_evpn_neighbor
:
  NEIGHBOR
  (
    v4 = IP_ADDRESS
    | v6 = IPV6_ADDRESS
    | pg = VARIABLE
  )
  eos_rb_af_neighbor_common
;

eos_rb_af_evpn_no:
  NO
  eos_rb_af_evpn_no_neighbor
;

eos_rb_af_evpn_no_neighbor
:
  NEIGHBOR
  (
    v4 = IP_ADDRESS
    | v6 = IPV6_ADDRESS
    | pg = VARIABLE
  )
  eos_rb_af_no_neighbor_common
;

eos_rb_af_neighbor_common
:
  (
    eos_rbafnc_activate
//    | eos_rbafnc_additional_paths
//    | eos_rbafnc_graceful_restart
//    | eos_rbafnc_next_hop_unchanged
//    | oes_rbafnc_route_map
//    | oes_rbafnc_weight
  )
;

eos_rbafeb_additional_paths
:
  ADDITIONAL_PATHS (SEND ANY | RECEIVE) NEWLINE
;

eos_rbafeb_next_hop_unchanged
:
  NEXT_HOP_UNCHANGED NEWLINE
;

eos_rbafnc_activate
:
  ACTIVATE NEWLINE
;

eos_rb_af_no_neighbor_common
:
  eos_rbafnonc_activate
;

eos_rbafnonc_activate
:
  ACTIVATE NEWLINE
;

eos_rb_inner
:
  eos_rbi_aggregate_address
  | eos_rbi_default_metric
  | eos_rbi_distance
  | eos_rbi_neighbor
  | eos_rbi_network
  | eos_rbi_no
  | eos_rbi_redistribute
  | eos_rbi_router_id
  | eos_rbi_shutdown
  | eos_rbi_timers
  | eos_rbi_ucmp
  | eos_rbi_update
;

eos_rbi_aggregate_address
:
  AGGREGATE_ADDRESS
  (
    eos_rb_aa_v4
    | eos_rb_aa_v6
  )
;

eos_rbi_default_metric
:
  DEFAULT_METRIC metric = DEC NEWLINE
;

eos_rbi_distance
:
  DISTANCE external = DEC (internal = DEC local = DEC)? NEWLINE
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
    | eos_rbinc_maximum_accepted_routes
    | eos_rbinc_maximum_routes
//    | eos_rbinc_metric_out
//    | eos_rbinc_monitoring
//    | eos_rbinc_next_hop_peer
    | eos_rbinc_next_hop_self
//    | eos_rbinc_next_hop_unchanged
//    | eos_rbinc_out_delay
    | eos_rbinc_password
//    | eos_rbinc_prefix_list
    | eos_rbinc_remote_as
//    | eos_rbinc_remove_private_as
//    | eos_rbinc_route_map
//    | eos_rbinc_route_reflector_client
//    | eos_rbinc_route_to_peer
    | eos_rbinc_send_community
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

eos_rbinc_maximum_accepted_routes
:
  MAXIMUM_ACCEPTED_ROUTES num = DEC (WARNING_LIMIT warn_limit = DEC)? NEWLINE
;

eos_rbinc_maximum_routes
:
  MAXIMUM_ROUTES num = DEC
  (
    WARNING_ONLY
    | (WARNING_LIMIT warn_limit = DEC PERCENT_LITERAL?)
  )*
  NEWLINE
;

eos_rbinc_next_hop_self
:
  NEXT_HOP_SELF NEWLINE
;

eos_rbinc_password
:
  PASSWORD (encrypt_level = DEC)? VARIABLE NEWLINE
;

eos_rbinc_remote_as
:
  REMOTE_AS asn = bgp_asn NEWLINE
;

eos_rbinc_send_community
:
  SEND_COMMUNITY
  (ADD | REMOVE)?
  (
    EXTENDED
    | STANDARD
//    TODO: support for link-bandwidth
//    | LINK_BANDWIDTH
//      (
//        AGGREGATE "0.0-4294967295.0 or nn.nn(K|M|G)  Reference link speed in bits/second"
//        | DIVIDE (EQUAL | RATIO)
//      )
  )*
  NEWLINE
;

// Assigning a peer group to a neighbor
eos_rbin_peer_group
:
  PEER_GROUP name = VARIABLE NEWLINE
;

eos_rbi_network
:
  NETWORK
  (
    eos_rbi_network4
    | eos_rbi_network6
  )
;

eos_rbi_network4
:
  (
    ip = IP_ADDRESS mask = IP_ADDRESS
    | prefix = IP_PREFIX
  )
  (ROUTE_MAP rm = VARIABLE)?
  NEWLINE
;

eos_rbi_network6
:
  IPV6_PREFIX (ROUTE_MAP rm = VARIABLE)? NEWLINE
;

eos_rbi_no
:
  NO
  eos_rbino_bgp
;

eos_rbino_bgp
:
  BGP
  eos_rbino_bgp_default
;

eos_rbino_bgp_default
:
  DEFAULT
  eos_rbino_bgp_default_ipv4_unicast
;

eos_rbino_bgp_default_ipv4_unicast
:
  IPV4_UNICAST NEWLINE
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

eos_rbi_redistribute
:
  REDISTRIBUTE
  (
    eos_rbir_attached_host
    | eos_rbir_connected
    | eos_rbir_dynamic
    | eos_rbir_isis
    | eos_rbir_ospf
    | eos_rbir_ospf3
    | eos_rbir_rip
    | eos_rbir_static
  )
;

eos_rbir_attached_host
:
  ATTACHED_HOST (ROUTE_MAP rm = VARIABLE)? NEWLINE
;

eos_rbir_connected
:
  CONNECTED (ROUTE_MAP rm = VARIABLE)? NEWLINE
;

eos_rbir_dynamic
:
  DYNAMIC (ROUTE_MAP rm = VARIABLE)? NEWLINE
;

eos_rbir_isis
:
  ISIS
  (LEVEL_1 | LEVEL_2 | LEVEL_1_2)?
  (ROUTE_MAP rm = VARIABLE)?
  NEWLINE
;

eos_rbir_ospf
:
  OSPF
  (
    MATCH
    (
      INTERNAL
      | EXTERNAL
      | (NSSA_EXTERNAL DEC?)
    )
  )?
  (ROUTE_MAP rm = VARIABLE)?
  NEWLINE
;

eos_rbir_ospf3
:
  OSPF3
  (
    MATCH
    (
      INTERNAL
      | EXTERNAL
      | (NSSA_EXTERNAL DEC?)
    )
  )?
  (ROUTE_MAP rm = VARIABLE)?
  NEWLINE
;

eos_rbir_rip
:
  RIP (ROUTE_MAP rm = VARIABLE)? NEWLINE
;

eos_rbir_static
:
  STATIC (ROUTE_MAP rm = VARIABLE)? NEWLINE
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

eos_rbi_ucmp
:
  UCMP
  (
    eos_rbiu_fec
    | eos_rbiu_link_bandwidth
    | eos_rbiu_mode
  )
;

eos_rbiu_fec
:
  FEC THRESHOLD TRIGGER trigger = DEC CLEAR clear = DEC WARNING_ONLY NEWLINE
;

eos_rbiu_link_bandwidth
:
  LINK_BANDWIDTH (ENCODING_WEIGHTED | RECURSIVE | UPDATE_DELAY DEC) NEWLINE
;

eos_rbiu_mode
:
  MODE mode_num = DEC
  (
    next_hops = DEC
    (oversubscription = FLOAT)?
  )? NEWLINE
;

eos_rbi_update
:
  UPDATE
  (
    WAIT_FOR_CONVERGENCE
    | WAIT_INSTALL (BATCH_SIZE DEC)?
  ) NEWLINE
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

eos_rbv_address_family
:
  eos_rb_af_ipv4
//  | eos_rb_af_ipv4_multicast
//  | eos_rb_af_ipv6
;

eos_rbv_local_as
:
  LOCAL_AS asn = bgp_asn NEWLINE
;

eos_rbv_rd
:
  RD rd = route_distinguisher NEWLINE
;

eos_rbv_route_target
:
  ROUTE_TARGET
  ( IMPORT | EXPORT )
  rt = route_target NEWLINE
;

eos_rb_vrf
:
  VRF name = VARIABLE NEWLINE
  (
    eos_rbv_address_family
    | eos_rb_inner
    | eos_rbv_local_as
    | eos_rbv_rd
    | eos_rbv_route_target
  )*
;