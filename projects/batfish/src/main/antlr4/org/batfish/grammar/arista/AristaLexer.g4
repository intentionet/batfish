lexer grammar AristaLexer;

options {
   superClass = 'org.batfish.grammar.arista.parsing.AristaBaseLexer';
}

tokens {
   BANNER_DELIMITER_EOS,
   BANNER_BODY,
   HEX_FRAGMENT,
   IS_LOCAL,
   ISO_ADDRESS,
   PAREN_LEFT_LITERAL,
   PAREN_RIGHT_LITERAL,
   PASSWORD_SEED,
   PIPE,
   QUOTED_TEXT,
   RAW_TEXT,
   SELF_SIGNED,
   STATEFUL_DOT1X,
   STATEFUL_KERBEROS,
   STATEFUL_NTLM,
   TEXT,
   WIRED,
   WISPR,
   WORD
} 

// Cisco Keywords

AAA: 'aaa';

AAA_PROFILE: 'aaa-profile';

AAA_USER: 'aaa-user';

ABSOLUTE_TIMEOUT: 'absolute-timeout';

ACAP: 'acap';

ACCEPT: 'accept';

ACCEPT_DIALIN: 'accept-dialin';

ACCEPT_LIFETIME: 'accept-lifetime';

ACCEPT_OWN: 'accept-own';

ACCEPT_REGISTER: 'accept-register';

ACCEPT_RP: 'accept-rp';

ACCESS: 'access';

ACCESS_CLASS: 'access-class';

ACCESS_GROUP: 'access-group';

ACCESS_LIST
:
  'access-list'
  {
    if (lastTokenType() == IP || lastTokenType() == IPV6) {
      pushMode(M_Ip_access_list);
    }
  }
;

ACCESS_SESSION: 'access-session';

ACCOUNTING: 'accounting';

ACCOUNTING_SERVER_GROUP: 'accounting-server-group';

ACCT_PORT: 'acct-port';

ACFE: 'acfe';

ACK: 'ack';

ACL: 'acl';

ACR_NEMA: 'acr-nema';

ACTION: 'action';

ACTION_TYPE: 'action-type';

ACTIVATE: 'activate';

ACTIVATED_SERVICE_TEMPLATE: 'activated-service-template';

ACTIVATION_CHARACTER: 'activation-character';

ACTIVE: 'active';

ADD: 'add';

ADDITIONAL_PATHS: 'additional-paths';

ADDITIVE: 'additive';

ADDRESS: 'address';

ADDRESS_FAMILY: 'address-family';

ADDRESS_HIDING: 'address-hiding';

ADDRESS_POOL: 'address-pool';

ADDRGROUP: 'addrgroup';

ADJACENCY: 'adjacency';

ADJACENCY_CHECK: 'adjacency-check';

ADJUST: 'adjust';

ADMIN_DIST: 'admin-dist';

ADMIN_DISTANCE: 'admin-distance';

ADMIN_STATE: 'admin-state';

ADMIN_VDC: 'admin-vdc';

ADMINISTRATIVELY_PROHIBITED: 'administratively-prohibited';

ADMISSION_CONTROL: 'admission-control';

ADVERTISE: 'advertise';

ADVERTISEMENT: 'advertisement';

ADVERTISE_INACTIVE: 'advertise-inactive';

ADVERTISE_ONLY: 'advertise-only';

AES: 'aes';

AES128: 'aes128';

AES192: 'aes192';

AES256: 'aes256';

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

AGING: 'aging';

AGGREGATE: 'aggregate';

AGGREGATE_ADDRESS: 'aggregate-address';

AGGREGATE_ROUTE: 'aggregate-route';

AH: 'ah';

AH_MD5_HMAC: 'ah-md5-hmac';

AH_SHA_HMAC: 'ah-sha-hmac';

AHP: 'ahp';

AIRGROUPSERVICE: 'airgroupservice';

AIS_SHUT: 'ais-shut';

ALARM: 'alarm';

ALARM_REPORT: 'alarm-report';

ALERT: 'alert';

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

ALL_SUBNETS: 'all-subnets';

ALLOCATE: 'allocate';

ALLOCATION: 'allocation';

ALLOW: 'allow';

ALLOW_CONNECTIONS: 'allow-connections';

ALLOW_DEFAULT: 'allow-default';

ALLOW_FAIL_THROUGH: 'allow-fail-through';

ALLOW_NOPASSWORD_REMOTE_LOGIN: 'allow-nopassword-remote-login';

ALLOWED: 'allowed';

ALLOWAS_IN: 'allowas-in';

ALTERNATE_ADDRESS: 'alternate-address';

ALWAYS: 'always';

ALWAYS_COMPARE_MED: 'always-compare-med';

ALWAYS_ON: 'always-on';

ALWAYS_ON_VPN: 'always-on-vpn';

AM_DISABLE: 'am-disable';

AM_SCAN_PROFILE: 'am-scan-profile';

AMT: 'amt';

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

AP_GROUP: 'ap-group';

AP_NAME: 'ap-name';

AP_RULE_MATCHING: 'ap-rule-matching';

AP_SYSTEM_PROFILE: 'ap-system-profile';

API: 'api';

APP: 'app';

APPCATEGORY: 'appcategory';

APPLICATION: 'application';

ARAP: 'arap';

ARCHIVE: 'archive';

ARCHIVE_LENGTH: 'archive-length';

AREA: 'area';

AREA_PASSWORD: 'area-password';

ARM_PROFILE: 'arm-profile';

ARM_RF_DOMAIN_PROFILE: 'arm-rf-domain-profile';

ARP
:
   'arp'
;

ARNS: 'arns';

AS: 'as';

AS_NUMBER: 'as-number';

AS_PATH
:
   'as-path' -> pushMode ( M_AsPath )
;

AS_RANGE: 'as-range';

ASPATH_CMP_INCLUDE_NEXTHOP: 'aspath-cmp-include-nexthop';

AS_SET: 'as-set';

ASCENDING: 'ascending';

ASCII_AUTHENTICATION: 'ascii-authentication';

ASDM: 'asdm';

ASDM_BUFFER_SIZE: 'asdm-buffer-size';

ASDOT: 'asdot';

ASF_RMCP: 'asf-rmcp';

ASIP_WEBADMIN: 'asip-webadmin';

ASN: 'asn';

ASPLAIN: 'asplain';

ASSIGNMENT: 'assignment';

ASSOC_RETRANSMIT: 'assoc-retransmit';

ASSOCIATE: 'associate';

ASSOCIATION: 'association';

ASYNC: 'async';

ASYNCHRONOUS: 'asynchronous';

ATTACHED_HOST: 'attached-host';

ATTACHED_HOSTS: 'attached-hosts';

ATTACHED_ROUTES: 'attached-routes';

ATM: 'atm';

ATTEMPTS: 'attempts';

ATTRIBUTE: 'attribute';

ATTRIBUTE_MAP: 'attribute-map';

ATTRIBUTE_NAMES: 'attribute-names';

ATTRIBUTE_SET: 'attribute-set';

ATTRIBUTES: 'attributes';

AT_RTMP: 'at-rtmp';

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

AUTHENTICATION_RESTART: 'authentication-restart';

AUTHENTICATION_RETRIES: 'authentication-retries';

AUTHENTICATION_SERVER: 'authentication-server';

AUTHENTICATION_SERVER_GROUP: 'authentication-server-group';

AUTHORITATIVE: 'authoritative';

AUTHORIZATION: 'authorization';

AUTHORIZATION_STATUS: 'authorization-status';

AUTHORIZE: 'authorize';

AUTHORIZED: 'authorized';

AUTO: 'auto';

AUTO_CERT_ALLOW_ALL: 'auto-cert-allow-all';

AUTO_CERT_ALLOWED_ADDRS: 'auto-cert-allowed-addrs';

AUTO_CERT_PROV: 'auto-cert-prov';

AUTO_COST: 'auto-cost';

AUTO_DISCARD: 'auto-discard';

AUTO_IMPORT: 'auto-import';

AUTO_LOCAL_ADDR: 'auto-local-addr';

AUTO_RECOVERY: 'auto-recovery';

AUTO_RP: 'auto-rp';

AUTO_SUMMARY: 'auto-summary';

AUTO_SYNC: 'auto-sync';

AUTO_TUNNEL: 'auto-tunnel';

AUTO_UPGRADE: 'auto-upgrade';

AUTOHANGUP: 'autohangup';

AUTOROUTE: 'autoroute';

AUTORP: 'autorp';

AUTOSELECT: 'autoselect';

AUTOSTATE: 'autostate';

AUX: 'aux';

BACKBONEFAST: 'backbonefast';

BACKOFF_TIME: 'backoff-time';

BACKUP: 'backup';

BAND_STEERING: 'band-steering';

BANDWIDTH: 'bandwidth';

BANDWIDTH_CONTRACT: 'bandwidth-contract';

BANNER
:
  'banner' -> pushMode ( M_Banner )
;

BATCH_SIZE: 'batch-size';

BCMC_OPTIMIZATION: 'bcmc-optimization';

BCN_RPT_REQ_PROFILE: 'bcn-rpt-req-profile';

BEACON: 'beacon';

BESTPATH: 'bestpath';

BEYOND_SCOPE: 'beyond-scope';

BFD: 'bfd';

BFD_ECHO: 'bfd-echo';

BFD_INSTANCE: 'bfd-instance';

BFD_TEMPLATE: 'bfd-template';

BFTP: 'bftp';

BGMP: 'bgmp';

BGP: 'bgp';

BGP_POLICY: 'bgp-policy';

BIDIRECTIONAL: 'bidirectional';

BIDIR_ENABLE: 'bidir-enable';

BIDIR_OFFER_INTERVAL: 'bidir-offer-interval';

BIDIR_OFFER_LIMIT: 'bidir-offer-limit';

BIDIR_RP_LIMIT: 'bidir-rp-limit';

BIFF: 'biff';

BIND: 'bind';

BITTORRENT: 'bittorrent';

BITTORRENT_APPLICATION: 'bittorrent-application';

BKUP_LMS_IP: 'bkup-lms-ip';

BLACKLIST: 'blacklist';

BLACKLIST_TIME: 'blacklist-time';

BLOCK: 'block';

BOOT: 'boot';

BOOT_END_MARKER: 'boot-end-marker';

BOOT_START_MARKER: 'boot-start-marker';

BOOTFILE: 'bootfile';

BOOTPC: 'bootpc';

BOOTPS: 'bootps';

BORDER: 'border';

BORDER_ROUTER: 'border-router';

BOTH: 'both';

BOUNDARY
:
  'boundary'
  {
    if (lastTokenType() == MULTICAST) {
      pushMode(M_Prefix_Or_Standard_Acl);
    }
  }
;

BPDUFILTER: 'bpdufilter';

BPDUGUARD: 'bpduguard';

BREAKOUT: 'breakout';

BRIDGE: 'bridge';

BRIDGE_DOMAIN: 'bridge-domain';

BRIDGE_GROUP: 'bridge-group';

BROADCAST: 'broadcast';

BROADCAST_FILTER: 'broadcast-filter';

BSR: 'bsr';

BSR_BORDER: 'bsr-border';

BSR_CANDIDATE: 'bsr-candidate';

BUCKETS: 'buckets';

BUFFER_LIMIT: 'buffer-limit';

BUFFER_SIZE: 'buffer-size';

BUFFERED: 'buffered';

BUG_ALERT: 'bug-alert';

BUNDLE: 'bundle';

BURST: 'burst';
BURST_SIZE: 'burst-size';

BYPASS: 'bypass';

BYTES: 'bytes';

CA: 'ca';

CABLE: 'cable';

CABLE_DOWNSTREAM: 'cable-downstream';

CABLE_RANGE: 'cable-range';

CABLE_UPSTREAM: 'cable-upstream';

CABLELENGTH
:
   'cablelength' -> pushMode ( M_COMMENT )
;

CACHE: 'cache';

CALL: 'call';

CALL_BLOCK: 'call-block';

CALL_FORWARD: 'call-forward';

CALL_HOME: 'call-home';

CALL_MANAGER_FALLBACK: 'call-manager-fallback';

CALLER_ID: 'caller-id';

CALLHOME: 'callhome';

CAPABILITY: 'capability';

CAPACITY: 'capacity';

CAPTIVE: 'captive';

CAPTIVE_PORTAL: 'captive-portal';

CAPTIVE_PORTAL_CERT: 'captive-portal-cert';

CAPTURE: 'capture';

CARD_TRAP_INH: 'card-trap-inh';

CARRIER_DELAY: 'carrier-delay';

CASE: 'case';

CAUSE: 'cause';

CCAP_CORE: 'ccap-core';

CDP: 'cdp';

CDP_URL: 'cdp-url';

CEILING: 'ceiling';

CENTRALIZED_LICENSING_ENABLE: 'centralized-licensing-enable';

