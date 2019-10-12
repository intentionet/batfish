package org.batfish.grammar.palo_alto;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.CATCHALL_APPLICATION_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.CATCHALL_ZONE_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.DEFAULT_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.PANORAMA_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.SHARED_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.computeObjectName;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP_OR_ADDRESS_OBJECT;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_OBJECT;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.APPLICATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.APPLICATION_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION_OR_NONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.RULE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP_OR_NONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SHARED_GATEWAY;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.VIRTUAL_ROUTER;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ZONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.ADDRESS_GROUP_STATIC;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.APPLICATION_GROUP_MEMBERS;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.BGP_PEER_LOCAL_ADDRESS_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.IMPORT_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULEBASE_SERVICE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_APPLICATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_DESTINATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_FROM_ZONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_SELF_REF;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_SOURCE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_TO_ZONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.SERVICE_GROUP_MEMBER;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.STATIC_ROUTE_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.STATIC_ROUTE_NEXT_VR;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.VIRTUAL_ROUTER_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.VIRTUAL_ROUTER_SELF_REFERENCE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.VSYS_IMPORT_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.ZONE_INTERFACE;
import static org.batfish.representation.palo_alto.Zone.Type.EXTERNAL;
import static org.batfish.representation.palo_alto.Zone.Type.LAYER2;
import static org.batfish.representation.palo_alto.Zone.Type.LAYER3;
import static org.batfish.representation.palo_alto.Zone.Type.TAP;
import static org.batfish.representation.palo_alto.Zone.Type.VIRTUAL_WIRE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpRange;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_asnContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_enableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_install_routeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_local_asContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_local_prefContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_peer_group_nameContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_peer_nameContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_reject_default_routeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgp_router_idContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppg_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppg_enableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppg_peerContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_coi_allowContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_coi_remote_portContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_coo_allowContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_coo_local_portContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_enableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_la_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_la_ipContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_peer_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_peer_asContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgp_reflector_clientContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgt_ebgpContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgppgt_ibgpContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgpro_as_formatContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgpro_default_local_preferenceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgpro_reflector_cluster_idContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgproa_aggregate_medContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgprog_enableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgprom_always_compare_medContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Bgprom_deterministic_med_comparisonContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_authenticationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_dh_groupContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_encryptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_encryption_algoContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_hashContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Cp_lifetimeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.If_commentContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.If_tagContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Interface_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ip_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ip_address_or_slash32Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospf_areaContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospf_enableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospf_graceful_restartContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospf_reject_default_routeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospf_router_idContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfa_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_dead_countsContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_enableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_hello_intervalContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_link_typeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_metricContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_passiveContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_priorityContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_retransmit_intervalContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfai_transit_delayContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfat_normalContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfat_nssaContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfat_stubContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfatn_accept_summaryContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfatndr_disableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfatndra_metricContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfatndra_typeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfats_accept_summaryContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfatsdr_advertise_metricContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ospfatsdr_disableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Palo_alto_configurationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Panorama_post_rulebaseContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Panorama_pre_rulebaseContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Port_numberContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Port_or_rangeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Protocol_adContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_address_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_address_group_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_application_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_policy_panoramaContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_rulebaseContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_service_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_service_group_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_sharedContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_tagContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_vsys_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.S_zone_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_fqdnContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_ip_netmaskContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_ip_rangeContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sa_tagContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sag_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sag_staticContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sag_tagContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sagd_filterContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sapp_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sappg_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sappg_membersContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_default_gatewayContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_hostnameContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_ip_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_netmaskContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sds_ntp_serversContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sdsd_serversContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sdsn_ntp_server_addressContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Set_line_config_devicesContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sn_shared_gateway_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_ethernet_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_loopbackContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_tunnelContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sni_vlanContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snicp_global_protectContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snicp_ike_crypto_profilesContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snicp_ipsec_crypto_profilesContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snie_link_stateContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel2_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel3_ipContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel3_mtuContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniel3_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snil_ipContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snil_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snit_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sniv_unitContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snsg_display_nameContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snsg_zone_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snsgi_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Snsgzn_layer3Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.Src_or_dst_list_itemContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_actionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_applicationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_destinationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_disabledContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_fromContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_negate_destinationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_negate_sourceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_serviceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_sourceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Srs_toContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_descriptionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_portContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_protocolContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sserv_source_portContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sservgrp_membersContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ssl_syslogContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Ssls_serverContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Sslss_serverContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.St_commentsContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Svi_visible_vsysContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Svin_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Szn_externalContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Szn_layer2Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.Szn_layer3Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.Szn_tapContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Szn_virtual_wireContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Uint16Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.Uint32Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.Uint8Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.VariableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Variable_listContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Variable_list_itemContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vlan_tagContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vr_definitionContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vr_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vr_routing_tableContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_ebgpContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_ibgpContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_ospf_extContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_ospf_intContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_ospfv3_extContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_ospfv3_intContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_ripContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_staticContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrad_static_ipv6Context;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrp_bgpContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrp_ospfContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrrt_admin_distContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrrt_destinationContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrrt_interfaceContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrrt_metricContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrrtn_ipContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Vrrtn_next_vrContext;
import org.batfish.grammar.palo_alto.PaloAltoParser.Yes_or_noContext;
import org.batfish.representation.palo_alto.AddressGroup;
import org.batfish.representation.palo_alto.AddressObject;
import org.batfish.representation.palo_alto.Application;
import org.batfish.representation.palo_alto.ApplicationBuiltIn;
import org.batfish.representation.palo_alto.ApplicationGroup;
import org.batfish.representation.palo_alto.BgpPeer;
import org.batfish.representation.palo_alto.BgpPeer.ReflectorClient;
import org.batfish.representation.palo_alto.BgpPeerGroup;
import org.batfish.representation.palo_alto.BgpVr;
import org.batfish.representation.palo_alto.BgpVrRoutingOptions.AsFormat;
import org.batfish.representation.palo_alto.CryptoProfile;
import org.batfish.representation.palo_alto.CryptoProfile.Type;
import org.batfish.representation.palo_alto.Interface;
import org.batfish.representation.palo_alto.OspfArea;
import org.batfish.representation.palo_alto.OspfAreaNormal;
import org.batfish.representation.palo_alto.OspfAreaNssa;
import org.batfish.representation.palo_alto.OspfAreaNssa.DefaultRouteType;
import org.batfish.representation.palo_alto.OspfAreaStub;
import org.batfish.representation.palo_alto.OspfInterface;
import org.batfish.representation.palo_alto.OspfInterface.LinkType;
import org.batfish.representation.palo_alto.OspfVr;
import org.batfish.representation.palo_alto.PaloAltoConfiguration;
import org.batfish.representation.palo_alto.PaloAltoStructureType;
import org.batfish.representation.palo_alto.PaloAltoStructureUsage;
import org.batfish.representation.palo_alto.Rule;
import org.batfish.representation.palo_alto.RuleEndpoint;
import org.batfish.representation.palo_alto.Service;
import org.batfish.representation.palo_alto.ServiceBuiltIn;
import org.batfish.representation.palo_alto.ServiceGroup;
import org.batfish.representation.palo_alto.ServiceOrServiceGroupReference;
import org.batfish.representation.palo_alto.StaticRoute;
import org.batfish.representation.palo_alto.SyslogServer;
import org.batfish.representation.palo_alto.Tag;
import org.batfish.representation.palo_alto.VirtualRouter;
import org.batfish.representation.palo_alto.Vsys;
import org.batfish.representation.palo_alto.Vsys.NamespaceType;
import org.batfish.representation.palo_alto.Zone;

public class PaloAltoConfigurationBuilder extends PaloAltoParserBaseListener {

  /** Indicates which rulebase that new rules go into. */
  private enum RulebaseId {
    /** Vsys.getRules(). */
    DEFAULT,
    /** Vsys.getPreRules(). */
    PRE,
    /** Vsys.getPostRules(). */
    POST
  }

  private PaloAltoConfiguration _configuration;
  private Vsys _defaultVsys;
  private PaloAltoCombinedParser _parser;
  private final String _text;
  private final Warnings _w;

