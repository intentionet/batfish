parser grammar F5BigipStructured_bgp;

import F5BigipStructured_common;

options {
  tokenVocab = F5BigipStructuredLexer;
}

nr_bgp
:
  BGP name = word BRACE_LEFT
  (
    NEWLINE
    (
      nrb_address_family
      | nrb_local_as
      | nrb_neighbor
      | nrb_router_id
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrb_address_family
:
  ADDRESS_FAMILY BRACE_LEFT
  (
    NEWLINE
    (
      nrbaf_ipv4
      | nrbaf_ipv6
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbaf_ipv4
:
  IPV4 BRACE_LEFT
  (
    NEWLINE
    (
      nrbaf_common
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbaf_ipv6
:
  IPV6 BRACE_LEFT
  (
    NEWLINE
    (
      nrbaf_common
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbaf_common
:
  nrbafc_redistribute
;

nrbafc_redistribute
:
  REDISTRIBUTE BRACE_LEFT
  (
    NEWLINE
    (
      nrbafcr_kernel
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbafcr_kernel
:
  KERNEL BRACE_LEFT
  (
    NEWLINE
    (
      nrbafcrk_route_map
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbafcrk_route_map
:
  ROUTE_MAP name = word NEWLINE
;

nrb_local_as
:
  LOCAL_AS as = word NEWLINE
;

nrb_neighbor
:
  NEIGHBOR BRACE_LEFT
  (
    NEWLINE nrbn_name*
  )? BRACE_RIGHT NEWLINE
;

nrbn_name
:
  name = word BRACE_LEFT
  (
    NEWLINE
    (
      nrbnn_address_family
      | nrbnn_description
      | nrbnn_remote_as
      | nrbnn_update_source
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbnn_address_family
:
  ADDRESS_FAMILY BRACE_LEFT
  (
    NEWLINE
    (
      nrbnnaf_ipv4
      | nrbnnaf_ipv6
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbnnaf_ipv4
:
  IPV4 BRACE_LEFT
  (
    NEWLINE
    (
      nrbnnaf_common
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbnnaf_ipv6
:
  IPV6 BRACE_LEFT
  (
    NEWLINE
    (
      nrbnnaf_common
      | unrecognized
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbnnaf_common
:
  nrbnnafc_activate
  | nrbnnafc_route_map
;

nrbnnafc_activate
:
  ACTIVATE DISABLED NEWLINE
;

nrbnnafc_route_map
:
  ROUTE_MAP BRACE_LEFT
  (
    NEWLINE
    (
      nrbnnafcr_out
    )*
  )? BRACE_RIGHT NEWLINE
;

nrbnnafcr_out
:
  OUT name = word NEWLINE
;

nrbnn_description
:
  DESCRIPTION description = word NEWLINE
;

nrbnn_remote_as
:
  REMOTE_AS as = word NEWLINE
;

nrbnn_update_source
:
  UPDATE_SOURCE name = word NEWLINE
;

nrb_router_id
:
  ROUTER_ID id = word NEWLINE
;
