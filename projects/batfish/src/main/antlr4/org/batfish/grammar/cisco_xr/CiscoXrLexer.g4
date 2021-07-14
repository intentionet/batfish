lexer grammar CiscoXrLexer;

options {
   superClass = 'org.batfish.grammar.cisco_xr.parsing.CiscoXrBaseLexer';
}

tokens {
  ADVERTISE_AS_VPN,
  ALLOW_IMPORTED_VPN,
  AS_PATH_REGEX,
  BANNER_DELIMITER_IOS,
  BANNER_BODY,
  COMMUNITY_SET_REGEX,
  CONFIG_SAVE,
  DFA_REGEX,
  DOTDOT,
  HEX_FRAGMENT,
  IOS_REGEX,
  IS_LOCAL,
  ISO_ADDRESS,
  NEIGHBOR_IS,
  ONE_LITERAL,
  ORIGINATES_FROM,
  PARAMETER,
  PAREN_LEFT_LITERAL,
  PAREN_RIGHT_LITERAL,
  PASSES_THROUGH,
  PASSWORD_SEED,
  PEERAS,
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
  UNIQUE_LENGTH,
  VALUE,
  WIRED,
  WISPR,
  WORD
} 

// CiscoXr Keywords

AAA: 'aaa';

AAA_PROFILE: 'aaa-profile';

AAA_SERVER: 'aaa-server';

AAA_USER: 'aaa-user';

ABSOLUTE_TIMEOUT: 'absolute-timeout';

ACAP: 'acap';

ACCEPT: 'accept';

ACCEPT_DIALIN: 'accept-dialin';

ACCEPT_LIFETIME: 'accept-lifetime';

ACCEPT_OWN: 'accept-own';

ACCEPT_RATE: 'accept-rate';

ACCEPT_REGISTER: 'accept-register' -> pushMode(M_Word);

ACCEPT_RP: 'accept-rp';

ACCESS: 'access';

ACCESS_CLASS: 'access-class';

ACCESS_GROUP
:
  'access-group'
  {
    switch(lastTokenType()) {
      case IPV4:
      case IPV6:
        pushMode(M_InterfaceAccessGroup);
        break;
      case NTP:
      case NEWLINE: // need to update if access-group may be first word in other blocks
        switch(accessGroupContextTokenType()) {
          case IGMP:
          case MLD:
            pushMode(M_Word);
            break;
          case NTP:
            pushMode(M_NtpAccessGroup);
            break;
          default:
            break;
        }
        break;
      default:
        break;
    }
  }
;

ACCESS_LIST: 'access-list' -> pushMode(M_Word);

ACCESS_LOG: 'access-log';

ACCESS_MAP: 'access-map';

ACCESS_SESSION: 'access-session';

ACCOUNTING: 'accounting';

ACCOUNTING_LIST: 'accounting-list';

ACCOUNTING_PORT: 'accounting-port';

ACCOUNTING_SERVER_GROUP: 'accounting-server-group';

ACCOUNTING_THRESHOLD: 'accounting-threshold';

ACCT_PORT: 'acct-port';

ACFE: 'acfe';

ACK: 'ack';

ACL: 'acl';

ACL_POLICY: 'acl-policy';

ACLLOG: 'acllog';

ACR_NEMA: 'acr-nema';

ACTION: 'action';

ACTION_TYPE: 'action-type';

ACTIVATE: 'activate';

ACTIVATE_SERVICE_WHITELIST: 'activate-service-whitelist';

ACTIVATED_SERVICE_TEMPLATE: 'activated-service-template';

ACTIVATION_CHARACTER: 'activation-character';

ACTIVE: 'active';

ADD: 'add';

ADD_PATHS: 'add-paths';

ADD_ROUTE: 'add-route';

ADD_VLAN: 'add-vlan';

ADDITIONAL_PATHS: 'additional-paths';

ADDITIVE: 'additive';

ADDRESS: 'address';

ADDRESS_FAMILY: 'address-family';

ADDRESS_HIDING: 'address-hiding';

ADDRESS_POOL: 'address-pool';

ADDRESS_POOLS: 'address-pools';

ADDRESS_RANGE: 'address-range';

ADDRESS_TABLE: 'address-table';

ADDRESS_UNREACHABLE: 'address-unreachable';

ADDRGROUP: 'addrgroup';

ADJACENCY: 'adjacency';

ADJACENCY_CHECK: 'adjacency-check';

ADJACENCY_STALE_TIMER: 'adjacency-stale-timer';

ADJMGR: 'adjmgr';

ADJUST_MSS: 'adjust-mss';

ADMIN: 'admin';

ADMIN_DIST: 'admin-dist';

ADMIN_DISTANCE: 'admin-distance';

ADMIN_STATE: 'admin-state';

ADMIN_VDC: 'admin-vdc';

ADMINISTRATIVE_WEIGHT: 'administrative-weight';

ADMINISTRATIVELY_PROHIBITED: 'administratively-prohibited';

ADMISSION: 'admission';

ADP: 'adp';

ADVERTISE: 'advertise';

ADVERTISE_TO: 'advertise-to' -> pushMode(M_Word);

ADVERTISEMENT_INTERVAL: 'advertisement-interval';

ADVERTISE_INACTIVE: 'advertise-inactive';

AES: 'aes';

AESA: 'aesa';

AF_GROUP: 'af-group';

AF_INTERFACE
:
   'af-interface' -> pushMode ( M_Interface )
;

AF11: 'af11';

AF12: 'af12';

AF13: 'af13';

AF21: 'af21';

AF22: 'af22';

AF23: 'af23';

AF31: 'af31';

AF32: 'af32';

AF33: 'af33';

AF41: 'af41';

AF42: 'af42';

AF43: 'af43';

AFFINITY: 'affinity';

AFFINITY_MAP: 'affinity-map';

AFPOVERTCP: 'afpovertcp';

AGE: 'age';

AGGREGATE: 'aggregate';

AGGREGATE_ADDRESS: 'aggregate-address';

AGING: 'aging';

AH: 'ah';

AH_MD5_HMAC: 'ah-md5-hmac';

AH_SHA_HMAC: 'ah-sha-hmac';

AHP: 'ahp';

AIRGROUP: 'airgroup';

AIRGROUPSERVICE: 'airgroupservice';

ALARM: 'alarm';

ALERT_GROUP: 'alert-group';

ALERTS: 'alerts';

ALG: 'alg';

ALG_BASED_CAC: 'alg-based-cac';

ALIAS
:
   'alias' -> pushMode ( M_Alias )
;

ALL: 'all';

ALL_ALARMS: 'all-alarms';

ALL_OF_ROUTER: 'all-of-router';

ALLOCATE: 'allocate';

ALLOCATION: 'allocation';

ALLOW: 'allow';

ALLOW_CONNECTIONS: 'allow-connections';

ALLOW_DEFAULT: 'allow-default';

ALLOW_FAIL_THROUGH: 'allow-fail-through';

ALLOW_NOPASSWORD_REMOTE_LOGIN: 'allow-nopassword-remote-login';

ALLOW_OVERRIDE: 'allow-override';

ALLOW_RP: 'allow-rp';

ALLOW_SELF_PING: 'allow-self-ping';

ALLOWED: 'allowed';

ALLOWAS_IN: 'allowas-in';

ALTERNATE_ADDRESS: 'alternate-address';

ALTERNATE_AS: 'alternate-as';

ALWAYS: 'always';

ALWAYS_COMPARE_MED: 'always-compare-med';

ALWAYS_ON: 'always-on';

ALWAYS_ON_VPN: 'always-on-vpn';

AM_DISABLE: 'am-disable';

AM_SCAN_PROFILE: 'am-scan-profile';

AMON: 'amon';

AMT: 'amt';

AND: 'and';

ANTENNA: 'antenna';

ANY: 'any';

ANY4: 'any4';

ANY6: 'any6';

ANYCONNECT: 'anyconnect';

ANYCONNECT_ESSENTIALS: 'anyconnect-essentials';

AOL: 'aol';

AP: 'ap';

AP_BLACKLIST_TIME: 'ap-blacklist-time';

AP_CLASSIFICATION_RULE: 'ap-classification-rule';

AP_CRASH_TRANSFER: 'ap-crash-transfer';

AP_GROUP: 'ap-group';

AP_LACP_STRIPING_IP: 'ap-lacp-striping-ip';

AP_NAME: 'ap-name';

AP_RULE_MATCHING: 'ap-rule-matching';

AP_SYSTEM_PROFILE: 'ap-system-profile';

API: 'api';

APP: 'app';

APPLETALK: 'appletalk';

APPLICATION: 'application';

APPLY: 'apply' -> pushMode(M_Word);

AQM_REGISTER_FNF: 'aqm-register-fnf';

ARAP: 'arap';

ARCHIVE: 'archive';

ARCHIVE_LENGTH: 'archive-length';

ARCHIVE_SIZE: 'archive-size';

AREA: 'area';

AREA_PASSWORD: 'area-password';

ARM_PROFILE: 'arm-profile';

ARM_RF_DOMAIN_PROFILE: 'arm-rf-domain-profile';

ARP: 'arp';

ARNS: 'arns';

AS: 'as';

AS_OVERRIDE: 'as-override';

AS_PATH: 'as-path' -> pushMode (M_AsPath);

AS_PATH_SET: 'as-path-set' -> pushMode(M_AsPathSet);

AS_SET: 'as-set';

ASCENDING: 'ascending';

ASCII_AUTHENTICATION: 'ascii-authentication';

ASDM: 'asdm';

ASDM_BUFFER_SIZE: 'asdm-buffer-size';

ASF_RMCP: 'asf-rmcp';

ASIP_WEBADMIN: 'asip-webadmin';

ASN: 'asn';

ASSEMBLER: 'assembler';

ASSIGNMENT: 'assignment';

ASSOC_RETRANSMIT: 'assoc-retransmit';

ASSOCIATE: 'associate';

ASSOCIATION: 'association';

ASYNC: 'async';

ASYNC_BOOTP: 'async-bootp';

ASYNCHRONOUS: 'asynchronous';

ATM: 'atm';

ATTEMPTS: 'attempts';

ATTRIBUTE_DOWNLOAD: 'attribute-download';

ATTRIBUTE_NAMES: 'attribute-names';

ATTRIBUTE_SET: 'attribute-set';

ATTRIBUTES: 'attributes';

AT_RTMP: 'at-rtmp';

AUDIT: 'audit';

AURP: 'aurp';

AUTH: 'auth';

AUTH_FAILURE_BLACKLIST_TIME: 'auth-failure-blacklist-time';

AUTH_PORT: 'auth-port';

AUTH_PROXY: 'auth-proxy';

AUTH_SERVER: 'auth-server';

AUTH_TYPE: 'auth-type';

AUTHENTICATE: 'authenticate';

AUTHENTICATION
:
   'authentication' -> pushMode ( M_Authentication )
;

AUTHENTICATION_DOT1X: 'authentication-dot1x';

AUTHENTICATION_KEY: 'authentication-key';

AUTHENTICATION_MAC: 'authentication-mac';

AUTHENTICATION_PORT: 'authentication-port';

AUTHENTICATION_RESTART: 'authentication-restart';

AUTHENTICATION_RETRIES: 'authentication-retries';

AUTHENTICATION_SERVER: 'authentication-server';

AUTHENTICATION_SERVER_GROUP: 'authentication-server-group';

AUTHORITATIVE: 'authoritative';

AUTHORIZATION: 'authorization';

AUTHORIZATION_REQUIRED: 'authorization-required';

AUTHORIZATION_STATUS: 'authorization-status';

AUTHORIZATION_SERVER_GROUP: 'authorization-server-group';

AUTHORIZE: 'authorize';

AUTHORIZED: 'authorized';

AUTO: 'auto';

AUTO_CERT_ALLOW_ALL: 'auto-cert-allow-all';

AUTO_CERT_ALLOWED_ADDRS: 'auto-cert-allowed-addrs';

AUTO_CERT_PROV: 'auto-cert-prov';

AUTO_CONFIG: 'auto-config';

AUTO_COST: 'auto-cost';

AUTO_DISCARD: 'auto-discard';

AUTO_IMPORT: 'auto-import';

AUTO_LOCAL_ADDR: 'auto-local-addr';

AUTO_RECOVERY: 'auto-recovery';

AUTO_RP: 'auto-rp';

AUTO_SHUTDOWN_NEW_NEIGHBORS: 'auto-shutdown-new-neighbors';

AUTO_SUMMARY: 'auto-summary';

AUTO_SYNC: 'auto-sync';

AUTO_TUNNEL: 'auto-tunnel';

AUTO_UPGRADE: 'auto-upgrade';

AUTOCLASSIFY: 'autoclassify';

AUTOHANGUP: 'autohangup';

AUTONOMOUS_SYSTEM: 'autonomous-system';

AUTORECOVERY: 'autorecovery';

AUTOROUTE: 'autoroute';

AUTOROUTE_EXCLUDE: 'autoroute-exclude';

AUTORP: 'autorp';

AUTOSELECT: 'autoselect';

AUTOSTATE: 'autostate';

AUX: 'aux';

BACK_UP: 'back-up';

BACKBONEFAST: 'backbonefast';

BACKGROUND_ROUTES_ENABLE: 'background-routes-enable';

BACKOFF_TIME: 'backoff-time';

BACKUP: 'backup';

BACKUPCRF: 'backupcrf';

BAND_STEERING: 'band-steering';

BANDWIDTH: 'bandwidth';

BANDWIDTH_CONTRACT: 'bandwidth-contract';

BANDWIDTH_PERCENT: 'bandwidth-percent';

BANDWIDTH_PERCENTAGE: 'bandwidth-percentage';

BANNER: 'banner';

BANNER_IOS
:
  'banner' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BANNER_CONFIG_SAVE_IOS
:
  'banner' F_Whitespace+ 'config-save' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BANNER_EXEC_IOS
:
  'banner' F_Whitespace+ 'exec' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BANNER_INCOMING_IOS
:
  'banner' F_Whitespace+ 'incoming' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BANNER_LOGIN_IOS
:
  'banner' F_Whitespace+ 'login' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BANNER_MOTD_IOS
:
  'banner' F_Whitespace+ 'motd' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BANNER_PROMPT_TIMEOUT_IOS
:
  'banner' F_Whitespace+ 'prompt-timeout' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BANNER_SLIP_PPP_IOS
:
  'banner' F_Whitespace+ 'slip-ppp' F_Whitespace+ -> pushMode(M_BannerIosDelimiter)
;

BASE: 'base';

BASH: 'bash';

BASIC_1_0
:
  'basic-1.0'
;

BASIC_2_0
:
  'basic-2.0'
;

BASIC_5_5
:
  'basic-5.5'
;

BASIC_6_0
:
  'basic-6.0'
;

BASIC_9_0
:
  'basic-9.0'
;

BASIC_11_0
:
  'basic-11.0'
;

BASIC_12_0
:
  'basic-12.0'
;

BASIC_18_0
:
  'basic-18.0'
;

BASIC_24_0
:
  'basic-24.0'
;

BASIC_36_0
:
  'basic-36.0'
;

BASIC_48_0
:
  'basic-48.0'
;

BASIC_54_0
:
  'basic-54.0'
;

BCMC_OPTIMIZATION: 'bcmc-optimization';

BCN_RPT_REQ_PROFILE: 'bcn-rpt-req-profile';

BEACON: 'beacon';

BESTPATH: 'bestpath';

BEYOND_SCOPE: 'beyond-scope';

BFD: 'bfd';

BFD_ECHO: 'bfd-echo';

BFD_ENABLE: 'bfd-enable';

BFD_TEMPLATE: 'bfd-template';

BFTP: 'bftp';

BGMP: 'bgmp';

BGP: 'bgp';

BGP_COMMUNITY: 'bgp-community';

BGP_POLICY: 'bgp-policy';

BIDIR: 'bidir';

BIDIR_ENABLE: 'bidir-enable';

BIDIR_OFFER_INTERVAL: 'bidir-offer-interval';

BIDIR_OFFER_LIMIT: 'bidir-offer-limit';

BIDIR_RP_LIMIT: 'bidir-rp-limit';

BIFF: 'biff';

BIG: 'big';

BIND: 'bind';

BKUP_LMS_IP: 'bkup-lms-ip';

BLACKLIST: 'blacklist';

BLACKLIST_TIME: 'blacklist-time';

BLOCK: 'block';

BLOGGERD: 'bloggerd';

BOOT: 'boot';

BOOT_END_MARKER: 'boot-end-marker';

BOOT_START_MARKER: 'boot-start-marker';

BOOTFILE: 'bootfile';

BOOTP_RELAY: 'bootp-relay';

BOOTP: 'bootp';

BOOTPC: 'bootpc';

BOOTPS: 'bootps';

BORDER: 'border';

BORDER_ROUTER: 'border-router';

BOTH: 'both';

BOUNDARY: 'boundary';

BPDUFILTER: 'bpdufilter';

BPDUGUARD: 'bpduguard';

BREAKOUT: 'breakout';

BRIDGE: 'bridge';

BRIDGE_DOMAIN: 'bridge-domain' -> pushMode(M_Word);

BRIDGE_GROUP: 'bridge-group';

BRIDGE_PRIORITY: 'bridge-priority';

BROADCAST: 'broadcast';

BROADCAST_ADDRESS: 'broadcast-address';

BROADCAST_FILTER: 'broadcast-filter';

BSD_CLIENT: 'bsd-client';

BSD_USERNAME: 'bsd-username';

BSR: 'bsr';

BSR_BORDER: 'bsr-border';

BSR_CANDIDATE: 'bsr-candidate';

BUCKETS: 'buckets';

BUFFER_LIMIT: 'buffer-limit';

BUFFER_SIZE: 'buffer-size';

BUFFERED: 'buffered';

BUILDING_CONFIGURATION
:
   'Building configuration'
;

BUNDLE: 'bundle';

BUFFERS: 'buffers';

BURST_SIZE: 'burst-size';

BYTES: 'bytes';

C_MULTICAST_ROUTING: 'c-multicast-routing';

CA: 'ca';

CABLE: 'cable';

CABLE_RANGE: 'cable-range';

CABLELENGTH
:
   'cablelength' -> pushMode ( M_COMMENT )
;

CACHE: 'cache';

CACHE_SA_HOLDTIME: 'cache-sa-holdtime';

CACHE_SA_STATE: 'cache-sa-state';

CACHE_TIMEOUT: 'cache-timeout';

CALL: 'call';

CALL_BLOCK: 'call-block';

CALL_FORWARD: 'call-forward';

CALL_HOME: 'call-home';

CALL_MANAGER_FALLBACK: 'call-manager-fallback';

CALLER_ID: 'caller-id';

CALLHOME: 'callhome';

CAM_ACL: 'cam-acl';

CAM_PROFILE: 'cam-profile';

CANDIDATE_BSR: 'candidate-bsr';

CANDIDATE_RP
:
  'candidate-rp'
  {
    if (lastTokenType() == AUTO_RP) {
      pushMode(M_Interface);
    }
  }
;

CAPABILITY: 'capability';

CAPTIVE_PORTAL: 'captive-portal';

CAPTIVE_PORTAL_CERT: 'captive-portal-cert';

CAPTURE: 'capture';

CARD: 'card';

CARD_TRAP_INH: 'card-trap-inh';

CARRIER_DELAY: 'carrier-delay';

CAS_CUSTOM: 'cas-custom';

CASE: 'case';

CCM_MANAGER: 'ccm-manager';

CDP: 'cdp';

CDP_URL: 'cdp-url';

CEF: 'cef';

CENTRALIZED_LICENSING_ENABLE: 'centralized-licensing-enable';

CERTIFICATE
:
   'certificate' -> pushMode ( M_Certificate )
;

CFS: 'cfs';

CGMP: 'cgmp';

CHAIN: 'chain';

CHANGES: 'changes';

CHANNEL: 'channel';

CHANNEL_GROUP: 'channel-group';

CHANNEL_PROTOCOL: 'channel-protocol';

CHANNELIZED: 'channelized';

CHAP: 'chap';

CHARGEN: 'chargen';

CHASSIS_ID: 'chassis-id';

CHAT_SCRIPT: 'chat-script';

CHECK: 'check';

CIFS: 'cifs';

CIPC: 'cipc';

CIR: 'cir';

CIRCUIT_TYPE: 'circuit-type';

CISCO: 'cisco';

CISCO_TDP: 'cisco_TDP';

CISP: 'cisp';

CITADEL: 'citadel';

CITRIX_ICA: 'citrix-ica';

CLASS: 'class';