CERTIFICATE
:
   'certificate' -> pushMode ( M_Certificate )
;

CHAIN: 'chain';

CHANGES: 'changes';

CHANNEL: 'channel';

CHANNEL_GROUP: 'channel-group';

CHANNEL_PROTOCOL: 'channel-protocol';

CHAP: 'chap';

CHARGEN: 'chargen';

CHASSIS_ID: 'chassis-id';

CHECK: 'check';

CIFS: 'cifs';

CIPC: 'cipc';

CIR: 'cir';

CIRCUIT_ID
:
   'circuit-id' -> pushMode ( M_Word )
;

CIRCUIT_TYPE: 'circuit-type';

CISCO_TDP: 'cisco_TDP';

CITADEL: 'citadel';

CITRIX_ICA: 'citrix-ica';

CLASS: 'class';

CLASS_DEFAULT: 'class-default';

CLASS_MAP: 'class-map';

CLEAR: 'clear';

CLEARCASE: 'clearcase';

CLEAR_SESSION: 'clear-session';

CLIENT: 'client';

CLIENT_IDENTIFIER: 'client-identifier';

CLIENT_NAME: 'client-name';

CLIENT_TO_CLIENT: 'client-to-client';

CLNS: 'clns';

CLOCK: 'clock';

CLOCK_PERIOD: 'clock-period';

CLOSED: 'closed';

CLUSTER: 'cluster';

CLUSTER_ID: 'cluster-id';

CLUSTER_LIST_LENGTH: 'cluster-list-length';

CMD: 'cmd';

CMTS: 'cmts';

CODEC: 'codec';

COLLECT: 'collect';

COLLECT_STATS: 'collect-stats';

COMM_LIST: 'comm-list';

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
;

COMMUNITY_LIST
:
   'community-list' -> pushMode(M_CommunityList)
;

COMMUNITY_MAP
:
   'community-map' -> pushMode ( M_Name )
;

COMPATIBLE: 'compatible';

CON: 'con';

CONF_LEVEL_INCR: 'conf-level-incr';

CONFED: 'confed';

CONFEDERATION: 'confederation';

CONFIG_COMMANDS: 'config-commands';

CONFIGURATION: 'configuration';

CONFIGURE: 'configure';

CONFORM_ACTION: 'conform-action';

CONGESTION_DROPS: 'congestion-drops';

CONGESTION_CONTROL: 'congestion-control';

CONNECT_SOURCE: 'connect-source';

CONNECTED: 'connected';

CONNECTION: 'connection';

CONNECTION_MODE: 'connection-mode';

CONNECTION_REUSE: 'connection-reuse';

CONSOLE: 'console';

CONSORTIUM: 'consortium';

CONTACT: 'contact';

CONTACT_EMAIL_ADDR: 'contact-email-addr';

CONTACT_NAME
:
   'contact-name' -> pushMode ( M_Description )
;

CONTENT_TYPE: 'content-type';

CONTEXT: 'context';

CONTEXT_NAME: 'context-name';

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

CONTRACT_ID: 'contract-id';

CONTROL: 'control';

CONTROL_DIRECTION: 'control-direction';

CONTROL_PLANE: 'control-plane';

CONTROL_PLANE_FILTER: 'control-plane-filter';

CONTROL_PLANE_SECURITY: 'control-plane-security';

CONTROLLER
:
   'controller' -> pushMode ( M_Interface )
;

CONVERGENCE: 'convergence';

CONVERSION_ERROR: 'conversion-error';

COOKIE: 'cookie';

COPS: 'cops';

COS: 'cos';

COS_QUEUE_GROUP: 'cos-queue-group';

COST: 'cost';

COUNT: 'count';

COUNTRY: 'country';

COUNTRY_CODE: 'country-code';

COUNTER: 'counter';

COUNTERS: 'counters';

COURIER: 'courier';

CPTONE: 'cptone';

CRC: 'crc';

CRITICAL: 'critical';

CRYPTO: 'crypto';

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

CUSTOM: 'custom';

CUSTOMER_ID: 'customer-id';

CVX: 'cvx';

CVX_CLUSTER: 'cvx-cluster';

CVX_LICENSE: 'cvx-license';

CWR: 'cwr';

D20_GGRP_DEFAULT: 'd20-ggrp-default';

D30_GGRP_DEFAULT: 'd30-ggrp-default';

DAD: 'dad';

DAEMON: 'daemon';

DAMPENING: 'dampening';

DATA_PRIVACY: 'data-privacy';

DATABASE: 'database';

DATABITS: 'databits';

DAYTIME: 'daytime';

DBL: 'dbl';

DCB: 'dcb';

DCB_POLICY: 'dcb-policy';

DCBX: 'dcbx';

DCE_MODE: 'dce-mode';

DEAD_INTERVAL: 'dead-interval';

DEADTIME: 'deadtime';

DEBUG: 'debug';

DEBUG_TRACE: 'debug-trace';

DEBUGGING: 'debugging';

DEFAULT: 'default';

DEFAULT_ACTION: 'default-action';

DEFAULT_ALLOW: 'default-allow';

DEFAULT_COST: 'default-cost';

DEFAULT_DESTINATION: 'default-destination';

DEFAULT_GATEWAY: 'default-gateway';

DEFAULT_GROUP_POLICY: 'default-group-policy';

DEFAULT_GUEST_ROLE: 'default-guest-role';

DEFAULT_GW: 'default-gw';

DEFAULT_INFORMATION: 'default-information';

DEFAULT_INFORMATION_ORIGINATE: 'default-information-originate';

DEFAULT_INSPECTION_TRAFFIC: 'default-inspection-traffic';

DEFAULT_METRIC: 'default-metric';

DEFAULT_ORIGINATE: 'default-originate';

DEFAULT_ROLE: 'default-role';

DEFAULT_ROUTER: 'default-router';

DEFAULT_ROUTE: 'default-route';

DEFAULT_TASKGROUP: 'default-taskgroup';

DEFAULT_TOS_QOS10: 'default-tos-qos10';

DEFINITION: 'definition' -> pushMode(M_Word);

DEL: 'Del';

DELIMITER: 'delimiter';

DELAY: 'delay';

DELAY_START: 'delay-start';

DELETE: 'delete';

DELETE_DYNAMIC_LEARN: 'delete-dynamic-learn';

DENY: 'deny';

DENY_IN_OUT: 'deny-in-out';

DENY_INTER_USER_TRAFFIC: 'deny-inter-user-traffic';

DEPI_CLASS: 'depi-class';

DEPI_TUNNEL: 'depi-tunnel';

DERIVATION_RULES: 'derivation-rules';

DES: 'des';

DESCENDING: 'descending';

DESCRIPTION
:
   'description' -> pushMode ( M_Description )
;

DESIGNATED_FORWARDER: 'designated-forwarder';

DESIRABLE: 'desirable';

DEST_IP: 'dest-ip';

DESTINATION: 'destination';

DESTINATION_PATTERN: 'destination-pattern';

DESTINATION_PROFILE: 'destination-profile';

DESTINATION_UNREACHABLE: 'destination-unreachable';

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

DEVICE: 'device';

DEVICE_ID: 'device-id';

DISCARDS: 'discards';

DISCRIMINATOR: 'discriminator';

DISPUTE: 'dispute';

DF: 'df';

DF_BIT: 'df-bit';

DFS: 'dfs';

DHCP: 'dhcp';

DHCP_FAILOVER2: 'dhcp-failover2';

DHCPV6_CLIENT: 'dhcpv6-client';

DHCPV6_SERVER: 'dhcpv6-server';

DIAGNOSTIC_SIGNATURE: 'diagnostic-signature';

DIAL_PEER: 'dial-peer';

DIAL_STRING: 'dial-string';

DIALER: 'dialer';

DIALER_GROUP: 'dialer-group';

DIALPLAN_PATTERN: 'dialplan-pattern';

DIALPLAN_PROFILE: 'dialplan-profile';

DIRECT: 'direct';

DIRECT_INSTALL: 'direct-install';

DIRECT_INWARD_DIAL: 'direct-inward-dial';

DIRECTED_BROADCAST: 'directed-broadcast';

DIRECTED_REQUEST: 'directed-request';

DIRECTION: 'direction';

DISABLE: 'disable';

DISABLED: 'disabled';

DISCARD: 'discard';

DISCARD_ROUTE: 'discard-route';

DISCOVERED_AP_CNT: 'discovered-ap-cnt';

DISCOVERY: 'discovery';

DISTANCE: 'distance';

DISTRIBUTE: 'distribute';

DISTRIBUTE_LIST: 'distribute-list';

DM_FALLBACK: 'dm-fallback';

DNS: 'dns';

DNS_DOMAIN: 'dns-domain';

DNS_SERVER: 'dns-server';

DNS_SERVERS: 'dns-servers';

DNS_SUFFIXES: 'dns-suffixes';

DNSIX: 'dnsix';

DO_UNTIL_FAILURE: 'do-until-failure';

DOCSIS_ENABLE: 'docsis-enable';

DOCSIS_GROUP: 'docsis-group';

DOCSIS_POLICY: 'docsis-policy';

DOCSIS_VERSION: 'docsis-version';

DOCSIS30_ENABLE: 'docsis30-enable';

DOD_HOST_PROHIBITED: 'dod-host-prohibited';

DOD_NET_PROHIBITED: 'dod-net-prohibited';

DOMAIN: 'domain';

DOMAIN_ID: 'domain-id';

DOMAIN_LIST: 'domain-list';

DOMAIN_NAME: 'domain-name';

DONT_CAPABILITY_NEGOTIATE: 'dont-capability-negotiate';

DOS_PROFILE: 'dos-profile';

DOT11: 'dot11';

DOT11A_RADIO_PROFILE: 'dot11a-radio-profile';

DOT11G_RADIO_PROFILE: 'dot11g-radio-profile';

DOT11K_PROFILE: 'dot11k-profile';

DOT11R_PROFILE: 'dot11r-profile';

DOT1P_PRIORITY: 'dot1p-priority';

DOT1Q
:
   'dot1' [Qq]
;

DOT1Q_TUNNEL: 'dot1q-tunnel';

DOT1X: 'dot1x';

DOT1X_DEFAULT_ROLE: 'dot1x-default-role';

DOT1X_SERVER_GROUP: 'dot1x-server-group';

DOWNSTREAM: 'downstream';

DOWNSTREAM_START_THRESHOLD: 'downstream-start-threshold';

DPG: 'dpg';

DR_PRIORITY: 'dr-priority';

DROP: 'drop';

DS_HELLO_INTERVAL: 'ds-hello-interval';

DS_MAX_BURST: 'ds-max-burst';

DSCP: 'dscp';

DSCP_VALUE: 'dscp-value';

DSG: 'dsg';

DSL: 'dsl';

DSP: 'dsp';

DSPFARM: 'dspfarm';

DST_NAT: 'dst-nat';

DSU: 'dsu';

DTMF_RELAY: 'dtmf-relay';

DTP: 'dtp';

DUAL_ACTIVE: 'dual-active';

DUPLEX: 'duplex';

DUPLICATE_MESSAGE: 'duplicate-message';

DURATION: 'duration';

DYNAMIC: 'dynamic';

DYNAMIC_ACCESS_POLICY_RECORD: 'dynamic-access-policy-record';

DYNAMIC_AUTHOR: 'dynamic-author';

DYNAMIC_MAP: 'dynamic-map';

DYNAMIC_MCAST_OPTIMIZATION: 'dynamic-mcast-optimization';

DYNAMIC_MCAST_OPTIMIZATION_THRESH: 'dynamic-mcast-optimization-thresh';

E164: 'e164';

E164_PATTERN_MAP: 'e164-pattern-map';

EAP_PASSTHROUGH: 'eap-passthrough';

EAPOL_RATE_OPT: 'eapol-rate-opt';

EARLY_OFFER: 'early-offer';

EBGP_MULTIHOP: 'ebgp-multihop';

ECE: 'ece';

ECHO: 'echo';

ECHO_CANCEL: 'echo-cancel';

ECHO_REPLY: 'echo-reply';

ECHO_REQUEST: 'echo-request';

ECHO_RX_INTERVAL: 'echo-rx-interval';

ECMP: 'ecmp';

ECMP_FAST: 'ecmp-fast';

ECN: 'ecn';

EDCA_PARAMETERS_PROFILE: 'edca-parameters-profile';

EDGE: 'edge';

EF: 'ef';

EFP: 'EFP';

EFS: 'efs';

EGP: 'egp';

EGRESS: 'egress';

EGRESS_INTERFACE_SELECTION: 'egress-interface-selection';

ELECTION: 'election';

EMAIL: 'email';

EMAIL_ADDR
:
   'email-addr' -> pushMode ( M_Description )
;

EMAIL_CONTACT
:
   'email-contact' -> pushMode ( M_Description )
