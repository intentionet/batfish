lexer grammar CumulusFrrLexer;

options {
  superClass = 'org.batfish.grammar.cumulus_frr.parsing.CumulusFrrBaseLexer';
}

tokens {
  QUOTED_TEXT,
  REMARK_TEXT,
  WORD
}

ACCESS_LIST
:
  'access-list' -> pushMode(M_Word)
;

ACTIVATE: 'activate';

ADDRESS: 'address';

ADDRESS_FAMILY: 'address-family';

ADDITIVE: 'additive';

ADMINISTRATIVE: 'administrative';

ADVERTISE: 'advertise';

ADVERTISE_ALL_VNI: 'advertise-all-vni';

ADVERTISE_DEFAULT_GW: 'advertise-default-gw';

AGENTX: 'agentx';

AGGREGATE_ADDRESS: 'aggregate-address';

ALERTS: 'alerts';

ALL: 'all';

ALLOWAS_IN: 'allowas-in';

ALWAYS_COMPARE_MED: 'always-compare-med';

ANY: 'any';

AREA: 'area';

AS_PATH
:
  'as-path'
  {
    if (lastTokenType() == MATCH) {
        pushMode(M_Word);
    }
  }
;

AS_SET: 'as-set';

AUTHENTICATION: 'authentication';


BESTPATH: 'bestpath';

BFD
:
  'bfd' -> pushMode(M_Words)
;


BGP: 'bgp';

CALL
:
  'call' -> pushMode(M_Word)
;

CLUSTER_ID: 'cluster-id';

COLON
:
  ':'
;

COMMANDS: 'commands';

COMMENT_LINE
:
  (
    F_Whitespace
  )* [!]
  {lastTokenType() == NEWLINE || lastTokenType() == -1}?

  F_NonNewline*
  (
    F_Newline+
    | EOF
  ) -> channel ( HIDDEN )
;

COMM_LIST
:
  'comm-list' -> pushMode ( M_Word )
;

COMMUNITY
:
  'community'
  // All other instances are followed by keywords or tokens in default mode
  {
    switch (lastTokenType()) {
      case MATCH:
        pushMode(M_Words);
        break;
      default:
        break;
    }
  }
;

COMMUNITY_LIST: 'community-list';

CONFEDERATION: 'confederation';

CONNECTED: 'connected';

COST: 'cost';

CRITICAL: 'critical';

DATACENTER: 'datacenter';

DEBUGGING: 'debugging';

DEFAULT: 'default';

DEFAULT_ORIGINATE: 'default-originate';

DEFAULTS: 'defaults';

DELETE: 'delete';

DENY: 'deny';

DESCRIPTION
:
  'description' -> pushMode ( M_Remark )
;

DOUBLE_QUOTE
:
  '"' -> pushMode ( M_DoubleQuote )
;

EGP: 'egp';

EIGRP: 'eigrp';

EMERGENCIES: 'emergencies';

ENABLE: 'enable';

END: 'end';

ERRORS: 'errors';

EVPN: 'evpn';

EXIT_ADDRESS_FAMILY: 'exit-address-family';

EXIT_VRF: 'exit-vrf';

EXPANDED
:
  'expanded' -> pushMode(M_Expanded)
;

EXTENDED: 'extended';

EXTERNAL: 'external';

EBGP_MULTIHOP: 'ebgp-multihop';

FILE
:
  'file' -> pushMode(M_Remark)
;

FORCE: 'force';

FORWARDING: 'forwarding';

FRR: 'frr';

GE: 'ge';

GOTO: 'goto';

HOSTNAME
:
  'hostname' -> pushMode(M_Word)
;

IDENTIFIER: 'identifier';

IGP: 'igp';

IMPORT
:
   'import' -> pushMode(M_Import)
;

IN: 'in';

INCOMPLETE: 'incomplete';

INFORMATIONAL: 'informational';

INTEGRATED_VTYSH_CONFIG: 'integrated-vtysh-config';

INTERFACE
:
  'interface'
  {
    switch (lastTokenType()) {
      case MATCH:
      case -1:  // this is the first token in the file
      case NEWLINE: // this is the first token in the line
        pushMode(M_Word);
        break;
      case WORD:
        break;
      default:
        throw new IllegalStateException("unexpected use of keyword interface");
    }
  }
;

INBOUND: 'inbound';

INTERNAL: 'internal';

INTERNET: 'internet';

IP: 'ip';

IPV4: 'ipv4';

IPV4_UNICAST: 'ipv4-unicast';

IPV6: 'ipv6';

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

ISIS: 'isis';

KERNEL: 'kernel';

L2VPN: 'l2vpn';

LE: 'le';

LOCAL_AS
:
  [Ll][Oo][Cc][Aa][Ll]'-'[Aa][Ss]
;

LOCAL_PREFERENCE: 'local-preference';

LOG: 'log';

LOG_ADJACENCY_CHANGES: 'log-adjacency-changes';