CLASS_DEFAULT: 'class-default';

CLASS_MAP: 'class-map';

CLASSLESS: 'classless';

CLEAR: 'clear';

CLEARCASE: 'clearcase';

CLEAR_SESSION: 'clear-session';

CLI: 'cli';

CLIENT: 'client';

CLIENT_GROUP: 'client-group';

CLIENT_IDENTIFIER: 'client-identifier';

CLIENT_NAME: 'client-name';

CLIENT_TO_CLIENT: 'client-to-client';

CLNS: 'clns';

CLOCK: 'clock';

CLOCK_PERIOD: 'clock-period';

CLONE: 'clone';

CLOSED: 'closed';

CLUSTER: 'cluster';

CLUSTER_ID: 'cluster-id';

CMD: 'cmd';

CMTS: 'cmts';

CNS: 'cns';

COAP: 'coap';

CODEC: 'codec';

COLLECT: 'collect';

COLLECT_STATS: 'collect-stats';

COMMAND
:
   'command' -> pushMode ( M_Command )
;

COMMANDS: 'commands';

COMMERCE: 'commerce';

COMMIT: 'commit';

COMMON: 'common';

COMMON_NAME: 'common-name';

COMMUNITY
:
  'community'
  {
    int ltt = lastTokenType();
    if (ltt == SET) {
      pushMode(M_CommunitySetExpr);
    } else if (ltt == DELETE) {
      pushMode(M_DeleteCommunity);
    }
  }
;

COMMUNITY_MAP
:
   'community-map' -> pushMode ( M_Name )
;

COMMUNITY_SET: 'community-set' -> pushMode(M_CommunitySet);

COMPARE_ROUTERID: 'compare-routerid';

COMPATIBLE: 'compatible';

COMPRESSION_CONNECTIONS: 'compression-connections';

CON: 'con';

CONF_LEVEL_INCR: 'conf-level-incr';

CONFDCONFIG: 'confdConfig';

CONFED: 'confed';

CONFEDERATION: 'confederation';

CONFIG: 'config';

CONFIG_COMMANDS: 'config-commands';

CONFIG_REGISTER: 'config-register';

CONFIGURATION: 'configuration';

CONFIGURE: 'configure';

CONFLICT_POLICY: 'conflict-policy';

CONFORM_ACTION: 'conform-action';

CONGESTION_CONTROL: 'congestion-control';

CONN: 'conn';

CONN_HOLDDOWN: 'conn-holddown';

CONNECT_RETRY: 'connect-retry';

CONNECT_SOURCE: 'connect-source';

CONNECTED: 'connected';

CONNECTION: 'connection';

CONNECTION_REUSE: 'connection-reuse';

CONSOLE: 'console';

CONTACT: 'contact';

CONTACT_EMAIL_ADDR: 'contact-email-addr';

CONTACT_NAME
:
   'contact-name' -> pushMode ( M_Description )
;

CONTENT_TYPE: 'content-type';

CONTEXT: 'context';

CONTEXT_NAME: 'context-name';

CONTRACT_ID: 'contract-id';

CONTROL: 'control';

CONTROL_APPS_USE_MGMT_PORT: 'control-apps-use-mgmt-port';

CONTROL_DIRECTION: 'control-direction';

CONTROL_PLANE: 'control-plane';

CONTROL_PLANE_SECURITY: 'control-plane-security';

CONTROL_WORD: 'control-word';

CONTROLLER
:
   'controller' -> pushMode ( M_Interface )
;

CONVERGENCE: 'convergence';

CONVERGENCE_TIMEOUT: 'convergence-timeout';

CONVERSION_ERROR: 'conversion-error';

CONTROLLER_IP: 'controller-ip';

COOKIE: 'cookie';

COPP: 'copp';

COPS: 'cops';

COPY: 'copy';

CORE_TREE_PROTOCOL: 'core-tree-protocol';

COS: 'cos';

COS_MAPPING: 'cos-mapping';

COS_QUEUE_GROUP: 'cos-queue-group';

COST: 'cost';

COUNTRY: 'country';

COUNTRY_CODE: 'country-code';

COUNTER: 'counter';

COUNTERS: 'counters';

COURIER: 'courier';

CPD: 'cpd';

CPTONE: 'cptone';

CPU_SHARE: 'cpu-share';

CRC: 'crc';

CREDENTIALS: 'credentials';

CRITICAL: 'critical';

CRYPTO: 'crypto';

CRYPTOCHECKSUM: 'Cryptochecksum';

CRYPTO_LOCAL: 'crypto-local';

CRYPTOGRAPHIC_ALGORITHM: 'cryptographic-algorithm';

CRL: 'crl';

CS1: 'cs1';

CS2: 'cs2';

CS3: 'cs3';

CS4: 'cs4';

CS5: 'cs5';

CS6: 'cs6';

CS7: 'cs7';

CSD: 'csd';

CSNP_INTERVAL: 'csnp-interval';

CSNET_NS: 'csnet-ns';

CSR_PARAMS: 'csr-params';

CTIQBE: 'ctiqbe';

CTL_FILE: 'ctl-file';

CTS: 'cts';

CURRENT_CONFIGURATION
:
   'Current configuration'
;

CUSTOM: 'custom';

CUSTOMER_ID: 'customer-id';

DAEMON: 'daemon';

DAMPEN: 'dampen';

DAMPEN_IGP_METRIC: 'dampen-igp-metric';

DAMPENING: 'dampening';

DAMPENING_CHANGE: 'dampening-change';

DAMPENING_INTERVAL: 'dampening-interval';

DATA: 'data';

DATA_PRIVACY: 'data-privacy';

DATABASE: 'database';

DATABITS: 'databits';

DAYTIME: 'daytime';

DBL: 'dbl';

DCB: 'dcb';

DCB_BUFFER_THRESHOLD: 'dcb-buffer-threshold';

DCB_POLICY: 'dcb-policy';

DCBX: 'dcbx';

DCE_MODE: 'dce-mode';

DEACTIVATE: 'deactivate';

DEAD_INTERVAL: 'dead-interval';

DEADTIME: 'deadtime';

DEBUG: 'debug';

DEBUG_TRACE: 'debug-trace';

DEBUGGING: 'debugging';

DECAP_GROUP: 'decap-group';

DECREMENT: 'decrement';

DEFAULT: 'default';

DEFAULT_ACTION: 'default-action';

DEFAULT_COST: 'default-cost';

DEFAULT_DESTINATION: 'default-destination';

DEFAULT_DOMAIN: 'default-domain';

DEFAULT_GATEWAY: 'default-gateway';

DEFAULT_GROUP_POLICY: 'default-group-policy';

DEFAULT_GUEST_ROLE: 'default-guest-role';

DEFAULT_GW: 'default-gw';

DEFAULT_INFORMATION: 'default-information';

DEFAULT_INFORMATION_ORIGINATE: 'default-information-originate';

DEFAULT_INSPECTION_TRAFFIC: 'default-inspection-traffic';

DEFAULT_MAX_FRAME_SIZE: 'default-max-frame-size';

DEFAULT_METRIC: 'default-metric';

DEFAULT_NETWORK: 'default-network';

DEFAULT_ORIGINATE: 'default-originate';

DEFAULT_PEER: 'default-peer';

DEFAULT_ROLE: 'default-role';

DEFAULT_ROUTER: 'default-router';

DEFAULT_ROUTE_TAG: 'default-route-tag';

DEFAULT_TASKGROUP: 'default-taskgroup';

DEFAULT_VALUE: 'default-value';

DEFAULT_VRF: 'default-vrf';

DEFINITION: 'definition';

DEL: 'Del';

DELIMITER: 'delimiter';

DELAY: 'delay';

DELAY_START: 'delay-start';

DELETE: 'delete';

DELETE_DYNAMIC_LEARN: 'delete-dynamic-learn';

DEMAND_CIRCUIT: 'demand-circuit';

DENSE_MODE: 'dense-mode';

DENY: 'deny';

DENY_INTER_USER_TRAFFIC: 'deny-inter-user-traffic';

DEPLOY: 'deploy';

DERIVATION_RULES: 'derivation-rules';

DES: 'des';

DESCENDING: 'descending';

DESCRIPTION
:
   'description' -> pushMode ( M_Description )
;

DESIRABLE: 'desirable';

DEST_IP: 'dest-ip';

DESTINATION: 'destination';

DESTINATION_PATTERN: 'destination-pattern';

DESTINATION_PROFILE: 'destination-profile';

DESTINATION_SLOT: 'destination-slot';

DESTINATION_UNREACHABLE: 'destination-unreachable';

DESTINATION_VRF: 'destination-vrf';

DETAIL: 'detail';

DETECT_ADHOC_NETWORK: 'detect-adhoc-network';

DETECT_AP_FLOOD: 'detect-ap-flood';

DETECT_AP_IMPERSONATION: 'detect-ap-impersonation';

DETECT_BAD_WEP: 'detect-bad-wep';

DETECT_BEACON_WRONG_CHANNEL: 'detect-beacon-wrong-channel';

DETECT_CHOPCHOP_ATTACK: 'detect-chopchop-attack';

DETECT_CLIENT_FLOOD: 'detect-client-flood';

DETECT_CTS_RATE_ANOMALY: 'detect-cts-rate-anomaly';

DETECT_EAP_RATE_ANOMALY: 'detect-eap-rate-anomaly';

DETECT_HOTSPOTTER: 'detect-hotspotter';

DETECT_HT_40MHZ_INTOLERANCE: 'detect-ht-40mhz-intolerance';

DETECT_HT_GREENFIELD: 'detect-ht-greenfield';

DETECT_INVALID_ADDRESS_COMBINATION: 'detect-invalid-address-combination';

DETECT_INVALID_MAC_OUI: 'detect-invalid-mac-oui';

DETECT_MALFORMED_ASSOCIATION_REQUEST: 'detect-malformed-association-request';

DETECT_MALFORMED_AUTH_FRAME: 'detect-malformed-auth-frame';

DETECT_MALFORMED_HTIE: 'detect-malformed-htie';

DETECT_MALFORMED_LARGE_DURATION: 'detect-malformed-large-duration';

DETECT_MISCONFIGURED_AP: 'detect-misconfigured-ap';

DETECT_OVERFLOW_EAPOL_KEY: 'detect-overflow-eapol-key';

DETECT_OVERFLOW_IE: 'detect-overflow-ie';

DETECT_RATE_ANOMALIES: 'detect-rate-anomalies';

DETECT_RTS_RATE_ANOMALY: 'detect-rts-rate-anomaly';

DETECT_TKIP_REPLAY_ATTACK: 'detect-tkip-replay-attack';

DETECT_VALID_SSID_MISUSE: 'detect-valid-ssid-misuse';

DETECT_WIRELESS_BRIDGE: 'detect-wireless-bridge';

DETECT_WIRELESS_HOSTED_NETWORK: 'detect-wireless-hosted-network';

DETERMINISTIC_MED: 'deterministic-med';

DEV
:
   'dev' -> pushMode ( M_Interface )
;

DEVICE: 'device';

DEVICE_ID: 'device-id';

DEVICE_SENSOR: 'device-sensor';

DISABLE_CONNECTED_CHECK: 'disable-connected-check';

DISCRIMINATOR: 'discriminator';

DISPLAY: 'display';

DISPLAY_LOCATION: 'display-location';

DISPUTE: 'dispute';

DF: 'df';

DF_BIT: 'df-bit';

DFS: 'dfs';

DHCP: 'dhcp';

DHCP_FAILOVER2: 'dhcp-failover2';

DHCP_GIADDR: 'dhcp-giaddr';

DHCPD: 'dhcpd';

DHCPRELAY: 'dhcprelay';

DHCPV6_CLIENT: 'dhcpv6-client';

DHCPV6_SERVER: 'dhcpv6-server';

DIAGNOSTIC: 'diagnostic';

DIAGNOSTIC_SIGNATURE: 'diagnostic-signature';

DIAL_CONTROL_MIB: 'dial-control-mib';

DIAL_PEER: 'dial-peer';

DIAL_STRING: 'dial-string';

DIALER: 'dialer';

DIALER_GROUP: 'dialer-group';

DIALER_LIST: 'dialer-list';

DIALPLAN_PATTERN: 'dialplan-pattern';

DIALPLAN_PROFILE: 'dialplan-profile';

DIRECT: 'direct';

DIRECT_INWARD_DIAL: 'direct-inward-dial';

DIRECTED_BROADCAST: 'directed-broadcast';

DIRECTED_REQUEST: 'directed-request';

DIRECTORY: 'directory';

DISABLE: 'disable';

DISABLE_ADVERTISEMENT: 'disable-advertisement';

DISCARD: 'discard';

DISCARD_ROUTE: 'discard-route';

DISCONNECT: 'disconnect';

DISCOVERED_AP_CNT: 'discovered-ap-cnt';

DISCOVERY: 'discovery';

DISTANCE: 'distance';

DISTRIBUTE: 'distribute';

DISTRIBUTE_LIST: 'distribute-list' -> pushMode(M_DistributeList);

DISTRIBUTION: 'distribution';

DM_FALLBACK: 'dm-fallback';

DNS: 'dns';

DNS_DOMAIN: 'dns-domain';

DNS_GUARD: 'dns-guard';

DNS_SERVER: 'dns-server';

DNSIX: 'dnsix';

DO: 'do';

DO_ALL: 'do-all';

DO_UNTIL_FAILURE: 'do-until-failure';

DO_UNTIL_SUCCESS: 'do-until-success';

DOD_HOST_PROHIBITED: 'dod-host-prohibited';

DOD_NET_PROHIBITED: 'dod-net-prohibited';

DOMAIN: 'domain';

DOMAIN_ID: 'domain-id';

DOMAIN_LIST: 'domain-list';

DOMAIN_LOOKUP: 'domain-lookup';

DOMAIN_NAME: 'domain-name';

DONE: 'done';

DONT_CAPABILITY_NEGOTIATE: 'dont-capability-negotiate';

DOS_PROFILE: 'dos-profile';

DOT11: 'dot11';

DOT11A_RADIO_PROFILE: 'dot11a-radio-profile';

DOT11G_RADIO_PROFILE: 'dot11g-radio-profile';

DOT11K_PROFILE: 'dot11k-profile';

DOT11R_PROFILE: 'dot11r-profile';

DOT1Q
:
   'dot1' [Qq]
;

DOT1Q_TUNNEL: 'dot1q-tunnel';

DOT1X: 'dot1x';

DOT1X_DEFAULT_ROLE: 'dot1x-default-role';

DOT1X_ENABLE: 'dot1x-enable';

DOT1X_SERVER_GROUP: 'dot1x-server-group';

DOWNLINK: 'downlink';

DOWNSTREAM: 'downstream';

DPG: 'dpg';

DR_PRIORITY: 'dr-priority';

DROP: 'drop';

DS_HELLO_INTERVAL: 'ds-hello-interval';

DS0_GROUP: 'ds0-group';

DSCP: 'dscp';

DSCP_VALUE: 'dscp-value';

DSG: 'dsg';

DSL: 'dsl';

DSP: 'dsp';

DSPFARM: 'dspfarm';

DSS: 'dss';

DSU: 'dsu';

DTMF_RELAY: 'dtmf-relay';

DTP: 'dtp';

DUAL_ACTIVE: 'dual-active';

DUAL_MODE_DEFAULT_VLAN: 'dual-mode-default-vlan';

DUPLEX: 'duplex';

DUPLICATE_MESSAGE: 'duplicate-message';

DURATION: 'duration';

DVMRP: 'dvmrp';

DYNAMIC: 'dynamic';

DYNAMIC_ACCESS_POLICY_RECORD: 'dynamic-access-policy-record';

DYNAMIC_AUTHOR: 'dynamic-author';

DYNAMIC_CAPABILITY: 'dynamic-capability';

DYNAMIC_EXTENDED: 'dynamic-extended';

DYNAMIC_MAP: 'dynamic-map';

DYNAMIC_MCAST_OPTIMIZATION: 'dynamic-mcast-optimization';

DYNAMIC_MCAST_OPTIMIZATION_THRESH: 'dynamic-mcast-optimization-thresh';

DYNAMIC_TEMPLATE: 'dynamic-template';

E164: 'e164';

E164_PATTERN_MAP: 'e164-pattern-map';

EAP_PASSTHROUGH: 'eap-passthrough';

EAPOL_RATE_OPT: 'eapol-rate-opt';

EARLY_OFFER: 'early-offer';

EBGP: 'ebgp';

EBGP_MULTIHOP: 'ebgp-multihop';

ECHO: 'echo';

ECHO_CANCEL: 'echo-cancel';

ECHO_REPLY: 'echo-reply';

ECHO_REQUEST: 'echo-request';

ECHO_RX_INTERVAL: 'echo-rx-interval';

ECMP: 'ecmp';

ECMP_GROUP: 'ecmp-group';

EDCA_PARAMETERS_PROFILE: 'edca-parameters-profile';

EDGE: 'edge';

EF: 'ef';

EFS: 'efs';

EGP: 'egp';

EGRESS: 'egress';

EGRESS_INTERFACE_SELECTION: 'egress-interface-selection';

EIBGP: 'eibgp';

EIGRP: 'eigrp';

ELSE: 'else';

ELSEIF: 'elseif';

EMAIL: 'email';

EMAIL_ADDR
:
   'email-addr' -> pushMode ( M_Description )
;

EMAIL_CONTACT
:
   'email-contact' -> pushMode ( M_Description )
;

EMBEDDED_RP: 'embedded-rp';

EMERGENCIES: 'emergencies';

EMPTY: 'empty';

ENABLE: 'enable';

ENABLE_ACL_CAM_SHARING: 'enable-acl-cam-sharing';

ENABLE_ACL_COUNTER: 'enable-acl-counter';

ENABLE_QOS_STATISTICS: 'enable-qos-statistics';

ENABLE_WELCOME_PAGE: 'enable-welcome-page';

ENABLED: 'enabled';

ENCAPSULATION: 'encapsulation';

ENCR: 'encr';

ENCRYPTED: 'encrypted';

ENCRYPTED_PASSWORD: 'encrypted-password';

ENCRYPTION: 'encryption';

END: 'end';

ENDIF: 'endif';

END_CLASS_MAP: 'end-class-map';

END_POLICY: 'end-policy';

END_POLICY_MAP: 'end-policy-map';

END_SET: 'end-set';

ENET_LINK_PROFILE: 'enet-link-profile';

ENFORCE_DHCP: 'enforce-dhcp';

ENFORCE_FIRST_AS: 'enforce-first-as';

ENGINE: 'engine';

ENGINEID
:
   (
      'engineid'
      | 'engineID'
   ) -> pushMode ( M_COMMENT )
;

ENROLLMENT: 'enrollment';

ENVIRONMENT: 'environment';

ENVIRONMENT_MONITOR: 'environment-monitor';

EOU: 'eou';

EPHONE_DN_TEMPLATE: 'ephone-dn-template';

EPM: 'epm';

EPP: 'epp';

EQ: 'eq';

ERRDISABLE: 'errdisable';

ERROR: 'error';

ERROR_ENABLE: 'error-enable';

ERROR_RATE_THRESHOLD: 'error-rate-threshold';

ERROR_RECOVERY: 'error-recovery';

ERROR_PASSTHRU: 'error-passthru';

ERRORS: 'errors';

ERSPAN_ID: 'erspan-id';

ESCAPE_CHARACTER: 'escape-character';

ESM: 'esm';

ESP: 'esp';

ESP_3DES: 'esp-3des';

ESP_AES: 'esp-aes';

ESP_DES: 'esp-des';

ESP_GCM: 'esp-gcm';

ESP_GMAC: 'esp-gmac';

ESP_MD5_HMAC: 'esp-md5-hmac';

ESP_NULL: 'esp-null';

ESP_SEAL: 'esp-seal';

ESP_SHA_HMAC: 'esp-sha-hmac';

ESP_SHA256_HMAC: 'esp-sha256-hmac';

ESP_SHA512_HMAC: 'esp-sha512-hmac';

ESRO_GEN: 'esro-gen';

ESSID: 'essid';

ESTABLISHED: 'established';

ETH: 'eth';

ETHERCHANNEL: 'etherchannel';

ETHERNET: 'ethernet';

ETHERNET_SERVICES: 'ethernet-services';

ETYPE: 'etype';

EUI_64: 'eui-64';

EVALUATE: 'evaluate';

EVENT: 'event';

EVENT_HANDLER: 'event-handler';

EVENT_HISTORY: 'event-history';

EVENT_LOG_SIZE: 'event-log-size';

