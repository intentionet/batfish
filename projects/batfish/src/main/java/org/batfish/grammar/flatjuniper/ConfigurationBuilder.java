package org.batfish.grammar.flatjuniper;

import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_GLOBAL_POLICY;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION_OR_APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureType.AUTHENTICATION_KEY_CHAIN;
import static org.batfish.representation.juniper.JuniperStructureType.BGP_GROUP;
import static org.batfish.representation.juniper.JuniperStructureType.DHCP_RELAY_SERVER_GROUP;
import static org.batfish.representation.juniper.JuniperStructureType.FIREWALL_FILTER;
import static org.batfish.representation.juniper.JuniperStructureType.IKE_GATEWAY;
import static org.batfish.representation.juniper.JuniperStructureType.IKE_POLICY;
import static org.batfish.representation.juniper.JuniperStructureType.IKE_PROPOSAL;
import static org.batfish.representation.juniper.JuniperStructureType.INTERFACE;
import static org.batfish.representation.juniper.JuniperStructureType.IPSEC_POLICY;
import static org.batfish.representation.juniper.JuniperStructureType.IPSEC_PROPOSAL;
import static org.batfish.representation.juniper.JuniperStructureType.POLICY_STATEMENT;
import static org.batfish.representation.juniper.JuniperStructureType.PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureType.VLAN;
import static org.batfish.representation.juniper.JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION;
import static org.batfish.representation.juniper.JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureUsage.AUTHENTICATION_KEY_CHAINS_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.BGP_ALLOW;
import static org.batfish.representation.juniper.JuniperStructureUsage.BGP_EXPORT_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.BGP_IMPORT_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.BGP_NEIGHBOR;
import static org.batfish.representation.juniper.JuniperStructureUsage.DHCP_RELAY_GROUP_ACTIVE_SERVER_GROUP;
import static org.batfish.representation.juniper.JuniperStructureUsage.FIREWALL_FILTER_DESTINATION_PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureUsage.FIREWALL_FILTER_PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureUsage.FIREWALL_FILTER_SOURCE_PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureUsage.FORWARDING_TABLE_EXPORT_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.GENERATED_ROUTE_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.IKE_GATEWAY_EXTERNAL_INTERFACE;
import static org.batfish.representation.juniper.JuniperStructureUsage.IKE_GATEWAY_IKE_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.IKE_POLICY_IKE_PROPOSAL;
import static org.batfish.representation.juniper.JuniperStructureUsage.INTERFACE_FILTER;
import static org.batfish.representation.juniper.JuniperStructureUsage.INTERFACE_INCOMING_FILTER;
import static org.batfish.representation.juniper.JuniperStructureUsage.INTERFACE_OUTGOING_FILTER;
import static org.batfish.representation.juniper.JuniperStructureUsage.INTERFACE_VLAN;
import static org.batfish.representation.juniper.JuniperStructureUsage.IPSEC_POLICY_IPSEC_PROPOSAL;
import static org.batfish.representation.juniper.JuniperStructureUsage.IPSEC_VPN_BIND_INTERFACE;
import static org.batfish.representation.juniper.JuniperStructureUsage.IPSEC_VPN_IKE_GATEWAY;
import static org.batfish.representation.juniper.JuniperStructureUsage.IPSEC_VPN_IPSEC_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.OSPF_EXPORT_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.POLICY_STATEMENT_POLICY;
import static org.batfish.representation.juniper.JuniperStructureUsage.POLICY_STATEMENT_PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureUsage.POLICY_STATEMENT_PREFIX_LIST_FILTER;
import static org.batfish.representation.juniper.JuniperStructureUsage.SECURITY_POLICY_MATCH_APPLICATION;
import static org.batfish.representation.juniper.JuniperStructureUsage.SNMP_COMMUNITY_PREFIX_LIST;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.JuniperUtils;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAuthenticationAlgorithm;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.ExtendedCommunity;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.IpsecProtocol;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SnmpCommunity;
import org.batfish.datamodel.SnmpHost;
import org.batfish.datamodel.SnmpServer;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.VrrpGroup;
import org.batfish.datamodel.isis.IsisAuthenticationAlgorithm;
import org.batfish.datamodel.isis.IsisHelloAuthenticationType;
import org.batfish.datamodel.isis.IsisOption;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.vendor_family.juniper.TacplusServer;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.A_applicationContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.A_application_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Aa_termContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Aas_applicationContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Aas_application_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Aat_destination_portContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Aat_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Aat_source_portContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.As_path_exprContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.As_unitContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_advertise_externalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_advertise_inactiveContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_advertise_peer_asContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_allowContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_authentication_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_authentication_keyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_authentication_key_chainContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_clusterContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_descriptionContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_enforce_first_asContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_exportContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_importContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_local_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_multihopContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_multipathContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_neighborContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_remove_privateContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.B_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Bl_loopsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Bl_numberContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Bl_privateContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Bpa_asContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Dh_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.DirectionContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ec_literalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ec_namedContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Encryption_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Extended_communityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.F_familyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.F_filterContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ff_termContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_destination_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_destination_portContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_destination_prefix_listContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_first_fragmentContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_fragment_offsetContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_fragment_offset_exceptContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_icmp_codeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_icmp_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_ip_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_is_fragmentContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_packet_lengthContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_packet_length_exceptContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_portContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_prefix_listContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_source_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_source_portContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_source_prefix_listContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_tcp_establishedContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_tcp_flagsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftf_tcp_initialContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftfa_address_mask_prefixContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftt_acceptContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftt_discardContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftt_next_ipContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftt_next_termContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftt_nopContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftt_rejectContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fftt_routing_instanceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.FilterContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fo_dhcp_relayContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fod_active_server_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fod_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fod_server_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Fodg_interfaceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Hello_authentication_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Hib_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Hib_system_serviceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.I_descriptionContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.I_disableContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.I_enableContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.I_mtuContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.I_unitContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Icmp_codeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Icmp_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ife_filterContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ife_port_modeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ife_vlanContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifi_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifi_filterContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifia_arpContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifia_preferredContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifia_primaryContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifia_vrrp_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifiav_preemptContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifiav_priorityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifiav_virtual_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ifiso_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ike_authentication_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ike_authentication_methodContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Int_namedContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Interface_idContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ip_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ipsec_authentication_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ipsec_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Is_exportContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Is_interfaceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Is_levelContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Is_no_ipv4_routingContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Is_reference_bandwidthContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isi_levelContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isi_passiveContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isi_point_to_pointContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isib_minimum_intervalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isib_multiplierContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isil_enableContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isil_hello_authentication_keyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isil_hello_authentication_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isil_hello_intervalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isil_hold_timeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isil_metricContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isil_te_metricContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isl_disableContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Isl_wide_metrics_onlyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Iso_timeoutContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ist_credibility_protocol_preferenceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ist_family_shortcutsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Junos_applicationContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Junos_application_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.O_areaContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.O_exportContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.O_reference_bandwidthContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oa_interfaceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oa_nssaContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oa_stubContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oai_interface_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oai_passiveContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oan_default_lsaContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oan_no_summariesContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oand_metric_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oand_type_7Context;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Oas_no_summariesContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.P_bgpContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Po_communityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Po_policy_statementContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Po_prefix_listContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Poc_invert_matchContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Poc_membersContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Policy_expressionContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Poplt_ip6Context;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Poplt_network6Context;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Poplt_networkContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Pops_commonContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Pops_termContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_as_pathContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_as_path_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_colorContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_communityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_familyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_instanceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_interfaceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_local_preferenceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_metricContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_prefix_listContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_prefix_list_filterContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_ribContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_route_filterContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsf_tagContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_address_maskContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_exactContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_longerContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_orlongerContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_prefix_length_rangeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_thenContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_throughContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popsfrf_uptoContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_acceptContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_as_path_prependContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_community_addContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_community_deleteContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_community_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_default_action_acceptContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_default_action_rejectContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_externalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_local_preferenceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_metricContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_metric_addContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_next_hopContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_next_hop_selfContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_next_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_next_termContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_originContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_preferenceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Popst_rejectContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.PortContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Proposal_set_typeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.RangeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Reference_bandwidthContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ri_interfaceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ri_named_routing_instanceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ro_aggregateContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ro_autonomous_systemContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ro_generateContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ro_ribContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ro_router_idContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ro_staticContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Roa_communityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Roa_preferenceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Roa_tagContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Roaa_pathContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rof_exportContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rog_metricContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rog_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Roifie_lanContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Roifie_point_to_pointContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Ros_routeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rosr_discardContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rosr_metricContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rosr_next_hopContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rosr_no_installContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rosr_preferenceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rosr_rejectContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Rosr_tagContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Routing_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.S_firewallContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.S_routing_optionsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.S_snmpContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.S_vlans_namedContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sc_literalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sc_namedContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Se_address_bookContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Se_authentication_key_chainContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Se_zonesContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sea_keyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sea_toleranceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sead_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sead_address_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seada_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seada_address_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seak_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seak_optionsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seak_secretContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seak_start_timeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seik_gatewayContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seik_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seik_proposalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikg_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikg_external_interfaceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikg_ike_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikg_local_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikp_pre_shared_keyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikp_proposal_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikp_proposalsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikpr_authentication_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikpr_authentication_methodContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikpr_dh_groupContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikpr_encryption_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seikpr_lifetime_secondsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seip_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seip_proposalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seip_vpnContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seipp_perfect_forward_secrecyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seipp_proposal_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seipp_proposalsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seippr_authentication_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seippr_encryption_algorithmContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seippr_lifetime_kilobytesContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seippr_lifetime_secondsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seippr_protocolContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seipv_bind_interfaceContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seipvi_gatewayContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Seipvi_ipsec_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sep_default_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sep_from_zoneContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sep_globalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sepctx_policyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sepctxpm_applicationContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sepctxpm_destination_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sepctxpm_source_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sepctxpm_source_identityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sepctxpt_denyContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sepctxpt_permitContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sez_security_zoneContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezs_address_bookContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezs_host_inbound_trafficContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezs_interfacesContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezsa_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezsa_address_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezsaad_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezsaad_address_setContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezsh_protocolsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sezsh_system_servicesContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Snmp_communityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Snmpc_authorizationContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Snmpc_client_list_nameContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Snmptg_targetsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Standard_communityContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.SubrangeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sy_default_address_selectionContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sy_domain_nameContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sy_host_nameContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sy_name_serverContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sy_tacplus_serverContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Syn_serverContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Syr_encrypted_passwordContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Sys_hostContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Syt_secretContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Syt_source_addressContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Tcp_flagsContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Tcp_flags_alternativeContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Tcp_flags_atomContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Tcp_flags_literalContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.VariableContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Vlt_vlan_idContext;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Wildcard_addressContext;
import org.batfish.representation.juniper.AddressAddressBookEntry;
import org.batfish.representation.juniper.AddressBook;
import org.batfish.representation.juniper.AddressBookEntry;
import org.batfish.representation.juniper.AddressSetAddressBookEntry;
import org.batfish.representation.juniper.AddressSetEntry;
import org.batfish.representation.juniper.AggregateRoute;
import org.batfish.representation.juniper.ApplicationOrApplicationSetReference;
import org.batfish.representation.juniper.ApplicationSet;
import org.batfish.representation.juniper.ApplicationSetMemberReference;
import org.batfish.representation.juniper.ApplicationSetReference;
import org.batfish.representation.juniper.BaseApplication;
import org.batfish.representation.juniper.BaseApplication.Term;
import org.batfish.representation.juniper.BgpGroup;
import org.batfish.representation.juniper.BgpGroup.BgpGroupType;
import org.batfish.representation.juniper.CommunityList;
import org.batfish.representation.juniper.CommunityListLine;
import org.batfish.representation.juniper.DhcpRelayGroup;
import org.batfish.representation.juniper.DhcpRelayServerGroup;
import org.batfish.representation.juniper.Family;
import org.batfish.representation.juniper.FirewallFilter;
import org.batfish.representation.juniper.FwFrom;
import org.batfish.representation.juniper.FwFromApplicationOrApplicationSet;
import org.batfish.representation.juniper.FwFromDestinationAddress;
import org.batfish.representation.juniper.FwFromDestinationAddressBookEntry;
import org.batfish.representation.juniper.FwFromDestinationAddressExcept;
import org.batfish.representation.juniper.FwFromDestinationPort;
import org.batfish.representation.juniper.FwFromDestinationPrefixList;
import org.batfish.representation.juniper.FwFromDestinationPrefixListExcept;
import org.batfish.representation.juniper.FwFromFragmentOffset;
import org.batfish.representation.juniper.FwFromHostProtocol;
import org.batfish.representation.juniper.FwFromHostService;
import org.batfish.representation.juniper.FwFromIcmpCode;
import org.batfish.representation.juniper.FwFromIcmpType;
import org.batfish.representation.juniper.FwFromJunosApplication;
import org.batfish.representation.juniper.FwFromJunosApplicationSet;
import org.batfish.representation.juniper.FwFromPacketLength;
import org.batfish.representation.juniper.FwFromPort;
import org.batfish.representation.juniper.FwFromPrefixList;
import org.batfish.representation.juniper.FwFromProtocol;
import org.batfish.representation.juniper.FwFromSourceAddress;
import org.batfish.representation.juniper.FwFromSourceAddressBookEntry;
import org.batfish.representation.juniper.FwFromSourceAddressExcept;
import org.batfish.representation.juniper.FwFromSourcePort;
import org.batfish.representation.juniper.FwFromSourcePrefixList;
import org.batfish.representation.juniper.FwFromSourcePrefixListExcept;
import org.batfish.representation.juniper.FwFromTcpFlags;
import org.batfish.representation.juniper.FwTerm;
import org.batfish.representation.juniper.FwThenAccept;
import org.batfish.representation.juniper.FwThenDiscard;
import org.batfish.representation.juniper.FwThenNextIp;
import org.batfish.representation.juniper.FwThenNextTerm;
import org.batfish.representation.juniper.FwThenNop;
import org.batfish.representation.juniper.GeneratedRoute;
import org.batfish.representation.juniper.HostProtocol;
import org.batfish.representation.juniper.HostSystemService;
import org.batfish.representation.juniper.IkeGateway;
import org.batfish.representation.juniper.IkePolicy;
import org.batfish.representation.juniper.IkeProposal;
import org.batfish.representation.juniper.Interface;
import org.batfish.representation.juniper.IpBgpGroup;
import org.batfish.representation.juniper.IpsecPolicy;
import org.batfish.representation.juniper.IpsecProposal;
import org.batfish.representation.juniper.IpsecVpn;
import org.batfish.representation.juniper.IsisInterfaceLevelSettings;
import org.batfish.representation.juniper.IsisLevelSettings;
import org.batfish.representation.juniper.IsisSettings;
import org.batfish.representation.juniper.JuniperAuthenticationKey;
import org.batfish.representation.juniper.JuniperAuthenticationKeyChain;
import org.batfish.representation.juniper.JuniperConfiguration;
import org.batfish.representation.juniper.JuniperStructureUsage;
import org.batfish.representation.juniper.JunosApplication;
import org.batfish.representation.juniper.JunosApplicationReference;
import org.batfish.representation.juniper.JunosApplicationSet;
import org.batfish.representation.juniper.JunosApplicationSetReference;
import org.batfish.representation.juniper.NamedBgpGroup;
import org.batfish.representation.juniper.NodeDevice;
import org.batfish.representation.juniper.NssaSettings;
import org.batfish.representation.juniper.OspfArea;
import org.batfish.representation.juniper.PolicyStatement;
import org.batfish.representation.juniper.PrefixList;
import org.batfish.representation.juniper.PsFrom;
import org.batfish.representation.juniper.PsFromAsPath;
import org.batfish.representation.juniper.PsFromColor;
import org.batfish.representation.juniper.PsFromCommunity;
import org.batfish.representation.juniper.PsFromFamilyInet;
import org.batfish.representation.juniper.PsFromFamilyInet6;
import org.batfish.representation.juniper.PsFromInterface;
import org.batfish.representation.juniper.PsFromLocalPreference;
import org.batfish.representation.juniper.PsFromMetric;
import org.batfish.representation.juniper.PsFromPolicyStatement;
import org.batfish.representation.juniper.PsFromPolicyStatementConjunction;
import org.batfish.representation.juniper.PsFromPrefixList;
import org.batfish.representation.juniper.PsFromPrefixListFilterLonger;
import org.batfish.representation.juniper.PsFromPrefixListFilterOrLonger;
import org.batfish.representation.juniper.PsFromProtocol;
import org.batfish.representation.juniper.PsFromRouteFilter;
import org.batfish.representation.juniper.PsFromTag;
import org.batfish.representation.juniper.PsTerm;
import org.batfish.representation.juniper.PsThen;
import org.batfish.representation.juniper.PsThenAccept;
import org.batfish.representation.juniper.PsThenAsPathPrepend;
import org.batfish.representation.juniper.PsThenCommunityAdd;
import org.batfish.representation.juniper.PsThenCommunityDelete;
import org.batfish.representation.juniper.PsThenCommunitySet;
import org.batfish.representation.juniper.PsThenDefaultActionAccept;
import org.batfish.representation.juniper.PsThenDefaultActionReject;
import org.batfish.representation.juniper.PsThenExternal;
import org.batfish.representation.juniper.PsThenLocalPreference;
import org.batfish.representation.juniper.PsThenMetric;
import org.batfish.representation.juniper.PsThenMetricAdd;
import org.batfish.representation.juniper.PsThenNextHopIp;
import org.batfish.representation.juniper.PsThenNextHopSelf;
import org.batfish.representation.juniper.PsThenNextPolicy;
import org.batfish.representation.juniper.PsThenOrigin;
import org.batfish.representation.juniper.PsThenPreference;
import org.batfish.representation.juniper.PsThenReject;
import org.batfish.representation.juniper.Route4FilterLine;
import org.batfish.representation.juniper.Route4FilterLineAddressMask;
import org.batfish.representation.juniper.Route4FilterLineExact;
import org.batfish.representation.juniper.Route4FilterLineLengthRange;
import org.batfish.representation.juniper.Route4FilterLineLonger;
import org.batfish.representation.juniper.Route4FilterLineOrLonger;
import org.batfish.representation.juniper.Route4FilterLineThrough;
import org.batfish.representation.juniper.Route4FilterLineUpTo;
import org.batfish.representation.juniper.Route6FilterLine;
import org.batfish.representation.juniper.Route6FilterLineAddressMask;
import org.batfish.representation.juniper.Route6FilterLineExact;
import org.batfish.representation.juniper.Route6FilterLineLengthRange;
import org.batfish.representation.juniper.Route6FilterLineLonger;
import org.batfish.representation.juniper.Route6FilterLineOrLonger;
import org.batfish.representation.juniper.Route6FilterLineThrough;
import org.batfish.representation.juniper.Route6FilterLineUpTo;
import org.batfish.representation.juniper.RouteFilter;
import org.batfish.representation.juniper.RoutingInformationBase;
import org.batfish.representation.juniper.RoutingInstance;
import org.batfish.representation.juniper.StaticRoute;
import org.batfish.representation.juniper.StubSettings;
import org.batfish.representation.juniper.Vlan;
import org.batfish.representation.juniper.Zone;
import org.batfish.vendor.StructureType;

public class ConfigurationBuilder extends FlatJuniperParserBaseListener {

  private static final boolean DEFAULT_VRRP_PREEMPT = true;

  private static final int DEFAULT_VRRP_PRIORITY = 100;

  private static final AggregateRoute DUMMY_AGGREGATE_ROUTE = new AggregateRoute(Prefix.ZERO);

  private static final BgpGroup DUMMY_BGP_GROUP = new BgpGroup();

  private static final StaticRoute DUMMY_STATIC_ROUTE = new StaticRoute(Prefix.ZERO);

  private static final String F_BGP_LOCAL_AS_LOOPS =
      "protocols - bgp - group? - local-as - loops - currently we allow infinite occurences of "
          + "local as";

  private static final String F_BGP_LOCAL_AS_PRIVATE =
      "protocols - bgp - group? - local-as - private";

  private static final String F_FIREWALL_TERM_THEN_ROUTING_INSTANCE =
      "firewall - filter - term - then - routing-instance";

  private static final String F_IPV6 = "ipv6 - other";

  private static final String F_ISIS_EXPORT = "protocols - isis - export";

  private static final String F_PERMIT_TUNNEL =
      "security - policies - from-zone - to-zone - policy - then - permit - tunnel";

  private static final String F_POLICY_TERM_THEN_NEXT_HOP =
      "policy-statement - term - then - next-hop";

  private static final String GLOBAL_ADDRESS_BOOK_NAME = "global";

  /** Mark the specified structure as defined on each line in the supplied context */
  private void defineStructure(StructureType type, String name, RuleContext ctx) {
    /* Recursively process children to find all relevant definition lines for the specified context */
    for (int i = 0; i < ctx.getChildCount(); i++) {
      ParseTree child = ctx.getChild(i);
      if (child instanceof TerminalNode) {
        _configuration.defineStructure(type, name, getLine(((TerminalNode) child).getSymbol()));
      } else if (child instanceof RuleContext) {
        defineStructure(type, name, (RuleContext) child);
      }
    }
  }

  /** Return original line number for the specified token */
  private int getLine(Token t) {
    return _parser.getLine(t);
  }

  public static NamedPort getNamedPort(PortContext ctx) {
    if (ctx.AFS() != null) {
      return NamedPort.AFS;
    } else if (ctx.BGP() != null) {
      return NamedPort.BGP;
    } else if (ctx.BIFF() != null) {
      return NamedPort.BIFFudp_OR_EXECtcp;
    } else if (ctx.BOOTPC() != null) {
      return NamedPort.BOOTPC;
    } else if (ctx.BOOTPS() != null) {
      return NamedPort.BOOTPS_OR_DHCP;
    } else if (ctx.CMD() != null) {
      return NamedPort.CMDtcp_OR_SYSLOGudp;
    } else if (ctx.CVSPSERVER() != null) {
      return NamedPort.CVSPSERVER;
    } else if (ctx.DHCP() != null) {
      return NamedPort.BOOTPS_OR_DHCP;
    } else if (ctx.DOMAIN() != null) {
      return NamedPort.DOMAIN;
    } else if (ctx.EKLOGIN() != null) {
      return NamedPort.EKLOGIN;
    } else if (ctx.EKSHELL() != null) {
      return NamedPort.EKSHELL;
    } else if (ctx.EXEC() != null) {
      return NamedPort.BIFFudp_OR_EXECtcp;
    } else if (ctx.FINGER() != null) {
      return NamedPort.FINGER;
    } else if (ctx.FTP() != null) {
      return NamedPort.FTP;
    } else if (ctx.FTP_DATA() != null) {
      return NamedPort.FTP_DATA;
    } else if (ctx.HTTP() != null) {
      return NamedPort.HTTP;
    } else if (ctx.HTTPS() != null) {
      return NamedPort.HTTPS;
    } else if (ctx.IDENT() != null) {
      return NamedPort.IDENT;
    } else if (ctx.IMAP() != null) {
      return NamedPort.IMAP;
    } else if (ctx.KERBEROS_SEC() != null) {
      return NamedPort.KERBEROS_SEC;
    } else if (ctx.KLOGIN() != null) {
      return NamedPort.KLOGIN;
    } else if (ctx.KPASSWD() != null) {
      return NamedPort.KPASSWD;
    } else if (ctx.KRB_PROP() != null) {
      return NamedPort.KRB_PROP;
    } else if (ctx.KRBUPDATE() != null) {
      return NamedPort.KRBUPDATE;
    } else if (ctx.KSHELL() != null) {
      return NamedPort.KSHELL;
    } else if (ctx.LDAP() != null) {
      return NamedPort.LDAP;
    } else if (ctx.LDP() != null) {
      return NamedPort.LDP;
    } else if (ctx.LOGIN() != null) {
      return NamedPort.LOGINtcp_OR_WHOudp;
    } else if (ctx.MOBILEIP_AGENT() != null) {
      return NamedPort.MOBILE_IP_AGENT;
    } else if (ctx.MOBILIP_MN() != null) {
      return NamedPort.MOBILE_IP_MN;
    } else if (ctx.MSDP() != null) {
      return NamedPort.MSDP;
    } else if (ctx.NETBIOS_DGM() != null) {
      return NamedPort.NETBIOS_DGM;
    } else if (ctx.NETBIOS_NS() != null) {
      return NamedPort.NETBIOS_NS;
    } else if (ctx.NETBIOS_SSN() != null) {
      return NamedPort.NETBIOS_SSN;
    } else if (ctx.NFSD() != null) {
      return NamedPort.NFSD;
    } else if (ctx.NNTP() != null) {
      return NamedPort.NNTP;
    } else if (ctx.NTALK() != null) {
      return NamedPort.NTALK;
    } else if (ctx.NTP() != null) {
      return NamedPort.NTP;
    } else if (ctx.POP3() != null) {
      return NamedPort.POP3;
    } else if (ctx.PPTP() != null) {
      return NamedPort.PPTP;
    } else if (ctx.PRINTER() != null) {
      return NamedPort.LPD;
    } else if (ctx.RADACCT() != null) {
      return NamedPort.RADIUS_JUNIPER;
    } else if (ctx.RADIUS() != null) {
      return NamedPort.RADIUS_JUNIPER;
    } else if (ctx.RIP() != null) {
      return NamedPort.RIP;
    } else if (ctx.RKINIT() != null) {
      return NamedPort.RKINIT;
    } else if (ctx.SMTP() != null) {
      return NamedPort.SMTP;
    } else if (ctx.SNMP() != null) {
      return NamedPort.SNMP;
    } else if (ctx.SNMPTRAP() != null) {
      return NamedPort.SNMPTRAP;
    } else if (ctx.SNPP() != null) {
      return NamedPort.SNPP;
    } else if (ctx.SOCKS() != null) {
      return NamedPort.SOCKS;
    } else if (ctx.SSH() != null) {
      return NamedPort.SSH;
    } else if (ctx.SUNRPC() != null) {
      return NamedPort.SUNRPC;
    } else if (ctx.SYSLOG() != null) {
      return NamedPort.CMDtcp_OR_SYSLOGudp;
    } else if (ctx.TACACS() != null) {
      return NamedPort.TACACS;
    } else if (ctx.TACACS_DS() != null) {
      return NamedPort.TACACS_DS;
    } else if (ctx.TALK() != null) {
      return NamedPort.TALK;
    } else if (ctx.TELNET() != null) {
      return NamedPort.TELNET;
    } else if (ctx.TFTP() != null) {
      return NamedPort.TFTP;
    } else if (ctx.TIMED() != null) {
      return NamedPort.TIMED;
    } else if (ctx.WHO() != null) {
      return NamedPort.LOGINtcp_OR_WHOudp;
    } else if (ctx.XDMCP() != null) {
      return NamedPort.XDMCP;
    } else {
      throw new BatfishException("missing port-number mapping for port: \"" + ctx.getText() + "\"");
    }
  }

