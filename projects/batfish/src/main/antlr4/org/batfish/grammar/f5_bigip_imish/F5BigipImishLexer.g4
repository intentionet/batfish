lexer grammar F5BigipImishLexer;

options {
  superClass = 'org.batfish.grammar.BatfishLexer';
}

@members {
// Java code to end up in F5BigipImishLexer.java goes here

private int lastTokenType = -1;

@Override
public void emit(Token token) {
    super.emit(token);
    if (token.getChannel() != HIDDEN) {
       lastTokenType = token.getType();
    }
}

}

tokens {
  DESCRIPTION_LINE
}

// Keywords

ACCESS_LIST
:
  'access-list'
;

ADDRESS
:
  'address'
;

ADVERTISEMENT_INTERVAL
:
  'advertisement-interval'
;

AGGREGATE_ADDRESS
:
  'aggregate-address'
;

ALWAYS_COMPARE_MED
:
  'always-compare-med'
;

ANY
:
  'any'
;

AREA
:
  'area'
;

AS_SET
:
  'as-set'
;

BFD
:
  'bfd'
;

BGP
:
  'bgp'
;

CAPABILITY
:
  'capability'
;

COMMUNITY
:
  'community'
;

CON
:
  'con'
;

CONFEDERATION
:
  'confederation'
;

DENY
:
  'deny'
;

DESCRIPTION
:
  'description' -> pushMode ( M_Description )
;

DETERMINISTIC_MED
:
  'deterministic-med'
;

EBGP
:
  'ebgp'
;

EGP
:
  'egp'
;

END
:
  'end'
;

FALL_OVER
:
  'fall-over'
;

GE
:
  'ge'
;

GRACEFUL_RESTART
:
  'graceful-restart'
;

IDENTIFIER
:
  'identifier'
;

IGP
:
  'igp'
;

IN
:
  'in'
;

INCOMPLETE
:
  'incomplete'
;

INTERFACE
:
  'interface'
;

IP
:
  'ip'
;

KERNEL
:
  'kernel'
;

LE
:
  'le'
;

LINE
:
  'line'
;

LOG_NEIGHBOR_CHANGES
:
  'log-neighbor-changes'
;

LOGIN
:
  'login'
;

MATCH
:
  'match'
;

MAX_PATHS
:
  'max-paths'
;

MAXIMUM_PREFIX
:
  'maximum-prefix'
;

METRIC
:
  'metric'
;

NEIGHBOR
:
  'neighbor'
;

NETWORK
:
  'network'
;

NEXT_HOP
:
  'next-hop'
;

NEXT_HOP_SELF
:
  'next-hop-self'
;

NO
:
  'no'
;

NON_BROADCAST
:
  'non-broadcast'
;

ORIGIN
:
  'origin'
;

OSPF
:
  'ospf'
;

OUT
:
  'out'
;

PASSIVE_INTERFACE
:
  'passive-interface'
;

PASSWORD
:
  'password' -> pushMode ( M_Description )
;

PRIMARY
:
  'primary'
;

PEER_GROUP
:
  'peer-group'
;

PEERS
:
  'peers'
;

PERMIT
:
  'permit'
;

PREFIX_LIST
:
  'prefix-list'
;

REDISTRIBUTE
:
  'redistribute'
;

REMOTE_AS
:
  'remote-as'
;

ROUTE_MAP
:
  'route-map'
;

ROUTER
:
  'router'
;

ROUTER_ID
:
  'router-id'
;

SEQ
:
  'seq'
;

SERVICE
:
  'service'
;

SET
:
  'set'
;

SUMMARY_ONLY
:
  'summary-only'
;

UPDATE_SOURCE
:
  'update-source'
;

VTY
:
  'vty'
;

// Complex tokens

COMMENT_LINE
:
  (
    F_Whitespace
  )* '!'
  {lastTokenType == NEWLINE || lastTokenType == -1}?

  F_NonNewlineChar* F_Newline+ -> channel ( HIDDEN )
;

COMMENT_TAIL
:
  '!' F_NonNewlineChar* -> channel ( HIDDEN )
;

CONNECTED
:
  'connected'
;

DEC
:
  F_Digit+
;

IP_ADDRESS
:
  F_IpAddress
;

IP_PREFIX
:
  F_IpPrefix
;

IPV6_ADDRESS
:
  F_Ipv6Address
;

IPV6_PREFIX
:
  F_Ipv6Prefix
;

NEWLINE
:
  F_Newline+
;

STANDARD_COMMUNITY
:
  F_StandardCommunity
;

WORD
:
  F_WordChar+
;

