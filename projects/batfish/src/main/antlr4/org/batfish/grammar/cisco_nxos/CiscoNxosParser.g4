parser grammar CiscoNxosParser;

import
  CiscoNxos_common,
  CiscoNxos_aaa,
  CiscoNxos_bgp,
  CiscoNxos_class_map,
  CiscoNxos_eigrp,
  CiscoNxos_evpn,
  CiscoNxos_interface,
  CiscoNxos_ip_access_list,
  CiscoNxos_ip_as_path_access_list,
  CiscoNxos_ip_community_list,
  CiscoNxos_ip_prefix_list,
  CiscoNxos_ipv6_access_list,
  CiscoNxos_ipv6_prefix_list,
  CiscoNxos_logging,
  CiscoNxos_ntp,
  CiscoNxos_object_group,
  CiscoNxos_ospf,
  CiscoNxos_policy_map,
  CiscoNxos_route_map,
  CiscoNxos_snmp,
  CiscoNxos_static,
  CiscoNxos_tacacs_server,
  CiscoNxos_vlan,
  CiscoNxos_vrf;

options {
  superClass = 'org.batfish.grammar.cisco_nxos.parsing.CiscoNxosBaseParser';
  tokenVocab = CiscoNxosLexer;
}

cisco_nxos_configuration
:
  statement+ EOF
;

statement
:
  s_aaa
  | s_banner
  | s_class_map
  | s_control_plane
  | s_evpn
  | s_hostname
  | s_interface
  | s_ip
  | s_ipv6
  | s_key
  | s_logging
  | s_no
  | s_ntp
  | s_null
  | s_nv
  | s_object_group
  | s_policy_map
  | s_role
  | s_route_map
  | s_router
  | s_snmp_server
  | s_system
  | s_tacacs_server
  | s_track
  | s_version
  | s_vlan
  | s_vrf_context
;

s_banner
:
  BANNER
  (
    banner_exec
    | banner_motd
  )
;

banner_exec
:
  EXEC BANNER_DELIMITER body=BANNER_BODY? BANNER_DELIMITER NEWLINE
;

banner_motd
:
  MOTD BANNER_DELIMITER body=BANNER_BODY? BANNER_DELIMITER NEWLINE
;

s_control_plane
:
  CONTROL_PLANE NEWLINE cp_service_policy*
;

cp_service_policy
:
  SERVICE_POLICY INPUT name = policy_map_name NEWLINE
;

s_hostname
:
  (HOSTNAME | SWITCHNAME) hostname = subdomain_name NEWLINE
;

s_ip
:
  IP
  (
    ip_access_list
    | ip_as_path_access_list
    | ip_community_list
    | ip_domain_name
    | ip_name_server
    | ip_null
    | ip_prefix_list
    | ip_route
    | ip_sla
  )
;

ip_domain_name
:
  DOMAIN_NAME domain = domain_name NEWLINE
;

domain_name
:
// 1-64 characters
  WORD
;

ip_name_server
:
  NAME_SERVER servers += name_server+ (USE_VRF vrf = vrf_name)? NEWLINE
;

name_server
:
  ip_address
  | ipv6_address
;

ip_null
:
  DOMAIN_LOOKUP
;

ip_sla
:
  SLA
  (
    entry = uint32 NEWLINE ip_sla_entry+
    |
    (
      GROUP
      | LOGGING
      | REACTION_CONFIGURATION
      | REACTION_TRIGGER
      | RESET
      | RESPONDER
      | SCHEDULE
    ) null_rest_of_line
  )
;

ip_sla_entry
:
  ( DNS | HTTP | ICMP_ECHO | TCP_CONNECT | UDP_ECHO | UDP_JITTER ) null_rest_of_line
;

s_ipv6
:
  IPV6
  (
    ipv6_access_list
    | ipv6_prefix_list
  )
;

s_key
:
  KEY key_chain
;

key_chain
:
  CHAIN name = key_chain_name NEWLINE kc_key*
;

kc_key
:
  KEY num = uint16 NEWLINE kck_key_string*
;

kck_key_string
:
  KEY_STRING key_text = key_string_text NEWLINE
;

key_string_text
:
// 1-63 characters
  REMARK_TEXT
;

s_null
:
  (
    BOOT
    | CLI
    | CLOCK
    | ERRDISABLE
    | FEATURE
    | SERVICE
    | SPANNING_TREE
    | USERNAME
  ) null_rest_of_line
;

s_no
:
  NO
  (
    IP
  ) null_rest_of_line
;

s_nv
:
  NV OVERLAY EVPN NEWLINE
;

s_role
:
  ROLE NAME name = role_name NEWLINE role_null*
;

role_name
:
// 1-16 characters
  WORD
;

role_null
:
  (
    DESCRIPTION
    | RULE
  ) null_rest_of_line
;

s_router
:
  ROUTER
  (
    router_bgp
    | router_eigrp
    | router_ospf
  )
;

s_system
:
  SYSTEM
  (
    sys_default
    | sys_qos
  )
;

sys_default
:
  DEFAULT sysd_switchport
;

sysd_switchport
:
  SWITCHPORT
  (
    sysds_shutdown
    | sysds_switchport
  )
;

sysds_shutdown
:
  SHUTDOWN NEWLINE
;

sysds_switchport
:
  NEWLINE
;

sys_qos
:
  QOS NEWLINE sysqos_service_policy*
;

sysqos_service_policy
:
  SERVICE_POLICY TYPE 
  (
    sysqosspt_network_qos
    | sysqosspt_qos
    | sysqosspt_queueing
  )
;

sysqosspt_network_qos
:
  NETWORK_QOS name = policy_map_name NEWLINE
;

sysqosspt_qos
:
  QOS INPUT name = policy_map_name NEWLINE
;

sysqosspt_queueing
:
  QUEUING (INPUT | OUTPUT) name = policy_map_name NEWLINE
;

s_track
:
  TRACK num = track_object_number
  (
    track_interface
    | track_ip
  )
;

track_interface
:
  INTERFACE null_rest_of_line
;

track_ip
:
  IP
  (
    track_ip_route
    | track_ip_sla
  )
;

track_ip_route
:
  ROUTE null_rest_of_line tir_vrf*
;

tir_vrf
:
  VRF MEMBER name = vrf_name NEWLINE
;

track_ip_sla
:
  SLA null_rest_of_line
;

s_version
:
// arbitray string, not actual command
  VERSION version = REMARK_TEXT NEWLINE
;