  public static int getPortNumber(PortContext ctx) {
    if (ctx.DEC() != null) {
      int port = toInt(ctx.DEC());
      return port;
    } else {
      NamedPort namedPort = getNamedPort(ctx);
      return namedPort.number();
    }
  }

  private static long toReferenceBandwidth(Reference_bandwidthContext ctx) {
    long base = toLong(ctx.base);
    if (ctx.K() != null) {
      return base * 1000L;
    } else if (ctx.M() != null) {
      return base * 1000000L;
    } else if (ctx.G() != null) {
      return base * 1000000000L;
    } else {
      return base;
    }
  }

  private IsisHelloAuthenticationType toIsisHelloAuthenticationType(
      Hello_authentication_typeContext ctx) {
    if (ctx.MD5() != null) {
      return IsisHelloAuthenticationType.MD5;
    } else if (ctx.SIMPLE() != null) {
      return IsisHelloAuthenticationType.SIMPLE;
    } else {
      throw convError(IsisHelloAuthenticationType.class, ctx);
    }
  }

  private JunosApplication toJunosApplication(Junos_applicationContext ctx) {
    if (ctx.ANY() != null) {
      return JunosApplication.ANY;
    } else if (ctx.JUNOS_AOL() != null) {
      return JunosApplication.JUNOS_AOL;
    } else if (ctx.JUNOS_BGP() != null) {
      return JunosApplication.JUNOS_BGP;
    } else if (ctx.JUNOS_BIFF() != null) {
      return JunosApplication.JUNOS_BIFF;
    } else if (ctx.JUNOS_BOOTPC() != null) {
      return JunosApplication.JUNOS_BOOTPC;
    } else if (ctx.JUNOS_BOOTPS() != null) {
      return JunosApplication.JUNOS_BOOTPS;
    } else if (ctx.JUNOS_CHARGEN() != null) {
      return JunosApplication.JUNOS_CHARGEN;
    } else if (ctx.JUNOS_CVSPSERVER() != null) {
      return JunosApplication.JUNOS_CVSPSERVER;
    } else if (ctx.JUNOS_DHCP_CLIENT() != null) {
      return JunosApplication.JUNOS_DHCP_CLIENT;
    } else if (ctx.JUNOS_DHCP_RELAY() != null) {
      return JunosApplication.JUNOS_DHCP_RELAY;
    } else if (ctx.JUNOS_DHCP_SERVER() != null) {
      return JunosApplication.JUNOS_DHCP_SERVER;
    } else if (ctx.JUNOS_DISCARD() != null) {
      return JunosApplication.JUNOS_DISCARD;
    } else if (ctx.JUNOS_DNS_TCP() != null) {
      return JunosApplication.JUNOS_DNS_TCP;
    } else if (ctx.JUNOS_DNS_UDP() != null) {
      return JunosApplication.JUNOS_DNS_UDP;
    } else if (ctx.JUNOS_ECHO() != null) {
      return JunosApplication.JUNOS_ECHO;
    } else if (ctx.JUNOS_FINGER() != null) {
      return JunosApplication.JUNOS_FINGER;
    } else if (ctx.JUNOS_FTP() != null) {
      return JunosApplication.JUNOS_FTP;
    } else if (ctx.JUNOS_FTP_DATA() != null) {
      return JunosApplication.JUNOS_FTP_DATA;
    } else if (ctx.JUNOS_GNUTELLA() != null) {
      return JunosApplication.JUNOS_GNUTELLA;
    } else if (ctx.JUNOS_GOPHER() != null) {
      return JunosApplication.JUNOS_GOPHER;
    } else if (ctx.JUNOS_GPRS_GTP_C() != null) {
      return JunosApplication.JUNOS_GPRS_GTP_C;
    } else if (ctx.JUNOS_GPRS_GTP_U() != null) {
      return JunosApplication.JUNOS_GPRS_GTP_U;
    } else if (ctx.JUNOS_GPRS_GTP_V0() != null) {
      return JunosApplication.JUNOS_GPRS_GTP_V0;
    } else if (ctx.JUNOS_GRE() != null) {
      return JunosApplication.JUNOS_GRE;
    } else if (ctx.JUNOS_GTP() != null) {
      return JunosApplication.JUNOS_GTP;
    } else if (ctx.JUNOS_H323() != null) {
      return JunosApplication.JUNOS_H323;
    } else if (ctx.JUNOS_HTTP() != null) {
      return JunosApplication.JUNOS_HTTP;
    } else if (ctx.JUNOS_HTTP_EXT() != null) {
      return JunosApplication.JUNOS_HTTP_EXT;
    } else if (ctx.JUNOS_HTTPS() != null) {
      return JunosApplication.JUNOS_HTTPS;
    } else if (ctx.JUNOS_ICMP_ALL() != null) {
      return JunosApplication.JUNOS_ICMP_ALL;
    } else if (ctx.JUNOS_ICMP_PING() != null) {
      return JunosApplication.JUNOS_ICMP_PING;
    } else if (ctx.JUNOS_ICMP6_ALL() != null) {
      return JunosApplication.JUNOS_ICMP6_ALL;
    } else if (ctx.JUNOS_ICMP6_DST_UNREACH_ADDR() != null) {
      return JunosApplication.JUNOS_ICMP6_DST_UNREACH_ADDR;
    } else if (ctx.JUNOS_ICMP6_DST_UNREACH_ADMIN() != null) {
      return JunosApplication.JUNOS_ICMP6_DST_UNREACH_ADMIN;
    } else if (ctx.JUNOS_ICMP6_DST_UNREACH_BEYOND() != null) {
      return JunosApplication.JUNOS_ICMP6_DST_UNREACH_BEYOND;
    } else if (ctx.JUNOS_ICMP6_DST_UNREACH_PORT() != null) {
      return JunosApplication.JUNOS_ICMP6_DST_UNREACH_PORT;
    } else if (ctx.JUNOS_ICMP6_DST_UNREACH_ROUTE() != null) {
      return JunosApplication.JUNOS_ICMP6_DST_UNREACH_ROUTE;
    } else if (ctx.JUNOS_ICMP6_ECHO_REPLY() != null) {
      return JunosApplication.JUNOS_ICMP6_ECHO_REPLY;
    } else if (ctx.JUNOS_ICMP6_ECHO_REQUEST() != null) {
      return JunosApplication.JUNOS_ICMP6_ECHO_REQUEST;
    } else if (ctx.JUNOS_ICMP6_PACKET_TOO_BIG() != null) {
      return JunosApplication.JUNOS_ICMP6_PACKET_TOO_BIG;
    } else if (ctx.JUNOS_ICMP6_PARAM_PROB_HEADER() != null) {
      return JunosApplication.JUNOS_ICMP6_PARAM_PROB_HEADER;
    } else if (ctx.JUNOS_ICMP6_PARAM_PROB_NEXTHDR() != null) {
      return JunosApplication.JUNOS_ICMP6_PARAM_PROB_NEXTHDR;
    } else if (ctx.JUNOS_ICMP6_PARAM_PROB_OPTION() != null) {
      return JunosApplication.JUNOS_ICMP6_PARAM_PROB_OPTION;
    } else if (ctx.JUNOS_ICMP6_TIME_EXCEED_REASSEMBLY() != null) {
      return JunosApplication.JUNOS_ICMP6_TIME_EXCEED_REASSEMBLY;
    } else if (ctx.JUNOS_ICMP6_TIME_EXCEED_TRANSIT() != null) {
      return JunosApplication.JUNOS_ICMP6_TIME_EXCEED_TRANSIT;
    } else if (ctx.JUNOS_IDENT() != null) {
      return JunosApplication.JUNOS_IDENT;
    } else if (ctx.JUNOS_IKE() != null) {
      return JunosApplication.JUNOS_IKE;
    } else if (ctx.JUNOS_IKE_NAT() != null) {
      return JunosApplication.JUNOS_IKE_NAT;
    } else if (ctx.JUNOS_IMAP() != null) {
      return JunosApplication.JUNOS_IMAP;
    } else if (ctx.JUNOS_IMAPS() != null) {
      return JunosApplication.JUNOS_IMAPS;
    } else if (ctx.JUNOS_INTERNET_LOCATOR_SERVICE() != null) {
      return JunosApplication.JUNOS_INTERNET_LOCATOR_SERVICE;
    } else if (ctx.JUNOS_IRC() != null) {
      return JunosApplication.JUNOS_IRC;
    } else if (ctx.JUNOS_L2TP() != null) {
      return JunosApplication.JUNOS_L2TP;
    } else if (ctx.JUNOS_LDAP() != null) {
      return JunosApplication.JUNOS_LDAP;
    } else if (ctx.JUNOS_LDP_TCP() != null) {
      return JunosApplication.JUNOS_LDP_TCP;
    } else if (ctx.JUNOS_LDP_UDP() != null) {
      return JunosApplication.JUNOS_LDP_UDP;
    } else if (ctx.JUNOS_LPR() != null) {
      return JunosApplication.JUNOS_LPR;
    } else if (ctx.JUNOS_MAIL() != null) {
      return JunosApplication.JUNOS_MAIL;
    } else if (ctx.JUNOS_MGCP_CA() != null) {
      return JunosApplication.JUNOS_MGCP_CA;
    } else if (ctx.JUNOS_MGCP_UA() != null) {
      return JunosApplication.JUNOS_MGCP_UA;
    } else if (ctx.JUNOS_MS_RPC_EPM() != null) {
      return JunosApplication.JUNOS_MS_RPC_EPM;
    } else if (ctx.JUNOS_MS_RPC_IIS_COM_1() != null) {
      return JunosApplication.JUNOS_MS_RPC_IIS_COM_1;
    } else if (ctx.JUNOS_MS_RPC_IIS_COM_ADMINBASE() != null) {
      return JunosApplication.JUNOS_MS_RPC_IIS_COM_ADMINBASE;
    } else if (ctx.JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_NSP() != null) {
      return JunosApplication.JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_NSP;
    } else if (ctx.JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_RFR() != null) {
      return JunosApplication.JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_RFR;
    } else if (ctx.JUNOS_MS_RPC_MSEXCHANGE_INFO_STORE() != null) {
      return JunosApplication.JUNOS_MS_RPC_MSEXCHANGE_INFO_STORE;
    } else if (ctx.JUNOS_MS_RPC_TCP() != null) {
      return JunosApplication.JUNOS_MS_RPC_TCP;
    } else if (ctx.JUNOS_MS_RPC_UDP() != null) {
      return JunosApplication.JUNOS_MS_RPC_UDP;
    } else if (ctx.JUNOS_MS_RPC_UUID_ANY_TCP() != null) {
      return JunosApplication.JUNOS_MS_RPC_UUID_ANY_TCP;
    } else if (ctx.JUNOS_MS_RPC_UUID_ANY_UDP() != null) {
      return JunosApplication.JUNOS_MS_RPC_UUID_ANY_UDP;
    } else if (ctx.JUNOS_MS_RPC_WMIC_ADMIN() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_ADMIN;
    } else if (ctx.JUNOS_MS_RPC_WMIC_ADMIN2() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_ADMIN2;
    } else if (ctx.JUNOS_MS_RPC_WMIC_MGMT() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_MGMT;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_CALLRESULT() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_CALLRESULT;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_CLASSOBJECT() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_CLASSOBJECT;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_LEVEL1LOGIN() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_LEVEL1LOGIN;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_LOGIN_CLIENTID() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_LOGIN_CLIENTID;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_LOGIN_HELPER() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_LOGIN_HELPER;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_OBJECTSINK() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_OBJECTSINK;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_REFRESHING_SERVICES() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_REFRESHING_SERVICES;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_REMOTE_REFRESHER() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_REMOTE_REFRESHER;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_SERVICES() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_SERVICES;
    } else if (ctx.JUNOS_MS_RPC_WMIC_WEBM_SHUTDOWN() != null) {
      return JunosApplication.JUNOS_MS_RPC_WMIC_WEBM_SHUTDOWN;
    } else if (ctx.JUNOS_MS_SQL() != null) {
      return JunosApplication.JUNOS_MS_SQL;
    } else if (ctx.JUNOS_MSN() != null) {
      return JunosApplication.JUNOS_MSN;
    } else if (ctx.JUNOS_NBDS() != null) {
      return JunosApplication.JUNOS_NBDS;
    } else if (ctx.JUNOS_NBNAME() != null) {
      return JunosApplication.JUNOS_NBNAME;
    } else if (ctx.JUNOS_NETBIOS_SESSION() != null) {
      return JunosApplication.JUNOS_NETBIOS_SESSION;
    } else if (ctx.JUNOS_NFS() != null) {
      return JunosApplication.JUNOS_NFS;
    } else if (ctx.JUNOS_NFSD_TCP() != null) {
      return JunosApplication.JUNOS_NFSD_TCP;
    } else if (ctx.JUNOS_NFSD_UDP() != null) {
      return JunosApplication.JUNOS_NFSD_UDP;
    } else if (ctx.JUNOS_NNTP() != null) {
      return JunosApplication.JUNOS_NNTP;
    } else if (ctx.JUNOS_NS_GLOBAL() != null) {
      return JunosApplication.JUNOS_NS_GLOBAL;
    } else if (ctx.JUNOS_NS_GLOBAL_PRO() != null) {
      return JunosApplication.JUNOS_NS_GLOBAL_PRO;
    } else if (ctx.JUNOS_NSM() != null) {
      return JunosApplication.JUNOS_NSM;
    } else if (ctx.JUNOS_NTALK() != null) {
      return JunosApplication.JUNOS_NTALK;
    } else if (ctx.JUNOS_NTP() != null) {
      return JunosApplication.JUNOS_NTP;
    } else if (ctx.JUNOS_OSPF() != null) {
      return JunosApplication.JUNOS_OSPF;
    } else if (ctx.JUNOS_PC_ANYWHERE() != null) {
      return JunosApplication.JUNOS_PC_ANYWHERE;
    } else if (ctx.JUNOS_PERSISTENT_NAT() != null) {
      return JunosApplication.JUNOS_PERSISTENT_NAT;
    } else if (ctx.JUNOS_PING() != null) {
      return JunosApplication.JUNOS_PING;
    } else if (ctx.JUNOS_PINGV6() != null) {
      return JunosApplication.JUNOS_PINGV6;
    } else if (ctx.JUNOS_POP3() != null) {
      return JunosApplication.JUNOS_POP3;
    } else if (ctx.JUNOS_PPTP() != null) {
      return JunosApplication.JUNOS_PPTP;
    } else if (ctx.JUNOS_PRINTER() != null) {
      return JunosApplication.JUNOS_PRINTER;
    } else if (ctx.JUNOS_R2CP() != null) {
      return JunosApplication.JUNOS_R2CP;
    } else if (ctx.JUNOS_RADACCT() != null) {
      return JunosApplication.JUNOS_RADACCT;
    } else if (ctx.JUNOS_RADIUS() != null) {
      return JunosApplication.JUNOS_RADIUS;
    } else if (ctx.JUNOS_REALAUDIO() != null) {
      return JunosApplication.JUNOS_REALAUDIO;
    } else if (ctx.JUNOS_RIP() != null) {
      return JunosApplication.JUNOS_RIP;
    } else if (ctx.JUNOS_RSH() != null) {
      return JunosApplication.JUNOS_RSH;
    } else if (ctx.JUNOS_RTSP() != null) {
      return JunosApplication.JUNOS_RTSP;
    } else if (ctx.JUNOS_SCCP() != null) {
      return JunosApplication.JUNOS_SCCP;
    } else if (ctx.JUNOS_SCTP_ANY() != null) {
      return JunosApplication.JUNOS_SCTP_ANY;
    } else if (ctx.JUNOS_SIP() != null) {
      return JunosApplication.JUNOS_SIP;
    } else if (ctx.JUNOS_SMB() != null) {
      return JunosApplication.JUNOS_SMB;
    } else if (ctx.JUNOS_SMB_SESSION() != null) {
      return JunosApplication.JUNOS_SMB_SESSION;
    } else if (ctx.JUNOS_SMTP() != null) {
      return JunosApplication.JUNOS_SMTP;
    } else if (ctx.JUNOS_SNMP_AGENTX() != null) {
      return JunosApplication.JUNOS_SNMP_AGENTX;
    } else if (ctx.JUNOS_SNPP() != null) {
      return JunosApplication.JUNOS_SNPP;
    } else if (ctx.JUNOS_SQL_MONITOR() != null) {
      return JunosApplication.JUNOS_SQL_MONITOR;
    } else if (ctx.JUNOS_SQLNET_V1() != null) {
      return JunosApplication.JUNOS_SQLNET_V1;
    } else if (ctx.JUNOS_SQLNET_V2() != null) {
      return JunosApplication.JUNOS_SQLNET_V2;
    } else if (ctx.JUNOS_SSH() != null) {
      return JunosApplication.JUNOS_SSH;
    } else if (ctx.JUNOS_STUN() != null) {
      return JunosApplication.JUNOS_STUN;
    } else if (ctx.JUNOS_SUN_RPC_ANY_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_ANY_TCP;
    } else if (ctx.JUNOS_SUN_RPC_ANY_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_ANY_UDP;
    } else if (ctx.JUNOS_SUN_RPC_MOUNTD_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_MOUNTD_TCP;
    } else if (ctx.JUNOS_SUN_RPC_MOUNTD_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_MOUNTD_UDP;
    } else if (ctx.JUNOS_SUN_RPC_NFS_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_NFS_TCP;
    } else if (ctx.JUNOS_SUN_RPC_NFS_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_NFS_UDP;
    } else if (ctx.JUNOS_SUN_RPC_NLOCKMGR_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_NLOCKMGR_TCP;
    } else if (ctx.JUNOS_SUN_RPC_NLOCKMGR_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_NLOCKMGR_UDP;
    } else if (ctx.JUNOS_SUN_RPC_PORTMAP_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_PORTMAP_TCP;
    } else if (ctx.JUNOS_SUN_RPC_PORTMAP_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_PORTMAP_UDP;
    } else if (ctx.JUNOS_SUN_RPC_RQUOTAD_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_RQUOTAD_TCP;
    } else if (ctx.JUNOS_SUN_RPC_RQUOTAD_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_RQUOTAD_UDP;
    } else if (ctx.JUNOS_SUN_RPC_RUSERD_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_RUSERD_TCP;
    } else if (ctx.JUNOS_SUN_RPC_RUSERD_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_RUSERD_UDP;
    } else if (ctx.JUNOS_SUN_RPC_SADMIND_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_SADMIND_TCP;
    } else if (ctx.JUNOS_SUN_RPC_SADMIND_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_SADMIND_UDP;
    } else if (ctx.JUNOS_SUN_RPC_SPRAYD_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_SPRAYD_TCP;
    } else if (ctx.JUNOS_SUN_RPC_SPRAYD_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_SPRAYD_UDP;
    } else if (ctx.JUNOS_SUN_RPC_STATUS_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_STATUS_TCP;
    } else if (ctx.JUNOS_SUN_RPC_STATUS_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_STATUS_UDP;
    } else if (ctx.JUNOS_SUN_RPC_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_TCP;
    } else if (ctx.JUNOS_SUN_RPC_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_UDP;
    } else if (ctx.JUNOS_SUN_RPC_WALLD_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_WALLD_TCP;
    } else if (ctx.JUNOS_SUN_RPC_WALLD_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_WALLD_UDP;
    } else if (ctx.JUNOS_SUN_RPC_YPBIND_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_YPBIND_TCP;
    } else if (ctx.JUNOS_SUN_RPC_YPBIND_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_YPBIND_UDP;
    } else if (ctx.JUNOS_SUN_RPC_YPSERV_TCP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_YPSERV_TCP;
    } else if (ctx.JUNOS_SUN_RPC_YPSERV_UDP() != null) {
      return JunosApplication.JUNOS_SUN_RPC_YPSERV_UDP;
    } else if (ctx.JUNOS_SYSLOG() != null) {
      return JunosApplication.JUNOS_SYSLOG;
    } else if (ctx.JUNOS_TACACS() != null) {
      return JunosApplication.JUNOS_TACACS;
    } else if (ctx.JUNOS_TACACS_DS() != null) {
      return JunosApplication.JUNOS_TACACS_DS;
    } else if (ctx.JUNOS_TALK() != null) {
      return JunosApplication.JUNOS_TALK;
    } else if (ctx.JUNOS_TCP_ANY() != null) {
      return JunosApplication.JUNOS_TCP_ANY;
    } else if (ctx.JUNOS_TELNET() != null) {
      return JunosApplication.JUNOS_TELNET;
    } else if (ctx.JUNOS_TFTP() != null) {
      return JunosApplication.JUNOS_TFTP;
    } else if (ctx.JUNOS_UDP_ANY() != null) {
      return JunosApplication.JUNOS_UDP_ANY;
    } else if (ctx.JUNOS_UUCP() != null) {
      return JunosApplication.JUNOS_UUCP;
    } else if (ctx.JUNOS_VDO_LIVE() != null) {
      return JunosApplication.JUNOS_VDO_LIVE;
    } else if (ctx.JUNOS_VNC() != null) {
      return JunosApplication.JUNOS_VNC;
    } else if (ctx.JUNOS_WAIS() != null) {
      return JunosApplication.JUNOS_WAIS;
    } else if (ctx.JUNOS_WHO() != null) {
      return JunosApplication.JUNOS_WHO;
    } else if (ctx.JUNOS_WHOIS() != null) {
      return JunosApplication.JUNOS_WHOIS;
    } else if (ctx.JUNOS_WINFRAME() != null) {
      return JunosApplication.JUNOS_WINFRAME;
    } else if (ctx.JUNOS_WXCONTROL() != null) {
      return JunosApplication.JUNOS_WXCONTROL;
    } else if (ctx.JUNOS_X_WINDOWS() != null) {
      return JunosApplication.JUNOS_X_WINDOWS;
    } else if (ctx.JUNOS_XNM_CLEAR_TEXT() != null) {
      return JunosApplication.JUNOS_XNM_CLEAR_TEXT;
    } else if (ctx.JUNOS_XNM_SSL() != null) {
      return JunosApplication.JUNOS_XNM_SSL;
    } else if (ctx.JUNOS_YMSG() != null) {
      return JunosApplication.JUNOS_YMSG;
    } else {
      throw convError(JunosApplication.class, ctx);
    }
  }

