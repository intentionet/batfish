lexer grammar CiscoNxosLexer;

options {
  superClass = 'org.batfish.grammar.cisco_nxos.parsing.CiscoNxosBaseLexer';
}

tokens {
  BANNER_BODY,
  BANNER_DELIMITER,
  HSRP_VERSION_1,
  HSRP_VERSION_2,
  LOCAL,
  MOTD,
  PASSWORD_0,
  PASSWORD_0_TEXT,
  PASSWORD_3,
  PASSWORD_3_MALFORMED_TEXT,
  PASSWORD_3_TEXT,
  PASSWORD_7,
  PASSWORD_7_MALFORMED_TEXT,
  PASSWORD_7_TEXT,
  QUOTED_TEXT,
  REMARK_TEXT,
  SNMP_VERSION_1,
  SNMP_VERSION_2,
  SNMP_VERSION_2C,
  SUBDOMAIN_NAME,
  WORD
}

AAA
:
  'aaa'
;

ACCESS
:
  'access'
;

ACCESS_GROUP
:
  'access-group'
;

ACCESS_LIST
:
  'access-list' -> pushMode ( M_Word )
;

ACCESS_MAP
:
  'access-map'
;

ACCOUNTING
:
  'accounting'
;

ACK
:
  'ack'
;

ACL_COMMON_IP_OPTIONS
:
  'acl_common_ip_options'
;

ACL_GLOBAL_OPTIONS
:
  'acl_global_options'
;

ACL_ICMP
:
  'acl_icmp'
;

ACL_IGMP
:
  'acl_igmp'
;

ACL_INDICES
:
  'acl_indices'
;

ACL_SIMPLE_PROTOCOLS
:
  'acl_simple_protocols'
;

ACL_TCP
:
  'acl_tcp'
;

ACL_UDP
:
  'acl_udp'
;

ACTION
:
  'action'
;

ACTIVE
:
  'active'
;

ADD
:
  'add'
;

ADDITIONAL_PATHS
:
  'additional-paths'
;

ADDITIVE
:
  'additive'
;

ADDRESS
:
  'address'
  // All other instances are followed by tokens in default mode
  {
    if (secondToLastTokenType() == MATCH && lastTokenType() == IP) {
      pushMode(M_MatchIpAddress);
    } else if (secondToLastTokenType() == OBJECT_GROUP) {
      pushMode(M_Word);
    }
  }

;

ADDRESS_FAMILY
:
  'address-family'
;

ADDRGROUP
:
  'addrgroup' -> pushMode ( M_Word )
;

ADMINISTRATIVELY_PROHIBITED
:
  'administratively-prohibited'
;

ADVERTISE
:
  'advertise'
;

ADVERTISE_MAP
:
  'advertise-map'
;

AES_128
:
  'aes-128'
;

AF11
:
  'af11'
;

AF12
:
  'af12'
;

AF13
:
  'af13'
;

AF21
:
  'af21'
;

AF22
:
  'af22'
;

AF23
:
  'af23'
;

AF31
:
  'af31'
;

AF32
:
  'af32'
;

AF33
:
  'af33'
;

AF41
:
  'af41'
;

AF42
:
  'af42'
;

AF43
:
  'af43'
;

AGGREGATE_ADDRESS
:
  'aggregate-address'
;

AGGRESSIVE
:
  'aggressive'
;

AHP
:
  'ahp'
;

ALIAS
:
  'alias'
;

ALL
:
  'all'
;

ALLOWAS_IN
:
  'allowas-in'
;

ALLOWED
:
  'allowed'
;

ALTERNATE_ADDRESS
:
  'alternate-address'
;

ALWAYS
:
  'always'
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

AS_OVERRIDE
:
  'as-override'
;

AS_PATH
:
  'as-path'
  // All other instances are followed by keywords
  {
    if (lastTokenType() == MATCH) {
      pushMode(M_Words);
    }
  }

;

AS_SET
:
  'as-set'
;

ASSOCIATE
:
  'associate'
;

ASSOCIATE_VRF
:
  'associate-vrf'
;

ATTRIBUTE_MAP
:
  'attribute-map'
;

AUTH
:
  'auth'
;

AUTHENTICATION
:
  'authentication'
;

AUTHORIZATION
:
  'authorization'
;

AUTO
:
  'auto'
;

AUTO_COST
:
  'auto-cost'
;

AUTOSTATE
:
  'autostate'
;

BACKUP
:
  'backup'
;

BANDWIDTH
:
  'bandwidth'
;

BANNER
:
  'banner' -> pushMode ( M_Banner )
;

BASH_SHELL
:
  'bash-shell'
;

BC
:
  'bc'
;

BGP
:
  'bgp'
;

BESTPATH
:
  'bestpath'
;

BESTPATH_LIMIT
:
  'bestpath-limit'
;

BFD
:
  'bfd'
;

BIFF
:
  'biff'
;

BOOT
:
  'boot'
;

BOOTPC
:
  'bootpc'
;

BOOTPS
:
  'bootps'
;

BOTH
:
  'both'
;

BPDUFILTER
:
  'bpdufilter'
;

BPDUGUARD
:
  'bpduguard'
;

BPS
:
  'bps'
;

BRIDGE
:
  'bridge'
;

BROADCAST
:
  'broadcast'
;

BYTES
:
  'bytes'
;

CALLHOME
:
  'callhome'
;

CAPABILITY
:
  'capability'
;

CAUSE
:
  'cause'
;

CCMCLIRUNNINGCONFIGCHANGED
:
  [Cc] [Cc] [Mm] [Cc] [Ll] [Ii] [Rr] [Uu] [Nn] [Nn] [Ii] [Nn] [Gg] [Cc] [Oo]
  [Nn] [Ff] [Ii] [Gg] [Cc] [Hh] [Aa] [Nn] [Gg] [Ee] [Dd]
;

CDP
:
  'cdp'
;

CFS
:
  'cfs'
;

CHAIN
:
  'chain' -> pushMode ( M_Word )
;

CHANNEL_GROUP
:
  'channel-group'
;

CHARGEN
:
  'chargen'
;

CIR
:
  'cir'
;

CLASS
:
  'class' -> pushMode ( M_Class )
;

CLASS_DEFAULT
:
  'class-default'
;

CLASS_MAP
:
  'class-map' -> pushMode ( M_ClassMap )
;

CLEAR
:
  'clear'
;

CLI
:
  'cli'
;

CLIENT_TO_CLIENT
:
  'client-to-client'
;

CLOCK
:
  'clock'
;

CLUSTER_ID
:
  'cluster-id'