;

EMERGENCIES: 'emergencies';

EMPTY: 'empty';

ENABLE: 'enable';

ENABLE_AUTHENTICATION: 'enable-authentication';

ENABLE_WELCOME_PAGE: 'enable-welcome-page';

ENABLED: 'enabled';

ENCAPSULATION: 'encapsulation';

ENCODING: 'encoding';

ENCODING_WEIGHTED: 'encoding-weighted';

ENCR: 'encr';

ENCRYPTED: 'encrypted';

ENCRYPTED_PASSWORD: 'encrypted-password';

ENCRYPTION: 'encryption';

END: 'end';

END_CLASS_MAP: 'end-class-map';

END_POLICY_MAP: 'end-policy-map';

ENET_LINK_PROFILE: 'enet-link-profile';

ENFORCE_DHCP: 'enforce-dhcp';

ENFORCE_FIRST_AS: 'enforce-first-as';

ENFORCE_RULE: 'enforce-rule';

ENFORCED: 'enforced';

ENGINE: 'engine';

ENGINEID
:
   (
      'engineid'
      | 'engineID'
   ) -> pushMode ( M_COMMENT )
;

ENROLLMENT: 'enrollment';

EOU: 'eou';

EPHONE_DN_TEMPLATE: 'ephone-dn-template';

EPP: 'epp';

EQ: 'eq';

ERRDISABLE: 'errdisable';

ERROR: 'error';

ERROR_CORRECTION: 'error-correction';

ERROR_ENABLE: 'error-enable';

ERROR_RATE_THRESHOLD: 'error-rate-threshold';

ERROR_PASSTHRU: 'error-passthru';

ERROR_RECOVERY: 'error-recovery';

ERROR_REPORTING: 'error-reporting';

ERRORS: 'errors';

ERSPAN_ID: 'erspan-id';

ESCAPE_CHARACTER: 'escape-character';

ESM: 'esm';

ESP: 'esp';

ESP_3DES: 'esp-3des';

ESP_AES: 'esp-aes';

ESP_AES128: 'esp-aes128';

ESP_AES192: 'esp-aes192';

ESP_AES256: 'esp-aes256';


ESP_DES: 'esp-des';

ESP_GCM: 'esp-gcm';

ESP_AES128_GCM: 'esp-aes128-gcm';

ESP_AES256_GCM: 'esp-aes256-gcm';

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

ETHERNET_SEGMENT: 'ethernet-segment';

ETHERNET_SERVICES: 'ethernet-services';

ETHERTYPE: 'ethertype';

ETYPE: 'etype';

EVALUATE: 'evaluate';

EVENT: 'event';

EVENT_HANDLER: 'event-handler';

EVENT_HISTORY: 'event-history';

EVENT_MONITOR: 'event-monitor';

EVENT_THRESHOLDS_PROFILE: 'event-thresholds-profile';

EVENTS: 'events';

EVPN: 'evpn';

EXCEED_ACTION: 'exceed-action';

EXCEPT: 'except';

EXCEPTION: 'exception';

EXCLUDE: 'exclude';

EXCLUDED_ADDRESS: 'excluded-address';

EXEC: 'exec';

EXEC_TIMEOUT: 'exec-timeout';

EXIT: 'exit';

EXIT_ADDRESS_FAMILY: 'exit-address-family';

EXPIRE: 'expire';

EXPLICIT_NULL: 'explicit-null';

EXPORT: 'export';

EXPORT_LOCALPREF: 'export-localpref';

EXPORT_PROTOCOL: 'export-protocol';

EXPORTER: 'exporter';

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

EXTEND: 'extend';

EXTENDED: 'extended';

EXTENDED_COUNTERS: 'extended-counters';

EXTENDED_DELAY: 'extended-delay';

EXTERNAL: 'external';

EXTERNAL_LSA: 'external-lsa';

FABRIC: 'fabric';

FABRIC_MODE: 'fabric-mode';

FABRICPATH: 'fabricpath';

FACILITY
:
   'facility' -> pushMode ( M_Word )
;

FAIL_MESSAGE: 'fail-message';

FAILED_LIST: 'failed-list';

FAILOVER: 'failover';

FAILURE: 'failure';

FAIR_QUEUE: 'fair-queue';

FALL_OVER: 'fall-over';

FALLBACK: 'fallback';

FALLBACK_DN: 'fallback-dn';

FAST_AGE: 'fast-age';

FAST_DETECT: 'fast-detect';

FAST_FLOOD: 'fast-flood';

FAST_REROUTE: 'fast-reroute';

FASTDROP: 'fastdrop';

FAX: 'fax';

FCOE: 'fcoe';

FDL
:
	'fdl'
;

FEATURE: 'feature';

FEC: 'fec';

FEX: 'fex';

FEX_FABRIC: 'fex-fabric';

FIB: 'fib';

FIBER_NODE
:
   'fiber-node' -> pushMode ( M_FiberNode )
;

FILE_TRANSFER: 'file-transfer';

FILTER: 'filter';

FILTER_LIST: 'filter-list';

FIN: 'fin';

FINGER: 'finger';

FLAP_LIST: 'flap-list';

FLASH: 'flash';

FLASH_OVERRIDE: 'flash-override';

FLOOD: 'flood';

FLOW: 'flow';

FLOW_SAMPLER_MAP: 'flow-sampler-map';

FLOW_SPEC: 'flow-spec';

FLOWCONTROL: 'flowcontrol';

FLUSH_AT_ACTIVATION: 'flush-at-activation';

FLUSH_R1_ON_NEW_R0: 'flush-r1-on-new-r0';

FORCED: 'forced';

FOR: 'for';

FORMAT: 'format';

FORTYG_FULL: '40gfull';

FORWARD: 'forward';

FORWARD_DIGITS: 'forward-digits';

FORWARDER: 'forwarder';

FORWARDING: 'forwarding';

FQDN: 'fqdn';

FRAGMENTATION: 'fragmentation';

FRAGMENT_RULES: 'fragment-rules';

FRAGMENTS: 'fragments';

FRAME_RELAY: 'frame-relay';

FRAMING: 'framing';

FREE_CHANNEL_INDEX: 'free-channel-index';

FREQUENCY: 'frequency';

FROM: 'from';

FROM_USER: 'from-user';

FTP: 'ftp';

FTP_DATA: 'ftp-data';

FTPS: 'ftps';

FTPS_DATA: 'ftps-data';

FULL_DUPLEX: 'full-duplex';

FULL_TXT: 'full-txt';

G709: 'g709';

G729: 'g729';

GBPS: 'Gbps';

GDOI: 'gdoi';

GE: 'ge';

GENERAL_GROUP_DEFAULTS: 'general-group-defaults';

GENERAL_PARAMETER_PROBLEM: 'general-parameter-problem';

GENERAL_PROFILE: 'general-profile';

GENERATE: 'generate';

GIG_DEFAULT: 'gig-default';

GLBP: 'glbp';

GLOBAL: 'global';

GLOBALENFORCEPRIV: 'globalEnforcePriv';

GLOBAL_PORT_SECURITY: 'global-port-security';

GOPHER: 'gopher';

GODI: 'godi';

GRACEFUL: 'graceful';

GRACE_PERIOD: 'grace-period';

GRACEFUL_RESTART: 'graceful-restart';

GRACEFUL_RESTART_HELPER: 'graceful-restart-helper';

GRACETIME: 'gracetime';

GRANT: 'grant';

GRATUITOUS: 'gratuitous';

GRE: 'gre';

GREEN: 'green';

GROUP: 'group';

GROUP_LIST: 'group-list';

GROUP_LOCK: 'group-lock';

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

GSHUT: 'GSHUT';

GT: 'gt';

GTP_C: 'gtp-c';

GTP_PRIME: 'gtp-prime';

GTP_U: 'gtp-u';

GUARANTEED: 'guaranteed';

GUARD: 'guard';

GUEST_ACCESS_EMAIL: 'guest-access-email';

GUEST_LOGON: 'guest-logon';

GUEST_MODE: 'guest-mode';

H225: 'h225';

H323: 'h323';

H323_GATEWAY: 'h323-gateway';

HA_CLUSTER: 'ha-cluster';

HALF_DUPLEX: 'half-duplex';

HANDOVER_TRIGGER_PROFILE: 'handover-trigger-profile';

HARDWARE: 'hardware';

HARDWARE_ADDRESS: 'hardware-address';

HASH: 'hash';

HEADER_PASSING: 'header-passing';

HEARTBEAT: 'heartbeat';

HEARTBEAT_INTERVAL: 'heartbeat-interval';

HEARTBEAT_TIMEOUT: 'heartbeat-timeout';

HELLO: 'hello';

HELLO_INTERVAL: 'hello-interval';

HELLO_MULTIPLIER: 'hello-multiplier';

HELLO_PADDING: 'hello-padding';

HELLO_PASSWORD: 'hello-password';

HELPER_ADDRESS: 'helper-address';

HEX_KEY: 'hex-key';

HIDDEN_LITERAL: 'hidden';

HIDEKEYS: 'hidekeys';

HIGH: 'high';

HIGH_RESOLUTION: 'high-resolution';

HISTORY: 'history';

HOLD_TIME: 'hold-time';

HOLD_QUEUE: 'hold-queue';

HOP_LIMIT: 'hop-limit';

HOPLIMIT: 'hoplimit';

HOPS_OF_STATISTICS_KEPT: 'hops-of-statistics-kept';

HOST
:
  'host'
  {
    if (lastTokenType() == LOGGING || secondToLastTokenType() == VRF) {
      pushMode(M_Word);
    }
  }
;

HOST_ASSOCIATION: 'host-association';

HOST_FLAP: 'host-flap';

HOST_ISOLATED: 'host-isolated';

HOST_PRECEDENCE_UNREACHABLE: 'host-precedence-unreachable';

HOST_PROXY: 'host-proxy';

HOST_REDIRECT: 'host-redirect';

HOST_ROUTE: 'host-route';

HOST_ROUTES: 'host-routes';

HOST_TOS_REDIRECT: 'host-tos-redirect';

HOST_TOS_UNREACHABLE: 'host-tos-unreachable';

HOST_UNKNOWN: 'host-unknown';

HOST_UNREACHABLE: 'host-unreachable';

HOSTNAME: 'hostname';

HOSTNAMEPREFIX: 'hostnameprefix';

HOTSPOT: 'hotspot';

HP_ALARM_MGR: 'hp-alarm-mgr';

HSC: 'hsc';

HT_SSID_PROFILE: 'ht-ssid-profile';

HTTP: 'http';

HTTP_ALT: 'http-alt';

HTTP_COMMANDS: 'http-commands';

HTTP_MGMT: 'http-mgmt';

HTTP_RPC_EPMAP: 'http-tpc-epmap';

HTTPS: 'https';

HUNT: 'hunt';

IBURST: 'iburst';

ICMP: 'icmp';
ICMP_ECHO: 'icmp-echo';

ICMP6: 'icmp6';

ICMPV6: 'icmpv6';

ID: 'id';

ID_MISMATCH: 'id-mismatch';

ID_RANDOMIZATION: 'id-randomization';

IDEAL_COVERAGE_INDEX: 'ideal-coverage-index';

IDENT: 'ident';

IDENTIFIER: 'identifier';

IDENTITY: 'identity';

IDLE_TIMEOUT: 'idle-timeout';

IDLE_RESTART_TIMER: 'idle-restart-timer';

IDP_CERT: 'idp-cert';

IDS: 'ids';

IDS_PROFILE: 'ids-profile';

IEC: 'iec';

IEEE: 'ieee';

IEEE_MMS_SSL: 'ieee-mms-ssl';

IFACL: 'ifacl';

IF_NEEDED: 'if-needed';

IFINDEX: 'ifindex';

IFMAP: 'ifmap';

IFMIB: 'ifmib';

IFP: 'IFP';

IGMP: 'igmp';

IGP_COST: 'igp-cost';

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

IMMEDIATE: 'immediate';

IMPERSONATION_PROFILE: 'impersonation-profile';

IMPLICIT_USER: 'implicit-user';

IMPORT: 'import';

IMPORT_LOCALPREF: 'import-localpref';

IN: 'in';

IN_PLACE: 'in-place';

INACTIVITY_TIMER: 'inactivity-timer';

INBAND: 'inband';

INBOUND: 'inbound';

INCLUDE: 'include';

INCLUDE_STUB: 'include-stub';

INCOMING: 'incoming';

INCOMPLETE: 'incomplete';

INDEX: 'index';

INFINITY: 'infinity';

INFORM: 'inform';

INFORMATION: 'information';

INFORMATION_REPLY: 'information-reply';

INFORMATION_REQUEST: 'information-request';

INFORMATIONAL: 'informational';

INFORMS: 'informs';

INGRESS: 'ingress';

INHERITANCE: 'inheritance';

INIT: 'init';

