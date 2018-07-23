package org.batfish.specifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.role.NodeRole;
import org.junit.Test;

/** Tests for built-in location specifiers. */
public class LocationSpecifierTest {
  private static final Map<String, Configuration> _testConfigs;
  private static final List<Interface> _testConfigsInterfaces;
  private static final SpecifierContext _context;
  private static final String _roleDim;
  private static final Configuration _roleNode;
  private static final Pattern _rolePattern;

  static {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb = nf.configurationBuilder();
    cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder();
    Interface.Builder ib = nf.interfaceBuilder();

    Configuration n1 = cb.build();
    vb.setOwner(n1);
    ib.setOwner(n1);

    Vrf vrf1 = vb.build();
    Vrf vrf2 = vb.build();

    Interface i1 = ib.setVrf(vrf1).build();
    i1.setDescription(i1.getName() + " description");
    Interface i2 = ib.setVrf(vrf2).build();
    i2.setDescription(i2.getName() + " description");

    Configuration n2 = cb.build();
    vb.setOwner(n2);
    ib.setOwner(n2);

    Vrf vrf3 = vb.build();
    Vrf vrf4 = vb.build();

    Interface i3 = ib.setVrf(vrf3).build();
    i3.setDescription(i3.getName() + " description");
    Interface i4 = ib.setVrf(vrf4).build();
    i4.setDescription(i4.getName() + " description");

    _testConfigs = ImmutableMap.of(n1.getHostname(), n1, n2.getHostname(), n2);
    _testConfigsInterfaces = ImmutableList.of(i1, i2, i3, i4);

    // create a role that matches just n1
    String roleName = "testRole";
    _roleDim = "roleDim";
    _roleNode = n1;
    _rolePattern = Pattern.compile(roleName);
    _context =
        MockSpecifierContext.builder()
            .setConfigs(_testConfigs)
            .setNodeRolesByDimension(
                ImmutableMap.of(
                    _roleDim, ImmutableSet.of(new NodeRole(roleName, n1.getHostname()))))
            .build();
  }

  private static Set<InterfaceLocation> interfaceLocations(Collection<Interface> interfaces) {
    return interfaces
        .stream()
        .map(i -> new InterfaceLocation(i.getOwner().getHostname(), i.getName()))
        .collect(ImmutableSet.toImmutableSet());
  }

