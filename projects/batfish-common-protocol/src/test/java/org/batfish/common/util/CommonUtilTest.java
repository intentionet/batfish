package org.batfish.common.util;

import static org.batfish.common.util.CommonUtil.asNegativeIpWildcards;
import static org.batfish.common.util.CommonUtil.asPositiveIpWildcards;
import static org.batfish.common.util.CommonUtil.communityStringToLong;
import static org.batfish.common.util.CommonUtil.longToCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Topology;
import org.junit.Ignore;
import org.junit.Test;

/** Tests of utility methods from {@link org.batfish.common.util.CommonUtil} */
public class CommonUtilTest {

  /** Test that asPositiveIpWildcards handles null */
  @Test
  public void testAsPositiveIpWildcards() {
    assertThat(asPositiveIpWildcards(null), nullValue());
  }

  /** Test that asNegativeIpWildcards handles null */
  @Test
  public void testAsNegativeIpWildcards() {
    assertThat(asNegativeIpWildcards(null), nullValue());
  }

  @Test
  public void testCommunityStringToLong() {
    assertThat(communityStringToLong("0:0"), equalTo(0L));
    assertThat(communityStringToLong("65535:65535"), equalTo(4294967295L));
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testCommunityStringToLongInvalidInput() {
    communityStringToLong("111");
  }

  @Test(expected = NumberFormatException.class)
  public void testCommunityStringToLongNoInput() {
    communityStringToLong("");
  }

  @Test(expected = IllegalArgumentException.class)
  @Ignore("https://github.com/batfish/batfish/issues/2103")
  public void testCommunityStringHighTooBig() {
    communityStringToLong("65537:1");
  }

  @Test(expected = IllegalArgumentException.class)
  @Ignore("https://github.com/batfish/batfish/issues/2103")
  public void testCommunityStringLowTooBig() {
    communityStringToLong("1:65537");
  }

  @Test
  public void testLongToCommunity() {
    assertThat(longToCommunity(0L), equalTo("0:0"));
    assertThat(longToCommunity(4294967295L), equalTo("65535:65535"));
  }

  @Test
  public void testSynthesizeTopology_asymmetric() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    Interface i1 =
        nf.interfaceBuilder().setOwner(c1).setAddresses(new InterfaceAddress("1.2.3.4/24")).build();
    Interface i2 =
        nf.interfaceBuilder().setOwner(c2).setAddresses(new InterfaceAddress("1.2.3.5/28")).build();
    Topology t =
        CommonUtil.synthesizeTopology(ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2));
    assertThat(t.getEdges(), equalTo(ImmutableSet.of(new Edge(i1, i2), new Edge(i2, i1))));
  }

  @Test
  public void testSynthesizeTopology_asymmetricPartialOverlap() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    nf.interfaceBuilder().setOwner(c1).setAddresses(new InterfaceAddress("1.2.3.4/24")).build();
    nf.interfaceBuilder().setOwner(c2).setAddresses(new InterfaceAddress("1.2.3.17/28")).build();
    Topology t =
        CommonUtil.synthesizeTopology(ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2));
    assertThat(t.getEdges(), empty());
  }

  @Test
  public void testSynthesizeTopology_asymmetricSharedIp() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    nf.interfaceBuilder().setOwner(c1).setAddresses(new InterfaceAddress("1.2.3.4/24")).build();
    nf.interfaceBuilder().setOwner(c2).setAddresses(new InterfaceAddress("1.2.3.4/28")).build();
    Topology t =
        CommonUtil.synthesizeTopology(ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2));
    assertThat(t.getEdges(), empty());
  }
}