INIT_STRING: 'init-string';

INIT_TECH_LIST: 'init-tech-list';

INITIAL_ROLE: 'initial-role';

INPUT: 'input';

INSPECT: 'inspect';

INSTALL: 'install';

INSTALL_MAP: 'install-map';

INSTALL_OIFS: 'install-oifs';

INSTANCE: 'instance'
{
  if (lastTokenType() == VRF) {
    pushMode(M_Word);
  }
};

INTEGRITY: 'integrity';

INTERCEPT: 'intercept';

INTERFACE
:
   'int' 'erface'? -> pushMode(M_Interface)
;

INTERNAL: 'internal';

INTERNET: 'internet';

INTERVAL: 'interval';

INVALID_SPI_RECOVERY: 'invalid-spi-recovery';

INVALID_USERNAME_LOG: 'invalid-username-log';

INVERT: 'invert';

IP: 'ip';

IPADDRESS: 'ipaddress';

IPC: 'ipc';

IPENACL: 'ipenacl';

IPINIP: 'ipinip';

IPP: 'ipp';

IPSEC: 'ipsec';

IPSEC_ISAKMP: 'ipsec-isakmp';

IPSEC_MANUAL: 'ipsec-manual';

IPSEC_OVER_TCP: 'ipsec-over-tcp';

IPSEC_PROPOSAL: 'ipsec-proposal';

IPSLA: 'ipsla';

IPV4
:
   [iI] [pP] [vV] '4'
;

IPV4_L5: 'ipv4-l5';

IPV4_UNICAST: 'ipv4-unicast';

IPV6
:
   [iI] [pP] [vV] '6'
;

IPV6_ADDRESS_POOL: 'ipv6-address-pool';

IPV6_UNICAST: 'ipv6-unicast';

IPV6IP: 'ipv6ip';

IPX: 'ipx';

IRC: 'irc';

IRIS_BEEP: 'iris-beep';

ISAKMP: 'isakmp';

ISAKMP_PROFILE: 'isakmp-profile';

ISDN: 'isdn';

IS_TYPE: 'is-type';

ISCSI: 'iscsi';

ISI_GL: 'isi-gl';

ISIS: 'isis';

ISL: 'isl';

ISO_TSAP: 'iso-tsap';

ISOLATE: 'isolate';

ISPF: 'ispf';

ISSUER_NAME: 'issuer-name';

IUC: 'iuc';

JOIN_GROUP: 'join-group';

JOIN_PRUNE: 'join-prune';

JOIN_PRUNE_COUNT: 'join-prune-count';

JOIN_PRUNE_INTERVAL: 'join-prune-interval';

JUMBO: 'jumbo';

JUMBOMTU: 'jumbomtu';

KBPS: 'kbps';

KBYTES: 'kbytes';

KEEPALIVE: 'keepalive';

KEEPALIVE_ENABLE: 'keepalive-enable';

KEEPOUT: 'keepout';

KERBEROS: 'kerberos';

KERBEROS_ADM: 'kerberos-adm';

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

KEYRING: 'keyring';

KLOGIN: 'klogin';

KOD: 'kod';

KPASSWD: 'kpasswd';

KRB5: 'krb5';

KRB5_TELNET: 'krb5-telnet';

KSHELL: 'kshell';

L2: 'l2';

L2_FILTER: 'l2-filter';

L2_PROTOCOL: 'l2-protocol';

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

LANE: 'lane';

LANZ: 'lanz';

LAPB: 'lapb';

LAST_AS: 'last-as';

LAST_LISTENER_QUERY_COUNT: 'last-listener-query-count';

LAST_LISTENER_QUERY_INTERVAL: 'last-listener-query-interval';

LAST_MEMBER_QUERY_COUNT: 'last-member-query-count';

LAST_MEMBER_QUERY_INTERVAL: 'last-member-query-interval';

LAST_MEMBER_QUERY_RESPONSE_TIME: 'last-member-query-response-time';

LDAP: 'ldap';

LDAPS: 'ldaps';

LDP: 'ldp';

LE: 'le';

LEARNED: 'learned';

LEARNING: 'learning';

LEASE: 'lease';

LEVEL
:
  'level'
  {
    if (lastTokenType() == LOGGING) {
      pushMode(M_Word);
    }
  }
;

LEVEL_1: 'level-1';

LEVEL_1_2: 'level-1-2';

LEVEL_2: 'level-2';

LEVEL_2_ONLY: 'level-2-only';

LENGTH: 'length';

LICENSE: 'license';

LIFE: 'life';

LIFETIME: 'lifetime';

LIMIT: 'limit';

LIMIT_DN: 'limit-dn';

LINE: 'line';

LINE_PROTOCOL: 'line-protocol';

LINE_TERMINATION: 'line-termination';

LINECARD: 'linecard';

LINECARD_GROUP: 'linecard-group';

LINECODE: 'linecode';

LINK: 'link';

LINK_BANDWIDTH: 'link-bandwidth';

LINK_CHANGE: 'link-change';

LINK_DEBOUNCE: 'link-debounce';

LINK_FAIL: 'link-fail';

LINK_FAULT_SIGNALING: 'link-fault-signaling';

LINK_STATUS: 'link-status';

LINK_TYPE: 'link-type';

LINKSEC: 'linksec';

LINKDEBOUNCE: 'linkdebounce';

LIST: 'list';

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

LOCAL: 'local';

LOCALITY: 'locality';

LOCAL_ADDRESS: 'local-address';

LOCAL_AS
:
   [Ll][Oo][Cc][Aa][Ll]'-'[Aa][Ss]
;

LOCAL_CASE: 'local-case';

LOCAL_INTERFACE: 'local-interface';

LOCAL_IP: 'local-ip';

LOCAL_PORT: 'local-port';

LOCAL_PREFERENCE: 'local-preference';

LOCAL_PROXY_ARP: 'local-proxy-arp';

LOCAL_V4_ADDR: 'local-v4-addr';

LOCAL_V6_ADDR: 'local-v6-addr';

LOCAL_VOLATILE: 'local-volatile';

LOCATION
:
   'location' -> pushMode ( M_COMMENT )
;

LOG: 'log';

LOG_ADJ_CHANGES: 'log-adj-changes';

LOG_ADJACENCY_CHANGES: 'log-adjacency-changes';

LOG_CONSOLE: 'log-console';

LOG_FILE: 'log-file';

LOG_SYSLOG: 'log-syslog';

LOG_ENABLE: 'log-enable';

LOG_INPUT: 'log-input';

LOG_INTERNAL_SYNC: 'log-internal-sync';

LOG_NEIGHBOR_CHANGES: 'log-neighbor-changes';

LOGFILE: 'logfile';

LOGGING: 'logging';

LOGIN: 'login';

LOGIN_ATTEMPTS: 'login-attempts';

LOGIN_AUTHENTICATION: 'login-authentication';

LOGIN_PAGE: 'login-page';

LOGOUT_WARNING: 'logout-warning';

LOOKUP: 'lookup';

LOOPBACK: 'loopback';

LOOPGUARD: 'loopguard';

LOOSE: 'loose';

LOTUSNOTES: 'lotusnotes';

LPD: 'lpd';

LPTS: 'lpts';

LRE: 'lre';

LSP_GEN_INTERVAL: 'lsp-gen-interval';

LSP_INTERVAL: 'lsp-interval';

LSP_PASSWORD: 'lsp-password';

LSP_REFRESH_INTERVAL: 'lsp-refresh-interval';

LT: 'lt';

MAB: 'mab';

MAC: 'mac';

MAC_ADDRESS: 'mac-address';

MAC_DEFAULT_ROLE: 'mac-default-role';

MAC_MOVE: 'mac-move';

MAC_SERVER_GROUP: 'mac-server-group';

MAC_SRVR_ADMIN: 'mac-srvr-admin';

MACHINE_AUTHENTICATION: 'machine-authentication';

MACRO: 'macro';

MAIL_SERVER: 'mail-server';

MAIN_CPU: 'main-cpu';

MAINTENANCE: 'maintenance';

MANAGED_CONFIG_FLAG: 'managed-config-flag';

MANAGEMENT: 'management';

MANAGEMENT_ONLY: 'management-only';

MANAGEMENT_PLANE: 'management-plane';

MANAGEMENT_PROFILE: 'management-profile';

MANAGER: 'manager';

MAP: 'map';

MAP_CLASS: 'map-class';

MAP_GROUP: 'map-group';

MAPPING: 'mapping';

MASK: 'mask';

MASK_REPLY: 'mask-reply';

MASK_REQUEST: 'mask-request';

MASTER: 'master';

MATCH: 'match';

MATCH_ALL: 'match-all';

MATCH_ANY: 'match-any';

MATCH_MAP: 'match-map';

MATCH_NONE: 'match-none';

MATIP_TYPE_A: 'matip-type-a';

MATIP_TYPE_B: 'matip-type-b';

MAX: 'max';

MAX_ASSOCIATIONS: 'max-associations';

MAX_AUTHENTICATION_FAILURES: 'max-authentication-failures';

MAX_BURST: 'max-burst';

MAX_CLIENTS: 'max-clients';

MAX_CONCAT_BURST: 'max-concat-burst';

MAX_CONFERENCES: 'max-conferences';

MAX_CONNECTIONS: 'max-connections';

MAX_DN: 'max-dn';

MAX_EPHONES: 'max-ephones';

MAX_IFINDEX_PER_MODULE: 'max-ifindex-per-module';

MAX_LSA: 'max-lsa';

MAX_LSP_LIFETIME: 'max-lsp-lifetime';

MAX_METRIC: 'max-metric';

MAX_RATE: 'max-rate';

MAX_ROUTE: 'max-route';

MAX_SESSIONS: 'max-sessions';

MAX_TX_POWER: 'max-tx-power';

MAXIMUM: 'maximum';

MAXIMUM_ACCEPTED_ROUTES: 'maximum-accepted-routes';

MAXIMUM_HOPS: 'maximum-hops';

MAXIMUM_PATHS: 'maximum-paths';

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

MED: 'med';

MEDIUM: 'medium';

MEDIA: 'media';

MEDIA_TERMINATION: 'media-termination';

MEDIA_TYPE: 'media-type';

MEMBER: 'member';

MEMBER_INTERFACE
:
   'member-interface' -> pushMode(M_Interface)
;

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

METERING: 'metering';

METHOD: 'method';

METHOD_UTILIZATION: 'method-utilization';

METRIC: 'metric';

METRIC_OUT: 'metric-out';

METRIC_STYLE: 'metric-style';

METRIC_TYPE: 'metric-type';

MFIB: 'mfib';

MGMT: 'mgmt';

MGMT_AUTH: 'mgmt-auth';

MIB: 'mib';

MICRO_BFD: 'micro-bfd';

MICROSOFT_DS: 'microsoft-ds';

MIDCALL_SIGNALING: 'midcall-signaling';

MIN_PACKET_SIZE: 'min-packet-size';

MIN_RATE: 'min-rate';

MIN_RX: 'min-rx';

MIN_RX_VAR: 'min_rx';

MIN_TX_POWER: 'min-tx-power';

MINIMUM: 'minimum';

MINIMUM_INTERVAL: 'minimum-interval';

MINIMUM_LINKS: 'minimum-links';

MINIMUM_THRESHOLD: 'minimum-threshold';

MINPOLL: 'minpoll';

MISMATCH: 'mismatch';

MISSING_AS_WORST: 'missing-as-worst';

MISSING_POLICY: 'missing-policy';

MLAG: 'mlag';

MLAG_SYSTEM_ID: 'mlag-system-id';

MLD: 'mld';

MLD_QUERY: 'mld-query';

MLD_REDUCTION: 'mld-reduction';

MLD_REPORT: 'mld-report';

MLDV2: 'mldv2';

MLS: 'mls';

MOBILE_HOST_REDIRECT: 'mobile-host-redirect';

MOBILE_IP: 'mobile-ip';

MOBILITY: 'mobility';

MODE: 'mode';

MODEM: 'modem';

MODULATION_PROFILE: 'modulation-profile';

MODULE: 'module';

MODULE_TYPE: 'module-type';

MONITOR: 'monitor';

MONITOR_MAP: 'monitor-map';

MONITOR_SESSION: 'monitor-session';

MONITORING: 'monitoring';

MONITORING_BASICS: 'monitoring-basics';

MOP: 'mop';

MOTD: 'motd';

MPP: 'mpp';

MPLS: 'mpls';

MPLS_LABEL: 'mpls-label';

MROUTE: 'mroute';

MS_SQL_M: 'ms-sql-m';

MS_SQL_S: 'ms-sql-s';

MSCHAP: 'mschap';

MSCHAPV2: 'mschapv2';

MSDP: 'msdp';

MSEC: 'msec';

MSEXCH_ROUTING: 'msexch-routing';

MSG_ICP: 'msg-icp';

