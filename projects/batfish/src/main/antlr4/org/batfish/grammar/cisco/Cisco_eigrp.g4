parser grammar Cisco_eigrp;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

re_classic
:
   ROUTER EIGRP asnum = DEC NEWLINE
   re_classic_tail*
;

re_classic_tail
:
   rec_address_family
   | rec_null
   | re_network
   | re_passive_interface_default
   | re_passive_interface
   | re_redistribute_bgp
   | re_redistribute_connected
   | re_redistribute_eigrp
   | re_redistribute_isis
   | re_redistribute_ospf
   | re_redistribute_rip
   | re_redistribute_static
;

re_named
:
   ROUTER EIGRP virtname = variable NEWLINE
   re_named_tail*
;

re_named_tail
:
   ren_address_family
   | ren_null
   | ren_service_family
;

re_network
:
   NETWORK address = IP_ADDRESS mask = IP_ADDRESS? NEWLINE
;

re_passive_interface
:
   NO? PASSIVE_INTERFACE i = interface_name NEWLINE
;

re_passive_interface_default
:
   NO? PASSIVE_INTERFACE DEFAULT NEWLINE
;

re_redistribute_bgp
:
   REDISTRIBUTE BGP asn = bgp_asn
   (
      METRIC bw_kbps = DEC delay_10us = DEC reliability = DEC eff_bw = DEC mtu = DEC
      | ROUTE_MAP map = variable
   )* NEWLINE
;

re_redistribute_connected
:
   REDISTRIBUTE CONNECTED
   (
      METRIC bw_kbps = DEC delay_10us = DEC reliability = DEC eff_bw = DEC mtu = DEC
      | ROUTE_MAP map = variable
   )* NEWLINE
;

re_redistribute_eigrp
:
   REDISTRIBUTE EIGRP asn = DEC
   (
      METRIC bw_kbps = DEC delay_10us = DEC reliability = DEC eff_bw = DEC mtu = DEC
      | ROUTE_MAP map = variable
   )* NEWLINE
;

re_redistribute_isis
:
   REDISTRIBUTE ISIS (tag = variable)?
   (
      LEVEL_1
      | LEVEL_1_2
      | LEVEL_2
      | METRIC bw_kbps = DEC delay_10us = DEC reliability = DEC eff_bw = DEC mtu = DEC
      | ROUTE_MAP map = variable
   )* NEWLINE
;

re_redistribute_ospf
:
   REDISTRIBUTE OSPF proc = DEC
   (
      MATCH (
         EXTERNAL DEC?
         | INTERNAL
         | NSSA_EXTERNAL DEC?
      )+
      | METRIC bw_kbps = DEC delay_10us = DEC reliability = DEC eff_bw = DEC mtu = DEC
      | ROUTE_MAP map = variable
   )* NEWLINE
;

re_redistribute_rip
:
   REDISTRIBUTE RIP
   (
      METRIC bw_kbps = DEC delay_10us = DEC reliability = DEC eff_bw = DEC mtu = DEC
      | ROUTE_MAP map = variable
   )* NEWLINE
;

re_redistribute_static
:
   REDISTRIBUTE STATIC
   (
      METRIC bw_kbps = DEC delay_10us = DEC reliability = DEC eff_bw = DEC mtu = DEC
      | ROUTE_MAP map = variable
   )* NEWLINE
;

re_topology_base
:
   TOPOLOGY BASE NEWLINE
;

re_topology_name
:
   TOPOLOGY topo_name = variable TID topo_num = DEC NEWLINE
;

reaf_interface
:
   AF_INTERFACE iname = interface_name NEWLINE
   reaf_interface_tail*
   (
      EXIT_AF_INTERFACE NEWLINE
   )?
;

reaf_interface_default
:
   AF_INTERFACE DEFAULT NEWLINE
   reaf_interface_tail*
   (
      EXIT_AF_INTERFACE NEWLINE
   )?
;

reaf_interface_null
:
   NO?
   (
      ADD_PATHS
      | AUTHENTICATION
      | BANDWIDTH_PERCENT
      | BFD
      | DAMPENING_CHANGE
      | DAMPENING_INTERVAL
      | HELLO_INTERVAL
      | HOLD_TIME
      | NEXT_HOP_SELF
      | SHUTDOWN
      | SPLIT_HORIZON
      | SUMMARY_ADDRESS
   ) null_rest_of_line
;

reaf_interface_tail
:
   reaf_interface_null
   | reafi_passive_interface
;

reaf_topology_tail
:
   reaf_topology_null
;

reaf_topology
:
   (
     re_topology_base
     | re_topology_name
   ) reaf_topology_tail*
   (
      EXIT_AF_TOPOLOGY NEWLINE
   )?
;

