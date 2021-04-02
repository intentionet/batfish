parser grammar CiscoNxos_static;

import CiscoNxos_common;

options {
  tokenVocab = CiscoNxosLexer;
}

ip_route
:
  ROUTE
  (
    ip_route_network
    | ip_route_static
  )
;

ip_route_network
:
  network = route_network
  (
    null0 = NULL0
    | nhint = interface_name nhip = ip_address?
    | nhip = ip_address
  )
  (
    VRF nhvrf = vrf_name
  )?
  (
    TRACK track = track_object_number
  )?
  (
    NAME name = static_route_name
  )?
  (
    (TAG tag = uint32) pref = protocol_distance?
    | pref = protocol_distance (TAG tag = uint32)?
  )? NEWLINE
;

static_route_name
:
// 1-50 characters
  WORD
;

ip_route_static
:
  STATIC BFD name = interface_name ip = ip_address NEWLINE
;

ipv6_route
:
  ROUTE network = ipv6_prefix
  (
    null0 = NULL0
    | nhint = interface_name nhip = ipv6_address?
    | nhip = ipv6_address
  )
  (
    VRF nhvrf = vrf_name
  )?
  (
    TRACK track = track_object_number
  )?
  (
    NAME name = static_route_name
  )?
  (
    (TAG tag = uint32) pref = protocol_distance?
    | pref = protocol_distance (TAG tag = uint32)?
  )? NEWLINE
;