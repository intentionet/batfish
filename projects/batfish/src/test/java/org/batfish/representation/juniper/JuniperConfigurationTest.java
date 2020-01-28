package org.batfish.representation.juniper;

import static org.batfish.common.Warnings.TAG_PEDANTIC;
import static org.batfish.common.Warnings.TAG_UNIMPLEMENTED;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.matchers.AclLineMatchers.isExprAclLineThat;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.hasConjuncts;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.isAndMatchExprThat;
import static org.batfish.datamodel.matchers.ExprAclLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasSrcIps;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.hasHeaderSpace;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.isMatchHeaderSpaceThat;
import static org.batfish.representation.juniper.JuniperConfiguration.DEFAULT_DEAD_INTERVAL;
import static org.batfish.representation.juniper.JuniperConfiguration.DEFAULT_HELLO_INTERVAL;
import static org.batfish.representation.juniper.JuniperConfiguration.DEFAULT_ISIS_COST;
import static org.batfish.representation.juniper.JuniperConfiguration.DEFAULT_NBMA_DEAD_INTERVAL;
import static org.batfish.representation.juniper.JuniperConfiguration.DEFAULT_NBMA_HELLO_INTERVAL;
import static org.batfish.representation.juniper.JuniperConfiguration.MAX_ISIS_COST_WITHOUT_WIDE_METRICS;
import static org.batfish.representation.juniper.JuniperConfiguration.OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER;
import static org.batfish.representation.juniper.JuniperConfiguration.buildScreen;
import static org.batfish.representation.juniper.JuniperConfiguration.mergeIpAccessListLines;
import static org.batfish.representation.juniper.JuniperConfiguration.toOspfDeadInterval;
import static org.batfish.representation.juniper.JuniperConfiguration.toOspfHelloInterval;
import static org.batfish.representation.juniper.JuniperConfiguration.toRibId;
import static org.batfish.representation.juniper.JuniperStructureType.FIREWALL_FILTER_TERM;
import static org.batfish.representation.juniper.NatPacketLocation.interfaceLocation;
import static org.batfish.representation.juniper.NatPacketLocation.routingInstanceLocation;
import static org.batfish.representation.juniper.NatPacketLocation.zoneLocation;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.dataplane.rib.RibId;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.representation.juniper.Interface.OspfInterfaceType;
import org.batfish.vendor.VendorStructureId;
import org.junit.Test;

public class JuniperConfigurationTest {

  private static JuniperConfiguration createConfig() {
    JuniperConfiguration config = new JuniperConfiguration();
    config.setFilename("file");
    config._c = new Configuration("host", ConfigurationFormat.JUNIPER);
    return config;
  }

  @Test
  public void testToIpAccessList() {
    JuniperConfiguration config = createConfig();
    ConcreteFirewallFilter filter = new ConcreteFirewallFilter("filter", Family.INET);
    IpAccessList emptyAcl = config.toIpAccessList(filter);

    FwTerm term = new FwTerm("term");
    String ipAddrPrefix = "1.2.3.0/24";
    term.getFroms().add(new FwFromSourceAddress(IpWildcard.parse(ipAddrPrefix)));
    term.getThens().add(FwThenAccept.INSTANCE);
    filter.getTerms().put("term", term);
    IpAccessList headerSpaceAcl = config.toIpAccessList(filter);

    Zone zone = new Zone("zone", new AddressBook("global", null));
    String interface1Name = "interface1";
    zone.getInterfaces().add(interface1Name);
    String interface2Name = "interface2";
    zone.getInterfaces().add(interface2Name);
    config.getMasterLogicalSystem().getZones().put("zone", zone);
    filter.setFromZone("zone");
    IpAccessList headerSpaceAndSrcInterfaceAcl = config.toIpAccessList(filter);

    // ACL from empty filter should have no lines
    assertThat(emptyAcl.getLines(), iterableWithSize(0));

    // ACL from headerSpace filter should have one line
    AclLine headerSpaceAclLine = Iterables.getOnlyElement(headerSpaceAcl.getLines());
    // It should have a MatchHeaderSpace match condition, matching the ipAddrPrefix from above
    ImmutableList.of("1.2.3.0", "1.2.3.255").stream()
        .map(Ip::parse)
        .forEach(
            ip ->
                assertThat(
                    headerSpaceAclLine,
                    isExprAclLineThat(
                        hasMatchCondition(
                            isMatchHeaderSpaceThat(hasHeaderSpace(hasSrcIps(containsIp(ip))))))));

    // ACL from headerSpace and zone filter should have one line
    AclLine comboAclLine = Iterables.getOnlyElement(headerSpaceAndSrcInterfaceAcl.getLines());
    // It should have an AndMatchExpr match condition, containing both a MatchSrcInterface
    // condition and a MatchHeaderSpace condition
    assertThat(
        comboAclLine,
        isExprAclLineThat(
            hasMatchCondition(
                isAndMatchExprThat(
                    hasConjuncts(
                        containsInAnyOrder(
                            new MatchSrcInterface(ImmutableList.of(interface1Name, interface2Name)),
                            new MatchHeaderSpace(
                                HeaderSpace.builder()
                                    .setSrcIps(IpWildcard.parse(ipAddrPrefix).toIpSpace())
                                    .build())))))));
  }

