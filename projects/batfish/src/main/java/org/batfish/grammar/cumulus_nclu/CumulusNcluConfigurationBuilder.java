package org.batfish.grammar.cumulus_nclu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.MacAddress;
import org.batfish.datamodel.Prefix;
import org.batfish.grammar.ParseTreePrettyPrinter;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_bgpContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_bondContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_dot1xContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_hostnameContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_interfaceContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_loopbackContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_ptpContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_snmp_serverContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_timeContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_vlanContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_vrfContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.A_vxlanContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.B_autonomous_systemContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.B_ipv4_unicastContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.B_l2vpnContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.B_neighborContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.B_router_idContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.B_vrfContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bi4_neighborContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bi4_networkContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bi4_redistribute_connectedContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bi4_redistribute_staticContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bi4n_activateContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bi4n_route_reflector_clientContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ble_advertise_all_vniContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ble_advertise_default_gwContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ble_advertise_ipv4_unicastContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ble_neighborContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Blen_activateContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Blen_route_reflector_clientContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bn_interfaceContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bn_peerContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bn_peer_groupContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bnp_descriptionContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bnp_peer_groupContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bnp_remote_asContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bob_accessContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bob_pvidContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bob_vidsContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bobo_slavesContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bond_clag_idContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bond_ip_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Bond_vrfContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Brbr_portsContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Brbr_pvidContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Brbr_vidsContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Brbr_vlan_awareContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Cumulus_nclu_configurationContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Dn4Context;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Dn6Context;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Frr_exit_vrfContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Frr_unrecognizedContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Frr_usernameContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Frr_vrfContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Frrv_ip_routeContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.GlobContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Glob_range_setContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.I_ip_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.I_link_speedContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.I_vrfContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ib_accessContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ib_pvidContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ib_vidsContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ic_backup_ipContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ic_peer_ipContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ic_priorityContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ic_sys_macContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Interface_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ip_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ip_prefixContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Ipv6_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.L_ip_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Lc_vxlan_anycast_ipContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Line_actionContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Mac_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.R_defaults_datacenterContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.R_logContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.R_routeContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.R_route_mapContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.R_service_integrated_vtysh_configContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.RangeContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Range_setContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Rmm_interfaceContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.S_net_add_unrecognizedContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Uint16Context;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Uint32Context;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.V_ip_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.V_ip_address_virtualContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.V_vlan_idContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.V_vlan_raw_deviceContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.V_vrfContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vlan_idContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vlan_rangeContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vlan_range_setContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vni_numberContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vrf_ip_addressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vrf_vniContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vx_stpContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vxb_accessContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vxb_arp_nd_suppressContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vxb_learningContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vxv_idContext;
import org.batfish.grammar.cumulus_nclu.CumulusNcluParser.Vxv_local_tunnelipContext;
import org.batfish.representation.cumulus.BgpInterfaceNeighbor;
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
import org.batfish.representation.cumulus.Bond;
import org.batfish.representation.cumulus.CumulusInterfaceType;
import org.batfish.representation.cumulus.CumulusNcluConfiguration;
import org.batfish.representation.cumulus.CumulusRoutingProtocol;
import org.batfish.representation.cumulus.CumulusStructureType;
import org.batfish.representation.cumulus.CumulusStructureUsage;
import org.batfish.representation.cumulus.Interface;
import org.batfish.representation.cumulus.InterfaceClagSettings;
import org.batfish.representation.cumulus.RemoteAsType;
import org.batfish.representation.cumulus.RouteMap;
import org.batfish.representation.cumulus.RouteMapEntry;
import org.batfish.representation.cumulus.RouteMapMatchInterface;
import org.batfish.representation.cumulus.StaticRoute;
import org.batfish.representation.cumulus.Vlan;
import org.batfish.representation.cumulus.Vrf;
import org.batfish.representation.cumulus.Vxlan;

/**
 * A listener that builds a {@link CumulusNcluConfiguration} while walking a parse tree produced by
 * {@link CumulusNcluCombinedParser#parse}.
 */
public class CumulusNcluConfigurationBuilder extends CumulusNcluParserBaseListener {

  private static final Pattern NUMBERED_WORD_PATTERN = Pattern.compile("^(.*[^0-9])([0-9]+)$");
  private static final Pattern PHYSICAL_INTERFACE_PATTERN = Pattern.compile("(swp|eth)[0-9]+");
  private static final Pattern SUBINTERFACE_PATTERN = Pattern.compile("^(.*)\\.([0-9]+)$");
  private static final Pattern VLAN_INTERFACE_PATTERN = Pattern.compile("^vlan([0-9]+)$");
  private static final int MAX_VXLAN_ID = (1 << 24) - 1; // 24 bit number

  private static int toInteger(Uint16Context ctx) {
    return Integer.parseInt(ctx.getText(), 10);
  }

  /**
   * Attempt to parse uint32 into an {@code int} value.
   *
   * @throws NumberFormatException if the value cannot be represented as a <em>signed</em> java
   *     integer
   */
  private static int toInteger(Uint32Context ctx) {
    return Integer.parseInt(ctx.getText(), 10);
  }

  private static int toInteger(Vlan_idContext ctx) {
    return Integer.parseInt(ctx.getText(), 10);
  }

  private static int toInteger(Vni_numberContext ctx) {
    return Integer.parseInt(ctx.getText(), 10);
  }

  private static @Nonnull ConcreteInterfaceAddress toInterfaceAddress(
      Interface_addressContext ctx) {
    return ConcreteInterfaceAddress.parse(ctx.getText());
  }

  private static @Nonnull Ip toIp(Ip_addressContext ctx) {
    return Ip.parse(ctx.getText());
  }

  private static @Nonnull Ip6 toIp6(Ipv6_addressContext ctx) {
    return Ip6.parse(ctx.getText());
  }

  private static @Nonnull LineAction toLineAction(Line_actionContext ctx) {
    if (ctx.PERMIT() != null) {
      return LineAction.PERMIT;
    } else {
      return LineAction.DENY;
    }
  }

  private static long toLong(Uint32Context ctx) {
    return Long.parseLong(ctx.getText(), 10);
  }

  private static @Nonnull MacAddress toMacAddress(Mac_addressContext ctx) {
    return MacAddress.parse(ctx.getText());
  }

  private static @Nonnull Prefix toPrefix(Ip_prefixContext ctx) {
    return Prefix.parse(ctx.getText());
  }

  private static @Nonnull Range<Long> toRange(RangeContext ctx) {
    long low = toLong(ctx.low);
    long high = ctx.high != null ? toLong(ctx.high) : low;
    return Range.closed(low, high);
  }

