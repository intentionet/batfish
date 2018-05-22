parser grammar Cisco_ignored;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

null_block
:
   NO?
   (
      AAA_SERVER
      | ACCESS_GROUP
      | ACCESS
      | ACL_POLICY
      | ACLLOG
      | ADMIN
      | ALLOW
      | APPLETALK
      | AS_PATH_SET
      | ATM
      | BASH
      | BFD
      | BGP DISABLE_ADVERTISEMENT
      | BLOGGERD
      | BSD_CLIENT
      | BSD_USERNAME
      | BUFFERS
      | CAM_ACL
      | CAM_PROFILE
      | CEF
      | CHAT_SCRIPT
      | CISP
      | CLI
      | CLNS
      | CLOCK
      | COMMIT
      | CONFDCONFIG
      | CONFIGURATION
      | COAP
      | COPP
      | COPY
      | CPD
      | CRYPTOCHECKSUM
      | DAEMON
      | DCB
      | DCB_BUFFER_THRESHOLD
      | DEBUG
      | DEFAULT_MAX_FRAME_SIZE
      | DEFAULT_VALUE
      | DEVICE
      | DHCPRELAY
      | DO STOP
      | DOT1X
      | DOT1X_ENABLE
      | DUAL_MODE_DEFAULT_VLAN
      | DYNAMIC_ACCESS_POLICY_RECORD
      | ENABLE_ACL_COUNTER
      | ENABLE_QOS_STATISTICS
      | ETHERNET
      | EXCEPTION_SLAVE
      | EXIT
      | FABRICPATH
      | FEATURE_SET
      | FEX
      | FPD
      | GATEKEEPER
      | GATEWAY
      | GLOBAL_PORT_SECURITY
      | GROUP_POLICY
      | HASH_ALGORITHM
      | HPM
      | HSRP
      | HW_SWITCH
      | INSTALL
      |
      (
         IP
         (
            (
               ACCESS_LIST LOGGING
            )
            | ACCOUNTING_LIST
            | ACCOUNTING_THRESHOLD
            | ADJACENCY
            | ADJMGR
            | BOOTP_RELAY
            | DECAP_GROUP
            | DNS
            | ECMP_GROUP
            | FLOW_AGGREGATION
            | FLOW_CAPTURE
            | FLOW_SAMPLING_MODE
            | FLOW_TOP_TALKERS
            | GLOBAL_MTU
            | HARDWARE
            | ICMP_ERRORS
            | INSPECT
            | INTERNAL
            | POLICY_LIST
            | RATE_LIMIT
            | RECEIVE
            | REFLEXIVE_LIST
            | ROUTER_ID
            | RSVP
            | SDR
            | SOURCE
            | SYSLOG
            | VIRTUAL_ROUTER
            |
            (
               VRF ~NEWLINE
            )
         )
      )
      |
      (
         IPV4
         (
            ASSEMBLER
            | CONFLICT_POLICY
            | HARDWARE
            | ROUTING
            | UNNUMBERED
            | VIRTUAL
         )
      )
      |
      (
         IPV6
         (
            ADJACENCY
            | ADJACENCY_STALE_TIMER
            | CONFLICT_POLICY
            | GLOBAL_MTU
            | ENABLE_ACL_CAM_SHARING
            | HARDWARE
            | ICMP
            | MROUTE
            | NEIGHBOR
            | ROUTING
         )
      )
      | KEYSTORE
      | KRON
      | LACP
      | LAG
      | LINECARD
      | LOAD_BALANCE
      | LOGIN
      | MAC_LEARN
      | MACRO
      | MANAGEMENT_ACCESS
      | MAP_LIST
      | MASTERIP
      | MENU
      | MLAG
      | MODULE
      | MONITOR_INTERFACE
      |
      (
         MPLS
         (
            (
               IP
               | IPV6
               | LDP ~NEWLINE
            )
            | OAM
            | STRIP
            | TRAFFIC_ENG
         )
      )
      | MULTI_CONFIG
      | NLS
      | NO_BANNER
      | NO_L4R_SHIM
      | NSR
      | ONE
      | OPTICAL_MONITOR
      |
      (
         OSPF
         (
            NAME_LOOKUP
         )
      )
      | PASSWORD_POLICY
      | PLAT
      | PLATFORM
      | POLICY_MAP_INPUT
      | POLICY_MAP_OUTPUT
      | POOL
      | PORT_PROFILE
      | POWEROFF
      | POWER_MGR
      | PRIORITY_FLOW_CONTROL
      | PSEUDOWIRE_CLASS
      | PTP
      | QOS_POLICY
      | QOS_POLICY_OUTPUT
      | RELOAD_TYPE
      | RMON
      | ROUTE_ONLY
      |
      (
         ROUTER
         (
            LOG
         )
      )
      | RP
      | RX_COS_SLOT
      | SAMPLER
      | SAMPLER_MAP
      | SAP
      | SDR
      | SENSOR
      | SERVICE_CLASS
      | SFLOW
      | SLOT
      | SLOT_TABLE_COS
      | STACK_MAC
      | STACK_UNIT
      | SVCLC
      | SWITCH
      | SWITCH_PROFILE
      | SWITCH_TYPE
      | SYSLOGD
      | SYSTEM_INIT
      | SYSTEM_MAX
      | TAG_TYPE
      | TASKGROUP
      | TCP
      | TEMPLATE
      | TERMINAL
      | TIME_RANGE
      | TFTP
      | TLS_PROXY
      | TRACE
      | TRANSCEIVER
      | TRANSCEIVER_TYPE_CHECK
      | TRANSPARENT_HW_FLOODING
      | TUNNEL_GROUP
      | UDF
      | USERGROUP
      | VDC
      | VER
      | VIRTUAL_SERVICE
      |
      (
         VLAN
         (
            DOT1Q
         )
      )
      | VLAN_GROUP
      | VLAN_POLICY
      | VLT
      | VXLAN
      | VTY_POOL
      | WISM
      | WRED_PROFILE
      | WSMA
      | XDR
      | XML
   ) null_rest_of_line
   (
      description_line
      | null_inner
   )*