  private JunosApplicationSet toJunosApplicationSet(Junos_application_setContext ctx) {
    if (ctx.JUNOS_CIFS() != null) {
      return JunosApplicationSet.JUNOS_CIFS;
    } else if (ctx.JUNOS_MGCP() != null) {
      return JunosApplicationSet.JUNOS_MGCP;
    } else if (ctx.JUNOS_MS_RPC() != null) {
      return JunosApplicationSet.JUNOS_MS_RPC;
    } else if (ctx.JUNOS_MS_RPC_ANY() != null) {
      return JunosApplicationSet.JUNOS_MS_RPC_ANY;
    } else if (ctx.JUNOS_MS_RPC_IIS_COM() != null) {
      return JunosApplicationSet.JUNOS_MS_RPC_IIS_COM;
    } else if (ctx.JUNOS_MS_RPC_MSEXCHANGE() != null) {
      return JunosApplicationSet.JUNOS_MS_RPC_MSEXCHANGE;
    } else if (ctx.JUNOS_MS_RPC_WMIC() != null) {
      return JunosApplicationSet.JUNOS_MS_RPC_WMIC;
    } else if (ctx.JUNOS_ROUTING_INBOUND() != null) {
      return JunosApplicationSet.JUNOS_ROUTING_INBOUND;
    } else if (ctx.JUNOS_SUN_RPC() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC;
    } else if (ctx.JUNOS_SUN_RPC_ANY() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_ANY;
    } else if (ctx.JUNOS_SUN_RPC_MOUNTD() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_MOUNTD;
    } else if (ctx.JUNOS_SUN_RPC_NFS() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_NFS;
    } else if (ctx.JUNOS_SUN_RPC_NFS_ACCESS() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_NFS_ACCESS;
    } else if (ctx.JUNOS_SUN_RPC_NLOCKMGR() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_NLOCKMGR;
    } else if (ctx.JUNOS_SUN_RPC_PORTMAP() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_PORTMAP;
    } else if (ctx.JUNOS_SUN_RPC_RQUOTAD() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_RQUOTAD;
    } else if (ctx.JUNOS_SUN_RPC_RUSERD() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_RUSERD;
    } else if (ctx.JUNOS_SUN_RPC_SADMIND() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_SADMIND;
    } else if (ctx.JUNOS_SUN_RPC_SPRAYD() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_SPRAYD;
    } else if (ctx.JUNOS_SUN_RPC_STATUS() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_STATUS;
    } else if (ctx.JUNOS_SUN_RPC_WALLD() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_WALLD;
    } else if (ctx.JUNOS_SUN_RPC_YPBIND() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_YPBIND;
    } else if (ctx.JUNOS_SUN_RPC_YPSERV() != null) {
      return JunosApplicationSet.JUNOS_SUN_RPC_YPSERV;
    } else {
      throw convError(JunosApplicationSet.class, ctx);
    }
  }

  private static long toCommunityLong(Sc_literalContext ctx) {
    String text = ctx.COMMUNITY_LITERAL().getText();
    return CommonUtil.communityStringToLong(text);
  }

  private static long toCommunityLong(Sc_namedContext ctx) {
    if (ctx.NO_ADVERTISE() != null) {
      return 0xFFFFFF02L;
    }
    if (ctx.NO_EXPORT() != null) {
      return 0xFFFFFF01L;
    } else {
      throw new BatfishException(
          "missing named-community-to-long mapping for: \"" + ctx.getText() + "\"");
    }
  }

  private static long toCommunityLong(Standard_communityContext ctx) {
    if (ctx.sc_literal() != null) {
      return toCommunityLong(ctx.sc_literal());
    } else if (ctx.sc_named() != null) {
      return toCommunityLong(ctx.sc_named());
    } else {
      throw new BatfishException("Cannot convert to community long");
    }
  }

  private static EncryptionAlgorithm toEncryptionAlgorithm(Encryption_algorithmContext ctx) {
    if (ctx.THREEDES_CBC() != null) {
      return EncryptionAlgorithm.THREEDES_CBC;
    } else if (ctx.AES_128_CBC() != null) {
      return EncryptionAlgorithm.AES_128_CBC;
    } else if (ctx.AES_128_GCM() != null) {
      return EncryptionAlgorithm.AES_128_GCM;
    } else if (ctx.AES_192_CBC() != null) {
      return EncryptionAlgorithm.AES_192_CBC;
    } else if (ctx.AES_192_GCM() != null) {
      return EncryptionAlgorithm.AES_192_GCM;
    } else if (ctx.AES_256_CBC() != null) {
      return EncryptionAlgorithm.AES_256_CBC;
    } else if (ctx.AES_256_GCM() != null) {
      return EncryptionAlgorithm.AES_256_GCM;
    } else if (ctx.DES_CBC() != null) {
      return EncryptionAlgorithm.DES_CBC;
    } else {
      throw new BatfishException("Invalid encryption algorithm: " + ctx.getText());
    }
  }

  private static HostProtocol toHostProtocol(Hib_protocolContext ctx) {
    if (ctx.ALL() != null) {
      return HostProtocol.ALL;
    } else if (ctx.BFD() != null) {
      return HostProtocol.BFD;
    } else if (ctx.BGP() != null) {
      return HostProtocol.BGP;
    } else if (ctx.DVMRP() != null) {
      return HostProtocol.DVMRP;
    } else if (ctx.IGMP() != null) {
      return HostProtocol.IGMP;
    } else if (ctx.LDP() != null) {
      return HostProtocol.LDP;
    } else if (ctx.MSDP() != null) {
      return HostProtocol.MSDP;
    } else if (ctx.NHRP() != null) {
      return HostProtocol.NHRP;
    } else if (ctx.OSPF() != null) {
      return HostProtocol.OSPF;
    } else if (ctx.OSPF3() != null) {
      return HostProtocol.OSPF3;
    } else if (ctx.PGM() != null) {
      return HostProtocol.PGM;
    } else if (ctx.PIM() != null) {
      return HostProtocol.PIM;
    } else if (ctx.RIP() != null) {
      return HostProtocol.RIP;
    } else if (ctx.RIPNG() != null) {
      return HostProtocol.RIPNG;
    } else if (ctx.ROUTER_DISCOVERY() != null) {
      return HostProtocol.ROUTER_DISCOVERY;
    } else if (ctx.RSVP() != null) {
      return HostProtocol.RSVP;
    } else if (ctx.SAP() != null) {
      return HostProtocol.SAP;
    } else if (ctx.VRRP() != null) {
      return HostProtocol.VRRP;
    } else {
      throw new BatfishException("Invalid host protocol");
    }
  }

  private static HostSystemService toHostSystemService(Hib_system_serviceContext ctx) {
    if (ctx.ALL() != null) {
      return HostSystemService.ALL;
    } else if (ctx.ANY_SERVICE() != null) {
      return HostSystemService.ANY_SERVICE;
    } else if (ctx.DHCP() != null) {
      return HostSystemService.DHCP;
    } else if (ctx.DNS() != null) {
      return HostSystemService.DNS;
    } else if (ctx.FINGER() != null) {
      return HostSystemService.FINGER;
    } else if (ctx.FTP() != null) {
      return HostSystemService.FTP;
    } else if (ctx.HTTP() != null) {
      return HostSystemService.HTTP;
    } else if (ctx.HTTPS() != null) {
      return HostSystemService.HTTPS;
    } else if (ctx.IDENT_RESET() != null) {
      return HostSystemService.IDENT_RESET;
    } else if (ctx.IKE() != null) {
      return HostSystemService.IKE;
    } else if (ctx.LSPING() != null) {
      return HostSystemService.LSPING;
    } else if (ctx.NETCONF() != null) {
      return HostSystemService.NETCONF;
    } else if (ctx.NTP() != null) {
      return HostSystemService.NTP;
    } else if (ctx.PING() != null) {
      return HostSystemService.PING;
    } else if (ctx.R2CP() != null) {
      return HostSystemService.R2CP;
    } else if (ctx.REVERSE_SSH() != null) {
      return HostSystemService.REVERSE_SSH;
    } else if (ctx.REVERSE_TELNET() != null) {
      return HostSystemService.REVERSE_TELNET;
    } else if (ctx.RLOGIN() != null) {
      return HostSystemService.RLOGIN;
    } else if (ctx.RPM() != null) {
      return HostSystemService.RPM;
    } else if (ctx.RSH() != null) {
      return HostSystemService.RSH;
    } else if (ctx.SIP() != null) {
      return HostSystemService.SIP;
    } else if (ctx.SNMP() != null) {
      return HostSystemService.SNMP;
    } else if (ctx.SNMP_TRAP() != null) {
      return HostSystemService.SNMP_TRAP;
    } else if (ctx.SSH() != null) {
      return HostSystemService.SSH;
    } else if (ctx.TELNET() != null) {
      return HostSystemService.TELNET;
    } else if (ctx.TFTP() != null) {
      return HostSystemService.TFTP;
    } else if (ctx.TRACEROUTE() != null) {
      return HostSystemService.TRACEROUTE;
    } else if (ctx.XNM_CLEAR_TEXT() != null) {
      return HostSystemService.XNM_CLEAR_TEXT;
    } else if (ctx.XNM_SSL() != null) {
      return HostSystemService.XNM_SSL;
    } else {
      throw new BatfishException("Invalid host system service");
    }
  }

  @Nullable
  private static Integer toIcmpCode(Icmp_codeContext ctx, Warnings w) {
    if (ctx.FRAGMENTATION_NEEDED() != null) {
      return IcmpCode.PACKET_TOO_BIG;
    } else if (ctx.HOST_UNREACHABLE() != null) {
      return IcmpCode.DESTINATION_HOST_UNREACHABLE;
    } else if (ctx.TTL_EQ_ZERO_DURING_TRANSIT() != null) {
      return IcmpCode.TTL_EQ_ZERO_DURING_TRANSIT;
    } else {
      w.redFlag(String.format("Missing mapping for icmp-code: '%s'", ctx.getText()));
      return null;
    }
  }

  private static int toIcmpType(Icmp_typeContext ctx) {
    if (ctx.ECHO_REPLY() != null) {
      return IcmpType.ECHO_REPLY;
    } else if (ctx.ECHO_REQUEST() != null) {
      return IcmpType.ECHO_REQUEST;
    } else if (ctx.PARAMETER_PROBLEM() != null) {
      return IcmpType.PARAMETER_PROBLEM;
    } else if (ctx.SOURCE_QUENCH() != null) {
      return IcmpType.SOURCE_QUENCH;
    } else if (ctx.TIME_EXCEEDED() != null) {
      return IcmpType.TIME_EXCEEDED;
    } else if (ctx.UNREACHABLE() != null) {
      return IcmpType.DESTINATION_UNREACHABLE;
    } else {
      throw new BatfishException("Missing mapping for icmp-type: '" + ctx.getText() + "'");
    }
  }

  private static IkeHashingAlgorithm toIkeHashingAlgorithm(
      Ike_authentication_algorithmContext ctx) {
    if (ctx.MD5() != null) {
      return IkeHashingAlgorithm.MD5;
    } else if (ctx.SHA1() != null) {
      return IkeHashingAlgorithm.SHA1;
    } else if (ctx.SHA_256() != null) {
      return IkeHashingAlgorithm.SHA_256;
    } else if (ctx.SHA_384() != null) {
      return IkeHashingAlgorithm.SHA_384;
    } else {
      throw new BatfishException("invalid ike authentication algorithm: " + ctx.getText());
    }
  }

  private static IkeAuthenticationMethod toIkeAuthenticationMethod(
      Ike_authentication_methodContext ctx) {
    if (ctx.DSA_SIGNATURES() != null) {
      return IkeAuthenticationMethod.DSA_SIGNATURES;
    } else if (ctx.PRE_SHARED_KEYS() != null) {
      return IkeAuthenticationMethod.PRE_SHARED_KEYS;
    } else if (ctx.RSA_SIGNATURES() != null) {
      return IkeAuthenticationMethod.RSA_SIGNATURES;
    } else {
      throw new BatfishException("Invalid ike authentication method: " + ctx.getText());
    }
  }

  private static int toInt(TerminalNode node) {
    return toInt(node.getSymbol());
  }

  private static int toInt(Token token) {
    return Integer.parseInt(token.getText());
  }

  private static long toLong(TerminalNode node) {
    return toLong(node.getSymbol());
  }

  private static long toLong(Token token) {
    return Long.parseLong(token.getText());
  }

  private static IpProtocol toIpProtocol(Ip_protocolContext ctx) {
    if (ctx.DEC() != null) {
      int protocolNum = toInt(ctx.DEC());
      return IpProtocol.fromNumber(protocolNum);
    } else if (ctx.AH() != null) {
      return IpProtocol.AHP;
    } else if (ctx.DSTOPTS() != null) {
      return IpProtocol.IPV6_OPTS;
    } else if (ctx.EGP() != null) {
      return IpProtocol.EGP;
    } else if (ctx.ESP() != null) {
      return IpProtocol.ESP;
    } else if (ctx.FRAGMENT() != null) {
      return IpProtocol.IPV6_FRAG;
    } else if (ctx.GRE() != null) {
      return IpProtocol.GRE;
    } else if (ctx.HOP_BY_HOP() != null) {
      return IpProtocol.HOPOPT;
    } else if (ctx.ICMP() != null) {
      return IpProtocol.ICMP;
    } else if (ctx.ICMP6() != null) {
      return IpProtocol.IPV6_ICMP;
    } else if (ctx.ICMPV6() != null) {
      return IpProtocol.IPV6_ICMP;
    } else if (ctx.IGMP() != null) {
      return IpProtocol.IGMP;
    } else if (ctx.IPIP() != null) {
      return IpProtocol.IPINIP;
    } else if (ctx.IPV6() != null) {
      return IpProtocol.IPV6;
    } else if (ctx.OSPF() != null) {
      return IpProtocol.OSPF;
    } else if (ctx.PIM() != null) {
      return IpProtocol.PIM;
    } else if (ctx.RSVP() != null) {
      return IpProtocol.RSVP;
    } else if (ctx.SCTP() != null) {
      return IpProtocol.SCTP;
    } else if (ctx.TCP() != null) {
      return IpProtocol.TCP;
    } else if (ctx.UDP() != null) {
      return IpProtocol.UDP;
    } else if (ctx.VRRP() != null) {
      return IpProtocol.VRRP;
    } else {
      throw new BatfishException(
          "missing protocol-enum mapping for protocol: \"" + ctx.getText() + "\"");
    }
  }

  private static IpsecAuthenticationAlgorithm toIpsecAuthenticationAlgorithm(
      Ipsec_authentication_algorithmContext ctx) {
    if (ctx.HMAC_MD5_96() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_MD5_96;
    } else if (ctx.HMAC_SHA1_96() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA1_96;
    } else {
      throw new BatfishException("invalid ipsec authentication algorithm: " + ctx.getText());
    }
  }

  private static SortedSet<IpsecProtocol> toIpsecProtocol(Ipsec_protocolContext ctx) {
    if (ctx.AH() != null) {
      return ImmutableSortedSet.of(IpsecProtocol.AH);
    } else if (ctx.ESP() != null) {
      return ImmutableSortedSet.of(IpsecProtocol.ESP);
    } else if (ctx.BUNDLE() != null) {
      return ImmutableSortedSet.of(IpsecProtocol.AH, IpsecProtocol.ESP);
    } else {
      throw new BatfishException("invalid ipsec protocol: " + ctx.getText());
    }
  }

  private static List<SubRange> toRange(RangeContext ctx) {
    List<SubRange> range = new ArrayList<>();
    for (SubrangeContext sc : ctx.range_list) {
      SubRange sr = toSubRange(sc);
      range.add(sr);
    }
    return range;
  }

  private static RoutingProtocol toRoutingProtocol(Routing_protocolContext ctx) {
    if (ctx.AGGREGATE() != null) {
      return RoutingProtocol.AGGREGATE;
    } else if (ctx.BGP() != null) {
      return RoutingProtocol.BGP;
    } else if (ctx.DIRECT() != null) {
      return RoutingProtocol.CONNECTED;
    } else if (ctx.ISIS() != null) {
      return RoutingProtocol.ISIS;
    } else if (ctx.LDP() != null) {
      return RoutingProtocol.LDP;
    } else if (ctx.LOCAL() != null) {
      return RoutingProtocol.LOCAL;
    } else if (ctx.OSPF() != null) {
      return RoutingProtocol.OSPF;
    } else if (ctx.OSPF3() != null) {
      return RoutingProtocol.OSPF3;
    } else if (ctx.RSVP() != null) {
      return RoutingProtocol.RSVP;
    } else if (ctx.STATIC() != null) {
      return RoutingProtocol.STATIC;
    } else {
      throw new BatfishException("missing routing protocol-enum mapping");
    }
  }

  private static SubRange toSubRange(SubrangeContext ctx) {
    int low = toInt(ctx.low);
    int high = (ctx.high != null) ? toInt(ctx.high) : low;
    return new SubRange(low, high);
  }

  private static TcpFlags toTcpFlags(Tcp_flags_alternativeContext ctx) {
    TcpFlags tcpFlags = new TcpFlags();
    for (Tcp_flags_literalContext literalCtx : ctx.literals) {
      boolean value = literalCtx.BANG() == null;
      Tcp_flags_atomContext atom = literalCtx.tcp_flags_atom();
      if (atom.ACK() != null) {
        tcpFlags.setUseAck(true);
        tcpFlags.setAck(value);
      } else if (atom.CWR() != null) {
        tcpFlags.setUseCwr(true);
        tcpFlags.setCwr(value);
      } else if (atom.ECE() != null) {
        tcpFlags.setUseEce(true);
        tcpFlags.setEce(value);
      } else if (atom.FIN() != null) {
        tcpFlags.setUseFin(true);
        tcpFlags.setFin(value);
      } else if (atom.PSH() != null) {
        tcpFlags.setUsePsh(true);
        tcpFlags.setPsh(value);
      } else if (atom.RST() != null) {
        tcpFlags.setUseRst(true);
        tcpFlags.setRst(value);
      } else if (atom.SYN() != null) {
        tcpFlags.setUseSyn(true);
        tcpFlags.setSyn(value);
      } else if (atom.URG() != null) {
        tcpFlags.setUseUrg(true);
        tcpFlags.setUrg(value);
      } else {
        throw new BatfishException("Invalid tcp-flags atom: " + atom.getText());
      }
    }
    return tcpFlags;
  }

  private static List<TcpFlags> toTcpFlags(Tcp_flagsContext ctx) {
    List<TcpFlags> tcpFlagsList = new ArrayList<>();
    for (Tcp_flags_alternativeContext alternativeCtx : ctx.alternatives) {
      TcpFlags tcpFlags = toTcpFlags(alternativeCtx);
      tcpFlagsList.add(tcpFlags);
    }
    return tcpFlagsList;
  }

  static String unquote(String text) {
    if (text.length() == 0) {
      return text;
    }
    if (text.charAt(0) != '"') {
      return text;
    } else if (text.charAt(text.length() - 1) != '"') {
      throw new BatfishException("Improperly-quoted string");
    } else {
      return text.substring(1, text.length() - 1);
    }
  }

  private JuniperConfiguration _configuration;

  private int _conjunctionPolicyIndex;

  private AddressBook _currentAddressBook;

  private AddressSetAddressBookEntry _currentAddressSetAddressBookEntry;

  private AggregateRoute _currentAggregateRoute;

  private BaseApplication _currentApplication;

  private Term _currentApplicationTerm;

  private OspfArea _currentArea;

  @Nullable private Long _currentAreaRangeMetric;

  private Prefix _currentAreaRangePrefix;

  private boolean _currentAreaRangeRestrict;

  private JuniperAuthenticationKey _currentAuthenticationKey;

  private JuniperAuthenticationKeyChain _currentAuthenticationKeyChain;

  private BgpGroup _currentBgpGroup;

  private CommunityList _currentCommunityList;

  private DhcpRelayGroup _currentDhcpRelayGroup;

  private FirewallFilter _currentFilter;

  private Family _currentFirewallFamily;

  private Zone _currentFromZone;

  private FwTerm _currentFwTerm;

  private GeneratedRoute _currentGeneratedRoute;

  private IkeGateway _currentIkeGateway;

  private IkePolicy _currentIkePolicy;

  private IkeProposal _currentIkeProposal;

  private Interface _currentInterface;

  private InterfaceAddress _currentInterfaceAddress;

  private IpsecPolicy _currentIpsecPolicy;

  private IpsecProposal _currentIpsecProposal;

  private IpsecVpn _currentIpsecVpn;

  private Interface _currentIsisInterface;

  private IsisInterfaceLevelSettings _currentIsisInterfaceLevelSettings;

  private IsisLevelSettings _currentIsisLevelSettings;

  private Interface _currentMasterInterface;

  private Interface _currentOspfInterface;

  private PolicyStatement _currentPolicyStatement;

  private PrefixList _currentPrefixList;

  private PsTerm _currentPsTerm;

  private Set<PsThen> _currentPsThens;

  private RoutingInformationBase _currentRib;

  private Route6FilterLine _currentRoute6FilterLine;

  private Prefix6 _currentRoute6FilterPrefix;

  private RouteFilter _currentRouteFilter;

  private Route4FilterLine _currentRouteFilterLine;

  private Prefix _currentRouteFilterPrefix;

  private RoutingInstance _currentRoutingInstance;

  private SnmpCommunity _currentSnmpCommunity;

  private SnmpServer _currentSnmpServer;

  private StaticRoute _currentStaticRoute;

  private TacplusServer _currentTacplusServer;

  private Zone _currentToZone;

  private VrrpGroup _currentVrrpGroup;

  private String _currentVlanName;

  private Zone _currentZone;

  private FirewallFilter _currentZoneInboundFilter;

  private Interface _currentZoneInterface;

  private LineAction _defaultCrossZoneAction;

  private int _disjunctionPolicyIndex;

  private AddressBook _globalAddressBook;

  private boolean _hasZones;

  private FlatJuniperCombinedParser _parser;

  private final Map<PsTerm, RouteFilter> _termRouteFilters;

  private final String _text;

  private final Set<String> _unimplementedFeatures;

  private final boolean _unrecognizedAsRedFlag;

  private final Warnings _w;

  private ApplicationSet _currentApplicationSet;

  private NssaSettings _currentNssaSettings;

  private StubSettings _currentStubSettings;

  public ConfigurationBuilder(
      FlatJuniperCombinedParser parser,
      String text,
      Warnings warnings,
      Set<String> unimplementedFeatures,
      boolean unrecognizedAsRedFlag) {
    _parser = parser;
    _text = text;
    _configuration = new JuniperConfiguration(unimplementedFeatures);
    _currentRoutingInstance = _configuration.getDefaultRoutingInstance();
    _termRouteFilters = new HashMap<>();
    _unimplementedFeatures = unimplementedFeatures;
    _unrecognizedAsRedFlag = unrecognizedAsRedFlag;
    _w = warnings;
    _conjunctionPolicyIndex = 0;
    _disjunctionPolicyIndex = 0;
    _globalAddressBook =
        _configuration
            .getGlobalAddressBooks()
            .computeIfAbsent(GLOBAL_ADDRESS_BOOK_NAME, n -> new AddressBook(n, new TreeMap<>()));
  }

  private BatfishException convError(Class<?> type, ParserRuleContext ctx) {
    String typeName = type.getSimpleName();
    String txt = getFullText(ctx);
    return new BatfishException("Could not convert to " + typeName + ": " + txt);
  }

  @Override
  public void enterA_application(A_applicationContext ctx) {
    String name = ctx.name.getText();
    _currentApplication =
        _configuration.getApplications().computeIfAbsent(name, BaseApplication::new);
    _currentApplicationTerm = _currentApplication.getMainTerm();
    defineStructure(APPLICATION, name, ctx);
  }

  @Override
  public void enterA_application_set(A_application_setContext ctx) {
    String name = ctx.name.getText();
    _currentApplicationSet =
        _configuration.getApplicationSets().computeIfAbsent(name, ApplicationSet::new);
    defineStructure(APPLICATION_SET, name, ctx);
  }

  @Override
  public void exitA_application_set(A_application_setContext ctx) {
    _currentApplicationSet = null;
  }

  @Override
  public void exitAas_application(Aas_applicationContext ctx) {
    if (ctx.junos_application() != null) {
      JunosApplication application = toJunosApplication(ctx.junos_application());
      if (!application.hasDefinition()) {
        _w.redFlag(
            String.format(
                "unimplemented pre-defined junos application: '%s'",
                ctx.junos_application().getText()));
        return;
      }
      _currentApplicationSet.setMembers(
          ImmutableList.<ApplicationSetMemberReference>builder()
              .addAll(_currentApplicationSet.getMembers())
              .add(new JunosApplicationReference(application))
              .build());
    } else {
      String name = ctx.name.getText();
      int line = getLine(ctx.name.getStart());
      _currentApplicationSet.setMembers(
          ImmutableList.<ApplicationSetMemberReference>builder()
              .addAll(_currentApplicationSet.getMembers())
              .add(new ApplicationOrApplicationSetReference(name))
              .build());
      // only mark the structure as referenced if we know it's not a pre-defined application
      _configuration.referenceStructure(
          APPLICATION_OR_APPLICATION_SET, name, APPLICATION_SET_MEMBER_APPLICATION, line);
    }
  }

