parser grammar CiscoNxos_ospf;

import CiscoNxos_common;

options {
  tokenVocab = CiscoNxosLexer;
}

router_ospf
:
  OSPF name = router_ospf_name NEWLINE
  (
    ro_common
    | ro_vrf
  )*
;

ro_common
:
  ro_area
  | ro_auto_cost
  | ro_bfd
  | ro_default_information
  | ro_graceful_restart
  | ro_log_adjacency_changes
  | ro_max_metric
  | ro_name_lookup
  | ro_network
  | ro_passive_interface
  | ro_redistribute
  | ro_router_id
  | ro_summary_address
  | ro_timers
;

ro_area
:
  AREA id = ospf_area_id
  (
    roa_authentication
    | roa_default_cost
    | roa_filter_list
    | roa_nssa
    | roa_range
    | roa_stub
    | roa_virtual_link
  )
;

roa_authentication
:
  AUTHENTICATION digest = MESSAGE_DIGEST? NEWLINE
;

roa_default_cost
:
  DEFAULT_COST cost = ospf_area_default_cost NEWLINE
;

roa_filter_list
:
  FILTER_LIST ROUTE_MAP name = route_map_name
  (
    in = IN
    | out = OUT
  ) NEWLINE
;

roa_nssa
:
// non-zero area only
  NSSA
  (
    no_redistribution = NO_REDISTRIBUTION
    | no_summary = NO_SUMMARY
    | ROUTE_MAP rm = route_map_name
  )* NEWLINE
;

roa_range
:
  RANGE network = route_network
  (
    COST cost = ospf_area_range_cost not_advertise = NOT_ADVERTISE?
    | not_advertise = NOT_ADVERTISE
    (
      COST cost = ospf_area_range_cost
    )?
  )? NEWLINE
;

roa_stub
:
// non-zero area only
  STUB no_summary = NO_SUMMARY? NEWLINE
;

roa_virtual_link
:
  VIRTUAL_LINK ip = ip_address NEWLINE
;

ro_auto_cost
:
  AUTO_COST REFERENCE_BANDWIDTH
  (
    gbps = ospf_ref_bw_gbps GBPS
    | mbps = ospf_ref_bw_mbps MBPS?
  ) NEWLINE
;

ro_bfd
:
  BFD NEWLINE
;

ro_default_information
:
  DEFAULT_INFORMATION ORIGINATE always = ALWAYS?
  (
    ROUTE_MAP rm = route_map_name
  )? NEWLINE
;

ro_graceful_restart
:
  GRACEFUL_RESTART
  (
    rogr_graceful_restart
    | rogr_null
  )
;

rogr_graceful_restart
:
  NEWLINE
;

rogr_null
:
  (
    GRACE_PERIOD
    | HELPER_DISABLE
    | PLANNED_ONLY
  ) null_rest_of_line
;

ro_log_adjacency_changes
:
  LOG_ADJACENCY_CHANGES DETAIL? NEWLINE
;

ro_max_metric
:
  MAX_METRIC ROUTER_LSA
  (
    external_lsa = EXTERNAL_LSA manual_external_lsa = ospf_max_metric_external_lsa?
  )? include_stub = INCLUDE_STUB?
  (
    on_startup = ON_STARTUP wait_period = ospf_on_startup_wait_period?
    (
      WAIT_FOR BGP as = bgp_asn
    )?
  )?
  (
    summary_lsa = SUMMARY_LSA manual_summary_lsa = max_metric_summary_lsa?
  )? NEWLINE
;

max_metric_summary_lsa
:
// 1-16777215
  uint32
;

ro_name_lookup
:
  NAME_LOOKUP NEWLINE
;

ro_network
:
  NETWORK
  (
    ip = ip_address wildcard = ip_address
    | prefix = ip_prefix
  ) AREA id = ospf_area_id NEWLINE
;

ro_passive_interface
:
  PASSIVE_INTERFACE DEFAULT NEWLINE
;

ro_redistribute
:
  REDISTRIBUTE
  (
    ror_redistribute_route_map
    | ror_null
  )
;

ror_null
:
  MAXIMUM_PREFIX null_rest_of_line
;

ror_redistribute_route_map
:
  routing_instance_v4 ROUTE_MAP mapname = route_map_name NEWLINE
;

ro_router_id
:
  ROUTER_ID id = ip_address NEWLINE
;

ro_summary_address
:
  SUMMARY_ADDRESS network = route_network
  (
    not_advertise = NOT_ADVERTISE
    | TAG tag = uint32
  )? NEWLINE
;

ro_timers
:
  TIMERS
  (
    rot_lsa_arrival
    | rot_lsa_group_pacing
    | rot_throttle
  )
;

rot_lsa_arrival
:
  LSA_ARRIVAL time_ms = lsa_arrival_interval NEWLINE
;

lsa_arrival_interval
:
// 10-600000
  uint32
;

rot_lsa_group_pacing
:
  LSA_GROUP_PACING time_s = lsa_group_pacing_interval NEWLINE
;

lsa_group_pacing_interval
:
// 1-1800
  uint16
;

rot_throttle
:
  THROTTLE
  (
    rott_lsa
    | rott_null
  )
;

rott_lsa
:
  LSA start_interval_ms = lsa_start_interval hold_interval_ms =
  lsa_hold_interval max_interval_ms = lsa_max_interval NEWLINE
;

lsa_start_interval
:
// 0-5000
  uint16
;

lsa_hold_interval
:
// 50-30000
  uint16
;

lsa_max_interval
:
// 50-30000
  uint16
;

rott_null
:
  SPF null_rest_of_line
;

ro_vrf
:
  VRF name = vrf_non_default_name NEWLINE
  ro_common*
;