MSP: 'msp';

MSRP: 'msrp';

MSRPC: 'msrpc';

MSS: 'mss';

MST: 'mst';

MTU: 'mtu';

MTU_IGNORE: 'mtu-ignore';

MULTICAST: 'multicast';

MULTICAST_GROUP: 'multicast-group';

MULTICAST_ROUTING: 'multicast-routing';

MULTICAST_STATIC_ONLY: 'multicast-static-only';

MULTIPATH: 'multipath';

MULTIPATH_RELAX: 'multipath-relax';

MULTIPLIER: 'multiplier';

MULTIPOINT: 'multipoint';

MULTI_TOPOLOGY: 'multi-topology';

MUST_SECURE: 'must-secure';

MVRP: 'mvrp';

NAME
:
   'name' -> pushMode ( M_Name )
;

NAME_RESOLUTION: 'name-resolution';

NAME_SERVER: 'name-server';

NAMED_KEY: 'named-key';

NAMESERVER: 'nameserver';

NAS: 'nas';

NAT
:
   [Nn][Aa][Tt]
;

NAT_TRANSPARENCY: 'nat-transparency';

NAT_TRAVERSAL: 'nat-traversal';

NATIVE: 'native';

NBAR: 'nbar';

NCP: 'ncp';

ND: 'nd';

ND_NA: 'nd-na';

ND_NS: 'nd-ns';

ND_TYPE: 'nd-type';

NEGOTIATE: 'negotiate';

NEGOTIATION: 'negotiation';

NEIGHBOR
:
   'neighbor'
   {
     if (lastTokenType() != TRACE) {
       pushMode ( M_NEIGHBOR );
     }
   }
;

NEIGHBOR_FILTER: 'neighbor-filter';

NEIGHBOR_IS: 'neighbor-is';

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

NETDESTINATION: 'netdestination';

NETDESTINATION6: 'netdestination6';

NETMASK: 'netmask';

NETRJS_1: 'netrjs-1';

NETRJS_2: 'netrjs-2';

NETRJS_3: 'netrjs-3';

NETRJS_4: 'netrjs-4';

NETSERVICE: 'netservice';

NETWALL: 'netwall';

NETWNEWS: 'netwnews';

NETWORK: 'network';

NETWORK_DELAY: 'network-delay';

NETWORK_POLICY: 'network-policy';

NETWORK_QOS: 'network-qos';

NETWORK_UNKNOWN: 'network-unknown';

NEW_MODEL: 'new-model';

NEW_RWHO: 'new-rwho';

NEWINFO: 'newinfo';

NEXT_HOP: 'next-hop';

NEXT_HOP_PEER: 'next-hop-peer';

NEXT_HOP_SELF: 'next-hop-self';

NEXT_HOP_UNCHANGED: 'next-hop-unchanged';

NEXT_HOP_V6_ADDR: 'next-hop-v6-addr';

NEXT_SERVER: 'next-server';

NEXTHOP1: 'nexthop1';

NEXTHOP2: 'nexthop2';

NFS: 'nfs';

NHOP_ONLY: 'nhop-only';

NLRI: 'nlri';

NMSP: 'nmsp';

NNTP: 'nntp';

NNTPS: 'nntps';

NO: 'no';

NOPASSWORD: 'nopassword';

NO_ADVERTISE: 'no-advertise';

NO_EXPORT: 'no-export';

NO_PREPEND: 'no-prepend';

NO_REDISTRIBUTION: 'no-redistribution';

NO_ROOM_FOR_OPTION: 'no-room-for-option';

NO_SUMMARY: 'no-summary';

NOAUTH: 'noauth';

NOE: 'noe';

NOHANGUP: 'nohangup';

NON500_ISAKMP: 'non500-isakmp';

NON_BROADCAST: 'non-broadcast';

NON_CLIENT_NRT: 'non-client-nrt';

NON_DR: 'non-dr';

NON_ECT: 'non-ect';

NON_MLAG: 'non-mlag';

NONE: 'none';

NONEGOTIATE: 'nonegotiate';

NORMAL: 'normal';

NOS: 'nos';

NOT: 'not';

NOTATION: 'notation';

NOT_ADVERTISE: 'not-advertise';

NOTIFICATION_TIMER: 'notification-timer';

NOTIFICATIONS: 'notifications';

NOTIFY: 'notify';

NOTIFY_FILTER: 'notify-filter';

NSF: 'nsf';

NSR: 'nsr';

NSSA: 'nssa';

NSSA_EXTERNAL: 'nssa-external';

NSW_FE: 'nsw-fe';

NT_ENCRYPTED: 'nt-encrypted';

NTP: 'ntp';

NULL: 'null';

NV: 'nv';

OBJECT: 'object';

ODMR: 'odmr';

OLSR: 'olsr';

ON: 'on';

ON_FAILURE: 'on-failure';

ON_PASSIVE: 'on-passive';

ON_STARTUP: 'on-startup';

ON_SUCCESS: 'on-success';

ONE_HUNDRED_FULL: '100full';

ONE_HUNDREDG_FULL: '100gfull';

ONE_OUT_OF: 'one-out-of';

ONE_THOUSAND_FULL: '1000full';

ONEP: 'onep';

OPEN: 'open';

OPENFLOW: 'openflow';

OPENSTACK: 'openstack';

OPENVPN: 'openvpn';

OPERATION: 'operation';

OPMODE: 'opmode';

OPTICAL_MONITOR: 'optical-monitor';

OPTIMIZATION_PROFILE: 'optimization-profile';

OPTIMIZE: 'optimize';

OPTION: 'option';

OPTION_MISSING: 'option-missing';

OPTIONAL: 'optional';

OPTIONS: 'options';

ORGANIZATION_NAME: 'organization-name';

ORGANIZATION_UNIT: 'organization-unit';

ORIGIN: 'origin';

ORIGIN_ID: 'origin-id';

ORIGINATE: 'originate';

ORIGINATES_FROM: 'originates-from';

ORIGINATOR_ID: 'originator-id';

OSPF: 'ospf';

OSPF3: 'ospf3';

OSPFV3: 'ospfv3';

OTHER_ACCESS: 'other-access';

OTHER_CONFIG_FLAG: 'other-config-flag';

OUT: 'out';

OUT_DELAY: 'out-delay';

OUT_OF_BAND: 'out-of-band';

OUTBOUND_ACL_CHECK: 'outbound-acl-check';

OUTER: 'outer';

OUTPUT: 'output';

OVERLOAD: 'overload';

OVERLOAD_CONTROL: 'overload-control';

OVERRIDE: 'override';

OVSDB_SHUTDOWN: 'ovsdb-shutdown';

OWNER: 'owner';

P2P: 'p2p';

PACKET: 'packet';

PACKET_TOO_BIG: 'packet-too-big';

PACKETS: 'packets';

PACKETSIZE: 'packetsize';

PAGP: 'pagp';

PARAM: 'param';

PARAMETER_PROBLEM: 'parameter-problem';

PARAMETERS: 'parameters';

PARTICIPATE: 'participate';

PASS: 'pass';

PASSES_THROUGH: 'passes-through';

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

PASSWD: 'passwd';

PATH_ECHO: 'path-echo';

PATH_MTU_DISCOVERY: 'path-mtu-discovery';

PATH_OPTION: 'path-option';

PATH_RETRANSMIT: 'path-retransmit';

PATHCOST: 'pathcost';

PATH: 'path';

PATHS: 'paths';

PATHS_OF_STATISTICS_KEPT: 'paths-of-statistics-kept';

PAUSE: 'pause';

PBKDF2: 'pbkdf2';

PBR: 'pbr';

PCANYWHERE_DATA: 'pcanywhere-data';

PCANYWHERE_STATUS: 'pcanywhere-status';

PCP: 'pcp';

PCP_VALUE: 'pcp-value';

PDP: 'pdp';

PEAKDETECT: 'peakdetect';

PEER: 'peer';

PEER_ADDRESS: 'peer-address';

PEER_CONFIG_CHECK_BYPASS: 'peer-config-check-bypass';

PEER_FILTER
:
   'peer-filter' -> pushMode (M_Word)
;

PEER_GROUP
:
   'peer-group' -> pushMode ( M_NEIGHBOR )
;

PEER_GATEWAY: 'peer-gateway';

PEER_KEEPALIVE: 'peer-keepalive';

PEER_LINK: 'peer-link';

PEER_MAC_RESOLUTION_TIMEOUT: 'peer-mac-resolution-timeout';

PEER_SWITCH: 'peer-switch';

PEER_TO_PEER: 'peer-to-peer';

PEERS: 'peers';

PENALTY_PERIOD: 'penalty-period';

PERCENT_LITERAL: 'percent';

PERIODIC: 'periodic';

PERIODIC_INVENTORY: 'periodic-inventory';

PERMANENT: 'permanent';

PERMISSION: 'permission';

PERMIT: 'permit';

PERMIT_HOSTDOWN: 'permit-hostdown';

PERSIST_DATABASE: 'persist-database';

PFC: 'pfc';

PFS: 'pfs';

PHONE: 'phone';

PHONE_CONTACT
:
   'phone-contact' -> pushMode ( M_Description )
;

PHONE_NUMBER: 'phone-number';

PHONE_PROXY: 'phone-proxy';

PHY: 'phy';

PHYSICAL_LAYER: 'physical-layer';

PICKUP: 'pickup';

PIM: 'pim';

PIM_AUTO_RP: 'pim-auto-rp';

PKI: 'pki';

PKIX_TIMESTAMP: 'pkix-timestamp';

PKT_KRB_IPSEC: 'pkt-krb-ipsec';

PLATFORM: 'platform';

PM: 'pm';

PMTUD: 'pmtud';

POINT_TO_MULTIPOINT: 'point-to-multipoint';

POINT_TO_POINT: 'point-to-point';

POLICE: 'police';

POLICY: 'policy';

POLICY_LIST: 'policy-list';

POLICY_MAP: 'policy-map';

POOL: 'pool';

POP: 'pop';

POP2: 'pop2';

POP3: 'pop3';

POP3S: 'pop3s';

PORT: 'port';

PORTFAST: 'portfast';

PORT_CHANNEL: 'port-channel';

PORT_CHANNEL_PROTOCOL: 'port-channel-protocol';

PORT_NAME: 'port-name';

PORT_PRIORITY: 'port-priority';

PORT_SECURITY: 'port-security';

PORT_TYPE: 'port-type';

PORT_UNREACHABLE: 'port-unreachable';

PORTMODE: 'portmode';

POS: 'pos';

POST_POLICY: 'post-policy';

POWER: 'power';

POWER_LEVEL: 'power-level';

PPP: 'ppp';

PPTP: 'pptp';

PRC_INTERVAL: 'prc-interval';

PRE_EQUALIZATION: 'pre-equalization';

PRE_POLICY: 'pre-policy';

PRE_SHARE: 'pre-share';

PRE_SHARED_KEY: 'pre-shared-key';

PRECEDENCE: 'precedence';

PRECEDENCE_UNREACHABLE: 'precedence-unreachable';

PRECONFIGURE: 'preconfigure';

PREEMPT: 'preempt';

PREFER: 'prefer';

PREFERENCE: 'preference';

PREFERRED: 'preferred';

PREFIX: 'prefix';

PREFIX_LENGTH: 'prefix-length';

PREFIX_LIST
:
   'prefix-list' -> pushMode(M_PrefixList)
;

PREPEND: 'prepend';

PREPEND_OWN: 'prepend-own';

PRESERVE_ATTRIBUTES: 'preserve-attributes';

PRF: 'prf';

PRI_GROUP: 'pri-group';

PRINT_SRV: 'print-srv';

PRIORITY: 'priority';

PRIORITY_FLOW_CONTROL: 'priority-flow-control';

PRIORITY_FORCE: 'priority-force';

PRIORITY_LEVEL: 'priority-level';

PRIORITY_MAPPING: 'priority-mapping';

PRIORITY_QUEUE: 'priority-queue';

PRIV: 'priv';

PRIVACY: 'privacy';

PRIVATE_AS: 'private-as';

PRIVATE_VLAN: 'private-vlan';

PRIVILEGE: 'privilege';

PRIVILEGE_MODE: 'privilege-mode';

PROACTIVE: 'proactive';

PROBE: 'probe';

PROCESS_MAX_TIME: 'process-max-time';

PROFILE: 'profile';

PROGRESS_IND: 'progress_ind';

PROPAGATE: 'propagate';

PROPOSAL: 'proposal';

PROPRIETARY: 'proprietary';

PROTECT: 'protect';

PROTECT_SSID: 'protect-ssid';

PROTECT_TUNNEL: 'protect-tunnel';

PROTECT_VALID_STA: 'protect-valid-sta';

PROTECTION: 'protection';

PROTOCOL: 'protocol';

PROTOCOL_DISCOVERY: 'protocol-discovery';

