package org.batfish.specifier;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.regex.Pattern;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

public class VrfNameRegexInterfaceSpecifierTest {

  @Test
  public void resolve() {

    Configuration node1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
    Configuration node2 = new Configuration("node2", ConfigurationFormat.CISCO_IOS);

    Vrf vrf1 = new Vrf("vrf1");
    Vrf vrf2 = new Vrf("vrf2");
    Vrf vrf3 = new Vrf("vrf2");

    Interface iface11 = Interface.builder().setName("iface11").setOwner(node1).setVrf(vrf1).build();
    Interface iface12 = Interface.builder().setName("iface12").setOwner(node1).setVrf(vrf2).build();
    Interface iface2 = Interface.builder().setName("iface2").setOwner(node2).setVrf(vrf3).build();

    node1.setVrfs(ImmutableSortedMap.of("vrf1", vrf1, "vrf2", vrf2));
    vrf1.setInterfaces(ImmutableSortedMap.of("iface11", iface11));
    vrf2.setInterfaces(ImmutableSortedMap.of("iface12", iface12));

    node2.setVrfs(ImmutableSortedMap.of("vrf3", vrf3));
    vrf3.setInterfaces(ImmutableSortedMap.of("iface2", iface2));

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of("node1", node1, "node2", node2))
            .build();

    assertThat(
        new VrfNameRegexInterfaceSpecifier(Pattern.compile("vrf1"))
            .resolve(ImmutableSet.of("node1"), ctxt),
        equalTo(ImmutableSet.of(NodeInterfacePair.of(iface11))));
  }
}
