package org.batfish.specifier;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.regex.Pattern;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

public class NameRegexInterfaceSpecifierTest {

  @Test
  public void testResolve() {

    Configuration node1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
    Configuration node2 = new Configuration("node2", ConfigurationFormat.CISCO_IOS);

    Interface iface1node1 = Interface.builder().setName("iface1").setOwner(node1).build();
    Interface iface2node1 = Interface.builder().setName("iface2").setOwner(node1).build();
    Interface iface1node2 = Interface.builder().setName("iface1").setOwner(node2).build();

    node1.setInterfaces(
        ImmutableSortedMap.of(
            iface1node1.getName(), iface1node1, iface2node1.getName(), iface2node1));
    node2.setInterfaces(ImmutableSortedMap.of(iface1node2.getName(), iface1node2));

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of(node1.getHostname(), node1, node2.getHostname(), node2))
            .build();

    assertThat(
        new NameRegexInterfaceSpecifier(Pattern.compile(".*1"))
            .resolve(ImmutableSet.of("node1"), ctxt),
        contains(NodeInterfacePair.of(iface1node1)));

    assertThat(
        new NameRegexInterfaceSpecifier(Pattern.compile("iface1.*"))
            .resolve(ImmutableSet.of("node1", "node2"), ctxt),
        containsInAnyOrder(NodeInterfacePair.of(iface1node1), NodeInterfacePair.of(iface1node2)));

    // bad regex
    assertThat(
        new NameRegexInterfaceSpecifier(Pattern.compile("iface3"))
            .resolve(ImmutableSet.of("node1"), ctxt),
        empty());
  }
}
