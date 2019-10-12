lexer grammar PaloAltoLexer;

options {
  superClass = 'org.batfish.grammar.BatfishLexer';
}

tokens {
  WORD
}

@members {
// Java code to end up in PaloAltoLexer.java goes here
}

// Keywords

TWO_BYTE
:
    '2-byte'
;

FOUR_BYTE
:
    '4-byte'
;

ACCEPT_SUMMARY
:
    'accept-summary'
;

ACTION
:
    'action'
;

ADDRESS
:
    'address'
;

ADDRESS_GROUP
:
    'address-group'
;

ADMIN_DIST
:
    'admin-dist'
;

ADMIN_DISTS
:
    'admin-dists'
;

ADVERTISE
:
    'advertise'
;

AES_128_CBC
:
    'aes-128-cbc'
;

AES_128_GCM
:
    'aes-128-gcm'
;

AES_192_CBC
:
    'aes-192-cbc'
;

AES_256_CBC
:
    'aes-256-cbc'
;

AES_256_GCM
:
    'aes-256-gcm'
;

AGGREGATE
:
    'aggregate'
;

AGGREGATE_MED
:
    'aggregate-med'
;

ALLOW
:
    'allow'
;

ALWAYS_COMPARE_MED
:
    'always-compare-med'
;

ANY
:
    'any'
;

APPLICATION
:
    'application'
;

APPLICATION_FILTER
:
    'application-filter'
;

APPLICATION_GROUP
:
    'application-group'
;

AREA
:
    'area'
;

AS_FORMAT
:
    'as-format'
;

AUTH_PROFILE
:
    'auth-profile'
;

AUTHENTICATION
:
    'authentication'
;

AUTHENTICATION_PROFILE
:
    'authentication-profile'
;

AUTHENTICATION_TYPE
:
    'authentication-type'
;

AUTO
:
    'auto'
;

BFD
:
    'bfd'
;

BGP
:
    'bgp'
;

BOTNET
:
    'botnet'
;

BROADCAST
:
    'broadcast'
;

CATEGORY
:
    'category'
;

CERTIFICATE
:
    'certificate'
;

CERTIFICATE_PROFILE
:
    'certificate-profile'
;

CLIENT
:
    'client'
;

COLOR
:
    'color'
;

COMMENT
:
    'comment'
;

COMMENTS
:
    'comments'
;

CONFIG
:
    'config'
;

CONNECTION_OPTIONS
:
    'connection-options'
;

CONTENT_PREVIEW
:
    'content-preview'
;

CRYPTO_PROFILES
:
    'crypto-profiles'
;

DAMPENING_PROFILE
:
    'dampening-profile'
;

DAYS
:
    'days'
;

DEAD_COUNTS
:
    'dead-counts'
;

DEFAULT_GATEWAY
:
    'default-gateway'
;

DEFAULT_LOCAL_PREFERENCE
:
    'default-local-preference'
;

DEFAULT_ROUTE
:
    'default-route'
;

DENY
:
    'deny'
;

DES
:
    'des'
;

DESCRIPTION
:
    'description'
;

DESTINATION
:
    'destination'
;

DETERMINISTIC_MED_COMPARISON
:
    'deterministic-med-comparison'
;

DEVICES
:
    'devices'
;

DEVICECONFIG
:
    'deviceconfig'
;

DH_GROUP
:
    'dh-group'
;

DISABLE
:
    'disable'
;

DISABLED
:
    'disabled'
;

DISPLAY_NAME
:
    'display-name'
;

DNS
:
    'dns'
;

DNS_SETTING
:
    'dns-setting'
;

DOWN
:
    'down'
;

DROP
:
    'drop'
;

DYNAMIC
:
    'dynamic'
;

EBGP
:
    'ebgp'
;

ENABLE
:
    'enable'
;

ENCRYPTION
:
    'encryption'
;

ESP
:
    'esp'
;

EVASIVE
:
    'evasive'
;

EXCESSIVE_BANDWIDTH_USE
:
    'excessive-bandwidth-use'
;

EXPORT
:
    'export'
;

EXT_1
:
    'ext-1'
;

EXT_2
:
    'ext-2'
;

EXTERNAL
:
    'external'
;

ETHERNET
:
    'ethernet'
;

FILTER
:
    'filter'
;

FQDN
:
    'fqdn'
;

FROM
:
    'from'
;

GATEWAY
:
    'gateway'
;

GLOBAL_BFD
:
    'global-bfd'
;

GLOBAL_PROTECT_APP_CRYPTO_PROFILES
:
    'global-protect-app-crypto-profiles'
;

GR_DELAY
:
    'gr-delay'
;

GRACEFUL_RESTART
:
    'graceful-restart'
;

GROUP1
:
    'group1'
;

GROUP2
:
    'group2'
;

GROUP5
:
    'group5'
;

GROUP14
:
    'group14'
;

GROUP19
:
    'group19'
;

