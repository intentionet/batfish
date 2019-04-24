package org.batfish.grammar.f5_bigip_imish;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.F5_bigip_imish_configurationContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Ip_prefixContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Ip_prefix_lengthContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Ip_specContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Line_actionContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Origin_typeContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rb_bgp_always_compare_medContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rb_bgp_deterministic_medContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rb_bgp_router_idContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rb_neighbor_ipv4Context;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rb_neighbor_ipv6Context;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rb_neighbor_peer_groupContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rb_redistribute_kernelContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rbn_descriptionContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rbn_next_hop_selfContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rbn_peer_groupContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rbn_peer_group_assignContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rbn_remote_asContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rbn_route_map_outContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rmm_ip_addressContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rmm_ip_address_prefix_listContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rms_communityContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rms_metricContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Rms_originContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.S_access_listContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.S_ip_prefix_listContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.S_route_mapContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.S_router_bgpContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Standard_communityContext;
import org.batfish.grammar.f5_bigip_imish.F5BigipImishParser.Uint32Context;
import org.batfish.representation.f5_bigip.AbstractBgpNeighbor;
import org.batfish.representation.f5_bigip.AccessList;
import org.batfish.representation.f5_bigip.AccessListLine;
import org.batfish.representation.f5_bigip.BgpNeighbor;
import org.batfish.representation.f5_bigip.BgpPeerGroup;
import org.batfish.representation.f5_bigip.BgpProcess;
import org.batfish.representation.f5_bigip.BgpRedistributionPolicy;
import org.batfish.representation.f5_bigip.F5BigipConfiguration;
import org.batfish.representation.f5_bigip.F5BigipRoutingProtocol;
import org.batfish.representation.f5_bigip.F5BigipStructureType;
import org.batfish.representation.f5_bigip.F5BigipStructureUsage;
import org.batfish.representation.f5_bigip.MatchAccessList;
import org.batfish.representation.f5_bigip.PrefixList;
import org.batfish.representation.f5_bigip.PrefixListEntry;
import org.batfish.representation.f5_bigip.RouteMap;
import org.batfish.representation.f5_bigip.RouteMapEntry;
import org.batfish.representation.f5_bigip.RouteMapMatchPrefixList;
import org.batfish.representation.f5_bigip.RouteMapSetCommunity;
import org.batfish.representation.f5_bigip.RouteMapSetMetric;
import org.batfish.representation.f5_bigip.RouteMapSetOrigin;
import org.batfish.vendor.StructureType;

public class F5BigipImishConfigurationBuilder extends F5BigipImishParserBaseListener {

  private static int toInteger(Ip_prefix_lengthContext ctx) {
    return Integer.parseInt(ctx.getText(), 10);
  }

  private static @Nonnull LineAction toLineAction(Line_actionContext ctx) {
    return ctx.PERMIT() != null ? LineAction.PERMIT : LineAction.DENY;
  }

  private static long toLong(Uint32Context ctx) {
    return Long.parseLong(ctx.getText(), 10);
  }

  private static @Nonnull OriginType toOriginType(Origin_typeContext ctx) {
    if (ctx.EGP() != null) {
      return OriginType.EGP;
    } else if (ctx.IGP() != null) {
      return OriginType.IGP;
    } else {
      return OriginType.INCOMPLETE;
    }
  }

  private static @Nonnull Prefix toPrefix(Ip_prefixContext ctx) {
    return Prefix.parse(ctx.getText());
  }

  private final @Nonnull F5BigipConfiguration _c;
  private @Nullable AbstractBgpNeighbor _currentAbstractNeighbor;
  private @Nullable BgpProcess _currentBgpProcess;
  private @Nullable BgpNeighbor _currentNeighbor;

  /**
   * When entering a bgp neighbor statement, {@code _currentNeighborName} is set to a non-null
   * value. Iff it refers to a neighbor that already exists, then {@code _currentAbstractNeighbor}
   * and one of {@code _currentNeighbor} or {@code _currentPeerGroup} are set to that existing
   * neighbor.
   */
  private @Nullable String _currentNeighborName;

  private @Nullable BgpPeerGroup _currentPeerGroup;
  private @Nullable RouteMapEntry _currentRouteMapEntry;

