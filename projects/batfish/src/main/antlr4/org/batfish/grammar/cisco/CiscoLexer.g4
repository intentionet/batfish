lexer grammar CiscoLexer;

options {
   superClass = 'org.batfish.grammar.BatfishLexer';
}

@members {
private int lastTokenType = -1;
private boolean enableIPV6_ADDRESS = true;
private boolean enableIP_ADDRESS = true;
private boolean enableDEC = true;
private boolean enableACL_NUM = false;
private boolean enableCOMMUNITY_LIST_NUM = false;
private boolean enableREGEX = false;
private boolean _inAccessList = false;
private boolean inCommunitySet = false;
private boolean _foundry = false;
private boolean _cadant = false;

@Override
public void emit(Token token) {
    super.emit(token);
    if (token.getChannel() != HIDDEN) {
       lastTokenType = token.getType();
    }
}

public void setCadant(boolean cadant) {
   _cadant = cadant;
}

public void setFoundry(boolean foundry) {
   _foundry = foundry;
}

public String printStateVariables() {
   StringBuilder sb = new StringBuilder();
   sb.append("enableIPV6_ADDRESS: " + enableIPV6_ADDRESS + "\n");
   sb.append("enableIP_ADDRESS: " + enableIP_ADDRESS + "\n");
   sb.append("enableDEC: " + enableDEC + "\n");
   sb.append("enableACL_NUM: " + enableACL_NUM+ "\n");
   sb.append("enableCOMMUNITY_LIST_NUM: " + enableCOMMUNITY_LIST_NUM + "\n");
   return sb.toString();
}

}

tokens {
   ACL_NUM_APPLETALK,
   ACL_NUM_EXTENDED,
   ACL_NUM_EXTENDED_IPX,
   ACL_NUM_EXTENDED_MAC,
   ACL_NUM_FOUNDRY_L2,
   ACL_NUM_IPX,
   ACL_NUM_IPX_SAP,
   ACL_NUM_MAC,
   ACL_NUM_OTHER,
   ACL_NUM_PROTOCOL_TYPE_CODE,
   ACL_NUM_STANDARD,
   AS_PATH_SET_REGEX,
   ASA_BANNER_LINE,
   COMMUNITY_LIST_NUM_EXPANDED,
   COMMUNITY_LIST_NUM_STANDARD,
   COMMUNITY_SET_REGEX,
   CONFIG_SAVE,
   DSA1024,
   END_CADANT,
   HEX_FRAGMENT,
   IS_LOCAL,
   ISO_ADDRESS,
   LINE_CADANT,
   PAREN_LEFT_LITERAL,
   PAREN_RIGHT_LITERAL,
   PASSWORD_SEED,
   PIPE,
   PROMPT_TIMEOUT,
   QUOTED_TEXT,
   RAW_TEXT,
   SELF_SIGNED,
   SLIP_PPP,
   STATEFUL_DOT1X,
   STATEFUL_KERBEROS,
   STATEFUL_NTLM,
   TEXT,
   VALUE,
   WIRED,
   WISPR
} // Cisco Keywords

AAA
:
   'aaa'
;

AAA_PROFILE
:
   'aaa-profile'
;

AAA_SERVER
:
   'aaa-server'
;

AAA_USER
:
   'aaa-user'
;

ABSOLUTE_TIMEOUT
:
   'absolute-timeout'
;

ACCEPT_DIALIN
:
   'accept-dialin'
;

ACCEPT_LIFETIME
:
   'accept-lifetime'
;

ACCEPT_OWN
:
   'accept-own'
;

ACCEPT_REGISTER
:
   'accept-register'
;

ACCEPT_RP
:
   'accept-rp'
;

ACCESS
:
   'access'
;

ACCESS_CLASS
:
   'access-class'
;

ACCESS_GROUP
:
   'access-group'
;

ACCESS_LIST
:
   'access-list'
   {enableACL_NUM = true; enableDEC = false;_inAccessList = true;}

;

ACCESS_LOG
:
   'access-log'
;

ACCESS_MAP
:
   'access-map'
;

ACCOUNTING
:
   'accounting'
;

ACCOUNTING_LIST
:
   'accounting-list'
;

ACCOUNTING_PORT
:
   'accounting-port'
;

ACCOUNTING_SERVER_GROUP
:
   'accounting-server-group'
;

ACCOUNTING_THRESHOLD
:
   'accounting-threshold'
;

ACCT_PORT
:
   'acct-port'
;

ACFE
:
   'acfe'
;

ACK
:
   'ack'
;

ACL
:
   'acl'
;

ACL_POLICY
:
   'acl-policy'
;

ACLLOG
:
   'acllog'
;

ACTION
:
   'action'
;

ACTION_TYPE
:
   'action-type'
;

ACTIVATE
:
   'activate'
;

ACTIVATE_SERVICE_WHITELIST
:
   'activate-service-whitelist'
;

ACTIVATION_CHARACTER
:
   'activation-character'
;

ACTIVE
:
   'active'
;

ADD
:
   'add'
;

ADD_VLAN
:
   'add-vlan'
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
;

ADDRESS_FAMILY
:
   'address-family'
;

ADDRESS_HIDING
:
   'address-hiding'
;

ADDRESS_POOL
:
   'address-pool'
;

ADDRESS_POOLS
:
   'address-pools'
;

ADDRESS_RANGE
:
   'address-range'
;

ADDRESS_TABLE
:
   'address-table'
;

ADDRGROUP
:
   'addrgroup'
;

ADJACENCY
:
   'adjacency'
;

ADJACENCY_CHECK
:
   'adjacency-check'
;

ADJACENCY_STALE_TIMER
:
   'adjacency-stale-timer'
;

ADJMGR
:
   'adjmgr'
;

ADMIN
:
   'admin'
;

ADMIN_DIST
:
   'admin-dist'
;

ADMIN_DISTANCE
:
   'admin-distance'
;

ADMIN_STATE
:
   'admin-state'
;

ADMIN_VDC
:
   'admin-vdc'
;

ADMINISTRATIVE_WEIGHT
:
   'administrative-weight'
;

ADMISSION
:
   'admission'
;

ADMISSION_CONTROL
:
   'admission-control'
;

ADP
:
   'adp'
;

ADVERTISE
:
   'advertise'
;

ADVERTISEMENT_INTERVAL
:
   'advertisement-interval'
;

ADVERTISE_INACTIVE
:
   'advertise-inactive'
;

ADVERTISE_MAP
:
   'advertise-map'
;

AES
:
   'aes'
;

AES128
:
   'aes128'
;

AES192
:
   'aes192'
;

AES256
:
   'aes256'
;

AES128_SHA1
:
   'aes128-sha1'
;

AES256_SHA1
:
   'aes256-sha1'
;

AESA
:
   'aesa'
;

AF_GROUP
:
   'af-group'
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

AFFINITY
:
   'affinity'
;

AFFINITY_MAP
:
   'affinity-map'
;

AGE
:
   'age'
;

AGGREGATE
:
   'aggregate'
;

AGGREGATE_ADDRESS
:
   'aggregate-address'
;

AGING
:
   'aging'
;

AHP
:
   'ahp'
;

AIRGROUP
:
   'airgroup'
;

AIRGROUPSERVICE
:
   'airgroupservice'
;

AIS_SHUT
:
   'ais-shut'
;

ALARM
:
   'alarm'
;

ALARM_REPORT
:
   'alarm-report'
;

ALERT_GROUP
:
   'alert-group'
;

ALERTS
:
   'alerts'
;

ALG
:
   'alg'
;

ALG_BASED_CAC
:
   'alg-based-cac'
;

ALIAS
:
   'alias' -> pushMode ( M_Alias )
;

ALL
:
   'all'
;

ALL_ALARMS
:
   'all-alarms'
;

ALL_OF_ROUTER
:
   'all-of-router'
;

ALLOCATE
:
   'allocate'
;

ALLOW
:
   'allow'
;

ALLOW_CONNECTIONS
:
   'allow-connections'
;

ALLOW_DEFAULT
:
   'allow-default'
;

ALLOW_FAIL_THROUGH
:
   'allow-fail-through'
;

ALLOW_NOPASSWORD_REMOTE_LOGIN
:
   'allow-nopassword-remote-login'
;

ALLOW_SELF_PING
:
   'allow-self-ping'
;

ALLOWED
:
   'allowed'
;

ALLOWAS_IN
:
   'allowas-in'
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

ALWAYS_ON
:
   'always-on'
;

ALWAYS_ON_VPN
:
   'always-on-vpn'
;

AM_DISABLE
:
   'am-disable'
;

AM_SCAN_PROFILE
:
   'am-scan-profile'
;

AMON
:
   'amon'
;

AMT
:
   'amt'
;

AND
:
   'and'
;

ANTENNA
:
   'antenna'
;

ANY
:
   'any'
;

ANY4
:
   'any4'
;

ANY6
:
   'any6'
;

ANYCONNECT
:
   'anyconnect'
;

ANYCONNECT_ESSENTIALS
:
   'anyconnect-essentials'
;

AOL
:
   'aol'
;

AP
:
   'ap'
;

AP_BLACKLIST_TIME
:
   'ap-blacklist-time'
;

AP_CLASSIFICATION_RULE
:
   'ap-classification-rule'
;

AP_CRASH_TRANSFER
:
   'ap-crash-transfer'
;

AP_GROUP
:
   'ap-group'
;

AP_LACP_STRIPING_IP
:
   'ap-lacp-striping-ip'
;

AP_NAME
:
   'ap-name'
;

AP_RULE_MATCHING
:
   'ap-rule-matching'
;

AP_SYSTEM_PROFILE
:
   'ap-system-profile'
;

API
:
   'api'
;

APP
:
   'app'
;

APPCATEGORY
:
   'appcategory'
;

APPLETALK
:
   'appletalk'
;

APPLICATION
:
   'application'
;

APPLY
:
   'apply'
;

AQM_REGISTER_FNF
:
   'aqm-register-fnf'
;

ARAP
:
   'arap'
;

ARCHIVE
:
   'archive'
;

ARCHIVE_LENGTH
:
   'archive-length'
;

ARCHIVE_SIZE
:
   'archive-size'
;

AREA
:
   'area'
;

AREA_PASSWORD
:
   'area-password'
;

ARM_PROFILE
:
   'arm-profile'
;

ARM_RF_DOMAIN_PROFILE
:
   'arm-rf-domain-profile'
;

ARP
:
   'arp'
   { enableIPV6_ADDRESS = false; }

;

AS_NUMBER
:
   'as-number'
;

AS_OVERRIDE
:
   'as-override'
;

AS_PATH
:
   'as-path' -> pushMode ( M_AsPath )
;

AS_PATH_SET
:
   'as-path-set'
;

AS_SET
:
   'as-set'
;

AS
:
   'as'
;

ASA
:
   'ASA'
;

ASCII_AUTHENTICATION
:
   'ascii-authentication'
;

ASDM
:
   'asdm'
;

ASDM_BUFFER_SIZE
:
   'asdm-buffer-size'
;

ASN
:
   'asn'
;

ASSEMBLER
:
   'assembler'
;

ASSIGNMENT
:
   'assignment'
;

ASSOC_RETRANSMIT
:
   'assoc-retransmit'
;

ASSOCIATE
:
   'associate'
;

ASSOCIATION
:
   'association'
;

ASYMMETRIC
:
   'asymmetric'
;

ASYNC
:
   'async'
;

ASYNC_BOOTP
:
   'async-bootp'
;

ASYNCHRONOUS
:
   'asynchronous'
;

ATM
:
   'atm'
;

ATTEMPTS
:
   'attempts'
;

ATTRIBUTE
:
   'attribute'
;

ATTRIBUTE_DOWNLOAD
:
   'attribute-download'
;

ATTRIBUTE_MAP
:
   'attribute-map'
;

ATTRIBUTE_NAMES
:
   'attribute-names'
;

ATTRIBUTE_SET
:
   'attribute-set'
;

ATTRIBUTES
:
   'attributes'
;

AUDIT
:
   'audit'
;

AUTH
:
   'auth'
;

AUTH_FAILURE_BLACKLIST_TIME
:
   'auth-failure-blacklist-time'
;

AUTH_PORT
:
   'auth-port'
;

AUTH_PROXY
:
   'auth-proxy'
;

AUTH_SERVER
:
   'auth-server'
;

AUTHENTICATE
:
   'authenticate'
;

AUTHENTICATION
:
   'authentication' -> pushMode ( M_Authentication )
;

AUTHENTICATION_DOT1X
:
   'authentication-dot1x'
;

AUTHENTICATION_KEY
:
   'authentication-key'
;

AUTHENTICATION_MAC
:
   'authentication-mac'
;

AUTHENTICATION_PORT
:
   'authentication-port'
;

AUTHENTICATION_RETRIES
:
   'authentication-retries'
;

AUTHENTICATION_SERVER
:
   'authentication-server'
;

AUTHENTICATION_SERVER_GROUP
:
   'authentication-server-group'
;

AUTHORITATIVE
:
   'authoritative'
;

AUTHORIZATION
:
   'authorization'
;

AUTHORIZATION_REQUIRED
:
   'authorization-required'
;

AUTHORIZATION_SERVER_GROUP
:
   'authorization-server-group'
;

AUTO
:
   'auto'
;

AUTO_CERT_ALLOW_ALL
:
   'auto-cert-allow-all'
;

AUTO_CERT_ALLOWED_ADDRS
:
   'auto-cert-allowed-addrs'
;

AUTO_CERT_PROV
:
   'auto-cert-prov'
;

AUTO_COST
:
   'auto-cost'
;

AUTO_DISCARD
:
   'auto-discard'
;

AUTO_IMPORT
:
   'auto-import'
;

AUTO_LOCAL_ADDR
:
   'auto-local-addr'
;

AUTO_RECOVERY
:
   'auto-recovery'
;

AUTO_RP
:
   'auto-rp'
;

AUTO_SHUTDOWN_NEW_NEIGHBORS
:
   'auto-shutdown-new-neighbors'
;

AUTO_SUMMARY
:
   'auto-summary'
;

AUTO_SYNC
:
   'auto-sync'
;

AUTO_TUNNEL
:
   'auto-tunnel'
;

AUTO_UPGRADE
:
   'auto-upgrade'
;

AUTOHANGUP
:
   'autohangup'
;

AUTORECOVERY
:
   'autorecovery'
;

AUTOROUTE
:
   'autoroute'
;

AUTORP
:
   'autorp'
;

AUTOSELECT
:
   'autoselect'
;

AUTOSTATE
:
   'autostate'
;

AUX
:
   'aux'
;

BACK_UP
:
   'back-up'
;

BACKBONEFAST
:
   'backbonefast'
;

BACKGROUND_ROUTES_ENABLE
:
   'background-routes-enable'
;

BACKOFF_TIME
:
   'backoff-time'
;

BACKUP
:
   'backup'
;

BACKUPCRF
:
   'backupcrf'
;

BAND_STEERING
:
   'band-steering'
;

BANDWIDTH
:
   'bandwidth'
;

BANDWIDTH_CONTRACT
:
   'bandwidth-contract'
;

BANDWIDTH_PERCENTAGE
:
   'bandwidth-percentage'
;

BANNER
:
   'banner' -> pushMode ( M_Banner )
;

BASH
:
   'bash'
;

BCMC_OPTIMIZATION
:
   'bcmc-optimization'
;

BCN_RPT_REQ_PROFILE
:
   'bcn-rpt-req-profile'
;

BEACON
:
   'beacon'
;

BESTPATH
:
   'bestpath'
;

BESTPATH_LIMIT
:
   'bestpath-limit'
;

BEYOND_SCOPE
:
   'beyond-scope'
;

BFD
:
   'bfd'
;

BFD_ENABLE
:
   'bfd-enable'
;

BGP
:
   'bgp'
;

BGP_COMMUNITY
:
   'bgp-community'
;

BGP_POLICY
:
   'bgp-policy'
;

BIDIR_ENABLE
:
   'bidir-enable'
;

BIDIR_OFFER_INTERVAL
:
   'bidir-offer-interval'
;

BIDIR_OFFER_LIMIT
:
   'bidir-offer-limit'
;

BIDIR_RP_LIMIT
:
   'bidir-rp-limit'
;

BIFF
:
   'biff'
;

BIND
:
   'bind'
;

BITTORRENT
:
   'bittorrent'
;

BITTORRENT_APPLICATION
:
   'bittorrent-application'
;

BKUP_LMS_IP
:
   'bkup-lms-ip'
;

BLACKLIST
:
   'blacklist'
;

BLACKLIST_TIME
:
   'blacklist-time'
;

BLOCK
:
   'block'
;

BLOGGERD
:
   'bloggerd'
;

BOOT
:
   'boot'
;

BOOT_END_MARKER
:
   'boot-end-marker'
;

BOOT_START_MARKER
:
   'boot-start-marker'
;

BOOTFILE
:
   'bootfile'
;

BOOTP_RELAY
:
   'bootp-relay'
;

BOOTP
:
   'bootp'
;

BOOTPC
:
   'bootpc'
;

BOOTPS
:
   'bootps'
;

BORDER
:
   'border'
;

BORDER_ROUTER
:
   'border-router'
;

BOTH
:
   'both'
;

BOUNDARY
:
   'boundary'
;

BPDUFILTER
:
   'bpdufilter'
;

BPDUGUARD
:
   'bpduguard'
;

BREAKOUT
:
   'breakout'
;

BRIDGE
:
   'bridge'
;

BRIDGE_DOMAIN
:
   'bridge-domain'
;

BRIDGE_GROUP
:
   'bridge-group'
;

BRIDGE_PRIORITY
:
   'bridge-priority'
;

BROADCAST
:
   'broadcast'
;

BROADCAST_ADDRESS
:
   'broadcast-address'
;

BROADCAST_FILTER
:
   'broadcast-filter'
;

BSD_CLIENT
:
   'bsd-client'
;

BSD_USERNAME
:
   'bsd-username'
;

BSR_BORDER
:
   'bsr-border'
;

BSR_CANDIDATE
:
   'bsr-candidate'
;

BUCKETS
:
   'buckets'
;

BUFFER_SIZE
:
   'buffer-size'
;

BUFFERED
:
   'buffered'
;

BUILDING_CONFIGURATION
:
   'Building configuration'
;

BUNDLE
:
   'bundle'
;

BUFFERS
:
   'buffers'
;

BURST_SIZE
:
   'burst-size'
;

CA
:
   'ca'
;

CABLE
:
   'cable'
;

CABLE_DOWNSTREAM
:
   'cable-downstream'
;

CABLE_RANGE
:
   'cable-range'
;

CABLE_UPSTREAM
:
   'cable-upstream'
;

CABLELENGTH
:
   'cablelength' -> pushMode ( M_COMMENT )
;

CACHE
:
   'cache'
;

CACHE_TIMEOUT
:
   'cache-timeout'
;

CALL
:
   'call'
;

CALL_BLOCK
:
   'call-block'
;

CALL_FORWARD
:
   'call-forward'
;

CALL_HOME
:
   'call-home'
;

CALL_MANAGER_FALLBACK
:
   'call-manager-fallback'
;

CALLER_ID
:
   'caller-id'
;

CALLHOME
:
   'callhome'
;

CAM_ACL
:
   'cam-acl'
;

CAM_PROFILE
:
   'cam-profile'
;

CAPABILITY
:
   'capability'
;

CAPTIVE
:
   'captive'
;

CAPTIVE_PORTAL
:
   'captive-portal'
;

CAPTIVE_PORTAL_CERT
:
   'captive-portal-cert'
;

CAPTURE
:
   'capture'
;

CARD
:
   'card'
;

CARD_TRAP_INH
:
   'card-trap-inh'
;

CARRIER_DELAY
:
   'carrier-delay'
;

CAS_CUSTOM
:
   'cas-custom'
;

CASE
:
   'case'
;

CCM
:
   'ccm'
;

CCM_GROUP
:
   'ccm-group'
;

CCM_MANAGER
:
   'ccm-manager'
;

CDP
:
   'cdp'
;

CDP_URL
:
   'cdp-url'
;

CEF
:
   'cef'
;

CENTRALIZED_LICENSING_ENABLE
:
   'centralized-licensing-enable'
;

CERTIFICATE
:
   'certificate' -> pushMode ( M_Certificate )
;

CFS
:
   'cfs'
;

CGMP
:
   'cgmp'
;

CHAIN
:
   'chain'
;

CHANGES
:
   'changes'
;

CHANNEL
:
   'channel'
;

CHANNEL_GROUP
:
   'channel-group'
;

CHANNEL_PROTOCOL
:
   'channel-protocol'
;

CHANNELIZED
:
   'channelized'
;

CHAP
:
   'chap'
;

CHARGEN
:
   'chargen'
;

CHASSIS_ID
:
   'chassis-id'
;

CHAT_SCRIPT
:
   'chat-script'
;