EVENT_THRESHOLDS_PROFILE: 'event-thresholds-profile';

EVENTS: 'events';

EVPN: 'evpn';

EXACT: 'exact';

EXCEED_ACTION: 'exceed-action';

EXCEPT: 'except';

EXCEPTION: 'exception';

EXCEPTION_SLAVE: 'exception-slave';

EXCLUDE: 'exclude';

EXCLUDED_ADDRESS: 'excluded-address';

EXEC: 'exec';

EXEC_TIMEOUT: 'exec-timeout';

EXECUTE
:
   'execute' -> pushMode ( M_Execute )
;

EXIT: 'exit';

EXIT_ADDRESS_FAMILY: 'exit-address-family';

EXIT_AF_INTERFACE: 'exit-af-interface';

EXIT_AF_TOPOLOGY: 'exit-af-topology';

EXIT_PEER_POLICY: 'exit-peer-policy';

EXIT_PEER_SESSION: 'exit-peer-session';

EXIT_SERVICE_FAMILY: 'exit-service-family';

EXIT_SF_INTERFACE: 'exit-sf-interface';

EXIT_SF_TOPOLOGY: 'exit-sf-topology';

EXIT_VRF: 'exit-vrf';

EXPECT: 'expect';

EXPLICIT_NULL: 'explicit-null';

EXPLICIT_RPF_VECTOR: 'explicit-rpf-vector';

EXPLICIT_TRACKING: 'explicit-tracking' -> pushMode(M_ExplicitTracking);

EXPORT: 'export';

EXPORT_PROTOCOL: 'export-protocol';

EXPORT_RT: 'export-rt';

EXPORTER: 'exporter' -> pushMode(M_Word);

EXPORTER_MAP: 'exporter-map';

EXPANDED: 'expanded';

EXTCOMMUNITY
:
   'extcommunity'
   {
     if (lastTokenType() == SET) {
       pushMode(M_Extcommunity);
     }
   }
;

EXTCOMMUNITY_SET: 'extcommunity-set';

EXTEND: 'extend';

EXTENDED: 'extended';

EXTENDED_COUNTERS: 'extended-counters';

EXTENDED_DELAY: 'extended-delay';

EXTENDED_SHOW_WIDTH: 'extended-show-width';

EXTERNAL: 'external';

EXTERNAL_LSA: 'external-lsa';

FABRIC: 'fabric';

FABRIC_MODE: 'fabric-mode';

FABRICPATH: 'fabricpath';

FACILITY: 'facility';

FACILITY_ALARM: 'facility-alarm';

FAIL_MESSAGE: 'fail-message';

FAILED: 'failed';

FAILOVER: 'failover';

FAILURE: 'failure';

FAIL_OVER: 'fail-over';

FAIR_QUEUE: 'fair-queue';

FALL_OVER: 'fall-over';

FALLBACK: 'fallback';

FALLBACK_DN: 'fallback-dn';

FAN: 'fan';

FAST_AGE: 'fast-age';

FAST_DETECT: 'fast-detect';

FAST_EXTERNAL_FALLOVER: 'fast-external-fallover';

FAST_FLOOD: 'fast-flood';

FAST_REROUTE: 'fast-reroute';

FAX: 'fax';

FCOE: 'fcoe';

FEATURE_SET: 'feature-set';

FEX: 'fex';

FEX_FABRIC: 'fex-fabric';

FIELDS: 'fields';

FILE: 'file';

FILE_BROWSING: 'file-browsing';

FILE_ENTRY: 'file-entry';

FILE_SIZE: 'file-size';

FILE_TRANSFER: 'file-transfer';

FILTER: 'filter';

FILTER_LIST: 'filter-list';

FIREWALL: 'firewall';

FIREWALL_VISIBILITY: 'firewall-visibility';

FIN: 'fin';

FINGER: 'finger';

FIRMWARE: 'firmware';

FLAP_LIST: 'flap-list';

FLOATING_CONN: 'floating-conn';

FLOW: 'flow' -> pushMode(M_Flow);

FLOW_AGGREGATION: 'flow-aggregation';

FLOW_CAPTURE: 'flow-capture';

FLOW_CACHE: 'flow-cache';

FLOW_CONTROL: 'flow-control';

FLOW_EXPORT: 'flow-export';

FLOW_SAMPLING_MODE: 'flow-sampling-mode';

FLOW_SAMPLER: 'flow-sampler';

FLOW_SAMPLER_MAP: 'flow-sampler-map';

FLOW_TOP_TALKERS: 'flow-top-talkers';

FLOWCONTROL: 'flowcontrol';

FLUSH_AT_ACTIVATION: 'flush-at-activation';

FLUSH_R1_ON_NEW_R0: 'flush-r1-on-new-r0';

FORCE: 'force';

FOR: 'for';

FORMAT: 'format';

FORWARD: 'forward';

FORWARD_DIGITS: 'forward-digits';

FORWARD_PROTOCOL: 'forward-protocol';

FORWARDER: 'forwarder';

FORWARDING: 'forwarding';

FORWARDING_LATENCY: 'forwarding-latency';

FPD: 'fpd';

FQDN: 'fqdn';

FLOWSPEC: 'flowspec';

FRAGMENTATION: 'fragmentation';

FRAGMENTS: 'fragments';

FRAME_RELAY: 'frame-relay';

FRAMING: 'framing';

FREE_CHANNEL_INDEX: 'free-channel-index';

FREQUENCY: 'frequency';

FRI: 'Fri';

FROM
:
  'from'
  {
    switch(lastTokenType()) {
      case ACCEPT:
        pushMode(M_Word);
        break;
      default:
        break;
    }
  }
;

FT: 'ft';

FTP: 'ftp';

FTP_DATA: 'ftp-data';

FTP_SERVER: 'ftp-server';

FTPS: 'ftps';

FTPS_DATA: 'ftps-data';

FULL_DUPLEX: 'full-duplex';

FULL_TXT: 'full-txt';

G729: 'g729';

GATEKEEPER: 'gatekeeper';

GATEWAY: 'gateway';

GBPS: 'Gbps';

GDOI: 'gdoi';

GE: 'ge';

GENERAL_PARAMETER_PROBLEM: 'general-parameter-problem';

GENERAL_PROFILE: 'general-profile';

GENERATE: 'generate';

GID: 'gid';

GIG_DEFAULT: 'gig-default';

GLBP: 'glbp';

GLOBAL: 'global';

GLOBALENFORCEPRIV: 'globalEnforcePriv';

GLOBAL_MTU: 'global-mtu';

GLOBAL_PORT_SECURITY: 'global-port-security';

GOPHER: 'gopher';

GODI: 'godi';

GRACEFUL: 'graceful';

GRACEFUL_RESTART: 'graceful-restart';

GRACEFUL_SHUTDOWN: 'graceful-shutdown';

GRACETIME: 'gracetime';

GRANT: 'grant';

GRATUITOUS_ARPS: 'gratuitous-arps';

GRE: 'gre';

GREEN: 'green';

GROUP
:
  'group'
  {
    if (lastTokenType() == BRIDGE) {
      pushMode(M_Word);
    }
  }
;

GROUP_ALIAS: 'group-alias';

GROUP_LIST: 'group-list' -> pushMode(M_Word);

GROUP_LOCK: 'group-lock';

GROUP_OBJECT: 'group-object';

GROUP_POLICY: 'group-policy';

GROUP_RANGE: 'group-range';

GROUP_TIMEOUT: 'group-timeout';

GROUP_URL: 'group-url';

GROUP1: 'group1';

GROUP14: 'group14';

GROUP15: 'group15';

GROUP16: 'group16';

GROUP19: 'group19';

GROUP2: 'group2';

GROUP20: 'group20';

GROUP21: 'group21';

GROUP24: 'group24';

GROUP5: 'group5';

GROUPS: 'groups';

GROUPS_PER_INTERFACE: 'groups-per-interface' -> pushMode(M_GroupsPerInterface);

GT: 'gt';

GTP_C: 'gtp-c';

GTP_PRIME: 'gtp-prime';

GTP_U: 'gtp-u';

GUARANTEED: 'guaranteed';

GUARD: 'guard';

GUEST_ACCESS_EMAIL: 'guest-access-email';

GUEST_LOGON: 'guest-logon';

GUEST_MODE: 'guest-mode';

GW_TYPE_PREFIX: 'gw-type-prefix';

H225: 'h225';

H323: 'h323';

H323_GATEWAY: 'h323-gateway';

HA_CLUSTER: 'ha-cluster';

HA_POLICY: 'ha-policy';

HALF_CLOSED: 'half-closed';

HALF_DUPLEX: 'half-duplex';

HANDOVER_TRIGGER_PROFILE: 'handover-trigger-profile';

HARDWARE: 'hardware';

HARDWARE_ADDRESS: 'hardware-address';

HARDWARE_COUNT: 'hardware-count';

HASH: 'hash';

HASH_ALGORITHM: 'hash-algorithm';

HEADER_COMPRESSION: 'header-compression';

HEADER_PASSING: 'header-passing';

HEARTBEAT_INTERVAL: 'heartbeat-interval';

HEARTBEAT_TIME: 'heartbeat-time';

HELLO: 'hello';

HELLO_ADJACENCY: 'hello-adjacency';

HELLO_INTERVAL: 'hello-interval';

HELLO_MULTIPLIER: 'hello-multiplier';

HELLO_PADDING: 'hello-padding';

HELLO_PASSWORD: 'hello-password';

HELPER_ADDRESS: 'helper-address';

HEX_KEY: 'hex-key';

HIDDEN_LITERAL: 'hidden';

HIDDEN_SHARES: 'hidden-shares';

HIDEKEYS: 'hidekeys';

HIGH_AVAILABILITY: 'high-availability';

HIGH_RESOLUTION: 'high-resolution';

HIGHEST_IP: 'highest-ip';

HISTORY: 'history';

HOLD_TIME: 'hold-time';

HOLD_QUEUE: 'hold-queue';

HOLDTIME: 'holdtime';

HOMEDIR: 'homedir' -> pushMode(M_Word);

HOP_LIMIT: 'hop-limit';

HOPLIMIT: 'hoplimit';

HOPS_OF_STATISTICS_KEPT: 'hops-of-statistics-kept';

HOST: 'host';

HOST_ASSOCIATION: 'host-association';

HOST_INFO: 'host-info';

HOST_ISOLATED: 'host-isolated';

HOST_PRECEDENCE_UNREACHABLE: 'host-precedence-unreachable';

HOST_PROXY: 'host-proxy';

HOST_REDIRECT: 'host-redirect';

HOST_ROUTING: 'host-routing';

HOST_TOS_REDIRECT: 'host-tos-redirect';

HOST_TOS_UNREACHABLE: 'host-tos-unreachable';

HOST_UNKNOWN: 'host-unknown';

HOST_UNREACHABLE: 'host-unreachable';

HOSTNAME: 'hostname' -> pushMode(M_Hostname);

HOSTNAMEPREFIX: 'hostnameprefix';

HOTSPOT: 'hotspot';

HP_ALARM_MGR: 'hp-alarm-mgr';

HPM: 'hpm';

HSRP: 'hsrp';

HT_SSID_PROFILE: 'ht-ssid-profile';

HTTP: 'http';

HTTP_ALT: 'http-alt';

HTTP_COMMANDS: 'http-commands';

HTTP_MGMT: 'http-mgmt';

HTTP_RPC_EPMAP: 'http-tpc-epmap';

HTTPS: 'https';

HUNT: 'hunt';

HW_MODULE: 'hw-module';

HW_SWITCH: 'hw-switch';

IBGP: 'ibgp';

ICMP: 'icmp';

ICMP_ECHO: 'icmp-echo';

ICMP_ERROR: 'icmp-error';

ICMP_ERRORS: 'icmp-errors';

ICMP_OBJECT: 'icmp-object';

ICMP_OFF: 'icmp-off';

ICMP_TYPE: 'icmp-type';

ICMP6: 'icmp6';

ICMPV6: 'icmpv6';

ID: 'id';

ID_MISMATCH: 'id-mismatch';

ID_RANDOMIZATION: 'id-randomization';

IDEAL_COVERAGE_INDEX: 'ideal-coverage-index';

IDENT: 'ident';

IDENTITY: 'identity';

IDLE: 'idle';

IDLE_TIMEOUT: 'idle-timeout';

IDP_CERT: 'idp-cert';

IDP_SYNC_UPDATE: 'idp-sync-update';

IDS: 'ids';

IDS_PROFILE: 'ids-profile';

IEC: 'iec';

IEEE_MMS_SSL: 'ieee-mms-ssl';

IETF: 'ietf';

IETF_FORMAT: 'ietf-format';

IF: 'if';

IFACL: 'ifacl';

IFDESCR: 'ifdescr';

IF_NEEDED: 'if-needed';

IFINDEX: 'ifindex';

IFMAP: 'ifmap';

IFMIB: 'ifmib';

IGMP: 'igmp' { setAccessGroupContextTokenType(IGMP); };

IGP_COST: 'igp-cost';

IGP_INTACT: 'igp-intact';

IGRP: 'igrp';

IGNORE: 'ignore';

IGNORE_ATTACHED_BIT: 'ignore-attached-bit';

IGP: 'igp';

IKE: 'ike';

IKEV1: 'ikev1';

IKEV2: 'ikev2';

IKEV2_PROFILE: 'ikev2-profile';

IMAP: 'imap';

IMAP3: 'imap3';

IMAP4: 'imap4';

IMAPS: 'imaps';

IMPERSONATION_PROFILE: 'impersonation-profile';

IMPLICIT_USER: 'implicit-user';

IMPORT: 'import';

IMPORT_RT: 'import-rt';

IN
:
  'in'
  {
    if (lastTokenType() == RD) {
      pushMode(M_RdSetMatchExpr);
    }
  }
;

INACTIVITY_TIMER: 'inactivity-timer';

INBAND: 'inband';

INCLUDE: 'include';

INCLUDE_STUB: 'include-stub';

INCOMING: 'incoming';

INCOMPLETE: 'incomplete';

INFINITY: 'infinity';

INFORM: 'inform';

INFORMATION: 'information';

INFORMATION_REPLY: 'information-reply';

INFORMATION_REQUEST: 'information-request';

INFORMATIONAL: 'informational';

INFORMS: 'informs';

INGRESS: 'ingress';

INHERIT: 'inherit';

INHERITANCE_DISABLE: 'inheritance-disable';

INIT: 'init';

INIT_STRING: 'init-string';

INITIAL_ROLE: 'initial-role';

INPUT: 'input';

INSERVICE: 'inservice';

INSIDE: 'inside';

INSPECT: 'inspect';

INSTALL: 'install';

INSTANCE: 'instance';

INTEGRITY: 'integrity';

INTERAREA: 'interarea';

INTERFACE
:
   'int' 'erface'?
   {
     if (lastTokenType() == NEWLINE || lastTokenType() == ROUTED || lastTokenType() == -1) {
       pushMode(M_Interface);
     }
   }
;

INTERFACE_INHERITANCE: 'interface-inheritance';

INTERFACE_STATISTICS: 'interface-statistics';

INTERNAL: 'internal';

INTERNET: 'internet';

INTERVAL: 'interval';

INTERWORKING: 'interworking';

INVALID: 'invalid';

INVALID_SPI_RECOVERY: 'invalid-spi-recovery';

INVALID_USERNAME_LOG: 'invalid-username-log';

INVERT: 'invert';

IP: 'ip';

IPADDRESS: 'ipaddress';

IP_ADDRESS_LITERAL: 'ip-address';

IP_FLOW_EXPORT_PROFILE: 'ip-flow-export-profile';

IPC: 'ipc';

IPENACL: 'ipenacl';

IPHC_FORMAT: 'iphc-format';

IPINIP: 'ipinip';

IPP: 'ipp';

IPSEC: 'ipsec';

IPSEC_ISAKMP: 'ipsec-isakmp';

IPSEC_MANUAL: 'ipsec-manual';

IPSEC_OVER_TCP: 'ipsec-over-tcp';

IPSEC_PROPOSAL: 'ipsec-proposal';

IPSEC_UDP: 'ipsec-udp';

IPSLA: 'ipsla';

IPV4
:
   [iI] [pP] [vV] '4'
;

IPV4_L5: 'ipv4-l5';

IPV6
:
   [iI] [pP] [vV] '6'
;

IPV6_ADDRESS_POOL: 'ipv6-address-pool';

IPV6IP: 'ipv6ip';

IPX: 'ipx';

IRC: 'irc';

IRDP: 'irdp';

IRIS_BEEP: 'iris-beep';

ISAKMP: 'isakmp';

ISAKMP_PROFILE: 'isakmp-profile';

ISDN: 'isdn';

IS: 'is';

IS_TYPE: 'is-type';

ISCSI: 'iscsi';

ISI_GL: 'isi-gl';

ISIS: 'isis';

ISIS_METRIC: 'isis-metric';

ISL: 'isl';

ISO_TSAP: 'iso-tsap';

ISOLATE: 'isolate';

ISOLATION: 'isolation';

ISPF: 'ispf';

ISSUER_NAME: 'issuer-name';

JOIN_GROUP: 'join-group';

JOIN_PRUNE_INTERVAL: 'join-prune-interval';

JOIN_PRUNE_MTU: 'join-prune-mtu';

JUMBO: 'jumbo';

JUMBOMTU: 'jumbomtu';

KBPS: 'kbps';

KBYTES: 'kbytes';

KEEPALIVE: 'keepalive';

KEEPALIVE_ENABLE: 'keepalive-enable';

KEEPOUT: 'keepout';

KERBEROS: 'kerberos';

KERBEROS_ADM: 'kerberos-adm';

KERNEL: 'kernel';

KEY: 'key';

KEY_CHAIN: 'key-chain';

KEY_EXCHANGE: 'key-exchange';

KEY_HASH: 'key-hash';

KEY_SOURCE: 'key-source';

KEY_STRING: 'key-string';

KEYED_SHA1
:
   [kK][eE][yY][eE][dD]'-'[sS][hH][aA]'1'
;

KEYID: 'keyid';

KEYPAIR: 'keypair';

KEYPATH: 'keypath';

KEYRING: 'keyring';

KEYSTORE: 'keystore';

KLOGIN: 'klogin';

KOD: 'kod';

KPASSWD: 'kpasswd';

KRB5: 'krb5';

KRB5_TELNET: 'krb5-telnet';

KRON: 'kron';

KSHELL: 'kshell';

L2: 'l2';

L2_FILTER: 'l2-filter';

L2_SRC: 'l2-src';

L2PROTOCOL: 'l2protocol';

L2PROTOCOL_TUNNEL: 'l2protocol-tunnel';

L2TP: 'l2tp';

L2TP_CLASS: 'l2tp-class';

L2TRANSPORT: 'l2transport';

L2VPN: 'l2vpn';

LABEL: 'label';

LA_MAINT: 'la-maint';

LABELED_UNICAST: 'labeled-unicast';

LACP: 'lacp';

LACP_TIMEOUT: 'lacp-timeout';

LAG: 'lag';

LAN: 'lan';

LANE: 'lane';

LANZ: 'lanz';

LAPB: 'lapb';

LAST_MEMBER_QUERY_COUNT: 'last-member-query-count';

LAST_MEMBER_QUERY_INTERVAL: 'last-member-query-interval';

LAST_MEMBER_QUERY_RESPONSE_TIME: 'last-member-query-response-time';

LCD_MENU: 'lcd-menu';

LDAP: 'ldap';

LDAPS: 'ldaps';

LDP: 'ldp';

LE: 'le';

LEASE: 'lease';

LEVEL: 'level';

LEVEL_1: 'level-1';

LEVEL_1_2: 'level-1-2';

LEVEL_2: 'level-2';

LEVEL_2_ONLY: 'level-2-only';

LDAP_BASE_DN: 'ldap-base-dn';

LDAP_LOGIN: 'ldap-login';

LDAP_LOGIN_DN: 'ldap-login-dn';

LDAP_NAMING_ATTRIBUTE: 'ldap-naming-attribute';

LDAP_SCOPE: 'ldap-scope';

LENGTH: 'length';

LICENSE: 'license';

LIFE: 'life';

LIFETIME: 'lifetime';

LIMIT: 'limit';

LIMIT_DN: 'limit-dn';

LIMIT_RESOURCE: 'limit-resource';

LINE: 'line';

LINE_PROTOCOL: 'line-protocol';

LINECARD: 'linecard';

LINECARD_GROUP: 'linecard-group';

LINECODE: 'linecode';

LINK: 'link';

LINK_FAIL: 'link-fail';

LINK_FAULT_SIGNALING: 'link-fault-signaling';

