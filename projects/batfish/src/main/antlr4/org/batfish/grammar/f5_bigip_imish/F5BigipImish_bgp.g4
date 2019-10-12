parser grammar F5BigipImish_bgp;

import F5BigipImish_common;

options {
  tokenVocab = F5BigipImishLexer;
}

rb_bgp_always_compare_med
:
  ALWAYS_COMPARE_MED NEWLINE
;

rb_bgp_deterministic_med
:
  DETERMINISTIC_MED NEWLINE
;

rb_bgp_null
:
  (
    GRACEFUL_RESTART
    | LOG_NEIGHBOR_CHANGES
  ) null_rest_of_line
;

rb_bgp_router_id
:
  ROUTER_ID id = IP_ADDRESS NEWLINE
;

rb_neighbor_ipv4
:
  NO? NEIGHBOR ip = IP_ADDRESS
  (
    rbn_common
    | rbn_peer_group_assign
  )
;

rb_neighbor_ipv6
:
  NO? NEIGHBOR ip6 = IPV6_ADDRESS
  (
    rbn_common
    | rbn_peer_group_assign
  )
;

rb_neighbor_peer_group
:
  NO? NEIGHBOR name = peer_group_name
  (
    rbn_common
    | rbn_peer_group
  )
;

rbn_common
:
  (
    rbn_description
    | rbn_next_hop_self
    | rbn_null
    | rbn_remote_as
    | rbn_route_map_out
    | rbn_password
    | rbn_update_source
  )
;

rbn_description
:
  DESCRIPTION text = DESCRIPTION_LINE NEWLINE
;

rbn_next_hop_self
:
  NEXT_HOP_SELF NEWLINE
;

rbn_password
:
  PASSWORD password = DESCRIPTION_LINE NEWLINE
;

rbn_peer_group
:
  PEER_GROUP NEWLINE
;

rbn_peer_group_assign
:
  PEER_GROUP name = peer_group_name NEWLINE
;

rbn_null
:
  (
    ADVERTISEMENT_INTERVAL
    | CAPABILITY
    | FALL_OVER
    | MAXIMUM_PREFIX
  ) null_rest_of_line
;

rbn_remote_as
:
  REMOTE_AS remoteas = uint32 NEWLINE
;

rbn_route_map_out
:
  ROUTE_MAP name = word OUT NEWLINE
;

rbn_update_source
:
  rbn_update_source_ip
  | rbn_update_source_interface
;

rbn_update_source_ip
:
  UPDATE_SOURCE ip = IP_ADDRESS NEWLINE
;

rbn_update_source_interface
:
  UPDATE_SOURCE name = word NEWLINE
;

rb_null
:
  (
    MAX_PATHS
  ) null_rest_of_line
;

rb_redistribute
:
  REDISTRIBUTE
  (
    rbr_kernel
    | rbr_connected
  )
;

rbr_kernel
:
  KERNEL
  (
    ROUTE_MAP rm = word
  )?
  NEWLINE
;

rbr_connected
:
  CONNECTED NEWLINE
;

router_bgp
:
  BGP localas = uint32 NEWLINE
  (
    rb_aggregate_address
    | rb_bgp
    | rb_neighbor_ipv4
    | rb_neighbor_ipv6
    | rb_neighbor_peer_group
    | rb_no
    | rb_null
    | rb_redistribute
  )*
;

peer_group_name
:
  ~( IP_ADDRESS | IPV6_ADDRESS | NEWLINE )
;

rb_aggregate_address
:
  AGGREGATE_ADDRESS prefix = ip_prefix
  (
    as_set = AS_SET summary_only = SUMMARY_ONLY?
    | summary_only = SUMMARY_ONLY
  )? NEWLINE
;

rb_bgp
:
  BGP
  (
    rb_bgp_always_compare_med
    | rb_bgp_confederation
    | rb_bgp_deterministic_med
    | rb_bgp_null
    | rb_bgp_router_id
  )
;

rb_bgp_confederation
:
  CONFEDERATION
  (
    rbbc_identifier
    | rbbc_peers
  )
;

rbbc_identifier
:
  IDENTIFIER id = uint32 NEWLINE
;

rbbc_peers
:
  PEERS peers += uint32+ NEWLINE
;

rb_no
:
  NO rb_no_null
;

rb_no_null
:
  (
    BGP
    | MAX_PATHS
  ) null_rest_of_line
;
