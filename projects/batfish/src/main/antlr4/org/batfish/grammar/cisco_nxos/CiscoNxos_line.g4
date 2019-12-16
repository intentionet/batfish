parser grammar CiscoNxos_line;

import CiscoNxos_common;

options {
  tokenVocab = CiscoNxosLexer;
}

line_timeout
:
// 0-525600, in minutes, 0 to disable
  uint32
;

s_line
:
  LINE
  (
    line_console
    | line_vty
  )
;

line_console
:
  CONSOLE NEWLINE
  (
    lc_exec_timeout
  )*
;

lc_exec_timeout
:
  EXEC_TIMEOUT mins = line_timeout NEWLINE
;

line_vty
:
  VTY NEWLINE
  (
    lv_access_class
    | lv_exec_timeout
    | lv_ipv6
    | lv_session_limit
  )*
;

lv_access_class
:
  ACCESS_CLASS acl = ip_access_list_name (IN | OUT) NEWLINE
;

lv_exec_timeout
:
  EXEC_TIMEOUT mins = line_timeout NEWLINE
;

lv_ipv6
:
  IPV6 lv6_access_class
;

lv6_access_class
:
  ACCESS_CLASS acl = ip_access_list_name (IN | OUT) NEWLINE
;

lv_session_limit
:
  SESSION_LIMIT limit = uint8 NEWLINE
;