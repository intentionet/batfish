parser grammar Cisco_acl;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

access_list_ip_range
:
   (
      ip = IP_ADDRESS wildcard = IP_ADDRESS
   )
   | ANY
   | ANY4
   |
   (
      HOST? ip = IP_ADDRESS
   )
   | prefix = IP_PREFIX
   |
   (
      ADDRGROUP address_group = variable
   )
   |
   (
      INTERFACE iface = variable
   )
   |
   (
      OBJECT obj = variable
   )
   |
   (
      OBJECT_GROUP og = variable
   )
;

access_list_ip6_range
:
   (
      ip = IPV6_ADDRESS wildcard = IPV6_ADDRESS
   )
   | ANY
   | ANY6
   |
   (
      HOST? ipv6 = IPV6_ADDRESS
   )
   | ipv6_prefix = IPV6_PREFIX
   |
   (
      ADDRGROUP address_group = variable
   )
;

access_list_mac_range
:
   ANY
   |
   (
      address = MAC_ADDRESS_LITERAL wildcard = MAC_ADDRESS_LITERAL
   )
   |
   (
      HOST address = MAC_ADDRESS_LITERAL
   )
;

appletalk_access_list_null_tail
:
   action = access_list_action
   (
      (
         CABLE_RANGE ~NEWLINE*
      )
      | OTHER_ACCESS
   )? NEWLINE
;

appletalk_access_list_stanza
:
   ACCESS_LIST name = ACL_NUM_APPLETALK appletalk_access_list_null_tail
;

aruba_access_list_action
:
   action = access_list_action
   | CAPTIVE
   |
   (
      DST_NAT dstnat = DEC
   )
   | SRC_NAT
;

aruba_app
:
   BITTORRENT
   | BITTORRENT_APPLICATION
;

aruba_appcategory
:
   PEER_TO_PEER
;

as_path_set_elem
:
   IOS_REGEX AS_PATH_SET_REGEX
;

as_path_set_stanza
:
   AS_PATH_SET name = variable NEWLINE
   (
      elems += as_path_set_elem NEWLINE?
      (
         COMMA NEWLINE? elems += as_path_set_elem NEWLINE?
      )*
   )? END_SET NEWLINE
;

bandwidth_irs_stanza
:
   BANDWIDTH null_rest_of_line
;

cadant_stdacl_name
:
   NAME name = variable_permissive NEWLINE
;

community_set_stanza
:
   COMMUNITY_SET name = variable NEWLINE
   community_set_elem_list END_SET NEWLINE
;

community_set_elem_list
:
// no elements

   |
   (
      (
         (
            elems += community_set_elem COMMA
         )
         | hash_comment
      ) NEWLINE
   )*
   (
      elems += community_set_elem
      | hash_comment
   ) NEWLINE
;

etype
:
   ANY
   | ARP
   | IPV4_L5
;

extended_access_list_additional_feature
:
   ACK
   | BEYOND_SCOPE
   | BFD_ECHO
   | COUNT
   | CWR
   | DESTINATION_UNREACHABLE
   |
   (
      DSCP dscp_type
   )
   |
   (
      icmpv6_message_type = DEC icmpv6_message_code = DEC?
   )
   | ECE
   | ECHO
   | ECHO_REPLY
   | ECHO_REQUEST
   |
   (
      ECN ecn = DEC
   )
   | ESTABLISHED
   | FIN
   | FRAGMENTS
   | HOP_LIMIT
   | HOPLIMIT
   | HOST_UNKNOWN
   | HOST_UNREACHABLE
   | LOG
   | LOG_INPUT
   | MLD_QUERY
   | MLD_REDUCTION
   | MLD_REPORT
   | MLDV2
   | ND
   | ND_NA
   | ND_NS
   | ND_TYPE
   | NEIGHBOR
   | NETWORK_UNKNOWN
   | NET_UNREACHABLE
   | PACKET_TOO_BIG
   | PARAMETER_PROBLEM
   | PORT_UNREACHABLE
   | PSH
   | REDIRECT
   | ROUTER
   | ROUTER_ADVERTISEMENT
   | ROUTER_SOLICITATION
   | RST
   | SOURCE_QUENCH
   | SYN
   | TIME_EXCEEDED
   | TRACEROUTE
   | TRACKED
   | TTL_EXCEEDED
   | TTL EQ DEC
   | UNREACHABLE
   | URG
