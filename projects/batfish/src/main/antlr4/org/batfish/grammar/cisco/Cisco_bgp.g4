parser grammar Cisco_bgp;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

activate_bgp_tail
:
   ACTIVATE NEWLINE
;

address_family_header returns [String addressFamilyStr]
:
   ADDRESS_FAMILY af = bgp_address_family
   {$addressFamilyStr = $af.ctx.getText();}

   NEWLINE
;

bgp_address_family
:
   (
      (
         IPV4
         (
            MDT
            | LABELED_UNICAST
         )?
      )
      | IPV6
      (
         LABELED_UNICAST
      )?
      | L2VPN
      | VPNV4
      | VPNV6
   )
   (
      UNICAST
      | MULTICAST
      | VPLS
   )?
   (
      VRF vrf_name = VARIABLE
   )?
;

address_family_rb_stanza
:
   address_family_header
   (
      additional_paths_rb_stanza
      | aggregate_address_rb_stanza
      | bgp_tail
      |
      {!_multilineBgpNeighbors}?

      neighbor_flat_rb_stanza
      | no_neighbor_activate_rb_stanza
      | no_neighbor_shutdown_rb_stanza
      | null_no_neighbor_rb_stanza
      | peer_group_assignment_rb_stanza
      | peer_group_creation_rb_stanza
   )* address_family_footer
;

address_family_enable_rb_stanza
:
   ADDRESS_FAMILY af = bgp_address_family ENABLE NEWLINE
;

af_group_rb_stanza
:
   AF_GROUP name = variable ADDRESS_FAMILY bgp_address_family NEWLINE bgp_tail*
;

aggregate_address_rb_stanza
:
   AGGREGATE_ADDRESS
   (
      (
         network = IP_ADDRESS subnet = IP_ADDRESS
      )
      | prefix = IP_PREFIX
      | ipv6_prefix = IPV6_PREFIX
   )
   (
      as_set = AS_SET
      | summary_only = SUMMARY_ONLY
      |
      (
         ATTRIBUTE_MAP mapname = variable
      )
   )* NEWLINE
;

additional_paths_rb_stanza
:
   BGP ADDITIONAL_PATHS
   (
      SELECT ALL
      |
      (
         SEND RECEIVE?
      )
      |
      (
         RECEIVE SEND?
      )
   ) NEWLINE
;

advertise_bgp_tail
:
   ADVERTISE ADDITIONAL_PATHS ALL NEWLINE
;

allowas_in_bgp_tail
:
   ALLOWAS_IN
   (
      num = DEC
   )? NEWLINE
;

always_compare_med_rb_stanza
:
   BGP ALWAYS_COMPARE_MED NEWLINE
;

as_override_bgp_tail
:
   AS_OVERRIDE NEWLINE
;

as_path_multipath_relax_rb_stanza
:
   NO? BGP? BESTPATH AS_PATH MULTIPATH_RELAX NEWLINE
;

auto_summary_bgp_tail
:
   NO? AUTO_SUMMARY NEWLINE
;

bgp_advertise_inactive_rb_stanza
:
   BGP ADVERTISE_INACTIVE NEWLINE
;

//confederations are not currently implemented
//not putting this under null so we can warn the user

bgp_confederation_rb_stanza
:
   BGP CONFEDERATION ~NEWLINE+ NEWLINE
;

bgp_listen_range_rb_stanza
:
   BGP LISTEN RANGE
   (
      IP_PREFIX
      | IPV6_PREFIX
   ) PEER_GROUP name = variable
   (
      REMOTE_AS as = DEC
   )? NEWLINE
;

bgp_redistribute_internal_rb_stanza
:
   BGP REDISTRIBUTE_INTERNAL NEWLINE
;

bgp_tail
:
   activate_bgp_tail
   | advertise_bgp_tail
   | allowas_in_bgp_tail
   | as_override_bgp_tail
   | cluster_id_bgp_tail
   | default_metric_bgp_tail
   | default_originate_bgp_tail
   | default_shutdown_bgp_tail
   | description_bgp_tail
   | disable_peer_as_check_bgp_tail
   | distribute_list_bgp_tail
   | ebgp_multihop_bgp_tail
   | local_as_bgp_tail
   | maximum_paths_bgp_tail
   | maximum_peers_bgp_tail
   | network_bgp_tail
   | network6_bgp_tail
   | next_hop_self_bgp_tail
   | no_network_bgp_tail
   | null_bgp_tail
   | prefix_list_bgp_tail
   | redistribute_aggregate_bgp_tail
   | redistribute_connected_bgp_tail
   | redistribute_ospf_bgp_tail
   | redistribute_rip_bgp_tail
   | redistribute_static_bgp_tail
   | remove_private_as_bgp_tail
   | route_map_bgp_tail
   | route_policy_bgp_tail
   | route_reflector_client_bgp_tail
   | router_id_bgp_tail
   | send_community_bgp_tail
   | shutdown_bgp_tail
   | subnet_bgp_tail
   | update_source_bgp_tail
   | weight_bgp_tail
