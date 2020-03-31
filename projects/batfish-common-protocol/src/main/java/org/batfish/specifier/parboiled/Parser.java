package org.batfish.specifier.parboiled;

import static org.batfish.specifier.parboiled.Anchor.Type.ADDRESS_GROUP_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_ICMP;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_ICMP_TYPE;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_ICMP_TYPE_CODE;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_PORT;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_PORTS;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_PORT_RANGE;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_TCP;
import static org.batfish.specifier.parboiled.Anchor.Type.APP_UDP;
import static org.batfish.specifier.parboiled.Anchor.Type.DEPRECATED;
import static org.batfish.specifier.parboiled.Anchor.Type.ENUM_SET_NOT;
import static org.batfish.specifier.parboiled.Anchor.Type.ENUM_SET_REGEX;
import static org.batfish.specifier.parboiled.Anchor.Type.ENUM_SET_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.ENUM_SET_VALUE;
import static org.batfish.specifier.parboiled.Anchor.Type.FILTER_INTERFACE_IN;
import static org.batfish.specifier.parboiled.Anchor.Type.FILTER_INTERFACE_OUT;
import static org.batfish.specifier.parboiled.Anchor.Type.FILTER_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.FILTER_NAME_REGEX;
import static org.batfish.specifier.parboiled.Anchor.Type.FILTER_PARENS;
import static org.batfish.specifier.parboiled.Anchor.Type.FILTER_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.HIDDEN;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_CONNECTED_TO;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_GROUP_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_NAME_REGEX;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_PARENS;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_TYPE;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_VRF;
import static org.batfish.specifier.parboiled.Anchor.Type.INTERFACE_ZONE;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_ADDRESS;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_ADDRESS_MASK;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_PREFIX;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_PROTOCOL_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_PROTOCOL_NOT;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_PROTOCOL_NUMBER;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_PROTOCOL_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_RANGE;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_SPACE_PARENS;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_SPACE_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.IP_WILDCARD;
import static org.batfish.specifier.parboiled.Anchor.Type.LOCATION_ENTER;
import static org.batfish.specifier.parboiled.Anchor.Type.LOCATION_PARENS;
import static org.batfish.specifier.parboiled.Anchor.Type.LOCATION_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.NAME_SET_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.NAME_SET_REGEX;
import static org.batfish.specifier.parboiled.Anchor.Type.NAME_SET_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_AND_FILTER;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_AND_FILTER_TAIL;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_AND_INTERFACE;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_AND_INTERFACE_TAIL;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_NAME_REGEX;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_PARENS;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_ROLE_AND_DIMENSION;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_ROLE_AND_DIMENSION_TAIL;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_ROLE_DIMENSION_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_ROLE_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.NODE_TYPE;
import static org.batfish.specifier.parboiled.Anchor.Type.REFERENCE_BOOK_AND_ADDRESS_GROUP;
import static org.batfish.specifier.parboiled.Anchor.Type.REFERENCE_BOOK_AND_ADDRESS_GROUP_TAIL;
import static org.batfish.specifier.parboiled.Anchor.Type.REFERENCE_BOOK_AND_INTERFACE_GROUP;
import static org.batfish.specifier.parboiled.Anchor.Type.REFERENCE_BOOK_AND_INTERFACE_GROUP_TAIL;
import static org.batfish.specifier.parboiled.Anchor.Type.REFERENCE_BOOK_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.ROUTING_POLICY_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.ROUTING_POLICY_NAME_REGEX;
import static org.batfish.specifier.parboiled.Anchor.Type.ROUTING_POLICY_PARENS;
import static org.batfish.specifier.parboiled.Anchor.Type.ROUTING_POLICY_SET_OP;
import static org.batfish.specifier.parboiled.Anchor.Type.VRF_NAME;
import static org.batfish.specifier.parboiled.Anchor.Type.ZONE_NAME;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.batfish.common.util.isp.IspModelingUtils;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.InterfaceType;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.support.Var;

/**
 * A parboiled-based parser for flexible specifiers. The rules for all types of expressions are in
 * this file (not sure how to put them in different files when they point to each other).
 *
 * <p>{@link Anchor} annotation: For each path from the top-level rule of an expression to leaf
 * values, there should be at least one rule with a Anchor annotation. This annotation is used for
 * error messages and generating auto completion suggestions. Character and string literals are
 * treated as implicit anchors. See findPathAnchor() in {@link ParserUtils}.
 */
@SuppressWarnings({
  "checkstyle:methodname", // this class uses idiomatic names
  "WeakerAccess", // access of Rule methods is needed for parser auto-generation.
})
public class Parser extends CommonParser {

  static final Map<String, Anchor.Type> ANCHORS = initAnchors(Parser.class);

  /**
   * An array of Rules for matching enum values. They should have been private and static but
   * parboiled does not like those things in this context.
   */
  final Rule[] _deviceTypeRules = initValuesRules(Arrays.asList(DeviceType.values()));

  final Rule[] _interfaceTypeRules = initValuesRules(Arrays.asList(InterfaceType.values()));

  static Parser instance() {
    return Parboiled.createParser(Parser.class);
  }