CHECK
:
   'check'
;

CIPC
:
   'cipc'
;

CIR
:
   'cir'
;

CIRCUIT_TYPE
:
   'circuit-type'
;

CISP
:
   'cisp'
;

CITRIX_ICA
:
   'citrix-ica'
;

CLASS
:
   'class'
;

CLASS_DEFAULT
:
   'class-default'
;

CLASS_MAP
:
   'class-map'
;

CLASSLESS
:
   'classless'
;

CLEANUP
:
   'cleanup'
;

CLEAR
:
   'clear'
;

CLI
:
   'cli'
;

CLIENT
:
   'client'
;

CLIENT_GROUP
:
   'client-group'
;

CLIENT_IDENTIFIER
:
   'client-identifier'
;

CLIENT_NAME
:
   'client-name'
;

CLIENT_TO_CLIENT
:
   'client-to-client'
;

CLNS
:
   'clns'
;

CLOCK
:
   'clock'
;

CLOCK_PERIOD
:
   'clock-period'
;

CLOSED
:
   'closed'
;

CLUSTER
:
   'cluster'
;

CLUSTER_ID
:
   'cluster-id'
;

CMD
:
   'cmd'
;

CMTS
:
   'cmts'
;

CNS
:
   'cns'
;

COAP
:
   'coap'
;

CODEC
:
   'codec'
;

COLLECT
:
   'collect'
;

COLLECT_STATS
:
   'collect-stats'
;

COMM_LIST
:
   'comm-list'
;

COMMAND
:
   'command' -> pushMode ( M_Command )
;

COMMANDER_ADDRESS
:
   'commander-address'
   { enableIPV6_ADDRESS = false; }

;

COMMANDS
:
   'commands'
;

COMMIT
:
   'commit'
;

COMMON
:
   'common'
;

COMMUNITY
:
   'community'
   { enableIPV6_ADDRESS = false; }

;

COMMUNITY_LIST
:
   'community-list'
   {
      enableIPV6_ADDRESS = false;
      enableCOMMUNITY_LIST_NUM = true;
      enableDEC = false;
   }

;

COMMUNITY_MAP
:
   'community-map' -> pushMode ( M_Name )
;

COMMUNITY_SET
:
   'community-set'
   {
      inCommunitySet = true;
      enableIPV6_ADDRESS = false;
   }

;

COMPARE_ROUTERID
:
   'compare-routerid'
;

COMPATIBLE
:
   'compatible'
;

CON
:
   'con'
;

CONF_LEVEL_INCR
:
   'conf-level-incr'
;

CONFDCONFIG
:
   'confdConfig'
;

CONFED
:
   'confed'
;

CONFEDERATION
:
   'confederation'
;

CONFIG
:
   'config'
;

CONFIG_COMMANDS
:
   'config-commands'
;

CONFIG_REGISTER
:
   'config-register'
;

CONFIGURATION
:
   'configuration'
;

CONFIGURE
:
   'configure'
;

CONFLICT_POLICY
:
   'conflict-policy'
;

CONFORM_ACTION
:
   'conform-action'
;

CONGESTION_CONTROL
:
   'congestion-control'
;

CONN
:
   'conn'
;

CONN_HOLDDOWN
:
   'conn-holddown'
;

CONNECT_RETRY
:
   'connect-retry'
;

CONNECT_SOURCE
:
   'connect-source'
;

CONNECTED
:
   'connected'
;

CONNECTION
:
   'connection'
;

CONNECTION_MODE
:
   'connection-mode'
;

CONNECTION_REUSE
:
   'connection-reuse'
;

CONSOLE
:
   'console'
;

CONTACT
:
   'contact'
;

CONTACT_EMAIL_ADDR
:
   'contact-email-addr'
;

CONTACT_NAME
:
   'contact-name' -> pushMode ( M_Description )
;

CONTEXT
:
   'context'
;

CONTINUE
:
   (
      'continue'
   )
   |
   (
      'Continue'
   )
;

CONTRACT_ID
:
   'contract-id'
;

CONTROL_APPS_USE_MGMT_PORT
:
   'control-apps-use-mgmt-port'
;

CONTROL_DIRECTION
:
   'control-direction'
;

CONTROL_PLANE
:
   'control-plane'
;

CONTROL_PLANE_SECURITY
:
   'control-plane-security'
;

CONTROL_WORD
:
   'control-word'
;

CONTROLLER
:
   'controller' -> pushMode ( M_Interface )
;

CONVERSION_ERROR
:
   'conversion-error'
;

CONTROLLER_IP
:
   'controller-ip'
;

COOKIE
:
   'cookie'
;

COPP
:
   'copp'
;

COPS
:
   'cops'
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

COS_MAPPING
:
   'cos-mapping'
;

COS_QUEUE_GROUP
:
   'cos-queue-group'
;

COST
:
   'cost'
;

COST_COMMUNITY
:
   'cost-community'
;

COUNT
:
   'count'
;

COUNTRY
:
   'country'
;

COUNTRY_CODE
:
   'country-code'
;

COUNTER
:
   'counter'
;

COUNTERS
:
   'counters'
;

CPD
:
   'cpd'
;

CPTONE
:
   'cptone'
;

CPU_SHARE
:
   'cpu-share'
;

CRASHINFO
:
   'crashinfo'
;

CRC
:
   'crc'
;

CREDENTIALS
:
   'credentials'
;

CRITICAL
:
   'critical'
;

CRYPTO
:
   'crypto'
;

CRYPTOCHECKSUM
:
   'Cryptochecksum'
;

CRYPTO_LOCAL
:
   'crypto-local'
;

CRYPTOGRAPHIC_ALGORITHM
:
   'cryptographic-algorithm'
;

CRL
:
   'crl'
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

CSD
:
   'csd'
;

CSNP_INTERVAL
:
   'csnp-interval'
;

CTIQBE
:
   'ctiqbe'
;

CTL_FILE
:
   'ctl-file'
;

CTS
:
   'cts'
;

CURRENT_CONFIGURATION
:
   'Current configuration'
;

CUSTOM
:
   'custom'
;

CUSTOMER_ID
:
   'customer-id'
;

CVX
:
   'cvx'
;

CWR
:
   'cwr'
;

D20_GGRP_DEFAULT
:
   'd20-ggrp-default'
;

D30_GGRP_DEFAULT
:
   'd30-ggrp-default'
;

DAEMON
:
   'daemon'
;

DAMPEN
:
   'dampen'
;

DAMPEN_IGP_METRIC
:
   'dampen-igp-metric'
;

DAMPENING
:
   'dampening'
;

DATA_PRIVACY
:
   'data-privacy'
;

DATABASE
:
   'database'
;

DATABITS
:
   'databits'
;

DAYTIME
:
   'daytime'
;

DBL
:
   'dbl'
;

DCB
:
   'dcb'
;

DCB_BUFFER_THRESHOLD
:
   'dcb-buffer-threshold'
;

DCB_POLICY
:
   'dcb-policy'
;

DCBX
:
   'dcbx'
;

DCE_MODE
:
   'dce-mode'
;

DEAD_INTERVAL
:
   'dead-interval'
;

DEADTIME
:
   'deadtime'
;

DEBUG
:
   'debug'
;

DEBUG_TRACE
:
   'debug-trace'
;

DEBUGGING
:
   'debugging'
;

DECAP_GROUP
:
   'decap-group'
;

DEFAULT
:
   'default'
;

DEFAULT_ACTION
:
   'default-action'
;

DEFAULT_COST
:
   'default-cost'
;

DEFAULT_DESTINATION
:
   'default-destination'
;

DEFAULT_DOMAIN
:
   'default-domain'
;

DEFAULT_GATEWAY
:
   'default-gateway'
;

DEFAULT_GROUP_POLICY
:
   'default-group-policy'
;

DEFAULT_GUEST_ROLE
:
   'default-guest-role'
;

DEFAULT_GW
:
   'default-gw'
;

DEFAULT_INFORMATION
:
   'default-information'
;

DEFAULT_INFORMATION_ORIGINATE
:
   'default-information-originate'
;

DEFAULT_INSPECTION_TRAFFIC
:
   'default-inspection-traffic'
;

DEFAULT_MAX_FRAME_SIZE
:
   'default-max-frame-size'
;

DEFAULT_METRIC
:
   'default-metric'
;

DEFAULT_NETWORK
:
   'default-network'
;

DEFAULT_ORIGINATE
:
   'default-originate'
;

DEFAULT_ROLE
:
   'default-role'
;

DEFAULT_ROUTER
:
   'default-router'
;

DEFAULT_TASKGROUP
:
   'default-taskgroup'
;

DEFAULT_TOS_QOS10
:
   'default-tos-qos10'
;

DEFAULT_VALUE
:
   'default-value'
;

DEFINITION
:
   'definition'
;

DEL
:
   'Del'
;

DELIMITER
:
   'delimiter'
;

DELAY
:
   'delay'
;

DELAY_START
:
   'delay-start'
;

DELETE
:
   'delete'
;

DELETE_DYNAMIC_LEARN
:
   'delete-dynamic-learn'
;

DEMAND_CIRCUIT
:
   'demand-circuit'
;

DENSE_MODE
:
   'dense-mode'
;

DENY
:
   'deny'
;

DENY_INTER_USER_TRAFFIC
:
   'deny-inter-user-traffic'
;

DEPI
:
   'depi'
;

DEPI_CLASS
:
   'depi-class'
;

DEPI_TUNNEL
:
   'depi-tunnel'
;

DEPLOY
:
   'deploy'
;

DERIVATION_RULES
:
   'derivation-rules'
;

DES
:
   'des' -> pushMode ( M_Des )
;

DES_SHA1
:
   'des-sha1'
;

DESCRIPTION
:
   'description' -> pushMode ( M_Description )
;

DESIRABLE
:
   'desirable'
;

DEST_IP
:
   'dest-ip'
;

DESTINATION
:
   'destination'
;

DESTINATION_PATTERN
:
   'destination-pattern'
;

DESTINATION_PROFILE
:
   'destination-profile'
;

DESTINATION_SLOT
:
   'destination-slot'
;

DESTINATION_UNREACHABLE
:
   'destination-unreachable'
;

DESTINATION_VRF
:
   'destination-vrf'
;

DETAIL
:
   'detail'
;

DETECT_ADHOC_NETWORK
:
   'detect-adhoc-network'
;

DETECT_AP_FLOOD
:
   'detect-ap-flood'
;

DETECT_AP_IMPERSONATION
:
   'detect-ap-impersonation'
;

DETECT_BAD_WEP
:
   'detect-bad-wep'
;

DETECT_BEACON_WRONG_CHANNEL
:
   'detect-beacon-wrong-channel'
;

DETECT_CHOPCHOP_ATTACK
:
   'detect-chopchop-attack'
;

DETECT_CLIENT_FLOOD
:
   'detect-client-flood'
;

DETECT_CTS_RATE_ANOMALY
:
   'detect-cts-rate-anomaly'
;

DETECT_EAP_RATE_ANOMALY
:
   'detect-eap-rate-anomaly'
;

DETECT_HOTSPOTTER
:
   'detect-hotspotter'
;

DETECT_HT_40MHZ_INTOLERANCE
:
   'detect-ht-40mhz-intolerance'
;

DETECT_HT_GREENFIELD
:
   'detect-ht-greenfield'
;

DETECT_INVALID_ADDRESS_COMBINATION
:
   'detect-invalid-address-combination'
;

DETECT_INVALID_MAC_OUI
:
   'detect-invalid-mac-oui'
;

DETECT_MALFORMED_ASSOCIATION_REQUEST
:
   'detect-malformed-association-request'
;

DETECT_MALFORMED_AUTH_FRAME
:
   'detect-malformed-auth-frame'
;

DETECT_MALFORMED_HTIE
:
   'detect-malformed-htie'
;

DETECT_MALFORMED_LARGE_DURATION
:
   'detect-malformed-large-duration'
;

DETECT_MISCONFIGURED_AP
:
   'detect-misconfigured-ap'
;

DETECT_OVERFLOW_EAPOL_KEY
:
   'detect-overflow-eapol-key'
;

DETECT_OVERFLOW_IE
:
   'detect-overflow-ie'
;

DETECT_RATE_ANOMALIES
:
   'detect-rate-anomalies'
;

DETECT_RTS_RATE_ANOMALY
:
   'detect-rts-rate-anomaly'
;

DETECT_TKIP_REPLAY_ATTACK
:
   'detect-tkip-replay-attack'
;

DETECT_VALID_SSID_MISUSE
:
   'detect-valid-ssid-misuse'
;

DETECT_WIRELESS_BRIDGE
:
   'detect-wireless-bridge'
;

DETECT_WIRELESS_HOSTED_NETWORK
:
   'detect-wireless-hosted-network'
;

DETERMINISTIC_MED
:
   'deterministic-med'
;

DEV
:
   'dev' -> pushMode ( M_Interface )
;

DEVICE
:
   'device'
;

DEVICE_SENSOR
:
   'device-sensor'
;

DISABLE_CONNECTED_CHECK
:
   'disable-connected-check'
;

DISABLE_PEER_AS_CHECK
:
   'disable-peer-as-check'
;

DISCRIMINATOR
:
   'discriminator'
;

DISPUTE
:
   'dispute'
;

DF
:
   'df'
;

DF_BIT
:
   'df-bit'
;

DFA_REGEX
:
   'dfa-regex'
;

DFS
:
   'dfs'
;

DHCP
:
   'dhcp'
;

DHCP_GIADDR
:
   'dhcp-giaddr'
;

DHCPD
:
   'dhcpd'
;

DHCPRELAY
:
   'dhcprelay'
;

DIAGNOSTIC
:
   'diagnostic'
;

DIAGNOSTIC_SIGNATURE
:
   'diagnostic-signature'
;

DIAL_CONTROL_MIB
:
   'dial-control-mib'
;

DIAL_PEER
:
   'dial-peer'
;

DIAL_STRING
:
   'dial-string'
;

DIALER
:
   'dialer'
;

DIALER_GROUP
:
   'dialer-group'
;

DIALER_LIST
:
   'dialer-list'
;

DIALPLAN_PATTERN
:
   'dialplan-pattern'
;

DIALPLAN_PROFILE
:
   'dialplan-profile'
;

DIRECT
:
   'direct'
;

DIRECT_INWARD_DIAL
:
   'direct-inward-dial'
;

DIRECTED_BROADCAST
:
   'directed-broadcast'
;

DIRECTED_REQUEST
:
   'directed-request'
;

DISABLE
:
   'disable'
;

DISABLE_ADVERTISEMENT
:
   'disable-advertisement'
;

DISCARD
:
   'discard'
;

DISCARD_ROUTE
:
   'discard-route'
;

DISCOVERED_AP_CNT
:
   'discovered-ap-cnt'
;

DISCOVERY
:
   'discovery'
;

DISTANCE
:
   'distance'
;

DISTRIBUTE
:
   'distribute'
;

DISTRIBUTE_LIST
:
   'distribute-list'
;

DISTRIBUTION
:
   'distribution'
;

DM_FALLBACK
:
   'dm-fallback'
;

DNS
:
   'dns'
;

DNS_DOMAIN
:
   'dns-domain'
;

DNS_GUARD
:
   'dns-guard'
;

DNS_SERVER
:
   'dns-server'
;

DNSIX
:
   'dnsix'
;

DO
:
   'do'
;

DOCSIS_ENABLE
:
   'docsis-enable'
;

DOCSIS_GROUP
:
   'docsis-group'
;

DOCSIS_POLICY
:
   'docsis-policy'
;

DOCSIS_VERSION
:
   'docsis-version'
;

DOCSIS30_ENABLE
:
   'docsis30-enable'
;

DOMAIN
:
   'domain'
;

DOMAIN_ID
:
   'domain-id'
;

DOMAIN_LIST
:
   'domain-list'
;

DOMAIN_LOOKUP
:
   'domain-lookup'
;

DOMAIN_NAME
:
   'domain-name'
;

DONE
:
   'done'
;

DONT_CAPABILITY_NEGOTIATE
:
   'dont-capability-negotiate'
;

DOS_PROFILE
:
   'dos-profile'
;

DOT11
:
   'dot11'
;

DOT11A_RADIO_PROFILE
:
   'dot11a-radio-profile'
;

DOT11G_RADIO_PROFILE
:
   'dot11g-radio-profile'
;

DOT11K_PROFILE
:
   'dot11k-profile'
;

DOT11R_PROFILE
:
   'dot11r-profile'
;

DOT1P_PRIORITY
:
   'dot1p-priority'
;

DOT1Q
:
   'dot1' [Qq]
;

DOT1Q_TUNNEL
:
   'dot1q-tunnel'
;

DOT1X
:
   'dot1x'
;

DOT1X_DEFAULT_ROLE
:
   'dot1x-default-role'
;

DOT1X_ENABLE
:
   'dot1x-enable'
;

DOT1X_SERVER_GROUP
:
   'dot1x-server-group'
;

DOWNLINK
:
   'downlink'
;

DOWNSTREAM
:
   'downstream'
;

DOWNSTREAM_START_THRESHOLD
:
   'downstream-start-threshold'
;

DPG
:
    'dpg'
;

DR_PRIORITY
:
   'dr-priority'
;

DROP
:
   'drop'
;

DS_HELLO_INTERVAL
:
   'ds-hello-interval'
;

DS_MAX_BURST
:
   'ds-max-burst'
;

DS0_GROUP
:
   'ds0-group'
;

DSCP
:
   'dscp'
;

DSCP_VALUE
:
   'dscp-value'
;

DSG
:
   'dsg'
;

DSL
:
   'dsl'
;

DSP
:
   'dsp'
;

DSPFARM
:
   'dspfarm'
;

DSS
:
   'dss'
;

DST_NAT
:
   'dst-nat'
;

DSU
:
   'dsu'
;

DTMF_RELAY
:
   'dtmf-relay'
;

DUAL_ACTIVE
:
   'dual-active'
;

DUAL_AS
:
   'dual-as'
;

DUAL_MODE_DEFAULT_VLAN
:
   'dual-mode-default-vlan'
;

DUPLEX
:
   'duplex'
;

DUPLICATE_MESSAGE
:
   'duplicate-message'
;

DURATION
:
   'duration'
;

DVMRP
:
   'dvmrp'
;

DYNAMIC
:
   'dynamic'
;

DYNAMIC_ACCESS_POLICY_RECORD
:
   'dynamic-access-policy-record'
;

DYNAMIC_CAPABILITY
:
   'dynamic-capability'
;

DYNAMIC_EXTENDED
:
   'dynamic-extended'
;

DYNAMIC_MAP
:
   'dynamic-map'
;

DYNAMIC_MCAST_OPTIMIZATION
:
   'dynamic-mcast-optimization'
;

DYNAMIC_MCAST_OPTIMIZATION_THRESH
:
   'dynamic-mcast-optimization-thresh'
;

E164
:
    'e164'
;

E164_PATTERN_MAP
:
    'e164-pattern-map'
;

EAP_PASSTHROUGH
:
   'eap-passthrough'
;

EAPOL_RATE_OPT
:
   'eapol-rate-opt'
;

EARLY_OFFER
:
   'early-offer'
;

EBGP
:
   'ebgp'
;

EBGP_MULTIHOP
:
   'ebgp-multihop'
;

ECE
:
   'ece'
;

ECHO
:
   'echo'
;

ECHO_CANCEL
:
   'echo-cancel'
;

ECHO_REPLY
:
   'echo-reply'
;

ECHO_REQUEST
:
   'echo-request'
;

ECMP
:
   'ecmp'
;

ECMP_GROUP
:
   'ecmp-group'
;

ECN
:
   'ecn'
;

EDCA_PARAMETERS_PROFILE
:
   'edca-parameters-profile'
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

EGRESS
:
   'egress'
;

EGRESS_INTERFACE_SELECTION
:
   'egress-interface-selection'
;

EIBGP
:
   'eibgp'
;

EIGRP
:
   'eigrp'
;

ELSE
:
   'else'
;

ELSEIF
:
   'elseif'
;

EMAIL
:
   'email'
;

EMAIL_ADDR
:
   'email-addr' -> pushMode ( M_Description )
;

EMAIL_CONTACT
:
   'email-contact' -> pushMode ( M_Description )
;

EMERGENCIES
:
   'emergencies'
;

EMPTY
:
   'empty'
;

ENABLE
:
   'enable'
;

ENABLE_ACL_CAM_SHARING
:
   'enable-acl-cam-sharing'
;

ENABLE_ACL_COUNTER
:
   'enable-acl-counter'
;

ENABLE_AUTHENTICATION
:
   'enable-authentication'
;

ENABLE_QOS_STATISTICS
:
   'enable-qos-statistics'
;

ENABLE_WELCOME_PAGE
:
   'enable-welcome-page'
;

ENABLED
:
   'enabled'
;

ENCAPSULATION
:
   'encapsulation'
