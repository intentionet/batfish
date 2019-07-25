parser grammar Cisco_routemap;

import Cisco_common;

options {
   tokenVocab = CiscoLexer;
}

apply_rp_stanza
:
   APPLY name = variable
   (
      PAREN_LEFT varlist = route_policy_params_list PAREN_RIGHT
   )? NEWLINE
;

as_expr
:
   DEC
   | AUTO
   | RP_VARIABLE
;

as_path_set_elem
:
   IOS_REGEX AS_PATH_SET_REGEX
;

as_path_set_expr
:
   inline = as_path_set_inline
   | rpvar = RP_VARIABLE
   | named = variable
;

as_path_set_inline
:
   PAREN_LEFT elems += as_path_set_elem
   (
      COMMA elems += as_path_set_elem
   )* PAREN_RIGHT
;

as_range_expr
:
   SINGLE_QUOTE
   (
      subranges += rp_subrange
   )+ SINGLE_QUOTE EXACT?
;

boolean_and_rp_stanza
:
   boolean_not_rp_stanza
   | boolean_and_rp_stanza AND boolean_not_rp_stanza
;

boolean_apply_rp_stanza
:
   APPLY name = variable
   (
      PAREN_LEFT varlist = route_policy_params_list PAREN_RIGHT
   )?
;

boolean_as_path_in_rp_stanza
:
   AS_PATH IN expr = as_path_set_expr
;

boolean_as_path_is_local_rp_stanza
:
   AS_PATH IS_LOCAL
;

boolean_as_path_neighbor_is_rp_stanza
:
   AS_PATH NEIGHBOR_IS as_range_expr
;

boolean_as_path_originates_from_rp_stanza
:
   AS_PATH ORIGINATES_FROM as_range_expr
;

boolean_as_path_passes_through_rp_stanza
:
   AS_PATH PASSES_THROUGH as_range_expr
;

boolean_community_matches_any_rp_stanza
:
   COMMUNITY MATCHES_ANY rp_community_set
;

boolean_community_matches_every_rp_stanza
:
   COMMUNITY MATCHES_EVERY rp_community_set
;

boolean_destination_rp_stanza
:
   DESTINATION IN rp_prefix_set
;

boolean_local_preference_rp_stanza
:
   LOCAL_PREFERENCE int_comp rhs = int_expr
;

boolean_med_rp_stanza
:
   MED int_comp rhs = int_expr
;

boolean_next_hop_in_rp_stanza
:
   NEXT_HOP IN rp_prefix_set
;

boolean_not_rp_stanza
:
   boolean_simple_rp_stanza
   | NOT boolean_simple_rp_stanza
;

boolean_rib_has_route_rp_stanza
:
   RIB_HAS_ROUTE IN rp_prefix_set
;

boolean_route_type_is_rp_stanza
:
   ROUTE_TYPE IS type = rp_route_type
;

boolean_rp_stanza
:
   boolean_and_rp_stanza
   | boolean_rp_stanza OR boolean_and_rp_stanza
;

boolean_simple_rp_stanza
:
   PAREN_LEFT boolean_rp_stanza PAREN_RIGHT
   | boolean_apply_rp_stanza
   | boolean_as_path_in_rp_stanza
   | boolean_as_path_is_local_rp_stanza
   | boolean_as_path_neighbor_is_rp_stanza
   | boolean_as_path_originates_from_rp_stanza
   | boolean_as_path_passes_through_rp_stanza
   | boolean_community_matches_any_rp_stanza
   | boolean_community_matches_every_rp_stanza
   | boolean_destination_rp_stanza
   | boolean_local_preference_rp_stanza
   | boolean_med_rp_stanza
   | boolean_next_hop_in_rp_stanza
   | boolean_rib_has_route_rp_stanza
   | boolean_route_type_is_rp_stanza
   | boolean_tag_is_rp_stanza
;

boolean_tag_is_rp_stanza
:
   TAG int_comp int_expr
;

continue_rm_stanza
:
   CONTINUE DEC? NEWLINE
;

delete_rp_stanza
:
   DELETE COMMUNITY
   (
      ALL
      | NOT? IN rp_community_set
   ) NEWLINE
;

disposition_rp_stanza
:
   (
      DONE
      | DROP
      | PASS
      | UNSUPPRESS_ROUTE
   ) NEWLINE
;

elseif_rp_stanza
:
   ELSEIF boolean_rp_stanza THEN NEWLINE rp_stanza*