  private AddressGroup _currentAddressGroup;
  private AddressObject _currentAddressObject;
  private Application _currentApplication;
  private ApplicationGroup _currentApplicationGroup;
  private BgpPeer _currentBgpPeer;
  private BgpPeerGroup _currentBgpPeerGroup;
  private BgpVr _currentBgpVr;
  private CryptoProfile _currentCrytoProfile;
  private String _currentDeviceName;
  private Interface _currentInterface;
  private boolean _currentNtpServerPrimary;
  private Interface _currentParentInterface;
  private OspfArea _currentOspfArea;
  private OspfInterface _currentOspfInterface;
  private OspfAreaStub _currentOspfStubAreaType;
  private OspfAreaNssa _currentOspfNssaAreaType;
  private OspfVr _currentOspfVr;
  private RulebaseId _currentRuleScope;
  private Rule _currentRule;
  private Service _currentService;
  private ServiceGroup _currentServiceGroup;
  private StaticRoute _currentStaticRoute;
  private SyslogServer _currentSyslogServer;
  private String _currentSyslogServerGroupName;
  private Tag _currentTag;
  private VirtualRouter _currentVirtualRouter;
  private Vsys _currentVsys;
  private Zone _currentZone;

  public PaloAltoConfigurationBuilder(
      PaloAltoCombinedParser parser, String text, Warnings warnings) {
    _configuration = new PaloAltoConfiguration();
    _parser = parser;
    _text = text;
    _w = warnings;
  }

  @SuppressWarnings("unused")
  private BatfishException convError(Class<?> type, ParserRuleContext ctx) {
    String typeName = type.getSimpleName();
    String txt = getFullText(ctx);
    return new BatfishException("Could not convert to " + typeName + ": " + txt);
  }

  private static String convErrorMessage(Class<?> type) {
    return String.format("Could not convert to %s", type.getSimpleName());
  }

