parser grammar FlatJuniper_policy_options;

import FlatJuniper_common;

options {
   tokenVocab = FlatJuniperLexer;
}

base_community_regex
:
   ~( COLON | NEWLINE )+ COLON ~( COLON | NEWLINE )+
;

base_extended_community_regex
:
   ~( COLON | NEWLINE )+ COLON ~( COLON | NEWLINE )+ COLON ~( COLON | NEWLINE
   )+
;

community_regex
:
   (
      base_community_regex PIPE
   )* base_community_regex
;

extended_community_regex
:
   (
      base_extended_community_regex PIPE
   )* base_extended_community_regex
;

invalid_community_regex
:
   ~NEWLINE*
;

metric_expression
:
   (
      METRIC
      | METRIC2
   ) MULTIPLIER multiplier = DEC
   (
      OFFSET offset = DEC
   )?
;

po_as_path
:
   AS_PATH name = variable regex = AS_PATH_REGEX
;

po_community
:
   COMMUNITY name = variable
   (
      poc_invert_match
      | poc_members
   )
;

po_condition
:
   CONDITION null_filler
;

po_policy_statement
:
   POLICY_STATEMENT
   (
      WILDCARD
      | name = variable
   )
   (
      pops_term
      | pops_common
   )
;

po_prefix_list
:
   PREFIX_LIST name = variable
   (
      apply
      | poplt_apply_path
      | poplt_network
      | poplt_network6
   )
;

poc_invert_match
:
   INVERT_MATCH
;

poc_members
:
   MEMBERS
   (
      extended_community
      | standard_community
      // community_regex intentionally on bottom

      | community_regex
      | extended_community_regex
      // invalid_community_regex MUST BE LAST

      | invalid_community_regex
   )
;

poplt_apply_path
:
   APPLY_PATH path = DOUBLE_QUOTED_STRING
;

poplt_ip6
:
   ip6 = IPV6_ADDRESS
;

poplt_network
:
   network = IP_PREFIX
;

poplt_network6
:
   network = IPV6_PREFIX
;

pops_common
:
   apply
   | pops_from
   | pops_then
   | pops_to
;

pops_from
:
   FROM
   (
      popsf_area
      | popsf_as_path
      | popsf_as_path_group
      | popsf_color
      | popsf_community
      | popsf_family
      | popsf_instance
      | popsf_interface
      | popsf_level
      | popsf_local_preference
      | popsf_metric
      | popsf_neighbor
      | popsf_origin
      | popsf_policy
      | popsf_prefix_list
      | popsf_prefix_list_filter
      | popsf_protocol
      | popsf_rib
      | popsf_route_filter
      | popsf_route_type
      | popsf_source_address_filter
      | popsf_tag
   )
;

pops_term
:
   TERM
   (
      WILDCARD
      | name = variable
   ) pops_common
;

pops_then
:
   THEN popst_common
;

pops_to
:
   TO
   (
      popsto_level
      | popsto_rib
   )
;

popsf_area
:
   AREA area = IP_ADDRESS
;

popsf_as_path
:
   AS_PATH
   (
      name = variable
   )?
;

popsf_as_path_group
:
   AS_PATH_GROUP name = variable
;

popsf_color
:
   COLOR color = DEC
;

popsf_community
:
   COMMUNITY name = variable
;

popsf_family
:
   FAMILY
   (
      INET
      | INET6
   )
;

popsf_instance
:
   INSTANCE name = variable
;

popsf_interface
:
   INTERFACE id = interface_id
;

popsf_level
:
   LEVEL DEC
;

popsf_local_preference
:
   LOCAL_PREFERENCE
   (
      localpref = DEC
      | apply_groups
   )
;

popsf_metric
:
   METRIC
   (
      metric = DEC
      | apply_groups
   )
;

popsf_neighbor
:
   NEIGHBOR
   (
      IP_ADDRESS
      | IPV6_ADDRESS
   )
;

popsf_origin
:
   ORIGIN origin_type
;

popsf_policy
:
   POLICY expr = policy_expression
;

popsf_prefix_list
:
   PREFIX_LIST name = variable
;

popsf_prefix_list_filter
:
   PREFIX_LIST_FILTER name = variable
   (
      popsfpl_exact
      | popsfpl_longer
      | popsfpl_orlonger
   )
;

popsf_protocol
:
   PROTOCOL protocol = routing_protocol
;

popsf_rib
:
   RIB name = variable
;

popsf_route_filter
:
   ROUTE_FILTER
   (
      IP_PREFIX
      | IPV6_PREFIX
   ) popsfrf_common then = popsfrf_then?
;

popsf_route_type
:
   ROUTE_TYPE
   (
      EXTERNAL
      | INTERNAL
   )
;

popsf_source_address_filter
:
   SOURCE_ADDRESS_FILTER
   (
      IP_PREFIX
      | IPV6_PREFIX
   ) popsfrf_common
;

popsf_tag
:
   TAG DEC