  /**
   * Sets up the given configuration with one VRF called vrf containing one interface called iface.
   *
   * @return the created interface
   */
  private static org.batfish.datamodel.Interface createInterface(Configuration c) {
    Vrf vrf = new Vrf("vrf");
    c.setVrfs(ImmutableMap.of("vrf", vrf));
    org.batfish.datamodel.Interface iface =
        org.batfish.datamodel.Interface.builder().setName("iface").setOwner(c).setVrf(vrf).build();
    return iface;
  }

  /**
   * Creates a {@link RoutingInstance} configured with one VRF called vrf containing one enabled
   * Juniper {@link Interface} called iface with the given {@code ifaceBandwidth}. If {@code
   * configuredIfaceIsisMetric} is not null, interface's level 1 IS-IS settings will use it. If
   * {@code referenceBandwidth} is not null, routing instance's IS-IS settings will use it.
   *
   * @param ifaceBandwidth Bandwidth of interface
   * @param configuredIfaceIsisMetric IS-IS metric for the interface's level 1 IS-IS settings
   * @param referenceBandwidth Reference bandwidth for the routing instance's {@link IsisSettings}
   */
  private static RoutingInstance createRoutingInstance(
      @Nonnull Double ifaceBandwidth,
      @Nullable Long configuredIfaceIsisMetric,
      @Nullable Double referenceBandwidth) {
    Interface iface = new Interface("iface");
    iface.setBandwidth(ifaceBandwidth);
    iface.getOrInitIsisSettings();
    if (configuredIfaceIsisMetric != null) {
      iface.getIsisSettings().getLevel1Settings().setMetric(configuredIfaceIsisMetric);
    }

    RoutingInstance routingInstance = new RoutingInstance("vrf");
    routingInstance.getInterfaces().put("iface", iface);
    if (referenceBandwidth != null) {
      routingInstance.getIsisSettings().setReferenceBandwidth(referenceBandwidth);
    }

    return routingInstance;
  }

