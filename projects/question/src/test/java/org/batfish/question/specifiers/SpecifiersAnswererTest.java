package org.batfish.question.specifiers;

import static org.batfish.question.specifiers.SpecifiersAnswerer.COL_FILTER_NAME;
import static org.batfish.question.specifiers.SpecifiersAnswerer.COL_INTERFACE;
import static org.batfish.question.specifiers.SpecifiersAnswerer.COL_IP_SPACE;
import static org.batfish.question.specifiers.SpecifiersAnswerer.COL_LOCATION;
import static org.batfish.question.specifiers.SpecifiersAnswerer.COL_LOCATIONS;
import static org.batfish.question.specifiers.SpecifiersAnswerer.COL_NODE;
import static org.batfish.question.specifiers.SpecifiersAnswerer.resolveFilter;
import static org.batfish.question.specifiers.SpecifiersAnswerer.resolveInterface;
import static org.batfish.question.specifiers.SpecifiersAnswerer.resolveIpSpace;
import static org.batfish.question.specifiers.SpecifiersAnswerer.resolveIpSpaceOfLocation;
import static org.batfish.question.specifiers.SpecifiersAnswerer.resolveLocation;
import static org.batfish.question.specifiers.SpecifiersAnswerer.resolveNode;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.Row;
import org.batfish.question.specifiers.SpecifiersQuestion.QueryType;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierFactories;
import org.batfish.specifier.SpecifierFactories.Version;
import org.junit.Before;
import org.junit.Test;

/** End-to-end tests of {@link SpecifiersAnswerer}. */
public class SpecifiersAnswererTest {

  Configuration _c1;
  Configuration _c2;
  IpAccessList _filter1;
  IpAccessList _filter2;
  Interface _iface1;
  Interface _iface2;
  SpecifierContext _context;

