lexer grammar CumulusNcluLexer;

options {
  superClass = 'org.batfish.grammar.BatfishLexer';
}

tokens {
  EXTRA_CONFIGURATION_FOOTER,
  USERNAME
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

// Keywords

ACCESS
:
  'access'
;

ACTIVATE
:
  'activate'
;

ADD
:
  'add'
;

ADDRESS
:
  'address'
;

ADDRESS_VIRTUAL
:
  'address-virtual'
;

ADVERTISE
:
  'advertise'
;

ADVERTISE_ALL_VNI
:
  'advertise-all-vni'
;

ADVERTISE_DEFAULT_GW
:
  'advertise-default-gw'
;

ALERTS
:
  'alerts'
;

ARP_ND_SUPPRESS
:
  'arp-nd-suppress'
;

AUTO
:
  'auto'
;

AUTONOMOUS_SYSTEM
:
  'autonomous-system'
;

BACKUP_IP
:
  'backup-ip'
;

BGP
:
  'bgp'
;

BOND
:
  'bond'
;

BPDUGUARD
:
  'bpduguard'
;

BRIDGE
:
  'bridge'
;

CLAG
:
  'clag'
;

COMMIT
:
  'commit'
;

CONNECTED
:
  'connected'
;

CRITICAL
:
  'critical'
;

DATACENTER
:
  'datacenter'
;

DEBUGGING
:
  'debugging'
;

DEFAULTS
:
  'defaults'
;

DEL
:
  'del'
;

DENY
:
  'deny'
;

DESCRIPTION
:
  'description'
;

DNS
:
  'dns'
;

DOT1X
:
  'dot1x'
;

EMERGENCIES
:
  'emergencies'
;

ERRORS
:
  'errors'
;

EVPN
:
  'evpn'
;

EXIT_VRF
:
  'exit-vrf'
;

EXTERNAL
:
  'external'
;

HOSTNAME
:
  'hostname'
;

HWADDRESS
:
  'hwaddress'
;

IBURST
:
  'iburst'
;

ID
:
  'id'
;

INFORMATIONAL
:
  'informational'
;

INTEGRATED_VTYSH_CONFIG
:
  'integrated-vtysh-config'
;

INTERFACE
:
  'interface'
;

INTERNAL
:
  'internal'
;

IP
:
  'ip'
;

IPV4
:
  'ipv4'
;

IPV6
:
  'ipv6'
;

L2VPN
:
  'l2vpn'
;

LEARNING
:
  'learning'
;

LINK
:
  'link'
;

LINKLOCAL
:
  'linklocal'
;

LO
:
  'lo'
;

LOCAL_TUNNELIP
:
  'local-tunnelip'
;

LOG
:
  'log'
;

LOOPBACK
:
  'loopback'
;

MATCH
:
  'match'
;

NAMESERVER
:
  'nameserver'
;

NEIGHBOR
:
  'neighbor'
;

NETWORK
:
  'network'
;

NET
:
  'net'
;

NOTIFICATIONS
:
  'notifications'
;

NTP
:
  'ntp'
;

OFF
:
  'off'
;

ON
:
  'on'
;

PEER_GROUP
:
  'peer-group'
;

PEER_IP
:
  'peer-ip'
;

PERMIT
:
  'permit'
;

PORTBPDUFILTER
:
  'portbpdufilter'
;

PORTS
:
  'ports'
;

PRIORITY
:
  'priority'
;

PTP
:
  'ptp'
;

PVID
:
  'pvid'
;

REDISTRIBUTE
:
  'redistribute'
;

REMOTE_AS
:
  'remote-as'
;

ROUTE
:
  'route'
;

ROUTE_MAP
:
  'route-map'
;

ROUTE_REFLECTOR_CLIENT
:
  'route-reflector-client'
;

ROUTER_ID
:
  'router-id'
;

ROUTING
:
  'routing'
;

SERVER
:
  'server'
;

SERVICE
:
  'service'
;

SLAVES
:
  'slaves'
;

SNMP_SERVER
:
  'snmp-server'
;

SOURCE
:
  'source'
;

STATIC
:
  'static'
;

STP
:
  'stp'
;

SPEED
:
  'speed'
;

SYS_MAC
:
  'sys-mac'
;

SYSLOG
:
  'syslog'
;

TIME
:
  'time'
;

UNICAST
:
  'unicast'
;

VIDS
:
  'vids'
;

VLAN
:
  'vlan'
;

VLAN_AWARE
:
  'vlan-aware'
;

VLAN_ID
:
  'vlan-id'
;

VLAN_RAW_DEVICE
:
  'vlan-raw-device'
;

VNI
:
  'vni'
;

VRF
:
  'vrf'
;

VRF_TABLE
:
  'vrf-table'
;

VXLAN
:
  'vxlan'
;

VXLAN_ANYCAST_IP
:
  'vxlan-anycast-ip'
;

WARNINGS
:
  'warnings'
;

ZONE
:
  'zone'
;

// Complex tokens

EXTRA_CONFIGURATION_HEADER
:
  'sudo sh -c "printf \'' -> pushMode ( M_Printf )
;

COMMA
:
  ','
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

DASH
:
  '-'
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

MAC_ADDRESS
:
  F_MacAddress
;

NEWLINE
:
  F_Newline+
;

NUMBERED_WORD
:
  F_NumberedWord
;

// Do NOT move above NUMBERED_WORD

WORD
:
  F_Word
;

WS
:
  F_Whitespace+ -> channel ( HIDDEN ) // parser never sees tokens on hidden channel

;

// Fragments

fragment
F_Alpha
:
  [A-Za-z]
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
F_MacAddress
:
  F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':'
  F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit
;

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
F_NonWhitespaceChar
:
  ~[\r\n \t\u000C]
;

fragment
F_NumberedWord
:
  F_Word F_Uint32
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
F_Uint32
:
// 0-4294967295
  F_Digit
  | F_PositiveDigit F_Digit F_Digit? F_Digit? F_Digit? F_Digit? F_Digit?
  F_Digit? F_Digit? F_Digit?
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
  [ \t\u000C] // tab or space or unicode 0x000C

;

fragment
F_Word
:
  F_WordSegment
  (
    (
      '-' F_WordSegment
    )*
    | F_Digit+
  )
;

fragment
F_WordChar
:
  ~( [ \t\n\r{}[\],\\] | '-' )
;

fragment
F_WordSegment
:
  F_Alpha F_WordChar*
  | F_Digit F_WordChar* F_Alpha F_WordChar*
;

// Lexer Modes
mode M_Printf;

// FRR in printf keywords

M_Printf_EXIT_VRF
:
  'exit-vrf' -> type ( EXIT_VRF )
;

M_Printf_IP
:
  'ip' -> type ( IP )
;

M_Printf_ROUTE
:
  'route' -> type ( ROUTE )
;

M_Printf_VRF
:
  'vrf' -> type ( VRF )
;

// FRR in printf complex tokens

M_Printf_EXTRA_CONFIGURATION_FOOTER
:
  '\' >> /etc/frr/frr.conf"' -> type ( EXTRA_CONFIGURATION_FOOTER ) , popMode
;

M_Printf_IP_ADDRESS
:
  F_IpAddress -> type ( IP_ADDRESS )
;

M_Printf_IP_PREFIX
:
  F_IpPrefix -> type ( IP_PREFIX )
;

M_Printf_NEWLINE
:
  '\\n' -> type ( NEWLINE )
;

M_Printf_USERNAME
:
  'username' -> type ( USERNAME )
;

M_Printf_WORD
:
  F_Word -> type ( WORD )
;

M_Printf_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;
