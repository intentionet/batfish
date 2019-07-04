lexer grammar FlatJuniperLexer;

options {
   superClass = 'org.batfish.grammar.BatfishLexer';
}

@header {
import static com.google.common.base.MoreObjects.firstNonNull;
}

@members {
boolean enableIPV6_ADDRESS = true;
boolean enableIP_ADDRESS = true;
boolean enableDEC = true;
boolean _markWildcards = false;
private Integer _overrideTokenStartLine;

public boolean isPrefix() {
   char nextChar = (char)this.getInputStream().LA(1);
   if(Character.isDigit(nextChar) || nextChar == '.'){
      return false;
    }
    return true;
}

@Override
public Token emit() {
  Token t = _factory.create(_tokenFactorySourcePair, _type, _text, _channel, _tokenStartCharIndex, getCharIndex()-1,
    firstNonNull(_overrideTokenStartLine, _tokenStartLine), _tokenStartCharPositionInLine);
  emit(t);
  return t;
}

@Override
public String printStateVariables() {
   StringBuilder sb = new StringBuilder();
   sb.append("enableIPV6_ADDRESS: " + enableIPV6_ADDRESS + "\n");
   sb.append("enableIP_ADDRESS: " + enableIP_ADDRESS + "\n");
   sb.append("enableDEC: " + enableDEC + "\n");
   sb.append("markWildcards: " + _markWildcards + "\n");
   return sb.toString();
}

public void setMarkWildcards(boolean markWildcards) {
   _markWildcards = markWildcards;
}

public void setOverrideTokenStartLine(Integer overrideTokenStartLine) {
  _overrideTokenStartLine = overrideTokenStartLine;
}

private void setWildcard() {
  setType(_markWildcards? WILDCARD_ARTIFACT : WILDCARD);
}

}

tokens {
   ACK,
   BANG,
   DYNAMIC_DB,
   FIN,
   INTERFACE_NAME,
   ISO_ADDRESS,
   PIPE,
   RST,
   SYN,
   VERSION_STRING,
   WILDCARD_ARTIFACT
}

// Juniper Keywords

ACCEPT
:
   'accept'
;

ACCEPT_DATA
:
   'accept-data'
;

ACCEPTED_PREFIX_LIMIT
:
   'accepted-prefix-limit'
;

ACCESS
:
   'access'
;

ACCESS_PROFILE
:
   'access-profile'
;

ACCOUNTING
:
   'accounting'
;

ACTIVE
:
   'active'
;

ACTIVE_SERVER_GROUP
:
   'active-server-group'
;

ADD
:
   'add'
;

ADD_PATH
:
   'add-path'
;

ADDRESS
:
   'address'
;

ADDRESS_BOOK
:
   'address-book'
;

ADDRESS_MASK
:
   'address-mask'
;

ADDRESS_SET
:
   'address-set'
;

ADVERTISE_EXTERNAL
:
   'advertise-external'
;

ADVERTISE_INACTIVE
:
   'advertise-inactive'
;

ADVERTISE_INTERVAL
:
   'advertise-interval'
;

ADVERTISE_PEER_AS
:
   'advertise-peer-as'
;

AFS
:
   'afs'
;

AGGREGATE
:
   'aggregate'
;

AGGREGATED_ETHER_OPTIONS
:
   'aggregated-ether-options'
;

AGGREGATOR
:
  'aggregator'
;

AGGRESSIVE
:
   'aggressive'
;

AES_128_CBC
:
   'aes-128-cbc'
;

AES_128_CMAC_96
:
   'aes-128-cmac-96'
;

AES_128_GCM
:
   'aes-128-gcm'
;


AES_192_CBC
:
   'aes-192-cbc'
;

AES_192_GCM
:
   'aes-192-gcm'
;

AES_256_CBC
:
   'aes-256-cbc'
;

AES_256_GCM
:
   'aes-256-gcm'
;

AH
:
   'ah'
;

AH_HEADER
:
   'AH-header'
;

ALARM_WITHOUT_DROP
:
   'alarm-without-drop'
;

ALARM_THRESHOLD
:
   'alarm-threshold'
;

ALG
:
   'alg'
;

ALGORITHM
:
   'algorithm'
;

ALIAS
:
   'alias'
;

ALIASES
:
   'aliases'
;

ALL
:
   'all'
;

ALLOW
:
   'allow'
;

ALLOW_DUPLICATES
:
   'allow-duplicates'
;

ALLOW_SNOOPED_CLIENTS
:
   'allow-snooped-clients'
;

ALLOW_V4MAPPED_PACKETS
:
   'allow-v4mapped-packets'
;

ALWAYS_COMPARE_MED
:
   'always-compare-med'
;

ALWAYS_SEND
:
   'always-send'
;

ALWAYS_WRITE_GIADDR
:
   'always-write-giaddr'
;

ANALYZER
:
  'analyzer'
;

ANY
:
   'any'
;

ANY_IPV4
:
   'any-ipv4'
;

ANY_IPV6
:
   'any-ipv6'
;

ANY_REMOTE_HOST
:
   'any-remote-host'
;

ANY_SERVICE
:
   'any-service'
;

APPLICATION
:
   'application'
;

APPLICATION_PROTOCOL
:
   'application-protocol'
;

APPLICATION_SET
:
   'application-set'
;

APPLICATION_TRACKING
:
   'application-tracking'
;

APPLICATIONS
:
   'applications'
;

APPLY_GROUPS
:
   'apply-groups'
;

APPLY_GROUPS_EXCEPT
:
   'apply-groups-except'
;

APPLY_MACRO
:
   'apply-macro'
;

APPLY_PATH
:
   'apply-path'
;

ARCHIVE
:
   'archive'
;

AREA
:
   'area'
;

AREA_RANGE
:
   'area-range'
;

ARP
:
   'arp'
;

ARP_RESP
:
   'arp-resp'
;

AS_OVERRIDE
:
   'as-override'
;

AS_PATH
:
   'as-path' -> pushMode ( M_AsPath )
;

AS_PATH_EXPAND
:
   'as-path-expand'
;

AS_PATH_GROUP
:
   'as-path-group' -> pushMode ( M_AsPathGroup )
;

AS_PATH_PREPEND
:
   'as-path-prepend' -> pushMode ( M_AsPathPrepend )
;

ASCII_TEXT
:
   'ascii-text'
;

ASDOT_NOTATION
:
   'asdot-notation'
;

ATTACK_THRESHOLD
:
   'attack-threshold'
;

ATTACH
:
   'attach'
;

AUTHENTICATION
:
   'authentication'
;

AUTHENTICATION_ALGORITHM
:
   'authentication-algorithm'
;

AUTHENTICATION_KEY
:
   'authentication-key'
;

AUTHENTICATION_KEY_CHAIN
:
   'authentication-key-chain'
;

AUTHENTICATION_KEY_CHAINS
:
   'authentication-key-chains'
;

AUTHENTICATION_METHOD
:
   'authentication-method'
;

AUTHENTICATION_ORDER
:
   'authentication-order'
;

AUTHENTICATION_TYPE
:
   'authentication-type'
;

AUTHORIZATION
:
   'authorization'
;

AUTHORIZED_KEYS_COMMAND
:
   'authorized-keys-command'
;

AUTHORIZED_KEYS_COMMAND_USER
:
   'authorized-keys-command-user'
;

AUTO
:
   'auto'
;

AUTO_EXPORT
:
   'auto-export'
;

AUTO_NEGOTIATION
:
   'auto-negotiation'
;

AUTO_SNAPSHOT
:
  'auto-snapshot'
;

AUTONOMOUS_SYSTEM
:
   'autonomous-system'
;

AUXILIARY
:
   'auxiliary'
;

BAD_INNER_HEADER
:
   'bad-inner-header'
;

BAD_OPTION
:
   'bad-option'
;

BACKUP_ROUTER
:
   'backup-router'
;

BANDWIDTH
:
   'bandwidth' -> pushMode ( M_Bandwidth )
;

BASIC
:
   'basic'
;

BFD
:
   'bfd'
;

BFD_LIVENESS_DETECTION
:
   'bfd-liveness-detection'
;

BGP
:
   'bgp'
;

BIFF
:
   'biff'
;

BIND_INTERFACE
:
   'bind-interface' -> pushMode ( M_Interface )
;

BLOCK_FRAG
:
   'block-frag'
;

BMP
:
   'bmp'
;

BOOT_SERVER
:
   'boot-server'
;

BOOTP
:
   'bootp'
;

BOOTP_SUPPORT
:
   'bootp-support'
;

BOOTPC
:
   'bootpc'
;

BOOTPS
:
   'bootps'
;

BRIDGE
:
   'bridge'
;

BRIDGE_DOMAINS
:
   'bridge-domains'
;

BROADCAST_CLIENT
:
   'broadcast-client'
;

BUNDLE
:
   'bundle'
;

C
:
    'c'
;

CALIPSO_OPTION
:
   'CALIPSO-option'
;

CATEGORIES
:
   'categories'
;

CCC
:
   'ccc'
;

CERTIFICATES
:
   'certificates'
;

CHANGE_LOG
:
    'change-log'
;

CHASSIS
:
   'chassis'
;

CIPHERS
:
   'ciphers'
;

CLASS
:
   'class'
;

CLASS_OF_SERVICE
:
   'class-of-service'
;

CLEAR
:
   'clear'
;

CLIENT
:
   'client'
;

CLIENT_ALIVE_COUNT_MAX
:
   'client-alive-count-max'
;

CLIENT_ALIVE_INTERVAL
:
   'client-alive-interval'
;

CLIENT_LIST
:
   'client-list'
;

CLIENT_LIST_NAME
:
   'client-list-name'
;

CLIENTS
:
   'clients'
;

CLUSTER
:
   'cluster'
;

CMD
:
   'cmd'
;

COLOR
:
   'color'
;

COLOR2
:
   'color2'
;

COMMIT
:
   'commit'
;

COMMUNICATION_PROHIBITED_BY_FILTERING
:
   'communication-prohibited-by-filtering'
;

COMMUNITY
:
   'community'
   {
      enableIPV6_ADDRESS = false;
   }

;

COMPATIBLE
:
   'compatible'
;

COMPRESS_CONFIGURATION_FILES
:
   'compress-configuration-files'
;

CONDITION
:
   'condition'
;

CONFEDERATION
:
   'confederation'
;

CONNECTIONS
:
   'connections'
;

CONNECTION_LIMIT
:
   'connection-limit'
;

CONNECTIONS_LIMIT
:
   'connections-limit'
;

CONSOLE
:
   'console'
;

CONTACT
:
   'contact'
;

COS_NEXT_HOP_MAP
:
   'cos-next-hop-map'
;

COUNT
:
   'count'
;

CREDIBILITY_PROTOCOL_PREFERENCE
:
   'credibility-protocol-preference'
;

CVSPSERVER
:
   'cvspserver'
;

CWR
:
   'cwr'
;

DAEMON
:
   'daemon'
;

DAMPING
:
   'damping'
;

DATABASE_REPLICATION
:
   'database-replication'
;

DESTINATION_HEADER
:
   'destination-header'
;

DESTINATION_THRESHOLD
:
   'destination-threshold'
;

DCBX
:
   'dcbx'
;

DDOS_PROTECTION
:
   'ddos-protection'
;

DEACTIVATE
:
   'deactivate'
;

DEAD_INTERVAL
:
   'dead-interval'
;

DEAD_PEER_DETECTION
:
   'dead-peer-detection'
;

DEFAULT_ACTION
:
   'default-action'
;

DEFAULT_ADDRESS_SELECTION
:
   'default-address-selection'
;

DEFAULT_GATEWAY
:
   'default-gateway'
;

DEFAULT_LSA
:
   'default-lsa'
;

DEFAULT_METRIC
:
   'default-metric'
;

DEFAULT_POLICY
:
   'default-policy'
;

DEFAULTS
:
   'defaults'
;

DELETE
:
   'delete'
;

DELETE_BINDING_ON_RENEGOTIATION
:
   'delete-binding-on-renegotiation'
;

DENY
:
   'deny'
;

DENY_ALL
:
   'deny-all'
;

DES_CBC
:
   'des-cbc'
;

DESCRIPTION
:
   'description' -> pushMode ( M_Description )
;

DESIGNATED_FORWARDER_ELECTION_HOLD_TIME
:
   'designated-forwarder-election-hold-time'
;

DESTINATION
:
   'destination'
;

DESTINATION_ADDRESS
:
   'destination-address'
;

DESTINATION_ADDRESS_EXCLUDED
:
   'destination-address-excluded'
;

DESTINATION_ADDRESS_NAME
:
    'destination-address-name'
