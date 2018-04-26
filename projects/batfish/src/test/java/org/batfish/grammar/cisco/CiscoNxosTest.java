package org.batfish.grammar.cisco;

import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.main.BatfishTestUtils.getBatfishForTextConfigs;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.main.Batfish;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CiscoNxosTest {
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private Configuration parseConfig(String hostname) {
    try {
      return parseTextConfigs(hostname).get(hostname);
    } catch (IOException e) {
      throw new AssertionError("Failed to parse config " + hostname, e);
    }
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    Batfish batfish = getBatfishForTextConfigs(_folder, names);
    batfish.getSettings().setEnableCiscoNxParser(true);
    return batfish.loadConfigurations();
  }

  @Test
  public void testRouterId() {
    Configuration c = parseConfig("nxosBgpRouterId");
    // default VRF has manually set router id.
    assertThat(c, hasVrf("default", hasBgpProcess(hasRouterId(new Ip("4.4.4.4")))));
    // vrf1 has manually set router id.
    assertThat(c, hasVrf("vrf1", hasBgpProcess(hasRouterId(new Ip("2.3.1.4")))));
    // vrf2 has no configured router id, but there is an associated loopback.
    assertThat(c, hasVrf("vrf2", hasBgpProcess(hasRouterId(new Ip("1.1.1.1")))));
    // vrf3 has no configured router id and no interfaces. Cisco uses 0.0.0.0. Note that it does NOT
    // inherit from default VRF's manual config or pickup Loopback0 in another VRF.
    assertThat(c, hasVrf("vrf3", hasBgpProcess(hasRouterId(Ip.ZERO))));
    // vrf4 has loopback0.
    assertThat(c, hasVrf("vrf4", hasBgpProcess(hasRouterId(new Ip("1.2.3.4")))));
  }
}
