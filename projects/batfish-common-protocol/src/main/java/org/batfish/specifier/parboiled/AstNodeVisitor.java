package org.batfish.specifier.parboiled;

interface AstNodeVisitor<T> {
  T visitAddressGroupIpSpaceAstNode(AddressGroupIpSpaceAstNode addressGroupIpSpaceAstNode);

  T visitCommaIpSpaceAstNode(CommaIpSpaceAstNode commaIpSpaceAstNode);

  T visitIpAstNode(IpAstNode ipAstNode);

  T visitIpWildcardAstNode(IpWildcardAstNode ipWildcardAstNode);

  T visitPrefixAstNode(PrefixAstNode prefixAstNode);

  T visitIpRangeAstNode(IpRangeAstNode rangeIpSpaceAstNode);

  T visitStringAstNode(StringAstNode stringAstNode);

  T visitUnionInterfaceSpecAstNode(UnionInterfaceAstNode unionInterfaceSpecAstNode);

  T visitDifferenceInterfaceSpecAstNode(DifferenceInterfaceAstNode differenceInterfaceAstNode);

  T visitConnectedToInterfaceSpecAstNode(ConnectedToInterfaceAstNode connectedToInterfaceAstNode);

  T visitTypeInterfaceSpecAstNode(TypeInterfaceAstNode typeInterfaceSpecAstNode);

  T visitInterfaceTypeAstNode(InterfaceTypeAstNode interfaceTypeAstNode);

  T visitNameInterfaceSpecAstNode(NameInterfaceAstNode nameInterfaceSpecAstNode);

  T visitNameRegexInterfaceSpecAstNode(NameRegexInterfaceAstNode nameRegexInterfaceSpecAstNode);

  T visitVrfInterfaceSpecAstNode(VrfInterfaceAstNode vrfInterfaceSpecAstNode);

  T visitZoneInterfaceSpecAstNode(ZoneInterfaceAstNode zoneInterfaceSpecAstNode);

  T visitInterfaceGroupInterfaceSpecAstNode(
      InterfaceGroupInterfaceAstNode interfaceGroupInterfaceAstNode);

  T visitIntersectionInterfaceSpecAstNode(
      IntersectionInterfaceAstNode intersectionInterfaceSpecAstNode);
}
