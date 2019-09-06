package org.batfish.datamodel.eigrp;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Map;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

/** Tests for {@link EigrpTopologyUtils} */
public class EigrpTopologyUtilsTest {

  @Test
  public void testInitNeighborConfigs() {
    NetworkFactory nf = new NetworkFactory();
    Configuration configuration =
        nf.configurationBuilder()
            .setHostname("conf")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    Vrf vrf = nf.vrfBuilder().setName("vrf").setOwner(configuration).build();
    EigrpMetric metric =
        WideMetric.builder()
            .setValues(EigrpMetricValues.builder().setBandwidth(1d).setDelay(1d).build())
            .build();
    nf.interfaceBuilder()
        .setName("iface1")
        .setOwner(configuration)
        .setVrf(vrf)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24))
        .setEigrp(
            EigrpInterfaceSettings.builder()
                .setAsn(1L)
                .setExportPolicy("ep")
                .setEnabled(true)
                .setPassive(true)
                .setMetric(metric)
                .build())
        .build();
    nf.interfaceBuilder()
        .setName("iface2")
        .setOwner(configuration)
        .setVrf(vrf)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24))
        .setEigrp(
            EigrpInterfaceSettings.builder()
                .setAsn(2L)
                .setExportPolicy("ep")
                .setEnabled(true)
                .setPassive(true)
                .setMetric(metric)
                .build())
        .build();
    vrf.addEigrpProcess(
        EigrpProcess.builder()
            .setAsNumber(1L)
            .setExportPolicy("ep")
            .setMode(EigrpProcessMode.NAMED)
            .setRouterId(Ip.parse("1.1.1.1"))
            .setExportPolicy("policy")
            .build());

    EigrpTopologyUtils.initNeighborConfigs(
        NetworkConfigurations.of(ImmutableMap.of(configuration.getHostname(), configuration)));

    // only the neighbor corresponding to matching ASN is initialized
    assertThat(
        configuration.getVrfs().get("vrf").getEigrpProcesses().get(1L).getNeighbors(),
        equalTo(
            ImmutableMap.of(
                "iface1",
                EigrpNeighborConfig.builder()
                    .setAsn(1L)
                    .setInterfaceName("iface1")
                    .setPassive(true)
                    .setExportPolicy("ep")
                    .setHostname("conf")
                    .setVrfName("vrf")
                    .setIp(Ip.parse("1.1.1.1"))
                    .build())));
  }

  private static Map<String, Configuration> configurationsForEigrpTopology() {
    EigrpMetric metric =
        WideMetric.builder()
            .setValues(EigrpMetricValues.builder().setBandwidth(1d).setDelay(1d).build())
            .build();
    EigrpInterfaceSettings eigrpInterfaceSettings =
        EigrpInterfaceSettings.builder()
            .setAsn(1L)
            .setEnabled(true)
            .setMetric(metric)
            .setExportPolicy("ep")
            .build();

    NetworkFactory nf = new NetworkFactory();

    Configuration conf1 =
        nf.configurationBuilder()
            .setHostname("conf1")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    Vrf vrf = nf.vrfBuilder().setName("vrf").setOwner(conf1).build();
    nf.interfaceBuilder()
        .setName("iface1")
        .setOwner(conf1)
        .setVrf(vrf)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24))
        .setEigrp(eigrpInterfaceSettings)
        .build();
    nf.interfaceBuilder()
        .setName("iface2")
        .setOwner(conf1)
        .setVrf(vrf)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24))
        .setEigrp(eigrpInterfaceSettings)
        .build();
    vrf.addEigrpProcess(
        EigrpProcess.builder()
            .setAsNumber(1L)
            .setMode(EigrpProcessMode.NAMED)
            .setRouterId(Ip.parse("1.1.1.1"))
            .build());

    Configuration conf2 =
        nf.configurationBuilder()
            .setHostname("conf2")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    vrf = nf.vrfBuilder().setName("vrf").setOwner(conf2).build();
    nf.interfaceBuilder()
        .setName("iface1")
        .setOwner(conf2)
        .setVrf(vrf)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.4"), 24))
        .setEigrp(eigrpInterfaceSettings)
        .build();
    nf.interfaceBuilder()
        .setName("iface2")
        .setOwner(conf2)
        .setVrf(vrf)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.4"), 24))
        .setEigrp(eigrpInterfaceSettings)
        .build();
    vrf.addEigrpProcess(
        EigrpProcess.builder()
            .setAsNumber(1L)
            .setMode(EigrpProcessMode.NAMED)
            .setRouterId(Ip.parse("1.1.1.4"))
            .build());
    return ImmutableMap.of(conf1.getHostname(), conf1, conf2.getHostname(), conf2);
  }

  @Test
  public void testInitEigrpTopology() {
    Map<String, Configuration> configurations = configurationsForEigrpTopology();
    NodeInterfacePair conf1Iface1 = NodeInterfacePair.of("conf1", "iface1");
    NodeInterfacePair conf2Iface1 = NodeInterfacePair.of("conf2", "iface1");
    Topology layer3Topology =
        new Topology(
            ImmutableSortedSet.of(
                new Edge(conf1Iface1, conf2Iface1), new Edge(conf2Iface1, conf1Iface1)));

    EigrpTopologyUtils.initNeighborConfigs(NetworkConfigurations.of(configurations));
    EigrpTopology eigrpTopology =
        EigrpTopologyUtils.initEigrpTopology(configurations, layer3Topology);

    EigrpNeighborConfigId conf1Iface1Id = new EigrpNeighborConfigId(1L, "conf1", "iface1", "vrf");
    EigrpNeighborConfigId conf1Iface2Id = new EigrpNeighborConfigId(1L, "conf1", "iface2", "vrf");
    EigrpNeighborConfigId conf2Iface1Id = new EigrpNeighborConfigId(1L, "conf2", "iface1", "vrf");
    EigrpNeighborConfigId conf2Iface2Id = new EigrpNeighborConfigId(1L, "conf2", "iface2", "vrf");
    // assert that all four EIGRP nodes are present
    assertThat(
        eigrpTopology.getNetwork().nodes(),
        equalTo(ImmutableSet.of(conf1Iface1Id, conf1Iface2Id, conf2Iface1Id, conf2Iface2Id)));

    // only the EIGRP edge corresponding to the L3 edge is present
    assertThat(
        eigrpTopology.getNetwork().edges(),
        equalTo(
            ImmutableSet.of(
                new EigrpEdge(conf1Iface1Id, conf2Iface1Id),
                new EigrpEdge(conf2Iface1Id, conf1Iface1Id))));
  }
}
