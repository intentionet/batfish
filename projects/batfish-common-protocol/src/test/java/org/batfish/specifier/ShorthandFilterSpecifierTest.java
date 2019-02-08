package org.batfish.specifier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.questions.FiltersSpecifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ShorthandFilterSpecifierTest {
  @Rule public final ExpectedException exception = ExpectedException.none();

  private static final String _interfaceName = "eth0";
  private static final String _nodeName = "node0";
  private static final MockSpecifierContext _ctxt;

  private static final IpAccessList _filter1 = IpAccessList.builder().setName("filter1").build();
  private static final IpAccessList _filter2 = IpAccessList.builder().setName("filter2").build();

  static {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb = nf.configurationBuilder().setHostname(_nodeName);
    cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = nf.interfaceBuilder();

    Configuration n1 = cb.build();

    ib.setOwner(n1)
        .setName(_interfaceName)
        .setIncomingFilter(_filter1)
        .setIncomingFilter(_filter2)
        .build();

    n1.getIpAccessLists()
        .putAll(ImmutableMap.of(_filter1.getName(), _filter1, _filter2.getName(), _filter2));

    _ctxt = MockSpecifierContext.builder().setConfigs(ImmutableMap.of(_nodeName, n1)).build();
  }

  @Test
  public void resolve() {
    assertThat(
        new ShorthandFilterSpecifier(new FiltersSpecifier(_filter1.getName()))
            .resolve(_nodeName, _ctxt),
        equalTo(ImmutableSet.of(_filter1)));
  }

  @Test
  public void resolveMissingNode() {
    exception.expect(IllegalArgumentException.class);
    new ShorthandFilterSpecifier(FiltersSpecifier.ALL).resolve("NonExistentNode", _ctxt);
  }
}
