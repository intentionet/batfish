parser grammar FlatJuniper_routing_instances;

import
FlatJuniper_common, FlatJuniper_forwarding_options, FlatJuniper_protocols, FlatJuniper_snmp;

options {
   tokenVocab = FlatJuniperLexer;
}

ri_common
:
   apply
   | ri_description
   | s_forwarding_options
   | s_routing_options
;

ri_description
:
   description
;

ri_instance_type
:
   INSTANCE_TYPE
   (
      FORWARDING
      | L2VPN
      | VIRTUAL_ROUTER
      | VIRTUAL_SWITCH
      | VRF
   )
;

ri_interface
:
   INTERFACE id = interface_id
;

ri_named_routing_instance
:
   name = variable
   (
      ri_common
      | ri_instance_type
      | ri_interface
      | ri_null
      | ri_protocols
      | ri_route_distinguisher
      | ri_snmp
      | ri_vrf_export
      | ri_vrf_import
      | ri_vrf_table_label
      | ri_vrf_target
   )
;

ri_null
:
   (
      BRIDGE_DOMAINS
      | CHASSIS
      | CLASS_OF_SERVICE
      | EVENT_OPTIONS
      | PROVIDER_TUNNEL
      | SERVICES
   ) null_filler
;

ri_protocols
:
   s_protocols
;

ri_route_distinguisher
:
   ROUTE_DISTINGUISHER
   (
      DEC
      | IP_ADDRESS
   ) COLON DEC
;

ri_snmp
:
   s_snmp
;

ri_vrf_export
:
   VRF_EXPORT name = variable
;

ri_vrf_import
:
   VRF_IMPORT name = variable
;

ri_vrf_table_label
:
   VRF_TABLE_LABEL
;

ri_vrf_target
:
   VRF_TARGET
   (
      riv_community
      | riv_export
      | riv_import
   )
;

riv_community
:
   extended_community
;

riv_export
:
   EXPORT extended_community
;

riv_import
:
   IMPORT extended_community
;

ro_aggregate
:
   AGGREGATE ROUTE
   (
      prefix = IP_PREFIX
      | prefix6 = IPV6_PREFIX
   )
   (
      apply
      | roa_as_path
      | roa_community
      | roa_preference
      | roa_tag
   )
;

ro_auto_export
:
   AUTO_EXPORT
;

ro_autonomous_system
:
   AUTONOMOUS_SYSTEM as = DEC?
   (
      apply
      |
      (
         roas_asdot_notation
         | roas_loops
      )*
   )
;

ro_bmp
:
   BMP
   (
      rob_station_address
      | rob_station_port
   )
;

ro_forwarding_table
:
   FORWARDING_TABLE
   (
      rof_export
      | rof_no_ecmp_fast_reroute
      | rof_null
   )
;

ro_generate
:
   GENERATE ROUTE
   (
      IP_PREFIX
      | IPV6_PREFIX
   )
   (
      rog_discard
      | rog_metric
      | rog_policy
   )
;

ro_interface_routes
:
   INTERFACE_ROUTES
   (
      roi_family
      | roi_rib_group
   )
;

ro_martians
:
   MARTIANS null_filler
;

ro_null
:
   (
      GRACEFUL_RESTART
      | MULTICAST
      | MULTIPATH
      | NONSTOP_ROUTING
      | OPTIONS
      | PPM
      | RESOLUTION
      | TRACEOPTIONS
   ) null_filler
;

ro_rib
:
   RIB name = VARIABLE
   (
      apply
      | ro_aggregate
      | ro_generate
      | ro_static
   )
;

ro_rib_groups
:
   RIB_GROUPS name = variable
   (
      ror_export_rib
      | ror_import_policy
      | ror_import_rib
   )
;

ro_router_id
:
   ROUTER_ID id = IP_ADDRESS
;

ro_srlg
:
   SRLG name = variable
   (
      roslrg_srlg_cost
      | roslrg_srlg_value
   )
;

ro_static
:
   STATIC
   (
      ros_rib_group
      | ros_route
   )
;

roa_as_path
:
   AS_PATH?
   (
      roaa_aggregator
      | roaa_origin
      | roaa_path
   )
;

roa_community
:
   COMMUNITY community = COMMUNITY_LITERAL
;

roa_preference
:
   PREFERENCE preference = DEC
;

roa_tag
:
   TAG tag = DEC
;

roaa_aggregator
:
   AGGREGATOR as = DEC ip = IP_ADDRESS
