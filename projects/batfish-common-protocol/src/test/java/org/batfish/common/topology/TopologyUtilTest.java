package org.batfish.common.topology;

import static org.batfish.common.topology.TopologyUtil.computeLayer2Topology;
import static org.batfish.common.topology.TopologyUtil.computeLayer3Topology;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import java.util.SortedMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.junit.Before;
import org.junit.Test;

public final class TopologyUtilTest {

  private Builder _cb;
  private Interface.Builder _ib;
  private NetworkFactory _nf;
  private Vrf.Builder _vb;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _vb = _nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    _ib = _nf.interfaceBuilder().setActive(true);
  }

  @Test
  public void testComputeLayer1Topology() {
    String c1Name = "c1";
    String c2Name = "c2";
    String c1i1Name = "c1i1";
    String c1i2Name = "c1i2";
    String c2i1Name = "c2i1";
    String c2i2Name = "c2i2";

    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1);
    _ib.setName(c1i1Name).build();
    _ib.setName(c1i2Name).build();

    Configuration c2 = _cb.setHostname(c2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2);
    _ib.setName(c2i1Name).build();
    _ib.setName(c2i2Name).setActive(false).build();

    Map<String, Configuration> configurations = ImmutableMap.of(c1Name, c1, c2Name, c2);
    Layer1Topology rawLayer1Topology =
        new Layer1Topology(
            ImmutableSet.of(
                new Layer1Edge(new Layer1Node(c1Name, c1i1Name), new Layer1Node(c2Name, c2i1Name)),
                new Layer1Edge(new Layer1Node(c2Name, c2i1Name), new Layer1Node(c1Name, c1i1Name)),
                new Layer1Edge(new Layer1Node(c1Name, c1i2Name), new Layer1Node(c2Name, c2i2Name)),
                new Layer1Edge(
                    new Layer1Node(c2Name, c2i2Name), new Layer1Node(c1Name, c1i2Name))));

    // inactive c2i2 should break c1i2<=>c2i2 link
    assertThat(
        TopologyUtil.computeLayer1Topology(rawLayer1Topology, configurations).getGraph().edges(),
        containsInAnyOrder(
            new Layer1Edge(new Layer1Node(c1Name, c1i1Name), new Layer1Node(c2Name, c2i1Name)),
            new Layer1Edge(new Layer1Node(c2Name, c2i1Name), new Layer1Node(c1Name, c1i1Name))));
  }

  @Test
  public void testComputeLayer2Topology() {
    String c1Name = "c1";
    String c2Name = "c2";
    String c3Name = "c3";

    String c1i1Name = "c1i1";
    String c1i4Name = "c1i4";
    String c1i5Name = "c1i5";
    String c1i6Name = "c1i6";
    String c2i1Name = "c2i1";
    String c2i4Name = "c2i4";
    String c3i5Name = "c3i5";
    String c3i6Name = "c3i6";

    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1);
    _ib.setName(c1i1Name).build();
    Interface c1i4 = _ib.setName(c1i4Name).build();
    c1i4.setSwitchport(true);
    c1i4.setSwitchportMode(SwitchportMode.TRUNK);
    c1i4.setAllowedVlans(ImmutableList.of(new SubRange(1, 3)));
    c1i4.setNativeVlan(0);
    Interface c1i5 = _ib.setName(c1i5Name).build();
    c1i5.setSwitchport(true);
    c1i5.setSwitchportMode(SwitchportMode.ACCESS);
    c1i5.setAccessVlan(1);
    Interface c1i6 = _ib.setName(c1i6Name).build();
    c1i6.setSwitchport(true);
    c1i6.setSwitchportMode(SwitchportMode.TRUNK);
    c1i6.setNativeVlan(4);

    Configuration c2 = _cb.setHostname(c2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2);
    _ib.setName(c2i1Name).build();
    Interface c2i4 = _ib.setName(c2i4Name).build();
    c2i4.setSwitchport(true);
    c2i4.setSwitchportMode(SwitchportMode.TRUNK);
    c2i4.setAllowedVlans(ImmutableList.of(new SubRange(1, 2)));
    c2i4.setNativeVlan(0);

    Configuration c3 = _cb.setHostname(c3Name).build();
    Vrf v3 = _vb.setOwner(c3).build();
    _ib.setOwner(c3).setVrf(v3);
    _ib.setName(c3i5Name).build();
    _ib.setName(c3i6Name).build();

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.of(c1Name, c1, c2Name, c2, c3Name, c3);

    /*
     * c1i1 <=> c2i1 is non-switchport link
     *
     * c1i4 <=> c2i4 is trunk connection with:
     * - native vlan 0
     * - allowed vlans 1,2 on both c1 and c2
     * - allowed vlan 3 on c1 only
     *
     * c1i5 <=> c3i5 is link with:
     * - access vlan 1 on c1i5
     * - non-switchport interface c3i5
     *
     * c1i6 <=> c3i6 is link with:
     * - trunking with native vlan 4 on c1
     * - non-switchport on c3i6
     */
    Layer1Topology layer1Topology =
        new Layer1Topology(
            ImmutableList.<Layer1Edge>builder()
                .add(new Layer1Edge(c1Name, c1i1Name, c2Name, c2i1Name))
                .add(new Layer1Edge(c2Name, c2i1Name, c1Name, c1i1Name))
                .add(new Layer1Edge(c1Name, c1i4Name, c2Name, c2i4Name))
                .add(new Layer1Edge(c2Name, c2i4Name, c1Name, c1i4Name))
                .add(new Layer1Edge(c1Name, c1i5Name, c3Name, c3i5Name))
                .add(new Layer1Edge(c3Name, c3i5Name, c1Name, c1i5Name))
                .add(new Layer1Edge(c1Name, c1i6Name, c3Name, c3i6Name))
                .add(new Layer1Edge(c3Name, c3i6Name, c1Name, c1i6Name))
                .build());
    Layer2Topology layer2Topology = computeLayer2Topology(layer1Topology, configurations);

    // all layer1 edges should make it into layer2, plus self and transitive edges for switchports
    assertThat(
        layer2Topology.getGraph().edges(),
        containsInAnyOrder(
            // direct edges
            new Layer2Edge(c1Name, c1i1Name, null, c2Name, c2i1Name, null, null),
            new Layer2Edge(c2Name, c2i1Name, null, c1Name, c1i1Name, null, null),
            new Layer2Edge(c1Name, c1i4Name, 0, c2Name, c2i4Name, 0, null),
            new Layer2Edge(c2Name, c2i4Name, 0, c1Name, c1i4Name, 0, null),
            new Layer2Edge(c1Name, c1i4Name, 1, c2Name, c2i4Name, 1, 1),
            new Layer2Edge(c2Name, c2i4Name, 1, c1Name, c1i4Name, 1, 1),
            new Layer2Edge(c1Name, c1i4Name, 2, c2Name, c2i4Name, 2, 2),
            new Layer2Edge(c2Name, c2i4Name, 2, c1Name, c1i4Name, 2, 2),
            new Layer2Edge(c1Name, c1i5Name, 1, c3Name, c3i5Name, null, null),
            new Layer2Edge(c3Name, c3i5Name, null, c1Name, c1i5Name, 1, null),
            new Layer2Edge(c1Name, c1i6Name, 4, c3Name, c3i6Name, null, null),
            new Layer2Edge(c3Name, c3i6Name, null, c1Name, c1i6Name, 4, null),
            // internal edges
            new Layer2Edge(c1Name, c1i4Name, 1, c1Name, c1i5Name, 1, null),
            new Layer2Edge(c1Name, c1i5Name, 1, c1Name, c1i4Name, 1, null),
            // transitive edges
            new Layer2Edge(c1Name, c1i5Name, 1, c2Name, c2i4Name, 1, null),
            new Layer2Edge(c2Name, c2i4Name, 1, c1Name, c1i5Name, 1, null),
            new Layer2Edge(c1Name, c1i4Name, 1, c3Name, c3i5Name, null, null),
            new Layer2Edge(c3Name, c3i5Name, null, c1Name, c1i4Name, 1, null),
            new Layer2Edge(c2Name, c2i4Name, 1, c3Name, c3i5Name, null, null),
            new Layer2Edge(c3Name, c3i5Name, null, c2Name, c2i4Name, 1, null)));
  }

  @Test
  public void testComputeLayer3Topology() {
    String c1Name = "c1";
    String c2Name = "c2";
    String c3Name = "c3";

    String c1i1Name = "c1i1";
    String c1i2Name = "c1i2";
    String c1i3Name = "c1i3";
    String c1i4Name = "c1i4";
    String c1i5Name = "c1i5";
    String c1i6Name = "c1i6";
    String c2i1Name = "c2i1";
    String c2i2Name = "c2i2";
    String c2i3Name = "c2i3";
    String c2i4Name = "c2i4";
    String c3i5Name = "c3i5";
    String c3i6Name = "c3i6";
    String vlan1Name = "Vlan1";
    String vlan2Name = "Vlan2";
    String vlan3Name = "Vlan3";
    String vlan4Name = "Vlan4";

    Prefix p1 = Prefix.parse("10.0.0.0/31");
    Prefix p3 = Prefix.parse("10.0.0.2/31");

    int vlanPrefixLength = 24;
    Ip c1Vlan1Ip = new Ip("10.10.1.1");
    Ip c1Vlan2Ip = new Ip("10.10.2.1");
    Ip c1Vlan3Ip = new Ip("10.10.3.1");
    Ip c1Vlan4Ip = new Ip("10.10.4.1");
    Ip c2Vlan1Ip = new Ip("10.10.1.2");
    Ip c2Vlan2Ip = new Ip("10.10.2.2");
    Ip c2Vlan3Ip = new Ip("10.10.3.2");
    Ip c2Vlan4Ip = new Ip("10.10.4.2");
    Ip c3i5Ip = new Ip("10.10.1.3");
    Ip c3i6Ip = new Ip("10.10.4.3");

    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1);
    _ib.setName(c1i1Name)
        .setAddresses(new InterfaceAddress(p1.getStartIp(), p1.getPrefixLength()))
        .build();
    _ib.setName(c1i2Name).setAddresses(null).build();
    _ib.setName(c1i3Name)
        .setAddresses(new InterfaceAddress(p3.getStartIp(), p3.getPrefixLength()))
        .build();
    Interface c1i4 = _ib.setName(c1i4Name).setAddresses(null).build();
    c1i4.setSwitchport(true);
    c1i4.setSwitchportMode(SwitchportMode.TRUNK);
    c1i4.setAllowedVlans(ImmutableList.of(new SubRange(1, 3)));
    c1i4.setNativeVlan(0);
    Interface c1i5 = _ib.setName(c1i5Name).build();
    c1i5.setSwitchport(true);
    c1i5.setSwitchportMode(SwitchportMode.ACCESS);
    c1i5.setAccessVlan(1);
    Interface c1i6 = _ib.setName(c1i6Name).build();
    c1i6.setSwitchport(true);
    c1i6.setSwitchportMode(SwitchportMode.TRUNK);
    c1i6.setNativeVlan(4);
    Interface c1Vlan1 =
        _ib.setName(vlan1Name)
            .setAddresses(new InterfaceAddress(c1Vlan1Ip, vlanPrefixLength))
            .build();
    c1Vlan1.setInterfaceType(InterfaceType.VLAN);
    c1Vlan1.setVlan(1);
    Interface c1Vlan2 =
        _ib.setName(vlan2Name)
            .setAddresses(new InterfaceAddress(c1Vlan2Ip, vlanPrefixLength))
            .build();
    c1Vlan2.setInterfaceType(InterfaceType.VLAN);
    c1Vlan2.setVlan(2);
    Interface c1Vlan3 =
        _ib.setName(vlan3Name)
            .setAddresses(new InterfaceAddress(c1Vlan3Ip, vlanPrefixLength))
            .build();
    c1Vlan3.setInterfaceType(InterfaceType.VLAN);
    c1Vlan3.setVlan(3);
    Interface c1Vlan4 =
        _ib.setName(vlan4Name)
            .setAddresses(new InterfaceAddress(c1Vlan4Ip, vlanPrefixLength))
            .build();
    c1Vlan4.setInterfaceType(InterfaceType.VLAN);
    c1Vlan4.setVlan(4);

    Configuration c2 = _cb.setHostname(c2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2);
    _ib.setName(c2i1Name)
        .setAddresses(new InterfaceAddress(p1.getEndIp(), p1.getPrefixLength()))
        .build();
    _ib.setName(c2i2Name).setAddresses(null).build();
    _ib.setName(c2i3Name)
        .setAddresses(new InterfaceAddress(p3.getStartIp(), p3.getPrefixLength()))
        .build();
    Interface c2i4 = _ib.setName(c2i4Name).setAddresses(null).build();
    c2i4.setSwitchport(true);
    c2i4.setSwitchportMode(SwitchportMode.TRUNK);
    c2i4.setAllowedVlans(ImmutableList.of(new SubRange(1, 2)));
    c2i4.setNativeVlan(0);
    Interface c2Vlan1 =
        _ib.setName(vlan1Name)
            .setAddresses(new InterfaceAddress(c2Vlan1Ip, vlanPrefixLength))
            .build();
    c2Vlan1.setInterfaceType(InterfaceType.VLAN);
    c2Vlan1.setVlan(1);
    Interface c2Vlan2 =
        _ib.setName(vlan2Name)
            .setAddresses(new InterfaceAddress(c2Vlan2Ip, vlanPrefixLength))
            .build();
    c2Vlan2.setInterfaceType(InterfaceType.VLAN);
    c2Vlan2.setVlan(2);
    Interface c2Vlan3 =
        _ib.setName(vlan3Name)
            .setAddresses(new InterfaceAddress(c2Vlan3Ip, vlanPrefixLength))
            .build();
    c2Vlan3.setInterfaceType(InterfaceType.VLAN);
    c2Vlan3.setVlan(3);
    Interface c2Vlan4 =
        _ib.setName(vlan4Name)
            .setAddresses(new InterfaceAddress(c2Vlan4Ip, vlanPrefixLength))
            .build();
    c2Vlan4.setInterfaceType(InterfaceType.VLAN);
    c2Vlan4.setVlan(4);

    Configuration c3 = _cb.setHostname(c3Name).build();
    Vrf v3 = _vb.setOwner(c3).build();
    _ib.setOwner(c3).setVrf(v3);
    _ib.setName(c3i5Name).setAddresses(new InterfaceAddress(c3i5Ip, vlanPrefixLength)).build();
    _ib.setName(c3i6Name).setAddresses(new InterfaceAddress(c3i6Ip, vlanPrefixLength)).build();

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.of(c1Name, c1, c2Name, c2, c3Name, c3);

    /*
     * c1i1 <=> c2i1 is link with proper l3 addressing.
     *
     * c1i2 <=> c2i2 is link with no l3 addressing.
     *
     * c1i3 <=> c2i3 is link with same subnet but same IP (conflict).
     *
     * c1i4 <=> c2i4 is trunk connection with:
     * - native vlan 0
     * - allowed vlans 1,2 on both c1 and c2
     * - allowed vlan 3 on c1 only
     *
     * c1i5 <=> c3i5 is link with:
     * - access vlan 1 on c1i5
     * - ordinary l3 addressing on c3i5
     *
     * c1i6 <=> c3i6 is link with:
     * - trunking with native vlan 4 on c1
     * - ordinary l3 addressing on c3i6
     */

    // all layer1 edges should make it into layer2, plus self and transitive edges for switchports
    Layer2Topology layer2Topology =
        new Layer2Topology(
            ImmutableSet.of(
                // direct edges
                new Layer2Edge(c1Name, c1i1Name, null, c2Name, c2i1Name, null, null),
                new Layer2Edge(c2Name, c2i1Name, null, c1Name, c1i1Name, null, null),
                new Layer2Edge(c1Name, c1i2Name, null, c2Name, c2i2Name, null, null),
                new Layer2Edge(c2Name, c2i2Name, null, c1Name, c1i2Name, null, null),
                new Layer2Edge(c1Name, c1i3Name, null, c2Name, c2i3Name, null, null),
                new Layer2Edge(c2Name, c2i3Name, null, c1Name, c1i3Name, null, null),
                new Layer2Edge(c1Name, c1i4Name, 0, c2Name, c2i4Name, 0, null),
                new Layer2Edge(c2Name, c2i4Name, 0, c1Name, c1i4Name, 0, null),
                new Layer2Edge(c1Name, c1i4Name, 1, c2Name, c2i4Name, 1, 1),
                new Layer2Edge(c2Name, c2i4Name, 1, c1Name, c1i4Name, 1, 1),
                new Layer2Edge(c1Name, c1i4Name, 2, c2Name, c2i4Name, 2, 2),
                new Layer2Edge(c2Name, c2i4Name, 2, c1Name, c1i4Name, 2, 2),
                new Layer2Edge(c1Name, c1i5Name, 1, c3Name, c3i5Name, null, null),
                new Layer2Edge(c3Name, c3i5Name, null, c1Name, c1i5Name, 1, null),
                new Layer2Edge(c1Name, c1i6Name, 4, c3Name, c3i6Name, null, null),
                new Layer2Edge(c3Name, c3i6Name, null, c1Name, c1i6Name, 4, null),
                // internal edges
                new Layer2Edge(c1Name, c1i4Name, 1, c1Name, c1i5Name, 1, null),
                new Layer2Edge(c1Name, c1i5Name, 1, c1Name, c1i4Name, 1, null),
                // transitive edges
                new Layer2Edge(c1Name, c1i5Name, 1, c2Name, c2i4Name, 1, null),
                new Layer2Edge(c2Name, c2i4Name, 1, c1Name, c1i5Name, 1, null),
                new Layer2Edge(c1Name, c1i4Name, 1, c3Name, c3i5Name, null, null),
                new Layer2Edge(c3Name, c3i5Name, null, c1Name, c1i4Name, 1, null),
                new Layer2Edge(c2Name, c2i4Name, 1, c3Name, c3i5Name, null, null),
                new Layer2Edge(c3Name, c3i5Name, null, c2Name, c2i4Name, 1, null)));

    Layer3Topology layer3Topology = computeLayer3Topology(layer2Topology, configurations);
    // layer3 consists of layer2 interfaces with l3 addressing, plus Vlan/IRB interfaces
    assertThat(
        layer3Topology.getGraph().edges(),
        containsInAnyOrder(
            new Layer3Edge(c1Name, c1i1Name, c2Name, c2i1Name),
            new Layer3Edge(c2Name, c2i1Name, c1Name, c1i1Name),
            new Layer3Edge(c1Name, vlan1Name, c2Name, vlan1Name),
            new Layer3Edge(c2Name, vlan1Name, c1Name, vlan1Name),
            new Layer3Edge(c1Name, vlan2Name, c2Name, vlan2Name),
            new Layer3Edge(c2Name, vlan2Name, c1Name, vlan2Name),
            new Layer3Edge(c1Name, vlan1Name, c3Name, c3i5Name),
            new Layer3Edge(c3Name, c3i5Name, c1Name, vlan1Name),
            new Layer3Edge(c1Name, vlan4Name, c3Name, c3i6Name),
            new Layer3Edge(c3Name, c3i6Name, c1Name, vlan4Name),
            new Layer3Edge(c2Name, vlan1Name, c3Name, c3i5Name),
            new Layer3Edge(c3Name, c3i5Name, c2Name, vlan1Name)));
  }
}
