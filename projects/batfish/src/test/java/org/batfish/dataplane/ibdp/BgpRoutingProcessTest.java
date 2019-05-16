package org.batfish.dataplane.ibdp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.dataplane.rib.Rib;
import org.junit.Test;

/** Tests of {@link BgpRoutingProcess} */
public class BgpRoutingProcessTest {
  @Test
  public void testInitRibsEmpty() {
    NetworkFactory nf = new NetworkFactory();
    BgpRoutingProcess process =
        new BgpRoutingProcess(
            new BgpProcess(Ip.ZERO),
            nf.configurationBuilder()
                .setHostname("c")
                .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
                .build(),
            "vrf",
            new Rib());
    // iBGP
    assertThat(process._ibgpv4Rib.getRoutes(), empty());
    assertThat(process._ibgpv4StagingRib.getRoutes(), empty());
    // eBGP
    assertThat(process._ebgpv4Rib.getRoutes(), empty());
    assertThat(process._ebgpv4StagingRib.getRoutes(), empty());
    // EVPN
    assertThat(process._ebgpEvpnRib.getRoutes(), empty());
    assertThat(process._ibgpEvpnRib.getRoutes(), empty());
    // Combined bgp
    assertThat(process._bgpv4Rib.getRoutes(), empty());
  }
}