LINK_LOCAL: 'link-local';

LINK_STATUS: 'link-status';

LINK_TYPE: 'link-type';

LINKSEC: 'linksec';

LINKDEBOUNCE: 'linkdebounce';

LIST: 'list' -> pushMode(M_Word);

LISTEN: 'listen';

LISTEN_PORT: 'listen-port';

LISTENER: 'listener';

LLDP: 'lldp';

LMP: 'lmp';

LMS_IP: 'lms-ip';

LMS_PREEMPTION: 'lms-preemption';

LOAD_BALANCE: 'load-balance';

LOAD_BALANCING: 'load-balancing';

LOAD_INTERVAL: 'load-interval';

LOAD_SHARING: 'load-sharing';

LOCAL: 'local';

LOCALITY: 'locality';

LOCAL_ADDRESS: 'local-address';

LOCAL_AS: F_LocalAs;

LOCAL_CASE: 'local-case';

LOCAL_INTERFACE: 'local-interface';

LOCAL_IP: 'local-ip';

LOCAL_PORT: 'local-port';

LOCAL_PREFERENCE: 'local-preference';

LOCAL_V6_ADDR: 'local-v6-addr';

LOCAL_VOLATILE: 'local-volatile';

LOCATION
:
   'location' -> pushMode ( M_COMMENT )
;

LOCALE: 'locale';

LOCALIP: 'localip';

LOG: 'log';

LOG_ADJ_CHANGES: 'log-adj-changes';

LOG_ADJACENCY_CHANGES: 'log-adjacency-changes';

LOG_ENABLE: 'log-enable';

LOG_INPUT: 'log-input';

LOG_INTERNAL_SYNC: 'log-internal-sync';

LOG_NEIGHBOR_CHANGES: 'log-neighbor-changes';

LOG_NEIGHBOR_WARNINGS: 'log-neighbor-warnings';

LOG_TRAPS: 'log-traps';

LOGFILE: 'logfile';

LOGGING: 'logging';

LOGIN: 'login';

LOGIN_ATTEMPTS: 'login-attempts';

LOGIN_PAGE: 'login-page';

LOGINSESSION: 'loginsession';

LOGOUT_WARNING: 'logout-warning';

LONGEST_PREFIX: 'longest-prefix';

LOOKUP: 'lookup';

LOOPBACK: 'loopback';

LOOPGUARD: 'loopguard';

LOTUSNOTES: 'lotusnotes';

LPD: 'lpd';

LPTS: 'lpts';

LRE: 'lre';

LRQ: 'lrq';

LSP_GEN_INTERVAL: 'lsp-gen-interval';

LSP_INTERVAL: 'lsp-interval';

LSP_PASSWORD: 'lsp-password';

LSP_REFRESH_INTERVAL: 'lsp-refresh-interval';

LT: 'lt';

M0_7: 'm0-7';

M0_DOT
:
  'm0.'
;

M1_DOT
:
  'm1.'
;

M2_DOT
:
  'm2.'
;

M3_DOT
:
  'm3.'
;

M4_DOT
:
  'm4.'
;

M5_DOT
:
  'm5.'
;

M6_DOT
:
  'm6.'
;

M7_DOT
:
  'm7.'
;

M8_DOT
:
  'm8.'
;

M8_15: 'm8-15';

M9_DOT
:
  'm9.'
;

M10_DOT
:
  'm10.'
;

M11_DOT
:
  'm11.'
;

M12_DOT
:
  'm12.'
;

M13_DOT
:
  'm13.'
;

M14_DOT
:
  'm14.'
;

M15_DOT
:
  'm15.'
;

MAB: 'mab';

MAC: 'mac';

MAC_ADDRESS
:
   'mac-address' -> pushMode ( M_COMMENT )
;

MAC_ADDRESS_TABLE: 'mac-address-table';

MAC_DEFAULT_ROLE: 'mac-default-role';

MAC_LEARN: 'mac-learn';

MAC_MOVE: 'mac-move';

MAC_SERVER_GROUP: 'mac-server-group';

MAC_SRVR_ADMIN: 'mac-srvr-admin';

MACHINE_AUTHENTICATION: 'machine-authentication';

MACRO: 'macro';

MAIL_SERVER: 'mail-server';

MAIN_CPU: 'main-cpu';

MAINTENANCE: 'maintenance';

MANAGEMENT: 'management';

MANAGEMENT_ACCESS: 'management-access';

MANAGEMENT_ADDRESS: 'management-address';

MANAGEMENT_ONLY: 'management-only';

MANAGEMENT_PLANE: 'management-plane';

MANAGEMENT_PROFILE: 'management-profile';

MANAGER: 'manager';

MAP: 'map';

MAP_CLASS: 'map-class';

MAP_GROUP: 'map-group';

MAP_LIST: 'map-list';

MAPPING: 'mapping';

MAPPING_AGENT: 'mapping-agent';

MASK: 'mask';

MASK_REPLY: 'mask-reply';

MASK_REQUEST: 'mask-request';

MASTER: 'master';

MASTERIP: 'masterip';

MATCH: 'match';

MATCH_ALL: 'match-all';

MATCH_ANY: 'match-any';

MATCH_NONE: 'match-none';

MATCH1: 'match1';

MATCH2: 'match2';

MATCH3: 'match3';

MATCHES_ANY
:
  'matches-any'
  {
    if (lastTokenType() == COMMUNITY) {
      pushMode(M_CommunitySetMatchExpr);
    }
  }
;

MATCHES_EVERY
:
  'matches-every'
  {
    if (lastTokenType() == COMMUNITY) {
      pushMode(M_CommunitySetMatchExpr);
    }
  }
;

MATIP_TYPE_A: 'matip-type-a';

MATIP_TYPE_B: 'matip-type-b';

MAXAS_LIMIT: 'maxas-limit';

MAX_ASSOCIATIONS: 'max-associations';

MAX_AUTHENTICATION_FAILURES: 'max-authentication-failures';

MAX_CLIENTS: 'max-clients';

MAX_CONFERENCES: 'max-conferences';

MAX_CONNECTIONS: 'max-connections';

MAX_DN: 'max-dn';

MAX_EPHONES: 'max-ephones';

MAX_IFINDEX_PER_MODULE: 'max-ifindex-per-module';

MAX_LSA: 'max-lsa';

MAX_LSP_LIFETIME: 'max-lsp-lifetime';

MAX_METRIC: 'max-metric';

MAX_ROUTE: 'max-route';

MAX_SERVERS: 'max-servers';

MAX_SESSIONS: 'max-sessions';

MAX_TX_POWER: 'max-tx-power';

MAXIMUM: 'maximum';

MAXIMUM_ACCEPTED_ROUTES: 'maximum-accepted-routes';

MAXIMUM_ACTIVE: 'maximum-active';

MAXIMUM_HOPS: 'maximum-hops';

MAXIMUM_PATHS: 'maximum-paths';

MAXIMUM_PEERS: 'maximum-peers';

MAXIMUM_PREFIX: 'maximum-prefix';

MAXIMUM_ROUTES: 'maximum-routes';

MAXPOLL: 'maxpoll';

MAXSTARTUPS: 'maxstartups';

MBPS: 'Mbps';

MBSSID: 'mbssid';

MBYTES: 'mbytes';

MCAST_RATE_OPT: 'mcast-rate-opt';

MD5: 'md5';

MDIX: 'mdix';

MDT: 'mdt';

MDT_HELLO_INTERVAL: 'mdt-hello-interval';

MED: 'med';

MEDIUM: 'medium';

MEDIA: 'media';

MEDIA_TERMINATION: 'media-termination';

MEDIA_TYPE: 'media-type';

MEMBER: 'member';

MEMORY: 'memory';

MEMORY_SIZE: 'memory-size';

MENU: 'menu';

MESH_CLUSTER_PROFILE: 'mesh-cluster-profile';

MESH_GROUP: 'mesh-group';

MESH_HT_SSID_PROFILE: 'mesh-ht-ssid-profile';

MESH_RADIO_PROFILE: 'mesh-radio-profile';

MESSAGE: 'message';

MESSAGE_COUNTER: 'message-counter';

MESSAGE_DIGEST: 'message-digest';

MESSAGE_DIGEST_KEY: 'message-digest-key';

MESSAGE_LENGTH: 'message-length';

MESSAGE_LEVEL: 'message-level';

MESSAGE_SIZE: 'message-size';

METHOD: 'method';

METRIC: 'metric';

METRIC_STYLE: 'metric-style';

METRIC_TYPE: 'metric-type';

MFIB: 'mfib';

MFIB_MODE: 'mfib-mode';

MFWD: 'mfwd';

MGCP: 'mgcp';

MGCP_PAT: 'mgcp-pat';

MGMT: 'mgmt';

MGMT_AUTH: 'mgmt-auth';

MGMT_SERVER: 'mgmt-server';

MGMT_USER: 'mgmt-user';

MIB: 'mib';

MIBS: 'mibs';

MICRO_BFD: 'micro-bfd';

MICROCODE: 'microcode';

MICROSOFT_DS: 'microsoft-ds';

MIDCALL_SIGNALING: 'midcall-signaling';

MIN_RX: 'min-rx';

MIN_RX_VAR: 'min_rx';

MIN_TX_POWER: 'min-tx-power';

MINIMAL: 'minimal';

MINIMUM: 'minimum';

MINIMUM_ACTIVE: 'minimum-active';

MINIMUM_INTERVAL: 'minimum-interval';

MINIMUM_LINKS: 'minimum-links';

MINPOLL: 'minpoll';

MIRROR: 'mirror';

MISMATCH: 'mismatch';

MLAG: 'mlag';

MLD: 'mld' { setAccessGroupContextTokenType(MLD); };

MLS: 'mls';

MOBILE: 'mobile';

MOBILE_IP: 'mobile-ip';

MOBILE_REDIRECT: 'mobile-redirect';

MOBILITY: 'mobility';

MODE: 'mode';

MODEM: 'modem';

MODULE: 'module';

MODULE_TYPE: 'module-type';

MOFRR: 'mofrr';

MOFRR_LOCKOUT_TIMER: 'mofrr-lockout-timer';

MOFRR_LOSS_DETECTION_TIMER: 'mofrr-loss-detection-timer';

MON: 'Mon';

MONITOR: 'monitor';

MONITOR_INTERFACE: 'monitor-interface';

MONITOR_MAP: 'monitor-map';

MONITOR_SESSION: 'monitor-session';

MONITORING: 'monitoring';

MOP: 'mop';

MPP: 'mpp';

MPLS: 'mpls';

MROUTE: 'mroute';

MROUTE_CACHE: 'mroute-cache';

MS_SQL_M: 'ms-sql-m';

MS_SQL_S: 'ms-sql-s';

MSCHAP: 'mschap';

MSCHAPV2: 'mschapv2';

MSDP: 'msdp';

MSDP_PEER: 'msdp-peer';

MSEC: 'msec';

MSEXCH_ROUTING: 'msexch-routing';

MSG_ICP: 'msg-icp';

MSIE_PROXY: 'msie-proxy';

MSP: 'msp';

MSRPC: 'msrpc';

MSS: 'mss';

MST: 'mst';

MTA: 'mta';

MTU: 'mtu';

MTU_IGNORE: 'mtu-ignore';

MULTICAST: 'multicast';

MULTICAST_BOUNDARY: 'multicast-boundary';

MULTICAST_INTACT: 'multicast-intact';

MULTICAST_ROUTING: 'multicast-routing';

MULTICAST_STATIC_ONLY: 'multicast-static-only';

MULTIHOP: 'multihop';

MULTILINK: 'multilink';

MULTIPATH: 'multipath';

MULTIPATH_RELAX: 'multipath-relax';

MULTIPLIER: 'multiplier';

MULTIPOINT: 'multipoint';

MULTI_CONFIG: 'multi-config';

MULTI_TOPOLOGY: 'multi-topology';

MUST_SECURE: 'must-secure';

MVR: 'mvr';

NAME
:
   'name' -> pushMode ( M_Name )
;

NAME_LOOKUP: 'name-lookup';

NAME_SERVER: 'name-server';

NAMED_KEY: 'named-key';

NAMESPACE: 'namespace';

NAMES: 'names';

NAMESERVER: 'nameserver';

NAS: 'nas';

NAT
:
   [Nn][Aa][Tt]
;

NAT_CONTROL: 'nat-control';

NAT_TRANSPARENCY: 'nat-transparency';

NAT_TRAVERSAL: 'nat-traversal';

NATIVE: 'native';

NBAR: 'nbar';

NBR_UNCONFIG: 'nbr-unconfig';

NCP: 'ncp';

ND: 'nd';

ND_NA: 'nd-na';

ND_NS: 'nd-ns';

NEGOTIATE: 'negotiate';

NEGOTIATED: 'negotiated';

NEGOTIATION: 'negotiation';

NEIGHBOR
:
   'neighbor' -> pushMode ( M_NEIGHBOR )
;

NEIGHBOR_CHECK_ON_RECV: 'neighbor-check-on-recv';

NEIGHBOR_CHECK_ON_SEND: 'neighbor-check-on-send';

NEIGHBOR_DOWN: 'neighbor-down';

NEIGHBOR_FILTER: 'neighbor-filter' -> pushMode(M_Word);

NEIGHBOR_GROUP: 'neighbor-group';

NEQ: 'neq';

NESTED: 'nested';

NET
:
   'net' -> pushMode ( M_ISO_Address )
;

NET_REDIRECT: 'net-redirect';

NET_TOS_REDIRECT: 'net-tos-redirect';

NET_TOS_UNREACHABLE: 'net-tos-unreachable';

NET_UNREACHABLE: 'net-unreachable';

NETBIOS_DGM: 'netbios-dgm';

NETBIOS_NS: 'netbios-ns';

NETBIOS_SS: 'netbios-ss';

NETBIOS_SSN: 'netbios-ssn';

NETCONF: 'netconf';

NETDESTINATION: 'netdestination';

NETDESTINATION6: 'netdestination6';

NETEXTHDR: 'netexthdr';

NETMASK_FORMAT: 'netmask-format';

NETRJS_1: 'netrjs-1';

NETRJS_2: 'netrjs-2';

NETRJS_3: 'netrjs-3';

NETRJS_4: 'netrjs-4';

NETSERVICE: 'netservice';

NETWALL: 'netwall';

NETWNEWS: 'netwnews';

NETWORK: 'network';

NETWORK_CLOCK: 'network-clock';

NETWORK_CLOCK_PARTICIPATE: 'network-clock-participate';

NETWORK_CLOCK_SELECT: 'network-clock-select';

NETWORK_OBJECT: 'network-object';

NETWORK_QOS: 'network-qos';

NETWORK_UNKNOWN: 'network-unknown';

NEW_MODEL: 'new-model';

NEW_RWHO: 'new-rwho';

NEWINFO: 'newinfo';

NEXT_HOP: 'next-hop';

NEXT_HOP_SELF: 'next-hop-self';

NEXT_HOP_THIRD_PARTY: 'next-hop-third-party';

NEXT_SERVER: 'next-server';

NEXTHOP: 'nexthop';

NEXTHOP1: 'nexthop1';

NEXTHOP2: 'nexthop2';

NEXTHOP3: 'nexthop3';

NEXTHOP_ATTRIBUTE: 'nexthop-attribute';

NEXTHOP_LIST: 'nexthop-list';

NFS: 'nfs';

NHOP_ONLY: 'nhop-only';

NHRP: 'nhrp';

NLRI: 'nlri';

NLS: 'nls';

NMSP: 'nmsp';

NNTP: 'nntp';

NNTPS: 'nntps';

NO: 'no';

NO_ADVERTISE: 'no-advertise';

NO_BANNER
:
   'no' F_Whitespace+ 'banner'
;

NO_EXPORT: 'no-export';

NO_L4R_SHIM
:
   'No l4r_shim'
;

NO_LIMIT: 'no-limit';

NO_PREPEND: 'no-prepend';

NO_REDISTRIBUTION: 'no-redistribution';

NO_ROOM_FOR_OPTION: 'no-room-for-option';

NO_SUMMARY: 'no-summary';

NOAUTH: 'noauth';

NODE: 'node';

NOE: 'noe';

NOHANGUP: 'nohangup';

NOMATCH1: 'nomatch1';

NOMATCH2: 'nomatch2';

NOMATCH3: 'nomatch3';

NON_BROADCAST: 'non-broadcast';

NON_CLIENT_NRT: 'non-client-nrt';

NON_DETERMINISTIC_MED: 'non-deterministic-med';

NON_REVERTIVE: 'non-revertive';

NON_SILENT: 'non-silent';

NON500_ISAKMP: 'non500-isakmp';

NONE: 'none';

NONEGOTIATE: 'nonegotiate';

NOPASSWORD: 'nopassword';

NOS: 'nos';

NOT: 'not';

NOT_ADVERTISE: 'not-advertise';

NOTIFICATION: 'notification';

NOTIFICATION_TIMER: 'notification-timer';

NOTIFICATIONS: 'notifications';

NOTIFY: 'notify';

NOTIFY_FILTER: 'notify-filter';

NSF: 'nsf';

NSR: 'nsr';

NSR_DELAY: 'nsr-delay';

NSR_DOWN: 'nsr-down';

NSSA: 'nssa';

NSSA_EXTERNAL: 'nssa-external';

NSW_FE: 'nsw-fe';

NT_ENCRYPTED: 'nt-encrypted';

NTP: 'ntp' { setAccessGroupContextTokenType(NTP); };

NULL: 'null';

NUM_THREAD: 'num-thread';

NV: 'nv';

OAM: 'oam';

OBJECT: 'object';

OBJECT_GROUP
:
   'object-group' -> pushMode(M_ObjectGroup)
;

ODMR: 'odmr';

OFDM: 'ofdm';

OFDM_THROUGHPUT: 'ofdm-throughput';

OFFSET_LIST: 'offset-list';

OLD_REGISTER_CHECKSUM: 'old-register-checksum';

OLSR: 'olsr';

ON: 'on';

ON_FAILURE: 'on-failure';

ON_PASSIVE: 'on-passive';

ON_STARTUP: 'on-startup';

ON_SUCCESS: 'on-success';

ONE: 'one';

ONE_OUT_OF: 'one-out-of';

ONEP: 'onep';

ONLY_OFDM: 'only-ofdm';

OOM_HANDLING: 'oom-handling';

OPEN: 'open';

OPENFLOW: 'openflow';

OPENVPN: 'openvpn';

OPERATION: 'operation';

OPMODE: 'opmode';

OPS: 'ops';

OPTICAL_MONITOR: 'optical-monitor';

OPTIMIZATION_PROFILE: 'optimization-profile';

OPTIMIZE: 'optimize';

OPTIMIZED: 'optimized';

OPTION: 'option';

OPTION_MISSING: 'option-missing';

OPTIONS: 'options';

OR: 'or';

ORGANIZATION_NAME: 'organization-name';

ORGANIZATION_UNIT: 'organization-unit';

ORIGIN: 'origin';

ORIGIN_ID: 'origin-id';

ORIGINATE: 'originate';

ORIGINATOR_ID: 'originator-id';

OSPF: 'ospf';

OSPF3: 'ospf3';

OSPF_EXTERNAL_TYPE_1: 'ospf-external-type-1';

OSPF_EXTERNAL_TYPE_2: 'ospf-external-type-2';

OSPF_INTER_AREA: 'ospf-inter-area';

OSPF_INTRA_AREA: 'ospf-intra-area';

OSPF_NSSA_TYPE_1: 'ospf-nssa-type-1';

OSPF_NSSA_TYPE_2: 'ospf-nssa-type-2';

OSPFV3: 'ospfv3';

OTHER_ACCESS: 'other-access';

OUI
:
   'oui' -> pushMode ( M_COMMENT )
;

OUT: 'out';

OUT_OF_BAND: 'out-of-band';

OUTBOUND_ACL_CHECK: 'outbound-acl-check';

OUTPUT: 'output';

OUTSIDE: 'outside';

OVERLOAD_CONTROL: 'overload-control';

OVERRIDE: 'override';

OVERRIDE_INTERVAL: 'override-interval';

OWNER: 'owner';

P2P: 'p2p';

PACKET: 'packet';

PACKET_CAPTURE_DEFAULTS: 'packet-capture-defaults';

PACKET_LENGTH: 'packet-length';

PACKET_TOO_BIG: 'packet-too-big';

PACKETCABLE: 'packetcable';

PACKETS: 'packets';

PACKETSIZE: 'packetsize';

PAGER: 'pager';

PAGP: 'pagp';

PAN: 'pan';

