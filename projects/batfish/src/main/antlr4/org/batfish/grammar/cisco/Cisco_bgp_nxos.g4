parser grammar Cisco_bgp_nxos;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

bgp_asn
:
    asn = DEC
    | asn_hi = DEC PERIOD asn_lo = DEC
;

router_bgp_nxos_toplevel
:
    rbnx_enforce_first_as
    | rbnx_event_history
    | rbnx_fast_external_fallover
    | rbnx_flush_routes
    | rbnx_isolate
    | rbnx_proc_vrf_common
    | rbnx_shutdown
    | rbnx_template_peer
    | rbnx_template_peer_policy
    | rbnx_template_peer_session
    | rbnx_vrf
;

rbnx_proc_vrf_common
:
    rbnx_address_family
    | rbnx_bestpath
    | rbnx_cluster_id
    | rbnx_confederation_identifier     // TODO
    | rbnx_confederation_peers          // TODO
    | rbnx_graceful_restart             // TODO
    | rbnx_graceful_restart_helper      // TODO
    | rbnx_log_neighbor_changes
    | rbnx_maxas_limit
    | rbnx_neighbor
    | rbnx_reconnect_interval
    | rbnx_router_id
    | rbnx_suppress_fib_pending         // TODO
    | rbnx_timers_bestpath_limit        // TODO
    | rbnx_timers_bgp                   // TODO
    | rbnx_timers_prefix_peer_timeout   // TODO
    | rbnx_timers_prefix_peer_wait      // TODO
;

rbnx_address_family
:
    ADDRESS_FAMILY first = (IPV4 | IPV6 | L2VPN) second = (MULTICAST | MVPN | UNICAST) NEWLINE
    rbnx_af_inner*
;

rbnx_af_inner
:
    rbnx_af_additional_paths
    | rbnx_af_aggregate_address
    | rbnx_af_client_to_client
    | rbnx_af_dampen_igp_metric
    | rbnx_af_dampening
    | rbnx_af_default_information
    | rbnx_af_default_metric
    | rbnx_af_distance
    | rbnx_af_inject_map
    | rbnx_af_maximum_paths
    | rbnx_af_network
    | rbnx_af_nexthop_route_map
    | rbnx_af_nexthop_trigger_delay
    | rbnx_af_redistribute_direct
    | rbnx_af_redistribute_eigrp
    | rbnx_af_redistribute_isis
    | rbnx_af_redistribute_lisp
    | rbnx_af_redistribute_ospf
    | rbnx_af_redistribute_rip
    | rbnx_af_redistribute_static
    | rbnx_af_suppress_inactive
    | rbnx_af_table_map
    | rbnx_af_wait_igp_convergence
;

rbnx_af_additional_paths
:
    ADDITIONAL_PATHS (
        INSTALL BACKUP
        | RECEIVE
        | SELECTION ROUTE_MAP mapname = variable
        | SEND
    ) NEWLINE
;

rbnx_af_aggregate_address
:
    AGGREGATE_ADDRESS (
        network = IP_ADDRESS MASK subnet = IP_ADDRESS
        | prefix = IP_PREFIX
    ) rbnx_af_aa_tail* NEWLINE
;

rbnx_af_aa_tail
:
    ADVERTISE_MAP mapname = variable
    | AS_SET
    | ATTRIBUTE_MAP mapname = variable
    | SUMMARY_ONLY
    | SUPPRESS_MAP mapname = variable
;

rbnx_af_client_to_client
:
    CLIENT_TO_CLIENT REFLECTION NEWLINE
;

rbnx_af_dampen_igp_metric
:
    DAMPEN_IGP_METRIC interval_secs = DEC NEWLINE
;

rbnx_af_dampening
:
    DAMPENING (
        half_life = DEC start_reuse = DEC start_suppress = DEC max_suppress = DEC
        | ROUTE_MAP mapname = variable
    ) NEWLINE
;

rbnx_af_default_information
:
    DEFAULT_INFORMATION ORIGINATE NEWLINE
;

rbnx_af_default_metric
:
    DEFAULT_METRIC metric = DEC NEWLINE
;

rbnx_af_distance
:
    DISTANCE ebgp = DEC ibgp = DEC local = DEC NEWLINE
;

rbnx_af_inject_map
:
    INJECT_MAP injectmap = variable EXIST_MAP existmap = variable COPY_ATTRIBUTES? NEWLINE
;