;

CMD
:
  'cmd'
;

COMMAND
:
  'command'
;

COMMANDS
:
  'commands'
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
      case SNMP_SERVER:
        pushMode(M_Word);
        break;
      default:
        break;
    }
  }

;

COMMUNITY_LIST
:
  'community-list'
;

COMPARE_ROUTERID
:
  'compare-routerid'
;

CONFIG
:
  'config'
;

CONFIG_COMMANDS
:
  'config-commands'
;

CONFIGURATION
:
  'configuration'
;

CONFED
:
  'confed'
;

CONFEDERATION
:
  'confederation'
;

CONFORM
:
  'conform'
;

CONNECT
:
  'connect'
;

CONNECTION_MODE
:
  'connection-mode'
;

CONTACT
:
// followed by arbitrary contact information
  'contact' -> pushMode ( M_Remark )
;

CONTEXT
:
  'context' -> pushMode ( M_Word )
;

CONTINUE
:
  'continue'
;

CONTROL_PLANE
:
  'control-plane'
;

CONVERSION_ERROR
:
  'conversion-error'
;

COPY
:
  'copy'
;

COPY_ATTRIBUTES
:
  'copy-attributes'
;

COS
:
  'cos'
;

COST
:
  'cost'
;

COST_COMMUNITY
:
  'cost-community'
;

COUNTERS
:
  'counters'
;

CRITICAL
:
  'critical'
;

CS1
:
  'cs1'
;

CS2
:
  'cs2'
;

CS3
:
  'cs3'
;

CS4
:
  'cs4'
;

CS5
:
  'cs5'
;

CS6
:
  'cs6'
;

CS7
:
  'cs7'
;

DAMPEN_IGP_METRIC
:
  'dampen-igp-metric'
;

DAMPENING
:
  'dampening'
;

DAYTIME
:
  'daytime'
;

DEAD_INTERVAL
:
  'dead-interval'
;

DECREMENT
:
  'decrement'
;

DEFAULT
:
  'default'
;

DEFAULT_COST
:
  'default-cost'
;

DEFAULT_INFORMATION
:
  'default-information'
;

DEFAULT_METRIC
:
  'default-metric'
;

DEFAULT_ORIGINATE
:
  'default-originate'
;

DELAY
:
  'delay'
;

DELETE
:
  'delete'
;

DENY
:
  'deny'
;

DENY_ALL
:
  'deny-all'
;

DESCRIPTION
:
  'description' -> pushMode ( M_Remark )
;

DEST_IP
:
  'dest-ip'
;

DEST_MISS
:
  'dest-miss'
;

DETAIL
:
  'detail'
;

DHCP
:
  'dhcp'
;

DIR
:
  'dir'
;

DIRECT
:
  'direct'
;

DIRECTLY_CONNECTED_SOURCES
:
  'directly-connected-sources'
;

DISABLE
:
  'disable'
;

DISABLE_CONNECTED_CHECK
:
  'disable-connected-check'
;

DISABLE_PEER_AS_CHECK
:
  'disable-peer-as-check'
;

DISCARD
:
  'discard'
;

DISTANCE
:
  'distance'
;

DNSIX
:
  'dnsix'
;

DOD_HOST_PROHIBITED
:
  'dod-host-prohibited'
;

DOD_NET_PROHIBITED
:
  'dod-net-prohibited'
;

DOMAIN
:
  'domain'
;

DOMAIN_LOOKUP
:
  'domain-lookup'
;

DONT_CAPABILITY_NEGOTIATE
:
  'dont-capability-negotiate'
;

DOT1Q
:
  [Dd] [Oo] [Tt] '1' [Qq]
;

DOT1Q_TUNNEL
:
  'dot1q-tunnel'
;

DRIP
:
  'drip'
;

DROP
:
  'drop'
;

DROP_ON_FAIL
:
  'drop-on-fail'
;

DSCP
:
  'dscp'
;

DUAL_AS
:
  'dual-as'
;

DUPLEX
:
  'duplex'
;

DVMRP
:
  'dvmrp'
;

DYNAMIC_CAPABILITY
:
  'dynamic-capability'
;

EBGP_MULTIHOP
:
  'ebgp-multihop'
;

ECHO
:
  'echo'
;

ECHO_REPLY
:
  'echo-reply'
;

EDGE
:
  'edge'
;

EF
:
  'ef'
;

EGP
:
  'egp'
;

EIGRP
:
  'eigrp'
;

EIBGP
:
  'eibgp'
;

ENABLE
:
  'enable'
;

ENCAPSULATION
:
  'encapsulation'
;

ENFORCE_FIRST_AS
:
  'enforce-first-as'
;

EQ
:
  'eq'
;

ERRDISABLE
:
  'errdisable'
;

ERROR_ENABLE
:
  'error-enable'
;

ERRORS
:
  'errors'
;

ESP
:
  'esp'
;

ESTABLISHED
:
  'established'
;

ETHERNET
:
  [Ee] [Tt] [Hh] [Ee] [Rr] [Nn] [Ee] [Tt]
;

EVENT_HISTORY
:
  'event-history'
;

EVENT_NOTIFY
:
  'event-notify'
;

EVENTS
:
  'events'
;

EVPN
:
  'evpn'
;

EXCEPT
:
  'except'
;

EXCEPTION
:
  'exception'
;

EXEC
:
  'exec'
;

EXEMPT
:
  'exempt'
;

EXIST_MAP
:
  'exist-map'
;

EXPLICIT_TRACKING
:
  'explicit-tracking'
;

EXPORT
:
  'export'
;

EXTENDED
:
  'extended'
;

EXTERNAL
:
  'external'
;

EXTERNAL_LSA
:
  'external-lsa'
;

FAST_EXTERNAL_FALLOVER
:
  'fast-external-fallover'
;

FAST_LEAVE
:
  'fast-leave'
;

FCOE_FIB_MISS
:
  'fcoe-fib-miss'
;

FEATURE
:
  'feature'
;

FEATURE_CONTROL
:
  'feature-control'
;

FEATUREOPSTATUSCHANGE
:
  [Ff] [Ee] [Aa] [Tt] [Uu] [Rr] [Ee] [Oo] [Pp] [Ss] [Tt] [Aa] [Tt] [Uu] [Ss]
  [Cc] [Hh] [Aa] [Nn] [Gg] [Ee]
;

FEX
:
  'fex'
;

FEX_FABRIC
:
  'fex-fabric'
;

FILTER
:
  'filter'
;

FILTER_LIST
:
  'filter-list'
;

FIN
:
  'fin'
