parser grammar CiscoParser;

import
Cisco_common, Cisco_aaa, Cisco_acl, Cisco_bgp, Cisco_cable, Cisco_crypto, Cisco_callhome, Cisco_eigrp, Cisco_hsrp, Cisco_ignored, Cisco_interface, Cisco_isis, Cisco_line, Cisco_logging, Cisco_mpls, Cisco_ntp, Cisco_ospf, Cisco_pim, Cisco_qos, Cisco_rip, Cisco_routemap, Cisco_snmp, Cisco_static;

options {
   superClass = 'org.batfish.grammar.BatfishParser';
   tokenVocab = CiscoLexer;
}

@members {
   private boolean _cadant;

   private boolean _multilineBgpNeighbors;

   public void setCadant(boolean b) {
      _cadant = b;
   }

   public void setMultilineBgpNeighbors(boolean multilineBgpNeighbors) {
      _multilineBgpNeighbors = multilineBgpNeighbors;
   }
   
   @Override
   public String getStateInfo() {
      return String.format("_cadant: %s\n_multilineBgpNeighbors: %s\n",
         _cadant,
         _multilineBgpNeighbors
      );
   }
}

address_aiimgp_stanza
:
   ADDRESS ~NEWLINE* NEWLINE
;

address_family_multicast_stanza
:
   ADDRESS_FAMILY
   (
      IPV4
      | IPV6
   ) NEWLINE address_family_multicast_tail
;

address_family_multicast_tail
:
   (
      (
         MULTIPATH NEWLINE
      )
      |
      (
         INTERFACE ALL ENABLE NEWLINE
      )
      | null_af_multicast_tail
      | interface_multicast_stanza
      | ip_pim_tail
   )*
;

ags_null
:
   NO?
   (
      DESCRIPTION
      | ID
   ) ~NEWLINE* NEWLINE
;

aiimgp_stanza
:
   address_aiimgp_stanza
;

al_null
:
   NO?
   (
      HIDEKEYS
      | LOGGING
      | NOTIFY
   ) ~NEWLINE* NEWLINE
;

allow_iimgp_stanza
:
   ALLOW ~NEWLINE* NEWLINE aiimgp_stanza*
;

ap_null
:
   NO?
   (
      AP_BLACKLIST_TIME
      | ENET_LINK_PROFILE
      | FLUSH_R1_ON_NEW_R0
      | GENERAL_PROFILE
      | GROUP
      | LLDP
      | MESH_CLUSTER_PROFILE
      | MESH_HT_SSID_PROFILE
      | MESH_RADIO_PROFILE
      | PROVISIONING_PROFILE
      | SPECTRUM
      | WIRED_AP_PROFILE
      | WIRED_PORT_PROFILE
   ) ~NEWLINE* NEWLINE
;

ap_regulatory_domain_profile
:
   REGULATORY_DOMAIN_PROFILE ~NEWLINE* NEWLINE
   (
      aprdp_null
   )*
;

ap_system_profile
:
   SYSTEM_PROFILE ~NEWLINE* NEWLINE
   (
      apsp_null
   )*
;

apg_null
:
   NO?
   (
      AP_SYSTEM_PROFILE
      | DOT11A_RADIO_PROFILE
      | DOT11G_RADIO_PROFILE
      | IDS_PROFILE
      | VIRTUAL_AP
   ) ~NEWLINE* NEWLINE
;

apn_null
:
   NO?
   (
      VIRTUAL_AP
   ) ~NEWLINE* NEWLINE
;

aprdp_null
:
   NO?
   (
      COUNTRY_CODE
      | VALID_11A_40MHZ_CHANNEL_PAIR
      | VALID_11A_80MHZ_CHANNEL_GROUP
      | VALID_11A_CHANNEL
      | VALID_11G_40MHZ_CHANNEL_PAIR
      | VALID_11G_CHANNEL
   ) ~NEWLINE* NEWLINE
;

apsp_null
:
   NO?
   (
      BKUP_LMS_IP
      | DNS_DOMAIN
      | LMS_IP
      | LMS_PREEMPTION
   ) ~NEWLINE* NEWLINE
;

archive_log
:
   LOG ~NEWLINE* NEWLINE
   (
      al_null
   )*
;

archive_null
:
   NO?
   (
      MAXIMUM
      | PATH
      | WRITE_MEMORY
   ) ~NEWLINE* NEWLINE
;

asa_comment_stanza
:
   COLON ~NEWLINE* NEWLINE
;

av_null
:
   NO?
   (
      CAPTURE
      | INTERFACE
      | MODE
      | SHUTDOWN
      | TIMESOURCE
   ) ~NEWLINE* NEWLINE
;

banner_stanza
:
   BANNER banner_type banner
;

cisco_configuration
:
   NEWLINE?
   (
      sl += stanza
   )+ COLON? NEWLINE? EOF
;

configure_maintenance
:
   MAINTENANCE ~NEWLINE* NEWLINE
   (
      configure_maintenance_null
      | configure_maintenance_router
   )*
;

configure_maintenance_null
:
   NO?
   (
      IP
   ) ~NEWLINE* NEWLINE
;

configure_maintenance_router
:
   NO?
   (
      ROUTER
   ) ~NEWLINE* NEWLINE
   (
      configure_maintenance_router_null
   )*
;

configure_maintenance_router_null
:
   NO?
   (
      ISOLATE
   ) ~NEWLINE* NEWLINE
;

configure_null
:
   NO?
   (
      | SESSION
      | TERMINAL
   ) ~NEWLINE* NEWLINE
;

cops_listener
:
   LISTENER
   (
      copsl_access_list
   )
;

copsl_access_list
:
   ACCESS_LIST name = variable_permissive NEWLINE
;

cp_ip_access_group
:
   (
      IP
      | IPV6
   ) ACCESS_GROUP name = variable
   (
      VRF vrf = variable
   )?
   (
      IN
      | OUT
   ) NEWLINE
;

cp_ip_flow
:
   IP FLOW MONITOR name = variable
   (
      INPUT
      | OUTPUT
   ) NEWLINE
;

cp_management_plane
:
   MANAGEMENT_PLANE NEWLINE mgp_stanza*
;

cp_null
:
   NO?
   (
      EXIT
      | SCALE_FACTOR
   ) ~NEWLINE* NEWLINE
;

cp_service_policy
:
   SERVICE_POLICY
   (
      INPUT
      | OUTPUT
   ) name = variable NEWLINE
;

cps_null
:
   NO?
   (
      AUTO_CERT_ALLOW_ALL
      | AUTO_CERT_ALLOWED_ADDRS
      | AUTO_CERT_PROV
   ) ~NEWLINE* NEWLINE
;

cqg_null
:
   NO?
   (
      PRECEDENCE
      | QUEUE
      | RANDOM_DETECT_LABEL
   ) ~NEWLINE* NEWLINE
;

cmf_null
:
   NO?
   (
      ALIAS
      | CALL_FORWARD
      | DEFAULT_DESTINATION
      | DIALPLAN_PATTERN
      | IP
      | KEEPALIVE
      | LIMIT_DN
      | MAX_CONFERENCES
      | MAX_DN
      | MAX_EPHONES
      | SECONDARY_DIALTONE
      | TIME_FORMAT
      | TIME_ZONE
      | TRANSFER_SYSTEM
      | TRANSLATION_PROFILE
   ) ~NEWLINE* NEWLINE
;

