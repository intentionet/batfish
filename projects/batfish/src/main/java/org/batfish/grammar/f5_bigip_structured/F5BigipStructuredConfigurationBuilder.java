package org.batfish.grammar.f5_bigip_structured;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_NEIGHBOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_PROCESS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.INTERFACE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_HTTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_HTTPS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.NODE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE_SOURCE_ADDR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.POOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PREFIX_LIST;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_CLIENT_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_HTTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_OCSP_STAPLING_PARAMS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_ONE_CONNECT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SERVER_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_TCP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.ROUTE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.ROUTE_MAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.RULE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SELF;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNAT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNATPOOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNAT_TRANSLATION;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VIRTUAL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VIRTUAL_ADDRESS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VLAN;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VLAN_MEMBER_INTERFACE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.BGP_ADDRESS_FAMILY_REDISTRIBUTE_KERNEL_ROUTE_MAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.BGP_NEIGHBOR_IPV4_ROUTE_MAP_OUT;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.BGP_NEIGHBOR_IPV6_ROUTE_MAP_OUT;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.BGP_NEIGHBOR_SELF_REFERENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.BGP_NEIGHBOR_UPDATE_SOURCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.BGP_PROCESS_SELF_REFERENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.INTERFACE_SELF_REFERENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.MONITOR_HTTPS_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.MONITOR_HTTPS_SSL_PROFILE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.MONITOR_HTTP_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PERSISTENCE_SOURCE_ADDR_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PERSISTENCE_SSL_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.POOL_MEMBER;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.POOL_MONITOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PROFILE_CLIENT_SSL_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PROFILE_HTTP_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PROFILE_OCSP_STAPLING_PARAMS_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PROFILE_ONE_CONNECT_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PROFILE_SERVER_SSL_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.PROFILE_TCP_DEFAULTS_FROM;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.ROUTE_MAP_MATCH_IPV4_ADDRESS_PREFIX_LIST;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.ROUTE_SELF_REFERENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.SELF_SELF_REFERENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.SELF_VLAN;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.SNATPOOL_MEMBERS_MEMBER;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.SNAT_SELF_REFERENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.SNAT_SNATPOOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.SNAT_VLANS_VLAN;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.TRUNK_INTERFACE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_DESTINATION;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_PERSIST_PERSISTENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_POOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_PROFILE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_RULES_RULE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_SELF_REFERENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_SOURCE_ADDRESS_TRANSLATION_POOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VIRTUAL_VLANS_VLAN;
import static org.batfish.representation.f5_bigip.F5BigipStructureUsage.VLAN_INTERFACE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.vendor_family.f5_bigip.Pool;
import org.batfish.datamodel.vendor_family.f5_bigip.PoolMember;
import org.batfish.datamodel.vendor_family.f5_bigip.RouteAdvertisementMode;
import org.batfish.datamodel.vendor_family.f5_bigip.Virtual;
import org.batfish.datamodel.vendor_family.f5_bigip.VirtualAddress;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Bundle_speedContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.F5_bigip_structured_configurationContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Imish_chunkContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ip_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ip_address_portContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ip_prefixContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ip_protocolContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ipv6_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ipv6_address_portContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ipv6_prefixContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_nodeContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_poolContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_ruleContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_snatContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_snat_translationContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_snatpoolContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_virtualContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.L_virtual_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lm_httpContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lm_httpsContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lmh_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lmhs_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lmhs_ssl_profileContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ln_address6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ln_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lp_descriptionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lp_monitorContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lper_source_addrContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lper_sslContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lpersa_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lperss_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lpm_memberContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lpmm_address6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lpmm_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lpmm_descriptionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprof_client_sslContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprof_httpContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprof_ocsp_stapling_paramsContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprof_one_connectContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprof_server_sslContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprof_tcpContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprofcs_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprofh_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprofoc_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprofon_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lprofss_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lproft_defaults_fromContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ls_snatpoolContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ls_vlans_disabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ls_vlans_enabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lso_origin6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lso_originContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lspm_memberContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lst_address6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lst_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lsv_vlanContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_descriptionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_destinationContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_disabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_enabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_ip_forwardContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_ip_protocolContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_mask6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_maskContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_poolContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_profiles_profileContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_rejectContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_source6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_sourceContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_translate_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_translate_portContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_vlans_disabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lv_vlans_enabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lva_address6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lva_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lva_arpContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lva_icmp_echoContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lva_mask6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lva_maskContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lva_route_advertisementContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lvp_persistenceContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lvr_ruleContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lvsat_poolContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Lvv_vlanContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Net_interfaceContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Net_routeContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Net_selfContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Net_trunkContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Net_vlanContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ni_bundleContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ni_disabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ni_enabledContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nr_bgpContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nr_prefix_listContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nr_route_mapContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrb_local_asContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrb_router_id6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrb_router_idContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbaf_ipv4Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbaf_ipv6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbafcr_kernelContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbafcrk_route_mapContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbn_nameContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnn_descriptionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnn_ebgp_multihopContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnn_remote_asContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnn_update_sourceContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnnaf_ipv4Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnnaf_ipv6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnnafc_activateContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrbnnafcr_outContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nreem4a_prefix_listContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nreesc_valueContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nroute_gw6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nroute_gwContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nroute_network6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nroute_networkContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrp_route_domainContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrpe_entryContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrpee_actionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrpee_prefix6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrpee_prefixContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrpee_prefix_len_rangeContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrr_route_domainContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrre_entryContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nrree_actionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ns_address6Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ns_addressContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ns_allow_serviceContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ns_traffic_groupContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ns_vlanContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nti_interfaceContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Ntp_serversContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nv_tagContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Nvi_interfaceContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Prefix_len_rangeContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Prefix_list_actionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Route_advertisement_modeContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Route_map_actionContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Sgs_hostnameContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Standard_communityContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Structure_nameContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Structure_name_with_portContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Uint16Context;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.UnrecognizedContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Vlan_idContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.WordContext;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredParser.Word_portContext;
import org.batfish.representation.f5_bigip.BgpAddressFamily;
import org.batfish.representation.f5_bigip.BgpIpv4AddressFamily;
import org.batfish.representation.f5_bigip.BgpNeighbor;
import org.batfish.representation.f5_bigip.BgpNeighborAddressFamily;
import org.batfish.representation.f5_bigip.BgpProcess;
import org.batfish.representation.f5_bigip.BgpRedistributionPolicy;
import org.batfish.representation.f5_bigip.BuiltinMonitor;
import org.batfish.representation.f5_bigip.BuiltinPersistence;
import org.batfish.representation.f5_bigip.BuiltinProfile;
import org.batfish.representation.f5_bigip.F5BigipConfiguration;
import org.batfish.representation.f5_bigip.F5BigipRoutingProtocol;
import org.batfish.representation.f5_bigip.F5BigipStructureType;
import org.batfish.representation.f5_bigip.F5BigipStructureUsage;
import org.batfish.representation.f5_bigip.Interface;
import org.batfish.representation.f5_bigip.Ipv4Origin;
import org.batfish.representation.f5_bigip.Ipv6Origin;
import org.batfish.representation.f5_bigip.Node;
import org.batfish.representation.f5_bigip.PrefixList;
import org.batfish.representation.f5_bigip.PrefixListEntry;
import org.batfish.representation.f5_bigip.Route;
import org.batfish.representation.f5_bigip.RouteMap;
import org.batfish.representation.f5_bigip.RouteMapEntry;
import org.batfish.representation.f5_bigip.RouteMapMatchPrefixList;
import org.batfish.representation.f5_bigip.RouteMapSetCommunity;
import org.batfish.representation.f5_bigip.Self;
import org.batfish.representation.f5_bigip.Snat;
import org.batfish.representation.f5_bigip.SnatPool;
import org.batfish.representation.f5_bigip.SnatTranslation;
import org.batfish.representation.f5_bigip.Trunk;
import org.batfish.representation.f5_bigip.Vlan;
import org.batfish.representation.f5_bigip.VlanInterface;
import org.batfish.vendor.StructureType;