;

FINGER
:
  'finger'
;

FLASH
:
  'flash'
;

FLASH_OVERRIDE
:
  'flash-override'
;

FLOW
:
  'flow'
;

FORCE
:
  'force'
;

FORCE_ORDER
:
  'force-order'
;

FORWARDING_THRESHOLD
:
  'forwarding-threshold'
;

FLUSH_ROUTES
:
  'flush-routes'
;

FOUR_BYTE_AS
:
  'four-byte-as'
;

FRAGMENTS
:
  'fragments'
;

FTP_DATA
:
  'ftp-data'
;

FTP
:
  'ftp'
;

FULL
:
  'full'
;

GBPS
:
  [Gg] [Bb] [Pp] [Ss]
;

GENERAL_PARAMETER_PROBLEM
:
  'general-parameter-problem'
;

GE
:
  'ge'
;

GET
:
  'get'
;

GLEAN
:
  'glean'
;

GLOBAL
:
  'global'
;

GOPHER
:
  'gopher'
;

GRACEFUL_RESTART
:
  'graceful-restart'
;

GRACEFUL_RESTART_HELPER
:
  'graceful-restart-helper'
;

GRE
:
  'gre'
;

GROUP
:
  'group'
  // When preceded by 'default, followed by list of AAA server groups.
  // If preceded by 'community' and then a secret, followed by the name of a user group.
  // Otherwise, stay in default mode.
  {
    if (lastTokenType() == DEFAULT) {
      pushMode(M_AaaGroup);
    } else if (secondToLastTokenType() == COMMUNITY && lastTokenType() == WORD) {
      pushMode(M_Word);
    }
  }
;

GROUP_TIMEOUT
:
  'group-timeout'
;

GT
:
  'gt'
;

GUARD
:
  'guard'
;

HEAD
:
  'head'
;

HELLO_INTERVAL
:
  'hello-interval'
;

HMM
:
  'hmm'
;

HOST
:
  'host'
;

HOST_ISOLATED
:
  'host-isolated'
;

HOST_PRECEDENCE_UNREACHABLE
:
  'host-precedence-unreachable'
;

HOST_QUERY
:
  'host-query'
;

HOST_REACHABILITY
:
  'host-reachability'
;

HOST_REDIRECT
:
  'host-redirect'
;

HOST_REPORT
:
  'host-report'
;

HOST_TOS_REDIRECT
:
  'host-tos-redirect'
;

HOST_TOS_UNREACHABLE
:
  'host-tos-unreachable'
;

HOST_UNKNOWN
:
  'host-unknown'
;

HOST_UNREACHABLE
:
  'host-unreachable'
;

HOSTNAME
:
  'hostname'
  // Mode is needed so as not to interfere with interface names.
  // E.g. 'Ethernet1' should be ETHERNET UINT8 rather than SUBDOMAIN_NAME
  // May be revisited as grammar is fleshed out.
  {
    if (lastTokenType() == NEWLINE || lastTokenType() == -1) {
      pushMode(M_Hostname);
    }
  }

;

HSRP
:
  'hsrp'
;

HTTP_METHOD
:
  'http-method'
;

IBGP
:
  'ibgp'
;

ICMP
:
  'icmp'
;

IDENT
:
  'ident'
;

IDENTIFIER
:
  'identifier'
;

IGMP
:
  'igmp'
;

IGNORE
:
  'ignore'
;

IGP
:
  'igp'
;

IMMEDIATE
:
  'immediate'
;

IMPORT
:
  'import'
;

IN
:
  'in'
;

INBOUND
:
  'inbound'
;

INCLUDE_STUB
:
  'include-stub'
;

INCOMPLETE
:
  'incomplete'
;

INCONSISTENCY
:
  'inconsistency'
;

INFORMATION_REPLY
:
  'information-reply'
;

INFORMATION_REQUEST
:
  'information-request'
;

INGRESS_REPLICATION
:
  'ingress-replication'
;

INHERIT
:
  'inherit'
;

INJECT_MAP
:
  'inject-map'
;

INPUT
:
  'input'
  {
    if (lastTokenType() == SERVICE_POLICY) {
      pushMode(M_Word);
    }
  }
;

INSTALL
:
  'install'
;

INTERFACE
:
// most common abbreviation
  'int'
  (
    'erface'
  )?
;

INTERFACE_VLAN
:
  'interface-vlan'
;

INTERNAL
:
  'internal'
;

INTERNET
:
  'internet'
;

INTERVAL
:
  'interval'
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

IPV6_DEST_MISS
:
  'ipv6-dest-miss'
;

IPV6_RPF_FAILURE
:
  'ipv6-rpf-failure'
;

IPV6_SG_RPF_FAILURE
:
  'ipv6-sg-rpf-failure'
;

IRC
:
  'irc'
;

ISAKMP
:
  'isakmp'
;

ISIS
:
  'isis'
;

ISOLATE
:
  'isolate'
;

KBPS
:
  'kbps'
;

KEY
:
  'key'
;

KEY_CHAIN
:
  'key-chain' -> pushMode ( M_Word )
;

KEY_STRING
:
  'key-string' -> pushMode ( M_Remark )
;

KICKSTART
:
  'kickstart'
  // name of image follows if preceded by 'boot'
  {
    if (lastTokenType() == BOOT) {
      pushMode(M_Remark);
    }
  }

;

KLOGIN
:
  'klogin'
;

KSHELL
:
  'kshell'
;

L2
:
  [lL] '2'
;

L2VPN
:
  'l2vpn'
;

L3
:
  'L3'
;

LACP
:
  'lacp'
;

LARGE
:
  'large'
;

LAST_AS
:
  'last-as'
;

LAST_MEMBER_QUERY_INTERVAL
:
  'last-member-query-interval'
;

LE
:
  'le'
;

LEVEL
:
  'level'
;

LINE_PROTOCOL
:
  'line-protocol'
;

LINK_FLAP
:
  'link-flap'
;

LINK_LOCAL_GROUPS_SUPPRESSION
:
  'link-local-groups-suppression'
;

LINK_STATE
:
  'link-state'
;

LINK_STATUS
:
  'link-status'
;

LISP
:
  'lisp'
;

LLDP
:
  'lldp'
;

LOAD_SHARE
:
  'load-share'
;

LOCAL_AS
:
  [Ll] [Oo] [Cc] [Aa] [Ll] '-' [Aa] [Ss]
;

LOCAL_PREFERENCE
:
  'local-preference'
;

LOCALIZEDKEY
:
  'localizedkey'
;