;

DESTINATION_HOST_PROHIBITED
:
   'destination-host-prohibited'
;

DESTINATION_HOST_UNKNOWN
:
   'destination-host-unknown'
;

DESTINATION_IP
:
   'destination-ip'
;

DESTINATION_IP_BASED
:
   'destination-ip-based'
;

DESTINATION_NAT
:
   'destination-nat'
;

DESTINATION_NETWORK_PROHIBITED
:
   'destination-network-prohibited'
;

DESTINATION_NETWORK_UNKNOWN
:
   'destination-network-unknown'
;

DESTINATION_PORT
:
   'destination-port'
;

DESTINATION_PORT_EXCEPT
:
   'destination-port-except'
;

DESTINATION_PREFIX_LIST
:
   'destination-prefix-list'
;

DESTINATION_UNREACHABLE
:
   'destination-unreachable'
;

DF_BIT
:
   'df-bit'
;

DH_GROUP
:
   'dh-group'
;

DHCP
:
   'dhcp'
;

DHCP_LOCAL_SERVER
:
   'dhcp-local-server'
;

DHCP_RELAY
:
   'dhcp-relay'
;

DIRECT
:
   'direct'
;

DISABLE
:
   'disable'
;

DISABLE_4BYTE_AS
:
   'disable-4byte-as'
;

DISCARD
:
   'discard'
;

DNS
:
   'dns'
;

DOMAIN
:
   'domain'
;

DOMAIN_NAME
:
   'domain-name'
;

DOMAIN_SEARCH
:
   'domain-search'
;

DROP_PATH_ATTRIBUTES
:
   'drop-path-attributes'
;

DSA_SIGNATURES
:
   'dsa-signatures'
;

DSCP
:
   'dscp' -> pushMode ( M_DSCP )
;

DSLITE
:
   'dslite'
;

DSTOPTS
:
   'dstopts'
;

DTCP_ONLY
:
   'dtcp-only'
;

DUMPONPANIC
:
   'dump-on-panic'
;

DVMRP
:
   'dvmrp'
;

DYNAMIC
:
   'dynamic'
;

DYNAMIC_DNS
:
   'dynamic-dns'
;

ECE
:
   'ece'
;

ECHO_REPLY
:
   'echo-reply'
;

ECHO_REQUEST
:
   'echo-request'
;

EGP
:
   'egp'
;

EGRESS
:
  'egress'
;

EIGHT02_3AD
:
   '802.3ad'
;

EKLOGIN
:
   'eklogin'
;

EKSHELL
:
   'ekshell'
;

ELIGIBLE
:
   'eligible'
;

ENABLE
:
   'enable'
;

ENCAPSULATION
:
   'encapsulation'
;

ENCRYPTED_PASSWORD
:
   'encrypted-password'
;

ENCRYPTION_ALGORITHM
:
   'encryption-algorithm'
;

ENFORCE_FIRST_AS
:
   'enforce-first-as'
;

ENHANCED_HASH_KEY
:
   'enhanced-hash-key'
;

ESP
:
   'esp'
;

ESP_HEADER
:
   'ESP-header'
;

ESTABLISH_TUNNELS
:
   'establish-tunnels'
;

ETHER_OPTIONS
:
   'ether-options'
;

ETHER_TYPE
:
   'ether-type'
;

ETHERNET_SWITCHING
:
   'ethernet-switching'
;

ETHERNET_SWITCHING_OPTIONS
:
   'ethernet-switching-options'
;

EVENT_OPTIONS
:
   'event-options'
;

EVPN
:
   'evpn'
;

EXACT
:
   'exact'
;

EXCEPT
:
   'except'
;

EXEC
:
   'exec'
;

EXP
:
   'exp'
;

EXPLICIT_PRIORITY
:
   'explicit-priority'
;

EXPORT
:
   'export'
;

EXPORT_RIB
:
   'export-rib'
;

EXPRESSION
:
   'expression'
;

EXTENDED_VNI_LIST
:
   'extended-vni-list'
;

EXTENSIBLE_SUBSCRIBER
:
   'extensible-subscriber'
;

EXTENSION_SERVICE
:
   'extension-service'
;

EXTERNAL
:
   'external'
;

EXTERNAL_INTERFACE
:
   'external-interface' -> pushMode ( M_Interface )
;

EXTERNAL_PREFERENCE
:
   'external-preference'
;

EXTERNAL_ROUTER_ID
:
   'external-router-id'
;

EXTENSION_HEADER
:
    'extension-header'
;

EXTENSIONS
:
   'extensions'
;

FABRIC
:
   'fabric'
;

FABRIC_OPTIONS
:
   'fabric-options'
;

FACILITY_OVERRIDE
:
   'facility-override'
;

FAIL_FILTER
:
   'fail-filter'
;

FAMILY
:
   'family'
;

FASTETHER_OPTIONS
:
   'fastether-options'
;

FILE
:
   'file'
;

FILTER
:
   'filter'
;

FILTER_DUPLICATES
:
    'filter-duplicates'
;

FILTER_INTERFACES
:
    'filter-interfaces'
;

FIN_NO_ACK
:
   'fin-no-ack'
;

FINGER
:
   'finger'
;

FINGERPRINT_HASH
:
   'fingerprint-hash'
;

FIREWALL
:
   'firewall'
;

FIRST_FRAGMENT
:
   'first-fragment'
;

FLEXIBLE_VLAN_TAGGING
:
   'flexible-vlan-tagging'
;

FLOOD
:
   'flood'
;

FLOW
:
   'flow'
;

FLOW_CONTROL
:
   'flow-control'
;

FLOW_GATE
:
   'flow-gate'
;

FLOW_SESSION
:
   'flow-session'
;

FORCE_UP
:
   'force-up'
;

FOREVER
:
   'forever'
;

FORWARD_SNOOPED_CLIENTS
:
    'forward-snooped-clients'
;

FORWARDING
:
   'forwarding'
;

FORWARDING_CLASS
:
   'forwarding-class'
;

FORWARDING_CLASS_ACCOUNTING
:
   'forwarding-class-accounting'
;

FORWARDING_OPTIONS
:
   'forwarding-options'
;

FORWARDING_TABLE
:
   'forwarding-table'
;

FRAGMENT
:
   'fragment'
;

FRAGMENT_HEADER
:
   'fragment-header'
;

FRAGMENTATION_NEEDED
:
   'fragmentation-needed'
;

FRAGMENT_OFFSET
:
   'fragment-offset'
;

FRAGMENT_OFFSET_EXCEPT
:
   'fragment-offset-except'
;

FRAMING
:
   'framing'
;

FROM
:
   'from'
;

FROM_ZONE
:
   'from-zone'
;

FTP
:
   'ftp'
;

FTP_DATA
:
   'ftp-data'
;

FULL_DUPLEX
:
   'full-duplex'
;

G
:
   'g'
;

GATEWAY
:
   'gateway'
;

GENERATE
:
   'generate'
;

GIGETHER_OPTIONS
:
   'gigether-options'
;

GLOBAL
:
  'global'
;

GRACEFUL_RESTART
:
   'graceful-restart'
;

GRE
:
   'gre'
;

GRE_4IN4
:
   'gre-4in4'
;

GRE_4IN6
:
   'gre-4in6'
;

GRE_6IN4
:
   'gre-6in4'
;

GRE_6IN6
:
   'gre-6in6'
;

GROUP
:
   'group' -> pushMode ( M_VarOrWildcard )
;

GROUP_IKE_ID
:
   'group-ike-id'
;

GROUP1
:
   'group1'
;

GROUP14
:
   'group14'
;

GROUP15
:
   'group15'
;

GROUP16
:
   'group16'
;

GROUP19
:
   'group19'
;

GROUP2
:
   'group2'
;

GROUP20
:
   'group20'
;

GROUP24
:
   'group24'
;

GROUP5
:
   'group5'
;

GROUPS
:
   'groups'
;

HASH_KEY
:
   'hash-key'
;

HELLO_AUTHENTICATION_KEY
:
   'hello-authentication-key'
;

HELLO_AUTHENTICATION_TYPE
:
   'hello-authentication-type'
;

HELLO_INTERVAL
:
   'hello-interval'
;

HELLO_PADDING
:
   'hello-padding'
;

HELPERS
:
   'helpers'
;

HIGH
:
   'high'
;

HIP_HEADER
:
   'HIP-header'
;

HOME_ADDRESS_OPTION
:
   'home-address-option'
;

HOP_BY_HOP_HEADER
:
   'hop-by-hop-header'
;

HMAC_MD5_96
:
   'hmac-md5-96'
;

HMAC_SHA1
:
   'hmac-sha-1'
;

HMAC_SHA1_96
:
   'hmac-sha1-96'
;

HMAC_SHA_1_96
:
   'hmac-sha-1-96'
;

HOLD_TIME
:
   'hold-time'
;

HOP_BY_HOP
:
   'hop-by-hop'
;

HOST
:
   'host'
;

HOST_INBOUND_TRAFFIC
:
   'host-inbound-traffic'
;

HOST_NAME
:
   'host-name'
;

HOST_PRECEDENCE_VIOLATION
:
   'host-precedence-violation'
;

HOST_UNREACHABLE
:
   'host-unreachable'
;

HOST_UNREACHABLE_FOR_TOS
:
   'host-unreachable-for-tos'
;

HOSTKEY_ALGORITHM
:
   'hostkey-algorithm'
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

ICCP
:
   'iccp'
;

ICMP
:
   'icmp'
;

ICMP_CODE
:
   'icmp-code'
;

ICMP_TYPE
:
   'icmp-type'
;

ICMP6
:
   'icmp6'
;

ICMP6_CODE
:
   'icmp6-code'
;

ICMPV6_MALFORMED
:
   'icmpv6-malformed'
;

ICMP6_TYPE
:
   'icmp6-type'
;

ICMPV6
:
   'icmpv6'
;

IDENT
:
   'ident'
;

IDENT_RESET
:
   'ident-reset'
;

IDLE_TIMEOUT
:
   'idle-timeout'
;

IDS_OPTION
:
   'ids-option'
;

IGMP
:
   'igmp'
;

IGMP_SNOOPING
:
   'igmp-snooping'
;

IGNORE
:
   'ignore'
;

IGNORE_L3_INCOMPLETES
:
   'ignore-l3-incompletes'
;

IGP
:
   'igp'
;

IKE
:
   'ike'
;

IKE_ESP_NAT
:
   'ike-esp-nat'
;

IKE_POLICY
:
   'ike-policy'
;

IKE_USER_TYPE
:
   'ike-user-type'
;

ILNP_NONCE_OPTION
:
   'ILNP-nonce-option'
;

IMAP
:
   'imap'
;

IMMEDIATELY
:
   'immediately'
;

IMPORT
:
   'import'
;

IMPORT_POLICY
:
   'import-policy'
;

IMPORT_RIB
:
   'import-rib'
;

INACTIVE
:
   'inactive'
;

INACTIVITY_TIMEOUT
:
   'inactivity-timeout'
;

INCLUDE_MP_NEXT_HOP
:
   'include-mp-next-hop'
;

INCOMPLETE
:
   'incomplete'
;

INDIRECT_NEXT_HOP
:
   'indirect-next-hop'
;

INDIRECT_NEXT_HOP_CHANGE_ACKNOWLEDGEMENTS
:
   'indirect-next-hop-change-acknowledgements'
;

INET
:
   'inet'
;

INET6
:
   'inet6'
;

INET_MDT
:
   'inet-mdt'
;

INET_MVPN
:
   'inet-mvpn'
;

INET6_MVPN
:
   'inet6-mvpn'
;

INET_VPN
:
   'inet-vpn'
;

INET6_VPN
:
   'inet6-vpn'
;

INFO_REPLY
:
  'info-reply'
;

INFO_REQUEST
:
  'info-request'
;

INGRESS
:
  'ingress'
;

INGRESS_REPLICATION
:
  'ingress-replication'
;

INNER
:
   'inner'
;

INPUT
:
   'input'
;

INPUT_LIST
:
   'input-list'
;

INPUT_VLAN_MAP
:
   'input-vlan-map'
;

INSECURE
:
   'insecure'
;

INSTALL
:
   'install'
;

INSTALL_NEXTHOP
:
   'install-nexthop'
;

INSTANCE
:
   'instance'
;

INSTANCE_IMPORT
:
   'instance-import'
;

INSTANCE_TYPE
:
   'instance-type'
;

INTERACTIVE_COMMANDS
:
   'interactive-commands'
;

INTERCONNECT_DEVICE
:
   'interconnect-device'
;

INTERFACE
:
   'interface' -> pushMode ( M_Interface )
;