  /**
   * Convert a range context to an integer range.
   *
   * @throws IllegalArgumentException if the values are out of range
   */
  private static @Nonnull Range<Integer> toRangeInt(RangeContext ctx) {
    long low = toLong(ctx.low);
    long high = ctx.high != null ? toLong(ctx.high) : low;
    checkArgument(
        low <= Integer.MAX_VALUE && high <= Integer.MAX_VALUE,
        "Invalid integer range: %s",
        ctx.getText());
    return Range.closed((int) low, (int) high);
  }

  private static @Nonnull Range<Integer> toRange(Vlan_rangeContext ctx) {
    int low = toInteger(ctx.low);
    int high = ctx.high != null ? toInteger(ctx.high) : low;
    return Range.closed(low, high);
  }

  private static @Nonnull RangeSet<Long> toRangeSet(Range_setContext ctx) {
    return ctx.range().stream()
        .map(CumulusNcluConfigurationBuilder::toRange)
        .collect(ImmutableRangeSet.toImmutableRangeSet());
  }

  /**
   * Convert a range set context to a range set of integers.
   *
   * @throws IllegalArgumentException if the values are out of range
   */
  private static @Nonnull RangeSet<Integer> toRangeSetInt(Range_setContext ctx) {
    return ctx.range().stream()
        .map(CumulusNcluConfigurationBuilder::toRangeInt)
        .collect(ImmutableRangeSet.toImmutableRangeSet());
  }

  /**
   * Check that the given RangeSet is upper-bounded by {@code maxValue}, otherwise throw {@link
   * IllegalArgumentException}
   */
  private static void checkUpperBound(RangeSet<? extends Number> rangeSet, long maxValue) {
    Range<? extends Number> range =
        Iterables.getFirst(rangeSet.asDescendingSetOfRanges(), Range.singleton(maxValue));
    assert range != null; // range set won't give us null ranges
    Number upperBound = range.upperEndpoint();
    checkArgument(
        range.upperBoundType() == BoundType.CLOSED
            ? upperBound.longValue() <= maxValue
            : upperBound.longValue() < maxValue,
        "Invalid range %s, max value allowed is %d",
        rangeSet,
        maxValue);
  }

  private static @Nonnull RangeSet<Integer> toRangeSet(Vlan_range_setContext ctx) {
    return ctx.vlan_range().stream()
        .map(CumulusNcluConfigurationBuilder::toRange)
        .collect(ImmutableRangeSet.toImmutableRangeSet());
  }

  private static @Nonnull Set<String> toStrings(Glob_range_setContext ctx, long maxValue) {
    if (ctx.unnumbered != null) {
      return ImmutableSet.of(ctx.unnumbered.getText());
    }
    String baseWord = ctx.base_word.getText();
    if (ctx.first_interval_end == null && ctx.other_numeric_ranges == null) {
      return ImmutableSet.of(baseWord);
    }
    Matcher matcher = NUMBERED_WORD_PATTERN.matcher(baseWord);
    matcher.matches(); // parser+lexer guarantee match
    String prefix = matcher.group(1);
    long firstIntervalStart = Long.parseLong(matcher.group(2), 10);
    long firstIntervalEnd =
        ctx.first_interval_end != null
            ? Long.parseLong(ctx.first_interval_end.getText(), 10)
            : firstIntervalStart;
    checkArgument(firstIntervalStart <= maxValue && firstIntervalEnd <= maxValue);
    // add first interval
    ImmutableRangeSet.Builder<Long> builder =
        ImmutableRangeSet.<Long>builder().add(Range.closed(firstIntervalStart, firstIntervalEnd));
    if (ctx.other_numeric_ranges != null) {
      // add other intervals
      RangeSet<Long> rangeSet = toRangeSet(ctx.other_numeric_ranges);
      checkUpperBound(rangeSet, maxValue);
      builder.addAll(rangeSet);
    }
    return builder.build().asRanges().stream()
        .flatMapToLong(r -> LongStream.rangeClosed(r.lowerEndpoint(), r.upperEndpoint()))
        .mapToObj(i -> String.format("%s%d", prefix, i))
        .collect(ImmutableSet.toImmutableSet());
  }

  private static @Nonnull Set<String> toStrings(GlobContext ctx) {
    return toStrings(ctx, Long.MAX_VALUE);
  }

  private static @Nonnull Set<String> toStrings(GlobContext ctx, long maxValue) {
    return ctx.glob_range_set().stream()
        .flatMap(grs -> toStrings(grs, maxValue).stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  private @Nullable CumulusNcluConfiguration _c;
  private @Nullable BgpNeighbor _currentBgpNeighbor;
  private @Nullable String _currentBgpNeighborName;
  private @Nullable BgpProcess _currentBgpProcess;
  private @Nullable BgpVrf _currentBgpVrf;
  private @Nullable Bond _currentBond;
  private @Nullable List<Interface> _currentInterfaces;
  private @Nullable RouteMapEntry _currentRouteMapEntry;
  private @Nullable Vlan _currentVlan;
  private @Nullable List<Vlan> _currentVlans;
  private @Nullable Vrf _currentVrf;
  private @Nullable List<Vrf> _currentVrfs;
  private @Nullable List<Vxlan> _currentVxlans;
  private final @Nonnull CumulusNcluCombinedParser _parser;
  private final @Nonnull String _text;
  private final @Nonnull Warnings _w;

  public CumulusNcluConfigurationBuilder(
      CumulusNcluCombinedParser parser, String text, Warnings w) {
    _parser = parser;
    _text = text;
    _w = w;
  }

  @SuppressWarnings("unused")
  private @Nullable <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, @Nullable U defaultReturnValue) {
    _w.redFlag(
        String.format("Could not convert to %s: %s", returnType.getSimpleName(), getFullText(ctx)));
    return defaultReturnValue;
  }

  /**
   * Returns a newly-created {@link Bond} with given {@code name}, or {@code null} if {@code name}
   * is invalid.
   */
  private @Nullable Bond createBond(String name, A_bondContext ctx) {
    if (name.equals(CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME)
        || PHYSICAL_INTERFACE_PATTERN.matcher(name).matches()
        || SUBINTERFACE_PATTERN.matcher(name).matches()
        || VLAN_INTERFACE_PATTERN.matcher(name).matches()) {
      _w.redFlag(String.format("Invalid name '%s' for bond in: %s", name, getFullText(ctx)));
      return null;
    }
    if (_c.getVrfs().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for bond clashes with existing vrf in: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (_c.getVxlans().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for bond clashes with existing vxlan in: %s",
              name, getFullText(ctx)));
      return null;
    }
    return new Bond(name);
  }