  @Override
  Rule getInputRule(Grammar grammar) {
    switch (grammar) {
      case APPLICATION_SPECIFIER:
        return input(AppSpec());
      case BGP_PEER_PROPERTY_SPECIFIER:
      case BGP_PROCESS_PROPERTY_SPECIFIER:
      case BGP_SESSION_COMPAT_STATUS_SPECIFIER:
      case BGP_SESSION_STATUS_SPECIFIER:
      case BGP_SESSION_TYPE_SPECIFIER:
        return input(EnumSetSpec(Grammar.getEnumValues(grammar)));
      case FILTER_SPECIFIER:
        return input(FilterSpec());
      case INTERFACE_PROPERTY_SPECIFIER:
        return input(EnumSetSpec(Grammar.getEnumValues(grammar)));
      case INTERFACE_SPECIFIER:
        return input(InterfaceSpec());
      case IP_PROTOCOL_SPECIFIER:
        return input(IpProtocolSpec());
      case IP_SPACE_SPECIFIER:
        return input(IpSpaceSpec());
      case IPSEC_SESSION_STATUS_SPECIFIER:
        return input(EnumSetSpec(Grammar.getEnumValues(grammar)));
      case LOCATION_SPECIFIER:
        return input(LocationSpec());
      case MLAG_ID_SPECIFIER:
        return input(NameSetSpec());
      case NAMED_STRUCTURE_SPECIFIER:
        return input(EnumSetSpec(Grammar.getEnumValues(grammar)));
      case NODE_PROPERTY_SPECIFIER:
        return input(EnumSetSpec(Grammar.getEnumValues(grammar)));
      case NODE_SPECIFIER:
        return input(NodeSpec());
      case OSPF_INTERFACE_PROPERTY_SPECIFIER:
      case OSPF_PROCESS_PROPERTY_SPECIFIER:
      case OSPF_SESSION_STATUS_SPECIFIER:
      case ROUTING_PROTOCOL_SPECIFIER:
        return input(EnumSetSpec(Grammar.getEnumValues(grammar)));
      case ROUTING_POLICY_SPECIFIER:
        return input(RoutingPolicySpec());
      case VXLAN_VNI_PROPERTY_SPECIFIER:
        return input(EnumSetSpec(Grammar.getEnumValues(grammar)));
      default:
        throw new IllegalArgumentException(
            "Main grammar rule not defined for " + grammar.getFriendlyName());
    }
  }

  /** Matches Reference Book name. */
  @Anchor(REFERENCE_BOOK_NAME)
  public Rule ReferenceBook() {
    return Sequence(NameLiteral(), WhiteSpace());
  }

  /**
   * Application specifier grammar
   *
   * <pre>
   * appSpec := appTerm
   *           | appTerm, appSpec
   *
   * appTerm := namedApp | ICMP (/ icmpTypeSpec)? | TCP (/ ports-spec)? | UDP (/ ports-spec)?
   *
   * namedApp := HTTP | HTTPS | …
   *
   * icmpTypeSpec := namedIcmpType | type (/ code)?
   *
   * namedIcmpType := echo-request | echo-response, ...
   *
   * portsSpec := portsTerm | portsTerm, portsSpec
   *
   * portsTerm := number (- number)?
   * </pre>
   */

  /** A Filter expression is one or more intersection terms separated by , or \ */
  @Anchor(APP_SET_OP)
  public Rule AppSpec() {
    return Sequence(
        AppTerm(),
        WhiteSpace(),
        ZeroOrMore(", ", AppTerm(), push(new UnionAppAstNode(pop(1), pop())), WhiteSpace()));
  }

  public Rule AppTerm() {
    return FirstOf(AppName(), AppNameRegex(), AppIcmpTerm(), AppTcpTerm(), AppUdpTerm());
  }

  @Anchor(APP_NAME)
  public Rule AppName() {
    return Sequence(
        EnumValue(),
        ACTION(namedApplications.contains(match().toUpperCase())),
        push(new NameAppAstNode(match())));
  }

  /**
   * For backward compatibility with the old specifier that allowed regex over the six named
   * applications in {@link org.batfish.datamodel.Protocol}s it supported.
   */
  @Anchor(DEPRECATED)
  public Rule AppNameRegex() {
    return Sequence(Regex(), push(new RegexAppAstNode(pop())));
  }

  @Anchor(APP_ICMP)
  public Rule AppIcmpTerm() {
    return Sequence(
        IgnoreCase("icmp"), WhiteSpace(), push(new IcmpAllAppAstNode()), Optional(AppIcmpType()));
  }

  @Anchor(APP_ICMP_TYPE)
  public Rule AppIcmpType() {
    return Sequence(
        "/ ",
        Number(),
        ACTION(pop() != null),
        push(new IcmpTypeAppAstNode(Integer.parseInt(match()))),
        WhiteSpace(),
        Optional(AppIcmpTypeCode()));
  }

  @Anchor(APP_ICMP_TYPE_CODE)
  public Rule AppIcmpTypeCode() {
    return Sequence(
        "/ ", Number(), push(IcmpTypeCodeAppAstNode.create(pop(), Integer.parseInt(match()))));
  }

  @Anchor(APP_TCP)
  public Rule AppTcpTerm() {
    return Sequence(
        IgnoreCase("tcp"), WhiteSpace(), push(new TcpAppAstNode()), Optional(AppPortSpec()));
  }

  @Anchor(APP_UDP)
  public Rule AppUdpTerm() {
    return Sequence(
        IgnoreCase("udp"), WhiteSpace(), push(new UdpAppAstNode()), Optional(AppPortSpec()));
  }

  @Anchor(APP_PORTS)
  public Rule AppPortSpec() {
    return Sequence(
        "/ ", AppPortTerm(), WhiteSpace(), ZeroOrMore(", ", AppPortTerm(), WhiteSpace()));
  }

  public Rule AppPortTerm() {
    return FirstOf(AppPortRange(), AppPort());
  }

  @Anchor(APP_PORT)
  public Rule AppPort() {
    return Sequence(Number(), push(PortAppAstNode.createFrom(pop(), match())));
  }