;

extended_access_list_null_tail
:
   (
      (
         SEQ
         | SEQUENCE
      )? num = DEC
   )?
   (
      (
         access_list_action protocol access_list_ip_range port_specifier?
         access_list_ip_range port_specifier? REFLECT
      )
      | DYNAMIC
      | EVALUATE
      | MENU
      | REMARK
      | STATISTICS
   ) null_rest_of_line
;

extended_access_list_stanza
:
   (
      (
         IP ACCESS_LIST EXTENDED name = variable_aclname
      )
      |
      (
         ACCESS_LIST num = ACL_NUM_EXTENDED
      )
      |
      (
         (
            IP
            | IPV4
         ) ACCESS_LIST
         (
            shortname = variable
            | name = variable_aclname
         )
      )
      |
      (
         ACCESS_LIST name = variable_aclname EXTENDED
      )
   )
   (
      (
         NEWLINE
         (
            extended_access_list_tail
            | extended_access_list_null_tail
         )*
      )
      |
      (
         extended_access_list_tail
         | extended_access_list_null_tail
      )
   ) exit_line?
;

extended_access_list_tail
:
   (
      (
         SEQ
         | SEQUENCE
      )? num = DEC
   )? ala = access_list_action
   (
      VLAN vlan = DEC vmask = HEX
   )?
   (
      prot = protocol
      |
      (
         OBJECT_GROUP ogs = variable
      )
   ) srcipr = access_list_ip_range
   (
      alps_src = port_specifier
   )? dstipr = access_list_ip_range
   (
      alps_dst = port_specifier
   )? features += extended_access_list_additional_feature*
   (
      NEXTHOP1 IPV4 nexthop1 = IP_ADDRESS
      (
         NEXTHOP2 IPV4 nexthop2 = IP_ADDRESS
      )?
   )?
   (
      SEQUENCE num = DEC
   )? NEWLINE
;

extended_ipv6_access_list_stanza
:
   IPV6 ACCESS_LIST EXTENDED? name = variable_permissive
   (
      (
         NEWLINE
         (
            extended_ipv6_access_list_tail
            | extended_access_list_null_tail
         )*
      )
      |
      (
         extended_ipv6_access_list_tail
         | extended_access_list_null_tail
      )
   ) exit_line?
;

extended_ipv6_access_list_tail
:
   (
      (
         SEQ
         | SEQUENCE
      )? num = DEC
   )? ala = access_list_action prot = protocol srcipr = access_list_ip6_range
   (
      alps_src = port_specifier
   )? dstipr = access_list_ip6_range
   (
      alps_dst = port_specifier
   )? features += extended_access_list_additional_feature*
   (
      NEXTHOP1 IPV6 nexthop1 = IPV6_ADDRESS
      (
         NEXTHOP2 IPV6 nexthop2 = IPV6_ADDRESS
      )?
   )?
   (
      SEQUENCE num = DEC
   )? NEWLINE
;

interface_rs_stanza
:
   INTERFACE name = interface_name NEWLINE irs_stanza*
;

ip_as_path_access_list_stanza
:
   IP AS_PATH ACCESS_LIST name = variable
   (
      ip_as_path_access_list_tail
      |
      (
         NEWLINE ip_as_path_access_list_tail*
      )
   )
;

ip_as_path_access_list_tail
:
   (
      SEQ DEC
   )? action = access_list_action as_path_regex = RAW_TEXT NEWLINE
;

ip_community_list_expanded_stanza
:
   IP COMMUNITY_LIST
   (
      (
         (
            EXPANDED
            | EXTENDED
         ) name = variable
      )
      | num = COMMUNITY_LIST_NUM_EXPANDED
   ) ip_community_list_expanded_tail
;

ip_community_list_expanded_tail
:
   (
      SEQ DEC
   )? ala = access_list_action DOUBLE_QUOTE?
   (
      remainder += ~( NEWLINE | DOUBLE_QUOTE )
   )+ DOUBLE_QUOTE? NEWLINE