;

else_rp_stanza
:
   ELSE NEWLINE rp_stanza*
;

if_rp_stanza
:
   IF boolean_rp_stanza THEN NEWLINE rp_stanza* elseif_rp_stanza*
   else_rp_stanza?
   (
      ENDIF
      | EXIT
   ) NEWLINE
;

int_comp
:
   EQ
   | IS
   | GE
   | LE
;

ip_policy_list_stanza
:
   IP POLICY_LIST name = variable access_list_action NEWLINE match_rm_stanza*
;

isis_level_expr
:
   isis_level
   | RP_VARIABLE
;

match_as_number_rm_stanza
:
   AS_NUMBER num = DEC NEWLINE
;

match_as_path_access_list_rm_stanza
:
   AS_PATH
   (
      name_list += variable
   )+ NEWLINE
;

match_as_rm_stanza
:
   MATCH AS num = DEC NEWLINE
;

match_community_list_rm_stanza
:
   COMMUNITY
   (
      name_list += variable
   )+ NEWLINE
;

match_extcommunity_rm_stanza
:
   EXTCOMMUNITY
   (
      name_list += variable
   )+ NEWLINE
;

match_interface_rm_stanza
:
   INTERFACE interface_name+ NEWLINE
;

match_ip_access_list_rm_stanza
:
   IP ADDRESS
   (
      name_list += variable_access_list
   )+ NEWLINE
;

match_ipv6_access_list_rm_stanza
:
   IPV6 ADDRESS
   (
      name_list += variable_access_list
   )+ NEWLINE
;

match_ip_multicast_rm_stanza
:
   IP MULTICAST null_rest_of_line
;

match_ip_next_hop_rm_stanza_null
:
   IP NEXT_HOP
   (
      NULL
   ) NEWLINE
;

match_ip_prefix_list_rm_stanza
:
   IP ADDRESS IP? PREFIX_LIST
   (
      name_list += variable
   )+ NEWLINE
;

match_ip_route_source_rm_stanza
:
   IP ROUTE_SOURCE src = DEC NEWLINE
;

match_ipv6_prefix_list_rm_stanza
:
   IPV6 ADDRESS PREFIX_LIST
   (
      name_list += variable
   )+ NEWLINE
;

match_length_rm_stanza
:
   LENGTH null_rest_of_line
;

match_policy_list_rm_stanza
:
   POLICY_LIST
   (
      name_list += variable
   )+ NEWLINE
;

match_route_type_rm_stanza
:
   ROUTE_TYPE variable NEWLINE
;

match_source_protocol_rm_stanza
:
   SOURCE_PROTOCOL
   (
       BGP (bgpasn = bgp_asn)?
       | CONNECTED
       | ISIS
       | OSPF (area = DEC)?
       | RIP
       | STATIC
   )+ NEWLINE
;

match_rm_stanza
:
   MATCH
   (
      match_as_number_rm_stanza
      | match_as_path_access_list_rm_stanza
      | match_as_rm_stanza
      | match_community_list_rm_stanza
      | match_extcommunity_rm_stanza
      | match_interface_rm_stanza
      | match_ip_access_list_rm_stanza
      | match_ip_multicast_rm_stanza
      | match_ip_next_hop_rm_stanza_null
      | match_ip_prefix_list_rm_stanza
      | match_ip_route_source_rm_stanza
      | match_ipv6_access_list_rm_stanza
      | match_ipv6_prefix_list_rm_stanza
      | match_length_rm_stanza
      | match_policy_list_rm_stanza
      | match_route_type_rm_stanza
      | match_source_protocol_rm_stanza
      | match_tag_rm_stanza
   )
;

match_tag_rm_stanza
:
   TAG
   (
      tag_list += DEC
   )+ NEWLINE
;

no_route_map_stanza
:
   NO ROUTE_MAP name = variable NEWLINE
;

null_rm_stanza
:
   NO?
   (
      DESCRIPTION
      | SUB_ROUTE_MAP
   ) null_rest_of_line
;

origin_expr
:
   origin_expr_literal
   | RP_VARIABLE
;

origin_expr_literal
:
   (
      EGP bgp_asn
   )
   | IGP
   | INCOMPLETE
;

prepend_as_path_rp_stanza
:
   PREPEND AS_PATH as = as_expr
   (
      number = int_expr
   )? NEWLINE
;

rm_stanza
:
   continue_rm_stanza
   | match_rm_stanza
   | null_rm_stanza
   | set_rm_stanza