  @Override
  public void exitAas_application_set(Aas_application_setContext ctx) {
    if (ctx.junos_application_set() != null) {
      JunosApplicationSet junosApplicationSet = toJunosApplicationSet(ctx.junos_application_set());
      if (!junosApplicationSet.hasDefinition()) {
        _w.redFlag(
            String.format(
                "unimplemented pre-defined junos application-set: '%s'",
                ctx.junos_application_set().getText()));
        return;
      }
      _currentApplicationSet.setMembers(
          ImmutableList.<ApplicationSetMemberReference>builder()
              .addAll(_currentApplicationSet.getMembers())
              .add(new JunosApplicationSetReference(junosApplicationSet))
              .build());
    } else {
      String name = ctx.name.getText();
      int line = getLine(ctx.name.getStart());
      _currentApplicationSet.setMembers(
          ImmutableList.<ApplicationSetMemberReference>builder()
              .addAll(_currentApplicationSet.getMembers())
              .add(new ApplicationSetReference(name))
              .build());
      // only mark the structure as referenced if we know it's not a pre-defined application-set
      _configuration.referenceStructure(
          APPLICATION_SET, name, APPLICATION_SET_MEMBER_APPLICATION_SET, line);
    }
  }

  @Override
  public void enterAa_term(Aa_termContext ctx) {
    String name = ctx.name.getText();
    _currentApplicationTerm = _currentApplication.getTerms().computeIfAbsent(name, Term::new);
  }

  @Override
  public void enterB_allow(B_allowContext ctx) {
    if (_currentBgpGroup.getGroupName() != null) {
      _configuration.referenceStructure(
          BGP_GROUP, _currentBgpGroup.getGroupName(), BGP_ALLOW, getLine(ctx.getStart()));
    }
    if (ctx.IPV6_PREFIX() != null) {
      _currentBgpGroup.setIpv6(true);
      _currentBgpGroup = DUMMY_BGP_GROUP;
      // not supported for now
      return;
    }
    Prefix remotePrefix = Prefix.ZERO; // equivalent to ALL
    if (ctx.IP_PREFIX() != null) {
      remotePrefix = Prefix.parse(ctx.IP_PREFIX().getText());
    }
    Map<Prefix, IpBgpGroup> ipBgpGroups = _currentRoutingInstance.getIpBgpGroups();
    IpBgpGroup ipBgpGroup = ipBgpGroups.get(remotePrefix);
    if (ipBgpGroup == null) {
      ipBgpGroup = new IpBgpGroup(remotePrefix);
      ipBgpGroup.setParent(_currentBgpGroup);
      ipBgpGroups.put(remotePrefix, ipBgpGroup);
      ipBgpGroup.setDynamic(true);
    }
    _currentBgpGroup = ipBgpGroup;
  }

  @Override
  public void enterB_group(B_groupContext ctx) {
    String name = ctx.name.getText();
    Map<String, NamedBgpGroup> namedBgpGroups = _currentRoutingInstance.getNamedBgpGroups();
    NamedBgpGroup namedBgpGroup = namedBgpGroups.get(name);
    if (namedBgpGroup == null) {
      namedBgpGroup = new NamedBgpGroup(name);
      namedBgpGroup.setParent(_currentBgpGroup);
      namedBgpGroups.put(name, namedBgpGroup);
    }
    _currentBgpGroup = namedBgpGroup;
    defineStructure(BGP_GROUP, name, ctx);
  }

  @Override
  public void enterB_neighbor(B_neighborContext ctx) {
    if (_currentBgpGroup.getGroupName() != null) {
      _configuration.referenceStructure(
          BGP_GROUP,
          _currentBgpGroup.getGroupName(),
          BGP_NEIGHBOR,
          getLine(ctx.NEIGHBOR().getSymbol()));
    }
    if (ctx.IP_ADDRESS() != null) {
      Prefix remoteAddress =
          new Prefix(new Ip(ctx.IP_ADDRESS().getText()), Prefix.MAX_PREFIX_LENGTH);
      Map<Prefix, IpBgpGroup> ipBgpGroups = _currentRoutingInstance.getIpBgpGroups();
      IpBgpGroup ipBgpGroup = ipBgpGroups.get(remoteAddress);
      if (ipBgpGroup == null) {
        ipBgpGroup = new IpBgpGroup(remoteAddress);
        ipBgpGroup.setParent(_currentBgpGroup);
        ipBgpGroups.put(remoteAddress, ipBgpGroup);
      }
      _currentBgpGroup = ipBgpGroup;
    } else if (ctx.IPV6_ADDRESS() != null) {
      _currentBgpGroup.setIpv6(true);
      _currentBgpGroup = DUMMY_BGP_GROUP;
    }
  }

  @Override
  public void enterF_family(F_familyContext ctx) {
    _currentFirewallFamily = toFamily(ctx);
  }

  @Override
  public void enterF_filter(F_filterContext ctx) {
    String name = ctx.name.getText();
    Map<String, FirewallFilter> filters = _configuration.getFirewallFilters();
    _currentFilter = filters.get(name);
    if (_currentFirewallFamily == null) {
      _currentFirewallFamily = Family.INET;
    }
    if (_currentFilter == null) {
      _currentFilter = new FirewallFilter(name, _currentFirewallFamily);
      filters.put(name, _currentFilter);
    }
    defineStructure(FIREWALL_FILTER, name, ctx);
  }

  @Override
  public void enterFf_term(Ff_termContext ctx) {
    String name = ctx.name.getText();
    Map<String, FwTerm> terms = _currentFilter.getTerms();
    _currentFwTerm = terms.computeIfAbsent(name, FwTerm::new);
  }

  @Override
  public void enterFo_dhcp_relay(Fo_dhcp_relayContext ctx) {
    _currentDhcpRelayGroup =
        _currentRoutingInstance
            .getDhcpRelayGroups()
            .computeIfAbsent(DhcpRelayGroup.MASTER_DHCP_RELAY_GROUP_NAME, DhcpRelayGroup::new);
  }

  @Override
  public void enterFod_group(Fod_groupContext ctx) {
    String name = ctx.name.getText();
    _currentDhcpRelayGroup =
        _currentRoutingInstance.getDhcpRelayGroups().computeIfAbsent(name, DhcpRelayGroup::new);
  }

  @Override
  public void enterFod_server_group(Fod_server_groupContext ctx) {
    String name = ctx.name.getText();
    DhcpRelayServerGroup serverGroup =
        _currentRoutingInstance
            .getDhcpRelayServerGroups()
            .computeIfAbsent(name, DhcpRelayServerGroup::new);
    Ip ip = new Ip(ctx.address.getText());
    serverGroup.getServers().add(ip);
    defineStructure(DHCP_RELAY_SERVER_GROUP, name, ctx);
  }

  @Override
  public void enterI_unit(I_unitContext ctx) {
    String unit = ctx.num.getText();
    int definitionLine = getLine(ctx.num);
    String unitFullName = _currentMasterInterface.getName() + "." + unit;
    Map<String, Interface> units = _currentMasterInterface.getUnits();
    _currentInterface = units.get(unitFullName);
    if (_currentInterface == null) {
      _currentInterface = new Interface(unitFullName, definitionLine);
      _currentInterface.setRoutingInstance(_currentRoutingInstance.getName());
      _currentInterface.setParent(_currentMasterInterface);
      units.put(unitFullName, _currentInterface);
    }
  }

  @Override
  public void enterIfi_address(Ifi_addressContext ctx) {
    Set<InterfaceAddress> allAddresses = _currentInterface.getAllAddresses();
    InterfaceAddress address;
    if (ctx.IP_PREFIX() != null) {
      address = new InterfaceAddress(ctx.IP_PREFIX().getText());
    } else if (ctx.IP_ADDRESS() != null) {
      Ip ip = new Ip(ctx.IP_ADDRESS().getText());
      address = new InterfaceAddress(ip, Prefix.MAX_PREFIX_LENGTH);
    } else {
      throw new BatfishException("Invalid or missing address");
    }
    _currentInterfaceAddress = address;
    if (_currentInterface.getPrimaryAddress() == null) {
      _currentInterface.setPrimaryAddress(address);
    }
    if (_currentInterface.getPreferredAddress() == null) {
      _currentInterface.setPreferredAddress(address);
    }
    allAddresses.add(address);
    Ip ip = address.getIp();
    _currentInterface.getAllAddressIps().add(ip);
  }

  @Override
  public void enterIfia_vrrp_group(Ifia_vrrp_groupContext ctx) {
    int group = toInt(ctx.number);
    VrrpGroup currentVrrpGroup = _currentInterface.getVrrpGroups().get(group);
    if (currentVrrpGroup == null) {
      currentVrrpGroup = new VrrpGroup(group);
      currentVrrpGroup.setPreempt(DEFAULT_VRRP_PREEMPT);
      currentVrrpGroup.setPriority(DEFAULT_VRRP_PRIORITY);
      _currentInterface.getVrrpGroups().put(group, currentVrrpGroup);
    }
    _currentVrrpGroup = currentVrrpGroup;
  }

  @Override
  public void enterInt_named(Int_namedContext ctx) {
    Interface currentInterface;
    if (ctx.interface_id() == null) {
      currentInterface = _configuration.getGlobalMasterInterface();
    } else {
      String ifaceName = getInterfaceName(ctx.interface_id());
      Map<String, Interface> interfaces;
      String nodeDevicePrefix = "";
      if (ctx.interface_id().node == null) {
        interfaces = _configuration.getInterfaces();
      } else {
        String nodeDeviceName = ctx.interface_id().node.getText();
        nodeDevicePrefix = nodeDeviceName + ":";
        NodeDevice nodeDevice =
            _configuration.getNodeDevices().computeIfAbsent(nodeDeviceName, NodeDevice::new);
        interfaces = nodeDevice.getInterfaces();
      }
      currentInterface = interfaces.get(ifaceName);
      if (currentInterface == null) {
        String fullIfaceName = nodeDevicePrefix + ifaceName;
        int definitionLine = getLine(ctx.interface_id().getStart());
        currentInterface = new Interface(fullIfaceName, definitionLine);
        currentInterface.setRoutingInstance(_currentRoutingInstance.getName());
        currentInterface.setParent(_configuration.getGlobalMasterInterface());
        interfaces.put(fullIfaceName, currentInterface);
      }
    }
    _currentInterface = currentInterface;
    _currentMasterInterface = currentInterface;
  }

  @Override
  public void enterIs_interface(Is_interfaceContext ctx) {
    Map<String, Interface> interfaces = _configuration.getInterfaces();
    String unitFullName = null;
    String name = getInterfaceName(ctx.id);
    int definitionLine = getLine(ctx.id.name);
    String unit = null;
    if (ctx.id.unit != null) {
      unit = ctx.id.unit.getText();
    }
    unitFullName = name + "." + unit;
    _currentIsisInterface = interfaces.get(name);
    if (_currentIsisInterface == null) {
      _currentIsisInterface = new Interface(name, definitionLine);
      _currentIsisInterface.setRoutingInstance(_currentRoutingInstance.getName());
      interfaces.put(name, _currentIsisInterface);
    }
    if (unit != null) {
      definitionLine = getLine(ctx.id.unit);
      Map<String, Interface> units = _currentIsisInterface.getUnits();
      _currentIsisInterface = units.get(unitFullName);
      if (_currentIsisInterface == null) {
        _currentIsisInterface = new Interface(unitFullName, definitionLine);
        _currentIsisInterface.setRoutingInstance(_currentRoutingInstance.getName());
        units.put(unitFullName, _currentIsisInterface);
      }
    }
    _currentIsisInterface.getIsisSettings().setEnabled(true);
  }

  @Override
  public void enterIs_level(Is_levelContext ctx) {
    IsisSettings isisSettings = _currentRoutingInstance.getIsisSettings();
    int level = toInt(ctx.DEC());
    switch (level) {
      case 1:
        _currentIsisLevelSettings = isisSettings.getLevel1Settings();
        break;
      case 2:
        _currentIsisLevelSettings = isisSettings.getLevel2Settings();
        break;
      default:
        throw new BatfishException("invalid level: " + level);
    }
    _currentIsisLevelSettings.setEnabled(true);
  }

  @Override
  public void enterIsi_level(Isi_levelContext ctx) {
    int level = toInt(ctx.DEC());
    switch (level) {
      case 1:
        _currentIsisInterfaceLevelSettings =
            _currentIsisInterface.getIsisSettings().getLevel1Settings();
        break;
      case 2:
        _currentIsisInterfaceLevelSettings =
            _currentIsisInterface.getIsisSettings().getLevel2Settings();
        break;
      default:
        throw new BatfishException("invalid IS-IS level: " + level);
    }
    _currentIsisInterfaceLevelSettings.setEnabled(true);
  }

  @Override
  public void enterO_area(O_areaContext ctx) {
    Ip areaIp = new Ip(ctx.area.getText());
    Map<Long, OspfArea> areas = _currentRoutingInstance.getOspfAreas();
    _currentArea = areas.computeIfAbsent(areaIp.asLong(), OspfArea::new);
  }

  @Override
  public void enterOa_area_range(FlatJuniperParser.Oa_area_rangeContext ctx) {
    // Set up defaults: no overridden metric, routes advertised.
    _currentAreaRangeMetric = null;
    _currentAreaRangePrefix = null;
    _currentAreaRangeRestrict = false;

    if (ctx.IP_PREFIX() != null) {
      Prefix range = Prefix.parse(ctx.IP_PREFIX().getText());
      _currentAreaRangePrefix = range;
    } else {
      todo(ctx, F_IPV6);
    }
  }

  @Override
  public void enterOa_interface(Oa_interfaceContext ctx) {
    Map<String, Interface> interfaces = _configuration.getInterfaces();
    String unitFullName = null;
    if (ctx.ALL() != null) {
      _currentOspfInterface = _currentRoutingInstance.getGlobalMasterInterface();
    } else if (ctx.ip != null) {
      Ip ip = new Ip(ctx.ip.getText());
      for (Interface iface : interfaces.values()) {
        for (Interface unit : iface.getUnits().values()) {
          if (unit.getAllAddressIps().contains(ip)) {
            _currentOspfInterface = unit;
            unitFullName = unit.getName();
          }
        }
      }
      if (_currentOspfInterface == null) {
        throw new BatfishException("Could not find interface with ip address: " + ip);
      }
    } else {
      _currentOspfInterface = initInterface(ctx.id);
      unitFullName = _currentOspfInterface.getName();
    }
    Ip currentArea = new Ip(_currentArea.getName());
    Ip currentInterfaceArea = _currentOspfInterface.getOspfArea();
    if (currentInterfaceArea != null && !currentArea.equals(currentInterfaceArea)) {
      _w.redFlag("Interface: \"" + unitFullName + "\" assigned to multiple areas");
    } else {
      _currentOspfInterface.setOspfArea(currentArea);
    }
  }

  @Override
  public void enterOa_nssa(Oa_nssaContext ctx) {
    _currentNssaSettings = _currentArea.getNssaSettings();
    if (_currentNssaSettings == null) {
      _currentNssaSettings = new NssaSettings();
      _currentArea.setNssaSettings(_currentNssaSettings);
    }
    _currentArea.setStubType(StubType.NSSA);
  }

  @Override
  public void exitOa_nssa(Oa_nssaContext ctx) {
    _currentNssaSettings = null;
  }

  @Override
  public void enterOa_stub(Oa_stubContext ctx) {
    _currentStubSettings = _currentArea.getStubSettings();
    if (_currentStubSettings == null) {
      _currentStubSettings = new StubSettings();
      _currentArea.setStubSettings(_currentStubSettings);
    }
    _currentArea.setStubType(StubType.STUB);
  }

  @Override
  public void exitOa_stub(Oa_stubContext ctx) {
    _currentStubSettings = null;
  }

  @Override
  public void exitOai_passive(Oai_passiveContext ctx) {
    _currentOspfInterface.setOspfPassive(true);
  }

  @Override
  public void enterOan_default_lsa(Oan_default_lsaContext ctx) {
    if (_currentNssaSettings.getDefaultLsaType() == OspfDefaultOriginateType.NONE) {
      _currentNssaSettings.setDefaultLsaType(OspfDefaultOriginateType.INTER_AREA);
    }
  }

  @Override
  public void exitOand_metric_type(Oand_metric_typeContext ctx) {
    if (ctx.METRIC_TYPE_1() != null) {
      _currentNssaSettings.setDefaultLsaType(OspfDefaultOriginateType.EXTERNAL_TYPE1);
    }
    if (ctx.METRIC_TYPE_2() != null) {
      _currentNssaSettings.setDefaultLsaType(OspfDefaultOriginateType.EXTERNAL_TYPE2);
    }
  }

  @Override
  public void exitOand_type_7(Oand_type_7Context ctx) {
    if (_currentNssaSettings.getDefaultLsaType() != OspfDefaultOriginateType.EXTERNAL_TYPE1) {
      _currentNssaSettings.setDefaultLsaType(OspfDefaultOriginateType.EXTERNAL_TYPE2);
    }
  }

  @Override
  public void exitOan_no_summaries(Oan_no_summariesContext ctx) {
    _currentNssaSettings.setNoSummaries(true);
  }

  @Override
  public void exitOas_no_summaries(Oas_no_summariesContext ctx) {
    _currentStubSettings.setNoSummaries(true);
  }

  @Override
  public void enterP_bgp(P_bgpContext ctx) {
    _currentBgpGroup = _currentRoutingInstance.getMasterBgpGroup();
  }

  @Override
  public void enterPo_community(Po_communityContext ctx) {
    String name = ctx.name.getText();
    Map<String, CommunityList> communityLists = _configuration.getCommunityLists();
    _currentCommunityList = communityLists.computeIfAbsent(name, CommunityList::new);
  }

  @Override
  public void enterPo_policy_statement(Po_policy_statementContext ctx) {
    String name = ctx.name.getText();
    Map<String, PolicyStatement> policyStatements = _configuration.getPolicyStatements();
    _currentPolicyStatement = policyStatements.computeIfAbsent(name, PolicyStatement::new);
    _currentPsTerm = _currentPolicyStatement.getDefaultTerm();
    _currentPsThens = _currentPsTerm.getThens();
    defineStructure(POLICY_STATEMENT, name, ctx);
  }

  @Override
  public void enterPo_prefix_list(Po_prefix_listContext ctx) {
    String name = ctx.name.getText();
    Map<String, PrefixList> prefixLists = _configuration.getPrefixLists();
    _currentPrefixList = prefixLists.computeIfAbsent(name, PrefixList::new);
    defineStructure(PREFIX_LIST, name, ctx);
  }

  @Override
  public void enterPops_term(Pops_termContext ctx) {
    String name = ctx.name.getText();
    Map<String, PsTerm> terms = _currentPolicyStatement.getTerms();
    _currentPsTerm = terms.computeIfAbsent(name, PsTerm::new);
    _currentPsThens = _currentPsTerm.getThens();
  }

  @Override
  public void enterPopsf_route_filter(Popsf_route_filterContext ctx) {
    _currentRouteFilter = _termRouteFilters.get(_currentPsTerm);
    if (_currentRouteFilter == null) {
      String rfName = _currentPolicyStatement.getName() + ":" + _currentPsTerm.getName();
      _currentRouteFilter = new RouteFilter(rfName);
      _termRouteFilters.put(_currentPsTerm, _currentRouteFilter);
      _configuration.getRouteFilters().put(rfName, _currentRouteFilter);
      PsFromRouteFilter from = new PsFromRouteFilter(rfName);
      _currentPsTerm.getFroms().add(from);
    }
    if (ctx.IP_PREFIX() != null) {
      _currentRouteFilterPrefix = Prefix.parse(ctx.IP_PREFIX().getText());
      _currentRouteFilter.setIpv4(true);
    } else if (ctx.IPV6_PREFIX() != null) {
      _currentRoute6FilterPrefix = new Prefix6(ctx.IPV6_PREFIX().getText());
      _currentRouteFilter.setIpv6(true);
    }
  }

  @Override
  public void exitPopsfrf_address_mask(Popsfrf_address_maskContext ctx) {
    if (_currentRouteFilterPrefix != null) { // ipv4
      if (ctx.IP_ADDRESS() != null) {
        Route4FilterLine line =
            new Route4FilterLineAddressMask(
                _currentRouteFilterPrefix, new Ip(ctx.IP_ADDRESS().getText()).inverted());
        _currentRouteFilterLine = _currentRouteFilter.insertLine(line, Route4FilterLine.class);
      } else {
        _w.redFlag(
            String.format(
                "Route filter mask does not match version for prefix %s",
                _currentRouteFilterPrefix));
      }
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      if (ctx.IPV6_ADDRESS() != null) {
        Route6FilterLine line =
            new Route6FilterLineAddressMask(
                _currentRoute6FilterPrefix, new Ip6(ctx.IPV6_ADDRESS().getText()).inverted());
        _currentRoute6FilterLine = _currentRouteFilter.insertLine(line, Route6FilterLine.class);
      } else {
        _w.redFlag(
            String.format(
                "Route filter mask does not match version for prefix %s",
                _currentRouteFilterPrefix));
      }
    }
  }

  @Override
  public void enterPopsfrf_exact(Popsfrf_exactContext ctx) {
    if (_currentRouteFilterPrefix != null) { // ipv4
      Route4FilterLine line = new Route4FilterLineExact(_currentRouteFilterPrefix);
      _currentRouteFilterLine = _currentRouteFilter.insertLine(line, Route4FilterLine.class);
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      Route6FilterLine line = new Route6FilterLineExact(_currentRoute6FilterPrefix);
      _currentRoute6FilterLine = _currentRouteFilter.insertLine(line, Route6FilterLine.class);
    }
  }

  @Override
  public void enterPopsfrf_longer(Popsfrf_longerContext ctx) {
    if (_currentRouteFilterPrefix != null) { // ipv4
      Route4FilterLine line = new Route4FilterLineLonger(_currentRouteFilterPrefix);
      _currentRouteFilterLine = _currentRouteFilter.insertLine(line, Route4FilterLine.class);
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      Route6FilterLine line = new Route6FilterLineLonger(_currentRoute6FilterPrefix);
      _currentRoute6FilterLine = _currentRouteFilter.insertLine(line, Route6FilterLine.class);
    }
  }

  @Override
  public void enterPopsfrf_orlonger(Popsfrf_orlongerContext ctx) {
    if (_currentRouteFilterPrefix != null) { // ipv4
      Route4FilterLine line = new Route4FilterLineOrLonger(_currentRouteFilterPrefix);
      _currentRouteFilterLine = _currentRouteFilter.insertLine(line, Route4FilterLine.class);
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      Route6FilterLine line = new Route6FilterLineOrLonger(_currentRoute6FilterPrefix);
      _currentRoute6FilterLine = _currentRouteFilter.insertLine(line, Route6FilterLine.class);
    }
  }

  @Override
  public void enterPopsfrf_prefix_length_range(Popsfrf_prefix_length_rangeContext ctx) {
    int minPrefixLength = toInt(ctx.low);
    int maxPrefixLength = toInt(ctx.high);
    if (_currentRouteFilterPrefix != null) { // ipv4
      Route4FilterLine line =
          new Route4FilterLineLengthRange(
              _currentRouteFilterPrefix, minPrefixLength, maxPrefixLength);
      _currentRouteFilterLine = _currentRouteFilter.insertLine(line, Route4FilterLine.class);
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      Route6FilterLine line =
          new Route6FilterLineLengthRange(
              _currentRoute6FilterPrefix, minPrefixLength, maxPrefixLength);
      _currentRoute6FilterLine = _currentRouteFilter.insertLine(line, Route6FilterLine.class);
    }
  }

  @Override
  public void enterPopsfrf_then(Popsfrf_thenContext ctx) {
    if (_currentRouteFilterPrefix != null) { // ipv4
      Route4FilterLine line = _currentRouteFilterLine;
      _currentPsThens = line.getThens();
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      Route6FilterLine line = _currentRoute6FilterLine;
      _currentPsThens = line.getThens();
    }
  }

  @Override
  public void enterPopsfrf_through(Popsfrf_throughContext ctx) {
    if (_currentRouteFilterPrefix != null) { // ipv4
      Prefix throughPrefix = Prefix.parse(ctx.IP_PREFIX().getText());
      Route4FilterLine line = new Route4FilterLineThrough(_currentRouteFilterPrefix, throughPrefix);
      _currentRouteFilterLine = _currentRouteFilter.insertLine(line, Route4FilterLine.class);
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      Prefix6 throughPrefix6 = new Prefix6(ctx.IPV6_PREFIX().getText());
      Route6FilterLine line =
          new Route6FilterLineThrough(_currentRoute6FilterPrefix, throughPrefix6);
      _currentRoute6FilterLine = _currentRouteFilter.insertLine(line, Route6FilterLine.class);
    }
  }

  @Override
  public void enterPopsfrf_upto(Popsfrf_uptoContext ctx) {
    int maxPrefixLength = toInt(ctx.high);
    if (_currentRouteFilterPrefix != null) { // ipv4
      Route4FilterLine line = new Route4FilterLineUpTo(_currentRouteFilterPrefix, maxPrefixLength);
      _currentRouteFilterLine = _currentRouteFilter.insertLine(line, Route4FilterLine.class);
    } else if (_currentRoute6FilterPrefix != null) { // ipv6
      Route6FilterLine line = new Route6FilterLineUpTo(_currentRoute6FilterPrefix, maxPrefixLength);
      _currentRoute6FilterLine = _currentRouteFilter.insertLine(line, Route6FilterLine.class);
    }
  }