PAN_OPTIONS: 'pan-options';

PARAM: 'param';

PARAMETER_PROBLEM: 'parameter-problem';

PARAMETERS: 'parameters';

PARENT: 'parent';

PARITY: 'parity';

PARSER: 'parser';

PARTICIPATE: 'participate';

PASS: 'pass';

PASSIVE: 'passive';

PASSIVE_INTERFACE
:
   'passive-interface' -> pushMode ( M_Interface )
;

PASSIVE_ONLY: 'passive-only';

PASSPHRASE: 'passphrase';

PASSWORD: 'password';

PASSWORD_POLICY: 'password-policy';

PASSWORD_PROMPT: 'password-prompt';

PASSWORD_STORAGE: 'password-storage';

PASSWD: 'passwd';

PAT_XLATE: 'pat-xlate';

PATH_ECHO: 'path-echo';

PATH_JITTER: 'path-jitter';

PATH_MTU_DISCOVERY: 'path-mtu-discovery';

PATH_OPTION: 'path-option';

PATH_RETRANSMIT: 'path-retransmit';

PATH_SELECTION: 'path-selection';

PATHCOST: 'pathcost';

PATH: 'path';

PATHS: 'paths';

PATHS_OF_STATISTICS_KEPT: 'paths-of-statistics-kept';

PAUSE: 'pause';

PBKDF2: 'pbkdf2';

PBR: 'pbr';

PBTS: 'pbts';

PCANYWHERE_DATA: 'pcanywhere-data';

PCANYWHERE_STATUS: 'pcanywhere-status';

PCP: 'pcp';

PCP_VALUE: 'pcp-value';

PD_ROUTE_INJECTION: 'pd-route-injection';

PEAKDETECT: 'peakdetect';

PEER: 'peer';

PEER_ADDRESS: 'peer-address';

PEER_CONFIG_CHECK_BYPASS: 'peer-config-check-bypass';

PEER_GROUP
:
   'peer-group' -> pushMode ( M_NEIGHBOR )
;

PEER_GATEWAY: 'peer-gateway';

PEER_ID_VALIDATE: 'peer-id-validate';

PEER_KEEPALIVE: 'peer-keepalive';

PEER_LINK: 'peer-link';

PEER_POLICY: 'peer-policy';

PEER_SESSION: 'peer-session';

PEER_SWITCH: 'peer-switch';

PERCENT_LITERAL: 'percent';

PERFORMANCE_TRAFFIC: 'performance-traffic';

PERIODIC: 'periodic';

PERIODIC_INVENTORY: 'periodic-inventory';

PERIODIC_REFRESH: 'periodic-refresh';

PERMANENT: 'permanent';

PERMIT: 'permit';

PERMIT_HOSTDOWN: 'permit-hostdown';

PER_CE: 'per-ce';

PER_PREFIX: 'per-prefix';

PER_VRF: 'per-vrf';

PERSISTENT: 'persistent';

PFC: 'pfc';

PFS: 'pfs';

PHONE_CONTACT
:
   'phone-contact' -> pushMode ( M_Description )
;

PHONE_NUMBER: 'phone-number';

PHONE_PROXY: 'phone-proxy';

PHY: 'phy';

PHYSICAL_LAYER: 'physical-layer';

PHYSICAL_PORT: 'physical-port';

PICKUP: 'pickup';

PIM: 'pim';

PIM_AUTO_RP: 'pim-auto-rp';

PIM_SPARSE: 'pim-sparse';

PINNING: 'pinning';

PKI: 'pki';

PKIX_TIMESTAMP: 'pkix-timestamp';

PKT_KRB_IPSEC: 'pkt-krb-ipsec';

PLAT: 'plat';

PLATFORM: 'platform';

PM: 'pm';

POAP: 'poap';

POINT_TO_MULTIPOINT: 'point-to-multipoint';

POINT_TO_POINT: 'point-to-point';

POLICE: 'police';

POLICY: 'policy';

POLICY_LIST: 'policy-list';

POLICY_MAP: 'policy-map';

POLICY_MAP_INPUT: 'policy-map-input';

POLICY_MAP_OUTPUT: 'policy-map-output';

POOL: 'pool';

POP: 'pop';

POP2: 'pop2';

POP3: 'pop3';

POP3S: 'pop3s';

PORT: 'port';

PORTFAST: 'portfast';

PORTS: 'ports';

PORT_CHANNEL: 'port-channel';

PORT_CHANNEL_PROTOCOL: 'port-channel-protocol';

PORT_DESCRIPTION: 'port-description';

PORT_NAME: 'port-name';

PORT_OBJECT: 'port-object';

PORT_PRIORITY: 'port-priority';

PORT_PROFILE: 'port-profile';

PORT_SECURITY: 'port-security';

PORT_TYPE: 'port-type';

PORT_UNREACHABLE: 'port-unreachable';

PORTMODE: 'portmode';

POS: 'pos';

POWER: 'power';

POWEROFF: 'poweroff';

POWER_LEVEL: 'power-level';

POWER_MGR: 'power-mgr';

POWER_MONITOR: 'power-monitor';

PPP: 'ppp';

PPTP: 'pptp';

PRC_INTERVAL: 'prc-interval';

PRE_SHARE: 'pre-share';

PRE_SHARED_KEY: 'pre-shared-key';

PRECEDENCE: 'precedence';

PRECEDENCE_UNREACHABLE: 'precedence-unreachable';

PRECONFIGURE: 'preconfigure';

PREDICTOR: 'predictor';

PREEMPT: 'preempt';

PREFER: 'prefer';

PREFERENCE: 'preference';

PREFERRED: 'preferred';

PREFERRED_PATH: 'preferred-path';

PREFIX: 'prefix';

PREFIX_LIST
:
   'prefix-list'
   {
     if (lastTokenType() == ADDRESS) {
       pushMode(M_Words);
     } else {
       pushMode(M_Name);
     }
   }
;

PREFIX_SET: 'prefix-set';

PREPEND: 'prepend' -> pushMode(M_Prepend);

PRF: 'prf';

PRIMARY: 'primary';

PRIMARY_PORT: 'primary-port';

PRIMARY_PRIORITY: 'primary-priority';

PRINT_SRV: 'print-srv';

PRIORITY: 'priority';

PRIORITY_FLOW_CONTROL: 'priority-flow-control';

PRIORITY_FORCE: 'priority-force';

PRIORITY_MAPPING: 'priority-mapping';

PRIORITY_QUEUE: 'priority-queue';

PRIV: 'priv';

PRIVACY: 'privacy';

PRIVATE_AS: 'private-as';

PRIVATE_VLAN: 'private-vlan';

PRIVILEGE: 'privilege';

PRIVILEGE_MODE: 'privilege-mode';

PROBE: 'probe';

PROCESS: 'process';

PROCESS_FAILURES: 'process-failures';

PROCESS_MAX_TIME: 'process-max-time';

PROFILE: 'profile';

PROGRESS_IND: 'progress_ind';

PROMPT: 'prompt';

PROPAGATE: 'propagate';

PROPAGATION_DELAY: 'propagation-delay';

PROPOSAL: 'proposal';

PROPRIETARY: 'proprietary';

PROTECT: 'protect';

PROTECT_SSID: 'protect-ssid';

PROTECT_VALID_STA: 'protect-valid-sta';

PROTECTION: 'protection';

PROTOCOL: 'protocol';

PROTOCOL_DISCOVERY: 'protocol-discovery';

PROTOCOL_HTTP: 'protocol-http';

PROTOCOL_OBJECT: 'protocol-object';

PROTOCOL_UNREACHABLE: 'protocol-unreachable';

PROTOCOL_VIOLATION: 'protocol-violation';

PROVISION: 'provision';

PROVISIONING_PROFILE: 'provisioning-profile';

PROXY_ARP: 'proxy-arp';

PROXY_SERVER: 'proxy-server';

PRUNING: 'pruning';

PSEUDO_INFORMATION: 'pseudo-information';

PSEUDOWIRE: 'pseudowire';

PSEUDOWIRE_CLASS: 'pseudowire-class';

PSH: 'psh';

PTP: 'ptp';

PTP_EVENT: 'ptp-event';

PTP_GENERAL: 'ptp-general';

PUBKEY_CHAIN: 'pubkey-chain';

PVC: 'pvc';

QMTP: 'qmtp';

QOS: 'qos';

QOS_GROUP: 'qos-group';

QOS_MAPPING: 'qos-mapping';

QOS_POLICY: 'qos-policy';

QOS_POLICY_OUTPUT: 'qos-policy-output';

QOS_SC: 'qos-sc';

QOTD: 'qotd';

QUERY: 'query';

QUERY_INTERVAL: 'query-interval';

QUERY_MAX_RESPONSE_TIME: 'query-max-response-time';

QUERY_ONLY: 'query-only';

QUERY_TIMEOUT: 'query-timeout';

QUEUE: 'queue';

QUEUE_BUFFERS: 'queue-buffers';

QUEUE_LENGTH: 'queue-length';

QUEUE_LIMIT: 'queue-limit';

QUEUE_MONITOR: 'queue-monitor';

QUEUE_SET: 'queue-set';

QUEUEING: 'queueing';

QUEUING: 'queuing';

QUIT: 'quit';

RADIUS: 'radius';

RADIUS_ACCOUNTING: 'radius-accounting';

RADIUS_ACCT: 'radius-acct';

RADIUS_COMMON_PW: 'radius-common-pw';

RADIUS_INTERIM_ACCOUNTING: 'radius-interim-accounting';

RADIUS_SERVER: 'radius-server';

RANDOM: 'random';

RANDOM_DETECT: 'random-detect';

RANDOM_DETECT_LABEL: 'random-detect-label';

RANGE
:
  'range'
  {
    if (lastTokenType() == THRESHOLD) {
      pushMode(M_Word);
    }
  }
;

RATE_LIMIT: 'rate-limit';

RATE_MODE: 'rate-mode';

RATE_PER_ROUTE: 'rate-per-route';

RATE_THRESHOLDS_PROFILE: 'rate-thresholds-profile';

RBACL: 'rbacl';

RCMD: 'rcmd';

RCP: 'rcp';

RCV_QUEUE: 'rcv-queue';

RD: 'rd' -> pushMode(M_Rd);

RD_SET: 'rd-set' -> pushMode(M_RdSet);

RE_MAIL_CK: 're-mail-ck';

REACHABLE_VIA: 'reachable-via';

REACT: 'react';

REACTION: 'reaction';

READ_ONLY_PASSWORD: 'read-only-password';

REAL: 'real';

REAL_TIME_CONFIG: 'real-time-config';

REASSEMBLY_TIMEOUT: 'reassembly-timeout';

REAUTHENTICATION: 'reauthentication';

RECEIVE: 'receive';

RECEIVE_QUEUE: 'receive-queue';

RECEIVE_WINDOW: 'receive-window';

RECONNECT_INTERVAL: 'reconnect-interval';

RECORD: 'record';

RECORD_ENTRY: 'record-entry';

RED: 'red';

REDIRECT: 'redirect';

REDIRECT_FQDN: 'redirect-fqdn';

REDIRECT_LIST: 'redirect-list';

REDIRECT_PAUSE: 'redirect-pause';

REDIRECTS: 'redirects';

REDISTRIBUTE: 'redistribute';

REDISTRIBUTE_INTERNAL: 'redistribute-internal';

REDISTRIBUTED_PREFIXES: 'redistributed-prefixes';

REDUNDANCY: 'redundancy';

REDUNDANCY_GROUP: 'redundancy-group';

REFERENCE_BANDWIDTH: 'reference-bandwidth';

REFLECT: 'reflect';

REFLECTION: 'reflection';

REFLEXIVE_LIST: 'reflexive-list';

REGISTER_RATE_LIMIT: 'register-rate-limit';

REGISTER_SOURCE: 'register-source';

REGULATORY_DOMAIN_PROFILE: 'regulatory-domain-profile';

REINIT: 'reinit';

RELAY: 'relay';

RELOAD: 'reload';

RELOAD_DELAY: 'reload-delay';

RELOAD_TYPE: 'reload-type';

REMARK
:
   'remark' -> pushMode ( M_REMARK )
;

REMOTE: 'remote';

REMOTE_ACCESS: 'remote-access';

REMOTE_AS: 'remote-as';

REMOTE_IP: 'remote-ip';

REMOTE_NEIGHBORS: 'remote-neighbors';

REMOTE_PORT: 'remote-port';

REMOTE_PORTS: 'remote-ports';

REMOTE_SERVER: 'remote-server';

REMOTE_SPAN: 'remote-span';

REMOTEFS: 'remotefs';

REMOVE: 'remove';

REMOVE_PRIVATE_AS
:
   'remove-private-' [Aa] [Ss]
;

REOPTIMIZE: 'reoptimize';

REPCMD: 'repcmd';

REPLACE_AS: 'replace-as';

REPLY_TO: 'reply-to';

REPORT_INTERVAL: 'report-interval';

REQ_RESP: 'req-resp';

REQUEST: 'request';

REQUEST_DATA_SIZE: 'request-data-size';

REQUIRE_WPA: 'require-wpa';

RESOURCE: 'resource';

RESOURCE_POOL: 'resource-pool';

RESOURCES: 'resources';

RESPONDER: 'responder';

RESPONSE: 'response';

RESTRICT: 'restrict';

RESULT_TYPE: 'result-type';

RESUME: 'resume';

RETRANSMIT: 'retransmit';

RETRANSMIT_INTERVAL: 'retransmit-interval';

RETRANSMIT_TIMEOUT: 'retransmit-timeout';

RETRIES: 'retries';

RETRY: 'retry';

REVERSE_ACCESS: 'reverse-access';

REVERSE_PATH: 'reverse-path';

REVERSE_ROUTE: 'reverse-route';

REVERTIVE: 'revertive';

REVISION: 'revision';

REVOCATION_CHECK: 'revocation-check';

REWRITE: 'rewrite';

RF: 'rf';

RF_SWITCH: 'rf-switch';

RFC_3576_SERVER: 'rfc-3576-server';

RFC1583: 'rfc1583';

RFC1583COMPATIBILITY: 'rfc1583compatibility';

RIB: 'rib' -> pushMode(M_Word);

RIB_HAS_ROUTE: 'rib-has-route';

RIB_METRIC_AS_EXTERNAL: 'rib-metric-as-external';

RIB_METRIC_AS_INTERNAL: 'rib-metric-as-internal';

RIB_SCALE: 'rib-scale';

RING: 'ring';

RIP: 'rip';

RJE: 'rje';

RLP: 'rlp';

RLZDBASE: 'rlzdbase';

RMC: 'rmc';

RMON: 'rmon';

RMONITOR: 'rmonitor';

RO
:
   [rR] [oO]
;

ROBUSTNESS_COUNT: 'robustness-count';

ROBUSTNESS_VARIABLE: 'robustness-variable';

ROGUE_AP_AWARE: 'rogue-ap-aware';

ROLE: 'role';

ROOT: 'root';

ROTARY: 'rotary';

ROUTE: 'route';

ROUTE_CACHE: 'route-cache';

ROUTE_ONLY: 'route-only';

ROUTE_POLICY: 'route-policy' -> pushMode(M_Word);

ROUTE_PREFERENCE: 'route-preference';

ROUTE_REFLECTOR_CLIENT: 'route-reflector-client';

ROUTE_TAG: 'route-tag';

ROUTE_TARGET: 'route-target';

ROUTE_TYPE: 'route-type';

ROUTED: 'routed';

ROUTER: 'router';

ROUTER_ADVERTISEMENT: 'router-advertisement';

ROUTER_ALERT: 'router-alert';

ROUTER_ID: 'router-id';

ROUTER_INTERFACE: 'router-interface';

ROUTER_LSA: 'router-lsa';

ROUTER_SOLICITATION: 'router-solicitation';

ROUTING: 'routing';

RP: 'rp';

RP_ADDRESS: 'rp-address' -> pushMode(M_RpAddress);

RP_ANNOUNCE_FILTER: 'rp-announce-filter';

RP_CANDIDATE
:
   'rp-candidate' -> pushMode(M_Interface)
;

RP_LIST: 'rp-list' -> pushMode(M_Word);

RP_STATIC_DENY: 'rp-static-deny' -> pushMode(M_Word);

RPC2PORTMAP: 'rpc2portmap';

RPF: 'rpf';

RPF_REDIRECT: 'rpf-redirect';

RPF_VECTOR: 'rpf-vector';

RRM_IE_PROFILE: 'rrm-ie-profile';

RSA: 'rsa';

RSA_ENCR: 'rsa-encr';

RSA_SIG: 'rsa-sig';

RSAKEYPAIR: 'rsakeypair';

RSH: 'rsh';

RST: 'rst';

RSTP: 'rstp';

RSVP: 'rsvp';

RSVP_TE: 'rsvp-te';

RSYNC: 'rsync';

RT: 'rt';

RTCP_INACTIVITY: 'rtcp-inactivity';

RTELNET: 'rtelnet';

RTP: 'rtp';

RTP_PORT: 'rtp-port';

RTR: 'rtr';

RTR_ADV: 'rtr-adv';

RTSP: 'rtsp';

RULE_NAME: 'rule-name';

RUN: 'run';

RW
:
   [Rr] [Ww]
;

RX: 'rx';

RX_COS_SLOT: 'rx-cos-slot';

RXSPEED: 'rxspeed';

SA_FILTER: 'sa-filter';

SAMPLER: 'sampler';

SAMPLER_MAP: 'sampler-map' -> pushMode(M_Word);

SAMPLES_OF_HISTORY_KEPT: 'samples-of-history-kept';

SAP: 'sap';

SAT: 'Sat';

SATELLITE: 'satellite';

SATELLITE_FABRIC_LINK: 'satellite-fabric-link';

SCALE_FACTOR: 'scale-factor';

SCAN_TIME: 'scan-time';

SCANNING: 'scanning';

SCCP: 'sccp';

SCHEDULE: 'schedule';

SCHEDULER: 'scheduler';

SCHEME: 'scheme';

SCOPE: 'scope';

SCP: 'scp';

SCRAMBLE: 'scramble';

SCRIPT: 'script';

SCRIPTING: 'scripting';

SCTP: 'sctp';

SDM: 'sdm';

SDR: 'sdr';

SDROWNER: 'SDROwner';

SECONDARY: 'secondary';

SECONDARY_DIALTONE: 'secondary-dialtone';

SECRET: 'secret';

SECUREID_UDP: 'secureid-udp';

SECURE_MAC_ADDRESS: 'secure-mac-address';

SECURITY: 'security';

SECURITY_ASSOCIATION: 'security-association';

SELECT: 'select';

SELECTION: 'selection';

SELECTIVE: 'selective';

SELECTIVE_ACK: 'selective-ack';

SELF: 'self';

SELF_IDENTITY: 'self-identity';

SEND: 'send';

SEND_COMMUNITY: 'send-community';

SEND_COMMUNITY_EBGP: 'send-community-ebgp';

SEND_EXTENDED_COMMUNITY_EBGP: 'send-extended-community-ebgp';

SEND_LABEL: 'send-label';

SEND_LIFETIME: 'send-lifetime';

SEND_RP_ANNOUNCE
:
   'send-rp-announce' -> pushMode(M_Interface)
;

SEND_RP_DISCOVERY: 'send-rp-discovery';

SENDER: 'sender';

SENSOR: 'sensor';

SEQ: 'seq';

SEQUENCE: 'sequence';

SEQUENCE_NUMS: 'sequence-nums';

SERIAL: 'serial';

SERIAL_NUMBER: 'serial-number';

SERVE: 'serve';

SERVE_ONLY: 'serve-only';

SERVER: 'server';

SERVER_ARP: 'server-arp';

SERVER_GROUP: 'server-group';

SERVER_KEY: 'server-key';

SERVER_PRIVATE: 'server-private';

SERVER_TYPE: 'server-type';

SERVERFARM: 'serverfarm';

SERVICE: 'service';

SERVICE_CLASS: 'service-class';

SERVICE_FAMILY: 'service-family';

SERVICE_LIST: 'service-list';

SERVICE_MODULE: 'service-module';

SERVICE_OBJECT: 'service-object';

SERVICE_POLICY: 'service-policy';

SERVICE_QUEUE: 'service-queue';

SERVICE_TEMPLATE: 'service-template';

SERVICE_TYPE: 'service-type';

SESSION: 'session';

SESSION_AUTHORIZATION: 'session-authorization';

SESSION_DISCONNECT_WARNING
:
   'session-disconnect-warning' -> pushMode ( M_COMMENT )
;

SESSION_GROUP: 'session-group';

