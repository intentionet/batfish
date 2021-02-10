parser grammar CiscoNxos_ip_prefix_list;

import CiscoNxos_common;

options {
  tokenVocab = CiscoNxosLexer;
}

ip_prefix_list
:
  PREFIX_LIST name = ip_prefix_list_name
  (
    pl_action
    | pl_description
  )
;

pl_action
:
  (
    SEQ num = ip_prefix_list_line_number
  )? action = line_action prefix = ip_prefix
  (
    EQ eq = ip_prefix_length
    | (GE ge = ip_prefix_length)? (LE le = ip_prefix_length)?
  )
  (
    MASK mask = ip_address
  )? NEWLINE
;

ip_prefix_length
:
// 1-32
  UINT8
;

pl_description
:
  DESCRIPTION text = ip_prefix_list_description NEWLINE
;