;

roaa_origin
:
   ORIGIN IGP
;

roaa_path
:
   PATH path = as_path_expr
;

roas_asdot_notation
:
   ASDOT_NOTATION
;

roas_loops
:
   LOOPS DEC
;

rob_station_address
:
   STATION_ADDRESS IP_ADDRESS
;

rob_station_port
:
   STATION_PORT DEC
;

rof_export
:
   EXPORT name = variable
;

rof_no_ecmp_fast_reroute
:
   NO_ECMP_FAST_REROUTE
;

rof_null
:
   (
      INDIRECT_NEXT_HOP
      | INDIRECT_NEXT_HOP_CHANGE_ACKNOWLEDGEMENTS
   ) null_filler
;

rog_discard
:
   DISCARD
;

rog_metric
:
   METRIC metric = DEC
;

rog_policy
:
   POLICY policy = variable
;

roi_family
:
   FAMILY
   (
      roif_inet
      | roif_null
   )
;

roi_rib_group
:
   RIB_GROUP
   (
      roir_inet
      | roir_null
   )
;

roif_inet
:
   INET
   (
      roifi_export
   )
;

roif_null
:
   INET6 null_filler
;

roifi_export
:
   EXPORT
   (
      roifie_lan
      | roifie_point_to_point
   )
;

roifie_lan
:
   LAN
;

roifie_point_to_point
:
   POINT_TO_POINT
;

roir_inet
:
   INET name = variable
;

roir_null
:
   INET6 null_filler
;

ror_export_rib
:
   EXPORT_RIB rib = variable
;

ror_import_policy
:
   IMPORT_POLICY name = variable
;

ror_import_rib
:
   IMPORT_RIB rib = variable
;

ros_rib_group
:
   RIB_GROUP name = variable
;

ros_route
:
   ROUTE
   (
      IP_PREFIX
      | IPV6_PREFIX
   )
   (
      rosr_common
      | rosr_qualified_next_hop
   )
;

roslrg_srlg_cost
:
   SRLG_COST cost = DEC
;

roslrg_srlg_value
:
   SRLG_VALUE value = DEC
;

rosr_active
:
   ACTIVE
;

rosr_as_path
:
   AS_PATH PATH
   (
      path += DEC
   )+
;

rosr_common
:
   rosr_active
   | rosr_as_path
   | rosr_community
   | rosr_discard
   | rosr_install
   | rosr_no_install
   | rosr_metric
   | rosr_next_hop
   | rosr_next_table
   | rosr_no_readvertise
   | rosr_no_retain
   | rosr_passive
   | rosr_preference
   | rosr_readvertise
   | rosr_reject
   | rosr_resolve
   | rosr_retain
   | rosr_tag
;

rosr_community
:
   COMMUNITY standard_community
;

rosr_discard
:
   DISCARD
;

rosr_install
:
   INSTALL
;

rosr_metric
:
   METRIC metric = DEC
   (
      TYPE DEC
   )?
;

rosr_next_hop
:
   NEXT_HOP
   (
      IP_ADDRESS
      | IPV6_ADDRESS
      | interface_id
   )
;

rosr_next_table
:
   NEXT_TABLE name = variable
;

rosr_no_install
:
   NO_INSTALL
;

rosr_no_readvertise
:
   NO_READVERTISE
;

rosr_no_retain
:
   NO_RETAIN
;

rosr_passive
:
   PASSIVE
;

rosr_preference
:
   PREFERENCE pref = DEC
;

rosr_qualified_next_hop
:
   QUALIFIED_NEXT_HOP nexthop = IP_ADDRESS rosr_common?
;

rosr_readvertise
:
   READVERTISE
;

rosr_reject
:
   REJECT
;

rosr_resolve
:
   RESOLVE
;

rosr_retain
:
   RETAIN
;

rosr_tag
:
   TAG tag = DEC
;

s_routing_instances
:
   ROUTING_INSTANCES
   (
      ri_common
      | ri_named_routing_instance
   )
;

s_routing_options
:
   ROUTING_OPTIONS
   (
      ro_aggregate
      | ro_auto_export
      | ro_autonomous_system
      | ro_bmp
      | ro_forwarding_table
      | ro_generate
      | ro_interface_routes
      | ro_martians
      | ro_null
      | ro_rib
      | ro_rib_groups
      | ro_router_id
      | ro_srlg
      | ro_static
   )
;