WS
:
  F_Whitespace+ -> channel ( HIDDEN ) // parser never sees tokens on hidden channel

;

// Fragments

fragment
F_Newline
:
  [\r\n] // carriage return or line feed

;

fragment
F_NonNewlineChar
:
  ~[\r\n] // carriage return or line feed

;

fragment
F_DecByte
:
  F_Digit
  | F_PositiveDigit F_Digit
  | '1' F_Digit F_Digit
  | '2' [0-4] F_Digit
  | '25' [0-5]
;

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
F_HexWord
:
  F_HexDigit F_HexDigit? F_HexDigit? F_HexDigit?
;

fragment
F_HexWord2
:
  F_HexWord ':' F_HexWord
;

fragment
F_HexWord3
:
  F_HexWord2 ':' F_HexWord
;

fragment
F_HexWord4
:
  F_HexWord3 ':' F_HexWord
;

fragment
F_HexWord5
:
  F_HexWord4 ':' F_HexWord
;

fragment
F_HexWord6
:
  F_HexWord5 ':' F_HexWord
;

fragment
F_HexWord7
:
  F_HexWord6 ':' F_HexWord
;

fragment
F_HexWord8
:
  F_HexWord6 ':' F_HexWordFinal2
;

fragment
F_HexWordFinal2
:
  F_HexWord2
  | F_IpAddress
;

fragment
F_HexWordFinal3
:
  F_HexWord ':' F_HexWordFinal2
;

fragment
F_HexWordFinal4
:
  F_HexWord ':' F_HexWordFinal3
;

fragment
F_HexWordFinal5
:
  F_HexWord ':' F_HexWordFinal4
;

fragment
F_HexWordFinal6
:
  F_HexWord ':' F_HexWordFinal5
;

fragment
F_HexWordFinal7
:
  F_HexWord ':' F_HexWordFinal6
;

fragment
F_HexWordLE1
:
  F_HexWord?
;

fragment
F_HexWordLE2
:
  F_HexWordLE1
  | F_HexWordFinal2
;

fragment
F_HexWordLE3
:
  F_HexWordLE2
  | F_HexWordFinal3
;

fragment
F_HexWordLE4
:
  F_HexWordLE3
  | F_HexWordFinal4
;

fragment
F_HexWordLE5
:
  F_HexWordLE4
  | F_HexWordFinal5
;

fragment
F_HexWordLE6
:
  F_HexWordLE5
  | F_HexWordFinal6
;

fragment
F_HexWordLE7
:
  F_HexWordLE6
  | F_HexWordFinal7
;

fragment
F_IpAddress
:
  F_DecByte '.' F_DecByte '.' F_DecByte '.' F_DecByte
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
F_Ipv6Address
:
  '::' F_HexWordLE7
  | F_HexWord '::' F_HexWordLE6
  | F_HexWord2 '::' F_HexWordLE5
  | F_HexWord3 '::' F_HexWordLE4
  | F_HexWord4 '::' F_HexWordLE3
  | F_HexWord5 '::' F_HexWordLE2
  | F_HexWord6 '::' F_HexWordLE1
  | F_HexWord7 '::'
  | F_HexWord8
;

fragment
F_Ipv6Prefix
:
  F_Ipv6Address '/' F_Ipv6PrefixLength
;

fragment
F_Ipv6PrefixLength
:
  F_Digit
  | F_PositiveDigit F_Digit
  | '1' [01] F_Digit
  | '12' [0-8]
;

fragment
F_NonWhitespaceChar
:
  ~[\r\n \t\u000C]
;

fragment
F_PositiveDigit
:
  '1' .. '9'
;

fragment
F_StandardCommunity
:
  F_Uint16 ':' F_Uint16
;

fragment
F_Uint16
:
  F_Digit
  | F_PositiveDigit F_Digit F_Digit? F_Digit?
  | [1-5] F_Digit F_Digit F_Digit F_Digit
  | '6' [0-4] F_Digit F_Digit F_Digit
  | '65' [0-4] F_Digit F_Digit
  | '655' [0-2] F_Digit
  | '6553' [0-5]
;

fragment
F_Whitespace
:
  [ \t\u000C] // tab or space or unicode 0x000C

;

fragment
F_WordChar
:
  ~[ \t\n\r{}[\]]
;

// Lexer modes
mode M_Description;

M_Description_LINE
:
  F_NonWhitespaceChar F_NonNewlineChar* -> type ( DESCRIPTION_LINE ) , popMode
;

M_Description_WS
:
  F_Whitespace+ -> channel ( HIDDEN ) // parser never sees tokens on hidden channel

;

