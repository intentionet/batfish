parser grammar CiscoNxos_mac;

import CiscoNxos_common;

options {
  tokenVocab = CiscoNxosLexer;
}

s_mac
:
  MAC mac_access_list
;

mac_access_list
:
  ACCESS_LIST name = mac_access_list_name NEWLINE
  macl_line*
;

macl_line
:
  num = ip_access_list_line_number?
  (
    macll_action
  )
;

macll_action
:
  action = line_action maclla_src_address_spec maclla_dst_address_spec maclla_protocol_spec? NEWLINE
;

maclla_address_spec
:
  address = mac_address_literal wildcard = mac_address_literal
  | ANY
;

maclla_dst_address_spec
:
  maclla_address_spec
;

maclla_src_address_spec
:
  maclla_address_spec
;

maclla_protocol_spec
:
  // TODO: ethertype
  ANY // not actually valid
;