ctlf_null
:
   NO?
   (
      RECORD_ENTRY
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

cvx_null
:
   NO?
   (
      SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

d11_null
:
   NO?
   (
      ACCOUNTING
      | AUTHENTICATION
      | GUEST_MODE
      | MAX_ASSOCIATIONS
      | MBSSID
      | VLAN
   ) ~NEWLINE* NEWLINE
;

daemon_null
:
   NO?
   (
      EXEC
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

dapr_null
:
   NO?
   (
      ACTION
      | USER_MESSAGE
   ) ~NEWLINE* NEWLINE
;

dapr_webvpn
:
   WEBVPN NEWLINE
   (
      daprw_null
   )*
;

daprw_null
:
   NO?
   (
      ALWAYS_ON_VPN
      | SVC
      | URL_LIST
   ) ~NEWLINE* NEWLINE
;

del_stanza
:
   DEL ~NEWLINE* NEWLINE
;

dhcp_null
:
   NO?
   (
      INTERFACE
   ) ~NEWLINE* NEWLINE
;

dhcp_profile
:
   NO? PROFILE ~NEWLINE* NEWLINE
   (
      dhcp_profile_null
   )*
;

dhcp_profile_null
:
   NO?
   (
      DEFAULT_ROUTER
      | DOMAIN_NAME
      | DNS_SERVER
      | HELPER_ADDRESS
      | LEASE
      | POOL
      | SUBNET_MASK
   ) ~NEWLINE* NEWLINE
;

dialer_group
:
   GROUP ~NEWLINE* NEWLINE
   (
      dialer_group_null
   )*
;

dialer_group_null
:
   NO?
   (
      DIAL_STRING
      | INIT_STRING
   ) ~NEWLINE* NEWLINE
;

dialer_null
:
   NO?
   (
      WATCH_LIST
   ) ~NEWLINE* NEWLINE
;

domain_lookup
:
   LOOKUP
   (
      SOURCE_INTERFACE iname = interface_name
      | DISABLE
   ) NEWLINE
;

domain_name
:
   NAME hostname = variable_hostname NEWLINE
;

domain_name_server
:
   NAME_SERVER hostname = variable_hostname NEWLINE
;

dspf_null
:
   NO?
   (
      ASSOCIATE
      | DESCRIPTION
      | CODEC
      | MAXIMUM
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

eh_null
:
   NO?
   (
      ACTION
      | ASYNCHRONOUS
      | DELAY
      | TRIGGER
   ) ~NEWLINE* NEWLINE
;

enable_null
:
   (
      ENCRYPTED_PASSWORD
      | READ_ONLY_PASSWORD
      | SUPER_USER_PASSWORD
      | TELNET
   ) ~NEWLINE* NEWLINE
;

enable_password
:
   PASSWORD
   (
      (
         (
            sha512pass = SHA512_PASSWORD
         ) seed = PASSWORD_SEED?
      )
      |
      (
         DEC pass = variable
      )
   ) NEWLINE
;

enable_secret
:
   SECRET
   (
      (
         DEC pass = variable_secret
      )
      | double_quoted_string
   ) NEWLINE
;

event_null
:
   NO?
   (
      ACTION
      | EVENT
      | SET
   ) ~NEWLINE* NEWLINE
;

failover_lan
:
   LAN failover_lan_tail
;

failover_lan_tail
:
   flan_interface
   | flan_unit
;

failover_link
:
   LINK name = variable iface = interface_name NEWLINE
;

failover_interface
:
   INTERFACE IP name = variable pip = IP_ADDRESS pmask = IP_ADDRESS STANDBY sip
   = IP_ADDRESS NEWLINE
;

flan_interface
:
   INTERFACE name = variable iface = interface_name NEWLINE
;

flan_unit
:
   UNIT
   (
      PRIMARY
      | SECONDARY
   ) NEWLINE
;

flow_null
:
   NO?
   (
      CACHE
      | COLLECT
      | DESCRIPTION
      | DESTINATION
      | EXPORT_PROTOCOL
      | EXPORTER
      | MATCH
      | RECORD
      | SOURCE
      | STATISTICS
      | TRANSPORT
   ) ~NEWLINE* NEWLINE
;

flow_version
:
   NO? VERSION ~NEWLINE* NEWLINE
   (
      flowv_null
   )*
;

flowv_null
:
   NO?
   (
      OPTIONS
      | TEMPLATE
   ) ~NEWLINE* NEWLINE
;

gae_null
:
   NO?
   (
      SMTP_SERVER
   ) ~NEWLINE* NEWLINE
;

gk_null
:
   NO?
   (
      GW_TYPE_PREFIX
      | LRQ
      | SHUTDOWN
      | ZONE
   ) ~NEWLINE* NEWLINE
;

gpsec_null
:
   NO?
   (
      AGE
      | DELETE_DYNAMIC_LEARN
   ) ~NEWLINE* NEWLINE
;

hardware_null
:
   NO?
   (
      IFACL
      | QOS
      | RBACL
      | SPAN
      | VACL
   ) ~NEWLINE* NEWLINE
;

ids_ap_classification_rule
:
   AP_CLASSIFICATION_RULE double_quoted_string NEWLINE
   (
      ids_ap_classification_rule_null
   )*
;

ids_ap_classification_rule_null
:
   NO?
   (
      CONF_LEVEL_INCR
      | DISCOVERED_AP_CNT
      | SSID
      | SNR_MAX
      | SNR_MIN
   ) ~NEWLINE* NEWLINE
;

ids_ap_rule_matching
:
   AP_RULE_MATCHING NEWLINE
   (
      ids_ap_rule_matching_null
   )*
;

ids_ap_rule_matching_null
:
   NO?
   (
      RULE_NAME
   ) ~NEWLINE* NEWLINE
;

ids_dos_profile
:
   DOS_PROFILE double_quoted_string NEWLINE
   (
      ids_dos_profile_null
   )*
;

ids_dos_profile_null
:
   NO?
   (
      DETECT_AP_FLOOD
      | DETECT_CHOPCHOP_ATTACK
      | DETECT_CLIENT_FLOOD
      | DETECT_CTS_RATE_ANOMALY
      | DETECT_EAP_RATE_ANOMALY
      | DETECT_HT_40MHZ_INTOLERANCE
      | DETECT_INVALID_ADDRESS_COMBINATION
      | DETECT_MALFORMED_ASSOCIATION_REQUEST
      | DETECT_MALFORMED_AUTH_FRAME
      | DETECT_MALFORMED_HTIE
      | DETECT_MALFORMED_LARGE_DURATION
      | DETECT_OVERFLOW_EAPOL_KEY
      | DETECT_OVERFLOW_IE
      | DETECT_RATE_ANOMALIES
      | DETECT_RTS_RATE_ANOMALY
      | DETECT_TKIP_REPLAY_ATTACK
   ) ~NEWLINE* NEWLINE
;

ids_general_profile
:
   GENERAL_PROFILE double_quoted_string NEWLINE
   (
      ids_general_profile_null
   )*
;

ids_general_profile_null
:
   NO?
   (
      WIRED_CONTAINMENT
      | WIRELESS_CONTAINMENT
   ) ~NEWLINE* NEWLINE
;

ids_impersonation_profile
:
   IMPERSONATION_PROFILE double_quoted_string NEWLINE
   (
      ids_impersonation_profile_null
   )*
;

ids_impersonation_profile_null
:
   NO?
   (
      DETECT_AP_IMPERSONATION
      | DETECT_BEACON_WRONG_CHANNEL
      | DETECT_HOTSPOTTER
   ) ~NEWLINE* NEWLINE
;

ids_null
:
   NO?
   (
      MANAGEMENT_PROFILE
      | RATE_THRESHOLDS_PROFILE
      | SIGNATURE_PROFILE
      | WMS_LOCAL_SYSTEM_PROFILE
   ) ~NEWLINE* NEWLINE
;

ids_profile
:
   PROFILE double_quoted_string NEWLINE
   (
      ids_profile_null
   )*
;

ids_profile_null
:
   NO?
   (
      DOS_PROFILE
      | GENERAL_PROFILE
      | SIGNATURE_MATCHING_PROFILE
      | IMPERSONATION_PROFILE
      | UNAUTHORIZED_DEVICE_PROFILE
   ) ~NEWLINE* NEWLINE
;

ids_signature_matching_profile
:
   SIGNATURE_MATCHING_PROFILE double_quoted_string NEWLINE
   (
      ids_signature_matching_profile_null
   )*
;

ids_signature_matching_profile_null
:
   NO?
   (
      SIGNATURE
   ) ~NEWLINE* NEWLINE
;

ids_unauthorized_device_profile
:
   UNAUTHORIZED_DEVICE_PROFILE double_quoted_string NEWLINE
   (
      ids_unauthorized_device_profile_null
   )*
;

ids_unauthorized_device_profile_null
:
   NO?
   (
      DETECT_ADHOC_NETWORK
      | DETECT_BAD_WEP
      | DETECT_HT_GREENFIELD
      | DETECT_INVALID_MAC_OUI
      | DETECT_MISCONFIGURED_AP
      | DETECT_VALID_SSID_MISUSE
      | DETECT_WIRELESS_BRIDGE
      | DETECT_WIRELESS_HOSTED_NETWORK
      | PRIVACY
      | PROTECT_SSID
      | PROTECT_VALID_STA
      | REQUIRE_WPA
      | SUSPECT_ROGUE_CONF_LEVEL
      | VALID_AND_PROTECTED_SSID
   ) ~NEWLINE* NEWLINE
;

ids_wms_general_profile
:
   WMS_GENERAL_PROFILE NEWLINE
   (
      ids_wms_general_profile_null
   )*
;

ids_wms_general_profile_null
:
   NO?
   (
      COLLECT_STATS
   ) ~NEWLINE* NEWLINE
;

ifmap_null
:
   NO?
   (
      ENABLE
   ) ~NEWLINE* NEWLINE
;

iimgp_stanza
:
   allow_iimgp_stanza
;

imgp_stanza
:
   interface_imgp_stanza
   | null_imgp_stanza
;

inband_mgp_stanza
:
   (
      INBAND
      | OUT_OF_BAND
   ) NEWLINE imgp_stanza*
;

interface_imgp_stanza
:
   INTERFACE ~NEWLINE* NEWLINE iimgp_stanza*
;

interface_multicast_stanza
:
   INTERFACE interface_name NEWLINE interface_multicast_tail*
;

interface_multicast_tail
:
   (
      BOUNDARY
      | BSR_BORDER
      | DISABLE
      | DR_PRIORITY
      | ENABLE
      | ROUTER
   ) ~NEWLINE* NEWLINE
;

ip_as_path_regex_mode_stanza
:
   IP AS_PATH REGEX_MODE
   (
      ASN
      | STRING
   ) NEWLINE
;

ip_dhcp_null
:
   (
      EXCLUDED_ADDRESS
      | PACKET
      | SMART_RELAY
      | SNOOPING
   ) ~NEWLINE* NEWLINE
;

ip_dhcp_pool
:
   POOL name = variable NEWLINE
   (
      ip_dhcp_pool_null
   )*
;

ip_dhcp_pool_null
:
   NO?
   (
      AUTHORITATIVE
      | BOOTFILE
      | CLIENT_IDENTIFIER
      | CLIENT_NAME
      | DEFAULT_ROUTER
      | DNS_SERVER
      | DOMAIN_NAME
      | HARDWARE_ADDRESS
      | HOST
      | LEASE
      | NETWORK
      | NEXT_SERVER
      | OPTION
   ) ~NEWLINE* NEWLINE
;

ip_dhcp_relay
:
   RELAY
   (
      NEWLINE
      | ip_dhcp_relay_null
      | ip_dhcp_relay_server
   )
;

ip_dhcp_relay_null
:
   (
      ALWAYS_ON
      | INFORMATION
      | OPTION
      | SOURCE_ADDRESS
      | SOURCE_INTERFACE
      | SUB_OPTION
      | USE_LINK_ADDRESS
   ) ~NEWLINE* NEWLINE
;

ip_dhcp_relay_server
:
   SERVER
   (
      ip = IP_ADDRESS
      | ip6 = IPV6_ADDRESS
   ) NEWLINE
;

ip_domain_lookup
:
   LOOKUP
   (
      SOURCE_INTERFACE iname = interface_name
   )? NEWLINE
;

ip_domain_name
:
   NAME
   (
      VRF vrf = variable
   )? hostname = variable_hostname NEWLINE
;

ip_domain_null
:
   (
      LIST
   ) ~NEWLINE* NEWLINE
;

ip_nat_null
:
   (
      INSIDE
      | LOG
      | OUTSIDE
      | TRANSLATION
   ) ~NEWLINE* NEWLINE
;

ip_nat_pool
:
   (
      POOL name = variable PREFIX_LENGTH prefix_length = DEC NEWLINE
      ip_nat_pool_range*
   )
   |
   (
      POOL name = variable first = IP_ADDRESS last = IP_ADDRESS
      (
      // intentional blank

         |
         (
            NETMASK mask = IP_ADDRESS
         )
         |
         (
            PREFIX_LENGTH prefix_length = DEC
         )
      ) NEWLINE
   )
;

ip_nat_pool_range
:
   RANGE first = IP_ADDRESS last = IP_ADDRESS NEWLINE
;

ip_probe_null
:
   NO?
   (
      BURST_SIZE
      | FREQUENCY
      | MODE
      | RETRIES
   ) ~NEWLINE* NEWLINE
;

ip_route_stanza
:
   (
      IP
      | MANAGEMENT
   ) ROUTE
   (
      VRF vrf = variable
   )? ip_route_tail
;

ip_route_tail
:
   (
      (
         address = IP_ADDRESS mask = IP_ADDRESS
      )
      | prefix = IP_PREFIX
   )
   (
      nexthopip = IP_ADDRESS
      | nexthopprefix = IP_PREFIX
      | GLOBAL
      | nexthopint = interface_name
   )*
   (
      (
         (
            ADMIN_DIST
            | ADMIN_DISTANCE
         )? distance = DEC
      )
      |
      (
         METRIC metric = DEC
      )
      |
      (
         TAG tag = DEC
      )
      | perm = PERMANENT
      |
      (
         TRACK track = DEC
      )
      |
      (
         NAME variable
      )
   )* NEWLINE
;

ip_sla_null
:
   NO?
   (
      FREQUENCY
      | HISTORY
      | HOPS_OF_STATISTICS_KEPT
      | ICMP_ECHO
      | PATH_ECHO
      | PATHS_OF_STATISTICS_KEPT
      | REQUEST_DATA_SIZE
      | SAMPLES_OF_HISTORY_KEPT
      | TAG
      | TOS
      | UDP_JITTER
   ) ~NEWLINE* NEWLINE
;

ip_ssh_null
:
   (
      AUTHENTICATION_RETRIES
      | CLIENT
      | MAXSTARTUPS
      | PORT
      | RSA
      | SERVER
      |
      (
         NO SHUTDOWN
      )
      | SOURCE_INTERFACE
      | TIME_OUT
   ) ~NEWLINE* NEWLINE
;

ip_ssh_private_key
:
   PRIVATE_KEY ~END_CADANT+ END_CADANT
;

ip_ssh_public_key
:
   PUBLIC_KEY ~END_CADANT+ END_CADANT
;

ip_ssh_pubkey_chain
:
   PUBKEY_CHAIN NEWLINE
   (
      (
         KEY_HASH
         | QUIT
         | USERNAME
      ) ~NEWLINE* NEWLINE
   )+
;

ip_ssh_version
:
   VERSION version = DEC NEWLINE
;

ipc_association
:
   ASSOCIATION ~NEWLINE* NEWLINE
   (
      ipca_null
   )*
;

ipca_null
:
   NO?
   (
      ASSOC_RETRANSMIT
      | LOCAL_IP
      | LOCAL_PORT
      | PATH_RETRANSMIT
      | PROTOCOL
      | REMOTE_IP
      | REMOTE_PORT
      | RETRANSMIT_TIMEOUT
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

ipdg_address
:
   ip = IP_ADDRESS NEWLINE
;

ipdg_null
:
   (
      IMPORT
   ) ~NEWLINE* NEWLINE
;

ispla_operation
:
   NO? OPERATION ~NEWLINE* NEWLINE
   (
      ipslao_type
   )*
;

ipsla_reaction
:
   NO? REACTION ~NEWLINE* NEWLINE
   (
      ipslar_react
   )*
;

ipsla_responder
:
   NO? RESPONDER ~NEWLINE* NEWLINE
   (
      ipslarp_null
   )*
;

ipsla_schedule
:
   NO? SCHEDULE ~NEWLINE* NEWLINE
   (
      ipslas_null
   )*
;

ipslao_type
:
   NO? TYPE ~NEWLINE* NEWLINE
   (
      ipslaot_null
      | ipslaot_statistics
   )*
;

ipslaot_null
:
   NO?
   (
      DESTINATION
      | FREQUENCY
      | SOURCE
      | TIMEOUT
      | TOS
      | VERIFY_DATA
   ) ~NEWLINE* NEWLINE
;

ipslaot_statistics
:
   NO? STATISTICS ~NEWLINE* NEWLINE
   (
      ipslaots_null
   )*
;

ipslaots_null
:
   NO?
   (
      BUCKETS
   ) ~NEWLINE* NEWLINE
;

ipslar_react
:
   NO? REACT ~NEWLINE* NEWLINE
   (
      ispalrr_null
   )*
;

ipslarp_null
:
   NO?
   (
      TYPE
   ) ~NEWLINE* NEWLINE
;

ispalrr_null
:
   NO?
   (
      ACTION
      | THRESHOLD
   ) ~NEWLINE* NEWLINE
;

ipslas_null
:
   NO?
   (
      LIFE
      | START_TIME
   ) ~NEWLINE* NEWLINE
;

l2_null
:
   NO?
   (
      BRIDGE_DOMAIN
      | MTU
      | NEIGHBOR
      | VPN
   ) ~NEWLINE* NEWLINE
;

l2tpc_null
:
   NO? DEFAULT?
   (
      AUTHENTICATION
      | COOKIE
      | HELLO
      | HIDDEN_LITERAL
      | HOSTNAME
      | PASSWORD
      | RECEIVE_WINDOW
      | RETRANSMIT
      | TIMEOUT
   ) ~NEWLINE* NEWLINE
;

l2vpn_bridge_group
:
   BRIDGE GROUP name = variable NEWLINE
   (
      lbg_bridge_domain
   )*
;

l2vpn_logging
:
   LOGGING NEWLINE
   (
      (
         BRIDGE_DOMAIN
         | PSEUDOWIRE
         | VFI
      ) NEWLINE
   )+
;

l2vpn_xconnect
:
   XCONNECT GROUP variable NEWLINE
   (
      l2vpn_xconnect_p2p
   )*
;

l2vpn_xconnect_p2p
:
   NO? P2P ~NEWLINE* NEWLINE
   (
      lxp_neighbor
      | lxp_null
   )*
;

lbg_bridge_domain
:
   BRIDGE_DOMAIN name = variable NEWLINE
   (
      lbgbd_mac
      | lbgbd_null
      | lbgbd_vfi
   )*
;

lbgbd_mac
:
   NO? MAC ~NEWLINE* NEWLINE
   (
      lbgbdm_limit
   )*
;

lbgbd_null
:
   NO?
   (
      INTERFACE
      | MTU
      | NEIGHBOR
      | ROUTED
   ) ~NEWLINE* NEWLINE
;

lbgbd_vfi
:
   NO? VFI ~NEWLINE* NEWLINE
   (
      lbgbdv_null
   )*
;

lbgbdm_limit
:
   NO? LIMIT ~NEWLINE* NEWLINE
   (
      lbgbdml_null
   )*
;

lbgbdml_null
:
   NO?
   (
      ACTION
      | MAXIMUM
   ) ~NEWLINE* NEWLINE
;

lbgbdv_null
:
   NO?
   (
      NEIGHBOR
   ) ~NEWLINE* NEWLINE
;

license_null
:
   NO?
   (
      CENTRALIZED_LICENSING_ENABLE
   ) ~NEWLINE* NEWLINE
;

lpts_null
:
   NO?
   (
      FLOW
   ) ~NEWLINE* NEWLINE
;

lxp_neighbor
:
   NO? NEIGHBOR ~NEWLINE* NEWLINE
   (
      lxpn_l2tp
      | lxpn_null
   )*
;

lxp_null
:
   NO?
   (
      INTERFACE
      | MONITOR_SESSION
   ) ~NEWLINE* NEWLINE
;

lxpn_null
:
   NO?
   (
      SOURCE
   ) ~NEWLINE* NEWLINE
;

lxpn_l2tp
:
   NO? L2TP ~NEWLINE* NEWLINE
   (
      lxpnl_null
   )*
;

lxpnl_null
:
   NO?
   (
      LOCAL
      | REMOTE
   ) ~NEWLINE* NEWLINE
;

map_class_null
:
   NO?
   (
      DIALER
   ) ~NEWLINE* NEWLINE
;

management_api
:
   API HTTP_COMMANDS NEWLINE
   (
      management_api_null
      | management_api_vrf
   )*
;

management_api_null
:
   NO?
   (
      AUTHENTICATION
      | EXIT
      | IDLE_TIMEOUT
      | PROTOCOL
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

management_api_vrf
:
   VRF name = variable NEWLINE
   (
      management_api_vrf_null
   )*
;

management_api_vrf_null
:
   NO?
   (
      SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

management_console
:
   CONSOLE NEWLINE
   (
      management_console_null
   )*
;

management_console_null
:
   NO?
   (
      IDLE_TIMEOUT
   ) ~NEWLINE* NEWLINE
;

management_egress_interface_selection
:
   MANAGEMENT EGRESS_INTERFACE_SELECTION NEWLINE
   (
      management_egress_interface_selection_null
   )*
   (
      EXIT NEWLINE
   )?
;

management_egress_interface_selection_null
:
   NO?
   (
      APPLICATION
   ) ~NEWLINE* NEWLINE
;

management_ssh
:
   SSH NEWLINE
   (
      management_ssh_null
   )*
;

management_ssh_null
:
   NO?
   (
      AUTHENTICATION
      | IDLE_TIMEOUT
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

management_telnet
:
   TELNET NEWLINE
   (
      management_telnet_ip_access_group
      | management_telnet_null
   )*
;

management_telnet_ip_access_group
:
   IP ACCESS_GROUP name = variable
   (
      IN
      | OUT
   ) NEWLINE
;

management_telnet_null
:
   NO?
   (
      IDLE_TIMEOUT
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

mgp_stanza
:
   inband_mgp_stanza
;

monitor_destination
:
   NO? DESTINATION ~NEWLINE* NEWLINE
   (
      monitor_destination_null
   )*
;

monitor_destination_null
:
   NO?
   (
      ERSPAN_ID
      | IP
      | MTU
      | ORIGIN
   ) ~NEWLINE* NEWLINE
;

monitor_null
:
   NO?
   (
      BUFFER_SIZE
      | DESCRIPTION
      | SHUTDOWN
      | SOURCE
   ) ~NEWLINE* NEWLINE
;

monitor_session_null
:
   NO?
   (
      DESTINATION
   ) ~NEWLINE* NEWLINE
;

mp_null
:
   NO?
   (
      CONNECT_SOURCE
      | DESCRIPTION
      | MESH_GROUP
      | REMOTE_AS
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

mt_null
:
   NO?
   (
      ADDRESS
   ) ~NEWLINE* NEWLINE
;

multicast_routing_stanza
:
   MULTICAST_ROUTING NEWLINE
   (
      address_family_multicast_stanza
   )*
;

no_aaa_group_server_stanza
:
   NO AAA GROUP SERVER ~NEWLINE* NEWLINE
;

no_failover
:
   NO FAILOVER NEWLINE
;

no_ip_access_list_stanza
:
   NO IP ACCESS_LIST ~NEWLINE* NEWLINE
;

null_af_multicast_tail
:
   NSF NEWLINE
;

vrfd_af_null
:
   NO?
   (
      MAXIMUM
   ) ~NEWLINE* NEWLINE
;

null_imgp_stanza
:
   NO?
   (
      VRF
   ) ~NEWLINE* NEWLINE
;

nv_satellite
:
   NO?
   (
      SATELLITE
   ) ~NEWLINE* NEWLINE
   (
      nvs_null
   )*
;

nvs_null
:
   NO?
   (
      DESCRIPTION
      | IP
      | SERIAL_NUMBER
      | TYPE
   ) ~NEWLINE* NEWLINE
;

of_null
:
   NO?
   (
      BIND
      | CONTROLLER
      | DEFAULT_ACTION
      | DESCRIPTION
      | ENABLE
   ) ~NEWLINE* NEWLINE
;

peer_sa_filter
:
   SA_FILTER
   (
      IN
      | OUT
   )
   (
      LIST
      | RP_LIST
   ) name = variable NEWLINE
;

peer_stanza
:
   PEER IP_ADDRESS NEWLINE
   (
      mp_null
      | peer_sa_filter
   )*
;

phone_proxy_null
:
   NO?
   (
      CIPC
      | CTL_FILE
      | DISABLE
      | MEDIA_TERMINATION
      | PROXY_SERVER
      | TFTP_SERVER
      | TLS_PROXY
   ) ~NEWLINE* NEWLINE
;

qm_length
:
   LENGTH ~NEWLINE* NEWLINE
;

qm_streaming
:
   STREAMING NEWLINE
   (
      qms_null
   )*
;

qms_null
:
   NO?
   (
      MAX_CONNECTIONS
      | SHUTDOWN
   ) ~NEWLINE* NEWLINE
;

redundancy_linecard_group
:
   LINECARD_GROUP ~NEWLINE* NEWLINE
   (
      rlcg_null
   )*
;

redundancy_main_cpu
:
   MAIN_CPU ~NEWLINE* NEWLINE
   (
      redundancy_main_cpu_null
   )*
;

redundancy_main_cpu_null
:
   NO?
   (
      AUTO_SYNC
   ) ~NEWLINE* NEWLINE
;

redundancy_null
:
   NO?
   (
      KEEPALIVE_ENABLE
      | MODE
      | NOTIFICATION_TIMER
      | PROTOCOL
      | SCHEME
   ) ~NEWLINE* NEWLINE
;

rf_arm_profile
:
   ARM_PROFILE double_quoted_string NEWLINE
   (
      rf_arm_profile_null
   )*
;

rf_arm_profile_null
:
   NO?
   (
      ASSIGNMENT
      | BACKOFF_TIME
      | ERROR_RATE_THRESHOLD
      | FREE_CHANNEL_INDEX
      | IDEAL_COVERAGE_INDEX
      | MAX_TX_POWER
      | MIN_TX_POWER
      | ROGUE_AP_AWARE
      | SCANNING
   ) ~NEWLINE* NEWLINE
;

rf_null
:
   NO?
   (
      AM_SCAN_PROFILE
      | ARM_RF_DOMAIN_PROFILE
      | EVENT_THRESHOLDS_PROFILE
      | OPTIMIZATION_PROFILE
   ) ~NEWLINE* NEWLINE
;

rf_dot11a_radio_profile
:
   DOT11A_RADIO_PROFILE double_quoted_string NEWLINE
   (
      rf_dot11a_radio_profile_null
   )*
;

rf_dot11a_radio_profile_null
:
   NO?
   (
      ARM_PROFILE
      | MODE
      | SPECTRUM_LOAD_BALANCING
      | SPECTRUM_MONITORING
   ) ~NEWLINE* NEWLINE
;

rf_dot11g_radio_profile
:
   DOT11G_RADIO_PROFILE double_quoted_string NEWLINE
   (
      rf_dot11g_radio_profile_null
   )*
;

rf_dot11g_radio_profile_null
:
   NO?
   (
      ARM_PROFILE
      | MODE
      | SPECTRUM_LOAD_BALANCING
      | SPECTRUM_MONITORING
   ) ~NEWLINE* NEWLINE
;

rlcg_null
:
   NO?
   (
      MEMBER
      | MODE
      | REVERTIVE
      | RF_SWITCH
   ) ~NEWLINE* NEWLINE
;

rmc_null
:
   NO?
   (
      MAXIMUM
   ) ~NEWLINE* NEWLINE
;

role_null
:
   NO?
   (
      DESCRIPTION
      | RULE
   ) ~NEWLINE* NEWLINE
;

router_multicast_stanza
:
   IPV6? ROUTER
   (
      IGMP
      | MLD
      | MSDP
      | PIM
   ) NEWLINE router_multicast_tail
;

router_multicast_tail
:
   (
      address_family_multicast_stanza
      |
      (
         INTERFACE ALL ~NEWLINE* NEWLINE
      )
      | interface_multicast_stanza
      | null_inner
      | peer_stanza
      | rmc_null
   )*
;

s_airgroupservice
:
   AIRGROUPSERVICE ~NEWLINE* NEWLINE
   (
      ags_null
   )*
;

s_ap
:
   AP
   (
      ap_null
      | ap_regulatory_domain_profile
      | ap_system_profile
   )
;

s_ap_group
:
   AP_GROUP double_quoted_string NEWLINE
   (
      apg_null
   )*
;

s_ap_name
:
   AP_NAME double_quoted_string NEWLINE
   (
      apn_null
   )*
;

s_application
:
   APPLICATION NEWLINE SERVICE name = variable ~NEWLINE* NEWLINE
   (
      PARAM ~NEWLINE* NEWLINE
   )*
   (
      GLOBAL NEWLINE SERVICE name = variable ~NEWLINE* NEWLINE
   )?
;

s_application_var
:
   APPLICATION name = variable NEWLINE
   (
      av_null
   )*
;

s_archive
:
   ARCHIVE ~NEWLINE* NEWLINE
   (
      archive_log
      | archive_null
   )*
;

s_authentication
:
   AUTHENTICATION ~NEWLINE* NEWLINE
;

s_cluster
:
   NO? CLUSTER
   (
      ENABLE
      | RUN
   ) ~NEWLINE* NEWLINE
;

s_call_manager_fallback
:
   NO? CALL_MANAGER_FALLBACK NEWLINE
   (
      cmf_null
   )+
;

s_configure
:
   NO? CONFIGURE
   (
      configure_maintenance
      | configure_null
   )
;

s_control_plane
:
   CONTROL_PLANE
   (
      SLOT DEC
   )? NEWLINE s_control_plane_tail*
;

s_control_plane_tail
:
   cp_ip_access_group
   | cp_ip_flow
   | cp_management_plane
   | cp_null
   | cp_service_policy
;

s_control_plane_security
:
   CONTROL_PLANE_SECURITY NEWLINE
   (
      cps_null
   )*
;

s_cops
:
   COPS
   (
      cops_listener
   )
;

s_cos_queue_group
:
   COS_QUEUE_GROUP ~NEWLINE* NEWLINE
   (
      cqg_null
   )*
;

s_ctl_file
:
   NO? CTL_FILE ~NEWLINE* NEWLINE
   (
      ctlf_null
   )*
;

s_cvx
:
   CVX NEWLINE
   (
      cvx_null
   )*
;

s_daemon
:
   DAEMON ~NEWLINE* NEWLINE
   (
      daemon_null
   )*
;

s_dhcp
:
   NO? DHCP ~NEWLINE* NEWLINE
   (
      dhcp_null
      | dhcp_profile
   )*
;

s_dialer
:
   DIALER
   (
      dialer_group
      | dialer_null
   )
;

s_dial_peer
:
   DIAL_PEER ~NEWLINE* NEWLINE
   (
      NO?
      (
         CALL_BLOCK
         | CODEC
         | DESCRIPTION
         | DESTINATION_PATTERN
         | DIRECT_INWARD_DIAL
         | DTMF_RELAY
         | FAX
         | FORWARD_DIGITS
         | INCOMING
         |
         (
            IP
            (
               QOS
            )
         )
         | MEDIA
         | PORT
         | PREFERENCE
         | PREFIX
         | PROGRESS_IND
         | SERVICE
         | SESSION
         | SHUTDOWN
         | SIGNALING
         | TRANSLATION_PROFILE
         | VAD
         | VOICE_CLASS
      ) ~NEWLINE* NEWLINE
   )*
;

s_domain
:
   DOMAIN
   (
      VRF vrf = variable
   )?
   (
      domain_lookup
      | domain_name
      | domain_name_server
   )
;

s_domain_name
:
   DOMAIN_NAME hostname = variable_hostname NEWLINE
;

s_dot11
:
   DOT11 ~NEWLINE* NEWLINE
   (
      d11_null
   )*
;

s_dspfarm
:
   NO? DSPFARM ~NEWLINE* NEWLINE
   (
      dspf_null
   )*
;

s_dynamic_access_policy_record
:
   NO? DYNAMIC_ACCESS_POLICY_RECORD ~NEWLINE* NEWLINE
   (
      dapr_null
      | dapr_webvpn
   )*
;

s_enable
:
   ENABLE
   (
      enable_null
      | enable_password
      | enable_secret
   )
;

s_event
:
   NO? EVENT ~NEWLINE* NEWLINE
   (
      event_null
   )*
;

s_event_handler
:
   NO? EVENT_HANDLER ~NEWLINE* NEWLINE
   (
      eh_null
   )*
;

s_failover
:
   FAILOVER s_failover_tail
;

s_failover_tail
:
   NEWLINE
   | failover_lan
   | failover_link
   | failover_interface
;

s_feature
:
   NO? FEATURE
   (
      words += variable
   )+ NEWLINE
;

s_flow
:
   FLOW
   (
      EXPORTER
      | EXPORTER_MAP
      | HARDWARE
      | MONITOR
      | MONITOR_MAP
      | PLATFORM
      | RECORD
   ) ~NEWLINE* NEWLINE
   (
      flow_null
      | flow_version
   )*
;

s_flow_sampler_map
:
   NO? FLOW_SAMPLER_MAP ~NEWLINE* NEWLINE fsm_mode?
;

fsm_mode
:
   MODE RANDOM ONE_OUT_OF DEC NEWLINE
;

s_gatekeeper
:
   GATEKEEPER NEWLINE
   (
      gk_null
   )*
;

s_global_port_security
:
   GLOBAL_PORT_SECURITY NEWLINE
   (
      gpsec_null
   )*
;

s_guest_access_email
:
   GUEST_ACCESS_EMAIL NEWLINE
   (
      gae_null
   )*
;

s_hardware
:
   NO? HARDWARE ~NEWLINE* NEWLINE
   (
      hardware_null
   )*
;

s_hostname
:
   (
      HOSTNAME
      | SWITCHNAME
   )
   (
      quoted_name = double_quoted_string
      |
      (
         (
            name_parts += ~NEWLINE
         )+
      )
   ) NEWLINE
;

s_ids
:
   IDS
   (
      ids_ap_classification_rule
      | ids_ap_rule_matching
      | ids_dos_profile
      | ids_general_profile
      | ids_impersonation_profile
      | ids_null
      | ids_profile
      | ids_signature_matching_profile
      | ids_unauthorized_device_profile
      | ids_wms_general_profile
   )
;

s_ifmap
:
   IFMAP ~NEWLINE* NEWLINE
   (
      ifmap_null
   )*
;

s_interface_line
:
   NO? INTERFACE BREAKOUT ~NEWLINE* NEWLINE
;

s_ip_default_gateway
:
   NO? IP DEFAULT_GATEWAY
   (
      ipdg_address
      | ipdg_null
   )
;

s_ip_dhcp
:
   NO?
   (
      IP
      | IPV6
   ) DHCP
   (
      ip_dhcp_null
      | ip_dhcp_pool
      | ip_dhcp_relay
   )
;

s_ip_domain
:
   NO? IP DOMAIN
   (
      ip_domain_lookup
      | ip_domain_name
      | ip_domain_null
   )
;

s_ip_domain_name
:
   IP DOMAIN_NAME hostname = variable_hostname
   (
      USE_VRF variable
   )? NEWLINE
;

s_ip_name_server
:
   IP NAME_SERVER
   (
      VRF vrf = variable
   )?
   (
      hostnames += ip_hostname
   )+
   (
      USE_VRF vrf = variable
   )? NEWLINE
;

s_ip_nat
:
   NO? IP NAT
   (
      ip_nat_null
      | ip_nat_pool
   )
;

s_ip_probe
:
   IP PROBE ~NEWLINE* NEWLINE
   (
      ip_probe_null
   )*
;

s_ip_route_mos
:
   IP ROUTE IP_ADDRESS DEV interface_name NEWLINE
;

s_ip_sla
:
   NO? IP SLA ~NEWLINE* NEWLINE
   (
      ip_sla_null
   )*
;

s_ip_source_route
:
   NO? IP SOURCE_ROUTE NEWLINE
;

s_ip_ssh
:
   NO? IP SSH
   (
      ip_ssh_private_key
      | ip_ssh_pubkey_chain
      | ip_ssh_public_key
      | ip_ssh_version
      | ip_ssh_null
   )
;

s_ip_tacacs_source_interface
:
   IP TACACS
   (
      VRF vrf = variable
   )? SOURCE_INTERFACE iname = interface_name NEWLINE
;

s_ip_wccp
:
   NO? IP WCCP
   (
      VRF vrf = variable
   )?
   (
      wccp_id
      | wccp_null
   )
;

s_ipc
:
   IPC ~NEWLINE* NEWLINE
   (
      ipc_association
   )*
;

s_ipsla
:
   NO? IPSLA ~NEWLINE* NEWLINE
   (
      ispla_operation
      | ipsla_reaction
      | ipsla_responder
      | ipsla_schedule
   )*
;

s_l2
:
   NO? L2 ~NEWLINE* NEWLINE
   (
      l2_null
   )*
;

s_l2tp_class
:
   NO? L2TP_CLASS name = variable NEWLINE
   (
      l2tpc_null
   )*
;

s_l2vpn
:
   NO? L2VPN ~NEWLINE* NEWLINE
   (
      l2vpn_bridge_group
      | l2vpn_logging
      | l2vpn_xconnect
   )*
;

s_license
:
   NO? LICENSE ~NEWLINE* NEWLINE
   (
      license_null
   )*
;

s_lpts
:
   NO? LPTS ~NEWLINE* NEWLINE
   (
      lpts_null
   )*
;

s_management
:
   MANAGEMENT
   (
      management_api
      | management_console
      | management_egress_interface_selection
      | management_ssh
      | management_telnet
   )
;

s_map_class
:
   NO? MAP_CLASS ~NEWLINE* NEWLINE
   (
      map_class_null
   )*
;

s_media_termination
:
   NO? MEDIA_TERMINATION ~NEWLINE* NEWLINE
   (
      mt_null
   )*
;

s_monitor
:
   NO? MONITOR ~NEWLINE* NEWLINE
   (
      monitor_destination
      | monitor_null
   )*
;

s_monitor_session
:
   NO? MONITOR_SESSION ~NEWLINE* NEWLINE
   (
      monitor_session_null
   )*
;

s_mtu
:
   MTU variable DEC NEWLINE
;

s_name
:
   NAME variable variable ~NEWLINE* NEWLINE
;

s_no_access_list_extended
:
   NO ACCESS_LIST ACL_NUM_EXTENDED NEWLINE
;

s_no_access_list_standard
:
   NO ACCESS_LIST ACL_NUM_STANDARD NEWLINE
;

s_nv
:
   NO? NV NEWLINE
   (
      nv_satellite
   )*
;

s_openflow
:
   NO? OPENFLOW ~NEWLINE* NEWLINE
   (
      of_null
   )*
;

s_passwd
:
   NO? PASSWD pass = variable ENCRYPTED? NEWLINE
;

s_phone_proxy
:
   NO? PHONE_PROXY ~NEWLINE* NEWLINE
   (
      phone_proxy_null
   )*
;

s_privilege
:
   NO? PRIVILEGE
   (
      CLEAR
      | CMD
      | CONFIGURE
      | EXEC
      | INTERFACE
      | IPENACL
      | ROUTER
      | SHOW
   ) ~NEWLINE* NEWLINE
;

s_process_max_time
:
   NO? PROCESS_MAX_TIME DEC NEWLINE
;

s_queue_monitor
:
   QUEUE_MONITOR
   (
      qm_length
      | qm_streaming
   )
;

s_radius_server
:
   RADIUS SERVER name = variable NEWLINE
   (
      (
         ADDRESS
         | KEY
         | RETRANSMIT
         | TIMEOUT
      ) ~NEWLINE* NEWLINE
   )+
;

s_redundancy
:
   NO? REDUNDANCY ~NEWLINE* NEWLINE
   (
      redundancy_linecard_group
      | redundancy_main_cpu
      | redundancy_null
   )*
;

s_rf
:
   RF
   (
      rf_arm_profile
      | rf_null
      | rf_dot11a_radio_profile
      | rf_dot11g_radio_profile
   )
;

s_role
:
   NO? ROLE ~NEWLINE* NEWLINE
   (
      role_null
   )*
;

s_router_vrrp
:
   NO? ROUTER VRRP NEWLINE
   (
      vrrp_interface
   )*
;

s_sccp
:
   NO? SCCP ~NEWLINE* NEWLINE
   (
      sccp_null
   )*
;

s_service
:
   NO? SERVICE
   (
      words += variable
   )+ NEWLINE
;

s_sip_ua
:
   SIP_UA NEWLINE
   (
      sip_ua_null
   )*
;

s_sntp
:
   SNTP sntp_server
;

s_spanning_tree
:
   NO? SPANNING_TREE
   (
      spanning_tree_mst
      | spanning_tree_portfast
      | spanning_tree_pseudo_information
      | spanning_tree_null
      | NEWLINE
   )
;

s_ssh
:
   SSH
   (
      ssh_access_group
      | ssh_client
      | ssh_null
      | ssh_server
      | ssh_timeout
   )
;

s_statistics
:
   NO? STATISTICS ~NEWLINE* NEWLINE
   (
      statistics_null
   )*
;

s_stcapp
:
   STCAPP ~NEWLINE* NEWLINE
   (
      (
         CALL
         | CPTONE
         | FALLBACK_DN
         | PICKUP
         | PORT
         | PREFIX
      ) ~NEWLINE* NEWLINE
   )*
;

s_switchport
:
   SWITCHPORT DEFAULT MODE
   (
      ACCESS
      | ROUTED
   ) NEWLINE
;

s_system
:
   NO? SYSTEM
   (
      system_default
      | system_null
      | system_qos
   )
;

s_tacacs
:
   TACACS
   (
      t_null
      | t_server
      | t_source_interface
   )
;

s_tacacs_server
:
   NO? TACACS_SERVER
   (
      ts_common
      | ts_host
      |
      (
         ts_host ts_common*
      )
   )
;

s_tap
:
   NO? TAP ~NEWLINE* NEWLINE
   (
      tap_null
   )*
;

s_time_range
:
   TIME_RANGE name = variable PERIODIC? NEWLINE
   (
      tr_null
   )*
;

s_track
:
   TRACK ~NEWLINE* NEWLINE
   (
      track_null
   )*
;

s_tunnel_group
:
   NO? TUNNEL_GROUP ~NEWLINE* NEWLINE
   (
      tg_null
   )*
;

s_user_role
:
   USER_ROLE name = variable_permissive NEWLINE
   (
      ur_access_list
      | ur_null
   )*
;

s_username
:
   USERNAME
   (
      quoted_user = double_quoted_string
      | user = variable
   )
   (
      (
         u+ NEWLINE
      )
      |
      (
         NEWLINE
         (
            u NEWLINE
         )*
      )
   )
;

s_username_attributes
:
   USERNAME user = variable ATTRIBUTES NEWLINE
   (
      ua_null
   )*
;

s_vlan
:
   NO? VLAN
   (
      ACCESS_MAP
      |
      (
         variable_vlan? DEC
      )
   ) ~NEWLINE* NEWLINE
   (
      vlan_null
   )*
;

s_vlan_name
:
   VLAN_NAME name = variable_permissive NEWLINE
;

s_voice
:
   NO? VOICE
   (
      voice_class
      | voice_null
      | voice_service
      | voice_translation_profile
      | voice_translation_rule
   )
;

s_voice_card
:
   NO? VOICE_CARD ~NEWLINE* NEWLINE
   (
      vc_null
   )*
;

s_voice_port
:
   NO? VOICE_PORT ~NEWLINE* NEWLINE
   (
      vp_null
   )*
;

s_vpc
:
   NO? VPC ~NEWLINE* NEWLINE
   (
      vpc_null
   )*
;

s_vpdn_group
:
   NO? VPDN_GROUP ~NEWLINE* NEWLINE
   (
      vpdng_accept_dialin
      | vpdng_null
   )*
;

s_vpn
:
   NO? VPN ~NEWLINE* NEWLINE
   (
      vpn_null
   )*
;

s_vpn_dialer
:
   VPN_DIALER name = variable NEWLINE
   (
      vpn_dialer_null
   )*
;

s_vrf_context
:
   VRF CONTEXT name = variable NEWLINE
   (
      vrfc_ip_route
      | vrfc_null
   )*
;

s_vrf_definition
:
   VRF DEFINITION? name = variable NEWLINE
   (
      vrfd_address_family
      | vrfd_null
   )*
   (
      EXIT_VRF NEWLINE
   )?
;

s_web_server
:
   WEB_SERVER PROFILE NEWLINE
   (
      web_server_null
   )*
;

s_webvpn
:
   NO? WEBVPN ~NEWLINE* NEWLINE
   (
      webvpn_null
   )*
;

s_wlan
:
   WLAN
   (
      wlan_null
      | wlan_ssid_profile
      | wlan_virtual_ap
   )
;

s_wsma
:
   WSMA ~NEWLINE* NEWLINE
   (
      wsma_null
   )*
;

s_xconnect_logging
:
   NO? XCONNECT LOGGING ~NEWLINE* NEWLINE
;

sccp_null
:
   NO?
   (
      ASSOCIATE
      | BIND
      | DESCRIPTION
      | SWITCHBACK
   ) ~NEWLINE* NEWLINE
;

sd_null
:
   (
      DCE_MODE
      | INTERFACE
      | LINK_FAIL
   ) ~NEWLINE* NEWLINE
;

sd_switchport
:
   SWITCHPORT
   (
      sd_switchport_blank
      | sd_switchport_null
      | sd_switchport_shutdown
   )
;

sd_switchport_blank
:
   NEWLINE
;

sd_switchport_null
:
   (
      FABRICPATH
      | MONITOR
   ) ~NEWLINE* NEWLINE
;

sd_switchport_shutdown
:
   SHUTDOWN NEWLINE
;

sip_ua_null
:
   NO?
   (
      CONNECTION_REUSE
      | RETRY
      | SET
      | SIP_SERVER
      | TIMERS
   ) ~NEWLINE* NEWLINE
;

sntp_server
:
   SERVER hostname = variable
   (
      VERSION version = DEC
   )? NEWLINE
;

spanning_tree_mst
:
   MST ~NEWLINE* NEWLINE spanning_tree_mst_null*
;

spanning_tree_mst_null
:
   NO?
   (
      INSTANCE
      | NAME
      | REVISION
   ) ~NEWLINE* NEWLINE
;

spanning_tree_portfast
:
   PORTFAST
   (
      bpdufilter = BPDUFILTER
      | bpduguard = BPDUGUARD
      | defaultLiteral = DEFAULT
      | edge = EDGE
   )* NEWLINE
;

spanning_tree_pseudo_information
:
   PSEUDO_INFORMATION NEWLINE
   (
      spti_null
   )*
;

spanning_tree_null
:
   (
      BACKBONEFAST
      | BRIDGE
      | DISPUTE
      | ETHERCHANNEL
      | EXTEND
      | FCOE
      | LOGGING
      | LOOPGUARD
      | MODE
      | OPTIMIZE
      | PATHCOST
      | PORT
      | UPLINKFAST
      | VLAN
   ) ~NEWLINE* NEWLINE
;

spti_null
:
   NO?
   (
      MST
   ) ~NEWLINE* NEWLINE
;

srlg_interface_numeric_stanza
:
   DEC ~NEWLINE* NEWLINE
;

srlg_interface_stanza
:
   INTERFACE ~NEWLINE* NEWLINE srlg_interface_numeric_stanza*
;

srlg_stanza
:
   SRLG NEWLINE srlg_interface_stanza*
;

ssh_access_group
:
   ACCESS_GROUP IPV6? name = variable NEWLINE
;

ssh_client
:
   CLIENT ~NEWLINE* NEWLINE
;

ssh_null
:
   (
      IP_ADDRESS
      | KEY
      | KEY_EXCHANGE
      | LOGIN_ATTEMPTS
      | MGMT_AUTH
      | STRICTHOSTKEYCHECK
      | VERSION
   ) ~NEWLINE* NEWLINE
;

ssh_server
:
   SERVER
   (
      (
         IPV4 ACCESS_LIST acl = variable
      )
      |
      (
         IPV6 ACCESS_LIST acl6 = variable
      )
      |
      (
         SESSION_LIMIT limit = DEC
      )
      | V2
      |
      (
         VRF vrf = variable
      )
   )* NEWLINE
;

ssh_timeout
:
   TIMEOUT DEC NEWLINE
;

stanza
:
   appletalk_access_list_stanza
   | asa_comment_stanza
   | as_path_set_stanza
   | banner_stanza
   | community_set_stanza
   | del_stanza
   | extended_access_list_stanza
   | extended_ipv6_access_list_stanza
   | ip_as_path_access_list_stanza
   | ip_as_path_regex_mode_stanza
   | ip_community_list_expanded_stanza
   | ip_community_list_standard_stanza
   | ip_prefix_list_stanza
   | ip_route_stanza
   | ipv6_prefix_list_stanza
   | ipx_sap_access_list_stanza
   | multicast_routing_stanza
   | no_aaa_group_server_stanza
   | no_failover
   | no_ip_access_list_stanza
   | no_ip_prefix_list_stanza
   | no_route_map_stanza
   | prefix_set_stanza
   | protocol_type_code_access_list_stanza
   | route_map_stanza
   | route_policy_stanza
   | router_bgp_stanza
   | router_hsrp_stanza
   | router_isis_stanza
   | router_multicast_stanza
   | rsvp_stanza
   | s_aaa
   | s_airgroupservice
   | s_ap
   | s_ap_group
   | s_ap_name
   | s_application
   | s_application_var
   | s_archive
   | s_authentication
   | s_cable
   | s_call_home
   | s_callhome
   | s_call_manager_fallback
   | s_class_map
   | s_cluster
   | s_configure
   | s_control_plane
   | s_control_plane_security
   | s_controller
   | s_cops
   | s_cos_queue_group
   | s_crypto
   | s_ctl_file
   | s_cvx
   | s_daemon
   | s_depi_class
   | s_depi_tunnel
   | s_dhcp
   | s_dialer
   | s_dial_peer
   | s_domain
   | s_domain_name
   | s_dot11
   | s_dspfarm
   | s_dynamic_access_policy_record
   | s_enable
   | s_ethernet_services
   | s_event
   | s_event_handler
   | s_failover
   | s_flow
   | s_flow_sampler_map
   | s_foundry_mac_access_list
   | s_feature
   | s_gatekeeper
   | s_global_port_security
   | s_guest_access_email
   | s_hardware
   | s_hostname
   | s_ids
   | s_ifmap
   |
   // do not move below s_interface
   s_interface_line
   | s_interface
   | s_ip_access_list_eth
   | s_ip_access_list_session
   | s_ip_default_gateway
   | s_ip_dhcp
   | s_ip_domain
   | s_ip_domain_name
   | s_ip_name_server
   | s_ip_nat
   | s_ip_pim
   | s_ip_probe
   | s_ip_route_mos
   | s_ip_sla
   | s_ip_source_route
   | s_ip_ssh
   | s_ip_tacacs_source_interface
   | s_ip_wccp
   | s_ipc
   | s_ipv6_router_ospf
   | s_ipsla
   | s_key
   | s_l2
   | s_l2tp_class
   | s_l2vpn
   | s_license
   |
   {!_cadant}?

   s_line
   |
   {_cadant}?

   s_line_cadant
   | s_logging
   | s_lpts
   | s_management
   | s_mac_access_list
   | s_mac_access_list_extended
   | s_map_class
   | s_media_termination
   | s_monitor
   | s_monitor_session
   | s_mpls_label_range
   | s_mpls_ldp
   | s_mpls_traffic_eng
   | s_mtu
   | s_name
   | s_netdestination
   | s_netdestination6
   | s_netservice
   | s_no_access_list_extended
   | s_no_access_list_standard
   | s_ntp
   | s_null
   | s_nv
   | s_object
   | s_object_group
   | s_openflow
   | s_passwd
   | s_phone_proxy
   | s_policy_map
   | s_privilege
   | s_process_max_time
   | s_qos_mapping
   | s_queue_monitor
   | s_radius_server
   | s_redundancy
   | s_rf
   | s_role
   | s_router_eigrp
   | s_router_ospf
   | s_router_ospfv3
   | s_router_rip
   | s_router_static
   | s_router_vrrp
   | s_sccp
   | s_service
   | s_sip_ua
   | s_snmp_server
   | s_sntp
   | s_spanning_tree
   | s_ssh
   | s_statistics
   | s_stcapp
   | s_switchport
   | s_system
   | s_table_map
   | s_tacacs
   | s_tacacs_server
   | s_tap
   | s_time_range
   | s_track
   | s_tunnel_group
   | s_user_role
   | s_username
   | s_username_attributes
   | s_vlan
   | s_vlan_name
   | s_voice
   | s_voice_card
   | s_voice_port
   | s_vpc
   | s_vpdn_group
   | s_vpn
   | s_vpn_dialer
   | s_vrf_context
   | s_vrf_definition
   | s_web_server
   | s_webvpn
   | s_wlan
   | s_wsma
   | s_xconnect_logging
   | srlg_stanza
   | standard_access_list_stanza
   | standard_ipv6_access_list_stanza
   | switching_mode_stanza
;

statistics_null
:
   NO?
   (
      EXTENDED_COUNTERS
      | TM_VOQ_COLLECTION
   ) ~NEWLINE* NEWLINE
;

switching_mode_stanza
:
   SWITCHING_MODE ~NEWLINE* NEWLINE
;

system_default
:
   DEFAULT
   (
      sd_null
      | sd_switchport
   )
;

system_null
:
   (
      ADMIN_VDC
      | AUTO_UPGRADE
      | FABRIC
      | FABRIC_MODE
      | FLOWCONTROL
      | INTERFACE
      | JUMBOMTU
      | MODE
      | MODULE_TYPE
      | MTU
      | ROUTING
      | URPF
      | VLAN
   ) ~NEWLINE* NEWLINE
;

system_qos
:
   QOS NEWLINE
   (
      system_qos_null
   )*
;

system_qos_null
:
   NO?
   (
      FEX
      | SERVICE_POLICY
   ) ~NEWLINE* NEWLINE
;

t_null
:
   (
      GROUP
      | HOST
   ) ~NEWLINE* NEWLINE
;

t_server
:
   SERVER hostname = variable_hostname NEWLINE
   (
      t_server_address
      | t_key
      | t_server_null
   )*
;

t_server_address
:
   ADDRESS
   (
      (
         IPV4 IP_ADDRESS
      )
      |
      (
         IPV6 IPV6_ADDRESS
      )
   ) NEWLINE
;

t_server_null
:
   NO?
   (
      SINGLE_CONNECTION
   ) ~NEWLINE* NEWLINE
;

t_key
:
   KEY DEC? variable_permissive NEWLINE
;

t_source_interface
:
   SOURCE_INTERFACE iname = interface_name
   (
      VRF name = variable
   )? NEWLINE
;

tap_null
:
   NO?
   (
      MODE
   ) ~NEWLINE* NEWLINE
;

tg_null
:
   NO?
   (
      ACCOUNTING_SERVER_GROUP
      | ADDRESS_POOL
      | AUTHENTICATION
      | AUTHENTICATION_SERVER_GROUP
      | DEFAULT_GROUP_POLICY
      | GROUP_URL
      | IPV6_ADDRESS_POOL
      | ISAKMP
   ) ~NEWLINE* NEWLINE
;

tr_null
:
   NO?
   (
      WEEKDAY
      | WEEKEND
   ) ~NEWLINE* NEWLINE
;

track_null
:
   NO?
   (
      DELAY
      | OBJECT
      | TYPE
   ) ~NEWLINE* NEWLINE
;

ts_common
:
   ts_null
;

ts_host
:
   HOST hostname =
   (
      IP_ADDRESS
      | IPV6_ADDRESS
   ) ~NEWLINE* NEWLINE t_key?
;

ts_null
:
   (
      DEADTIME
      | DIRECTED_REQUEST
      | KEY
      | RETRANSMIT
      | TEST
      | TIMEOUT
   ) ~NEWLINE* NEWLINE
;

vi_address_family
:
   NO? ADDRESS_FAMILY IPV4 NEWLINE
   (
      viaf_vrrp
   )*
;

u
:
   u_encrypted_password
   | u_nohangup
   | u_passphrase
   | u_password
   | u_privilege
   | u_role
;

u_encrypted_password
:
   ENCRYPTED_PASSWORD pass = variable_permissive
;

u_nohangup
:
   NOHANGUP
;

u_passphrase
:
   PASSPHRASE
   (
      GRACETIME gracetime = DEC
      | LIFETIME lifetime = DEC
      | WARNTIME warntime = DEC
   )*
;

u_password
:
   (
      PASSWORD
      | SECRET
   )
   (
      up_arista_md5
      | up_arista_sha512
      | up_cisco
   )
;

u_privilege
:
   PRIVILEGE privilege = variable
;

u_role
:
   (
      GROUP
      | ROLE
   ) role = variable
;

ua_null
:
   (
      GROUP_LOCK
      | VPN_GROUP_POLICY
   ) ~NEWLINE* NEWLINE
;

up_arista_md5
:
   DEC
   (
      pass = MD5_ARISTA
   )
;

up_arista_sha512
:
   SHA512 pass = SHA512_ARISTA
;

up_cisco
:
   DEC pass = variable_secret
;

ur_access_list
:
   ACCESS_LIST SESSION name = variable_permissive NEWLINE
;

ur_null
:
   NO?
   (
      CAPTIVE_PORTAL
      | MAX_SESSIONS
      | VLAN
   ) ~NEWLINE* NEWLINE
;

vc_null
:
   NO?
   (
      CODEC
      | DSP
      | DSPFARM
      | VOICE_SERVICE
      | WATCHDOG
   ) ~NEWLINE* NEWLINE
;

viaf_vrrp
:
   NO? VRRP groupnum = DEC NEWLINE
   (
      viafv_address
      | viafv_null
      | viafv_preempt
      | viafv_priority
   )*
;

viafv_address
:
   ADDRESS address = IP_ADDRESS NEWLINE
;

viafv_null
:
   NO?
   (
      TIMERS
      | TRACK
   ) ~NEWLINE* NEWLINE
;

viafv_preempt
:
   PREEMPT
   (
      DELAY delay = DEC
   ) NEWLINE
;

viafv_priority
:
   PRIORITY priority = DEC NEWLINE
;

vlan_null
:
   NO?
   (
      ACTION
      | BACKUPCRF
      | BRIDGE
      | MATCH
      | MEDIA
      | MTU
      | MULTICAST
      | NAME
      | PARENT
      | PRIORITY
      | PRIVATE_VLAN
      | REMOTE_SPAN
      | ROUTER_INTERFACE
      | SPANNING_TREE
      | STATE
      | STATISTICS
      | STP
      | TAGGED
      | TRUNK
      | TB_VLAN1
      | TB_VLAN2
      | UNTAGGED
   ) ~NEWLINE* NEWLINE
;

voice_class
:
   CLASS
   (
      voice_class_codec
      | voice_class_h323
      | voice_class_sip_profiles
   )
;

voice_class_codec
:
   CODEC ~NEWLINE* NEWLINE
   (
      voice_class_codec_null
   )*
;

voice_class_codec_null
:
   NO?
   (
      CODEC
   ) ~NEWLINE* NEWLINE
;

voice_class_h323
:
   H323 ~NEWLINE* NEWLINE
   (
      voice_class_h323_null
   )*
;

voice_class_h323_null
:
   NO?
   (
      CALL
      | H225
   ) ~NEWLINE* NEWLINE
;

voice_class_sip_profiles
:
   SIP_PROFILES ~NEWLINE* NEWLINE
   (
      voice_class_sip_profiles_null
   )*
;

voice_class_sip_profiles_null
:
   NO?
   (
      REQUEST
   ) ~NEWLINE* NEWLINE
;

voice_null
:
   (
      ALG_BASED_CAC
      | CALL
      | DIALPLAN_PROFILE
      | HUNT
      | IEC
      | LOGGING
      | REAL_TIME_CONFIG
      | RTCP_INACTIVITY
      | RTP
      | SIP
      | SIP_MIDCALL_REQ_TIMEOUT
   ) ~NEWLINE* NEWLINE
;

voice_service
:
   SERVICE
   (
      voice_service_voip
   )
;

voice_service_voip
:
   VOIP NEWLINE
   (
      voice_service_voip_h323
      | voice_service_voip_ip_address_trusted_list
      | voice_service_voip_null
      | voice_service_voip_sip
   )*
;

voice_service_voip_h323
:
   H323 NEWLINE
   (
      voice_service_voip_h323_null
   )*
;

voice_service_voip_h323_null
:
   NO?
   (
      CALL
      | H225
   ) ~NEWLINE* NEWLINE
;

voice_service_voip_ip_address_trusted_list
:
   IP ADDRESS TRUSTED LIST NEWLINE
   (
      voice_service_voip_ip_address_trusted_list_null
   )*
;

voice_service_voip_ip_address_trusted_list_null
:
   NO?
   (
      IPV4
   ) ~NEWLINE* NEWLINE
;

voice_service_voip_null
:
   NO?
   (
      ADDRESS_HIDING
      | ALLOW_CONNECTIONS
      | FAX
      | H225
      | MODEM
      | SHUTDOWN
      | SUPPLEMENTARY_SERVICE
   ) ~NEWLINE* NEWLINE
;

voice_service_voip_sip
:
   SIP NEWLINE
   (
      voice_service_voip_sip_null
   )*
;

voice_service_voip_sip_null
:
   NO?
   (
      BIND
      | EARLY_OFFER
      | ERROR_PASSTHRU
      | G729
      | HEADER_PASSING
      | LISTEN_PORT
      | MIDCALL_SIGNALING
      | SIP_PROFILES
      | TRANSPORT
   ) ~NEWLINE* NEWLINE
;

voice_translation_profile
:
   TRANSLATION_PROFILE ~NEWLINE* NEWLINE
   (
      voice_translation_profile_null
   )*
;

voice_translation_profile_null
:
   NO?
   (
      TRANSLATE
   ) ~NEWLINE* NEWLINE
;

voice_translation_rule
:
   TRANSLATION_RULE ~NEWLINE* NEWLINE
   (
      voice_translation_rule_null
   )*
;

voice_translation_rule_null
:
   NO?
   (
      RULE
   ) ~NEWLINE* NEWLINE
;

vp_null
:
   NO?
   (
      CALLER_ID
      | CONNECTION
      | CPTONE
      | DESCRIPTION
      | ECHO_CANCEL
      | SHUTDOWN
      | SIGNAL
      | TIMEOUTS
      | TIMING
   ) ~NEWLINE* NEWLINE
;

vpc_null
:
   NO?
   (
      DELAY
      | IP
      | PEER_GATEWAY
      | PEER_KEEPALIVE
      | PEER_SWITCH
      | ROLE
      | SYSTEM_PRIORITY
   ) ~NEWLINE* NEWLINE
;

vpdng_accept_dialin
:
   NO? ACCEPT_DIALIN ~NEWLINE* NEWLINE
   (
      vpdnga_null
   )*
;

vpdng_null
:
   NO?
   (
      L2TP
   ) ~NEWLINE* NEWLINE
;

vpdnga_null
:
   NO?
   (
      PROTOCOL
      | VIRTUAL_TEMPLATE
   ) ~NEWLINE* NEWLINE
;

vpn_dialer_null
:
   NO?
   (
      IKE
   ) ~NEWLINE* NEWLINE
;

vpn_null
:
   NO?
   (
      CLUSTER
      | PARTICIPATE
      | PRIORITY
      | REDIRECT_FQDN
   ) ~NEWLINE* NEWLINE
;

vrfc_ip_route
:
   IP ROUTE ip_route_tail
;

vrfc_null
:
   NO?
   (
      (
         IP
         (
            PIM
         )
      )
      | MDT
   ) ~NEWLINE* NEWLINE
;

vrfd_address_family
:
   ADDRESS_FAMILY
   (
      IPV4
      | IPV6
   )
   (
      MULTICAST
      | UNICAST
   )?
   (
      MAX_ROUTE DEC
   )? NEWLINE
   (
      vrfd_af_null
   )*
   (
      EXIT_ADDRESS_FAMILY NEWLINE
   )?
;

vrfd_null
:
   NO?
   (
      AUTO_IMPORT
      | RD
      | ROUTE_TARGET
      |
      (
         NO SHUTDOWN
      )
   ) ~NEWLINE* NEWLINE
;

vrrp_interface
:
   NO? INTERFACE iface = interface_name NEWLINE
   (
      vi_address_family
   )* NEWLINE?
;

wccp_id
:
   id = DEC
   (
      (
         GROUP_LIST group_list = variable
      )
      |
      (
         MODE
         (
            CLOSED
            | OPEN
         )
      )
      |
      (
         PASSWORD DEC? password = variable
      )
      |
      (
         REDIRECT_LIST redirect_list = variable
      )
      |
      (
         SERVICE_LIST service_list = variable
      )
   )* NEWLINE
;

wccp_null
:
   (
      CHECK
      | OUTBOUND_ACL_CHECK
      | SOURCE_INTERFACE
      | VERSION
      | WEB_CACHE
   ) ~NEWLINE* NEWLINE
;

web_server_null
:
   NO?
   (
      CAPTIVE_PORTAL_CERT
      | IDP_CERT
      | SESSION_TIMEOUT
      | SWITCH_CERT
      | WEB_HTTPS_PORT_443
      | WEB_MAX_CLIENTS
   ) ~NEWLINE* NEWLINE
;

webvpn_null
:
   NO?
   (
      ANYCONNECT
      | ANYCONNECT_ESSENTIALS
      | CACHE
      | CSD
      | DISABLE
      | ENABLE
      | ERROR_RECOVERY
      | KEEPOUT
      | TUNNEL_GROUP_LIST
   ) ~NEWLINE* NEWLINE
;

wlan_null
:
   NO?
   (
      BCN_RPT_REQ_PROFILE
      | DOT11K_PROFILE
      | DOT11R_PROFILE
      | EDCA_PARAMETERS_PROFILE
      | HANDOVER_TRIGGER_PROFILE
      | HOTSPOT
      | HT_SSID_PROFILE
      | RRM_IE_PROFILE
      | TSM_REQ_PROFILE
      | VOIP_CAC_PROFILE
   ) ~NEWLINE* NEWLINE
;

wlan_ssid_profile
:
   SSID_PROFILE double_quoted_string NEWLINE
   (
      wlan_ssid_profile_null
   )*
;

wlan_ssid_profile_null
:
   NO?
   (
      EAPOL_RATE_OPT
      | ESSID
      | HT_SSID_PROFILE
      | MAX_CLIENTS
      | MCAST_RATE_OPT
      | OPMODE
      | SSID_ENABLE
      | WMM
   ) ~NEWLINE* NEWLINE
;

wlan_virtual_ap
:
   VIRTUAL_AP double_quoted_string NEWLINE
   (
      wlan_virtual_ap_null
   )*
;

wlan_virtual_ap_null
:
   NO?
   (
      AAA_PROFILE
      | AUTH_FAILURE_BLACKLIST_TIME
      | BAND_STEERING
      | BLACKLIST
      | BLACKLIST_TIME
      | BROADCAST_FILTER
      | DENY_INTER_USER_TRAFFIC
      | DYNAMIC_MCAST_OPTIMIZATION
      | DYNAMIC_MCAST_OPTIMIZATION_THRESH
      | SSID_PROFILE
      | VAP_ENABLE
      | VLAN
   ) ~NEWLINE* NEWLINE
;

wsma_null
:
   NO?
   (
      PROFILE
      | TRANSPORT
   ) ~NEWLINE* NEWLINE
;
