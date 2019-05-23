package org.batfish.datamodel.bgp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.bgp.Layer3VniConfig.Builder;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.junit.Test;

/** Tests of {@link EvpnAddressFamily} */
public class EvpnAddressFamilyTest {
  @Test
  public void testEquals() {
    EvpnAddressFamily af = new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of());
    new EqualsTester()
        .addEqualityGroup(af, af, new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of()))
        .addEqualityGroup(
            new EvpnAddressFamily(
                ImmutableSet.of(
                    new Layer2VniConfig(
                        1, "v", RouteDistinguisher.from(1L, 1), ExtendedCommunity.of(0, 1, 1))),
                ImmutableSet.of()))
        .addEqualityGroup(
            new EvpnAddressFamily(
                ImmutableSet.of(),
                ImmutableSet.of(
                    new Builder()
                        .setVni(1)
                        .setVrf("v")
                        .setRouteDistinguisher(RouteDistinguisher.from(1L, 1))
                        .setRouteTarget(ExtendedCommunity.of(0, 1, 1))
                        .setImportRouteTarget(Layer3VniConfig.importRtPatternForAnyAs(1))
                        .setAdvertisev4Unicast(false)
                        .build())))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    EvpnAddressFamily af = new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of());
    assertThat(SerializationUtils.clone(af), equalTo(af));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    EvpnAddressFamily af = new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of());
    assertThat(BatfishObjectMapper.clone(af, EvpnAddressFamily.class), equalTo(af));
  }
}