;

route_map_stanza
:
   ROUTE_MAP name = variable rmt = access_list_action num = DEC NEWLINE
   rm_stanza*
;

route_policy_stanza
:
   ROUTE_POLICY name = variable
   (
      PAREN_LEFT varlist = route_policy_params_list PAREN_RIGHT
   )? NEWLINE
   (
         stanzas += rp_stanza
   )*
   END_POLICY NEWLINE
;

route_policy_params_list
:
   params_list += variable
   (
      COMMA params_list += variable
   )*
;

rp_community_set
:
   name = variable
   | PAREN_LEFT elems += community_set_elem
   (
      COMMA elems += community_set_elem
   )* PAREN_RIGHT
;

rp_isis_metric_type
:
   EXTERNAL
   | INTERNAL
   | RIB_METRIC_AS_EXTERNAL
   | RIB_METRIC_AS_INTERNAL
;

rp_metric_type
:
   rp_isis_metric_type
   | rp_ospf_metric_type
   | RP_VARIABLE
;

rp_ospf_metric_type
:
   TYPE_1
   | TYPE_2
;

rp_prefix_set
:
   name = variable
   | PAREN_LEFT elems += prefix_set_elem
   (
      COMMA elems += prefix_set_elem
   )* PAREN_RIGHT
;

rp_route_type
:
   LOCAL
   | INTERAREA
   | INTERNAL
   | LEVEL_1
   | LEVEL_1_2
   | LEVEL_2
   | LOCAL
   | OSPF_EXTERNAL_TYPE_1
   | OSPF_EXTERNAL_TYPE_2
   | OSPF_INTER_AREA
   | OSPF_INTRA_AREA
   | OSPF_NSSA_TYPE_1
   | OSPF_NSSA_TYPE_2
   | RP_VARIABLE
   | TYPE_1
   | TYPE_2
;

rp_stanza
:
   apply_rp_stanza
   | delete_rp_stanza
   | disposition_rp_stanza
   |
   (
      hash_comment NEWLINE
   )
   | if_rp_stanza
   | set_rp_stanza
;

set_as_path_prepend_rm_stanza
:
   SET AS_PATH PREPEND LAST_AS?
   (
      as_list += as_expr
   )+ NEWLINE
;

set_as_path_tag_rm_stanza
:
   SET AS_PATH TAG NEWLINE
;

set_comm_list_delete_rm_stanza
:
   SET COMM_LIST name = variable DELETE NEWLINE
;

set_community_additive_rm_stanza
:
   SET COMMUNITY
   (
      (
         ADD
         (
            communities += community
         )+
      )
      |
      (
         (
            communities += community
         )+ ADDITIVE
      )
   ) NEWLINE
;

set_community_list_additive_rm_stanza
:
   SET COMMUNITY COMMUNITY_LIST
   (
      comm_lists += variable
   )+ ADDITIVE NEWLINE
;

set_community_list_rm_stanza
:
   SET COMMUNITY COMMUNITY_LIST
   (
      comm_lists += variable
   )+ NEWLINE
;

set_community_none_rm_stanza
:
   SET COMMUNITY NONE NEWLINE
;

set_community_rm_stanza
:
   SET COMMUNITY
   (
      communities += community
   )+ NEWLINE
;

set_community_rp_stanza
:
   SET COMMUNITY rp_community_set ADDITIVE? NEWLINE
;

set_extcomm_list_rm_stanza
:
   SET EXTCOMM_LIST
   (
      comm_list += community
   )+ DELETE NEWLINE
;

set_extcommunity_rm_stanza
:
   SET EXTCOMMUNITY
   (
      COST
      | RT
   ) extended_community NEWLINE
;

set_interface_rm_stanza
:
   SET INTERFACE null_rest_of_line
;

set_ip_default_nexthop_stanza
:
   SET IP DEFAULT NEXT_HOP nhip = IP_ADDRESS NEWLINE
;

set_ip_df_rm_stanza
:
   SET IP DF null_rest_of_line
;

set_ip_precedence_stanza
:
   SET IP PRECEDENCE
   (
      val = DIGIT
      | CRITICAL
      | FLASH
      | FLASH_OVERRIDE
      | IMMEDIATE
      | INTERNET
      | NETWORK
      | PRIORITY
      | ROUTE
   ) NEWLINE
;

set_ipv6_rm_stanza
:
   SET IPV6 null_rest_of_line
;