;

cluster_id_bgp_tail
:
   CLUSTER_ID
   (
      DEC
      | IP_ADDRESS
   ) NEWLINE
;

cluster_id_rb_stanza
:
   BGP cluster_id_bgp_tail
;

compare_routerid_rb_stanza
:
   (
      BGP? BESTPATH
   )? COMPARE_ROUTERID NEWLINE
;

default_information_originate_rb_stanza
:
   DEFAULT_INFORMATION ORIGINATE NEWLINE
;

default_metric_bgp_tail
:
   DEFAULT_METRIC metric = DEC NEWLINE
;

default_originate_bgp_tail
:
   DEFAULT_ORIGINATE
   (
      ROUTE_MAP map = variable
      | ROUTE_POLICY policy = VARIABLE
   )? NEWLINE
;

default_shutdown_bgp_tail
:
   DEFAULT SHUTDOWN NEWLINE
;

description_bgp_tail
:
   description_line
;

disable_peer_as_check_bgp_tail
:
   DISABLE_PEER_AS_CHECK NEWLINE
;

distribute_list_bgp_tail
:
   DISTRIBUTE_LIST ~NEWLINE* NEWLINE
;

ebgp_multihop_bgp_tail
:
   EBGP_MULTIHOP
   (
      hop = DEC
   )? NEWLINE
;

empty_neighbor_block_address_family
:
   address_family_header address_family_footer
;

filter_list_bgp_tail
:
   FILTER_LIST num = DEC
   (
      IN
      | OUT
   ) NEWLINE
;

inherit_peer_policy_bgp_tail
:
   INHERIT PEER_POLICY name = variable num = DEC? NEWLINE
;

inherit_peer_session_bgp_tail
:
   INHERIT PEER_SESSION name = variable NEWLINE
;

local_as_bgp_tail
:
   LOCAL_AS as = DEC
   (
      NO_PREPEND
      | REPLACE_AS
   )* NEWLINE
;

maximum_peers_bgp_tail
:
   MAXIMUM_PEERS DEC NEWLINE
;

maximum_paths_bgp_tail
:
   MAXIMUM_PATHS
   (
      EBGP
      | IBGP
      | EIBGP
   )? paths = DEC
   (
      ECMP ecmp_paths = DEC
   )? SELECTIVE? NEWLINE
;

maximum_prefix_bgp_tail
:
   MAXIMUM_PREFIX DEC NEWLINE
;

neighbor_block_address_family
:
   address_family_header
   (
      bgp_tail
      | use_af_group_bgp_tail
   )+ address_family_footer
;

neighbor_block_inherit
:
   INHERIT PEER name = VARIABLE NEWLINE
;

neighbor_block_rb_stanza
locals
[java.util.Set<String> addressFamilies, java.util.Set<String> consumedAddressFamilies]
@init {
   $addressFamilies = new java.util.HashSet<String>();
   $consumedAddressFamilies = new java.util.HashSet<String>();
}
:
   NEIGHBOR
   (
      ip_address = IP_ADDRESS
      | ipv6_address = IPV6_ADDRESS
      | ip_prefix = IP_PREFIX
      | ipv6_prefix = IPV6_PREFIX
   )
   (
      REMOTE_AS asnum = DEC
   )? NEWLINE
   (
      bgp_tail
      | neighbor_block_inherit
      | no_shutdown_rb_stanza
      | remote_as_bgp_tail
      | use_neighbor_group_bgp_tail
      | use_session_group_bgp_tail
   )*
   (
      (
         empty_neighbor_block_address_family
         | neighbor_block_address_family
      )* neighbor_block_address_family
   )?
;

neighbor_flat_rb_stanza
:
   NEIGHBOR
   (
      ip = IP_ADDRESS
      | ip6 = IPV6_ADDRESS
      | peergroup = ~( IP_ADDRESS | IPV6_ADDRESS | NEWLINE )
   )
   (
      bgp_tail
      | inherit_peer_session_bgp_tail
      | inherit_peer_policy_bgp_tail
      | filter_list_bgp_tail
      | remote_as_bgp_tail
   )
;