INTERFACE_MODE
:
   'interface-mode'
;

INTERFACE_RANGE
:
   'interface-range'
;

INTERFACE_SPECIFIC
:
   'interface-specific'
;

INTERFACE_SWITCH
:
   'interface-switch'
;

INTERFACE_TRANSMIT_STATISTICS
:
   'interface-transmit-statistics'
;

INTERFACES
:
   'interfaces' -> pushMode ( M_Interface )
;

INTERFACE_ROUTES
:
   'interface-routes'
;

INTERFACE_TYPE
:
   'interface-type'
;

INTERNAL
:
   'internal'
;

INTERNET_OPTIONS
:
   'internet-options'
;

INVERT_MATCH
:
   'invert-match'
;

IP
:
   'ip'
;

IP_DESTINATION_ADDRESS
:
   'ip-destination-address'
;

IP_HEADER_BAD
:
   'ip-header-bad'
;

IP_IN_UDP
:
   'ip-in-udp'
;

IP_OPTIONS
:
   'ip-options'
;

IP_PROTOCOL
:
   'ip-protocol'
;

IP_SOURCE_ADDRESS
:
   'ip-source-address'
;

IP_SWEEP
:
   'ip-sweep'
;

IPIP
:
   'ipip'
;

IPIP_4IN4
:
   'ipip-4in4'
;

IPIP_4IN6
:
   'ipip-4in6'
;

IPIP_6IN4
:
   'ipip-6in4'
;

IPIP_6IN6
:
   'ipip-6in6'
;

IPIP_6OVER4
:
   'ipip-6over4'
;

IPIP_6TO4RELAY
:
   'ipip-6to4relay'
;

IPSEC
:
   'ipsec'
;

IPSEC_POLICY
:
   'ipsec-policy'
;

IPSEC_VPN
:
   'ipsec-vpn'
;

IPV6
:
   'ipv6'
;

IPV6_EXTENSION_HEADER
:
   'ipv6-extension-header'
;

IPV6_EXTENSION_HEADER_LIMIT
:
   'ipv6-extension-header-limit'
;

IPV6_MALFORMED_HEADER
:
   'ipv6-malformed-header'
;

IS_FRAGMENT
:
   'is-fragment'
;

ISATAP
:
   'isatap'
;

ISIS
:
   'isis'
;

ISIS_ENHANCED
:
   'isis-enhanced'
;

ISO
:
   'iso' -> pushMode ( M_ISO )
;

ISO_VPN
:
   'iso-vpn'
;

JUMBO_PAYLOAD_OPTION
:
   'jumbo-payload-option'
;

JUNOS_AOL
:
   'junos-aol'
;

JUNOS_BGP
:
   'junos-bgp'
;

JUNOS_BIFF
:
   'junos-biff'
;

JUNOS_BOOTPC
:
   'junos-bootpc'
;

JUNOS_BOOTPS
:
   'junos-bootps'
;

JUNOS_CHARGEN
:
   'junos-chargen'
;

JUNOS_CIFS
:
   'junos-cifs'
;

JUNOS_CVSPSERVER
:
   'junos-cvspserver'
;

JUNOS_DHCP_CLIENT
:
   'junos-dhcp-client'
;

JUNOS_DHCP_RELAY
:
   'junos-dhcp-relay'
;

JUNOS_DHCP_SERVER
:
   'junos-dhcp-server'
;

JUNOS_DISCARD
:
   'junos-discard'
;

JUNOS_DNS_TCP
:
   'junos-dns-tcp'
;

JUNOS_DNS_UDP
:
   'junos-dns-udp'
;

JUNOS_ECHO
:
   'junos-echo'
;

JUNOS_FINGER
:
   'junos-finger'
;

JUNOS_FTP
:
   'junos-ftp'
;

JUNOS_FTP_DATA
:
    'junos-ftp-data'
;

JUNOS_GNUTELLA
:
   'junos-gnutella'
;

JUNOS_GOPHER
:
   'junos-gopher'
;

JUNOS_GPRS_GTP_C
:
    'junos-gprs-gtp-c'
;


JUNOS_GPRS_GTP_U
:
    'junos-gprs-gtp-u'
;


JUNOS_GPRS_GTP_V0
:
    'junos-gprs-gtp-v0'
;

JUNOS_GPRS_SCTP
:
    'junos-gprs-sctp'
;

JUNOS_GRE
:
   'junos-gre'
;

JUNOS_GTP
:
   'junos-gtp'
;

JUNOS_H323
:
   'junos-h323'
;

JUNOS_HOST
:
   'junos-host'
;

JUNOS_HTTP
:
   'junos-http'
;

JUNOS_HTTP_EXT
:
   'junos-http-ext'
;

JUNOS_HTTPS
:
   'junos-https'
;

JUNOS_ICMP_ALL
:
   'junos-icmp-all'
;

JUNOS_ICMP_PING
:
   'junos-icmp-ping'
;

JUNOS_ICMP6_ALL
:
   'junos-icmp6-all'
;

JUNOS_ICMP6_DST_UNREACH_ADDR
:
   'junos-icmp6-dst-unreach-addr'
;

JUNOS_ICMP6_DST_UNREACH_ADMIN
:
   'junos-icmp6-dst-unreach-admin'
;

JUNOS_ICMP6_DST_UNREACH_BEYOND
:
   'junos-icmp6-dst-unreach-beyond'
;

JUNOS_ICMP6_DST_UNREACH_PORT
:
   'junos-icmp6-dst-unreach-port'
;

JUNOS_ICMP6_DST_UNREACH_ROUTE
:
   'junos-icmp6-dst-unreach-route'
;

JUNOS_ICMP6_ECHO_REPLY
:
   'junos-icmp6-echo-reply'
;

JUNOS_ICMP6_ECHO_REQUEST
:
   'junos-icmp6-echo-request'
;

JUNOS_ICMP6_PACKET_TOO_BIG
:
   'junos-icmp6-packet-too-big'
;

JUNOS_ICMP6_PARAM_PROB_HEADER
:
   'junos-icmp6-param-prob-header'
;

JUNOS_ICMP6_PARAM_PROB_NEXTHDR
:
   'junos-icmp6-param-prob-nexthdr'
;

JUNOS_ICMP6_PARAM_PROB_OPTION
:
   'junos-icmp6-param-prob-option'
;

JUNOS_ICMP6_TIME_EXCEED_REASSEMBLY
:
   'junos-icmp6-time-exceed-reassembly'
;

JUNOS_ICMP6_TIME_EXCEED_TRANSIT
:
   'junos-icmp6-time-exceed-transit'
;

JUNOS_IDENT
:
   'junos-ident'
;

JUNOS_IKE
:
   'junos-ike'
;

JUNOS_IKE_NAT
:
   'junos-ike-nat'
;

JUNOS_IMAP
:
   'junos-imap'
;

JUNOS_IMAPS
:
   'junos-imaps'
;

JUNOS_INTERNET_LOCATOR_SERVICE
:
   'junos-internet-locator-service'
;

JUNOS_IRC
:
   'junos-irc'
;

JUNOS_L2TP
:
   'junos-l2tp'
;

JUNOS_LDAP
:
   'junos-ldap'
;

JUNOS_LDP_TCP
:
   'junos-ldp-tcp'
;

JUNOS_LDP_UDP
:
   'junos-ldp-udp'
;

JUNOS_LPR
:
   'junos-lpr'
;

JUNOS_MAIL
:
   'junos-mail'
;

JUNOS_MGCP
:
   'junos-mgcp'
;

JUNOS_MGCP_CA
:
   'junos-mgcp-ca'
;

JUNOS_MGCP_UA
:
   'junos-mgcp-ua'
;

JUNOS_MS_RPC
:
   'junos-ms-rpc'
;

JUNOS_MS_RPC_ANY
:
   'junos-ms-rpc-any'
;

JUNOS_MS_RPC_EPM
:
   'junos-ms-rpc-epm'
;

JUNOS_MS_RPC_IIS_COM
:
   'junos-ms-rpc-iis-com'
;

JUNOS_MS_RPC_IIS_COM_1
:
   'junos-ms-rpc-iis-com-1'
;

JUNOS_MS_RPC_IIS_COM_ADMINBASE
:
   'junos-ms-rpc-iis-com-adminbase'
;

JUNOS_MS_RPC_MSEXCHANGE
:
   'junos-ms-rpc-msexchange'
;

JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_NSP
:
   'junos-ms-rpc-msexchange-directory-nsp'
;

JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_RFR
:
   'junos-ms-rpc-msexchange-directory-rfr'
;

JUNOS_MS_RPC_MSEXCHANGE_INFO_STORE
:
   'junos-ms-rpc-msexchange-info-store'
;

JUNOS_MS_RPC_TCP
:
   'junos-ms-rpc-tcp'
;

JUNOS_MS_RPC_UDP
:
   'junos-ms-rpc-udp'
;

JUNOS_MS_RPC_UUID_ANY_TCP
:
   'junos-ms-rpc-uuid-any-tcp'
;

JUNOS_MS_RPC_UUID_ANY_UDP
:
   'junos-ms-rpc-uuid-any-udp'
;

JUNOS_MS_RPC_WMIC
:
   'junos-ms-rpc-wmic'
;

JUNOS_MS_RPC_WMIC_ADMIN
:
   'junos-ms-rpc-wmic-admin'
;

JUNOS_MS_RPC_WMIC_ADMIN2
:
   'junos-ms-rpc-wmic-admin2'
;

JUNOS_MS_RPC_WMIC_MGMT
:
   'junos-ms-rpc-wmic-mgmt'
;

JUNOS_MS_RPC_WMIC_WEBM_CALLRESULT
:
   'junos-ms-rpc-wmic-webm-callresult'
;

JUNOS_MS_RPC_WMIC_WEBM_CLASSOBJECT
:
   'junos-ms-rpc-wmic-webm-classobject'
;

JUNOS_MS_RPC_WMIC_WEBM_LEVEL1LOGIN
:
   'junos-ms-rpc-wmic-webm-level1login'
;

JUNOS_MS_RPC_WMIC_WEBM_LOGIN_CLIENTID
:
   'junos-ms-rpc-wmic-webm-login-clientid'
;

JUNOS_MS_RPC_WMIC_WEBM_LOGIN_HELPER
:
   'junos-ms-rpc-wmic-webm-login-helper'
;

JUNOS_MS_RPC_WMIC_WEBM_OBJECTSINK
:
   'junos-ms-rpc-wmic-webm-objectsink'
;

JUNOS_MS_RPC_WMIC_WEBM_REFRESHING_SERVICES
:
   'junos-ms-rpc-wmic-webm-refreshing-services'
;

JUNOS_MS_RPC_WMIC_WEBM_REMOTE_REFRESHER
:
   'junos-ms-rpc-wmic-webm-remote-refresher'
;

JUNOS_MS_RPC_WMIC_WEBM_SERVICES
:
   'junos-ms-rpc-wmic-webm-services'
;

JUNOS_MS_RPC_WMIC_WEBM_SHUTDOWN
:
   'junos-ms-rpc-wmic-webm-shutdown'
;

JUNOS_MS_SQL
:
   'junos-ms-sql'
;

JUNOS_MSN
:
   'junos-msn'
;

JUNOS_NBDS
:
   'junos-nbds'
;

JUNOS_NBNAME
:
   'junos-nbname'
;

JUNOS_NETBIOS_SESSION
:
   'junos-netbios-session'
;

JUNOS_NFS
:
   'junos-nfs'
;

JUNOS_NFSD_TCP
:
   'junos-nfsd-tcp'
;

JUNOS_NFSD_UDP
:
   'junos-nfsd-udp'
;

JUNOS_NNTP
:
   'junos-nntp'
;

JUNOS_NS_GLOBAL
:
   'junos-ns-global'
;

JUNOS_NS_GLOBAL_PRO
:
   'junos-ns-global-pro'
;

JUNOS_NSM
:
   'junos-nsm'
;

JUNOS_NTALK
:
   'junos-ntalk'
;

JUNOS_NTP
:
   'junos-ntp'
;

JUNOS_OSPF
:
   'junos-ospf'
;

JUNOS_PC_ANYWHERE
:
   'junos-pc-anywhere'
;

JUNOS_PERSISTENT_NAT
:
   'junos-persistent-nat'
;

JUNOS_PING
:
   'junos-ping'
;

JUNOS_PINGV6
:
   'junos-pingv6'
;

JUNOS_POP3
:
   'junos-pop3'
;

JUNOS_PPTP
:
   'junos-pptp'
;

JUNOS_PRINTER
:
   'junos-printer'
;

JUNOS_R2CP
:
   'junos-r2cp'
;

