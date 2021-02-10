parser grammar CumulusFrr_common;

options {
  tokenVocab = CumulusFrrLexer;
}

autonomous_system
:
  uint32
;

double_quoted_string
:
  DOUBLE_QUOTE text = quoted_text? DOUBLE_QUOTE
;

ip_address
:
  IP_ADDRESS
  | SUBNET_MASK
;

ip_community_list_name
:
// 1-63 characters
  WORD
;

ip_prefix_list_name
:
// 1-63 chars
  WORD
;

ip_prefix_length
:
// 0-32
  UINT8
;

line_action
:
  deny = DENY
  | permit = PERMIT
;

literal_as_path
:
  (
    asns += uint32
  )+
;

literal_standard_community
:
  high = uint16 COLON low = uint16
;

loglevel
:
    ALERTS
    | CRITICAL
    | DEBUGGING
    | EMERGENCIES
    | ERRORS
    | INFORMATIONAL
    | NOTIFICATIONS
    | WARNINGS
;

ospf_area
:
  ip = ip_address
  | num = uint32
;

ospf_redist_type
:
  STATIC | CONNECTED | BGP
;

prefix
:
  IP_PREFIX
;

quoted_text
:
  QUOTED_TEXT
;

route_map_name
:
  WORD
;

bgp_redist_type
:
   STATIC | CONNECTED | OSPF
;

standard_community
:
  literal = literal_standard_community
  | INTERNET
  | LOCAL_AS
  | NO_ADVERTISE
  | NO_EXPORT
;

vni_number
:
  v = uint32
  {isVniNumber($v.ctx)}?

;

uint8
:
  UINT8
;

uint16
:
  UINT8
  | UINT16
;

vrf_name
:
  word
;

uint32
:
  UINT8
  | UINT16
  | UINT32
;

word
:
  WORD
;

null_rest_of_line
:
  ~NEWLINE* NEWLINE
;