LOCATION
:
// followed by arbitrary location description
  'location' -> pushMode ( M_Remark )
;

LOG
:
  'log'
;

LOG_ADJACENCY_CHANGES
:
  'log-adjacency-changes'
;

LOG_NEIGHBOR_CHANGES
:
  'log-neighbor-changes'
;

LOGIN
:
  'login'
;

LONG
:
  'long'
;

LOOP
:
  'loop'
;

LOOP_INCONSISTENCY
:
  'loop-inconsistency'
;

LOOPBACK
:
// most common abbreviation
  [Ll] [Oo]
  (
    [Oo] [Pp] [Bb] [Aa] [Cc] [Kk]
  )?
;

LOW_MEMORY
:
  'low-memory'
;

LOWER
:
  'lower'
;

LPD
:
  'lpd'
;

LSA
:
  'lsa'
;

LSA_ARRIVAL
:
  'lsa-arrival'
;

LSA_GROUP_PACING
:
  'lsa-group-pacing'
;

LT
:
  'lt'
;

MASK
:
  'mask'
;

MASK_REPLY
:
  'mask-reply'
;

MASK_REQUEST
:
  'mask-request'
;

MATCH
:
  'match'
;

MATCH_ALL
:
  'match-all'
;

MATCH_ANY
:
  'match-any'
;

MAX_METRIC
:
  'max-metric'
;

MAXAS_LIMIT
:
  'maxas-limit'
;

MAXIMUM
:
  'maximum'
;

MAXIMUM_PATHS
:
  'maximum-paths'
;

MAXIMUM_PEERS
:
  'maximum-peers'
;

MAXIMUM_PREFIX
:
  'maximum-prefix'
;

MBPS
:
  [Mm] [Bb] [Pp] [Ss]
;

MCAST_GROUP
:
  'mcast-group'
;

MD5
:
  'md5'
  // if preceded by 'auth', password follows
  {
    if (lastTokenType() == AUTH) {
      pushMode(M_Word);
    }
  }
;

MED
:
  'med'
;

MEDIA
:
  'media'
;

MEDIUM
:
  'medium'
;

MEMBER
:
  'member'
  // All other instances are followed by keywords or tokens in default mode
  {
    if (lastTokenType() == VRF) {
      pushMode(M_Word);
    }
  }

;

MERGE_FAILURE
:
  'merge-failure'
;

MESSAGE_DIGEST
:
  'message-digest'
;

MESSAGE_DIGEST_KEY
:
  'message-digest-key' -> pushMode ( M_Password )
;

METHOD
:
  'method'
;

METRIC
:
  'metric'
;

METRIC_TYPE
:
  'metric-type'
;

MGMT
:
  [Mm] [Gg] [Mm] [Tt]
;

MIN_LINKS
:
  'min-links'
;

MIN_RX
:
  'min_rx'
;

MINIMUM
:
  'minimum'
;

MISSING_AS_WORST
:
  'missing-as-worst'
;

MOBILE_IP
:
  'mobile-ip'
;

MOBILE_REDIRECT
:
  'mobile-redirect'
;

MODE
:
  'mode'
;

MONITOR
:
  'monitor'
  {
    if (lastTokenType() == FLOW) {
      pushMode(M_Word);
    }
  }
;

MROUTER
:
  'mrouter'
;

MSEC
:
  'msec'
;

MST
:
  'mst'
;

MTU
:
  'mtu'
;

MTU_FAILURE
:
  'mtu-failure'
;

MULTICAST
:
  'multicast'
;

MULTIPATH_RELAX
:
  'multipath-relax'
;

MULTIPLIER
:
  'multiplier'
;

MVPN
:
  'mvpn'
;

NAME
:
  'name'
  // If preceded by 'alias', name and then arbitrary text definition follow.
  // Otherwise, just a name follows.
  {
    if (lastTokenType() == ALIAS) {
      pushMode(M_AliasName);
    } else {
      pushMode(M_Word);
    }
  }

;

NAMESERVER
:
  'nameserver'
;

NAT_FLOW
:
  'nat-flow'
;

NATIVE
:
  'native'
;

ND_NA
:
  'nd-na'
;

ND_NS
:
  'nd-ns'
;

NEGOTIATE
:
  'negotiate'
;

NEIGHBOR
:
  'neighbor'
;

NEQ
:
  'neq'
;

NETBIOS_DGM
:
  'netbios-dgm'
;

NETBIOS_NS
:
  'netbios-ns'
;

NETBIOS_SS
:
  'netbios-ss'
;

NET_REDIRECT
:
  'net-redirect'
;

NET_TOS_REDIRECT
:
  'net-tos-redirect'
;

NET_TOS_UNREACHABLE
:
  'net-tos-unreachable'
;

NET_UNREACHABLE
:
  'net-unreachable'
;

NETWORK
:
  'network'
;

NETWORK_QOS
:
  'network-qos'
;

NETWORK_UNKNOWN
:
  'network-unknown'
;

NEWROOT
:
  'newroot'
;

NEXT_HOP
:
  'next-hop'
;

NEXT_HOP_SELF
:
  'next-hop-self'
;

NEXT_HOP_THIRD_PARTY
:
  'next-hop-third-party'
;

NEXTHOP
:
  'nexthop'
;

NNTP
:
  'nntp'
;

NO
:
  'no'
;

NO_ADVERTISE
:
  'no-advertise'
;

NO_EXPORT
:
  'no-export'
;

NO_PREPEND
:
  'no-prepend'
;

NO_REDISTRIBUTION
:
  'no-redistribution'
;

NO_ROOM_FOR_OPTION
:
  'no-room-for-option'
;

NO_SUMMARY
:
  'no-summary'
;

NON_CRITICAL
:
  'non-critical'
;

NON_DETERMINISTIC
:
  'non-deterministic'
;

NON_EXIST_MAP
:
  'non-exist-map'
;

NON500_ISAKMP
:
  'non500-isakmp'
;

NONE
:
  'none'
;

NORMAL
:
  'normal'
;

NOS
:
  'nos'
;

NOT_ADVERTISE
:
  'not-advertise'
;

NSSA
:
  'nssa'
;

NTP
:
  'ntp'
;

NULL0
:
  [Nn] [Uu] [Ll] [Ll] ' '* '0'
;

NV
:
  'nv'
;

NVE
:
  'nve'
;

NXOS
:
  'nxos'
  // name of image follows if preceded by 'boot'
  {
    if (lastTokenType() == BOOT) {
      pushMode(M_Remark);
    }
  }

;

OBJECT_GROUP
:
  'object-group'
;

OBJSTORE
:
  'objstore'