  /**
   * Returns a newly-created {@link Interface} with given {@code name}, or {@code null} if {@code
   * name} is invalid.
   */
  private @Nullable Interface createInterface(String name, ParserRuleContext ctx) {
    Matcher subinterfaceMatcher = SUBINTERFACE_PATTERN.matcher(name);
    Integer encapsulationVlan = null;
    CumulusInterfaceType type;
    String superInterfaceName = null;

    // Early exits
    if (name.equals(CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME)) {
      _w.redFlag(
          String.format(
              "Loopback interface can only be configured via 'net add loopback' family of commands; following is invalid: %s",
              getFullText(ctx)));
      return null;
    }
    if (_c.getBonds().containsKey(name)) {
      _w.redFlag(
          String.format(
              "bond interface '%s' can only be configured via 'net add bond' family of commands; following is invalid: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (_c.getVrfs().containsKey(name)) {
      _w.redFlag(
          String.format(
              "VRF loopback interface '%s' can only be configured via 'net add vrf' family of commands; following is invalid: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (_c.getVxlans().containsKey(name)) {
      _w.redFlag(
          String.format(
              "VXLAN interface '%s' can only be configured via 'net add vxlan' family of commands; following is invalid: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (PHYSICAL_INTERFACE_PATTERN.matcher(name).matches()) {
      type = CumulusInterfaceType.PHYSICAL;
    } else if (subinterfaceMatcher.matches()) {
      superInterfaceName = subinterfaceMatcher.group(1);
      String vlanStr = subinterfaceMatcher.group(2);
      if (PHYSICAL_INTERFACE_PATTERN.matcher(superInterfaceName).matches()) {
        type = CumulusInterfaceType.PHYSICAL_SUBINTERFACE;
      } else if (_c.getBonds().containsKey(superInterfaceName)) {
        type = CumulusInterfaceType.BOND_SUBINTERFACE;
      } else {
        _w.redFlag(
            String.format(
                "Subinterface name '%s' is invalid since '%s' is neither a physical nor a bond interface in: %s",
                name, superInterfaceName, getFullText(ctx)));
        return null;
      }
      try {
        encapsulationVlan = Integer.parseInt(vlanStr, 10);
        checkArgument(1 <= encapsulationVlan && encapsulationVlan <= 4094);
      } catch (IllegalArgumentException e) {
        _w.redFlag(
            String.format(
                "Subinterface name '%s' is invalid since '%s' is not a valid VLAN number in: %s",
                name, vlanStr, getFullText(ctx)));
        return null;
      }
    } else {
      _w.redFlag(String.format("Interface name '%s' is invalid in: %s", name, getFullText(ctx)));
      return null;
    }
    return new Interface(name, type, superInterfaceName, encapsulationVlan);
  }

  /**
   * Returns a newly-created {@link Vrf} with given {@code name}, or {@code null} if {@code name} is
   * invalid.
   */
  private @Nullable Vrf createVrf(String name, ParserRuleContext ctx) {
    if (name.equals(CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME)
        || PHYSICAL_INTERFACE_PATTERN.matcher(name).matches()
        || SUBINTERFACE_PATTERN.matcher(name).matches()
        || VLAN_INTERFACE_PATTERN.matcher(name).matches()) {
      _w.redFlag(String.format("Invalid name '%s' for vrf in: %s", name, getFullText(ctx)));
      return null;
    }
    if (_c.getBonds().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for vrf clashes with existing bond interface in: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (_c.getInterfaces().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for vrf clashes with existing interface in: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (_c.getVxlans().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for vrf clashes with existing vxlan in: %s",
              name, getFullText(ctx)));
      return null;
    }
    return new Vrf(name);
  }

  /**
   * Returns a newly-created {@link Vxlan} with given {@code name}, or {@code null} if {@code name}
   * is invalid.
   */
  private @Nullable Vxlan createVxlan(String name, ParserRuleContext ctx) {
    if (name.equals(CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME)
        || PHYSICAL_INTERFACE_PATTERN.matcher(name).matches()
        || SUBINTERFACE_PATTERN.matcher(name).matches()
        || VLAN_INTERFACE_PATTERN.matcher(name).matches()) {
      _w.redFlag(String.format("Invalid name '%s' for vxlan in: %s", name, getFullText(ctx)));
      return null;
    }
    if (_c.getBonds().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for vxlan clashes with existing bond interface in: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (_c.getInterfaces().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for vxlan clashes with existing interface in: %s",
              name, getFullText(ctx)));
      return null;
    }
    if (_c.getVrfs().containsKey(name)) {
      _w.redFlag(
          String.format(
              "Invalid name '%s' for vxlan clashes with existing vrf in: %s",
              name, getFullText(ctx)));
      return null;
    }
    return new Vxlan(name);
  }

  @Override
  public void enterA_bgp(A_bgpContext ctx) {
    // Line is only valid if either:
    // 1. BGP process already exists
    // 2. This line is an autonomous-system assignment, which can create a BGP process.
    _currentBgpProcess = _c.getBgpProcess();
    if (_currentBgpProcess == null) {
      _currentBgpProcess = new BgpProcess();
      if (ctx.b_common() != null && ctx.b_common().b_autonomous_system() != null) {
        // Attach new BGP process to configuration
        _c.setBgpProcess(_currentBgpProcess);
      } else {
        // Do not attach new BGP process to configuration since since line is invalid
        _w.redFlag(
            String.format(
                "Must first create BGP process via 'net add bgp autonomous-system <number>' before: %s",
                getFullText(ctx)));
      }
    }
    _currentBgpVrf = _currentBgpProcess.getDefaultVrf();
  }

  @Override
  public void enterA_bond(A_bondContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    Bond bond = _c.getBonds().get(name);
    if (bond == null) {
      bond = createBond(name, ctx);
      if (bond == null) {
        bond = new Bond("dummy");
      } else {
        _c.defineStructure(CumulusStructureType.BOND, name, line);
        _c.referenceStructure(
            CumulusStructureType.BOND, name, CumulusStructureUsage.BOND_SELF_REFERENCE, line);
        _c.getBonds().put(name, bond);
      }
    }
    _currentBond = bond;
  }

  @Override
  public void enterA_interface(A_interfaceContext ctx) {
    Set<String> interfaceNames = toStrings(ctx.interfaces);
    _currentInterfaces =
        initInterfacesIfAbsent(interfaceNames, ctx, CumulusStructureUsage.INTERFACE_SELF_REFERENCE);
  }

  @Override
  public void enterA_loopback(A_loopbackContext ctx) {
    _c.getLoopback().setConfigured(true);
    _c.defineStructure(
        CumulusStructureType.LOOPBACK,
        CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME,
        ctx.getStart().getLine());
    _c.referenceStructure(
        CumulusStructureType.LOOPBACK,
        CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME,
        CumulusStructureUsage.LOOPBACK_SELF_REFERENCE,
        ctx.getStart().getLine());
  }

  @Override
  public void enterA_vlan(A_vlanContext ctx) {
    int line = ctx.getStart().getLine();
    if (ctx.suffix != null) {
      String name = String.format("vlan%d", toInteger(ctx.suffix));
      _currentVlan = initVlansIfAbsent(ImmutableSet.of(name), line).iterator().next();
      _c.referenceStructure(
          CumulusStructureType.VLAN, name, CumulusStructureUsage.VLAN_SELF_REFERENCE, line);
    } else {
      Set<String> names =
          LongSpace.of(toRangeSet(ctx.suffixes)).enumerate().stream()
              .map(suffix -> String.format("vlan%d", suffix))
              .collect(ImmutableSet.toImmutableSet());
      _currentVlans = initVlansIfAbsent(names, line);
      names.forEach(
          name ->
              _c.referenceStructure(
                  CumulusStructureType.VLAN,
                  name,
                  CumulusStructureUsage.VLAN_SELF_REFERENCE,
                  line));
    }
  }

  @Override
  public void enterA_vrf(A_vrfContext ctx) {
    Set<String> vrfNames = toStrings(ctx.names);
    _currentVrfs = initVrfsIfAbsent(vrfNames, ctx, CumulusStructureUsage.VRF_SELF_REFERENCE);
  }

  @Override
  public void enterA_vxlan(A_vxlanContext ctx) {
    Set<String> names = toStrings(ctx.names, MAX_VXLAN_ID);
    if (ctx.vx_vxlan() != null && ctx.vx_vxlan().vxv_id() != null) {
      // create them if necessary when setting id
      _currentVxlans = initVxlansIfAbsent(names, ctx);
    } else if (!_c.getVxlans().keySet().containsAll(names)) {
      _w.redFlag(
          String.format(
              "All referenced vxlan instances must be created via 'net add vxlan <name> vxlan id <id>' before line: %s",
              getFullText(ctx.getParent())));
      _currentVxlans = ImmutableList.of();
    } else {
      _currentVxlans =
          names.stream().map(_c.getVxlans()::get).collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public void enterB_ipv4_unicast(B_ipv4_unicastContext ctx) {
    if (_currentBgpVrf.getIpv4Unicast() == null) {
      _currentBgpVrf.setIpv4Unicast(new BgpIpv4UnicastAddressFamily());
    }
  }

  @Override
  public void enterBi4_neighbor(Bi4_neighborContext ctx) {
    _currentBgpNeighborName = ctx.name.getText();
    _currentBgpNeighbor = _currentBgpVrf.getNeighbors().get(_currentBgpNeighborName);

    assert _currentBgpNeighbor != null; // Ensure neighbor exists
    if (_currentBgpNeighbor.getIpv4UnicastAddressFamily() == null) {
      _currentBgpNeighbor.setIpv4UnicastAddressFamily(new BgpNeighborIpv4UnicastAddressFamily());
    }
  }

  @Override
  public void exitBi4_neighbor(Bi4_neighborContext ctx) {
    _currentBgpNeighborName = null;
    _currentBgpNeighbor = null;
  }

  @Override
  public void exitBi4n_activate(Bi4n_activateContext ctx) {
    assert _currentBgpNeighbor != null; // Ensure neighbor exists
    assert _currentBgpNeighbor.getIpv4UnicastAddressFamily() != null;
    _currentBgpNeighbor.getIpv4UnicastAddressFamily().setActivated(true);
  }

  @Override
  public void exitBi4n_route_reflector_client(Bi4n_route_reflector_clientContext ctx) {
    assert _currentBgpNeighbor != null; // Ensure neighbor exists
    BgpNeighborIpv4UnicastAddressFamily ipv4UnicastAddressFamily =
        _currentBgpNeighbor.getIpv4UnicastAddressFamily();
    assert ipv4UnicastAddressFamily != null;
    // The neighbor must have been explicitly activated for route-reflector-client to take effect
    // https://docs.cumulusnetworks.com/display/DOCS/Border+Gateway+Protocol+-+BGP#BorderGatewayProtocol-BGP-RouteReflectors
    if (Boolean.TRUE.equals(ipv4UnicastAddressFamily.getActivated())) {
      ipv4UnicastAddressFamily.setRouteReflectorClient(true);
    }
  }

  @Override
  public void enterB_l2vpn(B_l2vpnContext ctx) {
    if (_currentBgpVrf.getL2VpnEvpn() == null) {
      _currentBgpVrf.setL2VpnEvpn(new BgpL2vpnEvpnAddressFamily());
    }
  }

  @Override
  public void enterB_neighbor(B_neighborContext ctx) {
    _currentBgpNeighborName = ctx.name.getText();
  }

  @Override
  public void exitB_neighbor(B_neighborContext ctx) {
    _currentBgpNeighborName = null;
  }

  @Override
  public void enterB_vrf(B_vrfContext ctx) {
    String name = ctx.name.getText();
    if (initVrfsIfAbsent(ImmutableSet.of(name), ctx, CumulusStructureUsage.BGP_VRF).isEmpty()) {
      // VRF name is invalid. Set a dummy VRF so deeper parse tree node actions do not NPE.
      _currentBgpVrf = new BgpVrf("");
      return;
    }
    _currentBgpVrf = _currentBgpProcess.getVrfs().computeIfAbsent(name, BgpVrf::new);
  }

  @Override
  public void enterBn_interface(Bn_interfaceContext ctx) {
    assert _currentBgpNeighborName != null;
    if (!referenceAbstractInterfaces(
        ImmutableSet.of(_currentBgpNeighborName),
        ctx,
        CumulusStructureUsage.BGP_NEIGHBOR_INTERFACE)) {
      _w.redFlag(
          String.format(
              "Cannot create BGP neighbor for illegal abstract interface name '%s' in: %s",
              _currentBgpNeighborName, getFullText(ctx)));
      return;
    }

    assert _currentBgpVrf != null;
    _currentBgpNeighbor =
        _currentBgpVrf
            .getNeighbors()
            .computeIfAbsent(_currentBgpNeighborName, BgpInterfaceNeighbor::new);
  }

  @Override
  public void exitBn_interface(Bn_interfaceContext ctx) {
    _currentBgpNeighbor = null;
  }

  @Override
  public void enterBn_peer(Bn_peerContext ctx) {
    assert _currentBgpNeighborName != null;
    assert _currentBgpVrf != null;
    // TODO: only IP neighbors should be created here.
    //  Peer group neighbors must have already been declared.
    _currentBgpNeighbor =
        _currentBgpVrf
            .getNeighbors()
            .computeIfAbsent(_currentBgpNeighborName, BgpPeerGroupNeighbor::new);
  }

  @Override
  public void exitBn_peer(Bn_peerContext ctx) {
    _currentBgpNeighbor = null;
  }

  @Override
  public void enterCumulus_nclu_configuration(Cumulus_nclu_configurationContext ctx) {
    _c = new CumulusNcluConfiguration();
  }

  @Override
  public void enterFrr_vrf(Frr_vrfContext ctx) {
    _currentVrf =
        initVrfsIfAbsent(
                ImmutableSet.of(ctx.name.getText()), ctx, CumulusStructureUsage.VRF_SELF_REFERENCE)
            .iterator()
            .next();
  }

  @Override
  public void enterFrrv_ip_route(Frrv_ip_routeContext ctx) {
    _currentVrf.getStaticRoutes().add(new StaticRoute(toPrefix(ctx.network), toIp(ctx.nhip), null));
  }

  @Override
  public void enterR_route_map(R_route_mapContext ctx) {
    String name = ctx.name.getText();
    _c.defineStructure(CumulusStructureType.ROUTE_MAP, name, ctx.getStart().getLine());
    LineAction action = toLineAction(ctx.action);
    _currentRouteMapEntry =
        _c.getRouteMaps()
            .computeIfAbsent(name, RouteMap::new)
            .getEntries()
            .compute(
                toInteger(ctx.num),
                (num, existingEntry) ->
                    existingEntry != null && existingEntry.getAction().equals(action)
                        ? existingEntry
                        : new RouteMapEntry(num, action));
  }

  @Override
  public void exitA_bgp(A_bgpContext ctx) {
    _currentBgpProcess = null;
    _currentBgpVrf = null;
  }

  @Override
  public void exitA_bond(A_bondContext ctx) {
    _currentBond = null;
  }

  @Override
  public void exitA_dot1x(A_dot1xContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitA_hostname(A_hostnameContext ctx) {
    _c.setHostname(ctx.hostname.getText());
  }

  @Override
  public void exitA_interface(A_interfaceContext ctx) {
    _currentInterfaces = null;
  }

  @Override
  public void exitA_ptp(A_ptpContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitA_snmp_server(A_snmp_serverContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitA_time(A_timeContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitA_vlan(A_vlanContext ctx) {
    _currentVlan = null;
    _currentVlans = null;
  }

  @Override
  public void exitA_vrf(A_vrfContext ctx) {
    _currentVrfs = null;
  }

  @Override
  public void exitA_vxlan(A_vxlanContext ctx) {
    _currentVxlans = null;
  }

  @Override
  public void exitB_autonomous_system(B_autonomous_systemContext ctx) {
    _currentBgpVrf.setAutonomousSystem(toLong(ctx.as));
  }

  @Override
  public void exitB_router_id(B_router_idContext ctx) {
    _currentBgpVrf.setRouterId(toIp(ctx.id));
  }

  @Override
  public void exitBi4_network(Bi4_networkContext ctx) {
    _currentBgpVrf
        .getIpv4Unicast()
        .getNetworks()
        .computeIfAbsent(toPrefix(ctx.network), BgpNetwork::new);
  }

  @Override
  public void exitBi4_redistribute_connected(Bi4_redistribute_connectedContext ctx) {
    String routeMap = null;
    if (ctx.rm != null) {
      routeMap = ctx.rm.getText();
      _c.referenceStructure(
          CumulusStructureType.ROUTE_MAP,
          routeMap,
          CumulusStructureUsage.BGP_IPV4_UNICAST_REDISTRIBUTE_CONNECTED_ROUTE_MAP,
          ctx.getStart().getLine());
    }
    _currentBgpVrf
        .getIpv4Unicast()
        .getRedistributionPolicies()
        .put(
            CumulusRoutingProtocol.CONNECTED,
            new BgpRedistributionPolicy(CumulusRoutingProtocol.CONNECTED, routeMap));
  }

  @Override
  public void exitBi4_redistribute_static(Bi4_redistribute_staticContext ctx) {
    String routeMap = null;
    if (ctx.rm != null) {
      routeMap = ctx.rm.getText();
      _c.referenceStructure(
          CumulusStructureType.ROUTE_MAP,
          routeMap,
          CumulusStructureUsage.BGP_IPV4_UNICAST_REDISTRIBUTE_STATIC_ROUTE_MAP,
          ctx.getStart().getLine());
    }
    _currentBgpVrf
        .getIpv4Unicast()
        .getRedistributionPolicies()
        .put(
            CumulusRoutingProtocol.STATIC,
            new BgpRedistributionPolicy(CumulusRoutingProtocol.STATIC, routeMap));
  }

  @Override
  public void exitBle_advertise_all_vni(Ble_advertise_all_vniContext ctx) {
    _currentBgpVrf.getL2VpnEvpn().setAdvertiseAllVni(true);
  }

  @Override
  public void exitBle_advertise_default_gw(Ble_advertise_default_gwContext ctx) {
    _currentBgpVrf.getL2VpnEvpn().setAdvertiseDefaultGw(true);
  }

  @Override
  public void exitBle_advertise_ipv4_unicast(Ble_advertise_ipv4_unicastContext ctx) {
    if (_currentBgpVrf.getL2VpnEvpn().getAdvertiseIpv4Unicast() == null) {
      _currentBgpVrf.getL2VpnEvpn().setAdvertiseIpv4Unicast(new BgpL2VpnEvpnIpv4Unicast());
    }
  }

  @Override
  public void enterBle_neighbor(Ble_neighborContext ctx) {
    _currentBgpNeighborName = ctx.name.getText();
    _currentBgpNeighbor = _currentBgpVrf.getNeighbors().get(_currentBgpNeighborName);

    assert _currentBgpNeighbor != null; // Ensure neighbor exists
    if (_currentBgpNeighbor.getL2vpnEvpnAddressFamily() == null) {
      _currentBgpNeighbor.setL2vpnEvpnAddressFamily(new BgpNeighborL2vpnEvpnAddressFamily());
    }
  }

  @Override
  public void exitBle_neighbor(Ble_neighborContext ctx) {
    _currentBgpNeighborName = null;
    _currentBgpNeighbor = null;
  }

  @Override
  public void exitBlen_activate(Blen_activateContext ctx) {
    assert _currentBgpNeighbor != null; // Ensure neighbor exists
    assert _currentBgpNeighbor.getL2vpnEvpnAddressFamily() != null;
    _currentBgpNeighbor.getL2vpnEvpnAddressFamily().setActivated(true);
  }

  @Override
  public void exitBlen_route_reflector_client(Blen_route_reflector_clientContext ctx) {
    assert _currentBgpNeighbor != null; // Ensure neighbor exists
    BgpNeighborL2vpnEvpnAddressFamily l2vpnEvpnAddressFamily =
        _currentBgpNeighbor.getL2vpnEvpnAddressFamily();
    assert l2vpnEvpnAddressFamily != null;
    // The neighbor must have been explicitly activated for route-reflector-client to take effect
    // https://docs.cumulusnetworks.com/display/DOCS/Border+Gateway+Protocol+-+BGP#BorderGatewayProtocol-BGP-RouteReflectors
    if (Boolean.TRUE.equals(l2vpnEvpnAddressFamily.getActivated())) {
      l2vpnEvpnAddressFamily.setRouteReflectorClient(true);
    }
  }

  @Override
  public void exitBn_peer_group(Bn_peer_groupContext ctx) {
    assert _currentBgpNeighborName != null;
    assert _currentBgpVrf != null;
    _currentBgpNeighbor =
        _currentBgpVrf
            .getNeighbors()
            .computeIfAbsent(_currentBgpNeighborName, BgpPeerGroupNeighbor::new);
  }

  @Override
  public void exitBnp_description(Bnp_descriptionContext ctx) {
    assert _currentBgpNeighbor != null;
    _currentBgpNeighbor.setDescription(ctx.text.getText());
  }

  @Override
  public void exitBnp_peer_group(Bnp_peer_groupContext ctx) {
    assert _currentBgpNeighbor != null;
    _currentBgpNeighbor.setPeerGroup(ctx.name.getText());
  }

  @Override
  public void exitBnp_remote_as(Bnp_remote_asContext ctx) {
    assert _currentBgpNeighbor != null;
    if (ctx.EXTERNAL() != null) {
      _currentBgpNeighbor.setRemoteAsType(RemoteAsType.EXTERNAL);
      _currentBgpNeighbor.setRemoteAs(null);
    } else if (ctx.INTERNAL() != null) {
      _currentBgpNeighbor.setRemoteAsType(RemoteAsType.INTERNAL);
      _currentBgpNeighbor.setRemoteAs(null);
    } else {
      assert ctx.as != null;
      _currentBgpNeighbor.setRemoteAsType(RemoteAsType.EXPLICIT);
      _currentBgpNeighbor.setRemoteAs(toLong(ctx.as));
    }
  }

  @Override
  public void exitBob_access(Bob_accessContext ctx) {
    _currentBond.getBridge().setAccess(toInteger(ctx.vlan));
  }

  @Override
  public void exitBob_pvid(Bob_pvidContext ctx) {
    _currentBond.getBridge().setPvid(toInteger(ctx.id));
  }

  @Override
  public void exitBob_vids(Bob_vidsContext ctx) {
    _currentBond.getBridge().setVids(IntegerSpace.of(toRangeSet(ctx.vlans)));
  }

  @Override
  public void exitBobo_slaves(Bobo_slavesContext ctx) {
    Set<String> slaves = toStrings(ctx.slaves);
    _currentBond.setSlaves(
        initInterfacesIfAbsent(slaves, ctx, CumulusStructureUsage.BOND_SLAVE).isEmpty()
            ? ImmutableSet.of()
            : slaves);
  }

  @Override
  public void exitBond_clag_id(Bond_clag_idContext ctx) {
    _currentBond.setClagId(toInteger(ctx.id));
  }

  @Override
  public void exitBond_ip_address(Bond_ip_addressContext ctx) {
    _currentBond.getIpAddresses().add(toInterfaceAddress(ctx.address));
  }

  @Override
  public void exitBond_vrf(Bond_vrfContext ctx) {
    String vrf = ctx.name.getText();
    if (initVrfsIfAbsent(ImmutableSet.of(vrf), ctx, CumulusStructureUsage.BOND_VRF).isEmpty()) {
      return;
    }
    _currentBond.setVrf(vrf);
  }

  @Override
  public void exitBrbr_ports(Brbr_portsContext ctx) {
    Set<String> ports = toStrings(ctx.ports);
    List<String> subinterfaces =
        ports.stream()
            .filter(port -> SUBINTERFACE_PATTERN.matcher(port).matches())
            .collect(ImmutableList.toImmutableList());
    if (!subinterfaces.isEmpty()) {
      _w.redFlag(
          String.format(
              "Cannot add subinterfaces: %s to bridge in: %s", subinterfaces, getFullText(ctx)));
      return;
    }
    if (!referenceAbstractInterfaces(ports, ctx, CumulusStructureUsage.BRIDGE_PORT)) {
      _w.redFlag(
          String.format(
              "Cannot add illegal ports glob '%s' to bridge in: %s",
              ctx.ports.getText(), getFullText(ctx)));
      return;
    }
    _c.getBridge().setPorts(ports);
  }

  @Override
  public void exitBrbr_pvid(Brbr_pvidContext ctx) {
    _c.getBridge().setPvid(toInteger(ctx.pvid));
  }

  @Override
  public void exitBrbr_vids(Brbr_vidsContext ctx) {
    _c.getBridge().setVids(IntegerSpace.of(toRangeSetInt(ctx.ids)));
  }

  @Override
  public void exitBrbr_vlan_aware(Brbr_vlan_awareContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitDn4(Dn4Context ctx) {
    _c.getIpv4Nameservers().add(toIp(ctx.address));
  }

  @Override
  public void exitDn6(Dn6Context ctx) {
    _c.getIpv6Nameservers().add(toIp6(ctx.address6));
  }

  @Override
  public void exitFrr_unrecognized(Frr_unrecognizedContext ctx) {
    unrecognized(ctx);
  }

  @Override
  public void exitFrr_username(Frr_usernameContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitFrr_vrf(Frr_vrfContext ctx) {
    _currentVrf = null;
  }

  @Override
  public void exitFrr_exit_vrf(Frr_exit_vrfContext ctx) {
    _currentVrf = null;
  }

  @Override
  public void exitI_ip_address(I_ip_addressContext ctx) {
    ConcreteInterfaceAddress address = toInterfaceAddress(ctx.address);
    _currentInterfaces.forEach(iface -> iface.getIpAddresses().add(address));
  }

  @Override
  public void exitI_link_speed(I_link_speedContext ctx) {
    _currentInterfaces.forEach(iface -> iface.setSpeed(toInteger(ctx.speed)));
  }

  @Override
  public void exitI_vrf(I_vrfContext ctx) {
    String vrf = ctx.name.getText();
    if (initVrfsIfAbsent(ImmutableSet.of(vrf), ctx, CumulusStructureUsage.INTERFACE_VRF)
        .isEmpty()) {
      return;
    }
    _currentInterfaces.forEach(
        iface -> {
          iface.setVrf(vrf);
        });
  }

  @Override
  public void exitIb_access(Ib_accessContext ctx) {
    _currentInterfaces.forEach(iface -> iface.getBridge().setAccess(toInteger(ctx.vlan)));
  }

  @Override
  public void exitIb_pvid(Ib_pvidContext ctx) {
    _currentInterfaces.forEach(iface -> iface.getBridge().setPvid(toInteger(ctx.id)));
  }

  @Override
  public void exitIb_vids(Ib_vidsContext ctx) {
    _currentInterfaces.forEach(
        iface -> iface.getBridge().setVids(IntegerSpace.of(toRangeSet(ctx.vlans))));
  }

  @Override
  public void exitIc_backup_ip(Ic_backup_ipContext ctx) {
    Ip backupIp = toIp(ctx.backup_ip);
    String vrf;
    if (ctx.vrf != null) {
      vrf = ctx.vrf.getText();
      if (initVrfsIfAbsent(
              ImmutableSet.of(vrf), ctx, CumulusStructureUsage.INTERFACE_CLAG_BACKUP_IP_VRF)
          .isEmpty()) {
        return;
      }
    } else {
      vrf = null;
    }
    _currentInterfaces.forEach(
        iface -> {
          InterfaceClagSettings clag = iface.getOrInitClag();
          clag.setBackupIp(backupIp);
          clag.setBackupIpVrf(vrf);
        });
  }

  @Override
  public void exitIc_peer_ip(Ic_peer_ipContext ctx) {
    Ip peerIp = ctx.peer_ip != null ? toIp(ctx.peer_ip) : null;
    _currentInterfaces.forEach(
        iface -> {
          InterfaceClagSettings clag = iface.getOrInitClag();
          clag.setPeerIp(peerIp);
          clag.setPeerIpLinkLocal(ctx.LINKLOCAL() != null);
        });
  }

  @Override
  public void exitIc_priority(Ic_priorityContext ctx) {
    int priority = toInteger(ctx.priority);
    _currentInterfaces.forEach(
        iface -> {
          iface.getOrInitClag().setPriority(priority);
        });
  }

  @Override
  public void exitIc_sys_mac(Ic_sys_macContext ctx) {
    MacAddress macAddress = toMacAddress(ctx.mac);
    _currentInterfaces.forEach(
        iface -> {
          iface.getOrInitClag().setSysMac(macAddress);
        });
  }

  @Override
  public void exitL_ip_address(L_ip_addressContext ctx) {
    _c.getLoopback().getAddresses().add(toInterfaceAddress(ctx.address));
  }

  @Override
  public void exitLc_vxlan_anycast_ip(Lc_vxlan_anycast_ipContext ctx) {
    _c.getLoopback().setClagVxlanAnycastIp(toIp(ctx.ip));
  }

  @Override
  public void exitR_defaults_datacenter(R_defaults_datacenterContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitR_log(R_logContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitR_route(R_routeContext ctx) {
    _c.getStaticRoutes()
        .add(
            new StaticRoute(
                toPrefix(ctx.prefix),
                ctx.nhip != null ? toIp(ctx.nhip) : null,
                ctx.iface != null ? ctx.iface.getText() : null));
  }

  @Override
  public void exitR_route_map(R_route_mapContext ctx) {
    _currentRouteMapEntry = null;
  }

  @Override
  public void exitR_service_integrated_vtysh_config(R_service_integrated_vtysh_configContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRmm_interface(Rmm_interfaceContext ctx) {
    Set<String> names = toStrings(ctx.interfaces);
    if (!referenceAbstractInterfaces(names, ctx, CumulusStructureUsage.ROUTE_MAP_MATCH_INTERFACE)) {
      _w.redFlag(
          String.format(
              "Cannot match illegal interface glob '%s' in: %s",
              ctx.interfaces.getText(), getFullText(ctx)));
      return;
    }
    _currentRouteMapEntry.setMatchInterface(new RouteMapMatchInterface(names));
  }

  @Override
  public void exitS_net_add_unrecognized(S_net_add_unrecognizedContext ctx) {
    unrecognized(ctx);
  }

  @Override
  public void exitV_ip_address(V_ip_addressContext ctx) {
    InterfaceAddress address = toInterfaceAddress(ctx.address);
    _currentVlans.forEach(vlan -> vlan.getAddresses().add(address));
  }

  @Override
  public void exitV_ip_address_virtual(V_ip_address_virtualContext ctx) {
    MacAddress macAddress = toMacAddress(ctx.mac);
    InterfaceAddress address = toInterfaceAddress(ctx.address);
    _currentVlans.forEach(
        vlan ->
            vlan.getAddressVirtuals()
                .computeIfAbsent(macAddress, m -> new HashSet<>())
                .add(address));
  }

  @Override
  public void exitV_vlan_id(V_vlan_idContext ctx) {
    _currentVlan.setVlanId(toInteger(ctx.id));
  }

  @Override
  public void exitV_vlan_raw_device(V_vlan_raw_deviceContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitV_vrf(V_vrfContext ctx) {
    String name = ctx.name.getText();
    if (initVrfsIfAbsent(ImmutableSet.of(name), ctx, CumulusStructureUsage.VLAN_VRF).isEmpty()) {
      // exit if VRF is invalid
      return;
    }
    _currentVlans.forEach(vlan -> vlan.setVrf(name));
  }

  @Override
  public void exitVrf_ip_address(Vrf_ip_addressContext ctx) {
    ConcreteInterfaceAddress address = toInterfaceAddress(ctx.address);
    _currentVrfs.forEach(vrf -> vrf.getAddresses().add(address));
  }

  @Override
  public void exitVrf_vni(Vrf_vniContext ctx) {
    int vni = toInteger(ctx.vni);
    _currentVrfs.forEach(vrf -> vrf.setVni(vni));
  }

  @Override
  public void exitVx_stp(Vx_stpContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitVxb_access(Vxb_accessContext ctx) {
    int vlan = toInteger(ctx.vlan);
    _currentVxlans.forEach(vxlan -> vxlan.setBridgeAccessVlan(vlan));
  }

  @Override
  public void exitVxb_arp_nd_suppress(Vxb_arp_nd_suppressContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitVxb_learning(Vxb_learningContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitVxv_id(Vxv_idContext ctx) {
    int id = toInteger(ctx.vni);
    _currentVxlans.forEach(vxlan -> vxlan.setId(id));
  }

  @Override
  public void exitVxv_local_tunnelip(Vxv_local_tunnelipContext ctx) {
    Ip localTunnelip = toIp(ctx.ip);
    _currentVxlans.forEach(vxlan -> vxlan.setLocalTunnelip(localTunnelip));
  }

  /**
   * Returns built {@link CumulusNcluConfiguration}.
   *
   * @throws IllegalStateException if called before walking parse tree produced by {@link
   *     CumulusNcluCombinedParser#parse}
   */
  public @Nonnull CumulusNcluConfiguration getConfiguration() {
    checkState(
        _c != null,
        "Cannot return vendor configuration before walking valid Cumulus NCLU parse tree");
    return _c;
  }

  private @Nonnull String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    String text = _text.substring(start, end + 1);
    return text;
  }

  /**
   * Returns already-present or newly-created {@link Interface}s with given {@code names}, or an
   * empty {@link List} if any name in {@code names} is invalid.
   */
  private @Nonnull List<Interface> initInterfacesIfAbsent(
      Set<String> names, ParserRuleContext ctx, @Nonnull CumulusStructureUsage usage) {
    ImmutableList.Builder<Interface> interfacesBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> newInterfaces = ImmutableList.builder();
    for (String name : names) {
      Interface iface = _c.getInterfaces().get(name);
      if (iface == null) {
        iface = createInterface(name, ctx);
        if (iface == null) {
          return ImmutableList.of();
        }
        newInterfaces.add(name);
      }
      interfacesBuilder.add(iface);
    }
    List<Interface> interfaces = interfacesBuilder.build();
    interfaces.forEach(iface -> _c.getInterfaces().computeIfAbsent(iface.getName(), n -> iface));
    int line = ctx.getStart().getLine();
    newInterfaces
        .build()
        .forEach(
            name -> {
              _c.defineStructure(CumulusStructureType.INTERFACE, name, line);
            });
    names.forEach(name -> _c.referenceStructure(CumulusStructureType.INTERFACE, name, usage, line));
    return interfaces;
  }

  /** Returns already-present or newly-created {@link Vlan}s with given {@code names}. */
  private @Nonnull List<Vlan> initVlansIfAbsent(Set<String> names, int line) {
    return names.stream()
        .map(
            name ->
                _c.getVlans()
                    .computeIfAbsent(
                        name,
                        n -> {
                          _c.defineStructure(CumulusStructureType.VLAN, n, line);
                          return new Vlan(n);
                        }))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns already-present or newly-created {@link Vrf}s with given {@code names}, or an empty
   * {@link List} if any name in {@code names} is invalid.
   */
  private @Nonnull List<Vrf> initVrfsIfAbsent(
      Set<String> names, ParserRuleContext ctx, @Nonnull CumulusStructureUsage usage) {
    ImmutableList.Builder<Vrf> vrfsBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> newVrfs = ImmutableList.builder();
    for (String name : names) {
      Vrf vrf = _c.getVrfs().get(name);
      if (vrf == null) {
        vrf = createVrf(name, ctx);
        if (vrf == null) {
          return ImmutableList.of();
        }
        newVrfs.add(name);
      }
      vrfsBuilder.add(vrf);
    }
    List<Vrf> vrfs = vrfsBuilder.build();
    vrfs.forEach(vrf -> _c.getVrfs().computeIfAbsent(vrf.getName(), n -> vrf));
    int line = ctx.getStart().getLine();
    newVrfs
        .build()
        .forEach(
            name -> {
              _c.defineStructure(CumulusStructureType.VRF, name, line);
            });
    names.forEach(name -> _c.referenceStructure(CumulusStructureType.VRF, name, usage, line));
    return vrfs;
  }

  /**
   * Returns already-present or newly-created {@link Vxlan}s with given {@code names} if all {@code
   * names}. are valid. Returns an empty {@link List} without any side-effects if any name is
   * invalid, since in that case the whole line would be rejected.
   */
  private @Nonnull List<Vxlan> initVxlansIfAbsent(Set<String> names, ParserRuleContext ctx) {
    ImmutableList.Builder<Vxlan> vxlansBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> newVxlans = ImmutableList.builder();
    for (String name : names) {
      Vxlan vxlan = _c.getVxlans().get(name);
      if (vxlan == null) {
        vxlan = createVxlan(name, ctx);
        if (vxlan == null) {
          return ImmutableList.of();
        }
        newVxlans.add(name);
      }
      vxlansBuilder.add(vxlan);
    }
    List<Vxlan> vxlans = vxlansBuilder.build();
    vxlans.forEach(vxlan -> _c.getVxlans().computeIfAbsent(vxlan.getName(), n -> vxlan));
    int line = ctx.getStart().getLine();
    newVxlans
        .build()
        .forEach(
            name -> {
              _c.defineStructure(CumulusStructureType.VXLAN, name, line);
              _c.referenceStructure(
                  CumulusStructureType.VXLAN,
                  name,
                  CumulusStructureUsage.VXLAN_SELF_REFERENCE,
                  line);
            });
    return vxlans;
  }

  /**
   * Attempt to add references to named abstract interfaces of type 'bond', 'interface', 'loopback'
   * 'vlan', 'vrf', 'vxlan'. Any non-existent interface will be created if its name is valid for an
   * 'interface'-type interface.
   *
   * @return {@code false} iff some name in {@code names} does not correspond to an existing
   *     abstract interface nor is a valid name for an 'interface'-type interface.
   */
  private boolean referenceAbstractInterfaces(
      Set<String> names, ParserRuleContext ctx, CumulusStructureUsage usage) {
    Set<String> potentialInterfaceNames =
        names.stream()
            .filter(not(_c.getBonds()::containsKey))
            .filter(not(_c.getVlans()::containsKey))
            .filter(not(_c.getVrfs()::containsKey))
            .filter(not(_c.getVxlans()::containsKey))
            .filter(not("lo"::equals))
            .collect(ImmutableSet.toImmutableSet());
    if (!potentialInterfaceNames.isEmpty()
        && initInterfacesIfAbsent(potentialInterfaceNames, ctx, usage).isEmpty()) {
      return false;
    }
    int line = ctx.getStart().getLine();
    if (potentialInterfaceNames.contains(CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME)
        && !_c.getLoopback().getConfigured()) {
      _c.defineStructure(
          CumulusStructureType.LOOPBACK, CumulusNcluConfiguration.LOOPBACK_INTERFACE_NAME, line);
    }
    names.forEach(
        name -> _c.referenceStructure(CumulusStructureType.ABSTRACT_INTERFACE, name, usage, line));
    return true;
  }

  @SuppressWarnings("unused")
  private void todo(ParserRuleContext ctx) {
    _w.todo(ctx, getFullText(ctx), _parser);
  }

  private void unrecognized(ParserRuleContext ctx) {
    ParseWarning warning =
        new ParseWarning(
            ctx.getStart().getLine(),
            getFullText(ctx),
            ctx.toString(Arrays.asList(_parser.getParser().getRuleNames())),
            "This syntax is unrecognized");
    unrecognized(warning, ctx);
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();

    if (token instanceof UnrecognizedLineToken) {
      UnrecognizedLineToken unrecToken = (UnrecognizedLineToken) token;
      ParseWarning warning =
          new ParseWarning(
              line, lineText, unrecToken.getParserContext(), "This syntax is unrecognized");
      unrecognized(warning, null);
    } else {
      String msg = String.format("Unrecognized Line: %d: %s", line, lineText);
      _w.redFlag(msg + " Subsequent lines may not be processed correctly");
    }
  }

  private void unrecognized(ParseWarning warning, @Nullable ParserRuleContext ctx) {
    // for testing
    if (_parser.getSettings().getDisableUnrecognized()) {
      try {
        String warningStr = BatfishObjectMapper.writePrettyString(warning);
        String parseTreeStr =
            ctx != null
                ? ParseTreePrettyPrinter.print(
                    ctx, _parser, _parser.getSettings().getPrintParseTreeLineNums())
                : "";
        throw new BatfishException(
            String.format(
                "Forcing failure on unrecognized line: %s\n%s", warningStr, parseTreeStr));
      } catch (JsonProcessingException e) {
        throw new BatfishException("Failure describing unrecognized line", e);
      }
    }

    _w.getParseWarnings().add(warning);
    _c.setUnrecognized(true);
  }
}
