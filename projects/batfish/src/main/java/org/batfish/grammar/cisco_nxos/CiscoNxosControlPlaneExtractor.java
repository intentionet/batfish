package org.batfish.grammar.cisco_nxos;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.IpWildcard.ipWithWildcardMask;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.getCanonicalInterfaceNamePrefix;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.BGP_TEMPLATE_PEER;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.BGP_TEMPLATE_PEER_POLICY;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.BGP_TEMPLATE_PEER_SESSION;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.INTERFACE;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.IPV6_PREFIX_LIST;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.IP_ACCESS_LIST;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.IP_AS_PATH_ACCESS_LIST;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.IP_COMMUNITY_LIST_STANDARD;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.IP_PREFIX_LIST;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.NVE;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.OBJECT_GROUP_IP_ADDRESS;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.PORT_CHANNEL;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.ROUTER_OSPF;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.ROUTE_MAP_ENTRY;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.VLAN;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.VRF;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_ADDITIONAL_PATHS_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_ADVERTISE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_ATTRIBUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_DAMPENING_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_DEFAULT_ORIGINATE_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_EXIST_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_INJECT_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_L2VPN_EVPN_RETAIN_ROUTE_TARGET_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR6_FILTER_LIST_IN;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR6_FILTER_LIST_OUT;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR6_PREFIX_LIST_IN;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR6_PREFIX_LIST_OUT;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_ADVERTISE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_EXIST_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_FILTER_LIST_IN;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_FILTER_LIST_OUT;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_INHERIT_PEER;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_INHERIT_PEER_POLICY;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_INHERIT_PEER_SESSION;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_NON_EXIST_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_PREFIX_LIST_IN;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_PREFIX_LIST_OUT;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_ROUTE_MAP_IN;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_ROUTE_MAP_OUT;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEIGHBOR_UPDATE_SOURCE;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NETWORK6_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NETWORK_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_NEXTHOP_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_DIRECT_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_EIGRP_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_ISIS_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_LISP_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_OSPFV3_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_OSPF_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_OSPF_SOURCE_TAG;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_RIP_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_REDISTRIBUTE_STATIC_ROUTE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_SUPPRESS_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_TABLE_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.BGP_UNSUPPRESS_MAP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.INTERFACE_CHANNEL_GROUP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.INTERFACE_IP_POLICY;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.INTERFACE_SELF_REFERENCE;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.INTERFACE_VLAN;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.INTERFACE_VRF_MEMBER;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.IP_ACCESS_LIST_DESTINATION_ADDRGROUP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.IP_ACCESS_LIST_SOURCE_ADDRGROUP;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_INTERFACE;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_VRF;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.NVE_SELF_REFERENCE;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.NVE_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS_PREFIX_LIST;
import static org.batfish.representation.cisco_nxos.Interface.VLAN_RANGE;
import static org.batfish.representation.cisco_nxos.Interface.newNonVlanInterface;
import static org.batfish.representation.cisco_nxos.Interface.newVlanInterface;
import static org.batfish.representation.cisco_nxos.StaticRoute.STATIC_ROUTE_PREFERENCE_RANGE;
import static org.batfish.representation.cisco_nxos.StaticRoute.STATIC_ROUTE_TRACK_RANGE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.Table;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.common.WellKnownCommunity;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acl_fragmentsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acl_lineContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acll_actionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acll_remarkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3_address_specContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3_dst_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3_fragmentsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3_protocol_specContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3_src_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3o_dscpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3o_logContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3o_packet_lengthContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3o_packet_length_specContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3o_precedenceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal3o_ttlContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4_icmpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4_tcpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4_udpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4icmp_optionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4igmp_optionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcp_destination_portContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcp_port_specContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcp_port_spec_literalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcp_port_spec_port_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcp_source_portContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcpo_establishedContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcpo_flagsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4tcpo_tcp_flags_maskContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4udp_destination_portContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4udp_port_specContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4udp_port_spec_literalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4udp_port_spec_port_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Acllal4udp_source_portContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.As_path_regexContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Banner_execContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Banner_motdContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Bgp_asnContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Bgp_distanceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Both_export_importContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Channel_idContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Cisco_nxos_configurationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Dscp_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Dscp_specContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ebgp_multihop_ttlContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ev_vniContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Evv_rdContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Evv_route_targetContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_bandwidthContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_channel_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_delayContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_descriptionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_encapsulationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_ip_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_ip_policyContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_mtuContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_no_autostateContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_no_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_no_switchportContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_speed_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_accessContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_mode_accessContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_mode_dot1q_tunnelContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_mode_fex_fabricContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_mode_trunkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_monitorContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_switchportContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_trunk_allowedContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_trunk_nativeContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_vrf_memberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Icl_standardContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ih_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ih_versionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ihd_reloadContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ihg_ipContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ihg_preemptContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ihg_priorityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ihg_timersContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ihg_trackContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ihgam_key_chainContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Iipo_bfdContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Iipo_costContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Iipo_dead_intervalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Iipo_hello_intervalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Iipo_networkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Iipo_passive_interfaceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Iipr_ospfContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Il_min_linksContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Inherit_sequence_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Interface_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Interface_bandwidth_kbpsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Interface_descriptionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Interface_mtuContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Interface_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Interface_prefixContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_access_listContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_access_list_line_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_access_list_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_as_path_access_listContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_as_path_access_list_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_as_path_access_list_seqContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_community_list_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_community_list_seqContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_prefixContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_prefix_listContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_prefix_list_descriptionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_prefix_list_line_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_prefix_list_line_prefix_lengthContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_prefix_list_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_protocolContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ip_routeContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ipv6_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ipv6_prefixContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Last_as_num_prependsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Line_actionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Literal_standard_communityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Maxas_limitContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Maximum_pathsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nve_host_reachabilityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nve_memberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nve_no_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nve_source_interfaceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvg_ingress_replicationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvg_mcast_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvg_suppress_arpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvm_ingress_replicationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvm_mcast_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvm_peer_ipContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvm_suppress_arpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Object_group_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ogip_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ogipa_lineContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ospf_area_default_costContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ospf_area_idContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ospf_area_range_costContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Packet_lengthContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Pl_actionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Pl_descriptionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af4_aggregate_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af4_networkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af4_redistribute_ospfContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af6_aggregate_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af6_networkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af6_redistribute_ospfv3Context;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af_ipv4_multicastContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af_ipv4_unicastContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af_ipv6_multicastContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af_ipv6_unicastContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_af_l2vpnContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_aa_tailContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_additional_pathsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_client_to_clientContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_dampeningContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_default_informationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_default_metricContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_distanceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_inject_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_maximum_pathsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_nexthop_route_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_redistribute_directContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_redistribute_eigrpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_redistribute_isisContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_redistribute_lispContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_redistribute_ripContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_redistribute_staticContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_suppress_inactiveContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afip_table_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_afl2v_retainContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_bestpathContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_cluster_idContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_confederation_identifierContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_confederation_peersContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_enforce_first_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_log_neighbor_changesContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_maxas_limitContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_address_familyContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_advertise_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_allowas_inContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_as_overrideContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_default_originateContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_disable_peer_as_checkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_filter_listContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_inheritContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_next_hop_selfContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_next_hop_third_partyContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_prefix_listContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_route_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_route_reflector_clientContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_send_communityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_suppress_inactiveContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_af_unsuppress_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_descriptionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_ebgp_multihopContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_inheritContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_local_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_no_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_remote_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_remove_private_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_n_update_sourceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_neighborContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_no_enforce_first_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_router_idContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_template_peerContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_template_peer_policyContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_template_peer_sessionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_v_local_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rb_vrfContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmm_as_pathContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmm_communityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmm_interfaceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmm_metricContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmm_tagContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmmipa_pbrContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmmipa_prefix_listContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rms_communityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rms_local_preferenceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rms_metricContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rms_metric_typeContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rms_originContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rms_tagContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsapp_last_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsapp_literalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsipnh_literalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsipnh_unchangedContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_areaContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_auto_costContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_bfdContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_default_informationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_max_metricContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_networkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_passive_interfaceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_router_idContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_summary_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ro_vrfContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Roa_authenticationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Roa_default_costContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Roa_filter_listContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Roa_nssaContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Roa_rangeContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Roa_stubContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ror_directContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Ror_staticContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rot_lsa_arrivalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rot_lsa_group_pacingContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rott_lsaContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_distinguisherContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_distinguisher_or_autoContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_map_entryContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_map_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_map_pbr_statisticsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_map_sequenceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_networkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_targetContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_target_or_autoContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Router_bgpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Router_ospfContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Router_ospf_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_evpnContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_hostnameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_interface_nveContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_interface_regularContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_route_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_trackContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_versionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_vrf_contextContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Standard_communityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Static_route_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Static_route_prefContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Subnet_maskContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Tcp_flags_maskContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Tcp_portContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Tcp_port_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Template_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Track_object_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Udp_portContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Udp_port_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Uint16Context;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Uint32Context;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Uint8Context;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vc_no_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vc_rdContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vc_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vc_vniContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vcaf4u_route_targetContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vcaf6u_route_targetContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vlan_idContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vlan_id_rangeContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vlan_vlanContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vni_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vrf_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Vv_vn_segmentContext;
import org.batfish.representation.cisco_nxos.ActionIpAccessListLine;
import org.batfish.representation.cisco_nxos.AddrGroupIpAddressSpec;
import org.batfish.representation.cisco_nxos.AddressFamily;
import org.batfish.representation.cisco_nxos.BgpVrfAddressFamilyAggregateNetworkConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfAddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfAddressFamilyConfiguration.Type;
import org.batfish.representation.cisco_nxos.BgpVrfConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfIpAddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfIpv4AddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfIpv6AddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfL2VpnEvpnAddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfL2VpnEvpnAddressFamilyConfiguration.RetainRouteType;
import org.batfish.representation.cisco_nxos.BgpVrfNeighborAddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfNeighborConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfNeighborConfiguration.RemovePrivateAsMode;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage;
import org.batfish.representation.cisco_nxos.DefaultVrfOspfProcess;
import org.batfish.representation.cisco_nxos.Evpn;
import org.batfish.representation.cisco_nxos.EvpnVni;
import org.batfish.representation.cisco_nxos.ExtendedCommunityOrAuto;
import org.batfish.representation.cisco_nxos.FragmentsBehavior;
import org.batfish.representation.cisco_nxos.HsrpGroup;
import org.batfish.representation.cisco_nxos.HsrpTrack;
import org.batfish.representation.cisco_nxos.IcmpOptions;
import org.batfish.representation.cisco_nxos.Interface;
import org.batfish.representation.cisco_nxos.InterfaceAddressWithAttributes;
import org.batfish.representation.cisco_nxos.IpAccessList;
import org.batfish.representation.cisco_nxos.IpAccessListLine;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.IpAsPathAccessList;
import org.batfish.representation.cisco_nxos.IpAsPathAccessListLine;
import org.batfish.representation.cisco_nxos.IpCommunityList;
import org.batfish.representation.cisco_nxos.IpCommunityListStandard;
import org.batfish.representation.cisco_nxos.IpCommunityListStandardLine;
import org.batfish.representation.cisco_nxos.IpPrefixList;
import org.batfish.representation.cisco_nxos.IpPrefixListLine;
import org.batfish.representation.cisco_nxos.Layer3Options;
import org.batfish.representation.cisco_nxos.LiteralIpAddressSpec;
import org.batfish.representation.cisco_nxos.LiteralPortSpec;
import org.batfish.representation.cisco_nxos.Nve;
import org.batfish.representation.cisco_nxos.Nve.HostReachabilityProtocol;
import org.batfish.representation.cisco_nxos.Nve.IngressReplicationProtocol;
import org.batfish.representation.cisco_nxos.NveVni;
import org.batfish.representation.cisco_nxos.ObjectGroup;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddress;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddressLine;
import org.batfish.representation.cisco_nxos.OspfArea;
import org.batfish.representation.cisco_nxos.OspfAreaAuthentication;
import org.batfish.representation.cisco_nxos.OspfAreaNssa;
import org.batfish.representation.cisco_nxos.OspfAreaRange;
import org.batfish.representation.cisco_nxos.OspfAreaStub;
import org.batfish.representation.cisco_nxos.OspfDefaultOriginate;
import org.batfish.representation.cisco_nxos.OspfInterface;
import org.batfish.representation.cisco_nxos.OspfMaxMetricRouterLsa;
import org.batfish.representation.cisco_nxos.OspfNetworkType;
import org.batfish.representation.cisco_nxos.OspfProcess;
import org.batfish.representation.cisco_nxos.OspfSummaryAddress;
import org.batfish.representation.cisco_nxos.OspfVrf;
import org.batfish.representation.cisco_nxos.PortGroupPortSpec;
import org.batfish.representation.cisco_nxos.PortSpec;
import org.batfish.representation.cisco_nxos.RemarkIpAccessListLine;
import org.batfish.representation.cisco_nxos.RouteDistinguisherOrAuto;
import org.batfish.representation.cisco_nxos.RouteMap;
import org.batfish.representation.cisco_nxos.RouteMapEntry;
import org.batfish.representation.cisco_nxos.RouteMapMatchAsPath;
import org.batfish.representation.cisco_nxos.RouteMapMatchCommunity;
import org.batfish.representation.cisco_nxos.RouteMapMatchInterface;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddress;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddressPrefixList;
import org.batfish.representation.cisco_nxos.RouteMapMatchMetric;
import org.batfish.representation.cisco_nxos.RouteMapMatchTag;
import org.batfish.representation.cisco_nxos.RouteMapMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLastAs;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLiteralAs;
import org.batfish.representation.cisco_nxos.RouteMapSetCommunity;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHop;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopLiteral;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopUnchanged;
import org.batfish.representation.cisco_nxos.RouteMapSetLocalPreference;
import org.batfish.representation.cisco_nxos.RouteMapSetMetric;
import org.batfish.representation.cisco_nxos.RouteMapSetMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetOrigin;
import org.batfish.representation.cisco_nxos.RouteMapSetTag;
import org.batfish.representation.cisco_nxos.StaticRoute;
import org.batfish.representation.cisco_nxos.TcpOptions;
import org.batfish.representation.cisco_nxos.UdpOptions;
import org.batfish.representation.cisco_nxos.Vlan;
import org.batfish.representation.cisco_nxos.Vrf;
import org.batfish.representation.cisco_nxos.VrfAddressFamily;
import org.batfish.vendor.VendorConfiguration;

