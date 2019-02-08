package org.batfish.specifier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.specifier.InterfaceSpecifierFilterSpecifier.Type;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link InterfaceSpecifierFilterSpecifier}, */
public class InterfaceSpecifierFilterSpecifierTest {
  @Rule public final ExpectedException exception = ExpectedException.none();

  private static final String _nodeName = "node0";
  private static final MockSpecifierContext _ctxt;

  private static final IpAccessList _filter1 = IpAccessList.builder().setName("filter1").build();
  private static final IpAccessList _filter2 = IpAccessList.builder().setName("filter2").build();

  static {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb = nf.configurationBuilder().setHostname(_nodeName);
    cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    Configuration n1 = cb.build();

    nf.interfaceBuilder()
        .setOwner(n1)
        .setName("eth0")
        .setIncomingFilter(_filter1)
        .setOutgoingFilter(_filter2)
        .build();

    // second interface with no filters.
    nf.interfaceBuilder().setOwner(n1).setName("eth1").build();

    n1.getIpAccessLists()
        .putAll(ImmutableMap.of(_filter1.getName(), _filter1, _filter2.getName(), _filter2));

    _ctxt = MockSpecifierContext.builder().setConfigs(ImmutableMap.of(_nodeName, n1)).build();
  }

  @Test
  public void resolveInFilter() {
    assertThat(
        new InterfaceSpecifierFilterSpecifier(
                Type.IN_FILTER,
                new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier("eth0"))
            .resolve(_nodeName, _ctxt),
        equalTo(ImmutableSet.of(_filter1)));
  }

  @Test
  public void resolveOutFilter() {
    assertThat(
        new InterfaceSpecifierFilterSpecifier(
                Type.OUT_FILTER,
                new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier("eth0"))
            .resolve(_nodeName, _ctxt),
        equalTo(ImmutableSet.of(_filter2)));
  }

  @Test
  public void resolveMissingFilter() {
    assertThat(
        new InterfaceSpecifierFilterSpecifier(
                Type.OUT_FILTER,
                new FlexibleInterfaceSpecifierFactory().buildInterfaceSpecifier("eth1"))
            .resolve(_nodeName, _ctxt),
        equalTo(ImmutableSet.of()));
  }
}