  private static Set<InterfaceLinkLocation> interfaceLinkLocations(
      Collection<Interface> interfaces) {
    return interfaces
        .stream()
        .map(i -> new InterfaceLinkLocation(i.getOwner().getHostname(), i.getName()))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Test
  public void testAllInterfacesLocationSpecifiers() {

    assertThat(
        AllInterfacesLocationSpecifier.INSTANCE.resolve(_context),
        equalTo(interfaceLocations(_testConfigsInterfaces)));
    assertThat(
        AllInterfaceLinksLocationSpecifier.INSTANCE.resolve(_context),
        equalTo(interfaceLinkLocations(_testConfigsInterfaces)));
  }

  @Test
  public void testDescriptionRegexInterfaceLocationSpecifiers() {
    // choose 1 interface from each node
    List<Interface> interfaces =
        _testConfigs
            .entrySet()
            .stream()
            .map(entry -> entry.getValue().getInterfaces().values().iterator().next())
            .collect(Collectors.toList());

    Pattern pat =
        Pattern.compile(
            String.format(
                "(%s|%s) description", interfaces.get(0).getName(), interfaces.get(1).getName()));

    assertThat(
        new DescriptionRegexInterfaceLocationSpecifier(pat).resolve(_context),
        equalTo(interfaceLocations(interfaces)));
    assertThat(
        new DescriptionRegexInterfaceLinkLocationSpecifier(pat).resolve(_context),
        equalTo(interfaceLinkLocations(interfaces)));
  }

  @Test
  public void testIntersectionLocationSpecifier_equalsHashCode() {
    LocationSpecifier spec1a =
        new IntersectionLocationSpecifier(
            AllInterfacesLocationSpecifier.INSTANCE, NullLocationSpecifier.INSTANCE);
    LocationSpecifier spec1b =
        new IntersectionLocationSpecifier(
            AllInterfacesLocationSpecifier.INSTANCE, NullLocationSpecifier.INSTANCE);
    LocationSpecifier spec2 =
        new IntersectionLocationSpecifier(
            AllInterfaceLinksLocationSpecifier.INSTANCE, NullLocationSpecifier.INSTANCE);

    assertThat(spec1a, equalTo(spec1a));
    assertThat(spec1a, equalTo(spec1b));
    assertThat(spec1a.hashCode(), equalTo(spec1b.hashCode()));
    assertThat(spec1a, not(equalTo(spec2)));
    assertThat(spec1a, not(equalTo(NullLocationSpecifier.INSTANCE)));
  }

  @Test
  public void testNameRegexInterfaceLocationSpecifiers() {
    // choose 1 interface from each node
    List<Interface> interfaces =
        _testConfigs
            .entrySet()
            .stream()
            .map(entry -> entry.getValue().getInterfaces().values().iterator().next())
            .collect(Collectors.toList());

    Pattern pat = Pattern.compile(interfaces.get(0).getName() + "|" + interfaces.get(1).getName());

    assertThat(
        new NameRegexInterfaceLocationSpecifier(pat).resolve(_context),
        equalTo(interfaceLocations(interfaces)));
    assertThat(
        new NameRegexInterfaceLinkLocationSpecifier(pat).resolve(_context),
        equalTo(interfaceLinkLocations(interfaces)));
  }

  @Test
  public void testNodeNameRegexInterfaceLocationSpecifiers() {
    Configuration config = _testConfigs.values().iterator().next();
    List<Interface> ifaces = ImmutableList.copyOf(config.getInterfaces().values());
    Pattern pat = Pattern.compile(config.getHostname());

    assertThat(
        new NodeNameRegexInterfaceLocationSpecifier(pat).resolve(_context),
        equalTo(interfaceLocations(ifaces)));

    assertThat(
        new NodeNameRegexInterfaceLinkLocationSpecifier(pat).resolve(_context),
        equalTo(interfaceLinkLocations(ifaces)));
  }

  @Test
  public void testNodeRoleRegexInterfaceLocationSpecifier() {
    Set<Location> nodeInterfaceLocations =
        _roleNode
            .getInterfaces()
            .values()
            .stream()
            .map(iface -> new InterfaceLocation(iface.getOwner().getHostname(), iface.getName()))
            .collect(ImmutableSet.toImmutableSet());

    Set<Location> nodeInterfaceLinkLocations =
        _roleNode
            .getInterfaces()
            .values()
            .stream()
            .map(
                iface -> new InterfaceLinkLocation(iface.getOwner().getHostname(), iface.getName()))
            .collect(ImmutableSet.toImmutableSet());

    assertThat(
        new NodeRoleRegexInterfaceLocationSpecifier(_roleDim, _rolePattern).resolve(_context),
        equalTo(nodeInterfaceLocations));

    assertThat(
        new NodeRoleRegexInterfaceLinkLocationSpecifier(_roleDim, _rolePattern).resolve(_context),
        equalTo(nodeInterfaceLinkLocations));
  }

  @Test
  public void testNullLocationSpecifier() {
    assertThat(NullLocationSpecifier.INSTANCE.resolve(_context), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testSetTheoreticLocationSpecifiers() {
    // first specifier will match interfaces 0 and 1
    List<Interface> interfaces01 = _testConfigsInterfaces.subList(0, 2);
    Pattern pattern01 =
        Pattern.compile(
            String.format("%s|%s", interfaces01.get(0).getName(), interfaces01.get(1).getName()));
    LocationSpecifier locationSpecifier01 = new NameRegexInterfaceLocationSpecifier(pattern01);
    Set<Location> locations01 = locationSpecifier01.resolve(_context);

    // first specifier will match interfaces 1 and 2
    List<Interface> interfaces12 = _testConfigsInterfaces.subList(1, 3);
    Pattern pattern12 =
        Pattern.compile(
            String.format("%s|%s", interfaces12.get(0).getName(), interfaces12.get(1).getName()));
    LocationSpecifier locationSpecifier12 = new NameRegexInterfaceLocationSpecifier(pattern12);
    Set<Location> locations12 = locationSpecifier12.resolve(_context);

    // make sure the location sets have non-trivial union/intersection/difference
    assertThat(locations01, hasSize(2));
    assertThat(locations12, hasSize(2));
    assertThat(Sets.union(locations01, locations12), hasSize(3));
    assertThat(Sets.intersection(locations01, locations12), hasSize(1));
    assertThat(Sets.difference(locations01, locations12), hasSize(1));

    assertThat(
        new IntersectionLocationSpecifier(locationSpecifier01, locationSpecifier12)
            .resolve(_context),
        equalTo(Sets.intersection(locations01, locations12)));

    assertThat(
        new UnionLocationSpecifier(locationSpecifier01, locationSpecifier12).resolve(_context),
        equalTo(Sets.union(locations01, locations12)));

    assertThat(
        new DifferenceLocationSpecifier(locationSpecifier01, locationSpecifier12).resolve(_context),
        equalTo(Sets.difference(locations01, locations12)));
  }

  @Test
  public void testUnionLocationSpecifier_equalsHashCode() {
    LocationSpecifier spec1a =
        new UnionLocationSpecifier(
            AllInterfacesLocationSpecifier.INSTANCE, NullLocationSpecifier.INSTANCE);
    LocationSpecifier spec1b =
        new UnionLocationSpecifier(
            AllInterfacesLocationSpecifier.INSTANCE, NullLocationSpecifier.INSTANCE);
    LocationSpecifier spec2 =
        new UnionLocationSpecifier(
            AllInterfaceLinksLocationSpecifier.INSTANCE, NullLocationSpecifier.INSTANCE);

    assertThat(spec1a, equalTo(spec1a));
    assertThat(spec1a, equalTo(spec1b));
    assertThat(spec1a.hashCode(), equalTo(spec1b.hashCode()));
    assertThat(spec1a, not(equalTo(spec2)));
    assertThat(spec1a, not(equalTo(NullLocationSpecifier.INSTANCE)));
  }

  @Test
  public void testVrfNameRegexLocationSpecifiers() {
    List<Interface> interfaces = _testConfigsInterfaces.subList(1, 3);
    List<Vrf> vrfs = interfaces.stream().map(Interface::getVrf).collect(Collectors.toList());

    Pattern pattern =
        Pattern.compile(String.format("%s|%s", vrfs.get(0).getName(), vrfs.get(1).getName()));

    Set<Location> interfaceLocations =
        interfaces
            .stream()
            .map(iface -> new InterfaceLocation(iface.getOwner().getHostname(), iface.getName()))
            .collect(Collectors.toSet());

    Set<Location> interfaceLinkLocations =
        interfaces
            .stream()
            .map(
                iface -> new InterfaceLinkLocation(iface.getOwner().getHostname(), iface.getName()))
            .collect(Collectors.toSet());

    assertThat(
        new VrfNameRegexInterfaceLocationSpecifier(pattern).resolve(_context),
        equalTo(interfaceLocations));

    assertThat(
        new VrfNameRegexInterfaceLinkLocationSpecifier(pattern).resolve(_context),
        equalTo(interfaceLinkLocations));
  }
}
