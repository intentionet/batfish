parser grammar CumulusFrr_interface;

import CumulusFrr_common;

options {
  tokenVocab = CumulusFrrLexer;
}

s_interface
:
  INTERFACE name = word (VRF vrf = word)? NEWLINE
  (
    si_description
    | si_ip
    | si_ipv6
    | si_no
    | si_shutdown
  )*
;

si_shutdown
:
  SHUTDOWN NEWLINE
;

si_ip
:
  IP 
  (
    siip_address
    | siip_ospf
  )  
;

siip_address
:
  ADDRESS ip_prefix = IP_PREFIX NEWLINE
;

si_ipv6
:
   IPV6 null_rest_of_line
;

si_no
:
   NO
   (
      sino_ipv6
   )
;

sino_ipv6
:
   IPV6 null_rest_of_line
;

si_description
:
  DESCRIPTION description = REMARK_TEXT NEWLINE
;

siip_ospf
:
  OSPF
  (
    siipo_area
    | siipo_authentication
    | siipo_message_digest_key
    | siipo_network_p2p
    | siipo_cost
    | siipo_hello_interval
    | siipo_dead_interval
  )
  NEWLINE
;

siipo_area
:
  AREA area = ospf_area
;

siipo_authentication
:
  AUTHENTICATION MESSAGE_DIGEST
;

siipo_message_digest_key
:
  MESSAGE_DIGEST_KEY uint32 MD5 REMARK_TEXT
;

siipo_network_p2p
:
  NETWORK POINT_TO_POINT
;

siipo_cost
:
  COST interface_ospf_cost
;

siipo_hello_interval
:
  HELLO_INTERVAL interface_ospf_interval
;

siipo_dead_interval
:
  DEAD_INTERVAL interface_ospf_interval
;

interface_ospf_cost
:
  uint16
;

interface_ospf_interval
:
  uint16
;