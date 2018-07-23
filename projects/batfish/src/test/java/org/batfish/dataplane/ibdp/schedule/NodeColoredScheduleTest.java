package org.batfish.dataplane.ibdp.schedule;

import static org.batfish.common.util.CommonUtil.computeIpNodeOwners;
import static org.batfish.common.util.CommonUtil.initBgpTopology;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.ValueGraph;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.dataplane.ibdp.Node;
import org.batfish.dataplane.ibdp.TestUtils;
import org.batfish.dataplane.ibdp.schedule.NodeColoredSchedule.Coloring;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link org.batfish.dataplane.ibdp.schedule.NodeColoredSchedule} */
@RunWith(Parameterized.class)
public class NodeColoredScheduleTest {

  @Parameters
  public static Collection<Object[]> data() {
    return ImmutableList.copyOf(
        new Coloring[][] {{Coloring.SATURATION}, {Coloring.GREEDY}, {Coloring.RANDOM}});
  }

  @Parameter public Coloring _coloring;

  @Test
  public void testSinglenodeColoring() {
    Node n = TestUtils.makeIosRouter("r1");
    Map<String, Node> nodes = ImmutableMap.of("r1", n);
    Map<String, Configuration> configs = ImmutableMap.of("r1", n.getConfiguration());
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(configs, computeIpNodeOwners(configs, false), false);
    NodeColoredSchedule schedule = new NodeColoredSchedule(nodes, _coloring, bgpTopology);

    assertThat(schedule.hasNext(), is(true));
    assertThat(schedule.next(), equalTo(nodes));
    assertThat(schedule.hasNext(), is(false));
  }

  @Test
  public void testTwoNodeColoringDisconnected() {
    Map<String, Node> nodes =
        ImmutableMap.of("r1", TestUtils.makeIosRouter("r1"), "r2", TestUtils.makeIosRouter("r2"));

    Map<String, Configuration> configs =
        nodes
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getConfiguration()));
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(configs, computeIpNodeOwners(configs, false), false);
    NodeColoredSchedule schedule = new NodeColoredSchedule(nodes, _coloring, bgpTopology);

    // Expect both nodes to have the same color because there is no edge between them
    assertThat(schedule.hasNext(), is(true));
    assertThat(schedule.next(), allOf(hasKey("r1"), hasKey("r2")));
    assertThat(schedule.hasNext(), is(false));
  }

  @Test
  public void testTwoNodesConnectedDirectly() {
    // Init BGP processes
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder();
    Interface.Builder ib = nf.interfaceBuilder();
    BgpProcess.Builder pb = nf.bgpProcessBuilder();
    BgpActivePeerConfig.Builder nb = nf.bgpNeighborBuilder();

    Configuration r1 = cb.setHostname("r1").build();
    Vrf vEdge1 = vb.setOwner(r1).build();
    ib.setOwner(r1).setVrf(vEdge1).setActive(true);
    ib.setAddress(new InterfaceAddress(new Ip("1.1.1.1"), 32)).build();
    BgpProcess r1Proc = pb.setRouterId(new Ip("1.1.1.1")).setVrf(vEdge1).build();
    nb.setRemoteAs(2L)
        .setPeerAddress(new Ip("2.2.2.2"))
        .setBgpProcess(r1Proc)
        .setLocalAs(1L)
        .setLocalIp(new Ip("1.1.1.1"))
        .build();

    Configuration r2 = cb.setHostname("r2").build();
    Vrf vrf2 = vb.setOwner(r2).build();
    ib.setOwner(r2).setVrf(vrf2);
    ib.setAddress(new InterfaceAddress(new Ip("2.2.2.2"), 32)).build();
    BgpProcess r2Proc = pb.setRouterId(new Ip("2.2.2.2")).setVrf(vrf2).build();
    nb.setRemoteAs(1L)
        .setPeerAddress(new Ip("1.1.1.1"))
        .setBgpProcess(r2Proc)
        .setLocalAs(2L)
        .setRouteReflectorClient(true)
        .setLocalIp(new Ip("2.2.2.2"))
        .build();

    SortedMap<String, Configuration> configurations =
        new ImmutableSortedMap.Builder<String, Configuration>(String::compareTo)
            .put(r1.getHostname(), r1)
            .put(r2.getHostname(), r2)
            .build();

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(configurations, computeIpNodeOwners(configurations, false), false);
    ImmutableMap<String, Node> nodes = ImmutableMap.of("r1", new Node(r1), "r2", new Node(r2));

    NodeColoredSchedule schedule = new NodeColoredSchedule(nodes, _coloring, bgpTopology);
    ImmutableList<Map<String, Node>> coloredNodes =
        ImmutableList.copyOf(schedule.getAllRemaining());
    // 2 separate colors because of direct connection.
    assertThat(coloredNodes, hasSize(2));
  }
}
