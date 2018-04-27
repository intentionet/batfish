package org.batfish.dataplane.ibdp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsEqual.equalTo;

import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.dataplane.rib.RipInternalRib;
import org.junit.Test;

public class RipInternalRibTest {

  @Test
  public void testComparePreference() {
    RipInternalRib rib = new RipInternalRib(null);
    RipInternalRoute r1 =
        new RipInternalRoute(
            Prefix.parse("10.1.0.0/16"),
            Ip.AUTO,
            RoutingProtocol.RIP.getDefaultAdministrativeCost(ConfigurationFormat.CISCO_IOS),
            10);
    RipInternalRoute r2 =
        new RipInternalRoute(
            Prefix.parse("10.1.0.0/16"),
            Ip.AUTO,
            RoutingProtocol.RIP.getDefaultAdministrativeCost(ConfigurationFormat.CISCO_IOS),
            12);

    assertThat(rib.comparePreference(r1, r1), equalTo(0));
    assertThat(rib.comparePreference(r1, r2), greaterThan(0));
    assertThat(rib.comparePreference(r2, r1), lessThan(0));
  }
}