SESSION_ID: 'session-id';

SESSION_KEY: 'session-key';

SESSION_LIMIT: 'session-limit';

SESSION_OPEN_MODE: 'session-open-mode';

SESSION_PROTECTION: 'session-protection';

SESSION_TIMEOUT: 'session-timeout';

SET: 'set';

SET_COLOR: 'set-color';

SET_OVERLOAD_BIT: 'set-overload-bit';

SET_TIMER: 'set-timer';

SETUP: 'setup';

SEVERITY: 'severity';

SF_INTERFACE
:
   'sf-interface' -> pushMode ( M_Interface )
;

SFLOW: 'sflow';

SFTP: 'sftp';

SG_EXPIRY_TIMER: 'sg-expiry-timer';

SG_LIST: 'sg-list' -> pushMode(M_Word);

SGBP: 'sgbp';

SGMP: 'sgmp';

SHA: 'sha';

SHA2_256_128: 'sha2-256-128';

SHA512_PASSWORD
:
   '$sha512$' [0-9]+ '$' F_Base64String '$' F_Base64String -> pushMode ( M_SeedWhitespace )
;

SHAPE: 'shape';

SHELFNAME: 'shelfname';

SHELL: 'shell';

SHORT_TXT: 'short-txt';

SHOULD_SECURE: 'should-secure';

SHOW: 'show';

SHUTDOWN
:
   'shut' 'down'?
;

SIGNAL: 'signal';

SIGNALING: 'signaling';

SIGNALLED_BANDWIDTH: 'signalled-bandwidth';

SIGNALLED_NAME: 'signalled-name';

SIGNALLING: 'signalling';

SIGNATURE: 'signature';

SIGNATURE_MATCHING_PROFILE: 'signature-matching-profile';

SIGNATURE_PROFILE: 'signature-profile';

SIGNING: 'signing';

SILC: 'silc';

SILENT: 'silent';

SINGLE_CONNECTION: 'single-connection';

SINGLE_HOP: 'single-hop';

SINGLE_ROUTER_MODE: 'single-router-mode';

SINGLE_TOPOLOGY: 'single-topology';

SIP: 'sip';

SIP_DISCONNECT: 'sip-disconnect';

SIP_INVITE: 'sip-invite';

SIP_MEDIA: 'sip_media';

SIP_MIDCALL_REQ_TIMEOUT: 'sip-midcall-req-timeout';

SIP_PROFILES: 'sip-profiles';

SIP_PROVISIONAL_MEDIA: 'sip-provisional-media';

SIP_SERVER: 'sip-server';

SIP_UA: 'sip-ua';

SIPS: 'sips';

SITE_ID: 'site-id';

SLA: 'sla';

SLOT: 'slot';

SLOT_TABLE_COS: 'slot-table-cos';

SMALL_HELLO: 'small-hello';

SMART_RELAY: 'smart-relay';

SMTP: 'smtp';

SMTP_SERVER: 'smtp-server';

SMUX: 'smux';

SNAGAS: 'snagas';

SNMP_AUTHFAIL: 'snmp-authfail';

SNMP: 'snmp';

// must be kept above SNMP_SERVER
SNMP_SERVER_COMMUNITY
:
   'snmp-server'
   {(lastTokenType() == NEWLINE || lastTokenType() == -1)
     && isWhitespace(_input.LA(1))
     && lookAheadStringSkipWhitespace("community ".length()).equals("community ")}?
   -> type ( SNMP_SERVER ) , pushMode ( M_SnmpServerCommunity )
;

SNMP_SERVER: 'snmp-server';

SNMP_TRAP: 'snmp-trap';

SNMPTRAP: 'snmptrap';

SNOOPING: 'snooping';

SNP: 'snp';

SNPP: 'snpp';

SNR_MAX: 'snr-max';

SNR_MIN: 'snr-min';

SNTP: 'sntp';

SORT_BY: 'sort-by';

SPE: 'spe';

SPECTRUM: 'spectrum';

SPECTRUM_LOAD_BALANCING: 'spectrum-load-balancing';

SPECTRUM_MONITORING: 'spectrum-monitoring';

SPF_INTERVAL: 'spf-interval';

SOFT_PREEMPTION: 'soft-preemption';

SOFT_RECONFIGURATION
:
   'soft' '-reconfiguration'?
;

SOFTWARE: 'software';

SONET: 'sonet';

SOURCE: 'source';

SOURCE_ADDRESS: 'source-address';

SOURCE_INTERFACE
:
   'source-interface' -> pushMode ( M_Interface )
;

SOURCE_IP_ADDRESS: 'source-ip-address';

SOURCE_ROUTE: 'source-route';

SOURCE_ROUTE_FAILED: 'source-route-failed';

SOURCE_QUENCH: 'source-quench';

SPAN: 'span';

SPANNING_TREE: 'spanning-tree';

SPARSE_DENSE_MODE: 'sparse-dense-mode';

SPARSE_MODE: 'sparse-mode';

SPARSE_MODE_SSM: 'sparse-mode-ssm';

SPD: 'spd';

SPEED: 'speed';

SPEED_DUPLEX: 'speed-duplex';

SPLIT_HORIZON: 'split-horizon';

SPLIT_TUNNEL_NETWORK_LIST: 'split-tunnel-network-list';

SPLIT_TUNNEL_POLICY: 'split-tunnel-policy';

SPT_THRESHOLD: 'spt-threshold';

SQLNET: 'sqlnet';

SQLSRV: 'sqlsrv';

SQLSERV: 'sqlserv';

SRC_IP: 'src-ip';

SRLG: 'srlg';

SRR_QUEUE: 'srr-queue';

SRST: 'srst';

SSH: 'ssh';

SSH_CERTIFICATE: 'ssh-certificate';

SSH_KEYDIR: 'ssh_keydir';

SSH_PUBLICKEY: 'ssh-publickey';

SSID: 'ssid';

SSID_ENABLE: 'ssid-enable';

SSID_PROFILE: 'ssid-profile';

SSL: 'ssl';

SSM: 'ssm';

STACK_MAC: 'stack-mac';

STACK_MIB: 'stack-mib';

STACK_UNIT: 'stack-unit';

STALE_ROUTE: 'stale-route';

STANDBY: 'standby';

START: 'start';

START_STOP: 'start-stop';

START_TIME: 'start-time';

STARTUP_QUERY_COUNT: 'startup-query-count';

STARTUP_QUERY_INTERVAL: 'startup-query-interval';

STATE: 'state';

STATIC
:
  'static'
  {
    if (lastTokenType() == MAP) {
      pushMode(M_SsmMapStatic);
    }
  }
;

STATIC_GROUP: 'static-group';

STATIC_RPF: 'static-rpf';

STATION_ROLE: 'station-role';

STATISTICS: 'statistics';

STBC: 'stbc';

STCAPP: 'stcapp';

STICKY: 'sticky';

STICKY_ARP: 'sticky-arp';

STOP: 'stop';

STOP_ONLY: 'stop-only';

STOP_RECORD: 'stop-record';

STOP_TIMER: 'stop-timer';

STOPBITS: 'stopbits';

STORM_CONTROL: 'storm-control';

STP: 'stp';

STREAMING: 'streaming';

STREET_ADDRESS: 'street-address';

STREETADDRESS
:
   'streetaddress' -> pushMode ( M_Description )
;

STRICTHOSTKEYCHECK: 'stricthostkeycheck';

STRING: 'string';

STRIP: 'strip';

STUB: 'stub';

SUBINTERFACE: 'subinterface';

SUBINTERFACES: 'subinterfaces';

SUBJECT_NAME: 'subject-name';

SUBNET: 'subnet';

SUBNET_BROADCAST: 'subnet-broadcast';

SUBNET_MASK: 'subnet-mask';

SUBNET_ZERO: 'subnet-zero';

SUB_OPTION: 'sub-option';

SUBMISSION: 'submission';

SUBSCRIBE_TO: 'subscribe-to';

SUBSCRIBE_TO_ALERT_GROUP: 'subscribe-to-alert-group';

SUBSCRIBER: 'subscriber';

SUCCESS: 'success';

SUMMARY_ADDRESS: 'summary-address';

SUMMARY_LSA: 'summary-lsa';

SUMMARY_METRIC: 'summary-metric';

SUMMARY_ONLY: 'summary-only';

SUN: 'Sun';

SUNRPC: 'sunrpc';

SUPER_USER_PASSWORD: 'super-user-password';

SUPPLEMENTARY_SERVICE: 'supplementary-service';

SUPPRESS: 'suppress';

SUPPRESS_ARP: 'suppress-arp';

SUPPRESS_RA: 'suppress-ra';

SUPPRESS_DATA_REGISTERS: 'suppress-data-registers';

SUPPRESS_FIB_PENDING: 'suppress-fib-pending';

SUPPRESS_RPF_CHANGE_PRUNES: 'suppress-rpf-change-prunes';

SUPPRESSED: 'suppressed';

SUSPECT_ROGUE_CONF_LEVEL: 'suspect-rogue-conf-level';

SVC: 'svc';

SVCLC: 'svclc';

SVP: 'svp';

SVRLOC: 'svrloc';

SWITCH: 'switch';

SWITCH_CERT: 'switch-cert';

SWITCH_PRIORITY: 'switch-priority';

SWITCH_PROFILE: 'switch-profile';

SWITCH_TYPE: 'switch-type';

SWITCHBACK: 'switchback';

SWITCHING_MODE: 'switching-mode';

SWITCHNAME: 'switchname';

SWITCHOVER: 'switchover';

SWITCHPORT: 'switchport';

SYMMETRIC: 'symmetric';

SYN: 'syn';

SYNC: 'sync';

SYNC_IGP_SHORTCUTS: 'sync-igp-shortcuts';

SYNCHRONIZATION: 'synchronization';

SYNCHRONOUS: 'synchronous';

SYNWAIT_TIME: 'synwait-time';

SYSCONTACT: 'syscontact';

SYSLOCATION: 'syslocation';

SYSLOG: 'syslog';

SYSLOGD: 'syslogd';

SYSOPT: 'sysopt';

SYSTAT: 'systat';

SYSTEM: 'system';

SYSTEM_CAPABILITIES: 'system-capabilities';

SYSTEM_DESCRIPTION: 'system-description';

SYSTEM_INIT: 'system-init';

SYSTEM_MAX: 'system-max';

SYSTEM_NAME: 'system-name';

SYSTEM_PRIORITY: 'system-priority';

SYSTEM_PROFILE: 'system-profile';

SYSTEM_SHUTDOWN: 'system-shutdown';

SYSTEMOWNER: 'SystemOwner';

TABLE_MAP: 'table-map';

TACACS: 'tacacs';

TACACS_DS: 'tacacs-ds';

TACACS_PLUS
:
   'tacacs+'
;

TACACS_SERVER: 'tacacs-server';

TAC_PLUS: 'tac_plus';

TAG: 'tag';

TAG_SWITCHING: 'tag-switching';

TAG_TYPE: 'tag-type';

TAGGED: 'tagged';

TALK: 'talk';

TAP: 'tap';

TARGETED_HELLO: 'targeted-hello';

TASK: 'task';

TASK_SPACE_EXECUTE
:
   'task execute'
;

TASKGROUP: 'taskgroup';

TB_VLAN1: 'tb-vlan1';

TB_VLAN2: 'tb-vlan2';

TBRPF: 'tbrpf';

TCAM: 'tcam';

TCP: 'tcp';

TCP_CONNECT: 'tcp-connect';

TCP_INSPECTION: 'tcp-inspection';

TCP_PROXY_REASSEMBLY: 'tcp-proxy-reassembly';

TCP_SESSION: 'tcp-session';

TCP_UDP: 'tcp-udp';

TCPMUX: 'tcpmux';

TCPNETHASPSRV: 'tcpnethaspsrv';

TELEPHONY_SERVICE: 'telephony-service';

TELNET: 'telnet';

TELNET_SERVER: 'telnet-server';

TEMPLATE: 'template';

TERMINAL: 'terminal';

TERMINAL_TYPE: 'terminal-type';

TERMINATE: 'terminate';

TERMINATION: 'termination';

TEST: 'test';

TFTP: 'tftp';

TFTP_SERVER: 'tftp-server';

TFTP_SERVER_LIST: 'tftp-server-list';

THEN: 'then';

THREAT_DETECTION: 'threat-detection';

THREAT_VISIBILITY: 'threat-visibility';

THREE_DES: '3des';

THRESHOLD: 'threshold';

THROTTLE: 'throttle';

THROUGHPUT: 'throughput';

THU: 'Thu';

TID: 'tid';

TIME: 'time';

TIME_EXCEEDED: 'time-exceeded';

TIME_FORMAT: 'time-format';

TIME_RANGE: 'time-range';

TIME_OUT: 'time-out';

TIME_ZONE: 'time-zone';

TIMED: 'timed';

TIMEOUT: 'timeout';

TIMEOUTS: 'timeouts';

TIMER: 'timer';

TIMERS: 'timers';

TIMESOURCE: 'timesource';

TIMESTAMP: 'timestamp';

TIMESTAMP_REPLY: 'timestamp-reply';

TIMESTAMP_REQUEST: 'timestamp-request';

TIMEZONE: 'timezone';

TIMING: 'timing';

TLS_PROXY: 'tls-proxy';

TLV_SELECT: 'tlv-select';

TM_VOQ_COLLECTION: 'tm-voq-collection';

TO: 'to';

TOKEN: 'token';

TOOL: 'tool';

TOP: 'top';

TOPOLOGY
:
  'topology'
  {
    if (lastTokenType() == RPF) {
      pushMode(M_Word);
    }
  }
;

TOS: 'tos';

TRACE: 'trace';

TRACEROUTE: 'traceroute';

TRACK: 'track';

TRACKED: 'tracked';

TRACKING_PRIORITY_INCREMENT: 'tracking-priority-increment';

TRADITIONAL: 'traditional';

TRAFFIC: 'traffic';

TRAFFIC_ENG: 'traffic-eng';

TRAFFIC_EXPORT: 'traffic-export';

TRAFFIC_SHARE: 'traffic-share';

TRANSFER_SYSTEM: 'transfer-system';

TRANSFORM_SET: 'transform-set';

TRANSCEIVER: 'transceiver';

TRANSCEIVER_TYPE_CHECK: 'transceiver-type-check';

TRANSLATE: 'translate';

TRANSLATION_RULE: 'translation-rule';

TRANSLATION_PROFILE: 'translation-profile';

TRANSMIT: 'transmit';

TRANSMIT_DELAY: 'transmit-delay';

TRANSPARENT_HW_FLOODING: 'transparent-hw-flooding';

TRANSPORT: 'transport';

TRANSPORT_ADDRESS: 'transport-address';

TRANSPORT_METHOD: 'transport-method';

TRANSPORT_MODE: 'transport-mode';

TRAP: 'trap';

TRAP_SOURCE
:
   'trap-source' -> pushMode ( M_Interface )
;

TRAP_TIMEOUT: 'trap-timeout';

TRAPS: 'traps';

TRIGGER: 'trigger';

TRIMODE: 'trimode';

TRUNK: 'trunk';

TRUNK_THRESHOLD: 'trunk-threshold';

TRUST: 'trust';

TRUSTED: 'trusted';

TRUSTED_KEY: 'trusted-key';

TRUSTPOINT: 'trustpoint';

TRUSTPOOL: 'trustpool';

TSM_REQ_PROFILE: 'tsm-req-profile';

TTL: 'ttl';

TTL_EXCEEDED: 'ttl-exceeded';

TTL_THRESHOLD: 'ttl-threshold';

TTY: 'tty';

TUE: 'Tue';

TUNABLE_OPTIC: 'tunable-optic';

TUNNEL: 'tunnel';

TUNNEL_GROUP: 'tunnel-group';

TUNNEL_GROUP_LIST: 'tunnel-group-list';

TUNNEL_ID: 'tunnel-id';

TUNNELED: 'tunneled';

TUNNELED_NODE_ADDRESS: 'tunneled-node-address';

TX_QUEUE: 'tx-queue';

TXSPEED: 'txspeed';

TYPE: 'type';

TYPE_1: 'type-1';

TYPE_2: 'type-2';

UAUTH: 'uauth';

UC_TX_QUEUE: 'uc-tx-queue';

UDF: 'udf';

UDLD: 'udld';

UDP: 'udp';

UDP_JITTER: 'udp-jitter';

UID: 'uid';

UNABLE: 'Unable';

UNAUTHORIZED: 'unauthorized';

UNAUTHORIZED_DEVICE_PROFILE: 'unauthorized-device-profile';

UNICAST_QOS_ADJUST: 'unicast-qos-adjust';

UNICAST_ROUTING: 'unicast-routing';

UNIQUE: 'unique';

UNIT: 'unit';

UNNUMBERED: 'unnumbered';

UNREACHABLE: 'unreachable';

UNREACHABLES: 'unreachables';

UNSET: 'unset';

UNSUPPRESS_ROUTE: 'unsuppress-route';

UNICAST: 'unicast';

UNTAGGED: 'untagged';

UPDATE: 'update';

UPDATE_CALENDAR: 'update-calendar';

UPDATE_SOURCE
:
   'update-source' -> pushMode ( M_Interface )
;

UPGRADE: 'upgrade';

UPGRADE_PROFILE: 'upgrade-profile';

UPLINK: 'uplink';

UPLINKFAST: 'uplinkfast';

UPS: 'ups';

URG: 'urg';

URI: 'uri';

URL: 'url';

URL_LIST: 'url-list';

URPF: 'urpf';

USE: 'use';

USE_ACL: 'use-acl';

USE_BIA: 'use-bia';

USE_IPV4_ACL: 'use-ipv4-acl';

USE_IPV6_ACL: 'use-ipv6-acl';

USE_LINK_ADDRESS: 'use-link-address';

USE_VRF: 'use-vrf';

USER: 'user';

USER_IDENTITY: 'user-identity';

USERINFO
:
	'userinfo'
;

USER_MESSAGE
:
   'user-message' -> pushMode ( M_Description )
;

USER_ROLE: 'user-role';

USER_STATISTICS: 'user-statistics';

USERGROUP: 'usergroup' -> pushMode(M_Word);

USERNAME: 'username';

USERNAME_PROMPT: 'username-prompt';

USERPASSPHRASE: 'userpassphrase';

USERS: 'users';

USING: 'Using';

UUCP: 'uucp';

UUCP_PATH: 'uucp-path';

V1_RP_REACHABILITY: 'v1-rp-reachability';

V2: 'v2';

V4: 'v4';

V6: 'v6';

VACANT_MESSAGE: 'vacant-message';

VACANT_MESSAGE_BANNER_IOS
:
  'vacant-message' F_Whitespace+
  {
    if (lastTokenType() != NO) {
      pushMode(M_BannerIosDelimiter);
    }
  } -> type(VACANT_MESSAGE)
;

VACL: 'vacl';

VAD: 'vad';

VALID: 'valid';

VALID_11A_40MHZ_CHANNEL_PAIR: 'valid-11a-40mhz-channel-pair';

VALID_11A_80MHZ_CHANNEL_GROUP: 'valid-11a-80mhz-channel-group';

VALID_11A_CHANNEL: 'valid-11a-channel';

VALID_11G_40MHZ_CHANNEL_PAIR: 'valid-11g-40mhz-channel-pair';

VALID_11G_CHANNEL: 'valid-11g-channel';

VALID_AND_PROTECTED_SSID: 'valid-and-protected-ssid';

VALID_NETWORK_OUI_PROFILE: 'valid-network-oui-profile';

VALIDATION_STATE: 'validation-state';

VALIDATION_USAGE: 'validation-usage';

VAP_ENABLE: 'vap-enable';

VARIANCE: 'variance';

VDC: 'vdc';

VER: 'ver';

VERIFY: 'verify';

VERIFY_DATA: 'verify-data';

VERSION: 'version';

VIDEO: 'video';

VIEW: 'view';

VIOLATE_ACTION: 'violate-action';

VIOLATION: 'violation';

VIRTUAL: 'virtual';

VIRTUAL_ADDRESS: 'virtual-address';

VIRTUAL_AP: 'virtual-ap';

VIRTUAL_REASSEMBLY: 'virtual-reassembly';

VIRTUAL_ROUTER: 'virtual-router';

VIRTUAL_SERVICE: 'virtual-service';

VIRTUAL_TEMPLATE: 'virtual-template';

VFI: 'vfi';

VLAN: 'vlan';

VLAN_GROUP: 'vlan-group';

VLAN_NAME: 'vlan-name';

VLAN_POLICY: 'vlan-policy';

