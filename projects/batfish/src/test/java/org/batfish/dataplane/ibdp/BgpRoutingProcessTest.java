package org.batfish.dataplane.ibdp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConfigurationFormat;
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
            new BgpProcess(),
            nf.configurationBuilder()
                .setHostname("c")
                .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
                .build(),
            "vrf",
            new Rib());
    // iBGP
    assertThat(process._ibgpRib.getRoutes(), empty());
    assertThat(process._ibgpStagingRib.getRoutes(), empty());
    // eBGP
    assertThat(process._ebgpRib.getRoutes(), empty());
    assertThat(process._ebgpStagingRib.getRoutes(), empty());
    // EVPN
    assertThat(process._ebgpEvpnRib.getRoutes(), empty());
    assertThat(process._ibgpEvpnRib.getRoutes(), empty());
    // Combined bgp
    assertThat(process._bgpRib.getRoutes(), empty());
  }

  @Test
  public void testInitQueuesEmpty() {}
}
