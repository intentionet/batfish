lexer grammar FortiosLexer;

options {
  superClass = 'org.batfish.grammar.fortios.parsing.FortiosBaseLexer';
}

tokens {
  IGNORED_CONFIG_BLOCK,
  QUOTED_TEXT,
  STR_SEPARATOR,
  UNIMPLEMENTED_PLACEHOLDER,
  UNQUOTED_WORD_CHARS
}

// Keyword Tokens

ACCPROFILE: 'accprofile' {
  // ignore config system accprofile
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
ADMIN:
  'admin'
  {
    if (lastTokenType() == REPLACEMSG) {
      pushMode(M_Str);
    } else if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
      // ignore config system admin
      setType(IGNORED_CONFIG_BLOCK);
      pushMode(M_IgnoredConfigBlock);
    }
  }
;
ACCEPT: 'accept';
ACCESS_LIST: 'access-list';
ACTION: 'action';
ADDRESS: 'address';
ADDRESS6: 'address6' {
  // ignore config firewall address6
  if (lastTokenType() == FIREWALL && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
ADDRGRP: 'addrgrp';
AFTER: 'after' -> pushMode(M_SingleStr);
AGGREGATE: 'aggregate';
ALERTMAIL: 'alertmail' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
ALIAS: 'alias' -> pushMode(M_Str);
ALLOW: 'allow';
ALLOW_ROUTING: 'allow-routing';
ANY: 'any';
API_USER: 'api-user' {
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    // ignore config system api-user
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
APPEND: 'append';
AS: 'as' -> pushMode(M_Str);
ASSOCIATED_INTERFACE: 'associated-interface' -> pushMode(M_Str);
AUTH: 'auth' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
AUTOMATION_ACTION: 'automation-action' {
  // ignore config system automation-action
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
AUTOMATION_STITCH: 'automation-stitch' {
  // ignore config system automation-stitch
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
AUTOMATION_TRIGGER: 'automation-trigger' {
  // ignore config system automation-trigger
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
BEFORE: 'before' -> pushMode(M_SingleStr);
BGP: 'bgp';
BUFFER: 'buffer' -> pushMode(M_Str);
CATEGORY: 'category' {
  // ignore config firewall service category
  if (lastTokenType() == SERVICE && secondToLastTokenType() == FIREWALL) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  } else if (lastTokenType() == SET && secondToLastTokenType() == NEWLINE) {
    pushMode(M_Str);
  }
};
CLEAR: 'clear';
CLONE: 'clone' -> pushMode(M_SingleStr);
COLOR: 'color';
COMMENT: 'comment' -> pushMode(M_Str);
COMMENTS: 'comments' -> pushMode(M_Str);
CONFIG: 'config';
COUNTRY: 'country';
CUSTOM: 'custom';
CUSTOM_LANGUAGE: 'custom-language' {
  // ignore config system custom-language
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};

DEFAULT: 'default';
DELETE: 'delete' -> pushMode(M_Str);
DENY: 'deny';
DESCRIPTION: 'description' -> pushMode(M_Str);
DEVICE: 'device' -> pushMode(M_Str);
DISABLE: 'disable';
DISTANCE: 'distance';
DOWN: 'down';
DST: 'dst';
DSTADDR: 'dstaddr' -> pushMode(M_Str);
DSTINTF: 'dstintf' -> pushMode(M_Str);
DYNAMIC: 'dynamic';
EBGP_MULTIPATH: 'ebgp-multipath';
EDIT: 'edit' -> pushMode(M_Str);
EMAC_VLAN: 'emac-vlan';
EMAIL_SERVER: 'email-server' {
  // ignore config system email-server
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
ENABLE: 'enable';
END: 'end';
END_IP: 'end-ip';
EXACT_MATCH: 'exact-match';
EXCLUDE: 'exclude';
EXCLUDE_MEMBER: 'exclude-member' -> pushMode(M_Str);
FABRIC_OBJECT: 'fabric-object';
FIREWALL: 'firewall';
FOLDER: 'folder';
FORTIGUARD_WF: 'fortiguard-wf' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
FQDN: 'fqdn';
FTP: 'ftp' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
GATEWAY: 'gateway';
GEOGRAPHY: 'geography';
GLOBAL: 'global';
GROUP: 'group';
HOSTNAME: 'hostname' -> pushMode(M_Str);
HTTP: 'http' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
IBGP_MULTIPATH: 'ibgp-multipath';
ICAP: 'icap' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
ICMP: 'ICMP';
ICMP6: 'ICMP6';
ICMPCODE: 'icmpcode';
ICMPTYPE: 'icmptype';
INTERFACE: 'interface' -> pushMode(M_Str);
INTERFACE_SUBNET: 'interface-subnet';
INTERNET_SERVICE_ID: 'internet-service-id' -> pushMode(M_Str);
INTERNET_SERVICE_NAME: 'internet-service-name';
INTRAZONE: 'intrazone';
IP: 'ip';
IPMASK: 'ipmask';
IPRANGE: 'iprange';
IP_UPPER: 'IP';
IPSEC: 'ipsec';
LOCATION: 'location';
LOOPBACK: 'loopback';
MAC: 'mac';
MAIL: 'mail' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
MATCH_IP_ADDRESS: 'match-ip-address' -> pushMode(M_Str);
MEMBER: 'member' -> pushMode(M_Str);
MOVE: 'move' -> pushMode(M_SingleStr);
MTU: 'mtu';
MTU_OVERRIDE: 'mtu-override';
MULTICAST_ADDRESS: 'multicast-address' {
  // ignore config firewall multicast-address
  if (lastTokenType() == FIREWALL && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
NAC_QUAR: 'nac-quar' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
NAME: 'name' -> pushMode(M_Str);
NEIGHBOR: 'neighbor';
NETWORK: 'network';
NEXT: 'next';
NP6: 'np6' {
  // ignore config system np6
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
PERMIT: 'permit';
PHYSICAL: 'physical';
POLICY: 'policy';
PREFIX: 'prefix';
PROTOCOL: 'protocol';
PROTOCOL_NUMBER: 'protocol-number';
REDISTRIBUTE: 'redistribute' -> pushMode(M_Str);
REDUNDANT: 'redundant';
REMOTE_AS: 'remote-as' -> pushMode(M_Str);
RENAME: 'rename' -> pushMode(M_SingleStr);
REPLACEMSG: 'replacemsg';
REPLACEMSG_IMAGE: 'replacemsg-image'{
  // ignore config system replacemsg-image
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
ROUTE_MAP: 'route-map';
ROUTE_MAP_IN: 'route-map-in' -> pushMode(M_Str);
ROUTE_MAP_OUT: 'route-map-out' -> pushMode(M_Str);
ROUTER: 'router';
ROUTER_ID: 'router-id';
RULE: 'rule';
SCTP_PORTRANGE: 'sctp-portrange';
SDN: 'sdn';
SDWAN: 'sdwan';
SECONDARY_IP: 'secondary-IP';
SECONDARYIP: 'secondaryip';
SELECT: 'select';
SERVICE:
  'service'
  {
    // After `firewall service`, we expect keywords, not strings
    if (lastTokenType() != FIREWALL) {
      pushMode(M_Str);
    }
  }
;
SESSION_HELPER: 'session-helper' {
  // ignore config system session-helper
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
SET: 'set';
SNMP_INDEX: 'snmp-index';
SPAM: 'spam' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
SRCADDR: 'srcaddr' -> pushMode(M_Str);
SRCINTF: 'srcintf' -> pushMode(M_Str);
SSLVPN: 'sslvpn' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
SSO_ADMIN: 'sso-admin' {
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    // ignore config system sso-admin
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
START_IP: 'start-ip';
STATIC: 'static';
STATUS: 'status';
STORAGE: 'storage' {
  // ignore config system storage
  if (lastTokenType() == SYSTEM && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
SUBNET: 'subnet';
SUB_TYPE: 'sub-type';
SWITCH_CONTROLLER: 'switch-controller' {
  // ignore config switch-controller
  if (lastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
SYSTEM: 'system';
TAGGING: 'tagging';
TCP_PORTRANGE: 'tcp-portrange';
TCP_UDP_SCTP: 'TCP/UDP/SCTP';
TO: 'to' -> pushMode(M_SingleStr);
TRAFFIC_QUOTA: 'traffic-quota' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
TUNNEL: 'tunnel';
TYPE: 'type';
UDP_PORTRANGE: 'udp-portrange';
UNSELECT: 'unselect';
UNSET: 'unset';
UP: 'up';
UPDATE_SOURCE: 'update-source' -> pushMode(M_Str);
UTM: 'utm' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
UUID: 'uuid' -> pushMode(M_Str);
VDOM: 'vdom' -> pushMode(M_Str);
VISIBILITY: 'visibility';
VLAN: 'vlan';
VLANID: 'vlanid';
VRF: 'vrf';
WANOPT: 'wanopt' {
  // ignore config wanopt
  if (lastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
WEBPROXY: 'webproxy' {
  if (lastTokenType() == REPLACEMSG) {
    pushMode(M_Str);
  }
};
WILDCARD: 'wildcard';
WILDCARD_FQDN: 'wildcard-fqdn' {
  // ignore config firewall wildcard-fqdn
  if (lastTokenType() == FIREWALL && secondToLastTokenType() == CONFIG) {
    setType(IGNORED_CONFIG_BLOCK);
    pushMode(M_IgnoredConfigBlock);
  }
};
WL_MESH: 'wl-mesh';
ZONE: 'zone';

// Other Tokens

COLON: ':';

HYPHEN: '-';

COMMENT_LINE
:
  F_Whitespace* '#'
  {lastTokenType() == NEWLINE || lastTokenType() == -1}?
  F_NonNewline* (F_Newline | EOF) -> channel(HIDDEN)
;

DOUBLE_QUOTE: '"' -> pushMode(M_DoubleQuote);

SUBNET_MASK
:
  F_SubnetMask
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

MAC_ADDRESS_LITERAL
:
  F_MacAddress
;

NEWLINE
:
  F_Newline
;

SINGLE_QUOTE: ['] -> pushMode(M_SingleQuote);

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

WS
:
  F_Whitespace+ -> channel ( HIDDEN )
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
F_HexUint32
:
  '0x' F_HexDigit F_HexDigit? F_HexDigit? F_HexDigit? F_HexDigit? F_HexDigit? F_HexDigit? F_HexDigit?
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
  '::' F_Ipv6HexWordLE7
  | F_Ipv6HexWord '::' F_Ipv6HexWordLE6
  | F_Ipv6HexWord2 '::' F_Ipv6HexWordLE5
  | F_Ipv6HexWord3 '::' F_Ipv6HexWordLE4
  | F_Ipv6HexWord4 '::' F_Ipv6HexWordLE3
  | F_Ipv6HexWord5 '::' F_Ipv6HexWordLE2
  | F_Ipv6HexWord6 '::' F_Ipv6HexWordLE1
  | F_Ipv6HexWord7 '::'
  | F_Ipv6HexWord8
;

fragment
F_Ipv6HexWord
:
  F_HexDigit F_HexDigit? F_HexDigit? F_HexDigit?
;

fragment
F_Ipv6HexWord2
:
  F_Ipv6HexWord ':' F_Ipv6HexWord
;

fragment
F_Ipv6HexWord3
:
  F_Ipv6HexWord2 ':' F_Ipv6HexWord
;

fragment
F_Ipv6HexWord4
:
  F_Ipv6HexWord3 ':' F_Ipv6HexWord
;

fragment
F_Ipv6HexWord5
:
  F_Ipv6HexWord4 ':' F_Ipv6HexWord
;

fragment
F_Ipv6HexWord6
:
  F_Ipv6HexWord5 ':' F_Ipv6HexWord
;

fragment
F_Ipv6HexWord7
:
  F_Ipv6HexWord6 ':' F_Ipv6HexWord
;

fragment
F_Ipv6HexWord8
:
  F_Ipv6HexWord6 ':' F_Ipv6HexWordFinal2
;

fragment
F_Ipv6HexWordFinal2
:
  F_Ipv6HexWord2
  | F_IpAddress
;

fragment
F_Ipv6HexWordFinal3
:
  F_Ipv6HexWord ':' F_Ipv6HexWordFinal2
;

fragment
F_Ipv6HexWordFinal4
:
  F_Ipv6HexWord ':' F_Ipv6HexWordFinal3
;

fragment
F_Ipv6HexWordFinal5
:
  F_Ipv6HexWord ':' F_Ipv6HexWordFinal4
;

fragment
F_Ipv6HexWordFinal6
:
  F_Ipv6HexWord ':' F_Ipv6HexWordFinal5
;

fragment
F_Ipv6HexWordFinal7
:
  F_Ipv6HexWord ':' F_Ipv6HexWordFinal6
;

fragment
F_Ipv6HexWordLE1
:
  F_Ipv6HexWord?
;

fragment
F_Ipv6HexWordLE2
:
  F_Ipv6HexWordLE1
  | F_Ipv6HexWordFinal2
;

fragment
F_Ipv6HexWordLE3
:
  F_Ipv6HexWordLE2
  | F_Ipv6HexWordFinal3
;

fragment
F_Ipv6HexWordLE4
:
  F_Ipv6HexWordLE3
  | F_Ipv6HexWordFinal4
;

fragment
F_Ipv6HexWordLE5
:
  F_Ipv6HexWordLE4
  | F_Ipv6HexWordFinal5
;

fragment
F_Ipv6HexWordLE6
:
  F_Ipv6HexWordLE5
  | F_Ipv6HexWordFinal6
;

fragment
F_Ipv6HexWordLE7
:
  F_Ipv6HexWordLE6
  | F_Ipv6HexWordFinal7
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
F_MacAddress
:
  F_HexDigit F_HexDigit F_HexDigit F_HexDigit '.'
  F_HexDigit F_HexDigit F_HexDigit F_HexDigit '.'
  F_HexDigit F_HexDigit F_HexDigit F_HexDigit
;

// Any number of newlines, allowing whitespace in between
fragment
F_Newline
:
  F_NewlineChar (F_Whitespace* F_NewlineChar+)*
;

// A single newline character [sequence - allowing \r, \r\n, or \n]
fragment
F_NewlineChar
:
  '\r' '\n'?
  | '\n'
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
F_StandardCommunity
:
  F_Uint16 ':' F_Uint16
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
F_Whitespace
:
  ' '
  | '\t'
  | '\u000C'
  | '\u00A0'
;

fragment
F_LineContinuation: '\\' F_Newline;

fragment
F_UnquotedEscapedChar: '\\' ~[\n];

fragment
F_QuotedEscapedChar: '\\' ["'\\];

fragment
F_WordChar: ~[ \t\u000C\u00A0\r\n#()<>?'"\\];

mode M_DoubleQuote;

M_DoubleQuote_DOUBLE_QUOTE: '"' -> type(DOUBLE_QUOTE), popMode;

M_DoubleQuote_QUOTED_TEXT: (F_QuotedEscapedChar | ~'"')+ -> type(QUOTED_TEXT);

mode M_SingleQuote;

M_SingleQuote_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), popMode;

M_SingleQuote_QUOTED_TEXT: ~[']+ -> type(QUOTED_TEXT);

mode M_Str;

M_Str_DOUBLE_QUOTE: '"' -> type(DOUBLE_QUOTE), pushMode(M_DoubleQuote);

M_Str_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), pushMode(M_SingleQuote);

M_Str_LINE_CONTINUATION: F_LineContinuation -> skip;

M_Str_UNQUOTED_WORD_CHARS: (F_WordChar | F_UnquotedEscapedChar)+ -> type(UNQUOTED_WORD_CHARS);

M_Str_WS: F_Whitespace+ -> type(STR_SEPARATOR);

M_Str_NEWLINE: F_Newline -> type(NEWLINE), popMode;

mode M_SingleStr;

M_SingleStr_WS: F_Whitespace+ -> type(STR_SEPARATOR), mode(M_SingleStrValue);

M_SingleStr_NEWLINE: F_Newline -> type(NEWLINE), popMode;

mode M_SingleStrValue;

M_SingleStrValue_DOUBLE_QUOTE: '"' -> type(DOUBLE_QUOTE), pushMode(M_DoubleQuote);

M_SingleStrValue_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), pushMode(M_SingleQuote);

M_SingleStrValue_LINE_CONTINUATION: F_LineContinuation -> skip;

M_SingleStrValue_UNQUOTED_WORD_CHARS: (F_WordChar | F_UnquotedEscapedChar)+ -> type(UNQUOTED_WORD_CHARS);

M_SingleStrValue_WS: F_Whitespace+ -> skip, popMode;

M_SingleStrValue_NEWLINE: F_Newline -> type(NEWLINE), popMode;


/////////////////////////////////////////////
/// Mode to ignore an entire config block ///
/////////////////////////////////////////////

// This is the entrance, after `config <something we want to ignore> [maybe more]`. Eat rest of line,
// then start ignoring inner lines.
mode M_IgnoredConfigBlock;

M_IgnoredConfigBlock_REST_OF_LINE: F_NonNewline* F_Newline -> more, mode(M_IgnoredConfigBlockInner);

// We are on some line inside an ignored config block. Eat lines, push if we hit an inner stanza.
mode M_IgnoredConfigBlockInner;

M_IgnoredConfigBlockInner_EDIT: 'edit' F_NonNewline* F_Newline -> more, pushMode(M_IgnoredEditBlock);

M_IgnoredConfigBlockInner_SINGLE_LINE: ('set' | 'unset') F_NonNewline* F_Newline -> more;

M_IgnoredConfigBlockInner_END: 'end' -> type(END), popMode;

M_IgnoredConfigBlockInner_WS: F_Whitespace+ -> more;

// This is the entrance to an edit line
mode M_IgnoredEditBlock;

M_IgnoredEditBlock_CONFIG: 'config' F_NonNewline* F_Newline -> more, pushMode(M_IgnoredInteriorConfigBlockInner);

M_IgnoredEditBlock_SINGLE_LINE: ('set' | 'unset') F_NonNewline* F_Newline -> more;

M_IgnoredEditBlock_NEXT: 'next' F_Whitespace* F_Newline -> more, popMode;

M_IgnoredEditBlock_WS: F_Whitespace+ -> more;

// We are on some line inside an ignored config block (not the outermost on).
// Eat lines, push if we hit an inner stanza.
// This is the same as M_IgnoredConfigBlockInner, except that the END token is also skipped.
mode M_IgnoredInteriorConfigBlockInner;

M_IgnoredInteriorConfigBlockInner_EDIT: 'edit' F_NonNewline* F_Newline -> more, pushMode(M_IgnoredEditBlock);

M_IgnoredInteriorConfigBlockInner_SINGLE_LINE: ('set' | 'unset') F_NonNewline* F_Newline -> more;

M_IgnoredInteriorConfigBlockInner_END: 'end' F_Whitespace* F_Newline -> more, popMode;

M_IgnoredInteriorConfigBlockInner_WS: F_Whitespace+ -> more;