JUNOS_RADACCT
:
   'junos-radacct'
;

JUNOS_RADIUS
:
   'junos-radius'
;

JUNOS_REALAUDIO
:
   'junos-realaudio'
;

JUNOS_RIP
:
   'junos-rip'
;

JUNOS_ROUTING_INBOUND
:
   'junos-routing-inbound'
;

JUNOS_RSH
:
   'junos-rsh'
;

JUNOS_RTSP
:
   'junos-rtsp'
;

JUNOS_SCCP
:
   'junos-sccp'
;

JUNOS_SCTP_ANY
:
   'junos-sctp-any'
;

JUNOS_SIP
:
   'junos-sip'
;

JUNOS_SMB
:
   'junos-smb'
;

JUNOS_SMB_SESSION
:
   'junos-smb-session'
;

JUNOS_SMTP
:
   'junos-smtp'
;

JUNOS_SMTPS
:
    'junos-smtps'
;

JUNOS_SNMP_AGENTX
:
   'junos-snmp-agentx'
;

JUNOS_SNPP
:
   'junos-snpp'
;

JUNOS_SQL_MONITOR
:
   'junos-sql-monitor'
;

JUNOS_SQLNET_V1
:
   'junos-sqlnet-v1'
;

JUNOS_SQLNET_V2
:
   'junos-sqlnet-v2'
;

JUNOS_SSH
:
   'junos-ssh'
;

JUNOS_STUN
:
   'junos-stun'
;

JUNOS_SUN_RPC
:
   'junos-sun-rpc'
;

JUNOS_SUN_RPC_ANY
:
   'junos-sun-rpc-any'
;

JUNOS_SUN_RPC_ANY_TCP
:
   'junos-sun-rpc-any-tcp'
;

JUNOS_SUN_RPC_ANY_UDP
:
   'junos-sun-rpc-any-udp'
;

JUNOS_SUN_RPC_MOUNTD
:
   'junos-sun-rpc-mountd'
;

JUNOS_SUN_RPC_MOUNTD_TCP
:
   'junos-sun-rpc-mountd-tcp'
;

JUNOS_SUN_RPC_MOUNTD_UDP
:
   'junos-sun-rpc-mountd-udp'
;

JUNOS_SUN_RPC_NFS
:
   'junos-sun-rpc-nfs'
;

JUNOS_SUN_RPC_NFS_ACCESS
:
   'junos-sun-rpc-nfs-access'
;

JUNOS_SUN_RPC_NFS_TCP
:
   'junos-sun-rpc-nfs-tcp'
;

JUNOS_SUN_RPC_NFS_UDP
:
   'junos-sun-rpc-nfs-udp'
;

JUNOS_SUN_RPC_NLOCKMGR
:
   'junos-sun-rpc-nlockmgr'
;

JUNOS_SUN_RPC_NLOCKMGR_TCP
:
   'junos-sun-rpc-nlockmgr-tcp'
;

JUNOS_SUN_RPC_NLOCKMGR_UDP
:
   'junos-sun-rpc-nlockmgr-udp'
;

JUNOS_SUN_RPC_PORTMAP
:
   'junos-sun-rpc-portmap'
;

JUNOS_SUN_RPC_PORTMAP_TCP
:
   'junos-sun-rpc-portmap-tcp'
;

JUNOS_SUN_RPC_PORTMAP_UDP
:
   'junos-sun-rpc-portmap-udp'
;

JUNOS_SUN_RPC_RQUOTAD
:
   'junos-sun-rpc-rquotad'
;

JUNOS_SUN_RPC_RQUOTAD_TCP
:
   'junos-sun-rpc-rquotad-tcp'
;

JUNOS_SUN_RPC_RQUOTAD_UDP
:
   'junos-sun-rpc-rquotad-udp'
;

JUNOS_SUN_RPC_RUSERD
:
   'junos-sun-rpc-ruserd'
;

JUNOS_SUN_RPC_RUSERD_TCP
:
   'junos-sun-rpc-ruserd-tcp'
;

JUNOS_SUN_RPC_RUSERD_UDP
:
   'junos-sun-rpc-ruserd-udp'
;

JUNOS_SUN_RPC_SADMIND
:
   'junos-sun-rpc-sadmind'
;

JUNOS_SUN_RPC_SADMIND_TCP
:
   'junos-sun-rpc-sadmind-tcp'
;

JUNOS_SUN_RPC_SADMIND_UDP
:
   'junos-sun-rpc-sadmind-udp'
;

JUNOS_SUN_RPC_SPRAYD
:
   'junos-sun-rpc-sprayd'
;

JUNOS_SUN_RPC_SPRAYD_TCP
:
   'junos-sun-rpc-sprayd-tcp'
;

JUNOS_SUN_RPC_SPRAYD_UDP
:
   'junos-sun-rpc-sprayd-udp'
;

JUNOS_SUN_RPC_STATUS
:
   'junos-sun-rpc-status'
;

JUNOS_SUN_RPC_STATUS_TCP
:
   'junos-sun-rpc-status-tcp'
;

JUNOS_SUN_RPC_STATUS_UDP
:
   'junos-sun-rpc-status-udp'
;

JUNOS_SUN_RPC_TCP
:
   'junos-sun-rpc-tcp'
;

JUNOS_SUN_RPC_UDP
:
   'junos-sun-rpc-udp'
;

JUNOS_SUN_RPC_WALLD
:
   'junos-sun-rpc-walld'
;

JUNOS_SUN_RPC_WALLD_TCP
:
   'junos-sun-rpc-walld-tcp'
;

JUNOS_SUN_RPC_WALLD_UDP
:
   'junos-sun-rpc-walld-udp'
;

JUNOS_SUN_RPC_YPBIND
:
   'junos-sun-rpc-ypbind'
;

JUNOS_SUN_RPC_YPBIND_TCP
:
   'junos-sun-rpc-ypbind-tcp'
;

JUNOS_SUN_RPC_YPBIND_UDP
:
   'junos-sun-rpc-ypbind-udp'
;

JUNOS_SUN_RPC_YPSERV
:
   'junos-sun-rpc-ypserv'
;

JUNOS_SUN_RPC_YPSERV_TCP
:
   'junos-sun-rpc-ypserv-tcp'
;

JUNOS_SUN_RPC_YPSERV_UDP
:
   'junos-sun-rpc-ypserv-udp'
;

JUNOS_SYSLOG
:
   'junos-syslog'
;

JUNOS_TACACS
:
   'junos-tacacs'
;

JUNOS_TACACS_DS
:
   'junos-tacacs-ds'
;

JUNOS_TALK
:
   'junos-talk'
;

JUNOS_TCP_ANY
:
   'junos-tcp-any'
;

JUNOS_TELNET
:
   'junos-telnet'
;

JUNOS_TFTP
:
   'junos-tftp'
;

JUNOS_UDP_ANY
:
   'junos-udp-any'
;

JUNOS_UUCP
:
   'junos-uucp'
;

JUNOS_VDO_LIVE
:
   'junos-vdo-live'
;

JUNOS_VNC
:
   'junos-vnc'
;

JUNOS_WAIS
:
   'junos-wais'
;

JUNOS_WHO
:
   'junos-who'
;

JUNOS_WHOIS
:
   'junos-whois'
;

JUNOS_WINFRAME
:
   'junos-winframe'
;

JUNOS_WXCONTROL
:
   'junos-wxcontrol'
;

JUNOS_X_WINDOWS
:
   'junos-x-windows'
;

JUNOS_XNM_CLEAR_TEXT
:
   'junos-xnm-clear-text'
;

JUNOS_XNM_SSL
:
   'junos-xnm-ssl'
;

JUNOS_YMSG
:
   'junos-ymsg'
;

K
:
  'k'
;

KEEP
:
   'keep'
;

KERBEROS_SEC
:
   'kerberos-sec'
;

KERNEL
:
   'kernel'
;

KEY
:
   'key'
;

KEYS
:
   'keys'
;

KEY_CHAIN
:
   'key-chain'
;

KEY_EXCHANGE
:
   'key-exchange'
;

KLOGIN
:
   'klogin'
;

KPASSWD
:
   'kpasswd'
;

KRB_PROP
:
   'krb-prop'
;

KRBUPDATE
:
   'krbupdate'
;

KSHELL
:
   'kshell'
;

L
:
   'L'
;

L2CIRCUIT
:
   'l2circuit'
;

L2VPN
:
   'l2vpn'
;

L2_INTERFACE
:
  'l2-interface' -> pushMode(M_Interface)
;

L2_LEARNING
:
  'l2-learning'
;

L3_INTERFACE
:
   'l3-interface' -> pushMode(M_Interface)
;

LABEL_SWITCHED_PATH
:
   'label-switched-path'
;

LABELED_UNICAST
:
   'labeled-unicast'
;

LACP
:
   'lacp'
;

LAN
:
   'lan'
;

LAND
:
   'land'
;

LARGE
:
   'large'
;

LAST_AS
:
   'last-as'
;

LAYER2_CONTROL
:
   'layer2-control'
;

LDP_SYNCHRONIZATION
:
   'ldp-synchronization'
;

LICENSE
:
   'license'
;

LINE_IDENTIFICATION_OPTION
:
   'line-identification-option'
;

LINK_MODE
:
   'link-mode'
;

LDAP
:
   'ldap'
;

LDP
:
   'ldp'
;

LEARN_VLAN_1P_PRIORITY
:
   'learn-vlan-1p-priority'
;

LEVEL
:
   'level'
;

LIFETIME_KILOBYTES
:
   'lifetime-kilobytes'
;

LIFETIME_SECONDS
:
   'lifetime-seconds'
;

LIMIT_SESSION
:
   'limit-session'
;

LINK_PROTECTION
:
   'link-protection'
;

LLDP
:
   'lldp'
;

LLDP_MED
:
   'lldp-med'
;

LOAD_BALANCE
:
   'load-balance'
;

LOCAL
:
   'local'
;

LOCAL_ADDRESS
:
   'local-address'
;

LOCAL_AS
:
   'local-as'
;

LOCAL_IDENTITY
:
   'local-identity'
;

LOCAL_PREFERENCE
:
   'local-preference'
;

LOCATION
:
   'location'
;

LOG
:
   'log'
;

LOG_OUT_ON_DISCONNECT
:
   'log-out-on-disconnect'
;

LOG_PREFIX
:
   'log-prefix'
;

LOG_UPDOWN
:
   'log-updown'
;

LOGICAL_SYSTEM
:
   'logical-system'
;

LOGICAL_SYSTEMS
:
   'logical-systems'
;

LOGIN
:
   'login'
;

LONGER
:
   'longer'
;

LOOPBACK
:
   'loopback'
;

LOOPS
:
   'loops'
;

LOOSE_SOURCE_ROUTE_OPTION
:
   'loose-source-route-option'
;

LOSS_PRIORITY
:
   'loss-priority'
;

LOW
:
   'low'
;

LSP
:
   'lsp'
;

LSP_EQUAL_COST
:
   'lsp-equal-cost'
;

LSP_INTERVAL
:
   'lsp-interval'
;

LSP_LIFETIME
:
   'lsp-lifetime'
;

LSP_TELEMETRY
:
   'lsp-telemetry'
;

LSPING
:
   'lsping'
;

M
:
   'm'
;

MAC
:
   'mac' -> pushMode ( M_MacAddress )
;

MACS
:
   'macs'
;

MAIN
:
   'main'
;

MAPPED_PORT
:
   'mapped-port'
;

MARTIANS
:
   'martians'
;

MASK_REPLY
:
   'mask-reply'
;

MASK_REQUEST
:
   'mask-request'
;

MASTER_ONLY
:
   'master-only'
;

MATCH
:
   'match'
;

MAX_CONFIGURATIONS_ON_FLASH
:
   'max-configurations-on-flash'
;

MAX_CONFIGURATION_ROLLBACKS
:
   'max-configuration-rollbacks'
;

MAX_PRE_AUTHENTICATION_PACKETS
:
   'max-pre-authentication-packets'
;

MAX_SESSION_NUMBER
:
   'max-session-number'
;

MAX_SESSIONS_PER_CONNECTION
:
   'max-sessions-per-connection'
;

MAXIMUM
:
   'maximum'
;

MAXIMUM_LABELS
:
   'maximum-labels'
;

MD5
:
   'md5'
;

MEDIUM_HIGH
:
   'medium-high'
;

MEDIUM_LOW
:
   'medium-low'
;

MEMBER
:
   'member' -> pushMode(M_Interface)
;

MEMBER_RANGE
:
   'member-range' -> pushMode(M_MemberRange)
;

MEMBERS
:
   'members' -> pushMode ( M_Members )
