parser grammar CiscoNxos_bgp;

import CiscoNxos_common;

options {
  tokenVocab = CiscoNxosLexer;
}

////////////////////////////
///// VARIOUS SUBTYPES /////
////////////////////////////

bgp_distance
:
// 1-255
  UINT8
;

dampen_igp_metric_interval
:
// 20-3600
  UINT8
  | UINT16
;

decay_half_life
:
// 1-45
  UINT8
;

maximum_paths
:
// 1-64
  UINT8
;

start_reuse_val
:
// 1-20000
  UINT8
  | UINT16
;

start_suppress_val
:
// 1-20000
  UINT8
  | UINT16
;

/////////////////////////////
///// Actual BGP config /////
/////////////////////////////

router_bgp
:
  BGP asn = bgp_asn NEWLINE
  (
    rb_address_family
    | rb_enforce_first_as
    | rb_event_history
    | rb_fast_external_fallover
    | rb_flush_routes
    | rb_isolate
    | rb_no_enforce_first_as
    | rb_proc_vrf_common
    | rb_shutdown
    | rb_template
    | rb_vrf
  )*
;

rb_proc_vrf_common
:
  rb_bestpath
  | rb_cluster_id
  | rb_confederation
  | rb_graceful_restart
  | rb_graceful_restart_helper
  | rb_log_neighbor_changes
  | rb_maxas_limit
  | rb_neighbor
  | rb_reconnect_interval
  | rb_router_id
  | rb_suppress_fib_pending
  | rb_timers
;

rb_address_family
:
  ADDRESS_FAMILY
  (
    rb_af_ipv4
    | rb_af_ipv6
    | rb_af_l2vpn
    | rb_af_link_state
    | rb_af_vpnv4
    | rb_af_vpnv6
  )
;

rb_af_ipv4
:
  IPV4
  (
    rb_af_ipv4_multicast
    | rb_af_ipv4_mvpn
    | rb_af_ipv4_unicast
  )
;

rb_af_ipv4_multicast
:
  MULTICAST NEWLINE
  rb_af4_inner*
;

rb_af_ipv4_mvpn
:
  MVPN NEWLINE
  // todo
;

rb_af_ipv4_unicast
:
  UNICAST NEWLINE
  rb_af4_inner*
;

rb_af_ipv6
:
  IPV6
  (
    rb_af_ipv6_multicast
    | rb_af_ipv6_mvpn
    | rb_af_ipv6_unicast
  )
;

rb_af_ipv6_multicast
:
  MULTICAST NEWLINE
  rb_af6_inner*
;

rb_af_ipv6_mvpn
:
  MVPN NEWLINE
  // todo
;

rb_af_ipv6_unicast
:
  UNICAST NEWLINE
  rb_af6_inner*
;

rb_af_l2vpn
:
  L2VPN EVPN NEWLINE
  (
    rb_afl2v_retain
  )*
;

rb_afl2v_retain
:
  RETAIN ROUTE_TARGET
  (
    ALL
    | ROUTE_MAP map = route_map_name
  ) NEWLINE
;

rb_af_link_state
:
  LINK_STATE NEWLINE
  // todo
;

rb_af_vpnv4
:
  VPNV4 UNICAST NEWLINE
  // todo
;

rb_af_vpnv6
:
  VPNV6 UNICAST NEWLINE
  // todo
;

// Common to ipv4 unicast and ipv4 multicast
rb_af4_inner
:
  // IPv4 ONLY
  rb_af4_aggregate_address
  | rb_af4_network
  | rb_af4_redistribute
  // IPv4 or IPv6
  | rb_afip_common
;

rb_af4_aggregate_address
:
  AGGREGATE_ADDRESS network = route_network rb_afip_aa_tail* NEWLINE
;

rb_af4_network
:
  NETWORK network = route_network (ROUTE_MAP mapname = route_map_name)? NEWLINE
;

rb_af4_redistribute
:
  REDISTRIBUTE routing_instance_v4 ROUTE_MAP mapname = route_map_name NEWLINE
;

// Common to ipv6 unicast and ipv6 multicast
rb_af6_inner
:
  // IPv6 ONLY
  rb_af6_aggregate_address
  | rb_af6_network
  | rb_af6_redistribute
  // IPv4 or IPv6
  | rb_afip_common
;

rb_af6_aggregate_address
:
  AGGREGATE_ADDRESS network = ipv6_prefix rb_afip_aa_tail* NEWLINE
;

