parser grammar FlatJuniper_snmp;

import FlatJuniper_common;

options {
   tokenVocab = FlatJuniperLexer;
}

s_snmp
:
   SNMP
   (
      snmp_community
      | snmp_null
      | snmp_trap_group
   )
;

snmp_community
:
   COMMUNITY comm = variable
   (
      apply
      | snmpc_authorization
      | snmpc_client_list_name
      | snmpc_null
   )
;

snmp_null
:
   (
      CONTACT
      | DESCRIPTION
      | FILTER_DUPLICATES
      | FILTER_INTERFACES
      | INTERFACE
      | LOCATION
      | STATS_CACHE_LIFETIME
      | TRACEOPTIONS
      | TRAP_OPTIONS
      | VIEW
   ) null_filler
;

snmp_trap_group
:
   TRAP_GROUP name = variable
   (
      snmptg_null
      | snmptg_targets
   )
;

snmpc_authorization
:
   AUTHORIZATION
   (
      READ_ONLY
      | READ_WRITE
   )
;

snmpc_client_list_name
:
   CLIENT_LIST_NAME name = variable
;

snmpc_null
:
   (
      CLIENTS
      | VIEW
   ) null_filler
;

snmptg_null
:
   (
      CATEGORIES
      | VERSION
   ) null_filler
;

snmptg_targets
:
   TARGETS target = IP_ADDRESS
;