;

ON
:
  'on'
;

ON_STARTUP
:
  'on-startup'
;

OPTION
:
  'option'
;

OPTION_MISSING
:
  'option-missing'
;

ORIGIN
:
  'origin'
;

ORIGINATE
:
  'originate'
;

OSPF
:
  'ospf'
  // All other instances are followed by keywords or tokens in default mode
  {
    if (lastTokenType() == REDISTRIBUTE || lastTokenType() == ROUTER) {
      pushMode(M_Word);
    }
  }

;

OSPFV3
:
  'ospfv3'
;

OUT
:
  'out'
;

OVERLAY
:
  'overlay'
;

PACKET_LENGTH
:
  'packet-length'
;

PACKET_TOO_BIG
:
  'packet-too-big'
;

PARAMETER_PROBLEM
:
  'parameter-problem'
;

PASSIVE
:
  'passive'
;

PASSIVE_INTERFACE
:
  'passive-interface'
;

PASSWORD
:
  'password' -> pushMode ( M_Password )
;

PATHCOST
:
  'pathcost'
;

PBR
:
  'pbr'
;

PBR_STATISTICS
:
  'pbr-statistics'
;

PCP
:
  'pcp'
;

PEER
:
  'peer' -> pushMode ( M_Word )
;

PEER_IP
:
  'peer-ip'
;

PEER_POLICY
:
  'peer-policy'
;

PEER_SESSION
:
  'peer-session'
;

PEER_VTEP
:
  'peer-vtep'
;

PEERS
:
  'peers'
;

PER_ENTRY
:
  'per-entry'
;

PER_LINK
:
  'per-link'
;

PERIODIC
:
  'periodic'
;

PERMIT
:
  'permit'
;

PERMIT_ALL
:
  'permit-all'
;

PIM
:
  'pim'
;

PIM_AUTO_RP
:
  'pim-auto-rp'
;

PIM6
:
  'pim6'
;

PING
:
  'ping'
;

POAP
:
  'poap'
;

POINT_TO_POINT
:
  'point-to-point'
;

POLICE
:
  'police'
;

POLICY
:
  'policy' -> pushMode ( M_Word )
;

POLICY_MAP
:
  'policy-map' -> pushMode ( M_PolicyMap )
;

POP2
:
  'pop2'
;

POP3
:
  'pop3'
;

PORT
:
  'port'
;

PORT_CHANNEL
:
  [Pp] [Oo] [Rr] [Tt] '-' [Cc] [Hh] [Aa] [Nn] [Nn] [Ee] [Ll]
;

PORT_PRIORITY
:
  'port-priority'
;

PORT_UNREACHABLE
:
  'port-unreachable'
;

PORTGROUP
:
  'portgroup' -> pushMode ( M_Word )
;

POST
:
  'post'
;

PPS
:
  'pps'
;

PRECEDENCE
:
  'precedence'
;

PRECEDENCE_UNREACHABLE
:
  'precedence-unreachable'
;

PREEMPT
:
  'preempt'
;

PREFIX_LIST
:
  'prefix-list' -> pushMode ( M_Word )
;

PREFIX_PEER_TIMEOUT
:
  'prefix-peer-timeout'
;

PREFIX_PEER_WAIT
:
  'prefix-peer-wait'
;

PREPEND
:
  'prepend'
;

PRIORITY
:
  'priority'
;

PRIV
:
  'priv' -> pushMode ( M_Priv )
;

PROTOCOL
:
  'protocol'
;

PROTOCOL_UNREACHABLE
:
  'protocol-unreachable'
;

PROXY
:
  'proxy'
;

PROXY_LEAVE
:
  'proxy-leave'
;

PSECURE_VIOLATION
:
  'psecure-violation'
;

PSH
:
  'psh'
;

PUT
:
  'put'
;

QOS
:
  'qos'
;

QOS_GROUP
:
  'qos-group'
;

QUERIER
:
  'querier'
;

QUERIER_TIMEOUT
:
  'querier-timeout'
;

QUERY_INTERVAL
:
  'query-interval'
;

QUERY_MAX_RESPONSE_TIME
:
  'query-max-response-time'
;

QUEUEING
:
  'queueing'
;

RANGE
:
  'range'
;

RD
:
  'rd'
;

REACHABILITY
:
  'reachability'
;

READ
:
  'read'
;

REASSEMBLY_TIMEOUT
:
  'reassembly-timeout'
;

RECEIVE
:
  'receive'
;

RECONNECT_INTERVAL
:
  'reconnect-interval'
;

RECOVERY
:
  'recovery'
;

REDIRECT
:
  'redirect'
;

REDIRECTS
:
  'redirects'
;

REDISTRIBUTE
:
  'redistribute'
;

REFERENCE_BANDWIDTH
:
  'reference-bandwidth'
;

REFLECTION
:
  'reflection'
;

RELOAD
:
  'reload'
;

REMARK
:
  'remark' -> pushMode ( M_Remark )
;

REMOTE_AS
:
  'remote-as'
;

REMOVE
:
  'remove'
;

REMOVE_PRIVATE_AS
:
  'remove-private-as'
;

REPLACE_AS
:
  'replace-as'
;

REPORT_FLOOD
:
  'report-flood'
;

REPORT_POLICY
:
  'report-policy'
;

REPORT_SUPPRESSION
:
  'report-suppression'
;

RESTART
:
  'restart'
;

RESTART_TIME
:
  'restart-time'
;

RETAIN
:
  'retain'
;

RIP
:
  'rip'
;

ROBUSTNESS_VARIABLE
:
  'robustness-variable'
;

ROLE
:
  'role'
;

ROOT
:
  'root'
;

ROOT_INCONSISTENCY
:
  'root-inconsistency'
;

ROUTABLE
:
  'routable'
;

ROUTE
:
  'route'
;

ROUTE_MAP
:
  'route-map' -> pushMode ( M_Word )
;

ROUTE_REFLECTOR_CLIENT
:
  'route-reflector-client'
;

ROUTE_TARGET
:
  'route-target'
;

ROUTER
:
  'router'
;

ROUTER_ADVERTISEMENT
:
  'router-advertisement'
;

ROUTER_ID
:
  'router-id'
;

ROUTER_LSA
:
  'router-lsa'
;

ROUTER_SOLICITATION
:
  'router-solicitation'
;

ROUTINE
:
  'routine'
;

RPF_FAILURE
:
  'rpf-failure'
;

RST
:
  'rst'
;

RULE
:
  'rule'
;

SAMPLER
:
  'sampler' -> pushMode(M_Word)