rb_af6_network
:
  NETWORK network = ipv6_prefix (ROUTE_MAP mapname = route_map_name)? NEWLINE
;

rb_af6_redistribute
:
  REDISTRIBUTE routing_instance_v6 ROUTE_MAP mapname = route_map_name NEWLINE
;

// Common to IPv4 or IPv6, unicast or multicast
rb_afip_common
:
  rb_afip_additional_paths
  | rb_afip_advertise
  | rb_afip_client_to_client
  | rb_afip_dampen_igp_metric
  | rb_afip_dampening
  | rb_afip_default_information
  | rb_afip_default_metric
  | rb_afip_distance
  | rb_afip_inject_map
  | rb_afip_maximum_paths
  | rb_afip_nexthop
  | rb_afip_no_default_originate
  | rb_afip_suppress_inactive
  | rb_afip_table_map
  | rb_afip_wait_igp_convergence
;

rb_afip_additional_paths
:
  ADDITIONAL_PATHS
  (
    INSTALL BACKUP
    | RECEIVE
    | SELECTION ROUTE_MAP mapname = route_map_name
    | SEND
  ) NEWLINE
;

rb_afip_advertise
:
  ADVERTISE L2VPN EVPN NEWLINE
;

rb_afip_aa_tail
:
  ADVERTISE_MAP mapname = route_map_name
  | AS_SET
  | ATTRIBUTE_MAP mapname = route_map_name
  | SUMMARY_ONLY
  | SUPPRESS_MAP mapname = route_map_name
;

rb_afip_client_to_client
:
  CLIENT_TO_CLIENT REFLECTION NEWLINE
;

rb_afip_dampen_igp_metric
:
  DAMPEN_IGP_METRIC interval_secs = dampen_igp_metric_interval NEWLINE
;

rb_afip_dampening
:
  DAMPENING
  (
    half_life = decay_half_life start_reuse = start_reuse_val start_suppress =
    start_suppress_val max_suppress = uint8
    | ROUTE_MAP mapname = route_map_name
  ) NEWLINE
;

rb_afip_default_information
:
  DEFAULT_INFORMATION ORIGINATE NEWLINE
;

rb_afip_default_metric
:
  DEFAULT_METRIC metric = uint32 NEWLINE
;

rb_afip_distance
:
  DISTANCE ebgp = bgp_distance ibgp = bgp_distance local = bgp_distance NEWLINE
;

rb_afip_inject_map
:
  INJECT_MAP injectmap = route_map_name EXIST_MAP existmap = route_map_name
  COPY_ATTRIBUTES? NEWLINE
;

rb_afip_maximum_paths
:
  MAXIMUM_PATHS
  (
    EIBGP
    | IBGP
  )? numpaths = maximum_paths NEWLINE
;

rb_afip_nexthop
:
  NEXTHOP
  (
    rb_afip_nexthop_route_map
    | rb_afip_nexthop_trigger_delay
  )
;

rb_afip_nexthop_route_map
:
  ROUTE_MAP mapname = route_map_name NEWLINE
;

rb_afip_nexthop_trigger_delay
:
  TRIGGER_DELAY CRITICAL critical = uint32 NON_CRITICAL noncritical = uint32 NEWLINE
;

rb_afip_no_default_originate
:
  NO DEFAULT_ORIGINATE NEWLINE
;

rb_afip_suppress_inactive
:
  SUPPRESS_INACTIVE NEWLINE
;

rb_afip_table_map
:
  TABLE_MAP mapname = route_map_name FILTER? NEWLINE
;

rb_afip_wait_igp_convergence
:
  WAIT_IGP_CONVERGENCE NEWLINE
;

rb_bestpath
:
  BESTPATH
  (
    ALWAYS_COMPARE_MED
    | AS_PATH MULTIPATH_RELAX
    | COMPARE_ROUTERID
    | COST_COMMUNITY IGNORE
    | MED
    (
      CONFED
      | MISSING_AS_WORST
      | NON_DETERMINISTIC
    )
  ) NEWLINE
;

rb_cluster_id
:
  CLUSTER_ID
  (
    ip_as_int = uint32
    | ip = ip_address
  ) NEWLINE
;

rb_confederation
:
  CONFEDERATION
  (
    rb_confederation_identifier
    | rb_confederation_peers
  )
;

rb_confederation_identifier
:
  IDENTIFIER asn = bgp_asn NEWLINE
;

rb_confederation_peers
:
  PEERS asns += bgp_asn NEWLINE asns += bgp_asn+ NEWLINE
;

rb_enforce_first_as
:
  ENFORCE_FIRST_AS NEWLINE