neighbor_group_rb_stanza
:
   NEIGHBOR_GROUP name = variable NEWLINE
   (
      address_family_rb_stanza
      | remote_as_bgp_tail
      | update_source_bgp_tail
      | use_neighbor_group_bgp_tail
      | use_session_group_bgp_tail
   )*
;

network_bgp_tail
:
   NETWORK
   (
      (
         ip = IP_ADDRESS
         (
            MASK mask = IP_ADDRESS
         )?
      )
      | prefix = IP_PREFIX
   )?
   (
      ROUTE_MAP mapname = variable
   )?
   (
      ROUTE_POLICY policyname = VARIABLE
   )? NEWLINE
;

network6_bgp_tail
:
   NETWORK prefix = IPV6_PREFIX
   (
      ROUTE_MAP mapname = variable
   )?
   (
      ROUTE_POLICY policyname = VARIABLE
   )? NEWLINE
;

next_hop_self_bgp_tail
:
   NO? NEXT_HOP_SELF NEWLINE
;

vrf_block_rb_stanza
:
   VRF name = ~NEWLINE NEWLINE
   (
      address_family_rb_stanza
      | always_compare_med_rb_stanza
      | bgp_listen_range_rb_stanza
      | bgp_tail
      | neighbor_flat_rb_stanza
      | neighbor_block_rb_stanza
      | no_neighbor_activate_rb_stanza
      | no_neighbor_shutdown_rb_stanza
      | no_redistribute_connected_rb_stanza
      | null_no_neighbor_rb_stanza
      | peer_group_assignment_rb_stanza
      | peer_group_creation_rb_stanza
      | router_id_rb_stanza
      | template_peer_rb_stanza
      | template_peer_session_rb_stanza
   )*
;

no_bgp_enforce_first_as_stanza
:
   NO BGP ENFORCE_FIRST_AS NEWLINE
;

no_neighbor_activate_rb_stanza
:
   NO NEIGHBOR
   (
      ip = IP_ADDRESS
      | ip6 = IPV6_ADDRESS
      | peergroup = ~( IP_ADDRESS | IPV6_ADDRESS | NEWLINE )
   ) ACTIVATE NEWLINE
;

no_neighbor_shutdown_rb_stanza
:
   (
      NO NEIGHBOR
      (
         ip = IP_ADDRESS
         | ip6 = IPV6_ADDRESS
         | peergroup = ~( IP_ADDRESS | IPV6_ADDRESS | NEWLINE )
      ) SHUTDOWN NEWLINE
   )
   |
   (
      NEIGHBOR
      (
         ip = IP_ADDRESS
         | ip6 = IPV6_ADDRESS
         | peergroup = ~( IP_ADDRESS | IPV6_ADDRESS | NEWLINE )
      ) NO SHUTDOWN NEWLINE
   )
;

no_network_bgp_tail
:
   NO NETWORK ~NEWLINE* NEWLINE
;

no_redistribute_connected_rb_stanza
:
   NO REDISTRIBUTE
   (
      CONNECTED
      | DIRECT
   ) ~NEWLINE* NEWLINE
;

no_shutdown_rb_stanza
:
   NO SHUTDOWN NEWLINE
;

null_bgp_tail
:
   NO?
   (
      ADVERTISEMENT_INTERVAL
      | AUTO_SHUTDOWN_NEW_NEIGHBORS
      | AUTO_SUMMARY
      | AUTO_LOCAL_ADDR
      |
      (
         AGGREGATE_ADDRESS
         (
            IPV6_ADDRESS
            | IPV6_PREFIX
         )
      )
      |
      (
         BESTPATH
         (
            AS_PATH
            (
               CONFED
            )
         )
      )
      | BFD
      | BFD_ENABLE
      |
      (
         BGP
         (
            ATTRIBUTE_DOWNLOAD
            |
            (
               BESTPATH
               (
                  AS_PATH
                  (
                     CONFED
                  )
               )
            )
            | CLIENT_TO_CLIENT
            | DAMPENING
            | DEFAULT
            | DETERMINISTIC_MED
            | FAST_EXTERNAL_FALLOVER
            | GRACEFUL_RESTART
            |
            (
               LISTEN LIMIT
            )
            | LOG
            | LOG_NEIGHBOR_CHANGES
            | NEXTHOP
            | NON_DETERMINISTIC_MED
            | REDISTRIBUTE_INTERNAL
            | SCAN_TIME
         )
      )
      | CAPABILITY
      | CLIENT_TO_CLIENT
      | CONNECT_RETRY
      | DAMPEN
      | DAMPEN_IGP_METRIC
      | DAMPENING
      | DESCRIPTION
      | DISTANCE
      | DONT_CAPABILITY_NEGOTIATE
      | DYNAMIC_CAPABILITY
      | ENFORCE_FIRST_AS
      | EVENT_HISTORY
      | EXIT
      | FAIL_OVER
      | FALL_OVER
      | FAST_EXTERNAL_FALLOVER
      | GRACEFUL_RESTART
      | LOCAL_V6_ADDR
      | LOG_NEIGHBOR_CHANGES
      | MAXIMUM_PREFIX
      | MAXIMUM_ACCEPTED_ROUTES
      | MAXIMUM_ROUTES
      | MULTIPATH
      |
      (
         NO
         (
            REMOTE_AS
            | ROUTE_MAP
            | UPDATE_SOURCE
            | SHUTDOWN
         )
      )
      | NEIGHBOR_DOWN
      | NEXT_HOP_THIRD_PARTY
      | NEXTHOP
      | NSR
      | PASSWORD
      | RECONNECT_INTERVAL
      | SEND_LABEL
      | SESSION_OPEN_MODE
      | SOFT_RECONFIGURATION
      | SUPPRESS_FIB_PENDING
      | SYNCHRONIZATION
      | TABLE_MAP
      | TIMERS
      | TRANSPORT
      |
      (
         USE
         (
            NEXTHOP_ATTRIBUTE
         )
      )
      | VERSION
   ) ~NEWLINE* NEWLINE
