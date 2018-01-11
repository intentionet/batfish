package org.batfish.grammar.routing_table.ios;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RouteBuilder;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.grammar.RoutingTableExtractor;
import org.batfish.grammar.routing_table.ios.IosRoutingTableParser.Ios_routing_tableContext;
import org.batfish.grammar.routing_table.ios.IosRoutingTableParser.ProtocolContext;
import org.batfish.grammar.routing_table.ios.IosRoutingTableParser.RouteContext;
import org.batfish.grammar.routing_table.ios.IosRoutingTableParser.Vrf_declarationContext;

public class IosRoutingTableExtractor extends IosRoutingTableParserBaseListener
    implements RoutingTableExtractor {

  private static int toInteger(Token t) {
    return Integer.parseInt(t.getText());
  }

  private String _currentVrfName;

  private SortedSet<Route> _currentVrfRoutes;

  private final String _hostname;

  private final Map<Ip, String> _ipOwners;

  @SuppressWarnings("unused")
  private IosRoutingTableCombinedParser _parser;

  private RoutesByVrf _routesByVrf;

  private final String _text;

  @SuppressWarnings("unused")
  private final Warnings _w;

  public IosRoutingTableExtractor(
      String hostname,
      String text,
      IosRoutingTableCombinedParser parser,
      Warnings w,
      IBatfish batfish) {
    _hostname = hostname;
    _text = text;
    _parser = parser;
    _w = w;
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, String> ipOwnersSimple = CommonUtil.computeIpOwnersSimple(configurations, true);
    _ipOwners = ipOwnersSimple;
  }

  private BatfishException convError(Class<?> type, ParserRuleContext ctx) {
    String typeName = type.getSimpleName();
    String txt = getFullText(ctx);
    return new BatfishException("Could not convert to " + typeName + ": " + txt);
  }

  @Override
  public void enterIos_routing_table(Ios_routing_tableContext ctx) {
    _routesByVrf = new RoutesByVrf();
  }

  @Override
  public void enterVrf_declaration(Vrf_declarationContext ctx) {
    _currentVrfName = ctx.identifier().getText();
    initVrf(_currentVrfName);
  }

  @Override
  public void exitRoute(RouteContext ctx) {
    if (_currentVrfName == null) {
      _currentVrfName = Configuration.DEFAULT_VRF_NAME;
      initVrf(_currentVrfName);
    }
    RoutingProtocol protocol = toProtocol(ctx.protocol());
    if (protocol == RoutingProtocol.LOCAL) {
      return;
    }
    Prefix prefix = Prefix.fromString(ctx.IP_PREFIX().getText());
    int admin;
    int cost;
    List<String> nextHopInterfaces =
        ctx.nexthopifaces
            .stream()
            .map(nextHopIfaceCtx -> nextHopIfaceCtx.getText())
            .collect(Collectors.toList());
    List<Ip> nextHopIps =
        ctx.nexthops
            .stream()
            .map(nextHopIpCtx -> new Ip(nextHopIpCtx.getText()))
            .collect(Collectors.toList());
    int numIterations = Math.max(nextHopIps.size(), nextHopInterfaces.size());
    for (int i = 0; i < numIterations; i++) {
      String nextHopInterface;
      Ip nextHopIp;
      if (nextHopInterfaces.isEmpty()) {
        nextHopInterface = Route.UNSET_NEXT_HOP_INTERFACE;
      } else {
        nextHopInterface = nextHopInterfaces.get(i);
      }
      if (ctx.IS_DIRECTLY_CONNECTED() != null) {
        if (protocol == RoutingProtocol.STATIC) {
          // static
          if (ctx.admin != null) {
            admin = toInteger(ctx.admin);
          } else {
            admin = 1;
          }
          if (ctx.cost != null) {
            cost = toInteger(ctx.cost);
          } else {
            cost = 0;
          }
        } else {
          // connected
          admin = 0;
          cost = 0;
        }
        nextHopIp = Route.UNSET_ROUTE_NEXT_HOP_IP;
      } else {
        admin = toInteger(ctx.admin);
        cost = toInteger(ctx.cost);
        nextHopIp = nextHopIps.get(i);
      }
      // TODO: support IOS route tags
      int tag = Route.UNSET_ROUTE_TAG;
      // TODO: support IOS next hop
      if (protocol == RoutingProtocol.BGP
          && admin
              > RoutingProtocol.BGP.getDefaultAdministrativeCost(ConfigurationFormat.CISCO_IOS)) {
        protocol = RoutingProtocol.IBGP;
      }
      RouteBuilder rb = new RouteBuilder();
      rb.setNode(_hostname);
      rb.setNetwork(prefix);
      if (protocol == RoutingProtocol.CONNECTED
          || (protocol == RoutingProtocol.STATIC && nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP))
          || Interface.NULL_INTERFACE_NAME.equals(nextHopInterface)) {
        rb.setNextHop(Configuration.NODE_NONE_NAME);
      }
      if (!nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
        rb.setNextHopIp(nextHopIp);
        String nextHop = _ipOwners.get(nextHopIp);
        if (nextHop != null) {
          rb.setNextHop(nextHop);
        }
      }
      if (nextHopInterface != null) {
        rb.setNextHopInterface(nextHopInterface);
      }
      rb.setAdministrativeCost(admin);
      rb.setCost(cost);
      rb.setProtocol(protocol);
      rb.setTag(tag);
      rb.setVrf(_currentVrfName);
      Route route = rb.build();
      _currentVrfRoutes.add(route);
    }
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  @Override
  public RoutesByVrf getRoutesByVrf() {
    return _routesByVrf;
  }

  private void initVrf(String vrfName) {
    _currentVrfRoutes = _routesByVrf.computeIfAbsent(vrfName, k -> new TreeSet<>());
  }

  @Override
  public void processParseTree(ParserRuleContext tree) {
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);
  }

  private RoutingProtocol toProtocol(ProtocolContext ctx) {
    String protocolStr = ctx.getText();
    switch (protocolStr) {
      case "B":
        return RoutingProtocol.BGP;

      case "C":
        return RoutingProtocol.CONNECTED;

      case "L":
        return RoutingProtocol.LOCAL;

      case "O":
        return RoutingProtocol.OSPF;

      case "IA":
        return RoutingProtocol.OSPF_IA;

      case "E1":
        return RoutingProtocol.OSPF_E1;

      case "E2":
        return RoutingProtocol.OSPF_E2;

      case "S":
        return RoutingProtocol.STATIC;

      case "L1":
        return RoutingProtocol.ISIS_L1;

      case "L2":
        return RoutingProtocol.ISIS_L2;

      default:
        throw convError(RoutingProtocol.class, ctx);
    }
  }
}