;

ENCR
:
   'encr'
;

ENCRYPTED
:
   'encrypted'
;

ENCRYPTED_PASSWORD
:
   'encrypted-password'
;

ENCRYPTION
:
   'encryption'
;

END
:
   'end'
;

ENDIF
:
   'endif'
;

END_CLASS_MAP
:
   'end-class-map'
;

END_POLICY
:
   'end-policy'
;

END_POLICY_MAP
:
   'end-policy-map'
;

END_SET
:
   'end-set'
   { inCommunitySet = false; }

;

ENET_LINK_PROFILE
:
   'enet-link-profile'
;

ENFORCE_DHCP
:
   'enforce-dhcp'
;

ENFORCE_FIRST_AS
:
   'enforce-first-as'
;

ENFORCE_RULE
:
   'enforce-rule'
;

ENFORCED
:
   'enforced'
;

ENGINE
:
   'engine'
;

ENGINEID
:
   (
      'engineid'
      | 'engineID'
   ) -> pushMode ( M_COMMENT )
;

ENROLLMENT
:
   'enrollment'
;

ENVIRONMENT
:
   'environment'
;

ENVIRONMENT_MONITOR
:
   'environment-monitor'
;

EOF_LITERAL
:
   'EOF'
;

EOU
:
   'eou'
;

EQ
:
   'eq'
;

ERRDISABLE
:
   'errdisable'
;

ERROR
:
   'error'
;

ERROR_ENABLE
:
   'error-enable'
;

ERROR_RATE_THRESHOLD
:
   'error-rate-threshold'
;

ERROR_RECOVERY
:
   'error-recovery'
;

ERROR_PASSTHRU
:
   'error-passthru'
;

ERRORS
:
   'errors'
;

ERSPAN_ID
:
   'erspan-id'
;

ESCAPE_CHARACTER
:
   'escape-character'
;

ESM
:
   'esm'
;

ESP
:
   'esp'
;

ESP_3DES
:
   'esp-3des'
;

ESP_AES
:
   'esp-aes'
;

ESP_DES
:
   'esp-des'
;

ESP_GCM
:
   'esp-gcm'
;

ESP_GMAC
:
   'esp-gmac'
;

ESP_MD5_HMAC
:
   'esp-md5-hmac'
;

ESP_NULL
:
   'esp-null'
;

ESP_SEAL
:
   'esp-seal'
;

ESP_SHA_HMAC
:
   'esp-sha-hmac'
;

ESP_SHA256_HMAC
:
   'esp-sha256-hmac'
;

ESP_SHA512_HMAC
:
   'esp-sha512-hmac'
;

ESSID
:
   'essid'
;

ESTABLISHED
:
   'established'
;

ETH
:
   'eth'
;

ETHERCHANNEL
:
   'etherchannel'
;

ETHERNET
:
   'ethernet'
;

ETHERNET_SERVICES
:
   'ethernet-services'
;

ETYPE
:
   'etype'
;

EVALUATE
:
   'evaluate'
;

EVENT
:
   'event'
;

EVENT_HANDLER
:
   'event-handler'
;

EVENT_HISTORY
:
   'event-history'
;

EVENT_THRESHOLDS_PROFILE
:
   'event-thresholds-profile'
;

EVENTS
:
   'events'
;

EXACT
:
   'exact'
;

EXCEED_ACTION
:
   'exceed-action'
;

EXCEPT
:
   'except'
;

EXCEPTION
:
   'exception'
;

EXCEPTION_SLAVE
:
   'exception-slave'
;

EXCLUDE
:
   'exclude'
;

EXCLUDED_ADDRESS
:
   'excluded-address'
;

EXEC
:
   'exec'
;

EXEC_TIMEOUT
:
   'exec-timeout'
;

EXECUTE
:
   'execute' -> pushMode ( M_Execute )
;

EXEMPT
:
   'exempt'
;

EXIST_MAP
:
   'exist-map'
;

EXIT
:
   'exit'
;

EXIT_ADDRESS_FAMILY
:
   'exit-address-family'
;

EXIT_PEER_POLICY
:
   'exit-peer-policy'
;

EXIT_PEER_SESSION
:
   'exit-peer-session'
;

EXIT_VRF
:
   'exit-vrf'
;

EXPECT
:
   'expect'
;

EXPLICIT_NULL
:
   'explicit-null'
;

EXPORT
:
   'export'
;

EXPORT_PROTOCOL
:
   'export-protocol'
;

EXPORTER
:
   'exporter'
;

EXPORTER_MAP
:
   'exporter-map'
;

EXPANDED
:
   'expanded'
;

EXTCOMM_LIST
:
   'extcomm-list'
;

EXTCOMMUNITY
:
   'extcommunity'
   {
     if (lastTokenType == SET) {
       pushMode(M_Extcommunity);
     }
   }
;

EXTCOMMUNITY_LIST
:
   'extcommunity-list'
;

EXTEND
:
   'extend'
;

EXTENDED
:
   'extended'
   { enableDEC = true; enableACL_NUM = false; }

;

EXTENDED_COUNTERS
:
   'extended-counters'
;

EXTENDED_DELAY
:
   'extended-delay'
;

EXTERNAL
:
   'external'
;

EXTERNAL_LSA
:
   'external-lsa'
;

FABRIC
:
   'fabric'
;

FABRIC_MODE
:
   'fabric-mode'
;

FABRICPATH
:
   'fabricpath'
;

FACILITY
:
   'facility'
;

FACILITY_ALARM
:
   'facility-alarm'
;

FAIL_MESSAGE
:
   'fail-message'
;

FAILED
:
   'failed'
;

FAILED_LIST
:
   'failed-list'
;

FAILOVER
:
   'failover'
;

FAILURE
:
   'failure'
;

FAIL_OVER
:
   'fail-over'
;

FAIR_QUEUE
:
   'fair-queue'
;

FALL_OVER
:
   'fall-over'
;

FALLBACK
:
   'fallback'
;

FALLBACK_DN
:
   'fallback-dn'
;

FAN
:
   'fan'
;

FAST_AGE
:
   'fast-age'
;

FAST_DETECT
:
   'fast-detect'
;

FAST_EXTERNAL_FALLOVER
:
   'fast-external-fallover'
;

FAST_FLOOD
:
   'fast-flood'
;

FAST_REROUTE
:
   'fast-reroute'
;

FAX
:
   'fax'
;

FAX_RELAY
:
   'fax-relay'
;

FCOE
:
   'fcoe'
;

FDL
:
	'fdl'
;

FEATURE
:
   'feature'
;

FEATURE_SET
:
   'feature-set'
;

FEX
:
   'fex'
;

FEX_FABRIC
:
   'fex-fabric'
;

FIBER_NODE
:
   'fiber-node' -> pushMode ( M_FiberNode )
;

FIELDS
:
   'fields'
;

FILE
:
   'file'
;

FILE_BROWSING
:
   'file-browsing'
;

FILE_ENTRY
:
   'file-entry'
;

FILE_SIZE
:
   'file-size'
;

FILE_TRANSFER
:
   'file-transfer'
;

FILTER
:
   'filter'
;

FILTER_LIST
:
   'filter-list'
;

FIREWALL
:
   'firewall'
   { enableIPV6_ADDRESS = false; }

;

FIREWALL_VISIBILITY
:
   'firewall-visibility'
;

FIN
:
   'fin'
;

FINGER
:
   'finger'
;

FIRMWARE
:
   'firmware'
;

FLAP_LIST
:
   'flap-list'
;

FLASH
:
   'flash'
;

FLASH_OVERRIDE
:
   'flash-override'
;

FLOATING_CONN
:
   'floating-conn'
;

FLOW
:
   'flow'
;

FLOW_AGGREGATION
:
   'flow-aggregation'
;

FLOW_CAPTURE
:
   'flow-capture'
;

FLOW_CACHE
:
   'flow-cache'
;

FLOW_CONTROL
:
   'flow-control'
;

FLOW_EXPORT
:
   'flow-export'
;

FLOW_SAMPLING_MODE
:
   'flow-sampling-mode'
;

FLOW_SAMPLER
:
   'flow-sampler'
;

FLOW_SAMPLER_MAP
:
   'flow-sampler-map'
;

FLOW_TOP_TALKERS
:
   'flow-top-talkers'
;

FLOWCONTROL
:
   'flowcontrol'
;

FLUSH_AT_ACTIVATION
:
   'flush-at-activation'
;

FLUSH_R1_ON_NEW_R0
:
   'flush-r1-on-new-r0'
;

FLUSH_ROUTES
:
   'flush-routes'
;

FORCE
:
   'force'
;

FOR
:
   'for'
;

FORMAT
:
   'format'
;

FORWARD_DIGITS
:
   'forward-digits'
;

FORWARD_PROTOCOL
:
   'forward-protocol'
;

FORWARDER
:
   'forwarder'
;

FORWARDING
:
   'forwarding'
;

FPD
:
   'fpd'
;

FQDN
:
   'fqdn'
;

FRAGMENTS
:
   'fragments'
;

FRAME_RELAY
:
   'frame-relay'
;

FRAMING
:
   'framing'
;

FREE_CHANNEL_INDEX
:
   'free-channel-index'
;

FREQUENCY
:
   'frequency'
;

FRI
:
   'Fri'
;

FROM
:
   'from'
;

FT
:
   'ft'
;

FTP
:
   'ftp'
;

FTP_DATA
:
   'ftp-data'
;

FTP_SERVER
:
   'ftp-server'
;

FULL_DUPLEX
:
   'full-duplex'
;

FULL_TXT
:
   'full-txt'
;

G709
:
   'g709'
;

G729
:
   'g729'
;

GATEKEEPER
:
   'gatekeeper'
;

GATEWAY
:
   'gateway'
;

GBPS
:
    'Gbps'
;

GE
:
   'ge'
;

GENERAL_GROUP_DEFAULTS
:
   'general-group-defaults'
;

GENERAL_PROFILE
:
   'general-profile'
;

GENERATE
:
   'generate'
;

GID
:
   'gid'
;

GIG_DEFAULT
:
   'gig-default'
;

GLBP
:
   'glbp'
;

GLOBAL
:
   'global'
;

GLOBALENFORCEPRIV
:
   'globalEnforcePriv'
;

GLOBAL_MTU
:
   'global-mtu'
;

GLOBAL_PORT_SECURITY
:
   'global-port-security'
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

GRACETIME
:
   'gracetime'
;

GRANT
:
   'grant'
;

GRATUITOUS_ARPS
:
   'gratuitous-arps'
;

GRE
:
   'gre'
;

GREEN
:
   'green'
;

GROUP
:
   'group'
;

GROUP_ALIAS
:
   'group-alias'
;

GROUP_LIST
:
   'group-list'
;

GROUP_LOCK
:
   'group-lock'
;

GROUP_OBJECT
:
   'group-object'
;

GROUP_POLICY
:
   'group-policy'
;

GROUP_RANGE
:
   'group-range'
;

GROUP_TIMEOUT
:
   'group-timeout'
;

GROUP_URL
:
   'group-url'
;

GROUP1
:
   'group1'
;

GROUP2
:
   'group2'
;

GSHUT
:
   [Gg][Ss][Hh][Uu][Tt]
;

GT
:
   'gt'
;

GUARANTEED
:
   'guaranteed'
;

GUARD
:
   'guard'
;

GUEST_ACCESS_EMAIL
:
   'guest-access-email'
;

GUEST_LOGON
:
   'guest-logon'
;

GUEST_MODE
:
   'guest-mode'
;

GW_TYPE_PREFIX
:
   'gw-type-prefix'
;

H225
:
   'h225'
;

H323
:
   'h323'
;

H323_GATEWAY
:
   'h323-gateway'
;

HA_POLICY
:
   'ha-policy'
;

HALF_CLOSED
:
   'half-closed'
;

HALF_DUPLEX
:
   'half-duplex'
;

HANDOVER_TRIGGER_PROFILE
:
   'handover-trigger-profile'
;

HARDWARE
:
   'hardware'
;

HARDWARE_ADDRESS
:
   'hardware-address'
;

HARDWARE_COUNT
:
   'hardware-count'
;

HASH
:
   'hash'
;

HASH_ALGORITHM
:
   'hash-algorithm'
;

HEADER_COMPRESSION
:
   'header-compression'
;

HEADER_PASSING
:
   'header-passing'
;

HEARTBEAT_INTERVAL
:
   'heartbeat-interval'
;

HEARTBEAT_TIME
:
   'heartbeat-time'
;

HELLO
:
   'hello'
;

HELLO_INTERVAL
:
   'hello-interval'
;

HELLO_MULTIPLIER
:
   'hello-multiplier'
;

HELLO_PADDING
:
   'hello-padding'
;

HELLO_PASSWORD
:
   'hello-password'
;

HELPER_ADDRESS
:
   'helper-address'
;

HIDDEN_LITERAL
:
   'hidden'
;

HIDDEN_SHARES
:
   'hidden-shares'
;

HIDEKEYS
:
   'hidekeys'
;

HIGH
:
   'high'
;

HIGH_AVAILABILITY
:
   'high-availability'
;

HIGH_RESOLUTION
:
   'high-resolution'
;

HISTORY
:
   'history'
;

HOLD_QUEUE
:
   'hold-queue'
;

HOMEDIR
:
   'homedir'
;

HOP_LIMIT
:
   'hop-limit'
;

HOPLIMIT
:
   'hoplimit'
;

HOPS_OF_STATISTICS_KEPT
:
   'hops-of-statistics-kept'
;

HOST
:
   'host'
;

HOST_ASSOCIATION
:
   'host-association'
;

HOST_INFO
:
   'host-info'
;

HOST_PROXY
:
   'host-proxy'
;

HOST_ROUTING
:
   'host-routing'
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
;

HOTSPOT
:
   'hotspot'
;

HPM
:
   'hpm'
;

HSRP
:
   'hsrp'
;

HT_SSID_PROFILE
:
   'ht-ssid-profile'
;

HTTP
:
   'http'
;

HTTP_COMMANDS
:
   'http-commands'
;

HTTPS
:
   'https'
;

HUNT
:
   'hunt'
;

HW_MODULE
:
   'hw-module'
;

HW_SWITCH
:
   'hw-switch'
;

IBGP
:
   'ibgp'
;

ICMP
:
   'icmp'
;

ICMP_ECHO
:
   'icmp-echo'
;

ICMP_ERROR
:
   'icmp-error'
;

ICMP_ERRORS
:
   'icmp-errors'
;

ICMP_OBJECT
:
   'icmp-object'
;

ICMP_TYPE
:
   'icmp-type'
;

ICMP6
:
   'icmp6'
;

ICMPV6
:
   'icmpv6'
;

ID
:
   'id'
;

ID_MISMATCH
:
   'id-mismatch'
;

ID_RANDOMIZATION
:
   'id-randomization'
;

IDEAL_COVERAGE_INDEX
:
   'ideal-coverage-index'
;

IDENT
:
   'ident'
;

IDENTIFIER
:
   'identifier'
;

IDENTITY
:
   'identity'
;

IDLE
:
   'idle'
;

IDLE_TIMEOUT
:
   'idle-timeout'
;

IDP_CERT
:
   'idp-cert'
;

IDS
:
   'ids'
;

IDS_PROFILE
:
   'ids-profile'
;

IEC
:
   'iec'
;

IETF_FORMAT
:
   'ietf-format'
;

IF
:
   'if'
;

IFACL
:
   'ifacl'
;

IFDESCR
:
   'ifdescr'
;

IF_NEEDED
:
   'if-needed'
;

IFINDEX
:
   'ifindex'
;

IFMAP
:
   'ifmap'
;

IFMIB
:
   'ifmib'
;

IGMP
:
   'igmp'
;

IGP_COST
:
   'igp-cost'
;

IGRP
:
   'igrp'
;

IGNORE
:
   'ignore'
;

IGNORE_ATTACHED_BIT
:
   'ignore-attached-bit'
;

IGP
:
   'igp'
;

IKE
:
   'ike'
;

IKEV1
:
   'ikev1'
;

IKEV2
:
   'ikev2'
;

IKEV2_PROFILE
:
   'ikev2-profile'
;

ILMI_KEEPALIVE
:
   'ilmi-keepalive'
;

IMAP4
:
   'imap4'
;

IMMEDIATE
:
   'immediate'
;

IMPERSONATION_PROFILE
:
   'impersonation-profile'
;

IMPLICIT_USER
:
   'implicit-user'
;

IMPORT
:
   'import'
;

IN
:
   'in'
;

INACTIVITY_TIMER
:
   'inactivity-timer'
;

INBAND
:
   'inband'
;

INBOUND
:
   'inbound'
;

INCLUDE
:
   'include'
;

INCLUDE_STUB
:
   'include-stub'
;

INCOMING
:
   'incoming'
;

INCOMPLETE
:
   'incomplete'
;

INDEX
:
   'index'
;

INFINITY
:
   'infinity'
;

INFORM
:
   'inform'
;

INFORMATION
:
   'information'
;

INFORMATION_REPLY
:
   'information-reply'
;

INFORMATION_REQUEST
:
   'information-request'
;

INFORMATIONAL
:
   'informational'
;

INFORMS
:
   'informs'
;

INGRESS
:
   'ingress'
;

INHERIT
:
   'inherit'
;

INHERITANCE_DISABLE
:
   'inheritance-disable'
;

INIT
:
   'init'
;

INIT_STRING
:
   'init-string'
;

INIT_TECH_LIST
:
   'init-tech-list'
;

INITIAL_ROLE
:
   'initial-role'
;

INJECT_MAP
:
   'inject-map'
;

INPUT
:
   'input'
;

INSERVICE
:
   'inservice'
;

INSIDE
:
   'inside'
;

INSPECT
:
   'inspect'
;

INSTALL
:
   'install'
;

INSTANCE
:
   'instance'
;

INTEGRITY
:
   'integrity'
;

INTERAREA
:
   'interarea'
;

INTERCEPT
:
   'intercept'
;

INTERFACE
:
   'int' 'erface'?
   { enableIPV6_ADDRESS = false; if (!_inAccessList) {pushMode(M_Interface);}}

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

INTERWORKING
:
   'interworking'
;

INVALID_SPI_RECOVERY
:
   'invalid-spi-recovery'
;

INVALID_USERNAME_LOG
:
   'invalid-username-log'
;

INVERT
:
   'invert'
;

IOS_REGEX
:
   'ios-regex' -> pushMode ( M_IosRegex )
;

IP
:
   'ip'
;

IP_ADDRESS_LITERAL
:
   'ip-address'
;

IP_FLOW_EXPORT_PROFILE
:
   'ip-flow-export-profile'
;

IPC
:
   'ipc'
;

IPENACL
:
   'ipenacl'
;

IPHC_FORMAT
:
   'iphc-format'
;

IPINIP
:
   'ipinip'
;

IPSEC
:
   'ipsec'
;

IPSEC_ISAKMP
:
   'ipsec-isakmp'
;

IPSEC_OVER_TCP
:
   'ipsec-over-tcp'
;

IPSEC_PROPOSAL
:
   'ipsec-proposal'
;

IPSEC_UDP
:
   'ipsec-udp'
;

IPSLA
:
   'ipsla'
;

IPV4
:
   [iI] [pP] [vV] '4'
;

IPV4_L5
:
   'ipv4-l5'
;

IPV6
:
   [iI] [pP] [vV] '6'
;

IPV6_ADDRESS_POOL
:
   'ipv6-address-pool'
;

IPX
:
   'ipx'
;

IRC
:
   'irc'
;

IRDP
:
   'irdp'
;

ISAKMP
:
   'isakmp'
;

ISDN
:
   'isdn'
;

IS
:
   'is'
;

IS_TYPE
:
   'is-type'
;

ISIS
:
   'isis'
;

ISIS_METRIC
:
   'isis-metric'
;

ISL
:
   'isl'
;

ISOLATE
:
  'isolate'
;

ISPF
:
   'ispf'
;

ISSUER_NAME
:
   'issuer-name'
;

IUC
:
   'iuc'
;

JOIN_GROUP
:
    'join-group'
;

JUMBO
:
   'jumbo'
;

JUMBOMTU
:
   'jumbomtu'
;

KBPS
:
   'kbps'
;

KEEPALIVE
:
   'keepalive'
;

KEEPALIVE_ENABLE
:
   'keepalive-enable'
;

KEEPOUT
:
   'keepout'
;

KERBEROS
:
   'kerberos'
;

KERNEL
:
   'kernel'
;

KEY
:
   'key'
;

KEY_EXCHANGE
:
   'key-exchange'
;

KEY_HASH
:
   'key-hash'
;

KEY_SOURCE
:
   'key-source'
;

KEY_STRING
:
   'key-string'
;

KEYPAIR
:
   'keypair'
;

KEYPATH
:
   'keypath'
;

KEYRING
:
   'keyring'
;

KEYSTORE
:
   'keystore'