;

METRIC
:
   'metric'
;

METRIC2
:
   'metric2'
;

METRIC_OUT
:
   'metric-out'
;

METRIC_TYPE
:
   'metric-type' -> pushMode ( M_MetricType )
;

MGCP_CA
:
   'mgcp-ca'
;

MGCP_UA
:
   'mgcp-ua'
;

MINIMUM_INTERVAL
:
  'minimum-interval'
;

MS_RPC
:
   'ms-rpc'
;

MLD
:
   'mld'
;

MOBILEIP_AGENT
:
   'mobileip-agent'
;

MOBILIP_MN
:
   'mobilip-mn'
;

MOBILITY_HEADER
:
   'mobility-header'
;

MODE
:
   'mode'
;

MPLS
:
   'mpls'
;

MSDP
:
   'msdp'
;

MSTP
:
   'mstp'
;

MTU
:
   'mtu'
;

MTU_DISCOVERY
:
   'mtu-discovery'
;

MULTI_CHASSIS
:
   'multi-chassis'
;

MULTICAST
:
   'multicast'
;

MULTICAST_MAC
:
   'multicast-mac' -> pushMode ( M_MacAddress )
;

MULTICAST_MODE
:
   'multicast-mode'
;

MULTIHOP
:
   'multihop'
;

MULTIPATH
:
   'multipath'
;

MULTIPLE_AS
:
   'multiple-as'
;

MULTIPLIER
:
   'multiplier'
;

MULTISERVICE_OPTIONS
:
   'multiservice-options'
;

MVPN
:
   'mvpn'
;

NAME
:
   'name'
;

NAME_RESOLUTION
:
   'name-resolution'
;

NAME_SERVER
:
   'name-server'
;

NAT
:
   'nat'
;

NATIVE_VLAN_ID
:
   'native-vlan-id'
;

NBMA
:
   'nbma'
;

NEAREST
:
   'nearest'
;

NEIGHBOR
:
   'neighbor'
;

NEIGHBOR_ADVERTISEMENT
:
   'neighbor-advertisement'
;

NEIGHBOR_DISCOVERY
:
   'neighbor-discovery'
;

NEIGHBOR_SOLICIT
:
   'neighbor-solicit'
;

NETBIOS_DGM
:
   'netbios-dgm'
;

NETBIOS_NS
:
   'netbios-ns'
;

NETBIOS_SSN
:
   'netbios-ssn'
;

NETCONF
:
   'netconf'
;

NETWORK_DOMAIN
:
   'network-domain'
;

NETWORK_SUMMARY_EXPORT
:
   'network-summary-export'
;

NETWORK_UNREACHABLE_FOR_TOS
:
   'network-unreachable-for-tos'
;

NETWORK_UNREACHABLE
:
   'network-unreachable'
;

NEVER
:
   'never'
;

NEXT
:
   'next'
;

NEXT_HEADER
:
   'next-header'
;

NEXT_HOP
:
   'next-hop' -> pushMode(M_Interface)
;

NEXT_IP
:
   'next-ip'
;

NEXT_IP6
:
   'next-ip6'
;

NEXT_TABLE
:
   'next-table'
;

NFSD
:
   'nfsd'
;

NHRP
:
   'nhrp'
;

NNTP
:
   'nntp'
;

NTALK
:
   'ntalk'
;

NO_ACTIVE_BACKBONE
:
   'no-active-backbone'
;

NO_ADJACENCY_DOWN_NOTIFICATION
:
   'no-adjacency-down-notification'
;

NO_ADVERTISE
:
   'no-advertise'
;

NO_ANTI_REPLAY
:
   'no-anti-replay'
;

NO_ARP
:
  'no-arp'
;

NO_AUTO_NEGOTIATION
:
   'no-auto-negotiation'
;

NO_CLIENT_REFLECT
:
   'no-client-reflect'
;

NO_ECMP_FAST_REROUTE
:
   'no-ecmp-fast-reroute'
;

NO_EXPORT
:
   'no-export'
;

NO_EXPORT_SUBCONFED
:
   'no-export-subconfed'
;

NO_FLOW_CONTROL
:
   'no-flow-control'
;

NO_GATEWAY_COMMUNITY
:
   'no-gateway-community'
;

NO_INSTALL
:
   'no-install'
;

NO_IPV4_ROUTING
:
   'no-ipv4-routing'
;

NO_NAT_TRAVERSAL
:
   'no-nat-traversal'
;

NO_NEIGHBOR_DOWN_NOTIFICATION
:
   'no-neighbor-down-notification'
;

NO_NEXT_HEADER
:
   'no-next-header'
;

NO_NEXTHOP_CHANGE
:
   'no-nexthop-change'
;

NO_PASSWORDS
:
   'no-passwords'
;

NO_PEER_LOOP_CHECK
:
   'no-peer-loop-check'
;

NO_PING_RECORD_ROUTE
:
   'no-ping-record-route'
;

NO_PING_TIME_STAMP
:
   'no-ping-time-stamp'
;

NO_READVERTISE
:
   'no-readvertise'
;

NO_REDIRECTS
:
   'no-redirects'
;

NO_REDIRECTS_IPV6
:
   'no-redirects-ipv6'
;

NO_RESOLVE
:
   'no-resolve'
;

NO_RETAIN
:
   'no-retain'
;

NO_RFC_1583
:
   'no-rfc-1583'
;

NO_NEIGHBOR_LEARN
:
   'no-neighbor-learn'
;

NO_SUMMARIES
:
   'no-summaries'
;

NO_TCP_FORWARDING
:
   'no-tcp-forwarding'
;

NO_TRANSLATION
:
   'no-translation'
;

NO_TRAPS
:
   'no-traps'
;

NODE_DEVICE
:
   'node-device'
;

NODE_GROUP
:
   'node-group'
;

NODE_LINK_PROTECTION
:
  'node-link-protection'
;

NONSTOP_ROUTING
:
   'nonstop-routing'
;

NSSA
:
   'nssa'
;

NTP
:
   'ntp'
;

OFF
:
   'off'
;

OFFSET
:
   'offset'
;

OPTIONS
:
   'options'
;

ORIGIN
:
   'origin'
;

ORLONGER
:
   'orlonger'
;

OSPF
:
   'ospf'
;

OSPF3
:
   'ospf3'
;

OUT_DELAY
:
   'out-delay'
;

OUTBOUND_SSH
:
   'outbound-ssh'
;

OUTPUT
:
   'output'
;

OUTPUT_LIST
:
   'output-list'
;

OUTPUT_VLAN_MAP
:
   'output-vlan-map'
;

OUTER
:
   'outer'
;

OVERLOAD
:
   'overload'
;

OVERRIDE_METRIC
:
   'override-metric'
;

OVERRIDES
:
   'overrides'
;

P2MP
:
   'p2mp'
;

P2MP_OVER_LAN
:
   'p2mp-over-lan'
;

P2P
:
   'p2p'
;

PACKET_LENGTH
:
   'packet-length'
;

PACKET_LENGTH_EXCEPT
:
   'packet-length-except'
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

PASSWORD
:
   'password'
;

PATH
:
   'path'
;

PATH_COUNT
:
   'path-count'
;

PATH_SELECTION
:
   'path-selection'
;

PAYLOAD_PROTOCOL
:
   'payload-protocol'
;

PEER_ADDRESS
:
   'peer-address'
;

PEER_AS
:
   'peer-as'
;

PEER_UNIT
:
   'peer-unit'
;

PER_PACKET
:
   'per-packet'
;

PER_UNIT_SCHEDULER
:
   'per-unit-scheduler'
;

PERFECT_FORWARD_SECRECY
:
   'perfect-forward-secrecy'
;

PERMIT
:
   'permit'
;

PERMIT_ALL
:
   'permit-all'
;

PERSISTENT_NAT
:
   'persistent-nat'
;

PGM
:
   'pgm'
;

PIM
:
   'pim'
;

PING
:
   'ping'
;

PING_DEATH
:
   'ping-death'
;

POE
:
   'poe'
;

POINT_TO_POINT
:
   'point-to-point'
;

POLICER
:
   'policer'
;

POLICIES
:
   'policies'
;

POLICY
:
   'policy'
;

POLICY_OPTIONS
:
   'policy-options'
;

POLICY_STATEMENT
:
   'policy-statement'
;

POLL_INTERVAL
:
   'poll-interval'
;

POOL
:
   'pool'
;

POOL_DEFAULT_PORT_RANGE
:
   'pool-default-port-range'
;

POOL_UTILIZATION_ALARM
:
    'pool-utilization-alarm'
;

POP3
:
   'pop3'
;

PORT
:
   'port'
;

PORTS
:
   'ports'
;

PORT_MIRROR
:
   'port-mirror'
;

PORT_MIRRORING
:
   'port-mirroring'
;

PORT_MODE
:
   'port-mode'
;

PORT_OVERLOADING
:
   'port-overloading'
;

PORT_OVERLOADING_FACTOR
:
   'port-overloading-factor'
;

PORT_RANDOMIZATION
:
   'port-randomization'
;

PORT_SCAN
:
	'port-scan'
;

PORT_UNREACHABLE
:
   'port-unreachable'
;

PPM
:
   'ppm'
;

PPTP
:
   'pptp'
;

PRE_SHARED_KEY
:
   'pre-shared-key'
;

PRE_SHARED_KEYS
:
   'pre-shared-keys'
;

PRECEDENCE
:
   'precedence'
;

PRECEDENCE_CUTOFF_IN_EFFECT
:
   'precedence-cutoff-in-effect'
;

PRECISION_TIMERS
:
   'precision-timers'
;

PREEMPT
:
   'preempt'
;

PREFER
:
   'prefer'
;

PREFERENCE
:
   'preference'
;

PREFERRED
:
   'preferred'
;

PREFIX
:
   'prefix'
;

PREFIX_NAME
:
   'prefix-name'
;

PREFIX_EXPORT_LIMIT
:
   'prefix-export-limit'
;

PREFIX_LENGTH_RANGE
:
   'prefix-length-range'
;

PREFIX_LIMIT
:
   'prefix-limit'
;

PREFIX_LIST
:
   'prefix-list' -> pushMode ( M_PrefixListName )
;

PREFIX_LIST_FILTER
:
   'prefix-list-filter'
;

PREFIX_POLICY
:
   'prefix-policy'
;

PRIMARY
:
   'primary'
;

PRINTER
:
   'printer'
;

PRIORITY
:
   'priority'
;

PRIORITY_COST
:
   'priority-cost'
;

PRIVATE
:
   'private'
;

PROCESSES
:
   'processes'
;

PROPOSAL
:
   'proposal'
;

PROPOSAL_SET
:
   'proposal-set'
;

PROPOSALS
:
   'proposals'
;

PROTECT
:
   'protect'
;

PROTOCOL
:
   'protocol'
;

PROTOCOL_UNREACHABLE
:
   'protocol-unreachable'
;

PROTOCOL_VERSION
:
   'protocol-version'
;

PROTOCOLS
:
   'protocols'
;

PROVIDER_TUNNEL
:
   'provider-tunnel'
;

PROXY_ARP
:
   'proxy-arp'
;

PROXY_IDENTITY
:
   'proxy-identity'
;

PROXY_MACIP_ADVERTISEMENT
:
   'proxy-macip-advertisement'
;

PSH
:
   'psh'
;

Q931
:
   'q931'
;

QUALIFIED_NEXT_HOP
:
   'qualified-next-hop'
;

QUICK_START_OPTION
:
   'quick-start-option'
;

R2CP
:
   'r2cp'
;

RADACCT
:
   'radacct'
;

RADIUS
:
   'radius'
;

RADIUS_OPTIONS
:
   'radius-options'
;

RADIUS_SERVER
:
   'radius-server'
;

RANGE
:
   'range'
;

RANGE_ADDRESS
:
   'range-address'
;

RAS
:
   'ras'
;

RATE_LIMIT
:
   'rate-limit'
;

REALAUDIO
:
   'realaudio'
;

READ_ONLY
:
   'read-only'
;

READ_WRITE
:
   'read-write'
;

READVERTISE
:
   'readvertise'
;

RECEIVE
:
   'receive'
;

RECORD_ROUTE_OPTION
:
   'record-route-option'
;

REDIRECT
:
   'redirect'
;

REDIRECT_FOR_HOST
:
   'redirect-for-host'
;

REDIRECT_FOR_NETWORK
:
   'redirect-for-network'
;

REDIRECT_FOR_TOS_AND_HOST
:
   'redirect-for-tos-and-host'
;

REDIRECT_FOR_TOS_AND_NET
:
   'redirect-for-tos-and-net'
;

