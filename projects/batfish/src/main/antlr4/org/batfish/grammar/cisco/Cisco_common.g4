parser grammar Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

access_list_action
:
   PERMIT
   | DENY
;

address_family_footer
:
   (
      (
         EXIT_ADDRESS_FAMILY
         | EXIT
      ) NEWLINE
   )?
;

banner
:
   (
      (
         (
            ESCAPE_C ~ESCAPE_C* ESCAPE_C
         )
         |
         (
            POUND ~POUND* POUND
         )
         |
         (
            NEWLINE ~( EOF_LITERAL | LINE_CADANT )* EOF_LITERAL
         )
         |
         (
            NEWLINE LINE_CADANT* END_CADANT
         )
         |
         (
            ASA_BANNER_LINE
         )
      )
   ) NEWLINE?
;

banner_type
:
   CONFIG_SAVE
   | EXEC
   | INCOMING
   | LOGIN
   | MOTD
   | PROMPT_TIMEOUT
   | SLIP_PPP
;

community
:
   com = COMMUNITY_NUMBER
   | com = DEC
   | com = GSHUT
   | com = INTERNET
   | com = LOCAL_AS
   | com = NO_ADVERTISE
   | com = NO_EXPORT
;

description_line
:
   DESCRIPTION text = RAW_TEXT? NEWLINE
;

double_quoted_string
:
   DOUBLE_QUOTE
   (
      inner_text += ~DOUBLE_QUOTE
   )* DOUBLE_QUOTE
;

dscp_type
:
   DEC
   | AF11
   | AF12
   | AF13
   | AF21
   | AF22
   | AF23
   | AF31
   | AF32
   | AF33
   | AF41
   | AF42
   | AF43
   | CS1
   | CS2
   | CS3
   | CS4
   | CS5
   | CS6
   | CS7
   | DEFAULT
   | EF
;

ec_literal
:
   DEC COLON DEC
;

exit_line
:
   EXIT NEWLINE
;

extended_community
:
   ec_literal
;

hash_comment
:
   POUND RAW_TEXT
;

icmp_object_type
:
   DEC
   | ALTERNATE_ADDRESS
   | CONVERSION_ERROR
   | ECHO
   | ECHO_REPLY
   | INFORMATION_REPLY
   | INFORMATION_REQUEST
   | MASK_REPLY
   | MASK_REQUEST
   | MOBILE_REDIRECT
   | PARAMETER_PROBLEM
   | REDIRECT
   | ROUTER_ADVERTISEMENT
   | ROUTER_SOLICITATION
   | SOURCE_QUENCH
   | TIME_EXCEEDED
   | TIMESTAMP_REPLY
   | TIMESTAMP_REQUEST
   | TRACEROUTE
   | UNREACHABLE
;

int_expr
:
   (
      (
         PLUS
         | DASH
      )? DEC
   )
   | IGP_COST
   | RP_VARIABLE
;

interface_name
:
   (
      name_prefix_alpha = M_Interface_PREFIX
      (
         (
            (
               name_middle_parts += M_Interface_PREFIX
            )? name_middle_parts += DEC
            (
               name_middle_parts += FORWARD_SLASH
               | name_middle_parts += PERIOD
               | name_middle_parts += COLON
            )
         )*
         | name_middle_parts += MODULE
      ) range?
   )
   |
   (
      (
         VARIABLE
         | variable_interface_name DEC?
      )
      (
         (
            COLON
            | FORWARD_SLASH
            | PERIOD
         ) DEC
      )*
   )
;

ip_hostname
:
   IP_ADDRESS
   | IPV6_ADDRESS
;

isis_level
:
   LEVEL_1
   | LEVEL_1_2
   | LEVEL_2
;

line_type
:
// intentional blank

   | AUX
   | CON
   | CONSOLE
   | DEFAULT
   |
   (
      TEMPLATE name = variable
   )
   | TTY
   | VTY
;

line_type_cadant
:
   CONSOLE
   | VTY
;

netservice_alg
:
   DHCP
   | DNS
   | FTP
   | NOE
   | RTSP
   | SCCP
   | SIPS
   | SVP
   | TFTP
   | VOCERA
;

null_rest_of_line
:
    ~NEWLINE* NEWLINE
;

ospf_route_type
:
   (
      EXTERNAL DEC?
   )
   | INTERNAL
   |
   (
      NSSA_EXTERNAL DEC?
   )
;

port_specifier
:
   (
      EQ
      (
         args += port
      )+
   )
   |
   (
      GT arg = port
   )
   |
   (
      NEQ arg = port
   )
   |
   (
      LT arg = port
   )
   |
   (
      RANGE arg1 = port arg2 = port
   )
;