GROUP20
:
    'group20'
;

HAS_KNOWN_VULNERABILITIES
:
    'has-known-vulnerabilities'
;

HASH
:
    'hash'
;

HELLO_INTERVAL
:
    'hello-interval'
;

HELPER_ENABLE
:
    'helper-enable'
;

HIP_PROFILES
:
    'hip-profiles'
;

HOSTNAME
:
    'hostname'
;

HOURS
:
    'hours'
;

IBGP
:
    'ibgp'
;

ICMP
:
    'icmp'
;

IKE
:
    'ike'
;

IKE_CRYPTO_PROFILES
:
    'ike-crypto-profiles'
;

IMPORT
:
    'import'
;

INCOMING_BGP_CONNECTION
:
    'incoming-bgp-connection'
;

INSTALL_ROUTE
:
    'install-route'
;

INTERFACE
:
    'interface'
;

IP
:
    'ip'
;

IP_ADDRESS_LITERAL
:
    'ip-address'
;

IP_NETMASK
:
    'ip-netmask'
;

IP_RANGE_LITERAL
:
    'ip-range'
;

IPSEC_CRYPTO_PROFILES
:
    'ipsec-crypto-profiles'
;

IPV6
:
    'ipv6'
;

LAYER2
:
    'layer2'
;

LAYER3
:
    'layer3'
;

LIFETIME
:
    'lifetime'
;

LINK
:
    'link' -> pushMode ( M_Url )
;

LINK_STATE
:
    'link-state'
;

LINK_TYPE
:
    'link-type'
;

LLDP
:
    'lldp'
;

LOCAL_ADDRESS
:
    'local-address'
;

LOCAL_AS
:
    'local-as'
;

LOCAL_PORT
:
    'local-port'
;

LOG_SETTINGS
:
    'log-settings'
;

LOOPBACK
:
    'loopback'
;

MD5
:
    'md5'
;

MED
:
    'med'
;

MESHED_CLIENT
:
    'meshed-client'
;

MINUTES
:
    'minutes'
;

MEMBERS
:
    'members'
;

METRIC
:
    'metric'
;

MGT_CONFIG
:
    'mgt-config'
;

MTU
:
    'mtu'
;

NDP_PROXY
:
    'ndp-proxy'
;

NEGATE_DESTINATION
:
    'negate-destination'
;

NEGATE_SOURCE
:
    'negate-source'
;

NETMASK
:
    'netmask'
;

NETWORK
:
    'network'
;

NEXT_VR
:
  'next-vr'
;

NEXTHOP
:
    'nexthop'
;

NO
:
    'no'
;

NON_CLIENT
:
    'non-client'
;

NONE
:
    'none'
;

NORMAL
:
    'normal'
;

NSSA
:
    'nssa'
;

NTP_SERVER_ADDRESS
:
    'ntp-server-address'
;

NTP_SERVERS
:
    'ntp-servers'
;

NULL
:
    'null'
;

OSPF
:
    'ospf'
;

OSPF_EXT
:
    'ospf-ext'
;

OSPF_INT
:
    'ospf-int'
;

OSPFV3
:
    'ospfv3'
;

OSPFV3_EXT
:
    'ospfv3-ext'
;

OSPFV3_INT
:
    'ospfv3-int'
;

OUTGOING_BGP_CONNECTION
:
    'outgoing-bgp-connection'
;

P2P
:
    'p2p'
;

P2MP
:
    'p2mp'
;

PANORAMA
:
    'panorama'
;

PANORAMA_SERVER
:
    'panorama-server'
;

PASSIVE
:
    'passive'
;

PEER
:
    'peer'
;

PEER_ADDRESS
:
    'peer-address'
;

PEER_AS
:
    'peer-as'
;

PEER_GROUP
:
    'peer-group'
;

PERVASIVE
:
    'pervasive'
;

POLICY
:
    'policy'
;

PORT
:
    'port'
;

POST_RULEBASE
:
    'post-rulebase'
;

PRE_RULEBASE
:
    'pre-rulebase'
;

PRIMARY
:
    'primary'
;

PRIMARY_NTP_SERVER
:
    'primary-ntp-server'
;

PRIORITY
:
    'priority'
;

PROFILES
:
    'profiles'
;

PRONE_TO_MISUSE
:
    'prone-to-misuse'
;

PROTOCOL
:
    'protocol'
;

QOS
:
    'qos'
;

REFLECTOR_CLIENT
:
    'reflector-client'
;

REFLECTOR_CLUSTER_ID
:
    'reflector-cluster-id'
;

REJECT_DEFAULT_ROUTE
:
    'reject-default-route'
;

REMOTE_PORT
:
    'remote-port'
;

RESET_BOTH
:
    'reset-both'
;

RESET_CLIENT
:
    'reset-client'
;

RESET_SERVER
:
    'reset-server'
;

RETRANSMIT_INTERVAL
:
    'retransmit-interval'
;