REDUNDANCY_GROUP
:
   'redundancy-group'
;

REDUNDANT_ETHER_OPTIONS
:
   'redundant-ether-options'
;

REDUNDANT_PARENT
:
   'redundant-parent'
;

REFERENCE_BANDWIDTH
:
   'reference-bandwidth' -> pushMode ( M_Bandwidth )
;

REJECT
:
   'reject'
;

REKEY
:
   'rekey'
;

RELAY_AGENT_OPTION
:
   'relay-agent-option'
;

REMOTE
:
   'remote'
;

REMOVE_PRIVATE
:
   'remove-private'
;

REMOVED
:
   'Removed'
;

REPLACE
:
   'replace'
;

REQUIRED_OPTION_MISSING
:
   'required-option-missing'
;

RESOLUTION
:
   'resolution'
;

RESOLVE
:
   'resolve'
;

RESOURCES
:
   'resources'
;

REST
:
   'rest'
;

RESTRICT
:
   'restrict'
;

RETAIN
:
   'retain'
;

REVERSE
:
   'reverse'
;

REVERSE_SSH
:
   'reverse-ssh'
;

REVERSE_TELNET
:
   'reverse-telnet'
;

RIB
:
   'rib'
;

RIB_GROUP
:
   'rib-group'
;

RIB_GROUPS
:
   'rib-groups'
;

RIP
:
   'rip'
;

RIPNG
:
   'ripng'
;

RKINIT
:
   'rkinit'
;

RLOGIN
:
   'rlogin'
;

ROOT_AUTHENTICATION
:
   'root-authentication'
;

ROOT_LOGIN
:
   'root-login'
;

ROUTE
:
   'route'
;

ROUTE_DISTINGUISHER
:
   'route-distinguisher' -> pushMode ( M_RouteDistinguisher )
;

ROUTE_DISTINGUISHER_ID
:
   'route-distinguisher-id'
;

ROUTE_FILTER
:
   'route-filter'
;

ROUTE_TARGET
:
   'route-target'
;

ROUTE_TYPE
:
   'route-type'
;

ROUTER_ADVERTISEMENT
:
   'router-advertisement'
;

ROUTER_ALERT_OPTION
:
   'router-alert-option'
;

ROUTER_DISCOVERY
:
   'router-discovery'
;

ROUTER_ID
:
   'router-id'
;

ROUTER_SOLICIT
:
   'router-solicit'
;

ROUTING_HEADER
:
   'routing-header'
;

ROUTING_INSTANCE
:
   'routing-instance'
;

ROUTING_INSTANCES
:
   'routing-instances'
;

ROUTING_OPTIONS
:
   'routing-options'
;

RPC_PROGRAM_NUMBER
:
   'rpc-program-number'
;

RPF_CHECK
:
   'rpf-check'
;

RPL_OPTION
:
   'RPL-option'
;

RPM
:
   'rpm'
;

RSA_SIGNATURES
:
   'rsa-signatures'
;

RSH
:
   'rsh'
;

RSTP
:
   'rstp'
;

RSVP
:
   'rsvp'
;

RTSP
:
   'rtsp'
;

RULE
:
   'rule'
;

RULE_SET
:
   'rule-set'
;

SAMPLE
:
   'sample'
;

SAMPLING
:
   'sampling'
;

SAP
:
   'sap'
;

SAVED_CORE_CONTEXT
:
   'saved-core-context'
;

SAVED_CORE_FILES
:
   'saved-core-files'
;

SCCP
:
   'sccp'
;

SCHEDULER
:
   'scheduler'
;

SCREEN
:
   'screen'
;

SCRIPTS
:
   'scripts'
;

SCTP
:
   'sctp'
;

SCRUBBED
:
  '<SCRUBBED>'
;

SECRET
:
   'secret'
;

SECURITY
:
   'security'
;

SECURITY_OPTION
:
   'security-option'
;

SECURITY_PROFILE
:
   'security-profile'
;

SECURITY_ZONE
:
   'security-zone'
;

SERVER
:
   'server'
;

SERVER_GROUP
:
   'server-group'
;

SERVICE
:
   'service'
;

SERVICE_DEPLOYMENT
:
   'service-deployment'
;

SERVICE_FILTER
:
   'service-filter'
;

SERVICES
:
   'services'
;

SELF
:
   'self'
;

SEND
:
   'send'
;

SET
:
   'set'
;

SFLOW
:
   'sflow'
;

SFM_DPD_OPTION
:
   'SFM-DPD-option'
;

SHA_256
:
   'sha-256'
;

SHA_384
:
   'sha-384'
;

SHA1
:
   'sha1'
;

SHARED_IKE_ID
:
   'shared-ike-id'
;

SHIM6_HEADER
:
   'shim6-header'
;

SHORTCUTS
:
   'shortcuts'
;

SIGNALING
:
   'signaling'
;

SIMPLE
:
   'simple'
;

SINGLE_CONNECTION
:
   'single-connection'
;

SIP
:
   'sip'
;

SQLNET_V2
:
   'sqlnet-v2'
;

SRLG
:
   'srlg'
;

SRLG_COST
:
   'srlg-cost'
;

SRLG_VALUE
:
   'srlg-value'
;

SMTP
:
   'smtp'
;

SNMP
:
   'snmp'
;

SNMP_TRAP
:
   'snmp-trap'
;

SNMPTRAP
:
   'snmptrap'
;

SNPP
:
   'snpp'
;

SOCKS
:
   'socks'
;

SONET_OPTIONS
:
   'sonet-options'
;

SOURCE
:
   'source'
;

SOURCE_ADDRESS
:
   'source-address'
;

SOURCE_ADDRESS_EXCLUDED
:
   'source-address-excluded'
;

SOURCE_ADDRESS_FILTER
:
   'source-address-filter'
;

SOURCE_ADDRESS_NAME
:
    'source-address-name'
;

SOURCE_HOST_ISOLATED
:
   'source-host-isolated'
;

SOURCE_IDENTITY
:
   'source-identity'
;

SOURCE_INTERFACE
:
   'source-interface' -> pushMode(M_Interface)
;

SOURCE_IP_BASED
:
   'source-ip-based'
;

SOURCE_MAC_ADDRESS
:
   'source-mac-address' -> pushMode ( M_MacAddress )
;

SOURCE_NAT
:
   'source-nat'
;

SOURCE_PORT
:
   'source-port'
;

SOURCE_PREFIX_LIST
:
   'source-prefix-list'
;

SOURCE_ROUTE_FAILED
:
   'source-route-failed'
;

SOURCE_ROUTE_OPTION
:
   'source-route-option'
;

SOURCE_THRESHOLD
:
   'source-threshold'
;

SOURCE_QUENCH
:
   'source-quench'
;

SPEED
:
   'speed' -> pushMode ( M_Speed )
;

SPF_OPTIONS
:
   'spf-options'
;

SPOOFING
:
   'spoofing'
;

SSH
:
   'ssh'
;

STANDARD
:
   'standard'
;

START_TIME
:
   'start-time'
;

STATIC
:
   'static'
;

STATIC_HOST_MAPPING
:
   'static-host-mapping'
;

STATIC_NAT
:
   'static-nat'
;

STATION_ADDRESS
:
   'station-address'
;

STATION_PORT
:
   'station-port'
;

STATS_CACHE_LIFETIME
:
   'stats-cache-lifetime'
;

STORM_CONTROL
:
   'storm-control'
;

STORM_CONTROL_PROFILES
:
   'storm-control-profiles'
;

STP
:
   'stp'
;

STREAM_OPTION
:
   'stream-option'
;

STRICT_SOURCE_ROUTE_OPTION
:
   'strict-source-route-option'
;

STRUCTURED_DATA
:
   'structured-data'
;

STUB
:
   'stub'
;

SUBSCRIBER_MANAGEMENT
:
   'subscriber-management'
;

SUBTRACT
:
   'subtract'
;

SUN_RPC
:
   'sun-rpc'
;

SUNRPC
:
   'sunrpc'
;

SWITCH_OPTIONS
:
   'switch-options'
;

SWITCHOVER_ON_ROUTING_CRASH
:
   'switchover-on-routing-crash'
;

SYN_ACK_ACK_PROXY
:
   'syn-ack-ack-proxy'
;

SYN_FIN
:
   'syn-fin'
;

SYN_FLOOD
:
   'syn-flood'
;

SYN_FRAG
:
   'syn-frag'
;

SYSLOG
:
   'syslog'
;

SYSTEM
:
   'system'
;

SYSTEM_SERVICES
:
   'system-services'
;

TACACS
:
   'tacacs'
;

TACACS_DS
:
   'tacacs-ds'
;

TACPLUS
:
   'tacplus'
;

TACPLUS_SERVER
:
   'tacplus-server'
;

TAG
:
   'tag'
;

TALK
:
   'talk'
;

TARGET
:
   'target'
;

TARGET_HOST
:
   'target-host'
;

TARGET_HOST_PORT
:
   'target-host-port'
;

TARGETED_BROADCAST
:
   'targeted-broadcast'
;

TARGETS
:
   'targets'
;

TCP
:
   'tcp'
;

TCP_ESTABLISHED
:
   'tcp-established'
;

TCP_FLAGS
:
   'tcp-flags' -> pushMode ( M_TcpFlags )
;

TCP_FORWARDING
:
   'tcp-forwarding'
;

TCP_INITIAL
:
   'tcp-initial'
;

TCP_MSS
:
   'tcp-mss'
;

TCP_NO_FLAG
:
   'tcp-no-flag'
;

TCP_RST
:
   'tcp-rst'
;

TCP_SWEEP
:
   'tcp-sweep'
;

TE_METRIC
:
   'te-metric'
;

TEARDOWN
:
   'teardown'
;

TEAR_DROP
:
   'tear-drop'
;

TEREDO
:
   'teredo'
;

TELNET
:
   'telnet'
;

TERM
:
   'term'
;

TFTP
:
   'tftp'
;

TFTP_SERVER
:
   'tftp-server'
;

THEN
:
   'then'
;

THREEDES_CBC
:
   '3des-cbc'
;

THRESHOLD
:
   'threshold'
;

THROUGH
:
   'through'
;

TIME_FORMAT
:
   'time-format'
;

TIME_EXCEEDED
:
   'time-exceeded'
;

TIME_ZONE
:
   'time-zone'
;

TIMED
:
   'timed'
;

TIMEOUT
:
   'timeout'
;

TIMESTAMP
:
   'timestamp'
;

TIMESTAMP_OPTION
:
   'timestamp-option'
;

TIMESTAMP_REPLY
:
   'timestamp-reply'
;

TO
:
   'to'
;

TOLERANCE
:
   'tolerance'
;
TO_ZONE
:
   'to-zone'
;

TRACE
:
   'trace'
;

TRACEOPTIONS
:
   'traceoptions'
;

TRACEROUTE
:
   'traceroute'
;

TRACK
:
   'track'
;

TRAFFIC_ENGINEERING
:
   'traffic-engineering'
;

TRAP_DESTINATIONS
:
   'trap-destinations'
;

TRAP
:
   'trap'
;

TRAP_GROUP
:
   'trap-group'
;

TRAP_OPTIONS
:
   'trap-options'
;

TRAPS
:
   'traps'
;

TRUNK
:
   'trunk'
;

TRUST
:
   'trust'
;

TTL
:
   'ttl'
;

TTL_EQ_ZERO_DURING_REASSEMBLY
:
   'ttl-eq-zero-during-reassembly'
;

TTL_EQ_ZERO_DURING_TRANSIT
:
   'ttl-eq-zero-during-transit'
;

TUNNEL
:
   'tunnel'
;

TUNNEL_ENCAPSULATION_LIMIT_OPTION
:
   'tunnel-encapsulation-limit-option'
;

TYPE
:
   'type'
;

TYPE_7
:
   'type-7'
;

UDP
:
   'udp'
;

UDP_SWEEP
:
   'udp-sweep'
;

UNICAST
:
   'unicast'
;

UNIT
:
   'unit'
;

UNKNOWN_PROTOCOL
:
   'unknown-protocol'
;

UNREACHABLE
:
   'unreachable'
;

UNTRUST
:
   'untrust'
;

UNTRUST_SCREEN
:
   'untrust-screen'
;

UPLINK_FAILURE_DETECTION
:
   'uplink-failure-detection'
;

UPTO
:
   'upto'
;

URG
:
   'urg'
;

URPF_LOGGING
:
   'urpf-logging'
;

USER
:
   'user'
;

USER_DEFINED_OPTION_TYPE
:
   'user-defined-option-type'
;

UUID
:
   'uuid'
;

V1_ONLY
:
   'v1-only'
;

