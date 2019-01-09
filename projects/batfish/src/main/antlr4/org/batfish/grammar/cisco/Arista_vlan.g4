parser grammar Arista_mlag;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

eos_vlan_id
:
   (
      vlan_ids += subrange
      (
         COMMA vlan_ids += subrange
      )*
   ) NEWLINE
;

eos_vlan_internal
:
   NO? INTERNAL ALLOCATION POLICY (ASCENDING | DESCENDING) RANGE lo=DEC hi=DEC NEWLINE
;

eos_vlan_name
:
   NAME name = variable NEWLINE
;

eos_vlan_state
:
   STATE (ACTIVE | SUSPEND) NEWLINE
;

eos_vlan_trunk
:
   TRUNK GROUP name = variable NEWLINE
;