;

KLOGIN
:
   'klogin'
;

KRON
:
   'kron'
;

KSHELL
:
   'kshell'
;

L2
:
   'l2'
;

L2_FILTER
:
   'l2-filter'
;

L2_SRC
:
   'l2-src'
;

L2PROTOCOL
:
   'l2protocol'
;

L2PROTOCOL_TUNNEL
:
   'l2protocol-tunnel'
;

L2TP
:
   'l2tp'
;

L2TP_CLASS
:
   'l2tp-class'
;

L2TRANSPORT
:
   'l2transport'
;

L2VPN
:
   'l2vpn'
;

LABEL
:
   'label'
;

LABELED_UNICAST
:
   'labeled-unicast'
;

LACP
:
   'lacp'
;

LACP_TIMEOUT
:
   'lacp-timeout'
;

LAG
:
   'lag'
;

LAN
:
   'lan'
;

LANE
:
   'lane'
;

LAPB
:
   'lapb'
;

LARGE
:
   'large'
;

LAST_AS
:
   'last-as'
;

LAST_MEMBER_QUERY_COUNT
:
   'last-member-query-count'
;

LAST_MEMBER_QUERY_INTERVAL
:
   'last-member-query-interval'
;

LAST_MEMBER_QUERY_RESPONSE_TIME
:
   'last-member-query-response-time'
;

LCD_MENU
:
   'lcd-menu'
;

LDAP
:
   'ldap'
;

LDAPS
:
   'ldaps'
;

LDP
:
   'ldp'
;

LE
:
   'le'
;

LEASE
:
   'lease'
;

LEVEL
:
   'level'
;

LEVEL_1
:
   'level-1'
;

LEVEL_1_2
:
   'level-1-2'
;

LEVEL_2
:
   'level-2'
;

LEVEL_2_ONLY
:
   'level-2-only'
;

LDAP_BASE_DN
:
   'ldap-base-dn'
;

LDAP_LOGIN
:
   'ldap-login'
;

LDAP_LOGIN_DN
:
   'ldap-login-dn'
;

LDAP_NAMING_ATTRIBUTE
:
   'ldap-naming-attribute'
;

LDAP_SCOPE
:
   'ldap-scope'
;

LENGTH
:
   'length'
;

LICENSE
:
   'license'
;

LIFE
:
   'life'
;

LIFETIME
:
   'lifetime'
;

LIMIT
:
   'limit'
;

LIMIT_DN
:
   'limit-dn'
;

LIMIT_RESOURCE
:
   'limit-resource'
;

LINE
:
   'line'
;

LINECARD
:
   'linecard'
;

LINECARD_GROUP
:
   'linecard-group'
;

LINECODE
:
   'linecode'
;

LINK
:
   'link'
;

LINK_FAIL
:
   'link-fail'
;

LINK_FAULT_SIGNALING
:
   'link-fault-signaling'
;

LINK_TYPE
:
   'link-type'
;

LINKDEBOUNCE
:
   'linkdebounce'
;

LISP
:
   'lisp'
;

LIST
:
   'list'
;

LISTEN
:
   'listen'
;

LISTEN_PORT
:
   'listen-port'
;

LISTENER
:
   'listener'
;

LLDP
:
   'lldp'
;

LMS_IP
:
   'lms-ip'
;

LMS_PREEMPTION
:
   'lms-preemption'
;

LOAD_BALANCE
:
   'load-balance'
;

LOAD_BALANCING
:
   'load-balancing'
;

LOAD_INTERVAL
:
   'load-interval'
;

LOAD_SHARING
:
   'load-sharing'
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
   [Ll][Oo][Cc][Aa][Ll]'-'[Aa][Ss]
;

LOCAL_ASA
:
   'LOCAL'
;

LOCAL_INTERFACE
:
   'local-interface'
;

LOCAL_IP
:
   'local-ip'
;

LOCAL_PORT
:
   'local-port'
;

LOCAL_PREFERENCE
:
   'local-preference'
;

LOCAL_V6_ADDR
:
   'local-v6-addr'
;

LOCAL_VOLATILE
:
   'local-volatile'
;

LOCATION
:
   'location' -> pushMode ( M_COMMENT )
;

LOCALE
:
   'locale'
;

LOCALIP
:
   'localip'
;

LOG
:
   'log'
;

LOG_ADJ_CHANGES
:
   'log-adj-changes'
;

LOG_ADJACENCY_CHANGES
:
   'log-adjacency-changes'
;

LOG_ENABLE
:
   'log-enable'
;

LOG_INPUT
:
   'log-input'
;

LOG_INTERNAL_SYNC
:
   'log-internal-sync'
;

LOG_NEIGHBOR_CHANGES
:
   'log-neighbor-changes'
;

LOGFILE
:
   'logfile'
;

LOGGING
:
   'logging'
;

LOGIN
:
   'login'
;

LOGIN_ATTEMPTS
:
   'login-attempts'
;

LOGIN_AUTHENTICATION
:
   'login-authentication'
;

LOGIN_PAGE
:
   'login-page'
;

LOGINSESSION
:
   'loginsession'
;

LOGOUT_WARNING
:
   'logout-warning'
;

LOOKUP
:
   'lookup'
;

LOOPBACK
:
   'loopback'
;

LOOPGUARD
:
   'loopguard'
;

LOTUSNOTES
:
   'lotusnotes'
;

LOW_MEMORY
:
   'low-memory'
;

LPD
:
   'lpd'
;

LPTS
:
   'lpts'
;

LRE
:
   'lre'
;

LRQ
:
   'lrq'
;

LSP_GEN_INTERVAL
:
   'lsp-gen-interval'
;

LSP_INTERVAL
:
   'lsp-interval'
;

LSP_PASSWORD
:
   'lsp-password'
;

LSP_REFRESH_INTERVAL
:
   'lsp-refresh-interval'
;

LT
:
   'lt'
;

MAC
:
   'mac'
;

MAC_ADDRESS
:
   'mac-address' -> pushMode ( M_COMMENT )
;

MAC_ADDRESS_TABLE
:
   'mac-address-table'
;

MAC_DEFAULT_ROLE
:
   'mac-default-role'
;

MAC_LEARN
:
   'mac-learn'
;

MAC_MOVE
:
   'mac-move'
;

MAC_SERVER_GROUP
:
   'mac-server-group'
;

MACHINE_AUTHENTICATION
:
   'machine-authentication'
;

MACRO
:
   'macro'
;

MAIL_SERVER
:
   'mail-server'
;

MAIN_CPU
:
   'main-cpu'
;

MAINTENANCE
:
  'maintenance'
;

MANAGEMENT
:
   'management'
;

MANAGEMENT_ACCESS
:
   'management-access'
;

MANAGEMENT_ONLY
:
   'management-only'
;

MANAGEMENT_PLANE
:
   'management-plane'
;

MANAGEMENT_PROFILE
:
   'management-profile'
;

MANAGER
:
   'manager'
;

MAP
:
   'map'
;

MAP_CLASS
:
   'map-class'
;

MAP_GROUP
:
   'map-group'
;

MAP_LIST
:
   'map-list'
;

MAPPING
:
   'mapping'
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

MASTER
:
   'master'
;

MASTERIP
:
   'masterip'
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

MATCHES_ANY
:
   'matches-any'
;

MATCHES_EVERY
:
   'matches-every'
;

MAXAS_LIMIT
:
   'maxas-limit'
;

MAX_ASSOCIATIONS
:
   'max-associations'
;

MAX_AUTHENTICATION_FAILURES
:
   'max-authentication-failures'
;

MAX_BURST
:
   'max-burst'
;

MAX_CLIENTS
:
   'max-clients'
;

MAX_CONCAT_BURST
:
   'max-concat-burst'
;

MAX_CONFERENCES
:
   'max-conferences'
;

MAX_CONNECTIONS
:
   'max-connections'
;

MAX_DN
:
   'max-dn'
;

MAX_EPHONES
:
   'max-ephones'
;

MAX_IFINDEX_PER_MODULE
:
   'max-ifindex-per-module'
;

MAX_LSA
:
   'max-lsa'
;

MAX_LSP_LIFETIME
:
   'max-lsp-lifetime'
;

MAX_METRIC
:
   'max-metric'
;

MAX_RATE
:
   'max-rate'
;

MAX_ROUTE
:
   'max-route'
;

MAX_SESSIONS
:
   'max-sessions'
;

MAX_TX_POWER
:
   'max-tx-power'
;

MAXIMUM
:
   'maximum'
;

MAXIMUM_ACCEPTED_ROUTES
:
   'maximum-accepted-routes'
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

MAXIMUM_ROUTES
:
   'maximum-routes'
;

MAXPOLL
:
   'maxpoll'
;

MAXSTARTUPS
:
   'maxstartups'
;

MBPS
:
    'Mbps'
;

MBSSID
:
   'mbssid'
;

MCAST_BOUNDARY
:
   'mcast-boundary'
;

MCAST_RATE_OPT
:
   'mcast-rate-opt'
;

MD5
:
   'md5'
;

MDIX
:
   'mdix'
;

MDT
:
   'mdt'
;

MED
:
   'med'
;

MEDIUM
:
   'medium'
;

MEDIA
:
   'media'
;

MEDIA_TERMINATION
:
   'media-termination'
;

MEDIA_TYPE
:
   'media-type'
;

MEMBER
:
   'member'
;

MEMORY
:
   'memory'
;

MEMORY_SIZE
:
   'memory-size'
;

MENU
:
   'menu'
;

MESH_CLUSTER_PROFILE
:
   'mesh-cluster-profile'
;

MESH_GROUP
:
   'mesh-group'
;

MESH_HT_SSID_PROFILE
:
   'mesh-ht-ssid-profile'
;

MESH_RADIO_PROFILE
:
   'mesh-radio-profile'
;

MESSAGE_COUNTER
:
   'message-counter'
;

MESSAGE_DIGEST
:
   'message-digest'
;

MESSAGE_DIGEST_KEY
:
   'message-digest-key'
;

MESSAGE_LENGTH
:
   'message-length'
;

MESSAGE_LEVEL
:
   'message-level'
;

MESSAGE_SIZE
:
   'message-size'
;

METERING
:
   'metering'
;

METHOD
:
   'method'
;

METHOD_UTILIZATION
:
   'method-utilization'
;

METRIC
:
   'metric'
;

METRIC_STYLE
:
   'metric-style'
;

METRIC_TYPE
:
   'metric-type'
;

MFIB
:
   'mfib'
;

MFIB_MODE
:
   'mfib-mode'
;

MFWD
:
   'mfwd'
;

MGCP
:
   'mgcp'
;

MGCP_PAT
:
   'mgcp-pat'
;

MGMT
:
   'mgmt'
;

MGMT_AUTH
:
   'mgmt-auth'
;

MGMT_SERVER
:
   'mgmt-server'
;

MGMT_USER
:
   'mgmt-user'
;

MIB
:
   'mib'
;

MICROCODE
:
   'microcode'
;

MICROSOFT_DS
:
   'microsoft-ds'
;

MIDCALL_SIGNALING
:
   'midcall-signaling'
;

MIN_PACKET_SIZE
:
   'min-packet-size'
;

MIN_RATE
:
   'min-rate'
;

MIN_TX_POWER
:
   'min-tx-power'
;

MINIMAL
:
   'minimal'
;

MINIMUM
:
   'minimum'
;

MINIMUM_INTERVAL
:
   'minimum-interval'
;

MINIMUM_LINKS
:
   'minimum-links'
;

MINPOLL
:
   'minpoll'
;

MIRROR
:
   'mirror'
;

MISSING_AS_WORST
:
   'missing-as-worst'
;

MLAG
:
   'mlag'
;

MLD
:
   'mld'
;

MLD_QUERY
:
   'mld-query'
;

MLD_REDUCTION
:
   'mld-reduction'
;

MLD_REPORT
:
   'mld-report'
;

MLDV2
:
   'mldv2'
;

MLS
:
   'mls'
;

MOBILE
:
   'mobile'
;

MOBILE_IP
:
   'mobile-ip'
;

MOBILE_REDIRECT
:
   'mobile-redirect'
;

MOBILITY
:
   'mobility'
;

MODE
:
   'mode'
;

MODEM
:
   'modem'
;

MODULATION_PROFILE
:
   'modulation-profile'
;

MODULE
:
   'module'
;

MODULE_TYPE
:
   'module-type'
;

MON
:
   'Mon'
;

MONITOR
:
   'monitor'
;

MONITOR_INTERFACE
:
   'monitor-interface'
;

MONITOR_MAP
:
   'monitor-map'
;

MONITOR_SESSION
:
   'monitor-session'
;

MONITORING
:
   'monitoring'
;

MONITORING_BASICS
:
   'monitoring-basics'
;

MOP
:
   'mop'
;

MOTD
:
   'motd'
;

MPLS
:
   'mpls'
;

MPLS_LABEL
:
   'mpls-label'
;

MROUTE
:
   'mroute'
;

MROUTE_CACHE
:
   'mroute-cache'
;

MSDP
:
   'msdp'
;

MSDP_PEER
:
   'msdp-peer'
;

MSCHAP
:
   'mschap'
;

MSCHAPV2
:
   'mschapv2'
;

MSIE_PROXY
:
   'msie-proxy'
;

MSRPC
:
   'msrpc'
;

MST
:
   'mst'
;

MTA
:
   'mta'
;

MTU
:
   'mtu'
;

MTU_IGNORE
:
   'mtu-ignore'
;

MULTICAST
:
   'multicast'
;

MULTICAST_BOUNDARY
:
   'multicast-boundary'
;

MULTICAST_ROUTING
:
   'multicast-routing'
;

MULTICAST_STATIC_ONLY
:
   'multicast-static-only'
;

MULTILINK
:
   'multilink'
;

MULTIPATH
:
   'multipath'
;

MULTIPATH_RELAX
:
   'multipath-relax'
;

MULTIPLIER
:
   'multiplier'
;

MULTIPOINT
:
   'multipoint'
;

MULTI_CONFIG
:
   'multi-config'
;

MULTI_TOPOLOGY
:
   'multi-topology'
;

MVPN
:
   'mvpn'
;

MVR
:
   'mvr'
;

NAME
:
   'name' -> pushMode ( M_Name )
;

NAME_LOOKUP
:
   'name-lookup'
;

NAME_SERVER
:
   'name-server'
;

NAMED_KEY
:
   'named-key'
;

NAMEIF
:
   'nameif'
;

NAMESPACE
:
   'namespace'
;

NAMES
:
   'names'
;

NAMESERVER
:
   'nameserver'
;

NAT
:
   [Nn][Aa][Tt]
;

NAT_CONTROL
:
   'nat-control'
;

NAT_TRANSPARENCY
:
   'nat-transparency'
;

NAT_TRAVERSAL
:
   'nat-traversal'
;

NATIVE
:
   'native'
;

NATPOOL
:
   'natpool'
;

NBAR
:
   'nbar'
;

ND
:
   'nd'
;

ND_NA
:
   'nd-na'
;

ND_NS
:
   'nd-ns'
;

ND_TYPE
:
   'nd-type'
;

NEGOTIATE
:
   'negotiate'
;

NEGOTIATED
:
   'negotiated'
;

NEGOTIATION
:
   'negotiation'
;

NEIGHBOR
:
   'neighbor' -> pushMode ( M_NEIGHBOR )
;

NEIGHBOR_DOWN
:
   'neighbor-down'
;

NEIGHBOR_FILTER
:
   'neighbor-filter'
;

NEIGHBOR_GROUP
:
   'neighbor-group'
;

NEIGHBOR_IS
:
   'neighbor-is'
;

NEQ
:
   'neq'
;

NESTED
:
   'nested'
;

NET
:
   'net' -> pushMode ( M_ISO_Address )
;

NET_UNREACHABLE
:
   'net-unreachable'
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

NETBIOS_SSN
:
   'netbios-ssn'
;

NETCONF
:
   'netconf'
;

NETDESTINATION
:
   'netdestination'
;

NETDESTINATION6
:
   'netdestination6'
;

NETEXTHDR
:
   'netexthdr'
;

NETMASK
:
   'netmask'
;

NETSERVICE
:
   'netservice'
;

NETWORK
:
   'network'
;

NETWORK_CLOCK
:
   'network-clock'
;

NETWORK_CLOCK_PARTICIPATE
:
   'network-clock-participate'
;

NETWORK_CLOCK_SELECT
:
   'network-clock-select'
;

NETWORK_DELAY
:
   'network-delay'
;

NETWORK_OBJECT
:
   'network-object'
;

NETWORK_QOS
:
   'network-qos'
;

NETWORK_UNKNOWN
:
   'network-unknown'
;

NEW_MODEL
:
   'new-model'
;

NEWINFO
:
   'newinfo'
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

NEXT_SERVER
:
   'next-server'
;

NEXTHOP
:
   'nexthop'
;

NEXTHOP1
:
   'nexthop1'
;

NEXTHOP2
:
   'nexthop2'
;

NEXTHOP_ATTRIBUTE
:
   'nexthop-attribute'
;

NEXTHOP_LIST
:
   'nexthop-list'
;

NHOP_ONLY
:
   'nhop-only'
;

NHRP
:
   'nhrp'
;

NLRI
:
   'nlri'
;

NLS
:
   'nls'
;

NMSP
:
   'nmsp'
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

NO_BANNER
:
   'no' F_Whitespace+ 'banner'
;

NO_EXPORT
:
   'no-export'
;

NO_L4R_SHIM
:
   'No l4r_shim'
;

NO_PREPEND
:
   'no-prepend'
;

NO_REDISTRIBUTION
:
   'no-redistribution'
;

NO_SUMMARY
:
   'no-summary'
;

NOAUTH
:
   'noauth'
;

NODE
:
   'node'
;

NOE
:
   'noe'
;

NOHANGUP
:
   'nohangup'
;

NON500_ISAKMP
:
   'non500-isakmp'
;

NON_BROADCAST
:
   'non-broadcast'
;

NON_CLIENT_NRT
:
   'non-client-nrt'
;

NON_CRITICAL
:
   'non-critical'
;

NON_DETERMINISTIC
:
   'non-deterministic'
;

NON_DETERMINISTIC_MED
:
   'non-deterministic-med'
;

NON_EXIST_MAP
:
   'non-exist-map'
;

NONE
:
   'none'
;

NONEGOTIATE
:
   'nonegotiate'
;

NOS
:
   'nos'
;

NOT
:
   'not'
;

NOT_ADVERTISE
:
   'not-advertise'
;

NOTIFICATION
:
   'notification'
;

NOTIFICATION_TIMER
:
   'notification-timer'
;

NOTIFICATIONS
:
   'notifications'
;

NOTIFY
:
   'notify'
;

NOTIFY_FILTER
:
   'notify-filter'
;

NSF
:
   'nsf'
;

NSR
:
   'nsr'
;

NSSA
:
   'nssa'
;

NSSA_EXTERNAL
:
   'nssa-external'
;

NTP
:
   'ntp'
;

NULL
:
   'null'
;

NV
:
   'nv'
;

OAM
:
   'oam'
;

OBJECT
:
   'object'
;

OBJECT_GROUP
:
   'object-group' -> pushMode(M_ObjectGroup)
;

ON
:
   'on'
;

ON_FAILURE
:
   'on-failure'
;

ON_PASSIVE
:
   'on-passive'
;

ON_STARTUP
:
   'on-startup'
;

ON_SUCCESS
:
   'on-success'
;

ONE
:
   'one'
;

ONE_OUT_OF
:
   'one-out-of'
;

ONEP
:
   'onep'
;

OPEN
:
   'open'
;

OPENFLOW
:
   'openflow'
;

OPERATION
:
   'operation'
;

OPMODE
:
   'opmode'
;

OPS
:
   'ops'
;

OPTICAL_MONITOR
:
   'optical-monitor'
;

OPTIMIZATION_PROFILE
:
   'optimization-profile'
;

OPTIMIZE
:
   'optimize'
;

OPTIMIZED
:
   'optimized'
;

OPTION
:
   'option'
;

OPTIONS
:
   'options'
;

OR
:
   'or'
;

ORIGIN
:
   'origin'
;

ORIGIN_ID
:
   'origin-id'
;

ORIGINATE
:
   'originate'
;

ORIGINATES_FROM
:
   'originates-from'
;

ORIGINATOR_ID
:
   'originator-id'
;

OSPF
:
   'ospf'
;

OSPF3
:
   'ospf3'
;

OSPF_EXTERNAL_TYPE_1
:
   'ospf-external-type-1'
;

OSPF_EXTERNAL_TYPE_2
:
   'ospf-external-type-2'
;

OSPF_INTER_AREA
:
   'ospf-inter-area'
;

OSPF_INTRA_AREA
:
   'ospf-intra-area'
;

OSPF_NSSA_TYPE_1
:
   'ospf-nssa-type-1'
;