  @Test
  public void testProcessIsisInterfaceSettings() {
    // Config with loopback and two other interfaces
    JuniperConfiguration config = createConfig();
    String loopbackName = "lo0.0";
    String iface1Name = "iface1";
    String iface2Name = "iface2";
    Vrf vrf = new Vrf("vrf");
    org.batfish.datamodel.Interface.builder()
        .setOwner(config._c)
        .setName(loopbackName)
        .setVrf(vrf)
        .setType(InterfaceType.LOOPBACK)
        .build();
    org.batfish.datamodel.Interface.builder()
        .setName(iface1Name)
        .setOwner(config._c)
        .setVrf(vrf)
        .build();
    org.batfish.datamodel.Interface.builder()
        .setName(iface2Name)
        .setOwner(config._c)
        .setVrf(vrf)
        .build();
    config._c.setVrfs(ImmutableMap.of("vrf", vrf));

    // Loopback has IS-IS enabled at both levels; other interfaces' IS-IS settings vary by test
    RoutingInstance routingInstance = new RoutingInstance("vrf");
    Interface vsLoopback = new Interface(loopbackName);
    vsLoopback.getOrInitIsisSettings();
    routingInstance.getInterfaces().put(loopbackName, vsLoopback);

    {
      // LEVEL_1_2 if both enabled in process and each enabled on at least one interface
      Interface vsIface1 = new Interface(iface1Name);
      Interface vsIface2 = new Interface(iface2Name);
      vsIface1.getOrInitIsisSettings().getLevel1Settings().setEnabled(false);
      vsIface2.getOrInitIsisSettings().getLevel2Settings().setEnabled(false);
      routingInstance.getInterfaces().put(iface1Name, vsIface1);
      routingInstance.getInterfaces().put(iface2Name, vsIface2);
      assertThat(
          config.processIsisInterfaceSettings(routingInstance, true, true),
          equalTo(IsisLevel.LEVEL_1_2));

      // LEVEL_1 if process doesn't have L2 enabled; LEVEL_2 if process doesn't have L1 enabled
      assertThat(
          config.processIsisInterfaceSettings(routingInstance, true, false),
          equalTo(IsisLevel.LEVEL_1));
      assertThat(
          config.processIsisInterfaceSettings(routingInstance, false, true),
          equalTo(IsisLevel.LEVEL_2));
    }

    {
      // LEVEL_1 if both enabled in process but only L1 enabled on non-loopback interfaces
      Interface vsIface1 = new Interface(iface1Name);
      Interface vsIface2 = new Interface(iface2Name);
      vsIface1.getOrInitIsisSettings().getLevel2Settings().setEnabled(false);
      routingInstance.getInterfaces().put(iface1Name, vsIface1);
      routingInstance.getInterfaces().put(iface2Name, vsIface2);
      assertThat(
          config.processIsisInterfaceSettings(routingInstance, true, true),
          equalTo(IsisLevel.LEVEL_1));
    }

    {
      // LEVEL_2 if both enabled in process but only L2 enabled on non-loopback interfaces
      Interface vsIface1 = new Interface(iface1Name);
      Interface vsIface2 = new Interface(iface2Name);
      vsIface1.getOrInitIsisSettings().getLevel1Settings().setEnabled(false);
      routingInstance.getInterfaces().put(iface1Name, vsIface1);
      routingInstance.getInterfaces().put(iface2Name, vsIface2);
      assertThat(
          config.processIsisInterfaceSettings(routingInstance, true, true),
          equalTo(IsisLevel.LEVEL_2));
    }
  }

  @Test
  public void testIsisCostFromInterfaceSettings() {
    // Reference bandwidth and interface IS-IS cost are both set. Test that interface IS-IS cost
    // takes precedence.
    JuniperConfiguration config = createConfig();
    org.batfish.datamodel.Interface viIface = createInterface(config._c);
    RoutingInstance routingInstance = createRoutingInstance(100D, 5L, 10000D);

    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(viIface.getIsis().getLevel1().getCost(), equalTo(5L));
  }

  @Test
  public void testIsisCostFromReferenceBandwidth() {
    // Reference bandwidth is set and interface IS-IS cost is not. Test that reference bandwidth is
    // used to produce IS-IS cost.
    JuniperConfiguration config = createConfig();
    org.batfish.datamodel.Interface viIface = createInterface(config._c);
    RoutingInstance routingInstance = createRoutingInstance(500D, null, 10000D);

    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(viIface.getIsis().getLevel1().getCost(), equalTo(20L));
  }

  @Test
  public void testIsisCostFromSmallReferenceBandwidth() {
    // Interface IS-IS cost is not set. Reference bandwidth is set smaller than interface bandwidth.
    // IS-IS cost should be 1.
    JuniperConfiguration config = createConfig();
    org.batfish.datamodel.Interface viIface = createInterface(config._c);
    RoutingInstance routingInstance = createRoutingInstance(100D, null, 10D);

    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(viIface.getIsis().getLevel1().getCost(), equalTo(1L));
  }

  @Test
  public void testDefaultIsisCost() {
    // Neither reference bandwidth nor interface IS-IS cost is set. Test that IS-IS cost is default.
    JuniperConfiguration config = createConfig();
    org.batfish.datamodel.Interface viIface = createInterface(config._c);
    RoutingInstance routingInstance = createRoutingInstance(100D, null, null);

    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(viIface.getIsis().getLevel1().getCost(), equalTo((long) DEFAULT_ISIS_COST));
  }

