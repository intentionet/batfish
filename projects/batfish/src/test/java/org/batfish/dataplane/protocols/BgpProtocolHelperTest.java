package org.batfish.dataplane.protocols;

import static org.batfish.datamodel.AbstractRoute.MAX_TAG;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.datamodel.Route.UNSET_ROUTE_TAG;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.convertGeneratedRouteToBgp;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRouteOnImport;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRoutePostExport;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.bgp.BgpTopologyUtils.ConfedSessionType;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Before;
import org.junit.Test;

public class BgpProtocolHelperTest {
  private static final Ip DEST_IP = Ip.parse("3.3.3.3");
  private static final Prefix DEST_NETWORK = Prefix.parse("4.4.4.0/24");
  private static final Ip ORIGINATOR_IP = Ip.parse("1.1.1.1");
  private final BgpProcess _process =
      BgpProcess.builder()
          .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
          .setRouterId(ORIGINATOR_IP)
          .build();
  private Builder _baseBgpRouteBuilder;

  /** Reset route builder */
  @Before
  public void resetDefaultRouteBuilders() {
    _baseBgpRouteBuilder =
        new Bgpv4Route.Builder()
            .setOriginatorIp(ORIGINATOR_IP)
            .setOriginType(OriginType.IGP)
            .setNetwork(DEST_NETWORK)
            .setNextHopIp(DEST_IP)
            .setProtocol(RoutingProtocol.IBGP)
            .setReceivedFromIp(Ip.ZERO);
  }

  @Test
  public void testTransformOnImportNoAllowAsIn() {
    assertThat(
        "AS path loop, return null",
        transformBgpRouteOnImport(
            _baseBgpRouteBuilder.setAsPath(AsPath.ofSingletonAsSets(1L)).build(),
            1L,
            false,
            true,
            _process,
            Ip.ZERO,
            null),
        nullValue());
  }

  @Test
  public void testTransformOnImportAllowAsIn() {
    assertThat(
        "AS path loop allowed",
        transformBgpRouteOnImport(
            _baseBgpRouteBuilder.setAsPath(AsPath.ofSingletonAsSets(1L)).build(),
            1L,
            true,
            true,
            _process,
            Ip.ZERO,
            null),
        notNullValue());
  }

  @Test
  public void testTransformOnImportEbgp() {
    assertThat(
        "No AS path loop, eBGP",
        transformBgpRouteOnImport(
                _baseBgpRouteBuilder.setAsPath(AsPath.ofSingletonAsSets(1L)).build(),
                2L,
                false,
                true,
                _process,
                Ip.ZERO,
                null)
            .getProtocol(),
        equalTo(RoutingProtocol.BGP));
  }

  @Test
  public void testTransformOnImportClearsNextHop() {
    assertThat(
        "NextHopInterface should be cleared even if peerInterface is null",
        transformBgpRouteOnImport(
                _baseBgpRouteBuilder
                    .setAsPath(AsPath.ofSingletonAsSets(1L))
                    .setNextHopInterface("foobar")
                    .build(),
                2L,
                false,
                true,
                _process,
                Ip.ZERO,
                null)
            .getNextHopInterface(),
        equalTo(UNSET_NEXT_HOP_INTERFACE));
  }

  @Test
  public void testTransformOnImportWithPeerInterface() {
    assertThat(
        "NextHopInterface should be set to peerInterface",
        transformBgpRouteOnImport(
                _baseBgpRouteBuilder
                    .setAsPath(AsPath.ofSingletonAsSets(1L))
                    .setNextHopInterface("foobar")
                    .build(),
                2L,
                false,
                true,
                _process,
                Ip.ZERO,
                "baz")
            .getNextHopInterface(),
        equalTo("baz"));
  }

  @Test
  public void testTransformOnImportIbgp() {
    assertThat(
        "No AS path loop, iBGP",
        transformBgpRouteOnImport(
                _baseBgpRouteBuilder.build(), 1L, false, false, _process, Ip.ZERO, null)
            .getProtocol(),
        equalTo(RoutingProtocol.IBGP));
  }

  @Test
  public void testTransformOnImportReceivedFromIp() {
    assertThat(
        transformBgpRouteOnImport(
                _baseBgpRouteBuilder.build(), 1L, false, false, _process, Ip.parse("1.2.3.4"), null)
            .build()
            .getReceivedFromIp(),
        equalTo(Ip.parse("1.2.3.4")));
  }

  @Test
  public void testTransformOnImportClearAdminSetInterface() {
    final Builder builder =
        transformBgpRouteOnImport(
            _baseBgpRouteBuilder.setAdmin(Integer.MAX_VALUE).build(),
            2L,
            false,
            true,
            _process,
            Ip.ZERO,
            "eth0");
    assertThat("PeerInterface is set", builder.getNextHopInterface(), equalTo("eth0"));
    assertThat(
        "AdminDistance is set",
        builder.getAdmin(),
        equalTo(RoutingProtocol.BGP.getDefaultAdministrativeCost(ConfigurationFormat.CISCO_IOS)));
  }

  @Test
  public void testTransformPostExportClearTag() {
    Builder builder = _baseBgpRouteBuilder.setTag(MAX_TAG);
    transformBgpRoutePostExport(builder, true, ConfedSessionType.NO_CONFED, 1, DEST_IP, Ip.ZERO);
    assertThat("Tag is cleared", builder.getTag(), equalTo(UNSET_ROUTE_TAG));
    transformBgpRoutePostExport(builder, false, ConfedSessionType.NO_CONFED, 1, DEST_IP, Ip.ZERO);
    assertThat("Tag is cleared", builder.getTag(), equalTo(UNSET_ROUTE_TAG));
  }

