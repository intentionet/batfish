package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.BgpRoute.Builder;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link BgpRoute} */
public class BgpRouteTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testJavaSerialization() {
    BgpRoute br =
        BgpRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setNextHopInterface("blah")
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    assertThat(SerializationUtils.clone(br), equalTo(br));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    BgpRoute br =
        BgpRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setNextHopInterface("blah")
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    assertThat(BatfishObjectMapper.clone(br, BgpRoute.class), equalTo(br));
  }

  @Test
  public void testToBuilder() {
    BgpRoute br =
        BgpRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setNextHopInterface("blah")
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    assertThat(br, equalTo(br.toBuilder().build()));
  }

  @Test
  public void testEquals() {
    Builder brb =
        BgpRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP);
    new EqualsTester()
        .addEqualityGroup(brb.build(), brb.build())
        .addEqualityGroup(brb.setNetwork(Prefix.parse("1.1.2.0/24")).build())
        .addEqualityGroup(brb.setAdmin(10).build())
        .addEqualityGroup(brb.setNonRouting(true).build())
        .addEqualityGroup(brb.setNonForwarding(true).build())
        .addEqualityGroup(brb.setAsPath(AsPath.ofSingletonAsSets(1L, 1L)).build())
        .addEqualityGroup(brb.setClusterList(ImmutableSet.of(1L)).build())
        .addEqualityGroup(brb.setCommunities(ImmutableSet.of(StandardCommunity.of(1L))).build())
        .addEqualityGroup(brb.setDiscard(true).build())
        .addEqualityGroup(brb.setLocalPreference(10).build())
        .addEqualityGroup(brb.setMetric(10).build())
        .addEqualityGroup(brb.setNextHopInterface("blah").build())
        .addEqualityGroup(brb.setNextHopIp(Ip.parse("2.2.2.2")).build())
        .addEqualityGroup(brb.setOriginatorIp(Ip.parse("2.2.2.2")).build())
        .addEqualityGroup(brb.setOriginType(OriginType.INCOMPLETE).build())
        .addEqualityGroup(brb.setReceivedFromIp(Ip.parse("1.1.1.1")).build())
        .addEqualityGroup(brb.setReceivedFromRouteReflectorClient(true).build())
        .addEqualityGroup(brb.setProtocol(RoutingProtocol.IBGP).build())
        .addEqualityGroup(brb.setSrcProtocol(RoutingProtocol.STATIC).build())
        .addEqualityGroup(brb.setWeight(1).build())
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testThrowsWithoutOriginType() {
    thrown.expect(IllegalArgumentException.class);
    BgpRoute.builder()
        .setNetwork(Prefix.parse("1.1.1.0/24"))
        .setOriginatorIp(Ip.parse("1.1.1.1"))
        .setProtocol(RoutingProtocol.BGP)
        .build();
  }

  @Test
  public void testThrowsWithoutOriginatorIp() {
    thrown.expect(IllegalArgumentException.class);
    BgpRoute.builder()
        .setNetwork(Prefix.parse("1.1.1.0/24"))
        .setProtocol(RoutingProtocol.BGP)
        .setOriginType(OriginType.IGP)
        .build();
  }

  @Test
  public void testThrowsWithoutProtocol() {
    thrown.expect(IllegalArgumentException.class);
    BgpRoute.builder()
        .setNetwork(Prefix.parse("1.1.1.0/24"))
        .setOriginatorIp(Ip.parse("1.1.1.1"))
        .setOriginType(OriginType.IGP)
        .build();
  }
}