;

rb_event_history
:
  EVENT_HISTORY
  (
    CLI
    | DETAIL
    | ERRORS
    | EVENTS
    | OBJSTORE
    | PERIODIC
  )
  (
    SIZE
    (
      DISABLE
      | LARGE
      | MEDIUM
      | SMALL
      | sizebytes = event_history_buffer_size
    )
  )? NEWLINE
;

event_history_buffer_size
:
// 32768-8388608
  UINT8
  | UINT16
  | UINT32
;

rb_fast_external_fallover
:
  FAST_EXTERNAL_FALLOVER NEWLINE
;

rb_flush_routes
:
  FLUSH_ROUTES NEWLINE
;

rb_graceful_restart
:
  GRACEFUL_RESTART
  (
    RESTART_TIME restart_secs = restart_time
    | STALEPATH_TIME stalepath_secs = stalepath_time
  )? NEWLINE
;

restart_time
:
// 1-3600
  UINT8
  | UINT16
;

stalepath_time
:
// 1-3600
  UINT8
  | UINT16
;

rb_graceful_restart_helper
:
  GRACEFUL_RESTART_HELPER NEWLINE
;

rb_log_neighbor_changes
:
  LOG_NEIGHBOR_CHANGES NEWLINE
;

rb_isolate
:
  ISOLATE NEWLINE
;

rb_maxas_limit
:
  MAXAS_LIMIT limit = maxas_limit NEWLINE
;

maxas_limit
:
// 1-512
  UINT8
  | UINT16
;

rb_neighbor
:
  NEIGHBOR
  (
    ip = ip_address
    | prefix = ip_prefix
    | ip6 = ipv6_address
    | prefix6 = ipv6_prefix
  )
  (
    REMOTE_AS
    (
      asn = bgp_asn?
      | ROUTE_MAP mapname = route_map_name
    )
  )? NEWLINE rb_n_inner*
;

// We might get to this level of the hierarchy in four ways:
//  1. configuring an actual neighbor (router bgp ; (optional vrf) ; neighbor)
//  2. configuring a template peer (router bgp ; template peer)
//  3. configuring a template peer-session (router bgp ; template peer-session)
//  4. inside of DEFAULT command in any of the above 3 levels.
//     For now, DEFAULT is unhandled.
//
// We use a few different rules to distinguish the rest.
//  rb_n_common are commands valid at any of the levels 1-3.
//  rnbx_n_address_family is only valid at levels 1 and 2.

rb_n_inner
:
  rb_n_common
  | rb_n_address_family
;

rb_n_common
:
  rb_n_bfd
  | rb_n_capability
  | rb_n_default
  | rb_n_description
  | rb_n_disable_connected_check
  | rb_n_dont_capability_negotiate
  | rb_n_dynamic_capability
  | rb_n_ebgp_multihop
  | rb_n_inherit
  | rb_n_local_as
  | rb_n_low_memory
  | rb_n_maximum_peers
  | rb_n_no
  | rb_n_password
  | rb_n_remote_as
  | rb_n_remove_private_as
  | rb_n_shutdown
  | rb_n_timers
  | rb_n_transport
  | rb_n_update_source
;

rb_n_address_family
:
  ADDRESS_FAMILY first =
  (
    IPV4
    | IPV6
    | L2VPN
  ) second = (EVPN | MULTICAST | UNICAST) NEWLINE rb_n_af_inner*
;

// We might get to this level of the hierarchy in four ways:
//  1. configuring an actual neighbor (router bgp ; (optional vrf) ; neighbor ; address-family)
//  2. configuring a template peer (router bgp ; template peer ; address-family)
//  3. configuring a template peer-policy (router bgp ; template peer-policy)
//  4. inside of DEFAULT command in any of the above 3 levels.
//     For now, DEFAULT is unhandled.
//
// All the remaining commands are valid at all levels 1-3.

rb_n_af_inner
:
  rb_n_af_advertise_map
  | rb_n_af_allowas_in
  | rb_n_af_as_override
  | rb_n_af_capability
  | rb_n_af_default
  | rb_n_af_default_originate
  | rb_n_af_disable_peer_as_check
  | rb_n_af_filter_list
  | rb_n_af_inherit
  | rb_n_af_maximum_prefix
  | rb_n_af_next_hop_self
  | rb_n_af_next_hop_third_party
  | rb_n_af_prefix_list
  | rb_n_af_route_map
  | rb_n_af_route_reflector_client
  | rb_n_af_send_community
  | rb_n_af_soft_reconfiguration
  | rb_n_af_soo
  | rb_n_af_suppress_inactive
  | rb_n_af_unsuppress_map
  | rb_n_af_weight