PROTOCOL_HTTP: 'protocol-http';

PROTOCOL_UNREACHABLE: 'protocol-unreachable';

PROTOCOL_VIOLATION: 'protocol-violation';

PROVISIONING_PROFILE: 'provisioning-profile';

PROXY_ARP: 'proxy-arp';

PROXY_SERVER: 'proxy-server';

PSEUDO_INFORMATION: 'pseudo-information';

PSEUDOWIRE: 'pseudowire';

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

QOTD: 'qotd';

QUERY_COUNT: 'query-count';

QUERY_INTERVAL: 'query-interval';

QUERY_MAX_RESPONSE_TIME: 'query-max-response-time';

QUERY_ONLY: 'query-only';

QUERY_RESPONSE_INTERVAL: 'query-response-interval';

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

RA: 'ra';

RADIUS: 'radius';

RADIUS_ACCOUNTING: 'radius-accounting';

RADIUS_ACCT: 'radius-acct';

RADIUS_INTERIM_ACCOUNTING: 'radius-interim-accounting';

RADIUS_SERVER: 'radius-server';

RANDOM: 'random';

RANDOM_DETECT: 'random-detect';

RANDOM_DETECT_LABEL: 'random-detect-label';

RANGE: 'range';

RATE: 'rate';

RATE_LIMIT: 'rate-limit';

RATE_MODE: 'rate-mode';

RATE_THRESHOLDS_PROFILE: 'rate-thresholds-profile';

RBACL: 'rbacl';

RCP: 'rcp';

RCV_QUEUE: 'rcv-queue';

RD: 'rd';

RE_MAIL_CK: 're-mail-ck';

REACHABLE_TIME: 'reachable-time';

REACHABLE_VIA: 'reachable-via';

REACT: 'react';

REACTION: 'reaction';

READ_ONLY_PASSWORD: 'read-only-password';

REAL_TIME_CONFIG: 'real-time-config';

REASSEMBLY_TIMEOUT: 'reassembly-timeout';

REAUTHENTICATION: 'reauthentication';

RECEIVE: 'receive';

RECEIVE_WINDOW: 'receive-window';

RECEIVED: 'received';

RECIRCULATION: 'recirculation';

RECORD: 'record';

RECORD_ENTRY: 'record-entry';

RECURSIVE: 'recursive';

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

REED_SOLOMON: 'reed-solomon';

REFERENCE_BANDWIDTH: 'reference-bandwidth';

REFLECT: 'reflect';

REFLECTION: 'reflection';

REGEX_MODE: 'regex-mode';

REGEXP: 'regexp';

REGISTER_RATE_LIMIT: 'register-rate-limit';

REGISTER_SOURCE: 'register-source';

REGISTERED: 'registered';

REGULATORY_DOMAIN_PROFILE: 'regulatory-domain-profile';

REJECT: 'reject';

RELAY: 'relay';

RELOAD: 'reload';

RELOAD_DELAY: 'reload-delay';

RELOGGING_INTERVAL: 'relogging-interval';

REPEAT_MESSAGES: 'repeat-messages';

REMARK
:
   'remark' -> pushMode ( M_REMARK )
;

REMOTE: 'remote';

REMOTE_ACCESS: 'remote-access';

REMOTE_AS: 'remote-as';

REMOTE_IP: 'remote-ip';

REMOTE_PORT: 'remote-port';

REMOTE_PORTS: 'remote-ports';

REMOTE_SERVER: 'remote-server';

REMOTEFS: 'remotefs';

REMOVE: 'remove';

REMOVE_PRIVATE_AS
:
   'remove-private-' [Aa] [Ss]
;

REOPTIMIZE: 'reoptimize';

REPCMD: 'repcmd';

REPLACE_AS: 'replace-as';

REPLACE: 'replace';

REPLY: 'reply';

REPLY_TO: 'reply-to';

REPORT_INTERVAL: 'report-interval';

REQ_RESP: 'req-resp';

REQ_TRANS_POLICY: 'req-trans-policy';

REQUEST: 'request';

REQUEST_DATA_SIZE: 'request-data-size';

REQUIRE_WPA: 'require-wpa';

REQUIRED: 'required';

RESOLUTION: 'resolution';

RESPONDER: 'responder';

RESPONSE: 'response';

RESTART_TIME: 'restart-time';

RESTRICT: 'restrict';

RESTRICTED: 'restricted';

RESULT: 'result';

RESULT_TYPE: 'result-type';

RESUME: 'resume';


RESYNC_PERIOD: 'resync-period';

RETAIN: 'retain';

RETRANSMIT: 'retransmit';

RETRANSMIT_TIMEOUT: 'retransmit-timeout';

RETRIES: 'retries';

RETRY: 'retry';

REVERSE_ACCESS: 'reverse-access';

REVERSE_ROUTE: 'reverse-route';

REVERTIVE: 'revertive';

REVISION: 'revision';

REVOCATION_CHECK: 'revocation-check';

REWRITE: 'rewrite';

RF: 'rf';

RF_CHANNEL: 'rf-channel';

RF_POWER: 'rf-power';

RF_SHUTDOWN: 'rf-shutdown';

RF_SWITCH: 'rf-switch';

RFC_3576_SERVER: 'rfc-3576-server';

RFC1583: 'rfc1583';

RFC1583COMPATIBILITY: 'rfc1583compatibility';

RIB: 'rib';

RIB_IN: 'rib-in';

RIBS: 'ribs';

RIP: 'rip';

RJE: 'rje';

RLP: 'rlp';

RLZDBASE: 'rlzdbase';

RMC: 'rmc';

RMONITOR: 'rmonitor';

RO
:
   [rR] [oO]
;

ROBUSTNESS: 'robustness';

ROBUSTNESS_VARIABLE: 'robustness-variable';

ROGUE_AP_AWARE: 'rogue-ap-aware';

ROLE: 'role';

ROOT: 'root';

ROTARY: 'rotary';

ROUTE: 'route';

ROUTE_CACHE: 'route-cache';

ROUTE_KEY: 'route-key';

ROUTE_MAP
:
   'route-map' -> pushMode ( M_RouteMap )
;

ROUTE_ONLY: 'route-only';

ROUTE_REFLECTOR: 'route-reflector';

ROUTE_REFLECTOR_CLIENT: 'route-reflector-client';

ROUTE_SOURCE: 'route-source';

ROUTE_TARGET: 'route-target';

ROUTE_TO_PEER: 'route-to-peer';

ROUTE_TYPE: 'route-type';

ROUTED: 'routed';

ROUTER: 'router';

ROUTER_ADVERTISEMENT: 'router-advertisement';

ROUTER_ALERT: 'router-alert';

ROUTER_ID: 'router-id';

ROUTER_LSA: 'router-lsa';

ROUTER_MAC: 'router-mac';

ROUTER_PREFERENCE: 'router-preference';

ROUTER_SOLICITATION: 'router-solicitation';

ROUTES: 'routes';

ROUTING: 'routing';

RP_ADDRESS: 'rp-address';

RP_ANNOUNCE_FILTER: 'rp-announce-filter';

RP_CANDIDATE
:
   'rp-candidate' -> pushMode(M_Interface)
;

RP_LIST: 'rp-list';

RPC2PORTMAP: 'rpc2portmap';

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

RSYNC: 'rsync';

RT: 'rt';

RTCP_INACTIVITY: 'rtcp-inactivity';

RTELNET: 'rtelnet';

RTP: 'rtp';

RTP_PORT: 'rtp-port';

RTR_ADV: 'rtr-adv';

RTSP: 'rtsp';

RULE: 'rule';

RULE_NAME: 'rule-name';

RUN: 'run';

RW
:
   [Rr] [Ww]
;

RX: 'rx';

RXSPEED: 'rxspeed';

SA_FILTER: 'sa-filter';

SAMPLED: 'sampled';

SAMPLES_OF_HISTORY_KEPT: 'samples-of-history-kept';

SATELLITE: 'satellite';

SATELLITE_FABRIC_LINK: 'satellite-fabric-link';

SCALE_FACTOR: 'scale-factor';

SCANNING: 'scanning';

SCCP: 'sccp';

SCHED_TYPE: 'sched-type';

SCHEDULE: 'schedule';

SCHEME: 'scheme';

SCOPE: 'scope';

SCP: 'scp';

SCRAMBLE: 'scramble';

SCRIPT: 'script';

SCTP: 'sctp';

SDROWNER: 'SDROwner';

SECONDARY: 'secondary';

SECONDARY_DIALTONE: 'secondary-dialtone';

SECRET: 'secret';

SECUREID_UDP: 'secureid-udp';

SECURITY: 'security';

SECURITY_ASSOCIATION: 'security-association';

SELF_IDENTITY: 'self-identity';

SEND: 'send';

SEND_COMMUNITY: 'send-community';

SEND_LIFETIME: 'send-lifetime';

SEND_RP_ANNOUNCE
:
   'send-rp-announce' -> pushMode(M_Interface)
;

SEND_RP_DISCOVERY: 'send-rp-discovery';

SEND_TIME: 'send-time';

SENDER: 'sender';

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

SERVICE: 'service';

SERVICE_CLASS: 'service-class';

SERVICE_LIST: 'service-list';

SERVICE_MODULE: 'service-module';

SERVICE_POLICY: 'service-policy';

SERVICE_TEMPLATE: 'service-template';

SESSION: 'session';

SESSION_AUTHORIZATION: 'session-authorization';

SESSION_DISCONNECT_WARNING
:
   'session-disconnect-warning' -> pushMode ( M_COMMENT )
;

SESSION_ID: 'session-id';

SESSION_KEY: 'session-key';

SESSION_LIMIT: 'session-limit';

SESSION_PROTECTION: 'session-protection';

SESSION_TIMEOUT: 'session-timeout';

SET: 'set';

SET_COLOR: 'set-color';

SET_OVERLOAD_BIT: 'set-overload-bit';

SEVERITY: 'severity';

SFLOW: 'sflow';

SFTP: 'sftp';

SG_EXPIRY_TIMER: 'sg-expiry-timer';

SGBP: 'sgbp';

SGMP: 'sgmp';

SHA: 'sha';

SHA2_256_128: 'sha2-256-128';

SHA512: 'sha512';

SHA512_PASSWORD
:
   '$sha512$' [0-9]+ '$' F_Base64String '$' F_Base64String -> pushMode ( M_SeedWhitespace )
;

SHAPE: 'shape';

SHARED_SECONDARY_SECRET: 'shared-secondary-secret';

SHARED_SECRET: 'shared-secret';

SHIFT: 'shift';

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

SILC: 'silc';

SINGLE_CONNECTION: 'single-connection';

SINGLE_HOP: 'single-hop';

SINGLE_TOPOLOGY: 'single-topology';

SIP: 'sip';

SIP_MIDCALL_REQ_TIMEOUT: 'sip-midcall-req-timeout';

SIP_PROFILES: 'sip-profiles';

SIP_SERVER: 'sip-server';

SIP_UA: 'sip-ua';

SIPS: 'sips';

SITE_ID: 'site-id';

SIXPE: '6pe';

// cannot declare a rule with reserved name SKIP
SKIP_LITERAL: 'skip';

SLA: 'sla';

SLOT: 'slot';

SLOW_PEER: 'slow-peer';

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

SOFT_PREEMPTION: 'soft-preemption';

SOFT_RECONFIGURATION
:
   'soft' '-reconfiguration'?
;

SONET: 'sonet';

SOURCE: 'source';

SOURCE_ADDRESS: 'source-address';

SOURCE_INTERFACE
:
   'source-interface' -> pushMode ( M_Interface )
;

SOURCE_IP_ADDRESS: 'source-ip-address';

SOURCE_PROTOCOL: 'source-protocol';

SOURCE_ROUTE: 'source-route';

SOURCE_ROUTE_FAILED: 'source-route-failed';

SOURCE_QUENCH: 'source-quench';

SPAN: 'span';

SPANNING_TREE: 'spanning-tree';

SPARSE_MODE: 'sparse-mode';

SPECTRUM: 'spectrum';

SPECTRUM_LOAD_BALANCING: 'spectrum-load-balancing';

SPECTRUM_MONITORING: 'spectrum-monitoring';

SPEED: 'speed';

SPEED_DUPLEX: 'speed-duplex';

SPF_INTERVAL: 'spf-interval';

SPLIT_HORIZON: 'split-horizon';

SPT_THRESHOLD: 'spt-threshold';

SQLNET: 'sqlnet';

SQLSRV: 'sqlsrv';

SQLSERV: 'sqlserv';

SRC_IP: 'src-ip';

SRC_NAT: 'src-nat';

SRLG: 'srlg';

SRR_QUEUE: 'srr-queue';

SR_TE: 'sr-te';

SRST: 'srst';

SSH: 'ssh';

SSH_CERTIFICATE: 'ssh-certificate';

