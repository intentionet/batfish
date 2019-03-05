lexer grammar F5BigipStructuredLexer;

options {
  superClass = 'org.batfish.grammar.BatfishLexer';
}

@members {
// Java code to end up in F5BigipStructuredLexer.java goes here

private int lastTokenType = -1;

@Override
public void emit(Token token) {
    super.emit(token);
    if (token.getChannel() != HIDDEN) {
       lastTokenType = token.getType();
    }
}

}

// Keywords

ACTION
:
  'action'
;

ACTIVATE
:
  'activate'
;

ADDRESS
:
  'address'
;

ADDRESS_FAMILY
:
  'address-family'
;

ALL
:
  'all'
;

ALLOW_SERVICE
:
  'allow-service'
;

ALWAYS
:
  'always'
;

ANY
:
  'any'
;

ARP
:
  'arp'
;

BGP
:
  'bgp'
;

BUNDLE
:
  'bundle'
;

BUNDLE_SPEED
:
  'bundle-speed'
;

CLIENT_SSL
:
  'client-ssl'
;

COMMUNITY
:
  'community'
;

DEFAULTS_FROM
:
  'defaults-from'
;

DENY
:
  'deny'
;

DESCRIPTION
:
  'description'
;

DESTINATION
:
  'destination'
;

DISABLED
:
  'disabled'
;

ENABLED
:
  'enabled'
;

ENTRIES
:
  'entries'
;

FORTY_G
:
  '40G'
;

GLOBAL_SETTINGS
:
  'global-settings'
;

HOSTNAME
:
  'hostname'
;

HTTP
:
  'http'
;

HTTPS
:
  'https'
;

IF
:
  'if'
;

INTERFACE
:
  'interface'
;

INTERFACES
:
  'interfaces'
;

IP_PROTOCOL
:
  'ip-protocol'
;

IPV4
:
  'ipv4'
;

IPV6
:
  'ipv6'
;

KERNEL
:
  'kernel'
;

LOCAL_AS
:
  'local-as'
;

LTM
:
  'ltm'
;

MASK
:
  'mask'
;

MATCH
:
  'match'
;

MEMBERS
:
  'members'
;

MONITOR
:
  'monitor'
;

NEIGHBOR
:
  'neighbor'
;

NET
:
  'net'
;

NODE
:
  'node'
;

OCSP_STAPLING_PARAMS
:
  'ocsp-stapling-params'
;

ONE_CONNECT
:
  'one-connect'
;

ONE_HUNDRED_G
:
  '100G'
;

ORIGINS
:
  'origins'
;

OUT
:
  'out'
;

PERMIT
:
  'permit'
;

PERSIST
:
  'persist'
;

PERSISTENCE
:
  'persistence'
;

POOL
:
  'pool'
;

PREFIX
:
  'prefix'
;

PREFIX_LEN_RANGE
:
  'prefix-len-range'
;

PREFIX_LIST
:
  'prefix-list'
;

PROFILE
:
  'profile'
;

PROFILES
:
  'profiles'
;

REDISTRIBUTE
:
  'redistribute'
;

REMOTE_AS
:
  'remote-as'
;

ROUTE_ADVERTISEMENT
:
  'route-advertisement'
;

ROUTE_DOMAIN
:
  'route-domain'
;

ROUTE_MAP
:
  'route-map'
;

ROUTER_ID
:
  'router-id'
;

ROUTING
:
  'routing'
;

RULE
:
  'rule'
;

RULES
:
  'rules'
;

SELECTIVE
:
  'selective'
;

SELF
:
  'self'
;

SERVER_SSL
:
  'server-ssl'
;

SET
:
  'set'
;

SNAT
:
  'snat'
;

SNAT_TRANSLATION
:
  'snat-translation'
;

SNATPOOL
:
  'snatpool'
;

SOURCE
:
  'source'
;

SOURCE_ADDR
:
  'source-addr'
;

SOURCE_ADDRESS_TRANSLATION
:
  'source-address-translation'
;

SSL
:
  'ssl'
;

SSL_PROFILE
:
  'ssl-profile'
;

SYS
:
  'sys'
;

TAG
:
  'tag'
;

TCP
:
  'tcp'
;

TRAFFIC_GROUP
:
  'traffic-group'
;

TRANSLATE_ADDRESS
:
  'translate-address'
;

TRANSLATE_PORT
:
  'translate-port'
;

TYPE
:
  'type'
;

UDP
:
  'udp'
;

UPDATE_SOURCE
:
  'update-source'
;

VALUE
:
  'value'
;

VIRTUAL
:
  'virtual'
;

VIRTUAL_ADDRESS
:
  'virtual-address'
;

VLAN
:
  'vlan'
;

VLANS
:
  'vlans'
;

VLANS_DISABLED
:
  'vlans-disabled'
;

VLANS_ENABLED
:
  'vlans-enabled'
;

// Complex tokens

BRACE_LEFT
:
  '{'
;

BRACE_RIGHT
:
  '}'
;

BRACKET_LEFT
:
  '['
;

BRACKET_RIGHT
:
  ']'
;

COMMENT_LINE
:
  (
    F_Whitespace
  )* '#'
  {lastTokenType == NEWLINE || lastTokenType == -1}?

  F_NonNewlineChar* F_Newline+ -> channel ( HIDDEN )
;

COMMENT_TAIL
:
  '#' F_NonNewlineChar* -> channel ( HIDDEN )
;

DOUBLE_QUOTED_STRING
:
  '"' ~'"'* '"'
;

NEWLINE
:
  F_Newline+
;

SEMICOLON
:
  ';' -> channel ( HIDDEN )
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
F_Whitespace
:
  [ \t\u000C] // tab or space or unicode 0x000C

;

fragment
F_WordChar
:
  ~[ \t\n\r{}[\]]
;