@ParametersAreNonnullByDefault
public class F5BigipStructuredConfigurationBuilder extends F5BigipStructuredParserBaseListener {

  private static int toInteger(Ip_address_portContext ctx) {
    String text = ctx.getText();
    return Integer.parseInt(text.substring(text.indexOf(":") + 1));
  }

  private static int toInteger(Ipv6_address_portContext ctx) {
    String text = ctx.getText();
    return Integer.parseInt(text.substring(text.lastIndexOf(".") + 1));
  }

  private static int toInteger(Uint16Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(Vlan_idContext ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(Word_portContext ctx) {
    String text = ctx.getText();
    return Integer.parseInt(text.substring(text.lastIndexOf(":") + 1));
  }

  private static @Nonnull InterfaceAddress toInterfaceAddress(Ip_prefixContext ctx) {
    return new InterfaceAddress(ctx.getText());
  }

  private static @Nonnull Ip toIp(Ip_address_portContext ctx) {
    String text = ctx.getText();
    return Ip.parse(text.substring(0, text.indexOf(":")));
  }

  private static @Nonnull Ip toIp(Ip_addressContext ctx) {
    return Ip.parse(ctx.getText());
  }

  private static @Nonnull Ip6 toIp6(Ipv6_address_portContext ctx) {
    String text = ctx.getText();
    return Ip6.parse(text.substring(0, text.lastIndexOf(".")));
  }

  private static @Nonnull Ip6 toIp6(Ipv6_addressContext ctx) {
    return Ip6.parse(ctx.getText());
  }

  private static long toLong(ParserRuleContext ctx) {
    return Long.parseUnsignedLong(ctx.getText(), 10);
  }

  private static @Nonnull String toName(Structure_name_with_portContext ctx) {
    String unqualifiedName;
    if (ctx.ipp != null) {
      unqualifiedName = toIp(ctx.ipp).toString();
    } else if (ctx.ip6p != null) {
      unqualifiedName = toIp6(ctx.ip6p).toString();
    } else {
      unqualifiedName = toName(ctx.wp);
    }
    return ctx.partition != null
        ? String.format("%s%s", ctx.partition.getText(), unqualifiedName)
        : unqualifiedName;
  }

  private static @Nonnull String toName(Structure_nameContext ctx) {
    return ctx.getText();
  }

  private static @Nonnull String toName(Word_portContext ctx) {
    String text = ctx.getText();
    return text.substring(0, text.lastIndexOf(":"));
  }

  private static int toPort(Structure_name_with_portContext ctx) {
    if (ctx.ipp != null) {
      return toInteger(ctx.ipp);
    } else if (ctx.ip6p != null) {
      return toInteger(ctx.ip6p);
    } else {
      return toInteger(ctx.wp);
    }
  }

  private static @Nonnull Prefix toPrefix(Ip_prefixContext ctx) {
    return Prefix.parse(ctx.getText());
  }

  private static @Nonnull Prefix6 toPrefix6(Ipv6_prefixContext ctx) {
    return new Prefix6(ctx.getText());
  }

  private static @Nonnull String unquote(String text) {
    if (text.length() < 2 || text.charAt(0) != '"' || text.charAt(text.length() - 1) != '"') {
      return text;
    } else {
      return text.substring(1, text.length() - 1);
    }
  }

  private @Nullable F5BigipConfiguration _c;
  private @Nullable BgpAddressFamily _currentBgpAddressFamily;
  private @Nullable BgpNeighbor _currentBgpNeighbor;
  private @Nullable BgpNeighborAddressFamily _currentBgpNeighborAddressFamily;
  private @Nullable BgpProcess _currentBgpProcess;
  private @Nullable BgpRedistributionPolicy _currentBgpRedistributionPolicy;
  private @Nullable Interface _currentInterface;
  private @Nullable Node _currentNode;
  private @Nullable Pool _currentPool;
  private @Nullable PoolMember _currentPoolMember;
  private @Nullable PrefixList _currentPrefixList;
  private @Nullable PrefixListEntry _currentPrefixListEntry;
  private @Nullable Route _currentRoute;
  private @Nullable RouteMap _currentRouteMap;
  private @Nullable RouteMapEntry _currentRouteMapEntry;
  private @Nullable Self _currentSelf;
  private @Nullable Snat _currentSnat;
  private @Nullable SnatPool _currentSnatPool;
  private @Nullable SnatTranslation _currentSnatTranslation;
  private @Nullable Trunk _currentTrunk;
  private @Nullable UnrecognizedContext _currentUnrecognized;
  private @Nullable Virtual _currentVirtual;
  private @Nullable VirtualAddress _currentVirtualAddress;
  private @Nullable Vlan _currentVlan;
  private @Nullable Integer _imishConfigurationLine;
  private @Nullable Integer _imishConfigurationOffset;
  private final @Nonnull F5BigipStructuredCombinedParser _parser;
  private final @Nonnull String _text;
  private final @Nonnull Warnings _w;

  public F5BigipStructuredConfigurationBuilder(
      F5BigipStructuredCombinedParser parser, String text, Warnings w) {
    _parser = parser;
    _text = text;
    _w = w;
  }

  private @Nonnull String convErrorMessage(Class<?> type, ParserRuleContext ctx) {
    return String.format("Could not convert to %s: %s", type.getSimpleName(), getFullText(ctx));
  }

  private @Nullable <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, @Nullable U defaultReturnValue) {
    _w.redFlag(convErrorMessage(returnType, ctx));
    return defaultReturnValue;
  }

  /** Mark the specified structure as defined on each line in the supplied context */
  private void defineStructure(StructureType type, String name, RuleContext ctx) {
    /* Recursively process children to find all relevant definition lines for the specified context */
    for (int i = 0; i < ctx.getChildCount(); i++) {
      ParseTree child = ctx.getChild(i);
      if (child instanceof TerminalNode) {
        _c.defineStructure(type, name, ((TerminalNode) child).getSymbol().getLine());
      } else if (child instanceof RuleContext) {
        defineStructure(type, name, (RuleContext) child);
      }
    }
  }

  @Override
  public void enterF5_bigip_structured_configuration(F5_bigip_structured_configurationContext ctx) {
    _c = new F5BigipConfiguration();
  }

  @Override
  public void enterL_node(L_nodeContext ctx) {
    String name = toName(ctx.name);
    defineStructure(NODE, name, ctx);
    _currentNode = _c.getNodes().computeIfAbsent(name, Node::new);
  }

  @Override
  public void enterL_pool(L_poolContext ctx) {
    String name = toName(ctx.name);
    defineStructure(POOL, name, ctx);
    _currentPool = _c.getPools().computeIfAbsent(name, Pool::new);
  }

  @Override
  public void enterL_rule(L_ruleContext ctx) {
    String name = toName(ctx.name);
    defineStructure(RULE, name, ctx);
  }

  @Override
  public void enterL_snat(L_snatContext ctx) {
    String name = toName(ctx.name);
    defineStructure(SNAT, name, ctx);
    _c.referenceStructure(SNAT, name, SNAT_SELF_REFERENCE, ctx.name.getStart().getLine());
    _currentSnat = _c.getSnats().computeIfAbsent(name, Snat::new);
  }

  @Override
  public void enterL_snat_translation(L_snat_translationContext ctx) {
    String name = toName(ctx.name);
    defineStructure(SNAT_TRANSLATION, name, ctx);
    _currentSnatTranslation = _c.getSnatTranslations().computeIfAbsent(name, SnatTranslation::new);
  }

  @Override
  public void enterL_snatpool(L_snatpoolContext ctx) {
    String name = toName(ctx.name);
    defineStructure(SNATPOOL, name, ctx);
    _currentSnatPool = _c.getSnatPools().computeIfAbsent(name, SnatPool::new);
  }

  @Override
  public void enterL_virtual(L_virtualContext ctx) {
    String name = toName(ctx.name);
    defineStructure(VIRTUAL, name, ctx);
    _c.referenceStructure(VIRTUAL, name, VIRTUAL_SELF_REFERENCE, ctx.name.getStart().getLine());
    _currentVirtual = _c.getVirtuals().computeIfAbsent(name, Virtual::new);
  }

  @Override
  public void enterL_virtual_address(L_virtual_addressContext ctx) {
    String name = toName(ctx.name);
    defineStructure(VIRTUAL_ADDRESS, name, ctx);
    _currentVirtualAddress = _c.getVirtualAddresses().computeIfAbsent(name, VirtualAddress::new);
  }

  @Override
  public void enterLm_http(Lm_httpContext ctx) {
    String name = toName(ctx.name);
    defineStructure(MONITOR_HTTP, name, ctx);
  }

  @Override
  public void enterLm_https(Lm_httpsContext ctx) {
    String name = toName(ctx.name);
    defineStructure(MONITOR_HTTPS, name, ctx);
  }

  @Override
  public void enterLper_source_addr(Lper_source_addrContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PERSISTENCE_SOURCE_ADDR, name, ctx);
  }