  /** Set up a network with two nodes, each with one interface and one filter */
  @Before
  public void initNetwork() {
    NetworkFactory nf = new NetworkFactory();
    _c1 =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("c1")
            .build();
    _iface1 =
        nf.interfaceBuilder()
            .setOwner(_c1)
            .setName("iface1")
            .setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/24"))
            .build();
    _c1.setInterfaces(ImmutableSortedMap.of(_iface1.getName(), _iface1));
    _filter1 = IpAccessList.builder().setName("filter1").setOwner(_c1).build();
    _c1.setIpAccessLists(ImmutableSortedMap.of(_filter1.getName(), _filter1));

    _c2 =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("c2")
            .build();
    _iface2 =
        nf.interfaceBuilder()
            .setOwner(_c2)
            .setName("iface2")
            .setAddress(ConcreteInterfaceAddress.parse("2.2.2.2/24"))
            .build();
    _c2.setInterfaces(ImmutableSortedMap.of(_iface2.getName(), _iface2));
    _filter2 = IpAccessList.builder().setName("filter2").setOwner(_c2).build();
    _c2.setIpAccessLists(ImmutableSortedMap.of(_filter2.getName(), _filter2));

    _context =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableSortedMap.of(_c1.getHostname(), _c1, _c2.getHostname(), _c2))
            .setInterfaceOwnedIps(
                ImmutableMap.of(
                    _c1.getHostname(),
                    ImmutableMap.of(
                        _iface1.getName(), _iface1.getConcreteAddress().getIp().toIpSpace()),
                    _c2.getHostname(),
                    ImmutableMap.of(
                        _iface2.getName(), _iface2.getConcreteAddress().getIp().toIpSpace())))
            .build();
  }

  @Test
  public void resolveFilterTest() {
    SpecifiersQuestion question = new SpecifiersQuestion(QueryType.FILTER);
    question.setFilterSpecifierInput(_filter1.getName());

    // only filter1 should be present (with a node specifier all nodes are included)
    assertThat(
        resolveFilter(question, _context).getRows().getData(),
        equalTo(
            ImmutableMultiset.of(
                Row.of(
                    COL_NODE, new Node(_c1.getHostname()), COL_FILTER_NAME, _filter1.getName()))));

    // nothing should match since the node specifier does not match anything
    question.setNodeSpecifierInput("foofoo");
    assertThat(
        resolveFilter(question, _context).getRows().getData(), equalTo(ImmutableMultiset.of()));
  }

  @Test
  public void resolveInterfaceTest() {
    SpecifiersQuestion question = new SpecifiersQuestion(QueryType.INTERFACE);
    question.setInterfaceSpecifierInput(_iface1.getName());

    // only iface1 should be present (with a node specifier all nodes are included)
    assertThat(
        resolveInterface(question, _context).getRows().getData(),
        equalTo(
            ImmutableMultiset.of(
                Row.of(
                    COL_INTERFACE, NodeInterfacePair.of(_c1.getHostname(), _iface1.getName())))));

    // nothing should match since the node specifier does not match anything
    question.setNodeSpecifierInput("foofoo");
    assertThat(
        resolveInterface(question, _context).getRows().getData(), equalTo(ImmutableMultiset.of()));
  }

  @Test
  public void resolveIpSpaceTest() {
    String prefix = "3.3.3.3/24";

    SpecifiersQuestion questionWithIp = new SpecifiersQuestion(QueryType.LOCATION);
    questionWithIp.setIpSpaceSpecifierInput(prefix);

    // both interface locations should be mapped to 3.3.3.3/24
    assertThat(
        resolveIpSpace(questionWithIp, _context).getRows().getData(),
        equalTo(
            ImmutableMultiset.of(
                Row.of(
                    COL_IP_SPACE,
                    SpecifierFactories.ACTIVE_VERSION == Version.V1
                        ? IpWildcardSetIpSpace.builder()
                            .including(IpWildcard.parse(prefix))
                            .build()
                            .toString()
                        : Prefix.parse(prefix).toIpSpace().toString()))));
  }

  @Test
  public void resolveIpSpaceOfLocationTest() {
    SpecifiersQuestion questionWithLocation =
        new SpecifiersQuestion(QueryType.IP_SPACE_OF_LOCATION);
    questionWithLocation.setLocationSpecifierInput(_c1.getHostname());

    // only c1:iface1 should be present
    assertThat(
        resolveIpSpaceOfLocation(questionWithLocation, _context).getRows().getData(),
        equalTo(
            ImmutableMultiset.of(
                Row.of(
                    COL_LOCATIONS,
                    ImmutableSet.of(new InterfaceLocation(_c1.getHostname(), _iface1.getName()))
                        .toString(),
                    COL_IP_SPACE,
                    _iface1.getConcreteAddress().getIp().toIpSpace().toString()))));
  }

  @Test
  public void resolveIpSpaceOfLocationTestDefault() {
    SpecifiersQuestion question = new SpecifiersQuestion(QueryType.IP_SPACE);

    assertThat(
        resolveIpSpaceOfLocation(question, _context).getRows().getData(),
        equalTo(
            ImmutableMultiset.of(
                Row.of(
                    COL_LOCATIONS,
                    ImmutableSet.of(new InterfaceLocation(_c1.getHostname(), _iface1.getName()))
                        .toString(),
                    COL_IP_SPACE,
                    _iface1.getConcreteAddress().getIp().toIpSpace().toString()),
                Row.of(
                    COL_LOCATIONS,
                    ImmutableSet.of(new InterfaceLocation(_c2.getHostname(), _iface2.getName()))
                        .toString(),
                    COL_IP_SPACE,
                    _iface2.getConcreteAddress().getIp().toIpSpace().toString()))));
  }

  @Test
  public void resolveLocationTest() {
    SpecifiersQuestion question = new SpecifiersQuestion(QueryType.LOCATION);
    question.setLocationSpecifierInput(_c1.getHostname());

    // only c1:iface1 should be present
    assertThat(
        resolveLocation(question, _context).getRows().getData(),
        equalTo(
            ImmutableMultiset.of(
                Row.of(
                    COL_LOCATION,
                    new InterfaceLocation(_c1.getHostname(), _iface1.getName()).toString()))));
  }

  @Test
  public void resolveNodeTest() {
    SpecifiersQuestion question = new SpecifiersQuestion(QueryType.NODE);
    question.setNodeSpecifierInput(_c1.getHostname());

    // only c1 should be present
    assertThat(
        resolveNode(question, _context).getRows().getData(),
        equalTo(ImmutableMultiset.of(Row.of(COL_NODE, new Node(_c1.getHostname())))));
  }
}