;

ip_community_list_standard_stanza
:
   IP COMMUNITY_LIST
   (
      (
         STANDARD name = variable
      )
      | num = COMMUNITY_LIST_NUM_STANDARD
      | name_cl = variable_community_list
   ) ip_community_list_standard_tail
;

ip_community_list_standard_tail
:
   ala = access_list_action
   (
      communities += community
   )+ NEWLINE
;

ip_prefix_list_stanza
:
   (
      IP
      | IPV4
   )? PREFIX_LIST name = variable
   (
      (
         NEWLINE
         (
            ip_prefix_list_null_tail
            | ip_prefix_list_tail
         )*
      )
      |
      (
         ip_prefix_list_tail
         | ip_prefix_list_null_tail
      )
   )
;

ipv6_prefix_list_stanza
:
   IPV6 PREFIX_LIST name = variable
   (
      (
         NEWLINE
         (
            ip_prefix_list_null_tail
            | ipv6_prefix_list_tail
         )*
      )
      |
      (
         ipv6_prefix_list_tail
         | ip_prefix_list_null_tail
      )
   )
;

ip_prefix_list_null_tail
:
   (
      description_line
   )
   |
   (
      NO SEQ DEC NEWLINE
   )
;

ip_prefix_list_tail
:
   (
      SEQ? seqnum = DEC
   )? action = access_list_action prefix = IP_PREFIX
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
   )* NEWLINE
;

ipacleth_line
:
   action = access_list_action ipacleth_range NEWLINE
;

ipacleth_range
:
   ANY
;

ipaclsession_ip_range
:
   (
      ALIAS alias = variable
   )
   | ANY
   |
   (
      HOST hostip = IP_ADDRESS
   )
   |
   (
      NETWORK net = IP_ADDRESS mask = IP_ADDRESS
   )
   | USER
;

ipaclsession_ip6_range
:
   (
      ALIAS alias = variable
   )
   | ANY
   |
   (
      HOST hostip = IPV6_ADDRESS
   )
   |
   (
      NETWORK net = IPV6_PREFIX
   )
   | USER
;

ipaclsession_line
:
   src = ipaclsession_ip_range dst = ipaclsession_ip_range svc =
   ipaclsession_service action = aruba_access_list_action
   (
      (
         DOT1P_PRIORITY d1ppri = DEC
      )
      | LOG
      |
      (
         QUEUE
         (
            HIGH
         )
      )
      |
      (
         TOS tos = DEC
      )
   )* NEWLINE
;

ipaclsession_line6
:
   IPV6 src = ipaclsession_ip6_range dst = ipaclsession_ip6_range svc =
   ipaclsession_service6 action = aruba_access_list_action
   (
      LOG
      |
      (
         QUEUE
         (
            HIGH
         )
      )
   )* NEWLINE
;

ipaclsession_service
:
   (
      APP app = aruba_app
   )
   |
   (
      APPCATEGORY appcat = aruba_appcategory
   )
   |
   (
      prot = protocol ps = netservice_port_specifier?
   )
   | netsvc = variable
;

ipaclsession_service6
:
   (
      (
         (
            TCP
            | UDP
         ) ps = netservice_port_specifier?
      )
      |
      (
         ICMPV6 is = netservice_icmpv6_specifier?
      )
      |
      (
         prot = protocol
      )
   )
   | netsvc = variable
;

ipv6_prefix_list_tail
:
   (
      SEQ? seqnum = DEC
   )? action = access_list_action prefix6 = IPV6_PREFIX
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
   )* NEWLINE
;

ipx_sap_access_list_null_tail
:
   action = access_list_action null_rest_of_line
;

ipx_sap_access_list_stanza
:
   ACCESS_LIST name = ACL_NUM_IPX_SAP ipx_sap_access_list_null_tail
;

irs_stanza
:
   bandwidth_irs_stanza
   | null_irs_stanza
;

mac_access_list_additional_feature
:
   (
      ETYPE etype
   )
   | HEX
   | IP
   | LOG_ENABLE
   |
   (
      PRIORITY priority = DEC
   )
   |
   (
      PRIORITY_FORCE priority_force = DEC
   )
   |
   (
      PRIORITY_MAPPING priority_mapping = DEC
   )
