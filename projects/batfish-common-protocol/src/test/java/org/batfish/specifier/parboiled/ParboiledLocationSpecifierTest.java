package org.batfish.specifier.parboiled;

import static org.batfish.specifier.Location.interfaceLinkLocation;
import static org.batfish.specifier.Location.interfaceLocation;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.batfish.common.util.isp.IspModelingUtils;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.MockSpecifierContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParboiledLocationSpecifierTest {

  private static final String _node1 = "node1";
  private static final String _node2 = "node2";
  private static final String _iface1 = "eth1";
  private static final String _iface2 = "eth2";
  private static final MockSpecifierContext _ctxt;

  static {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    Configuration n1 = cb.setHostname(_node1).build();
    nf.interfaceBuilder().setOwner(n1).setName(_iface1).build();
    nf.interfaceBuilder().setOwner(n1).setName(_iface2).build();

    // make the address different because interface equality does not consider owners
    Configuration n2 = cb.setHostname(_node2).build();
    nf.interfaceBuilder()
        .setOwner(n2)
        .setName(_iface1)
        .setAddress(ConcreteInterfaceAddress.parse("3.3.3.3/30"))
        .build();

    _ctxt =
        MockSpecifierContext.builder().setConfigs(ImmutableMap.of(_node1, n1, _node2, n2)).build();
  }

  @Test
  public void testResolveDifference() {
    assertThat(
        new ParboiledLocationSpecifier(
                new DifferenceLocationAstNode(
                    InterfaceLocationAstNode.createFromNode(_node1),
                    InterfaceLocationAstNode.createFromInterface(
                        new NameInterfaceAstNode(_iface1))))
            .resolve(_ctxt),
        equalTo(ImmutableSet.of(new InterfaceLocation(_node1, _iface2))));
  }

  @Test
  public void testResolveIntersection() {
    assertThat(
        new ParboiledLocationSpecifier(
                new IntersectionLocationAstNode(
                    InterfaceLocationAstNode.createFromNode(_node1),
                    InterfaceLocationAstNode.createFromInterface(
                        new NameInterfaceAstNode(_iface1))))
            .resolve(_ctxt),
        equalTo(ImmutableSet.of(new InterfaceLocation(_node1, _iface1))));
  }

  @Test
  public void testResolveInterface() {
    assertThat(
        new ParboiledLocationSpecifier(
                InterfaceLocationAstNode.createFromInterface(new NameInterfaceAstNode(_iface1)))
            .resolve(_ctxt),
        equalTo(
            ImmutableSet.of(
                new InterfaceLocation(_node1, _iface1), new InterfaceLocation(_node2, _iface1))));
  }

  @Test
  public void testResolveNode() {
    assertThat(
        new ParboiledLocationSpecifier(InterfaceLocationAstNode.createFromNode(_node1))
            .resolve(_ctxt),
        equalTo(
            ImmutableSet.of(
                new InterfaceLocation(_node1, _iface1), new InterfaceLocation(_node1, _iface2))));
  }

  @Test
  public void testResolveNodeInterface() {
    assertThat(
        new ParboiledLocationSpecifier(
                InterfaceLocationAstNode.createFromInterfaceWithNode(
                    new NameNodeAstNode(_node1), new NameInterfaceAstNode(_iface1)))
            .resolve(_ctxt),
        equalTo(ImmutableSet.of(new InterfaceLocation(_node1, _iface1))));
  }

  @Test
  public void testResolveUnion() {
    assertThat(
        new ParboiledLocationSpecifier(
                new UnionLocationAstNode(
                    InterfaceLocationAstNode.createFromNode(_node1),
                    InterfaceLocationAstNode.createFromInterface(
                        new NameInterfaceAstNode(_iface1))))
            .resolve(_ctxt),
        equalTo(
            ImmutableSet.of(
                new InterfaceLocation(_node1, _iface1),
                new InterfaceLocation(_node1, _iface2),
                new InterfaceLocation(_node2, _iface1))));
  }

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testParseBadInput() {
    _thrown.expect(IllegalArgumentException.class);
    _thrown.expectMessage("Error parsing");
    ParboiledLocationSpecifier.parse("@connected");
  }

  @Test
  public void testParseGoodInput() {
    assertThat(
        ParboiledLocationSpecifier.parse("node0"),
        equalTo(new ParboiledLocationSpecifier(InterfaceLocationAstNode.createFromNode("node0"))));
  }

  @Test
  public void testParseInternet() {
    assertThat(
        ParboiledLocationSpecifier.parse("internet"),
        equalTo(new ParboiledLocationSpecifier(InternetLocationAstNode.INSTANCE)));
  }

  @Test
  public void testParseInternet_noInternet() {
    assertThat(ParboiledLocationSpecifier.parse("internet").resolve(_ctxt), empty());
  }

  @Test
  public void testParseInternet_batfishInternet() {
    NetworkFactory nf = new NetworkFactory();

    Configuration inet =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(IspModelingUtils.INTERNET_HOST_NAME)
            .setDeviceModel(DeviceModel.BATFISH_INTERNET)
            .build();
    Interface i1 =
        nf.interfaceBuilder()
            .setOwner(inet)
            .setName(IspModelingUtils.INTERNET_OUT_INTERFACE)
            .build();

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of(inet.getHostname(), inet))
            .build();

    assertThat(
        ParboiledLocationSpecifier.parse("internet").resolve(ctxt),
        contains(interfaceLinkLocation(i1)));
  }

  @Test
  public void testParseInternet_nonBatfishInternet() {
    NetworkFactory nf = new NetworkFactory();

    Configuration inet =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(IspModelingUtils.INTERNET_HOST_NAME)
            .build();
    Interface i1 = nf.interfaceBuilder().setOwner(inet).setName(_iface1).build();

    MockSpecifierContext ctxt =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of(inet.getHostname(), inet))
            .build();

    assertThat(
        ParboiledLocationSpecifier.parse("internet").resolve(ctxt),
        contains(interfaceLocation(i1)));
  }
}