;

SCHEDULER
:
  'scheduler'
;

SECONDARY
:
  'secondary'
;

SECURITY_VIOLATION
:
  'security-violation'
;

SELECTION
:
  'selection'
;

SEND
:
  'send'
;

SEND_COMMUNITY
:
  'send-community'
;

SEQ
:
  'seq'
;

SERVER
:
  'server'
;

SERVER_STATE_CHANGE
:
  'server-state-change'
;

SERVICE
:
  'service'
;

SERVICE_POLICY
:
  'service-policy'
;

SET
:
  'set'
;

SFLOW
:
  'sflow'
;

SG_RPF_FAILURE
:
  'sg-rpf-failure'
;

SHA
:
  'sha'
  // if preceded by 'auth', password follows
  {
    if (lastTokenType() == AUTH) {
      pushMode(M_Word);
    }
  }
;

SHOW
:
  'show'
;

SHUTDOWN
:
  'shutdown'
;

SIZE
:
  'size'
;

SLA
:
  'sla'
;

SMALL
:
  'small'
;

SMTP
:
  'smtp'
;

SMTP_SEND_FAIL
:
  'smtp-send-fail'
;

SNMP
:
  'snmp'
;

SNMP_SERVER
:
  'snmp-server'
;

SNMPTRAP
:
  'snmptrap'
;

SOFT_RECONFIGURATION
:
  'soft-reconfiguration'
;

SOO
:
  'soo'
;

SOURCE_INTERFACE
:
  'source-interface'
;

SOURCE_QUENCH
:
  'source-quench'
;

SOURCE_ROUTE_FAILED
:
  'source-route-failed'
;

SPANNING_TREE
:
  'spanning-tree'
;

SPARSE_MODE
:
  'sparse-mode'
;

SPEED
:
  'speed'
;

SPF
:
  'spf'
;

SPINE_ANYCAST_GATEWAY
:
  'spine-anycast-gateway'
;

SRC_IP
:
  'src-ip'
;

STALEPATH_TIME
:
  'stalepath-time'
;

STANDARD
:
  'standard' -> pushMode ( M_Word )
;

STARTUP_QUERY_COUNT
:
  'startup-query-count'
;

STARTUP_QUERY_INTERVAL
:
  'startup-query-interval'
;

STATE
:
  'state'
;

STATE_CHANGE
:
  'state-change'
;

STATE_CHANGE_NOTIF
:
  'state-change-notif'
;

STATIC
:
  'static'
;

STATIC_GROUP
:
  'static-group'
;

STATISTICS
:
  'statistics'
;

STORM_CONTROL
:
  'storm-control'
;

STPX
:
  'stpx'
;

STUB
:
  'stub'
;

SUMMARY_ADDRESS
:
  'summary-address'
;

SUMMARY_LSA
:
  'summary-lsa'
;

SUMMARY_ONLY
:
  'summary-only'
;

SUNRPC
:
  'sunrpc'
;

SUPPRESS
:
  'suppress'
;

SUPPRESS_ARP
:
  'suppress-arp'
;

SUPPRESS_FIB_PENDING
:
  'suppress-fib-pending'
;

SUPPRESS_INACTIVE
:
  'suppress-inactive'
;

SUPPRESS_MAP
:
  'suppress-map'
;

SUSPEND_INDIVIDUAL
:
  'suspend-individual'
;

SWITCHNAME
:
  'switchname'
  {
    if (lastTokenType() == NEWLINE || lastTokenType() == -1) {
      pushMode(M_Hostname);
    }
  }
;

SWITCHPORT
:
  'switchport'
;

SYN
:
  'syn'
;

SYNC
:
  'sync'
;

SYSLOG
:
  'syslog'
;

SYSTEM
:
  'system'
  // name of image follows if preceded by 'boot'
  {
    if (lastTokenType() == BOOT) {
      pushMode(M_Remark);
    }
  }

;

TABLE_MAP
:
  'table-map'
;

TACACS
:
  'tacacs'
;

TACACSP
:
  'tacacs+'
  // Other instances are followed by tokens in default mode, or occur in non-default mode.
  {
    if (lastTokenType() == SERVER) {
      pushMode(M_Word);
    }
  }
;

TAG
:
  'tag'
;

TALK
:
  'talk'
;

TCP
:
  'tcp'
;

TCP_FLAGS_MASK
:
  'tcp-flags-mask'
;

TCP_OPTION_LENGTH
:
  'tcp-option-length'
;

TELNET
:
  'telnet'
;

TEMPLATE
:
  'template'
;

TERMINAL
:
  'terminal'
;

TEXT
:
  'text' -> pushMode ( M_Word )
;

TFTP
:
  'tftp'
;

THROTTLE
:
  'throttle'
;

TIME
:
  'time'
;

TIME_EXCEEDED
:
  'time-exceeded'
;

TIMERS
:
  'timers'
;

TIMESTAMP_REPLY
:
  'timestamp-reply'
;

TIMESTAMP_REQUEST
:
  'timestamp-request'
;

TIMEZONE
:
  'timezone' -> pushMode ( M_Remark )
;

TOPOLOGYCHANGE
:
  'topologychange'
;

TRACEROUTE
:
  'traceroute'
;

TRACE
:
  'trace'
;

TRACK
:
  'track'
;

TRAFFIC_FILTER
:
  'traffic-filter' -> pushMode ( M_Word )
;

TRANSMIT
:
  'transmit'
;

TRANSPORT
:
  'transport'
;

TRAP
:
  'trap'
;

TRAPS
:
  'traps'
  // if not preceded by 'enable', followed by 'version' or SNMP community secret
  {
    if (lastTokenType() != ENABLE) {
      pushMode(M_SnmpHostTraps);
    }
  }
;

TRIGGER_DELAY
:
  'trigger-delay'
;

TRUNK
:
  'trunk'
;

TTL
:
  'ttl'
;

TTL_EXCEEDED
:
  'ttl-exceeded'
;

TTL_FAILURE
:
  'ttl-failure'
;

TYPE
:
  'type'
  // Other instances are followed by tokens in default mode, or occur in non-default mode.
  {
    if (lastTokenType() == SERVICE_POLICY) {
      pushMode(M_ClassType);
    }
  }
;

TYPE_1
:
  'type-1'
;

TYPE_2
:
  'type-2'
;

UDLD
:
  'udld'
;

UDP
:
  'udp'
;

UNCHANGED
:
  'unchanged'
;

UNICAST
:
  'unicast'
;

UNREACHABLE
:
  'unreachable'
;

UNREACHABLES
:
  'unreachables'