  @Test
  public void testIsisCostInterfaceBandwidth0() {
    // Reference bandwidth is set, but IS-IS cost can't be calculated because interface bandwidth is
    // 0. IS-IS cost should be default value; conversion warning should be generated.
    JuniperConfiguration config = createConfig();
    org.batfish.datamodel.Interface viIface = createInterface(config._c);
    RoutingInstance routingInstance = createRoutingInstance(0D, null, 10000D);
    config.setWarnings(new Warnings(true, false, false));

    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(viIface.getIsis().getLevel1().getCost(), equalTo((long) DEFAULT_ISIS_COST));
    assertThat(
        config.getWarnings().getPedanticWarnings(),
        contains(
            new Warning(
                "Cannot use IS-IS reference bandwidth for interface 'iface' because interface bandwidth is 0.",
                TAG_PEDANTIC)));
  }

  @Test
  public void testRefBandwidthBasedIsisCostClipped() {
    // Reference bandwidth = 10000, bandwidth = 10. IS-IS cost would be 1000 if wide-metrics-only
    // were set, but since it is not, cost should be clipped to 63.
    JuniperConfiguration config = createConfig();
    org.batfish.datamodel.Interface viIface = createInterface(config._c);
    RoutingInstance routingInstance = createRoutingInstance(10D, null, 10000D);

    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(
        viIface.getIsis().getLevel1().getCost(),
        equalTo((long) MAX_ISIS_COST_WITHOUT_WIDE_METRICS));

    // Now ensure that when wide-metrics-only is set, IS-IS cost doesn't get clipped.
    routingInstance.getIsisSettings().getLevel1Settings().setWideMetricsOnly(true);
    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(viIface.getIsis().getLevel1().getCost(), equalTo(1000L));
  }

  @Test
  public void testInterfaceSettingsBasedIsisCostClipped() {
    // Interface IS-IS metric = 100, but wide-metrics-only is not set. Cost should be clipped to 63.
    JuniperConfiguration config = createConfig();
    org.batfish.datamodel.Interface viIface = createInterface(config._c);
    RoutingInstance routingInstance = createRoutingInstance(10D, 100L, null);

    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(
        viIface.getIsis().getLevel1().getCost(),
        equalTo((long) MAX_ISIS_COST_WITHOUT_WIDE_METRICS));

    // Now ensure that when wide-metrics-only is set, IS-IS cost doesn't get clipped.
    routingInstance.getIsisSettings().getLevel1Settings().setWideMetricsOnly(true);
    config.processIsisInterfaceSettings(routingInstance, true, false);
    assertThat(viIface.getIsis().getLevel1().getCost(), equalTo(100L));
  }

  @Test
  public void testFromNatPacketLocationMatchExprs() {
    JuniperConfiguration config = createConfig();

    Interface parent = new Interface("parent");

    String interface1Name = "interface1";
    parent.getUnits().put(interface1Name, new Interface(interface1Name));
    config.getMasterLogicalSystem().getInterfaces().put(parent.getName(), parent);

    Zone zone = new Zone("zone", new AddressBook("global", null));
    String zoneInterface1Name = "zoneInterface1";
    zone.getInterfaces().add(zoneInterface1Name);
    String zoneInterface2Name = "zoneInterface2";
    zone.getInterfaces().add(zoneInterface2Name);
    config.getMasterLogicalSystem().getZones().put(zone.getName(), zone);

    RoutingInstance routingInstance = config.getMasterLogicalSystem().getDefaultRoutingInstance();
    String routingInstanceInterface1Name = "routingInstanceInterface1";
    routingInstance
        .getInterfaces()
        .put(routingInstanceInterface1Name, new Interface(routingInstanceInterface1Name));
    String routingInstanceInterface2Name = "routingInstanceInterface2";
    routingInstance
        .getInterfaces()
        .put(routingInstanceInterface2Name, new Interface(routingInstanceInterface2Name));

    Map<NatPacketLocation, AclLineMatchExpr> matchExprs = config.fromNatPacketLocationMatchExprs();
    assertThat(
        matchExprs,
        equalTo(
            ImmutableMap.of(
                interfaceLocation(interface1Name), matchSrcInterface(interface1Name),
                zoneLocation(zone.getName()),
                    matchSrcInterface(zoneInterface1Name, zoneInterface2Name),
                routingInstanceLocation(routingInstance.getName()),
                    matchSrcInterface(
                        routingInstanceInterface1Name, routingInstanceInterface2Name))));
  }