port
:
   DEC
   | AOL
   | BGP
   | BIFF
   | BOOTPC
   | BOOTPS
   | CHARGEN
   | CITRIX_ICA
   | CMD
   | CTIQBE
   | DAYTIME
   | DISCARD
   | DNSIX
   | DOMAIN
   | ECHO
   | EXEC
   | FINGER
   | FTP
   | FTP_DATA
   | GOPHER
   | H323
   | HTTPS
   | HOSTNAME
   | IDENT
   | IMAP4
   | IRC
   | ISAKMP
   | KERBEROS
   | KLOGIN
   | KSHELL
   | LDAP
   | LDAPS
   | LDP
   | LPD
   | LOGIN
   | LOTUSNOTES
   | MICROSOFT_DS
   | MLAG
   | MOBILE_IP
   | MSRPC
   | NAMESERVER
   | NETBIOS_DGM
   | NETBIOS_NS
   | NETBIOS_SS
   | NETBIOS_SSN
   | NNTP
   | NON500_ISAKMP
   | NTP
   | PCANYWHERE_DATA
   | PCANYWHERE_STATUS
   | PIM_AUTO_RP
   | POP2
   | POP3
   | PPTP
   | RADIUS
   | RADIUS_ACCT
   | RIP
   | SECUREID_UDP
   | SMTP
   | SNMP
   | SNMP_TRAP
   | SNMPTRAP
   | SQLNET
   | SSH
   | SUNRPC
   | SYSLOG
   | TACACS
   | TACACS_DS
   | TALK
   | TELNET
   | TFTP
   | TIME
   | UUCP
   | WHO
   | WHOIS
   | WWW
   | XDMCP
;

prefix_set_elem
:
   (
      ipa = IP_ADDRESS
      | prefix = IP_PREFIX
      | ipv6a = IPV6_ADDRESS
      | ipv6_prefix = IPV6_PREFIX
   )
   (
      (
         GE minpl = DEC
      )
      |
      (
         LE maxpl = DEC
      )
      |
      (
         EQ eqpl = DEC
      )
   )*
;

protocol
:
   AH
   | AHP
   | DEC
   | EIGRP
   | ESP
   | GRE
   | ICMP
   | ICMP6
   | ICMPV6
   | IGMP
   | IGRP
   | IP
   | IPINIP
   | IPSEC
   | IPV4
   | IPV6
   | ND
   | NOS
   | OSPF
   | PIM
   | PPTP
   | SCTP
   | SNP
   | TCP
   | TCP_UDP
   | UDP
   | VRRP
;

range
:
   (
      range_list += subrange
      (
         COMMA range_list += subrange
      )*
   )
   | NONE
;

route_policy_params_list
:
   params_list += variable
   (
      COMMA params_list += variable
   )*
;

rp_community_set_elem
:
   (
      prefix = rp_community_set_elem_half COLON suffix =
      rp_community_set_elem_half
   )
   | community
;

rp_community_set_elem_half
:
   value = DEC
   | var = RP_VARIABLE
   |
   (
      BRACKET_LEFT first = DEC PERIOD PERIOD last = DEC BRACKET_RIGHT
   )
   | ASTERISK
;

rp_subrange
:
   first = int_expr
   |
   (
      BRACKET_LEFT first = int_expr PERIOD PERIOD last = int_expr BRACKET_RIGHT
   )
;

subrange
:
   low = DEC
   (
      DASH high = DEC
   )?
;

switchport_trunk_encapsulation
:
   DOT1Q
   | ISL
   | NEGOTIATE
;

variable
:
   ~NEWLINE
;

variable_aclname
:
   (
      ~( ETH | EXTENDED | NEWLINE | REMARK | STANDARD | SESSION | WS )
   )+
;

variable_community_name
:
   ~( NEWLINE | DOUBLE_QUOTE | GROUP | IPV4 | IPV6 | RO | RW | SDROWNER |
   SYSTEMOWNER | USE_ACL | USE_IPV4_ACL | USE_IPV6_ACL | VIEW )
;

variable_hostname
:
   ~( USE_VRF | NEWLINE | VRF )+
;

variable_interface_name
:
   ~( DEC | IP_ADDRESS | IP_PREFIX | ADMIN_DIST | ADMIN_DISTANCE | METRIC |
   NAME | NEWLINE | TAG | TRACK | VARIABLE )
;

variable_max_metric
:
   ~(NEWLINE | BGP | EXTERNAL_LSA | INCLUDE_STUB | ON_STARTUP | ROUTER_LSA | SUMMARY_LSA | WAIT_FOR)
;

variable_permissive
:
   (
      ~( EXTENDED | NEWLINE | STANDARD | WS )
   )+
;

variable_secret
:
   ~( NEWLINE | ROLE )+
;

variable_vlan
:
   ~( NEWLINE | ACCESS_MAP | DEC )
;