  @Test
  public void testTransformPostExportPrependAs() {
    AsPath baseAsPath = AsPath.of(ImmutableList.of(AsSet.of(777), AsSet.confed(888)));
    // Prepend own as
    _baseBgpRouteBuilder.setAsPath(baseAsPath);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, true, ConfedSessionType.NO_CONFED, 1, DEST_IP, Ip.ZERO);
    assertThat(
        _baseBgpRouteBuilder.getAsPath(),
        equalTo(
            AsPath.of(
                ImmutableList.<AsSet>builder()
                    .add(AsSet.of(1))
                    .addAll(baseAsPath.getAsSets())
                    .build())));

    // Prepend own as across border
    resetDefaultRouteBuilders();
    _baseBgpRouteBuilder.setAsPath(baseAsPath);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, true, ConfedSessionType.ACROSS_CONFED_BORDER, 2, DEST_IP, Ip.ZERO);
    assertThat(
        _baseBgpRouteBuilder.getAsPath(),
        equalTo(
            AsPath.of(ImmutableList.<AsSet>builder().add(AsSet.of(2)).add(AsSet.of(777)).build())));

    // Prepend confederation AS set
    resetDefaultRouteBuilders();
    _baseBgpRouteBuilder.setAsPath(baseAsPath);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, true, ConfedSessionType.WITHIN_CONFED, 4, DEST_IP, Ip.ZERO);
    assertThat(
        _baseBgpRouteBuilder.getAsPath(),
        equalTo(
            AsPath.of(
                ImmutableList.<AsSet>builder()
                    .add(AsSet.confed(4))
                    .addAll(baseAsPath.getAsSets())
                    .build())));

    // Do not prepend for IBGP
    resetDefaultRouteBuilders();
    _baseBgpRouteBuilder.setAsPath(baseAsPath);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, false, ConfedSessionType.NO_CONFED, 5, DEST_IP, Ip.ZERO);
    assertThat(_baseBgpRouteBuilder.getAsPath(), equalTo(baseAsPath));

    // Do not prepend for IBGP within confed
    resetDefaultRouteBuilders();
    _baseBgpRouteBuilder.setAsPath(baseAsPath);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, false, ConfedSessionType.WITHIN_CONFED, 6, DEST_IP, Ip.ZERO);
    assertThat(_baseBgpRouteBuilder.getAsPath(), equalTo(baseAsPath));
  }

  @Test
  public void testTransformPostExportNextHopIp() {
    Ip nextHopIp = Ip.parse("1.2.3.4");
    _baseBgpRouteBuilder.setNextHopIp(null);

    // Pure eBGP, not set by the policy
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, true, ConfedSessionType.NO_CONFED, 1, nextHopIp, DEST_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(nextHopIp));

    // eBGP across confederation border
    _baseBgpRouteBuilder.setNextHopIp(null);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, true, ConfedSessionType.ACROSS_CONFED_BORDER, 1, nextHopIp, DEST_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(nextHopIp));

    // eBGP within confederation -- change
    _baseBgpRouteBuilder.setNextHopIp(null);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, true, ConfedSessionType.WITHIN_CONFED, 1, nextHopIp, DEST_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(nextHopIp));

    // iBGP no confederation -- no change
    _baseBgpRouteBuilder.setNextHopIp(null);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, false, ConfedSessionType.NO_CONFED, 1, nextHopIp, DEST_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(DEST_IP));

    // iBGP within confederation -- no change
    _baseBgpRouteBuilder.setNextHopIp(null);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder, false, ConfedSessionType.WITHIN_CONFED, 1, nextHopIp, DEST_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(DEST_IP));

    // eBGP within confederation, unset original IP -- overwrite
    _baseBgpRouteBuilder.setNextHopIp(null);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder,
        true,
        ConfedSessionType.WITHIN_CONFED,
        1,
        nextHopIp,
        UNSET_ROUTE_NEXT_HOP_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(nextHopIp));

    // iBGP no confederation, unset original IP -- overwrite
    _baseBgpRouteBuilder.setNextHopIp(null);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder,
        false,
        ConfedSessionType.NO_CONFED,
        1,
        nextHopIp,
        UNSET_ROUTE_NEXT_HOP_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(nextHopIp));

    // iBGP within confederation, unset original IP -- overwrite
    _baseBgpRouteBuilder.setNextHopIp(null);
    transformBgpRoutePostExport(
        _baseBgpRouteBuilder,
        false,
        ConfedSessionType.WITHIN_CONFED,
        1,
        nextHopIp,
        UNSET_ROUTE_NEXT_HOP_IP);
    assertThat(_baseBgpRouteBuilder.getNextHopIp(), equalTo(nextHopIp));
  }

  @Test
  public void testTransformGeneratedRouteWithAttrPolicy() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setHostname("c")
            .setConfigurationFormat(ConfigurationFormat.ARISTA)
            .build();
    StandardCommunity community = StandardCommunity.of(1L);
    Bgpv4Route result =
        convertGeneratedRouteToBgp(
            GeneratedRoute.builder().setNetwork(Prefix.ZERO).setDiscard(true).build(),
            nf.routingPolicyBuilder()
                .setOwner(c)
                .setStatements(
                    ImmutableList.of(
                        new SetCommunity(new LiteralCommunity(community)),
                        Statements.ReturnTrue.toStaticStatement()))
                .build(),
            Ip.MAX,
            Ip.ZERO,
            true);

    assertThat(result.getCommunities().getCommunities(), equalTo(ImmutableSet.of(community)));
  }
}