RIP
:
    'rip'
;

RISK
:
    'risk'
;

ROUTER_ID
:
    'router-id'
;

ROUTING_OPTIONS
:
    'routing-options'
;

ROUTING_TABLE
:
    'routing-table'
;

RULEBASE
:
    'rulebase'
;

RULES
:
    'rules'
;

SCTP
:
    'sctp'
;

SECONDARY
:
    'secondary'
;

SECONDARY_NTP_SERVER
:
    'secondary-ntp-server'
;

SECONDS
:
    'seconds'
;

SECURITY
:
    'security'
;

SERVER
:
    'server'
;

SERVER_PROFILE
:
    'server-profile'
;

SERVERS
:
    'servers'
;

SERVICE
:
    'service'
;

SERVICE_GROUP
:
    'service-group'
;

SET
:
    'set'
;

SETTING
:
    'setting'
;

SHA1
:
    'sha1'
;

SHA256
:
    'sha256'
;

SHA384
:
    'sha384'
;

SHA512
:
    'sha512'
;

SHARED
:
    'shared'
;

SHARED_GATEWAY
:
    'shared-gateway'
;

SOURCE
:
    'source'
;

SOURCE_PORT
:
    'source-port'
;

SOURCE_USER
:
    'source-user'
;

STATIC
:
    'static'
;

STATIC_IPV6
:
    'static-ipv6'
;

STATIC_ROUTE
:
    'static-route'
;

STRICT_LSA_CHECKING
:
    'strict-LSA-checking'
;

STUB
:
    'stub'
;

SUBCATEGORY
:
    'subcategory'
;

SYSLOG
:
    'syslog'
;

SYSTEM
:
    'system'
;

TAG
:
    'tag'
;

TAP
:
    'tap'
;

TCP
:
    'tcp'
;

TECHNOLOGY
:
    'technology'
;

TEMPLATE
:
    'template'
;

THREE_DES
:
    '3des'
;

TIMEZONE
:
    'timezone'
;

TO
:
    'to'
;

TRANSFERS_FILES
:
    'transfers-files'
;

TRANSIT_DELAY
:
    'transit-delay'
;

TUNNEL
:
    'tunnel'
;

TUNNELS_OTHER_APPS
:
    'tunnels-other-apps'
;

TYPE
:
    'type'
;

UDP
:
    'udp'
;

UNITS
:
    'units'
;

UP
:
    'up'
;

UPDATE_SCHEDULE
:
    'update-schedule'
;

UPDATE_SERVER
:
    'update-server'
;

USED_BY_MALWARE
:
    'used-by-malware'
;

VIRTUAL_ROUTER
:
    'virtual-router'
;

VIRTUAL_WIRE
:
    'virtual-wire'
;

VISIBLE_VSYS
:
    'visible-vsys'
;

VLAN
:
    'vlan'
;

VSYS
:
    'vsys'
;

YES
:
    'yes'
;

ZONE
:
    'zone'
;

// Complex tokens

CLOSE_BRACKET
:
    ']'
;

COMMA
:
    ','
;

DASH
:
    '-'
;

DOUBLE_QUOTED_STRING
:
    '"' ~'"'* '"'
;

IP_ADDRESS
:
    F_IpAddress
;

IP_PREFIX
:
    F_IpPrefix
;

IP_RANGE
:
    F_IpAddress '-' F_IpAddress
;

LINE_COMMENT
:
    (
        '#'
        | '!'
    )
    F_NonNewlineChar* F_Newline+ -> channel ( HIDDEN )
;

NEWLINE
:
    F_Newline+
;

OPEN_BRACKET
:
    '['
;

RANGE
:
    F_Digit+ '-' F_Digit+
;

SINGLE_QUOTED_STRING
:
    '\'' ~'\''* '\''
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

VARIABLE
:
    F_Variable_VarChar+
;

WS
:
    F_Whitespace+ -> channel(HIDDEN) // parser never sees tokens on hidden channel
;

// Fragments

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
F_Url
:
    F_UrlStart F_UrlInner+
;

F_UrlStart
:
    [a-zA-Z]
;

F_UrlInner
:
    F_UrlInnerAlphaNum
    | F_UrlInnerReserved
    | F_UrlInnerUnreserved
;

F_UrlInnerAlphaNum
:
    [a-zA-Z0-9]
;

F_UrlInnerReserved
:
    [!*'();:@&=+$,/?%#[\]]
;

F_UrlInnerUnreserved
:
    [-_.~]
;

fragment
F_Whitespace
:
    [ \t\u000C] // tab or space or unicode 0x000C
;

fragment
F_Variable_VarChar
:
    ~[ \t\n\r;,{}[\]&|()"']
;

// Modes

mode M_Url;

M_Url_NEWLINE
:
    F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Url_WORD
:
    F_Url -> type ( WORD ) , popMode
;

M_Url_WS
:
    F_Whitespace+ -> channel ( HIDDEN )
;