  @Override
  public void enterRi_named_routing_instance(Ri_named_routing_instanceContext ctx) {
    String name;
    name = ctx.name.getText();
    _currentRoutingInstance =
        _configuration.getRoutingInstances().computeIfAbsent(name, RoutingInstance::new);
  }

  @Override
  public void enterRo_aggregate(Ro_aggregateContext ctx) {
    if (ctx.prefix != null) {
      Prefix prefix = Prefix.parse(ctx.IP_PREFIX().getText());
      Map<Prefix, AggregateRoute> aggregateRoutes = _currentRib.getAggregateRoutes();
      _currentAggregateRoute = aggregateRoutes.computeIfAbsent(prefix, AggregateRoute::new);
    } else {
      _currentAggregateRoute = DUMMY_AGGREGATE_ROUTE;
    }
  }

  @Override
  public void enterRo_generate(Ro_generateContext ctx) {
    if (ctx.IP_PREFIX() != null) {
      Prefix prefix = Prefix.parse(ctx.IP_PREFIX().getText());
      Map<Prefix, GeneratedRoute> generatedRoutes = _currentRib.getGeneratedRoutes();
      _currentGeneratedRoute = generatedRoutes.computeIfAbsent(prefix, GeneratedRoute::new);
    } else if (ctx.IPV6_PREFIX() != null) {
      // dummy generated route not added to configuration
      _currentGeneratedRoute = new GeneratedRoute(null);
      todo(ctx, F_IPV6);
    }
  }

  @Override
  public void enterRo_rib(Ro_ribContext ctx) {
    String name = ctx.name.getText();
    Map<String, RoutingInformationBase> ribs = _currentRoutingInstance.getRibs();
    _currentRib = ribs.computeIfAbsent(name, RoutingInformationBase::new);
  }

  @Override
  public void enterRos_route(Ros_routeContext ctx) {
    if (ctx.IP_PREFIX() != null) {
      Prefix prefix = Prefix.parse(ctx.IP_PREFIX().getText());
      Map<Prefix, StaticRoute> staticRoutes = _currentRib.getStaticRoutes();
      _currentStaticRoute = staticRoutes.computeIfAbsent(prefix, StaticRoute::new);
    } else if (ctx.IPV6_PREFIX() != null) {
      _currentStaticRoute = DUMMY_STATIC_ROUTE;
    }
  }

  @Override
  public void enterS_firewall(S_firewallContext ctx) {
    _currentFirewallFamily = Family.INET;
  }

  @Override
  public void enterS_routing_options(S_routing_optionsContext ctx) {
    _currentRib = _currentRoutingInstance.getRibs().get(RoutingInformationBase.RIB_IPV4_UNICAST);
  }

  @Override
  public void enterS_snmp(S_snmpContext ctx) {
    SnmpServer snmpServer = _currentRoutingInstance.getSnmpServer();
    if (snmpServer == null) {
      snmpServer = new SnmpServer();
      snmpServer.setVrf(_currentRoutingInstance.getName());
      _currentRoutingInstance.setSnmpServer(snmpServer);
    }
    _currentSnmpServer = snmpServer;
  }

  @Override
  public void enterSe_address_book(Se_address_bookContext ctx) {
    String name = ctx.GLOBAL().getText();
    _currentAddressBook =
        _configuration
            .getGlobalAddressBooks()
            .computeIfAbsent(name, n -> new AddressBook(n, new TreeMap<>()));
  }

  @Override
  public void exitSe_address_book(Se_address_bookContext ctx) {
    _currentAddressBook = null;
  }

  @Override
  public void enterSead_address_set(Sead_address_setContext ctx) {
    String name = ctx.name.getText();
    AddressBookEntry entry =
        _currentAddressBook.getEntries().computeIfAbsent(name, AddressSetAddressBookEntry::new);
    try {
      _currentAddressSetAddressBookEntry = (AddressSetAddressBookEntry) entry;
    } catch (ClassCastException e) {
      _w.redFlag(
          "Cannot create address-set address-book entry \""
              + name
              + "\" because a different type of address-book entry with that name already exists");
      // Create a throw-away entry so deeper parsing does not fail for this entry
      _currentAddressSetAddressBookEntry = new AddressSetAddressBookEntry(name);
    }
  }

  @Override
  public void exitSead_address(Sead_addressContext ctx) {
    String name = ctx.name.getText();
    if (ctx.wildcard_address() != null) {
      IpWildcard ipWildcard = toIpWildcard(ctx.wildcard_address());
      AddressBookEntry addressEntry = new AddressAddressBookEntry(name, ipWildcard);
      _currentAddressBook.getEntries().put(name, addressEntry);
    } else if (ctx.address != null) {
      IpWildcard ipWildcard = new IpWildcard(new Ip(ctx.address.getText()));
      AddressBookEntry addressEntry = new AddressAddressBookEntry(name, ipWildcard);
      _currentAddressBook.getEntries().put(name, addressEntry);
    } else if (ctx.prefix != null) {
      IpWildcard ipWildcard = new IpWildcard(Prefix.parse(ctx.prefix.getText()));
      AddressBookEntry addressEntry = new AddressAddressBookEntry(name, ipWildcard);
      _currentAddressBook.getEntries().put(name, addressEntry);
    } else if (ctx.DESCRIPTION() != null) {
      /* TODO - data model doesn't have a place to put this yet. */
    } else {
      throw convError(IpWildcard.class, ctx);
    }
  }

  @Override
  public void exitSead_address_set(Sead_address_setContext ctx) {
    _currentAddressSetAddressBookEntry = null;
  }

  @Override
  public void exitSeada_address(Seada_addressContext ctx) {
    String name = ctx.name.getText();
    _currentAddressSetAddressBookEntry
        .getEntries()
        .add(new AddressSetEntry(name, _currentAddressBook));
  }

  @Override
  public void exitSeada_address_set(Seada_address_setContext ctx) {
    String name = ctx.name.getText();
    _currentAddressSetAddressBookEntry
        .getEntries()
        .add(new AddressSetEntry(name, _currentAddressBook));
  }

  @Override
  public void enterSe_authentication_key_chain(Se_authentication_key_chainContext ctx) {
    String name = ctx.name.getText();
    int line = getLine(ctx.getStart());
    JuniperAuthenticationKeyChain authenticationkeyChain =
        _configuration
            .getAuthenticationKeyChains()
            .computeIfAbsent(name, n -> new JuniperAuthenticationKeyChain(n, line));
    _currentAuthenticationKeyChain = authenticationkeyChain;
    defineStructure(AUTHENTICATION_KEY_CHAIN, name, ctx);
  }

  @Override
  public void enterSea_key(Sea_keyContext ctx) {
    String name = ctx.name.getText();
    JuniperAuthenticationKey authenticationkey =
        _currentAuthenticationKeyChain
            .getKeys()
            .computeIfAbsent(name, JuniperAuthenticationKey::new);
    _currentAuthenticationKey = authenticationkey;
  }

  @Override
  public void enterSeik_gateway(Seik_gatewayContext ctx) {
    String name = ctx.name.getText();
    _currentIkeGateway = _configuration.getIkeGateways().computeIfAbsent(name, IkeGateway::new);
    defineStructure(IKE_GATEWAY, name, ctx);
  }

  @Override
  public void enterSeik_policy(Seik_policyContext ctx) {
    String name = ctx.name.getText();
    _currentIkePolicy = _configuration.getIkePolicies().computeIfAbsent(name, IkePolicy::new);
    defineStructure(IKE_POLICY, name, ctx);
  }

  @Override
  public void enterSeik_proposal(Seik_proposalContext ctx) {
    String name = ctx.name.getText();
    _currentIkeProposal = _configuration.getIkeProposals().computeIfAbsent(name, IkeProposal::new);
    defineStructure(IKE_PROPOSAL, name, ctx);
  }

  @Override
  public void enterSeip_policy(Seip_policyContext ctx) {
    String name = ctx.name.getText();
    _currentIpsecPolicy = _configuration.getIpsecPolicies().computeIfAbsent(name, IpsecPolicy::new);
    defineStructure(IPSEC_POLICY, name, ctx);
  }

  @Override
  public void enterSeip_proposal(Seip_proposalContext ctx) {
    String name = ctx.name.getText();
    _currentIpsecProposal =
        _configuration.getIpsecProposals().computeIfAbsent(name, IpsecProposal::new);
    defineStructure(IPSEC_PROPOSAL, name, ctx);
  }

  @Override
  public void enterSeip_vpn(Seip_vpnContext ctx) {
    String name = ctx.name.getText();
    _currentIpsecVpn = _configuration.getIpsecVpns().computeIfAbsent(name, IpsecVpn::new);
  }

  @Override
  public void enterSep_from_zone(Sep_from_zoneContext ctx) {
    if (ctx.from.JUNOS_HOST() != null && ctx.to.JUNOS_HOST() != null) {
      _w.redFlag("Cannot create security policy from junos-host to junos-host");
    } else {
      String fromName = ctx.from.getText();
      String toName = ctx.to.getText();
      String policyName = "~FROM_ZONE~" + fromName + "~TO_ZONE~" + toName;
      if (ctx.from.JUNOS_HOST() == null) {
        _currentFromZone = _configuration.getZones().get(fromName);
        if (_currentFromZone == null) {
          _currentFromZone = new Zone(fromName, _configuration.getGlobalAddressBooks());
          _configuration.getZones().put(fromName, _currentFromZone);
          _configuration
              .getFirewallFilters()
              .put(
                  _currentFromZone.getInboundFilter().getName(),
                  _currentFromZone.getInboundFilter());
        }
      }

      if (ctx.to.JUNOS_HOST() == null) {
        _currentToZone = _configuration.getZones().get(toName);
        if (_currentToZone == null) {
          _currentToZone = new Zone(toName, _configuration.getGlobalAddressBooks());
          _configuration
              .getFirewallFilters()
              .put(_currentToZone.getInboundFilter().getName(), _currentToZone.getInboundFilter());
          _configuration.getZones().put(toName, _currentToZone);
        }
      }

      if (ctx.from.JUNOS_HOST() != null) {
        // Policy for traffic originating from this device
        _currentFilter = _currentToZone.getFromHostFilter();
        if (_currentFilter == null) {
          _currentFilter = new FirewallFilter(policyName, Family.INET);
          _configuration.getFirewallFilters().put(policyName, _currentFilter);
          _currentToZone.setFromHostFilter(_currentFilter);
        }
      } else if (ctx.to.JUNOS_HOST() != null) {
        // Policy for traffic destined for this device
        _currentFilter = _currentFromZone.getToHostFilter();
        if (_currentFilter == null) {
          _currentFilter = new FirewallFilter(policyName, Family.INET);
          _configuration.getFirewallFilters().put(policyName, _currentFilter);
          _currentFromZone.setToHostFilter(_currentFilter);
        }
      } else {
        // Policy for thru traffic
        _currentFilter = _currentFromZone.getToZonePolicies().get(toName);
        if (_currentFilter == null) {
          _currentFilter = new FirewallFilter(policyName, Family.INET);
          _configuration.getFirewallFilters().put(policyName, _currentFilter);
          _currentFromZone.getToZonePolicies().put(toName, _currentFilter);
        }
        // Add this filter to the to-zone for easy combination with egress ACL
        _currentToZone.getFromZonePolicies().put(policyName, _currentFilter);
      }

      /*
       * Need to keep track of the from-zone for this filter to apply srcInterface filter to the
       * firewallFilter
       */
      if (_currentFromZone != null) {
        _currentFilter.setFromZone(_currentFromZone.getName());
      }
    }
  }

  @Override
  public void enterSep_global(Sep_globalContext ctx) {
    _currentFilter =
        _configuration
            .getFirewallFilters()
            .computeIfAbsent(ACL_NAME_GLOBAL_POLICY, n -> new FirewallFilter(n, Family.INET));
  }

  @Override
  public void exitSep_global(Sep_globalContext ctx) {
    _currentFilter = null;
  }

  @Override
  public void enterSepctx_policy(Sepctx_policyContext ctx) {
    String termName = ctx.name.getText();
    _currentFwTerm = _currentFilter.getTerms().computeIfAbsent(termName, FwTerm::new);
  }

  @Override
  public void enterSez_security_zone(Sez_security_zoneContext ctx) {
    String zoneName = ctx.zone().getText();
    _currentZone = _configuration.getZones().get(zoneName);
    if (_currentZone == null) {
      _currentZone = new Zone(zoneName, _configuration.getGlobalAddressBooks());
      _configuration
          .getFirewallFilters()
          .put(_currentZone.getInboundFilter().getName(), _currentZone.getInboundFilter());
      _configuration.getZones().put(zoneName, _currentZone);
    }
    _currentZoneInboundFilter = _currentZone.getInboundFilter();
  }

  @Override
  public void enterSezs_address_book(Sezs_address_bookContext ctx) {
    _currentAddressBook = _currentZone.getAddressBook();
  }

  @Override
  public void enterSezs_host_inbound_traffic(Sezs_host_inbound_trafficContext ctx) {
    if (_currentZoneInterface != null) {
      _currentZoneInboundFilter =
          _currentZone.getInboundInterfaceFilters().get(_currentZoneInterface);
      if (_currentZoneInboundFilter == null) {
        String name =
            "~ZONE_INTERFACE_FILTER~"
                + _currentZone.getName()
                + "~INTERFACE~"
                + _currentZoneInterface.getName();
        _currentZoneInboundFilter = new FirewallFilter(name, Family.INET);
        _configuration.getFirewallFilters().put(name, _currentZoneInboundFilter);
        _currentZone
            .getInboundInterfaceFilters()
            .put(_currentZoneInterface, _currentZoneInboundFilter);
      }
    }
  }

  @Override
  public void enterSezs_interfaces(Sezs_interfacesContext ctx) {
    _currentZoneInterface = initInterface(ctx.interface_id());
    _currentZone.getInterfaces().add(_currentZoneInterface);
    _configuration.getInterfaceZones().put(_currentZoneInterface, _currentZone);
  }

  @Override
  public void enterSezsa_address_set(Sezsa_address_setContext ctx) {
    String name = ctx.name.getText();
    AddressBookEntry entry =
        _currentAddressBook.getEntries().computeIfAbsent(name, AddressSetAddressBookEntry::new);
    try {
      _currentAddressSetAddressBookEntry = (AddressSetAddressBookEntry) entry;
    } catch (ClassCastException e) {
      _w.redFlag(
          "Cannot create address-set address-book entry \""
              + name
              + "\" because a different type of address-book entry with that name already exists");
      // Create a throw-away entry so deeper parsing does not fail for this entry
      _currentAddressSetAddressBookEntry = new AddressSetAddressBookEntry(name);
    }
  }

  @Override
  public void enterSnmp_community(Snmp_communityContext ctx) {
    String community = ctx.comm.getText();
    SnmpCommunity snmpCommunity =
        _currentSnmpServer.getCommunities().computeIfAbsent(community, SnmpCommunity::new);
    _currentSnmpCommunity = snmpCommunity;
  }

  @Override
  public void enterSy_tacplus_server(Sy_tacplus_serverContext ctx) {
    String hostname = ctx.hostname.getText();
    _configuration.getTacplusServers().add(hostname);
    _currentTacplusServer =
        _configuration.getJf().getTacplusServers().computeIfAbsent(hostname, TacplusServer::new);
  }

  @Override
  public void enterSyn_server(Syn_serverContext ctx) {
    String hostname = ctx.hostname.getText();
    _configuration.getNtpServers().add(hostname);
  }

  @Override
  public void enterSys_host(Sys_hostContext ctx) {
    String hostname = ctx.hostname.getText();
    _configuration.getSyslogHosts().add(hostname);
  }

  @Override
  public void enterS_vlans_named(S_vlans_namedContext ctx) {
    _currentVlanName = ctx.name.getText();
    defineStructure(VLAN, _currentVlanName, ctx);
  }

  @Override
  public void exitA_application(A_applicationContext ctx) {
    _currentApplication = null;
    _currentApplicationTerm = null;
  }

  @Override
  public void exitAa_term(Aa_termContext ctx) {
    _currentApplicationTerm = _currentApplication.getMainTerm();
  }

  @Override
  public void exitAat_destination_port(Aat_destination_portContext ctx) {
    SubRange subrange = toSubRange(ctx.subrange());
    HeaderSpace oldHeaderSpace = _currentApplicationTerm.getHeaderSpace();
    _currentApplicationTerm.setHeaderSpace(
        oldHeaderSpace
            .toBuilder()
            .setDstPorts(
                ImmutableSet.<SubRange>builder()
                    .addAll(oldHeaderSpace.getDstPorts())
                    .add(subrange)
                    .build())
            .build());
  }

  @Override
  public void exitAat_protocol(Aat_protocolContext ctx) {
    IpProtocol protocol = toIpProtocol(ctx.ip_protocol());
    HeaderSpace oldHeaderSpace = _currentApplicationTerm.getHeaderSpace();
    _currentApplicationTerm.setHeaderSpace(
        oldHeaderSpace
            .toBuilder()
            .setIpProtocols(
                ImmutableSet.<IpProtocol>builder()
                    .addAll(oldHeaderSpace.getIpProtocols())
                    .add(protocol)
                    .build())
            .build());
  }

  @Override
  public void exitAat_source_port(Aat_source_portContext ctx) {
    SubRange subrange = toSubRange(ctx.subrange());
    HeaderSpace oldHeaderSpace = _currentApplicationTerm.getHeaderSpace();
    _currentApplicationTerm.setHeaderSpace(
        oldHeaderSpace
            .toBuilder()
            .setSrcPorts(
                ImmutableSet.<SubRange>builder()
                    .addAll(oldHeaderSpace.getSrcPorts())
                    .add(subrange)
                    .build())
            .build());
  }

  @Override
  public void exitB_advertise_external(B_advertise_externalContext ctx) {
    _currentBgpGroup.setAdvertiseExternal(true);
  }

  @Override
  public void exitB_advertise_inactive(B_advertise_inactiveContext ctx) {
    _currentBgpGroup.setAdvertiseInactive(true);
  }

  @Override
  public void exitB_advertise_peer_as(B_advertise_peer_asContext ctx) {
    _currentBgpGroup.setAdvertisePeerAs(true);
  }

  @Override
  public void exitB_authentication_algorithm(B_authentication_algorithmContext ctx) {
    if (ctx.AES_128_CMAC_96() != null) {
      _currentBgpGroup.setAuthenticationAlgorithm(BgpAuthenticationAlgorithm.AES_128_CMAC_96);
    } else if (ctx.MD5() != null) {
      _currentBgpGroup.setAuthenticationAlgorithm(BgpAuthenticationAlgorithm.TCP_ENHANCED_MD5);
    } else {
      _currentBgpGroup.setAuthenticationAlgorithm(BgpAuthenticationAlgorithm.HMAC_SHA_1_96);
    }
  }

  @Override
  public void exitB_authentication_key(B_authentication_keyContext ctx) {
    _currentBgpGroup.setAuthenticationKey(ctx.key.getText());
  }

  @Override
  public void exitB_authentication_key_chain(B_authentication_key_chainContext ctx) {
    _currentBgpGroup.setAuthenticationKeyChainName(ctx.name.getText());
    int line = getLine(ctx.getStart());
    _configuration.referenceStructure(
        AUTHENTICATION_KEY_CHAIN, ctx.name.getText(), AUTHENTICATION_KEY_CHAINS_POLICY, line);
  }

  @Override
  public void exitB_cluster(B_clusterContext ctx) {
    Ip clusterId = new Ip(ctx.id.getText());
    _currentBgpGroup.setClusterId(clusterId);
  }

  @Override
  public void exitB_description(B_descriptionContext ctx) {
    _currentBgpGroup.setDescription(ctx.description().text.getText());
  }

  @Override
  public void exitB_enforce_first_as(B_enforce_first_asContext ctx) {
    _currentBgpGroup.setEnforceFirstAs(true);
  }

  @Override
  public void exitB_export(B_exportContext ctx) {
    Policy_expressionContext expr = ctx.expr;
    _currentBgpGroup.getExportPolicies().add(toComplexPolicyStatement(expr, BGP_EXPORT_POLICY));
  }

  @Override
  public void exitB_import(B_importContext ctx) {
    Policy_expressionContext expr = ctx.expr;
    _currentBgpGroup.getImportPolicies().add(toComplexPolicyStatement(expr, BGP_IMPORT_POLICY));
  }

  @Override
  public void exitB_local_address(B_local_addressContext ctx) {
    if (ctx.IP_ADDRESS() != null) {
      Ip localAddress = new Ip(ctx.IP_ADDRESS().getText());
      _currentBgpGroup.setLocalAddress(localAddress);
    }
  }

  @Override
  public void exitB_multihop(B_multihopContext ctx) {
    _currentBgpGroup.setEbgpMultihop(true);
  }

  @Override
  public void exitB_multipath(B_multipathContext ctx) {
    _currentBgpGroup.setMultipath(true);
    if (ctx.MULTIPLE_AS() != null) {
      _currentBgpGroup.setMultipathMultipleAs(true);
    }
  }

  @Override
  public void exitB_remove_private(B_remove_privateContext ctx) {
    _currentBgpGroup.setRemovePrivate(true);
  }

  @Override
  public void exitB_type(B_typeContext ctx) {
    if (ctx.INTERNAL() != null) {
      _currentBgpGroup.setType(BgpGroupType.INTERNAL);
    } else if (ctx.EXTERNAL() != null) {
      _currentBgpGroup.setType(BgpGroupType.EXTERNAL);
    }
  }

  @Override
  public void exitBl_loops(Bl_loopsContext ctx) {
    todo(ctx, F_BGP_LOCAL_AS_LOOPS);
    int loops = toInt(ctx.DEC());
    _currentBgpGroup.setLoops(loops);
  }

  @Override
  public void exitBl_number(Bl_numberContext ctx) {
    int localAs = toInt(ctx.as);
    _currentBgpGroup.setLocalAs(localAs);
  }

  @Override
  public void exitBl_private(Bl_privateContext ctx) {
    todo(ctx, F_BGP_LOCAL_AS_PRIVATE);
  }

  @Override
  public void exitBpa_as(Bpa_asContext ctx) {
    int peerAs = toInt(ctx.as);
    _currentBgpGroup.setPeerAs(peerAs);
  }

  @Override
  public void exitF_filter(F_filterContext ctx) {
    _currentFilter = null;
  }

  @Override
  public void exitFf_term(Ff_termContext ctx) {
    _currentFwTerm = null;
  }

  @Override
  public void exitFftf_destination_address(Fftf_destination_addressContext ctx) {
    FwFrom from;
    IpWildcard ipWildcard = formIpWildCard(ctx.fftfa_address_mask_prefix());
    if (ipWildcard != null) {
      from =
          ctx.EXCEPT() != null
              ? new FwFromDestinationAddressExcept(ipWildcard)
              : new FwFromDestinationAddress(ipWildcard);
      _currentFwTerm.getFroms().add(from);
    }
  }

  @Override
  public void exitFftf_destination_port(Fftf_destination_portContext ctx) {
    if (ctx.port() != null) {
      int port = getPortNumber(ctx.port());
      SubRange subrange = new SubRange(port, port);
      FwFrom from = new FwFromDestinationPort(subrange);
      _currentFwTerm.getFroms().add(from);
    } else if (ctx.range() != null) {
      for (SubrangeContext subrangeContext : ctx.range().range_list) {
        SubRange subrange = toSubRange(subrangeContext);
        FwFrom from = new FwFromDestinationPort(subrange);
        _currentFwTerm.getFroms().add(from);
      }
    }
  }

  @Override
  public void exitFftf_destination_prefix_list(Fftf_destination_prefix_listContext ctx) {
    String name = ctx.name.getText();
    // temporary
    if (_currentFilter.getFamily() != Family.INET) {
      _configuration.getIgnoredPrefixLists().add(name);
    }
    FwFrom from;
    if (ctx.EXCEPT() != null) {
      from = new FwFromDestinationPrefixListExcept(name);
    } else {
      from = new FwFromDestinationPrefixList(name);
    }
    _currentFwTerm.getFroms().add(from);
    _configuration.referenceStructure(
        PREFIX_LIST, name, FIREWALL_FILTER_DESTINATION_PREFIX_LIST, getLine(ctx.name.start));
  }

