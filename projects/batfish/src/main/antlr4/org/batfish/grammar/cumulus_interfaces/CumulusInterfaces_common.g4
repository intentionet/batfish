parser grammar CumulusInterfaces_common;

options {
  tokenVocab = CumulusInterfacesLexer;
}

interface_name
:
  WORD
;

vrf_name
:
  WORD
;

vrf_table_name
:
  WORD
;