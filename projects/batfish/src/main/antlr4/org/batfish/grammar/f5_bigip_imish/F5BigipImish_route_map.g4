parser grammar F5BigipImish_route_map;

import F5BigipImish_common;

options {
  tokenVocab = F5BigipImishLexer;
}

origin_type
:
  EGP
  | IGP
  | INCOMPLETE
;

rm_match
:
  MATCH
  (
    rmm_ip_address
    | rmm_ip_address_prefix_list
  )
;

rmm_ip_address
:
  IP ADDRESS name = word NEWLINE
;

rmm_ip_address_prefix_list
:
  IP ADDRESS PREFIX_LIST name = word NEWLINE
;

rm_set
:
  SET
  (
    rms_community
    | rms_metric
    | rms_origin
    | rms_ip
  )
;

rms_community
:
  COMMUNITY communities += standard_community+ NEWLINE
;

rms_metric
:
  METRIC metric = uint32 NEWLINE
;

rms_origin
:
  ORIGIN origin = origin_type NEWLINE
;

rms_ip
:
  IP rms_next_hop
;

rms_next_hop
:
  NEXT_HOP ip = IP_ADDRESS PRIMARY? NEWLINE
;

standard_community
:
  STANDARD_COMMUNITY
;

s_route_map
:
  ROUTE_MAP name = word action = line_action num = uint32 NEWLINE
  (
    rm_match
    | rm_set
  )*
;