set_isis_metric_rp_stanza
:
   SET ISIS_METRIC int_expr NEWLINE
;

set_level_rp_stanza
:
   SET LEVEL isis_level_expr NEWLINE
;

set_local_preference_rm_stanza
:
   SET LOCAL_PREFERENCE pref = int_expr NEWLINE
;

set_local_preference_rp_stanza
:
   SET LOCAL_PREFERENCE pref = int_expr NEWLINE
;

set_med_rp_stanza
:
   SET MED med = int_expr NEWLINE
;

set_metric_eigrp_rm_stanza
:
   SET METRIC bw = DEC load = DEC delay = DEC reliability = DEC mtu = DEC NEWLINE
;

set_metric_rm_stanza
:
   SET METRIC metric = int_expr NEWLINE
;

set_metric_type_rm_stanza
:
   SET METRIC_TYPE type = variable NEWLINE
;

set_metric_type_rp_stanza
:
   SET METRIC_TYPE type = rp_metric_type NEWLINE
;

set_mpls_label_rm_stanza
:
   SET MPLS_LABEL NEWLINE
;

set_next_hop_peer_address_stanza
:
   SET IP NEXT_HOP PEER_ADDRESS NEWLINE
;

set_next_hop_rm_stanza
:
   SET IP? NEXT_HOP
   (
      nexthop_list += IP_ADDRESS
   )+ NEWLINE
;

set_next_hop_rp_stanza
:
   SET NEXT_HOP
   (
      DISCARD
      | IP_ADDRESS
      | IPV6_ADDRESS
      | PEER_ADDRESS
   ) DESTINATION_VRF? NEWLINE
;

set_next_hop_self_rp_stanza
:
   SET NEXT_HOP SELF NEWLINE
;

set_nlri_rm_stanza_null
:
   SET NLRI
   (
      UNICAST
      | MULTICAST
   )+ NEWLINE
;

set_origin_rm_stanza
:
   SET ORIGIN origin_expr_literal NEWLINE
;

set_origin_rp_stanza
:
   SET ORIGIN origin_expr NEWLINE
;

set_path_selection_rp_stanza
:
   SET PATH_SELECTION null_rest_of_line
;

set_tag_rm_stanza
:
   SET TAG tag = DEC NEWLINE
;

set_tag_rp_stanza
:
   SET TAG tag = int_expr NEWLINE
;

set_traffic_index_rm_stanza_null
:
   SET TRAFFIC_INDEX index = DEC NEWLINE
;

set_weight_rm_stanza
:
   SET WEIGHT weight = DEC NEWLINE
;

set_weight_rp_stanza
:
   SET WEIGHT weight = int_expr NEWLINE
;

set_rm_stanza
:
   set_as_path_prepend_rm_stanza
   | set_as_path_tag_rm_stanza
   | set_comm_list_delete_rm_stanza
   | set_community_rm_stanza
   | set_community_additive_rm_stanza
   | set_community_list_additive_rm_stanza
   | set_community_list_rm_stanza
   | set_community_none_rm_stanza
   | set_extcomm_list_rm_stanza
   | set_extcommunity_rm_stanza
   | set_interface_rm_stanza
   | set_ip_default_nexthop_stanza
   | set_ip_df_rm_stanza
   | set_ip_precedence_stanza
   | set_ipv6_rm_stanza
   | set_local_preference_rm_stanza
   | set_metric_eigrp_rm_stanza
   | set_metric_rm_stanza
   | set_metric_type_rm_stanza
   | set_mpls_label_rm_stanza
   | set_next_hop_peer_address_stanza
   | set_next_hop_rm_stanza
   | set_nlri_rm_stanza_null
   | set_origin_rm_stanza
   | set_tag_rm_stanza
   | set_traffic_index_rm_stanza_null
   | set_weight_rm_stanza
;

set_rp_stanza
:
   prepend_as_path_rp_stanza
   | set_community_rp_stanza
   | set_isis_metric_rp_stanza
   | set_level_rp_stanza
   | set_local_preference_rp_stanza
   | set_med_rp_stanza
   | set_metric_type_rp_stanza
   | set_next_hop_rp_stanza
   | set_next_hop_self_rp_stanza
   | set_origin_rp_stanza
   | set_path_selection_rp_stanza
   | set_tag_rp_stanza
   | set_weight_rp_stanza
;

variable_access_list
:
   ~( IP | IPV6 | NEWLINE | PREFIX_LIST )
;