OSPF_NSSA_TYPE_2
:
   'ospf-nssa-type-2'
;

OSPFV3
:
   'ospfv3'
;

OTHER_ACCESS
:
   'other-access'
;

OUI
:
   'oui' -> pushMode ( M_COMMENT )
;

OUT
:
   'out'
;

OUT_OF_BAND
:
   'out-of-band'
;

OUTBOUND_ACL_CHECK
:
   'outbound-acl-check'
;

OUTPUT
:
   'output'
;

OUTSIDE
:
   'outside'
;

OVERLOAD
:
   'overload'
;

OVERLOAD_CONTROL
:
   'overload-control'
;

OVERRIDE
:
   'override'
;

OWNER
:
   'owner'
;

P2P
:
   'p2p'
;

PACKET
:
   'packet'
;

PACKET_CAPTURE_DEFAULTS
:
   'packet-capture-defaults'
;

PACKET_TOO_BIG
:
   'packet-too-big'
;

PACKETCABLE
:
   'packetcable'
;

PACKETSIZE
:
   'packetsize'
;

PAGER
:
   'pager'
;

PAN
:
   'pan'
;

PAN_OPTIONS
:
   'pan-options'
;

PARAM
:
   'param'
;

PARAMETER_PROBLEM
:
   'parameter-problem'
;

PARAMETERS
:
   'parameters'
;

PARENT
:
   'parent'
;

PARITY
:
   'parity'
;

PARSER
:
   'parser'
;

PARTICIPATE
:
   'participate'
;

PASS
:
   'pass'
;

PASSES_THROUGH
:
   'passes-through'
;

PASSIVE
:
   'passive'
;

PASSIVE_INTERFACE
:
   'passive-interface' -> pushMode ( M_Interface )
;

PASSIVE_ONLY
:
   'passive-only'
;

PASSPHRASE
:
   'passphrase'
;

PASSWORD
:
   'password'
;

PASSWORD_POLICY
:
   'password-policy'
;

PASSWORD_PROMPT
:
   'password-prompt'
;

PASSWORD_STORAGE
:
   'password-storage'
;

PASSWD
:
   'passwd'
;

PAT_XLATE
:
   'pat-xlate'
;

PATH_ECHO
:
   'path-echo'
;

PATH_JITTER
:
   'path-jitter'
;

PATH_OPTION
:
   'path-option'
;

PATH_RETRANSMIT
:
   'path-retransmit'
;

PATHCOST
:
   'pathcost'
;

PATH
:
   'path'
;

PATHS
:
   'paths'
;

PATHS_OF_STATISTICS_KEPT
:
   'paths-of-statistics-kept'
;

PAUSE
:
   'pause'
;

PBR
:
  'pbr'
;

PCANYWHERE_DATA
:
   'pcanywhere-data'
;

PCANYWHERE_STATUS
:
   'pcanywhere-status'
;

PCP
:
   'pcp'
;

PCP_VALUE
:
   'pcp-value'
;

PD_ROUTE_INJECTION
:
   'pd-route-injection'
;

PEAKDETECT
:
   'peakdetect'
;

PEER
:
   'peer'
;

PEERS
:
   'peers'
;

PEER_ADDRESS
:
   'peer-address'
;

PEER_CONFIG_CHECK_BYPASS
:
   'peer-config-check-bypass'
;

PEER_GROUP
:
   'peer-group' -> pushMode ( M_NEIGHBOR )
;

PEER_GATEWAY
:
   'peer-gateway'
;

PEER_ID_VALIDATE
:
   'peer-id-validate'
;

PEER_KEEPALIVE
:
   'peer-keepalive'
;

PEER_LINK
:
   'peer-link'
;

PEER_POLICY
:
   'peer-policy'
;

PEER_SESSION
:
   'peer-session'
;

PEER_SWITCH
:
   'peer-switch'
;

PEER_TO_PEER
:
   'peer-to-peer'
;

PENALTY_PERIOD
:
   'penalty-period'
;

PERCENT_LITERAL
:
   'percent'
;

PERIODIC
:
   'periodic'
;

PERIODIC_INVENTORY
:
   'periodic-inventory'
;

PERIODIC_REFRESH
:
   'periodic-refresh'
;

PERMANENT
:
   'permanent'
;

PERMISSION
:
   'permission'
;

PERMIT
:
   'permit'
;

PERMIT_HOSTDOWN
:
   'permit-hostdown'
;

PERSISTENT
:
   'persistent'
;

PFC
:
   'pfc'
;

PFS
:
   'pfs'
;

PHONE_CONTACT
:
   'phone-contact' -> pushMode ( M_Description )
;

PHONE_NUMBER
:
   'phone-number'
;

PHONE_PROXY
:
   'phone-proxy'
;

PHY
:
   'phy'
;

PHYSICAL_LAYER
:
   'physical-layer'
;

PHYSICAL_PORT
:
   'physical-port'
;

PICKUP
:
   'pickup'
;

PIM
:
   'pim'
;

PIM_AUTO_RP
:
   'pim-auto-rp'
;

PIM_SPARSE
:
   'pim-sparse'
;

PINNING
:
   'pinning'
;

PKI
:
   'pki'
;

PLAT
:
   'plat'
;

PLATFORM
:
   'platform'
;

PM
:
   'pm'
;

POAP
:
   'poap'
;

POINT_TO_MULTIPOINT
:
   'point-to-multipoint'
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
   'policy'
;

POLICY_LIST
:
   'policy-list'
;

POLICY_MAP
:
   'policy-map'
;

POLICY_MAP_INPUT
:
   'policy-map-input'
;

POLICY_MAP_OUTPUT
:
   'policy-map-output'
;

POOL
:
   'pool'
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

PORTFAST
:
   'portfast'
;

PORTS
:
   'ports'
;

PORT_CHANNEL
:
   'port-channel'
;

PORT_CHANNEL_PROTOCOL
:
   'port-channel-protocol'
;

PORT_NAME
:
   'port-name'
;

PORT_OBJECT
:
   'port-object'
;

PORT_PRIORITY
:
   'port-priority'
;

PORT_PROFILE
:
   'port-profile'
;

PORT_SECURITY
:
   'port-security'
;

PORT_TYPE
:
   'port-type'
;

PORT_UNREACHABLE
:
   'port-unreachable'
;

PORTMODE
:
   'portmode'
;

POS
:
   'pos'
;

POWER
:
   'power'
;

POWEROFF
:
   'poweroff'
;

POWER_LEVEL
:
   'power-level'
;

POWER_MGR
:
   'power-mgr'
;

POWER_MONITOR
:
   'power-monitor'
;

PPP
:
   'ppp'
;

PPTP
:
   'pptp'
;

PRC_INTERVAL
:
   'prc-interval'
;

PRE_EQUALIZATION
:
   'pre-equalization'
;

PRE_SHARE
:
   'pre-share'
;

PRE_SHARED_KEY
:
   'pre-shared-key'
;

PRECEDENCE
:
   'precedence'
;

PRECONFIGURE
:
   'preconfigure'
;

PREDICTOR
:
   'predictor'
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

PREFERRED_PATH
:
   'preferred-path'
;

PREFIX
:
   'prefix'
;

PREFIX_LENGTH
:
   'prefix-length'
;

PREFIX_LIST
:
   'prefix-list'
;

PREFIX_PEER_TIMEOUT
:
   'prefix-peer-timeout'
;

PREFIX_PEER_WAIT
:
   'prefix-peer-wait'
;

PREFIX_SET
:
   'prefix-set'
;

PREPEND
:
   'prepend'
;

PRF
:
   'prf'
;

PRI_GROUP
:
   'pri-group'
;

PRIMARY
:
   'primary'
;

PRIMARY_PORT
:
   'primary-port'
;

PRIMARY_PRIORITY
:
   'primary-priority'
;

PRIORITY
:
   'priority'
;

PRIORITY_FLOW_CONTROL
:
   'priority-flow-control'
;

PRIORITY_FORCE
:
   'priority-force'
;

PRIORITY_MAPPING
:
   'priority-mapping'
;

PRIORITY_QUEUE
:
   'priority-queue'
;

PRIV
:
   'priv'
;

PRIVACY
:
   'privacy'
;

PRIVATE_AS
:
   'private-as'
;

PRIVATE_KEY
:
   'private-key' -> pushMode ( M_SshKey )
;

PRIVATE_VLAN
:
   'private-vlan'
;

PRIVILEGE
:
   'privilege'
;

PRIVILEGE_MODE
:
   'privilege-mode'
;

PROACTIVE
:
   'proactive'
;

PROBE
:
   'probe'
;

PROCESS
:
   'process'
;

PROCESS_MAX_TIME
:
   'process-max-time'
;

PROFILE
:
   'profile'
;

PROGRESS_IND
:
   'progress_ind'
;

PROMPT
:
   'prompt'
;

PROPAGATE
:
   'propagate'
;

PROPOSAL
:
   'proposal'
;

PROPRIETARY
:
   'proprietary'
;

PROTECT
:
   'protect'
;

PROTECT_SSID
:
   'protect-ssid'
;

PROTECT_TUNNEL
:
   'protect-tunnel'
;

PROTECT_VALID_STA
:
   'protect-valid-sta'
;

PROTECTION
:
   'protection'
;

PROTOCOL
:
   'protocol'
;

PROTOCOL_HTTP
:
   'protocol-http'
;

PROTOCOL_OBJECT
:
   'protocol-object'
;

PROTOCOL_VIOLATION
:
   'protocol-violation'
;

PROVISION
:
   'provision'
;

PROVISIONING_PROFILE
:
   'provisioning-profile'
;

PROXY_ARP
:
   'proxy-arp'
;

PROXY_SERVER
:
   'proxy-server'
;

PRUNING
:
   'pruning'
;

PSEUDO_INFORMATION
:
   'pseudo-information'
;

PSEUDOWIRE
:
   'pseudowire'
;

PSEUDOWIRE_CLASS
:
   'pseudowire-class'
;

PSH
:
   'psh'
;

PTP
:
   'ptp'
;

PUBKEY_CHAIN
:
   'pubkey-chain'
;

PUBLIC_KEY
:
   'public-key' -> pushMode ( M_SshKey )
;

PVC
:
   'pvc'
;

QOS
:
   'qos'
;

QOS_GROUP
:
   'qos-group'
;

QOS_MAPPING
:
   'qos-mapping'
;

QOS_POLICY
:
   'qos-policy'
;

QOS_POLICY_OUTPUT
:
   'qos-policy-output'
;

QOS_SC
:
   'qos-sc'
;

QUERY_INTERVAL
:
   'query-interval'
;

QUERY_MAX_RESPONSE_TIME
:
   'query-max-response-time'
;

QUERY_ONLY
:
   'query-only'
;

QUERY_TIMEOUT
:
   'query-timeout'
;

QUEUE
:
   'queue'
;

QUEUE_BUFFERS
:
   'queue-buffers'
;

QUEUE_LENGTH
:
   'queue-length'
;

QUEUE_LIMIT
:
   'queue-limit'
;

QUEUE_MONITOR
:
   'queue-monitor'
;

QUEUE_SET
:
   'queue-set'
;

QUEUEING
:
   'queueing'
;

QUEUING
:
   'queuing'
;

QUIT
:
   'quit'
;

RADIUS
:
   'radius'
;

RADIUS_ACCOUNTING
:
   'radius-accounting'
;

RADIUS_ACCT
:
   'radius-acct'
;

RADIUS_COMMON_PW
:
   'radius-common-pw'
;

RADIUS_INTERIM_ACCOUNTING
:
   'radius-interim-accounting'
;

RADIUS_SERVER
:
   'radius-server'
;

RANDOM
:
   'random'
;

RANDOM_DETECT
:
   'random-detect'
;

RANDOM_DETECT_LABEL
:
   'random-detect-label'
;

RANGE
:
   'range'
;

RATE_LIMIT
:
   'rate-limit'
;

RATE_MODE
:
   'rate-mode'
;

RATE_THRESHOLDS_PROFILE
:
   'rate-thresholds-profile'
;

RBACL
:
   'rbacl'
;

RC4_SHA1
:
   'rc4-sha1'
;

RCMD
:
   'rcmd'
;

RCP
:
   'rcp'
;

RCV_QUEUE
:
   'rcv-queue'
;

RD
:
   'rd'
;

REACHABLE_VIA
:
   'reachable-via'
;

REACT
:
   'react'
;

REACTION
:
   'reaction'
;

READ_ONLY_PASSWORD
:
   'read-only-password'
;

REAL
:
   'real'
;

REAL_TIME_CONFIG
:
   'real-time-config'
;

REAUTHENTICATION
:
   'reauthentication'
;

RECEIVE
:
   'receive'
;

RECEIVE_WINDOW
:
   'receive-window'
;

RECONNECT_INTERVAL
:
   'reconnect-interval'
;

RECORD
:
   'record'
;

RECORD_ENTRY
:
   'record-entry'
;

RED
:
   'red'
;

REDIRECT
:
   'redirect'
;

REDIRECT_FQDN
:
   'redirect-fqdn'
;

REDIRECT_LIST
:
   'redirect-list'
;

REDIRECT_PAUSE
:
   'redirect-pause'
;

REDIRECTS
:
   'redirects'
;

REDISTRIBUTE
:
   'redistribute'
;

REDISTRIBUTE_INTERNAL
:
   'redistribute-internal'
;

REDISTRIBUTED_PREFIXES
:
   'redistributed-prefixes'
;

REDUNDANCY
:
   'redundancy'
;

REDUNDANCY_GROUP
:
   'redundancy-group'
;

REFERENCE_BANDWIDTH
:
   'reference-bandwidth'
;

REFLECT
:
   'reflect'
;

REFLECTION
:
   'reflection'
;

REFLEXIVE_LIST
:
   'reflexive-list'
;

REGEX_MODE
:
   'regex-mode'
;

REGISTER_RATE_LIMIT
:
   'register-rate-limit'
;

REGISTER_SOURCE
:
   'register-source'
;

REGISTERED
:
   'registered'
;

REGULATORY_DOMAIN_PROFILE
:
   'regulatory-domain-profile'
;

RELAY
:
   'relay'
;

RELOAD
:
   'reload'
;

RELOAD_DELAY
:
   'reload-delay'
;

RELOAD_TYPE
:
   'reload-type'
;

REMARK
:
   'remark' -> pushMode ( M_REMARK )
;

REMOTE
:
   'remote'
;

REMOTE_ACCESS
:
   'remote-access'
;

REMOTE_AS
:
   'remote-as'
;

REMOTE_IP
:
   'remote-ip'
;

REMOTE_PORT
:
   'remote-port'
;

REMOTE_PORTS
:
   'remote-ports'
;

REMOVE_PRIVATE_AS
:
   'remove-private-' [Aa] [Ss]
;

REMOTE_SERVER
:
   'remote-server'
;

REMOTE_SPAN
:
   'remote-span'
;

REMOVE
:
   'remove'
;

REPLACE_AS
:
   'replace-as'
;

REPLY_TO
:
   'reply-to'
;

REOPTIMIZE
:
   'reoptimize'
;

REQ_TRANS_POLICY
:
   'req-trans-policy'
;

REQUEST
:
   'request'
;

REQUEST_DATA_SIZE
:
   'request-data-size'
;

REQUIRE_WPA
:
   'require-wpa'
;

RESOURCE
:
   'resource'
;

RESOURCE_POOL
:
   'resource-pool'
;

RESOURCES
:
   'resources'
;

RESPONDER
:
   'responder'
;

RESPONSE
:
   'response'
;

RESTART
:
   'restart'
;

RESTART_TIME
:
   'restart-time'
;

RESTRICTED
:
   'restricted'
;

RETRANSMIT
:
   'retransmit'
;

RETRANSMIT_INTERVAL
:
   'retransmit-interval'
;

RETRANSMIT_TIMEOUT
:
   'retransmit-timeout'
;

RETRIES
:
   'retries'
;

RETRY
:
   'retry'
;

REVERSE_ACCESS
:
   'reverse-access'
;

REVERSE_PATH
:
   'reverse-path'
;

REVERSE_ROUTE
:
   'reverse-route'
;

REVERTIVE
:
   'revertive'
;

REVISION
:
   'revision'
;

REVOCATION_CHECK
:
   'revocation-check'
;

REWRITE
:
   'rewrite'
;

RF
:
   'rf'
;

RF_CHANNEL
:
   'rf-channel'
;

RF_POWER
:
   'rf-power'
;

RF_SHUTDOWN
:
   'rf-shutdown'
;

RF_SWITCH
:
   'rf-switch'
;

RFC_3576_SERVER
:
   'rfc-3576-server'
;

RFC1583
:
   'rfc1583'
;

RFC1583COMPATIBILITY
:
   'rfc1583compatibility'
;

RIB_HAS_ROUTE
:
   'rib-has-route'
;

RIB_METRIC_AS_EXTERNAL
:
   'rib-metric-as-external'
;

RIB_METRIC_AS_INTERNAL
:
   'rib-metric-as-internal'
;

RING
:
   'ring'
;

RIP
:
   'rip'
;

RMON
:
   'rmon'
;

RO
:
   [rR] [oO]
;

ROBUSTNESS_VARIABLE
:
   'robustness-variable'
;

ROGUE_AP_AWARE
:
   'rogue-ap-aware'
;

ROLE
:
   'role'
;

ROOT
:
   'root'
;

ROTARY
:
   'rotary'
;

ROUTE
:
   'route'
;

ROUTE_CACHE
:
   'route-cache'
;

ROUTE_MAP
:
   'route-map' -> pushMode ( M_RouteMap )
;

ROUTE_ONLY
:
   'route-only'
;

ROUTE_POLICY
:
   'route-policy'
;

ROUTE_REFLECTOR_CLIENT
:
   'route-reflector-client'
;

ROUTE_SOURCE
:
   'route-source'
;

ROUTE_TARGET
:
   'route-target'
;

ROUTE_TYPE
:
   'route-type'
;

ROUTED
:
   'routed'
;

ROUTER
:
   'router'
;

ROUTER_ADVERTISEMENT
:
   'router-advertisement'
;

ROUTER_ALERT
:
   'router-alert'
;

ROUTER_ID
:
   'router-id'
;

ROUTER_INTERFACE
:
   'router-interface'
;

ROUTER_LSA
:
  'router-lsa'
;

ROUTER_SOLICITATION
:
   'router-solicitation'
;

ROUTING
:
   'routing'
;

RP
:
   'rp'
;

RPF_VECTOR
:
   'rpf-vector'
;

RP_ADDRESS
:
   'rp-address'
;

RP_ANNOUNCE_FILTER
:
   'rp-announce-filter'
;

RP_CANDIDATE
:
   'rp-candidate'
;

RP_LIST
:
   'rp-list'
;

RRM_IE_PROFILE
:
   'rrm-ie-profile'
;

RSA
:
   'rsa'
;

RSA_SIG
:
   'rsa-sig'
;

RSAKEYPAIR
:
   'rsakeypair'
;

RST
:
   'rst'
;

RSTP
:
   'rstp'
;

RSVP
:
   'rsvp'
;

RT
:
   'rt'
;

RTCP_INACTIVITY
:
   'rtcp-inactivity'
;

RTP
:
   'rtp'
;

RTP_PORT
:
   'rtp-port'
;

RTR
:
   'rtr'
;

RTR_ADV
:
   'rtr-adv'
;

RTSP
:
   'rtsp'
;

RULE
:
   'rule' {enableREGEX = true;}
;

RULE_NAME
:
   'rule-name'
;

RUN
:
   'run'
;

RW
:
   [Rr] [Ww]
;

RX
:
   'rx'
;

RX_COS_SLOT
:
   'rx-cos-slot'
;

RXSPEED
:
   'rxspeed'
;

SA_FILTER
:
   'sa-filter'
;

SAME_SECURITY_TRAFFIC
:
   'same-security-traffic'
;

SAMPLER
:
   'sampler'
;

SAMPLER_MAP
:
   'sampler-map'
;

SAMPLES_OF_HISTORY_KEPT
:
   'samples-of-history-kept'
;

SAP
:
   'sap'
;

SAT
:
   'Sat'
;

SATELLITE
:
   'satellite'
;

SATELLITE_FABRIC_LINK
:
   'satellite-fabric-link'
;

SCALE_FACTOR
:
   'scale-factor'
;

SCAN_TIME
:
   'scan-time'
;

SCANNING
:
   'scanning'
;

SCCP
:
   'sccp'
;

SCHED_TYPE
:
   'sched-type'
;

SCHEDULE
:
   'schedule'
;

SCHEDULER
:
   'scheduler'
;

SCHEME
:
   'scheme'
;

SCOPE
:
   'scope'
;

SCP
:
   'scp'
;

SCRAMBLE
:
   'scramble'
;

SCRIPT
:
   'script'
;

SCRIPTING
:
   'scripting'
;

SCTP
:
   'sctp'
;

