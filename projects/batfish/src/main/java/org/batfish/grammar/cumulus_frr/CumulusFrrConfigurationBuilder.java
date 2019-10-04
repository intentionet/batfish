package org.batfish.grammar.cumulus_frr;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Long.parseLong;
import static org.batfish.representation.cumulus.CumulusRoutingProtocol.CONNECTED;
import static org.batfish.representation.cumulus.CumulusRoutingProtocol.STATIC;
import static org.batfish.representation.cumulus.CumulusStructureType.IP_COMMUNITY_LIST;
import static org.batfish.representation.cumulus.CumulusStructureUsage.BGP_IPV4_UNICAST_REDISTRIBUTE_CONNECTED_ROUTE_MAP;
import static org.batfish.representation.cumulus.CumulusStructureUsage.BGP_IPV4_UNICAST_REDISTRIBUTE_STATIC_ROUTE_MAP;
import static org.batfish.representation.cumulus.CumulusStructureUsage.ROUTE_MAP_MATCH_COMMUNITY_LIST;
import static org.batfish.representation.cumulus.RemoteAsType.EXPLICIT;
import static org.batfish.representation.cumulus.RemoteAsType.EXTERNAL;
import static org.batfish.representation.cumulus.RemoteAsType.INTERNAL;
import static org.batfish.representation.cumulus_interfaces.Interface.isPhysicalInterfaceType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Icl_expandedContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Ip_prefix_listContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Literal_standard_communityContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Pl_lineContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rm_descriptionContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rmm_communityContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rmm_interfaceContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rmm_tagContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rmmipa_prefix_listContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rms_communityContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rms_local_preferenceContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rms_metricContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Rmsipnh_literalContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Ro_passive_interfaceContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.S_bgpContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.S_interfaceContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.S_routemapContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.S_router_ospfContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.S_vrfContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sb_neighborContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbaf_ipv4_unicastContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbaf_l2vpn_evpnContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafi_aggregate_addressContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafi_neighborContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafi_networkContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafi_redistributeContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafin_activateContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafin_next_hop_selfContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafin_route_mapContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafin_route_reflector_clientContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafls_advertise_all_vniContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafls_advertise_ipv4_unicastContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbafls_neighbor_activateContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbb_router_idContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbbb_aspath_multipath_relaxContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbn_interfaceContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbn_ipContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbn_nameContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbn_peer_group_declContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbnp_descriptionContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbnp_ebgp_multihopContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbnp_peer_groupContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sbnp_remote_asContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Si_descriptionContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Siip_addressContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Siipo_areaContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Siipo_network_p2pContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sv_routeContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Sv_vniContext;
import org.batfish.grammar.cumulus_frr.CumulusFrrParser.Uint32Context;
import org.batfish.representation.cumulus.BgpInterfaceNeighbor;
import org.batfish.representation.cumulus.BgpIpNeighbor;
import org.batfish.representation.cumulus.BgpIpv4UnicastAddressFamily;
import org.batfish.representation.cumulus.BgpL2VpnEvpnIpv4Unicast;
import org.batfish.representation.cumulus.BgpL2vpnEvpnAddressFamily;
import org.batfish.representation.cumulus.BgpNeighbor;
import org.batfish.representation.cumulus.BgpNeighborIpv4UnicastAddressFamily;
import org.batfish.representation.cumulus.BgpNeighborL2vpnEvpnAddressFamily;
import org.batfish.representation.cumulus.BgpNetwork;
import org.batfish.representation.cumulus.BgpPeerGroupNeighbor;
import org.batfish.representation.cumulus.BgpProcess;
import org.batfish.representation.cumulus.BgpRedistributionPolicy;
import org.batfish.representation.cumulus.BgpVrf;
import org.batfish.representation.cumulus.BgpVrfAddressFamilyAggregateNetworkConfiguration;
import org.batfish.representation.cumulus.CumulusInterfaceType;
import org.batfish.representation.cumulus.CumulusNcluConfiguration;
import org.batfish.representation.cumulus.CumulusRoutingProtocol;
import org.batfish.representation.cumulus.CumulusStructureType;
import org.batfish.representation.cumulus.CumulusStructureUsage;
import org.batfish.representation.cumulus.Interface;
import org.batfish.representation.cumulus.IpCommunityListExpanded;
import org.batfish.representation.cumulus.IpPrefixList;
import org.batfish.representation.cumulus.IpPrefixListLine;
import org.batfish.representation.cumulus.OspfNetworkType;
import org.batfish.representation.cumulus.OspfProcess;
import org.batfish.representation.cumulus.RouteMap;
import org.batfish.representation.cumulus.RouteMapEntry;
import org.batfish.representation.cumulus.RouteMapMatchCommunity;
import org.batfish.representation.cumulus.RouteMapMatchInterface;
import org.batfish.representation.cumulus.RouteMapMatchIpAddressPrefixList;
import org.batfish.representation.cumulus.RouteMapMatchTag;
import org.batfish.representation.cumulus.RouteMapSetCommunity;
import org.batfish.representation.cumulus.RouteMapSetIpNextHopLiteral;
import org.batfish.representation.cumulus.RouteMapSetLocalPreference;
import org.batfish.representation.cumulus.RouteMapSetMetric;
import org.batfish.representation.cumulus.StaticRoute;
import org.batfish.representation.cumulus.Vrf;

