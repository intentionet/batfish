lexer grammar CumulusInterfacesLexer;

options {
  superClass = 'org.batfish.grammar.cumulus_interfaces.parsing.CumulusInterfacesBaseLexer';
}
tokens {
  TEXT, WORD
}

// Keyword tokens

ADDRESS
:
  'address'
;

ADDRESS_VIRTUAL
:
  'address-virtual'
;

ALIAS
:
  'alias' -> pushMode(M_LineText)
;

AUTO
:
  'auto' -> pushMode (M_Word)
;

BOND_SLAVES
:
  'bond-slaves' -> pushMode(M_Words)
;

BOND_LACP_BYPASS_ALLOW
:
  'bond-lacp-bypass-allow' -> pushMode(M_DropUntilNewline)
;

BRIDGE_PORTS
:
  'bridge-ports' -> pushMode(M_Words)
;

BRIDGE_ACCESS
:
  'bridge-access'
;

BRIDGE_ARP_ND_SUPPRESS
:
  'bridge-arp-nd-suppress' -> pushMode(M_DropUntilNewline)
;

BRIDGE_LEARNING
:
  'bridge-learning' -> pushMode(M_DropUntilNewline)
;

BRIDGE_PVID
:
  'bridge-pvid'
;

BRIDGE_VIDS
:
  'bridge-vids'
;

BRIDGE_VLAN_AWARE
:
  'bridge-vlan-aware'
;

CLAG_ID
:
  'clag-id'
;

CLAGD_BACKUP_IP
:
  'clagd-backup-ip'
;

CLAGD_PEER_IP
:
  'clagd-peer-ip'
;

CLAGD_SYS_MAC
:
  'clagd-sys-mac'
;

HWADDRESS
:
  'hwaddress'
;

IFACE
:
  'iface' -> pushMode(M_Word)
;

LINK_LOCAL
:
  'linklocal'
;

LINK_SPEED
:
  'link-speed'
;

MSTPCTL_BPDUGUARD
:
  'mstpctl-bpduguard' -> pushMode(M_DropUntilNewline)
;

MSTPCTL_PORTADMINEDGE
:
  'mstpctl-portadminedge' -> pushMode(M_DropUntilNewline)
;

MSTPCTL_PORTBPDUFILTER
:
  'mstpctl-portbpdufilter' -> pushMode(M_DropUntilNewline)
;

NO
:
  'no'
;

VLAN_ID
:
  'vlan-id'
;

VLAN_RAW_DEVICE
:
  'vlan-raw-device' -> pushMode(M_Word)
;

VRF
:
  'vrf' -> pushMode(M_Word)
;

VRF_TABLE
:
  'vrf-table' -> pushMode(M_Word)
;

VXLAN_ID
:
  'vxlan-id'
;

VXLAN_LOCAL_TUNNEL_IP
:
  'vxlan-local-tunnelip'
;

YES
:
  'yes'
;

// Complex tokens
BLANK_LINE
:
  (
    F_Whitespace
  )* F_Newline
  {lastTokenType() == NEWLINE|| lastTokenType() == -1}?

  F_Newline* -> channel ( HIDDEN )
;

COMMENT_LINE
:
  (
    F_Whitespace
  )* [#]
  {lastTokenType() == NEWLINE || lastTokenType() == -1}?

  F_NonNewline*
  (
    F_Newline+
    | EOF
  ) -> channel ( HIDDEN )
;

IP_ADDRESS
:
  F_IpAddress
;

IP_PREFIX
:
  F_IpPrefix
;

MAC_ADDRESS
:
  F_MacAddress
;

NEWLINE
:
  F_Newline+
;

NUMBER
:
  F_Digit+
;

WS
:
  F_Whitespace+ -> channel ( HIDDEN ) // parser never sees tokens on hidden channel
;

// Fragments
fragment
F_Digit
:
  [0-9]
;

fragment
F_HexDigit
:
  [0-9A-Fa-f]
;

fragment
F_IpAddress
:
  F_Uint8 '.' F_Uint8 '.' F_Uint8 '.' F_Uint8
;

fragment
F_IpPrefix
:
  F_IpAddress '/' F_IpPrefixLength
;

fragment
F_IpPrefixLength
:
  F_Digit
  | [12] F_Digit
  | [3] [012]
;

fragment
F_MacAddress
:
  F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':'
  F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit
;


fragment
F_Newline
:
  [\n\r]
;

fragment
F_NonNewline
:
  ~[\n\r]
;

fragment
F_NonWhitespace
:
  ~[ \t\u000C\u00A0\n\r]
;


fragment
F_PositiveDigit
:
  [1-9]
;

fragment
F_Uint8
:
  F_Digit
  | F_PositiveDigit F_Digit
  | '1' F_Digit F_Digit
  | '2' [0-4] F_Digit
  | '25' [0-5]
;

fragment
F_Whitespace
:
  ' '
  | '\t'
  | '\u000C'
  | '\u00A0'
  | '\\\n'  // continue on next line
;

fragment
F_Word
:
  F_WordChar+
;

fragment
F_WordChar
:
  [0-9A-Za-z_.:] | '-'
;

mode M_DropUntilNewline;

M_DropUntilNewline_NonNewline
:
  F_NonNewline+ -> channel(HIDDEN)
;

M_DropUntilNewline_Newline
:
  F_Newline+ -> type(NEWLINE), popMode
;

mode M_LineText;

M_LineText_TEXT
:
  F_NonWhitespace F_NonNewline* -> type (TEXT), popMode
;

M_LineText_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Word;

M_Word_WORD
:
  F_Word -> type ( WORD ) , popMode
;

M_Word_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Words;

M_Words_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Words_WORD
:
  F_Word -> type ( WORD )
;

M_Words_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;