;

UNSUPPRESS_MAP
:
  'unsuppress-map'
;

UPDATE_SOURCE
:
  'update-source'
;

UPGRADE
:
  'upgrade'
;

UPGRADEJOBSTATUSNOTIFY
:
  [Uu] [Pp] [Gg] [Rr] [Aa] [Dd] [Ee] [Jj] [Oo] [Bb] [Ss] [Tt] [Aa] [Tt] [Uu]
  [Ss] [Nn] [Oo] [Tt] [Ii] [Ff] [Yy]
;

UPGRADEOPNOTIFYONCOMPLETION
:
  [Uu] [Pp] [Gg] [Rr] [Aa] [Dd] [Ee] [Oo] [Pp] [Nn] [Oo] [Tt] [Ii] [Ff] [Yy]
  [Oo] [Nn] [Cc] [Oo] [Mm] [Pp] [Ll] [Ee] [Tt] [Ii] [Oo] [Nn]
;

UPPER
:
  'upper'
;

URG
:
  'urg'
;

USE_ACL
:
  'use-acl' -> pushMode ( M_Word )
;

USE_VRF
:
  'use-vrf' -> pushMode ( M_Word )
;

USER
:
  'user'
  {
    if (lastTokenType() == SNMP_SERVER) {
      pushMode(M_SnmpUser);
    }
  }
;

USERNAME
:
  'username' -> pushMode ( M_Remark )
;

UUCP
:
  'uucp'
;

V3_REPORT_SUPPRESSION
:
  'v3-report-suppression'
;

VERSION
:
  'version'
  // If preceded by 'traps', snmp version follows.
  // Otherwise, arbitrary version string follows.
  {
    switch(lastTokenType()) {
      case TRAPS:
        pushMode(M_SnmpVersion);
        break;
      case HSRP:
        pushMode(M_HsrpVersion);
        break;
      default:
        pushMode(M_Remark);
        break;
    }
  }
;

VIOLATE
:
  'violate'
;

VIRTUAL_LINK
:
  'virtual-link'
;

VLAN
:
  [Vv] [Ll] [Aa] [Nn]
;

VN_SEGMENT
:
  'vn-segment'
;

VN_SEGMENT_VLAN_BASED
:
  'vn-segment-vlan-based'
;

VNI
:
  'vni'
;

VPNV4
:
  'vpnv4'
;

VPNV6
:
  'vpnv6'
;

VRF
:
  'vrf' -> pushMode ( M_Vrf )
;

WAIT_FOR
:
  'wait-for'
;

WAIT_IGP_CONVERGENCE
:
  'wait-igp-convergence'
;

WARNING_ONLY
:
  'warning-only'
;

WEIGHT
:
  'weight'
;

WHOIS
:
  'whois'
;

WHO
:
  'who'
;

WITHDRAW
:
  'withdraw'
;

WWW
:
  'www'
;

XCONNECT
:
  'xconnect'
;

XDMCP
:
  'xdmcp'
;

// Other Tokens

ASTERISK
:
  '*'
;

BLANK_LINE
:
  (
    F_Whitespace
  )* F_Newline
  {lastTokenType() == NEWLINE|| lastTokenType() == -1}?

  F_Newline* -> channel ( HIDDEN )
;

COLON
:
  ':'
;

COMMA
:
  ','
;

COMMENT_LINE
:
  (
    F_Whitespace
  )* [!#]
  {lastTokenType() == NEWLINE || lastTokenType() == -1}?

  F_NonNewline*
  (
    F_Newline+
    | EOF
  ) -> channel ( HIDDEN )
;

DASH
:
  '-'
;

DOUBLE_QUOTE
:
  '"' -> pushMode ( M_DoubleQuote )
;

FORWARD_SLASH
:
  '/'
;

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

NEWLINE
:
  F_Newline+
;

PERIOD
:
  '.'
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
F_StandardCommunity
:
  F_Uint16 ':' F_Uint16
;

fragment
F_SubnetMask
:
  F_SubnetMaskOctet '.0.0.0'
  | '255.' F_SubnetMaskOctet . '.0.0'
  | '255.255.' F_SubnetMaskOctet . '.0'
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

mode M_AaaGroup;

M_AaaGroup_LOCAL
:
  'local' -> type ( LOCAL ) , popMode
;

M_AaaGroup_WORD
:
  F_Word -> type ( WORD )
;

M_AaaGroup_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;


mode M_AliasName;

M_AliasName_WORD
:
  F_Word -> type ( WORD ) , mode ( M_Remark )
;

M_AliasName_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Banner;

M_Banner_EXEC
:
  'exec' -> type ( EXEC ) , mode ( M_BannerDelimiter )
;

M_Banner_MOTD
:
  'motd' -> type ( MOTD ) , mode ( M_BannerDelimiter )
;

M_Banner_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Banner_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_BannerDelimiter;

M_BannerDelimiter_BANNER_DELIMITER
:
  F_NonWhitespace
  {
    setBannerDelimiter();
  }

  -> type ( BANNER_DELIMITER ) , mode ( M_BannerText )
;

M_BannerDelimiter_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_BannerText;

M_BannerText_BANNER_DELIMITER
:
  {bannerDelimiterFollows()}?

  .
  {
    unsetBannerDelimiter();
  }

  -> type ( BANNER_DELIMITER ) , mode ( M_BannerCleanup )
;

M_BannerText_BODY
:
  .
  {
    if (bannerDelimiterFollows()) {
      setType(BANNER_BODY);
    } else {
      more();
    }
  }

;

mode M_BannerCleanup;

M_BannerCleanup_IGNORED
:
  F_NonNewline+ -> channel ( HIDDEN )
;

M_BannerCleanup_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ) , popMode
;

mode M_Class;

M_Class_TYPE
:
  'type' -> type ( TYPE ) , mode ( M_ClassType )
;

M_Class_WORD
:
  F_NonWhitespace+ -> type ( WORD ) , popMode
;

M_Class_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_ClassType;

// control-plane omitted on purpose

M_ClassType_NETWORK_QOS
:
  'network-qos' -> type ( NETWORK_QOS ) , mode ( M_Word )
;

M_ClassType_QOS
:
  'qos' -> type ( QOS ) , mode ( M_Word )
;

M_ClassType_QUEUEING
:
  'queueing' -> type ( QUEUEING ) , mode ( M_Word )
;

M_ClassType_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_ClassMap;

M_ClassMap_MATCH_ALL
:
  'match-all' -> type ( MATCH_ALL ) , mode ( M_Word )
;