  @Override
  public void exitFftf_first_fragment(Fftf_first_fragmentContext ctx) {
    SubRange subRange = new SubRange(0, 0);
    FwFrom from = new FwFromFragmentOffset(subRange, false);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_fragment_offset(Fftf_fragment_offsetContext ctx) {
    SubRange subRange = toSubRange(ctx.subrange());
    FwFrom from = new FwFromFragmentOffset(subRange, false);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_fragment_offset_except(Fftf_fragment_offset_exceptContext ctx) {
    SubRange subRange = toSubRange(ctx.subrange());
    FwFrom from = new FwFromFragmentOffset(subRange, true);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_icmp_code(Fftf_icmp_codeContext ctx) {
    if (_currentFirewallFamily == Family.INET6) {
      // TODO: support icmpv6
      return;
    }
    SubRange icmpCodeRange = null;
    if (ctx.subrange() != null) {
      icmpCodeRange = toSubRange(ctx.subrange());
    } else if (ctx.icmp_code() != null) {
      Integer icmpCode = toIcmpCode(ctx.icmp_code(), _w);
      if (icmpCode != null) {
        icmpCodeRange = new SubRange(icmpCode, icmpCode);
      }
    } else {
      _w.redFlag(String.format("Invalid icmp-code: '%s'", ctx.getText()));
    }
    if (icmpCodeRange != null) {
      FwFrom from = new FwFromIcmpCode(icmpCodeRange);
      _currentFwTerm.getFroms().add(from);
    }
  }

  @Override
  public void exitFftf_icmp_type(Fftf_icmp_typeContext ctx) {
    if (_currentFirewallFamily == Family.INET6) {
      // TODO: support icmpv6
      return;
    }
    SubRange icmpTypeRange;
    if (ctx.subrange() != null) {
      icmpTypeRange = toSubRange(ctx.subrange());
    } else if (ctx.icmp_type() != null) {
      int icmpType = toIcmpType(ctx.icmp_type());
      icmpTypeRange = new SubRange(icmpType, icmpType);
    } else {
      throw new BatfishException("Invalid icmp-type");
    }
    FwFrom from = new FwFromIcmpType(icmpTypeRange);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_ip_protocol(Fftf_ip_protocolContext ctx) {
    IpProtocol protocol = toIpProtocol(ctx.ip_protocol());
    FwFrom from = new FwFromProtocol(protocol);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_is_fragment(Fftf_is_fragmentContext ctx) {
    SubRange subRange = new SubRange(0, 0);
    FwFrom from = new FwFromFragmentOffset(subRange, true);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_packet_length(Fftf_packet_lengthContext ctx) {
    List<SubRange> range = toRange(ctx.range());
    FwFrom from = new FwFromPacketLength(range, false);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_packet_length_except(Fftf_packet_length_exceptContext ctx) {
    List<SubRange> range = toRange(ctx.range());
    FwFrom from = new FwFromPacketLength(range, true);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_port(Fftf_portContext ctx) {
    if (ctx.port() != null) {
      int port = getPortNumber(ctx.port());
      SubRange subrange = new SubRange(port, port);
      FwFrom from = new FwFromPort(subrange);
      _currentFwTerm.getFroms().add(from);
    } else if (ctx.range() != null) {
      for (SubrangeContext subrangeContext : ctx.range().range_list) {
        SubRange subrange = toSubRange(subrangeContext);
        FwFrom from = new FwFromPort(subrange);
        _currentFwTerm.getFroms().add(from);
      }
    }
  }

  @Override
  public void exitFftf_prefix_list(Fftf_prefix_listContext ctx) {
    String name = ctx.name.getText();
    // temporary
    if (_currentFilter.getFamily() != Family.INET) {
      _configuration.getIgnoredPrefixLists().add(name);
    }
    FwFromPrefixList from = new FwFromPrefixList(name);
    _currentFwTerm.getFroms().add(from);
    _configuration.referenceStructure(
        PREFIX_LIST, name, FIREWALL_FILTER_PREFIX_LIST, getLine(ctx.name.start));
  }

  @Override
  public void exitFftf_protocol(Fftf_protocolContext ctx) {
    IpProtocol protocol = toIpProtocol(ctx.ip_protocol());
    FwFrom from = new FwFromProtocol(protocol);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_source_address(Fftf_source_addressContext ctx) {
    FwFrom from;
    IpWildcard ipWildcard = formIpWildCard(ctx.fftfa_address_mask_prefix());
    if (ipWildcard != null) {
      from =
          ctx.EXCEPT() != null
              ? new FwFromSourceAddressExcept(ipWildcard)
              : new FwFromSourceAddress(ipWildcard);
      _currentFwTerm.getFroms().add(from);
    }
  }

  @Override
  public void exitFftf_source_port(Fftf_source_portContext ctx) {
    if (ctx.port() != null) {
      int port = getPortNumber(ctx.port());
      SubRange subrange = new SubRange(port, port);
      FwFrom from = new FwFromSourcePort(subrange);
      _currentFwTerm.getFroms().add(from);
    } else if (ctx.range() != null) {
      for (SubrangeContext subrangeContext : ctx.range().range_list) {
        SubRange subrange = toSubRange(subrangeContext);
        FwFrom from = new FwFromSourcePort(subrange);
        _currentFwTerm.getFroms().add(from);
      }
    }
  }

  @Override
  public void exitFftf_source_prefix_list(Fftf_source_prefix_listContext ctx) {
    String name = ctx.name.getText();
    // temporary
    if (_currentFilter.getFamily() != Family.INET) {
      _configuration.getIgnoredPrefixLists().add(name);
    }
    FwFrom from;
    if (ctx.EXCEPT() != null) {
      from = new FwFromSourcePrefixListExcept(name);
    } else {
      from = new FwFromSourcePrefixList(name);
    }
    _currentFwTerm.getFroms().add(from);
    _configuration.referenceStructure(
        PREFIX_LIST, name, FIREWALL_FILTER_SOURCE_PREFIX_LIST, getLine(ctx.name.start));
  }

  @Override
  public void exitFftf_tcp_established(Fftf_tcp_establishedContext ctx) {
    List<TcpFlags> tcpFlags = new ArrayList<>();
    TcpFlags alt1 = new TcpFlags();
    alt1.setUseAck(true);
    alt1.setAck(true);
    tcpFlags.add(alt1);
    TcpFlags alt2 = new TcpFlags();
    alt2.setUseRst(true);
    alt2.setRst(true);
    tcpFlags.add(alt2);
    FwFrom from = new FwFromTcpFlags(tcpFlags);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_tcp_flags(Fftf_tcp_flagsContext ctx) {
    List<TcpFlags> tcpFlags = toTcpFlags(ctx.tcp_flags());
    FwFrom from = new FwFromTcpFlags(tcpFlags);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftf_tcp_initial(Fftf_tcp_initialContext ctx) {
    List<TcpFlags> tcpFlags = new ArrayList<>();
    TcpFlags alt1 = new TcpFlags();
    alt1.setUseAck(true);
    alt1.setAck(false);
    alt1.setUseSyn(true);
    alt1.setSyn(true);
    tcpFlags.add(alt1);
    FwFrom from = new FwFromTcpFlags(tcpFlags);
    _currentFwTerm.getFroms().add(from);
  }

  @Override
  public void exitFftt_accept(Fftt_acceptContext ctx) {
    _currentFwTerm.getThens().add(FwThenAccept.INSTANCE);
  }

  @Override
  public void exitFftt_discard(Fftt_discardContext ctx) {
    _currentFwTerm.getThens().add(FwThenDiscard.INSTANCE);
  }

  @Override
  public void exitFftt_next_ip(Fftt_next_ipContext ctx) {
    Prefix nextPrefix;
    if (ctx.ip != null) {
      Ip nextIp = new Ip(ctx.ip.getText());
      nextPrefix = new Prefix(nextIp, Prefix.MAX_PREFIX_LENGTH);
    } else {
      nextPrefix = Prefix.parse(ctx.prefix.getText());
    }
    FwThenNextIp then = new FwThenNextIp(nextPrefix);
    _currentFwTerm.getThens().add(then);
    _currentFwTerm.getThens().add(FwThenAccept.INSTANCE);
    _currentFilter.setRoutingPolicy(true);
  }

  @Override
  public void exitFftt_next_term(Fftt_next_termContext ctx) {
    _currentFwTerm.getThens().add(FwThenNextTerm.INSTANCE);
  }

  @Override
  public void exitFftt_nop(Fftt_nopContext ctx) {
    _currentFwTerm.getThens().add(FwThenNop.INSTANCE);
  }

  @Override
  public void exitFftt_reject(Fftt_rejectContext ctx) {
    _currentFwTerm.getThens().add(FwThenDiscard.INSTANCE);
  }

  @Override
  public void exitFftt_routing_instance(Fftt_routing_instanceContext ctx) {
    // TODO: implement
    _w.unimplemented(ConfigurationBuilder.F_FIREWALL_TERM_THEN_ROUTING_INSTANCE);
    _currentFwTerm.getThens().add(FwThenDiscard.INSTANCE);
  }

  @Override
  public void exitFlat_juniper_configuration(Flat_juniper_configurationContext ctx) {
    if (_hasZones) {
      if (_defaultCrossZoneAction == null) {
        _defaultCrossZoneAction = LineAction.REJECT;
      }
      _configuration.setDefaultCrossZoneAction(_defaultCrossZoneAction);
      _configuration.setDefaultInboundAction(LineAction.REJECT);
    } else {
      _configuration.setDefaultInboundAction(LineAction.ACCEPT);
    }
  }

  @Override
  public void exitFo_dhcp_relay(Fo_dhcp_relayContext ctx) {
    _currentDhcpRelayGroup = null;
  }

  @Override
  public void exitFod_active_server_group(Fod_active_server_groupContext ctx) {
    String name = ctx.name.getText();
    _currentDhcpRelayGroup.setActiveServerGroup(name);
    _configuration.referenceStructure(
        DHCP_RELAY_SERVER_GROUP,
        name,
        DHCP_RELAY_GROUP_ACTIVE_SERVER_GROUP,
        getLine(ctx.name.getStart()));
  }

  @Override
  public void exitFod_group(Fod_groupContext ctx) {
    _currentDhcpRelayGroup = null;
  }

  @Override
  public void exitFodg_interface(Fodg_interfaceContext ctx) {
    if (ctx.ALL() != null) {
      _currentDhcpRelayGroup.setAllInterfaces(true);
    } else {
      Interface iface = initInterface(ctx.interface_id());
      String interfaceName = iface.getName();
      _currentDhcpRelayGroup.getInterfaces().add(interfaceName);
    }
  }

  @Override
  public void exitI_description(I_descriptionContext ctx) {
    String text = unquote(ctx.description().text.getText());
    _currentInterface.setDescription(text);
  }

  @Override
  public void exitI_disable(I_disableContext ctx) {
    _currentInterface.setActive(false);
  }

  @Override
  public void exitI_enable(I_enableContext ctx) {
    _currentInterface.setActive(true);
  }

  @Override
  public void exitI_mtu(I_mtuContext ctx) {
    int size = toInt(ctx.size);
    _currentInterface.setMtu(size);
  }

  @Override
  public void exitI_unit(I_unitContext ctx) {
    _currentInterface = _currentMasterInterface;
  }

  @Override
  public void exitIfe_filter(Ife_filterContext ctx) {
    FilterContext filter = ctx.filter();
    String name = filter.name.getText();
    int line = getLine(filter.name.getStart());
    _configuration.referenceStructure(FIREWALL_FILTER, name, INTERFACE_FILTER, line);
  }

  @Override
  public void exitIfe_port_mode(Ife_port_modeContext ctx) {
    if (ctx.TRUNK() != null) {
      _currentInterface.setSwitchportMode(SwitchportMode.TRUNK);
    }
  }

  @Override
  public void exitIfe_vlan(Ife_vlanContext ctx) {
    if (_currentInterface.getSwitchportMode() == SwitchportMode.TRUNK) {
      if (ctx.range() != null) {
        List<SubRange> subRanges = toRange(ctx.range());
        subRanges.forEach(subRange -> _currentInterface.getAllowedVlans().add(subRange));
      }
    } else if (ctx.name != null) {
      // SwitchPortMode here can be ACCESS or NONE, overwrite both with ACCESS(considered default)
      _currentInterface.setSwitchportMode(SwitchportMode.ACCESS);
      String name = ctx.name.getText();
      _currentInterface.setAccessVlan(name);
      _configuration.referenceStructure(VLAN, name, INTERFACE_VLAN, getLine(ctx.name.getStart()));
    }
  }

  @Override
  public void exitIfi_address(Ifi_addressContext ctx) {
    _currentInterfaceAddress = null;
  }

  @Override
  public void exitIfi_filter(Ifi_filterContext ctx) {
    FilterContext filter = ctx.filter();
    String name = filter.name.getText();
    JuniperStructureUsage usage = INTERFACE_FILTER;
    if (filter.direction() != null) {
      DirectionContext direction = filter.direction();
      if (direction.INPUT() != null) {
        _currentInterface.setIncomingFilter(name);
        usage = INTERFACE_INCOMING_FILTER;
      } else if (direction.OUTPUT() != null) {
        _currentInterface.setOutgoingFilter(name);
        usage = INTERFACE_OUTGOING_FILTER;
      }
    }
    _configuration.referenceStructure(
        FIREWALL_FILTER, name, usage, getLine(filter.name.getStart()));
  }

  @Override
  public void exitIfia_arp(Ifia_arpContext ctx) {
    Ip ip = new Ip(ctx.ip.getText());
    _currentInterface.setAdditionalArpIps(
        ImmutableSet.<Ip>builder().addAll(_currentInterface.getAdditionalArpIps()).add(ip).build());
  }

  @Override
  public void exitIfia_preferred(Ifia_preferredContext ctx) {
    _currentInterface.setPreferredAddress(_currentInterfaceAddress);
  }

  @Override
  public void exitIfia_primary(Ifia_primaryContext ctx) {
    _currentInterface.setPrimaryAddress(_currentInterfaceAddress);
  }

  @Override
  public void exitIfia_vrrp_group(Ifia_vrrp_groupContext ctx) {
    _currentVrrpGroup = null;
  }

  @Override
  public void exitIfiav_preempt(Ifiav_preemptContext ctx) {
    _currentVrrpGroup.setPreempt(true);
  }

  @Override
  public void exitIfiav_priority(Ifiav_priorityContext ctx) {
    int priority = toInt(ctx.priority);
    _currentVrrpGroup.setPriority(priority);
  }

  @Override
  public void exitIfiav_virtual_address(Ifiav_virtual_addressContext ctx) {
    Ip virtualAddress = new Ip(ctx.IP_ADDRESS().getText());
    int prefixLength = _currentInterfaceAddress.getNetworkBits();
    _currentVrrpGroup.setVirtualAddress(new InterfaceAddress(virtualAddress, prefixLength));
  }

  @Override
  public void exitIfiso_address(Ifiso_addressContext ctx) {
    IsoAddress address = new IsoAddress(ctx.ISO_ADDRESS().getText());
    _currentInterface.setIsoAddress(address);
  }

  @Override
  public void exitInt_named(Int_namedContext ctx) {
    _currentInterface = null;
    _currentMasterInterface = null;
  }

  @Override
  public void exitIs_export(Is_exportContext ctx) {
    Set<String> policies = new LinkedHashSet<>();
    for (VariableContext policy : ctx.policies) {
      policies.add(policy.getText());
    }
    _currentRoutingInstance.getIsisSettings().getExportPolicies().addAll(policies);
    // Needs data plane implementation
    todo(ctx, F_ISIS_EXPORT);
  }

  @Override
  public void exitIs_interface(Is_interfaceContext ctx) {
    _currentIsisInterface = null;
  }

  @Override
  public void exitIs_level(Is_levelContext ctx) {
    _currentIsisLevelSettings = null;
  }

  @Override
  public void exitIs_no_ipv4_routing(Is_no_ipv4_routingContext ctx) {
    _currentRoutingInstance.getIsisSettings().setNoIpv4Routing(true);
  }

  @Override
  public void exitIso_timeout(Iso_timeoutContext ctx) {
    _currentRoutingInstance.getIsisSettings().setOverloadTimeout(toInt(ctx.DEC()));
  }

  @Override
  public void exitIs_reference_bandwidth(Is_reference_bandwidthContext ctx) {
    long referenceBandwidth = toReferenceBandwidth(ctx.reference_bandwidth());
    _currentRoutingInstance.getIsisSettings().setReferenceBandwidth((double) referenceBandwidth);
  }

  @Override
  public void exitIsi_level(Isi_levelContext ctx) {
    _currentIsisInterfaceLevelSettings = null;
  }

  @Override
  public void exitIsi_passive(Isi_passiveContext ctx) {
    _currentIsisInterface.getIsisSettings().setPassive(true);
  }

  @Override
  public void exitIsi_point_to_point(Isi_point_to_pointContext ctx) {
    _currentIsisInterface.getIsisSettings().setPointToPoint(true);
  }

  @Override
  public void exitIsib_minimum_interval(Isib_minimum_intervalContext ctx) {
    _currentIsisInterface
        .getIsisSettings()
        .setBfdLivenessDetectionMinimumInterval(toInt(ctx.DEC()));
  }

  @Override
  public void exitIsib_multiplier(Isib_multiplierContext ctx) {
    _currentIsisInterface.getIsisSettings().setBfdLivenessDetectionMultiplier(toInt(ctx.DEC()));
  }

  @Override
  public void exitIsil_enable(Isil_enableContext ctx) {
    _currentIsisInterfaceLevelSettings.setEnabled(true);
  }

  @Override
  public void exitIsil_hello_authentication_key(Isil_hello_authentication_keyContext ctx) {
    String key = unquote(ctx.key.getText());
    String decodedKeyHash = decryptIfNeededAndHash(key, getLine(ctx.key.getStart()));
    _currentIsisInterfaceLevelSettings.setHelloAuthenticationKey(decodedKeyHash);
  }

  @Override
  public void exitIsil_hello_authentication_type(Isil_hello_authentication_typeContext ctx) {
    _currentIsisInterfaceLevelSettings.setHelloAuthenticationType(
        toIsisHelloAuthenticationType(ctx.hello_authentication_type()));
  }

  @Override
  public void exitIsil_hello_interval(Isil_hello_intervalContext ctx) {
    _currentIsisInterfaceLevelSettings.setHelloInterval(toInt(ctx.DEC()));
  }

  @Override
  public void exitIsil_hold_time(Isil_hold_timeContext ctx) {
    _currentIsisInterfaceLevelSettings.setHoldTime(toInt(ctx.DEC()));
  }

  @Override
  public void exitIsil_metric(Isil_metricContext ctx) {
    int metric = toInt(ctx.DEC());
    _currentIsisInterfaceLevelSettings.setMetric(metric);
  }

  @Override
  public void exitIsil_te_metric(Isil_te_metricContext ctx) {
    int teMetric = toInt(ctx.DEC());
    _currentIsisInterfaceLevelSettings.setTeMetric(teMetric);
  }

  @Override
  public void exitIsl_disable(Isl_disableContext ctx) {
    _currentIsisLevelSettings.setEnabled(false);
  }

  @Override
  public void exitIsl_wide_metrics_only(Isl_wide_metrics_onlyContext ctx) {
    _currentIsisLevelSettings.setWideMetricsOnly(true);
  }

  @Override
  public void exitIst_credibility_protocol_preference(
      Ist_credibility_protocol_preferenceContext ctx) {
    _currentRoutingInstance
        .getIsisSettings()
        .setTrafficEngineeringCredibilityProtocolPreference(true);
  }

  @Override
  public void exitIst_family_shortcuts(Ist_family_shortcutsContext ctx) {
    if (ctx.INET6() != null) {
      todo(ctx, F_IPV6);
    } else { // ipv4
      _currentRoutingInstance.getIsisSettings().setTrafficEngineeringShortcuts(true);
    }
  }

  @Override
  public void exitO_area(O_areaContext ctx) {
    _currentArea = null;
  }

  @Override
  public void exitO_export(O_exportContext ctx) {
    String name = ctx.name.getText();
    _currentRoutingInstance.getOspfExportPolicies().add(name);
    _configuration.referenceStructure(
        POLICY_STATEMENT, name, OSPF_EXPORT_POLICY, getLine(ctx.name.getStart()));
  }

  @Override
  public void exitO_reference_bandwidth(O_reference_bandwidthContext ctx) {
    long referenceBandwidth = toReferenceBandwidth(ctx.reference_bandwidth());
    _currentRoutingInstance.setOspfReferenceBandwidth((double) referenceBandwidth);
  }

  @Override
  public void exitOa_area_range(FlatJuniperParser.Oa_area_rangeContext ctx) {
    if (_currentAreaRangePrefix != null) {
      OspfAreaSummary summary =
          new OspfAreaSummary(!_currentAreaRangeRestrict, _currentAreaRangeMetric);
      Map<Prefix, OspfAreaSummary> summaries = _currentArea.getSummaries();
      summaries.put(_currentAreaRangePrefix, summary);
    } else {
      todo(ctx, F_IPV6);
    }
  }

  @Override
  public void exitOa_interface(Oa_interfaceContext ctx) {
    _currentOspfInterface = null;
  }

  @Override
  public void exitOaa_override_metric(FlatJuniperParser.Oaa_override_metricContext ctx) {
    _currentAreaRangeMetric = Long.parseLong(ctx.DEC().getText());
  }

  @Override
  public void exitOaa_restrict(FlatJuniperParser.Oaa_restrictContext ctx) {
    _currentAreaRangeRestrict = true;
  }

  @Override
  public void exitOai_interface_type(Oai_interface_typeContext ctx) {
    if (ctx.P2P() != null) {
      _currentOspfInterface.setOspfPointToPoint(true);
    }
  }

  @Override
  public void exitOai_metric(FlatJuniperParser.Oai_metricContext ctx) {
    int ospfCost = toInt(ctx.DEC());
    _currentOspfInterface.setOspfCost(ospfCost);
  }

  @Override
  public void exitP_bgp(P_bgpContext ctx) {
    _currentBgpGroup = null;
  }

  @Override
  public void exitPo_community(Po_communityContext ctx) {
    _currentCommunityList = null;
  }

  @Override
  public void exitPo_policy_statement(Po_policy_statementContext ctx) {
    _currentPolicyStatement = null;
  }

  @Override
  public void exitPo_prefix_list(Po_prefix_listContext ctx) {
    _currentPrefixList = null;
  }

  @Override
  public void exitPoc_invert_match(Poc_invert_matchContext ctx) {
    _currentCommunityList.setInvertMatch(true);
  }

  @Override
  public void exitPoc_members(Poc_membersContext ctx) {
    if (ctx.community_regex() != null) {
      String text = ctx.community_regex().getText();
      _currentCommunityList.getLines().add(new CommunityListLine(text));
      if (text.matches("[0-9]+:[0-9]+")) {
        long communityVal = CommonUtil.communityStringToLong(text);
        _configuration.getAllStandardCommunities().add(communityVal);
      }
    } else if (ctx.extended_community_regex() != null) {
      String text = ctx.extended_community_regex().getText();
      _currentCommunityList.getLines().add(new CommunityListLine(text));
    } else if (ctx.standard_community() != null) {
      long communityVal = toCommunityLong(ctx.standard_community());
      _configuration.getAllStandardCommunities().add(communityVal);
      String communityStr = CommonUtil.longToCommunity(communityVal);
      _currentCommunityList.getLines().add(new CommunityListLine(communityStr));
    } else if (ctx.extended_community() != null) {
      long communityVal = toCommunityLong(ctx.extended_community());
      String communityStr = CommonUtil.longToCommunity(communityVal);
      _currentCommunityList.getLines().add(new CommunityListLine(communityStr));
    } else if (ctx.invalid_community_regex() != null) {
      String text = ctx.invalid_community_regex().getText();
      _currentCommunityList.getLines().add(new CommunityListLine(text));
      _w.redFlag(
          getLine(ctx.getStart())
              + ": In community-list '"
              + _currentCommunityList.getName()
              + "': Invalid or unsupported community regex: '"
              + text
              + "'");
    }
  }

  @Override
  public void exitPoplt_ip6(Poplt_ip6Context ctx) {
    _currentPrefixList.setIpv6(true);
    todo(ctx, F_IPV6);
  }

  @Override
  public void exitPoplt_network(Poplt_networkContext ctx) {
    Prefix prefix = Prefix.parse(ctx.network.getText());
    _currentPrefixList.getPrefixes().add(prefix);
  }

  @Override
  public void exitPoplt_network6(Poplt_network6Context ctx) {
    _currentPrefixList.setIpv6(true);
    todo(ctx, F_IPV6);
  }

  @Override
  public void exitPops_common(Pops_commonContext ctx) {
    _currentPsTerm = null;
    _currentPsThens = null;
  }

  @Override
  public void exitPopsf_as_path(Popsf_as_pathContext ctx) {
    String name = ctx.name.getText();
    PsFromAsPath fromAsPath = new PsFromAsPath(name);
    _currentPsTerm.getFroms().add(fromAsPath);
  }

  @Override
  public void exitPopsf_as_path_group(Popsf_as_path_groupContext ctx) {
    _w.redFlag(
        String.format(
            "unimplemented 'policy-options policy-statement term' from clause: %s",
            getFullText(ctx)));
  }

  @Override
  public void exitPopsf_color(Popsf_colorContext ctx) {
    int color = toInt(ctx.color);
    PsFromColor fromColor = new PsFromColor(color);
    _currentPsTerm.getFroms().add(fromColor);
  }

  @Override
  public void exitPopsf_community(Popsf_communityContext ctx) {
    String name = ctx.name.getText();
    PsFromCommunity fromCommunity = new PsFromCommunity(name);
    _currentPsTerm.getFroms().add(fromCommunity);
  }

  @Override
  public void exitPopsf_family(Popsf_familyContext ctx) {
    PsFrom from;
    if (ctx.INET() != null) {
      from = new PsFromFamilyInet();
    } else if (ctx.INET6() != null) {
      from = new PsFromFamilyInet6();
    } else {
      throw new BatfishException("Unsupported family: " + ctx.getText());
    }
    _currentPsTerm.getFroms().add(from);
  }

  @Override
  public void exitPopsf_instance(Popsf_instanceContext ctx) {
    _w.redFlag(
        String.format(
            "unimplemented 'policy-options policy-statement term' from clause: %s",
            getFullText(ctx)));
  }

  @Override
  public void exitPopsf_interface(Popsf_interfaceContext ctx) {
    String name = getInterfaceName(ctx.id);
    int definitionLine = getLine(ctx.id.name);
    String unit = null;
    if (ctx.id.unit != null) {
      unit = ctx.id.unit.getText();
      definitionLine = getLine(ctx.id.unit);
    }
    String unitFullName = name + "." + unit;
    Map<String, Interface> interfaces = _configuration.getInterfaces();
    Interface iface = interfaces.get(name);
    if (iface == null) {
      iface = new Interface(name, definitionLine);
      iface.setRoutingInstance(_currentRoutingInstance.getName());
      interfaces.put(name, iface);
    }
    PsFrom from;
    if (unit != null) {
      Map<String, Interface> units = iface.getUnits();
      iface = units.get(unitFullName);
      if (iface == null) {
        iface = new Interface(unitFullName, definitionLine);
        iface.setRoutingInstance(_currentRoutingInstance.getName());
        units.put(unitFullName, iface);
      }
      from = new PsFromInterface(unitFullName);
    } else {
      from = new PsFromInterface(name);
    }
    _currentPsTerm.getFroms().add(from);
  }

  @Override
  public void exitPopsf_local_preference(Popsf_local_preferenceContext ctx) {
    int localPreference = toInt(ctx.localpref);
    PsFromLocalPreference fromLocalPreference = new PsFromLocalPreference(localPreference);
    _currentPsTerm.getFroms().add(fromLocalPreference);
  }

  @Override
  public void exitPopsf_metric(Popsf_metricContext ctx) {
    int metric = toInt(ctx.metric);
    PsFromMetric fromMetric = new PsFromMetric(metric);
    _currentPsTerm.getFroms().add(fromMetric);
  }

  @Override
  public void exitPopsf_policy(Popsf_policyContext ctx) {
    String policyName = toComplexPolicyStatement(ctx.policy_expression(), POLICY_STATEMENT_POLICY);
    PsFrom from = new PsFromPolicyStatement(policyName);
    _currentPsTerm.getFroms().add(from);
  }

  @Override
  public void exitPopsf_prefix_list(Popsf_prefix_listContext ctx) {
    String name = ctx.name.getText();
    PsFrom from = new PsFromPrefixList(name);
    _currentPsTerm.getFroms().add(from);
    _configuration.referenceStructure(
        PREFIX_LIST, name, POLICY_STATEMENT_PREFIX_LIST, getLine(ctx.name.start));
  }

  @Override
  public void exitPopsf_prefix_list_filter(Popsf_prefix_list_filterContext ctx) {
    String name = ctx.name.getText();
    PsFrom from;
    if (ctx.popsfpl_exact() != null) {
      from = new PsFromPrefixList(name);
    } else if (ctx.popsfpl_longer() != null) {
      from = new PsFromPrefixListFilterLonger(name);
    } else if (ctx.popsfpl_orlonger() != null) {
      from = new PsFromPrefixListFilterOrLonger(name);
    } else {
      throw new BatfishException("Invalid prefix-list-filter length specification");
    }
    _currentPsTerm.getFroms().add(from);
    _configuration.referenceStructure(
        PREFIX_LIST, name, POLICY_STATEMENT_PREFIX_LIST_FILTER, getLine(ctx.name.start));
  }

  @Override
  public void exitPopsf_protocol(Popsf_protocolContext ctx) {
    RoutingProtocol protocol = toRoutingProtocol(ctx.protocol);
    PsFromProtocol fromProtocol = new PsFromProtocol(protocol);
    _currentPsTerm.getFroms().add(fromProtocol);
  }

  @Override
  public void exitPopsf_rib(Popsf_ribContext ctx) {
    _w.redFlag(
        String.format(
            "unimplemented 'policy-options policy-statement term' from clause: %s",
            getFullText(ctx)));
  }

  @Override
  public void exitPopsf_route_filter(Popsf_route_filterContext ctx) {
    _currentRouteFilterPrefix = null;
    _currentRoute6FilterPrefix = null;
    _currentRouteFilter = null;
    _currentRouteFilterLine = null;
    _currentRoute6FilterLine = null;
  }

  @Override
  public void exitPopsf_tag(Popsf_tagContext ctx) {
    int tag = toInt(ctx.DEC());
    _currentPsTerm.getFroms().add(new PsFromTag(tag));
  }

  @Override
  public void exitPopsfrf_then(Popsfrf_thenContext ctx) {
    _currentPsThens = _currentPsTerm.getThens();
  }

  @Override
  public void exitPopst_accept(Popst_acceptContext ctx) {
    _currentPsThens.add(PsThenAccept.INSTANCE);
  }

  @Override
  public void exitPopst_as_path_prepend(Popst_as_path_prependContext ctx) {
    List<Long> asPaths = new LinkedList<>();
    if (ctx.DEC() != null) {
      asPaths.add(toLong(ctx.DEC()));
    } else if (ctx.DOUBLE_QUOTED_STRING() != null) {
      String[] unquoted = unquote(ctx.DOUBLE_QUOTED_STRING().getText()).split("\\s+");
      Arrays.stream(unquoted).map(Long::parseLong).forEach(asPaths::add);
    } else {
      _w.redFlag(
          String.format(
              "unimplemented 'policy-options policy-statement term' then clause: %s",
              getFullText(ctx)));
      return;
    }
    _currentPsThens.add(new PsThenAsPathPrepend(asPaths));
  }

  @Override
  public void exitPopst_community_add(Popst_community_addContext ctx) {
    String name = ctx.name.getText();
    PsThenCommunityAdd then = new PsThenCommunityAdd(name, _configuration);
    _currentPsThens.add(then);
  }

  @Override
  public void exitPopst_community_delete(Popst_community_deleteContext ctx) {
    String name = ctx.name.getText();
    PsThenCommunityDelete then = new PsThenCommunityDelete(name, _configuration);
    _currentPsThens.add(then);
  }

  @Override
  public void exitPopst_community_set(Popst_community_setContext ctx) {
    String name = ctx.name.getText();
    PsThenCommunitySet then = new PsThenCommunitySet(name, _configuration);
    _currentPsThens.add(then);
  }

  @Override
  public void exitPopst_default_action_accept(Popst_default_action_acceptContext ctx) {
    _currentPsThens.add(new PsThenDefaultActionAccept());
  }

  @Override
  public void exitPopst_default_action_reject(Popst_default_action_rejectContext ctx) {
    _currentPsThens.add(new PsThenDefaultActionReject());
  }

  @Override
  public void exitPopst_external(Popst_externalContext ctx) {
    int type = toInt(ctx.DEC());
    if (type == 1) {
      _currentPsThens.add(new PsThenExternal(OspfMetricType.E1));
    } else if (type == 2) {
      _currentPsThens.add(new PsThenExternal(OspfMetricType.E2));
    } else {
      _w.redFlag(String.format("unimplemented: then %s", getFullText(ctx)));
    }
  }

  @Override
  public void exitPopst_local_preference(Popst_local_preferenceContext ctx) {
    int localPreference = toInt(ctx.localpref);
    PsThenLocalPreference then = new PsThenLocalPreference(localPreference);
    _currentPsThens.add(then);
  }

  @Override
  public void exitPopst_metric(Popst_metricContext ctx) {
    int metric = toInt(ctx.metric);
    PsThenMetric then = new PsThenMetric(metric);
    _currentPsThens.add(then);
  }

  @Override
  public void exitPopst_metric_add(Popst_metric_addContext ctx) {
    int metric = toInt(ctx.metric);
    _currentPsThens.add(new PsThenMetricAdd(metric));
  }

  @Override
  public void exitPopst_next_hop(Popst_next_hopContext ctx) {
    PsThen then;
    if (ctx.IP_ADDRESS() != null) {
      Ip nextHopIp = new Ip(ctx.IP_ADDRESS().getText());
      then = new PsThenNextHopIp(nextHopIp);
    } else {
      todo(ctx, F_POLICY_TERM_THEN_NEXT_HOP);
      return;
    }
    _currentPsThens.add(then);
  }

  @Override
  public void exitPopst_next_hop_self(Popst_next_hop_selfContext ctx) {
    _currentPsThens.add(PsThenNextHopSelf.INSTANCE);
  }

  @Override
  public void exitPopst_next_policy(Popst_next_policyContext ctx) {
    _currentPsThens.add(PsThenNextPolicy.INSTANCE);
  }

  @Override
  public void exitPopst_next_term(Popst_next_termContext ctx) {
    // The next-term action itself is a no-op in Batfish, so we do not model this behavior.
    //
    // TODO(https://github.com/batfish/batfish/issues/1551): need to implement next_term replacing
    // any existing flow control action.
  }

  @Override
  public void exitPopst_origin(Popst_originContext ctx) {
    OriginType origin;
    if (ctx.EGP() != null) {
      origin = OriginType.EGP;
    } else if (ctx.IGP() != null) {
      origin = OriginType.IGP;
    } else if (ctx.INCOMPLETE() != null) {
      origin = OriginType.INCOMPLETE;
    } else {
      _w.redFlag(String.format("unimplemented origin type: %s", getFullText(ctx)));
      return;
    }
    _currentPsThens.add(new PsThenOrigin(origin));
  }

  @Override
  public void exitPopst_preference(Popst_preferenceContext ctx) {
    int preference = toInt(ctx.preference);
    PsThenPreference then = new PsThenPreference(preference);
    _currentPsThens.add(then);
  }

  @Override
  public void exitPopst_reject(Popst_rejectContext ctx) {
    _currentPsThens.add(PsThenReject.INSTANCE);
  }

  @Override
  public void exitRi_interface(Ri_interfaceContext ctx) {
    Interface iface = initInterface(ctx.id);
    iface.setRoutingInstance(_currentRoutingInstance.getName());
    defineStructure(INTERFACE, iface.getName(), ctx);
  }

  @Override
  public void exitRi_named_routing_instance(Ri_named_routing_instanceContext ctx) {
    _currentRoutingInstance = _configuration.getDefaultRoutingInstance();
  }

  @Override
  public void exitRo_aggregate(Ro_aggregateContext ctx) {
    _currentAggregateRoute = null;
  }

  @Override
  public void exitRo_autonomous_system(Ro_autonomous_systemContext ctx) {
    if (ctx.as != null) {
      int as = toInt(ctx.as);
      _currentRoutingInstance.setAs(as);
    }
  }

  @Override
  public void exitRo_generate(Ro_generateContext ctx) {
    _currentGeneratedRoute = null;
  }

  @Override
  public void exitRo_router_id(Ro_router_idContext ctx) {
    Ip id = new Ip(ctx.id.getText());
    _currentRoutingInstance.setRouterId(id);
  }

  @Override
  public void exitRo_static(Ro_staticContext ctx) {
    _currentStaticRoute = null;
  }

  @Override
  public void exitRoa_community(Roa_communityContext ctx) {
    long community = CommonUtil.communityStringToLong(ctx.COMMUNITY_LITERAL().getText());
    _configuration.getAllStandardCommunities().add(community);
    _currentAggregateRoute.getCommunities().add(community);
  }

  @Override
  public void exitRoa_preference(Roa_preferenceContext ctx) {
    int preference = toInt(ctx.preference);
    _currentAggregateRoute.setPreference(preference);
  }

  @Override
  public void exitRoa_tag(Roa_tagContext ctx) {
    int tag = toInt(ctx.tag);
    _currentAggregateRoute.setTag(tag);
  }

  @Override
  public void exitRoaa_path(Roaa_pathContext ctx) {
    AsPath asPath = toAsPath(ctx.path);
    _currentAggregateRoute.setAsPath(asPath);
  }

  @Override
  public void exitRof_export(Rof_exportContext ctx) {
    String name = ctx.name.getText();
    _configuration.getDefaultRoutingInstance().setForwardingTableExportPolicy(name);
    _configuration.referenceStructure(
        POLICY_STATEMENT, name, FORWARDING_TABLE_EXPORT_POLICY, getLine(ctx.name.getStart()));
  }

  @Override
  public void exitRog_metric(Rog_metricContext ctx) {
    int metric = toInt(ctx.metric);
    _currentGeneratedRoute.setMetric(metric);
  }

  @Override
  public void exitRog_policy(Rog_policyContext ctx) {
    if (_currentGeneratedRoute != null) { // not ipv6
      String policy = ctx.policy.getText();
      _configuration.referenceStructure(
          POLICY_STATEMENT, policy, GENERATED_ROUTE_POLICY, getLine(ctx.policy.getStart()));
      _currentGeneratedRoute.getPolicies().add(policy);
    }
  }

  @Override
  public void enterRoifie_lan(Roifie_lanContext ctx) {
    _currentRoutingInstance.setExportLocalRoutesLan(true);
  }

  @Override
  public void exitRoifie_point_to_point(Roifie_point_to_pointContext ctx) {
    _currentRoutingInstance.setExportLocalRoutesPointToPoint(true);
  }

  @Override
  public void exitRosr_discard(Rosr_discardContext ctx) {
    _currentStaticRoute.setDrop(true);
  }

  @Override
  public void exitRosr_metric(Rosr_metricContext ctx) {
    int metric = toInt(ctx.metric);
    _currentStaticRoute.setMetric(metric);
  }

  @Override
  public void exitRosr_next_hop(Rosr_next_hopContext ctx) {
    if (ctx.IP_ADDRESS() != null) {
      Ip nextHopIp = new Ip(ctx.IP_ADDRESS().getText());
      _currentStaticRoute.setNextHopIp(nextHopIp);
    } else if (ctx.interface_id() != null) {
      initInterface(ctx.interface_id());
      _currentStaticRoute.setNextHopInterface(ctx.interface_id().getText());
    }
  }

  @Override
  public void exitRosr_no_install(Rosr_no_installContext ctx) {
    _currentStaticRoute.setNoInstall(ctx.NO_INSTALL() != null);
  }

  @Override
  public void exitRosr_preference(Rosr_preferenceContext ctx) {
    _currentStaticRoute.setDistance(toInt(ctx.pref));
  }

  @Override
  public void exitRosr_reject(Rosr_rejectContext ctx) {
    _currentStaticRoute.setDrop(true);
  }

  @Override
  public void exitRosr_tag(Rosr_tagContext ctx) {
    int tag = toInt(ctx.tag);
    _currentStaticRoute.setTag(tag);
  }

  @Override
  public void exitS_firewall(S_firewallContext ctx) {
    _currentFirewallFamily = null;
  }

  @Override
  public void exitS_routing_options(S_routing_optionsContext ctx) {
    _currentRib = null;
  }

  @Override
  public void exitS_snmp(S_snmpContext ctx) {
    _currentSnmpServer = null;
  }

  @Override
  public void exitS_vlans_named(S_vlans_namedContext ctx) {
    _currentVlanName = null;
  }

  @Override
  public void exitSe_zones(Se_zonesContext ctx) {
    _hasZones = true;
  }

  @Override
  public void exitSea_tolerance(Sea_toleranceContext ctx) {
    _currentAuthenticationKeyChain.setTolerance(toInt(ctx.DEC()));
  }

  @Override
  public void exitSeak_algorithm(Seak_algorithmContext ctx) {
    _currentAuthenticationKey.setIsisAuthenticationAlgorithm(
        ctx.HMAC_SHA1() != null
            ? IsisAuthenticationAlgorithm.HMAC_SHA_1
            : IsisAuthenticationAlgorithm.MD5);
  }

  @Override
  public void exitSeak_options(Seak_optionsContext ctx) {
    _currentAuthenticationKey.setIsisOption(
        ctx.ISIS_ENHANCED() != null ? IsisOption.ISIS_ENHANCED : IsisOption.BASIC);
  }

  @Override
  public void exitSeak_secret(Seak_secretContext ctx) {
    _currentAuthenticationKey.setSecret(ctx.key.getText());
  }

  @Override
  public void exitSeak_start_time(Seak_start_timeContext ctx) {
    _currentAuthenticationKey.setStartTime(ctx.time.getText());
  }

  @Override
  public void exitSeik_gateway(Seik_gatewayContext ctx) {
    _currentIkeGateway = null;
  }

  @Override
  public void exitSeik_policy(Seik_policyContext ctx) {
    _currentIkePolicy = null;
  }

  @Override
  public void exitSeik_proposal(Seik_proposalContext ctx) {
    _currentIkeProposal = null;
  }

  @Override
  public void exitSeikg_address(Seikg_addressContext ctx) {
    Ip ip = new Ip(ctx.IP_ADDRESS().getText());
    _currentIkeGateway.setAddress(ip);
  }

  @Override
  public void exitSeikg_external_interface(Seikg_external_interfaceContext ctx) {
    Interface_idContext interfaceId = ctx.interface_id();
    Interface iface = initInterface(interfaceId);
    _currentIkeGateway.setExternalInterface(iface);
    _configuration.referenceStructure(
        INTERFACE,
        iface.getName(),
        IKE_GATEWAY_EXTERNAL_INTERFACE,
        getLine(ctx.interface_id().getStart()));
  }

  @Override
  public void exitSeikg_ike_policy(Seikg_ike_policyContext ctx) {
    String name = ctx.name.getText();
    _currentIkeGateway.setIkePolicy(name);
    _configuration.referenceStructure(
        IKE_POLICY, name, IKE_GATEWAY_IKE_POLICY, getLine(ctx.name.getStart()));
  }

  @Override
  public void exitSeikg_local_address(Seikg_local_addressContext ctx) {
    Ip ip = new Ip(ctx.IP_ADDRESS().getText());
    _currentIkeGateway.setLocalAddress(ip);
  }

  @Override
  public void exitSeikp_pre_shared_key(Seikp_pre_shared_keyContext ctx) {
    String key = unquote(ctx.key.getText());
    String decodedKeyHash = decryptIfNeededAndHash(key, getLine(ctx.key));
    _currentIkePolicy.setPreSharedKeyHash(decodedKeyHash);
  }

  @Override
  public void exitSeikp_proposal_set(Seikp_proposal_setContext ctx) {
    Set<String> proposalsInSet = initIkeProposalSet(ctx.proposal_set_type());
    for (String proposal : proposalsInSet) {
      _currentIkePolicy.getProposals().add(proposal);
    }
  }

  @Override
  public void exitSeikp_proposals(Seikp_proposalsContext ctx) {
    for (VariableContext proposal : ctx.proposals) {
      String name = proposal.getText();
      _currentIkePolicy.getProposals().add(name);
      _configuration.referenceStructure(
          IKE_PROPOSAL, name, IKE_POLICY_IKE_PROPOSAL, getLine(proposal.getStart()));
    }
  }

  @Override
  public void exitSeikpr_authentication_algorithm(Seikpr_authentication_algorithmContext ctx) {
    IkeHashingAlgorithm alg = toIkeHashingAlgorithm(ctx.ike_authentication_algorithm());
    _currentIkeProposal.setAuthenticationAlgorithm(alg);
  }

  @Override
  public void exitSeikpr_authentication_method(Seikpr_authentication_methodContext ctx) {
    IkeAuthenticationMethod authenticationMethod =
        toIkeAuthenticationMethod(ctx.ike_authentication_method());
    _currentIkeProposal.setAuthenticationMethod(authenticationMethod);
  }

  @Override
  public void exitSeikpr_dh_group(Seikpr_dh_groupContext ctx) {
    DiffieHellmanGroup group = toDhGroup(ctx.dh_group());
    _currentIkeProposal.setDiffieHellmanGroup(group);
  }

  @Override
  public void exitSeikpr_encryption_algorithm(Seikpr_encryption_algorithmContext ctx) {
    EncryptionAlgorithm alg = toEncryptionAlgorithm(ctx.encryption_algorithm());
    _currentIkeProposal.setEncryptionAlgorithm(alg);
  }

  @Override
  public void exitSeikpr_lifetime_seconds(Seikpr_lifetime_secondsContext ctx) {
    int lifetimeSeconds = toInt(ctx.seconds);
    _currentIkeProposal.setLifetimeSeconds(lifetimeSeconds);
  }

  @Override
  public void exitSeip_policy(Seip_policyContext ctx) {
    _currentIpsecPolicy = null;
  }

  @Override
  public void exitSeip_proposal(Seip_proposalContext ctx) {
    _currentIpsecProposal = null;
  }

  @Override
  public void exitSeip_vpn(Seip_vpnContext ctx) {
    _currentIpsecVpn = null;
  }

  @Override
  public void exitSeipp_perfect_forward_secrecy(Seipp_perfect_forward_secrecyContext ctx) {
    DiffieHellmanGroup dhGroup = toDhGroup(ctx.dh_group());
    _currentIpsecPolicy.setPfsKeyGroup(dhGroup);
  }

  @Override
  public void exitSeipp_proposal_set(Seipp_proposal_setContext ctx) {
    for (String proposal : initIpsecProposalSet(ctx.proposal_set_type())) {
      _currentIpsecPolicy.getProposals().add(proposal);
    }
  }

  @Override
  public void exitSeipp_proposals(Seipp_proposalsContext ctx) {
    for (VariableContext proposal : ctx.proposals) {
      String name = proposal.getText();
      _currentIpsecPolicy.getProposals().add(name);
      _configuration.referenceStructure(
          IPSEC_PROPOSAL, name, IPSEC_POLICY_IPSEC_PROPOSAL, getLine(proposal.getStart()));
    }
  }

  @Override
  public void exitSeippr_authentication_algorithm(Seippr_authentication_algorithmContext ctx) {
    IpsecAuthenticationAlgorithm alg =
        toIpsecAuthenticationAlgorithm(ctx.ipsec_authentication_algorithm());
    _currentIpsecProposal.setAuthenticationAlgorithm(alg);
  }

  @Override
  public void exitSeippr_encryption_algorithm(Seippr_encryption_algorithmContext ctx) {
    EncryptionAlgorithm alg = toEncryptionAlgorithm(ctx.encryption_algorithm());
    _currentIpsecProposal.setEncryptionAlgorithm(alg);
  }

  @Override
  public void exitSeippr_lifetime_kilobytes(Seippr_lifetime_kilobytesContext ctx) {
    int lifetimeKilobytes = toInt(ctx.kilobytes);
    _currentIpsecProposal.setLifetimeKilobytes(lifetimeKilobytes);
  }

  @Override
  public void exitSeippr_lifetime_seconds(Seippr_lifetime_secondsContext ctx) {
    int lifetimeSeconds = toInt(ctx.seconds);
    _currentIpsecProposal.setLifetimeSeconds(lifetimeSeconds);
  }

  @Override
  public void exitSeippr_protocol(Seippr_protocolContext ctx) {
    _currentIpsecProposal.setProtocols(toIpsecProtocol(ctx.ipsec_protocol()));
  }

  @Override
  public void exitSeipv_bind_interface(Seipv_bind_interfaceContext ctx) {
    Interface iface = initInterface(ctx.interface_id());
    _currentIpsecVpn.setBindInterface(iface);
    _configuration.referenceStructure(
        INTERFACE,
        iface.getName(),
        IPSEC_VPN_BIND_INTERFACE,
        getLine(ctx.interface_id().getStart()));
  }

  @Override
  public void exitSeipvi_gateway(Seipvi_gatewayContext ctx) {
    String name = ctx.name.getText();
    _currentIpsecVpn.setGateway(name);
    _configuration.referenceStructure(
        IKE_GATEWAY, name, IPSEC_VPN_IKE_GATEWAY, getLine(ctx.name.getStart()));
  }

  @Override
  public void exitSeipvi_ipsec_policy(Seipvi_ipsec_policyContext ctx) {
    String name = ctx.name.getText();
    _currentIpsecVpn.setIpsecPolicy(name);
    _configuration.referenceStructure(
        IPSEC_POLICY, name, IPSEC_VPN_IPSEC_POLICY, getLine(ctx.name.getStart()));
  }

  @Override
  public void exitSep_default_policy(Sep_default_policyContext ctx) {
    if (ctx.PERMIT_ALL() != null) {
      _defaultCrossZoneAction = LineAction.ACCEPT;
    } else if (ctx.DENY_ALL() != null) {
      _defaultCrossZoneAction = LineAction.REJECT;
    }
  }

  @Override
  public void exitSep_from_zone(Sep_from_zoneContext ctx) {
    _currentFilter = null;
  }

  @Override
  public void exitSepctx_policy(Sepctx_policyContext ctx) {
    _currentFwTerm = null;
  }

  @Override
  public void exitSepctxpm_application(Sepctxpm_applicationContext ctx) {
    if (ctx.junos_application() != null) {
      JunosApplication application = toJunosApplication(ctx.junos_application());
      if (!application.hasDefinition()) {
        _w.redFlag(
            String.format(
                "unimplemented pre-defined junos application: '%s'",
                ctx.junos_application().getText()));
        return;
      }
      if (application.getIpv6()) {
        _currentFwTerm.setIpv6(true);
      } else {
        FwFromJunosApplication from = new FwFromJunosApplication(application);
        _currentFwTerm.getFromApplicationSetMembers().add(from);
      }
    } else if (ctx.junos_application_set() != null) {
      JunosApplicationSet applicationSet = toJunosApplicationSet(ctx.junos_application_set());
      if (!applicationSet.hasDefinition()) {
        _w.redFlag(
            String.format(
                "unimplemented pre-defined junos application-set: '%s'",
                ctx.junos_application_set().getText()));
        return;
      }
      FwFromJunosApplicationSet from = new FwFromJunosApplicationSet(applicationSet);
      _currentFwTerm.getFromApplicationSetMembers().add(from);
    } else {
      String name;
      int line;
      name = ctx.name.getText();
      line = getLine(ctx.name.getStart());
      _configuration.referenceStructure(
          APPLICATION_OR_APPLICATION_SET, name, SECURITY_POLICY_MATCH_APPLICATION, line);
      FwFromApplicationOrApplicationSet from = new FwFromApplicationOrApplicationSet(name);
      _currentFwTerm.getFromApplicationSetMembers().add(from);
    }
  }

  @Override
  public void exitSepctxpm_destination_address(Sepctxpm_destination_addressContext ctx) {
    if (ctx.address_specifier().ANY() != null) {
      return;
    } else if (ctx.address_specifier().ANY_IPV4() != null) {
      return;
    } else if (ctx.address_specifier().ANY_IPV6() != null) {
      _currentFwTerm.setIpv6(true);
      return;
    } else if (ctx.address_specifier().variable() != null) {
      String addressBookEntryName = ctx.address_specifier().variable().getText();
      // Use global address book for global policies (no toZone)
      AddressBook addressBook =
          (_currentToZone == null) ? _globalAddressBook : _currentToZone.getAddressBook();
      FwFrom match = new FwFromDestinationAddressBookEntry(addressBook, addressBookEntryName);
      _currentFwTerm.getFroms().add(match);
    } else {
      throw new BatfishException("Invalid address-specifier");
    }
  }

  @Override
  public void exitSepctxpm_source_address(Sepctxpm_source_addressContext ctx) {
    if (ctx.address_specifier().ANY() != null) {
      return;
    } else if (ctx.address_specifier().ANY_IPV4() != null) {
      return;
    } else if (ctx.address_specifier().ANY_IPV6() != null) {
      _currentFwTerm.setIpv6(true);
      return;
    } else if (ctx.address_specifier().variable() != null) {
      String addressBookEntryName = ctx.address_specifier().variable().getText();
      // Use global address book for global policies (no fromZone)
      AddressBook addressBook =
          (_currentFromZone == null) ? _globalAddressBook : _currentFromZone.getAddressBook();
      FwFrom match = new FwFromSourceAddressBookEntry(addressBook, addressBookEntryName);
      _currentFwTerm.getFroms().add(match);
    } else {
      throw new BatfishException("Invalid address-specifier");
    }
  }

  @Override
  public void exitSepctxpm_source_identity(Sepctxpm_source_identityContext ctx) {
    if (ctx.ANY() != null) {
      return;
    } else {
      throw new UnsupportedOperationException("no implementation for generated method");
      // TODO Auto-generated method stub
    }
  }

  @Override
  public void exitSepctxpt_deny(Sepctxpt_denyContext ctx) {
    _currentFwTerm.getThens().add(FwThenDiscard.INSTANCE);
  }

  @Override
  public void exitSepctxpt_permit(Sepctxpt_permitContext ctx) {
    if (ctx.sepctxptp_tunnel() != null) {
      // Used for dynamic VPNs (no bind interface tied to a zone)
      // TODO: change from deny to appropriate action when implemented
      todo(ctx, F_PERMIT_TUNNEL);
      _currentFwTerm.getThens().add(FwThenDiscard.INSTANCE);
    } else {
      _currentFwTerm.getThens().add(FwThenAccept.INSTANCE);
    }
  }

  @Override
  public void exitSez_security_zone(Sez_security_zoneContext ctx) {
    _currentZone = null;
    _currentZoneInboundFilter = null;
  }

  @Override
  public void exitSezs_address_book(Sezs_address_bookContext ctx) {
    _currentAddressBook = null;
  }

  @Override
  public void exitSezs_interfaces(Sezs_interfacesContext ctx) {
    _currentZoneInterface = null;
  }

  private static IpWildcard toIpWildcard(Wildcard_addressContext ctx) {
    Ip address = new Ip(ctx.ip_address.getText());
    Ip mask = new Ip(ctx.wildcard_mask.getText());
    // Mask needs to be inverted since 0's are don't-cares in this context
    return new IpWildcard(address, mask.inverted());
  }

  @Override
  public void exitSezsa_address(Sezsa_addressContext ctx) {
    String name = ctx.name.getText();
    IpWildcard ipWildcard = null;
    if (ctx.wildcard_address() != null) {
      ipWildcard = toIpWildcard(ctx.wildcard_address());
    } else if (ctx.address != null) {
      ipWildcard = new IpWildcard(new Ip(ctx.address.getText()));
    } else if (ctx.prefix != null) {
      ipWildcard = new IpWildcard(Prefix.parse(ctx.prefix.getText()));
    } else {
      throw convError(IpWildcard.class, ctx);
    }
    AddressBookEntry addressEntry = new AddressAddressBookEntry(name, ipWildcard);
    _currentZone.getAddressBook().getEntries().put(name, addressEntry);
  }

  @Override
  public void exitSezsa_address_set(Sezsa_address_setContext ctx) {
    _currentAddressSetAddressBookEntry = null;
  }

  @Override
  public void exitSezsaad_address(Sezsaad_addressContext ctx) {
    String name = ctx.name.getText();
    _currentAddressSetAddressBookEntry
        .getEntries()
        .add(new AddressSetEntry(name, _currentAddressBook));
  }

  @Override
  public void exitSezsaad_address_set(Sezsaad_address_setContext ctx) {
    String name = ctx.name.getText();
    _currentAddressSetAddressBookEntry
        .getEntries()
        .add(new AddressSetEntry(name, _currentAddressBook));
  }

  @Override
  public void exitSezsh_protocols(Sezsh_protocolsContext ctx) {
    HostProtocol protocol = toHostProtocol(ctx.hib_protocol());
    String termName = protocol.name();
    FwTerm newTerm = new FwTerm(termName);
    _currentZoneInboundFilter.getTerms().put(termName, newTerm);
    newTerm.getFromHostProtocols().add(new FwFromHostProtocol(protocol));
    newTerm.getThens().add(FwThenAccept.INSTANCE);
  }

  @Override
  public void exitSezsh_system_services(Sezsh_system_servicesContext ctx) {
    HostSystemService service = toHostSystemService(ctx.hib_system_service());
    String termName = service.name();
    FwTerm newTerm = new FwTerm(termName);
    _currentZoneInboundFilter.getTerms().put(termName, newTerm);
    newTerm.getFromHostServices().add(new FwFromHostService(service));
    newTerm.getThens().add(FwThenAccept.INSTANCE);
  }

  @Override
  public void exitSnmp_community(Snmp_communityContext ctx) {
    _currentSnmpCommunity = null;
  }

  @Override
  public void exitSnmpc_authorization(Snmpc_authorizationContext ctx) {
    if (ctx.READ_ONLY() != null) {
      _currentSnmpCommunity.setRo(true);
    } else if (ctx.READ_WRITE() != null) {
      _currentSnmpCommunity.setRw(true);
    } else {
      throw new BatfishException("Invalid authorization");
    }
  }

  @Override
  public void exitSnmpc_client_list_name(Snmpc_client_list_nameContext ctx) {
    String list = ctx.name.getText();
    _currentSnmpCommunity.setAccessList(list);
    _configuration.referenceStructure(
        PREFIX_LIST, list, SNMP_COMMUNITY_PREFIX_LIST, getLine(ctx.name.getStart()));
    // TODO: verify whether both ipv4 and ipv6 list should be set with this
    // command
    _currentSnmpCommunity.setAccessList6(list);
  }

  @Override
  public void exitSnmptg_targets(Snmptg_targetsContext ctx) {
    Ip ip = new Ip(ctx.target.getText());
    String name = ip.toString();
    _currentSnmpServer.getHosts().computeIfAbsent(name, k -> new SnmpHost(ip.toString()));
  }

  @Override
  public void exitSy_default_address_selection(Sy_default_address_selectionContext ctx) {
    _configuration.setDefaultAddressSelection(true);
  }

  @Override
  public void exitSy_domain_name(Sy_domain_nameContext ctx) {
    String domainName = ctx.variable().getText();
    _currentRoutingInstance.setDomainName(domainName);
  }

  @Override
  public void exitSy_host_name(Sy_host_nameContext ctx) {
    String hostname = ctx.variable().getText();
    _currentRoutingInstance.setHostname(hostname);
  }

  @Override
  public void exitSy_name_server(Sy_name_serverContext ctx) {
    Set<String> dnsServers = _configuration.getDnsServers();
    String hostname = ctx.hostname.getText();
    dnsServers.add(hostname);
  }

  @Override
  public void exitSy_tacplus_server(Sy_tacplus_serverContext ctx) {
    _currentTacplusServer = null;
  }

  @Override
  public void exitSyr_encrypted_password(Syr_encrypted_passwordContext ctx) {
    String hash = ctx.password.getText();
    String rehashedPassword = CommonUtil.sha256Digest(hash + CommonUtil.salt());
    _configuration.getJf().setRootAuthenticationEncryptedPassword(rehashedPassword);
  }

  @Override
  public void exitSyt_secret(Syt_secretContext ctx) {
    String cipherText = unquote(ctx.secret.getText());
    _currentTacplusServer.setSecret(decryptIfNeededAndHash(cipherText, getLine(ctx.secret)));
  }

  private String decryptIfNeededAndHash(String text, int line) {
    if (JuniperUtils.isJuniper9CipherText(text)) {
      return JuniperUtils.decryptAndHashJuniper9CipherText(text);
    } else {
      _w.redFlag(String.format("Unencrypted key stored at line: %d", line));
      return CommonUtil.sha256Digest(text + CommonUtil.salt());
    }
  }

  @Override
  public void exitSyt_source_address(Syt_source_addressContext ctx) {
    Ip sourceAddress = new Ip(ctx.address.getText());
    _currentTacplusServer.setSourceAddress(sourceAddress);
  }

  @Override
  public void exitVlt_vlan_id(Vlt_vlan_idContext ctx) {
    Vlan vlan = new Vlan(_currentVlanName, toInt(ctx.id));
    _configuration.getVlanNameToVlan().put(_currentVlanName, vlan);
  }

  @Nullable
  private IpWildcard formIpWildCard(Fftfa_address_mask_prefixContext ctx) {
    IpWildcard ipWildcard = null;
    if (ctx == null) {
      return null;
    } else if (ctx.ip_address != null && ctx.wildcard_mask != null) {
      Ip ipAddress = new Ip(ctx.ip_address.getText());
      Ip mask = new Ip(ctx.wildcard_mask.getText());
      ipWildcard = new IpWildcard(ipAddress, mask.inverted());
    } else if (ctx.ip_address != null) {
      ipWildcard =
          new IpWildcard(new Prefix(new Ip(ctx.ip_address.getText()), Prefix.MAX_PREFIX_LENGTH));
    } else if (ctx.IP_PREFIX() != null) {
      ipWildcard = new IpWildcard(Prefix.parse(ctx.IP_PREFIX().getText()));
    }
    return ipWildcard;
  }

  public JuniperConfiguration getConfiguration() {
    return _configuration;
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  private String getInterfaceName(Interface_idContext ctx) {
    String name = ctx.name.getText();
    if (ctx.suffix != null) {
      name += ":" + ctx.suffix.getText();
    }
    return name;
  }

  private String initIkeProposal(IkeProposal proposal) {
    String name = proposal.getName();
    _configuration.getIkeProposals().put(name, proposal);
    return name;
  }

  private Set<String> initIkeProposalSet(Proposal_set_typeContext ctx) {
    Set<String> proposals = new HashSet<>();

    // the proposal-sets have been defined as per the specifications in the link:
    // https://www.juniper.net/documentation/en_US/junos/topics/reference/configuration-statement/security-edit-proposal-set-ike.html
    if (ctx.BASIC() != null) {
      IkeProposal proposal1 = new IkeProposal("PSK_DES_DH1_SHA1");
      IkeProposal proposal2 = new IkeProposal("PSK_DES_DH1_MD5");

      proposals.add(
          initIkeProposal(
              proposal1
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS)
                  .setDiffieHellmanGroup(DiffieHellmanGroup.GROUP1)
                  .setAuthenticationAlgorithm(IkeHashingAlgorithm.SHA1)));
      proposals.add(
          initIkeProposal(
              proposal2
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS)
                  .setDiffieHellmanGroup(DiffieHellmanGroup.GROUP1)
                  .setAuthenticationAlgorithm(IkeHashingAlgorithm.MD5)));
    } else if (ctx.COMPATIBLE() != null) {
      IkeProposal proposal1 = new IkeProposal("PSK_3DES_DH2_MD5");
      IkeProposal proposal2 = new IkeProposal("PSK_DES_DH2_SHA1");
      IkeProposal proposal3 = new IkeProposal("PSK_DES_DH2_MD5");

      proposals.add(
          initIkeProposal(
              proposal1
                  .setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS)
                  .setDiffieHellmanGroup(DiffieHellmanGroup.GROUP2)
                  .setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC)
                  .setAuthenticationAlgorithm(IkeHashingAlgorithm.MD5)));
      proposals.add(
          initIkeProposal(
              proposal2
                  .setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS)
                  .setDiffieHellmanGroup(DiffieHellmanGroup.GROUP2)
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationAlgorithm(IkeHashingAlgorithm.SHA1)));
      proposals.add(
          initIkeProposal(
              proposal3
                  .setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS)
                  .setDiffieHellmanGroup(DiffieHellmanGroup.GROUP2)
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationAlgorithm(IkeHashingAlgorithm.MD5)));

    } else if (ctx.STANDARD() != null) {
      IkeProposal proposal1 = new IkeProposal("PSK_3DES_DH2_SHA1");
      IkeProposal proposal2 = new IkeProposal("PSK_AES128_DH2_SHA1");

      proposals.add(
          initIkeProposal(
              proposal1
                  .setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS)
                  .setDiffieHellmanGroup(DiffieHellmanGroup.GROUP2)
                  .setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC)
                  .setAuthenticationAlgorithm(IkeHashingAlgorithm.SHA1)));
      proposals.add(
          initIkeProposal(
              proposal2
                  .setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS)
                  .setDiffieHellmanGroup(DiffieHellmanGroup.GROUP2)
                  .setEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC)
                  .setAuthenticationAlgorithm(IkeHashingAlgorithm.SHA1)));
    }
    return proposals;
  }

  private Interface initInterface(Interface_idContext id) {
    String currentRoutingInstance = _currentRoutingInstance.getName();
    Map<String, Interface> interfaces;
    if (id.node != null) {
      String nodeDeviceName = id.node.getText();
      NodeDevice nodeDevice =
          _configuration.getNodeDevices().computeIfAbsent(nodeDeviceName, NodeDevice::new);
      interfaces = nodeDevice.getInterfaces();
    } else {
      interfaces = _configuration.getInterfaces();
    }
    String name = getInterfaceName(id);
    int definitionLine = getLine(id.name);
    String unit = null;
    if (id.unit != null) {
      unit = id.unit.getText();
      definitionLine = getLine(id.unit);
    }
    String unitFullName = name + "." + unit;
    Interface iface = interfaces.get(name);
    if (iface == null) {
      iface = new Interface(name, definitionLine);
      iface.setRoutingInstance(currentRoutingInstance);
      interfaces.put(name, iface);
    }
    if (unit != null) {
      Map<String, Interface> units = iface.getUnits();
      iface = units.get(unitFullName);
      if (iface == null) {
        iface = new Interface(unitFullName, definitionLine);
        iface.setRoutingInstance(currentRoutingInstance);
        units.put(unitFullName, iface);
      }
    }
    return iface;
  }

  private String initIpsecProposal(IpsecProposal proposal) {
    String name = proposal.getName();
    _configuration.getIpsecProposals().put(name, proposal);
    return name;
  }

  private List<String> initIpsecProposalSet(Proposal_set_typeContext ctx) {
    List<String> proposals = new ArrayList<>();
    if (ctx.BASIC() != null) {
      IpsecProposal proposal1 = new IpsecProposal("NOPFS_ESP_DES_SHA");
      IpsecProposal proposal2 = new IpsecProposal("NOPFS_ESP_DES_MD5");
      proposals.add(
          initIpsecProposal(
              proposal1
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96)));
      proposals.add(
          initIpsecProposal(
              proposal2
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_MD5_96)));
    } else if (ctx.COMPATIBLE() != null) {
      IpsecProposal proposal1 = new IpsecProposal("NOPFS_ESP_3DES_SHA");
      IpsecProposal proposal2 = new IpsecProposal("NOPFS_ESP_3DES_MD5");
      IpsecProposal proposal3 = new IpsecProposal("NOPFS_ESP_DES_SHA");
      IpsecProposal proposal4 = new IpsecProposal("NOPFS_ESP_DES_MD5");
      proposals.add(
          initIpsecProposal(
              proposal1
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96)));
      proposals.add(
          initIpsecProposal(
              proposal2
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_MD5_96)));
      proposals.add(
          initIpsecProposal(
              proposal3
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96)));
      proposals.add(
          initIpsecProposal(
              proposal4
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_MD5_96)));
    } else if (ctx.STANDARD() != null) {
      IpsecProposal proposal1 = new IpsecProposal("G2_ESP_3DES_SHA");
      IpsecProposal proposal2 = new IpsecProposal("G2_ESP_AES128_SHA");
      proposals.add(
          initIpsecProposal(
              proposal1
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96)));
      proposals.add(
          initIpsecProposal(
              proposal2
                  .setProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP))
                  .setEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC)
                  .setAuthenticationAlgorithm(IpsecAuthenticationAlgorithm.HMAC_SHA1_96)));
      _currentIpsecPolicy.setPfsKeyGroup(DiffieHellmanGroup.GROUP2);
    }
    return proposals;
  }

  private AsPath toAsPath(As_path_exprContext path) {
    List<SortedSet<Long>> asPath = new ArrayList<>();
    for (As_unitContext ctx : path.items) {
      SortedSet<Long> asSet = new TreeSet<>();
      if (ctx.DEC() != null) {
        asSet.add(toLong(ctx.DEC()));
      } else {
        for (Token token : ctx.as_set().items) {
          asSet.add(toLong(token));
        }
      }
      asPath.add(asSet);
    }
    return new AsPath(asPath);
  }

  private long toCommunityLong(Ec_literalContext ctx) {
    String[] parts = ctx.getText().split(":");
    int part1 = Integer.parseInt(parts[0]);
    long part2 = Long.parseLong(parts[1]);
    int part3 = Integer.parseInt(parts[2]);
    ExtendedCommunity c = new ExtendedCommunity(part1, part2, part3);
    return c.asLong();
  }

  private long toCommunityLong(Ec_namedContext ctx) {
    ExtendedCommunity ec = new ExtendedCommunity(ctx.getText());
    return ec.asLong();
  }

  private long toCommunityLong(Extended_communityContext ctx) {
    if (ctx.ec_literal() != null) {
      return toCommunityLong(ctx.ec_literal());
    } else if (ctx.ec_named() != null) {
      return toCommunityLong(ctx.ec_named());
    } else {
      throw new BatfishException("invalid extended community");
    }
  }

  private String toComplexPolicyStatement(
      Policy_expressionContext expr, JuniperStructureUsage usage) {
    if (expr.pe_nested() != null) {
      return toComplexPolicyStatement(expr.pe_nested().policy_expression(), usage);
    } else if (expr.variable() != null) {
      String name = expr.variable().getText();
      _configuration.referenceStructure(
          POLICY_STATEMENT, name, usage, getLine(expr.variable().getStart()));
      return name;
    } else if (expr.pe_conjunction() != null) {
      Set<String> conjuncts = new LinkedHashSet<>();
      for (Policy_expressionContext conjunctCtx : expr.pe_conjunction().policy_expression()) {
        String conjunctName = toComplexPolicyStatement(conjunctCtx, usage);
        conjuncts.add(conjunctName);
      }
      String conjunctionPolicyName = "~CONJUNCTION_POLICY_" + _conjunctionPolicyIndex + "~";
      _conjunctionPolicyIndex++;
      PolicyStatement conjunctionPolicy = new PolicyStatement(conjunctionPolicyName);
      PsTerm conjunctionPolicyTerm = conjunctionPolicy.getDefaultTerm();
      PsFromPolicyStatementConjunction from = new PsFromPolicyStatementConjunction(conjuncts);
      conjunctionPolicyTerm.getFroms().add(from);
      conjunctionPolicyTerm.getThens().add(PsThenAccept.INSTANCE);
      _configuration.getPolicyStatements().put(conjunctionPolicyName, conjunctionPolicy);
      return conjunctionPolicyName;
    } else if (expr.pe_disjunction() != null) {
      Set<String> disjuncts = new LinkedHashSet<>();
      for (Policy_expressionContext disjunctCtx : expr.pe_disjunction().policy_expression()) {
        String disjunctName = toComplexPolicyStatement(disjunctCtx, usage);
        disjuncts.add(disjunctName);
      }
      String disjunctionPolicyName = "~DISJUNCTION_POLICY_" + _disjunctionPolicyIndex + "~";
      _disjunctionPolicyIndex++;
      PolicyStatement disjunctionPolicy = new PolicyStatement(disjunctionPolicyName);
      PsTerm disjunctionPolicyTerm = disjunctionPolicy.getDefaultTerm();
      for (String disjunct : disjuncts) {
        PsFromPolicyStatement from = new PsFromPolicyStatement(disjunct);
        disjunctionPolicyTerm.getFroms().add(from);
      }
      disjunctionPolicyTerm.getThens().add(PsThenAccept.INSTANCE);
      _configuration.getPolicyStatements().put(disjunctionPolicyName, disjunctionPolicy);
      return disjunctionPolicyName;
    } else {
      throw new BatfishException("Invalid policy expression");
    }
  }

  private DiffieHellmanGroup toDhGroup(Dh_groupContext ctx) {
    if (ctx.GROUP1() != null) {
      return DiffieHellmanGroup.GROUP1;
    } else if (ctx.GROUP14() != null) {
      return DiffieHellmanGroup.GROUP14;
    } else if (ctx.GROUP15() != null) {
      return DiffieHellmanGroup.GROUP15;
    } else if (ctx.GROUP16() != null) {
      return DiffieHellmanGroup.GROUP16;
    } else if (ctx.GROUP19() != null) {
      return DiffieHellmanGroup.GROUP19;
    } else if (ctx.GROUP2() != null) {
      return DiffieHellmanGroup.GROUP2;
    } else if (ctx.GROUP20() != null) {
      return DiffieHellmanGroup.GROUP20;
    } else if (ctx.GROUP24() != null) {
      return DiffieHellmanGroup.GROUP24;
    } else if (ctx.GROUP5() != null) {
      return DiffieHellmanGroup.GROUP5;
    } else {
      throw new BatfishException("invalid dh-group");
    }
  }

  private void todo(ParserRuleContext ctx, String feature) {
    _w.todo(ctx, feature, _parser, _text);
    _unimplementedFeatures.add("Juniper: " + feature);
  }

  private Family toFamily(F_familyContext ctx) {
    if (ctx.ANY() != null) {
      return Family.ANY;
    } else if (ctx.BRIDGE() != null) {
      return Family.BRIDGE;
    } else if (ctx.CCC() != null) {
      return Family.CCC;
    } else if (ctx.ETHERNET_SWITCHING() != null) {
      return Family.ETHERNET_SWITCHING;
    } else if (ctx.INET() != null) {
      return Family.INET;
    } else if (ctx.INET6() != null) {
      return Family.INET6;
    } else if (ctx.MPLS() != null) {
      return Family.MPLS;
    } else {
      throw convError(Family.class, ctx);
    }
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    int line = getLine(token);
    String msg = String.format("Unrecognized Line: %d: %s", line, lineText);
    if (_unrecognizedAsRedFlag) {
      _w.redFlag(msg + " SUBSEQUENT LINES MAY NOT BE PROCESSED CORRECTLY");
      _configuration.setUnrecognized(true);
    } else {
      _parser.getErrors().add(msg);
    }
  }
}
