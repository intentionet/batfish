parser grammar FlatJuniper_common;

options {
   tokenVocab = FlatJuniperLexer;
}

administrator_as
:
   DEC L
;

administrator_dec
:
   DEC
;

administrator_dotted_as
:
   DEC PERIOD DEC
;

administrator_ip
:
   DEC PERIOD DEC PERIOD DEC PERIOD DEC
;

apply
:
// intentional blank

   | apply_groups
   | apply_groups_except
;

apply_groups
:
   APPLY_GROUPS name = variable
;

apply_groups_except
:
   APPLY_GROUPS_EXCEPT name = variable
;

as_path_expr
:
   (
      items += as_unit
   )+
;

as_set
:
   OPEN_BRACKET
   (
      items += DEC
   )+ CLOSE_BRACKET
;

as_unit
:
   as_set
   | DEC
;

description
:
   DESCRIPTION text = M_Description_DESCRIPTION?
;

ec_administrator
:
   administrator_as
   | administrator_dec
   | administrator_dotted_as
   | administrator_ip
;

ec_literal
:
   DEC COLON DEC COLON DEC
;

ec_named
:
   ec_type COLON ec_administrator COLON assigned_number = DEC
;

ec_type
:
   ORIGIN
   | TARGET
;

extended_community
:
   ec_literal
   | ec_named
;

icmp_code
:
   DESTINATION_HOST_UNKNOWN
   | DESTINATION_NETWORK_UNKNOWN
   | FRAGMENTATION_NEEDED
   | HOST_UNREACHABLE
   | NETWORK_UNREACHABLE
   | PORT_UNREACHABLE
;

icmp_type
:
   DESTINATION_UNREACHABLE
   | ECHO_REPLY
   | ECHO_REQUEST
   | NEIGHBOR_ADVERTISEMENT
   | NEIGHBOR_SOLICIT
   | PACKET_TOO_BIG
   | PARAMETER_PROBLEM
   | REDIRECT
   | ROUTER_ADVERTISEMENT
   | ROUTER_SOLICIT
   | SOURCE_QUENCH
   | TIME_EXCEEDED
   | UNREACHABLE
;

interface_id
:
   (
      node = variable COLON
   )?
   (
      name = VARIABLE
      (
         COLON suffix = DEC
      )?
      (
         PERIOD unit = DEC
      )?
   )
;

ip_option
:
   SECURITY
;

ip_protocol
:
   AH
   | DEC
   | DSTOPTS
   | EGP
   | ESP
   | FRAGMENT
   | GRE
   | HOP_BY_HOP
   | ICMP
   | ICMP6
   | ICMPV6
   | IGMP
   | IPIP
   | IPV6
   | OSPF
   | PIM
   | RSVP
   | SCTP
   | TCP
   | UDP
   | VRRP
;

