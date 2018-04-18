package org.batfish.grammar.cisco;

import static org.batfish.datamodel.ConfigurationFormat.CISCO_ASA;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasDstIps;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasSrcIps;
import static org.batfish.datamodel.matchers.IpAccessListLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.hasHeaderSpace;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.isMatchHeaderSpaceThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CiscoAclTest {
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private Configuration parseConfig(String hostname) throws IOException {
    String[] names = new String[] {TESTCONFIGS_PREFIX + hostname};
    return BatfishTestUtils.parseTextConfigs(_folder, names).get(hostname);
  }

  @Test
  public void testAclMasks() throws IOException {
    testAcls("aclAsa", CISCO_ASA);
    testAcls("aclIos", CISCO_IOS);
  }

  private void testAcls(String hostname, ConfigurationFormat format) throws IOException {
    Configuration c = parseConfig(hostname);
    IpAccessList acl = c.getIpAccessLists().get("acl");
    IpAccessListLine line = Iterables.getOnlyElement(acl.getLines());

    assertThat(c.getConfigurationFormat(), equalTo(format));
    ImmutableList.of("1.2.3.0", "1.2.3.255")
        .stream()
        .map(Ip::new)
        .forEach(
            ip ->
                assertThat(
                    line,
                    hasMatchCondition(
                        isMatchHeaderSpaceThat(hasHeaderSpace(hasSrcIps(containsIp(ip)))))));
    ImmutableList.of("4.3.0.0", "4.3.255.255")
        .stream()
        .map(Ip::new)
        .forEach(
            ip ->
                assertThat(
                    line,
                    hasMatchCondition(
                        isMatchHeaderSpaceThat(hasHeaderSpace(hasDstIps(containsIp(ip)))))));
  }
}