  @SuppressWarnings("unused")
  private @Nullable Boolean _no;

  @SuppressWarnings("unused")
  private final @Nonnull F5BigipImishCombinedParser _parser;

  private final @Nonnull String _text;
  private final @Nonnull Warnings _w;

  public F5BigipImishConfigurationBuilder(
      F5BigipImishCombinedParser parser,
      String text,
      Warnings w,
      F5BigipConfiguration configuration) {
    _parser = parser;
    _text = text;
    _w = w;
    _c = configuration;
  }

  private String convErrorMessage(Class<?> type, ParserRuleContext ctx) {
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
  public void enterF5_bigip_imish_configuration(F5_bigip_imish_configurationContext ctx) {
    _c.setImish(true);
  }

  @Override
  public void enterRb_neighbor_ipv4(Rb_neighbor_ipv4Context ctx) {
    _no = ctx.NO() != null;
    _currentNeighborName = ctx.ip.getText();
    _currentNeighbor = _currentBgpProcess.getNeighbors().get(_currentNeighborName);
    _currentAbstractNeighbor = _currentNeighbor != null ? _currentNeighbor : _currentPeerGroup;
  }

  @Override
  public void enterRb_neighbor_ipv6(Rb_neighbor_ipv6Context ctx) {
    _no = ctx.NO() != null;
    _currentNeighborName = ctx.ip6.getText();
    _currentNeighbor = _currentBgpProcess.getNeighbors().get(_currentNeighborName);
    _currentAbstractNeighbor = _currentNeighbor != null ? _currentNeighbor : _currentPeerGroup;
  }

  @Override
  public void enterRb_neighbor_peer_group(Rb_neighbor_peer_groupContext ctx) {
    _no = ctx.NO() != null;
    _currentNeighborName = ctx.name.getText();
    _currentPeerGroup = _currentBgpProcess.getPeerGroups().get(_currentNeighborName);
    _currentAbstractNeighbor = _currentNeighbor != null ? _currentNeighbor : _currentPeerGroup;
  }

  @Override
  public void enterS_route_map(S_route_mapContext ctx) {
    String name = ctx.name.getText();
    defineStructure(F5BigipStructureType.ROUTE_MAP, name, ctx);
    _currentRouteMapEntry =
        _c.getRouteMaps()
            .computeIfAbsent(name, RouteMap::new)
            .getEntries()
            .computeIfAbsent(toLong(ctx.num), RouteMapEntry::new);
    _currentRouteMapEntry.setAction(toLineAction(ctx.action));
  }

  @Override
  public void enterS_router_bgp(S_router_bgpContext ctx) {
    String name = ctx.localas.getText();
    _currentBgpProcess = _c.getBgpProcesses().computeIfAbsent(name, BgpProcess::new);
    _currentBgpProcess.setLocalAs(toLong(ctx.localas));
    defineStructure(F5BigipStructureType.BGP_PROCESS, name, ctx);
    _c.referenceStructure(
        F5BigipStructureType.BGP_PROCESS,
        name,
        F5BigipStructureUsage.BGP_PROCESS_SELF_REFERENCE,
        ctx.localas.getStart().getLine());
  }

  @Override
  public void exitRb_bgp_always_compare_med(Rb_bgp_always_compare_medContext ctx) {
    _currentBgpProcess.setAlwaysCompareMed(true);
  }

  @Override
  public void exitRb_bgp_deterministic_med(Rb_bgp_deterministic_medContext ctx) {
    _currentBgpProcess.setDeterministicMed(true);
  }

  @Override
  public void exitRb_bgp_router_id(Rb_bgp_router_idContext ctx) {
    _currentBgpProcess.setRouterId(Ip.parse(ctx.id.getText()));
  }

  @Override
  public void exitRb_neighbor_ipv4(Rb_neighbor_ipv4Context ctx) {
    _no = null;
    _currentNeighborName = null;
    _currentNeighbor = null;
    _currentAbstractNeighbor = null;
  }

  @Override
  public void exitRb_neighbor_ipv6(Rb_neighbor_ipv6Context ctx) {
    _no = null;
    _currentNeighborName = null;
    _currentNeighbor = null;
    _currentAbstractNeighbor = null;
  }

  @Override
  public void exitRb_neighbor_peer_group(Rb_neighbor_peer_groupContext ctx) {
    _no = null;
    _currentNeighborName = null;
    _currentAbstractNeighbor = null;
    _currentPeerGroup = null;
  }

  @Override
  public void exitRb_redistribute_kernel(Rb_redistribute_kernelContext ctx) {
    String routeMapName = null;
    if (ctx.rm != null) {
      routeMapName = ctx.rm.getText();
      _c.referenceStructure(
          F5BigipStructureType.ROUTE_MAP,
          routeMapName,
          F5BigipStructureUsage.BGP_REDISTRIBUTE_KERNEL_ROUTE_MAP,
          ctx.rm.getStart().getLine());
    }
    _currentBgpProcess
        .getIpv4AddressFamily()
        .getRedistributionPolicies()
        .computeIfAbsent(F5BigipRoutingProtocol.KERNEL, BgpRedistributionPolicy::new)
        .setRouteMap(routeMapName);
  }

  @Override
  public void exitRbn_description(Rbn_descriptionContext ctx) {
    if (_currentAbstractNeighbor == null) {
      _w.redFlag(
          String.format(
              "Cannot add description to non-existent neighbor: '%s' in: '%s'",
              _currentNeighborName, getFullText(ctx.getParent().getParent())));
      return;
    }
    _currentAbstractNeighbor.setDescription(ctx.text.getText());
  }

  @Override
  public void exitRbn_next_hop_self(Rbn_next_hop_selfContext ctx) {
    if (_currentAbstractNeighbor == null) {
      _w.redFlag(
          String.format(
              "Cannot apply next-hop-self to non-existent neighbor: '%s' in: '%s'",
              _currentNeighborName, getFullText(ctx.getParent().getParent())));
      return;
    }
    _currentAbstractNeighbor.setNextHopSelf(true);
  }

  @Override
  public void exitRbn_peer_group(Rbn_peer_groupContext ctx) {
    BgpPeerGroup pg =
        _currentBgpProcess.getPeerGroups().computeIfAbsent(_currentNeighborName, BgpPeerGroup::new);
    defineStructure(F5BigipStructureType.PEER_GROUP, _currentNeighborName, ctx.parent);
    pg.getIpv4AddressFamily().setActivate(true);
    pg.getIpv6AddressFamily().setActivate(true);
  }

  @Override
  public void exitRbn_peer_group_assign(Rbn_peer_group_assignContext ctx) {
    String peerGroupName = ctx.name.getText();
    _c.referenceStructure(
        F5BigipStructureType.PEER_GROUP,
        peerGroupName,
        F5BigipStructureUsage.BGP_NEIGHBOR_PEER_GROUP,
        ctx.name.getStart().getLine());
    boolean ipv4 = false;
    boolean ipv6 = false;
    if (Ip.tryParse(_currentNeighborName).isPresent()) {
      ipv4 = true;
    } else if (Ip6.tryParse(_currentNeighborName).isPresent()) {
      ipv6 = true;
    } else {
      // should not be possible
      _w.redFlag(
          String.format(
              "Unsupported neighbor id: '%s' in: '%s'",
              _currentNeighborName, getFullText(ctx.getParent().getParent())));
      return;
    }
    if (!_currentBgpProcess.getPeerGroups().containsKey(peerGroupName)) {
      _w.redFlag(
          String.format(
              "Cannot assign bgp neighbor to non-existent peer-group: '%s' in: '%s'",
              peerGroupName, getFullText(ctx.getParent())));
      return;
    }
    if (_currentNeighbor == null) {
      _currentNeighbor =
          _currentBgpProcess.getNeighbors().computeIfAbsent(_currentNeighborName, BgpNeighbor::new);
      defineStructure(F5BigipStructureType.BGP_NEIGHBOR, _currentNeighborName, ctx.parent);
      _c.referenceStructure(
          F5BigipStructureType.BGP_NEIGHBOR,
          _currentNeighborName,
          F5BigipStructureUsage.BGP_NEIGHBOR_SELF_REFERENCE,
          ctx.getStart().getLine());
      _currentNeighbor.getIpv4AddressFamily().setActivate(ipv4);
      _currentNeighbor.getIpv6AddressFamily().setActivate(ipv6);
    }
    _currentNeighbor.setPeerGroup(peerGroupName);
  }

  @Override
  public void exitRbn_remote_as(Rbn_remote_asContext ctx) {
    boolean ipv4 = false;
    boolean ipv6 = false;
    if (Ip.tryParse(_currentNeighborName).isPresent()) {
      ipv4 = true;
    } else if (Ip6.tryParse(_currentNeighborName).isPresent()) {
      ipv6 = true;
    } else if (_currentPeerGroup == null) {
      _w.redFlag(
          String.format(
              "Cannot assign remote-as to non-existent peer-group: '%s' in: '%s'",
              _currentNeighborName, getFullText(ctx.getParent().getParent())));
      return;
    }
    if (_currentAbstractNeighbor == null) {
      _currentAbstractNeighbor =
          _currentBgpProcess.getNeighbors().computeIfAbsent(_currentNeighborName, BgpNeighbor::new);
      defineStructure(F5BigipStructureType.BGP_NEIGHBOR, _currentNeighborName, ctx.parent);
      _c.referenceStructure(
          F5BigipStructureType.BGP_NEIGHBOR,
          _currentNeighborName,
          F5BigipStructureUsage.BGP_NEIGHBOR_SELF_REFERENCE,
          ctx.getStart().getLine());
      _currentAbstractNeighbor.getIpv4AddressFamily().setActivate(ipv4);
      _currentAbstractNeighbor.getIpv6AddressFamily().setActivate(ipv6);
    }
    _currentAbstractNeighbor.setRemoteAs(toLong(ctx.remoteas));
  }

  @Override
  public void exitRbn_route_map_out(Rbn_route_map_outContext ctx) {
    String routeMapName = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    boolean ipv4;
    boolean ipv6;
    if (Ip.tryParse(_currentNeighborName).isPresent()) {
      _c.referenceStructure(
          F5BigipStructureType.ROUTE_MAP,
          routeMapName,
          F5BigipStructureUsage.BGP_NEIGHBOR_IPV4_ROUTE_MAP_OUT,
          line);
      ipv4 = true;
      ipv6 = false;
    } else if (Ip6.tryParse(_currentNeighborName).isPresent()) {
      _c.referenceStructure(
          F5BigipStructureType.ROUTE_MAP,
          routeMapName,
          F5BigipStructureUsage.BGP_NEIGHBOR_IPV6_ROUTE_MAP_OUT,
          line);
      ipv4 = false;
      ipv6 = true;
    } else {
      _c.referenceStructure(
          F5BigipStructureType.ROUTE_MAP,
          routeMapName,
          F5BigipStructureUsage.BGP_PEER_GROUP_ROUTE_MAP_OUT,
          line);
      ipv4 = true;
      ipv6 = true;
    }
    if (_currentAbstractNeighbor == null) {
      _w.redFlag(
          String.format(
              "Cannot assign outbound route-map to non-existent neighbor: '%s' in: '%s'",
              _currentNeighborName, getFullText(ctx.getParent().getParent())));
      return;
    }
    if (ipv4) {
      _currentAbstractNeighbor.getIpv4AddressFamily().setRouteMapOut(routeMapName);
    }
    if (ipv6) {
      _currentAbstractNeighbor.getIpv6AddressFamily().setRouteMapOut(routeMapName);
    }
  }

  @Override
  public void exitRmm_ip_address(Rmm_ip_addressContext ctx) {
    String name = ctx.name.getText();
    _c.referenceStructure(
        F5BigipStructureType.ACCESS_LIST,
        name,
        F5BigipStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS,
        ctx.name.getStart().getLine());
    _currentRouteMapEntry.setMatchAccessList(new MatchAccessList(name));
  }

  @Override
  public void exitRmm_ip_address_prefix_list(Rmm_ip_address_prefix_listContext ctx) {
    String name = ctx.name.getText();
    _c.referenceStructure(
        F5BigipStructureType.PREFIX_LIST,
        name,
        F5BigipStructureUsage.ROUTE_MAP_MATCH_IPV4_ADDRESS_PREFIX_LIST,
        ctx.name.getStart().getLine());
    _currentRouteMapEntry.setMatchPrefixList(new RouteMapMatchPrefixList(name));
  }

  @Override
  public void exitRms_community(Rms_communityContext ctx) {
    _currentRouteMapEntry.setSetCommunity(
        new RouteMapSetCommunity(
            ctx.communities.stream()
                .map(this::toLong)
                .filter(Objects::nonNull)
                .collect(ImmutableSet.toImmutableSet())));
  }

  @Override
  public void exitRms_metric(Rms_metricContext ctx) {
    _currentRouteMapEntry.setSetMetric(new RouteMapSetMetric(toLong(ctx.metric)));
  }

  @Override
  public void exitRms_origin(Rms_originContext ctx) {
    _currentRouteMapEntry.setSetOrigin(new RouteMapSetOrigin(toOriginType(ctx.origin)));
  }

  @Override
  public void exitS_access_list(S_access_listContext ctx) {
    String name = ctx.name.getText();
    Prefix prefix = toPrefix(ctx.ip_spec());
    defineStructure(F5BigipStructureType.ACCESS_LIST, name, ctx);
    if (prefix == null) {
      _w.redFlag(
          String.format(
              "Invalid source IP specifier: '%s' in: '%s'",
              ctx.ip_spec().getText(), getFullText(ctx)));
      return;
    }
    _c.getAccessLists()
        .computeIfAbsent(name, AccessList::new)
        .getLines()
        .add(new AccessListLine(toLineAction(ctx.action), prefix, getFullText(ctx)));
  }

  @Override
  public void exitS_ip_prefix_list(S_ip_prefix_listContext ctx) {
    String name = ctx.name.getText();
    long num = toLong(ctx.num);
    Prefix prefix = toPrefix(ctx.prefix);
    int prefixLength = prefix.getPrefixLength();
    Integer ge = ctx.ge != null ? toInteger(ctx.ge) : null;
    Integer le = ctx.le != null ? toInteger(ctx.le) : null;
    int low = prefixLength;
    int high = Prefix.MAX_PREFIX_LENGTH;
    if (ge != null) {
      if (ge < prefixLength) {
        // Ineffectual, so warn
        _w.redFlag(
            String.format(
                "ge (min) arg '%d' less than prefix-length '%d' in: %s",
                ge, prefixLength, getFullText(ctx)));
      } else {
        low = ge;
      }
    }
    if (le != null) {
      if (le < prefixLength) {
        // Invalid and cannot match anything, so warn and do not add
        _w.redFlag(
            String.format(
                "le (max) arg '%d' less than prefix-length '%d' in: %s",
                le, prefixLength, getFullText(ctx)));
        return;
      } else if (ge != null && le < ge) {
        // Invalid and cannot match anything, so warn and do not add
        _w.redFlag(
            String.format(
                "le (max) arg '%d' less than ge (min) arg '%d' in: %s", le, ge, getFullText(ctx)));
        return;
      } else {
        high = le;
      }
    }
    defineStructure(F5BigipStructureType.PREFIX_LIST, name, ctx);
    PrefixListEntry entry = new PrefixListEntry(num);
    entry.setAction(toLineAction(ctx.action));
    entry.setPrefix(prefix);
    entry.setLengthRange(new SubRange(low, high));
    _c.getPrefixLists().computeIfAbsent(name, PrefixList::new).getEntries().put(num, entry);
  }

  @Override
  public void exitS_route_map(S_route_mapContext ctx) {
    _currentRouteMapEntry = null;
  }

  @Override
  public void exitS_router_bgp(S_router_bgpContext ctx) {
    _currentBgpProcess = null;
  }

  private @Nonnull String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  private @Nullable Long toLong(Standard_communityContext ctx) {
    if (ctx.STANDARD_COMMUNITY() != null) {
      return StandardCommunity.parse(ctx.getText()).asLong();
    } else {
      return convProblem(Long.class, ctx, null);
    }
  }

  private @Nullable Prefix toPrefix(Ip_specContext ctx) {
    if (ctx.ANY() != null) {
      return Prefix.ZERO;
    } else if (ctx.prefix != null) {
      return toPrefix(ctx.prefix);
    } else {
      return convProblem(Prefix.class, ctx, null);
    }
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