SSH_PUBLICKEY: 'ssh-publickey';

SSID: 'ssid';

SSID_ENABLE: 'ssid-enable';

SSID_PROFILE: 'ssid-profile';

SSL: 'ssl';

SSM: 'ssm';

STACK_MIB: 'stack-mib';

STALEPATH_TIME: 'stalepath-time';

STANDARD: 'standard';

STANDBY: 'standby';

START_STOP: 'start-stop';

START_TIME: 'start-time';

STARTUP_QUERY_COUNT: 'startup-query-count';

STARTUP_QUERY_INTERVAL: 'startup-query-interval';

STATE: 'state';

STATIC: 'static';

STATIC_GROUP: 'static-group';

STATION: 'station';

STATION_ROLE: 'station-role';

STATISTICS: 'statistics';

STBC: 'stbc';

STCAPP: 'stcapp';

STOP_ONLY: 'stop-only';

STOP_RECORD: 'stop-record';

STOPBITS: 'stopbits';

STORM_CONTROL: 'storm-control';

STP: 'stp';

STREAMING: 'streaming';

STREET_ADDRESS: 'street-address';

STREETADDRESS
:
   'streetaddress' -> pushMode ( M_Description )
;

STRICT: 'strict';

STRICTHOSTKEYCHECK: 'stricthostkeycheck';

STRING: 'string';

STRIP: 'strip';

STS_1: 'sts-1';

STUB: 'stub';

SUBINTERFACE: 'subinterface';

SUBJECT_NAME: 'subject-name';

SUBMGMT: 'submgmt';

SUBNET_MASK: 'subnet-mask';

SUBNETS: 'subnets';

SUB_OPTION: 'sub-option';

SUB_ROUTE_MAP: 'sub-route-map';

SUBMISSION: 'submission';

SUBSCRIBE_TO_ALERT_GROUP: 'subscribe-to-alert-group';

SUBSCRIBER: 'subscriber';

SUCCESS: 'success';

SUMMARY_ADDRESS: 'summary-address';

SUMMARY_LSA: 'summary-lsa';

SUMMARY_ONLY: 'summary-only';

SUNRPC: 'sunrpc';

SUPER_USER_PASSWORD: 'super-user-password';

SUPPLEMENTARY_SERVICE: 'supplementary-service';

SUPPRESS: 'suppress';

SUPPRESS_ARP: 'suppress-arp';

SUPPRESSED: 'suppressed';

SUSPECT_ROGUE_CONF_LEVEL: 'suspect-rogue-conf-level';

SUSPEND: 'suspend';

SVC: 'svc';

SVP: 'svp';

SVRLOC: 'svrloc';

SWITCH_CERT: 'switch-cert';

SWITCH_PRIORITY: 'switch-priority';

SWITCHBACK: 'switchback';

SWITCHING_MODE: 'switching-mode';

SWITCHNAME: 'switchname';

SWITCHPORT: 'switchport';

SYN: 'syn';

SYNCHRONOUS: 'synchronous';

SYSLOG: 'syslog';

SYSTAT: 'systat';

SYSTEM: 'system';

SYSTEM_CONNECTED: 'system-connected';

SYSTEM_PRIORITY: 'system-priority';

SYSTEM_PROFILE: 'system-profile';

SYSTEM_SHUTDOWN: 'system-shutdown';

SYSTEM_TUNNEL_RIB: 'system-tunnel-rib';

SYSTEM_UNICAST_RIB: 'system-unicast-rib';

SYSTEMOWNER: 'SystemOwner';

TABLE: 'table';

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

TAGGED: 'tagged';

TALK: 'talk';

TAP: 'tap';

TBRPF: 'tbrpf';

TCAM: 'tcam';

TCP: 'tcp';

TCP_INSPECTION: 'tcp-inspection';

TCP_SESSION: 'tcp-session';
TCP_UDP: 'tcp-udp';

TCPMUX: 'tcpmux';

TCPNETHASPSRV: 'tcpnethaspsrv';

TCS_LOAD_BALANCE: 'tcs-load-balance';

TELEPHONY_SERVICE: 'telephony-service';

TELNET: 'telnet';

TEMPLATE: 'template';

TEN_THOUSAND_FULL: '10000full';

TERMINAL: 'terminal';

TERMINAL_TYPE: 'terminal-type';

TERMINATE: 'terminate';

TERMINATION: 'termination';

TEST: 'test';

TFTP: 'tftp';

TFTP_SERVER: 'tftp-server';

TFTP_SERVER_LIST: 'tftp-server-list';

THREE_DES: '3des';

THRESHOLD: 'threshold';

THRESHOLDS: 'thresholds';

TIE_BREAK: 'tie-break';

TIME: 'time';

TIME_EXCEEDED: 'time-exceeded';

TIME_FORMAT: 'time-format';

TIME_RANGE: 'time-range';

TIME_OUT: 'time-out';

TIMED: 'timed';

TIMEOUT: 'timeout';

TIMEOUTS: 'timeouts';

TIMER: 'timer';

TIMERS: 'timers';

TIMESOURCE: 'timesource';

TIMESTAMP: 'timestamp';

TIMESTAMP_REPLY: 'timestamp-reply';

TIMESTAMP_REQUEST: 'timestamp-request';

TIME_ZONE: 'time-zone';

TIMING: 'timing';

TLS: 'tls';

TLS_PROXY: 'tls-proxy';

TM_VOQ_COLLECTION: 'tm-voq-collection';

TO: 'to';

TOKEN: 'token';

TOOL: 'tool';

TOS: 'tos';

TOS_OVERWRITE: 'tos-overwrite';

TRACE: 'trace';

TRACER: 'tracer';

TRACEROUTE: 'traceroute';

TRACK: 'track';

TRACKED: 'tracked';

TRACKER: 'tracker';

TRADITIONAL: 'traditional';

TRAFFIC_CLASS: 'traffic-class';

TRAFFIC_ENG: 'traffic-eng';

TRAFFIC_FILTER: 'traffic-filter';

TRAFFIC_INDEX: 'traffic-index';

TRAFFIC_LOOPBACK: 'traffic-loopback';

TRANSFER_SYSTEM: 'transfer-system';

TRANSFORM_SET: 'transform-set';

TRANSCEIVER: 'transceiver';

TRANSLATE: 'translate';

TRANSLATION: 'translation';

TRANSLATION_RULE: 'translation-rule';

TRANSLATION_PROFILE: 'translation-profile';

TRANSMIT: 'transmit';

TRANSMITTER: 'transmitter';

TRANSPORT: 'transport';

TRANSPORT_METHOD: 'transport-method';

TRANSPORT_MODE: 'transport-mode';

TRAP: 'trap';

TRAP_NEW_MASTER: 'trap-new-master';

TRAP_SOURCE
:
   'trap-source' -> pushMode ( M_Interface )
;

TRAP_TIMEOUT: 'trap-timeout';

TRAPS: 'traps';

TRIGGER: 'trigger';

TRIMODE: 'trimode';

TRUNCATION: 'truncation';

TRUNK: 'trunk';

TRUST: 'trust';

TRUSTED: 'trusted';

TRUSTED_KEY: 'trusted-key';

TRUSTPOINT: 'trustpoint';

TRUSTPOOL: 'trustpool';

TSID: 'tsid';

TSM_REQ_PROFILE: 'tsm-req-profile';

TTL: 'ttl';

TTL_EXCEEDED: 'ttl-exceeded';

TTL_EXCEPTION: 'ttl-exception';

TTY: 'tty';

TUNABLE_OPTIC: 'tunable-optic';

TUNNEL: 'tunnel';

TUNNEL_GROUP: 'tunnel-group';

TUNNEL_GROUP_LIST: 'tunnel-group-list';

TUNNEL_ID: 'tunnel-id';

TUNNEL_RIB: 'tunnel-rib';

TUNNELED: 'tunneled';

TWENTY_FIVE_GBASE_CR: '25gbase-cr';

TX_QUEUE: 'tx-queue';

TXSPEED: 'txspeed';

TYPE: 'type';

UCMP: 'ucmp';

UC_TX_QUEUE: 'uc-tx-queue';

UDLD: 'udld';

UDP: 'udp';

UDP_JITTER: 'udp-jitter';

UDP_PORT: 'udp-port';

UDP_TIMEOUT: 'udp-timeout';

UNAUTHORIZED: 'unauthorized';

UNAUTHORIZED_DEVICE_PROFILE: 'unauthorized-device-profile';

UNICAST: 'unicast';

UNIDIRECTIONAL: 'unidirectional';

UNIX_SOCKET: 'unix-socket';

UNIQUE: 'unique';

UNKNOWN_UNICAST: 'unknown-unicast';

UNREACHABLE: 'unreachable';

UNTAGGED: 'untagged';

UPDATE: 'update';

UPDATE_CALENDAR: 'update-calendar';

UPDATE_DELAY: 'update-delay';

UPDATE_INTERVAL: 'update-interval';

UPDATE_SOURCE
:
   'update-source' -> pushMode ( M_Interface )
;

UPLINK_FAILURE_DETECTION: 'uplink-failure-detection';

UPLINKFAST: 'uplinkfast';

UPS: 'ups';

UPSTREAM: 'upstream';

UPSTREAM_START_THRESHOLD: 'upstream-start-threshold';

URG: 'urg';

URI: 'uri';

URL: 'url';

URL_LIST: 'url-list';

URPF: 'urpf';

USE: 'use';

USE_ACL: 'use-acl';

USE_CONFIG_SESSION: 'use-config-session';

USE_GLOBAL: 'use-global';

USE_IPV4_ACL: 'use-ipv4-acl';

USE_IPV6_ACL: 'use-ipv6-acl';

USE_LINK_ADDRESS: 'use-link-address';

USER: 'user';

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

USERNAME: 'username';

USERNAME_PROMPT: 'username-prompt';

UTIL_INTERVAL: 'util-interval';

UUCP: 'uucp';

UUCP_PATH: 'uucp-path';

V1_RP_REACHABILITY: 'v1-rp-reachability';

V2: 'v2';

VACANT_MESSAGE: 'vacant-message';

VACL: 'vacl';

VAD: 'vad';

VALID_11A_40MHZ_CHANNEL_PAIR: 'valid-11a-40mhz-channel-pair';

VALID_11A_80MHZ_CHANNEL_GROUP: 'valid-11a-80mhz-channel-group';

VALID_11A_CHANNEL: 'valid-11a-channel';

VALID_11G_40MHZ_CHANNEL_PAIR: 'valid-11g-40mhz-channel-pair';

VALID_11G_CHANNEL: 'valid-11g-channel';

VALID_AND_PROTECTED_SSID: 'valid-and-protected-ssid';

VALIDATION_USAGE: 'validation-usage';

VAP_ENABLE: 'vap-enable';

VENDOR_OPTION: 'vendor-option';

VERIFICATION: 'verification';

VERIFY: 'verify';

VERIFY_DATA: 'verify-data';

VERSION: 'version';

VIEW: 'view';

VIOLATE_ACTION: 'violate-action';

VIRTUAL: 'virtual';

VIRTUAL_ADDRESS: 'virtual-address';

VIRTUAL_AP: 'virtual-ap';

VIRTUAL_ROUTER: 'virtual-router';

VIRTUAL_TEMPLATE: 'virtual-template';

VFI: 'vfi';

VFP: 'VFP';

VLAN: 'vlan';

VLAN_AWARE_BUNDLE: 'vlan-aware-bundle';

VLAN_NAME: 'vlan-name';

VLT_PEER_LAG: 'vlt-peer-lag';

VMNET: 'vmnet';

VMTRACER: 'vmtracer';

VNI: 'vni';

VOCERA: 'vocera';

VOICE: 'voice';

VOICE_CARD: 'voice-card';

VOICE_CLASS: 'voice-class';

VOICE_PORT: 'voice-port';

VOICE_SERVICE: 'voice-service';

VOIP: 'voip';

VOIP_CAC_PROFILE: 'voip-cac-profile';

VPC: 'vpc';

VPDN_GROUP: 'vpdn-group';

VPN: 'vpn';

VPN_DIALER: 'vpn-dialer';

VPN_GROUP_POLICY: 'vpn-group-policy';

VPN_IPV4: 'vpn-ipv4';

VPN_IPV6: 'vpn-ipv6';

VRF: 'vrf' -> pushMode(M_Vrf);

VRF_ALSO: 'vrf-also';

VRF_UNICAST_RIB: 'vrf-unicast-rib';

VRRP: 'vrrp';

VTEP: 'vtep';

VTP: 'vtp';

VTY: 'vty';

VXLAN: 'vxlan';

VXLAN_ENCAPSULATION: 'vxlan-encapsulation';

VXLAN_VTEP_LEARN: 'vxlan-vtep-learn';

WAIT_FOR: 'wait-for';

