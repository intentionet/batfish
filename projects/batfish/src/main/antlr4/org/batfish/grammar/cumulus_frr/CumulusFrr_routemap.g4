parser grammar CumulusFrr_routemap;

import CumulusFrr_common;

options {
  tokenVocab = CumulusFrrLexer;
}

s_routemap
:
  ROUTE_MAP name = word action = line_action sequence =
  route_map_sequence NEWLINE
  (
    rm_description
    | rm_match
    | rm_set
  )*
;

rm_description
:
  DESCRIPTION text = route_map_description NEWLINE
;

route_map_description
:
// 1-90 characters
  REMARK_TEXT
;

rm_match
:
  MATCH
  (
    rmm_community
    | rmm_interface
    | rmm_ip
  )
;

route_map_sequence
:
// 0-65535
  uint32
;

rmm_community
:
  COMMUNITY names += ip_community_list_name+ NEWLINE
;

rm_set
:
  SET
  (
    rms_metric
    | rms_community
    | rms_ip
  )
;

rms_metric
:
  METRIC metric = uint32 NEWLINE
;

rmm_ip
:
  IP rmmip_address
;

rmmip_address
:
  ADDRESS rmmipa_prefix_list
;

rmmipa_prefix_list
:
  PREFIX_LIST name = ip_prefix_list_name NEWLINE
;

rmm_interface
:
  INTERFACE name = WORD NEWLINE
;

rms_ip
:
  IP rmsip_next_hop
;

rmsip_next_hop
:
  NEXT_HOP
  (
    rmsipnh_literal
  )
;

rmsipnh_literal
:
  next_hop = ip_address NEWLINE
;

rms_community
:
  COMMUNITY communities += literal_standard_community+ NEWLINE
;
