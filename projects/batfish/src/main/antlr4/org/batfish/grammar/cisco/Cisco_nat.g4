parser grammar CiscoParser;

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
    ipnio_source
    | ipni_destination
  )
;

ipn_outside
:
  OUTSIDE ipnio_source
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

// Source NAT is allowed on both inside and outside
ipnio_source
:
  SOURCE
  (
    ipnios_list
    // | ipnios_route_map
    | ipnios_static
  )
;

// Modifier common to different source NATs
ipnios_modifiers
:
   (
      ipniosm_add_route
      | NO_ALIAS
      // | ipniosm_overload
      // | ipniosm_vrf
   )
;

ipniosm_add_route
:
   ADD_ROUTE
;

ipnios_list
:
   // Delegate to common ACL NAT specification
   list = ipnc_list
   ipnios_modifiers*
   NEWLINE
;

ipnios_static
:
   STATIC
   (
      ipnios_static_addr
      | ipnios_static_network
   )
;

ipnios_static_addr
:
   ips = ipnioss_local_global
   ipnios_modifiers*
   NEWLINE
;

ipnios_static_network
:
   NETWORK ips = ipnioss_local_global
   (
      mask = IP_ADDRESS
      | FORWARD_SLASH prefix = DEC
   )
   ipnios_modifiers*
   NEWLINE
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