LOG_NEIGHBOR_CHANGES: 'log-neighbor-changes';

LINE: 'line';

MAXIMUM_PATHS: 'maximum-paths';

MAX_MED: 'max-med';

MAX_METRIC: 'max-metric';

MESSAGE_DIGEST: 'message-digest';

MESSAGE_DIGEST_KEY: 'message-digest-key';

MD5
:
  'md5' -> pushMode(M_Remark)
;

MULTIPATH_RELAX: 'multipath-relax';

ND: 'nd';

NEIGHBOR
:
  'neighbor' -> pushMode(M_Neighbor)
;

NETWORK: 'network';

NEXT: 'next';

NEXT_HOP_SELF: 'next-hop-self';

NO: 'no';

NO_ADVERTISE: 'no-advertise';

NO_EXPORT: 'no-export';

NO_PREPEND: 'no-prepend';

NOTIFICATIONS: 'notifications';

ON_MATCH: 'on-match';

OSPF: 'ospf';

OUT: 'out';

PASSIVE_INTERFACE
:
  'passive-interface' -> pushMode(M_Default_Or_Word)
;

PASSWORD
:
  'password' -> pushMode(M_Remark)
;

PASSWORD_ENCRYPTION: 'password-encryption';

PEER_GROUP
:
  'peer-group' -> pushMode(M_PeerGroup)
;

PERMIT: 'permit';

POINT_TO_POINT: 'point-to-point';

PREFIX_LEN: 'prefix-len';

PREFIX_LIST
:
  'prefix-list' -> pushMode ( M_Word )
;

PREPEND: 'prepend';

RA_INTERVAL: 'ra-interval';

RANGE: 'range';

REDISTRIBUTE: 'redistribute';

REMOTE_AS: 'remote-as';

REPLACE_AS: 'replace-as';

RIP: 'rip';

ROUTE_MAP
:
  'route-map' -> pushMode(M_Word)
;

ROUTE
:
  'route' -> pushMode(M_Static_Route_Next_Hop)
;

ROUTER: 'router';

ROUTER_ID: 'router-id';

SEND_COMMUNITY: 'send-community';

SET: 'set';

SEQ: 'seq';

SERVICE: 'service';

SHUTDOWN: 'shutdown';

SOFT_RECONFIGURATION: 'soft-reconfiguration';

SOURCE_PROTOCOL: 'source-protocol';

STANDARD
:
  'standard' -> pushMode ( M_Word )
;

STATIC: 'static';

SUMMARY_ONLY: 'summary-only';

SUPPRESS_MAP: 'suppress-map' -> pushMode(M_Word);

SUPPRESS_RA: 'suppress-ra';

SYSLOG: 'syslog';

TAG: 'tag';

TRADITIONAL: 'traditional';

TYPE_1: 'type-1';
TYPE_2: 'type-2';

WARNINGS: 'warnings';

MATCH: 'match';

MATCHING_MED_ONLY: 'matching-' [Mm][Ee][Dd] '-only';

METRIC: 'metric';

METRIC_TYPE: 'metric-type';

NEWLINE
:
  F_Newline+
;

NEXT_HOP: 'next-hop';

ORIGIN: 'origin';

ROUTE_REFLECTOR_CLIENT: 'route-reflector-client';

ROUTER_LSA: 'router-lsa';

SUBNET_MASK
:
  F_SubnetMask
;

UINT8
:
  F_Uint8
;

UINT16
:
  F_Uint16
;

UINT32
:
  F_Uint32
;

UPDATE_SOURCE
:
  'update-source' -> pushMode(M_Update_Source)
;

USERNAME
:
  'username' -> pushMode(M_Words)
;

UNICAST: 'unicast';

DEC
:
  F_Digit+
;

DETAIL: 'detail';

VERSION
:
  'version' -> pushMode(M_Remark)
;

VNI: 'vni';

VRF
:
  'vrf' -> pushMode(M_Word)
;

VTY: 'vty';

WEIGHT: 'weight';

WS
:
  F_Whitespace+ -> channel ( HIDDEN ) // parser never sees tokens on hidden channel

;

// Complex tokens

BLANK_LINE
:
  F_Whitespace* F_Newline+
  {lastTokenType() == NEWLINE|| lastTokenType() == -1}?
    -> channel ( HIDDEN )
;

DASH: '-';

PLUS
:
   '+'
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
F_SubnetMask
:
  F_SubnetMaskOctet '.0.0.0'
  | '255.' F_SubnetMaskOctet '.0.0'
  | '255.255.' F_SubnetMaskOctet '.0'
  | '255.255.255.' F_SubnetMaskOctet
;

