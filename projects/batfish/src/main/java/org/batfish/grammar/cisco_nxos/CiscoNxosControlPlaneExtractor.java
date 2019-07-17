package org.batfish.grammar.cisco_nxos;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.getCanonicalInterfaceNamePrefix;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.ETHERNET;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.LOOPBACK;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.MGMT;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.PORT_CHANNEL;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.VLAN;
import static org.batfish.representation.cisco_nxos.Interface.VLAN_RANGE;
import static org.batfish.representation.cisco_nxos.Interface.newNonVlanInterface;
import static org.batfish.representation.cisco_nxos.Interface.newVlanInterface;
import static org.batfish.representation.cisco_nxos.StaticRoute.STATIC_ROUTE_PREFERENCE_RANGE;
import static org.batfish.representation.cisco_nxos.StaticRoute.STATIC_ROUTE_TRACK_RANGE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.common.WellKnownCommunity;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.bgp.RouteDistinguisher;
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
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Bgp_asnContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Channel_idContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Cisco_nxos_configurationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Dscp_numberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Dscp_specContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_bandwidthContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_channel_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_descriptionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_encapsulationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_ip_addressContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_mtuContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_no_autostateContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_no_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_no_switchportContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_accessContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_trunk_allowedContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_switchport_trunk_nativeContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.I_vrf_memberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Icl_standardContext;
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
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Last_as_num_prependsContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Line_actionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Literal_standard_communityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nve_memberContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nve_no_shutdownContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nve_source_interfaceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvg_ingress_replicationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvg_mcast_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvg_suppress_arpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvm_ingress_replicationContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvm_mcast_groupContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Nvm_suppress_arpContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Packet_lengthContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Pl_actionContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Pl_descriptionContext;
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
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rms_tagContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsapp_last_asContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsapp_literalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsipnh_literalContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Rmsipnh_unchangedContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_distinguisherContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_distinguisher_or_autoContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_map_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_map_sequenceContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Route_networkContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_hostnameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_interface_nveContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_interface_regularContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_route_mapContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.S_vrf_contextContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Standard_communityContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Static_route_nameContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Static_route_prefContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Subnet_maskContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Tcp_flags_maskContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Tcp_portContext;
import org.batfish.grammar.cisco_nxos.CiscoNxosParser.Tcp_port_numberContext;
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
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureUsage;
import org.batfish.representation.cisco_nxos.FragmentsBehavior;
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
import org.batfish.representation.cisco_nxos.Nve.IngressReplicationProtocol;
import org.batfish.representation.cisco_nxos.NveVni;
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
  private static final IntegerSpace DSCP_RANGE = IntegerSpace.of(Range.closed(0, 63));
  private static final IntegerSpace INTERFACE_DESCRIPTION_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 254));
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
  private static final int MAX_VRF_NAME_LENGTH = 32;
  private static final IntegerSpace NUM_AS_PATH_PREPENDS_RANGE =
      IntegerSpace.of(Range.closed(1, 10));
  private static final IntegerSpace PACKET_LENGTH_RANGE = IntegerSpace.of(Range.closed(20, 9210));
  private static final IntegerSpace PORT_CHANNEL_RANGE = IntegerSpace.of(Range.closed(1, 4096));
  private static final IntegerSpace ROUTE_MAP_ENTRY_SEQUENCE_RANGE =
      IntegerSpace.of(Range.closed(0, 65535));
  private static final IntegerSpace ROUTE_MAP_NAME_LENGTH_RANGE =
      IntegerSpace.of(Range.closed(1, 63));
  private static final IntegerSpace TCP_FLAGS_MASK_RANGE = IntegerSpace.of(Range.closed(0, 63));
  private static final IntegerSpace TCP_PORT_RANGE = IntegerSpace.of(Range.closed(0, 65535));
  private static final IntegerSpace UDP_PORT_RANGE = IntegerSpace.of(Range.closed(0, 65535));
  private static final IntegerSpace VNI_RANGE = IntegerSpace.of(Range.closed(0, 16777214));

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

  private CiscoNxosConfiguration _configuration;
  private ActionIpAccessListLine.Builder _currentActionIpAccessListLineBuilder;
  private Boolean _currentActionIpAccessListLineUnusable;
  private List<Interface> _currentInterfaces;
  private IpAccessList _currentIpAccessList;
  private Optional<Long> _currentIpAccessListLineNum;
  private IpPrefixList _currentIpPrefixList;
  private Layer3Options.Builder _currentLayer3OptionsBuilder;
  private List<Nve> _currentNves;
  private List<NveVni> _currentNveVnis;
  private RouteMapEntry _currentRouteMapEntry;
  private TcpFlags.Builder _currentTcpFlagsBuilder;
  private TcpOptions.Builder _currentTcpOptionsBuilder;
  private UdpOptions.Builder _currentUdpOptionsBuilder;
  private IntegerSpace _currentValidVlanRange;
  private List<Vlan> _currentVlans;
  private Vrf _currentVrf;

  private @Nonnull final CiscoNxosCombinedParser _parser;
  private @Nonnull final String _text;
  private @Nonnull final Warnings _w;

  public CiscoNxosControlPlaneExtractor(
      String text, CiscoNxosCombinedParser parser, Warnings warnings) {
    _text = text;
    _parser = parser;
    _w = warnings;
  }

  private boolean checkPortChannelCompatibilitySettings(Interface referenceIface, Interface iface) {
    return Objects.equals(iface.getAccessVlan(), referenceIface.getAccessVlan())
        && Objects.equals(iface.getAllowedVlans(), referenceIface.getAllowedVlans())
        && Objects.equals(iface.getNativeVlan(), referenceIface.getNativeVlan())
        && iface.getSwitchportMode() == referenceIface.getSwitchportMode();
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

  private void copyPortChannelCompatibilitySettings(Interface referenceIface, Interface iface) {
    iface.setAccessVlan(referenceIface.getAccessVlan());
    iface.setAllowedVlans(referenceIface.getAllowedVlans());
    iface.setNativeVlan(referenceIface.getNativeVlan());
    iface.setSwitchportMode(referenceIface.getSwitchportMode());
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
  public void enterCisco_nxos_configuration(Cisco_nxos_configurationContext ctx) {
    _configuration = new CiscoNxosConfiguration();
    _currentValidVlanRange = VLAN_RANGE.difference(_configuration.getReservedVlanRange());
    _currentVrf = _configuration.getDefaultVrf();
  }

  @Override
  public void enterIcl_standard(Icl_standardContext ctx) {
    int line = ctx.getStart().getLine();
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
                  _configuration.defineStructure(
                      CiscoNxosStructureType.IP_COMMUNITY_LIST_STANDARD, n, line);
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
                  _configuration.defineStructure(
                      CiscoNxosStructureType.IP_ACCESS_LIST, name, ctx.getStart().getLine());
                  return new IpAccessList(name);
                });
  }

  @Override
  public void enterIp_as_path_access_list(Ip_as_path_access_listContext ctx) {
    int line = ctx.getStart().getLine();
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
                  _configuration.defineStructure(
                      CiscoNxosStructureType.IP_AS_PATH_ACCESS_LIST, n, line);
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
                              _configuration.defineStructure(
                                  CiscoNxosStructureType.IP_PREFIX_LIST,
                                  name,
                                  ctx.getStart().getLine());
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
  public void exitNve_member(Nve_memberContext ctx) {
    _currentNveVnis = null;
  }

  @Override
  public void exitNvm_ingress_replication(Nvm_ingress_replicationContext ctx) {
    IngressReplicationProtocol protocol =
        ctx.BGP() != null
            ? Nve.IngressReplicationProtocol.BGP
            : Nve.IngressReplicationProtocol.STATIC;
    _currentNveVnis.forEach(vni -> vni.setIngressReplicationProtocol(protocol));
  }

  @Override
  public void exitNvm_mcast_group(Nvm_mcast_groupContext ctx) {
    Ip mcastIp = toIp(ctx.first);
    if (!Prefix.MULTICAST.containsIp(mcastIp)) {
      warn(ctx, String.format("IPv4 address %s is not a valid multicast IP", mcastIp));
      return;
    }
    _currentNveVnis.forEach(vni -> vni.setMcastGroup(mcastIp));
  }

  @Override
  public void exitNvm_suppress_arp(Nvm_suppress_arpContext ctx) {
    boolean value = ctx.DISABLE() == null;
    _currentNveVnis.forEach(vni -> vni.setSuppressArp(value));
  }

  @Override
  public void exitNvg_ingress_replication(Nvg_ingress_replicationContext ctx) {
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
                              _configuration.defineStructure(
                                  CiscoNxosStructureType.NVE, nveName, line);
                              _configuration.referenceStructure(
                                  CiscoNxosStructureType.NVE,
                                  nveName,
                                  CiscoNxosStructureUsage.NVE_SELF_REFERENCE,
                                  line);
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
    if (type != ETHERNET && type != PORT_CHANNEL && parentInterface != null) {
      _w.redFlag(
          String.format(
              "Cannot construct subinterface for interface type '%s' in: %s",
              type, getFullText(ctx)));
      _currentInterfaces = ImmutableList.of();
      return;
    }

    // Validate VLAN numbers
    if (type == VLAN
        && !_currentValidVlanRange.contains(IntegerSpace.of(Range.closed(first, last)))) {
      _w.redFlag(
          String.format(
              "Vlan number(s) outside of range %s in Vlan interface declaration: %s",
              _currentValidVlanRange, getFullText(ctx)));
      _currentInterfaces = ImmutableList.of();
      return;
    }

    // Validate port-channel numbers
    if (type == PORT_CHANNEL
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
                            _configuration.defineStructure(
                                CiscoNxosStructureType.INTERFACE, n, line);
                            _configuration.referenceStructure(
                                CiscoNxosStructureType.INTERFACE,
                                n,
                                CiscoNxosStructureUsage.INTERFACE_SELF_REFERENCE,
                                line);
                            if (type == VLAN) {
                              _configuration.referenceStructure(
                                  CiscoNxosStructureType.VLAN,
                                  Integer.toString(i),
                                  CiscoNxosStructureUsage.INTERFACE_VLAN,
                                  line);
                              return newVlanInterface(n, i);
                            } else {
                              if (type == PORT_CHANNEL) {
                                _configuration.defineStructure(
                                    CiscoNxosStructureType.PORT_CHANNEL, n, line);
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
    Optional<String> nameOpt = toString(ctx, ctx.name);
    if (!nameOpt.isPresent()) {
      return;
    }
    int line = ctx.getStart().getLine();
    _currentRouteMapEntry =
        _configuration
            .getRouteMaps()
            .computeIfAbsent(
                nameOpt.get(),
                name -> {
                  _configuration.defineStructure(CiscoNxosStructureType.ROUTE_MAP, name, line);
                  return new RouteMap(name);
                })
            .getEntries()
            .computeIfAbsent(
                sequence,
                seq -> {
                  _configuration.defineStructure(
                      CiscoNxosStructureType.ROUTE_MAP_ENTRY, seq.toString(), line);
                  return new RouteMapEntry(seq);
                });
    _currentRouteMapEntry.setAction(toLineAction(ctx.action));
  }

  @Override
  public void enterS_vrf_context(S_vrf_contextContext ctx) {
    String name = toVrfName(ctx, ctx.name);
    if (name == null) {
      _currentVrf = new Vrf("dummy");
      return;
    }
    _currentVrf =
        _configuration
            .getVrfs()
            .computeIfAbsent(
                name,
                n -> {
                  _configuration.defineStructure(
                      CiscoNxosStructureType.VRF, name, ctx.getStart().getLine());
                  return new Vrf(n);
                });
  }

  @Override
  public void enterVlan_vlan(Vlan_vlanContext ctx) {
    IntegerSpace vlans = toVlanIdRange(ctx, ctx.vlans);
    int line = ctx.getStart().getLine();
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
                              _configuration.defineStructure(
                                  CiscoNxosStructureType.VLAN, Integer.toString(id), line);
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
  public void exitI_bandwidth(I_bandwidthContext ctx) {
    Integer bandwidth = toBandwidth(ctx, ctx.bw);
    if (bandwidth == null) {
      return;
    }
    _currentInterfaces.forEach(iface -> iface.setBandwidth(bandwidth));
  }

  @Override
  public void exitI_channel_group(I_channel_groupContext ctx) {
    int line = ctx.getStart().getLine();
    String channelGroup = toPortChannel(ctx, ctx.id);
    if (channelGroup == null) {
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

    boolean channelExists = _configuration.getInterfaces().containsKey(channelGroup);
    boolean force = ctx.force != null;

    _configuration.referenceStructure(
        CiscoNxosStructureType.PORT_CHANNEL,
        channelGroup,
        CiscoNxosStructureUsage.INTERFACE_CHANNEL_GROUP,
        line);

    if (_currentInterfaces.isEmpty()) {
      // Stop now, since later logic requires non-empty list
      return;
    }

    Interface referenceIface =
        channelExists
            ? _configuration.getInterfaces().get(channelGroup)
            : _currentInterfaces.iterator().next();

    if (!force) {
      Optional<Interface> incompatibleInterface =
          _currentInterfaces.stream()
              .filter(iface -> iface != referenceIface)
              .filter(iface -> !checkPortChannelCompatibilitySettings(referenceIface, iface))
              .findFirst();
      if (incompatibleInterface.isPresent()) {
        _w.redFlag(
            String.format(
                "Cannot set channel-group because interface '%s' has settings that do not conform to those of interface '%s' in: %s",
                incompatibleInterface.get().getName(), referenceIface.getName(), getFullText(ctx)));
        return;
      }
    }

    Interface portChannelIface;
    if (channelExists) {
      portChannelIface = referenceIface;
    } else {
      portChannelIface =
          newNonVlanInterface(channelGroup, null, CiscoNxosInterfaceType.PORT_CHANNEL);
      copyPortChannelCompatibilitySettings(referenceIface, portChannelIface);
      _configuration.getInterfaces().put(channelGroup, portChannelIface);
      _configuration.defineStructure(CiscoNxosStructureType.INTERFACE, channelGroup, line);
      _configuration.referenceStructure(
          CiscoNxosStructureType.INTERFACE,
          channelGroup,
          CiscoNxosStructureUsage.INTERFACE_SELF_REFERENCE,
          line);
      _configuration.defineStructure(CiscoNxosStructureType.PORT_CHANNEL, channelGroup, line);
    }

    _currentInterfaces.forEach(
        iface -> {
          iface.setChannelGroup(channelGroup);
          copyPortChannelCompatibilitySettings(referenceIface, iface);
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
    String name = toVrfName(ctx, ctx.name);
    if (name == null) {
      return;
    }
    if (_currentInterfaces.stream()
        .anyMatch(iface -> iface.getSwitchportMode() != SwitchportMode.NONE)) {
      _w.redFlag(
          String.format("Cannot assign VRF to switchport interface(s) in: %s", getFullText(ctx)));
      return;
    }
    _configuration.referenceStructure(
        CiscoNxosStructureType.VRF,
        name,
        CiscoNxosStructureUsage.INTERFACE_VRF_MEMBER,
        ctx.getStart().getLine());
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
      _configuration.referenceStructure(
          CiscoNxosStructureType.INTERFACE,
          nhint,
          CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_INTERFACE,
          line);
    }
    if (ctx.nhip != null) {
      builder.setNextHopIp(toIp(ctx.nhip));
    }
    if (ctx.nhvrf != null) {
      String vrf = toVrfName(ctx, ctx.nhvrf);
      if (vrf == null) {
        return;
      }
      _configuration.referenceStructure(
          CiscoNxosStructureType.VRF, vrf, CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_VRF, line);
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
        CiscoNxosStructureType.INTERFACE,
        iname,
        CiscoNxosStructureUsage.NVE_SOURCE_INTERFACE,
        ctx.name.getStart().getLine());
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
      Optional<String> name = toString(ctx, nameCtx);
      if (!name.isPresent()) {
        return;
      }
      names.add(name.get());
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
  public void exitRms_tag(Rms_tagContext ctx) {
    _currentRouteMapEntry.setSetTag(new RouteMapSetTag(toLong(ctx.tag)));
  }

  @Override
  public void exitS_hostname(S_hostnameContext ctx) {
    _configuration.setHostname(ctx.hostname.getText());
  }

  @Override
  public void exitS_route_map(S_route_mapContext ctx) {
    _currentRouteMapEntry = null;
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

  @Override
  public void exitVcaf4u_route_target(Vcaf4u_route_targetContext ctx) {
    VrfAddressFamily af = _currentVrf.getAddressFamily(AddressFamily.IPV4_UNICAST);
    RouteDistinguisherOrAuto rd = toRouteDistinguisher(ctx.rd);
    boolean setExport = ctx.BOTH() != null || ctx.EXPORT() != null;
    boolean setImport = ctx.BOTH() != null || ctx.IMPORT() != null;
    boolean evpn = ctx.EVPN() != null;
    if (!evpn && setExport) {
      af.setExportRt(rd);
    }
    if (evpn && setExport) {
      af.setExportRtEvpn(rd);
    }
    if (!evpn && setImport) {
      af.setImportRt(rd);
    }
    if (evpn && setImport) {
      af.setImportRtEvpn(rd);
    }
  }

  @Override
  public void exitVcaf6u_route_target(Vcaf6u_route_targetContext ctx) {
    VrfAddressFamily af = _currentVrf.getAddressFamily(AddressFamily.IPV6_UNICAST);
    RouteDistinguisherOrAuto rd = toRouteDistinguisher(ctx.rd);
    boolean setExport = ctx.BOTH() != null || ctx.EXPORT() != null;
    boolean setImport = ctx.BOTH() != null || ctx.IMPORT() != null;
    boolean evpn = ctx.EVPN() != null;
    if (!evpn && setExport) {
      af.setExportRt(rd);
    }
    if (evpn && setExport) {
      af.setExportRtEvpn(rd);
    }
    if (!evpn && setImport) {
      af.setImportRt(rd);
    }
    if (evpn && setImport) {
      af.setImportRtEvpn(rd);
    }
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
      S_route_mapContext messageCtx, Route_map_sequenceContext ctx) {
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

  private Optional<String> toString(
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
      S_route_mapContext messageCtx, Route_map_nameContext ctx) {
    return toStringWithLengthInSpace(
        messageCtx, ctx, ROUTE_MAP_NAME_LENGTH_RANGE, "route-map name");
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
      return ETHERNET;
    } else if (ctx.LOOPBACK() != null) {
      return LOOPBACK;
    } else if (ctx.MGMT() != null) {
      return MGMT;
    } else if (ctx.PORT_CHANNEL() != null) {
      return PORT_CHANNEL;
    } else if (ctx.VLAN() != null) {
      return VLAN;
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

  private @Nullable String toVrfName(ParserRuleContext messageCtx, Vrf_nameContext ctx) {
    String name = ctx.getText();
    if (name.length() > MAX_VRF_NAME_LENGTH) {
      _w.redFlag(
          String.format(
              "VRF name cannot exceed %d chars, but was '%s' in: %s",
              MAX_VRF_NAME_LENGTH, name, getFullText(messageCtx)));
      return null;
    }
    // Case-insensitive, so just canonicalize as lower-case
    return name.toLowerCase();
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
