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
   address_family_header address_family_inner* ( EXIT |  EXIT_ADDRESS_FAMILY ) NEWLINE
;

address_family_inner
:
   additional_paths_rb_stanza
   | aggregate_address_rb_stanza
   | bgp_af_import
   | bgp_tail
   | default_information_originate_rb_stanza
   | neighbor_flat_rb_stanza
   | no_neighbor_activate_rb_stanza
   | no_neighbor_shutdown_rb_stanza
   | null_no_neighbor_rb_stanza
   | peer_group_assignment_rb_stanza
   | peer_group_creation_rb_stanza
   | router_id_rb_stanza
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
    agg_advertise_map
    | agg_as_set
    | agg_attribute_map
    | agg_summary_only
    | agg_suppress_map
  )* NEWLINE
;

agg_advertise_map: ADVERTISE_MAP adv_map = variable;

agg_as_set: AS_SET;

agg_attribute_map: ATTRIBUTE_MAP att_map = variable;

agg_summary_only: SUMMARY_ONLY;

agg_suppress_map: SUPPRESS_MAP sup_map = variable;

additional_paths_rb_stanza
:
   BGP ADDITIONAL_PATHS
   (
      INSTALL
      | SELECT ALL
      | SEND RECEIVE?
      | RECEIVE SEND?
   ) NEWLINE
;

advertise_bgp_tail
:
   ADVERTISE ADDITIONAL_PATHS ALL NEWLINE
;

advertise_map_bgp_tail
:
  ADVERTISE_MAP am_name = variable EXIST_MAP em_name = variable NEWLINE
;

allowas_in_bgp_tail
:
   ALLOWAS_IN
   (
      num = dec
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

bgp_af_import
:
   IMPORT bgp_af_import_path
;

bgp_af_import_path
:
   PATH
   (
      bgp_afip_limit
      | bgp_afip_selection
   )
;

bgp_afip_limit
:
   // No effect on Batfish models, just BGP convergence time
   LIMIT num = dec NEWLINE
;

bgp_afip_selection
:
   // No effect on Batfish models, just BGP convergence time
   SELECTION
   (
      ALL
      | BESTPATH STRICT?
      | MULTIPATH STRICT?
   ) NEWLINE
;

//confederations are not currently implemented
//not putting this under null so we can warn the user

bgp_confederation_rb_stanza
:
   BGP CONFEDERATION
   (
     bgp_conf_identifier_rb_stanza
     | bgp_conf_peers_rb_stanza
   )
;

bgp_conf_identifier_rb_stanza
:
  IDENTIFIER id = bgp_asn NEWLINE
;

bgp_conf_peers_rb_stanza
:
  PEERS peers += bgp_asn+ NEWLINE
;

bgp_enforce_first_as_stanza
:
   BGP ENFORCE_FIRST_AS NEWLINE
;

bgp_listen_range_rb_stanza
:
   BGP LISTEN RANGE
   (
      IP_PREFIX
      | IPV6_PREFIX
   ) PEER_GROUP name = variable
   (
      REMOTE_AS bgp_asn
   )? NEWLINE
;

bgp_maxas_limit_rb_stanza
:
   BGP MAXAS_LIMIT limit = dec NEWLINE
;

bgp_redistribute_internal_rb_stanza
:
   BGP REDISTRIBUTE_INTERNAL NEWLINE
;

bgp_scan_time_bgp_tail
:
   BGP SCAN_TIME secs = dec NEWLINE
;

bgp_tail
:
   activate_bgp_tail
   | advertise_bgp_tail
   | advertise_map_bgp_tail
   | allowas_in_bgp_tail
   | as_override_bgp_tail
   | cluster_id_bgp_tail
   | bgp_scan_time_bgp_tail
   | default_metric_bgp_tail
   | default_originate_bgp_tail
   | default_shutdown_bgp_tail
   | description_bgp_tail
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
   | redistribute_eigrp_bgp_tail
   | redistribute_ospf_bgp_tail
   | redistribute_ospfv3_bgp_tail
   | redistribute_rip_bgp_tail
   | redistribute_static_bgp_tail
   | remove_private_as_bgp_tail
   | route_map_bgp_tail
   | route_reflector_client_bgp_tail
   | router_id_bgp_tail
   | send_community_bgp_tail
   | shutdown_bgp_tail
   | subnet_bgp_tail
   | unsuppress_map_bgp_tail
   | update_source_bgp_tail
   | weight_bgp_tail
;

cluster_id_bgp_tail
:
   CLUSTER_ID
   (
      dec
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
   DEFAULT_METRIC metric = dec NEWLINE
;

default_originate_bgp_tail
:
   DEFAULT_ORIGINATE (ROUTE_MAP map = variable)? NEWLINE
;

default_shutdown_bgp_tail
:
   DEFAULT SHUTDOWN NEWLINE
;

description_bgp_tail
:
   description_line
;

distribute_list_bgp_tail
:
   DISTRIBUTE_LIST  list_name = variable
   (
      IN
      | OUT
   ) NEWLINE
;

ebgp_multihop_bgp_tail
:
   EBGP_MULTIHOP
   (
      hop = dec
   )? NEWLINE
;

filter_list_bgp_tail
:
   FILTER_LIST num = dec
   (
      IN
      | OUT
   ) NEWLINE
;

inherit_peer_policy_bgp_tail
:
   INHERIT PEER_POLICY name = variable_permissive num = dec? NEWLINE
;

inherit_peer_session_bgp_tail
:
   INHERIT PEER_SESSION name = variable_permissive NEWLINE
;

local_as_bgp_tail
:
   LOCAL_AS bgp_asn
   (
      NO_PREPEND
      | REPLACE_AS
   )* NEWLINE
;

maximum_peers_bgp_tail
:
   MAXIMUM_PEERS dec NEWLINE
;

maximum_paths_bgp_tail
:
   MAXIMUM_PATHS
   (
      EBGP
      | IBGP
      | EIBGP
   )? paths = dec
   (
      ECMP ecmp_paths = dec
   )? SELECTIVE? NEWLINE
;

maximum_prefix_bgp_tail
:
   MAXIMUM_PREFIX dec NEWLINE
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
      | null_bgp_tail
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
   NEWLINE
;

network6_bgp_tail
:
   NETWORK prefix = IPV6_PREFIX
   (
      ROUTE_MAP mapname = variable
   )?
   NEWLINE
;

next_hop_self_bgp_tail
:
   NO? NEXT_HOP_SELF NEWLINE
;

no_bgp_asnotation_dot
:
   NO BGP ASNOTATION DOT NEWLINE
;

no_bgp_consistency_checker
:
   NO BGP CONSISTENCY_CHECKER NEWLINE
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
   NO NETWORK null_rest_of_line
;

no_redistribute_connected_rb_stanza
:
   NO REDISTRIBUTE
   (
      CONNECTED
      | DIRECT
   ) null_rest_of_line
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
            AGGREGATE_TIMER
            | ATTRIBUTE_DOWNLOAD
            |
            (
               BESTPATH
               (
                  AS_PATH CONFED
                  | MED
               )
            )
            | CLIENT_TO_CLIENT
            | DAMPENING
            | DEFAULT
            | DETERMINISTIC_MED
            | DYNAMIC_MED_INTERVAL
            | FAST_EXTERNAL_FALLOVER
            | GRACEFUL_RESTART
            | LISTEN LIMIT
            | LOG
            | LOG_NEIGHBOR_CHANGES
            | NEXTHOP
            | NON_DETERMINISTIC_MED
            | REDISTRIBUTE_INTERNAL
            | REFRESH
            | REGEXP
            | ROUTE_MAP_CACHE
            | TRANSPORT
            | UPDATE_DELAY
            | UPDATE_GROUP
         )
      )
      | CAPABILITY
      | CLIENT_TO_CLIENT
      | CONNECT_RETRY
      | DAMPEN
      | DAMPEN_IGP_METRIC
      | DAMPENING
      | DESCRIPTION
      | DISABLE_CONNECTED_CHECK
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
      | UPDATE
      |
      (
         USE
         (
            NEXTHOP_ATTRIBUTE
         )
      )
      | VERSION
   ) null_rest_of_line
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
   REMOTE_AS remote = bgp_asn (ALTERNATE_AS alt_ases += bgp_asn+)? NEWLINE
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
         METRIC metric = dec
      )
   )* NEWLINE