;

netdestination_description
:
   desc = description_line
;

netdestination_host
:
   HOST ip = IP_ADDRESS NEWLINE
;

netdestination_invert
:
   INVERT NEWLINE
;

netdestination_name
:
   NAME name = variable_permissive NEWLINE
;

netdestination_network
:
   NETWORK net = IP_ADDRESS mask = IP_ADDRESS NEWLINE
;

netdestination6_description
:
   desc = description_line
;

netdestination6_host
:
   HOST ip6 = IPV6_ADDRESS NEWLINE
;

netdestination6_invert
:
   INVERT NEWLINE
;

netdestination6_name
:
   NAME name = variable_permissive NEWLINE
;

netdestination6_network
:
   NETWORK net6 = IPV6_PREFIX NEWLINE
;

netservice_icmpv6_specifier
:
   DEC
   | RTR_ADV
;

netservice_port_specifier
:
   (
      (
         start_port = DEC
         (
            end_port = DEC
         )?
      )
      |
      (
         LIST DOUBLE_QUOTE
         (
            elems += DEC
         )+ DOUBLE_QUOTE
      )
   )
;

no_ip_prefix_list_stanza
:
   NO IP PREFIX_LIST name = variable NEWLINE
;

null_as_path_regex
:
   ~NEWLINE*
;

null_irs_stanza
:
   NO?
   (
      SIGNALLING
   ) null_rest_of_line
;

null_rs_stanza
:
   NO?
   (
      AUTHENTICATION
      | KEY_SOURCE
      | LOGGING
      | WINDOW_SIZE
   ) null_rest_of_line
;

prefix_set_stanza
:
   PREFIX_SET name = variable NEWLINE prefix_set_elem_list END_SET NEWLINE
;

prefix_set_elem_list
:
// no elements

   |
   (
      (
         hash_comment
         |
         (
            prefix_set_elem COMMA
         )
      ) NEWLINE
   )*
   (
      hash_comment
      | prefix_set_elem
   ) NEWLINE
;

protocol_type_code_access_list_null_tail
:
   action = access_list_action null_rest_of_line
;

protocol_type_code_access_list_stanza
:
   ACCESS_LIST name = ACL_NUM_PROTOCOL_TYPE_CODE
   protocol_type_code_access_list_null_tail
;

rs_stanza
:
   interface_rs_stanza
   | null_rs_stanza
;

rsvp_stanza
:
   RSVP NEWLINE rs_stanza*
;

s_arp_access_list_extended
:
   ARP ACCESS_LIST name = variable_permissive NEWLINE
   s_arp_access_list_extended_tail*
;

s_arp_access_list_extended_tail
:
   (
      (
         SEQ
         | SEQUENCE
      )? num = DEC
   )? action = access_list_action
   (
      REQUEST
      | RESPONSE
   )? IP senderip = access_list_ip_range
   (
      targetip = access_list_ip_range
   )? MAC sendermac = access_list_mac_range
   (
      targetmac = access_list_mac_range
   )? LOG? NEWLINE
;

s_ethernet_services
:
   ETHERNET_SERVICES ACCESS_LIST name = variable_permissive NEWLINE
   s_ethernet_services_tail*
;

s_ethernet_services_tail
:
   num = DEC? action = access_list_action src_mac = xr_mac_specifier dst_mac =
   xr_mac_specifier NEWLINE
;

s_foundry_mac_access_list
:
   ACCESS_LIST num = ACL_NUM_FOUNDRY_L2 action = access_list_action
   (
      (
         src_address = MAC_ADDRESS_LITERAL src_wildcard = MAC_ADDRESS_LITERAL
      )
      | src_any = ANY
   )
   (
      (
         dst_address = MAC_ADDRESS_LITERAL dst_wildcard = MAC_ADDRESS_LITERAL
      )
      | dst_any = ANY
   )
   (
      vlan = DEC
      | vlan_any = ANY
   )
   (
      (
         ETYPE etype
      )
      | LOG_ENABLE
      |
      (
         PRIORITY priority = DEC
      )
      |
      (
         PRIORITY_FORCE priority_force = DEC
      )
      |
      (
         PRIORITY_MAPPING priority_mapping = DEC
      )
   )* NEWLINE