;

popsfpl_exact
:
   EXACT
;

popsfpl_longer
:
   LONGER
;

popsfpl_orlonger
:
   ORLONGER
;

popsfrf_common
:
   popsfrf_address_mask
   | popsfrf_exact
   | popsfrf_longer
   | popsfrf_orlonger
   | popsfrf_prefix_length_range
   | popsfrf_through
   | popsfrf_upto
;

popsfrf_address_mask
:
   ADDRESS_MASK
   (
      IP_ADDRESS
      | IPV6_ADDRESS
   )
;

popsfrf_exact
:
   EXACT
;

popsfrf_longer
:
   LONGER
;

popsfrf_orlonger
:
   ORLONGER
;

popsfrf_prefix_length_range
:
   PREFIX_LENGTH_RANGE FORWARD_SLASH low = DEC DASH FORWARD_SLASH high = DEC
;

popsfrf_then
:
   popst_common
;

popsfrf_through
:
   THROUGH
   (
      IP_PREFIX
      | IPV6_PREFIX
   )
;

popsfrf_upto
:
   UPTO FORWARD_SLASH high = DEC
;

popst_accept
:
   ACCEPT
;

popst_as_path_expand
:
   AS_PATH_EXPAND LAST_AS COUNT DEC
;

popst_as_path_prepend
:
   AS_PATH_PREPEND
   (
      DEC
      | DOUBLE_QUOTED_STRING
   )
;

popst_color
:
   COLOR
   (
      apply
      | popstc_add_color
      | popstc_color
   )
;

popst_color2
:
   COLOR2
   (
      apply
      | popstc2_add_color
      | popstc2_color
   )
;

popst_common
:
   popst_accept
   | popst_as_path_expand
   | popst_as_path_prepend
   | popst_color
   | popst_color2
   | popst_community_add
   | popst_community_delete
   | popst_community_set
   | popst_cos_next_hop_map
   | popst_default_action_accept
   | popst_default_action_reject
   | popst_external
   | popst_forwarding_class
   | popst_install_nexthop
   | popst_local_preference
   | popst_metric
   | popst_metric_add
   | popst_metric_expression
   | popst_metric_igp
   | popst_metric2
   | popst_metric2_expression
   | popst_next_hop
   | popst_next_hop_self
   | popst_next_policy
   | popst_next_term
   | popst_null
   | popst_origin
   | popst_preference
   | popst_priority
   | popst_reject
   | popst_tag
;

popst_community_add
:
   COMMUNITY ADD name = variable
;

popst_community_delete
:
   COMMUNITY DELETE name = variable
;

popst_community_set
:
   COMMUNITY SET name = variable
;

popst_cos_next_hop_map
:
   COS_NEXT_HOP_MAP name = variable
;

popst_default_action_accept
:
   DEFAULT_ACTION ACCEPT
;

popst_default_action_reject
:
   DEFAULT_ACTION REJECT
;

popst_external
:
   EXTERNAL TYPE DEC
;

popst_forwarding_class
:
   FORWARDING_CLASS variable
;

popst_install_nexthop
:
   INSTALL_NEXTHOP null_filler
;

popst_local_preference
:
   LOCAL_PREFERENCE
   (
      localpref = DEC
      | apply_groups
   )
;

popst_metric
:
   METRIC
   (
      metric = DEC
      | apply_groups
   )
;

popst_metric_add
:
   METRIC ADD metric = DEC
;

popst_metric2
:
   METRIC2 metric2 = DEC
;

popst_metric_expression
:
   METRIC EXPRESSION metric_expression
;

popst_metric_igp
:
   METRIC IGP offset = DEC?
;

popst_metric2_expression
:
   METRIC2 EXPRESSION metric_expression
;

popst_next_hop
:
   NEXT_HOP
   (
      IP_ADDRESS
      | IPV6_ADDRESS
      | PEER_ADDRESS
   )
;

popst_next_hop_self
:
   NEXT_HOP SELF
;

popst_next_policy
:
   NEXT POLICY
;

popst_next_term
:
   NEXT TERM
;

popst_null
:
   LOAD_BALANCE null_filler
;

popst_origin
:
   ORIGIN (EGP | IGP | INCOMPLETE)
;

popst_preference
:
   PREFERENCE preference = DEC
;

popst_priority
:
   PRIORITY
   (
      HIGH
      | LOW
   )
;

popst_reject
:
   REJECT
;

popst_tag
:
   TAG DEC
;

popstc_add_color
:
   ADD color = DEC
;

popstc_color
:
   color = DEC
;

popstc2_add_color
:
   ADD color2 = DEC
;

popstc2_color
:
   color2 = DEC
;

popsto_level
:
   LEVEL DEC
;

popsto_rib
:
   RIB variable
;

s_policy_options
:
   POLICY_OPTIONS
   (
      apply
      | po_as_path
      | po_community
      | po_condition
      | po_policy_statement
      | po_prefix_list
   )
;
