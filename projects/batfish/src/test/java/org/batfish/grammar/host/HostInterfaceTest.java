package org.batfish.grammar.host;

import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSourceNats;
import static org.batfish.datamodel.matchers.SourceNatMatchers.hasPoolIpFirst;
import static org.batfish.datamodel.matchers.SourceNatMatchers.hasPoolIpLast;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import org.batfish.common.Warnings;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.representation.host.HostInterface;
import org.hamcrest.FeatureMatcher;
import org.junit.Before;
import org.junit.Test;

public class HostInterfaceTest {

  public static class IsShared extends FeatureMatcher<HostInterface, Boolean> {

    public IsShared() {
      super(equalTo(true), "shared", "shared");
    }

    @Override
    protected Boolean featureValueOf(HostInterface actual) {
      return actual.getShared();
    }
  }

  public static IsShared isShared() {
    return new IsShared();
  }

  private Configuration _c;

  private NetworkFactory _factory;

  private Warnings _w;

  @Before
  public void setup() {
    _factory = new NetworkFactory();
    _c =
        _factory
            .configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.HOST)
            .setHostname("hostInterfaceTest")
            .build();
    _w = new Warnings();
  }

  @Test
  public void testShared() throws IOException {
    Ip sharedIp = new Ip("1.0.0.1");
    InterfaceAddress sharedAddress = new InterfaceAddress(sharedIp, 24);
    Prefix nonShared1Prefix = Prefix.parse("2.0.0.2/24");
    Prefix nonShared2Prefix = Prefix.parse("3.0.0.2/24");
    String ifaceSharedText =
        "{\"name\":\"shared_interface\", \"prefix\":\"" + sharedAddress + "\", \"shared\":true}";
    String ifaceNonShared1Text =
        "{\"name\":\"non_shared1_interface\", \"prefix\":\""
            + nonShared1Prefix
            + "\", \"shared\":false}";
    String ifaceNonShared2Text =
        "{\"name\":\"non_shared2_interface\", \"prefix\":\"" + nonShared2Prefix + "\"}";

    HostInterface sharedHostInterface =
        BatfishObjectMapper.mapper().readValue(ifaceSharedText, HostInterface.class);
    HostInterface nonShared1HostInterface =
        BatfishObjectMapper.mapper().readValue(ifaceNonShared1Text, HostInterface.class);
    HostInterface nonShared2HostInterface =
        BatfishObjectMapper.mapper().readValue(ifaceNonShared2Text, HostInterface.class);
    Interface sharedInterface = sharedHostInterface.toInterface(_c, _w);
    Interface nonShared1Interface = nonShared1HostInterface.toInterface(_c, _w);
    Interface nonShared2Interface = nonShared2HostInterface.toInterface(_c, _w);

    /*
     * Check that shared status from text is propagated into instances with correct defaults.
     */
    assertThat(sharedHostInterface, isShared());
    assertThat(nonShared1HostInterface, not(isShared()));
    assertThat(nonShared2HostInterface, not(isShared()));

    /*
     * The shared interface should contain source NAT info as indicated, while the other interfaces
     * should not contain any source NAT information.
     */
    assertThat(
        sharedInterface,
        hasSourceNats(hasItem(allOf(hasPoolIpFirst(sharedIp), hasPoolIpLast(sharedIp)))));
    assertThat(nonShared1Interface, hasSourceNats(empty()));
    assertThat(nonShared2Interface, hasSourceNats(empty()));
  }
}