fragment
F_SubnetMaskOctet
:
  '0'
  | '128'
  | '192'
  | '224'
  | '240'
  | '248'
  | '252'
  | '254'
  | '255'
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
F_Uint32
:
// 0-4294967295
  F_Digit
  | F_PositiveDigit F_Digit F_Digit? F_Digit? F_Digit? F_Digit? F_Digit?
  F_Digit? F_Digit?
  | [1-3] F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit
  F_Digit
  | '4' [0-1] F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit
  | '42' [0-8] F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit
  | '429' [0-3] F_Digit F_Digit F_Digit F_Digit F_Digit F_Digit
  | '4294' [0-8] F_Digit F_Digit F_Digit F_Digit F_Digit
  | '42949' [0-5] F_Digit F_Digit F_Digit F_Digit
  | '429496' [0-6] F_Digit F_Digit F_Digit
  | '4294967' [0-1] F_Digit F_Digit
  | '42949672' [0-8] F_Digit
  | '429496729' [0-5]
;

fragment
F_Word
:
  F_WordChar+
;

fragment
F_WordChar
:
  [0-9A-Za-z!@#$^*_=+.;:{}]
  | '-'
;

fragment
F_Newline
:
  [\n\r] // carriage return or line feed

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
F_Whitespace
:
  ' '
  | '\t'
  | '\u000C'
  | '\u00A0'
;

// modes
mode M_Default_Or_Word;

M_Default_Or_Word_DEFAULT
:
  'default' -> type (DEFAULT) , popMode
;


M_Default_Or_Word_WORD
:
  F_Word -> type ( WORD ) , popMode
;

M_Default_Or_Word_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_DoubleQuote;

M_DoubleQuote_DOUBLE_QUOTE
:
  '"' -> type ( DOUBLE_QUOTE ) , popMode
;

M_DoubleQuote_NEWLINE
:
// Break out if termination does not occur on same line
  F_Newline+ -> type ( NEWLINE ) , popMode
;

M_DoubleQuote_QUOTED_TEXT
:
  ~["\r\n]+ -> type ( QUOTED_TEXT )
;

mode M_Expanded;

M_Expanded_WORD
:
  F_Word -> type ( WORD ) , mode(M_Expanded2)
;

M_Expanded_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Expanded2;

M_Expanded2_DENY
:
  'deny' -> type ( DENY ), mode(M_Expanded3)
;

M_Expanded2_PERMIT
:
  'permit' -> type ( PERMIT ), mode(M_Expanded3)
;

M_Expanded2_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Expanded3;

M_Expanded3_WS
:
  F_Whitespace+ -> channel ( HIDDEN ), mode(M_Expanded4)
;

mode M_Expanded4;

M_Expanded4_DOUBLE_QUOTE
:
  '"' -> type(DOUBLE_QUOTE), mode(M_DoubleQuote)
;

M_Expanded4_REMARK_TEXT
:
  ~["\r\n] F_NonWhitespace* (F_Whitespace+ F_NonWhitespace+)* -> type(REMARK_TEXT), popMode
;


mode M_Static_Route_Next_Hop;
// Parsing for static routes in the format of 'ip route 1.1.1.1/32 (eth0|2.2.2.2)

M_Static_Route_IP_Prefix
:
  F_IpPrefix -> type(IP_PREFIX)
;

M_Static_Route_IP_Address
:
  F_IpAddress -> type(IP_ADDRESS) , popMode
;

M_Static_Route_Word
:
  F_Word -> type(WORD) , popMode
;

M_Static_Route_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;


mode M_Import;

M_Import_VRF
:
   'vrf' -> type ( VRF ), mode(M_ImportVrf)
;

M_Import_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Import_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;


mode M_ImportVrf;

M_ImportVrf_ROUTE_MAP
:
   'route-map' -> type ( ROUTE_MAP ), mode(M_Word)
;

M_ImportVrf_WORD
:
   F_Word -> type ( WORD ), popMode
;

M_ImportVrf_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ) , popMode
;

M_ImportVrf_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;


mode M_Neighbor;

M_Neighbor_IP_Address
:
  F_IpAddress -> type(IP_ADDRESS) , popMode
;

M_Neighbor_IPV6_Address
:
  F_Ipv6Address -> type(IPV6_ADDRESS) , popMode
;

M_Neighbor_Word
:
  F_Word -> type(WORD) , popMode
;

M_Neighbor_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_PeerGroup;

M_Newline
:
  F_Newline -> type(NEWLINE), popMode
;

M_PeerGroup_Word
:
  F_Word -> type(WORD) , popMode
;

M_PeerGroup_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;


mode M_Word;

M_Word_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ) , popMode
;

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

mode M_Remark;

M_Remark_REMARK_TEXT
:
  F_NonWhitespace F_NonNewline* -> type ( REMARK_TEXT )
;

M_Remark_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ), popMode
;

M_Remark_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Update_Source;

M_Update_Source_IP_Address
:
  F_IpAddress -> type(IP_ADDRESS) , popMode
;

M_Update_Source_Word
:
  F_Word -> type(WORD) , popMode
;

M_Update_Source_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

M_Update_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ), popMode
;