SDM
:
   'sdm'
;

SDR
:
   'sdr'
;

SDROWNER
:
   'SDROwner'
;

SECONDARY
:
   'secondary'
;

SECONDARY_DIALTONE
:
   'secondary-dialtone'
;

SECRET
:
   'secret'
;

SECUREID_UDP
:
   'secureid-udp'
;

SECURE_MAC_ADDRESS
:
   'secure-mac-address'
;

SECURITY
:
   'security'
;

SECURITY_ASSOCIATION
:
   'security-association'
;

SECURITY_LEVEL
:
   'security-level'
;

SELECT
:
   'select'
;

SELECTION
:
   'selection'
;

SELECTIVE
:
   'selective'
;

SELF
:
   'self'
;

SEND
:
   'send'
;

SEND_COMMUNITY
:
   'send-community'
;

SEND_COMMUNITY_EBGP
:
   'send-community-ebgp'
;

SEND_EXTENDED_COMMUNITY_EBGP
:
   'send-extended-community-ebgp'
;

SEND_LABEL
:
   'send-label'
;

SEND_LIFETIME
:
   'send-lifetime'
;

SEND_RP_ANNOUNCE
:
   'send-rp-announce'
;

SEND_RP_DISCOVERY
:
   'send-rp-discovery'
;

SENDER
:
   'sender'
;

SENSOR
:
   'sensor'
;

SEQ
:
   'seq'
;

SEQUENCE
:
   'sequence'
;

SEQUENCE_NUMS
:
   'sequence-nums'
;

SERIAL
:
   'serial'
;

SERIAL_NUMBER
:
   'serial-number'
;

SERVE
:
   'serve'
;

SERVE_ONLY
:
   'serve-only'
;

SERVER
:
   'server'
;

SERVER_ARP
:
   'server-arp'
;

SERVER_GROUP
:
   'server-group'
;

SERVERFARM
:
   'serverfarm'
;

SERVER_PRIVATE
:
   'server-private'
;

SERVER_TYPE
:
   'server-type'
;

SERVICE
:
   'service'
;

SERVICE_CLASS
:
   'service-class'
;

SERVICE_LIST
:
   'service-list'
;

SERVICE_MODULE
:
   'service-module'
;

SERVICE_OBJECT
:
   'service-object'
;

SERVICE_POLICY
:
   'service-policy'
;

SERVICE_QUEUE
:
   'service-queue'
;

SERVICE_TYPE
:
   'service-type'
;

SESSION
:
   'session'
;

SESSION_AUTHORIZATION
:
   'session-authorization'
;

SESSION_DISCONNECT_WARNING
:
   'session-disconnect-warning' -> pushMode ( M_COMMENT )
;

SESSION_GROUP
:
   'session-group'
;

SESSION_ID
:
   'session-id'
;

SESSION_LIMIT
:
   'session-limit'
;

SESSION_OPEN_MODE
:
   'session-open-mode'
;

SESSION_PROTECTION
:
   'session-protection'
;

SESSION_TIMEOUT
:
   'session-timeout'
;

SET
:
   'set'
;

SET_COLOR
:
   'set-color'
;

SET_OVERLOAD_BIT
:
   'set-overload-bit'
;

SETUP
:
   'setup'
;

SEVERITY
:
   'severity'
;

SFLOW
:
   'sflow'
;

SFTP
:
   'sftp'
;

SG_EXPIRY_TIMER
:
   'sg-expiry-timer'
;

SGBP
:
   'sgbp'
;

SHA
:
   'sha' -> pushMode ( M_Sha )
;

SHA1
:
   'sha1' -> pushMode ( M_SHA1 )
;

SHA2_256_128
:
   'sha2-256-128'
;

SHA512
:
   'sha512'
;

SHA512_PASSWORD
:
   '$sha512$' [0-9]+ '$' F_Base64String '$' F_Base64String -> pushMode ( M_SeedWhitespace )
;

SHAPE
:
   'shape'
;

SHARED_SECONDARY_SECRET
:
   'shared-secondary-secret'
;

SHARED_SECRET
:
   'shared-secret'
;

SHELFNAME
:
   'shelfname'
;

SHELL
:
   'shell'
;

SHORT_TXT
:
   'short-txt'
;

SHOW
:
   'show'
;

SHUTDOWN
:
   'shut' 'down'?
;

SIGNAL
:
   'signal'
;

SIGNALING
:
   'signaling'
;

SIGNALLED_BANDWIDTH
:
   'signalled-bandwidth'
;

SIGNALLED_NAME
:
   'signalled-name'
;

SIGNALLING
:
   'signalling'
;

SIGNATURE
:
   'signature'
;

SIGNATURE_MATCHING_PROFILE
:
   'signature-matching-profile'
;

SIGNATURE_PROFILE
:
   'signature-profile'
;

SIGNING
:
   'signing'
;

SINGLE_CONNECTION
:
   'single-connection'
;

SINGLE_ROUTER_MODE
:
   'single-router-mode'
;

SINGLE_TOPOLOGY
:
   'single-topology'
;

SIP
:
   'sip'
;

SIP_DISCONNECT
:
   'sip-disconnect'
;

SIP_INVITE
:
   'sip-invite'
;

SIP_MEDIA
:
   'sip_media'
;

SIP_MIDCALL_REQ_TIMEOUT
:
   'sip-midcall-req-timeout'
;

SIP_PROFILES
:
   'sip-profiles'
;

SIP_PROVISIONAL_MEDIA
:
   'sip-provisional-media'
;

SIP_SERVER
:
   'sip-server'
;

SIP_UA
:
   'sip-ua'
;

SIPS
:
   'sips'
;

SITE_ID
:
   'site-id'
;

SIZE
:
   'size'
;

SLA
:
   'sla'
;

SLOT
:
   'slot'
;

SLOT_TABLE_COS
:
   'slot-table-cos'
;

SMALL
:
   'small'
;

SMALL_HELLO
:
   'small-hello'
;

SMART_RELAY
:
   'smart-relay'
;

SMTP
:
   'smtp'
;

SMTP_SERVER
:
   'smtp-server'
;

SNMP_AUTHFAIL
:
   'snmp-authfail'
;

SNMP
:
   'snmp'
;

SNMP_SERVER
:
   'snmp-server'
;

SNMP_TRAP
:
   'snmp-trap'
;

SNMPTRAP
:
   'snmptrap'
;

SNOOPING
:
   'snooping'
;

SNP
:
   'snp'
;

SNR_MAX
:
   'snr-max'
;

SNR_MIN
:
   'snr-min'
;

SNTP
:
   'sntp'
;

SOO
:
   'soo'
;

SORT_BY
:
   'sort-by'
;

SPE
:
   'spe'
;

SPECTRUM
:
   'spectrum'
;

SPECTRUM_LOAD_BALANCING
:
   'spectrum-load-balancing'
;

SPECTRUM_MONITORING
:
   'spectrum-monitoring'
;

SPF_INTERVAL
:
   'spf-interval'
;

SOFT_PREEMPTION
:
   'soft-preemption'
;

SOFT_RECONFIGURATION
:
   'soft' '-reconfiguration'?
;

SOFTWARE
:
   'software'
;

SONET
:
   'sonet'
;

SOURCE
:
   'source'
;

SOURCE_ADDRESS
:
   'source-address'
;

SOURCE_INTERFACE
:
   'source-interface' -> pushMode ( M_Interface )
;

SOURCE_IP_ADDRESS
:
   'source-ip-address'
;

SOURCE_PROTOCOL
:
   'source-protocol'
;

SOURCE_ROUTE
:
   'source-route'
;

SOURCE_QUENCH
:
   'source-quench'
;

SPAN
:
   'span'
;

SPANNING_TREE
:
   'spanning-tree'
;

SPARSE_DENSE_MODE
:
   'sparse-dense-mode'
;

SPARSE_MODE
:
   'sparse-mode'
;

SPARSE_MODE_SSM
:
   'sparse-mode-ssm'
;

SPD
:
   'spd'
;

SPEED
:
   'speed'
;

SPEED_DUPLEX
:
   'speed-duplex'
;

SPLIT_TUNNEL_NETWORK_LIST
:
   'split-tunnel-network-list'
;

SPLIT_TUNNEL_POLICY
:
   'split-tunnel-policy'
;

SPT_THRESHOLD
:
   'spt-threshold'
;

SQLNET
:
   'sqlnet'
;

SRC_NAT
:
   'src-nat'
;

SRLG
:
   'srlg'
;

SRR_QUEUE
:
   'srr-queue'
;

SSH
:
   'ssh'
;

SSH_CERTIFICATE
:
   'ssh-certificate'
;

SSH_KEYDIR
:
   'ssh_keydir'
;

SSH_PUBLICKEY
:
   'ssh-publickey'
;

SSID
:
   'ssid'
;

SSID_ENABLE
:
   'ssid-enable'
;

SSID_PROFILE
:
   'ssid-profile'
;

SSL
:
   'ssl'
;

SSM
:
   'ssm'
;

STACK_MAC
:
   'stack-mac'
;

STACK_MIB
:
   'stack-mib'
;

STACK_UNIT
:
   'stack-unit'
;

STALEPATH_TIME
:
   'stalepath-time'
;

STALE_ROUTE
:
   'stale-route'
;

STANDARD
:
   'standard'
   { enableDEC = true; enableACL_NUM = false; }

;

STANDBY
:
   'standby'
;

START_STOP
:
   'start-stop'
;

START_TIME
:
   'start-time'
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

STATE_REFRESH
:
   'state-refresh'
;

STATIC
:
   'static'
;

STATIC_GROUP
:
   'static-group'
;

STATION_ROLE
:
   'station-role'
;

STATISTICS
:
   'statistics'
;

STBC
:
   'stbc'
;

STCAPP
:
   'stcapp'
;

STICKY
:
   'sticky'
;

STICKY_ARP
:
   'sticky-arp'
;

STOP
:
   'stop'
;

STOP_ONLY
:
   'stop-only'
;

STOP_RECORD
:
   'stop-record'
;

STOPBITS
:
   'stopbits'
;

STORM_CONTROL
:
   'storm-control'
;

STP
:
   'stp'
;

STREAMING
:
   'streaming'
;

STREET_ADDRESS
:
   'street-address'
;

STREETADDRESS
:
   'streetaddress' -> pushMode ( M_Description )
;

STRICTHOSTKEYCHECK
:
   'stricthostkeycheck'
;

STRING
:
   'string'
;

STRIP
:
   'strip'
;

STS_1
:
   'sts-1'
;

STUB
:
   'stub'
;

SUBJECT_NAME
:
   'subject-name'
;

SUBMGMT
:
   'submgmt'
;

SUBNET
:
   'subnet'
;

SUBNET_BROADCAST
:
   'subnet-broadcast'
;

SUBNET_MASK
:
   'subnet-mask'
;

SUBNETS
:
   'subnets'
;

SUBNET_ZERO
:
   'subnet-zero'
;

SUB_OPTION
:
   'sub-option'
;

SUB_ROUTE_MAP
:
   'sub-route-map'
;

SUBSCRIBE_TO
:
   'subscribe-to'
;

SUBSCRIBE_TO_ALERT_GROUP
:
   'subscribe-to-alert-group'
;

SUBSCRIBER
:
   'subscriber'
;

SUCCESS
:
   'success'
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

SUN
:
   'Sun'
;

SUNRPC
:
   'sunrpc'
;

SUPER_USER_PASSWORD
:
   'super-user-password'
;

SUPPLEMENTARY_SERVICE
:
   'supplementary-service'
;

SUPPLEMENTARY_SERVICES
:
   'supplementary-services'
;

SUPPRESS
:
   'suppress'
;

SUPPRESS_ARP
:
   'suppress-arp'
;

SUPPRESS_INACTIVE
:
   'suppress-inactive'
;

SUPPRESS_FIB_PENDING
:
   'suppress-fib-pending'
;

SUPPRESS_MAP
:
   'suppress-map'
;

SUPPRESSED
:
   'suppressed'
;

SUSPECT_ROGUE_CONF_LEVEL
:
   'suspect-rogue-conf-level'
;

SVC
:
   'svc'
;

SVCLC
:
   'svclc'
;

SVP
:
   'svp'
;

SWITCH
:
   'switch'
;

SWITCH_CERT
:
   'switch-cert'
;

SWITCH_PRIORITY
:
   'switch-priority'
;

SWITCH_PROFILE
:
   'switch-profile'
;

SWITCH_TYPE
:
   'switch-type'
;

SWITCHBACK
:
   'switchback'
;

SWITCHING_MODE
:
   'switching-mode'
;

SWITCHNAME
:
   'switchname'
;

SWITCHPORT
:
   'switchport'
;

SYMMETRIC
:
   'symmetric'
;

SYN
:
   'syn'
;

SYNC
:
   'sync'
;

SYNCHRONIZATION
:
   'synchronization'
;

SYNCHRONOUS
:
   'synchronous'
;

SYSCONTACT
:
   'syscontact'
;

SYSLOCATION
:
   'syslocation'
;

SYSLOG
:
   'syslog'
;

SYSLOGD
:
   'syslogd'
;

SYSOPT
:
   'sysopt'
;

SYSTEM
:
   'system'
;

SYSTEM_INIT
:
   'system-init'
;

SYSTEM_MAX
:
   'system-max'
;

SYSTEM_PRIORITY
:
   'system-priority'
;

SYSTEM_PROFILE
:
   'system-profile'
;

SYSTEM_SHUTDOWN
:
   'system-shutdown'
;

SYSTEMOWNER
:
   'SystemOwner'
;

TABLE_MAP
:
   'table-map'
;

TACACS
:
   'tacacs'
;

TACACS_DS
:
   'tacacs-ds'
;

TACACS_PLUS
:
   'tacacs+'
;

TACACS_PLUS_ASA
:
   'TACACS+'
;

TACACS_SERVER
:
   'tacacs-server'
;

TAC_PLUS
:
   'tac_plus'
;

TAG
:
   'tag'
;

TAG_SWITCHING
:
   'tag-switching'
;

TAG_TYPE
:
   'tag-type'
;

TAGGED
:
   'tagged'
;

TALK
:
   'talk'
;

TAP
:
   'tap'
;

TASK
:
   'task'
;

TASK_SPACE_EXECUTE
:
   'task execute'
;

TASKGROUP
:
   'taskgroup'
;

TB_VLAN1
:
   'tb-vlan1'
;

TB_VLAN2
:
   'tb-vlan2'
;

TCAM
:
   'tcam'
;

TCP
:
   'tcp'
;

TCP_CONNECT
:
   'tcp-connect'
;

TCP_INSPECTION
:
   'tcp-inspection'
;

TCP_PROXY_REASSEMBLY
:
   'tcp-proxy-reassembly'
;

TCP_SESSION
:
   'tcp-session'
;

TCP_UDP
:
   'tcp-udp'
;

TCS_LOAD_BALANCE
:
   'tcs-load-balance'
;

TELNET
:
   'telnet'
;

TELNET_SERVER
:
   'telnet-server'
;

TEMPLATE
:
   'template'
;

TERMINAL
:
   'terminal'
;

TERMINAL_TYPE
:
   'terminal-type'
;

TERMINATION
:
   'termination'
;

TEST
:
   'test'
;

TFTP
:
   'tftp'
;

TFTP_SERVER
:
   'tftp-server'
;

TFTP_SERVER_LIST
:
   'tftp-server-list'
;

THEN
:
   'then'
;

THREAT_DETECTION
:
   'threat-detection'
;

THREAT_VISIBILITY
:
   'threat-visibility'
;

THREE_DES
:
   '3des'
;

THREE_DES_SHA1
:
   '3des-sha1'
;

THRESHOLD
:
   'threshold'
;

THU
:
   'Thu'
;

TIME
:
   'time'
;

TIME_EXCEEDED
:
   'time-exceeded'
;

TIME_FORMAT
:
   'time-format'
;

TIME_RANGE
:
   'time-range'
;

TIME_OUT
:
   'time-out'
;

TIMEOUT
:
   'timeout'
;

TIMEOUTS
:
   'timeouts'
;

TIMER
:
   'timer'
;

TIMERS
:
   'timers'
;

TIMESOURCE
:
   'timesource'
;

TIMESTAMP
:
   'timestamp'
;

TIMESTAMP_REPLY
:
   'timestamp-reply'
;

TIMESTAMP_REQUEST
:
   'timestamp-request'
;

TIME_ZONE
:
   'time-zone'
;

TIMING
:
   'timing'
;

TLS_PROXY
:
   'tls-proxy'
;

TM_VOQ_COLLECTION
:
   'tm-voq-collection'
;

TOKEN
:
   'token'
;

TOOL
:
   'tool'
;

TOP
:
   'top'
;

TOS
:
   'tos'
;

TOS_OVERWRITE
:
   'tos-overwrite'
;

TRACE
:
   'trace'
;

TRACEROUTE
:
   'traceroute'
;

TRACK
:
   'track'
;

TRACKED
:
   'tracked'
;

TRACKING_PRIORITY_INCREMENT
:
   'tracking-priority-increment'
;

TRADITIONAL
:
   'traditional'
;

TRAFFIC_ENG
:
   'traffic-eng'
;

TRAFFIC_INDEX
:
   'traffic-index'
;

TRANSFER_SYSTEM
:
   'transfer-system'
;

TRANSFORM_SET
:
   'transform-set'
;

TRANSCEIVER
:
   'transceiver'
;

TRANSCEIVER_TYPE_CHECK
:
   'transceiver-type-check'
;

TRANSLATE
:
   'translate'
;

TRANSLATION
:
   'translation'
;

TRANSLATION_RULE
:
   'translation-rule'
;

TRANSLATION_PROFILE
:
   'translation-profile'
;

TRANSMIT
:
   'transmit'
;

TRANSMIT_DELAY
:
   'transmit-delay'
;

TRANSPARENT_HW_FLOODING
:
   'transparent-hw-flooding'
;

TRANSPORT
:
   'transport'
;

TRANSPORT_METHOD
:
   'transport-method'
;

TRANSPORT_MODE
:
   'transport-mode'
;

TRAP
:
   'trap'
;

TRAP_SOURCE
:
   'trap-source' -> pushMode ( M_Interface )
;

TRAP_TIMEOUT
:
   'trap-timeout'
;

TRAPS
:
   'traps'
;

TRIGGER
:
   'trigger'
;

TRIGGER_DELAY
:
   'trigger-delay'
;

TRIMODE
:
   'trimode'
;

TRUNK
:
   'trunk'
;

TRUNK_THRESHOLD
:
   'trunk-threshold'
;

TRUST
:
   'trust'
;

TRUSTED
:
   'trusted'
;

TRUSTED_KEY
:
   'trusted-key'
;

TRUSTPOINT
:
   'trustpoint'
;

TRUSTPOOL
:
   'trustpool'
;

TSID
:
   'tsid'
;

TSM_REQ_PROFILE
:
   'tsm-req-profile'
;

TTL
:
   'ttl'
;

TTL_EXCEEDED
:
   'ttl-exceeded'
;

TTL_THRESHOLD
:
   'ttl-threshold'
;

TTY
:
   'tty'
;

TUE
:
   'Tue'
;

TUNABLE_OPTIC
:
   'tunable-optic'
;

TUNNEL
:
   'tunnel'
;

TUNNEL_GROUP
:
   'tunnel-group'
;

TUNNEL_GROUP_LIST
:
   'tunnel-group-list'
;

TUNNEL_ID
:
   'tunnel-id'
;

TUNNELED_NODE_ADDRESS
:
   'tunneled-node-address'
;

TX_QUEUE
:
   'tx-queue'
;

TXSPEED
:
   'txspeed'
;

TYPE
:
   'type'
;

TYPE_1
:
   'type-1'
;

TYPE_2
:
   'type-2'
;

UAUTH
:
   'uauth'
;

UC_TX_QUEUE
:
   'uc-tx-queue'
;

UDF
:
   'udf'
;

UDLD
:
   'udld'
;

UDP
:
   'udp'
;

UDP_JITTER
:
   'udp-jitter'
;

UID
:
   'uid'
;

UNABLE
:
   'Unable'
;

UNAUTHORIZED_DEVICE_PROFILE
:
   'unauthorized-device-profile'
;

UNICAST_ROUTING
:
   'unicast-routing'
;

UNIQUE
:
   'unique'
;

UNIT
:
   'unit'
;

UNNUMBERED
:
   'unnumbered'
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

UNICAST
:
   'unicast'
;

UNTAGGED
:
   'untagged'
;

UPDATE
:
   'update'
;

UPDATE_CALENDAR
:
   'update-calendar'
;

UPDATE_SOURCE
:
   'update-source' -> pushMode ( M_Interface )
;

UPGRADE
:
   'upgrade'
;

UPGRADE_PROFILE
:
   'upgrade-profile'
;

UPLINK
:
   'uplink'
;

UPLINKFAST
:
   'uplinkfast'