;

rb_n_af_advertise_map
:
  ADVERTISE_MAP mapname = route_map_name
  (
    EXIST_MAP existmap = route_map_name
    | NON_EXIST_MAP nonexistmap = route_map_name
  ) NEWLINE
;

rb_n_af_allowas_in
:
  ALLOWAS_IN
  (
    num = allowas_in_max_occurrences
  )? NEWLINE
;

allowas_in_max_occurrences
:
// 1-10
  UINT8
;

rb_n_af_as_override
:
  AS_OVERRIDE NEWLINE
;

rb_n_af_capability
:
  CAPABILITY ADDITIONAL_PATHS
  (
    RECEIVE
    | SEND
  ) DISABLE? NEWLINE
;

rb_n_af_default
:
  DEFAULT null_rest_of_line
;

rb_n_af_default_originate
:
  DEFAULT_ORIGINATE
  (
    ROUTE_MAP mapname = route_map_name
  )? NEWLINE
;

rb_n_af_disable_peer_as_check
:
  DISABLE_PEER_AS_CHECK NEWLINE
;

rb_n_af_filter_list
:
  FILTER_LIST name = ip_as_path_access_list_name
  (
    IN
    | OUT
  ) NEWLINE
;

rb_n_af_inherit
:
  INHERIT PEER_POLICY template = template_name seq = inherit_sequence_number
  NEWLINE
;

inherit_sequence_number
:
// 1-65535
  UINT8
  | UINT16
;

rb_n_af_maximum_prefix
:
  MAXIMUM_PREFIX limit = uint32
  (
    threshold_pct = threshold_percentage
  )?
  (
    RESTART interval_min = restart_interval
    | WARNING_ONLY
  )? NEWLINE
;

threshold_percentage
:
// 1-100
  UINT8
;

restart_interval
:
// 1-65535
  UINT8
  | UINT16
;

rb_n_af_next_hop_self
:
  NEXT_HOP_SELF NEWLINE
;

rb_n_af_next_hop_third_party
:
  NEXT_HOP_THIRD_PARTY NEWLINE
;

rb_n_af_prefix_list
:
  PREFIX_LIST listname = ip_prefix_list_name
  (
    IN
    | OUT
  ) NEWLINE
;

rb_n_af_route_map
:
  ROUTE_MAP mapname = route_map_name
  (
    IN
    | OUT
  ) NEWLINE
;

rb_n_af_route_reflector_client
:
  ROUTE_REFLECTOR_CLIENT NEWLINE
;

rb_n_af_send_community
:
  SEND_COMMUNITY
  (
    BOTH
    | STANDARD
    | EXTENDED
  )? NEWLINE
;

rb_n_af_soft_reconfiguration
:
  SOFT_RECONFIGURATION INBOUND ALWAYS? NEWLINE
;

rb_n_af_soo
:
  SOO
  (
    asn = bgp_asn
    | ip = ip_address
  ) COLON community_id = uint32 NEWLINE
;

rb_n_af_suppress_inactive
:
  SUPPRESS_INACTIVE
;

rb_n_af_unsuppress_map
:
  UNSUPPRESS_MAP mapname = route_map_name NEWLINE
;

rb_n_af_weight
:
  WEIGHT weight = uint16 NEWLINE
;

rb_n_bfd
:
  BFD NEWLINE
;

rb_n_capability
:
  CAPABILITY SUPPRESS FOUR_BYTE_AS NEWLINE
;

rb_n_default
:
  DEFAULT null_rest_of_line
;

rb_n_description
:
  DESCRIPTION desc = bgp_neighbor_description NEWLINE
;

bgp_neighbor_description
:
// 1-80
  desc = REMARK_TEXT
;

rb_n_disable_connected_check
:
  DISABLE_CONNECTED_CHECK NEWLINE
;

rb_n_dont_capability_negotiate
:
  DONT_CAPABILITY_NEGOTIATE NEWLINE
;

rb_n_dynamic_capability
:
  DYNAMIC_CAPABILITY NEWLINE
;

rb_n_ebgp_multihop
:
  EBGP_MULTIHOP ebgp_ttl = ebgp_multihop_ttl NEWLINE
;

ebgp_multihop_ttl
:
//2-255
  UINT8
;

rb_n_inherit
:
  INHERIT
  (
    PEER
    | PEER_SESSION
  ) peer = template_name NEWLINE