rbnx_af_maximum_paths
:
    MAXIMUM_PATHS (EIBGP | IBGP)? numpaths = DEC NEWLINE
;

rbnx_af_network
:
    NETWORK (
        address = IP MASK mask = IP
        | prefix = IP_PREFIX
    ) (ROUTE_MAP mapname = variable)? NEWLINE
;

rbnx_af_nexthop_route_map
:
    NEXTHOP ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_nexthop_trigger_delay
:
    NEXTHOP TRIGGER_DELAY CRITICAL critical = DEC NON_CRITICAL noncritical = DEC NEWLINE
;

rbnx_af_redistribute_direct
:
    REDISTRIBUTE DIRECT ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_redistribute_eigrp
:
    REDISTRIBUTE EIGRP source_tag = variable ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_redistribute_isis
:
    REDISTRIBUTE ISIS source_tag = variable ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_redistribute_lisp
:
    REDISTRIBUTE LISP ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_redistribute_ospf
:
    REDISTRIBUTE OSPF source_tag = variable ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_redistribute_rip
:
    REDISTRIBUTE RIP source_tag = variable ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_redistribute_static
:
    REDISTRIBUTE STATIC ROUTE_MAP mapname = variable NEWLINE
;

rbnx_af_suppress_inactive
:
    SUPPRESS_INACTIVE NEWLINE
;

rbnx_af_table_map
:
    TABLE_MAP mapname = variable FILTER? NEWLINE
;

rbnx_af_wait_igp_convergence
:
    WAIT_IGP_CONVERGENCE NEWLINE
;

rbnx_bestpath
:
    BESTPATH (
        ALWAYS_COMPARE_MED
        | AS_PATH MULTIPATH_RELAX
        | COMPARE_ROUTERID
        | COST_COMMUNITY IGNORE
        | MED (CONFED | MISSING_AS_WORST | NON_DETERMINISTIC)
    ) NEWLINE
;

rbnx_cluster_id
:
    CLUSTER_ID (
        ip_as_int = DEC
        | ip = IP_ADDRESS
    ) NEWLINE
;

rbnx_confederation_identifier
:
    CONFEDERATION IDENTIFIER bgp_asn NEWLINE
;

rbnx_confederation_peers
:
    CONFEDERATION PEERS bgp_asn NEWLINE bgp_asn+ NEWLINE
;

rbnx_enforce_first_as
:
    ENFORCE_FIRST_AS NEWLINE
;

rbnx_event_history
:
    EVENT_HISTORY (CLI | DETAIL | ERRORS | EVENTS | OBJSTORE | PERIODIC)
    (SIZE (DISABLE | LARGE | MEDIUM | SMALL | sizebytes = DEC))?
    NEWLINE
;

rbnx_fast_external_fallover
:
    FAST_EXTERNAL_FALLOVER NEWLINE
;

rbnx_flush_routes
:
    FLUSH_ROUTES
;

rbnx_graceful_restart
:
    GRACEFUL_RESTART (
        RESTART_TIME restart_secs = DEC
        | STALEPATH_TIME stalepath_secs = DEC
    )?
    NEWLINE
;

rbnx_graceful_restart_helper
:
    GRACEFUL_RESTART_HELPER NEWLINE
;

rbnx_log_neighbor_changes
:
    LOG_NEIGHBOR_CHANGES NEWLINE
;

rbnx_isolate
:
    ISOLATE NEWLINE
;

rbnx_maxas_limit
:
    MAXAS_LIMIT limit = DEC NEWLINE
;

rbnx_neighbor
:
    NEIGHBOR (ip = IP_ADDRESS | prefix = IP_PREFIX)
    (
        REMOTE_AS (
            bgp_asn?
            | ROUTE_MAP mapname = variable
        )
    )? NEWLINE
    rbnx_n_inner*
;

// We might get to this level of the hierarchy in four ways:
//  1. configuring an actual neighbor (router bgp ; (optional vrf) ; neighbor)
//  2. configuring a template peer (router bgp ; template peer)
//  3. configuring a template peer-session (router bgp ; template peer-session)
//  4. inside of DEFAULT command in any of the above 3 levels.
//     For now, DEFAULT is unhandled.
//
// We use a few different rules to distinguish the rest.
//  rbnx_n_common are commands valid at any of the levels 1-3.
//  rnbx_n_address_family is only valid at levels 1 and 2.
rbnx_n_inner
:
    rbnx_n_common
    | rbnx_n_address_family