@ParametersAreNonnullByDefault
public final class CiscoNxosControlPlaneExtractor extends CiscoNxosParserBaseListener
    implements ControlPlaneExtractor {

  private static final IntegerSpace BANDWIDTH_RANGE = IntegerSpace.of(Range.closed(1, 100_000_000));
  private static final IntegerSpace BGP_DISTANCE_RANGE = IntegerSpace.of(Range.closed(1, 255));
  private static final IntegerSpace BGP_EBGP_MULTIHOP_TTL_RANGE =
      IntegerSpace.of(Range.closed(2, 255));
  private static final IntegerSpace BGP_INHERIT_RANGE = IntegerSpace.of(Range.closed(1, 65535));
  private static final IntegerSpace BGP_MAXAS_LIMIT_RANGE = IntegerSpace.of(Range.closed(1, 512));
  private static final IntegerSpace BGP_MAXIMUM_PATHS_RANGE = IntegerSpace.of(Range.closed(1, 64));
  private static final IntegerSpace BGP_NEIGHBOR_DESCRIPTION_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 80));
  private static final IntegerSpace BGP_TEMPLATE_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 63));
  private static final IntegerSpace DSCP_RANGE = IntegerSpace.of(Range.closed(0, 63));
  private static final IntegerSpace HSRP_DELAY_RELOAD_S_RANGE =
      IntegerSpace.of(Range.closed(0, 10000));
  private static final IntegerSpace HSRP_GROUP_RANGE = IntegerSpace.of(Range.closed(0, 4095));
  private static final IntegerSpace HSRP_HELLO_INTERVAL_MS_RANGE =
      IntegerSpace.of(Range.closed(250, 999));
  private static final IntegerSpace HSRP_HELLO_INTERVAL_S_RANGE =
      IntegerSpace.of(Range.closed(1, 254));
  private static final IntegerSpace HSRP_HOLD_TIME_MS_RANGE =
      IntegerSpace.of(Range.closed(750, 3000));
  private static final IntegerSpace HSRP_HOLD_TIME_S_RANGE = IntegerSpace.of(Range.closed(3, 255));
  private static final IntegerSpace HSRP_PREEMPT_DELAY_S_RANGE =
      IntegerSpace.of(Range.closed(0, 3600));
  private static final IntegerSpace HSRP_TRACK_DECREMENT_RANGE =
      IntegerSpace.of(Range.closed(1, 255));
  private static final IntegerSpace HSRP_VERSION_RANGE = IntegerSpace.of(Range.closed(1, 2));
  private static final IntegerSpace INTERFACE_DELAY_10US_RANGE =
      IntegerSpace.of(Range.closed(1, 16777215));
  private static final IntegerSpace INTERFACE_DESCRIPTION_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 254));
  private static final IntegerSpace INTERFACE_OSPF_COST_RANGE =
      IntegerSpace.of(Range.closed(1, 65535));
  private static final IntegerSpace INTERFACE_SPEED_RANGE_MBPS =
      IntegerSpace.builder()
          .including(100)
          .including(1_000)
          .including(10_000)
          .including(25_000)
          .including(40_000)
          .including(100_000)
          .build();
  private static final LongSpace IP_ACCESS_LIST_LINE_NUMBER_RANGE =
      LongSpace.of(Range.closed(1L, 4294967295L));
  private static final IntegerSpace IP_ACCESS_LIST_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 64));
  private static final IntegerSpace IP_AS_PATH_ACCESS_LIST_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 63));
  private static final IntegerSpace IP_AS_PATH_ACCESS_LIST_REGEX_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 63));
  private static final LongSpace IP_AS_PATH_ACCESS_LIST_SEQ_RANGE =
      LongSpace.of(Range.closed(1L, 4294967294L));
  private static final LongSpace IP_COMMUNITY_LIST_LINE_NUMBER_RANGE =
      LongSpace.of(Range.closed(1L, 4294967294L));
  private static final IntegerSpace IP_COMMUNITY_LIST_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 63));
  private static final IntegerSpace IP_PREFIX_LIST_DESCRIPTION_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 90));
  private static final LongSpace IP_PREFIX_LIST_LINE_NUMBER_RANGE =
      LongSpace.of(Range.closed(1L, 4294967294L));
  private static final IntegerSpace IP_PREFIX_LIST_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 63));
  private static final IntegerSpace IP_PREFIX_LIST_PREFIX_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 32));
  private static final IntegerSpace LACP_MIN_LINKS_RANGE = IntegerSpace.of(Range.closed(1, 32));
  private static final IntegerSpace NUM_AS_PATH_PREPENDS_RANGE =
      IntegerSpace.of(Range.closed(1, 10));
  private static final IntegerSpace OBJECT_GROUP_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 64));
  private static final LongSpace OBJECT_GROUP_SEQUENCE_RANGE =
      LongSpace.of(Range.closed(1L, 4294967295L));
  private static final IntegerSpace OSPF_AREA_DEFAULT_COST_RANGE =
      IntegerSpace.of(Range.closed(0, 16777215));
  private static final IntegerSpace OSPF_AREA_RANGE_COST_RANGE =
      IntegerSpace.of(Range.closed(0, 16777215));
  private static final IntegerSpace OSPF_AUTO_COST_REFERENCE_BANDWIDTH_GBPS_RANGE =
      IntegerSpace.of(Range.closed(1, 4_000));
  private static final IntegerSpace OSPF_AUTO_COST_REFERENCE_BANDWIDTH_MBPS_RANGE =
      IntegerSpace.of(Range.closed(1, 4_000_000));
  private static final IntegerSpace OSPF_DEAD_INTERVAL_S_RANGE =
      IntegerSpace.of(Range.closed(1, 65535));
  private static final IntegerSpace OSPF_HELLO_INTERVAL_S_RANGE =
      IntegerSpace.of(Range.closed(1, 65535));
  private static final IntegerSpace OSPF_MAX_METRIC_EXTERNAL_LSA_RANGE =
      IntegerSpace.of(Range.closed(1, 16777215));
  private static final IntegerSpace OSPF_MAX_METRIC_SUMMARY_LSA_RANGE =
      IntegerSpace.of(Range.closed(1, 16777215));
  private static final IntegerSpace OSPF_PROCESS_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 20));
  private static final IntegerSpace OSPF_TIMERS_LSA_ARRIVAL_MS_RANGE =
      IntegerSpace.of(Range.closed(10, 600_000));
  private static final IntegerSpace OSPF_TIMERS_LSA_GROUP_PACING_S_RANGE =
      IntegerSpace.of(Range.closed(1, 1800));
  private static final IntegerSpace OSPF_TIMERS_LSA_HOLD_INTERVAL_MS_RANGE =
      IntegerSpace.of(Range.closed(50, 30000));
  private static final IntegerSpace OSPF_TIMERS_LSA_MAX_INTERVAL_MS_RANGE =
      IntegerSpace.of(Range.closed(50, 30000));
  private static final IntegerSpace OSPF_TIMERS_LSA_START_INTERVAL_MS_RANGE =
      IntegerSpace.of(Range.closed(0, 5000));

  @VisibleForTesting
  public static final IntegerSpace PACKET_LENGTH_RANGE = IntegerSpace.of(Range.closed(20, 9210));

  private static final IntegerSpace PORT_CHANNEL_RANGE = IntegerSpace.of(Range.closed(1, 4096));
  private static final IntegerSpace ROUTE_MAP_ENTRY_SEQUENCE_RANGE =
      IntegerSpace.of(Range.closed(0, 65535));
  private static final IntegerSpace ROUTE_MAP_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 63));
  private static final IntegerSpace TCP_FLAGS_MASK_RANGE = IntegerSpace.of(Range.closed(0, 63));

  @VisibleForTesting
  public static final IntegerSpace TCP_PORT_RANGE = IntegerSpace.of(Range.closed(0, 65535));

  @VisibleForTesting
  public static final IntegerSpace UDP_PORT_RANGE = IntegerSpace.of(Range.closed(0, 65535));

  private static final IntegerSpace VNI_RANGE = IntegerSpace.of(Range.closed(0, 16777214));
  private static final IntegerSpace VRF_NAME_LENGTH_RANGE = IntegerSpace.of(Range.closed(1, 32));

  private static @Nonnull IpAddressSpec toAddressSpec(Acllal3_address_specContext ctx) {
    if (ctx.address != null) {
      // address and wildcard
      Ip address = toIp(ctx.address);
      Ip wildcard = toIp(ctx.wildcard);
      return new LiteralIpAddressSpec(IpWildcard.ipWithWildcardMask(address, wildcard).toIpSpace());
    } else if (ctx.prefix != null) {
      return new LiteralIpAddressSpec(toPrefix(ctx.prefix).toIpSpace());
    } else if (ctx.group != null) {
      return new AddrGroupIpAddressSpec(ctx.group.getText());
    } else if (ctx.host != null) {
      return new LiteralIpAddressSpec(toIp(ctx.host).toIpSpace());
    } else {
      // ANY
      checkArgument(ctx.ANY() != null, "Expected 'any', but got %s", ctx.getText());
      return new LiteralIpAddressSpec(UniverseIpSpace.INSTANCE);
    }
  }

  private static int toInteger(Subnet_maskContext ctx) {
    return Ip.parse(ctx.getText()).numSubnetBits();
  }

  private static int toInteger(Uint16Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(Uint8Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static @Nonnull InterfaceAddressWithAttributes toInterfaceAddress(
      Interface_addressContext ctx) {
    // TODO: support exotic address types
    return ctx.iaddress != null
        ? new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse(ctx.getText()))
        : new InterfaceAddressWithAttributes(
            ConcreteInterfaceAddress.create(toIp(ctx.address), toInteger(ctx.mask)));
  }

  private static @Nonnull Ip toIp(Ip_addressContext ctx) {
    return Ip.parse(ctx.getText());
  }

  private static @Nonnull Ip6 toIp6(Ipv6_addressContext ctx) {
    return Ip6.parse(ctx.getText());
  }

  private static @Nonnull IpProtocol toIpProtocol(Ip_protocolContext ctx) {
    if (ctx.num != null) {
      return IpProtocol.fromNumber(toInteger(ctx.num));
    } else if (ctx.AHP() != null) {
      return IpProtocol.AHP;
    } else if (ctx.EIGRP() != null) {
      return IpProtocol.EIGRP;
    } else if (ctx.ESP() != null) {
      return IpProtocol.ESP;
    } else if (ctx.GRE() != null) {
      return IpProtocol.GRE;
    } else if (ctx.ICMP() != null) {
      return IpProtocol.ICMP;
    } else if (ctx.IGMP() != null) {
      return IpProtocol.IGMP;
    } else if (ctx.NOS() != null) {
      return IpProtocol.IPIP;
    } else if (ctx.OSPF() != null) {
      return IpProtocol.OSPF;
    } else if (ctx.PCP() != null) {
      return IpProtocol.IPCOMP;
    } else if (ctx.PIM() != null) {
      return IpProtocol.PIM;
    } else if (ctx.TCP() != null) {
      return IpProtocol.TCP;
    } else if (ctx.UDP() != null) {
      return IpProtocol.UDP;
    } else {
      // All variants should be covered, so just throw if we get here
      throw new IllegalArgumentException(String.format("Unsupported protocol: %s", ctx.getText()));
    }
  }

  private static @Nonnull LineAction toLineAction(Line_actionContext ctx) {
    if (ctx.deny != null) {
      return LineAction.DENY;
    } else {
      return LineAction.PERMIT;
    }
  }

  private static long toLong(Bgp_asnContext ctx) {
    if (ctx.large != null) {
      return toLong(ctx.large);
    } else {
      return (((long) toInteger(ctx.high)) << 16) | ((long) toInteger(ctx.low));
    }
  }

  private static long toLong(Ospf_area_idContext ctx) {
    if (ctx.ip != null) {
      return toIp(ctx.ip).asLong();
    } else {
      assert ctx.num != null;
      return toLong(ctx.num);
    }
  }

  private static long toLong(Uint32Context ctx) {
    return Long.parseLong(ctx.getText());
  }

  private static @Nonnull PortSpec toPortSpec(Acllal4tcp_port_spec_port_groupContext ctx) {
    return new PortGroupPortSpec(ctx.name.getText());
  }

  private static @Nonnull PortSpec toPortSpec(Acllal4udp_port_spec_port_groupContext ctx) {
    return new PortGroupPortSpec(ctx.name.getText());
  }

  private static @Nonnull Prefix toPrefix(Ip_prefixContext ctx) {
    return Prefix.parse(ctx.getText());
  }

  private static @Nonnull Prefix6 toPrefix6(Ipv6_prefixContext ctx) {
    return new Prefix6(ctx.getText());
  }

  private static @Nonnull Prefix toPrefix(Route_networkContext ctx) {
    if (ctx.address != null) {
      return Prefix.create(toIp(ctx.address), toInteger(ctx.mask));
    } else {
      return toPrefix(ctx.prefix);
    }
  }

  private static @Nonnull RouteDistinguisher toRouteDistinguisher(Route_distinguisherContext ctx) {
    if (ctx.hi0 != null) {
      assert ctx.lo0 != null;
      return RouteDistinguisher.from(toInteger(ctx.hi0), toLong(ctx.lo0));
    } else if (ctx.hi1 != null) {
      assert ctx.lo1 != null;
      return RouteDistinguisher.from(toIp(ctx.hi1), toInteger(ctx.lo1));
    } else {
      assert ctx.hi2 != null;
      assert ctx.lo2 != null;
      return RouteDistinguisher.from(toLong(ctx.hi2), toInteger(ctx.lo2));
    }
  }

  private static @Nonnull RouteDistinguisherOrAuto toRouteDistinguisher(
      Route_distinguisher_or_autoContext ctx) {
    if (ctx.AUTO() != null) {
      return RouteDistinguisherOrAuto.auto();
    }
    assert ctx.route_distinguisher() != null;
    return RouteDistinguisherOrAuto.of(toRouteDistinguisher(ctx.route_distinguisher()));
  }

  private static @Nonnull ExtendedCommunity toExtendedCommunity(Route_targetContext ctx) {
    if (ctx.hi0 != null) {
      assert ctx.lo0 != null;
      return ExtendedCommunity.target((long) toInteger(ctx.hi0), toLong(ctx.lo0));
    } else {
      assert ctx.hi2 != null;
      assert ctx.lo2 != null;
      return ExtendedCommunity.target(toLong(ctx.hi2), (long) toInteger(ctx.lo2));
    }
  }

  private static @Nonnull ExtendedCommunityOrAuto toExtendedCommunityOrAuto(
      Route_target_or_autoContext ctx) {
    if (ctx.AUTO() != null) {
      return ExtendedCommunityOrAuto.auto();
    }
    assert ctx.route_target() != null;
    return ExtendedCommunityOrAuto.of(toExtendedCommunity(ctx.route_target()));
  }

  private CiscoNxosConfiguration _configuration;
  private ActionIpAccessListLine.Builder _currentActionIpAccessListLineBuilder;
  private Boolean _currentActionIpAccessListLineUnusable;
  private BgpVrfIpAddressFamilyConfiguration _currentBgpVrfIpAddressFamily;
  private BgpVrfL2VpnEvpnAddressFamilyConfiguration _currentBgpVrfL2VpnEvpnAddressFamily;
  private BgpVrfAddressFamilyAggregateNetworkConfiguration
      _currentBgpVrfAddressFamilyAggregateNetwork;
  private BgpVrfConfiguration _currentBgpVrfConfiguration;
  private BgpVrfNeighborConfiguration _currentBgpVrfNeighbor;
  private BgpVrfNeighborAddressFamilyConfiguration _currentBgpVrfNeighborAddressFamily;
  private DefaultVrfOspfProcess _currentDefaultVrfOspfProcess;
  private EvpnVni _currentEvpnVni;
  private Function<Interface, HsrpGroup> _currentHsrpGroupGetter;
  private List<Interface> _currentInterfaces;
  private IpAccessList _currentIpAccessList;
  private Optional<Long> _currentIpAccessListLineNum;
  private IpPrefixList _currentIpPrefixList;
  private Layer3Options.Builder _currentLayer3OptionsBuilder;
  private List<Nve> _currentNves;
  private List<NveVni> _currentNveVnis;
  private ObjectGroupIpAddress _currentObjectGroupIpAddress;
  private OspfArea _currentOspfArea;
  private OspfProcess _currentOspfProcess;
  private RouteMapEntry _currentRouteMapEntry;
  private Optional<String> _currentRouteMapName;
  private TcpFlags.Builder _currentTcpFlagsBuilder;
  private TcpOptions.Builder _currentTcpOptionsBuilder;
  private UdpOptions.Builder _currentUdpOptionsBuilder;
  private IntegerSpace _currentValidVlanRange;
  private List<Vlan> _currentVlans;
  private Vrf _currentVrf;
  private boolean _inIpv6BgpPeer;

  /**
   * On NX-OS, many structure names are case-insensitive but capitalized according to how they were
   * entered at first use. This table keeps track of the preferred name for each of the structures.
   *
   * <p>{@code (Structure Type, LowerCaseName) -> Preferred name}.
   */
  private final @Nonnull Table<CiscoNxosStructureType, String, String> _preferredNames;

  /** Returns the preferred name for a structure with the given name and type. */
  private @Nonnull String getPreferredName(String configName, CiscoNxosStructureType type) {
    return _preferredNames
        .row(type)
        .computeIfAbsent(configName.toLowerCase(), lcName -> configName);
  }

  private @Nonnull final CiscoNxosCombinedParser _parser;
  private @Nonnull final String _text;
  private @Nonnull final Warnings _w;

  public CiscoNxosControlPlaneExtractor(
      String text, CiscoNxosCombinedParser parser, Warnings warnings) {
    _text = text;
    _parser = parser;
    _preferredNames = HashBasedTable.create();
    _w = warnings;
  }

  /**
   * Clears layer-3 configuration of an interface to enable safe assignment to a new VRF.
   *
   * <p>NX-OS switches clear all layer-3 configuration from interfaces when an interface is assigned
   * to a VRF, presumably to prevent accidental leakage of any connected routes from the old VRF
   * into the new one.
   */
  private void clearLayer3Configuration(Interface iface) {
    iface.setAddress(null);
    iface.getSecondaryAddresses().clear();
  }

  private @Nonnull String convErrorMessage(Class<?> type, ParserRuleContext ctx) {
    return String.format("Could not convert to %s: %s", type.getSimpleName(), getFullText(ctx));
  }

  private @Nullable <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, @Nullable U defaultReturnValue) {
    _w.redFlag(convErrorMessage(returnType, ctx));
    return defaultReturnValue;
  }

  /** This function must be kept in sync with {@link #copyPortChannelCompatibilitySettings}. */
  private static boolean checkPortChannelCompatibilitySettings(
      Interface referenceIface, Interface iface) {
    return Objects.equals(iface.getAccessVlan(), referenceIface.getAccessVlan())
        && Objects.equals(iface.getAllowedVlans(), referenceIface.getAllowedVlans())
        && Objects.equals(iface.getNativeVlan(), referenceIface.getNativeVlan())
        && iface.getSwitchportMode() == referenceIface.getSwitchportMode();
  }

  /** This function must be kept in sync with {@link #checkPortChannelCompatibilitySettings}. */
  private static void copyPortChannelCompatibilitySettings(
      Interface referenceIface, Interface iface) {
    iface.setAccessVlan(referenceIface.getAccessVlan());
    iface.setAllowedVlans(referenceIface.getAllowedVlans());
    iface.setNativeVlan(referenceIface.getNativeVlan());
    iface.setSwitchportMode(referenceIface.getSwitchportMode());
    assert checkPortChannelCompatibilitySettings(referenceIface, iface);
  }

  @Override
  public void enterAcl_line(Acl_lineContext ctx) {
    if (ctx.num != null) {
      _currentIpAccessListLineNum = toLong(ctx, ctx.num);
    } else if (!_currentIpAccessList.getLines().isEmpty()) {
      _currentIpAccessListLineNum = Optional.of(_currentIpAccessList.getLines().lastKey() + 10L);
    } else {
      _currentIpAccessListLineNum = Optional.of(10L);
    }
  }

  @Override
  public void enterAcll_action(Acll_actionContext ctx) {
    _currentActionIpAccessListLineBuilder =
        ActionIpAccessListLine.builder().setAction(toLineAction(ctx.action));
    _currentIpAccessListLineNum.ifPresent(
        num -> _currentActionIpAccessListLineBuilder.setLine(num));
    _currentLayer3OptionsBuilder = Layer3Options.builder();
    _currentActionIpAccessListLineUnusable = false;
  }

  @Override
  public void enterAcllal4_tcp(Acllal4_tcpContext ctx) {
    _currentActionIpAccessListLineBuilder.setProtocol(IpProtocol.TCP);
    _currentTcpOptionsBuilder = TcpOptions.builder();
  }

  @Override
  public void enterAcllal4_udp(Acllal4_udpContext ctx) {
    _currentActionIpAccessListLineBuilder.setProtocol(IpProtocol.UDP);
    _currentUdpOptionsBuilder = UdpOptions.builder();
  }

  @Override
  public void exitBanner_exec(Banner_execContext ctx) {
    String body = ctx.body != null ? ctx.body.getText() : "";
    _configuration.setBannerExec(body);
  }

  @Override
  public void exitBanner_motd(Banner_motdContext ctx) {
    String body = ctx.body != null ? ctx.body.getText() : "";
    _configuration.setBannerMotd(body);
  }

  @Override
  public void enterCisco_nxos_configuration(Cisco_nxos_configurationContext ctx) {
    _configuration = new CiscoNxosConfiguration();
    _currentValidVlanRange = VLAN_RANGE.difference(_configuration.getReservedVlanRange());
    _currentVrf = _configuration.getDefaultVrf();
  }

  @Override
  public void enterEv_vni(Ev_vniContext ctx) {
    Optional<Integer> vniOrError = toInteger(ctx, ctx.vni);
    if (!vniOrError.isPresent()) {
      // Create a dummy for subsequent configuration commands.
      _currentEvpnVni = new EvpnVni(0);
      return;
    }
    int vni = vniOrError.get();
    Evpn e = _configuration.getEvpn();
    assert e != null;
    _currentEvpnVni = e.getVni(vni);
  }

  @Override
  public void exitEv_vni(Ev_vniContext ctx) {
    _currentEvpnVni = null;
  }

  @Override
  public void exitI_speed_number(I_speed_numberContext ctx) {
    toIntegerInSpace(ctx, ctx.speed, INTERFACE_SPEED_RANGE_MBPS, "interface speed")
        .ifPresent(speed -> _currentInterfaces.forEach(iface -> iface.setSpeed(speed)));
  }

  @Override
  public void enterIcl_standard(Icl_standardContext ctx) {
    Long explicitSeq;
    if (ctx.seq != null) {
      Optional<Long> seqOpt = toLong(ctx, ctx.seq);
      if (!seqOpt.isPresent()) {
        return;
      }
      explicitSeq = seqOpt.get();
    } else {
      explicitSeq = null;
    }
    Optional<Set<StandardCommunity>> communities = toStandardCommunitySet(ctx.communities);
    if (!communities.isPresent()) {
      return;
    }
    Optional<String> nameOpt = toString(ctx, ctx.name);
    if (!nameOpt.isPresent()) {
      return;
    }
    String name = nameOpt.get();
    IpCommunityList communityList =
        _configuration
            .getIpCommunityLists()
            .computeIfAbsent(
                name,
                n -> {
                  _configuration.defineStructure(IP_COMMUNITY_LIST_STANDARD, n, ctx);
                  return new IpCommunityListStandard(n);
                });
    if (!(communityList instanceof IpCommunityListStandard)) {
      _w.addWarning(
          ctx,
          getFullText(ctx),
          _parser,
          String.format(
              "Cannot define standard community-list '%s' because another community-list with that name but a different type already exists.",
              name));
      return;
    }
    IpCommunityListStandard communityListStandard = (IpCommunityListStandard) communityList;
    SortedMap<Long, IpCommunityListStandardLine> lines = communityListStandard.getLines();
    long seq;
    if (explicitSeq != null) {
      seq = explicitSeq;
    } else if (!lines.isEmpty()) {
      seq = lines.lastKey() + 1L;
    } else {
      seq = 1L;
    }
    communityListStandard
        .getLines()
        .put(
            seq, new IpCommunityListStandardLine(toLineAction(ctx.action), seq, communities.get()));
  }

  @Override
  public void exitIhd_reload(Ihd_reloadContext ctx) {
    toIntegerInSpace(ctx, ctx.delay_s, HSRP_DELAY_RELOAD_S_RANGE, "hsrp reload delay seconds")
        .ifPresent(
            delay ->
                _currentInterfaces.forEach(
                    iface -> iface.getOrCreateHsrp().setDelayReloadSeconds(delay)));
  }

  @Override
  public void enterIh_group(Ih_groupContext ctx) {
    Optional<Integer> groupOrErr = toIntegerInSpace(ctx, ctx.group, HSRP_GROUP_RANGE, "hsrp group");
    if (!groupOrErr.isPresent()) {
      // dummy
      _currentHsrpGroupGetter = iface -> new HsrpGroup(0);
    } else {
      _currentHsrpGroupGetter =
          iface ->
              iface.getOrCreateHsrp().getGroups().computeIfAbsent(groupOrErr.get(), HsrpGroup::new);
    }
  }

  @Override
  public void exitIhgam_key_chain(Ihgam_key_chainContext ctx) {
    // TODO: support HSRP md5 authentication key-chain
    todo(ctx);
  }

  @Override
  public void exitI_delay(I_delayContext ctx) {
    toIntegerInSpace(
            ctx, ctx.delay, INTERFACE_DELAY_10US_RANGE, "Interface delay (tens of microseconds)")
        .ifPresent(
            delay -> _currentInterfaces.forEach(iface -> iface.setDelayTensOfMicroseconds(delay)));
  }

  @Override
  public void exitI_switchport_switchport(I_switchport_switchportContext ctx) {
    _currentInterfaces.stream()
        .filter(iface -> iface.getSwitchportMode() == SwitchportMode.NONE)
        .forEach(iface -> iface.setSwitchportMode(SwitchportMode.ACCESS));
  }

  @Override
  public void exitIh_group(Ih_groupContext ctx) {
    _currentHsrpGroupGetter = null;
  }

  @Override
  public void exitIhg_ip(Ihg_ipContext ctx) {
    if (ctx.prefix != null) {
      todo(ctx);
      return;
    }
    assert ctx.ip != null;
    Ip ip = toIp(ctx.ip);
    _currentInterfaces.forEach(iface -> _currentHsrpGroupGetter.apply(iface).setIp(ip));
  }

  @Override
  public void exitIhg_preempt(Ihg_preemptContext ctx) {
    @Nullable Integer minimumSeconds = null;
    if (ctx.minimum_s != null) {
      Optional<Integer> minimumSecondsOrErr =
          toIntegerInSpace(
              ctx, ctx.minimum_s, HSRP_PREEMPT_DELAY_S_RANGE, "hspr preempt delay minimum seconds");
      if (!minimumSecondsOrErr.isPresent()) {
        return;
      }
      minimumSeconds = minimumSecondsOrErr.get();
    }
    @Nullable Integer reloadSeconds = null;
    if (ctx.reload_s != null) {
      Optional<Integer> reloadSecondsOrErr =
          toIntegerInSpace(
              ctx, ctx.reload_s, HSRP_PREEMPT_DELAY_S_RANGE, "hspr preempt delay reload seconds");
      if (!reloadSecondsOrErr.isPresent()) {
        return;
      }
      reloadSeconds = reloadSecondsOrErr.get();
    }
    @Nullable Integer syncSeconds = null;
    if (ctx.sync_s != null) {
      Optional<Integer> syncSecondsOrErr =
          toIntegerInSpace(
              ctx, ctx.sync_s, HSRP_PREEMPT_DELAY_S_RANGE, "hspr preempt delay sync seconds");
      if (!syncSecondsOrErr.isPresent()) {
        return;
      }
      syncSeconds = syncSecondsOrErr.get();
    }
    for (Interface iface : _currentInterfaces) {
      HsrpGroup group = _currentHsrpGroupGetter.apply(iface);
      if (minimumSeconds != null) {
        group.setPreemptDelayMinimumSeconds(minimumSeconds);
      }
      if (reloadSeconds != null) {
        group.setPreemptDelayReloadSeconds(reloadSeconds);
      }
      if (syncSeconds != null) {
        group.setPreemptDelaySyncSeconds(syncSeconds);
      }
    }
  }

  @Override
  public void exitIhg_priority(Ihg_priorityContext ctx) {
    int priority = toInteger(ctx.priority);
    _currentInterfaces.forEach(iface -> _currentHsrpGroupGetter.apply(iface).setPriority(priority));
    if (ctx.FORWARDING_THRESHOLD() != null) {
      // TODO: forwarding-threshold for HSRP priority
      todo(ctx);
    }
  }

  @Override
  public void exitIhg_timers(Ihg_timersContext ctx) {
    int helloIntervalMs;
    if (ctx.hello_interval_ms != null) {
      Optional<Integer> helloIntervalMsOrErr =
          toIntegerInSpace(
              ctx,
              ctx.hello_interval_ms,
              HSRP_HELLO_INTERVAL_MS_RANGE,
              "hsrp timers hello-interval ms");
      if (!helloIntervalMsOrErr.isPresent()) {
        return;
      }
      helloIntervalMs = helloIntervalMsOrErr.get();
    } else {
      assert ctx.hello_interval_s != null;
      Optional<Integer> helloIntervalSecondsOrErr =
          toIntegerInSpace(
              ctx,
              ctx.hello_interval_s,
              HSRP_HELLO_INTERVAL_S_RANGE,
              "hsrp timers hello-interval seconds");
      if (!helloIntervalSecondsOrErr.isPresent()) {
        return;
      }
      helloIntervalMs = helloIntervalSecondsOrErr.get() * 1000;
    }
    int holdTimeMs;
    if (ctx.hold_time_ms != null) {
      Optional<Integer> holdTimeMsOrErr =
          toIntegerInSpace(
              ctx, ctx.hold_time_ms, HSRP_HOLD_TIME_MS_RANGE, "hsrp timers hold-time ms");
      if (!holdTimeMsOrErr.isPresent()) {
        return;
      }
      holdTimeMs = holdTimeMsOrErr.get();
    } else {
      assert ctx.hold_time_s != null;
      Optional<Integer> holdTimeSecondsOrErr =
          toIntegerInSpace(
              ctx, ctx.hold_time_s, HSRP_HOLD_TIME_S_RANGE, "hsrp timers hold-time seconds");
      if (!holdTimeSecondsOrErr.isPresent()) {
        return;
      }
      holdTimeMs = holdTimeSecondsOrErr.get() * 1000;
    }
    // TODO: check constraints on relationship between hello and hold
    _currentInterfaces.forEach(
        iface -> {
          HsrpGroup group = _currentHsrpGroupGetter.apply(iface);
          group.setHelloIntervalMs(helloIntervalMs);
          group.setHoldTimeMs(holdTimeMs);
        });
  }

  @Override
  public void exitIhg_track(Ihg_trackContext ctx) {
    Optional<Integer> trackObjectNumberOrErr =
        toIntegerInSpace(ctx, ctx.num, STATIC_ROUTE_TRACK_RANGE, "hsrp group track object number");
    if (!trackObjectNumberOrErr.isPresent()) {
      return;
    }
    int trackObjectNumber = trackObjectNumberOrErr.get();
    @Nullable Integer decrement;
    if (ctx.decrement != null) {
      Optional<Integer> decrementOrErr =
          toIntegerInSpace(
              ctx, ctx.decrement, HSRP_TRACK_DECREMENT_RANGE, "hspr group track decrement");
      if (!decrementOrErr.isPresent()) {
        return;
      }
      decrement = decrementOrErr.get();
    } else {
      // disable instead of decrement when tracked object goes down
      decrement = null;
    }
    _currentInterfaces.forEach(
        iface ->
            _currentHsrpGroupGetter
                .apply(iface)
                .getTracks()
                .computeIfAbsent(trackObjectNumber, num -> new HsrpTrack(num, decrement)));
  }

  @Override
  public void exitIh_version(Ih_versionContext ctx) {
    toIntegerInSpace(ctx, ctx.version, HSRP_VERSION_RANGE, "hsrp version")
        .ifPresent(
            version ->
                _currentInterfaces.forEach(iface -> iface.getOrCreateHsrp().setVersion(version)));
  }

  @Override
  public void exitIl_min_links(Il_min_linksContext ctx) {
    toIntegerInSpace(ctx, ctx.num, LACP_MIN_LINKS_RANGE, "lacp min-links")
        .ifPresent(
            minLinks ->
                _currentInterfaces.forEach(iface -> iface.getOrCreateLacp().setMinLinks(minLinks)));
  }

  @Override
  public void exitIipo_bfd(Iipo_bfdContext ctx) {
    _currentInterfaces.forEach(iface -> iface.getOrCreateOspf().setBfd(true));
  }

  @Override
  public void exitIipo_cost(Iipo_costContext ctx) {
    toIntegerInSpace(ctx, ctx.cost, INTERFACE_OSPF_COST_RANGE, "OSPF cost")
        .ifPresent(
            cost -> _currentInterfaces.forEach(iface -> iface.getOrCreateOspf().setCost(cost)));
  }

  @Override
  public void exitIipo_dead_interval(Iipo_dead_intervalContext ctx) {
    Optional<Integer> deadIntervalOrErr =
        toIntegerInSpace(ctx, ctx.interval_s, OSPF_DEAD_INTERVAL_S_RANGE, "OSPF dead-interval");
    deadIntervalOrErr.ifPresent(
        deadInterval ->
            _currentInterfaces.forEach(
                iface -> iface.getOrCreateOspf().setDeadIntervalS(deadInterval)));
  }

  @Override
  public void exitIipo_hello_interval(Iipo_hello_intervalContext ctx) {
    Optional<Integer> helloIntervalOrErr =
        toIntegerInSpace(ctx, ctx.interval_s, OSPF_HELLO_INTERVAL_S_RANGE, "OSPF hello-interval");
    helloIntervalOrErr.ifPresent(
        helloInterval ->
            _currentInterfaces.forEach(
                iface -> iface.getOrCreateOspf().setHelloIntervalS(helloInterval)));
  }

  @Override
  public void exitIipo_network(Iipo_networkContext ctx) {
    OspfNetworkType type;
    if (ctx.BROADCAST() != null) {
      type = OspfNetworkType.BROADCAST;
    } else if (ctx.POINT_TO_POINT() != null) {
      type = OspfNetworkType.POINT_TO_POINT;
    } else {
      // assume valid but unsupported
      todo(ctx);
      return;
    }
    _currentInterfaces.forEach(iface -> iface.getOrCreateOspf().setNetwork(type));
  }

  @Override
  public void exitIipo_passive_interface(Iipo_passive_interfaceContext ctx) {
    _currentInterfaces.forEach(iface -> iface.getOrCreateOspf().setPassive(true));
  }

  @Override
  public void exitIipr_ospf(Iipr_ospfContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      return;
    }
    String name = nameOrErr.get();
    long area = toLong(ctx.area);
    _currentInterfaces.forEach(
        iface -> {
          OspfInterface ospf = iface.getOrCreateOspf();
          ospf.setProcess(name);
          ospf.setArea(area);
        });
  }

  @Override
  public void enterIp_access_list(Ip_access_listContext ctx) {
    Optional<String> nameOpt = toString(ctx, ctx.name);
    if (!nameOpt.isPresent()) {
      _currentIpAccessList = new IpAccessList("dummy");
      return;
    }
    _currentIpAccessList =
        _configuration
            .getIpAccessLists()
            .computeIfAbsent(
                nameOpt.get(),
                name -> {
                  _configuration.defineStructure(IP_ACCESS_LIST, name, ctx);
                  return new IpAccessList(name);
                });
  }

  @Override
  public void enterIp_as_path_access_list(Ip_as_path_access_listContext ctx) {
    Long explicitSeq;
    if (ctx.seq != null) {
      Optional<Long> seqOpt = toLong(ctx, ctx.seq);
      if (!seqOpt.isPresent()) {
        return;
      }
      explicitSeq = seqOpt.get();
    } else {
      explicitSeq = null;
    }
    Optional<String> nameOpt = toString(ctx, ctx.name);
    if (!nameOpt.isPresent()) {
      return;
    }
    Optional<String> regexOpt = toString(ctx, ctx.regex);
    if (!regexOpt.isPresent()) {
      return;
    }
    String name = nameOpt.get();
    IpAsPathAccessList asPathAccessList =
        _configuration
            .getIpAsPathAccessLists()
            .computeIfAbsent(
                name,
                n -> {
                  _configuration.defineStructure(IP_AS_PATH_ACCESS_LIST, n, ctx);
                  return new IpAsPathAccessList(n);
                });
    SortedMap<Long, IpAsPathAccessListLine> lines = asPathAccessList.getLines();
    long seq;
    if (explicitSeq != null) {
      seq = explicitSeq;
    } else if (!lines.isEmpty()) {
      seq = lines.lastKey() + 1L;
    } else {
      seq = 1L;
    }
    asPathAccessList
        .getLines()
        .put(seq, new IpAsPathAccessListLine(toLineAction(ctx.action), seq, regexOpt.get()));
  }

  @Override
  public void enterIp_prefix_list(Ip_prefix_listContext ctx) {
    _currentIpPrefixList =
        toString(ctx, ctx.name)
            .map(
                name ->
                    _configuration
                        .getIpPrefixLists()
                        .computeIfAbsent(
                            name,
                            n -> {
                              _configuration.defineStructure(IP_PREFIX_LIST, name, ctx);
                              return new IpPrefixList(n);
                            }))
            .orElse(new IpPrefixList("dummy"));
  }

  @Override
  public void enterNve_member(Nve_memberContext ctx) {
    Optional<Integer> vniOrError = toInteger(ctx, ctx.vni);
    if (!vniOrError.isPresent()) {
      // dummy NVE member VNI
      _currentNveVnis = ImmutableList.of();
      return;
    }
    int vni = vniOrError.get();
    _currentNveVnis =
        _currentNves.stream()
            .map(nve -> nve.getMemberVni(vni))
            .collect(ImmutableList.toImmutableList());
    if (ctx.ASSOCIATE_VRF() != null) {
      _currentNveVnis.forEach(memberVni -> memberVni.setAssociateVrf(true));
    }
  }

  @Override
  public void enterRo_area(Ro_areaContext ctx) {
    long areaId = toLong(ctx.id);
    _currentOspfArea =
        _currentOspfProcess
            .getAreas()
            .computeIfAbsent(
                areaId,
                id -> {
                  _configuration.defineStructure(
                      CiscoNxosStructureType.OSPF_AREA, Long.toString(id), ctx);
                  return new OspfArea(id);
                });
  }

  @Override
  public void exitRo_area(Ro_areaContext ctx) {
    _currentOspfArea = null;
  }

  @Override
  public void exitRo_auto_cost(Ro_auto_costContext ctx) {
    if (ctx.gbps != null) {
      toIntegerInSpace(
              ctx,
              ctx.gbps,
              OSPF_AUTO_COST_REFERENCE_BANDWIDTH_GBPS_RANGE,
              "router ospf auto-cost reference-bandwidth gbps")
          .ifPresent(gbps -> _currentOspfProcess.setAutoCostReferenceBandwidthMbps(gbps * 1000));
    } else {
      assert ctx.mbps != null;
      toIntegerInSpace(
              ctx,
              ctx.mbps,
              OSPF_AUTO_COST_REFERENCE_BANDWIDTH_MBPS_RANGE,
              "router ospf auto-cost reference-bandwidth mbps")
          .ifPresent(_currentOspfProcess::setAutoCostReferenceBandwidthMbps);
    }
  }

  @Override
  public void exitRo_bfd(Ro_bfdContext ctx) {
    _currentOspfProcess.setBfd(true);
  }

  @Override
  public void exitRo_default_information(Ro_default_informationContext ctx) {
    String routeMap = null;
    if (ctx.rm != null) {
      Optional<String> routeMapOrErr = toString(ctx, ctx.rm);
      if (!routeMapOrErr.isPresent()) {
        return;
      }
      routeMap = routeMapOrErr.get();
    }
    OspfDefaultOriginate defaultOriginate = _currentOspfProcess.getDefaultOriginate();
    if (defaultOriginate == null) {
      defaultOriginate = new OspfDefaultOriginate();
      _currentOspfProcess.setDefaultOriginate(defaultOriginate);
    }
    if (ctx.always != null) {
      defaultOriginate.setAlways(true);
    }
    if (routeMap != null) {
      defaultOriginate.setRouteMap(routeMap);
    }
  }

  @Override
  public void exitRo_max_metric(Ro_max_metricContext ctx) {
    @Nullable Integer externalLsa = null;
    if (ctx.external_lsa != null) {
      if (ctx.manual_external_lsa != null) {
        Optional<Integer> externalLsaOrErr =
            toIntegerInSpace(
                ctx,
                ctx.manual_external_lsa,
                OSPF_MAX_METRIC_EXTERNAL_LSA_RANGE,
                "OSPF external LSA max metric");
        if (!externalLsaOrErr.isPresent()) {
          return;
        }
        externalLsa = externalLsaOrErr.get();
      } else {
        externalLsa = OspfMaxMetricRouterLsa.DEFAULT_OSPF_MAX_METRIC;
      }
    }
    @Nullable Integer summaryLsa = null;
    if (ctx.summary_lsa != null) {
      if (ctx.manual_summary_lsa != null) {
        Optional<Integer> summaryLsaOrErr =
            toIntegerInSpace(
                ctx,
                ctx.manual_summary_lsa,
                OSPF_MAX_METRIC_SUMMARY_LSA_RANGE,
                "OSPF summary LSA max metric");
        if (!summaryLsaOrErr.isPresent()) {
          return;
        }
        summaryLsa = summaryLsaOrErr.get();
      } else {
        summaryLsa = OspfMaxMetricRouterLsa.DEFAULT_OSPF_MAX_METRIC;
      }
    }

    OspfMaxMetricRouterLsa maxMetricRouterLsa = _currentOspfProcess.getMaxMetricRouterLsa();
    if (maxMetricRouterLsa == null) {
      maxMetricRouterLsa = new OspfMaxMetricRouterLsa();
      _currentOspfProcess.setMaxMetricRouterLsa(maxMetricRouterLsa);
    }
    if (externalLsa != null) {
      maxMetricRouterLsa.setExternalLsa(externalLsa);
    }
    if (ctx.include_stub != null) {
      maxMetricRouterLsa.setIncludeStub(true);
    }
    if (summaryLsa != null) {
      maxMetricRouterLsa.setSummaryLsa(summaryLsa);
    }
  }

  @Override
  public void exitRo_network(Ro_networkContext ctx) {
    IpWildcard wildcard;
    if (ctx.ip != null) {
      wildcard = ipWithWildcardMask(toIp(ctx.ip), toIp(ctx.wildcard));
    } else {
      assert ctx.prefix != null;
      wildcard = IpWildcard.create(toPrefix(ctx.prefix));
    }
    long areaId = toLong(ctx.id);
    _currentOspfProcess.getAreas().computeIfAbsent(areaId, OspfArea::new);
    _currentOspfProcess.getNetworks().put(wildcard, toLong(ctx.id));
  }

  @Override
  public void exitRo_passive_interface(Ro_passive_interfaceContext ctx) {
    _currentOspfProcess.setPassiveInterfaceDefault(true);
  }

  @Override
  public void exitRor_direct(Ror_directContext ctx) {
    toString(ctx, ctx.rm).ifPresent(_currentOspfProcess::setRedistributeDirectRouteMap);
  }

  @Override
  public void exitRor_static(Ror_staticContext ctx) {
    toString(ctx, ctx.rm).ifPresent(_currentOspfProcess::setRedistributeStaticRouteMap);
  }

  @Override
  public void exitRo_router_id(Ro_router_idContext ctx) {
    _currentOspfProcess.setRouterId(toIp(ctx.id));
  }

  @Override
  public void exitRo_summary_address(Ro_summary_addressContext ctx) {
    OspfSummaryAddress summaryAddress =
        _currentOspfProcess
            .getSummaryAddresses()
            .computeIfAbsent(toPrefix(ctx.network), OspfSummaryAddress::new);
    if (ctx.not_advertise != null) {
      summaryAddress.setNotAdvertise(true);
    } else if (ctx.tag != null) {
      summaryAddress.setNotAdvertise(false);
      summaryAddress.setTag(toLong(ctx.tag));
    }
  }

  @Override
  public void enterRo_vrf(Ro_vrfContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      _currentOspfProcess = new OspfVrf("dummy");
      return;
    }
    _currentOspfProcess =
        _currentDefaultVrfOspfProcess.getVrfs().computeIfAbsent(nameOrErr.get(), OspfVrf::new);
  }

  @Override
  public void exitRo_vrf(Ro_vrfContext ctx) {
    _currentOspfProcess = _currentDefaultVrfOspfProcess;
  }

  @Override
  public void exitRot_lsa_arrival(Rot_lsa_arrivalContext ctx) {
    toIntegerInSpace(
            ctx, ctx.time_ms, OSPF_TIMERS_LSA_ARRIVAL_MS_RANGE, "OSPF LSA arrival interval")
        .ifPresent(_currentOspfProcess::setTimersLsaArrival);
  }

  @Override
  public void enterRot_lsa_group_pacing(Rot_lsa_group_pacingContext ctx) {
    toIntegerInSpace(
            ctx, ctx.time_s, OSPF_TIMERS_LSA_GROUP_PACING_S_RANGE, "OSPF LSA group pacing interval")
        .ifPresent(_currentOspfProcess::setTimersLsaGroupPacing);
  }

  @Override
  public void exitRott_lsa(Rott_lsaContext ctx) {
    Optional<Integer> startIntervalOrErr =
        toIntegerInSpace(
            ctx,
            ctx.start_interval_ms,
            OSPF_TIMERS_LSA_START_INTERVAL_MS_RANGE,
            "OSPF LSA start interval");
    if (!startIntervalOrErr.isPresent()) {
      return;
    }
    Optional<Integer> holdIntervalOrErr =
        toIntegerInSpace(
            ctx,
            ctx.hold_interval_ms,
            OSPF_TIMERS_LSA_HOLD_INTERVAL_MS_RANGE,
            "OSPF LSA hold interval");
    if (!holdIntervalOrErr.isPresent()) {
      return;
    }
    Optional<Integer> maxIntervalOrErr =
        toIntegerInSpace(
            ctx,
            ctx.max_interval_ms,
            OSPF_TIMERS_LSA_MAX_INTERVAL_MS_RANGE,
            "OSPF LSA max interval");
    if (!maxIntervalOrErr.isPresent()) {
      return;
    }
    _currentOspfProcess.setTimersLsaStartInterval(startIntervalOrErr.get());
    _currentOspfProcess.setTimersLsaHoldInterval(holdIntervalOrErr.get());
    _currentOspfProcess.setTimersLsaMaxInterval(maxIntervalOrErr.get());
  }

  @Override
  public void exitRoa_authentication(Roa_authenticationContext ctx) {
    _currentOspfArea.setAuthentication(
        ctx.digest != null ? OspfAreaAuthentication.MESSAGE_DIGEST : OspfAreaAuthentication.SIMPLE);
  }

  @Override
  public void exitRoa_default_cost(Roa_default_costContext ctx) {
    toInteger(ctx, ctx.cost).ifPresent(_currentOspfArea::setDefaultCost);
  }

  @Override
  public void exitRoa_filter_list(Roa_filter_listContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      return;
    }
    String name = nameOrErr.get();
    CiscoNxosStructureUsage usage;
    if (ctx.in != null) {
      usage = CiscoNxosStructureUsage.OSPF_AREA_FILTER_LIST_IN;
      _currentOspfArea.setFilterListIn(name);
    } else {
      assert ctx.out != null;
      usage = CiscoNxosStructureUsage.OSPF_AREA_FILTER_LIST_OUT;
      _currentOspfArea.setFilterListOut(name);
    }
    _configuration.referenceStructure(
        CiscoNxosStructureType.ROUTE_MAP, name, usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRoa_nssa(Roa_nssaContext ctx) {
    if (_currentOspfArea.getId() == 0L) {
      _w.addWarning(ctx, getFullText(ctx), _parser, "Backbone area cannot be an NSSA");
      return;
    }
    String routeMap = null;
    if (ctx.rm != null) {
      Optional<String> routeMapOrErr = toString(ctx, ctx.rm);
      if (!routeMapOrErr.isPresent()) {
        return;
      }
      routeMap = routeMapOrErr.get();
    }
    OspfAreaNssa nssa =
        Optional.ofNullable(_currentOspfArea.getTypeSettings())
            .filter(OspfAreaNssa.class::isInstance)
            .map(OspfAreaNssa.class::cast)
            .orElseGet(
                () -> {
                  // overwrite if missing or a different area type
                  OspfAreaNssa newNssa = new OspfAreaNssa();
                  _currentOspfArea.setTypeSettings(newNssa);
                  return newNssa;
                });
    if (ctx.no_redistribution != null) {
      nssa.setNoRedistribution(true);
    }
    if (ctx.no_summary != null) {
      nssa.setNoSummary(true);
    }
    if (routeMap != null) {
      nssa.setRouteMap(routeMap);
    }
  }

  @Override
  public void exitRoa_range(Roa_rangeContext ctx) {
    Integer cost = null;
    if (ctx.cost != null) {
      Optional<Integer> costOrErr = toInteger(ctx, ctx.cost);
      if (!costOrErr.isPresent()) {
        return;
      }
      cost = costOrErr.get();
    }
    OspfAreaRange range =
        _currentOspfArea.getRanges().computeIfAbsent(toPrefix(ctx.network), OspfAreaRange::new);
    if (cost != null) {
      range.setCost(cost);
    }
    if (ctx.not_advertise != null) {
      range.setNotAdvertise(true);
    }
  }

  @Override
  public void exitRoa_stub(Roa_stubContext ctx) {
    if (_currentOspfArea.getId() == 0L) {
      _w.addWarning(ctx, getFullText(ctx), _parser, "Backbone area cannot be a stub");
      return;
    }
    OspfAreaStub stub =
        Optional.ofNullable(_currentOspfArea.getTypeSettings())
            .filter(OspfAreaStub.class::isInstance)
            .map(OspfAreaStub.class::cast)
            .orElseGet(
                () -> {
                  // overwrite if missing or a different area type
                  OspfAreaStub newStub = new OspfAreaStub();
                  _currentOspfArea.setTypeSettings(newStub);
                  return newStub;
                });
    if (ctx.no_summary != null) {
      stub.setNoSummary(true);
    }
  }

  @Override
  public void enterRouter_ospf(Router_ospfContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      return;
    }
    _currentDefaultVrfOspfProcess =
        _configuration
            .getOspfProcesses()
            .computeIfAbsent(
                nameOrErr.get(),
                name -> {
                  _configuration.defineStructure(CiscoNxosStructureType.ROUTER_OSPF, name, ctx);
                  return new DefaultVrfOspfProcess(name);
                });
    _currentOspfProcess = _currentDefaultVrfOspfProcess;
  }

  @Override
  public void exitRouter_ospf(Router_ospfContext ctx) {
    _currentDefaultVrfOspfProcess = null;
    _currentOspfProcess = null;
  }

  @Override
  public void exitNve_member(Nve_memberContext ctx) {
    _currentNveVnis = null;
  }

  @Override
  public void exitNvm_ingress_replication(Nvm_ingress_replicationContext ctx) {
    if (ctx.BGP() != null) {
      if (_currentNves.stream()
          .anyMatch(nve -> nve.getHostReachabilityProtocol() != HostReachabilityProtocol.BGP)) {
        warn(
            ctx,
            "Cannot enable ingress replication bgp under VNI without host-reachability protocol bgp");
        return;
      }
      _currentNveVnis.forEach(
          vni -> vni.setIngressReplicationProtocol(IngressReplicationProtocol.BGP));
    } else {
      assert ctx.STATIC() != null;
      if (_currentNves.stream().anyMatch(nve -> nve.getHostReachabilityProtocol() != null)) {
        warn(
            ctx,
            "Cannot enable ingress replication static under VNI without unset host-reachability protocol");
        return;
      }
      _currentNveVnis.forEach(
          vni -> vni.setIngressReplicationProtocol(IngressReplicationProtocol.STATIC));
    }
  }

  @Override
  public void exitNvm_mcast_group(Nvm_mcast_groupContext ctx) {
    Ip mcastIp = toIp(ctx.first);
    if (!Prefix.MULTICAST.containsIp(mcastIp)) {
      warn(ctx, String.format("IPv4 address %s is not a valid multicast IP", mcastIp));
      return;
    }
    if (_currentNveVnis.stream()
        .anyMatch(
            vni -> vni.getIngressReplicationProtocol() == IngressReplicationProtocol.STATIC)) {
      warn(ctx, "Cannot set multicast group with ingress-replication protocol static");
      return;
    }
    _currentNveVnis.forEach(vni -> vni.setMcastGroup(mcastIp));
  }

  @Override
  public void exitNvm_peer_ip(Nvm_peer_ipContext ctx) {
    if (_currentNveVnis.stream()
        .anyMatch(
            vni -> vni.getIngressReplicationProtocol() != IngressReplicationProtocol.STATIC)) {
      warn(ctx, "Cannot set peer-ip unless ingress-replication protocol is static");
      return;
    }
    Ip peerIp = toIp(ctx.ip_address());
    _currentNveVnis.forEach(vni -> vni.addPeerIp(peerIp));
  }

  @Override
  public void exitNvm_suppress_arp(Nvm_suppress_arpContext ctx) {
    boolean value = ctx.DISABLE() == null;
    _currentNveVnis.forEach(vni -> vni.setSuppressArp(value));
  }

  @Override
  public void exitNvg_ingress_replication(Nvg_ingress_replicationContext ctx) {
    if (_currentNves.stream()
        .anyMatch(nve -> nve.getHostReachabilityProtocol() != HostReachabilityProtocol.BGP)) {
      _w.addWarning(
          ctx,
          getFullText(ctx),
          _parser,
          "Cannot configure Ingress replication protocol BGP for nve without host reachability protocol bgp.");
      return;
    }
    _currentNves.forEach(
        vni -> vni.setGlobalIngressReplicationProtocol(IngressReplicationProtocol.BGP));
  }

  @Override
  public void exitNvg_mcast_group(Nvg_mcast_groupContext ctx) {
    Ip mcastIp = toIp(ctx.ip_address());
    if (!Prefix.MULTICAST.containsIp(mcastIp)) {
      warn(ctx, String.format("IPv4 address %s is not a valid multicast IP", mcastIp));
      return;
    }
    if (ctx.L2() != null) {
      _currentNves.forEach(vni -> vni.setMulticastGroupL2(mcastIp));
    } else {
      assert ctx.L3() != null;
      _currentNves.forEach(vni -> vni.setMulticastGroupL3(mcastIp));
    }
  }

  @Override
  public void exitNvg_suppress_arp(Nvg_suppress_arpContext ctx) {
    _currentNves.forEach(vni -> vni.setGlobalSuppressArp(true));
  }

  @Override
  public void enterOgip_address(Ogip_addressContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      _currentObjectGroupIpAddress = new ObjectGroupIpAddress("dummy");
      return;
    }
    String name = nameOrErr.get();
    ObjectGroup existing = _configuration.getObjectGroups().get(name);
    if (existing != null) {
      if (!(existing instanceof ObjectGroupIpAddress)) {
        warn(
            ctx,
            String.format(
                "Cannot create object-group '%s' of type ip address because an object-group of a different type already exists with that name.",
                name));
        _currentObjectGroupIpAddress = new ObjectGroupIpAddress("dummy");
        return;
      }
      _currentObjectGroupIpAddress = (ObjectGroupIpAddress) existing;
    } else {
      _currentObjectGroupIpAddress = new ObjectGroupIpAddress(name);
      _configuration.defineStructure(OBJECT_GROUP_IP_ADDRESS, name, ctx);
      _configuration.getObjectGroups().put(name, _currentObjectGroupIpAddress);
    }
  }

  @Override
  public void exitOgip_address(Ogip_addressContext ctx) {
    _currentObjectGroupIpAddress = null;
  }

  @Override
  public void exitOgipa_line(Ogipa_lineContext ctx) {
    long seq;
    if (ctx.seq != null) {
      Optional<Long> seqOrErr =
          toLongInSpace(ctx, ctx.seq, OBJECT_GROUP_SEQUENCE_RANGE, "object-group sequence number");
      if (!seqOrErr.isPresent()) {
        return;
      }
      seq = seqOrErr.get();
    } else if (_currentObjectGroupIpAddress.getLines().isEmpty()) {
      seq = 10L;
    } else {
      seq = _currentObjectGroupIpAddress.getLines().lastKey() + 10L;
    }
    IpWildcard ipWildcard;
    if (ctx.address != null) {
      Ip address = toIp(ctx.address);
      if (ctx.wildcard != null) {
        ipWildcard = IpWildcard.ipWithWildcardMask(address, toIp(ctx.wildcard));
      } else {
        // host
        ipWildcard = IpWildcard.create(address);
      }
    } else {
      assert ctx.prefix != null;
      ipWildcard = IpWildcard.create(toPrefix(ctx.prefix));
    }
    _currentObjectGroupIpAddress.getLines().put(seq, new ObjectGroupIpAddressLine(seq, ipWildcard));
  }

  @Override
  public void enterRb_af_ipv4_multicast(Rb_af_ipv4_multicastContext ctx) {
    BgpVrfAddressFamilyConfiguration af =
        _currentBgpVrfConfiguration.getOrCreateAddressFamily(Type.IPV4_MULTICAST);
    assert af instanceof BgpVrfIpv4AddressFamilyConfiguration;
    _currentBgpVrfIpAddressFamily = (BgpVrfIpAddressFamilyConfiguration) af;
  }

  @Override
  public void exitRb_af_ipv4_multicast(Rb_af_ipv4_multicastContext ctx) {
    _currentBgpVrfIpAddressFamily = null;
  }

  @Override
  public void enterRb_af_ipv4_unicast(Rb_af_ipv4_unicastContext ctx) {
    BgpVrfAddressFamilyConfiguration af =
        _currentBgpVrfConfiguration.getOrCreateAddressFamily(Type.IPV4_UNICAST);
    assert af instanceof BgpVrfIpv4AddressFamilyConfiguration;
    _currentBgpVrfIpAddressFamily = (BgpVrfIpAddressFamilyConfiguration) af;
  }

  @Override
  public void exitRb_af_ipv4_unicast(Rb_af_ipv4_unicastContext ctx) {
    _currentBgpVrfIpAddressFamily = null;
  }

  @Override
  public void enterRb_af4_aggregate_address(Rb_af4_aggregate_addressContext ctx) {
    assert _currentBgpVrfIpAddressFamily instanceof BgpVrfIpv4AddressFamilyConfiguration;
    BgpVrfIpv4AddressFamilyConfiguration afConfig =
        (BgpVrfIpv4AddressFamilyConfiguration) _currentBgpVrfIpAddressFamily;
    Prefix prefix = toPrefix(ctx.network);
    _currentBgpVrfAddressFamilyAggregateNetwork = afConfig.getOrCreateAggregateNetwork(prefix);
  }

  @Override
  public void exitRb_af4_aggregate_address(Rb_af4_aggregate_addressContext ctx) {
    _currentBgpVrfAddressFamilyAggregateNetwork = null;
  }

  @Override
  public void exitRb_af4_network(Rb_af4_networkContext ctx) {
    assert _currentBgpVrfIpAddressFamily instanceof BgpVrfIpv4AddressFamilyConfiguration;
    BgpVrfIpv4AddressFamilyConfiguration afConfig =
        (BgpVrfIpv4AddressFamilyConfiguration) _currentBgpVrfIpAddressFamily;
    String mapname = null;
    if (ctx.mapname != null) {
      Optional<String> nameOrError = toString(ctx, ctx.mapname);
      if (!nameOrError.isPresent()) {
        return;
      }
      mapname = nameOrError.get();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, BGP_NETWORK_ROUTE_MAP, ctx.getStart().getLine());
    }

    Prefix prefix = toPrefix(ctx.network);
    afConfig.addNetwork(prefix, mapname);
  }

  @Override
  public void exitRb_af4_redistribute_ospf(Rb_af4_redistribute_ospfContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_OSPF_ROUTE_MAP, ctx.getStart().getLine());
    Optional<String> ospfProcessOrError = toString(ctx, ctx.source_tag);
    if (!ospfProcessOrError.isPresent()) {
      return;
    }
    String ospfProcess = ospfProcessOrError.get();
    _configuration.referenceStructure(
        ROUTER_OSPF, ospfProcess, BGP_REDISTRIBUTE_OSPF_SOURCE_TAG, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(RoutingProtocol.OSPF, name, ospfProcess);
  }

  @Override
  public void enterRb_af_ipv6_multicast(Rb_af_ipv6_multicastContext ctx) {
    BgpVrfAddressFamilyConfiguration af =
        _currentBgpVrfConfiguration.getOrCreateAddressFamily(Type.IPV6_MULTICAST);
    assert af instanceof BgpVrfIpv6AddressFamilyConfiguration;
    _currentBgpVrfIpAddressFamily = (BgpVrfIpAddressFamilyConfiguration) af;
  }

  @Override
  public void exitRb_af_ipv6_multicast(Rb_af_ipv6_multicastContext ctx) {
    _currentBgpVrfIpAddressFamily = null;
  }

  @Override
  public void enterRb_af_ipv6_unicast(Rb_af_ipv6_unicastContext ctx) {
    BgpVrfAddressFamilyConfiguration af =
        _currentBgpVrfConfiguration.getOrCreateAddressFamily(Type.IPV6_UNICAST);
    assert af instanceof BgpVrfIpv6AddressFamilyConfiguration;
    _currentBgpVrfIpAddressFamily = (BgpVrfIpAddressFamilyConfiguration) af;
  }

  @Override
  public void exitRb_af_ipv6_unicast(Rb_af_ipv6_unicastContext ctx) {
    _currentBgpVrfIpAddressFamily = null;
  }

  @Override
  public void enterRb_af6_aggregate_address(Rb_af6_aggregate_addressContext ctx) {
    Prefix6 prefix = toPrefix6(ctx.network);

    assert _currentBgpVrfIpAddressFamily instanceof BgpVrfIpv6AddressFamilyConfiguration;
    BgpVrfIpv6AddressFamilyConfiguration afConfig =
        (BgpVrfIpv6AddressFamilyConfiguration) _currentBgpVrfIpAddressFamily;
    _currentBgpVrfAddressFamilyAggregateNetwork = afConfig.getOrCreateAggregateNetwork(prefix);
  }

  @Override
  public void exitRb_af6_aggregate_address(Rb_af6_aggregate_addressContext ctx) {
    _currentBgpVrfAddressFamilyAggregateNetwork = null;
  }

  @Override
  public void exitRb_af6_network(Rb_af6_networkContext ctx) {
    String mapname = null;
    if (ctx.mapname != null) {
      Optional<String> nameOrError = toString(ctx, ctx.mapname);
      if (!nameOrError.isPresent()) {
        return;
      }
      mapname = nameOrError.get();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, BGP_NETWORK6_ROUTE_MAP, ctx.getStart().getLine());
    }

    Prefix6 prefix = toPrefix6(ctx.network);
    assert _currentBgpVrfIpAddressFamily instanceof BgpVrfIpv6AddressFamilyConfiguration;
    BgpVrfIpv6AddressFamilyConfiguration afConfig =
        (BgpVrfIpv6AddressFamilyConfiguration) _currentBgpVrfIpAddressFamily;
    afConfig.addNetwork(prefix, mapname);
  }

  @Override
  public void exitRb_af6_redistribute_ospfv3(Rb_af6_redistribute_ospfv3Context ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_OSPFV3_ROUTE_MAP, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(RoutingProtocol.OSPF, name, sourceTag);
  }

  @Override
  public void exitRb_afip_aa_tail(Rb_afip_aa_tailContext ctx) {
    int line = ctx.getStart().getLine();
    if (ctx.ADVERTISE_MAP() != null) {
      Optional<String> nameOrError = toString(ctx, ctx.mapname);
      if (!nameOrError.isPresent()) {
        return;
      }
      String name = nameOrError.get();
      _configuration.referenceStructure(ROUTE_MAP, name, BGP_ADVERTISE_MAP, line);
      _currentBgpVrfAddressFamilyAggregateNetwork.setAdvertiseMap(name);
    } else if (ctx.AS_SET() != null) {
      _currentBgpVrfAddressFamilyAggregateNetwork.setAsSet(true);
    } else if (ctx.ATTRIBUTE_MAP() != null) {
      Optional<String> nameOrError = toString(ctx, ctx.mapname);
      if (!nameOrError.isPresent()) {
        return;
      }
      String name = nameOrError.get();
      _configuration.referenceStructure(ROUTE_MAP, name, BGP_ATTRIBUTE_MAP, line);
      _currentBgpVrfAddressFamilyAggregateNetwork.setAttributeMap(name);
    } else if (ctx.SUMMARY_ONLY() != null) {
      _currentBgpVrfAddressFamilyAggregateNetwork.setSummaryOnly(true);
    } else if (ctx.SUPPRESS_MAP() != null) {
      Optional<String> nameOrError = toString(ctx, ctx.mapname);
      if (!nameOrError.isPresent()) {
        return;
      }
      String name = nameOrError.get();
      _configuration.referenceStructure(ROUTE_MAP, name, BGP_SUPPRESS_MAP, line);
      _currentBgpVrfAddressFamilyAggregateNetwork.setSuppressMap(name);
    }
  }

  @Override
  public void exitRb_afip_additional_paths(Rb_afip_additional_pathsContext ctx) {
    todo(ctx);
    if (ctx.mapname != null) {
      toString(ctx, ctx.mapname)
          .ifPresent(
              name ->
                  _configuration.referenceStructure(
                      ROUTE_MAP, name, BGP_ADDITIONAL_PATHS_ROUTE_MAP, ctx.getStart().getLine()));
    }
  }

  @Override
  public void exitRb_afip_client_to_client(Rb_afip_client_to_clientContext ctx) {
    _currentBgpVrfIpAddressFamily.setClientToClientReflection(true);
  }

  @Override
  public void exitRb_afip_dampening(Rb_afip_dampeningContext ctx) {
    todo(ctx);
    if (ctx.mapname != null) {
      toString(ctx, ctx.mapname)
          .ifPresent(
              name ->
                  _configuration.referenceStructure(
                      ROUTE_MAP, name, BGP_DAMPENING_ROUTE_MAP, ctx.getStart().getLine()));
    }
  }

  @Override
  public void exitRb_afip_default_metric(Rb_afip_default_metricContext ctx) {
    long metric = toLong(ctx.metric);
    _currentBgpVrfIpAddressFamily.setDefaultMetric(metric);
  }

  @Override
  public void exitRb_afip_default_information(Rb_afip_default_informationContext ctx) {
    _currentBgpVrfIpAddressFamily.setDefaultInformationOriginate(true);
  }

  @Override
  public void exitRb_afip_distance(Rb_afip_distanceContext ctx) {
    Optional<Integer> ebgp = toInteger(ctx, ctx.ebgp);
    Optional<Integer> ibgp = toInteger(ctx, ctx.ibgp);
    Optional<Integer> local = toInteger(ctx, ctx.local);
    if (!ebgp.isPresent() || !ibgp.isPresent() || !local.isPresent()) {
      return;
    }
    _currentBgpVrfIpAddressFamily.setDistanceEbgp(ebgp.get());
    _currentBgpVrfIpAddressFamily.setDistanceIbgp(ibgp.get());
    _currentBgpVrfIpAddressFamily.setDistanceLocal(local.get());
  }

  @Override
  public void exitRb_afip_inject_map(Rb_afip_inject_mapContext ctx) {
    Optional<String> injectMap = toString(ctx, ctx.injectmap);
    Optional<String> existMap = toString(ctx, ctx.existmap);
    if (!injectMap.isPresent() || !existMap.isPresent()) {
      return;
    }
    todo(ctx);
    _configuration.referenceStructure(
        ROUTE_MAP, injectMap.get(), BGP_INJECT_MAP, ctx.getStart().getLine());
    _configuration.referenceStructure(
        ROUTE_MAP, existMap.get(), BGP_EXIST_MAP, ctx.getStart().getLine());
  }

  @Override
  public void exitRb_afip_maximum_paths(Rb_afip_maximum_pathsContext ctx) {
    Optional<Integer> limitOrError = toInteger(ctx, ctx.numpaths);
    if (!limitOrError.isPresent()) {
      return;
    }
    int limit = limitOrError.get();
    if (ctx.IBGP() != null) {
      _currentBgpVrfIpAddressFamily.setMaximumPathsIbgp(limit);
    } else if (ctx.EIBGP() != null) {
      _currentBgpVrfIpAddressFamily.setMaximumPathsEbgp(limit);
      _currentBgpVrfIpAddressFamily.setMaximumPathsIbgp(limit);
    } else {
      _currentBgpVrfIpAddressFamily.setMaximumPathsEbgp(limit);
    }
  }

  @Override
  public void exitRb_afip_nexthop_route_map(Rb_afip_nexthop_route_mapContext ctx) {
    todo(ctx);
    toString(ctx, ctx.mapname)
        .ifPresent(
            name ->
                _configuration.referenceStructure(
                    ROUTE_MAP, name, BGP_NEXTHOP_ROUTE_MAP, ctx.getStart().getLine()));
  }

  @Override
  public void exitRb_afip_redistribute_direct(Rb_afip_redistribute_directContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_DIRECT_ROUTE_MAP, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(RoutingProtocol.CONNECTED, name, null);
  }

  @Override
  public void exitRb_afip_redistribute_eigrp(Rb_afip_redistribute_eigrpContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_EIGRP_ROUTE_MAP, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(RoutingProtocol.EIGRP, name, sourceTag);
  }

  @Override
  public void exitRb_afip_redistribute_isis(Rb_afip_redistribute_isisContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_ISIS_ROUTE_MAP, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(
        RoutingProtocol.ISIS_ANY, name, sourceTag);
  }

  @Override
  public void exitRb_afip_redistribute_lisp(Rb_afip_redistribute_lispContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_LISP_ROUTE_MAP, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(RoutingProtocol.LISP, name, null);
  }

  @Override
  public void exitRb_afip_redistribute_rip(Rb_afip_redistribute_ripContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_RIP_ROUTE_MAP, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(RoutingProtocol.RIP, name, sourceTag);
  }

  @Override
  public void exitRb_afip_redistribute_static(Rb_afip_redistribute_staticContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_STATIC_ROUTE_MAP, ctx.getStart().getLine());
    _currentBgpVrfIpAddressFamily.setRedistributionPolicy(RoutingProtocol.STATIC, name, null);
  }

  @Override
  public void exitRb_afip_suppress_inactive(Rb_afip_suppress_inactiveContext ctx) {
    _currentBgpVrfIpAddressFamily.setSuppressInactive(true);
  }

  @Override
  public void exitRb_afip_table_map(Rb_afip_table_mapContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _configuration.referenceStructure(ROUTE_MAP, name, BGP_TABLE_MAP, ctx.getStart().getLine());
  }

  @Override
  public void enterRb_af_l2vpn(Rb_af_l2vpnContext ctx) {
    BgpVrfAddressFamilyConfiguration af =
        _currentBgpVrfConfiguration.getOrCreateAddressFamily(Type.L2VPN_EVPN);
    assert af instanceof BgpVrfL2VpnEvpnAddressFamilyConfiguration;
    _currentBgpVrfL2VpnEvpnAddressFamily = (BgpVrfL2VpnEvpnAddressFamilyConfiguration) af;
  }

  @Override
  public void exitRb_af_l2vpn(Rb_af_l2vpnContext ctx) {
    _currentBgpVrfL2VpnEvpnAddressFamily = null;
  }

  @Override
  public void exitRb_afl2v_retain(Rb_afl2v_retainContext ctx) {
    if (ctx.ROUTE_MAP() != null) {
      Optional<String> mapOrError = toString(ctx, ctx.map);
      if (!mapOrError.isPresent()) {
        return;
      }
      String map = mapOrError.get();
      _currentBgpVrfL2VpnEvpnAddressFamily.setRetainMode(RetainRouteType.ROUTE_MAP);
      _currentBgpVrfL2VpnEvpnAddressFamily.setRetainRouteMap(map);
      _configuration.referenceStructure(
          ROUTE_MAP,
          map,
          BGP_L2VPN_EVPN_RETAIN_ROUTE_TARGET_ROUTE_MAP,
          ctx.map.getStart().getLine());
    } else {
      assert ctx.ALL() != null;
      _currentBgpVrfL2VpnEvpnAddressFamily.setRetainMode(RetainRouteType.ALL);
    }
  }

  @Override
  public void exitRb_bestpath(Rb_bestpathContext ctx) {
    if (ctx.ALWAYS_COMPARE_MED() != null) {
      _currentBgpVrfConfiguration.setBestpathAlwaysCompareMed(true);
    } else if (ctx.AS_PATH() != null && ctx.MULTIPATH_RELAX() != null) {
      _currentBgpVrfConfiguration.setBestpathAsPathMultipathRelax(true);
    } else if (ctx.COMPARE_ROUTERID() != null) {
      _currentBgpVrfConfiguration.setBestpathCompareRouterId(true);
    } else if (ctx.COST_COMMUNITY() != null && ctx.IGNORE() != null) {
      _currentBgpVrfConfiguration.setBestpathCostCommunityIgnore(true);
    } else if (ctx.MED() != null && ctx.CONFED() != null) {
      _currentBgpVrfConfiguration.setBestpathMedConfed(true);
    } else if (ctx.MED() != null && ctx.MISSING_AS_WORST() != null) {
      _currentBgpVrfConfiguration.setBestpathMedMissingAsWorst(true);
    } else if (ctx.MED() != null && ctx.NON_DETERMINISTIC() != null) {
      _currentBgpVrfConfiguration.setBestpathMedNonDeterministic(true);
    } else {
      _w.redFlag("Unsupported BGP bestpath configuration: " + ctx.getText());
    }
  }

  @Override
  public void exitRb_cluster_id(Rb_cluster_idContext ctx) {
    if (ctx.ip != null) {
      _currentBgpVrfConfiguration.setClusterId(toIp(ctx.ip));
    } else {
      _currentBgpVrfConfiguration.setClusterId(Ip.create(toLong(ctx.ip_as_int)));
    }
  }

  @Override
  public void exitRb_confederation_identifier(Rb_confederation_identifierContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRb_confederation_peers(Rb_confederation_peersContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRb_enforce_first_as(Rb_enforce_first_asContext ctx) {
    _configuration.getBgpGlobalConfiguration().setEnforceFirstAs(true);
  }

  @Override
  public void exitRb_log_neighbor_changes(Rb_log_neighbor_changesContext ctx) {
    _currentBgpVrfConfiguration.setLogNeighborChanges(true);
  }

  @Override
  public void exitRb_maxas_limit(Rb_maxas_limitContext ctx) {
    toInteger(ctx, ctx.limit).ifPresent(_currentBgpVrfConfiguration::setMaxasLimit);
  }

  @Override
  public void enterRb_neighbor(Rb_neighborContext ctx) {
    if (ctx.ip != null) {
      Ip ip = toIp(ctx.ip);
      _currentBgpVrfNeighbor = _currentBgpVrfConfiguration.getOrCreateNeighbor(ip);
    } else if (ctx.prefix != null) {
      Prefix prefix = toPrefix(ctx.prefix);
      _currentBgpVrfNeighbor = _currentBgpVrfConfiguration.getOrCreatePassiveNeighbor(prefix);
    } else if (ctx.ip6 != null) {
      Ip6 ip = toIp6(ctx.ip6);
      _currentBgpVrfNeighbor = _currentBgpVrfConfiguration.getOrCreateNeighbor(ip);
    } else if (ctx.prefix6 != null) {
      Prefix6 prefix = toPrefix6(ctx.prefix6);
      _currentBgpVrfNeighbor = _currentBgpVrfConfiguration.getOrCreatePassiveNeighbor(prefix);
    } else {
      throw new BatfishException(
          "BGP neighbor IP definition not supported in line " + ctx.getText());
    }

    if (ctx.REMOTE_AS() != null && ctx.bgp_asn() != null) {
      long asn = toLong(ctx.bgp_asn());
      _currentBgpVrfNeighbor.setRemoteAs(asn);
    }

    if (ctx.REMOTE_AS() != null && ctx.ROUTE_MAP() != null) {
      Optional<String> nameOrError = toString(ctx, ctx.mapname);
      if (!nameOrError.isPresent()) {
        return;
      }
      String name = nameOrError.get();
      _currentBgpVrfNeighbor.setRemoteAsRouteMap(name);
      _configuration.referenceStructure(
          ROUTE_MAP, name, BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRb_neighbor(Rb_neighborContext ctx) {
    _currentBgpVrfNeighbor = null;
  }

  @Override
  public void enterRb_n_address_family(Rb_n_address_familyContext ctx) {
    String familyStr = ctx.first.getText() + '-' + ctx.second.getText();
    _currentBgpVrfNeighborAddressFamily =
        _currentBgpVrfNeighbor.getOrCreateAddressFamily(familyStr);
  }

  @Override
  public void exitRb_n_address_family(Rb_n_address_familyContext ctx) {
    _currentBgpVrfNeighborAddressFamily = null;
  }

  @Override
  public void exitRb_n_af_advertise_map(Rb_n_af_advertise_mapContext ctx) {
    Optional<String> advMap = toString(ctx, ctx.mapname);
    Optional<String> existMap =
        ctx.EXIST_MAP() != null ? toString(ctx, ctx.existmap) : Optional.empty();
    Optional<String> nonExistMap =
        ctx.NON_EXIST_MAP() != null ? toString(ctx, ctx.nonexistmap) : Optional.empty();
    if (!advMap.isPresent()
        || ctx.EXIST_MAP() != null && !existMap.isPresent()
        || ctx.NON_EXIST_MAP() != null && !nonExistMap.isPresent()) {
      return;
    }
    todo(ctx);
    _configuration.referenceStructure(
        ROUTE_MAP, advMap.get(), BGP_NEIGHBOR_ADVERTISE_MAP, ctx.getStart().getLine());
    existMap.ifPresent(
        name ->
            _configuration.referenceStructure(
                ROUTE_MAP, name, BGP_NEIGHBOR_EXIST_MAP, ctx.getStart().getLine()));
    nonExistMap.ifPresent(
        name ->
            _configuration.referenceStructure(
                ROUTE_MAP, name, BGP_NEIGHBOR_NON_EXIST_MAP, ctx.getStart().getLine()));
  }

  @Override
  public void exitRb_n_af_allowas_in(Rb_n_af_allowas_inContext ctx) {
    if (ctx.num != null) {
      todo(ctx);
    }
    _currentBgpVrfNeighborAddressFamily.setAllowAsIn(true);
  }

  @Override
  public void exitRb_n_af_as_override(Rb_n_af_as_overrideContext ctx) {
    _currentBgpVrfNeighborAddressFamily.setAsOverride(true);
  }

  @Override
  public void exitRb_n_af_default_originate(Rb_n_af_default_originateContext ctx) {
    if (ctx.ROUTE_MAP() != null) {
      Optional<String> nameOrError = toString(ctx, ctx.mapname);
      if (!nameOrError.isPresent()) {
        return;
      }
      String name = nameOrError.get();
      _currentBgpVrfNeighborAddressFamily.setDefaultOriginateMap(name);
      _configuration.referenceStructure(
          ROUTE_MAP, name, BGP_DEFAULT_ORIGINATE_ROUTE_MAP, ctx.getStart().getLine());
    }
    _currentBgpVrfNeighborAddressFamily.setDefaultOriginate(true);
  }

  @Override
  public void exitRb_n_af_disable_peer_as_check(Rb_n_af_disable_peer_as_checkContext ctx) {
    _currentBgpVrfNeighborAddressFamily.setDisablePeerAsCheck(true);
  }

  @Override
  public void exitRb_n_af_filter_list(Rb_n_af_filter_listContext ctx) {
    Optional<String> filterList = toString(ctx, ctx.name);
    if (!filterList.isPresent()) {
      return;
    }
    todo(ctx);
    CiscoNxosStructureUsage usage =
        _inIpv6BgpPeer
            ? ((ctx.IN() != null) ? BGP_NEIGHBOR6_FILTER_LIST_IN : BGP_NEIGHBOR6_FILTER_LIST_OUT)
            : ((ctx.IN() != null) ? BGP_NEIGHBOR_FILTER_LIST_IN : BGP_NEIGHBOR_FILTER_LIST_OUT);
    _configuration.referenceStructure(
        IP_AS_PATH_ACCESS_LIST, filterList.get(), usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRb_n_af_inherit(Rb_n_af_inheritContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.template);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    Optional<Integer> seqOrError = toInteger(ctx, ctx.seq);
    if (!seqOrError.isPresent()) {
      return;
    }
    int sequence = seqOrError.get();
    _currentBgpVrfNeighborAddressFamily.setInheritPeerPolicy(sequence, name);
    _configuration.referenceStructure(
        BGP_TEMPLATE_PEER_POLICY, name, BGP_NEIGHBOR_INHERIT_PEER_POLICY, ctx.getStart().getLine());
  }

  @Override
  public void exitRb_n_af_next_hop_self(Rb_n_af_next_hop_selfContext ctx) {
    _currentBgpVrfNeighborAddressFamily.setNextHopSelf(true);
  }

  @Override
  public void exitRb_n_af_next_hop_third_party(Rb_n_af_next_hop_third_partyContext ctx) {
    _currentBgpVrfNeighborAddressFamily.setNextHopThirdParty(true);
  }

  @Override
  public void exitRb_n_af_prefix_list(Rb_n_af_prefix_listContext ctx) {
    Optional<String> prefixList = toString(ctx, ctx.listname);
    if (!prefixList.isPresent()) {
      return;
    }
    todo(ctx);
    CiscoNxosStructureType type = _inIpv6BgpPeer ? IPV6_PREFIX_LIST : IP_PREFIX_LIST;
    CiscoNxosStructureUsage usage =
        _inIpv6BgpPeer
            ? ((ctx.IN() != null) ? BGP_NEIGHBOR6_PREFIX_LIST_IN : BGP_NEIGHBOR6_PREFIX_LIST_OUT)
            : ((ctx.IN() != null) ? BGP_NEIGHBOR_PREFIX_LIST_IN : BGP_NEIGHBOR_PREFIX_LIST_OUT);
    _configuration.referenceStructure(type, prefixList.get(), usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRb_n_af_route_map(Rb_n_af_route_mapContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    CiscoNxosStructureUsage usage;
    if (ctx.IN() != null) {
      usage = BGP_NEIGHBOR_ROUTE_MAP_IN;
      _currentBgpVrfNeighborAddressFamily.setInboundRouteMap(name);
    } else {
      usage = BGP_NEIGHBOR_ROUTE_MAP_OUT;
      _currentBgpVrfNeighborAddressFamily.setOutboundRouteMap(name);
    }
    _configuration.referenceStructure(ROUTE_MAP, name, usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRb_n_af_route_reflector_client(Rb_n_af_route_reflector_clientContext ctx) {
    _currentBgpVrfNeighborAddressFamily.setRouteReflectorClient(true);
  }

  @Override
  public void exitRb_n_af_send_community(Rb_n_af_send_communityContext ctx) {
    if (ctx.BOTH() != null || ctx.EXTENDED() != null) {
      _currentBgpVrfNeighborAddressFamily.setSendCommunityExtended(true);
    }
    if (ctx.BOTH() != null || ctx.STANDARD() != null || ctx.EXTENDED() == null) {
      _currentBgpVrfNeighborAddressFamily.setSendCommunityStandard(true);
    }
  }

  @Override
  public void exitRb_n_af_suppress_inactive(Rb_n_af_suppress_inactiveContext ctx) {
    _currentBgpVrfNeighborAddressFamily.setSuppressInactive(true);
  }

  @Override
  public void exitRb_n_af_unsuppress_map(Rb_n_af_unsuppress_mapContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.mapname);
    if (!nameOrError.isPresent()) {
      return;
    }
    todo(ctx);
    String name = nameOrError.get();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_UNSUPPRESS_MAP, ctx.getStart().getLine());
  }

  @Override
  public void exitRb_n_description(Rb_n_descriptionContext ctx) {
    Optional<String> desc =
        toStringWithLengthInSpace(
            ctx, ctx.desc, BGP_NEIGHBOR_DESCRIPTION_LENGTH_RANGE, "bgp neighbor description");
    desc.ifPresent(_currentBgpVrfNeighbor::setDescription);
  }

  @Override
  public void exitRb_n_ebgp_multihop(Rb_n_ebgp_multihopContext ctx) {
    Optional<Integer> ttlOrError = toInteger(ctx, ctx.ebgp_ttl);
    if (!ttlOrError.isPresent()) {
      return;
    }
    _currentBgpVrfNeighbor.setEbgpMultihopTtl(ttlOrError.get());
  }

  @Override
  public void exitRb_n_inherit(Rb_n_inheritContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.peer);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    if (ctx.PEER() != null) {
      _currentBgpVrfNeighbor.setInheritPeer(name);
      _configuration.referenceStructure(
          BGP_TEMPLATE_PEER, name, BGP_NEIGHBOR_INHERIT_PEER, ctx.getStart().getLine());
    } else {
      _currentBgpVrfNeighbor.setInheritPeerSession(name);
      _configuration.referenceStructure(
          BGP_TEMPLATE_PEER_SESSION,
          name,
          BGP_NEIGHBOR_INHERIT_PEER_SESSION,
          ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRb_n_local_as(Rb_n_local_asContext ctx) {
    long asn = toLong(ctx.bgp_asn());
    _currentBgpVrfNeighbor.setLocalAs(asn);
  }

  @Override
  public void exitRb_n_no_shutdown(Rb_n_no_shutdownContext ctx) {
    _currentBgpVrfNeighbor.setShutdown(false);
  }

  @Override
  public void exitRb_n_remote_as(Rb_n_remote_asContext ctx) {
    long asn = toLong(ctx.bgp_asn());
    _currentBgpVrfNeighbor.setRemoteAs(asn);
  }

  @Override
  public void exitRb_n_remove_private_as(Rb_n_remove_private_asContext ctx) {
    if (ctx.ALL() != null) {
      _currentBgpVrfNeighbor.setRemovePrivateAs(RemovePrivateAsMode.ALL);
    } else if (ctx.REPLACE_AS() != null) {
      _currentBgpVrfNeighbor.setRemovePrivateAs(RemovePrivateAsMode.REPLACE_AS);
    }
  }

  @Override
  public void exitRb_n_shutdown(Rb_n_shutdownContext ctx) {
    _currentBgpVrfNeighbor.setShutdown(true);
  }

  @Override
  public void exitRb_n_update_source(Rb_n_update_sourceContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.interface_name());
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _currentBgpVrfNeighbor.setUpdateSource(name);
    _configuration.referenceStructure(
        INTERFACE, name, BGP_NEIGHBOR_UPDATE_SOURCE, ctx.getStart().getLine());
  }

  @Override
  public void exitRb_no_enforce_first_as(Rb_no_enforce_first_asContext ctx) {
    _configuration.getBgpGlobalConfiguration().setEnforceFirstAs(false);
  }

  @Override
  public void exitRb_router_id(Rb_router_idContext ctx) {
    Ip ip = toIp(ctx.ip_address());
    _currentBgpVrfConfiguration.setRouterId(ip);
  }

  @Override
  public void enterRb_template_peer(Rb_template_peerContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.peer);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _currentBgpVrfNeighbor =
        _configuration.getBgpGlobalConfiguration().getOrCreateTemplatePeer(name);
    _configuration.defineStructure(BGP_TEMPLATE_PEER, name, ctx);
  }

  @Override
  public void exitRb_template_peer(Rb_template_peerContext ctx) {
    _currentBgpVrfNeighbor = null;
  }

  @Override
  public void enterRb_template_peer_policy(Rb_template_peer_policyContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.policy);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _currentBgpVrfNeighborAddressFamily =
        _configuration.getBgpGlobalConfiguration().getOrCreateTemplatePeerPolicy(name);
    _configuration.defineStructure(BGP_TEMPLATE_PEER_POLICY, name, ctx);
  }

  @Override
  public void exitRb_template_peer_policy(Rb_template_peer_policyContext ctx) {
    _currentBgpVrfNeighborAddressFamily = null;
  }

  @Override
  public void enterRb_template_peer_session(Rb_template_peer_sessionContext ctx) {
    Optional<String> nameOrError = toString(ctx, ctx.session);
    if (!nameOrError.isPresent()) {
      return;
    }
    String name = nameOrError.get();
    _currentBgpVrfNeighbor =
        _configuration.getBgpGlobalConfiguration().getOrCreateTemplatePeerSession(name);
    _configuration.defineStructure(BGP_TEMPLATE_PEER_SESSION, name, ctx);
  }

  @Override
  public void exitRb_template_peer_session(Rb_template_peer_sessionContext ctx) {
    _currentBgpVrfNeighbor = null;
  }

  @Override
  public void enterRb_vrf(Rb_vrfContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      // Dummy BGP config so inner stuff works.
      _currentBgpVrfConfiguration = new BgpVrfConfiguration();
      return;
    }
    _currentBgpVrfConfiguration =
        _configuration.getBgpGlobalConfiguration().getOrCreateVrf(nameOrErr.get());
  }

  @Override
  public void exitRb_vrf(Rb_vrfContext ctx) {
    _currentBgpVrfConfiguration =
        _configuration.getBgpGlobalConfiguration().getOrCreateVrf(DEFAULT_VRF_NAME);
  }

  @Override
  public void exitRb_v_local_as(Rb_v_local_asContext ctx) {
    long asNum = toLong(ctx.bgp_asn());
    _currentBgpVrfConfiguration.setLocalAs(asNum);
  }

  @Override
  public void enterRouter_bgp(Router_bgpContext ctx) {
    _currentBgpVrfConfiguration =
        _configuration.getBgpGlobalConfiguration().getOrCreateVrf(DEFAULT_VRF_NAME);
    _configuration.getBgpGlobalConfiguration().setLocalAs(toLong(ctx.bgp_asn()));
  }

  @Override
  public void exitRouter_bgp(Router_bgpContext ctx) {
    _currentBgpVrfConfiguration = null;
  }

  @Override
  public void enterS_evpn(S_evpnContext ctx) {
    // TODO: check feature presence
    if (_configuration.getEvpn() == null) {
      _configuration.setEvpn(new Evpn());
    }
  }

  @Override
  public void enterS_interface_nve(S_interface_nveContext ctx) {
    int line = ctx.getStart().getLine();
    int first = toInteger(ctx.nverange.iname.first);
    int last = ctx.nverange.last != null ? toInteger(ctx.nverange.last) : first;
    // flip first and last if range is backwards
    if (last < first) {
      int tmp = last;
      last = first;
      first = tmp;
    }

    _currentNves =
        IntStream.range(first, last + 1)
            .mapToObj(
                i ->
                    _configuration
                        .getNves()
                        .computeIfAbsent(
                            i,
                            n -> {
                              String nveName = "nve" + i;
                              _configuration.defineStructure(NVE, nveName, ctx);
                              _configuration.referenceStructure(
                                  NVE, nveName, NVE_SELF_REFERENCE, line);
                              return new Nve(i);
                            }))
            .collect(ImmutableList.toImmutableList());
  }

  @Override
  public void exitS_interface_nve(S_interface_nveContext ctx) {
    _currentNves = null;
  }

  @Override
  public void enterS_interface_regular(S_interface_regularContext ctx) {
    int line = ctx.getStart().getLine();
    String declaredName = getFullText(ctx.irange);
    String prefix = ctx.irange.iname.prefix.getText();
    CiscoNxosInterfaceType type = toType(ctx.irange.iname.prefix);
    if (type == null) {
      _w.redFlag(String.format("Unsupported interface type: %s", prefix));
      _currentInterfaces = ImmutableList.of();
      return;
    }
    String canonicalPrefix = getCanonicalInterfaceNamePrefix(prefix);
    if (canonicalPrefix == null) {
      _w.redFlag(String.format("Unsupported interface name/range: %s", declaredName));
      _currentInterfaces = ImmutableList.of();
      return;
    }
    String middle = ctx.irange.iname.middle != null ? ctx.irange.iname.middle.getText() : "";
    String parentSuffix =
        ctx.irange.iname.parent_suffix != null ? ctx.irange.iname.parent_suffix.getText() : "";
    String lead = String.format("%s%s%s", canonicalPrefix, middle, parentSuffix);
    String parentInterface =
        parentSuffix.isEmpty()
            ? null
            : String.format(
                "%s%s%s", canonicalPrefix, middle, ctx.irange.iname.parent_suffix.num.getText());
    int first = toInteger(ctx.irange.iname.first);
    int last = ctx.irange.last != null ? toInteger(ctx.irange.last) : first;

    // flip first and last if range is backwards
    if (last < first) {
      int tmp = last;
      last = first;
      first = tmp;
    }

    // disallow subinterfaces except for physical and port-channel interfaces
    if (type != CiscoNxosInterfaceType.ETHERNET
        && type != CiscoNxosInterfaceType.PORT_CHANNEL
        && parentInterface != null) {
      _w.redFlag(
          String.format(
              "Cannot construct subinterface for interface type '%s' in: %s",
              type, getFullText(ctx)));
      _currentInterfaces = ImmutableList.of();
      return;
    }

    // Validate VLAN numbers
    if (type == CiscoNxosInterfaceType.VLAN
        && !_currentValidVlanRange.contains(IntegerSpace.of(Range.closed(first, last)))) {
      _w.redFlag(
          String.format(
              "Vlan number(s) outside of range %s in Vlan interface declaration: %s",
              _currentValidVlanRange, getFullText(ctx)));
      _currentInterfaces = ImmutableList.of();
      return;
    }

    // Validate port-channel numbers
    if (type == CiscoNxosInterfaceType.PORT_CHANNEL
        && !PORT_CHANNEL_RANGE.contains(IntegerSpace.of(Range.closed(first, last)))) {
      _w.redFlag(
          String.format(
              "port-channel number(s) outside of range %s in port-channel interface declaration: %s",
              PORT_CHANNEL_RANGE, getFullText(ctx)));
      _currentInterfaces = ImmutableList.of();
      return;
    }

    _currentInterfaces =
        IntStream.range(first, last + 1)
            .mapToObj(
                i -> {
                  String ifaceName = lead + i;
                  return _configuration
                      .getInterfaces()
                      .computeIfAbsent(
                          ifaceName,
                          n -> {
                            _configuration.defineStructure(INTERFACE, n, ctx);
                            _configuration.referenceStructure(
                                INTERFACE, n, INTERFACE_SELF_REFERENCE, line);
                            if (type == CiscoNxosInterfaceType.VLAN) {
                              _configuration.referenceStructure(
                                  VLAN, Integer.toString(i), INTERFACE_VLAN, line);
                              return newVlanInterface(n, i);
                            } else {
                              if (type == CiscoNxosInterfaceType.PORT_CHANNEL) {
                                _configuration.defineStructure(PORT_CHANNEL, n, ctx);
                              }
                              return newNonVlanInterface(n, parentInterface, type);
                            }
                          });
                })
            .collect(ImmutableList.toImmutableList());
    _currentInterfaces.forEach(i -> i.getDeclaredNames().add(declaredName));
  }

  @Override
  public void exitS_interface_regular(S_interface_regularContext ctx) {
    _currentInterfaces = null;
  }

  @Override
  public void enterS_route_map(S_route_mapContext ctx) {
    _currentRouteMapName = toString(ctx, ctx.name);
  }

  @Override
  public void exitS_route_map(S_route_mapContext ctx) {
    _currentRouteMapName = null;
  }

  @Override
  public void enterRoute_map_entry(Route_map_entryContext ctx) {
    if (!_currentRouteMapName.isPresent()) {
      _currentRouteMapEntry = new RouteMapEntry(1); // dummy
      return;
    }
    String name = _currentRouteMapName.get();
    int sequence;
    if (ctx.sequence != null) {
      Optional<Integer> seqOpt = toInteger(ctx, ctx.sequence);
      if (!seqOpt.isPresent()) {
        return;
      }
      sequence = seqOpt.get();
    } else {
      sequence = 10;
    }
    _currentRouteMapEntry =
        _configuration
            .getRouteMaps()
            .computeIfAbsent(name, RouteMap::new)
            .getEntries()
            .computeIfAbsent(sequence, RouteMapEntry::new);
    _currentRouteMapEntry.setAction(toLineAction(ctx.action));

    _configuration.defineStructure(ROUTE_MAP, name, ctx.parent);
    _configuration.defineStructure(ROUTE_MAP_ENTRY, Integer.toString(sequence), ctx.parent);
  }

  @Override
  public void exitRoute_map_entry(Route_map_entryContext ctx) {
    _currentRouteMapEntry = null;
  }

  @Override
  public void exitRoute_map_pbr_statistics(Route_map_pbr_statisticsContext ctx) {
    if (!_currentRouteMapName.isPresent()) {
      return;
    }
    _configuration
        .getRouteMaps()
        .computeIfAbsent(
            _currentRouteMapName.get(),
            name -> {
              _configuration.defineStructure(ROUTE_MAP, name, ctx.parent);
              return new RouteMap(name);
            })
        .setPbrStatistics(true);
  }

  @Override
  public void exitS_track(S_trackContext ctx) {
    // TODO: support object tracking
    todo(ctx);
  }

  @Override
  public void exitS_version(S_versionContext ctx) {
    _configuration.setVersion(ctx.version.getText());
  }

  @Override
  public void enterS_vrf_context(S_vrf_contextContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      _currentVrf = new Vrf("dummy");
      return;
    }
    _currentVrf =
        _configuration
            .getVrfs()
            .computeIfAbsent(
                nameOrErr.get(),
                name -> {
                  _configuration.defineStructure(VRF, name, ctx);
                  return new Vrf(name);
                });
  }

  @Override
  public void enterVlan_vlan(Vlan_vlanContext ctx) {
    IntegerSpace vlans = toVlanIdRange(ctx, ctx.vlans);
    if (vlans == null) {
      _currentVlans = ImmutableList.of();
      return;
    }
    _currentVlans =
        vlans.stream()
            .map(
                vlanId ->
                    _configuration
                        .getVlans()
                        .computeIfAbsent(
                            vlanId,
                            id -> {
                              _configuration.defineStructure(VLAN, Integer.toString(id), ctx);
                              return new Vlan(id);
                            }))
            .collect(ImmutableList.toImmutableList());
  }

  @Override
  public void exitAcl_fragments(Acl_fragmentsContext ctx) {
    if (ctx.deny != null) {
      _currentIpAccessList.setFragmentsBehavior(FragmentsBehavior.DENY_ALL);
    } else {
      _currentIpAccessList.setFragmentsBehavior(FragmentsBehavior.PERMIT_ALL);
    }
  }

  @Override
  public void exitAcl_line(Acl_lineContext ctx) {
    _currentIpAccessListLineNum = null;
  }

  @Override
  public void exitAcll_action(Acll_actionContext ctx) {
    _currentIpAccessListLineNum.ifPresent(
        num -> {
          IpAccessListLine line;
          if (_currentActionIpAccessListLineUnusable) {
            // unsupported, so just add current line as a remark
            line = new RemarkIpAccessListLine(num, getFullText(ctx.getParent()));
          } else {
            line =
                _currentActionIpAccessListLineBuilder
                    .setL3Options(_currentLayer3OptionsBuilder.build())
                    .build();
          }

          _currentIpAccessList.getLines().put(num, line);
        });
    _currentActionIpAccessListLineBuilder = null;
    _currentActionIpAccessListLineUnusable = null;
    _currentLayer3OptionsBuilder = null;
  }

  @Override
  public void exitAcll_remark(Acll_remarkContext ctx) {
    _currentIpAccessListLineNum.ifPresent(
        num ->
            _currentIpAccessList
                .getLines()
                .put(num, new RemarkIpAccessListLine(num, ctx.text.getText())));
  }

  @Override
  public void exitAcllal3_dst_address(Acllal3_dst_addressContext ctx) {
    _currentActionIpAccessListLineBuilder.setDstAddressSpec(toAddressSpec(ctx.addr));
    if (ctx.addr.group != null) {
      // TODO: name validation
      String name = ctx.addr.group.getText();
      _configuration.referenceStructure(
          OBJECT_GROUP_IP_ADDRESS,
          name,
          IP_ACCESS_LIST_DESTINATION_ADDRGROUP,
          ctx.getStart().getLine());
    }
  }

  @Override
  public void exitAcllal3_fragments(Acllal3_fragmentsContext ctx) {
    _currentActionIpAccessListLineBuilder.setFragments(true);
  }

  @Override
  public void exitAcllal3_protocol_spec(Acllal3_protocol_specContext ctx) {
    if (ctx.prot != null) {
      _currentActionIpAccessListLineBuilder.setProtocol(toIpProtocol(ctx.prot));
    }
  }

  @Override
  public void exitAcllal3_src_address(Acllal3_src_addressContext ctx) {
    _currentActionIpAccessListLineBuilder.setSrcAddressSpec(toAddressSpec(ctx.addr));
    if (ctx.addr.group != null) {
      // TODO: name validation
      String name = ctx.addr.group.getText();
      _configuration.referenceStructure(
          OBJECT_GROUP_IP_ADDRESS, name, IP_ACCESS_LIST_SOURCE_ADDRGROUP, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitAcllal3o_dscp(Acllal3o_dscpContext ctx) {
    Optional<Integer> dscp = toInteger(ctx, ctx.dscp);
    if (!dscp.isPresent()) {
      _currentActionIpAccessListLineUnusable = true;
      return;
    } else {
      _currentLayer3OptionsBuilder.setDscp(dscp.get());
    }
  }

  @Override
  public void exitAcllal3o_log(Acllal3o_logContext ctx) {
    _currentActionIpAccessListLineBuilder.setLog(true);
  }

  @Override
  public void exitAcllal3o_packet_length(Acllal3o_packet_lengthContext ctx) {
    Optional<IntegerSpace> spec = toIntegerSpace(ctx, ctx.spec);
    if (!spec.isPresent()) {
      _currentActionIpAccessListLineUnusable = true;
    } else {
      _currentLayer3OptionsBuilder.setPacketLength(spec.get());
    }
  }

  @Override
  public void exitAcllal3o_precedence(Acllal3o_precedenceContext ctx) {
    // TODO: discover and implement precedence numbers for NX-OS ACL precedence option
    todo(ctx);
    _currentActionIpAccessListLineUnusable = true;
  }

  @Override
  public void exitAcllal3o_ttl(Acllal3o_ttlContext ctx) {
    _currentLayer3OptionsBuilder.setTtl(toInteger(ctx.num));
  }

  @Override
  public void exitAcllal4_icmp(Acllal4_icmpContext ctx) {
    _currentActionIpAccessListLineBuilder.setProtocol(IpProtocol.ICMP);
  }

  @Override
  public void exitAcllal4_tcp(Acllal4_tcpContext ctx) {
    if (_currentTcpFlagsBuilder != null) {
      _currentTcpOptionsBuilder.setTcpFlags(_currentTcpFlagsBuilder.build());
      _currentTcpFlagsBuilder = null;
    }
    _currentActionIpAccessListLineBuilder.setL4Options(_currentTcpOptionsBuilder.build());
    _currentTcpOptionsBuilder = null;
  }

  @Override
  public void exitAcllal4_udp(Acllal4_udpContext ctx) {
    _currentActionIpAccessListLineBuilder.setL4Options(_currentUdpOptionsBuilder.build());
    _currentUdpOptionsBuilder = null;
  }

  @Override
  public void exitAcllal4icmp_option(Acllal4icmp_optionContext ctx) {
    // See https://www.iana.org/assignments/icmp-parameters/icmp-parameters.xhtml
    Integer type = null;
    Integer code = null;
    if (ctx.type != null) {
      type = toInteger(ctx.type);
      if (ctx.code != null) {
        code = toInteger(ctx.code);
      }
    } else if (ctx.ADMINISTRATIVELY_PROHIBITED() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.COMMUNICATION_ADMINISTRATIVELY_PROHIBITED;
    } else if (ctx.ALTERNATE_ADDRESS() != null) {
      type = IcmpType.ALTERNATE_ADDRESS;
    } else if (ctx.CONVERSION_ERROR() != null) {
      type = IcmpType.CONVERSION_ERROR;
    } else if (ctx.DOD_HOST_PROHIBITED() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.DESTINATION_HOST_PROHIBITED;
    } else if (ctx.DOD_NET_PROHIBITED() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.DESTINATION_NETWORK_PROHIBITED;
    } else if (ctx.ECHO() != null) {
      type = IcmpType.ECHO_REQUEST;
    } else if (ctx.ECHO_REPLY() != null) {
      type = IcmpType.ECHO_REPLY;
    } else if (ctx.GENERAL_PARAMETER_PROBLEM() != null) {
      // Interpreting as type 12 (parameter problem), unrestricted code
      type = IcmpType.PARAMETER_PROBLEM;
    } else if (ctx.HOST_ISOLATED() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.SOURCE_HOST_ISOLATED;
    } else if (ctx.HOST_PRECEDENCE_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.HOST_PRECEDENCE_VIOLATION;
    } else if (ctx.HOST_REDIRECT() != null) {
      type = IcmpType.REDIRECT_MESSAGE;
      code = IcmpCode.HOST_ERROR;
    } else if (ctx.HOST_TOS_REDIRECT() != null) {
      type = IcmpType.REDIRECT_MESSAGE;
      code = IcmpCode.TOS_AND_HOST_ERROR;
    } else if (ctx.HOST_TOS_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.HOST_UNREACHABLE_FOR_TOS;
    } else if (ctx.HOST_UNKNOWN() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.DESTINATION_HOST_UNKNOWN;
    } else if (ctx.HOST_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.HOST_UNREACHABLE;
    } else if (ctx.INFORMATION_REPLY() != null) {
      type = IcmpType.INFO_REPLY;
    } else if (ctx.INFORMATION_REQUEST() != null) {
      type = IcmpType.INFO_REQUEST;
    } else if (ctx.MASK_REPLY() != null) {
      type = IcmpType.MASK_REPLY;
    } else if (ctx.MASK_REQUEST() != null) {
      type = IcmpType.MASK_REQUEST;
    } else if (ctx.MOBILE_REDIRECT() != null) {
      type = IcmpType.MOBILE_REDIRECT;
    } else if (ctx.NET_REDIRECT() != null) {
      type = IcmpType.REDIRECT_MESSAGE;
      code = IcmpCode.NETWORK_ERROR;
    } else if (ctx.NET_TOS_REDIRECT() != null) {
      type = IcmpType.REDIRECT_MESSAGE;
      code = IcmpCode.TOS_AND_NETWORK_ERROR;
    } else if (ctx.NET_TOS_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.NETWORK_UNREACHABLE_FOR_TOS;
    } else if (ctx.NET_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.NETWORK_UNREACHABLE;
    } else if (ctx.NETWORK_UNKNOWN() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.DESTINATION_NETWORK_UNKNOWN;
    } else if (ctx.NO_ROOM_FOR_OPTION() != null) {
      type = IcmpType.PARAMETER_PROBLEM;
      code = IcmpCode.BAD_LENGTH;
    } else if (ctx.OPTION_MISSING() != null) {
      type = IcmpType.PARAMETER_PROBLEM;
      code = IcmpCode.REQUIRED_OPTION_MISSING;
    } else if (ctx.PACKET_TOO_BIG() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.FRAGMENTATION_NEEDED;
    } else if (ctx.PARAMETER_PROBLEM() != null) {
      type = IcmpType.PARAMETER_PROBLEM;
      code = IcmpCode.INVALID_IP_HEADER;
    } else if (ctx.PORT_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.PORT_UNREACHABLE;
    } else if (ctx.PRECEDENCE_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.PRECEDENCE_CUTOFF_IN_EFFECT;
    } else if (ctx.PROTOCOL_UNREACHABLE() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.PROTOCOL_UNREACHABLE;
    } else if (ctx.REASSEMBLY_TIMEOUT() != null) {
      type = IcmpType.TIME_EXCEEDED;
      code = IcmpCode.TIME_EXCEEDED_DURING_FRAGMENT_REASSEMBLY;
    } else if (ctx.REDIRECT() != null) {
      // interpreting as unrestricted type 5 (redirect)
      type = IcmpType.REDIRECT_MESSAGE;
    } else if (ctx.ROUTER_ADVERTISEMENT() != null) {
      // interpreting as unrestricted type 9 (router advertisement)
      type = IcmpType.ROUTER_ADVERTISEMENT;
    } else if (ctx.ROUTER_SOLICITATION() != null) {
      type = IcmpType.ROUTER_SOLICITATION;
    } else if (ctx.SOURCE_QUENCH() != null) {
      type = IcmpType.SOURCE_QUENCH;
    } else if (ctx.SOURCE_ROUTE_FAILED() != null) {
      type = IcmpType.DESTINATION_UNREACHABLE;
      code = IcmpCode.SOURCE_ROUTE_FAILED;
    } else if (ctx.TIME_EXCEEDED() != null) {
      // interpreting as unrestricted type 11 (time exceeded)
      type = IcmpType.TIME_EXCEEDED;
    } else if (ctx.TIMESTAMP_REPLY() != null) {
      type = IcmpType.TIMESTAMP_REPLY;
    } else if (ctx.TIMESTAMP_REQUEST() != null) {
      type = IcmpType.TIMESTAMP_REQUEST;
    } else if (ctx.TRACEROUTE() != null) {
      type = IcmpType.TRACEROUTE;
    } else if (ctx.TTL_EXCEEDED() != null) {
      type = IcmpType.TIME_EXCEEDED;
      code = IcmpCode.TTL_EQ_ZERO_DURING_TRANSIT;
    } else if (ctx.UNREACHABLE() != null) {
      // interpreting as unrestricted type 3 (destination unreachable)
      type = IcmpType.DESTINATION_UNREACHABLE;
    } else {
      // assume valid but unsupported
      todo(ctx);
      _currentActionIpAccessListLineUnusable = true;
    }
    if (_currentActionIpAccessListLineUnusable) {
      return;
    }
    _currentActionIpAccessListLineBuilder.setL4Options(new IcmpOptions(type, code));
  }

  @Override
  public void exitAcllal4igmp_option(Acllal4igmp_optionContext ctx) {
    // TODO: discover and implement IGMP message types/codes for NX-OS ACL IGMP options
    todo(ctx);
    _currentActionIpAccessListLineUnusable = true;
  }

  @Override
  public void exitAcllal4tcp_destination_port(Acllal4tcp_destination_portContext ctx) {
    Optional<PortSpec> portSpec = toPortSpec(ctx, ctx.port);
    if (!portSpec.isPresent()) {
      _currentActionIpAccessListLineUnusable = true;
    } else {
      _currentTcpOptionsBuilder.setDstPortSpec(portSpec.get());
    }
  }

  @Override
  public void exitAcllal4tcp_source_port(Acllal4tcp_source_portContext ctx) {
    Optional<PortSpec> portSpec = toPortSpec(ctx, ctx.port);
    if (!portSpec.isPresent()) {
      _currentActionIpAccessListLineUnusable = true;
    } else {
      _currentTcpOptionsBuilder.setSrcPortSpec(portSpec.get());
    }
  }

  @Override
  public void exitAcllal4tcpo_established(Acllal4tcpo_establishedContext ctx) {
    _currentTcpOptionsBuilder.setEstablished(true);
  }

  @Override
  public void exitAcllal4tcpo_flags(Acllal4tcpo_flagsContext ctx) {
    if (_currentTcpFlagsBuilder == null) {
      _currentTcpFlagsBuilder = TcpFlags.builder();
    }
    if (ctx.ACK() != null) {
      _currentTcpFlagsBuilder.setAck(true);
    } else if (ctx.FIN() != null) {
      _currentTcpFlagsBuilder.setFin(true);
    } else if (ctx.PSH() != null) {
      _currentTcpFlagsBuilder.setPsh(true);
    } else if (ctx.RST() != null) {
      _currentTcpFlagsBuilder.setRst(true);
    } else if (ctx.SYN() != null) {
      _currentTcpFlagsBuilder.setSyn(true);
    } else if (ctx.URG() != null) {
      _currentTcpFlagsBuilder.setUrg(true);
    } else {
      // assume valid but unsupported
      todo(ctx);
      _currentActionIpAccessListLineUnusable = true;
    }
  }

  @Override
  public void exitAcllal4tcpo_tcp_flags_mask(Acllal4tcpo_tcp_flags_maskContext ctx) {
    Optional<Integer> mask = toInteger(ctx, ctx.mask);
    if (!mask.isPresent()) {
      _currentActionIpAccessListLineUnusable = true;
    } else {
      _currentTcpOptionsBuilder.setTcpFlagsMask(mask.get());
    }
  }

  @Override
  public void exitAcllal4udp_destination_port(Acllal4udp_destination_portContext ctx) {
    Optional<PortSpec> portSpec = toPortSpec(ctx, ctx.port);
    if (!portSpec.isPresent()) {
      _currentActionIpAccessListLineUnusable = true;
    } else {
      _currentUdpOptionsBuilder.setDstPortSpec(portSpec.get());
    }
  }

  @Override
  public void exitAcllal4udp_source_port(Acllal4udp_source_portContext ctx) {
    Optional<PortSpec> portSpec = toPortSpec(ctx, ctx.port);
    if (!portSpec.isPresent()) {
      _currentActionIpAccessListLineUnusable = true;
    } else {
      _currentUdpOptionsBuilder.setSrcPortSpec(portSpec.get());
    }
  }

  @Override
  public void exitEvv_rd(Evv_rdContext ctx) {
    RouteDistinguisherOrAuto rd = toRouteDistinguisher(ctx.rd);
    _currentEvpnVni.setRd(rd);
  }

  @Override
  public void exitEvv_route_target(Evv_route_targetContext ctx) {
    boolean setImport = ctx.dir.BOTH() != null || ctx.dir.IMPORT() != null;
    boolean setExport = ctx.dir.BOTH() != null || ctx.dir.EXPORT() != null;
    assert setImport || setExport;
    ExtendedCommunityOrAuto ecOrAuto = toExtendedCommunityOrAuto(ctx.rt);
    if (setExport) {
      _currentEvpnVni.setExportRt(ecOrAuto);
    }
    if (setImport) {
      _currentEvpnVni.setImportRt(ecOrAuto);
    }
  }

  @Override
  public void exitI_bandwidth(I_bandwidthContext ctx) {
    if (ctx.bw != null) {
      Integer bandwidth = toBandwidth(ctx, ctx.bw);
      if (bandwidth == null) {
        return;
      }
      _currentInterfaces.forEach(iface -> iface.setBandwidth(bandwidth));
    }
    if (ctx.inherit != null) {
      // TODO: support bandwidth inherit
      todo(ctx);
    }
  }

  @Override
  public void exitI_channel_group(I_channel_groupContext ctx) {
    int line = ctx.getStart().getLine();
    String portChannelInterfaceName = toPortChannel(ctx, ctx.id);
    if (portChannelInterfaceName == null) {
      return;
    }
    // To be added to a channel-group, all interfaces in range must be:
    // - compatible with each other
    // - compatible with the port-channel if it already exists
    // If the port-channel does not exist, it is created with compatible settings.

    // However, if force flag is set, then compatibility is forced as follows:
    // - If port-channel already exists, all interfaces in range copy its settings
    // - Otherwise, the new port-channel and interfaces beyond the first copy settings
    //   from the first interface in the range.

    boolean portChannelExists =
        _configuration.getInterfaces().containsKey(portChannelInterfaceName);
    boolean force = ctx.force != null;

    _configuration.referenceStructure(
        PORT_CHANNEL, portChannelInterfaceName, INTERFACE_CHANNEL_GROUP, line);

    if (_currentInterfaces.isEmpty()) {
      // Stop now, since later logic requires non-empty list
      return;
    }

    // Where settings are copied from: the port-channel[ID] if it exists, or the first interface
    // in the current interface range.
    Interface referenceIface =
        portChannelExists
            ? _configuration.getInterfaces().get(portChannelInterfaceName)
            : _currentInterfaces.iterator().next();

    if (!force) {
      Optional<Interface> incompatibleInterface =
          _currentInterfaces.stream()
              .filter(iface -> iface != referenceIface)
              .filter(iface -> !checkPortChannelCompatibilitySettings(referenceIface, iface))
              .findFirst();
      // If some incompatible interface found, warn, do not set the group,
      // do not create the port-channel, and do not copy settings.
      if (incompatibleInterface.isPresent()) {
        _w.addWarning(
            ctx,
            getFullText(ctx),
            _parser,
            String.format(
                "Cannot set channel-group because interface '%s' has settings that do not conform to those of interface '%s'",
                incompatibleInterface.get().getName(), referenceIface.getName()));
        return;
      }
    }

    if (!portChannelExists) {
      // Make the port-channel[ID] parent interface and copy settings "up" to it.
      Interface portChannelIface =
          newNonVlanInterface(portChannelInterfaceName, null, CiscoNxosInterfaceType.PORT_CHANNEL);
      copyPortChannelCompatibilitySettings(referenceIface, portChannelIface);
      _configuration.getInterfaces().put(portChannelInterfaceName, portChannelIface);
      _configuration.defineStructure(INTERFACE, portChannelInterfaceName, line);
      _configuration.referenceStructure(
          INTERFACE, portChannelInterfaceName, INTERFACE_SELF_REFERENCE, line);
      _configuration.defineStructure(PORT_CHANNEL, portChannelInterfaceName, line);
    }

    _currentInterfaces.forEach(
        iface -> {
          iface.setChannelGroup(portChannelInterfaceName);
          if (force) {
            copyPortChannelCompatibilitySettings(referenceIface, iface);
          } else {
            assert checkPortChannelCompatibilitySettings(referenceIface, iface);
          }
        });
  }

  @Override
  public void exitI_description(I_descriptionContext ctx) {
    Optional<String> description = toString(ctx, ctx.desc);
    if (description.isPresent()) {
      _currentInterfaces.forEach(i -> i.setDescription(description.get()));
    }
  }

  @Override
  public void exitI_encapsulation(I_encapsulationContext ctx) {
    Integer vlanId = toVlanId(ctx, ctx.vlan);
    if (vlanId == null) {
      return;
    }
    _currentInterfaces.forEach(iface -> iface.setEncapsulationVlan(vlanId));
  }

  @Override
  public void exitI_ip_address(I_ip_addressContext ctx) {
    InterfaceAddressWithAttributes address = toInterfaceAddress(ctx.addr);
    if (ctx.SECONDARY() != null) {
      // secondary addresses are appended
      _currentInterfaces.forEach(iface -> iface.getSecondaryAddresses().add(address));
    } else {
      // primary address is replaced
      _currentInterfaces.forEach(iface -> iface.setAddress(address));
    }
    if (ctx.tag != null) {
      warn(ctx, "Unsupported: tag on interface ip address");
      address.setTag(toLong(ctx.tag));
    }
  }

  @Override
  public void exitI_ip_policy(I_ip_policyContext ctx) {
    toString(ctx, ctx.name)
        .ifPresent(
            pbrPolicyName -> {
              _currentInterfaces.forEach(iface -> iface.setPbrPolicy(pbrPolicyName));
              _configuration.referenceStructure(
                  ROUTE_MAP, pbrPolicyName, INTERFACE_IP_POLICY, ctx.getStart().getLine());
            });
  }

  @Override
  public void exitI_mtu(I_mtuContext ctx) {
    Optional<Integer> mtu = toInteger(ctx, ctx.interface_mtu());
    if (!mtu.isPresent()) {
      return;
    }
    _currentInterfaces.forEach(iface -> iface.setMtu(mtu.get()));
  }

  @Override
  public void exitI_no_autostate(I_no_autostateContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setAutostate(false));
  }

  @Override
  public void exitI_no_shutdown(I_no_shutdownContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setShutdown(false));
  }

  @Override
  public void exitI_no_switchport(I_no_switchportContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setSwitchportMode(SwitchportMode.NONE));
  }

  @Override
  public void exitI_shutdown(I_shutdownContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setShutdown(true));
  }

  @Override
  public void exitI_switchport_access(I_switchport_accessContext ctx) {
    Integer vlanId = toVlanId(ctx, ctx.vlan);
    if (vlanId == null) {
      return;
    }
    _currentInterfaces.forEach(
        iface -> {
          iface.setSwitchportMode(SwitchportMode.ACCESS);
          iface.setAccessVlan(vlanId);
        });
  }

  @Override
  public void exitI_switchport_mode_access(I_switchport_mode_accessContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setSwitchportMode(SwitchportMode.ACCESS));
  }

  @Override
  public void exitI_switchport_mode_dot1q_tunnel(I_switchport_mode_dot1q_tunnelContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitI_switchport_mode_fex_fabric(I_switchport_mode_fex_fabricContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitI_switchport_mode_trunk(I_switchport_mode_trunkContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setSwitchportMode(SwitchportMode.TRUNK));
  }

  @Override
  public void exitI_switchport_monitor(I_switchport_monitorContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setSwitchportMonitor(true));
  }

  @Override
  public void exitI_switchport_trunk_allowed(I_switchport_trunk_allowedContext ctx) {
    IntegerSpace vlans;
    if (ctx.vlans != null) {
      vlans = ctx.vlans != null ? toVlanIdRange(ctx, ctx.vlans) : null;
      if (vlans == null) {
        // invalid VLAN in range
        return;
      }
    } else if (ctx.except != null) {
      Integer except = toVlanId(ctx, ctx.except);
      if (except == null) {
        // invalid VLAN to exclude
        return;
      }
      vlans = _currentValidVlanRange.difference(IntegerSpace.of(except));
    } else if (ctx.NONE() != null) {
      vlans = IntegerSpace.EMPTY;
    } else {
      todo(ctx);
      return;
    }
    _currentInterfaces.forEach(
        iface -> {
          iface.setSwitchportMode(SwitchportMode.TRUNK);
          if (ctx.ADD() != null) {
            iface.setAllowedVlans(iface.getAllowedVlans().union(vlans));
          } else if (ctx.REMOVE() != null) {
            iface.setAllowedVlans(iface.getAllowedVlans().difference(vlans));
          } else {
            iface.setAllowedVlans(vlans);
          }
        });
  }

  @Override
  public void exitI_switchport_trunk_native(I_switchport_trunk_nativeContext ctx) {
    Integer vlanId = toVlanId(ctx, ctx.vlan);
    if (vlanId == null) {
      return;
    }
    _currentInterfaces.forEach(
        iface -> {
          iface.setSwitchportMode(SwitchportMode.TRUNK);
          iface.setNativeVlan(vlanId);
        });
  }

  @Override
  public void exitI_vrf_member(I_vrf_memberContext ctx) {
    Optional<String> nameOrErr = toString(ctx, ctx.name);
    if (!nameOrErr.isPresent()) {
      return;
    }
    if (_currentInterfaces.stream()
        .anyMatch(iface -> iface.getSwitchportMode() != SwitchportMode.NONE)) {
      _w.addWarning(ctx, getFullText(ctx), _parser, "Cannot assign VRF to switchport interface(s)");
      return;
    }
    String name = nameOrErr.get();
    _configuration.referenceStructure(VRF, name, INTERFACE_VRF_MEMBER, ctx.getStart().getLine());
    _currentInterfaces.forEach(
        iface -> {
          clearLayer3Configuration(iface);
          iface.setVrfMember(name);
        });
  }

  @Override
  public void exitIp_access_list(Ip_access_listContext ctx) {
    _currentIpAccessList = null;
  }

  @Override
  public void exitIp_prefix_list(Ip_prefix_listContext ctx) {
    _currentIpPrefixList = null;
  }

  @Override
  public void exitIp_route(Ip_routeContext ctx) {
    int line = ctx.getStart().getLine();
    StaticRoute.Builder builder = StaticRoute.builder().setPrefix(toPrefix(ctx.network));
    if (ctx.name != null) {
      String name = toString(ctx, ctx.name);
      if (name == null) {
        return;
      }
      builder.setName(name);
    }
    if (ctx.nhint != null) {
      String nhint = _configuration.canonicalizeInterfaceName(ctx.nhint.getText());
      builder.setNextHopInterface(nhint);
      _configuration.referenceStructure(INTERFACE, nhint, IP_ROUTE_NEXT_HOP_INTERFACE, line);
    }
    if (ctx.nhip != null) {
      builder.setNextHopIp(toIp(ctx.nhip));
    }
    if (ctx.nhvrf != null) {
      Optional<String> vrfOrErr = toString(ctx, ctx.nhvrf);
      if (!vrfOrErr.isPresent()) {
        return;
      }
      String vrf = vrfOrErr.get();
      _configuration.referenceStructure(VRF, vrf, IP_ROUTE_NEXT_HOP_VRF, line);
      builder.setNextHopVrf(vrf);

      // TODO: support looking up next-hop-ip in a different VRF
      todo(ctx);
    }
    if (ctx.null0 != null) {
      builder.setDiscard(true);
    }
    if (ctx.pref != null) {
      Short pref = toShort(ctx, ctx.pref);
      if (pref == null) {
        return;
      }
      builder.setPreference(pref);
    }
    if (ctx.tag != null) {
      builder.setTag(toLong(ctx.tag));
    }
    if (ctx.track != null) {
      Short track = toShort(ctx, ctx.track);
      if (track == null) {
        return;
      }
      builder.setTrack(track);
      // TODO: support track object number
      todo(ctx);
    }
    StaticRoute route = builder.build();
    _currentVrf.getStaticRoutes().put(route.getPrefix(), route);
  }

  @Override
  public void exitNve_host_reachability(Nve_host_reachabilityContext ctx) {
    if (_currentNves.stream()
        .flatMap(nve -> nve.getMemberVnis().values().stream())
        .anyMatch(
            vni -> vni.getIngressReplicationProtocol() == IngressReplicationProtocol.STATIC)) {
      _w.addWarning(
          ctx,
          getFullText(ctx),
          _parser,
          "Please remove ingress replication static under VNIs before configuring host reachability bgp.");
      return;
    }
    _currentNves.forEach(nve -> nve.setHostReachabilityProtocol(HostReachabilityProtocol.BGP));
  }

  @Override
  public void exitNve_no_shutdown(Nve_no_shutdownContext ctx) {
    _currentNves.forEach(n -> n.setShutdown(false));
  }

  @Override
  public void exitNve_source_interface(Nve_source_interfaceContext ctx) {
    Optional<String> inameOrError = toString(ctx, ctx.name);
    if (!inameOrError.isPresent()) {
      return;
    }
    String iname = inameOrError.get();
    _configuration.referenceStructure(
        INTERFACE, iname, NVE_SOURCE_INTERFACE, ctx.name.getStart().getLine());
    _currentNves.forEach(n -> n.setSourceInterface(iname));
  }

  @Override
  public void exitPl_action(Pl_actionContext ctx) {
    if (ctx.mask != null) {
      todo(ctx);
      return;
    }
    long num;
    if (ctx.num != null) {
      Optional<Long> numOption = toLong(ctx, ctx.num);
      if (!numOption.isPresent()) {
        return;
      }
      num = numOption.get();
    } else if (!_currentIpPrefixList.getLines().isEmpty()) {
      num = _currentIpPrefixList.getLines().lastKey() + 5L;
    } else {
      num = 5L;
    }
    Prefix prefix = toPrefix(ctx.prefix);
    int low;
    int high;
    int prefixLength = prefix.getPrefixLength();
    if (ctx.eq != null) {
      Optional<Integer> eqOption = toInteger(ctx, ctx.eq);
      if (!eqOption.isPresent()) {
        // invalid line
        return;
      }
      int eq = eqOption.get();
      low = eq;
      high = eq;
    } else if (ctx.ge != null || ctx.le != null) {
      if (ctx.ge != null) {
        Optional<Integer> geOption = toInteger(ctx, ctx.ge);
        if (!geOption.isPresent()) {
          // invalid line
          return;
        }
        low = geOption.get();
      } else {
        low = prefixLength;
      }
      if (ctx.le != null) {
        Optional<Integer> leOption = toInteger(ctx, ctx.le);
        if (!leOption.isPresent()) {
          // invalid line
          return;
        }
        high = leOption.get();
      } else {
        high = Prefix.MAX_PREFIX_LENGTH;
      }
    } else {
      low = prefixLength;
      high = Prefix.MAX_PREFIX_LENGTH;
    }
    IpPrefixListLine pll =
        new IpPrefixListLine(toLineAction(ctx.action), num, prefix, new SubRange(low, high));
    _currentIpPrefixList.getLines().put(num, pll);
  }

  @Override
  public void exitPl_description(Pl_descriptionContext ctx) {
    toString(ctx, ctx.text)
        .ifPresent(description -> _currentIpPrefixList.setDescription(description));
  }

  @Override
  public void exitRmm_as_path(Rmm_as_pathContext ctx) {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    Optional.ofNullable(_currentRouteMapEntry.getMatchAsPath())
        .ifPresent(old -> names.addAll(old.getNames()));
    for (Ip_as_path_access_list_nameContext nameCtx : ctx.names) {
      Optional<String> name = toString(ctx, nameCtx);
      if (!name.isPresent()) {
        return;
      }
      names.add(name.get());
    }
    _currentRouteMapEntry.setMatchAsPath(new RouteMapMatchAsPath(names.build()));
  }

  @Override
  public void exitRmm_community(Rmm_communityContext ctx) {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    Optional.ofNullable(_currentRouteMapEntry.getMatchCommunity())
        .ifPresent(old -> names.addAll(old.getNames()));
    for (Ip_community_list_nameContext nameCtx : ctx.names) {
      Optional<String> name = toString(ctx, nameCtx);
      if (!name.isPresent()) {
        return;
      }
      names.add(name.get());
    }
    _currentRouteMapEntry.setMatchCommunity(new RouteMapMatchCommunity(names.build()));
  }

  @Override
  public void exitRmm_interface(Rmm_interfaceContext ctx) {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    Optional.ofNullable(_currentRouteMapEntry.getMatchInterface())
        .ifPresent(old -> names.addAll(old.getNames()));
    for (Interface_nameContext nameCtx : ctx.interfaces) {
      Optional<String> name = toString(ctx, nameCtx);
      if (!name.isPresent()) {
        return;
      }
      names.add(name.get());
    }
    _currentRouteMapEntry.setMatchInterface(new RouteMapMatchInterface(names.build()));
  }

  @Override
  public void exitRmm_metric(Rmm_metricContext ctx) {
    _currentRouteMapEntry.setMatchMetric(new RouteMapMatchMetric(toLong(ctx.metric)));
  }

  @Override
  public void exitRmm_tag(Rmm_tagContext ctx) {
    _currentRouteMapEntry.setMatchTag(new RouteMapMatchTag(toLong(ctx.tag)));
  }

  @Override
  public void exitRmmipa_pbr(Rmmipa_pbrContext ctx) {
    Optional<String> nameOpt = toString(ctx, ctx.name);
    if (!nameOpt.isPresent()) {
      return;
    }
    if (_currentRouteMapEntry.getMatchIpAddress() != null) {
      _w.addWarning(
          ctx,
          getFullText(ctx),
          _parser,
          "route-map entry cannot match more than one ip access-list");
      return;
    }
    _currentRouteMapEntry.setMatchIpAddress(new RouteMapMatchIpAddress(nameOpt.get()));
  }

  @Override
  public void exitRmmipa_prefix_list(Rmmipa_prefix_listContext ctx) {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    Optional.ofNullable(_currentRouteMapEntry.getMatchIpAddressPrefixList())
        .ifPresent(old -> names.addAll(old.getNames()));
    for (Ip_prefix_list_nameContext nameCtx : ctx.names) {
      Optional<String> nameOrError = toString(ctx, nameCtx);
      if (!nameOrError.isPresent()) {
        return;
      }
      String name = nameOrError.get();
      _configuration.referenceStructure(
          IP_PREFIX_LIST,
          name,
          ROUTE_MAP_MATCH_IP_ADDRESS_PREFIX_LIST,
          nameCtx.getStart().getLine());
      names.add(name);
    }
    _currentRouteMapEntry.setMatchIpAddressPrefixList(
        new RouteMapMatchIpAddressPrefixList(names.build()));
  }

  @Override
  public void exitRmsapp_last_as(Rmsapp_last_asContext ctx) {
    Optional<Integer> numPrependsOpt = toInteger(ctx, ctx.num_prepends);
    if (!numPrependsOpt.isPresent()) {
      return;
    }
    _currentRouteMapEntry.setSetAsPathPrepend(
        new RouteMapSetAsPathPrependLastAs(numPrependsOpt.get()));
  }

  @Override
  public void exitRmsapp_literal(Rmsapp_literalContext ctx) {
    _currentRouteMapEntry.setSetAsPathPrepend(
        new RouteMapSetAsPathPrependLiteralAs(
            ctx.asns.stream()
                .map(CiscoNxosControlPlaneExtractor::toLong)
                .collect(ImmutableList.toImmutableList())));
  }

  @Override
  public void exitRms_community(Rms_communityContext ctx) {
    ImmutableList.Builder<StandardCommunity> communities = ImmutableList.builder();
    for (Standard_communityContext communityCtx : ctx.communities) {
      Optional<StandardCommunity> communityOpt = toStandardCommunity(communityCtx);
      if (!communityOpt.isPresent()) {
        return;
      }
      communities.add(communityOpt.get());
    }
    RouteMapSetCommunity old = _currentRouteMapEntry.getSetCommunity();
    boolean additive = ctx.additive != null;
    if (old != null) {
      communities.addAll(old.getCommunities());
      additive = additive || old.getAdditive();
    }
    _currentRouteMapEntry.setSetCommunity(new RouteMapSetCommunity(communities.build(), additive));
  }

  @Override
  public void exitRmsipnh_literal(Rmsipnh_literalContext ctx) {
    // Incompatible with:
    // - peer-address (TODO)
    // - redist-unchanged (TODO)
    // - unchanged
    ImmutableList.Builder<Ip> nextHops = ImmutableList.builder();
    RouteMapSetIpNextHop old = _currentRouteMapEntry.getSetIpNextHop();
    if (old != null) {
      if (!(old instanceof RouteMapSetIpNextHopLiteral)) {
        _w.addWarning(
            ctx,
            getFullText(ctx),
            _parser,
            "Cannot mix literal next-hop IP(s) with peer-address, redist-unchanged, nor unchanged");
        return;
      }
      nextHops.addAll(((RouteMapSetIpNextHopLiteral) old).getNextHops());
    }
    ctx.next_hops.stream().map(CiscoNxosControlPlaneExtractor::toIp).forEach(nextHops::add);
    _currentRouteMapEntry.setSetIpNextHop(new RouteMapSetIpNextHopLiteral(nextHops.build()));
  }

  @Override
  public void exitRmsipnh_unchanged(Rmsipnh_unchangedContext ctx) {
    // Incompatible with:
    // - literal IP(s)
    // - peer-address (TODO)
    RouteMapSetIpNextHop old = _currentRouteMapEntry.getSetIpNextHop();
    if (old != null && !(old instanceof RouteMapSetIpNextHopUnchanged)) {
      _w.addWarning(
          ctx,
          getFullText(ctx),
          _parser,
          "Cannot mix unchanged with literal next-hop IP(s) nor peer-address");
    }
    _currentRouteMapEntry.setSetIpNextHop(RouteMapSetIpNextHopUnchanged.INSTANCE);
  }

  @Override
  public void exitRms_local_preference(Rms_local_preferenceContext ctx) {
    _currentRouteMapEntry.setSetLocalPreference(
        new RouteMapSetLocalPreference(toLong(ctx.local_preference)));
  }

  @Override
  public void exitRms_metric(Rms_metricContext ctx) {
    _currentRouteMapEntry.setSetMetric(new RouteMapSetMetric(toLong(ctx.metric)));
  }

  @Override
  public void exitRms_metric_type(Rms_metric_typeContext ctx) {
    RouteMapMetricType type;
    if (ctx.EXTERNAL() != null) {
      type = RouteMapMetricType.EXTERNAL;
    } else if (ctx.INTERNAL() != null) {
      type = RouteMapMetricType.INTERNAL;
    } else if (ctx.TYPE_1() != null) {
      type = RouteMapMetricType.TYPE_1;
    } else if (ctx.TYPE_2() != null) {
      type = RouteMapMetricType.TYPE_2;
    } else {
      // assume valid but unsupported
      todo(ctx);
      return;
    }
    _currentRouteMapEntry.setSetMetricType(new RouteMapSetMetricType(type));
  }

  @Override
  public void exitRms_origin(Rms_originContext ctx) {
    OriginType type;
    if (ctx.EGP() != null) {
      type = OriginType.EGP;
    } else if (ctx.IGP() != null) {
      type = OriginType.IGP;
    } else if (ctx.INCOMPLETE() != null) {
      type = OriginType.INCOMPLETE;
    } else {
      // Realllly should not get here
      throw new IllegalArgumentException(String.format("Invalid origin type: %s", ctx.getText()));
    }
    _currentRouteMapEntry.setSetOrigin(new RouteMapSetOrigin(type));
  }

  @Override
  public void exitRms_tag(Rms_tagContext ctx) {
    _currentRouteMapEntry.setSetTag(new RouteMapSetTag(toLong(ctx.tag)));
  }

  @Override
  public void exitS_hostname(S_hostnameContext ctx) {
    _configuration.setHostname(ctx.hostname.getText());
  }

  @Override
  public void exitS_vrf_context(S_vrf_contextContext ctx) {
    _currentVrf = _configuration.getDefaultVrf();
  }

  @Override
  public void exitVc_no_shutdown(Vc_no_shutdownContext ctx) {
    _currentVrf.setShutdown(false);
  }

  @Override
  public void exitVc_rd(Vc_rdContext ctx) {
    _currentVrf.setRd(toRouteDistinguisher(ctx.rd));
  }

  @Override
  public void exitVc_shutdown(Vc_shutdownContext ctx) {
    _currentVrf.setShutdown(true);
  }

  @Override
  public void exitVc_vni(Vc_vniContext ctx) {
    Optional<Integer> vniOrError = toInteger(ctx, ctx.vni_number());
    if (!vniOrError.isPresent()) {
      return;
    }
    Integer vni = vniOrError.get();
    _currentVrf.setVni(vni);
  }

  private static void setRouteTarget(
      Route_target_or_autoContext rtAutoCtx,
      Both_export_importContext direction,
      @Nullable TerminalNode evpn,
      VrfAddressFamily af) {
    ExtendedCommunityOrAuto ecOrAuto = toExtendedCommunityOrAuto(rtAutoCtx);
    boolean setExport = direction.BOTH() != null || direction.EXPORT() != null;
    boolean setImport = direction.BOTH() != null || direction.IMPORT() != null;
    boolean isEvpn = evpn != null;
    if (!isEvpn && setExport) {
      af.setExportRt(ecOrAuto);
    }
    if (isEvpn && setExport) {
      af.setExportRtEvpn(ecOrAuto);
    }
    if (!isEvpn && setImport) {
      af.setImportRt(ecOrAuto);
    }
    if (isEvpn && setImport) {
      af.setImportRtEvpn(ecOrAuto);
    }
  }

  @Override
  public void exitVcaf4u_route_target(Vcaf4u_route_targetContext ctx) {
    VrfAddressFamily af = _currentVrf.getAddressFamily(AddressFamily.IPV4_UNICAST);
    setRouteTarget(ctx.rt, ctx.both_export_import(), ctx.EVPN(), af);
  }

  @Override
  public void exitVcaf6u_route_target(Vcaf6u_route_targetContext ctx) {
    VrfAddressFamily af = _currentVrf.getAddressFamily(AddressFamily.IPV6_UNICAST);
    setRouteTarget(ctx.rt, ctx.both_export_import(), ctx.EVPN(), af);
  }

  @Override
  public void exitVlan_vlan(Vlan_vlanContext ctx) {
    _currentVlans = null;
  }

  @Override
  public void exitVv_vn_segment(Vv_vn_segmentContext ctx) {
    Optional<Integer> vniOrError = toInteger(ctx, ctx.vni_number());
    if (!vniOrError.isPresent()) {
      return;
    }
    Integer vni = vniOrError.get();
    _currentVlans.forEach(v -> v.setVni(vni));
  }

  private @Nonnull String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    return _text.substring(start, end + 1);
  }

  @Override
  public @Nullable VendorConfiguration getVendorConfiguration() {
    return _configuration;
  }

  @Override
  public void processParseTree(ParserRuleContext tree) {
    ParseTreeWalker walker = new BatfishParseTreeWalker(_parser);
    walker.walk(this, tree);
  }

  private @Nullable Integer toBandwidth(
      ParserRuleContext messageCtx, Interface_bandwidth_kbpsContext ctx) {
    int bandwidth = Integer.parseInt(ctx.getText());
    if (!BANDWIDTH_RANGE.contains(bandwidth)) {
      _w.redFlag(
          String.format(
              "Expected bandwidth in range %s, but got '%d' in: %s",
              BANDWIDTH_RANGE, bandwidth, getFullText(messageCtx)));
      return null;
    }
    return bandwidth;
  }

  private void todo(ParserRuleContext ctx) {
    _w.todo(ctx, getFullText(ctx), _parser);
  }

  private void warn(ParserRuleContext ctx, String message) {
    _w.addWarning(ctx, getFullText(ctx), _parser, message);
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Bgp_distanceContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, BGP_DISTANCE_RANGE, "BGP distance");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Dscp_numberContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, DSCP_RANGE, "DSCP number");
  }

  private @Nonnull Optional<Integer> toInteger(ParserRuleContext messageCtx, Dscp_specContext ctx) {
    if (ctx.num != null) {
      return toInteger(messageCtx, ctx.num);
    } else if (ctx.AF11() != null) {
      return Optional.of(DscpType.AF11.number());
    } else if (ctx.AF12() != null) {
      return Optional.of(DscpType.AF12.number());
    } else if (ctx.AF13() != null) {
      return Optional.of(DscpType.AF13.number());
    } else if (ctx.AF21() != null) {
      return Optional.of(DscpType.AF21.number());
    } else if (ctx.AF22() != null) {
      return Optional.of(DscpType.AF22.number());
    } else if (ctx.AF23() != null) {
      return Optional.of(DscpType.AF23.number());
    } else if (ctx.AF31() != null) {
      return Optional.of(DscpType.AF31.number());
    } else if (ctx.AF32() != null) {
      return Optional.of(DscpType.AF32.number());
    } else if (ctx.AF33() != null) {
      return Optional.of(DscpType.AF33.number());
    } else if (ctx.AF41() != null) {
      return Optional.of(DscpType.AF41.number());
    } else if (ctx.AF42() != null) {
      return Optional.of(DscpType.AF42.number());
    } else if (ctx.AF43() != null) {
      return Optional.of(DscpType.AF43.number());
    } else if (ctx.CS1() != null) {
      return Optional.of(DscpType.CS1.number());
    } else if (ctx.CS2() != null) {
      return Optional.of(DscpType.CS2.number());
    } else if (ctx.CS3() != null) {
      return Optional.of(DscpType.CS3.number());
    } else if (ctx.CS4() != null) {
      return Optional.of(DscpType.CS4.number());
    } else if (ctx.CS5() != null) {
      return Optional.of(DscpType.CS5.number());
    } else if (ctx.CS6() != null) {
      return Optional.of(DscpType.CS6.number());
    } else if (ctx.CS7() != null) {
      return Optional.of(DscpType.CS7.number());
    } else if (ctx.DEFAULT() != null) {
      return Optional.of(DscpType.DEFAULT.number());
    } else if (ctx.EF() != null) {
      return Optional.of(DscpType.EF.number());
    } else {
      // assumed to be valid but unsupported
      todo(ctx);
      return Optional.empty();
    }
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Ebgp_multihop_ttlContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, BGP_EBGP_MULTIHOP_TTL_RANGE, "BGP ebgp-multihop ttl");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Inherit_sequence_numberContext ctx) {
    return toIntegerInSpace(
        messageCtx, ctx, BGP_INHERIT_RANGE, "BGP neighbor inherit peer-policy seq");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Interface_mtuContext ctx) {
    assert messageCtx != null; // prevent unused warning.
    // TODO: the valid MTU ranges are dependent on interface type.
    return Optional.of(toInteger(ctx.mtu));
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Ip_prefix_list_line_prefix_lengthContext ctx) {
    return toIntegerInSpace(
        messageCtx, ctx, IP_PREFIX_LIST_PREFIX_LENGTH_RANGE, "ip prefix-list prefix-length bound");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Last_as_num_prependsContext ctx) {
    return toIntegerInSpace(
        messageCtx,
        ctx,
        NUM_AS_PATH_PREPENDS_RANGE,
        "set as-path prepend last-as number of prepends");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Maxas_limitContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, BGP_MAXAS_LIMIT_RANGE, "BGP maxas-limit");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Maximum_pathsContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, BGP_MAXIMUM_PATHS_RANGE, "BGP maximum-paths");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Ospf_area_default_costContext ctx) {
    return toIntegerInSpace(
        messageCtx, ctx, OSPF_AREA_DEFAULT_COST_RANGE, "router ospf area default-cost");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Ospf_area_range_costContext ctx) {
    return toIntegerInSpace(
        messageCtx, ctx, OSPF_AREA_RANGE_COST_RANGE, "router ospf area range cost");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Packet_lengthContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, PACKET_LENGTH_RANGE, "packet length");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Tcp_flags_maskContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, TCP_FLAGS_MASK_RANGE, "tcp-flags-mask");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Tcp_port_numberContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, TCP_PORT_RANGE, "TCP port");
  }

  private @Nonnull Optional<Integer> toInteger(ParserRuleContext messageCtx, Tcp_portContext ctx) {
    if (ctx.num != null) {
      return toInteger(messageCtx, ctx.num);
    } else if (ctx.BGP() != null) {
      return Optional.of(NamedPort.BGP.number());
    } else if (ctx.CHARGEN() != null) {
      return Optional.of(NamedPort.CHARGEN.number());
    } else if (ctx.CMD() != null) {
      return Optional.of(NamedPort.CMDtcp_OR_SYSLOGudp.number());
    } else if (ctx.DAYTIME() != null) {
      return Optional.of(NamedPort.DAYTIME.number());
    } else if (ctx.DISCARD() != null) {
      return Optional.of(NamedPort.DISCARD.number());
    } else if (ctx.DOMAIN() != null) {
      return Optional.of(NamedPort.DOMAIN.number());
    } else if (ctx.DRIP() != null) {
      return Optional.of(NamedPort.DRIP.number());
    } else if (ctx.ECHO() != null) {
      return Optional.of(NamedPort.ECHO.number());
    } else if (ctx.EXEC() != null) {
      return Optional.of(NamedPort.BIFFudp_OR_EXECtcp.number());
    } else if (ctx.FINGER() != null) {
      return Optional.of(NamedPort.FINGER.number());
    } else if (ctx.FTP() != null) {
      return Optional.of(NamedPort.FTP.number());
    } else if (ctx.FTP_DATA() != null) {
      return Optional.of(NamedPort.FTP_DATA.number());
    } else if (ctx.GOPHER() != null) {
      return Optional.of(NamedPort.GOPHER.number());
    } else if (ctx.HOSTNAME() != null) {
      return Optional.of(NamedPort.HOSTNAME.number());
    } else if (ctx.IDENT() != null) {
      return Optional.of(NamedPort.IDENT.number());
    } else if (ctx.IRC() != null) {
      return Optional.of(NamedPort.IRC.number());
    } else if (ctx.KLOGIN() != null) {
      return Optional.of(NamedPort.KLOGIN.number());
    } else if (ctx.KSHELL() != null) {
      return Optional.of(NamedPort.KSHELL.number());
    } else if (ctx.LOGIN() != null) {
      return Optional.of(NamedPort.LOGINtcp_OR_WHOudp.number());
    } else if (ctx.LPD() != null) {
      return Optional.of(NamedPort.LPD.number());
    } else if (ctx.NNTP() != null) {
      return Optional.of(NamedPort.NNTP.number());
    } else if (ctx.PIM_AUTO_RP() != null) {
      return Optional.of(NamedPort.PIM_AUTO_RP.number());
    } else if (ctx.POP2() != null) {
      return Optional.of(NamedPort.POP2.number());
    } else if (ctx.POP3() != null) {
      return Optional.of(NamedPort.POP3.number());
    } else if (ctx.SMTP() != null) {
      return Optional.of(NamedPort.SMTP.number());
    } else if (ctx.SUNRPC() != null) {
      return Optional.of(NamedPort.SUNRPC.number());
    } else if (ctx.TACACS() != null) {
      return Optional.of(NamedPort.TACACS.number());
    } else if (ctx.TALK() != null) {
      return Optional.of(NamedPort.TALK.number());
    } else if (ctx.TELNET() != null) {
      return Optional.of(NamedPort.TELNET.number());
    } else if (ctx.TIME() != null) {
      return Optional.of(NamedPort.TIME.number());
    } else if (ctx.UUCP() != null) {
      return Optional.of(NamedPort.UUCP.number());
    } else if (ctx.WHOIS() != null) {
      return Optional.of(NamedPort.WHOIS.number());
    } else if (ctx.WWW() != null) {
      return Optional.of(NamedPort.HTTP.number());
    } else {
      // assume valid but unsupported
      todo(ctx);
      return Optional.empty();
    }
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Udp_port_numberContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, UDP_PORT_RANGE, "UDP port");
  }

  private @Nonnull Optional<Integer> toInteger(ParserRuleContext messageCtx, Udp_portContext ctx) {
    if (ctx.num != null) {
      return toInteger(messageCtx, ctx.num);
    } else if (ctx.BIFF() != null) {
      return Optional.of(NamedPort.BIFFudp_OR_EXECtcp.number());
    } else if (ctx.BOOTPC() != null) {
      return Optional.of(NamedPort.BOOTPC.number());
    } else if (ctx.BOOTPS() != null) {
      return Optional.of(NamedPort.BOOTPS_OR_DHCP.number());
    } else if (ctx.DISCARD() != null) {
      return Optional.of(NamedPort.DISCARD.number());
    } else if (ctx.DNSIX() != null) {
      return Optional.of(NamedPort.DNSIX.number());
    } else if (ctx.DOMAIN() != null) {
      return Optional.of(NamedPort.DOMAIN.number());
    } else if (ctx.ECHO() != null) {
      return Optional.of(NamedPort.ECHO.number());
    } else if (ctx.ISAKMP() != null) {
      return Optional.of(NamedPort.ISAKMP.number());
    } else if (ctx.MOBILE_IP() != null) {
      return Optional.of(NamedPort.MOBILE_IP_AGENT.number());
    } else if (ctx.NAMESERVER() != null) {
      return Optional.of(NamedPort.NAMESERVER.number());
    } else if (ctx.NETBIOS_DGM() != null) {
      return Optional.of(NamedPort.NETBIOS_DGM.number());
    } else if (ctx.NETBIOS_NS() != null) {
      return Optional.of(NamedPort.NETBIOS_NS.number());
    } else if (ctx.NETBIOS_SS() != null) {
      return Optional.of(NamedPort.NETBIOS_SSN.number());
    } else if (ctx.NON500_ISAKMP() != null) {
      return Optional.of(NamedPort.NON500_ISAKMP.number());
    } else if (ctx.NTP() != null) {
      return Optional.of(NamedPort.NTP.number());
    } else if (ctx.PIM_AUTO_RP() != null) {
      return Optional.of(NamedPort.PIM_AUTO_RP.number());
    } else if (ctx.RIP() != null) {
      return Optional.of(NamedPort.EFStcp_OR_RIPudp.number());
    } else if (ctx.SNMP() != null) {
      return Optional.of(NamedPort.SNMP.number());
    } else if (ctx.SNMPTRAP() != null) {
      return Optional.of(NamedPort.SNMPTRAP.number());
    } else if (ctx.SUNRPC() != null) {
      return Optional.of(NamedPort.SUNRPC.number());
    } else if (ctx.SYSLOG() != null) {
      return Optional.of(NamedPort.CMDtcp_OR_SYSLOGudp.number());
    } else if (ctx.TACACS() != null) {
      return Optional.of(NamedPort.TACACS.number());
    } else if (ctx.TALK() != null) {
      return Optional.of(NamedPort.TALK.number());
    } else if (ctx.TFTP() != null) {
      return Optional.of(NamedPort.TFTP.number());
    } else if (ctx.TIME() != null) {
      return Optional.of(NamedPort.TIME.number());
    } else if (ctx.WHO() != null) {
      return Optional.of(NamedPort.LOGINtcp_OR_WHOudp.number());
    } else if (ctx.XDMCP() != null) {
      return Optional.of(NamedPort.XDMCP.number());
    } else {
      // assume valid but unsupported
      todo(ctx);
      return Optional.empty();
    }
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Vni_numberContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, VNI_RANGE, "VNI");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Route_map_sequenceContext ctx) {
    return toIntegerInSpace(
        messageCtx, ctx, ROUTE_MAP_ENTRY_SEQUENCE_RANGE, "route-map entry sequence");
  }

  /**
   * Convert a {@link ParserRuleContext} whose text is guaranteed to represent a valid signed 32-bit
   * decimal integer to an {@link Integer} if it is contained in the provided {@code space}, or else
   * {@link Optional#empty}.
   */
  private @Nonnull Optional<Integer> toIntegerInSpace(
      ParserRuleContext messageCtx, ParserRuleContext ctx, IntegerSpace space, String name) {
    int num = Integer.parseInt(ctx.getText());
    if (!space.contains(num)) {
      _w.addWarning(
          messageCtx,
          getFullText(messageCtx),
          _parser,
          String.format("Expected %s in range %s, but got '%d'", name, space, num));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  private @Nonnull Optional<IntegerSpace> toIntegerSpace(
      ParserRuleContext messageCtx, Acllal3o_packet_length_specContext ctx) {
    boolean range = ctx.range != null;
    return toInteger(messageCtx, ctx.arg1)
        .map(
            arg1 ->
                toIntegerSpace(
                        messageCtx,
                        arg1,
                        range ? toInteger(messageCtx, ctx.arg2) : Optional.empty(),
                        ctx.eq != null,
                        ctx.lt != null,
                        ctx.gt != null,
                        ctx.neq != null,
                        range,
                        PACKET_LENGTH_RANGE)
                    .orElse(null));
  }

  private @Nonnull Optional<IntegerSpace> toIntegerSpace(
      ParserRuleContext messageCtx, Acllal4tcp_port_spec_literalContext ctx) {
    boolean range = ctx.range != null;
    return toInteger(messageCtx, ctx.arg1)
        .map(
            arg1 ->
                toIntegerSpace(
                        messageCtx,
                        arg1,
                        range ? toInteger(messageCtx, ctx.arg2) : Optional.empty(),
                        ctx.eq != null,
                        ctx.lt != null,
                        ctx.gt != null,
                        ctx.neq != null,
                        range,
                        TCP_PORT_RANGE)
                    .orElse(null));
  }

  private @Nonnull Optional<IntegerSpace> toIntegerSpace(
      ParserRuleContext messageCtx, Acllal4udp_port_spec_literalContext ctx) {
    boolean range = ctx.range != null;
    return toInteger(messageCtx, ctx.arg1)
        .map(
            arg1 ->
                toIntegerSpace(
                        messageCtx,
                        arg1,
                        range ? toInteger(messageCtx, ctx.arg2) : Optional.empty(),
                        ctx.eq != null,
                        ctx.lt != null,
                        ctx.gt != null,
                        ctx.neq != null,
                        range,
                        UDP_PORT_RANGE)
                    .orElse(null));
  }

  /**
   * Helper for NX-OS integer space specifiers to convert to IntegerSpace if valid, or else {@link
   * Optional#empty}.
   */
  private @Nonnull Optional<IntegerSpace> toIntegerSpace(
      ParserRuleContext messageCtx,
      int arg1,
      Optional<Integer> arg2Optional,
      boolean eq,
      boolean lt,
      boolean gt,
      boolean neq,
      boolean range,
      IntegerSpace space) {
    if (eq) {
      return Optional.of(IntegerSpace.of(arg1));
    } else if (lt) {
      if (arg1 <= space.least()) {
        return Optional.empty();
      }
      return Optional.of(space.intersection(IntegerSpace.of(Range.closed(0, arg1 - 1))));
    } else if (gt) {
      if (arg1 >= space.greatest()) {
        return Optional.empty();
      }
      return Optional.of(
          space.intersection(IntegerSpace.of(Range.closed(arg1 + 1, Integer.MAX_VALUE))));
    } else if (neq) {
      return Optional.of(space.difference(IntegerSpace.of(arg1)));
    } else if (range) {
      // both args guaranteed to be in range
      return arg2Optional.map(arg2 -> IntegerSpace.of(Range.closed(arg1, arg2)));
    } else {
      // assume valid but unsupported by caller
      todo(messageCtx);
      return Optional.empty();
    }
  }

  private @Nonnull Optional<Long> toLong(
      Ip_as_path_access_listContext messageCtx, Ip_as_path_access_list_seqContext ctx) {
    return toLongInSpace(
        messageCtx, ctx, IP_AS_PATH_ACCESS_LIST_SEQ_RANGE, "ip as-path access-list seq");
  }

  private @Nonnull Optional<Long> toLong(
      ParserRuleContext messageCtx, Ip_access_list_line_numberContext ctx) {
    return toLongInSpace(
        messageCtx, ctx, IP_ACCESS_LIST_LINE_NUMBER_RANGE, "ip access-list line number");
  }

  private @Nonnull Optional<Long> toLong(
      ParserRuleContext messageCtx, Ip_community_list_seqContext ctx) {
    return toLongInSpace(
        messageCtx, ctx, IP_COMMUNITY_LIST_LINE_NUMBER_RANGE, "ip community-list line number");
  }

  private @Nonnull Optional<Long> toLong(
      ParserRuleContext messageCtx, Ip_prefix_list_line_numberContext ctx) {
    return toLongInSpace(messageCtx, ctx, IP_PREFIX_LIST_LINE_NUMBER_RANGE, "ip prefix-list seq");
  }

  /**
   * Convert a {@link ParserRuleContext} whose text is guaranteed to represent a valid signed 64-bit
   * decimal integer to a {@link Long} if it is contained in the provided {@code space}, or else
   * {@link Optional#empty}.
   */
  private @Nonnull Optional<Long> toLongInSpace(
      ParserRuleContext messageCtx, ParserRuleContext ctx, LongSpace space, String name) {
    long num = Long.parseLong(ctx.getText());
    if (!space.contains(num)) {
      _w.addWarning(
          messageCtx,
          getFullText(messageCtx),
          _parser,
          String.format("Expected %s in range %s, but got '%d'", name, space, num));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  private @Nullable String toPortChannel(ParserRuleContext messageCtx, Channel_idContext ctx) {
    int id = Integer.parseInt(ctx.getText());
    // not a mistake; range is 1-4096 (not zero-based).
    if (!PORT_CHANNEL_RANGE.contains(id)) {
      _w.redFlag(
          String.format(
              "Expected port-channel id in range %s, but got '%d' in: %s",
              PORT_CHANNEL_RANGE, id, getFullText(messageCtx)));
      return null;
    }
    return "port-channel" + id;
  }

  private @Nonnull Optional<PortSpec> toPortSpec(
      ParserRuleContext messageCtx, Acllal4tcp_port_specContext ctx) {
    if (ctx.literal != null) {
      return toIntegerSpace(messageCtx, ctx.literal)
          .map(literalPorts -> new LiteralPortSpec(literalPorts));
    } else if (ctx.group != null) {
      return Optional.of(toPortSpec(ctx.group));
    } else {
      // assume valid but unsupported
      todo(ctx);
      return Optional.empty();
    }
  }

  private @Nonnull Optional<PortSpec> toPortSpec(
      ParserRuleContext messageCtx, Acllal4udp_port_specContext ctx) {
    if (ctx.literal != null) {
      return toIntegerSpace(messageCtx, ctx.literal)
          .map(literalPorts -> new LiteralPortSpec(literalPorts));
    } else if (ctx.group != null) {
      return Optional.of(toPortSpec(ctx.group));
    } else {
      // assume valid but unsupported
      todo(ctx);
      return Optional.empty();
    }
  }

  private @Nullable Short toShort(ParserRuleContext messageCtx, Static_route_prefContext ctx) {
    short pref = Short.parseShort(ctx.getText());
    if (!STATIC_ROUTE_PREFERENCE_RANGE.contains((int) pref)) {
      _w.redFlag(
          String.format(
              "Expected prefernce in range %s, but got '%d' in: %s",
              STATIC_ROUTE_PREFERENCE_RANGE, pref, getFullText(messageCtx)));
      return null;
    }
    return pref;
  }

  private @Nullable Short toShort(ParserRuleContext messageCtx, Track_object_numberContext ctx) {
    short track = Short.parseShort(ctx.getText());
    if (!STATIC_ROUTE_TRACK_RANGE.contains((int) track)) {
      _w.redFlag(
          String.format(
              "Expected track in range %s, but got '%d' in: %s",
              STATIC_ROUTE_TRACK_RANGE, track, getFullText(messageCtx)));
      return null;
    }
    return track;
  }

  private @Nonnull StandardCommunity toStandardCommunity(Literal_standard_communityContext ctx) {
    return StandardCommunity.of(toInteger(ctx.high), toInteger(ctx.low));
  }

  private @Nonnull Optional<StandardCommunity> toStandardCommunity(Standard_communityContext ctx) {
    if (ctx.literal != null) {
      return Optional.of(toStandardCommunity(ctx.literal));
    } else if (ctx.INTERNET() != null) {
      return Optional.of(StandardCommunity.of(WellKnownCommunity.INTERNET));
    } else if (ctx.LOCAL_AS() != null) {
      return Optional.of(StandardCommunity.of(WellKnownCommunity.NO_EXPORT_SUBCONFED));
    } else if (ctx.NO_ADVERTISE() != null) {
      return Optional.of(StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE));
    } else if (ctx.NO_EXPORT() != null) {
      return Optional.of(StandardCommunity.of(WellKnownCommunity.NO_EXPORT));
    } else {
      // assume valid but unsupported
      todo(ctx);
      return Optional.empty();
    }
  }

  private @Nonnull Optional<Set<StandardCommunity>> toStandardCommunitySet(
      Iterable<Standard_communityContext> communities) {
    ImmutableSet.Builder<StandardCommunity> builder = ImmutableSet.builder();
    for (Standard_communityContext communityCtx : communities) {
      Optional<StandardCommunity> community = toStandardCommunity(communityCtx);
      if (!community.isPresent()) {
        return Optional.empty();
      }
      builder.add(community.get());
    }
    return Optional.of(builder.build());
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, As_path_regexContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx,
        ctx.dqs.text,
        IP_AS_PATH_ACCESS_LIST_REGEX_LENGTH_RANGE,
        "ip as-path access-list line regex");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Interface_descriptionContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, INTERFACE_DESCRIPTION_LENGTH_RANGE, "interface description");
  }

  public @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Interface_nameContext ctx) {
    String declaredName = getFullText(ctx);
    String prefix = ctx.prefix.getText();
    CiscoNxosInterfaceType type = toType(ctx.prefix);
    if (type == null) {
      _w.addWarning(
          messageCtx,
          getFullText(messageCtx),
          _parser,
          String.format("Unsupported interface type: %s", prefix));
      return Optional.empty();
    }
    String canonicalPrefix = getCanonicalInterfaceNamePrefix(prefix);
    if (canonicalPrefix == null) {
      _w.addWarning(
          messageCtx,
          getFullText(messageCtx),
          _parser,
          String.format("Unsupported interface name: %s", declaredName));
      return Optional.empty();
    }
    String middle = ctx.middle != null ? ctx.middle.getText() : "";
    String parentSuffix = ctx.parent_suffix != null ? ctx.parent_suffix.getText() : "";
    String lead = String.format("%s%s%s", canonicalPrefix, middle, parentSuffix);
    int first = toInteger(ctx.first);
    return Optional.of(String.format("%s%d", lead, first));
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Ip_access_list_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, IP_ACCESS_LIST_NAME_LENGTH_RANGE, "ip access-list name");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Ip_as_path_access_list_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, IP_AS_PATH_ACCESS_LIST_NAME_LENGTH_RANGE, "ip as-path access-list name");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Ip_community_list_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, IP_COMMUNITY_LIST_NAME_LENGTH_RANGE, "ip community-list name");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Ip_prefix_list_descriptionContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, IP_PREFIX_LIST_DESCRIPTION_LENGTH_RANGE, "ip prefix-list description");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Ip_prefix_list_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, IP_PREFIX_LIST_NAME_LENGTH_RANGE, "ip prefix-list name");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Object_group_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, OBJECT_GROUP_NAME_LENGTH_RANGE, "object-group name");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Route_map_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, ROUTE_MAP_NAME_LENGTH_RANGE, "route-map name");
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Router_ospf_nameContext ctx) {
    Optional<String> procName =
        toStringWithLengthInSpace(
            messageCtx, ctx, OSPF_PROCESS_NAME_LENGTH_RANGE, "OSPF process name");
    // OSPF process name is case-insensitive.
    return procName.map(name -> getPreferredName(name, ROUTER_OSPF));
  }

  private @Nullable String toString(ParserRuleContext messageCtx, Static_route_nameContext ctx) {
    String name = ctx.getText();
    if (name.length() > StaticRoute.MAX_NAME_LENGTH) {
      _w.redFlag(
          String.format(
              "Expected name <= %d characters,but got '%s' in: %s",
              StaticRoute.MAX_NAME_LENGTH, name, getFullText(messageCtx)));
      return null;
    }
    return name;
  }

  private @Nonnull Optional<String> toString(
      ParserRuleContext messageCtx, Template_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, BGP_TEMPLATE_NAME_LENGTH_RANGE, "bgp template name");
  }

  private @Nonnull Optional<String> toString(ParserRuleContext messageCtx, Vrf_nameContext ctx) {
    Optional<String> vrfName =
        toStringWithLengthInSpace(messageCtx, ctx, VRF_NAME_LENGTH_RANGE, "VRF name");
    // VRF names are case-insensitive.
    return vrfName.map(name -> getPreferredName(name, VRF));
  }

  /**
   * Return the text of the provided {@code ctx} if its length is within the provided {@link
   * IntegerSpace lengthSpace}, or else {@link Optional#empty}.
   */
  private @Nonnull Optional<String> toStringWithLengthInSpace(
      ParserRuleContext messageCtx, ParserRuleContext ctx, IntegerSpace lengthSpace, String name) {
    String text = ctx.getText();
    if (!lengthSpace.contains(text.length())) {
      _w.addWarning(
          messageCtx,
          getFullText(messageCtx),
          _parser,
          String.format(
              "Expected %s with length in range %s, but got '%s'", text, lengthSpace, name));
      return Optional.empty();
    }
    return Optional.of(text);
  }

  private @Nullable CiscoNxosInterfaceType toType(Interface_prefixContext ctx) {
    if (ctx.ETHERNET() != null) {
      return CiscoNxosInterfaceType.ETHERNET;
    } else if (ctx.LOOPBACK() != null) {
      return CiscoNxosInterfaceType.LOOPBACK;
    } else if (ctx.MGMT() != null) {
      return CiscoNxosInterfaceType.MGMT;
    } else if (ctx.PORT_CHANNEL() != null) {
      return CiscoNxosInterfaceType.PORT_CHANNEL;
    } else if (ctx.VLAN() != null) {
      return CiscoNxosInterfaceType.VLAN;
    }
    return convProblem(CiscoNxosInterfaceType.class, ctx, null);
  }

  private @Nullable Integer toVlanId(ParserRuleContext messageCtx, Vlan_idContext ctx) {
    int vlan = Integer.parseInt(ctx.getText());
    if (!_currentValidVlanRange.contains(vlan)) {
      _w.redFlag(
          String.format(
              "Expected VLAN in range %s, but got '%d' in: %s",
              _currentValidVlanRange, vlan, getFullText(messageCtx)));
      return null;
    }
    return vlan;
  }

  private @Nullable IntegerSpace toVlanIdRange(
      ParserRuleContext messageCtx, Vlan_id_rangeContext ctx) {
    String rangeText = ctx.getText();
    IntegerSpace value = IntegerSpace.parse(rangeText);
    if (!_currentValidVlanRange.contains(value)) {
      _w.redFlag(
          String.format(
              "Expected VLANs in range %s, but got '%s' in: %s",
              _currentValidVlanRange, rangeText, getFullText(messageCtx)));
      return null;
    }
    return value;
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    _configuration.setUnrecognized(true);

    if (token instanceof UnrecognizedLineToken) {
      UnrecognizedLineToken unrecToken = (UnrecognizedLineToken) token;
      _w.getParseWarnings()
          .add(
              new ParseWarning(
                  line, lineText, unrecToken.getParserContext(), "This syntax is unrecognized"));
    } else {
      String msg = String.format("Unrecognized Line: %d: %s", line, lineText);
      _w.redFlag(msg + " SUBSEQUENT LINES MAY NOT BE PROCESSED CORRECTLY");
    }
  }
}
