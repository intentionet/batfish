parser grammar CumulusNclu_common;

options {
  tokenVocab = CumulusNcluLexer;
}

glob
:
  glob_range_set
  (
    COMMA glob_range_set
  )*
;

glob_range
:
  GLOB_RANGE
;

glob_range_set
:
  glob_range
  | range
  | (aword = word)
;

interface_address
:
  IP_PREFIX
;

ip_address
:
  IP_ADDRESS
;

ip_prefix
:
  IP_PREFIX
;

ipv6_address
:
  IPV6_ADDRESS
;

line_action
:
  DENY
  | PERMIT
;

mac_address
:
  MAC_ADDRESS
;

null_rest_of_line
:
  ~NEWLINE* NEWLINE
;

range
:
  NUMERIC_RANGE | DEC
;

range_set
:
  range
  (
    COMMA range
  )*
;

uint16
:
  d = DEC
  {isUint16($d)}?
;

uint32
:
  d = DEC
  {isUint32($d)}?
;

vlan_id
:
  v = DEC
  {isVlanId($v)}?
;

vlan_range_set
:
  (range | vlan_id)
  (
    COMMA (range | vlan_id)
  )*
;

vni_number
:
  v = DEC
  {isVniNumber($v)}?

;

word
:
  ~NEWLINE
;

zero
:
  d = DEC { isZero($d) }?
;
