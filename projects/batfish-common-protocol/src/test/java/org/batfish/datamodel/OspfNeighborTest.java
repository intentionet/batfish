package org.batfish.datamodel;

import static org.batfish.datamodel.matchers.LinkMatchers.hasIp1;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasOspfNeighbors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfProcess;
import org.junit.Before;
import org.junit.Test;

public class OspfNeighborTest {

  private Configuration.Builder _cb;

  private Interface.Builder _ib;

  private NetworkFactory _nf;

  private OspfArea.Builder _oab;

  private OspfProcess.Builder _opb;

  private Vrf.Builder _vb;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _ib = _nf.interfaceBuilder().setActive(true).setOspfCost(100);
    _oab = _nf.ospfAreaBuilder().setNumber(0L);
    _opb = _nf.ospfProcessBuilder();
    _vb = _nf.vrfBuilder();
  }

  @Test
  public void testNoPassiveNeighbors() {
    Prefix p = Prefix.parse("1.0.0.0/31");
    Ip ip1 = p.getStartIp();
    Ip ip2 = p.getEndIp();
    int pl = p.getPrefixLength();

    Configuration c1 = _cb.build();
    Vrf v1 = _vb.setOwner(c1).build();
    OspfProcess op1 = _opb.setVrf(v1).build();
    OspfArea oa1 = _oab.setOspfProcess(op1).build();
    // i1
    _ib.setOwner(c1).setVrf(v1).setOspfArea(oa1).setAddress(new InterfaceAddress(ip1, pl)).build();

    Configuration c2 = _cb.build();
    Vrf v2 = _vb.setOwner(c2).build();
    OspfProcess op2 = _opb.setVrf(v2).build();
    OspfArea oa2 = _oab.setOspfProcess(op2).build();
    // i2
    _ib.setOwner(c2)
        .setVrf(v2)
        .setOspfArea(oa2)
        .setAddress(new InterfaceAddress(ip2, pl))
        .setOspfPassive(true)
        .build();

    Map<String, Configuration> configurations =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    Topology topology = CommonUtil.synthesizeTopology(configurations);
    CommonUtil.initRemoteOspfNeighbors(configurations, ipOwners, topology);

    assertThat(op1, hasOspfNeighbors(hasKey(new IpLink(ip1, Ip.ZERO))));
    assertThat(op1, hasOspfNeighbors(not(hasKey(hasIp1(equalTo(ip2))))));
  }
}
