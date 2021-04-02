parser grammar CiscoNxos_vrf;

import CiscoNxos_common;

options {
  tokenVocab = CiscoNxosLexer;
}

s_vrf_context
:
  VRF CONTEXT name = vrf_name NEWLINE
  (
    vc_address_family
    | vc_description
    | vc_ip
    | vc_ipv6
    | vc_no
    | vc_null
    | vc_rd
    | vc_shutdown
    | vc_vni
  )*
;

vc_address_family
:
  ADDRESS_FAMILY
  (
    (
      IPV4 UNICAST NEWLINE
      (
        vcaf4u_null
        | vcaf4u_route_target
      )*
    )
    |
    (
      IPV6 UNICAST NEWLINE
      (
        vcaf6u_null
        | vcaf6u_route_target
      )*
    )
  )
;

vcaf4u_null
:
  NO?
  (
    MAXIMUM
  ) null_rest_of_line
;

vcaf4u_route_target
:
  ROUTE_TARGET both_export_import rt = route_target_or_auto EVPN? NEWLINE
;

vcaf6u_null
:
  NO?
  (
    MAXIMUM
  ) null_rest_of_line
;

vcaf6u_route_target
:
  ROUTE_TARGET both_export_import rt = route_target_or_auto EVPN? NEWLINE
;

vc_description
 :
   DESCRIPTION desc = vrf_description NEWLINE
;

vrf_description
:
// 1-255
  desc = REMARK_TEXT
;

vc_ip
:
  IP
  (
    ip_route
    | ip_name_server
    | vc_ip_null
  )
;

vc_ip_null
:
  (
    DOMAIN_LIST
    | DOMAIN_NAME
    | PIM
  )
  null_rest_of_line
;

vc_ipv6
:
  IPV6
  (
    ipv6_route
  )
;

vc_no
:
  NO vc_no_shutdown
;

vc_no_shutdown
:
  SHUTDOWN NEWLINE
;

vc_null
:
  (
    MDT
    | PIM
  ) null_rest_of_line
;

vc_rd
:
  RD rd = route_distinguisher_or_auto NEWLINE
;

vc_shutdown
:
  SHUTDOWN NEWLINE
;

vc_vni
:
  VNI vni_number NEWLINE
;