;

null_inner
:
   NO?
   (
      ACCEPT_DIALIN
      | ACCOUNTING
      | ACTIVE
      | ADD_VLAN
      | ADDRESS
      | ADDRESS_POOLS
      | ADDRESS_RANGE
      | ADMINISTRATIVE_WEIGHT
      | ADVERTISE
      | AESA
      | ALLOCATE
      | ALWAYS_ON_VPN
      | APPLICATION
      | ARCHIVE_LENGTH
      | ARCHIVE_SIZE
      | AUTHORIZATION_REQUIRED
      | AUTHORIZATION_SERVER_GROUP
      | AUTO_RECOVERY
      | BACK_UP
      | BACKGROUND_ROUTES_ENABLE
      | BANDWIDTH_PERCENTAGE
      |
      (
         BANNER
         (
            NONE
            | VALUE
         )
      )
      | BRIDGE_PRIORITY
      | CACHE_TIMEOUT
      | CALL
      | CAS_CUSTOM
      | CERTIFICATE
      | CHANNEL_GROUP
      | CHANNELIZED
      | CLIENT_GROUP
      | CLOCK
      | COMMAND
      | CONNECT_SOURCE
      | CONTEXT
      | CONTROL_WORD
      | CPU_SHARE
      | CREDENTIALS
      | DEADTIME
      | DEFAULT_DOMAIN
      | DEFAULT_ROUTER
      | DENY
      | DEPLOY
      | DESTINATION_PATTERN
      | DESTINATION_SLOT
      | DIAGNOSTIC
      | DISTRIBUTION
      | DNS_SERVER
      | DOMAIN_ID
      | DROP
      | DS0_GROUP
      | DUAL_ACTIVE
      | ECHO
      | EGRESS
      | ENABLED
      | ENCAPSULATION
      | ESCAPE_CHARACTER
      | EXIT
      | EXPECT
      | EXPORT
      | FABRIC
      | FAILED
      | FAIR_QUEUE
      | FALLBACK_DN
      | FIELDS
      | FILE_BROWSING
      | FILE_ENTRY
      | FILE_SIZE
      | FLUSH_AT_ACTIVATION
      | FORWARD_DIGITS
      | FRAMING
      | FT
      | GATEWAY
      | GID
      | GROUP
      | GROUP_ALIAS
      | GROUP_LOCK
      | GROUP_POLICY
      | HA_POLICY
      |
      (
         HASH SYMMETRIC
      )
      | HEARTBEAT_INTERVAL
      | HEARTBEAT_TIME
      | HELPER_ADDRESS
      | HIDDEN_LITERAL
      | HIDDEN_SHARES
      | HIDEKEYS
      | HIGH_AVAILABILITY
      | HOMEDIR
      | ICMP_ECHO
      | IDLE
      | IDLE_TIMEOUT
      | IMPORT
      | INCOMING
      | INGRESS
      | INHERIT
      | INSERVICE
      | INSTANCE
      |
      (
         INTERFACE POLICY
      )
      | INTERVAL
      | INTERWORKING
      |
      (
         (
            IP
            | IPV6
         )
         (
            ACCESS_GROUP
            | ADDRESS
            | ARP
            | FLOW
         )
      )
      | IPSEC_UDP
      | IPX
      | KEYPATH
      | LACP_TIMEOUT
      | LEASE
      | LENGTH
      | LIMIT_RESOURCE
      | LINECODE
      | LLDP
      | LOCAL_INTERFACE
      | LOG
      | LPTS
      | MAC_ADDRESS
      | MEDIA
      | MEMBER
      | MESH_GROUP
      | MODE
      | MONITORING
      | MSDP_PEER
      | MSIE_PROXY
      | NAMESPACE
      | NEGOTIATE
      | NETWORK
      | NODE
      | NOTIFY
      | OPEN
      | OPTION
      | OPS
      | ORIGINATOR_ID
      | OUI
      | PARAMETERS
      | PARITY
      | PASSWORD
      | PASSWORD_STORAGE
      | PATH_JITTER
      | PEER_ADDRESS
      | PEER_CONFIG_CHECK_BYPASS
      | PEER_ID_VALIDATE
      | PEER_LINK
      | PERIODIC
      | PERMIT
      | PERSISTENT
      | PHYSICAL_PORT
      | PICKUP
      | PINNING
      | POLICY
      | POLICY_LIST
      | PORT_NAME
      | PORTS
      | PREDICTOR
      | PREEMPT
      | PREFERRED_PATH
      | PREFIX
      | PRIMARY_PORT
      | PRIMARY_PRIORITY
      | PROBE
      | PROPOSAL
      | PROVISION
      | RANDOM
      | RANDOM_DETECT
      | RD
      | REACT
      | REAL
      | RECEIVE
      | REDISTRIBUTE
      | RELOAD
      | RELOAD_DELAY
      | REMARK
      | REMOTE_AS
      | REQUEST
      | RESOURCES
      | RESPONDER
      | RETRIES
      | REVISION
      | RING
      | ROUTE
      | ROUTE_TARGET
      | RP_ADDRESS
      | SA_FILTER
      | SATELLITE
      | SECRET
      | SEQUENCE
      | SERVER
      | SERVERFARM
      | SERVER_PRIVATE
      | SERVICE_POLICY
      | SERVICE_QUEUE
      | SERVICE_TYPE
      | SESSION
      | SEVERITY
      | SIGNING
      | SINGLE_CONNECTION
      | SINGLE_ROUTER_MODE
      | SLOT
      | SORT_BY
      | SPEED
      | SPLIT_TUNNEL_NETWORK_LIST
      | SPLIT_TUNNEL_POLICY
      | SSH_KEYDIR
      | STICKY
      | SWITCHPORT
      | SYNC
      | TAG
      | TAGGED
      | TASK
      | TASK_SPACE_EXECUTE
      | TASKGROUP
      | TCP_CONNECT
      | TIMEOUT
      | TIMER
      | TOP
      | TRACKING_PRIORITY_INCREMENT
      | TRANSLATION_PROFILE
      | TRUNK
      | TRUNK_THRESHOLD
      | TRUST
      | TTL_THRESHOLD
      | TUNNEL
      | TUNNEL_GROUP
      | TYPE
      | UDP_JITTER
      | UID
      | USE_VRF
      | USERS
      | VERSION
      | VIRTUAL
      | VIRTUAL_ROUTER
      | VM_CPU
      | VM_MEMORY
      | VPN_FILTER
      | VPN_GROUP_POLICY
      | VPN_IDLE_TIMEOUT
      | VPN_SESSION_TIMEOUT
      | VPN_SIMULTANEOUS_LOGINS
      | VPN_TUNNEL_PROTOCOL
      | VSERVER
      | WINS_SERVER
      | WITHOUT_CSD
      | WRED
      | XML_CONFIG
   ) null_rest_of_line