;

rbnx_n_common
:
    rbnx_n_capability
    | rbnx_n_default
    | rbnx_n_description
    | rbnx_n_disable_connected_check
    | rbnx_n_dont_capability_negotiate
    | rbnx_n_dynamic_capability
    | rbnx_n_ebgp_multihop
    | rbnx_n_inherit
    | rbnx_n_local_as
    | rbnx_n_low_memory
    | rbnx_n_maximum_peers
    | rbnx_n_no_shutdown
    | rbnx_n_password
    | rbnx_n_remote_as
    | rbnx_n_remove_private_as
    | rbnx_n_shutdown
    | rbnx_n_timers
    | rbnx_n_transport
    | rbnx_n_update_source
;

rbnx_n_address_family
:
    ADDRESS_FAMILY first = (IPV4 | IPV6) second = (MULTICAST | UNICAST) NEWLINE
    rbnx_n_af_inner*
;

// We might get to this level of the hierarchy in four ways:
//  1. configuring an actual neighbor (router bgp ; (optional vrf) ; neighbor ; address-family)
//  2. configuring a template peer (router bgp ; template peer ; address-family)
//  3. configuring a template peer-policy (router bgp ; template peer-policy)
//  4. inside of DEFAULT command in any of the above 3 levels.
//     For now, DEFAULT is unhandled.
//
// All the remaining commands are valid at all levels 1-3.
rbnx_n_af_inner
:
    rbnx_n_af_advertise_map
    | rbnx_n_af_allowas_in
    | rbnx_n_af_as_override
    | rbnx_n_af_capability
    | rbnx_n_af_default
    | rbnx_n_af_default_originate
    | rbnx_n_af_disable_peer_as_check
    | rbnx_n_af_filter_list
    | rbnx_n_af_inherit
    | rbnx_n_af_maximum_prefix
    | rbnx_n_af_next_hop_self
    | rbnx_n_af_next_hop_third_party
    | rbnx_n_af_prefix_list
    | rbnx_n_af_route_map
    | rbnx_n_af_route_reflector_client
    | rbnx_n_af_send_community
    | rbnx_n_af_soft_reconfiguration
    | rbnx_n_af_soo
    | rbnx_n_af_suppress_inactive
    | rbnx_n_af_unsuppress_map
    | rbnx_n_af_weight
;

rbnx_n_af_advertise_map
:
    ADVERTISE_MAP mapname = variable (
        EXIST_MAP existmap = variable
        | NON_EXIST_MAP nonexistmap = variable
    ) NEWLINE
;

rbnx_n_af_allowas_in
:
    ALLOWAS_IN (num = DEC)? NEWLINE
;

rbnx_n_af_as_override
:
    AS_OVERRIDE NEWLINE
;

rbnx_n_af_capability
:
    CAPABILITY ADDITIONAL_PATHS (RECEIVE | SEND) DISABLE? NEWLINE
;

rbnx_n_af_default
:
    // TODO: implement default handling.
    DEFAULT null_rest_of_line
;

rbnx_n_af_default_originate
:
    DEFAULT_ORIGINATE (ROUTE_MAP mapname = variable)? NEWLINE
;

rbnx_n_af_disable_peer_as_check
:
    DISABLE_PEER_AS_CHECK NEWLINE
;

rbnx_n_af_filter_list
:
    FILTER_LIST name = variable (IN | OUT) NEWLINE
;

rbnx_n_af_inherit
:
    INHERIT PEER_POLICY template = variable seq = DEC NEWLINE
;

rbnx_n_af_maximum_prefix
:
    MAXIMUM_PREFIX limit = DEC (threshold_pct = DEC)?
    (
        RESTART interval_min = DEC
        | WARNING_ONLY
    )?
    NEWLINE
;

rbnx_n_af_next_hop_self
:
    NEXT_HOP_SELF NEWLINE
;

rbnx_n_af_next_hop_third_party
:
    NEXT_HOP_THIRD_PARTY NEWLINE
;

rbnx_n_af_prefix_list
:
    PREFIX_LIST listname = variable (IN | OUT) NEWLINE
;

rbnx_n_af_route_map
:
    ROUTE_MAP mapname = variable (IN | OUT) NEWLINE
;

rbnx_n_af_route_reflector_client
:
    ROUTE_REFLECTOR_CLIENT NEWLINE
;

rbnx_n_af_send_community
:
    SEND_COMMUNITY (BOTH | STANDARD | EXTENDED)? NEWLINE
