package org.batfish.specifier.parboiled;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.referencelibrary.InterfaceGroup;
import org.batfish.referencelibrary.ReferenceBook;
import org.batfish.specifier.MockSpecifierContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParboiledInterfaceSpecifierTest {

  // Set up a basic network with two nodes and three interfaces.
  private Configuration _node1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
  private Configuration _node2 = new Configuration("node2", ConfigurationFormat.CISCO_IOS);
  private Set<String> _nodes = ImmutableSet.of("node1", "node2");

  // Leave the interfaces in builder form, so tests can add to it as needed
  private Interface.Builder _iface11B = Interface.builder().setName("iface11").setOwner(_node1);
  private Interface.Builder _iface12B = Interface.builder().setName("iface12").setOwner(_node1);
  private Interface.Builder _iface2B = Interface.builder().setName("iface2").setOwner(_node2);

  // Leave the context in builder form too
  private MockSpecifierContext.Builder _ctxtB =
      MockSpecifierContext.builder().setConfigs(ImmutableMap.of("node1", _node1, "node2", _node2));

  // The builders are realized into these variables once build() is called
  private Interface _iface11;
  private Interface _iface12;
  private Interface _iface2;
  private MockSpecifierContext _ctxt;

  // This function builds things; it should be called within each test after building is complete
  private void build() {
    _iface11 = _iface11B.build();
    _iface12 = _iface12B.build();
    _iface2 = _iface2B.build();
    _ctxt = _ctxtB.build();
  }

  @Test
  public void testResolveDifference() {
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(
                new DifferenceInterfaceAstNode(
                    new NameRegexInterfaceAstNode("iface1.*"), new NameInterfaceAstNode("iface11")))
            .resolve(_nodes, _ctxt),
        equalTo(ImmutableSet.of(NodeInterfacePair.of(_iface12))));
    assertThat(
        new ParboiledInterfaceSpecifier(
                new DifferenceInterfaceAstNode(
                    new NameInterfaceAstNode("iface11"), new NameRegexInterfaceAstNode("iface1.*")))
            .resolve(_nodes, _ctxt),
        equalTo(ImmutableSet.of()));
  }

  @Test
  public void testResolveInterfaceGroup() {
    String interfaceGroup = "ig";
    String book = "book";
    _ctxtB.setReferenceBooks(
        ImmutableSortedSet.of(
            ReferenceBook.builder(book)
                .setInterfaceGroups(
                    ImmutableList.of(
                        new InterfaceGroup(
                            ImmutableSortedSet.of(NodeInterfacePair.of("node1", "iface11")),
                            interfaceGroup)))
                .build()));
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(new InterfaceGroupInterfaceAstNode(book, interfaceGroup))
            .resolve(_nodes, _ctxt),
        contains(NodeInterfacePair.of(_iface11)));

    // reverse is also the same
    assertThat(
        new ParboiledInterfaceSpecifier(new InterfaceGroupInterfaceAstNode(interfaceGroup, book))
            .resolve(_nodes, _ctxt),
        contains(NodeInterfacePair.of(_iface11)));
  }

  @Test
  public void testResolveInterfaceWithNode() {
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(
                new InterfaceWithNodeInterfaceAstNode(
                    new NameNodeAstNode(_node1.getHostname()),
                    new NameRegexInterfaceAstNode(".*2")))
            .resolve(_nodes, _ctxt),
        contains(NodeInterfacePair.of(_iface12)));
  }

  @Test
  public void testResolveIntersection() {
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(
                new IntersectionInterfaceAstNode(
                    new NameRegexInterfaceAstNode("iface1.*"), new NameInterfaceAstNode("iface11")))
            .resolve(_nodes, _ctxt),
        contains(NodeInterfacePair.of(_iface11)));
    assertThat(
        new ParboiledInterfaceSpecifier(
                new IntersectionInterfaceAstNode(
                    new NameInterfaceAstNode("iface2"), new NameRegexInterfaceAstNode("iface1.*")))
            .resolve(_nodes, _ctxt),
        empty());
  }

  @Test
  public void testResolveName() {
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(new NameInterfaceAstNode("iface11")).resolve(_nodes, _ctxt),
        contains(NodeInterfacePair.of(_iface11)));
    // This regex looking name below should not match anything
    assertThat(
        new ParboiledInterfaceSpecifier(new NameInterfaceAstNode("iface1.*"))
            .resolve(_nodes, _ctxt),
        empty());
  }

  @Test
  public void testResolveNameRegex() {
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(new NameRegexInterfaceAstNode("iface.*2"))
            .resolve(_nodes, _ctxt),
        containsInAnyOrder(NodeInterfacePair.of(_iface12), NodeInterfacePair.of(_iface2)));
  }

  @Test
  public void testResolveType() {
    _iface11B.setType(InterfaceType.VLAN);
    _iface12B.setType(InterfaceType.VPN);
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(new TypeInterfaceAstNode("vpn")).resolve(_nodes, _ctxt),
        contains(NodeInterfacePair.of(_iface12)));
  }

  @Test
  public void testResolveUnion() {
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(
                new UnionInterfaceAstNode(
                    new NameInterfaceAstNode("iface11"), new NameInterfaceAstNode("iface2")))
            .resolve(_nodes, _ctxt),
        containsInAnyOrder(NodeInterfacePair.of(_iface11), NodeInterfacePair.of(_iface2)));
  }

  @Test
  public void testResolveVrf() {
    Vrf vrf1 = new Vrf("vrf1");
    _iface11B.setVrf(vrf1);
    build();
    vrf1.setInterfaces(ImmutableSortedMap.of("iface11", _iface11));
    _node1.setVrfs(ImmutableMap.of("node1", vrf1));
    assertThat(
        new ParboiledInterfaceSpecifier(new VrfInterfaceAstNode("vrf1")).resolve(_nodes, _ctxt),
        contains(NodeInterfacePair.of(_iface11)));
  }

  @Test
  public void testResolveZone() {
    Zone zone1 = new Zone("zone1");
    _node1.setZones(ImmutableSortedMap.of("zone1", zone1));
    _node2.setZones(ImmutableSortedMap.of("zone1", zone1));
    zone1.setInterfaces(ImmutableSet.of("iface11", "iface2"));
    build();
    assertThat(
        new ParboiledInterfaceSpecifier(new ZoneInterfaceAstNode("zone1")).resolve(_nodes, _ctxt),
        containsInAnyOrder(NodeInterfacePair.of(_iface11), NodeInterfacePair.of(_iface2)));
  }

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testParseBadInput() {
    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage("Error parsing");
    ParboiledInterfaceSpecifier.parse("@connected");
  }

  @Test
  public void testParseGoodInput() {
    assertThat(
        ParboiledInterfaceSpecifier.parse("eth0"),
        equalTo(new ParboiledInterfaceSpecifier(new NameInterfaceAstNode("eth0"))));
  }
}
