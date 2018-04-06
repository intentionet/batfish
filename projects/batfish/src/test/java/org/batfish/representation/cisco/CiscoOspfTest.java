package org.batfish.representation.cisco;

import static org.batfish.datamodel.RoutingProtocol.BGP;
import static org.batfish.datamodel.routing_policy.statement.Statements.ExitAccept;
import static org.batfish.representation.cisco.CiscoConfiguration.NOT_DEFAULT_ROUTE;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_REDISTRIBUTE_BGP_MAP;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.OspfMetricType;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.RouteIsClassfull;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CiscoOspfTest {
  private static CiscoConfiguration makeConfig() {
    CiscoConfiguration config = new CiscoConfiguration(Collections.emptySet());
    config.setVendor(ConfigurationFormat.CISCO_IOS);
    return config;
  }

  private CiscoConfiguration _config;
  private OspfProcess _proc;

  @Before
  public void before() {
    _proc = new OspfProcess("10", ConfigurationFormat.CISCO_IOS);
    _config = makeConfig();
  }

  @Test
  public void testBasicConvertRcp() {
    OspfRedistributionPolicy rcp = new OspfRedistributionPolicy(BGP);
    rcp.setOspfMetricType(OspfMetricType.E2);
    rcp.setRouteMap("some-map");
    _config.getRouteMaps().put("some-map", new RouteMap("some-map", 10));

    If policy = _config.convertOspfRedistributionPolicy(rcp, _proc, OSPF_REDISTRIBUTE_BGP_MAP);
    List<BooleanExpr> guard = ((Conjunction) policy.getGuard()).getConjuncts();
    assertThat(
        guard,
        contains(
            new MatchProtocol(BGP),
            NOT_DEFAULT_ROUTE,
            RouteIsClassfull.instance(),
            new CallExpr("some-map")));
    assertThat(
        policy.getTrueStatements(),
        contains(
            new SetOspfMetricType(OspfMetricType.E2),
            new SetMetric(new LiteralLong(1L)),
            ExitAccept.toStaticStatement()));
  }

  @Test
  public void testConvertRcpMetric() {
    OspfRedistributionPolicy rcp = new OspfRedistributionPolicy(BGP);
    rcp.setOspfMetricType(OspfMetricType.E2);

    // Vendor default BGP metric is 1 for IOS.
    If policy = _config.convertOspfRedistributionPolicy(rcp, _proc, OSPF_REDISTRIBUTE_BGP_MAP);
    assertThat(policy.getTrueStatements(), hasItem(new SetMetric(new LiteralLong(1L))));

    // Vendor default overridden by process default.
    _proc.setDefaultMetric(3L);
    policy = _config.convertOspfRedistributionPolicy(rcp, _proc, OSPF_REDISTRIBUTE_BGP_MAP);
    assertThat(policy.getTrueStatements(), hasItem(new SetMetric(new LiteralLong(3L))));

    // RCP metric configured wins.
    rcp.setMetric(5L);
    policy = _config.convertOspfRedistributionPolicy(rcp, _proc, OSPF_REDISTRIBUTE_BGP_MAP);
    assertThat(policy.getTrueStatements(), hasItem(new SetMetric(new LiteralLong(5L))));
  }
}
