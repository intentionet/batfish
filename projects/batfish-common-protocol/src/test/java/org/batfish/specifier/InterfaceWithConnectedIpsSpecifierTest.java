package org.batfish.specifier;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.specifier.InterfaceWithConnectedIpsSpecifier.Factory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link InterfaceWithConnectedIpsSpecifier}. */
public class InterfaceWithConnectedIpsSpecifierTest {

  @Rule public final ExpectedException _exception = ExpectedException.none();

  @Test
  public void testLoadNull() {
    Factory f = new Factory();

    _exception.expect(IllegalArgumentException.class);
    _exception.expectMessage("wildcard provided as a string input, not null");
    f.buildInterfaceSpecifier(null);
  }

  @Test
  public void testLoadNotString() {
    Factory f = new Factory();

    _exception.expect(IllegalArgumentException.class);
    _exception.expectMessage("wildcard provided as a string input, not 3");
    f.buildInterfaceSpecifier(3);
  }

  @Test
  public void testLoadNotIpSpace() {
    Factory f = new Factory();

    _exception.expect(IllegalArgumentException.class);
    _exception.expectMessage("Invalid IPv4 address: 45.6");
    f.buildInterfaceSpecifier("45.6");
  }

  @Test
  public void testResolve() {
    Configuration node1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
    Interface iface11 =
        Interface.builder()
            .setName("iface11")
            .setOwner(node1)
            .setAddress(ConcreteInterfaceAddress.parse("1.2.3.4/24"))
            .setActive(true)
            .build();
    Interface iface12 =
        Interface.builder()
            .setName("iface12")
            .setActive(true)
            .setOwner(node1)
            .setAddress(ConcreteInterfaceAddress.parse("2.3.4.5/24"))
            .build();
    node1.setInterfaces(ImmutableSortedMap.of("iface11", iface11, "iface12", iface12));

    Configuration node2 = new Configuration("node2", ConfigurationFormat.CISCO_IOS);
    Interface iface2 =
        Interface.builder()
            .setName("iface2")
            .setActive(true)
            .setOwner(node2)
            .setAddress(ConcreteInterfaceAddress.parse("1.2.3.5/30"))
            .build();
    node2.setInterfaces(ImmutableSortedMap.of("iface2", iface2));

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of("node1", node1, "node2", node2))
            .build();

    assertThat(
        new InterfaceWithConnectedIpsSpecifier(Ip.parse("1.2.3.4").toIpSpace())
            .resolve(ImmutableSet.of("node1", "node2"), ctxt),
        equalTo(ImmutableSet.of(NodeInterfacePair.of(iface11), NodeInterfacePair.of(iface2))));

    assertThat(
        new InterfaceWithConnectedIpsSpecifier(Ip.parse("1.2.3.8").toIpSpace())
            .resolve(ImmutableSet.of("node1", "node2"), ctxt),
        equalTo(ImmutableSet.of(NodeInterfacePair.of(iface11))));

    assertThat(
        new InterfaceWithConnectedIpsSpecifier(Ip.parse("2.3.4.5").toIpSpace())
            .resolve(ImmutableSet.of("node1", "node2"), ctxt),
        equalTo(ImmutableSet.of(NodeInterfacePair.of(iface12))));

    assertThat(
        new InterfaceWithConnectedIpsSpecifier(Prefix.parse("3.0.0.0/24").toIpSpace())
            .resolve(ImmutableSet.of("node1", "node2"), ctxt),
        empty());
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new InterfaceWithConnectedIpsSpecifier(Ip.parse("1.2.3.4").toIpSpace()),
            new InterfaceWithConnectedIpsSpecifier(Ip.parse("1.2.3.4").toIpSpace()))
        .addEqualityGroup(new InterfaceWithConnectedIpsSpecifier(Ip.parse("1.2.3.5").toIpSpace()))
        .testEquals();
  }
}