;

rbnx_n_af_soft_reconfiguration
:
    SOFT_RECONFIGURATION INBOUND ALWAYS? NEWLINE
;

rbnx_n_af_soo
:
    SOO (bgp_asn | ip = IP_ADDRESS) COLON community_id = DEC NEWLINE
;

rbnx_n_af_suppress_inactive
:
    SUPPRESS_INACTIVE
;

rbnx_n_af_unsuppress_map
:
    UNSUPPRESS_MAP mapname = variable NEWLINE
;

rbnx_n_af_weight
:
    WEIGHT weight = DEC NEWLINE
;

rbnx_n_capability
:
    CAPABILITY SUPPRESS FOUR_BYTE_AS NEWLINE
;

rbnx_n_default
:
    // TODO: implement default handling.
    DEFAULT null_rest_of_line
;

rbnx_n_description
:
    DESCRIPTION desc = variable NEWLINE
;

rbnx_n_disable_connected_check
:
    DISABLE_CONNECTED_CHECK NEWLINE
;

rbnx_n_dont_capability_negotiate
:
    DONT_CAPABILITY_NEGOTIATE NEWLINE
;

rbnx_n_dynamic_capability
:
    DYNAMIC_CAPABILITY NEWLINE
;

rbnx_n_ebgp_multihop
:
    EBGP_MULTIHOP ebgp_ttl = DEC NEWLINE
;

rbnx_n_inherit
:
    INHERIT (PEER | PEER_SESSION) peer = variable NEWLINE
;

rbnx_n_local_as
:
    LOCAL_AS bgp_asn
    (NO_PREPEND (REPLACE_AS DUAL_AS?)?)?
    NEWLINE
;

rbnx_n_low_memory
:
    LOW_MEMORY EXEMPT NEWLINE
;

rbnx_n_maximum_peers
:
    MAXIMUM_PEERS max = DEC NEWLINE
;

rbnx_n_no_shutdown
:
    NO SHUTDOWN NEWLINE
;

rbnx_n_password
:
    PASSWORD type = DEC? password = ~NEWLINE+ NEWLINE
;

rbnx_n_remote_as
:
    REMOTE_AS bgp_asn NEWLINE
;

rbnx_n_remove_private_as
:
    REMOVE_PRIVATE_AS (ALL | REPLACE_AS)? NEWLINE
;

rbnx_n_shutdown
:
    SHUTDOWN NEWLINE
;

rbnx_n_timers
:
    TIMERS keepalive_secs = DEC holdtime_secs = DEC NEWLINE
;

rbnx_n_transport
:
    TRANSPORT CONNECTION_MODE PASSIVE NEWLINE
;

rbnx_n_update_source
:
    UPDATE_SOURCE interface_name NEWLINE
;

rbnx_reconnect_interval
:
    RECONNECT_INTERVAL secs = DEC NEWLINE
;

rbnx_router_id
:
    ROUTER_ID id = IP_ADDRESS NEWLINE
;

rbnx_shutdown
:
    SHUTDOWN NEWLINE
;

rbnx_suppress_fib_pending
:
    SUPPRESS_FIB_PENDING NEWLINE
;

rbnx_template_peer
:
    TEMPLATE PEER peer = variable NEWLINE
    rbnx_n_inner*
;

rbnx_template_peer_policy
:
    TEMPLATE PEER_POLICY policy = variable NEWLINE
    rbnx_n_af_inner*
;

rbnx_template_peer_session
:
    TEMPLATE PEER_SESSION session = variable NEWLINE
    rbnx_n_common*
;

rbnx_timers_bestpath_limit
:
    TIMERS BESTPATH_LIMIT timeout_secs = DEC ALWAYS? NEWLINE
;

rbnx_timers_bgp
:
    TIMERS BGP keepalive_secs = DEC holdtime_secs = DEC NEWLINE
;

rbnx_timers_prefix_peer_timeout
:
    TIMERS PREFIX_PEER_TIMEOUT timeout_secs = DEC NEWLINE
;

rbnx_timers_prefix_peer_wait
:
    TIMERS PREFIX_PEER_WAIT wait_secs = DEC NEWLINE
;

rbnx_vrf
:
    VRF name = variable NEWLINE
    (
        rbnx_proc_vrf_common
        | rbnx_v_local_as
    )*
;

rbnx_v_local_as
:
    LOCAL_AS bgp_asn NEWLINE
;