WAIT_FOR_BGP: 'wait-for-bgp';

WAIT_FOR_CONVERGENCE: 'wait-for-convergence';

WAIT_INSTALL: 'wait-install';

WAIT_START: 'wait-start';

WARNINGS: 'warnings';

WARNING_LIMIT: 'warning-limit';

WARNING_ONLY: 'warning-only';

WARNTIME: 'warntime';

WATCHDOG: 'watchdog';

WATCH_LIST: 'watch-list';

WAVELENGTH: 'wavelength';

WCCP: 'wccp';

WEB_CACHE: 'web-cache';

WEB_HTTPS_PORT_443: 'web-https-port-443';

WEB_MAX_CLIENTS: 'web-max-clients';

WEB_SERVER: 'web-server';

WEBAUTH: 'webauth';

WEBVPN: 'webvpn';

WEEKDAY: 'weekday';

WEEKEND: 'weekend';

WEIGHT: 'weight';

WEIGHTING: 'weighting';

WELCOME_PAGE: 'welcome-page';

WHITE_LIST: 'white-list';

WHO: 'who';

WHOIS: 'whois';

WIDE: 'wide';

WIDE_METRIC: 'wide-metric';

WIDEBAND: 'wideband';

WINDOW: 'window';

WINDOW_SIZE: 'window-size';

WIRED_AP_PROFILE: 'wired-ap-profile';

WIRED_CONTAINMENT: 'wired-containment';

WIRED_PORT_PROFILE: 'wired-port-profile';

WIRED_TO_WIRELESS_ROAM: 'wired-to-wireless-roam';

WIRELESS_CONTAINMENT: 'wireless-containment';

WLAN: 'wlan';

WMM: 'wmm';

WMS_GENERAL_PROFILE: 'wms-general-profile';

WMS_LOCAL_SYSTEM_PROFILE: 'wms-local-system-profile';

WPA_FAST_HANDOVER: 'wpa-fast-handover';

WRITE_MEMORY: 'write-memory';

WRR_QUEUE: 'wrr-queue';

WSMA: 'wsma';

WWW: 'www';

X25: 'x25';

XCONNECT: 'xconnect';

XCVR_MISCONFIGURED: 'xcvr-misconfigured';

XCVR_OVERHEAT: 'xcvr-overheat';

XCVR_POWER_UNSUPPORTED: 'xcvr-power-unsupported';

XCVR_UNSUPPORTED: 'xcvr-unsupported';

XDMCP: 'xdmcp';

XML
:
   'XML'
   | 'xml'
;

XNS_CH: 'xns-ch';

XNS_MAIL: 'xns-mail';

XNS_TIME: 'xns-time';

YELLOW: 'yellow';

Z39_50: 'z39-50';

/* Other Tokens */

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

BLANK_LINE
:
   F_Whitespace+ F_Newline+ {lastTokenType() == NEWLINE}? -> channel ( HIDDEN )
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
  (
    F_Whitespace
  )* [!#]
  {
    // TODO: in JDK 12. can use inline switch case
    ((java.util.function.Supplier<Boolean>)() -> {
      switch(lastTokenType()) {
        case -1:
        case BANNER_DELIMITER_EOS:
        case NEWLINE:
          return true;
        default:
          return false;
      }}).get()
  }?
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
   {lastTokenType() == NEWLINE || lastTokenType() == -1}?

   'show' F_Whitespace+ 'run' ( 'n' ( 'i' ( 'n' ( 'g' ( '-' ( 'c' ( 'o' ( 'n' ( 'f' ( 'i' 'g'? )? )? )? )? )? )? )? )? )? )? F_Whitespace* F_Newline+ -> channel ( HIDDEN )
;

DASH: '-';

DOLLAR
:
   '$'
;

// Numbers: keep in order
UINT8: F_Uint8;
UINT16: F_Uint16;
UINT32: F_Uint32;

// Hope to kill these two eventually
DEC: F_Digit+;
DIGIT: F_Digit;

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
      F_PositiveDigit F_Digit* '.' F_Digit+
   )
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

NEWLINE
:
  F_Newline+
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

// Variable should be last unless we can find a reason not to.
VARIABLE
:
   F_Variable_VarChar* F_Variable_RequiredVarChar F_Variable_VarChar*
;

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

M_AsPath_IGNORE
:
   'ignore' -> type ( IGNORE ) , popMode
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

M_AsPath_PREPEND_OWN
:
   'prepend-own' -> type ( PREPEND_OWN ) , popMode
;

M_AsPath_ORIGINATES_FROM
:
   'originates-from' -> type ( ORIGINATES_FROM ) , popMode
;

M_AsPath_REGEX_MODE
:
   'regex-mode' -> type ( REGEX_MODE ) , popMode
;

M_AsPath_REMOTE_AS
:
   'remote-as' -> type ( REMOTE_AS ) , popMode
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

M_AsPathAccessList_WORD
:
   F_NonWhitespace+ -> type(WORD), mode(M_AsPathAccessList_Action)
;

M_AsPathAccessList_NEWLINE
:
   F_Newline+ -> type(NEWLINE), popMode
;

M_AsPathAccessList_WS
:
   F_Whitespace+ -> channel(HIDDEN)
;

mode M_AsPathAccessList_Action;

M_AsPathAccessList_Action_DENY
:
   'deny' -> type(DENY), mode(M_Word)
;

M_AsPathAccessList_Action_PERMIT
:
   'permit' -> type(PERMIT), mode(M_Word)
;

M_AsPathAccessList_Action_NEWLINE
:
   F_Newline+ -> type(NEWLINE), popMode
;

M_AsPathAccessList_Action_WS
:
   F_Whitespace+ -> channel(HIDDEN)
;

mode M_Authentication;

M_Authentication_DOUBLE_QUOTE
:
   '"' -> mode ( M_DoubleQuote )
;

M_Authentication_BANNER
:
  'banner' F_Whitespace+ -> type ( BANNER )
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

M_Authentication_DEC
:
  F_Digit+ -> type ( DEC ) , popMode
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

mode M_Banner;

M_Banner_EXEC
:
   'exec' -> type ( EXEC ), mode ( M_BannerEos )
;

M_Banner_LOGIN
:
   'login' -> type ( LOGIN ), mode ( M_BannerEos )
;

M_Banner_MOTD
:
   'motd' -> type ( MOTD ), mode ( M_BannerEos )
;

M_Banner_NEWLINE
:
  // Did not get a banner type, so exit banner mode
  F_Newline -> type ( NEWLINE ) , popMode
;

M_Banner_WS
:
  F_Whitespace+ -> channel ( HIDDEN )
;

mode M_BannerEos;

M_BannerEos_NEWLINE
:
  // Consume single newline. Subsequent newlines are part of banner.
  F_Newline -> type(NEWLINE), mode(M_BannerEosText)
;

M_BannerEos_WS
:
  F_Whitespace+ -> channel(HIDDEN)
;

mode M_BannerEosText;

M_BannerEos_BANNER_DELIMITER_EOS
:
  'EOF' F_Newline+ -> type(BANNER_DELIMITER_EOS), popMode
;

M_BannerEos_BODY
:
  F_NonNewline* F_Newline+
  {
    if (bannerEosDelimiterFollows()) {
      setType(BANNER_BODY);
    } else {
      more();
    }
  }
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

////////
// <4.23
// ip community-list NAME standard <permit|deny> communities...
// ip community-list NAME expanded <permit|deny> regex
// 4.23+
// ip community-list NAME <permit|deny> communities...
// ip community-list regexp NAME <permit|deny> regex
///////

mode M_CommunityList;

M_CommunityList_EXPANDED
:
  // expanded list, old style
  'expanded' -> type(EXPANDED), mode(M_CommunityList_Regexp)
;

M_CommunityList_REGEXP
:
  // expanded list, new style
  'regexp' -> type(REGEXP), mode(M_CommunityList_Regexp)
;

M_CommunityList_STANDARD
:
  // standard list, old style. Action and communities can be lexed normally
  'standard' -> type(STANDARD), mode(M_Word)
;

M_CommunityList_WORD
:
  // standard list, communities can be parsed normally
  F_NonWhitespace+ -> type(WORD), popMode
;

M_CommunityList_NEWLINE
:
  // bail in case of short line
  F_Newline+ -> type(NEWLINE), popMode
;

M_CommunityList_WS
:
  F_Whitespace+ -> channel(HIDDEN)
;

mode M_CommunityList_Regexp;

M_CommunityList_Regexp_WORD
:
  // name, now need action
  F_NonWhitespace+ -> type(WORD), mode(M_CommunityList_Regexp_Action)
;

M_CommunityList_Regexp_NEWLINE
:
  // bail in case of short line
  F_Newline+ -> type(NEWLINE), popMode
;

M_CommunityList_Regexp_WS
:
  F_Whitespace+ -> channel(HIDDEN)
;

mode M_CommunityList_Regexp_Action;

M_CommunityList_Regexp_Action_DENY
:
  'deny' -> type(DENY), mode(M_Word)
;

M_CommunityList_Regexp_Action_PERMIT
:
  'permit' -> type(PERMIT), mode(M_Word)
;

M_CommunityList_Regexp_Action_NEWLINE
:
  // bail in case of short line
  F_Newline+ -> type(NEWLINE), popMode
;

M_CommunityList_Regexp_Action_WS
:
  F_Whitespace+ -> channel(HIDDEN)
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

M_Interface_LINE_PROTOCOL
:
   'line-protocol' -> type ( LINE_PROTOCOL ) , popMode
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
   'vrf' -> type (VRF), mode(M_Vrf)
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

// M_Interface_VXLAN must come before M_Interface_PREFIX
M_Interface_VXLAN
:
   [Vv]'xlan' -> type (VXLAN)
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

mode M_Ip_access_list;

M_Ip_access_list_STANDARD
:
  'standard' -> type(STANDARD), mode(M_Word)
;

M_Ip_access_list_NEWLINE
:
   F_Newline+ -> type(NEWLINE), popMode
;

M_Ip_access_list_WORD
:
  F_NonWhitespace+ -> type(WORD), popMode
;

M_Ip_access_list_WS
:
  F_Whitespace+ -> channel(HIDDEN)
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

mode M_Name;

M_Name_NAME
:
   (
      F_NonWhitespace+
      | '"' ~'"'* '"'
   )  -> type ( VARIABLE ) , popMode
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

M_NEIGHBOR_DEFAULT
:
   'default' -> type ( DEFAULT ) , popMode
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
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_NEIGHBOR_VARIABLE
:
   F_Variable_VarChar+ -> type ( VARIABLE ) , popMode
;

M_NEIGHBOR_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Prefix_Or_Standard_Acl;

M_Prefix_Or_Standard_Acl_IP_ADDRESS
:
  F_IpAddress -> type ( IP_ADDRESS ) , popMode
;

M_Prefix_Or_Standard_Acl_IP_PREFIX
:
  F_IpPrefix -> type ( IP_PREFIX ) , popMode
;

M_Prefix_Or_Standard_Acl_WORD
:
   F_NonWhitespace+ -> type ( WORD ) , popMode
;

M_Prefix_Or_Standard_Acl_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Prefix_Or_Standard_Acl_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_PrefixList;

M_PrefixList_IN
:
   'in' -> type ( IN )
;

M_PrefixList_OUT
:
   'out' -> type ( OUT )
;

M_PrefixList_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_PrefixList_VARIABLE
:
   F_NonWhitespace+ -> type ( VARIABLE ) , popMode
;

M_PrefixList_WS
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
   F_NonNewline+ -> type(RAW_TEXT)
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
   F_NonWhitespace+ -> type ( VARIABLE ) , popMode
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

mode M_Vrf;

M_Vrf_DEFINITION
:
  'definition' -> type(DEFINITION), mode(M_Word)
;

M_Vrf_FORWARDING
:
  'forwarding' -> type(FORWARDING), mode(M_Word)
;

M_Vrf_INSTANCE
:
  'instance' -> type(INSTANCE), mode(M_Word)
;

M_Vrf_WORD
:
  F_NonWhitespace+ -> type(WORD), popMode
;

M_Vrf_NEWLINE
:
   F_Newline+ -> type (NEWLINE), popMode
;

M_Vrf_WS
:
   F_Whitespace+ -> channel(HIDDEN)
;

mode M_Word;

M_Word_WORD
:
   F_NonWhitespace+ -> type ( WORD ) , popMode
;

M_Word_NEWLINE
:
   F_Newline+ -> type ( NEWLINE ) , popMode
;

M_Word_WS
:
   F_Whitespace+ -> channel ( HIDDEN )
;

mode M_Words;

M_Words_WORD
:
   F_NonWhitespace+ -> type(WORD)
;

M_Words_NEWLINE
:
   F_Newline+ -> type(NEWLINE), popMode
;

M_Words_WS
:
   F_Whitespace+ -> channel(HIDDEN)
;

