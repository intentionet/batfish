package org.batfish.specifier;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TypesInterfaceSpecifierTest {
  @Rule public final ExpectedException thrown = ExpectedException.none();

  private static final Interface AGGREGATED;
  private static final Interface LOOPBACK;
  private static final Interface NULL;
  private static final Interface PHYSICAL;
  private static final Interface REDUNDANT;
  private static final Interface TUNNEL;
  private static final Interface UNKNOWN;
  private static final Interface VLAN;
  private static final Interface VPN;

  private static final String HOSTNAME = "hostname";
  private static final MockSpecifierContext CTXT;

  static {
    NetworkFactory nf = new NetworkFactory();
    Configuration config =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(HOSTNAME)
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(config).build();
    Interface.Builder ib = nf.interfaceBuilder().setOwner(config).setVrf(vrf);

    AGGREGATED = ib.build();
    AGGREGATED.setInterfaceType(InterfaceType.AGGREGATED);

    LOOPBACK = ib.build();
    LOOPBACK.setInterfaceType(InterfaceType.LOOPBACK);

    NULL = ib.build();
    NULL.setInterfaceType(InterfaceType.NULL);

    PHYSICAL = ib.build();
    PHYSICAL.setInterfaceType(InterfaceType.PHYSICAL);

    REDUNDANT = ib.build();
    REDUNDANT.setInterfaceType(InterfaceType.REDUNDANT);

    TUNNEL = ib.build();
    TUNNEL.setInterfaceType(InterfaceType.TUNNEL);

    UNKNOWN = ib.build();
    UNKNOWN.setInterfaceType(InterfaceType.UNKNOWN);

    VLAN = ib.build();
    VLAN.setInterfaceType(InterfaceType.VLAN);

    VPN = ib.build();
    VPN.setInterfaceType(InterfaceType.VPN);

    CTXT =
        MockSpecifierContext.builder()
            .setConfigs(ImmutableMap.of(config.getHostname(), config))
            .build();
  }

  @Test
  public void testBadRegex() {
    thrown.expect(IllegalArgumentException.class);
    new TypesInterfaceSpecifier(Pattern.compile("bad regex"));
  }

  private static Set<NodeInterfacePair> specifiedInterfaces(String regex) {
    return new TypesInterfaceSpecifier(Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
        .resolve(ImmutableSet.of(HOSTNAME), CTXT);
  }

  @Test
  public void test() {
    assertThat(
        specifiedInterfaces("a.*"), equalTo(ImmutableSet.of(NodeInterfacePair.of(AGGREGATED))));
    assertThat(
        specifiedInterfaces("l.*"), equalTo(ImmutableSet.of(NodeInterfacePair.of(LOOPBACK))));
    assertThat(specifiedInterfaces("n.*"), equalTo(ImmutableSet.of(NodeInterfacePair.of(NULL))));
    assertThat(
        specifiedInterfaces("p.*"), equalTo(ImmutableSet.of(NodeInterfacePair.of(PHYSICAL))));
    assertThat(
        specifiedInterfaces("r.*"), equalTo(ImmutableSet.of(NodeInterfacePair.of(REDUNDANT))));
    assertThat(specifiedInterfaces("t.*"), equalTo(ImmutableSet.of(NodeInterfacePair.of(TUNNEL))));
    assertThat(specifiedInterfaces("u.*"), equalTo(ImmutableSet.of(NodeInterfacePair.of(UNKNOWN))));
    assertThat(specifiedInterfaces("vlan"), equalTo(ImmutableSet.of(NodeInterfacePair.of(VLAN))));
    assertThat(specifiedInterfaces("vpn"), equalTo(ImmutableSet.of(NodeInterfacePair.of(VPN))));
    assertThat(
        specifiedInterfaces(".*"),
        equalTo(
            ImmutableSet.of(
                NodeInterfacePair.of(AGGREGATED),
                NodeInterfacePair.of(LOOPBACK),
                NodeInterfacePair.of(NULL),
                NodeInterfacePair.of(PHYSICAL),
                NodeInterfacePair.of(REDUNDANT),
                NodeInterfacePair.of(TUNNEL),
                NodeInterfacePair.of(UNKNOWN),
                NodeInterfacePair.of(VLAN),
                NodeInterfacePair.of(VPN))));
  }
}