;

redistribute_eigrp_bgp_tail
:
   REDISTRIBUTE EIGRP id = dec (METRIC metric = dec)? (ROUTE_MAP map = variable)? NEWLINE
;

redistribute_ospf_bgp_tail
:
   REDISTRIBUTE OSPF (procname = variable)?
   (
      (
         ROUTE_MAP map = variable
      )
      |
      (
         METRIC metric = dec
      )
      |
      (
         MATCH ospf_route_type*
      )
      |
      (
         VRF vrf = variable
      )
   )* NEWLINE
;

redistribute_ospfv3_bgp_tail
:
   REDISTRIBUTE (OSPFV3 | OSPF3) (procname = variable)?
   (
      (
         ROUTE_MAP map = variable
      )
      |
      (
         METRIC metric = dec
      )
      |
      (
         MATCH ospf_route_type*
      )
      |
      (
         VRF vrf = variable
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
         METRIC metric = dec
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
         METRIC metric = dec
      )
   )* NEWLINE
;

router_bgp_stanza
:
   ROUTER BGP
   (
      procnum = bgp_asn
   )? NEWLINE (
      router_bgp_stanza_tail
   )*
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
   | bgp_confederation_rb_stanza
   | bgp_enforce_first_as_stanza
   | bgp_listen_range_rb_stanza
   | bgp_maxas_limit_rb_stanza
   | bgp_redistribute_internal_rb_stanza
   | bgp_tail
   | cluster_id_rb_stanza
   | compare_routerid_rb_stanza
   | default_information_originate_rb_stanza
   | neighbor_flat_rb_stanza
   | neighbor_group_rb_stanza
   | no_bgp_asnotation_dot
   | no_bgp_consistency_checker
   | no_bgp_enforce_first_as_stanza
   | no_neighbor_activate_rb_stanza
   | no_neighbor_shutdown_rb_stanza
   | no_redistribute_connected_rb_stanza
   | null_no_neighbor_rb_stanza
   | peer_group_assignment_rb_stanza
   | peer_group_creation_rb_stanza
   | router_id_rb_stanza
   | session_group_rb_stanza
   | template_peer_policy_rb_stanza
   | template_peer_session_rb_stanza
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

template_peer_policy_rb_stanza
:
   TEMPLATE PEER_POLICY name = variable_permissive NEWLINE
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
   TEMPLATE PEER_SESSION name = variable_permissive NEWLINE
   (
      bgp_tail
      | remote_as_bgp_tail
   )*
   (
      EXIT_PEER_SESSION NEWLINE
   )?
;

unsuppress_map_bgp_tail
:
    UNSUPPRESS_MAP mapname = variable_permissive NEWLINE
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
   WEIGHT weight = dec NEWLINE
;