  @Anchor(APP_PORT_RANGE)
  public Rule AppPortRange() {
    return Sequence(
        Number(),
        push(new StringAstNode(match())),
        WhiteSpace(),
        "- ",
        Number(),
        push(PortAppAstNode.createFrom(pop(1), ((StringAstNode) pop(0)).getStr(), match())));
  }

  /**
   * Enum set grammar. Shared grammar for multiple enum types, or more precisely, over a known,
   * network-independent set of {@code values}. The values may be for Java enums such as {@link
   * org.batfish.datamodel.Protocol} or properties in {@link
   * org.batfish.datamodel.questions.NodePropertySpecifier}
   *
   * <pre>
   *   enumSetSpec := enumSetTerm [(,|&|\) enumSetTerm]*
   *
   *   enumSetTerm := enumVal
   *                 | regex over values
   * </pre>
   */

  /** An enumSetSpec is one or more terms separated by , */
  @Anchor(ENUM_SET_SET_OP)
  public <T> Rule EnumSetSpec(Collection<T> values) {
    return Sequence(
        EnumSetTerm(values),
        WhiteSpace(),
        ZeroOrMore(
            ", ", EnumSetTerm(values), push(new UnionEnumSetAstNode(pop(1), pop())), WhiteSpace()));
  }

  public <T> Rule EnumSetTerm(Collection<T> values) {
    return FirstOf(EnumSetBase(values), EnumSetNotTerm(values));
  }

  public <T> Rule EnumSetBase(Collection<T> values) {
    return FirstOf(EnumSetRegexDeprecated(), EnumSetRegex(), EnumSetValue(values));
  }

  @Anchor(ENUM_SET_VALUE)
  public <T> Rule EnumSetValue(Collection<T> allValues) {
    return Sequence(
        EnumValue(),
        ACTION(ValueEnumSetAstNode.isValidValue(match(), allValues)),
        push(new ValueEnumSetAstNode<>(match(), allValues)));
  }