VLT: 'vlt';

VLT_PEER_LAG: 'vlt-peer-lag';

VM_CPU: 'vm-cpu';

VM_MEMORY: 'vm-memory';

VMNET: 'vmnet';

VMPS: 'vmps';

VMTRACER: 'vmtracer';

VNI: 'vni';

VN_SEGMENT: 'vn-segment';

VOCERA: 'vocera';

VOICE: 'voice';

VOICE_CARD: 'voice-card';

VOICE_CLASS: 'voice-class';

VOICE_PORT: 'voice-port';

VOICE_SERVICE: 'voice-service';

VOIP: 'voip';

VOIP_CAC_PROFILE: 'voip-cac-profile';

VPC: 'vpc';

VPDN: 'vpdn';

VPDN_GROUP: 'vpdn-group';

VPLS: 'vpls';

VPN: 'vpn';

VPN_DIALER: 'vpn-dialer';

VPN_GROUP_POLICY: 'vpn-group-policy';

VPN_FILTER: 'vpn-filter';

VPN_IDLE_TIMEOUT: 'vpn-idle-timeout';

VPN_SESSION_TIMEOUT: 'vpn-session-timeout';

VPN_SIMULTANEOUS_LOGINS: 'vpn-simultaneous-logins';

VPN_TUNNEL_PROTOCOL: 'vpn-tunnel-protocol';

VPNV4: 'vpnv4';

VPNV6: 'vpnv6';

VRF: 'vrf' -> pushMode(M_VrfName);

VRF_ALSO: 'vrf-also';

VRRP: 'vrrp';

VSERVER: 'vserver';

VSTACK: 'vstack';

VTP: 'vtp';

VTY: 'vty';

VTY_POOL: 'vty-pool';

VXLAN: 'vxlan';

WAIT_FOR: 'wait-for';

WAIT_FOR_BGP: 'wait-for-bgp';

WAIT_START: 'wait-start';

WARNING: 'warning';

WARNINGS: 'warnings';

WARNTIME: 'warntime';

WATCHDOG: 'watchdog';

WATCH_LIST: 'watch-list';

WCCP: 'wccp';

WEB_CACHE: 'web-cache';

WEB_HTTPS_PORT_443: 'web-https-port-443';

WEB_MAX_CLIENTS: 'web-max-clients';

WEB_SERVER: 'web-server';

WEBAUTH: 'webauth';

WEBVPN: 'webvpn';

WED: 'Wed';

WEEKDAY: 'weekday';

WEEKEND: 'weekend';

WEIGHT: 'weight';

WEIGHTING: 'weighting';

WEIGHTS: 'weights';

WELCOME_PAGE: 'welcome-page';

WHITE_LIST: 'white-list';

WHO: 'who';

WHOIS: 'whois';

WIDE: 'wide';

WIDE_METRIC: 'wide-metric';

WINDOW_SIZE: 'window-size';

WINS_SERVER: 'wins-server';

WIRED_AP_PROFILE: 'wired-ap-profile';

WIRED_CONTAINMENT: 'wired-containment';

WIRED_PORT_PROFILE: 'wired-port-profile';

WIRED_TO_WIRELESS_ROAM: 'wired-to-wireless-roam';

WIRELESS_CONTAINMENT: 'wireless-containment';

WISM: 'wism';

WITHOUT_CSD: 'without-csd';

WLAN: 'wlan';

WMM: 'wmm';

WMS_GENERAL_PROFILE: 'wms-general-profile';

WMS_LOCAL_SYSTEM_PROFILE: 'wms-local-system-profile';

WPA_FAST_HANDOVER: 'wpa-fast-handover';

WRED: 'wred';

WRED_PROFILE: 'wred-profile';

WRITE_MEMORY: 'write-memory';

WRR: 'wrr';

WRR_QUEUE: 'wrr-queue';

WSMA: 'wsma';

WWW: 'www';

X25: 'x25';

X29: 'x29';

XCONNECT: 'xconnect';

XDMCP: 'xdmcp';

XDR: 'xdr';

XLATE: 'xlate';

XML
:
   'XML'
   | 'xml'
;

XML_CONFIG: 'xml-config';

XNS_CH: 'xns-ch';

XNS_MAIL: 'xns-mail';

XNS_TIME: 'xns-time';

YELLOW: 'yellow';

Z39_50: 'z39-50';

ZONE: 'zone';

/* Other Tokens */

MULTICONFIGPART
:
   '############ MultiConfigPart' F_NonNewline* F_Newline -> channel ( HIDDEN
   )
;

POUND
:
   '#' -> pushMode ( M_Description )
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

