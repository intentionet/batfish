package org.batfish.specifier;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

public class ZoneNameInterfaceSpecifierTest {

  @Test
  public void resolve() {

    Configuration node1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
    Configuration node2 = new Configuration("node2", ConfigurationFormat.CISCO_IOS);

    Zone zone1node1 = new Zone("zone1");
    Zone zone2node1 = new Zone("zone2");
    Zone zone1node2 = new Zone("zone1");

    Interface iface11 = Interface.builder().setName("iface11").setOwner(node1).build();
    Interface iface12 = Interface.builder().setName("iface12").setOwner(node1).build();
    @SuppressWarnings("unused")
    Interface iface13 = Interface.builder().setName("iface13").setOwner(node1).build();
    Interface iface2 = Interface.builder().setName("iface2").setOwner(node2).build();

    node1.setZones(ImmutableSortedMap.of("zone1", zone1node1, "zone2", zone2node1));
    zone1node1.setInterfaces(ImmutableSortedSet.of("iface11", "iface12"));
    zone2node1.setInterfaces(ImmutableSortedSet.of("iface13"));

    node2.setZones(ImmutableSortedMap.of("zone1", zone1node2));
    zone1node2.setInterfaces(ImmutableSortedSet.of("iface2"));

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of("node1", node1, "node2", node2))
            .build();

    // zone1 on node1 should only return two interfaces
    assertThat(
        new ZoneNameInterfaceSpecifier("zone1").resolve(ImmutableSet.of("node1"), ctxt),
        equalTo(ImmutableSet.of(NodeInterfacePair.of(iface11), NodeInterfacePair.of(iface12))));

    // case insensitivity
    assertThat(
        new ZoneNameInterfaceSpecifier("ZoNe1").resolve(ImmutableSet.of("node1"), ctxt),
        equalTo(ImmutableSet.of(NodeInterfacePair.of(iface11), NodeInterfacePair.of(iface12))));

    // zone1 on both nodes should only return three interfaces
    assertThat(
        new ZoneNameInterfaceSpecifier("zone1").resolve(ImmutableSet.of("node1", "node2"), ctxt),
        equalTo(
            ImmutableSet.of(
                NodeInterfacePair.of(iface11),
                NodeInterfacePair.of(iface12),
                NodeInterfacePair.of(iface2))));

    // empty set with invalid zone names
    assertThat(
        new ZoneNameInterfaceSpecifier("zone").resolve(ImmutableSet.of("node1"), ctxt),
        equalTo(ImmutableSet.of()));
    assertThat(
        new ZoneNameInterfaceSpecifier("z.*").resolve(ImmutableSet.of("node1"), ctxt),
        equalTo(ImmutableSet.of()));
  }
}