junos_application
:
   ANY
   | JUNOS_AOL
   | JUNOS_BGP
   | JUNOS_BIFF
   | JUNOS_BOOTPC
   | JUNOS_BOOTPS
   | JUNOS_CHARGEN
   | JUNOS_CVSPSERVER
   | JUNOS_DHCP_CLIENT
   | JUNOS_DHCP_RELAY
   | JUNOS_DHCP_SERVER
   | JUNOS_DISCARD
   | JUNOS_DNS_TCP
   | JUNOS_DNS_UDP
   | JUNOS_ECHO
   | JUNOS_FINGER
   | JUNOS_FTP
   | JUNOS_FTP_DATA
   | JUNOS_GNUTELLA
   | JUNOS_GOPHER
   | JUNOS_GPRS_GTP_C
   | JUNOS_GPRS_GTP_U
   | JUNOS_GPRS_GTP_V0
   | JUNOS_GPRS_SCTP
   | JUNOS_GRE
   | JUNOS_GTP
   | JUNOS_H323
   | JUNOS_HTTP
   | JUNOS_HTTP_EXT
   | JUNOS_HTTPS
   | JUNOS_ICMP_ALL
   | JUNOS_ICMP_PING
   | JUNOS_ICMP6_ALL
   | JUNOS_ICMP6_DST_UNREACH_ADDR
   | JUNOS_ICMP6_DST_UNREACH_ADMIN
   | JUNOS_ICMP6_DST_UNREACH_BEYOND
   | JUNOS_ICMP6_DST_UNREACH_PORT
   | JUNOS_ICMP6_DST_UNREACH_ROUTE
   | JUNOS_ICMP6_ECHO_REPLY
   | JUNOS_ICMP6_ECHO_REQUEST
   | JUNOS_ICMP6_PACKET_TOO_BIG
   | JUNOS_ICMP6_PARAM_PROB_HEADER
   | JUNOS_ICMP6_PARAM_PROB_NEXTHDR
   | JUNOS_ICMP6_PARAM_PROB_OPTION
   | JUNOS_ICMP6_TIME_EXCEED_REASSEMBLY
   | JUNOS_ICMP6_TIME_EXCEED_TRANSIT
   | JUNOS_IDENT
   | JUNOS_IKE
   | JUNOS_IKE_NAT
   | JUNOS_IMAP
   | JUNOS_IMAPS
   | JUNOS_INTERNET_LOCATOR_SERVICE
   | JUNOS_IRC
   | JUNOS_L2TP
   | JUNOS_LDAP
   | JUNOS_LDP_TCP
   | JUNOS_LDP_UDP
   | JUNOS_LPR
   | JUNOS_MAIL
   | JUNOS_MGCP
   | JUNOS_MGCP_CA
   | JUNOS_MGCP_UA
   | JUNOS_MS_RPC
   | JUNOS_MS_RPC_ANY
   | JUNOS_MS_RPC_EPM
   | JUNOS_MS_RPC_IIS_COM
   | JUNOS_MS_RPC_IIS_COM_1
   | JUNOS_MS_RPC_IIS_COM_ADMINBASE
   | JUNOS_MS_RPC_MSEXCHANGE
   | JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_NSP
   | JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_RFR
   | JUNOS_MS_RPC_MSEXCHANGE_INFO_STORE
   | JUNOS_MS_RPC_TCP
   | JUNOS_MS_RPC_UDP
   | JUNOS_MS_RPC_UUID_ANY_TCP
   | JUNOS_MS_RPC_UUID_ANY_UDP
   | JUNOS_MS_RPC_WMIC
   | JUNOS_MS_RPC_WMIC_ADMIN
   | JUNOS_MS_RPC_WMIC_ADMIN2
   | JUNOS_MS_RPC_WMIC_MGMT
   | JUNOS_MS_RPC_WMIC_WEBM_CALLRESULT
   | JUNOS_MS_RPC_WMIC_WEBM_CLASSOBJECT
   | JUNOS_MS_RPC_WMIC_WEBM_LEVEL1LOGIN
   | JUNOS_MS_RPC_WMIC_WEBM_LOGIN_CLIENTID
   | JUNOS_MS_RPC_WMIC_WEBM_LOGIN_HELPER
   | JUNOS_MS_RPC_WMIC_WEBM_OBJECTSINK
   | JUNOS_MS_RPC_WMIC_WEBM_REFRESHING_SERVICES
   | JUNOS_MS_RPC_WMIC_WEBM_REMOTE_REFRESHER
   | JUNOS_MS_RPC_WMIC_WEBM_SERVICES
   | JUNOS_MS_RPC_WMIC_WEBM_SHUTDOWN
   | JUNOS_MS_SQL
   | JUNOS_MSN
   | JUNOS_NBDS
   | JUNOS_NBNAME
   | JUNOS_NETBIOS_SESSION
   | JUNOS_NFS
   | JUNOS_NFSD_TCP
   | JUNOS_NFSD_UDP
   | JUNOS_NNTP
   | JUNOS_NS_GLOBAL
   | JUNOS_NS_GLOBAL_PRO
   | JUNOS_NSM
   | JUNOS_NTALK
   | JUNOS_NTP
   | JUNOS_OSPF
   | JUNOS_PC_ANYWHERE
   | JUNOS_PERSISTENT_NAT
   | JUNOS_PING
   | JUNOS_PINGV6
   | JUNOS_POP3
   | JUNOS_PPTP
   | JUNOS_PRINTER
   | JUNOS_R2CP
   | JUNOS_RADACCT
   | JUNOS_RADIUS
   | JUNOS_REALAUDIO
   | JUNOS_RIP
   | JUNOS_ROUTING_INBOUND
   | JUNOS_RSH
   | JUNOS_RTSP
   | JUNOS_SCCP
   | JUNOS_SCTP_ANY
   | JUNOS_SIP
   | JUNOS_SMB
   | JUNOS_SMB_SESSION
   | JUNOS_SMTP
   | JUNOS_SMTPS
   | JUNOS_SNMP_AGENTX
   | JUNOS_SNPP
   | JUNOS_SQL_MONITOR
   | JUNOS_SQLNET_V1
   | JUNOS_SQLNET_V2
   | JUNOS_SSH
   | JUNOS_STUN
   | JUNOS_SUN_RPC
   | JUNOS_SUN_RPC_ANY
   | JUNOS_SUN_RPC_ANY_TCP
   | JUNOS_SUN_RPC_ANY_UDP
   | JUNOS_SUN_RPC_MOUNTD
   | JUNOS_SUN_RPC_MOUNTD_TCP
   | JUNOS_SUN_RPC_MOUNTD_UDP
   | JUNOS_SUN_RPC_NFS
   | JUNOS_SUN_RPC_NFS_ACCESS
   | JUNOS_SUN_RPC_NFS_TCP
   | JUNOS_SUN_RPC_NFS_UDP
   | JUNOS_SUN_RPC_NLOCKMGR
   | JUNOS_SUN_RPC_NLOCKMGR_TCP
   | JUNOS_SUN_RPC_NLOCKMGR_UDP
   | JUNOS_SUN_RPC_PORTMAP
   | JUNOS_SUN_RPC_PORTMAP_TCP
   | JUNOS_SUN_RPC_PORTMAP_UDP
   | JUNOS_SUN_RPC_RQUOTAD
   | JUNOS_SUN_RPC_RQUOTAD_TCP
   | JUNOS_SUN_RPC_RQUOTAD_UDP
   | JUNOS_SUN_RPC_RUSERD
   | JUNOS_SUN_RPC_RUSERD_TCP
   | JUNOS_SUN_RPC_RUSERD_UDP
   | JUNOS_SUN_RPC_SADMIND
   | JUNOS_SUN_RPC_SADMIND_TCP
   | JUNOS_SUN_RPC_SADMIND_UDP
   | JUNOS_SUN_RPC_SPRAYD
   | JUNOS_SUN_RPC_SPRAYD_TCP
   | JUNOS_SUN_RPC_SPRAYD_UDP
   | JUNOS_SUN_RPC_STATUS
   | JUNOS_SUN_RPC_STATUS_TCP
   | JUNOS_SUN_RPC_STATUS_UDP
   | JUNOS_SUN_RPC_TCP
   | JUNOS_SUN_RPC_UDP
   | JUNOS_SUN_RPC_WALLD
   | JUNOS_SUN_RPC_WALLD_TCP
   | JUNOS_SUN_RPC_WALLD_UDP
   | JUNOS_SUN_RPC_YPBIND
   | JUNOS_SUN_RPC_YPBIND_TCP
   | JUNOS_SUN_RPC_YPBIND_UDP
   | JUNOS_SUN_RPC_YPSERV
   | JUNOS_SUN_RPC_YPSERV_TCP
   | JUNOS_SUN_RPC_YPSERV_UDP
   | JUNOS_SYSLOG
   | JUNOS_TACACS
   | JUNOS_TACACS_DS
   | JUNOS_TALK
   | JUNOS_TCP_ANY
   | JUNOS_TELNET
   | JUNOS_TFTP
   | JUNOS_UDP_ANY
   | JUNOS_UUCP
   | JUNOS_VDO_LIVE
   | JUNOS_VNC
   | JUNOS_WAIS
   | JUNOS_WHO
   | JUNOS_WHOIS
   | JUNOS_WINFRAME
   | JUNOS_WXCONTROL
   | JUNOS_X_WINDOWS
   | JUNOS_XNM_CLEAR_TEXT
   | JUNOS_XNM_SSL
   | JUNOS_YMSG
