package org.batfish.representation.cisco_nxos;

/** A visitor of {@link RouteMapMatch}. */
public interface RouteMapMatchVisitor<T> {

  T visitRouteMapMatchAsPath(RouteMapMatchAsPath routeMapMatchAsPath);

  T visitRouteMapMatchCommunity(RouteMapMatchCommunity routeMapMatchCommunity);

  T visitRouteMapMatchInterface(RouteMapMatchInterface routeMapMatchInterface);

  T visitRouteMapMatchIpAddress(RouteMapMatchIpAddress routeMapMatchIpAddress);

  T visitRouteMapMatchIpAddressPrefixList(
      RouteMapMatchIpAddressPrefixList routeMapMatchIpAddressPrefixList);

  T visitRouteMapMatchIpv6Address(RouteMapMatchIpv6Address routeMapMatchIpv6Address);

  T visitRouteMapMatchIpv6AddressPrefixList(
      RouteMapMatchIpv6AddressPrefixList routeMapMatchIpv6AddressPrefixList);

  T visitRouteMapMatchMetric(RouteMapMatchMetric routeMapMatchMetric);

  T visitRouteMapMatchSourceProtocol(RouteMapMatchSourceProtocol routeMapMatchSourceProtocol);

  T visitRouteMapMatchTag(RouteMapMatchTag routeMapMatchTag);
}