COMMENT_LINE
:
  F_Whitespace* [!#]
  {
    // TODO: in JDK 12. can use inline switch case
    ((java.util.function.Supplier<Boolean>)() -> {
      switch(lastTokenType()) {
        case -1:
        case NEWLINE:
          return true;
        default:
          return false;
      }}).get()
  }?
  F_NonNewline* F_Newline -> channel ( HIDDEN )
;

COMMENT_TAIL
:
   '!' F_NonNewline* -> channel ( HIDDEN )
;

DASH: '-';

DOLLAR
:
   '$'
;

UINT8: F_Uint8;
UINT16: F_Uint16;
UINT32: F_Uint32;
UINT64: F_Uint64;
UINT_BIG: F_UintBig;

DOUBLE_QUOTE
:
   '"'
;

EQUALS
:
   '='
;

FLOAT
:
   (
      F_PositiveDigit* F_Digit '.' F_Digit+
   ) {lastTokenType() == SPEED}?
;

FORWARD_SLASH
:
   '/'
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

NEWLINE: F_Newline;

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

UNDERSCORE: '_';

WS
:
   F_Whitespace+ -> channel ( HIDDEN )
; // Fragments

VARIABLE
:
  F_Variable_VarChar* F_Variable_RequiredVarChar F_Variable_VarChar*
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
F_NameChar
:
  ~[ \t\r\n\u000C\u00A0(){}"]
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
   ~( ' ' | '\t' | '\u000C' | '\u00A0' | '\n' | '\r' )
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
  '0'
  | F_PositiveDigit F_Digit F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit?
  | [1-3] F_Digit F_Digit F_Digit F_Digit F_FiveDigits
  | '4' [0-1] F_Digit F_Digit F_Digit F_FiveDigits
  | '42' [0-8] F_Digit F_Digit F_FiveDigits
  | '429' [0-3] F_Digit F_FiveDigits
  | '4294' [0-8] F_FiveDigits
  | '42949' [0-5] F_Digit F_Digit F_Digit F_Digit
  | '429496' [0-6] F_Digit F_Digit F_Digit
  | '4294967' [0-1] F_Digit F_Digit
  | '42949672' [0-8] F_Digit
  | '429496729' [0-5]
;

fragment
F_FiveDigits
:
  F_Digit F_Digit F_Digit F_Digit F_Digit
;

fragment
F_Uint64
:
// 0-18446744073709551615
    '1844674407370955161' [0-5]
  | '184467440737095516' '0' F_Digit
  | '18446744073709551' [0-5] F_Digit F_Digit
  | '1844674407370955' '0' F_Digit F_Digit F_Digit
  | '184467440737095' [0-4] F_Digit F_Digit F_Digit F_Digit
  | '18446744073709' [0-4] F_FiveDigits
  | '1844674407370' [0-8] F_Digit F_FiveDigits
     // nothing lower than 0 for thirteenth digit
  | '18446744073' [0-6] F_Digit F_Digit F_Digit F_FiveDigits
  | '1844674407' [0-2] F_Digit F_Digit F_Digit F_Digit F_FiveDigits
  | '184467440' [0-6] F_FiveDigits F_FiveDigits
     // nothing lower than 0 for ninth digit
  | '1844674' [0-3] F_Digit F_Digit F_FiveDigits F_FiveDigits
  | '184467' [0-3] F_Digit F_Digit F_Digit F_FiveDigits F_FiveDigits
  | '18446' [0-6] F_Digit F_Digit F_Digit F_Digit F_FiveDigits F_FiveDigits
  | '1844' [0-5] F_FiveDigits F_FiveDigits F_FiveDigits
  | '184' [0-3] F_Digit F_FiveDigits F_FiveDigits F_FiveDigits
  | '18' [0-3] F_Digit F_Digit F_FiveDigits F_FiveDigits F_FiveDigits
  | '1' [0-7] F_Digit F_Digit F_Digit F_FiveDigits F_FiveDigits F_FiveDigits
  // All the non-zero numbers from 1-19 digits
  | F_PositiveDigit F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit? F_Digit?
  // Zero
  | '0'
;

fragment
F_UintBig
:
  F_PositiveDigit F_Digit*
  | '0'
;

fragment
F_UpperCaseLetter
:
   'A' .. 'Z'
;

fragment
F_Variable_RequiredVarChar
:
   ~( '0' .. '9' | '-' | [ \t\u000C\u00A0\n\r(),!+$'"*#] | '[' | ']' | [/.] | ':' )
;

fragment
F_Variable
:
   F_Variable_VarChar* F_Variable_RequiredVarChar F_Variable_VarChar*
;

fragment
F_Variable_VarChar
:
   ~( [ \t\u000C\u00A0\n\r(),!$'"*#] | '[' | ']' )
;

fragment
F_Variable_VarChar_Ipv6
:
   ~( [ \t\u000C\u00A0\n\r(),!$'"*#] | '[' | ']' | ':' )
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
F_WordChar
:
  ~( [ \t\u000C\u00A0\n\r(),!$'"*#] | '[' | ']' )
;

fragment
F_Word
:
  F_WordChar+
;

fragment
F_Parameter
:
  '$' [A-Za-z0-9_]+
;

fragment
F_LocalAs
:
  [Ll][Oo][Cc][Aa][Ll]'-'[Aa][Ss]
;

fragment
F_AsPathRegexChar
:
  ~['&<>\r\n]
;

fragment
F_AsPathRegex
:
  F_AsPathRegexChar+
;

fragment
F_CommunitySetRegexComponentChar
:
  ~[':&<> ]
;

fragment
F_CommunitySetRegex
:
  ['] F_CommunitySetRegexComponentChar* ':' F_CommunitySetRegexComponentChar* [']
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

mode M_Authentication;

M_Authentication_DOUBLE_QUOTE
:
   '"' -> mode ( M_DoubleQuote )
;

M_Authentication_BANNER
:
  'banner' F_Whitespace+ -> type ( BANNER ), mode ( M_BannerIosDelimiter )
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

M_Authentication_UINT_BIG
:
  F_UintBig -> type (UINT_BIG) , popMode
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

M_Authentication_KEY_CHAIN
:
   'key-chain' -> type ( KEY_CHAIN ) , popMode
;

M_Authentication_KEYED_SHA1
:
   [kK][eE][yY][eE][dD]'-'[sS][hH][aA]'1' -> type ( KEYED_SHA1 ) , popMode
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

M_Authentication_MODE
:
   'mode' -> type ( MODE ) , popMode
;

M_Authentication_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
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

M_Authentication_RSA_ENCR
:
   'rsa-encr' -> type ( RSA_ENCR ) , popMode
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

mode M_BannerIosDelimiter;
// whitespace should have been consumed before entering this mode

M_BannerIosDelimiter_BANNER_DELIMITER_IOS
:
  (
    '^C'
    | F_NonWhitespace
  )
  {
    setBannerIosDelimiter();
  } -> type(BANNER_DELIMITER_IOS), mode(M_BannerIosText)
;

M_BannerIosDelimiter_NEWLINE
:
  // illegal, but pop anyway and let parser deal with it
  F_Newline -> type(NEWLINE), popMode
;

mode M_BannerIosText;

M_BannerIosText_BANNER_DELIMITER_IOS
:
  {bannerIosDelimiterFollows()}?

  .
  {
    unsetBannerIosDelimiter();
  } -> type ( BANNER_DELIMITER_IOS ) , mode ( M_BannerIosCleanup )
;

M_BannerIosText_BODY
:
  .
  {
    if (bannerIosDelimiterFollows()) {
      setType(BANNER_BODY);
    } else {
      more();
    }
  }
;

mode M_BannerIosCleanup;

M_BannerIosCleanup_IGNORED
:
  F_NonNewline+ -> channel ( HIDDEN )
;

M_BannerIosCleanup_NEWLINE
:
  F_Newline -> type ( NEWLINE ) , popMode
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
   F_Newline -> type ( NEWLINE ) , popMode
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
   F_Newline -> type ( NEWLINE ) , popMode
;

M_COMMENT_NON_NEWLINE
:
   F_NonNewline+
;

mode M_Description;

M_Description_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
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

M_Extcommunity_RT
:
   'rt' -> type ( RT ), mode(M_Name)
;

M_Extcommunity_WS
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

M_Interface_EIGRP
:
   'eigrp' -> type ( EIGRP ) , popMode
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

M_Interface_LINE_PROTOCOL
:
   'line-protocol' -> type ( LINE_PROTOCOL ) , popMode
;

M_Interface_MULTIPOINT
:
   'multipoint' -> type ( MULTIPOINT ) , popMode
;

M_Interface_SCOPE: 'scope' -> type(SCOPE), popMode;

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
   F_Newline -> type ( NEWLINE ) , popMode
;

M_Interface_NUMBER: F_UintBig -> type (UINT_BIG);

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

mode M_Hostname;

M_Hostname_WORD: ([A-Za-z0-9_.] | '-')+ -> type(WORD), popMode;

M_Hostname_NEWLINE: F_Newline -> type(NEWLINE), popMode;

M_Hostname_WS: F_Whitespace+ -> channel(HIDDEN);

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

mode M_Name;

M_Name_NAME
:
   (
      F_NameChar+
      | '"' ~'"'* '"'
   )  -> type ( VARIABLE ) , popMode
;

M_Name_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
;

M_Name_PAREN_LEFT
:
  '(' -> type(PAREN_LEFT), popMode
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
   F_IpAddress -> type ( IP_ADDRESS ) ,  popMode
;

M_NEIGHBOR_IP_PREFIX
:
   F_IpPrefix -> type ( IP_PREFIX ) , popMode
;

M_NEIGHBOR_IPV6_ADDRESS
:
   F_Ipv6Address -> type ( IPV6_ADDRESS ) , popMode
;

M_NEIGHBOR_IPV6_PREFIX
:
   F_Ipv6Prefix -> type ( IPV6_PREFIX ) , popMode
;

M_NEIGHBOR_NLRI
:
   'nlri' -> type ( NLRI ) , popMode
;

M_NEIGHBOR_PASSIVE
:
   'passive' -> type ( PASSIVE ) , popMode
;

M_NEIGHBOR_SRC_IP
:
   'src-ip' -> type ( SRC_IP ) , popMode
;

M_NEIGHBOR_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
;

M_NEIGHBOR_VARIABLE
:
   F_Variable_VarChar+ -> type ( VARIABLE ) , popMode
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

M_ObjectGroup_ICMP_TYPE
:
  'icmp-type' -> type ( ICMP_TYPE ) , popMode
;

M_ObjectGroup_GROUP
:
  'group' -> type ( GROUP ) , popMode
;

/* Do not reorder above literals */
M_ObjectGroup_NAME
:
   F_NonWhitespace+ -> type ( VARIABLE ) , popMode
;

M_ObjectGroup_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
;

M_ObjectGroup_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_REMARK;

M_REMARK_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
;

M_REMARK_REMARK
:
   F_NonNewline+
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

mode M_SnmpServerCommunity;

M_SnmpServerCommunity_COMMUNITY
:
  'community' -> type ( COMMUNITY )
;

M_SnmpServerCommunity_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

M_SnmpServerCommunity_DOUBLE_QUOTE
:
   '"' -> type ( DOUBLE_QUOTE ), mode ( M_DoubleQuote )
;

M_SnmpServerCommunity_CHAR
:
   F_NonWhitespace -> mode ( M_Name ), more
;

mode M_SshKey;

M_SshKey_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
;

M_SshKey_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Word;

M_Word_WORD: F_Word -> type(WORD), popMode;

M_Word_NEWLINE: F_Newline -> type(NEWLINE), popMode;

M_Word_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_Words;

M_Words_WORD
:
   F_NonWhitespace+ -> type ( VARIABLE )
;

M_Words_NEWLINE
:
   F_Newline -> type ( NEWLINE ) , popMode
;

M_Words_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;


mode M_AsPath;

M_AsPath_CONFED: 'confed' -> type (CONFED), popMode;
M_AsPath_IN: 'in' -> type(IN), mode(M_AsPathSetInline);
M_AsPath_IS_LOCAL: 'is-local' -> type(IS_LOCAL), popMode;
M_AsPath_LENGTH: 'length' -> type(LENGTH), mode(M_AsPathLength);
M_AsPath_MULTIPATH_RELAX: 'multipath-relax' -> type (MULTIPATH_RELAX), popMode;
M_AsPath_NEIGHBOR_IS: 'neighbor-is' -> type(NEIGHBOR_IS), mode(M_AsPathSetInlineElemAsRangeList);
M_AsPath_ORIGINATES_FROM: 'originates-from' -> type(ORIGINATES_FROM), mode(M_AsPathSetInlineElemAsRangeList);
M_AsPath_PASSES_THROUGH: 'passes-through' -> type (PASSES_THROUGH), mode(M_AsPathSetInlineElemAsRangeList);
M_AsPath_UNIQUE_LENGTH: 'unique-length' -> type(UNIQUE_LENGTH), mode(M_AsPathLength);

M_AsPath_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPath_WS: F_Whitespace+ -> channel(HIDDEN);

// as-path-set definition
mode M_AsPathSet;

M_AsPathSet_WORD: F_Word -> type(WORD);
M_AsPathSet_NEWLINE: F_Newline -> type(NEWLINE), mode(M_AsPathSetElem);
M_AsPathSet_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathSetElem;

M_AsPathSetElem_ALL: 'all' -> type(ALL);
M_AsPathSetElem_COMMA: ',' -> type(COMMA);
M_AsPathSetElem_DFA_REGEX: 'dfa-regex' -> type(DFA_REGEX), pushMode(M_AsPathRegex);
M_AsPathSetElem_EXACT: 'exact' -> type(EXACT);
M_AsPathSetElem_IOS_REGEX: 'ios-regex' -> type(IOS_REGEX), pushMode(M_AsPathRegex);
M_AsPathSetElem_LENGTH: 'length' -> type(LENGTH), pushMode(M_AsPathLength);
M_AsPathSetElem_NEIGHBOR_IS: 'neighbor-is' -> type(NEIGHBOR_IS), pushMode(M_AsPathSetElemAsRangeList);
M_AsPathSetElem_ORIGINATES_FROM: 'originates-from' -> type(ORIGINATES_FROM), pushMode(M_AsPathSetElemAsRangeList);
M_AsPathSetElem_PASSES_THROUGH: 'passes-through' -> type(PASSES_THROUGH), pushMode(M_AsPathSetElemAsRangeList);
M_AsPathSetElem_UNIQUE_LENGTH: 'unique-length' -> type(UNIQUE_LENGTH), pushMode(M_AsPathLength);

M_AsPathSetElem_END_SET: 'end-set' -> type(END_SET), popMode;

// TODO: save remarks
M_AsPathSetElem_REMARK: F_Whitespace* '#' F_NonNewline* F_Newline {lastTokenType() == NEWLINE}? -> skip;

M_AsPathSetElem_NEWLINE: F_Newline -> type(NEWLINE);
M_AsPathSetElem_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathLength;
M_AsPathLength_GE: 'ge' -> type(GE);
M_AsPathLength_EQ: 'eq' -> type(EQ);
M_AsPathLength_IS: 'is' -> type(IS);
M_AsPathLength_LE: 'le' -> type(LE);

M_AsPathLength_PARAMETER: F_Parameter -> type(PARAMETER), popMode;
M_AsPathLength_UINT16: F_Uint16 -> type(UINT16), popMode;

M_AsPathLength_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathLength_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathRegex;

M_AsPathRegex_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), mode(M_AsPathRegex2);

M_AsPathRegex_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathRegex_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathRegex2;

M_AsPathRegex2_AS_PATH_REGEX: F_AsPathRegex -> type(AS_PATH_REGEX);
M_AsPathRegex2_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), popMode;

M_AsPathRegex2_NEWLINE: F_Newline -> type(NEWLINE), popMode;

mode M_AsPathSetElemAsRangeList;
M_AsPathSetElemAsRangeList_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), mode(M_AsPathSetElemAsRangeList2);

M_AsPathSetElemAsRangeList_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathSetElemAsRangeList_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathSetElemAsRangeList2;
M_AsPathSetElemAsRangeList2_BRACKET_LEFT: '[' -> type(BRACKET_LEFT);
M_AsPathSetElemAsRangeList2_BRACKET_RIGHT: ']' -> type(BRACKET_RIGHT);
M_AsPathSetElemAsRangeList2_DOTDOT: '..' -> type(DOTDOT);
M_AsPathSetElemAsRangeList2_PERIOD: '.' -> type(PERIOD);
M_AsPathSetElemAsRangeList2_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), popMode;
M_AsPathSetElemAsRangeList2_UINT16: F_Uint16 -> type(UINT16);
M_AsPathSetElemAsRangeList2_UINT32: F_Uint32 -> type(UINT32);

M_AsPathSetElemAsRangeList2_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathSetElemAsRangeList2_WS: F_Whitespace+ -> channel(HIDDEN);

// inline as-path-set
mode M_AsPathSetInline;

M_AsPathSetInline_PARAMETER: F_Parameter -> type(PARAMETER), popMode;
M_AsPathSetInline_PAREN_LEFT: '(' -> type(PAREN_LEFT), mode(M_AsPathSetInlineElem);
M_AsPathSetInline_WORD: F_Word -> type(WORD), popMode;
M_AsPathSetInline_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathSetInline_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathSetInlineElem;

M_AsPathSetInlineElem_ALL: 'all' -> type(ALL);
M_AsPathSetInlineElem_COMMA: ',' -> type(COMMA);
M_AsPathSetInlineElem_DFA_REGEX: 'dfa-regex' -> type(DFA_REGEX), pushMode(M_AsPathRegex);
M_AsPathSetInlineElem_END_SET: 'end-set' -> type(END_SET), popMode;
M_AsPathSetInlineElem_EXACT: 'exact' -> type(EXACT);
M_AsPathSetInlineElem_IOS_REGEX: 'ios-regex' -> type(IOS_REGEX), pushMode(M_AsPathRegex);
M_AsPathSetInlineElem_LENGTH: 'length' -> type(LENGTH), pushMode(M_AsPathLength);
M_AsPathSetInlineElem_NEIGHBOR_IS: 'neighbor-is' -> type(NEIGHBOR_IS), pushMode(M_AsPathSetInlineElemAsRangeList);
M_AsPathSetInlineElem_ORIGINATES_FROM: 'originates-from' -> type(ORIGINATES_FROM), pushMode(M_AsPathSetInlineElemAsRangeList);
M_AsPathSetInlineElem_PASSES_THROUGH: 'passes-through' -> type(PASSES_THROUGH), pushMode(M_AsPathSetInlineElemAsRangeList);
M_AsPathSetInlineElem_UNIQUE_LENGTH: 'unique-length' -> type(UNIQUE_LENGTH), pushMode(M_AsPathLength);

M_AsPathSetInlineElem_PAREN_RIGHT: ')' -> type(PAREN_RIGHT), popMode;
M_AsPathSetInlineElem_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathSetInlineElem_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathSetInlineElemAsRangeList;
M_AsPathSetInlineElemAsRangeList_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), mode(M_AsPathSetInlineElemAsRangeList2);

M_AsPathSetInlineElemAsRangeList_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathSetInlineElemAsRangeList_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_AsPathSetInlineElemAsRangeList2;
M_AsPathSetInlineElemAsRangeList2_BRACKET_LEFT: '[' -> type(BRACKET_LEFT);
M_AsPathSetInlineElemAsRangeList2_BRACKET_RIGHT: ']' -> type(BRACKET_RIGHT);
M_AsPathSetInlineElemAsRangeList2_DOTDOT: '..' -> type(DOTDOT);
M_AsPathSetInlineElemAsRangeList2_PERIOD: '.' -> type(PERIOD);
M_AsPathSetInlineElemAsRangeList2_PARAMETER: F_Parameter -> type(PARAMETER);
M_AsPathSetInlineElemAsRangeList2_SINGLE_QUOTE: ['] -> type(SINGLE_QUOTE), popMode;
M_AsPathSetInlineElemAsRangeList2_UINT16: F_Uint16 -> type(UINT16);
M_AsPathSetInlineElemAsRangeList2_UINT32: F_Uint32 -> type(UINT32);

M_AsPathSetInlineElemAsRangeList2_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_AsPathSetInlineElemAsRangeList2_WS: F_Whitespace+ -> channel(HIDDEN);

// community-set definition
mode M_CommunitySet;

M_CommunitySet_WORD: F_Word -> type(WORD);
M_CommunitySet_NEWLINE: F_Newline -> type(NEWLINE), mode(M_CommunitySetElem);
M_CommunitySet_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_CommunitySetElem;
M_CommunitySetElem_ACCEPT_OWN: 'accept-own' -> type(ACCEPT_OWN);
M_CommunitySetElem_DFA_REGEX: 'dfa-regex' -> type(DFA_REGEX), pushMode(M_CommunitySetRegex);
M_CommunitySetElem_GRACEFUL_SHUTDOWN: 'graceful-shutdown' -> type(GRACEFUL_SHUTDOWN);
M_CommunitySetElem_INTERNET: 'internet' -> type(INTERNET);
M_CommunitySetElem_IOS_REGEX: 'ios-regex' -> type(IOS_REGEX), pushMode(M_CommunitySetRegex);
M_CommunitySetElem_LOCAL_AS: F_LocalAs -> type(LOCAL_AS);
M_CommunitySetElem_NO_ADVERTISE: 'no-advertise' -> type(NO_ADVERTISE);
M_CommunitySetElem_NO_EXPORT: 'no-export' -> type(NO_EXPORT);
M_CommunitySetElem_PRIVATE_AS: 'private-as' -> type(PRIVATE_AS);

M_CommunitySetElem_ASTERISK: '*' -> type(ASTERISK);
M_CommunitySetElem_BRACKET_LEFT: '[' -> type(BRACKET_LEFT);
M_CommunitySetElem_BRACKET_RIGHT: ']' -> type(BRACKET_RIGHT);
M_CommunitySetElem_COMMA: ',' -> type(COMMA);
M_CommunitySetElem_DOTDOT: '..' -> type(DOTDOT);
M_CommunitySetElem_END_SET: 'end-set' -> type(END_SET), popMode;
M_CommunitySetElem_UINT16: F_Uint16 -> type(UINT16);
M_CommunitySetElem_COLON: ':' -> type(COLON);

// NEWLINE can be interspersed between any other tokens in this mode
M_CommunitySetElem_NEWLINE: F_Newline -> type(NEWLINE);

// TODO: save remarks
M_CommunitySetElem_REMARK: F_Whitespace* '#' F_NonNewline* F_Newline {lastTokenType() == NEWLINE}? -> skip;

M_CommunitySetElem_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_CommunitySetRegex;

M_CommunitySetRegex_COMMUNITY_SET_REGEX: F_CommunitySetRegex -> type(COMMUNITY_SET_REGEX), popMode;
M_CommunitySetRegex_WS: F_Whitespace+ -> channel(HIDDEN);

// route-policy set community expression
mode M_CommunitySetExpr;

M_CommunitySetExpr_PARAMETER: F_Parameter -> type(PARAMETER), popMode;
M_CommunitySetExpr_PAREN_LEFT: '(' -> type(PAREN_LEFT), mode(M_CommunitySetExprElem);
M_CommunitySetExpr_WORD: F_Word -> type(WORD), popMode;
M_CommunitySetExpr_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_CommunitySetExpr_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_CommunitySetExprElem;

M_CommunitySetExprElem_ACCEPT_OWN: 'accept-own' -> type(ACCEPT_OWN);
M_CommunitySetExprElem_GRACEFUL_SHUTDOWN: 'graceful-shutdown' -> type(GRACEFUL_SHUTDOWN);
M_CommunitySetExprElem_INTERNET: 'internet' -> type(INTERNET);
M_CommunitySetExprElem_LOCAL_AS: F_LocalAs -> type(LOCAL_AS);
M_CommunitySetExprElem_NO_ADVERTISE: 'no-advertise' -> type(NO_ADVERTISE);
M_CommunitySetExprElem_NO_EXPORT: 'no-export' -> type(NO_EXPORT);
M_CommunitySetExprElem_PEERAS: 'peeras' -> type(PEERAS);

M_CommunitySetExprElem_COMMA: ',' -> type(COMMA);
M_CommunitySetExprElem_UINT16: F_Uint16 -> type(UINT16);
M_CommunitySetExprElem_PARAMETER: F_Parameter -> type(PARAMETER);
M_CommunitySetExprElem_PAREN_RIGHT: ')' -> type(PAREN_RIGHT), popMode;
M_CommunitySetExprElem_COLON: ':' -> type(COLON);
M_CommunitySetExprElem_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_CommunitySetExprElem_WS: F_Whitespace+ -> channel(HIDDEN);

// route-policy community matches-{any,all} / delete community in expression
mode M_CommunitySetMatchExpr;

M_CommunitySetMatchExpr_PARAMETER: F_Parameter -> type(PARAMETER), popMode;
M_CommunitySetMatchExpr_PAREN_LEFT: '(' -> type(PAREN_LEFT), mode(M_CommunitySetMatchExprElem);
M_CommunitySetMatchExpr_WORD: F_Word -> type(WORD), popMode;
M_CommunitySetMatchExpr_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_CommunitySetMatchExpr_WS: F_Whitespace+ -> channel(HIDDEN);

// community matches-{any-all} / delete community single community match expression
mode M_CommunitySetMatchExprElem;

M_CommunitySetMatchExprElem_ACCEPT_OWN: 'accept-own' -> type(ACCEPT_OWN);
M_CommunitySetMatchExprElem_DFA_REGEX: 'dfa-regex' -> type(DFA_REGEX), pushMode(M_CommunitySetRegex);
M_CommunitySetMatchExprElem_GRACEFUL_SHUTDOWN: 'graceful-shutdown' -> type(GRACEFUL_SHUTDOWN);
M_CommunitySetMatchExprElem_INTERNET: 'internet' -> type(INTERNET);
M_CommunitySetMatchExprElem_IOS_REGEX: 'ios-regex' -> type(IOS_REGEX), pushMode(M_CommunitySetRegex);
M_CommunitySetMatchExprElem_LOCAL_AS: F_LocalAs -> type(LOCAL_AS);
M_CommunitySetMatchExprElem_NO_ADVERTISE: 'no-advertise' -> type(NO_ADVERTISE);
M_CommunitySetMatchExprElem_NO_EXPORT: 'no-export' -> type(NO_EXPORT);
M_CommunitySetMatchExprElem_PEERAS: 'peeras' -> type(PEERAS);
M_CommunitySetMatchExprElem_PRIVATE_AS: 'private-as' -> type(PRIVATE_AS);

M_CommunitySetMatchExprElem_ASTERISK: '*' -> type(ASTERISK);
M_CommunitySetMatchExprElem_BRACKET_LEFT: '[' -> type(BRACKET_LEFT);
M_CommunitySetMatchExprElem_BRACKET_RIGHT: ']' -> type(BRACKET_RIGHT);
M_CommunitySetMatchExprElem_COMMA: ',' -> type(COMMA);
M_CommunitySetMatchExprElem_DOTDOT: '..' -> type(DOTDOT);
M_CommunitySetMatchExprElem_UINT16: F_Uint16 -> type(UINT16);
M_CommunitySetMatchExprElem_PARAMETER: F_Parameter -> type(PARAMETER);
M_CommunitySetMatchExprElem_PAREN_RIGHT: ')' -> type(PAREN_RIGHT), popMode;
M_CommunitySetMatchExprElem_COLON: ':' -> type(COLON);
M_CommunitySetMatchExprElem_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_CommunitySetMatchExprElem_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_DeleteCommunity;

M_DeleteCommunity_ALL: 'all' -> type(ALL), popMode;
M_DeleteCommunity_NOT: 'not' -> type(NOT);
M_DeleteCommunity_IN: 'in' -> type(IN), mode(M_CommunitySetMatchExpr);

M_DeleteCommunity_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_DeleteCommunity_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_DistributeList;

M_DistributeList_PREFIX_LIST: 'prefix-list' -> type(PREFIX_LIST), mode(M_Word);
M_DistributeList_ROUTE_POLICY: 'route-policy' -> type(ROUTE_POLICY), mode(M_Word);
M_DistributeList_WORD: F_Word -> type(WORD), popMode;

M_DistributeList_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_DistributeList_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_InterfaceAccessGroup;

M_InterfaceAccessGroup_COMMON: 'common' -> type(COMMON), mode(M_InterfaceAccessGroupCommon1);

M_InterfaceAccessGroup_WORD: F_Word -> type(WORD), popMode; // interface acl name
M_InterfaceAccessGroup_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_InterfaceAccessGroup_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_InterfaceAccessGroupCommon1;

M_InterfaceAccessGroupCommon1_WORD: F_Word -> type(WORD), mode(M_InterfaceAccessGroupCommon2); // common acl name
M_InterfaceAccessGroupCommon1_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_InterfaceAccessGroupCommon1_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_InterfaceAccessGroupCommon2;

M_InterfaceAccessGroupCommon2_INGRESS: 'ingress' -> type(INGRESS), popMode;

M_InterfaceAccessGroupCommon2_WORD: F_Word -> type(WORD), popMode; // interface acl name
M_InterfaceAccessGroupCommon2_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_InterfaceAccessGroupCommon2_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_NtpAccessGroup;

M_NtpAccessGroup_IPV4: 'ipv4' -> type(IPV4);
M_NtpAccessGroup_IPV6: 'ipv6' -> type(IPV6);
M_NtpAccessGroup_PEER: 'peer' -> type(PEER), mode(M_Word);
M_NtpAccessGroup_QUERY_ONLY: 'query-only' -> type(QUERY_ONLY), mode(M_Word);
M_NtpAccessGroup_SERVE: 'serve' -> type(SERVE), mode(M_Word);
M_NtpAccessGroup_SERVE_ONLY: 'serve-only' -> type(SERVE_ONLY), mode(M_Word);
M_NtpAccessGroup_VRF: 'vrf' -> type(VRF), pushMode(M_Word);

M_NtpAccessGroup_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_NtpAccessGroup_WS: F_Whitespace+ -> channel(HIDDEN);

// community-set definition
mode M_RdSet;

M_RdSet_WORD: F_Word -> type(WORD);
M_RdSet_NEWLINE: F_Newline -> type(NEWLINE), mode(M_RdSetElem);
M_RdSet_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_RdSetElem;
M_RdSetElem_DFA_REGEX: 'dfa-regex' -> type(DFA_REGEX), pushMode(M_CommunitySetRegex);
M_RdSetElem_IOS_REGEX: 'ios-regex' -> type(IOS_REGEX), pushMode(M_CommunitySetRegex);

M_RdSetElem_ASTERISK: '*' -> type(ASTERISK);
M_RdSetElem_COMMA: ',' -> type(COMMA);
M_RdSetElem_END_SET: 'end-set' -> type(END_SET), popMode;
M_RdSetElem_IP_ADDRESS: F_IpAddress -> type(IP_ADDRESS);
M_RdSetElem_IP_PREFIX: F_IpPrefix -> type(IP_PREFIX);
M_RdSetElem_PERIOD: '.' -> type(PERIOD);
M_RdSetElem_UINT16: F_Uint16 -> type(UINT16);
M_RdSetElem_UINT32: F_Uint32 -> type(UINT32);
M_RdSetElem_COLON: ':' -> type(COLON);

// NEWLINE can be interspersed between any other tokens in this mode
M_RdSetElem_NEWLINE: F_Newline -> type(NEWLINE);

// TODO: save remarks
M_RdSetElem_REMARK: F_Whitespace* '#' F_NonNewline* F_Newline {lastTokenType() == NEWLINE}? -> skip;

M_RdSetElem_WS: F_Whitespace+ -> channel(HIDDEN);

// route-policy rd in expression
mode M_RdSetMatchExpr;

M_RdSetMatchExpr_PARAMETER: F_Parameter -> type(PARAMETER), popMode;
M_RdSetMatchExpr_WORD: F_Word -> type(WORD), popMode;
M_RdSetMatchExpr_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_RdSetMatchExpr_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_Rd;

M_Rd_IN: 'in' -> type(IN), mode(M_RdMatchExpr);

M_Rd_COLON: ':' -> type(COLON);
M_Rd_IP_ADDRESS: F_IpAddress -> type(IP_ADDRESS);
M_Rd_PERIOD: '.' -> type(PERIOD);
M_Rd_UINT16: F_Uint16 -> type(UINT16);
M_Rd_UINT32: F_Uint16 -> type(UINT16);
M_Rd_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_Rd_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_RdMatchExpr;

// TODO: support inline `rd in (...)` expression
M_RdMatchExpr_PAREN_LEFT: '(' -> type(PAREN_LEFT), popMode;

M_RdMatchExpr_PARAMETER: F_Parameter -> type(PARAMETER), popMode;
M_RdMatchExpr_WORD: F_Word -> type(WORD), popMode;
M_RdMatchExpr_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_RdMatchExpr_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_ExplicitTracking;

M_ExplicitTracking_DISABLE: 'disable' -> type(DISABLE), popMode;

M_ExplicitTracking_WORD: F_Word -> type(WORD), popMode;
M_ExplicitTracking_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_ExplicitTracking_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_GroupsPerInterface;

M_GroupsPerInterface_UINT16: F_Uint16 -> type(UINT16), mode(M_GroupsPerInterface2);

M_GroupsPerInterface_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_GroupsPerInterface_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_GroupsPerInterface2;

M_GroupsPerInterface2_THRESHOLD: 'threshold' -> type(THRESHOLD), mode(M_GroupsPerInterfaceThreshold);
M_GroupsPerInterface2_WORD: F_Word -> type(WORD), popMode;

M_GroupsPerInterface2_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_GroupsPerInterface2_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_GroupsPerInterfaceThreshold;

M_GroupsPerInterfaceThreshold_1: '1' -> type(ONE_LITERAL), mode(M_Word);

M_GroupsPerInterfaceThreshold_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_GroupsPerInterfaceThreshold_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_SsmMapStatic;

M_SsmMapStatic_IP_ADDRESS: F_IpAddress -> type(IP_ADDRESS), mode(M_Word);
M_SsmMapStatic_IPV6_ADDRESS: F_Ipv6Address -> type(IPV6_ADDRESS), mode(M_Word);

M_SsmMapStatic_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_SsmMapStatic_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_Flow;

M_Flow_EXPORTER_MAP: 'exporter-map' -> type(EXPORTER_MAP), mode(M_Word);
M_Flow_IPV4: 'ipv4' -> type(IPV4), mode(M_InterfaceFlow);
M_Flow_IPV6: 'ipv6' -> type(IPV6), mode(M_InterfaceFlow);
M_Flow_MONITOR_MAP: 'monitor-map' -> type(MONITOR_MAP), mode(M_Word);

M_Flow_WORD: F_Word -> type(WORD), popMode;
M_Flow_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_Flow_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_InterfaceFlow;

M_InterfaceFlow_EGRESS: 'egress' -> type(EGRESS), popMode;
M_InterfaceFlow_INGRESS: 'ingress' -> type(INGRESS), popMode;
M_InterfaceFlow_MONITOR: 'monitor' -> type(MONITOR), pushMode(M_Word);
M_InterfaceFlow_SAMPLER: 'sampler' -> type(SAMPLER), pushMode(M_Word);

M_InterfaceFlow_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_InterfaceFlow_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_RpAddress;

M_RpAddress_IP_ADDRESS: F_IpAddress -> type(IP_ADDRESS), mode(M_Word);
M_RpAddress_IPV6_ADDRESS: F_Ipv6Address -> type(IPV6_ADDRESS), mode(M_Word);
M_RpAddress_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_RpAddress_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_VrfName;

M_VrfName_ADVERTISE_AS_VPN: 'advertise-as-vpn' -> type(ADVERTISE_AS_VPN), popMode;
M_VrfName_ALLOW_IMPORTED_VPN: 'allow-imported-vpn' -> type(ALLOW_IMPORTED_VPN), popMode;
M_VrfName_WORD: F_Word -> type(WORD), popMode;

M_VrfName_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_VrfName_WS: F_Whitespace+ -> channel(HIDDEN);

mode M_Prepend;

M_Prepend_AS_PATH: 'as-path' -> type(AS_PATH);

M_Prepend_UINT16: F_Uint16 -> type(UINT16);
M_Prepend_UINT32: F_Uint32 -> type(UINT32);
M_Prepend_NEWLINE: F_Newline -> type(NEWLINE), popMode;
M_Prepend_WS: F_Whitespace+ -> channel(HIDDEN);

