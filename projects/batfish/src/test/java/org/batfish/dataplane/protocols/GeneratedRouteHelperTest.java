package org.batfish.dataplane.protocols;

import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.dataplane.ibdp.TestUtils.annotateRoute;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GeneratedRoute.Builder;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link GeneratedRouteHelper}. */
public class GeneratedRouteHelperTest {

  private GeneratedRoute.Builder _builder;

  @Before
  public void setup() {
    _builder = GeneratedRoute.builder();
  }

  @Test
  public void activateWhenPolicyIsNull() {
    GeneratedRoute gr = _builder.setNetwork(Prefix.ZERO).build();

    Builder newRoute = GeneratedRouteHelper.activateGeneratedRoute(gr, null, ImmutableSet.of());

    assertThat(newRoute, notNullValue());
  }

  @Test
  public void testDiscardIsHonored() {
    GeneratedRoute gr = _builder.setDiscard(true).setNetwork(Prefix.ZERO).build();

    GeneratedRouteHelper.activateGeneratedRoute(gr, null, ImmutableSet.of());

    assertThat(gr.getDiscard(), equalTo(true));
  }

  @Test
  public void doNotActivateWithoutPolicyMatch() {
    GeneratedRoute gr = _builder.setDiscard(true).setNetwork(Prefix.parse("1.1.1.0/24")).build();
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("n1")
            .build();

    RoutingPolicy policy =
        nf.routingPolicyBuilder()
            .setName("no match")
            .setOwner(c)
            .setStatements(ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))
            .build();

    Builder newRoute = GeneratedRouteHelper.activateGeneratedRoute(gr, policy, ImmutableSet.of());
    assertThat(newRoute, nullValue());
  }

  @Test
  public void activateWithPolicyMatch() {
    GeneratedRoute gr = _builder.setDiscard(true).setNetwork(Prefix.parse("1.1.1.0/24")).build();
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("n1")
            .build();
    nf.vrfBuilder().setOwner(c).build();

    RoutingPolicy policy =
        nf.routingPolicyBuilder()
            .setName("always match")
            .setOwner(c)
            .setStatements(ImmutableList.of(Statements.ReturnTrue.toStaticStatement()))
            .build();

    Builder newRoute =
        GeneratedRouteHelper.activateGeneratedRoute(
            gr,
            policy,
            ImmutableSet.of(
                annotateRoute(
                    StaticRoute.testBuilder()
                        .setNetwork(Prefix.parse("2.2.2.2/32"))
                        .setNextHopIp(null)
                        .setNextHopInterface("eth0")
                        .setAdministrativeCost(1)
                        .setMetric(0L)
                        .setTag(1L)
                        .build())));

    assertThat(newRoute, notNullValue());
  }

  @Test
  public void testActivateAndSetBgpProperties() {
    GeneratedRoute gr = _builder.setDiscard(true).setNetwork(Prefix.parse("1.1.1.0/24")).build();
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("n1")
            .build();
    nf.vrfBuilder().setOwner(c).build();

    RoutingPolicy policy =
        nf.routingPolicyBuilder()
            .setName("always match")
            .setOwner(c)
            .setStatements(
                ImmutableList.of(
                    new SetCommunities(
                        new LiteralCommunitySet(CommunitySet.of(StandardCommunity.of(1L)))),
                    Statements.ReturnTrue.toStaticStatement()))
            .build();

    Builder newRoute =
        GeneratedRouteHelper.activateGeneratedRoute(
            gr,
            policy,
            ImmutableSet.of(
                annotateRoute(new ConnectedRoute(Prefix.strict("2.2.2.2/32"), "blah"))));

    assertThat(newRoute.build(), hasCommunities(contains(StandardCommunity.of(1L))));
  }
}