M_ClassMap_MATCH_ANY
:
  'match-any' -> type ( MATCH_ANY ) , mode ( M_Word )
;

M_ClassMap_TYPE
:
  'type' -> type ( TYPE ) , mode ( M_ClassMapType )
;

M_ClassMap_WORD
:
  F_NonWhitespace+ -> type ( WORD ) , popMode
;

M_ClassMap_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_ClassMapType;

M_ClassMapType_CONTROL_PLANE
:
  'control-plane' -> type ( CONTROL_PLANE ) , mode ( M_ClassMapType2 )
;

M_ClassMapType_NETWORK_QOS
:
  'network-qos' -> type ( NETWORK_QOS ) , mode ( M_ClassMapType2 )
;

M_ClassMapType_QOS
:
  'qos' -> type ( QOS ) , mode ( M_ClassMapType2 )
;

M_ClassMapType_QUEUEING
:
  'queueing' -> type ( QUEUEING ) , mode ( M_ClassMapType2 )
;

M_ClassMapType_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_ClassMapType2;

M_ClassMapType2_MATCH_ALL
:
  'match-all' -> type ( MATCH_ALL ) , mode ( M_Word )
;

M_ClassMapType2_MATCH_ANY
:
  'match-any' -> type ( MATCH_ANY ) , mode ( M_Word )
;

M_ClassMapType2_WORD
:
  F_NonWhitespace+ -> type ( WORD ) , popMode
;

M_ClassMapType2_WS
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

mode M_Hostname;

M_Hostname_SUBDOMAIN_NAME
:
  (
    (
      [A-Za-z0-9_]
      | '-'
    )+ '.'
  )*
  (
    [A-Za-z0-9_]
    | '-'
  )+ -> type ( SUBDOMAIN_NAME ) , popMode
;

M_Hostname_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_HsrpVersion;

M_HsrpVersion_VERSION_1
:
  '1' -> type ( HSRP_VERSION_1 ) , popMode
;

M_HsrpVersion_VERSION_2
:
  '2' -> type ( HSRP_VERSION_2 ) , popMode
;

M_HsrpVersion_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_MatchIpAddress;

M_MatchIpAddress_PREFIX_LIST
:
  'prefix-list' -> type ( PREFIX_LIST ) , mode ( M_Words )
;

M_MatchIpAddress_WORD
:
  F_Word -> type ( WORD ) , mode ( M_Words )
;

M_MatchIpAddress_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Password;

M_Password_PASSWORD_0
:
  '0' ' '+ -> type ( PASSWORD_0 )
;

M_Password_PASSWORD_3
:
  '3' ' '+ -> type ( PASSWORD_3 ) , mode ( M_Password3 )
;

M_Password_PASSWORD_7
:
  '7' ' '+ -> type ( PASSWORD_7 ) , mode ( M_Password7 )
;

M_Password_PASSWORD_0_TEXT
:
  F_NonNewline+ -> type ( PASSWORD_0_TEXT ) , popMode
;

mode M_Password3;

M_Password3_PASSWORD_3_TEXT
:
// TODO: differentiate from malformed
  F_NonNewline+ -> type ( PASSWORD_3_TEXT ) , popMode
;

M_Password3_PASSWORD_3_MALFORMED_TEXT
:
  F_NonNewline+ -> type ( PASSWORD_3_MALFORMED_TEXT ) , popMode
;

mode M_Password7;

M_Password7_PASSWORD_7_TEXT
:
// TODO: differentiate from malformed
  F_NonNewline+ -> type ( PASSWORD_7_TEXT ) , popMode
;

M_Password7_PASSWORD_7_MALFORMED_TEXT
:
  F_NonNewline+ -> type ( PASSWORD_7_MALFORMED_TEXT ) , popMode
;

mode M_PolicyMap;

M_PolicyMap_TYPE
:
  'type' -> type ( TYPE ) , mode ( M_PolicyMapType )
;

M_PolicyMap_WORD
:
  F_NonWhitespace+ -> type ( WORD ) , popMode
;

M_PolicyMap_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_PolicyMapType;

M_PolicyMapType_CONTROL_PLANE
:
  'control-plane' -> type ( CONTROL_PLANE ) , mode ( M_Word )
;

M_PolicyMapType_NETWORK_QOS
:
  'network-qos' -> type ( NETWORK_QOS ) , mode ( M_Word )
;

M_PolicyMapType_QOS
:
  'qos' -> type ( QOS ) , mode ( M_Word )
;

M_PolicyMapType_QUEUEING
:
  'queueing' -> type ( QUEUEING ) , mode ( M_Word )
;

M_PolicyMapType_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Priv;

M_Priv_AES_128
:
  'aes-128' -> type ( AES_128 ) , mode ( M_Word )
;

M_Priv_WORD
:
  F_Word -> type ( WORD ) , popMode
;

M_Priv_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Remark;

M_Remark_REMARK_TEXT
:
  F_NonWhitespace F_NonNewline* -> type ( REMARK_TEXT ) , popMode
;

M_Remark_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_SnmpHostTraps;

M_SnmpHostTraps_VERSION
:
  'version' -> type ( VERSION ) , mode ( M_SnmpVersion )
;

M_SnmpHostTraps_WORD
:
  F_Word -> type ( WORD ) , popMode
;

M_SnmpHostTraps_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_SnmpUser;

M_SnmpUser_WORD
:
  F_Word -> type ( WORD ) , mode ( M_SnmpUserGroup )
;

M_SnmpUser_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_SnmpUserGroup;

M_SnmpUserGroup_AUTH
:
  'auth' -> type ( AUTH ) , popMode
;

M_SnmpUserGroup_WORD
:
  F_Word -> type ( WORD ) , popMode
;

M_SnmpUserGroup_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_SnmpVersion;

M_SnmpVersion_SNMP_VERSION_1
:
  '1' -> type ( SNMP_VERSION_1 ) , mode ( M_Word )
;

M_SnmpVersion_SNMP_VERSION_2
:
  '2' -> type ( SNMP_VERSION_2 ) , mode ( M_Word )
;

M_SnmpVersion_SNMP_VERSION_2C
:
  '2' [Cc] -> type ( SNMP_VERSION_2C ) , mode ( M_Word )
;

M_SnmpVersion_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Vrf;

M_Vrf_CONTEXT
:
  'context' -> type ( CONTEXT )
;

M_Vrf_MEMBER
:
  'member' -> type ( MEMBER )
;

M_Vrf_NEWLINE
:
  F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Vrf_WORD
:
  F_Word -> type ( WORD ) , popMode
;

M_Vrf_WS
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