public class CumulusFrrConfigurationBuilder extends CumulusFrrParserBaseListener {
  private final CumulusNcluConfiguration _c;
  private final CumulusFrrCombinedParser _parser;
  private final Warnings _w;

  private @Nullable Vrf _currentVrf;
  private @Nullable RouteMapEntry _currentRouteMapEntry;
  private @Nullable BgpVrf _currentBgpVrf;
  private @Nullable BgpNeighbor _currentBgpNeighbor;
  private @Nullable IpPrefixList _currentIpPrefixList;
  private @Nullable BgpNeighborIpv4UnicastAddressFamily _currentBgpNeighborIpv4UnicastAddressFamily;
  private @Nullable Interface _currentInterface;

  public CumulusFrrConfigurationBuilder(
      CumulusNcluConfiguration configuration, CumulusFrrCombinedParser parser, Warnings w) {
    _c = configuration;
    _parser = parser;
    _w = w;
  }

  CumulusNcluConfiguration getVendorConfiguration() {
    return _c;
  }

  private @Nonnull StandardCommunity toStandardCommunity(Literal_standard_communityContext ctx) {
    return StandardCommunity.of(
        Integer.parseInt(ctx.high.getText()), Integer.parseInt(ctx.low.getText()));
  }

  @Nonnull
  private Long toLong(Uint32Context ctx) {
    return Long.parseUnsignedLong(ctx.getText());
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

  @Override
  public void enterS_bgp(S_bgpContext ctx) {
    if (_c.getBgpProcess() == null) {
      _c.setBgpProcess(new BgpProcess());
    }

    if (ctx.vrf_name() == null) {
      _currentBgpVrf = _c.getBgpProcess().getDefaultVrf();
    } else {
      String vrfName = ctx.vrf_name().getText();
      _currentBgpVrf = new BgpVrf(vrfName);
      _c.getBgpProcess().getVrfs().put(vrfName, _currentBgpVrf);
      _c.referenceStructure(
          CumulusStructureType.VRF,
          vrfName,
          CumulusStructureUsage.BGP_VRF,
          ctx.vrf_name().getStart().getLine());
    }
    _currentBgpVrf.setAutonomousSystem(parseLong(ctx.autonomous_system().getText()));
  }

  @Override
  public void exitS_bgp(S_bgpContext ctx) {
    _currentBgpVrf = null;
  }

  @Override
  public void enterSbaf_ipv4_unicast(Sbaf_ipv4_unicastContext ctx) {
    if (_currentBgpVrf.getIpv4Unicast() != null) {
      _w.addWarning(ctx, ctx.getText(), _parser, "duplicate 'address-family ipv4 unicast'");
    }
    _currentBgpVrf.setIpv4Unicast(new BgpIpv4UnicastAddressFamily());
  }

  @Override
  public void enterSbaf_l2vpn_evpn(Sbaf_l2vpn_evpnContext ctx) {
    if (_currentBgpVrf.getL2VpnEvpn() != null) {
      _w.addWarning(ctx, ctx.getText(), _parser, "duplicate 'address-family l2vpn evpn'");
    }
    _currentBgpVrf.setL2VpnEvpn(new BgpL2vpnEvpnAddressFamily());
  }

  @Override
  public void exitSbafi_redistribute(Sbafi_redistributeContext ctx) {
    CumulusRoutingProtocol protocol;
    CumulusStructureUsage usage;
    if (ctx.STATIC() != null) {
      protocol = STATIC;
      usage = BGP_IPV4_UNICAST_REDISTRIBUTE_STATIC_ROUTE_MAP;
    } else if (ctx.CONNECTED() != null) {
      protocol = CONNECTED;
      usage = BGP_IPV4_UNICAST_REDISTRIBUTE_CONNECTED_ROUTE_MAP;
    } else {
      throw new BatfishException("Unexpected redistribution protocol");
    }

    String routeMap;
    if (ctx.route_map_name() != null) {
      routeMap = ctx.route_map_name().getText();
      _c.referenceStructure(
          CumulusStructureType.ROUTE_MAP, routeMap, usage, ctx.getStart().getLine());
    } else {
      routeMap = null;
    }

    BgpRedistributionPolicy oldRedistributionPolicy =
        _currentBgpVrf
            .getIpv4Unicast()
            .getRedistributionPolicies()
            .put(protocol, new BgpRedistributionPolicy(protocol, routeMap));

    if (oldRedistributionPolicy != null) {
      _w.addWarning(
          ctx,
          ctx.getStart().getText(),
          _parser,
          String.format(
              "overwriting BgpRedistributionPolicy for vrf %s, protocol %s",
              _currentBgpVrf.getVrfName(), protocol));
    }
  }

  @Override
  public void exitSbafi_network(Sbafi_networkContext ctx) {
    _currentBgpVrf
        .getIpv4Unicast()
        .getNetworks()
        .computeIfAbsent(Prefix.parse(ctx.IP_PREFIX().getText()), BgpNetwork::new);
  }

  @Override
  public void exitSbafls_advertise_all_vni(Sbafls_advertise_all_vniContext ctx) {
    _currentBgpVrf.getL2VpnEvpn().setAdvertiseAllVni(true);
  }

  @Override
  public void enterSbafls_advertise_ipv4_unicast(Sbafls_advertise_ipv4_unicastContext ctx) {
    // setting in enter instead of exit since in future we can attach a routemap
    _currentBgpVrf.getL2VpnEvpn().setAdvertiseIpv4Unicast(new BgpL2VpnEvpnIpv4Unicast());
  }

  @Override
  public void exitSbafi_aggregate_address(Sbafi_aggregate_addressContext ctx) {
    Map<Prefix, BgpVrfAddressFamilyAggregateNetworkConfiguration> aggregateNetworks =
        _currentBgpVrf.getIpv4Unicast().getAggregateNetworks();
    Prefix prefix = Prefix.parse(ctx.IP_PREFIX().getText());
    BgpVrfAddressFamilyAggregateNetworkConfiguration agg =
        new BgpVrfAddressFamilyAggregateNetworkConfiguration();
    agg.setSummaryOnly(ctx.SUMMARY_ONLY() != null);
    if (aggregateNetworks.put(prefix, agg) != null) {
      _w.addWarning(
          ctx, ctx.getText(), _parser, "Overwriting aggregate-address for " + prefix.toString());
    }
  }

  @Override
  public void enterSbafi_neighbor(Sbafi_neighborContext ctx) {
    String name;
    if (ctx.ip != null) {
      name = ctx.ip.getText();
    } else if (ctx.name != null) {
      name = ctx.name.getText();
    } else {
      throw new BatfishException("neightbor name or address");
    }

    BgpNeighbor bgpNeighbor = _currentBgpVrf.getNeighbors().get(name);
    if (bgpNeighbor == null) {
      _w.addWarning(
          ctx,
          ctx.getStart().getText(),
          _parser,
          String.format("neighbor %s does not exist", name));
    } else {
      _currentBgpNeighborIpv4UnicastAddressFamily = bgpNeighbor.getIpv4UnicastAddressFamily();
      if (_currentBgpNeighborIpv4UnicastAddressFamily == null) {
        _currentBgpNeighborIpv4UnicastAddressFamily = new BgpNeighborIpv4UnicastAddressFamily();
        bgpNeighbor.setIpv4UnicastAddressFamily(_currentBgpNeighborIpv4UnicastAddressFamily);
      }
    }
  }

  @Override
  public void exitSbafi_neighbor(Sbafi_neighborContext ctx) {
    _currentBgpNeighborIpv4UnicastAddressFamily = null;
  }

  @Override
  public void enterS_interface(S_interfaceContext ctx) {
    String name = ctx.name.getText();

    _currentInterface = _c.getInterfaces().get(name);

    if (_currentInterface == null) {
      // Currently only support defining lo and physical interfaces in frr
      // TODO: check other interface types
      CumulusInterfaceType type = null;
      if (name.equals(CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME)) {
        type = CumulusInterfaceType.LOOPBACK;
      } else if (isPhysicalInterfaceType(name)) {
        type = CumulusInterfaceType.PHYSICAL;
      } else {
        _w.addWarning(
            ctx,
            ctx.getText(),
            _parser,
            String.format(
                ("cannot recognize interface %s. Only support loopback and physical interfaces"),
                name));
      }

      if (type == null) {
        return;
      }

      _currentInterface = new Interface(name, type, null, null);
      _c.setInterfaces(
          new ImmutableMap.Builder<String, Interface>()
              .putAll(_c.getInterfaces())
              .put(name, _currentInterface)
              .build());
    }

    if (ctx.VRF() != null) {
      String vrf = ctx.vrf.getText();
      if (_currentInterface.getVrf() == null) {
        _currentInterface.setVrf(vrf);
      } else if (!vrf.equals(_currentInterface.getVrf())) {
        _w.addWarning(
            ctx,
            ctx.getText(),
            _parser,
            String.format(
                "vrf %s of interface %s does not match vrf %s defined already",
                vrf, name, _currentInterface.getVrf()));
      }
    } else if (_currentInterface.getVrf() != null) {
      _w.addWarning(
          ctx,
          ctx.getText(),
          _parser,
          String.format(
              "default vrf of interface %s does not match vrf %s defined already",
              name, _currentInterface.getVrf()));
    }
  }

  @Override
  public void exitS_interface(S_interfaceContext ctx) {
    _currentInterface = null;
  }

  @Override
  public void exitSiip_address(Siip_addressContext ctx) {
    if (_currentInterface == null) {
      _w.addWarning(ctx, ctx.getText(), _parser, "no interfaces found for %s");
    }

    _currentInterface.getIpAddresses().add(ConcreteInterfaceAddress.parse(ctx.ip_prefix.getText()));
  }

  @Override
  public void enterS_router_ospf(S_router_ospfContext ctx) {
    if (_c.getOspfProcess() == null) {
      _c.setOspfProcess(new OspfProcess());
    }
  }

  @Override
  public void exitSi_description(Si_descriptionContext ctx) {
    if (_currentInterface == null) {
      return;
    }
    if (_currentInterface.getAlias() == null) {
      _currentInterface.setAlias(ctx.description.getText());
    }
  }

  @Override
  public void exitSiipo_area(Siipo_areaContext ctx) {
    if (_currentInterface == null) {
      return;
    }

    _currentInterface.getOrCreateOspf().setOspfArea(Ip.parse(ctx.ip.getText()).asLong());
  }

  @Override
  public void exitSbafin_activate(Sbafin_activateContext ctx) {
    if (_currentBgpNeighborIpv4UnicastAddressFamily == null) {
      return;
    }
    _currentBgpNeighborIpv4UnicastAddressFamily.setActivated(true);
  }

  @Override
  public void exitSbafin_next_hop_self(Sbafin_next_hop_selfContext ctx) {
    if (_currentBgpNeighborIpv4UnicastAddressFamily == null) {
      return;
    }
    _currentBgpNeighborIpv4UnicastAddressFamily.setNextHopSelf(true);
  }

  @Override
  public void exitSbafin_route_reflector_client(Sbafin_route_reflector_clientContext ctx) {
    if (_currentBgpNeighborIpv4UnicastAddressFamily == null) {
      return;
    }
    _currentBgpNeighborIpv4UnicastAddressFamily.setRouteReflectorClient(true);
  }

  @Override
  public void exitSbafls_neighbor_activate(Sbafls_neighbor_activateContext ctx) {
    String neighborName = ctx.neighbor.getText();
    BgpNeighbor neighbor = _currentBgpVrf.getNeighbors().get(neighborName);
    if (neighbor == null) {
      _w.addWarning(
          ctx, ctx.getText(), _parser, String.format("neighbor %s does not exist", neighborName));
    } else {
      BgpNeighborL2vpnEvpnAddressFamily addressFamily = neighbor.getL2vpnEvpnAddressFamily();
      if (addressFamily == null) {
        addressFamily = new BgpNeighborL2vpnEvpnAddressFamily();
        neighbor.setL2vpnEvpnAddressFamily(addressFamily);
      }
      addressFamily.setActivated(true);
    }
  }

  @Override
  public void exitSiipo_network_p2p(Siipo_network_p2pContext ctx) {
    _currentInterface.getOrCreateOspf().setNetwork(OspfNetworkType.POINT_TO_POINT);
  }

  @Override
  public void exitSbbb_aspath_multipath_relax(Sbbb_aspath_multipath_relaxContext ctx) {
    _currentBgpVrf.setAsPathMultipathRelax(true);
  }

  @Override
  public void exitSbb_router_id(Sbb_router_idContext ctx) {
    _currentBgpVrf.setRouterId(Ip.parse(ctx.IP_ADDRESS().getText()));
  }

  @Override
  public void enterSbn_ip(Sbn_ipContext ctx) {
    String name = ctx.ip.getText();
    _currentBgpNeighbor =
        _currentBgpVrf
            .getNeighbors()
            .computeIfAbsent(
                name,
                (ipStr) -> {
                  BgpIpNeighbor neighbor = new BgpIpNeighbor(ipStr);
                  neighbor.setPeerIp(Ip.parse(ipStr));
                  return neighbor;
                });
  }

  @Override
  public void enterSbn_name(Sbn_nameContext ctx) {
    // if neighbor does not already exist, get will return null. That's ok -- the listener for
    // child parse node will create it.
    _currentBgpNeighbor = _currentBgpVrf.getNeighbors().get(ctx.name.getText());
  }

  @Override
  public void exitSb_neighbor(Sb_neighborContext ctx) {
    _currentBgpNeighbor = null;
  }

  @Override
  public void enterSbn_interface(Sbn_interfaceContext ctx) {
    if (_currentBgpNeighbor != null) {
      // warn if it's not an interface
      if (!(_currentBgpNeighbor instanceof BgpInterfaceNeighbor)) {
        String line = ctx.getParent().getText() + ctx.getText();
        _w.addWarning(
            ctx,
            line,
            _parser,
            String.format(
                "neighbor %s not declared to be an interface", _currentBgpNeighbor.getName()));
      }
    } else {
      Sbn_nameContext parentCtx = (Sbn_nameContext) ctx.getParent();
      String ifaceName = parentCtx.name.getText();
      _currentBgpNeighbor = new BgpInterfaceNeighbor(ifaceName);
      checkState(
          _currentBgpVrf.getNeighbors().put(ifaceName, _currentBgpNeighbor) == null,
          "neighbor should not already exist since _currentBgpNeighbor was null");
    }
  }

  @Override
  public void exitSbn_peer_group_decl(Sbn_peer_group_declContext ctx) {
    if (_currentBgpNeighbor != null) {
      String line = ctx.getParent().getText() + ctx.getText();
      _w.addWarning(
          ctx,
          line,
          _parser,
          String.format("neighbor %s already defined", _currentBgpNeighbor.getName()));
    }

    Sbn_nameContext parentCtx = (Sbn_nameContext) ctx.getParent();
    String peerGroupName = parentCtx.name.getText();
    checkState(
        _currentBgpVrf.getNeighbors().put(peerGroupName, new BgpPeerGroupNeighbor(peerGroupName))
            == null,
        "neighbor should not already exist since _currentBgpNeighbor was null");
  }

  @Override
  public void exitSbnp_description(Sbnp_descriptionContext ctx) {
    _currentBgpNeighbor.setDescription(ctx.REMARK_TEXT().getText());
  }

  @Override
  public void exitSbnp_peer_group(Sbnp_peer_groupContext ctx) {
    _currentBgpNeighbor.setPeerGroup(ctx.name.getText());
  }

  @Override
  public void exitSbnp_remote_as(Sbnp_remote_asContext ctx) {
    if (ctx.autonomous_system() != null) {
      _currentBgpNeighbor.setRemoteAsType(EXPLICIT);
      _currentBgpNeighbor.setRemoteAs(parseLong(ctx.autonomous_system().getText()));
    } else if (ctx.EXTERNAL() != null) {
      _currentBgpNeighbor.setRemoteAsType(EXTERNAL);
      _currentBgpNeighbor.setRemoteAs(null);
    } else if (ctx.INTERNAL() != null) {
      _currentBgpNeighbor.setRemoteAsType(INTERNAL);
      _currentBgpNeighbor.setRemoteAs(null);
    }
  }

  @Override
  public void exitSbnp_ebgp_multihop(Sbnp_ebgp_multihopContext ctx) {
    long num = parseLong(ctx.num.getText());
    _currentBgpNeighbor.setEbgpMultihop(num);
  }

  @Override
  public void enterS_vrf(S_vrfContext ctx) {
    String name = ctx.name.getText();

    // VRFs are declared in /etc/network/interfaces file, but this is part of the definition
    _currentVrf = _c.getVrfs().get(name);
    _c.defineStructure(CumulusStructureType.VRF, name, ctx);
  }

  @Override
  public void exitS_vrf(S_vrfContext ctx) {
    _currentVrf = null;
  }

  @Override
  public void exitSbafin_route_map(Sbafin_route_mapContext ctx) {
    String name = ctx.name.getText();
    CumulusStructureUsage usage;
    if (ctx.IN() != null) {
      usage = CumulusStructureUsage.BGP_IPV4_UNICAST_NEIGHBOR_ROUTE_MAP_IN;
      _currentBgpNeighborIpv4UnicastAddressFamily.setRouteMapIn(name);
    } else if (ctx.OUT() != null) {
      usage = CumulusStructureUsage.BGP_IPV4_UNICAST_NEIGHBOR_ROUTE_MAP_OUT;
      _currentBgpNeighborIpv4UnicastAddressFamily.setRouteMapOut(name);
    } else {
      throw new IllegalStateException("only support in and out in route map");
    }
    _c.referenceStructure(
        CumulusStructureType.ROUTE_MAP, name, usage, ctx.name.getStart().getLine());
  }

  @Override
  public void exitSv_vni(Sv_vniContext ctx) {
    int vni = Integer.parseInt(ctx.vni.v.getText());
    _currentVrf.setVni(vni);
  }

  @Override
  public void exitSv_route(Sv_routeContext ctx) {
    Ip nextHop = Ip.parse(ctx.ip_address().getText());
    Prefix network = Prefix.parse(ctx.prefix().getText());
    _currentVrf.getStaticRoutes().add(new StaticRoute(network, nextHop, null));
  }

  @Override
  public void enterS_routemap(S_routemapContext ctx) {
    int sequence = Integer.parseInt(ctx.sequence.getText());
    String name = ctx.name.getText();
    LineAction action;
    if (ctx.action.permit != null) {
      action = LineAction.PERMIT;
    } else if (ctx.action.deny != null) {
      action = LineAction.DENY;
    } else {
      throw new IllegalStateException("only support permit and deny in route map");
    }
    _currentRouteMapEntry =
        _c.getRouteMaps()
            .computeIfAbsent(name, RouteMap::new)
            .getEntries()
            .computeIfAbsent(
                sequence, k -> new RouteMapEntry(Integer.parseInt(ctx.sequence.getText()), action));
    _c.defineStructure(CumulusStructureType.ROUTE_MAP, name, ctx);
  }

  @Override
  public void exitRo_passive_interface(Ro_passive_interfaceContext ctx) {
    String ifaceName = ctx.name.getText();
    Interface iface = _c.getInterfaces().get(ifaceName);
    if (iface == null) {
      _w.addWarning(
          ctx, ctx.getText(), _parser, String.format("interface %s is not defined", ifaceName));
      return;
    }
    iface.getOrCreateOspf().setPassive(true);
  }

  @Override
  public void exitRm_description(Rm_descriptionContext ctx) {
    _currentRouteMapEntry.setDescription(ctx.route_map_description().getText());
  }

  @Override
  public void exitS_routemap(S_routemapContext ctx) {
    _currentRouteMapEntry = null;
  }

  @Override
  public void exitRmmipa_prefix_list(Rmmipa_prefix_listContext ctx) {
    String name = ctx.name.getText();
    RouteMapMatchIpAddressPrefixList matchPrefixList =
        _currentRouteMapEntry.getMatchIpAddressPrefixList();
    List<String> prefixNameList =
        matchPrefixList == null ? new ArrayList<>() : new ArrayList<>(matchPrefixList.getNames());
    prefixNameList.add(name);

    _currentRouteMapEntry.setMatchIpAddressPrefixList(
        new RouteMapMatchIpAddressPrefixList(prefixNameList));

    _c.referenceStructure(
        CumulusStructureType.IP_PREFIX_LIST,
        name,
        CumulusStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS_PREFIX_LIST,
        ctx.getStart().getLine());
  }

  @Override
  public void exitRmm_interface(Rmm_interfaceContext ctx) {
    String name = ctx.name.getText();
    _currentRouteMapEntry.setMatchInterface(new RouteMapMatchInterface(ImmutableSet.of(name)));
    _c.referenceStructure(
        CumulusStructureType.ABSTRACT_INTERFACE,
        name,
        CumulusStructureUsage.ROUTE_MAP_MATCH_INTERFACE,
        ctx.getStart().getLine());
  }

  @Override
  public void exitRmm_community(Rmm_communityContext ctx) {
    ctx.names.forEach(
        name ->
            _c.referenceStructure(
                IP_COMMUNITY_LIST, name.getText(),
                ROUTE_MAP_MATCH_COMMUNITY_LIST, name.getStart().getLine()));

    _currentRouteMapEntry.setMatchCommunity(
        new RouteMapMatchCommunity(
            ImmutableList.<String>builder()
                // add old names
                .addAll(
                    Optional.ofNullable(_currentRouteMapEntry.getMatchCommunity())
                        .map(RouteMapMatchCommunity::getNames)
                        .orElse(ImmutableList.of()))
                // add new names
                .addAll(ctx.names.stream().map(RuleContext::getText).iterator())
                .build()));
  }

  @Override
  public void exitRmm_tag(Rmm_tagContext ctx) {
    _currentRouteMapEntry.setMatchTag(new RouteMapMatchTag(toLong(ctx.tag)));
  }

  @Override
  public void exitRms_metric(Rms_metricContext ctx) {
    _currentRouteMapEntry.setSetMetric(new RouteMapSetMetric(parseLong(ctx.metric.getText())));
  }

  @Override
  public void exitRmsipnh_literal(Rmsipnh_literalContext ctx) {
    Ip ip = Ip.parse(ctx.next_hop.getText());
    RouteMapSetIpNextHopLiteral setNextHop = _currentRouteMapEntry.getSetIpNextHop();
    if (setNextHop != null) {
      _w.addWarning(
          ctx, ctx.getText(), _parser, "next-hop already exists will be replaced by this one");
    }

    _currentRouteMapEntry.setSetIpNextHop(new RouteMapSetIpNextHopLiteral(ip));
  }

  @Override
  public void exitRms_community(Rms_communityContext ctx) {
    RouteMapSetCommunity old = _currentRouteMapEntry.getSetCommunity();
    if (old != null) {
      _w.addWarning(ctx, ctx.getText(), _parser, "overwriting set community");
    }
    _currentRouteMapEntry.setSetCommunity(
        new RouteMapSetCommunity(
            ctx.communities.stream()
                .map(this::toStandardCommunity)
                .collect(ImmutableList.toImmutableList())));
  }

  @Override
  public void exitRms_local_preference(Rms_local_preferenceContext ctx) {
    _currentRouteMapEntry.setSetLocalPreference(new RouteMapSetLocalPreference(toLong(ctx.pref)));
  }

  @Override
  public void exitIcl_expanded(Icl_expandedContext ctx) {
    String name = ctx.name.getText();

    LineAction action;
    if (ctx.action.permit != null) {
      action = LineAction.PERMIT;
    } else if (ctx.action.deny != null) {
      action = LineAction.DENY;
    } else {
      throw new IllegalStateException("only support permit and deny in route map");
    }

    List<StandardCommunity> communityList =
        ctx.communities.stream()
            .map(RuleContext::getText)
            .map(StandardCommunity::parse)
            .collect(ImmutableList.toImmutableList());

    _c.defineStructure(IP_COMMUNITY_LIST, name, ctx);
    _c.getIpCommunityLists().put(name, new IpCommunityListExpanded(name, action, communityList));
  }

  @Override
  public void enterIp_prefix_list(Ip_prefix_listContext ctx) {
    String name = ctx.name.getText();
    _currentIpPrefixList = _c.getIpPrefixLists().computeIfAbsent(name, IpPrefixList::new);
    _c.defineStructure(CumulusStructureType.IP_PREFIX_LIST, name, ctx);
  }

  @Override
  public void exitIp_prefix_list(Ip_prefix_listContext ctx) {
    _currentIpPrefixList = null;
  }

  @Override
  public void exitPl_line(Pl_lineContext ctx) {
    long num = parseLong(ctx.num.getText());
    Prefix prefix = Prefix.parse(ctx.ip_prefix.getText());
    int prefixLength = prefix.getPrefixLength();
    int low = ctx.ge != null ? Integer.parseInt(ctx.ge.getText()) : prefixLength;
    int high = ctx.le != null ? Integer.parseInt(ctx.le.getText()) : Prefix.MAX_PREFIX_LENGTH;
    LineAction action = ctx.action.permit != null ? LineAction.PERMIT : LineAction.DENY;
    IpPrefixListLine pll = new IpPrefixListLine(action, num, prefix, new SubRange(low, high));
    _currentIpPrefixList.getLines().put(num, pll);
  }
}
