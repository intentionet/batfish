parser grammar Cisco_nat;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

s_ip_nat
:
  IP NAT
  (
    // ipn_create
    ipn_inside
    | ipn_log
    // | ipn_name
    | ipn_outside
    | ipn_pool
    // | ipn_service
    // | ipn_settings
    // | ipn_switchover
    | ipn_translation
  )
;

ipn_inside
:
  INSIDE
  (
    ipni_source
    | ipni_destination
  )
;

ipn_outside
:
  OUTSIDE ipno_source
;

ipn_log
:
  LOG null_rest_of_line
;

// ipn_name
// :
//   name = variable
//   (
//     ipn_inside
//     | ipn_outside
//   )
// ;

ipn_translation
:
  TRANSLATION null_rest_of_line
;

// Defines a pool using an ACL, applicable to multiple types of NAT
ipnc_list
:
   LIST acl = variable POOL pool = variable
;

ipni_source
:
  SOURCE
  (
    ipnis_list
    | ipnis_route_map
    | ipnis_static
  )
;

ipno_source
:
  SOURCE
  (
    ipnos_list
    | ipnos_route_map
    | ipnos_static
  )
;

ipnosm_add_route
:
   ADD_ROUTE
;

ipnis_list
:
   // Delegate to common ACL NAT specification
   acl_pool = ipnc_list
   // order matters
   ipnios_vrf?
   OVERLOAD?
   NEWLINE
;

ipnis_route_map
:
   ROUTE_MAP mapname = variable
   INTERFACE iname = interface_name
   ( VRF variable )?
   OVERLOAD?
   NEWLINE
;

ipnis_static
:
   STATIC
   (
      ipnios_static_addr
      | ipnios_static_network
   )
   //order of these options matters
   ipnios_vrf?
   ( ROUTE_MAP mapname = variable )?
   ipnioss_modifiers*
   NEWLINE
;

ipnos_list
:
   // Delegate to common ACL NAT specification
   acl_pool = ipnc_list
   // order matters
   ipnios_vrf?
   ipnosm_add_route?
   NEWLINE
;

ipnos_route_map
:
   ROUTE_MAP mapname = variable
   POOL pname = variable
   ( VRF vrfname = variable )?
   ipnosm_add_route?
   NEWLINE
;

ipnos_static
:
   STATIC
   (
      ipnios_static_addr
      | ipnios_static_network
   )
   ipnios_vrf?
   (
      ipnosm_add_route
      | ipnioss_modifiers
   )*
   NEWLINE
;

ipnios_vrf
:
  VRF vrfname = variable
;

// common modifiers for inside and outside source static NAT
ipnioss_modifiers
:
   ipniossm_extendable
   | NO_ALIAS
   | NO_PAYLOAD
   | REDUNDANCY variable
;

ipniossm_extendable
:
   EXTENDABLE
;


ipnios_static_addr
:
   ips = ipnioss_local_global
;

ipnios_static_network
:
   NETWORK ips = ipnioss_local_global
   (
      mask = IP_ADDRESS
      | FORWARD_SLASH prefix = DEC
   )
;

ipnioss_local_global
:
   local = IP_ADDRESS global = IP_ADDRESS
;

ipni_destination
:
   DESTINATION list = ipnc_list
   // Note: other modifiers such as vrf, overload, etc are not allowed on dest NAT.
   // TODO: (redundancy num = DEC)?
   NEWLINE
;

ipn_pool
:
   POOL name = variable
   (
     ipn_pool_prefix
     | ipn_pool_range
   )
;

ipn_pool_prefix
:
   first = IP_ADDRESS last = IP_ADDRESS
   (
      NETMASK mask = IP_ADDRESS
      | PREFIX_LENGTH prefix_length = DEC
   )? NEWLINE
;

ipn_pool_range
:
   PREFIX_LENGTH prefix_length = DEC NEWLINE
   (
      RANGE first = IP_ADDRESS last = IP_ADDRESS NEWLINE
   )+
;