;

UPSTREAM
:
   'upstream'
;

UPSTREAM_START_THRESHOLD
:
   'upstream-start-threshold'
;

URG
:
   'urg'
;

URI
:
    'uri'
;

URL
:
    'url'
;

URL_LIST
:
   'url-list'
;

URPF
:
   'urpf'
;

USE
:
   'use'
;

USE_ACL
:
   'use-acl'
;

USE_BIA
:
   'use-bia'
;

USE_IPV4_ACL
:
   'use-ipv4-acl'
;

USE_IPV6_ACL
:
   'use-ipv6-acl'
;

USE_LINK_ADDRESS
:
   'use-link-address'
;

USE_VRF
:
   'use-vrf'
;

USER
:
   'user'
;

USER_IDENTITY
:
   'user-identity'
;

USERINFO
:
	'userinfo'
;

USER_MESSAGE
:
   'user-message' -> pushMode ( M_Description )
;

USER_ROLE
:
   'user-role'
;

USER_STATISTICS
:
   'user-statistics'
;

USERGROUP
:
   'usergroup'
;

USERNAME
:
   'username'
;

USERNAME_PROMPT
:
   'username-prompt'
;

USERPASSPHRASE
:
   'userpassphrase'
;

USERS
:
   'users'
;

USING
:
   'Using'
;

UTIL_INTERVAL
:
   'util-interval'
;

UUCP
:
   'uucp'
;

V1_RP_REACHABILITY
:
   'v1-rp-reachability'
;

V2
:
   'v2'
;

V4
:
   'v4'
;

V6
:
   'v6'
;

VACANT_MESSAGE
:
   'vacant-message' -> pushMode ( M_VacantMessageText )
;

VACL
:
   'vacl'
;

VAD
:
   'vad'
;

VALID_11A_40MHZ_CHANNEL_PAIR
:
   'valid-11a-40mhz-channel-pair'
;

VALID_11A_80MHZ_CHANNEL_GROUP
:
   'valid-11a-80mhz-channel-group'
;

VALID_11A_CHANNEL
:
   'valid-11a-channel'
;

VALID_11G_40MHZ_CHANNEL_PAIR
:
   'valid-11g-40mhz-channel-pair'
;

VALID_11G_CHANNEL
:
   'valid-11g-channel'
;

VALID_AND_PROTECTED_SSID
:
   'valid-and-protected-ssid'
;

VALID_NETWORK_OUI_PROFILE
:
   'valid-network-oui-profile'
;

VALIDATION_USAGE
:
   'validation-usage'
;

VAP_ENABLE
:
   'vap-enable'
;

VDC
:
   'vdc'
;

VER
:
   'ver'
;

VERIFY
:
   'verify'
;

VERIFY_DATA
:
   'verify-data'
;

VERSION
:
   'version'
;

VIDEO
:
   'video'
;

VIEW
:
   'view'
;

VIOLATE_ACTION
:
   'violate-action'
;

VIOLATION
:
   'violation'
;

VIRTUAL
:
   'virtual'
;

VIRTUAL_ADDRESS
:
   'virtual-address'
;

VIRTUAL_AP
:
   'virtual-ap'
;

VIRTUAL_REASSEMBLY
:
   'virtual-reassembly'
;

VIRTUAL_ROUTER
:
   'virtual-router'
;

VIRTUAL_SERVICE
:
   'virtual-service'
;

VIRTUAL_TEMPLATE
:
   'virtual-template'
;

VFI
:
   'vfi'
;

VLAN
:
   'vlan'
;

VLAN_GROUP
:
   'vlan-group'
;

VLAN_NAME
:
   'vlan-name'
;

VLAN_POLICY
:
   'vlan-policy'
;

VLT
:
   'vlt'
;

VLT_PEER_LAG
:
   'vlt-peer-lag'
;

VM_CPU
:
   'vm-cpu'
;

VM_MEMORY
:
   'vm-memory'
;

VMPS
:
   'vmps'
;

VMTRACER
:
   'vmtracer'
;

VOCERA
:
   'vocera'
;

VOICE
:
   'voice'
;

VOICE_CARD
:
   'voice-card'
;

VOICE_CLASS
:
   'voice-class'
;

VOICE_PORT
:
   'voice-port'
;

VOICE_SERVICE
:
   'voice-service'
;

VOIP
:
   'voip'
;

VOIP_CAC_PROFILE
:
   'voip-cac-profile'
;

VPC
:
   'vpc'
;

VPDN
:
   'vpdn'
;

VPDN_GROUP
:
   'vpdn-group'
;

VPLS
:
   'vpls'
;

VPN
:
   'vpn'
;

VPN_DIALER
:
   'vpn-dialer'
;

VPN_GROUP_POLICY
:
   'vpn-group-policy'
;

VPN_FILTER
:
   'vpn-filter'
;

VPN_IDLE_TIMEOUT
:
   'vpn-idle-timeout'
;

VPN_SESSION_TIMEOUT
:
   'vpn-session-timeout'
;

VPN_SIMULTANEOUS_LOGINS
:
   'vpn-simultaneous-logins'
;

VPN_TUNNEL_PROTOCOL
:
   'vpn-tunnel-protocol'
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
   'vrf'
   {enableIPV6_ADDRESS = false;}

;

VRF_ALSO
:
   'vrf-also'
;

VRRP
:
   'vrrp'
;

VRRP_GROUP
:
   'vrrp-group'
;

VSERVER
:
   'vserver'
;

VSTACK
:
   'vstack'
;

VTP
:
   'vtp'
;

VTY
:
   'vty'
;

VTY_POOL
:
   'vty-pool'
;

VXLAN
:
   'vxlan'
;

WAIT_FOR
:
  'wait-for'
;

WAIT_IGP_CONVERGENCE
:
   'wait-igp-convergence'
;

WAIT_START
:
   'wait-start'
;

WARNINGS
:
   'warnings'
;

WARNING_ONLY
:
   'warning-only'
;

WARNTIME
:
   'warntime'
;

WATCHDOG
:
   'watchdog'
;

WATCH_LIST
:
   'watch-list'
;

WAVELENGTH
:
   'wavelength'
;

WCCP
:
   'wccp'
;

WEB_CACHE
:
   'web-cache'
;

WEB_HTTPS_PORT_443
:
   'web-https-port-443'
;

WEB_MAX_CLIENTS
:
   'web-max-clients'
;

WEB_SERVER
:
   'web-server'
;

WEBVPN
:
   'webvpn'
;

WED
:
   'Wed'
;

WEEKDAY
:
   'weekday'
;

WEEKEND
:
   'weekend'
;

WEIGHT
:
   'weight'
;

WEIGHTING
:
   'weighting'
;

WELCOME_PAGE
:
   'welcome-page'
;

WHITE_LIST
:
   'white-list'
;

WHO
:
   'who'
;

WHOIS
:
   'whois'
;

WIDE
:
   'wide'
;

WIDE_METRIC
:
   'wide-metric'
;

WIDEBAND
:
   'wideband'
;

WINDOW_SIZE
:
   'window-size'
;

WINS_SERVER
:
   'wins-server'
;

WIRED_AP_PROFILE
:
   'wired-ap-profile'
;

WIRED_CONTAINMENT
:
   'wired-containment'
;

WIRED_PORT_PROFILE
:
   'wired-port-profile'
;

WIRED_TO_WIRELESS_ROAM
:
   'wired-to-wireless-roam'
;

WIRELESS_CONTAINMENT
:
   'wireless-containment'
;

WISM
:
   'wism'
;

WITHOUT_CSD
:
   'without-csd'
;

WLAN
:
   'wlan'
;

WMM
:
   'wmm'
;

WMS_GENERAL_PROFILE
:
   'wms-general-profile'
;

WMS_LOCAL_SYSTEM_PROFILE
:
   'wms-local-system-profile'
;

WPA_FAST_HANDOVER
:
   'wpa-fast-handover'
;

WRED
:
   'wred'
;

WRED_PROFILE
:
   'wred-profile'
;

WRITE_MEMORY
:
   'write-memory'
;

WRR
:
   'wrr'
;

WRR_QUEUE
:
   'wrr-queue'
;

WSMA
:
   'wsma'
;

WWW
:
   'www'
;

X25
:
   'x25'
;

X29
:
   'x29'
;

XCONNECT
:
   'xconnect'
;

XDMCP
:
   'xdmcp'
;

XDR
:
   'xdr'
;

XLATE
:
   'xlate'
;

XML
:
   'XML'
   | 'xml'
;

XML_CONFIG
:
   'xml-config'
;

YELLOW
:
   'yellow'
;

ZONE
:
   'zone'
;

ZONE_MEMBER
:
   'zone-member'
;

ZONE_PAIR
:
  'zone-pair'
;

/* Other Tokens */

MULTICONFIGPART
:
   '############ MultiConfigPart' F_NonNewline* F_Newline+ -> channel ( HIDDEN
   )
;

MD5_ARISTA
:
   '$1$' F_AristaBase64String '$' F_AristaBase64String
;

SHA512_ARISTA
:
   '$6$' F_AristaBase64String '$' F_AristaBase64String
;

POUND
:
   '#' -> pushMode ( M_Description )
;

COMMUNITY_NUMBER
:
   F_Digit
   {!enableIPV6_ADDRESS}?

   F_Digit* ':' F_Digit+
;

MAC_ADDRESS_LITERAL
:
   F_HexDigit F_HexDigit F_HexDigit F_HexDigit '.' F_HexDigit F_HexDigit
   F_HexDigit F_HexDigit '.' F_HexDigit F_HexDigit F_HexDigit F_HexDigit
;

HEX
:
   '0x' F_HexDigit+
;

VARIABLE
:
   (
      (
         F_Variable_RequiredVarChar
         (
            (
               {!enableIPV6_ADDRESS}?

               F_Variable_VarChar*
            )
            |
            (
               {enableIPV6_ADDRESS}?

               F_Variable_VarChar_Ipv6*
            )
         )
      )
      |
      (
         (
            F_Variable_VarChar
            {!enableIPV6_ADDRESS}?

            F_Variable_VarChar* F_Variable_RequiredVarChar F_Variable_VarChar*
         )
         |
         (
            F_Variable_VarChar_Ipv6
            {enableIPV6_ADDRESS}?

            F_Variable_VarChar_Ipv6* F_Variable_RequiredVarChar
            F_Variable_VarChar_Ipv6*
         )
      )
   )
   {
      if (enableACL_NUM) {
         enableACL_NUM = false;
         enableDEC = true;
      }
      if (enableCOMMUNITY_LIST_NUM) {
         enableCOMMUNITY_LIST_NUM = false;
         enableDEC = true;
      }
   }

;

ACL_NUM
:
   F_Digit
   {enableACL_NUM}?

   F_Digit*
   {
	int val = Integer.parseInt(getText());
	if ((1 <= val && val <= 99) || (1300 <= val && val <= 1999)) {
		_type = ACL_NUM_STANDARD;
	}
	else if ((100 <= val && val <= 199) || (2000 <= val && val <= 2699)) {
		_type = ACL_NUM_EXTENDED;
	}
	else if (200 <= val && val <= 299) {
		_type = ACL_NUM_PROTOCOL_TYPE_CODE;
	}
   else if (_foundry && 400 <= val && val <= 1399) {
      _type = ACL_NUM_FOUNDRY_L2;
   }
	else if (600 <= val && val <= 699) {
		_type = ACL_NUM_APPLETALK;
	}
   else if (700 <= val && val <= 799) {
      _type = ACL_NUM_MAC;
   }
	else if (800 <= val && val <= 899) {
		_type = ACL_NUM_IPX;
	}
	else if (900 <= val && val <= 999) {
		_type = ACL_NUM_EXTENDED_IPX;
	}
	else if (1000 <= val && val <= 1099) {
		_type = ACL_NUM_IPX_SAP;
	}
	else if (1100 <= val && val <= 1199) {
		_type = ACL_NUM_EXTENDED_MAC;
	}
	else {
		_type = ACL_NUM_OTHER;
	}
	enableDEC = true;
	enableACL_NUM = false;
}

;

AMPERSAND
:
   '&'
;

ANGLE_BRACKET_LEFT
:
   '<'
;

ANGLE_BRACKET_RIGHT
:
   '>'
;

ASTERISK
:
   '*'
;

AT
:
   '@'
;

BACKSLASH
:
   '\\'
;

BLANK_LINE
:
   (
      F_Whitespace
   )* F_Newline
   {lastTokenType == NEWLINE}?

   F_Newline* -> channel ( HIDDEN )
;

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

CARAT
:
   '^'
;

COLON
:
   ':'
;

COMMA
:
   ','
;

COMMUNITY_LIST_NUM
:
   F_Digit
   {enableCOMMUNITY_LIST_NUM}?

   F_Digit*
   {
		int val = Integer.parseInt(getText());
		if (1 <= val && val <= 99) {
			_type = COMMUNITY_LIST_NUM_STANDARD;
		}
		else if (100 <= val && val <= 500) {
			_type = COMMUNITY_LIST_NUM_EXPANDED;
		}
		enableCOMMUNITY_LIST_NUM = false;
		enableDEC = true;
	}

;