  @Test
  public void testBuildScreen() {
    List<ScreenOption> screenOptionList =
        ImmutableList.of(
            IcmpLarge.INSTANCE,
            IpUnknownProtocol.INSTANCE,
            TcpFinNoAck.INSTANCE,
            TcpSynFin.INSTANCE,
            TcpNoFlag.INSTANCE);

    Screen screen = new Screen("screen");
    for (ScreenOption option : screenOptionList) {
      screen.getScreenOptions().add(option);
    }

    IpAccessList screenAcl = buildScreen(screen, "~SCREEN~screen");

    assertThat(
        screenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN~screen")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.rejecting(
                            new OrMatchExpr(
                                screenOptionList.stream()
                                    .map(ScreenOption::getAclLineMatchExpr)
                                    .collect(Collectors.toList()))),
                        ExprAclLine.ACCEPT_ALL))
                .build()));

    Screen screen2 = new Screen("screen2");
    IpAccessList screenAcl2 = buildScreen(screen2, "~SCREEN~screen2");
    assertThat(screenAcl2, nullValue());
  }

  @Test
  public void testBuildScreensPerZone() {
    JuniperConfiguration config = createConfig();

    List<ScreenOption> screenOptionList1 =
        ImmutableList.of(IcmpLarge.INSTANCE, IpUnknownProtocol.INSTANCE, TcpFinNoAck.INSTANCE);

    List<ScreenOption> screenOptionList2 = ImmutableList.of(TcpSynFin.INSTANCE, TcpNoFlag.INSTANCE);

    Screen screen1 = new Screen("screen1");
    for (ScreenOption option : screenOptionList1) {
      screen1.getScreenOptions().add(option);
    }

    Screen screen2 = new Screen("screen2");
    for (ScreenOption option : screenOptionList2) {
      screen2.getScreenOptions().add(option);
    }
    screen2.setAction(ScreenAction.ALARM_WITHOUT_DROP);

    Screen screen3 = new Screen("screen3");

    config.getMasterLogicalSystem().getScreens().put("screen1", screen1);
    config.getMasterLogicalSystem().getScreens().put("screen2", screen2);
    config.getMasterLogicalSystem().getScreens().put("screen3", screen3);

    Zone zone = new Zone("zone", null);
    zone.getScreens().add("screen1");
    zone.getScreens().add("screen2");
    zone.getScreens().add("screen3");

    IpAccessList screenAcl = config.buildScreensPerZone(zone, "~SCREEN_ZONE~zone");

    // screenAcl should only have screen1, because screen2 has an alarm action and screen3 has not
    // options.
    assertThat(
        screenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN_ZONE~zone")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.accepting(
                            new AndMatchExpr(
                                ImmutableList.of(new PermittedByAcl("~SCREEN~screen1"))))))
                .build()));
    assertThat(config._c.getIpAccessLists().get("~SCREEN~screen1"), notNullValue());
    assertThat(config._c.getIpAccessLists().get("~SCREEN~screen2"), nullValue());
    assertThat(config._c.getIpAccessLists().get("~SCREEN~screen3"), nullValue());
    assertThat(
        config._c.getIpAccessLists().get("~SCREEN~screen1"),
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN~screen1")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.rejecting(
                            new OrMatchExpr(
                                screenOptionList1.stream()
                                    .map(ScreenOption::getAclLineMatchExpr)
                                    .collect(Collectors.toList()))),
                        ExprAclLine.ACCEPT_ALL))
                .build()));
  }

  @Test
  public void testBuildScreensPerInterface() {
    JuniperConfiguration config = createConfig();
    Interface iface = new Interface("iface");

    List<ScreenOption> screenOptionList =
        ImmutableList.of(IcmpLarge.INSTANCE, IpUnknownProtocol.INSTANCE, TcpFinNoAck.INSTANCE);

    Screen screen = new Screen("screen");
    for (ScreenOption option : screenOptionList) {
      screen.getScreenOptions().add(option);
    }

    config.getMasterLogicalSystem().getScreens().put("screen", screen);

    Zone zone = new Zone("zone", null);
    zone.getScreens().add("screen");

    config.getMasterLogicalSystem().getInterfaceZones().put(iface.getName(), zone);

    IpAccessList screenAcl = config.buildScreensPerInterface(iface);

    // screenAcl should only have screen, because screen2 has an alarm action and screen3 has not
    // options.
    assertThat(
        screenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN_INTERFACE~iface")
                .setLines(
                    ImmutableList.of(
                        ExprAclLine.accepting(new PermittedByAcl("~SCREEN_ZONE~zone"))))
                .build()));
    assertThat(config._c.getIpAccessLists().get("~SCREEN_ZONE~zone"), notNullValue());
  }

  @Test
  public void testToRibId() {
    String hostname = "host";
    assertThat(
        toRibId(hostname, "inet.0", null),
        equalTo(new RibId(hostname, Configuration.DEFAULT_VRF_NAME, RibId.DEFAULT_RIB_NAME)));
    assertThat(
        toRibId(hostname, "inet.1", null),
        equalTo(new RibId(hostname, Configuration.DEFAULT_VRF_NAME, "inet.1")));
    assertThat(
        toRibId(hostname, "VRF1.inet.0", null),
        equalTo(new RibId(hostname, "VRF1", RibId.DEFAULT_RIB_NAME)));
    assertThat(
        toRibId(hostname, "VRF1.inet.1", null), equalTo(new RibId(hostname, "VRF1", "inet.1")));

    Warnings w = new Warnings(true, true, true);
    assertThat(toRibId(hostname, "inet6.0", w), nullValue());
    assertThat(
        w.getUnimplementedWarnings(),
        equalTo(
            ImmutableList.of(
                new Warning(
                    "Rib name conversion: inet6 address family is not supported",
                    TAG_UNIMPLEMENTED))));
  }

  @Test
  public void testToOspfDeadIntervalExplicit() {
    Interface iface = new Interface("ge-0/0/0");
    iface.setOspfDeadInterval(7);
    // Explicitly set dead interval should be preferred over inference
    assertThat(toOspfDeadInterval(iface), equalTo(7));
  }

  @Test
  public void testToOspfDeadIntervalFromHello() {
    Interface iface = new Interface("ge-0/0/0");
    int helloInterval = 1;
    iface.setOspfHelloInterval(helloInterval);
    // Since the dead interval is not set, it should be inferred as four times the hello interval
    assertThat(
        toOspfDeadInterval(iface), equalTo(OSPF_DEAD_INTERVAL_HELLO_MULTIPLIER * helloInterval));
  }

  @Test
  public void testToOspfDeadIntervalFromType() {
    Interface iface = new Interface("ge-0/0/0");
    // Since the dead interval and hello interval are not set, it should be inferred from the
    // network type
    iface.setOspfInterfaceType(OspfInterfaceType.P2P);
    assertThat(toOspfDeadInterval(iface), equalTo(DEFAULT_DEAD_INTERVAL));
    iface.setOspfInterfaceType(OspfInterfaceType.NBMA);
    assertThat(toOspfDeadInterval(iface), equalTo(DEFAULT_NBMA_DEAD_INTERVAL));
  }

  @Test
  public void testToOspfHelloIntervalExplicit() {
    Interface iface = new Interface("ge-0/0/0");
    iface.setOspfHelloInterval(7);
    // Explicitly set hello interval should be preferred over inference
    assertThat(toOspfHelloInterval(iface), equalTo(7));
  }

  @Test
  public void testToOspfHelloIntervalFromType() {
    Interface iface = new Interface("ge-0/0/0");
    // Since the hello interval is not set, it should be inferred from the network type
    iface.setOspfInterfaceType(OspfInterfaceType.P2P);
    assertThat(toOspfHelloInterval(iface), equalTo(DEFAULT_HELLO_INTERVAL));
    iface.setOspfInterfaceType(OspfInterfaceType.NBMA);
    assertThat(toOspfHelloInterval(iface), equalTo(DEFAULT_NBMA_HELLO_INTERVAL));
  }

  @Test
  public void testBuildSecurityPolicyAcl_traceElement_fromHost() {
    JuniperConfiguration config = createConfig();
    Zone zone = new Zone("toZone", null);

    ConcreteFirewallFilter crossZoneFilter =
        new ConcreteFirewallFilter("crossZonePolicy", Family.INET);

    zone.getFromZonePolicies().put("crossZonePolicy", crossZoneFilter);

    config.buildSecurityPolicyAcl("acl", zone);
    IpAccessList acl = config._c.getIpAccessLists().get("acl");
    assertNotNull(acl);
    assertThat(acl.getLines(), hasSize(3));

    assertThat(
        acl.getLines().get(0).getTraceElement(),
        equalTo(TraceElement.of("Matched Juniper semantics on traffic originated from device")));

    assertThat(
        acl.getLines().get(1).getTraceElement(),
        equalTo(TraceElement.of("Matched security policy from junos-host to zone toZone")));

    assertThat(
        acl.getLines().get(2).getTraceElement(),
        equalTo(TraceElement.of("Matched default policy")));
  }

  @Test
  public void testBuildSecurityPolicyAcl_traceElement_fromZone() {
    JuniperConfiguration config = createConfig();
    Zone zone = new Zone("toZone", null);

    ConcreteFirewallFilter crossZoneFilter =
        new ConcreteFirewallFilter("crossZonePolicy", Family.INET);

    crossZoneFilter.setFromZone("fromZone");

    zone.getFromZonePolicies().put("crossZonePolicy", crossZoneFilter);

    config.buildSecurityPolicyAcl("acl", zone);
    IpAccessList acl = config._c.getIpAccessLists().get("acl");
    assertNotNull(acl);
    assertThat(acl.getLines(), hasSize(3));

    assertThat(
        acl.getLines().get(0).getTraceElement(),
        equalTo(TraceElement.of("Matched Juniper semantics on traffic originated from device")));

    assertThat(
        acl.getLines().get(1).getTraceElement(),
        equalTo(TraceElement.of("Matched security policy from zone fromZone to zone toZone")));

    assertThat(
        acl.getLines().get(2).getTraceElement(),
        equalTo(TraceElement.of("Matched default policy")));
  }

  @Test
  public void testFwTermToIpAccessList_traceElement() {
    JuniperConfiguration config = createConfig();
    FwTerm term = new FwTerm("term");
    term.getThens().add(FwThenAccept.INSTANCE);
    List<FwTerm> terms = ImmutableList.of(term);
    IpAccessList acl = config.fwTermsToIpAccessList("acl", terms, null);

    assertThat(acl.getLines(), hasSize(1));

    TraceElement expected =
        TraceElement.builder()
            .add("Matched ")
            .add(
                "term",
                new VendorStructureId("file", FIREWALL_FILTER_TERM.getDescription(), "acl term"))
            .build();
    TraceElement actual = acl.getLines().get(0).getTraceElement();
    assertThat(actual, equalTo(expected));
    assertThat(actual.getText(), equalTo("Matched term"));
  }

  @Test
  public void testMergeIpAccessListLines() {
    List<ExprAclLine> lines =
        ImmutableList.of(
            new ExprAclLine(
                LineAction.PERMIT,
                new MatchHeaderSpace(
                    HeaderSpace.builder().setSrcIps(Ip.parse("1.1.1.1").toIpSpace()).build()),
                "match1",
                TraceElement.of("trace of match1")));

    MatchHeaderSpace conj =
        new MatchHeaderSpace(
            HeaderSpace.builder().setDstIps(Ip.parse("2.2.2.2").toIpSpace()).build());

    List<AclLine> aclLines = mergeIpAccessListLines(lines, conj);

    assertThat(aclLines, hasSize(1));
    assertThat(aclLines.get(0).getTraceElement(), equalTo(TraceElement.of("trace of match1")));
  }
}