;

null_single
:
   NO?
   (
      ABSOLUTE_TIMEOUT
      |
      (
         ACCESS_LIST
         (
            (
               (
                  DEC
                  | variable_aclname
               )
               (
                  EXTENDED
                  | REMARK
               )
            )
            | DYNAMIC_EXTENDED
         )
      )
      | ACCOUNTING
      | ACCOUNTING_PORT
      | ACTIVATE_SERVICE_WHITELIST
      | ADP
      | AGING
      | AIRGROUP
      | ALIAS
      | AMON
      | AP_CRASH_TRANSFER
      | AP_LACP_STRIPING_IP
      | AQM_REGISTER_FNF
      | APP
      | ARP
      | ASA
      | ASDM
      | ASYNC_BOOTP
      | AUTHENTICATION_PORT
      | AUTHORIZATION
      | AUTO
      | AUTORECOVERY
      | BOOT
      | BOOT_END_MARKER
      | BOOT_START_MARKER
      | BRIDGE
      | BRIDGE_DOMAIN
      | BUILDING_CONFIGURATION
      | CALL
      | CARD
      | CCM_MANAGER
      | CDP
      | CFS
      | CLOCK
      | CNS
      | CONFIG
      | CONFIG_REGISTER
      | CONSOLE
      | CONTROLLER_IP
      | COS_MAPPING
      | COUNTRY
      | CRYPTO_LOCAL
      | CTS
      | CURRENT_CONFIGURATION
      | DATABASE
      | DEFAULT
      | DEPI
      | DEVICE_SENSOR
      | DHCPD
      | DIAGNOSTIC
      | DIAL_CONTROL_MIB
      | DIALER_LIST
      | DNS
      | DNS_GUARD
      | DOWNLINK
      | DSP
      | DSS
      | END
      | ENVIRONMENT
      | ENVIRONMENT_MONITOR
      | ERRDISABLE
      | ESCAPE_CHARACTER
      | EXCEPTION
      | EXEC
      | FABRIC
      | FABRIC_MODE
      | FACILITY_ALARM
      | FAN
      | FILE
      | FIREWALL
      | FIREWALL_VISIBILITY
      | FIRMWARE
      | FLOWCONTROL
      | FRAME_RELAY
      | FRI
      | FTP
      | FTP_SERVER
      | GROUP
      | HOST
      | HTTP
      | HW_MODULE
      | ICMP
      | ICMP_OBJECT
      | IDENTITY
      | INACTIVITY_TIMER
      |
      (
         IP
         (
            ADDRESS_POOL
            | ADMISSION
            | ALIAS
            | ARP
            | AUDIT
            | AUTH_PROXY
            | BOOTP
            | BGP_COMMUNITY
            | CEF
            | CLASSLESS
            | DEFAULT_NETWORK
            | DEVICE
            | DOMAIN_LIST
            |
            (
               NO? DOMAIN_LOOKUP
            )
            | DVMRP
            | EXTCOMMUNITY_LIST
            | FINGER
            | FLOW_CACHE
            | FLOW_EXPORT
            | FORWARD_PROTOCOL
            | FTP
            | GRATUITOUS_ARPS
            | HOST
            | HOST_ROUTING
            | HTTP
            | ICMP
            | IGMP
            | LOAD_SHARING
            | LOCAL
            | MFIB
            | MFWD
            | MOBILE
            | MROUTE
            | MSDP
            | MULTICAST
            | MULTICAST_ROUTING
            | NEXTHOP_LIST
            |
            (
               OSPF
               (
                  NAME_LOOKUP
               )
            )
            | RADIUS
            | RCMD
            | ROUTING //might want to use this eventually

            | SAP
            | SCP
            | SPD
            | SUBNET_ZERO
            | TCP
            | TELNET
            | TFTP
            | VERIFY
         )
      )
      | IP_ADDRESS_LITERAL
      | IP_FLOW_EXPORT_PROFILE
      |
      (
         IPV6
         (
            CEF
            | FIREWALL
            | HOP_LIMIT
            | HOST
            | LOCAL
            | MFIB
            | MFIB_MODE
            | MLD
            | MULTICAST
            | MULTICAST_ROUTING
            | ND
            |
            (
               OSPF NAME_LOOKUP
            )
            | PD_ROUTE_INJECTION
            | PIM
            | ROUTE
            | SOURCE_ROUTE
            | UNICAST_ROUTING
         )
      )
      | ISDN
      | KERNEL
      | L2PROTOCOL
      | LCD_MENU
      | LDAP_BASE_DN
      | LDAP_LOGIN
      | LDAP_LOGIN_DN
      | LDAP_NAMING_ATTRIBUTE
      | LDAP_SCOPE
      | LLDP
      | LOAD_INTERVAL
      | LOCALE
      | LOCALIP
      | LOCATION
      | LOGINSESSION
      |
      (
         MAC
         (
            ADDRESS_TABLE
         )
      )
      | MAC_ADDRESS_TABLE
      | MASTERIP
      | MEMORY
      | MEMORY_SIZE
      | MGCP
      | MGMT_SERVER
      | MGMT_USER
      | MICROCODE
      | MIRROR
      | MLS
      | MODEM
      | MON
      | MTA
      | MULTILINK
      | MVR
      | NAME_SERVER
      | NAMES
      | NAT_CONTROL
      | NETCONF
      | NETWORK_OBJECT
      | NETWORK_CLOCK
      | NETWORK_CLOCK_PARTICIPATE
      | NETWORK_CLOCK_SELECT
      |
      (
         NO
         (
            (
               AAA
               (
                  AUTHENTICATION
                  | NEW_MODEL
                  | ROOT
                  |
                  (
                     USER DEFAULT_ROLE
                  )
               )
            )
            | CLASS_MAP
            |
            (
               IP
               (
                  AS_PATH
                  |
                  (
                     TACACS SOURCE_INTERFACE
                  )
               )
            )
            |
            (
               SNMP_SERVER
               (
                  AAA
                  | CONTACT
                  | ENABLE
                  | GLOBALENFORCEPRIV
                  | LOCATION
               )
            )
            | SSH
         )
      )
      |
      (
         OSPFV3
         (
            NAME_LOOKUP
         )
      )
      | NETEXTHDR
      | ONEP
      | OWNER
      | PACKET_CAPTURE_DEFAULTS
      | PACKETCABLE
      | PAGER
      | PAN
      | PAN_OPTIONS
      | PARSER
      | PASSWORD
      | PERCENT
      | PLATFORM
      | POAP
      | PORT_CHANNEL
      | PORT_OBJECT
      | POWER
      | POWER_MONITOR
      | PRIORITY_QUEUE
      | PROCESS
      | PROMPT
      | PROTOCOL_OBJECT
      | QOS
      | QOS_SC
      | QUIT
      | RADIUS_COMMON_PW
      | RADIUS_SERVER
      | RD
      | RESOURCE
      | RESOURCE_POOL
      | ROUTE
      | ROUTE_TARGET
      | RTR
      | SAME_SECURITY_TRAFFIC
      | SAT
      | SCHEDULE
      | SCHEDULER
      | SCRIPTING
      | SDM
      | SECURITY
      | SERVER_TYPE
      | SERVICE_POLICY
      | SETUP
      | SHELFNAME
      | SHELL
      | SMTP_SERVER
      | SNMP
      | SOFTWARE
      | SPD
      | SPE
      | SPEED
      | STOPBITS
      | SSL
      | STATIC
      | SUBNET
      | SUBSCRIBER
      | SUBSCRIBE_TO
      | SUN
      | SYSCONTACT
      | SYSLOCATION
      | SYSOPT
      | TAG_SWITCHING
      | TELNET
      | TELNET_SERVER
      | TFTP_SERVER
      | THREAT_DETECTION
      | THREAT_VISIBILITY
      | THU
      |
      (
         TIMEOUT
         (
            CONN
            | CONN_HOLDDOWN
            | FLOATING_CONN
            | H225
            | H323
            | HALF_CLOSED
            | ICMP
            | ICMP_ERROR
            | IGP STALE_ROUTE
            | MGCP
            | MGCP_PAT
            | PAT_XLATE
            | SCTP
            | SIP
            | SIP_DISCONNECT
            | SIP_INVITE
            | SIP_MEDIA
            | SIP_PROVISIONAL_MEDIA
            | SUNRPC
            | TCP_PROXY_REASSEMBLY
            | UAUTH
            | UDP
            | XLATE
         )
      )
      | TRANSLATE
      | TUE
      | TUNNELED_NODE_ADDRESS
      | UDLD
      | UNABLE
      | UPGRADE
      | UPGRADE_PROFILE
      | UPLINK
      | USER_IDENTITY
      | USERPASSPHRASE
      | USE_VRF
      | USING
      | VALID_NETWORK_OUI_PROFILE
      | VERSION
      | VIDEO
      |
      (
         VLAN
         (
            ACCESS_LOG
            | CONFIGURATION
            | DOT1Q
            | INTERNAL
            | IFDESCR DETAIL
         )
      )
      | VMPS
      | VPDN
      | VSTACK
      | VTP
      | WED
      | WRR
      | WRR_QUEUE
      | X25
      | X29
      | XLATE
      | XML SERVER
   )
   (
      remaining_tokens += ~NEWLINE
   )* NEWLINE
;

null_no
:
   NO
   (
      TIMEOUT
   )
   NEWLINE
;

s_null
:
   null_block
   | null_single
   | null_no
;
