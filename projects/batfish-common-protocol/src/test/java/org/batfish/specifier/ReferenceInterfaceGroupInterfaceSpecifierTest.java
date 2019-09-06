package org.batfish.specifier;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.referencelibrary.InterfaceGroup;
import org.batfish.referencelibrary.ReferenceBook;
import org.junit.Test;

public class ReferenceInterfaceGroupInterfaceSpecifierTest {

  @Test
  public void resolve() {

    Configuration node1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
    Configuration node2 = new Configuration("node2", ConfigurationFormat.CISCO_IOS);

    Interface iface11 = Interface.builder().setName("iface1").setOwner(node1).build();
    Interface iface12 = Interface.builder().setName("iface2").setOwner(node1).build();
    Interface iface21 = Interface.builder().setName("iface1").setOwner(node2).build();

    node1.setInterfaces(
        ImmutableSortedMap.of(iface11.getName(), iface11, iface12.getName(), iface12));
    node2.setInterfaces(ImmutableSortedMap.of(iface21.getName(), iface21));

    ReferenceBook referenceBook =
        ReferenceBook.builder("refbook")
            .setInterfaceGroups(
                ImmutableList.of(
                    new InterfaceGroup(
                        ImmutableSortedSet.of(
                            NodeInterfacePair.of(node1.getHostname(), iface11.getName())),
                        "group")))
            .build();

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of(node1.getHostname(), node1, node2.getHostname(), node2))
            .setReferenceBooks(ImmutableSortedSet.of(referenceBook))
            .build();

    // we should only see iface11
    assertThat(
        new ReferenceInterfaceGroupInterfaceSpecifier("group", "refbook")
            .resolve(ImmutableSet.of(node1.getHostname()), ctxt),
        contains(NodeInterfacePair.of(iface11)));
  }
}