  @Anchor(ENUM_SET_REGEX)
  public Rule EnumSetRegex() {
    return Sequence(Regex(), push(new RegexEnumSetAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule EnumSetRegexDeprecated() {
    return Sequence(RegexDeprecated(), push(new RegexEnumSetAstNode(pop())));
  }

  @Anchor(ENUM_SET_NOT)
  public <T> Rule EnumSetNotTerm(Collection<T> values) {
    return Sequence("! ", EnumSetBase(values), push(new NotEnumSetAstNode(pop())));
  }

  /**
   * Filter grammar
   *
   * <pre>
   *   filterSpec := filterTerm [{@literal &} | , | \ filterTerm]*
   *
   *   filterTerm := filterWithNode
   *                    | filterWithoutNode
   *                    | ( filterSpec )
   *
   *   filterWithNode := nodeTerm [filterWithoutNode]
   *
   *   filterWithoutNode := filterWithoutNodeTerm [{@literal &} | , | \ filterWithoutNodeTerm]*
   *
   *   filterWithoutNodeTerm := @in(interfaceSpec)  // inFilterOf is also supported for back compat
   *               | @out(interfaceSpec) // outFilterOf is also supported
   *               | filterName
   *               | filterNameRegex
   *               | ( filterWithoutNode )
   * </pre>
   */

  /** A Filter expression is one or more intersection terms separated by , or \ */
  @Anchor(FILTER_SET_OP)
  public Rule FilterSpec() {
    Var<Character> op = new Var<>();
    return Sequence(
        FilterIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            FilterIntersection(),
            push(SetOpFilterAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule FilterIntersection() {
    return Sequence(
        FilterTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ", FilterTerm(), push(new IntersectionFilterAstNode(pop(1), pop())), WhiteSpace()));
  }

  public Rule FilterTerm() {
    return FirstOf(FilterWithNode(), FilterWithoutNode(), FilterParens());
  }

  @Anchor(NODE_AND_FILTER)
  public Rule FilterWithNode() {
    return Sequence(
        NodeTerm(),
        WhiteSpace(),
        FilterWithNodeTail(),
        push(new FilterWithNodeFilterAstNode(pop(1), pop())));
  }

  @Anchor(NODE_AND_FILTER_TAIL)
  public Rule FilterWithNodeTail() {
    return Sequence("[ ", FilterWithoutNode(), WhiteSpace(), CloseBrackets());
  }

  @Anchor(FILTER_SET_OP)
  public Rule FilterWithoutNode() {
    Var<Character> op = new Var<>();
    return Sequence(
        FilterWithoutNodeIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            FilterWithoutNodeIntersection(),
            push(SetOpFilterAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule FilterWithoutNodeIntersection() {
    return Sequence(
        FilterWithoutNodeTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ",
            FilterWithoutNodeTerm(),
            push(new IntersectionFilterAstNode(pop(1), pop())),
            WhiteSpace()));
  }

  public Rule FilterWithoutNodeTerm() {
    return FirstOf(
        FilterInterfaceIn(),
        FilterInterfaceOut(),
        FilterDirectionDeprecated(),
        FilterNameRegexDeprecated(),
        FilterNameRegex(),
        FilterName(),
        FilterWithoutNodeParens());
  }

  @Anchor(FILTER_INTERFACE_IN)
  public Rule FilterInterfaceIn() {
    return Sequence(
        IgnoreCase("@in"),
        WhiteSpace(),
        "( ",
        InterfaceSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new InFilterAstNode((pop()))));
  }

  @Anchor(FILTER_INTERFACE_OUT)
  public Rule FilterInterfaceOut() {
    return Sequence(
        IgnoreCase("@out"),
        WhiteSpace(),
        "( ",
        InterfaceSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new OutFilterAstNode((pop()))));
  }

  @Anchor(DEPRECATED)
  public Rule FilterDirectionDeprecated() {
    Var<String> direction = new Var<>();
    return Sequence(
        FirstOf(IgnoreCase("inFilterOf"), IgnoreCase("outFilterOf")),
        direction.set(match()),
        WhiteSpace(),
        "( ",
        InterfaceSpec(),
        WhiteSpace(),
        CloseParens(),
        push(DirectionFilterAstNode.create(direction.get(), pop())));
  }

  @Anchor(FILTER_NAME)
  public Rule FilterName() {
    return Sequence(NameLiteral(), push(new NameFilterAstNode(pop())));
  }

  @Anchor(FILTER_NAME_REGEX)
  public Rule FilterNameRegex() {
    return Sequence(Regex(), push(new NameRegexFilterAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule FilterNameRegexDeprecated() {
    return Sequence(RegexDeprecated(), push(new NameRegexFilterAstNode(pop())));
  }

  @Anchor(FILTER_PARENS)
  public Rule FilterParens() {
    // Leave the stack as is -- no need to remember that this was a parenthetical term
    return Sequence("( ", FilterSpec(), WhiteSpace(), CloseParens());
  }

  // The anchor here is an approximation to simplify user messages
  @Anchor(FILTER_PARENS)
  public Rule FilterWithoutNodeParens() {
    // Leave the stack as is -- no need to remember that this was a parenthetical term
    return Sequence("( ", FilterWithoutNode(), WhiteSpace(), CloseParens());
  }

  /**
   * Interface grammar
   *
   * <pre>
   *   interfaceSpec := interfaceTerm [{@literal &} | , | \ interfaceTerm]*
   *
   *   interfaceTerm := interfaceWithNode
   *                    | interfaceWithoutNode
   *                    | ( interfaceSpec )
   *
   *   interfaceWithNode := nodeTerm [interfaceWithoutNode]
   *
   *   interfaceWithoutNode := interfaceWithoutNodeTerm [{@literal &} | , | \ interfaceWithoutNodeTerm]*
   *
   *   interfaceWithoutNodeTerm
   *                        := @connectedTo(ipSpaceSpec)  // non-@ versions also supported for back compat
   *                        | @interfacegroup(a, b)
   *                        | @interfaceType(interfaceType)
   *                        | @vrf(vrfName)
   *                        | @zone(zoneName)
   *                        | interfaceName
   *                        | interfaceNameRegex
   *                        | ( interfaceWithoutNode )
   * </pre>
   */

  /** An Interface expression is union and difference of one or more intersections */
  @Anchor(INTERFACE_SET_OP)
  public Rule InterfaceSpec() {
    Var<Character> op = new Var<>();
    return Sequence(
        InterfaceIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            InterfaceIntersection(),
            push(SetOpInterfaceAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule InterfaceIntersection() {
    return Sequence(
        InterfaceTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ",
            InterfaceTerm(),
            push(new IntersectionInterfaceAstNode(pop(1), pop())),
            WhiteSpace()));
  }

  public Rule InterfaceTerm() {
    return FirstOf(InterfaceWithNode(), InterfaceWithoutNode(), InterfaceParens());
  }

  @Anchor(NODE_AND_INTERFACE)
  public Rule InterfaceWithNode() {
    return Sequence(
        NodeTerm(),
        WhiteSpace(),
        InterfaceWithNodeTail(),
        push(new InterfaceWithNodeInterfaceAstNode(pop(1), pop())));
  }

  @Anchor(NODE_AND_INTERFACE_TAIL)
  public Rule InterfaceWithNodeTail() {
    return Sequence("[ ", InterfaceWithoutNode(), WhiteSpace(), CloseBrackets());
  }

  @Anchor(INTERFACE_SET_OP)
  public Rule InterfaceWithoutNode() {
    Var<Character> op = new Var<>();
    return Sequence(
        InterfaceWithoutNodeIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            InterfaceWithoutNodeIntersection(),
            push(SetOpInterfaceAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule InterfaceWithoutNodeIntersection() {
    return Sequence(
        InterfaceWithoutNodeTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ",
            InterfaceWithoutNodeTerm(),
            push(new IntersectionInterfaceAstNode(pop(1), pop())),
            WhiteSpace()));
  }

  public Rule InterfaceWithoutNodeTerm() {
    return FirstOf(
        InterfaceFunc(),
        InterfaceNameRegexDeprecated(),
        InterfaceNameRegex(),
        InterfaceName(),
        InterfaceWithoutNodeParens());
  }

  /**
   * To avoid ambiguity in location specification, interface and node functions should be distinct
   */
  public Rule InterfaceFunc() {
    return FirstOf(
        InterfaceConnectedTo(),
        InterfaceConnectedToDeprecated(),
        InterfaceInterfaceGroup(),
        InterfaceInterfaceGroupDeprecated(),
        InterfaceType(),
        InterfaceTypeDeprecated(),
        InterfaceVrf(),
        InterfaceVrfDeprecated(),
        InterfaceZone(),
        InterfaceZoneDeprecated());
  }

  @Anchor(INTERFACE_CONNECTED_TO)
  public Rule InterfaceConnectedTo() {
    return Sequence(
        IgnoreCase("@connectedTo"),
        WhiteSpace(),
        "( ",
        IpSpaceSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new ConnectedToInterfaceAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule InterfaceConnectedToDeprecated() {
    return Sequence(
        IgnoreCase("connectedTo"),
        WhiteSpace(),
        "( ",
        IpSpaceSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new ConnectedToInterfaceAstNode(pop())));
  }

  public Rule InterfaceInterfaceGroup() {
    return Sequence(
        IgnoreCase("@interfaceGroup"),
        WhiteSpace(),
        InterfaceGroupAndReferenceBook(),
        push(new InterfaceGroupInterfaceAstNode(pop(1), pop())));
  }

  @Anchor(DEPRECATED)
  public Rule InterfaceInterfaceGroupDeprecated() {
    return Sequence(
        IgnoreCase("ref.interfaceGroup"),
        WhiteSpace(),
        InterfaceGroupAndReferenceBook(),
        push(new InterfaceGroupInterfaceAstNode(pop(1), pop())));
  }

  /** Matches InterfaceGroup and ReferenceBook pair */
  @Anchor(REFERENCE_BOOK_AND_INTERFACE_GROUP)
  public Rule InterfaceGroupAndReferenceBook() {
    return Sequence("( ", ReferenceBook(), InterfaceGroupAndReferenceBookTail());
  }

  @Anchor(REFERENCE_BOOK_AND_INTERFACE_GROUP_TAIL)
  public Rule InterfaceGroupAndReferenceBookTail() {
    return Sequence(", ", InterfaceGroup(), CloseParens());
  }

  /** Matches Interface Group name */
  @Anchor(INTERFACE_GROUP_NAME)
  public Rule InterfaceGroup() {
    return Sequence(NameLiteral(), WhiteSpace());
  }

  @Anchor(INTERFACE_TYPE)
  public Rule InterfaceType() {
    return Sequence(
        IgnoreCase("@interfaceType"),
        WhiteSpace(),
        "( ",
        InterfaceTypeSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new TypeInterfaceAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule InterfaceTypeDeprecated() {
    return Sequence(
        IgnoreCase("type"),
        WhiteSpace(),
        "( ",
        InterfaceTypeSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new TypeInterfaceAstNode(pop())));
  }

  public Rule InterfaceTypeSpec() {
    return Sequence(FirstOf(_interfaceTypeRules), push(new StringAstNode(match())));
  }

  @Anchor(INTERFACE_VRF)
  public Rule InterfaceVrf() {
    return Sequence(
        IgnoreCase("@vrf"),
        WhiteSpace(),
        "( ",
        VrfName(),
        WhiteSpace(),
        CloseParens(),
        push(new VrfInterfaceAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule InterfaceVrfDeprecated() {
    return Sequence(
        IgnoreCase("vrf"),
        WhiteSpace(),
        "( ",
        VrfName(),
        WhiteSpace(),
        CloseParens(),
        push(new VrfInterfaceAstNode(pop())));
  }

  @Anchor(VRF_NAME)
  public Rule VrfName() {
    return NameLiteral();
  }

  @Anchor(INTERFACE_ZONE)
  public Rule InterfaceZone() {
    return Sequence(
        IgnoreCase("@zone"),
        WhiteSpace(),
        "( ",
        ZoneName(),
        WhiteSpace(),
        CloseParens(),
        push(new ZoneInterfaceAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule InterfaceZoneDeprecated() {
    return Sequence(
        IgnoreCase("zone"),
        WhiteSpace(),
        "( ",
        ZoneName(),
        WhiteSpace(),
        CloseParens(),
        push(new ZoneInterfaceAstNode(pop())));
  }

  @Anchor(ZONE_NAME)
  public Rule ZoneName() {
    return NameLiteral();
  }

  @Anchor(INTERFACE_NAME)
  public Rule InterfaceName() {
    return Sequence(NameLiteral(), push(new NameInterfaceAstNode(pop())));
  }

  @Anchor(INTERFACE_NAME_REGEX)
  public Rule InterfaceNameRegex() {
    return Sequence(Regex(), push(new NameRegexInterfaceAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule InterfaceNameRegexDeprecated() {
    return Sequence(RegexDeprecated(), push(new NameRegexInterfaceAstNode(pop())));
  }

  @Anchor(INTERFACE_PARENS)
  public Rule InterfaceParens() {
    // Leave the stack as is -- no need to remember that this was a parenthetical term
    return Sequence("( ", InterfaceSpec(), WhiteSpace(), CloseParens());
  }

  // The anchor here is an approximation to simplify user messages
  @Anchor(INTERFACE_PARENS)
  public Rule InterfaceWithoutNodeParens() {
    // Leave the stack as is -- no need to remember that this was a parenthetical term
    return Sequence("( ", InterfaceWithoutNode(), WhiteSpace(), CloseParens());
  }

  /**
   * IpProtocol grammar
   *
   * <pre>
   *   ipProtocolSpec := ipProtocolTerm [, ipProtocolTerm]*
   *
   *   ipProtocolTerm := ipProtocol
   *                     | ! ipProtocol
   *
   *   ipProtocol :=     Name      // e.g., TCP
   *                     | Number  // e.g., 51
   * </pre>
   */

  /** A IP protocol expression is one or more terms separated by */
  @Anchor(IP_PROTOCOL_SET_OP)
  public Rule IpProtocolSpec() {
    return Sequence(
        IpProtocolTerm(),
        WhiteSpace(),
        ZeroOrMore(
            ", ", IpProtocolTerm(), push(new UnionIpProtocolAstNode(pop(1), pop())), WhiteSpace()));
  }

  public Rule IpProtocolTerm() {
    return FirstOf(IpProtocol(), IpProtocolNot());
  }

  public Rule IpProtocol() {
    return FirstOf(IpProtocolName(), IpProtocolNumber());
  }

  @Anchor(IP_PROTOCOL_NOT)
  public Rule IpProtocolNot() {
    return Sequence("! ", IpProtocol(), push(new NotIpProtocolAstNode(pop())));
  }

  @Anchor(IP_PROTOCOL_NAME)
  public Rule IpProtocolName() {
    return Sequence(
        EnumValue(),
        ACTION(IpProtocolIpProtocolAstNode.isValidName(match())),
        push(new IpProtocolIpProtocolAstNode(match())));
  }

  @Anchor(IP_PROTOCOL_NUMBER)
  public Rule IpProtocolNumber() {
    return Sequence(
        Number(),
        ACTION(IpProtocolIpProtocolAstNode.isValidNumber(Integer.parseInt(match()))),
        push(new IpProtocolIpProtocolAstNode(match())));
  }

  /**
   * IpSpace grammar
   *
   * <pre>
   * ipSpaceSpec := ipSpecTerm [{@literal &} | , | \ ipSpecTerm]*
   *
   * ipSpecTerm := @addgressgroup(groupname, bookname)  //ref.addressgroup for back compat
   *               | locationSpec
   *               | ofLocation(locationSpec)           // back compat
   *               | ipPrefix (e.g., 1.1.1.0/24)
   *               | ipWildcard (e.g., 1.1.1.1:255.255.255.0)
   *               | ipRange (e.g., 1.1.1.1-1.1.1.2)
   *               | ipAddress (e.g., 1.1.1.1)
   *               | ( ipSpaceSpec )
   * </pre>
   */

  /* An IpSpace expression is union or difference of IpSpace intersection terms */
  @Anchor(IP_SPACE_SET_OP)
  public Rule IpSpaceSpec() {
    Var<Character> op = new Var<>();
    return Sequence(
        IpSpaceIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            IpSpaceIntersection(),
            push(SetOpIpSpaceAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule IpSpaceIntersection() {
    return Sequence(
        IpSpaceTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ",
            IpSpaceTerm(),
            push(new IntersectionIpSpaceAstNode(pop(1), pop())),
            WhiteSpace()));
  }

  /* An IpSpace term is one of these things */
  public Rule IpSpaceTerm() {
    return FirstOf(
        IpPrefix(),
        IpWildcard(),
        IpRange(),
        IpAddress(),
        IpSpaceAddressGroup(),
        IpSpaceAddressGroupDeprecated(),
        IpSpaceLocationDeprecated(),
        IpSpaceLocation(),
        IpSpaceParens());
  }

  @Anchor(IP_SPACE_PARENS)
  public Rule IpSpaceParens() {
    return Sequence("( ", IpSpaceSpec(), WhiteSpace(), CloseParens());
  }

  public Rule IpSpaceAddressGroup() {
    return Sequence(
        IgnoreCase("@addressgroup"),
        WhiteSpace(),
        AddressGroupAndReferenceBook(),
        push(new AddressGroupIpSpaceAstNode(pop(1), pop())));
  }

  @Anchor(DEPRECATED)
  public Rule IpSpaceAddressGroupDeprecated() {
    return Sequence(
        IgnoreCase("ref.addressgroup"),
        WhiteSpace(),
        AddressGroupAndReferenceBook(),
        push(new AddressGroupIpSpaceAstNode(pop(1), pop())));
  }

  /** Matches AddressGroup and ReferenceBook pair */
  @Anchor(REFERENCE_BOOK_AND_ADDRESS_GROUP)
  public Rule AddressGroupAndReferenceBook() {
    return Sequence("( ", ReferenceBook(), AddressGroupAndReferenceBookTail());
  }

  @Anchor(REFERENCE_BOOK_AND_ADDRESS_GROUP_TAIL)
  public Rule AddressGroupAndReferenceBookTail() {
    return Sequence(", ", AddressGroup(), CloseParens());
  }

  /** Matches AddressGroup name */
  @Anchor(ADDRESS_GROUP_NAME)
  public Rule AddressGroup() {
    return Sequence(NameLiteral(), WhiteSpace());
  }

  public Rule IpSpaceLocation() {
    return Sequence(LocationSpec(), push(new LocationIpSpaceAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule IpSpaceLocationDeprecated() {
    return Sequence(
        IgnoreCase("ofLocation"),
        WhiteSpace(),
        "( ",
        LocationSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new LocationIpSpaceAstNode(pop())));
  }

  /**
   * Matches IP addresses. Throws an exception if something matches syntactically but is invalid
   * semantically (e.g., 1.1.1.256)
   */
  @Anchor(IP_ADDRESS)
  public Rule IpAddress() {
    return Sequence(IpAddressUnchecked(), push(new IpAstNode(match())));
  }

  /**
   * Matches IP address mask (e.g., 255.255.255.0). It is syntactically similar to IP addresses but
   * we have a separate rule that helps with error messages and auto completion.
   */
  @Anchor(IP_ADDRESS_MASK)
  public Rule IpAddressMask() {
    return Sequence(IpAddressUnchecked(), push(new IpAstNode(match())));
  }

  /**
   * Matches IP prefixes. Throws an exception if something matches syntactically but is invalid
   * semantically (e.g., 1.1.1.2/33)
   */
  @Anchor(IP_PREFIX)
  public Rule IpPrefix() {
    return Sequence(IpPrefixUnchecked(), push(new PrefixAstNode(match())));
  }

  /** Matches Ip ranges (e.g., 1.1.1.1 - 1.1.1.2) */
  @Anchor(IP_RANGE)
  public Rule IpRange() {
    return Sequence(
        IpAddress(),
        WhiteSpace(),
        "- ",
        IpAddress(),
        push(new IpRangeAstNode(pop(1), pop())),
        WhiteSpace());
  }

  /** Matches Ip wildcards (e.g., 1.1.1.1:255.255.255.255) */
  @Anchor(IP_WILDCARD)
  public Rule IpWildcard() {
    return Sequence(IpAddress(), ':', IpAddressMask(), push(new IpWildcardAstNode(pop(1), pop())));
  }

  /**
   * Location grammar
   *
   * <pre>
   *   locationSpec := locationTerm [{@literal &} | , | \ locationTerm]*
   *
   *   locationTerm := locationInterface
   *               | @enter(locationInterface)   // non-@ versions also supported
   *               | @exit(locationInterface)
   *               | ( locationSpec )
   *
   *   locationInterface := nodeTerm[interfaceTerm]
   *                        | nodeTerm
   *                        | interfaceFunc
   * </pre>
   */

  /** A location expression is one or more intersection terms separated by + or - */
  @Anchor(LOCATION_SET_OP)
  public Rule LocationSpec() {
    Var<Character> op = new Var<>();
    return Sequence(
        LocationIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            LocationIntersection(),
            push(SetOpLocationAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule LocationIntersection() {
    return Sequence(
        LocationTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ",
            LocationTerm(),
            push(new IntersectionLocationAstNode(pop(1), pop())),
            WhiteSpace()));
  }

  public Rule LocationTerm() {
    return FirstOf(
        LocationInternet(),
        LocationEnterDeprecated(),
        LocationEnter(),
        LocationInterfaceDeprecated(),
        LocationInterface(),
        LocationParens());
  }

  @Anchor(HIDDEN)
  public Rule LocationInternet() {
    return Sequence(
        IgnoreCase(IspModelingUtils.INTERNET_HOST_NAME), push(InternetLocationAstNode.INSTANCE));
  }

  @Anchor(LOCATION_ENTER)
  public Rule LocationEnter() {
    return Sequence(
        IgnoreCase("@enter"),
        WhiteSpace(),
        "( ",
        LocationInterface(),
        CloseParens(),
        push(new EnterLocationAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule LocationEnterDeprecated() {
    return Sequence(
        IgnoreCase("enter"),
        WhiteSpace(),
        "( ",
        FirstOf(LocationInterfaceDeprecated(), LocationInterface()),
        CloseParens(),
        push(new EnterLocationAstNode(pop())));
  }

  /**
   * LocationInterface specifies interfaces to use as locations. It is not simply pointing to
   * interfaceSpec because of how we want to treat a simple string like "foo". We want to treat that
   * string as a node name, not an interface name which interfaceSpec or interfaceWithoutNode will
   * do. For that reason, valid inputs for LocationInterface are InterfaceWithNode, NodeTerm, and
   * InterfaceFunc. To avoid confusion, it is a requirement that functions in NodeTerm (e.g., @role)
   * and InterfaceFunc (e.g., @vrf) be distinct.
   */
  public Rule LocationInterface() {
    return FirstOf(
        Sequence(
            InterfaceWithNode(), push(InterfaceLocationAstNode.createFromInterfaceWithNode(pop()))),
        Sequence(NodeTerm(), WhiteSpace(), push(InterfaceLocationAstNode.createFromNode(pop()))),
        Sequence(
            InterfaceFunc(),
            WhiteSpace(),
            push(InterfaceLocationAstNode.createFromInterface(pop()))));
  }

  @Anchor(DEPRECATED)
  public Rule LocationInterfaceDeprecated() {
    return Sequence(
        // brackets without node expression
        "[ ",
        InterfaceSpec(),
        WhiteSpace(),
        CloseBrackets(),
        push(InterfaceLocationAstNode.createFromInterface(pop())));
  }

  @Anchor(LOCATION_PARENS)
  public Rule LocationParens() {
    // Leave the stack as is -- no need to remember that this was a parenthetical term
    return Sequence("( ", LocationSpec(), WhiteSpace(), CloseParens());
  }

  /**
   * Name set grammar. Shared grammar for multiple types of names such as structure names or MLAG
   * IDs.
   *
   * <pre>
   *   nameSetSpec := nameSetTerm [(,|&|\) nameSetTerm]*
   *
   *   nameSetTerm := nameVal
   *                 | regex over names
   * </pre>
   */

  /** An enumSetSpec is one or more terms separated by , */
  @Anchor(NAME_SET_SET_OP)
  public Rule NameSetSpec() {
    return Sequence(
        NameSetTerm(),
        WhiteSpace(),
        ZeroOrMore(
            ", ", NameSetTerm(), push(new UnionNameSetAstNode(pop(1), pop())), WhiteSpace()));
  }

  public <T> Rule NameSetTerm() {
    return FirstOf(NameSetRegexDeprecated(), NameSetRegex(), NameSetName());
  }

  @Anchor(NAME_SET_NAME)
  public <T> Rule NameSetName() {
    return Sequence(NameLiteral(), push(new SingletonNameSetAstNode(pop())));
  }

  @Anchor(NAME_SET_REGEX)
  public Rule NameSetRegex() {
    return Sequence(Regex(), push(new RegexNameSetAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule NameSetRegexDeprecated() {
    return Sequence(RegexDeprecated(), push(new RegexNameSetAstNode(pop())));
  }

  /**
   * Node grammar
   *
   * <pre>
   *   nodeSpec := nodeTerm [{@literal &} | , | \ nodeTerm]*
   *
   *   nodeTerm := @role(a, b) // ref.noderole is also supported for back compat
   *               | @deviceType(a)
   *               | nodeName
   *               | nodeNameRegex
   *               | ( nodeSpec )
   * </pre>
   */

  /* A Node expression is one or more intersection terms separated by + or - */
  @Anchor(NODE_SET_OP)
  public Rule NodeSpec() {
    Var<Character> op = new Var<>();
    return Sequence(
        NodeIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            NodeIntersection(),
            push(SetOpNodeAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule NodeIntersection() {
    return Sequence(
        NodeTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ", NodeTerm(), push(new IntersectionNodeAstNode(pop(1), pop())), WhiteSpace()));
  }

  public Rule NodeTerm() {
    return FirstOf(
        NodeRole(),
        NodeRoleDeprecated(),
        NodeType(),
        NodeNameRegexDeprecated(),
        NodeNameRegex(),
        NodeName(),
        NodeParens());
  }

  public Rule NodeRole() {
    return Sequence(
        IgnoreCase("@role"),
        WhiteSpace(),
        NodeRoleAndDimension(),
        push(new RoleNodeAstNode(pop(1), pop())));
  }

  @Anchor(DEPRECATED)
  public Rule NodeRoleDeprecated() {
    return Sequence(
        IgnoreCase("ref.noderole"),
        WhiteSpace(),
        NodeRoleAndDimension(),
        push(new RoleNodeAstNode(pop(1), pop())));
  }

  @Anchor(NODE_ROLE_AND_DIMENSION)
  public Rule NodeRoleAndDimension() {
    return Sequence("( ", NodeRoleDimensionName(), NodeRoleAndDimensionTail());
  }

  @Anchor(NODE_ROLE_AND_DIMENSION_TAIL)
  public Rule NodeRoleAndDimensionTail() {
    return Sequence(", ", NodeRoleName(), CloseParens());
  }

  /** Matches Node Role Dimension name */
  @Anchor(NODE_ROLE_DIMENSION_NAME)
  public Rule NodeRoleDimensionName() {
    return Sequence(NameLiteral(), WhiteSpace());
  }

  /** Matches Node Role name */
  @Anchor(NODE_ROLE_NAME)
  public Rule NodeRoleName() {
    return Sequence(NameLiteral(), WhiteSpace());
  }

  @Anchor(NODE_TYPE)
  public Rule NodeType() {
    return Sequence(
        IgnoreCase("@deviceType"),
        WhiteSpace(),
        "( ",
        NodeTypeSpec(),
        WhiteSpace(),
        CloseParens(),
        push(new TypeNodeAstNode(pop())));
  }

  public Rule NodeTypeSpec() {
    return Sequence(FirstOf(_deviceTypeRules), push(new StringAstNode(match())));
  }

  @Anchor(NODE_NAME)
  public Rule NodeName() {
    return Sequence(NameLiteral(), push(new NameNodeAstNode(pop())));
  }

  @Anchor(NODE_NAME_REGEX)
  public Rule NodeNameRegex() {
    return Sequence(Regex(), push(new NameRegexNodeAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule NodeNameRegexDeprecated() {
    return Sequence(RegexDeprecated(), push(new NameRegexNodeAstNode(pop())), WhiteSpace());
  }

  @Anchor(NODE_PARENS)
  public Rule NodeParens() {
    // Leave the stack as is -- no need to remember that this was a parenthetical term
    return Sequence("( ", NodeSpec(), WhiteSpace(), CloseParens());
  }

  /**
   * Routing policy grammar
   *
   * <pre>
   *   routingPolicySpec := routingPolicyTerm [{@literal &} | , | \ routingPolicyTerm]*
   *
   *   routingPolicyTerm :=
   *               | routingPolicyName
   *               | routingPolicyNameRegex
   *               | ( routingPolicySpec )
   * </pre>
   */

  /** A RoutingPolicy expression is one or more intersection terms separated by , or \ */
  @Anchor(ROUTING_POLICY_SET_OP)
  public Rule RoutingPolicySpec() {
    Var<Character> op = new Var<>();
    return Sequence(
        RoutingPolicyIntersection(),
        WhiteSpace(),
        ZeroOrMore(
            FirstOf(", ", "\\ "),
            op.set(matchedChar()),
            RoutingPolicyIntersection(),
            push(SetOpRoutingPolicyAstNode.create(op.get(), pop(1), pop())),
            WhiteSpace()));
  }

  public Rule RoutingPolicyIntersection() {
    return Sequence(
        RoutingPolicyTerm(),
        WhiteSpace(),
        ZeroOrMore(
            "& ",
            RoutingPolicyTerm(),
            push(new IntersectionRoutingPolicyAstNode(pop(1), pop())),
            WhiteSpace()));
  }

  public Rule RoutingPolicyTerm() {
    return FirstOf(
        RoutingPolicyNameRegexDeprecated(),
        RoutingPolicyNameRegex(),
        RoutingPolicyName(),
        RoutingPolicyParens());
  }

  @Anchor(ROUTING_POLICY_NAME)
  public Rule RoutingPolicyName() {
    return Sequence(NameLiteral(), push(new NameRoutingPolicyAstNode(pop())));
  }

  @Anchor(ROUTING_POLICY_NAME_REGEX)
  public Rule RoutingPolicyNameRegex() {
    return Sequence(Regex(), push(new NameRegexRoutingPolicyAstNode(pop())));
  }

  @Anchor(DEPRECATED)
  public Rule RoutingPolicyNameRegexDeprecated() {
    return Sequence(RegexDeprecated(), push(new NameRegexRoutingPolicyAstNode(pop())));
  }

  @Anchor(ROUTING_POLICY_PARENS)
  public Rule RoutingPolicyParens() {
    // Leave the stack as is -- no need to remember that this was a parenthetical term
    return Sequence("( ", RoutingPolicySpec(), WhiteSpace(), CloseParens());
  }
}