COMMENT_LINE
:
   (
      F_Whitespace
   )* [!#]
   {lastTokenType == NEWLINE || lastTokenType == END_CADANT || lastTokenType == -1}?

   F_NonNewline* F_Newline+ -> channel ( HIDDEN )
;

COMMENT_TAIL
:
   '!' F_NonNewline* -> channel ( HIDDEN )
;

ARISTA_PAGINATION_DISABLED
:
   'Pagination disabled.' F_Newline+ -> channel ( HIDDEN )
;

ARISTA_PROMPT_SHOW_RUN
:
   F_NonWhitespace+ [>#]
   {lastTokenType == NEWLINE || lastTokenType == -1}?

   'show' F_Whitespace+ 'run' ( 'n' ( 'i' ( 'n' ( 'g' ( '-' ( 'c' ( 'o' ( 'n' ( 'f' ( 'i' 'g'? )? )? )? )? )? )? )? )? )? )? F_Whitespace* F_Newline+ -> channel ( HIDDEN )
;

DASH
:
   '-'
;

DOLLAR
:
   '$'
;

DEC
:
   F_Digit
   {enableDEC}?

   F_Digit*
;

DIGIT
:
   F_Digit
;

DOUBLE_QUOTE
:
   '"'
;

EQUALS
:
   '='
;

ESCAPE_C
:
   (
      '^C'
      | '\u0003'
      | '#'
   )
;

FLOAT
:
   (
      F_PositiveDigit* F_Digit '.' F_Digit+
   )
;

FORWARD_SLASH
:
   '/'
;

FOUR_BYTE_AS
:
   '4-byte-as'
;

IP_ADDRESS
:
   F_DecByte '.'
   {enableIP_ADDRESS}?

   F_DecByte '.' F_DecByte '.' F_DecByte
;

IP_PREFIX
:
   F_DecByte '.'
   {enableIP_ADDRESS}?

   F_DecByte '.' F_DecByte '.' F_DecByte '/' F_Digit F_Digit?
;

IPV6_ADDRESS
:
   (
      (
         ':'
         {enableIPV6_ADDRESS}?

         ':'
         (
            (
               F_HexDigit+ ':'
            )* F_HexDigit+
         )?
      )
      |
      (
         (
            F_HexDigit+
            {enableIPV6_ADDRESS}?

            ':' ':'?
         )+ F_HexDigit*
      )
   )
   (
      ':' F_DecByte '.' F_DecByte '.' F_DecByte '.' F_DecByte
   )?
;

IPV6_PREFIX
:
   (
      (
         ':'
         {enableIPV6_ADDRESS}?

         ':'
         (
            (
               F_HexDigit+ ':'
            )* F_HexDigit+
         )?
      )
      |
      (
         (
            F_HexDigit+
            {enableIPV6_ADDRESS}?

            ':' ':'?
         )+ F_HexDigit*
      )
   )
   (
      ':' F_DecByte '.' F_DecByte '.' F_DecByte '.' F_DecByte
   )? '/' F_DecByte
;

NEWLINE
:
   F_Newline+
   {
      if (!inCommunitySet) {
   	  enableIPV6_ADDRESS = true;
   	}
   	enableIP_ADDRESS = true;
      enableDEC = true;
      enableREGEX = false;
      enableACL_NUM = false;
      _inAccessList = false;
  }

;

PAREN_LEFT
:
   '('
;

PAREN_RIGHT
:
   ')'
;

PERCENT
:
   '%'
;

PERIOD
:
   '.'
;

PLUS
:
   '+'
;

REGEX
:
   '/' {enableREGEX}?
   (
      ~('/' | '\\')
      |
      ( '\\' '/')
   )* '/'
;

RP_VARIABLE
:
   '$' F_Variable_RequiredVarChar F_Variable_VarChar_Ipv6*
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

WS
:
   F_Whitespace+ -> channel ( HIDDEN )
; // Fragments

fragment
F_AristaBase64Char
:
   [0-9A-Za-z/.]
;

fragment
F_AristaBase64String
:
   F_AristaBase64Char+
;

fragment
F_Base64Char
:
   [0-9A-Za-z/+]
;

fragment
F_Base64Quadruple
:
   F_Base64Char F_Base64Char F_Base64Char F_Base64Char
;
fragment
F_Base64String
:
   F_Base64Quadruple*
   (
      F_Base64Quadruple
      | F_Base64Char F_Base64Char '=='
      | F_Base64Char F_Base64Char F_Base64Char '='
   )
;

fragment
F_Dec16
:
   (
      F_PositiveDigit F_Digit F_Digit F_Digit F_Digit
   )
   |
   (
      F_PositiveDigit F_Digit F_Digit F_Digit
   )
   |
   (
      F_PositiveDigit F_Digit F_Digit
   )
   |
   (
      F_PositiveDigit F_Digit
   )
   | F_Digit
;

fragment
F_DecByte
:
   (
      F_PositiveDigit F_Digit F_Digit
   )
   |
   (
      F_PositiveDigit F_Digit
   )
   | F_Digit
;

fragment
F_Digit
:
   '0' .. '9'
;

fragment
F_HexDigit
:
   (
      '0' .. '9'
      | 'a' .. 'f'
      | 'A' .. 'F'
   )
;

fragment
F_HexWord
:
   F_HexDigit F_HexDigit F_HexDigit F_HexDigit
;

fragment
F_Letter
:
   F_LowerCaseLetter
   | F_UpperCaseLetter
;

fragment
F_LowerCaseLetter
:
   'a' .. 'z'
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
   ~( ' ' | '\t' | '\u000C' | '\n' | '\r' )
;

F_PositiveHexDigit
:
   (
      '1' .. '9'
      | 'a' .. 'f'
      | 'A' .. 'F'
   )
;

fragment
F_PositiveDigit
:
   '1' .. '9'
;

fragment
F_UpperCaseLetter
:
   'A' .. 'Z'
;

fragment
F_Variable_RequiredVarChar
:
   ~( '0' .. '9' | '-' | [ \t\n\r(),!+$'"*#] | '[' | ']' | [/.] | ':' )
;

fragment
F_Variable
:
   F_Variable_VarChar* F_Variable_RequiredVarChar F_Variable_VarChar*
;

fragment
F_Variable_VarChar
:
   ~( [ \t\n\r(),!$'"*#] | '[' | ']' )
;

fragment
F_Variable_VarChar_Ipv6
:
   ~( [ \t\n\r(),!$'"*#] | '[' | ']' | ':' )
;

fragment
F_Whitespace
:
   ' '
   | '\t'
   | '\u000C'
;

mode M_Alias;

M_Alias_VARIABLE
:
   F_NonWhitespace+ -> type ( VARIABLE ) , popMode
;

M_Alias_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_AsPath;

M_AsPath_ACCESS_LIST
:
   'access-list' -> type ( ACCESS_LIST ) , mode ( M_AsPathAccessList )
;

M_AsPath_CONFED
:
   'confed' -> type ( CONFED ) , popMode
;

M_AsPath_DEC
:
   F_Digit+ -> type ( DEC ) , popMode
;

M_AsPath_RP_VARIABLE
:
   '$' F_Variable_RequiredVarChar F_Variable_VarChar_Ipv6* -> type (
   RP_VARIABLE ) , popMode
;

M_AsPath_IN
:
   'in' -> type ( IN ) , popMode
;

M_AsPath_IS_LOCAL
:
   'is-local' -> type ( IS_LOCAL ) , popMode
;

M_AsPath_MULTIPATH_RELAX
:
   'multipath-relax' -> type ( MULTIPATH_RELAX ) , popMode
;

M_AsPath_NEIGHBOR_IS
:
   'neighbor-is' -> type ( NEIGHBOR_IS ) , popMode
;

M_AsPath_PASSES_THROUGH
:
   'passes-through' -> type ( PASSES_THROUGH ) , popMode
;

M_AsPath_PREPEND
:
   'prepend' -> type ( PREPEND ) , popMode
;

M_AsPath_ORIGINATES_FROM
:
   'originates-from' -> type ( ORIGINATES_FROM ) , popMode
;

M_AsPath_REGEX_MODE
:
   'regex-mode' -> type ( REGEX_MODE ) , popMode
;

M_AsPath_TAG
:
   'tag' -> type ( TAG ) , popMode
;

M_AsPath_VARIABLE
:
   F_Variable_RequiredVarChar F_Variable_VarChar* -> type ( VARIABLE ) ,
   popMode
;

M_AsPath_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_AsPathAccessList;

M_AsPathAccessList_DEC
:
   F_Digit+ -> type ( DEC )
;

M_AsPathAccessList_DENY
:
   'deny' -> type ( DENY ) , mode ( M_Description )
;

M_AsPathAccessList_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , mode ( DEFAULT_MODE )
;

M_AsPathAccessList_PERMIT
:
   'permit' -> type ( PERMIT ) , mode ( M_Description )
;

M_AsPathAccessList_SEQ
:
   'seq' -> type ( SEQ )
;

M_AsPathAccessList_VARIABLE
:
   F_Variable_RequiredVarChar F_Variable_VarChar* -> type ( VARIABLE )
;

M_AsPathAccessList_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Authentication;

M_Authentication_DOUBLE_QUOTE
:
   '"' -> mode ( M_DoubleQuote )
;

M_Authentication_BANNER
:
   'banner' -> type ( BANNER ) , mode ( M_BannerText )
;

M_Authentication_ARAP
:
   'arap' -> type ( ARAP ) , popMode
;

M_Authentication_ATTEMPTS
:
   'attempts' -> type ( ATTEMPTS ) , popMode
;

M_Authentication_CAPTIVE_PORTAL
:
   'captive-portal' -> type ( CAPTIVE_PORTAL ) , popMode
;

M_Authentication_COMMAND
:
   'command' -> type ( COMMAND ) , popMode
;

M_Authentication_CONTROL_DIRECTION
:
   'control-direction' -> type ( CONTROL_DIRECTION ) , popMode
;

M_Authentication_DOT1X
:
   'dot1x' -> type ( DOT1X ) , popMode
;

M_Authentication_ENABLE
:
   'enable' -> type ( ENABLE ) , popMode
;

M_Authentication_EOU
:
   'eou' -> type ( EOU ) , popMode
;

M_Authentication_FAIL_MESSAGE
:
   'fail-message' -> type ( FAIL_MESSAGE ) , popMode
;

M_Authentication_FAILURE
:
   'failure' -> type ( FAILURE ) , popMode
;

M_Authentication_HTTP
:
   'http' -> type ( HTTP ) , popMode
;

M_Authentication_INCLUDE
:
   'include' -> type ( INCLUDE ) , popMode
;

M_Authentication_LOGIN
:
   'login' -> type ( LOGIN ) , popMode
;

M_Authentication_MAC
:
   'mac' -> type ( MAC ) , popMode
;

M_Authentication_MAC_MOVE
:
   'mac-move' -> type ( MAC_MOVE ) , popMode
;

M_Authentication_MESSAGE_DIGEST
:
   'message-digest' -> type ( MESSAGE_DIGEST ) , popMode
;

M_Authentication_MGMT
:
   'mgmt' -> type ( MGMT ) , popMode
;

M_Authentication_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Authentication_ONEP
:
   'onep' -> type ( ONEP ) , popMode
;

M_Authentication_PASSWORD_PROMPT
:
   'password-prompt' -> type ( PASSWORD_PROMPT ) , popMode
;

M_Authentication_POLICY
:
   'policy' -> type ( POLICY ) , popMode
;

M_Authentication_PPP
:
   'ppp' -> type ( PPP ) , popMode
;

M_Authentication_PRE_SHARE
:
   'pre-share' -> type ( PRE_SHARE ) , popMode
;

M_Authentication_RSA_SIG
:
   'rsa-sig' -> type ( RSA_SIG ) , popMode
;

M_Authentication_SGBP
:
   'sgbp' -> type ( SGBP ) , popMode
;

M_Authentication_SERIAL
:
   'serial' -> type ( SERIAL ) , popMode
;

M_Authentication_SSH
:
   'ssh' -> type ( SSH ) , popMode
;

M_Authentication_STATEFUL_DOT1X
:
   'stateful-dot1x' -> type ( STATEFUL_DOT1X ) , popMode
;

M_Authentication_STATEFUL_KERBEROS
:
   'stateful-kerberos' -> type ( STATEFUL_KERBEROS ) , popMode
;

M_Authentication_STATEFUL_NTLM
:
   'stateful-ntlm' -> type ( STATEFUL_NTLM ) , popMode
;

M_Authentication_SUCCESS
:
   'success' -> type ( SUCCESS ) , popMode
;

M_Authentication_SUPPRESS
:
   'suppress' -> type ( SUPPRESS ) , popMode
;

M_Authentication_TELNET
:
   'telnet' -> type ( TELNET ) , popMode
;

M_Authentication_TEXT
:
   'text' -> type ( TEXT ) , popMode
;

M_Authentication_USERNAME_PROMPT
:
   'username-prompt' -> type ( USERNAME_PROMPT ) , mode (
   M_AuthenticationUsernamePrompt )
;

M_Authentication_VPN
:
   'vpn' -> type ( VPN ) , popMode
;

M_Authentication_WIRED
:
   'wired' -> type ( WIRED ) , popMode
;

M_Authentication_WISPR
:
   'wispr' -> type ( WISPR ) , popMode
;

M_Authentication_VARIABLE
:
   F_Variable -> type ( VARIABLE ) , popMode
;

M_Authentication_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_AuthenticationUsernamePrompt;

M_AuthenticationUsernamePrompt_DOUBLE_QUOTE
:
   '"' -> type ( DOUBLE_QUOTE ) , mode ( M_AuthenticationUsernamePromptText )
;

M_AuthenticationUsernamePrompt_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_AuthenticationUsernamePromptText;

M_AuthenticationUsernamePromptText_RAW_TEXT
:
   ~'"'+ -> type ( RAW_TEXT )
;

M_AuthenticationUsernamePromptText_DOUBLE_QUOTE
:
   '"' -> type ( DOUBLE_QUOTE ) , popMode
;

mode M_Banner;

M_Banner_CONFIG_SAVE
:
   'config-save' -> type ( CONFIG_SAVE ) , mode ( M_BannerText )
;

M_Banner_EXEC
:
   'exec' -> type ( EXEC ) , mode ( M_BannerText )
;

M_Banner_INCOMING
:
   'incoming' -> type ( INCOMING ) , mode ( M_BannerText )
;

M_Banner_LOGIN
:
   'login' -> type ( LOGIN ) , mode ( M_BannerText )
;

M_Banner_MOTD
:
   'motd' -> type ( MOTD ) , mode ( M_BannerText )
;

M_Banner_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Banner_NONE
:
   'none' -> type ( NONE )
;

M_Banner_PROMPT_TIMEOUT
:
   'prompt-timeout' -> type ( PROMPT_TIMEOUT ) , mode ( M_BannerText )
;

M_Banner_SLIP_PPP
:
   'slip-ppp' -> type ( SLIP_PPP ) , mode ( M_BannerText )
;

M_Banner_VALUE
:
   'value' -> type ( VALUE ) , mode ( M_Description )
;

M_Banner_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_BannerCadant;

M_BannerCadant_END_CADANT
:
   '/end' F_Newline -> type ( END_CADANT ) , popMode
;

M_BannerCadant_LINE_CADANT
:
   F_NonNewline* F_Newline+ -> type ( LINE_CADANT )
;

mode M_BannerText;

M_BannerText_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

M_BannerText_ESCAPE_C
:
   (
      '^C'
      | '^'
      | '\u0003'
   ) {!_cadant}? -> type ( ESCAPE_C ) , mode ( M_MOTD_C )
;

M_BannerText_HASH
:
   '#' {!_cadant}? -> type ( POUND ) , mode ( M_MOTD_HASH )
;

M_BannerText_ASA_BANNER_LINE
:
   ~[#^\r\n \t\u000C] F_NonNewline* -> type ( ASA_BANNER_LINE ) , popMode
;

M_BannerText_NEWLINE
:
   F_Newline {!_cadant}? F_Newline* -> type ( NEWLINE ) , mode ( M_MOTD_EOF )
;

M_BannerText_NEWLINE_CADANT
:
   F_Newline {_cadant}? F_Newline* -> type ( NEWLINE ) , mode ( M_BannerCadant )
;

mode M_CadantSshKey;

M_CadantSshKey_END
:
   '/end' F_NonNewline* F_Newline -> type ( END_CADANT ) , popMode
;

M_CadantSshKey_LINE
:
   F_HexDigit+ F_Newline+
;

M_CadantSshKey_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

M_CadantSshKey_NEWLINE
:
   F_Newline+ -> type ( NEWLINE )
;

mode M_Certificate;

M_Certificate_CA
:
   'ca' -> type ( CA ) , pushMode ( M_CertificateText )
;

M_Certificate_CHAIN
:
   'chain' -> type ( CHAIN ) , popMode
;

M_Certificate_SELF_SIGNED
:
   'self-signed' -> type ( SELF_SIGNED ) , pushMode ( M_CertificateText )
;

M_Cerficate_HEX_FRAGMENT
:
   [A-Fa-f0-9]+ -> type ( HEX_FRAGMENT ) , pushMode ( M_CertificateText )
;

M_Certificate_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_CertificateText;

M_CertificateText_QUIT
:
   'quit' -> type ( QUIT ) , mode ( DEFAULT_MODE )
;

M_CertificateText_HEX_FRAGMENT
:
   [A-Fa-f0-9]+ -> type ( HEX_FRAGMENT )
;

M_CertificateText_WS
:
   (
      F_Whitespace
      | F_Newline
   )+ -> channel ( HIDDEN )
;

mode M_Command;

M_Command_QuotedString
:
   '"'
   (
      ~'"'
   )* '"' -> type ( QUOTED_TEXT )
;

M_Command_Newline
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Command_Variable
:
   F_NonWhitespace+ -> type ( VARIABLE )
;

M_Command_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_COMMENT;

M_COMMENT_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_COMMENT_NON_NEWLINE
:
   F_NonNewline+
;

mode M_Des;

M_Des_DEC_PART
:
   F_Digit+
;

M_Des_HEX_PART
:
   F_HexDigit+ -> popMode
;

M_Des_REDACTED
:
   '*'+ -> popMode
;

M_Des_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Des_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Description;

M_Description_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Description_NON_NEWLINE
:
   F_NonNewline+ -> type ( RAW_TEXT )
;

mode M_DoubleQuote;

M_DoubleQuote_DOUBLE_QUOTE
:
   '"' -> type ( DOUBLE_QUOTE ) , popMode
;

M_DoubleQuote_TEXT
:
   ~'"'+
;

mode M_Execute;

M_Execute_TEXT
:
   ~'}'+
;

M_Execute_BRACE_RIGHT
:
   '}' -> type ( BRACE_RIGHT ) , popMode
;

mode M_Extcommunity;

M_Extcommunity_COLON
:
   ':' -> type ( COLON )
;

M_Extcommunity_DEC
:
   F_Digit+ -> type ( DEC )
;

M_ExtCommunity_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Extcommunity_RT
:
   'rt' -> type ( RT )
;

M_Extcommunity_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_FiberNode;

M_FiberNode_DEC
:
   F_Digit+ -> type ( DEC ) , popMode
;

M_FiberNode_DOUBLE_QUOTE
:
   '"' -> type ( DOUBLE_QUOTE ) , mode ( M_DoubleQuote )
;

M_FiberNode_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Interface;

M_Interface_ALL
:
   'all' -> type ( ALL ) , popMode
;

M_Interface_BREAKOUT
:
   'breakout' -> type ( BREAKOUT ) , popMode
;

M_Interface_CABLE
:
   'cable' -> type ( CABLE ) , popMode
;

M_Interface_DEFAULT
:
   'default' -> type ( DEFAULT ) , popMode
;

M_Interface_DOLLAR
:
   '$' -> type ( DOLLAR ) , popMode
;

M_Interface_EQ
:
   'eq' -> type ( EQ ) , popMode
;

M_Interface_GLOBAL
:
   'global' -> type ( GLOBAL ) , popMode
;

M_Interface_GT
:
   'gt' -> type ( GT ) , popMode
;

M_Interface_INFORM
:
   'inform' -> type ( INFORM )
;

M_Interface_INFORMS
:
   'informs' -> type ( INFORMS )
;

M_Interface_IP
:
   'ip' -> type ( IP ) , popMode
;

M_Interface_IPV4
:
   'IPv4' -> type ( IPV4 )
;

M_Interface_POINT_TO_POINT
:
   'point-to-point' -> type ( POINT_TO_POINT ) , popMode
;

M_Interface_POLICY
:
   'policy' -> type ( POLICY ) , popMode
;

M_Interface_L2TRANSPORT
:
   'l2transport' -> type ( L2TRANSPORT ) , popMode
;

M_Interface_LT
:
   'lt' -> type ( LT ) , popMode
;

M_Interface_MODULE
:
   'module' -> type ( MODULE )
;

M_Interface_NO
:
   'no' -> type ( NO ) , popMode
;

M_Interface_MULTIPOINT
:
   'multipoint' -> type ( MULTIPOINT ) , popMode
;

M_Interface_SHUTDOWN
:
   'shutdown' -> type ( SHUTDOWN ) , popMode
;

M_Interface_TRAP
:
   'trap' -> type ( TRAP )
;

M_Interface_TRAPS
:
   'traps' -> type ( TRAPS )
;

M_Interface_VRF
:
   'vrf' -> type ( VRF ) , popMode
;

M_Interface_COLON
:
   ':' -> type ( COLON )
;

M_Interface_COMMA
:
   ',' -> type ( COMMA )
;

M_Interface_DASH
:
   '-' -> type ( DASH )
;

M_Interface_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Interface_NUMBER
:
   DEC -> type ( DEC )
;

M_Interface_PERIOD
:
   '.' -> type ( PERIOD )
;

M_Interface_PIPE
:
   '|' -> type ( PIPE ) , popMode
;

M_Interface_PRECFONFIGURE
:
   'preconfigure' -> type ( PRECONFIGURE )
;

M_Interface_PREFIX
:
   (
      F_Letter
      (
         F_Letter
         | '-'
         | '_'
      )*
   )
   | 'Dot11Radio'
   | [Ee]'1'
   | [Tt]'1'
;

M_Interface_RELAY
:
   'relay' -> type ( RELAY ) , popMode
;

M_Interface_SLASH
:
   '/' -> type ( FORWARD_SLASH )
;

M_Interface_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_IosRegex;

M_IosRegex_COMMUNITY_SET_REGEX
:
   '\'' ~[':&<> ]* ':' ~[':&<> ]* '\'' -> type ( COMMUNITY_SET_REGEX ) ,
   popMode
;

M_IosRegex_AS_PATH_SET_REGEX
:
   '\'' ~'\''* '\'' -> type ( AS_PATH_SET_REGEX ) , popMode
;

M_IosRegex_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
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
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_MOTD_C;

M_MOTD_C_ESCAPE_C
:
   (
      '^C'
      |
      (
         '^' F_Newline
      )
      | 'cC'
      | '\u0003'
   ) -> type ( ESCAPE_C ) , mode ( DEFAULT_MODE )
;

M_MOTD_C_MOTD
:
   (
      (
         '^' ~[^C\u0003\n\r]
      )
      |
      (
         'c' ~[^C\u0003]
      )
      | ~[c^\u0003]
   )+
;

mode M_MOTD_EOF;

M_MOTD_EOF_EOF
:
   'EOF' -> type ( EOF_LITERAL ) , mode ( DEFAULT_MODE )
;

M_MOTD_EOF_MOTD
:
   (
      ~'E'
      |
      (
         'E' ~'O'
      )
      |
      (
         'EO' ~'F'
      )
   )+
;

mode M_MOTD_HASH;

M_MOTD_HASH_HASH
:
   '#' -> type ( POUND ) , mode ( DEFAULT_MODE )
;

M_MOTD_HASH_MOTD
:
   ~'#'+
;

mode M_Name;

M_Name_NAME
:
   F_NonWhitespace+ -> type ( VARIABLE ) , popMode
;

M_Name_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Name_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_NEIGHBOR;

M_NEIGHBOR_CHANGES
:
   'changes' -> type ( CHANGES ) , popMode
;

M_NEIGHBOR_IP_ADDRESS
:
   F_DecByte '.' F_DecByte '.' F_DecByte '.' F_DecByte -> type ( IP_ADDRESS ) ,
   popMode
;

M_NEIGHBOR_IP_PREFIX
:
   F_DecByte '.' F_DecByte '.' F_DecByte '.' F_DecByte '/' F_Digit F_Digit? ->
   type ( IP_PREFIX ) , popMode
;

M_NEIGHBOR_IPV6_ADDRESS
:
   (
      (
         (
            '::'
            (
               (
                  F_HexDigit+ ':'
               )* F_HexDigit+
            )?
         )
         |
         (
            (
               F_HexDigit+ ':' ':'?
            )+ F_HexDigit*
         )
      )
      (
         ':' F_DecByte '.' F_DecByte '.' F_DecByte '.' F_DecByte
      )?
   ) -> type ( IPV6_ADDRESS ) , popMode
;

M_NEIGHBOR_IPV6_PREFIX
:
   (
      (
         (
            '::'
            (
               (
                  F_HexDigit+ ':'
               )* F_HexDigit+
            )?
         )
         |
         (
            (
               F_HexDigit+ ':' ':'?
            )+ F_HexDigit*
         )
      )
      (
         ':' F_DecByte '.' F_DecByte '.' F_DecByte '.' F_DecByte
      )? '/' F_DecByte
   ) -> type ( IPV6_PREFIX ) , popMode
;

M_NEIGHBOR_NLRI
:
   'nlri' -> type ( NLRI ) , popMode
;

M_NEIGHBOR_PASSIVE
:
   'passive' -> type ( PASSIVE ) , popMode
;

M_Neighbor_VARIABLE
:
   F_Variable_VarChar+ -> type ( VARIABLE ) , popMode
;

M_NEIGHBOR_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_NEIGHBOR_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_ObjectGroup;

M_ObjectGroup_IP
:
  'ip' -> type ( IP ) , popMode
;

M_ObjectGroup_NETWORK
:
  'network' -> type ( NETWORK ) , popMode
;

M_ObjectGroup_PROTOCOL
:
  'protocol' -> type ( PROTOCOL ) , popMode
;

M_ObjectGroup_SERVICE
:
  'service' -> type ( SERVICE ) , popMode
;

M_ObjectGroup_USER
:
  'user' -> type ( USER ) , popMode
;

/* Do not reorder above literals */
M_ObjectGroup_NAME
:
   F_NonWhitespace+ -> type ( VARIABLE ) , popMode
;

M_ObjectGroup_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_ObjectGroup_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_REMARK;

M_REMARK_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_REMARK_REMARK
:
   F_NonNewline+
;

mode M_RouteMap;

M_RouteMap_IN
:
   'in' -> type ( IN )
;

M_RouteMap_OUT
:
   'out' -> type ( OUT )
;

M_RouteMap_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_RouteMap_VARIABLE
:
   F_NonWhitespace+
   {
      if (enableACL_NUM) {
         enableACL_NUM = false;
         enableDEC = true;
      }
      if (enableCOMMUNITY_LIST_NUM) {
         enableCOMMUNITY_LIST_NUM = false;
         enableDEC = true;
      }
   }
   -> type ( VARIABLE ) , popMode
;

M_RouteMap_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Seed;

M_Seed_PASSWORD_SEED
:
   F_NonWhitespace+ -> type ( PASSWORD_SEED ) , popMode
;

mode M_SeedWhitespace;

M_Seed_WS
:
   F_Whitespace+ -> channel ( HIDDEN ) , mode ( M_Seed )
;

mode M_Sha;

M_Sha_DEC_PART
:
   F_Digit+
;

M_Sha_HEX_PART
:
   F_HexDigit+ -> popMode
;

M_Sha_REDACTED
:
   '*'+ -> popMode
;

M_Sha_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Sha_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_SHA1;

M_SHA1_DEC_PART
:
   F_Digit+
;

M_SHA1_HEX_PART
:
   F_HexDigit+ -> popMode
;

M_SHA1_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_SshKey;

M_SshKey_DSA1024
:
   'dsa1024' -> type ( DSA1024 ), mode ( M_CadantSshKey )
;

M_SshKey_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_SshKey_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_VacantMessageText;

M_VacantMessage_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

M_VacantMessage_ESCAPE_C
:
   (
      '^C'
      | '\u0003'
   ) -> type ( ESCAPE_C ) , mode ( M_MOTD_C )
;

M_VacantMessage_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;
