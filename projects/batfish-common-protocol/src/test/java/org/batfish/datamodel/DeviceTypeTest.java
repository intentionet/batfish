package org.batfish.datamodel;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DeviceTypeTest {

  NetworkFactory _nf;
  Configuration.Builder _cb;
  Vrf.Builder _vb;

  @Before
  public void initializeBuilders() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _vb = _nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
  }

  @Test
  public void emptyConfigurationIsSwitch() {
    Configuration c = _cb.build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.SWITCH));
  }

  @Test
  public void configWithEmptyVrfIsSwitch() {
    Configuration c = _cb.build();
    _vb.setOwner(c).build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.SWITCH));
  }

  @Test
  public void hostConfigIsHost() {
    Configuration c = _cb.setConfigurationFormat(ConfigurationFormat.HOST).build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.HOST));
  }

  @Test
  public void hostWithBgpIsHost() {
    Configuration c = _cb.setConfigurationFormat(ConfigurationFormat.HOST).build();
    Vrf vrf = _vb.setOwner(c).build();
    _nf.bgpProcessBuilder().setVrf(vrf).setRouterId(Ip.ZERO).build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.HOST));
  }

  @Test
  public void configWithFirstVrfEmptyIsRouter() {
    Configuration c = _cb.build();
    _vb.setOwner(c).build();
    Vrf vrf = _vb.setOwner(c).build();
    _nf.bgpProcessBuilder().setVrf(vrf).setRouterId(Ip.ZERO).build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.ROUTER));
  }

  @Test
  public void configWithBgpIsRouter() {
    Configuration c = _cb.build();
    Vrf vrf = _vb.setOwner(c).build();
    _nf.bgpProcessBuilder().setVrf(vrf).setRouterId(Ip.ZERO).build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.ROUTER));
  }

  @Test
  public void configWithOspfIsRouter() {
    Configuration c = _cb.build();
    Vrf vrf = _vb.setOwner(c).build();
    _nf.ospfProcessBuilder().setVrf(vrf).build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.ROUTER));
  }

  @Test
  public void configWithRipIsRouter() {
    Configuration c = _cb.build();
    Vrf vrf = _vb.setOwner(c).build();
    _nf.ospfProcessBuilder().setVrf(vrf).build();
    postProcessConfiguration(c);
    assertThat(c.getDeviceType(), is(DeviceType.ROUTER));
  }

  // Exact copy of the code executed for each configuration in Batfish.postProcessConfigurations()
  private void postProcessConfiguration(Configuration c) {
    // Set device type to host iff the configuration format is HOST
    if (c.getConfigurationFormat() == ConfigurationFormat.HOST) {
      c.setDeviceType(DeviceType.HOST);
    }
    for (Vrf vrf : c.getVrfs().values()) {
      // If vrf has BGP, OSPF, or RIP process and device isn't a host, set device type to router
      if (c.getDeviceType() == null
          && (vrf.getBgpProcess() != null
              || !vrf.getOspfProcesses().isEmpty()
              || vrf.getRipProcess() != null)) {
        c.setDeviceType(DeviceType.ROUTER);
      }
      // Compute OSPF interface costs where they are missing
      vrf.getOspfProcesses().values().forEach(proc -> proc.initInterfaceCosts(c));
    }
    // If device was not a host or router, call it a switch
    if (c.getDeviceType() == null) {
      c.setDeviceType(DeviceType.SWITCH);
    }
  }
}