  @Override
  public void enterLper_ssl(Lper_sslContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PERSISTENCE_SSL, name, ctx);
  }

  @Override
  public void enterLpm_member(Lpm_memberContext ctx) {
    Structure_name_with_portContext np = ctx.name_with_port;
    String name = toName(np);
    int port = toPort(np);
    _c.referenceStructure(NODE, name, POOL_MEMBER, np.getStart().getLine());
    _currentPoolMember =
        _currentPool.getMembers().computeIfAbsent(np.getText(), n -> new PoolMember(n, name, port));
  }

  @Override
  public void enterLprof_client_ssl(Lprof_client_sslContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PROFILE_CLIENT_SSL, name, ctx);
  }

  @Override
  public void enterLprof_http(Lprof_httpContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PROFILE_HTTP, name, ctx);
  }

  @Override
  public void enterLprof_ocsp_stapling_params(Lprof_ocsp_stapling_paramsContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PROFILE_OCSP_STAPLING_PARAMS, name, ctx);
  }

  @Override
  public void enterLprof_one_connect(Lprof_one_connectContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PROFILE_ONE_CONNECT, name, ctx);
  }

  @Override
  public void enterLprof_server_ssl(Lprof_server_sslContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PROFILE_SERVER_SSL, name, ctx);
  }

  @Override
  public void enterLprof_tcp(Lprof_tcpContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PROFILE_TCP, name, ctx);
  }

  @Override
  public void enterLv_profiles_profile(Lv_profiles_profileContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(PROFILE, name, VIRTUAL_PROFILE, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void enterLva_route_advertisement(Lva_route_advertisementContext ctx) {
    _currentVirtualAddress.setRouteAdvertisementMode(toRouteAdvertisementMode(ctx.ramode));
  }

  @Override
  public void enterLvv_vlan(Lvv_vlanContext ctx) {
    String name = ctx.name.getText();
    _c.referenceStructure(VLAN, name, VIRTUAL_VLANS_VLAN, ctx.name.getStart().getLine());
    _currentVirtual.getVlans().add(name);
  }

  @Override
  public void enterNet_interface(Net_interfaceContext ctx) {
    String name = ctx.name.getText();
    defineStructure(INTERFACE, name, ctx);
    _c.referenceStructure(INTERFACE, name, INTERFACE_SELF_REFERENCE, ctx.name.getStart().getLine());
    _currentInterface = _c.getInterfaces().computeIfAbsent(name, Interface::new);
  }

  @Override
  public void enterNet_route(Net_routeContext ctx) {
    String name = ctx.name.getText();
    defineStructure(ROUTE, name, ctx);
    _c.referenceStructure(ROUTE, name, ROUTE_SELF_REFERENCE, ctx.name.getStart().getLine());
    _currentRoute = _c.getRoutes().computeIfAbsent(name, Route::new);
  }

  @Override
  public void enterNet_self(Net_selfContext ctx) {
    String name = toName(ctx.name);
    defineStructure(SELF, name, ctx);
    _c.referenceStructure(SELF, name, SELF_SELF_REFERENCE, ctx.name.getStart().getLine());
    _currentSelf = _c.getSelves().computeIfAbsent(name, Self::new);
  }

  @Override
  public void enterNet_trunk(Net_trunkContext ctx) {
    String name = toName(ctx.name);
    defineStructure(F5BigipStructureType.TRUNK, name, ctx);
    _currentTrunk = _c.getTrunks().computeIfAbsent(name, Trunk::new);
  }

  @Override
  public void enterNet_vlan(Net_vlanContext ctx) {
    String name = toName(ctx.name);
    defineStructure(VLAN, name, ctx);
    _currentVlan = _c.getVlans().computeIfAbsent(name, Vlan::new);
  }

  @Override
  public void enterNr_bgp(Nr_bgpContext ctx) {
    String name = toName(ctx.name);
    defineStructure(BGP_PROCESS, name, ctx);
    _c.referenceStructure(
        BGP_PROCESS, name, BGP_PROCESS_SELF_REFERENCE, ctx.name.getStart().getLine());
    _currentBgpProcess = _c.getBgpProcesses().computeIfAbsent(name, BgpProcess::new);
  }

  @Override
  public void enterNr_prefix_list(Nr_prefix_listContext ctx) {
    String name = toName(ctx.name);
    defineStructure(PREFIX_LIST, name, ctx);
    _currentPrefixList = _c.getPrefixLists().computeIfAbsent(name, PrefixList::new);
  }

  @Override
  public void enterNr_route_map(Nr_route_mapContext ctx) {
    String name = toName(ctx.name);
    defineStructure(ROUTE_MAP, name, ctx);
    _currentRouteMap = _c.getRouteMaps().computeIfAbsent(name, RouteMap::new);
  }

  @Override
  public void enterNrbaf_ipv4(Nrbaf_ipv4Context ctx) {
    _currentBgpAddressFamily = _currentBgpProcess.getIpv4AddressFamily();
  }

  @Override
  public void enterNrbaf_ipv6(Nrbaf_ipv6Context ctx) {
    _currentBgpAddressFamily = _currentBgpProcess.getIpv6AddressFamily();
  }

  @Override
  public void enterNrbafcr_kernel(Nrbafcr_kernelContext ctx) {
    _currentBgpRedistributionPolicy =
        _currentBgpAddressFamily
            .getRedistributionPolicies()
            .computeIfAbsent(F5BigipRoutingProtocol.KERNEL, BgpRedistributionPolicy::new);
  }

  @Override
  public void enterNrbn_name(Nrbn_nameContext ctx) {
    String name = ctx.name.getText();
    defineStructure(BGP_NEIGHBOR, name, ctx);
    _c.referenceStructure(BGP_NEIGHBOR, name, BGP_NEIGHBOR_SELF_REFERENCE, ctx.name.getLine());
    _currentBgpNeighbor = _currentBgpProcess.getNeighbors().computeIfAbsent(name, BgpNeighbor::new);
  }

  @Override
  public void enterNrbnnaf_ipv4(Nrbnnaf_ipv4Context ctx) {
    _currentBgpNeighborAddressFamily = _currentBgpNeighbor.getIpv4AddressFamily();
  }

  @Override
  public void enterNrbnnaf_ipv6(Nrbnnaf_ipv6Context ctx) {
    _currentBgpNeighborAddressFamily = _currentBgpNeighbor.getIpv6AddressFamily();
  }

  @Override
  public void enterNrpe_entry(Nrpe_entryContext ctx) {
    _currentPrefixListEntry =
        _currentPrefixList.getEntries().computeIfAbsent(toLong(ctx.num), PrefixListEntry::new);
  }

  @Override
  public void enterNrre_entry(Nrre_entryContext ctx) {
    _currentRouteMapEntry =
        _currentRouteMap.getEntries().computeIfAbsent(toLong(ctx.num), RouteMapEntry::new);
  }

  @Override
  public void enterUnrecognized(UnrecognizedContext ctx) {
    _c.setUnrecognized(true);
    if (_currentUnrecognized == null) {
      _currentUnrecognized = ctx;
    }
  }

  @Override
  public void exitBundle_speed(Bundle_speedContext ctx) {
    Double speed = toSpeed(ctx);
    _currentInterface.setSpeed(speed);
  }

  @Override
  public void exitImish_chunk(Imish_chunkContext ctx) {
    _imishConfigurationLine = ctx.getStart().getLine();
    _imishConfigurationOffset = ctx.getStart().getStartIndex();
  }

  @Override
  public void exitL_node(L_nodeContext ctx) {
    _currentNode = null;
  }

  @Override
  public void exitL_pool(L_poolContext ctx) {
    _currentPool = null;
  }

  @Override
  public void exitL_snat(L_snatContext ctx) {
    _currentSnat = null;
  }

  @Override
  public void exitL_snat_translation(L_snat_translationContext ctx) {
    _currentSnatTranslation = null;
  }

  @Override
  public void exitL_snatpool(L_snatpoolContext ctx) {
    _currentSnatPool = null;
  }

  @Override
  public void exitL_virtual(L_virtualContext ctx) {
    _currentVirtual = null;
  }

  @Override
  public void exitL_virtual_address(L_virtual_addressContext ctx) {
    _currentVirtualAddress = null;
  }

  @Override
  public void exitLmh_defaults_from(Lmh_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinMonitor.getBuiltinMonitor(name) == null) {
      _c.referenceStructure(
          MONITOR_HTTP, name, MONITOR_HTTP_DEFAULTS_FROM, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLmhs_defaults_from(Lmhs_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinMonitor.getBuiltinMonitor(name) == null) {
      _c.referenceStructure(
          MONITOR_HTTPS, name, MONITOR_HTTPS_DEFAULTS_FROM, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLmhs_ssl_profile(Lmhs_ssl_profileContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(
          PROFILE_SERVER_SSL, name, MONITOR_HTTPS_SSL_PROFILE, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLn_address(Ln_addressContext ctx) {
    _currentNode.setAddress(toIp(ctx.address));
  }

  @Override
  public void exitLn_address6(Ln_address6Context ctx) {
    _currentNode.setAddress6(toIp6(ctx.address));
  }

  @Override
  public void exitLp_description(Lp_descriptionContext ctx) {
    _currentPool.setDescription(unquote(ctx.text.getText()));
  }

  @Override
  public void exitLp_monitor(Lp_monitorContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinMonitor.getBuiltinMonitor(name) == null) {
      _c.referenceStructure(MONITOR, name, POOL_MONITOR, ctx.name.getStart().getLine());
    }
    _currentPool.setMonitor(name);
  }

  @Override
  public void exitLpersa_defaults_from(Lpersa_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinPersistence.getBuiltinPersistence(name) == null) {
      _c.referenceStructure(
          PERSISTENCE_SOURCE_ADDR,
          name,
          PERSISTENCE_SOURCE_ADDR_DEFAULTS_FROM,
          ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLperss_defaults_from(Lperss_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinPersistence.getBuiltinPersistence(name) == null) {
      _c.referenceStructure(
          PERSISTENCE_SSL, name, PERSISTENCE_SSL_DEFAULTS_FROM, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLpm_member(Lpm_memberContext ctx) {
    _currentPoolMember = null;
  }

  @Override
  public void exitLpmm_address(Lpmm_addressContext ctx) {
    _currentPoolMember.setAddress(toIp(ctx.address));
  }

  @Override
  public void exitLpmm_address6(Lpmm_address6Context ctx) {
    _currentPoolMember.setAddress6(toIp6(ctx.address6));
  }

  @Override
  public void exitLpmm_description(Lpmm_descriptionContext ctx) {
    _currentPoolMember.setDescription(unquote(ctx.text.getText()));
  }

  @Override
  public void exitLprofcs_defaults_from(Lprofcs_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(
          PROFILE_CLIENT_SSL,
          name,
          PROFILE_CLIENT_SSL_DEFAULTS_FROM,
          ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLprofh_defaults_from(Lprofh_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(
          PROFILE_HTTP, name, PROFILE_HTTP_DEFAULTS_FROM, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLprofoc_defaults_from(Lprofoc_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(
          PROFILE_OCSP_STAPLING_PARAMS,
          name,
          PROFILE_OCSP_STAPLING_PARAMS_DEFAULTS_FROM,
          ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLprofon_defaults_from(Lprofon_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(
          PROFILE_ONE_CONNECT,
          name,
          PROFILE_ONE_CONNECT_DEFAULTS_FROM,
          ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLprofss_defaults_from(Lprofss_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(
          PROFILE_SERVER_SSL,
          name,
          PROFILE_SERVER_SSL_DEFAULTS_FROM,
          ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLproft_defaults_from(Lproft_defaults_fromContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinProfile.getBuiltinProfile(name) == null) {
      _c.referenceStructure(
          PROFILE_TCP, name, PROFILE_TCP_DEFAULTS_FROM, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLs_snatpool(Ls_snatpoolContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(SNATPOOL, name, SNAT_SNATPOOL, ctx.name.getStart().getLine());
    _currentSnat.setSnatpool(name);
  }

  @Override
  public void exitLs_vlans_disabled(Ls_vlans_disabledContext ctx) {
    _currentSnat.setVlansEnabled(false);
  }

  @Override
  public void exitLs_vlans_enabled(Ls_vlans_enabledContext ctx) {
    _currentSnat.setVlansEnabled(true);
  }

  @Override
  public void exitLso_origin(Lso_originContext ctx) {
    _currentSnat.getIpv4Origins().computeIfAbsent(toPrefix(ctx.origin), Ipv4Origin::new);
  }

  @Override
  public void exitLso_origin6(Lso_origin6Context ctx) {
    _currentSnat.getIpv6Origins().computeIfAbsent(toPrefix6(ctx.origin6), Ipv6Origin::new);
  }

  @Override
  public void exitLspm_member(Lspm_memberContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(
        SNAT_TRANSLATION, name, SNATPOOL_MEMBERS_MEMBER, ctx.name.getStart().getLine());
    _currentSnatPool.getMembers().add(name);
  }

  @Override
  public void exitLst_address(Lst_addressContext ctx) {
    _currentSnatTranslation.setAddress(toIp(ctx.address));
  }

  @Override
  public void exitLst_address6(Lst_address6Context ctx) {
    _currentSnatTranslation.setAddress6(toIp6(ctx.address6));
  }

  @Override
  public void exitLsv_vlan(Lsv_vlanContext ctx) {
    String name = ctx.name.getText();
    _c.referenceStructure(VLAN, name, SNAT_VLANS_VLAN, ctx.name.getStart().getLine());
    _currentSnat.getVlans().add(name);
  }

  @Override
  public void exitLv_description(Lv_descriptionContext ctx) {
    _currentVirtual.setDescription(unquote(ctx.text.getText()));
  }

  @Override
  public void exitLv_destination(Lv_destinationContext ctx) {
    Structure_name_with_portContext np = ctx.name_with_port;
    String name = toName(np);
    int port = toPort(np);
    _c.referenceStructure(VIRTUAL_ADDRESS, name, VIRTUAL_DESTINATION, np.getStart().getLine());
    _currentVirtual.setDestination(name);
    _currentVirtual.setDestinationPort(port);
  }

  @Override
  public void exitLv_disabled(Lv_disabledContext ctx) {
    _currentVirtual.setDisabled(true);
  }

  @Override
  public void exitLv_enabled(Lv_enabledContext ctx) {
    _currentVirtual.setDisabled(false);
  }

  @Override
  public void exitLv_ip_forward(Lv_ip_forwardContext ctx) {
    if (_currentVirtual.getPool() != null) {
      _w.redFlag(
          String.format(
              "'ip-forward' mode incompatible with pool '%s' already configured on virtual '%s'",
              _currentVirtual.getPool(), _currentVirtual.getName()));
      return;
    }
    if (_currentVirtual.getReject()) {
      _w.redFlag(
          String.format(
              "'ip-forward' mode incompatible with 'reject' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    _currentVirtual.setIpForward(true);
    _currentVirtual.setTranslateAddress(false);
    _currentVirtual.setTranslatePort(false);
  }

  @Override
  public void exitLv_ip_protocol(Lv_ip_protocolContext ctx) {
    _currentVirtual.setIpProtocol(toIpProtocol(ctx.ip_protocol()));
  }

  @Override
  public void exitLv_mask(Lv_maskContext ctx) {
    _currentVirtual.setMask(toIp(ctx.mask));
  }

  @Override
  public void exitLv_mask6(Lv_mask6Context ctx) {
    _currentVirtual.setMask6(toIp6(ctx.mask6));
  }

  @Override
  public void exitLv_pool(Lv_poolContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(POOL, name, VIRTUAL_POOL, ctx.name.getStart().getLine());
    if (_currentVirtual.getIpForward()) {
      _w.redFlag(
          String.format(
              "'pool' incompatible with 'ip-forward' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    if (_currentVirtual.getReject()) {
      _w.redFlag(
          String.format(
              "'pool' incompatible with 'reject' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    _currentVirtual.setPool(name);
    if (_currentVirtual.getTranslateAddress() == null) {
      _currentVirtual.setTranslateAddress(true);
    }
    if (_currentVirtual.getTranslatePort() == null) {
      _currentVirtual.setTranslatePort(true);
    }
  }

  @Override
  public void exitLv_reject(Lv_rejectContext ctx) {
    if (_currentVirtual.getIpForward()) {
      _w.redFlag(
          String.format(
              "'reject' mode incompatible with 'ip-forward' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    if (_currentVirtual.getPool() != null) {
      _w.redFlag(
          String.format(
              "'reject' mode incompatible with pool '%s' already configured on virtual '%s'",
              _currentVirtual.getPool(), _currentVirtual.getName()));
      return;
    }
    _currentVirtual.setReject(true);
    _currentVirtual.setTranslateAddress(false);
    _currentVirtual.setTranslatePort(false);
  }

  @Override
  public void exitLv_source(Lv_sourceContext ctx) {
    _currentVirtual.setSource(toPrefix(ctx.source));
  }

  @Override
  public void exitLv_source6(Lv_source6Context ctx) {
    _currentVirtual.setSource6(toPrefix6(ctx.source6));
  }

  @Override
  public void exitLv_translate_address(Lv_translate_addressContext ctx) {
    boolean enabled = ctx.ENABLED() != null;
    if (enabled && _currentVirtual.getIpForward()) {
      _w.redFlag(
          String.format(
              "'translate-address enabled' incompatible with 'ip-forward' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    if (enabled && _currentVirtual.getReject()) {
      _w.redFlag(
          String.format(
              "'translate-address enabled' incompatible with 'reject' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    _currentVirtual.setTranslateAddress(enabled);
  }

  @Override
  public void exitLv_translate_port(Lv_translate_portContext ctx) {
    boolean enabled = ctx.ENABLED() != null;
    if (enabled && _currentVirtual.getIpForward()) {
      _w.redFlag(
          String.format(
              "'translate-port enabled' incompatible with 'ip-forward' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    if (enabled && _currentVirtual.getReject()) {
      _w.redFlag(
          String.format(
              "'translate-port enabled' incompatible with 'reject' mode already configured on virtual '%s'",
              _currentVirtual.getName()));
      return;
    }
    _currentVirtual.setTranslatePort(enabled);
  }

  @Override
  public void exitLv_vlans_disabled(Lv_vlans_disabledContext ctx) {
    _currentVirtual.setVlansEnabled(false);
  }

  @Override
  public void exitLv_vlans_enabled(Lv_vlans_enabledContext ctx) {
    _currentVirtual.setVlansEnabled(true);
  }

  @Override
  public void exitLva_address(Lva_addressContext ctx) {
    _currentVirtualAddress.setAddress(toIp(ctx.address));
  }

  @Override
  public void exitLva_address6(Lva_address6Context ctx) {
    _currentVirtualAddress.setAddress6(toIp6(ctx.address));
  }

  @Override
  public void exitLva_arp(Lva_arpContext ctx) {
    _currentVirtualAddress.setArpDisabled(ctx.DISABLED() != null);
  }

  @Override
  public void exitLva_icmp_echo(Lva_icmp_echoContext ctx) {
    _currentVirtualAddress.setIcmpEchoDisabled(ctx.DISABLED() != null);
  }

  @Override
  public void exitLva_mask(Lva_maskContext ctx) {
    _currentVirtualAddress.setMask(toIp(ctx.mask));
  }

  @Override
  public void exitLva_mask6(Lva_mask6Context ctx) {
    _currentVirtualAddress.setMask6(toIp6(ctx.mask6));
  }

  @Override
  public void exitLvp_persistence(Lvp_persistenceContext ctx) {
    String name = toName(ctx.name);
    if (BuiltinPersistence.getBuiltinPersistence(name) == null) {
      _c.referenceStructure(
          PERSISTENCE, name, VIRTUAL_PERSIST_PERSISTENCE, ctx.name.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitLvr_rule(Lvr_ruleContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(RULE, name, VIRTUAL_RULES_RULE, ctx.name.getStart().getLine());
  }

  @Override
  public void exitLvsat_pool(Lvsat_poolContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(
        SNATPOOL, name, VIRTUAL_SOURCE_ADDRESS_TRANSLATION_POOL, ctx.name.getStart().getLine());
    _currentVirtual.setSourceAddressTranslationPool(name);
  }

  @Override
  public void exitNet_interface(Net_interfaceContext ctx) {
    _currentInterface = null;
  }

  @Override
  public void exitNet_self(Net_selfContext ctx) {
    _currentSelf = null;
  }

  @Override
  public void exitNet_trunk(Net_trunkContext ctx) {
    _currentTrunk = null;
  }

  @Override
  public void exitNet_vlan(Net_vlanContext ctx) {
    _currentVlan = null;
  }

  @Override
  public void exitNi_bundle(Ni_bundleContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitNi_disabled(Ni_disabledContext ctx) {
    _currentInterface.setDisabled(true);
  }

  @Override
  public void exitNi_enabled(Ni_enabledContext ctx) {
    _currentInterface.setDisabled(false);
  }

  @Override
  public void exitNr_bgp(Nr_bgpContext ctx) {
    _currentBgpProcess = null;
  }

  @Override
  public void exitNr_prefix_list(Nr_prefix_listContext ctx) {
    _currentPrefixList = null;
  }

  @Override
  public void exitNr_route_map(Nr_route_mapContext ctx) {
    _currentRouteMap = null;
  }

  @Override
  public void exitNrb_local_as(Nrb_local_asContext ctx) {
    _currentBgpProcess.setLocalAs(toLong(ctx.as));
  }

  @Override
  public void exitNrb_router_id(Nrb_router_idContext ctx) {
    _currentBgpProcess.setRouterId(toIp(ctx.id));
  }

  @Override
  public void exitNrb_router_id6(Nrb_router_id6Context ctx) {
    todo(ctx);
  }

  @Override
  public void exitNrbaf_ipv4(Nrbaf_ipv4Context ctx) {
    _currentBgpAddressFamily = null;
  }

  @Override
  public void exitNrbaf_ipv6(Nrbaf_ipv6Context ctx) {
    _currentBgpAddressFamily = null;
  }

  @Override
  public void exitNrbafcr_kernel(Nrbafcr_kernelContext ctx) {
    _currentBgpRedistributionPolicy = null;
  }

  @Override
  public void exitNrbafcrk_route_map(Nrbafcrk_route_mapContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(
        ROUTE_MAP,
        name,
        BGP_ADDRESS_FAMILY_REDISTRIBUTE_KERNEL_ROUTE_MAP,
        ctx.name.getStart().getLine());
    _currentBgpRedistributionPolicy.setRouteMap(name);
  }

  @Override
  public void exitNrbn_name(Nrbn_nameContext ctx) {
    _currentBgpNeighbor = null;
  }

  @Override
  public void exitNrbnn_description(Nrbnn_descriptionContext ctx) {
    _currentBgpNeighbor.setDescription(unquote(ctx.description.getText()));
  }

  @Override
  public void exitNrbnn_ebgp_multihop(Nrbnn_ebgp_multihopContext ctx) {
    _currentBgpNeighbor.setEbgpMultihop(toInteger(ctx.count));
  }

  @Override
  public void exitNrbnn_remote_as(Nrbnn_remote_asContext ctx) {
    _currentBgpNeighbor.setRemoteAs(toLong(ctx.as));
  }

  @Override
  public void exitNrbnn_update_source(Nrbnn_update_sourceContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(VLAN, name, BGP_NEIGHBOR_UPDATE_SOURCE, ctx.name.getStart().getLine());
    _currentBgpNeighbor.setUpdateSource(name);
  }

  @Override
  public void exitNrbnnaf_ipv4(Nrbnnaf_ipv4Context ctx) {
    _currentBgpNeighborAddressFamily = null;
  }

  @Override
  public void exitNrbnnaf_ipv6(Nrbnnaf_ipv6Context ctx) {
    _currentBgpNeighborAddressFamily = null;
  }

  @Override
  public void exitNrbnnafc_activate(Nrbnnafc_activateContext ctx) {
    _currentBgpNeighborAddressFamily.setActivate(ctx.DISABLED() == null);
  }

  @Override
  public void exitNrbnnafcr_out(Nrbnnafcr_outContext ctx) {
    String name = toName(ctx.name);
    F5BigipStructureUsage usage =
        _currentBgpAddressFamily instanceof BgpIpv4AddressFamily
            ? BGP_NEIGHBOR_IPV4_ROUTE_MAP_OUT
            : BGP_NEIGHBOR_IPV6_ROUTE_MAP_OUT;
    _c.referenceStructure(ROUTE_MAP, name, usage, ctx.name.getStart().getLine());
    _currentBgpNeighborAddressFamily.setRouteMapOut(name);
  }

  @Override
  public void exitNreem4a_prefix_list(Nreem4a_prefix_listContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(
        PREFIX_LIST, name, ROUTE_MAP_MATCH_IPV4_ADDRESS_PREFIX_LIST, ctx.name.getStart().getLine());
    _currentRouteMapEntry.setMatchPrefixList(new RouteMapMatchPrefixList(name));
  }

  @Override
  public void exitNreesc_value(Nreesc_valueContext ctx) {
    _currentRouteMapEntry.setSetCommunity(
        new RouteMapSetCommunity(
            ctx.communities.stream()
                .map(this::toCommunity)
                .filter(Objects::nonNull)
                .collect(ImmutableSet.toImmutableSet())));
  }

  @Override
  public void exitNroute_gw(Nroute_gwContext ctx) {
    Ip ip = toIp(ctx.gw);
    // Gateway IP is valid iff it is on a directly-connected network
    if (_c.getSelves().values().stream()
        .map(Self::getAddress)
        .filter(Objects::nonNull)
        .map(InterfaceAddress::getPrefix)
        .anyMatch(directlyConnectedNetwork -> directlyConnectedNetwork.containsIp(ip))) {
      _w.redFlag(
          String.format(
              "Cannot set gateway IP '%s' for route '%s' that is not on a directly-connected network in: %s",
              ip, _currentRoute.getName(), getFullText(ctx)));
    }
    _currentRoute.setGw(ip);
  }

  @Override
  public void exitNroute_gw6(Nroute_gw6Context ctx) {
    _currentRoute.setGw6(toIp6(ctx.gw6));
    todo(ctx);
  }

  @Override
  public void exitNroute_network(Nroute_networkContext ctx) {
    _currentRoute.setNetwork(ctx.network != null ? toPrefix(ctx.network) : Prefix.ZERO);
  }

  @Override
  public void exitNroute_network6(Nroute_network6Context ctx) {
    _currentRoute.setNetwork6(toPrefix6(ctx.network6));
    todo(ctx);
  }

  @Override
  public void exitNrp_route_domain(Nrp_route_domainContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitNrpe_entry(Nrpe_entryContext ctx) {
    _currentPrefixListEntry = null;
  }

  @Override
  public void exitNrpee_action(Nrpee_actionContext ctx) {
    _currentPrefixListEntry.setAction(toLineAction(ctx.action));
  }

  @Override
  public void exitNrpee_prefix(Nrpee_prefixContext ctx) {
    _currentPrefixListEntry.setPrefix(toPrefix(ctx.prefix));
  }

  @Override
  public void exitNrpee_prefix_len_range(Nrpee_prefix_len_rangeContext ctx) {
    _currentPrefixListEntry.setLengthRange(toSubRange(ctx.range));
  }

  @Override
  public void exitNrpee_prefix6(Nrpee_prefix6Context ctx) {
    _currentPrefixListEntry.setPrefix6(toPrefix6(ctx.prefix6));
  }

  @Override
  public void exitNrr_route_domain(Nrr_route_domainContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitNrre_entry(Nrre_entryContext ctx) {
    _currentRouteMapEntry = null;
  }

  @Override
  public void exitNrree_action(Nrree_actionContext ctx) {
    _currentRouteMapEntry.setAction(toLineAction(ctx.action));
  }

  @Override
  public void exitNs_address(Ns_addressContext ctx) {
    _currentSelf.setAddress(toInterfaceAddress(ctx.interface_address));
  }

  @Override
  public void exitNs_address6(Ns_address6Context ctx) {
    // TODO: implement IPv6 interface address
    todo(ctx);
  }

  @Override
  public void exitNs_allow_service(Ns_allow_serviceContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitNs_traffic_group(Ns_traffic_groupContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitNs_vlan(Ns_vlanContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(VLAN, name, SELF_VLAN, ctx.name.getStart().getLine());
    _currentSelf.setVlan(name);
  }

  @Override
  public void exitNti_interface(Nti_interfaceContext ctx) {
    String name = ctx.name.getText();
    if (!_c.getInterfaces().containsKey(name)) {
      _c.getInterfaces().put(name, new Interface(name));
      defineStructure(INTERFACE, name, ctx);
    }
    _c.referenceStructure(INTERFACE, name, TRUNK_INTERFACE, ctx.name.getStart().getLine());
    _currentTrunk.getInterfaces().add(name);
  }

  @Override
  public void exitNtp_servers(Ntp_serversContext ctx) {
    _c.setNtpServers(
        ctx.servers.stream().map(WordContext::getText).collect(ImmutableList.toImmutableList()));
  }

  @Override
  public void exitNv_tag(Nv_tagContext ctx) {
    _currentVlan.setTag(toInteger(ctx.tag));
  }

  @Override
  public void exitNvi_interface(Nvi_interfaceContext ctx) {
    String name = toName(ctx.name);
    _c.referenceStructure(
        VLAN_MEMBER_INTERFACE, name, VLAN_INTERFACE, ctx.name.getStart().getLine());
    _currentVlan.getInterfaces().computeIfAbsent(name, VlanInterface::new);
  }

  @Override
  public void exitSgs_hostname(Sgs_hostnameContext ctx) {
    String hostname = unquote(ctx.hostname.getText()).toLowerCase();
    _c.setHostname(hostname);
  }

  @Override
  public void exitUnrecognized(UnrecognizedContext ctx) {
    if (_currentUnrecognized != ctx) {
      return;
    }
    unrecognized(ctx);
    _currentUnrecognized = null;
  }

  public F5BigipConfiguration getConfiguration() {
    return _c;
  }

  private @Nonnull String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  public @Nullable Integer getImishConfigurationLine() {
    return _imishConfigurationLine;
  }

  @Nullable
  Integer getImishConfigurationOffset() {
    return _imishConfigurationOffset;
  }

  private @Nonnull String getUnrecognizedLeadText(UnrecognizedContext ctx) {
    if (ctx.u_if() != null) {
      return ctx.u_if().getText();
    } else {
      return _text.substring(
          ctx.getStart().getStartIndex(), ctx.last_word.getStop().getStopIndex() + 1);
    }
  }

  private @Nullable Long toCommunity(Standard_communityContext ctx) {
    if (ctx.STANDARD_COMMUNITY() != null) {
      return StandardCommunity.parse(ctx.getText()).asLong();
    } else {
      return convProblem(Long.class, ctx, null);
    }
  }

  private void todo(ParserRuleContext ctx) {
    _w.todo(ctx, getFullText(ctx), _parser);
  }

  private @Nullable IpProtocol toIpProtocol(Ip_protocolContext ctx) {
    if (ctx.TCP() != null) {
      return IpProtocol.TCP;
    } else if (ctx.UDP() != null) {
      return IpProtocol.UDP;
    } else {
      return convProblem(IpProtocol.class, ctx, null);
    }
  }

  private @Nullable LineAction toLineAction(Prefix_list_actionContext ctx) {
    if (ctx.PERMIT() != null) {
      return LineAction.PERMIT;
    } else if (ctx.DENY() != null) {
      return LineAction.DENY;
    } else {
      return convProblem(LineAction.class, ctx, null);
    }
  }

  private @Nullable LineAction toLineAction(Route_map_actionContext ctx) {
    if (ctx.PERMIT() != null) {
      return LineAction.PERMIT;
    } else if (ctx.DENY() != null) {
      return LineAction.DENY;
    } else {
      return convProblem(LineAction.class, ctx, null);
    }
  }

  private @Nullable RouteAdvertisementMode toRouteAdvertisementMode(
      Route_advertisement_modeContext ctx) {
    if (ctx.ALL() != null) {
      return RouteAdvertisementMode.ALL;
    } else if (ctx.ALWAYS() != null) {
      return RouteAdvertisementMode.ALWAYS;
    } else if (ctx.ANY() != null) {
      return RouteAdvertisementMode.ANY;
    } else if (ctx.DISABLED() != null) {
      return RouteAdvertisementMode.DISABLED;
    } else if (ctx.ENABLED() != null) {
      return RouteAdvertisementMode.ENABLED;
    } else if (ctx.SELECTIVE() != null) {
      return RouteAdvertisementMode.SELECTIVE;
    } else {
      return convProblem(RouteAdvertisementMode.class, ctx, null);
    }
  }

  private @Nullable Double toSpeed(Bundle_speedContext ctx) {
    if (ctx.FORTY_G() != null) {
      return 40E9D;
    } else if (ctx.ONE_HUNDRED_G() != null) {
      return 100E9D;
    } else {
      return convProblem(Double.class, ctx, null);
    }
  }

  private @Nullable SubRange toSubRange(Prefix_len_rangeContext ctx) {
    String[] parts = ctx.getText().split(":", -1);
    try {
      checkArgument(parts.length == 2);
      int low = Integer.parseInt(parts[0], 10);
      int high = Integer.parseInt(parts[1], 10);
      return new SubRange(low, high);
    } catch (IllegalArgumentException e) {
      return convProblem(SubRange.class, ctx, null);
    }
  }

  private void unrecognized(UnrecognizedContext ctx) {
    Token start = ctx.getStart();
    int line = start.getLine();
    _w.getParseWarnings()
        .add(
            new ParseWarning(
                line,
                getUnrecognizedLeadText(ctx),
                ctx.toString(Arrays.asList(_parser.getParser().getRuleNames())),
                "This syntax is unrecognized"));
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    _c.setUnrecognized(true);

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