reaf_topology_null
:
   NO?
   (
      AUTO_SUMMARY
      | CTS
      | DEFAULT_INFORMATION
      | DEFAULT_METRIC
      | DISTANCE
      | DISTRIBUTE_LIST
      | EIGRP
      | FAST_REROUTE
      | MAXIMUM_PATHS
      | METRIC
      | OFFSET_LIST
      | REDISTRIBUTE
      | SNMP
      | SUMMARY_METRIC
      | TIMERS
      | TRAFFIC_SHARE
      | VARIANCE
   ) null_rest_of_line
;

reafi_passive_interface
:
   NO? PASSIVE_INTERFACE NEWLINE
;

rec_address_family
:
   ADDRESS_FAMILY IPV4 UNICAST? VRF vrf = variable
   (
      AUTONOMOUS_SYSTEM asnum = DEC
   )? NEWLINE
   rec_address_family_tail* address_family_footer
;

rec_address_family_null
:
   NO?
   (
      AUTO_SUMMARY
      | AUTONOMOUS_SYSTEM
      | BFD
      | DEFAULT_INFORMATION
      | DEFAULT_METRIC
      | DISTANCE
      | DISTRIBUTE_LIST
      | EIGRP
      | MAXIMUM_PATHS
      | MAXIMUM_PREFIX
      | METRIC
      | NEIGHBOR
      | NSF
      | OFFSET_LIST
      | REDISTRIBUTE
   ) null_rest_of_line
;

rec_address_family_tail
:
   re_network
   | re_passive_interface_default
   | re_passive_interface
   | rec_address_family_null
;

rec_null
:
   NO?
   (
      AUTO_SUMMARY
      | BFD
      | DEFAULT_INFORMATION
      | DEFAULT_METRIC
      | DISTANCE
      | DISTRIBUTE_LIST
      | EIGRP
      | HELLO_INTERVAL
      | MAXIMUM_PATHS
      | METRIC
      | NEIGHBOR
      | NSF
      | OFFSET_LIST
      | SHUTDOWN
      | SPLIT_HORIZON
      | SUMMARY_METRIC
      | TIMERS
      | TRAFFIC_SHARE
      | VARIANCE
   ) null_rest_of_line
;

ren_address_family
:
   ADDRESS_FAMILY
   (
      IPV4
      | IPV6
   )
   (
      UNICAST
      | MULTICAST
   )?
   (
      VRF vrf = variable
   )? AUTONOMOUS_SYSTEM asnum = DEC NEWLINE
   ren_address_family_tail* address_family_footer
;

ren_address_family_null
:
   NO?
   (
      EIGRP
      | MAXIMUM_PREFIX
      | METRIC
      | NEIGHBOR
      | NSF
      | REMOTE_NEIGHBORS
      | SHUTDOWN
      | TIMERS
   ) null_rest_of_line
;

ren_address_family_tail
:
   re_network
   | re_passive_interface_default
   | re_passive_interface
   | reaf_interface_default
   | reaf_interface
   | reaf_topology
   | ren_address_family_null
;

ren_null
:
   NO? SHUTDOWN
;

ren_service_family
:
   SERVICE_FAMILY
   (
      IPV4
      | IPV6
   )
   (
      VRF vrf = variable
   )? AUTONOMOUS_SYSTEM asnum = DEC NEWLINE
   ren_service_family_tail*
   resf_footer
;

ren_service_family_null
:
   NO?
   (
      EIGRP
      | TIMERS
   ) null_rest_of_line
;

ren_service_family_tail
:
   ren_service_family_null
   | resf_interface_default
   | resf_interface
   | resf_topology
;

resf_interface
:
   SF_INTERFACE iname = interface_name NEWLINE
   resf_interface_tail*
   (
      EXIT_SF_INTERFACE NEWLINE
   )?
;

resf_interface_default
:
   SF_INTERFACE DEFAULT NEWLINE
   resf_interface_tail*
   (
      EXIT_SF_INTERFACE NEWLINE
   )?
;

resf_interface_null
:
   NO?
   (
      AUTHENTICATION
      | BANDWIDTH_PERCENT
      | DAMPENING_CHANGE
      | DAMPENING_INTERVAL
      | HELLO_INTERVAL
      | HOLD_TIME
      | SHUTDOWN
      | SPLIT_HORIZON
   ) null_rest_of_line
;

resf_interface_tail
:
   resf_interface_null
;

resf_null
:
   NO?
   (
      EIGRP
      | METRIC
      | NEIGHBOR
      | REMOTE_NEIGHBORS
      | SHUTDOWN
      | TIMERS
   ) null_rest_of_line
;

resf_topology_tail
:
   resf_topology_null
;

resf_topology_null
:
   NO?
   (
      EIGRP
      | METRIC
      | TIMERS
   ) null_rest_of_line
;

resf_footer
:
   (
      (
         EXIT_SERVICE_FAMILY
         | EXIT
      ) NEWLINE
   )?
;

resf_topology
:
   (
     re_topology_base
     | re_topology_name
   ) resf_topology_tail*
   (
      EXIT_SF_TOPOLOGY NEWLINE
   )?
;

s_router_eigrp
:
  re_classic
  | re_named
;