VERSION
:
   'version' -> pushMode ( M_Version )
;

VIEW
:
   'view'
;

VIRTUAL_ADDRESS
:
   'virtual-address'
;

VIRTUAL_CHASSIS
:
   'virtual-chassis'
;

VIRTUAL_ROUTER
:
   'virtual-router'
;

VIRTUAL_SWITCH
:
   'virtual-switch'
;

VLAN
:
   'vlan' -> pushMode ( M_Vlan )
;

VLANS
:
   'vlans'
;

VLAN_ID
:
   'vlan-id'
;

VLAN_ID_LIST
:
   'vlan-id-list'
;

VLAN_TAGS
:
   'vlan-tags'
;

VLAN_TAGGING
:
   'vlan-tagging'
;

VNI
:
   'vni'
;

VNI_OPTIONS
:
   'vni-options'
;

VPLS
:
   'vpls'
;

VPN
:
   'vpn'
;

VPN_MONITOR
:
   'vpn-monitor'
;

VRF
:
   'vrf'
;

VRF_EXPORT
:
   'vrf-export'
;

VRF_IMPORT
:
   'vrf-import'
;

VRF_TABLE_LABEL
:
   'vrf-table-label'
;

VRF_TARGET
:
   'vrf-target' -> pushMode ( M_VrfTarget )
;

VRRP
:
   'vrrp'
;

VRRP_GROUP
:
   'vrrp-group'
;

VSTP
:
   'vstp'
;

VTEP_SOURCE_INTERFACE
:
   'vtep-source-interface' -> pushMode(M_Interface)
;

VXLAN
:
   'vxlan'
;

WEB_MANAGEMENT
:
   'web-management'
;

WEBAPI
:
   'webapi'
;

WHITE_LIST
:
   'white-list'
;

WHO
:
   'who'
;

WIDE_METRICS_ONLY
:
   'wide-metrics-only'
;

WILDCARD_ADDRESS
:
   'wildcard-address' -> pushMode(M_WildcardAddress)
;

WINNUKE
:
   'winnuke'
;

XAUTH
:
   'xauth'
;

XDMCP
:
   'xdmcp'
;

XNM_CLEAR_TEXT
:
   'xnm-clear-text'
;

XNM_SSL
:
   'xnm-ssl'
;

ZONE
:
   'zone'
;

ZONES
:
   'zones'
;

// End of Juniper keywords

STANDARD_COMMUNITY
:
  F_StandardCommunity {!enableIPV6_ADDRESS}?
;

VARIABLE
:
   (
      (
         (
            F_Variable_RequiredVarChar
            {!enableIPV6_ADDRESS}?

            F_Variable_VarChar*
         )
         |
         (
            F_Variable_RequiredVarChar_Ipv6
            {enableIPV6_ADDRESS}?

            F_Variable_VarChar_Ipv6*
         )
      )
      |
      (
         (
            F_Variable_LeadingVarChar
            {!enableIPV6_ADDRESS}?

            F_Variable_VarChar* F_Variable_RequiredVarChar F_Variable_VarChar*
         )
         |
         (
            F_Variable_LeadingVarChar_Ipv6
            {enableIPV6_ADDRESS}?

            F_Variable_VarChar_Ipv6* F_Variable_RequiredVarChar_Ipv6
            F_Variable_VarChar_Ipv6*
         )
      )
   )
;

AMPERSAND
:
   '&'
;

ASTERISK
:
   '*'
;

BACKSLASH
:
   '\\'
;

CARAT
:
   '^'
;

CLOSE_BRACE
:
   '}'
;

CLOSE_BRACKET
:
   ']'
;

CLOSE_PAREN
:
   ')'
;

COLON
:
   ':'
;

COMMA
:
   ','
;

DASH
:
   '-'
;

DEC
:
   F_Digit+
;

DOLLAR
:
   '$'
;

DOUBLE_AMPERSAND
:
   '&&'
;

DOUBLE_PIPE
:
   '||'
;

DOUBLE_QUOTED_STRING
:
   '"' ~'"'* '"'
;

/*
FLOAT
:
   F_PositiveDigit* F_Digit '.'
   (
      '0'
      | F_Digit* F_PositiveDigit
   )
;
*/

FORWARD_SLASH
:
   '/'
;

GREATER_THAN
:
   '>'
;

IP_ADDRESS
:
   F_IpAddress{enableIP_ADDRESS}?
;

IP_PREFIX
:
   F_IpPrefix {enableIP_ADDRESS}? {isPrefix()}?
;

IPV6_ADDRESS
:
   F_Ipv6Address {enableIPV6_ADDRESS}?
;

IPV6_PREFIX
:
   F_Ipv6Prefix {enableIPV6_ADDRESS}?
;

LINE_COMMENT
:
   (
      '#'
      | '!'
   ) F_NonNewlineChar* F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> channel ( HIDDEN )
;

MULTILINE_COMMENT
:
   '/*' .*? '*/' -> channel ( HIDDEN )
;

NEWLINE
:
   F_NewlineChar+
   {
      enableIPV6_ADDRESS = true;
   }

;

OPEN_BRACE
:
   '{'
;

OPEN_BRACKET
:
   '['
;

OPEN_PAREN
:
   '('
;

PERIOD
:
   '.'
;

PLUS
:
   '+'
;

QUESTION_MARK
:
   '?'
;

SEMICOLON
:
   ';'
;

SINGLE_QUOTE
:
   '\''
;

UNDERSCORE
:
   '_'
;

WILDCARD
:
   '<' ~'>'* '>' {setWildcard();}
;

WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
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
F_InterfaceMediaType
:
   ('ae' |
   'as' |
   'at' |
   'bcm' |
   'cau4' |
   'ca1' |
   'ci' |
   'coc1' |
   'coc3' |
   'coc12' |
   'coc48' |
   'cp' |
   'cstm1' |
   'cstm4' |
   'cstm16' |
   'ct1' |
   'ct3' |
   'demux' |
   'dfc' |
   'ds' |
   'dsc' |
   'e1' |
   'e3' |
   'em' |
   'es' |
   'et' |
   'fab' |
   'fe' |
   'fxp' |
   'ge' |
   'gr' |
   'gre' |
   'ip' |
   'ipip' |
   'ixgbe' |
   'iw' |
   'lc' |
   'lo' |
   'ls' |
   'lsi' |
   'lm' |
   'me' |
   'mo' |
   'ms' |
   'mt' |
   'mtun' |
   'oc3' |
   'pd' |
   'pe' |
   'pimd' |
   'pime' |
   'reth' |
   'rlsq' |
   'rms' |
   'rsp' |
   'se' |
   'si' |
   'so' |
   'sp' |
   'st' |
   'stm1' |
   'stm4' |
   'stm16' |
   't1' |
   't3' |
   'tap' |
   'umd' |
   'vc4' |
   'vme' |
   'vsp' |
   'vt' |
   'xe' |
   'xt')
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
F_Letter
:
   [A-Za-z]
;

fragment
F_NewlineChar
:
   [\r\n]
;

fragment
F_NonNewlineChar
:
   ~[\r\n]
;

fragment
F_NonWhitespaceChar
:
   ~[ \t\u000C\r\n]
;

fragment
F_PositiveDigit
:
   [1-9]
;

