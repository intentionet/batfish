package org.batfish.common.topology;

import static org.batfish.common.topology.TopologyUtil.computeLayer1LogicalTopology;
import static org.batfish.common.topology.TopologyUtil.computeLayer2Topology;
import static org.batfish.common.topology.TopologyUtil.computeRawLayer3Topology;
import static org.batfish.common.util.isp.IspModelingUtils.INTERNET_HOST_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.junit.Before;
import org.junit.Test;

/** Tests for correctness of L3 topology for various situations that arise in ISP modeling */
public class TopologyUtilIspTest {

  private static Configuration.Builder _cb;
  private static Interface.Builder _ib;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    _cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _ib = nf.interfaceBuilder().setType(InterfaceType.PHYSICAL);
  }

  private static class TopologySetup {

    private final int _borderCount;
    private final Map<String, Configuration> _configurations;
    private final Layer1Topology _layer1Topology;

    private static String ISP_NAME = "isp";
    private static String SPINE_NAME = "spine";

    private static String borderName(int borderRouterIndex) {
      return "border" + borderRouterIndex;
    }

    /**
     * Create a setup with the specified number of border routers. Each border router connects to
     * the "spine" and to the ISP. The ISP connects to the Internet.
     *
     * <p>The type of interface address between border-spine and border-isp is specified by the
     * user. ISP-Internet always uses LLA.
     *
     * <p>If the userL1 variable is true, L1 edges are added between border and spines. L1 edges are
     * always added for other cases (as ISP modeling will do).
     *
     * <p>In the topology, interface names are remote endpoint names.
     */
    private TopologySetup(int borderCount, boolean llaToIsp, boolean llaToSpine, boolean userL1) {
      _borderCount = borderCount;
      _configurations = new HashMap<>();
      List<Layer1Edge> l1edges = new LinkedList<>();

      _configurations.put(
          INTERNET_HOST_NAME,
          _cb.setHostname(INTERNET_HOST_NAME).setDeviceModel(DeviceModel.BATFISH_INTERNET).build());
      _configurations.put(
          ISP_NAME, _cb.setHostname(ISP_NAME).setDeviceModel(DeviceModel.BATFISH_ISP).build());
      _configurations.put(
          SPINE_NAME,
          _cb.setHostname(SPINE_NAME).setDeviceModel(DeviceModel.CISCO_UNSPECIFIED).build());

      // interfaces that connect isp and internet
      _ib.setOwner(_configurations.get(ISP_NAME))
          .setName(INTERNET_HOST_NAME)
          .setAddress(LinkLocalAddress.of(Ip.parse("169.254.0.1")))
          .build();
      _ib.setOwner(_configurations.get(INTERNET_HOST_NAME))
          .setName(ISP_NAME)
          .setAddress(LinkLocalAddress.of(Ip.parse("169.254.0.1")))
          .build();

      l1edges.add(new Layer1Edge(ISP_NAME, INTERNET_HOST_NAME, INTERNET_HOST_NAME, ISP_NAME));
      l1edges.add(new Layer1Edge(INTERNET_HOST_NAME, ISP_NAME, ISP_NAME, INTERNET_HOST_NAME));

      for (int i = 0; i < borderCount; i++) {
        _configurations.put(
            borderName(i),
            _cb.setHostname(borderName(i)).setDeviceModel(DeviceModel.CISCO_UNSPECIFIED).build());

        // border-isp address space is 1.1.{border-index}.0/30 -- e.g., 1.1.0.1/30 and 1.1.0.2/30
        // border-spine address space is 1.2.{border-index}.0/30

        // interfaces that connect the border and isp
        _ib.setOwner(_configurations.get(borderName(i)))
            .setName(ISP_NAME)
            .setAddress(makeInterfaceAddress(1, i, 1, llaToIsp))
            .build();
        _ib.setOwner(_configurations.get(ISP_NAME))
            .setName(borderName(i))
            .setAddress(makeInterfaceAddress(1, i, 2, llaToIsp))
            .build();

        // interfaces that connect the border and spine
        _ib.setOwner(_configurations.get(borderName(i)))
            .setName(SPINE_NAME)
            .setAddress(makeInterfaceAddress(2, i, 1, llaToSpine))
            .build();

        _ib.setOwner(_configurations.get(SPINE_NAME))
            .setName(borderName(i))
            .setAddress(makeInterfaceAddress(2, i, 2, llaToSpine))
            .build();

        // these L1 edges are added by ISP modeling
        l1edges.add(new Layer1Edge(borderName(i), ISP_NAME, ISP_NAME, borderName(i)));
        l1edges.add(new Layer1Edge(ISP_NAME, borderName(i), borderName(i), ISP_NAME));

        if (userL1) {
          l1edges.add(new Layer1Edge(borderName(i), SPINE_NAME, SPINE_NAME, borderName(i)));
          l1edges.add(new Layer1Edge(SPINE_NAME, borderName(i), borderName(i), SPINE_NAME));
        }
      }

      _layer1Topology = new Layer1Topology(l1edges);
    }

    private static InterfaceAddress makeInterfaceAddress(
        int ispOrSpineLayerIndicator, int borderIndex, int borderOrRemoteIndicator, boolean lla) {
      if (lla) {
        return LinkLocalAddress.of(Ip.parse("169.254.0.1"));
      }
      return ConcreteInterfaceAddress.parse(
          String.format(
              "1.%d.%d.%d/30", ispOrSpineLayerIndicator, borderIndex, borderOrRemoteIndicator));
    }

    private Edge borderToNeighborEdge(int borderIndex, String neighborName) {
      return new Edge(
          NodeInterfacePair.of(
              _configurations.get(borderName(borderIndex)).getAllInterfaces().get(neighborName)),
          NodeInterfacePair.of(
              _configurations.get(neighborName).getAllInterfaces().get(borderName(borderIndex))));
    }

    private Edge neighborToBorderEdge(int borderIndex, String neighborName) {
      return new Edge(
          NodeInterfacePair.of(
              _configurations.get(neighborName).getAllInterfaces().get(borderName(borderIndex))),
          NodeInterfacePair.of(
              _configurations.get(borderName(borderIndex)).getAllInterfaces().get(neighborName)));
    }

    private Edge ispToInternetEdge() {
      return new Edge(
          NodeInterfacePair.of(_configurations.get(ISP_NAME).getAllInterfaces().get("internet")),
          NodeInterfacePair.of(
              _configurations.get(INTERNET_HOST_NAME).getAllInterfaces().get(ISP_NAME)));
    }

    private Edge internetToIspEdge() {
      return new Edge(
          NodeInterfacePair.of(
              _configurations.get(INTERNET_HOST_NAME).getAllInterfaces().get(ISP_NAME)),
          NodeInterfacePair.of(_configurations.get(ISP_NAME).getAllInterfaces().get("internet")));
    }
  }

  /** The default way to go from topology setup to layer3 */
  private static Topology computeLayer3(TopologySetup topo) {
    Layer1Topology logicalLayer1 =
        computeLayer1LogicalTopology(topo._layer1Topology, topo._configurations);
    Layer2Topology layer2 =
        computeLayer2Topology(logicalLayer1, VxlanTopology.EMPTY, topo._configurations);
    return computeRawLayer3Topology(
        new LegacyL3Adjacencies(topo._layer1Topology, logicalLayer1, layer2, topo._configurations),
        topo._configurations);
  }

  private static SortedSet<Edge> allEdges(TopologySetup topo) {
    List<Edge> edges = new LinkedList<>();
    edges.add(topo.ispToInternetEdge());
    edges.add(topo.internetToIspEdge());
    for (int i = 0; i < topo._borderCount; i++) {
      edges.add(topo.borderToNeighborEdge(i, TopologySetup.ISP_NAME));
      edges.add(topo.neighborToBorderEdge(i, TopologySetup.ISP_NAME));
      edges.add(topo.borderToNeighborEdge(i, TopologySetup.SPINE_NAME));
      edges.add(topo.neighborToBorderEdge(i, TopologySetup.SPINE_NAME));
    }
    return ImmutableSortedSet.copyOf(edges);
  }

  @Test
  public void testComputeRawLayer3TopologyAllConcrete() {
    {
      // without user L1
      TopologySetup topo = new TopologySetup(1, false, false, false);
      assertThat(computeLayer3(topo).getEdges(), equalTo(allEdges(topo)));
    }
    {
      // with the L1
      TopologySetup topo = new TopologySetup(1, false, false, true);
      assertThat(computeLayer3(topo).getEdges(), equalTo(allEdges(topo)));
    }
  }

  @Test
  public void testComputeRawLayer3TopologyAllLla() {
    {
      // without user L1. No border-spine edges.
      TopologySetup topo = new TopologySetup(1, true, true, false);
      assertThat(
          computeLayer3(topo).getEdges(),
          equalTo(
              ImmutableSortedSet.of(
                  topo.internetToIspEdge(),
                  topo.ispToInternetEdge(),
                  topo.borderToNeighborEdge(0, TopologySetup.ISP_NAME),
                  topo.neighborToBorderEdge(0, TopologySetup.ISP_NAME))));
    }
    {
      // with the L1. full topology.
      TopologySetup topo = new TopologySetup(1, true, true, true);
      assertThat(computeLayer3(topo).getEdges(), equalTo(allEdges(topo)));
    }
  }

  @Test
  public void testComputeRawLayer3TopologyLlaIspConcreteSpine() {
    {
      // without user L1.
      TopologySetup topo = new TopologySetup(1, true, false, false);
      assertThat(computeLayer3(topo).getEdges(), equalTo(allEdges(topo)));
    }
    {
      // with the L1.
      TopologySetup topo = new TopologySetup(1, true, true, true);
      assertThat(computeLayer3(topo).getEdges(), equalTo(allEdges(topo)));
    }
  }

  @Test
  public void testComputeRawLayer3TopologyConcreteIspLlaSpine() {
    {
      // without user L1. No border-spine edges.
      TopologySetup topo = new TopologySetup(1, false, true, false);
      assertThat(
          computeLayer3(topo).getEdges(),
          equalTo(
              ImmutableSortedSet.of(
                  topo.internetToIspEdge(),
                  topo.ispToInternetEdge(),
                  topo.borderToNeighborEdge(0, TopologySetup.ISP_NAME),
                  topo.neighborToBorderEdge(0, TopologySetup.ISP_NAME))));
    }
    {
      // with the L1. full topology.
      TopologySetup topo = new TopologySetup(1, false, true, true);
      assertThat(computeLayer3(topo).getEdges(), equalTo(allEdges(topo)));
    }
  }

  /**
   * Tests the case of two borders that should be connected do get connected based in L3 inference.
   */
  @Test
  public void testComputeRawLayer3TopologyTwoBorders() {

    TopologySetup topo = new TopologySetup(2, false, false, false);

    String borderName0 = TopologySetup.borderName(0);
    String borderName1 = TopologySetup.borderName(1);

    // add interfaces to the two borders
    Interface i01 =
        _ib.setOwner(topo._configurations.get(borderName0))
            .setName(borderName1)
            .setAddress(ConcreteInterfaceAddress.parse("3.1.1.1/30"))
            .build();
    Interface i10 =
        _ib.setOwner(topo._configurations.get(borderName1))
            .setName(borderName0)
            .setAddress(ConcreteInterfaceAddress.parse("3.1.1.2/30"))
            .build();

    Set<Edge> expectedSet =
        ImmutableSet.<Edge>builder()
            .addAll(allEdges(topo))
            .add(
                new Edge(NodeInterfacePair.of(i01), NodeInterfacePair.of(i10)),
                new Edge(NodeInterfacePair.of(i10), NodeInterfacePair.of(i01)))
            .build();

    assertThat(computeLayer3(topo).getEdges(), equalTo(ImmutableSortedSet.copyOf(expectedSet)));
  }
}