  private <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, U defaultReturnValue) {
    warn(ctx, convErrorMessage(returnType));
    return defaultReturnValue;
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  /** Return original line number for specified token */
  private int getLine(Token t) {
    return _parser.getLine(t);
  }

  /** Return token text with enclosing quotes removed, if applicable */
  private String getText(ParserRuleContext ctx) {
    return unquote(ctx.getText());
  }

  /** Return token text with enclosing quotes removed, if applicable */
  private String getText(TerminalNode t) {
    return unquote(t.getText());
  }

  /** Return token text with enclosing quotes removed, if applicable */
  private String getText(Token t) {
    return unquote(t.getText());
  }

  /**
   * Helper function to add the correct service reference type for a given reference. For references
   * that may be pointing to built-in services, this is needed to make sure we don't create false
   * positive undefined references.
   */
  private void referenceService(Variable_list_itemContext var, PaloAltoStructureUsage usage) {
    String serviceName = getText(var);
    // Use constructed object name so same-named refs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), serviceName);

    if (Arrays.stream(ServiceBuiltIn.values()).anyMatch(n -> serviceName.equals(n.getName()))) {
      // Built-in services can be overridden, so add optional object reference
      _configuration.referenceStructure(
          SERVICE_OR_SERVICE_GROUP_OR_NONE, uniqueName, usage, getLine(var.start));
    } else {
      _configuration.referenceStructure(
          SERVICE_OR_SERVICE_GROUP, uniqueName, usage, getLine(var.start));
    }
  }

  private DiffieHellmanGroup toDiffieHellmanGroup(Cp_dh_groupContext ctx) {
    if (ctx.GROUP1() != null) {
      return DiffieHellmanGroup.GROUP1;
    } else if (ctx.GROUP2() != null) {
      return DiffieHellmanGroup.GROUP2;
    } else if (ctx.GROUP5() != null) {
      return DiffieHellmanGroup.GROUP5;
    } else if (ctx.GROUP14() != null) {
      return DiffieHellmanGroup.GROUP14;
    } else if (ctx.GROUP19() != null) {
      return DiffieHellmanGroup.GROUP19;
    } else if (ctx.GROUP20() != null) {
      return DiffieHellmanGroup.GROUP20;
    } else {
      return convProblem(DiffieHellmanGroup.class, ctx, null);
    }
  }

  private EncryptionAlgorithm toEncryptionAlgo(Cp_encryption_algoContext ctx) {
    if (ctx.AES_128_CBC() != null) {
      return EncryptionAlgorithm.AES_128_CBC;
    } else if (ctx.AES_128_GCM() != null) {
      return EncryptionAlgorithm.AES_128_GCM;
    } else if (ctx.AES_192_CBC() != null) {
      return EncryptionAlgorithm.AES_192_CBC;
    } else if (ctx.AES_256_CBC() != null) {
      return EncryptionAlgorithm.AES_256_CBC;
    } else if (ctx.AES_256_GCM() != null) {
      return EncryptionAlgorithm.AES_256_GCM;
    } else if (ctx.DES() != null) {
      return EncryptionAlgorithm.DES_CBC;
    } else if (ctx.THREE_DES() != null) {
      return EncryptionAlgorithm.THREEDES_CBC;
    } else if (ctx.NULL() != null) {
      return EncryptionAlgorithm.NULL;
    }
    return convProblem(EncryptionAlgorithm.class, ctx, null);
  }

  private IkeHashingAlgorithm toIkeHashingAlgorithm(Cp_hashContext ctx) {
    if (ctx.MD5() != null) {
      return IkeHashingAlgorithm.MD5;
    } else if (ctx.SHA1() != null) {
      return IkeHashingAlgorithm.SHA1;
    } else if (ctx.SHA256() != null) {
      return IkeHashingAlgorithm.SHA_256;
    } else if (ctx.SHA384() != null) {
      return IkeHashingAlgorithm.SHA_384;
    } else if (ctx.SHA512() != null) {
      return IkeHashingAlgorithm.SHA_512;
    }
    return convProblem(IkeHashingAlgorithm.class, ctx, null);
  }

  private IpsecAuthenticationAlgorithm toIpsecAuthenticationAlgorithm(
      Cp_authenticationContext ctx) {
    if (ctx.MD5() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_MD5_96;
    } else if (ctx.SHA1() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA1_96;
    } else if (ctx.SHA256() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_256_128;
    } else if (ctx.SHA384() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_384;
    } else if (ctx.SHA512() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_512;
    } else if (ctx.NONE() != null) {
      return null;
    }
    return convProblem(IpsecAuthenticationAlgorithm.class, ctx, null);
  }

  /** Convert source or destination list item into an appropriate IpSpace */
  private RuleEndpoint toRuleEndpoint(Src_or_dst_list_itemContext ctx) {
    String text = getText(ctx);
    if (ctx.any != null) {
      return new RuleEndpoint(RuleEndpoint.Type.Any, text);
    } else if (ctx.address != null) {
      return new RuleEndpoint(RuleEndpoint.Type.IP_ADDRESS, text);
    } else if (ctx.prefix != null) {
      return new RuleEndpoint(RuleEndpoint.Type.IP_PREFIX, text);
    } else if (ctx.range != null) {
      return new RuleEndpoint(RuleEndpoint.Type.IP_RANGE, text);
    }
    return new RuleEndpoint(RuleEndpoint.Type.REFERENCE, text);
  }

  private String unquote(String text) {
    if (text.length() < 2) {
      return text;
    }
    char leading = text.charAt(0);
    char trailing = text.charAt(text.length() - 1);
    if (leading == '\'' || leading == '"') {
      if (leading == trailing) {
        return text.substring(1, text.length() - 1);
      } else {
        _w.redFlag("Improperly-quoted string: " + text);
      }
    }
    return text;
  }

  /** A helper function to extract all variables from an optional list. */
  private static List<Variable_list_itemContext> variables(@Nullable Variable_listContext ctx) {
    if (ctx == null || ctx.variable_list_item() == null) {
      return ImmutableList.of();
    }
    return ctx.variable_list_item();
  }

  @Override
  public void exitBgp_enable(Bgp_enableContext ctx) {
    _currentBgpVr.setEnable(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgp_install_route(Bgp_install_routeContext ctx) {
    _currentBgpVr.setInstallRoute(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgp_local_as(Bgp_local_asContext ctx) {
    toAsn(ctx, ctx.asn, _currentBgpVr.getRoutingOptions().getAsFormat())
        .ifPresent(_currentBgpVr::setLocalAs);
  }

  @Override
  public void enterBgppg_definition(Bgppg_definitionContext ctx) {
    _currentBgpPeerGroup =
        toString(ctx, ctx.name)
            .map(_currentBgpVr::getOrCreatePeerGroup)
            .orElseGet(() -> new BgpPeerGroup("dummy")); // create dummy if the name is invalid.
  }

  @Override
  public void exitBgppg_definition(Bgppg_definitionContext ctx) {
    _currentBgpPeerGroup = null;
  }

  @Override
  public void exitBgppg_enable(Bgppg_enableContext ctx) {
    _currentBgpPeerGroup.setEnable(toBoolean(ctx.yn));
  }

  @Override
  public void enterBgppg_peer(Bgppg_peerContext ctx) {
    _currentBgpPeer =
        toString(ctx, ctx.name)
            .map(_currentBgpPeerGroup::getOrCreatePeerGroup)
            .orElseGet(() -> new BgpPeer("dummy")); // create dummy if the name is invalid.
  }

  @Override
  public void exitBgppgp_coi_allow(Bgppgp_coi_allowContext ctx) {
    _currentBgpPeer.getConnectionOptions().setIncomingAllow(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgppgp_coi_remote_port(Bgppgp_coi_remote_portContext ctx) {
    _currentBgpPeer.getConnectionOptions().setRemotePort(toInteger(ctx.p));
  }

  @Override
  public void exitBgppgp_coo_allow(Bgppgp_coo_allowContext ctx) {
    _currentBgpPeer.getConnectionOptions().setOutgoingAllow(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgppgp_coo_local_port(Bgppgp_coo_local_portContext ctx) {
    _currentBgpPeer.getConnectionOptions().setLocalPort(toInteger(ctx.p));
  }

  @Override
  public void exitBgppgp_enable(Bgppgp_enableContext ctx) {
    _currentBgpPeer.setEnable(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgppgp_la_interface(Bgppgp_la_interfaceContext ctx) {
    String name = getText(ctx.name);
    _currentBgpPeer.setLocalInterface(name);
    _configuration.referenceStructure(
        INTERFACE, name, BGP_PEER_LOCAL_ADDRESS_INTERFACE, getLine(ctx.name.start));
  }

  @Override
  public void exitBgppgp_la_ip(Bgppgp_la_ipContext ctx) {
    ConcreteInterfaceAddress address = toInterfaceAddress(ctx.interface_address());
    _currentBgpPeer.setLocalAddress(address.getIp());
  }

  @Override
  public void exitBgppgp_peer_address(Bgppgp_peer_addressContext ctx) {
    toIp(ctx, ctx.addr, "BGP peer-address").ifPresent(_currentBgpPeer::setPeerAddress);
  }

  @Override
  public void exitBgppgp_peer_as(Bgppgp_peer_asContext ctx) {
    toAsn(ctx, ctx.asn, _currentBgpVr.getRoutingOptions().getAsFormat())
        .ifPresent(_currentBgpPeer::setPeerAs);
  }

  @Override
  public void exitBgppg_peer(Bgppg_peerContext ctx) {
    _currentBgpPeer = null;
  }

  @Override
  public void exitBgppgp_reflector_client(Bgppgp_reflector_clientContext ctx) {
    if (ctx.CLIENT() != null) {
      _currentBgpPeer.setReflectorClient(ReflectorClient.CLIENT);
    } else if (ctx.MESHED_CLIENT() != null) {
      _currentBgpPeer.setReflectorClient(ReflectorClient.MESHED_CLIENT);
    } else {
      assert ctx.NON_CLIENT() != null;
      _currentBgpPeer.setReflectorClient(ReflectorClient.NON_CLIENT);
    }
  }

  @Override
  public void enterBgppgt_ebgp(Bgppgt_ebgpContext ctx) {
    _currentBgpPeerGroup.updateAndGetEbgpType();
  }

  @Override
  public void enterBgppgt_ibgp(Bgppgt_ibgpContext ctx) {
    _currentBgpPeerGroup.updateAndGetIbgpType();
  }

  @Override
  public void exitBgp_reject_default_route(Bgp_reject_default_routeContext ctx) {
    _currentBgpVr.setRejectDefaultRoute(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgproa_aggregate_med(Bgproa_aggregate_medContext ctx) {
    _currentBgpVr.getRoutingOptions().setAggregateMed(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgpro_as_format(Bgpro_as_formatContext ctx) {
    if (ctx.TWO_BYTE() != null) {
      _currentBgpVr.getRoutingOptions().setAsFormat(AsFormat.TWO_BYTE_AS);
    } else {
      assert ctx.FOUR_BYTE() != null;
      _currentBgpVr.getRoutingOptions().setAsFormat(AsFormat.FOUR_BYTE_AS);
    }
  }

  @Override
  public void exitBgpro_default_local_preference(Bgpro_default_local_preferenceContext ctx) {
    _currentBgpVr.getRoutingOptions().setDefaultLocalPreference(toLong(ctx.pref));
  }

  @Override
  public void exitBgpro_reflector_cluster_id(Bgpro_reflector_cluster_idContext ctx) {
    _currentBgpVr.getRoutingOptions().setReflectorClusterId(toIp(ctx.id));
  }

  @Override
  public void exitBgprog_enable(Bgprog_enableContext ctx) {
    _currentBgpVr.getRoutingOptions().setGracefulRestartEnable(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgprom_always_compare_med(Bgprom_always_compare_medContext ctx) {
    _currentBgpVr.getRoutingOptions().setAlwaysCompareMed(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgprom_deterministic_med_comparison(
      Bgprom_deterministic_med_comparisonContext ctx) {
    _currentBgpVr.getRoutingOptions().setDeterministicMedComparison(toBoolean(ctx.yn));
  }

  @Override
  public void exitBgp_router_id(Bgp_router_idContext ctx) {
    toIp(ctx, ctx.addr, "BGP router-id").ifPresent(_currentBgpVr::setRouterId);
  }

  @Override
  public void exitCp_authentication(Cp_authenticationContext ctx) {
    if (_currentCrytoProfile.getType() == Type.IKE) {
      warn(ctx, "'authentication' is illegal for ike-crypto-profile");
      return;
    }
    IpsecAuthenticationAlgorithm algo = toIpsecAuthenticationAlgorithm(ctx);
    if (algo != null) {
      _currentCrytoProfile.setAuthAlgorithm(algo);
    }
  }

  @Override
  public void exitCp_dh_group(Cp_dh_groupContext ctx) {
    if (_currentCrytoProfile.getType() == Type.GLOBAL_PROTECT_APP) {
      warn(ctx, "'dh-group' is illegal for global-proptect-app-crypto-profile");
      return;
    }
    DiffieHellmanGroup dhGroup = toDiffieHellmanGroup(ctx);
    if (dhGroup != null) {
      _currentCrytoProfile.setDhGroup(dhGroup);
    }
  }

  @Override
  public void exitCp_encryption(Cp_encryptionContext ctx) {
    List<EncryptionAlgorithm> algos =
        ctx.algo.stream().map(this::toEncryptionAlgo).collect(Collectors.toList());
    _currentCrytoProfile.setEncryptionAlgorithms(algos);
  }

  @Override
  public void exitCp_hash(Cp_hashContext ctx) {
    if (_currentCrytoProfile.getType() != Type.IKE) {
      warn(ctx, "'hash' is illegal for non-Ike crypto profiles");
      return;
    }
    IkeHashingAlgorithm algo = toIkeHashingAlgorithm(ctx);
    if (algo != null) {
      _currentCrytoProfile.setHashAlgorithm(algo);
    }
  }

  @Override
  public void exitCp_lifetime(Cp_lifetimeContext ctx) {
    if (_currentCrytoProfile.getType() == Type.GLOBAL_PROTECT_APP) {
      warn(ctx, "'lifetime' is illegal for global-protect-app-crypto profile");
      return;
    }
    int val = toInteger(ctx.val);
    if (ctx.DAYS() != null) {
      val *= 24 * 60 * 60;
    } else if (ctx.HOURS() != null) {
      val *= 60 * 60;
    } else if (ctx.MINUTES() != null) {
      val *= 60;
    }
    _currentCrytoProfile.setLifetimeSeconds(val);
  }

  @Override
  public void exitIf_comment(If_commentContext ctx) {
    _currentInterface.setComment(getText(ctx.text));
  }

  @Override
  public void exitIf_tag(If_tagContext ctx) {
    toInteger(ctx, ctx.tag).ifPresent(_currentInterface::setTag);
  }

  @Override
  public void enterOspf_area(Ospf_areaContext ctx) {
    _currentOspfArea =
        toIp(ctx, ctx.addr, "OSPF area-id")
            .map(_currentOspfVr::getOrCreateOspfArea)
            // dummy area with Ip MAX
            .orElseGet(() -> new OspfArea(Ip.MAX));
  }

  @Override
  public void exitOspf_area(Ospf_areaContext ctx) {
    _currentOspfArea = null;
  }

  @Override
  public void enterOspfa_interface(Ospfa_interfaceContext ctx) {
    _currentOspfInterface = _currentOspfArea.getOrCreateOspfInterface(getText(ctx.name));
  }

  @Override
  public void exitOspfai_enable(Ospfai_enableContext ctx) {
    _currentOspfInterface.setEnable(toBoolean(ctx.yn));
  }

  @Override
  public void exitOspfai_hello_interval(Ospfai_hello_intervalContext ctx) {
    _currentOspfInterface.setHelloInterval(toInteger(ctx.hello_interval.uint16()));
  }

  @Override
  public void exitOspfai_link_type(Ospfai_link_typeContext ctx) {
    if (ctx.BROADCAST() != null) {
      _currentOspfInterface.setLinkType(LinkType.BROADCAST);
    } else if (ctx.P2P() != null) {
      _currentOspfInterface.setLinkType(LinkType.P2P);
    } else if (ctx.P2MP() != null) {
      _currentOspfInterface.setLinkType(LinkType.P2MP);
    }
  }

  @Override
  public void exitOspfai_metric(Ospfai_metricContext ctx) {
    _currentOspfInterface.setMetric(toInteger(ctx.metric.uint8()));
  }

  @Override
  public void exitOspfai_passive(Ospfai_passiveContext ctx) {
    _currentOspfInterface.setPassive(toBoolean(ctx.yn));
  }

  @Override
  public void exitOspfai_priority(Ospfai_priorityContext ctx) {
    _currentOspfInterface.setPriority(toInteger(ctx.priority.uint8()));
  }

  @Override
  public void exitOspfai_retransmit_interval(Ospfai_retransmit_intervalContext ctx) {
    _currentOspfInterface.setRetransmitInterval(toInteger(ctx.retransmit_interval.uint16()));
  }

  @Override
  public void exitOspfai_transit_delay(Ospfai_transit_delayContext ctx) {
    _currentOspfInterface.setTransitDelay(toInteger(ctx.transit_delay.uint16()));
  }

  @Override
  public void exitOspfa_interface(Ospfa_interfaceContext ctx) {
    _currentOspfInterface = null;
  }

  @Override
  public void exitOspfai_dead_counts(Ospfai_dead_countsContext ctx) {
    _currentOspfInterface.setDeadCounts(toInteger(ctx.dead_counts.uint8()));
  }

  @Override
  public void enterOspfat_stub(Ospfat_stubContext ctx) {
    _currentOspfStubAreaType =
        Optional.ofNullable(_currentOspfArea.getTypeSettings())
            .filter(OspfAreaStub.class::isInstance)
            .map(OspfAreaStub.class::cast)
            .orElseGet(
                () -> {
                  // overwrite if missing or a different area type
                  OspfAreaStub newStubArea = new OspfAreaStub();
                  _currentOspfArea.setTypeSettings(newStubArea);
                  return newStubArea;
                });
  }

  @Override
  public void exitOspfat_stub(Ospfat_stubContext ctx) {
    _currentOspfStubAreaType = null;
  }

  @Override
  public void exitOspfat_normal(Ospfat_normalContext ctx) {
    _currentOspfArea.setTypeSettings(new OspfAreaNormal());
  }

  @Override
  public void exitOspfatsdr_disable(Ospfatsdr_disableContext ctx) {
    _currentOspfStubAreaType.setDefaultRouteDisable(true);
  }

  @Override
  public void exitOspfatsdr_advertise_metric(Ospfatsdr_advertise_metricContext ctx) {
    _currentOspfStubAreaType.setDefaultRouteMetric(toInteger(ctx.metric.uint8()));
  }

  @Override
  public void exitOspfats_accept_summary(Ospfats_accept_summaryContext ctx) {
    _currentOspfStubAreaType.setAcceptSummary(toBoolean(ctx.yn));
  }

  @Override
  public void enterOspfat_nssa(Ospfat_nssaContext ctx) {
    _currentOspfNssaAreaType =
        Optional.ofNullable(_currentOspfArea.getTypeSettings())
            .filter(OspfAreaNssa.class::isInstance)
            .map(OspfAreaNssa.class::cast)
            .orElseGet(
                () -> {
                  // overwrite if missing or a different area type
                  OspfAreaNssa newNssaArea = new OspfAreaNssa();
                  _currentOspfArea.setTypeSettings(newNssaArea);
                  return newNssaArea;
                });
  }

  @Override
  public void exitOspfat_nssa(Ospfat_nssaContext ctx) {
    _currentOspfNssaAreaType = null;
  }

  @Override
  public void exitOspfatndr_disable(Ospfatndr_disableContext ctx) {
    _currentOspfNssaAreaType.setDefaultRouteDisable(true);
  }

  @Override
  public void exitOspfatndra_metric(Ospfatndra_metricContext ctx) {
    _currentOspfNssaAreaType.setDefaultRouteMetric(toInteger(ctx.metric.uint8()));
  }

  @Override
  public void exitOspfatndra_type(Ospfatndra_typeContext ctx) {
    if (ctx.EXT_1() != null) {
      _currentOspfNssaAreaType.setDefaultRouteType(DefaultRouteType.EXT_1);
      return;
    }
    _currentOspfNssaAreaType.setDefaultRouteType(DefaultRouteType.EXT_2);
  }

  @Override
  public void exitOspfatn_accept_summary(Ospfatn_accept_summaryContext ctx) {
    _currentOspfNssaAreaType.setAcceptSummary(toBoolean(ctx.yn));
  }

  @Override
  public void exitOspf_enable(Ospf_enableContext ctx) {
    _currentOspfVr.setEnable(toBoolean(ctx.yn));
  }

  @Override
  public void exitOspf_reject_default_route(Ospf_reject_default_routeContext ctx) {
    _currentOspfVr.setRejectDefaultRoute(toBoolean(ctx.yn));
  }

  @Override
  public void exitOspf_router_id(Ospf_router_idContext ctx) {
    toIp(ctx, ctx.addr, "OSPF router-id").ifPresent(_currentOspfVr::setRouterId);
  }

  @Override
  public void exitOspf_graceful_restart(Ospf_graceful_restartContext ctx) {
    todo(ctx);
  }

  @Override
  public void enterPalo_alto_configuration(Palo_alto_configurationContext ctx) {
    _configuration = new PaloAltoConfiguration();
    _defaultVsys = _configuration.getVirtualSystems().computeIfAbsent(DEFAULT_VSYS_NAME, Vsys::new);
    _currentVsys = _defaultVsys;
  }

  @Override
  public void enterPanorama_pre_rulebase(Panorama_pre_rulebaseContext ctx) {
    _currentRuleScope = RulebaseId.PRE;
  }

  @Override
  public void exitPanorama_pre_rulebase(Panorama_pre_rulebaseContext ctx) {
    _currentRuleScope = null;
  }

  @Override
  public void enterPanorama_post_rulebase(Panorama_post_rulebaseContext ctx) {
    _currentRuleScope = RulebaseId.POST;
  }

  @Override
  public void exitPanorama_post_rulebase(Panorama_post_rulebaseContext ctx) {
    _currentRuleScope = null;
  }

  @Override
  public void enterS_address_definition(S_address_definitionContext ctx) {
    String name = getText(ctx.name);
    if (_currentVsys.getAddressGroups().get(name) != null) {
      warn(
          ctx,
          String.format(
              "Cannot have an address object and group with the same name '%s'. Ignoring the object definition.",
              name));
      _currentAddressObject = new AddressObject(name);
    } else {
      _currentAddressObject =
          _currentVsys.getAddressObjects().computeIfAbsent(name, AddressObject::new);

      // Use constructed name so same-named defs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), name);
      _configuration.defineFlattenedStructure(ADDRESS_OBJECT, uniqueName, ctx, _parser);
    }
  }

  @Override
  public void exitS_address_definition(S_address_definitionContext ctx) {
    _currentAddressObject = null;
  }

  @Override
  public void exitSa_tag(Sa_tagContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String tag = getText(var);
      _currentAddressObject.getTags().add(tag);
    }
  }

  @Override
  public void exitSag_tag(Sag_tagContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String tag = getText(var);
      _currentAddressGroup.getTags().add(tag);
    }
  }

  @Override
  public void exitSagd_filter(Sagd_filterContext ctx) {
    _currentAddressGroup.setFilter(getText(ctx.filter));
  }

  @Override
  public void enterS_tag(S_tagContext ctx) {
    String name = getText(ctx.name);
    _currentTag = _currentVsys.getTags().computeIfAbsent(name, Tag::new);
  }

  @Override
  public void exitS_tag(S_tagContext ctx) {
    _currentTag = null;
  }

  @Override
  public void exitSt_comments(St_commentsContext ctx) {
    _currentTag.setComments(getText(ctx.comments));
  }

  @Override
  public void enterS_address_group_definition(S_address_group_definitionContext ctx) {
    String name = getText(ctx.name);
    if (_currentVsys.getAddressObjects().get(name) != null) {
      warn(
          ctx,
          String.format(
              "Cannot have an address object and group with the same name '%s'. Ignoring the group definition.",
              name));
      _currentAddressGroup = new AddressGroup(name);
    } else {
      _currentAddressGroup =
          _currentVsys.getAddressGroups().computeIfAbsent(name, AddressGroup::new);

      // Use constructed name so same-named defs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), name);
      _configuration.defineFlattenedStructure(ADDRESS_GROUP, uniqueName, ctx, _parser);
    }
  }

  @Override
  public void exitS_address_group_definition(S_address_group_definitionContext ctx) {
    _currentAddressGroup = null;
  }

  @Override
  public void enterS_application_definition(S_application_definitionContext ctx) {
    String name = ctx.name.getText();
    _currentApplication =
        _currentVsys
            .getApplications()
            .computeIfAbsent(name, n -> Application.builder(name).build());
    // Use constructed name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    _configuration.defineFlattenedStructure(APPLICATION, uniqueName, ctx, _parser);
  }

  @Override
  public void exitS_application_definition(S_application_definitionContext ctx) {
    _currentApplication = null;
  }

  @Override
  public void enterSappg_definition(Sappg_definitionContext ctx) {
    String name = ctx.name.getText();
    _currentApplicationGroup =
        _currentVsys.getApplicationGroups().computeIfAbsent(name, ApplicationGroup::new);
    // Use constructed name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    _configuration.defineFlattenedStructure(APPLICATION_GROUP, uniqueName, ctx, _parser);
  }

  @Override
  public void exitSappg_definition(Sappg_definitionContext ctx) {
    _currentApplicationGroup = null;
  }

  @Override
  public void exitSappg_members(Sappg_membersContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentApplicationGroup.getMembers().add(name);
      String uniqueName = computeObjectName(_currentVsys.getName(), name);
      referenceApplicationLike(name, uniqueName, APPLICATION_GROUP_MEMBERS, var);
    }
  }

  @Override
  public void exitSapp_description(Sapp_descriptionContext ctx) {
    _currentApplication.setDescription(getText(ctx.description));
  }

  @Override
  public void enterS_zone_definition(S_zone_definitionContext ctx) {
    String name = getText(ctx.name);
    _currentZone = _currentVsys.getZones().computeIfAbsent(name, n -> new Zone(n, _currentVsys));

    // Use constructed zone name so same-named zone defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    _configuration.defineFlattenedStructure(ZONE, uniqueName, ctx, _parser);
  }

  @Override
  public void exitS_zone_definition(S_zone_definitionContext ctx) {
    _currentZone = null;
  }

  @Override
  public void exitSa_description(Sa_descriptionContext ctx) {
    _currentAddressObject.setDescription(getText(ctx.description));
  }

  @Override
  public void exitSa_fqdn(Sa_fqdnContext ctx) {
    warn(ctx, ctx.FQDN().getSymbol(), "FQDN in address objects is not currently supported");
  }

  @Override
  public void exitSa_ip_netmask(Sa_ip_netmaskContext ctx) {
    if (ctx.ip_address() != null) {
      _currentAddressObject.setIpSpace(toIp(ctx.ip_address()).toIpSpace());
    } else if (ctx.IP_PREFIX() != null) {
      _currentAddressObject.setIpSpace(Prefix.parse(getText(ctx.IP_PREFIX())).toIpSpace());
    } else {
      warn(ctx, "Cannot understand what follows 'ip-netmask'");
    }
  }

  @Override
  public void exitSa_ip_range(Sa_ip_rangeContext ctx) {
    String[] ips = getText(ctx.IP_RANGE()).split("-");
    _currentAddressObject.setIpSpace(IpRange.range(Ip.parse(ips[0]), Ip.parse(ips[1])));
  }

  @Override
  public void exitSag_description(Sag_descriptionContext ctx) {
    _currentAddressGroup.setDescription(getText(ctx.description));
  }

  @Override
  public void exitSag_static(Sag_staticContext ctx) {
    for (VariableContext var : ctx.variable()) {
      String objectName = getText(var);
      if (objectName.equals(_currentAddressGroup.getName())) {
        warn(ctx, String.format("The address group '%s' cannot contain itself", objectName));
      } else {
        _currentAddressGroup.addMember(objectName);

        // Use constructed name so same-named defs across vsys are unique
        String uniqueName = computeObjectName(_currentVsys.getName(), objectName);
        _configuration.referenceStructure(
            ADDRESS_GROUP_OR_ADDRESS_OBJECT, uniqueName, ADDRESS_GROUP_STATIC, getLine(var.start));
      }
    }
  }

  @Override
  public void enterSds_default_gateway(Sds_default_gatewayContext ctx) {
    _configuration.setMgmtIfaceGateway(toIp(ctx.ip_address()));
  }

  @Override
  public void exitSds_hostname(Sds_hostnameContext ctx) {
    _configuration.setHostname(getText(ctx.name));
  }

  @Override
  public void enterSds_ip_address(Sds_ip_addressContext ctx) {
    _configuration.setMgmtIfaceAddress(toIp(ctx.ip_address()));
  }

  @Override
  public void enterSds_netmask(Sds_netmaskContext ctx) {
    _configuration.setMgmtIfaceNetmask(toIp(ctx.ip_address()));
  }

  @Override
  public void enterSds_ntp_servers(Sds_ntp_serversContext ctx) {
    _currentNtpServerPrimary = ctx.PRIMARY_NTP_SERVER() != null;
  }

  @Override
  public void exitSdsn_ntp_server_address(Sdsn_ntp_server_addressContext ctx) {
    if (_currentNtpServerPrimary) {
      _configuration.setNtpServerPrimary(getText(ctx.address));
    } else {
      _configuration.setNtpServerSecondary(getText(ctx.address));
    }
  }

  @Override
  public void exitSdsd_servers(Sdsd_serversContext ctx) {
    if (ctx.primary_name != null) {
      _configuration.setDnsServerPrimary(getText(ctx.primary_name));
    } else if (ctx.secondary_name != null) {
      _configuration.setDnsServerSecondary(getText(ctx.secondary_name));
    }
  }

  @Override
  public void enterSn_shared_gateway_definition(Sn_shared_gateway_definitionContext ctx) {
    String name = getText(ctx.name);
    _currentVsys = _configuration.getSharedGateways().computeIfAbsent(name, Vsys::new);
    _configuration.defineFlattenedStructure(SHARED_GATEWAY, name, ctx, _parser);
  }

  @Override
  public void exitSn_shared_gateway_definition(Sn_shared_gateway_definitionContext ctx) {
    _currentVsys = null;
  }

  @Override
  public void exitSnsg_display_name(Snsg_display_nameContext ctx) {
    _currentVsys.setDisplayName(getText(ctx.name));
  }

  @Override
  public void exitSnsgi_interface(Snsgi_interfaceContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentVsys.getImportedInterfaces().add(name);
      _configuration.referenceStructure(INTERFACE, name, IMPORT_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void enterSnsg_zone_definition(Snsg_zone_definitionContext ctx) {
    String name = getText(ctx.name);
    _currentZone = _currentVsys.getZones().computeIfAbsent(name, n -> new Zone(n, _currentVsys));

    // Use constructed zone name so same-named zone defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    _configuration.defineFlattenedStructure(ZONE, uniqueName, ctx, _parser);
  }

  @Override
  public void exitSnsg_zone_definition(Snsg_zone_definitionContext ctx) {
    _currentZone = null;
  }

  @Override
  public void exitSnsgzn_layer3(Snsgzn_layer3Context ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      if (_currentVsys.getImportedInterfaces().contains(name)) {
        _currentZone.getInterfaceNames().add(name);
      } else {
        warn(ctx, "Cannot add an interface to a shared-gateway zone before it is imported");
      }
      _configuration.referenceStructure(INTERFACE, name, ZONE_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void enterSet_line_config_devices(Set_line_config_devicesContext ctx) {
    if (ctx.name != null) {
      String deviceName = getText(ctx.name);
      _currentDeviceName = firstNonNull(_currentDeviceName, deviceName);
      if (!_currentDeviceName.equals(deviceName)) {
        /* Do not currently handle multiple device names, which presumably happens only if multiple
         * physical devices are configured from a single config */
        warn(ctx, "Multiple devices encountered: " + deviceName);
      }
    }
  }

  @Override
  public void enterS_policy_panorama(S_policy_panoramaContext ctx) {
    _currentVsys = _configuration.getPanorama();
    if (_currentVsys == null) {
      _currentVsys = new Vsys(PANORAMA_VSYS_NAME, NamespaceType.PANORAMA);
      _configuration.setPanorama(_currentVsys);
    }
  }

  @Override
  public void exitS_policy_panorama(S_policy_panoramaContext ctx) {
    _currentVsys = _defaultVsys;
  }

  @Override
  public void enterVr_definition(Vr_definitionContext ctx) {
    String name = getText(ctx.name);
    _currentVirtualRouter =
        _configuration.getVirtualRouters().computeIfAbsent(name, VirtualRouter::new);
    _configuration.defineFlattenedStructure(VIRTUAL_ROUTER, name, ctx, _parser);
    _configuration.referenceStructure(
        VIRTUAL_ROUTER, name, VIRTUAL_ROUTER_SELF_REFERENCE, getLine(ctx.name.getStart()));
  }

  @Override
  public void exitVr_definition(Vr_definitionContext ctx) {
    _currentVirtualRouter = null;
  }

  @Override
  public void enterSni_ethernet_definition(Sni_ethernet_definitionContext ctx) {
    String name = getText(ctx.name);
    _currentParentInterface =
        _configuration
            .getInterfaces()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.PHYSICAL));
    _currentInterface = _currentParentInterface;
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSni_ethernet_definition(Sni_ethernet_definitionContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSni_loopback(Sni_loopbackContext ctx) {
    String name = getText(ctx.LOOPBACK());
    _currentParentInterface =
        _configuration
            .getInterfaces()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.LOOPBACK));
    _currentInterface = _currentParentInterface;
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSni_loopback(Sni_loopbackContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSni_tunnel(Sni_tunnelContext ctx) {
    String name = getText(ctx.TUNNEL());
    _currentParentInterface =
        _configuration
            .getInterfaces()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.TUNNEL));
    _currentInterface = _currentParentInterface;
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSni_tunnel(Sni_tunnelContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSni_vlan(Sni_vlanContext ctx) {
    String name = getText(ctx.VLAN());
    _currentParentInterface =
        _configuration
            .getInterfaces()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.VLAN));
    _currentInterface = _currentParentInterface;
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSni_vlan(Sni_vlanContext ctx) {
    _currentParentInterface = null;
    _currentInterface = null;
  }

  @Override
  public void enterSnicp_global_protect(Snicp_global_protectContext ctx) {
    String name = getText(ctx.name);
    _currentCrytoProfile = _configuration.getCryptoProfileOrCreate(name, Type.GLOBAL_PROTECT_APP);
  }

  @Override
  public void exitSnicp_global_protect(Snicp_global_protectContext ctx) {
    _currentCrytoProfile = null;
  }

  @Override
  public void enterSnicp_ike_crypto_profiles(Snicp_ike_crypto_profilesContext ctx) {
    String name = getText(ctx.name);
    _currentCrytoProfile = _configuration.getCryptoProfileOrCreate(name, Type.IKE);
  }

  @Override
  public void exitSnicp_ike_crypto_profiles(Snicp_ike_crypto_profilesContext ctx) {
    _currentCrytoProfile = null;
  }

  @Override
  public void enterSnicp_ipsec_crypto_profiles(Snicp_ipsec_crypto_profilesContext ctx) {
    String name = getText(ctx.name);
    _currentCrytoProfile = _configuration.getCryptoProfileOrCreate(name, Type.IPSEC);
  }

  @Override
  public void exitSnicp_ipsec_crypto_profiles(Snicp_ipsec_crypto_profilesContext ctx) {
    _currentCrytoProfile = null;
  }

  @Override
  public void exitSnie_link_state(Snie_link_stateContext ctx) {
    _currentInterface.setActive((ctx.DOWN() == null));
  }

  @Override
  public void enterSniel2_unit(Sniel2_unitContext ctx) {
    String name = getText(ctx.name);
    _currentInterface =
        _currentParentInterface
            .getUnits()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.LAYER2));
    _currentInterface.setParent(_currentParentInterface);
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSniel2_unit(Sniel2_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void exitSniel3_ip(Sniel3_ipContext ctx) {
    ConcreteInterfaceAddress address = toInterfaceAddress(ctx.address);
    _currentInterface.addAddress(address);
  }

  @Override
  public void exitSniel3_mtu(Sniel3_mtuContext ctx) {
    _currentInterface.setMtu(Integer.parseInt(getText(ctx.mtu)));
  }

  @Override
  public void enterSniel3_unit(Sniel3_unitContext ctx) {
    String name = getText(ctx.name);
    _currentInterface =
        _currentParentInterface
            .getUnits()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.LAYER3));
    _currentInterface.setParent(_currentParentInterface);
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSniel3_unit(Sniel3_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void exitSnil_ip(Snil_ipContext ctx) {
    ConcreteInterfaceAddress address = toInterfaceAddress(ctx.address);
    if (address.getPrefix().getPrefixLength() != Prefix.MAX_PREFIX_LENGTH) {
      warn(ctx, ctx.address, "Loopback ip address must be /32 or without mask");
      return;
    }
    _currentInterface.addAddress(address);
  }

  @Override
  public void enterSnil_unit(Snil_unitContext ctx) {
    String name = getText(ctx.name);
    _currentInterface =
        _currentParentInterface
            .getUnits()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.LOOPBACK));
    _currentInterface.setParent(_currentParentInterface);
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSnil_unit(Snil_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void enterSnit_unit(Snit_unitContext ctx) {
    String name = getText(ctx.name);
    _currentInterface =
        _currentParentInterface
            .getUnits()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.TUNNEL));
    _currentInterface.setParent(_currentParentInterface);
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSnit_unit(Snit_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void enterSniv_unit(Sniv_unitContext ctx) {
    String name = getText(ctx.name);
    _currentInterface =
        _currentParentInterface
            .getUnits()
            .computeIfAbsent(name, n -> new Interface(n, Interface.Type.VLAN));
    _currentInterface.setParent(_currentParentInterface);
    _configuration.defineFlattenedStructure(INTERFACE, name, ctx, _parser);
  }

  @Override
  public void exitSniv_unit(Sniv_unitContext ctx) {
    _currentInterface = _currentParentInterface;
  }

  @Override
  public void exitVrad_ebgp(Vrad_ebgpContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setEbgp);
  }

  @Override
  public void exitVrad_ibgp(Vrad_ibgpContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setIbgp);
  }

  @Override
  public void exitVrad_ospf_ext(Vrad_ospf_extContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setOspfExt);
  }

  @Override
  public void exitVrad_ospf_int(Vrad_ospf_intContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setOspfInt);
  }

  @Override
  public void exitVrad_ospfv3_ext(Vrad_ospfv3_extContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setOspfV3Ext);
  }

  @Override
  public void exitVrad_ospfv3_int(Vrad_ospfv3_intContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setOspfV3Int);
  }

  @Override
  public void exitVrad_rip(Vrad_ripContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setRip);
  }

  @Override
  public void exitVrad_static(Vrad_staticContext ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setStatic);
  }

  @Override
  public void exitVrad_static_ipv6(Vrad_static_ipv6Context ctx) {
    toInteger(ctx, ctx.ad).ifPresent(_currentVirtualRouter.getAdminDists()::setStaticv6);
  }

  @Override
  public void enterVr_routing_table(Vr_routing_tableContext ctx) {
    _currentStaticRoute =
        _currentVirtualRouter
            .getStaticRoutes()
            .computeIfAbsent(getText(ctx.name), StaticRoute::new);
  }

  @Override
  public void exitVr_interface(Vr_interfaceContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentVirtualRouter.getInterfaceNames().add(name);
      _configuration.referenceStructure(
          INTERFACE, name, VIRTUAL_ROUTER_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void exitVr_routing_table(Vr_routing_tableContext ctx) {
    _currentStaticRoute = null;
  }

  @Override
  public void enterVrp_bgp(Vrp_bgpContext ctx) {
    _currentBgpVr = _currentVirtualRouter.getOrCreateBgp();
  }

  @Override
  public void enterVrp_ospf(Vrp_ospfContext ctx) {
    _currentOspfVr = _currentVirtualRouter.getOrCreateOspf();
  }

  @Override
  public void exitVrp_ospf(Vrp_ospfContext ctx) {
    _currentOspfVr = null;
  }

  @Override
  public void exitVrp_bgp(Vrp_bgpContext ctx) {
    _currentBgpVr = null;
  }

  @Override
  public void exitVrrt_admin_dist(Vrrt_admin_distContext ctx) {
    toInteger(ctx, ctx.distance).ifPresent(_currentStaticRoute::setAdminDistance);
  }

  @Override
  public void exitVrrt_destination(Vrrt_destinationContext ctx) {
    _currentStaticRoute.setDestination(Prefix.parse(getText(ctx.destination)));
  }

  @Override
  public void exitVrrt_interface(Vrrt_interfaceContext ctx) {
    String name = getText(ctx.iface);
    _currentStaticRoute.setNextHopInterface(name);
    _configuration.referenceStructure(
        INTERFACE, name, STATIC_ROUTE_INTERFACE, getLine(ctx.iface.start));
  }

  @Override
  public void exitVrrt_metric(Vrrt_metricContext ctx) {
    _currentStaticRoute.setMetric(Integer.parseInt(getText(ctx.metric)));
  }

  @Override
  public void exitVrrtn_ip(Vrrtn_ipContext ctx) {
    toIp(ctx, ctx.addr, "static route nexthop ip-address")
        .ifPresent(_currentStaticRoute::setNextHopIp);
  }

  @Override
  public void exitVrrtn_next_vr(Vrrtn_next_vrContext ctx) {
    String name = getText(ctx.name);
    _currentStaticRoute.setNextVr(name);
    _configuration.referenceStructure(
        VIRTUAL_ROUTER, name, STATIC_ROUTE_NEXT_VR, getLine(ctx.name.start));
  }

  @Override
  public void enterSrs_definition(Srs_definitionContext ctx) {
    String name = getText(ctx.name);
    Map<String, Rule> rulebase;
    if (_currentRuleScope == RulebaseId.DEFAULT) {
      rulebase = _currentVsys.getRules();
    } else if (_currentRuleScope == RulebaseId.PRE) {
      rulebase = _currentVsys.getPreRules();
    } else {
      assert _currentRuleScope == RulebaseId.POST;
      rulebase = _currentVsys.getPostRules();
    }
    _currentRule = rulebase.computeIfAbsent(name, n -> new Rule(n, _currentVsys));

    // Use constructed name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    _configuration.defineFlattenedStructure(RULE, uniqueName, ctx, _parser);
    _configuration.referenceStructure(RULE, uniqueName, RULE_SELF_REF, getLine(ctx.name.start));
  }

  @Override
  public void exitSrs_definition(Srs_definitionContext ctx) {
    _currentRule = null;
  }

  @Override
  public void exitSrs_action(Srs_actionContext ctx) {
    if (ctx.ALLOW() != null) {
      _currentRule.setAction(LineAction.PERMIT);
    } else {
      _currentRule.setAction(LineAction.DENY);
    }
  }

  private void referenceApplicationLike(
      String name, String uniqueName, PaloAltoStructureUsage usage, ParserRuleContext var) {
    PaloAltoStructureType type =
        name.equals(CATCHALL_APPLICATION_NAME)
                || ApplicationBuiltIn.getBuiltInApplication(name).isPresent()
            /*
             * Since the name matches a builtin, we'll add a reference if the user defined
             * over the builtin, but it's okay if they did not.
             */
            ? APPLICATION_GROUP_OR_APPLICATION_OR_NONE
            /* This is not a pre-defined name, the application must be defined in config. */
            : APPLICATION_GROUP_OR_APPLICATION;
    _configuration.referenceStructure(type, uniqueName, usage, getLine(var.start));
  }

  @Override
  public void exitSrs_application(Srs_applicationContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentRule.getApplications().add(name);
      // Use constructed object name so same-named refs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), name);
      referenceApplicationLike(name, uniqueName, RULE_APPLICATION, var);
    }
  }

  @Override
  public void exitSrs_description(Srs_descriptionContext ctx) {
    _currentRule.setDescription(getText(ctx.description));
  }

  @Override
  public void exitSrs_destination(Srs_destinationContext ctx) {
    for (Src_or_dst_list_itemContext var : ctx.src_or_dst_list().src_or_dst_list_item()) {
      RuleEndpoint endpoint = toRuleEndpoint(var);
      _currentRule.getDestination().add(endpoint);

      // Use constructed object name so same-named refs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), endpoint.getValue());

      // At this time, don't know if something that looks like a constant (e.g. IP address) is a
      // reference or not.  So mark a reference to a very permissive abstract structure type.
      PaloAltoStructureType type = ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE;
      if (endpoint.getType() == RuleEndpoint.Type.REFERENCE) {
        // We know this reference doesn't look like a valid constant, so it must be pointing to an
        // object/group
        type = ADDRESS_GROUP_OR_ADDRESS_OBJECT;
      }
      _configuration.referenceStructure(type, uniqueName, RULE_DESTINATION, getLine(var.start));
    }
  }

  @Override
  public void exitSrs_disabled(Srs_disabledContext ctx) {
    _currentRule.setDisabled(toBoolean(ctx.yn));
  }

  @Override
  public void exitSrs_from(Srs_fromContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String zoneName = getText(var);
      _currentRule.getFrom().add(zoneName);

      if (!zoneName.equals(CATCHALL_ZONE_NAME)) {
        // Use constructed object name so same-named refs across vsys are unique
        String uniqueName = computeObjectName(_currentVsys.getName(), zoneName);
        _configuration.referenceStructure(ZONE, uniqueName, RULE_FROM_ZONE, getLine(var.start));
      }
    }
  }

  @Override
  public void exitSrs_negate_destination(Srs_negate_destinationContext ctx) {
    _currentRule.setNegateDestination(toBoolean(ctx.yn));
  }

  @Override
  public void exitSrs_negate_source(Srs_negate_sourceContext ctx) {
    _currentRule.setNegateSource(toBoolean(ctx.yn));
  }

  @Override
  public void exitSrs_service(Srs_serviceContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String serviceName = getText(var);
      _currentRule.getService().add(new ServiceOrServiceGroupReference(serviceName));
      referenceService(var, RULEBASE_SERVICE);
    }
  }

  @Override
  public void exitSrs_source(Srs_sourceContext ctx) {
    for (Src_or_dst_list_itemContext var : ctx.src_or_dst_list().src_or_dst_list_item()) {
      RuleEndpoint endpoint = toRuleEndpoint(var);
      _currentRule.getSource().add(endpoint);
      // Use constructed object name so same-named refs across vsys are unique
      String uniqueName = computeObjectName(_currentVsys.getName(), endpoint.getValue());

      // At this time, don't know if something that looks like a constant (e.g. IP address) is a
      // reference or not.  So mark a reference to a very permissive abstract structure type.
      PaloAltoStructureType type = ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE;
      if (endpoint.getType() == RuleEndpoint.Type.REFERENCE) {
        type = ADDRESS_GROUP_OR_ADDRESS_OBJECT;
      }
      _configuration.referenceStructure(type, uniqueName, RULE_SOURCE, getLine(var.start));
    }
  }

  @Override
  public void exitSrs_to(Srs_toContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String zoneName = getText(var);
      _currentRule.getTo().add(zoneName);

      if (!zoneName.equals(CATCHALL_ZONE_NAME)) {
        // Use constructed object name so same-named refs across vsys are unique
        String uniqueName = computeObjectName(_currentVsys.getName(), zoneName);
        _configuration.referenceStructure(ZONE, uniqueName, RULE_TO_ZONE, getLine(var.start));
      }
    }
  }

  @Override
  public void enterS_rulebase(S_rulebaseContext ctx) {
    _currentRuleScope = RulebaseId.DEFAULT;
  }

  @Override
  public void exitS_rulebase(S_rulebaseContext ctx) {
    _currentRuleScope = null;
  }

  @Override
  public void enterS_service_definition(S_service_definitionContext ctx) {
    String name = getText(ctx.name);
    _currentService = _currentVsys.getServices().computeIfAbsent(name, Service::new);

    // Use constructed service name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    _configuration.defineFlattenedStructure(
        PaloAltoStructureType.SERVICE, uniqueName, ctx, _parser);
  }

  @Override
  public void exitS_service_definition(S_service_definitionContext ctx) {
    _currentService = null;
  }

  @Override
  public void exitSserv_description(Sserv_descriptionContext ctx) {
    _currentService.setDescription(getText(ctx.description));
  }

  @Override
  public void exitSserv_port(Sserv_portContext ctx) {
    for (Port_or_rangeContext item : ctx.variable_port_list().port_or_range()) {
      if (item.port != null) {
        _currentService.addPort(toInteger(item.port));
      } else {
        assert item.range != null;
        _currentService.addPorts(new SubRange(getText(item.range)));
      }
    }
  }

  @Override
  public void exitSserv_protocol(Sserv_protocolContext ctx) {
    if (ctx.SCTP() != null) {
      _currentService.setProtocol(IpProtocol.SCTP);
    } else if (ctx.TCP() != null) {
      _currentService.setProtocol(IpProtocol.TCP);
    } else if (ctx.UDP() != null) {
      _currentService.setProtocol(IpProtocol.UDP);
    }
  }

  @Override
  public void exitSserv_source_port(Sserv_source_portContext ctx) {
    for (Port_or_rangeContext item : ctx.variable_port_list().port_or_range()) {
      if (item.port != null) {
        _currentService.addSourcePort(toInteger(item.port));
      } else {
        assert item.range != null;
        _currentService.addSourcePorts(new SubRange(getText(item.range)));
      }
    }
  }

  @Override
  public void enterS_service_group_definition(S_service_group_definitionContext ctx) {
    String name = getText(ctx.name);
    _currentServiceGroup = _currentVsys.getServiceGroups().computeIfAbsent(name, ServiceGroup::new);

    // Use constructed service-group name so same-named defs across vsys are unique
    String uniqueName = computeObjectName(_currentVsys.getName(), name);
    _configuration.defineFlattenedStructure(SERVICE_GROUP, uniqueName, ctx, _parser);
  }

  @Override
  public void exitS_service_group_definition(S_service_group_definitionContext ctx) {
    _currentServiceGroup = null;
  }

  @Override
  public void exitSservgrp_members(Sservgrp_membersContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentServiceGroup.getReferences().add(new ServiceOrServiceGroupReference(name));
      referenceService(var, SERVICE_GROUP_MEMBER);
    }
  }

  @Override
  public void enterS_shared(S_sharedContext ctx) {
    _currentVsys = _configuration.getShared();
    if (_currentVsys == null) {
      _currentVsys = new Vsys(SHARED_VSYS_NAME, NamespaceType.SHARED);
      _configuration.setShared(_currentVsys);
    }
  }

  @Override
  public void exitS_shared(S_sharedContext ctx) {
    _currentVsys = _defaultVsys;
  }

  @Override
  public void enterSsl_syslog(Ssl_syslogContext ctx) {
    _currentSyslogServerGroupName = getText(ctx.name);
  }

  @Override
  public void exitSsl_syslog(Ssl_syslogContext ctx) {
    _currentSyslogServerGroupName = null;
  }

  @Override
  public void enterSsls_server(Ssls_serverContext ctx) {
    _currentSyslogServer =
        _currentVsys.getSyslogServer(_currentSyslogServerGroupName, getText(ctx.name));
  }

  @Override
  public void exitSsls_server(Ssls_serverContext ctx) {
    _currentSyslogServer = null;
  }

  @Override
  public void exitSslss_server(Sslss_serverContext ctx) {
    _currentSyslogServer.setAddress(getText(ctx.address));
  }

  @Override
  public void enterS_vsys_definition(S_vsys_definitionContext ctx) {
    _currentVsys = _configuration.getVirtualSystems().computeIfAbsent(getText(ctx.name), Vsys::new);
  }

  @Override
  public void exitS_vsys_definition(S_vsys_definitionContext ctx) {
    _currentVsys = _defaultVsys;
  }

  @Override
  public void exitSvi_visible_vsys(Svi_visible_vsysContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentVsys.getImportedVsyses().add(name);
    }
  }

  @Override
  public void exitSvin_interface(Svin_interfaceContext ctx) {
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentVsys.getImportedInterfaces().add(name);
      _configuration.referenceStructure(INTERFACE, name, VSYS_IMPORT_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void exitSzn_external(Szn_externalContext ctx) {
    _currentZone.setType(EXTERNAL);
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentZone.getExternalNames().add(name);
    }
  }

  @Override
  public void exitSzn_layer2(Szn_layer2Context ctx) {
    _currentZone.setType(LAYER2);
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentZone.getInterfaceNames().add(name);
      _configuration.referenceStructure(INTERFACE, name, ZONE_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void exitSzn_layer3(Szn_layer3Context ctx) {
    _currentZone.setType(LAYER3);
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentZone.getInterfaceNames().add(name);
      _configuration.referenceStructure(INTERFACE, name, ZONE_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void exitSzn_tap(Szn_tapContext ctx) {
    _currentZone.setType(TAP);
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentZone.getInterfaceNames().add(name);
      _configuration.referenceStructure(INTERFACE, name, ZONE_INTERFACE, getLine(var.start));
    }
  }

  @Override
  public void exitSzn_virtual_wire(Szn_virtual_wireContext ctx) {
    _currentZone.setType(VIRTUAL_WIRE);
    for (Variable_list_itemContext var : variables(ctx.variable_list())) {
      String name = getText(var);
      _currentZone.getInterfaceNames().add(name);
      _configuration.referenceStructure(INTERFACE, name, ZONE_INTERFACE, getLine(var.start));
    }
  }

  public PaloAltoConfiguration getConfiguration() {
    return _configuration;
  }

  private static boolean toBoolean(Yes_or_noContext ctx) {
    if (ctx.YES() != null) {
      return true;
    }
    assert ctx.NO() != null;
    return false;
  }

  private static @Nonnull ConcreteInterfaceAddress toInterfaceAddress(
      Interface_addressContext ctx) {
    if (ctx.addr != null) {
      // PAN allows implicit /32 in lots of places.
      return ConcreteInterfaceAddress.create(toIp(ctx.addr), Prefix.MAX_PREFIX_LENGTH);
    }
    return ConcreteInterfaceAddress.parse(ctx.getText());
  }

  private static @Nonnull Ip toIp(Ip_addressContext ctx) {
    return Ip.parse(ctx.getText());
  }

  private @Nonnull Optional<Ip> toIp(
      ParserRuleContext ctx, Ip_address_or_slash32Context addr, String ipType) {
    ConcreteInterfaceAddress ip = toInterfaceAddress(addr.addr);
    if (ip.getNetworkBits() != Prefix.MAX_PREFIX_LENGTH) {
      warn(ctx, addr, String.format("Expecting 32-bit mask for %s, ignoring", ipType));
      return Optional.empty();
    }
    return Optional.of(ip.getIp());
  }

  /////////////////////////////////////////
  ///// Range-aware type conversions. /////
  /////////////////////////////////////////

  private int toInteger(Port_numberContext ctx) {
    return toInteger(ctx.uint16());
  }

  private int toInteger(Uint8Context t) {
    return Integer.parseInt(getText(t));
  }

  private int toInteger(Uint16Context t) {
    return Integer.parseInt(getText(t));
  }

  private static final IntegerSpace PORT_NUMBER_SPACE = IntegerSpace.of(Range.closed(0, 65535));

  private static final IntegerSpace PROTOCOL_ADMIN_DISTANCE_SPACE =
      IntegerSpace.of(Range.closed(10, 240));

  private Optional<Integer> toInteger(ParserRuleContext ctx, Port_numberContext pn) {
    return toIntegerInSpace(ctx, pn, PORT_NUMBER_SPACE, "admin-dist");
  }

  private Optional<Integer> toInteger(ParserRuleContext ctx, Protocol_adContext ad) {
    return toIntegerInSpace(ctx, ad, PROTOCOL_ADMIN_DISTANCE_SPACE, "admin-dist");
  }

  private static final IntegerSpace VLAN_TAG_SPACE = IntegerSpace.of(Range.closed(1, 4094));

  private Optional<Integer> toInteger(ParserRuleContext ctx, Vlan_tagContext vlan) {
    return toIntegerInSpace(ctx, vlan, VLAN_TAG_SPACE, "vlan/tag");
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

  private static final LongSpace BGP_2_BYTE_ASN_SPACE = LongSpace.of(Range.closed(1L, 65535L));
  private static final LongSpace BGP_4_BYTE_ASN_SPACE = LongSpace.of(Range.closed(1L, 4294967295L));

  private Optional<Long> toAsn(ParserRuleContext ctx, Bgp_asnContext asn, AsFormat fmt) {
    if (fmt == AsFormat.TWO_BYTE_AS) {
      return toLongInSpace(ctx, asn, BGP_2_BYTE_ASN_SPACE, "2-byte BGP AS number");
    } else {
      assert fmt == AsFormat.FOUR_BYTE_AS;
      return toLongInSpace(ctx, asn, BGP_4_BYTE_ASN_SPACE, "4-byte BGP AS number");
    }
  }

  private long toLong(Uint32Context t) {
    return Long.parseLong(getText(t));
  }

  private static long toLong(Bgp_local_prefContext ctx) {
    return Long.parseLong(ctx.getText());
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
      warn(
          messageCtx,
          ctx,
          String.format("Expected %s in range %s, but got '%d'", name, space, num));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  private static final IntegerSpace BGP_PEER_GROUP_NAME_LENGTH_SPACE =
      IntegerSpace.of(Range.closed(1, 31));

  private @Nonnull Optional<String> toString(ParserRuleContext ctx, Bgp_peer_group_nameContext pg) {
    return toStringWithLengthInSpace(ctx, pg, BGP_PEER_GROUP_NAME_LENGTH_SPACE, "bgp peer-group");
  }

  private static final IntegerSpace BGP_PEER_NAME_LENGTH_SPACE =
      IntegerSpace.of(Range.closed(1, 31));

  private @Nonnull Optional<String> toString(ParserRuleContext ctx, Bgp_peer_nameContext peer) {
    return toStringWithLengthInSpace(ctx, peer, BGP_PEER_NAME_LENGTH_SPACE, "bgp peer");
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

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    String lineText = getText(errorNode).replace("\n", "").replace("\r", "").trim();
    int line = getLine(token);
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

  private void todo(ParserRuleContext ctx) {
    _w.todo(ctx, getFullText(ctx), _parser);
  }

  private void warn(ParserRuleContext ctx, String message) {
    _w.addWarning(ctx, getFullText(ctx), _parser, message);
  }

  private void warn(ParserRuleContext ctx, ParserRuleContext warnCtx, String message) {
    warn(ctx, warnCtx.start, message);
  }

  private void warn(ParserRuleContext ctx, Token warnToken, String message) {
    _w.addWarningOnLine(_parser.getLine(warnToken), ctx, getFullText(ctx), _parser, message);
  }
}