;

junos_application_set
:
   JUNOS_CIFS
;

null_filler
:
   ~( APPLY_GROUPS | NEWLINE )* apply_groups?
;

origin_type
:
   EGP
   | IGP
   | INCOMPLETE
;

pe_conjunction
:
   OPEN_PAREN policy_expression
   (
      DOUBLE_AMPERSAND policy_expression
   )+ CLOSE_PAREN
;

pe_disjunction
:
   OPEN_PAREN policy_expression
   (
      DOUBLE_PIPE policy_expression
   )+ CLOSE_PAREN
;

pe_nested
:
   OPEN_PAREN policy_expression CLOSE_PAREN
;

policy_expression
:
   pe_conjunction
   | pe_disjunction
   | pe_nested
   | variable
;

port
:
   AFS
   | BGP
   | BIFF
   | BOOTPC
   | BOOTPS
   | CMD
   | CVSPSERVER
   | DEC
   | DHCP
   | DOMAIN
   | EKLOGIN
   | EKSHELL
   | EXEC
   | FINGER
   | FTP
   | FTP_DATA
   | HTTP
   | HTTPS
   | IDENT
   | IMAP
   | KERBEROS_SEC
   | KLOGIN
   | KPASSWD
   | KRB_PROP
   | KRBUPDATE
   | KSHELL
   | LDAP
   | LDP
   | LOGIN
   | MOBILEIP_AGENT
   | MOBILIP_MN
   | MSDP
   | NETBIOS_DGM
   | NETBIOS_NS
   | NETBIOS_SSN
   | NFSD
   | NNTP
   | NTALK
   | NTP
   | POP3
   | PPTP
   | PRINTER
   | RADACCT
   | RADIUS
   | RIP
   | RKINIT
   | SMTP
   | SNMP
   | SNMPTRAP
   | SNPP
   | SOCKS
   | SSH
   | SUNRPC
   | SYSLOG
   | TACACS
   | TACACS_DS
   | TALK
   | TELNET
   | TFTP
   | TIMED
   | WHO
   | XDMCP
;

range
:
   range_list += subrange
   (
      COMMA range_list += subrange
   )*
;

routing_protocol
:
   AGGREGATE
   | BGP
   | DIRECT
   | ISIS
   | LDP
   | LOCAL
   | OSPF
   | OSPF3
   | RSVP
   | STATIC
;

sc_literal
:
   COMMUNITY_LITERAL
;

sc_named
:
   NO_ADVERTISE
   | NO_EXPORT
;

standard_community
:
   sc_literal
   | sc_named
;

string
:
   DOUBLE_QUOTED_STRING
   | variable
;

subrange
:
   low = DEC
   (
      DASH high = DEC
   )?
;

variable
:
   text = ~( APPLY_GROUPS | APPLY_GROUPS_EXCEPT | APPLY_PATH | NEWLINE |
   OPEN_PAREN | OPEN_BRACKET | OPEN_BRACE | WILDCARD )
;

variable_permissive
:
   ~NEWLINE+
;

wildcard_address
:
   ip_address = IP_ADDRESS FORWARD_SLASH wildcard_mask = IP_ADDRESS
;