fragment
F_Variable_RequiredVarChar
:
   ~[ 0-9\t\n\r/.,\-;{}<>[\]&|()"']
;

fragment
F_Hostname_LeadingChar
:
   [A-Za-z]
;

fragment
F_Hostname_TrailingChar
:
   [A-Za-z0-9_]|'-'
;

fragment
F_StandardCommunity
:
  F_Uint16 ':' F_Uint16
;

fragment
F_Variable_RequiredVarChar_Ipv6
:
   ~[ 0-9\t\n\r/.,\-:;{}<>[\]&|()"']
;

fragment
F_Variable_InterfaceVarChar
:
   ~[ \t\n\r.,:;{}<>[\]&|()"']
;

fragment
F_Variable_LeadingVarChar
:
   ~[ \t\n\r:;{}<>[\]&|()"']
;

fragment
F_Variable_LeadingVarChar_Ipv6
:
   ~[ \t\n\r:;{}<>[\]&|()"']
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
F_Variable_VarChar
:
   ~[ \t\n\r;{}[\]&|()"']
;

fragment
F_Variable_VarChar_Ipv6
:
   ~[ \t\n\r:;{}[\]&|()"']
;

fragment
F_WhitespaceChar
:
   [ \t\u000C]
;

mode M_AsPath;

M_AsPath_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_AsPath_AGGREGATOR
:
   'aggregator' -> type ( AGGREGATOR ) , popMode
;

M_AsPath_ORIGIN
:
   'origin' -> type ( ORIGIN ) , popMode
;

M_AsPath_PATH
:
   'path' -> type ( PATH ) , mode ( M_AsPathPath )
;

M_AsPath_TERM
:
   'term' -> type ( TERM ) , popMode
;

M_AsPath_VARIABLE
:
   (
      F_Digit
      | F_Variable_RequiredVarChar
   ) F_Variable_VarChar* -> type ( VARIABLE ) , mode ( M_AsPathRegex )
;

M_AsPath_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_AsPathPath;

M_AsPathPath_DEC
:
   F_Digit+ -> type ( DEC ) , popMode
;

M_AsPathPath_DOUBLE_QUOTE
:
   '"' -> channel ( HIDDEN ) , mode ( M_AsPathExpr )
;

M_AsPathPath_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_AsPathExpr;

M_AsPathExpr_DEC
:
   F_Digit+ -> type ( DEC )
;

M_AsPathExpr_PERIOD
:
   '.' -> type ( PERIOD )
;

M_AsPathExpr_OPEN_BRACKET
:
   '[' -> type ( OPEN_BRACKET )
;

M_AsPathExpr_CLOSE_BRACKET
:
   ']' -> type ( CLOSE_BRACKET )
;

M_AsPathExpr_DOUBLE_QUOTE
:
   '"' -> channel ( HIDDEN ) , popMode
;

M_AsPathExpr_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_AsPathGroup;

M_AsPathGroup_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

M_AsPathGroup_NAME_QUOTED
:
  '"' ~'"'+ '"' -> mode ( M_AsPathGroup2 )
;

M_AsPathGroup_NAME
:
  F_NonWhitespaceChar+ -> mode(M_AsPathGroup2)
;

mode M_AsPathGroup2;

M_AsPathGroup2_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

M_AsPathGroup2_NEWLINE
:
  F_NewlineChar+ -> type(NEWLINE), popMode
;

M_AsPathGroup2_AS_PATH
:
   'as-path' -> type(AS_PATH), mode(M_AsPathGroup3)
;

M_AsPathGroup2_DYNAMIC_DB
:
  'dynamic-db' -> type(DYNAMIC_DB), popMode
;

mode M_AsPathGroup3;

M_AsPathGroup3_WS
:
   F_WhitespaceChar+ -> channel(HIDDEN)
;

M_AsPathGroup3_NAME_QUOTED
:
  '"' ~'"'+ '"' -> mode (M_AsPathRegex)
;

M_AsPathGroup3_NAME
:
  F_NonWhitespaceChar+ -> mode(M_AsPathRegex)
;

mode M_AsPathPrepend;

M_AsPathPrepend_DEC
:
   F_Digit+ -> type ( DEC ) , popMode
;

M_AsPathPrepend_DOUBLE_QUOTE
:
   '"' -> channel( HIDDEN ) , mode ( M_AsPathPrepend_Inner )
;

M_AsPathPrepend_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_AsPathPrepend_Inner;

M_AsPathPrepend_Inner_DEC
:
   F_Digit+ -> type ( DEC )
;

M_AsPathPrepend_Inner_DOUBLE_QUOTE
:
   '"' -> channel( HIDDEN ) , popMode
;

M_AsPathPrepend_Inner_PERIOD
:
   '.' -> type ( PERIOD )
;

M_AsPathPrepend_Inner_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_AsPathRegex;

AS_PATH_REGEX
:
   [0-9,^$[\]\-*.{}+|()] [0-9,^$[\]\-*.{}+|() _?]*
;

M_AsPathRegex_DOUBLE_QUOTE
:
   '"' -> channel ( HIDDEN )
;

M_AsPathRegex_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_AsPathRegex_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_Description;

M_Description_DESCRIPTION
:
   F_NonWhitespaceChar F_NonNewlineChar*
;

M_Description_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_Description_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_DSCP;

M_DSCP_VARIABLE
:
   F_NonWhitespaceChar+ -> type ( VARIABLE )
;

M_DSCP_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_DSCP_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_Interface;

M_Interface_COLON
:
   ':' -> type (COLON)
;

M_Interface_DEC
:
   DEC -> type ( DEC ) , popMode
;


M_Interface_ALL
:
   'all' -> type ( ALL ) , popMode
;

M_Interface_APPLY_GROUPS
:
   'apply-groups' -> type ( APPLY_GROUPS ) , popMode
;

M_Interface_APPLY_GROUPS_EXCEPT
:
   'apply-groups-except' -> type ( APPLY_GROUPS_EXCEPT ) , popMode
;

M_Interface_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_Interface_INTERFACE
:
   'interface' -> type ( INTERFACE )
;

M_Interface_INTERFACE_RANGE
:
   'interface-range' -> type ( INTERFACE_RANGE ) , popMode
;

M_Interface_INTERFACE_NAME
:
   (
      F_InterfaceMediaType '-'? F_Digit+ ('/' F_Digit+)*
      | 'irb'
      | 'vlan'
      | 'vme'
   ) -> type(INTERFACE_NAME), popMode
;

M_Interface_PORT_OVERLOADING
:
   'port-overloading' -> type ( PORT_OVERLOADING ) , popMode
;

M_Interface_PORT_OVERLOADING_FACTOR
:
   'port-overloading-factor' -> type ( PORT_OVERLOADING_FACTOR ) , popMode
;

M_Interface_QUOTE
:
   '"' -> channel ( HIDDEN ) , mode ( M_InterfaceQuote )
;

M_Interface_SELF
:
    'self' -> type ( SELF ), popMode
;

M_Interface_TRACEOPTIONS
:
   'traceoptions' -> type ( TRACEOPTIONS ) , popMode
;

M_Interface_HOSTNAME
:
   F_Hostname_LeadingChar F_Hostname_TrailingChar* -> type ( VARIABLE )
;

M_Interface_WILDCARD
:
   '<' ~'>'* '>' {setType(_markWildcards?WILDCARD_ARTIFACT:WILDCARD);} -> popMode
;

M_Interface_IP_ADDRESS
:
   F_IpAddress -> type ( IP_ADDRESS ) , popMode
;

M_Interface_IPV6_ADDRESS
:
   F_Ipv6Address -> type (IPV6_ADDRESS), popMode
;

M_Interface_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_InterfaceQuote;

M_InterfaceQuote_QUOTE
:
   '"' -> channel ( HIDDEN ) , popMode
;

M_InterfaceQuote_VARIABLE
:
   F_Variable_RequiredVarChar F_Variable_InterfaceVarChar* -> type ( VARIABLE )
;

M_InterfaceQuote_WILDCARD
:
   '<' ~'>'* '>' {setType(_markWildcards?WILDCARD_ARTIFACT:WILDCARD);}
;

M_InterfaceQuote_DOUBLE_QUOTED_STRING
:
    ~'"'+ -> type ( DOUBLE_QUOTED_STRING )
;

mode M_ISO;

M_ISO_ADDRESS
:
   'address' -> type ( ADDRESS ) , mode ( M_ISO_Address )
;

M_ISO_MTU
:
   'mtu' -> type ( MTU ) , popMode
;

M_ISO_Newline
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_ISO_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_ISO_Address;

M_ISO_Address_ISO_ADDRESS
:
   F_HexDigit+
   (
      '.' F_HexDigit+
   )+ -> type ( ISO_ADDRESS ) , popMode
;

M_ISO_Address_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_MacAddress;

MAC_ADDRESS
:
   F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit
   ':' F_HexDigit F_HexDigit ':' F_HexDigit F_HexDigit ':' F_HexDigit
   F_HexDigit -> popMode
;

M_MacAddress_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_MemberRange;

M_MemberRange_INTERFACE_NAME:
   (
      F_InterfaceMediaType '-'? F_Digit+ ('/' F_Digit+)*
      | 'irb' | 'vlan'
   ) -> type(INTERFACE_NAME), mode(M_MemberRange2)
;

M_MemberRange_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_MemberRange2;

M_MemberRange2_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

M_MemberRange2_TO:
   'to' -> type(TO), mode(M_Interface)
;

mode M_Members;

M_Members_ASTERISK
:
   '*' -> type ( ASTERISK )
;

M_Members_BACKSLASH
:
   '\\' -> type (BACKSLASH)
;

M_Members_CARAT
:
   '^' -> type ( CARAT )
;

M_Members_CLOSE_BRACE
:
   '}' -> type ( CLOSE_BRACE )
;

M_Members_CLOSE_BRACKET
:
   ']' -> type ( CLOSE_BRACKET )
;

M_Members_CLOSE_PAREN
:
   ')' -> type ( CLOSE_PAREN )
;

M_Members_COLON
:
   ':' -> type ( COLON )
;

M_Members_COMMA
:
   ',' -> type ( COMMA )
;

M_Members_DASH
:
   '-' -> type ( DASH )
;

M_Members_DEC
:
   F_Digit+ -> type ( DEC )
;

M_Members_DOLLAR
:
   '$' -> type ( DOLLAR )
;

M_Members_DOUBLE_QUOTE
:
   '"' -> channel ( HIDDEN )
;

M_Members_L
:
   'L' -> type ( L )
;

M_Members_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_Members_NO_ADVERTISE
:
   'no-advertise' -> type ( NO_ADVERTISE )
;

M_Members_NO_EXPORT
:
   'no-export' -> type ( NO_EXPORT )
;

M_Members_NO_EXPORT_SUBCONFED
:
   'no-export-subconfed' -> type ( NO_EXPORT_SUBCONFED )
;

M_Members_OPEN_BRACE
:
   '{' -> type ( OPEN_BRACE )
;

M_Members_OPEN_BRACKET
:
   '[' -> type ( OPEN_BRACKET )
;

M_Members_OPEN_PAREN
:
   '(' -> type ( OPEN_PAREN )
;

M_Members_ORIGIN
:
   'origin' -> type ( ORIGIN )
;

M_Members_PERIOD
:
   '.' -> type ( PERIOD )
;

M_Members_PLUS
:
   '+' -> type ( PLUS )
;

M_Members_PIPE
:
   '|' -> type ( PIPE )
;

M_Members_QUESTION_MARK
:
   '?' -> type ( QUESTION_MARK )
;

M_Members_TARGET
:
   'target' -> type ( TARGET )
;

M_Members_UNDERSCORE
:
   '_' -> type ( UNDERSCORE )
;

M_Members_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_PrefixListName;

M_PrefixListName_WILDCARD
:
   '<' ~'>'* '>' {setType(_markWildcards?WILDCARD_ARTIFACT:WILDCARD);} -> popMode
;

M_PrefixListName_VARIABLE
:
   ~[ \t\n\r&|()"]+ -> type ( VARIABLE ) , popMode
;

M_PrefixListName_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_Bandwidth;

M_Bandwidth_DEC
:
  F_Digit+ -> type ( DEC )
;

M_Bandwidth_C
:
  'c' -> type ( C ) , popMode
;

M_Bandwidth_G
:
  'g' -> type ( G ) , popMode
;

M_Bandwidth_K
:
  'k' -> type ( K ) , popMode
;

M_Bandwidth_M
:
  'm' -> type ( M ) , popMode
;

M_Bandwidth_NEWLINE
:
  F_NewlineChar+ -> type ( NEWLINE ) , popMode
;

M_Bandwidth_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_TcpFlags;

M_TcpFlags_ACK
:
   'ack' -> type ( ACK )
;

M_TcpFlags_CWR
:
   'cwr' -> type ( CWR )
;

M_TcpFlags_DOUBLE_QUOTE
:
   '"' -> channel ( HIDDEN ) , pushMode ( M_TcpFlags2 )
;

M_TcpFlags_ECE
:
   'ece' -> type ( ECE )
;

M_TcpFlags_FIN
:
   'fin' -> type ( FIN )
;

M_TcpFlags_NEWLINE
:
   F_NewlineChar+ -> type ( NEWLINE ) , popMode
;

M_TcpFlags_RST
:
   'rst' -> type ( RST )
;

M_TcpFlags_SYN
:
   'syn' -> type ( SYN )
;

M_TcpFlags_URG
:
   'urg' -> type ( URG )
;

M_TcpFlags_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_TcpFlags2;

M_TcpFlags2_ACK
:
   'ack' -> type ( ACK )
;

M_TcpFlags2_AMPERSAND
:
   '&' -> type ( AMPERSAND )
;

M_TcpFlags2_BANG
:
   '!' -> type ( BANG )
;

M_TcpFlags2_CLOSE_PAREN
:
   ')' -> type ( CLOSE_PAREN )
;

M_TcpFlags2_CWR
:
   'cwr' -> type ( CWR )
;

M_TcpFlags2_ECE
:
   'ece' -> type ( ECE )
;

M_TcpFlags2_FIN
:
   'fin' -> type ( FIN )
;

M_TcpFlags2_OPEN_PAREN
:
   '(' -> type ( OPEN_PAREN )
;

M_TcpFlags2_PIPE
:
   '|' -> type ( PIPE )
;

M_TcpFlags2_RST
:
   'rst' -> type ( RST )
;

M_TcpFlags2_SYN
:
   'syn' -> type ( SYN )
;

M_TcpFlags2_URG
:
   'urg' -> type ( URG )
;

M_TcpFlags2_DOUBLE_QUOTE
:
   '"' -> channel ( HIDDEN ) , mode ( DEFAULT_MODE )
;

M_TcpFlags2_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_MetricType;

METRIC_TYPE_1
:
   '1' -> popMode
;

METRIC_TYPE_2
:
   '2' -> popMode
;

M_MetricType_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_RouteDistinguisher;

M_RouteDistinguisher_COLON
:
   ':' -> type ( COLON )
;

M_RouteDistinguisher_IP_ADDRESS
:
   F_IpAddress -> type ( IP_ADDRESS )
;

M_RouteDistinguisher_DEC
:
   F_Digit+ -> type ( DEC )
;

M_RouteDistinguisher_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_RouteDistinguisher_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_Speed;

M_Speed_DEC
:
   F_Digit+ -> type ( DEC )
;

M_Speed_G
:
   'g' -> type ( G ) , popMode
;

M_Speed_M
:
   'm' -> type ( M ) , popMode
;

M_Speed_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_VarOrWildcard;

M_VarOrWildcard_VARIABLE
:
   ~[ \t\u000C\r\n<]+ -> type ( VARIABLE ) , popMode
;

M_VarOrWildcard_WILDCARD
:
   '<' ~'>'* '>' {setType(_markWildcards?WILDCARD_ARTIFACT:WILDCARD);}-> popMode
;

M_VarOrWildcard_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_Version;

M_Version_V1_ONLY
:
   'v1-only' -> type ( V1_ONLY ) , popMode
;

M_Version_QUOTED_STRING
:
   '"' ~'"'* '"' -> type ( VERSION_STRING ) , popMode
;

M_Version_VERSION_STRING
:
   ~[ \t\u000C\r\n;]+ -> type ( VERSION_STRING ) , popMode
;

M_Version_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_Vlan;

M_Vlan_MEMBERS
:
   'members' -> type ( MEMBERS ) , popMode
;

M_Vlan_UNIT
:
   'unit' -> type ( UNIT ) , popMode
;

M_Vlan_VARIABLE
:
   ~[ \t\n\r&|()"]+ -> type ( VARIABLE ) , popMode
;

M_Vlan_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_VrfTarget;

M_VrfTarget_COLON
:
   ':' -> type ( COLON )
;

M_VrfTarget_DEC
:
   F_Digit+ -> type ( DEC )
;

M_VrfTarget_EXPORT
:
   'export' -> type ( EXPORT )
;

M_VrfTarget_IMPORT
:
   'import' -> type ( IMPORT )
;

M_VrfTarget_L
:
   'L' -> type ( L )
;

M_VrfTarget_NEWLINE
:
   F_NewlineChar+
   {enableIPV6_ADDRESS = true;}

   -> type ( NEWLINE ) , popMode
;

M_VrfTarget_PERIOD
:
   '.' -> type ( PERIOD )
;

M_VrfTarget_TARGET
:
   'target' -> type ( TARGET )
;

M_VrfTarget_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_WildcardAddress;

M_WildcardAddress_IP_ADDRESS
:
    F_IpAddress -> type ( IP_ADDRESS )
;

M_WildcardAddress_FORWARD_SLASH
:
    '/' -> type ( FORWARD_SLASH ) , mode ( M_WildcardAddress2 )
;

M_WildcardAddress_WS
:
   F_WhitespaceChar+ -> channel ( HIDDEN )
;

mode M_WildcardAddress2;

M_WildcardAddress2_IP_ADDRESS
:
    F_IpAddress -> type ( IP_ADDRESS ) , popMode
;
