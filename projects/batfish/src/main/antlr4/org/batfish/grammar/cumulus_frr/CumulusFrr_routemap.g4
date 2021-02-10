parser grammar CumulusFrr_routemap;

import CumulusFrr_common;

options {
  tokenVocab = CumulusFrrLexer;
}

int_expr
:
  (
     PLUS
     | DASH
  )? uint32
;

s_routemap
:
  ROUTE_MAP name = word action = line_action sequence =
  route_map_sequence NEWLINE
  (
    rm_call
    | rm_description
    | rm_match
    | rm_on_match
    | rm_set
  )*
;

rm_call
:
  CALL name = word NEWLINE
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
    rmm_as_path
    | rmm_community
    // | rmm_evpn
    // | rmm_extcommunity
    | rmm_interface
    | rmm_ip
    | rmm_ipv6
    // | rmm_large_community
    // | rmm_local_preference
    // | rmm_mac
    // | rmm_metric
    // | rmm_origin
    // | rmm_peer
    // | rmm_probability
    // | rmm_source_protocol
    // | rmm_source_vrf
    | rmm_tag
  )
;

route_map_sequence
:
// 0-65535
  uint32
;

rmm_as_path
:
  AS_PATH name = word NEWLINE
;

rmm_community
:
  COMMUNITY names += ip_community_list_name+ NEWLINE
;

rm_set
:
  SET
  (
    rms_as_path
    | rms_comm_list
    | rms_community
    | rms_ip
    | rms_local_preference
    | rms_metric
    | rms_tag
    | rms_weight
  )
;

rms_weight
:
  WEIGHT weight = uint32 NEWLINE
;

rms_metric
:
  METRIC metric = int_expr NEWLINE
;

rmm_ip
:
  IP
  (
    rmmip_address
    // | rmmip_next_hop
    // | rmmip_route_source
  )
;

rmm_ipv6
:
  IPV6 null_rest_of_line
;

rmm_tag
:
  TAG tag = uint32 NEWLINE
;

rmmip_address
:
  ADDRESS
  (
    // rmmipa_access_list_standard
    // rmmipa_access_list_extended
    // rmmipa_access_list_name
    rmmipa_prefix_len
    | rmmipa_prefix_list
  )
;

rmmipa_prefix_len
:
// len; 0-32
  PREFIX_LEN len = ip_prefix_length NEWLINE
;

rmmipa_prefix_list
:
  PREFIX_LIST name = ip_prefix_list_name NEWLINE
;

rmm_interface
:
  INTERFACE name = WORD NEWLINE
;

rms_as_path
:
  AS_PATH PREPEND as_path = literal_as_path NEWLINE
;

rm_on_match
:
  ON_MATCH
  (
    rmom_next
    | rmom_goto
  ) NEWLINE
;

rmom_next
:
  NEXT
;

rmom_goto
:
  GOTO seq=uint32
;

rms_ip
:
  IP rmsip_next_hop
;

rms_tag
:
  TAG tag = uint32 NEWLINE
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

rms_comm_list
:
  COMM_LIST name = ip_community_list_name DELETE NEWLINE
;

rms_community
:
  COMMUNITY communities += literal_standard_community+ ADDITIVE? NEWLINE
;

rms_local_preference
:
  LOCAL_PREFERENCE pref = uint32 NEWLINE
;