;

s_ip_access_list_eth
:
   IP ACCESS_LIST ETH name = variable NEWLINE
   (
      ipacleth_line
   )*
;

s_ip_access_list_session
:
   IP ACCESS_LIST SESSION name = variable NEWLINE
   (
      ipaclsession_line
      | ipaclsession_line6
   )*
;

s_mac_access_list
:
   ACCESS_LIST num = ACL_NUM_MAC action = access_list_action address =
   MAC_ADDRESS_LITERAL wildcard = MAC_ADDRESS_LITERAL NEWLINE
;

s_mac_access_list_extended
:
   (
      ACCESS_LIST num = ACL_NUM_EXTENDED_MAC s_mac_access_list_extended_tail
   )
   |
   (
      MAC ACCESS_LIST EXTENDED? name = variable_permissive EXTENDED? NEWLINE
      s_mac_access_list_extended_tail*
   )
;

s_mac_access_list_extended_tail
:
   (
      (SEQ | SEQUENCE)? num = DEC
   )?
   action = access_list_action src = access_list_mac_range dst = access_list_mac_range
   (
      vlan = DEC
      | vlan_any = ANY
   )? mac_access_list_additional_feature* NEWLINE
;

s_netdestination
:
   NETDESTINATION name = variable NEWLINE
   (
      netdestination_description
      | netdestination_host
      | netdestination_invert
      | netdestination_name
      | netdestination_network
   )*
;

s_netdestination6
:
   NETDESTINATION6 name = variable NEWLINE
   (
      netdestination6_description
      | netdestination6_host
      | netdestination6_invert
      | netdestination6_name
      | netdestination6_network
   )*
;

s_netservice
:
   NETSERVICE name = variable prot = protocol ps = netservice_port_specifier?
   (
      ALG alg = netservice_alg
   )? NEWLINE
;

standard_access_list_additional_feature
:
   (
      DSCP dscp_type
   )
   |
   (
      ECN ecn = DEC
   )
   | LOG
;

standard_access_list_null_tail
:
   (
      (
         SEQ
         | SEQUENCE
      )? num = DEC
   )?
   (
      REMARK
      | STATISTICS
   ) null_rest_of_line
;

standard_access_list_stanza
:
   (
      (
         IP ACCESS_LIST STANDARD name = variable_aclname
      )
      |
      (
         ACCESS_LIST num = ACL_NUM_STANDARD
      )
      |
      (
         ACCESS_LIST name = variable_aclname STANDARD
      )
   )
   (
      (
         NEWLINE
         (
            standard_access_list_tail
            | cadant_stdacl_name
            | standard_access_list_null_tail
         )*
      )
      |
      (
         (
            standard_access_list_tail
            | standard_access_list_null_tail
         )
      )
   )
;

standard_ipv6_access_list_stanza
:
   IPV6 ACCESS_LIST STANDARD name = variable
   (
      (
         NEWLINE
         (
            standard_ipv6_access_list_tail
            | standard_access_list_null_tail
         )*
      )
      |
      (
         (
            standard_ipv6_access_list_tail
            | standard_access_list_null_tail
         )
      )
   )
;

standard_access_list_tail
:
   (
      (
         SEQ
         | SEQUENCE
      )? num = DEC
   )? ala = access_list_action ipr = access_list_ip_range
   (
      features += standard_access_list_additional_feature
   )* NEWLINE
;

standard_ipv6_access_list_tail
:
   (
      (
         SEQ
         | SEQUENCE
      )? num = DEC
   )? ala = access_list_action ipr = access_list_ip6_range
   (
      features += standard_access_list_additional_feature
   )* NEWLINE
;

variable_community_list
:
   ~( NEWLINE | COMMUNITY_LIST_NUM_STANDARD | COMMUNITY_LIST_NUM_EXPANDED | DEC
   )
;

xr_mac_specifier
:
   ANY
   |
   (
      HOST host = MAC_ADDRESS_LITERAL
   )
   |
   (
      address = MAC_ADDRESS_LITERAL mask = MAC_ADDRESS_LITERAL
   )
;
