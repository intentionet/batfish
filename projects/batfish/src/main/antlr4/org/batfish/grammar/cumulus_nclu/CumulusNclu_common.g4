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

glob_range_set
:
  unnumbered = glob_word
  |
  (
    base_word = numbered_word
    (
      DASH first_interval_end = uint32
    )?
    (
      COMMA other_numeric_ranges = range_set
    )?
  )
;

glob_word
:
  ~( NEWLINE | NUMBERED_WORD | DEC )
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

numbered_word
:
  NUMBERED_WORD
;

range
:
  low = uint32
  (
    DASH high = uint32
  )?
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

vlan_range
:
  low = vlan_id
  (
    DASH high = vlan_id
  )?
;

vlan_range_set
:
  vlan_range
  (
    COMMA vlan_range
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