;

null_no_neighbor_rb_stanza
:
   NO NEIGHBOR
   (
      ip = IP_ADDRESS
      | ip6 = IPV6_ADDRESS
      | peergroup = ~( IP_ADDRESS | IPV6_ADDRESS | NEWLINE )
   ) null_bgp_tail
;

peer_group_assignment_rb_stanza
:
   NEIGHBOR
   (
      address = IP_ADDRESS
      | address6 = IPV6_ADDRESS
   ) PEER_GROUP name = variable NEWLINE
;

peer_group_creation_rb_stanza
:
   NEIGHBOR name = VARIABLE PEER_GROUP PASSIVE?
   (
      NLRI
      | UNICAST
      | MULTICAST
   )* NEWLINE
;

prefix_list_bgp_tail
:
   PREFIX_LIST list_name = VARIABLE
   (
      IN
      | OUT
   ) NEWLINE
;

remote_as_bgp_tail
:
   REMOTE_AS as = DEC NEWLINE
;

remove_private_as_bgp_tail
:
   REMOVE_PRIVATE_AS ALL? NEWLINE
;

route_map_bgp_tail
:
   ROUTE_MAP
   (
      name = variable
      (
         IN
         | OUT
      )
      |
      (
         IN
         | OUT
      ) name = variable
   ) NEWLINE
;

route_policy_bgp_tail
:
   ROUTE_POLICY name = variable
   (
      PAREN_LEFT route_policy_params_list PAREN_RIGHT
   )?
   (
      IN
      | OUT
   ) NEWLINE
;

route_reflector_client_bgp_tail
:
   ROUTE_REFLECTOR_CLIENT NEWLINE
;

redistribute_aggregate_bgp_tail
:
   REDISTRIBUTE AGGREGATE NEWLINE
;

redistribute_connected_bgp_tail
:
   REDISTRIBUTE
   (
      CONNECTED
      | DIRECT
   )
   (
      (
         ROUTE_MAP map = variable
      )
      |
      (
         ROUTE_POLICY policy = variable
      )
      |
      (
         METRIC metric = DEC
      )
   )* NEWLINE
;

redistribute_ospf_bgp_tail
:
   REDISTRIBUTE OSPF procnum = DEC
   (
      (
         ROUTE_MAP map = variable
      )
      |
      (
         METRIC metric = DEC
      )
      |
      (
         MATCH ospf_route_type*
      )
   )* NEWLINE
;

redistribute_rip_bgp_tail
:
   REDISTRIBUTE RIP
   (
      (
         ROUTE_MAP map = variable
      )
      |
      (
         METRIC metric = DEC
      )
   )* NEWLINE
;

redistribute_static_bgp_tail
:
   REDISTRIBUTE STATIC
   (
      (
         ROUTE_MAP map = variable
      )
      |
      (
         ROUTE_POLICY policy = VARIABLE
      )
      |
      (
         METRIC metric = DEC
      )
   )* NEWLINE
;

router_bgp_stanza
:
   ROUTER BGP
   (
      procnum = DEC
   )? NEWLINE router_bgp_stanza_tail+
;

