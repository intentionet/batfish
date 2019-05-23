package org.batfish.datamodel.bgp;

import static org.batfish.datamodel.bgp.Layer3VniConfig.importRtPatternForAnyAs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.bgp.Layer3VniConfig.Builder;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link Layer3VniConfig} */
public class Layer3VniConfigTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testEquals() {
    Layer3VniConfig vni =
        new Builder()
            .setVni(1)
            .setVrf("v")
            .setRouteDistinguisher(RouteDistinguisher.from(65555L, 1))
            .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
            .setImportRouteTarget(importRtPatternForAnyAs(1))
            .setAdvertisev4Unicast(false)
            .build();
    new EqualsTester()
        .addEqualityGroup(
            vni,
            vni,
            new Builder()
                .setVni(1)
                .setVrf("v")
                .setRouteDistinguisher(RouteDistinguisher.from(65555L, 1))
                .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
                .setImportRouteTarget(importRtPatternForAnyAs(1))
                .setAdvertisev4Unicast(false)
                .build())
        .addEqualityGroup(
            new Builder()
                .setVni(2)
                .setVrf("v")
                .setRouteDistinguisher(RouteDistinguisher.from(65555L, 1))
                .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
                .setImportRouteTarget(importRtPatternForAnyAs(1))
                .setAdvertisev4Unicast(false)
                .build())
        .addEqualityGroup(
            new Builder()
                .setVni(1)
                .setVrf("v2")
                .setRouteDistinguisher(RouteDistinguisher.from(65555L, 1))
                .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
                .setImportRouteTarget(importRtPatternForAnyAs(1))
                .setAdvertisev4Unicast(false)
                .build())
        .addEqualityGroup(
            new Builder()
                .setVni(1)
                .setVrf("v")
                .setRouteDistinguisher(RouteDistinguisher.from(65555L, 2))
                .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
                .setImportRouteTarget(importRtPatternForAnyAs(1))
                .setAdvertisev4Unicast(false)
                .build())
        .addEqualityGroup(
            new Builder()
                .setVni(1)
                .setVrf("v")
                .setRouteDistinguisher(RouteDistinguisher.from(65555L, 0))
                .setRouteTarget(ExtendedCommunity.of(0, 2, 1))
                .setImportRouteTarget("^2:1$")
                .setAdvertisev4Unicast(false)
                .build())
        .addEqualityGroup(
            new Builder()
                .setVni(1)
                .setVrf("v")
                .setRouteDistinguisher(RouteDistinguisher.from(65555L, 0))
                .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
                .setImportRouteTarget(importRtPatternForAnyAs(1))
                .setAdvertisev4Unicast(true)
                .build())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    Layer3VniConfig vni =
        new Builder()
            .setVni(1)
            .setVrf("v")
            .setRouteDistinguisher(RouteDistinguisher.from(65555L, 1))
            .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
            .setImportRouteTarget(importRtPatternForAnyAs(1))
            .setAdvertisev4Unicast(false)
            .build();
    assertThat(SerializationUtils.clone(vni), equalTo(vni));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    Layer3VniConfig vni =
        new Builder()
            .setVni(1)
            .setVrf("v")
            .setRouteDistinguisher(RouteDistinguisher.from(65555L, 1))
            .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
            .setImportRouteTarget(importRtPatternForAnyAs(1))
            .setAdvertisev4Unicast(false)
            .build();
    assertThat(BatfishObjectMapper.clone(vni, Layer3VniConfig.class), equalTo(vni));
  }

  @Test
  public void testImportRtPatternForAnyAs() {
    assertThat(importRtPatternForAnyAs(1), equalTo("^\\d+:1$"));
    assertThat(importRtPatternForAnyAs(2), equalTo("^\\d+:2$"));
  }

  @Test
  public void testImportRtPatternForAnyAsTooLow() {
    _thrown.expect(IllegalArgumentException.class);
    importRtPatternForAnyAs(-1);
  }

  @Test
  public void testImportRtPatternForAnyAsTooHigh() {
    _thrown.expect(IllegalArgumentException.class);
    importRtPatternForAnyAs(16777216);
  }

  @Test
  public void testInvalidPattern() {
    _thrown.expect(IllegalArgumentException.class);
    importRtPatternForAnyAs(16777216);
  }
}