;

rb_n_local_as
:
  LOCAL_AS asn = bgp_asn
  (
    NO_PREPEND
    (
      REPLACE_AS DUAL_AS?
    )?
  )? NEWLINE
;

rb_n_low_memory
:
  LOW_MEMORY EXEMPT NEWLINE
;

rb_n_maximum_peers
:
  MAXIMUM_PEERS max = maximum_peers NEWLINE
;

maximum_peers
:
// 1-1000
  UINT8
  | UINT16
;

rb_n_no
:
  NO
  (
    rb_n_no_bfd
    | rb_n_no_shutdown
  )
;

rb_n_no_bfd
:
  BFD NEWLINE
;

rb_n_no_shutdown
:
  SHUTDOWN NEWLINE
;

rb_n_password
:
  PASSWORD pass = cisco_nxos_password NEWLINE
;

rb_n_remote_as
:
  REMOTE_AS asn = bgp_asn NEWLINE
;

rb_n_remove_private_as
:
  REMOVE_PRIVATE_AS
  (
    ALL
    | REPLACE_AS
  )? NEWLINE
;

rb_n_shutdown
:
  SHUTDOWN NEWLINE
;

rb_n_timers
:
  TIMERS keepalive_secs = keepalive_interval holdtime_secs = holdtime NEWLINE
;

keepalive_interval
:
// 0-3600
  UINT8
  | UINT16
;

holdtime
:
// 0-3600
  UINT8
  | UINT16
;

rb_n_transport
:
  TRANSPORT CONNECTION_MODE PASSIVE NEWLINE
;

rb_n_update_source
:
  UPDATE_SOURCE interface_name NEWLINE
;

rb_no_enforce_first_as
:
  NO ENFORCE_FIRST_AS NEWLINE
;

rb_reconnect_interval
:
  RECONNECT_INTERVAL secs = reconnect_interval NEWLINE
;

reconnect_interval
:
// 1-60
  UINT8
;

rb_router_id
:
  ROUTER_ID id = ip_address NEWLINE
;

rb_shutdown
:
  SHUTDOWN NEWLINE
;

rb_suppress_fib_pending
:
  SUPPRESS_FIB_PENDING NEWLINE
;

rb_template
:
  TEMPLATE
  (
    rb_template_peer
    | rb_template_peer_policy
    | rb_template_peer_session
  )
;

rb_template_peer
:
  PEER peer = template_name NEWLINE rb_n_inner*
;

rb_template_peer_policy
:
  PEER_POLICY policy = template_name NEWLINE rb_n_af_inner*
;

rb_template_peer_session
:
  PEER_SESSION session = template_name NEWLINE rb_n_common*
;

rb_timers
:
  TIMERS
  (
    rb_timers_bestpath_limit
    | rb_timers_bgp
    | rb_timers_prefix_peer_timeout
    | rb_timers_prefix_peer_wait
  )
;

rb_timers_bestpath_limit
:
  BESTPATH_LIMIT timeout_secs = bestpath_timeout ALWAYS? NEWLINE
;

bestpath_timeout
:
// 1-3600
  UINT8
  | UINT16
;

rb_timers_bgp
:
  BGP keepalive_secs = keepalive_interval holdtime_secs = holdtime
  NEWLINE
;

rb_timers_prefix_peer_timeout
:
  PREFIX_PEER_TIMEOUT timeout_secs = prefix_peer_timeout NEWLINE
;

prefix_peer_timeout
:
// 0-1200
  UINT8
  | UINT16
;

rb_timers_prefix_peer_wait
:
  PREFIX_PEER_WAIT wait_secs = prefix_peer_wait_timer NEWLINE
;

prefix_peer_wait_timer
:
// 0-1200
  UINT8
  | UINT16
;

rb_vrf
:
  VRF name = vrf_non_default_name NEWLINE
  (
    rb_proc_vrf_common
    | rb_v_address_family
    | rb_v_local_as
  )*
;

rb_v_address_family
:
  ADDRESS_FAMILY
  (
    rb_vaf_ipv4
    | rb_vaf_ipv6
  )
;

rb_vaf_ipv4
:
  IPV4
  (
    rb_af_ipv4_multicast
    | rb_af_ipv4_unicast
  )
;

rb_vaf_ipv6
:
  IPV6
  (
    rb_af_ipv6_multicast
    | rb_af_ipv6_unicast
  )
;

rb_v_local_as
:
  LOCAL_AS asn = bgp_asn NEWLINE
;