router_bgp_stanza_tail
:
   additional_paths_rb_stanza
   | address_family_rb_stanza
   | address_family_enable_rb_stanza
   | af_group_rb_stanza
   | aggregate_address_rb_stanza
   | always_compare_med_rb_stanza
   | as_path_multipath_relax_rb_stanza
   | bgp_advertise_inactive_rb_stanza
   | bgp_confederation_rb_stanza
   | bgp_listen_range_rb_stanza
   | bgp_redistribute_internal_rb_stanza
   | bgp_tail
   | cluster_id_rb_stanza
   | compare_routerid_rb_stanza
   | default_information_originate_rb_stanza
   |
   // do NOT put neighbor_block_rb_stanza under neighbor_flat_rb_stanza
   {_multilineBgpNeighbors}?

   neighbor_block_rb_stanza
   | neighbor_flat_rb_stanza
   | neighbor_group_rb_stanza
   | no_bgp_enforce_first_as_stanza
   | no_neighbor_activate_rb_stanza
   | no_neighbor_shutdown_rb_stanza
   | no_redistribute_connected_rb_stanza
   | null_no_neighbor_rb_stanza
   | peer_group_assignment_rb_stanza
   | peer_group_creation_rb_stanza
   | router_id_rb_stanza
   | session_group_rb_stanza
   | template_peer_rb_stanza
   | template_peer_policy_rb_stanza
   | template_peer_session_rb_stanza
   | vrf_block_rb_stanza
;

router_id_bgp_tail
:
   ROUTER_ID routerid = IP_ADDRESS NEWLINE
;

router_id_rb_stanza
:
   BGP router_id_bgp_tail
;

send_community_bgp_tail
:
   (
      (
         SEND_COMMUNITY EXTENDED? BOTH?
      )
      | SEND_COMMUNITY_EBGP
      |
      (
         SEND_EXTENDED_COMMUNITY_EBGP INHERITANCE_DISABLE?
      )
   ) NEWLINE
;

session_group_rb_stanza
:
   SESSION_GROUP name = variable NEWLINE
   (
      bgp_tail
      | remote_as_bgp_tail
      | use_session_group_bgp_tail
   )*
;

shutdown_bgp_tail
:
   SHUTDOWN NEWLINE
;

subnet_bgp_tail
:
   SUBNET
   (
      prefix = IP_PREFIX
      | ipv6_prefix = IPV6_PREFIX
   ) NEWLINE
;

template_peer_address_family
:
   address_family_header bgp_tail* address_family_footer
;

template_peer_rb_stanza
locals [java.util.Set<String> addressFamilies] @init {
   $addressFamilies = new java.util.HashSet<String>();
}
:
   TEMPLATE PEER name = VARIABLE NEWLINE template_peer_rb_stanza_tail
   [$addressFamilies]
;

template_peer_rb_stanza_tail [java.util.Set<String> addressFamilies]
locals [boolean active]
:
{
   if (_input.LT(1).getType() == ADDRESS_FAMILY) {
      String addressFamilyString = "";
      for (int i = 1, currentType = -1; _input.LT(i).getType() != NEWLINE; i++) {
         addressFamilyString += " " + _input.LT(i).getText();
      }
      if ($addressFamilies.contains(addressFamilyString)) {
         $active = false;
      }
      else {
         $addressFamilies.add(addressFamilyString);
         $active = true;
      }
   }
   else {
      $active = true;
   }
}

   (
      {$active}?

      (
         bgp_tail
         | inherit_peer_session_bgp_tail
         | no_shutdown_rb_stanza
         | remote_as_bgp_tail
         | template_peer_address_family
      ) template_peer_rb_stanza_tail [$addressFamilies]
      | // intentional blank

   )
;

template_peer_policy_rb_stanza
:
   TEMPLATE PEER_POLICY name = VARIABLE NEWLINE
   (
      bgp_tail
      | inherit_peer_policy_bgp_tail
   )*
   (
      EXIT_PEER_POLICY NEWLINE
   )
;

template_peer_session_rb_stanza
:
   TEMPLATE PEER_SESSION name = VARIABLE NEWLINE
   (
      bgp_tail
      | remote_as_bgp_tail
   )*
   (
      EXIT_PEER_SESSION NEWLINE
   )?
;

update_source_bgp_tail
:
   UPDATE_SOURCE source = interface_name NEWLINE
;

use_af_group_bgp_tail
:
   USE AF_GROUP name = variable NEWLINE
;

use_neighbor_group_bgp_tail
:
   USE NEIGHBOR_GROUP name = variable NEWLINE
;

use_session_group_bgp_tail
:
   USE SESSION_GROUP name = variable NEWLINE
;

weight_bgp_tail
:
   WEIGHT weight = DEC NEWLINE
;
